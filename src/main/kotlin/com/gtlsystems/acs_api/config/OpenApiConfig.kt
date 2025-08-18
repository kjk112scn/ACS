package com.gtlsystems.acs_api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import com.gtlsystems.acs_api.util.ApiDescriptions

@Configuration
class OpenApiConfig {

    @Value("\${server.port:8080}")
    private lateinit var serverPort: String

    // 기본 OpenAPI 설정 (SpringDoc이 자동으로 스캔)
    @Bean
    @Primary
    fun defaultOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("ACS API - Antenna Control System")
                    .description(getKoreanDescription())
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("GTL Systems")
                            .email("support@gtlsystems.com")
                            .url("https://www.gtlsystems.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:$serverPort")
                        .description("로컬 개발 서버"),
                    Server()
                        .url("https://api.gtlsystems.com")
                        .description("프로덕션 서버")
                )
            )
    }

    // 기본 API 그룹 설정 (SpringDoc이 자동으로 스캔)
    @Bean
    fun defaultApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("default")
            .pathsToMatch("/**")
            .packagesToScan("com.gtlsystems.acs_api.controller")
            .addOperationCustomizer(defaultOperationCustomizer())
            .build()
    }
    
    // 기본 그룹용 OperationCustomizer (@Operation 어노테이션 그대로 유지)
    @Bean
    fun defaultOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            // @Operation 어노테이션이 있으면 그대로 유지 (아무것도 변경하지 않음)
            operation
        }
    }

    // 한국어 API 그룹 설정
    @Bean
    fun koreanApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("korean")
            .pathsToMatch("/**")
            .addOpenApiCustomizer(OpenApiCustomizer { openApi ->
                openApi.info(
                    Info()
                        .title("ACS API - Antenna Control System (한국어)")
                        .description(getKoreanDescription())
                        .version("1.0.0")
                        .contact(
                            Contact()
                                .name("GTL Systems")
                                .email("support@gtlsystems.com")
                                .url("https://www.gtlsystems.com")
                        )
                        .license(
                            License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")
                        )
                )
                openApi.servers(
                    listOf(
                        Server()
                            .url("http://localhost:$serverPort")
                            .description("로컬 개발 서버"),
                        Server()
                            .url("https://api.gtlsystems.com")
                            .description("프로덕션 서버")
                    )
                )
                
                // 태그를 한국어로 번역
                openApi.tags?.forEach { tag ->
                    val koreanDescription = when (tag.name?.lowercase()) {
                        "mode" -> "운영 모드 - 위성 추적, 태양 추적, 통과 일정 등"
                        "system" -> "시스템 관리 - 설정, 로깅, 모니터링, 성능 추적"
                        "icd" -> "외부 시스템 통신 - UDP 기반 통신 프로토콜"
                        "data" -> "데이터 관리 - 실시간 데이터 및 WebSocket 푸시"
                        else -> tag.description
                    }
                    tag.description = koreanDescription
                    println("🏷️ [한국어] 태그 번역: ${tag.name} -> $koreanDescription")
                }
            })
            .addOperationCustomizer(koreanOperationCustomizer())
            .build()
    }

    // 영어 API 그룹 설정
    @Bean
    fun englishApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("english")
            .pathsToMatch("/**")
            .addOpenApiCustomizer(OpenApiCustomizer { openApi ->
                openApi.info(
                    Info()
                        .title("ACS API - Antenna Control System (English)")
                        .description(getEnglishDescription())
                        .version("1.0.0")
                        .contact(
                            Contact()
                                .name("GTL Systems")
                                .email("support@gtlsystems.com")
                                .url("https://www.gtlsystems.com")
                        )
                        .license(
                            License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")
                        )
                )
                openApi.servers(
                    listOf(
                        Server()
                            .url("http://localhost:$serverPort")
                            .description("Local Development Server"),
                        Server()
                            .url("https://api.gtlsystems.com")
                            .description("Production Server")
                    )
                )
                
                // 태그를 영어로 번역 - 강제로 영어 설명 적용
                openApi.tags?.forEach { tag ->
                    println("🔍 [영어] 원본 태그: ${tag.name} = ${tag.description}")
                    
                    // 태그 이름에 관계없이 영어 설명으로 강제 변경
                    val englishDescription = when {
                        tag.name?.contains("Ephemeris") == true -> "Operational Modes - Satellite tracking, orbital calculation, and tracking"
                        tag.name?.contains("Sun Track") == true -> "Solar Tracking - Sun position calculation and tracking operations"
                        tag.name?.contains("Pass Schedule") == true -> "Pass Scheduling - Satellite pass schedule management and optimization"
                        tag.name?.contains("Configuration") == true -> "System Configuration - Settings management and validation"
                        tag.name?.contains("Performance") == true -> "Performance Monitoring - System performance and health tracking"
                        tag.name?.contains("Communication") == true -> "External Communication - UDP-based communication protocols"
                        else -> "API Operations for ${tag.name}"
                    }
                    
                    // 강제로 영어 설명 적용
                    tag.description = englishDescription
                    println("✅ [영어] 태그 번역: ${tag.name} -> $englishDescription")
                }
            })
            .addOperationCustomizer(englishOperationCustomizer())
            .build()
    }

    // API 설명을 한국어로 커스터마이징 (한국어 그룹용)
    @Bean
    fun koreanOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            // API 경로 및 메서드 정보 추출
            val methodName = handlerMethod.method.name
            
            // 컨트롤러 타입 추출 (예: EphemerisController -> ephemeris)
            val controllerType = handlerMethod.beanType.simpleName
                .replace("Controller", "")
                .lowercase()
                
            // API 경로 패턴 추출 (어노테이션에서)
            val requestMappingAnnotation = handlerMethod.beanType.getAnnotation(org.springframework.web.bind.annotation.RequestMapping::class.java)
            val methodRequestMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.RequestMapping::class.java).firstOrNull()
            val methodGetMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.GetMapping::class.java).firstOrNull()
            val methodPostMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.PostMapping::class.java).firstOrNull()
            val methodPutMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.PutMapping::class.java).firstOrNull()
            val methodDeleteMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.DeleteMapping::class.java).firstOrNull()
            
            // 클래스 레벨 경로 (예: /api/ephemeris)
            val basePath = requestMappingAnnotation?.value?.firstOrNull() ?: ""
            
            // 메서드 레벨 경로 (예: /tracking/stop)
            val methodPath = when {
                methodRequestMapping != null -> methodRequestMapping.value.firstOrNull() ?: ""
                methodGetMapping != null -> methodGetMapping.value.firstOrNull() ?: ""
                methodPostMapping != null -> methodPostMapping.value.firstOrNull() ?: ""
                methodPutMapping != null -> methodPutMapping.value.firstOrNull() ?: ""
                methodDeleteMapping != null -> methodDeleteMapping.value.firstOrNull() ?: ""
                else -> ""
            }
            
            // 전체 API 경로 (예: /api/ephemeris/tracking/stop)
            val fullPath = "$basePath$methodPath".replace("//", "/")
            
            // API 경로에서 키 추출 (예: ephemeris.tracking.stop)
            val pathSegments = fullPath.trim('/').split("/")
            val apiGroup = if (pathSegments.size > 1) pathSegments[1] else controllerType // api 다음 세그먼트 (ephemeris, pass-schedule 등)
            
            // 경로 변수 처리 (예: /api/ephemeris/tracking/start/{passId})
            val processedSegments = pathSegments.drop(2).map { segment ->
                if (methodPath.contains("{") && methodPath.contains("}") && segment.matches(Regex("[0-9]+"))) {
                    // 숫자로만 된 세그먼트가 있고 메서드 경로에 변수가 있으면 원래 변수명으로 대체
                    val variablePattern = Regex("\\{([^}]+)\\}")
                    val matchResult = variablePattern.find(methodPath)
                    if (matchResult != null) {
                        "{${matchResult.groupValues[1]}}"
                    } else {
                        segment
                    }
                } else {
                    segment
                }
            }
            
            val apiAction = processedSegments.joinToString(".") // 나머지 경로를 점으로 연결
            
            println("🔍 [한국어] 원본 경로: $fullPath")
            println("🔍 [한국어] API 키: $apiGroup.$apiAction")
            
            // 다양한 키 조합 시도 (ApiDescriptions.kt에 있는 키와 매칭)
            val possibleKeys = listOf(
                "$apiGroup.$apiAction",         // ephemeris.tracking.start.{passId} (API 경로 기반)
                methodName,                     // startEphemerisTracking (메서드명)
                "${controllerType}.$methodName", // ephemeris.startEphemerisTracking (컨트롤러+메서드)
                apiAction,                      // tracking.start.{passId} (액션만)
                methodName.lowercase(),         // startephemeristracking (소문자 메서드명)
                apiAction.split(".").last()     // {passId} 또는 stop (마지막 액션만)
            )
            
            // 추가 키 조합 - 경로 변수가 있는 경우
            if (apiAction.contains("{") && apiAction.contains("}")) {
                // 경로 변수를 제거한 버전도 시도 (예: ephemeris.tracking.start)
                val actionWithoutVariable = apiAction.split(".").dropLast(1).joinToString(".")
                possibleKeys.plus(listOf(
                    "$apiGroup.$actionWithoutVariable",  // ephemeris.tracking.start
                    actionWithoutVariable                // tracking.start
                ))
            }
            
            // 여러 키를 시도하여 매칭되는 번역 찾기
            var koreanSummary = methodName
            var koreanDescription = methodName
            
            for (key in possibleKeys) {
                val tempSummary = ApiDescriptions.getKoreanDescription(key, "summary")
                if (tempSummary != key) {
                    koreanSummary = tempSummary
                    break
                }
            }
            
            for (key in possibleKeys) {
                val tempDescription = ApiDescriptions.getKoreanDescription(key, "description")
                if (tempDescription != key) {
                    koreanDescription = tempDescription
                    break
                }
            }
            
            println("🔍 [한국어] 컨트롤러: $controllerType, 메서드: $methodName")
            
            // 한국어 번역이 있으면 적용
            if (koreanSummary != methodName) {
                operation.summary = koreanSummary
                println("✅ [한국어] Summary 적용됨: $koreanSummary")
            } else {
                println("⚠️ [한국어] Summary 번역 없음: $methodName")
            }
            
            if (koreanDescription != methodName) {
                operation.description = koreanDescription
                println("✅ [한국어] Description 적용됨")
            } else {
                println("⚠️ [한국어] Description 번역 없음: $methodName")
            }
            
            operation
        }
    }



    // API 설명을 영어로 커스터마이징 (영어 그룹용)
    @Bean
    fun englishOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            // API 경로 및 메서드 정보 추출
            val methodName = handlerMethod.method.name
            
            // 컨트롤러 타입 추출 (예: EphemerisController -> ephemeris)
            val controllerType = handlerMethod.beanType.simpleName
                .replace("Controller", "")
                .lowercase()
                
            // API 경로 패턴 추출 (어노테이션에서)
            val requestMappingAnnotation = handlerMethod.beanType.getAnnotation(org.springframework.web.bind.annotation.RequestMapping::class.java)
            val methodRequestMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.RequestMapping::class.java).firstOrNull()
            val methodGetMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.GetMapping::class.java).firstOrNull()
            val methodPostMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.PostMapping::class.java).firstOrNull()
            val methodPutMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.PutMapping::class.java).firstOrNull()
            val methodDeleteMapping = handlerMethod.method.getAnnotationsByType(org.springframework.web.bind.annotation.DeleteMapping::class.java).firstOrNull()
            
            // 클래스 레벨 경로 (예: /api/ephemeris)
            val basePath = requestMappingAnnotation?.value?.firstOrNull() ?: ""
            
            // 메서드 레벨 경로 (예: /tracking/stop)
            val methodPath = when {
                methodRequestMapping != null -> methodRequestMapping.value.firstOrNull() ?: ""
                methodGetMapping != null -> methodGetMapping.value.firstOrNull() ?: ""
                methodPostMapping != null -> methodPostMapping.value.firstOrNull() ?: ""
                methodPutMapping != null -> methodPutMapping.value.firstOrNull() ?: ""
                methodDeleteMapping != null -> methodDeleteMapping.value.firstOrNull() ?: ""
                else -> ""
            }
            
            // 전체 API 경로 (예: /api/ephemeris/tracking/stop)
            val fullPath = "$basePath$methodPath".replace("//", "/")
            
            // API 경로에서 키 추출 (예: ephemeris.tracking.stop)
            val pathSegments = fullPath.trim('/').split("/")
            val apiGroup = if (pathSegments.size > 1) pathSegments[1] else controllerType // api 다음 세그먼트 (ephemeris, pass-schedule 등)
            
            // 경로 변수 처리 (예: /api/ephemeris/tracking/start/{passId})
            val processedSegments = pathSegments.drop(2).map { segment ->
                if (methodPath.contains("{") && methodPath.contains("}") && segment.matches(Regex("[0-9]+"))) {
                    // 숫자로만 된 세그먼트가 있고 메서드 경로에 변수가 있으면 원래 변수명으로 대체
                    val variablePattern = Regex("\\{([^}]+)\\}")
                    val matchResult = variablePattern.find(methodPath)
                    if (matchResult != null) {
                        "{${matchResult.groupValues[1]}}"
                    } else {
                        segment
                    }
                } else {
                    segment
                }
            }
            
            val apiAction = processedSegments.joinToString(".") // 나머지 경로를 점으로 연결
            
            println("🔍 [영어] 원본 경로: $fullPath")
            println("🔍 [영어] API 키: $apiGroup.$apiAction")
            
            // 다양한 키 조합 시도 (ApiDescriptions.kt에 있는 키와 매칭)
            val possibleKeys = listOf(
                "$apiGroup.$apiAction",         // ephemeris.tracking.start.{passId} (API 경로 기반)
                methodName,                     // startEphemerisTracking (메서드명)
                "${controllerType}.$methodName", // ephemeris.startEphemerisTracking (컨트롤러+메서드)
                apiAction,                      // tracking.start.{passId} (액션만)
                methodName.lowercase(),         // startephemeristracking (소문자 메서드명)
                apiAction.split(".").last()     // {passId} 또는 stop (마지막 액션만)
            )
            
            // 추가 키 조합 - 경로 변수가 있는 경우
            if (apiAction.contains("{") && apiAction.contains("}")) {
                // 경로 변수를 제거한 버전도 시도 (예: ephemeris.tracking.start)
                val actionWithoutVariable = apiAction.split(".").dropLast(1).joinToString(".")
                possibleKeys.plus(listOf(
                    "$apiGroup.$actionWithoutVariable",  // ephemeris.tracking.start
                    actionWithoutVariable                // tracking.start
                ))
            }
            
            // 여러 키를 시도하여 매칭되는 번역 찾기
            var englishSummary = methodName
            var englishDescription = methodName
            
            for (key in possibleKeys) {
                val tempSummary = ApiDescriptions.getEnglishDescription(key, "summary")
                val tempDescription = ApiDescriptions.getEnglishDescription(key, "description")
                
                if (tempSummary != key) {
                    englishSummary = tempSummary
                    break
                }
            }
            
            for (key in possibleKeys) {
                val tempDescription = ApiDescriptions.getEnglishDescription(key, "description")
                if (tempDescription != key) {
                    englishDescription = tempDescription
                    break
                }
            }
            
            println("🔍 [영어] 컨트롤러: $controllerType, 메서드: $methodName")
            println("🔍 [영어] 원본 Summary: ${operation.summary}")
            
            // 영어 번역이 있으면 적용
            if (englishSummary != methodName) {
                operation.summary = englishSummary
                println("✅ [영어] Summary 적용됨: $englishSummary")
            } else {
                // ApiDescriptions.kt에서 매칭되는 키를 찾지 못한 경우
                // 기존 한글 요약에서 영어로 변환 (강제 변환은 하지 않고 로그만 남김)
                println("⚠️ [영어] Summary 번역 없음: $methodName")
            }
            
            if (englishDescription != methodName) {
                operation.description = englishDescription
                println("✅ [영어] Description 적용됨")
            } else {
                // ApiDescriptions.kt에서 매칭되는 키를 찾지 못한 경우
                println("⚠️ [영어] Description 번역 없음: $methodName")
            }
            
            operation
        }
    }



    private fun getKoreanDescription(): String {
        return """
            # ACS API - 안테나 제어 시스템
            
            ## 🚀 주요 기능
            - **시스템 설정 관리**: ConfigurationService를 통한 중앙 집중식 설정 관리
            - **위성 추적**: Ephemeris 기반 위성 궤도 계산 및 추적
            - **태양 추적**: 태양 위치 계산 및 태양 추적 모드
            - **통과 일정**: 위성 통과 일정 관리 및 최적화
            - **ICD 통신**: UDP 기반 외부 시스템과의 통신 프로토콜
            - **실시간 모니터링**: WebSocket을 통한 실시간 데이터 푸시
            - **성능 모니터링**: API 응답 시간, 메모리 사용량, 시스템 성능 추적
            
            ## 📚 API 그룹
            - **System**: 시스템 설정, 로깅, 모니터링, 성능 추적
            - **Mode**: 위성 추적, 태양 추적, 통과 일정 등 운영 모드
            - **ICD**: 외부 시스템과의 통신 프로토콜
            - **Data**: 실시간 데이터 및 WebSocket 푸시
            
            ## 📊 성능 모니터링
            - **응답 시간**: API 응답 시간 실시간 모니터링
            - **메모리 사용량**: 시스템 리소스 사용량 추적
            - **성능 메트릭**: 상세한 성능 지표 제공
            - **시스템 건강도**: 메모리, 스레드, CPU 상태 모니터링
            
            ## 🔧 사용법
            - **Swagger UI**: `/swagger-ui.html`에서 API 문서 확인
            - **OpenAPI Spec**: `/v3/api-docs/korean` (한국어) 또는 `/v3/api-docs/english` (영어)에서 JSON 형식 문서 다운로드
            - **언어 전환**: 우측 상단 언어 선택 드롭다운 사용
        """.trimIndent()
    }

    private fun getEnglishDescription(): String {
        return """
            # ACS API - Antenna Control System
            
            ## 🚀 Key Features
            - **System Configuration Management**: Centralized configuration management via ConfigurationService
            - **Satellite Tracking**: Ephemeris-based satellite orbit calculation and tracking
            - **Solar Tracking**: Solar position calculation and solar tracking mode
            - **Pass Scheduling**: Satellite pass schedule management and optimization
            - **ICD Communication**: UDP-based communication protocol with external systems
            - **Real-time Monitoring**: Real-time data push via WebSocket
            - **Performance Monitoring**: API response time, memory usage, system performance tracking
            
            ## 📚 API Groups
            - **System**: System configuration, logging, monitoring, performance tracking
            - **Mode**: Satellite tracking, solar tracking, pass scheduling and operational modes
            - **ICD**: Communication protocols with external systems
            - **Data**: Real-time data and WebSocket push
            
            ## 📊 Performance Monitoring
            - **Response Time**: Real-time API response time monitoring
            - **Memory Usage**: System resource usage tracking
            - **Performance Metrics**: Detailed performance indicators
            - **System Health**: Memory, thread, CPU status monitoring
            
            ## 🔧 Usage
            - **Swagger UI**: Check API documentation at `/swagger-ui.html`
            - **OpenAPI Spec**: Download JSON format document at `/v3/api-docs/korean` (Korean) or `/v3/api-docs/english` (English)
            - **Language Toggle**: Use language selection dropdown in top right
        """.trimIndent()
    }
} 