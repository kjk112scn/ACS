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
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.service.icd.ICDService
import com.gtlsystems.acs_api.service.udp.UdpFwICDService
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

    private var currentTrackingPassId: UInt? = null
    private var subscriptions: MutableList<Disposable> = mutableListOf()

    // ✅ 간단한 실행 완료 플래그 (Set 사용)
    private val executedActions = mutableSetOf<String>()
    // ✅ Timer 사용 (간단함)

    private val trackingStatus = PushData.TRACKING_STATUS

    // ✅ 통합 쓰레드 관리자 사용
    private var modeExecutor: ScheduledExecutorService? = null
    private var modeTask: ScheduledFuture<*>? = null

    // ✅ 정지궤도 추적 상태 관리
    enum class TrackingState {
        IDLE,
        MOVING_TRAIN_TO_ZERO,
        WAITING_FOR_TRAIN_STABILIZATION,
        MOVING_TO_TARGET,
        TRACKING_ACTIVE
    }

    private var currentTrackingState = TrackingState.IDLE
    private var stabilizationStartTime: Long = 0
    private var targetAzimuth: Float = 0f
    private var targetElevation: Float = 0f

    // ✅ Train 축 안정화 대기 시간
    companion object {
        const val TRAIN_STABILIZATION_TIMEOUT = 3L // Tilt 안정화: 10분
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
                // 위성 추적 헤더가 수신되면 초기 추적 데이터 전송
                currentTrackingPassId?.let { passId ->
                    sendInitialTrackingData(passId)
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
            val geo3AxisPosition = getCurrentGeostationaryPositionWith3AxisTransform(tleLine1, tleLine2)

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

            // ✅ 공통 상태머신 진입
            currentTrackingState = TrackingState.MOVING_TRAIN_TO_ZERO

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
     * 2축 추적 데이터 생성 (축변환 적용)
     * TLE 데이터로 위성 궤도 추적
     * 위성 이름이 제공되지 않으면 TLE에서 추출
     */
    fun generateEphemerisDesignationTrackSync(
        tleLine1: String, tleLine2: String, satelliteName: String? = null
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        try {
            logger.info("🚀 위성 궤도 추적 시작 (리팩토링된 단계별 처리)")

            // ✅ 1단계: 원본 데이터 생성
            val (originalMst, originalDtl) = generateOriginalTrackingData(tleLine1, tleLine2, satelliteName)
            logger.info("✅ 1단계 완료: 원본 데이터 생성 - ${originalMst.size}개 마스터, ${originalDtl.size}개 세부")

            // ✅ 2단계: 축변환 적용
            val (axisTransformedMst, axisTransformedDtl) = applyAxisTransformation(originalMst, originalDtl)
            logger.info("✅ 2단계 완료: 축변환 적용 - ${axisTransformedMst.size}개 마스터, ${axisTransformedDtl.size}개 세부")

            // ✅ 3단계: 방위각 변환 (±270도 제한)
            val (finalMst, finalDtl) = applyAngleLimitTransformation(axisTransformedMst, axisTransformedDtl)
            logger.info("✅ 3단계 완료: 방위각 변환 - ${finalMst.size}개 마스터, ${finalDtl.size}개 세부")

            // ✅ 4단계: 모든 변환 데이터 저장
            saveAllTransformationData(
                originalMst,
                originalDtl,
                axisTransformedMst,
                axisTransformedDtl,
                finalMst,
                finalDtl
            )
            logger.info("✅ 4단계 완료: 모든 변환 데이터 저장")
            logger.info("🎉 위성 궤도 추적 완료 (리팩토링된 단계별 처리)")
            return Pair(finalMst, finalDtl)
        } catch (e: Exception) {
            logger.error("❌ 위성 궤도 추적 중 오류 발생: ${e.message}", e)
            throw e
        }
    }

    /**
     * ✅ 1단계: 원본 데이터 생성
     */
    private fun generateOriginalTrackingData(
        tleLine1: String, tleLine2: String, satelliteName: String?
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        logger.info("📊 1단계: 원본 데이터 생성 시작")

        // TLE에서 위성 ID 추출
        val satelliteId = tleLine1.substring(2, 7).trim()
        val actualSatelliteName = satelliteName ?: getSatelliteNameFromId(satelliteId)
        logger.info("위성 정보: ID=$satelliteId, 이름=$actualSatelliteName")

        // 추적 기간 설정 (오늘 00시부터 내일 00시까지)
        val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val ephemerisTrackMst = mutableListOf<Map<String, Any?>>()
        val ephemerisTrackDtl = mutableListOf<Map<String, Any?>>()

        // 위성 추적 스케줄 생성
        val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
            tleLine1 = tleLine1,
            tleLine2 = tleLine2,
            startDate = today.withZoneSameInstant(ZoneOffset.UTC),
            durationDays = 2,
            minElevation = settingsService.minElevationAngle,
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            altitude = locationData.altitude,
        )
        logger.info("위성 추적 스케줄 생성 완료: ${schedule.trackingPasses.size}개 패스")

        // 생성 메타데이터
        val creationDate = ZonedDateTime.now()
        val creator = "System"

        // 스케줄 정보로 마스터 리스트 채우기 (원본 데이터)
        schedule.trackingPasses.forEachIndexed { index, pass ->
            val mstId = index + 1
            val startTimeWithMs = pass.startTime.withZoneSameInstant(ZoneOffset.UTC)
            val endTimeWithMs = pass.endTime.withZoneSameInstant(ZoneOffset.UTC)

            logger.info("패스 #$mstId: 시작=$startTimeWithMs, 종료=$endTimeWithMs")

            val maxElevationAzimuth = pass.trackingData
                .maxByOrNull { it.elevation }?.azimuth ?: 0.0
            // 원본 데이터로 마스터 정보 생성
            ephemerisTrackMst.add(
                mapOf(
                    "No" to mstId.toUInt(),
                    "SatelliteID" to satelliteId,
                    "SatelliteName" to actualSatelliteName,
                    "StartTime" to startTimeWithMs,
                    "EndTime" to endTimeWithMs,
                    "Duration" to pass.getDurationString(),
                    "MaxElevationTime" to pass.maxElevationTime,
                    "MaxElevation" to pass.maxElevation,
                    "MaxAzimuth" to maxElevationAzimuth,
                    "StartAzimuth" to pass.startAzimuth,
                    "StartElevation" to pass.startElevation,
                    "EndAzimuth" to pass.endAzimuth,
                    "EndElevation" to pass.endElevation,
                    "MaxAzRate" to pass.maxAzimuthRate,
                    "MaxElRate" to pass.maxElevationRate,
                    "MaxAzAccel" to pass.maxAzimuthAccel,
                    "MaxElAccel" to pass.maxElevationAccel,
                    "CreationDate" to creationDate,
                    "Creator" to creator,
                    "DataType" to "original"
                )
            )

            // 원본 추적 좌표로 세부 리스트 채우기
            pass.trackingData.forEachIndexed { dtlIndex, data ->
                ephemerisTrackDtl.add(
                    mapOf(
                        "No" to (dtlIndex + 1).toUInt(),
                        "MstId" to mstId.toUInt(),
                        "Time" to data.timestamp,
                        "Azimuth" to data.azimuth,
                        "Elevation" to data.elevation,
                        "Range" to data.range,
                        "Altitude" to data.altitude,
                        "DataType" to "original"
                    )
                )
            }
        }

        logger.info("📊 1단계 완료: 원본 데이터 생성 - ${ephemerisTrackMst.size}개 마스터, ${ephemerisTrackDtl.size}개 세부")
        return Pair(ephemerisTrackMst, ephemerisTrackDtl)
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


    // Tilt만 0으로 이동
    private fun moveTrainToZero(TrainAngle: Float) {
        val multiAxis = BitSet()
        multiAxis.set(2)  // Tilt 축만 활성화
        PushData.CMD.cmdTrainAngle = GlobalData.Offset.trainPositionOffset
        udpFwICDService.singleManualCommand(
            multiAxis, TrainAngle, 5f
        )

        logger.info("🔄 TrainAngle를 ${TrainAngle} 도로 이동 시작)")
    }

    // 목표 Az/El로 이동
    private fun moveToTargetAzEl() {
        GlobalData.EphemerisTrakingAngle.azimuthAngle = targetAzimuth
        GlobalData.EphemerisTrakingAngle.elevationAngle = targetElevation
        val multiAxis = BitSet()
        multiAxis.set(0)  // Azimuth
        multiAxis.set(1)  // Elevation
        udpFwICDService.multiManualCommand(
            multiAxis, targetAzimuth, 5f, targetElevation, 5f, 0f, 0f
        )
        logger.info("🔄 목표 Az/El로 이동: Az=${targetAzimuth}°, El=${targetElevation}°")
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

    fun startEphemerisTracking(passId: UInt) {
        logger.info("🚀 위성 추적 시작: 패스 ID = {}", passId)
        stopModeTimer()
        executedActions.clear()
        logger.info("🔄 실행 플래그 초기화 완료")
        currentTrackingPassId = passId
        currentTrackingPass = ephemerisTrackMstStorage.find { it["No"] == passId }
        if (currentTrackingPass == null) {
            logger.error("패스 ID {}에 해당하는 데이터를 찾을 수 없습니다", passId)
            return
        }
        logger.info("✅ ephemeris 추적 준비 완료 (실제 추적 시작 전)")
        // 상태머신 진입
        moveToStartPosition(passId)
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

        // ✅ 정지궤도 추적 상태 초기화
        if (trackingStatus.geostationaryStatus == true) {
            trackingStatus.geostationaryStatus = false
        }

        // ✅ ephemeris 상태도 초기화 (내부 상태 + 프론트엔드 전달)
        if (trackingStatus.ephemerisStatus == true) {
            trackingStatus.ephemerisStatus = false
            trackingStatus.ephemerisTrackingState = "IDLE"  // ✅ 추가
        }
        dataStoreService.setEphemerisTracking(false) // ✅ 프론트엔드에 추적 종료 알림

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

        // ✅ 위성 추적 시작 상태 설정
        trackingStatus.ephemerisStatus = true
        trackingStatus.ephemerisTrackingState = "TRAIN_MOVING_TO_ZERO"
        logger.info("🚀 위성 추적 시작 - Tilt 시작 위치로 이동")

        // ✅ 통합 모드 실행기 사용
        modeExecutor = threadManager.getModeExecutor()

        // ✅ 안정성 우선 스케줄링
        modeTask = modeExecutor?.scheduleAtFixedRate(
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
        return modeExecutor != null && modeTask != null && !modeTask!!.isCancelled
    }

    /**
     * ✅ 모드 타이머 상세 상태 정보 (새로운 메서드)
     */
    fun getTimerStatus(): Map<String, Any> {
        val executor = modeExecutor
        val task = modeTask

        return mapOf(
            "isRunning" to isTimerRunning(),
            "executorExists" to (executor != null),
            "taskExists" to (task != null),
            "taskCancelled" to (task?.isCancelled ?: true),
            "taskDone" to (task?.isDone ?: true),
            "threadName" to "mode-worker"
        )
    }

    /**
     * 100ms 주기 상태 체크 (핵심 로직)
     */
    private fun trackingSatelliteStateCheck() {
        try {
            // ✅ Offset 값 변경 감지 및 CMD 값 업데이트 로직 추가
            //checkAndApplyPositionOffsets()
            if (trackingStatus.ephemerisStatus != true) {
                return
            }
            when (currentTrackingState) {
                TrackingState.MOVING_TRAIN_TO_ZERO -> {
                    // ✅ Tilt 시작 위치로 이동 상태 표시
                    trackingStatus.ephemerisTrackingState = "TRAIN_MOVING_TO_ZERO"
                    var trainAngle = 0f
                    GlobalData.EphemerisTrakingAngle.trainAngle = trainAngle
                    moveTrainToZero(trainAngle)
                    if (isTrainAtZero()) {
                        currentTrackingState = TrackingState.WAITING_FOR_TRAIN_STABILIZATION
                        stabilizationStartTime = System.currentTimeMillis()
                        // ✅ Tilt 0도 이동 완료, 안정화 대기 상태로 업데이트
                        trackingStatus.ephemerisTrackingState = "TRAIN_STABILIZING"
                        logger.info("✅ Train가 0도에 도달, 안정화 대기 시작")
                    }
                }

                TrackingState.WAITING_FOR_TRAIN_STABILIZATION -> {
                    // ✅ Tilt 안정화 대기 상태 표시
                    trackingStatus.ephemerisTrackingState = "TRAIN_STABILIZING"

                    if (System.currentTimeMillis() - stabilizationStartTime >= TRAIN_STABILIZATION_TIMEOUT && isTrainStabilized()) {
                        moveToTargetAzEl()
                        currentTrackingState = TrackingState.MOVING_TO_TARGET
                        logger.info("✅ TRAIN 안정화 완료, 목표 Az/El로 이동 시작")
                    }
                }

                TrackingState.MOVING_TO_TARGET -> {
                    // 목표 위치 도달 체크는 생략(즉시 활성화)
                    currentTrackingState = TrackingState.TRACKING_ACTIVE
                    // ✅ 목표 위치 이동 완료, 시작 위치 이동 상태로 업데이트
                    trackingStatus.ephemerisTrackingState = "MOVING_TO_START"
                    logger.info("✅ 목표 위치 이동 완료, 시작 위치 이동 상태")

                    // ✅ 추적 대기 상태 추가 (다음 상태 체크에서 처리)
                    logger.info("⏳ 위성 추적 대기 상태로 전환 준비")
                }

                TrackingState.TRACKING_ACTIVE -> {
                    // ✅ 정지궤도와 저궤도 구분 처리
                    if (trackingStatus.geostationaryStatus == true) {
                        // 정지궤도: 현재시간 1포인트 추적 (추가 동작 없음)
                        logger.debug("🔄 정지궤도 추적 활성 상태 유지")
                    } else {
                        // 저궤도: 시간 기반 스케줄 추적
                        val passId = currentTrackingPassId
                        if (passId == null) {
                            logger.warn("현재 추적 중인 패스 ID가 설정되지 않았습니다.")
                            return
                        }
                        val (startTime, endTime) = getCurrentTrackingPassTimes()
                        val calTime = GlobalData.Time.calUtcTimeOffsetTime
                        val timeDifference = Duration.between(startTime, calTime).seconds
                        logger.debug("⏰ 상태체크 - 시간차: {}초, 실행완료: {}", timeDifference, executedActions)

                        // ✅ 추적 대기 상태 표시 (실제 추적 시작 전)
                        if (!executedActions.contains("WAITING_FOR_TRACKING")) {
                            trackingStatus.ephemerisTrackingState = "WAITING_FOR_TRACKING"
                            logger.info("⏳ 위성 추적 대기 상태")
                            executedActions.add("WAITING_FOR_TRACKING") // ✅ 중복 방지
                        }

                        when {
                            timeDifference <= 0 && !executedActions.contains("BEFORE_START") -> {
                                executedActions.add("BEFORE_START")
                                logger.info("📍 시작 전 처리 실행 - 시작 위치로 이동(상태머신)")
                            }

                            timeDifference > 0 && calTime.isBefore(endTime) -> {
                                if (!executedActions.contains("IN_PROGRESS")) {
                                    executedActions.add("IN_PROGRESS")
                                    logger.info("📡 추적 진행 중 처리 실행 - 데이터 전송 시작")
                                    handleInProgress(passId)
                                }
                                saveRealtimeTrackingData(passId, calTime, startTime)
                                //moveTiltToZero(GlobalData.Offset.tiltPositionOffset+ GlobalData.Offset.trueNorthOffset)

                            }

                            calTime.isAfter(endTime) && !executedActions.contains("COMPLETED") -> {
                                executedActions.add("COMPLETED")
                                logger.info("✅ 추적 완료 처리 실행")
                                handleCompleted()
                            }

                            else -> {
                                logger.debug("⏸️ 대기 중 또는 이미 처리됨")
                            }
                        }
                    }
                }

                else -> {}
            }
        } catch (e: Exception) {
            logger.error("추적 상태 체크 오류: ${e.message}", e)
        }
    }

    /**
     * 추적 시작 전 처리
     */
    private fun handleBeforeStart(passId: UInt) {
        logger.info("📍 시작 전 상태 - 시작 위치로 이동")
        moveToStartPosition(passId)

    }

    /**                                                                        R
     * 추적 진행 중 처리
     */
    private fun handleInProgress(passId: UInt) {
        logger.info("📡 진행 중 상태 - 추적 데이터 전송 시작")
        trackingStatus.ephemerisTrackingState = "TRACKING"  // ✅ 추가
        dataStoreService.setEphemerisTracking(true)
        sendHeaderTrackingData(passId)
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
        logger.info("🔄 공통 추적 상태 초기화 완료")
    }

    /**
     * ✅ 배치 처리를 사용한 실시간 추적 데이터 저장
     */
    private fun saveRealtimeTrackingData(passId: UInt, currentTime: ZonedDateTime, startTime: ZonedDateTime) {
        try {
            // ✅ 실시간 추적 데이터 생성
            val realtimeData = createRealtimeTrackingData(passId, currentTime, startTime)

            // ✅ 배치 처리로 변경
            batchStorageManager.addToBatch(realtimeData)

            // ✅ 주기적 로깅 (배치 상태 포함)
            if (trackingDataIndex % 100 == 0) {
                val batchStatus = batchStorageManager.getBatchStatus()
                logger.info(
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
     * ✅ 실시간 추적 데이터 생성 (개선된 버전 - 시간 기반 인덱스 매칭)
     */
    private fun createRealtimeTrackingData(
        passId: UInt,
        currentTime: ZonedDateTime,
        startTime: ZonedDateTime
    ): Map<String, Any?> {
        val elapsedTimeSeconds = Duration.between(startTime, currentTime).toMillis() / 1000.0f

        // 1. 이론치 데이터 타입별로 분리해서 가져오기
        val originalPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "original")
        val axisTransformedPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "axis_transformed")
        val finalTransformedPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "final_transformed")

        if (originalPassDetails.isEmpty()) {
            logger.debug("원본 이론치 데이터가 없어 실시간 데이터 저장을 건너뜁니다.")
            return emptyMap()
        }

        // 2. ✅ 시간 기반으로 정확한 이론치 인덱스 계산
        val timeDifferenceMs = Duration.between(startTime, currentTime).toMillis()
        val theoreticalIndex = (timeDifferenceMs / 100.0).toInt().coerceIn(0, originalPassDetails.size - 1)

        // 3. ✅ 해당 인덱스의 실제 이론치 데이터 가져오기 (보간 없이 직접 매칭)
        val theoreticalPoint = if (theoreticalIndex < originalPassDetails.size) {
            originalPassDetails[theoreticalIndex]
        } else {
            originalPassDetails.last()
        }

        val theoreticalAxisPoint = if (theoreticalIndex < axisTransformedPassDetails.size) {
            axisTransformedPassDetails[theoreticalIndex]
        } else {
            axisTransformedPassDetails.last()
        }

        val theoreticalFinalPoint = if (theoreticalIndex < finalTransformedPassDetails.size) {
            finalTransformedPassDetails[theoreticalIndex]
        } else {
            finalTransformedPassDetails.last()
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

        val finalTransformedAzimuth = (theoreticalFinalPoint["Azimuth"] as? Double)?.toFloat() ?: axisTransformedAzimuth
        val finalTransformedElevation =
            (theoreticalFinalPoint["Elevation"] as? Double)?.toFloat() ?: axisTransformedElevation
        val finalTransformedRange = (theoreticalFinalPoint["Range"] as? Double)?.toFloat() ?: axisTransformedRange
        val finalTransformedAltitude =
            (theoreticalFinalPoint["Altitude"] as? Double)?.toFloat() ?: axisTransformedAltitude

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

            // ✅ 최종 변환 데이터 (±270도 제한 적용)
            "finalTransformedAzimuth" to finalTransformedAzimuth,
            "finalTransformedElevation" to finalTransformedElevation,
            "finalTransformedRange" to finalTransformedRange,
            "finalTransformedAltitude" to finalTransformedAltitude,

            // ✅ 실제 추적 데이터
            "cmdAz" to finalTransformedAzimuth,  // 최종 변환 데이터를 명령으로 사용
            "cmdEl" to finalTransformedElevation,
            "actualAz" to currentData.azimuthAngle,
            "actualEl" to currentData.elevationAngle,

            "elapsedTimeSeconds" to elapsedTimeSeconds,
            "trackingAzimuthTime" to trackingCmdAzimuthTime,
            "trackingCMDAzimuthAngle" to trackingCmdAzimuth,
            "trackingActualAzimuthAngle" to trackingActualAzimuth,
            "trackingElevationTime" to trackingCmdElevationTime,
            "trackingCMDElevationAngle" to trackingCmdElevation,
            "trackingActualElevationAngle" to trackingActualElevation,
            "trackingTrainTime" to trackingCmdTrainTime,
            "trackingCMDTrainAngle" to trackingCmdTrain,
            "trackingActualTrainAngle" to trackingActualTrain,
            "passId" to passId,

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
            trackingData.forEach { (key, value) ->
                logger.info("    - {}: {}", key, value)
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
     */
    private fun moveToStartPosition(passId: UInt) {
        val passDetails = getEphemerisTrackDtlByMstId(passId)
        if (passDetails.isNotEmpty()) {
            val startPoint = passDetails.first()
            targetAzimuth = (startPoint["Azimuth"] as Double).toFloat()
            targetElevation = (startPoint["Elevation"] as Double).toFloat()
            // 상태머신 진입
            currentTrackingState = TrackingState.MOVING_TRAIN_TO_ZERO
            // ✅ Tilt 시작 위치로 이동 상태는 이미 startModeTimer()에서 설정됨
        }
    }

    /**
     * 위성 추적 시작 - 헤더 정보 전송
     * 2.12.1 위성 추적 해더 정보 송신 프로토콜 사용
     */
    fun sendHeaderTrackingData(passId: UInt) {
        try {
            udpFwICDService.writeNTPCommand()
            currentTrackingPassId = passId
            // 선택된 패스 ID에 해당하는 마스터 데이터 찾기
            val selectedPass = ephemerisTrackMstStorage.find { it["No"] == passId }
            // 시작 방위각과 고도각 가져오기

            if (selectedPass == null) {
                logger.error("선택된 패스 ID($passId)에 해당하는 데이터를 찾을 수 없습니다.")
                return
            }
            // 현재 추적 중인 패스 설정
            currentTrackingPass = selectedPass

            // 패스 시작 및 종료 시간 가져오기
            val startTime = (selectedPass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
            val endTime = (selectedPass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)

            // 시작 시간과 종료 시간을 문자열로 변환 (밀리초 포함)
            logger.info("위성 추적 시작: ${selectedPass["SatelliteName"]} (패스 ID: $passId)")
            logger.info("시작 시간: $startTime, 종료 시간: $endTime")

            // 밀리초 추출
            val startTimeMs = (startTime.nano / 1_000_000).toUShort()
            val endTimeMs = (endTime.nano / 1_000_000).toUShort()

            // 전체 데이터 길이 검증
            val totalLength = calculateDataLength(passId)
            val actualDataCount = getEphemerisTrackDtlByMstId(passId).size
            logger.info("전체 데이터 길이: ${totalLength}개")
            logger.info("실제 데이터 개수: ${actualDataCount}개")

            if (totalLength != actualDataCount) {
                logger.warn("데이터 길이 불일치: 계산된 길이=${totalLength}, 실제 길이=${actualDataCount}")
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
            logger.info("위성 추적 전체 길이 ${calculateDataByteSize(passId).toUShort()}")
            logger.info("위성 추적 헤더 정보 전송 완료")

            dataStoreService.setEphemerisTracking(true)


        } catch (e: Exception) {
            dataStoreService.setEphemerisTracking(false)
            logger.error("위성 추적 시작 중 오류 발생: ${e.message}", e)
        }
    }

    /**
     * 위성 추적 초기 제어 명령 전송
     * 2.12.2 위성 추적 초기 제어 명령 프로토콜 사용
     */
    fun sendInitialTrackingData(passId: UInt) {
        try {
            if (currentTrackingPass == null || trackingStatus.ephemerisStatus != true) {
                logger.error("위성 추적이 시작되지 않았습니다. 먼저 startSatelliteTracking을 호출하세요.")
                return
            }
            var initialTrackingData: List<Triple<UInt, Float, Float>> = emptyList()
            val passDetails = getEphemerisTrackDtlByMstId(passId)

            // ✅ 시간 정보 가져오기
            val (startTime, endTime) = getCurrentTrackingPassTimes()
            val calTime = GlobalData.Time.calUtcTimeOffsetTime

            val timeStatus = checkTimeInTrackingRange(calTime, startTime, endTime)
            when (timeStatus) {
                TimeRangeStatus.IN_RANGE -> {
                    logger.info("🎯 현재 시간이 추적 범위 내에 있습니다 - 실시간 추적 모드")

                    // 정상 추적 로직
                    // ✅ 실시간 추적: 현재 시간에 정확히 맞는 데이터 추출
                    val timeDifferenceMs = Duration.between(startTime, calTime).toMillis()
                    val calculatedIndex = (timeDifferenceMs / 100).toInt()

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
                logger.error("선택된 패스 ID($passId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
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
     */
    fun handleEphemerisTrackingDataRequest(timeAcc: UInt, requestDataLength: UShort) {
        if (trackingStatus.ephemerisStatus != true || currentTrackingPass == null) {
            logger.error("위성 추적이 활성화되어 있지 않습니다.")
            return
        }
        logger.info("timeAcc :${timeAcc}.")
        logger.info("requestDataLength :${requestDataLength}.")
        val passId = currentTrackingPass!!["No"] as UInt

        // timeAcc를 기반으로 시작 인덱스 계산 (timeAcc는 ms 단위)
        val startIndex = (timeAcc.toInt()) //
        logger.info("startIndex :${startIndex}.")
        // 요청된 데이터 길이에 따라 데이터 포인트 수 계산
        sendAdditionalTrackingData(passId, startIndex, requestDataLength.toInt())
        //dataStoreService.setEphemerisTracking(true)
    }

    /**
     * 위성 추적 추가 데이터 전송
     * 2.12.3 위성 추적 추가 데이터 요청에 대한 응답으로 사용
     */
    fun sendAdditionalTrackingData(passId: UInt, startIndex: Int, requestDataLength: Int = 25) {
        try {
            if (currentTrackingPass == null || trackingStatus.ephemerisStatus != true) {
                logger.error("위성 추적이 시작되지 않았습니다. 먼저 startSatelliteTracking을 호출하세요.")
                return
            }
            logger.info("startIndex :${startIndex}.")
            // 선택된 패스 ID에 해당하는 세부 데이터 가져오기
            val passDetails = getEphemerisTrackDtlByMstId(passId)

            if (passDetails.isEmpty()) {
                logger.error("선택된 패스 ID($passId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
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
            // 현재 추적 중인 패스가 있을 때만 초기 데이터 전송
            currentTrackingPassId?.let { passId ->
                logger.info("추적 중인 패스 발견, 초기 데이터 전송 시작: passId={}", passId)
                sendInitialTrackingData(passId)
                logger.info("초기 추적 데이터 전송 완료: passId={}", passId)
            } ?: run {
                logger.warn("현재 추적 중인 패스가 없어서 초기 데이터를 전송하지 않습니다")
            }
            //Time Offset 전달
            udpFwICDService.timeOffsetCommand(inputTimeOffset)
            // 글로벌 데이터 업데이트


            logger.info("TimeOffset 명령 전송 완료: {}s", inputTimeOffset)
        }.subscribeOn(Schedulers.boundedElastic()).subscribe({ /* 성공 */ }, { error ->
            logger.error("시간 오프셋 명령 처리 오류: {}", error.message, error)
        })
    }

    fun setCurrentTrackingPassId(newPassId: UInt?) {
        // 유효성 검사
        if (newPassId != null && newPassId <= 0u) {
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
     */
    private fun getFirstAzimuthForPass(passId: UInt): Float {
        val passDetails = getEphemerisTrackDtlByMstId(passId)
        return if (passDetails.isNotEmpty()) {
            (passDetails.first()["Azimuth"] as Double).toFloat()
        } else {
            0.0f
        }
    }

    /**
     * 패스의 첫 번째 고도각 가져오기
     */
    private fun getFirstElevationForPass(passId: UInt): Float {
        val passDetails = getEphemerisTrackDtlByMstId(passId)
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
     */
    private fun calculateDataByteSize(passId: UInt): Int {
        val passDetails = getEphemerisTrackDtlByMstId(passId)
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
     */
    private fun calculateDataLength(passId: UInt): Int {
        val passDetails = getEphemerisTrackDtlByMstId(passId)
        logger.info("전체 데이터 길이 계산 시작: 패스 ID = $passId , 사이즈 : ${passDetails.size}")
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
     * 특정 마스터 ID에 해당하는 세부 추적 데이터 조회 (최종 변환된 데이터만)
     * 축변환 후 ±270도 제한이 적용된 최종 데이터를 조회합니다.
     */
    fun getEphemerisTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
        return ephemerisTrackDtlStorage.filter {
            it["MstId"] == mstId && it["DataType"] == "final_transformed"
        }
    }

    /**
     * 특정 마스터 ID에 해당하는 원본 세부 추적 데이터 조회
     */
    fun getOriginalEphemerisTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
        return ephemerisTrackDtlStorage.filter {
            it["MstId"] == mstId && it["DataType"] == "original"
        }
    }

    /**
     * 특정 마스터 ID에 해당하는 방위각 변환 세부 추적 데이터 조회
     */
    fun getAngleLimitedEphemerisTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
        return ephemerisTrackDtlStorage.filter {
            it["MstId"] == mstId && it["DataType"] == "angle_limited"
        }
    }

    /**
     * 특정 마스터 ID에 해당하는 모든 데이터 타입의 세부 추적 데이터 조회
     */
    fun getAllEphemerisTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
        return ephemerisTrackDtlStorage.filter { it["MstId"] == mstId }
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
     */
    fun getEphemerisTrackDtlByMstIdAndDataType(mstId: UInt, dataType: String): List<Map<String, Any?>> {
        return ephemerisTrackDtlStorage.filter {
            it["MstId"] == mstId && it["DataType"] == dataType
        }
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
            val allMstIds = getAllEphemerisTrackMst().map { it["No"] as UInt }
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
                    val result = exportMstDataToCsv(mstId.toInt(), outputDirectory)
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
     * 📊 특정 MST ID의 데이터를 CSV 파일로 추출
     * 원본, 축변환, 최종 변환 데이터를 매칭하여 하나의 CSV 파일로 생성
     */
    fun exportMstDataToCsv(mstId: Int, outputDirectory: String = "csv_exports"): Map<String, Any?> {
        try {
            logger.info("📊 MST ID $mstId CSV 파일 생성 시작")
            val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "original")
            val axisTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "axis_transformed")
            val finalTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "final_transformed")
            if (originalDtl.isEmpty()) {
                logger.error("❌ MST ID $mstId 의 원본 데이터를 찾을 수 없습니다")
                return mapOf<String, Any?>("success" to false, "error" to "원본 데이터를 찾을 수 없습니다")
            }
            val mstInfo = getAllEphemerisTrackMst().find { it["No"] == mstId.toUInt() }
            val satelliteName = mstInfo?.get("SatelliteName") as? String ?: "Unknown"
            val startTime = mstInfo?.get("StartTime") as? java.time.ZonedDateTime
            val endTime = mstInfo?.get("EndTime") as? java.time.ZonedDateTime
            val timestamp =
                java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val filename = "MST${mstId}_${satelliteName}_${timestamp}.csv"
            val filePath = "$outputDirectory/$filename"
            java.io.FileWriter(filePath).use { writer ->
                writer.write("Index,Time,Original_Azimuth,Original_Elevation,Original_Range,Original_Altitude,")
                writer.write("AxisTransformed_Azimuth,AxisTransformed_Elevation,AxisTransformed_Range,AxisTransformed_Altitude,")
                writer.write("FinalTransformed_Azimuth,FinalTransformed_Elevation,FinalTransformed_Range,FinalTransformed_Altitude,")
                writer.write("Azimuth_Transformation_Error,Elevation_Transformation_Error\n")
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
                    val axisTransformedRange = axisTransformedPoint?.get("Range") as? Double ?: 0.0
                    val axisTransformedAltitude = axisTransformedPoint?.get("Altitude") as? Double ?: 0.0
                    val finalTransformedAz = finalTransformedPoint?.get("Azimuth") as? Double ?: 0.0
                    val finalTransformedEl = finalTransformedPoint?.get("Elevation") as? Double ?: 0.0
                    val finalTransformedRange = finalTransformedPoint?.get("Range") as? Double ?: 0.0
                    val finalTransformedAltitude = finalTransformedPoint?.get("Altitude") as? Double ?: 0.0
                    val azimuthTransformationError = axisTransformedAz - originalAz
                    val elevationTransformationError = axisTransformedEl - originalEl
                    val timeString =
                        originalTime?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                            ?: ""
                    writer.write("$i,$timeString,")
                    writer.write(
                        "${String.format("%.6f", originalAz)},${
                            String.format(
                                "%.6f",
                                originalEl
                            )
                        },${String.format("%.6f", originalRange)},${String.format("%.6f", originalAltitude)},"
                    )
                    writer.write(
                        "${String.format("%.6f", axisTransformedAz)},${
                            String.format(
                                "%.6f",
                                axisTransformedEl
                            )
                        },${String.format("%.6f", axisTransformedRange)},${
                            String.format(
                                "%.6f",
                                axisTransformedAltitude
                            )
                        },"
                    )
                    writer.write(
                        "${String.format("%.6f", finalTransformedAz)},${
                            String.format(
                                "%.6f",
                                finalTransformedEl
                            )
                        },${String.format("%.6f", finalTransformedRange)},${
                            String.format(
                                "%.6f",
                                finalTransformedAltitude
                            )
                        },"
                    )
                    writer.write(
                        "${String.format("%.6f", azimuthTransformationError)},${
                            String.format(
                                "%.6f",
                                elevationTransformationError
                            )
                        }\n"
                    )
                }
            }
            logger.info("📊 MST ID $mstId CSV 파일 생성 완료: $filePath")
            logger.info("  - 원본 데이터: ${originalDtl.size}개")
            logger.info("  - 축변환 데이터: ${axisTransformedDtl.size}개")
            logger.info("  - 최종 변환 데이터: ${finalTransformedDtl.size}개")
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
     */
    fun exportMstDataToSimpleCsv(mstId: Int, outputDirectory: String = "csv_exports"): Map<String, Any?> {
        try {
            logger.info("📊 MST ID $mstId 간단 CSV 파일 생성 시작")
            val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "original")
            if (originalDtl.isEmpty()) {
                logger.error("❌ MST ID $mstId 의 원본 데이터를 찾을 수 없습니다")
                return mapOf<String, Any?>("success" to false, "error" to "원본 데이터를 찾을 수 없습니다")
            }
            val mstInfo = getAllEphemerisTrackMst().find { it["No"] == mstId.toUInt() }
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
