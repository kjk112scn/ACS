package com.gtlsystems.acs_api.tracking.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

/**
 * ICD 제어 상태 엔티티 (Hypertable)
 * - 100ms 주기 ICD 상태 데이터 저장
 * - PRIMARY KEY 없음 (TimescaleDB Hypertable)
 * - 90일 후 자동 삭제 (retention policy)
 */
@Table("icd_status")
data class IcdStatusEntity(
    @Column("timestamp")
    val timestamp: OffsetDateTime,

    // 각도 (6개)
    @Column("azimuth_angle")
    val azimuthAngle: Float? = null,

    @Column("elevation_angle")
    val elevationAngle: Float? = null,

    @Column("train_angle")
    val trainAngle: Float? = null,

    @Column("servo_driver_azimuth_angle")
    val servoDriverAzimuthAngle: Float? = null,

    @Column("servo_driver_elevation_angle")
    val servoDriverElevationAngle: Float? = null,

    @Column("servo_driver_train_angle")
    val servoDriverTrainAngle: Float? = null,

    // 속도 (3개)
    @Column("azimuth_speed")
    val azimuthSpeed: Float? = null,

    @Column("elevation_speed")
    val elevationSpeed: Float? = null,

    @Column("train_speed")
    val trainSpeed: Float? = null,

    // 토크 (3개)
    @Column("torque_azimuth")
    val torqueAzimuth: Float? = null,

    @Column("torque_elevation")
    val torqueElevation: Float? = null,

    @Column("torque_train")
    val torqueTrain: Float? = null,

    // 가속도 (6개)
    @Column("azimuth_acceleration")
    val azimuthAcceleration: Float? = null,

    @Column("elevation_acceleration")
    val elevationAcceleration: Float? = null,

    @Column("train_acceleration")
    val trainAcceleration: Float? = null,

    @Column("azimuth_max_acceleration")
    val azimuthMaxAcceleration: Float? = null,

    @Column("elevation_max_acceleration")
    val elevationMaxAcceleration: Float? = null,

    @Column("train_max_acceleration")
    val trainMaxAcceleration: Float? = null,

    // 환경 (4개)
    @Column("wind_speed")
    val windSpeed: Float? = null,

    @Column("wind_direction")
    val windDirection: Short? = null,

    @Column("rtd_one")
    val rtdOne: Float? = null,

    @Column("rtd_two")
    val rtdTwo: Float? = null,

    // 상태 비트 (12개)
    @Column("mode_status_bits")
    val modeStatusBits: String? = null,

    @Column("main_board_protocol_status")
    val mainBoardProtocolStatus: String? = null,

    @Column("main_board_status")
    val mainBoardStatus: String? = null,

    @Column("main_board_mc_onoff")
    val mainBoardMcOnoff: String? = null,

    @Column("main_board_reserve")
    val mainBoardReserve: String? = null,

    @Column("azimuth_servo_status")
    val azimuthServoStatus: String? = null,

    @Column("azimuth_board_status")
    val azimuthBoardStatus: String? = null,

    @Column("elevation_servo_status")
    val elevationServoStatus: String? = null,

    @Column("elevation_board_status")
    val elevationBoardStatus: String? = null,

    @Column("train_servo_status")
    val trainServoStatus: String? = null,

    @Column("train_board_status")
    val trainBoardStatus: String? = null,

    @Column("feed_board_etc_status")
    val feedBoardEtcStatus: String? = null,

    // Feed 상태 (3개)
    @Column("feed_s_board_status")
    val feedSBoardStatus: String? = null,

    @Column("feed_x_board_status")
    val feedXBoardStatus: String? = null,

    @Column("feed_ka_board_status")
    val feedKaBoardStatus: String? = null,

    // LNA 전류 (6개)
    @Column("current_sband_lna_lhcp")
    val currentSbandLnaLhcp: Float? = null,

    @Column("current_sband_lna_rhcp")
    val currentSbandLnaRhcp: Float? = null,

    @Column("current_xband_lna_lhcp")
    val currentXbandLnaLhcp: Float? = null,

    @Column("current_xband_lna_rhcp")
    val currentXbandLnaRhcp: Float? = null,

    @Column("current_kaband_lna_lhcp")
    val currentKabandLnaLhcp: Float? = null,

    @Column("current_kaband_lna_rhcp")
    val currentKabandLnaRhcp: Float? = null,

    // RSSI (6개)
    @Column("rssi_sband_lna_lhcp")
    val rssiSbandLnaLhcp: Float? = null,

    @Column("rssi_sband_lna_rhcp")
    val rssiSbandLnaRhcp: Float? = null,

    @Column("rssi_xband_lna_lhcp")
    val rssiXbandLnaLhcp: Float? = null,

    @Column("rssi_xband_lna_rhcp")
    val rssiXbandLnaRhcp: Float? = null,

    @Column("rssi_kaband_lna_lhcp")
    val rssiKabandLnaLhcp: Float? = null,

    @Column("rssi_kaband_lna_rhcp")
    val rssiKabandLnaRhcp: Float? = null,

    // 추적 CMD/실측 (9개)
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
    val trackingActualTrain: Float? = null
)
