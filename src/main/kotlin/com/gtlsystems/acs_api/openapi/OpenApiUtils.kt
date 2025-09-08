package com.gtlsystems.acs_api.openapi 
 
import io.swagger.v3.oas.models.Operation
import com.gtlsystems.acs_api.config.Language 
import org.springframework.web.method.HandlerMethod 
 
/**
 * OpenAPI ?�명 관리�? ?�한 공통 ?�틸리티
 * �?컨트롤러??API ?�명???�절???�어�??�용?�는 ??��???�당?�니??
 */
object OpenApiUtils { 

    /**
     * 컨트롤러 ?�?�에 ?�라 ?�절??API ?�명???�용?�니??
     *
     * @param operation OpenAPI Operation 객체
     * @param handlerMethod 컨트롤러 메서???�보
     * @param language ?�용???�어
     */
    fun applyApiDescriptions(operation: Operation, handlerMethod: HandlerMethod, language: Language) {
        // operationId가 ?�으�??�명???�용?????�음
        val operationId = operation.operationId?.lowercase() ?: return
        
        // 컨트롤러 ?�래???�름??기�??�로 ?�절???�명 ?�용
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
            // 추�? 컨트롤러가 ?�다�??�기??추�?
        }
    }

    /**
     * API ?�명???�바르게 ?�용?�었?��? 검증합?�다.
     *
     * @param operation OpenAPI Operation 객체
     * @return ?�명???�바르게 ?�용?�었?��? ?��?
     */
    fun validateApiDescriptions(operation: Operation): Boolean {
        // ?�수 ?�드 검�?
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
     * API ?�명??초기?�합?�다.
     *
     * @param operation OpenAPI Operation 객체
     */
    fun clearApiDescriptions(operation: Operation) {
        operation.summary = null
        operation.description = null
    }
} 
