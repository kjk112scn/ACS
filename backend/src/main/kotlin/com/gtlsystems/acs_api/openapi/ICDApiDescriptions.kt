package com.gtlsystems.acs_api.openapi 

import io.swagger.v3.oas.models.Operation
import com.gtlsystems.acs_api.config.Language 

object ICDApiDescriptions { 
    fun applyDescriptions(operation: Operation, operationId: String, language: Language) { 
        when (operationId.lowercase()) {
            "servopresetcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "서보 프리셋 명령"
                        operation.description = """
                            <h4>안테나 서보 시스템의 프리셋 명령을 전송합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>각 축(방위각, 고도각, 기울기)의 프리셋 위치 설정</li>
                                <li>UDP 통신을 통한 외부 시스템 프리셋 제어</li>
                            </ul>

                            <h4>제어 파라미터:</h4>
                            <ul>
                                <li>azimuth: 방위각 축 프리셋 여부</li>
                                <li>elevation: 고도각 축 프리셋 여부</li>
                                <li>tilt: 기울기 축 프리셋 여부</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Servo Preset Command"
                        operation.description = """
                            <h4>Sends preset command to antenna servo system.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Preset position setting for each axis (Azimuth, Elevation, Tilt)</li>
                                <li>External system control via UDP communication</li>
                                <li>Axis status control using bitmap</li>
                            </ul>

                            <h4>Control Parameters:</h4>
                            <ul>
                                <li>azimuth: Azimuth axis preset status</li>
                                <li>elevation: Elevation axis preset status</li>
                                <li>tilt: Tilt axis preset status</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "standbycommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "대기(제동) 명령"
                        operation.description = """
                            <h4>안테나 서보 시스템을 안전한 제동 상태로 전환합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>각 축의 안전한 대기(제동) 위치 설정</li>
                                <li>시스템 보호 및 에너지 절약</li>
                                <li>축별 개별 대기(제동) 모드 전환</li>
                            </ul>

                            <h4>제어 파라미터:</h4>
                            <ul>
                                <li>azStandby: 방위각 축 대기(제동) 모드</li>
                                <li>elStandby: 고도각 축 대기(제동) 모드</li>
                                <li>tiStandby: 기울기 축 대기(제동) 모드</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Standby (Brake) Command"
                        operation.description = """
                            <h4>Transitions antenna servo system to a safe standby (brake) state.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Set safe standby (brake) position for each axis</li>
                                <li>System protection and energy saving</li>
                                <li>Individual axis standby (brake) mode transition</li>
                            </ul>

                            <h4>Control Parameters:</h4>
                            <ul>
                                <li>azStandby: Azimuth axis standby (brake) mode</li>
                                <li>elStandby: Elevation axis standby (brake) mode</li>
                                <li>tiStandby: Tilt axis standby (brake) mode</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "onemergencystopcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "비상 정지 명령"
                        operation.description = """
                            <h4>안테나 시스템의 즉각적인 안전 정지를 수행합니다.</h4>

                            <h4>명령 유형:</h4>
                            <ul>
                                <li>'E': 긴급 정지 (Emergency Stop)</li>
                                <li>'S': 긴급 정지 해제 (Safe)</li>
                            </ul>

                            <h4>긴급 정지 특징:</h4>
                            <ul>
                                <li>모든 동작 즉시 중단</li>
                                <li>안전 장치 즉각 작동</li>
                                <li>시스템 보호 최우선</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Emergency Stop Command"
                        operation.description = """
                            <h4>Performs immediate safe stop of the antenna system.</h4>

                            <h4>Command Types:</h4>
                            <ul>
                                <li>'E': Emergency Stop</li>
                                <li>'S': Safe</li>
                            </ul>

                            <h4>Emergency Stop Characteristics:</h4>
                            <ul>
                                <li>Immediate halt of all operations</li>
                                <li>Safety devices activated instantly</li>
                                <li>System protection as top priority</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "timeoffsetcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "시간 오프셋 명령"
                        operation.description = """
                            <h4>시스템의 시간 오프셋을 조정합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>시간 동기화 보정</li>
                                <li>정밀한 타임스탬프 조정</li>
                                <li>시스템 간 시간 일치</li>
                            </ul>

                            <h4>제어 파라미터:</h4>
                            <ul>
                                <li>inputTimeOffset: 시간 오프셋 값 (초 단위)</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Time Offset Command"
                        operation.description = """
                            <h4>Adjusts system time offset.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Time synchronization correction</li>
                                <li>Precise timestamp adjustment</li>
                                <li>Time alignment between systems</li>
                            </ul>

                            <h4>Control Parameters:</h4>
                            <ul>
                                <li>inputTimeOffset: Time offset value (in seconds)</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "multicontrolcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "다중 축 수동 제어 명령"
                        operation.description = """
                            <h4>다중 축에 대한 수동 제어 명령을 전송합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>각 축의 개별 각도 및 속도 제어</li>
                                <li>방위각, 고도각, 기울기 축 동시 제어</li>
                                <li>정밀한 위치 및 속도 조정</li>
                            </ul>

                            <h4>제어 파라미터:</h4>
                            <ul>
                                <li>azimuth: 방위각 축 제어 여부</li>
                                <li>elevation: 고도각 축 제어 여부</li>
                                <li>tilt: 기울기 축 제어 여부</li>
                                <li>각도 및 속도 값 설정</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Multi-Axis Manual Control Command"
                        operation.description = """
                            <h4>Sends manual control command for multiple axes.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Individual angle and speed control for each axis</li>
                                <li>Simultaneous control of azimuth, elevation, and tilt axes</li>
                                <li>Precise position and speed adjustment</li>
                            </ul>

                            <h4>Control Parameters:</h4>
                            <ul>
                                <li>azimuth: Azimuth axis control status</li>
                                <li>elevation: Elevation axis control status</li>
                                <li>tilt: Tilt axis control status</li>
                                <li>Angle and speed value settings</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "feedonoffcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "피드 온/오프 명령"
                        operation.description = """
                            <h4>안테나 피드 시스템의 전원을 제어합니다.</h4>

                            <h4>제어 대상:</h4>
                            <ul>
                                <li>S-밴드 LHCP/RHCP</li>
                                <li>X-밴드 LHCP/RHCP</li>
                                <li>RF 스위치</li>
                                <li>냉각 팬</li>
                            </ul>

                            <h4>기능:</h4>
                            <ul>
                                <li>개별 피드 전원 제어</li>
                                <li>통신 대역폭 선택</li>
                                <li>시스템 냉각 관리</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Feed On/Off Command"
                        operation.description = """
                            <h4>Controls power of antenna feed system.</h4>

                            <h4>Control Targets:</h4>
                            <ul>
                                <li>S-Band LHCP/RHCP</li>
                                <li>X-Band LHCP/RHCP</li>
                                <li>RF Switch</li>
                                <li>Cooling Fan</li>
                            </ul>

                            <h4>Features:</h4>
                            <ul>
                                <li>Individual feed power control</li>
                                <li>Communication bandwidth selection</li>
                                <li>System cooling management</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "positionoffsetcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "각도 오프셋 명령"
                        operation.description = """
                            <h4>안테나 각 축의 각도 오프셋을 조정합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>방위각, 고도각, 기울기 축 미세 조정</li>
                                <li>기계적 오차 보정</li>
                                <li>정밀 각도 제어</li>
                            </ul>

                            <h4>제어 파라미터:</h4>
                            <ul>
                                <li>azOffset: 방위각 축 오프셋 (도)</li>
                                <li>elOffset: 고도각 축 오프셋 (도)</li>
                                <li>tiOffset: 기울기 축 오프셋 (도)</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Position Offset Command"
                        operation.description = """
                            <h4>Adjusts position offset for each antenna axis.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Fine-tuning of azimuth, elevation, and tilt axes</li>
                                <li>Mechanical error correction</li>
                                <li>Precise position control</li>
                            </ul>

                            <h4>Control Parameters:</h4>
                            <ul>
                                <li>azOffset: Azimuth axis offset (degrees)</li>
                                <li>elOffset: Elevation axis offset (degrees)</li>
                                <li>tiOffset: Tilt axis offset (degrees)</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "stopcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "축 정지 명령"
                        operation.description = """
                            <h4>선택된 축의 동작을 즉시 중지합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>개별 축 정지</li>
                                <li>긴급 동작 중단</li>
                                <li>시스템 안전 보장</li>
                            </ul>

                            <h4>제어 파라미터:</h4>
                            <ul>
                                <li>azStop: 방위각 축 정지</li>
                                <li>elStop: 고도각 축 정지</li>
                                <li>tiStop: 기울기 축 정지</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Axis Stop Command"
                        operation.description = """
                            <h4>Immediately stops motion of selected axes.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Individual axis stopping</li>
                                <li>Emergency motion interruption</li>
                                <li>System safety assurance</li>
                            </ul>

                            <h4>Control Parameters:</h4>
                            <ul>
                                <li>azStop: Azimuth axis stop</li>
                                <li>elStop: Elevation axis stop</li>
                                <li>tiStop: Tilt axis stop</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "defaultinfocommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "기본 정보 명령"
                        operation.description = """
                            <h4>시스템의 기본 설정 정보를 반환합니다.</h4>

                            <h4>포함 정보:</h4>
                            <ul>
                                <li>현재 시간 오프셋</li>
                                <li>축별 위치 오프셋</li>
                                <li>시스템 기본 설정값</li>
                            </ul>

                            <h4>용도:</h4>
                            <ul>
                                <li>시스템 초기 상태 확인</li>
                                <li>설정 값 검증</li>
                                <li>디버깅 및 모니터링</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Default Information Command"
                        operation.description = """
                            <h4>Returns system's default configuration information.</h4>

                            <h4>Included Information:</h4>
                            <ul>
                                <li>Current time offset</li>
                                <li>Axis position offsets</li>
                                <li>System default settings</li>
                            </ul>

                            <h4>Purpose:</h4>
                            <ul>
                                <li>Verify initial system state</li>
                                <li>Validate configuration values</li>
                                <li>Debugging and monitoring</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "writentpcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "NTP 시간 동기화 명령"
                        operation.description = """
                            <h4>시스템의 NTP 시간을 동기화합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>정밀한 시간 동기화</li>
                                <li>네트워크 시간 프로토콜 적용</li>
                                <li>시스템 간 시간 일치</li>
                            </ul>

                            <h4>장점:</h4>
                            <ul>
                                <li>밀리초 단위 정확도</li>
                                <li>글로벌 시간 표준 준수</li>
                                <li>시스템 신뢰성 향상</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "NTP Time Synchronization Command"
                        operation.description = """
                            <h4>Synchronizes system time using NTP.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Precise time synchronization</li>
                                <li>Network Time Protocol implementation</li>
                                <li>Time alignment between systems</li>
                            </ul>

                            <h4>Benefits:</h4>
                            <ul>
                                <li>Millisecond-level accuracy</li>
                                <li>Global time standard compliance</li>
                                <li>Enhanced system reliability</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "stowcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "Stow 모드 명령"
                        operation.description = """
                            <h4>안테나 시스템을 안전한 Stow 위치로 이동합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>모든 축을 안전한 Stow 위치로 이동</li>
                                <li>시스템 보호 및 안전 상태 유지</li>
                                <li>장기 미사용 시 최적 상태 유지</li>
                            </ul>

                            <h4>안전 조치:</h4>
                            <ul>
                                <li>기계적 스트레스 최소화</li>
                                <li>환경 영향 방지</li>
                                <li>시스템 수명 연장</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Stow Mode Command"
                        operation.description = """
                            <h4>Moves antenna system to a safe stowed position.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Move all axes to safe stowed position</li>
                                <li>Maintain system protection and safety state</li>
                                <li>Preserve optimal condition during long-term non-use</li>
                            </ul>

                            <h4>Safety Measures:</h4>
                            <ul>
                                <li>Minimize mechanical stress</li>
                                <li>Prevent environmental impact</li>
                                <li>Extend system lifespan</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "communicationstatus" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "통신 상태 확인"
                        operation.description = """
                            <h4>외부 시스템과의 UDP 통신 상태를 모니터링합니다.</h4>

                            <h4>확인 항목:</h4>
                            <ul>
                                <li>통신 연결 상태</li>
                                <li>패킷 전송/수신 통계</li>
                                <li>지연 시간 및 성능 지표</li>
                            </ul>

                            <h4>제공 정보:</h4>
                            <ul>
                                <li>연결 건강성</li>
                                <li>패킷 성공률</li>
                                <li>평균 지연 시간</li>
                                <li>타임스탬프</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Communication Status Check"
                        operation.description = """
                            <h4>Monitors UDP communication status with external systems.</h4>

                            <h4>Checked Items:</h4>
                            <ul>
                                <li>Communication connection status</li>
                                <li>Packet transmission/reception statistics</li>
                                <li>Latency and performance indicators</li>
                            </ul>

                            <h4>Provided Information:</h4>
                            <ul>
                                <li>Connection health</li>
                                <li>Packet success rate</li>
                                <li>Average latency</li>
                                <li>Timestamp</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            "testcommand" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "테스트 명령"
                        operation.description = """
                            <h4>시스템 통신 및 응답성 테스트를 수행합니다.</h4>

                            <h4>목적:</h4>
                            <ul>
                                <li>기본 통신 경로 확인</li>
                                <li>시스템 응답 테스트</li>
                                <li>네트워크 연결 검증</li>
                            </ul>

                            <h4>사용 시나리오:</h4>
                            <ul>
                                <li>초기 시스템 진단</li>
                                <li>통신 문제 해결</li>
                                <li>연결 상태 모니터링</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Test Command"
                        operation.description = """
                            <h4>Performs system communication and responsiveness test.</h4>

                            <h4>Purpose:</h4>
                            <ul>
                                <li>Verify basic communication path</li>
                                <li>Test system response</li>
                                <li>Validate network connection</li>
                            </ul>

                            <h4>Usage Scenarios:</h4>
                            <ul>
                                <li>Initial system diagnostics</li>
                                <li>Troubleshoot communication issues</li>
                                <li>Monitor connection status</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
        }
    }
} 