package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.model.GlobalData.Time.calUtcTimeOffsetTime
import com.gtlsystems.acs_api.model.SatelliteTrackingData
import jakarta.annotation.PostConstruct
import org.orekit.propagation.analytical.tle.TLE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.File
import java.time.Duration
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

    @PostConstruct
    fun init() {
        satellite_Test()
    }

    fun satellite_Test() {
        try {
            // 예제 TLE 데이터 추가 (AQUA 위성의 TLE 예시)
            val aquaId = "AQUA"
            val aquaTleLine1 = "1 27424U 02022A   25133.82570022  .00001016  00000-0  21586-3 0  9998"
            val aquaTleLine2 = "2 27424  98.3761  91.7503 0001869 101.5252 279.7693 14.61237975224972"

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

        } catch (e: Exception) {
            logger.error("satellite_Test 실행 중 오류 발생: ${e.message}", e)
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
    /*

        */
    /**
     * 현재 시간의 위성 위치를 계산합니다.
     *//*

    fun getCurrentPosition(
        satelliteId: String,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Mono<SatelliteTrackData> {
        return Mono.fromCallable {
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")
            orekitCalculator.getCurrentPosition(tle.first, tle.second, latitude, longitude, altitude)
        }
    }

    */
    /**
     * 지정된 시간의 위성 위치를 계산합니다.
     *//*

    fun getPositionAtTime(
        satelliteId: String,
        dateTime: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Mono<SatelliteTrackData> {
        return Mono.fromCallable {
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")
            orekitCalculator.calculatePosition(tle.first, tle.second, dateTime, latitude, longitude, altitude)
        }
    }

    */
    /**
     * 특정 시간 범위 동안의 위성 위치를 계산합니다.
     *//*

    fun getTrackingPath(
        satelliteId: String,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        interval: Int = 1, // 기본값 1분 간격
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Mono<SatelliteTrackData> {
        return Mono.fromCallable {
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")
            orekitCalculator.calculateTrackingPath(
                tle.first,
                tle.second,
                startTime,
                endTime,
                interval,
                latitude,
                longitude,
                altitude
            )
        }
    }
*/

    /*   */
    /**
     * 기본 관측 위치를 설정합니다.
     *//*
    fun setDefaultLocation(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        this.defaultLatitude = latitude
        this.defaultLongitude = longitude
        this.defaultAltitude = altitude
        logger.info("기본 관측 위치가 업데이트되었습니다: 위도 $latitude, 경도 $longitude, 고도 $altitude")
    }

    */
    /**
     * 기본 관측 위치를 반환합니다.
     *//*
    fun getDefaultLocation(): Triple<Double, Double, Double> {
        return Triple(defaultLatitude, defaultLongitude, defaultAltitude)
    }
*/
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
     * 위성 추적 스케줄을 생성합니다.
     *
     * @param satelliteId 위성 ID
     * @param startDate 시작 날짜 (기본값: 현재 시간)
     * @param durationDays 계산할 기간(일) (기본값: 1일)
     * @param minElevation 최소 고도각(도) (기본값: 5도)
     * @param trackingIntervalMs 추적 데이터 간격(밀리초) (기본값: 1000ms)
     * @return 위성 추적 스케줄
     */
    fun generateSatelliteTrackingSchedule(
        satelliteId: String,
        startDate: ZonedDateTime,
        durationDays: Int,
        minElevation: Float,
        trackingIntervalMs: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Mono<OrekitCalculator.SatelliteTrackingSchedule> {
        return Mono.fromCallable {
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")

            orekitCalculator.generateSatelliteTrackingSchedule(
                tle.first, tle.second, startDate, durationDays, minElevation,
                latitude, longitude, altitude, trackingIntervalMs
            )
        }
    }

    /**
     * 위성 추적 스케줄의 요약 정보를 문자열로 반환합니다.
     *
     * @param satelliteId 위성 ID
     * @param startDate 시작 날짜 (기본값: 현재 시간)
     * @param durationDays 계산할 기간(일) (기본값: 1일)
     * @param minElevation 최소 고도각(도) (기본값: 5도)
     * @return 위성 추적 스케줄 요약 정보
     */
    fun getSatelliteTrackingScheduleSummary(
        satelliteId: String,
        startDate: ZonedDateTime = calUtcTimeOffsetTime,
        durationDays: Int,
        minElevation: Float,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Mono<String> {
        return generateSatelliteTrackingSchedule(
            satelliteId,
            startDate,
            durationDays,
            minElevation,
            100,
            latitude,
            longitude,
            altitude
        )
            .map { schedule ->
                val summary = StringBuilder()
                summary.append(schedule.getSummary())
                summary.append("\n\n")

                summary.append("패스 목록:\n")
                schedule.trackingPasses.forEachIndexed { index, pass ->
                    summary.append("${index + 1}. ${pass.getSummary()}\n")
                }

                summary.toString()
            }
    }

    /**
     * 특정 날짜의 위성 추적 스케줄을 생성합니다.
     *
     * @param satelliteId 위성 ID
     * @param date 날짜 (기본값: 오늘)
     * @param minElevation 최소 고도각(도) (기본값: 5도)
     * @return 위성 추적 스케줄
     */
    fun generateDailySatelliteTrackingSchedule(
        satelliteId: String,
        startDate: ZonedDateTime,
        durationDays: Int,
        minElevation: Float,
        trackingIntervalMs: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Mono<OrekitCalculator.SatelliteTrackingSchedule> {
        return generateSatelliteTrackingSchedule(
            satelliteId = satelliteId,
            startDate = startDate,
            durationDays = durationDays,
            minElevation = minElevation,
            trackingIntervalMs = trackingIntervalMs,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude
        )
    }

    /**
     * 여러 위성의 추적 스케줄을 생성합니다.
     *
     * @param satelliteIds 위성 ID 목록
     * @param startDate 시작 날짜 (기본값: 현재 시간)
     * @param durationDays 계산할 기간(일) (기본값: 1일)
     * @param minElevation 최소 고도각(도) (기본값: 5도)
     * @return 위성 ID를 키로 하고 스케줄을 값으로 하는 맵
     */
    fun generateMultipleSatelliteTrackingSchedules(
        satelliteIds: List<String>,
        startDate: ZonedDateTime,
        durationDays: Int,
        minElevation: Float,
        trackingIntervalMs: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double

    ): Mono<Map<String, OrekitCalculator.SatelliteTrackingSchedule>> {
        val scheduleMonos = satelliteIds.map { satelliteId ->
            generateSatelliteTrackingSchedule(
                satelliteId = satelliteId,
                startDate = startDate,
                durationDays = durationDays,
                minElevation = minElevation,
                trackingIntervalMs = trackingIntervalMs,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude
            ).map { schedule -> satelliteId to schedule }
        }

        return Flux.fromIterable(scheduleMonos)
            .flatMap { it }
            .collectMap({ it.first }, { it.second })
    }

    /**
     * 위성 가시성 기간을 계산합니다.
     *
     * @param satelliteId 위성 ID
     * @param startTime 시작 시간
     * @param durationDays 계산 기간(일)
     * @param minElevation 최소 고도각(도)
     * @return 가시성 기간 목록
     */
    fun calculateSatelliteVisibilityPeriods(
        satelliteId: String,
        startTime: ZonedDateTime = calUtcTimeOffsetTime,
        durationDays: Int = 1,
        minElevation: Float = 5.0f,
        timeStepMs: Int = 100,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
    ): Mono<List<OrekitCalculator.VisibilityPeriod>> {
        return Mono.fromCallable {
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")

            orekitCalculator.calculateVisibilityPeriodsWithMaxElevation(
                tle.first, tle.second, startTime, durationDays, minElevation,
                latitude, longitude, altitude, timeStepMs
            )
        }
    }

    /**
     * 위성 가시성 기간의 요약 정보를 생성합니다.
     *
     * @param satelliteId 위성 ID
     * @param startTime 시작 시간
     * @param durationDays 계산 기간(일)
     * @param minElevation 최소 고도각(도)
     * @return 가시성 기간 요약 정보
     */
    fun getSatelliteVisibilityPeriodsSummary(
        satelliteId: String,
        startTime: ZonedDateTime = calUtcTimeOffsetTime,
        durationDays: Int,
        minElevation: Float,
        timeStepMs: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Mono<String> {
        return calculateSatelliteVisibilityPeriods(
            satelliteId,
            startTime,
            durationDays,
            minElevation,
            timeStepMs,
            latitude,
            longitude,
            altitude
        )
            .map { periods ->
                val totalDuration = periods.fold(Duration.ZERO) { acc, period -> acc.plus(period.duration) }
                val hours = totalDuration.toHours()
                val minutes = totalDuration.toMinutesPart()
                val seconds = totalDuration.toSecondsPart()

                val summary = StringBuilder()
                summary.append("위성 ID: $satelliteId\n")
                summary.append(
                    "기간: ${startTime.format(DateTimeFormatter.ISO_LOCAL_DATE)} ~ ${
                        startTime.plusDays(
                            durationDays.toLong()
                        ).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }\n"
                )
                summary.append("최소 고도각: ${String.format("%.1f", minElevation)}°\n")
                summary.append("총 패스 수: ${periods.size}\n")
                summary.append("총 가시 시간: ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}\n\n")

                summary.append("패스 목록:\n")
                periods.forEachIndexed { index, period ->
                    summary.append("${index + 1}. ${period.toString()}\n")
                }

                summary.toString()
            }
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
     * 위성 추적 스케줄을 계산하고 로그로 출력합니다.
     * 최적화된 버전으로 성능 측정 기능이 추가되었습니다.
     */
    fun printSatelliteTrackingSchedule(
        satelliteId: String,
        startDate: ZonedDateTime = calUtcTimeOffsetTime,
        durationDays: Int = 1,
        minElevation: Float = 5.0f,
        timeStepMs: Int = 100,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ) {
        logger.info("위성 ID: $satelliteId 의 추적 스케줄 계산 시작")
        val totalStartTime = System.currentTimeMillis()

        try {
            // 1. TLE 데이터 확인 및 파싱 (한 번만 수행)
            val tleStartTime = System.currentTimeMillis()
            val tle = getSatelliteTle(satelliteId)
            if (tle == null) {
                logger.error("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")
                return
            }
            val tleEndTime = System.currentTimeMillis()
            logger.info("TLE 데이터 검색 시간: ${tleEndTime - tleStartTime}ms")

            // TLE 객체 생성 (한 번만 파싱하여 재사용)
            val tleParseStartTime = System.currentTimeMillis()
            val tleObj = try {
                TLE(tle.first, tle.second)
            } catch (e: Exception) {
                logger.error("TLE 파싱 중 오류 발생: ${e.message}", e)
                return
            }
            val tleParseEndTime = System.currentTimeMillis()
            logger.info("TLE 파싱 시간: ${tleParseEndTime - tleParseStartTime}ms")

            // 2. 병렬로 여러 계산 수행 (CompletableFuture 사용)
            val parallelStartTime = System.currentTimeMillis()

            // 2.1 궤도 요소 계산 (비동기)
            val orbitalElementsFuture = CompletableFuture.supplyAsync {
                val startTime = System.currentTimeMillis()
                val result = calculateOrbitalElements(satelliteId).block()
                val endTime = System.currentTimeMillis()
                Pair(result, endTime - startTime)
            }

            // 2.2 위성 고도 계산 (비동기)
            val altitudeFuture = CompletableFuture.supplyAsync {
                val startTime = System.currentTimeMillis()
                val result = calculateSatelliteAltitude(satelliteId).block()
                val endTime = System.currentTimeMillis()
                Pair(result, endTime - startTime)
            }

            // 2.3 위성 속도 계산 (비동기)
            val velocityFuture = CompletableFuture.supplyAsync {
                val startTime = System.currentTimeMillis()
                val result = calculateSatelliteVelocity(satelliteId).block()
                val endTime = System.currentTimeMillis()
                Pair(result, endTime - startTime)
            }
            // 2.4 위성 가시성 기간 계산 (최적화된 방식 사용)
            val visibilityStartTime = System.currentTimeMillis()

            // 최적화된 계산 함수 사용
            val visibilityPeriods = orekitCalculator.calculateVisibilityPeriodsWithMaxElevationOptimized(
                tleObj, startDate, durationDays, minElevation, latitude, longitude, altitude, timeStepMs
            )

            val visibilityEndTime = System.currentTimeMillis()
            logger.info("위성 가시성 기간 계산 시간: ${(visibilityEndTime - visibilityStartTime)/1000}초 (${visibilityEndTime - visibilityStartTime}ms)")

            // 3. 비동기 작업 결과 가져오기
            val (orbitalElements, orbitalElementsTime) = orbitalElementsFuture.get()
            val (satelliteAltitude, altitudeTime) = altitudeFuture.get()
            val (velocity, velocityTime) = velocityFuture.get()

            val parallelEndTime = System.currentTimeMillis()
            logger.info("병렬 계산 총 시간: ${(parallelEndTime - parallelStartTime)/1000}초 (${parallelEndTime - parallelStartTime}ms)")

            // 4. 결과 출력 (로그 버퍼링 사용)
            val outputStartTime = System.currentTimeMillis()

            // 로그 버퍼 사용 (많은 로그를 한 번에 출력하기 위함)
            val logBuffer = StringBuilder()

            // 4.1 기본 정보 출력
            logBuffer.appendLine("위성 ID: $satelliteId")
            logBuffer.appendLine("TLE 데이터:")
            logBuffer.appendLine(tle.first)
            logBuffer.appendLine(tle.second)
            logBuffer.appendLine()
            // 4.2 궤도 요소 출력
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

            // 4.3 고도 및 속도 출력
            if (satelliteAltitude != null) {
                logBuffer.appendLine("위성 고도: ${String.format("%.1f", satelliteAltitude)} km")
            }

            if (velocity != null) {
                logBuffer.appendLine("위성 속도: ${String.format("%.1f", velocity)} km/s")
            }
            logBuffer.appendLine()

            // 4.4 가시성 기간 정보 출력 (최적화: 로그 버퍼링 사용)
            if (visibilityPeriods.isNotEmpty()) {
                logBuffer.appendLine("위성 가시성 기간 (총 ${visibilityPeriods.size}개):")

                // 총 가시 시간 계산
                val totalDuration = visibilityPeriods.fold(Duration.ZERO) { acc, period -> acc.plus(period.duration) }
                logBuffer.appendLine("총 가시 시간: ${formatDuration(totalDuration)}")
                logBuffer.appendLine()
                // 각 가시성 기간 정보 출력 (로그 양이 많을 경우 요약 정보만 출력)
                val maxDetailedPasses = 10 // 상세 정보를 출력할 최대 패스 수
                val passesToLog = if (visibilityPeriods.size > maxDetailedPasses) {
                    logBuffer.appendLine("패스가 많아 처음 ${maxDetailedPasses}개만 상세 정보를 출력합니다.")
                    visibilityPeriods.take(maxDetailedPasses)
                } else {
                    visibilityPeriods
                }

                // 패스 정보를 테이블 형식으로 출력 (가독성 향상)
                logBuffer.appendLine("┌─────┬────────────────────┬────────────────────┬──────────┬─────────────┬───────────┬───────────┬───────────┬───────────┐")
                logBuffer.appendLine("│ 번호│      시작 시간     │      종료 시간     │ 지속시간 │ 최대고도각  │ 방위각속도│ 고도각속도│방위각가속도│고도각가속도│")
                logBuffer.appendLine("├─────┼────────────────────┼────────────────────┼──────────┼─────────────┼───────────┼───────────┼───────────┼───────────┤")

                passesToLog.forEachIndexed { index, period ->
                    val durationStr = formatDuration(period.duration)
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
                        val durationStr = formatDuration(period.duration)
                        val maxElevationStr = String.format("%.2f°", period.maxElevation)

                        logBuffer.appendLine("│ ${String.format("%3d", index + 1)} │ ${period.maxElevationTime?.format(timeFormatterWithMillis) ?: "N/A".padEnd(20)} │ ${durationStr} │ ${maxElevationStr.padEnd(11)} │")
                    }
                    logBuffer.appendLine("└─────┴────────────────────┴──────────┴─────────────┘")
                }
            } else {
                logBuffer.appendLine("지정된 기간 동안 위성 가시성 기간이 없습니다.")
            }

            // 로그 버퍼의 내용을 한 번에 출력 (로그 I/O 최소화)
            logger.info(logBuffer.toString())

            val outputEndTime = System.currentTimeMillis()
            logger.info("결과 출력 시간: ${outputEndTime - outputStartTime}ms")
            val totalEndTime = System.currentTimeMillis()
            logger.info("위성 ID: $satelliteId 의 추적 스케줄 계산 완료 (총 소요 시간: ${totalEndTime - totalStartTime}ms)")

            // 5. 성능 요약 정보 (시각화 개선)
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


    /*

        */
/**
     * 위성 추적 스케줄을 계산하고 로그로 출력합니다.
     * 최적화된 버전으로 성능 측정 기능이 추가되었습니다.
     *//*

    fun printSatelliteTrackingSchedule(
        satelliteId: String,
        startDate: ZonedDateTime = calUtcTimeOffsetTime,
        durationDays: Int = 1,
        minElevation: Float = 5.0f,
        timeStepMs: Int = 100,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ) {
        logger.info("위성 ID: $satelliteId 의 추적 스케줄 계산 시작")
        val totalStartTime = System.currentTimeMillis()

        try {
            // TLE 데이터 확인
            val tleStartTime = System.currentTimeMillis()
            val tle = getSatelliteTle(satelliteId)
            if (tle == null) {
                logger.error("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")
                return
            }
            val tleEndTime = System.currentTimeMillis()
            logger.info("TLE 데이터 검색 시간: ${tleEndTime - tleStartTime}ms")

            // TLE 객체 생성 (한 번만 파싱하여 재사용)
            val tleParseStartTime = System.currentTimeMillis()
            val tleObj = try {
                TLE(tle.first, tle.second)
            } catch (e: Exception) {
                logger.error("TLE 파싱 중 오류 발생: ${e.message}", e)
                return
            }
            val tleParseEndTime = System.currentTimeMillis()
            logger.info("TLE 파싱 시간: ${tleParseEndTime - tleParseStartTime}ms")

            // 병렬로 여러 계산 수행 (비동기 처리)
            val parallelStartTime = System.currentTimeMillis()

            // 1. 궤도 요소 계산
            val orbitalElementsStartTime = System.currentTimeMillis()
            val orbitalElements = calculateOrbitalElements(satelliteId).block()
            val orbitalElementsEndTime = System.currentTimeMillis()
            logger.info("궤도 요소 계산 시간: ${orbitalElementsEndTime - orbitalElementsStartTime}ms")

            // 2. 위성 고도 계산
            val altitudeStartTime = System.currentTimeMillis()
            val satelliteAltitude = calculateSatelliteAltitude(satelliteId).block()
            val altitudeEndTime = System.currentTimeMillis()
            logger.info("위성 고도 계산 시간: ${altitudeEndTime - altitudeStartTime}ms")

            // 3. 위성 속도 계산
            val velocityStartTime = System.currentTimeMillis()
            val velocity = calculateSatelliteVelocity(satelliteId).block()
            val velocityEndTime = System.currentTimeMillis()
            logger.info("위성 속도 계산 시간: ${velocityEndTime - velocityStartTime}ms")

            // 4. 위성 가시성 기간 계산 (최적화된 방식 사용)
            val visibilityStartTime = System.currentTimeMillis()
            val visibilityPeriods = orekitCalculator.calculateVisibilityPeriodsWithMaxElevationOptimized(
                tleObj, startDate, durationDays, minElevation, latitude, longitude, altitude, timeStepMs
            )
            val visibilityEndTime = System.currentTimeMillis()
            logger.info("위성 가시성 기간 계산 시간: ${(visibilityEndTime - visibilityStartTime)/1000}초 (${visibilityEndTime - visibilityStartTime}ms)")

            val parallelEndTime = System.currentTimeMillis()
            logger.info("병렬 계산 총 시간: ${(parallelEndTime - parallelStartTime)/1000}초 (${parallelEndTime - parallelStartTime}ms)")

            // 결과 출력
            val outputStartTime = System.currentTimeMillis()

            // 기본 정보 출력
            logger.info("위성 ID: $satelliteId")
            logger.info("TLE 데이터:")
            logger.info(tle.first)
            logger.info(tle.second)

            // 궤도 요소 출력
            if (orbitalElements != null) {
                logger.info("위성 궤도 요소:")
                logger.info("- 경사각(Inclination): ${String.format("%.2f", orbitalElements["inclination"] ?: 0.0)}°")
                logger.info("- 승교점 적경(RAAN): ${String.format("%.2f", orbitalElements["rightAscension"] ?: 0.0)}°")
                logger.info("- 이심률(Eccentricity): ${String.format("%.6f", orbitalElements["eccentricity"] ?: 0.0)}")
                logger.info("- 근지점 인수(Arg of Perigee): ${String.format("%.2f", orbitalElements["argumentOfPerigee"] ?: 0.0)}°")
                logger.info("- 평균 근점 이각(Mean Anomaly): ${String.format("%.2f", orbitalElements["meanAnomaly"] ?: 0.0)}°")
                logger.info("- 평균 운동(Mean Motion): ${String.format("%.6f", orbitalElements["meanMotion"] ?: 0.0)} rev/day")
            }

            // 고도 및 속도 출력
            if (satelliteAltitude != null) {
                logger.info("위성 고도: ${String.format("%.1f", satelliteAltitude)} km")
            }

            if (velocity != null) {
                logger.info("위성 속도: ${String.format("%.1f", velocity)} km/s")
            }

            // 가시성 기간 정보 출력 (최적화: 로그 버퍼링 사용)
            if (visibilityPeriods.isNotEmpty()) {
                logger.info("위성 가시성 기간 (총 ${visibilityPeriods.size}개):")

                // 총 가시 시간 계산
                val totalDuration = visibilityPeriods.fold(Duration.ZERO) { acc, period -> acc.plus(period.duration) }
                logger.info("총 가시 시간: ${formatDuration(totalDuration)}")

                // 각 가시성 기간 정보 출력 (로그 양이 많을 경우 요약 정보만 출력)
                val maxDetailedPasses = 10 // 상세 정보를 출력할 최대 패스 수
                val passesToLog = if (visibilityPeriods.size > maxDetailedPasses) {
                    logger.info("패스가 많아 처음 ${maxDetailedPasses}개만 상세 정보를 출력합니다.")
                    visibilityPeriods.take(maxDetailedPasses)
                } else {
                    visibilityPeriods
                }

                passesToLog.forEachIndexed { index, period ->
                    val durationStr = formatDuration(period.duration)

                    logger.info("패스 #${index + 1}:")
                    logger.info("- 시작: ${period.startTime.format(timeFormatterWithMillis)}")
                    logger.info("- 종료: ${period.endTime.format(timeFormatterWithMillis)}")
                    logger.info("- 지속 시간: $durationStr")
                    logger.info("- 최대 고도각: ${String.format("%.2f", period.maxElevation)}° (${period.maxElevationTime?.format(timeOnlyFormatterWithMillis)})")

                    // 성능 최적화: 속도와 가속도 정보는 필요한 경우에만 출력
                    if (period.maxAzimuthRate > 0.1 || period.maxElevationRate > 0.1) {
                        logger.info("- 최대 방위각 속도: ${String.format("%.2f", period.maxAzimuthRate)}°/s")
                        logger.info("- 최대 고도각 속도: ${String.format("%.2f", period.maxElevationRate)}°/s")
                    }

                    if (period.maxAzimuthAccel > 0.01 || period.maxElevationAccel > 0.01) {
                        logger.info("- 최대 방위각 가속도: ${String.format("%.2f", period.maxAzimuthAccel)}°/s²")
                        logger.info("- 최대 고도각 가속도: ${String.format("%.2f", period.maxElevationAccel)}°/s²")
                    }
                }

                // 나머지 패스에 대한 요약 정보
                if (visibilityPeriods.size > maxDetailedPasses) {
                    logger.info("나머지 ${visibilityPeriods.size - maxDetailedPasses}개 패스의 요약 정보:")

                    // 최대 고도각 기준으로 정렬하여 중요한 패스 정보 제공
                    val remainingPasses = visibilityPeriods.drop(maxDetailedPasses)
                        .sortedByDescending { it.maxElevation }
                        .take(5) // 상위 5개만 표시

                    remainingPasses.forEachIndexed { index, period ->
                        logger.info("중요 패스 #${index + 1}: 최대 고도각 ${String.format("%.2f", period.maxElevation)}°, " +
                                "시간 ${period.maxElevationTime?.format(timeFormatterWithMillis)}, " +
                                "지속 시간 ${formatDuration(period.duration)}")
                    }
                }
            } else {
                logger.info("지정된 기간 동안 위성 가시성 기간이 없습니다.")
            }

            val outputEndTime = System.currentTimeMillis()
            logger.info("결과 출력 시간: ${outputEndTime - outputStartTime}ms")

            val totalEndTime = System.currentTimeMillis()
            logger.info("위성 ID: $satelliteId 의 추적 스케줄 계산 완료 (총 소요 시간: ${totalEndTime - totalStartTime}ms)")

            // 성능 요약 정보
            logger.info("성능 요약:")
            logger.info("- TLE 데이터 검색: ${tleEndTime - tleStartTime}ms (${((tleEndTime - tleStartTime) * 100.0 / (totalEndTime - totalStartTime)).toInt()}%)")
            logger.info("- TLE 파싱: ${tleParseEndTime - tleParseStartTime}ms (${((tleParseEndTime - tleParseStartTime) * 100.0 / (totalEndTime - totalStartTime)).toInt()}%)")
            logger.info("- 궤도 요소 계산: ${orbitalElementsEndTime - orbitalElementsStartTime}ms (${((orbitalElementsEndTime - orbitalElementsStartTime) * 100.0 / (totalEndTime - totalStartTime)).toInt()}%)")
            logger.info("- 위성 고도 계산: ${altitudeEndTime - altitudeStartTime}ms (${((altitudeEndTime - altitudeStartTime) * 100.0 / (totalEndTime - totalStartTime)).toInt()}%)")
            logger.info("- 위성 속도 계산: ${velocityEndTime - velocityStartTime}ms (${((velocityEndTime - velocityStartTime) * 100.0 / (totalEndTime - totalStartTime)).toInt()}%)")
            logger.info("- 가시성 기간 계산: ${(visibilityEndTime - visibilityStartTime)/1000}초 (${visibilityEndTime - visibilityStartTime}ms( (${((visibilityEndTime - visibilityStartTime) * 100.0 / (totalEndTime - totalStartTime)).toInt()}%)")
            logger.info("- 결과 출력: ${outputEndTime - outputStartTime}ms (${((outputEndTime - outputStartTime) * 100.0 / (totalEndTime - totalStartTime)).toInt()}%)")

        } catch (e: Exception) {
            val totalEndTime = System.currentTimeMillis()
            logger.error("위성 추적 스케줄 계산 중 오류 발생: ${e.message}", e)
            logger.info("오류 발생까지 소요 시간: ${totalEndTime - totalStartTime}ms")
        }
    }
*/

    /**
     * 특정 위성의 추적 스케줄을 계산하고 로그로 출력합니다.
     */
    /*fun printSatelliteTrackingSchedule(
        satelliteId: String,
        startDate: ZonedDateTime,
        durationDays: Int,
        minElevation: Float,
        timeStepMs: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ) {
        try {
            logger.info("위성 ID: $satelliteId 의 추적 스케줄 계산 시작 (기간: $durationDays 일, 최소 고도각: $minElevation°)")

            // TLE 데이터 확인
            val tle = getSatelliteTle(satelliteId)
            if (tle == null) {
                logger.error("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")
                return
            }

            logger.info("위성 TLE 데이터:")
            logger.info(tle.first)
            logger.info(tle.second)

            try {
                // 위성 궤도 요소 계산
                val orbitalElements = calculateOrbitalElements(satelliteId).block()
                if (orbitalElements != null) {
                    logger.info("위성 궤도 요소:")
                    logger.info("- 경사각(Inclination): ${String.format("%.2f", orbitalElements["inclination"] ?: 0.0)}°")
                    logger.info("- 승교점 적경(RAAN): ${String.format("%.2f", orbitalElements["rightAscension"] ?: 0.0)}°")
                    logger.info("- 이심률(Eccentricity): ${String.format("%.6f", orbitalElements["eccentricity"] ?: 0.0)}")
                    logger.info(
                        "- 근지점 인수(Arg of Perigee): ${
                            String.format(
                                "%.2f",
                                orbitalElements["argumentOfPerigee"] ?: 0.0
                            )
                        }°"
                    )
                    logger.info(
                        "- 평균 근점 이각(Mean Anomaly): ${
                            String.format(
                                "%.2f",
                                orbitalElements["meanAnomaly"] ?: 0.0
                            )
                        }°"
                    )
                    logger.info(
                        "- 평균 운동(Mean Motion): ${
                            String.format(
                                "%.6f",
                                orbitalElements["meanMotion"] ?: 0.0
                            )
                        } rev/day"
                    )
                }
            } catch (e: Exception) {
                logger.error("궤도 요소 계산 중 오류 발생: ${e.message}", e)
            }

            try {
                // 위성 고도 계산
                val altitude = calculateSatelliteAltitude(satelliteId).block()
                if (altitude != null) {
                    logger.info("위성 고도: ${String.format("%.1f", altitude)} km")
                }
            } catch (e: Exception) {
                logger.error("위성 고도 계산 중 오류 발생: ${e.message}", e)
            }

            try {
                // 위성 속도 계산
                val velocity = calculateSatelliteVelocity(satelliteId).block()
                if (velocity != null) {
                    logger.info("위성 속도: ${String.format("%.1f", velocity)} km/s")
                }
            } catch (e: Exception) {
                logger.error("위성 속도 계산 중 오류 발생: ${e.message}", e)
            }

            try {
                // 위성 가시성 기간 계산
                val visibilityPeriods = calculateSatelliteVisibilityPeriods(
                    satelliteId, startDate, durationDays, minElevation, timeStepMs, latitude, longitude, altitude
                ).block()

                if (visibilityPeriods != null && visibilityPeriods.isNotEmpty()) {
                    logger.info("위성 가시성 기간 (총 ${visibilityPeriods.size}개):")

                    // 총 가시 시간 계산
                    val totalDuration =
                        visibilityPeriods.fold(Duration.ZERO) { acc, period -> acc.plus(period.duration) }
                    val hours = totalDuration.toHours()
                    val minutes = totalDuration.toMinutesPart()
                    val seconds = totalDuration.toSecondsPart()
                    val millis = totalDuration.toMillisPart()

                    logger.info("총 가시 시간: ${String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)}")

                    // 각 가시성 기간 정보 출력
                    visibilityPeriods.forEachIndexed { index, period ->
                        val durationStr = String.format(
                            "%02d:%02d:%02d.%03d",
                            period.duration.toHours(),
                            period.duration.toMinutesPart(),
                            period.duration.toSecondsPart(),
                            period.duration.toMillisPart()
                        )

                        logger.info("패스 #${index + 1}:")
                        logger.info("- 시작: ${period.startTime.format(timeFormatterWithMillis)}")
                        logger.info("- 종료: ${period.endTime.format(timeFormatterWithMillis)}")
                        logger.info("- 지속 시간: $durationStr")
                        logger.info(
                            "- 최대 고도각: ${
                                String.format(
                                    "%.2f",
                                    period.maxElevation
                                )
                            }° (${period.maxElevationTime?.format(timeOnlyFormatterWithMillis)})"
                        )
                        logger.info("- 최대 방위각 속도: ${String.format("%.2f", period.maxAzimuthRate)}°/s")
                        logger.info("- 최대 고도각 속도: ${String.format("%.2f", period.maxElevationRate)}°/s")
                        logger.info("- 최대 방위각 가속도: ${String.format("%.2f", period.maxAzimuthAccel)}°/s²")
                        logger.info("- 최대 고도각 가속도: ${String.format("%.2f", period.maxElevationAccel)}°/s²")
                    }
                } else {
                    logger.info("지정된 기간 동안 위성 가시성 기간이 없습니다.")
                }
            } catch (e: Exception) {
                logger.error("위성 가시성 기간 계산 중 오류 발생: ${e.message}", e)
            }

            logger.info("위성 ID: $satelliteId 의 추적 스케줄 계산 완료")
        } catch (e: Exception) {
            logger.error("위성 추적 스케줄 계산 중 오류 발생: ${e.message}", e)
        }
    }*/

    /**
     * 모든 캐시된 위성의 추적 스케줄을 계산하고 로그로 출력합니다.
     *
     * @param startDate 시작 날짜 (기본값: 현재 시간)
     * @param durationDays 계산할 기간(일) (기본값: 1일)
     * @param minElevation 최소 고도각(도) (기본값: 5도)
     */
    fun printAllSatelliteTrackingSchedules(
        startDate: ZonedDateTime = calUtcTimeOffsetTime,
        durationDays: Int = 1,
        minElevation: Float = 5.0f,
        timeStepMs: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ) {
        val satelliteIds = getAllSatelliteIds()

        if (satelliteIds.isEmpty()) {
            logger.info("캐시된 위성이 없습니다.")
            return
        }

        logger.info("총 ${satelliteIds.size}개 위성의 추적 스케줄 계산 시작")

        satelliteIds.forEach { satelliteId ->
            printSatelliteTrackingSchedule(
                satelliteId,
                startDate,
                durationDays,
                minElevation,
                timeStepMs,
                latitude,
                longitude,
                altitude
            )
        }

        logger.info("모든 위성의 추적 스케줄 계산 완료")
    }

    /**
     * 특정 위성의 추적 스케줄을 계산하고 문자열로 반환합니다.
     */
    fun getSatelliteTrackingScheduleAsString(
        satelliteId: String,
        startDate: ZonedDateTime,
        durationDays: Int,
        minElevation: Float,
        timeStepMs: Int,
        latitude: Double,
        longitude: Double,
    ): Mono<String> {
        return Mono.fromCallable {
            val sb = StringBuilder()

            // TLE 데이터 확인
            val tle = getSatelliteTle(satelliteId)
                ?: throw IllegalArgumentException("위성 ID에 해당하는 TLE 데이터가 없습니다: $satelliteId")

            sb.appendLine("위성 ID: $satelliteId")
            sb.appendLine("TLE 데이터:")
            sb.appendLine(tle.first)
            sb.appendLine(tle.second)
            sb.appendLine()

            // 위성 궤도 요소 계산
            val orbitalElements = calculateOrbitalElements(satelliteId).block()
            if (orbitalElements != null) {
                sb.appendLine("위성 궤도 요소:")
                sb.appendLine("- 경사각(Inclination): ${String.format("%.2f", orbitalElements["inclination"] ?: 0.0)}°")
                sb.appendLine("- 승교점 적경(RAAN): ${String.format("%.2f", orbitalElements["rightAscension"] ?: 0.0)}°")
                sb.appendLine("- 이심률(Eccentricity): ${String.format("%.6f", orbitalElements["eccentricity"] ?: 0.0)}")
                sb.appendLine(
                    "- 근지점 인수(Arg of Perigee): ${
                        String.format(
                            "%.2f",
                            orbitalElements["argumentOfPerigee"] ?: 0.0
                        )
                    }°"
                )
                sb.appendLine(
                    "- 평균 근점 이각(Mean Anomaly): ${
                        String.format(
                            "%.2f",
                            orbitalElements["meanAnomaly"] ?: 0.0
                        )
                    }°"
                )
                sb.appendLine(
                    "- 평균 운동(Mean Motion): ${
                        String.format(
                            "%.6f",
                            orbitalElements["meanMotion"] ?: 0.0
                        )
                    } rev/day"
                )
                sb.appendLine()
            }

            // 위성 고도 계산
            val altitude = calculateSatelliteAltitude(satelliteId).block()
            if (altitude != null) {
                sb.appendLine("위성 고도: ${String.format("%.1f", altitude)} km")
            }

            // 위성 속도 계산
            val velocity = calculateSatelliteVelocity(satelliteId).block()
            if (velocity != null) {
                sb.appendLine("위성 속도: ${String.format("%.1f", velocity)} km/s")
            }
            sb.appendLine()

            // 위성 가시성 기간 계산
            val visibilityPeriods = calculateSatelliteVisibilityPeriods(
                satelliteId, startDate, durationDays, minElevation, timeStepMs, latitude, longitude, altitude ?: 0.0
            ).block()

            if (visibilityPeriods != null && visibilityPeriods.isNotEmpty()) {
                sb.appendLine("위성 가시성 기간 (총 ${visibilityPeriods.size}개):")

                // 총 가시 시간 계산
                val totalDuration = visibilityPeriods.fold(Duration.ZERO) { acc, period -> acc.plus(period.duration) }
                val hours = totalDuration.toHours()
                val minutes = totalDuration.toMinutesPart()
                val seconds = totalDuration.toSecondsPart()
                val millis = totalDuration.toMillisPart()

                sb.appendLine("총 가시 시간: ${String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)}")
                sb.appendLine()

                // 각 가시성 기간 정보 출력
                visibilityPeriods.forEachIndexed { index, period ->
                    val durationStr = String.format(
                        "%02d:%02d:%02d.%03d",
                        period.duration.toHours(),
                        period.duration.toMinutesPart(),
                        period.duration.toSecondsPart(),
                        period.duration.toMillisPart()
                    )

                    sb.appendLine("패스 #${index + 1}:")
                    sb.appendLine("- 시작: ${period.startTime.format(timeFormatterWithMillis)}")
                    sb.appendLine("- 종료: ${period.endTime.format(timeFormatterWithMillis)}")
                    sb.appendLine("- 지속 시간: $durationStr")
                    sb.appendLine(
                        "- 최대 고도각: ${
                            String.format(
                                "%.2f",
                                period.maxElevation
                            )
                        }° (${period.maxElevationTime?.format(timeOnlyFormatterWithMillis)})"
                    )
                    sb.appendLine("- 최대 방위각 속도: ${String.format("%.2f", period.maxAzimuthRate)}°/s")
                    sb.appendLine("- 최대 고도각 속도: ${String.format("%.2f", period.maxElevationRate)}°/s")
                    sb.appendLine("- 최대 방위각 가속도: ${String.format("%.2f", period.maxAzimuthAccel)}°/s²")
                    sb.appendLine("- 최대 고도각 가속도: ${String.format("%.2f", period.maxElevationAccel)}°/s²")
                    sb.appendLine()
                }
            } else {
                sb.appendLine("지정된 기간 동안 위성 가시성 기간이 없습니다.")
            }

            sb.toString()
        }
    }

    /**
     * 위성 추적 스케줄을 파일로 저장합니다.
     *
     * @param satelliteId 위성 ID
     * @param filePath 저장할 파일 경로
     * @param startDate 시작 날짜 (기본값: 현재 시간)
     * @param durationDays 계산할 기간(일) (기본값: 1일)
     * @param minElevation 최소 고도각(도) (기본값: 5도)
     * @return 파일 저장 성공 여부
     */
    fun saveSatelliteTrackingScheduleToFile(
        satelliteId: String,
        filePath: String,
        startDate: ZonedDateTime,
        durationDays: Int,
        minElevation: Float,
        timeStepMs: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Mono<Boolean> {
        return getSatelliteTrackingScheduleAsString(
            satelliteId,
            startDate,
            durationDays,
            minElevation,
            timeStepMs,
            latitude,
            longitude
        )
            .map { scheduleString ->
                try {
                    File(filePath).writeText(scheduleString)
                    logger.info("위성 추적 스케줄이 파일에 저장되었습니다: $filePath")
                    true
                } catch (e: Exception) {
                    logger.error("위성 추적 스케줄 파일 저장 중 오류 발생: ${e.message}", e)
                    false
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

