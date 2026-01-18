package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.config.ThreadManager
import com.gtlsystems.acs_api.model.GlobalData
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class InitService(
    private val threadManager: ThreadManager
) {
    companion object {
        private val logger = LoggerFactory.getLogger(InitService::class.java)
    }

    @PostConstruct
    fun init() {
        logger.info("InitService 초기화 시작")
        logger.info("시스템 시간 정보 - UTC: {}, Local: {}, TimeZone: {}",
            GlobalData.Time.utcNow,
            GlobalData.Time.localNow,
            GlobalData.Time.serverTimeZone)
        logger.debug("ServerTime: {}, resultTimeOffsetCalTime: {}",
            GlobalData.Time.serverTime,
            GlobalData.Time.resultTimeOffsetCalTime)

        applyHardwareOptimization()
    }
    /**
     * 하드웨어 최적화 적용
     */
    private fun applyHardwareOptimization() {
        try {
            val specs = threadManager.detectSystemSpecs()
            val tier = threadManager.classifyPerformanceTier(specs)
            logger.info("성능 등급: {}", tier)

            threadManager.applyHardwareOptimization(tier)
            logger.info("하드웨어 최적화 완료")
        } catch (e: Exception) {
            logger.error("하드웨어 최적화 실패: {}", e.message, e)
        }
    }
}
