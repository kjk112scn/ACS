package com.gtlsystems.acs_api.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import com.gtlsystems.acs_api.service.system.settings.SettingsService
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
class ThreadManager(
    private val settingsService: SettingsService
) {
    
    private val logger = LoggerFactory.getLogger(ThreadManager::class.java)
    
    @PostConstruct
    fun initialize() {
        logger.info("🚀 ThreadManager 초기화 시작")
        
        val specs = detectSystemSpecs()
        val tier = classifyPerformanceTier(specs)
        applyHardwareOptimization(tier)
        
        // ✅ 통합 스레드 풀 초기화
        initializeIntegratedThreadPools(tier)
        
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
    
    // ✅ 우선순위 체계 정의
    enum class ThreadPriority(val priority: Int) {
        CRITICAL(Thread.MAX_PRIORITY),      // 하드웨어 통신 (10ms/30ms)
        HIGH(Thread.MAX_PRIORITY - 1),     // WebSocket 브로드캐스트 (30ms)
        NORMAL(Thread.NORM_PRIORITY),      // 추적 작업 (100ms)
        LOW(Thread.MIN_PRIORITY)           // 배치 처리, 계산
    }
    
    // ✅ 기존 쓰레드 풀 인스턴스 (하위 호환성)
    private var realtimeExecutor: ScheduledExecutorService? = null
    private var modeExecutor: ScheduledExecutorService? = null
    private var batchExecutor: ExecutorService? = null
    
    // ✅ 통합 쓰레드 풀 인스턴스
    private var udpExecutor: ScheduledExecutorService? = null
    private var websocketExecutor: ScheduledExecutorService? = null
    private var trackingExecutor: ScheduledExecutorService? = null
    private var batchScheduler: ScheduledExecutorService? = null
    private var calculationExecutor: ScheduledExecutorService? = null
    
    /**
     * ✅ 통합 스레드 풀 초기화
     */
    private fun initializeIntegratedThreadPools(tier: PerformanceTier) {
        logger.info("🔧 통합 스레드 풀 초기화 시작 (등급: {})", tier)
        
        when (tier) {
            PerformanceTier.ULTRA -> createUltraThreadPools()
            PerformanceTier.HIGH -> createHighThreadPools()
            PerformanceTier.MEDIUM -> createMediumThreadPools()
            PerformanceTier.LOW -> createLowThreadPools()
        }
        
        logger.info("✅ 통합 스레드 풀 초기화 완료")
    }
    
    /**
     * ✅ ThreadFactory 생성 메서드
     */
    private fun createThreadFactory(name: String, priority: ThreadPriority): ThreadFactory {
        return ThreadFactory { runnable ->
            Thread(runnable, name).apply {
                this.priority = priority.priority
                isDaemon = true
                setUncaughtExceptionHandler { thread, ex ->
                    logger.error("스레드 오류: ${thread.name}", ex)
                }
            }
        }
    }
    
    /**
     * ✅ ULTRA 등급 통합 스레드 풀 생성
     */
    private fun createUltraThreadPools() {
        logger.info("🚀 ULTRA 등급 통합 스레드 풀 생성")
        
        // ✅ 1. UDP 통신 (CRITICAL)
        udpExecutor = Executors.newScheduledThreadPool(
            2, 
            createThreadFactory("udp-", ThreadPriority.CRITICAL)
        )
        
        // ✅ 2. WebSocket 브로드캐스트 (HIGH)
        websocketExecutor = Executors.newScheduledThreadPool(
            2, 
            createThreadFactory("websocket-", ThreadPriority.HIGH)
        )
        
        // ✅ 3. 통합 추적 스레드 (NORMAL)
        trackingExecutor = Executors.newScheduledThreadPool(
            1, 
            createThreadFactory("tracking-", ThreadPriority.NORMAL)
        )
        
        // ✅ 4. 배치 저장 처리 (LOW)
        batchExecutor = Executors.newFixedThreadPool(
            4, 
            createThreadFactory("batch-", ThreadPriority.LOW)
        )
        
        // ✅ 5. 배치 스케줄링 (LOW)
        batchScheduler = Executors.newScheduledThreadPool(
            1, 
            createThreadFactory("batch-scheduler-", ThreadPriority.LOW)
        )
        
        // ✅ 6. 계산 처리 (LOW)
        calculationExecutor = Executors.newScheduledThreadPool(
            1, 
            createThreadFactory("calculation-", ThreadPriority.LOW)
        )
    }
    
    /**
     * ✅ HIGH 등급 통합 스레드 풀 생성
     */
    private fun createHighThreadPools() {
        logger.info("⚡ HIGH 등급 통합 스레드 풀 생성")
        
        udpExecutor = Executors.newScheduledThreadPool(2, createThreadFactory("udp-", ThreadPriority.CRITICAL))
        websocketExecutor = Executors.newScheduledThreadPool(2, createThreadFactory("websocket-", ThreadPriority.HIGH))
        trackingExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("tracking-", ThreadPriority.NORMAL))
        batchExecutor = Executors.newFixedThreadPool(3, createThreadFactory("batch-", ThreadPriority.LOW))
        batchScheduler = Executors.newScheduledThreadPool(1, createThreadFactory("batch-scheduler-", ThreadPriority.LOW))
        calculationExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("calculation-", ThreadPriority.LOW))
    }
    
    /**
     * ✅ MEDIUM 등급 통합 스레드 풀 생성
     */
    private fun createMediumThreadPools() {
        logger.info("📊 MEDIUM 등급 통합 스레드 풀 생성")
        
        udpExecutor = Executors.newScheduledThreadPool(2, createThreadFactory("udp-", ThreadPriority.CRITICAL))
        websocketExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("websocket-", ThreadPriority.HIGH))
        trackingExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("tracking-", ThreadPriority.NORMAL))
        batchExecutor = Executors.newFixedThreadPool(2, createThreadFactory("batch-", ThreadPriority.LOW))
        batchScheduler = Executors.newScheduledThreadPool(1, createThreadFactory("batch-scheduler-", ThreadPriority.LOW))
        calculationExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("calculation-", ThreadPriority.LOW))
    }
    
    /**
     * ✅ LOW 등급 통합 스레드 풀 생성
     */
    private fun createLowThreadPools() {
        logger.info("💡 LOW 등급 통합 스레드 풀 생성")
        
        udpExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("udp-", ThreadPriority.CRITICAL))
        websocketExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("websocket-", ThreadPriority.HIGH))
        trackingExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("tracking-", ThreadPriority.NORMAL))
        batchExecutor = Executors.newFixedThreadPool(1, createThreadFactory("batch-", ThreadPriority.LOW))
        batchScheduler = Executors.newScheduledThreadPool(1, createThreadFactory("batch-scheduler-", ThreadPriority.LOW))
        calculationExecutor = Executors.newScheduledThreadPool(1, createThreadFactory("calculation-", ThreadPriority.LOW))
    }
    
    /**
     * ✅ 시스템 사양 자동 감지
     */
    fun detectSystemSpecs(): SystemSpecs {
        val runtime = Runtime.getRuntime()
        
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
        // 설정에서 성능 등급 기준 로드
        val ultraCores = settingsService.systemPerformanceUltraCores
        val highCores = settingsService.systemPerformanceHighCores
        val mediumCores = settingsService.systemPerformanceMediumCores
        val ultraMemory = settingsService.systemPerformanceUltraMemory * 1024 * 1024 * 1024  // GB를 바이트로 변환
        val highMemory = settingsService.systemPerformanceHighMemory * 1024 * 1024 * 1024
        val mediumMemory = settingsService.systemPerformanceMediumMemory * 1024 * 1024 * 1024
        
        return when {
            specs.cpuCores >= ultraCores && specs.totalMemory >= ultraMemory -> PerformanceTier.ULTRA
            specs.cpuCores >= highCores && specs.totalMemory >= highMemory -> PerformanceTier.HIGH
            specs.cpuCores >= mediumCores && specs.totalMemory >= mediumMemory -> PerformanceTier.MEDIUM
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
        
        // ✅ 실시간 성능 최우선 JVM 최적화 (설정에서 값 로드)
        val gcPause = settingsService.systemJvmGcPause
        val heapRegionSize = settingsService.systemJvmHeapRegionSize
        val concurrentThreads = settingsService.systemJvmConcurrentThreads
        val parallelThreads = settingsService.systemJvmParallelThreads
        
        System.setProperty("spring.jvm.gc", "G1GC")
        System.setProperty("spring.jvm.gc.pause", gcPause.toString())  // 설정에서 GC 일시정지 시간 로드
        System.setProperty("spring.jvm.gc.heap.region.size", "${heapRegionSize}m")  // 설정에서 힙 영역 크기 로드
        System.setProperty("spring.jvm.gc.concurrent.threads", concurrentThreads.toString())  // 설정에서 동시 스레드 수 로드
        System.setProperty("spring.jvm.gc.parallel.threads", parallelThreads.toString())   // 설정에서 병렬 스레드 수 로드
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
        @Suppress("UNUSED_PARAMETER") maxThreads: Int,
        @Suppress("UNUSED_PARAMETER") queueCapacity: Int
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
     * ✅ 실시간 실행기 반환 (하위 호환성)
     */
    @Deprecated("Use getHardwareExecutor() instead")
    fun getRealtimeExecutor(): ScheduledExecutorService? {
        return realtimeExecutor
    }
    
    /**
     * ✅ 모드 실행기 반환 (하위 호환성)
     */
    @Deprecated("Use getTrackingExecutor() instead")
    fun getModeExecutor(): ScheduledExecutorService? {
        return modeExecutor
    }
    
    /**
     * ✅ 배치 실행기 반환 (하위 호환성)
     */
    // ✅ 통합 스레드 풀 접근 메서드들
    
    /**
     * ✅ UDP 통신 실행기 반환 (CRITICAL 우선순위)
     */
    fun getUdpExecutor(): ScheduledExecutorService? {
        return udpExecutor
    }
    
    /**
     * ✅ WebSocket 브로드캐스트 실행기 반환 (HIGH 우선순위)
     */
    fun getWebsocketExecutor(): ScheduledExecutorService? {
        return websocketExecutor
    }
    
    /**
     * ✅ 통합 추적 실행기 반환 (NORMAL 우선순위)
     */
    fun getTrackingExecutor(): ScheduledExecutorService? {
        return trackingExecutor
    }
    
    /**
     * ✅ 배치 저장 실행기 반환 (LOW 우선순위)
     */
    fun getBatchExecutor(): ExecutorService? {
        return batchExecutor
    }
    
    /**
     * ✅ 배치 스케줄링 실행기 반환 (LOW 우선순위)
     */
    fun getBatchScheduler(): ScheduledExecutorService? {
        return batchScheduler
    }
    
    /**
     * ✅ 계산 처리 실행기 반환 (LOW 우선순위)
     */
    fun getCalculationExecutor(): ScheduledExecutorService? {
        return calculationExecutor
    }
    
    /**
     * ✅ 스레드 풀 상태 모니터링
     */
    fun getThreadPoolStats(): Map<String, Map<String, Any>> {
        return mapOf(
            "udpExecutor" to getExecutorStats(udpExecutor),
            "websocketExecutor" to getExecutorStats(websocketExecutor),
            "trackingExecutor" to getExecutorStats(trackingExecutor),
            "batchExecutor" to getExecutorStats(batchExecutor),
            "batchScheduler" to getExecutorStats(batchScheduler),
            "calculationExecutor" to getExecutorStats(calculationExecutor)
        )
    }
    
    /**
     * ✅ 개별 스레드 풀 상태 조회
     */
    private fun getExecutorStats(executor: Any?): Map<String, Any> {
        return when (executor) {
            is ScheduledExecutorService -> mapOf(
                "type" to "ScheduledExecutorService",
                "isShutdown" to executor.isShutdown,
                "isTerminated" to executor.isTerminated,
                "activeThreads" to "N/A" // ScheduledExecutorService는 직접적인 활성 스레드 수 조회 불가
            )
            is ExecutorService -> mapOf(
                "type" to "ExecutorService",
                "isShutdown" to executor.isShutdown,
                "isTerminated" to executor.isTerminated,
                "activeThreads" to "N/A" // ExecutorService는 직접적인 활성 스레드 수 조회 불가
            )
            else -> mapOf(
                "type" to "null",
                "isShutdown" to true,
                "isTerminated" to true,
                "activeThreads" to 0
            )
        }
    }
    
    /**
     * ✅ CPU 모델 정보 가져오기
     */
    private fun getCpuModel(): String {
        return try {
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
        logger.info("🔄 스레드 풀 정리 시작")
        
        // ✅ 기존 스레드 풀 정리
        realtimeExecutor?.shutdown()
        modeExecutor?.shutdown()
        batchExecutor?.shutdown()
        
        // ✅ 통합 스레드 풀 정리
        udpExecutor?.shutdown()
        websocketExecutor?.shutdown()
        trackingExecutor?.shutdown()
        batchScheduler?.shutdown()
        calculationExecutor?.shutdown()
        
        try {
            // ✅ 기존 스레드 풀 종료 대기
            if (!realtimeExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                realtimeExecutor?.shutdownNow()
            }
            if (!modeExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                modeExecutor?.shutdownNow()
            }
            if (!batchExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                batchExecutor?.shutdownNow()
            }
            
            // ✅ 통합 스레드 풀 종료 대기
            if (!udpExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                udpExecutor?.shutdownNow()
            }
            if (!websocketExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                websocketExecutor?.shutdownNow()
            }
            if (!trackingExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                trackingExecutor?.shutdownNow()
            }
            if (!batchScheduler?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                batchScheduler?.shutdownNow()
            }
            if (!calculationExecutor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
                calculationExecutor?.shutdownNow()
            }
        } catch (e: InterruptedException) {
            // ✅ 강제 종료
            realtimeExecutor?.shutdownNow()
            modeExecutor?.shutdownNow()
            batchExecutor?.shutdownNow()
            udpExecutor?.shutdownNow()
            websocketExecutor?.shutdownNow()
            trackingExecutor?.shutdownNow()
            batchScheduler?.shutdownNow()
            calculationExecutor?.shutdownNow()
        }
        
        logger.info("✅ 모든 스레드 풀 정리 완료")
    }
} 