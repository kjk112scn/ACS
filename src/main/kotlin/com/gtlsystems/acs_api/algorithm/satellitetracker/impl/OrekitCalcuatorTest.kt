package com.gtlsystems.acs_api.algorithm.satellitetracker.impl

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator.SatelliteTrackingPass
import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator.SatelliteTrackingSchedule
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import org.hipparchus.util.FastMath
import org.orekit.bodies.GeodeticPoint
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.time.AbsoluteDate
import org.orekit.utils.Constants
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.gtlsystems.acs_api.service.system.settings.SettingsService

class OrekitCalcuatorTest {
/*

    private val settingsService: SettingsService

    init {
        // SettingsService 초기화
        // 실제 애플리케이션에서는 의존성 주입 또는 전역 객체로 관리
        // 여기서는 임시로 생성
        settingsService = SettingsService()
    }
*/

    /**
     * 한 번에 가시성 기간 찾기 + 상세 추적 데이터 생성
     */
    /*
    fun generateSatelliteTrackingScheduleOptimized(
        tleLine1: String,
        tleLine2: String,
        startDate: ZonedDateTime,
        durationDays: Int = 1,
        minElevation: Float = 0.0f,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        trackingIntervalMs: Int = settingsService.systemTrackingInterval.toInt()
    ): SatelliteTrackingSchedule {

        logger.info("최적화된 위성 추적 스케줄 생성 시작: ${startDate}, 기간: ${durationDays}일")
        logger.info("간격: ${trackingIntervalMs}ms, 최소 고도각: ${minElevation}°")

        val endDate = startDate.plusDays(durationDays.toLong())
        val trackingPasses = mutableListOf<SatelliteTrackingPass>()

        try {
            // TLE 및 기본 설정
            val tle = TLE(tleLine1, tleLine2)
            val propagator = TLEPropagator.selectExtrapolator(tle)

            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")

            // 현재 패스 추적 변수들
            var currentPassData = mutableListOf<SatelliteTrackData>()
            var passStartTime: ZonedDateTime? = null
            var maxElevationInPass = -90.0
            var maxElevationTime: ZonedDateTime? = null
            var maxAzimuthRate = 0.0
            var maxElevationRate = 0.0
            var maxAzimuthAccel = 0.0
            var maxElevationAccel = 0.0

            // 이전 값들 (속도/가속도 계산용)
            var prevAzimuth: Double? = null
            var prevElevation: Double? = null
            var prevAzimuthRate: Double? = null
            var prevElevationRate: Double? = null
            var prevTime: ZonedDateTime? = null

            var isVisible = false
            var currentTime = startDate
            var pointsCalculated = 0

            logger.info("통합 계산 시작...")

            while (!currentTime.isAfter(endDate)) {
                pointsCalculated++

                // 위성 위치 계산
                val date = AbsoluteDate(
                    currentTime.year, currentTime.monthValue, currentTime.dayOfMonth,
                    currentTime.hour, currentTime.minute,
                    currentTime.second + currentTime.nano / 1e9,
                    utcTimeScale
                )

                val state = propagator.propagate(date)
                val pvInStation = state.getPVCoordinates(stationFrame)
                val pos = pvInStation.position

                val x = pos.x
                val y = pos.y
                val z = pos.z
                val distance = pos.norm

                // 방위각과 고도각 계산
                val elevation = FastMath.toDegrees(FastMath.asin(z / distance))
                var azimuth = FastMath.toDegrees(FastMath.atan2(x, y))
                if (azimuth < 0) azimuth += 360.0

                // 위성 고도 계산
                val satellitePosition = state.getPVCoordinates(earthFrame).position
                val satelliteRadius = satellitePosition.norm
                val altitudeConversionFactor = settingsService.systemUdpMaxBufferSize
                val satelliteAltitude = (satelliteRadius - Constants.WGS84_EARTH_EQUATORIAL_RADIUS) / altitudeConversionFactor.toDouble()

                val wasVisible = isVisible
                isVisible = elevation >= minElevation

                // 가시성 시작
                if (isVisible && !wasVisible) {
                    passStartTime = currentTime
                    currentPassData.clear()
                    maxElevationInPass = elevation
                    maxElevationTime = currentTime
                    maxAzimuthRate = 0.0
                    maxElevationRate = 0.0
                    maxAzimuthAccel = 0.0
                    maxElevationAccel = 0.0

                    logger.debug("패스 시작: ${currentTime.format(DateTimeFormatter.ISO_LOCAL_TIME)}, 고도각: ${String.format("%.2f", elevation)}°")
                }

                // 가시성 중인 경우 데이터 추가
                if (isVisible) {
                    // 최대 고도각 업데이트
                    if (elevation > maxElevationInPass) {
                        maxElevationInPass = elevation
                        maxElevationTime = currentTime
                    }

                    // 추적 데이터 추가
                    val rangeConversionFactor = settingsService.systemUdpMaxBufferSize
                    currentPassData.add(
                        SatelliteTrackData(
                            azimuth = azimuth,
                            elevation = elevation,
                            timestamp = currentTime,
                            range = distance / rangeConversionFactor.toDouble(),
                            altitude = satelliteAltitude
                        )
                    )

                    // 속도 및 가속도 계산
                    if (prevAzimuth != null && prevElevation != null && prevTime != null) {
                        val timeDiff = Duration.between(prevTime, currentTime).toMillis() / 1000.0

                        // 방위각 변화 (360도 경계 처리)
                        var azimuthDiff = azimuth - prevAzimuth
                        if (azimuthDiff > 180) azimuthDiff -= 360
                        if (azimuthDiff < -180) azimuthDiff += 360

                        val elevationDiff = elevation - prevElevation

                        // 각속도 계산
                        val azimuthRate = azimuthDiff / timeDiff
                        val elevationRate = elevationDiff / timeDiff

                        maxAzimuthRate = Math.max(maxAzimuthRate, Math.abs(azimuthRate))
                        maxElevationRate = Math.max(maxElevationRate, Math.abs(elevationRate))

                        // 각가속도 계산
                        if (prevAzimuthRate != null && prevElevationRate != null) {
                            val azimuthAccel = (azimuthRate - prevAzimuthRate) / timeDiff
                            val elevationAccel = (elevationRate - prevElevationRate) / timeDiff

                            maxAzimuthAccel = Math.max(maxAzimuthAccel, Math.abs(azimuthAccel))
                            maxElevationAccel = Math.max(maxElevationAccel, Math.abs(elevationAccel))
                        }

                        prevAzimuthRate = azimuthRate
                        prevElevationRate = elevationRate
                    }
                }

                // 가시성 종료
                if (!isVisible && wasVisible && passStartTime != null) {
                    logger.debug("패스 종료: ${currentTime.format(DateTimeFormatter.ISO_LOCAL_TIME)}, 최대고도각: ${String.format("%.2f", maxElevationInPass)}°")

                    // 시작/종료 각도 계산
                    val startAzimuth = currentPassData.firstOrNull()?.azimuth ?: 0.0
                    val startElevation = currentPassData.firstOrNull()?.elevation ?: 0.0
                    val endAzimuth = currentPassData.lastOrNull()?.azimuth ?: 0.0
                    val endElevation = currentPassData.lastOrNull()?.elevation ?: 0.0

                    // 패스 객체 생성
                    val pass = SatelliteTrackingPass(
                        startTime = passStartTime,
                        endTime = currentTime,
                        maxElevation = maxElevationInPass,
                        maxElevationTime = maxElevationTime,
                        duration = Duration.between(passStartTime, currentTime),
                        trackingData = currentPassData.toList(), // 복사본 생성
                        maxAzimuthRate = maxAzimuthRate,
                        maxElevationRate = maxElevationRate,
                        maxAzimuthAccel = maxAzimuthAccel,
                        maxElevationAccel = maxElevationAccel,
                        startAzimuth = startAzimuth,
                        startElevation = startElevation,
                        endAzimuth = endAzimuth,
                        endElevation = endElevation
                    )

                    trackingPasses.add(pass)
                    logger.info("패스 ${trackingPasses.size} 완료: ${currentPassData.size}개 포인트, 최대고도각: ${String.format("%.2f", maxElevationInPass)}°")
                }

                // 이전 값 저장
                prevAzimuth = azimuth
                prevElevation = elevation
                prevTime = currentTime

                // 다음 시간으로 이동
                currentTime = currentTime.plus(trackingIntervalMs.toLong(), ChronoUnit.MILLIS)

                // 진행 상황 로깅 (1시간마다)
                if (pointsCalculated % (3600000 / trackingIntervalMs) == 0) {
                    val progress = Duration.between(startDate, currentTime).toHours()
                    val total = Duration.between(startDate, endDate).toHours()
                    logger.info("진행 상황: ${progress}/${total}시간, 발견된 패스: ${trackingPasses.size}개")
                }
            }

            // 마지막 패스 처리 (종료 시점에서 가시성이 유지되는 경우)
            if (isVisible && passStartTime != null && currentPassData.isNotEmpty()) {
                val startAzimuth = currentPassData.firstOrNull()?.azimuth ?: 0.0
                val startElevation = currentPassData.firstOrNull()?.elevation ?: 0.0
                val endAzimuth = currentPassData.lastOrNull()?.azimuth ?: 0.0
                val endElevation = currentPassData.lastOrNull()?.elevation ?: 0.0

                val pass = SatelliteTrackingPass(
                    startTime = passStartTime,
                    endTime = currentTime,
                    maxElevation = maxElevationInPass,
                    maxElevationTime = maxElevationTime,
                    duration = Duration.between(passStartTime, currentTime),
                    trackingData = currentPassData.toList(),
                    maxAzimuthRate = maxAzimuthRate,
                    maxElevationRate = maxElevationRate,
                    maxAzimuthAccel = maxAzimuthAccel,
                    maxElevationAccel = maxElevationAccel,
                    startAzimuth = startAzimuth,
                    startElevation = startElevation,
                    endAzimuth = endAzimuth,
                    endElevation = endElevation
                )

                trackingPasses.add(pass)
                logger.info("마지막 패스 완료: ${currentPassData.size}개 포인트")
            }

            val totalPoints = trackingPasses.sumOf { it.trackingData.size }
            logger.info("통합 계산 완료: ${trackingPasses.size}개 패스, 총 ${totalPoints}개 포인트 (계산된 포인트: $pointsCalculated)")

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
            logger.error("최적화된 위성 추적 스케줄 생성 중 오류: ${e.message}", e)
            throw e
        }
    }
*/

}