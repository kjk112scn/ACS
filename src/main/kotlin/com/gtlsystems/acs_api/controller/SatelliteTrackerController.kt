package com.gtlsystems.acs_api.controller

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime

@RestController
@RequestMapping("/api/satellite")
class SatelliteTrackerController(private val orekitCalculator: OrekitCalculator) {

    /**
     * 현재 시간의 위성 위치를 계산합니다.
     */
    @PostMapping("/position/current")
    fun getCurrentPosition(@RequestBody request: SatellitePositionRequest): ResponseEntity<SatelliteTrackData> {
        val position = orekitCalculator.getCurrentPosition(
            request.tleLine1,
            request.tleLine2,
            request.latitude,
            request.longitude,
            request.altitude
        )
        return ResponseEntity.ok(position)
    }

    /**
     * 지정된 시간의 위성 위치를 계산합니다.
     */
    @PostMapping("/position/at-time")
    fun getPositionAtTime(@RequestBody request: SatellitePositionTimeRequest): ResponseEntity<SatelliteTrackData> {
        val position = orekitCalculator.calculatePosition(
            request.tleLine1,
            request.tleLine2,
            request.dateTime,
            request.latitude,
            request.longitude,
            request.altitude
        )
        return ResponseEntity.ok(position)
    }
/*

    */
/**
     * 시간 범위 동안의 위성 위치를 계산합니다.
     *//*

    @PostMapping("/tracking/path")
    fun getTrackingPath(@RequestBody request: SatelliteTrackingPathRequest): ResponseEntity<SatelliteTrackData> {
        val trackingPath = orekitCalculator.calculateTrackingPath(
            request.tleLine1,
            request.tleLine2,
            request.startTime,
            request.endTime,
            request.interval,
            request.latitude,
            request.longitude,
            request.altitude
        )
        return ResponseEntity.ok(trackingPath)
    }

    */
/**
     * 위성의 가시 시간을 계산합니다.
     *//*

    @PostMapping("/tracking/visibility")
    fun getVisibilityPeriods(@RequestBody request: SatelliteVisibilityRequest): ResponseEntity<List<VisibilityPeriod>> {
        val periods = orekitCalculator.calculateVisibilityPeriods(
            request.tleLine1,
            request.tleLine2,
            request.startTime,
            request.endTime,
            request.interval,
            request.latitude,
            request.longitude,
            request.altitude,
            request.minElevation
        )

        val response = periods.map {
            VisibilityPeriod(it.first, it.second)
        }

        return ResponseEntity.ok(response)
    }
*/

    /**
     * 위성 추적 스케줄을 생성합니다.
     */
    @PostMapping("/tracking/schedule")
    fun generateTrackingSchedule(@RequestBody request: SatelliteTrackingScheduleRequest): ResponseEntity<OrekitCalculator.SatelliteTrackingSchedule> {
        val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
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
        return ResponseEntity.ok(schedule)
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