package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * TLE 데이터를 캐시로 관리하고 위성 패스 스케줄링을 담당하는 서비스
 */
@Service
class PassScheduleService(
    private val orekitCalculator: OrekitCalculator
) {
    private val logger = LoggerFactory.getLogger(PassScheduleService::class.java)

    // TLE 캐시 저장소 (위성 카탈로그 ID -> TLE Line1, Line2)
    private val passScheduleTleCache = ConcurrentHashMap<String, Pair<String, String>>()

    @PostConstruct
    fun init() {
        logger.info("PassScheduleService 초기화 완료")
    }

    /**
     * 위성 TLE 데이터를 캐시에 추가합니다.
     */
    fun addPassScheduleTle(satelliteId: String, tleLine1: String, tleLine2: String) {
        passScheduleTleCache[satelliteId] = Pair(tleLine1, tleLine2)
        logger.info("위성 TLE 데이터가 캐시에 추가되었습니다. 위성 ID: $satelliteId")
    }

    /**
     * 위성 TLE 데이터를 캐시에서 가져옵니다.
     */
    fun getPassScheduleTle(satelliteId: String): Pair<String, String>? {
        return passScheduleTleCache[satelliteId]
    }

    /**
     * 위성 TLE 데이터를 캐시에서 삭제합니다.
     */
    fun removePassScheduleTle(satelliteId: String) {
        passScheduleTleCache.remove(satelliteId)
        logger.info("위성 TLE 데이터가 캐시에서 삭제되었습니다. 위성 ID: $satelliteId")
    }

    /**
     * 캐시된 모든 위성 ID 목록을 반환합니다.
     */
    fun getAllPassScheduleTleIds(): List<String> {
        return passScheduleTleCache.keys.toList()
    }

    /**
     * 캐시된 TLE 개수를 반환합니다.
     */
    fun getCacheSize(): Int {
        return passScheduleTleCache.size
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    fun clearCache() {
        val size = passScheduleTleCache.size
        passScheduleTleCache.clear()
        logger.info("TLE 캐시 전체 삭제 완료: ${size}개 항목 삭제")
    }
}