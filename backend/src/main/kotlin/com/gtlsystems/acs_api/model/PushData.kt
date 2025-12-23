package com.gtlsystems.acs_api.model

import java.time.ZonedDateTime

object PushData {
    data class CMDValue (
        var cmdAzimuthAngle: Float? = null,
        var cmdElevationAngle: Float? = null,
        var cmdTrainAngle: Float? = null,
        var cmdTime: ZonedDateTime? = null
    )
    data class TrackingStatus (
        var ephemerisStatus: Boolean? = null,           // 기존 (버튼 상태)
        var ephemerisTrackingState: String? = null,     // 신규 (상세 추적 상태)
        var passScheduleStatus: Boolean? = null,
        var passScheduleTrackingState: String? = null,  // 신규 (상세 패스 스케줄 추적 상태)
        var sunTrackStatus: Boolean? = null,
        var sunTrackTrackingState: String? = null,      // 신규 (상세 태양 추적 상태)
        var manualControlStatus: Boolean? = null,
        var geostationaryStatus: Boolean? = null
    )

    /**
     * 추적 상태 enum (단순화됨 - 6개 상태)
     *
     * 상태 흐름: IDLE → PREPARING → WAITING → TRACKING → COMPLETED
     *                                              ↘ ERROR
     */
    enum class EphemerisTrackingState {
        IDLE,           // 대기 (추적 비활성)
        PREPARING,      // 준비 중 (Train 이동 + 안정화 + Az/El 이동)
        WAITING,        // 시작 대기 (시작 시간 전, 12.1 헤더 전송 완료)
        TRACKING,       // 추적 중 (12.2 초기 데이터 전송, 실시간 추적)
        COMPLETED,      // 완료
        ERROR           // 오류
    }

    /**
     * 태양 추적 상태 enum (단순화됨)
     */
    enum class SunTrackTrackingState {
        IDLE,           // 대기 (태양 추적 정지 상태)
        PREPARING,      // 준비 중 (Train 이동 + 안정화)
        TRACKING        // 실제 추적 중
    }
    data class ReadData(
        val modeStatusBits: String? = null,
        val azimuthAngle: Float? = null,
        val elevationAngle: Float? = null,
        val trainAngle: Float? = null,
        val azimuthSpeed: Float? = null,
        val elevationSpeed: Float? = null,
        val trainSpeed: Float? = null,
        val servoDriverAzimuthAngle: Float? = null,
        val servoDriverElevationAngle: Float? = null,
        val servoDriverTrainAngle: Float? = null,
        val torqueAzimuth: Float? = null,
        val torqueElevation: Float? = null,
        val torqueTrain: Float? = null,
        val windSpeed: Float? = null,
        val windDirection: UShort? = null,
        val rtdOne: Float? = null,
        val rtdTwo: Float? = null,
        val mainBoardProtocolStatusBits: String? = null,
        val mainBoardStatusBits: String? = null,
        val mainBoardMCOnOffBits: String? = null,
        val mainBoardReserveBits: String? = null,
        val azimuthBoardServoStatusBits: String? = null,
        val azimuthBoardStatusBits: String? = null,
        val elevationBoardServoStatusBits: String? = null,
        val elevationBoardStatusBits: String? = null,
        val trainBoardServoStatusBits: String? = null,
        val trainBoardStatusBits: String? = null,
        val feedBoardETCStatusBits: String? = null,
        val feedSBoardStatusBits: String? = null,
        val feedXBoardStatusBits: String? = null,
        val feedKaBoardStatusBits: String? = null,
        val currentSBandLNALHCP: Float? = null,
        val currentSBandLNARHCP: Float? = null,
        val currentXBandLNALHCP: Float? = null,
        val currentXBandLNARHCP: Float? = null,
        val currentKaBandLNALHCP: Float? = null,
        val currentKaBandLNARHCP: Float? = null,
        val rssiSBandLNALHCP: Float? = null,
        val rssiSBandLNARHCP: Float? = null,
        val rssiXBandLNALHCP: Float? = null,
        val rssiXBandLNARHCP: Float? = null,
        val rssiKaBandLNALHCP: Float? = null,
        val rssiKaBandLNARHCP: Float? = null,
        val azimuthAcceleration: Float? = null,
        val elevationAcceleration: Float? = null,
        val trainAcceleration: Float? = null,
        val azimuthMaxAcceleration: Float? = null,
        val elevationMaxAcceleration: Float? = null,
        val trainMaxAcceleration: Float? = null,
        val trackingAzimuthTime : Float? = null,
        val trackingCMDAzimuthAngle : Float? = null,
        val trackingActualAzimuthAngle : Float? = null,
        val trackingElevationTime : Float? = null,
        val trackingCMDElevationAngle : Float? = null,
        val trackingActualElevationAngle : Float? = null,
        val trackingTrainTime : Float? = null,
        val trackingCMDTrainAngle : Float? = null,
        val trackingActualTrainAngle : Float? = null
    )
    // ✅ 인스턴스 객체
    val CMD = CMDValue()
    val TRACKING_STATUS = TrackingStatus(ephemerisTrackingState = "IDLE", passScheduleTrackingState = "IDLE") // ✅ 초기 상태 설정
    val READ_DATA = ReadData() // ✅ 추가 필요!
}