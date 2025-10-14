package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.axistransformation.CoordinateTransformer
import com.gtlsystems.acs_api.algorithm.suntrack.impl.SolarOrekitCalculator
import com.gtlsystems.acs_api.config.ThreadManager
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.model.PushData.CMD
import com.gtlsystems.acs_api.service.system.settings.SettingsService
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

@Service
class SunTrackService(
    private val udpFwICDService: UdpFwICDService,
    private val dataStoreService: com.gtlsystems.acs_api.service.datastore.DataStoreService,
    private val threadManager: ThreadManager,
    private val solarOrekitCalculator: SolarOrekitCalculator,
    private val settingsService: SettingsService
) {
    private val logger = LoggerFactory.getLogger(SunTrackService::class.java)

    // ✅ ThreadManager 통합 사용
    private var trackingExecutor: ScheduledExecutorService? = null
    private var modeTask: ScheduledFuture<*>? = null

    // ✅ 성능 모니터링
    private var lastProcessingTime = 0L
    private var processingTimeWarningThreshold = 50L // 50ms 이상이면 경고
    private var lastTrackingTime: Long? = null // 마지막 추적 시간

    // ✅ SunTrack 상태 관리 (핵심 변수만)
    private var sunTrackState = SunTrackState.IDLE
    private var targetTrainAngle: Double? = null
    private var trainStabilizationStartTime: Long? = null
    private var isInitialTrainMovementCompleted = false

    // ✅ 추적 상태 참조
    private val trackingStatus = PushData.TRACKING_STATUS

    // ✅ SunTrack 상태 열거형
    enum class SunTrackState {
        IDLE,           // 대기 상태
        INITIAL_Train,   // 초기 Train 이동 중
        STABILIZING,    // Train 안정화 대기 중
        TRACKING        // 실시간 태양 추적 중
    }

    // ✅ Train 각도 계산 결과
    data class TrainAngleResult(
        val angle: Double,
        val calculationMethod: String
    )

    // ✅ 속도 설정 변수 추가
    private var azimuthSpeed: Float = 1.0f
    private var elevationSpeed: Float = 1.0f
    private var trainSpeed: Float = 5.0f

    @PostConstruct
    fun init() {
        logger.info("SunTrackService 초기화 시작")
        // ✅ 통합 추적 실행기 사용 (NORMAL 우선순위)
        trackingExecutor = threadManager.getTrackingExecutor()
        
        // ✅ 지상국 초기화
        try {
            solarOrekitCalculator.initializeGroundStation(
                settingsService.locationData.latitude,
                settingsService.locationData.longitude,
                settingsService.locationData.altitude
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
     * ✅ 모드 타이머 시작
     */
    private fun startModeTimer() {
        if (modeTask != null && !modeTask!!.isCancelled) {
            logger.warn("모드 타이머가 이미 실행 중입니다")
            return
        }

        try {
            modeTask = trackingExecutor?.scheduleAtFixedRate(
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
                100, // 100ms 간격
                TimeUnit.MILLISECONDS
            )
            
            logger.info("Sun Track 모드 타이머 시작 (100ms 간격)")
            
        } catch (e: Exception) {
            logger.error("Sun Track 모드 타이머 시작 실패: {}", e.message, e)
        }
    }

    /**
     * ✅ 모드 타이머 중지
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
                trackingStatus.sunTrackTrackingState = "IDLE"
                logger.debug("☀️ Sun Track 상태: IDLE")
            }
            
            SunTrackState.INITIAL_Train -> {
                trackingStatus.sunTrackTrackingState = "TRAIN_MOVING_TO_ZERO"
                logger.debug("☀️ Sun Track 상태: TRAIN_MOVING_TO_ZERO")
                processInitialTrainMovement()
            }
            
            SunTrackState.STABILIZING -> {
                trackingStatus.sunTrackTrackingState = "TRAIN_STABILIZING"
                logger.debug("☀️ Sun Track 상태: TRAIN_STABILIZING")
                processTrainStabilization()
            }
            
            SunTrackState.TRACKING -> {
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
     * ✅ Train 각도를 기계적 제한 범위(±270도)로 정규화
     */
    private fun normalizeTrainAngleToMechanicalLimits(angle: Double): Double {
        return when {
            angle > 270.0 -> {
                val normalized = angle - 360.0
                logger.debug("Train 각도 정규화: {}° → {}° (역방향)", 
                    String.format("%.1f", angle),
                    String.format("%.1f", normalized))
                normalized
            }
            angle < -270.0 -> {
                val normalized = angle + 360.0
                logger.debug("Train 각도 정규화: {}° → {}° (정방향)", 
                    String.format("%.1f", angle),
                    String.format("%.1f", normalized))
                normalized
            }
            else -> {
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
     * ✅ 단순화된 Train 각도 계산 (정오 태양 방위각 기반)
     */
    private fun calculateTrainAngleByMiddleOfSunriseAndSunset(): TrainAngleResult {
        val todaySunInfo = solarOrekitCalculator.getTodaySunriseAndSunset()
        val sunriseInfo = todaySunInfo["sunrise"]
        val sunsetInfo = todaySunInfo["sunset"]
        
        if (sunriseInfo is Map<*, *> && sunsetInfo is Map<*, *>) {
            val sunriseAzimuth = (sunriseInfo["azimuth_degrees"] as String).toDouble()
            val sunsetAzimuth = (sunsetInfo["azimuth_degrees"] as String).toDouble()
            val sunriseTime = sunriseInfo["time"] as String
            val sunsetTime = sunsetInfo["time"] as String
            
            // ✅ UTC → KST 변환 함수
            fun utcToKst(utcTimeStr: String): String {
                val utcDateTime = LocalDateTime.parse(utcTimeStr)
                val kstDateTime = utcDateTime.atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                return kstDateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }
            
            // ✅ 실제 정오 시간 계산 (현지 12:00 기준)
            val todayDate = LocalDateTime.now(ZoneOffset.UTC).toLocalDate()
            val longitude = settingsService.locationData.longitude
            val utcOffsetHours = longitude / 15.0  // 경도 15도 = 1시간
            val utcNoon = todayDate.atTime(12, 0).minusHours(utcOffsetHours.toLong()).minusMinutes(((utcOffsetHours % 1) * 60).toLong())
            
            // ✅ 정오 시 태양 방위각 확인 (핵심 로직!)  
            val noonSunPosition = solarOrekitCalculator.getSunPositionAt(utcNoon)
            val noonAzimuth = noonSunPosition.azimuthDegrees
            
            // ✅ 단순화된 Train 각도 결정
            val (trainAngle, pathType) = if (noonAzimuth >= 135.0 && noonAzimuth <= 225.0) {
                // 정오에 남쪽 → 동남서 경로 → Train: 일출/일몰 중간
                val rawAngle = (sunriseAzimuth + sunsetAzimuth) / 2.0
                val finalAngle = normalizeTrainAngleToMechanicalLimits(rawAngle)
                finalAngle to "동남서 경로"
            } else {
                // 정오에 북쪽 → 동북서 경로 → Train: 0° 근처
                val adjustedSunset = if (sunsetAzimuth > 180.0) sunsetAzimuth - 360.0 else sunsetAzimuth
                val rawAngle = (sunriseAzimuth + adjustedSunset) / 2.0
                val finalAngle = normalizeTrainAngleToMechanicalLimits(rawAngle)
                finalAngle to "동북서 경로"
            }
            
            logger.info("🌅 단순화된 Train 각도 계산 완료:")
            logger.info("  📍 일출: {}° (UTC: {} | KST: {})", 
                String.format("%.3f", sunriseAzimuth), 
                sunriseTime, 
                utcToKst(sunriseTime))
            logger.info("  📍 일몰: {}° (UTC: {} | KST: {})", 
                String.format("%.3f", sunsetAzimuth), 
                sunsetTime, 
                utcToKst(sunsetTime))
            logger.info("  📍 정오: {}° (UTC: {} | KST: {})", 
                String.format("%.3f", noonAzimuth), 
                utcNoon.toString(), 
                utcToKst(utcNoon.toString()))
            logger.info("  🎯 경로: {} → Train 각도: {}°", pathType, String.format("%.3f", trainAngle))
            
            return TrainAngleResult(trainAngle, "단순화 로직 ($pathType)")
        } else {
            throw RuntimeException("일출/일몰 정보를 가져올 수 없습니다: $todaySunInfo")
        }
    }

    /**
     * ✅ 단순화된 전세계 Train 각도 계산 (일출 존재 여부 기반)
     */
    private fun calculateOptimalTrainAngleUniversal(): TrainAngleResult {
        try {
            // ✅ 일출/일몰 정보 확인
            val todaySunInfo = solarOrekitCalculator.getTodaySunriseAndSunset()
            val sunriseInfo = todaySunInfo["sunrise"]
            val currentSun = solarOrekitCalculator.getCurrentSunPosition()
            
            logger.info("🌍 전세계 Train 각도 계산: 현재 태양 고도={}°", 
                String.format("%.3f", currentSun.elevationDegrees))
            
            return when {
                sunriseInfo != "일출 없음" -> {
                    // ✅ 정상 케이스: 일출/일몰 존재 → 정오 방위각으로 판단
                    logger.info("📅 정상 지역: 일출/일몰 존재 → 정오 방위각 기반 계산")
                    calculateTrainAngleByMiddleOfSunriseAndSunset()
                }
                
                currentSun.elevationDegrees > 0 -> {
                    // ✅ 백야: 일출 없음 + 태양 보임
                    logger.info("☀️ 백야 지역: 24시간 태양 → 특수 처리")
                    calculateTrainAngleForMidnightSun()
                }
                
                else -> {
                    // ✅ 극야: 일출 없음 + 태양 안 보임
                    logger.info("🌑 극야 지역: 24시간 어둠 → 추적 중단")
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
     * ✅ 연속 추적을 위한 Azimuth 경로 조정 (핵심 수정!)
     * 동남서/동북서 모두 180도 넘으면 음수로 변환하여 한방향 연속 추적
     */
    private fun calculateAzimuthBySunPath(azimuth: Double): Double {
        return if (azimuth > 180.0) {
            val negativeAzimuth = azimuth - 360.0
            logger.debug("연속 추적: {}° → {}° (음수 변환)", 
                String.format("%.3f", azimuth),
                String.format("%.3f", negativeAzimuth))
            negativeAzimuth
        } else {
            logger.debug("연속 추적: {}° (양수 유지)", String.format("%.3f", azimuth))
            azimuth
        }
    }

    /**
     * ✅ 초기 Train 이동 처리
     */
    private fun processInitialTrainMovement() {
        try {
            if (targetTrainAngle == null) {
                // ✅ 통합된 범용 Train 각도 계산 사용
                val trainResult = calculateOptimalTrainAngleUniversal()
                
                if (trainResult.angle.isNaN()) {
                    // 극야 등으로 Train 계산 불가능
                    logger.warn("Train 각도 계산 불가능: {}", trainResult.calculationMethod)
                    sunTrackState = SunTrackState.IDLE
                    return
                }
                
                targetTrainAngle = trainResult.angle
                CMD.cmdTrainAngle = getTrainOffsetCalculator()!!.toFloat()
                logger.info("개선된 Train 각도 설정 완료: {}° ({})", 
                    String.format("%.3f", trainResult.angle),
                    trainResult.calculationMethod)
                
                // ✅ Train 이동 명령 전송
                GlobalData.SunTrackingData.trainAngle = targetTrainAngle?.toFloat()!!
                sendTrainMovementCommand(targetTrainAngle?.toFloat()!!, trainSpeed)
                    
                // ✅ 안정화 단계로 전환
                sunTrackState = SunTrackState.STABILIZING
                trainStabilizationStartTime = System.currentTimeMillis()
                
                logger.info("Train 이동 명령 전송 완료, 안정화 단계 진입")
            } else {
                // ✅ targetTrainAngle이 이미 설정되어 있으면 매번 목표 각도 도달 확인
                val currentTrainAngle = dataStoreService.getLatestData().trainAngle
                val moveTolerance = 1.0 // ±1.0도 허용
                
                if (currentTrainAngle != null && targetTrainAngle != null) {
                    val angleDifference = Math.abs(currentTrainAngle - getTrainOffsetCalculator()!!.toFloat())
                    
                    logger.debug("Train 목표 각도 확인 중: 현재={}°, 목표={}°, 차이={}°",
                        String.format("%.3f", currentTrainAngle),
                        String.format("%.3f", targetTrainAngle),
                        String.format("%.3f", angleDifference))
                    
                    // ✅ 목표 각도 도달 시 STABILIZING 상태로 전환
                    if (angleDifference <= moveTolerance) {
                        logger.info("Train 목표 각도 도달: 현재={}°, 목표={}°, 차이={}° (허용오차: ±{}°)",
                            String.format("%.3f", currentTrainAngle),
                            String.format("%.3f", targetTrainAngle),
                            String.format("%.3f", angleDifference),
                            moveTolerance)
                        
                        sunTrackState = SunTrackState.STABILIZING
                        trainStabilizationStartTime = System.currentTimeMillis()
                        logger.info("Train 안정화 단계 시작")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("초기 Train 이동 처리 중 오류: {}", e.message, e)
            sunTrackState = SunTrackState.IDLE
        }
    }

    /**
     * ✅ Train 안정화 대기 처리
     */
    private fun processTrainStabilization() {
        try {
            val currentTrainAngle = dataStoreService.getLatestData().trainAngle
            val stabilizationTolerance = 0.5 // ±0.5도 허용

            if (currentTrainAngle != null && targetTrainAngle != null) {
                val angleDifference = Math.abs(currentTrainAngle - getTrainOffsetCalculator()!!.toFloat())
                
                if (sunTrackState == SunTrackState.STABILIZING) {
                    if (trainStabilizationStartTime == null) {
                        trainStabilizationStartTime = System.currentTimeMillis()
                        logger.info("Train 안정화 타이머 시작")
                        return
                    }

                    val currentTime = System.currentTimeMillis()
                    val stabilizationDuration = currentTime - trainStabilizationStartTime!!
                    
                    // ✅ 5초마다 로그 출력
                    if (stabilizationDuration % 5000 < 100) {
                        logger.debug("Train 안정화 대기: 현재={}°, 목표={}°, 차이={}°, 경과시간={}ms",
                            String.format("%.3f", currentTrainAngle),
                            String.format("%.3f", targetTrainAngle),
                            String.format("%.3f", angleDifference),
                            stabilizationDuration)
                    }
                    
                    // ✅ 1초 안정화 완료
                    if (stabilizationDuration >= 1000 && angleDifference <= stabilizationTolerance) {
                        logger.info("Train 안정화 완료: 현재={}°, 목표={}°, 차이={}°, 대기시간={}ms",
                            String.format("%.3f", currentTrainAngle),
                            String.format("%.3f", targetTrainAngle),
                            String.format("%.3f", angleDifference),
                            stabilizationDuration)
                        
                        // ✅ 실시간 추적 상태로 전환
                        sunTrackState = SunTrackState.TRACKING
                        isInitialTrainMovementCompleted = true
                        logger.info("Sun Track 실시간 추적 상태로 전환 완료")
                    } else if (stabilizationDuration > 300000) {
                        // ✅ 5분 후에도 안정화되지 못한 경우
                        logger.warn("Train 안정화 실패: 현재={}°, 목표={}°, 차이={}°, 대기시간={}ms",
                            String.format("%.3f", currentTrainAngle),
                            String.format("%.3f", targetTrainAngle),
                            String.format("%.3f", angleDifference),
                            stabilizationDuration)
                        
                        // ✅ 실패 시에도 추적 시작
                        sunTrackState = SunTrackState.TRACKING
                        isInitialTrainMovementCompleted = true
                        logger.info("Train 안정화 실패했지만 추적 시작")
                    }
                }
            } else {
                // ✅ 각도 데이터가 없으면 SunTrack을 정지
                logger.error("Train 각도 데이터 없음. 현재={}, 목표={}. SunTrack을 정지합니다.",
                    currentTrainAngle, targetTrainAngle)
                sunTrackState = SunTrackState.IDLE
                targetTrainAngle = null
                trainStabilizationStartTime = null
            }
        } catch (e: Exception) {
            logger.error("Train 안정화 처리 중 오류: {}", e.message, e)
            sunTrackState = SunTrackState.IDLE
        }
    }

    /**
     * ✅ 실시간 태양 추적 처리 (수정된 연속 추적 로직)
     */
    private fun processRealTimeSunTracking() {
        val totalStartTime = System.currentTimeMillis()
        
        try {
            if (targetTrainAngle?.toFloat() != null) {
                // 1단계: Cal Time 계산
                val calTimeStart = System.currentTimeMillis()
                val calTime = GlobalData.Time.resultTimeOffsetCalTime
                val utcLocalDateTime = calTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                val calTimeDuration = System.currentTimeMillis() - calTimeStart
                
                // 2단계: 태양 위치 계산
                val sunCalcStart = System.currentTimeMillis()
                val sunPosition = solarOrekitCalculator.getSunPositionAt(utcLocalDateTime)
                val sunCalcDuration = System.currentTimeMillis() - sunCalcStart
                
                // 3단계: 3축 좌표 변환
                val transformStart = System.currentTimeMillis()
                val (transformedAz, transformedEl) = CoordinateTransformer.transformCoordinatesWithTrain(
                    azimuth = sunPosition.azimuthDegrees,
                    elevation = sunPosition.elevationDegrees,
                    tiltAngle = settingsService.tiltAngle,
                    trainAngle = targetTrainAngle!!
                )
                val transformDuration = System.currentTimeMillis() - transformStart
                
                // ✅ 4단계: 연속 추적을 위한 Azimuth 경로 조정 (핵심 수정!)
                val pathAdjustedAzimuth = calculateAzimuthBySunPath(transformedAz)
                
                // 5단계: 명령 전송
                val commandStart = System.currentTimeMillis()
                sendAzimuthAndElevationAxisCommand(
                    pathAdjustedAzimuth.toFloat(), 
                    azimuthSpeed,
                    transformedEl.toFloat(), 
                    elevationSpeed,
                    targetTrainAngle!!.toFloat(),
                    trainSpeed
                )
                val commandDuration = System.currentTimeMillis() - commandStart
                
                // ✅ 데이터 스토어 업데이트
                dataStoreService.setSunTracking(true)
                
                // ✅ 전체 성능 분석
                val totalEndTime = System.currentTimeMillis()
                val totalProcessingTime = totalEndTime - totalStartTime
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCycle = if (lastTrackingTime != null) currentTime - lastTrackingTime!! else 0L
                lastTrackingTime = currentTime
                
                // ✅ 성능 경고
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
                
                logger.info("📊 SunTrack 성능: CalTime={}ms, SunCalc={}ms, Transform={}ms, Command={}ms, Total={}ms", 
                    calTimeDuration, sunCalcDuration, transformDuration, commandDuration, totalProcessingTime)
                
                logger.info("[CalTime] 원본 태양 위치: Az={}°, El={}°", 
                    String.format("%.3f", sunPosition.azimuthDegrees),
                    String.format("%.3f", sunPosition.elevationDegrees))
                logger.info("[CalTime] 3축 변환 후: Az={}°, El={}° (Tilt={}°, Train={}°)", 
                    String.format("%.3f", transformedAz),
                    String.format("%.3f", transformedEl),
                    String.format("%.3f", settingsService.tiltAngle),
                    String.format("%.3f", targetTrainAngle?.toFloat()!!))
                logger.info("[CalTime] 연속 추적: {}° → {}°", 
                    String.format("%.3f", transformedAz),
                    String.format("%.3f", pathAdjustedAzimuth))
                
            } else {
                logger.error("Train 회전 각도 정보를 가져올 수 없습니다: targetTrainAngle?.toFloat()가 null입니다")
                dataStoreService.setSunTracking(false)
            }
            
        } catch (e: Exception) {
            val errorDuration = System.currentTimeMillis() - totalStartTime
            logger.error("실시간 태양 추적 처리 중 오류 (처리시간: {}ms): {}", errorDuration, e.message, e)
            dataStoreService.setSunTracking(false)
        }
    }

  fun getTrainOffsetCalculator(): Double? {
        val offsetAppliedAngle = targetTrainAngle?.let { targetAngle ->
            targetAngle.toFloat() + GlobalData.Offset.trainPositionOffset + GlobalData.Offset.trueNorthOffset
        }
        
        return if (offsetAppliedAngle != null) {
            //CMD.cmdTrainAngle = offsetAppliedAngle
            offsetAppliedAngle.toDouble()
        } else {
            null
        }
    }

    /**
     * ✅ Azimuth와 Elevation 축 명령 전송
     */
    fun sendAzimuthAndElevationAxisCommand(cmdAzimuthAngle: Float, cmdAzimuthSpeed: Float, cmdElevationAngle: Float, cmdElevationSpeed: Float, cmdTrainAngle: Float, cmdTrainSpeed: Float) {
        //CMD.cmdTiltAngle = targetTrainAngle!!.toFloat()
        //CMD.cmdAzimuthAngle = cmdAzimuthAngle
        //CMD.cmdElevationAngle = cmdElevationAngle
        val multiAxis = BitSet()
        multiAxis.set(0) // azimuth
        multiAxis.set(1) // elevation
        //multiAxis.set(2) // train
        GlobalData.SunTrackingData.azimuthSpeed = cmdAzimuthSpeed
        GlobalData.SunTrackingData.elevationSpeed = cmdElevationSpeed
        udpFwICDService.multiManualCommand(
            multiAxis,
            cmdAzimuthAngle,
            cmdAzimuthSpeed,
            cmdElevationAngle,
            cmdElevationSpeed,
            cmdTrainAngle,
            cmdTrainSpeed
        )
    }

    /**
     * ✅ Train 이동 명령 전송
     */
    private fun sendTrainMovementCommand(targetAngle: Float, trainSpeed: Float) {
        try {
            val multiAxis = BitSet()
            multiAxis.set(2) // Train만 이동
            GlobalData.SunTrackingData.trainSpeed = trainSpeed
            udpFwICDService.singleManualCommand(
                multiAxis,
                targetAngle, // 목표 Train 각도
                trainSpeed // Train 속도
            )

            logger.info("Train 이동 명령 전송: {}도", String.format("%.6f", targetAngle))
            
        } catch (e: Exception) {
            logger.error("Train 이동 명령 전송 실패: {}", e.message, e)
            throw e
        }
    }
    // ✅ 속도 설정 메서드 추가
    fun setSpeeds(azimuthSpeed: Float, elevationSpeed: Float, trainSpeed: Float) {
        this.azimuthSpeed = azimuthSpeed
        this.elevationSpeed = elevationSpeed
        this.trainSpeed = trainSpeed
        logger.info("Sun Track 속도 설정: Az={}°/s, El={}°/s, Train={}°/s",
            azimuthSpeed, elevationSpeed, trainSpeed)
    }
    /**
     * ✅ Sun Track 시작
     */
   // ✅ startSunTrack 메서드 수정
   fun startSunTrack(azimuthSpeed: Float, elevationSpeed: Float, trainSpeed: Float) {
    try {
        logger.info("Sun Track 시작 (개선된 버전)")
        
        // ✅ 속도 설정
        setSpeeds(azimuthSpeed, elevationSpeed, trainSpeed)
        
            
            // ✅ 상태 초기화
            sunTrackState = SunTrackState.INITIAL_Train
            targetTrainAngle = null
            trainStabilizationStartTime = null
            isInitialTrainMovementCompleted = false
            
            // ✅ 추적 상태 설정
            trackingStatus.sunTrackStatus = true
            trackingStatus.sunTrackTrackingState = "TRAIN_MOVING_TO_ZERO"
            
            logger.info("☀️ Sun Track 시작 - 상태 설정: status={}, trackingState={}", 
                trackingStatus.sunTrackStatus, trackingStatus.sunTrackTrackingState)
            
            // 기존 타이머 정리
            stopModeTimer()
            
            // 새 타이머 시작
            startModeTimer()
            
            // 상태 업데이트
            dataStoreService.setSunTracking(true)
            
            logger.info("Sun Track 시작 완료 - 초기 Train 이동 단계로 진입")

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
            targetTrainAngle = null
            trainStabilizationStartTime = null
            isInitialTrainMovementCompleted = false
            
            // ✅ 추적 상태 초기화
            trackingStatus.sunTrackStatus = false
            trackingStatus.sunTrackTrackingState = "IDLE"
            
            // ✅ 모든 축 정지 명령 전송
            val allAxes = BitSet()
            allAxes.set(0) // azimuth
            allAxes.set(1) // elevation
            allAxes.set(2) // Train
            
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
            "isInitialTrainMovementCompleted" to isInitialTrainMovementCompleted
        )
    }

    /**
     * ✅ Train 각도 정보 조회
     */
    fun getTrainAngleInfo(): Map<String, Any?> {
        val currentTrainAngle = dataStoreService.getLatestData().trainAngle
        
        return mapOf(
            "currentTrainAngle" to currentTrainAngle,
            "targetTrainAngle" to targetTrainAngle,
            "angleDifference" to if (currentTrainAngle != null && targetTrainAngle != null) {
                Math.abs(currentTrainAngle - targetTrainAngle!!)
            } else null,
            "isReached" to isTrainAngleReached(),
            "sunTrackState" to sunTrackState.name,
            "stabilizationStartTime" to trainStabilizationStartTime,
            "stabilizationDuration" to if (trainStabilizationStartTime != null) {
                System.currentTimeMillis() - trainStabilizationStartTime!!
            } else null
        )
    }

    /**
     * ✅ Train 각도 도착 확인
     */
    private fun isTrainAngleReached(): Boolean {
        val currentTrainAngle = dataStoreService.getLatestData().trainAngle
        val tolerance = 0.5 // ±0.5도 허용 오차
        
        return if (currentTrainAngle != null && targetTrainAngle != null) {
            val angleDifference = Math.abs(currentTrainAngle - targetTrainAngle!!)
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
}

