package com.gtlsystems.acs_api.model

import com.gtlsystems.acs_api.model.PushData.ReadData
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference

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
        val systemTimeZone: ZoneId = ZoneId.systemDefault()

        var serverTimeZone: ZoneId  = systemTimeZone

        var clientTimeZone: ZoneId = systemTimeZone
        /**
         * 현재 로컬 시간을 출력함.
         * utcNow + addLocalTime을 계산하여 반환
         * 로컬 타임으로 표시해줌 한국 시간으로 +9시간이 적용되어있음.
         */
        val serverTime: ZonedDateTime
            get() = ZonedDateTime.now(serverTimeZone)

        /**
         * 현재 서버 시간 + Offset.TimeOffset을 계산하여 반환
         */
        val resultTimeOffsetCalTime: ZonedDateTime
            get() = serverTime.plusSeconds(Offset.TimeOffset.toLong())
        val calUtcTimeOffsetTime: ZonedDateTime
            get() = utcNow.plusSeconds(Offset.TimeOffset.toLong())
    }

    object Offset {
        var TimeOffset: Float = 0.0f
        var azimuthPositionOffset: Float = 0.0f
        var elevationPositionOffset: Float = 0.0f
        var tiltPositionOffset: Float = 0.0f
        var trueNorthOffset: Float = 0.0f
    }
    object EphemerisTrakingAngle {
        var azimuthAngle: Float = 0.0f
        var elevationAngle: Float = 0.0f
        var tiltAngle: Float = 0.0f
    }
    object SunTrackingData {
        var azimuthAngle: Float = 0.0f
        var azimuthSpeed: Float = 0.0f
        var elevationAngle: Float = 0.0f
        var elevationSpeed: Float = 0.0f
        var tiltAngle: Float = 0.0f
        var tiltSpeed: Float = 0.0f
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
/* 
    object Location {
         //한국 GTL 본사
        var latitude: Double = 35.317540//35.3175
        var longitude: Double = 128.608510//128.6083
        //시드니 남반구 테스트 태양추적시시
        //var latitude: Double = -33.8688 //35.3175
        //var longitude: Double = 151.2093//128.6083
        var altitude: Double = 0.0
    }
     */
}
