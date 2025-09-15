package com.gtlsystems.acs_api.openapi 

import io.swagger.v3.oas.models.Operation
import com.gtlsystems.acs_api.config.Language 
import org.springframework.web.method.HandlerMethod 

/**
 * OpenAPI 설명 관리를 위한 공통 유틸리티
 * 각 컨트롤러의 API 설명을 절차에 맞게 적용하는 클래스입니다.
 */ 1   
object OpenApiUtils { 

    /**
     * 컨트롤러 유형에 따라 적절한 API 설명을 적용합니다.
     *
     * @param operation OpenAPI Operation 객체
     * @param handlerMethod 컨트롤러 메서드 정보
     * @param language 사용자 언어
     */
    fun applyApiDescriptions(operation: Operation, handlerMethod: HandlerMethod, language: Language) {
        // operationId가 없으면 설명을 적용할 수 없음
        val operationId = operation.operationId?.lowercase() ?: return
        
        // 컨트롤러 클래스 이름을 기준으로 적절한 설명 적용
        when (handlerMethod.beanType.simpleName) {
            "EphemerisController" -> {
                EphemerisApiDescriptions.applyDescriptions(operation, operationId, language)
            }
            "PassScheduleController" -> {
                PassScheduleApiDescriptions.applyDescriptions(operation, operationId, language)
            }
            "SunTrackController" -> {
                SunTrackApiDescriptions.applyDescriptions(operation, operationId, language)
            }
            "ICDController" -> {
                ICDApiDescriptions.applyDescriptions(operation, operationId, language)
            }
            // Settings 관련 API 추가
            "SettingsController" -> {
                SettingsApiDescriptions.applyDescriptions(operation, operationId, language)
            }
            // 추가 컨트롤러가 있다면 여기에 추가
        }
    }

    /**
     * API 설명이 올바르게 적용되었는지 검증합니다.
     *
     * @param operation OpenAPI Operation 객체
     * @return 설명이 올바르게 적용되었는지 여부
     */
    fun validateApiDescriptions(operation: Operation): Boolean {
        // 필수 필드 검증
        if (operation.operationId == null) {
            return false
        }
        if (operation.summary == null || operation.summary.isBlank()) {
            return false
        }
        if (operation.description == null || operation.description.isBlank()) {
            return false
        }

        return true
    }

    /**
     * API 설명을 초기화합니다.
     *
     * @param operation OpenAPI Operation 객체
     */
    fun clearApiDescriptions(operation: Operation) {
        operation.summary = null
        operation.description = null
    }
} 
