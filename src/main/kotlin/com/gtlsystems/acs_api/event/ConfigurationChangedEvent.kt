package com.gtlsystems.acs_api.event

import org.springframework.context.ApplicationEvent

/**
 * 설정 변경 이벤트
 * 설정이 변경될 때 발행됩니다.
 */
class ConfigurationChangedEvent(
    val configKey: String,
    val oldValue: Any?,
    val newValue: Any?
) : ApplicationEvent(configKey) {
    
    override fun toString(): String {
        return "ConfigurationChangedEvent(key='$configKey', oldValue=$oldValue, newValue=$newValue)"
    }
} 