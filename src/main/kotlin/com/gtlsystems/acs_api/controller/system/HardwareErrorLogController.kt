package com.gtlsystems.acs_api.controller.system

import com.gtlsystems.acs_api.service.hardware.HardwareErrorLogService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 하드웨어 에러 로그 컨트롤러
 */
@RestController
@RequestMapping("/api/hardware-error-logs")
class HardwareErrorLogController(
    private val hardwareErrorLogService: HardwareErrorLogService
) {
    
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
}
