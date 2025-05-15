package com.gtlsystems.acs_api.model

import java.time.ZonedDateTime

/**
 * 위성 추적 설정 정보를 제공하는 클래스
 * GlobalData의 값을 캡슐화하여 제공합니다.
 */
object SatelliteTrackingData {
    /**
     * 위치 관련 설정
     */
    object Location {
        val latitude: Double
            get() = GlobalData.Location.latitude

        val longitude: Double
            get() = GlobalData.Location.longitude

        val altitude: Double
            get() = GlobalData.Location.altitude
    }

    /**
     * 추적 관련 설정
     */
    object Tracking {
        val msInterval: Int
            get() = GlobalData.Tracking.msIntervel

        val durationDays: Long
            get() = GlobalData.Tracking.durationDays.toLong()

        val startDate: ZonedDateTime
            get() = GlobalData.Tracking.startDate

        val minElevationAngle: Float
            get() = GlobalData.Tracking.minElevationAngle
    }
}
