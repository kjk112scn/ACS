package com.gtlsystems.acs_api.algorithm.satellitetracker.impl

import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import com.gtlsystems.acs_api.config.OrekitConfig
import org.orekit.time.AbsoluteDate
import org.hipparchus.util.FastMath
import org.orekit.bodies.GeodeticPoint
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.frames.Frame
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.propagation.events.ElevationDetector
import org.orekit.propagation.events.EventDetector
import org.orekit.propagation.events.handlers.EventHandler
import org.orekit.propagation.SpacecraftState
import org.hipparchus.ode.events.Action
import org.orekit.time.TimeScale
import org.orekit.time.TimeScalesFactory
import org.orekit.utils.Constants
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Orekit 라이브러리를 사용하여 위성 위치를 계산하는 클래스
 * 
 * ✅ Orekit 13.0.2 공식 문서 기반 ElevationDetector 구현
 * @see https://www.orekit.org/static/apidocs/org/orekit/propagation/events/ElevationDetector.html
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

    init {
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
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)

            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")

            val date = AbsoluteDate(
                dateTime.year, dateTime.monthValue, dateTime.dayOfMonth,
                dateTime.hour, dateTime.minute, dateTime.second + dateTime.nano / 1e9,
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
            val azimuth = FastMath.toDegrees(FastMath.atan2(x, y))
            val normalizedAzimuth = if (azimuth < 0) azimuth + 360.0 else azimuth

            val satellitePosition = state.getPVCoordinates(earthFrame).position
            val satelliteRadius = satellitePosition.norm
            val satelliteAltitude = (satelliteRadius - Constants.WGS84_EARTH_EQUATORIAL_RADIUS) / 1000.0

            return SatelliteTrackData(
                azimuth = normalizedAzimuth,
                elevation = elevation,
                timestamp = dateTime,
                range = distance / 1000.0,
                altitude = satelliteAltitude
            )

        } catch (e: Exception) {
            logger.error("위성 위치 계산 중 오류 발생: ${e.message}", e)
            throw e
        }
    }

    /**
     * ElevationDetector를 사용한 가시성 기간 감지
     * 
     * ✅ 이벤트 기반: 시간 간격 설정 불필요
     * ✅ 자동 정밀도: 내부 이진 탐색으로 정확한 시점 탐지
     * ✅ 성능 최적화: 필요한 시점만 계산
     * 
     * @see https://www.orekit.org/static/apidocs/org/orekit/propagation/events/ElevationDetector.html
     */
    private fun detectVisibilityPeriods(
        tleLine1: String,
        tleLine2: String,
        startDate: ZonedDateTime,
        durationDays: Int,
        minElevation: Float,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): List<VisibilityPeriod> {
        logger.info("🔍 ElevationDetector로 가시성 기간 감지 시작")
        logger.info("📊 파라미터: minElevation=${minElevation}°, 위치=(${latitude}, ${longitude}), 기간=${durationDays}일")
        
        val visibilityPeriods = mutableListOf<VisibilityPeriod>()
        
        try {
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)
            
            logger.info("🛰️ TLE: ${tle.satelliteNumber}, Epoch: ${tle.date}")
            
            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")
            
            // ✅ 시작 시점의 elevation 확인 (별도 propagator 사용 - 기존 propagator와 독립)
            val startAbsoluteDate = toAbsoluteDate(startDate)
            val endAbsoluteDate = toAbsoluteDate(startDate.plusDays(durationDays.toLong()))
            
            val checkPropagator = TLEPropagator.selectExtrapolator(tle)
            val initialState = checkPropagator.propagate(startAbsoluteDate)
            val initialPvInStation = initialState.getPVCoordinates(stationFrame)
            val initialPosInStation = initialPvInStation.position
            val initialElevation = FastMath.toDegrees(
                FastMath.asin(initialPosInStation.z / initialPosInStation.norm)
            )
            logger.info("🔎 시작 시점 elevation: ${String.format("%.6f", initialElevation)}° (minElevation: ${minElevation}°)")
            
            // ✅ 이벤트를 직접 수집하는 리스트
            val eventList = mutableListOf<Pair<ZonedDateTime, Boolean>>()  // (시간, isIncreasing)
            
            // ✅ ElevationDetector 설정 - 커스텀 EventHandler 사용
            // Orekit 13.0.2에서는 EventHandler가 제네릭이 아닙니다
            val customHandler = object : EventHandler {
                override fun eventOccurred(
                    s: SpacecraftState,
                    detector: org.orekit.propagation.events.EventDetector,
                    increasing: Boolean
                ): Action {
                    val eventTime = toZonedDateTime(s.date)
                    
                    // elevation 값 계산 (로깅용)
                    val pvInStation = s.getPVCoordinates(stationFrame)
                    val posInStation = pvInStation.position
                    val elevation = FastMath.toDegrees(
                        FastMath.asin(posInStation.z / posInStation.norm)
                    )
                    
                    val eventType = if (increasing) "AOS" else "LOS"
                    logger.info("📡 가시성 ${if (increasing) "시작" else "종료"} ($eventType): $eventTime (고도각: ${String.format("%.6f", elevation)}°)")
                    
                    eventList.add(Pair(eventTime, increasing))
                    
                    return Action.CONTINUE
                }
            }
            
            val elevationDetector = ElevationDetector(stationFrame)
                .withConstantElevation(FastMath.toRadians(minElevation.toDouble()))
                .withMaxCheck(60.0)      // 최대 체크 간격 10분 (Orekit이 자동 최적화)
                .withThreshold(1.0e-3)    // 이벤트 시점 정밀도 1ms (충분함)
                .withHandler(customHandler)
            
            // ✅ Detector 등록
            propagator.addEventDetector(elevationDetector)
            
            // ✅ 시간 범위 propagate - 자동으로 이벤트 감지
            logger.info("🔄 Propagation 시작: ${startDate} ~ ${toZonedDateTime(endAbsoluteDate)}")
            try {
                propagator.propagate(startAbsoluteDate, endAbsoluteDate)
                logger.info("✅ Propagation 완료: ${eventList.size}개 이벤트 감지됨")
            } catch (e: Exception) {
                logger.warn("⚠️ Propagation 중 예외 발생 (정상일 수 있음): ${e.message}")
            }
            
            // ✅ 이벤트를 가시성 기간으로 변환
            var currentStart: ZonedDateTime? = null
            
            // 시작 시점이 이미 가시성 범위 내라면 시작 시점을 AOS로 설정
            if (initialElevation >= minElevation) {
                logger.info("⚠️ 시작 시점이 이미 가시성 범위 내 (elevation: ${String.format("%.6f", initialElevation)}°)")
                logger.info("   → 첫 LOS 이벤트까지 가시성 기간 시작으로 설정")
                currentStart = startDate
            }
            
            for ((eventTime, isIncreasing) in eventList) {
                if (isIncreasing) {
                    // AOS: 위성 상승 (가시성 시작)
                    currentStart = eventTime
                } else {
                    // LOS: 위성 하강 (가시성 종료)ㅋ`
                    if (currentStart != null) {
                        visibilityPeriods.add(VisibilityPeriod(startTime = currentStart, endTime = eventTime))
                        logger.info("✅ 가시성 기간 추가: $currentStart ~ $eventTime")
                        currentStart = null
                    } else {
                        logger.warn("⚠️ LOS 이벤트인데 AOS가 없음! (시작 시점 가시성 범위 밖)")
                    }
                }
            }
            
            // ✅ 마지막 가시성 기간 처리 (종료 시점에 가시성 유지 중인 경우)
            if (currentStart != null) {
                val endTime = toZonedDateTime(endAbsoluteDate)
                visibilityPeriods.add(VisibilityPeriod(startTime = currentStart, endTime = endTime))
                logger.debug("📡 마지막 가시성 기간 종료 시점을 스케줄 종료로 설정")
            }
            
            logger.info("✅ ${visibilityPeriods.size}개 가시성 기간 감지 완료")
            
            // ✅ 각 기간의 정보 로깅 (검증용)
            visibilityPeriods.forEachIndexed { index, period ->
                logger.debug("  패스 ${index + 1}: ${period.startTime} ~ ${period.endTime}")
            }
            
            return visibilityPeriods
            
        } catch (e: Exception) {
            logger.error("❌ 가시성 기간 감지 실패: ${e.message}", e)
            throw RuntimeException("가시성 기간 감지 실패", e)
        }
    }
    
    /**
     * 🔴 백업: 이전 5분 간격 체크 방식
     * 
     * 문제 발생 시 이 함수로 롤백 가능
     * 
    private fun detectVisibilityPeriodsOld(
        tleLine1: String,
        tleLine2: String,
        startDate: ZonedDateTime,
        durationDays: Int,
        minElevation: Float,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): List<VisibilityPeriod> {
        logger.info("🔍 간단한 가시성 기간 감지 시작")
        logger.debug("파라미터: minElevation=${minElevation}°, 위치=(${latitude}, ${longitude}), 기간=${durationDays}일")
        
        val visibilityPeriods = mutableListOf<VisibilityPeriod>()
        
        try {
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)
            
            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")
            
            // 간단한 시간 간격으로 가시성 확인
            val startAbsoluteDate = toAbsoluteDate(startDate)
            val endAbsoluteDate = toAbsoluteDate(startDate.plusDays(durationDays.toLong()))
            
            var currentDate = startAbsoluteDate
            val stepSize = 300.0 // 5분 간격
            var isVisible = false
            var visibilityStart: ZonedDateTime? = null
            
            while (currentDate.compareTo(endAbsoluteDate) < 0) {
                val state = propagator.propagate(currentDate)
                val pvInStation = state.getPVCoordinates(stationFrame)
                val posInStation = pvInStation.position
                
                val elevation = FastMath.toDegrees(FastMath.asin(posInStation.z / posInStation.norm))
                val currentTime = toZonedDateTime(currentDate)
                
                if (elevation >= minElevation && !isVisible) {
                    // 가시성 시작
                    isVisible = true
                    visibilityStart = currentTime
                    logger.debug("📡 가시성 시작: $currentTime (고도각: ${elevation}°)")
                } else if (elevation < minElevation && isVisible) {
                    // 가시성 종료
                    isVisible = false
                    if (visibilityStart != null) {
                        visibilityPeriods.add(VisibilityPeriod(startTime = visibilityStart, endTime = currentTime))
                        logger.debug("📡 가시성 종료: $currentTime (고도각: ${elevation}°)")
                    }
                }
                
                currentDate = currentDate.shiftedBy(stepSize)
            }
            
            // 마지막 가시성 기간 처리
            if (isVisible && visibilityStart != null) {
                val endTime = toZonedDateTime(endAbsoluteDate)
                visibilityPeriods.add(VisibilityPeriod(startTime = visibilityStart, endTime = endTime))
            }
            
            logger.info("✅ ${visibilityPeriods.size}개 가시성 기간 감지 완료")
            
            return visibilityPeriods
            
        } catch (e: Exception) {
            logger.error("❌ 가시성 기간 감지 실패: ${e.message}", e)
            throw RuntimeException("가시성 기간 감지 실패", e)
        }
    }
    */

    /**
     * Orekit AbsoluteDate를 ZonedDateTime으로 변환
     * 
     * @param absoluteDate Orekit의 AbsoluteDate
     * @return ZonedDateTime (UTC 기준)
     */
    private fun toZonedDateTime(absoluteDate: AbsoluteDate): ZonedDateTime {
        val components = absoluteDate.getComponents(utcTimeScale)
        val date = components.date
        val time = components.time
        
        return ZonedDateTime.of(
            date.year,
            date.month,
            date.day,
            time.hour,
            time.minute,
            time.second.toInt(),
            0, // 나노초는 0으로 설정 (간단한 해결)
            ZoneOffset.UTC
        )
    }

    /**
     * 지정된 기간 동안 위성 추적 스케줄을 생성합니다.
     * 
     * ✅ 계획: ElevationDetector 사용, 순수 2축 데이터만 생성
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
        logger.info("🚀 위성 추적 스케줄 생성 시작 (ElevationDetector 사용)")
        logger.info("위성: ${tleLine1.substring(2, 7).trim()}, 기간: ${durationDays}일, 최소고도: ${minElevation}°")
        
        val endDate = startDate.plusDays(durationDays.toLong())
        logger.info("스케줄 기간: ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} ~ ${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")

        try {
            val visibilityPeriods = detectVisibilityPeriods(
                tleLine1, tleLine2, startDate, durationDays,
                minElevation, latitude, longitude, altitude
            )
            
            if (visibilityPeriods.isEmpty()) {
                logger.warn("⚠️ 가시성 기간이 없습니다.")
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
                    trackingPasses = emptyList()
                )
            }
            
            logger.info("✅ ${visibilityPeriods.size}개 가시성 기간 감지 완료")

            val trackingPasses = visibilityPeriods.mapIndexed { index, period ->
                logger.debug("패스 ${index + 1}/${visibilityPeriods.size} 상세 데이터 생성: ${period.startTime} ~ ${period.endTime}")
                
                val detailedData = generateDetailedTrackingData(
                    tleLine1 = tleLine1,
                    tleLine2 = tleLine2,
                    startTime = period.startTime!!,
                    endTime = period.endTime!!,
                    intervalMs = trackingIntervalMs,
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    minElevation = minElevation
                )
                
                logger.debug("패스 ${index + 1} 생성 완료: ${detailedData.size}개 데이터 포인트")
                
                SatelliteTrackingPass(
                    startTime = period.startTime,
                    endTime = period.endTime,
                    trackingData = detailedData
                )
            }
            
            logger.info("✅ ${trackingPasses.size}개 패스 생성 완료, 총 ${trackingPasses.sumOf { it.trackingData.size }}개 데이터 포인트")

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
            logger.error("❌ 위성 추적 스케줄 생성 실패: ${e.message}", e)
            throw RuntimeException("위성 추적 스케줄 생성 실패", e)
        }
    }

    /**
     * 지정된 시간 범위 내에서 상세 추적 데이터를 생성합니다.
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

        logger.info("상세 추적 데이터 생성 시작")

        try {
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)

            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")

            var currentTime = startTime
            var pointsCalculated = 0
            var pointsAdded = 0

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

                val x = posInStation.x
                val y = posInStation.y
                val z = posInStation.z
                val distance = posInStation.norm

                val elevation = FastMath.toDegrees(FastMath.asin(z / distance))

                if (elevation >= minElevation) {
                    val azimuth = FastMath.toDegrees(FastMath.atan2(x, y))
                    val normalizedAzimuth = if (azimuth < 0) azimuth + 360.0 else azimuth

                    val satellitePosition = state.getPVCoordinates(earthFrame).position
                    val satelliteRadius = satellitePosition.norm
                    val satelliteAltitude = (satelliteRadius - Constants.WGS84_EARTH_EQUATORIAL_RADIUS) / 1000.0

                    trackingData.add(
                        SatelliteTrackData(
                            azimuth = normalizedAzimuth,
                            elevation = elevation,
                            timestamp = currentTime,
                            range = distance / 1000.0,
                            altitude = satelliteAltitude
                        )
                    )
                    pointsAdded++
                }

                currentTime = currentTime.plus(intervalMs.toLong(), ChronoUnit.MILLIS)
            }

            logger.info("상세 추적 데이터 생성 완료: ${pointsAdded}개 포인트")

            return trackingData
        } catch (e: Exception) {
            logger.error("상세 추적 데이터 생성 중 오류 발생: ${e.message}", e)
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
        val totalPasses: Int = trackingPasses.size
        val totalTrackingDuration: Duration = trackingPasses
            .map { it.duration }
            .fold(Duration.ZERO) { acc, duration -> acc.plus(duration) }

        fun getTotalTrackingDurationString(): String {
            val hours = totalTrackingDuration.toHours()
            val minutes = totalTrackingDuration.toMinutesPart()
            val seconds = totalTrackingDuration.toSecondsPart()
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

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
     * 위성 추적 패스 (순수 2축 데이터만 포함)
     * 
     * ✅ 계획: 메타데이터 제거, Processor에서 계산
     */
    data class SatelliteTrackingPass(
        val startTime: ZonedDateTime,
        val endTime: ZonedDateTime,
        val trackingData: List<SatelliteTrackData>
    ) {
        val duration: Duration = Duration.between(startTime, endTime)
        val dataPointCount: Int = trackingData.size

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
                    "- 지속 시간: ${getDurationString()}\n" +
                    "- 데이터 포인트: $dataPointCount"
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
     * 가시성 기간 (ElevationDetector용)
     * 
     * 순수 2축 계산: 메타데이터 없이 시작/종료 시간만 포함
     * 모든 메타데이터는 Processor에서 계산
     */
    private data class VisibilityPeriod(
        val startTime: ZonedDateTime?,
        val endTime: ZonedDateTime?
    )
}