package com.gtlsystems.acs_api.algorithm.suntrack.impl

import org.orekit.bodies.CelestialBodies
import org.orekit.bodies.CelestialBody
import org.orekit.data.DataContext
import org.orekit.frames.FramesFactory
import org.orekit.frames.TopocentricFrame
import org.orekit.time.AbsoluteDate
import org.orekit.time.TimeScalesFactory
import org.orekit.utils.Constants
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.bodies.GeodeticPoint
import org.orekit.utils.PVCoordinates
import org.orekit.utils.IERSConventions
import org.hipparchus.geometry.euclidean.threed.Vector3D
import org.hipparchus.util.FastMath
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class SolarOrekitCalculator {

    private lateinit var groundStation: TopocentricFrame
    private lateinit var sun: CelestialBody
    private var isInitialized = false

    /**
     * 지상국 위치를 설정하고 초기화
     * @param latitude 위도 (도)
     * @param longitude 경도 (도)
     * @param altitude 고도 (미터)
     */
    fun initializeGroundStation(latitude: Double, longitude: Double, altitude: Double) {
        try {
            // DataContext에서 CelestialBodies 가져오기 (Orekit 13.x 방식)
            val celestialBodies = DataContext.getDefault().celestialBodies
            sun = celestialBodies.sun

            // 지구 모델 생성 (Orekit 13.x 방식)
            val earth = OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getITRF(IERSConventions.IERS_2010, true)
            )

            // 지상국 위치 설정
            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )

            // 지상국 기준 좌표계 생성
            groundStation = TopocentricFrame(earth, stationPosition, "GroundStation")

            isInitialized = true

        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize ground station: ${e.message}", e)
        }
    }

    /**
     * 현재 시간의 태양 위치 계산
     */
    fun getCurrentSunPosition(): SunPosition {
        checkInitialized()
        // 현재 시간을 AbsoluteDate로 변환
        val now = getCurrentAbsoluteDate()
        return calculateSunPosition(now)
    }

    /**
     * 특정 시간의 태양 위치 계산
     * @param dateTime 계산할 시간
     */
    fun getSunPositionAt(dateTime: LocalDateTime): SunPosition {
        checkInitialized()
        val absoluteDate = localDateTimeToAbsoluteDate(dateTime)
        return calculateSunPosition(absoluteDate)
    }

    /**
     * 태양 위치 예측 (시간 범위)
     * @param startDateTime 시작 시간
     * @param endDateTime 종료 시간
     * @param stepMinutes 계산 간격 (분)
     */
    fun predictSunPositions(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        stepMinutes: Double
    ): List<SunPosition> {
        checkInitialized()

        val positions = mutableListOf<SunPosition>()
        val startDate = localDateTimeToAbsoluteDate(startDateTime)
        val endDate = localDateTimeToAbsoluteDate(endDateTime)

        var currentDate = startDate
        val stepSeconds = stepMinutes * 60.0

        while (currentDate.compareTo(endDate) <= 0) {
            val position = calculateSunPosition(currentDate)
            positions.add(position)
            currentDate = currentDate.shiftedBy(stepSeconds)
        }

        return positions
    }

    /**
     * 다음 일출 시간 찾기
     */
    fun findNextSunrise(fromDateTime: LocalDateTime? = null): SunPosition? {
        checkInitialized()

        val startDate = if (fromDateTime != null) {
            localDateTimeToAbsoluteDate(fromDateTime)
        } else {
            getCurrentAbsoluteDate()
        }

        var currentDate = startDate
        val stepMinutes = 10.0

        // 24시간 동안 검색
        repeat(144) {
            val position = calculateSunPosition(currentDate)
            if (position.isSunVisible()) {
                return position
            }
            currentDate = currentDate.shiftedBy(stepMinutes * 60)
        }

        return null
    }

    /**
     * 다음 일몰 시간 찾기
     */
    fun findNextSunset(fromDateTime: LocalDateTime? = null): SunPosition? {
        checkInitialized()

        val startDate = if (fromDateTime != null) {
            localDateTimeToAbsoluteDate(fromDateTime)
        } else {
            getCurrentAbsoluteDate()
        }

        // 현재 태양이 보이는지 확인
        val currentPosition = calculateSunPosition(startDate)
        if (!currentPosition.isSunVisible()) {
            return null // 태양이 이미 보이지 않으면 일몰을 찾을 수 없음
        }

        var currentDate = startDate
        val stepMinutes = 10.0

        // 24시간 동안 검색
        repeat(144) {
            val position = calculateSunPosition(currentDate)
            if (!position.isSunVisible()) {
                return position
            }
            currentDate = currentDate.shiftedBy(stepMinutes * 60)
        }

        return null
    }

    /**
     * 현재 시간을 AbsoluteDate로 변환
     */
    private fun getCurrentAbsoluteDate(): AbsoluteDate {
        val currentTimeMillis = System.currentTimeMillis()
        val currentDate = Date(currentTimeMillis)
        return AbsoluteDate(currentDate, TimeScalesFactory.getUTC())
    }

    /**
     * LocalDateTime을 AbsoluteDate로 변환
     */
    private fun localDateTimeToAbsoluteDate(dateTime: LocalDateTime): AbsoluteDate {
        val epochSecond = dateTime.toEpochSecond(ZoneOffset.UTC)
        val date = Date(epochSecond * 1000)
        return AbsoluteDate(date, TimeScalesFactory.getUTC())
    }

    /**
     * 태양의 방위각과 고도각 계산 (내부 메서드)
     */
    private fun calculateSunPosition(date: AbsoluteDate): SunPosition {
        // 태양의 위치를 지상국 기준 좌표계에서 계산
        val sunPV: PVCoordinates = sun.getPVCoordinates(date, groundStation)
        val sunPosition: Vector3D = sunPV.position

        // 방위각(Azimuth)과 고도각(Elevation) 계산
        val range = sunPosition.norm
        val elevation = FastMath.asin(sunPosition.z / range)
        var azimuth = FastMath.atan2(sunPosition.x, sunPosition.y)

        // 방위각을 0-360도 범위로 정규화
        if (azimuth < 0) {
            azimuth += 2 * FastMath.PI
        }

        return SunPosition(
            azimuthDegrees = FastMath.toDegrees(azimuth),
            elevationDegrees = FastMath.toDegrees(elevation),
            rangeKm = range / 1000.0,
            dateTime = absoluteDateToLocalDateTime(date)
        )
    }

    /**
     * AbsoluteDate를 LocalDateTime으로 변환
     */
    private fun absoluteDateToLocalDateTime(absoluteDate: AbsoluteDate): LocalDateTime {
        val date = absoluteDate.toDate(TimeScalesFactory.getUTC())
        val instant = date.toInstant()
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    }

    /**
     * 초기화 상태 확인
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("SolarOrekitCalculator is not initialized. Call initializeGroundStation() first.")
        }
    }

    /**
     * 태양 위치 정보를 담는 데이터 클래스
     */
    data class SunPosition(
        val azimuthDegrees: Double,      // 방위각 (도)
        val elevationDegrees: Double,    // 고도각 (도)
        val rangeKm: Double,            // 거리 (km)
        val dateTime: LocalDateTime     // 계산 시간
    ) {
        /**
         * 태양이 지평선 위에 보이는지 확인
         */
        fun isSunVisible(): Boolean = elevationDegrees > 0.0

        /**
         * 방위각을 라디안으로 반환
         */
        fun getAzimuthRadians(): Double = FastMath.toRadians(azimuthDegrees)

        /**
         * 고도각을 라디안으로 반환
         */
        fun getElevationRadians(): Double = FastMath.toRadians(elevationDegrees)

        override fun toString(): String {
            return "SunPosition(time=$dateTime, azimuth=${String.format("%.2f", azimuthDegrees)}°, " +
                    "elevation=${String.format("%.2f", elevationDegrees)}°, " +
                    "range=${String.format("%.0f", rangeKm)}km, visible=${isSunVisible()})"
        }
    }
}