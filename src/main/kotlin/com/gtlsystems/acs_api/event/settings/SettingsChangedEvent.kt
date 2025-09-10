package com.gtlsystems.acs_api.event.settings

/**
 * 설정 변경 이벤트
 * 설정이 변경될 때 발행되어 다른 컴포넌트들에게 알림
 */
data class SettingsChangedEvent(
    val key: String,           // 변경된 설정의 키
    val value: Any,           // 변경된 새로운 값
    val userId: String,       // 변경을 수행한 사용자 ID
    val timestamp: Long = System.currentTimeMillis()  // 변경 시간
)
