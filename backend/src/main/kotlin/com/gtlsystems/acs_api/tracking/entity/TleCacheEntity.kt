package com.gtlsystems.acs_api.tracking.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * TLE 캐시 엔티티
 * - Ephemeris/PassSchedule TLE 저장
 * - Soft Delete: is_active=FALSE로 비활성화 (이력 보관)
 */
@Table("tle_cache")
data class TleCacheEntity(
    @Id
    val id: Long? = null,

    // 위성 식별
    @Column("satellite_id")
    val satelliteId: String,

    @Column("norad_id")
    val noradId: Int? = null,

    @Column("satellite_name")
    val satelliteName: String? = null,

    // TLE 데이터
    @Column("tle_line_1")
    val tleLine1: String,

    @Column("tle_line_2")
    val tleLine2: String,

    @Column("epoch_date")
    val epochDate: OffsetDateTime,

    // 운영 정보
    @Column("mode")
    val mode: String,  // 'EPHEMERIS' | 'PASS_SCHEDULE'

    @Column("is_active")
    val isActive: Boolean = true,

    @Column("source")
    val source: String = "MANUAL",  // 'MANUAL' | 'CELESTRAK' | 'SPACE_TRACK'

    // 시스템 필드
    @Column("created_at")
    val createdAt: OffsetDateTime? = null,

    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @Column("deactivated_at")
    val deactivatedAt: OffsetDateTime? = null
) {
    companion object {
        const val MODE_EPHEMERIS = "EPHEMERIS"
        const val MODE_PASS_SCHEDULE = "PASS_SCHEDULE"

        const val SOURCE_MANUAL = "MANUAL"
        const val SOURCE_CELESTRAK = "CELESTRAK"
        const val SOURCE_SPACE_TRACK = "SPACE_TRACK"
    }
}
