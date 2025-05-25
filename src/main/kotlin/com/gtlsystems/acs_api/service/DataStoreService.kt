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
            currentSBandLNA_LHCP = newData.currentSBandLNA_LHCP ?: currentData.currentSBandLNA_LHCP,
            currentSBandLNA_RHCP = newData.currentSBandLNA_RHCP ?: currentData.currentSBandLNA_RHCP,
            currentXBandLNA_LHCP = newData.currentXBandLNA_LHCP ?: currentData.currentXBandLNA_LHCP,
            currentXBandLNA_RHCP = newData.currentXBandLNA_RHCP ?: currentData.currentXBandLNA_RHCP,
            rssiSBandLNA_LHCP = newData.rssiSBandLNA_LHCP ?: currentData.rssiSBandLNA_LHCP,
            rssiSBandLNA_RHCP = newData.rssiSBandLNA_RHCP ?: currentData.rssiSBandLNA_RHCP,
            rssiXBandLNA_LHCP = newData.rssiXBandLNA_LHCP ?: currentData.rssiXBandLNA_LHCP,
            rssiXBandLNA_RHCP = newData.rssiXBandLNA_RHCP ?: currentData.rssiXBandLNA_RHCP
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
            data.currentSBandLNA_LHCP, data.currentSBandLNA_RHCP,
            data.currentXBandLNA_LHCP, data.currentXBandLNA_RHCP,
            data.rssiSBandLNA_LHCP, data.rssiSBandLNA_RHCP,
            data.rssiXBandLNA_LHCP, data.rssiXBandLNA_RHCP
        ).size
    }
}