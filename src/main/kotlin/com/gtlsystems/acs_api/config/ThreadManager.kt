package com.gtlsystems.acs_api.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * ✅ 통합 쓰레드 관리자 (ThreadManager)
 * 하이브리드 하드웨어 최적화 + 쓰레드 통합 관리
 */
@Configuration
class ThreadManager {
    
    private val logger = LoggerFactory.getLogger(ThreadManager::class.java)
    
    @PostConstruct
    fun initialize() {
        logger.info("🚀 ThreadManager 초기화 시작")
        
        val specs = detectSystemSpecs()
        val tier = classifyPerformanceTier(specs)
        applyHardwareOptimization(tier)
        
        logger.info("✅ ThreadManager 초기화 완료 (성능 등급: {})", tier)
    }
    
    // ✅ 시스템 사양 자동 감지
    data class SystemSpecs(
        val cpuCores: Int,
        val cpuModel: String,
        val totalMemory: Long,
        val availableMemory: Long,
        val cpuArchitecture: String,
        val osName: String,
        val osVersion: String
    )
    
    // ✅ 성능 등급 분류
    enum class PerformanceTier {
        LOW, MEDIUM, HIGH, ULTRA
    }
    
    // ✅ 쓰레드 풀 인스턴스
    private var realtimeExecutor: ScheduledExecutorService? = null
    private var modeExecutor: ScheduledExecutorService? = null
    private var batchExecutor: ExecutorService? = null
    
    /**
     * ✅ 시스템 사양 자동 감지
     */
    fun detectSystemSpecs(): SystemSpecs {
        val runtime = Runtime.getRuntime()
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        
        val specs = SystemSpecs(
            cpuCores = runtime.availableProcessors(),
            cpuModel = getCpuModel(),
            totalMemory = runtime.maxMemory(),
            availableMemory = runtime.freeMemory(),
            cpuArchitecture = System.getProperty("os.arch"),
            osName = System.getProperty("os.name"),
            osVersion = System.getProperty("os.version")
        )
        
        logger.info("🔍 시스템 사양 감지: $specs")
        return specs
    }
    
    /**
     * ✅ 성능 등급 자동 분류
     */
    fun classifyPerformanceTier(specs: SystemSpecs): PerformanceTier {
        return when {
            specs.cpuCores >= 12 && specs.totalMemory >= 16_000_000_000L -> PerformanceTier.ULTRA
            specs.cpuCores >= 8 && specs.totalMemory >= 8_000_000_000L -> PerformanceTier.HIGH
            specs.cpuCores >= 4 && specs.totalMemory >= 4_000_000_000L -> PerformanceTier.MEDIUM
            else -> PerformanceTier.LOW
        }
    }
    
    /**
     * ✅ 하이브리드 하드웨어 최적화 적용
     */
    fun applyHardwareOptimization(tier: PerformanceTier) {
        when (tier) {
            PerformanceTier.ULTRA -> applyUltraOptimization()
            PerformanceTier.HIGH -> applyHighOptimization()
            PerformanceTier.MEDIUM -> applyMediumOptimization()
            PerformanceTier.LOW -> applyLowOptimization()
        }
    }
    
