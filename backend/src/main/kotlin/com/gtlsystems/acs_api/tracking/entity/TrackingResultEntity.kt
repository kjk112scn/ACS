package com.gtlsystems.acs_api.tracking.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * 실측 추적 결과 엔티티 (Hypertable)
 * - 실제 추적 결과 데이터 저장 (실측치만)
 * - 이론치는 tracking_trajectory에서 JOIN으로 조회
 * - PRIMARY KEY 없음 (TimescaleDB Hypertable)
 *
 * V006 재설계:
 * - 이론치 컬럼 제거 (trajectory에서 관리)
 * - ICD 추적 데이터 추가
 * - 정밀 추적 메타데이터 추가
 */
@Table("tracking_result")
data class TrackingResultEntity(
    @Column("timestamp")
    val timestamp: OffsetDateTime,

    @Column("session_id")
    val sessionId: Long,

    // 인덱스
    @Column("index")
    val index: Int,

    @Column("theoretical_index")
    val theoreticalIndex: Int? = null,

    // ===== 정밀 추적 메타데이터 (V006) =====
    @Column("theoretical_timestamp")
    val theoreticalTimestamp: OffsetDateTime? = null,

    @Column("time_offset_ms")
    val timeOffsetMs: Double? = null,

    @Column("interpolation_fraction")
    val interpolationFraction: Double? = null,

    @Column("lower_theoretical_index")
    val lowerTheoreticalIndex: Int? = null,

    @Column("upper_theoretical_index")
    val upperTheoreticalIndex: Int? = null,

    // ===== 명령값 =====
    @Column("cmd_azimuth")
    val cmdAzimuth: Double? = null,

    @Column("cmd_elevation")
    val cmdElevation: Double? = null,

    @Column("cmd_train")
    val cmdTrain: Double? = null,

    // ===== 실측값 =====
    @Column("actual_azimuth")
    val actualAzimuth: Double? = null,

    @Column("actual_elevation")
    val actualElevation: Double? = null,

    @Column("actual_train")
    val actualTrain: Double? = null,

    // ===== 위치값 =====
    @Column("position_azimuth")
    val positionAzimuth: Double? = null,

    @Column("position_elevation")
    val positionElevation: Double? = null,

    @Column("position_train")
    val positionTrain: Double? = null,

    // ===== ICD 추적 데이터 (V006) =====
    @Column("tracking_azimuth_time")
    val trackingAzimuthTime: Float? = null,

    @Column("tracking_cmd_azimuth")
    val trackingCmdAzimuth: Float? = null,

    @Column("tracking_actual_azimuth")
    val trackingActualAzimuth: Float? = null,

    @Column("tracking_elevation_time")
    val trackingElevationTime: Float? = null,

    @Column("tracking_cmd_elevation")
    val trackingCmdElevation: Float? = null,

    @Column("tracking_actual_elevation")
    val trackingActualElevation: Float? = null,

    @Column("tracking_train_time")
    val trackingTrainTime: Float? = null,

    @Column("tracking_cmd_train")
    val trackingCmdTrain: Float? = null,

    @Column("tracking_actual_train")
    val trackingActualTrain: Float? = null,

    // ===== 오차 =====
    @Column("azimuth_error")
    val azimuthError: Double? = null,

    @Column("elevation_error")
    val elevationError: Double? = null,

    @Column("train_error")
    val trainError: Double? = null,

    @Column("total_error")
    val totalError: Double? = null,

    // ===== 상태 =====
    @Column("keyhole_active")
    val keyholeActive: Boolean = false,

    @Column("keyhole_optimized")
    val keyholeOptimized: Boolean = false,

    @Column("tracking_quality")
    val trackingQuality: String? = null,

    @Column("interpolation_accuracy")
    val interpolationAccuracy: Double? = null,

    // ===== 칼만 필터 (V006 - 향후 확장) =====
    @Column("kalman_azimuth")
    val kalmanAzimuth: Double? = null,

    @Column("kalman_elevation")
    val kalmanElevation: Double? = null,

    @Column("kalman_gain")
    val kalmanGain: Double? = null,

    @Column("created_at")
    val createdAt: OffsetDateTime? = null
)