package com.gtlsystems.acs_api.openapi

import io.swagger.v3.oas.models.Operation
import com.gtlsystems.acs_api.config.Language

object PassScheduleApiDescriptions {
    fun applyDescriptions(operation: Operation, operationId: String, language: Language) {
        when (operationId.lowercase()) {
            "addtle" -> {
        when (language) {
                                Language.KOREAN -> {
                        operation.summary = "TLE 데이터 추가"
                        operation.description = """
                            <h4>새로운 위성의 TLE 데이터를 추가합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>TLE 데이터 검증</li>
                                <li>위성 정보 등록</li>
                                <li>궤도 요소 계산</li>
                                <li>데이터베이스 저장</li>
                            </ul>

                            <h4>입력 형식:</h4>
                            <ul>
                                <li>TLE 라인 0: 위성명</li>
                                <li>TLE 라인 1: 첫 번째 궤도 요소</li>
                                <li>TLE 라인 2: 두 번째 궤도 요소</li>
                                <li>형식: NORAD 표준</li>
                            </ul>

                            <h4>검증 항목:</h4>
                            <ul>
                                <li>TLE 형식 검증</li>
                                <li>체크섬 확인</li>
                                <li>날짜 유효성 검사</li>
                                <li>궤도 요소 범위 확인</li>
                            </ul>
                        """.trimIndent()
            }
                                Language.ENGLISH -> {
                        operation.summary = "Add TLE Data"
                        operation.description = """
                            <h4>Adds new satellite TLE data.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>TLE data validation</li>
                                <li>Satellite information registration</li>
                                <li>Orbital elements calculation</li>
                                <li>Database storage</li>
                            </ul>

                            <h4>Input Format:</h4>
                            <ul>
                                <li>TLE Line 0: Satellite name</li>
                                <li>TLE Line 1: First orbital elements</li>
                                <li>TLE Line 2: Second orbital elements</li>
                                <li>Format: NORAD standard</li>
                            </ul>

                            <h4>Validation Items:</h4>
                            <ul>
                                <li>TLE format validation</li>
                                <li>Checksum verification</li>
                                <li>Date validity check</li>
                                <li>Orbital elements range check</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "generateallpassscheduletracking" -> {
        when (language) {
            Language.KOREAN -> {
                        operation.description = """
                            <h4>모든 위성의 통과 스케줄을 생성합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>궤도 전파: 모든 위성의 궤도 계산</li>
                                <li>통과 예측: 가시성 분석 및 통과 시간 계산</li>
                                <li>스케줄 생성: 통과 이벤트 스케줄링</li>
                                <li>우선순위 할당: 위성별 우선순위 적용</li>
                            </ul>

                            <h4>계산 항목:</h4>
                            <ul>
                                <li>통과 시간: 시작/종료/최대 고도 시간</li>
                                <li>통과 각도: 방위각/고도각 프로파일</li>
                                <li>신호 품질: 예상 신호 세기 계산</li>
                                <li>충돌 검사: 스케줄 충돌 확인</li>
                            </ul>

                            <h4>출력 데이터:</h4>
                            <ul>
                                <li>통과 목록: 모든 예상 통과 이벤트</li>
                                <li>상세 정보: 각 통과별 상세 정보</li>
                                <li>통계 정보: 통과 빈도 및 분포</li>
                                <li>충돌 정보: 중첩된 통과 정보</li>
                            </ul>
                        """.trimIndent()
            }
            Language.ENGLISH -> {
                        operation.description = """
                            <h4>Generates pass schedules for all satellites.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Orbit Propagation: Calculate all satellite orbits</li>
                                <li>Pass Prediction: Visibility analysis and pass time calculation</li>
                                <li>Schedule Generation: Pass event scheduling</li>
                                <li>Priority Assignment: Apply satellite priorities</li>
                            </ul>

                            <h4>Calculation Items:</h4>
                            <ul>
                                <li>Pass Times: Start/End/Max elevation times</li>
                                <li>Pass Angles: Azimuth/Elevation profiles</li>
                                <li>Signal Quality: Expected signal strength calculation</li>
                                <li>Conflict Check: Schedule conflict verification</li>
                            </ul>

                            <h4>Output Data:</h4>
                            <ul>
                                <li>Pass List: All predicted pass events</li>
                                <li>Detailed Info: Per-pass detailed information</li>
                                <li>Statistics: Pass frequency and distribution</li>
                                <li>Conflict Info: Overlapping pass information</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "startpassscheduletracking" -> {
        when (language) {
            Language.KOREAN -> {
                        operation.description = """
                            <h4>선택된 통과 스케줄의 추적을 시작합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>스케줄 활성화: 선택된 통과 스케줄 시작</li>
                                <li>자동 추적: 실시간 위성 추적 시작</li>
                                <li>모니터링: 추적 상태 실시간 감시</li>
                                <li>이벤트 처리: 통과 이벤트 자동 처리</li>
                            </ul>

                            <h4>제어 항목:</h4>
                            <ul>
                                <li>추적 모드: 자동/수동 모드 설정</li>
                                <li>속도 제어: 안테나 이동 속도 설정</li>
                                <li>정밀도: 추적 정밀도 설정</li>
                                <li>타이밍: 시작/종료 시간 제어</li>
                            </ul>

                            <h4>안전 기능:</h4>
                            <ul>
                                <li>충돌 방지: 기계적 제한 확인</li>
                                <li>우선순위: 긴급 상황 처리</li>
                                <li>백업: 추적 데이터 저장</li>
                                <li>복구: 오류 시 자동 복구</li>
                            </ul>
                        """.trimIndent()
            }
            Language.ENGLISH -> {
                        operation.description = """
                            <h4>Starts tracking of selected pass schedule.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Schedule Activation: Start selected pass schedule</li>
                                <li>Auto Tracking: Begin real-time satellite tracking</li>
                                <li>Monitoring: Real-time tracking status monitoring</li>
                                <li>Event Handling: Automatic pass event processing</li>
                            </ul>

                            <h4>Control Items:</h4>
                            <ul>
                                <li>Track Mode: Auto/Manual mode setting</li>
                                <li>Speed Control: Antenna movement speed setting</li>
                                <li>Precision: Tracking precision setting</li>
                                <li>Timing: Start/End time control</li>
                            </ul>

                            <h4>Safety Features:</h4>
                            <ul>
                                <li>Collision Prevention: Mechanical limit check</li>
                                <li>Priority: Emergency situation handling</li>
                                <li>Backup: Tracking data storage</li>
                                <li>Recovery: Automatic error recovery</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "stoppassscheduletracking" -> {
        when (language) {
            Language.KOREAN -> {
                        operation.description = """
                            <h4>현재 실행 중인 통과 스케줄 추적을 중지합니다.</h4>

                            <h4>수행 작업:</h4>
                            <ul>
                                <li>추적 중지: 위성 추적 즉시 중단</li>
                                <li>안테나 정지: 안전한 위치로 이동</li>
                                <li>데이터 저장: 추적 이력 저장</li>
                                <li>상태 초기화: 시스템 상태 리셋</li>
                            </ul>

                            <h4>안전 조치:</h4>
                            <ul>
                                <li>감속 제어: 부드러운 정지</li>
                                <li>알람 해제: 경고 상태 초기화</li>
                                <li>로그 기록: 중지 이벤트 기록</li>
                                <li>상태 보고: 중지 상태 통보</li>
                            </ul>

                            <h4>후속 처리:</h4>
                            <ul>
                                <li>스케줄 조정: 다음 통과 준비</li>
                                <li>리소스 해제: 시스템 자원 반환</li>
                                <li>상태 검증: 정상 중지 확인</li>
                                <li>보고서 생성: 추적 결과 요약</li>
                            </ul>
                        """.trimIndent()
            }
            Language.ENGLISH -> {
                        operation.description = """
                            <h4>Stops currently running pass schedule tracking.</h4>

                            <h4>Actions:</h4>
                            <ul>
                                <li>Track Stop: Immediate satellite tracking stop</li>
                                <li>Antenna Stop: Move to safe position</li>
                                <li>Data Save: Save tracking history</li>
                                <li>State Reset: Reset system state</li>
                            </ul>

                            <h4>Safety Measures:</h4>
                            <ul>
                                <li>Deceleration Control: Smooth stop</li>
                                <li>Alarm Clear: Reset warning states</li>
                                <li>Log Record: Record stop event</li>
                                <li>Status Report: Notify stop status</li>
                            </ul>

                            <h4>Follow-up:</h4>
                            <ul>
                                <li>Schedule Adjustment: Prepare next pass</li>
                                <li>Resource Release: Return system resources</li>
                                <li>State Verification: Confirm normal stop</li>
                                <li>Report Generation: Summarize tracking results</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "getpassschedulemonitorstatus" -> {
        when (language) {
            Language.KOREAN -> {
                        operation.description = """
                            <h4>현재 통과 스케줄 모니터링 상태를 조회합니다.</h4>

                            <h4>상태 정보:</h4>
                            <ul>
                                <li>추적 상태: 활성/대기/오류</li>
                                <li>현재 통과: 진행 중인 통과 정보</li>
                                <li>다음 통과: 예정된 다음 통과</li>
                                <li>시스템 상태: 하드웨어/소프트웨어 상태</li>
                            </ul>

                            <h4>모니터링 항목:</h4>
                            <ul>
                                <li>안테나 상태: 현재 위치/속도</li>
                                <li>신호 상태: 수신 신호 강도</li>
                                <li>추적 품질: 추적 정확도 지표</li>
                                <li>리소스: 시스템 자원 사용량</li>
                            </ul>

                            <h4>통계 정보:</h4>
                            <ul>
                                <li>성공률: 통과 추적 성공률</li>
                                <li>오류율: 시스템 오류 발생률</li>
                                <li>가동률: 시스템 가동 시간</li>
                                <li>성능: 추적 성능 지표</li>
                            </ul>
                        """.trimIndent()
            }
            Language.ENGLISH -> {
                        operation.description = """
                            <h4>Retrieves current pass schedule monitoring status.</h4>

                            <h4>Status Information:</h4>
                            <ul>
                                <li>Track Status: Active/Standby/Error</li>
                                <li>Current Pass: Ongoing pass information</li>
                                <li>Next Pass: Scheduled next pass</li>
                                <li>System Status: Hardware/Software status</li>
                            </ul>

                            <h4>Monitoring Items:</h4>
                            <ul>
                                <li>Antenna Status: Current position/speed</li>
                                <li>Signal Status: Received signal strength</li>
                                <li>Track Quality: Tracking accuracy metrics</li>
                                <li>Resources: System resource usage</li>
                            </ul>

                            <h4>Statistics:</h4>
                            <ul>
                                <li>Success Rate: Pass tracking success rate</li>
                                <li>Error Rate: System error occurrence rate</li>
                                <li>Uptime: System operation time</li>
                                <li>Performance: Tracking performance metrics</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            // ... 나머지 API 설명들도 동일한 패턴으로 추가 ...
        }
    }
} 