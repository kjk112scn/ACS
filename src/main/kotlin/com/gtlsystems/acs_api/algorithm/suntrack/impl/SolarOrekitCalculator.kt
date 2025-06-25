package com.gtlsystems.acs_api.algorithm.suntrack.impl

import com.gtlsystems.acs_api.config.OrekitConfig
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
import org.orekit.time.TimeScale
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

// 수정 후
@Component
class SolarOrekitCalculator(
    private val utcTimeScale: TimeScale,
    private val ut1TimeScale: TimeScale,
    private val earthModel: OneAxisEllipsoid,
    private val sun: CelestialBody,
    private val orekitStatus: OrekitConfig.OrekitInitializationStatus
) {

    private lateinit var groundStation: TopocentricFrame
    private var isInitialized = false
    private val logger = LoggerFactory.getLogger(javaClass)
    /**
     * 지상국 위치를 설정하고 초기화
     */
    fun initializeGroundStation(latitude: Double, longitude: Double, altitude: Double) {
        try {

            // ✅ 간단한 상태 확인만
            if (!orekitStatus.isInitialized) {
                throw RuntimeException("Orekit이 초기화되지 않았습니다. OrekitConfig를 확인하세요.")
            }

            // 지상국 위치 설정 (주입받은 earthModel 사용)
            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )

            // 지상국 기준 좌표계 생성
            groundStation = TopocentricFrame(earthModel, stationPosition, "GroundStation")

            isInitialized = true
            logger.info("SolarOrekitCalculator 초기화 완료 (위도: $latitude, 경도: $longitude, 고도: $altitude)")

        } catch (e: Exception) {
            logger.error("SolarOrekitCalculator 초기화 실패: ${e.message}", e)
            throw RuntimeException("Failed to initialize ground station: ${e.message}", e)
        }
    }

    /**
     * 초기화 후 정확도 검증 수행
     */
    private fun performInitialAccuracyValidation() {
        try {
            logger.info("태양 위치 계산 정확도 초기 검증 시작...")

            val currentTime = LocalDateTime.now()
            val accuracy = validateSunPositionAccuracy(currentTime)

            val totalDiff = (accuracy["angular_differences"] as Map<*, *>)["total_angular_diff_arcsec"] as String
            val totalDiffValue = totalDiff.toDouble()

            when {
                totalDiffValue < 1.0 -> logger.info("태양 위치 정확도: 매우 높음 (${totalDiff}\")")
                totalDiffValue < 10.0 -> logger.info("태양 위치 정확도: 높음 (${totalDiff}\")")
                totalDiffValue < 60.0 -> logger.warn("태양 위치 정확도: 보통 (${totalDiff}\")")
                else -> logger.error("태양 위치 정확도: 낮음 (${totalDiff}\") - EOP 데이터 확인 필요")
            }

        } catch (e: Exception) {
            logger.warn("초기 정확도 검증 실패: ${e.message}")
        }
    }

    /**
     * 개발 모드 확인
     */
    private fun isDevelopmentMode(): Boolean {
        return System.getProperty("spring.profiles.active")?.contains("dev") == true ||
               System.getProperty("spring.profiles.active")?.contains("test") == true
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
        return AbsoluteDate(currentDate, ut1TimeScale)
    }

    /**
     * LocalDateTime을 AbsoluteDate로 변환
     */
    private fun localDateTimeToAbsoluteDate(dateTime: LocalDateTime): AbsoluteDate {
        val epochSecond = dateTime.toEpochSecond(ZoneOffset.UTC)
        val date = Date(epochSecond * 1000)
        return AbsoluteDate(date, ut1TimeScale)
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
     * AbsoluteDate를 LocalDateTime으로 변환 (UT1 기준)
     * @param absoluteDate UT1 기준 AbsoluteDate
     */
    private fun absoluteDateToLocalDateTime(absoluteDate: AbsoluteDate): LocalDateTime {
        // ✅ 명시적으로 UT1 시간 척도 사용
        val date = absoluteDate.toDate(ut1TimeScale)
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
     * 시간 척도 정보 확인 (디버깅용)
     */
    fun getTimeScaleInfo(): Map<String, Any> {
        checkInitialized()

        val now = Date()
        val utcDate = AbsoluteDate(now, utcTimeScale)
        val ut1Date = AbsoluteDate(now, ut1TimeScale)

        // DUT1 = UT1 - UTC 계산
        val dut1 = ut1Date.durationFrom(utcDate)

        return mapOf(
            "using_ut1_frame" to true,
            "using_ut1_time" to true,
            "dut1_seconds" to dut1,
            "dut1_milliseconds" to (dut1 * 1000).toInt()
        )
    }
    /**
     * 특정 시간에서 UTC와 UT1 시간 척도 차이에 따른 태양 위치 비교
     */
    fun compareTimeScalesAndSunPosition(dateTime: LocalDateTime): Map<String, Any> {
        checkInitialized()

        val epochSecond = dateTime.toEpochSecond(ZoneOffset.UTC)
        val date = Date(epochSecond * 1000)

        // UTC 기준 계산
        val utcAbsoluteDate = AbsoluteDate(date, utcTimeScale)
        val utcSunPosition = calculateSunPositionWithTimeScale(utcAbsoluteDate)

        // UT1 기준 계산
        val ut1AbsoluteDate = AbsoluteDate(date, ut1TimeScale)
        val ut1SunPosition = calculateSunPositionWithTimeScale(ut1AbsoluteDate)

        // 시간 차이 계산 (DUT1 = UT1 - UTC)
        val dut1Seconds = ut1AbsoluteDate.durationFrom(utcAbsoluteDate)

        // 각도 차이 계산
        val azimuthDiff = ut1SunPosition.azimuthDegrees - utcSunPosition.azimuthDegrees
        val elevationDiff = ut1SunPosition.elevationDegrees - utcSunPosition.elevationDegrees

        // 각도 차이를 아크초(arcsecond)로 변환 (1도 = 3600 아크초)
        val azimuthDiffArcsec = azimuthDiff * 3600.0
        val elevationDiffArcsec = elevationDiff * 3600.0

        // 총 각도 차이 (벡터 크기)
        val totalAngularDiff = Math.sqrt(azimuthDiff * azimuthDiff + elevationDiff * elevationDiff)
        val totalAngularDiffArcsec = totalAngularDiff * 3600.0

        return mapOf(
            "input_time" to dateTime.toString(),
            "time_comparison" to mapOf(
                "dut1_seconds" to dut1Seconds,
                "dut1_milliseconds" to (dut1Seconds * 1000).toInt(),
                "utc_absolute_date" to utcAbsoluteDate.toString(),
                "ut1_absolute_date" to ut1AbsoluteDate.toString()
            ),
            "sun_position_utc" to mapOf(
                "azimuth_deg" to String.format("%.6f", utcSunPosition.azimuthDegrees),
                "elevation_deg" to String.format("%.6f", utcSunPosition.elevationDegrees),
                "range_km" to String.format("%.3f", utcSunPosition.rangeKm),
                "time_scale" to "UTC"
            ),
            "sun_position_ut1" to mapOf(
                "azimuth_deg" to String.format("%.6f", ut1SunPosition.azimuthDegrees),
                "elevation_deg" to String.format("%.6f", ut1SunPosition.elevationDegrees),
                "range_km" to String.format("%.3f", ut1SunPosition.rangeKm),
                "time_scale" to "UT1"
            ),
            "angular_differences" to mapOf(
                // 원본 Double 값들 (계산용)
                "azimuth_diff_deg_raw" to azimuthDiff,
                "elevation_diff_deg_raw" to elevationDiff,
                "azimuth_diff_arcsec_raw" to azimuthDiffArcsec,
                "elevation_diff_arcsec_raw" to elevationDiffArcsec,
                "total_angular_diff_deg_raw" to totalAngularDiff,
                "total_angular_diff_arcsec_raw" to totalAngularDiffArcsec,

                // 표시용 String 값들
                "azimuth_diff_deg" to String.format("%.6f", azimuthDiff),
                "elevation_diff_deg" to String.format("%.6f", elevationDiff),
                "azimuth_diff_arcsec" to String.format("%.3f", azimuthDiffArcsec),
                "elevation_diff_arcsec" to String.format("%.3f", elevationDiffArcsec),
                "total_angular_diff_deg" to String.format("%.6f", totalAngularDiff),
                "total_angular_diff_arcsec" to String.format("%.3f", totalAngularDiffArcsec)
            ),
            "accuracy_impact" to mapOf(
                "significant_difference" to (Math.abs(totalAngularDiffArcsec) > 1.0),
                "precision_category" to when {
                    Math.abs(totalAngularDiffArcsec) < 0.1 -> "매우 높음 (<0.1\")"
                    Math.abs(totalAngularDiffArcsec) < 1.0 -> "높음 (<1\")"
                    Math.abs(totalAngularDiffArcsec) < 10.0 -> "보통 (<10\")"
                    else -> "낮음 (≥10\")"
                }
            )
        )
    }

    /**
     * 특정 시간 척도로 태양 위치 계산 (내부 메서드)
     */
    private fun calculateSunPositionWithTimeScale(date: AbsoluteDate): SunPosition {
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
     * 시간 범위에서 UTC/UT1 태양 위치 차이 분석
     */
    /**
     * UT1 SimpleEOP true vs false 설정에 따른 태양 위치 차이 분석 (각도 기준)
     */
    fun analyzeTimeScaleDifferencesOverTime(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        stepMinutes: Double = 60.0
    ): Map<String, Any> {
        checkInitialized()

        val analysisResults = mutableListOf<Map<String, Any>>()
        var currentDateTime = startDateTime
        val stepDuration = java.time.Duration.ofMinutes(stepMinutes.toLong())

        var maxAngularDiff = 0.0
        var maxAngularDiffTime: LocalDateTime? = null
        var minAngularDiff = Double.MAX_VALUE
        var minAngularDiffTime: LocalDateTime? = null

        // ✅ eopEnabled 참조 제거하고 직접 값 사용
        val ut1SimpleTrue = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true)   // SimpleEOP = true
        val ut1SimpleFalse = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, false) // SimpleEOP = false

        while (currentDateTime.isBefore(endDateTime) || currentDateTime.isEqual(endDateTime)) {
            val epochSecond = currentDateTime.toEpochSecond(ZoneOffset.UTC)
            val date = Date(epochSecond * 1000)

            // UT1 SimpleEOP = true 기준 계산
            val ut1SimpleTrueDate = AbsoluteDate(date, ut1SimpleTrue)
            val ut1SimpleTrueSunPos = calculateSunPositionWithTimeScale(ut1SimpleTrueDate)

            // UT1 SimpleEOP = false 기준 계산
            val ut1SimpleFalseDate = AbsoluteDate(date, ut1SimpleFalse)
            val ut1SimpleFalseSunPos = calculateSunPositionWithTimeScale(ut1SimpleFalseDate)

            // 시간 차이 계산 (DUT1 차이)
            val dut1Difference = ut1SimpleFalseDate.durationFrom(ut1SimpleTrueDate)

            // 지상국에서 태양을 바라보는 각도 차이 계산 (degree)
            val azimuthDiff = ut1SimpleFalseSunPos.azimuthDegrees - ut1SimpleTrueSunPos.azimuthDegrees
            val elevationDiff = ut1SimpleFalseSunPos.elevationDegrees - ut1SimpleTrueSunPos.elevationDegrees

            // 총 각도 차이 (벡터 크기, degree)
            val totalAngularDiff = Math.sqrt(azimuthDiff * azimuthDiff + elevationDiff * elevationDiff)

            // 각도 차이를 아크초(arcsecond)로도 변환 (참고용)
            val azimuthDiffArcsec = azimuthDiff * 3600.0
            val elevationDiffArcsec = elevationDiff * 3600.0
            val totalAngularDiffArcsec = totalAngularDiff * 3600.0

            // 최대/최소 차이 추적 (degree 기준)
            if (totalAngularDiff > maxAngularDiff) {
                maxAngularDiff = totalAngularDiff
                maxAngularDiffTime = currentDateTime
            }
            if (totalAngularDiff < minAngularDiff) {
                minAngularDiff = totalAngularDiff
                minAngularDiffTime = currentDateTime
            }

            analysisResults.add(mapOf(
                "time" to currentDateTime.toString(),
                // 각도 차이 (degree) - 주요 비교 기준
                "total_angular_diff_deg" to String.format("%.8f", totalAngularDiff),
                "azimuth_diff_deg" to String.format("%.8f", azimuthDiff),
                "elevation_diff_deg" to String.format("%.8f", elevationDiff),
                // 각도 차이 (arcsecond) - 참고용
                "total_angular_diff_arcsec" to String.format("%.3f", totalAngularDiffArcsec),
                "azimuth_diff_arcsec" to String.format("%.3f", azimuthDiffArcsec),
                "elevation_diff_arcsec" to String.format("%.3f", elevationDiffArcsec),
                // UT1 SimpleEOP = true 태양 위치
                "ut1_simple_true_azimuth_deg" to String.format("%.6f", ut1SimpleTrueSunPos.azimuthDegrees),
                "ut1_simple_true_elevation_deg" to String.format("%.6f", ut1SimpleTrueSunPos.elevationDegrees),
                // UT1 SimpleEOP = false 태양 위치
                "ut1_simple_false_azimuth_deg" to String.format("%.6f", ut1SimpleFalseSunPos.azimuthDegrees),
                "ut1_simple_false_elevation_deg" to String.format("%.6f", ut1SimpleFalseSunPos.elevationDegrees),
                // 시간 차이 정보
                "dut1_difference_seconds" to String.format("%.6f", dut1Difference),
                "dut1_difference_milliseconds" to String.format("%.3f", dut1Difference * 1000)
            ))

            currentDateTime = currentDateTime.plus(stepDuration)
        }

        // 통계 계산 (degree 기준)
        val totalDiffs = analysisResults.map {
            (it["total_angular_diff_deg"] as String).toDouble()
        }
        val azimuthDiffs = analysisResults.map {
            Math.abs((it["azimuth_diff_deg"] as String).toDouble())
        }
        val elevationDiffs = analysisResults.map {
            Math.abs((it["elevation_diff_deg"] as String).toDouble())
        }

        val avgDiff = if (totalDiffs.isNotEmpty()) totalDiffs.average() else 0.0
        val stdDev = if (totalDiffs.isNotEmpty()) {
            Math.sqrt(totalDiffs.map { (it - avgDiff) * (it - avgDiff) }.average())
        } else 0.0

        val avgAzDiff = if (azimuthDiffs.isNotEmpty()) azimuthDiffs.average() else 0.0
        val avgElDiff = if (elevationDiffs.isNotEmpty()) elevationDiffs.average() else 0.0

        // 최대/최소 시간 찾기
        val maxAzDiffResult = analysisResults.maxByOrNull {
            Math.abs((it["azimuth_diff_deg"] as String).toDouble())
        }
        val minAzDiffResult = analysisResults.minByOrNull {
            Math.abs((it["azimuth_diff_deg"] as String).toDouble())
        }
        val maxElDiffResult = analysisResults.maxByOrNull {
            Math.abs((it["elevation_diff_deg"] as String).toDouble())
        }
        val minElDiffResult = analysisResults.minByOrNull {
            Math.abs((it["elevation_diff_deg"] as String).toDouble())
        }

        return mapOf(
            "analysis_period" to mapOf(
                "start" to startDateTime.toString(),
                "end" to endDateTime.toString(),
                "step_minutes" to stepMinutes,
                "total_points" to analysisResults.size
            ),
            "comparison_description" to mapOf(
                "comparison_type" to "UT1 SimpleEOP true vs false",
                "ut1_simple_true" to "기본 UT1-UTC 차이만 적용 (빠름, 낮은 정확도)",
                "ut1_simple_false" to "모든 지구 자전 변화 적용 (느림, 높은 정확도)",
                "measurement_unit" to "degree (지상국에서 태양을 바라보는 각도)",
                "measurement_description" to "지상국 기준 태양의 방위각(Azimuth)과 고도각(Elevation) 차이"
            ),
            "statistics" to mapOf(
                "max_angular_diff_deg" to String.format("%.8f", maxAngularDiff),
                "max_angular_diff_arcsec" to String.format("%.3f", maxAngularDiff * 3600),
                "max_diff_time" to (maxAngularDiffTime?.toString() ?: ""),
                "min_angular_diff_deg" to String.format("%.8f", minAngularDiff),
                "min_angular_diff_arcsec" to String.format("%.3f", minAngularDiff * 3600),
                "min_diff_time" to (minAngularDiffTime?.toString() ?: ""),
                "average_diff_deg" to String.format("%.8f", avgDiff),
                "average_diff_arcsec" to String.format("%.3f", avgDiff * 3600),
                "std_deviation_deg" to String.format("%.8f", stdDev),
                "std_deviation_arcsec" to String.format("%.3f", stdDev * 3600)
            ),
            "angular_differences_deg" to mapOf(
                "total_angular_diff" to mapOf(
                    "max_deg" to String.format("%.8f", maxAngularDiff),
                    "min_deg" to String.format("%.8f", minAngularDiff),
                    "average_deg" to String.format("%.8f", avgDiff),
                    "max_time" to (maxAngularDiffTime?.toString() ?: ""),
                    "min_time" to (minAngularDiffTime?.toString() ?: "")
                ),
                "azimuth_diff" to mapOf(
                    "max_deg" to String.format("%.8f", azimuthDiffs.maxOrNull() ?: 0.0),
                    "min_deg" to String.format("%.8f", azimuthDiffs.minOrNull() ?: 0.0),
                    "average_deg" to String.format("%.8f", avgAzDiff),
                    "max_time" to (maxAzDiffResult?.get("time") ?: ""),
                    "min_time" to (minAzDiffResult?.get("time") ?: "")
                ),
                "elevation_diff" to mapOf(
                    "max_deg" to String.format("%.8f", elevationDiffs.maxOrNull() ?: 0.0),
                    "min_deg" to String.format("%.8f", elevationDiffs.minOrNull() ?: 0.0),
                    "average_deg" to String.format("%.8f", avgElDiff),
                    "max_time" to (maxElDiffResult?.get("time") ?: ""),
                    "min_time" to (minElDiffResult?.get("time") ?: "")
                )
            ),
            "detailed_results" to analysisResults,
            "recommendations" to mapOf(
                "precision_impact" to when {
                    maxAngularDiff < 0.0001 -> "무시할 수 있는 수준 (<0.0001°, <0.36\")"
                    maxAngularDiff < 0.001 -> "매우 작은 영향 (<0.001°, <3.6\")"
                    maxAngularDiff < 0.01 -> "작은 영향 (<0.01°, <36\")"
                    maxAngularDiff < 0.1 -> "보통 영향 (<0.1°, <360\")"
                    else -> "큰 영향 (≥0.1°, ≥360\")"
                },
                "azimuth_precision_impact" to when {
                    (azimuthDiffs.maxOrNull() ?: 0.0) < 0.0001 -> "방위각: 무시할 수 있는 수준"
                    (azimuthDiffs.maxOrNull() ?: 0.0) < 0.001 -> "방위각: 매우 작은 영향"
                    (azimuthDiffs.maxOrNull() ?: 0.0) < 0.01 -> "방위각: 작은 영향"
                    (azimuthDiffs.maxOrNull() ?: 0.0) < 0.1 -> "방위각: 보통 영향"
                    else -> "방위각: 큰 영향"
                },
                "elevation_precision_impact" to when {
                    (elevationDiffs.maxOrNull() ?: 0.0) < 0.0001 -> "고도각: 무시할 수 있는 수준"
                    (elevationDiffs.maxOrNull() ?: 0.0) < 0.001 -> "고도각: 매우 작은 영향"
                    (elevationDiffs.maxOrNull() ?: 0.0) < 0.01 -> "고도각: 작은 영향"
                    (elevationDiffs.maxOrNull() ?: 0.0) < 0.1 -> "고도각: 보통 영향"
                    else -> "고도각: 큰 영향"
                },
                "use_simple_eop_false_recommended" to (maxAngularDiff > 0.001),
                "recommendation_summary" to if (maxAngularDiff > 0.001) {
                    "SimpleEOP 설정 차이가 유의미함 - 정확한 계산을 위해 SimpleEOP=false 권장"
                } else {
                    "SimpleEOP 설정 차이가 작음 - 성능을 위해 SimpleEOP=true 사용 가능"
                }
            )
        )
    }

    /**
     * 태양 위치 계산 정확도 검증
     */
    fun validateSunPositionAccuracy(dateTime: LocalDateTime): Map<String, Any> {
        checkInitialized()

        val epochSecond = dateTime.toEpochSecond(ZoneOffset.UTC)
        val date = Date(epochSecond * 1000)

        // UTC vs UT1 비교
        val utcDate = AbsoluteDate(date, utcTimeScale)
        val ut1Date = AbsoluteDate(date, ut1TimeScale)

        val utcSunPos = calculateSunPositionWithTimeScale(utcDate)
        val ut1SunPos = calculateSunPositionWithTimeScale(ut1Date)

        // DUT1 계산
        val dut1Seconds = ut1Date.durationFrom(utcDate)

        // 각도 차이 계산
        val azimuthDiff = ut1SunPos.azimuthDegrees - utcSunPos.azimuthDegrees
        val elevationDiff = ut1SunPos.elevationDegrees - utcSunPos.elevationDegrees
        val totalAngularDiff = Math.sqrt(azimuthDiff * azimuthDiff + elevationDiff * elevationDiff)

        return mapOf(
            "input_time" to dateTime.toString(),
            "dut1_seconds" to dut1Seconds,
            "utc_sun_position" to mapOf(
                "azimuth_deg" to String.format("%.6f", utcSunPos.azimuthDegrees),
                "elevation_deg" to String.format("%.6f", utcSunPos.elevationDegrees)
            ),
            "ut1_sun_position" to mapOf(
                "azimuth_deg" to String.format("%.6f", ut1SunPos.azimuthDegrees),
                "elevation_deg" to String.format("%.6f", ut1SunPos.elevationDegrees)
            ),
            "angular_differences" to mapOf(
                "azimuth_diff_deg" to String.format("%.6f", azimuthDiff),
                "elevation_diff_deg" to String.format("%.6f", elevationDiff),
                "total_angular_diff_deg" to String.format("%.6f", totalAngularDiff),
                "total_angular_diff_arcsec" to String.format("%.3f", totalAngularDiff * 3600.0)
            ),
            "accuracy_assessment" to when {
                totalAngularDiff < 0.001 -> "매우 높은 정확도 (<0.001°, <3.6\")"
                totalAngularDiff < 0.01 -> "높은 정확도 (<0.01°, <36\")"
                totalAngularDiff < 0.1 -> "보통 정확도 (<0.1°, <360\")"
                else -> "낮은 정확도 (≥0.1°, ≥360\")"
            }
        )
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
