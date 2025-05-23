package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.model.PushData
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference
import java.time.Instant

@Service
class DataStoreService {
    // 최신 데이터 저장
    private val latestData = AtomicReference(PushData.ReadData())

    // 마지막 UDP 업데이트 시간
    private val lastUdpUpdateTime = AtomicReference(Instant.now())

    // UDP 연결 상태
    private val udpConnected = AtomicReference(false)

    // 데이터 업데이트 (UDP에서 호출)
    fun updateDataFromUdp(newData: PushData.ReadData) {
        // 기존 데이터와 새 데이터를 병합
        val currentData = latestData.get()

        // 새 데이터의 null이 아닌 필드만 업데이트
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
            currentSBandLNA_LHCP = newData.currentSBandLNA_LHCP ?: currentData.currentSBandLNA_LHCP,
            currentSBandLNA_RHCP = newData.currentSBandLNA_RHCP ?: currentData.currentSBandLNA_RHCP,
            currentXBandLNA_LHCP = newData.currentXBandLNA_LHCP ?: currentData.currentXBandLNA_LHCP,
            currentXBandLNA_RHCP = newData.currentXBandLNA_RHCP ?: currentData.currentXBandLNA_RHCP,
            rssiSBandLNA_LHCP = newData.rssiSBandLNA_LHCP ?: currentData.rssiSBandLNA_LHCP,
            rssiSBandLNA_RHCP = newData.rssiSBandLNA_RHCP ?: currentData.rssiSBandLNA_RHCP,
            rssiXBandLNA_LHCP = newData.rssiXBandLNA_LHCP ?: currentData.rssiXBandLNA_LHCP,
            rssiXBandLNA_RHCP = newData.rssiXBandLNA_RHCP ?: currentData.rssiXBandLNA_RHCP
        )

        // 데이터 업데이트
        latestData.set(mergedData)

        // 마지막 업데이트 시간 갱신
        lastUdpUpdateTime.set(Instant.now())

        // UDP 연결 상태 업데이트
        udpConnected.set(true)
    }

    // 최신 데이터 가져오기 (푸시 서비스에서 호출)
    fun getLatestData(): PushData.ReadData {
        return latestData.get()
    }

    // UDP 연결 상태 확인
    fun isUdpConnected(): Boolean {
        // 마지막 업데이트 후 5초가 지나면 연결 끊김으로 간주
        val timeoutSeconds = 5L
        val now = Instant.now()
        val lastUpdate = lastUdpUpdateTime.get()

        val connected = now.minusSeconds(timeoutSeconds).isBefore(lastUpdate)
        udpConnected.set(connected)
        return connected
    }

    // UDP 연결 상태 수동 설정
    fun setUdpConnectionStatus(connected: Boolean) {
        udpConnected.set(connected)
        if (connected) {
            lastUdpUpdateTime.set(Instant.now())
        }
    }

    // 마지막 UDP 업데이트 시간 가져오기
    fun getLastUdpUpdateTime(): Instant {
        return lastUdpUpdateTime.get()
    }
}