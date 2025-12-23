package com.gtlsystems.acs_api.settings.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "setting_history")
data class SettingHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "setting_key", nullable = false)
    val settingKey: String,

    @Column(name = "old_value")
    val oldValue: String? = null,

    @Column(name = "new_value", nullable = false)
    val newValue: String,

    @Column(name = "changed_by")
    val changedBy: String? = null,

    @Column(name = "change_reason")
    val changeReason: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)