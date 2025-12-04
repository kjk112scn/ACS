package com.gtlsystems.acs_api.service.datastore

import com.gtlsystems.acs_api.controller.websocket.PushDataController
import com.gtlsystems.acs_api.model.PushData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Service
class DataStoreService {
    private val logger = LoggerFactory.getLogger(PushDataController::class.java)
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
        
        // ✅ 추적 시작 직후에만 상세 로그 출력 (로그 스팸 방지)
        val isTrackingActive = trackingStatus.get().ephemerisStatus == true
        val trackingCmdChanged = 
            newData.trackingCMDAzimuthAngle != currentData.trackingCMDAzimuthAngle ||
            newData.trackingCMDElevationAngle != currentData.trackingCMDElevationAngle ||
            newData.trackingCMDTrainAngle != currentData.trackingCMDTrainAngle
        
        // ✅ trackingCMD 값이 0.0에서 변경되거나, 추적 시작 직후에만 로깅
        val isZeroToNonZero = 
            (currentData.trackingCMDAzimuthAngle == 0.0f && newData.trackingCMDAzimuthAngle != null && newData.trackingCMDAzimuthAngle != 0.0f) ||
            (currentData.trackingCMDElevationAngle == 0.0f && newData.trackingCMDElevationAngle != null && newData.trackingCMDElevationAngle != 0.0f)
        
        val shouldLog = trackingCmdChanged && (isTrackingActive || isZeroToNonZero)
        
        if (shouldLog) {
            logger.info("🔍 [DEBUG-DataStore] updateDataFromUdp 호출 (추적 중 또는 값 변경):")
            logger.info("  - newData.trackingCMDAzimuthAngle: ${newData.trackingCMDAzimuthAngle}")
            logger.info("  - newData.trackingCMDElevationAngle: ${newData.trackingCMDElevationAngle}")
            logger.info("  - newData.trackingCMDTrainAngle: ${newData.trackingCMDTrainAngle}")
            logger.info("  - currentData.trackingCMDAzimuthAngle: ${currentData.trackingCMDAzimuthAngle}")
            logger.info("  - currentData.trackingCMDElevationAngle: ${currentData.trackingCMDElevationAngle}")
            logger.info("  - currentData.trackingCMDTrainAngle: ${currentData.trackingCMDTrainAngle}")
        }

        //  기존 mergedData 로직 복원 (null 안전 병합)
        val mergedData = PushData.ReadData(
            modeStatusBits = newData.modeStatusBits ?: currentData.modeStatusBits,
            azimuthAngle = newData.azimuthAngle ?: currentData.azimuthAngle,
            elevationAngle = newData.elevationAngle ?: currentData.elevationAngle,
            trainAngle = newData.trainAngle ?: currentData.trainAngle,
            azimuthSpeed = newData.azimuthSpeed ?: currentData.azimuthSpeed,
            elevationSpeed = newData.elevationSpeed ?: currentData.elevationSpeed,
            trainSpeed = newData.trainSpeed ?: currentData.trainSpeed,
            servoDriverAzimuthAngle = newData.servoDriverAzimuthAngle ?: currentData.servoDriverAzimuthAngle,
            servoDriverElevationAngle = newData.servoDriverElevationAngle ?: currentData.servoDriverElevationAngle,
            servoDriverTrainAngle = newData.servoDriverTrainAngle ?: currentData.servoDriverTrainAngle,
            torqueAzimuth = newData.torqueAzimuth ?: currentData.torqueAzimuth,
            torqueElevation = newData.torqueElevation ?: currentData.torqueElevation,
            torqueTrain = newData.torqueTrain ?: currentData.torqueTrain,
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
            trainBoardServoStatusBits = newData.trainBoardServoStatusBits ?: currentData.trainBoardServoStatusBits,
            trainBoardStatusBits = newData.trainBoardStatusBits ?: currentData.trainBoardStatusBits,
            feedBoardETCStatusBits = newData.feedBoardETCStatusBits ?: currentData.feedBoardETCStatusBits,
            feedSBoardStatusBits = newData.feedSBoardStatusBits ?: currentData.feedSBoardStatusBits,
            feedXBoardStatusBits = newData.feedXBoardStatusBits ?: currentData.feedXBoardStatusBits,
            feedKaBoardStatusBits = newData.feedKaBoardStatusBits ?: currentData.feedKaBoardStatusBits,
            currentSBandLNALHCP = newData.currentSBandLNALHCP ?: currentData.currentSBandLNALHCP,
            currentSBandLNARHCP = newData.currentSBandLNARHCP ?: currentData.currentSBandLNARHCP,
            currentXBandLNALHCP = newData.currentXBandLNALHCP ?: currentData.currentXBandLNALHCP,
            currentXBandLNARHCP = newData.currentXBandLNARHCP ?: currentData.currentXBandLNARHCP,
            currentKaBandLNALHCP = newData.currentKaBandLNALHCP ?: currentData.currentKaBandLNALHCP,
            currentKaBandLNARHCP = newData.currentKaBandLNARHCP ?: currentData.currentKaBandLNARHCP,
            rssiSBandLNALHCP = newData.rssiSBandLNALHCP ?: currentData.rssiSBandLNALHCP,
            rssiSBandLNARHCP = newData.rssiSBandLNARHCP ?: currentData.rssiSBandLNARHCP,
            rssiXBandLNALHCP = newData.rssiXBandLNALHCP ?: currentData.rssiXBandLNALHCP,
            rssiXBandLNARHCP = newData.rssiXBandLNARHCP ?: currentData.rssiXBandLNARHCP,
            azimuthAcceleration = newData.azimuthAcceleration ?: currentData.azimuthAcceleration,
            elevationAcceleration = newData.elevationAcceleration ?: currentData.elevationAcceleration,
            trainAcceleration = newData.trainAcceleration ?: currentData.trainAcceleration,
            azimuthMaxAcceleration = newData.azimuthMaxAcceleration ?: currentData.azimuthMaxAcceleration,
            elevationMaxAcceleration = newData.elevationMaxAcceleration ?: currentData.elevationMaxAcceleration,
            trainMaxAcceleration = newData.trainMaxAcceleration ?: currentData.trainMaxAcceleration,
            trackingAzimuthTime = newData.trackingAzimuthTime ?: currentData.trackingAzimuthTime,
            trackingCMDAzimuthAngle = newData.trackingCMDAzimuthAngle ?: currentData.trackingCMDAzimuthAngle,
            trackingActualAzimuthAngle = newData.trackingActualAzimuthAngle ?: currentData.trackingActualAzimuthAngle,
            trackingElevationTime = newData.trackingElevationTime ?: currentData.trackingElevationTime,
            trackingCMDElevationAngle = newData.trackingCMDElevationAngle ?: currentData.trackingCMDElevationAngle,
            trackingActualElevationAngle = newData.trackingActualElevationAngle ?: currentData.trackingActualElevationAngle,
            trackingTrainTime = newData.trackingTrainTime ?: currentData.trackingTrainTime,
            trackingCMDTrainAngle = newData.trackingCMDTrainAngle ?: currentData.trackingCMDTrainAngle,
            trackingActualTrainAngle = newData.trackingActualTrainAngle ?: currentData.trackingActualTrainAngle,
        )

