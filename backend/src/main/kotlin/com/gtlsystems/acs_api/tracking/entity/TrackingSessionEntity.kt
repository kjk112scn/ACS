package com.gtlsystems.acs_api.tracking.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * 추적 세션 엔티티
 * - 패스별 요약 정보 저장
 * - FK 부모 테이블 (trajectory, result의 부모)
 */
@Table("tracking_session")
data class TrackingSessionEntity(
    @Id
    val id: Long? = null,

    @Column("mst_id")
    val mstId: Long,

    @Column("detail_id")
    val detailId: Int,

    @Column("satellite_id")
    val satelliteId: String,

    @Column("satellite_name")
    val satelliteName: String? = null,

    @Column("tracking_mode")
    val trackingMode: String,  // 'EPHEMERIS' | 'PASS_SCHEDULE'

    @Column("data_type")
    val dataType: String,  // 'original', 'axisTransformed', etc.

    // 시간 정보
    @Column("start_time")
    val startTime: OffsetDateTime,

    @Column("end_time")
    val endTime: OffsetDateTime,

    @Column("duration")
    val duration: Int? = null,  // 초 단위

    // 각도 정보 (Peak 값)
    @Column("max_elevation")
    val maxElevation: Double? = null,

    @Column("max_azimuth_rate")
    val maxAzimuthRate: Double? = null,

    @Column("max_elevation_rate")
    val maxElevationRate: Double? = null,

    @Column("keyhole_detected")
    val keyholeDetected: Boolean = false,

    // 추가 메타데이터
    @Column("recommended_train_angle")
    val recommendedTrainAngle: Double? = null,

    @Column("total_points")
    val totalPoints: Int? = null,

    // 시스템 필드
    @Column("created_at")
    val createdAt: OffsetDateTime? = null
)
