package com.gtlsystems.acs_api.controller

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import com.gtlsystems.acs_api.service.EphemerisService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.ZonedDateTime

@RestController
@RequestMapping("/api/ephemeris")
@CrossOrigin(origins = ["http://localhost:9000"])
class EphemerisController(
    private val orekitCalculator: OrekitCalculator,
    private val ephemerisService: EphemerisService
)
{
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
    @PostMapping("/generate")
    fun generateEphemerisTrack(@RequestBody request: EphemerisTrackRequest): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val (mstData, dtlData) = ephemerisService.generateEphemerisDesignationTrack(
                request.tleLine1,
                request.tleLine2,
                request.satelliteName
            )

            mapOf(
                "message" to "위성 궤도 추적 데이터 생성 완료",
                "mstCount" to mstData.size,
                "dtlCount" to dtlData.size
            )
        }
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
    @PostMapping("/start/{passId}")
    fun startEphemerisTracking(@PathVariable passId: UInt): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            // 위성 추적 시작 (헤더 정보 전송)
            ephemerisService.startEphemerisTracking(passId)
            // 초기 추적 데이터 전송
            ephemerisService.sendInitialTrackingData(passId)

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