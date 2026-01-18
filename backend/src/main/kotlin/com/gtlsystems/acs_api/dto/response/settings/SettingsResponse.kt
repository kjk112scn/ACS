package com.gtlsystems.acs_api.dto.response.settings

import com.gtlsystems.acs_api.settings.entity.Setting
import com.gtlsystems.acs_api.settings.entity.SettingType
import java.time.OffsetDateTime

data class SettingsResponse(
    val key: String,
    val value: String,
    val type: SettingType,
    val description: String?,
    val isSystemSetting: Boolean,
    val updatedAt: OffsetDateTime?
) {
    companion object {
        fun from(setting: Setting) = SettingsResponse(
            key = setting.key,
            value = setting.value,
            type = setting.type,
            description = setting.description,
            isSystemSetting = setting.isSystemSetting,
            updatedAt = setting.updatedAt
        )
    }
}
