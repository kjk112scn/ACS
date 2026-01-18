package com.gtlsystems.acs_api.service.mode.ephemeris

import com.gtlsystems.acs_api.tracking.entity.TleCacheEntity
import com.gtlsystems.acs_api.tracking.repository.TleCacheRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

/**
 * Ephemeris 위성 TLE 데이터 캐시 관리
 *
 * 위성 TLE(Two-Line Element) 데이터를 메모리에 캐시하고
 * DB와 동기화하여 서버 재시작 시에도 데이터를 보존합니다.
 *
 * Write-through 패턴:
 * - 메모리 캐시: 빠른 조회
 * - DB 저장: 영속성 보장
 * - 서버 시작 시 DB에서 로드
 *
 * Ephemeris 모드 특성:
 * - 1개의 활성 TLE만 허용 (새 TLE 입력 시 기존 것은 비활성화)
 *
 * @since Phase 5 - BE 서비스 분리
 * @since Phase 6 - DB 연동 추가
 */
@Component
class EphemerisTLECache(
    private val tleCacheRepository: TleCacheRepository?
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * TLE 데이터 캐시
     * Key: 위성 ID
     * Value: Pair(TLE Line 1, TLE Line 2)
     */
    private val cache = ConcurrentHashMap<String, Pair<String, String>>()

    /**
     * 서버 시작 시 DB에서 활성 TLE를 로드합니다.
     */
    @PostConstruct
    fun initFromDatabase() {
        if (tleCacheRepository == null) {
            logger.warn("TleCacheRepository가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        tleCacheRepository.findActiveEphemerisTle()
            .doOnNext { entity: TleCacheEntity ->
                cache[entity.satelliteId] = Pair(entity.tleLine1, entity.tleLine2)
                logger.info("📥 [DB→캐시] Ephemeris TLE 로드: satelliteId=${entity.satelliteId}")
            }
            .doOnError { e: Throwable ->
                logger.error("❌ DB에서 Ephemeris TLE 로드 실패: ${e.message}")
            }
            .subscribe()

        logger.info("🚀 EphemerisTLECache 초기화 완료 (DB 연동 모드)")
    }

    /**
     * 위성 TLE 데이터를 캐시와 DB에 추가합니다.
     * Ephemeris는 1개의 활성 TLE만 허용하므로 기존 TLE는 비활성화됩니다.
     *
     * @param satelliteId 위성 ID (NORAD 카탈로그 번호)
     * @param tleLine1 TLE 첫 번째 줄
     * @param tleLine2 TLE 두 번째 줄
     */
    fun add(satelliteId: String, tleLine1: String, tleLine2: String) {
        // 메모리 캐시 업데이트 (기존 것 제거 후 새로 추가)
        cache.clear()  // Ephemeris는 1개만 유지
        cache[satelliteId] = Pair(tleLine1, tleLine2)
        logger.info("✅ [캐시] Ephemeris TLE 추가: satelliteId=$satelliteId")

        // DB 저장 (Write-through)
        if (tleCacheRepository != null) {
            // 기존 활성 TLE 비활성화
            tleCacheRepository.deactivateAllByMode(TleCacheEntity.MODE_EPHEMERIS)
                .flatMap { deactivatedCount: Int ->
                    if (deactivatedCount > 0) logger.info("📝 [DB] 기존 Ephemeris TLE ${deactivatedCount}개 비활성화")
                    // 새 TLE 저장
                    val entity = TleCacheEntity(
                        satelliteId = satelliteId,
                        noradId = satelliteId.toIntOrNull(),
                        tleLine1 = tleLine1,
                        tleLine2 = tleLine2,
                        epochDate = parseTleEpoch(tleLine1),
                        mode = TleCacheEntity.MODE_EPHEMERIS,
                        isActive = true,
                        source = TleCacheEntity.SOURCE_MANUAL
                    )
                    tleCacheRepository.save(entity)
                }
                .doOnSuccess { saved: TleCacheEntity ->
                    logger.info("📝 [DB] Ephemeris TLE 저장 완료: id=${saved.id}, satelliteId=$satelliteId")
                }
                .doOnError { e: Throwable ->
                    logger.error("❌ [DB] Ephemeris TLE 저장 실패: ${e.message}")
                }
                .subscribe()
        }
    }

    /**
     * 위성 TLE 데이터를 캐시에서 가져옵니다.
     *
     * @param satelliteId 위성 ID
     * @return TLE 데이터 Pair(Line1, Line2), 없으면 null
     */
    fun get(satelliteId: String): Pair<String, String>? {
        return cache[satelliteId]
    }

    /**
     * 현재 활성화된 TLE를 가져옵니다 (Ephemeris는 1개만 존재).
     *
     * @return Pair(satelliteId, Pair(Line1, Line2)), 없으면 null
     */
    fun getActive(): Pair<String, Pair<String, String>>? {
        return cache.entries.firstOrNull()?.let { entry ->
            Pair(entry.key, entry.value)
        }
    }

    /**
     * 위성 TLE 데이터를 캐시와 DB에서 삭제(비활성화)합니다.
     *
     * @param satelliteId 위성 ID
     */
    fun remove(satelliteId: String) {
        cache.remove(satelliteId)
        logger.info("✅ [캐시] Ephemeris TLE 삭제: satelliteId=$satelliteId")

        // DB에서 비활성화 (Soft Delete)
        if (tleCacheRepository != null) {
            tleCacheRepository.deactivateBySatelliteIdAndMode(satelliteId, TleCacheEntity.MODE_EPHEMERIS)
                .doOnSuccess { count: Int ->
                    if (count > 0) logger.info("📝 [DB] Ephemeris TLE 비활성화: satelliteId=$satelliteId")
                }
                .doOnError { e: Throwable ->
                    logger.error("❌ [DB] Ephemeris TLE 비활성화 실패: ${e.message}")
                }
                .subscribe()
        }
    }

    /**
     * 캐시된 모든 위성 ID 목록을 반환합니다.
     *
     * @return 위성 ID 목록
     */
    fun getAllIds(): List<String> {
        return cache.keys.toList()
    }

    /**
     * 캐시와 DB의 모든 활성 TLE를 비활성화합니다.
     */
    fun clear() {
        val size = cache.size
        cache.clear()
        logger.info("✅ [캐시] Ephemeris TLE 캐시 초기화: ${size}개 삭제")

        // DB에서 모든 활성 TLE 비활성화
        if (tleCacheRepository != null) {
            tleCacheRepository.deactivateAllByMode(TleCacheEntity.MODE_EPHEMERIS)
                .doOnSuccess { count: Int ->
                    if (count > 0) logger.info("📝 [DB] Ephemeris TLE ${count}개 비활성화")
                }
                .doOnError { e: Throwable ->
                    logger.error("❌ [DB] Ephemeris TLE 비활성화 실패: ${e.message}")
                }
                .subscribe()
        }
    }

    /**
     * 캐시된 항목 수를 반환합니다.
     *
     * @return 캐시 크기
     */
    fun size(): Int {
        return cache.size
    }

    /**
     * 특정 위성 ID가 캐시에 존재하는지 확인합니다.
     *
     * @param satelliteId 위성 ID
     * @return 존재 여부
     */
    fun contains(satelliteId: String): Boolean {
        return cache.containsKey(satelliteId)
    }

    /**
     * TLE Line 1에서 Epoch 날짜를 파싱합니다.
     */
    private fun parseTleEpoch(tleLine1: String): OffsetDateTime {
        return try {
            // TLE Line 1 format: 1 NNNNNC NNNNNAAA YYDDD.DDDDDDDD ...
            // 위치 18-32: Epoch Year (2자리) + Day of Year (소수점 포함)
            val epochStr = tleLine1.substring(18, 32).trim()
            val year = epochStr.substring(0, 2).toInt()
            val dayOfYear = epochStr.substring(2).toDouble()

            val fullYear = if (year < 57) 2000 + year else 1900 + year
            val epochStart = OffsetDateTime.of(fullYear, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            epochStart.plusSeconds(((dayOfYear - 1) * 86400).toLong())
        } catch (e: Exception) {
            logger.warn("TLE Epoch 파싱 실패, 현재 시간 사용: ${e.message}")
            OffsetDateTime.now(ZoneOffset.UTC)
        }
    }
}
