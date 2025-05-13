package com.gtlsystems.acs_api.algorithm.suntrack.impl

import com.gtlsystems.acs_api.algorithm.suntrack.interfaces.SunPositionCalculator
import com.gtlsystems.acs_api.algorithm.suntrack.model.SunTrackData
import net.e175.klaus.solarpositioning.DeltaT
import net.e175.klaus.solarpositioning.Grena3
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Grena3 알고리즘을 사용하여 태양 위치를 계산하는 클래스
 */
class Grena3Calculator : SunPositionCalculator {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun calculatePosition(
        dateTime: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        elevation: Double
    ): SunTrackData {
        try {
            val result = Grena3.calculateSolarPosition(
                dateTime,
                latitude,
                longitude,
                DeltaT.estimate(dateTime.toLocalDate())
            )

            // 천정각에서 고도각으로 변환 (고도각 = 90° - 천정각)
            val elevationAngle = 90.0 - result.zenithAngle

            return SunTrackData(
                azimuth = result.azimuth.toFloat(),
                elevation = elevationAngle.toFloat(),
                timestamp = dateTime
            )
        } catch (e: Exception) {
            logger.error("Grena3 알고리즘 계산 오류: ${e.message}", e)
            throw e
        }
    }

    override fun calculateSunrise(
        date: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): ZonedDateTime {
        // 해당 날짜의 0시부터 시작
        val startOfDay = date.truncatedTo(ChronoUnit.DAYS)

        // 1분 간격으로 24시간 동안 태양 고도 검사
        for (minute in 0 until 24 * 60) {
            val time = startOfDay.plusMinutes(minute.toLong())
            val position = calculatePosition(time, latitude, longitude, 0.0)

            // 태양 고도가 0도를 넘어가는 순간이 일출
            if (position.elevation >= 0) {
                return time
            }
        }

        // 일출을 찾지 못한 경우 (극지방 등에서 발생 가능)
        return startOfDay
    }

    override fun calculateSunset(
        date: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): ZonedDateTime {
        // 해당 날짜의 정오부터 시작
        val noon = date.truncatedTo(ChronoUnit.DAYS).plusHours(12)

        // 1분 간격으로 12시간 동안 태양 고도 검사
        for (minute in 0 until 12 * 60) {
            val time = noon.plusMinutes(minute.toLong())
            val position = calculatePosition(time, latitude, longitude, 0.0)

            // 태양 고도가 0도 아래로 내려가는 순간이 일몰
            if (position.elevation < 0) {
                return time
            }
        }

        // 일몰을 찾지 못한 경우 (극지방 등에서 발생 가능)
        return noon.plusHours(12)
    }
}