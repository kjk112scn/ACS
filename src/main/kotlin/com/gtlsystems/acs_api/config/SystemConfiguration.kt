package com.gtlsystems.acs_api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

/**
 * 시스템 설정 클래스
 * application.properties에서 설정을 로드합니다.
 * 실제 하드코딩된 값들을 기반으로 작성되었습니다.
 */
@Configuration
@ConfigurationProperties(prefix = "system")
@Component
class SystemConfiguration {
    
    // === UDP 통신 설정 ===
    val udp = Udp()
    class Udp {
        var receiveInterval: Long = 10 // UDP Receive 간격 (ms) - 실제: 10ms
        var sendInterval: Long = 10 // UDP Send 간격 (ms) - 실제: 10ms
        var timeout: Long = 25 // UDP 타임아웃 (ms) - 실제: 25ms
        var reconnectInterval: Long = 1000 // UDP 재연결 간격 (ms) - 실제: 1000ms
        var maxBufferSize: Int = 1024 // 최대 버퍼 크기 - 실제: 1024
        var commandDelay: Long = 100 // 명령 전송 후 대기 시간 (ms) - 실제: 100ms
    }
    
    // === 추적 설정 ===
    val tracking = Tracking()
    class Tracking {
        var interval: Long = 100 // 추적 주기 (ms) - 실제: 100ms
        var transmissionInterval: Long = 100 // 전송 간격 (ms) - 실제: 100ms
        var fineInterval: Long = 100 // 정밀 계산 간격 (ms) - 실제: 100ms
        var coarseInterval: Long = 1000 // 일반 계산 간격 (ms) - 실제: 1000ms
        var performanceThreshold: Long = 100 // 성능 임계값 (ms) - 실제: 100ms
        var stabilizationTimeout: Long = 5000 // 안정화 타임아웃 (ms) - 실제: 5000ms
    }
    
    // === 데이터 저장 설정 ===
    val storage = Storage()
    class Storage {
        var batchSize: Int = 1000 // 배치 크기 - 실제: 1000개
        var saveInterval: Long = 100 // 저장 간격 (ms) - 실제: 100ms
        var progressLogInterval: Int = 1000 // 진행률 로깅 간격 - 실제: 1000개마다
    }
    
    // === 위치 설정 ===
    val location = Location()
    class Location {
        var latitude: Double = 37.4563 // 위도 - 실제: 37.4563
        var longitude: Double = 128.608510 // 경도 - 실제: 128.608510
        var trackingSpeed: Int = 5 // 추적 속도 - 실제: 10
    }
} 