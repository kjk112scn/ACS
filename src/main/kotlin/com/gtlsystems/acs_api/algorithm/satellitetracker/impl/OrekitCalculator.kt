package com.gtlsystems.acs_api.algorithm.satellitetracker.impl

import com.gtlsystems.acs_api.algorithm.satellitetracker.interfaces.SatellitePositionCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import com.gtlsystems.acs_api.controller.VisibilityPeriod
import com.gtlsystems.acs_api.model.GlobalData
import org.hipparchus.util.FastMath
import org.orekit.bodies.GeodeticPoint
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.data.DataContext
import org.orekit.data.DirectoryCrawler
import org.orekit.frames.Frame
import org.orekit.frames.FramesFactory
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.time.AbsoluteDate
import org.orekit.time.TimeScalesFactory
import org.orekit.utils.Constants
import org.orekit.utils.IERSConventions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.JarURLConnection
import java.nio.file.Files
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Orekit 라이브러리를 사용하여 위성 위치를 계산하는 클래스
 */
@Service
class OrekitCalculator : SatellitePositionCalculator {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val earthFrame: Frame
    private val earth: OneAxisEllipsoid

    init {
        // Orekit 데이터 파일 설정
        setupOrekitData()

        // 지구 모델 및 프레임 설정
        earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true)
        earth = OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            earthFrame
        )
    }

    /**
     * 지정된 시간과 위치에 대한 위성 위치를 계산합니다.
     */
    override fun calculatePosition(
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
            // 지상국 기준 위치 프레임 생성
            val stationFrame = TopocentricFrame(earth, stationPosition, "GroundStation")

            // UTC 시간을 Orekit의 AbsoluteDate로 변환
            val utcScale = TimeScalesFactory.getUTC()
            val date = AbsoluteDate(
                dateTime.year, dateTime.monthValue, dateTime.dayOfMonth,
                dateTime.hour, dateTime.minute, dateTime.second + dateTime.nano / 1e9,
                utcScale
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
            val azimuth = FastMath.atan2(y, x)

            // 라디안에서 도로 변환
            val elevationDegrees = FastMath.toDegrees(elevation)
            val azimuthDegrees = FastMath.toDegrees(azimuth)

            // 방위각을 0-360도 범위로 조정
            val normalizedAzimuth = if (azimuthDegrees < 0) azimuthDegrees + 360.0 else azimuthDegrees

            // 디버깅을 위한 로그 추가
            logger.info("원시 좌표 - x: $x, y: $y, z: $z")
            logger.info("방위각 계산 - atan2(y, x): ${FastMath.toDegrees(FastMath.atan2(y, x))}")
            logger.info("방위각 계산 - atan2(x, y): ${FastMath.toDegrees(FastMath.atan2(x, y))}")

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
     * 특정 시간 범위 동안의 위성 위치를 계산합니다
     */
    fun calculateTrackingPath(
        tleLine1: String,
        tleLine2: String,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        interval: Int = 1, // 기본값 분 간격
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0
    ): SatelliteTrackData {
        val positions = mutableListOf<Pair<ZonedDateTime, SatelliteTrackData>>()

        var currentTime = startTime
        while (!currentTime.isAfter(endTime)) {
            val position = calculatePosition(tleLine1, tleLine2, currentTime, latitude, longitude, altitude)
            positions.add(Pair(currentTime, position))
            currentTime = currentTime.plusMinutes(interval.toLong())
        }

        // 첫 번째 위치의 방위각과 고도각을 사용
        val firstPosition = positions.firstOrNull()?.second ?: SatelliteTrackData(0.0f, 0.0f)

        return SatelliteTrackData(
            azimuth = firstPosition.azimuth,
            elevation = firstPosition.elevation,
            startTime = startTime,
            endTime = endTime,
            interval = interval,
            positions = positions
        )
    }

    /**
     * 위성이 지평선 위에 있는 시간(가시 시간)을 계산합니다
     */
    fun calculateVisibilityPeriods(
        tleLine1: String,
        tleLine2: String,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        interval: Int = 100, // 기본값 간격
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        minElevation: Float = 0.0f // 최소 고도각 (기본값 0도)
    ): List<Pair<ZonedDateTime, ZonedDateTime>> {
        val visibilityPeriods = mutableListOf<Pair<ZonedDateTime, ZonedDateTime>>()
        var visibilityStart: ZonedDateTime? = null

        var currentTime = startTime
        while (!currentTime.isAfter(endTime)) {
            val position = calculatePosition(tleLine1, tleLine2, currentTime, latitude, longitude, altitude)

            // 위성이 지평선 위에 있고 가시성 시작 시간이 없는 경우
            if (position.elevation >= minElevation && visibilityStart == null) {
                visibilityStart = currentTime
            }
            // 위성이 지평선 아래로 내려가고 가시성 시작 시간이 있는 경우
            else if (position.elevation < minElevation && visibilityStart != null) {
                visibilityPeriods.add(Pair(visibilityStart, currentTime))
                visibilityStart = null
            }

            currentTime = currentTime.plus(interval.toLong(), ChronoUnit.MILLIS)
        }

        // 마지막 가시성 기간이 endTime까지 계속되는 경우
        if (visibilityStart != null) {
            visibilityPeriods.add(Pair(visibilityStart, endTime))
        }

        return visibilityPeriods
    }

    /**
     * Orekit 데이터 파일 설정 - 리소스 폴더에서 로드
     */
    private fun setupOrekitData() {
        try {
            logger.info("Orekit 데이터 파일을 리소스에서 로드합니다.")

            // 리소스에서 orekit-data-main 디렉토리 찾기
            val classLoader = javaClass.classLoader
            val orekitDataUrl = classLoader.getResource("orekit-data-main")

            if (orekitDataUrl == null) {
                logger.error("리소스에서 orekit-data-main 디렉토리를 찾을 수 없습니다.")
                throw FileNotFoundException("리소스에서 orekit-data-main 디렉토리를 찾을 수 없습니다.")
            }

            // URL을 File로 변환 (JAR 내부 리소스인 경우 임시 디렉토리에 복사)
            val orekitDataDir = if (orekitDataUrl.protocol == "jar") {
                // 임시 디렉토리 생성
                val tempDir = Files.createTempDirectory("orekit-data").toFile()
                tempDir.deleteOnExit()

                // JAR 내부 리소스를 임시 디렉토리에 복사
                copyResourcesFromJar("orekit-data-main", tempDir)
                tempDir
            } else {
                File(orekitDataUrl.toURI())
            }

            // 데이터 컨텍스트 설정
            val dataContext = DataContext.getDefault()
            val dataProvidersManager = dataContext.dataProvidersManager
            dataProvidersManager.addProvider(DirectoryCrawler(orekitDataDir))

            logger.info("Orekit 데이터 파일이 성공적으로 로드되었습니다. 경로: ${orekitDataDir.absolutePath}")
        } catch (e: Exception) {
            logger.error("Orekit 데이터 설정 중 오류 발생: ${e.message}", e)
            throw e
        }
    }

    /**
     * JAR 내부 리소스를 지정된 디렉토리로 복사합니다.
     */
    private fun copyResourcesFromJar(resourcePath: String, targetDir: File) {
        val classLoader = javaClass.classLoader
        val jarUrl = classLoader.getResource(resourcePath) ?: return
        val jarConnection = jarUrl.openConnection() as JarURLConnection
        val jarFile = jarConnection.jarFile

        val entries = jarFile.entries()
        val prefix = "$resourcePath/"

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.startsWith(prefix) && !entry.isDirectory) {
                val destFile = File(targetDir, entry.name.substring(prefix.length))

                // 필요한 경우 상위 디렉토리 생성
                destFile.parentFile?.mkdirs()

                // 파일 복사
                classLoader.getResourceAsStream(entry.name)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    /**
     * 지정된 기간 동안 위성 추적 스케줄을 생성합니다.
     *
     * @param tleLine1 TLE 첫 번째 줄
     * @param tleLine2 TLE 두 번째 줄
     * @param startDate 시작 날짜
     * @param durationDays 계산할 기간(일)
     * @param minElevation 최소 고도각(도)
     * @param latitude 지상국 위도
     * @param longitude 지상국 경도
     * @param altitude 지상국 고도
     * @param trackingIntervalMs 추적 데이터 간격(밀리초)
     * @return 위성 추적 스케줄 (가시성 기간 및 상세 추적 데이터)
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
        val endDate = startDate.plusDays(durationDays.toLong())

        // 1. 먼저 가시성 기간을 계산 (1분 간격으로 대략적인 계산)
        val visibilityPeriods = calculateVisibilityPeriodsWithMaxElevation(
            tleLine1, tleLine2, startDate, durationDays, minElevation,
            latitude, longitude, altitude, 1
        )

        // 2. 각 가시성 기간에 대해 상세 추적 데이터 생성
        val trackingPasses = visibilityPeriods.map { period ->
            // 각 가시성 기간에 대한 상세 추적 데이터 생성
            val detailedTrackingData = generateDetailedTrackingData(
                tleLine1, tleLine2, period.startTime, period.endTime,
                trackingIntervalMs, latitude, longitude, altitude
            )

            SatelliteTrackingPass(
                startTime = period.startTime,
                endTime = period.endTime,
                maxElevation = period.maxElevation,
                maxElevationTime = period.maxElevationTime,
                duration = period.duration,
                trackingData = detailedTrackingData
            )
        }

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
        altitude: Double = 0.0
    ): List<SatelliteTrackData> {
        val trackingData = mutableListOf<SatelliteTrackData>()

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
            val stationFrame = TopocentricFrame(earth, stationPosition, "GroundStation")

            // 시간 간격으로 위성 위치 계산
            var currentTime = startTime
            val utcScale = TimeScalesFactory.getUTC()

            while (!currentTime.isAfter(endTime)) {
                val date = AbsoluteDate(
                    currentTime.year, currentTime.monthValue, currentTime.dayOfMonth,
                    currentTime.hour, currentTime.minute, currentTime.second + currentTime.nano / 1e9,
                    utcScale
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

                // 방위각 계산
                val azimuth = FastMath.toDegrees(FastMath.atan2(y, x))
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

                // 다음 시간으로 이동 (밀리초 단위)
                currentTime = currentTime.plus(intervalMs.toLong(), ChronoUnit.MILLIS)
            }

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
        val trackingData: List<SatelliteTrackData>
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

        // 요약 정보
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
     * Duration을 읽기 쉬운 형식으로 포맷팅합니다.
     */
    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()
        val millis = duration.toMillisPart()
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
    /**
     * 최적화된 위성 가시성 기간 계산 메서드 (TLE 객체를 직접 받아 재파싱 방지)
     */
    fun calculateVisibilityPeriodsWithMaxElevationOptimized(
        tle: TLE,
        startTime: ZonedDateTime,
        durationDays: Int = 1,
        minElevation: Float = 0.0f,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        timeStepMs: Int = 100
    ): List<VisibilityPeriod> {
        val endTime = startTime.plusDays(durationDays.toLong())

        // 1. 첫 번째 패스: 위치 데이터만 계산하여 저장
        val positionData = mutableListOf<PositionData>()
        val visibilityPeriods = mutableListOf<VisibilityPeriodBasic>()
        var visibilityStart: ZonedDateTime? = null
        var maxElevationInPass: Double = -90.0
        var maxElevationTime: ZonedDateTime? = null

        try {
            // TLE 전파기(propagator) 생성
            val propagator = TLEPropagator.selectExtrapolator(tle)

            // 지상국 위치 설정
            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            val stationFrame = TopocentricFrame(earth, stationPosition, "GroundStation")

            // 시간 간격 설정 (밀리초 단위)
            val timeStep = timeStepMs / 1000.0

            // 시간 범위 설정
            var currentDate = toAbsoluteDate(startTime)
            var currentTime = startTime

            while (currentDate.compareTo(toAbsoluteDate(endTime)) < 0) {
                // 위성 위치 계산
                val state = propagator.propagate(currentDate)
                val pvInStation = state.getPVCoordinates(stationFrame)
                val posInStation = pvInStation.position

                // 직교 좌표를 구면 좌표로 변환
                val x = posInStation.x
                val y = posInStation.y
                val z = posInStation.z
                val distance = posInStation.norm

                // 고도각 계산
                val elevation = FastMath.toDegrees(FastMath.asin(z / distance))

                // 방위각 계산
                val azimuth = FastMath.toDegrees(FastMath.atan2(y, x))
                val normalizedAzimuth = if (azimuth < 0) azimuth + 360.0 else azimuth

                // 위치 데이터 저장
                positionData.add(PositionData(
                    time = currentTime,
                    azimuth = normalizedAzimuth,
                    elevation = elevation
                ))

                // 가시성 상태 변경 처리
                if (elevation >= minElevation) {
                    // 가시성 시작
                    if (visibilityStart == null) {
                        visibilityStart = currentTime
                        maxElevationInPass = elevation
                        maxElevationTime = currentTime
                    }

                    // 최대 고도각 업데이트
                    if (elevation > maxElevationInPass) {
                        maxElevationInPass = elevation
                        maxElevationTime = currentTime
                    }
                } else {
                    // 가시성 종료
                    if (visibilityStart != null) {
                        visibilityPeriods.add(
                            VisibilityPeriodBasic(
                                startTime = visibilityStart,
                                endTime = currentTime,
                                maxElevation = maxElevationInPass,
                                maxElevationTime = maxElevationTime
                            )
                        )
                        visibilityStart = null
                        maxElevationInPass = -90.0
                        maxElevationTime = null
                    }
                }

                // 다음 시간으로 이동
                currentDate = currentDate.shiftedBy(timeStep)
                currentTime = currentTime.plusNanos((timeStep * 1e9).toLong())
            }

            // 마지막 가시성 기간이 종료되지 않은 경우 처리
            if (visibilityStart != null) {
                visibilityPeriods.add(
                    VisibilityPeriodBasic(
                        startTime = visibilityStart,
                        endTime = currentTime,
                        maxElevation = maxElevationInPass,
                        maxElevationTime = maxElevationTime
                    )
                )
            }

            // 2. 두 번째 패스: 저장된 데이터를 사용하여 속도와 가속도 계산
            return calculateRatesAndAccelerations(visibilityPeriods, positionData, minElevation)

        } catch (e: Exception) {
            logger.error("위성 가시성 기간 계산 중 오류 발생: ${e.message}", e)
            throw e
        }
    }

    // 기본 가시성 기간 정보를 담는 데이터 클래스
    private data class VisibilityPeriodBasic(
        val startTime: ZonedDateTime,
        val endTime: ZonedDateTime,
        val maxElevation: Double,
        val maxElevationTime: ZonedDateTime?
    )

    // 위치 데이터를 담는 데이터 클래스
    private data class PositionData(
        val time: ZonedDateTime,
        val azimuth: Double,
        val elevation: Double
    )

    // 저장된 데이터를 사용하여 속도와 가속도를 계산하는 함수
    private fun calculateRatesAndAccelerations(
        visibilityPeriods: List<VisibilityPeriodBasic>,
        positionData: List<PositionData>,
        minElevation: Float
    ): List<VisibilityPeriod> {
        val result = mutableListOf<VisibilityPeriod>()

        for (period in visibilityPeriods) {
            // 현재 가시성 기간에 해당하는 위치 데이터 필터링
            val periodData = positionData.filter {
                it.time >= period.startTime && it.time <= period.endTime && it.elevation >= minElevation
            }

            if (periodData.size < 2) {
                // 데이터가 충분하지 않으면 기본값으로 처리
                result.add(VisibilityPeriod(
                    startTime = period.startTime,
                    endTime = period.endTime,
                    maxElevation = period.maxElevation,
                    maxElevationTime = period.maxElevationTime
                ))
                continue
            }

            var maxAzimuthRate = 0.0
            var maxElevationRate = 0.0
            var maxAzimuthAccel = 0.0
            var maxElevationAccel = 0.0

            // 속도 계산
            val rates = mutableListOf<Pair<Double, Double>>() // (azimuthRate, elevationRate)

            for (i in 1 until periodData.size) {
                val prev = periodData[i-1]
                val curr = periodData[i]

                val timeDiff = Duration.between(prev.time, curr.time).toMillis() / 1000.0 // 초 단위로 변환

                // 방위각 변화 처리 (360도 경계 처리)
                var azimuthDiff = curr.azimuth - prev.azimuth
                if (azimuthDiff > 180) azimuthDiff -= 360
                if (azimuthDiff < -180) azimuthDiff += 360

                val elevationDiff = curr.elevation - prev.elevation

                // 각속도 계산 (도/초)
                val azimuthRate = azimuthDiff / timeDiff
                val elevationRate = elevationDiff / timeDiff

                rates.add(Pair(azimuthRate, elevationRate))

                maxAzimuthRate = Math.max(maxAzimuthRate, Math.abs(azimuthRate))
                maxElevationRate = Math.max(maxElevationRate, Math.abs(elevationRate))
            }

            // 가속도 계산
            for (i in 1 until rates.size) {
                val prev = rates[i-1]
                val curr = rates[i]
                val timeDiff = Duration.between(periodData[i].time, periodData[i+1].time).toMillis() / 1000.0

                val azimuthAccel = (curr.first - prev.first) / timeDiff
                val elevationAccel = (curr.second - prev.second) / timeDiff

                maxAzimuthAccel = Math.max(maxAzimuthAccel, Math.abs(azimuthAccel))
                maxElevationAccel = Math.max(maxElevationAccel, Math.abs(elevationAccel))
            }

            result.add(VisibilityPeriod(
                startTime = period.startTime,
                endTime = period.endTime,
                maxElevation = period.maxElevation,
                maxElevationTime = period.maxElevationTime,
                maxAzimuthRate = maxAzimuthRate,
                maxElevationRate = maxElevationRate,
                maxAzimuthAccel = maxAzimuthAccel,
                maxElevationAccel = maxElevationAccel
            ))
        }

        return result
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
    fun calculateVisibilityPeriodsWithMaxElevation(
        tleLine1: String,
        tleLine2: String,
        startTime: ZonedDateTime,
        durationDays: Int = 1,
        minElevation: Float = 0.0f,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        timeStepMs: Int = 100 // 시간 간격을 파라미터로 받음
    ): List<VisibilityPeriod> {
        val endTime = startTime.plusDays(durationDays.toLong())
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

            // 시간 간격 설정 (밀리초 단위)
            val timeStep = timeStepMs / 1000.0

            // 시간 범위 설정
            var currentDate = startDate
            var currentTime = startTime

            while (currentDate.compareTo(endDate) < 0) {
                // 위성 위치 계산
                val pv = propagator.getPVCoordinates(currentDate, stationFrame)
                val position = pv.position

                // 방위각과 고도각 계산
                val azimuth = Math.toDegrees(Math.atan2(position.y, position.x))
                val elevation =
                    Math.toDegrees(Math.atan2(position.z, Math.sqrt(position.x * position.x + position.y * position.y)))

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
                        if (visibilityStart != null && elevation >= minElevation) {
                            maxAzimuthAccel = Math.max(maxAzimuthAccel, Math.abs(azimuthAccel))
                            maxElevationAccel = Math.max(maxElevationAccel, Math.abs(elevationAccel))
                        }
                    }

                    // 현재 가시성 기간 내에서만 최대 속도 업데이트
                    if (visibilityStart != null && elevation >= minElevation) {
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

                // 가시성 상태 변경 처리
                if (elevation >= minElevation) {
                    // 가시성 시작
                    if (visibilityStart == null) {
                        visibilityStart = currentTime
                        maxElevationInPass = elevation
                        maxElevationTime = currentTime

                        // 새로운 가시성 기간이 시작될 때 최대값 초기화
                        maxAzimuthRate = 0.0
                        maxElevationRate = 0.0
                        maxAzimuthAccel = 0.0
                        maxElevationAccel = 0.0
                    }

                    // 최대 고도각 업데이트
                    if (elevation > maxElevationInPass) {
                        maxElevationInPass = elevation
                        maxElevationTime = currentTime
                    }
                } else {
                    // 가시성 종료
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
                        visibilityStart = null
                        maxElevationInPass = -90.0
                        maxElevationTime = null
                    }
                }

                // 다음 시간으로 이동
                currentDate = currentDate.shiftedBy(timeStep)
                currentTime = currentTime.plusNanos((timeStep * 1e9).toLong())
            }

            // 마지막 가시성 기간이 종료되지 않은 경우 처리
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

            return visibilityPeriods
        } catch (e: Exception) {
            logger.error("위성 가시성 기간 계산 중 오류 발생: ${e.message}", e)
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
}
