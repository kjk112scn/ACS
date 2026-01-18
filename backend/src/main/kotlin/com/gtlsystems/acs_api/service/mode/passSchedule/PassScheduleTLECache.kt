package com.gtlsystems.acs_api.service.mode.passSchedule

import com.gtlsystems.acs_api.tracking.entity.TleCacheEntity
import com.gtlsystems.acs_api.tracking.repository.TleCacheRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

/**
 * PassSchedule 위성 TLE 데이터 캐시 관리
 *
 * 위성 TLE(Two-Line Element) 데이터와 위성 이름을 함께 관리합니다.
 * Triple(TLE Line 1, TLE Line 2, Satellite Name) 형태로 저장합니다.
 *
 * Write-through 패턴:
 * - 메모리 캐시: 빠른 조회
 * - DB 저장: 영속성 보장
 * - 서버 시작 시 DB에서 로드
 *
 * PassSchedule 모드 특성:
 * - 다중 위성 TLE 허용 (여러 위성 동시 관리)
 * - 개별 위성 삭제 시 Soft Delete
 *
 * @since Phase 5 - BE 서비스 분리
 * @since Phase 6 - DB 연동 추가
 */
@Component
class PassScheduleTLECache(
    private val tleCacheRepository: TleCacheRepository?
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * TLE 데이터 캐시
     * Key: 위성 ID
     * Value: Triple(TLE Line 1, TLE Line 2, Satellite Name)
     */
    private val cache = ConcurrentHashMap<String, Triple<String, String, String>>()

    /**
     * 서버 시작 시 DB에서 활성 TLE 목록을 로드합니다.
     */
    @PostConstruct
    fun initFromDatabase() {
        if (tleCacheRepository == null) {
            logger.warn("TleCacheRepository가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        tleCacheRepository.findActivePassScheduleTles()
            .doOnNext { entity: TleCacheEntity ->
                val name = entity.satelliteName ?: entity.satelliteId
                cache[entity.satelliteId] = Triple(entity.tleLine1, entity.tleLine2, name)
                logger.info("📥 [DB→캐시] PassSchedule TLE 로드: satelliteId=${entity.satelliteId}, name=$name")
            }
            .doOnComplete {
                logger.info("🚀 PassScheduleTLECache 초기화 완료: ${cache.size}개 위성 로드")
            }
            .doOnError { e: Throwable ->
                logger.error("❌ DB에서 PassSchedule TLE 로드 실패: ${e.message}")
            }
            .subscribe()
    }

    /**
     * 위성 TLE 데이터를 캐시와 DB에 추가합니다.
     * 기존에 같은 satelliteId가 있으면 비활성화 후 새로 추가합니다.
     *
     * @param satelliteId 위성 ID (NORAD 카탈로그 번호)
     * @param tleLine1 TLE 첫 번째 줄
     * @param tleLine2 TLE 두 번째 줄
     * @param satelliteName 위성 이름 (null이면 satelliteId 사용)
     */
    fun add(satelliteId: String, tleLine1: String, tleLine2: String, satelliteName: String? = null) {
        val finalName = satelliteName ?: satelliteId
        cache[satelliteId] = Triple(tleLine1, tleLine2, finalName)
        logger.info("✅ [캐시] PassSchedule TLE 추가: satelliteId=$satelliteId, name=$finalName")

        // DB 저장 (Write-through)
        if (tleCacheRepository != null) {
            // 기존 같은 위성 ID 비활성화 후 새로 저장
            tleCacheRepository.deactivateBySatelliteIdAndMode(satelliteId, TleCacheEntity.MODE_PASS_SCHEDULE)
                .flatMap { deactivatedCount: Int ->
                    if (deactivatedCount > 0) logger.info("📝 [DB] 기존 PassSchedule TLE 비활성화: satelliteId=$satelliteId")
                    val entity = TleCacheEntity(
                        satelliteId = satelliteId,
                        noradId = satelliteId.toIntOrNull(),
                        satelliteName = finalName,
                        tleLine1 = tleLine1,
                        tleLine2 = tleLine2,
                        epochDate = parseTleEpoch(tleLine1),
                        mode = TleCacheEntity.MODE_PASS_SCHEDULE,
                        isActive = true,
                        source = TleCacheEntity.SOURCE_MANUAL
                    )
                    tleCacheRepository.save(entity)
                }
                .doOnSuccess { saved: TleCacheEntity ->
                    logger.info("📝 [DB] PassSchedule TLE 저장 완료: id=${saved.id}, satelliteId=$satelliteId")
                }
                .doOnError { e: Throwable ->
                    logger.error("❌ [DB] PassSchedule TLE 저장 실패: ${e.message}")
                }
                .subscribe()
        }
    }

    /**
     * 위성 TLE 데이터를 캐시에서 가져옵니다 (이름 제외).
     *
     * @param satelliteId 위성 ID
     * @return TLE 데이터 Pair(Line1, Line2), 없으면 null
     */
    fun get(satelliteId: String): Pair<String, String>? {
        val data = cache[satelliteId]
        return data?.let { Pair(it.first, it.second) }
    }

    /**
     * 위성 TLE 데이터와 이름을 함께 가져옵니다.
     *
     * @param satelliteId 위성 ID
     * @return Triple(Line1, Line2, Name), 없으면 null
     */
    fun getWithName(satelliteId: String): Triple<String, String, String>? {
        return cache[satelliteId]
    }

    /**
     * 위성 이름만 가져옵니다.
     *
     * @param satelliteId 위성 ID
     * @return 위성 이름, 없으면 null
     */
    fun getName(satelliteId: String): String? {
        return cache[satelliteId]?.third
    }

    /**
     * 모든 활성 TLE를 Map 형태로 반환합니다.
     *
     * @return Map<satelliteId, Triple<Line1, Line2, Name>>
     */
    fun getAll(): Map<String, Triple<String, String, String>> {
        return cache.toMap()
    }

    /**
     * 위성 TLE 데이터를 캐시와 DB에서 삭제(비활성화)합니다.
     *
     * @param satelliteId 위성 ID
     */
    fun remove(satelliteId: String) {
        cache.remove(satelliteId)
        logger.info("✅ [캐시] PassSchedule TLE 삭제: satelliteId=$satelliteId")

        // DB에서 비활성화 (Soft Delete)
        if (tleCacheRepository != null) {
            tleCacheRepository.deactivateBySatelliteIdAndMode(satelliteId, TleCacheEntity.MODE_PASS_SCHEDULE)
                .doOnSuccess { count: Int ->
                    if (count > 0) logger.info("📝 [DB] PassSchedule TLE 비활성화: satelliteId=$satelliteId")
                }
                .doOnError { e: Throwable ->
                    logger.error("❌ [DB] PassSchedule TLE 비활성화 실패: ${e.message}")
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
        logger.info("✅ [캐시] PassSchedule TLE 캐시 초기화: ${size}개 삭제")

        // DB에서 모든 활성 TLE 비활성화
        if (tleCacheRepository != null) {
            tleCacheRepository.deactivateAllByMode(TleCacheEntity.MODE_PASS_SCHEDULE)
                .doOnSuccess { count: Int ->
                    if (count > 0) logger.info("📝 [DB] PassSchedule TLE ${count}개 비활성화")
                }
                .doOnError { e: Throwable ->
                    logger.error("❌ [DB] PassSchedule TLE 비활성화 실패: ${e.message}")
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
