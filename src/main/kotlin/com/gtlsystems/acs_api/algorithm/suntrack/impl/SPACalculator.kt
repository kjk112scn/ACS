package com.gtlsystems.acs_api.algorithm.suntrack.impl

import com.gtlsystems.acs_api.algorithm.suntrack.interfaces.SunPositionCalculator
import com.gtlsystems.acs_api.algorithm.suntrack.model.SunTrackData
import com.gtlsystems.acs_api.model.GlobalData
import net.e175.klaus.solarpositioning.DeltaT
import net.e175.klaus.solarpositioning.Grena3
import net.e175.klaus.solarpositioning.SPA
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.Disposable
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SPA(Solar Position Algorithm) 알고리즘을 사용하여 태양 위치를 계산하는 클래스
 */
@Service
class SPACalculator : SunPositionCalculator {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val isTrackingActive = AtomicBoolean(false)
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    // 기본 관측 위치 (필요시 업데이트)
    private var defaultLatitude: Double = GlobalData.Location.latitude // 서울 위도
    private var defaultLongitude: Double = GlobalData.Location.longitude
    private var defaultElevation: Double = GlobalData.Location.altitude

    private var sunTrackCommandSubscription: Disposable? = null
    enum class Algorithm { SPA, GRENA3 }

    private var defaultAlgorithm = Algorithm.SPA

    /**
     * 현재 태양 추적 명령 주기적 전송 상태 확인
     *
     * @return 실행 중이면 true, 그렇지 않으면 false
     */
    fun isSunTrackCommandRunning(): Boolean {
        return sunTrackCommandSubscription != null && !sunTrackCommandSubscription!!.isDisposed
    }

    /**
     * 지정된 알고리즘을 사용하여 태양의 위치를 계산합니다
     *
     * @param dateTime 계산할 날짜와 시간
     * @param latitude 관측자의 위도 (북쪽이 양수)
     * @param longitude 관측자의 경도 (동쪽이 양수)
     * @param elevation 관측자의 고도 (해수면 위 미터 단위)
     * @param algorithm 사용할 알고리즘
     * @return 방위각과 고도각을 포함하는 SolarPositionData 객체
     */
    fun calculateSunPosition(
        dateTime: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        elevation: Double = 0.0,
        algorithm: Algorithm = defaultAlgorithm
    ): SunTrackData {
        // GregorianCalendar 객체로 변환
        val calendar = GregorianCalendar.from(dateTime)

        // 태양 위치 계산
        val azimuth: Double
        val zenithAngle: Double

        when (algorithm) {
            Algorithm.SPA -> {
                // LocalDate 사용
                val result = SPA.calculateSolarPosition(
                    dateTime,  // ZonedDateTime 직접 사용
                    latitude,
                    longitude,
                    elevation,
                    DeltaT.estimate(dateTime.toLocalDate()),  // LocalDate 전달
                    1013.25, // 표준 대기압 (hPa)
                    15.0     // 표준 기온 (°C)
                )
                azimuth = result.azimuth
                zenithAngle = result.zenithAngle
            }

            Algorithm.GRENA3 -> {
                val result = Grena3.calculateSolarPosition(
                    dateTime,  // ZonedDateTime 직접 사용
                    latitude,
                    longitude,
                    DeltaT.estimate(dateTime.toLocalDate())  // LocalDate 전달
                )
                azimuth = result.azimuth
                zenithAngle = result.zenithAngle
            }
        }

        // 천정각에서 고도각으로 변환 (고도각 = 90° - 천정각)
        val elevation = 90.0 - zenithAngle

        return SunTrackData(
            azimuth = azimuth.toFloat(),
            elevation = elevation.toFloat(),
            timestamp = dateTime
        )
    }