        // ✅ 병합 결과도 조건부 로깅
        if (shouldLog) {
            logger.info("🔍 [DEBUG-DataStore] 병합 결과:")
            logger.info("  - mergedData.trackingCMDAzimuthAngle: ${mergedData.trackingCMDAzimuthAngle}")
            logger.info("  - mergedData.trackingCMDElevationAngle: ${mergedData.trackingCMDElevationAngle}")
            logger.info("  - mergedData.trackingCMDTrainAngle: ${mergedData.trackingCMDTrainAngle}")
            logger.info("  - newData.trackingCMDAzimuthAngle is null: ${newData.trackingCMDAzimuthAngle == null}")
            logger.info("  - newData.trackingCMDElevationAngle is null: ${newData.trackingCMDElevationAngle == null}")
            logger.info("  - newData.trackingCMDTrainAngle is null: ${newData.trackingCMDTrainAngle == null}")
            logger.info("  - 값 변경 여부: Az=${currentData.trackingCMDAzimuthAngle != mergedData.trackingCMDAzimuthAngle}, El=${currentData.trackingCMDElevationAngle != mergedData.trackingCMDElevationAngle}, Train=${currentData.trackingCMDTrainAngle != mergedData.trackingCMDTrainAngle}")
        }

        // ⚡ 최적화: 실제로 변경된 경우에만 업데이트
       // if (!isDataEqual(currentData, mergedData)) {
            latestData.set(mergedData)
            dataVersion.incrementAndGet() // 버전 증가

