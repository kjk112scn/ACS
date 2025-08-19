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
            // 상세한 API 메서드별 설명 (기존 내용 복원)
            when (operation.operationId?.lowercase()) {
                // === GET 요청 ===
                in listOf("get", "getall", "getlist", "getdata", "getstatus", "getinfo") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "데이터 조회"
                        operation.description = """
                            **📋 데이터 조회 API**
                            
                            **기능**: 요청된 정보를 조회하여 반환합니다.
                            **용도**: 
                            - 시스템 상태 확인
                            - 설정값 조회  
                            - 추적 데이터 확인
                            - 로그 정보 조회
                            
                            **특징**:
                            - 읽기 전용 작업
                            - 시스템 상태에 영향 없음
                            - 빠른 응답 시간
                        """.trimIndent()
                    } else {
                        operation.summary = "Get Data"
                        operation.description = """
                            **📋 Data Retrieval API**
                            
                            **Function**: Retrieves and returns requested information.
                            **Usage**: 
                            - Check system status
                            - Retrieve configuration values
                            - Check tracking data
                            - Retrieve log information
                            
                            **Features**:
                            - Read-only operations
                            - No impact on system state
                            - Fast response time
                        """.trimIndent()
                    }
                }
                
                // === POST 요청 ===
                in listOf("post", "create", "send", "submit", "upload") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "데이터 생성/전송"
                        operation.description = """
                            **📤 데이터 생성/전송 API**
                            
                            **기능**: 새로운 데이터를 생성하거나 서버로 전송합니다.
                            **용도**: 
                            - 새로운 설정 추가
                            - 파일 업로드
                            - 명령 전송
                            - 데이터 등록
                            
                            **주의사항**:
                            - 유효한 데이터 형식 필요
                            - 권한 확인 필수
                            - 중복 데이터 검증
                        """.trimIndent()
                    } else {
                        operation.summary = "Create/Send Data"
                        operation.description = """
                            **📤 Data Creation/Transmission API**
                            
                            **Function**: Creates new data or sends data to server.
                            **Usage**: 
                            - Add new configurations
                            - Upload files
                            - Send commands
                            - Register data
                            
                            **Notes**:
                            - Valid data format required
                            - Permission verification mandatory
                            - Duplicate data validation
                        """.trimIndent()
                    }
                }
                
                // === START 작업 ===
                in listOf("start", "begin", "initiate", "launch") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "시스템 시작"
                        operation.description = """
                            **🚀 시스템 시작 API**
                            
                            **기능**: 지정된 시스템 기능을 시작합니다.
                            **용도**: 
                            - 추적 모드 시작
                            - 서비스 활성화
                            - 프로세스 개시
                            - 모니터링 시작
                            
                            **작업 흐름**:
                            1. 시작 조건 검증
                            2. 리소스 할당
                            3. 프로세스 시작
                            4. 상태 확인
                        """.trimIndent()
                    } else {
                        operation.summary = "Start System"
                        operation.description = """
                            **🚀 System Start API**
                            
                            **Function**: Starts specified system functionality.
                            **Usage**: 
                            - Start tracking mode
                            - Activate services
                            - Initiate processes
                            - Begin monitoring
                            
                            **Workflow**:
                            1. Validate start conditions
                            2. Allocate resources
                            3. Start process
                            4. Verify status
                        """.trimIndent()
                    }
                }
                
                // === STOP 작업 ===
                in listOf("stop", "end", "terminate", "halt") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "시스템 정지"
                        operation.description = """
                            **🛑 시스템 정지 API**
                            
                            **기능**: 실행 중인 시스템 기능을 안전하게 정지합니다.
                            **용도**: 
                            - 추적 모드 중단
                            - 서비스 비활성화
                            - 프로세스 종료
                            - 모니터링 중단
                            
                            **안전 절차**:
                            1. 현재 작업 완료 대기
                            2. 리소스 정리
                            3. 안전한 종료
                            4. 상태 업데이트
                        """.trimIndent()
                    } else {
                        operation.summary = "Stop System"
                        operation.description = """
                            **🛑 System Stop API**
                            
                            **Function**: Safely stops running system functionality.
                            **Usage**: 
                            - Stop tracking mode
                            - Deactivate services
                            - Terminate processes
                            - Stop monitoring
                            
                            **Safety Procedures**:
                            1. Wait for current task completion
                            2. Clean up resources
                            3. Safe shutdown
                            4. Update status
                        """.trimIndent()
                    }
                }
                
                // === CLEAR 작업 ===
                in listOf("clear", "reset", "clean") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "데이터 초기화"
                        operation.description = """
                            **🧹 데이터 초기화 API**
                            
                            **기능**: 지정된 데이터를 초기화하거나 삭제합니다.
                            **용도**: 
                            - 로그 파일 정리
                            - 캐시 초기화
                            - 임시 데이터 삭제
                            - 설정 리셋
                            
                            **주의사항**:
                            - 되돌릴 수 없는 작업
                            - 백업 권장
                            - 관리자 권한 필요
                        """.trimIndent()
                    } else {
                        operation.summary = "Clear Data"
                        operation.description = """
                            **🧹 Data Clear API**
                            
                            **Function**: Initializes or deletes specified data.
                            **Usage**: 
                            - Clean log files
                            - Reset cache
                            - Delete temporary data
                            - Reset configurations
                            
                            **Caution**:
                            - Irreversible operation
                            - Backup recommended
                            - Administrator privileges required
                        """.trimIndent()
                    }
                }
                
                // === EXPORT 작업 ===
                in listOf("export", "download", "backup") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "데이터 내보내기"
                        operation.description = """
                            **📥 데이터 내보내기 API**
                            
                            **기능**: 시스템 데이터를 파일로 내보냅니다.
                            **용도**: 
                            - 로그 파일 다운로드
                            - 설정 백업
                            - 추적 데이터 내보내기
                            - 보고서 생성
                            
                            **지원 형식**:
                            - CSV 파일
                            - JSON 형식
                            - 압축 아카이브
                        """.trimIndent()
                    } else {
                        operation.summary = "Export Data"
                        operation.description = """
                            **📥 Data Export API**
                            
                            **Function**: Exports system data to files.
                            **Usage**: 
                            - Download log files
                            - Backup configurations
                            - Export tracking data
                            - Generate reports
                            
                            **Supported Formats**:
                            - CSV files
                            - JSON format
                            - Compressed archives
                        """.trimIndent()
                    }
                }
                
                // === CALCULATE 작업 ===
                in listOf("calculate", "compute", "process") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "데이터 계산"
                        operation.description = """
                            **🧮 데이터 계산 API**
                            
                            **기능**: 복잡한 계산을 수행하고 결과를 반환합니다.
                            **용도**: 
                            - 위성 궤도 계산
                            - 태양 위치 계산
                            - 각도 변환
                            - 경로 계산
                            
                            **계산 특징**:
                            - 고정밀 알고리즘
                            - 실시간 처리
                            - 다중 좌표계 지원
                        """.trimIndent()
                    } else {
                        operation.summary = "Calculate Data"
                        operation.description = """
                            **🧮 Data Calculation API**
                            
                            **Function**: Performs complex calculations and returns results.
                            **Usage**: 
                            - Satellite orbit calculation
                            - Solar position calculation
                            - Angle transformation
                            - Path calculation
                            
                            **Calculation Features**:
                            - High-precision algorithms
                            - Real-time processing
                            - Multiple coordinate system support
                        """.trimIndent()
                    }
                }
                
                // === GENERATE 작업 ===
                in listOf("generate", "create", "build") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "데이터 생성"
                        operation.description = """
                            **⚙️ 데이터 생성 API**
                            
                            **기능**: 새로운 데이터나 파일을 생성합니다.
                            **용도**: 
                            - 추적 경로 생성
                            - 스케줄 생성
                            - 보고서 작성
                            - 설정 파일 생성
                            
                            **생성 과정**:
                            1. 입력 데이터 검증
                            2. 알고리즘 적용
                            3. 결과 생성
                            4. 품질 검증
                        """.trimIndent()
                    } else {
                        operation.summary = "Generate Data"
                        operation.description = """
                            **⚙️ Data Generation API**
                            
                            **Function**: Generates new data or files.
                            **Usage**: 
                            - Generate tracking paths
                            - Create schedules
                            - Generate reports
                            - Create configuration files
                            
                            **Generation Process**:
                            1. Validate input data
                            2. Apply algorithms
                            3. Generate results
                            4. Quality verification
                        """.trimIndent()
                    }
                }
                
                // === SET 작업 ===
                in listOf("set", "update", "modify", "change") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "설정 변경"
                        operation.description = """
                            **⚙️ 설정 변경 API**
                            
                            **기능**: 시스템 설정을 변경합니다.
                            **용도**: 
                            - 시스템 파라미터 조정
                            - 사용자 설정 변경
                            - 운영 모드 전환
                            - 임계값 설정
                            
                            **변경 절차**:
                            1. 현재 설정 백업
                            2. 새 설정 검증
                            3. 설정 적용
                            4. 변경 사항 로깅
                        """.trimIndent()
                    } else {
                        operation.summary = "Change Settings"
                        operation.description = """
                            **⚙️ Settings Change API**
                            
                            **Function**: Changes system settings.
                            **Usage**: 
                            - Adjust system parameters
                            - Change user settings
                            - Switch operation modes
                            - Set thresholds
                            
                            **Change Procedure**:
                            1. Backup current settings
                            2. Validate new settings
                            3. Apply settings
                            4. Log changes
                        """.trimIndent()
                    }
                }
                
                // === TIME 관련 ===
                in listOf("time", "schedule", "timer") -> {
                    if (language == Language.KOREAN) {
                        operation.summary = "시간 관리"
                        operation.description = """
                            **⏰ 시간 관리 API**
                            
                            **기능**: 시간 관련 작업을 처리합니다.
                            **용도**: 
                            - 시간 동기화
                            - 스케줄 관리
                            - 타이머 설정
                            - 시간대 변환
                            
                            **시간 처리**:
                            - UTC 기준 시간
                            - 고정밀 타임스탬프
                            - 자동 시간 보정
                        """.trimIndent()
                    } else {
                        operation.summary = "Time Management"
                        operation.description = """
                            **⏰ Time Management API**
                            
                            **Function**: Handles time-related operations.
                            **Usage**: 
                            - Time synchronization
                            - Schedule management
                            - Timer settings
                            - Timezone conversion
                            
                            **Time Processing**:
                            - UTC-based time
                            - High-precision timestamps
                            - Automatic time correction
                        """.trimIndent()
                    }
                }
            }
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
        - **시스템 설정**: ConfigurationService를 통한 동적 설정 관리
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
        - **System Configuration**: Dynamic configuration management via ConfigurationService
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

    // ================================================
    // 🌍 7. 언어 열거형 (타입 안전성)
    // ================================================

    private enum class Language {
        KOREAN, ENGLISH
    }
} 