    /**
     * 현재 시간의 태양 위치를 계산합니다
     *
     * @param latitude 관측자의 위도 (북쪽이 양수)
     * @param longitude 관측자의 경도 (동쪽이 양수)
     * @param elevation 관측자의 고도 (해수면 위 미터 단위)
     * @param algorithm 사용할 알고리즘
     * @return 방위각과 고도각을 포함하는 SolarPositionData 객체
     */
    fun getCurrentSunPosition(
        latitude: Double = defaultLatitude,
        longitude: Double = defaultLongitude,
        elevation: Double = defaultElevation,
        algorithm: Algorithm = defaultAlgorithm
    ): SunTrackData {
        val now = ZonedDateTime.now()
        return calculateSunPosition(now, latitude, longitude, elevation, algorithm)
    }

    /**
     * 특정 시간 범위 동안의 태양 위치를 계산합니다
     *
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param interval 계산 간격 (분)
     * @param latitude 관측자의 위도
     * @param longitude 관측자의 경도
     * @param elevation 관측자의 고도
     * @param algorithm 사용할 알고리즘
     * @return 시간별 태양 위치 목록을 포함한 SolarPositionData 객체
     */
    fun calculateSunTrackingPath(
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        interval: Int = 1, // 기본값 1분 간격
        latitude: Double = defaultLatitude,
        longitude: Double = defaultLongitude,
        elevation: Double = defaultElevation,
        algorithm: Algorithm = defaultAlgorithm
    ): SunTrackData {
        val positions = mutableListOf<Pair<ZonedDateTime, SunTrackData>>()

        var currentTime = startTime
        while (!currentTime.isAfter(endTime)) {
            val position = calculateSunPosition(currentTime, latitude, longitude, elevation, algorithm)
            positions.add(Pair(currentTime, position))
            currentTime = currentTime.plusMinutes(interval.toLong())
        }

        // 첫 번째 위치의 방위각과 고도각을 사용
        val firstPosition = positions.firstOrNull()?.second ?: SunTrackData(0.0f, 0.0f)

        return SunTrackData(
            azimuth = firstPosition.azimuth,
            elevation = firstPosition.elevation,
            startTime = startTime,
            endTime = endTime,
            interval = interval,
            positions = positions
        )
    }

    /**
     * 태양 추적을 위한 방위각과 고도각 변화율을 계산합니다
     *
     * @param latitude 관측자의 위도
     * @param longitude 관측자의 경도
     * @param elevation 관측자의 고도
     * @param lookAheadMinutes 미래 예측 시간 (분)
     * @param algorithm 사용할 알고리즘
     * @return 방위각과 고도각 변화율 (도/분)
     */
    fun calculateSunTrackingRates(
        latitude: Double = defaultLatitude,
        longitude: Double = defaultLongitude,
        elevation: Double = defaultElevation,
        lookAheadMinutes: Int = 1,
        algorithm: Algorithm = defaultAlgorithm
    ): Pair<Float, Float> {
        val now = ZonedDateTime.now()
        val future = now.plusMinutes(lookAheadMinutes.toLong())

        val currentPosition = calculateSunPosition(now, latitude, longitude, elevation, algorithm)
        val futurePosition = calculateSunPosition(future, latitude, longitude, elevation, algorithm)

        // 방위각 변화율 계산 (도/분)
        var azimuthDelta = futurePosition.azimuth - currentPosition.azimuth
        // 방위각이 0/360 경계를 넘는 경우 처리
        if (azimuthDelta > 180) {
            azimuthDelta -= 360
        } else if (azimuthDelta < -180) {
            azimuthDelta += 360
        }
        val azimuthRate = azimuthDelta / lookAheadMinutes

        // 고도각 변화율 계산 (도/분)
        val elevationRate = (futurePosition.elevation - currentPosition.elevation) / lookAheadMinutes

        return Pair(azimuthRate, elevationRate)
    }

