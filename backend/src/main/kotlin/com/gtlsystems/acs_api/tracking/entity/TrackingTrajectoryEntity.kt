package com.gtlsystems.acs_api.tracking.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * 이론 궤적 엔티티 (Hypertable)
 * - 위성 궤적 이론 데이터 저장
 * - PRIMARY KEY 없음 (TimescaleDB Hypertable)
 */
@Table("tracking_trajectory")
data class TrackingTrajectoryEntity(
    @Column("timestamp")
    val timestamp: OffsetDateTime,

    @Column("session_id")
    val sessionId: Long,

    @Column("detail_id")
    val detailId: Int,

    @Column("data_type")
    val dataType: String,

    // 인덱스
    @Column("index")
    val index: Int,

    // 각도
    @Column("azimuth")
    val azimuth: Double,

    @Column("elevation")
    val elevation: Double,

    @Column("train")
    val train: Double? = null,

    // 속도
    @Column("azimuth_rate")
    val azimuthRate: Double? = null,

    @Column("elevation_rate")
    val elevationRate: Double? = null,

    @Column("train_rate")
    val trainRate: Double? = null,

    // 위성 정보 (V006)
    @Column("satellite_range")
    val satelliteRange: Double? = null,

    @Column("satellite_altitude")
    val satelliteAltitude: Double? = null,

    @Column("satellite_velocity")
    val satelliteVelocity: Double? = null,

    @Column("created_at")
    val createdAt: OffsetDateTime? = null
)
