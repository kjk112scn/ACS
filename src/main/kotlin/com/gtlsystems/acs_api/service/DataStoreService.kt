package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.model.PushData
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

@Service
class DataStoreService {

    // === 최적화된 데이터 저장 ===
    private val latestData = AtomicReference(PushData.ReadData())
    private val dataVersion = AtomicLong(0) // 버전 기반 변경 감지

    private val trackingStatus = AtomicReference(PushData.TrackingStatus())

    // === UDP 연결 상태 관리 ===
    private val lastUdpUpdateTime = AtomicReference(Instant.now())
    private val udpConnected = AtomicReference(false)

    /**
     * ✅ UDP에서 데이터 업데이트 (mergedData 로직 복원)
     * - 새 데이터의 null이 아닌 필드만 업데이트
     * - 기존 데이터 보존 (null 필드는 덮어쓰지 않음)
     */
    fun updateDataFromUdp(newData: PushData.ReadData) {
        val currentData = latestData.get()

        // 🔄 기존 mergedData 로직 복원 (null 안전 병합)
        val mergedData = PushData.ReadData(
            modeStatusBits = newData.modeStatusBits ?: currentData.modeStatusBits,
            azimuthAngle = newData.azimuthAngle ?: currentData.azimuthAngle,
            elevationAngle = newData.elevationAngle ?: currentData.elevationAngle,
            tiltAngle = newData.tiltAngle ?: currentData.tiltAngle,
            azimuthSpeed = newData.azimuthSpeed ?: currentData.azimuthSpeed,
            elevationSpeed = newData.elevationSpeed ?: currentData.elevationSpeed,
            tiltSpeed = newData.tiltSpeed ?: currentData.tiltSpeed,
            servoDriverAzimuthAngle = newData.servoDriverAzimuthAngle ?: currentData.servoDriverAzimuthAngle,
            servoDriverElevationAngle = newData.servoDriverElevationAngle ?: currentData.servoDriverElevationAngle,
            servoDriverTiltAngle = newData.servoDriverTiltAngle ?: currentData.servoDriverTiltAngle,
            torqueAzimuth = newData.torqueAzimuth ?: currentData.torqueAzimuth,
            torqueElevation = newData.torqueElevation ?: currentData.torqueElevation,
            torqueTilt = newData.torqueTilt ?: currentData.torqueTilt,
            windSpeed = newData.windSpeed ?: currentData.windSpeed,
            windDirection = newData.windDirection ?: currentData.windDirection,
            rtdOne = newData.rtdOne ?: currentData.rtdOne,
            rtdTwo = newData.rtdTwo ?: currentData.rtdTwo,
            mainBoardProtocolStatusBits = newData.mainBoardProtocolStatusBits ?: currentData.mainBoardProtocolStatusBits,
            mainBoardStatusBits = newData.mainBoardStatusBits ?: currentData.mainBoardStatusBits,
            mainBoardMCOnOffBits = newData.mainBoardMCOnOffBits ?: currentData.mainBoardMCOnOffBits,
            mainBoardReserveBits = newData.mainBoardReserveBits ?: currentData.mainBoardReserveBits,
            azimuthBoardServoStatusBits = newData.azimuthBoardServoStatusBits ?: currentData.azimuthBoardServoStatusBits,
            azimuthBoardStatusBits = newData.azimuthBoardStatusBits ?: currentData.azimuthBoardStatusBits,
            elevationBoardServoStatusBits = newData.elevationBoardServoStatusBits ?: currentData.elevationBoardServoStatusBits,
            elevationBoardStatusBits = newData.elevationBoardStatusBits ?: currentData.elevationBoardStatusBits,
            tiltBoardServoStatusBits = newData.tiltBoardServoStatusBits ?: currentData.tiltBoardServoStatusBits,
            tiltBoardStatusBits = newData.tiltBoardStatusBits ?: currentData.tiltBoardStatusBits,
            feedSBoardStatusBits = newData.feedSBoardStatusBits ?: currentData.feedSBoardStatusBits,
            feedXBoardStatusBits = newData.feedXBoardStatusBits ?: currentData.feedXBoardStatusBits,
            currentSBandLNALHCP = newData.currentSBandLNALHCP ?: currentData.currentSBandLNALHCP,
            currentSBandLNARHCP = newData.currentSBandLNARHCP ?: currentData.currentSBandLNARHCP,
            currentXBandLNALHCP = newData.currentXBandLNALHCP ?: currentData.currentXBandLNALHCP,
            currentXBandLNARHCP = newData.currentXBandLNARHCP ?: currentData.currentXBandLNARHCP,
            rssiSBandLNALHCP = newData.rssiSBandLNALHCP ?: currentData.rssiSBandLNALHCP,
            rssiSBandLNARHCP = newData.rssiSBandLNARHCP ?: currentData.rssiSBandLNARHCP,
            rssiXBandLNALHCP = newData.rssiXBandLNALHCP ?: currentData.rssiXBandLNALHCP,
            rssiXBandLNARHCP = newData.rssiXBandLNARHCP ?: currentData.rssiXBandLNARHCP,
            azimuthAcceleration = newData.azimuthAcceleration ?: currentData.azimuthAcceleration,
            elevationAcceleration = newData.elevationAcceleration ?: currentData.elevationAcceleration,
            tiltAcceleration = newData.tiltAcceleration ?: currentData.tiltAcceleration,
            azimuthMaxAcceleration = newData.azimuthMaxAcceleration ?: currentData.azimuthMaxAcceleration,
            elevationMaxAcceleration = newData.elevationMaxAcceleration ?: currentData.elevationMaxAcceleration,
            tiltMaxAcceleration = newData.tiltMaxAcceleration ?: currentData.tiltMaxAcceleration,
            trackingAzimuthTime = newData.trackingAzimuthTime ?: currentData.trackingAzimuthTime,
            trackingCMDAzimuthAngle = newData.trackingCMDAzimuthAngle ?: currentData.trackingCMDAzimuthAngle,
            trackingActualAzimuthAngle = newData.trackingActualAzimuthAngle ?: currentData.trackingActualAzimuthAngle,
            trackingElevationTime = newData.trackingElevationTime ?: currentData.trackingElevationTime,
            trackingCMDElevationAngle = newData.trackingCMDElevationAngle ?: currentData.trackingCMDElevationAngle,
            trackingActualElevationAngle = newData.trackingActualElevationAngle ?: currentData.trackingActualElevationAngle,
            trackingTiltTime = newData.trackingTiltTime ?: currentData.trackingTiltTime,
            trackingCMDTiltAngle = newData.trackingCMDTiltAngle ?: currentData.trackingCMDTiltAngle,
            trackingActualTiltAngle = newData.trackingActualTiltAngle ?: currentData.trackingActualTiltAngle,
        )

        // ⚡ 최적화: 실제로 변경된 경우에만 업데이트
        if (!isDataEqual(currentData, mergedData)) {
            latestData.set(mergedData)
            dataVersion.incrementAndGet() // 버전 증가

            // 연결 상태 업데이트
            lastUdpUpdateTime.set(Instant.now())
            udpConnected.set(true)
        }
    }
    /**
     * ✅ 전체 추적 데이터를 Map으로 반환하는 메서드 추가
     */
    fun getReadData(): Map<String, Any?> {
        val data = latestData.get()
        return mapOf(
            "modeStatusBits" to data.modeStatusBits,
            "azimuthAngle" to data.azimuthAngle,
            "elevationAngle" to data.elevationAngle,
            "tiltAngle" to data.tiltAngle,
            "azimuthSpeed" to data.azimuthSpeed,
            "elevationSpeed" to data.elevationSpeed,
            "tiltSpeed" to data.tiltSpeed,
            "servoDriverAzimuthAngle" to data.servoDriverAzimuthAngle,
            "servoDriverElevationAngle" to data.servoDriverElevationAngle,
            "servoDriverTiltAngle" to data.servoDriverTiltAngle,
            "torqueAzimuth" to data.torqueAzimuth,
            "torqueElevation" to data.torqueElevation,
            "torqueTilt" to data.torqueTilt,
            "windSpeed" to data.windSpeed,
            "windDirection" to data.windDirection,
            "rtdOne" to data.rtdOne,
            "rtdTwo" to data.rtdTwo,
            "mainBoardProtocolStatusBits" to data.mainBoardProtocolStatusBits,
            "mainBoardStatusBits" to data.mainBoardStatusBits,
            "mainBoardMCOnOffBits" to data.mainBoardMCOnOffBits,
            "mainBoardReserveBits" to data.mainBoardReserveBits,
            "azimuthBoardServoStatusBits" to data.azimuthBoardServoStatusBits,
            "azimuthBoardStatusBits" to data.azimuthBoardStatusBits,
            "elevationBoardServoStatusBits" to data.elevationBoardServoStatusBits,
            "elevationBoardStatusBits" to data.elevationBoardStatusBits,
            "tiltBoardServoStatusBits" to data.tiltBoardServoStatusBits,
            "tiltBoardStatusBits" to data.tiltBoardStatusBits,
            "feedSBoardStatusBits" to data.feedSBoardStatusBits,
            "feedXBoardStatusBits" to data.feedXBoardStatusBits,
            "currentSBandLNALHCP" to data.currentSBandLNALHCP,
            "currentSBandLNARHCP" to data.currentSBandLNARHCP,
            "currentXBandLNALHCP" to data.currentXBandLNALHCP,
            "currentXBandLNARHCP" to data.currentXBandLNARHCP,
            "rssiSBandLNALHCP" to data.rssiSBandLNALHCP,
            "rssiSBandLNARHCP" to data.rssiSBandLNARHCP,
            "rssiXBandLNALHCP" to data.rssiXBandLNALHCP,
            "rssiXBandLNARHCP" to data.rssiXBandLNARHCP,
            "azimuthAcceleration" to data.azimuthAcceleration,
            "elevationAcceleration" to data.elevationAcceleration,
            "tiltAcceleration" to data.tiltAcceleration,
            "azimuthMaxAcceleration" to data.azimuthMaxAcceleration,
            "elevationMaxAcceleration" to data.elevationMaxAcceleration,
            "tiltMaxAcceleration" to data.tiltMaxAcceleration,
            "trackingAzimuthTime" to data.trackingAzimuthTime,
            "trackingCMDAzimuthAngle" to data.trackingCMDAzimuthAngle,
            "trackingActualAzimuthAngle" to data.trackingActualAzimuthAngle,
            "trackingElevationTime" to data.trackingElevationTime,
            "trackingCMDElevationAngle" to data.trackingCMDElevationAngle,
            "trackingActualElevationAngle" to data.trackingActualElevationAngle,
            "trackingTiltTime" to data.trackingTiltTime,
            "trackingCMDTiltAngle" to data.trackingCMDTiltAngle,
            "trackingActualTiltAngle" to data.trackingActualTiltAngle
        )
    }
    /**
     * ✅ 추적 관련 데이터만 별도로 반환
     */
    fun getTrackingOnlyData(): Map<String, Float?> {
        val data = latestData.get()
        return mapOf(
            "trackingAzimuthTime" to data.trackingAzimuthTime,
            "trackingCMDAzimuthAngle" to data.trackingCMDAzimuthAngle,
            "trackingActualAzimuthAngle" to data.trackingActualAzimuthAngle,
            "trackingElevationTime" to data.trackingElevationTime,
            "trackingCMDElevationAngle" to data.trackingCMDElevationAngle,
            "trackingActualElevationAngle" to data.trackingActualElevationAngle,
            "trackingTiltTime" to data.trackingTiltTime,
            "trackingCMDTiltAngle" to data.trackingCMDTiltAngle,
            "trackingActualTiltAngle" to data.trackingActualTiltAngle
        )
    }
    /**
     * ✅ TrackingStatus 업데이트
     */
    // 기존 updateTrackingStatus 메서드는 그대로 유지
    fun updateTrackingStatus(newStatus: PushData.TrackingStatus) {
        val currentStatus = trackingStatus.get()

        val mergedStatus = PushData.TrackingStatus(
            ephemerisStatus = newStatus.ephemerisStatus ?: currentStatus.ephemerisStatus,
            passScheduleStatus = newStatus.passScheduleStatus ?: currentStatus.passScheduleStatus,
            sunTrackStatus = newStatus.sunTrackStatus ?: currentStatus.sunTrackStatus
        )

        trackingStatus.set(mergedStatus)

        // PushData 전역 객체와 동기화
        PushData.TRACKING_STATUS.ephemerisStatus = mergedStatus.ephemerisStatus
        PushData.TRACKING_STATUS.passScheduleStatus = mergedStatus.passScheduleStatus
        PushData.TRACKING_STATUS.sunTrackStatus = mergedStatus.sunTrackStatus

        dataVersion.incrementAndGet()
    }
    /**
     * ✅ 상호 배타적 추적 상태 업데이트 (하나만 true, 나머지는 false)
     */
    fun setEphemerisTracking(active: Boolean) {
        val newStatus = PushData.TrackingStatus(
            ephemerisStatus = active,
            passScheduleStatus = false,
            sunTrackStatus = false
        )
        updateTrackingStatus(newStatus)
    }

