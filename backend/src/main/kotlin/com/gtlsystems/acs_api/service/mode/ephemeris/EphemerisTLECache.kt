package com.gtlsystems.acs_api.service.mode.ephemeris

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Ephemeris 위성 TLE 데이터 캐시 관리
 *
 * 위성 TLE(Two-Line Element) 데이터를 메모리에 캐시하여
 * 빠른 조회와 관리를 제공합니다.
 *
 * @since Phase 5 - BE 서비스 분리
 */
@Component
class EphemerisTLECache {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * TLE 데이터 캐시
     * Key: 위성 ID
     * Value: Pair(TLE Line 1, TLE Line 2)
     */
    private val cache = ConcurrentHashMap<String, Pair<String, String>>()

    /**
     * 위성 TLE 데이터를 캐시에 추가합니다.
     *
     * @param satelliteId 위성 ID (NORAD 카탈로그 번호)
     * @param tleLine1 TLE 첫 번째 줄
     * @param tleLine2 TLE 두 번째 줄
     */
    fun add(satelliteId: String, tleLine1: String, tleLine2: String) {
        cache[satelliteId] = Pair(tleLine1, tleLine2)
        logger.info("위성 TLE 데이터가 캐시에 추가되었습니다. 위성 ID: $satelliteId")
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
        logger.info("Ephemeris TLE 캐시가 초기화되었습니다. ${size}개 항목 삭제")
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
