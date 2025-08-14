package com.gtlsystems.acs_api.service.system

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * ACS 시스템의 체계적인 로깅을 제공하는 서비스
 * 
 * 주요 기능:
 * - 로그 레벨별 메서드 제공
 * - 성능 로깅 (메서드 실행 시간 측정)
 * - 에러 로깅 (예외 정보 포함)
 * - 비즈니스 로깅 (사용자 액션 추적)
 * - 로그 통계 및 모니터링
 */
@Service
class LoggingService {
    
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LoggingService::class.java)
        private const val PERFORMANCE_THRESHOLD_MS = 100L // 성능 임계값 (100ms)
        private const val MAX_LOG_ENTRIES = 10000 // 최대 로그 엔트리 수
    }
    
    // 성능 로깅을 위한 타이머 저장소
    private val performanceTimers = ConcurrentHashMap<String, Long>()
    
    // 로그 통계 저장소
    private val logStats = ConcurrentHashMap<String, AtomicLong>()
    
    // 비즈니스 로그 저장소 (최근 N개만 유지)
    private val businessLogs = ConcurrentHashMap<String, BusinessLogEntry>()
    
    /**
     * 일반 정보 로그
     */
    fun info(message: String, vararg args: Any?) {
        logger.info(message, *args)
        updateLogStats("INFO")
    }
    
    /**
     * 디버그 로그
     */
    fun debug(message: String, vararg args: Any?) {
        logger.debug(message, *args)
        updateLogStats("DEBUG")
    }
    
    /**
     * 경고 로그
     */
    fun warn(message: String, vararg args: Any?) {
        logger.warn(message, *args)
        updateLogStats("WARN")
    }
    
    /**
     * 에러 로그
     */
    fun error(message: String, vararg args: Any?) {
        logger.error(message, *args)
        updateLogStats("ERROR")
    }
    
    /**
     * 에러 로그 (예외 포함)
     */
    fun error(message: String, throwable: Throwable, vararg args: Any?) {
        logger.error(message, throwable, *args)
        updateLogStats("ERROR")
        
        // 에러 상세 정보 로깅
        logErrorDetails(message, throwable)
    }
    
    /**
     * 성능 로깅 시작
     */
    fun startPerformanceLog(operationName: String): String {
        val timerId = generateTimerId(operationName)
        performanceTimers[timerId] = System.currentTimeMillis()
        logger.debug("성능 로깅 시작: $operationName (ID: $timerId)")
        return timerId
    }
    
    /**
     * 성능 로깅 완료
     */
    fun endPerformanceLog(timerId: String, operationName: String) {
        val startTime = performanceTimers.remove(timerId)
        if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            val logMessage = "성능 로깅 완료: $operationName (ID: $timerId) - 소요시간: ${duration}ms"
            
            if (duration > PERFORMANCE_THRESHOLD_MS) {
                logger.warn("$logMessage - 성능 임계값 초과!")
            } else {
                logger.info(logMessage)
            }
            
            updateLogStats("PERFORMANCE")
        } else {
            logger.warn("성능 로깅 타이머를 찾을 수 없음: $timerId")
        }
    }
    
    /**
     * 성능 로깅 (람다 함수용)
     */
    fun <T> logPerformance(operationName: String, operation: () -> T): T {
        val timerId = startPerformanceLog(operationName)
        try {
            val result = operation()
            endPerformanceLog(timerId, operationName)
            return result
        } catch (e: Exception) {
            endPerformanceLog(timerId, operationName)
            throw e
        }
    }
    
    /**
     * 비즈니스 로그 (사용자 액션 추적)
     */
    fun logBusinessAction(
        userId: String?,
        action: String,
        details: String,
        result: String = "SUCCESS",
        vararg args: Any?
    ) {
        val timestamp = LocalDateTime.now()
        val logMessage = "비즈니스 액션: 사용자=$userId, 액션=$action, 상세=$details, 결과=$result"
        
        logger.info(logMessage, *args)
        updateLogStats("BUSINESS")
        
        // 비즈니스 로그 저장
        val entry = BusinessLogEntry(
            timestamp = timestamp,
            userId = userId,
            action = action,
            details = details,
            result = result
        )
        
        val key = "${timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}_${System.nanoTime()}"
        businessLogs[key] = entry
        
        // 최대 로그 엔트리 수 제한
        if (businessLogs.size > MAX_LOG_ENTRIES) {
            val oldestKey = businessLogs.keys.minByOrNull { it }
            oldestKey?.let { businessLogs.remove(it) }
        }
    }
    
    /**
     * 시스템 시작 로그
     */
    fun logSystemStart(componentName: String, version: String? = null) {
        val message = if (version != null) {
            "시스템 시작: $componentName (버전: $version)"
        } else {
            "시스템 시작: $componentName"
        }
        
        logger.info(message)
        updateLogStats("SYSTEM_START")
    }
    
    /**
     * 시스템 종료 로그
     */
    fun logSystemStop(componentName: String, reason: String? = null) {
        val message = if (reason != null) {
            "시스템 종료: $componentName (사유: $reason)"
        } else {
            "시스템 종료: $componentName"
        }
        
        logger.info(message)
        updateLogStats("SYSTEM_STOP")
    }
    
    /**
     * 설정 변경 로그
     */
    fun logConfigurationChange(
        configKey: String,
        oldValue: Any?,
        newValue: Any?,
        changedBy: String? = null
    ) {
        val message = "설정 변경: $configKey = $oldValue -> $newValue" +
                (if (changedBy != null) " (변경자: $changedBy)" else "")
        
        logger.info(message)
        updateLogStats("CONFIG_CHANGE")
    }
    
    /**
     * 외부 API 호출 로그
     */
    fun logExternalApiCall(
        apiName: String,
        url: String,
        method: String,
        statusCode: Int? = null,
        responseTime: Long? = null
    ) {
        val message = "외부 API 호출: $apiName ($method $url)" +
                (if (statusCode != null) " - 상태코드: $statusCode" else "") +
                (if (responseTime != null) " - 응답시간: ${responseTime}ms" else "")
        
        logger.info(message)
        updateLogStats("EXTERNAL_API")
    }
    
    /**
     * 데이터베이스 작업 로그
     */
    fun logDatabaseOperation(
        operation: String,
        tableName: String,
        recordCount: Int? = null,
        executionTime: Long? = null
    ) {
        val message = "데이터베이스 작업: $operation 테이블=$tableName" +
                (if (recordCount != null) " - 레코드 수: $recordCount" else "") +
                (if (executionTime != null) " - 실행시간: ${executionTime}ms" else "")
        
        logger.info(message)
        updateLogStats("DATABASE")
    }
    
    /**
     * 로그 통계 조회
     */
    fun getLogStatistics(): Map<String, Long> {
        return logStats.mapValues { it.value.get() }
    }
    
    /**
     * 비즈니스 로그 조회 (최근 N개)
     */
    fun getRecentBusinessLogs(limit: Int = 100): List<BusinessLogEntry> {
        return businessLogs.values
            .sortedByDescending { it.timestamp }
            .take(limit)
            .toList()
    }
    
    /**
     * 로그 통계 초기화
     */
    fun resetLogStatistics() {
        logStats.clear()
        logger.info("로그 통계가 초기화되었습니다.")
    }
    
    /**
     * 비즈니스 로그 초기화
     */
    fun clearBusinessLogs() {
        val count = businessLogs.size
        businessLogs.clear()
        logger.info("비즈니스 로그가 초기화되었습니다. (삭제된 로그: $count 개)")
    }
    
    // Private helper methods
    
    private fun generateTimerId(operationName: String): String {
        return "${operationName}_${System.nanoTime()}"
    }
    
    private fun updateLogStats(logType: String) {
        logStats.computeIfAbsent(logType) { AtomicLong(0) }.incrementAndGet()
    }
    
    private fun logErrorDetails(message: String, throwable: Throwable) {
        logger.error("에러 상세 정보: $message", throwable)
        
        // 스택 트레이스 로깅 (DEBUG 레벨)
        logger.debug("스택 트레이스:", throwable)
    }
    
    /**
     * 비즈니스 로그 엔트리 데이터 클래스
     */
    data class BusinessLogEntry(
        val timestamp: LocalDateTime,
        val userId: String?,
        val action: String,
        val details: String,
        val result: String
    )
} 