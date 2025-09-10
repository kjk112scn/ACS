package com.gtlsystems.acs_api.settings.entity
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "settings")
data class Setting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "key", nullable = false, unique = true)
    val key: String,

    @Column(name = "value", nullable = false)
    var value: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: SettingType,

    @Column(name = "description")
    val description: String? = null,

    @Column(name = "is_system_setting", nullable = false)
    val isSystemSetting: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // JPA를 위한 기본 생성자 추가
    constructor() : this(
        id = null,
        key = "",
        value = "",
        type = SettingType.STRING,
        description = null,
        isSystemSetting = false,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}

enum class SettingType {
    STRING, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN
}