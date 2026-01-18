package com.gtlsystems.acs_api.service.system

import com.gtlsystems.acs_api.config.ThreadManager
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.service.system.settings.SettingsService
import com.gtlsystems.acs_api.service.system.LoggingService
import com.gtlsystems.acs_api.tracking.entity.TrackingResultEntity
import com.gtlsystems.acs_api.tracking.service.TrackingDataService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * ✅ 배치 저장 관리자
 * 실시간 추적 데이터의 배치 처리를 담당
 *
 * Write-through 패턴:
 * - 메모리 캐시: 빠른 조회
 * - DB 저장: 영속성 보장 (tracking_result)
 *
 * @since Phase 6 - DB 연동 추가
 */
@Service
class BatchStorageManager(
    private val threadManager: ThreadManager,
    private val dataStoreService: DataStoreService,
    private val settingsService: SettingsService,
    private val loggingService: LoggingService,
    private val trackingDataService: TrackingDataService?
) {
    private val logger = LoggerFactory.getLogger(BatchStorageManager::class.java)
    
    // ✅ 배치 설정 (SettingsService에서 로드)
    private val batchSize: Int get() = settingsService.systemStorageBatchSize
    private val batchTimeoutMs: Long get() = settingsService.systemStorageSaveInterval
    private val maxBatchSize: Int get() = settingsService.systemStorageBatchSize
    
    // ✅ 배치 데이터 관리
    private val batchBuffer = mutableListOf<Map<String, Any?>>()
    private var batchExecutor: ExecutorService? = null
    private var lastBatchTime = System.currentTimeMillis()
    
    // ✅ 배치 저장 스케줄러
    private var batchScheduler: java.util.concurrent.ScheduledExecutorService? = null
    
    // ✅ 실시간 추적 데이터 리스트 (기존과 동일)
    private val realtimeTrackingDataList = mutableListOf<Map<String, Any?>>()
    private var trackingDataIndex = 0
    
    @PostConstruct
    fun init() {
        loggingService.logSystemStart("BatchStorageManager", "1.0.0")
        
        // ✅ 지연 초기화로 의존성 주입 문제 해결
        batchExecutor = threadManager.getBatchExecutor()
        batchScheduler = threadManager.getRealtimeExecutor()
        
        // ✅ 주기적 배치 저장 스케줄링 (SettingsService에서 간격 로드)
        val saveInterval = settingsService.systemStorageSaveInterval
        batchScheduler?.scheduleAtFixedRate({
            processBatch()
        }, saveInterval, saveInterval, TimeUnit.MILLISECONDS)
        
        logger.info("✅ 배치 저장 관리자 초기화 완료 - 배치 크기: {}, 타임아웃: {}ms", batchSize, batchTimeoutMs)
    }
    
    /**
     * ✅ 배치 데이터 추가
     */
    fun addToBatch(data: Map<String, Any?>) {
        // 성능 로깅 비활성화
        // return loggingService.logPerformance("addToBatch") {
        synchronized(batchBuffer) {
            batchBuffer.add(data)
            
            // ✅ 배치 크기 또는 시간 조건 확인
            if (batchBuffer.size >= batchSize || 
                (System.currentTimeMillis() - lastBatchTime) >= batchTimeoutMs) {
                processBatch()
            }
        }
        // }
    }
    
    /**
     * ✅ 배치 처리 실행
     */
    private fun processBatch() {
        val dataToProcess = synchronized(batchBuffer) {
            if (batchBuffer.isEmpty()) return
            
            val currentTime = System.currentTimeMillis()
            val timeElapsed = currentTime - lastBatchTime
            
            // ✅ 배치 조건 확인
            val shouldProcess = batchBuffer.size >= batchSize || 
                              timeElapsed >= batchTimeoutMs ||
                              batchBuffer.size >= maxBatchSize
            
            if (!shouldProcess) return
            
            val data = batchBuffer.toList()
            batchBuffer.clear()
            lastBatchTime = currentTime
            data
        }
        
        if (dataToProcess.isNotEmpty()) {
            // ✅ 비동기 배치 저장 (null 체크 추가)
            batchExecutor?.submit {
                saveBatchData(dataToProcess)
            }
        }
    }
    
    /**
     * ✅ 배치 데이터 저장
     */
    private fun saveBatchData(batchData: List<Map<String, Any?>>) {
        try {
            val startTime = System.currentTimeMillis()
            
            // ✅ 배치 데이터 검증
            val validData = batchData.filter { data ->
                val hasValidData = data["hasValidData"] as? Boolean ?: false
                val timestamp = data["timestamp"] as? ZonedDateTime
                timestamp != null && hasValidData
            }
            
            if (validData.isEmpty()) {
                logger.debug("배치 데이터가 모두 무효하여 저장을 건너뜁니다.")
                return
            }
            
            // ✅ 배치 통계 계산
            val avgAzimuthError = validData.mapNotNull { 
                it["azimuthError"] as? Float 
            }.average()
            
            val avgElevationError = validData.mapNotNull { 
                it["elevationError"] as? Float 
            }.average()
            
            // ✅ 배치 메타데이터 생성
            val batchMetadata = mapOf(
                "batchSize" to validData.size,
                "batchTimestamp" to ZonedDateTime.now(),
                "avgAzimuthError" to avgAzimuthError,
                "avgElevationError" to avgElevationError,
                "processingTimeMs" to (System.currentTimeMillis() - startTime)
            )
            
            // ✅ 배치 데이터 저장
            saveBatchToStorage(validData, batchMetadata)
            
            logger.info("📦 배치 저장 완료: {}개 데이터, 평균 Az 오차: {}°, El 오차: {}°, 처리시간: {}ms",
                validData.size, 
                String.format("%.2f", avgAzimuthError),
                String.format("%.2f", avgElevationError),
                System.currentTimeMillis() - startTime)
                
        } catch (e: Exception) {
            logger.error("배치 데이터 저장 중 오류: ${e.message}", e)
        }
    }
    
    /**
     * ✅ 배치 데이터를 저장소에 저장
     */
    private fun saveBatchToStorage(data: List<Map<String, Any?>>, metadata: Map<String, Any?>) {
        // ✅ 기존 realtimeTrackingDataList에 추가 (메모리 캐시)
        synchronized(realtimeTrackingDataList) {
            realtimeTrackingDataList.addAll(data)
            trackingDataIndex += data.size
        }

        // ✅ DB에 배치 저장 (Write-through)
        saveToDatabase(data)
    }

    /**
     * ✅ DB에 추적 결과 데이터를 저장
     */
    private fun saveToDatabase(data: List<Map<String, Any?>>) {
        if (trackingDataService == null) {
            logger.debug("TrackingDataService가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        try {
            val results = data.mapNotNull { item ->
                try {
                    mapToTrackingResult(item)
                } catch (e: Exception) {
                    logger.debug("추적 결과 변환 실패: ${e.message}")
                    null
                }
            }

            if (results.isNotEmpty()) {
                trackingDataService.saveResults(results)
                    .doOnSuccess {
                        logger.debug("📝 [DB] 추적 결과 배치 저장 완료: ${results.size}개")
                    }
                    .doOnError { e: Throwable ->
                        logger.error("❌ [DB] 추적 결과 저장 실패: ${e.message}")
                    }
                    .subscribe()
            }
        } catch (e: Exception) {
            logger.error("❌ [DB] 추적 결과 배치 변환 실패: ${e.message}")
        }
    }

    /**
     * ✅ Map 데이터를 TrackingResultEntity로 변환
     */
    private fun mapToTrackingResult(data: Map<String, Any?>): TrackingResultEntity {
        val timestamp = when (val ts = data["timestamp"]) {
            is ZonedDateTime -> ts.toOffsetDateTime()
            is OffsetDateTime -> ts
            else -> OffsetDateTime.now(ZoneOffset.UTC)
        }

        val sessionId = (data["sessionId"] as? Number)?.toLong() ?: 0L
        val index = (data["index"] as? Number)?.toInt() ?: trackingDataIndex

        return TrackingResultEntity(
            timestamp = timestamp,
            sessionId = sessionId,
            index = index,
            theoreticalIndex = (data["theoreticalIndex"] as? Number)?.toInt(),

            // 원본 각도
            originalAzimuth = (data["originalAzimuth"] as? Number)?.toDouble(),
            originalElevation = (data["originalElevation"] as? Number)?.toDouble(),

            // 변환된 각도
            transformedAzimuth = (data["transformedAzimuth"] as? Number)?.toDouble(),
            transformedElevation = (data["transformedElevation"] as? Number)?.toDouble(),
            transformedTrain = (data["transformedTrain"] as? Number)?.toDouble(),

            // 최종 각도
            finalAzimuth = (data["finalAzimuth"] as? Number)?.toDouble(),
            finalElevation = (data["finalElevation"] as? Number)?.toDouble(),
            finalTrain = (data["finalTrain"] as? Number)?.toDouble(),

            // 실제 측정값 (ICD Position)
            actualAzimuth = (data["actualAzimuth"] as? Number)?.toDouble()
                ?: (data["positionAzimuth"] as? Number)?.toDouble(),
            actualElevation = (data["actualElevation"] as? Number)?.toDouble()
                ?: (data["positionElevation"] as? Number)?.toDouble(),
            actualTrain = (data["actualTrain"] as? Number)?.toDouble()
                ?: (data["positionTrain"] as? Number)?.toDouble(),

            // 오차
            azimuthError = (data["azimuthError"] as? Number)?.toDouble(),
            elevationError = (data["elevationError"] as? Number)?.toDouble(),
            trainError = (data["trainError"] as? Number)?.toDouble(),
            totalError = (data["totalError"] as? Number)?.toDouble(),

            // 속도
            azimuthRate = (data["azimuthRate"] as? Number)?.toDouble(),
            elevationRate = (data["elevationRate"] as? Number)?.toDouble(),
            trainRate = (data["trainRate"] as? Number)?.toDouble(),

            // 가속도
            azimuthAcceleration = (data["azimuthAcceleration"] as? Number)?.toDouble(),
            elevationAcceleration = (data["elevationAcceleration"] as? Number)?.toDouble(),
            trainAcceleration = (data["trainAcceleration"] as? Number)?.toDouble(),

            // 상태
            keyholeActive = data["keyholeActive"] as? Boolean ?: false,
            keyholeOptimized = data["keyholeOptimized"] as? Boolean ?: false,
            trackingQuality = data["trackingQuality"] as? String,

            // 보간 정보
            interpolationType = data["interpolationType"] as? String,
            interpolationAccuracy = (data["interpolationAccuracy"] as? Number)?.toDouble(),

            // 위성 정보
            satelliteRange = (data["satelliteRange"] as? Number)?.toDouble(),
            satelliteAltitude = (data["satelliteAltitude"] as? Number)?.toDouble(),
            satelliteVelocity = (data["satelliteVelocity"] as? Number)?.toDouble(),

            // CMD/Position
            cmdAzimuth = (data["cmdAzimuth"] as? Number)?.toDouble(),
            cmdElevation = (data["cmdElevation"] as? Number)?.toDouble(),
            cmdTrain = (data["cmdTrain"] as? Number)?.toDouble(),
            positionAzimuth = (data["positionAzimuth"] as? Number)?.toDouble(),
            positionElevation = (data["positionElevation"] as? Number)?.toDouble(),
            positionTrain = (data["positionTrain"] as? Number)?.toDouble()
        )
    }
    
    /**
     * ✅ 강제 배치 처리
     */
    fun forceProcessBatch() {
        processBatch()
    }
    
    /**
     * ✅ 배치 상태 조회
     */
    fun getBatchStatus(): Map<String, Any> {
        return synchronized(batchBuffer) {
            mapOf(
                "bufferSize" to batchBuffer.size,
                "lastBatchTime" to lastBatchTime,
                "timeSinceLastBatch" to (System.currentTimeMillis() - lastBatchTime),
                "totalProcessed" to trackingDataIndex,
                "totalStored" to realtimeTrackingDataList.size
            )
        }
    }
    
    /**
     * ✅ 실시간 추적 데이터 조회
     */
    fun getRealtimeTrackingData(): List<Map<String, Any?>> {
        return synchronized(realtimeTrackingDataList) {
            realtimeTrackingDataList.toList()
        }
    }
    
    /**
     * ✅ 실시간 추적 데이터 초기화
     */
    fun clearRealtimeTrackingData() {
        synchronized(realtimeTrackingDataList) {
            realtimeTrackingDataList.clear()
            trackingDataIndex = 0
        }
        synchronized(batchBuffer) {
            batchBuffer.clear()
        }
        logger.info("실시간 추적 데이터 초기화 완료")
    }
    
    /**
     * ✅ 실시간 추적 통계 정보
     */
    fun getRealtimeTrackingStats(): Map<String, Any> {
        return synchronized(realtimeTrackingDataList) {
            if (realtimeTrackingDataList.isEmpty()) {
                return mapOf(
                    "totalCount" to 0,
                    "averageAzimuthError" to 0.0,
                    "averageElevationError" to 0.0,
                    "maxAzimuthError" to 0.0,
                    "maxElevationError" to 0.0,
                    "averageTransformationError" to 0.0,
                    "maxTransformationError" to 0.0,
                    "transformationCount" to 0
                )
            }

            val azimuthErrors = realtimeTrackingDataList.mapNotNull {
                it["azimuthError"] as? Float
            }
            val elevationErrors = realtimeTrackingDataList.mapNotNull {
                it["elevationError"] as? Float
            }
            
            // 변환 오차 통계
            val azimuthTransformationErrors = realtimeTrackingDataList.mapNotNull {
                it["azimuthTransformationError"] as? Float
            }
            val elevationTransformationErrors = realtimeTrackingDataList.mapNotNull {
                it["elevationTransformationError"] as? Float
            }
            
            // 변환 적용된 데이터 수
            val transformationCount = realtimeTrackingDataList.count {
                it["hasTransformation"] as? Boolean == true
            }

            mapOf(
                "totalCount" to realtimeTrackingDataList.size,
                "averageAzimuthError" to azimuthErrors.average(),
                "averageElevationError" to elevationErrors.average(),
                "maxAzimuthError" to (azimuthErrors.maxOrNull() ?: 0.0),
                "maxElevationError" to (elevationErrors.maxOrNull() ?: 0.0),
                "minAzimuthError" to (azimuthErrors.minOrNull() ?: 0.0),
                "minElevationError" to (elevationErrors.minOrNull() ?: 0.0),
                
                // 변환 오차 통계
                "averageAzimuthTransformationError" to azimuthTransformationErrors.average(),
                "averageElevationTransformationError" to elevationTransformationErrors.average(),
                "maxAzimuthTransformationError" to (azimuthTransformationErrors.maxOrNull() ?: 0.0),
                "maxElevationTransformationError" to (elevationTransformationErrors.maxOrNull() ?: 0.0),
                "minAzimuthTransformationError" to (azimuthTransformationErrors.minOrNull() ?: 0.0),
                "minElevationTransformationError" to (elevationTransformationErrors.minOrNull() ?: 0.0),
                
                // 변환 적용 통계
                "transformationCount" to transformationCount,
                "transformationPercentage" to if (realtimeTrackingDataList.isNotEmpty()) {
                    (transformationCount * 100.0 / realtimeTrackingDataList.size)
                } else 0.0
            )
        }
    }

    /**
     * ✅ 안전한 배치 종료 처리 (Graceful Shutdown)
     */
    @PreDestroy
    fun safeShutdown(): Boolean {
        try {
            logger.info("🔄 안전한 배치 종료 처리 시작")
            val batchStatus = getBatchStatus()
            val bufferSize = batchStatus["bufferSize"] as? Int ?: 0
            if (bufferSize > 0) {
                logger.info("📦 배치 버퍼에 {}개 데이터가 남아있습니다. 강제 처리 중...", bufferSize)
                forceProcessBatch()
                var waitCount = 0
                while (waitCount < 50) { // 50 * 100ms = 5초
                    // Note: Shutdown context에서는 blocking 대기가 필요 (리액티브 변환 불필요)
                    @Suppress("BlockingMethodInNonBlockingContext")
                    Thread.sleep(100)
                    val newStatus = getBatchStatus()
                    val newBufferSize = newStatus["bufferSize"] as? Int ?: 0
                    if (newBufferSize == 0) {
                        logger.info("✅ 배치 버퍼 처리 완료")
                        return true
                    }
                    waitCount++
                }
                logger.warn("⚠️ 배치 처리 타임아웃 (5초). 남은 데이터가 손실될 수 있습니다.")
                return false
            } else {
                logger.info("✅ 배치 버퍼가 비어있습니다.")
                return true
            }
        } catch (e: Exception) {
            logger.error("❌ 안전한 배치 종료 처리 중 오류: ${e.message}", e)
            return false
        }
    }
} 