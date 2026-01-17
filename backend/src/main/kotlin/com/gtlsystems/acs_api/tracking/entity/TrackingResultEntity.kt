package com.gtlsystems.acs_api.tracking.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * 실측 추적 결과 엔티티 (Hypertable)
 * - 실제 추적 결과 데이터 저장
 * - PRIMARY KEY 없음 (TimescaleDB Hypertable)
 */
@Table("tracking_result")
data class TrackingResultEntity(
    @Column("timestamp")
    val timestamp: OffsetDateTime,

    @Column("session_id")
    val sessionId: Long,

    // 인덱스/시간
    @Column("index")
    val index: Int,

    @Column("theoretical_index")
    val theoreticalIndex: Int? = null,

    // 원본 각도 (2축)
    @Column("original_azimuth")
    val originalAzimuth: Double? = null,

    @Column("original_elevation")
    val originalElevation: Double? = null,

    // 변환된 각도 (3축)
    @Column("transformed_azimuth")
    val transformedAzimuth: Double? = null,

    @Column("transformed_elevation")
    val transformedElevation: Double? = null,

    @Column("transformed_train")
    val transformedTrain: Double? = null,

    // 최종 각도 (제한 적용)
    @Column("final_azimuth")
    val finalAzimuth: Double? = null,

    @Column("final_elevation")
    val finalElevation: Double? = null,

    @Column("final_train")
    val finalTrain: Double? = null,

    // 실제 측정값
    @Column("actual_azimuth")
    val actualAzimuth: Double? = null,

    @Column("actual_elevation")
    val actualElevation: Double? = null,

    @Column("actual_train")
    val actualTrain: Double? = null,

    // 오차
    @Column("azimuth_error")
    val azimuthError: Double? = null,

    @Column("elevation_error")
    val elevationError: Double? = null,

    @Column("train_error")
    val trainError: Double? = null,

    @Column("total_error")
    val totalError: Double? = null,

    // 속도
    @Column("azimuth_rate")
    val azimuthRate: Double? = null,

    @Column("elevation_rate")
    val elevationRate: Double? = null,

    @Column("train_rate")
    val trainRate: Double? = null,

    // 가속도
    @Column("azimuth_acceleration")
    val azimuthAcceleration: Double? = null,

    @Column("elevation_acceleration")
    val elevationAcceleration: Double? = null,

    @Column("train_acceleration")
    val trainAcceleration: Double? = null,

    // 상태
    @Column("keyhole_active")
    val keyholeActive: Boolean = false,

    @Column("keyhole_optimized")
    val keyholeOptimized: Boolean = false,

    @Column("tracking_quality")
    val trackingQuality: String? = null,

    // 보간 정보
    @Column("interpolation_type")
    val interpolationType: String? = null,

    @Column("interpolation_accuracy")
    val interpolationAccuracy: Double? = null,

    // 위성 위치 (참조)
    @Column("satellite_range")
    val satelliteRange: Double? = null,

    @Column("satellite_altitude")
    val satelliteAltitude: Double? = null,

    @Column("satellite_velocity")
    val satelliteVelocity: Double? = null,

    // CMD/Position
    @Column("cmd_azimuth")
    val cmdAzimuth: Double? = null,

    @Column("cmd_elevation")
    val cmdElevation: Double? = null,

    @Column("cmd_train")
    val cmdTrain: Double? = null,

    @Column("position_azimuth")
    val positionAzimuth: Double? = null,

    @Column("position_elevation")
    val positionElevation: Double? = null,

    @Column("position_train")
    val positionTrain: Double? = null,

    @Column("created_at")
    val createdAt: OffsetDateTime? = null
)
