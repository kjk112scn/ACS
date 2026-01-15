package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.axislimitangle.LimitAngleCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.model.GlobalData
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import com.gtlsystems.acs_api.algorithm.axistransformation.CoordinateTransformer
import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.event.subscribeToType
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.algorithm.satellitetracker.processor.SatelliteTrackingProcessor
import com.gtlsystems.acs_api.service.icd.ICDService
import com.gtlsystems.acs_api.service.udp.UdpFwICDService
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.config.ThreadManager
import io.netty.handler.timeout.TimeoutException
import jakarta.annotation.PreDestroy
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.BitSet
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import com.gtlsystems.acs_api.service.system.BatchStorageManager
import com.gtlsystems.acs_api.service.system.settings.SettingsService
import kotlin.math.abs

/**
 * 위성 추적 서비스
 * 위성의 위치를 계산하고 추적 정보를 제공합니다.
 */
@Service
class EphemerisService(
    private val orekitCalculator: OrekitCalculator,
    private val satelliteTrackingProcessor: com.gtlsystems.acs_api.algorithm.satellitetracker.processor.SatelliteTrackingProcessor, // ✅ Phase 3: Processor 추가
    private val acsEventBus: ACSEventBus,
    private val udpFwICDService: UdpFwICDService,
    private val dataStoreService: DataStoreService, // DataStoreService 주입
    private val threadManager: ThreadManager, // ✅ 통합 쓰레드 관리자 주입
    private val batchStorageManager: BatchStorageManager, // ✅ 배치 저장 관리자 주입
    private val settingsService: SettingsService // ✅ 설정 서비스 주입
) {

    // 밀리초를 포함하는 사용자 정의 포맷터 생성
    private val logger = LoggerFactory.getLogger(javaClass)

    // 위성 TLE 데이터 캐시
    private val satelliteTleCache = ConcurrentHashMap<String, Pair<String, String>>()
    private val locationData = settingsService.locationData

    // 위성 추적 마스터 및 세부 데이터 저장소 (실제로는 데이터베이스를 사용할 것입니다)
    private val ephemerisTrackMstStorage = mutableListOf<Map<String, Any?>>()
    private val ephemerisTrackDtlStorage = mutableListOf<Map<String, Any?>>()

    // 현재 추적 중인 위성 정보
    private var currentTrackingPass: Map<String, Any?>? = null

    private var currentTrackingPassId: Long? = null  // ✅ UInt → Long 변경 (PassSchedule과 동일)
    private var subscriptions: MutableList<Disposable> = mutableListOf()

    private val trackingStatus = PushData.TRACKING_STATUS

    // ✅ 통합 쓰레드 관리자 사용
    private var trackingExecutor: ScheduledExecutorService? = null
    private var modeTask: ScheduledFuture<*>? = null

    // ✅ 통합 상태 관리 (단순화: 6개 상태)
    enum class TrackingState {
        IDLE,           // 대기 (추적 비활성)
        PREPARING,      // 준비 중 (Train 이동 + 안정화 + Az/El 이동)
        WAITING,        // 시작 대기 (시작 시간 전, 12.1 헤더 전송 완료)
        TRACKING,       // 추적 중 (12.2 초기 데이터 전송, 실시간 추적)
        COMPLETED,      // 완료
        ERROR           // 오류
    }

    // ✅ 준비 단계 세부 상태 (PREPARING 내부에서만 사용)
    enum class PreparingPhase {
        TRAIN_MOVING,           // Train 각도 이동 중
        TRAIN_STABILIZING,      // Train 안정화 대기 중
        MOVING_TO_TARGET        // 목표 Az/El로 이동 중
    }

    private var currentTrackingState = TrackingState.IDLE
    private var currentPreparingPhase = PreparingPhase.TRAIN_MOVING
    private var stabilizationStartTime: Long = 0
    private var targetAzimuth: Float = 0f
    private var targetElevation: Float = 0f

    // ✅ 일회성 동작 플래그 (상태와 분리된 단순 플래그)
    private var headerSent: Boolean = false       // 12.1 헤더 전송 완료
    private var initialDataSent: Boolean = false  // 12.2 초기 데이터 전송 완료

    // ✅ 명령 전송 시간 기록 (도달 여부 확인 시 최소 대기 시간 보장)
    private var trainMoveCommandTime: Long = 0
    private var azElMoveCommandTime: Long = 0
    
    // ✅ 대기 상태 로그 출력 제한 (변경사항이 있거나 5초마다만 출력)
    private var lastWaitingLogTime: Long = 0
    private var lastWaitingTimeDifference: Long = -1
    private var lastWaitingAzimuth: Double = 0.0
    private var lastWaitingElevation: Double = 0.0

    // ✅ Keyhole 경고 로그 출력 제한 (추적당 한 번만)
    private var keyholeWarningLogged: Boolean = false

    // ✅ Train 축 안정화 대기 시간
    companion object {
        const val TRAIN_STABILIZATION_TIMEOUT = 3L // Tilt 안정화: 10분
        const val WAITING_LOG_INTERVAL_MS = 5000L // 대기 상태 로그 출력 주기: 5초
    }

    private var trackingDataIndex = 0
    private val limitAngleCalculator = LimitAngleCalculator()

    @PostConstruct
    fun init() {
        eventBus()
    }

    fun eventBus() {
        // 위성 추적 헤더 이벤트 구독
        val headerSubscription =
            acsEventBus.subscribeToType<ACSEvent.ICDEvent.SatelliteTrackHeaderReceived>().subscribe { event ->
                // ✅ 상태 기반: TRACKING 상태일 때만 초기 데이터 전송
                if (currentTrackingState == TrackingState.TRACKING) {
                    currentTrackingPassId?.let { passId ->
                        sendInitialTrackingData(passId)
                    }
                } else {
                    logger.info("⏳ 헤더 수신 완료, 시작 시간 대기 중 (초기 데이터는 TRACKING 상태에서 전송)")
                }
            }

        // 위성 추적 데이터 요청 이벤트 구독
        val dataRequestSubscription =
            acsEventBus.subscribeToType<ACSEvent.ICDEvent.SatelliteTrackDataRequested>().subscribe { event ->
                // 데이터 요청에 응답하여 추가 데이터 전송
                currentTrackingPassId?.let { passId ->
                    // 요청된 시간 누적치에 따라 적절한 데이터 전송
                    val requestData = event.requestData as ICDService.SatelliteTrackThree.GetDataFrame
                    handleEphemerisTrackingDataRequest(requestData.timeAcc, requestData.requestDataLength)
                }
            }

        // 구독 객체 저장
        subscriptions.add(headerSubscription)
        subscriptions.add(dataRequestSubscription)
    }

    // ✅ 서비스 종료 시 정리 (기존 destroy() 메서드에 추가)
    @PreDestroy
    fun destroy() {
        // 기존 구독 해제
        subscriptions.forEach { it.dispose() }
        subscriptions.clear()

        // ✅ 모드 타이머 정리
        stopModeTimer()

        // ✅ 배치 처리 안전 종료
        try {
            val batchShutdownSuccess = batchStorageManager.safeShutdown()
            if (batchShutdownSuccess) {
                logger.info("✅ 배치 처리 안전 종료 완료")
            } else {
                logger.warn("⚠️ 배치 처리 종료 중 일부 데이터가 손실될 수 있습니다")
            }
        } catch (e: Exception) {
            logger.error("❌ 배치 처리 종료 중 오류: ${e.message}", e)
        }
        logger.info("Destroy EphemerisService 정리 완료")
    }

    /**
     * 정지궤도 위성 추적 시작 (3축 변환 적용)
     */
    fun startGeostationaryTracking(tleLine1: String, tleLine2: String) {
        try {
            logger.info("🚀 정지궤도 위성 추적 시작 (3축 변환 적용)")

            // 3축 변환된 정지궤도 위치 계산
            val geo3AxisPosition = getCurrentGeostationaryPositionWith3AxisTransform(tleLine1, tleLine2, tiltAngle = settingsService.tiltAngle, trainAngle = 0.0)

            // 원본 좌표 추출
            val originalAzimuth = geo3AxisPosition["originalAzimuth"] as? Double ?: 0.0
            val originalElevation = geo3AxisPosition["originalElevation"] as? Double ?: 0.0

            // 변환된 좌표 추출
            val transformedAzimuth = geo3AxisPosition["transformedAzimuth"] as? Double ?: originalAzimuth
            val transformedElevation = geo3AxisPosition["transformedElevation"] as? Double ?: originalElevation

            // 변환 정보 추출
            val tiltAngle = settingsService.tiltAngle
            val trainAngle = geo3AxisPosition["trainAngle"] as? Double ?: 0.0

            logger.info(
                "📍 정지궤도 원본 좌표: Az=${String.format("%.2f", originalAzimuth)}°, El=${
                    String.format(
                        "%.2f",
                        originalElevation
                    )
                }°"
            )
            logger.info("🔄 3축 변환 적용: 기울기=${tiltAngle}°, 회전체=${trainAngle}°")
            logger.info(
                "📍 정지궤도 변환 좌표: Az=${String.format("%.2f", transformedAzimuth)}°, El=${
                    String.format(
                        "%.2f",
                        transformedElevation
                    )
                }°"
            )

            // 변환 오차 계산
            val azimuthDifference = transformedAzimuth - originalAzimuth
            val elevationDifference = transformedElevation - originalElevation

            logger.info(
                "📊 변환 오차: Az=${String.format("%.4f", azimuthDifference)}°, El=${
                    String.format(
                        "%.4f",
                        elevationDifference
                    )
                }°"
            )

            // ✅ 공통 상태머신 사용을 위한 목표 각도 설정
            targetAzimuth = transformedAzimuth.toFloat()
            targetElevation = transformedElevation.toFloat()

            // ✅ 정지궤도 추적 상태 설정
            trackingStatus.geostationaryStatus = true

            // ✅ 상태 단순화: PREPARING 상태로 진입
            currentTrackingState = TrackingState.PREPARING
            currentPreparingPhase = PreparingPhase.TRAIN_MOVING

            // ✅ 모드 타이머 시작 (공통 상태머신 체크용)
            startModeTimer()

            // 3축 변환 결과 로깅
            logger.info("✅ 3축 변환 완료")
            logger.info("🔄 변환 정보: 기울기=${tiltAngle}°, 회전체=${trainAngle}°")
            logger.info("✅ 정지궤도 추적 시작 완료 (공통 상태머신 적용)")

        } catch (e: Exception) {
            logger.error("❌ 정지궤도 추적 시작 실패: ${e.message}", e)
            throw RuntimeException("정지궤도 추적 시작 실패: ${e.message}", e)
        }
    }

    /**
     * 현재시간의 위성 좌표 1개만 추출하는 함수
     * 정지궤도 판단 시 동작하는 함수.
     */
    fun getCurrentSatellitePosition(
        tleLine1: String,
        tleLine2: String,
        targetTime: ZonedDateTime? = null
    ): Map<String, Any> {
        try {
            // 대상 시간 설정 (기본값: 현재시간)
            val currentTime = targetTime ?: GlobalData.Time.calUtcTimeOffsetTime

            logger.info("현재시간 위성 좌표 계산: ${currentTime}")

            // OrekitCalculator의 calculatePosition 함수 사용
            val satelliteData = orekitCalculator.calculatePosition(
                tleLine1 = tleLine1,
                tleLine2 = tleLine2,
                dateTime = currentTime,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                altitude = locationData.altitude
            )
            val result = mapOf<String, Any>(
                "timestamp" to currentTime,
                "azimuth" to satelliteData.azimuth,
                "elevation" to satelliteData.elevation,
                "range" to (satelliteData.range?: 0.0),
                "altitude" to (satelliteData.altitude?: 0.0),
                "satelliteId" to tleLine1.substring(2, 7).trim(),
                "calculationTime" to System.currentTimeMillis()
            )
            logger.info(
                "현재시간 위성 좌표 계산 완료: Az=${
                    String.format(
                        "%.2f",
                        satelliteData.azimuth
                    )
                }°, El=${String.format("%.2f", satelliteData.elevation)}°"
            )

            return result

        } catch (e: Exception) {
            logger.error("현재시간 위성 좌표 계산 실패: ${e.message}", e)
            throw RuntimeException("위성 좌표 계산 실패: ${e.message}", e)
        }
    }

    /**
     * 현재 시간 위성 좌표를 3축 변환하여 추출
     * 정지 궤도용
     */
    fun getCurrentGeostationaryPositionWith3AxisTransform(
        tleLine1: String,
        tleLine2: String,
        targetTime: ZonedDateTime? = null,
        tiltAngle: Double = -7.0,
        trainAngle: Double = 0.0  // 회전체 각도 (기본값 0도)
    ): Map<String, Any> {
        try {
            // 1. 현재 시간 위성 좌표 추출
            val currentPosition = getCurrentSatellitePosition(tleLine1, tleLine2, targetTime)

            val originalAzimuth = currentPosition["azimuth"] as Double
            val originalElevation = currentPosition["elevation"] as Double

            logger.info(
                "현재 시간 위성 좌표: Az=${String.format("%.2f", originalAzimuth)}°, El=${
                    String.format(
                        "%.2f",
                        originalElevation
                    )
                }°"
            )
            // 2. 3축 변환 적용 (단일 좌표 변환)
            val (transformedAzimuth, transformedElevation) = CoordinateTransformer.transformCoordinatesWithTrain(
                azimuth = originalAzimuth,
                elevation = originalElevation,
                tiltAngle = tiltAngle,
                trainAngle = trainAngle
            )
            // 3. 종합 결과 생성
            val result = currentPosition.toMutableMap().apply {
                put("originalAzimuth", originalAzimuth)
                put("originalElevation", originalElevation)
                put("tiltAngle", tiltAngle)
                put("trainAngle", trainAngle)
                put("transformedAzimuth", transformedAzimuth)
                put("transformedElevation", transformedElevation)
                put("azimuthDifference", transformedAzimuth - originalAzimuth)
                put("elevationDifference", transformedElevation - originalElevation)
                put("transformationType", "3axis_single_point")
            }
            logger.info(
                "3축 변환 완료: 원본 Az=${String.format("%.2f", originalAzimuth)}°, El=${
                    String.format(
                        "%.2f",
                        originalElevation
                    )
                }°"
            )
            logger.info(
                "변환 결과: Az=${String.format("%.2f", transformedAzimuth)}°, El=${
                    String.format(
                        "%.2f",
                        transformedElevation
                    )
                }°"
            )
            logger.info("변환 정보: 기울기=${tiltAngle}°, 회전체=${trainAngle}°")
            return result
        } catch (e: Exception) {
            logger.error("현재 시간 3축 변환 실패: ${e.message}", e)
            throw RuntimeException("3축 변환 실패: ${e.message}", e)
        }
    }

    fun generateEphemerisDesignationTrackAsync(
        tleLine1: String, tleLine2: String, satelliteName: String? = null
    ): Mono<Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>> {

        return Mono.fromCallable {
            generateEphemerisDesignationTrackSync(tleLine1, tleLine2, satelliteName)
        }.subscribeOn(Schedulers.boundedElastic()).doOnSubscribe {
            logger.info("위성 궤도 계산 시작 (비동기)")
        }.doOnSuccess {
            logger.info("위성 궤도 계산 완료 (비동기)")
        }.doOnError { error ->
            logger.error("위성 궤도 계산 실패 (비동기): ${error.message}", error)
        }.timeout(Duration.ofMinutes(60)).onErrorMap { error ->
            when (error) {
                is IOException -> RuntimeException("네트워크 연결 오류: ${error.message}", error)
                is TimeoutException -> RuntimeException("계산 시간 초과", error)
                else -> RuntimeException("위성 궤도 계산 실패: ${error.message}", error)
            }
        }
    }

    /**
     * ✅ Phase 3: 리팩토링된 위성 궤도 추적 (Processor 사용)
     * 
     * TLE 데이터로 위성 궤도 추적
     * 위성 이름이 제공되지 않으면 TLE에서 추출
     */
    fun generateEphemerisDesignationTrackSync(
        tleLine1: String, tleLine2: String, satelliteName: String? = null
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        try {
            logger.info("🚀 위성 궤도 추적 시작")

            // 1️⃣ OrekitCalculator: 순수 2축 각도만 생성
            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            val sourceMinEl = settingsService.sourceMinElevationAngle.toFloat()

            logger.info("📡 OrekitCalculator 호출 중...")
            var schedule = orekitCalculator.generateSatelliteTrackingSchedule(
                tleLine1 = tleLine1,
                tleLine2 = tleLine2,
                startDate = today.withZoneSameInstant(ZoneOffset.UTC),
                durationDays = 2,
                minElevation = sourceMinEl,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                altitude = locationData.altitude
            )
            if (schedule.trackingPasses.isEmpty()) {
                logger.warn("⚠️ 가시성 패스가 없습니다.")
                return Pair(emptyList(), emptyList())
            }
            
            logger.info("✅ OrekitCalculator 완료: ${schedule.trackingPasses.size}개 패스")

            // 2️⃣ Processor: 모든 변환 및 메타데이터 계산
            logger.info("🔄 SatelliteTrackingProcessor 호출 중...")
            val processedData = satelliteTrackingProcessor.processFullTransformation(
                schedule,
                satelliteName
            )
            logger.info("✅ Processor 완료")

            // 3️⃣ ephemerisTrackMstStorage, ephemerisTrackDtlStorage에 저장
            logger.info("💾 저장소에 데이터 저장 중...")
            ephemerisTrackMstStorage.clear()
            ephemerisTrackDtlStorage.clear()

            // Original 데이터 저장
            ephemerisTrackMstStorage.addAll(processedData.originalMst)
            ephemerisTrackDtlStorage.addAll(processedData.originalDtl)
            logger.debug("Original 저장: ${processedData.originalMst.size} Mst, ${processedData.originalDtl.size} Dtl")

            // 3축 변환 데이터 저장
            ephemerisTrackMstStorage.addAll(processedData.axisTransformedMst)
            ephemerisTrackDtlStorage.addAll(processedData.axisTransformedDtl)
            logger.debug("3축 변환 저장: ${processedData.axisTransformedMst.size} Mst, ${processedData.axisTransformedDtl.size} Dtl")

            // 최종 변환 데이터 저장 (Train=0, 각도 제한 ✅)
            ephemerisTrackMstStorage.addAll(processedData.finalTransformedMst)
            ephemerisTrackDtlStorage.addAll(processedData.finalTransformedDtl)
            logger.debug("최종 변환 저장: ${processedData.finalTransformedMst.size} Mst, ${processedData.finalTransformedDtl.size} Dtl")

            // ✅ Keyhole Axis 변환 데이터 저장 (Train≠0, 각도 제한 ❌)
            ephemerisTrackMstStorage.addAll(processedData.keyholeAxisTransformedMst)
            ephemerisTrackDtlStorage.addAll(processedData.keyholeAxisTransformedDtl)
            logger.debug("Keyhole Axis 저장: ${processedData.keyholeAxisTransformedMst.size} Mst, ${processedData.keyholeAxisTransformedDtl.size} Dtl")

            // ✅ Keyhole Final 변환 데이터 저장 (Train≠0, 각도 제한 ✅)
            ephemerisTrackMstStorage.addAll(processedData.keyholeFinalTransformedMst)
            ephemerisTrackDtlStorage.addAll(processedData.keyholeFinalTransformedDtl)
            logger.debug("Keyhole Final 저장: ${processedData.keyholeFinalTransformedMst.size} Mst, ${processedData.keyholeFinalTransformedDtl.size} Dtl")

            // ✅ Keyhole Optimized Axis 변환 데이터 저장 (Train≠0 최적화, 각도 제한 ❌)
            ephemerisTrackMstStorage.addAll(processedData.keyholeOptimizedAxisTransformedMst)
            ephemerisTrackDtlStorage.addAll(processedData.keyholeOptimizedAxisTransformedDtl)
            logger.debug("Keyhole Optimized Axis 저장: ${processedData.keyholeOptimizedAxisTransformedMst.size} Mst, ${processedData.keyholeOptimizedAxisTransformedDtl.size} Dtl")

            // ✅ Keyhole Optimized Final 변환 데이터 저장 (Train≠0 최적화, 각도 제한 ✅)
            ephemerisTrackMstStorage.addAll(processedData.keyholeOptimizedFinalTransformedMst)
            ephemerisTrackDtlStorage.addAll(processedData.keyholeOptimizedFinalTransformedDtl)
            logger.info("✅ Keyhole Optimized Final 저장: ${processedData.keyholeOptimizedFinalTransformedMst.size} Mst, ${processedData.keyholeOptimizedFinalTransformedDtl.size} Dtl")
            // 🔍 디버깅: 저장된 MST 데이터 상세 정보
            processedData.keyholeOptimizedFinalTransformedMst.forEach { mst ->
                logger.info("   저장된 MST - No: ${mst["No"]}, RecommendedTrainAngle: ${mst["RecommendedTrainAngle"]}, MaxAzRate: ${mst["MaxAzRate"]}, DataType: ${mst["DataType"]}")
            }

            logger.info("✅ 저장 완료: 총 ${ephemerisTrackMstStorage.size}개 Mst, ${ephemerisTrackDtlStorage.size}개 Dtl")
            logger.info("🎉 위성 궤도 추적 완료")

            // 최종 변환된 데이터 반환
            return Pair(processedData.finalTransformedMst, processedData.finalTransformedDtl)

        } catch (e: Exception) {
            logger.error("❌ 위성 궤도 추적 실패: ${e.message}", e)
            throw e
        }
    }

    /**
     * ⏱️ 성능 측정 헬퍼 함수
     */
    private fun <T> measurePerformance(name: String, block: () -> T): T {
        val start = System.nanoTime()
        return block().also {
            val duration = (System.nanoTime() - start) / 1_000_000
            logger.info("⏱️ $name 총 소요 시간: ${duration}ms")
        }
    }

    /**
     * ✅ 2단계: 축변환 적용 (기울기 변환)
     */
    private fun applyAxisTransformation(
        originalMst: List<Map<String, Any?>>,
        originalDtl: List<Map<String, Any?>>
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        logger.info("🔄 2단계: 축변환 적용 시작 (기울기 변환)")

        val axisTransformedDtl = mutableListOf<Map<String, Any?>>()
        val axisTransformedMst = mutableListOf<Map<String, Any?>>()

        // 각 패스별로 축변환 적용
        originalMst.forEach { originalMstData ->
            val mstId = originalMstData["No"] as UInt
            val passDtl = originalDtl.filter { it["MstId"] == mstId }

            logger.info("패스 #$mstId 축변환 처리 중: ${passDtl.size}개 좌표")

            val transformedPassDtl = mutableListOf<Map<String, Any?>>()

            // 각 좌표에 축변환 적용
            passDtl.forEachIndexed { index, originalPoint ->
                val originalAzimuth = originalPoint["Azimuth"] as Double
                val originalElevation = originalPoint["Elevation"] as Double

                // 축변환 적용 (기울기 -7도, 회전체 0도)
                val (transformedAzimuth, transformedElevation) = CoordinateTransformer.transformCoordinatesWithTrain(
                    azimuth = originalAzimuth,
                    elevation = originalElevation,
                    tiltAngle = settingsService.tiltAngle,
                    trainAngle = 0.0
                )

                // 변환된 좌표로 새로운 데이터 포인트 생성
                val transformedPoint = mapOf(
                    "No" to originalPoint["No"],
                    "MstId" to originalPoint["MstId"],
                    "Time" to originalPoint["Time"],
                    "Azimuth" to transformedAzimuth,
                    "Elevation" to transformedElevation,
                    "Range" to originalPoint["Range"],
                    "Altitude" to originalPoint["Altitude"],
                    "OriginalAzimuth" to originalAzimuth,
                    "OriginalElevation" to originalElevation,
                    "TiltAngle" to settingsService.tiltAngle,
                    "trainAngle" to 0.0,
                    "TransformationType" to "axis_transform",
                    "DataType" to "axis_transformed"
                )

                transformedPassDtl.add(transformedPoint)

                // 진행률 로깅 (1000개마다)
                if ((index + 1) % 1000 == 0) {
                    logger.info("패스 #$mstId 축변환 진행률: ${index + 1}/${passDtl.size} (${((index + 1) * 100.0 / passDtl.size).toInt()}%)")
                }
            }

            // 변환된 시계열에서 속도와 가속도 계산
            logger.info("패스 #$mstId 변환된 시계열에서 속도/가속도 계산 중")
            val calculatedDtl = calculateVelocityAndAcceleration(transformedPassDtl)

            // 변환된 데이터에서 실제 최대값들 다시 계산
            var actualMaxElevation = -90.0
            var actualMaxElevationTime: ZonedDateTime? = null
            var maxElevationAzimuth = 0.0
            var maxAzRate = 0.0
            var maxElRate = 0.0
            var maxAzAccel = 0.0
            var maxElAccel = 0.0

            calculatedDtl.forEach { point ->
                val transformedElevation = point["Elevation"] as Double
                val timestamp = point["Time"] as ZonedDateTime
                val azRate = point["AzimuthRate"] as? Double ?: 0.0
                val elRate = point["ElevationRate"] as? Double ?: 0.0
                val azAccel = point["AzimuthAccel"] as? Double ?: 0.0
                val elAccel = point["ElevationAccel"] as? Double ?: 0.0
                val azimuth = point["Azimuth"] as Double

                if (transformedElevation > actualMaxElevation) {
                    actualMaxElevation = transformedElevation
                    actualMaxElevationTime = timestamp
                    maxElevationAzimuth = azimuth
                }
                maxAzRate = maxOf(maxAzRate, abs(azRate))
                maxElRate = maxOf(maxElRate, abs(elRate))
                maxAzAccel = maxOf(maxAzAccel, abs(azAccel))
                maxElAccel = maxOf(maxElAccel, abs(elAccel))
            }

            logger.info(
                "패스 #$mstId 변환된 데이터 최대값: 고도각=${
                    String.format(
                        "%.2f",
                        actualMaxElevation
                    )
                }°, 원본=${String.format("%.2f", originalMstData["MaxElevation"])}°"
            )

            // 축변환된 마스터 데이터 생성
            val axisTransformedMstData = originalMstData.toMutableMap().apply {
                put("TiltAngle", settingsService.tiltAngle)
                put("RotatorAngle", 0.0)
                put("TransformationType", "axis_transform")
                put("OriginalDataCount", passDtl.size)
                put("TransformedDataCount", calculatedDtl.size)
                put("MaxAzRate", maxAzRate)
                put("MaxElRate", maxElRate)
                put("MaxAzAccel", maxAzAccel)
                put("MaxElAccel", maxElAccel)
                put("MaxAzimuth", maxElevationAzimuth)
                put("OriginalMaxAzimuth", originalMstData["MaxAzimuth"])
                put("MaxElevation", actualMaxElevation)
                put("MaxElevationTime", actualMaxElevationTime)
                put("OriginalMaxElevation", originalMstData["MaxElevation"])
                put("OriginalMaxElevationTime", originalMstData["MaxElevationTime"])
                put("DataType", "axis_transformed")
            }

            axisTransformedMst.add(axisTransformedMstData)
            axisTransformedDtl.addAll(calculatedDtl)

            // ✅ 원본 데이터의 MaxElevation 시점 인덱스 찾기 (MaxElevation 값으로 직접 찾기)
            val originalMaxElevation = originalMstData["MaxElevation"] as? Double
            val originalMaxElevationIndex = if (originalMaxElevation != null) {
                passDtl.mapIndexed { index, point ->
                    val elevation = point["Elevation"] as Double
                    val diff = abs(elevation - originalMaxElevation)
                    Triple(index, elevation, diff)
                }.minByOrNull { it.third }?.first ?: -1
            } else {
                -1
            }

            logger.info("패스 #$mstId 축변환 완료: ${calculatedDtl.size}개 좌표")
            val originalMaxAz = originalMstData["MaxAzimuth"] as? Double ?: 0.0
            val originalMaxEl = originalMstData["MaxElevation"] as? Double ?: 0.0

            // ✅ 정확한 매칭 로그 출력 (인덱스 포함)
            if (originalMaxElevationIndex >= 0 && originalMaxElevationIndex < calculatedDtl.size) {
                val originalMaxElevationAz = passDtl[originalMaxElevationIndex]["Azimuth"] as Double
                val originalMaxElevationEl = passDtl[originalMaxElevationIndex]["Elevation"] as Double
                val transformedMaxElevationAz = calculatedDtl[originalMaxElevationIndex]["Azimuth"] as Double
                val transformedMaxElevationEl = calculatedDtl[originalMaxElevationIndex]["Elevation"] as Double
                val originalPointTime = passDtl[originalMaxElevationIndex]["Time"] as ZonedDateTime

                logger.info(
                    "  MaxElevation 시점 매칭 [이론치 인덱스 $originalMaxElevationIndex]: 원본 Az=${
                        String.format(
                            "%.4f",
                            originalMaxElevationAz
                        )
                    }° El=${String.format("%.4f", originalMaxElevationEl)}° → 변환 Az=${
                        String.format(
                            "%.4f",
                            transformedMaxElevationAz
                        )
                    }° El=${String.format("%.4f", transformedMaxElevationEl)}°"
                )
                logger.info(
                    "  매칭 시간: ${originalPointTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))} (MaxElevation 값: ${
                        String.format(
                            "%.6f",
                            originalMaxElevation
                        )
                    }°)"
                )
            } else {
                logger.warn("  MaxElevation 시점 매칭 실패: 이론치 인덱스 $originalMaxElevationIndex")
                if (originalMaxElevation != null) {
                    logger.warn("  원본 MaxElevation 값: ${String.format("%.6f", originalMaxElevation)}°")
                }
            }

            logger.info(
                "  최대 방위각: 원본=${String.format("%.2f", originalMaxAz)}° → 변환=${
                    String.format(
                        "%.2f",
                        maxElevationAzimuth
                    )
                }°"
            )
            logger.info(
                "  최대 고도각: 원본=${String.format("%.2f", originalMaxEl)}° → 변환=${
                    String.format(
                        "%.2f",
                        actualMaxElevation
                    )
                }°"
            )
        }

        logger.info("🔄 2단계 완료: 축변환 적용 - ${axisTransformedMst.size}개 패스, ${axisTransformedDtl.size}개 좌표")
        logger.info("변환 정보: 기울기=${settingsService.tiltAngle}°, 회전체=0도")
        return Pair(axisTransformedMst, axisTransformedDtl)
    }

    /**
     * ✅ 3단계: 방위각 변환 (±270도 제한)
     */
    private fun applyAngleLimitTransformation(
        axisTransformedMst: List<Map<String, Any?>>,
        axisTransformedDtl: List<Map<String, Any?>>
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        logger.info("📐 3단계: 방위각 변환 시작 (0~360도 -> ±270도)")

        val (tempMst, tempDtl) = limitAngleCalculator.convertTrackingData(
            axisTransformedMst, axisTransformedDtl
        )

        // 최종 변환 데이터에 데이터 타입 설정
        val finalMst = tempMst.map { mst ->
            mst.toMutableMap().apply {
                put("DataType", "final_transformed")
            }
        }

        val finalDtl = tempDtl.map { dtl ->
            dtl.toMutableMap().apply {
                put("DataType", "final_transformed")
            }
        }

        logger.info("📐 3단계 완료: 방위각 변환 - ${finalMst.size}개 마스터, ${finalDtl.size}개 세부")

        // 최종 변환 결과 로깅
        finalMst.forEach { mst ->
            val mstId = mst["No"] as UInt
            val originalMaxAz = mst["OriginalMaxAzimuth"] as? Double
            val axisTransformedMaxAz = mst["MaxAzimuth"] as? Double
            val finalStartAz = mst["StartAzimuth"] as Double
            val finalEndAz = mst["EndAzimuth"] as Double

            logger.info("패스 #$mstId 최종 변환 결과:")
            logger.info("  원본 최대 방위각: ${String.format("%.2f", originalMaxAz)}°")
            logger.info("  축변환 최대 방위각: ${String.format("%.2f", axisTransformedMaxAz)}°")
            logger.info("  최종 방위각 범위: ${String.format("%.2f", finalStartAz)}° ~ ${String.format("%.2f", finalEndAz)}°")
        }

        return Pair(finalMst, finalDtl)
    }

    /**
     * ✅ 4단계: 모든 변환 데이터 저장
     */
    private fun saveAllTransformationData(
        originalMst: List<Map<String, Any?>>,
        originalDtl: List<Map<String, Any?>>,
        axisTransformedMst: List<Map<String, Any?>>,
        axisTransformedDtl: List<Map<String, Any?>>,
        finalMst: List<Map<String, Any?>>,
        finalDtl: List<Map<String, Any?>>
    ) {
        logger.info("💾 4단계: 모든 변환 데이터 저장 시작")

        // 저장소 초기화
        ephemerisTrackMstStorage.clear()
        ephemerisTrackDtlStorage.clear()

        // 원본 데이터 저장
        ephemerisTrackMstStorage.addAll(originalMst)
        ephemerisTrackDtlStorage.addAll(originalDtl)

        // 축변환 데이터 저장
        ephemerisTrackMstStorage.addAll(axisTransformedMst)
        ephemerisTrackDtlStorage.addAll(axisTransformedDtl)

        // 최종 변환 데이터 저장
        ephemerisTrackMstStorage.addAll(finalMst)
        ephemerisTrackDtlStorage.addAll(finalDtl)

        logger.info("💾 4단계 완료: 모든 변환 데이터 저장")
        logger.info("  - 원본 데이터: ${originalMst.size}개 마스터, ${originalDtl.size}개 세부")
        logger.info("  - 축변환 데이터: ${axisTransformedMst.size}개 마스터, ${axisTransformedDtl.size}개 세부")
        logger.info("  - 최종 변환 데이터: ${finalMst.size}개 마스터, ${finalDtl.size}개 세부")
    }


    // Tilt만 특정 각도로 이동
    private fun moveTrainToZero(TrainAngle: Float) {
        val multiAxis = BitSet()
        multiAxis.set(2)  // Tilt 축만 활성화
        // ✅ CMD에 offset 적용 (trainPositionOffset + trueNorthOffset)
        PushData.CMD.cmdTrainAngle = TrainAngle + GlobalData.Offset.trainPositionOffset + GlobalData.Offset.trueNorthOffset
        udpFwICDService.singleManualCommand(
            multiAxis, TrainAngle, 5f
        )

        logger.info("🔄 TrainAngle를 ${TrainAngle}° 로 이동 시작 (cmdTrainAngle=${PushData.CMD.cmdTrainAngle}°, trainPosOffset=${GlobalData.Offset.trainPositionOffset}°, trueNorthOffset=${GlobalData.Offset.trueNorthOffset}°)")
    }

    // 목표 Az/El로 이동
    private fun moveToTargetAzEl() {
        GlobalData.EphemerisTrakingAngle.azimuthAngle = targetAzimuth
        GlobalData.EphemerisTrakingAngle.elevationAngle = targetElevation

        // ✅ PushData.CMD에 목표 위치 설정 (Dashboard 표시용)
        PushData.CMD.cmdAzimuthAngle = targetAzimuth + GlobalData.Offset.azimuthPositionOffset
        PushData.CMD.cmdElevationAngle = targetElevation + GlobalData.Offset.elevationPositionOffset

        val multiAxis = BitSet()
        multiAxis.set(0)  // Azimuth
        multiAxis.set(1)  // Elevation
        // Train은 현재 CMD 값 그대로 유지
        val currentTrainCmd = PushData.CMD.cmdTrainAngle ?: 0f
        udpFwICDService.multiManualCommand(
            multiAxis, targetAzimuth, 5f, targetElevation, 5f, currentTrainCmd, 0f
        )
        logger.info("🔄 목표 Az/El로 이동: Az=${targetAzimuth}°, El=${targetElevation}° (CMD: Az=${PushData.CMD.cmdAzimuthAngle}°, El=${PushData.CMD.cmdElevationAngle}°)")
    }

    // Train가 0에 도달했는지 확인
    private fun isTrainAtZero(): Boolean {
        val cmdTilt = PushData.CMD.cmdTrainAngle ?: 0f  // null이면 0f 사용
        val currentTilt = dataStoreService.getLatestData().trainAngle ?: 0.0
        return kotlin.math.abs(cmdTilt - currentTilt.toFloat()) <= 0.1f
    }

    // Train가 안정화되었는지 확인
    private fun isTrainStabilized(): Boolean {
        val cmdTilt = PushData.CMD.cmdTrainAngle ?: 0f  // null이면 0f 사용
        val currentTilt = dataStoreService.getLatestData().trainAngle ?: 0.0
        return kotlin.math.abs(cmdTilt - currentTilt.toFloat()) <= 0.1f
    }

    // Azimuth/Elevation이 목표 위치에 도달했는지 확인
    private fun isAzElAtTarget(): Boolean {
        val cmdAz = targetAzimuth
        val cmdEl = targetElevation
        val currentAz = dataStoreService.getLatestData().azimuthAngle ?: 0.0
        val currentEl = dataStoreService.getLatestData().elevationAngle ?: 0.0
        
        val azDiff = kotlin.math.abs(cmdAz - currentAz.toFloat())
        val elDiff = kotlin.math.abs(cmdEl - currentEl.toFloat())
        
        // 1.0도 이내면 도달한 것으로 간주 (원래 로직)
        return azDiff <= 1.0f && elDiff <= 1.0f
    }

    /**
     * 위성 추적 시작
     * 
     * 위성 추적을 시작하고 상태머신을 초기화합니다.
     * Keyhole 여부에 따라 적절한 MST를 currentTrackingPass에 설정합니다.
     * 
     * ✅ mstId와 detailId를 사용하여 추적 시작 (PassSchedule과 동일한 구조)
     * 
     * @param mstId 추적할 마스터 ID
     * @param detailId 패스 인덱스 (기본값: 0)
     * 
     * @see getTrackingPassMst Keyhole 여부에 따라 적절한 MST 선택
     * @see moveToStartPosition 시작 위치로 이동
     * @see startModeTimer 모드 타이머 시작
     */
    fun startEphemerisTracking(mstId: Long, detailId: Int = 0) {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        logger.info("🚀 위성 추적 시작: mstId = {}, detailId = {}", mstId, detailId)
        stopModeTimer()

        // ✅ 상태 초기화 (executedActions 대신 상태 기반 관리)
        currentTrackingState = TrackingState.IDLE
        currentPreparingPhase = PreparingPhase.TRAIN_MOVING
        headerSent = false
        initialDataSent = false
        trainMoveCommandTime = 0  // ✅ Train 이동 명령 시간 초기화
        azElMoveCommandTime = 0   // ✅ Az/El 이동 명령 시간 초기화
        keyholeWarningLogged = false  // ✅ Keyhole 경고 로그 플래그 초기화

        // ✅ 이전 추적의 tracking 각도 값 초기화 (TRACKING 전환 시 이전 값으로 점프 방지)
        dataStoreService.clearTrackingAngles()

        // ✅ 이전 추적의 실시간 데이터 초기화 (새 추적 시작 시 이전 데이터 제거)
        clearRealtimeTrackingData()

        logger.info("🔄 상태 초기화 완료: state=${currentTrackingState}, phase=${currentPreparingPhase}")

        currentTrackingPassId = mstId
        
        // ✅ Keyhole 여부에 따라 적절한 MST 선택
        // Keyhole 발생: keyhole_final_transformed MST
        // Keyhole 미발생: final_transformed MST
        val selectedPass = getTrackingPassMst(mstId)
        
        if (selectedPass == null) {
            logger.error("MstId {}에 해당하는 데이터를 찾을 수 없습니다", mstId)
            return
        }
        
        // ✅ selectedPass에서 DetailId를 가져오기 (파라미터보다 우선)
        val actualDetailId = (selectedPass["DetailId"] as? Number)?.toInt() ?: detailId
        logger.info("📊 MST에서 DetailId 추출: mstId=${mstId}, MST DetailId=${selectedPass["DetailId"]}, 파라미터 detailId=${detailId}, 사용할 actualDetailId=${actualDetailId}")
        
        // ✅ 로컬 변수에 할당하여 smart cast 문제 해결
        // ✅ DetailId를 명시적으로 저장 (PassSchedule과 동일)
        currentTrackingPass = selectedPass.toMutableMap().apply {
            put("DetailId", actualDetailId)  // ✅ MST에서 가져온 detailId 저장
        }
        
        // Keyhole 정보 로깅
        val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
        val recommendedTrainAngle = selectedPass["RecommendedTrainAngle"] as? Double ?: 0.0
        logger.info("📊 추적 패스 정보: Keyhole=${if (isKeyhole) "YES" else "NO"}, RecommendedTrainAngle=${recommendedTrainAngle}°, DetailId=${actualDetailId}")

        // ✅ 추적 시간 확인 - 추적 중이면 시작 위치 건너뛰고 바로 TRACKING
        val startTime = try {
            (selectedPass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            logger.error("StartTime 추출 실패: {}", e.message, e)
            null
        }

        val endTime = try {
            (selectedPass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            logger.error("EndTime 추출 실패: {}", e.message, e)
            null
        }

        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        val isInTrackingTime = if (startTime != null && endTime != null) {
            calTime.isAfter(startTime) && calTime.isBefore(endTime)
        } else {
            false
        }

        logger.info("⏰ 추적 시간 확인: 현재=${calTime}, 시작=${startTime}, 종료=${endTime}, 추적중=${isInTrackingTime}")

        if (isInTrackingTime) {
            // ✅ 추적 시간 중 → 시작 위치 건너뛰고 바로 TRACKING 상태로 전환
            logger.info("🎯 추적 시간 중! 시작 위치 건너뛰고 현재 위치에서 즉시 TRACKING 시작")

            // ✅ Train 각도는 현재 위치 유지 (이동 명령 생략하여 이동 중 멈춤 방지)
            // Train 이동 명령을 보내지 않고 현재 Train 위치에서 바로 추적 시작
            val currentTrainAngle = dataStoreService.getLatestData().trainAngle?.toFloat() ?: 0f
            GlobalData.EphemerisTrakingAngle.trainAngle = currentTrainAngle
            logger.info("🔧 Train 현재 위치 유지: {}° (이동 명령 생략)", currentTrainAngle)

            // ✅ 상태를 먼저 설정 (sendInitialTrackingData에서 ephemerisStatus 체크하므로)
            currentTrackingState = TrackingState.TRACKING
            trackingStatus.ephemerisStatus = true
            trackingStatus.ephemerisTrackingState = "TRACKING"
            dataStoreService.setEphemerisTracking(true)
            dataStoreService.updateTrackingStatus(trackingStatus)  // ✅ WebSocket 전송용 상태 동기화
            logger.info("✅ TRACKING 상태 먼저 설정 (12.2 전송 전)")

            // ✅ 12.1 헤더 전송
            sendHeaderTrackingData(mstId, actualDetailId)
            headerSent = true
            logger.info("📡 12.1 헤더 전송 완료 (재추적)")

            // ✅ 12.2 초기 데이터 전송 (펌웨어 제어 시작에 필수!)
            sendInitialTrackingData(mstId, actualDetailId)
            initialDataSent = true
            logger.info("📡 12.2 초기 데이터 전송 완료 (재추적)")

            logger.info("✅ 즉시 TRACKING 상태로 전환 완료 (Train 이동 없이)")
        } else {
            // ✅ 추적 시간 이전 → 정상 플로우 (시작 위치로 이동 → WAITING)
            logger.info("⏰ 추적 시작 시간 이전 - 시작 위치로 이동 후 WAITING")
            moveToStartPosition(mstId, actualDetailId)
        }

        startModeTimer()
        logger.info("✅ 위성 추적 및 통합 모드 타이머 시작 완료")
    }

    /**
     * 위성 추적 중지 (안전한 배치 종료 포함)
     */
    fun stopEphemerisTracking() {
        // ✅ 내부 상태머신 진행용으로 ephemerisStatus 사용
        if (trackingStatus.ephemerisStatus != true && trackingStatus.geostationaryStatus != true) {
            logger.info("위성 추적이 이미 중지되어 있습니다.")
            return
        }
        logger.info("위성 추적 중지")
        stopCommand()

        // ✅ 공통 상태머신 초기화
        currentTrackingState = TrackingState.IDLE
        stabilizationStartTime = 0
        targetAzimuth = 0f
        targetElevation = 0f
        trainMoveCommandTime = 0  // ✅ 명령 전송 시간 초기화
        azElMoveCommandTime = 0  // ✅ 명령 전송 시간 초기화
        
        // ✅ 대기 상태 로그 변수 초기화
        lastWaitingLogTime = 0
        lastWaitingTimeDifference = -1
        lastWaitingAzimuth = 0.0
        lastWaitingElevation = 0.0

        // ✅ 정지궤도 추적 상태 초기화
        if (trackingStatus.geostationaryStatus == true) {
            trackingStatus.geostationaryStatus = false
        }

        // ✅ ephemeris 상태도 초기화 (내부 상태 + 프론트엔드 전달)
        trackingStatus.ephemerisStatus = false
        trackingStatus.ephemerisTrackingState = "IDLE"
        // ✅ DataStoreService에 상태 동기화 (중요!)
        dataStoreService.updateTrackingStatus(trackingStatus)
        dataStoreService.setEphemerisTracking(false) // ✅ 프론트엔드에 추적 종료 알림
        logger.info("✅ 추적 상태 초기화: ephemerisStatus=false, ephemerisTrackingState=IDLE")

        // ✅ 안전한 배치 종료 처리
        safeBatchShutdown()
        // ✅ 통합 모드 타이머 중지
        stopModeTimer()
        clearRealtimeTrackingData()
        dataStoreService.stopAllTracking()
        logger.info("✅ 위성 추적 및 통합 모드 타이머 중지 완료")
    }

    /**
     * ✅ 안전한 배치 종료 처리
     */
    private fun safeBatchShutdown() {
        try {
            logger.info("🔄 안전한 배치 종료 처리 시작")
            val batchShutdownSuccess = batchStorageManager.safeShutdown()
            if (batchShutdownSuccess) {
                logger.info("✅ 배치 데이터 안전 종료 완료")
            } else {
                logger.warn("⚠️ 배치 데이터 일부 손실 가능성")
            }
            val finalStats = batchStorageManager.getRealtimeTrackingStats()
            logger.info("📊 최종 배치 처리 통계:")
            logger.info("  - 총 처리된 데이터: ${finalStats["totalCount"]}개")
            logger.info("  - 평균 Az 오차: ${finalStats["averageAzimuthError"]}°")
            logger.info("  - 평균 El 오차: ${finalStats["averageElevationError"]}°")
        } catch (e: Exception) {
            logger.error("❌ 안전한 배치 종료 처리 중 오류: ${e.message}", e)
        }
    }

    /**
     * ✅ 통합 모드 실행 (기존 startTimer() 메서드 대체)
     */
    private fun startModeTimer() {
        // 기존 타이머가 있다면 정리
        stopModeTimer()

        // ✅ 위성 추적 시작 상태 설정 (이미 TRACKING 상태면 덮어쓰지 않음)
        trackingStatus.ephemerisStatus = true
        if (currentTrackingState != TrackingState.TRACKING) {
            trackingStatus.ephemerisTrackingState = "TRAIN_MOVING_TO_ZERO"
            logger.info("🚀 위성 추적 시작 - Tilt 시작 위치로 이동")
        } else {
            logger.info("🚀 위성 추적 시작 - 이미 TRACKING 상태, 상태 유지")
        }

        // ✅ 통합 추적 실행기 사용 (NORMAL 우선순위)
        trackingExecutor = threadManager.getTrackingExecutor()

        // ✅ 안정성 우선 스케줄링
        modeTask = trackingExecutor?.scheduleAtFixedRate(
            {
                try {
                    val startTime = System.nanoTime()
                    trackingSatelliteStateCheck()

                    // ✅ 정확한 성능 모니터링
                    val processingTime = (System.nanoTime() - startTime) / 1_000_000
                    if (processingTime > 100) {  // 100ms 임계값으로 정확한 보장
                        logger.warn("⚠️ 100ms 저장 지연 감지: {}ms (임계값: 100ms)", processingTime)
                    }
                } catch (e: Exception) {
                    logger.error("위성 추적 상태 체크 중 오류: ${e.message}", e)
                }
            }, 0,      // 초기 지연 시간
            100,    // 실행 간격 (100ms) - 유지
            TimeUnit.MILLISECONDS
        )

        logger.info("⏰ 정확한 성능 모니터링 100ms 주기 타이머 시작")
    }

    /**
     * ✅ 통합 모드 타이머 중지 (기존 stopTimer() 메서드 대체)
     */
    private fun stopModeTimer() {
        // 실행 중인 작업 취소
        modeTask?.let { task ->
            if (!task.isCancelled) {
                task.cancel(false) // 진행 중인 작업은 완료하도록 함
                logger.debug("⏹️ 모드 작업 취소 완료")
            }
        }
        modeTask = null

        // ExecutorService 종료 (공유 실행기이므로 완전 종료하지 않음)
        logger.info("⏹️ 통합 모드 타이머 정리 완료")
    }

    /**
     * ✅ 모드 타이머 상태 확인 (기존 isTimerRunning() 메서드 수정)
     */
    fun isTimerRunning(): Boolean {
        return trackingExecutor != null && modeTask != null && !modeTask!!.isCancelled
    }

    /**
     * ✅ 모드 타이머 상세 상태 정보 (새로운 메서드)
     */
    fun getTimerStatus(): Map<String, Any> {
        val executor = trackingExecutor
        val task = modeTask

        return mapOf(
            "isRunning" to isTimerRunning(),
            "executorExists" to (executor != null),
            "taskExists" to (task != null),
            "taskCancelled" to (task?.isCancelled ?: true),
            "taskDone" to (task?.isDone ?: true),
            "threadName" to "tracking-worker"
        )
    }

    /**
     * 100ms 주기 상태 체크 (핵심 로직)
     * ✅ 리팩토링: executedActions 제거, 상태 기반 관리로 단순화
     */
    private fun trackingSatelliteStateCheck() {
        try {
            if (trackingStatus.ephemerisStatus != true) {
                return
            }

            when (currentTrackingState) {
                TrackingState.PREPARING -> handlePreparingState()
                TrackingState.WAITING -> handleWaitingState()
                TrackingState.TRACKING -> handleTrackingState()
                TrackingState.COMPLETED -> { /* 완료 상태 - 추가 처리 없음 */ }
                TrackingState.ERROR -> { /* 오류 상태 - 추가 처리 없음 */ }
                TrackingState.IDLE -> { /* 대기 상태 - 추가 처리 없음 */ }
            }
        } catch (e: Exception) {
            logger.error("추적 상태 체크 오류: ${e.message}", e)
            currentTrackingState = TrackingState.ERROR
            trackingStatus.ephemerisTrackingState = "ERROR"
        }
    }

    /**
     * PREPARING 상태 처리 (Train 이동 → 안정화 → Az/El 이동)
     */
    private fun handlePreparingState() {
        trackingStatus.ephemerisTrackingState = "PREPARING"
        dataStoreService.updateTrackingStatus(trackingStatus)

        when (currentPreparingPhase) {
            PreparingPhase.TRAIN_MOVING -> {
                // Train 각도 이동 (한 번만 명령 전송 - trainMoveCommandTime으로 판단)
                if (trainMoveCommandTime == 0L) {
                    val recommendedTrainAngle = currentTrackingPass?.get("RecommendedTrainAngle") as? Double ?: 0.0
                    val isKeyhole = currentTrackingPass?.get("IsKeyhole") as? Boolean ?: false
                    val trainAngle = if (isKeyhole) recommendedTrainAngle.toFloat() else 0f

                    GlobalData.EphemerisTrakingAngle.trainAngle = trainAngle
                    moveTrainToZero(trainAngle)
                    trainMoveCommandTime = System.currentTimeMillis()
                    logger.info("🔄 Train 각도 설정: Keyhole=${if (isKeyhole) "YES" else "NO"}, Train=${trainAngle}°")
                }

                // Train 각도 도달 확인
                val cmdTilt = PushData.CMD.cmdTrainAngle ?: 0f
                val currentTilt = dataStoreService.getLatestData().trainAngle ?: 0.0
                val timeSinceCommand = System.currentTimeMillis() - trainMoveCommandTime

                if (timeSinceCommand >= 500 && isTrainAtZero()) {
                    currentPreparingPhase = PreparingPhase.TRAIN_STABILIZING
                    stabilizationStartTime = System.currentTimeMillis()
                    logger.info("✅ Train 목표 도달, 안정화 대기 시작 (cmd=${cmdTilt}°, current=${currentTilt}°)")
                } else if (timeSinceCommand % 5000 < 100) {
                    logger.info("⏳ Train 이동 중: 목표=${cmdTilt}°, 현재=${currentTilt}°")
                }
            }

            PreparingPhase.TRAIN_STABILIZING -> {
                val elapsedTime = System.currentTimeMillis() - stabilizationStartTime
                if (elapsedTime >= TRAIN_STABILIZATION_TIMEOUT * 1000) {
                    currentPreparingPhase = PreparingPhase.MOVING_TO_TARGET
                    logger.info("✅ Train 안정화 완료, 목표 위치로 이동 시작")
                }
            }

            PreparingPhase.MOVING_TO_TARGET -> {
                // Az/El 이동 명령 (한 번만 - azElMoveCommandTime으로 판단)
                if (azElMoveCommandTime == 0L) {
                    moveToTargetAzEl()
                    azElMoveCommandTime = System.currentTimeMillis()
                    logger.info("🔄 목표 Az/El 이동 명령: Az=${targetAzimuth}°, El=${targetElevation}°")
                }

                // ✅ 목표 위치 도달 확인 (±0.05° 허용 오차, 2분 타임아웃)
                val latestData = dataStoreService.getLatestData()
                val currentAz = latestData.azimuthAngle ?: 0.0f
                val currentEl = latestData.elevationAngle ?: 0.0f
                val azDiff = kotlin.math.abs(currentAz - targetAzimuth)
                val elDiff = kotlin.math.abs(currentEl - targetElevation)
                val timeSinceCommand = System.currentTimeMillis() - azElMoveCommandTime

                // ✅ CRITICAL: 모터 정지 상태 확인
                val azStatusBits = latestData.azimuthBoardServoStatusBits
                val elStatusBits = latestData.elevationBoardServoStatusBits

                // 비트 7 체크: 0 = 정지, 1 = 움직이는 중
                val isAzMoving = azStatusBits?.get(7) == '1'
                val isElMoving = elStatusBits?.get(7) == '1'
                val isMotorStopped = !isAzMoving && !isElMoving

                // 🔧 FIX: 목표 El이 0 이하일 때 현재 El이 0 근처면 도달한 것으로 처리
                // (안테나가 물리적으로 0° 이하로 내려갈 수 없음)
                val isElAtTarget = if (targetElevation <= 0f) {
                    currentEl <= 0.5f  // El 0° 근처면 도달
                } else {
                    elDiff < 0.05f
                }

                // 각도가 범위 내인지 확인
                val isAngleClose = azDiff < 0.05f && isElAtTarget

                // ✅ 도달 조건:
                // 1) 각도 차이 < 0.05° AND 모터 정지 AND 3초 안정화 대기 → 도달
                // 2) 각도 차이 < 0.05° AND 모터가 계속 움직이는 중 → 10초 타임아웃 후 도달
                val isAtTarget = if (isAngleClose) {
                    if (isMotorStopped) {
                        // 모터 정지 상태여도 3초 안정화 대기
                        timeSinceCommand >= 3_000
                    } else {
                        // 모터가 움직이는 중이면 10초 타임아웃 후 강제 도달
                        timeSinceCommand >= 10_000
                    }
                } else {
                    false
                }

                val isTimeout = timeSinceCommand > 120_000  // 2분 전체 타임아웃

                // 5초마다 진행 상황 로깅
                if (timeSinceCommand % 5000 < 100) {
                    val statusMsg = when {
                        isAngleClose && isMotorStopped -> "목표 각도 도달 및 모터 정지, 안정화 대기 중 (${timeSinceCommand/1000}초/3초)"
                        isAngleClose && !isMotorStopped -> "목표 각도 도달, 모터 정지 대기 중 (${timeSinceCommand/1000}초/10초)"
                        else -> "목표 위치 이동 중"
                    }
                    logger.info("⏳ ${statusMsg}: 현재 Az=${currentAz}°, El=${currentEl}° → 목표 Az=${targetAzimuth}°, El=${targetElevation}° (차이: Az=${azDiff}°, El=${elDiff}°, Az모터=${if(isAzMoving) "이동중" else "정지"}, El모터=${if(isElMoving) "이동중" else "정지"})")
                }

                // 목표 도달 또는 타임아웃 시 WAITING 상태로 전환
                if (isAtTarget || isTimeout) {
                    if (isTimeout && !isAtTarget) {
                        logger.warn("⚠️ 목표 위치 이동 타임아웃 (2분). 현재 위치: Az=${currentAz}°, El=${currentEl}°")
                    } else if (isAngleClose && !isMotorStopped) {
                        logger.warn("⚠️ 목표 각도 도달했으나 모터 정지 확인 타임아웃 (10초). 강제로 WAITING 상태로 전환")
                    } else {
                        logger.info("✅ 목표 위치 도달 및 모터 정지 후 3초 안정화 완료: Az=${currentAz}°, El=${currentEl}°")
                    }

                    currentTrackingState = TrackingState.WAITING
                    trackingStatus.ephemerisTrackingState = "WAITING"
                    dataStoreService.updateTrackingStatus(trackingStatus)

                    // 12.1 헤더 전송 (WAITING 진입 시)
                    if (!headerSent) {
                        val mstId = currentTrackingPassId ?: return
                        val detailId = (currentTrackingPass?.get("DetailId") as? Number)?.toInt() ?: 0
                        logger.info("📡 12.1 헤더 전송 시작")
                        sendHeaderTrackingData(mstId, detailId)
                        headerSent = true
                        logger.info("✅ 12.1 헤더 전송 완료")
                    }

                    logger.info("✅ 시작 위치 이동 완료, WAITING 상태로 전환")
                }
            }
        }
    }

    /**
     * WAITING 상태 처리 (시작 시간 대기)
     */
    private fun handleWaitingState() {
        val mstId = currentTrackingPassId ?: return
        val detailId = (currentTrackingPass?.get("DetailId") as? Number)?.toInt() ?: 0
        val (startTime, endTime) = getCurrentTrackingPassTimes()
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        val timeDifference = Duration.between(startTime, calTime).seconds

        val currentAz = (dataStoreService.getLatestData().azimuthAngle ?: 0.0f).toDouble()
        val currentEl = (dataStoreService.getLatestData().elevationAngle ?: 0.0f).toDouble()

        logger.debug("⏰ WAITING 상태 - 시간차: {}초, 현재: Az={}°, El={}°", timeDifference, currentAz, currentEl)

        when {
            // 시작 시간 도달 → TRACKING으로 전환
            timeDifference > 0 && calTime.isBefore(endTime) -> {
                // ✅ TRACKING 전환 전에 먼저 첫 번째 CMD 값 설정 (0으로 점프 방지)
                val firstTrackingData = createRealtimeTrackingData(mstId, detailId, calTime, startTime)
                if (firstTrackingData.isNotEmpty()) {
                    // ✅ Keyhole 여부 확인
                    val isKeyhole = currentTrackingPass?.get("IsKeyhole") as? Boolean ?: false

                    // ✅ Keyhole이면 keyholeFinalTransformed 값 우선 사용
                    val cmdAz = if (isKeyhole) {
                        (firstTrackingData["keyholeFinalTransformedAzimuth"] as? Number)?.toFloat()
                            ?: (firstTrackingData["axisTransformedAzimuth"] as? Number)?.toFloat()
                            ?: (firstTrackingData["finalTransformedAzimuth"] as? Number)?.toFloat()
                    } else {
                        (firstTrackingData["axisTransformedAzimuth"] as? Number)?.toFloat()
                            ?: (firstTrackingData["finalTransformedAzimuth"] as? Number)?.toFloat()
                    }

                    val cmdEl = if (isKeyhole) {
                        (firstTrackingData["keyholeFinalTransformedElevation"] as? Number)?.toFloat()
                            ?: (firstTrackingData["axisTransformedElevation"] as? Number)?.toFloat()
                            ?: (firstTrackingData["finalTransformedElevation"] as? Number)?.toFloat()
                    } else {
                        (firstTrackingData["axisTransformedElevation"] as? Number)?.toFloat()
                            ?: (firstTrackingData["finalTransformedElevation"] as? Number)?.toFloat()
                    }

                    // ✅ Train CMD는 moveTrainToZero()에서 이미 설정됨 - 덮어쓰지 않음
                    val cmdTrain = PushData.CMD.cmdTrainAngle ?: 0f

                    if (cmdAz != null && cmdEl != null) {
                        PushData.CMD.cmdAzimuthAngle = cmdAz
                        PushData.CMD.cmdElevationAngle = cmdEl
                        // Train CMD는 덮어쓰지 않음 (moveTrainToZero에서 설정한 값 유지)
                        logger.info("📡 TRACKING 전환 - 첫 CMD 설정 (Keyhole=${isKeyhole}): Az=${cmdAz}°, El=${cmdEl}°, Train=${cmdTrain}° (유지)")

                        // ✅ DataStore의 trackingCMD 값도 즉시 설정 (0,0 점프 방지)
                        val currentData = dataStoreService.getLatestData()
                        val initialTrackingData = currentData.copy(
                            trackingCMDAzimuthAngle = cmdAz,
                            trackingCMDElevationAngle = cmdEl,
                            trackingCMDTrainAngle = cmdTrain
                        )
                        dataStoreService.updateDataFromUdp(initialTrackingData)
                        logger.info("📡 TRACKING 전환 - trackingCMD 값 DataStore에 설정 완료")
                    }
                }

                currentTrackingState = TrackingState.TRACKING
                trackingStatus.ephemerisTrackingState = "TRACKING"
                dataStoreService.updateTrackingStatus(trackingStatus)

                logger.info("📡 추적 시작 - TRACKING 상태로 전환")
                logger.info("  - timeDifference: ${timeDifference}초 (시작 시간 도달)")

                // 12.1 헤더가 전송되지 않았다면 전송
                if (!headerSent) {
                    logger.info("📡 12.1 헤더 전송 (TRACKING 진입 시)")
                    sendHeaderTrackingData(mstId, detailId)
                    headerSent = true
                }

                // 12.2 초기 데이터 전송
                if (!initialDataSent) {
                    logger.info("📡 12.2 초기 데이터 전송")
                    sendInitialTrackingData(mstId, detailId)
                    initialDataSent = true
                    logger.info("✅ 12.2 초기 데이터 전송 완료")
                }
            }

            // 종료 시간 경과 → COMPLETED
            calTime.isAfter(endTime) -> {
                currentTrackingState = TrackingState.COMPLETED
                trackingStatus.ephemerisTrackingState = "COMPLETED"
                logger.info("✅ 추적 완료 (WAITING에서 종료 시간 경과)")
                handleCompleted()
            }

            // 대기 중 - 로그 출력 (5초마다)
            else -> {
                val now = System.currentTimeMillis()
                val shouldLog = now - lastWaitingLogTime >= WAITING_LOG_INTERVAL_MS ||
                        timeDifference != lastWaitingTimeDifference ||
                        abs(currentAz - lastWaitingAzimuth) > 0.1 ||
                        abs(currentEl - lastWaitingElevation) > 0.1

                if (shouldLog) {
                    logger.info("⏳ 추적 대기: 시작까지 ${-timeDifference}초 (Az=${currentAz}°, El=${currentEl}°)")
                    lastWaitingLogTime = now
                    lastWaitingTimeDifference = timeDifference
                    lastWaitingAzimuth = currentAz
                    lastWaitingElevation = currentEl
                }
            }
        }
    }

    /**
     * TRACKING 상태 처리 (실시간 추적)
     */
    private fun handleTrackingState() {
        // ✅ TRACKING 상태 업데이트 (프론트엔드 표시 및 펌웨어 제어용)
        trackingStatus.ephemerisTrackingState = "TRACKING"
        dataStoreService.updateTrackingStatus(trackingStatus)

        // 정지궤도 처리
        if (trackingStatus.geostationaryStatus == true) {
            logger.debug("🔄 정지궤도 추적 활성 상태 유지")
            return
        }

        val mstId = currentTrackingPassId ?: run {
            logger.warn("현재 추적 중인 MstId가 설정되지 않았습니다.")
            return
        }
        val detailId = (currentTrackingPass?.get("DetailId") as? Number)?.toInt() ?: 0
        val (startTime, endTime) = getCurrentTrackingPassTimes()
        val calTime = GlobalData.Time.calUtcTimeOffsetTime

        // 종료 시간 체크
        if (calTime.isAfter(endTime)) {
            currentTrackingState = TrackingState.COMPLETED
            trackingStatus.ephemerisTrackingState = "COMPLETED"
            dataStoreService.updateTrackingStatus(trackingStatus)
            logger.info("✅ 추적 완료 처리")
            handleCompleted()
            return
        }

        // 실시간 추적 데이터 저장
        saveRealtimeTrackingData(mstId, detailId, calTime, startTime)
    }

    // ✅ 이전 TRACKING_ACTIVE 블록 호환을 위한 헬퍼 (제거 예정)
    @Deprecated("상태 기반으로 대체됨", ReplaceWith("currentTrackingState == TrackingState.TRACKING"))
    private fun isInProgress(): Boolean = currentTrackingState == TrackingState.TRACKING

    /**
     * [레거시 호환] 이전 상태 처리 - 삭제 예정
     * 아래는 기존 TRACKING_ACTIVE 블록의 복잡한 로직을 참고용으로 남겨둠
     */
    private fun legacyTrackingActiveHandler() {
        // 이 함수는 사용되지 않음 - 참고용으로만 보존
        /*
        // 정지궤도와 저궤도 구분 처리
        if (trackingStatus.geostationaryStatus == true) {
            logger.debug("🔄 정지궤도 추적 활성 상태 유지")
        } else {
            // 저궤도: 시간 기반 스케줄 추적
            val mstId = currentTrackingPassId ?: return
            val detailId = (currentTrackingPass?.get("DetailId") as? Number)?.toInt() ?: 0
            val (startTime, endTime) = getCurrentTrackingPassTimes()
            val calTime = GlobalData.Time.calUtcTimeOffsetTime
            val timeDifference = Duration.between(startTime, calTime).seconds

            when {
                timeDifference <= 0 -> { /* WAITING 상태와 동일 */ }
                timeDifference > 0 && calTime.isBefore(endTime) -> { /* TRACKING 상태와 동일 */ }
                calTime.isAfter(endTime) -> { handleCompleted() }
            }
        }
        */
    }


    /**
     * 추적 시작 전 처리
     * ✅ mstId와 detailId를 사용하여 시작 전 처리
     */
    private fun handleBeforeStart(mstId: Long, detailId: Int = 0) {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        logger.info("📍 시작 전 상태 - 시작 위치로 이동")
        moveToStartPosition(mstId, detailId)

    }

    /**                                                                        R
     * 추적 진행 중 처리
     * ✅ mstId와 detailId를 사용하여 진행 중 처리
     * 
     * @deprecated 이 함수는 더 이상 사용되지 않습니다. 
     * 헤더는 MOVING_TO_TARGET → TRACKING_ACTIVE 전환 시 전송되고,
     * 초기 데이터는 TRACKING_ACTIVE 상태에서 timeDifference > 0일 때 전송됩니다.
     */
    private fun handleInProgress(mstId: Long, detailId: Int = 0) {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        logger.info("📡 진행 중 상태 - 추적 데이터 전송 시작")
        trackingStatus.ephemerisTrackingState = "TRACKING"  // ✅ 추가
        dataStoreService.setEphemerisTracking(true)
        // ✅ 헤더는 이미 전송되었으므로 초기 데이터만 전송
        sendInitialTrackingData(mstId, detailId)
    }

    /**
     * 추적 완료 처리
     */
    private fun handleCompleted() {
        logger.info("✅ 완료 상태 - 추적 종료")
        //trackingStatus.ephemerisStatus = false // Internal state update
        trackingStatus.ephemerisTrackingState = "COMPLETED"  // ✅ 추가
        //dataStoreService.setEphemerisTracking(false) // Frontend state update
    }

    /**
     * 공통 추적 상태 초기화
     */
    private fun resetTrackingState() {
        currentTrackingState = TrackingState.IDLE
        stabilizationStartTime = 0
        targetAzimuth = 0f
        targetElevation = 0f
        trainMoveCommandTime = 0  // ✅ 명령 전송 시간 초기화
        azElMoveCommandTime = 0  // ✅ 명령 전송 시간 초기화
        logger.info("🔄 공통 추적 상태 초기화 완료")
    }

    /**
     * ✅ 배치 처리를 사용한 실시간 추적 데이터 저장
     * ✅ mstId와 detailId를 사용하여 실시간 데이터 저장
     * ✅ CMD 값 업데이트 추가
     */
    private fun saveRealtimeTrackingData(mstId: Long, detailId: Int, currentTime: ZonedDateTime, startTime: ZonedDateTime) {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        try {
            // logger.info("🔍 [CMD 업데이트] saveRealtimeTrackingData 호출: mstId=$mstId, detailId=$detailId, currentTime=$currentTime")

            // ✅ 실시간 추적 데이터 생성
            val realtimeData = createRealtimeTrackingData(mstId, detailId, currentTime, startTime)

            // logger.info("🔍 [CMD 업데이트] createRealtimeTrackingData 결과: isEmpty=${realtimeData.isEmpty()}, keys=${realtimeData.keys.take(10)}")

            // ✅ CMD 값 업데이트 (DashboardPage에서 표시하기 위해)
            val cmdAz = (realtimeData["cmdAz"] as? Number)?.toFloat()
            val cmdEl = (realtimeData["cmdEl"] as? Number)?.toFloat()
            val cmdTrain = (realtimeData["trackingCMDTrainAngle"] as? Number)?.toFloat()
            
            // ✅ trackingCMD 값도 추출 (DataStoreService 업데이트용)
            val trackingCmdAz = (realtimeData["trackingCMDAzimuthAngle"] as? Number)?.toFloat()
            val trackingCmdEl = (realtimeData["trackingCMDElevationAngle"] as? Number)?.toFloat()
            val trackingActualAz = (realtimeData["trackingActualAzimuthAngle"] as? Number)?.toFloat()
            val trackingActualEl = (realtimeData["trackingActualElevationAngle"] as? Number)?.toFloat()
            val trackingActualTrain = (realtimeData["trackingActualTrainAngle"] as? Number)?.toFloat()
            
            // logger.info("🔍 [CMD 업데이트] 추출된 값: cmdAz=$cmdAz, cmdEl=$cmdEl, cmdTrain=$cmdTrain")

            // ✅ PushData.CMD에 설정 (WebSocket 전송용 - PushDataService에서 직접 읽음)
            if (cmdAz != null) {
                PushData.CMD.cmdAzimuthAngle = cmdAz
            }

            if (cmdEl != null) {
                PushData.CMD.cmdElevationAngle = cmdEl
            }

            // ✅ Train CMD는 덮어쓰지 않음 - moveTrainToZero()에서 설정한 값 유지
            // offset 변경 시에만 UdpFwICDService.positionOffsetCommand()에서 업데이트됨
            // if (cmdTrain != null) {
            //     PushData.CMD.cmdTrainAngle = cmdTrain
            // }
            
            // ✅ DataStoreService에도 trackingCMD 값 업데이트 (프론트엔드 동기화용)
            val currentData = dataStoreService.getLatestData()
            val updatedData = PushData.ReadData(
                // 기존 데이터 유지
                modeStatusBits = currentData.modeStatusBits,
                azimuthAngle = currentData.azimuthAngle,
                elevationAngle = currentData.elevationAngle,
                trainAngle = currentData.trainAngle,
                azimuthSpeed = currentData.azimuthSpeed,
                elevationSpeed = currentData.elevationSpeed,
                trainSpeed = currentData.trainSpeed,
                servoDriverAzimuthAngle = currentData.servoDriverAzimuthAngle,
                servoDriverElevationAngle = currentData.servoDriverElevationAngle,
                servoDriverTrainAngle = currentData.servoDriverTrainAngle,
                torqueAzimuth = currentData.torqueAzimuth,
                torqueElevation = currentData.torqueElevation,
                torqueTrain = currentData.torqueTrain,
                windSpeed = currentData.windSpeed,
                windDirection = currentData.windDirection,
                rtdOne = currentData.rtdOne,
                rtdTwo = currentData.rtdTwo,
                mainBoardProtocolStatusBits = currentData.mainBoardProtocolStatusBits,
                mainBoardStatusBits = currentData.mainBoardStatusBits,
                mainBoardMCOnOffBits = currentData.mainBoardMCOnOffBits,
                mainBoardReserveBits = currentData.mainBoardReserveBits,
                azimuthBoardServoStatusBits = currentData.azimuthBoardServoStatusBits,
                azimuthBoardStatusBits = currentData.azimuthBoardStatusBits,
                elevationBoardServoStatusBits = currentData.elevationBoardServoStatusBits,
                elevationBoardStatusBits = currentData.elevationBoardStatusBits,
                trainBoardServoStatusBits = currentData.trainBoardServoStatusBits,
                trainBoardStatusBits = currentData.trainBoardStatusBits,
                feedBoardETCStatusBits = currentData.feedBoardETCStatusBits,
                feedSBoardStatusBits = currentData.feedSBoardStatusBits,
                feedXBoardStatusBits = currentData.feedXBoardStatusBits,
                feedKaBoardStatusBits = currentData.feedKaBoardStatusBits,
                currentSBandLNALHCP = currentData.currentSBandLNALHCP,
                currentSBandLNARHCP = currentData.currentSBandLNARHCP,
                currentXBandLNALHCP = currentData.currentXBandLNALHCP,
                currentXBandLNARHCP = currentData.currentXBandLNARHCP,
                rssiSBandLNALHCP = currentData.rssiSBandLNALHCP,
                rssiSBandLNARHCP = currentData.rssiSBandLNARHCP,
                rssiXBandLNALHCP = currentData.rssiXBandLNALHCP,
                rssiXBandLNARHCP = currentData.rssiXBandLNARHCP,
                azimuthAcceleration = currentData.azimuthAcceleration,
                elevationAcceleration = currentData.elevationAcceleration,
                trainAcceleration = currentData.trainAcceleration,
                azimuthMaxAcceleration = currentData.azimuthMaxAcceleration,
                elevationMaxAcceleration = currentData.elevationMaxAcceleration,
                trainMaxAcceleration = currentData.trainMaxAcceleration,
                trackingAzimuthTime = currentData.trackingAzimuthTime,
                // ✅ trackingCMD 값 업데이트 (프론트엔드 동기화)
                trackingCMDAzimuthAngle = trackingCmdAz ?: cmdAz,
                // ✅ Actual 값은 UDP에서 받은 값 유지 (덮어쓰지 않음)
                trackingActualAzimuthAngle = currentData.trackingActualAzimuthAngle,  // ✅ UDP 값 유지
                trackingElevationTime = currentData.trackingElevationTime,
                trackingCMDElevationAngle = trackingCmdEl ?: cmdEl,
                trackingActualElevationAngle = currentData.trackingActualElevationAngle,  // ✅ UDP 값 유지
                trackingTrainTime = currentData.trackingTrainTime,
                trackingCMDTrainAngle = cmdTrain,
                trackingActualTrainAngle = currentData.trackingActualTrainAngle  // ✅ UDP 값 유지
            )
            dataStoreService.updateDataFromUdp(updatedData)

            // ✅ 배치 처리로 변경
            batchStorageManager.addToBatch(realtimeData)

            // ✅ 주기적 로깅 (배치 상태 포함) - 100번마다가 아니라 1000번마다로 변경하여 로그 감소
            if (trackingDataIndex % 1000 == 0) {
                val batchStatus = batchStorageManager.getBatchStatus()
                logger.debug(
                    "📊 배치 처리 중 - 총 {}개 데이터 포인트, 버퍼 크기: {}",
                    trackingDataIndex, batchStatus["bufferSize"]
                )
            }

            trackingDataIndex++

        } catch (e: Exception) {
            logger.error("배치 실시간 추적 데이터 저장 중 오류: ${e.message}", e)
        }
    }

    /**
     * ✅ 실시간 추적 데이터 생성 (개선된 버전 - Keyhole 대응 + 필터링 + keyhole_final_transformed 추가)
     * 
     * Keyhole 여부에 따라 적절한 DataType 사용:
     * - Keyhole 발생: keyhole_final_transformed (Train≠0)
     * - Keyhole 미발생: final_transformed (Train=0)
     * 
     * displayMinElevationAngle 기준으로 필터링:
     * - 실제 추적 명령은 displayMinElevationAngle 이상만 사용
     * 
     * ✅ 예외 처리:
     * - final_transformed MST 없음: 빈 Map 반환
     * - 필터링 후 데이터 없음: 빈 Map 반환
     * - Keyhole 발생 시 keyhole_final_transformed 데이터 없음: null 반환
     * 
     * ✅ mstId와 detailId를 사용하여 실시간 데이터 생성
     * 
     * @param mstId 마스터 ID
     * @param detailId 패스 인덱스 (기본값: 0)
     * @param currentTime 현재 시간
     * @param startTime 추적 시작 시간
     * @return 실시간 추적 데이터 Map
     */
    private fun createRealtimeTrackingData(
        mstId: Long,  // ✅ UInt → Long 변경 (PassSchedule과 동일)
        detailId: Int = 0,  // ✅ UInt → Int 변경 (PassSchedule과 동일)
        currentTime: ZonedDateTime,
        startTime: ZonedDateTime
    ): Map<String, Any?> {
        val elapsedTimeSeconds = Duration.between(startTime, currentTime).toMillis() / 1000.0f

        // logger.info("🔍 [createRealtimeTrackingData] 시작: mstId=$mstId, detailId=$detailId, currentTime=$currentTime, startTime=$startTime, elapsedTimeSeconds=$elapsedTimeSeconds")

        // ✅ original과 axis_transformed 데이터는 별도로 조회해야 함
        // getEphemerisTrackDtlByMstIdAndDetailId는 final_transformed만 반환하므로
        val originalPassDetails = getEphemerisTrackDtlByMstIdAndDataType(mstId, "original", detailId)
        val axisTransformedPassDetails = getEphemerisTrackDtlByMstIdAndDataType(mstId, "axis_transformed", detailId)
        
        // ✅ final_transformed 데이터는 getEphemerisTrackDtlByMstIdAndDetailId 사용 (하드웨어 제한 각도 필터링 포함)
        val allPassDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, detailId)
        
        // logger.info("🔍 [createRealtimeTrackingData] originalPassDetails 크기: ${originalPassDetails.size}, axisTransformedPassDetails 크기: ${axisTransformedPassDetails.size}, allPassDetails 크기: ${allPassDetails.size}")

        // ✅ original 데이터가 없으면 에러 (모든 변환을 거쳐야 하므로 original은 반드시 있어야 함)
        if (originalPassDetails.isEmpty()) {
            logger.error("❌ [createRealtimeTrackingData] 원본 이론치 데이터가 없습니다. mstId=$mstId, detailId=$detailId - 데이터 저장 과정에 문제가 있을 수 있습니다.")
            return emptyMap()
        }
        
        // ✅ final_transformed 데이터도 없으면 에러
        if (allPassDetails.isEmpty()) {
            logger.error("❌ [createRealtimeTrackingData] 최종 변환 데이터가 없습니다. mstId=$mstId, detailId=$detailId - 데이터 저장 과정에 문제가 있을 수 있습니다.")
            return emptyMap()
        }
        
        // ✅ Keyhole 여부 확인 (final_transformed MST에서)
        // ✅ MstId 필드만 사용 (No 필드 제거)
        // logger.info("🔍 [createRealtimeTrackingData] MST 저장소 크기: ${ephemerisTrackMstStorage.size}")
        val finalMst = ephemerisTrackMstStorage.find { 
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            dataMstId == mstId && it["DataType"] == "final_transformed" 
        }
        
        if (finalMst == null) {
            logger.warn("⚠️ [createRealtimeTrackingData] MstId(${mstId})에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다. 저장소 크기: ${ephemerisTrackMstStorage.size}")
            val availableMstIds =    ephemerisTrackMstStorage.mapNotNull { (it["MstId"] as? Number)?.toLong() }.distinct()
            logger.warn("⚠️ [createRealtimeTrackingData] 사용 가능한 MstId 목록: $availableMstIds")
            return emptyMap()
        }
        
        // logger.info("🔍 [createRealtimeTrackingData] finalMst 찾음: MstId=$mstId, IsKeyhole=${finalMst["IsKeyhole"]}")
        
        val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
        
        // ✅ Keyhole 여부에 따라 DataType 선택
        val finalDataType = if (isKeyhole) {
            logger.debug("🔑 실시간 추적: MstId(${mstId}) Keyhole 발생 → keyhole_optimized_final_transformed 사용")
            "keyhole_optimized_final_transformed"  // Keyhole이면 최적화 데이터 사용
        } else {
            logger.debug("✅ 실시간 추적: MstId(${mstId}) Keyhole 미발생 → final_transformed 사용")
            "final_transformed"  // Keyhole 아니면 기본 데이터 사용
        }
        
        // ✅ allPassDetails는 이미 getEphemerisTrackDtlByMstIdAndDetailId에서 반환된 데이터로
        // Keyhole 여부에 따라 final_transformed 또는 keyhole_optimized_final_transformed만 포함됨
        // 그리고 하드웨어 제한 각도 기준으로 이미 필터링되어 있음
        val filteredFinalTransformed = allPassDetails
        
        // logger.info("🔍 [createRealtimeTrackingData] filteredFinalTransformed 크기: ${filteredFinalTransformed.size}")
        
        // 필터링된 데이터가 비어있으면 로깅
        if (filteredFinalTransformed.isEmpty()) {
            logger.warn("⚠️ [createRealtimeTrackingData] MstId(${mstId}), DetailId(${detailId}): 필터링 결과 데이터가 없습니다.")
            return emptyMap()
        }

        // 2. ✅ 시간 기반으로 정확한 이론치 인덱스 계산
        val timeDifferenceMs = Duration.between(startTime, currentTime).toMillis()
        
        // ✅ original 데이터가 있으면 original 기준으로 인덱스 계산, 없으면 final_transformed 기준으로 계산
        val theoreticalIndex = if (originalPassDetails.isNotEmpty()) {
            (timeDifferenceMs / 100.0).toInt().coerceIn(0, originalPassDetails.size - 1)
        } else if (allPassDetails.isNotEmpty()) {
            // original이 없으면 final_transformed를 기준으로 인덱스 계산
            (timeDifferenceMs / 100.0).toInt().coerceIn(0, allPassDetails.size - 1)
        } else {
            0
        }

        // 3. ✅ 해당 인덱스의 실제 이론치 데이터 가져오기 (보간 없이 직접 매칭)
        val theoreticalPoint = if (originalPassDetails.isNotEmpty() && theoreticalIndex < originalPassDetails.size) {
            originalPassDetails[theoreticalIndex]
        } else if (originalPassDetails.isNotEmpty()) {
            originalPassDetails.last()
        } else if (allPassDetails.isNotEmpty() && theoreticalIndex < allPassDetails.size) {
            // original이 없으면 final_transformed 사용 (fallback)
            allPassDetails[theoreticalIndex]
        } else if (allPassDetails.isNotEmpty()) {
            allPassDetails.last()
        } else {
            emptyMap<String, Any?>()
        }

        val theoreticalAxisPoint = if (axisTransformedPassDetails.isNotEmpty() && theoreticalIndex < axisTransformedPassDetails.size) {
            axisTransformedPassDetails[theoreticalIndex]
        } else if (axisTransformedPassDetails.isNotEmpty()) {
            axisTransformedPassDetails.last()
        } else {
            // axis_transformed가 없으면 theoreticalPoint 사용 (fallback)
            theoreticalPoint
        }

        // ✅ 필터링된 final_transformed 데이터에서 인덱스 찾기
        val theoreticalFinalPoint = if (filteredFinalTransformed.isNotEmpty()) {
            val targetTime = theoreticalPoint["Time"] as? ZonedDateTime
            if (targetTime != null) {
                filteredFinalTransformed.minByOrNull { point ->
                    val pointTime = point["Time"] as? ZonedDateTime
                    if (pointTime != null) {
                        abs(Duration.between(targetTime, pointTime).toMillis())
        } else {
                        Long.MAX_VALUE
                    }
                } ?: filteredFinalTransformed.first()
            } else {
                val filteredIndex = (theoreticalIndex * filteredFinalTransformed.size / originalPassDetails.size)
                    .coerceIn(0, filteredFinalTransformed.size - 1)
                filteredFinalTransformed[filteredIndex]
            }
        } else {
            emptyMap<String, Any?>()
        }

        // 4. ✅ 정확한 이론치 값 추출 (보간 없이 직접 매칭)
        val originalAzimuth = (theoreticalPoint["Azimuth"] as? Double)?.toFloat() ?: 0.0f
        val originalElevation = (theoreticalPoint["Elevation"] as? Double)?.toFloat() ?: 0.0f
        val originalRange = (theoreticalPoint["Range"] as? Double)?.toFloat() ?: 0.0f
        val originalAltitude = (theoreticalPoint["Altitude"] as? Double)?.toFloat() ?: 0.0f

        val axisTransformedAzimuth = (theoreticalAxisPoint["Azimuth"] as? Double)?.toFloat() ?: originalAzimuth
        val axisTransformedElevation = (theoreticalAxisPoint["Elevation"] as? Double)?.toFloat() ?: originalElevation
        val axisTransformedRange = (theoreticalAxisPoint["Range"] as? Double)?.toFloat() ?: originalRange
        val axisTransformedAltitude = (theoreticalAxisPoint["Altitude"] as? Double)?.toFloat() ?: originalAltitude

        // ✅ 필터링된 final_transformed 데이터에서 값 추출
        val finalTransformedAzimuth = (theoreticalFinalPoint["Azimuth"] as? Double)?.toFloat() ?: axisTransformedAzimuth
        val finalTransformedElevation =
            (theoreticalFinalPoint["Elevation"] as? Double)?.toFloat() ?: axisTransformedElevation
        val finalTransformedRange = (theoreticalFinalPoint["Range"] as? Double)?.toFloat() ?: axisTransformedRange
        val finalTransformedAltitude =
            (theoreticalFinalPoint["Altitude"] as? Double)?.toFloat() ?: axisTransformedAltitude

        // ✅ 필터링 제거: 3축 변환 후 Elevation 음수 허용
        // 필터링 기준 확인 (하드웨어 제한 각도)
        // val filterThreshold = settingsService.angleElevationMin
        // 
        // logger.info("🔍 [createRealtimeTrackingData] 필터링 체크: finalTransformedElevation=$finalTransformedElevation, filterThreshold=$filterThreshold")
        // 
        // if (finalTransformedElevation < filterThreshold) {
        //     logger.warn("⚠️ [createRealtimeTrackingData] 실시간 추적 데이터: Elevation(${finalTransformedElevation}°) < 필터 기준(${filterThreshold}°) - 빈 Map 반환")
        //     return emptyMap()
        // }
        
        // logger.info("🔍 [createRealtimeTrackingData] 필터링 제거: finalTransformedElevation=$finalTransformedElevation")

        // ✅ 성능 최적화: Keyhole Final 변환 데이터를 한 번만 조회하고 재사용
        // ✅ 수정: keyhole_final_transformed → keyhole_optimized_final_transformed (DataType 통일)
        val keyholeFinalPassDetails = if (isKeyhole) {
            allPassDetails.filter {
                it["DataType"] == "keyhole_optimized_final_transformed"
            }
        } else {
            emptyList()
        }
        
        // ✅ Keyhole Final 변환 데이터 추출 (Keyhole 발생 시만, 이미 조회한 데이터 재사용)
        val keyholeFinalPoint = if (isKeyhole && keyholeFinalPassDetails.isNotEmpty()) {
            if (theoreticalIndex < keyholeFinalPassDetails.size) {
                    keyholeFinalPassDetails[theoreticalIndex]
                } else {
                    keyholeFinalPassDetails.lastOrNull()
                }
            } else {
                null
            }
        
        val keyholeFinalTransformedAzimuth = if (isKeyhole && keyholeFinalPoint != null) {
            (keyholeFinalPoint.get("Azimuth") as? Double)?.toFloat()
        } else {
            if (isKeyhole && keyholeFinalPassDetails.isEmpty() && !keyholeWarningLogged) {
                logger.warn("⚠️ MstId(${mstId}), DetailId(${detailId}): Keyhole 발생 시 keyhole_optimized_final_transformed 데이터가 없습니다. final_transformed로 fallback합니다.")
                keyholeWarningLogged = true  // ✅ 한 번만 로그 출력
            }
            null
        }
        
        val keyholeFinalTransformedElevation = if (isKeyhole && keyholeFinalPoint != null) {
            (keyholeFinalPoint.get("Elevation") as? Double)?.toFloat()
        } else null
        
        val keyholeFinalTransformedRange = if (isKeyhole && keyholeFinalPoint != null) {
            (keyholeFinalPoint.get("Range") as? Double)?.toFloat()
        } else null
        
        val keyholeFinalTransformedAltitude = if (isKeyhole && keyholeFinalPoint != null) {
            (keyholeFinalPoint.get("Altitude") as? Double)?.toFloat()
        } else null

        // 변환 정보 추출
        val tiltAngle = settingsService.tiltAngle
        val transformationType = theoreticalAxisPoint["TransformationType"] as? String ?: "none"

        // ✅ 변경: PushData 대신 DataStoreService에서 데이터 가져오기
        val currentData = dataStoreService.getLatestData()

        // ✅ DataStoreService에서 추적 관련 데이터만 별도로 가져오기
        val trackingOnlyData = dataStoreService.getTrackingOnlyData()

        val trackingCmdAzimuthTime = trackingOnlyData["trackingAzimuthTime"]
        val trackingCmdElevationTime = trackingOnlyData["trackingElevationTime"]
        val trackingCmdTrainTime = trackingOnlyData["trackingTiltTime"]

        val trackingCmdAzimuth = trackingOnlyData["trackingCMDAzimuthAngle"]
        val trackingActualAzimuth = trackingOnlyData["trackingActualAzimuthAngle"]
        val trackingCmdElevation = trackingOnlyData["trackingCMDElevationAngle"]
        val trackingActualElevation = trackingOnlyData["trackingActualElevationAngle"]
        val trackingCmdTrain = trackingOnlyData["trackingCMDTrainAngle"]
        val trackingActualTrain = trackingOnlyData["trackingActualTrainAngle"]

        // ✅ 데이터 유효성 검사
        val hasValidData =
            trackingCmdAzimuth != null || trackingActualAzimuth != null || trackingCmdElevation != null || trackingActualElevation != null

        if (!hasValidData && trackingDataIndex % 50 == 0) {
            logger.warn("⚠️ DataStoreService에서 유효한 추적 데이터를 받지 못하고 있습니다.")
            debugDataStoreStatus()
        }

        // 실시간 추적 데이터 생성 (원본, 축변환, 최종 변환 데이터 모두 포함)
        return mapOf(
            "index" to trackingDataIndex,  // 실시간 데이터 인덱스
            "theoreticalIndex" to theoreticalIndex,  // ✅ 이론치 데이터 인덱스 추가
            "timestamp" to currentTime,

            // ✅ 원본 데이터 (변환 전)
            "originalAzimuth" to originalAzimuth,
            "originalElevation" to originalElevation,
            "originalRange" to originalRange,
            "originalAltitude" to originalAltitude,

            // ✅ 축변환 데이터 (기울기 변환 적용)
            "axisTransformedAzimuth" to axisTransformedAzimuth,
            "axisTransformedElevation" to axisTransformedElevation,
            "axisTransformedRange" to axisTransformedRange,
            "axisTransformedAltitude" to axisTransformedAltitude,

            // ✅ 최종 변환 데이터 (±270도 제한 적용, Train=0)
            "finalTransformedAzimuth" to finalTransformedAzimuth,
            "finalTransformedElevation" to finalTransformedElevation,
            "finalTransformedRange" to finalTransformedRange,
            "finalTransformedAltitude" to finalTransformedAltitude,

            // ✅ Keyhole Final 변환 데이터 (±270도 제한 적용, Train≠0) [Keyhole 발생 시만]
            "keyholeFinalTransformedAzimuth" to keyholeFinalTransformedAzimuth,
            "keyholeFinalTransformedElevation" to keyholeFinalTransformedElevation,
            "keyholeFinalTransformedRange" to keyholeFinalTransformedRange,
            "keyholeFinalTransformedAltitude" to keyholeFinalTransformedAltitude,

            // ✅ 실제 추적 명령 데이터 (Keyhole 여부에 따라 선택) + Offset 적용
            "cmdAz" to ((if (isKeyhole && keyholeFinalTransformedAzimuth != null) keyholeFinalTransformedAzimuth else finalTransformedAzimuth) + GlobalData.Offset.azimuthPositionOffset),
            "cmdEl" to ((if (isKeyhole && keyholeFinalTransformedElevation != null) keyholeFinalTransformedElevation else finalTransformedElevation) + GlobalData.Offset.elevationPositionOffset),
            "actualAz" to currentData.azimuthAngle,
            "actualEl" to currentData.elevationAngle,

            "elapsedTimeSeconds" to elapsedTimeSeconds,
            "trackingAzimuthTime" to trackingCmdAzimuthTime,
            // ✅ 하드웨어에서 받은 CMD/Actual 값 그대로 저장 (이론치 아님)
            "trackingCMDAzimuthAngle" to trackingCmdAzimuth,
            "trackingActualAzimuthAngle" to trackingActualAzimuth,
            "trackingElevationTime" to trackingCmdElevationTime,
            "trackingCMDElevationAngle" to trackingCmdElevation,
            "trackingActualElevationAngle" to trackingActualElevation,
            "trackingTrainTime" to trackingCmdTrainTime,
            "trackingCMDTrainAngle" to trackingCmdTrain,
            "trackingActualTrainAngle" to trackingActualTrain,
            "passId" to mstId, // 하위 호환성을 위해 유지
            "mstId" to mstId, // ✅ mstId 추가
            "detailId" to detailId, // ✅ detailId 추가

            // ✅ 변환 오차 계산
            "originalToAxisTransformationError" to (axisTransformedAzimuth - originalAzimuth),
            "axisToFinalTransformationError" to (finalTransformedAzimuth - axisTransformedAzimuth),
            "totalTransformationError" to (finalTransformedAzimuth - originalAzimuth),

            // ✅ 실제 추적 오차
            "azimuthError" to ((trackingCmdAzimuth ?: 0.0f) - (trackingActualAzimuth ?: 0.0f)),
            "elevationError" to ((trackingCmdElevation ?: 0.0f) - (trackingActualElevation ?: 0.0f)),

            // ✅ 정확도 분석 (새로 추가된 필드들)
            "timeAccuracy" to (elapsedTimeSeconds - (trackingCmdAzimuthTime as? Float ?: 0.0f)),
            "azCmdAccuracy" to (finalTransformedAzimuth - (trackingCmdAzimuth as? Float ?: 0.0f)),
            "azActAccuracy" to ((trackingCmdAzimuth as? Float ?: 0.0f) - (trackingActualAzimuth as? Float ?: 0.0f)),
            "azFinalAccuracy" to (finalTransformedAzimuth - (trackingActualAzimuth as? Float ?: 0.0f)),
            "elCmdAccuracy" to (finalTransformedElevation - (trackingCmdElevation as? Float ?: 0.0f)),
            "elActAccuracy" to ((trackingCmdElevation as? Float ?: 0.0f) - (trackingActualElevation as? Float ?: 0.0f)),
            "elFinalAccuracy" to (finalTransformedElevation - (trackingActualElevation as? Float ?: 0.0f)),

            "hasValidData" to hasValidData,
            "dataSource" to "DataStoreService", // ✅ 데이터 소스 표시

            // ✅ 변환 정보
            "tiltAngle" to tiltAngle,
            "transformationType" to transformationType,
            "isKeyhole" to isKeyhole,
            "finalDataType" to finalDataType,

            // ✅ 변환 적용 여부
            "hasTransformation" to (transformationType != "none"),

            // ✅ 보간 정보 (직접 매칭이므로 정확도 1.0)
            "interpolationMethod" to "direct_matching",
            "interpolationAccuracy" to 1.0
        )
    }

    /**
     * ✅ 선형 보간법으로 정확한 위치 계산
     */
    private fun calculateInterpolatedPosition(
        passDetails: List<Map<String, Any?>>,
        currentTime: ZonedDateTime,
        startTime: ZonedDateTime
    ): Map<String, Any?> {
        try {
            // 현재 시간에 해당하는 목표 위치 찾기
            val timeDifferenceMs = Duration.between(startTime, currentTime).toMillis()
            val calculatedIndex = timeDifferenceMs / 100.0  // 소수점 인덱스 사용

            // 인덱스 범위 확인
            if (calculatedIndex < 0 || calculatedIndex >= passDetails.size - 1) {
                // 범위를 벗어난 경우 가장 가까운 데이터 반환
                val safeIndex = when {
                    calculatedIndex < 0 -> 0
                    else -> passDetails.size - 1
                }
                val targetPoint = passDetails[safeIndex]
                return extractAllTransformationData(targetPoint)
            }

            // 선형 보간법 적용
            val lowerIndex = calculatedIndex.toInt()
            val upperIndex = lowerIndex + 1
            val fraction = calculatedIndex - lowerIndex  // 0.0 ~ 1.0

            val lowerPoint = passDetails[lowerIndex]
            val upperPoint = passDetails[upperIndex]

            // 원본 데이터 보간
            val originalAzimuth = interpolateValue(
                lowerPoint["OriginalAzimuth"] as? Double ?: lowerPoint["Azimuth"] as Double,
                upperPoint["OriginalAzimuth"] as? Double ?: upperPoint["Azimuth"] as Double,
                fraction
            )
            val originalElevation = interpolateValue(
                lowerPoint["OriginalElevation"] as? Double ?: lowerPoint["Elevation"] as Double,
                upperPoint["OriginalElevation"] as? Double ?: upperPoint["Elevation"] as Double,
                fraction
            )
            val originalRange = interpolateValue(
                lowerPoint["Range"] as? Double ?: 0.0,
                upperPoint["Range"] as? Double ?: 0.0,
                fraction
            )
            val originalAltitude = interpolateValue(
                lowerPoint["Altitude"] as? Double ?: 0.0,
                upperPoint["Altitude"] as? Double ?: 0.0,
                fraction
            )

            // 축변환 데이터 보간
            val axisTransformedAzimuth = interpolateValue(
                lowerPoint["Azimuth"] as Double,
                upperPoint["Azimuth"] as Double,
                fraction
            )
            val axisTransformedElevation = interpolateValue(
                lowerPoint["Elevation"] as Double,
                upperPoint["Elevation"] as Double,
                fraction
            )
            val axisTransformedRange = interpolateValue(
                lowerPoint["Range"] as? Double ?: 0.0,
                upperPoint["Range"] as? Double ?: 0.0,
                fraction
            )
            val axisTransformedAltitude = interpolateValue(
                lowerPoint["Altitude"] as? Double ?: 0.0,
                upperPoint["Altitude"] as? Double ?: 0.0,
                fraction
            )

            // 최종 변환 데이터 (축변환과 동일하지만 ±270도 제한이 적용됨)
            val finalTransformedAzimuth = axisTransformedAzimuth
            val finalTransformedElevation = axisTransformedElevation
            val finalTransformedRange = axisTransformedRange
            val finalTransformedAltitude = axisTransformedAltitude

            // 변환 정보
            val tiltAngle = settingsService.tiltAngle
            val transformationType = lowerPoint["TransformationType"] as? String ?: "none"

            // 보간 정확도 계산
            val interpolationAccuracy = 1.0 - fraction  // 1.0에 가까울수록 정확

            return mapOf(
                "originalAzimuth" to originalAzimuth.toFloat(),
                "originalElevation" to originalElevation.toFloat(),
                "originalRange" to originalRange.toFloat(),
                "originalAltitude" to originalAltitude.toFloat(),

                "axisTransformedAzimuth" to axisTransformedAzimuth.toFloat(),
                "axisTransformedElevation" to axisTransformedElevation.toFloat(),
                "axisTransformedRange" to axisTransformedRange.toFloat(),
                "axisTransformedAltitude" to axisTransformedAltitude.toFloat(),

                "finalTransformedAzimuth" to finalTransformedAzimuth.toFloat(),
                "finalTransformedElevation" to finalTransformedElevation.toFloat(),
                "finalTransformedRange" to finalTransformedRange.toFloat(),
                "finalTransformedAltitude" to finalTransformedAltitude.toFloat(),

                "tiltAngle" to tiltAngle,
                "transformationType" to transformationType,
                "interpolationAccuracy" to interpolationAccuracy
            )

        } catch (e: Exception) {
            logger.error("보간 위치 계산 중 오류: ${e.message}", e)
            return emptyMap()
        }
    }

    /**
     * ✅ 선형 보간법 헬퍼 함수
     */
    private fun interpolateValue(lower: Double, upper: Double, fraction: Double): Double {
        return lower + (upper - lower) * fraction
    }

    /**
     * ✅ 모든 변환 데이터 추출 헬퍼 함수
     */
    private fun extractAllTransformationData(targetPoint: Map<String, Any?>): Map<String, Any?> {
        val originalAzimuth =
            (targetPoint["OriginalAzimuth"] as? Double)?.toFloat() ?: (targetPoint["Azimuth"] as Double).toFloat()
        val originalElevation =
            (targetPoint["OriginalElevation"] as? Double)?.toFloat() ?: (targetPoint["Elevation"] as Double).toFloat()
        val originalRange = (targetPoint["Range"] as? Double)?.toFloat() ?: 0.0f
        val originalAltitude = (targetPoint["Altitude"] as? Double)?.toFloat() ?: 0.0f

        val axisTransformedAzimuth = (targetPoint["Azimuth"] as Double).toFloat()
        val axisTransformedElevation = (targetPoint["Elevation"] as Double).toFloat()
        val axisTransformedRange = (targetPoint["Range"] as? Double)?.toFloat() ?: 0.0f
        val axisTransformedAltitude = (targetPoint["Altitude"] as? Double)?.toFloat() ?: 0.0f

        val finalTransformedAzimuth = axisTransformedAzimuth
        val finalTransformedElevation = axisTransformedElevation
        val finalTransformedRange = axisTransformedRange
        val finalTransformedAltitude = axisTransformedAltitude

        val tiltAngle = settingsService.tiltAngle
        val transformationType = targetPoint["TransformationType"] as? String ?: "none"

        return mapOf(
            "originalAzimuth" to originalAzimuth,
            "originalElevation" to originalElevation,
            "originalRange" to originalRange,
            "originalAltitude" to originalAltitude,

            "axisTransformedAzimuth" to axisTransformedAzimuth,
            "axisTransformedElevation" to axisTransformedElevation,
            "axisTransformedRange" to axisTransformedRange,
            "axisTransformedAltitude" to axisTransformedAltitude,

            "finalTransformedAzimuth" to finalTransformedAzimuth,
            "finalTransformedElevation" to finalTransformedElevation,
            "finalTransformedRange" to finalTransformedRange,
            "finalTransformedAltitude" to finalTransformedAltitude,

            "tiltAngle" to tiltAngle,
            "transformationType" to transformationType,
            "interpolationAccuracy" to 1.0  // 정확한 데이터 포인트
        )
    }

    fun debugPushDataStatus() {
        val readData = PushData.READ_DATA
        logger.info("🔍 PushData 디버깅 정보:")
        logger.info("  - trackingAzimuthTime: {}", readData.trackingAzimuthTime)
        logger.info("  - trackingCMDAzimuthAngle: {}", readData.trackingCMDAzimuthAngle)
        logger.info("  - trackingActualAzimuthAngle: {}", readData.trackingActualAzimuthAngle)
        logger.info("  - trackingElevationTime: {}", readData.trackingElevationTime)
        logger.info("  - trackingCMDElevationAngle: {}", readData.trackingCMDElevationAngle)
        logger.info("  - trackingActualElevationAngle: {}", readData.trackingActualElevationAngle)
        logger.info("  - trackingTiltTime: {}", readData.trackingTrainTime)
        logger.info("  - trackingCMDTiltAngle: {}", readData.trackingCMDTrainAngle)
        logger.info("  - trackingActualTiltAngle: {}", readData.trackingActualTrainAngle)
    }

    // ✅ 새로운 디버깅 메서드 추가
    fun debugDataStoreStatus() {
        try {
            val currentData = dataStoreService.getLatestData()
            val trackingData = dataStoreService.getTrackingOnlyData()
            val statusInfo = dataStoreService.getStatusInfo()

            logger.info("🔍 DataStoreService 디버깅 정보:")
            logger.info("  - 데이터 버전: {}", statusInfo["dataVersion"])
            logger.info("  - 마지막 업데이트: {}", statusInfo["lastUpdateTime"])
            logger.info("  - UDP 연결 상태: {}", statusInfo["isUdpConnected"])
            logger.info("  - 유효한 데이터 여부: {}", statusInfo["hasValidData"])
            logger.info("  - null이 아닌 필드 수: {}", statusInfo["nonNullFields"])

            logger.info("  추적 전용 데이터:")
            // ✅ forEach 오버로드 모호성 해결: 명시적 타입 지정
            trackingData.forEach { entry: Map.Entry<String, Any?> ->
                logger.info("    - {}: {}", entry.key, entry.value)
            }

            logger.info("  일반 각도 데이터:")
            logger.info("    - azimuthAngle: {}", currentData.azimuthAngle)
            logger.info("    - elevationAngle: {}", currentData.elevationAngle)
            logger.info("    - tiltAngle: {}", currentData.trainAngle)

        } catch (e: Exception) {
            logger.error("DataStore 디버깅 중 오류: {}", e.message, e)
        }
    }

    /**
     * ✅ 실시간 추적 데이터 조회 (배치 관리자 사용)
     */
    fun getRealtimeTrackingData(): List<Map<String, Any?>> {
        return batchStorageManager.getRealtimeTrackingData()
    }

    /**
     * ✅ 실시간 추적 데이터 초기화 (배치 관리자 사용)
     */
    fun clearRealtimeTrackingData() {
        batchStorageManager.clearRealtimeTrackingData()
        trackingDataIndex = 0
        logger.info("실시간 추적 데이터 초기화 완료")
    }

    /**
     * ✅ 실시간 추적 통계 정보 (배치 관리자 사용)
     */
    fun getRealtimeTrackingStats(): Map<String, Any> {
        return batchStorageManager.getRealtimeTrackingStats()
    }

    /**
     * ✅ 배치 처리 상태 조회
     */
    fun getBatchStatus(): Map<String, Any> {
        return batchStorageManager.getBatchStatus()
    }

    /**
     * ✅ 강제 배치 처리
     */
    fun forceProcessBatch() {
        batchStorageManager.forceProcessBatch()
    }

    /**
     * ✅ 배치 처리 성능 테스트
     */
    fun testBatchPerformance() {
        logger.info("🚀 배치 처리 성능 테스트 시작")

        val testDataCount = 1000
        val startTime = System.currentTimeMillis()

        // 테스트 데이터 생성 및 배치 처리
        repeat(testDataCount) { i ->
            val testData = mapOf(
                "index" to i,
                "timestamp" to ZonedDateTime.now(),
                "cmdAz" to (i * 0.1f),
                "cmdEl" to (i * 0.05f),
                "actualAz" to (i * 0.1f + 0.5f),
                "actualEl" to (i * 0.05f + 0.3f),
                "azimuthError" to 0.5f,
                "elevationError" to 0.3f,
                "hasValidData" to true,
                "passId" to 1u
            )

            batchStorageManager.addToBatch(testData)
        }

        // 강제 배치 처리
        batchStorageManager.forceProcessBatch()

        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime

        val batchStatus = batchStorageManager.getBatchStatus()
        val stats = batchStorageManager.getRealtimeTrackingStats()

        logger.info("📊 배치 처리 성능 테스트 결과:")
        logger.info("  - 처리된 데이터 수: ${stats["totalCount"]}")
        logger.info("  - 총 처리 시간: ${processingTime}ms")
        val speed = if (processingTime > 0) testDataCount * 1000.0 / processingTime else 0.0
        logger.info("  - 평균 처리 속도: $speed 데이터/초")
        logger.info("  - 배치 버퍼 크기: ${batchStatus["bufferSize"]}")
        logger.info("  - 평균 Az 오차: ${stats["averageAzimuthError"]}")
        logger.info("  - 평균 El 오차: ${stats["averageElevationError"]}")
    }

    /**
     * 시작 위치로 이동 (공통)
     * ✅ mstId와 detailId를 사용하여 시작 위치 조회
     * ✅ Keyhole 여부에 따라 적절한 MST의 StartAzimuth, StartElevation 사용
     */
    private fun moveToStartPosition(mstId: Long, detailId: Int = 0) {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        // ✅ 현재 targetAzimuth, targetElevation 초기값 로깅
        val initialTargetAz = targetAzimuth
        val initialTargetEl = targetElevation
        // logger.info("📍 [moveToStartPosition] 함수 시작: mstId=${mstId}, detailId=${detailId}")
        // logger.info("📍 [moveToStartPosition] 현재 targetAzimuth=${initialTargetAz}°, targetElevation=${initialTargetEl}°")
        
        // ✅ currentTrackingPass에서 DetailId를 가져오기 (파라미터보다 우선)
        val actualDetailId = if (currentTrackingPass != null) {
            (currentTrackingPass?.get("DetailId") as? Number)?.toInt() ?: detailId
        } else {
            // ✅ currentTrackingPass가 없으면 MST에서 DetailId 가져오기
            val mst = ephemerisTrackMstStorage.find { 
                val dataMstId = (it["MstId"] as? Number)?.toLong()
                dataMstId == mstId && it["DataType"] == "final_transformed"
            }
            (mst?.get("DetailId") as? Number)?.toInt() ?: detailId
        }
        // logger.info("📍 [moveToStartPosition] 시작 위치 이동: mstId=${mstId}, detailId=${actualDetailId} (파라미터=${detailId})")

        // ✅ Keyhole 여부에 따라 적절한 MST 선택 (getTrackingPassMst 사용)
        val selectedPass = getTrackingPassMst(mstId)

        if (selectedPass != null) {
            // logger.info("📍 [moveToStartPosition] MST 데이터 찾음: mstId=${mstId}")

            // ✅ MST의 StartAzimuth, StartElevation 사용 (Keyhole 여부에 따라 올바른 MST 선택됨)
            val startAzimuth = selectedPass["StartAzimuth"] as? Double
            val startElevation = selectedPass["StartElevation"] as? Double

            // logger.info("📍 [moveToStartPosition] MST에서 추출한 값: startAzimuth=${startAzimuth}, startElevation=${startElevation}")
            
            if (startAzimuth != null && startElevation != null) {
                targetAzimuth = startAzimuth.toFloat()
                targetElevation = startElevation.toFloat()
                // ✅ 상태 단순화: PREPARING 상태로 진입, 세부 단계는 PreparingPhase로 관리
                currentTrackingState = TrackingState.PREPARING
                currentPreparingPhase = PreparingPhase.TRAIN_MOVING
                
                val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
                val dataType = selectedPass["DataType"] as? String
                
                // logger.info("✅ [moveToStartPosition] 시작 위치 설정 완료:")
                // logger.info("  - 이전 값: targetAzimuth=${initialTargetAz}°, targetElevation=${initialTargetEl}°")
                // logger.info("  - 새 값: targetAzimuth=${targetAzimuth}°, targetElevation=${targetElevation}°")
                // logger.info("  - 출처: MST StartAzimuth/StartElevation")
                // logger.info("  - Keyhole=${if (isKeyhole) "YES" else "NO"}, DataType=${dataType}")
            } else {
                logger.warn("⚠️ [moveToStartPosition] MST에서 StartAzimuth 또는 StartElevation이 null입니다. DTL fallback 시도")
                
                // ✅ Fallback: DTL의 첫 번째 포인트 사용
                val passDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, actualDetailId)
                
                // logger.info("📍 [moveToStartPosition] DTL 조회 결과: passDetails.size=${passDetails.size}, mstId=${mstId}, detailId=${actualDetailId}")
                
                if (passDetails.isNotEmpty()) {
                    val startPoint = passDetails.first()
                    val dtlAzimuth = startPoint["Azimuth"] as? Double
                    val dtlElevation = startPoint["Elevation"] as? Double
                    
                    // logger.info("📍 [moveToStartPosition] DTL 첫 번째 포인트 값: Azimuth=${dtlAzimuth}, Elevation=${dtlElevation}")
                    
                    if (dtlAzimuth != null && dtlElevation != null) {
                        targetAzimuth = dtlAzimuth.toFloat()
                        targetElevation = dtlElevation.toFloat()
                        // ✅ 상태 단순화: PREPARING 상태로 진입
                        currentTrackingState = TrackingState.PREPARING
                        currentPreparingPhase = PreparingPhase.TRAIN_MOVING
                        
                        // logger.info("✅ [moveToStartPosition] 시작 위치 설정 완료 (DTL fallback):")
                        // logger.info("  - 이전 값: targetAzimuth=${initialTargetAz}°, targetElevation=${initialTargetEl}°")
                        // logger.info("  - 새 값: targetAzimuth=${targetAzimuth}°, targetElevation=${targetElevation}°")
                        // logger.info("  - 출처: DTL 첫 번째 포인트")
                    } else {
                        logger.error("❌ [moveToStartPosition] DTL 첫 번째 포인트에서 Azimuth 또는 Elevation이 null입니다!")
                        logger.error("  - DTL 포인트 키: ${startPoint.keys}")
                        logger.error("  - targetAzimuth, targetElevation은 ${targetAzimuth}°, ${targetElevation}°로 유지됨")
                    }
                } else {
                    logger.error("❌ [moveToStartPosition] 시작 위치 데이터를 찾을 수 없습니다:")
                    logger.error("  - mstId=${mstId}, detailId=${actualDetailId}")
                    logger.error("  - 파라미터 detailId: ${detailId}")
                    logger.error("  - currentTrackingPass DetailId: ${currentTrackingPass?.get("DetailId")}")
                    logger.error("  - 사용된 actualDetailId: ${actualDetailId}")
                    logger.error("  - ephemerisTrackMstStorage 크기: ${ephemerisTrackMstStorage.size}")
                    logger.error("  - ephemerisTrackDtlStorage 크기: ${ephemerisTrackDtlStorage.size}")
                    logger.error("  - targetAzimuth, targetElevation은 ${targetAzimuth}°, ${targetElevation}°로 유지됨 (0.0이면 문제!)")
                }
            }
        } else {
            logger.error("❌ [moveToStartPosition] MST 데이터를 찾을 수 없습니다:")
            logger.error("  - mstId=${mstId}")
            logger.error("  - ephemerisTrackMstStorage 크기: ${ephemerisTrackMstStorage.size}")
            logger.error("  - 저장소의 MstId 목록: ${ephemerisTrackMstStorage.mapNotNull { (it["MstId"] as? Number)?.toLong() }.distinct()}")
            logger.error("  - targetAzimuth, targetElevation은 ${targetAzimuth}°, ${targetElevation}°로 유지됨 (0.0이면 문제!)")
        }
        
        // ✅ 최종 설정된 값 로깅
        // logger.info("📍 [moveToStartPosition] 최종 설정된 값: targetAzimuth=${targetAzimuth}°, targetElevation=${targetElevation}°")
    }

    /**
     * 위성 추적 시작 - 헤더 정보 전송
     * 
     * 2.12.1 위성 추적 해더 정보 송신 프로토콜 사용
     * Keyhole 여부에 따라 적절한 MST를 currentTrackingPass에 설정합니다.
     * 
     * ✅ mstId와 detailId를 사용하여 헤더 정보 전송
     * 
     * @param mstId 추적할 마스터 ID
     * @param detailId 패스 인덱스 (기본값: 0)
     * 
     * @see getTrackingPassMst Keyhole 여부에 따라 적절한 MST 선택
     */
    fun sendHeaderTrackingData(mstId: Long, detailId: Int = 0) {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        try {
            udpFwICDService.writeNTPCommand()
            currentTrackingPassId = mstId
            
            // ✅ Keyhole 여부에 따라 적절한 MST 선택
            // Keyhole 발생: keyhole_final_transformed MST
            // Keyhole 미발생: final_transformed MST
            val selectedPass = getTrackingPassMst(mstId)
            
            if (selectedPass == null) {
                logger.error("선택된 MstId($mstId)에 해당하는 데이터를 찾을 수 없습니다.")
                return
            }
            
            // ✅ selectedPass에서 DetailId를 가져오기 (파라미터보다 우선)
            val actualDetailId = (selectedPass["DetailId"] as? Number)?.toInt() ?: detailId
            logger.info("📡 헤더 전송: mstId=${mstId}, detailId=${actualDetailId} (파라미터=${detailId}, MST DetailId=${selectedPass["DetailId"]})")
            
            // 현재 추적 중인 패스 설정
            // ✅ DetailId를 명시적으로 저장 (PassSchedule과 동일)
            currentTrackingPass = selectedPass.toMutableMap().apply {
                put("DetailId", actualDetailId)  // ✅ MST에서 가져온 detailId 저장
            }
            
            // Keyhole 정보 로깅
            val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
            val recommendedTrainAngle = selectedPass["RecommendedTrainAngle"] as? Double ?: 0.0
            logger.info("📊 헤더 전송 패스 정보: Keyhole=${if (isKeyhole) "YES" else "NO"}, RecommendedTrainAngle=${recommendedTrainAngle}°")
            
            // 패스 시작 및 종료 시간 가져오기
            val startTime = (selectedPass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
            val endTime = (selectedPass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)

            // 시작 시간과 종료 시간을 문자열로 변환 (밀리초 포함)
            logger.info("위성 추적 시작: ${selectedPass["SatelliteName"]} (MstId: $mstId, DetailId: $actualDetailId)")
            logger.info("시작 시간: $startTime, 종료 시간: $endTime")

            // 밀리초 추출
            val startTimeMs = (startTime.nano / 1_000_000).toUShort()
            val endTimeMs = (endTime.nano / 1_000_000).toUShort()

            // 전체 데이터 길이 검증
            val totalLength = calculateDataLength(mstId, actualDetailId)
            val actualDataCount = getEphemerisTrackDtlByMstIdAndDetailId(mstId, actualDetailId).size
            logger.info("전체 데이터 길이: ${totalLength}개")
            logger.info("실제 데이터 개수: ${actualDataCount}개")

            // ✅ 필터링 후 데이터가 없으면 추적 시작 중단
            if (actualDataCount == 0) {
                logger.error("❌ MstId($mstId), DetailId($actualDetailId): 필터링 후 데이터가 없어 추적을 시작할 수 없습니다.")
                logger.error("   - 파라미터 detailId: ${detailId}")
                logger.error("   - MST DetailId: ${selectedPass["DetailId"]}")
                logger.error("   - 사용된 actualDetailId: ${actualDetailId}")
                dataStoreService.setEphemerisTracking(false)
                return
            }

            // ✅ 두 함수 모두 동일한 필터링 로직 사용하므로 항상 일치해야 함
            if (totalLength != actualDataCount) {
                logger.warn("⚠️ 데이터 길이 불일치: 계산된 길이=${totalLength}, 실제 길이=${actualDataCount}")
                logger.warn("   이는 예상치 못한 상황입니다. 두 함수가 동일한 필터링 로직을 사용하므로 항상 일치해야 합니다.")
            }

            // 2.12.1 위성 추적 헤더 정보 송신 프로토콜 생성
            val headerFrame = ICDService.SatelliteTrackOne.SetDataFrame(
                cmdOne = 'T',
                cmdTwo = 'T',
                dataLen = totalLength.toUShort(), // 검증된 전체 데이터 길이 사용
                aosYear = startTime.year.toUShort(),
                aosMonth = startTime.monthValue.toByte(),
                aosDay = startTime.dayOfMonth.toByte(),
                aosHour = startTime.hour.toByte(),
                aosMinute = startTime.minute.toByte(),
                aosSecond = startTime.second.toByte(),
                aosMs = startTimeMs,
                losYear = endTime.year.toUShort(),
                losMonth = endTime.monthValue.toByte(),
                losDay = endTime.dayOfMonth.toByte(),
                losHour = endTime.hour.toByte(),
                losMinute = endTime.minute.toByte(),
                losSecond = endTime.second.toByte(),
                losMs = endTimeMs,
            )

            // UdpFwICDService를 통해 데이터 전송
            udpFwICDService.sendSatelliteTrackHeader(headerFrame)
            logger.info("위성 추적 전체 길이 ${calculateDataByteSize(mstId, actualDetailId).toUShort()}")
            logger.info("위성 추적 헤더 정보 전송 완료 (MstId: $mstId, DetailId: $actualDetailId)")

            dataStoreService.setEphemerisTracking(true)


        } catch (e: Exception) {
            dataStoreService.setEphemerisTracking(false)
            logger.error("위성 추적 시작 중 오류 발생: ${e.message}", e)
        }
    }

    /**
     * 위성 추적 초기 제어 명령 전송
     * 2.12.2 위성 추적 초기 제어 명령 프로토콜 사용
     * ✅ mstId와 detailId를 사용하여 초기 데이터 전송
     */
    fun sendInitialTrackingData(mstId: Long, detailId: Int = 0) {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        try {
            if (currentTrackingPass == null || trackingStatus.ephemerisStatus != true) {
                logger.error("위성 추적이 시작되지 않았습니다. 먼저 startSatelliteTracking을 호출하세요.")
                return
            }
            
            // ✅ currentTrackingPass에서 DetailId를 가져오기 (파라미터보다 우선)
            val actualDetailId = (currentTrackingPass?.get("DetailId") as? Number)?.toInt() ?: detailId
            logger.info("📡 초기 추적 데이터 전송: mstId=${mstId}, detailId=${actualDetailId} (파라미터=${detailId})")
            
            var initialTrackingData: List<Triple<UInt, Float, Float>> = emptyList()
            val passDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, actualDetailId)

            // ✅ 시간 정보 가져오기
            val (startTime, endTime) = getCurrentTrackingPassTimes()
            val calTime = GlobalData.Time.calUtcTimeOffsetTime
            val timeOffsetSeconds = GlobalData.Offset.TimeOffset
            
            logger.info("⏰ Time Offset 정보: offset=${timeOffsetSeconds}s, calTime=${calTime}, startTime=${startTime}")

            // ✅ time offset 적용 시: calTime은 이미 offset이 적용된 시간이므로,
            // 추적 데이터의 Time 필드와 직접 비교하여 가장 가까운 포인트를 찾아야 함
            // time offset이 양수면 calTime은 미래 시간이므로, 해당 시간에 해당하는 데이터 포인트를 찾음
            val timeStatus = checkTimeInTrackingRange(calTime, startTime, endTime)
            when (timeStatus) {
                TimeRangeStatus.IN_RANGE -> {
                    logger.info("🎯 현재 시간이 추적 범위 내에 있습니다 - 실시간 추적 모드")

                    // ✅ time offset이 적용된 calTime과 데이터 포인트의 Time을 직접 비교
                    // calTime은 offset이 적용된 시간이므로, 이 시간에 해당하는 데이터 포인트를 찾음
                    val closestPoint = passDetails.minByOrNull { point ->
                        val pointTime = point["Time"] as? ZonedDateTime
                        if (pointTime != null) {
                            abs(Duration.between(calTime, pointTime).toMillis())
                        } else {
                            Long.MAX_VALUE
                        }
                    }
                    
                    val calculatedIndex = if (closestPoint != null) {
                        val index = passDetails.indexOf(closestPoint)
                        val pointTime = closestPoint["Time"] as? ZonedDateTime
                        logger.info("🔍 가장 가까운 포인트 찾음: 인덱스=${index}, 포인트 시간=${pointTime}, calTime=${calTime}, 시간 차이=${if (pointTime != null) Duration.between(calTime, pointTime).toMillis() else 0}ms")
                        index
                    } else {
                        // 시간 정보가 없으면 startTime과 calTime의 차이로 계산
                        val timeDifferenceMs = Duration.between(startTime, calTime).toMillis()
                        val index = (timeDifferenceMs / 100).toInt()
                        logger.info("🔍 포인트를 찾지 못함, 시간 차이로 계산: timeDifferenceMs=${timeDifferenceMs}ms, calculatedIndex=${index}")
                        index
                    }
                    
                    logger.info("🔍 최종 인덱스 계산: calculatedIndex=${calculatedIndex}")

                    val totalSize = passDetails.size
                    val safeStartIndex = when {
                        calculatedIndex < 0 -> 0
                        calculatedIndex >= totalSize -> maxOf(0, totalSize - 50)
                        else -> calculatedIndex
                    }
                    val actualCount = minOf(50, totalSize - safeStartIndex)
                    val progressPercentage = if (totalSize > 0) {
                        (safeStartIndex.toDouble() / totalSize.toDouble()) * 100.0
                    } else 0.0

                    logger.info(
                        "실시간 추적 정보: 진행률=${progressPercentage}%, 인덱스=${safeStartIndex}/${totalSize}, 추출=${actualCount}개"
                    )

                    initialTrackingData =
                        passDetails.drop(safeStartIndex).take(actualCount).mapIndexed { index, point ->
                            Triple(
                                ((safeStartIndex + index) * 100).toUInt(),
                                (point["Elevation"] as Double).toFloat(),
                                (point["Azimuth"] as Double).toFloat()
                            )
                        }
                    // 현재 위치 정보 로깅
                    val currentPoint = initialTrackingData.firstOrNull()
                    if (currentPoint != null) {
                        logger.info("현재 추적 위치: 시간=${currentPoint.first}ms, 고도=${currentPoint.second}°, 방위=${currentPoint.third}°")
                    }
                }

                TimeRangeStatus.BEFORE_START -> {
                    logger.info("추적 시작 전입니다. 대기 중...")
                    // 대기 로직
                    val timeUntilStart = Duration.between(calTime, startTime)
                    val secondsUntilStart = timeUntilStart.seconds
                    val minutesUntilStart = timeUntilStart.toMinutes()

                    logger.info(
                        "추적 시작까지: {}분 {}초 (총 {}초)", minutesUntilStart, secondsUntilStart % 60, secondsUntilStart
                    )
                    // ✅ 대기 모드: 초기 궤도 데이터 미리 준비
                    initialTrackingData = passDetails.take(50).mapIndexed { index, point ->
                        Triple(
                            (index * 100).toUInt(), //
                            (point["Elevation"] as Double).toFloat(), (point["Azimuth"] as Double).toFloat()
                        )
                    }
                    // 시작 예정 위치 정보
                    val startPoint = initialTrackingData.firstOrNull()
                    if (startPoint != null) {
                        logger.info(
                            "시작 예정 위치: 고도=${startPoint.second}°, 방위=${startPoint.third}",
                            startPoint.second,
                            startPoint.third
                        )

                    }
                }

                TimeRangeStatus.AFTER_END -> {
                    logger.warn("추적 종료 후입니다. 추적을 중지합니다")
                    // 추적 중지 로직
                }
            }
            if (passDetails.isEmpty()) {
                logger.error("선택된 MstId($mstId), DetailId($actualDetailId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
                logger.error("   - currentTrackingPass DetailId: ${currentTrackingPass?.get("DetailId")}")
                logger.error("   - 파라미터 detailId: ${detailId}")
                dataStoreService.setEphemerisTracking(false)
                return
            }
            // 현재 시간 기준으로 NTP 시간 정보 설정
            val currentTime = GlobalData.Time.utcNow

            // 2.12.2 위성 추적 초기 제어 명령 프로토콜 생성
            val initialControlFrame = ICDService.SatelliteTrackTwo.SetDataFrame(
                cmdOne = 'T',
                cmdTwo = 'M',
                dataLen = initialTrackingData.size.toUShort(),
                ntpYear = currentTime.year.toUShort(),
                ntpMonth = currentTime.monthValue.toByte(),
                ntpDay = currentTime.dayOfMonth.toByte(),
                ntpHour = currentTime.hour.toByte(),
                ntpMinute = currentTime.minute.toByte(),
                ntpSecond = currentTime.second.toByte(),
                ntpMs = (currentTime.nano / 1_000_000).toUShort(),
                timeOffset = GlobalData.Offset.TimeOffset.toInt(), // 전역 시간 오프셋 사용
                satelliteTrackData = initialTrackingData
            )

            // UdpFwICDService를 통해 데이터 전송
            udpFwICDService.sendSatelliteTrackInitialControl(initialControlFrame)

            logger.info("위성 추적 초기 제어 길이 (${calculateInitialDataByteSize(initialTrackingData.size)} 길이)")
            logger.info("위성 추적 초기 제어 명령 전송 완료 (${initialTrackingData.size}개 데이터 포인트)")

        } catch (e: Exception) {
            dataStoreService.setEphemerisTracking(false)
            logger.error("위성 추적 초기 제어 명령 전송 중 오류 발생: ${e.message}", e)
        }
    }

    // 열거형 정의
    enum class TimeRangeStatus {
        BEFORE_START, IN_RANGE, AFTER_END
    }

    // 시간 범위 체크 함수
    private fun checkTimeInTrackingRange(
        currentTime: ZonedDateTime, startTime: ZonedDateTime, endTime: ZonedDateTime
    ): TimeRangeStatus {
        return when {
            currentTime.isBefore(startTime) -> {
                val timeUntilStart = Duration.between(currentTime, startTime)
                logger.debug("추적 시작까지 남은 시간: {}초", timeUntilStart.seconds)
                TimeRangeStatus.BEFORE_START
            }

            currentTime.isAfter(endTime) -> {
                val timeAfterEnd = Duration.between(endTime, currentTime)
                logger.debug("추적 종료 후 경과 시간: {}초", timeAfterEnd.seconds)
                TimeRangeStatus.AFTER_END
            }

            else -> {
                val timeFromStart = Duration.between(startTime, currentTime)
                val timeToEnd = Duration.between(currentTime, endTime)
                logger.debug(
                    "추적 진행 중 - 시작 후: {}초, 종료까지: {}초", timeFromStart.seconds, timeToEnd.seconds
                )
                TimeRangeStatus.IN_RANGE
            }
        }
    }

    /**
     * 위성 추적 데이터 요청 처리 (ACU F/W로부터 요청 수신 시)
     * 2.12.3 위성 추적 추가 데이터 요청에 대한 응답
     * ✅ mstId와 detailId를 사용하여 추가 데이터 전송
     */
    fun handleEphemerisTrackingDataRequest(timeAcc: UInt, requestDataLength: UShort) {
        if (trackingStatus.ephemerisStatus != true || currentTrackingPass == null) {
            logger.error("위성 추적이 활성화되어 있지 않습니다.")
            return
        }
        logger.info("timeAcc :${timeAcc}.")
        logger.info("requestDataLength :${requestDataLength}.")
        // ✅ MstId 필드 사용 (No 필드 제거)
        val mstId = (currentTrackingPass!!["MstId"] as? Number)?.toLong() 
            ?: (currentTrackingPass!!["No"] as? Number)?.toLong() 
            ?: throw IllegalStateException("MstId가 없습니다")
        val detailId = (currentTrackingPass!!["DetailId"] as? Number)?.toInt() ?: 0  // ✅ UInt → Int 변경 (PassSchedule과 동일)
        logger.info("📡 데이터 요청 처리: mstId=${mstId}, detailId=${detailId}")

        // ✅ timeAcc를 인덱스로 변환 (timeAcc는 누적 시간 ms 단위, 인덱스는 100ms 단위)
        // timeAcc를 100으로 나눠서 데이터 포인트 인덱스를 구하고, 다시 100을 곱해서 ms 단위 startIndex 계산
        val startIndex = (timeAcc.toInt() / 100) * 100  // 100ms 단위로 정렬
        logger.info("startIndex :${startIndex} (timeAcc: ${timeAcc}ms -> 인덱스: ${timeAcc.toInt() / 100})")
        // 요청된 데이터 길이에 따라 데이터 포인트 수 계산
        sendAdditionalTrackingData(mstId, detailId, startIndex, requestDataLength.toInt())
        //dataStoreService.setEphemerisTracking(true)
    }

    /**
     * 위성 추적 추가 데이터 전송
     * 2.12.3 위성 추적 추가 데이터 요청에 대한 응답으로 사용
     * ✅ mstId와 detailId를 사용하여 추가 데이터 전송
     */
    fun sendAdditionalTrackingData(mstId: Long, detailId: Int = 0, startIndex: Int, requestDataLength: Int = 25) {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        try {
            if (currentTrackingPass == null || trackingStatus.ephemerisStatus != true) {
                logger.error("위성 추적이 시작되지 않았습니다. 먼저 startSatelliteTracking을 호출하세요.")
                return
            }
            
            // ✅ currentTrackingPass에서 DetailId를 가져오기 (파라미터보다 우선)
            val actualDetailId = (currentTrackingPass?.get("DetailId") as? Number)?.toInt() ?: detailId
            logger.info("📡 추가 추적 데이터 전송: mstId=${mstId}, detailId=${actualDetailId} (파라미터=${detailId}), startIndex=${startIndex}")
            
            // 선택된 패스 ID에 해당하는 세부 데이터 가져오기
            val passDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, actualDetailId)

            if (passDetails.isEmpty()) {
                logger.error("선택된 MstId($mstId), DetailId($actualDetailId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
                logger.error("   - 파라미터 detailId: ${detailId}")
                logger.error("   - currentTrackingPass DetailId: ${currentTrackingPass?.get("DetailId")}")
                logger.error("   - 사용된 actualDetailId: ${actualDetailId}")
                return
            }
            val indexMs = startIndex / 100
            logger.info("indexMs :${indexMs}.")
            // 요청된 인덱스부터 추가 데이터 준비
            val additionalTrackingData = passDetails.drop(indexMs).take(requestDataLength).mapIndexed { index, point ->
                Triple(
                    startIndex + index * 100, // 카운트 (누적 인덱스)
                    (point["Elevation"] as Double).toFloat(), (point["Azimuth"] as Double).toFloat()
                )
            }

            if (additionalTrackingData.isEmpty()) {
                logger.info("더 이상 전송할 추적 데이터가 없습니다.")


                return
            }

            // 2.12.3 위성 추적 추가 데이터 응답 프로토콜 생성
            val additionalDataFrame = ICDService.SatelliteTrackThree.SetDataFrame(
                cmdOne = 'T',
                cmdTwo = 'R',
                dataLength = additionalTrackingData.size.toUShort(),
                satelliteTrackData = additionalTrackingData
            )

            // UdpFwICDService를 통해 데이터 전송
            udpFwICDService.sendSatelliteTrackAdditionalData(additionalDataFrame)

            logger.info("위성 추적 추가 데이터 전송 완료 (${additionalTrackingData.size}개 데이터 포인트, 시작 인덱스: $startIndex)")

        } catch (e: Exception) {
            logger.error("위성 추적 추가 데이터 전송 중 오류 발생: ${e.message}", e)
        }
    }

    /**
     * 시간 오프셋 명령 - Mono 비동기 처리
     * 위성 초기 정보전달인 2.12.2 진행 후 Time Offset 값 전달
     */
    fun ephemerisTimeOffsetCommand(inputTimeOffset: Float) {
        Mono.fromCallable {
            GlobalData.Offset.TimeOffset = inputTimeOffset
            udpFwICDService.writeNTPCommand()
            // ✅ 상태 기반: TRACKING 상태일 때만 초기 데이터 전송
            if (currentTrackingState == TrackingState.TRACKING) {
                currentTrackingPassId?.let { mstId ->
                    val detailId = (currentTrackingPass?.get("DetailId") as? Number)?.toInt() ?: 0
                    logger.info("추적 중인 패스 발견 (TRACKING 상태), 초기 데이터 전송 시작: mstId={}, detailId={}", mstId, detailId)
                    sendInitialTrackingData(mstId, detailId)
                    logger.info("초기 추적 데이터 전송 완료: mstId={}, detailId={}", mstId, detailId)
                } ?: run {
                    logger.warn("현재 추적 중인 패스가 없어서 초기 데이터를 전송하지 않습니다")
                }
            } else {
                logger.info("⏳ Time Offset 설정 완료, 시작 시간 대기 중 (초기 데이터는 TRACKING 상태에서 전송)")
            }
            //Time Offset 전달
            udpFwICDService.timeOffsetCommand(inputTimeOffset)
            // 글로벌 데이터 업데이트


            logger.info("TimeOffset 명령 전송 완료: {}s", inputTimeOffset)
        }.subscribeOn(Schedulers.boundedElastic()).subscribe({ /* 성공 */ }, { error ->
            logger.error("시간 오프셋 명령 처리 오류: {}", error.message, error)
        })
    }

    fun setCurrentTrackingPassId(newPassId: Long?) {  // ✅ UInt → Long 변경 (PassSchedule과 동일)
        // 유효성 검사
        if (newPassId != null && newPassId <= 0L) {  // ✅ 0u → 0L 변경 (Long 타입)
            throw IllegalArgumentException("패스 ID는 양수여야 합니다: $newPassId")
        }
        // 새 패스 ID 설정
        currentTrackingPassId = newPassId
    }


    fun stopCommand() {
        val multiAxis = BitSet()
        multiAxis.set(0)
        multiAxis.set(1)
        multiAxis.set(2)
        udpFwICDService.stopCommand(multiAxis)
    }


    /**
     * 패스의 첫 번째 방위각 가져오기
     * ✅ mstId와 detailId를 사용하여 첫 번째 방위각 조회
     */
    private fun getFirstAzimuthForPass(mstId: Long, detailId: Int = 0): Float {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        val passDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, detailId)
        return if (passDetails.isNotEmpty()) {
            (passDetails.first()["Azimuth"] as Double).toFloat()
        } else {
            0.0f
        }
    }

    /**
     * 패스의 첫 번째 고도각 가져오기
     * ✅ mstId와 detailId를 사용하여 첫 번째 고도각 조회
     */
    private fun getFirstElevationForPass(mstId: Long, detailId: Int = 0): Float {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        val passDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, detailId)
        return if (passDetails.isNotEmpty()) {
            (passDetails.first()["Elevation"] as Double).toFloat()
        } else {
            0.0f
        }
    }

    // 헬퍼 함수 정의
    private fun getCurrentTrackingPassTimes(): Pair<ZonedDateTime, ZonedDateTime> {
        val pass = currentTrackingPass ?: throw IllegalStateException("현재 추적 중인 패스가 설정되지 않았습니다")

        val startTime = try {
            (pass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            logger.error("StartTime 추출 실패: {}", e.message, e)
            throw IllegalStateException("StartTime 정보를 가져올 수 없습니다: ${e.message}")
        }

        val endTime = try {
            (pass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            logger.error("EndTime 추출 실패: {}", e.message, e)
            throw IllegalStateException("EndTime 정보를 가져올 수 없습니다: ${e.message}")
        }

        logger.debug("현재 추적 패스 시간 정보 - 시작: {}, 종료: {}", startTime, endTime)

        return Pair(startTime, endTime)
    }

    /**
     * 전체 데이터 길이 계산
     * ✅ mstId와 detailId를 사용하여 데이터 바이트 크기 계산
     */
    private fun calculateDataByteSize(mstId: Long, detailId: Int = 0): Int {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        val passDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, detailId)
        return passDetails.size * 12 // 각 데이터 포인트는 12바이트 (4바이트 시간, 4바이트 방위각, 4바이트 고도각)
    }

    /**
     * 초기 데이터 길이 계산
     */
    private fun calculateInitialDataByteSize(dataPointCount: Int): Int {
        return (dataPointCount * 12) + 18 + 3 // 헤더 18바이트 + 각 데이터 포인트 12바이트
    }

    private fun calculateAdditionalDataLength(dataPointCount: Int): Int {

        return dataPointCount// 헤더 5바이트 + 각 데이터 포인트 12바이트
    }

    /**
     * 전체 데이터 길이 계산
     * ✅ mstId와 detailId를 사용하여 데이터 길이 계산
     */
    private fun calculateDataLength(mstId: Long, detailId: Int = 0): Int {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        val passDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, detailId)
        logger.info("전체 데이터 길이 계산 시작: MstId = $mstId, DetailId = $detailId, 사이즈 : ${passDetails.size}")
        return passDetails.size
    }

    /**
     * 현재 추적 상태를 반환합니다.
     */
    fun isTracking(): Boolean {
        return trackingStatus.ephemerisStatus == true
    }

    /**
     * 현재 추적 중인 패스 정보를 반환합니다.
     */
    fun getCurrentTrackingPass(): Map<String, Any?>? {
        return currentTrackingPass
    }

    /**
     * 모든 위성 추적 마스터 데이터 조회
     */
    fun getAllEphemerisTrackMst(): List<Map<String, Any?>> {
        return ephemerisTrackMstStorage.toList()
    }

    /**
     * ✅ Original과 FinalTransformed 데이터를 병합하여 반환
     * UI에서 2축/최종변환 값을 동시에 표시하기 위한 API
     * 
     * @return Original과 FinalTransformed 메타데이터가 병합된 MST 데이터 리스트
     */
    fun getAllEphemerisTrackMstMerged(): List<Map<String, Any?>> {
        try {
            // ✅ 요청 ID (디버깅용) - 함수 전체에서 재사용
            val requestId = System.currentTimeMillis() % 10000
            // logger.info("📊 [요청 #$requestId] Original, FinalTransformed, KeyholeAxisTransformed, KeyholeFinalTransformed, KeyholeOptimized 데이터 병합 시작")
            
            val originalMst = ephemerisTrackMstStorage.filter { it["DataType"] == "original" }
            val finalMst = ephemerisTrackMstStorage.filter { it["DataType"] == "final_transformed" }
            val keyholeAxisMst = ephemerisTrackMstStorage.filter { it["DataType"] == "keyhole_axis_transformed" }  // ✅ 추가
            val keyholeMst = ephemerisTrackMstStorage.filter { it["DataType"] == "keyhole_final_transformed" }
            val keyholeOptimizedMst = ephemerisTrackMstStorage.filter { it["DataType"] == "keyhole_optimized_final_transformed" }  // ✅ 추가: 방법 2 (최적화)
            
            if (finalMst.isEmpty()) {
                logger.warn("⚠️ FinalTransformed 데이터가 없습니다")
                return emptyList()
            }
            
            // 🔍 디버깅: finalMst 데이터 확인
            // logger.info("🔍 [요청 #$requestId] finalMst 크기: ${finalMst.size}")
            // if (finalMst.isNotEmpty()) {
            //     logger.info("🔍 [요청 #$requestId] 첫 번째 finalMst 항목의 키: ${finalMst[0].keys}")
            //     logger.info("🔍 [요청 #$requestId] 첫 번째 finalMst 항목의 MstId 필드: ${finalMst[0]["MstId"]} (타입: ${finalMst[0]["MstId"]?.let { it::class.simpleName }})")
            // }
            
            val mergedData = finalMst.mapNotNull { final ->
                // ✅ MstId 필드에서만 mstId 추출 (No 필드 제거)
                val mstId = try {
                    val mstIdValue = final["MstId"]
                    when (mstIdValue) {
                        is Number -> mstIdValue.toLong()
                        is Long -> mstIdValue
                        is Int -> mstIdValue.toLong()
                        is UInt -> mstIdValue.toLong()
                        else -> (mstIdValue as? Number)?.toLong()
                    }
                } catch (e: Exception) {
                    logger.error("❌ [요청 #$requestId] mstId 추출 실패: ${e.message}, MstId=${final["MstId"]}")
                    null
                }
                
                if (mstId == null) {
                    logger.warn("⚠️ [요청 #$requestId] MST 데이터에 MstId 필드가 없습니다: ${final.keys}")
                    logger.warn("⚠️ [요청 #$requestId] MstId 필드 값: ${final["MstId"]} (타입: ${final["MstId"]?.let { it::class.simpleName }})")
                    return@mapNotNull null  // ✅ null 반환하여 필터링
                }
                
                // ✅ MstId 필드만 사용 (No 필드 제거)
                val original = originalMst.find { 
                    val originalId = (it["MstId"] as? Number)?.toLong()
                    originalId == mstId
                }
                val keyholeAxis = keyholeAxisMst.find { 
                    val axisId = (it["MstId"] as? Number)?.toLong()
                    axisId == mstId
                }
                val keyhole = keyholeMst.find { 
                    val keyholeId = (it["MstId"] as? Number)?.toLong()
                    keyholeId == mstId
                }
                val keyholeOptimized = keyholeOptimizedMst.find { 
                    val optimizedId = (it["MstId"] as? Number)?.toLong()
                    optimizedId == mstId
                }
                
                // ✅ Keyhole 판단: final_transformed (Train=0) 기준으로 판단
                val train0MaxAzRate = final["MaxAzRate"] as? Double ?: 0.0
                val threshold = settingsService.keyholeAzimuthVelocityThreshold
                val isKeyhole = train0MaxAzRate >= threshold
                
                // 🔍 디버깅: Keyhole Optimized 데이터 확인 (비활성화)
                // if (isKeyhole) {
                //     logger.info("🔍 [요청 #$requestId] MST #$mstId Keyhole Optimized 디버깅:")
                //     logger.info("   [요청 #$requestId] keyholeOptimizedMst 전체 크기: ${keyholeOptimizedMst.size}")
                //     logger.info("   [요청 #$requestId] keyholeOptimizedMst의 No 필드들: ${keyholeOptimizedMst.map { it["No"] }}")
                //     logger.info("   [요청 #$requestId] 찾는 mstId: $mstId (타입: ${mstId::class.simpleName})")
                //     logger.info("   [요청 #$requestId] keyholeOptimized 찾음: ${keyholeOptimized != null}")
                //     logger.info("   [요청 #$requestId] isKeyhole: $isKeyhole")
                //     if (keyholeOptimized != null) {
                //         logger.info("   [요청 #$requestId] keyholeOptimized의 RecommendedTrainAngle: ${keyholeOptimized["RecommendedTrainAngle"]}")
                //         logger.info("   [요청 #$requestId] keyholeOptimized의 MaxAzRate: ${keyholeOptimized["MaxAzRate"]}")
                //     } else {
                //         logger.warn("⚠️ [요청 #$requestId] MST #$mstId: Keyhole 발생했으나 keyholeOptimized 데이터를 찾을 수 없습니다.")
                //         // 🔍 추가 디버깅: 타입 불일치 확인
                //         keyholeOptimizedMst.forEach { mst ->
                //             val mstNo = mst["No"]
                //             logger.info("   [요청 #$requestId] keyholeOptimizedMst 항목 - No: $mstNo (타입: ${mstNo?.let { it::class.simpleName }}), 일치 여부: ${mstNo == mstId}")
                //         }
                //     }
                // }
                
                // 백업: Original MST의 IsKeyhole도 확인 (데이터 정합성)
                val isKeyholeFromOriginal = original?.get("IsKeyhole") as? Boolean ?: false
                if (isKeyhole != isKeyholeFromOriginal) {
                    logger.warn("⚠️ MST #$mstId: Keyhole 판단 불일치 (Final: $isKeyhole, Original: $isKeyholeFromOriginal)")
                }
                
                // ✅ DetailId는 final에서 가져오기 (PassSchedule과 동일)
                val detailId = (final["DetailId"] as? Number)?.toInt() ?: 0
                
                // ✅ 각각 별도 계산 (합계법) - detailId 전달 (PassSchedule과 동일)
                val originalRates = calculateOriginalSumMethodRates(mstId, detailId)
                val finalRates = calculateFinalTransformedSumMethodRates(mstId, "final_transformed", detailId)
                
                final.toMutableMap().apply {
                    // ✅ MstId와 DetailId 필드 추가 (PassSchedule과 동일한 구조)
                    put("MstId", mstId)
                    put("DetailId", detailId)  // ✅ final의 DetailId 사용 (PassSchedule과 동일)
                    put("No", mstId)  // ✅ 하위 호환성을 위해 No 필드도 유지
                    
                    // Original (2축) 메타데이터 추가
                    put("OriginalMaxElevation", original?.get("MaxElevation"))
                    put("OriginalMaxAzAccel", original?.get("MaxAzAccel"))
                    put("OriginalMaxElAccel", original?.get("MaxElAccel"))
                    
                    // FinalTransformed 속도 (합계법, Train=0) - 참고용
                    put("FinalTransformedMaxAzRate", finalRates["maxAzRate"])
                    put("FinalTransformedMaxElRate", finalRates["maxElRate"])
                    
                    // Original (2축) 속도 (합계법)
                    put("OriginalMaxAzRate", originalRates["maxAzRate"])
                    put("OriginalMaxElRate", originalRates["maxElRate"])
                    
                    // ✅ Keyhole Axis Transformed 데이터 추가 (각도 제한 ❌, Train≠0)
                    if (keyholeAxis != null && isKeyhole) {
                        val keyholeAxisRates = calculateFinalTransformedSumMethodRates(mstId, "keyhole_axis_transformed", detailId)  // ✅ detailId 전달
                        put("KeyholeAxisTransformedMaxAzRate", keyholeAxisRates["maxAzRate"])  // ✅ Keyhole Axis 데이터
                        put("KeyholeAxisTransformedMaxElRate", keyholeAxisRates["maxElRate"])  // ✅ Keyhole Axis 데이터
                    } else {
                        // Keyhole 미발생 시 Train=0 값 사용
                        put("KeyholeAxisTransformedMaxAzRate", finalRates["maxAzRate"])
                        put("KeyholeAxisTransformedMaxElRate", finalRates["maxElRate"])
                    }
                    
                    // ✅ Keyhole 발생 시 KeyholeFinalTransformed 데이터로 속도 계산 (각도 제한 ✅, Train≠0)
                    // keyholeRates를 블록 밖에서 선언하여 재사용 가능하도록 함
                    val keyholeRates = if (keyhole != null && isKeyhole) {
                        calculateFinalTransformedSumMethodRates(mstId, "keyhole_optimized_final_transformed", detailId)
                    } else {
                        null
                    }
                    
                    if (keyholeRates != null) {
                        put("KeyholeFinalTransformedMaxAzRate", keyholeRates["maxAzRate"])  // ✅ Keyhole Final 데이터
                        put("KeyholeFinalTransformedMaxElRate", keyholeRates["maxElRate"])  // ✅ Keyhole Final 데이터
                    } else {
                        put("KeyholeFinalTransformedMaxAzRate", finalRates["maxAzRate"])  // FinalTransformed 사용
                        put("KeyholeFinalTransformedMaxElRate", finalRates["maxElRate"])  // FinalTransformed 사용
                    }
                    
                    // ✅ FinalTransformed 시작/종료 각도 및 최대 고도 (Train=0, ±270°)
                    // 항상 final_transformed MST의 값 제공
                    put("FinalTransformedStartAzimuth", final["StartAzimuth"])
                    put("FinalTransformedEndAzimuth", final["EndAzimuth"])
                    put("FinalTransformedStartElevation", final["StartElevation"])
                    put("FinalTransformedEndElevation", final["EndElevation"])
                    put("FinalTransformedMaxElevation", final["MaxElevation"])
                    
                    // ✅ KeyholeFinalTransformed 시작/종료 각도 및 최대 고도 (Train≠0, ±270°)
                    // 항상 keyhole_final_transformed MST의 값 제공 (없으면 null)
                    // 조건부 로직 없이 항상 제공 - 프론트엔드에서 선택
                    put("KeyholeFinalTransformedStartAzimuth", keyhole?.get("StartAzimuth"))
                    put("KeyholeFinalTransformedEndAzimuth", keyhole?.get("EndAzimuth"))
                    put("KeyholeFinalTransformedStartElevation", keyhole?.get("StartElevation"))
                    put("KeyholeFinalTransformedEndElevation", keyhole?.get("EndElevation"))
                    put("KeyholeFinalTransformedMaxElevation", keyhole?.get("MaxElevation"))
                    
                    // ✅ 하드웨어 제한 각도 기준으로 필터링된 데이터의 MaxElevation 재계산
                    // SelectSchedule 화면에서 필터링된 데이터 기준으로 표시하기 위함
                    // 필터링된 데이터 조회 (getEphemerisTrackDtlByMstIdAndDetailId는 이미 하드웨어 제한 각도 기준으로 필터링된 데이터 반환)
                    // ✅ detailId는 final에서 가져온 값 사용 (PassSchedule과 동일)
                    val filteredData = getEphemerisTrackDtlByMstIdAndDetailId(mstId, detailId)  // ✅ detailId 변수 사용
                    
                    // 필터링된 데이터 기준 MaxElevation 계산
                    val filteredMaxElevation = if (filteredData.isNotEmpty()) {
                        filteredData.maxOfOrNull { (it["Elevation"] as? Double) ?: Double.NEGATIVE_INFINITY }
                    } else {
                        null
                    }
                    
                    // ✅ MaxElevation 설정 (SelectSchedule에서 사용하는 필드)
                    // 필터링된 데이터 기준으로 계산된 값 사용, 없으면 final의 MaxElevation 사용
                    put("MaxElevation", filteredMaxElevation ?: (final["MaxElevation"] as? Double))
                    
                    // ✅ 방법 2 (신규): Keyhole Optimized 데이터 추가
                    // logger.info("🔍 [요청 #$requestId] MST #$mstId: Keyhole Optimized 조건 확인:")
                    // logger.info("   - keyholeOptimized != null: ${keyholeOptimized != null}")
                    // logger.info("   - isKeyhole: $isKeyhole")
                    // logger.info("   - 조건 결과 (keyholeOptimized != null && isKeyhole): ${keyholeOptimized != null && isKeyhole}")
                    
                    if (keyholeOptimized != null && isKeyhole) {
                        // logger.info("✅ [요청 #$requestId] MST #$mstId: Keyhole Optimized 데이터 처리 시작")
                        // 🔍 데이터 존재 여부 확인
                        val keyholeOptimizedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "keyhole_optimized_final_transformed", detailId)  // ✅ detailId 전달
                        // logger.info("   [요청 #$requestId] keyhole_optimized_final_transformed DTL 데이터 크기: ${keyholeOptimizedDtl.size}개")
                        if (keyholeOptimizedDtl.isEmpty()) {
                            logger.warn("⚠️ [요청 #$requestId] MST #$mstId: keyhole_optimized_final_transformed DTL 데이터가 없습니다!")
                        }

                        val keyholeOptimizedRates = calculateFinalTransformedSumMethodRates(
                            mstId,
                            "keyhole_optimized_final_transformed",
                            detailId  // ✅ detailId 전달
                        )
                        // logger.info("   [요청 #$requestId] 계산된 Rates: maxAzRate=${keyholeOptimizedRates["maxAzRate"]}, maxElRate=${keyholeOptimizedRates["maxElRate"]}")
                        // logger.info("   [요청 #$requestId] RecommendedTrainAngle: ${keyholeOptimized["RecommendedTrainAngle"]}")
                        // logger.info("   [요청 #$requestId] API 응답에 설정되는 값들:")
                        // logger.info("      - KeyholeOptimizedRecommendedTrainAngle: ${keyholeOptimized["RecommendedTrainAngle"]}")
                        // logger.info("      - KeyholeOptimizedFinalTransformedMaxAzRate: ${keyholeOptimizedRates["maxAzRate"]}")
                        // logger.info("      - KeyholeOptimizedFinalTransformedMaxElRate: ${keyholeOptimizedRates["maxElRate"]}")
                        val recommendedTrainAngleValue = keyholeOptimized["RecommendedTrainAngle"] as? Double ?: 0.0
                        val maxAzRateValue = keyholeOptimizedRates["maxAzRate"] as? Double ?: 0.0
                        val maxElRateValue = keyholeOptimizedRates["maxElRate"] as? Double ?: 0.0

                        // logger.info("   [요청 #$requestId] 실제 API 응답에 설정되는 값들:")
                        // logger.info("      - KeyholeOptimizedRecommendedTrainAngle: $recommendedTrainAngleValue")
                        // logger.info("      - KeyholeOptimizedFinalTransformedMaxAzRate: $maxAzRateValue")
                        // logger.info("      - KeyholeOptimizedFinalTransformedMaxElRate: $maxElRateValue")
                        
                        put("KeyholeOptimizedFinalTransformedMaxAzRate", maxAzRateValue)
                        put("KeyholeOptimizedFinalTransformedMaxElRate", maxElRateValue)
                        put("KeyholeOptimizedRecommendedTrainAngle", recommendedTrainAngleValue)
                        
                        // ✅ 비교 결과 계산 (방법 1의 keyholeRates 재사용 - 이미 위에서 계산됨)
                        val method1MaxAzRate = keyholeRates?.get("maxAzRate") as? Double ?: 0.0
                        val method2MaxAzRate = keyholeOptimizedRates["maxAzRate"] as? Double ?: 0.0
                        val improvement = method1MaxAzRate - method2MaxAzRate
                        val improvementRate = if (method1MaxAzRate > 0) {
                            (improvement / method1MaxAzRate) * 100.0
                        } else {
                            0.0
                        }
                        
                        logger.info("   [요청 #$requestId] 비교 결과:")
                        logger.info("      - OptimizationImprovement: $improvement")
                        logger.info("      - OptimizationImprovementRate: $improvementRate")
                        
                        put("OptimizationImprovement", improvement)
                        put("OptimizationImprovementRate", improvementRate)
                        
                        // 🔍 최종 확인: 실제로 put된 값들
                        logger.info("   [요청 #$requestId] 최종 확인 - put된 값들:")
                        logger.info("      - KeyholeOptimizedRecommendedTrainAngle: ${get("KeyholeOptimizedRecommendedTrainAngle")}")
                        logger.info("      - KeyholeOptimizedFinalTransformedMaxAzRate: ${get("KeyholeOptimizedFinalTransformedMaxAzRate")}")
                        logger.info("      - KeyholeOptimizedFinalTransformedMaxElRate: ${get("KeyholeOptimizedFinalTransformedMaxElRate")}")
                        logger.info("      - OptimizationImprovement: ${get("OptimizationImprovement")}")
                        logger.info("      - OptimizationImprovementRate: ${get("OptimizationImprovementRate")}")
                    } else {
                        // Keyhole 미발생 시 기본값 설정
                        if (isKeyhole && keyholeOptimized == null) {
                            logger.warn("⚠️ [요청 #$requestId] MST #$mstId: Keyhole 발생했으나 keyholeOptimized가 null입니다. 기본값(0)으로 설정합니다.")
                        } else if (!isKeyhole) {
                            logger.info("   [요청 #$requestId] MST #$mstId: Keyhole 미발생 (isKeyhole=false). 기본값(0)으로 설정합니다.")
                        } else {
                            logger.warn("⚠️ [요청 #$requestId] MST #$mstId: 예상치 못한 조건 (keyholeOptimized=${keyholeOptimized != null}, isKeyhole=$isKeyhole)")
                        }
                        put("KeyholeOptimizedFinalTransformedMaxAzRate", finalRates["maxAzRate"])
                        put("KeyholeOptimizedFinalTransformedMaxElRate", finalRates["maxElRate"])
                        put("KeyholeOptimizedRecommendedTrainAngle", 0.0)
                        put("OptimizationImprovement", 0.0)
                        put("OptimizationImprovementRate", 0.0)
                    }
                    
                    // ✅ Keyhole 관련 정보
                    // Keyhole 판단은 finalTransformedMst 기준으로 수행하므로, RecommendedTrainAngle도 finalTransformedMst에서 가져옴
                    put("IsKeyhole", isKeyhole)
                    put("RecommendedTrainAngle", final.get("RecommendedTrainAngle") as? Double ?: 0.0)
                    
                    // 중앙차분법 데이터는 주석으로 보관 (실시간 제어용)
                    put("CentralDiffMaxAzRate", original?.get("MaxAzRate"))
                    put("CentralDiffMaxElRate", original?.get("MaxElRate"))
                }
            }
            
            // ✅ Step 2: Select Schedule 목록에서 스케줄 필터링 (하드웨어 제한 각도 기준)
            // ✅ 필터링 제거: 3축 변환 후 Elevation 음수 허용
            // val elevationMin = settingsService.angleElevationMin
            // 
            // val filteredMergedData = mergedData.filter { item ->
            //     val maxElevation = item["MaxElevation"] as? Double
            //     // ✅ MaxElevation이 null이면 필터링에서 제외하지 않음 (데이터가 있는 경우만 필터링)
            //     if (maxElevation == null) {
            //         logger.warn("⚠️ [요청 #$requestId] MST #${item["MstId"]}: MaxElevation이 null입니다. 필터링에서 제외하지 않습니다.")
            //         true  // ✅ null인 경우도 포함
            //     } else {
            //         maxElevation >= elevationMin
            //     }
            // }
            
            logger.info("✅ [요청 #$requestId] 병합 완료: ${mergedData.size}개 MST 레코드 (KeyholeAxis + KeyholeFinal 데이터 포함)")
            logger.info("✅ [요청 #$requestId] 필터링 제거: 모든 데이터 반환")
            return mergedData
            
        } catch (error: Exception) {
            logger.error("❌ 데이터 병합 실패: ${error.message}", error)
            return emptyList()
        }
    }
    
    /**
     * ✅ Original (2축) 합계법 최대 속도 계산
     * 연속 10개 데이터(1초)의 변화량을 모두 더한 값 중 최대값을 반환
     * 이론치 계산용 - 시간으로 나누지 않음!
     * 
     * @param mstId 마스터 ID
     * @param detailId 패스 인덱스 (기본값: 0)
     * @return 합계법으로 계산된 최대 속도 (도/초)
     */
    private fun calculateOriginalSumMethodRates(mstId: Long, detailId: Int = 0): Map<String, Double> {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        try {
            // ✅ original 데이터는 getEphemerisTrackDtlByMstIdAndDataType으로 직접 조회
            // getEphemerisTrackDtlByMstIdAndDetailId는 final_transformed만 반환하므로 사용 불가
            val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "original", detailId)
            
            if (originalDtl.size < 11) {
                logger.warn("⚠️ MST ID $mstId: Original 속도 계산을 위한 데이터가 부족합니다 (${originalDtl.size}개)")
                return mapOf("maxAzRate" to 0.0, "maxElRate" to 0.0)
            }
            
            var maxAzRate = 0.0
            var maxElRate = 0.0
            
            // ✅ Backward Looking: 각 index의 과거 10개 변화량 계산 (현재 index 포함, 미래 제외)
            for (i in 9 until originalDtl.size) {
                var currentAzSum = 0.0
                var currentElSum = 0.0
                
                // Index i의 값 = (i-9)부터 i까지의 10개 변화량 합 (j-1이 유효하도록)
                for (j in (i - 9)..i) {
                    if (j > 0) { // j-1이 유효한 경우만 계산
                        val prevPoint = originalDtl[j - 1]
                        val currentPoint = originalDtl[j]
                        
                        val prevAz = prevPoint["Azimuth"] as Double
                        val currentAz = currentPoint["Azimuth"] as Double
                        val prevEl = prevPoint["Elevation"] as Double
                        val currentEl = currentPoint["Elevation"] as Double
                        
                        // 방위각 변화량 계산 (360도 경계 처리)
                        var azDiff = currentAz - prevAz
                        if (azDiff > 180) azDiff -= 360
                        if (azDiff < -180) azDiff += 360
                        
                        // 단순 합계 (시간으로 나누지 않음!)
                        currentAzSum += kotlin.math.abs(azDiff)
                        currentElSum += kotlin.math.abs(currentEl - prevEl)
                    }
                }
                
                // 최대값 업데이트
                maxAzRate = maxOf(maxAzRate, currentAzSum)
                maxElRate = maxOf(maxElRate, currentElSum)
            }
            
            logger.info("✅ Original 합계법: Az=${String.format("%.6f", maxAzRate)}°/s, El=${String.format("%.6f", maxElRate)}°/s")
            logger.info("  - 데이터 크기: ${originalDtl.size}개")
            logger.info("  - Backward Looking 반복: ${originalDtl.size - 9}회")
            logger.info("  - 계산 범위: Index 9 ~ ${originalDtl.size - 1}")
            
            // 디버깅: 첫 번째 계산 결과 확인
            if (originalDtl.size >= 10) {
                var debugSum = 0.0
                for (j in 1..9) {
                    val prevPoint = originalDtl[j - 1]
                    val currentPoint = originalDtl[j]
                    val prevAz = prevPoint["Azimuth"] as Double
                    val currentAz = currentPoint["Azimuth"] as Double
                    var azDiff = currentAz - prevAz
                    if (azDiff > 180) azDiff -= 360
                    if (azDiff < -180) azDiff += 360
                    debugSum += kotlin.math.abs(azDiff)
                }
                logger.info("  - Index 9 디버깅: 첫 10개 변화량 합 = ${String.format("%.6f", debugSum)}")
            }
            
            return mapOf(
                "maxAzRate" to maxAzRate,
                "maxElRate" to maxElRate
            )
            
        } catch (error: Exception) {
            logger.error("❌ Original 합계법 계산 실패: ${error.message}", error)
            return mapOf("maxAzRate" to 0.0, "maxElRate" to 0.0)
        }
    }

    /**
     * ✅ FinalTransformed 합계법 최대 속도 계산
     * 연속 10개 데이터(1초)의 변화량을 모두 더한 값 중 최대값을 반환
     * 이론치 계산용 - 시간으로 나누지 않음!
     * 
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입 (기본값: "final_transformed")
     * @param detailId 패스 인덱스 (기본값: 0)
     * @return 합계법으로 계산된 최대 속도 (도/초)
     */
    private fun calculateFinalTransformedSumMethodRates(
        mstId: Long,  // ✅ UInt → Long 변경 (PassSchedule과 동일)
        dataType: String = "final_transformed",  // ✅ 파라미터 추가
        detailId: Int = 0  // ✅ UInt → Int 변경 (PassSchedule과 동일)
    ): Map<String, Double> {
        try {
            // ✅ 특정 DataType 데이터는 getEphemerisTrackDtlByMstIdAndDataType으로 직접 조회
            // getEphemerisTrackDtlByMstIdAndDetailId는 Keyhole 여부에 따라 특정 DataType만 반환하므로 사용 불가
            val finalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, dataType, detailId)
            
            if (finalDtl.size < 11) {
                logger.warn("⚠️ MST ID $mstId: $dataType 속도 계산 부족")
                return mapOf("maxAzRate" to 0.0, "maxElRate" to 0.0)
            }
            
            var maxAzRate = 0.0
            var maxElRate = 0.0
            
            // ✅ Backward Looking: 각 index의 과거 10개 변화량 계산 (현재 index 포함, 미래 제외)
            for (i in 9 until finalDtl.size) {
                var currentAzSum = 0.0
                var currentElSum = 0.0
                
                // Index i의 값 = (i-9)부터 i까지의 10개 변화량 합 (j-1이 유효하도록)
                for (j in (i - 9)..i) {
                    if (j > 0) { // j-1이 유효한 경우만 계산
                        val prevPoint = finalDtl[j - 1]
                        val currentPoint = finalDtl[j]
                        
                        val prevAz = prevPoint["Azimuth"] as Double
                        val currentAz = currentPoint["Azimuth"] as Double
                        val prevEl = prevPoint["Elevation"] as Double
                        val currentEl = currentPoint["Elevation"] as Double
                        
                        // 방위각 변화량 계산 (360도 경계 처리)
                        var azDiff = currentAz - prevAz
                        if (azDiff > 180) azDiff -= 360
                        if (azDiff < -180) azDiff += 360
                        
                        // 단순 합계 (시간으로 나누지 않음!)
                        currentAzSum += kotlin.math.abs(azDiff)
                        currentElSum += kotlin.math.abs(currentEl - prevEl)
                    }
                }
                
                // 최대값 업데이트
                maxAzRate = maxOf(maxAzRate, currentAzSum)
                maxElRate = maxOf(maxElRate, currentElSum)
            }
            
            logger.info("✅ FinalTransformed 합계법: Az=${String.format("%.6f", maxAzRate)}°/s, El=${String.format("%.6f", maxElRate)}°/s")
            logger.info("  - 데이터 크기: ${finalDtl.size}개")
            logger.info("  - 슬라이딩 윈도우 반복: ${finalDtl.size - 10}회")
            
            return mapOf(
                "maxAzRate" to maxAzRate,
                "maxElRate" to maxElRate
            )
            
        } catch (error: Exception) {
            logger.error("❌ FinalTransformed 합계법 계산 실패: ${error.message}", error)
            return mapOf("maxAzRate" to 0.0, "maxElRate" to 0.0)
        }
    }

    /**
     * 특정 마스터 ID와 detailId에 해당하는 세부 추적 데이터 조회 (실제 추적 명령용)
     * 
     * ✅ mstId와 detailId를 사용하여 조회 (PassSchedule과 동일한 구조)
     * 
     * ✅ Keyhole 여부에 따라 적절한 DataType 자동 선택:
     *    - Keyhole 발생: keyhole_final_transformed (Train≠0, ±270°)
     *    - Keyhole 미발생: final_transformed (Train=0, ±270°)
     * 
     * ✅ 하드웨어 제한 각도 기준으로 필터링:
     *    - sourceMinElevationAngle로 넓게 추적한 데이터 중
     *    - 하드웨어 제한 각도(angleElevationMin) 이상만 사용
     *    - 순수 2축 sourceMinElevationAngle 기준으로만 판단
     * 
     * ✅ 예외 처리:
     *    - final_transformed MST 없음: 빈 리스트 반환 + 경고 로그
     *    - Keyhole 발생 시 keyhole_final_transformed 데이터 없음: final_transformed로 폴백 + 경고 로그
     *    - 필터링 후 데이터 없음: 빈 리스트 반환 + 경고 로그
     * 
     * @param mstId 마스터 ID
     * @param detailId 패스 인덱스 (기본값: 0)
     * @return 필터링된 세부 추적 데이터 리스트 (실제 추적 명령에 사용)
     */
    fun getEphemerisTrackDtlByMstIdAndDetailId(mstId: Long, detailId: Int = 0): List<Map<String, Any?>> {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        // 1. MST에서 Keyhole 여부 확인
        // final_transformed MST에 IsKeyhole 정보가 저장되어 있음
        // ✅ MstId 필드만 사용 (No 필드 제거)
        val finalMst = ephemerisTrackMstStorage.find { 
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            dataMstId == mstId && it["DataType"] == "final_transformed"
        }
        
        if (finalMst == null) {
            logger.warn("⚠️ MST ID ${mstId}에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
            return emptyList()
        }
        
        // Keyhole 여부 확인 (final_transformed MST의 IsKeyhole 필드 사용)
        val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
        
        // 2. Keyhole 여부에 따라 DataType 선택
        // Keyhole 발생 시: keyhole_optimized_final_transformed (최적화된 Train≠0 데이터)
        // Keyhole 미발생 시: final_transformed (Train=0 데이터)
        val dataType = if (isKeyhole) {
            "keyhole_optimized_final_transformed"  // Keyhole이면 최적화 데이터 사용
        } else {
            "final_transformed"  // Keyhole 아니면 기본 데이터 사용
        }
        
        // 3. 하드웨어 제한 각도 기준으로 필터링
        // ✅ 필터링 제거: 3축 변환 후 Elevation 음수 허용
        // val elevationMin = settingsService.angleElevationMin
        
        // 선택된 DataType의 데이터 조회 (mstId와 detailId 모두 일치하는 데이터만)
        // ✅ DetailId가 null이거나 없으면 기본값 0으로 처리 (하위 호환성)
        val allData = ephemerisTrackDtlStorage.filter {
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            val dataDetailId = (it["DetailId"] as? Number)?.toInt()
            val actualDetailId = dataDetailId ?: 0
            dataMstId == mstId && actualDetailId == detailId && it["DataType"] == dataType
        }
        
        // ✅ 필터링 제거: 3축 변환 후 Elevation 음수 허용
        // 하드웨어 제한 각도 기준으로 필터링
        // val filteredData = allData.filter {
        //     (it["Elevation"] as? Double ?: 0.0) >= elevationMin
        // }
        // 
        // // ✅ 디버깅: 필터링 실패 시 Elevation 값 상세 분석
        // if (filteredData.isEmpty() && allData.isNotEmpty()) {
        //     // Elevation 값 샘플 확인 (처음 10개)
        //     val elevationSamples = allData.take(10).mapIndexed { index, item ->
        //         val elevation = item["Elevation"]
        //         val elevationType = elevation?.javaClass?.simpleName ?: "null"
        //         val elevationValue = when (elevation) {
        //             is Double -> elevation
        //             is Float -> elevation.toDouble()
        //             is Number -> elevation.toDouble()
        //             is String -> elevation.toDoubleOrNull()
        //             else -> null
        //         }
        //         mapOf(
        //             "index" to index,
        //             "type" to elevationType,
        //             "raw" to elevation,
        //             "converted" to elevationValue,
        //             "meetsCriteria" to (elevationValue != null && elevationValue >= elevationMin)
        //         )
        //     }
        //     
        //     // Elevation 통계
        //     val elevationValues = allData.mapNotNull { 
        //         when (val el = it["Elevation"]) {
        //             is Double -> el
        //             is Float -> el.toDouble()
        //             is Number -> el.toDouble()
        //             is String -> el.toDoubleOrNull()
        //             else -> null
        //         }
        //     }
        //     
        //     val minElevation = elevationValues.minOrNull()
        //     val maxElevation = elevationValues.maxOrNull()
        //     val avgElevation = if (elevationValues.isNotEmpty()) elevationValues.average() else null
        //     
        //     // Elevation 타입 분포
        //     val typeDistribution = allData.groupingBy { 
        //         it["Elevation"]?.javaClass?.simpleName ?: "null" 
        //     }.eachCount()
        //     
        //     logger.warn("⚠️ MST ID ${mstId}, DetailId=${detailId}: 필터링 결과 데이터가 없습니다.")
        //     logger.warn("   - 필터 기준: ${elevationMin}°")
        //     logger.warn("   - 전체 데이터: ${allData.size}개")
        //     logger.warn("   - Elevation 샘플 (처음 10개):")
        //     elevationSamples.forEach { sample ->
        //         logger.warn("     [${sample["index"]}] type=${sample["type"]}, raw=${sample["raw"]}, converted=${sample["converted"]}, meetsCriteria=${sample["meetsCriteria"]}")
        //     }
        //     logger.warn("   - Elevation 통계: min=${minElevation}°, max=${maxElevation}°, avg=${avgElevation}°")
        //     logger.warn("   - Elevation 타입 분포: $typeDistribution")
        // }
        // 
        // if (filteredData.isEmpty() && allData.isEmpty()) {
        //     logger.error("❌ MST ID ${mstId}, DetailId=${detailId}: 데이터가 없습니다.")
        // }
        
        // 필터링 없이 모든 데이터 반환
        if (allData.isEmpty()) {
            logger.error("❌ MST ID ${mstId}, DetailId=${detailId}: 데이터가 없습니다.")
        }
        
        return allData
    }

    /**
     * ✅ 특정 마스터 ID와 detailId에 해당하는 원본 세부 추적 데이터 조회
     * ✅ mstId와 detailId를 사용하여 조회 (PassSchedule과 동일한 구조)
     */
    fun getOriginalEphemerisTrackDtlByMstId(mstId: Long, detailId: Int = 0): List<Map<String, Any?>> {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        return getEphemerisTrackDtlByMstIdAndDataType(mstId, "original", detailId)
    }

    /**
     * ✅ 특정 마스터 ID와 detailId에 해당하는 방위각 변환 세부 추적 데이터 조회
     * ✅ mstId와 detailId를 사용하여 조회 (PassSchedule과 동일한 구조)
     */
    fun getAngleLimitedEphemerisTrackDtlByMstId(mstId: Long, detailId: Int = 0): List<Map<String, Any?>> {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        return getEphemerisTrackDtlByMstIdAndDataType(mstId, "angle_limited", detailId)
    }

    /**
     * ✅ 특정 마스터 ID와 detailId에 해당하는 모든 데이터 타입의 세부 추적 데이터 조회
     * ✅ mstId와 detailId를 사용하여 조회 (PassSchedule과 동일한 구조)
     */
    fun getAllEphemerisTrackDtlByMstId(mstId: Long, detailId: Int = 0): List<Map<String, Any?>> {  // ✅ UInt → Long/Int 변경 (PassSchedule과 동일)
        return ephemerisTrackDtlStorage.filter {
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
            dataMstId == mstId && dataDetailId == detailId
        }
    }

    /**
     * ✅ 데이터 타입별 마스터 데이터 조회
     */
    fun getEphemerisTrackMstByDataType(dataType: String): List<Map<String, Any?>> {
        return ephemerisTrackMstStorage.filter { it["DataType"] == dataType }
    }

    /**
     * ✅ 데이터 타입별 세부 데이터 조회
     */
    fun getEphemerisTrackDtlByDataType(dataType: String): List<Map<String, Any?>> {
        return ephemerisTrackDtlStorage.filter { it["DataType"] == dataType }
    }

    /**
     * ✅ 특정 마스터 ID와 데이터 타입에 해당하는 세부 데이터 조회
     * ✅ detailId 파라미터 추가 (PassSchedule과 동일한 구조)
     */
    fun getEphemerisTrackDtlByMstIdAndDataType(mstId: Long, dataType: String, detailId: Int = 0): List<Map<String, Any?>> {  // ✅ detailId 파라미터 추가
        logger.debug("🔍 [조회] getEphemerisTrackDtlByMstIdAndDataType: mstId=$mstId, dataType=$dataType, detailId=$detailId")
        
        val result = ephemerisTrackDtlStorage.filter {
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
            val itDataType = it["DataType"] as? String
            
            val matches = dataMstId == mstId && dataDetailId == detailId && itDataType == dataType
            
            // 디버깅: mstId와 dataType이 일치하지만 detailId가 다른 경우 로깅
            if (dataMstId == mstId && itDataType == dataType && dataDetailId != detailId) {
                logger.warn("⚠️ [조회] detailId 불일치 발견: MstId=$dataMstId, 저장된 DetailId=$dataDetailId, 요청 DetailId=$detailId, DataType=$itDataType")
            }
            
            matches
        }
        
        if (result.isEmpty()) {
            logger.error("❌ [조회] 데이터 없음: mstId=$mstId, dataType=$dataType, detailId=$detailId")
            // detailId 불일치 가능성 확인을 위한 추가 로깅
            val sameMstIdAndDataType = ephemerisTrackDtlStorage.filter {
                val dataMstId = (it["MstId"] as? Number)?.toLong()
                val itDataType = it["DataType"] as? String
                dataMstId == mstId && itDataType == dataType
            }
            if (sameMstIdAndDataType.isNotEmpty()) {
                val availableDetailIds = sameMstIdAndDataType.mapNotNull { (it["DetailId"] as? Number)?.toInt() ?: 0 }.distinct()
                logger.error("❌ [조회] 사용 가능한 DetailId 목록: $availableDetailIds (요청한 detailId=$detailId 와 일치하지 않음)")
            }
        } else {
            logger.debug("🔍 [조회 결과] ${result.size}개 발견: mstId=$mstId, dataType=$dataType, detailId=$detailId")
        }
        
        return result
    }

    /**
     * Keyhole 여부에 따라 적절한 MST(Master) 데이터를 반환합니다.
     * 
     * 이 함수는 위성 추적 시작 시 currentTrackingPass를 설정하기 위해 사용됩니다.
     * passId로 조회하며, Keyhole 여부에 따라 DataType을 **동적으로 선택**합니다:
     * - Keyhole 발생: keyhole_final_transformed MST (Train≠0, ±270° 제한 적용)
     * - Keyhole 미발생: final_transformed MST (Train=0, ±270° 제한 적용)
     * 
     * 선택된 MST에는 다음 정보가 포함됩니다:
     * - IsKeyhole: Keyhole 여부 (Boolean)
     * - RecommendedTrainAngle: 권장 Train 각도 (Double, Keyhole 발생 시만 0이 아님)
     * - StartTime, EndTime: 추적 시작/종료 시간
     * - 기타 추적 메타데이터
     * 
     * @param passId 패스 ID (MST ID)
     * @return Keyhole 여부에 따라 선택된 MST 데이터, 없으면 null
     * 
     * @see getEphemerisTrackDtlByMstId 동일한 Keyhole 판단 로직 사용 (DTL 데이터 반환)
     * @see getAllEphemerisTrackMstMerged Keyhole 판단 기준과 일치
     * 
     * @note 이 함수는 현재 존재하지 않으며, 새로 생성해야 합니다.
     * @note DataType은 정해져 있지 않고, Keyhole 여부에 따라 동적으로 선택됩니다.
     */
    private fun getTrackingPassMst(passId: Long): Map<String, Any?>? {  // ✅ UInt → Long 변경 (PassSchedule과 동일)
        // 1. final_transformed MST에서 IsKeyhole 확인
        // final_transformed MST에 IsKeyhole 정보가 저장되어 있음
        // ✅ MstId 필드만 사용 (No 필드 제거)
        val finalMst = ephemerisTrackMstStorage.find { 
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            dataMstId == passId && it["DataType"] == "final_transformed"  // ✅ 타입 변환 추가 (PassSchedule과 동일)
        }
        
        if (finalMst == null) {
            logger.warn("⚠️ 패스 ID ${passId}에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
            return null
        }
        
        // Keyhole 여부 확인 (final_transformed MST의 IsKeyhole 필드 사용)
        val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
        
        // 2. Keyhole 여부에 따라 MST 선택
        // Keyhole 발생 시: keyhole_optimized_final_transformed MST (최적화된 Train≠0 데이터)
        // Keyhole 미발생 시: final_transformed MST (Train=0 데이터)
        val dataType = if (isKeyhole) {
            logger.debug("🔑 패스 ID ${passId}: Keyhole 발생 → keyhole_optimized_final_transformed MST 사용")
            "keyhole_optimized_final_transformed"  // Keyhole이면 최적화 MST 사용
        } else {
            logger.debug("✅ 패스 ID ${passId}: Keyhole 미발생 → final_transformed MST 사용")
            "final_transformed"  // Keyhole 아니면 기본 MST 사용
        }
        
        // 3. 선택된 DataType의 MST 반환
        // ✅ MstId 필드만 사용 (No 필드 제거)
        val selectedMst = ephemerisTrackMstStorage.find {
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            dataMstId == passId && it["DataType"] == dataType  // ✅ 타입 변환 추가 (PassSchedule과 동일)
        }
        
        if (selectedMst == null) {
            logger.error("❌ 패스 ID ${passId}: 선택된 DataType($dataType)의 MST를 찾을 수 없습니다.")
            return null
        }
        
        logger.info("📊 패스 ID ${passId} MST 선택: Keyhole=${if (isKeyhole) "YES" else "NO"}, DataType=${dataType}")
        
        return selectedMst
    }

    /**
     * ✅ 원본 데이터 조회
     */
    fun getOriginalEphemerisTrackMst(): List<Map<String, Any?>> {
        return getEphemerisTrackMstByDataType("original")
    }

    /**
     * ✅ 축변환 데이터 조회
     */
    fun getAxisTransformedEphemerisTrackMst(): List<Map<String, Any?>> {
        return getEphemerisTrackMstByDataType("axis_transformed")
    }

    /**
     * ✅ 최종 변환 데이터 조회 (방위각 변환까지 적용된)
     */
    fun getFinalTransformedEphemerisTrackMst(): List<Map<String, Any?>> {
        return getEphemerisTrackMstByDataType("final_transformed")
    }

    /**
     * 기울기 변환이 적용된 추적 데이터를 저장소에 저장
     */
    fun saveTiltTransformedData(mstData: List<Map<String, Any?>>, dtlData: List<Map<String, Any?>>) {
        synchronized(ephemerisTrackMstStorage) {
            ephemerisTrackMstStorage.addAll(mstData)
        }
        synchronized(ephemerisTrackDtlStorage) {
            ephemerisTrackDtlStorage.addAll(dtlData)
        }
        logger.info("기울기 변환된 추적 데이터 저장 완료: 마스터 ${mstData.size}개, 세부 ${dtlData.size}개")
    }

    /**
     * 변환된 시계열에서 속도와 가속도를 계산하는 헬퍼 함수
     */
    private fun calculateVelocityAndAcceleration(trackingData: List<Map<String, Any?>>): List<Map<String, Any?>> {
        if (trackingData.size < 2) {
            logger.warn("속도/가속도 계산을 위해서는 최소 2개 이상의 데이터 포인트가 필요합니다")
            return trackingData
        }

        val result = mutableListOf<Map<String, Any?>>()

        for (i in trackingData.indices) {
            val currentPoint = trackingData[i]
            val currentAzimuth = currentPoint["Azimuth"] as Double
            val currentElevation = currentPoint["Elevation"] as Double
            val currentTime = currentPoint["Time"] as ZonedDateTime

            var azimuthRate = 0.0
            var elevationRate = 0.0
            var azimuthAccel = 0.0
            var elevationAccel = 0.0

            // 속도 계산 (중앙 차분법 사용)
            when (i) {
                0 -> {
                    // 첫 번째 점: 전진 차분
                    val nextPoint = trackingData[i + 1]
                    val nextAzimuth = nextPoint["Azimuth"] as Double
                    val nextElevation = nextPoint["Elevation"] as Double
                    val nextTime = nextPoint["Time"] as ZonedDateTime

                    val timeDiff = Duration.between(currentTime, nextTime).toMillis() / 1000.0
                    if (timeDiff > 0) {
                        azimuthRate = (nextAzimuth - currentAzimuth) / timeDiff
                        elevationRate = (nextElevation - currentElevation) / timeDiff
                    }
                }

                trackingData.size - 1 -> {
                    // 마지막 점: 후진 차분
                    val prevPoint = trackingData[i - 1]
                    val prevAzimuth = prevPoint["Azimuth"] as Double
                    val prevElevation = prevPoint["Elevation"] as Double
                    val prevTime = prevPoint["Time"] as ZonedDateTime

                    val timeDiff = Duration.between(prevTime, currentTime).toMillis() / 1000.0
                    if (timeDiff > 0) {
                        azimuthRate = (currentAzimuth - prevAzimuth) / timeDiff
                        elevationRate = (currentElevation - prevElevation) / timeDiff
                    }
                }

                else -> {
                    // 중간 점: 중앙 차분
                    val prevPoint = trackingData[i - 1]
                    val nextPoint = trackingData[i + 1]
                    val prevAzimuth = prevPoint["Azimuth"] as Double
                    val nextAzimuth = nextPoint["Azimuth"] as Double
                    val prevElevation = prevPoint["Elevation"] as Double
                    val nextElevation = nextPoint["Elevation"] as Double
                    val prevTime = prevPoint["Time"] as ZonedDateTime
                    val nextTime = nextPoint["Time"] as ZonedDateTime

                    val totalTimeDiff = Duration.between(prevTime, nextTime).toMillis() / 1000.0
                    if (totalTimeDiff > 0) {
                        azimuthRate = (nextAzimuth - prevAzimuth) / totalTimeDiff
                        elevationRate = (nextElevation - prevElevation) / totalTimeDiff
                    }
                }
            }

            // 가속도 계산 (속도의 변화율)
            when (i) {
                0 -> {
                    // 첫 번째 점: 전진 차분
                    if (i + 1 < trackingData.size) {
                        val nextPoint = trackingData[i + 1]
                        val nextTime = nextPoint["Time"] as ZonedDateTime
                        val timeDiff = Duration.between(currentTime, nextTime).toMillis() / 1000.0

                        if (i + 2 < trackingData.size) {
                            val nextNextPoint = trackingData[i + 2]
                            val nextNextAzimuth = nextNextPoint["Azimuth"] as Double
                            val nextNextElevation = nextNextPoint["Elevation"] as Double
                            val nextNextTime = nextNextPoint["Time"] as ZonedDateTime

                            val nextTimeDiff = Duration.between(nextTime, nextNextTime).toMillis() / 1000.0
                            if (timeDiff > 0 && nextTimeDiff > 0) {
                                val nextAzRate = (nextNextAzimuth - nextPoint["Azimuth"] as Double) / nextTimeDiff
                                val nextElRate = (nextNextElevation - nextPoint["Elevation"] as Double) / nextTimeDiff

                                azimuthAccel = (nextAzRate - azimuthRate) / timeDiff
                                elevationAccel = (nextElRate - elevationRate) / timeDiff
                            }
                        }
                    }
                }

                trackingData.size - 1 -> {
                    // 마지막 점: 후진 차분
                    if (i - 1 >= 0) {
                        val prevPoint = trackingData[i - 1]
                        val prevTime = prevPoint["Time"] as ZonedDateTime
                        val timeDiff = Duration.between(prevTime, currentTime).toMillis() / 1000.0

                        if (i - 2 >= 0) {
                            val prevPrevPoint = trackingData[i - 2]
                            val prevPrevAzimuth = prevPrevPoint["Azimuth"] as Double
                            val prevPrevElevation = prevPrevPoint["Elevation"] as Double
                            val prevPrevTime = prevPrevPoint["Time"] as ZonedDateTime

                            val prevTimeDiff = Duration.between(prevPrevTime, prevTime).toMillis() / 1000.0
                            if (timeDiff > 0 && prevTimeDiff > 0) {
                                val prevAzRate = (prevPoint["Azimuth"] as Double - prevPrevAzimuth) / prevTimeDiff
                                val prevElRate = (prevPoint["Elevation"] as Double - prevPrevElevation) / prevTimeDiff

                                azimuthAccel = (azimuthRate - prevAzRate) / timeDiff
                                elevationAccel = (elevationRate - prevElRate) / timeDiff
                            }
                        }
                    }
                }

                else -> {
                    // 중간 점: 중앙 차분
                    val prevPoint = trackingData[i - 1]
                    val nextPoint = trackingData[i + 1]
                    val prevTime = prevPoint["Time"] as ZonedDateTime
                    val nextTime = nextPoint["Time"] as ZonedDateTime

                    val prevTimeDiff = Duration.between(prevTime, currentTime).toMillis() / 1000.0
                    val nextTimeDiff = Duration.between(currentTime, nextTime).toMillis() / 1000.0

                    if (prevTimeDiff > 0 && nextTimeDiff > 0) {
                        val prevAzRate = (currentAzimuth - prevPoint["Azimuth"] as Double) / prevTimeDiff
                        val prevElRate = (currentElevation - prevPoint["Elevation"] as Double) / prevTimeDiff
                        val nextAzRate = (nextPoint["Azimuth"] as Double - currentAzimuth) / nextTimeDiff
                        val nextElRate = (nextPoint["Elevation"] as Double - currentElevation) / nextTimeDiff

                        val avgTimeDiff = (prevTimeDiff + nextTimeDiff) / 2.0
                        azimuthAccel = (nextAzRate - prevAzRate) / avgTimeDiff
                        elevationAccel = (nextElRate - prevElRate) / avgTimeDiff
                    }
                }
            }

            // 결과 데이터 포인트 생성
            val resultPoint = currentPoint.toMutableMap().apply {
                put("AzimuthRate", azimuthRate)
                put("ElevationRate", elevationRate)
                put("AzimuthAccel", azimuthAccel)
                put("ElevationAccel", elevationAccel)
            }

            result.add(resultPoint)
        }

        logger.info("속도/가속도 계산 완료: ${result.size}개 데이터 포인트")
        return result
    }

    /**
     * 위성 ID로부터 위성 이름을 가져오는 헬퍼 함수
     * 실제 애플리케이션에서는 데이터베이스를 조회할 것입니다
     */
    private fun getSatelliteNameFromId(satelliteId: String): String {
        // 이것은 임시 구현입니다 - 실제 애플리케이션에서는 ID를 기반으로
        // 데이터베이스나 다른 소스에서 이름을 조회할 것입니다
        return when (satelliteId) {
            "27424" -> "AQUA"
            "25544" -> "ISS"
            "43013" -> "NOAA-20"
            else -> "Satellite-$satelliteId"
        }
    }

    /**
     * 위성 TLE 데이터를 캐시에 추가합니다.
     */
    fun addSatelliteTle(satelliteId: String, tleLine1: String, tleLine2: String) {
        satelliteTleCache[satelliteId] = Pair(tleLine1, tleLine2)
        logger.info("위성 TLE 데이터가 캐시에 추가되었습니다. 위성 ID: $satelliteId")
    }

    /**
     * 위성 TLE 데이터를 캐시에서 가져옵니다.
     */
    fun getSatelliteTle(satelliteId: String): Pair<String, String>? {
        return satelliteTleCache[satelliteId]
    }

    /**
     * 위성 TLE 데이터를 캐시에서 삭제합니다.
     */
    fun removeSatelliteTle(satelliteId: String) {
        satelliteTleCache.remove(satelliteId)
        logger.info("위성 TLE 데이터가 캐시에서 삭제되었습니다. 위성 ID: $satelliteId")
    }

    /**
     * 캐시된 모든 위성 ID 목록을 반환합니다.
     */
    fun getAllSatelliteIds(): List<String> {
        return satelliteTleCache.keys.toList()
    }

    /**
     * 3축 변환 계산 (단일 좌표)
     * 입력된 Azimuth, Elevation을 Tilt, Rotator 각도로 변환
     */
    fun calculateAxisTransform(
        azimuth: Double,
        elevation: Double,
        tilt: Double,
        rotator: Double
    ): Map<String, Any> {
        try {
            logger.info("3축 변환 계산 시작")
            logger.info("입력 좌표: Az=${String.format("%.6f", azimuth)}°, El=${String.format("%.6f", elevation)}°")
            logger.info("변환 파라미터: Tilt=${String.format("%.6f", tilt)}°, Rotator=${String.format("%.6f", rotator)}°")

            // 입력값 검증 (Elevation 범위를 0~180도로 수정)
            if (azimuth < 0 || azimuth > 360) {
                throw IllegalArgumentException("Azimuth는 0-360도 범위여야 합니다: $azimuth")
            }
            if (elevation < 0 || elevation > 180) {
                throw IllegalArgumentException("Elevation은 0-180도 범위여야 합니다: $elevation")
            }
            if (tilt < -90 || tilt > 90) {
                throw IllegalArgumentException("Tilt는 -90-90도 범위여야 합니다: $tilt")
            }
            if (rotator < 0 || rotator > 360) {
                throw IllegalArgumentException("Rotator는 0-360도 범위여야 합니다: $rotator")
            }

            // 3축 변환 계산
            val (transformedAz, transformedEl) = CoordinateTransformer.transformCoordinatesWithTrain(
                azimuth, elevation, tilt, rotator
            )

            logger.info("3축 변환 계산 완료")
            logger.info(
                "변환 결과: Az=${String.format("%.6f", transformedAz)}°, El=${
                    String.format(
                        "%.6f",
                        transformedEl
                    )
                }°"
            )

            return mapOf(
                "success" to true,
                "input" to mapOf(
                    "azimuth" to azimuth,
                    "elevation" to elevation,
                    "tilt" to tilt,
                    "rotator" to rotator
                ),
                "output" to mapOf(
                    "azimuth" to transformedAz,
                    "elevation" to transformedEl
                ),
                "message" to "3축 변환 계산이 완료되었습니다"
            )

        } catch (error: Exception) {
            logger.error("3축 변환 계산 실패: ${error.message}")
            return mapOf(
                "success" to false,
                "error" to (error.message ?: "알 수 없는 오류"),
                "message" to "3축 변환 계산에 실패했습니다"
            )
        }
    }

    /**
     * 📊 Original과 Final Transformed 데이터를 모두 반환하는 API
     * UI에서 비교 표시를 위해 사용
     */
    fun getAllEphemerisTrackMstWithComparison(): Map<String, Any?> {
        try {
            logger.info("📊 Original과 Final Transformed 데이터 비교 정보 조회 시작")
            
            val originalMst = getAllEphemerisTrackMst().filter { it["DataType"] == "original" }
            val finalTransformedMst = getAllEphemerisTrackMst().filter { it["DataType"] == "final_transformed" }
            
            if (originalMst.isEmpty() || finalTransformedMst.isEmpty()) {
                logger.warn("⚠️ 비교할 데이터가 없습니다")
                return mapOf(
                    "success" to false,
                    "error" to "비교할 데이터가 없습니다",
                    "originalMst" to emptyList<Map<String, Any?>>(),
                    "finalTransformedMst" to emptyList<Map<String, Any?>>()
                )
            }
            
            logger.info("✅ 비교 데이터 조회 완료: Original ${originalMst.size}개, Final ${finalTransformedMst.size}개")
            
            return mapOf(
                "success" to true,
                "originalMst" to originalMst,
                "finalTransformedMst" to finalTransformedMst,
                "message" to "비교 데이터 조회 완료"
            )
            
        } catch (error: Exception) {
            logger.error("❌ 비교 데이터 조회 실패: ${error.message}")
            return mapOf(
                "success" to false,
                "error" to (error.message ?: "알 수 없는 오류"),
                "originalMst" to emptyList<Map<String, Any?>>(),
                "finalTransformedMst" to emptyList<Map<String, Any?>>()
            )
        }
    }

    /**
     * 📊 모든 MST ID에 대해 CSV 파일 생성
     * 원본, 축변환, 최종 변환 데이터를 매칭하여 CSV 파일로 추출
     */
    fun exportAllMstDataToCsv(outputDirectory: String = "csv_exports"): Map<String, Any?> {
        try {
            logger.info("📊 모든 MST 데이터 CSV 파일 생성 시작")
            val outputDir = java.io.File(outputDirectory)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
                logger.info("📁 출력 디렉토리 생성: $outputDirectory")
            }
            // ✅ 중복 방지: original 데이터만 사용 (11개 스케줄)
            logger.info("🔍 디버그: ephemerisTrackMstStorage 총 개수: ${ephemerisTrackMstStorage.size}")
            logger.info("🔍 디버그: original 데이터 개수: ${ephemerisTrackMstStorage.filter { it["DataType"] == "original" }.size}")
            val allMstIds = ephemerisTrackMstStorage.filter { it["DataType"] == "original" }.map { (it["No"] as? Number)?.toLong() ?: 0L }.sorted()  // ✅ UInt → Long 변경
            if (allMstIds.isEmpty()) {
                logger.warn("⚠️ 추출할 MST 데이터가 없습니다")
                return mapOf<String, Any?>("success" to false, "error" to "추출할 데이터가 없습니다")
            }
            logger.info("총 ${allMstIds.size}개의 MST ID 발견")
            var successCount = 0
            var errorCount = 0
            val createdFiles = mutableListOf<String>()
            allMstIds.forEach { mstId ->
                try {
                    // ✅ MST에서 DetailId 가져오기
                    val mstInfo = ephemerisTrackMstStorage.filter { it["DataType"] == "original" }
                        .find { (it["MstId"] as? Number)?.toLong() == mstId }
                    val detailId = (mstInfo?.get("DetailId") as? Number)?.toInt() ?: 0
                    
                    val result = exportMstDataToCsv(mstId.toInt(), detailId, outputDirectory)  // ✅ detailId 전달
                    if (result["success"] == true) {
                        successCount++
                        createdFiles.add(result["filename"] as String)
                        logger.info("✅ MST ID $mstId CSV 파일 생성 완료")
                    } else {
                        errorCount++
                        logger.error("❌ MST ID $mstId CSV 파일 생성 실패: ${result["error"]}")
                    }
                } catch (e: Exception) {
                    errorCount++
                    logger.error("❌ MST ID $mstId CSV 파일 생성 중 오류: ${e.message}", e)
                }
            }
            logger.info("📊 CSV 파일 생성 완료:")
            logger.info("  - 성공: $successCount 개")
            logger.info("  - 실패: $errorCount 개")
            logger.info("  - 생성된 파일: ${createdFiles.joinToString(", ")}")
            return mapOf<String, Any?>(
                "success" to true,
                "totalMstCount" to allMstIds.size,
                "successCount" to successCount,
                "errorCount" to errorCount,
                "createdFiles" to createdFiles,
                "outputDirectory" to outputDirectory
            )
        } catch (e: Exception) {
            logger.error("❌ CSV 파일 생성 중 오류: ${e.message}", e)
            return mapOf<String, Any?>(
                "success" to false,
                "error" to e.message
            )
        }
    }

    /**
     * 📊 모든 MST 데이터를 하나의 통합된 CSV 파일로 생성
     * 사용자 요구사항: 하나의 파일로 모든 데이터 통합
     */
    fun exportAllMstDataToSingleCsv(outputDirectory: String = "csv_exports"): Map<String, Any?> {
        try {
            logger.info("📊 모든 MST 데이터를 하나의 통합 CSV 파일로 생성 시작")
            val outputDir = java.io.File(outputDirectory)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
                logger.info("📁 출력 디렉토리 생성: $outputDirectory")
            }
            
            val allMstIds = getAllEphemerisTrackMst().map { (it["No"] as? Number)?.toLong() ?: 0L }  // ✅ UInt → Long 변경
            if (allMstIds.isEmpty()) {
                logger.warn("⚠️ 추출할 MST 데이터가 없습니다")
                return mapOf<String, Any?>("success" to false, "error" to "추출할 데이터가 없습니다")
            }
            
            logger.info("총 ${allMstIds.size}개의 MST ID 발견 - 통합 CSV 파일 생성")
            
            // 통합 CSV 파일명 생성
            val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val filename = "All_MST_Data_${timestamp}.csv"
            val filePath = "$outputDirectory/$filename"
            
            // 기존 파일 확인 및 덮어쓰기 로그
            val file = java.io.File(filePath)
            if (file.exists()) {
                logger.info("🔄 기존 파일 덮어쓰기: $filename")
            } else {
                logger.info("📄 새 파일 생성: $filename")
            }
            
            var totalRows = 0
            var processedMstCount = 0
            
            java.io.FileWriter(filePath).use { writer ->
                // CSV 헤더 작성
                writer.write("MST_ID,Satellite_Name,Index,Time,")
                writer.write("Original_Azimuth,Original_Elevation,Original_Azimuth_Velocity,Original_Elevation_Velocity,")
                writer.write("Original_Range,Original_Altitude,")
                writer.write("AxisTransformed_Azimuth,AxisTransformed_Elevation,AxisTransformed_Azimuth_Velocity,AxisTransformed_Elevation_Velocity,")
                writer.write("FinalTransformed_Azimuth,FinalTransformed_Elevation,FinalTransformed_Azimuth_Velocity,FinalTransformed_Elevation_Velocity,")
                writer.write("Azimuth_Transformation_Error,Elevation_Transformation_Error\n")
                
                allMstIds.forEach { mstId ->
                    try {
                        // ✅ MST에서 DetailId 가져오기
                        val mstInfo = getAllEphemerisTrackMst().find { 
                            (it["MstId"] as? Number)?.toLong() == mstId && it["DataType"] == "final_transformed"
                        }
                        val detailId = (mstInfo?.get("DetailId") as? Number)?.toInt() ?: 0
                        
                        // ✅ detailId 전달하여 조회
                        val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "original", detailId)  // ✅ detailId 전달
                        val axisTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "axis_transformed", detailId)  // ✅ detailId 전달
                        val finalTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "final_transformed", detailId)  // ✅ detailId 전달
                        
                        if (originalDtl.isEmpty()) {
                            logger.warn("⚠️ MST ID $mstId 의 원본 데이터를 찾을 수 없습니다")
                            return@forEach
                        }
                        
                        // ✅ 위성 이름 가져오기 (original 타입 MST에서 조회)
                        val originalMstInfo = getAllEphemerisTrackMst().find { 
                            (it["MstId"] as? Number)?.toLong() == mstId && it["DataType"] == "original"
                        }
                        val satelliteName = originalMstInfo?.get("SatelliteName") as? String ?: "Unknown"
                        
                        val maxSize = maxOf(originalDtl.size, axisTransformedDtl.size, finalTransformedDtl.size)
                        
                        // 각 변환 단계별 각속도 계산을 위한 이전 값 저장
                        var prevOriginalAzimuth: Double? = null
                        var prevOriginalElevation: Double? = null
                        var prevAxisTransformedAzimuth: Double? = null
                        var prevAxisTransformedElevation: Double? = null
                        var prevFinalTransformedAzimuth: Double? = null
                        var prevFinalTransformedElevation: Double? = null
                        var prevTime: java.time.ZonedDateTime? = null
                        
                        for (i in 0 until maxSize) {
                            val originalPoint = if (i < originalDtl.size) originalDtl[i] else null
                            val axisTransformedPoint = if (i < axisTransformedDtl.size) axisTransformedDtl[i] else null
                            val finalTransformedPoint = if (i < finalTransformedDtl.size) finalTransformedDtl[i] else null
                            
                            val originalTime = originalPoint?.get("Time") as? java.time.ZonedDateTime
                            val originalAz = originalPoint?.get("Azimuth") as? Double ?: 0.0
                            val originalEl = originalPoint?.get("Elevation") as? Double ?: 0.0
                            val originalRange = originalPoint?.get("Range") as? Double ?: 0.0
                            val originalAltitude = originalPoint?.get("Altitude") as? Double ?: 0.0
                            
                            val axisTransformedAz = axisTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                            val axisTransformedEl = axisTransformedPoint?.get("Elevation") as? Double ?: 0.0
                            
                            val finalTransformedAz = finalTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                            val finalTransformedEl = finalTransformedPoint?.get("Elevation") as? Double ?: 0.0
                            
                            // 각 변환 단계별 각속도 계산 (이론치 합계법 - 10개 변화량의 합)
                            var originalAzimuthVelocity = 0.0
                            var originalElevationVelocity = 0.0
                            var axisTransformedAzimuthVelocity = 0.0
                            var axisTransformedElevationVelocity = 0.0
                            var finalTransformedAzimuthVelocity = 0.0
                            var finalTransformedElevationVelocity = 0.0
                            
                            // 이론치 합계법: 1초간(10개) 총 변화량 계산 (시간으로 나누지 않음)
                            if (i >= 9) { // Index 9부터 10개 데이터 구간 형성 가능
                                var currentOriginalAzSum = 0.0
                                var currentOriginalElSum = 0.0
                                var currentAxisTransformedAzSum = 0.0
                                var currentAxisTransformedElSum = 0.0
                                var currentFinalTransformedAzSum = 0.0
                                var currentFinalTransformedElSum = 0.0
                                
                                // 10개 구간의 변화량을 모두 더함 (j-1이 유효하도록)
                                for (j in (i - 9)..i) { // j는 현재 인덱스 i까지, 이전 9개 포함 (총 10개)
                                    if (j > 0) { // j-1이 유효한 경우만 계산
                                        val prevOriginalPoint = originalDtl[j - 1]
                                        val currentOriginalPoint = originalDtl[j]
                                        val prevAxisTransformedPoint = axisTransformedDtl[j - 1]
                                        val currentAxisTransformedPoint = axisTransformedDtl[j]
                                        val prevFinalTransformedPoint = finalTransformedDtl[j - 1]
                                        val currentFinalTransformedPoint = finalTransformedDtl[j]
                                        
                                        // Original
                                        val prevOriginalAz = prevOriginalPoint["Azimuth"] as Double
                                        val currentOriginalAz = currentOriginalPoint["Azimuth"] as Double
                                        val prevOriginalEl = prevOriginalPoint["Elevation"] as Double
                                        val currentOriginalEl = currentOriginalPoint["Elevation"] as Double
                                        var azDiffOriginal = currentOriginalAz - prevOriginalAz
                                        if (azDiffOriginal > 180) azDiffOriginal -= 360
                                        if (azDiffOriginal < -180) azDiffOriginal += 360
                                        currentOriginalAzSum += kotlin.math.abs(azDiffOriginal)
                                        currentOriginalElSum += kotlin.math.abs(currentOriginalEl - prevOriginalEl)
                                        
                                        // AxisTransformed
                                        val prevAxisTransformedAz = prevAxisTransformedPoint["Azimuth"] as Double
                                        val currentAxisTransformedAz = currentAxisTransformedPoint["Azimuth"] as Double
                                        val prevAxisTransformedEl = prevAxisTransformedPoint["Elevation"] as Double
                                        val currentAxisTransformedEl = currentAxisTransformedPoint["Elevation"] as Double
                                        var azDiffAxis = currentAxisTransformedAz - prevAxisTransformedAz
                                        if (azDiffAxis > 180) azDiffAxis -= 360
                                        if (azDiffAxis < -180) azDiffAxis += 360
                                        currentAxisTransformedAzSum += kotlin.math.abs(azDiffAxis)
                                        currentAxisTransformedElSum += kotlin.math.abs(currentAxisTransformedEl - prevAxisTransformedEl)
                                        
                                        // FinalTransformed
                                        val prevFinalTransformedAz = prevFinalTransformedPoint["Azimuth"] as Double
                                        val currentFinalTransformedAz = currentFinalTransformedPoint["Azimuth"] as Double
                                        val prevFinalTransformedEl = prevFinalTransformedPoint["Elevation"] as Double
                                        val currentFinalTransformedEl = currentFinalTransformedPoint["Elevation"] as Double
                                        var azDiffFinal = currentFinalTransformedAz - prevFinalTransformedAz
                                        if (azDiffFinal > 180) azDiffFinal -= 360
                                        if (azDiffFinal < -180) azDiffFinal += 360
                                        currentFinalTransformedAzSum += kotlin.math.abs(azDiffFinal)
                                        currentFinalTransformedElSum += kotlin.math.abs(currentFinalTransformedEl - prevFinalTransformedEl)
                                    }
                                }
                                
                                originalAzimuthVelocity = currentOriginalAzSum
                                originalElevationVelocity = currentOriginalElSum
                                axisTransformedAzimuthVelocity = currentAxisTransformedAzSum
                                axisTransformedElevationVelocity = currentAxisTransformedElSum
                                finalTransformedAzimuthVelocity = currentFinalTransformedAzSum
                                finalTransformedElevationVelocity = currentFinalTransformedElSum
                            }
                            
                            // 변환 오차 계산
                            val azimuthTransformationError = finalTransformedAz - originalAz
                            val elevationTransformationError = finalTransformedEl - originalEl
                            
                            val timeString = originalTime?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) ?: ""
                            
                            // 통합 CSV 데이터 출력
                            writer.write("$mstId,$satelliteName,$i,$timeString,")
                            writer.write("${String.format("%.6f", originalAz)},${String.format("%.6f", originalEl)},${String.format("%.6f", originalAzimuthVelocity)},${String.format("%.6f", originalElevationVelocity)},")
                            writer.write("${String.format("%.6f", originalRange)},${String.format("%.6f", originalAltitude)},")
                            writer.write("${String.format("%.6f", axisTransformedAz)},${String.format("%.6f", axisTransformedEl)},${String.format("%.6f", axisTransformedAzimuthVelocity)},${String.format("%.6f", axisTransformedElevationVelocity)},")
                            writer.write("${String.format("%.6f", finalTransformedAz)},${String.format("%.6f", finalTransformedEl)},${String.format("%.6f", finalTransformedAzimuthVelocity)},${String.format("%.6f", finalTransformedElevationVelocity)},")
                            writer.write("${String.format("%.6f", azimuthTransformationError)},${String.format("%.6f", elevationTransformationError)}\n")
                            
                            totalRows++
                            
                            // 다음 반복을 위한 값 저장
                            prevOriginalAzimuth = originalAz
                            prevOriginalElevation = originalEl
                            prevAxisTransformedAzimuth = axisTransformedAz
                            prevAxisTransformedElevation = axisTransformedEl
                            prevFinalTransformedAzimuth = finalTransformedAz
                            prevFinalTransformedElevation = finalTransformedEl
                            prevTime = originalTime
                        }
                        
                        processedMstCount++
                        logger.info("✅ MST ID $mstId 데이터 처리 완료 (${maxSize}개 행)")
                        
                    } catch (e: Exception) {
                        logger.error("❌ MST ID $mstId 처리 중 오류: ${e.message}", e)
                    }
                }
            }
            
            logger.info("📊 통합 CSV 파일 생성 완료: $filePath")
            logger.info("  - 처리된 MST: $processedMstCount 개")
            logger.info("  - 총 데이터 행: $totalRows 개")
            
            return mapOf<String, Any?>(
                "success" to true,
                "filename" to filename,
                "filePath" to filePath,
                "totalMstCount" to allMstIds.size,
                "processedMstCount" to processedMstCount,
                "totalRows" to totalRows,
                "outputDirectory" to outputDirectory
            )
            
        } catch (e: Exception) {
            logger.error("❌ 통합 CSV 파일 생성 중 오류: ${e.message}", e)
            return mapOf<String, Any?>(
                "success" to false,
                "error" to e.message
            )
        }
    }

    /**
     * 📊 특정 MST 데이터를 CSV 문자열로 생성 (브라우저 다운로드용)
     * 선택된 스케줄의 MST ID만 처리하여 빠른 응답
     */
    fun generateMstDataCsvContent(mstId: Long, detailId: Int? = null): String? {
        try {
            logger.info("📊 MST ID $mstId 데이터를 CSV 문자열로 생성 시작")

            // ✅ original 데이터에서 MST 정보 조회
            val originalMstInfo = ephemerisTrackMstStorage.find {
                (it["MstId"] as? Number)?.toLong() == mstId && it["DataType"] == "original"
            }

            if (originalMstInfo == null) {
                logger.warn("⚠️ MST ID $mstId 의 original MST 정보를 찾을 수 없습니다")
                return null
            }

            val actualDetailId = detailId ?: ((originalMstInfo["DetailId"] as? Number)?.toInt() ?: 0)
            val satelliteName = originalMstInfo["SatelliteName"] as? String ?: "Unknown"

            logger.info("📊 MST ID $mstId 처리: DetailId=$actualDetailId, SatelliteName=$satelliteName")

            val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "original", actualDetailId)
            val axisTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "axis_transformed", actualDetailId)
            val finalTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "final_transformed", actualDetailId)

            if (originalDtl.isEmpty()) {
                logger.warn("⚠️ MST ID $mstId 의 원본 DTL 데이터를 찾을 수 없습니다 (DetailId=$actualDetailId)")
                return null
            }

            logger.info("📊 MST ID $mstId: Original=${originalDtl.size}, AxisTransformed=${axisTransformedDtl.size}, FinalTransformed=${finalTransformedDtl.size}")

            val csvBuilder = StringBuilder()

            // CSV 헤더 작성 (한국 시간 컬럼 추가)
            csvBuilder.append("Index,Time_UTC,Time_KST,")
            csvBuilder.append("Original_Azimuth,Original_Elevation,Original_Azimuth_Velocity,Original_Elevation_Velocity,")
            csvBuilder.append("Original_Range,Original_Altitude,")
            csvBuilder.append("AxisTransformed_Azimuth,AxisTransformed_Elevation,AxisTransformed_Azimuth_Velocity,AxisTransformed_Elevation_Velocity,")
            csvBuilder.append("FinalTransformed_Azimuth,FinalTransformed_Elevation,FinalTransformed_Azimuth_Velocity,FinalTransformed_Elevation_Velocity,")
            csvBuilder.append("Azimuth_Transformation_Error,Elevation_Transformation_Error\n")

            val maxSize = maxOf(originalDtl.size, axisTransformedDtl.size, finalTransformedDtl.size)

            for (i in 0 until maxSize) {
                val originalPoint = if (i < originalDtl.size) originalDtl[i] else null
                val axisTransformedPoint = if (i < axisTransformedDtl.size) axisTransformedDtl[i] else null
                val finalTransformedPoint = if (i < finalTransformedDtl.size) finalTransformedDtl[i] else null

                val originalTime = originalPoint?.get("Time") as? java.time.ZonedDateTime
                val originalAz = originalPoint?.get("Azimuth") as? Double ?: 0.0
                val originalEl = originalPoint?.get("Elevation") as? Double ?: 0.0
                val originalRange = originalPoint?.get("Range") as? Double ?: 0.0
                val originalAltitude = originalPoint?.get("Altitude") as? Double ?: 0.0

                val axisTransformedAz = axisTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                val axisTransformedEl = axisTransformedPoint?.get("Elevation") as? Double ?: 0.0

                val finalTransformedAz = finalTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                val finalTransformedEl = finalTransformedPoint?.get("Elevation") as? Double ?: 0.0

                // 각 변환 단계별 각속도 계산
                var originalAzimuthVelocity = 0.0
                var originalElevationVelocity = 0.0
                var axisTransformedAzimuthVelocity = 0.0
                var axisTransformedElevationVelocity = 0.0
                var finalTransformedAzimuthVelocity = 0.0
                var finalTransformedElevationVelocity = 0.0

                if (i >= 9 && axisTransformedDtl.isNotEmpty() && finalTransformedDtl.isNotEmpty()) {
                    var currentOriginalAzSum = 0.0
                    var currentOriginalElSum = 0.0
                    var currentAxisTransformedAzSum = 0.0
                    var currentAxisTransformedElSum = 0.0
                    var currentFinalTransformedAzSum = 0.0
                    var currentFinalTransformedElSum = 0.0

                    for (j in (i - 9)..i) {
                        if (j > 0 && j < originalDtl.size && j < axisTransformedDtl.size && j < finalTransformedDtl.size) {
                            val prevOriginalAz = originalDtl[j - 1]["Azimuth"] as? Double ?: 0.0
                            val currentOriginalAz = originalDtl[j]["Azimuth"] as? Double ?: 0.0
                            val prevOriginalEl = originalDtl[j - 1]["Elevation"] as? Double ?: 0.0
                            val currentOriginalEl = originalDtl[j]["Elevation"] as? Double ?: 0.0
                            var azDiffOriginal = currentOriginalAz - prevOriginalAz
                            if (azDiffOriginal > 180) azDiffOriginal -= 360
                            if (azDiffOriginal < -180) azDiffOriginal += 360
                            currentOriginalAzSum += kotlin.math.abs(azDiffOriginal)
                            currentOriginalElSum += kotlin.math.abs(currentOriginalEl - prevOriginalEl)

                            val prevAxisAz = axisTransformedDtl[j - 1]["Azimuth"] as? Double ?: 0.0
                            val currentAxisAz = axisTransformedDtl[j]["Azimuth"] as? Double ?: 0.0
                            val prevAxisEl = axisTransformedDtl[j - 1]["Elevation"] as? Double ?: 0.0
                            val currentAxisEl = axisTransformedDtl[j]["Elevation"] as? Double ?: 0.0
                            var azDiffAxis = currentAxisAz - prevAxisAz
                            if (azDiffAxis > 180) azDiffAxis -= 360
                            if (azDiffAxis < -180) azDiffAxis += 360
                            currentAxisTransformedAzSum += kotlin.math.abs(azDiffAxis)
                            currentAxisTransformedElSum += kotlin.math.abs(currentAxisEl - prevAxisEl)

                            val prevFinalAz = finalTransformedDtl[j - 1]["Azimuth"] as? Double ?: 0.0
                            val currentFinalAz = finalTransformedDtl[j]["Azimuth"] as? Double ?: 0.0
                            val prevFinalEl = finalTransformedDtl[j - 1]["Elevation"] as? Double ?: 0.0
                            val currentFinalEl = finalTransformedDtl[j]["Elevation"] as? Double ?: 0.0
                            var azDiffFinal = currentFinalAz - prevFinalAz
                            if (azDiffFinal > 180) azDiffFinal -= 360
                            if (azDiffFinal < -180) azDiffFinal += 360
                            currentFinalTransformedAzSum += kotlin.math.abs(azDiffFinal)
                            currentFinalTransformedElSum += kotlin.math.abs(currentFinalEl - prevFinalEl)
                        }
                    }

                    originalAzimuthVelocity = currentOriginalAzSum
                    originalElevationVelocity = currentOriginalElSum
                    axisTransformedAzimuthVelocity = currentAxisTransformedAzSum
                    axisTransformedElevationVelocity = currentAxisTransformedElSum
                    finalTransformedAzimuthVelocity = currentFinalTransformedAzSum
                    finalTransformedElevationVelocity = currentFinalTransformedElSum
                }

                val azimuthTransformationError = finalTransformedAz - originalAz
                val elevationTransformationError = finalTransformedEl - originalEl

                // UTC 시간 포맷
                val timeStringUtc = originalTime?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) ?: ""
                // KST 시간 포맷 (UTC+9)
                val timeStringKst = originalTime?.plusHours(9)?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) ?: ""

                csvBuilder.append("$i,$timeStringUtc,$timeStringKst,")
                csvBuilder.append("${String.format("%.6f", originalAz)},${String.format("%.6f", originalEl)},${String.format("%.6f", originalAzimuthVelocity)},${String.format("%.6f", originalElevationVelocity)},")
                csvBuilder.append("${String.format("%.6f", originalRange)},${String.format("%.6f", originalAltitude)},")
                csvBuilder.append("${String.format("%.6f", axisTransformedAz)},${String.format("%.6f", axisTransformedEl)},${String.format("%.6f", axisTransformedAzimuthVelocity)},${String.format("%.6f", axisTransformedElevationVelocity)},")
                csvBuilder.append("${String.format("%.6f", finalTransformedAz)},${String.format("%.6f", finalTransformedEl)},${String.format("%.6f", finalTransformedAzimuthVelocity)},${String.format("%.6f", finalTransformedElevationVelocity)},")
                csvBuilder.append("${String.format("%.6f", azimuthTransformationError)},${String.format("%.6f", elevationTransformationError)}\n")
            }

            logger.info("📊 CSV 문자열 생성 완료: MST ID $mstId, $maxSize 행")
            return csvBuilder.toString()

        } catch (e: Exception) {
            logger.error("❌ MST ID $mstId CSV 문자열 생성 중 오류: ${e.message}", e)
            return null
        }
    }

    /**
     * 📊 모든 MST 데이터를 CSV 문자열로 생성 (브라우저 다운로드용)
     * 파일 저장 없이 CSV 콘텐츠를 문자열로 반환
     */
    fun generateAllMstDataCsvContent(): String? {
        try {
            logger.info("📊 모든 MST 데이터를 CSV 문자열로 생성 시작")

            // ✅ 중복 방지: original 데이터만 사용하여 MstId 추출
            logger.info("🔍 디버그: ephemerisTrackMstStorage 총 개수: ${ephemerisTrackMstStorage.size}")
            val originalMstData = ephemerisTrackMstStorage.filter { it["DataType"] == "original" }
            logger.info("🔍 디버그: original 데이터 개수: ${originalMstData.size}")

            val allMstIds = originalMstData.mapNotNull { (it["MstId"] as? Number)?.toLong() }.distinct().sorted()
            if (allMstIds.isEmpty()) {
                logger.warn("⚠️ 추출할 MST 데이터가 없습니다")
                return null
            }

            logger.info("총 ${allMstIds.size}개의 MST ID 발견 - CSV 문자열 생성: $allMstIds")

            val csvBuilder = StringBuilder()

            // CSV 헤더 작성 (한국 시간 컬럼 추가)
            csvBuilder.append("MST_ID,Satellite_Name,Index,Time_UTC,Time_KST,")
            csvBuilder.append("Original_Azimuth,Original_Elevation,Original_Azimuth_Velocity,Original_Elevation_Velocity,")
            csvBuilder.append("Original_Range,Original_Altitude,")
            csvBuilder.append("AxisTransformed_Azimuth,AxisTransformed_Elevation,AxisTransformed_Azimuth_Velocity,AxisTransformed_Elevation_Velocity,")
            csvBuilder.append("FinalTransformed_Azimuth,FinalTransformed_Elevation,FinalTransformed_Azimuth_Velocity,FinalTransformed_Elevation_Velocity,")
            csvBuilder.append("Azimuth_Transformation_Error,Elevation_Transformation_Error\n")

            var totalRows = 0
            var processedMstCount = 0

            allMstIds.forEach { mstId ->
                try {
                    // ✅ original 데이터에서 MST 정보 조회
                    val originalMstInfo = ephemerisTrackMstStorage.find {
                        (it["MstId"] as? Number)?.toLong() == mstId && it["DataType"] == "original"
                    }

                    if (originalMstInfo == null) {
                        logger.warn("⚠️ MST ID $mstId 의 original MST 정보를 찾을 수 없습니다")
                        return@forEach
                    }

                    val detailId = (originalMstInfo["DetailId"] as? Number)?.toInt() ?: 0
                    val satelliteName = originalMstInfo["SatelliteName"] as? String ?: "Unknown"

                    logger.info("📊 MST ID $mstId 처리 중: DetailId=$detailId, SatelliteName=$satelliteName")

                    val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "original", detailId)
                    val axisTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "axis_transformed", detailId)
                    val finalTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId, "final_transformed", detailId)

                    if (originalDtl.isEmpty()) {
                        logger.warn("⚠️ MST ID $mstId 의 원본 DTL 데이터를 찾을 수 없습니다 (DetailId=$detailId)")
                        return@forEach
                    }

                    logger.info("📊 MST ID $mstId: Original=${originalDtl.size}, AxisTransformed=${axisTransformedDtl.size}, FinalTransformed=${finalTransformedDtl.size}")

                    val maxSize = maxOf(originalDtl.size, axisTransformedDtl.size, finalTransformedDtl.size)

                    for (i in 0 until maxSize) {
                        val originalPoint = if (i < originalDtl.size) originalDtl[i] else null
                        val axisTransformedPoint = if (i < axisTransformedDtl.size) axisTransformedDtl[i] else null
                        val finalTransformedPoint = if (i < finalTransformedDtl.size) finalTransformedDtl[i] else null

                        val originalTime = originalPoint?.get("Time") as? java.time.ZonedDateTime
                        val originalAz = originalPoint?.get("Azimuth") as? Double ?: 0.0
                        val originalEl = originalPoint?.get("Elevation") as? Double ?: 0.0
                        val originalRange = originalPoint?.get("Range") as? Double ?: 0.0
                        val originalAltitude = originalPoint?.get("Altitude") as? Double ?: 0.0

                        val axisTransformedAz = axisTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                        val axisTransformedEl = axisTransformedPoint?.get("Elevation") as? Double ?: 0.0

                        val finalTransformedAz = finalTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                        val finalTransformedEl = finalTransformedPoint?.get("Elevation") as? Double ?: 0.0

                        // 각 변환 단계별 각속도 계산
                        var originalAzimuthVelocity = 0.0
                        var originalElevationVelocity = 0.0
                        var axisTransformedAzimuthVelocity = 0.0
                        var axisTransformedElevationVelocity = 0.0
                        var finalTransformedAzimuthVelocity = 0.0
                        var finalTransformedElevationVelocity = 0.0

                        if (i >= 9) {
                            var currentOriginalAzSum = 0.0
                            var currentOriginalElSum = 0.0
                            var currentAxisTransformedAzSum = 0.0
                            var currentAxisTransformedElSum = 0.0
                            var currentFinalTransformedAzSum = 0.0
                            var currentFinalTransformedElSum = 0.0

                            for (j in (i - 9)..i) {
                                if (j > 0 && j < originalDtl.size && j < axisTransformedDtl.size && j < finalTransformedDtl.size) {
                                    val prevOriginalPoint = originalDtl[j - 1]
                                    val currentOriginalPoint = originalDtl[j]
                                    val prevAxisTransformedPoint = axisTransformedDtl[j - 1]
                                    val currentAxisTransformedPoint = axisTransformedDtl[j]
                                    val prevFinalTransformedPoint = finalTransformedDtl[j - 1]
                                    val currentFinalTransformedPoint = finalTransformedDtl[j]

                                    val prevOriginalAz = prevOriginalPoint["Azimuth"] as? Double ?: 0.0
                                    val currentOriginalAz = currentOriginalPoint["Azimuth"] as? Double ?: 0.0
                                    val prevOriginalEl = prevOriginalPoint["Elevation"] as? Double ?: 0.0
                                    val currentOriginalEl = currentOriginalPoint["Elevation"] as? Double ?: 0.0
                                    var azDiffOriginal = currentOriginalAz - prevOriginalAz
                                    if (azDiffOriginal > 180) azDiffOriginal -= 360
                                    if (azDiffOriginal < -180) azDiffOriginal += 360
                                    currentOriginalAzSum += kotlin.math.abs(azDiffOriginal)
                                    currentOriginalElSum += kotlin.math.abs(currentOriginalEl - prevOriginalEl)

                                    val prevAxisTransformedAz = prevAxisTransformedPoint["Azimuth"] as? Double ?: 0.0
                                    val currentAxisTransformedAz = currentAxisTransformedPoint["Azimuth"] as? Double ?: 0.0
                                    val prevAxisTransformedEl = prevAxisTransformedPoint["Elevation"] as? Double ?: 0.0
                                    val currentAxisTransformedEl = currentAxisTransformedPoint["Elevation"] as? Double ?: 0.0
                                    var azDiffAxis = currentAxisTransformedAz - prevAxisTransformedAz
                                    if (azDiffAxis > 180) azDiffAxis -= 360
                                    if (azDiffAxis < -180) azDiffAxis += 360
                                    currentAxisTransformedAzSum += kotlin.math.abs(azDiffAxis)
                                    currentAxisTransformedElSum += kotlin.math.abs(currentAxisTransformedEl - prevAxisTransformedEl)

                                    val prevFinalTransformedAz = prevFinalTransformedPoint["Azimuth"] as? Double ?: 0.0
                                    val currentFinalTransformedAz = currentFinalTransformedPoint["Azimuth"] as? Double ?: 0.0
                                    val prevFinalTransformedEl = prevFinalTransformedPoint["Elevation"] as? Double ?: 0.0
                                    val currentFinalTransformedEl = currentFinalTransformedPoint["Elevation"] as? Double ?: 0.0
                                    var azDiffFinal = currentFinalTransformedAz - prevFinalTransformedAz
                                    if (azDiffFinal > 180) azDiffFinal -= 360
                                    if (azDiffFinal < -180) azDiffFinal += 360
                                    currentFinalTransformedAzSum += kotlin.math.abs(azDiffFinal)
                                    currentFinalTransformedElSum += kotlin.math.abs(currentFinalTransformedEl - prevFinalTransformedEl)
                                }
                            }

                            originalAzimuthVelocity = currentOriginalAzSum
                            originalElevationVelocity = currentOriginalElSum
                            axisTransformedAzimuthVelocity = currentAxisTransformedAzSum
                            axisTransformedElevationVelocity = currentAxisTransformedElSum
                            finalTransformedAzimuthVelocity = currentFinalTransformedAzSum
                            finalTransformedElevationVelocity = currentFinalTransformedElSum
                        }

                        val azimuthTransformationError = finalTransformedAz - originalAz
                        val elevationTransformationError = finalTransformedEl - originalEl

                        // UTC 시간 포맷
                        val timeStringUtc = originalTime?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) ?: ""

                        // KST 시간 포맷 (UTC+9)
                        val timeStringKst = originalTime?.plusHours(9)?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) ?: ""

                        csvBuilder.append("$mstId,$satelliteName,$i,$timeStringUtc,$timeStringKst,")
                        csvBuilder.append("${String.format("%.6f", originalAz)},${String.format("%.6f", originalEl)},${String.format("%.6f", originalAzimuthVelocity)},${String.format("%.6f", originalElevationVelocity)},")
                        csvBuilder.append("${String.format("%.6f", originalRange)},${String.format("%.6f", originalAltitude)},")
                        csvBuilder.append("${String.format("%.6f", axisTransformedAz)},${String.format("%.6f", axisTransformedEl)},${String.format("%.6f", axisTransformedAzimuthVelocity)},${String.format("%.6f", axisTransformedElevationVelocity)},")
                        csvBuilder.append("${String.format("%.6f", finalTransformedAz)},${String.format("%.6f", finalTransformedEl)},${String.format("%.6f", finalTransformedAzimuthVelocity)},${String.format("%.6f", finalTransformedElevationVelocity)},")
                        csvBuilder.append("${String.format("%.6f", azimuthTransformationError)},${String.format("%.6f", elevationTransformationError)}\n")

                        totalRows++
                    }

                    processedMstCount++
                    logger.info("✅ MST ID $mstId 데이터 처리 완료")

                } catch (e: Exception) {
                    logger.error("❌ MST ID $mstId 처리 중 오류: ${e.message}", e)
                }
            }

            logger.info("📊 CSV 문자열 생성 완료: $processedMstCount MST, $totalRows 행")
            return csvBuilder.toString()

        } catch (e: Exception) {
            logger.error("❌ CSV 문자열 생성 중 오류: ${e.message}", e)
            return null
        }
    }

    /**
     * ✅ MST 데이터를 CSV 파일로 내보내기 (개선된 버전 - 필터링 + Keyhole 대응)
     * 
     * ✅ displayMinElevationAngle 기준으로 필터링:
     *    - sourceMinElevationAngle = -20도로 넓게 추적했지만
     *    - 이론치 다운로드 CSV에는 displayMinElevationAngle = 0도 이상만 포함
     *    - 실제 추적 명령과 일치하는 데이터 제공
     * 
     * ✅ Keyhole 여부에 따라 적절한 DataType 사용:
     *    - Keyhole 발생: keyhole_final_transformed (Train≠0, ±270°)
     *    - Keyhole 미발생: final_transformed (Train=0, ±270°)
     * 
     * ✅ mstId와 detailId를 사용하여 조회 (PassSchedule과 동일한 구조)
     * 
     * @param mstId 마스터 ID
     * @param detailId 패스 인덱스 (기본값: 0, MST에서 자동 조회)
     * @param outputDirectory 출력 디렉토리
     * @return CSV 파일 생성 결과
     */
    fun exportMstDataToCsv(mstId: Int, detailId: Int? = null, outputDirectory: String = "csv_exports"): Map<String, Any?> {
        try {
            logger.info("📊 MST ID ${mstId} CSV 파일 생성 시작")
            
            // ✅ MST 정보 조회 및 Keyhole 여부 확인
            val finalMst = getAllEphemerisTrackMst().find { 
                (it["MstId"] as? Number)?.toLong() == mstId.toLong() && it["DataType"] == "final_transformed"  // ✅ MstId 필드 사용
            }
            
            if (finalMst == null) {
                logger.error("❌ MST ID ${mstId}에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
                return mapOf<String, Any?>("success" to false, "error" to "MST 데이터를 찾을 수 없습니다")
            }
            
            // ✅ MST에서 DetailId 가져오기 (파라미터보다 우선)
            val actualDetailId = detailId ?: ((finalMst["DetailId"] as? Number)?.toInt() ?: 0)
            logger.info("📊 CSV 생성: mstId=${mstId}, detailId=${actualDetailId} (파라미터=${detailId}, MST DetailId=${finalMst["DetailId"]})")
            
            val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
            
            // ✅ Keyhole 여부에 따라 DataType 선택 (keyhole_optimized_final_transformed 사용)
            val finalDataType = if (isKeyhole) {
                val keyholeDataExists = ephemerisTrackDtlStorage.any {
                    val dataMstId = (it["MstId"] as? Number)?.toLong()
                    val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
                    dataMstId == mstId.toLong() && dataDetailId == actualDetailId && it["DataType"] == "keyhole_optimized_final_transformed"
                }
                if (!keyholeDataExists) {
                    logger.warn("⚠️ MST ID ${mstId}, DetailId=${actualDetailId}: Keyhole로 판단되었으나 keyhole_optimized_final_transformed 데이터가 없습니다. final_transformed로 폴백합니다.")
                    "final_transformed"
                } else {
                    logger.info("🔑 MST ID ${mstId}, DetailId=${actualDetailId}: Keyhole 발생 → keyhole_optimized_final_transformed 사용")
                    "keyhole_optimized_final_transformed"
                }
            } else {
                logger.info("✅ MST ID ${mstId}, DetailId=${actualDetailId}: Keyhole 미발생 → final_transformed 사용")
                "final_transformed"
            }
            
            // ✅ 하드웨어 제한 각도 기준으로 필터링
            val elevationMin = settingsService.angleElevationMin
            
            // ✅ 원본 데이터 조회 (detailId 전달)
            val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toLong(), "original", actualDetailId)  // ✅ detailId 전달
            val axisTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toLong(), "axis_transformed", actualDetailId)  // ✅ detailId 전달
            
            // ✅ 필터링된 final_transformed 데이터 조회 (detailId 전달)
            val finalTransformedDtlAll = getEphemerisTrackDtlByMstIdAndDataType(mstId.toLong(), "final_transformed", actualDetailId)  // ✅ detailId 전달
            val finalTransformedDtl = finalTransformedDtlAll.filter {
                (it["Elevation"] as? Double ?: 0.0) >= elevationMin
            }
            
            // ✅ 필터링된 keyhole_optimized_final_transformed 데이터 조회 (Keyhole 발생 시만, detailId 전달)
            val keyholeFinalDtlAll = if (isKeyhole) {
                getEphemerisTrackDtlByMstIdAndDataType(mstId.toLong(), "keyhole_optimized_final_transformed", actualDetailId)
            } else {
                emptyList()
            }
            val keyholeFinalDtl = if (isKeyhole) {
                keyholeFinalDtlAll.filter {
                    (it["Elevation"] as? Double ?: 0.0) >= elevationMin
                }
            } else {
                emptyList()
            }
            
            // ✅ 필터링된 keyhole_optimized_final_transformed 데이터 조회 (Keyhole 발생 시만, detailId 전달)
            val keyholeOptimizedFinalDtlAll = if (isKeyhole) {
                getEphemerisTrackDtlByMstIdAndDataType(mstId.toLong(), "keyhole_optimized_final_transformed", actualDetailId)  // ✅ detailId 전달
            } else {
                emptyList()
            }
            val keyholeOptimizedFinalDtl = if (isKeyhole) {
                keyholeOptimizedFinalDtlAll.filter {
                    (it["Elevation"] as? Double ?: 0.0) >= elevationMin
                }
            } else {
                emptyList()
            }
            
            // ✅ Keyhole Axis 데이터 조회 (필터링 없음 - 중간 단계 데이터, detailId 전달)
            val keyholeAxisDtl = if (isKeyhole) {
                try {
                    getEphemerisTrackDtlByMstIdAndDataType(mstId.toLong(), "keyhole_axis_transformed", actualDetailId)  // ✅ detailId 전달
            } catch (e: Exception) {
                    logger.warn("⚠️ Keyhole Axis 데이터 조회 실패: ${e.message}")
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            // 필터링 결과 로깅
            logger.info("📊 MST ID ${mstId} CSV 생성:")
            logger.info("   - Keyhole 여부: ${if (isKeyhole) "YES" else "NO"}")
            logger.info("   - 사용 DataType: ${finalDataType}")
            logger.info("   - 필터 기준: elevationMin (하드웨어 제한) = ${elevationMin}°")
            logger.info("   - Original 데이터: ${originalDtl.size}개")
            logger.info("   - AxisTransformed 데이터: ${axisTransformedDtl.size}개")
            logger.info("   - FinalTransformed 전체: ${finalTransformedDtlAll.size}개")
            logger.info("   - FinalTransformed 필터링 후: ${finalTransformedDtl.size}개")
            if (isKeyhole) {
                logger.info("   - KeyholeFinal 전체: ${keyholeFinalDtlAll.size}개")
                logger.info("   - KeyholeFinal 필터링 후: ${keyholeFinalDtl.size}개")
                logger.info("   - KeyholeOptimizedFinal 전체: ${keyholeOptimizedFinalDtlAll.size}개")
                logger.info("   - KeyholeOptimizedFinal 필터링 후: ${keyholeOptimizedFinalDtl.size}개")
            }
            
            if (originalDtl.isEmpty()) {
                logger.error("❌ MST ID ${mstId} 의 원본 데이터를 찾을 수 없습니다")
                return mapOf<String, Any?>("success" to false, "error" to "원본 데이터를 찾을 수 없습니다")
            }
            
            // ✅ 필터링된 데이터가 없으면 경고
            if (finalTransformedDtl.isEmpty()) {
                logger.warn("⚠️ MST ID ${mstId}: 필터링 결과 데이터가 없습니다. (기준: ${elevationMin}°)")
                return mapOf<String, Any?>("success" to false, "error" to "필터링 후 데이터가 없습니다")
            }
            
            // ✅ finalTransformedMst에서 정보 가져오기 (Keyhole 판단 기준, MstId 필드 사용)
            val allMst = getAllEphemerisTrackMst()
            val finalTransformedMstInfo = allMst.find { 
                (it["MstId"] as? Number)?.toLong() == mstId.toLong() && it["DataType"] == "final_transformed"  // ✅ MstId 필드 사용
            }
            val originalMstInfo = allMst.find { 
                (it["MstId"] as? Number)?.toLong() == mstId.toLong() && it["DataType"] == "original"  // ✅ MstId 필드 사용
            }
            val keyholeOptimizedMstInfo = if (isKeyhole) {
                allMst.find { 
                    (it["MstId"] as? Number)?.toLong() == mstId.toLong() && it["DataType"] == "keyhole_optimized_final_transformed"  // ✅ MstId 필드 사용
                }
            } else {
                null
            }
            
            // ✅ finalTransformedMst에서 정보 가져오기 (없으면 original 사용)
            val mstInfo = finalTransformedMstInfo ?: originalMstInfo
            val satelliteName = mstInfo?.get("SatelliteName") as? String ?: "Unknown"
            val startTime = mstInfo?.get("StartTime") as? java.time.ZonedDateTime
            val endTime = mstInfo?.get("EndTime") as? java.time.ZonedDateTime
            
            // ✅ Train 각도 가져오기: finalTransformedMst의 RecommendedTrainAngle 사용 (Keyhole 판단 기준과 일치)
            val recommendedTrainAngle = finalTransformedMstInfo?.get("RecommendedTrainAngle") as? Double ?: 0.0
            val trainAngleFormatted = if (recommendedTrainAngle == 0.0) {
                "0"
            } else {
                String.format("%.6f", recommendedTrainAngle)
            }
            
            // ✅ Keyhole Optimized Train 각도 가져오기 (방법 2)
            val keyholeOptimizedRecommendedTrainAngle = if (isKeyhole) {
                keyholeOptimizedMstInfo?.get("RecommendedTrainAngle") as? Double ?: 0.0
            } else {
                0.0
            }
            val keyholeOptimizedTrainAngleFormatted = if (keyholeOptimizedRecommendedTrainAngle == 0.0) {
                "0"
            } else {
                String.format("%.6f", keyholeOptimizedRecommendedTrainAngle)
            }
            
            // ✅ 파일명 개선
            val dateOnly = startTime?.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) ?: "unknown"
            val filename = "MST${mstId}_${satelliteName}_${dateOnly}.csv"
            val filePath = "$outputDirectory/$filename"
            
            // ✅ Train=0 데이터는 필터링된 finalTransformedDtl 사용
            val train0Dtl = finalTransformedDtl.map { point ->
                val az = point["Azimuth"] as Double
                val el = point["Elevation"] as Double
                val time = point["Time"] as java.time.ZonedDateTime
                
                mapOf(
                    "Time" to time,
                    "Azimuth" to az,
                    "Elevation" to el
                )
            }
            logger.info("📊 Train=0 데이터 생성 완료: ${train0Dtl.size}개 (필터링된 finalTransformedDtl 사용)")
            
            // ✅ 필터링된 final_transformed 데이터 기준으로 original과 axis_transformed도 필터링
            // 필터링된 final_transformed 데이터의 시간을 기준으로 매칭
            val filteredFinalTransformedTimes = finalTransformedDtl.map { it["Time"] as? java.time.ZonedDateTime }.toSet()
            
            // ✅ 필터링된 final_transformed의 시간에 해당하는 original과 axis_transformed만 선택
            val filteredOriginalDtl = originalDtl.filter { 
                val time = it["Time"] as? java.time.ZonedDateTime
                time != null && filteredFinalTransformedTimes.contains(time)
            }
            val filteredAxisTransformedDtl = axisTransformedDtl.filter { 
                val time = it["Time"] as? java.time.ZonedDateTime
                time != null && filteredFinalTransformedTimes.contains(time)
            }
            
            // ✅ 필터링된 keyhole_final_transformed의 시간에 해당하는 keyhole_axis_transformed도 필터링
            val filteredKeyholeFinalTransformedTimes = if (isKeyhole) {
                keyholeFinalDtl.map { it["Time"] as? java.time.ZonedDateTime }.toSet()
            } else {
                emptySet()
            }
            val filteredKeyholeAxisDtl = if (isKeyhole) {
                keyholeAxisDtl.filter { 
                    val time = it["Time"] as? java.time.ZonedDateTime
                    time != null && filteredKeyholeFinalTransformedTimes.contains(time)
                }
            } else {
                emptyList()
            }
            
            // ✅ 필터링된 keyhole_optimized_final_transformed의 시간에 해당하는 데이터도 필터링
            val filteredKeyholeOptimizedFinalTransformedTimes = if (isKeyhole) {
                keyholeOptimizedFinalDtl.map { it["Time"] as? java.time.ZonedDateTime }.toSet()
            } else {
                emptySet()
            }
            
            logger.info("📊 필터링된 데이터 매칭:")
            logger.info("   - Original 필터링 후: ${filteredOriginalDtl.size}개")
            logger.info("   - AxisTransformed 필터링 후: ${filteredAxisTransformedDtl.size}개")
            logger.info("   - FinalTransformed 필터링 후: ${finalTransformedDtl.size}개")
            if (isKeyhole) {
                logger.info("   - KeyholeAxis 필터링 후: ${filteredKeyholeAxisDtl.size}개")
                logger.info("   - KeyholeFinal 필터링 후: ${keyholeFinalDtl.size}개")
            }
            
            // ✅ 필터링된 데이터 기준으로 최대 크기 계산
            val maxSize = maxOf(
                filteredOriginalDtl.size,
                filteredAxisTransformedDtl.size,
                finalTransformedDtl.size,
                if (isKeyhole) keyholeFinalDtl.size else 0,
                if (isKeyhole) keyholeOptimizedFinalDtl.size else 0
            )
            
            // ✅ 최대값 추적용 변수 (블록 밖에서 선언)
            var maxOriginalAzVelocity = 0.0
            var maxOriginalElVelocity = 0.0
            var maxAxisTransformedAzVelocity = 0.0
            var maxAxisTransformedElVelocity = 0.0
            var maxTrain0AzVelocity = 0.0
            var maxTrain0ElVelocity = 0.0
            var maxKeyholeAxisAzVelocity = 0.0
            var maxKeyholeAxisElVelocity = 0.0
            var maxKeyholeFinalAzVelocity = 0.0
            var maxKeyholeFinalElVelocity = 0.0
            var maxKeyholeOptimizedFinalAzVelocity = 0.0
            var maxKeyholeOptimizedFinalElVelocity = 0.0
            
            java.io.FileWriter(filePath).use { writer ->
                // ✅ 사용자 요구사항에 맞는 CSV 헤더: 각 변환 단계별 각속도 포함
                writer.write("Index,Time,")
                writer.write("Original_Azimuth,Original_Elevation,Original_Azimuth_Velocity,Original_Elevation_Velocity,")
                writer.write("Original_Range,Original_Altitude,")
                writer.write("AxisTransformed_Azimuth,AxisTransformed_Elevation,AxisTransformed_Azimuth_Velocity,AxisTransformed_Elevation_Velocity,")
                writer.write("FinalTransformed_train0_Azimuth,FinalTransformed_train0_Elevation,FinalTransformed_train0_Azimuth_Velocity,FinalTransformed_train0_Elevation_Velocity,")
                
                // Keyhole 발생 시만 Keyhole 컬럼 추가
                if (isKeyhole) {
                    writer.write("KeyholeAxisTransformed_train${trainAngleFormatted}_Azimuth,KeyholeAxisTransformed_train${trainAngleFormatted}_Elevation,KeyholeAxisTransformed_train${trainAngleFormatted}_Azimuth_Velocity,KeyholeAxisTransformed_train${trainAngleFormatted}_Elevation_Velocity,")
                    writer.write("KeyholeFinalTransformed_train${trainAngleFormatted}_Azimuth,KeyholeFinalTransformed_train${trainAngleFormatted}_Elevation,KeyholeFinalTransformed_train${trainAngleFormatted}_Azimuth_Velocity,KeyholeFinalTransformed_train${trainAngleFormatted}_Elevation_Velocity,")
                    // ✅ 방법 2 (신규): Keyhole Optimized 컬럼 추가
                    writer.write("KeyholeOptimizedFinalTransformed_train${keyholeOptimizedTrainAngleFormatted}_Azimuth,KeyholeOptimizedFinalTransformed_train${keyholeOptimizedTrainAngleFormatted}_Elevation,KeyholeOptimizedFinalTransformed_train${keyholeOptimizedTrainAngleFormatted}_Azimuth_Velocity,KeyholeOptimizedFinalTransformed_train${keyholeOptimizedTrainAngleFormatted}_Elevation_Velocity,")
                }
                
                writer.write("Azimuth_Transformation_Error,Elevation_Transformation_Error")
                
                // ✅ 비교 결과 컬럼 추가 (Keyhole 발생 시만)
                if (isKeyhole) {
                    writer.write(",OptimizationImprovement,OptimizationImprovementRate\n")
                } else {
                    writer.write("\n")
                }
                
                // ✅ 필터링된 데이터 기준으로 CSV 데이터 생성
                // 시간 기준으로 매칭하여 인덱스 불일치 방지
                var prevOriginalAzimuth: Double? = null
                var prevOriginalElevation: Double? = null
                var prevAxisTransformedAzimuth: Double? = null
                var prevAxisTransformedElevation: Double? = null
                var prevFinalTransformedAzimuth: Double? = null
                var prevFinalTransformedElevation: Double? = null
                var prevTime: java.time.ZonedDateTime? = null
                
                for (i in 0 until maxSize) {
                    // ✅ 필터링된 final_transformed 데이터 기준으로 매칭
                    val finalTransformedPoint = if (i < finalTransformedDtl.size) finalTransformedDtl[i] else null
                    val finalTransformedTime = finalTransformedPoint?.get("Time") as? java.time.ZonedDateTime
                    
                    // ✅ 시간 기준으로 original과 axis_transformed 매칭
                    val originalPoint = if (finalTransformedTime != null) {
                        filteredOriginalDtl.find { it["Time"] == finalTransformedTime }
                    } else {
                        if (i < filteredOriginalDtl.size) filteredOriginalDtl[i] else null
                    }
                    
                    val axisTransformedPoint = if (finalTransformedTime != null) {
                        filteredAxisTransformedDtl.find { it["Time"] == finalTransformedTime }
                    } else {
                        if (i < filteredAxisTransformedDtl.size) filteredAxisTransformedDtl[i] else null
                    }
                    
                    // ✅ Keyhole 데이터 매칭 (Keyhole 발생 시만)
                    val keyholeFinalPoint = if (isKeyhole && finalTransformedTime != null) {
                        keyholeFinalDtl.find { it["Time"] == finalTransformedTime }
                    } else {
                        null
                    }
                    
                    val keyholeAxisPoint = if (isKeyhole && finalTransformedTime != null) {
                        filteredKeyholeAxisDtl.find { it["Time"] == finalTransformedTime }
                    } else {
                        null
                    }
                    
                    // ✅ Keyhole Optimized 데이터 매칭 (Keyhole 발생 시만, 방법 2)
                    val keyholeOptimizedFinalPoint = if (isKeyhole && finalTransformedTime != null) {
                        keyholeOptimizedFinalDtl.find { it["Time"] == finalTransformedTime }
                    } else {
                        null
                    }
                    
                    val originalTime = originalPoint?.get("Time") as? java.time.ZonedDateTime
                    val originalAz = originalPoint?.get("Azimuth") as? Double ?: 0.0
                    val originalEl = originalPoint?.get("Elevation") as? Double ?: 0.0
                    val originalRange = originalPoint?.get("Range") as? Double ?: 0.0
                    val originalAltitude = originalPoint?.get("Altitude") as? Double ?: 0.0
                    
                    val axisTransformedAz = axisTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                    val axisTransformedEl = axisTransformedPoint?.get("Elevation") as? Double ?: 0.0
                    
                    val finalTransformedAz = finalTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                    val finalTransformedEl = finalTransformedPoint?.get("Elevation") as? Double ?: 0.0
                    
                    // ✅ 각 변환 단계별 각속도 계산 (이론치 합계법 - 10개 변화량의 합)
                    var originalAzimuthVelocity = 0.0
                    var originalElevationVelocity = 0.0
                    var axisTransformedAzimuthVelocity = 0.0
                    var axisTransformedElevationVelocity = 0.0
                    var train0AzimuthVelocity = 0.0
                    var train0ElevationVelocity = 0.0
                    var keyholeAxisAzimuthVelocity = 0.0
                    var keyholeAxisElevationVelocity = 0.0
                    var keyholeFinalAzimuthVelocity = 0.0
                    var keyholeFinalElevationVelocity = 0.0
                    var keyholeOptimizedFinalAzimuthVelocity = 0.0
                    var keyholeOptimizedFinalElevationVelocity = 0.0
                    
                    // Train=0 데이터 포인트 가져오기
                    val train0Point = if (i < train0Dtl.size) train0Dtl[i] else null
                    
                    // ✅ 이론치 합계법: 1초간(10개) 총 변화량 계산 (시간으로 나누지 않음)
                    if (i >= 9) { // Index 9부터 10개 데이터 구간 형성 가능
                        var currentOriginalAzSum = 0.0
                        var currentOriginalElSum = 0.0
                        var currentAxisTransformedAzSum = 0.0
                        var currentAxisTransformedElSum = 0.0
                        var currentTrain0AzSum = 0.0
                        var currentTrain0ElSum = 0.0
                        var currentKeyholeAxisAzSum = 0.0
                        var currentKeyholeAxisElSum = 0.0
                        var currentKeyholeFinalAzSum = 0.0
                        var currentKeyholeFinalElSum = 0.0
                        var currentKeyholeOptimizedFinalAzSum = 0.0
                        var currentKeyholeOptimizedFinalElSum = 0.0
                        
                        // 10개 구간의 변화량을 모두 더함 (j-1이 유효하도록)
                        // ✅ 필터링된 데이터 기준으로 계산
                        for (j in (i - 9)..i) { // j는 현재 인덱스 i까지, 이전 9개 포함 (총 10개)
                            if (j > 0 && j < filteredOriginalDtl.size && (j - 1) < filteredOriginalDtl.size) { // j-1이 유효한 경우만 계산
                                val prevOriginalPoint = filteredOriginalDtl[j - 1]
                                val currentOriginalPoint = filteredOriginalDtl[j]
                                val prevAxisTransformedPoint = filteredAxisTransformedDtl[j - 1]
                                val currentAxisTransformedPoint = filteredAxisTransformedDtl[j]
                                val prevTrain0Point = train0Dtl[j - 1]
                                val currentTrain0Point = train0Dtl[j]
                                
                                // Original
                                val prevOriginalAz = prevOriginalPoint["Azimuth"] as Double
                                val currentOriginalAz = currentOriginalPoint["Azimuth"] as Double
                                val prevOriginalEl = prevOriginalPoint["Elevation"] as Double
                                val currentOriginalEl = currentOriginalPoint["Elevation"] as Double
                                var azDiffOriginal = currentOriginalAz - prevOriginalAz
                                if (azDiffOriginal > 180) azDiffOriginal -= 360
                                if (azDiffOriginal < -180) azDiffOriginal += 360
                                currentOriginalAzSum += kotlin.math.abs(azDiffOriginal)
                                currentOriginalElSum += kotlin.math.abs(currentOriginalEl - prevOriginalEl)
                                
                                // AxisTransformed
                                val prevAxisTransformedAz = prevAxisTransformedPoint["Azimuth"] as Double
                                val currentAxisTransformedAz = currentAxisTransformedPoint["Azimuth"] as Double
                                val prevAxisTransformedEl = prevAxisTransformedPoint["Elevation"] as Double
                                val currentAxisTransformedEl = currentAxisTransformedPoint["Elevation"] as Double
                                var azDiffAxis = currentAxisTransformedAz - prevAxisTransformedAz
                                if (azDiffAxis > 180) azDiffAxis -= 360
                                if (azDiffAxis < -180) azDiffAxis += 360
                                currentAxisTransformedAzSum += kotlin.math.abs(azDiffAxis)
                                currentAxisTransformedElSum += kotlin.math.abs(currentAxisTransformedEl - prevAxisTransformedEl)
                                
                                // Train0
                                val prevTrain0Az = prevTrain0Point["Azimuth"] as Double
                                val currentTrain0Az = currentTrain0Point["Azimuth"] as Double
                                val prevTrain0El = prevTrain0Point["Elevation"] as Double
                                val currentTrain0El = currentTrain0Point["Elevation"] as Double
                                var azDiffTrain0 = currentTrain0Az - prevTrain0Az
                                if (azDiffTrain0 > 180) azDiffTrain0 -= 360
                                if (azDiffTrain0 < -180) azDiffTrain0 += 360
                                currentTrain0AzSum += kotlin.math.abs(azDiffTrain0)
                                currentTrain0ElSum += kotlin.math.abs(currentTrain0El - prevTrain0El)
                                
                                // ✅ Keyhole Axis (Keyhole 발생 시만)
                                if (isKeyhole && j < filteredKeyholeAxisDtl.size && (j - 1) < filteredKeyholeAxisDtl.size) {
                                    val prevKeyholeAxisPoint = filteredKeyholeAxisDtl[j - 1]
                                    val currentKeyholeAxisPoint = filteredKeyholeAxisDtl[j]
                                    val prevKeyholeAxisAz = prevKeyholeAxisPoint["Azimuth"] as Double
                                    val currentKeyholeAxisAz = currentKeyholeAxisPoint["Azimuth"] as Double
                                    val prevKeyholeAxisEl = prevKeyholeAxisPoint["Elevation"] as Double
                                    val currentKeyholeAxisEl = currentKeyholeAxisPoint["Elevation"] as Double
                                    var azDiffKeyholeAxis = currentKeyholeAxisAz - prevKeyholeAxisAz
                                    if (azDiffKeyholeAxis > 180) azDiffKeyholeAxis -= 360
                                    if (azDiffKeyholeAxis < -180) azDiffKeyholeAxis += 360
                                    currentKeyholeAxisAzSum += kotlin.math.abs(azDiffKeyholeAxis)
                                    currentKeyholeAxisElSum += kotlin.math.abs(currentKeyholeAxisEl - prevKeyholeAxisEl)
                                }
                                
                                // ✅ Keyhole Final (Keyhole 발생 시만)
                                if (isKeyhole && j < keyholeFinalDtl.size && (j - 1) < keyholeFinalDtl.size) {
                                    val prevKeyholeFinalPoint = keyholeFinalDtl[j - 1]
                                    val currentKeyholeFinalPoint = keyholeFinalDtl[j]
                                    val prevKeyholeFinalAz = prevKeyholeFinalPoint["Azimuth"] as Double
                                    val currentKeyholeFinalAz = currentKeyholeFinalPoint["Azimuth"] as Double
                                    val prevKeyholeFinalEl = prevKeyholeFinalPoint["Elevation"] as Double
                                    val currentKeyholeFinalEl = currentKeyholeFinalPoint["Elevation"] as Double
                                    var azDiffKeyholeFinal = currentKeyholeFinalAz - prevKeyholeFinalAz
                                    if (azDiffKeyholeFinal > 180) azDiffKeyholeFinal -= 360
                                    if (azDiffKeyholeFinal < -180) azDiffKeyholeFinal += 360
                                    currentKeyholeFinalAzSum += kotlin.math.abs(azDiffKeyholeFinal)
                                    currentKeyholeFinalElSum += kotlin.math.abs(currentKeyholeFinalEl - prevKeyholeFinalEl)
                                }
                                
                                // ✅ Keyhole Optimized Final (Keyhole 발생 시만, 방법 2)
                                if (isKeyhole && j < keyholeOptimizedFinalDtl.size && (j - 1) < keyholeOptimizedFinalDtl.size) {
                                    val prevKeyholeOptimizedFinalPoint = keyholeOptimizedFinalDtl[j - 1]
                                    val currentKeyholeOptimizedFinalPoint = keyholeOptimizedFinalDtl[j]
                                    val prevKeyholeOptimizedFinalAz = prevKeyholeOptimizedFinalPoint["Azimuth"] as Double
                                    val currentKeyholeOptimizedFinalAz = currentKeyholeOptimizedFinalPoint["Azimuth"] as Double
                                    val prevKeyholeOptimizedFinalEl = prevKeyholeOptimizedFinalPoint["Elevation"] as Double
                                    val currentKeyholeOptimizedFinalEl = currentKeyholeOptimizedFinalPoint["Elevation"] as Double
                                    var azDiffKeyholeOptimizedFinal = currentKeyholeOptimizedFinalAz - prevKeyholeOptimizedFinalAz
                                    if (azDiffKeyholeOptimizedFinal > 180) azDiffKeyholeOptimizedFinal -= 360
                                    if (azDiffKeyholeOptimizedFinal < -180) azDiffKeyholeOptimizedFinal += 360
                                    currentKeyholeOptimizedFinalAzSum += kotlin.math.abs(azDiffKeyholeOptimizedFinal)
                                    currentKeyholeOptimizedFinalElSum += kotlin.math.abs(currentKeyholeOptimizedFinalEl - prevKeyholeOptimizedFinalEl)
                                }
                            }
                        }
                        
                        originalAzimuthVelocity = currentOriginalAzSum
                        originalElevationVelocity = currentOriginalElSum
                        axisTransformedAzimuthVelocity = currentAxisTransformedAzSum
                        axisTransformedElevationVelocity = currentAxisTransformedElSum
                        train0AzimuthVelocity = currentTrain0AzSum
                        train0ElevationVelocity = currentTrain0ElSum
                        keyholeAxisAzimuthVelocity = currentKeyholeAxisAzSum
                        keyholeAxisElevationVelocity = currentKeyholeAxisElSum
                        keyholeFinalAzimuthVelocity = currentKeyholeFinalAzSum
                        keyholeFinalElevationVelocity = currentKeyholeFinalElSum
                        keyholeOptimizedFinalAzimuthVelocity = currentKeyholeOptimizedFinalAzSum
                        keyholeOptimizedFinalElevationVelocity = currentKeyholeOptimizedFinalElSum
                        
                        // 최대값 업데이트
                        maxOriginalAzVelocity = maxOf(maxOriginalAzVelocity, originalAzimuthVelocity)
                        maxOriginalElVelocity = maxOf(maxOriginalElVelocity, originalElevationVelocity)
                        maxAxisTransformedAzVelocity = maxOf(maxAxisTransformedAzVelocity, axisTransformedAzimuthVelocity)
                        maxAxisTransformedElVelocity = maxOf(maxAxisTransformedElVelocity, axisTransformedElevationVelocity)
                        maxTrain0AzVelocity = maxOf(maxTrain0AzVelocity, train0AzimuthVelocity)
                        maxTrain0ElVelocity = maxOf(maxTrain0ElVelocity, train0ElevationVelocity)
                        maxKeyholeAxisAzVelocity = maxOf(maxKeyholeAxisAzVelocity, keyholeAxisAzimuthVelocity)
                        maxKeyholeAxisElVelocity = maxOf(maxKeyholeAxisElVelocity, keyholeAxisElevationVelocity)
                        maxKeyholeFinalAzVelocity = maxOf(maxKeyholeFinalAzVelocity, keyholeFinalAzimuthVelocity)
                        maxKeyholeFinalElVelocity = maxOf(maxKeyholeFinalElVelocity, keyholeFinalElevationVelocity)
                        maxKeyholeOptimizedFinalAzVelocity = maxOf(maxKeyholeOptimizedFinalAzVelocity, keyholeOptimizedFinalAzimuthVelocity)
                        maxKeyholeOptimizedFinalElVelocity = maxOf(maxKeyholeOptimizedFinalElVelocity, keyholeOptimizedFinalElevationVelocity)
                    }
                    
                    val azimuthTransformationError = axisTransformedAz - originalAz
                    val elevationTransformationError = axisTransformedEl - originalEl
                    
                    val timeString = originalTime?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) ?: ""
                    
                    // ✅ Train=0 데이터 가져오기
                    val train0Az = train0Point?.get("Azimuth") as? Double ?: 0.0
                    val train0El = train0Point?.get("Elevation") as? Double ?: 0.0
                    
                    // ✅ 사용자 요구사항에 맞는 CSV 데이터 출력
                    writer.write("$i,$timeString,")
                    writer.write("${String.format("%.6f", originalAz)},${String.format("%.6f", originalEl)},${String.format("%.6f", originalAzimuthVelocity)},${String.format("%.6f", originalElevationVelocity)},")
                    writer.write("${String.format("%.6f", originalRange)},${String.format("%.6f", originalAltitude)},")
                    writer.write("${String.format("%.6f", axisTransformedAz)},${String.format("%.6f", axisTransformedEl)},${String.format("%.6f", axisTransformedAzimuthVelocity)},${String.format("%.6f", axisTransformedElevationVelocity)},")
                    
                    // Train=0 데이터 출력
                    writer.write("${String.format("%.6f", train0Az)},${String.format("%.6f", train0El)},${String.format("%.6f", train0AzimuthVelocity)},${String.format("%.6f", train0ElevationVelocity)},")
                    
                    // Keyhole 발생 시만 Keyhole 데이터 출력
                    if (isKeyhole) {
                        // ✅ Keyhole Axis 데이터 (각도 제한 ❌) - 필터링된 데이터 사용
                        val keyholeAxisAz = keyholeAxisPoint?.get("Azimuth") as? Double ?: 0.0
                        val keyholeAxisEl = keyholeAxisPoint?.get("Elevation") as? Double ?: 0.0
                            writer.write("${String.format("%.6f", keyholeAxisAz)},${String.format("%.6f", keyholeAxisEl)},${String.format("%.6f", keyholeAxisAzimuthVelocity)},${String.format("%.6f", keyholeAxisElevationVelocity)},")
                        
                        // ✅ Keyhole Final 데이터 (각도 제한 ✅) - 필터링된 데이터 사용
                        val keyholeFinalAz = keyholeFinalPoint?.get("Azimuth") as? Double ?: 0.0
                        val keyholeFinalEl = keyholeFinalPoint?.get("Elevation") as? Double ?: 0.0
                            writer.write("${String.format("%.6f", keyholeFinalAz)},${String.format("%.6f", keyholeFinalEl)},${String.format("%.6f", keyholeFinalAzimuthVelocity)},${String.format("%.6f", keyholeFinalElevationVelocity)},")
                        
                        // ✅ 방법 2 (신규): Keyhole Optimized Final 데이터 (각도 제한 ✅) - 필터링된 데이터 사용
                        val keyholeOptimizedFinalAz = keyholeOptimizedFinalPoint?.get("Azimuth") as? Double ?: 0.0
                        val keyholeOptimizedFinalEl = keyholeOptimizedFinalPoint?.get("Elevation") as? Double ?: 0.0
                            writer.write("${String.format("%.6f", keyholeOptimizedFinalAz)},${String.format("%.6f", keyholeOptimizedFinalEl)},${String.format("%.6f", keyholeOptimizedFinalAzimuthVelocity)},${String.format("%.6f", keyholeOptimizedFinalElevationVelocity)},")
                    }
                    
                    writer.write("${String.format("%.6f", azimuthTransformationError)},${String.format("%.6f", elevationTransformationError)}")
                    
                    // ✅ 비교 결과 출력 (Keyhole 발생 시만)
                    if (isKeyhole) {
                        val improvement = keyholeFinalAzimuthVelocity - keyholeOptimizedFinalAzimuthVelocity
                        val improvementRate = if (keyholeFinalAzimuthVelocity > 0) {
                            (improvement / keyholeFinalAzimuthVelocity) * 100.0
                        } else {
                            0.0
                        }
                        writer.write(",${String.format("%.6f", improvement)},${String.format("%.2f", improvementRate)}\n")
                    } else {
                        writer.write("\n")
                    }
                    
                    // ✅ 다음 반복을 위한 값 저장
                    prevOriginalAzimuth = originalAz
                    prevOriginalElevation = originalEl
                    prevAxisTransformedAzimuth = axisTransformedAz
                    prevAxisTransformedElevation = axisTransformedEl
                    prevFinalTransformedAzimuth = finalTransformedAz
                    prevFinalTransformedElevation = finalTransformedEl
                    prevTime = originalTime
                }
            }
            logger.info("📊 MST ID $mstId CSV 파일 생성 완료: $filePath")
            logger.info("  - 원본 데이터: ${originalDtl.size}개")
            logger.info("  - 축변환 데이터: ${axisTransformedDtl.size}개")
            logger.info("  - 최종 변환 데이터: ${finalTransformedDtl.size}개")
            logger.info("✅ CSV 합계법 최대값:")
            logger.info("  - Original_Azimuth_Velocity: ${String.format("%.6f", maxOriginalAzVelocity)}°/s")
            logger.info("  - Original_Elevation_Velocity: ${String.format("%.6f", maxOriginalElVelocity)}°/s")
            logger.info("  - Train0_Azimuth_Velocity: ${String.format("%.6f", maxTrain0AzVelocity)}°/s")
            logger.info("  - Train0_Elevation_Velocity: ${String.format("%.6f", maxTrain0ElVelocity)}°/s")
            if (isKeyhole) {
                logger.info("  - KeyholeAxis_train${trainAngleFormatted}_Azimuth_Velocity: ${String.format("%.6f", maxKeyholeAxisAzVelocity)}°/s")
                logger.info("  - KeyholeAxis_train${trainAngleFormatted}_Elevation_Velocity: ${String.format("%.6f", maxKeyholeAxisElVelocity)}°/s")
                logger.info("  - KeyholeFinal_train${trainAngleFormatted}_Azimuth_Velocity: ${String.format("%.6f", maxKeyholeFinalAzVelocity)}°/s")
                logger.info("  - KeyholeFinal_train${trainAngleFormatted}_Elevation_Velocity: ${String.format("%.6f", maxKeyholeFinalElVelocity)}°/s")
                logger.info("  - KeyholeOptimizedFinal_train${keyholeOptimizedTrainAngleFormatted}_Azimuth_Velocity: ${String.format("%.6f", maxKeyholeOptimizedFinalAzVelocity)}°/s")
                logger.info("  - KeyholeOptimizedFinal_train${keyholeOptimizedTrainAngleFormatted}_Elevation_Velocity: ${String.format("%.6f", maxKeyholeOptimizedFinalElVelocity)}°/s")
                
                // ✅ 비교 결과 로깅
                val improvement = maxKeyholeFinalAzVelocity - maxKeyholeOptimizedFinalAzVelocity
                val improvementRate = if (maxKeyholeFinalAzVelocity > 0) {
                    (improvement / maxKeyholeFinalAzVelocity) * 100.0
                } else {
                    0.0
                }
                logger.info("📊 비교 결과 (최대값 기준):")
                logger.info("  - 방법 1 (기존): ${String.format("%.6f", maxKeyholeFinalAzVelocity)}°/s")
                logger.info("  - 방법 2 (신규): ${String.format("%.6f", maxKeyholeOptimizedFinalAzVelocity)}°/s")
                logger.info("  - 개선량: ${String.format("%.6f", improvement)}°/s")
                logger.info("  - 개선율: ${String.format("%.2f", improvementRate)}%")
            }
            return mapOf<String, Any?>(
                "success" to true,
                "filename" to filename,
                "filePath" to filePath,
                "mstId" to mstId,
                "satelliteName" to satelliteName,
                "originalDataCount" to originalDtl.size,
                "axisTransformedDataCount" to axisTransformedDtl.size,
                "finalTransformedDataCount" to finalTransformedDtl.size,
                "startTime" to startTime,
                "endTime" to endTime
            )
        } catch (e: Exception) {
            logger.error("❌ MST ID $mstId CSV 파일 생성 중 오류: ${e.message}", e)
            return mapOf<String, Any?>(
                "success" to false,
                "error" to e.message,
                "mstId" to mstId
            )
        }
    }

    /**
     * 📊 특정 MST ID의 데이터를 간단한 CSV 파일로 추출 (기본 정보만)
     * ✅ mstId와 detailId를 사용하여 조회 (PassSchedule과 동일한 구조)
     * 
     * @param mstId 마스터 ID
     * @param detailId 패스 인덱스 (기본값: null, MST에서 자동 조회)
     * @param outputDirectory 출력 디렉토리
     * @return CSV 파일 생성 결과
     */
    fun exportMstDataToSimpleCsv(mstId: Int, detailId: Int? = null, outputDirectory: String = "csv_exports"): Map<String, Any?> {
        try {
            logger.info("📊 MST ID $mstId 간단 CSV 파일 생성 시작")
            
            // ✅ MST에서 DetailId 가져오기 (파라미터보다 우선)
            val mstInfo = getAllEphemerisTrackMst().find { 
                (it["MstId"] as? Number)?.toLong() == mstId.toLong() && it["DataType"] == "original"
            }
            val actualDetailId = detailId ?: ((mstInfo?.get("DetailId") as? Number)?.toInt() ?: 0)
            
            val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toLong(), "original", actualDetailId)  // ✅ detailId 전달
            if (originalDtl.isEmpty()) {
                logger.error("❌ MST ID $mstId 의 원본 데이터를 찾을 수 없습니다")
                return mapOf<String, Any?>("success" to false, "error" to "원본 데이터를 찾을 수 없습니다")
            }
            // ✅ 위성 이름 가져오기 (이미 조회한 mstInfo 재사용)
            val satelliteName = mstInfo?.get("SatelliteName") as? String ?: "Unknown"
            val timestamp =
                java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val filename = "MST${mstId}_${satelliteName}_Simple_${timestamp}.csv"
            val filePath = "$outputDirectory/$filename"
            java.io.FileWriter(filePath).use { writer ->
                writer.write("Index,Time,Azimuth,Elevation,Range,Altitude\n")
                originalDtl.forEachIndexed { index, point ->
                    val time = point["Time"] as? java.time.ZonedDateTime
                    val azimuth = point["Azimuth"] as? Double ?: 0.0
                    val elevation = point["Elevation"] as? Double ?: 0.0
                    val range = point["Range"] as? Double ?: 0.0
                    val altitude = point["Altitude"] as? Double ?: 0.0
                    val timeString =
                        time?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) ?: ""
                    writer.write(
                        "$index,$timeString,${String.format("%.6f", azimuth)},${
                            String.format(
                                "%.6f",
                                elevation
                            )
                        },${String.format("%.6f", range)},${String.format("%.6f", altitude)}\n"
                    )
                }
            }
            logger.info("📊 MST ID $mstId 간단 CSV 파일 생성 완료: $filePath")
            return mapOf<String, Any?>(
                "success" to true,
                "filename" to filename,
                "filePath" to filePath,
                "mstId" to mstId,
                "satelliteName" to satelliteName,
                "dataCount" to originalDtl.size
            )
        } catch (e: Exception) {
            logger.error("❌ MST ID $mstId 간단 CSV 파일 생성 중 오류: ${e.message}", e)
            return mapOf<String, Any?>(
                "success" to false,
                "error" to e.message,
                "mstId" to mstId
            )
        }
    }

    // 새로운 보간 함수 추가
    private fun calculateInterpolatedPositionWithSeparatedData(
        original: List<Map<String, Any?>>,
        axisTransformed: List<Map<String, Any?>>,
        finalTransformed: List<Map<String, Any?>>,
        currentTime: ZonedDateTime,
        startTime: ZonedDateTime
    ): Map<String, Any?> {
        val timeDifferenceMs = Duration.between(startTime, currentTime).toMillis()
        val calculatedIndex = timeDifferenceMs / 100.0

        fun interpolate(list: List<Map<String, Any?>>, key: String): Float {
            if (list.isEmpty()) return 0.0f
            val lowerIndex = calculatedIndex.toInt().coerceIn(0, list.size - 1)
            val upperIndex = (lowerIndex + 1).coerceAtMost(list.size - 1)
            val fraction = (calculatedIndex - lowerIndex).coerceIn(0.0, 1.0)
            val lower = (list[lowerIndex][key] as? Double) ?: 0.0
            val upper = (list[upperIndex][key] as? Double) ?: 0.0
            return (lower + (upper - lower) * fraction).toFloat()
        }

        return mapOf(
            "originalAzimuth" to interpolate(original, "Azimuth"),
            "originalElevation" to interpolate(original, "Elevation"),
            "originalRange" to interpolate(original, "Range"),
            "originalAltitude" to interpolate(original, "Altitude"),
            "axisTransformedAzimuth" to interpolate(axisTransformed, "Azimuth"),
            "axisTransformedElevation" to interpolate(axisTransformed, "Elevation"),
            "axisTransformedRange" to interpolate(axisTransformed, "Range"),
            "axisTransformedAltitude" to interpolate(axisTransformed, "Altitude"),
            "finalTransformedAzimuth" to interpolate(finalTransformed, "Azimuth"),
            "finalTransformedElevation" to interpolate(finalTransformed, "Elevation"),
            "finalTransformedRange" to interpolate(finalTransformed, "Range"),
            "finalTransformedAltitude" to interpolate(finalTransformed, "Altitude")
        )
    }
}
