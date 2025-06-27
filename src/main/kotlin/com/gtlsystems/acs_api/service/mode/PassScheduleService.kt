package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.axislimitangle.LimitAngleCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.SatelliteTrackingData
import com.gtlsystems.acs_api.service.udp.UdpFwICDService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
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

/**
 * TLE 데이터를 캐시로 관리하고 위성 패스 스케줄링을 담당하는 서비스
 */
@Service
class PassScheduleService(
    private val orekitCalculator: OrekitCalculator,
    private val udpFwICDService: UdpFwICDService  // 추가
) {
    private val logger = LoggerFactory.getLogger(PassScheduleService::class.java)

    // TLE 캐시 저장소 (위성 카탈로그 ID -> TLE Line1, Line2, SatelliteName)
    private val passScheduleTleCache = ConcurrentHashMap<String, Triple<String, String, String>>()

    // 위성 추적 마스터 및 세부 데이터 저장소 (위성 ID별로 관리)
    private val passScheduleTrackMstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()
    private val passScheduleTrackDtlStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()

    // ✅ 위성 추적 스케줄 대상 목록 저장소 추가
    private val trackingTargetList = mutableListOf<TrackingTarget>()

    // ✅ 선별된 추적 마스터 데이터 저장소 (추적 대상만 포함)
    private val selectedTrackMstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()

    // 추적 준비 상태 관리 변수 추가
    private var isPreparingForTracking = AtomicBoolean(false)
    private var lastPreparedSchedule: Map<String, Any?>? = null
    private var isInStowPosition = AtomicBoolean(true) // 초기에는 Stow 위치에 있다고 가정
    private val PREPARATION_TIME_MINUTES = 2L // 추적 시작 2분 전 준비

    // ✅ 위성 추적 스케줄 대상 데이터 클래스 추가
    data class TrackingTarget(
        val mstId: UInt,
        val satelliteId: String,
        val satelliteName: String? = null,
        val startTime: ZonedDateTime,
        val endTime: ZonedDateTime,
        val maxElevation: Double,
        val createdAt: ZonedDateTime = ZonedDateTime.now()
    )

    // 추적 데이터 및 위치 정보
    private val trackingData = SatelliteTrackingData.Tracking
    private val locationData = GlobalData.Location
    private val limitAngleCalculator = LimitAngleCalculator()
    private var globalMstId = 0;

    @PostConstruct
    fun init() {
        logger.info("PassScheduleService 초기화 완료")
    }

    /**
     * ✅ 기존 메서드들을 활용한 100ms 추적 모니터링 시작
     */
    fun startScheduleTracking() {
        if (isTrackingMonitorRunning.get()) {
            logger.warn("추적 모니터링이 이미 실행 중입니다.")
            return
        }

        trackingMonitorExecutor = Executors.newSingleThreadScheduledExecutor(trackingMonitorThreadFactory)
        trackingMonitorTask = trackingMonitorExecutor?.scheduleAtFixedRate(
            { checkTrackingScheduleUsingExistingMethods() }, 0, 100, TimeUnit.MILLISECONDS
        )

        isTrackingMonitorRunning.set(true)
        logger.info("🚀 추적 모니터링 시작 (기존 메서드 활용)")
    }


    /**
     * ✅ 기존 메서드들을 활용한 추적 체크 (GlobalData.Time 기준)
     */
    private fun checkTrackingScheduleUsingExistingMethods() {
        try {
            val calTime = GlobalData.Time.calUtcTimeOffsetTime
            val currentSchedule = getCurrentSelectedTrackingPassWithTime(calTime)
            val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)

            // 1. 현재 추적 상태 변경 처리 (시작/종료)
            if (currentSchedule != lastDisplayedSchedule) {
                handleScheduleChangeUsingExistingMethods(currentSchedule, calTime)
                lastDisplayedSchedule = currentSchedule
            }

            // 2. 추적 준비 상태 처리 (Stow 또는 시작 위치로 이동)
            prepareForTracking(currentSchedule, nextSchedule, calTime)

        } catch (e: Exception) {
            logger.error("추적 체크 중 오류: ${e.message}", e)
        }
    }
    private fun prepareForTracking(currentSchedule: Map<String, Any?>?, nextSchedule: Map<String, Any?>?, calTime: ZonedDateTime) {
        try {
            // 케이스 1: 현재 추적 중인 경우 - 아무 준비 동작 필요 없음
            if (currentSchedule != null) {
                return
            }

            // 케이스 2: 현재 추적 중이 아니고 다음 스케줄이 있는 경우
            if (nextSchedule != null) {
                val nextStartTime = nextSchedule["StartTime"] as? ZonedDateTime ?: return
                val timeUntilNextStart = Duration.between(calTime, nextStartTime)
                val minutesUntilStart = timeUntilNextStart.toMinutes()
                val nextMstId = nextSchedule["No"] as? UInt ?: return

                // 2-1: 다음 스케줄 시작 2분 이내인 경우 - 추적 시작 위치로 이동
                if (minutesUntilStart <= PREPARATION_TIME_MINUTES &&
                    minutesUntilStart >= 0 &&
                    nextSchedule != lastPreparedSchedule) {

                    moveToStartPosition(nextMstId)
                    lastPreparedSchedule = nextSchedule
                    isInStowPosition.set(false)
                    logger.info("⏳ 추적 시작 ${minutesUntilStart}분 전: 시작 위치로 이동 완료")
                }
                // 2-2: 다음 스케줄 시작까지 2분 이상 남은 경우 - Stow 위치로 이동
                else if (minutesUntilStart > PREPARATION_TIME_MINUTES &&
                    !isInStowPosition.get()) {

                    moveToStowPosition(calTime)
                    isInStowPosition.set(true)
                    logger.info("⏳ 추적 시작까지 ${minutesUntilStart}분 남음: Stow 위치로 이동")
                }
            }
            // 케이스 3: 현재 추적 중이 아니고 다음 스케줄도 없는 경우 - 모든 추적 완료, Stow 위치로 이동
            else if (!isInStowPosition.get()) {
                moveToStowPosition(calTime)
                isInStowPosition.set(true)
                lastPreparedSchedule = null
                logger.info("🏁 모든 추적 완료: Stow 위치로 이동")
            }
        } catch (e: Exception) {
            logger.error("추적 준비 중 오류: ${e.message}", e)
        }
    }

    /**
     * 시작 위치로 이동
     */
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
            multiAxis, cmdAzimuthAngle,  // null이면 0.0f 사용
            cmdAzimuthSpeed, cmdElevationAngle, cmdElevationSpeed, cmdTiltAngle ?: 0.0f, cmdTiltSpeed ?: 0.0f
        )
    }

    // Stow 위치로 이동
    private fun moveToStowPosition(calTime: ZonedDateTime) {
        logger.info("🏠 Stow 위치로 이동 (${calTime})")

        // UdpFwICDService의 StowCommand 호출
        udpFwICDService.StowCommand()

        isPreparingForTracking.set(false)
        lastPreparedSchedule = null
    }

    private fun handleScheduleChangeUsingExistingMethods(
        currentSchedule: Map<String, Any?>?, calTime: ZonedDateTime
    ) {
        when {
            // 새로운 추적 시작
            lastDisplayedSchedule == null && currentSchedule != null -> {
                outputCurrentScheduleInfo(currentSchedule, calTime)
                outputNextScheduleInfo(calTime)

                // 추적 시작 명령 실행 (실제 위성 추적 시작)
                val mstId = currentSchedule["No"] as? UInt
                if (mstId != null) {
                    startTracking(currentSchedule)
                }
            }

            // 추적 종료
            lastDisplayedSchedule != null && currentSchedule == null -> {
                outputTrackingEnd(lastDisplayedSchedule!!, calTime)

                // 추적 종료 명령 실행
                stopTracking(lastDisplayedSchedule!!)

                // 다음 스케줄 정보 출력
                val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)
                if (nextSchedule != null) {
                    outputUpcomingScheduleInfo(nextSchedule, calTime)
                } else {
                    outputScheduleFixed(lastDisplayedSchedule!!, calTime)
                }
            }

            // 추적 변경 (한 위성에서 다른 위성으로)
            lastDisplayedSchedule != null && currentSchedule != null &&
                    lastDisplayedSchedule!!["No"] != currentSchedule["No"] -> {
                outputScheduleChange(lastDisplayedSchedule!!, currentSchedule, calTime)
                outputNextScheduleInfo(calTime)

                // 이전 추적 종료 및 새 추적 시작
                stopTracking(lastDisplayedSchedule!!)
                startTracking(currentSchedule)
            }
        }
    }
    private fun startTracking(schedule: Map<String, Any?>) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val mstId = schedule["No"] as? UInt ?: return

        logger.info("🚀 추적 시작: $satelliteName (ID: $mstId)")

        // 여기서 실제 추적 시작 명령을 실행
        // 이미 추적 시작 위치로 이동했으므로 추적 명령만 실행
        // 실제 구현에 맞게 추적 시작 명령 코드 추가 필요
    }

    private fun stopTracking(schedule: Map<String, Any?>) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val mstId = schedule["No"] as? UInt ?: return

        logger.info("🛑 추적 종료: $satelliteName (ID: $mstId)")

        // 여기서 실제 추적 종료 명령을 실행
        // 실제 구현에 맞게 추적 종료 명령 코드 추가 필요
    }


    // stopScheduleTracking() 함수 수정 - 추적 준비 상태 초기화 추가
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

        trackingMonitorExecutor = null
        trackingMonitorTask = null
        lastDisplayedSchedule = null
        lastPreparedSchedule = null
        isPreparingForTracking.set(false)

        logger.info("🛑 추적 모니터링 중지 완료")
    }
    /**
     * ✅ 특정 시간 기준으로 현재 진행 중인 선별된 추적 패스를 조회합니다 (GlobalData.Time 기준)
     */
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

    /**
     * 모든 TLE 데이터에 대해 위성 추적 정보를 생성합니다 (비동기 - 병렬 처리)
     */
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

    /**
     * 특정 위성의 패스 스케줄 추적 데이터를 생성합니다 (비동기)
     */
    fun generatePassScheduleTrackingDataAsync(
        satelliteId: String, tleLine1: String, tleLine2: String, satelliteName: String? = null
    ): Mono<Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>> {
        return Mono.fromCallable {
            // 위성 이름이 제공되지 않은 경우 ID에서 추출
            val actualSatelliteName = satelliteName ?: satelliteId

            logger.info("$actualSatelliteName 위성의 패스 스케줄 추적 시작")

            // 추적 기간 설정 (오늘 00시부터 내일 00시까지)
            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)

            // 추적 스케줄을 위한 마스터 리스트 생성
            val passScheduleTrackMst = mutableListOf<Map<String, Any?>>()

            // 추적 좌표를 위한 세부 리스트 생성
            val passScheduleTrackDtl = mutableListOf<Map<String, Any?>>()

            // 위성 추적 스케줄 생성
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

            // 생성 메타데이터를 위한 현재 날짜와 사용자 정보
            val creationDate = ZonedDateTime.now()
            val creator = "PassScheduleService"

            // 스케줄 정보로 마스터 리스트 채우기
            schedule.trackingPasses.forEachIndexed { index, pass ->
                globalMstId++

                // 시작 시간과 종료 시간에 밀리초 정보 추가
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

                // 추적 좌표로 세부 리스트 채우기
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


    /**
     * 특정 위성의 패스 스케줄 마스터 데이터를 조회합니다.
     */
    fun getPassScheduleTrackMstBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return passScheduleTrackMstStorage[satelliteId]
    }

    /**
     * 특정 위성의 패스 스케줄 세부 데이터를 조회합니다.
     */
    fun getPassScheduleTrackDtlBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return passScheduleTrackDtlStorage[satelliteId]
    }

    /**
     * 특정 위성의 특정 패스에 대한 세부 데이터를 조회합니다.
     */
    fun getPassScheduleTrackDtlByMstId(satelliteId: String, mstId: UInt): List<Map<String, Any?>> {
        val dtlData = passScheduleTrackDtlStorage[satelliteId] ?: return emptyList()
        return dtlData.filter { it["MstId"] == mstId }
    }

    /**
     * 모든 위성의 패스 스케줄 마스터 데이터를 조회합니다.
     */
    fun getAllPassScheduleTrackMst(): Map<String, List<Map<String, Any?>>> {
        return passScheduleTrackMstStorage.toMap()
    }

    /**
     * 모든 위성의 패스 스케줄 세부 데이터를 조회합니다.
     */
    fun getAllPassScheduleTrackDtl(): Map<String, List<Map<String, Any?>>> {
        return passScheduleTrackDtlStorage.toMap()
    }

    /**
     * 특정 위성의 추적 데이터를 삭제합니다.
     */
    fun clearPassScheduleTrackingData(satelliteId: String) {
        passScheduleTrackMstStorage.remove(satelliteId)
        passScheduleTrackDtlStorage.remove(satelliteId)
        logger.info("위성 $satelliteId 의 패스 스케줄 추적 데이터가 삭제되었습니다.")
    }

    /**
     * 모든 위성의 추적 데이터를 삭제합니다.
     */
    fun clearAllPassScheduleTrackingData() {
        val mstSize = passScheduleTrackMstStorage.size
        val dtlSize = passScheduleTrackDtlStorage.values.sumOf { it.size }
        globalMstId = 0;
        passScheduleTrackMstStorage.clear()
        passScheduleTrackDtlStorage.clear()

        logger.info("모든 패스 스케줄 추적 데이터가 삭제되었습니다. (마스터: ${mstSize}개, 세부: ${dtlSize}개)")
    }

    /**
     * ✅ 위성 추적 스케줄 대상 목록을 설정합니다. (수정: 자동으로 선별된 데이터 생성)
     */
    fun setTrackingTargetList(targets: List<TrackingTarget>) {
        synchronized(trackingTargetList) {
            trackingTargetList.clear()
            trackingTargetList.addAll(targets)
        }
        logger.info("위성 추적 스케줄 대상 목록이 설정되었습니다. 총 ${targets.size}개 대상")

        // 대상 목록 로깅
        targets.forEach { target ->
            logger.info("추적 대상: ${target.satelliteName ?: target.satelliteId} (MST ID: ${target.mstId}, 최대 고도: ${target.maxElevation}°, 시작 시간: ${target.startTime}, 최대 고도: ${target.endTime}°)")
        }
        // ✅ 자동으로 선별된 추적 데이터 생성
        generateSelectedTrackingData()
    }

    /**
     * ✅ 위성 추적 스케줄 대상 목록을 조회합니다.
     */
    fun getTrackingTargetList(): List<TrackingTarget> {
        return synchronized(trackingTargetList) {
            trackingTargetList.toList()
        }
    }

    /**
     * ✅ 특정 위성의 추적 대상 목록을 조회합니다.
     */
    fun getTrackingTargetsBySatelliteId(satelliteId: String): List<TrackingTarget> {
        return synchronized(trackingTargetList) {
            trackingTargetList.filter { it.satelliteId == satelliteId }
        }
    }

    /**
     * ✅ 특정 MST ID의 추적 대상을 조회합니다.
     */
    fun getTrackingTargetByMstId(mstId: UInt): TrackingTarget? {
        return synchronized(trackingTargetList) {
            trackingTargetList.find { it.mstId == mstId }
        }
    }

    /**
     * ✅ 추적 대상 목록을 초기화합니다. (수정: 선별된 데이터도 함께 초기화)
     */
    fun clearTrackingTargetList() {
        val size = synchronized(trackingTargetList) {
            val currentSize = trackingTargetList.size
            trackingTargetList.clear()
            currentSize
        }

        // 선별된 추적 데이터도 함께 초기화
        clearSelectedTrackingData()

        logger.info("위성 추적 스케줄 대상 목록이 초기화되었습니다. ${size}개 항목 삭제")
    }


    /**
     * ✅ trackingTargetList를 기준으로 선별된 마스터 데이터를 생성합니다.
     */
    fun generateSelectedTrackingData() {
        synchronized(trackingTargetList) {
            if (trackingTargetList.isEmpty()) {
                logger.warn("추적 대상 목록이 비어있습니다.")
                selectedTrackMstStorage.clear()
                return
            }

            logger.info("선별된 추적 데이터 생성 시작: ${trackingTargetList.size}개 대상")

            // 기존 선별된 데이터 초기화
            selectedTrackMstStorage.clear()

            // 추적 대상의 mstId 목록 추출
            val targetMstIds = trackingTargetList.map { it.mstId }.toSet()

            // 위성별로 필터링
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

    /**
     * ✅ 특정 위성의 선별된 마스터 데이터를 조회합니다.
     */
    fun getSelectedTrackMstBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return selectedTrackMstStorage[satelliteId]
    }

    /**
     * ✅ 모든 위성의 선별된 마스터 데이터를 조회합니다.
     */
    fun getAllSelectedTrackMst(): Map<String, List<Map<String, Any?>>> {
        return selectedTrackMstStorage.toMap()
    }

    /**
     * ✅ 특정 MST ID의 선별된 마스터 데이터를 조회합니다.
     */
    fun getSelectedTrackMstByMstId(mstId: UInt): Map<String, Any?>? {
        selectedTrackMstStorage.values.forEach { mstDataList ->
            val found = mstDataList.find { it["No"] == mstId }
            if (found != null) return found
        }
        return null
    }

    /**
     * ✅ 특정 MST ID의 세부 데이터를 조회합니다 (기존 저장소에서 실시간 조회)
     */
    fun getSelectedTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
        // 먼저 해당 mstId가 선별된 목록에 있는지 확인
        val selectedMst = getSelectedTrackMstByMstId(mstId) ?: return emptyList()
        val satelliteId = selectedMst["SatelliteID"] as? String ?: return emptyList()

        // 기존 세부 데이터 저장소에서 조회
        val allDtlData = passScheduleTrackDtlStorage[satelliteId] ?: return emptyList()
        return allDtlData.filter { it["MstId"] == mstId }
    }

    /**
     * ✅ 선별된 추적 데이터를 시간순으로 정렬하여 조회합니다.
     */
    fun getSelectedTrackingSchedule(): List<Map<String, Any?>> {
        val allSelectedPasses = mutableListOf<Map<String, Any?>>()

        selectedTrackMstStorage.values.forEach { mstDataList ->
            allSelectedPasses.addAll(mstDataList)
        }

        // 시작 시간 기준으로 정렬
        return allSelectedPasses.sortedBy { mstRecord ->
            mstRecord["StartTime"] as? ZonedDateTime
        }
    }

    /**
     * ✅ 현재 시간 기준으로 진행 중인 선별된 추적 패스를 조회합니다.
     */
    fun getCurrentSelectedTrackingPass(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime

        selectedTrackMstStorage.values.forEach { mstDataList ->
            val currentPass = mstDataList.find { mstRecord ->
                val startTime = mstRecord["StartTime"] as? ZonedDateTime
                val endTime = mstRecord["EndTime"] as? ZonedDateTime

                startTime != null && endTime != null && !calTime.isBefore(startTime) && !calTime.isAfter(endTime)
            }
            if (currentPass != null) return currentPass
        }
        return null
    }

    /**
     * ✅ 다음 선별된 추적 패스를 조회합니다.
     */
    fun getNextSelectedTrackingPass(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        return getSelectedTrackingSchedule().filter { mstRecord ->
            val startTime = mstRecord["StartTime"] as? ZonedDateTime
            startTime != null && startTime.isAfter(calTime)
        }.minByOrNull { mstRecord ->
            mstRecord["StartTime"] as ZonedDateTime
        }
    }

    /**
     * ✅ 선별된 추적 데이터를 초기화합니다.
     */
    fun clearSelectedTrackingData() {
        val size = selectedTrackMstStorage.values.sumOf { it.size }
        selectedTrackMstStorage.clear()
        logger.info("선별된 추적 데이터가 초기화되었습니다. ${size}개 패스 삭제")
    }

    /**
     * 추적 데이터 통계 정보를 반환합니다.
     */
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
            "satelliteDetails" to satelliteStats
        )
    }

    // ✅ 기존 필드에 추가 (최소한만)
    private var trackingMonitorExecutor: ScheduledExecutorService? = null
    private var trackingMonitorTask: ScheduledFuture<*>? = null
    private var isTrackingMonitorRunning = AtomicBoolean(false)
    private var lastDisplayedSchedule: Map<String, Any?>? = null

    // ThreadFactory
    private val trackingMonitorThreadFactory = ThreadFactory { runnable ->
        Thread(runnable, "tracking-monitor").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY + 1
        }
    }
    /**
     * ✅ 특정 시간 기준으로 다음 선별된 추적 패스를 조회합니다 (GlobalData.Time 기준)
     */
    private fun getNextSelectedTrackingPassWithTime(targetTime: ZonedDateTime): Map<String, Any?>? {
        return getSelectedTrackingSchedule().filter { mstRecord ->
            val startTime = mstRecord["StartTime"] as? ZonedDateTime
            startTime != null && startTime.isAfter(targetTime)
        }.minByOrNull { mstRecord ->
            mstRecord["StartTime"] as ZonedDateTime
        }
    }
    /**
     * ✅ 현재 스케줄 정보 출력 (GlobalData.Time 기준)
     */
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
        logger.info("   🕐 계산시간: $calTime") // ✅ GlobalData.Time 표시

        // 기존 메서드 활용: 세부 데이터 정보
        if (passId != null) {
            val detailData = getSelectedTrackDtlByMstId(passId)
            logger.info("   📊 추적포인트: ${detailData.size}개")
        }
    }

    /**
     * ✅ 다음 스케줄 정보 출력 (GlobalData.Time 기준)
     */
    private fun outputNextScheduleInfo(calTime: ZonedDateTime) {
        // GlobalData.Time 기준으로 다음 스케줄 조회
        val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)

        if (nextSchedule != null) {
            val nextName = nextSchedule["SatelliteName"] as? String ?: "Unknown"
            val nextStart = nextSchedule["StartTime"] as? ZonedDateTime
            val nextId = nextSchedule["No"] as? UInt
            val nextMaxElevation = nextSchedule["MaxElevation"] as? Double

            if (nextStart != null) {
                val waitTime = Duration.between(calTime, nextStart)
                val minutes = waitTime.toMinutes()
                val seconds = waitTime.seconds % 60

                logger.info("   📅 다음예정: $nextName (ID: $nextId)")
                logger.info("   ⏰ 시작: $nextStart")
                logger.info("   📐 최대고도: ${nextMaxElevation}°")
                logger.info("   ⏳ 대기: ${minutes}분 ${seconds}초")
            }
        } else {
            logger.info("   📭 다음예정: 없음")
        }
    }

    /**
     * ✅ 예정 스케줄 정보 출력 (GlobalData.Time 기준)
     */
    private fun outputUpcomingScheduleInfo(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val startTime = schedule["StartTime"] as? ZonedDateTime
        val passId = schedule["No"] as? UInt

        if (startTime != null) {
            val waitTime = Duration.between(calTime, startTime)
            val minutes = waitTime.toMinutes()
            val seconds = waitTime.seconds % 60

            logger.info("📅 [다음 예정] $satelliteName (ID: $passId)")
            logger.info("   ⏰ 시작예정: $startTime")
            logger.info("   ⏳ 대기시간: ${minutes}분 ${seconds}초")
        }
    }

    /**
     * ✅ 추적 종료 출력 (GlobalData.Time 기준)
     */
    private fun outputTrackingEnd(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val passId = schedule["No"] as? UInt

        logger.info("🏁 [추적 종료] $satelliteName (ID: $passId)")
        logger.info("   🕐 종료시간: $calTime") // ✅ GlobalData.Time 표시
    }

    /**
     * ✅ 스케줄 변경 출력 (GlobalData.Time 기준)
     */
    private fun outputScheduleChange(prev: Map<String, Any?>, new: Map<String, Any?>, calTime: ZonedDateTime) {
        val prevName = prev["SatelliteName"] as? String ?: "Unknown"
        val newName = new["SatelliteName"] as? String ?: "Unknown"
        val prevId = prev["No"] as? UInt
        val newId = new["No"] as? UInt

        logger.info("🔄 [추적 변경] $prevName(ID:$prevId) → $newName(ID:$newId)")
        logger.info("   🕐 변경시간: $calTime") // ✅ GlobalData.Time 표시

        outputCurrentScheduleInfo(new, calTime)
    }

    /**
     * ✅ 스케줄 고정 출력 (GlobalData.Time 기준)
     */
    private fun outputScheduleFixed(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val passId = schedule["No"] as? UInt

        logger.info("📌 [스케줄 고정] $satelliteName (ID: $passId)")
        logger.info("   🕐 고정시간: $calTime") // ✅ GlobalData.Time 표시
        logger.info("   📭 모든 스케줄 완료 - 마지막 스케줄로 고정")
    }

    /**
     * ✅ 현재 표시 중인 스케줄 반환 (GlobalData.Time 기준)
     */
    fun getCurrentDisplayedSchedule(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        return getCurrentSelectedTrackingPassWithTime(calTime) ?: lastDisplayedSchedule
    }

    /**
     * ✅ 추적 모니터링 상태 반환 (GlobalData.Time 기준)
     */
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
            "calTime" to calTime.toString() // ✅ GlobalData.Time 정보 추가
        )
    }

    /**
     * 위성 TLE 데이터를 캐시에 추가합니다.
     */
    fun addPassScheduleTle(satelliteId: String, tleLine1: String, tleLine2: String, satelliteName: String? = null) {
        val finalSatelliteName = satelliteName ?: satelliteId
        passScheduleTleCache[satelliteId] = Triple(tleLine1, tleLine2, finalSatelliteName)
        logger.info("위성 TLE 데이터가 캐시에 추가되었습니다. 위성 ID: $satelliteId, 이름: $finalSatelliteName")
    }

    /**
     * 위성 TLE 데이터를 캐시에서 가져옵니다.
     */
    fun getPassScheduleTle(satelliteId: String): Pair<String, String>? {
        val tleData = passScheduleTleCache[satelliteId]
        return if (tleData != null) {
            Pair(tleData.first, tleData.second)
        } else {
            null
        }
    }

    /**
     * 위성 이름을 가져옵니다.
     */
    fun getPassScheduleSatelliteName(satelliteId: String): String? {
        return passScheduleTleCache[satelliteId]?.third
    }

    /**
     * 위성 TLE 전체 정보를 가져옵니다.
     */
    fun getPassScheduleTleWithName(satelliteId: String): Triple<String, String, String>? {
        return passScheduleTleCache[satelliteId]
    }

    /**
     * 위성 TLE 데이터를 캐시에서 삭제합니다.
     */
    fun removePassScheduleTle(satelliteId: String) {
        passScheduleTleCache.remove(satelliteId)
        // 해당 위성의 추적 데이터도 함께 삭제
        passScheduleTrackMstStorage.remove(satelliteId)
        passScheduleTrackDtlStorage.remove(satelliteId)
        logger.info("위성 TLE 데이터가 캐시에서 삭제되었습니다. 위성 ID: $satelliteId")
    }

    /**
     * 캐시된 모든 위성 ID 목록을 반환합니다.
     */
    fun getAllPassScheduleTleIds(): List<String> {
        return passScheduleTleCache.keys.toList()
    }

    /**
     * 캐시된 TLE 개수를 반환합니다.
     */
    fun getCacheSize(): Int {
        return passScheduleTleCache.size
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    fun clearCache() {
        val size = passScheduleTleCache.size
        passScheduleTleCache.clear()
        passScheduleTrackMstStorage.clear()
        passScheduleTrackDtlStorage.clear()
        logger.info("TLE 캐시 및 추적 데이터 전체 삭제 완료: ${size}개 항목 삭제")
    }

}
