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
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Calculate Geostationary Satellite Angles"
                        operation.description = """
                            <h4>Calculates antenna tracking angles based on geostationary satellite longitude.</h4>
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
                        operation.summary = "PassId 기반 위성 추적 시작."
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
                        operation.summary = "위성 추적 중지."
                        operation.description = """
                            <h4>위성 추적을 중지합니다.</h4>

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

            "getephemerisdata" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "실시간 위성 추적 데이터 조회"
                        operation.description = """
                            <h4>현재 추적 중인 위성의 실시간 데이터를 조회합니다.</h4>

                            <h4>조회 데이터:</h4>
                            <ul>
                                <li>실시간 각도: 방위각/고도각/기울기각</li>
                                <li>위성 정보: 위성 ID/이름/궤도 데이터</li>
                                <li>추적 상태: 현재 추적 상태 및 진행률</li>
                                <li>통계 정보: 추적 성능 및 오차 분석</li>
                                <li>추적한 데이터가 계속 축적됨 이를 조회하여 csv로 다운하고있음.</li>
                            </ul>

                            <h4>데이터 형식:</h4>
                            <ul>
                                <li>JSON 형식: 구조화된 데이터 제공</li>
                                <li>실시간 업데이트: 1초 간격 갱신</li>
                                <li>타임스탬프: UTC 기준 시간 정보</li>
                                <li>오차 분석: 각도별 정확도 정보</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Get Real-time Satellite Tracking Data"
                        operation.description = """
                            <h4>Retrieves real-time data for currently tracked satellite.</h4>

                            <h4>Retrieved Data:</h4>
                            <ul>
                                <li>Real-time Angles: Azimuth/Elevation/Tilt</li>
                                <li>Satellite Info: Satellite ID/Name/Orbital data</li>
                                <li>Tracking Status: Current tracking status and progress</li>
                                <li>Statistics: Tracking performance and error analysis</li>
                                <li>Accumulated tracking data is continuously stored and can be downloaded as CSV.</li>
                            </ul>

                            <h4>Data Format:</h4>
                            <ul>
                                <li>JSON Format: Structured data provision</li>
                                <li>Real-time Update: 1-second interval refresh</li>
                                <li>Timestamp: UTC-based time information</li>
                                <li>Error Analysis: Accuracy information per angle</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "generateephemeristrack" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "위성 추적 데이터 생성"
                        operation.description = """
                            <h4>TLE 데이터를 기반으로 위성 추적 데이터를 생성합니다.</h4>

                            <h4>생성 과정:</h4>
                            <ul>
                                <li>TLE 파싱: 위성 궤도 요소 추출</li>
                                <li>각도 계산: 방위각/고도각/기울기각</li>
                            </ul>

                            <h4>출력 데이터:</h4>
                            <ul>
                                <li>마스터 데이터: 추적 목록 정보</li>
                                <li>상세 데이터: 시간별 각도 정보</li>
                                <li>통계 정보: 생성된 데이터 개수</li>
                                <li>오류 처리: 생성 실패 시 오류 메시지</li>
                                <li>실제 위성 추적 데이터를 생성함.</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Generate Satellite Tracking Data"
                        operation.description = """
                            <h4>Generates satellite tracking data based on TLE data.</h4>

                            <h4>Generation Process:</h4>
                            <ul>
                                <li>TLE Parsing: Extract satellite orbital elements</li>
                                <li>Angle Calculation: Azimuth/Elevation/Tilt calculation</li>
                            </ul>

                            <h4>Output Data:</h4>
                            <ul>
                                <li>Master Data: Tracking list information</li>
                                <li>Detail Data: Time-based angle information</li>
                                <li>Statistics: Generated data count</li>
                                <li>Error Handling: Error message on failure</li>
                                <li>Generates actual satellite tracking data for real-time tracking.</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "getephemerislist" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "위성 추적 데이터 목록 조회"
                        operation.description = """
                            <h4>생성된 위성 추적 데이터 목록을 조회합니다.</h4>

                            <h4>조회 정보:</h4>
                            <ul>
                                <li>위성 정보: 위성 ID/이름/궤도 타입</li>
                                <li>시간 정보: 시작/종료 시간/지속 시간</li>
                                <li>각도 정보: 최대 고도/시작/종료 각도</li>
                                <li>상태 정보: 생성 상태/데이터 품질</li>
                                <li>위성 추적 데이터를 생성하면 실제 위성 추적 목록을 일괄 조회함.</li>
                            </ul>

                            <h4>정렬 및 필터링:</h4>
                            <ul>
                                <li>시간순 정렬: 최신 데이터 우선</li>
                                <li>위성별 그룹화: 동일 위성 데이터 묶음</li>
                                <li>상태별 필터링: 활성/비활성 데이터 구분</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Get Satellite Tracking Data List"
                        operation.description = """
                            <h4>Retrieves list of generated satellite tracking data.</h4>

                            <h4>Retrieved Information:</h4>
                            <ul>
                                <li>Satellite Info: Satellite ID/Name/Orbit type</li>
                                <li>Time Info: Start/End time/Duration</li>
                                <li>Angle Info: Maximum elevation/Start/End angles</li>
                                <li>Status Info: Generation status/Data quality</li>
                                <li>Retrieves all satellite tracking data lists when tracking data is generated.</li>
                            </ul>

                            <h4>Sorting & Filtering:</h4>
                            <ul>
                                <li>Time Sort: Latest data first</li>
                                <li>Satellite Grouping: Same satellite data grouped</li>
                                <li>Status Filtering: Active/Inactive data separation</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "getephemerisdetail" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "위성 추적 데이터 상세 조회"
                        operation.description = """
                            <h4>선택된 위성 추적 데이터의 상세 정보를 조회합니다.</h4>

                            <h4>상세 데이터:</h4>
                            <ul>
                                <li>시간별 각도: 방위각/고도각/기울기각</li>
                                <li>위성 위치: 거리/고도/속도 정보</li>
                                <li>추적 경로: 연속적인 궤적 데이터</li>
                                <li>오차 분석: 이론값 대비 실제값 오차</li>
                                <li>선택된 위성의 상세 추적 값을 조회함.</li>
                            </ul>

                            <h4>데이터 활용:</h4>
                            <ul>
                                <li>차트 표시: 궤적 시각화</li>
                                <li>추적 제어: 실시간 각도 제어</li>
                                <li>성능 분석: 추적 정확도 평가</li>
                                <li>CSV 내보내기: 데이터 분석용</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Get Satellite Tracking Data Detail"
                        operation.description = """
                            <h4>Retrieves detailed information for selected satellite tracking data.</h4>

                            <h4>Detail Data:</h4>
                            <ul>
                                <li>Time-based Angles: Azimuth/Elevation/Tilt</li>
                                <li>Satellite Position: Range/Altitude/Velocity information</li>
                                <li>Tracking Path: Continuous trajectory data</li>
                                <li>Error Analysis: Theoretical vs actual value error</li>
                                <li>Retrieves detailed tracking values for the selected satellite.</li>
                            </ul>

                            <h4>Data Usage:</h4>
                            <ul>
                                <li>Chart Display: Trajectory visualization</li>
                                <li>Tracking Control: Real-time angle control</li>
                                <li>Performance Analysis: Tracking accuracy evaluation</li>
                                <li>CSV Export: For data analysis</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "calculateaxistransform" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "3축 변환 계산"
                        operation.description = """
                            <h4>입력된 각도들을 3축 변환하여 안테나 좌표계로 변환합니다.</h4>

                            <h4>입력 파라미터:</h4>
                            <ul>
                                <li>Azimuth: 방위각 (0° ~ 360°)</li>
                                <li>Elevation: 고도각 (0° ~ 90°)</li>
                                <li>Tilt: 기울기각 (-90° ~ 90°)</li>
                                <li>Rotator: 회전각 (0° ~ 360°)</li>
                                <li>현재는 계산기로 활용하기 위함으로 개발함.</li>
                            </ul>

                            <h4>변환 과정:</h4>
                            <ul>
                                <li>좌표계 변환: 입력 좌표 → 안테나 좌표</li>
                                <li>기울기 보정: 편파면 일치를 위한 보정</li>
                                <li>회전 보정: 안테나 회전축 보정</li>
                                <li>각도 제한: 기계적 한계 고려</li>
                            </ul>

                            <h4>출력 결과:</h4>
                            <ul>
                                <li>변환된 방위각: 최종 방위각 값</li>
                                <li>변환된 고도각: 최종 고도각 값</li>
                                <li>변환 성공 여부: 변환 결과 상태</li>
                                <li>오류 메시지: 변환 실패 시 원인</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Calculate 3-Axis Transform"
                        operation.description = """
                            <h4>Transforms input angles using 3-axis transformation to antenna coordinates.</h4>

                            <h4>Input Parameters:</h4>
                            <ul>
                                <li>Azimuth: Azimuth angle (0° ~ 360°)</li>
                                <li>Elevation: Elevation angle (0° ~ 90°)</li>
                                <li>Tilt: Tilt angle (-90° ~ 90°)</li>
                                <li>Rotator: Rotation angle (0° ~ 360°)</li>
                                <li>Currently developed for use as a calculator tool.</li>
                            </ul>

                            <h4>Transformation Process:</h4>
                            <ul>
                                <li>Coordinate Transform: Input coordinates → Antenna coordinates</li>
                                <li>Tilt Correction: Polarization plane alignment correction</li>
                                <li>Rotation Correction: Antenna rotation axis correction</li>
                                <li>Angle Limits: Mechanical limits consideration</li>
                            </ul>

                            <h4>Output Results:</h4>
                            <ul>
                                <li>Transformed Azimuth: Final azimuth value</li>
                                <li>Transformed Elevation: Final elevation value</li>
                                <li>Transformation Success: Result status</li>
                                <li>Error Message: Failure cause if any</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "setcurrenttrackingpassid" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "현재 추적 ID 설정"
                        operation.description = """
                            <h4>현재 추적 중인 위성의 Pass ID를 설정합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>추적 ID 설정: 현재 추적할 위성 ID 지정</li>
                                <li>상태 업데이트: 추적 상태 정보 갱신</li>
                                <li>데이터 연동: 해당 ID의 추적 데이터 활성화</li>
                            </ul>

                            <h4>파라미터:</h4>
                            <ul>
                                <li>passId: 추적할 위성의 Pass ID (UInt)</li>
                            </ul>

                            <h4>응답:</h4>
                            <ul>
                                <li>성공: 설정 완료 메시지</li>
                                <li>실패: 오류 메시지 및 원인</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Set Current Tracking Pass ID"
                        operation.description = """
                            <h4>Sets the Pass ID for currently tracked satellite.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Pass ID Setting: Specify satellite ID to track</li>
                                <li>Status Update: Update tracking status information</li>
                                <li>Data Integration: Activate tracking data for the ID</li>
                            </ul>

                            <h4>Parameters:</h4>
                            <ul>
                                <li>passId: Pass ID of satellite to track (UInt)</li>
                            </ul>

                            <h4>Response:</h4>
                            <ul>
                                <li>Success: Setting completion message</li>
                                <li>Failure: Error message and cause</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }

            "startgeostationarytracking" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "정지궤도 위성 추적 시작"
                        operation.description = """
                            <h4>정지궤도 위성의 추적을 시작합니다.</h4>

                            <h4>기능:</h4>
                            <ul>
                                <li>정지궤도 추적: 지구와 동기화된 위성 추적</li>
                                <li>실시간 각도 계산: 방위각/고도각/기울기각</li>
                                <li>자동 제어: 안테나 방향 자동 조정</li>
                                <li>상태 모니터링: 추적 상태 실시간 확인</li>
                            </ul>

                            <h4>입력 데이터:</h4>
                            <ul>
                                <li>TLE Line 1: 위성 궤도 요소 1</li>
                                <li>TLE Line 2: 위성 궤도 요소 2</li>
                            </ul>

                            <h4>응답:</h4>
                            <ul>
                                <li>성공: 추적 시작 메시지 및 위성 ID</li>
                                <li>실패: 오류 메시지 및 원인</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Start Geostationary Satellite Tracking"
                        operation.description = """
                            <h4>Starts tracking of geostationary satellite.</h4>

                            <h4>Features:</h4>
                            <ul>
                                <li>Geostationary Tracking: Earth-synchronized satellite tracking</li>
                                <li>Real-time Angle Calculation: Azimuth/Elevation/Tilt angles</li>
                                <li>Automatic Control: Automatic antenna direction adjustment</li>
                                <li>Status Monitoring: Real-time tracking status check</li>
                            </ul>

                            <h4>Input Data:</h4>
                            <ul>
                                <li>TLE Line 1: Satellite orbital elements 1</li>
                                <li>TLE Line 2: Satellite orbital elements 2</li>
                            </ul>

                            <h4>Response:</h4>
                            <ul>
                                <li>Success: Tracking start message and satellite ID</li>
                                <li>Failure: Error message and cause</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
        }
    }
} 