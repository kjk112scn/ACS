package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.SatelliteTrackingData
import jakarta.annotation.PostConstruct
import org.orekit.propagation.analytical.tle.TLE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.Int
import kotlin.collections.fold
import kotlin.collections.forEachIndexed
import kotlin.collections.isNotEmpty

/**
 * 위성 추적 서비스
 * 위성의 위치를 계산하고 추적 정보를 제공합니다.
 */
@Service
class SatelliteTrackService(private val orekitCalculator: OrekitCalculator) {

    // 밀리초를 포함하는 사용자 정의 포맷터 생성
    private val timeFormatterWithMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    private val timeOnlyFormatterWithMillis = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val logger = LoggerFactory.getLogger(javaClass)

    // 위성 TLE 데이터 캐시
    private val satelliteTleCache = ConcurrentHashMap<String, Pair<String, String>>()
    private val trackingData= SatelliteTrackingData.Tracking
    private val locationData= GlobalData.Location

    @PostConstruct
    fun init() {
        satelliteTest()
    }

    fun satelliteTest() {
        try {
            val aquaId = "AQUA"
            val aquaTleLine1 = "1 27424U 02022A   25134.85411318  .00000946  00000-0  20168-3 0  9990"
            val aquaTleLine2 = "2 27424  98.3761  92.7913 0001892 100.4476 287.5765 14.61239902225127"
            addSatelliteTle(aquaId, aquaTleLine1, aquaTleLine2)
            // 위성 추적 스케줄 생성
            val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
                tleLine1 = aquaTleLine1,
                tleLine2 = aquaTleLine2,
                startDate = trackingData.startDate,
                durationDays = trackingData.durationDays.toInt(),
                minElevation = trackingData.minElevationAngle,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                altitude = locationData.altitude,
                trackingIntervalMs = trackingData.msInterval
            )
            logger.info("위성 추적 스케줄 생성 완료: 총 ${schedule.trackingPasses.size}개 패스")

            // 3. 추적 데이터 파일로 저장
            val outputDir = "tracking_data/aqua_test"
            val filePrefix = "AQUA_20250514"

            logger.info("추적 데이터 파일 저장 시작: 출력 디렉토리=${outputDir}")

            val savedFiles = orekitCalculator.saveAllTrackingData(schedule, outputDir, filePrefix)

            logger.info("추적 데이터 파일 저장 완료: 총 ${savedFiles.size}개 파일 생성")
            logger.info("생성된 파일 목록:")
            savedFiles.forEach { filePath ->
                logger.info("- $filePath")
            }/*

            // 특정 패스의 세부 데이터 출력 (예: 첫 번째 패스)
            orekitCalculator.printDetailedTrackingData(0, schedule)

            // 모든 패스의 세부 데이터 출력
            orekitCalculator.printAllDetailedTrackingData(schedule)

            // 특정 패스의 세부 데이터를 CSV 파일로 저장
            orekitCalculator.saveDetailedTrackingDataToCsv(0, schedule, "pass1_tracking_data.csv")
            logger.info("종료")

*/

            // 예제 TLE 데이터 추가 (AQUA 위성의 TLE 예시)

/*

            logger.info("AQUA TLE 데이터:")
            logger.info(aquaTleLine1)
            logger.info(aquaTleLine2)

            // TLE 데이터를 캐시에 추가
            addSatelliteTle(aquaId, aquaTleLine1, aquaTleLine2)
            logger.info("등록된 위성 : ${getAllSatelliteIds()}")
            // 위성 추적 스케줄 출력
             printSatelliteTrackingSchedule("AQUA", SatelliteTrackingData.Tracking.startDate, SatelliteTrackingData.Tracking.durationDays,
                  SatelliteTrackingData.Tracking.minElevationAngle, SatelliteTrackingData.Tracking.msInterval,
                  SatelliteTrackingData.Location.latitude, SatelliteTrackingData.Location.longitude, SatelliteTrackingData.Location.altitude)

*/



        } catch (e: Exception) {
            logger.error("satellite_Test 실행 중 오류 발생: ${e.message}", e)
        }
    }

    fun PrintConsoleSatelliteTrackeing()
    {
        val aquaId = "AQUA"
        val aquaTleLine1 = "1 27424U 02022A   25133.82570022  .00001016  00000-0  21586-3 0  9998"
        val aquaTleLine2 = "2 27424  98.3761  91.7503 0001869 101.5252 279.7693 14.61237975224972"
        addSatelliteTle(aquaId, aquaTleLine1, aquaTleLine2)
        logger.info("등록된 위성 : ${getAllSatelliteIds()}")
        // 위성 추적 스케줄 출력
        printSatelliteTrackingSchedule("AQUA", SatelliteTrackingData.Tracking.startDate, SatelliteTrackingData.Tracking.durationDays,
            SatelliteTrackingData.Tracking.minElevationAngle, SatelliteTrackingData.Tracking.msInterval,
            SatelliteTrackingData.Location.latitude, SatelliteTrackingData.Location.longitude, SatelliteTrackingData.Location.altitude)
    }
    fun SaveCSvFileSatelliteTrack()
    {
        val aquaId = "AQUA"
        val aquaTleLine1 = "1 27424U 02022A   25133.82570022  .00001016  00000-0  21586-3 0  9998"
        val aquaTleLine2 = "2 27424  98.3761  91.7503 0001869 101.5252 279.7693 14.61237975224972"
        addSatelliteTle(aquaId, aquaTleLine1, aquaTleLine2)
        // 위성 추적 스케줄 생성
        val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
            tleLine1 = aquaTleLine1,
            tleLine2 = aquaTleLine2,
            startDate = trackingData.startDate,
            durationDays = trackingData.durationDays.toInt(),
            minElevation = trackingData.minElevationAngle,
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            altitude = locationData.altitude,
            trackingIntervalMs = trackingData.msInterval
        )
        logger.info("위성 추적 스케줄 생성 완료: 총 ${schedule.trackingPasses.size}개 패스")

        // 3. 추적 데이터 파일로 저장
        val outputDir = "tracking_data/aqua_test"
        val filePrefix = "AQUA_20250514"

        logger.info("추적 데이터 파일 저장 시작: 출력 디렉토리=${outputDir}")

        val savedFiles = orekitCalculator.saveAllTrackingData(schedule, outputDir, filePrefix)

        logger.info("추적 데이터 파일 저장 완료: 총 ${savedFiles.size}개 파일 생성")
        logger.info("생성된 파일 목록:")
        savedFiles.forEach { filePath ->
            logger.info("- $filePath")
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

    /**
     * 특정 위성의 추적 스케줄을 계산하고 로그로 출력합니다.
     */
    fun printSatelliteTrackingSchedule(
        satelliteId: String,
        startDate: ZonedDateTime = ZonedDateTime.now(),
        durationDays: Long = 1,
        minElevation: Float = 0.0f,
        timeStepMs: Int = 100,
        latitude: Double = GlobalData.Location.latitude,
        longitude: Double = GlobalData.Location.longitude,
        altitude: Double = GlobalData.Location.altitude
    ) {
        val totalStartTime = System.currentTimeMillis()

        try {
            // 1. TLE 데이터 가져오기
            val tleStartTime = System.currentTimeMillis()
            val tle = getSatelliteTle(satelliteId)
            val tleEndTime = System.currentTimeMillis()

            if (tle == null) {
                logger.error("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")
                return
            }

            logger.info("위성 TLE 데이터 검색 시간: ${tleEndTime - tleStartTime}ms")

            // 2. TLE 객체 생성 (파싱)
            val tleParseStartTime = System.currentTimeMillis()
            val tleObj = TLE(tle.first, tle.second)
            val tleParseEndTime = System.currentTimeMillis()
            logger.info("TLE 파싱 시간: ${tleParseEndTime - tleParseStartTime}ms")

            // 3. 병렬 계산 시작
            val parallelStartTime = System.currentTimeMillis()

            // 3.1 궤도 요소, 고도, 속도 계산을 병렬로 수행
            val orbitalElementsFuture = CompletableFuture.supplyAsync {
                val startTime = System.currentTimeMillis()
                val result = calculateOrbitalElements(satelliteId).block()
                val endTime = System.currentTimeMillis()
                Pair(result, endTime - startTime)
            }

            val altitudeFuture = CompletableFuture.supplyAsync {
                val startTime = System.currentTimeMillis()
                val result = calculateSatelliteAltitude(satelliteId).block()
                val endTime = System.currentTimeMillis()
                Pair(result, endTime - startTime)
            }

            val velocityFuture = CompletableFuture.supplyAsync {
                val startTime = System.currentTimeMillis()
                val result = calculateSatelliteVelocity(satelliteId).block()
                val endTime = System.currentTimeMillis()
                Pair(result, endTime - startTime)
            }

            // 3.2 병렬 계산 결과 가져오기
            val (orbitalElements, orbitalElementsTime) = orbitalElementsFuture.get()
            val (satelliteAltitude, altitudeTime) = altitudeFuture.get()
            val (velocity, velocityTime) = velocityFuture.get()


            // 병렬 계산 종료 시간 기록
            val parallelEndTime = System.currentTimeMillis()
            val parallelTotalTime = parallelEndTime - parallelStartTime
            logger.info("병렬 계산 총 소요 시간: ${parallelTotalTime}ms")
            // 4. 위성 가시성 기간 계산 (최적화된 방식 사용)
            val visibilityStartTime = System.currentTimeMillis()

            // 개선된 OrekitCalculator의 최적화된 메서드 사용
            val visibilityPeriods = orekitCalculator.calculateVisibilityPeriodsOptimized(
                tleObj, startDate, durationDays, minElevation, latitude, longitude, altitude
            )

            val visibilityEndTime = System.currentTimeMillis()
            logger.info("위성 가시성 기간 계산 시간: ${(visibilityEndTime - visibilityStartTime)/1000}초 (${visibilityEndTime - visibilityStartTime}ms)")

            // 5. 결과 출력
            val outputStartTime = System.currentTimeMillis()

            // 로그 버퍼링을 위한 StringBuilder 사용
            val logBuffer = StringBuilder()

            // 5.1 기본 정보 출력
            logBuffer.appendLine("위성 ID: $satelliteId")
            logBuffer.appendLine("TLE 데이터:")
            logBuffer.appendLine(tle.first)
            logBuffer.appendLine(tle.second)
            logBuffer.appendLine()

            // 5.2 궤도 요소 출력
            if (orbitalElements != null) {
                logBuffer.appendLine("위성 궤도 요소:")
                logBuffer.appendLine("- 경사각(Inclination): ${String.format("%.2f", orbitalElements["inclination"] ?: 0.0)}°")
                logBuffer.appendLine("- 승교점 적경(RAAN): ${String.format("%.2f", orbitalElements["rightAscension"] ?: 0.0)}°")
                logBuffer.appendLine("- 이심률(Eccentricity): ${String.format("%.6f", orbitalElements["eccentricity"] ?: 0.0)}")
                logBuffer.appendLine("- 근지점 인수(Arg of Perigee): ${String.format("%.2f", orbitalElements["argumentOfPerigee"] ?: 0.0)}°")
                logBuffer.appendLine("- 평균 근점 이각(Mean Anomaly): ${String.format("%.2f", orbitalElements["meanAnomaly"] ?: 0.0)}°")
                logBuffer.appendLine("- 평균 운동(Mean Motion): ${String.format("%.6f", orbitalElements["meanMotion"] ?: 0.0)} rev/day")
                logBuffer.appendLine()
            }

            // 5.3 위성 고도 및 속도 출력
            logBuffer.appendLine("위성 고도: ${String.format("%.2f", satelliteAltitude ?: 0.0)} km")
            logBuffer.appendLine("위성 속도: ${String.format("%.2f", velocity ?: 0.0)} km/s")
            logBuffer.appendLine()

            // 5.4 가시성 기간 정보 출력
            if (visibilityPeriods.isNotEmpty()) {
                // 총 가시 시간 계산
                val totalDuration = visibilityPeriods.fold(Duration.ZERO) { acc, period -> acc.plus(period.duration) }
                val hours = totalDuration.toHours()
                val minutes = totalDuration.toMinutesPart()
                val seconds = totalDuration.toSecondsPart()
                val millis = totalDuration.toMillisPart()

                logBuffer.appendLine("가시성 기간 정보:")
                logBuffer.appendLine("- 총 패스 수: ${visibilityPeriods.size}")
                logBuffer.appendLine("- 총 가시 시간: ${String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)}")
                logBuffer.appendLine()

                // 날짜/시간 포맷터
                val timeFormatterWithMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                val timeOnlyFormatterWithMillis = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

                // 상세 정보를 표시할 최대 패스 수 (너무 많으면 로그가 너무 길어짐)
                val maxDetailedPasses = 10

                // 테이블 헤더 출력
                logBuffer.appendLine("┌─────┬────────────────────┬────────────────────┬──────────┬─────────────┬───────────┬───────────┬───────────┬───────────┐")
                logBuffer.appendLine("│ 번호│      시작 시간     │      종료 시간     │ 지속시간 │ 최대고도각  │ 최대Az속도│ 최대El속도│ 최대Az가속│ 최대El가속│")
                logBuffer.appendLine("├─────┼────────────────────┼────────────────────┼──────────┼─────────────┼───────────┼───────────┼───────────┼───────────┤")

                // 상세 정보 출력 (최대 maxDetailedPasses개까지)
                visibilityPeriods.take(maxDetailedPasses).forEachIndexed { index, period ->
                    val durationStr = String.format(
                        "%02d:%02d:%02d",
                        period.duration.toHours(),
                        period.duration.toMinutesPart(),
                        period.duration.toSecondsPart()
                    )

                    val maxElevationStr = String.format("%.2f°", period.maxElevation)
                    val maxAzRateStr = String.format("%.2f°/s", period.maxAzimuthRate)
                    val maxElRateStr = String.format("%.2f°/s", period.maxElevationRate)
                    val maxAzAccelStr = String.format("%.2f°/s²", period.maxAzimuthAccel)
                    val maxElAccelStr = String.format("%.2f°/s²", period.maxElevationAccel)

                    logBuffer.appendLine("│ ${String.format("%3d", index + 1)} │ ${period.startTime.format(timeFormatterWithMillis)} │ ${period.endTime.format(timeFormatterWithMillis)} │ ${durationStr} │ ${maxElevationStr.padEnd(11)} │ ${maxAzRateStr.padEnd(9)} │ ${maxElRateStr.padEnd(9)} │ ${maxAzAccelStr.padEnd(9)} │ ${maxElAccelStr.padEnd(9)} │")
                }
                logBuffer.appendLine("└─────┴────────────────────┴────────────────────┴──────────┴─────────────┴───────────┴───────────┴───────────┴───────────┘")

                // 나머지 패스에 대한 요약 정보
                if (visibilityPeriods.size > maxDetailedPasses) {
                    logBuffer.appendLine()
                    logBuffer.appendLine("나머지 ${visibilityPeriods.size - maxDetailedPasses}개 패스의 요약 정보:")

                    // 최대 고도각 기준으로 정렬하여 중요한 패스 정보 제공
                    val remainingPasses = visibilityPeriods.drop(maxDetailedPasses)
                        .sortedByDescending { it.maxElevation }
                        .take(5) // 상위 5개만 표시

                    logBuffer.appendLine("┌─────┬────────────────────┬──────────┬─────────────┐")
                    logBuffer.appendLine("│ 순위│   최대고도각 시간  │ 지속시간 │ 최대고도각  │")
                    logBuffer.appendLine("├─────┼────────────────────┼──────────┼─────────────┤")

                    remainingPasses.forEachIndexed { index, period ->
                        val durationStr = String.format(
                            "%02d:%02d:%02d",
                            period.duration.toHours(),
                            period.duration.toMinutesPart(),
                            period.duration.toSecondsPart()
                        )

                        val maxElevationStr = String.format("%.2f°", period.maxElevation)

                        logBuffer.appendLine("│ ${String.format("%3d", index + 1)} │ ${period.maxElevationTime?.format(timeFormatterWithMillis) ?: "N/A".padEnd(20)} │ ${durationStr} │ ${maxElevationStr.padEnd(11)} │")
                    }
                    logBuffer.appendLine("└─────┴────────────────────┴──────────┴─────────────┘")
                }
            } else {
                logBuffer.appendLine("지정된 기간 동안 위성 가시성 기간이 없습니다.")
            }

            // 로그 출력 (한 번에 출력하여 I/O 최소화)
            logger.info(logBuffer.toString())

            val outputEndTime = System.currentTimeMillis()

            // 6. 성능 요약 정보
            val totalEndTime = System.currentTimeMillis()
            logger.info("위성 ID: $satelliteId 의 추적 스케줄 계산 완료 (총 소요 시간: ${totalEndTime - totalStartTime}ms)")

            // 성능 요약 정보 (시각화 개선)
            val performanceData = listOf(
                Pair("TLE 데이터 검색", tleEndTime - tleStartTime),
                Pair("TLE 파싱", tleParseEndTime - tleParseStartTime),
                Pair("궤도 요소 계산", orbitalElementsTime),
                Pair("위성 고도 계산", altitudeTime),
                Pair("위성 속도 계산", velocityTime),
                Pair("가시성 기간 계산", visibilityEndTime - visibilityStartTime),
                Pair("결과 출력", outputEndTime - outputStartTime)
            )

            val totalTime = totalEndTime - totalStartTime

            logger.info("성능 요약:")
            logger.info("┌───────────────────┬──────────┬───────┐")
            logger.info("│       작업        │   시간   │  비율 │")
            logger.info("├───────────────────┼──────────┼───────┤")

            performanceData.forEach { (name, time) ->
                val percentage = ((time * 100.0) / totalTime).toInt()
                logger.info("│ ${name.padEnd(17)} │ ${String.format("%6d", time)}ms │ ${String.format("%3d", percentage)}% │")
            }

            logger.info("└───────────────────┴──────────┴───────┘")

        } catch (e: Exception) {
            val totalEndTime = System.currentTimeMillis()
            logger.error("위성 추적 스케줄 계산 중 오류 발생: ${e.message}", e)
            logger.info("오류 발생까지 소요 시간: ${totalEndTime - totalStartTime}ms")
        }
    }
    /**
     * 위성 궤도 요소를 계산합니다.
     */
    fun calculateOrbitalElements(satelliteId: String): Mono<Map<String, Double>> {
        return Mono.fromCallable {
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")

            try {
                // TLE 문자열 로깅
                logger.debug("TLE Line 1: ${tle.first}")
                logger.debug("TLE Line 2: ${tle.second}")

                // TLE 라인 2를 정규식으로 분리 (연속된 공백을 하나의 구분자로 처리)
                val parts = tle.second.trim().split("\\s+".toRegex())

                // 분리된 부분 로깅
                logger.debug("TLE Line 2 parts: ${parts.joinToString(", ")}")

                // 인덱스 범위 확인 및 안전한 변환
                if (parts.size < 8) {
                    throw IllegalArgumentException("TLE 라인 2의 형식이 올바르지 않습니다: ${tle.second}")
                }

                // 각 요소 추출 및 변환 시 예외 처리
                val inclination = parts[2].toDoubleOrNull() ?: 0.0
                val rightAscension = parts[3].toDoubleOrNull() ?: 0.0
                val eccentricity = ("0." + parts[4]).toDoubleOrNull() ?: 0.0
                val argumentOfPerigee = parts[5].toDoubleOrNull() ?: 0.0
                val meanAnomaly = parts[6].toDoubleOrNull() ?: 0.0

                // 평균 운동 추출 (인덱스 범위 확인)
                val meanMotion = if (parts.size > 7) {
                    parts[7].toDoubleOrNull() ?: 0.0
                } else {
                    0.0
                }

                mapOf(
                    "inclination" to inclination,
                    "rightAscension" to rightAscension,
                    "eccentricity" to eccentricity,
                    "argumentOfPerigee" to argumentOfPerigee,
                    "meanAnomaly" to meanAnomaly,
                    "meanMotion" to meanMotion
                )
            } catch (e: Exception) {
                logger.error("궤도 요소 계산 중 오류 발생: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * 위성 고도를 계산합니다.
     */
    fun calculateSatelliteAltitude(satelliteId: String): Mono<Double> {
        return Mono.fromCallable {
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")

            try {
                // TLE 문자열 파싱 전에 로그 출력
                logger.debug("TLE Line 1: ${tle.first}")
                logger.debug("TLE Line 2: ${tle.second}")

                // TLE 라인 2를 공백으로 분리
                val parts = tle.second.trim().split("\\s+".toRegex())

                // 분리된 부분 로깅
                logger.debug("TLE Line 2 parts: ${parts.joinToString(", ")}")

                // 평균 운동 추출
                val meanMotion = parts.getOrNull(7)?.toDoubleOrNull() ?: 0.0

                // 평균 운동은 일일 회전 수이므로, 주기(초)로 변환
                val periodInSeconds = 86400.0 / meanMotion

                // 케플러 제3법칙: a^3 / T^2 = GM / (4π^2)
                // 여기서 a는 궤도 반경, T는 주기, GM은 지구 중력 상수
                val GM = 3.986004418e14 // 지구 중력 상수 (m^3/s^2)
                val pi = Math.PI

                // 궤도 반경 계산 (m)
                val orbitRadius = Math.cbrt(GM * Math.pow(periodInSeconds / (2 * pi), 2.0))

                // 지구 반경 (m)
                val earthRadius = 6371000.0

                // 고도 계산 (m)
                val altitude = orbitRadius - earthRadius

                // 킬로미터로 변환하여 반환
                altitude / 1000.0
            } catch (e: Exception) {
                logger.error("위성 고도 계산 중 오류 발생: ${e.message}", e)
                throw e
            }
        }
    }
    /**
     * 위성 속도를 계산합니다.
     */
    fun calculateSatelliteVelocity(satelliteId: String): Mono<Double> {
        return Mono.fromCallable {
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")

            try {
                // TLE 문자열 파싱
                logger.debug("TLE Line 1: ${tle.first}")
                logger.debug("TLE Line 2: ${tle.second}")

                // TLE 라인 2를 공백으로 분리
                val parts = tle.second.trim().split("\\s+".toRegex())

                // 평균 운동 추출 (일일 회전 수)
                val meanMotion = parts.getOrNull(7)?.toDoubleOrNull() ?: 0.0

                // 평균 운동은 일일 회전 수이므로, 주기(초)로 변환
                val periodInSeconds = 86400.0 / meanMotion

                // 케플러 제3법칙: a^3 / T^2 = GM / (4π^2)
                // 여기서 a는 궤도 반경, T는 주기, GM은 지구 중력 상수
                val GM = 3.986004418e14 // 지구 중력 상수 (m^3/s^2)
                val pi = Math.PI

                // 궤도 반경 계산 (m)
                val orbitRadius = Math.cbrt(GM * Math.pow(periodInSeconds / (2 * pi), 2.0))

                // 원형 궤도 가정하에 속도 계산 (m/s)
                val velocity = 2 * pi * orbitRadius / periodInSeconds

                // 킬로미터/초로 변환하여 반환
                velocity / 1000.0
            } catch (e: Exception) {
                logger.error("위성 속도 계산 중 오류 발생: ${e.message}", e)
                throw e
            }
        }
    }
}

