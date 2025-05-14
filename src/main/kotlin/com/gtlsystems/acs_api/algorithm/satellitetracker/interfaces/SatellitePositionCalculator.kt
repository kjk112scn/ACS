package com.gtlsystems.acs_api.algorithm.satellitetracker.interfaces

import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import java.time.ZonedDateTime

/**
 * 위성 위치 계산 알고리즘을 제공하는 인터페이스
 */
interface SatellitePositionCalculator {
    /**
     * 지정된 시간과 위치에 대한 위성 위치를 계산합니다.
     */
    fun calculatePosition(
        tleLine1: String,
        tleLine2: String,
        dateTime: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0
    ): SatelliteTrackData
}