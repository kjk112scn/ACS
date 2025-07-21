package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.ElevationCalculator
import com.gtlsystems.acs_api.algorithm.axistransformation.CoordinateTransformer
import com.gtlsystems.acs_api.algorithm.suntrack.impl.SolarOrekitCalculator
import com.gtlsystems.acs_api.config.ThreadManager // ✅ ThreadManager 추가
import com.gtlsystems.acs_api.model.GlobalData
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

    // ✅ SunTrack 상태 열거형
    enum class SunTrackState {
        IDLE,           // 대기 상태
        INITIAL_TILT,   // 초기 Tilt 이동 중
        STABILIZING,    // Tilt 안정화 대기 중
        TRACKING        // 실시간 태양 추적 중
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
        when (sunTrackState) {
            SunTrackState.IDLE -> {
                // 대기 상태 - 아무것도 하지 않음
            }
            
            SunTrackState.INITIAL_TILT -> {
                // 초기 Tilt 이동 처리
                processInitialTiltMovement()
            }
            
            SunTrackState.STABILIZING -> {
                // Tilt 안정화 대기 처리
                processTiltStabilization()
            }
            
            SunTrackState.TRACKING -> {
                // 실시간 태양 추적 처리
                processRealTimeSunTracking()
            }
        }
    }

    /**
     * ✅ 초기 Tilt 이동 처리
     */
    private fun processInitialTiltMovement() {
        try {
            if (targetTiltAngle == null) {
                // ✅ 일출/일몰 가운데 Azimuth 각도 계산
                val todaySunInfo = solarOrekitCalculator.getTodaySunriseAndSunset()
                
                val sunriseInfo = todaySunInfo["sunrise"]
                val sunsetInfo = todaySunInfo["sunset"]
                
                if (sunriseInfo is Map<*, *> && sunsetInfo is Map<*, *>) {
                    // ✅ 일출/일몰 가운데 시간 계산 (한 번만)
                    val sunriseTime = LocalDateTime.parse(sunriseInfo["time"] as String)
                    val sunsetTime = LocalDateTime.parse(sunsetInfo["time"] as String)

                    // ✅ 일출/일몰 가운데 시간 계산
                    midTime = if (sunriseTime.isBefore(sunsetTime)) {
                        sunriseTime.plusSeconds(sunriseTime.until(sunsetTime, java.time.temporal.ChronoUnit.SECONDS) / 2)
                    } else {
                        sunsetTime.plusSeconds(sunsetTime.until(sunriseTime, java.time.temporal.ChronoUnit.SECONDS) / 2)
                    }
                    
                    // ✅ 가운데 시간의 태양 위치 계산하여 Rotator 각도 저장
                    // ✅ 현재 코드 (잘못된 방식):
                    // val sunPosition = solarOrekitCalculator.getSunPositionAt(midTime!!)
                    // rotatorAngle = sunPosition.azimuthDegrees

                    // ✅ 수정된 코드 (올바른 방식):
                    // 일출/일몰 Azimuth 각도의 가운데 계산
                    val sunriseAzimuth = (sunriseInfo["azimuth_degrees"] as String).toDouble()
                    val sunsetAzimuth = (sunsetInfo["azimuth_degrees"] as String).toDouble()

                    // 360도 경계 처리
                    val midAzimuth = if (sunriseAzimuth > sunsetAzimuth) {
                        val adjustedSunsetAzimuth = sunsetAzimuth + 360.0
                        (sunriseAzimuth + adjustedSunsetAzimuth) / 2.0
                    } else {
                        (sunriseAzimuth + sunsetAzimuth) / 2.0
                    }

                    // 360도 범위로 정규화
                    val normalizedMidAzimuth = (midAzimuth + 360.0) % 360.0
                    targetTiltAngle = normalizedMidAzimuth
                    CMD.cmdTiltAngle = normalizedMidAzimuth.toFloat()

                    // Rotator 각도도 동일하게 설정
                    rotatorAngle = normalizedMidAzimuth
                  
                    // ✅ 수정된 로그:
                    // 일출/일몰 Azimuth 각도의 가운데 계산
                    logger.info("일출/일몰 가운데 Azimuth 각도 계산 완료: 일출={}°, 일몰={}°, 가운데={}°", 
                        String.format("%.3f", sunriseAzimuth),
                        String.format("%.3f", sunsetAzimuth),
                        String.format("%.3f", normalizedMidAzimuth))

                    // ✅ Tilt 이동 명령 전송
                    sendTiltMovementCommand(rotatorAngle!!)
           
                    // ✅ 안정화 대기 상태로 전환
                    sunTrackState = SunTrackState.STABILIZING
                    tiltStabilizationStartTime = System.currentTimeMillis()
                    
                    logger.info("Tilt 이동 명령 전송 완료, 안정화 대기 시작")
                } else {
                    logger.error("일출/일몰 정보를 가져올 수 없습니다: {}", todaySunInfo)
                    sunTrackState = SunTrackState.IDLE
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
            if (tiltStabilizationStartTime == null) {
                tiltStabilizationStartTime = System.currentTimeMillis()
                return
            }

            val currentTime = System.currentTimeMillis()
            val stabilizationDuration = currentTime - tiltStabilizationStartTime!!
            val currentTiltAngle = dataStoreService.getLatestData().tiltAngle
            val tolerance = 0.5 // ±0.5도 허용

            if (currentTiltAngle != null && targetTiltAngle != null) {
                val angleDifference = Math.abs(currentTiltAngle - targetTiltAngle!!)
                
                logger.debug("Tilt 각도 비교: 현재={}°, 목표={}°, 차이={}°, 허용오차={}°", 
                    String.format("%.3f", currentTiltAngle),
                    String.format("%.3f", targetTiltAngle),
                    String.format("%.3f", angleDifference),
                    tolerance)
                
                // ✅ 목표 각도 도착 + 5초 안정화 완료
                if (angleDifference <= tolerance && stabilizationDuration >= 5000) {
                    logger.info("Tilt 안정화 완료: 현재={}°, 목표={}°, 차이={}°, 대기시간={}ms", 
                        String.format("%.3f", currentTiltAngle),
                        String.format("%.3f", targetTiltAngle),
                        String.format("%.3f", angleDifference),
                        stabilizationDuration)
                    
                    // ✅ 실시간 추적 상태로 전환
                    sunTrackState = SunTrackState.TRACKING
                    isInitialTiltMovementCompleted = true
                    
                    // ✅ 이 줄을 추가해야 합니다:
                    CMD.cmdTiltAngle = targetTiltAngle!!.toFloat()

                    // ✅ 상태 초기화
                    //targetTiltAngle = null
                    //tiltStabilizationStartTime = null
                } else if (angleDifference > tolerance && stabilizationDuration > 300000) {
                    // ✅ 5분 후에도 목표 각도에 도착하지 못한 경우
                    logger.warn("Tilt 목표 각도 도착 실패: 현재={}°, 목표={}°, 차이={}°, 대기시간={}ms", 
                        String.format("%.3f", currentTiltAngle),
                        String.format("%.3f", targetTiltAngle),
                        String.format("%.3f", angleDifference),
                        stabilizationDuration)
                    
                    // ✅ 실패 시에도 추적 시작 (안전장치)
                    sunTrackState = SunTrackState.TRACKING
                    isInitialTiltMovementCompleted = true
                    
                    //targetTiltAngle = null
                    //tiltStabilizationStartTime = null
                    CMD.cmdTiltAngle = targetTiltAngle!!.toFloat()
                } else {
                    logger.debug("Tilt 안정화 대기 중... 각도차이={}°, 경과시간={}ms", 
                        String.format("%.3f", angleDifference), stabilizationDuration)
                }
            } else {
                // ✅ 각도 데이터가 없으면 SunTrack을 정지(IDLE) 상태로 전환
                sunTrackState = SunTrackState.IDLE
                targetTiltAngle = null
                tiltStabilizationStartTime = null
                logger.error("Tilt 각도 데이터 없음. SunTrack을 정지합니다.")
            }
        } catch (e: Exception) {
            logger.error("Tilt 안정화 처리 중 오류: {}", e.message, e)
            sunTrackState = SunTrackState.IDLE
        }
    }

    /**
     * ✅ 실시간 태양 추적 처리 (일출/일몰 가운데 시간 기준)
     */
    private fun processRealTimeSunTracking() {
        val trackingStartTime = System.currentTimeMillis()
        
        try {
            // ✅ 저장된 값 사용 (재계산 없음)
            if (midTime != null && rotatorAngle != null) {
                val sunPosition = solarOrekitCalculator.getSunPositionAt(midTime!!)
                
                // ✅ Tilt가 이동한 각도만큼 보정된 Azimuth 계산
                val correctedAzimuth = if (targetTiltAngle != null) {
                    val adjustedAz = sunPosition.azimuthDegrees - targetTiltAngle!!
                    adjustedAz
                } else {
                    sunPosition.azimuthDegrees
                }
                
                val (transformedAz, transformedEl) = CoordinateTransformer.transformCoordinatesWithRotator(
                    azimuth = correctedAzimuth,
                    elevation = sunPosition.elevationDegrees,
                    tiltAngle = -6.98,
                    rotatorAngle = rotatorAngle!!
                )

                // ✅ CMD 업데이트 (변환된 좌표)
                CMD.cmdAzimuthAngle = transformedAz.toFloat()
                CMD.cmdElevationAngle = transformedEl.toFloat()
                //CMD.cmdTiltAngle = targetTiltAngle!!.toFloat()

                
                // ✅ 변환된 좌표로 명령 전송
                sendAzimuthAndElevationAxisCommand(transformedAz.toFloat(), 5.0f, transformedEl.toFloat(), 5.0f)
                
                // ✅ 데이터 스토어 업데이트
                dataStoreService.setSunTracking(true)
                
                // ✅ 성능 모니터링
                val trackingEndTime = System.currentTimeMillis()
                val processingTime = trackingEndTime - trackingStartTime
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCycle = if (lastTrackingTime != null) currentTime - lastTrackingTime!! else 0L
                lastTrackingTime = currentTime
                
                // ✅ 성능 경고 (50ms 이상이면 경고)
                if (processingTime > 50) {
                    logger.warn("SunTrack 처리 시간 경고: {}ms, 주기 지연: {}ms", processingTime, timeSinceLastCycle)
                }
                
                logger.debug("일출/일몰 가운데 시간 태양 추적: 가운데시간={}, 원본 Az={}°, El={}° → 보정 Az={}° → 변환 Az={}°, El={}°, Tilt={}°, Rotator={}°, 처리시간={}ms, 주기지연={}ms", 
                    midTime.toString(),
                    String.format("%.6f", sunPosition.azimuthDegrees),
                    String.format("%.6f", sunPosition.elevationDegrees),
                    String.format("%.6f", correctedAzimuth),
                    String.format("%.6f", transformedAz),
                    String.format("%.6f", transformedEl),
                    String.format("%.3f", -6.98), // Tilt 각도는 고정값
                    String.format("%.3f", rotatorAngle),
                    processingTime,
                    timeSinceLastCycle)
                
            } else {
                logger.error("일출/일몰 정보를 가져올 수 없습니다: {}", midTime)
                dataStoreService.setSunTracking(false)
            }
            
        } catch (e: Exception) {
            logger.error("실시간 태양 추적 처리 중 오류: {}", e.message, e)
            dataStoreService.setSunTracking(false)
        }
    }
    fun sendAzimuthAndElevationAxisCommand(cmdAzimuthAngle: Float, cmdAzimuthSpeed: Float, cmdElevationAngle: Float, cmdElevationSpeed: Float) {
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
            0.0f,
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
}