    fun setPassScheduleTracking(active: Boolean) {
        val newStatus = PushData.TrackingStatus(
            ephemerisStatus = false,
            passScheduleStatus = active,
            sunTrackStatus = false
        )
        updateTrackingStatus(newStatus)
    }

    fun setSunTracking(active: Boolean) {
        val newStatus = PushData.TrackingStatus(
            ephemerisStatus = false,
            passScheduleStatus = false,
            sunTrackStatus = active
        )
        updateTrackingStatus(newStatus)
    }

    fun stopAllTracking() {
        val newStatus = PushData.TrackingStatus(
            ephemerisStatus = false,
            passScheduleStatus = false,
            sunTrackStatus = false
        )
        updateTrackingStatus(newStatus)
    }


    /**
     * ✅ 데이터 동등성 체크 (성능 최적화)
     * - 실제 변경이 있을 때만 업데이트
     */
    private fun isDataEqual(data1: PushData.ReadData, data2: PushData.ReadData): Boolean {
        return data1.azimuthAngle == data2.azimuthAngle &&
               data1.elevationAngle == data2.elevationAngle &&
               data1.tiltAngle == data2.tiltAngle &&
               data1.azimuthSpeed == data2.azimuthSpeed &&
               data1.elevationSpeed == data2.elevationSpeed &&
               data1.tiltSpeed == data2.tiltSpeed &&
               data1.modeStatusBits == data2.modeStatusBits &&
               data1.windSpeed == data2.windSpeed &&
               data1.windDirection == data2.windDirection
               // 주요 필드들만 체크 (성능 고려)
    }

