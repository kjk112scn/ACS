package com.gtlsystems.acs_api.settings.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * 설정 변경 이력 엔티티 (R2DBC)
 * - 감사 로그: 누가 언제 어떤 설정을 변경했는지 추적
 */
@Table("setting_history")
data class SettingHistory(
    @Id
    val id: Long? = null,

    @Column("setting_key")
    val settingKey: String,

    @Column("old_value")
    val oldValue: String? = null,

    @Column("new_value")
    val newValue: String,

    @Column("changed_by")
    val changedBy: String? = null,

    @Column("change_reason")
    val changeReason: String? = null,

    @Column("created_at")
    val createdAt: OffsetDateTime? = null
)
