package com.gtlsystems.acs_api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import com.gtlsystems.acs_api.openapi.OpenApiUtils
import com.gtlsystems.acs_api.config.Language

/**
 * 📚 OpenAPI 설정 - 체계적이고 단순한 다국어 API 문서 관리
 * 
 * ✨ 핵심 원칙:
 * 1. 한국어 우선 개발 → 영어 자동 번역
 * 2. 단순하고 직관적인 구조
 * 3. 확장 가능한 태그 순서 관리
 * 4. 중복 코드 최소화
 */
@Configuration
class OpenApiConfiguration {

    @Value("\${server.port:8080}")
    private val serverPort: Int = 8080

    // ================================================
    // 🏗️ 1. API 그룹 정의 (한국어 우선)
    // ================================================
    
    /**
     * 한국어 API 그룹 (기본/Primary)
     */
    @Bean
    @Primary
    fun koreanApiGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("1korean")
            .displayName("korean")
            .pathsToMatch("/**")
            .packagesToScan("com.gtlsystems.acs_api.controller")
            .addOpenApiCustomizer(createApiCustomizer(Language.KOREAN))
            .addOperationCustomizer(createOperationCustomizer(Language.KOREAN))
            .build()
    }

    /**
     * 영어 API 그룹 (번역본)
     */
    @Bean
    fun englishApiGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("2english")
            .displayName("english")
            .pathsToMatch("/**")
            .packagesToScan("com.gtlsystems.acs_api.controller")
            .addOpenApiCustomizer(createApiCustomizer(Language.ENGLISH))
            .addOperationCustomizer(createOperationCustomizer(Language.ENGLISH))
            .build()
    }

    // ================================================
    // 🎯 2. 언어별 커스터마이저 팩토리 (중복 제거)
    // ================================================

    private fun createApiCustomizer(language: Language): OpenApiCustomizer {
        return OpenApiCustomizer { openApi ->
            openApi.info(createApiInfo(language))
            openApi.servers(listOf(createServer(language)))
            openApi.tags(createOrderedTags(language))
        }
    }

    private fun createOperationCustomizer(language: Language): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            OpenApiUtils.applyApiDescriptions(operation, handlerMethod, language)
            operation
        }
    }

    // ================================================
    // 📖 3. API 정보 생성 (언어별)
    // ================================================

    private fun createApiInfo(language: Language): Info {
        return when (language) {
            Language.KOREAN -> Info()
                .title("ACS API - 안테나 제어 시스템")
                .version("1.0.0")
                .description(createKoreanDescription())
                .contact(Contact().name("GTL Systems").email("support@gtlsystems.com"))
                
            Language.ENGLISH -> Info()
                .title("ACS API - Antenna Control System")
                .version("1.0.0") 
                .description(createEnglishDescription())
                .contact(Contact().name("GTL Systems").email("support@gtlsystems.com"))
        }
    }

    private fun createServer(language: Language): Server {
        val description = if (language == Language.KOREAN) "로컬 개발 서버" else "Local Development Server"
        return Server().url("http://localhost:$serverPort").description(description)
    }

    // ================================================
    // 🏷️ 4. 태그 순서 관리 (체계적 순서)
    // ================================================

    private fun createOrderedTags(language: Language): List<Tag> {
        return apiTagOrder.map { tagInfo ->
            Tag().name(tagInfo.name).description(
                if (language == Language.KOREAN) tagInfo.koreanDesc else tagInfo.englishDesc
            )
        }
    }

    // ================================================
    // 📋 5. API 태그 순서 정의 (확장 가능)
    // ================================================

    private data class ApiTagInfo(
        val name: String,
        val koreanDesc: String,
        val englishDesc: String
    )

    /**
     * 🎯 API 태그 표시 순서 (새 API 추가시 여기에 추가)
     */
    private val apiTagOrder = listOf(
        ApiTagInfo("ICD - Communication", "외부 시스템과의 ICD 통신", "ICD Communication with External Systems"),
        ApiTagInfo("Mode - Ephemeris", "위성 궤도 추적", "Satellite Ephemeris Tracking"),
        ApiTagInfo("Mode - Pass Schedule", "패스 스케줄링", "Pass Schedule Management"),
        ApiTagInfo("Mode - Sun Track", "태양 추적", "Sun Tracking"),
        ApiTagInfo("System - Configuration", "시스템 설정 관리", "System Configuration Management"),
        ApiTagInfo("System - Performance", "시스템 성능 모니터링", "System Performance Monitoring"),
        ApiTagInfo("language-api-docs-controller", "다국어 API 문서", "Multilingual API Documentation"),
        ApiTagInfo("logging-controller", "로깅 관리", "Logging Management")
    )

    // ================================================
    // 📝 6. API 설명 생성 (마크다운)
    // ================================================

    private fun createKoreanDescription(): String = """
        # 🚀 ACS API 시스템

        ## 📡 주요 기능
        - **시스템 설정**: SettingsService를 통한 동적 설정 관리
        - **위성 추적**: Ephemeris 기반 위성 궤도 계산 및 추적
        - **패스 관리**: 위성 통과 스케줄 생성 및 관리  
        - **태양 추적**: 태양 위치 계산 및 추적
        - **실시간 통신**: WebSocket을 통한 실시간 데이터 전송
        - **성능 모니터링**: 시스템 성능 및 로깅 관리

        ## 🔗 빠른 링크
        - **Swagger UI**: `/swagger-ui` (현재 페이지)
        - **OpenAPI Spec**: `/v3/api-docs/korean`에서 JSON 형식 문서 다운로드
        - **개발 문서**: 프로젝트 `docs/` 폴더 참조

        ## 📋 사용법
        1. 하단 API 그룹을 클릭하여 각 기능별 API를 탐색하세요
        2. 각 API의 `Try it out` 버튼으로 실제 요청을 테스트할 수 있습니다
        3. 언어 변경은 우상단 드롭다운에서 가능합니다
    """.trimIndent()

    private fun createEnglishDescription(): String = """
        # 🚀 ACS API System

        ## 📡 Key Features
        - **System Configuration**: Dynamic configuration management via SettingsService
        - **Satellite Tracking**: Ephemeris-based satellite orbit calculation and tracking
        - **Pass Management**: Satellite pass schedule generation and management
        - **Sun Tracking**: Solar position calculation and tracking
        - **Real-time Communication**: Real-time data transmission via WebSocket
        - **Performance Monitoring**: System performance and logging management

        ## 🔗 Quick Links
        - **Swagger UI**: `/swagger-ui` (current page)
        - **OpenAPI Spec**: Download JSON format document at `/v3/api-docs/english`
        - **Development Docs**: Refer to project `docs/` folder

        ## 📋 Usage
        1. Click API groups below to explore APIs by functionality
        2. Use `Try it out` button for each API to test actual requests
        3. Language switching is available in the top-right dropdown
    """.trimIndent()
} 