    /**
     * ✅ 최신 데이터 가져오기 (버전 정보 포함)
     */
    fun getLatestData(): PushData.ReadData {
        return latestData.get()
    }

    /**
     * ✅ 데이터 버전 확인 (변경 감지용)
     */
    fun getDataVersion(): Long {
        return dataVersion.get()
    }

    /**
     * ✅ 데이터 변경 여부 체크 (PushService 최적화용)
     */
    fun hasDataChanged(lastKnownVersion: Long): Boolean {
        return dataVersion.get() > lastKnownVersion
    }

    /**
     * ✅ UDP 연결 상태 확인
     */
    fun isUdpConnected(): Boolean {
        val timeoutSeconds = 5L
        val now = Instant.now()
        val lastUpdate = lastUdpUpdateTime.get()

        val connected = now.minusSeconds(timeoutSeconds).isBefore(lastUpdate)
        udpConnected.set(connected)
        return connected
    }

    /**
     * ✅ UDP 연결 상태 수동 설정
     */
    fun setUdpConnectionStatus(connected: Boolean) {
        udpConnected.set(connected)
        if (connected) {
            lastUdpUpdateTime.set(Instant.now())
        }
    }

    /**
     * ✅ 마지막 UDP 업데이트 시간 가져오기
     */
    fun getLastUdpUpdateTime(): Instant {
        return lastUdpUpdateTime.get()
    }

