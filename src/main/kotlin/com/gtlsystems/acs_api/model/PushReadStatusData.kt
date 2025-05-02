package com.gtlsystems.acs_api.model

import java.time.LocalDateTime
import java.util.BitSet

class PushReadStatusData {
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
        val currentSBandLNA_LHCP: Float? = null,
        val currentSBandLNA_RHCP: Float? = null,
        val currentXBandLNA_LHCP: Float? = null,
        val currentXBandLNA_RHCP: Float? = null,
        val rssiSBandLNA_LHCP: Float? = null,
        val rssiSBandLNA_RHCP: Float? = null,
        val rssiXBandLNA_LHCP: Float? = null,
        val rssiXBandLNA_RHCP: Float? = null
    )
}