package com.gtlsystems.acs_api.controller.mode

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import com.gtlsystems.acs_api.service.mode.EphemerisService
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
class EphemerisController(
    private val orekitCalculator: OrekitCalculator,
    private val ephemerisService: EphemerisService
)
{
    private val logger = LoggerFactory.getLogger(EphemerisController::class.java)

    @PostMapping("/3axis/tracking/geostationary/start")
    fun startGeostationaryTracking(@RequestBody request: GeostationaryTrackingRequest): Mono<Map<String, Any>> {
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
    /**
     * TLE 데이터로 기울기 변환이 적용된 위성 궤도 추적 데이터를 생성합니다.
     */
    @PostMapping("/3axis/tracking/generate-with-tilt-transform")
    fun generateEphemerisTrackWithTiltTransform(@RequestBody request: EphemerisTrackWithTiltRequest): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val (mstData, dtlData) = ephemerisService.generateEphemerisDesignationTrackWithTiltTransform(
                    request.tleLine1,
                    request.tleLine2,
                    request.satelliteName,
                    request.tiltAngle
                )

                mapOf<String, Any>(
                    "message" to "기울기 변환이 적용된 위성 궤도 추적 데이터 생성 완료",
                    "mstCount" to mstData.size,
                    "dtlCount" to dtlData.size,
                    "tiltAngle" to request.tiltAngle,
                    "transformationType" to "tilt_only"
                )
            } catch (e: Exception) {
                mapOf<String, Any>(
                    "message" to "기울기 변환이 적용된 위성 궤도 추적 데이터 생성 실패",
                    "error" to (e.message ?: "계산 중 오류가 발생했습니다"),
                    "tiltAngle" to request.tiltAngle
                )
            }
        }
    }

    /**
     * TLE 데이터로 축변환이 적용된 위성 궤도 추적 데이터를 생성합니다.
     * 방위각 변환 + 축변환 + 변환된 시계열에서 속도/가속도 계산
     */
    @PostMapping("/3axis/tracking/generate-with-axis-transform")
    fun generateEphemerisDesignationTrackWithTiltTransform(@RequestBody request: EphemerisTrackRequest): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val (mstData, dtlData) = ephemerisService.generateEphemerisDesignationTrackSync(
                    request.tleLine1,
                    request.tleLine2,
                    request.satelliteName
                )

                mapOf<String, Any>(
                    "message" to "축변환이 적용된 위성 궤도 추적 데이터 생성 완료",
                    "mstCount" to mstData.size,
                    "dtlCount" to dtlData.size,
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "axis_transform",
                    "processingSteps" to listOf(
                        "방위각 변환 (0~360도 -> ±270도)",
                        "축변환 (기울기 -6.98도, 회전체 0도)",
                        "변환된 시계열에서 속도/가속도 계산"
                    )
                )
            } catch (e: Exception) {
                mapOf<String, Any>(
                    "message" to "축변환이 적용된 위성 궤도 추적 데이터 생성 실패",
                    "error" to (e.message ?: "계산 중 오류가 발생했습니다"),
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0
                )
            }
        }
    }

    /**
     * 실시간 추적 데이터 조회 (JSON)
     */
    @GetMapping("/tracking/realtime-data")
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
    fun getCurrentPosition(@RequestBody request: SatellitePositionRequest): Mono<SatelliteTrackData> {
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
    fun getPositionAtTime(@RequestBody request: SatellitePositionTimeRequest): Mono<SatelliteTrackData> {
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
    fun generateTrackingSchedule(@RequestBody request: SatelliteTrackingScheduleRequest): Mono<OrekitCalculator.SatelliteTrackingSchedule> {
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
    fun generateEphemerisTrack(@RequestBody request: EphemerisTrackRequest): Mono<Map<String, Any>> {
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
    fun getAllEphemerisTrackMst(): Mono<List<Map<String, Any?>>> {
        return Mono.fromCallable {
            ephemerisService.getAllEphemerisTrackMst()
        }
    }

    /**
     * 특정 마스터 ID에 해당하는 세부 추적 데이터를 조회합니다.
     */
    @GetMapping("/detail/{mstId}")
    fun getEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<List<Map<String, Any?>>> {
        return Mono.fromCallable {
            ephemerisService.getEphemerisTrackDtlByMstId(mstId)
        }
    }

    /**
     * 위성 추적을 시작합니다.
     * 헤더 정보 전송 및 초기 추적 데이터 전송을 수행합니다.
     */
    @PostMapping("/tracking/start/{passId}")
    fun startEphemerisTracking(@PathVariable passId: UInt): Mono<Map<String, Any>> {
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
}
/**
 * CSV 데이터 생성 헬퍼 메서드
 */
private fun generateRealtimeTrackingCsv(data: List<Map<String, Any?>>): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

    try {
        // UTF-8 BOM 추가 (Excel에서 한글 깨짐 방지)
        outputStream.write(0xEF)
        outputStream.write(0xBB)
        outputStream.write(0xBF)

        // CSV 헤더 작성
        val headers = listOf(
            "Index", "Timestamp", "PassId", "ElapsedTime(s)",
            "CmdAzimuth(°)", "CmdElevation(°)",
            "TrackingAzimuthTime", "TrackingCMDAzimuth(°)", "TrackingActualAzimuth(°)",
            "TrackingElevationTime", "TrackingCMDElevation(°)", "TrackingActualElevation(°)",
            "TrackingTiltTime", "TrackingCMDTilt(°)", "TrackingActualTilt(°)",
            "AzimuthError(°)", "ElevationError(°)"
        )

        writer.write(headers.joinToString(","))
        writer.write("\n")

        // 데이터 행 작성
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        data.forEach { record ->
            val row = listOf(
                record["index"]?.toString() ?: "",
                (record["timestamp"] as? ZonedDateTime)?.format(dateFormatter) ?: "",
                record["passId"]?.toString() ?: "",
                String.format("%.3f", record["elapsedTimeSeconds"] as? Float ?: 0.0f),
                String.format("%.6f", record["cmdAz"] as? Float ?: 0.0f),
                String.format("%.6f", record["cmdEl"] as? Float ?: 0.0f),
                record["trackingAzimuthTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDAzimuthAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualAzimuthAngle"] as? Float ?: 0.0f),
                record["trackingElevationTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDElevationAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualElevationAngle"] as? Float ?: 0.0f),
                record["trackingTiltTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDTiltAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualTiltAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["azimuthError"] as? Float ?: 0.0f),
                String.format("%.6f", record["elevationError"] as? Float ?: 0.0f)
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
