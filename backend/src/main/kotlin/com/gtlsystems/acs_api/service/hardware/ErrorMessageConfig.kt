package com.gtlsystems.acs_api.service.hardware

/**
 * 에러 메시지 다국어 정의
 */
object ErrorMessageConfig {
    
    /**
     * 에러 메시지 다국어 맵
     */
    val ERROR_MESSAGES = mapOf(
        // Protocol Status
        "PROTOCOL_ELEVATION_ERROR" to mapOf(
            "ko" to "Elevation 프로토콜 통신 오류가 발생했습니다",
            "en" to "Elevation protocol communication error occurred"
        ),
        "PROTOCOL_AZIMUTH_ERROR" to mapOf(
            "ko" to "Azimuth 프로토콜 통신 오류가 발생했습니다",
            "en" to "Azimuth protocol communication error occurred"
        ),
        "PROTOCOL_TRAIN_ERROR" to mapOf(
            "ko" to "Train 프로토콜 통신 오류가 발생했습니다",
            "en" to "Train protocol communication error occurred"
        ),
        "PROTOCOL_FEED_ERROR" to mapOf(
            "ko" to "Feed 프로토콜 통신 오류가 발생했습니다",
            "en" to "Feed protocol communication error occurred"
        ),
        
        // Servo Power Status
        "SERVO_TRAIN_POWER_OFF" to mapOf(
            "ko" to "Train 서보 전원이 꺼졌습니다",
            "en" to "Train servo power is off"
        ),
        "SERVO_ELEVATION_POWER_OFF" to mapOf(
            "ko" to "Elevation 서보 전원이 꺼졌습니다",
            "en" to "Elevation servo power is off"
        ),
        "SERVO_AZIMUTH_POWER_OFF" to mapOf(
            "ko" to "Azimuth 서보 전원이 꺼졌습니다",
            "en" to "Azimuth servo power is off"
        ),
        
        // Emergency Stop
        "EMERGENCY_STOP_ACTIVE" to mapOf(
            "ko" to "비상 정지가 활성화되었습니다",
            "en" to "Emergency stop is active"
        ),
        
        // Stow Pin
        "STOW_PIN_ACTIVE" to mapOf(
            "ko" to "Stow Pin이 활성화되었습니다",
            "en" to "Stow pin is active"
        ),
        
        // Servo Alarms
        "AZIMUTH_SERVO_ALARM" to mapOf(
            "ko" to "Azimuth 서보 알람이 발생했습니다",
            "en" to "Azimuth servo alarm occurred"
        ),
        "AZIMUTH_SERVO_READY_OFF" to mapOf(
            "ko" to "Azimuth 서보 Ready가 꺼졌습니다",
            "en" to "Azimuth servo ready is off"
        ),
        "ELEVATION_SERVO_ALARM" to mapOf(
            "ko" to "Elevation 서보 알람이 발생했습니다",
            "en" to "Elevation servo alarm occurred"
        ),
        "ELEVATION_SERVO_READY_OFF" to mapOf(
            "ko" to "Elevation 서보 Ready가 꺼졌습니다",
            "en" to "Elevation servo ready is off"
        ),
        "TRAIN_SERVO_ALARM" to mapOf(
            "ko" to "Train 서보 알람이 발생했습니다",
            "en" to "Train servo alarm occurred"
        ),
        "TRAIN_SERVO_READY_OFF" to mapOf(
            "ko" to "Train 서보 Ready가 꺼졌습니다",
            "en" to "Train servo ready is off"
        ),
        
        // Resolved Messages
        "PROTOCOL_ELEVATION_RESOLVED" to mapOf(
            "ko" to "Elevation 프로토콜 통신이 정상화되었습니다",
            "en" to "Elevation protocol communication resolved"
        ),
        "PROTOCOL_AZIMUTH_RESOLVED" to mapOf(
            "ko" to "Azimuth 프로토콜 통신이 정상화되었습니다",
            "en" to "Azimuth protocol communication resolved"
        ),
        "PROTOCOL_TRAIN_RESOLVED" to mapOf(
            "ko" to "Train 프로토콜 통신이 정상화되었습니다",
            "en" to "Train protocol communication resolved"
        ),
        "PROTOCOL_FEED_RESOLVED" to mapOf(
            "ko" to "Feed 프로토콜 통신이 정상화되었습니다",
            "en" to "Feed protocol communication resolved"
        ),
        "SERVO_TRAIN_POWER_ON" to mapOf(
            "ko" to "Train 서보 전원이 켜졌습니다",
            "en" to "Train servo power is on"
        ),
        "SERVO_ELEVATION_POWER_ON" to mapOf(
            "ko" to "Elevation 서보 전원이 켜졌습니다",
            "en" to "Elevation servo power is on"
        ),
        "SERVO_AZIMUTH_POWER_ON" to mapOf(
            "ko" to "Azimuth 서보 전원이 켜졌습니다",
            "en" to "Azimuth servo power is on"
        ),
        "EMERGENCY_STOP_RESOLVED" to mapOf(
            "ko" to "비상 정지가 해제되었습니다",
            "en" to "Emergency stop is resolved"
        ),
        "STOW_PIN_RESOLVED" to mapOf(
            "ko" to "Stow Pin이 비활성화되었습니다",
            "en" to "Stow pin is deactivated"
        ),
        "AZIMUTH_SERVO_ALARM_RESOLVED" to mapOf(
            "ko" to "Azimuth 서보 알람이 해제되었습니다",
            "en" to "Azimuth servo alarm resolved"
        ),
        "AZIMUTH_SERVO_READY_ON" to mapOf(
            "ko" to "Azimuth 서보 Ready가 켜졌습니다",
            "en" to "Azimuth servo ready is on"
        ),
        "ELEVATION_SERVO_ALARM_RESOLVED" to mapOf(
            "ko" to "Elevation 서보 알람이 해제되었습니다",
            "en" to "Elevation servo alarm resolved"
        ),
        "ELEVATION_SERVO_READY_ON" to mapOf(
            "ko" to "Elevation 서보 Ready가 켜졌습니다",
            "en" to "Elevation servo ready is on"
        ),
        "TRAIN_SERVO_ALARM_RESOLVED" to mapOf(
            "ko" to "Train 서보 알람이 해제되었습니다",
            "en" to "Train servo alarm resolved"
        ),
        "TRAIN_SERVO_READY_ON" to mapOf(
            "ko" to "Train 서보 Ready가 켜졌습니다",
            "en" to "Train servo ready is on"
        )
    )
    
    /**
     * 언어별 에러 메시지 가져오기
     */
    fun getErrorMessage(key: String, language: String = "ko"): String {
        return ERROR_MESSAGES[key]?.get(language) ?: ERROR_MESSAGES[key]?.get("ko") ?: "Unknown error"
    }
}
