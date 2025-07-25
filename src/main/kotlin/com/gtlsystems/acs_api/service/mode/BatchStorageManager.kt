package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.config.ThreadManager
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct
import java.time.ZonedDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * ✅ 배치 저장 관리자
 * 실시간 추적 데이터의 배치 처리를 담당
 */
@Service
class BatchStorageManager(
    private val threadManager: ThreadManager,
    private val dataStoreService: DataStoreService
) {
    private val logger = LoggerFactory.getLogger(BatchStorageManager::class.java)
    
    // ✅ 배치 설정
    private val batchSize = 50  // 50개씩 배치 처리
    private val batchTimeoutMs = 2000L  // 2초 타임아웃
    private val maxBatchSize = 100  // 최대 배치 크기
    
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
        // ✅ 지연 초기화로 의존성 주입 문제 해결
        batchExecutor = threadManager.getBatchExecutor()
        batchScheduler = threadManager.getRealtimeExecutor()
        
        // ✅ 주기적 배치 저장 스케줄링
        batchScheduler?.scheduleAtFixedRate({
            processBatch()
        }, 100, 100, TimeUnit.MILLISECONDS)
        
        logger.info("✅ 배치 저장 관리자 초기화 완료 - 배치 크기: {}, 타임아웃: {}ms", batchSize, batchTimeoutMs)
    }
    
    /**
     * ✅ 배치 데이터 추가
     */
    fun addToBatch(data: Map<String, Any?>) {
        synchronized(batchBuffer) {
            batchBuffer.add(data)
            
            // ✅ 배치 크기 또는 시간 조건 확인
            if (batchBuffer.size >= batchSize || 
                (System.currentTimeMillis() - lastBatchTime) >= batchTimeoutMs) {
                processBatch()
            }
        }
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
        // ✅ 기존 realtimeTrackingDataList에 추가
        synchronized(realtimeTrackingDataList) {
            realtimeTrackingDataList.addAll(data)
            trackingDataIndex += data.size
        }
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
     * ✅ 안전한 배치 종료 처리
     */
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