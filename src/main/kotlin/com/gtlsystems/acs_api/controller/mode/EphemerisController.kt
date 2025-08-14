package com.gtlsystems.acs_api.controller.mode

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import com.gtlsystems.acs_api.service.mode.EphemerisService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ✅ 추가 필요한 import들
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/ephemeris")
@Tag(name = "Mode - Ephemeris", description = "위성 궤도 추적 API - TLE 기반 위성 위치 계산, 추적 경로 생성, 가시성 분석")
class EphemerisController(
    private val orekitCalculator: OrekitCalculator,
    private val ephemerisService: EphemerisService
)
{
    private val logger = LoggerFactory.getLogger(EphemerisController::class.java)

    @PostMapping("/3axis/tracking/geostationary/start")
    @Operation(
        tags = ["Mode - Ephemeris"]
    )
    fun startGeostationaryTracking(
        @Parameter(
            description = "정지궤도 위성 추적 요청 데이터",
            required = true
        )
        @RequestBody request: GeostationaryTrackingRequest
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                ephemerisService.startGeostationaryTracking(request.tleLine1, request.tleLine2)
                
                mapOf(
                    "message" to "정지궤도 위성 추적이 시작되었습니다.",
                    "satelliteId" to request.tleLine1.substring(2, 7).trim(),
                    "trackingType" to "geostationary"
                )
            } catch (e: Exception) {
                mapOf(
                    "message" to "정지궤도 위성 추적 시작 실패: ${e.message}",
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }

    @PostMapping("/3axis/tracking/geostationary/calculate-angles")
    @Operation(
        summary = "정지궤도 위성 각도 계산",
        description = """
            정지궤도 위성의 현재 위치에 대한 3축 각도를 계산합니다.
            
            ## 계산되는 각도
            - **방위각 (Azimuth)**: 북쪽을 기준으로 한 수평 각도 (0°~360°)
            - **고도각 (Elevation)**: 지평선을 기준으로 한 수직 각도 (0°~90°)
            - **기울기각 (Tilt)**: 안테나 기울기 보정 각도
            - **회전각 (Rotator)**: 안테나 회전 보정 각도
            
            ## 3축 변환
            - **원본 각도**: Orekit 계산 결과
            - **변환된 각도**: 안테나 시스템에 맞게 보정된 각도
            - **기울기 보정**: 지구 자전축 기울기 보정
            - **회전 보정**: 안테나 회전축 보정
            
            ## 응답 예시
            ```json
            {
              "azimuth": 180.5,
              "elevation": 45.2,
              "tiltAngle": -6.98,
              "rotatorAngle": 0.0
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun calculateGeostationaryAngles(
        @Parameter(
            description = "정지궤도 위성 각도 계산 요청 데이터",
            required = true
        )
        @RequestBody request: GeostationaryTrackingRequest
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val geoPosition = ephemerisService.getCurrentGeostationaryPositionWith3AxisTransform(
                    request.tleLine1, 
                    request.tleLine2
                )
                
                mapOf(
                    "message" to "정지궤도 각도 계산 완료",
                    "satelliteId" to request.tleLine1.substring(2, 7).trim(),
                    "azimuth" to (geoPosition["transformedAzimuth"] as Double),
                    "elevation" to (geoPosition["transformedElevation"] as Double),
                    "originalAzimuth" to (geoPosition["originalAzimuth"] as Double),
                    "originalElevation" to (geoPosition["originalElevation"] as Double),
                    "tiltAngle" to (geoPosition["tiltAngle"] as Double),
                    "rotatorAngle" to (geoPosition["rotatorAngle"] as Double),
                    "trackingType" to "geostationary"
                )
            } catch (e: Exception) {
                mapOf(
                    "message" to "정지궤도 각도 계산 실패: ${e.message}",
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }

    /**
     * 실시간 추적 데이터 조회 (JSON)
     */
    @GetMapping("/tracking/realtime-data")
    @Operation(
        summary = "실시간 추적 데이터 조회",
        description = """
            현재 추적 중인 모든 위성의 실시간 데이터를 조회합니다.
            
            ## 제공 데이터
            - **위성 정보**: 위성 ID, 이름, TLE 데이터
            - **위치 정보**: 현재 위도, 경도, 고도
            - **각도 정보**: 방위각, 고도각, 기울기각
            - **추적 상태**: 추적 시작 시간, 마지막 업데이트 시간
            
            ## 통계 정보
            - **totalCount**: 현재 추적 중인 위성 수
            - **data**: 각 위성별 상세 추적 데이터
            - **statistics**: 추적 성능 통계
            
            ## 사용 예시
            ```
            GET /api/ephemeris/tracking/realtime-data
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getRealtimeTrackingData(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val realtimeData = ephemerisService.getRealtimeTrackingData()
            val stats = ephemerisService.getRealtimeTrackingStats()

            mapOf(
                "totalCount" to realtimeData.size,
                "data" to realtimeData,
                "statistics" to stats
            )
        }
    }
    /**
     * 실시간 추적 데이터 초기화
     */
    @PostMapping("/realtime-data/clear")
    @Operation(
        summary = "실시간 추적 데이터 초기화",
        description = """
            모든 실시간 추적 데이터를 초기화합니다.
            
            ## 초기화 대상
            - **위성 추적 데이터**: 현재 추적 중인 모든 위성 정보
            - **통계 데이터**: 추적 성능 통계 정보
            - **임시 데이터**: 메모리에 저장된 임시 추적 데이터
            
            ## 주의사항
            - **데이터 손실**: 모든 추적 데이터가 영구적으로 삭제됩니다
            - **추적 중단**: 초기화 후 새로운 추적을 시작해야 합니다
            - **백업 권장**: 중요한 추적 데이터는 초기화 전 백업을 권장합니다
            
            ## 사용 예시
            ```
            POST /api/ephemeris/realtime-data/clear
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun clearRealtimeTrackingData(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            ephemerisService.clearRealtimeTrackingData()
            mapOf(
                "message" to "실시간 추적 데이터가 초기화되었습니다",
                "status" to "cleared"
            )
        }
    }

    @PostMapping("/set-current-tracking-pass-id")
    fun setCurrentTrackingPassId(@RequestParam passId: UInt?): ResponseEntity<Map<String, String>> {
        return try {
            ephemerisService.setCurrentTrackingPassId(passId)
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "setCurrentTrackingPassId 명령이 성공적으로 전송되었습니다",
                "command" to "setCurrentTrackingPassId",
                "passId" to passId.toString()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "setCurrentTrackingPassId 명령 전송 실패: ${e.message}"
            ))
        }
    }
    @PostMapping("/time-offset-command")
    fun timeOffsetCommand(@RequestParam inputTimeOffset: Float): ResponseEntity<Map<String, String>> {
        return try {
            ephemerisService.ephemerisTimeOffsetCommand(inputTimeOffset)
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "TimeOffset 명령이 성공적으로 전송되었습니다",
                "command" to "TimeOffset",
                "timeOffset" to inputTimeOffset.toString()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "TimeOffset 명령 전송 실패: ${e.message}"
            ))
        }
    }
    /**
     * 현재 시간의 위성 위치를 계산합니다.
     */
    @PostMapping("/position/current")
    @Operation(
        summary = "현재 위성 위치 계산",
        description = """
            TLE 데이터를 기반으로 현재 시간의 위성 위치를 계산합니다.
            
            ## 계산 항목
            - **위성 위치**: 위도, 경도, 고도
            - **추적 각도**: 방위각, 고도각
            - **궤도 정보**: 궤도 반지름, 궤도 속도
            
            ## 입력 파라미터
            - **TLE 데이터**: Two-Line Element (위성 궤도 요소)
            - **관측자 위치**: 위도, 경도, 고도
            - **현재 시간**: 시스템 시간 기준
            
            ## 응답 데이터
            - **위성 정보**: 위성 ID, 이름, TLE 데이터
            - **위치 정보**: 위도, 경도, 고도, 속도
            - **각도 정보**: 방위각, 고도각, 거리
            - **시간 정보**: 계산 시간, TLE 에포크
            
            ## 사용 예시
            ```json
            {
              "tleLine1": "1 25544U 98067A   21001.50000000 .00000000  00000+0  00000+0 0    04",
              "tleLine2": "2 25544  51.6400 114.5000 0001001 100.5000 259.5000 15.05431418000000",
              "latitude": 37.5665,
              "longitude": 126.9780,
              "altitude": 0.0
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getCurrentPosition(
        @Parameter(
            description = "위성 위치 계산 요청 데이터",
            required = true
        )
        @RequestBody request: SatellitePositionRequest
    ): Mono<SatelliteTrackData> {
        return Mono.fromCallable {
            orekitCalculator.getCurrentPosition(
                request.tleLine1,
                request.tleLine2,
                request.latitude,
                request.longitude,
                request.altitude
            )
        }
    }

    /**
     * 지정된 시간의 위성 위치를 계산합니다.
     */
    @PostMapping("/position/at-time")
    @Operation(
        summary = "지정 시간 위성 위치 계산",
        description = """
            TLE 데이터를 기반으로 특정 시간의 위성 위치를 계산합니다.
            
            ## 계산 항목
            - **위성 위치**: 지정 시간의 위도, 경도, 고도
            - **추적 각도**: 방위각, 고도각
            - **궤도 정보**: 궤도 반지름, 궤도 속도
            
            ## 입력 파라미터
            - **TLE 데이터**: Two-Line Element (위성 궤도 요소)
            - **관측자 위치**: 위도, 경도, 고도
            - **계산 시간**: ISO 8601 형식 (예: 2025-08-13T12:00:00Z)
            
            ## 응답 데이터
            - **위성 정보**: 위성 ID, 이름, TLE 데이터
            - **위치 정보**: 지정 시간의 위도, 경도, 고도, 속도
            - **각도 정보**: 방위각, 고도각, 거리
            - **시간 정보**: 계산 시간, TLE 에포크
            
            ## 사용 예시
            ```json
            {
              "tleLine1": "1 25544U 98067A   21001.50000000 .00000000  00000+0  00000+0 0    04",
              "tleLine2": "2 25544  51.6400 114.5000 0001001 100.5000 259.5000 15.05431418000000",
              "dateTime": "2025-08-13T12:00:00Z",
              "latitude": 37.5665,
              "longitude": 126.9780,
              "altitude": 0.0
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getPositionAtTime(
        @Parameter(
            description = "지정 시간 위성 위치 계산 요청 데이터",
            required = true
        )
        @RequestBody request: SatellitePositionTimeRequest
    ): Mono<SatelliteTrackData> {
        return Mono.fromCallable {
            orekitCalculator.calculatePosition(
                request.tleLine1,
                request.tleLine2,
                request.dateTime,
                request.latitude,
                request.longitude,
                request.altitude
            )
        }
    }
    /**
     * 위성 추적 스케줄을 생성합니다.
     */
    @PostMapping("/tracking/schedule")
    @Operation(
        summary = "위성 추적 스케줄 생성",
        description = """
            TLE 데이터를 기반으로 위성 추적 스케줄을 생성합니다.
            
            ## 스케줄 항목
            - **통과 시간**: 위성이 관측 가능한 시간대
            - **최적 각도**: 최대 고도각 시점
            - **추적 간격**: 설정된 간격으로 추적 데이터 생성
            - **가시성**: 최소 고도각 기준 통과 여부
            
            ## 입력 파라미터
            - **TLE 데이터**: Two-Line Element (위성 궤도 요소)
            - **시작 날짜**: 스케줄 생성 시작 날짜
            - **기간**: 스케줄 생성 기간 (일)
            - **최소 고도각**: 통과로 인정할 최소 고도각
            - **관측자 위치**: 위도, 경도, 고도
            - **추적 간격**: 추적 데이터 생성 간격 (밀리초)
            
            ## 응답 데이터
            - **스케줄 정보**: 통과 시작/종료 시간, 최대 고도각
            - **추적 데이터**: 설정된 간격의 추적 포인트
            - **통계 정보**: 총 통과 횟수, 총 추적 시간
            
            ## 사용 예시
            ```json
            {
              "tleLine1": "1 25544U 98067A   21001.50000000 .00000000  00000+0  00000+0 0    04",
              "tleLine2": "2 25544  51.6400 114.5000 0001001 100.5000 259.5000 15.05431418000000",
              "startDate": "2025-08-13T00:00:00Z",
              "durationDays": 7,
              "minElevation": 10.0,
              "latitude": 37.5665,
              "longitude": 126.9780,
              "altitude": 0.0,
              "trackingIntervalMs": 100
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun generateTrackingSchedule(
        @Parameter(
            description = "위성 추적 스케줄 생성 요청 데이터",
            required = true
        )
        @RequestBody request: SatelliteTrackingScheduleRequest
    ): Mono<OrekitCalculator.SatelliteTrackingSchedule> {
        return Mono.fromCallable {
            orekitCalculator.generateSatelliteTrackingSchedule(
                request.tleLine1,
                request.tleLine2,
                request.startDate,
                request.durationDays,
                request.minElevation,
                request.latitude,
                request.longitude,
                request.altitude,
                request.trackingIntervalMs
            )
        }
    }
    /**
     * TLE 데이터로 위성 궤도 추적 데이터를 생성합니다.
     */
    @PostMapping("/tracking/generate")
    @Operation(
        summary = "위성 궤도 추적 데이터 생성",
        description = """
            TLE 데이터를 기반으로 위성 궤도 추적 데이터를 생성합니다.
            
            ## 생성 데이터
            - **마스터 데이터 (MST)**: 위성 기본 정보 및 궤도 요소
            - **세부 데이터 (DTL)**: 시간별 상세 추적 정보
            - **변환 데이터**: 3축 변환이 적용된 최종 각도
            
            ## 처리 과정
            1. **TLE 파싱**: Two-Line Element 데이터 검증 및 파싱
            2. **궤도 계산**: Orekit을 사용한 정밀 궤도 계산
            3. **데이터 생성**: 마스터 및 세부 데이터 생성
            4. **비동기 처리**: 대용량 데이터 처리를 위한 비동기 실행
            
            ## 응답 데이터
            - **message**: 처리 결과 메시지
            - **mstCount**: 생성된 마스터 데이터 개수
            - **dtlCount**: 생성된 세부 데이터 개수
            
            ## 사용 예시
            ```json
            {
              "tleLine1": "1 25544U 98067A   21001.50000000 .00000000  00000+0  00000+0 0    04",
              "tleLine2": "2 25544  51.6400 114.5000 0001001 100.5000 259.5000 15.05431418000000",
              "satelliteName": "ISS"
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun generateEphemerisTrack(
        @Parameter(
            description = "위성 궤도 추적 데이터 생성 요청",
            required = true
        )
        @RequestBody request: EphemerisTrackRequest
    ): Mono<Map<String, Any>> {
        return ephemerisService.generateEphemerisDesignationTrackAsync(
            request.tleLine1,
            request.tleLine2,
            request.satelliteName
        )
            .map { (mstData, dtlData) ->
                mapOf<String, Any>(  // ✅ 명시적 타입 지정
                    "message" to "위성 궤도 추적 데이터 생성 완료",
                    "mstCount" to mstData.size,
                    "dtlCount" to dtlData.size
                )
            }
            .onErrorReturn(
                mapOf<String, Any>(  // ✅ 명시적 타입 지정
                    "message" to "위성 궤도 추적 데이터 생성 실패",
                    "error" to "계산 중 오류가 발생했습니다"
                )
            )
    }


    /**
     * 모든 위성 추적 마스터 데이터를 조회합니다.
     */
    @GetMapping("/master")
    @Operation(
        summary = "모든 위성 추적 마스터 데이터 조회",
        description = """
            시스템에 저장된 모든 위성 추적 마스터 데이터를 조회합니다.
            
            ## 제공 데이터
            - **위성 정보**: 위성 ID, 이름, TLE 데이터
            - **궤도 요소**: 궤도 반지름, 이심률, 궤도 경사각
            - **생성 정보**: 데이터 생성 시간, 처리 상태
            - **변환 정보**: 3축 변환이 적용된 최종 데이터
            
            ## 데이터 구조
            - **마스터 데이터 (MST)**: 위성별 기본 정보
            - **세부 데이터 (DTL)**: 시간별 상세 추적 정보
            - **변환 데이터**: 원본, 3축 변환, 최종 변환 단계
            
            ## 사용 예시
            ```
            GET /api/ephemeris/master
            ```
            
            ## 응답 예시
            ```json
            [
              {
                "mstId": 1,
                "satelliteName": "ISS",
                "tleLine1": "1 25544U 98067A...",
                "tleLine2": "2 25544 51.6400...",
                "createdAt": "2025-08-13T10:00:00Z",
                "status": "COMPLETED"
              }
            ]
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getAllEphemerisTrackMst(): Mono<List<Map<String, Any?>>> {
        return Mono.fromCallable {
            ephemerisService.getFinalTransformedEphemerisTrackMst()
        }
    }

    /**
     * 특정 마스터 ID에 해당하는 세부 추적 데이터를 조회합니다.
     */
    @GetMapping("/detail/{mstId}")
    @Operation(
        summary = "특정 위성 세부 추적 데이터 조회",
        description = """
            지정된 마스터 ID에 해당하는 위성의 세부 추적 데이터를 조회합니다.
            
            ## 제공 데이터
            - **시간별 위치**: 각 시간대의 위성 위치 (위도, 경도, 고도)
            - **추적 각도**: 방위각, 고도각, 거리
            - **궤도 정보**: 궤도 반지름, 궤도 속도
            - **변환 데이터**: 3축 변환이 적용된 각도
            
            ## 데이터 구조
            - **시간 정보**: 추적 시작/종료 시간, 간격
            - **위치 정보**: 위성의 3차원 위치 좌표
            - **각도 정보**: 관측자 기준 추적 각도
            - **메타데이터**: 데이터 생성 시간, 품질 정보
            
            ## 사용 예시
            ```
            GET /api/ephemeris/detail/1
            ```
            
            ## 응답 예시
            ```json
            [
              {
                "dtlId": 1,
                "mstId": 1,
                "timestamp": "2025-08-13T10:00:00Z",
                "latitude": 37.5665,
                "longitude": 126.9780,
                "altitude": 408.0,
                "azimuth": 180.5,
                "elevation": 45.2,
                "distance": 1234.5
              }
            ]
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getEphemerisTrackDtlByMstId(
        @Parameter(
            description = "마스터 데이터 ID",
            example = "1",
            required = true
        )
        @PathVariable mstId: UInt
    ): Mono<List<Map<String, Any?>>> {
        return Mono.fromCallable {
            ephemerisService.getEphemerisTrackDtlByMstId(mstId)
        }
    }

    /**
     * 위성 추적을 시작합니다.
     * 헤더 정보 전송 및 초기 추적 데이터 전송을 수행합니다.
     */
    @PostMapping("/tracking/start/{passId}")
    @Operation(
        summary = "위성 추적 시작",
        description = """
            지정된 통과 ID의 위성 추적을 시작합니다.
            
            ## 추적 시작 과정
            1. **헤더 정보 전송**: 위성 기본 정보 및 궤도 요소 전송
            2. **초기 추적 데이터**: 시작 시점의 위치 및 각도 정보
            3. **추적 상태 설정**: 시스템을 추적 모드로 전환
            
            ## 입력 파라미터
            - **passId**: 통과 일정 ID (UInt)
            
            ## 응답 데이터
            - **message**: 추적 시작 결과 메시지
            - **passId**: 시작된 통과 ID
            - **status**: 추적 상태 (tracking)
            
            ## 사용 예시
            ```
            POST /api/ephemeris/tracking/start/1
            ```
            
            ## 응답 예시
            ```json
            {
              "message": "위성 추적이 시작되었습니다.",
              "passId": 1,
              "status": "tracking"
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun startEphemerisTracking(
        @Parameter(
            description = "통과 일정 ID",
            example = "1",
            required = true
        )
        @PathVariable passId: UInt
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            // 위성 추적 시작 (헤더 정보 전송)
            ephemerisService.startEphemerisTracking(passId)
            // 초기 추적 데이터 전송
            //ephemerisService.sendInitialTrackingData(passId)

            mapOf(
                "message" to "위성 추적이 시작되었습니다.",
                "passId" to passId,
                "status" to "tracking"
            )
        }
    }

    /**
     * 위성 추적을 중지합니다.
     */
    @PostMapping("/tracking/stop")
    @Operation(
        summary = "위성 추적 중지",
        description = """
            현재 진행 중인 위성 추적을 중지합니다.
            
            ## 추적 중지 과정
            1. **추적 데이터 전송 중단**: 실시간 데이터 전송 중단
            2. **시스템 상태 변경**: 추적 모드에서 대기 모드로 전환
            3. **리소스 정리**: 추적 관련 메모리 및 연결 정리
            
            ## 응답 데이터
            - **message**: 추적 중지 결과 메시지
            - **status**: 시스템 상태 (stopped)
            
            ## 사용 예시
            ```
            POST /api/ephemeris/tracking/stop
            ```
            
            ## 응답 예시
            ```json
            {
              "message": "위성 추적이 중지되었습니다.",
              "status": "stopped"
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun stopEphemerisTracking(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            ephemerisService.stopEphemerisTracking()
            mapOf(
                "message" to "위성 추적이 중지되었습니다.",
                "status" to "stopped"
            )
        }
    }



    /**
     * 위성 추적 상태를 확인합니다.
     */
    @GetMapping("/tracking/status")
    @Operation(
        summary = "위성 추적 상태 확인",
        description = """
            현재 위성 추적 시스템의 상태를 확인합니다.
            
            ## 상태 정보
            - **tracking**: 추적 중인 상태
            - **idle**: 대기 중인 상태
            
            ## 추적 중일 때 제공 정보
            - **passId**: 현재 추적 중인 통과 일정 ID
            - **satelliteName**: 추적 중인 위성 이름
            - **startTime**: 추적 시작 시간
            - **endTime**: 추적 종료 예정 시간
            
            ## 사용 예시
            ```
            GET /api/ephemeris/tracking/status
            ```
            
            ## 응답 예시 (추적 중)
            ```json
            {
              "status": "tracking",
              "passId": 1,
              "satelliteName": "ISS",
              "startTime": "2025-08-13T10:00:00Z",
              "endTime": "2025-08-13T10:15:00Z"
            }
            ```
            
            ## 응답 예시 (대기 중)
            ```json
            {
              "status": "idle"
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getTrackingStatus(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val isTracking = ephemerisService.isTracking()
            val currentPass = ephemerisService.getCurrentTrackingPass()

            if (isTracking && currentPass != null) {
                mapOf(
                    "status" to "tracking",
                    "passId" to (currentPass["No"] ?: "unknown"),
                    "satelliteName" to (currentPass["SatelliteName"] ?: "unknown"),
                    "startTime" to (currentPass["StartTime"] ?: "unknown"),
                    "endTime" to (currentPass["EndTime"] ?: "unknown")
                )
            } else {
                mapOf(
                    "status" to "idle"
                )
            }
        }
    }

    /**
     * 3축 변환 계산 API
     */
    @PostMapping("/calculate-axis-transform")
    @Operation(
        summary = "3축 변환 계산",
        description = """
            위성 추적 각도를 3축 변환하여 안테나 제어에 적합한 각도로 변환합니다.
            
            ## 변환 과정
            1. **방위각 변환**: 지구 자전축 기울기 보정
            2. **고도각 변환**: 안테나 기울기 보정
            3. **회전각 계산**: 안테나 회전축 보정
            4. **최종 각도**: 모든 보정이 적용된 최종 각도
            
            ## 입력 파라미터
            - **azimuth**: 원본 방위각 (0°~360°)
            - **elevation**: 원본 고도각 (0°~90°)
            - **tilt**: 안테나 기울기 각도 (기본값: -6.98°)
            - **rotator**: 안테나 회전각 (기본값: 0°)
            
            ## 응답 데이터
            - **transformedAzimuth**: 변환된 방위각
            - **transformedElevation**: 변환된 고도각
            - **tiltAngle**: 적용된 기울기 각도
            - **rotatorAngle**: 적용된 회전각
            
            ## 사용 예시
            ```json
            {
              "azimuth": 180.0,
              "elevation": 45.0,
              "tilt": -6.98,
              "rotator": 0.0
            }
            ```
            
            ## 응답 예시
            ```json
            {
              "transformedAzimuth": 186.98,
              "transformedElevation": 51.98,
              "tiltAngle": -6.98,
              "rotatorAngle": 0.0
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun calculateAxisTransform(
        @Parameter(
            description = "3축 변환 계산 요청 데이터 (azimuth, elevation, tilt, rotator)",
            required = true
        )
        @RequestBody request: Map<String, Double>
    ): ResponseEntity<Map<String, Any>> {
        try {
            val azimuth = request["azimuth"] ?: 0.0
            val elevation = request["elevation"] ?: 0.0
            val tilt = request["tilt"] ?: 0.0
            val rotator = request["rotator"] ?: 0.0

            val result = ephemerisService.calculateAxisTransform(azimuth, elevation, tilt, rotator)
            
            return ResponseEntity.ok(result)
        } catch (error: Exception) {
            logger.error("3축 변환 계산 API 오류: ${error.message}")
            return ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "error" to (error.message ?: "알 수 없는 오류"),
                "message" to "3축 변환 계산 API 호출에 실패했습니다"
            ))
        }
    }

    // ✅ 새로운 데이터 타입별 조회 API들 추가

    /**
     * 원본 데이터 마스터 조회 API
     * 변환 전 원본 위성 추적 데이터를 조회합니다.
     */
    @GetMapping("/master/original")
    @Operation(
        summary = "원본 위성 추적 데이터 조회",
        description = """
            변환 전 원본 위성 추적 데이터를 조회합니다.
            
            ## 데이터 특징
            - **원본 데이터**: Orekit 계산 결과 그대로
            - **변환 미적용**: 3축 변환이 적용되지 않은 상태
            - **정밀도**: 최고 정밀도의 궤도 계산 결과
            
            ## 제공 데이터
            - **위성 정보**: 위성 ID, 이름, TLE 데이터
            - **궤도 요소**: 궤도 반지름, 이심률, 궤도 경사각
            - **위치 정보**: 위도, 경도, 고도, 속도
            - **각도 정보**: 방위각, 고도각, 거리
            
            ## 사용 예시
            ```
            GET /api/ephemeris/master/original
            ```
            
            ## 응답 예시
            ```json
            {
              "dataType": "original",
              "description": "변환 전 원본 위성 추적 데이터",
              "data": [
                {
                  "mstId": 1,
                  "satelliteName": "ISS",
                  "azimuth": 180.0,
                  "elevation": 45.0
                }
              ]
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getOriginalEphemerisTrackMst(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val originalMst = ephemerisService.getOriginalEphemerisTrackMst()
            mapOf(
                "dataType" to "original",
                "description" to "변환 전 원본 위성 추적 데이터",
                "count" to originalMst.size,
                "data" to originalMst
            )
        }
    }

    /**
     * 축변환 데이터 마스터 조회 API
     * 기울기 변환이 적용된 위성 추적 데이터를 조회합니다.
     */
    @GetMapping("/master/axis-transformed")
    @Operation(
        summary = "3축 변환 위성 추적 데이터 조회",
        description = """
            기울기 변환이 적용된 위성 추적 데이터를 조회합니다.
            
            ## 변환 특징
            - **기울기 보정**: 지구 자전축 기울기 (-6.98°) 보정
            - **회전각 보정**: 안테나 회전축 보정 (기본값: 0°)
            - **1단계 변환**: 원본 데이터에서 기울기 보정만 적용
            
            ## 제공 데이터
            - **위성 정보**: 위성 ID, 이름, TLE 데이터
            - **변환된 각도**: 기울기 보정이 적용된 방위각, 고도각
            - **변환 정보**: 적용된 기울기 각도, 회전각
            - **데이터 개수**: 변환된 데이터의 총 개수
            
            ## 변환 공식
            - **방위각**: 원본 방위각 + 기울기 보정각
            - **고도각**: 원본 고도각 + 기울기 보정각
            - **거리**: 원본 거리 (변경 없음)
            
            ## 사용 예시
            ```
            GET /api/ephemeris/master/axis-transformed
            ```
            
            ## 응답 예시
            ```json
            {
              "dataType": "axis_transformed",
              "description": "기울기 변환이 적용된 위성 추적 데이터",
              "count": 10,
              "transformationInfo": {
                "tiltAngle": -6.98,
                "rotatorAngle": 0.0,
                "transformationType": "axis_transform"
              }
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getAxisTransformedEphemerisTrackMst(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val axisTransformedMst = ephemerisService.getAxisTransformedEphemerisTrackMst()
            mapOf(
                "dataType" to "axis_transformed",
                "description" to "기울기 변환이 적용된 위성 추적 데이터",
                "count" to axisTransformedMst.size,
                "data" to axisTransformedMst,
                "transformationInfo" to mapOf(
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "axis_transform"
                )
            )
        }
    }

    /**
     * 최종 변환 데이터 마스터 조회 API
     * 방위각 변환까지 적용된 최종 위성 추적 데이터를 조회합니다.
     */
    @GetMapping("/master/final-transformed")
    @Operation(
        summary = "최종 변환 위성 추적 데이터 조회",
        description = """
            방위각 변환까지 적용된 최종 위성 추적 데이터를 조회합니다.
            
            ## 변환 특징
            - **기울기 보정**: 지구 자전축 기울기 (-6.98°) 보정
            - **회전각 보정**: 안테나 회전축 보정 (기본값: 0°)
            - **방위각 보정**: ±270도 제한이 적용된 최종 방위각
            - **2단계 변환**: 기울기 보정 + 방위각 제한 적용
            
            ## 제공 데이터
            - **위성 정보**: 위성 ID, 이름, TLE 데이터
            - **최종 각도**: 모든 보정이 적용된 방위각, 고도각
            - **변환 정보**: 적용된 기울기 각도, 회전각, 각도 제한
            - **데이터 개수**: 최종 변환된 데이터의 총 개수
            
            ## 변환 공식
            - **방위각**: (기울기 보정 방위각) ±270도 제한 적용
            - **고도각**: 기울기 보정 고도각 (변경 없음)
            - **거리**: 원본 거리 (변경 없음)
            
            ## 각도 제한
            - **방위각**: -270° ~ +270° 범위로 제한
            - **고도각**: 0° ~ 90° 범위 유지
            - **안전성**: 안테나 시스템의 물리적 제한 고려
            
            ## 사용 예시
            ```
            GET /api/ephemeris/master/final-transformed
            ```
            
            ## 응답 예시
            ```json
            {
              "dataType": "final_transformed",
              "description": "방위각 변환까지 적용된 최종 위성 추적 데이터",
              "count": 10,
              "transformationInfo": {
                "tiltAngle": -6.98,
                "rotatorAngle": 0.0,
                "transformationType": "final_transform",
                "angleLimit": "±270도"
              }
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun getFinalTransformedEphemerisTrackMst(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val finalTransformedMst = ephemerisService.getFinalTransformedEphemerisTrackMst()
            mapOf(
                "dataType" to "final_transformed",
                "description" to "방위각 변환까지 적용된 최종 위성 추적 데이터",
                "count" to finalTransformedMst.size,
                "data" to finalTransformedMst,
                "transformationInfo" to mapOf(
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "final_transform",
                    "angleLimit" to "±270도"
                )
            )
        }
    }

    /**
     * 특정 마스터 ID의 원본 세부 데이터 조회 API
     */
    @GetMapping("/detail/{mstId}/original")
    fun getOriginalEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val originalDtl = ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "original")
            mapOf(
                "mstId" to mstId,
                "dataType" to "original",
                "description" to "변환 전 원본 위성 추적 세부 데이터",
                "count" to originalDtl.size,
                "data" to originalDtl
            )
        }
    }

    /**
     * 특정 마스터 ID의 축변환 세부 데이터 조회 API
     */
    @GetMapping("/detail/{mstId}/axis-transformed")
    fun getAxisTransformedEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val axisTransformedDtl = ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "axis_transformed")
            mapOf(
                "mstId" to mstId,
                "dataType" to "axis_transformed",
                "description" to "기울기 변환이 적용된 위성 추적 세부 데이터",
                "count" to axisTransformedDtl.size,
                "data" to axisTransformedDtl,
                "transformationInfo" to mapOf(
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "axis_transform"
                )
            )
        }
    }

    /**
     * 특정 마스터 ID의 최종 변환 세부 데이터 조회 API
     */
    @GetMapping("/detail/{mstId}/final-transformed")
    fun getFinalTransformedEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val finalTransformedDtl = ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "final_transformed")
            mapOf(
                "mstId" to mstId,
                "dataType" to "final_transformed",
                "description" to "방위각 변환까지 적용된 최종 위성 추적 세부 데이터",
                "count" to finalTransformedDtl.size,
                "data" to finalTransformedDtl,
                "transformationInfo" to mapOf(
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "final_transform",
                    "angleLimit" to "±270도"
                )
            )
        }
    }

    /**
     * 데이터 타입별 마스터 데이터 조회 API (범용)
     */
    @GetMapping("/master/by-type/{dataType}")
    fun getEphemerisTrackMstByDataType(@PathVariable dataType: String): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val mstData = ephemerisService.getEphemerisTrackMstByDataType(dataType)
            mapOf(
                "dataType" to dataType,
                "count" to mstData.size,
                "data" to mstData,
                "availableDataTypes" to listOf("original", "axis_transformed", "final_transformed")
            )
        }
    }

    /**
     * 데이터 타입별 세부 데이터 조회 API (범용)
     */
    @GetMapping("/detail/by-type/{dataType}")
    fun getEphemerisTrackDtlByDataType(@PathVariable dataType: String): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val dtlData = ephemerisService.getEphemerisTrackDtlByDataType(dataType)
            mapOf(
                "dataType" to dataType,
                "count" to dtlData.size,
                "data" to dtlData,
                "availableDataTypes" to listOf("original", "axis_transformed", "final_transformed")
            )
        }
    }

    /**
     * 모든 변환 단계별 데이터 요약 조회 API
     */
    @GetMapping("/summary/all-transformations")
    fun getAllTransformationSummary(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val originalMst = ephemerisService.getOriginalEphemerisTrackMst()
            val axisTransformedMst = ephemerisService.getAxisTransformedEphemerisTrackMst()
            val finalTransformedMst = ephemerisService.getFinalTransformedEphemerisTrackMst()

            mapOf(
                "summary" to mapOf(
                    "original" to mapOf(
                        "mstCount" to originalMst.size,
                        "description" to "변환 전 원본 데이터"
                    ),
                    "axisTransformed" to mapOf(
                        "mstCount" to axisTransformedMst.size,
                        "description" to "기울기 변환이 적용된 데이터",
                        "tiltAngle" to -6.98,
                        "rotatorAngle" to 0.0
                    ),
                    "finalTransformed" to mapOf(
                        "mstCount" to finalTransformedMst.size,
                        "description" to "방위각 변환까지 적용된 최종 데이터",
                        "angleLimit" to "±270도"
                    )
                ),
                "totalMstCount" to (originalMst.size + axisTransformedMst.size + finalTransformedMst.size),
                "transformationSteps" to listOf(
                    "1. 원본 데이터 생성",
                    "2. 축변환 적용 (기울기 -6.98도)",
                    "3. 방위각 변환 (±270도 제한)",
                    "4. 최종 데이터 저장"
                )
            )
        }
    }

    /**
     * 모든 MST 데이터를 CSV 파일로 내보내기 API
     */
    @PostMapping("/export/csv/all")
    @Operation(
        summary = "모든 위성 추적 데이터 CSV 내보내기",
        description = """
            시스템에 저장된 모든 위성 추적 데이터를 CSV 파일로 내보냅니다.
            
            ## 내보내기 대상
            - **마스터 데이터 (MST)**: 모든 위성의 기본 정보
            - **세부 데이터 (DTL)**: 각 위성의 상세 추적 정보
            - **변환 데이터**: 원본, 3축 변환, 최종 변환 단계별 데이터
            
            ## 파일 구조
            - **파일명**: `ephemeris_track_all_YYYYMMDD_HHMMSS.csv`
            - **인코딩**: UTF-8 with BOM (Excel 호환성)
            - **구분자**: 쉼표 (,)
            
            ## 출력 디렉토리
            - **기본값**: `csv_exports`
            - **사용자 지정**: `outputDirectory` 파라미터로 변경 가능
            - **자동 생성**: 디렉토리가 없으면 자동으로 생성
            
            ## 응답 데이터
            - **totalMstCount**: 전체 마스터 데이터 개수
            - **successCount**: 성공적으로 내보낸 데이터 개수
            - **errorCount**: 오류 발생 데이터 개수
            - **createdFiles**: 생성된 CSV 파일 목록
            
            ## 사용 예시
            ```
            POST /api/ephemeris/export/csv/all?outputDirectory=my_exports
            ```
            
            ## 응답 예시
            ```json
            {
              "success": true,
              "message": "모든 MST 데이터가 CSV 파일로 성공적으로 내보내졌습니다.",
              "totalMstCount": 10,
              "successCount": 10,
              "errorCount": 0,
              "createdFiles": ["ephemeris_track_all_20250813_143000.csv"],
              "outputDirectory": "my_exports"
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun exportAllMstDataToCsv(
        @Parameter(
            description = "CSV 파일 출력 디렉토리 (기본값: csv_exports)",
            example = "csv_exports",
            required = false
        )
        @RequestParam(defaultValue = "csv_exports") outputDirectory: String
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val result = ephemerisService.exportAllMstDataToCsv(outputDirectory)
                
                if (result["success"] == true) {
                    mapOf(
                        "success" to true,
                        "message" to "모든 MST 데이터가 CSV 파일로 성공적으로 내보내졌습니다.",
                        "totalMstCount" to (result["totalMstCount"] ?: 0),
                        "successCount" to (result["successCount"] ?: 0),
                        "errorCount" to (result["errorCount"] ?: 0),
                        "createdFiles" to (result["createdFiles"] ?: emptyList<String>()),
                        "outputDirectory" to (result["outputDirectory"] ?: outputDirectory)
                    )
                } else {
                    mapOf(
                        "success" to false,
                        "message" to "CSV 내보내기 실패: ${result["error"] ?: "알 수 없는 오류"}",
                        "error" to (result["error"] ?: "알 수 없는 오류")
                    )
                }
            } catch (e: Exception) {
                mapOf(
                    "success" to false,
                    "message" to "CSV 내보내기 중 오류 발생: ${e.message ?: "알 수 없는 오류"}",
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }

    /**
     * 특정 MST ID의 데이터를 CSV 파일로 내보내기 API
     */
    @PostMapping("/export/csv/{mstId}")
    @Operation(
        summary = "특정 위성 추적 데이터 CSV 내보내기",
        description = """
            지정된 마스터 ID의 위성 추적 데이터를 CSV 파일로 내보냅니다.
            
            ## 내보내기 대상
            - **마스터 데이터 (MST)**: 위성 기본 정보 및 궤도 요소
            - **세부 데이터 (DTL)**: 시간별 상세 추적 정보
            - **변환 데이터**: 원본, 3축 변환, 최종 변환 단계별 데이터
            
            ## 파일 구조
            - **파일명**: `ephemeris_track_MST{mstId}_YYYYMMDD_HHMMSS.csv`
            - **인코딩**: UTF-8 with BOM (Excel 호환성)
            - **구분자**: 쉼표 (,)
            - **데이터 단계**: 원본, 3축 변환, 최종 변환 순서로 정렬
            
            ## 출력 디렉토리
            - **기본값**: `csv_exports`
            - **사용자 지정**: `outputDirectory` 파라미터로 변경 가능
            - **자동 생성**: 디렉토리가 없으면 자동으로 생성
            
            ## 응답 데이터
            - **filename**: 생성된 CSV 파일명
            - **filePath**: 파일의 전체 경로
            - **satelliteName**: 위성 이름
            - **originalDataCount**: 원본 데이터 개수
            - **axisTransformedDataCount**: 3축 변환 데이터 개수
            - **finalTransformedDataCount**: 최종 변환 데이터 개수
            
            ## 사용 예시
            ```
            POST /api/ephemeris/export/csv/1?outputDirectory=my_exports
            ```
            
            ## 응답 예시
            ```json
            {
              "success": true,
              "message": "MST ID 1 데이터가 CSV 파일로 성공적으로 내보내졌습니다.",
              "filename": "ephemeris_track_MST1_20250813_143000.csv",
              "filePath": "my_exports/ephemeris_track_MST1_20250813_143000.csv",
              "satelliteName": "ISS",
              "originalDataCount": 1000,
              "axisTransformedDataCount": 1000,
              "finalTransformedDataCount": 1000
            }
            ```
        """,
        tags = ["Mode - Ephemeris"]
    )
    fun exportMstDataToCsv(
        @Parameter(
            description = "마스터 데이터 ID",
            example = "1",
            required = true
        )
        @PathVariable mstId: Int,
        @Parameter(
            description = "CSV 파일 출력 디렉토리 (기본값: csv_exports)",
            example = "csv_exports",
            required = false
        )
        @RequestParam(defaultValue = "csv_exports") outputDirectory: String
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val result = ephemerisService.exportMstDataToCsv(mstId, outputDirectory)
                
                if (result["success"] == true) {
                    mapOf(
                        "success" to true,
                        "message" to "MST ID $mstId 데이터가 CSV 파일로 성공적으로 내보내졌습니다.",
                        "filename" to (result["filename"] ?: ""),
                        "filePath" to (result["filePath"] ?: ""),
                        "satelliteName" to (result["satelliteName"] ?: ""),
                        "originalDataCount" to (result["originalDataCount"] ?: 0),
                        "axisTransformedDataCount" to (result["axisTransformedDataCount"] ?: 0),
                        "finalTransformedDataCount" to (result["finalTransformedDataCount"] ?: 0)
                    )
                } else {
                    mapOf(
                        "success" to false,
                        "message" to "CSV 내보내기 실패: ${result["error"] ?: "알 수 없는 오류"}",
                        "error" to (result["error"] ?: "알 수 없는 오류")
                    )
                }
            } catch (e: Exception) {
                mapOf(
                    "success" to false,
                    "message" to "CSV 내보내기 중 오류 발생: ${e.message ?: "알 수 없는 오류"}",
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }
}
/**
 * CSV 데이터 생성 헬퍼 메서드 (개선된 버전 - createRealtimeTrackingData와 연계)
 */
private fun generateRealtimeTrackingCsv(data: List<Map<String, Any?>>): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

    try {
        // UTF-8 BOM 추가 (Excel에서 한글 깨짐 방지)
        outputStream.write(0xEF)
        outputStream.write(0xBB)
        outputStream.write(0xBF)

        // CSV 헤더 작성 (createRealtimeTrackingData의 모든 필드 포함)
        val headers = listOf(
            "Index", "TheoreticalIndex", "Timestamp", "PassId", "ElapsedTime(s)",
            
            // 원본 데이터 (변환 전)
            "OriginalAzimuth(°)", "OriginalElevation(°)", "OriginalRange(km)", "OriginalAltitude(km)",
            
            // 축변환 데이터 (기울기 변환 적용)
            "AxisTransformedAzimuth(°)", "AxisTransformedElevation(°)", "AxisTransformedRange(km)", "AxisTransformedAltitude(km)",
            
            // 최종 변환 데이터 (±270도 제한 적용)
            "FinalTransformedAzimuth(°)", "FinalTransformedElevation(°)", "FinalTransformedRange(km)", "FinalTransformedAltitude(km)",
            
            // 명령 및 실제 데이터
            "CmdAzimuth(°)", "CmdElevation(°)", "ActualAzimuth(°)", "ActualElevation(°)",
            
            // 추적 관련 데이터
            "TrackingAzimuthTime", "TrackingCMDAzimuth(°)", "TrackingActualAzimuth(°)",
            "TrackingElevationTime", "TrackingCMDElevation(°)", "TrackingActualElevation(°)",
            "TrackingTiltTime", "TrackingCMDTilt(°)", "TrackingActualTilt(°)",
            
            // 오차 분석
            "AzimuthError(°)", "ElevationError(°)",
            "OriginalToAxisTransformationError(°)", "AxisToFinalTransformationError(°)", "TotalTransformationError(°)",
            
            // 정확도 분석 (새로 추가된 필드들)
            "시간정확도(s)", "Az_CMD정확도(°)", "Az_Act정확도(°)", "Az_최종정확도(°)",
            "El_CMD정확도(°)", "El_Act정확도(°)", "El_최종정확도(°)",
            
            // 변환 정보
            "TiltAngle(°)", "TransformationType", "HasTransformation", "InterpolationMethod", "InterpolationAccuracy",
            "HasValidData", "DataSource"
        )

        writer.write(headers.joinToString(","))
        writer.write("\n")

        // 데이터 행 작성
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        data.forEach { record ->
            val row = listOf(
                record["index"]?.toString() ?: "",
                record["theoreticalIndex"]?.toString() ?: "",  // ✅ 이론치 데이터 인덱스 추가
                (record["timestamp"] as? ZonedDateTime)?.format(dateFormatter) ?: "",
                record["passId"]?.toString() ?: "",
                String.format("%.3f", record["elapsedTimeSeconds"] as? Float ?: 0.0f),
                
                // 원본 데이터
                String.format("%.6f", record["originalAzimuth"] as? Float ?: 0.0f),
                String.format("%.6f", record["originalElevation"] as? Float ?: 0.0f),
                String.format("%.6f", record["originalRange"] as? Float ?: 0.0f),
                String.format("%.6f", record["originalAltitude"] as? Float ?: 0.0f),
                
                // 축변환 데이터
                String.format("%.6f", record["axisTransformedAzimuth"] as? Float ?: 0.0f),
                String.format("%.6f", record["axisTransformedElevation"] as? Float ?: 0.0f),
                String.format("%.6f", record["axisTransformedRange"] as? Float ?: 0.0f),
                String.format("%.6f", record["axisTransformedAltitude"] as? Float ?: 0.0f),
                
                // 최종 변환 데이터
                String.format("%.6f", record["finalTransformedAzimuth"] as? Float ?: 0.0f),
                String.format("%.6f", record["finalTransformedElevation"] as? Float ?: 0.0f),
                String.format("%.6f", record["finalTransformedRange"] as? Float ?: 0.0f),
                String.format("%.6f", record["finalTransformedAltitude"] as? Float ?: 0.0f),
                
                // 명령 및 실제 데이터
                String.format("%.6f", record["cmdAz"] as? Float ?: 0.0f),
                String.format("%.6f", record["cmdEl"] as? Float ?: 0.0f),
                String.format("%.6f", record["actualAz"] as? Float ?: 0.0f),
                String.format("%.6f", record["actualEl"] as? Float ?: 0.0f),
                
                // 추적 관련 데이터
                record["trackingAzimuthTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDAzimuthAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualAzimuthAngle"] as? Float ?: 0.0f),
                record["trackingElevationTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDElevationAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualElevationAngle"] as? Float ?: 0.0f),
                record["trackingTiltTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDTiltAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualTiltAngle"] as? Float ?: 0.0f),
                
                // 오차 분석
                String.format("%.6f", record["azimuthError"] as? Float ?: 0.0f),
                String.format("%.6f", record["elevationError"] as? Float ?: 0.0f),
                String.format("%.6f", record["originalToAxisTransformationError"] as? Float ?: 0.0f),
                String.format("%.6f", record["axisToFinalTransformationError"] as? Float ?: 0.0f),
                String.format("%.6f", record["totalTransformationError"] as? Float ?: 0.0f),
                
                // 정확도 분석 (새로 추가된 필드들)
                String.format("%.6f", record["timeAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["azCmdAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["azActAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["azFinalAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["elCmdAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["elActAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["elFinalAccuracy"] as? Float ?: 0.0f),
                
                // 변환 정보
                String.format("%.6f", record["tiltAngle"] as? Double ?: 0.0),
                "\"${record["transformationType"] ?: ""}\"",
                (record["hasTransformation"] as? Boolean ?: false).toString(),
                "\"${record["interpolationMethod"] ?: ""}\"",
                String.format("%.6f", record["interpolationAccuracy"] as? Double ?: 0.0),
                (record["hasValidData"] as? Boolean ?: false).toString(),
                "\"${record["dataSource"] ?: ""}\""
            )

            writer.write(row.joinToString(","))
            writer.write("\n")
        }

        writer.flush()
        return outputStream.toByteArray()

    } finally {
        writer.close()
        outputStream.close()
    }
}
/**
 * 위성 위치 요청 모델
 */
data class SatellitePositionRequest(
    val tleLine1: String,
    val tleLine2: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
)

/**
 * 특정 시간의 위성 위치 요청 모델
 */
data class SatellitePositionTimeRequest(
    val tleLine1: String,
    val tleLine2: String,
    val dateTime: ZonedDateTime,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
)

/**
 * 위성 추적 경로 요청 모델
 */
data class SatelliteTrackingPathRequest(
    val tleLine1: String,
    val tleLine2: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val interval: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
)

/**
 * 위성 가시성 요청 모델
 */
data class SatelliteVisibilityRequest(
    val tleLine1: String,
    val tleLine2: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val interval: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val minElevation: Float = 0.0f
)

/**
 * 위성 가시성 기간 응답 모델
 */
data class VisibilityPeriod(
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime
)

/**
 * 위성 추적 스케줄 요청 모델
 */
data class SatelliteTrackingScheduleRequest(
    val tleLine1: String,
    val tleLine2: String,
    val startDate: ZonedDateTime = ZonedDateTime.now(),
    val durationDays: Int = 1,
    val minElevation: Float = 0.0f,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val trackingIntervalMs: Int = 100 // 기본값 100ms
)

/**
 * 위성 궤도 추적 요청 모델
 */
data class EphemerisTrackRequest(
    val tleLine1: String,
    val tleLine2: String,
    val satelliteName: String? = null
)

/**
 * 기울기 변환이 적용된 위성 궤도 추적 요청 모델
 */
data class EphemerisTrackWithTiltRequest(
    val tleLine1: String,
    val tleLine2: String,
    val satelliteName: String? = null,
    val tiltAngle: Double = -6.98  // 기본 기울기 각도
)

/**
 * 정지궤도 위성 추적 요청 모델
 */
data class GeostationaryTrackingRequest(
    val tleLine1: String,
    val tleLine2: String
)
