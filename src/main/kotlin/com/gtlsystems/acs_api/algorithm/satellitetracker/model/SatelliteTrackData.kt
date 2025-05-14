package com.gtlsystems.acs_api.algorithm.satellitetracker.model

import java.time.ZonedDateTime

/**
 * 위성 추적 데이터를 담는 클래스
 */
data class SatelliteTrackData(
    val azimuth: Double,
    val elevation: Double,
    val timestamp: ZonedDateTime? = null,
    val range: Double? = null,
    val altitude: Double? = null,
    val startTime: ZonedDateTime? = null,
    val endTime: ZonedDateTime? = null,
    val interval: Int? = null,
    val positions: List<Pair<ZonedDateTime, SatelliteTrackData>>? = null
) {
    // Float 타입을 받는 생성자 (이전 코드와의 호환성을 위해)
    constructor(azimuth: Float, elevation: Float) : this(
        azimuth = azimuth.toDouble(),
        elevation = elevation.toDouble(),
        timestamp = null,
        range = null,
        altitude = null,
        startTime = null,
        endTime = null,
        interval = null,
        positions = null
    )

    // 추가 파라미터를 포함한 Float 타입 생성자 (이전 코드와의 호환성을 위해)
    constructor(
        azimuth: Float,
        elevation: Float,
        timestamp: ZonedDateTime? = null,
        range: Float? = null
    ) : this(
        azimuth = azimuth.toDouble(),
        elevation = elevation.toDouble(),
        timestamp = timestamp,
        range = range?.toDouble(),
        altitude = null,
        startTime = null,
        endTime = null,
        interval = null,
        positions = null
    )
}