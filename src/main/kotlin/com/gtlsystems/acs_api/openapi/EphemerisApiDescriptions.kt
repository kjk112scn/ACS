package com.gtlsystems.acs_api.openapi 
 
import io.swagger.v3.oas.models.Operation
import com.gtlsystems.acs_api.config.Language 
 
object EphemerisApiDescriptions { 
    fun applyDescriptions(operation: Operation, operationId: String, language: Language) { 
        when (operationId.lowercase()) {
            "calculategeostationaryangles" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "정지궤도 위성 각도 계산"
                        operation.description = """
                            <h4>정지궤도 위성의 경도를 기반으로 안테나 추적 각도를 계산합니다.</h4>

                            <h4>계산되는 각도:</h4>
                            <ul>
                                <li>방위각: 북쪽 기준 수평각 (0° ~ 360°)</li>
                                <li>고도각: 지평선 기준 수직각 (0° ~ 90°)</li>
                                <li>기울기각: 편파면 일치를 위한 보정각</li>
                                <li>회전각: 안테나 회전축 보정각</li>
                            </ul>

                            <h4>좌표계 변환:</h4>
                            <ul>
                                <li>입력: 정지궤도 위성 경도</li>
                                <li>중간: ITRF/WGS84 좌표계</li>
                                <li>출력: 안테나 좌표계 (Az/El)</li>
                                <li>보정: 지구 자전/편평도 고려</li>
                            </ul>

                            <h4>정밀도:</h4>
                            <ul>
                                <li>각도 계산: 0.1° 이내</li>
                                <li>위치 오차: ±0.1°</li>
                                <li>갱신 주기: 1초</li>
                                <li>시간 동기: NTP 기반</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Calculate Geostationary Satellite Angles"
                        operation.description = """
                            <h4>Calculates antenna tracking angles based on geostationary satellite longitude.</h4>

                            <h4>Calculated Angles:</h4>
                            <ul>
                                <li>Azimuth: Horizontal angle from North (0° ~ 360°)</li>
                                <li>Elevation: Vertical angle from horizon (0° ~ 90°)</li>
                                <li>Tilt: Polarization plane alignment angle</li>
                                <li>Rotation: Antenna rotation axis correction</li>
                            </ul>

                            <h4>Coordinate Transformation:</h4>
                            <ul>
                                <li>Input: Geostationary satellite longitude</li>
                                <li>Intermediate: ITRF/WGS84 coordinates</li>
                                <li>Output: Antenna coordinates (Az/El)</li>
                                <li>Correction: Earth rotation/oblate consideration</li>
                            </ul>

                            <h4>Precision:</h4>
                            <ul>
                                <li>Angle calculation: Within 0.1°</li>
                                <li>Position error: ±0.1°</li>
                                <li>Update rate: 1 second</li>
                                <li>Time sync: NTP based</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
            
            "calculateephemerisangles" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "궤도 요소 기반 위성 추적 각도 계산"
                        operation.description = """
                            <h4>궤도 요소를 기반으로 위성 추적 각도를 계산합니다.</h4>

                            <h4>입력 데이터:</h4>
                            <ul>
                                <li>TLE 데이터: 위성 궤도 요소</li>
                                <li>시간: 계산 기준 시간 (UTC)</li>
                                <li>위치: 안테나 설치 위치 (위도/경도/고도)</li>
                            </ul>

                            <h4>계산 과정:</h4>
                            <ul>
                                <li>궤도 전파: SGP4/SDP4 모델 사용</li>
                                <li>좌표계 변환: ECI → ECEF → 로컬 수평면</li>
                                <li>각도 계산: 방위각/고도각/기울기각</li>
                                <li>보정: 대기 굴절/시차 효과 고려</li>
                            </ul>

                            <h4>출력 데이터:</h4>
                            <ul>
                                <li>방위각: 0° ~ 360°</li>
                                <li>고도각: -90° ~ 90°</li>
                                <li>기울기각: -180° ~ 180°</li>
                                <li>거리: 위성까지의 거리 (km)</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.description = """
                            <h4>Calculates satellite tracking angles based on orbital elements.</h4>

                            <h4>Input Data:</h4>
                            <ul>
                                <li>TLE Data: Satellite orbital elements</li>
                                <li>Time: Calculation reference time (UTC)</li>
                                <li>Location: Antenna installation position (Lat/Lon/Alt)</li>
                            </ul>

                            <h4>Calculation Process:</h4>
                            <ul>
                                <li>Orbit Propagation: Using SGP4/SDP4 model</li>
                                <li>Coordinate Transform: ECI → ECEF → Local Horizon</li>
                                <li>Angle Calculation: Azimuth/Elevation/Tilt</li>
                                <li>Correction: Atmospheric refraction/parallax effects</li>
                            </ul>

                            <h4>Output Data:</h4>
                            <ul>
                                <li>Azimuth: 0° ~ 360°</li>
                                <li>Elevation: -90° ~ 90°</li>
                                <li>Tilt: -180° ~ 180°</li>
                                <li>Range: Distance to satellite (km)</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "startephemeristrack" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.description = """
                            <h4>위성 추적을 시작하고 실시간 각도 계산을 수행합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>실시간 추적: 위성 위치 실시간 계산</li>
                                <li>각도 제어: 안테나 방향 자동 조정</li>
                                <li>속도 제어: 축별 이동 속도 최적화</li>
                                <li>상태 모니터링: 추적 상태 실시간 확인</li>
                            </ul>

                            <h4>제어 파라미터:</h4>
                            <ul>
                                <li>갱신 주기: 각도 계산 간격 (ms)</li>
                                <li>방위각 속도: 수평 회전 속도 (°/s)</li>
                                <li>고도각 속도: 수직 회전 속도 (°/s)</li>
                                <li>기울기각 속도: 편파면 회전 속도 (°/s)</li>
                            </ul>

                            <h4>안전 기능:</h4>
                            <ul>
                                <li>각도 제한: 기계적 한계 고려</li>
                                <li>속도 제한: 최대 이동 속도 제한</li>
                                <li>충돌 방지: 장애물 회피 로직</li>
                                <li>비상 정지: 이상 상황 감지 시 정지</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.description = """
                            <h4>Starts satellite tracking and performs real-time angle calculations.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Real-time Tracking: Live satellite position calculation</li>
                                <li>Angle Control: Automatic antenna direction adjustment</li>
                                <li>Speed Control: Axis movement speed optimization</li>
                                <li>Status Monitoring: Real-time tracking status check</li>
                            </ul>

                            <h4>Control Parameters:</h4>
                            <ul>
                                <li>Update Rate: Angle calculation interval (ms)</li>
                                <li>Azimuth Speed: Horizontal rotation speed (°/s)</li>
                                <li>Elevation Speed: Vertical rotation speed (°/s)</li>
                                <li>Tilt Speed: Polarization plane rotation speed (°/s)</li>
                            </ul>

                            <h4>Safety Features:</h4>
                            <ul>
                                <li>Angle Limits: Mechanical limits consideration</li>
                                <li>Speed Limits: Maximum movement speed restriction</li>
                                <li>Collision Avoidance: Obstacle avoidance logic</li>
                                <li>Emergency Stop: Stop on anomaly detection</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "stopephemeristrack" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.description = """
                            <h4>위성 추적을 중지하고 안테나를 안전 상태로 전환합니다.</h4>

                            <h4>수행 작업:</h4>
                            <ul>
                                <li>추적 중지: 실시간 각도 계산 중단</li>
                                <li>모터 정지: 모든 축 모터 정지</li>
                                <li>상태 저장: 현재 추적 상태 저장</li>
                                <li>이벤트 기록: 중지 이벤트 로깅</li>
                            </ul>

                            <h4>안전 조치:</h4>
                            <ul>
                                <li>감속 제어: 부드러운 정지 구현</li>
                                <li>위치 고정: 현재 위치 유지</li>
                                <li>상태 초기화: 추적 파라미터 초기화</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.description = """
                            <h4>Stops satellite tracking and transitions antenna to safe state.</h4>

                            <h4>Actions:</h4>
                            <ul>
                                <li>Track Stop: Stop real-time angle calculations</li>
                                <li>Motor Stop: Stop all axis motors</li>
                                <li>State Save: Save current tracking state</li>
                                <li>Event Log: Log stop event</li>
                            </ul>

                            <h4>Safety Measures:</h4>
                            <ul>
                                <li>Deceleration Control: Implement smooth stop</li>
                                <li>Position Lock: Maintain current position</li>
                                <li>State Reset: Reset tracking parameters</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "getephemerisstatus" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.description = """
                            <h4>현재 위성 추적 상태를 조회합니다.</h4>

                            <h4>상태 정보:</h4>
                            <ul>
                                <li>추적 상태: 활성/비활성/오류</li>
                                <li>현재 각도: 방위각/고도각/기울기각</li>
                                <li>목표 각도: 다음 이동 목표 각도</li>
                                <li>이동 속도: 각 축별 현재 속도</li>
                            </ul>

                            <h4>위성 정보:</h4>
                            <ul>
                                <li>위성 ID: 추적 중인 위성 식별자</li>
                                <li>궤도 정보: TLE 데이터 시간</li>
                                <li>예측 위치: 다음 위치 예측값</li>
                                <li>신호 강도: 수신 신호 레벨</li>
                            </ul>

                            <h4>시스템 상태:</h4>
                            <ul>
                                <li>모터 상태: 각 축 구동 상태</li>
                                <li>센서 값: 각도/속도 센서 값</li>
                                <li>오류 코드: 현재 발생 오류</li>
                                <li>동작 시간: 추적 지속 시간</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.description = """
                            <h4>Retrieves current satellite tracking status.</h4>

                            <h4>Status Information:</h4>
                            <ul>
                                <li>Track Status: Active/Inactive/Error</li>
                                <li>Current Angles: Azimuth/Elevation/Tilt</li>
                                <li>Target Angles: Next movement target angles</li>
                                <li>Movement Speed: Current speed per axis</li>
                            </ul>

                            <h4>Satellite Information:</h4>
                            <ul>
                                <li>Satellite ID: Tracked satellite identifier</li>
                                <li>Orbit Info: TLE data timestamp</li>
                                <li>Predicted Position: Next position prediction</li>
                                <li>Signal Strength: Received signal level</li>
                            </ul>

                            <h4>System Status:</h4>
                            <ul>
                                <li>Motor Status: Each axis drive status</li>
                                <li>Sensor Values: Angle/speed sensor values</li>
                                <li>Error Codes: Current error occurrences</li>
                                <li>Operation Time: Tracking duration</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            // ... 나머지 API 설명들도 동일한 패턴으로 추가 ...
        }
    }
} 