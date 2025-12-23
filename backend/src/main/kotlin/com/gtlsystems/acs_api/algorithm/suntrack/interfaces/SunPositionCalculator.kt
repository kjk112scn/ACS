package com.gtlsystems.acs_api.algorithm.suntrack.interfaces

import com.gtlsystems.acs_api.algorithm.suntrack.impl.SPACalculator
import com.gtlsystems.acs_api.algorithm.suntrack.model.SunTrackData
import net.e175.klaus.solarpositioning.SPA
import java.time.ZonedDateTime

/**
 * 태양 위치 계산 알고리즘을 제공하는 인터페이스
 */
interface SunPositionCalculator {
    /**
     * 지정된 시간과 위치에 대한 태양 위치를 계산합니다.
     */
    fun calculatePosition(
        dateTime: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        elevation: Double = 0.0,
    ): SunTrackData
    /**
     * 일출 시간을 계산합니다.
     */
    fun calculateSunrise(
        date: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): ZonedDateTime

    /**
     * 일몰 시간을 계산합니다.
     */
    fun calculateSunset(
        date: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): ZonedDateTime

}