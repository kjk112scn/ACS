package com.gtlsystems.acs_api.algorithm.satellitetracker.impl

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator.VisibilityPeriod
import java.util.concurrent.Future
import com.gtlsystems.acs_api.algorithm.satellitetracker.interfaces.SatellitePositionCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import org.hipparchus.ode.events.Action
import org.orekit.propagation.SpacecraftState
import org.orekit.propagation.events.EventDetector
import org.orekit.propagation.events.handlers.EventHandler
import org.orekit.time.AbsoluteDate
import org.orekit.propagation.events.ElevationDetector
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
import java.time.ZoneOffset
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
                writer.write("- 궤도 주기: ${String.format("%.2f", periodMinutes)} 분 (${String.format("%.2f", periodHours)} 시간)\n")

                // 근지점 및 원지점 고도 계산 (대략적인 계산)
                val earthRadius = 6378.137 // 지구 적도 반경 (km)
                val semiMajorAxis = (earthRadius + 42164.0) * Math.pow(24.0 / periodHours, 2.0/3.0) // 정지궤도 고도 기준 계산

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
                writer.write("기간: ${schedule.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} ~ ${schedule.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}\n")
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

                    writer.write(String.format("│ %3d │ %s │ %s │ %s │ %-11s │ %-9s │ %-9s │ %-9s │ %-9s │\n",
                        passNumber,
                        pass.startTime.format(formatter),
                        pass.endTime.format(formatter),
                        pass.getDurationString(),
                        maxElevation,
                        maxAzRateStr,
                        maxElRateStr,
                        maxAzAccelStr,
                        maxElAccelStr
                    ))
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
                    writer.write("- 최대 고도각: ${String.format("%.2f", pass.maxElevation)}° (${pass.maxElevationTime?.format(DateTimeFormatter.ISO_LOCAL_TIME) ?: "N/A"})\n")
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

                        writer.write("- 첫 데이터 포인트: 방위각=${String.format("%.2f", firstPoint.azimuth)}°, 고도각=${String.format("%.2f", firstPoint.elevation)}°\n")
                        writer.write("- 마지막 데이터 포인트: 방위각=${String.format("%.2f", lastPoint.azimuth)}°, 고도각=${String.format("%.2f", lastPoint.elevation)}°\n")
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
    // 함수 내에 로깅 추가
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

        try {
            // 1. 먼저 가시성 기간을 계산 (시간 간격을 늘려 빠르게 계산)
            logger.info("가시성 기간 계산 시작...")
            val visibilityStartTime = System.currentTimeMillis()
            val visibilityPeriods = calculateVisibilityPeriodsWithMaxElevation(
                tleLine1, tleLine2, startDate, durationDays, minElevation,
                latitude, longitude, altitude, 1000 // 1초 간격으로 변경
            )
            val visibilityEndTime = System.currentTimeMillis()
            val visibilityDuration = visibilityEndTime - visibilityStartTime
            logger.info("가시성 기간 계산 완료: ${visibilityPeriods.size}개 기간 발견 (소요 시간: ${visibilityDuration}ms)")

            // 2. 각 가시성 기간에 대해 상세 추적 데이터 생성
            logger.info("상세 추적 데이터 생성 시작...")
            val trackingPasses = visibilityPeriods.mapIndexed { index, period ->
                logger.info("패스 ${index + 1}/${visibilityPeriods.size} 처리 중...")
                // 각 가시성 기간에 대한 상세 추적 데이터 생성
                val detailedTrackingData = generateDetailedTrackingData(
                    tleLine1, tleLine2, period.startTime, period.endTime,
                    trackingIntervalMs, latitude, longitude, altitude
                )
                logger.info("패스 ${index + 1} 데이터 생성 완료: ${detailedTrackingData.size}개 포인트")

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
                    maxElevationAccel = period.maxElevationAccel
                )
            }
            logger.info("상세 추적 데이터 생성 완료")

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
            throw e
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
        val trackingData: List<SatelliteTrackData>,
        val maxAzimuthRate: Double = 0.0,         // 최대 방위각 속도 (도/초)
        val maxElevationRate: Double = 0.0,       // 최대 고도각 속도 (도/초)
        val maxAzimuthAccel: Double = 0.0,        // 최대 방위각 가속도 (도/초²)
        val maxElevationAccel: Double = 0.0       // 최대 고도각 가속도 (도/초²)
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
                        // 로그 추가 - 각속도 및 각가속도 정보 출력
                        logger.debug("패스 정보: 시작=${visibilityStart}, 종료=${currentTime}, 최대고도각=${maxElevationInPass}°")
                        logger.debug("최대 각속도: 방위각=${maxAzimuthRate}°/s, 고도각=${maxElevationRate}°/s")
                        logger.debug("최대 각가속도: 방위각=${maxAzimuthAccel}°/s², 고도각=${maxElevationAccel}°/s²")
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
                // 로그 추가 - 마지막 패스의 각속도 및 각가속도 정보 출력
                logger.debug("마지막 패스 정보: 시작=${visibilityStart}, 종료=${currentTime}, 최대고도각=${maxElevationInPass}°")
                logger.debug("최대 각속도: 방위각=${maxAzimuthRate}°/s, 고도각=${maxElevationRate}°/s")
                logger.debug("최대 각가속도: 방위각=${maxAzimuthAccel}°/s², 고도각=${maxElevationAccel}°/s²")
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
            logger.info("총 ${visibilityPeriods.size}개의 패스가 계산되었습니다.")
            if (visibilityPeriods.isNotEmpty()) {
                val maxAzRate = visibilityPeriods.maxOf { it.maxAzimuthRate }
                val maxElRate = visibilityPeriods.maxOf { it.maxElevationRate }
                val maxAzAccel = visibilityPeriods.maxOf { it.maxAzimuthAccel }
                val maxElAccel = visibilityPeriods.maxOf { it.maxElevationAccel }

                logger.info("전체 패스 중 최대 각속도: 방위각=${maxAzRate}°/s, 고도각=${maxElRate}°/s")
                logger.info("전체 패스 중 최대 각가속도: 방위각=${maxAzAccel}°/s², 고도각=${maxElAccel}°/s²")
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
}
/*


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
     * EventDetector를 사용하여 위성 가시성 기간을 빠르게 계산합니다.
     */
