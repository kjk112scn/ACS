package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.algorithm.ElevationCalculator
import com.gtlsystems.acs_api.algorithm.suntrack.impl.SolarOrekitCalculator
import com.gtlsystems.acs_api.algorithm.suntrack.interfaces.SunPositionCalculator
import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData.CMD
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.BitSet

@Service
class SunTrackService(
    private val udpFwICDService: UdpFwICDService,
    private val sunPositionCalculator: SunPositionCalculator,
    private val eventBus: ACSEventBus  // 이 부분이 올바르게 주입되는지 확인
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    val solarOrekitCalculator = SolarOrekitCalculator()
    val elevationCalculator = ElevationCalculator()
    // 주기적 작업을 관리하기 위한 Disposable 객체 저장 변수
    private var sunTrackCommandSubscription: Disposable? = null
    private var eventSubscription: Disposable? = null

    @PostConstruct
    fun initialize() {
        try {
            // 모든 추적 중지 이벤트를 구독
            eventSubscription = eventBus.subscribe(ACSEvent.TrackingEvent.StopAllTracking::class.java)
                .subscribe(
                    { event ->
                        stopSunTrackCommandPeriodically()
                        println("추적 중지 이벤트 수신: $event")
                    },
                    { error -> println("이벤트 구독 중 오류 발생: ${error.message}") }
                )

            // 태양 추적 중지 이벤트도 구독
            val sunTrackStopSubscription = eventBus.subscribe(ACSEvent.TrackingEvent.StopSunTracking::class.java)
                .subscribe(
                    { event ->
                        stopSunTrackCommandPeriodically()
                        println("태양 추적 중지 이벤트 수신: $event")
                    },
                    { error -> println("태양 추적 중지 이벤트 구독 중 오류 발생: ${error.message}") }
                )
            //SolarOrekitData()
            //ElevationCheck()
        } catch (e: Exception) {
            println("이벤트 버스 구독 설정 중 오류 발생: ${e.message}")
            e.printStackTrace() // 상세 오류 정보 출력
        }
    }

    fun ElevationCheck()
    {
        // 서울 좌표로 테스트
        val comparison = elevationCalculator.getElevationComparisonBlocking(
            latitude = GlobalData.Location.latitude,
            longitude = GlobalData.Location.longitude,
            googleApiKey = "AIzaSyBbfXRX59vC4TAbzzdGyaehc8k-39vTTdo" // 실제 키 입력하면 Google API도 테스트 가능
        )

        println(comparison)
    }
    fun SolarOrekitData()
    {
        // GTL 지상국 설정 (altitude는 m단위)
        solarOrekitCalculator.initializeGroundStation(GlobalData.Location.latitude, GlobalData.Location.longitude,0.0)
        // 현재 태양 위치
        val currentPosition = solarOrekitCalculator.getCurrentSunPosition()
        println("Current sun position: $currentPosition")

        // 오늘 UTC 00시부터 24시간 예측 (1시간 간격)
        val todayMidnightUTC = LocalDate.now(ZoneOffset.UTC).atStartOfDay() // UTC 기준 오늘 00:00:00
        //val todayMidnightUTC = LocalDate.of(2025,6,3).atStartOfDay() // UTC 기준 오늘 00:00:00
        val tomorrowMidnightUTC = todayMidnightUTC.plusDays(1)              // UTC 기준 내일 00:00:00

        println("Calculation period: ${todayMidnightUTC} UTC to ${tomorrowMidnightUTC} UTC")

        val predictions = solarOrekitCalculator.predictSunPositions(todayMidnightUTC, tomorrowMidnightUTC, 0.01)

        println("\nSun tracking predictions (UTC time):")
        predictions.forEach {
            // 로컬 시간도 함께 표시 (한국 시간 KST = UTC+9)
            val kstTime = it.dateTime.plusHours(9)
            println("UTC: ${it.dateTime} (KST: $kstTime) - Az: ${String.format("%.6f", it.azimuthDegrees)}°, El: ${String.format("%.6f", it.elevationDegrees)}°, Visible: ${it.isSunVisible()}")
        }

        // 오늘 UTC 00시부터 일출/일몰 찾기
        val todaySunrise = solarOrekitCalculator.findNextSunrise(todayMidnightUTC)
        val todaySunset = solarOrekitCalculator.findNextSunset(todayMidnightUTC)

        println("\nToday's events (UTC time):")
        if (todaySunrise != null) {
            val sunriseKST = todaySunrise.dateTime.plusHours(9)
            println("Sunrise: ${todaySunrise.dateTime} UTC (${sunriseKST} KST)")
        }

        if (todaySunset != null) {
            val sunsetKST = todaySunset.dateTime.plusHours(9)
            println("Sunset: ${todaySunset.dateTime} UTC (${sunsetKST} KST)")
        }

        // 현재 시간 정보 출력
        val nowUTC = LocalDateTime.now(ZoneOffset.UTC)
        val nowKST = nowUTC.plusHours(9)
        println("\nCurrent time: $nowUTC UTC ($nowKST KST)")

    }
    /**
     * 송신 부 반복 수행 시작
     *
     * @param interval 명령 전송 간격 (밀리초)
     * @param cmdAzimuthSpeed 방위각 속도
     * @param cmdElevationSpeed 고도각 속도
     * @param cmdTiltSpeed 틸트 속도
     * @return 생성된 Disposable 객체 (중지 시 사용)
     */
    fun startSunTrackCommandPeriodically(
        interval: Long,
        cmdAzimuthSpeed: Float,
        cmdElevationSpeed: Float,
        cmdTiltSpeed: Float
    ): Disposable {
        // 이미 실행 중인 경우 중지
        stopSunTrackCommandPeriodically()

        // 새로운 주기적 작업 시작
        sunTrackCommandSubscription = Flux.interval(Duration.ofMillis(interval))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe {
                sunTrackingStartCommand(cmdAzimuthSpeed, cmdElevationSpeed, cmdTiltSpeed)
            }

        println("태양 추적 명령 주기적 전송 시작 (간격: ${interval}ms)")
        return sunTrackCommandSubscription!!
    }

    fun sunTrackingStartCommand(
        cmdAzimuthSpeed: Float,
        cmdElevationSpeed: Float,
        cmdTiltSpeed: Float
    ) {
        try {
            val sunTrackData = sunPositionCalculator.calculatePosition(
                GlobalData.Time.calUtcTimeOffsetTime,
                GlobalData.Location.latitude,
                GlobalData.Location.longitude,
                GlobalData.Location.altitude,
            )
            val cmdTiltAngle = CMD.cmdTiltAngle
            val multiAxis = BitSet()
            multiAxis.set(0)
            multiAxis.set(1)
            //multiAxis.set(2)
            udpFwICDService.multiManualCommand(
                multiAxis,
                sunTrackData.azimuth,  // null이면 0.0f 사용
                cmdAzimuthSpeed,
                sunTrackData.elevation,
                cmdElevationSpeed,
                cmdTiltAngle ?: 0.0f,
                cmdTiltSpeed
            )

        } catch (e: Exception) {
            println("SunTracking 명령어 전송 오류: ${e.message}")
        }
    }

    /**
     * 송신 부 반복 수행 중지
     *
     * @return 중지 성공 여부
     */
    fun stopSunTrackCommandPeriodically(): Boolean {
        return try {
            if (sunTrackCommandSubscription != null && !sunTrackCommandSubscription!!.isDisposed) {
                sunTrackCommandSubscription!!.dispose()
                sunTrackCommandSubscription = null

                // 모든 축(azimuth, elevation, tilt)을 정지시키는 BitSet 생성
                val allAxes = BitSet()
                allAxes.set(0)  // azimuth
                allAxes.set(1)  // elevation
                allAxes.set(2)  // tilt

                // 모든 축을 정지시키는 stopCommand 호출
                udpFwICDService.stopCommand(allAxes)

                println("태양 추적 명령 주기적 전송 중지됨 (모든 축 정지)")
                true
            } else {
                println("태양 추적 명령 주기적 전송이 이미 중지되었거나 실행 중이 아님")
                false
            }
        } catch (e: Exception) {
            println("태양 추적 명령 주기적 전송 중지 중 오류 발생: ${e.message}")
            false
        }
    }

    /**
     * 현재 태양 추적 명령 주기적 전송 상태 확인
     *
     * @return 실행 중이면 true, 그렇지 않으면 false
     */
    fun isSunTrackCommandRunning(): Boolean {
        return sunTrackCommandSubscription != null && !sunTrackCommandSubscription!!.isDisposed
    }
    @PreDestroy
    fun cleanup() {
        // 이벤트 구독 해제
        eventSubscription?.dispose()

        // 태양 추적 중지
        stopSunTrackCommandPeriodically()
    }
}

