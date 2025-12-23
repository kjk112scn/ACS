package com.gtlsystems.acs_api.controller.system

import com.gtlsystems.acs_api.service.hardware.HardwareErrorLogService
import com.gtlsystems.acs_api.service.hardware.PopupResponse
import com.gtlsystems.acs_api.service.websocket.PushDataService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 하드웨어 에러 로그 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/hardware-error-logs")
class HardwareErrorLogController(
    private val hardwareErrorLogService: HardwareErrorLogService,
    private val pushDataService: PushDataService
) {
    
    private val logger = LoggerFactory.getLogger(HardwareErrorLogController::class.java)

    /**
     * 팝업 상태 설정 API
     * @param clientId 클라이언트 ID
     * @param isOpen 팝업 열림 상태
     * @return 팝업이 열릴 때는 전체 로그와 상태바 데이터 반환
     */
    @PostMapping("/popup-state")
    fun setPopupState(
        @RequestParam clientId: String,
        @RequestParam isOpen: Boolean
    ): ResponseEntity<Any> {
        return try {
            logger.info("📱 팝업 상태 설정 요청 - 클라이언트: {}, 열림: {}", clientId, isOpen)
            
            val response = pushDataService.setPopupState(clientId, isOpen)
            
            if (response != null) {
                logger.info("📱 팝업 열림 응답 - 로그 개수: {}", response.allLogs.size)
                ResponseEntity.ok(response)
            } else {
                logger.info("📱 팝업 닫힘 응답")
                ResponseEntity.ok(mapOf("success" to true, "message" to "팝업이 닫혔습니다"))
            }
        } catch (e: Exception) {
            logger.error("❌ 팝업 상태 설정 실패: {}", e.message, e)
            ResponseEntity.internalServerError()
                .body(mapOf("success" to false, "message" to "팝업 상태 설정 실패: ${e.message}"))
        }
    }
    
    /**
     * 모든 에러 로그 조회 API
     */
    @GetMapping
    fun getAllErrorLogs(): ResponseEntity<List<com.gtlsystems.acs_api.service.hardware.HardwareErrorLog>> {
        return try {
            val logs = hardwareErrorLogService.getAllErrorLogs()
            logger.info("📋 모든 에러 로그 조회 - 개수: {}", logs.size)
            ResponseEntity.ok(logs)
        } catch (e: Exception) {
            logger.error("❌ 에러 로그 조회 실패: {}", e.message, e)
            ResponseEntity.internalServerError().build()
        }
    }
    
    /**
     * 활성 에러 로그 조회 API
     */
    @GetMapping("/active")
    fun getActiveErrorLogs(): ResponseEntity<List<com.gtlsystems.acs_api.service.hardware.HardwareErrorLog>> {
        return try {
            val logs = hardwareErrorLogService.getActiveErrorLogs()
            logger.info("📋 활성 에러 로그 조회 - 개수: {}", logs.size)
            ResponseEntity.ok(logs)
        } catch (e: Exception) {
            logger.error("❌ 활성 에러 로그 조회 실패: {}", e.message, e)
            ResponseEntity.internalServerError().build()
        }
    }
    
    /**
     * 페이징된 에러 로그 조회 API (하이브리드 방식)
     */
    @GetMapping("/paginated")
    fun getErrorLogsPaginated(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) severity: String?,
        @RequestParam(required = false) resolvedStatus: String?
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val result = hardwareErrorLogService.getErrorLogsPaginated(
                page = page,
                size = size,
                startDate = startDate,
                endDate = endDate,
                category = category,
                severity = severity,
                resolvedStatus = resolvedStatus
            )
            logger.info("📋 페이징된 에러 로그 조회 - 페이지: {}, 크기: {}, 총개수: {}", page, size, result["totalElements"])
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            logger.error("❌ 페이징된 에러 로그 조회 실패: {}", e.message, e)
            ResponseEntity.internalServerError()
                .body(mapOf("success" to false, "message" to "페이징된 에러 로그 조회 실패: ${e.message}"))
        }
    }
    
    /**
     * 테스트 해결된 에러 로그 생성 API
     */
    @PostMapping("/test-resolved")
    fun createTestResolvedErrorLog(): ResponseEntity<Map<String, Any>> {
        return try {
            hardwareErrorLogService.createTestResolvedErrorLog()
            logger.info("✅ 테스트 해결 에러 로그 생성 완료")
            ResponseEntity.ok(mapOf("success" to true, "message" to "테스트 해결 에러 로그가 생성되었습니다"))
        } catch (e: Exception) {
            logger.error("❌ 테스트 해결 에러 로그 생성 실패: {}", e.message, e)
            ResponseEntity.internalServerError()
                .body(mapOf("success" to false, "message" to "테스트 해결 에러 로그 생성 실패: ${e.message}"))
        }
    }
}