    /**
     * ✅ 상태 정보 조회
     */
    fun getStatusInfo(): Map<String, Any> {
        val currentData = latestData.get()
        return mapOf(
            "dataVersion" to dataVersion.get(),
            "lastUpdateTime" to lastUdpUpdateTime.get(),
            "isUdpConnected" to isUdpConnected(),
            "hasValidData" to (currentData.azimuthAngle != null),
            "nonNullFields" to countNonNullFields(currentData),
            "architecture" to "Optimized with Null-Safe Merging"
        )
    }

    /**
     * ✅ null이 아닌 필드 개수 세기
     */
    private fun countNonNullFields(data: PushData.ReadData): Int {
        return listOfNotNull(
            data.modeStatusBits, data.azimuthAngle, data.elevationAngle, data.tiltAngle,
            data.azimuthSpeed, data.elevationSpeed, data.tiltSpeed,
            data.servoDriverAzimuthAngle, data.servoDriverElevationAngle, data.servoDriverTiltAngle,
            data.torqueAzimuth, data.torqueElevation, data.torqueTilt,
            data.windSpeed, data.windDirection, data.rtdOne, data.rtdTwo,
            data.mainBoardProtocolStatusBits, data.mainBoardStatusBits,
            data.mainBoardMCOnOffBits, data.mainBoardReserveBits,
            data.azimuthBoardServoStatusBits, data.azimuthBoardStatusBits,
            data.elevationBoardServoStatusBits, data.elevationBoardStatusBits,
            data.tiltBoardServoStatusBits, data.tiltBoardStatusBits,
            data.feedSBoardStatusBits, data.feedXBoardStatusBits,
            data.currentSBandLNALHCP, data.currentSBandLNARHCP,
            data.currentXBandLNALHCP, data.currentXBandLNARHCP,
            data.rssiSBandLNALHCP, data.rssiSBandLNARHCP,
            data.rssiXBandLNALHCP, data.rssiXBandLNARHCP
        ).size
    }
}