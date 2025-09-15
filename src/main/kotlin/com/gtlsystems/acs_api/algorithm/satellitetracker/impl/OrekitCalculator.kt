package com.gtlsystems.acs_api.algorithm.satellitetracker.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import com.gtlsystems.acs_api.config.OrekitConfig
import org.orekit.time.AbsoluteDate
import com.gtlsystems.acs_api.model.GlobalData
import org.hipparchus.util.FastMath
import org.orekit.bodies.GeodeticPoint
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.data.DataContext
import org.orekit.frames.Frame
import org.orekit.frames.FramesFactory
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.time.TimeScale
import org.orekit.time.TimeScalesFactory
import org.orekit.utils.Constants
import org.orekit.utils.IERSConventions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.net.JarURLConnection
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


/**
 * Orekit 라이브러리를 사용하여 위성 위치를 계산하는 클래스
 */
@Service
class OrekitCalculator(
    private val utcTimeScale: TimeScale,
    private val earthFrame: Frame,
    private val earthModel: OneAxisEllipsoid,
    private val orekitStatus: OrekitConfig.OrekitInitializationStatus,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var isOrekitInitialized = true

    // 수정 후 - 간단한 초기화 확인만
    init {
        // Orekit 초기화 상태 확인
        if (orekitStatus.isInitialized) {
            isOrekitInitialized = true
            logger.info("OrekitCalculator 초기화 완료 (데이터 프로바이더: ${orekitStatus.dataProvidersCount}개)")
        } else {
            logger.error("Orekit이 초기화되지 않았습니다!")
            throw RuntimeException("Orekit 초기화 실패")
        }
    }

    /**
     * 지정된 시간과 위치에 대한 위성 위치를 계산합니다.
     */
    fun calculatePosition(
        tleLine1: String,
        tleLine2: String,
        dateTime: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): SatelliteTrackData {
        try {
            // TLE 객체 생성
            val tle = TLE(tleLine1, tleLine2)
            // 지상국 위치 설정
            // TLE 전파기(propagator) 생성 - 최신 API 사용
            val propagator = TLEPropagator.selectExtrapolator(tle)

            // 지상국 위치 설정
            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            // 지상국 기준 위치 프레임 생성
            val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")

            val date = AbsoluteDate(
                dateTime.year, dateTime.monthValue, dateTime.dayOfMonth,
                dateTime.hour, dateTime.minute, dateTime.second + dateTime.nano / 1e9,
                utcTimeScale
            )
            // 해당 시간의 위성 위치 계산
            // 해당 시간의 위성 상태 계산
            val state = propagator.propagate(date)

            // 지상국에서 본 위성의 위치 계산
            val pvInStation = state.getPVCoordinates(stationFrame)
            val posInStation = pvInStation.position

            // 직교 좌표를 구면 좌표로 변환
            val x = posInStation.x
            val y = posInStation.y
            val z = posInStation.z
            val distance = posInStation.norm

            // 고도각 계산
            val elevation = FastMath.asin(z / distance)

            // 일반적인 방위각 계산 (북쪽이 0도, 동쪽이 90도)
            val azimuth = FastMath.atan2(x, y)

            // 라디안에서 도로 변환
            val elevationDegrees = FastMath.toDegrees(elevation)
            val azimuthDegrees = FastMath.toDegrees(azimuth)

            // 방위각을 0-360도 범위로 조정
            val normalizedAzimuth = if (azimuthDegrees < 0) azimuthDegrees + 360.0 else azimuthDegrees

            // 디버깅을 위한 로그 추가
            logger.info("원시 좌표 - x: $x, y: $y, z: $z")
            logger.info("방위각 계산 - atan2(y, x): ${FastMath.toDegrees(FastMath.atan2(x, y))}")

            // 위성의 지구 중심 좌표에서 고도 계산 (참고용)
            val satellitePosition = state.getPVCoordinates(earthFrame).position
            val satelliteRadius = satellitePosition.norm
            val satelliteAltitude = (satelliteRadius - Constants.WGS84_EARTH_EQUATORIAL_RADIUS) / 1000.0 // km

            logger.info("위성 고도: ${satelliteAltitude}km, 지상국에서 거리: ${distance / 1000.0}km")

            // Double 타입으로 모든 값을 반환
            return SatelliteTrackData(
                azimuth = normalizedAzimuth,
                elevation = elevationDegrees,
                timestamp = dateTime,
                range = distance / 1000.0, // 미터에서 킬로미터로 변환
                altitude = satelliteAltitude // 위성 고도
            )
        } catch (e: Exception) {
            logger.error("위성 위치 계산 중 오류 발생: ${e.message}", e)
            throw e
        }
    }

    /**
     * 현재 시간의 위성 위치를 계산합니다
     */
    fun getCurrentPosition(
        tleLine1: String,
        tleLine2: String,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0
    ): SatelliteTrackData {
        val now = ZonedDateTime.now()
        return calculatePosition(tleLine1, tleLine2, GlobalData.Time.utcNow, latitude, longitude, altitude)
    }
    /**
     * 지정된 기간 동안 위성 추적 스케줄을 생성합니다.
     */
    fun generateSatelliteTrackingSchedule(
        tleLine1: String,
        tleLine2: String,
        startDate: ZonedDateTime,
        durationDays: Int = 1,
        minElevation: Float = 0.0f,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        trackingIntervalMs: Int = 100
    ): SatelliteTrackingSchedule {
        logger.info("위성 추적 스케줄 생성 시작: ${startDate}, 기간: ${durationDays}일")
        val endDate = startDate.plusDays(durationDays.toLong())
        logger.info("스케줄 기간: ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} ~ ${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")

        try {
            // 1. 먼저 가시성 기간을 계산 (시간 간격을 늘려 빠르게 계산)
            logger.info("가시성 기간 계산 시작...")
            val visibilityStartTime = System.currentTimeMillis()
            val visibilityPeriods = calculateVisibilityPeriodsWithMaxElevation(
                tleLine1, tleLine2, startDate, durationDays, minElevation,
                latitude, longitude, altitude, 100 // 100ms 간격으로 계산
            )
            val visibilityEndTime = System.currentTimeMillis()
            val visibilityDuration = visibilityEndTime - visibilityStartTime
            logger.info("가시성 기간 계산 완료: ${visibilityPeriods.size}개 기간 발견 (소요 시간: ${visibilityDuration}ms)")

            // 날짜별 가시성 기간 수 로깅
            val periodsByDate = visibilityPeriods.groupBy { it.startTime.toLocalDate() }
            periodsByDate.forEach { (date, periods) ->
                logger.info("${date} 날짜의 가시성 기간 수: ${periods.size}개")
            }

            // 2. 각 가시성 기간에 대해 상세 추적 데이터 생성
            logger.info("상세 추적 데이터 생성 시작...")
            val trackingPasses = visibilityPeriods.mapIndexed { index, period ->
                logger.info("패스 ${index + 1}/${visibilityPeriods.size} 처리 중: ${period.startTime} ~ ${period.endTime}")

                // 각 가시성 기간에 대한 상세 추적 데이터 생성
                val detailedTrackingData = generateDetailedTrackingData(
                    tleLine1, tleLine2, period.startTime, period.endTime,
                    trackingIntervalMs, latitude, longitude, altitude, minElevation
                )
                logger.info("패스 ${index + 1} 데이터 생성 완료: ${detailedTrackingData.size}개 포인트")

                // 데이터가 없는 경우 경고 로그
                if (detailedTrackingData.isEmpty()) {
                    logger.warn("패스 ${index + 1}에 대한 상세 추적 데이터가 생성되지 않았습니다!")
                    logger.warn("패스 정보: 시작=${period.startTime}, 종료=${period.endTime}, 최대고도각=${period.maxElevation}°")
                }

                // 시작 및 종료 각도 추출
                val startAzimuth = detailedTrackingData.firstOrNull()?.azimuth ?: 0.0
                val startElevation = detailedTrackingData.firstOrNull()?.elevation ?: 0.0
                val endAzimuth = detailedTrackingData.lastOrNull()?.azimuth ?: 0.0
                val endElevation = detailedTrackingData.lastOrNull()?.elevation ?: 0.0

                SatelliteTrackingPass(
                    startTime = period.startTime,
                    endTime = period.endTime,
                    maxElevation = period.maxElevation,
                    maxElevationTime = period.maxElevationTime,
                    duration = period.duration,
                    trackingData = detailedTrackingData,
                    maxAzimuthRate = period.maxAzimuthRate,
                    maxElevationRate = period.maxElevationRate,
                    maxAzimuthAccel = period.maxAzimuthAccel,
                    maxElevationAccel = period.maxElevationAccel,
                    startAzimuth = startAzimuth,
                    startElevation = startElevation,
                    endAzimuth = endAzimuth,
                    endElevation = endElevation
                )
            }
            logger.info("상세 추적 데이터 생성 완료: 총 ${trackingPasses.sumOf { it.trackingData.size }}개 데이터 포인트")

            return SatelliteTrackingSchedule(
                satelliteTle1 = tleLine1,
                satelliteTle2 = tleLine2,
                startDate = startDate,
                endDate = endDate,
                stationLatitude = latitude,
                stationLongitude = longitude,
                stationAltitude = altitude,
                minElevation = minElevation,
                trackingIntervalMs = trackingIntervalMs,
                trackingPasses = trackingPasses
            )
        } catch (e: Exception) {
            logger.error("위성 추적 스케줄 생성 중 오류 발생: ${e.message}", e)
            e.printStackTrace()  // 스택 트레이스 출력
            throw e
        }
    }

    /**
     * 지정된 시간 범위 내에서 상세 추적 데이터를 생성합니다.
     * 시작과 종료 부근에서는 작은 간격으로, 그 사이에는 큰 간격으로 계산합니다.
     */
    fun generateDetailedTrackingData(
        tleLine1: String,
        tleLine2: String,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        intervalMs: Int = 100,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        minElevation: Float = 0.0f
    ): List<SatelliteTrackData> {
        val trackingData = mutableListOf<SatelliteTrackData>()

        logger.info(
            "상세 추적 데이터 생성 시작: ${startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} ~ ${
                endTime.format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                )
            }"
        )
        logger.info("간격: ${intervalMs}ms, 최소 고도각: ${minElevation}°")

        try {
            // TLE 객체 생성
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)

            // 지상국 위치 설정
            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")

            // 시간 간격으로 위성 위치 계산
            var currentTime = startTime
            var pointsCalculated = 0
            var pointsAdded = 0
            var pointsFiltered = 0
            val filteredData = mutableListOf<Triple<ZonedDateTime, Double, Double>>() // 필터링된 데이터 저장용

            while (!currentTime.isAfter(endTime)) {
                pointsCalculated++

                val date = AbsoluteDate(
                    currentTime.year, currentTime.monthValue, currentTime.dayOfMonth,
                    currentTime.hour, currentTime.minute, currentTime.second + currentTime.nano / 1e9,
                    utcTimeScale
                )

                val state = propagator.propagate(date)
                val pvInStation = state.getPVCoordinates(stationFrame)
                val posInStation = pvInStation.position

                // 직교 좌표를 구면 좌표로 변환
                val x = posInStation.x
                val y = posInStation.y
                val z = posInStation.z
                val distance = posInStation.norm

                // 고도각 계산
                val elevation = FastMath.toDegrees(FastMath.asin(z / distance))

                // 최소 고도각 이상인 경우에만 데이터 추가
                if (elevation >= minElevation) {
                    // 방위각 계산
                    val azimuth = FastMath.toDegrees(FastMath.atan2(x, y))
                    val normalizedAzimuth = if (azimuth < 0) azimuth + 360.0 else azimuth

                    // 위성의 지구 중심 좌표에서 고도 계산
                    val satellitePosition = state.getPVCoordinates(earthFrame).position
                    val satelliteRadius = satellitePosition.norm
                    val satelliteAltitude = (satelliteRadius - Constants.WGS84_EARTH_EQUATORIAL_RADIUS) / 1000.0 // km

                    trackingData.add(
                        SatelliteTrackData(
                            azimuth = normalizedAzimuth,
                            elevation = elevation,
                            timestamp = currentTime,
                            range = distance / 1000.0, // 미터에서 킬로미터로 변환
                            altitude = satelliteAltitude
                        )
                    )
                    pointsAdded++
                } else {
                    // 방위각 계산 (필터링된 데이터에도 방위각 정보 포함)
                    val azimuth = FastMath.toDegrees(FastMath.atan2(x, y))
                    val normalizedAzimuth = if (azimuth < 0) azimuth + 360.0 else azimuth

                    // 필터링된 데이터 저장 (시간, 고도각, 방위각)
                    filteredData.add(Triple(currentTime, elevation, normalizedAzimuth))
                    pointsFiltered++
                }

                // 다음 시간으로 이동 (밀리초 단위)
                currentTime = currentTime.plus(intervalMs.toLong(), ChronoUnit.MILLIS)
            }

            logger.info("상세 추적 데이터 생성 완료:")
            logger.info("- 계산된 포인트: $pointsCalculated")
            logger.info("- 추가된 포인트: $pointsAdded")
            logger.info("- 필터링된 포인트: $pointsFiltered (최소 고도각 미만)")

            // 필터링된 데이터 상세 정보 출력
            if (filteredData.isNotEmpty()) {
                logger.info("🔍 필터링된 데이터 상세 정보:")
                filteredData.forEachIndexed { index, (time, elevation, azimuth) ->
                    logger.info(
                        "  필터링 #${index + 1}: 시간=${time.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))}, 고도각=${
                            String.format(
                                "%.4f",
                                elevation
                            )
                        }°, 방위각=${String.format("%.4f", azimuth)}°"
                    )
                }
            }

            // 데이터가 없는 경우 로그 출력
            if (trackingData.isEmpty()) {
                logger.warn("생성된 추적 데이터가 없습니다! 시간 범위나 최소 고도각 설정을 확인하세요.")
                logger.warn("시작 시간: $startTime, 종료 시간: $endTime, 최소 고도각: $minElevation°")

                // 테스트 목적으로 최소 고도각 없이 몇 개의 포인트 계산
                val testPoints = 5
                logger.info("테스트: 최소 고도각 제한 없이 처음 $testPoints 포인트의 고도각 값 확인")

                currentTime = startTime
                for (i in 1..testPoints) {
                    val date = AbsoluteDate(
                        currentTime.year, currentTime.monthValue, currentTime.dayOfMonth,
                        currentTime.hour, currentTime.minute, currentTime.second + currentTime.nano / 1e9,
                        utcTimeScale
                    )

                    val state = propagator.propagate(date)
                    val pvInStation = state.getPVCoordinates(stationFrame)
                    val posInStation = pvInStation.position

                    val x = posInStation.x
                    val y = posInStation.y
                    val z = posInStation.z
                    val distance = posInStation.norm

                    val elevation = FastMath.toDegrees(FastMath.asin(z / distance))
                    logger.info("포인트 $i: 시간=${currentTime.format(DateTimeFormatter.ISO_LOCAL_TIME)}, 고도각=${elevation}°")

                    currentTime = currentTime.plus(intervalMs.toLong(), ChronoUnit.MILLIS)
                }
            }

            return trackingData
        } catch (e: Exception) {
            logger.error("상세 추적 데이터 생성 중 오류 발생: ${e.message}", e)
            e.printStackTrace()  // 스택 트레이스 출력
            throw e
        }
    }
    /**
     * 위성 추적 스케줄 데이터 클래스
     */
    data class SatelliteTrackingSchedule(
        val satelliteTle1: String,
        val satelliteTle2: String,
        val startDate: ZonedDateTime,
        val endDate: ZonedDateTime,
        val stationLatitude: Double,
        val stationLongitude: Double,
        val stationAltitude: Double,
        val minElevation: Float,
        val trackingIntervalMs: Int,
        val trackingPasses: List<SatelliteTrackingPass>
    ) {
        // 총 패스 수
        val totalPasses: Int = trackingPasses.size

        // 총 추적 시간
        val totalTrackingDuration: Duration = trackingPasses
            .map { it.duration }
            .fold(Duration.ZERO) { acc, duration -> acc.plus(duration) }

        // 총 추적 시간 문자열
        fun getTotalTrackingDurationString(): String {
            val hours = totalTrackingDuration.toHours()
            val minutes = totalTrackingDuration.toMinutesPart()
            val seconds = totalTrackingDuration.toSecondsPart()
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        // 요약 정보
        fun getSummary(): String {
            return "위성 추적 스케줄 요약:\n" +
                    "- 기간: ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} ~ ${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}\n" +
                    "- 총 패스 수: $totalPasses\n" +
                    "- 총 추적 시간: ${getTotalTrackingDurationString()}\n" +
                    "- 최소 고도각: $minElevation°\n" +
                    "- 추적 데이터 간격: $trackingIntervalMs ms"
        }
    }

    /**
     * 위성 추적 패스 데이터 클래스
     */
    data class SatelliteTrackingPass(
        val startTime: ZonedDateTime,
        val endTime: ZonedDateTime,
        val maxElevation: Double,  // Float에서 Double로 변경
        val maxElevationTime: ZonedDateTime?,
        val duration: Duration,
        val trackingData: List<SatelliteTrackData>,
        val maxAzimuthRate: Double = 0.0,         // 최대 방위각 속도 (도/초)
        val maxElevationRate: Double = 0.0,       // 최대 고도각 속도 (도/초)
        val maxAzimuthAccel: Double = 0.0,        // 최대 방위각 가속도 (도/초²)
        val maxElevationAccel: Double = 0.0,       // 최대 고도각 가속도 (도/초²)
        val startAzimuth: Double = 0.0,
        val startElevation: Double = 0.0,
        val endAzimuth: Double = 0.0,
        val endElevation: Double = 0.0
    ) {
        // 추적 데이터 포인트 수
        val dataPointCount: Int = trackingData.size

        // 지속 시간 문자열
        fun getDurationString(): String {
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            val seconds = duration.toSecondsPart()
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        fun getSummary(): String {
            return "패스 정보:\n" +
                    "- 시작: ${startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}\n" +
                    "- 종료: ${endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}\n" +
                    "- 최대 고도각: ${
                        String.format(
                            "%.2f",
                            maxElevation
                        )
                    }° (${maxElevationTime?.format(DateTimeFormatter.ISO_LOCAL_TIME)})\n" +
                    "- 지속 시간: ${getDurationString()}\n" +
                    "- 데이터 포인트: $dataPointCount\n" +
                    "- 최대 방위각 각속도: ${String.format("%.2f", maxAzimuthRate)}°/s\n" +
                    "- 최대 고도각 각속도: ${String.format("%.2f", maxElevationRate)}°/s\n" +
                    "- 최대 방위각 각가속도: ${String.format("%.2f", maxAzimuthAccel)}°/s²\n" +
                    "- 최대 고도각 각가속도: ${String.format("%.2f", maxElevationAccel)}°/s²"
        }
    }

    /**
     * ZonedDateTime을 Orekit의 AbsoluteDate로 변환하는 함수
     */
    private fun toAbsoluteDate(dateTime: ZonedDateTime): AbsoluteDate {
        val utcScale = TimeScalesFactory.getUTC()
        return AbsoluteDate(
            dateTime.year, dateTime.monthValue, dateTime.dayOfMonth,
            dateTime.hour, dateTime.minute, dateTime.second + dateTime.nano / 1e9,
            utcScale
        )
    }


    /**
     * 특정 기간 동안 최소 고도각 이상인 위성 가시성 기간을 계산합니다.
     *
     * @param tleLine1 TLE 첫 번째 줄
     * @param tleLine2 TLE 두 번째 줄
     * @param startTime 시작 시간
     * @param durationDays 계산할 기간(일)
     * @param minElevation 최소 고도각(도)
     * @param latitude 지상국 위도
     * @param longitude 지상국 경도
     * @param altitude 지상국 고도
     * @param stepMinutes 계산 간격(분)
     * @return 가시성 기간 목록 (시작 시간, 종료 시간, 최대 고도각)
     */
    /**
     * 특정 기간 동안 최소 고도각 이상인 위성 가시성 기간을 계산합니다.
     * 가시성 시작과 종료 부근에서는 작은 간격으로, 그 사이에는 큰 간격으로 계산합니다.
     */
    fun calculateVisibilityPeriodsWithMaxElevation(
        tleLine1: String,
        tleLine2: String,
        startTime: ZonedDateTime,
        durationDays: Int = 1,
        minElevation: Float = 0.0f,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        fineTimeStepMs: Int = 100,    // 정밀 계산 간격 (ms)
        coarseTimeStepMs: Int = 1000  // 일반 계산 간격 (ms)
    ): List<VisibilityPeriod> {
        val endTime = startTime.plusDays(durationDays.toLong())
        logger.info(
            "가시성 기간 계산: ${startTime.format(DateTimeFormatter.ISO_LOCAL_DATE)} ~ ${
                endTime.format(
                    DateTimeFormatter.ISO_LOCAL_DATE
                )
            }"
        )
        logger.info("정밀 계산 간격: ${fineTimeStepMs}ms, 일반 계산 간격: ${coarseTimeStepMs}ms")

        val visibilityPeriods = mutableListOf<VisibilityPeriod>()
        var visibilityStart: ZonedDateTime? = null
        var maxElevationInPass: Double = -90.0
        var maxElevationTime: ZonedDateTime? = null

        // 속도 및 가속도 계산을 위한 변수들
        var maxAzimuthRate: Double = 0.0
        var maxElevationRate: Double = 0.0
        var maxAzimuthAccel: Double = 0.0
        var maxElevationAccel: Double = 0.0

        // 이전 값들을 저장하기 위한 변수들
        var prevAzimuth: Double? = null
        var prevElevation: Double? = null
        var prevAzimuthRate: Double? = null
        var prevElevationRate: Double? = null
        var prevTime: ZonedDateTime? = null

        // 가시성 상태 변화 감지를 위한 변수
        var isVisible = false
        var wasVisible = false
        var transitionDetected = false
        var currentTimeStep = coarseTimeStepMs // 기본적으로 큰 간격 사용

        try {
            // TLE 객체 생성
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)

            // 지상국 위치 설정
            val earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS
            val earthShape = OneAxisEllipsoid(
                earthRadius,
                Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getITRF(IERSConventions.IERS_2010, true)
            )
            val stationPosition = GeodeticPoint(Math.toRadians(latitude), Math.toRadians(longitude), altitude)
            val stationFrame = TopocentricFrame(earthShape, stationPosition, "GroundStation")

            // 시작 시간과 종료 시간을 AbsoluteDate로 변환
            val startDate = toAbsoluteDate(startTime)
            val endDate = toAbsoluteDate(endTime)

            // 시간 범위 설정
            var currentDate = startDate
            var currentTime = startTime
            var pointsCalculated = 0

            logger.info("가시성 계산 시작...")

            while (currentTime.isBefore(endTime)) {
                pointsCalculated++

                // 위성 위치 계산
                val pv = propagator.getPVCoordinates(currentDate, stationFrame)
                val position = pv.position

                // 방위각과 고도각 계산
                val azimuth = Math.toDegrees(Math.atan2(position.x, position.y))
                val elevation =
                    Math.toDegrees(Math.atan2(position.z, Math.sqrt(position.x * position.x + position.y * position.y)))

                // 현재 가시성 상태 확인
                wasVisible = isVisible
                isVisible = elevation >= minElevation

                // 가시성 상태 변화 감지
                transitionDetected = wasVisible != isVisible

                // 상태 변화가 감지되면 정밀 간격으로 전환
                if (transitionDetected) {
                    // 상태 변화가 감지되면 이전 시점으로 돌아가서 정밀 간격으로 다시 계산
                    if (currentTimeStep == coarseTimeStepMs) {
                        logger.debug("가시성 상태 변화 감지: ${if (isVisible) "보이기 시작" else "보이지 않기 시작"} - 정밀 계산으로 전환")

                        // 이전 시점으로 돌아가기 (최대 coarseTimeStepMs만큼)
                        val backtrackTime = currentTime.minus(coarseTimeStepMs.toLong(), ChronoUnit.MILLIS)
                        if (backtrackTime.isAfter(startTime)) {
                            currentTime = backtrackTime
                            currentDate = toAbsoluteDate(currentTime)
                            currentTimeStep = fineTimeStepMs

                            // 이전 상태 초기화 (다시 계산하기 위해)
                            isVisible = false
                            wasVisible = false
                            transitionDetected = false
                            continue
                        }
                    }
                }

                // 가시성 상태에 따른 처리
                if (isVisible) {
                    // 가시성 시작
                    if (!wasVisible) {
                        visibilityStart = currentTime
                        maxElevationInPass = elevation
                        maxElevationTime = currentTime

                        // 새로운 가시성 기간이 시작될 때 최대값 초기화
                        maxAzimuthRate = 0.0
                        maxElevationRate = 0.0
                        maxAzimuthAccel = 0.0
                        maxElevationAccel = 0.0

                        logger.debug("가시성 기간 시작: ${currentTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}, 고도각: ${elevation}°")

                        // 정밀 간격 유지 (시작 부근)
                        currentTimeStep = fineTimeStepMs
                    }

                    // 최대 고도각 업데이트
                    if (elevation > maxElevationInPass) {
                        maxElevationInPass = elevation
                        maxElevationTime = currentTime

                    }

                    // 가시성 중간 부분에서는 큰 간격으로 전환
                    if (wasVisible && maxElevationInPass - elevation > 5.0) {
                        // 최대 고도각을 지나 하강 중이면 다시 정밀 간격으로
                        currentTimeStep = fineTimeStepMs
                    } else if (wasVisible && currentTimeStep == fineTimeStepMs && elevation > minElevation + 5.0) {
                        // 충분히 고도각이 높아지면 큰 간격으로 전환
                        currentTimeStep = coarseTimeStepMs
                    }
                } else {
                    // 가시성 종료
                    if (wasVisible) {
                        logger.debug("가시성 기간 종료: ${currentTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
                        logger.debug(
                            "패스 정보: 시작=${visibilityStart?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}, 종료=${
                                currentTime.format(
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                )
                            }, 최대고도각=${maxElevationInPass}°"
                        )

                        if (visibilityStart != null) {
                            visibilityPeriods.add(
                                VisibilityPeriod(
                                    visibilityStart,
                                    currentTime,
                                    maxElevationInPass,
                                    maxElevationTime,
                                    maxAzimuthRate,
                                    maxElevationRate,
                                    maxAzimuthAccel,
                                    maxElevationAccel
                                )
                            )
                        }

                        visibilityStart = null
                        maxElevationInPass = -90.0
                        maxElevationTime = null

                        // 가시성 종료 후에는 큰 간격으로 전환
                        currentTimeStep = coarseTimeStepMs
                    }
                }

                // 속도 계산 (이전 값이 있는 경우)
                if (prevAzimuth != null && prevElevation != null && prevTime != null) {
                    val timeDiff = Duration.between(prevTime, currentTime).toMillis() / 1000.0 // 초 단위로 변환

                    // 방위각 변화 처리 (360도 경계 처리)
                    var azimuthDiff = azimuth - prevAzimuth
                    if (azimuthDiff > 180) azimuthDiff -= 360
                    if (azimuthDiff < -180) azimuthDiff += 360

                    val elevationDiff = elevation - prevElevation

                    // 각속도 계산 (도/초)
                    val azimuthRate = azimuthDiff / timeDiff
                    val elevationRate = elevationDiff / timeDiff

                    // 각가속도 계산 (이전 속도 값이 있는 경우)
                    if (prevAzimuthRate != null && prevElevationRate != null) {
                        val azimuthAccel = (azimuthRate - prevAzimuthRate) / timeDiff
                        val elevationAccel = (elevationRate - prevElevationRate) / timeDiff

                        // 현재 가시성 기간 내에서만 최대값 업데이트
                        if (visibilityStart != null && isVisible) {
                            maxAzimuthAccel = Math.max(maxAzimuthAccel, Math.abs(azimuthAccel))
                            maxElevationAccel = Math.max(maxElevationAccel, Math.abs(elevationAccel))
                        }
                    }

                    // 현재 가시성 기간 내에서만 최대 속도 업데이트
                    if (visibilityStart != null && isVisible) {
                        maxAzimuthRate = Math.max(maxAzimuthRate, Math.abs(azimuthRate))
                        maxElevationRate = Math.max(maxElevationRate, Math.abs(elevationRate))
                    }

                    // 이전 속도 값 저장
                    prevAzimuthRate = azimuthRate
                    prevElevationRate = elevationRate
                }

                // 이전 값 저장
                prevAzimuth = azimuth
                prevElevation = elevation
                prevTime = currentTime

                // 다음 시간으로 이동 (현재 시간 간격 사용)
                val timeStepSeconds = currentTimeStep / 1000.0
                currentDate = currentDate.shiftedBy(timeStepSeconds)
                currentTime = currentTime.plus(currentTimeStep.toLong(), ChronoUnit.MILLIS)
            }

            // 마지막 가시성 기간이 종료되지 않은 경우 처리
            if (visibilityStart != null && isVisible) {
                logger.debug(
                    "마지막 패스 정보: 시작=${visibilityStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}, 종료=${
                        currentTime.format(
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        )
                    }, 최대고도각=${maxElevationInPass}°"
                )

                visibilityPeriods.add(
                    VisibilityPeriod(
                        visibilityStart,
                        currentTime,
                        maxElevationInPass,
                        maxElevationTime,
                        maxAzimuthRate,
                        maxElevationRate,
                        maxAzimuthAccel,
                        maxElevationAccel
                    )
                )
            }

            // 계산된 모든 패스에 대한 요약 정보 로깅
            logger.info("가시성 계산 완료: 총 ${visibilityPeriods.size}개의 패스가 계산되었습니다. (계산된 포인트: $pointsCalculated)")

            // 날짜별 패스 수 계산
            val passesByDate = visibilityPeriods.groupBy { it.startTime.toLocalDate() }
            passesByDate.forEach { (date, passes) ->
                logger.info("${date} 날짜의 패스 수: ${passes.size}개")
            }

            return visibilityPeriods
        } catch (e: Exception) {
            logger.error("위성 가시성 기간 계산 중 오류 발생: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 위성 가시성 기간 정보를 담는 데이터 클래스
     */
    data class VisibilityPeriod(
        val startTime: ZonedDateTime,
        val endTime: ZonedDateTime,
        val maxElevation: Double,
        val maxElevationTime: ZonedDateTime? = null,
        val maxAzimuthRate: Double = 0.0,         // 최대 방위각 속도 (도/초)
        val maxElevationRate: Double = 0.0,       // 최대 고도각 속도 (도/초)
        val maxAzimuthAccel: Double = 0.0,        // 최대 방위각 가속도 (도/초²)
        val maxElevationAccel: Double = 0.0       // 최대 고도각 가속도 (도/초²)
    ) {
        val duration: Duration = Duration.between(startTime, endTime)

        // 가시성 기간의 지속 시간을 문자열로 반환
        fun getDurationString(): String {
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            val seconds = duration.toSecondsPart()
            val millis = duration.toMillisPart()
            return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        }

        // 가시성 기간의 요약 정보를 문자열로 반환
        override fun toString(): String {
            return "시작: ${startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}, " +
                    "종료: ${endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}, " +
                    "최대 고도각: ${String.format("%.2f", maxElevation)}°, " +
                    "최대 고도각 시간: ${maxElevationTime?.format(DateTimeFormatter.ISO_LOCAL_TIME)}, " +
                    "지속 시간: ${getDurationString()}, " +
                    "최대 방위각 속도: ${String.format("%.2f", maxAzimuthRate)}°/s, " +
                    "최대 고도각 속도: ${String.format("%.2f", maxElevationRate)}°/s, " +
                    "최대 방위각 가속도: ${String.format("%.2f", maxAzimuthAccel)}°/s², " +
                    "최대 고도각 가속도: ${String.format("%.2f", maxElevationAccel)}°/s²"
        }
    }

    /**
     * Orekit 데이터 파일 설정 - 리소스 폴더에서 로드
     */
    private fun setupOrekitData() {
        try {
            logger.info("Orekit 데이터 초기화 상태 확인...")

            // OrekitConfig에서 이미 초기화되었는지 확인
            if (!orekitStatus.isInitialized) {
                throw RuntimeException("Orekit 데이터가 초기화되지 않았습니다. OrekitConfig를 확인하세요.")
            }

            logger.info("Orekit 데이터 초기화 확인 완료. (프로바이더 수: ${orekitStatus.dataProvidersCount})")

        } catch (e: Exception) {
            logger.error("Orekit 데이터 확인 중 오류 발생: ${e.message}", e)
            throw e
        }
    }

    /**
     * 위성 추적 스케줄의 모든 데이터를 저장합니다.
     * - 각 패스별 세부 데이터 파일 (CSV)
     * - 전체 요약 정보 파일
     *
     * @param schedule 위성 추적 스케줄
     * @param outputDir 출력 디렉토리 경로
     * @param filePrefix 파일 이름 접두사 (기본값: "satellite_tracking")
     * @return 생성된 모든 파일 경로 목록
     */
    fun saveAllTrackingData(
        schedule: SatelliteTrackingSchedule,
        outputDir: String,
        filePrefix: String = "satellite_tracking"
    ): List<String> {
        val createdFiles = mutableListOf<String>()

        try {
            // 디렉토리 생성
            val directory = File(outputDir)
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // 1. 각 패스별 CSV 파일 생성
            val baseFilePath = "$outputDir/${filePrefix}_pass"
            val passFiles = saveAllPassesTrackingDataToFiles(schedule, baseFilePath)
            createdFiles.addAll(passFiles)

            // 2. 요약 정보 파일 생성
            val summaryFilePath = "$outputDir/${filePrefix}_summary.txt"
            val summaryFile = saveTrackingScheduleSummary(schedule, summaryFilePath)
            createdFiles.add(summaryFile)

            // 3. 위성 정보 파일 생성 (선택 사항)
            val satelliteInfoFilePath = "$outputDir/${filePrefix}_info.txt"
            File(satelliteInfoFilePath).bufferedWriter().use { writer ->
                writer.write("위성 정보\n")
                writer.write("========\n\n")
                writer.write("TLE 데이터:\n")
                writer.write("${schedule.satelliteTle1}\n")
                writer.write("${schedule.satelliteTle2}\n\n")

                // TLE에서 위성 ID 추출
                val satelliteId = schedule.satelliteTle1.substring(2, 7).trim()
                writer.write("위성 ID: $satelliteId\n")

                // 국제 지정 번호 추출
                val internationalDesignator = schedule.satelliteTle1.substring(9, 17).trim()
                writer.write("국제 지정 번호: $internationalDesignator\n")

                // 궤도 정보 (TLE에서 추출)
                writer.write("\n궤도 정보:\n")

                // TLE 두 번째 줄에서 궤도 정보 추출
                val inclination = schedule.satelliteTle2.substring(8, 16).trim().toDouble()
                val rightAscension = schedule.satelliteTle2.substring(17, 25).trim().toDouble()
                val eccentricity = "0.${schedule.satelliteTle2.substring(26, 33).trim()}".toDouble()
                val argOfPerigee = schedule.satelliteTle2.substring(34, 42).trim().toDouble()
                val meanAnomaly = schedule.satelliteTle2.substring(43, 51).trim().toDouble()
                val meanMotion = schedule.satelliteTle2.substring(52, 63).trim().toDouble()

                writer.write("- 궤도 경사각: $inclination°\n")
                writer.write("- 승교점 적경: $rightAscension°\n")
                writer.write("- 이심률: $eccentricity\n")
                writer.write("- 근지점 인수: $argOfPerigee°\n")
                writer.write("- 평균 근점 이각: $meanAnomaly°\n")
                writer.write("- 평균 운동: $meanMotion 회/일\n")

                // 궤도 주기 계산
                val periodMinutes = 1440.0 / meanMotion
                val periodHours = periodMinutes / 60.0
                writer.write(
                    "- 궤도 주기: ${String.format("%.2f", periodMinutes)} 분 (${
                        String.format(
                            "%.2f",
                            periodHours
                        )
                    } 시간)\n"
                )

                // 근지점 및 원지점 고도 계산 (대략적인 계산)
                val earthRadius = 6378.137 // 지구 적도 반경 (km)
                val semiMajorAxis = (earthRadius + 42164.0) * Math.pow(24.0 / periodHours, 2.0 / 3.0) // 정지궤도 고도 기준 계산

                val perigeeRadius = semiMajorAxis * (1.0 - eccentricity)
                val apogeeRadius = semiMajorAxis * (1.0 + eccentricity)

                val perigeeAltitude = perigeeRadius - earthRadius
                val apogeeAltitude = apogeeRadius - earthRadius

                writer.write("- 근지점 고도: ${String.format("%.2f", perigeeAltitude)} km\n")
                writer.write("- 원지점 고도: ${String.format("%.2f", apogeeAltitude)} km\n")
            }
            createdFiles.add(satelliteInfoFilePath)

            logger.info("모든 위성 추적 데이터가 ${outputDir} 디렉토리에 저장되었습니다.")
            logger.info("총 ${createdFiles.size}개의 파일이 생성되었습니다.")
        } catch (e: Exception) {
            logger.error("위성 추적 데이터 저장 중 오류 발생: ${e.message}", e)
            throw e
        }

        return createdFiles
    }

    /**
     * 모든 패스의 세부 추적 데이터를 각각 별도의 CSV 파일로 저장합니다.
     *
     * @param schedule 위성 추적 스케줄
     * @param baseFilePath 기본 파일 경로 (예: "tracking_data/pass")
     * @param fileExtension 파일 확장자 (기본값: ".csv")
     * @return 생성된 파일 경로 목록
     */
    fun saveAllPassesTrackingDataToFiles(
        schedule: SatelliteTrackingSchedule,
        baseFilePath: String,
        fileExtension: String = ".csv"
    ): List<String> {
        val createdFiles = mutableListOf<String>()

        try {
            // 디렉토리 생성
            val directory = File(baseFilePath).parentFile
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // 각 패스에 대해 파일 생성
            schedule.trackingPasses.forEachIndexed { index, pass ->
                val passNumber = index + 1
                val filePath = "${baseFilePath}_${passNumber}${fileExtension}"
                val file = File(filePath)

                file.bufferedWriter().use { writer ->
                    // 파일 헤더 - CSV 형식
                    writer.write("시간,방위각(°),고도각(°),거리(km),고도(km)\n")

                    // 데이터 행
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    pass.trackingData.forEach { data ->
                        writer.write("${data.timestamp?.format(formatter)},${data.azimuth},${data.elevation},${data.range},${data.altitude}\n")
                    }
                }

                createdFiles.add(filePath)
                logger.info("패스 ${passNumber} 데이터가 ${filePath}에 저장되었습니다.")
            }

            logger.info("총 ${createdFiles.size}개의 패스 데이터 파일이 생성되었습니다.")
        } catch (e: Exception) {
            logger.error("패스 데이터 파일 생성 중 오류 발생: ${e.message}", e)
            throw e
        }

        return createdFiles
    }

    /**
     * 모든 패스의 요약 정보를 하나의 파일로 저장합니다.
     *
     * @param schedule 위성 추적 스케줄
     * @param filePath 파일 경로
     * @return 생성된 파일 경로
     */
    /**
     * 모든 패스의 요약 정보를 하나의 파일로 저장합니다.
     */
    fun saveTrackingScheduleSummary(
        schedule: SatelliteTrackingSchedule,
        filePath: String
    ): String {
        try {
            val file = File(filePath)

            // 디렉토리 생성
            file.parentFile?.mkdirs()

            file.bufferedWriter().use { writer ->
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

                // 스케줄 요약 정보
                writer.write("위성 추적 스케줄 요약\n")
                writer.write("===================\n\n")
                writer.write("위성 TLE:\n")
                writer.write("${schedule.satelliteTle1}\n")
                writer.write("${schedule.satelliteTle2}\n\n")
                writer.write(
                    "기간: ${schedule.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} ~ ${
                        schedule.endDate.format(
                            DateTimeFormatter.ISO_LOCAL_DATE
                        )
                    }\n"
                )
                writer.write("지상국 위치: 위도 ${schedule.stationLatitude}°, 경도 ${schedule.stationLongitude}°, 고도 ${schedule.stationAltitude}m\n")
                writer.write("최소 고도각: ${schedule.minElevation}°\n")
                writer.write("추적 간격: ${schedule.trackingIntervalMs}ms\n")
                writer.write("총 패스 수: ${schedule.totalPasses}\n")
                writer.write("총 추적 시간: ${schedule.getTotalTrackingDurationString()}\n\n")

                // 패스 목록 테이블 헤더 (각속도 및 각가속도 정보 추가)
                writer.write("패스 목록:\n")
                writer.write("─────┬────────────────────┬────────────────────┬──────────┬─────────────┬───────────┬───────────┬───────────┬───────────┐\n")
                writer.write("│ 번호│      시작 시간     │      종료 시간     │ 지속시간 │ 최대고도각  │ 최대Az속도│ 최대El속도│ 최대Az가속│ 최대El가속│\n")
                writer.write("├─────┼────────────────────┼────────────────────┼──────────┼─────────────┼───────────┼───────────┼───────────┼───────────┤\n")

                // 각 패스 정보 (각속도 및 각가속도 정보 추가)
                schedule.trackingPasses.forEachIndexed { index, pass ->
                    val passNumber = index + 1
                    val maxElevation = String.format("%.2f°", pass.maxElevation)
                    val maxAzRateStr = String.format("%.2f°/s", pass.maxAzimuthRate)
                    val maxElRateStr = String.format("%.2f°/s", pass.maxElevationRate)
                    val maxAzAccelStr = String.format("%.2f°/s²", pass.maxAzimuthAccel)
                    val maxElAccelStr = String.format("%.2f°/s²", pass.maxElevationAccel)

                    writer.write(
                        String.format(
                            "│ %3d │ %s │ %s │ %s │ %-11s │ %-9s │ %-9s │ %-9s │ %-9s │\n",
                            passNumber,
                            pass.startTime.format(formatter),
                            pass.endTime.format(formatter),
                            pass.getDurationString(),
                            maxElevation,
                            maxAzRateStr,
                            maxElRateStr,
                            maxAzAccelStr,
                            maxElAccelStr
                        )
                    )
                }

                writer.write("└─────┴────────────────────┴────────────────────┴──────────┴─────────────┴───────────┴───────────┴───────────┴───────────┘\n")

                // 전체 패스 중 최대값 출력
                if (schedule.trackingPasses.isNotEmpty()) {
                    val overallMaxAzRate = schedule.trackingPasses.maxOf { it.maxAzimuthRate }
                    val overallMaxElRate = schedule.trackingPasses.maxOf { it.maxElevationRate }
                    val overallMaxAzAccel = schedule.trackingPasses.maxOf { it.maxAzimuthAccel }
                    val overallMaxElAccel = schedule.trackingPasses.maxOf { it.maxElevationAccel }

                    writer.write("\n전체 패스 중 최대값:\n")
                    writer.write("- 최대 방위각 각속도: ${String.format("%.2f", overallMaxAzRate)}°/s\n")
                    writer.write("- 최대 고도각 각속도: ${String.format("%.2f", overallMaxElRate)}°/s\n")
                    writer.write("- 최대 방위각 각가속도: ${String.format("%.2f", overallMaxAzAccel)}°/s²\n")
                    writer.write("- 최대 고도각 각가속도: ${String.format("%.2f", overallMaxElAccel)}°/s²\n")
                }

                // 각 패스별 세부 정보
                writer.write("\n\n패스별 세부 정보:\n")
                writer.write("=================\n\n")

                schedule.trackingPasses.forEachIndexed { index, pass ->
                    val passNumber = index + 1

                    writer.write("패스 ${passNumber} 정보:\n")
                    writer.write("- 시작 시간: ${pass.startTime.format(formatter)}\n")
                    writer.write("- 종료 시간: ${pass.endTime.format(formatter)}\n")
                    writer.write(
                        "- 최대 고도각: ${
                            String.format(
                                "%.2f",
                                pass.maxElevation
                            )
                        }° (${pass.maxElevationTime?.format(DateTimeFormatter.ISO_LOCAL_TIME) ?: "N/A"})\n"
                    )
                    writer.write("- 지속 시간: ${pass.getDurationString()}\n")
                    writer.write("- 데이터 포인트 수: ${pass.dataPointCount}\n")

                    // 각속도 및 각가속도 정보 추가
                    writer.write("- 최대 방위각 각속도: ${String.format("%.2f", pass.maxAzimuthRate)}°/s\n")
                    writer.write("- 최대 고도각 각속도: ${String.format("%.2f", pass.maxElevationRate)}°/s\n")
                    writer.write("- 최대 방위각 각가속도: ${String.format("%.2f", pass.maxAzimuthAccel)}°/s²\n")
                    writer.write("- 최대 고도각 각가속도: ${String.format("%.2f", pass.maxElevationAccel)}°/s²\n")

                    // 첫 데이터 포인트와 마지막 데이터 포인트 정보
                    if (pass.trackingData.isNotEmpty()) {
                        val firstPoint = pass.trackingData.first()
                        val lastPoint = pass.trackingData.last()

                        writer.write(
                            "- 첫 데이터 포인트: 방위각=${
                                String.format(
                                    "%.2f",
                                    firstPoint.azimuth
                                )
                            }°, 고도각=${String.format("%.2f", firstPoint.elevation)}°\n"
                        )
                        writer.write(
                            "- 마지막 데이터 포인트: 방위각=${
                                String.format(
                                    "%.2f",
                                    lastPoint.azimuth
                                )
                            }°, 고도각=${String.format("%.2f", lastPoint.elevation)}°\n"
                        )
                    }

                    writer.write("\n")
                }
            }

            logger.info("위성 추적 스케줄 요약이 ${filePath}에 저장되었습니다.")
            return filePath
        } catch (e: Exception) {
            logger.error("위성 추적 스케줄 요약 저장 중 오류 발생: ${e.message}", e)
            throw e
        }
    }
}

