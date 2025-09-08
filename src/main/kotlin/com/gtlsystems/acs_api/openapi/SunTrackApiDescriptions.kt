package com.gtlsystems.acs_api.openapi

import io.swagger.v3.oas.models.Operation
import com.gtlsystems.acs_api.config.Language

object SunTrackApiDescriptions {
    fun applyDescriptions(operation: Operation, operationId: String, language: Language) {
        when (operationId.lowercase()) {
            "startsuntrack" -> {
        when (language) {
                                Language.KOREAN -> {
                        operation.summary = "태양 추적 시작"
                        operation.description = """
                            <h4>태양 추적을 시작하고 UDP 명령을 전송합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>태양 추적: 태양의 현재 위치를 계산하여 안테나 제어</li>
                                <li>UDP 명령: 외부 시스템에 태양 추적 시작 명령 전송</li>
                                <li>속도 제어: 각 축별 이동 속도 설정</li>
                                <li>실시간 추적: 태양 위치 변화에 따른 실시간 안테나 제어</li>
                            </ul>

                            <h4>입력 파라미터:</h4>
                            <ul>
                                <li>interval: 추적 간격 (밀리초 단위)</li>
                                <li>cmdAzimuthSpeed: 방위각 축 이동 속도 (도/초)</li>
                                <li>cmdElevationSpeed: 고도각 축 이동 속도 (도/초)</li>
                                <li>cmdTiltSpeed: 기울기 축 이동 속도 (도/초)</li>
                            </ul>

                            <h4>태양 추적 원리:</h4>
                            <ul>
                                <li>천문학적 계산: 태양의 적경/적위 계산</li>
                                <li>좌표 변환: 천구 좌표를 안테나 좌표로 변환</li>
                                <li>각도 계산: 방위각, 고도각, 기울기각 계산</li>
                                <li>제어 명령: 계산된 각도로 안테나 제어</li>
                            </ul>
                        """.trimIndent()
            }
                                Language.ENGLISH -> {
                        operation.summary = "Start Sun Tracking"
                        operation.description = """
                            <h4>Starts sun tracking and sends UDP commands.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Sun Tracking: Calculate current sun position for antenna control</li>
                                <li>UDP Command: Send sun tracking start command to external system</li>
                                <li>Speed Control: Set movement speed for each axis</li>
                                <li>Real-time Tracking: Real-time antenna control based on sun position changes</li>
                            </ul>

                            <h4>Input Parameters:</h4>
                            <ul>
                                <li>interval: Tracking interval (milliseconds)</li>
                                <li>cmdAzimuthSpeed: Azimuth axis movement speed (deg/sec)</li>
                                <li>cmdElevationSpeed: Elevation axis movement speed (deg/sec)</li>
                                <li>cmdTiltSpeed: Tilt axis movement speed (deg/sec)</li>
                            </ul>

                            <h4>Sun Tracking Principles:</h4>
                            <ul>
                                <li>Astronomical Calculation: Calculate sun's RA/Dec</li>
                                <li>Coordinate Transform: Convert celestial to antenna coordinates</li>
                                <li>Angle Calculation: Calculate azimuth, elevation, tilt angles</li>
                                <li>Control Command: Control antenna with calculated angles</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "stopsuntrack" -> {
        when (language) {
            Language.KOREAN -> {
                        operation.description = """
                            <h4>태양 추적을 중지하고 안테나를 안전 상태로 전환합니다.</h4>

                            <h4>수행 작업:</h4>
                            <ul>
                                <li>추적 중지: 태양 위치 계산 중단</li>
                                <li>모터 정지: 모든 축 모터 정지</li>
                                <li>상태 저장: 현재 추적 상태 저장</li>
                                <li>이벤트 기록: 중지 이벤트 로깅</li>
                            </ul>

                            <h4>안전 조치:</h4>
                            <ul>
                                <li>감속 제어: 부드러운 정지 구현</li>
                                <li>위치 고정: 현재 위치 유지</li>
                                <li>알람 해제: 모든 경고 상태 해제</li>
                                <li>상태 초기화: 추적 파라미터 초기화</li>
                            </ul>

                            <h4>후속 처리:</h4>
                            <ul>
                                <li>UDP 통신: 중지 명령 전송</li>
                                <li>리소스 해제: 시스템 자원 반환</li>
                                <li>상태 검증: 정상 중지 확인</li>
                                <li>보고서 생성: 추적 결과 요약</li>
                            </ul>
                        """.trimIndent()
            }
            Language.ENGLISH -> {
                        operation.description = """
                            <h4>Stops sun tracking and transitions antenna to safe state.</h4>

                            <h4>Actions:</h4>
                            <ul>
                                <li>Track Stop: Stop sun position calculation</li>
                                <li>Motor Stop: Stop all axis motors</li>
                                <li>State Save: Save current tracking state</li>
                                <li>Event Log: Log stop event</li>
                            </ul>

                            <h4>Safety Measures:</h4>
                            <ul>
                                <li>Deceleration Control: Implement smooth stop</li>
                                <li>Position Lock: Maintain current position</li>
                                <li>Alarm Clear: Clear all warning states</li>
                                <li>State Reset: Reset tracking parameters</li>
                            </ul>

                            <h4>Follow-up:</h4>
                            <ul>
                                <li>UDP Communication: Send stop command</li>
                                <li>Resource Release: Return system resources</li>
                                <li>State Verification: Confirm normal stop</li>
                                <li>Report Generation: Summarize tracking results</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
        }
    }
} 