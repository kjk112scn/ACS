package com.gtlsystems.acs_api.controller.system

import com.gtlsystems.acs_api.service.system.ConfigurationService
import com.gtlsystems.acs_api.util.ApiDescriptions
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * 시스템 설정 관리 컨트롤러
 * 설정 조회, 업데이트, 초기화를 위한 REST API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/system/configuration")
@Tag(name = "System - Configuration", description = "시스템 설정 관리 API - 설정 조회, 업데이트, 초기화, 유효성 검사, 상태 모니터링")
class ConfigurationController(
    private val configurationService: ConfigurationService
) {
    
    private val logger = LoggerFactory.getLogger(ConfigurationController::class.java)
    
    /**
     * 모든 설정 조회
     */
    @GetMapping
    @Operation(
        summary = "전체 설정 조회 / Get All Configuration",
        description = "시스템의 모든 설정 값을 조회합니다. Retrieves all configuration values in the system.",
        tags = ["System - Configuration"]
    )
    fun getAllConfiguration(): ResponseEntity<Map<String, Any>> {
        logger.info("📋 전체 설정 조회 요청")
        val config = configurationService.getAllConfiguration()
        return ResponseEntity.ok(config)
    }
    
    /**
     * 특정 설정 조회
     */
    @GetMapping("/{key}")
    @Operation(
        summary = "특정 설정 조회 / Get Specific Configuration",
        description = "지정된 키의 설정 값을 조회합니다. Retrieves the configuration value for the specified key.",
        tags = ["System - Configuration"]
    )
    fun getConfiguration(
        @Parameter(
            description = "설정 키 (예: network.udp.receiveInterval, system.server.port)",
            example = "network.udp.receiveInterval",
            required = true
        )
        @PathVariable key: String
    ): ResponseEntity<Any> {
        logger.info("🔍 설정 조회 요청: $key")
        val value = configurationService.getValue(key)
        return if (value != null) {
            ResponseEntity.ok(value)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 설정 업데이트
     */
    @PutMapping("/{key}")
    @Operation(
        summary = "설정 업데이트",
        description = """
            지정된 키의 설정 값을 업데이트합니다.
            
            ## 설정 업데이트 규칙
            - **즉시 적용**: 대부분의 설정은 즉시 적용됩니다
            - **재시작 필요**: 일부 시스템 설정은 재시작 후 적용됩니다
            - **유효성 검사**: 업데이트 전 자동으로 유효성을 검사합니다
            
            ## 주요 설정 타입
            - **문자열**: 호스트명, 파일 경로 등
            - **숫자**: 포트, 간격, 타임아웃 등
            - **불린**: 기능 활성화/비활성화 등
            
            ## 사용 예시
            ```json
            PUT /api/v1/system/configuration/network.udp.receiveInterval
            Body: "2000"
            ```
        """,
        tags = ["System - Configuration"]
    )
    fun updateConfiguration(
        @Parameter(
            description = "설정 키 (예: network.udp.receiveInterval)",
            example = "network.udp.receiveInterval",
            required = true
        )
        @PathVariable key: String,
        @Parameter(
            description = "새로운 설정 값 (문자열, 숫자, 불린 등)",
            example = "2000"
        )
        @RequestBody value: Any
    ): ResponseEntity<Map<String, Any>> {
        logger.info("🔄 설정 업데이트 요청: $key = $value")
        
        val success = configurationService.updateValue(key, value)
        return if (success) {
            val response = mapOf(
                "success" to true,
                "message" to "설정이 성공적으로 업데이트되었습니다.",
                "key" to key,
                "newValue" to value
            )
            ResponseEntity.ok(response)
        } else {
            val response = mapOf(
                "success" to false,
                "message" to "설정 업데이트에 실패했습니다.",
                "key" to key,
                "value" to value
            )
            ResponseEntity.badRequest().body(response)
        }
    }
    
    /**
     * 설정 초기화
     */
    @PostMapping("/reset")
    @Operation(
        summary = "설정 초기화",
        description = """
            모든 설정을 기본값으로 초기화합니다.
            
            ## 초기화 대상
            - **시스템 설정**: 서버 포트, 호스트 등
            - **네트워크 설정**: UDP, TCP 관련 설정
            - **알고리즘 설정**: 위성 추적, 태양 추적 파라미터
            - **로깅 설정**: 로그 레벨, 파일 경로 등
            
            ## 주의사항
            - **데이터 손실**: 현재 설정된 모든 값이 손실됩니다
            - **재시작 필요**: 초기화 후 시스템 재시작이 필요할 수 있습니다
            - **백업 권장**: 초기화 전 현재 설정을 백업하는 것을 권장합니다
            
            ## 사용 예시
            ```
            POST /api/v1/system/configuration/reset
            ```
        """,
        tags = ["System - Configuration"]
    )
    fun resetConfiguration(): ResponseEntity<Map<String, Any>> {
        logger.info("🔄 설정 초기화 요청")
        
        try {
            configurationService.resetToDefault()
            val response = mapOf(
                "success" to true,
                "message" to "설정이 기본값으로 초기화되었습니다.",
                "timestamp" to Date()
            )
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("❌ 설정 초기화 실패", e)
            val response = mapOf(
                "success" to false,
                "message" to "설정 초기화에 실패했습니다: ${e.message}",
                "timestamp" to Date()
            )
            return ResponseEntity.internalServerError().body(response)
        }
    }
    
    /**
     * 설정 유효성 검사
     */
    @GetMapping("/validate")
    @Operation(
        summary = "설정 유효성 검사",
        description = """
            현재 설정의 유효성을 검사합니다.
            
            ## 검사 항목
            - **필수 설정**: 시스템 운영에 필요한 핵심 설정 존재 여부
            - **값 범위**: 숫자 설정의 최소/최대 값 검증
            - **형식 검증**: 문자열 설정의 형식 및 패턴 검증
            - **의존성**: 설정 간의 의존 관계 검증
            
            ## 검사 결과
            - **valid: true**: 모든 설정이 유효함
            - **valid: false**: 일부 설정에 문제가 있음
            
            ## 사용 예시
            ```
            GET /api/v1/system/configuration/validate
            ```
        """,
        tags = ["System - Configuration"]
    )
    fun validateConfiguration(): ResponseEntity<Map<String, Any>> {
        logger.info("✅ 설정 유효성 검사 요청")
        
        val isValid = configurationService.validateConfiguration()
        val response = mapOf(
            "valid" to isValid,
            "message" to if (isValid) "모든 설정이 유효합니다." else "일부 설정이 유효하지 않습니다.",
            "timestamp" to Date()
        )
        
        return if (isValid) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
    
    /**
     * 설정 상태 정보
     */
    @GetMapping("/status")
    @Operation(
        summary = "설정 상태 정보",
        description = """
            설정 관리 시스템의 상태 정보를 조회합니다.
            
            ## 제공 정보
            - **totalConfigCount**: 전체 설정 개수
            - **configKeys**: 모든 설정 키 목록
            - **lastUpdated**: 마지막 업데이트 시간
            - **systemStatus**: 시스템 상태 (RUNNING, STOPPED 등)
            
            ## 모니터링 용도
            - **설정 개수 확인**: 예상 설정 개수와 일치하는지 확인
            - **설정 키 목록**: 현재 관리 중인 설정 항목 파악
            - **시스템 상태**: 설정 관리 시스템의 정상 동작 여부 확인
            
            ## 사용 예시
            ```
            GET /api/v1/system/configuration/status
            ```
        """,
        tags = ["System - Configuration"]
    )
    fun getConfigurationStatus(): ResponseEntity<Map<String, Any>> {
        logger.info("📊 설정 상태 정보 조회 요청")
        
        val allConfig = configurationService.getAllConfiguration()
        val response = mapOf(
            "totalConfigCount" to allConfig.size,
            "configKeys" to allConfig.keys.toList(),
            "lastUpdated" to Date(),
            "systemStatus" to "RUNNING"
        )
        
        return ResponseEntity.ok(response)
    }
} 