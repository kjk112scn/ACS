package com.gtlsystems.acs_api.dto.response.settings

import com.gtlsystems.acs_api.settings.entity.SettingHistory
import java.time.LocalDateTime

data class SettingsHistoryResponse(
    val key: String,
    val oldValue: String?,
    val newValue: String,
    val changedBy: String?,
    val createdAt: LocalDateTime,
    val changeReason: String?
) {
    companion object {
        fun from(history: SettingHistory) = SettingsHistoryResponse(
            key = history.settingKey,
            oldValue = history.oldValue,
            newValue = history.newValue,
            changedBy = history.changedBy,
            createdAt = history.createdAt,
            changeReason = history.changeReason
        )
    }
}