    /**
     * ✅ ULTRA 등급 최적화 (i7-1255U + 16GB RAM 등) - 실시간 성능 최우선
     */
    private fun applyUltraOptimization() {
        logger.info("🚀 ULTRA 등급 최적화 적용 (실시간 성능 최우선)")
        
        // JVM 힙 메모리 최적화
        System.setProperty("java.awt.headless", "true")
        System.setProperty("spring.jvm.memory.initial", "2g")
        System.setProperty("spring.jvm.memory.maximum", "9g")
        
        // ✅ 실시간 성능 최우선 JVM 최적화
        System.setProperty("spring.jvm.gc", "G1GC")
        System.setProperty("spring.jvm.gc.pause", "10")  // 10ms로 실시간 성능 보장
        System.setProperty("spring.jvm.gc.heap.region.size", "32m")  // 더 큰 영역으로 GC 빈도 감소
        System.setProperty("spring.jvm.gc.concurrent.threads", "6")  // 더 많은 동시 스레드
        System.setProperty("spring.jvm.gc.parallel.threads", "10")   // 더 많은 병렬 스레드
        System.setProperty("spring.jvm.gc.incremental.mode", "true") // 증분 모드로 일시정지 분산
        
        // ✅ 안정성 우선 메모리 최적화
        System.setProperty("spring.jvm.memory.metaspace.size", "256m")  // 안정적인 메타스페이스
        System.setProperty("spring.jvm.memory.compressed.oops", "true")
        
        // ✅ 안정성 우선 성능 최적화
        System.setProperty("spring.jvm.optimization.level", "2")        // 안정적인 최적화 레벨
        System.setProperty("spring.jvm.tiered.compilation", "true")
        System.setProperty("spring.jvm.adaptive.size.policy", "true")
        
        // ✅ 안정성 우선 쓰레드 최적화
        createOptimizedThreadPools(12, 8, 4)  // 안정적인 쓰레드 수
    }
    
    /**
     * ✅ HIGH 등급 최적화
     */
    private fun applyHighOptimization() {
        logger.info("⚡ HIGH 등급 최적화 적용")
        
        // JVM 힙 메모리 최적화
        System.setProperty("spring.jvm.memory.initial", "1g")
        System.setProperty("spring.jvm.memory.maximum", "6g")
        
        // ✅ 추가 JVM 최적화
        System.setProperty("spring.jvm.gc", "G1GC")
        System.setProperty("spring.jvm.gc.pause", "20")
        System.setProperty("spring.jvm.gc.heap.region.size", "8m")
        System.setProperty("spring.jvm.gc.concurrent.threads", "2")
        System.setProperty("spring.jvm.gc.parallel.threads", "4")
        
        // ✅ 메모리 최적화
        System.setProperty("spring.jvm.memory.metaspace.size", "128m")
        System.setProperty("spring.jvm.memory.compressed.oops", "true")
        
        // ✅ 성능 최적화
        System.setProperty("spring.jvm.optimization.level", "1")
        System.setProperty("spring.jvm.tiered.compilation", "true")
        
        // 쓰레드 풀 최적화
        createOptimizedThreadPools(8, 6, 3)
    }
    
    /**
     * ✅ MEDIUM 등급 최적화
     */
    private fun applyMediumOptimization() {
        logger.info("📊 MEDIUM 등급 최적화 적용")
        
        // JVM 힙 메모리 최적화
        System.setProperty("spring.jvm.memory.initial", "512m")
        System.setProperty("spring.jvm.memory.maximum", "3g")
        
        // ✅ 추가 JVM 최적화
        System.setProperty("spring.jvm.gc", "G1GC")
        System.setProperty("spring.jvm.gc.pause", "50")
        System.setProperty("spring.jvm.gc.heap.region.size", "4m")
        System.setProperty("spring.jvm.gc.concurrent.threads", "1")
        System.setProperty("spring.jvm.gc.parallel.threads", "2")
        
        // ✅ 메모리 최적화
        System.setProperty("spring.jvm.memory.metaspace.size", "64m")
        System.setProperty("spring.jvm.memory.compressed.oops", "true")
        
        // 쓰레드 풀 최적화
        createOptimizedThreadPools(4, 3, 2)
    }
    
