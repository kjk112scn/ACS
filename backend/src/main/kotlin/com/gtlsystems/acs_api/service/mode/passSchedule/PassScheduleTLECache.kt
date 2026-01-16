package com.gtlsystems.acs_api.service.mode.passSchedule

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * PassSchedule 위성 TLE 데이터 캐시 관리
 *
 * 위성 TLE(Two-Line Element) 데이터와 위성 이름을 함께 관리합니다.
 * Triple(TLE Line 1, TLE Line 2, Satellite Name) 형태로 저장합니다.
 *
 * @since Phase 5 - BE 서비스 분리
 */
@Component
class PassScheduleTLECache {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * TLE 데이터 캐시
     * Key: 위성 ID
     * Value: Triple(TLE Line 1, TLE Line 2, Satellite Name)
     */
    private val cache = ConcurrentHashMap<String, Triple<String, String, String>>()

    /**
     * 위성 TLE 데이터를 캐시에 추가합니다.
     *
     * @param satelliteId 위성 ID (NORAD 카탈로그 번호)
     * @param tleLine1 TLE 첫 번째 줄
     * @param tleLine2 TLE 두 번째 줄
     * @param satelliteName 위성 이름 (null이면 satelliteId 사용)
     */
    fun add(satelliteId: String, tleLine1: String, tleLine2: String, satelliteName: String? = null) {
        val finalName = satelliteName ?: satelliteId
        cache[satelliteId] = Triple(tleLine1, tleLine2, finalName)
        logger.info("위성 TLE 데이터가 캐시에 추가되었습니다. 위성 ID: $satelliteId, 이름: $finalName")
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
     * 위성 TLE 데이터를 캐시에서 삭제합니다.
     *
     * @param satelliteId 위성 ID
     */
    fun remove(satelliteId: String) {
        cache.remove(satelliteId)
        logger.info("위성 TLE 데이터가 캐시에서 삭제되었습니다. 위성 ID: $satelliteId")
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
     * 캐시를 완전히 비웁니다.
     */
    fun clear() {
        val size = cache.size
        cache.clear()
        logger.info("PassSchedule TLE 캐시가 초기화되었습니다. ${size}개 항목 삭제")
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
}
