package com.gtlsystems.acs_api.controller.system

import com.gtlsystems.acs_api.service.hardware.HardwareErrorLogService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.slf4j.LoggerFactory

/**
 * 하드웨어 에러 로그 컨트롤러
 */
@RestController
@RequestMapping("/api/hardware-error-logs")
class HardwareErrorLogController(
    private val hardwareErrorLogService: HardwareErrorLogService
) {
    
    private val logger = LoggerFactory.getLogger(HardwareErrorLogController::class.java)

    /**
     * 모든 하드웨어 에러 로그를 조회합니다
     */
    @GetMapping
    fun getAllErrorLogs() = hardwareErrorLogService.getAllErrorLogs()
    
    /**
     * 활성 하드웨어 에러 로그를 조회합니다
     */
    @GetMapping("/active")
    fun getActiveErrorLogs() = hardwareErrorLogService.getActiveErrorLogs()

    @PostMapping("/test")
    fun createTestLog() {
        hardwareErrorLogService.createTestErrorLog()
        logger.info("✅ 테스트 에러 로그 생성 요청 완료")
    }
}