            // 연결 상태 업데이트
            lastUdpUpdateTime.set(Instant.now())
            udpConnected.set(true)
        //}
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
            "trainAngle" to data.trainAngle,
            "azimuthSpeed" to data.azimuthSpeed,
            "elevationSpeed" to data.elevationSpeed,
            "trainSpeed" to data.trainSpeed,
            "servoDriverAzimuthAngle" to data.servoDriverAzimuthAngle,
            "servoDriverElevationAngle" to data.servoDriverElevationAngle,
            "servoDriverTrainAngle" to data.servoDriverTrainAngle,
            "torqueAzimuth" to data.torqueAzimuth,
            "torqueElevation" to data.torqueElevation,
            "torqueTrain" to data.torqueTrain,
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
            "trainBoardServoStatusBits" to data.trainBoardServoStatusBits,
            "trainBoardStatusBits" to data.trainBoardStatusBits,
            "feedBoardETCStatusBits" to data.feedBoardETCStatusBits,
            "feedSBoardStatusBits" to data.feedSBoardStatusBits,
            "feedXBoardStatusBits" to data.feedXBoardStatusBits,
            "feedKaBoardStatusBits" to data.feedKaBoardStatusBits,
            "currentSBandLNALHCP" to data.currentSBandLNALHCP,
            "currentSBandLNARHCP" to data.currentSBandLNARHCP,
            "currentXBandLNALHCP" to data.currentXBandLNALHCP,
            "currentXBandLNARHCP" to data.currentXBandLNARHCP,
            "currentKaBandLNALHCP" to data.currentKaBandLNALHCP,
            "currentKaBandLNARHCP" to data.currentKaBandLNARHCP,
            "rssiSBandLNALHCP" to data.rssiSBandLNALHCP,
            "rssiSBandLNARHCP" to data.rssiSBandLNARHCP,
            "rssiXBandLNALHCP" to data.rssiXBandLNALHCP,
            "rssiXBandLNARHCP" to data.rssiXBandLNARHCP,
            "azimuthAcceleration" to data.azimuthAcceleration,
            "elevationAcceleration" to data.elevationAcceleration,
            "trainAcceleration" to data.trainAcceleration,
            "azimuthMaxAcceleration" to data.azimuthMaxAcceleration,
            "elevationMaxAcceleration" to data.elevationMaxAcceleration,
            "trainMaxAcceleration" to data.trainMaxAcceleration,
            "trackingAzimuthTime" to data.trackingAzimuthTime,
            "trackingCMDAzimuthAngle" to data.trackingCMDAzimuthAngle,
            "trackingActualAzimuthAngle" to data.trackingActualAzimuthAngle,
            "trackingElevationTime" to data.trackingElevationTime,
            "trackingCMDElevationAngle" to data.trackingCMDElevationAngle,
            "trackingActualElevationAngle" to data.trackingActualElevationAngle,
            "trackingTrainTime" to data.trackingTrainTime,
            "trackingCMDTrainAngle" to data.trackingCMDTrainAngle,
            "trackingActualTrainAngle" to data.trackingActualTrainAngle
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
            "trackingTrainTime" to data.trackingTrainTime,
            "trackingCMDTrainAngle" to data.trackingCMDTrainAngle,
            "trackingActualTrainAngle" to data.trackingActualTrainAngle
        )
    }
    /**
     * ✅ 현재 추적 중인 전역 고유 MstId (Long 타입)
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 UInt → Long으로 변경
     * 전역 고유 ID를 지원하기 위해 Long 타입 사용
     */
    private val currentTrackingMstId = AtomicReference<Long?>(null)
    
    /**
     * ✅ 다음 추적 예정 전역 고유 MstId (Long 타입)
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 UInt → Long으로 변경
     * 전역 고유 ID를 지원하기 위해 Long 타입 사용
     */
    private val nextTrackingMstId = AtomicReference<Long?>(null)

    /**
     * ✅ 현재 추적 중인 DetailId (Int 타입)
     * 
     * mstId와 함께 사용하여 정확한 스케줄 식별
     */
    private val currentTrackingDetailId = AtomicReference<Int?>(null)
    
    /**
     * ✅ 다음 추적 예정 DetailId (Int 타입)
     * 
     * mstId와 함께 사용하여 정확한 스케줄 식별
     */
    private val nextTrackingDetailId = AtomicReference<Int?>(null)

    /**
     * ✅ 현재 추적 중인 mstId와 detailId 설정
     * 
     * @param mstId 전역 고유 MstId (Long 타입)
     * @param detailId 패스 인덱스 (Int 타입, null 가능)
     */
    fun setCurrentTrackingMstId(mstId: Long?, detailId: Int? = null) {
        currentTrackingMstId.set(mstId)
        currentTrackingDetailId.set(detailId)
        dataVersion.incrementAndGet()
    }

    /**
     * ✅ 다음 추적 예정 mstId와 detailId 설정
     * 
     * @param mstId 전역 고유 MstId (Long 타입)
     * @param detailId 패스 인덱스 (Int 타입, null 가능)
     */
    fun setNextTrackingMstId(mstId: Long?, detailId: Int? = null) {
        nextTrackingMstId.set(mstId)
        nextTrackingDetailId.set(detailId)
        dataVersion.incrementAndGet()
    }

    /**
     * ✅ 현재 추적 중인 mstId 조회
     * 
     * @return 전역 고유 MstId (Long 타입, null 가능)
     */
    fun getCurrentTrackingMstId(): Long? = currentTrackingMstId.get()
    
    /**
     * ✅ 다음 추적 예정 mstId 조회
     * 
     * @return 전역 고유 MstId (Long 타입, null 가능)
     */
    fun getNextTrackingMstId(): Long? = nextTrackingMstId.get()

    /**
     * ✅ 현재 추적 중인 detailId 조회
     * 
     * @return 패스 인덱스 (Int 타입, null 가능)
     */
    fun getCurrentTrackingDetailId(): Int? = currentTrackingDetailId.get()
    
    /**
     * ✅ 다음 추적 예정 detailId 조회
     * 
     * @return 패스 인덱스 (Int 타입, null 가능)
     */
    fun getNextTrackingDetailId(): Int? = nextTrackingDetailId.get()

    /**
     * ✅ 추적 mstId와 detailId 초기화
     */
    fun clearTrackingMstIds() {
        currentTrackingMstId.set(null)
        nextTrackingMstId.set(null)
        currentTrackingDetailId.set(null)
        nextTrackingDetailId.set(null)
        dataVersion.incrementAndGet()
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
        val currentStatus = trackingStatus.get()
        val newStatus = PushData.TrackingStatus(
            ephemerisStatus = active,
            ephemerisTrackingState = if (active) "TRACKING" else currentStatus.ephemerisTrackingState, // ✅ 기존 상태 유지
            passScheduleStatus = false,
            sunTrackStatus = false,
            manualControlStatus = currentStatus.manualControlStatus,
            geostationaryStatus = currentStatus.geostationaryStatus
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
    // private fun isDataEqual(data1: PushData.ReadData, data2: PushData.ReadData): Boolean {
    //     return data1.azimuthAngle == data2.azimuthAngle &&
    //            data1.elevationAngle == data2.elevationAngle &&
    //            data1.trainAngle == data2.trainAngle &&
    //            data1.azimuthSpeed == data2.azimuthSpeed &&
    //            data1.elevationSpeed == data2.elevationSpeed &&
    //            data1.trainSpeed == data2.trainSpeed &&
    //            data1.modeStatusBits == data2.modeStatusBits &&
    //            data1.windSpeed == data2.windSpeed &&
    //            data1.windDirection == data2.windDirection
    //            // 주요 필드들만 체크 (성능 고려)
    // }

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
            data.modeStatusBits, data.azimuthAngle, data.elevationAngle, data.trainAngle,
            data.azimuthSpeed, data.elevationSpeed, data.trainSpeed,
            data.servoDriverAzimuthAngle, data.servoDriverElevationAngle, data.servoDriverTrainAngle,
            data.torqueAzimuth, data.torqueElevation, data.torqueTrain,
            data.windSpeed, data.windDirection, data.rtdOne, data.rtdTwo,
            data.mainBoardProtocolStatusBits, data.mainBoardStatusBits,
            data.mainBoardMCOnOffBits, data.mainBoardReserveBits,
            data.azimuthBoardServoStatusBits, data.azimuthBoardStatusBits,
            data.elevationBoardServoStatusBits, data.elevationBoardStatusBits,
            data.trainBoardServoStatusBits, data.trainBoardStatusBits,
            data.feedBoardETCStatusBits, data.feedSBoardStatusBits, data.feedXBoardStatusBits, data.feedKaBoardStatusBits,
            data.currentSBandLNALHCP, data.currentSBandLNARHCP,
            data.currentXBandLNALHCP, data.currentXBandLNARHCP,
            data.rssiSBandLNALHCP, data.rssiSBandLNARHCP,
            data.rssiXBandLNALHCP, data.rssiXBandLNARHCP
        ).size
    }

    /**
     * ✅ Ephemeris 추적 상태 확인 (실제 추적 중인지)
     */
    fun isEphemerisTrackingActive(): Boolean {
        return trackingStatus.get().ephemerisTrackingState == "TRACKING"
    }

    /**
     * ✅ Ephemeris 추적 상태 가져오기
     */
    fun getEphemerisTrackingState(): String? {
        return trackingStatus.get().ephemerisTrackingState
    }

    /**
     * ✅ Pass Schedule 추적 상태 확인
     */
    fun isPassScheduleTrackingActive(): Boolean {
        return trackingStatus.get().passScheduleStatus == true
    }

    /**
     * ✅ Sun Track 추적 상태 확인
     */
    fun isSunTrackingActive(): Boolean {
        return trackingStatus.get().sunTrackStatus == true
    }

    /**
     * ✅ 현재 활성화된 추적 모드 확인
     */
    fun getActiveTrackingMode(): String? {
        return when {
            trackingStatus.get().ephemerisStatus == true -> "ephemeris"
            trackingStatus.get().passScheduleStatus == true -> "passSchedule"
            trackingStatus.get().sunTrackStatus == true -> "sunTrack"
            else -> null
        }
    }

    fun debugElevationBoardStatusBits(): String? {
        return latestData.get().elevationBoardStatusBits
    }
}