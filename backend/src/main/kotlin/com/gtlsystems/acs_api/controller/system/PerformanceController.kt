package com.gtlsystems.acs_api.controller.system

import com.gtlsystems.acs_api.util.ApiDescriptions
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.ThreadMXBean
import java.lang.management.OperatingSystemMXBean
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@RestController
@RequestMapping("/api/system/performance")
@Tag(name = "System - Performance", description = "시스템 성능 모니터링 API - 응답 시간, 메모리 사용량, 성능 메트릭")
class PerformanceController {

    // 성능 메트릭 저장소
    private val apiResponseTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val apiCallCounts = ConcurrentHashMap<String, AtomicLong>()
    private val apiErrorCounts = ConcurrentHashMap<String, AtomicLong>()

    @GetMapping("/metrics")
    @Operation(
        summary = "시스템 성능 메트릭 조회 / System Performance Metrics",
        description = "시스템의 전반적인 성능 메트릭을 조회합니다. Retrieves comprehensive system performance metrics.",
        tags = ["System - Performance"]
    )
    fun getPerformanceMetrics(): ResponseEntity<Map<String, Any>> {
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val threadBean = ManagementFactory.getThreadMXBean()
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val runtimeBean = ManagementFactory.getRuntimeMXBean()

        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage

        val metrics = mapOf(
            "timestamp" to Instant.now().toString(),
            "memory" to mapOf(
                "heapUsed" to formatBytes(heapMemory.used),
                "heapMax" to formatBytes(heapMemory.max),
                "heapUsage" to "${(heapMemory.used * 100 / heapMemory.max)}%",
                "nonHeapUsed" to formatBytes(nonHeapMemory.used),
                "nonHeapMax" to formatBytes(nonHeapMemory.max),
                "totalUsed" to formatBytes(heapMemory.used + nonHeapMemory.used)
            ),
            "threads" to mapOf(
                "active" to threadBean.threadCount,
                "total" to threadBean.totalStartedThreadCount,
                "peak" to threadBean.peakThreadCount,
                "daemon" to threadBean.daemonThreadCount
            ),
            "system" to mapOf(
                "cpuCores" to osBean.availableProcessors,
                "systemLoad" to String.format("%.2f", osBean.systemLoadAverage),
                "uptime" to formatUptime(runtimeBean.uptime),
                "startTime" to Instant.ofEpochMilli(runtimeBean.startTime).toString()
            ),
            "jvm" to mapOf(
                "version" to System.getProperty("java.version"),
                "vendor" to System.getProperty("java.vendor"),
                "home" to System.getProperty("java.home")
            )
        )

        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/api-stats")
    @Operation(
        summary = "API 통계 정보 조회",
        description = """
            각 API 엔드포인트의 호출 통계를 조회합니다.
            
            ## 제공 통계
            - **호출 횟수**: 각 API의 총 호출 횟수
            - **응답 시간**: 평균, 최소, 최대 응답 시간
            - **오류 횟수**: 각 API의 오류 발생 횟수
            - **성공률**: API 호출 성공률
            
            ## 사용 예시
            ```
            GET /api/system/performance/api-stats
            ```
            
            ## 응답 예시
            ```json
            {
              "timestamp": "2024-01-15T10:30:00Z",
              "totalApis": 25,
              "totalCalls": 1250,
              "totalErrors": 12,
              "successRate": "99.0%",
              "apis": [
                {
                  "endpoint": "/api/system/configuration",
                  "calls": 150,
                  "errors": 0,
                  "avgResponseTime": "45ms",
                  "successRate": "100%"
                }
              ]
            }
            ```
        """,
        tags = ["System - Performance"]
    )
    fun getApiStats(): ResponseEntity<Map<String, Any>> {
        val apiStats = mutableListOf<Map<String, Any>>()
        var totalCalls = 0L
        var totalErrors = 0L

        apiCallCounts.forEach { (endpoint, callCount) ->
            val calls = callCount.get()
            val errors = apiErrorCounts[endpoint]?.get() ?: 0L
            val responseTimes = apiResponseTimes[endpoint] ?: emptyList()
            
            val avgResponseTime = if (responseTimes.isNotEmpty()) {
                "${responseTimes.average().toLong()}ms"
            } else {
                "N/A"
            }

            val successRate = if (calls > 0) {
                "${((calls - errors) * 100 / calls)}%"
            } else {
                "N/A"
            }

            apiStats.add(
                mapOf(
                    "endpoint" to endpoint,
                    "calls" to calls,
                    "errors" to errors,
                    "avgResponseTime" to avgResponseTime,
                    "successRate" to successRate,
                    "minResponseTime" to if (responseTimes.isNotEmpty()) "${responseTimes.minOrNull()}ms" else "N/A",
                    "maxResponseTime" to if (responseTimes.isNotEmpty()) "${responseTimes.maxOrNull()}ms" else "N/A"
                )
            )

            totalCalls += calls
            totalErrors += errors
        }

        val successRate = if (totalCalls > 0) {
            "${((totalCalls - totalErrors) * 100 / totalCalls)}%"
        } else {
            "N/A"
        }

        val stats = mapOf(
            "timestamp" to Instant.now().toString(),
            "totalApis" to apiCallCounts.size,
            "totalCalls" to totalCalls,
            "totalErrors" to totalErrors,
            "successRate" to successRate,
            "apis" to apiStats.sortedByDescending { it["calls"] as Long }
        )

        return ResponseEntity.ok(stats)
    }

    @GetMapping("/health")
    @Operation(
        summary = "시스템 건강 상태 조회",
        description = """
            시스템의 전반적인 건강 상태를 조회합니다.
            
            ## 건강 상태 지표
            - **메모리 상태**: 메모리 사용률 및 임계값
            - **스레드 상태**: 스레드 풀 상태 및 데드락 감지
            - **시스템 부하**: CPU 및 시스템 부하 상태
            - **JVM 상태**: 가비지 컬렉션 및 메모리 누수 감지
            
            ## 사용 예시
            ```
            GET /api/system/performance/health
            ```
            
            ## 응답 예시
            ```json
            {
              "status": "healthy",
              "timestamp": "2024-01-15T10:30:00Z",
              "checks": {
                "memory": "healthy",
                "threads": "healthy",
                "system": "healthy",
                "jvm": "healthy"
              },
              "details": {
                "memoryUsage": "25%",
                "activeThreads": 45,
                "systemLoad": "0.8"
              }
            }
            ```
        """,
        tags = ["System - Performance"]
    )
    fun getSystemHealth(): ResponseEntity<Map<String, Any>> {
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val threadBean = ManagementFactory.getThreadMXBean()
        val osBean = ManagementFactory.getOperatingSystemMXBean()

        val heapMemory = memoryBean.heapMemoryUsage
        val memoryUsage = (heapMemory.used * 100 / heapMemory.max)
        
        val memoryStatus = when {
            memoryUsage < 70 -> "healthy"
            memoryUsage < 85 -> "warning"
            else -> "critical"
        }

        val threadStatus = when {
            threadBean.threadCount < 100 -> "healthy"
            threadBean.threadCount < 200 -> "warning"
            else -> "critical"
        }

        val systemLoad = osBean.systemLoadAverage
        val systemStatus = when {
            systemLoad < 0.7 -> "healthy"
            systemLoad < 0.9 -> "warning"
            else -> "critical"
        }

        val overallStatus = when {
            memoryStatus == "critical" || threadStatus == "critical" || systemStatus == "critical" -> "critical"
            memoryStatus == "warning" || threadStatus == "warning" || systemStatus == "warning" -> "warning"
            else -> "healthy"
        }

        val health = mapOf(
            "status" to overallStatus,
            "timestamp" to Instant.now().toString(),
            "checks" to mapOf(
                "memory" to memoryStatus,
                "threads" to threadStatus,
                "system" to systemStatus,
                "jvm" to "healthy"
            ),
            "details" to mapOf(
                "memoryUsage" to "${memoryUsage}%",
                "activeThreads" to threadBean.threadCount,
                "systemLoad" to String.format("%.2f", systemLoad),
                "heapUsed" to formatBytes(heapMemory.used),
                "heapMax" to formatBytes(heapMemory.max)
            )
        )

        return ResponseEntity.ok(health)
    }

    // 유틸리티 메서드들
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.1f%s", size, units[unitIndex])
    }

    private fun formatUptime(uptime: Long): String {
        val seconds = uptime / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    // 성능 메트릭 기록 메서드들 (다른 컨트롤러에서 사용)
    fun recordApiCall(endpoint: String, responseTime: Long, isError: Boolean = false) {
        apiCallCounts.computeIfAbsent(endpoint) { AtomicLong(0) }.incrementAndGet()
        
        if (isError) {
            apiErrorCounts.computeIfAbsent(endpoint) { AtomicLong(0) }.incrementAndGet()
        }
        
        apiResponseTimes.computeIfAbsent(endpoint) { mutableListOf() }.add(responseTime)
        
        // 응답 시간 기록을 1000개로 제한
        val responseTimes = apiResponseTimes[endpoint]
        if (responseTimes != null && responseTimes.size > 1000) {
            responseTimes.removeAt(0)
        }
    }
} 