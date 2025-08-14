package com.gtlsystems.acs_api.controller.system

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LanguageApiDocsController {
    
    // 사용 가능한 API 그룹 정보
    @GetMapping("/api/groups", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAvailableGroups(): ResponseEntity<Map<String, Any>> {
        val groups = mapOf(
            "availableGroups" to listOf(
                mapOf(
                    "name" to "default",
                    "description" to "기본 API 문서 (한국어)",
                    "swaggerUi" to "/swagger-ui.html",
                    "openApiSpec" to "/v3/api-docs"
                ),
                mapOf(
                    "name" to "korean",
                    "description" to "한국어 API 문서",
                    "swaggerUi" to "/swagger-ui.html?group=korean",
                    "openApiSpec" to "/v3/api-docs/korean"
                ),
                mapOf(
                    "name" to "english",
                    "description" to "English API Documentation",
                    "swaggerUi" to "/swagger-ui.html?group=english",
                    "openApiSpec" to "/v3/api-docs/english"
                )
            ),
            "defaultGroup" to "default",
            "message" to "언어별 API 문서에 접근하려면 위의 경로를 사용하세요. 기본 그룹은 한국어입니다."
        )
        return ResponseEntity.ok(groups)
    }
    
    // API 상태 확인
    @GetMapping("/api/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getApiStatus(): ResponseEntity<Map<String, Any>> {
        val status = mapOf(
            "status" to "running",
            "message" to "ACS API가 정상적으로 실행 중입니다",
            "version" to "1.0.0",
            "availableEndpoints" to listOf(
                "/swagger-ui.html",
                "/v3/api-docs",
                "/v3/api-docs/korean",
                "/v3/api-docs/english",
                "/api/groups",
                "/api/status"
            )
        )
        return ResponseEntity.ok(status)
    }
} 