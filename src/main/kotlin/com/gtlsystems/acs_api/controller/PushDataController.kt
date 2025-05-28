package com.gtlsystems.acs_api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.service.PushDataService
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class PushDataController(
    private val pushDataService: PushDataService,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(PushDataController::class.java)

    // === 세션 관리 ===
    private val connectedSessions = ConcurrentHashMap<String, SessionInfo>()
    private val totalConnections = AtomicLong(0)
    private val activeConnections = AtomicInteger(0)

    // === 성능 모니터링 ===
    private val messagesSent = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val transmissionCount = AtomicLong(0)

    // === 실시간 전송 설정 ===
    companion object {
        const val REALTIME_TRANSMISSION_INTERVAL_MS = 30L  // 30ms 주기
        const val MAX_PROCESSING_TIME_MS = 25L  // 25ms 이상이면 경고
        const val SESSION_TIMEOUT_MS = 30000L  // 30초 타임아웃
    }

    // === 세션 정보 클래스 (실시간 전송 전용) ===
    private data class SessionInfo(
        val sessionId: String,
        val session: WebSocketSession,
        val connectedTime: Long = System.currentTimeMillis(),
        val messagesSent: AtomicLong = AtomicLong(0),
        val lastDataSent: AtomicLong = AtomicLong(System.currentTimeMillis()),
        val isActive: AtomicBoolean = AtomicBoolean(true),
        val executor: ScheduledExecutorService,
        val errorCount: AtomicLong = AtomicLong(0),
        val threadName: String // ✅ 스레드 이름 추가
    )

    override fun handle(session: WebSocketSession): Mono<Void> {
        val sessionId = session.id
        val connectionNumber = totalConnections.incrementAndGet()

        logger.info("🔗 실시간 WebSocket 연결 #{}: {} (30ms 주기)", connectionNumber, sessionId)

        // ✅ 세션별 고유한 스레드 팩토리 생성
        val shortSessionId = sessionId.take(8) // 세션 ID 앞 8자리만 사용
        val threadName = "websocket-$shortSessionId"

        val sessionThreadFactory = ThreadFactory { runnable ->
            Thread(runnable, threadName).apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY + 1  // UDP보다 낮지만 일반보다 높음

                // ✅ 상세한 예외 처리기
                uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, ex ->
                    logger.error("🚨 WebSocket 세션 [{}] 스레드 [{}] 예외 발생: {}",
                        shortSessionId, thread.name, ex.message, ex)

                    // 세션 정리 시도
                    try {
                        handleDisconnection(sessionId)
                    } catch (cleanupEx: Exception) {
                        logger.error("세션 정리 중 추가 오류: {}", cleanupEx.message, cleanupEx)
                    }
                }

                logger.debug("🧵 세션 [{}] 전용 스레드 생성: {}", shortSessionId, threadName)
            }
        }

        // ✅ 세션별 전용 스케줄러 생성
        val sessionExecutor = Executors.newSingleThreadScheduledExecutor(sessionThreadFactory)

        logger.info("🏗️ 세션 [{}] 전용 스케줄러 생성 완료 - 스레드: {}", shortSessionId, threadName)

        // ✅ 세션 정보 등록 (스레드 이름 포함)
        val sessionInfo = SessionInfo(
            sessionId = sessionId,
            session = session,
            executor = sessionExecutor,
            threadName = threadName
        )
        connectedSessions[sessionId] = sessionInfo
        activeConnections.incrementAndGet()

        // ✅ PushDataService에 클라이언트 연결 알림 및 초기 데이터 전송
        try {
            val initialData = pushDataService.clientConnected()
            sendInitialData(session, initialData, sessionInfo)
            logger.info("📤 세션 [{}] 초기 데이터 전송 완료", shortSessionId)
        } catch (e: Exception) {
            logger.warn("⚠️ 세션 [{}] 클라이언트 연결 알림 실패: {}", shortSessionId, e.message)
        }

        // ✅ 30ms 주기 실시간 데이터 전송 시작
        startRealtimeTransmission(sessionInfo)

        // ✅ 세션 종료 처리 (스레드 이름 포함 로깅)
        return session.closeStatus()
            .doOnNext { status ->
                logger.info("🔌 실시간 세션 [{}] 스레드 [{}] 종료: {}",
                    shortSessionId, threadName, status.code)
                handleDisconnection(sessionId)
            }
            .doOnError { error ->
                logger.debug("세션 [{}] 스레드 [{}] 종료 감지 오류: {}",
                    shortSessionId, threadName, error.message)
                handleDisconnection(sessionId)
            }
            .onErrorResume { error ->
                logger.debug("세션 [{}] 스레드 [{}] 종료 복구", shortSessionId, threadName)
                handleDisconnection(sessionId)
                Mono.empty()
            }
            .then()
    }

    /**
     * ✅ 초기 데이터 전송 (스레드 정보 포함 로깅)
     */
    private fun sendInitialData(session: WebSocketSession, initialData: String, sessionInfo: SessionInfo) {
        val shortSessionId = sessionInfo.sessionId.take(8)

        if (initialData.isNotEmpty()) {
            try {
                session.send(Mono.just(session.textMessage(initialData)))
                    .subscribeOn(Schedulers.boundedElastic()) // ✅ 명시적 스케줄러 지정
                    .subscribe(
                        {
                            sessionInfo.messagesSent.incrementAndGet()
                            sessionInfo.lastDataSent.set(System.currentTimeMillis())
                            messagesSent.incrementAndGet()
                            logger.debug("📤 세션 [{}] 스레드 [{}] 초기 데이터 전송 성공 ({}자)",
                                shortSessionId, sessionInfo.threadName, initialData.length)
                        },
                        { error ->
                            errorCount.incrementAndGet()
                            logger.warn("⚠️ 세션 [{}] 스레드 [{}] 초기 데이터 전송 실패: {}",
                                shortSessionId, sessionInfo.threadName, error.message)
                        }
                    )
            } catch (e: Exception) {
                errorCount.incrementAndGet()
                logger.error("❌ 세션 [{}] 스레드 [{}] 초기 데이터 전송 중 예외: {}",
                    shortSessionId, sessionInfo.threadName, e.message, e)
            }
        }
    }

    /**
     * ✅ 30ms 주기 실시간 데이터 전송 시작 (상세 로깅)
     */
    private fun startRealtimeTransmission(sessionInfo: SessionInfo) {
        val sessionId = sessionInfo.sessionId
        val shortSessionId = sessionId.take(8)
        val threadName = sessionInfo.threadName

        logger.info("🚀 세션 [{}] 스레드 [{}] 실시간 전송 시작 ({}ms 주기)",
            shortSessionId, threadName, REALTIME_TRANSMISSION_INTERVAL_MS)

        // ✅ 스케줄러 상태 확인
        logger.debug("🧵 세션 [{}] 스케줄러 상태 - isShutdown: {}, isTerminated: {}",
            shortSessionId, sessionInfo.executor.isShutdown, sessionInfo.executor.isTerminated)

        // ✅ 즉시 한 번 실행해보기 (연결 테스트)
        try {
            logger.debug("🔥 세션 [{}] 즉시 실행 테스트 시작", shortSessionId)
            sendRealtimeData(sessionInfo)
            logger.debug("🔥 세션 [{}] 즉시 실행 테스트 완료", shortSessionId)
        } catch (e: Exception) {
            logger.error("💥 세션 [{}] 즉시 실행 테스트 실패: {}", shortSessionId, e.message, e)
        }

        // ✅ 30ms 주기로 정확한 실시간 데이터 전송
        sessionInfo.executor.scheduleAtFixedRate({
            try {
                // 스케줄러 실행 확인 (디버그 레벨)
               /* logger.debug("⏰ 세션 [{}] 스레드 [{}] 스케줄러 실행 - 시간: {}",
                    shortSessionId, threadName, System.currentTimeMillis())
*/
                if (sessionInfo.isActive.get() && sessionInfo.session.isOpen) {
                    val startTime = System.nanoTime()

                    sendRealtimeData(sessionInfo)

                    // ✅ 성능 모니터링
                    val processingTime = (System.nanoTime() - startTime) / 1_000_000
                    if (processingTime > MAX_PROCESSING_TIME_MS) {
                        logger.warn("🚨 세션 [{}] 스레드 [{}] 실시간 전송 지연: {}ms",
                            shortSessionId, threadName, processingTime)
                    }
                } else {
                    //logger.debug("⚠️ 세션 [{}] 스레드 [{}] 비활성 상태 - isActive: {}, isOpen: {}",
                        //shortSessionId, threadName, sessionInfo.isActive.get(), sessionInfo.session.isOpen)
                    // ✅ 비활성 상태 감지 시 즉시 정리
                    logger.warn("⚠️ 세션 [{}] 스레드 [{}] 비활성 상태 감지 - 즉시 정리 시작",
                        shortSessionId, threadName)
                    handleDisconnection(sessionId)
                    return@scheduleAtFixedRate // 스케줄러 태스크 종료
                }
            } catch (e: Exception) {
                sessionInfo.errorCount.incrementAndGet()
                errorCount.incrementAndGet()
                logger.debug("세션 [{}] 스레드 [{}] 스케줄러 실행 중 오류: {}",
                    shortSessionId, threadName, e.message)

                // ✅ 연속 오류 시 세션 정리
                if (sessionInfo.errorCount.get() > 20) {
                    logger.warn("세션 [{}] 스레드 [{}] 연속 오류({}회)로 인한 정리",
                        shortSessionId, threadName, sessionInfo.errorCount.get())
                    handleDisconnection(sessionId)
                }
            }
        }, 1000, REALTIME_TRANSMISSION_INTERVAL_MS, TimeUnit.MILLISECONDS) // 1초 후 시작

        logger.info("✅ 세션 [{}] 스레드 [{}] 실시간 전송 스케줄링 완료", shortSessionId, threadName)
    }

    /**
     * ✅ 실시간 데이터 전송 (Reactor 스케줄러 문제 해결)
     */
    private fun sendRealtimeData(sessionInfo: SessionInfo) {
        try {
            val session = sessionInfo.session
            val sessionId = sessionInfo.sessionId
            val shortSessionId = sessionId.take(8)
            val threadName = sessionInfo.threadName

            // ✅ PushDataService에서 실시간 데이터 가져오기
            val realtimeData = pushDataService.generateRealtimeData()

            if (realtimeData.isNotEmpty()) {
                // ✅ Reactor 스케줄러 문제 해결: subscribeOn 사용
                session.send(Mono.just(session.textMessage(realtimeData)))
                    .subscribeOn(Schedulers.boundedElastic()) // 명시적 스케줄러 지정
                    .subscribe(
                        //해당 쓰레드는 IDE랑 따로 돌아가므로 브레이크 포인트는 걸리지 않음. 로그 또한 확인 불가.
                        {
                            //logger.info("🔍 [DEBUG] 세션 [{}] 전송 성공 콜백 시작", shortSessionId)
                            // ✅ 전송 성공
                            val currentTime = System.currentTimeMillis()
                            sessionInfo.messagesSent.incrementAndGet()
                            sessionInfo.lastDataSent.set(currentTime)
                            messagesSent.incrementAndGet()
                            transmissionCount.incrementAndGet()
                           // logger.info("🔍 [DEBUG] 세션 [{}] 통계 업데이트 완료 - 메시지: {}, 시간: {}",
                            //    shortSessionId, sessionInfo.messagesSent.get(), currentTime)

                            // ✅ 주기적 로깅 (5초마다 또는 100개 메시지마다)
                            val messageCount = sessionInfo.messagesSent.get()
                            if (messageCount % 100 == 0L) {
                               // logger.info("📤 세션 [{}] 스레드 [{}] 실시간 전송 중 - 총 {}개 메시지",
                                 //   shortSessionId, threadName, messageCount)
                            }

                            // 디버그 로깅
                           // logger.debug("📤 세션 [{}] 스레드 [{}] 실시간 데이터 전송 성공 ({}자)",
                            //    shortSessionId, threadName, realtimeData.length)
                        },
                        { error ->
                            // ✅ 전송 실패
                            sessionInfo.errorCount.incrementAndGet()
                            errorCount.incrementAndGet()
                            logger.warn("⚠️ 세션 [{}] 스레드 [{}] 실시간 전송 실패: {}",
                                shortSessionId, threadName, error.message)

                            // ✅ 장시간 전송 실패 시 세션 정리
                            val timeSinceLastSuccess = System.currentTimeMillis() - sessionInfo.lastDataSent.get()
                            if (timeSinceLastSuccess > SESSION_TIMEOUT_MS) {
                                logger.warn("세션 [{}] 스레드 [{}] 장시간 실시간 전송 실패 ({}ms), 정리",
                                    shortSessionId, threadName, timeSinceLastSuccess)
                                handleDisconnection(sessionId)
                            }
                        }
                    )
            } else {
                logger.debug("세션 [{}] 스레드 [{}] 실시간 데이터 없음", shortSessionId, threadName)
            }

        } catch (e: Exception) {
            sessionInfo.errorCount.incrementAndGet()
            errorCount.incrementAndGet()
            val shortSessionId = sessionInfo.sessionId.take(8)
            logger.error("💥 세션 [{}] 스레드 [{}] 실시간 데이터 전송 중 예외: {}",
                shortSessionId, sessionInfo.threadName, e.message, e)
        }
    }

    /**
     * ✅ 연결 해제 처리 (상세 스레드 정리)
     */
    private fun handleDisconnection(sessionId: String) {
        val removedSession = connectedSessions.remove(sessionId)
        if (removedSession != null) {
            val shortSessionId = sessionId.take(8)
            val threadName = removedSession.threadName

            // ✅ 즉시 비활성화
            removedSession.isActive.set(false)
            activeConnections.decrementAndGet()

            logger.info("🔄 세션 [{}] 스레드 [{}] 정리 시작", shortSessionId, threadName)

            // ✅ 세션별 스케줄러 즉시 종료
            try {
                removedSession.executor.shutdown()
                if (!removedSession.executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    logger.warn("⚠️ 세션 [{}] 스레드 [{}] 정상 종료 실패, 강제 종료", shortSessionId, threadName)
                    val shutdownTasks = removedSession.executor.shutdownNow()
                    logger.info("강제 종료된 태스크 수: {}", shutdownTasks.size)
                } else {
                    logger.info("✅ 세션 [{}] 스레드 [{}] 정상 종료 완료", shortSessionId, threadName)
                }
            } catch (e: InterruptedException) {
                logger.warn("⚠️ 세션 [{}] 스레드 [{}] 종료 중 인터럽트", shortSessionId, threadName)
                removedSession.executor.shutdownNow()
                Thread.currentThread().interrupt()
            }

            // ✅ PushDataService에 클라이언트 해제 알림
            try {
                pushDataService.clientDisconnected()
                logger.debug("📉 세션 [{}] 클라이언트 해제 알림 완료", shortSessionId)
            } catch (e: Exception) {
                logger.warn("⚠️ 세션 [{}] 클라이언트 해제 알림 실패: {}", shortSessionId, e.message)
            }

            // ✅ 상세 통계 로깅
            val connectionDuration = System.currentTimeMillis() - removedSession.connectedTime
            val totalMessages = removedSession.messagesSent.get()
            val sessionErrors = removedSession.errorCount.get()
            val avgMessagesPerSecond = if (connectionDuration > 0) {
                (totalMessages * 1000.0 / connectionDuration)
            } else 0.0

            logger.info(
                "📊 세션 [{}] 스레드 [{}] 해제 완료 - 지속: {}ms, 메시지: {}개, 오류: {}회, 평균: {:.1f}msg/s",
                shortSessionId, threadName, connectionDuration, totalMessages, sessionErrors, avgMessagesPerSecond
            )
        } else {
            logger.debug("세션 [{}] 이미 정리됨", sessionId.take(8))
        }
    }

    // === 📊 실시간 성능 모니터링 메서드들 (스레드 정보 포함) ===

    /**
     * ✅ 실시간 연결 통계 (스레드 정보 포함)
     */
    fun getRealtimeStats(): Map<String, Any> {
        val currentTime = System.currentTimeMillis()
        val activeSessions = connectedSessions.values.filter { it.isActive.get() }

        val threadNames = activeSessions.map { it.threadName }

        return mapOf(
            "totalConnections" to totalConnections.get(),
            "activeConnections" to activeConnections.get(),
            "realtimeSessions" to activeSessions.size,
            "messagesSent" to messagesSent.get(),
            "transmissionCount" to transmissionCount.get(),
            "errorCount" to errorCount.get(),
            "averageConnectionDuration" to calculateAverageConnectionDuration(activeSessions, currentTime),
            "transmissionInterval" to "${REALTIME_TRANSMISSION_INTERVAL_MS}ms",
            "architecture" to "Session-Specific Thread WebSocket Controller",
            "threadPriority" to "NORM_PRIORITY+1",
            "serviceRole" to "WebSocket Transmission Only",
            "activeThreads" to threadNames, // ✅ 활성 스레드 이름 목록
            "threadNamingPattern" to "websocket-{sessionId8}",
            "features" to listOf(
                "30ms 정확한 주기",
                "세션별 전용 스레드",
                "고유한 스레드 이름",
                "상세한 스레드 모니터링",
                "자동 오류 복구",
                "성능 모니터링"
            ),
            "serverTime" to currentTime
        )
    }

    /**
     * ✅ 평균 연결 지속 시간 계산
     */
    private fun calculateAverageConnectionDuration(sessions: List<SessionInfo>, currentTime: Long): Long {
        if (sessions.isEmpty()) return 0
        return sessions.map { currentTime - it.connectedTime }.average().toLong()
    }
    /**
     * ✅ 실시간 세션 상세 정보 (스레드 정보 포함)
     */
    fun getRealtimeSessionDetails(sessionId: String): Map<String, Any>? {
        val sessionInfo = connectedSessions[sessionId] ?: return null
        val currentTime = System.currentTimeMillis()

        return mapOf(
            "sessionId" to sessionInfo.sessionId,
            "shortSessionId" to sessionInfo.sessionId.take(8),
            "threadName" to sessionInfo.threadName, // ✅ 스레드 이름
            "connectedTime" to sessionInfo.connectedTime,
            "connectionDuration" to (currentTime - sessionInfo.connectedTime),
            "isActive" to sessionInfo.isActive.get(),
            "messagesSent" to sessionInfo.messagesSent.get(),
            "lastDataSent" to sessionInfo.lastDataSent.get(),
            "timeSinceLastData" to (currentTime - sessionInfo.lastDataSent.get()),
            "errorCount" to sessionInfo.errorCount.get(),
            "transmissionInterval" to "${REALTIME_TRANSMISSION_INTERVAL_MS}ms",
            "isSessionOpen" to sessionInfo.session.isOpen,
            "messagesPerSecond" to calculateMessagesPerSecond(sessionInfo, currentTime),
            "executorStatus" to mapOf( // ✅ 스케줄러 상태 정보
                "isShutdown" to sessionInfo.executor.isShutdown,
                "isTerminated" to sessionInfo.executor.isTerminated
            ),
            "threadInfo" to mapOf( // ✅ 스레드 상세 정보
                "threadName" to sessionInfo.threadName,
                "threadPattern" to "websocket-{sessionId8}",
                "priority" to "NORM_PRIORITY+1",
                "isDaemon" to true
            )
        )
    }

    /**
     * ✅ 초당 메시지 수 계산
     */
    private fun calculateMessagesPerSecond(sessionInfo: SessionInfo, currentTime: Long): Double {
        val durationSeconds = (currentTime - sessionInfo.connectedTime) / 1000.0
        return if (durationSeconds > 0) {
            sessionInfo.messagesSent.get() / durationSeconds
        } else 0.0
    }

    /**
     * ✅ 모든 실시간 세션 목록 (스레드 정보 포함)
     */
    fun getRealtimeSessions(): List<Map<String, Any>> {
        return connectedSessions.values
            .filter { it.isActive.get() }
            .map { sessionInfo ->
                val currentTime = System.currentTimeMillis()
                mapOf(
                    "sessionId" to sessionInfo.sessionId,
                    "shortSessionId" to sessionInfo.sessionId.take(8),
                    "threadName" to sessionInfo.threadName, // ✅ 스레드 이름
                    "connectionDuration" to (currentTime - sessionInfo.connectedTime),
                    "messagesSent" to sessionInfo.messagesSent.get(),
                    "timeSinceLastData" to (currentTime - sessionInfo.lastDataSent.get()),
                    "errorCount" to sessionInfo.errorCount.get(),
                    "isSessionOpen" to sessionInfo.session.isOpen,
                    "messagesPerSecond" to calculateMessagesPerSecond(sessionInfo, currentTime),
                    "executorHealthy" to (!sessionInfo.executor.isShutdown && !sessionInfo.executor.isTerminated)
                )
            }
    }

    /**
     * ✅ 실시간 상태 요약 (스레드 정보 포함)
     */
    fun getRealtimeSummary(): String {
        val stats = getRealtimeStats()
        val sessions = getRealtimeSessions()

        return buildString {
            appendLine("=== 실시간 WebSocket 상태 요약 ===")
            appendLine("📊 전체 연결: ${stats["totalConnections"]}회")
            appendLine("🔗 활성 연결: ${stats["activeConnections"]}개")
            appendLine("⚡ 실시간 세션: ${stats["realtimeSessions"]}개")
            appendLine("📤 송신 메시지: ${stats["messagesSent"]}개")
            appendLine("🔄 전송 횟수: ${stats["transmissionCount"]}개")
            appendLine("❌ 오류 발생: ${stats["errorCount"]}회")
            appendLine("⏱️ 전송 간격: ${stats["transmissionInterval"]}")
            appendLine("🏗️ 아키텍처: ${stats["architecture"]}")
            appendLine("🧵 스레드 우선순위: ${stats["threadPriority"]}")
            appendLine("🎯 역할: ${stats["serviceRole"]}")
            appendLine("📛 스레드 패턴: ${stats["threadNamingPattern"]}")

            // ✅ 활성 스레드 목록
            val activeThreads = stats["activeThreads"] as List<*>
            if (activeThreads.isNotEmpty()) {
                appendLine("\n=== 활성 스레드 목록 ===")
                activeThreads.forEachIndexed { index, threadName ->
                    appendLine("${index + 1}. $threadName")
                }
            }

            if (sessions.isNotEmpty()) {
                appendLine("\n=== 실시간 세션 상세 ===")
                sessions.forEachIndexed { index, session ->
                    val mps = String.format("%.1f", session["messagesPerSecond"])
                    val threadName = session["threadName"]
                    val shortId = session["shortSessionId"]
                    appendLine("${index + 1}. [$shortId] $threadName - ${session["messagesSent"]}개 (${mps}msg/s)")
                }
            }
        }
    }

    /**
     * ✅ 실시간 성능 체크 (스레드 건강도 포함)
     */
    fun checkRealtimePerformance(): Map<String, Any> {
        val stats = getRealtimeStats()
        val sessions = getRealtimeSessions()

        val totalMessages = stats["messagesSent"] as Long
        val totalErrors = stats["errorCount"] as Long
        val errorRate = if (totalMessages > 0) {
            (totalErrors.toDouble() / totalMessages.toDouble()) * 100
        } else 0.0

        val avgMessagesPerSecond = sessions.map {
            it["messagesPerSecond"] as Double
        }.average().takeIf { !it.isNaN() } ?: 0.0

        val expectedMessagesPerSecond = 1000.0 / REALTIME_TRANSMISSION_INTERVAL_MS // 약 33.3 msg/s

        // ✅ 스레드 건강도 체크
        val healthyExecutors = sessions.count { it["executorHealthy"] as Boolean }
        val totalExecutors = sessions.size
        val executorHealthRate = if (totalExecutors > 0) {
            (healthyExecutors.toDouble() / totalExecutors.toDouble()) * 100
        } else 100.0

        return mapOf(
            "errorRate" to String.format("%.2f%%", errorRate),
            "avgMessagesPerSecond" to String.format("%.1f", avgMessagesPerSecond),
            "expectedMessagesPerSecond" to String.format("%.1f", expectedMessagesPerSecond),
            "performanceRatio" to String.format("%.1f%%", (avgMessagesPerSecond / expectedMessagesPerSecond) * 100),
            "executorHealthRate" to String.format("%.1f%%", executorHealthRate), // ✅ 스레드 건강도
            "healthyExecutors" to healthyExecutors,
            "totalExecutors" to totalExecutors,
            "isPerformanceGood" to (errorRate < 5.0 && avgMessagesPerSecond > (expectedMessagesPerSecond * 0.8) && executorHealthRate > 90.0),
            "recommendation" to when {
                executorHealthRate < 90.0 -> "스레드 상태가 불안정합니다. 시스템 리소스를 확인하세요."
                errorRate > 5.0 -> "오류율이 높습니다. 네트워크 상태를 확인하세요."
                avgMessagesPerSecond < (expectedMessagesPerSecond * 0.8) -> "전송 성능이 낮습니다. 서버 리소스를 확인하세요."
                else -> "실시간 성능이 양호합니다."
            }
        )
    }

    /**
     * ✅ 비활성 세션 정리 (스레드 정보 포함 로깅)
     */
    fun cleanupInactiveSessions(): Int {
        val currentTime = System.currentTimeMillis()
        var cleanedCount = 0

        val inactiveSessions = connectedSessions.values.filter { sessionInfo ->
            !sessionInfo.isActive.get() ||
                    !sessionInfo.session.isOpen ||
                    (currentTime - sessionInfo.lastDataSent.get()) > SESSION_TIMEOUT_MS ||
                    sessionInfo.executor.isShutdown ||
                    sessionInfo.executor.isTerminated
        }

        inactiveSessions.forEach { sessionInfo ->
            val shortSessionId = sessionInfo.sessionId.take(8)
            val threadName = sessionInfo.threadName

            logger.info("🧹 비활성 세션 [{}] 스레드 [{}] 정리 중...", shortSessionId, threadName)

            handleDisconnection(sessionInfo.sessionId)
            cleanedCount++
        }

        if (cleanedCount > 0) {
            logger.info("🧹 총 {}개 비활성 실시간 세션 및 스레드 정리 완료", cleanedCount)
        }

        return cleanedCount
    }

    /**
     * ✅ 실시간 서비스 상태 체크 (스레드 건강도 포함)
     */
    fun isRealtimeServiceHealthy(): Boolean {
        val stats = getRealtimeStats()
        val performance = checkRealtimePerformance()

        return (stats["activeConnections"] as Int) >= 0 &&
                (performance["isPerformanceGood"] as Boolean)
    }

    /**
     * ✅ 스레드 상태 진단
     */
    fun diagnoseThreadHealth(): Map<String, Any> {
        val sessions = connectedSessions.values.filter { it.isActive.get() }

        val threadDiagnostics = sessions.map { sessionInfo ->
            val shortSessionId = sessionInfo.sessionId.take(8)

            mapOf(
                "sessionId" to shortSessionId,
                "threadName" to sessionInfo.threadName,
                "isExecutorShutdown" to sessionInfo.executor.isShutdown,
                "isExecutorTerminated" to sessionInfo.executor.isTerminated,
                "isSessionOpen" to sessionInfo.session.isOpen,
                "isActive" to sessionInfo.isActive.get(),
                "errorCount" to sessionInfo.errorCount.get(),
                "messagesSent" to sessionInfo.messagesSent.get(),
                "lastDataSent" to sessionInfo.lastDataSent.get(),
                "timeSinceLastData" to (System.currentTimeMillis() - sessionInfo.lastDataSent.get()),
                "healthStatus" to when {
                    sessionInfo.executor.isShutdown || sessionInfo.executor.isTerminated -> "TERMINATED"
                    !sessionInfo.session.isOpen -> "DISCONNECTED"
                    !sessionInfo.isActive.get() -> "INACTIVE"
                    sessionInfo.errorCount.get() > 10 -> "ERROR_PRONE"
                    (System.currentTimeMillis() - sessionInfo.lastDataSent.get()) > SESSION_TIMEOUT_MS -> "TIMEOUT"
                    else -> "HEALTHY"
                }
            )
        }

        val healthySessions = threadDiagnostics.count { (it["healthStatus"] as String) == "HEALTHY" }
        val totalSessions = threadDiagnostics.size

        return mapOf(
            "totalSessions" to totalSessions,
            "healthySessions" to healthySessions,
            "healthyPercentage" to if (totalSessions > 0) {
                String.format("%.1f%%", (healthySessions.toDouble() / totalSessions.toDouble()) * 100)
            } else "100.0%",
            "threadDiagnostics" to threadDiagnostics,
            "overallHealth" to if (totalSessions == 0) "NO_SESSIONS"
            else if (healthySessions.toDouble() / totalSessions.toDouble() > 0.8) "GOOD"
            else if (healthySessions.toDouble() / totalSessions.toDouble() > 0.5) "FAIR"
            else "POOR"
        )
    }

    /**
     * ✅ 아키텍처 정보 (스레드 정보 포함)
     */
    fun getArchitectureInfo(): String {
        return """
        🏗️ 세션별 전용 스레드 WebSocket Controller 아키텍처
        
        📡 역할 분리:
        ├── PushDataService: 데이터 생성 + 클라이언트 카운트 관리
        ├── PushDataController: WebSocket 연결 + 실시간 전송
        └── 목적: 명확한 책임 분리
        
        🧵 스레드 관리:
        ├── 스레드 이름 패턴: websocket-{sessionId8}
        ├── 스레드 우선순위: NORM_PRIORITY+1 (UDP보다 낮음)
        ├── 데몬 스레드: true (메인 스레드 종료 시 자동 정리)
        ├── 예외 처리기: 스레드별 독립적 예외 처리
        └── 생명주기: 세션과 동일한 생명주기
        
        🔄 실시간 전송:
        ├── 30ms 정확한 주기: ScheduledExecutorService
        ├── 세션별 전용 스레드: 독립적 처리
        ├── Reactor 스케줄러: subscribeOn(Schedulers.boundedElastic())
        └── 자동 오류 복구: 연속 오류 시 세션 정리
        
        ⚡ 성능 최적화:
        1. 스레드 격리: 한 세션 문제가 다른 세션에 영향 없음
        2. 처리 시간 모니터링: 25ms 이상 시 경고
        3. 자동 세션 정리: 30초 타임아웃
        4. 상세한 스레드 진단: 실시간 건강도 체크
        
        🎯 장점:
        - 명확한 책임 분리: Service는 데이터, Controller는 전송
        - 스레드 격리: 세션별 독립적 처리
        - 실시간 보장: 30ms 정확한 주기
        - 확장성: 세션 수에 따른 선형 확장
        - 안정성: 자동 오류 복구 및 스레드 관리
        - 모니터링: 상세한 스레드 상태 진단
        """.trimIndent()
    }

    @PreDestroy
    fun cleanup() {
        logger.info("🏁 실시간 WebSocket Controller 종료 시작...")

        // ✅ 모든 실시간 세션 및 스레드 정리
        val sessionIds = connectedSessions.keys.toList()
        logger.info("🧹 총 {}개 세션 정리 예정", sessionIds.size)

        sessionIds.forEach { sessionId ->
            val sessionInfo = connectedSessions[sessionId]
            if (sessionInfo != null) {
                val shortSessionId = sessionId.take(8)
                val threadName = sessionInfo.threadName
                logger.info("🔄 세션 [{}] 스레드 [{}] 정리 중...", shortSessionId, threadName)
            }
            handleDisconnection(sessionId)
        }

        val finalStats = getRealtimeStats()
        val threadDiagnostics = diagnoseThreadHealth()

        logger.info("📊 최종 실시간 통계:")
        logger.info("  총 연결: {}", finalStats["totalConnections"])
        logger.info("  총 메시지: {}", finalStats["messagesSent"])
        logger.info("  총 오류: {}", finalStats["errorCount"])
        logger.info("  아키텍처: {}", finalStats["architecture"])
        logger.info("  스레드 건강도: {}", threadDiagnostics["overallHealth"])

        logger.info("✅ 실시간 WebSocket Controller 및 모든 스레드 종료 완료")
    }
}
