package com.gtlsystems.acs_api.tracking.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * 추적 세션 엔티티
 * - 패스별 요약 정보 저장
 * - FK 부모 테이블 (trajectory, result의 부모)
 * - Select Schedule에서 표시하는 모든 변환 단계별 메타데이터 포함
 *
 * 변환 파이프라인:
 *   Original (2축)
 *     → FinalTransformed (3축, Train=0, ±270°)
 *       → KeyholeAxisTransformed (3축, Train≠0) [Keyhole 발생 시]
 *         → KeyholeFinalTransformed (3축, Train≠0, ±270°)
 *           → KeyholeOptimizedFinalTransformed (최적화 Train, ±270°)
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

    // ===== 시간 정보 =====
    @Column("start_time")
    val startTime: OffsetDateTime,

    @Column("end_time")
    val endTime: OffsetDateTime,

    @Column("duration")
    val duration: Int? = null,  // 초 단위

    @Column("max_elevation_time")
    val maxElevationTime: OffsetDateTime? = null,

    // ===== 기본 각도 정보 (최종 사용값) =====
    @Column("start_azimuth")
    val startAzimuth: Double? = null,

    @Column("end_azimuth")
    val endAzimuth: Double? = null,

    @Column("start_elevation")
    val startElevation: Double? = null,

    @Column("end_elevation")
    val endElevation: Double? = null,

    @Column("train_angle")
    val trainAngle: Double? = null,

    // ===== 기본 Peak 값 =====
    @Column("max_elevation")
    val maxElevation: Double? = null,

    @Column("max_azimuth_rate")
    val maxAzimuthRate: Double? = null,

    @Column("max_elevation_rate")
    val maxElevationRate: Double? = null,

    @Column("max_azimuth_accel")
    val maxAzimuthAccel: Double? = null,

    @Column("max_elevation_accel")
    val maxElevationAccel: Double? = null,

    @Column("keyhole_detected")
    val keyholeDetected: Boolean = false,

    @Column("recommended_train_angle")
    val recommendedTrainAngle: Double? = null,

    @Column("total_points")
    val totalPoints: Int? = null,

    // ===== Original (2축) 메타데이터 =====
    @Column("original_start_azimuth")
    val originalStartAzimuth: Double? = null,

    @Column("original_end_azimuth")
    val originalEndAzimuth: Double? = null,

    @Column("original_max_elevation")
    val originalMaxElevation: Double? = null,

    @Column("original_max_az_rate")
    val originalMaxAzRate: Double? = null,

    @Column("original_max_el_rate")
    val originalMaxElRate: Double? = null,

    // ===== FinalTransformed (3축, Train=0, ±270°) =====
    @Column("final_start_azimuth")
    val finalStartAzimuth: Double? = null,

    @Column("final_end_azimuth")
    val finalEndAzimuth: Double? = null,

    @Column("final_start_elevation")
    val finalStartElevation: Double? = null,

    @Column("final_end_elevation")
    val finalEndElevation: Double? = null,

    @Column("final_max_elevation")
    val finalMaxElevation: Double? = null,

    @Column("final_max_az_rate")
    val finalMaxAzRate: Double? = null,

    @Column("final_max_el_rate")
    val finalMaxElRate: Double? = null,

    // ===== KeyholeAxisTransformed (3축, Train≠0, 각도 제한 전) =====
    @Column("keyhole_axis_max_az_rate")
    val keyholeAxisMaxAzRate: Double? = null,

    @Column("keyhole_axis_max_el_rate")
    val keyholeAxisMaxElRate: Double? = null,

    // ===== KeyholeFinalTransformed (3축, Train≠0, ±270°) =====
    @Column("keyhole_final_start_azimuth")
    val keyholeFinalStartAzimuth: Double? = null,

    @Column("keyhole_final_end_azimuth")
    val keyholeFinalEndAzimuth: Double? = null,

    @Column("keyhole_final_start_elevation")
    val keyholeFinalStartElevation: Double? = null,

    @Column("keyhole_final_end_elevation")
    val keyholeFinalEndElevation: Double? = null,

    @Column("keyhole_final_max_elevation")
    val keyholeFinalMaxElevation: Double? = null,

    @Column("keyhole_final_max_az_rate")
    val keyholeFinalMaxAzRate: Double? = null,

    @Column("keyhole_final_max_el_rate")
    val keyholeFinalMaxElRate: Double? = null,

    // ===== KeyholeOptimizedFinalTransformed (최적화 Train, ±270°) =====
    @Column("keyhole_opt_start_azimuth")
    val keyholeOptStartAzimuth: Double? = null,

    @Column("keyhole_opt_end_azimuth")
    val keyholeOptEndAzimuth: Double? = null,

    @Column("keyhole_opt_start_elevation")
    val keyholeOptStartElevation: Double? = null,

    @Column("keyhole_opt_end_elevation")
    val keyholeOptEndElevation: Double? = null,

    @Column("keyhole_opt_max_elevation")
    val keyholeOptMaxElevation: Double? = null,

    @Column("keyhole_opt_max_az_rate")
    val keyholeOptMaxAzRate: Double? = null,

    @Column("keyhole_opt_max_el_rate")
    val keyholeOptMaxElRate: Double? = null,

    // ===== TLE 연동 (V006) =====
    @Column("tle_cache_id")
    val tleCacheId: Long? = null,

    @Column("tle_line_1")
    val tleLine1: String? = null,

    @Column("tle_line_2")
    val tleLine2: String? = null,

    @Column("tle_epoch")
    val tleEpoch: OffsetDateTime? = null,

    // ===== 시스템 필드 =====
    @Column("created_at")
    val createdAt: OffsetDateTime? = null
)
