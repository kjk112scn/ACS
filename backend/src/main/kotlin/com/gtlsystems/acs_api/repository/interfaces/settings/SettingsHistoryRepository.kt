package com.gtlsystems.acs_api.repository.interfaces.settings

import com.gtlsystems.acs_api.settings.entity.SettingHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SettingsHistoryRepository : JpaRepository<SettingHistory, Long> {
    fun findBySettingKey(settingKey: String): List<SettingHistory>
    fun findByChangedBy(changedBy: String): List<SettingHistory>
    fun findByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<SettingHistory>
}