    /**
     * 태양 추적 명령을 생성합니다
     *
     * @param latitude 관측자의 위도
     * @param longitude 관측자의 경도
     * @param elevation 관측자의 고도
     * @param trackingMode 추적 모드 (CURRENT: 현재 위치, RATE: 변화율 기반)
     * @param algorithm 사용할 알고리즘
     * @return 태양 추적 명령을 포함한 SolarPositionData 객체
     */
    fun generateSunTrackingCommand(
        latitude: Double = defaultLatitude,
        longitude: Double = defaultLongitude,
        elevation: Double = defaultElevation,
        trackingMode: String = "CURRENT",
        algorithm: Algorithm = defaultAlgorithm
    ): SunTrackData {
        val currentPosition = getCurrentSunPosition(latitude, longitude, elevation, algorithm)

        return when (trackingMode.uppercase()) {
            "RATE" -> {
                val (azimuthRate, elevationRate) = calculateSunTrackingRates(
                    latitude, longitude, elevation, algorithm = algorithm
                )
                SunTrackData(
                    azimuth = currentPosition.azimuth,
                    elevation = currentPosition.elevation,
                    timestamp = ZonedDateTime.now(),
                    azimuthRate = azimuthRate,
                    elevationRate = elevationRate,
                    trackingMode = "RATE"
                )
            }

            else -> { // "CURRENT" 또는 기본값
                SunTrackData(
                    azimuth = currentPosition.azimuth,
                    elevation = currentPosition.elevation,
                    timestamp = ZonedDateTime.now(),
                    azimuthRate = 0.0f,
                    elevationRate = 0.0f,
                    trackingMode = "CURRENT"
                )
            }
        }
    }

    /**
     * 일출 시간을 계산합니다
     *
     * @param date 계산할 날짜
     * @param latitude 관측자의 위도
     * @param longitude 관측자의 경도
     * @return 일출 시간
     */
    override fun calculateSunrise(
        date: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): ZonedDateTime {
        // 해당 날짜의 0시부터 시작
        val startOfDay = date.truncatedTo(ChronoUnit.DAYS)

        // 1분 간격으로 24시간 동안 태양 고도 검사
        for (minute in 0 until 24 * 60) {
            val time = startOfDay.plusMinutes(minute.toLong())
            val position = calculatePosition(time, latitude, longitude, 0.0)

            // 태양 고도가 0도를 넘어가는 순간이 일출
            if (position.elevation >= 0) {
                return time
            }
        }

        // 일출을 찾지 못한 경우 (극지방 등에서 발생 가능)
        return startOfDay
    }

    /**
     * 일몰 시간을 계산합니다
     *
     * @param date 계산할 날짜
     * @param latitude 관측자의 위도
     * @param longitude 관측자의 경도
     * @return 일몰 시간
     */
    override fun calculateSunset(
        date: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): ZonedDateTime {
        // 해당 날짜의 정오부터 시작
        val noon = date.truncatedTo(ChronoUnit.DAYS).plusHours(12)

        // 1분 간격으로 12시간 동안 태양 고도 검사
        for (minute in 0 until 12 * 60) {
            val time = noon.plusMinutes(minute.toLong())
            val position = calculatePosition(time, latitude, longitude, 0.0)

            // 태양 고도가 0도 아래로 내려가는 순간이 일몰
            if (position.elevation < 0) {
                return time
            }
        }

        // 일몰을 찾지 못한 경우 (극지방 등에서 발생 가능)
        return noon.plusHours(12)
    }

    /**
     * 인터페이스에서 요구하는 calculatePosition 메서드 구현
     */
    override fun calculatePosition(
        dateTime: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        elevation: Double,
    ): SunTrackData {
        return calculateSunPosition(dateTime, latitude, longitude, elevation, defaultAlgorithm)
    }

    /**
     * 기본 관측 위치를 설정합니다
     */
    fun setDefaultLocation(latitude: Double, longitude: Double, elevation: Double = 0.0) {
        this.defaultLatitude = latitude
        this.defaultLongitude = longitude
        this.defaultElevation = elevation
        logger.info("기본 관측 위치가 업데이트되었습니다: 위도 $latitude, 경도 $longitude, 고도 $elevation")
    }

    /**
     * 기본 알고리즘을 설정합니다
     */
    fun setDefaultAlgorithm(algorithm: Algorithm) {
        this.defaultAlgorithm = algorithm
        logger.info("기본 알고리즘이 업데이트되었습니다: $algorithm")
    }
    }