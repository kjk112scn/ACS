package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.config.ThreadManager
import com.gtlsystems.acs_api.controller.mode.EphemerisController
import com.gtlsystems.acs_api.model.GlobalData
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service  // 이 어노테이션을 추가하여 Spring Bean으로 등록
class InitService(
    private val threadManager: ThreadManager  // ✅ 이대로 유지
) {
    private val logger = LoggerFactory.getLogger(EphemerisController::class.java)
    @PostConstruct
    /*
    DB 서버에서 초기 설정 정보를 입력하기 위함
    초기 동작해야하는 로직이 있으면 이곳에 추가
     */
    fun init() {
        //GlobalData.Location.latitude = 37.566535
        //GlobalData.Location.longitude = 126.9779692
        println("InitService init() called")
        println("utcNow: ${GlobalData.Time.utcNow}")
        println("localNow: ${GlobalData.Time.localNow}")
        println("serverTimeZone: ${GlobalData.Time.serverTimeZone}")
        println("ServerTime: ${GlobalData.Time.serverTime}")
        println("resultTimeOffsetCalTime: ${GlobalData.Time.resultTimeOffsetCalTime}")

        applyHardwareOptimization()
    }
    /**
     * ✅ 하드웨어 최적화 적용
     */
    private fun applyHardwareOptimization() {
        try {
            // 1. 시스템 사양 자동 감지
            val specs = threadManager.detectSystemSpecs()

            // 2. 성능 등급 분류
            val tier = threadManager.classifyPerformanceTier(specs)
            logger.info("📊 성능 등급: $tier")

            // 3. 하드웨어 최적화 설정 적용
            threadManager.applyHardwareOptimization(tier)

            logger.info("✅ 하드웨어 최적화 완료")
        } catch (e: Exception) {
            logger.error("❌ 하드웨어 최적화 실패: ${e.message}", e)
        }
    }
}
