package com.gtlsystems.acs_api.model

import java.time.LocalDateTime
import java.util.BitSet

class PushReadStatusData {
    data class ReadData(
        var modeStatusBits: BitSet = BitSet(8),
        var azimuthAngle: Float = 1f,
        var elevationAngle: Float = 0f,
        var tiltAngle: Float = 0f,
        var azimuthSpeed: Float = 0f,
        var elevationSpeed: Float = 0f,
        var tiltSpeed: Float = 0f,
        var servoDriverAzimuthAngle: Float = 0f,
        var servoDriverElevationAngle: Float = 0f,
        var servoDriverTiltAngle: Float = 0f,
        var torqueAzimuth: Float = 0f,
        var torqueElevation: Float = 0f,
        var torqueTilt: Float = 0f,
        var windSpeed: Float = 0f,
        var windDirection: UShort = 0u,
        var rtdOne: Float = 0f,
        var rtdTwo: Float = 0f,
        var mainBoardProtocolStatusBits: BitSet = BitSet(8),
        var mainBoardStatusBits: BitSet = BitSet(8),
        var mainBoardMCOnOffBits: BitSet = BitSet(8),
        var mainBoardReserveBits: BitSet = BitSet(8),
        var azimuthBoardServoStatusBits: BitSet = BitSet(8),
        var azimuthBoardStatusBits: BitSet = BitSet(8),
        var elevationBoardServoStatusBits: BitSet = BitSet(8),
        var elevationBoardStatusBits: BitSet = BitSet(8),
        var tiltBoardServoStatusBits: BitSet = BitSet(8),
        var tiltBoardStatusBits: BitSet = BitSet(8),
        var feedSBoardStatusBits: BitSet = BitSet(8),
        var feedXBoardStatusBits: BitSet = BitSet(8),
        var currentSBandLNA_LHCP: Float = 0f,
        var currentSBandLNA_RHCP: Float = 0f,
        var currentXBandLNA_LHCP: Float = 0f,
        var currentXBandLNA_RHCP: Float = 0f,
        var rssiSBandLNA_LHCP: Float = 0f,
        var rssiSBandLNA_RHCP: Float = 0f,
        var rssiXBandLNA_LHCP: Float = 0f,
        var rssiXBandLNA_RHCP: Float = 0f,
    )
}