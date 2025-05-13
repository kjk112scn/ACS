package com.gtlsystems.acs_api.event

/**
 * 애플리케이션 전체에서 사용되는 이벤트 타입을 정의합니다.
 */
sealed class ACSEvent {
    /**
     * 추적 관련 이벤트
     */
    sealed class TrackingEvent : ACSEvent() {
        // 태양 추적 관련 이벤트
        object StopSunTracking : TrackingEvent()
        data class StartSunTracking(val interval: Long, val speed: Float) : TrackingEvent()

        // 에페메리스 추적 관련 이벤트
        object StopEphemerisTracking : TrackingEvent()
        data class StartEphemerisTracking(val satelliteId: String) : TrackingEvent()

        // 패스 스케줄 관련 이벤트
        object StopPassSchedule : TrackingEvent()
        data class StartPassSchedule(val scheduleId: String) : TrackingEvent()

        // 모든 추적 중지
        object StopAllTracking : TrackingEvent()
    }

    /**
     * 시스템 상태 관련 이벤트
     */
    sealed class SystemEvent : ACSEvent() {
        object EmergencyStop : SystemEvent()
        object SystemReady : SystemEvent()
        data class Error(val message: String, val code: Int) : SystemEvent()
    }

    /**
     * 데이터 관련 이벤트
     */
    sealed class DataEvent : ACSEvent() {
        data class DataUpdated(val dataType: String, val data: Any) : DataEvent()
        data class ConfigChanged(val configType: String, val newValue: Any) : DataEvent()
    }
}