fun calculateVisibilityPeriodsWithEventDetector(
    tle: TLE,
    startTime: ZonedDateTime,
    durationDays: Long = 1,
    minElevation: Float = 0.0f,
    latitude: Double,
    longitude: Double,
    altitude: Double = 0.0
): List<VisibilityPeriod> {
    val visibilityPeriods = mutableListOf<VisibilityPeriod>()

    try {
        // 시작 및 종료 시간 설정
        val startDate = toAbsoluteDate(startTime)
        val endDate = toAbsoluteDate(startTime.plusDays(durationDays.toLong()))

        // 지상국 위치 설정
        val stationPosition = GeodeticPoint(
            FastMath.toRadians(latitude),
            FastMath.toRadians(longitude),
            altitude
        )
        val stationFrame = TopocentricFrame(earth, stationPosition, "GroundStation")

        // TLE 전파기 생성
        val propagator = TLEPropagator.selectExtrapolator(tle)

        // 가시성 이벤트 감지기 생성 (최소 고도각 기준)
        val minElevationRad = FastMath.toRadians(minElevation.toDouble())
        val elevationDetector = ElevationDetector(stationFrame)
            .withConstantElevation(minElevationRad)
            .withHandler(VisibilityHandler(visibilityPeriods, stationFrame, tle))

        // 이벤트 감지기 등록
        propagator.addEventDetector(elevationDetector)

        // 전체 기간에 대해 한 번에 전파 (이벤트 감지)
        propagator.propagate(startDate, endDate)

        return visibilityPeriods
    } catch (e: Exception) {
        logger.error("위성 가시성 기간 계산 중 오류 발생: ${e.message}", e)
        throw e
    }
}

    /**
     * 가시성 이벤트 처리를 위한 핸들러
     */
    private inner class VisibilityHandler(
        private val visibilityPeriods: MutableList<VisibilityPeriod>,
        private val stationFrame: TopocentricFrame,
        private val tle: TLE
    ) : EventHandler {
        private var visibilityStart: AbsoluteDate? = null
        private var maxElevation: Double = -90.0
        private var maxElevationTime: AbsoluteDate? = null

        override fun init(initialState: SpacecraftState, target: AbsoluteDate, detector: EventDetector) {
            // 초기화 코드
        }

        override fun eventOccurred(s: SpacecraftState, detector: EventDetector, increasing: Boolean): Action {
            val currentDate = s.date

            if (increasing) {
                // 위성이 지평선 위로 올라옴 (가시성 시작)
                visibilityStart = currentDate
                maxElevation = -90.0
                maxElevationTime = null
            } else {
                // 위성이 지평선 아래로 내려감 (가시성 종료)
                val startTime = visibilityStart
                if (startTime != null) {
                    // findMaxElevation 함수를 사용하여 최대 고도각과 그 시간 찾기

                    val result = findMaxElevation(
                        TLEPropagator.selectExtrapolator(tle),
                        stationFrame,
                        startTime,
                        currentDate
                    )
                    // 최대 고도각 및 시간 업데이트
                    maxElevation = result.first
                    maxElevationTime = result.second
                    // 가시성 기간 정보 생성
                    val zonedStartTime = toZonedDateTime(startTime)
                    logger.info("zonedStartTime: ${zonedStartTime}")
                    val zonedEndTime = toZonedDateTime(currentDate)
                    logger.info("zonedEndTime: ${zonedEndTime}")
                    // 최대 고도각 시간이 없는 경우 (드문 경우) 중간 시간으로 설정
                    val maxElTime = maxElevationTime ?: startTime.shiftedBy((currentDate.durationFrom(startTime)) / 2)

                    visibilityPeriods.add(
                        VisibilityPeriod(
                            startTime = zonedStartTime,
                            endTime = zonedEndTime,
                            maxElevation = maxElevation,
                            maxElevationTime = toZonedDateTime(maxElTime)
                        )
                    )

                    visibilityStart = null
                }
            }

            return Action.CONTINUE
        }

        override fun resetState(detector: EventDetector, oldState: SpacecraftState): SpacecraftState {
            return oldState
        }
    }


    /**
     * 이분 탐색을 활용하여 위성 가시성 기간을 빠르게 계산합니다.
     */
    fun calculateVisibilityPeriodsWithBinarySearch(
        tle: TLE,
        startTime: ZonedDateTime,
        durationDays: Long = 1,
        minElevation: Float = 0.0f,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0
    ): List<VisibilityPeriod> {
        val visibilityPeriods = mutableListOf<VisibilityPeriod>()

        try {
            // 시작 및 종료 시간 설정
            val startDate = toAbsoluteDate(startTime)
            val endDate = toAbsoluteDate(startTime.plusDays(durationDays.toLong()))

            // 지상국 위치 설정
            val stationPosition = GeodeticPoint(
                FastMath.toRadians(latitude),
                FastMath.toRadians(longitude),
                altitude
            )
            val stationFrame = TopocentricFrame(earth, stationPosition, "GroundStation")

            // TLE 전파기 생성
            val propagator = TLEPropagator.selectExtrapolator(tle)

            // 1. 먼저 큰 간격으로 샘플링하여 대략적인 가시성 영역 찾기
            val coarseSamplingStep = 5 * 60.0 // 5분 간격
            var currentDate = startDate
            var isVisible = false
            var visibilityStart: AbsoluteDate? = null

            val coarseVisibilityRanges = mutableListOf<Pair<AbsoluteDate, AbsoluteDate>>()

            while (currentDate.compareTo(endDate) < 0) {
                val state = propagator.propagate(currentDate)
                val pv = state.getPVCoordinates(stationFrame)

                val elevation = FastMath.toDegrees(
                    FastMath.atan2(
                        pv.position.z,
                        FastMath.sqrt(pv.position.x * pv.position.x + pv.position.y * pv.position.y)
                    )
                )

                if (elevation >= minElevation && !isVisible) {
                    // 가시성 시작 (대략적인 시간)
                    isVisible = true
                    visibilityStart = currentDate
                } else if (elevation < minElevation && isVisible) {
                    // 가시성 종료 (대략적인 시간)
                    isVisible = false
                    coarseVisibilityRanges.add(Pair(visibilityStart!!, currentDate))
                    visibilityStart = null
                }

                currentDate = currentDate.shiftedBy(coarseSamplingStep)
            }

            // 마지막 가시성 기간 처리
            if (isVisible && visibilityStart != null) {
                coarseVisibilityRanges.add(Pair(visibilityStart, endDate))
            }

            // 2. 각 대략적인 가시성 범위에 대해 이분 탐색으로 정확한 시작/종료 시간 찾기
            for (range in coarseVisibilityRanges) {
                // 정확한 시작 시간 찾기 (이분 탐색)
                val exactStartTime = findExactTransitionTime(
                    propagator, stationFrame, range.first.shiftedBy(-coarseSamplingStep),
                    range.first, minElevation.toDouble(), true
                )

                // 정확한 종료 시간 찾기 (이분 탐색)
                val exactEndTime = findExactTransitionTime(
                    propagator, stationFrame, range.second,
                    range.second.shiftedBy(coarseSamplingStep), minElevation.toDouble(), false
                )

                // 최대 고도각 계산
                val (maxElevation, maxElevationTime) = findMaxElevation(
                    propagator, stationFrame, exactStartTime, exactEndTime
                )

                visibilityPeriods.add(
                    VisibilityPeriod(
                        startTime = toZonedDateTime(exactStartTime),
                        endTime = toZonedDateTime(exactEndTime),
                        maxElevation = maxElevation,
                        maxElevationTime = toZonedDateTime(maxElevationTime)
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
     * 이분 탐색을 사용하여 정확한 전환 시간(가시성 시작/종료)을 찾습니다.
     */
    private fun findExactTransitionTime(
        propagator: TLEPropagator,
        stationFrame: TopocentricFrame,
        lowerBound: AbsoluteDate,
        upperBound: AbsoluteDate,
        minElevation: Double,
        findingStart: Boolean,

        ): AbsoluteDate {
        var low = lowerBound
        var high = upperBound
        val tolerance = 1 // 0.1초 정밀도

        while (high.durationFrom(low) > tolerance) {
            val mid = low.shiftedBy(high.durationFrom(low) / 2)
            val state = propagator.propagate(mid)
            val pv = state.getPVCoordinates(stationFrame)

            val elevation = FastMath.toDegrees(
                FastMath.atan2(
                    pv.position.z,
                    FastMath.sqrt(pv.position.x * pv.position.x + pv.position.y * pv.position.y)
                )
            )

            if ((elevation >= minElevation) == findingStart) {
                high = mid
            } else {
                low = mid
            }
        }

        return high
    }

    /**
     * 가시성 기간 동안 최대 고도각과 그 시간을 찾습니다.
     */
    private fun findMaxElevation(
        propagator: TLEPropagator,
        stationFrame: TopocentricFrame,
        startTime: AbsoluteDate,
        endTime: AbsoluteDate
    ): Pair<Double, AbsoluteDate> {
        // 골든 섹션 서치 알고리즘 사용
        val goldenRatio = (1 + Math.sqrt(5.0)) / 2
        val tolerance = 1.0 // 1초 정밀도

        var a = startTime
        var b = endTime
        var c = b.shiftedBy(-(b.durationFrom(a) / goldenRatio))
        var d = a.shiftedBy(b.durationFrom(a) / goldenRatio)

        // 고도각 계산 함수
        fun calculateElevation(time: AbsoluteDate): Double {
            val state = propagator.propagate(time)
            val pv = state.getPVCoordinates(stationFrame)
            return FastMath.toDegrees(
                FastMath.atan2(
                    pv.position.z,
                    FastMath.sqrt(pv.position.x * pv.position.x + pv.position.y * pv.position.y)
                )
            )
        }

        var fc = calculateElevation(c)
        var fd = calculateElevation(d)

        while (b.durationFrom(a) > tolerance) {
            if (fc < fd) {
                a = c
                c = d
                d = a.shiftedBy(b.durationFrom(a) / goldenRatio)
                fc = fd
                fd = calculateElevation(d)
            } else {
                b = d
                d = c
                c = b.shiftedBy(-(b.durationFrom(a) / goldenRatio))
                fd = fc
                fc = calculateElevation(c)
            }
        }

        // 최종 구간에서 가장 높은 고도각 찾기
        val midTime = a.shiftedBy(b.durationFrom(a) / 2)
        val midElevation = calculateElevation(midTime)

        return Pair(midElevation, midTime)
    }

    /**
     * AbsoluteDate를 ZonedDateTime으로 변환합니다.
     */
    private fun toZonedDateTime(date: AbsoluteDate): ZonedDateTime {
        val utcScale = TimeScalesFactory.getUTC()
        val components = date.getComponents(utcScale)
        return ZonedDateTime.of(
            components.date.year,
            components.date.month,
            components.date.day,
            components.time.hour,
            components.time.minute,
            components.time.second.toInt(),
            (components.time.second * 1_000_000_000 % 1_000_000_000).toInt(),
            ZoneOffset.UTC
        )
    }

    /**
     * 위성 가시성 기간을 빠르게 계산하는 최적화된 함수
     * (Orekit의 내장 기능과 이분 탐색을 결합한 방식)
     */
    fun calculateVisibilityPeriodsOptimized(
        tle: TLE,
        startTime: ZonedDateTime,
        durationDays: Long = 1,
        minElevation: Float = 0.0f,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0
    ): List<VisibilityPeriod> {
        // 기간이 짧은 경우 이벤트 감지기 사용
        if (durationDays <= 3) {
            return calculateVisibilityPeriodsWithEventDetector(
                tle, startTime, durationDays, minElevation, latitude, longitude, altitude
            )
        }

        // 기간이 긴 경우 이분 탐색 방식 사용
        return calculateVisibilityPeriodsWithBinarySearch(
            tle, startTime, durationDays, minElevation, latitude, longitude, altitude
        )
    }

 */

 /*
 /**
     * 특정 패스의 세부 추적 데이터를 출력합니다.
     *
     * @param passIndex 출력할 패스의 인덱스 (0부터 시작)
     * @param schedule 위성 추적 스케줄
     */
    fun printDetailedTrackingData(passIndex: Int, schedule: SatelliteTrackingSchedule) {
        if (passIndex < 0 || passIndex >= schedule.trackingPasses.size) {
            println("유효하지 않은 패스 인덱스입니다. 0부터 ${schedule.trackingPasses.size - 1}까지의 값을 입력하세요.")
            return
        }

        val pass = schedule.trackingPasses[passIndex]
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        println("패스 ${passIndex + 1} 세부 추적 데이터 (${pass.startTime.format(formatter)} ~ ${pass.endTime.format(formatter)}):")
        println("─────────────────────────────────────────────────────────────────────")
        println("│ 시간                    │ 방위각(°)  │ 고도각(°)  │ 거리(km)   │ 고도(km)   │")
        println("─────────────────────────────────────────────────────────────────────")

        pass.trackingData.forEach { data ->
            println(data.timestamp?.let { "│ ${it.format(formatter)} │ ${String.format("%9.2f", data.azimuth)} │ ${String.format("%9.2f", data.elevation)} │ ${String.format("%9.2f", data.range)} │ ${String.format("%9.2f", data.altitude)} │" })
        }

        println("─────────────────────────────────────────────────────────────────────")
        println("총 데이터 포인트: ${pass.trackingData.size}")
    }
    /**
     * 모든 패스의 세부 추적 데이터를 출력합니다.
     *
     * @param schedule 위성 추적 스케줄
     */
    fun printAllDetailedTrackingData(schedule: SatelliteTrackingSchedule) {
        println("위성 추적 세부 데이터 출력")
        println("위성 TLE: ${schedule.satelliteTle1.substring(0, 30)}...")
        println("기간: ${schedule.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} ~ ${schedule.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
        println("지상국 위치: 위도 ${schedule.stationLatitude}°, 경도 ${schedule.stationLongitude}°, 고도 ${schedule.stationAltitude}m")
        println("최소 고도각: ${schedule.minElevation}°")
        println("추적 간격: ${schedule.trackingIntervalMs}ms")
        println("총 패스 수: ${schedule.totalPasses}")
        println()

        for (i in 0 until schedule.trackingPasses.size) {
            printDetailedTrackingData(i, schedule)
            println()
        }
    }
    /**
     * 특정 패스의 세부 추적 데이터를 CSV 파일로 저장합니다.
     *
     * @param passIndex 저장할 패스의 인덱스 (0부터 시작)
     * @param schedule 위성 추적 스케줄
     * @param filePath 저장할 파일 경로
     */
    fun saveDetailedTrackingDataToCsv(passIndex: Int, schedule: SatelliteTrackingSchedule, filePath: String) {
        if (passIndex < 0 || passIndex >= schedule.trackingPasses.size) {
            logger.error("유효하지 않은 패스 인덱스입니다. 0부터 ${schedule.trackingPasses.size - 1}까지의 값을 입력하세요.")
            return
        }

        try {
            val pass = schedule.trackingPasses[passIndex]
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

            File(filePath).bufferedWriter().use { writer ->
                // CSV 헤더
                writer.write("시간,방위각(°),고도각(°),거리(km),고도(km)\n")

                // 데이터 행
                pass.trackingData.forEach { data ->
                    data.timestamp?.let { writer.write("${it.format(formatter)},${data.azimuth},${data.elevation},${data.range},${data.altitude}\n") }
                }
            }

            logger.info("패스 ${passIndex + 1}의 세부 추적 데이터가 ${filePath}에 저장되었습니다.")
        } catch (e: Exception) {
            logger.error("파일 저장 중 오류 발생: ${e.message}", e)
        }
    }


 */
