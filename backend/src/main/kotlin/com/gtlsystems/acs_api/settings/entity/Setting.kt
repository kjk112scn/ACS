package com.gtlsystems.acs_api.settings.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * 시스템 설정 엔티티 (R2DBC)
 * - 설정값 영속 저장
 * - 서버 재시작 시 복원
 */
@Table("settings")
data class Setting(
    @Id
    val id: Long? = null,

    @Column("key")
    val key: String,

    @Column("value")
    var value: String,

    @Column("type")
    val type: SettingType,

    @Column("description")
    val description: String? = null,

    @Column("is_system_setting")
    val isSystemSetting: Boolean = false,

    @Column("created_at")
    val createdAt: OffsetDateTime? = null,

    @Column("updated_at")
    var updatedAt: OffsetDateTime? = null
)

enum class SettingType {
    STRING, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN
}
