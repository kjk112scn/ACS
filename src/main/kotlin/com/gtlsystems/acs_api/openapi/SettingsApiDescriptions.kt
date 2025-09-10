package com.gtlsystems.acs_api.openapi

import io.swagger.v3.oas.models.Operation
import com.gtlsystems.acs_api.config.Language

object SettingsApiDescriptions {
    fun applyDescriptions(operation: Operation, operationId: String, language: Language) {
        when (operationId.lowercase()) {
            "getlocation" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "위치 설정 조회"
                        operation.description = """
                            <h4>현재 설정된 안테나 위치 정보를 조회합니다.</h4>

                            <h4>조회되는 위치 정보:</h4>
                            <ul>
                                <li>위도: 지구상의 북위/남위 좌표 (-90° ~ 90°)</li>
                                <li>경도: 지구상의 동경/서경 좌표 (-180° ~ 180°)</li>
                                <li>고도: 해수면 기준 상대 높이 (미터)</li>
                            </ul>

                            <h4>용도:</h4>
                            <ul>
                                <li>위성 추적 계산: 위성 위치 계산의 기준점</li>
                                <li>안테나 제어: 정확한 각도 계산을 위한 위치 정보</li>
                                <li>시스템 설정: 초기 설정값 확인 및 검증</li>
                            </ul>

                            <h4>기본값:</h4>
                            <ul>
                                <li>위도: 35.317540° (한국 표준 위치)</li>
                                <li>경도: 128.608510° (한국 표준 위치)</li>
                                <li>고도: 0.0m (해수면 기준)</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Get Location Settings"
                        operation.description = """
                            <h4>Retrieves current antenna location settings.</h4>

                            <h4>Location Information:</h4>
                            <ul>
                                <li>Latitude: North/South coordinate on Earth (-90° ~ 90°)</li>
                                <li>Longitude: East/West coordinate on Earth (-180° ~ 180°)</li>
                                <li>Altitude: Relative height above sea level (meters)</li>
                            </ul>

                            <h4>Purpose:</h4>
                            <ul>
                                <li>Satellite Tracking: Reference point for satellite position calculation</li>
                                <li>Antenna Control: Location information for accurate angle calculation</li>
                                <li>System Settings: Initial setting value confirmation and validation</li>
                            </ul>

                            <h4>Default Values:</h4>
                            <ul>
                                <li>Latitude: 35.317540° (Korean standard location)</li>
                                <li>Longitude: 128.608510° (Korean standard location)</li>
                                <li>Altitude: 0.0m (sea level reference)</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "setlocation" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "위치 설정 변경"
                        operation.description = """
                            <h4>안테나 위치 정보를 설정합니다.</h4>

                            <h4>설정 가능한 위치 정보:</h4>
                            <ul>
                                <li>위도: -90° ~ 90° 범위 내 유효한 값</li>
                                <li>경도: -180° ~ 180° 범위 내 유효한 값</li>
                                <li>고도: 0m 이상의 유효한 값</li>
                            </ul>

                            <h4>변경 시 자동 처리:</h4>
                            <ul>
                                <li>DB 저장: 설정값을 데이터베이스에 영구 저장</li>
                                <li>메모리 갱신: 실시간 접근을 위한 메모리 캐시 업데이트</li>
                                <li>이벤트 발행: 위치 변경 이벤트를 시스템에 알림</li>
                                <li>로그 기록: 변경 이력을 시스템 로그에 기록</li>
                            </ul>

                            <h4>영향 범위:</h4>
                            <ul>
                                <li>위성 추적: 새로운 위치 기준으로 추적 각도 재계산</li>
                                <li>태양 추적: 태양 위치 계산 기준점 변경</li>
                                <li>통과 스케줄: 위성 통과 시간 재계산</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Set Location Settings"
                        operation.description = """
                            <h4>Configures antenna location information.</h4>

                            <h4>Configurable Location Information:</h4>
                            <ul>
                                <li>Latitude: Valid value within -90° ~ 90° range</li>
                                <li>Longitude: Valid value within -180° ~ 180° range</li>
                                <li>Altitude: Valid value of 0m or higher</li>
                            </ul>

                            <h4>Automatic Processing on Change:</h4>
                            <ul>
                                <li>DB Storage: Permanently save setting values to database</li>
                                <li>Memory Update: Update memory cache for real-time access</li>
                                <li>Event Publishing: Notify system of location change event</li>
                                <li>Log Recording: Record change history in system logs</li>
                            </ul>

                            <h4>Impact Scope:</h4>
                            <ul>
                                <li>Satellite Tracking: Recalculate tracking angles based on new location</li>
                                <li>Sun Tracking: Change reference point for sun position calculation</li>
                                <li>Pass Schedule: Recalculate satellite pass times</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "gettracking" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "추적 설정 조회"
                        operation.description = """
                            <h4>현재 설정된 위성 추적 관련 설정을 조회합니다.</h4>

                            <h4>조회되는 추적 설정:</h4>
                            <ul>
                                <li>갱신 간격: 추적 데이터 계산 주기 (밀리초)</li>
                                <li>추적 기간: 추적 데이터 생성 기간 (일)</li>
                                <li>최소 고도각: 추적 시작 최소 고도각 (도)</li>
                            </ul>

                            <h4>설정별 용도:</h4>
                            <ul>
                                <li>갱신 간격: 실시간 추적 데이터 생성 주기 제어</li>
                                <li>추적 기간: 미래 추적 데이터 예측 기간 설정</li>
                                <li>최소 고도각: 지평선 근처 노이즈 필터링</li>
                            </ul>

                            <h4>기본값:</h4>
                            <ul>
                                <li>갱신 간격: 1000ms (1초)</li>
                                <li>추적 기간: 7일</li>
                                <li>최소 고도각: 10.0°</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Get Tracking Settings"
                        operation.description = """
                            <h4>Retrieves current satellite tracking related settings.</h4>

                            <h4>Tracking Settings:</h4>
                            <ul>
                                <li>Update Interval: Tracking data calculation period (milliseconds)</li>
                                <li>Tracking Duration: Tracking data generation period (days)</li>
                                <li>Minimum Elevation: Minimum elevation angle for tracking start (degrees)</li>
                            </ul>

                            <h4>Purpose by Setting:</h4>
                            <ul>
                                <li>Update Interval: Control real-time tracking data generation cycle</li>
                                <li>Tracking Duration: Set future tracking data prediction period</li>
                                <li>Minimum Elevation: Filter noise near horizon</li>
                            </ul>

                            <h4>Default Values:</h4>
                            <ul>
                                <li>Update Interval: 1000ms (1 second)</li>
                                <li>Tracking Duration: 7 days</li>
                                <li>Minimum Elevation: 10.0°</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "settracking" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "추적 설정 변경"
                        operation.description = """
                            <h4>위성 추적 관련 설정을 변경합니다.</h4>

                            <h4>설정 가능한 추적 파라미터:</h4>
                            <ul>
                                <li>갱신 간격: 100ms ~ 10000ms 범위 (권장: 1000ms)</li>
                                <li>추적 기간: 1일 ~ 30일 범위 (권장: 7일)</li>
                                <li>최소 고도각: 0° ~ 45° 범위 (권장: 10°)</li>
                            </ul>

                            <h4>성능 고려사항:</h4>
                            <ul>
                                <li>갱신 간격 단축: CPU 사용량 증가, 정밀도 향상</li>
                                <li>추적 기간 연장: 메모리 사용량 증가, 예측 정확도 향상</li>
                                <li>최소 고도각 조정: 추적 시작 조건 변경</li>
                            </ul>

                            <h4>변경 시 자동 처리:</h4>
                            <ul>
                                <li>실시간 적용: 현재 추적 중인 경우 즉시 적용</li>
                                <li>DB 저장: 설정값 영구 저장</li>
                                <li>이벤트 발행: 추적 설정 변경 이벤트 알림</li>
                                <li>로그 기록: 변경 이력 기록</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Set Tracking Settings"
                        operation.description = """
                            <h4>Modifies satellite tracking related settings.</h4>

                            <h4>Configurable Tracking Parameters:</h4>
                            <ul>
                                <li>Update Interval: 100ms ~ 10000ms range (recommended: 1000ms)</li>
                                <li>Tracking Duration: 1 day ~ 30 days range (recommended: 7 days)</li>
                                <li>Minimum Elevation: 0° ~ 45° range (recommended: 10°)</li>
                            </ul>

                            <h4>Performance Considerations:</h4>
                            <ul>
                                <li>Shorter Update Interval: Increased CPU usage, improved precision</li>
                                <li>Extended Tracking Duration: Increased memory usage, improved prediction accuracy</li>
                                <li>Minimum Elevation Adjustment: Change tracking start conditions</li>
                            </ul>

                            <h4>Automatic Processing on Change:</h4>
                            <ul>
                                <li>Real-time Application: Apply immediately if currently tracking</li>
                                <li>DB Storage: Permanently save setting values</li>
                                <li>Event Publishing: Notify tracking setting change event</li>
                                <li>Log Recording: Record change history</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "getallsettings" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "전체 설정 조회"
                        operation.description = """
                            <h4>시스템의 모든 설정값을 한 번에 조회합니다.</h4>

                            <h4>조회되는 설정 카테고리:</h4>
                            <ul>
                                <li>위치 설정: 위도, 경도, 고도</li>
                                <li>추적 설정: 갱신 간격, 추적 기간, 최소 고도각</li>
                                <li>시스템 설정: 기타 운영 관련 설정</li>
                            </ul>

                            <h4>용도:</h4>
                            <ul>
                                <li>설정 백업: 현재 설정값 전체 백업</li>
                                <li>설정 검증: 모든 설정값 일괄 확인</li>
                                <li>시스템 모니터링: 설정 상태 실시간 모니터링</li>
                                <li>디버깅: 설정 관련 문제 진단</li>
                            </ul>

                            <h4>응답 형식:</h4>
                            <ul>
                                <li>JSON 형태: 키-값 쌍으로 구성</li>
                                <li>실시간 값: 현재 메모리에 저장된 최신 값</li>
                                <li>타입 정보: 각 설정값의 데이터 타입 포함</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Get All Settings"
                        operation.description = """
                            <h4>Retrieves all system settings at once.</h4>

                            <h4>Setting Categories:</h4>
                            <ul>
                                <li>Location Settings: Latitude, Longitude, Altitude</li>
                                <li>Tracking Settings: Update Interval, Tracking Duration, Minimum Elevation</li>
                                <li>System Settings: Other operation-related settings</li>
                            </ul>

                            <h4>Purpose:</h4>
                            <ul>
                                <li>Setting Backup: Full backup of current setting values</li>
                                <li>Setting Validation: Batch confirmation of all setting values</li>
                                <li>System Monitoring: Real-time monitoring of setting status</li>
                                <li>Debugging: Diagnosis of setting-related issues</li>
                            </ul>

                            <h4>Response Format:</h4>
                            <ul>
                                <li>JSON Format: Key-value pairs</li>
                                <li>Real-time Values: Latest values stored in current memory</li>
                                <li>Type Information: Data type of each setting value included</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
        }
    }
}