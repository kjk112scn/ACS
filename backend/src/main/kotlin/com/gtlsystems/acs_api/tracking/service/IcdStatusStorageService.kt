package com.gtlsystems.acs_api.tracking.service

import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.service.system.settings.SettingsService
import com.gtlsystems.acs_api.tracking.entity.IcdStatusEntity
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import reactor.core.Disposable

/**
 * ICD 상태 DB 저장 서비스
 *
 * - DataStoreService에서 주기적으로 ICD 상태 수집
 * - 배치로 묶어서 DB에 저장 (system.storage.saveInterval 간격)
 * - UDP 연결 시에만 저장 (불필요한 null 데이터 방지)
 */
@Service
@ConditionalOnProperty(
    prefix = "spring.r2dbc",
    name = ["url"],
    matchIfMissing = false
)
class IcdStatusStorageService(
    private val dataStoreService: DataStoreService,
    private val trackingDataService: TrackingDataService,
    private val settingsService: SettingsService
) {
    private val logger = LoggerFactory.getLogger(IcdStatusStorageService::class.java)

    // 버퍼 및 상태 관리
    private val buffer = CopyOnWriteArrayList<IcdStatusEntity>()
    private val isRunning = AtomicBoolean(false)
    private val savedCount = AtomicLong(0)
    private val lastSaveTime = AtomicLong(0)

    // 스케줄러 disposable
    private var collectorDisposable: Disposable? = null
    private var saverDisposable: Disposable? = null

    // 설정값 (기본값)
    private var saveInterval: Long = 100L  // ms
    private var batchSize: Int = 1000
    private var progressLogInterval: Int = 1000  // 저장 건수

    @PostConstruct
    fun init() {
        loadSettings()
        startCollector()
        startSaver()
        logger.info("✅ ICD 상태 저장 서비스 시작 (saveInterval={}ms, batchSize={})", saveInterval, batchSize)
    }

    @PreDestroy
    fun shutdown() {
        isRunning.set(false)
        collectorDisposable?.dispose()
        saverDisposable?.dispose()

        // 남은 버퍼 저장
        if (buffer.isNotEmpty()) {
            flushBuffer().block()
        }
        logger.info("🛑 ICD 상태 저장 서비스 종료 (총 저장: {}건)", savedCount.get())
    }

    /**
     * 설정값 로드
     */
    private fun loadSettings() {
        try {
            saveInterval = settingsService.systemStorageSaveInterval
            batchSize = settingsService.systemStorageBatchSize
            progressLogInterval = settingsService.systemStorageProgressLogInterval
        } catch (e: Exception) {
            logger.warn("설정 로드 실패, 기본값 사용: {}", e.message)
        }
    }

    /**
     * 데이터 수집 스케줄러 시작
     * - saveInterval마다 현재 ICD 상태를 버퍼에 추가
     */
    private fun startCollector() {
        isRunning.set(true)
        collectorDisposable = Mono.delay(Duration.ofMillis(saveInterval))
            .repeat { isRunning.get() }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe {
                collectCurrentStatus()
            }
    }

    /**
     * 배치 저장 스케줄러 시작
     * - 1초마다 버퍼를 DB에 저장
     */
    private fun startSaver() {
        saverDisposable = Mono.delay(Duration.ofSeconds(1))
            .repeat { isRunning.get() }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe {
                flushBuffer().subscribe()
            }
    }

    /**
     * 현재 상태 수집 → 버퍼 추가
     */
    private fun collectCurrentStatus() {
        // UDP 연결 시에만 수집 (불필요한 null 데이터 방지)
        if (!dataStoreService.isUdpConnected()) {
            return
        }

        val data = dataStoreService.getLatestData()

        // 유효한 데이터가 있을 때만 저장 (azimuthAngle이 null이면 무효)
        if (data.azimuthAngle == null) {
            return
        }

        val entity = convertToEntity(data)
        buffer.add(entity)

        // 버퍼 크기 제한 (메모리 보호)
        if (buffer.size > batchSize * 2) {
            flushBuffer().subscribe()
        }
    }

    /**
     * 버퍼 → DB 저장
     */
    private fun flushBuffer(): Mono<Void> {
        if (buffer.isEmpty()) {
            return Mono.empty()
        }

        val toSave = buffer.toList()
        buffer.clear()

        return trackingDataService.saveIcdStatuses(toSave)
            .doOnSuccess {
                val count = savedCount.addAndGet(toSave.size.toLong())
                lastSaveTime.set(System.currentTimeMillis())

                // 진행 로그 (1000건마다)
                if (count % progressLogInterval == 0L) {
                    logger.debug("📊 ICD 상태 저장 진행: {}건", count)
                }
            }
            .doOnError { e ->
                logger.error("❌ ICD 상태 배치 저장 실패 ({}건): {}", toSave.size, e.message)
                // 실패 시 버퍼에 다시 추가 (재시도)
                buffer.addAll(toSave)
            }
    }

    /**
     * PushData.ReadData → IcdStatusEntity 변환
     */
    private fun convertToEntity(data: com.gtlsystems.acs_api.model.PushData.ReadData): IcdStatusEntity {
        return IcdStatusEntity(
            timestamp = OffsetDateTime.now(ZoneOffset.UTC),

            // 각도 (6개)
            azimuthAngle = data.azimuthAngle,
            elevationAngle = data.elevationAngle,
            trainAngle = data.trainAngle,
            servoDriverAzimuthAngle = data.servoDriverAzimuthAngle,
            servoDriverElevationAngle = data.servoDriverElevationAngle,
            servoDriverTrainAngle = data.servoDriverTrainAngle,

            // 속도 (3개)
            azimuthSpeed = data.azimuthSpeed,
            elevationSpeed = data.elevationSpeed,
            trainSpeed = data.trainSpeed,

            // 토크 (3개)
            torqueAzimuth = data.torqueAzimuth,
            torqueElevation = data.torqueElevation,
            torqueTrain = data.torqueTrain,

            // 가속도 (6개)
            azimuthAcceleration = data.azimuthAcceleration,
            elevationAcceleration = data.elevationAcceleration,
            trainAcceleration = data.trainAcceleration,
            azimuthMaxAcceleration = data.azimuthMaxAcceleration,
            elevationMaxAcceleration = data.elevationMaxAcceleration,
            trainMaxAcceleration = data.trainMaxAcceleration,

            // 환경 (4개)
            windSpeed = data.windSpeed,
            windDirection = data.windDirection?.let { it.toInt().toShort() },
            rtdOne = data.rtdOne,
            rtdTwo = data.rtdTwo,

            // 상태 비트 (12개)
            modeStatusBits = data.modeStatusBits,
            mainBoardProtocolStatus = data.mainBoardProtocolStatusBits,
            mainBoardStatus = data.mainBoardStatusBits,
            mainBoardMcOnoff = data.mainBoardMCOnOffBits,
            mainBoardReserve = data.mainBoardReserveBits,
            azimuthServoStatus = data.azimuthBoardServoStatusBits,
            azimuthBoardStatus = data.azimuthBoardStatusBits,
            elevationServoStatus = data.elevationBoardServoStatusBits,
            elevationBoardStatus = data.elevationBoardStatusBits,
            trainServoStatus = data.trainBoardServoStatusBits,
            trainBoardStatus = data.trainBoardStatusBits,
            feedBoardEtcStatus = data.feedBoardETCStatusBits,

            // Feed 상태 (3개)
            feedSBoardStatus = data.feedSBoardStatusBits,
            feedXBoardStatus = data.feedXBoardStatusBits,
            feedKaBoardStatus = data.feedKaBoardStatusBits,

            // LNA 전류 (6개)
            currentSbandLnaLhcp = data.currentSBandLNALHCP,
            currentSbandLnaRhcp = data.currentSBandLNARHCP,
            currentXbandLnaLhcp = data.currentXBandLNALHCP,
            currentXbandLnaRhcp = data.currentXBandLNARHCP,
            currentKabandLnaLhcp = data.currentKaBandLNALHCP,
            currentKabandLnaRhcp = data.currentKaBandLNARHCP,

            // RSSI (6개) - Ka-band RSSI는 Entity에 없음, X-band까지만
            rssiSbandLnaLhcp = data.rssiSBandLNALHCP,
            rssiSbandLnaRhcp = data.rssiSBandLNARHCP,
            rssiXbandLnaLhcp = data.rssiXBandLNALHCP,
            rssiXbandLnaRhcp = data.rssiXBandLNARHCP,
            rssiKabandLnaLhcp = null,  // PushData.ReadData에 없음
            rssiKabandLnaRhcp = null,  // PushData.ReadData에 없음

            // 추적 CMD/실측 (9개)
            trackingAzimuthTime = data.trackingAzimuthTime,
            trackingCmdAzimuth = data.trackingCMDAzimuthAngle,
            trackingActualAzimuth = data.trackingActualAzimuthAngle,
            trackingElevationTime = data.trackingElevationTime,
            trackingCmdElevation = data.trackingCMDElevationAngle,
            trackingActualElevation = data.trackingActualElevationAngle,
            trackingTrainTime = data.trackingTrainTime,
            trackingCmdTrain = data.trackingCMDTrainAngle,
            trackingActualTrain = data.trackingActualTrainAngle
        )
    }

    /**
     * 상태 정보 조회
     */
    fun getStatus(): Map<String, Any> {
        return mapOf(
            "isRunning" to isRunning.get(),
            "savedCount" to savedCount.get(),
            "bufferSize" to buffer.size,
            "lastSaveTime" to lastSaveTime.get(),
            "saveInterval" to saveInterval,
            "batchSize" to batchSize
        )
    }

    /**
     * 수동 플러시 (테스트용)
     */
    fun manualFlush(): Mono<Long> {
        return flushBuffer().thenReturn(savedCount.get())
    }
}
