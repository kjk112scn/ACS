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
            })
            .addOperationCustomizer(englishOperationCustomizer())
            .build()
    }

    // API 설명을 한국어로 커스터마이징 (한국어 그룹용)
    @Bean
    fun koreanOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            // API 키 생성 (컨트롤러명.메서드명)
            val controllerName = handlerMethod.beanType.simpleName.lowercase().removeSuffix("controller")
            val methodName = handlerMethod.method.name
            
            // 더 정확한 API 키 매핑
            val apiKey = when {
                controllerName.contains("ephemeris") -> "ephemeris.$methodName"
                controllerName.contains("configuration") -> "configuration.$methodName"
                controllerName.contains("suntrack") -> "suntrack.$methodName"
                controllerName.contains("passschedule") -> "passschedule.$methodName"
                controllerName.contains("icd") -> "icd.$methodName"
                controllerName.contains("performance") -> "performance.$methodName"
                else -> "$controllerName.$methodName"
            }
            
            // 한국어 설명으로 업데이트 (기존 @Operation 어노테이션 덮어쓰기)
            val koreanSummary = ApiDescriptions.getKoreanDescription(apiKey, "summary")
            val koreanDescription = ApiDescriptions.getKoreanDescription(apiKey, "description")
            
            if (koreanSummary != apiKey) {
                operation.summary = koreanSummary
            }
            if (koreanDescription != apiKey) {
                operation.description = koreanDescription
            }
            
            operation
        }
    }

    // API 설명을 영어로 커스터마이징 (영어 그룹용)
    @Bean
    fun englishOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            // API 키 생성 (컨트롤러명.메서드명)
            val controllerName = handlerMethod.beanType.simpleName.lowercase().removeSuffix("controller")
            val methodName = handlerMethod.method.name
            
            // 더 정확한 API 키 매핑
            val apiKey = when {
                controllerName.contains("ephemeris") -> "ephemeris.$methodName"
                controllerName.contains("configuration") -> "configuration.$methodName"
                controllerName.contains("suntrack") -> "suntrack.$methodName"
                controllerName.contains("passschedule") -> "passschedule.$methodName"
                controllerName.contains("icd") -> "icd.$methodName"
                controllerName.contains("performance") -> "performance.$methodName"
                else -> "$controllerName.$methodName"
            }
            
            // 영어 설명으로 업데이트 (기존 @Operation 어노테이션 덮어쓰기)
            val englishSummary = ApiDescriptions.getEnglishDescription(apiKey, "summary")
            val englishDescription = ApiDescriptions.getEnglishDescription(apiKey, "description")
            
            if (englishSummary != apiKey) {
                operation.summary = englishSummary
            }
            if (englishDescription != apiKey) {
                operation.description = englishDescription
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