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
        var latitude: Double = 35.317540//35.3175
        var longitude: Double = 128.608510//128.6083
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
    object Tracking {
        /**
         * 추적 주기 100ms 단위로 스케줄 출력
         */
        var msIntervel: Int = 100

        /**
         * 몇일 단위로 스케줄 출력 기준 
         * 1일치 단위 출력
         */
        var durationDays: Int = 1
        // 특정 날짜/시간 설정 (14일 00시, UTC)
        val utcZoneId = ZoneId.of("UTC")
        val startDate = ZonedDateTime.of(
            Time.utcNow.year, Time.utcNow.month.value, Time.utcNow.dayOfMonth,
            0, 0, 0, 0,
            utcZoneId
        )
        val minElevationAngle: Float = 0.005f//0.0f로 설정하면 미세한 음수값 추출가능하여 양수로 설정
    }

    /**
     * 수신 데이터 저장소
     */
    object ReceivedData {
        // 원자적 참조로 스레드 안전성 보장
        private val latestData = AtomicReference<ReadData>(ReadData())

        // 데이터 업데이트 함수
        fun updateData(newData: ReadData) {
            latestData.set(newData)
        }

        // 데이터 조회 함수
        fun getData(): ReadData {
            return latestData.get()
        }

        // 마지막 업데이트 시간
        var lastUpdateTime: Long = System.currentTimeMillis()
    }

    /**
     * 데이터 전송 관련 설정
     */
    object DataTransmission {
        /**
         * 웹소켓을 통한 데이터 전송 간격 (밀리초)
         */
        var transmissionInterval: Long = 100

        /**
         * UDP 연결 타임아웃 (초)
         * 이 시간 동안 UDP 패킷이 수신되지 않으면 연결이 끊긴 것으로 간주
         */
        var udpTimeoutSeconds: Long = 5

        /**
         * UDP 재연결 시도 간격 (밀리초)
         */
        var udpReconnectInterval: Long = 1000

        /**
         * 웹소켓 연결 타임아웃 (밀리초)
         */
        var webSocketTimeoutMillis: Long = 30000

        /**
         * 데이터 전송 최대 버퍼 크기
         */
        var maxBufferSize: Int = 1024

        /**
         * UDP 연결이 끊겼을 때 더미 데이터 사용 여부
         */
        var useDummyDataWhenDisconnected: Boolean = true
    }
}
