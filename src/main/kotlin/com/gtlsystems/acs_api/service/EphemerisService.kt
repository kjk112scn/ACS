package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.SatelliteTrackingData
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import com.gtlsystems.acs_api.algorithm.axistransformation.CoordinateTransformer
import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.event.subscribeToType
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * 위성 추적 서비스
 * 위성의 위치를 계산하고 추적 정보를 제공합니다.
 */
@Service
class EphemerisService(
    private val orekitCalculator: OrekitCalculator,
    private val acsEventBus: ACSEventBus,
    private val udpFwICDService: UdpFwICDService
) {

    // 밀리초를 포함하는 사용자 정의 포맷터 생성
    private val logger = LoggerFactory.getLogger(javaClass)

    // 위성 TLE 데이터 캐시
    private val satelliteTleCache = ConcurrentHashMap<String, Pair<String, String>>()
    private val trackingData = SatelliteTrackingData.Tracking
    private val locationData = GlobalData.Location

    // 위성 추적 마스터 및 세부 데이터 저장소 (실제로는 데이터베이스를 사용할 것입니다)
    private val ephemerisTrackMstStorage = mutableListOf<Map<String, Any?>>()
    private val ephemerisTrackDtlStorage = mutableListOf<Map<String, Any?>>()

    // 현재 추적 중인 위성 정보
    private var currentTrackingPass: Map<String, Any?>? = null
    private var isTracking = false

    private var currentTrackingPassId: UInt? = null
    private var subscriptions: MutableList<Disposable> = mutableListOf()

    @PostConstruct
    fun init() {

        eventBus()

        val satelliteName = "AQUA   "
        val tle1 = "1 27424U 02022A   25140.87892865  .00001007  00000+0  21407-3 0  9994"
        val tle2 = "2 27424  98.3765  98.8898 0002026  95.1111 287.6547 14.61253205226005"
        //generateEphemerisDesignationTrack(tle1,tle2,satelliteName)
        //compareTrackingPerformance(tle1,tle2)
        //satelliteTest()
    }

    fun eventBus() {
        // 위성 추적 헤더 이벤트 구독
        val headerSubscription = acsEventBus.subscribeToType<ACSEvent.ICDEvent.SatelliteTrackHeaderReceived>()
            .subscribe { event ->
                // 위성 추적 헤더가 수신되면 초기 추적 데이터 전송
                currentTrackingPassId?.let { passId ->
                    sendInitialTrackingData(passId)
                }
            }

        // 위성 추적 데이터 요청 이벤트 구독
        val dataRequestSubscription = acsEventBus.subscribeToType<ACSEvent.ICDEvent.SatelliteTrackDataRequested>()
            .subscribe { event ->
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

    // 서비스 종료 시 구독 해제
    fun destroy() {
        subscriptions.forEach { it.dispose() }
        subscriptions.clear()
    }

    fun satelliteTest() {
        try {

            calculateRotatorAngleTable(
                standardAzimuth = 177.796609998884,
                standardElevation = 46.4621529680836,
                tiltAngle = -6.98,
                rotatorStepDegrees = 356.62
            )
        } catch (e: Exception) {
            logger.error("satellite_Test 실행 중 오류 발생: ${e.message}", e)
        }
    }

    fun calculateRotatorAngleTable(
        standardAzimuth: Double,
        standardElevation: Double,
        tiltAngle: Double,
        rotatorStepDegrees: Double = 30.0
    ) {
        val table = CoordinateTransformer.generateRotatorAngleTable(
            standardAzimuth, standardElevation, tiltAngle, rotatorStepDegrees
        )

        logger.info("회전체 각도에 따른 방위각/고도각 변화 테이블")
        logger.info("표준 좌표: Az=${standardAzimuth}°, El=${standardElevation}°, 기울기=${tiltAngle}°")
        logger.info("─────────────────────────────────────────────")
        logger.info("│ 회전체 각도 │   방위각   │   고도각   │")
        logger.info("─────────────────────────────────────────────")

        table.forEach { (rotatorAngle, az, el) ->
            logger.info(
                "│ ${String.format("%20.8f", rotatorAngle)}° │ ${
                    String.format(
                        "%20.8f",
                        az
                    )
                }° │ ${String.format("%20.8f", el)}° │"
            )
        }

        logger.info("─────────────────────────────────────────────")
    }
    /*
     * 위성 추적 성능 비교 메서드 (3가지 방식)
     * 1. 기존 방식 (100ms 고정 간격)
     * 2. 가변 간격 방식 (필요 시 100ms, 일반적으로 1000ms)
     * 3. 최적화된 새로운 방식 (병렬 처리 + 적응형 간격 + 데이터 압축)
     */
    /**
     * 위성 추적 성능 비교 메서드 (2가지 방식)
     * 1. 기존 방식 (100ms 고정 간격)
     * 2. 가변 간격 방식 (필요 시 100ms, 일반적으로 1000ms)
     */
    fun compareTrackingPerformance(tleLine1: String, tleLine2: String) {
        logger.info("위성 추적 성능 비교 시작 (2가지 방식)")

        // 오늘 날짜 기준
        val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)

        // 1. 기존 방식 (100ms 고정 간격)
        logger.info("1. 기존 방식 (100ms 고정 간격) 실행 중...")
        val startTime1 = System.currentTimeMillis()
        val schedule1 = orekitCalculator.generateSatelliteTrackingSchedule(
            tleLine1 = tleLine1,
            tleLine2 = tleLine2,
            startDate = today,
            durationDays = 2,
            minElevation = trackingData.minElevationAngle,
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            altitude = locationData.altitude,
            trackingIntervalMs = 100  // 고정 간격 100ms
        )
        val endTime1 = System.currentTimeMillis()
        val duration1 = endTime1 - startTime1
        val points1 = schedule1.trackingPasses.sumOf { pass -> pass.trackingData.size }
        val passes1 = schedule1.trackingPasses.size

        // 2. 가변 간격 방식 (필요 시 100ms, 일반적으로 1000ms)
        logger.info("2. 가변 간격 방식 (100ms/1000ms) 실행 중...")
        val startTime2 = System.currentTimeMillis()
        val schedule2 = orekitCalculator.generateSatelliteTrackingScheduleWithVariableInterval(
            tleLine1 = tleLine1,
            tleLine2 = tleLine2,
            startDate = today,
            durationDays = 2,
            minElevation = trackingData.minElevationAngle,
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            altitude = locationData.altitude,
            fineIntervalMs = 100,    // 정밀 계산 간격 100ms
            coarseIntervalMs = 1000,  // 일반 계산 간격 1000ms
            1
        )
        val endTime2 = System.currentTimeMillis()
        val duration2 = endTime2 - startTime2
        val points2 = schedule2.trackingPasses.sumOf { pass -> pass.trackingData.size }
        val passes2 = schedule2.trackingPasses.size

        // 결과 출력
        logger.info("성능 비교 결과:")
        logger.info("1. 기존 방식 (100ms 고정 간격)")
        logger.info("   - 실행 시간: ${duration1}ms")
        logger.info("   - 패스 수: ${passes1}개")
        logger.info("   - 데이터 포인트: ${points1}개")
        logger.info("   - 패스당 평균 포인트: ${if (passes1 > 0) points1 / passes1 else 0}개")

        logger.info("2. 가변 간격 방식 (100ms/1000ms)")
        logger.info("   - 실행 시간: ${duration2}ms")
        logger.info("   - 패스 수: ${passes2}개")
        logger.info("   - 데이터 포인트: ${points2}개")
        logger.info("   - 패스당 평균 포인트: ${if (passes2 > 0) points2 / passes2 else 0}개")
        logger.info("   - 기존 대비 속도: ${String.format("%.2f", duration1.toDouble() / duration2.toDouble())}배")
        logger.info(
            "   - 기존 대비 데이터 감소율: ${
                String.format(
                    "%.2f",
                    (1 - points2.toDouble() / points1.toDouble()) * 100
                )
            }%"
        )

        // 패스별 세부 비교 (첫 번째 패스만)
        if (passes1 > 0 && passes2 > 0) {
            logger.info("첫 번째 패스 세부 비교:")

            val pass1 = schedule1.trackingPasses[0]
            val pass2 = schedule2.trackingPasses[0]

            logger.info(
                "   - 기존 방식: ${pass1.trackingData.size}개 포인트, 최대 고도각: ${
                    String.format(
                        "%.2f",
                        pass1.maxElevation
                    )
                }°"
            )
            logger.info(
                "   - 가변 간격 방식: ${pass2.trackingData.size}개 포인트, 최대 고도각: ${
                    String.format(
                        "%.2f",
                        pass2.maxElevation
                    )
                }°"
            )

            // 각속도 및 각가속도 비교
            logger.info("각속도 및 각가속도 비교:")
            logger.info(
                "   - 기존 방식: 최대 Az속도=${String.format("%.2f", pass1.maxAzimuthRate)}°/s, 최대 El속도=${
                    String.format(
                        "%.2f",
                        pass1.maxElevationRate
                    )
                }°/s"
            )
            logger.info(
                "   - 가변 간격 방식: 최대 Az속도=${
                    String.format(
                        "%.2f",
                        pass2.maxAzimuthRate
                    )
                }°/s, 최대 El속도=${String.format("%.2f", pass2.maxElevationRate)}°/s"
            )

            logger.info(
                "   - 기존 방식: 최대 Az가속도=${
                    String.format(
                        "%.2f",
                        pass1.maxAzimuthAccel
                    )
                }°/s², 최대 El가속도=${String.format("%.2f", pass1.maxElevationAccel)}°/s²"
            )
            logger.info(
                "   - 가변 간격 방식: 최대 Az가속도=${
                    String.format(
                        "%.2f",
                        pass2.maxAzimuthAccel
                    )
                }°/s², 최대 El가속도=${String.format("%.2f", pass2.maxElevationAccel)}°/s²"
            )
        }

        // 정확도 검증 (첫 번째 패스의 시작, 중간, 끝 지점 비교)
        if (passes1 > 0 && passes2 > 0) {
            val pass1 = schedule1.trackingPasses[0]
            val pass2 = schedule2.trackingPasses[0]

            if (pass1.trackingData.isNotEmpty() && pass2.trackingData.isNotEmpty()) {
                logger.info("정확도 검증 (첫 번째 패스):")

                // 시작 지점 비교
                val start1 = pass1.trackingData.first()
                val start2 = pass2.trackingData.first()

                logger.info("시작 지점:")
                logger.info(
                    "   - 기존 방식: Az=${String.format("%.2f", start1.azimuth)}°, El=${
                        String.format(
                            "%.2f",
                            start1.elevation
                        )
                    }°"
                )
                logger.info(
                    "   - 가변 간격 방식: Az=${String.format("%.2f", start2.azimuth)}°, El=${
                        String.format(
                            "%.2f",
                            start2.elevation
                        )
                    }°"
                )

                // 끝 지점 비교
                val end1 = pass1.trackingData.last()
                val end2 = pass2.trackingData.last()

                logger.info("끝 지점:")
                logger.info(
                    "   - 기존 방식: Az=${String.format("%.2f", end1.azimuth)}°, El=${
                        String.format(
                            "%.2f",
                            end1.elevation
                        )
                    }°"
                )
                logger.info(
                    "   - 가변 간격 방식: Az=${String.format("%.2f", end2.azimuth)}°, El=${
                        String.format(
                            "%.2f",
                            end2.elevation
                        )
                    }°"
                )

                // 최대 고도각 시간 비교
                logger.info("최대 고도각 시간:")
                logger.info("   - 기존 방식: ${pass1.maxElevationTime?.format(DateTimeFormatter.ISO_LOCAL_TIME) ?: "N/A"}")
                logger.info("   - 가변 간격 방식: ${pass2.maxElevationTime?.format(DateTimeFormatter.ISO_LOCAL_TIME) ?: "N/A"}")
            }
        }

        // 메모리 사용량 비교 (대략적인 추정)
        val memory1 = points1 * 40 // 각 데이터 포인트는 약 40바이트로 가정
        val memory2 = points2 * 40

        logger.info("메모리 사용량 추정:")
        logger.info("   - 기존 방식: ${memory1 / 1024} KB")
        logger.info("   - 가변 간격 방식: ${memory2 / 1024} KB (${String.format("%.2f", memory2 * 100.0 / memory1)}%)")

        logger.info("위성 추적 성능 비교 완료")

        // 결과 요약
        logger.info("성능 비교 요약:")
        logger.info("1. 기존 방식 (100ms 고정 간격): ${duration1}ms, ${points1}개 포인트")
        logger.info(
            "2. 가변 간격 방식: ${duration2}ms (${
                String.format(
                    "%.2f",
                    duration1.toDouble() / duration2.toDouble()
                )
            }배 빠름), ${points2}개 포인트 (${String.format("%.2f", points2 * 100.0 / points1)}%)"
        )
    }

    /**
     * TLE 데이터로 위성 궤도 추적
     * 위성 이름이 제공되지 않으면 TLE에서 추출
     */
    fun generateEphemerisDesignationTrack(
        tleLine1: String,
        tleLine2: String,
        satelliteName: String? = null
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        try {
            // TLE에서 위성 ID 추출
            val satelliteId = tleLine1.substring(2, 7).trim()

            // 위성 이름이 제공되지 않은 경우 ID에서 추출
            val actualSatelliteName = satelliteName ?: getSatelliteNameFromId(satelliteId)

            logger.info("$actualSatelliteName 위성의 궤도 추적 시작")

            // 추적 기간 설정 (오늘 00시부터 내일 00시까지)
            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)

            // 추적 스케줄을 위한 마스터 리스트 생성
            val ephemerisTrackMst = mutableListOf<Map<String, Any?>>()

            // 추적 좌표를 위한 세부 리스트 생성
            val ephemerisTrackDtl = mutableListOf<Map<String, Any?>>()

            // 위성 추적 스케줄 생성
            val schedule = orekitCalculator.generateSatelliteTrackingScheduleWithVariableInterval(
                tleLine1 = tleLine1,
                tleLine2 = tleLine2,
                startDate = today.withZoneSameInstant(ZoneOffset.UTC),
                durationDays = 2,
                minElevation = trackingData.minElevationAngle,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                altitude = locationData.altitude,
                fineIntervalMs = 100,    // 정밀 계산 간격 100ms
                coarseIntervalMs = 1000,  // 일반 계산 간격 1000ms
                transitionSeconds = 1
            )

            logger.info("위성 추적 스케줄 생성 완료: ${schedule.trackingPasses.size}개 패스")

            // 생성 메타데이터를 위한 현재 날짜와 사용자 정보
            val creationDate = ZonedDateTime.now()
            val creator = "System"

            // 스케줄 정보로 마스터 리스트 채우기
            schedule.trackingPasses.forEachIndexed { index, pass ->
                val mstId = index + 1

                // 시작 시간과 종료 시간에 밀리초 정보 추가
                val startTimeWithMs = pass.startTime.withZoneSameInstant(ZoneOffset.UTC)
                val endTimeWithMs = pass.endTime.withZoneSameInstant(ZoneOffset.UTC)

                logger.info("패스 #$mstId: 시작=$startTimeWithMs, 종료=$endTimeWithMs")

                ephemerisTrackMst.add(
                    mapOf(
                        "No" to mstId.toUInt(),
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
                    ephemerisTrackDtl.add(
                        mapOf(
                            "No" to (dtlIndex + 1).toUInt(),
                            "MstId" to mstId.toUInt(),  // 마스터 리스트의 No 값을 MstId로 사용
                            "Time" to data.timestamp,
                            "Azimuth" to data.azimuth,
                            "Elevation" to data.elevation,
                            "Range" to data.range,
                            "Altitude" to data.altitude
                        )
                    )
                }
            }

            logger.info("위성 궤도 추적 데이터 생성 완료: ${ephemerisTrackMst.size}개 스케줄 항목과 ${ephemerisTrackDtl.size}개 좌표 포인트")

            // 저장소에 데이터 저장
            ephemerisTrackMstStorage.clear()
            ephemerisTrackDtlStorage.clear()
            ephemerisTrackMstStorage.addAll(ephemerisTrackMst)
            ephemerisTrackDtlStorage.addAll(ephemerisTrackDtl)
            return Pair(ephemerisTrackMst, ephemerisTrackDtl)

        } catch (e: Exception) {
            logger.error("위성 궤도 추적 중 오류 발생: ${e.message}", e)
            throw e
        }
    }

    /**
     * 위성 추적 시작 - 헤더 정보 전송
     * 2.12.1 위성 추적 해더 정보 송신 프로토콜 사용
     */
    fun startEphemerisTracking(passId: UInt) {
        try {
            currentTrackingPassId = passId
            // 선택된 패스 ID에 해당하는 마스터 데이터 찾기
            val selectedPass = ephemerisTrackMstStorage.find { it["No"] == passId }

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

            // 2.12.1 위성 추적 헤더 정보 송신 프로토콜 생성
            val headerFrame = ICDService.SatelliteTrackOne.SetDataFrame(
                cmdOne = 'T',
                cmdTwo = 'T',
                dataLen = calculateDataLength(passId).toUShort(), // 전체 데이터 길이 계산
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
            isTracking = true

        } catch (e: Exception) {
            logger.error("위성 추적 시작 중 오류 발생: ${e.message}", e)
        }
    }

    /**
     * 위성 추적 초기 제어 명령 전송
     * 2.12.2 위성 추적 초기 제어 명령 프로토콜 사용
     */
    fun sendInitialTrackingData(passId: UInt) {
        try {
            if (currentTrackingPass == null || !isTracking) {
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
                        "실시간 추적 정보: 진행률={:.1f}%, 인덱스={}/{}, 추출={}개",
                        progressPercentage, safeStartIndex, totalSize, actualCount
                    )


                    initialTrackingData = passDetails
                        .drop(safeStartIndex)
                        .take(actualCount)
                        .mapIndexed { index, point ->
                            Triple(
                                ((safeStartIndex + index) * 100).toUInt(),
                                (point["Elevation"] as Double).toFloat(),
                                (point["Azimuth"] as Double).toFloat()
                            )
                        }
                    // 현재 위치 정보 로깅
                    val currentPoint = initialTrackingData.firstOrNull()
                    if (currentPoint != null) {
                        logger.info("현재 추적 위치: 시간={}ms, 고도={:.2f}°, 방위={:.2f}°",
                            currentPoint.first, currentPoint.second, currentPoint.third)
                    }
                }

                TimeRangeStatus.BEFORE_START -> {
                    logger.info("추적 시작 전입니다. 대기 중...")
                    // 대기 로직
                    val timeUntilStart = Duration.between(calTime, startTime)
                    val secondsUntilStart = timeUntilStart.seconds
                    val minutesUntilStart = timeUntilStart.toMinutes()

                    logger.info("추적 시작까지: {}분 {}초 (총 {}초)",
                        minutesUntilStart, secondsUntilStart % 60, secondsUntilStart)
                    // ✅ 대기 모드: 초기 궤도 데이터 미리 준비
                    initialTrackingData = passDetails.take(50).mapIndexed { index, point ->
                        Triple(
                            (index * 100).toUInt(), //
                            (point["Elevation"] as Double).toFloat(),
                            (point["Azimuth"] as Double).toFloat()
                        )
                    }
                    // 시작 예정 위치 정보
                    val startPoint = initialTrackingData.firstOrNull()
                    if (startPoint != null) {
                        logger.info("시작 예정 위치: 고도={:.2f}°, 방위={:.2f}°",
                            startPoint.second, startPoint.third)
                    }
                }
                TimeRangeStatus.AFTER_END -> {
                    logger.warn("추적 종료 후입니다. 추적을 중지합니다")
                    // 추적 중지 로직
                }
            }
            if (passDetails.isEmpty()) {
                logger.error("선택된 패스 ID($passId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
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
            logger.error("위성 추적 초기 제어 명령 전송 중 오류 발생: ${e.message}", e)
        }
    }

    // 열거형 정의
    enum class TimeRangeStatus {
        BEFORE_START,
        IN_RANGE,
        AFTER_END
    }

    // 시간 범위 체크 함수
    private fun checkTimeInTrackingRange(
        currentTime: ZonedDateTime,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
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
                    "추적 진행 중 - 시작 후: {}초, 종료까지: {}초",
                    timeFromStart.seconds, timeToEnd.seconds
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
        if (!isTracking || currentTrackingPass == null) {
            logger.error("위성 추적이 활성화되어 있지 않습니다.")
            return
        }
        logger.info("timeAcc :${timeAcc}.")
        logger.info("requestDataLength :${requestDataLength}.")
        val passId = currentTrackingPass!!["No"] as UInt

        // timeAcc를 기반으로 시작 인덱스 계산 (timeAcc는 ms 단위)
        val startIndex = (timeAcc.toInt()) //

        // 요청된 데이터 길이에 따라 데이터 포인트 수 계산


        sendAdditionalTrackingData(passId, startIndex, requestDataLength.toInt())
    }

    /**
     * 위성 추적 추가 데이터 전송
     * 2.12.3 위성 추적 추가 데이터 요청에 대한 응답으로 사용
     */
    fun sendAdditionalTrackingData(passId: UInt, startIndex: Int, requestDataLength: Int = 25) {
        try {
            if (currentTrackingPass == null || !isTracking) {
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
            val additionalTrackingData = passDetails
                .drop(indexMs)
                .take(requestDataLength)
                .mapIndexed { index, point ->
                    Triple(
                        startIndex + index * 100, // 카운트 (누적 인덱스)
                        (point["Elevation"] as Double).toFloat(),
                        (point["Azimuth"] as Double).toFloat()
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
                dataLength = calculateAdditionalDataLength(additionalTrackingData.size).toUShort(),
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
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("시간 오프셋 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    fun setCurrentTrackingPassId(newPassId: UInt?) {
        // 유효성 검사
        if (newPassId != null && newPassId <= 0u) {
            throw IllegalArgumentException("패스 ID는 양수여야 합니다: $newPassId")
        }
        // 새 패스 ID 설정
        currentTrackingPassId = newPassId
    }

    /**
     * 위성 추적 중지
     */
    fun stopEphemerisTracking() {
        if (!isTracking) {
            logger.info("위성 추적이 이미 중지되어 있습니다.")
            return
        }

        logger.info("위성 추적 중지")
        isTracking = false
        //currentTrackingPass = null
        //currentTrackingPassId = null
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
        val pass = currentTrackingPass
            ?: throw IllegalStateException("현재 추적 중인 패스가 설정되지 않았습니다")

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

    /**
     * 추가 데이터 길이 계산
     */
    private fun calculateAdditionalDataByteSize(dataPointCount: Int): Int {
        return 5 + (dataPointCount * 12) // 헤더 5바이트 + 각 데이터 포인트 12바이트
    }

    private fun calculateAdditionalDataLength(dataPointCount: Int): Int {
        return dataPointCount// 헤더 5바이트 + 각 데이터 포인트 12바이트
    }

    /**
     * 전체 데이터 길이 계산
     */
    private fun calculateDataLength(passId: UInt): Int {
        val passDetails = getEphemerisTrackDtlByMstId(passId)
        return passDetails.size // 각 데이터 포인트는 12바이트 (4바이트 시간, 4바이트 방위각, 4바이트 고도각)
    }

    /**
     * 현재 추적 상태를 반환합니다.
     */
    fun isTracking(): Boolean {
        return isTracking
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
     * 특정 마스터 ID에 해당하는 세부 추적 데이터 조회
     */
    fun getEphemerisTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
        return ephemerisTrackDtlStorage.filter { it["MstId"] == mstId }
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
}