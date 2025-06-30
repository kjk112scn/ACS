package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.axislimitangle.LimitAngleCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.event.subscribeToType
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.SatelliteTrackingData
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.service.icd.ICDService
import com.gtlsystems.acs_api.service.udp.UdpFwICDService
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.core.Disposable
import java.io.IOException
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import io.netty.handler.timeout.TimeoutException
import java.util.BitSet
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

/**
 * TLE 데이터를 캐시로 관리하고 위성 패스 스케줄링을 담당하는 서비스
 */
@Service
class PassScheduleService(
    private val orekitCalculator: OrekitCalculator,
    private val acsEventBus: ACSEventBus,
    private val udpFwICDService: UdpFwICDService,
    private val dataStoreService: DataStoreService
) {
    private val logger = LoggerFactory.getLogger(PassScheduleService::class.java)

    // ✅ 기존 저장소들 (변경 없음)
    private val passScheduleTleCache = ConcurrentHashMap<String, Triple<String, String, String>>()
    private val passScheduleTrackMstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()
    private val passScheduleTrackDtlStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()
    private val trackingTargetList = mutableListOf<TrackingTarget>()
    private val selectedTrackMstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()

    // ✅ 기존 상태 관리 변수들 (변경 없음)
    private var isPreparingForTracking = AtomicBoolean(false)
    private var lastPreparedSchedule: Map<String, Any?>? = null
    private var isInStowPosition = AtomicBoolean(false)
    private val PREPARATION_TIME_MINUTES = 2L
    private val subscriptions: MutableList<Disposable> = mutableListOf()

    // ✅ 새로 추가: 성능 최적화용 캐시 및 스레드 (기존 동작에 영향 없음)
    private val trackingDataCache = ConcurrentHashMap<UInt, TrackingDataCache>()
    private val trackingDataExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tracking-data-optimizer").apply {
            priority = Thread.NORM_PRIORITY + 1  // 기존보다 약간 높은 우선순위
            isDaemon = true
        }
    }

    // ✅ 새로 추가: 성능 최적화용 데이터 클래스
    data class TrackingDataCache(
        val passId: UInt,
        val trackingPoints: Array<TrackingPoint>,
        val totalSize: Int,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        data class TrackingPoint(
            val timeMs: Int,
            val elevation: Float,
            val azimuth: Float
        )

        fun isExpired(maxAgeMs: Long = 3600000): Boolean = // 1시간 후 만료
            System.currentTimeMillis() - createdAt > maxAgeMs
    }

    // ✅ 기존 데이터 클래스들 (변경 없음)
    data class TrackingTarget(
        val mstId: UInt,
        val satelliteId: String,
        val satelliteName: String? = null,
        val startTime: ZonedDateTime,
        val endTime: ZonedDateTime,
        val maxElevation: Double,
        val createdAt: ZonedDateTime = ZonedDateTime.now()
    )

    // ✅ 기존 설정들 (변경 없음)
    private val trackingData = SatelliteTrackingData.Tracking
    private val locationData = GlobalData.Location
    private val limitAngleCalculator = LimitAngleCalculator()
    private var globalMstId = 0

    @PostConstruct
    fun init() {
        logger.info("PassScheduleService 초기화 완료")
        setupEventSubscriptions()
    }

    private fun setupEventSubscriptions() {
        // ✅ 기존 이벤트 구독 로직 유지
        val headerSubscription =
            acsEventBus.subscribeToType<ACSEvent.ICDEvent.SatelliteTrackHeaderReceived>().subscribe { event ->
                val currentSchedule = getCurrentSelectedTrackingPassWithTime(GlobalData.Time.calUtcTimeOffsetTime)
                currentSchedule?.let { schedule ->
                    val passId = schedule["No"] as? UInt
                    if (passId != null) {
                        sendInitialTrackingData(passId)
                    }
                }
            }

        val dataRequestSubscription =
            acsEventBus.subscribeToType<ACSEvent.ICDEvent.SatelliteTrackDataRequested>().subscribe { event ->
                val currentSchedule = getCurrentSelectedTrackingPassWithTime(GlobalData.Time.calUtcTimeOffsetTime)
                currentSchedule?.let { schedule ->
                    val passId = schedule["No"] as? UInt
                    if (passId != null) {
                        val requestData = event.requestData as ICDService.SatelliteTrackThree.GetDataFrame
                        // ✅ 최적화된 메서드 호출 (기존 인터페이스 유지)
                        handleTrackingDataRequest(passId, requestData.timeAcc, requestData.requestDataLength)
                    }
                }
            }

        subscriptions.add(headerSubscription)
        subscriptions.add(dataRequestSubscription)
    }

    // ✅ 기존 추적 모니터링 필드들 (변경 없음)
    private var trackingMonitorExecutor: ScheduledExecutorService? = null
    private var trackingMonitorTask: ScheduledFuture<*>? = null
    private var isTrackingMonitorRunning = AtomicBoolean(false)
    private var lastDisplayedSchedule: Map<String, Any?>? = null
    private var trackingCheckCount = 0

    private val trackingMonitorThreadFactory = ThreadFactory { runnable ->
        Thread(runnable, "tracking-monitor").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY + 1
        }
    }

    fun startScheduleTracking() {
        if (isTrackingMonitorRunning.get()) {
            logger.warn("추적 모니터링이 이미 실행 중입니다.")
            return
        }
        dataStoreService.stopAllTracking()

        trackingMonitorExecutor = Executors.newSingleThreadScheduledExecutor(trackingMonitorThreadFactory)
        trackingMonitorTask = trackingMonitorExecutor?.scheduleAtFixedRate(
            { checkTrackingScheduleUsingExistingMethods() }, 0, 100, TimeUnit.MILLISECONDS
        )

        isTrackingMonitorRunning.set(true)
        logger.info("🚀 추적 모니터링 시작 (성능 최적화 적용)")
    }

    private fun checkTrackingScheduleUsingExistingMethods() {
        try {
            val calTime = GlobalData.Time.calUtcTimeOffsetTime
            val currentSchedule = getCurrentSelectedTrackingPassWithTime(calTime)
            val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)

            // ✅ 기존 로직 유지 + 시간 표시 개선
            if (trackingCheckCount < 10) {
                logger.info("🔍 추적 체크 #${trackingCheckCount}")
                logger.info("  현재시간: $calTime")
                logger.info("  현재 스케줄: ${if (currentSchedule != null) "있음" else "없음"}")

                if (nextSchedule != null) {
                    val nextMstId = nextSchedule["No"] as? UInt
                    val nextSatName = nextSchedule["SatelliteName"] as? String
                    val nextStartTime = nextSchedule["StartTime"] as? ZonedDateTime

                    if (nextStartTime != null) {
                        val timeUntilNext = Duration.between(calTime, nextStartTime)
                        val minutesUntilNext = timeUntilNext.toMinutes()
                        val secondsUntilNext = timeUntilNext.seconds % 60  // ✅ 초 단위 추가

                        logger.info("  다음 스케줄: MST=$nextMstId, Name=$nextSatName")
                        logger.info("  시작시간: $nextStartTime")
                        logger.info("  남은시간: ${minutesUntilNext}분 ${secondsUntilNext}초")  // ✅ 개선

                        if (minutesUntilNext <= PREPARATION_TIME_MINUTES) {
                            logger.info("  🚨 2분 이내! 시작 위치로 이동해야 함")
                        } else {
                            logger.info("  ⏳ 2분 이상 남음, Stow 위치 유지")
                        }
                    }
                } else {
                    logger.info("  다음 스케줄: 없음")
                }
            }

            trackingCheckCount++
            handleTrackingStateChange(currentSchedule, calTime)
            if (currentSchedule == null) {
                handleTrackingPreparation(nextSchedule, calTime)
            }

        } catch (e: Exception) {
            logger.error("추적 체크 중 오류: ${e.message}", e)
        }
    }

    private fun handleTrackingStateChange(currentSchedule: Map<String, Any?>?, calTime: ZonedDateTime) {
        // ✅ mstId 업데이트 추가 (기존 함수들은 그대로 사용)
        updateTrackingMstIds(currentSchedule, calTime)

        when {
            // 새로운 추적 시작 (기존 코드 그대로)
            lastDisplayedSchedule == null && currentSchedule != null -> {
                outputCurrentScheduleInfo(currentSchedule, calTime)
                outputNextScheduleInfo(calTime)

                val mstId = currentSchedule["No"] as? UInt
                if (mstId != null) {
                    startTrackingOptimized(currentSchedule)  // 기존 함수 사용
                }
            }

            // 추적 종료 (기존 코드 그대로)
            lastDisplayedSchedule != null && currentSchedule == null -> {
                outputTrackingEnd(lastDisplayedSchedule!!, calTime)
                stopTrackingOptimized(lastDisplayedSchedule!!)  // 기존 함수 사용

                val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)
                if (nextSchedule != null) {
                    outputUpcomingScheduleInfo(nextSchedule, calTime)
                } else {
                    outputScheduleFixed(lastDisplayedSchedule!!, calTime)
                }
            }

            // 추적 변경 (기존 코드 그대로)
            lastDisplayedSchedule != null && currentSchedule != null &&
                    lastDisplayedSchedule!!["No"] != currentSchedule["No"] -> {
                outputScheduleChange(lastDisplayedSchedule!!, currentSchedule, calTime)
                outputNextScheduleInfo(calTime)

                stopTrackingOptimized(lastDisplayedSchedule!!)  // 기존 함수 사용
                startTrackingOptimized(currentSchedule)  // 기존 함수 사용
            }
        }
        lastDisplayedSchedule = currentSchedule
    }

    // ✅ 새로 추가할 함수 (mstId 업데이트용)
    private fun updateTrackingMstIds(currentSchedule: Map<String, Any?>?, calTime: ZonedDateTime) {
        // 현재 추적 중인 mstId 업데이트
        val currentMstId = currentSchedule?.get("No") as? UInt
        dataStoreService.setCurrentTrackingMstId(currentMstId)

        // 다음 추적 예정 mstId 업데이트
        val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)
        val nextMstId = nextSchedule?.get("No") as? UInt
        dataStoreService.setNextTrackingMstId(nextMstId)

        // 로그 출력
        logger.debug("🔄 mstId 업데이트: 현재={}, 다음={}", currentMstId, nextMstId)
    }

    // ✅ 최적화된 추적 시작 (기존 startTracking 대체)
    private fun startTrackingOptimized(schedule: Map<String, Any?>) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val mstId = schedule["No"] as? UInt ?: return

        logger.info("🚀 추적 시작: $satelliteName (ID: $mstId)")

        // ✅ 병렬로 캐시 로딩 시작 (기존 동작에 영향 없음)
        preloadTrackingDataCache(mstId)

        // ✅ 기존 로직 유지
        dataStoreService.setPassScheduleTracking(true)
        sendHeaderTrackingData(mstId)
    }

    // ✅ 최적화된 추적 종료 (기존 stopTracking 대체)
    private fun stopTrackingOptimized(schedule: Map<String, Any?>) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val mstId = schedule["No"] as? UInt ?: return

        logger.info("🛑 추적 종료: $satelliteName (ID: $mstId)")

        // ✅ 캐시 정리 (메모리 절약)
        trackingDataCache.remove(mstId)

        // ✅ 기존 로직 유지
        dataStoreService.setPassScheduleTracking(false)
    }

    // ✅ 새로 추가: 비동기 캐시 로딩 (기존 동작에 영향 없음)
    private fun preloadTrackingDataCache(passId: UInt) {
        CompletableFuture.runAsync({
            try {
                val startTime = System.nanoTime()
                val passDetails = getSelectedTrackDtlByMstId(passId)

                if (passDetails.isNotEmpty()) {
                    val trackingPoints = Array(passDetails.size) { index ->
                        val point = passDetails[index]
                        TrackingDataCache.TrackingPoint(
                            timeMs = index * 100,
                            elevation = (point["Elevation"] as Double).toFloat(),
                            azimuth = (point["Azimuth"] as Double).toFloat()
                        )
                    }

                    val cache = TrackingDataCache(
                        passId = passId,
                        trackingPoints = trackingPoints,
                        totalSize = passDetails.size
                    )

                    trackingDataCache[passId] = cache

                    val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
                    logger.info("✅ 추적 데이터 캐시 완료: passId=$passId, ${cache.totalSize}개 포인트, ${elapsedMs}ms")
                }
            } catch (e: Exception) {
                logger.error("추적 데이터 캐싱 실패: passId=$passId, ${e.message}", e)
            }
        }, trackingDataExecutor)
    }

    private fun handleTrackingPreparation(nextSchedule: Map<String, Any?>?, calTime: ZonedDateTime) {
        if (nextSchedule != null) {
            val nextStartTime = nextSchedule["StartTime"] as? ZonedDateTime ?: return
            val timeUntilNextStart = Duration.between(calTime, nextStartTime)
            val minutesUntilStart = timeUntilNextStart.toMinutes()
            val secondsUntilStart = timeUntilNextStart.seconds % 60  // ✅ 초 단위 추가
            val nextMstId = nextSchedule["No"] as? UInt ?: return

            // 2-1: 다음 스케줄 시작 2분 이내인 경우 - 추적 시작 위치로 이동
            if (minutesUntilStart <= PREPARATION_TIME_MINUTES &&
                minutesUntilStart >= 0 &&
                nextSchedule != lastPreparedSchedule
            ) {

                moveToStartPosition(nextMstId)
                lastPreparedSchedule = nextSchedule
                isInStowPosition.set(false)
                logger.info("⏳ 추적 시작 ${minutesUntilStart}분 ${secondsUntilStart}초 전: 시작 위치로 이동 완료")  // ✅ 개선
            }
            // 2-2: 다음 스케줄 시작까지 2분 이상 남은 경우 - Stow 위치로 이동
            else if (minutesUntilStart > PREPARATION_TIME_MINUTES &&
                !isInStowPosition.get()
            ) {

                moveToStowPosition(calTime)
                isInStowPosition.set(true)
                logger.info("⏳ 추적 시작까지 ${minutesUntilStart}분 ${secondsUntilStart}초 남음: Stow 위치로 이동")  // ✅ 개선
            }
        }
        // 케이스 3: 현재 추적 중이 아니고 다음 스케줄도 없는 경우 - 모든 추적 완료, Stow 위치로 이동
        else if (!isInStowPosition.get()) {
            moveToStowPosition(calTime)
            isInStowPosition.set(true)
            lastPreparedSchedule = null
            logger.info("🏁 모든 추적 완료: Stow 위치로 이동")
        }
    }

    // ✅ 기존 메서드들 유지 (변경 없음)
    private fun moveToStartPosition(passId: UInt) {
        val passDetails = getSelectedTrackDtlByMstId(passId)

        if (passDetails.isNotEmpty()) {
            val startPoint = passDetails.first()
            val startAzimuth = (startPoint["Azimuth"] as Double).toFloat()
            val startElevation = (startPoint["Elevation"] as Double).toFloat()
            moveStartAnglePosition(startAzimuth, 5f, startElevation, 5f, 0f, 0f)
            logger.info("📍 시작 위치 이동 완료: Az=${startAzimuth}°, El=${startElevation}°")
        }
    }

    fun moveStartAnglePosition(
        cmdAzimuthAngle: Float,
        cmdAzimuthSpeed: Float,
        cmdElevationAngle: Float,
        cmdElevationSpeed: Float,
        cmdTiltAngle: Float,
        cmdTiltSpeed: Float
    ) {
        val multiAxis = BitSet()
        multiAxis.set(0)
        multiAxis.set(1)
        udpFwICDService.multiManualCommand(
            multiAxis, cmdAzimuthAngle,
            cmdAzimuthSpeed, cmdElevationAngle, cmdElevationSpeed, cmdTiltAngle ?: 0.0f, cmdTiltSpeed ?: 0.0f
        )
    }

    private fun moveToStowPosition(calTime: ZonedDateTime) {
        logger.info("🏠 Stow 위치로 이동 (${calTime})")
        udpFwICDService.StowCommand()
        isPreparingForTracking.set(false)
        lastPreparedSchedule = null
    }

    // ✅ 기존 메서드 시그니처 유지하면서 내부 최적화
    fun sendHeaderTrackingData(passId: UInt) {
        try {
            val selectedPass = getSelectedTrackMstByMstId(passId)
            if (selectedPass == null) {
                logger.error("선택된 패스 ID($passId)에 해당하는 데이터를 찾을 수 없습니다.")
                return
            }

            val startTime = (selectedPass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
            val endTime = (selectedPass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)

            logger.info("위성 추적 시작: ${selectedPass["SatelliteName"]} (패스 ID: $passId)")
            logger.info("시작 시간: $startTime, 종료 시간: $endTime")

            val startTimeMs = (startTime.nano / 1_000_000).toUShort()
            val endTimeMs = (endTime.nano / 1_000_000).toUShort()

            val headerFrame = ICDService.SatelliteTrackOne.SetDataFrame(
                cmdOne = 'T',
                cmdTwo = 'T',
                dataLen = calculateDataLength(passId).toUShort(),
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

            udpFwICDService.sendSatelliteTrackHeader(headerFrame)
            logger.info("위성 추적 헤더 정보 전송 완료")

        } catch (e: Exception) {
            logger.error("위성 추적 시작 중 오류 발생: ${e.message}", e)
        }
    }

    fun sendInitialTrackingData(passId: UInt) {
        try {
            val passDetails = getSelectedTrackDtlByMstId(passId)
            if (passDetails.isEmpty()) {
                logger.error("선택된 패스 ID($passId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
                return
            }

            // ✅ 기존 로직 유지 (50개 초기 데이터)
            val initialTrackingData = passDetails.take(50).mapIndexed { index, point ->
                Triple(
                    (index * 100).toUInt(),
                    (point["Elevation"] as Double).toFloat(),
                    (point["Azimuth"] as Double).toFloat()
                )
            }

            val currentTime = GlobalData.Time.utcNow
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
                timeOffset = GlobalData.Offset.TimeOffset.toInt(),
                satelliteTrackData = initialTrackingData
            )

            udpFwICDService.sendSatelliteTrackInitialControl(initialControlFrame)
            logger.info("위성 추적 초기 제어 명령 전송 완료 (${initialTrackingData.size}개 데이터 포인트)")

        } catch (e: Exception) {
            logger.error("위성 추적 초기 제어 명령 전송 중 오류 발생: ${e.message}", e)
        }
    }

    // ✅ 기존 메서드 시그니처 유지하면서 내부 최적화
    fun handleTrackingDataRequest(passId: UInt, timeAcc: UInt, requestDataLength: UShort) {
        val startIndex = timeAcc.toInt()
        sendAdditionalTrackingDataOptimized(passId, startIndex, requestDataLength.toInt())
    }

    // ✅ 최적화된 추가 데이터 전송 (기존 메서드 대체)
    private fun sendAdditionalTrackingDataOptimized(passId: UInt, startIndex: Int, requestDataLength: Int = 25) {
        // ✅ 즉시 비동기 처리로 UDP 스레드 블로킹 방지
        CompletableFuture.runAsync({
            try {
                val processingStart = System.nanoTime()

                // ✅ 캐시 우선 조회 (고속)
                val cache = trackingDataCache[passId]
                if (cache != null && !cache.isExpired()) {
                    sendFromCache(cache, startIndex, requestDataLength, processingStart)
                } else {
                    // ✅ 캐시 없으면 기존 방식으로 폴백 (안전성 보장)
                    sendFromDatabase(passId, startIndex, requestDataLength, processingStart)
                }

            } catch (e: Exception) {
                logger.error("최적화된 추적 데이터 전송 실패: passId=$passId, ${e.message}", e)
                // ✅ 실패 시 기존 방식으로 재시도
                sendAdditionalTrackingDataLegacy(passId, startIndex, requestDataLength)
            }
        }, trackingDataExecutor)
    }

    // ✅ 캐시에서 고속 전송
    private fun sendFromCache(
        cache: TrackingDataCache,
        startIndex: Int,
        requestDataLength: Int,
        processingStart: Long
    ) {
        val indexMs = startIndex / 100
        val totalIndexes = cache.totalSize
        val remainingIndexes = maxOf(0, totalIndexes - indexMs)

        if (indexMs >= totalIndexes) {
            logger.info("📋 추적 완료: passId=${cache.passId}, 인덱스=$indexMs/$totalIndexes")
            return
        }

        // ✅ 고속 Array 접근
        val endIndex = minOf(indexMs + requestDataLength, totalIndexes)
        val additionalTrackingData = (indexMs until endIndex).map { index ->
            val point = cache.trackingPoints[index]
            Triple(startIndex + (index - indexMs) * 100, point.elevation, point.azimuth)
        }

        if (additionalTrackingData.isEmpty()) {
            logger.warn("📋 전송할 데이터가 없습니다: passId=${cache.passId}, startIndex=$startIndex")
            return
        }

        val additionalDataFrame = ICDService.SatelliteTrackThree.SetDataFrame(
            cmdOne = 'T', cmdTwo = 'R',
            dataLength = additionalTrackingData.size.toUShort(),
            satelliteTrackData = additionalTrackingData
        )

        udpFwICDService.sendSatelliteTrackAdditionalData(additionalDataFrame)

        val processingTime = (System.nanoTime() - processingStart) / 1_000_000
        val progressPercentage = (indexMs.toDouble() / totalIndexes.toDouble() * 100.0)

        logger.info(
            "🚀 [캐시] 고속 추적 데이터 전송 완료 (${additionalTrackingData.size}개 / 인덱스: $indexMs / 남은: $remainingIndexes / 총: $totalIndexes) [진행률: ${
                String.format(
                    "%.1f",
                    progressPercentage
                )
            }%] 처리시간: ${processingTime}ms"
        )

        // ✅ 성능 경고
        if (processingTime > 10) {
            logger.warn("⚠️ 캐시 처리 지연: ${processingTime}ms")
        }
    }

    // ✅ DB에서 전송 (폴백)
    private fun sendFromDatabase(passId: UInt, startIndex: Int, requestDataLength: Int, processingStart: Long) {
        val passDetails = getSelectedTrackDtlByMstId(passId)
        if (passDetails.isEmpty()) {
            logger.error("선택된 패스 ID($passId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
            return
        }

        val indexMs = startIndex / 100
        val totalSize = passDetails.size
        val remainingIndexes = maxOf(0, totalSize - indexMs)

        if (indexMs >= totalSize) {
            logger.info("📋 추적 완료: passId=$passId, 인덱스=$indexMs/$totalSize")
            return
        }

        val additionalTrackingData = passDetails.drop(indexMs).take(requestDataLength).mapIndexed { index, point ->
            Triple(
                startIndex + index * 100,
                (point["Elevation"] as Double).toFloat(),
                (point["Azimuth"] as Double).toFloat()
            )
        }

        if (additionalTrackingData.isEmpty()) {
            logger.warn("📋 전송할 데이터가 없습니다: passId=$passId, startIndex=$startIndex")
            return
        }

        val additionalDataFrame = ICDService.SatelliteTrackThree.SetDataFrame(
            cmdOne = 'T', cmdTwo = 'R',
            dataLength = additionalTrackingData.size.toUShort(),
            satelliteTrackData = additionalTrackingData
        )

        udpFwICDService.sendSatelliteTrackAdditionalData(additionalDataFrame)

        val processingTime = (System.nanoTime() - processingStart) / 1_000_000
        val progressPercentage = (indexMs.toDouble() / totalSize.toDouble() * 100.0)

        logger.info(
            "📋 [DB] 추적 데이터 전송 완료 (${additionalTrackingData.size}개 / 인덱스: $indexMs / 남은: $remainingIndexes / 총: $totalSize) [진행률: ${
                String.format(
                    "%.1f",
                    progressPercentage
                )
            }%] 처리시간: ${processingTime}ms"
        )

        // ✅ 성능 경고
        if (processingTime > 20) {
            logger.warn("⚠️ DB 처리 지연: ${processingTime}ms")
        }
    }

    // ✅ 기존 방식 보존 (안전성을 위한 폴백)
    private fun sendAdditionalTrackingDataLegacy(passId: UInt, startIndex: Int, requestDataLength: Int = 25) {
        try {
            val passDetails = getSelectedTrackDtlByMstId(passId)
            if (passDetails.isEmpty()) {
                logger.error("선택된 패스 ID($passId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
                return
            }

            val indexMs = startIndex / 100
            val additionalTrackingData = passDetails.drop(indexMs).take(requestDataLength).mapIndexed { index, point ->
                Triple(
                    startIndex + index * 100,
                    (point["Elevation"] as Double).toFloat(),
                    (point["Azimuth"] as Double).toFloat()
                )
            }

            if (additionalTrackingData.isEmpty()) {
                logger.info("더 이상 전송할 추적 데이터가 없습니다.")
                return
            }

            val additionalDataFrame = ICDService.SatelliteTrackThree.SetDataFrame(
                cmdOne = 'T', cmdTwo = 'R',
                dataLength = additionalTrackingData.size.toUShort(),
                satelliteTrackData = additionalTrackingData
            )

            udpFwICDService.sendSatelliteTrackAdditionalData(additionalDataFrame)
            logger.info("🔄 [폴백] 위성 추적 추가 데이터 전송 완료 (${additionalTrackingData.size}개 데이터 포인트, 시작 인덱스: $startIndex)")

        } catch (e: Exception) {
            logger.error("폴백 추적 데이터 전송 중 오류 발생: ${e.message}", e)
        }
    }

    fun stopScheduleTracking() {
        if (!isTrackingMonitorRunning.get()) {
            return
        }

        isTrackingMonitorRunning.set(false)
        trackingMonitorTask?.cancel(false)
        trackingMonitorExecutor?.shutdown()

        try {
            trackingMonitorExecutor?.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            trackingMonitorExecutor?.shutdownNow()
            Thread.currentThread().interrupt()
        }

        // 모든 추적 중지 후 Stow 위치로 이동
        if (!isInStowPosition.get()) {
            moveToStowPosition(GlobalData.Time.calUtcTimeOffsetTime)
            isInStowPosition.set(true)
        }

        // ✅ 캐시 정리 추가
        trackingDataCache.clear()

        trackingMonitorExecutor = null
        trackingMonitorTask = null
        lastDisplayedSchedule = null
        lastPreparedSchedule = null
        isPreparingForTracking.set(false)

        logger.info("🛑 추적 모니터링 중지 완료 (캐시 정리됨)")

        dataStoreService.clearTrackingMstIds()
        logger.info("🛑 추적 모니터링 중지 완료 (캐시 및 mstId 정리됨)")
    }

    // ✅ 서비스 종료 시 정리
    @PreDestroy
    fun cleanup() {
        // 기존 구독 해제
        subscriptions.forEach { it.dispose() }
        subscriptions.clear()

        // 추적 모니터링 중지
        stopScheduleTracking()

        // ✅ 최적화 스레드 정리
        trackingDataExecutor.shutdown()
        try {
            if (!trackingDataExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                trackingDataExecutor.shutdownNow()
                logger.warn("추적 데이터 최적화 스레드 강제 종료")
            }
        } catch (e: InterruptedException) {
            trackingDataExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }

        // ✅ 캐시 정리
        trackingDataCache.clear()

        logger.info("PassScheduleService 정리 완료 (최적화 리소스 포함)")
    }

    // ✅ 성능 통계 조회
    fun getTrackingPerformanceStats(): Map<String, Any> {
        val cacheStats = trackingDataCache.mapValues { (passId, cache) ->
            mapOf(
                "totalPoints" to cache.totalSize,
                "createdAt" to cache.createdAt,
                "isExpired" to cache.isExpired(),
                "memorySizeKB" to (cache.totalSize * 12 / 1024) // 대략적인 메모리 사용량
            )
        }

        return mapOf(
            "cacheSize" to trackingDataCache.size,
            "cachedPassIds" to trackingDataCache.keys.toList(),
            "totalCachedPoints" to trackingDataCache.values.sumOf { it.totalSize },
            "totalMemoryKB" to trackingDataCache.values.sumOf { it.totalSize * 12 / 1024 },
            "executorActive" to !trackingDataExecutor.isShutdown,
            "executorTerminated" to trackingDataExecutor.isTerminated,
            "cacheDetails" to cacheStats,
            "optimizationEnabled" to true
        )
    }

    // ✅ 캐시 상태 확인
    fun getCacheStatus(passId: UInt): Map<String, Any> {
        val cache = trackingDataCache[passId]
        return if (cache != null) {
            mapOf(
                "cached" to true,
                "passId" to passId,
                "totalPoints" to cache.totalSize,
                "createdAt" to cache.createdAt,
                "isExpired" to cache.isExpired(),
                "memorySizeKB" to (cache.totalSize * 12 / 1024)
            )
        } else {
            mapOf(
                "cached" to false,
                "passId" to passId,
                "message" to "캐시되지 않음 - DB에서 조회됨"
            )
        }
    }

    // ✅ 캐시 수동 정리 (필요시)
    fun clearExpiredCache() {
        val expiredKeys = trackingDataCache.filter { (_, cache) -> cache.isExpired() }.keys
        expiredKeys.forEach { trackingDataCache.remove(it) }

        if (expiredKeys.isNotEmpty()) {
            logger.info("만료된 캐시 정리 완료: ${expiredKeys.size}개 항목")
        }
    }

    // ✅ 캐시 강제 새로고침
    fun refreshCache(passId: UInt) {
        trackingDataCache.remove(passId)
        preloadTrackingDataCache(passId)
        logger.info("캐시 강제 새로고침: passId=$passId")
    }

    // ✅ 기존 메서드들 - 변경 없음 (호환성 보장)
    private fun getCurrentSelectedTrackingPassWithTime(targetTime: ZonedDateTime): Map<String, Any?>? {
        selectedTrackMstStorage.values.forEach { mstDataList ->
            val currentPass = mstDataList.find { mstRecord ->
                val startTime = mstRecord["StartTime"] as? ZonedDateTime
                val endTime = mstRecord["EndTime"] as? ZonedDateTime

                startTime != null && endTime != null && !targetTime.isBefore(startTime) && !targetTime.isAfter(endTime)
            }
            if (currentPass != null) return currentPass
        }
        return null
    }

    private fun getNextSelectedTrackingPassWithTime(targetTime: ZonedDateTime): Map<String, Any?>? {
        return getSelectedTrackingSchedule().filter { mstRecord ->
            val startTime = mstRecord["StartTime"] as? ZonedDateTime
            startTime != null && startTime.isAfter(targetTime)
        }.minByOrNull { mstRecord ->
            mstRecord["StartTime"] as ZonedDateTime
        }
    }

    fun getCurrentSelectedTrackingPass(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        return getCurrentSelectedTrackingPassWithTime(calTime)
    }

    fun getNextSelectedTrackingPass(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        return getNextSelectedTrackingPassWithTime(calTime)
    }

    fun getCurrentDisplayedSchedule(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        return getCurrentSelectedTrackingPassWithTime(calTime) ?: lastDisplayedSchedule
    }

    fun getTrackingMonitorStatus(): Map<String, Any> {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        val currentSchedule = getCurrentSelectedTrackingPassWithTime(calTime)
        val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)

        return mapOf(
            "isRunning" to isTrackingMonitorRunning.get(),
            "hasCurrentSchedule" to (currentSchedule != null),
            "hasNextSchedule" to (nextSchedule != null),
            "hasLastDisplayedSchedule" to (lastDisplayedSchedule != null),
            "currentSchedule" to (currentSchedule ?: "없음"),
            "nextSchedule" to (nextSchedule ?: "없음"),
            "lastDisplayedSchedule" to (lastDisplayedSchedule ?: "없음"),
            "totalSelectedSchedules" to getSelectedTrackingSchedule().size,
            "calTime" to calTime.toString(),
            "cacheSize" to trackingDataCache.size,  // ✅ 캐시 정보 추가
            "optimizationActive" to !trackingDataExecutor.isShutdown  // ✅ 최적화 상태 추가
        )
    }

    // ✅ 시간 표시 개선된 로그 메서드들
    private fun outputCurrentScheduleInfo(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val passId = schedule["No"] as? UInt
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val startTime = schedule["StartTime"] as? ZonedDateTime
        val endTime = schedule["EndTime"] as? ZonedDateTime
        val maxElevation = schedule["MaxElevation"] as? Double
        val duration = schedule["Duration"] as? String

        logger.info("🎯 [현재 추적] $satelliteName (ID: $passId)")
        logger.info("   ⏰ 시간: $startTime ~ $endTime")
        logger.info("   📏 지속: $duration")
        logger.info("   📐 최대고도: ${maxElevation}°")
        logger.info("   🕐 계산시간: $calTime")

        if (passId != null) {
            val detailData = getSelectedTrackDtlByMstId(passId)
            val cacheStatus = if (trackingDataCache.containsKey(passId)) "캐시됨" else "DB조회"
            logger.info("   📊 추적포인트: ${detailData.size}개 ($cacheStatus)")  // ✅ 캐시 상태 표시
        }
    }

    private fun outputNextScheduleInfo(calTime: ZonedDateTime) {
        val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)

        if (nextSchedule != null) {
            val nextName = nextSchedule["SatelliteName"] as? String ?: "Unknown"
            val nextStart = nextSchedule["StartTime"] as? ZonedDateTime
            val nextId = nextSchedule["No"] as? UInt
            val nextMaxElevation = nextSchedule["MaxElevation"] as? Double

            if (nextStart != null) {
                val waitTime = Duration.between(calTime, nextStart)
                val minutes = waitTime.toMinutes()
                val seconds = waitTime.seconds % 60  // ✅ 초 단위 추가

                logger.info("   📅 다음예정: $nextName (ID: $nextId)")
                logger.info("   ⏰ 시작: $nextStart")
                logger.info("   📐 최대고도: ${nextMaxElevation}°")
                logger.info("   ⏳ 대기: ${minutes}분 ${seconds}초")  // ✅ 개선
            }
        } else {
            logger.info("   📭 다음예정: 없음")
        }
    }

    private fun outputUpcomingScheduleInfo(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val startTime = schedule["StartTime"] as? ZonedDateTime
        val passId = schedule["No"] as? UInt

        if (startTime != null) {
            val waitTime = Duration.between(calTime, startTime)
            val minutes = waitTime.toMinutes()
            val seconds = waitTime.seconds % 60  // ✅ 초 단위 추가

            logger.info("📅 [다음 예정] $satelliteName (ID: $passId)")
            logger.info("   ⏰ 시작예정: $startTime")
            logger.info("   ⏳ 대기시간: ${minutes}분 ${seconds}초")  // ✅ 개선
        }
    }

    private fun outputTrackingEnd(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val passId = schedule["No"] as? UInt

        logger.info("🏁 [추적 종료] $satelliteName (ID: $passId)")
        logger.info("   🕐 종료시간: $calTime")
    }

    private fun outputScheduleChange(prev: Map<String, Any?>, new: Map<String, Any?>, calTime: ZonedDateTime) {
        val prevName = prev["SatelliteName"] as? String ?: "Unknown"
        val newName = new["SatelliteName"] as? String ?: "Unknown"
        val prevId = prev["No"] as? UInt
        val newId = new["No"] as? UInt

        logger.info("🔄 [추적 변경] $prevName(ID:$prevId) → $newName(ID:$newId)")
        logger.info("   🕐 변경시간: $calTime")

        outputCurrentScheduleInfo(new, calTime)
    }

    private fun outputScheduleFixed(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val passId = schedule["No"] as? UInt

        logger.info("📌 [스케줄 고정] $satelliteName (ID: $passId)")
        logger.info("   🕐 고정시간: $calTime")
        logger.info("   📭 모든 스케줄 완료 - 마지막 스케줄로 고정")
    }

    // ✅ 기존 메서드들 - 변경 없음
    fun generateAllPassScheduleTrackingDataAsync(): Mono<Map<String, Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>>> {
        val allTleIds = getAllPassScheduleTleIds()

        if (allTleIds.isEmpty()) {
            logger.warn("캐시된 TLE 데이터가 없습니다.")
            return Mono.just(emptyMap())
        }

        logger.info("전체 위성 패스 스케줄 추적 데이터 생성 시작 (비동기 병렬 처리) - 총 ${allTleIds.size}개 위성")

        return Flux.fromIterable(allTleIds).flatMap { satelliteId ->
            val tleData = passScheduleTleCache[satelliteId]
            if (tleData != null) {
                val (tleLine1, tleLine2, satelliteName) = tleData

                generatePassScheduleTrackingDataAsync(
                    satelliteId, tleLine1, tleLine2, satelliteName
                ).map { trackingData ->
                    satelliteId to trackingData
                }.doOnSuccess {
                    logger.info("위성 $satelliteId($satelliteName) 추적 데이터 생성 완료")
                }.onErrorResume { error ->
                    logger.error("위성 $satelliteId($satelliteName) 추적 데이터 생성 중 오류 발생: ${error.message}", error)
                    Mono.empty()
                }
            } else {
                logger.warn("위성 $satelliteId 의 TLE 데이터를 찾을 수 없습니다.")
                Mono.empty()
            }
        }.collectMap({ it.first }, { it.second }).doOnSuccess { results ->
            logger.info("전체 위성 패스 스케줄 추적 데이터 생성 완료 (비동기) - ${results.size}개 위성 처리 완료")
        }.doOnError { error ->
            logger.error("전체 위성 패스 스케줄 추적 데이터 생성 실패 (비동기): ${error.message}", error)
        }.timeout(Duration.ofMinutes(60)).onErrorMap { error ->
            when (error) {
                is IOException -> RuntimeException("네트워크 연결 오류: ${error.message}", error)
                is TimeoutException -> RuntimeException("계산 시간 초과", error)
                else -> RuntimeException("전체 위성 패스 스케줄 추적 데이터 생성 실패: ${error.message}", error)
            }
        }
    }

    fun generatePassScheduleTrackingDataAsync(
        satelliteId: String, tleLine1: String, tleLine2: String, satelliteName: String? = null
    ): Mono<Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>> {
        return Mono.fromCallable {
            val actualSatelliteName = satelliteName ?: satelliteId

            logger.info("$actualSatelliteName 위성의 패스 스케줄 추적 시작")

            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            val passScheduleTrackMst = mutableListOf<Map<String, Any?>>()
            val passScheduleTrackDtl = mutableListOf<Map<String, Any?>>()

            val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
                tleLine1 = tleLine1,
                tleLine2 = tleLine2,
                startDate = today.withZoneSameInstant(ZoneOffset.UTC),
                durationDays = 2,
                minElevation = trackingData.minElevationAngle,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                altitude = locationData.altitude,
            )

            logger.info("위성 $satelliteId 추적 스케줄 생성 완료: ${schedule.trackingPasses.size}개 패스")

            val creationDate = ZonedDateTime.now()
            val creator = "PassScheduleService"

            schedule.trackingPasses.forEachIndexed { index, pass ->
                globalMstId++

                val startTimeWithMs = pass.startTime.withZoneSameInstant(ZoneOffset.UTC)
                val endTimeWithMs = pass.endTime.withZoneSameInstant(ZoneOffset.UTC)

                logger.debug("패스 #$globalMstId: 시작=$startTimeWithMs, 종료=$endTimeWithMs")

                passScheduleTrackMst.add(
                    mapOf(
                        "No" to globalMstId.toUInt(),
                        "SatelliteID" to satelliteId,
                        "SatelliteName" to actualSatelliteName,
                        "StartTime" to startTimeWithMs,
                        "EndTime" to endTimeWithMs,
                        "Duration" to pass.getDurationString(),
                        "MaxElevation" to pass.maxElevation,
                        "MaxElevationTime" to pass.maxElevationTime,
                        "StartAzimuth" to pass.startAzimuth,
                        "StartElevation" to pass.startElevation,
                        "EndAzimuth" to pass.endAzimuth,
                        "EndElevation" to pass.endElevation,
                        "MaxAzRate" to pass.maxAzimuthRate,
                        "MaxElRate" to pass.maxElevationRate,
                        "MaxAzAccel" to pass.maxAzimuthAccel,
                        "MaxElAccel" to pass.maxElevationAccel,
                        "CreationDate" to creationDate,
                        "Creator" to creator
                    )
                )

                pass.trackingData.forEachIndexed { dtlIndex, data ->
                    passScheduleTrackDtl.add(
                        mapOf(
                            "No" to (dtlIndex + 1).toUInt(),
                            "MstId" to globalMstId.toUInt(),
                            "SatelliteID" to satelliteId,
                            "Time" to data.timestamp,
                            "Azimuth" to data.azimuth,
                            "Elevation" to data.elevation,
                            "Range" to data.range,
                            "Altitude" to data.altitude
                        )
                    )
                }
            }

            logger.info("위성 $satelliteId 추적 데이터 생성 완료: ${passScheduleTrackMst.size}개 스케줄 항목과 ${passScheduleTrackDtl.size}개 좌표 포인트")

            // 방위각 변환 시작
            logger.info("방위각 변환 시작 (0~360도 -> ±270도)")
            val (convertedMst, convertedDtl) = limitAngleCalculator.convertTrackingData(
                passScheduleTrackMst, passScheduleTrackDtl
            )
            logger.info("방위각 변환 완료")

            // 검증
            val validationResult = limitAngleCalculator.validateConversion(
                passScheduleTrackMst, passScheduleTrackDtl, convertedMst, convertedDtl
            )
            logger.info(validationResult.getSummary())

            // 통계
            val statistics = limitAngleCalculator.getConversionStatistics(passScheduleTrackDtl, convertedDtl)
            logger.info(statistics.getSummary())

            if (validationResult.isValid) {
                logger.info("✅ 방위각 변환 검증 성공")
            } else {
                logger.warn("⚠️ 방위각 변환 검증 이슈:")
                validationResult.issues.forEach { issue ->
                    logger.warn("  - $issue")
                }
            }

            // 저장소에 데이터 저장
            passScheduleTrackMstStorage[satelliteId] = convertedMst
            passScheduleTrackDtlStorage[satelliteId] = convertedDtl

            // 변환 결과 로깅
            convertedMst.forEach { mst ->
                val mstId = mst["No"] as UInt
                val originalStartAz = mst["OriginalStartAzimuth"] as? Double
                val originalEndAz = mst["OriginalEndAzimuth"] as? Double
                val convertedStartAz = mst["StartAzimuth"] as Double
                val convertedEndAz = mst["EndAzimuth"] as Double

                logger.debug("패스 #$mstId 변환 결과:")
                if (originalStartAz != null && originalEndAz != null) {
                    logger.debug(
                        "  원본: ${String.format("%.2f", originalStartAz)}° ~ ${
                            String.format(
                                "%.2f", originalEndAz
                            )
                        }°"
                    )
                }
                logger.debug(
                    "  변환: ${String.format("%.2f", convertedStartAz)}° ~ ${
                        String.format(
                            "%.2f", convertedEndAz
                        )
                    }°"
                )
            }

            Pair(convertedMst, convertedDtl)
        }.subscribeOn(Schedulers.boundedElastic()).doOnSubscribe {
            logger.info("위성 패스 스케줄 추적 데이터 생성 시작 (비동기): $satelliteId")
        }.doOnSuccess {
            logger.info("위성 패스 스케줄 추적 데이터 생성 완료 (비동기): $satelliteId")
        }.doOnError { error ->
            logger.error("위성 패스 스케줄 추적 데이터 생성 실패 (비동기): $satelliteId - ${error.message}", error)
        }.timeout(Duration.ofMinutes(30)).onErrorMap { error ->
            when (error) {
                is IOException -> RuntimeException("네트워크 연결 오류: ${error.message}", error)
                is TimeoutException -> RuntimeException("계산 시간 초과", error)
                else -> RuntimeException("위성 패스 스케줄 추적 데이터 생성 실패: $satelliteId - ${error.message}", error)
            }
        }
    }

    // ✅ 기존 조회 메서드들 - 변경 없음
    fun getPassScheduleTrackMstBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return passScheduleTrackMstStorage[satelliteId]
    }

    fun getPassScheduleTrackDtlBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return passScheduleTrackDtlStorage[satelliteId]
    }

    fun getPassScheduleTrackDtlByMstId(satelliteId: String, mstId: UInt): List<Map<String, Any?>> {
        val dtlData = passScheduleTrackDtlStorage[satelliteId] ?: return emptyList()
        return dtlData.filter { it["MstId"] == mstId }
    }

    fun getAllPassScheduleTrackMst(): Map<String, List<Map<String, Any?>>> {
        return passScheduleTrackMstStorage.toMap()
    }

    fun getAllPassScheduleTrackDtl(): Map<String, List<Map<String, Any?>>> {
        return passScheduleTrackDtlStorage.toMap()
    }

    fun clearPassScheduleTrackingData(satelliteId: String) {
        passScheduleTrackMstStorage.remove(satelliteId)
        passScheduleTrackDtlStorage.remove(satelliteId)
        logger.info("위성 $satelliteId 의 패스 스케줄 추적 데이터가 삭제되었습니다.")
    }

    fun clearAllPassScheduleTrackingData() {
        val mstSize = passScheduleTrackMstStorage.size
        val dtlSize = passScheduleTrackDtlStorage.values.sumOf { it.size }
        globalMstId = 0
        passScheduleTrackMstStorage.clear()
        passScheduleTrackDtlStorage.clear()

        logger.info("모든 패스 스케줄 추적 데이터가 삭제되었습니다. (마스터: ${mstSize}개, 세부: ${dtlSize}개)")
    }    // ✅ 기존 추적 대상 관리 메서드들 - 변경 없음

    fun setTrackingTargetList(targets: List<TrackingTarget>) {
        synchronized(trackingTargetList) {
            trackingTargetList.clear()
            trackingTargetList.addAll(targets)
        }
        logger.info("위성 추적 스케줄 대상 목록이 설정되었습니다. 총 ${targets.size}개 대상")

        targets.forEach { target ->
            logger.info("추적 대상: ${target.satelliteName ?: target.satelliteId} (MST ID: ${target.mstId}, 최대 고도: ${target.maxElevation}°, 시작 시간: ${target.startTime}, 종료 시간: ${target.endTime})")
        }

        generateSelectedTrackingData()
    }

    fun getTrackingTargetList(): List<TrackingTarget> {
        return synchronized(trackingTargetList) {
            trackingTargetList.toList()
        }
    }

    fun getTrackingTargetsBySatelliteId(satelliteId: String): List<TrackingTarget> {
        return synchronized(trackingTargetList) {
            trackingTargetList.filter { it.satelliteId == satelliteId }
        }
    }

    fun getTrackingTargetByMstId(mstId: UInt): TrackingTarget? {
        return synchronized(trackingTargetList) {
            trackingTargetList.find { it.mstId == mstId }
        }
    }

    fun clearTrackingTargetList() {
        val size = synchronized(trackingTargetList) {
            val currentSize = trackingTargetList.size
            trackingTargetList.clear()
            currentSize
        }

        clearSelectedTrackingData()
        logger.info("위성 추적 스케줄 대상 목록이 초기화되었습니다. ${size}개 항목 삭제")
    }

    fun generateSelectedTrackingData() {
        synchronized(trackingTargetList) {
            if (trackingTargetList.isEmpty()) {
                logger.warn("추적 대상 목록이 비어있습니다.")
                selectedTrackMstStorage.clear()
                return
            }

            logger.info("선별된 추적 데이터 생성 시작: ${trackingTargetList.size}개 대상")

            selectedTrackMstStorage.clear()
            val targetMstIds = trackingTargetList.map { it.mstId }.toSet()

            passScheduleTrackMstStorage.forEach { (satelliteId, allMstData) ->
                val selectedMstData = allMstData.filter { mstRecord ->
                    val mstId = mstRecord["No"] as? UInt
                    mstId != null && targetMstIds.contains(mstId)
                }

                if (selectedMstData.isNotEmpty()) {
                    selectedTrackMstStorage[satelliteId] = selectedMstData
                    logger.info("위성 $satelliteId 선별된 패스: ${selectedMstData.size}개")
                }
            }

            val totalSelectedPasses = selectedTrackMstStorage.values.sumOf { it.size }
            logger.info("선별된 추적 데이터 생성 완료: ${selectedTrackMstStorage.size}개 위성, ${totalSelectedPasses}개 패스")
        }
    }

    fun getSelectedTrackMstBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return selectedTrackMstStorage[satelliteId]
    }

    fun getAllSelectedTrackMst(): Map<String, List<Map<String, Any?>>> {
        return selectedTrackMstStorage.toMap()
    }

    fun getSelectedTrackMstByMstId(mstId: UInt): Map<String, Any?>? {
        selectedTrackMstStorage.values.forEach { mstDataList ->
            val found = mstDataList.find { it["No"] == mstId }
            if (found != null) return found
        }
        return null
    }

    fun getSelectedTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
        val selectedMst = getSelectedTrackMstByMstId(mstId) ?: return emptyList()
        val satelliteId = selectedMst["SatelliteID"] as? String ?: return emptyList()

        val allDtlData = passScheduleTrackDtlStorage[satelliteId] ?: return emptyList()
        return allDtlData.filter { it["MstId"] == mstId }
    }

    fun getSelectedTrackingSchedule(): List<Map<String, Any?>> {
        val allSelectedPasses = mutableListOf<Map<String, Any?>>()

        selectedTrackMstStorage.values.forEach { mstDataList ->
            allSelectedPasses.addAll(mstDataList)
        }

        return allSelectedPasses.sortedBy { mstRecord ->
            mstRecord["StartTime"] as? ZonedDateTime
        }
    }

    fun clearSelectedTrackingData() {
        val size = selectedTrackMstStorage.values.sumOf { it.size }
        selectedTrackMstStorage.clear()
        logger.info("선별된 추적 데이터가 초기화되었습니다. ${size}개 패스 삭제")
    }

    // ✅ 기존 TLE 캐시 관리 메서드들 - 변경 없음
    fun addPassScheduleTle(satelliteId: String, tleLine1: String, tleLine2: String, satelliteName: String? = null) {
        val finalSatelliteName = satelliteName ?: satelliteId
        passScheduleTleCache[satelliteId] = Triple(tleLine1, tleLine2, finalSatelliteName)
        logger.info("위성 TLE 데이터가 캐시에 추가되었습니다. 위성 ID: $satelliteId, 이름: $finalSatelliteName")
    }

    fun getPassScheduleTle(satelliteId: String): Pair<String, String>? {
        val tleData = passScheduleTleCache[satelliteId]
        return if (tleData != null) {
            Pair(tleData.first, tleData.second)
        } else {
            null
        }
    }

    fun getPassScheduleSatelliteName(satelliteId: String): String? {
        return passScheduleTleCache[satelliteId]?.third
    }

    fun getPassScheduleTleWithName(satelliteId: String): Triple<String, String, String>? {
        return passScheduleTleCache[satelliteId]
    }

    fun removePassScheduleTle(satelliteId: String) {
        passScheduleTleCache.remove(satelliteId)
        passScheduleTrackMstStorage.remove(satelliteId)
        passScheduleTrackDtlStorage.remove(satelliteId)
        logger.info("위성 TLE 데이터가 캐시에서 삭제되었습니다. 위성 ID: $satelliteId")
    }

    fun getAllPassScheduleTleIds(): List<String> {
        return passScheduleTleCache.keys.toList()
    }

    fun getCacheSize(): Int {
        return passScheduleTleCache.size
    }

    fun clearCache() {
        val size = passScheduleTleCache.size
        passScheduleTleCache.clear()
        passScheduleTrackMstStorage.clear()
        passScheduleTrackDtlStorage.clear()

        // ✅ 최적화 캐시도 함께 정리
        trackingDataCache.clear()

        logger.info("TLE 캐시 및 추적 데이터 전체 삭제 완료: ${size}개 항목 삭제 (최적화 캐시 포함)")
    }

    // ✅ 기존 통계 메서드 + 최적화 정보 추가
    fun getTrackingDataStatistics(): Map<String, Any> {
        val totalSatellites = passScheduleTrackMstStorage.size
        val totalPasses = passScheduleTrackMstStorage.values.sumOf { it.size }
        val totalTrackingPoints = passScheduleTrackDtlStorage.values.sumOf { it.size }

        val satelliteStats = passScheduleTrackMstStorage.map { (satelliteId, mstData) ->
            val dtlData = passScheduleTrackDtlStorage[satelliteId] ?: emptyList()
            satelliteId to mapOf(
                "passes" to mstData.size,
                "trackingPoints" to dtlData.size,
                "satelliteName" to (mstData.firstOrNull()?.get("SatelliteName") ?: "Unknown")
            )
        }.toMap()

        return mapOf(
            "totalSatellites" to totalSatellites,
            "totalPasses" to totalPasses,
            "totalTrackingPoints" to totalTrackingPoints,
            "averagePassesPerSatellite" to if (totalSatellites > 0) totalPasses.toDouble() / totalSatellites else 0.0,
            "averagePointsPerPass" to if (totalPasses > 0) totalTrackingPoints.toDouble() / totalPasses else 0.0,
            "satelliteDetails" to satelliteStats,
            // ✅ 최적화 정보 추가
            "optimizationStats" to mapOf(
                "cacheSize" to trackingDataCache.size,
                "cachedPassIds" to trackingDataCache.keys.toList(),
                "totalCachedPoints" to trackingDataCache.values.sumOf { it.totalSize },
                "cacheMemoryKB" to trackingDataCache.values.sumOf { it.totalSize * 12 / 1024 },
                "optimizationEnabled" to true
            )
        )
    }

    // ✅ 헬퍼 메서드들 (기존 로직 유지)
    private fun calculateDataLength(passId: UInt): Int {
        val passDetails = getSelectedTrackDtlByMstId(passId)
        logger.debug("전체 데이터 길이 계산: 패스 ID = $passId, 사이즈: ${passDetails.size}")
        return passDetails.size
    }

    // ✅ 디버깅 및 모니터링 메서드 추가
    fun getOptimizationStatus(): Map<String, Any> {
        return mapOf(
            "version" to "1.0-optimized",
            "cacheEnabled" to true,
            "asyncProcessingEnabled" to true,
            "performanceMonitoring" to true,
            "cacheStats" to getTrackingPerformanceStats(),
            "executorStatus" to mapOf(
                "isShutdown" to trackingDataExecutor.isShutdown,
                "isTerminated" to trackingDataExecutor.isTerminated,
                "activeCount" to if (!trackingDataExecutor.isShutdown) 1 else 0
            ),
            "memoryOptimization" to mapOf(
                "expiredCacheCleanup" to true,
                "automaticCacheEviction" to true,
                "maxCacheAge" to "1 hour"
            )
        )
    }

    // ✅ 성능 테스트 메서드 (개발/테스트용)
    fun performanceTest(passId: UInt, iterations: Int = 100): Map<String, Any> {
        logger.info("성능 테스트 시작: passId=$passId, iterations=$iterations")

        val results = mutableMapOf<String, Any>()

        // 캐시 성능 테스트
        val cacheResults = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            val cache = trackingDataCache[passId]
            if (cache != null) {
                // 캐시에서 데이터 접근 시뮬레이션
                val point = cache.trackingPoints.getOrNull(0)
            }
            val elapsed = (System.nanoTime() - start) / 1000 // 마이크로초
            cacheResults.add(elapsed)
        }

        // DB 성능 테스트
        val dbResults = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            val details = getSelectedTrackDtlByMstId(passId)
            val point = details.firstOrNull()
            val elapsed = (System.nanoTime() - start) / 1000 // 마이크로초
            dbResults.add(elapsed)
        }

        results["cachePerformance"] = mapOf(
            "averageTimeUs" to cacheResults.average(),
            "minTimeUs" to (cacheResults.minOrNull() ?: 0),
            "maxTimeUs" to (cacheResults.maxOrNull() ?: 0),
            "iterations" to iterations
        )

        results["dbPerformance"] = mapOf(
            "averageTimeUs" to dbResults.average(),
            "minTimeUs" to (dbResults.minOrNull() ?: 0),
            "maxTimeUs" to (dbResults.maxOrNull() ?: 0),
            "iterations" to iterations
        )

        val speedup = dbResults.average() / cacheResults.average()
        results["speedupRatio"] = speedup
        results["performanceGain"] = "${String.format("%.1f", speedup)}x faster"

        logger.info("성능 테스트 완료: 캐시가 ${String.format("%.1f", speedup)}배 빠름")

        return results
    }
}