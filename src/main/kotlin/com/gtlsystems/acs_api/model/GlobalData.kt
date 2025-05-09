package com.gtlsystems.acs_api.model

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * 전역 데이터 클래스
 */

object GlobalData {
    /**
     * 시간 관련 정보
     * utcNow: 현재 시간 (UTC)
     * localNow: 현재 시간 (로컬)
     * addLocalTime: utcNow에 더할 시간 (예: 한국 시간은 +9시간)
     * resultServerTime: 최종 서버 시간 (UTC)
     */
    object Time {
        val utcNow: ZonedDateTime
            get() = ZonedDateTime.now(ZoneOffset.UTC)

        val localNow: LocalDateTime
            get() = LocalDateTime.now()

        var addLocalTime: Int = 0

        /**
         * 현재 로컬 시간을 출력함.
         * utcNow + addLocalTime을 계산하여 반환
         */
        val serverTime: ZonedDateTime
            get() = utcNow.plusSeconds(addLocalTime.toLong())

        /**
         * 현재 서버 시간 + Offset.TimeOffset을 계산하여 반환
         */
        val resultTimeOffsetCalTime: ZonedDateTime
            get() = serverTime.plusSeconds(Offset.TimeOffset.toLong())
    }

    object Offset {
        var TimeOffset: Float = 0.0f
        var azimuthPositionOffset: Float = 0.0f
        var elevationPositionOffset: Float = 0.0f
        var tiltPositionOffset: Float = 0.0f
        var trueNorthOffset: Float = 0.0f
    }

    /*
* 버전 정보
*/
    object Version {
        var apiVersion: String = "1.0.0"
        var buildDate: String = "2023-01-01T00:00:00Z"
    }

    /*
* 위치 정보
*/
    object Location {
        var latitude: Double = 0.0
        var longitude: Double = 0.0
        var altitude: Double = 0.0
    }

    /*
* 설정 정보(사용자 설정)
*/
    object Settings {
        var trackingMode: String = "Auto"
        var trackingInterval: Int = 60
        var trackingSpeed: Int = 10
        var trackingAccuracy: Int = 1
    }

    object CMD {
        var azimuth: Float = 0.0f
        var elevation: Float = 0.0f
        var tilt: Float = 0.0f
    }
}
