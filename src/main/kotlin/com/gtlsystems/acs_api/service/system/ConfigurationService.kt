package com.gtlsystems.acs_api.service.system

import com.gtlsystems.acs_api.config.SystemConfiguration
import com.gtlsystems.acs_api.event.ConfigurationChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PostConstruct
import com.gtlsystems.acs_api.service.system.LoggingService

/**
 * 시스템 설정 관리 서비스
 * 설정 변경을 감지하고 이벤트를 발행합니다.
 */
@Service
class ConfigurationService(
    private val systemConfiguration: SystemConfiguration,
    private val eventPublisher: ApplicationEventPublisher,
    private val loggingService: LoggingService
) {
    
    private val logger = LoggerFactory.getLogger(ConfigurationService::class.java)
    
    // 메모리 캐시 (DB 연동 전까지 사용)
    private val configCache = ConcurrentHashMap<String, Any>()
    
    @PostConstruct
    fun initialize() {
        loggingService.logSystemStart("ConfigurationService", "1.0.0")
        logger.info("🚀 ConfigurationService 초기화 시작")
        loadConfigurationToCache()
        logger.info("✅ ConfigurationService 초기화 완료")
    }
    
    /**
     * 설정 캐시를 메모리에 로드
     */
    private fun loadConfigurationToCache() {
        // 성능 로깅 비활성화
        // return loggingService.logPerformance("loadConfigurationToCache") {
        try {
            // UDP 설정
            configCache["udp.receiveInterval"] = systemConfiguration.udp.receiveInterval
            configCache["udp.sendInterval"] = systemConfiguration.udp.sendInterval
            configCache["udp.timeout"] = systemConfiguration.udp.timeout
            configCache["udp.reconnectInterval"] = systemConfiguration.udp.reconnectInterval
            configCache["udp.maxBufferSize"] = systemConfiguration.udp.maxBufferSize
            configCache["udp.commandDelay"] = systemConfiguration.udp.commandDelay
            
            // 추적 설정
            configCache["tracking.interval"] = systemConfiguration.tracking.interval
            configCache["tracking.transmissionInterval"] = systemConfiguration.tracking.transmissionInterval
            configCache["tracking.fineInterval"] = systemConfiguration.tracking.fineInterval
            configCache["tracking.coarseInterval"] = systemConfiguration.tracking.coarseInterval
            configCache["tracking.performanceThreshold"] = systemConfiguration.tracking.performanceThreshold
            configCache["tracking.stabilizationTimeout"] = systemConfiguration.tracking.stabilizationTimeout
            
            // 데이터 저장 설정
            configCache["storage.batchSize"] = systemConfiguration.storage.batchSize
            configCache["storage.saveInterval"] = systemConfiguration.storage.saveInterval
            configCache["storage.progressLogInterval"] = systemConfiguration.storage.progressLogInterval
            
            // 위치 설정
            configCache["location.latitude"] = systemConfiguration.location.latitude
            configCache["location.longitude"] = systemConfiguration.location.longitude
            configCache["location.trackingSpeed"] = systemConfiguration.location.trackingSpeed
            
            logger.info("📋 설정 캐시 로드 완료: ${configCache.size}개 항목")
        } catch (e: Exception) {
            loggingService.error("설정 캐시 로드 실패", e)
            logger.error("❌ 설정 캐시 로드 실패", e)
            throw RuntimeException("설정 초기화 실패", e)
        }
        // }
    }

    /**
     * 설정 값 조회
     */
    fun getValue(key: String): Any? {
        // 성능 로깅 비활성화
        // return loggingService.logPerformance("getValue") {
        val value = configCache[key]
        loggingService.debug("설정 값 조회: $key = $value")
        return value
        // }
    }

    /**
     * 설정 값 업데이트
     */
    fun updateValue(key: String, newValue: Any): Boolean {
        // 성능 로깅 비활성화
        // return loggingService.logPerformance("updateValue") {
        try {
            val oldValue = configCache[key]
            if (oldValue != newValue) {
                configCache[key] = newValue
                logger.info("🔄 설정 변경: $key = $oldValue → $newValue")
                
                // 설정 변경 로깅
                loggingService.logConfigurationChange(key, oldValue, newValue)
                
                // 설정 변경 이벤트 발행
                val event = ConfigurationChangedEvent(key, oldValue, newValue)
                eventPublisher.publishEvent(event)
                
                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            loggingService.error("설정 업데이트 실패: $key = $newValue", e)
            logger.error("❌ 설정 업데이트 실패: $key = $newValue", e)
            return false
        }
        // }
    }
    
    /**
     * 모든 설정 조회
     */
    fun getAllConfiguration(): Map<String, Any> {
        return configCache.toMap()
    }
    
    /**
     * 설정 초기화 (기본값으로 복원)
     */
    fun resetToDefault() {
        logger.info("🔄 설정을 기본값으로 초기화")
        loadConfigurationToCache()
        
        // 전체 설정 변경 이벤트 발행
        val event = ConfigurationChangedEvent("ALL", null, "RESET")
        eventPublisher.publishEvent(event)
    }
    
    /**
     * 설정 유효성 검사
     */
    fun validateConfiguration(): Boolean {
        return try {
            // 기본적인 유효성 검사
            systemConfiguration.udp.receiveInterval > 0 &&
            systemConfiguration.udp.sendInterval > 0 &&
            systemConfiguration.tracking.interval > 0 &&
            systemConfiguration.location.latitude in -90.0..90.0 &&
            systemConfiguration.location.longitude in -180.0..180.0
        } catch (e: Exception) {
            logger.error("❌ 설정 유효성 검사 실패", e)
            false
        }
    }
} 