package com.gtlsystems.acs_api.util

/**
 * API 엔드포인트의 다국어 설명을 관리하는 객체
 */
object ApiDescriptions {
    
    // Ephemeris 관련 API 설명
    val EPHEMERIS_DESCRIPTIONS = mapOf(
        "tracking.stop" to mapOf(
            "ko" to mapOf(
                "summary" to "위성 추적 중지",
                "description" to """
                    현재 진행 중인 위성 추적을 중지합니다.
                    
                    ## 추적 중지 과정
                    1. **추적 데이터 전송 중단**: 실시간 데이터 전송 중단
                    2. **시스템 상태 변경**: 추적 모드에서 대기 모드로 전환
                    3. **리소스 정리**: 추적 관련 메모리 및 연결 정리
                    
                    ## 응답 데이터
                    - **message**: 추적 중지 결과 메시지
                    - **status**: 시스템 상태 (stopped)
                    
                    ## 사용 예시
                    `POST /api/ephemeris/tracking/stop`
                    
                    ## 응답 예시
                    ```json
                    {
                      "message": "위성 추적이 중지되었습니다.",
                      "status": "stopped"
                    }
                    ```
                """.trimIndent()
            ),
            "en" to mapOf(
                "summary" to "Stop Satellite Tracking",
                "description" to """
                    Stop ongoing satellite tracking.
                    
                    ## Tracking Stop Process
                    1. **Stop tracking data transmission**: Discontinue real-time data transmission
                    2. **Change system status**: Switch from tracking mode to standby mode
                    3. **Resource cleanup**: Clean up tracking-related memory and connections
                    
                    ## Response Data
                    - **message**: Tracking stop result message
                    - **status**: System status (stopped)
                    
                    ## Usage Example
                    `POST /api/ephemeris/tracking/stop`
                    
                    ## Response Example
                    ```json
                    {
                      "message": "Satellite tracking has been stopped.",
                      "status": "stopped"
                    }
                    ```
                """.trimIndent()
            )
        ),
        "tracking.start" to mapOf(
            "ko" to mapOf(
                "summary" to "위성 추적 시작",
                "description" to "지정된 통과 ID로 위성 추적을 시작합니다."
            ),
            "en" to mapOf(
                "summary" to "Start Satellite Tracking",
                "description" to "Start satellite tracking with the specified pass ID."
            )
        ),
        "tracking.schedule" to mapOf(
            "ko" to mapOf(
                "summary" to "위성 추적 스케줄 생성",
                "description" to "위성 통과 일정을 기반으로 추적 스케줄을 생성합니다."
            ),
            "en" to mapOf(
                "summary" to "Generate Satellite Tracking Schedule",
                "description" to "Generate tracking schedule based on satellite pass schedule."
            )
        ),
        "tracking.generate" to mapOf(
            "ko" to mapOf(
                "summary" to "위성 궤도 추적 데이터 생성",
                "description" to "TLE 데이터를 기반으로 위성 궤도 추적 데이터를 생성합니다."
            ),
            "en" to mapOf(
                "summary" to "Generate Satellite Orbit Tracking Data",
                "description" to "Generate satellite orbit tracking data based on TLE data."
            )
        ),
        "timeOffsetCommand" to mapOf(
            "ko" to mapOf(
                "summary" to "시간 오프셋 명령",
                "description" to "시스템 시간 오프셋을 설정하는 명령을 처리합니다."
            ),
            "en" to mapOf(
                "summary" to "Time Offset Command",
                "description" to "Process command to set system time offset."
            )
        ),
        "setCurrentTrackingPassId" to mapOf(
            "ko" to mapOf(
                "summary" to "현재 추적 통과 ID 설정",
                "description" to "현재 추적 중인 위성의 통과 ID를 설정합니다."
            ),
            "en" to mapOf(
                "summary" to "Set Current Tracking Pass ID",
                "description" to "Set the pass ID of the currently tracking satellite."
            )
        ),
        "realtimeData.clear" to mapOf(
            "ko" to mapOf(
                "summary" to "실시간 추적 데이터 초기화",
                "description" to "실시간 추적 데이터를 초기화합니다."
            ),
            "en" to mapOf(
                "summary" to "Clear Real-time Tracking Data",
                "description" to "Clear real-time tracking data."
            )
        )
    )
    
