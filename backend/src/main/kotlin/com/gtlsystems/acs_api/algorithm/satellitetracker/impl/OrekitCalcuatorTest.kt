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
*/

}