    /**
     * ✅ LOW 등급 최적화
     */
    private fun applyLowOptimization() {
        logger.info("💡 LOW 등급 최적화 적용")
        
        // JVM 힙 메모리 최적화
        System.setProperty("spring.jvm.memory.initial", "256m")
        System.setProperty("spring.jvm.memory.maximum", "1g")
        
        // ✅ 추가 JVM 최적화
        System.setProperty("spring.jvm.gc", "G1GC")
        System.setProperty("spring.jvm.gc.pause", "100")
        System.setProperty("spring.jvm.gc.heap.region.size", "2m")
        System.setProperty("spring.jvm.gc.concurrent.threads", "1")
        System.setProperty("spring.jvm.gc.parallel.threads", "1")
        
        // ✅ 메모리 최적화
        System.setProperty("spring.jvm.memory.metaspace.size", "32m")
        System.setProperty("spring.jvm.memory.compressed.oops", "true")
        
        // 쓰레드 풀 최적화
        createOptimizedThreadPools(2, 2, 1)
    }
    
    /**
     * ✅ 안정성 우선 쓰레드 풀 생성
     */
    private fun createOptimizedThreadPools(
        coreThreads: Int,
        maxThreads: Int,
        queueCapacity: Int
    ) {
        // ✅ 안정적인 UDP/WebSocket 쓰레드 풀
        realtimeExecutor = Executors.newScheduledThreadPool(
            coreThreads,
            ThreadFactory { r ->
                Thread(r, "realtime-stable").apply {
                    priority = Thread.MAX_PRIORITY  // 최고 우선순위 유지
                    isDaemon = true
                    // ✅ 안정성 우선 예외 처리
                    setUncaughtExceptionHandler { thread, ex ->
                        logger.error("실시간 쓰레드 오류: ${thread.name}", ex)
                    }
                }
            }
        )
        
        // ✅ 안정적인 100ms 저장 쓰레드 풀
        modeExecutor = Executors.newSingleThreadScheduledExecutor(
            ThreadFactory { r ->
                Thread(r, "save-stable").apply {
                    priority = Thread.MAX_PRIORITY - 1  // 높은 우선순위 유지
                    isDaemon = true
                    // ✅ 안정성 우선 예외 처리
                    setUncaughtExceptionHandler { thread, ex ->
                        logger.error("저장 쓰레드 오류: ${thread.name}", ex)
                    }
                }
            }
        )
        
        // ✅ 안정적인 배치 처리 쓰레드 풀
        batchExecutor = Executors.newFixedThreadPool(
            2,  // 안정적인 배치 처리 쓰레드 수
            ThreadFactory { r ->
                Thread(r, "batch-stable").apply {
                    priority = Thread.NORM_PRIORITY  // 안정적인 우선순위
                    isDaemon = true
                }
            }
        )
        
        logger.info("✅ 안정성 우선 쓰레드 풀 생성 완료")
    }
    
    /**
     * ✅ 실시간 실행기 반환
     */
    fun getRealtimeExecutor(): ScheduledExecutorService? {
        return realtimeExecutor
    }
    
    /**
     * ✅ 모드 실행기 반환
     */
    fun getModeExecutor(): ScheduledExecutorService? {
        return modeExecutor
    }
    
    /**
     * ✅ 배치 실행기 반환
     */
    fun getBatchExecutor(): ExecutorService? {
        return batchExecutor
    }
    
    /**
     * ✅ CPU 모델 정보 가져오기
     */
    private fun getCpuModel(): String {
        return try {
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            System.getProperty("os.arch") + " " + 
            Runtime.getRuntime().availableProcessors() + " cores"
        } catch (e: Exception) {
            "Unknown CPU"
        }
    }
    
    /**
     * ✅ 쓰레드 풀 정리
     */
    fun shutdown() {
        realtimeExecutor?.shutdown()
        modeExecutor?.shutdown()
        batchExecutor?.shutdown()
        
        try {
            if (!realtimeExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                realtimeExecutor?.shutdownNow()
            }
            if (!modeExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                modeExecutor?.shutdownNow()
            }
            if (!batchExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                batchExecutor?.shutdownNow()
            }
        } catch (e: InterruptedException) {
            realtimeExecutor?.shutdownNow()
            modeExecutor?.shutdownNow()
            batchExecutor?.shutdownNow()
        }
        
        logger.info("✅ 쓰레드 풀 정리 완료")
    }
} 