    // Configuration 관련 API 설명
    val CONFIGURATION_DESCRIPTIONS = mapOf(
        "getAllConfigurations" to mapOf(
            "ko" to "모든 설정 조회",
            "en" to "Get All Configurations"
        ),
        "getConfiguration" to mapOf(
            "ko" to "특정 설정 조회",
            "en" to "Get Specific Configuration"
        ),
        "updateConfiguration" to mapOf(
            "ko" to "설정 업데이트",
            "en" to "Update Configuration"
        ),
        "deleteConfiguration" to mapOf(
            "ko" to "설정 삭제",
            "en" to "Delete Configuration"
        )
    )
    
    // SunTrack 관련 API 설명
    val SUNTRACK_DESCRIPTIONS = mapOf(
        "startTracking" to mapOf(
            "ko" to "태양 추적 시작",
            "en" to "Start Sun Tracking"
        ),
        "stopTracking" to mapOf(
            "ko" to "태양 추적 중지",
            "en" to "Stop Sun Tracking"
        ),
        "getTrackingData" to mapOf(
            "ko" to "태양 추적 데이터 조회",
            "en" to "Get Sun Tracking Data"
        )
    )
    
    // PassSchedule 관련 API 설명
    val PASS_SCHEDULE_DESCRIPTIONS = mapOf(
        "addTle" to mapOf(
            "ko" to "TLE 데이터 추가",
            "en" to "Add TLE Data"
        ),
        "generateTrackingData" to mapOf(
            "ko" to "추적 데이터 생성",
            "en" to "Generate Tracking Data"
        ),
        "getPassSchedules" to mapOf(
            "ko" to "통과 일정 조회",
            "en" to "Get Pass Schedules"
        )
    )
    
    // ICD 관련 API 설명
    val ICD_DESCRIPTIONS = mapOf(
        "sendCommand" to mapOf(
            "ko" to "명령 전송",
            "en" to "Send Command"
        ),
        "getStatus" to mapOf(
            "ko" to "상태 조회",
            "en" to "Get Status"
        ),
        "getCommunicationStatus" to mapOf(
            "ko" to "통신 상태 확인",
            "en" to "Check Communication Status"
        )
    )
    
    // Performance 관련 API 설명
    val PERFORMANCE_DESCRIPTIONS = mapOf(
        "getResponseTime" to mapOf(
            "ko" to "응답 시간 조회",
            "en" to "Get Response Time"
        ),
        "getMemoryUsage" to mapOf(
            "ko" to "메모리 사용량 조회",
            "en" to "Get Memory Usage"
        ),
        "getSystemHealth" to mapOf(
            "ko" to "시스템 건강도 조회",
            "en" to "Get System Health"
        )
    )
    
    /**
     * API 키에 해당하는 언어별 설명을 반환
     */
    fun getDescription(apiKey: String, language: String, type: String = "summary"): String {
        val descriptions = when {
            apiKey.startsWith("ephemeris.") -> EPHEMERIS_DESCRIPTIONS
            apiKey.startsWith("configuration.") -> CONFIGURATION_DESCRIPTIONS
            apiKey.startsWith("suntrack.") -> SUNTRACK_DESCRIPTIONS
            apiKey.startsWith("passschedule.") -> PASS_SCHEDULE_DESCRIPTIONS
            apiKey.startsWith("icd.") -> ICD_DESCRIPTIONS
            apiKey.startsWith("performance.") -> PERFORMANCE_DESCRIPTIONS
            else -> emptyMap()
        }
        
        val apiDescriptions = descriptions[apiKey.removePrefix("${apiKey.split(".").first()}.")]
        return when (val langDesc = apiDescriptions?.get(language)) {
            is Map<*, *> -> (langDesc[type] as? String) ?: apiKey
            is String -> langDesc
            else -> apiKey
        }
    }
    
    /**
     * 한국어 설명 반환
     */
    fun getKoreanDescription(apiKey: String, type: String = "summary"): String = getDescription(apiKey, "ko", type)
    
    /**
     * 영어 설명 반환
     */
    fun getEnglishDescription(apiKey: String, type: String = "summary"): String = getDescription(apiKey, "en", type)
} 