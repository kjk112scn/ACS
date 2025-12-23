package com.gtlsystems.acs_api.dto.request.settings

import com.gtlsystems.acs_api.settings.entity.SettingType
 
data class SettingsUpdateRequest(
    val value: String,
    val type: SettingType = SettingType.STRING,
    val reason: String? = null
) 
