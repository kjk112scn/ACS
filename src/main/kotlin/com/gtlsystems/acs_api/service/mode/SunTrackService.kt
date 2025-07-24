package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.ElevationCalculator
import com.gtlsystems.acs_api.algorithm.axistransformation.CoordinateTransformer
import com.gtlsystems.acs_api.algorithm.suntrack.impl.SolarOrekitCalculator
import com.gtlsystems.acs_api.config.ThreadManager // ✅ ThreadManager 추가
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.model.PushData.CMD
import com.gtlsystems.acs_api.service.udp.UdpFwICDService
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.BitSet
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.time.ZonedDateTime
import kotlin.math.abs

@Service
class SunTrackService(
    private val udpFwICDService: UdpFwICDService,
    private val dataStoreService: com.gtlsystems.acs_api.service.datastore.DataStoreService,
    private val threadManager: ThreadManager, // ✅ ThreadManager 주입
    private val solarOrekitCalculator: SolarOrekitCalculator // ✅ SolarOrekitCalculator 주입
) {
    private val logger = LoggerFactory.getLogger(SunTrackService::class.java)
    private val elevationCalculator = ElevationCalculator()

    // ✅ ThreadManager 통합 사용
    private var modeExecutor: ScheduledExecutorService? = null
    private var modeTask: ScheduledFuture<*>? = null

    // ✅ 성능 모니터링
    private var lastProcessingTime = 0L
    private var processingTimeWarningThreshold = 50L // 50ms 이상이면 경고
    private var lastTrackingTime: Long? = null // 마지막 추적 시간

    // ✅ SunTrack 상태 관리에 추가
    private var sunTrackState = SunTrackState.IDLE
    private var targetTiltAngle: Double? = null
    private var tiltStabilizationStartTime: Long? = null
    private var isInitialTiltMovementCompleted = false
    private var midTime: LocalDateTime? = null  // ✅ 일출/일몰 가운데 시간 저장
    private var rotatorAngle: Double? = null    // ✅ Rotator 각도 저장

    // ✅ 일출/일몰 방향 정보 추가
    private var sunriseAzimuth: Double? = null
    private var sunsetAzimuth: Double? = null
    private var isSouthPath: Boolean? = null // true면 동→남→서, false면 동→북→서

    // ✅ 추적 상태 참조 추가
    private val trackingStatus = PushData.TRACKING_STATUS

    // ✅ SunTrack 상태 열거형
    enum class SunTrackState {
        IDLE,           // 대기 상태
        INITIAL_TILT,   // 초기 Tilt 이동 중
        STABILIZING,    // Tilt 안정화 대기 중
        TRACKING        // 실시간 태양 추적 중
    }

    // ✅ SunTrack 타임아웃 설정
    companion object {
        const val TILT_MOVE_TIMEOUT = 120000L        // Tilt 이동: 2분
        const val TILT_STABILIZATION_TIMEOUT = 5000L // Tilt 안정화: 5초
    }

    @PostConstruct
    fun init() {
        logger.info("SunTrackService 초기화 시작")
        modeExecutor = threadManager.getModeExecutor()
        
        // ✅ 지상국 초기화
        try {
            solarOrekitCalculator.initializeGroundStation(
                GlobalData.Location.latitude,
                GlobalData.Location.longitude,
                GlobalData.Location.altitude
            )
            logger.info("지상국 초기화 완료")
        } catch (e: Exception) {
            logger.error("지상국 초기화 실패: {}", e.message, e)
        }
        
        logger.info("SunTrackService 초기화 완료 - ThreadManager 통합")
    }

    @PreDestroy
    fun cleanup() {
        logger.info("SunTrackService 정리 시작")
        stopModeTimer()
        logger.info("SunTrackService 정리 완료")
    }

    /**
     * ✅ 모드 타이머 시작 (EphemerisService 방식)
     */
    private fun startModeTimer() {
        if (modeTask != null && !modeTask!!.isCancelled) {
            logger.warn("모드 타이머가 이미 실행 중입니다")
            return
        }

        try {
            modeTask = modeExecutor?.scheduleAtFixedRate(
                {
                    try {
                        val startTime = System.currentTimeMillis()
                        
                        // ✅ SunTrack 상태별 처리
                        processSunTrackByState()
                        
                        // ✅ 성능 모니터링
                        val processingTime = System.currentTimeMillis() - startTime
                        lastProcessingTime = processingTime
                        
                        if (processingTime > processingTimeWarningThreshold) {
                            logger.warn("Sun Track 처리 시간 경고: {}ms", processingTime)
                        }
                        
                    } catch (e: Exception) {
                        logger.error("Sun Track 처리 중 오류: {}", e.message, e)
                    }
                },
                0, // 즉시 시작
                100, // 100ms 간격 (EphemerisService와 동일)
                TimeUnit.MILLISECONDS
            )
            
            logger.info("Sun Track 모드 타이머 시작 (100ms 간격)")
            
        } catch (e: Exception) {
            logger.error("Sun Track 모드 타이머 시작 실패: {}", e.message, e)
        }
    }

    /**
     * ✅ 모드 타이머 중지 (EphemerisService 방식)
     */
    private fun stopModeTimer() {
        try {
            modeTask?.let { task ->
                if (!task.isCancelled) {
                    task.cancel(false)
                    logger.info("Sun Track 모드 타이머 중지됨")
                }
            }
            modeTask = null

        } catch (e: Exception) {
            logger.error("Sun Track 모드 타이머 중지 실패: {}", e.message, e)
        }
    }

    /**
     * ✅ SunTrack 상태별 처리 로직
     */
    private fun processSunTrackByState() {
        // ✅ 이전 상태 저장
        val previousTrackingState = trackingStatus.sunTrackTrackingState
        
        when (sunTrackState) {
            SunTrackState.IDLE -> {
                // 대기 상태 - 아무것도 하지 않음
                trackingStatus.sunTrackTrackingState = "IDLE"
                logger.debug("☀️ Sun Track 상태: IDLE")
            }
            
            SunTrackState.INITIAL_TILT -> {
                // 초기 Tilt 이동 처리
                trackingStatus.sunTrackTrackingState = "TILT_MOVING_TO_ZERO"
                logger.debug("☀️ Sun Track 상태: TILT_MOVING_TO_ZERO")
                processInitialTiltMovement()
            }
            
            SunTrackState.STABILIZING -> {
                // Tilt 안정화 대기 처리
                trackingStatus.sunTrackTrackingState = "TILT_STABILIZING"
                logger.debug("☀️ Sun Track 상태: TILT_STABILIZING")
                processTiltStabilization()
            }
            
            SunTrackState.TRACKING -> {
                // 실시간 태양 추적 처리
                trackingStatus.sunTrackTrackingState = "TRACKING"
                logger.debug("☀️ Sun Track 상태: TRACKING")
                processRealTimeSunTracking()
            }
        }
        
        // ✅ 상태가 변경된 경우에만 로그 출력
        if (previousTrackingState != trackingStatus.sunTrackTrackingState) {
            logger.info("☀️ Sun Track 추적 상태 변경: {} → {}", 
                previousTrackingState, trackingStatus.sunTrackTrackingState)
        }
    }

        /**
     * ✅ 위도 기반 태양 추적 케이스 분류
     */
    enum class SunTrackingCase(val description: String) {
        EAST_SOUTH_WEST("동→남→서 (북반구 일반)"),
        EAST_NORTH_WEST("동→북→서 (남반구 일반)"), 
        MIDNIGHT_SUN("백야 (극지방 여름)"),
        POLAR_NIGHT("극야 (극지방 겨울)")
    }

    /**
     * ✅ 360도 경계 조건
     */
    enum class BoundaryCondition {
        NORMAL,         // 일반적인 경우
        CROSSES_NORTH   // 북쪽(0도) 경계를 넘음
    }

    /**
     * ✅ Train 각도 계산 결과
     */
    data class TrainAngleResult(
        val angle: Double,
        val calculationMethod: String
    )

    /**
     * ✅ GPS 위도 기반 자동 케이스 분류
     */
    private fun classifyTrackingCaseByLatitude(latitude: Double): SunTrackingCase {
        return when {
            latitude >= 66.5 -> {
                // 북극권: 백야/극야 판단 필요
                determineArcticCondition()
            }
            latitude > 23.5 -> {
                // 북반구 일반 (한국 포함)
                SunTrackingCase.EAST_SOUTH_WEST
            }
            latitude >= -23.5 -> {
                // 열대: 계절별 판단 (일단 북반구로 처리)
                SunTrackingCase.EAST_SOUTH_WEST
            }
            latitude > -66.5 -> {
                // 남반구 일반
                SunTrackingCase.EAST_NORTH_WEST
            }
            else -> {
                // 남극권: 백야/극야 판단 필요
                determineArcticCondition()
            }
        }
    }

    /**
     * ✅ 극지방 상태 판단
     */
    private fun determineArcticCondition(): SunTrackingCase {
        return try {
            val sunInfo = solarOrekitCalculator.getTodaySunriseAndSunset()
            val currentSun = solarOrekitCalculator.getCurrentSunPosition()
            
            when {
                sunInfo["sunrise"] == "일출 없음" && currentSun.elevationDegrees > 0 -> {
                    SunTrackingCase.MIDNIGHT_SUN
                }
                currentSun.elevationDegrees <= 0 -> {
                    SunTrackingCase.POLAR_NIGHT
                }
                else -> {
                    // 극지방이지만 정상적인 일출/일몰 있음
                    SunTrackingCase.EAST_SOUTH_WEST
                }
            }
        } catch (e: Exception) {
            logger.warn("극지방 상태 판단 실패, 기본값 사용: {}", e.message)
            SunTrackingCase.EAST_SOUTH_WEST
        }
    }

    /**
     * ✅ 360도 경계 조건 감지 (북반구 + 남반구 모두 지원)
     */
    private fun detectsBoundaryCondition(sunriseAz: Double, sunsetAz: Double): BoundaryCondition {
        // 북반구 스타일: 일출이 서쪽, 일몰이 동쪽 (예: 350° → 10°)
        val crossesNorthStyle = (sunriseAz > 270.0 && sunsetAz < 90.0)
        
        // 남반구 스타일: 일출이 동쪽, 일몰이 서쪽이지만 북쪽을 지남 (예: 32° → 293°)
        val crossesSouthStyle = (sunriseAz < 90.0 && sunsetAz > 270.0)
        
        return if (crossesNorthStyle || crossesSouthStyle) {
            if (crossesNorthStyle) {
                logger.info("360도 경계 감지 (북반구형): 일출={}°, 일몰={}° → 북쪽 경로", 
                    String.format("%.1f", sunriseAz),
                    String.format("%.1f", sunsetAz))
            } else {
                logger.info("360도 경계 감지 (남반구형): 일출={}°, 일몰={}° → 북쪽 경로", 
                    String.format("%.1f", sunriseAz),
                    String.format("%.1f", sunsetAz))
            }
            BoundaryCondition.CROSSES_NORTH
        } else {
            BoundaryCondition.NORMAL
        }
    }

    /**
     * ✅ 경계 조건 고려한 Train 각도 계산
     */
    private fun calculateTrainAngleWithBoundaryCorrection(
        sunriseAz: Double, 
        sunsetAz: Double
    ): TrainAngleResult {
        
        val boundary = detectsBoundaryCondition(sunriseAz, sunsetAz)
        
        return when (boundary) {
            BoundaryCondition.CROSSES_NORTH -> {
                // 북쪽 경계 보정: 남반구와 북반구 다르게 처리
                val isNorthernHemisphere = sunriseAz > 270.0 && sunsetAz < 90.0
                val isSouthernHemisphere = sunriseAz < 90.0 && sunsetAz > 270.0
                
                val trainAngle = if (isNorthernHemisphere) {
                    // 북반구: 350° → 0° → 10° (일몰에 +360)
                    val adjustedSunset = sunsetAz + 360.0
                    val rawMidpoint = (sunriseAz + adjustedSunset) / 2.0
                    rawMidpoint % 360.0
                } else if (isSouthernHemisphere) {
                    // 남반구: 32° → 0° → 293° (일몰에 -360, 더 가까운 쪽 선택)
                    val option1 = (sunriseAz + sunsetAz) / 2.0  // 163도 (남쪽)
                    val adjustedSunset = sunsetAz - 360.0  // -66.515
                    val option2Raw = (sunriseAz + adjustedSunset) / 2.0  // -16.763
                    val option2 = if (option2Raw < 0) option2Raw + 360 else option2Raw  // 343.237
                    
                    // 북쪽에 더 가까운 쪽 선택 (0도 기준)
                    val distanceToNorth1 = Math.min(option1, 360 - option1)  // min(163, 197) = 163
                    val distanceToNorth2 = Math.min(option2, 360 - option2)  // min(343, 17) = 17
                    
                    if (distanceToNorth2 < distanceToNorth1) option2 else option1
                } else {
                    (sunriseAz + sunsetAz) / 2.0  // 기본값
                }
                
                // ✅ Train 축 ±270도 제한 적용
                val finalTrainAngle = normalizeTrainAngleToMechanicalLimits(trainAngle)
                
                logger.info("북쪽 경계 처리: 일출={}°, 일몰={}° → Train={}° → 제한적용={}°",
                    String.format("%.1f", sunriseAz),
                    String.format("%.1f", sunsetAz),
                    String.format("%.1f", trainAngle),
                    String.format("%.1f", finalTrainAngle))
                
                TrainAngleResult(finalTrainAngle, "북쪽 경계 보정 (±270도 제한 적용)")
            }
            
            BoundaryCondition.NORMAL -> {
                // 일반적인 경우
                val rawTrainAngle = (sunriseAz + sunsetAz) / 2.0
                val finalTrainAngle = normalizeTrainAngleToMechanicalLimits(rawTrainAngle)
                
                logger.info("일반 계산: 일출={}°, 일몰={}° → Train={}° → 제한적용={}°",
                    String.format("%.1f", sunriseAz),
                    String.format("%.1f", sunsetAz),
                    String.format("%.1f", rawTrainAngle),
                    String.format("%.1f", finalTrainAngle))
                
                TrainAngleResult(finalTrainAngle, "일반 계산 (±270도 제한 적용)")
            }
        }
    }

    /**
     * ✅ Train 각도를 기계적 제한 범위(±270도)로 정규화
     */
    private fun normalizeTrainAngleToMechanicalLimits(angle: Double): Double {
        return when {
            angle > 270.0 -> {
                // 270도 초과시 역방향으로 변환 (343° → -17°)
                val normalized = angle - 360.0
                logger.debug("Train 각도 정규화: {}° → {}° (역방향)", 
                    String.format("%.1f", angle),
                    String.format("%.1f", normalized))
                normalized
            }
            angle < -270.0 -> {
                // -270도 미만시 정방향으로 변환 (-300° → 60°)
                val normalized = angle + 360.0
                logger.debug("Train 각도 정규화: {}° → {}° (정방향)", 
                    String.format("%.1f", angle),
                    String.format("%.1f", normalized))
                normalized
            }
            else -> {
                // ±270도 범위 내: 그대로 유지
                angle
            }
        }
    }

    /**
     * ✅ 백야 상황 전용 Train 각도 계산 (향후 구현)
     */
    private fun calculateTrainAngleForMidnightSun(): TrainAngleResult {
        logger.info("⚠️ 백야 상황 감지 - 특수 처리 모드")
        
        // TODO: 백야 상황에서 Train 각도 계산 로직
        // 가능한 방법들:
        // 1. 동적 Train 회전 (12시간마다 180도)
        // 2. 현재 태양 위치 기준 Train 설정
        // 3. 시간대 기반 Train 설정
        
        return TrainAngleResult(
            angle = 0.0,  // 임시값: 북쪽
            calculationMethod = "백야 전용 로직 (구현 예정)"
        )
    }

    /**
     * ✅ 극야 상황 처리
     */
    private fun handlePolarNight(): TrainAngleResult {
        logger.info("⚠️ 극야 상황 감지 - 태양 추적 불가능")
        
        return TrainAngleResult(
            angle = Double.NaN,
            calculationMethod = "극야 - 추적 중단"
        )
    }

    /**
     * ✅ 일반 지역용 일출/일몰 가운데 계산
     */
    private fun calculateTrainAngleByMiddleOfSunriseAndSunset(): TrainAngleResult {
        val todaySunInfo = solarOrekitCalculator.getTodaySunriseAndSunset()
        val sunriseInfo = todaySunInfo["sunrise"]
        val sunsetInfo = todaySunInfo["sunset"]
        
        if (sunriseInfo is Map<*, *> && sunsetInfo is Map<*, *>) {
            val sunriseAzimuth = (sunriseInfo["azimuth_degrees"] as String).toDouble()
            val sunsetAzimuth = (sunsetInfo["azimuth_degrees"] as String).toDouble()
            
            // 360도 경계 처리 적용
            val result = calculateTrainAngleWithBoundaryCorrection(sunriseAzimuth, sunsetAzimuth)
            
            logger.info("일출/일몰 가운데 계산: 일출={}°, 일몰={}°, Train={}° ({})",
                String.format("%.3f", sunriseAzimuth),
                String.format("%.3f", sunsetAzimuth), 
                String.format("%.3f", result.angle),
                result.calculationMethod)
            
            return result
        } else {
            throw RuntimeException("일출/일몰 정보를 가져올 수 없습니다: $todaySunInfo")
        }
    }

    /**
     * ✅ 모든 케이스를 고려한 최적 Train 각도 계산
     */
    private fun calculateOptimalTrainAngleUniversal(): TrainAngleResult {
        try {
            val latitude = GlobalData.Location.latitude
            val trackingCase = classifyTrackingCaseByLatitude(latitude)
            
            logger.info("GPS 위치 분석: 위도={}°, 케이스={}", 
                String.format("%.3f", latitude), 
                trackingCase.description)
            
            return when (trackingCase) {
                SunTrackingCase.EAST_SOUTH_WEST, SunTrackingCase.EAST_NORTH_WEST -> {
                    // 일반 지역: 일출/일몰 가운데 계산
                    calculateTrainAngleByMiddleOfSunriseAndSunset()
                }
                
                SunTrackingCase.MIDNIGHT_SUN -> {
                    // 백야: 특수 함수 (구현 예정)
                    calculateTrainAngleForMidnightSun()
                }
                
                SunTrackingCase.POLAR_NIGHT -> {
                    // 극야: 추적 중단
                    handlePolarNight()
                }
            }
        } catch (e: Exception) {
            logger.error("Train 각도 계산 실패: {}", e.message, e)
            // 기본값: 남쪽
            return TrainAngleResult(180.0, "에러 발생 - 기본값 사용")
        }
    }

    /**
     * ✅ 범용 상대 Azimuth 계산
     */
    private fun calculateUniversalAzimuth(sunAzimuth: Double, trainAngle: Double): Double {
        var relativeAzimuth = sunAzimuth - trainAngle
        
        // ±180도 범위로 정규화 (최단 경로)
        while (relativeAzimuth > 180.0) relativeAzimuth -= 360.0
        while (relativeAzimuth < -180.0) relativeAzimuth += 360.0
        
        return relativeAzimuth
    }

    /**
     * ✅ 일출/일몰 방향 정보 설정
     */
    private fun setSunriseSunsetAzimuths() {
                val todaySunInfo = solarOrekitCalculator.getTodaySunriseAndSunset()
        val sunriseInfo = todaySunInfo["sunrise"]
        val sunsetInfo = todaySunInfo["sunset"]

        if (sunriseInfo is Map<*, *> && sunsetInfo is Map<*, *>) {
            this.sunriseAzimuth = (sunriseInfo["azimuth_degrees"] as String).toDouble()
            this.sunsetAzimuth = (sunsetInfo["azimuth_degrees"] as String).toDouble()
            this.isSouthPath = (sunriseAzimuth!! < sunsetAzimuth!!) // true면 동→남→서, false면 동→북→서
        } else {
            logger.error("일출/일몰 정보를 가져올 수 없습니다: {}", todaySunInfo)
            this.sunriseAzimuth = null
            this.sunsetAzimuth = null
            this.isSouthPath = null
        }
    }

    /**
     * ✅ 일출/일몰 가운데 시간 계산
     */
    private fun calculateMidTime() {
        val todaySunInfo = solarOrekitCalculator.getTodaySunriseAndSunset()
                val sunriseInfo = todaySunInfo["sunrise"]
                val sunsetInfo = todaySunInfo["sunset"]
                
                if (sunriseInfo is Map<*, *> && sunsetInfo is Map<*, *>) {
                    val sunriseTime = LocalDateTime.parse(sunriseInfo["time"] as String)
                    val sunsetTime = LocalDateTime.parse(sunsetInfo["time"] as String)

                    midTime = if (sunriseTime.isBefore(sunsetTime)) {
                        sunriseTime.plusSeconds(sunriseTime.until(sunsetTime, java.time.temporal.ChronoUnit.SECONDS) / 2)
                    } else {
                        sunsetTime.plusSeconds(sunsetTime.until(sunriseTime, java.time.temporal.ChronoUnit.SECONDS) / 2)
            }
        } else {
            logger.error("일출/일몰 정보를 가져올 수 없습니다: {}", todaySunInfo)
            midTime = null
        }
    }

    /**
     * ✅ 일출/일몰 방향에 따른 Azimuth 계산
     * 동→남→서 경로: 270도 넘으면 음수로 변환
     * 동→북→서 경로: 양수로 유지
     */
    private fun calculateAzimuthBySunPath(azimuth: Double): Double {
        if (isSouthPath == null) {
            logger.warn("일출/일몰 방향 정보가 없습니다. 기본값(양수) 사용")
            return azimuth
        }
        
        return if (isSouthPath!!) {
            // 1번 상황: 동→남→서 경로 → 270도 넘으면 음수로 변환
            if (azimuth > 270.0) {
                val negativeAzimuth = azimuth - 360.0
                logger.debug("동→남→서 경로: {}° → {}° (음수 변환)", 
                    String.format("%.3f", azimuth),
                    String.format("%.3f", negativeAzimuth))
                negativeAzimuth
                    } else {
                logger.debug("동→남→서 경로: {}° (양수 유지)", String.format("%.3f", azimuth))
                azimuth
            }
        } else {
            // 2번 상황: 동→북→서 경로 → 양수로 유지
            logger.debug("동→북→서 경로: {}° (양수 유지)", String.format("%.3f", azimuth))
            azimuth
        }
    }

    /**
     * ✅ Azimuth 각도를 0~360도 범위로 정규화
     */
    private fun normalizeAzimuthTo360Range(azimuth: Double): Double {
        var normalized = azimuth
        while (normalized >= 360.0) normalized -= 360.0
        while (normalized < 0.0) normalized += 360.0
        return normalized
    }

            /**
     * ✅ 개선된 초기 Tilt 이동 처리
     */
    private fun processInitialTiltMovement() {
        try {
            if (targetTiltAngle == null) {
                // ✅ 통합된 범용 Train 각도 계산 사용
                val trainResult = calculateOptimalTrainAngleUniversal()
                
                if (trainResult.angle.isNaN()) {
                    // 극야 등으로 Train 계산 불가능
                    logger.warn("Train 각도 계산 불가능: {}", trainResult.calculationMethod)
                    sunTrackState = SunTrackState.IDLE
                    return
                }
                
                targetTiltAngle = trainResult.angle
                rotatorAngle = trainResult.angle
                CMD.cmdTiltAngle = trainResult.angle.toFloat()
                
                // ✅ 일출/일몰 방향 정보 설정 (호환성 유지)
                setSunriseSunsetAzimuths()
                
                // ✅ 일출/일몰 가운데 시간 계산 (호환성 유지)
                calculateMidTime()
                
                logger.info("개선된 Train 각도 설정 완료: {}° ({})", 
                    String.format("%.3f", trainResult.angle),
                    trainResult.calculationMethod)
                
                // ✅ Train 이동 명령 전송
                    sendTiltMovementCommand(rotatorAngle!!)
                    
                // ✅ 안정화 단계로 전환
                sunTrackState = SunTrackState.STABILIZING
                tiltStabilizationStartTime = System.currentTimeMillis()
                
                logger.info("Train 이동 명령 전송 완료, 안정화 단계 진입")
            } else {
                // ✅ targetTiltAngle이 이미 설정되어 있으면 매번 목표 각도 도달 확인
                val currentTiltAngle = dataStoreService.getLatestData().tiltAngle
                val moveTolerance = 1.0 // ±1.0도 허용 (EphemerisService와 동일)
                
                if (currentTiltAngle != null && targetTiltAngle != null) {
                    val angleDifference = Math.abs(currentTiltAngle - targetTiltAngle!!)
                    
                    logger.debug("Tilt 목표 각도 확인 중: 현재={}°, 목표={}°, 차이={}°", 
                        String.format("%.3f", currentTiltAngle),
                        String.format("%.3f", targetTiltAngle),
                        String.format("%.3f", angleDifference))
                    
                    // ✅ 목표 각도 도달 시 STABILIZING 상태로 전환
                    if (angleDifference <= moveTolerance) {
                        logger.info("Tilt 목표 각도 도달: 현재={}°, 목표={}°, 차이={}° (허용오차: ±{}°)", 
                            String.format("%.3f", currentTiltAngle),
                            String.format("%.3f", targetTiltAngle),
                            String.format("%.3f", angleDifference),
                            moveTolerance)
                        
                        sunTrackState = SunTrackState.STABILIZING
                        tiltStabilizationStartTime = System.currentTimeMillis()
                        logger.info("Tilt 안정화 단계 시작")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("초기 Tilt 이동 처리 중 오류: {}", e.message, e)
            sunTrackState = SunTrackState.IDLE
        }
    }

    /**
     * ✅ Tilt 안정화 대기 처리 (±0.5도 허용, 각도 데이터 없으면 정지)
     */
    private fun processTiltStabilization() {
        try {
            val currentTiltAngle = dataStoreService.getLatestData().tiltAngle
            val stabilizationTolerance = 0.5 // ±0.5도 허용 (안정화용)

            if (currentTiltAngle != null && targetTiltAngle != null) {
                val angleDifference = Math.abs(currentTiltAngle - targetTiltAngle!!)
                
                // ✅ STABILIZING 상태에서의 안정화 처리 (EphemerisService와 동일한 조건)
                if (sunTrackState == SunTrackState.STABILIZING) {
                    if (tiltStabilizationStartTime == null) {
                        tiltStabilizationStartTime = System.currentTimeMillis()
                        logger.info("Tilt 안정화 타이머 시작")
                        return
                    }

                    val currentTime = System.currentTimeMillis()
                    val stabilizationDuration = currentTime - tiltStabilizationStartTime!!
                    
                    // ✅ 1초마다 로그 출력 (너무 자주 출력하지 않도록)
                    if (stabilizationDuration % 1000 < 100) {
                        logger.debug("Tilt 안정화 대기: 현재={}°, 목표={}°, 차이={}°, 경과시간={}ms", 
                            String.format("%.3f", currentTiltAngle),
                            String.format("%.3f", targetTiltAngle),
                            String.format("%.3f", angleDifference),
                            stabilizationDuration)
                    }
                    
                    // ✅ 5초 안정화 완료 (EphemerisService와 동일한 조건)
                    if (stabilizationDuration >= 5000 && angleDifference <= stabilizationTolerance) {
                        logger.info("Tilt 안정화 완료: 현재={}°, 목표={}°, 차이={}°, 대기시간={}ms", 
                            String.format("%.3f", currentTiltAngle),
                            String.format("%.3f", targetTiltAngle),
                            String.format("%.3f", angleDifference),
                            stabilizationDuration)
                        
                        // ✅ 실시간 추적 상태로 전환
                        sunTrackState = SunTrackState.TRACKING
                        isInitialTiltMovementCompleted = true
                        CMD.cmdTiltAngle = targetTiltAngle!!.toFloat()
                        
                        logger.info("Sun Track 실시간 추적 상태로 전환 완료")
                        
                    } else if (stabilizationDuration > 300000) {
                        // ✅ 5분 후에도 안정화되지 못한 경우 (EphemerisService와 동일한 타임아웃)
                        logger.warn("Tilt 안정화 실패: 현재={}°, 목표={}°, 차이={}°, 대기시간={}ms", 
                            String.format("%.3f", currentTiltAngle),
                            String.format("%.3f", targetTiltAngle),
                            String.format("%.3f", angleDifference),
                            stabilizationDuration)
                        
                        // ✅ 실패 시에도 추적 시작 (안전장치)
                        sunTrackState = SunTrackState.TRACKING
                        isInitialTiltMovementCompleted = true
                        CMD.cmdTiltAngle = targetTiltAngle!!.toFloat()
                        
                        logger.info("Tilt 안정화 실패했지만 추적 시작")
                    }
                }
            } else {
                // ✅ 각도 데이터가 없으면 SunTrack을 정지(IDLE) 상태로 전환
                logger.error("Tilt 각도 데이터 없음. 현재={}, 목표={}. SunTrack을 정지합니다.", 
                    currentTiltAngle, targetTiltAngle)
                sunTrackState = SunTrackState.IDLE
                targetTiltAngle = null
                tiltStabilizationStartTime = null
            }
        } catch (e: Exception) {
            logger.error("Tilt 안정화 처리 중 오류: {}", e.message, e)
            sunTrackState = SunTrackState.IDLE
        }
    }

    /**
     * ✅ 실시간 태양 추적 처리 (Cal Time 기준)
     */
    private fun processRealTimeSunTracking() {
        val totalStartTime = System.currentTimeMillis()
        
        try {
            // ✅ Cal Time(보정된 기준 시간) 사용
            if (rotatorAngle != null) {
                // 1단계: Cal Time 계산 시간 측정
                val calTimeStart = System.currentTimeMillis()
                val calTime = GlobalData.Time.resultTimeOffsetCalTime
                val utcLocalDateTime = calTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                val calTimeDuration = System.currentTimeMillis() - calTimeStart
                
                // 2단계: 태양 위치 계산 시간 측정
                val sunCalcStart = System.currentTimeMillis()
                val sunPosition = solarOrekitCalculator.getSunPositionAt(utcLocalDateTime)
                val sunCalcDuration = System.currentTimeMillis() - sunCalcStart
                
                // 3단계: 3축 좌표 변환 시간 측정
                val transformStart = System.currentTimeMillis()
                val (transformedAz, transformedEl) = CoordinateTransformer.transformCoordinatesWithRotator(
                    azimuth = sunPosition.azimuthDegrees,  // 원본 태양 위치
                    elevation = sunPosition.elevationDegrees,
                    tiltAngle = -6.98,                     // 실제 Tilt 기울기
                    rotatorAngle = rotatorAngle!!          // Train 축 회전 각도 (일출/일몰 가운데)
                )
                val transformDuration = System.currentTimeMillis() - transformStart
                
                            // ✅ 4단계: 범용 상대 Azimuth 계산
            val relativeAzimuth = calculateUniversalAzimuth(
                sunPosition.azimuthDegrees, 
                rotatorAngle!!
            )
            
            // ✅ ±270도 제한 체크
            if (Math.abs(relativeAzimuth) > 270.0) {
                logger.warn("태양이 Train 제한범위 초과: 상대Az={}°, 제한=±270°", 
                    String.format("%.3f", relativeAzimuth))
                dataStoreService.setSunTracking(false)
                return
            }
                
                // 5단계: 명령 전송 시간 측정
                val commandStart = System.currentTimeMillis()
                sendAzimuthAndElevationAxisCommand(relativeAzimuth.toFloat(), 5.0f, transformedEl.toFloat(), 5.0f, rotatorAngle!!.toFloat())
                val commandDuration = System.currentTimeMillis() - commandStart
                
                // ✅ 데이터 스토어 업데이트
                dataStoreService.setSunTracking(true)
                
                // ✅ 전체 성능 분석
                val totalEndTime = System.currentTimeMillis()
                val totalProcessingTime = totalEndTime - totalStartTime
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCycle = if (lastTrackingTime != null) currentTime - lastTrackingTime!! else 0L
                lastTrackingTime = currentTime
                
                // ✅ 성능 경고 (각 단계별 + 전체)
                val performanceWarning = StringBuilder()
                if (calTimeDuration > 10) performanceWarning.append("CalTime:${calTimeDuration}ms ")
                if (sunCalcDuration > 20) performanceWarning.append("SunCalc:${sunCalcDuration}ms ")
                if (transformDuration > 10) performanceWarning.append("Transform:${transformDuration}ms ")
                if (commandDuration > 30) performanceWarning.append("Command:${commandDuration}ms ")
                if (totalProcessingTime > 50) performanceWarning.append("Total:${totalProcessingTime}ms ")
                if (timeSinceLastCycle > 150) performanceWarning.append("CycleDelay:${timeSinceLastCycle}ms ")
                
                if (performanceWarning.isNotEmpty()) {
                    logger.warn("🚨 SunTrack 성능 경고: {}", performanceWarning.toString())
                }
                
                // ✅ 상세 성능 로그 (INFO 레벨로 변경)
                logger.info("📊 SunTrack 성능 분석: CalTime={}ms, SunCalc={}ms, Transform={}ms, Command={}ms, Total={}ms, CycleDelay={}ms", 
                    calTimeDuration, sunCalcDuration, transformDuration, commandDuration, totalProcessingTime, timeSinceLastCycle)
                
                logger.info("[CalTime] 원본 태양 위치: Az={}°, El={}° (CalTime={})", 
                    String.format("%.3f", sunPosition.azimuthDegrees),
                    String.format("%.3f", sunPosition.elevationDegrees),
                    calTime)
                logger.info("[CalTime] 3축 변환 후: Az={}°, El={}° (Tilt={}°, Train={}°)", 
                    String.format("%.3f", transformedAz),
                    String.format("%.3f", transformedEl),
                    String.format("%.3f", -6.98),
                    String.format("%.3f", rotatorAngle))
                logger.info("[CalTime] 범용 Azimuth 계산: 태양={}°, Train={}°, 상대Az={}°", 
                    String.format("%.3f", sunPosition.azimuthDegrees),
                    String.format("%.3f", rotatorAngle),
                    String.format("%.3f", relativeAzimuth))                
                
                logger.debug("[CalTime] 범용 태양 추적: CalTime={}, 원본 Az={}°, El={}° → 3축변환 Az={}°, El={}° → 상대Az={}°, Tilt={}°, Train={}°, 처리시간={}ms, 주기지연={}ms", 
                    calTime.toString(),
                    String.format("%.6f", sunPosition.azimuthDegrees),
                    String.format("%.6f", sunPosition.elevationDegrees),
                    String.format("%.6f", transformedAz),
                    String.format("%.6f", transformedEl),
                    String.format("%.6f", relativeAzimuth),
                    String.format("%.3f", -6.98), // 실제 Tilt 기울기
                    String.format("%.3f", rotatorAngle), // Train 축 회전 각도
                    totalProcessingTime,
                    timeSinceLastCycle)
                
            } else {
                logger.error("Train 회전 각도 정보를 가져올 수 없습니다: {}", rotatorAngle)
                dataStoreService.setSunTracking(false)
            }
            
        } catch (e: Exception) {
            val errorDuration = System.currentTimeMillis() - totalStartTime
            logger.error("실시간 태양 추적 처리 중 오류 (처리시간: {}ms): {}", errorDuration, e.message, e)
            dataStoreService.setSunTracking(false)
        }
    }
    fun sendAzimuthAndElevationAxisCommand(cmdAzimuthAngle: Float, cmdAzimuthSpeed: Float, cmdElevationAngle: Float, cmdElevationSpeed: Float, cmdTiltAngle: Float) {
        CMD.cmdTiltAngle = targetTiltAngle!!.toFloat()
        val multiAxis = BitSet()
        multiAxis.set(0) // azimuth
        multiAxis.set(1) // elevation
        
        udpFwICDService.multiManualCommand(
            multiAxis,
            cmdAzimuthAngle,
            cmdAzimuthSpeed,
            cmdElevationAngle,
            cmdElevationSpeed,
            cmdTiltAngle,
            0.0f
        )
    }
    /**
     * ✅ Tilt 이동 명령 전송
     */
    private fun sendTiltMovementCommand(targetAngle: Double) {
        try {
            val cmdAzimuthSpeed = 0.0f  // Azimuth 이동 안함
            val cmdElevationSpeed = 0.0f // Elevation 이동 안함
            val cmdTiltSpeed = 5.0f      // Tilt만 이동
            
            val multiAxis = BitSet()
            // multiAxis.set(0) // azimuth (이동 안함)
            // multiAxis.set(1) // elevation (이동 안함)
            multiAxis.set(2) // tilt만 이동
            
            udpFwICDService.multiManualCommand(
                multiAxis,
                0.0f, // azimuth 각도 (이동 안함)
                cmdAzimuthSpeed,
                0.0f, // elevation 각도 (이동 안함)
                cmdElevationSpeed,
                targetAngle.toFloat(), // 목표 Tilt 각도
                cmdTiltSpeed
            )

            logger.info("Tilt 이동 명령 전송: {}도", String.format("%.6f", targetAngle))
            
        } catch (e: Exception) {
            logger.error("Tilt 이동 명령 전송 실패: {}", e.message, e)
            throw e
        }
    }

    fun sendTiltAxisCommand(cmdTiltAngle: Float, cmdTiltSpeed: Float) {
        val multiAxis = BitSet()
        multiAxis.set(2) // tilt
        
        udpFwICDService.multiManualCommand(
            multiAxis,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            cmdTiltAngle,
            cmdTiltSpeed
        )
    }
    /**
     * ✅ Sun Track 시작
     */
    fun startSunTrack() {
        try {
            logger.info("Sun Track 시작 (개선된 버전)")
            
            // ✅ 상태 초기화
            sunTrackState = SunTrackState.INITIAL_TILT
            targetTiltAngle = null
            tiltStabilizationStartTime = null
            isInitialTiltMovementCompleted = false
            
            // ✅ 추적 상태 설정
            trackingStatus.sunTrackStatus = true
            trackingStatus.sunTrackTrackingState = "TILT_MOVING_TO_ZERO"
            
            // ✅ 상태 업데이트 로그 추가
            logger.info("☀️ Sun Track 시작 - 상태 설정: status={}, trackingState={}", 
                trackingStatus.sunTrackStatus, trackingStatus.sunTrackTrackingState)
            
            // 기존 타이머 정리
            stopModeTimer()
            
            // 새 타이머 시작
            startModeTimer()
            
            // 상태 업데이트
            dataStoreService.setSunTracking(true)
            
            logger.info("Sun Track 시작 완료 - 초기 Tilt 이동 단계로 진입")

        } catch (e: Exception) {
            logger.error("Sun Track 시작 실패: {}", e.message, e)
            sunTrackState = SunTrackState.IDLE
            trackingStatus.sunTrackStatus = false
            trackingStatus.sunTrackTrackingState = "IDLE"
            throw e
        }
    }

    /**
     * ✅ Sun Track 중지
     */
    fun stopSunTrack() {
        try {
            logger.info("Sun Track 중지")
            
            // 타이머 중지
            stopModeTimer()
            
            // ✅ 상태 초기화
            sunTrackState = SunTrackState.IDLE
            targetTiltAngle = null
            tiltStabilizationStartTime = null
            isInitialTiltMovementCompleted = false
            
            // ✅ 추적 상태 초기화
            trackingStatus.sunTrackStatus = false
            trackingStatus.sunTrackTrackingState = "IDLE"
            
            // ✅ 모든 축 정지 명령 전송
                val allAxes = BitSet()
            allAxes.set(0) // azimuth
            allAxes.set(1) // elevation
            allAxes.set(2) // tilt
            
                udpFwICDService.stopCommand(allAxes)

            // 상태 업데이트
            dataStoreService.setSunTracking(false)
            
            logger.info("Sun Track 중지 완료")
            
        } catch (e: Exception) {
            logger.error("Sun Track 중지 실패: {}", e.message, e)
            throw e
        }
    }

    /**
     * ✅ Sun Track 상태 조회
     */
    fun isSunTrackActive(): Boolean {
        return modeTask != null && !modeTask!!.isCancelled
    }

    /**
     * ✅ 성능 정보 조회
     */
    fun getPerformanceInfo(): Map<String, Any> {
        return mapOf(
            "lastProcessingTime" to lastProcessingTime,
            "isActive" to isSunTrackActive(),
            "threadName" to "SunTrackMonitor",
            "monitoringInterval" to "100ms",
            "sunTrackState" to sunTrackState.name,
            "isInitialTiltMovementCompleted" to isInitialTiltMovementCompleted
        )
    }

    /**
     * ✅ Tilt 각도 정보 조회
     */
    fun getTiltAngleInfo(): Map<String, Any?> {
        val currentTiltAngle = dataStoreService.getLatestData().tiltAngle
        
        return mapOf(
            "currentTiltAngle" to currentTiltAngle,
            "targetTiltAngle" to targetTiltAngle,
            "angleDifference" to if (currentTiltAngle != null && targetTiltAngle != null) {
                Math.abs(currentTiltAngle - targetTiltAngle!!)
            } else null,
            "isReached" to isTiltAngleReached(),
            "sunTrackState" to sunTrackState.name,
            "stabilizationStartTime" to tiltStabilizationStartTime,
            "stabilizationDuration" to if (tiltStabilizationStartTime != null) {
                System.currentTimeMillis() - tiltStabilizationStartTime!!
            } else null
        )
    }

    /**
     * ✅ 일출/일몰 방향 기반 Azimuth 정보 조회
     */
    fun getAzimuthLimitInfo(): Map<String, Any?> {
        val currentAzimuth = dataStoreService.getLatestData().azimuthAngle
        val currentTiltAngle = dataStoreService.getLatestData().tiltAngle
        
        return mapOf(
            "currentAzimuth" to currentAzimuth,
            "currentTiltAngle" to currentTiltAngle,
            "rotatorAngle" to rotatorAngle,
            "sunriseAzimuth" to sunriseAzimuth,
            "sunsetAzimuth" to sunsetAzimuth,
            "isSouthPath" to isSouthPath,
            "sunPathType" to if (isSouthPath != null) (if (isSouthPath!!) "동→남→서" else "동→북→서") else "미설정",
            "azimuthCalculationType" to "일출/일몰 방향 기반 (동→남→서: 270도 넘으면 음수, 동→북→서: 양수)",
            "sunTrackState" to sunTrackState.name,
            "targetTiltAngle" to targetTiltAngle
        )
    }

    /**
     * ✅ Tilt 각도 도착 확인
     */
    private fun isTiltAngleReached(): Boolean {
        val currentTiltAngle = dataStoreService.getLatestData().tiltAngle
        val tolerance = 0.5 // ±0.5도 허용 오차
        
        return if (currentTiltAngle != null && targetTiltAngle != null) {
            val angleDifference = Math.abs(currentTiltAngle - targetTiltAngle!!)
            angleDifference <= tolerance
        } else {
            false
        }
    }

    /**
     * ✅ 오늘 일출/일몰 정보 조회
     */
    fun getTodaySunInfo(): Map<String, Any> {
        try {
            logger.info("오늘 일출/일몰 정보 조회")
            return solarOrekitCalculator.getTodaySunriseAndSunset()
        } catch (e: Exception) {
            logger.error("오늘 일출/일몰 정보 조회 실패: {}", e.message, e)
            return mapOf(
                "error" to "일출/일몰 정보 조회에 실패했습니다",
                "message" to (e.message ?: "알 수 없는 오류")
            )
        }
    }

    /**
     * ✅ 특정 날짜 일출/일몰 정보 조회
     */
    fun getSunInfoForDate(date: LocalDateTime): Map<String, Any> {
        try {
            logger.info("특정 날짜 일출/일몰 정보 조회: {}", date)
            return solarOrekitCalculator.getSunriseAndSunsetForDate(date)
        } catch (e: Exception) {
            logger.error("특정 날짜 일출/일몰 정보 조회 실패: {}", e.message, e)
            return mapOf(
                "error" to "일출/일몰 정보 조회에 실패했습니다",
                "message" to (e.message ?: "알 수 없는 오류"),
                "date" to date.toString()
            )
        }
    }

    /**
     * ✅ 오늘 일출 정보만 조회
     */
    fun getTodaySunrise(): Map<String, Any> {
        try {
            logger.info("오늘 일출 정보 조회")
            val sunrise = solarOrekitCalculator.getTodaySunrise()
            return sunrise?.let {
                mapOf(
                    "success" to true,
                    "time" to it.dateTime.toString(),
                    "azimuth_degrees" to String.format("%.6f", it.azimuthDegrees),
                    "elevation_degrees" to String.format("%.6f", it.elevationDegrees),
                    "range_km" to String.format("%.3f", it.rangeKm),
                    "is_visible" to it.isSunVisible()
                )
            } ?: mapOf(
                "success" to false,
                "message" to "오늘 일출이 없습니다"
            )
        } catch (e: Exception) {
            logger.error("오늘 일출 정보 조회 실패: {}", e.message, e)
            return mapOf(
                "success" to false,
                "error" to "일출 정보 조회에 실패했습니다",
                "message" to (e.message ?: "알 수 없는 오류")
            )
        }
    }

    /**
     * ✅ 오늘 일몰 정보만 조회
     */
    fun getTodaySunset(): Map<String, Any> {
        try {
            logger.info("오늘 일몰 정보 조회")
            val sunset = solarOrekitCalculator.getTodaySunset()
            return sunset?.let {
                mapOf(
                    "success" to true,
                    "time" to it.dateTime.toString(),
                    "azimuth_degrees" to String.format("%.6f", it.azimuthDegrees),
                    "elevation_degrees" to String.format("%.6f", it.elevationDegrees),
                    "range_km" to String.format("%.3f", it.rangeKm),
                    "is_visible" to it.isSunVisible()
                )
            } ?: mapOf(
                "success" to false,
                "message" to "오늘 일몰이 없습니다"
            )
        } catch (e: Exception) {
            logger.error("오늘 일몰 정보 조회 실패: {}", e.message, e)
            return mapOf(
                "success" to false,
                "error" to "일몰 정보 조회에 실패했습니다",
                "message" to (e.message ?: "알 수 없는 오류")
            )
        }
    }

    /**
     * ✅ Azimuth 최단 경로 계산 (동→서 이동 시 음수 각도 적용)
     * 현재 각도에서 목표 각도까지의 최단 경로를 계산하여 동에서 서로 이동할 때 음수 각도 사용
     */
    private fun calculateShortestAzimuthPath(currentAzimuth: Double, targetAzimuth: Double): Double {
        // 각도 차이 계산
        var angleDifference = targetAzimuth - currentAzimuth
        
        // ±180도 범위로 정규화 (최단 경로)
        while (angleDifference > 180.0) angleDifference -= 360.0
        while (angleDifference < -180.0) angleDifference += 360.0
        
        // 최종 목표 각도 계산
        val finalTarget = currentAzimuth + angleDifference
        
        logger.debug("Azimuth 최단 경로 계산: 현재={}°, 목표={}°, 차이={}°, 최종={}°", 
            String.format("%.3f", currentAzimuth),
            String.format("%.3f", targetAzimuth),
            String.format("%.3f", angleDifference),
            String.format("%.3f", finalTarget))
        
        return finalTarget
    }

}

