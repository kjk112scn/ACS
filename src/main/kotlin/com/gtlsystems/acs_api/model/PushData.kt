package com.gtlsystems.acs_api.model

import java.time.ZonedDateTime

object PushData {
    data class CMDValue (
        var cmdAzimuthAngle: Float? = null,
        var cmdElevationAngle: Float? = null,
        var cmdTiltAngle: Float? = null,
        var cmdTime: ZonedDateTime? = null
    )
    data class TrackingStatus (
        var ephemerisStatus: Boolean? = null,
        var passScheduleStatus: Boolean? = null,
        var sunTrackStatus: Boolean? = null,
    )
    data class ReadData(
        val modeStatusBits: String? = null,
        val azimuthAngle: Float? = null,
        val elevationAngle: Float? = null,
        val tiltAngle: Float? = null,
        val azimuthSpeed: Float? = null,
        val elevationSpeed: Float? = null,
        val tiltSpeed: Float? = null,
        val servoDriverAzimuthAngle: Float? = null,
        val servoDriverElevationAngle: Float? = null,
        val servoDriverTiltAngle: Float? = null,
        val torqueAzimuth: Float? = null,
        val torqueElevation: Float? = null,
        val torqueTilt: Float? = null,
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
        val tiltBoardServoStatusBits: String? = null,
        val tiltBoardStatusBits: String? = null,
        val feedSBoardStatusBits: String? = null,
        val feedXBoardStatusBits: String? = null,
        val currentSBandLNALHCP: Float? = null,
        val currentSBandLNARHCP: Float? = null,
        val currentXBandLNALHCP: Float? = null,
        val currentXBandLNARHCP: Float? = null,
        val rssiSBandLNALHCP: Float? = null,
        val rssiSBandLNARHCP: Float? = null,
        val rssiXBandLNALHCP: Float? = null,
        val rssiXBandLNARHCP: Float? = null,
        val azimuthAcceleration: Float? = null,
        val elevationAcceleration: Float? = null,
        val tiltAcceleration: Float? = null,
        val azimuthMaxAcceleration: Float? = null,
        val elevationMaxAcceleration: Float? = null,
        val tiltMaxAcceleration: Float? = null,
        val trackingAzimuthTime : Float? = null,
        val trackingCMDAzimuthAngle : Float? = null,
        val trackingActualAzimuthAngle : Float? = null,
        val trackingElevationTime : Float? = null,
        val trackingCMDElevationAngle : Float? = null,
        val trackingActualElevationAngle : Float? = null,
        val trackingTiltTime : Float? = null,
        val trackingCMDTiltAngle : Float? = null,
        val trackingActualTiltAngle : Float? = null
    )
    val CMD = CMDValue()
    val TRACKING_STATUS = TrackingStatus() // ✅ 인스턴스 객체
}