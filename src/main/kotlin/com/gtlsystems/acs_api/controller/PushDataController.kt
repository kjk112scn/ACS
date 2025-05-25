package com.gtlsystems.acs_api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.service.PushDataService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import java.time.Duration

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
    private val messagesReceived = AtomicLong(0)
    private val messagesSent = AtomicLong(0)
    private val smartPingCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val connectionErrors = AtomicLong(0)

    // === 핑-퐁 전략 상수 ===
    companion object {
        const val REALTIME_DATA_INTERVAL_MS = 15L
        const val DATA_TIMEOUT_THRESHOLD_MS = 5000L // 5초간 데이터 없으면 핑 시작
        const val CLIENT_RESPONSE_TIMEOUT_MS = 30000L // 30초간 클라이언트 응답 없으면 문제
        const val BACKGROUND_PING_INTERVAL_MS = 30000L // 백그라운드 모드시 30초마다 핑
        const val HEARTBEAT_INTERVAL_MS = 30000L // 하트비트 간격
        const val STREAM_RETRY_COUNT = 3 // 스트림 재시도 횟수
        const val BACKPRESSURE_BUFFER_SIZE = 1000 // 백프레셔 버퍼 크기
    }


    // === 세션 정보 클래스 (스마트 핑 전략 포함) ===
    private data class SessionInfo(
        val sessionId: String,
        val connectedTime: Long = System.currentTimeMillis(),
        val messagesSent: AtomicLong = AtomicLong(0),
        val messagesReceived: AtomicLong = AtomicLong(0),
        val lastActivity: AtomicLong = AtomicLong(System.currentTimeMillis()),
        val lastDataReceived: AtomicLong = AtomicLong(System.currentTimeMillis()),
        val lastPingTime: AtomicLong = AtomicLong(0),
        val clientInfo: MutableMap<String, Any> = mutableMapOf(),
        var isActive: Boolean = true,

        // === 스마트 핑 전략 필드 ===
        var isClientBackground: Boolean = false,
        var dataStreamActive: Boolean = true,
        var lastClientResponse: AtomicLong = AtomicLong(System.currentTimeMillis()),
        var networkLatency: Long = -1,
        var streamErrorCount: AtomicInteger = AtomicInteger(0),
        var lastHeartbeat: AtomicLong = AtomicLong(System.currentTimeMillis())
    )

    override fun handle(session: WebSocketSession): Mono<Void> {
        val sessionId = session.id
        val connectionNumber = totalConnections.incrementAndGet()

        logger.info("🔗 새 WebSocket 연결 #{}: {} (스트림 안정성 강화 버전)", connectionNumber, sessionId)

        // 세션 정보 등록
        val sessionInfo = SessionInfo(sessionId)
        connectedSessions[sessionId] = sessionInfo
        activeConnections.incrementAndGet()

        // PushDataService에 클라이언트 연결 알림
        val initialData = try {
            pushDataService.clientConnected()
        } catch (e: Exception) {
            logger.warn("⚠️ 초기 데이터 가져오기 실패: {}", e.message)
            ""
        }

        // 초기 데이터 전송
        sendInitialDataSafely(session, initialData, sessionInfo)

        // === 1️⃣ 입력 스트림 (완전 안전화) ===
        val input = session.receive()
            .doOnNext { message ->
                try {
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                    logger.error("❌ 메시지 처리 중 예외: {} - {}", sessionId, e.message, e)
                }
            }
            .doOnError { error ->
                errorCount.incrementAndGet()
                sessionInfo.streamErrorCount.incrementAndGet()
                logger.warn("⚠️ 입력 스트림 오류 (연결 유지): {} - {}", sessionId, error.message)
            }
            .onErrorResume { error ->
                logger.info("🔄 입력 스트림 복구: {} - 빈 스트림으로 대체", sessionId)
                Flux.empty<WebSocketMessage>()
            }
            .then()
            .onErrorResume { error ->
                logger.info("🔄 입력 스트림 최종 복구: {} - {}", sessionId, error.message)
                Mono.empty()
            }

        // === 2️⃣ 실시간 데이터 스트림 (백프레셔 + 안정성 강화) ===
        val realtimeData = createRealtimeDataStream(session, sessionInfo)

        // === 4️⃣ 하트비트 스트림 (최후 연결 유지) ===
        val heartbeat = createHeartbeatStream(session, sessionInfo)

        // === 5️⃣ 출력 스트림 (완전 안전화) ===
        val output = session.send(
           // Flux.merge(realtimeData, smartPing, heartbeat)
            Flux.merge(realtimeData)
                .doOnNext { message ->
                    sessionInfo.lastActivity.set(System.currentTimeMillis())
                    //logger.debug("📤 메시지 전송: {} (타입: {})", sessionId, getMessageType(message))
                }
                .doOnError { error ->
                    errorCount.incrementAndGet()
                    sessionInfo.streamErrorCount.incrementAndGet()
                    logger.warn("⚠️ 병합 스트림 오류 (연결 유지): {} - {}", sessionId, error.message)
                }
                .onErrorResume { error ->
                    logger.info("🔄 병합 스트림 복구: {} - 연결 유지를 위해 빈 스트림 제공", sessionId)
                    Flux.empty<WebSocketMessage>()
                }
                .switchIfEmpty(
                    // 모든 스트림이 비어있을 때 응급 하트비트
                    Flux.interval(Duration.ofMillis(HEARTBEAT_INTERVAL_MS))
                        .map {
                            session.textMessage(createEmergencyHeartbeat())
                        }
                        .doOnNext {
                            logger.debug("🚨 응급 하트비트 전송: {}", sessionId)
                            sessionInfo.lastHeartbeat.set(System.currentTimeMillis())
                        }
                )
        )
            .doOnError { error ->
                errorCount.incrementAndGet()
                sessionInfo.streamErrorCount.incrementAndGet()
                logger.warn("⚠️ 출력 스트림 오류 (연결 유지 시도): {} - {}", sessionId, error.message)
            }
            .onErrorResume { error ->
                logger.info("🔄 출력 스트림 최종 복구: {} - 연결 유지", sessionId)
                Mono.empty()
            }

        // === 6️⃣ 연결 해제 처리 함수 ===
        fun handleDisconnection() {
            val removedSession = connectedSessions.remove(sessionId)
            if (removedSession != null) {
                removedSession.isActive = false
                activeConnections.decrementAndGet()

                try {
                    pushDataService.clientDisconnected()
                } catch (e: Exception) {
                    logger.warn("⚠️ 클라이언트 해제 알림 실패: {}", e.message)
                }

                val connectionDuration = System.currentTimeMillis() - removedSession.connectedTime
                val totalMessages = removedSession.messagesSent.get()
                val errorCount = removedSession.streamErrorCount.get()
                val avgLatency = if (removedSession.networkLatency > 0) "${removedSession.networkLatency}ms" else "측정안됨"

                logger.info(
                    "📊 세션 {} 해제 완료 - 지속: {}ms, 메시지: {}개, 오류: {}회, 지연: {}",
                    sessionId, connectionDuration, totalMessages, errorCount, avgLatency
                )
            }
        }

        // === 7️⃣ 연결 종료 감지 (클라이언트 주도적 종료만) ===
        val close = session.closeStatus()
            .doOnNext { status ->
                logger.info(
                    "🔌 클라이언트 {} 정상 종료: {} - {}",
                    sessionId, status.code, status.reason ?: "정상 종료"
                )
                handleDisconnection()
            }
            .doOnError { error ->
                logger.debug("🔍 종료 상태 감지 중 오류 (정상): {} - {}", sessionId, error.message)
            }
            .onErrorResume { error ->
                logger.debug("🔄 종료 상태 감지 복구: {} - {}", sessionId, error.message)
                Mono.empty()
            }
            .then()

        // === 8️⃣ 최종 스트림 결합 (완전 안전화) ===
        return Mono.zip(input, output, close)
            .doOnSubscribe {
                logger.info("📡 WebSocket 세션 {} 안정화된 스트림 시작", sessionId)
            }
            .doOnTerminate {
                logger.info("🔚 WebSocket 세션 {} 정상 종료", sessionId)
                // handleDisconnection()은 close에서 이미 처리됨
            }
            .doOnError { error ->
                connectionErrors.incrementAndGet()
                logger.error("❌ WebSocket 세션 {} 예외적 오류: {}", sessionId, error.message, error)
                handleDisconnection() // 예외적 상황에서만 강제 해제
            }
            .onErrorResume { error ->
                logger.warn("🔄 WebSocket 세션 {} 최종 복구 완료", sessionId)
                Mono.empty() // 최종 안전망
            }
            .then()
    }

    /**
     * ✅ 실시간 데이터 스트림 생성 (안정성 강화)
     */
    private fun createRealtimeDataStream(session: WebSocketSession, sessionInfo: SessionInfo): Flux<WebSocketMessage> {
        return try {
            pushDataService.getReadStatusDataStream()
                .onBackpressureBuffer(BACKPRESSURE_BUFFER_SIZE) // 백프레셔 버퍼
                .publishOn(Schedulers.parallel())
                .doOnNext { message ->
                    sessionInfo.messagesSent.incrementAndGet()
                    sessionInfo.lastActivity.set(System.currentTimeMillis())
                    sessionInfo.lastDataReceived.set(System.currentTimeMillis())
                    sessionInfo.dataStreamActive = true
                    messagesSent.incrementAndGet()

                    logger.debug("📤 실시간 데이터: {} ({}자)", session.id, message.length)
                }
                .map { message -> session.textMessage(message) }
                .doOnError { error ->
                    errorCount.incrementAndGet()
                    sessionInfo.streamErrorCount.incrementAndGet()
                    sessionInfo.dataStreamActive = false
                    logger.warn("⚠️ 실시간 데이터 스트림 오류: {} - {}", session.id, error.message)
                }
                .onErrorResume { error ->
                    logger.info("🔄 실시간 데이터 스트림 복구: {} - 빈 스트림으로 대체", session.id)
                    sessionInfo.dataStreamActive = false
                    Flux.empty<WebSocketMessage>()
                }
                .retry(STREAM_RETRY_COUNT.toLong()) // 재시도
                .doOnComplete {
                    logger.info("🔚 실시간 데이터 스트림 완료: {}", session.id)
                    sessionInfo.dataStreamActive = false
                }
        } catch (e: Exception) {
            logger.error("❌ 실시간 데이터 스트림 생성 실패: {} - {}", session.id, e.message, e)
            sessionInfo.dataStreamActive = false
            Flux.empty()
        }
    }

    /**
     * ✅ 하트비트 스트림 생성 (최후 연결 유지)
     */
    private fun createHeartbeatStream(session: WebSocketSession, sessionInfo: SessionInfo): Flux<WebSocketMessage> {
        return Flux.interval(Duration.ofMillis(HEARTBEAT_INTERVAL_MS))
            .filter { isHeartbeatNeeded(sessionInfo) }
            .map {
                session.textMessage(createHeartbeatMessage(sessionInfo))
            }
            .doOnNext {
                sessionInfo.messagesSent.incrementAndGet()
                sessionInfo.lastHeartbeat.set(System.currentTimeMillis())
                messagesSent.incrementAndGet()

                logger.debug("💓 하트비트 전송: {} (데이터 활성: {})", session.id, sessionInfo.dataStreamActive)
            }
            .doOnError { error ->
                logger.warn("⚠️ 하트비트 스트림 오류: {} - {}", session.id, error.message)
            }
            .onErrorResume { error ->
                logger.info("🔄 하트비트 스트림 복구: {} - 빈 스트림으로 대체", session.id)
                Flux.empty<WebSocketMessage>()
            }
    }

    /**
     * ✅ 하트비트 필요성 판단
     */
    private fun isHeartbeatNeeded(sessionInfo: SessionInfo): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastData = currentTime - sessionInfo.lastDataReceived.get()
        val timeSinceLastHeartbeat = currentTime - sessionInfo.lastHeartbeat.get()

        // 데이터 스트림이 비활성이고 마지막 하트비트로부터 충분한 시간이 지났을 때
        return !sessionInfo.dataStreamActive &&
                timeSinceLastData > DATA_TIMEOUT_THRESHOLD_MS &&
                timeSinceLastHeartbeat > HEARTBEAT_INTERVAL_MS
    }
    /**
     * ✅ 하트비트 메시지 생성
     */
    private fun createHeartbeatMessage(sessionInfo: SessionInfo): String {
        val currentTime = System.currentTimeMillis()
        val heartbeatData = mapOf(
            "type" to "heartbeat",
            "timestamp" to currentTime,
            "sessionId" to sessionInfo.sessionId,
            "connectionDuration" to (currentTime - sessionInfo.connectedTime),
            "dataStreamActive" to sessionInfo.dataStreamActive,
            "messagesSent" to sessionInfo.messagesSent.get(),
            "purpose" to "CONNECTION_MAINTENANCE"
        )

        return try {
            objectMapper.writeValueAsString(heartbeatData)
        } catch (e: Exception) {
            logger.error("❌ 하트비트 메시지 생성 실패: {}", e.message, e)
            """{"type":"heartbeat","timestamp":${currentTime},"error":"serialization_failed"}"""
        }
    }

    /**
     * ✅ 응급 하트비트 생성 (모든 스트림 실패 시)
     */
    private fun createEmergencyHeartbeat(): String {
        val currentTime = System.currentTimeMillis()
        return """{"type":"emergencyHeartbeat","timestamp":${currentTime},"purpose":"STREAM_RECOVERY"}"""
    }

    /**
     * ✅ 메시지 타입 추출 (디버깅용)
     */
    private fun getMessageType(message: WebSocketMessage): String {
        return try {
            val payload = message.payloadAsText
            val jsonNode = objectMapper.readTree(payload)
            jsonNode.get("type")?.asText() ?: "unknown"
        } catch (e: Exception) {
            "data"
        }
    }

    /**
     * ✅ 클라이언트 정보 처리
     */
    private fun handleClientInfo(jsonNode: com.fasterxml.jackson.databind.JsonNode, sessionInfo: SessionInfo) {
        try {
            jsonNode.fields().forEach { (key, value) ->
                when (key) {
                    "userAgent" -> sessionInfo.clientInfo["userAgent"] = value.asText()
                    "browserType" -> sessionInfo.clientInfo["browserType"] = value.asText()
                    "screenResolution" -> sessionInfo.clientInfo["screenResolution"] = value.asText()
                    "timezone" -> sessionInfo.clientInfo["timezone"] = value.asText()
                    "language" -> sessionInfo.clientInfo["language"] = value.asText()
                    else -> sessionInfo.clientInfo[key] = value.asText()
                }
            }

            logger.info("📋 클라이언트 정보 업데이트: {} - {}", sessionInfo.sessionId, sessionInfo.clientInfo)

        } catch (e: Exception) {
            logger.error("❌ 클라이언트 정보 처리 오류: {} - {}", sessionInfo.sessionId, e.message, e)
        }
    }

    /**
     * ✅ 초기 데이터 안전 전송
     */
    private fun sendInitialDataSafely(session: WebSocketSession, initialData: String, sessionInfo: SessionInfo) {
        if (initialData.isNotEmpty()) {
            try {
                session.send(Mono.just(session.textMessage(initialData)))
                    .subscribe(
                        {
                            sessionInfo.messagesSent.incrementAndGet()
                            sessionInfo.lastDataReceived.set(System.currentTimeMillis())
                            messagesSent.incrementAndGet()
                            logger.debug("📤 초기 데이터 전송 성공: {} ({}자)", session.id, initialData.length)
                        },
                        { error ->
                            errorCount.incrementAndGet()
                            logger.warn("⚠️ 초기 데이터 전송 실패 {}: {}", session.id, error.message)
                        }
                    )
            } catch (e: Exception) {
                errorCount.incrementAndGet()
                logger.error("❌ 초기 데이터 전송 중 예외 {}: {}", session.id, e.message, e)
            }
        }
    }

    // === 📊 성능 모니터링 및 상태 확인 메서드들 ===

    /**
     * ✅ 전체 연결 통계 반환
     */
    fun getConnectionStats(): Map<String, Any> {
        val currentTime = System.currentTimeMillis()
        val activeSessions = connectedSessions.values.filter { it.isActive }

        return mapOf(
            "totalConnections" to totalConnections.get(),
            "activeConnections" to activeConnections.get(),
            "messagesReceived" to messagesReceived.get(),
            "messagesSent" to messagesSent.get(),
            "smartPingCount" to smartPingCount.get(),
            "errorCount" to errorCount.get(),
            "connectionErrors" to connectionErrors.get(),
            "averageConnectionDuration" to calculateAverageConnectionDuration(activeSessions, currentTime),
            "backgroundSessions" to activeSessions.count { it.isClientBackground },
            "dataStreamActiveSessions" to activeSessions.count { it.dataStreamActive },
            "averageLatency" to calculateAverageLatency(activeSessions),
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
     * ✅ 평균 네트워크 지연 계산
     */
    private fun calculateAverageLatency(sessions: List<SessionInfo>): String {
        val validLatencies = sessions.mapNotNull {
            if (it.networkLatency > 0) it.networkLatency else null
        }

        return if (validLatencies.isNotEmpty()) {
            "${validLatencies.average().toLong()}ms"
        } else {
            "측정안됨"
        }
    }

    /**
     * ✅ 특정 세션 상세 정보 반환
     */
    fun getSessionDetails(sessionId: String): Map<String, Any>? {
        val sessionInfo = connectedSessions[sessionId] ?: return null
        val currentTime = System.currentTimeMillis()

        return mapOf(
            "sessionId" to sessionInfo.sessionId,
            "connectedTime" to sessionInfo.connectedTime,
            "connectionDuration" to (currentTime - sessionInfo.connectedTime),
            "isActive" to sessionInfo.isActive,
            "messagesSent" to sessionInfo.messagesSent.get(),
            "messagesReceived" to sessionInfo.messagesReceived.get(),
            "lastActivity" to sessionInfo.lastActivity.get(),
            "timeSinceLastActivity" to (currentTime - sessionInfo.lastActivity.get()),
            "lastDataReceived" to sessionInfo.lastDataReceived.get(),
            "timeSinceLastData" to (currentTime - sessionInfo.lastDataReceived.get()),
            "lastPingTime" to sessionInfo.lastPingTime.get(),
            "timeSinceLastPing" to (currentTime - sessionInfo.lastPingTime.get()),
            "isClientBackground" to sessionInfo.isClientBackground,
            "dataStreamActive" to sessionInfo.dataStreamActive,
            "lastClientResponse" to sessionInfo.lastClientResponse.get(),
            "timeSinceLastResponse" to (currentTime - sessionInfo.lastClientResponse.get()),
            "networkLatency" to if (sessionInfo.networkLatency > 0) "${sessionInfo.networkLatency}ms" else "측정안됨",
            "streamErrorCount" to sessionInfo.streamErrorCount.get(),
            "lastHeartbeat" to sessionInfo.lastHeartbeat.get(),
            "timeSinceLastHeartbeat" to (currentTime - sessionInfo.lastHeartbeat.get()),
            "clientInfo" to sessionInfo.clientInfo,
            "isHeartbeatNeeded" to isHeartbeatNeeded(sessionInfo)
        )
    }

    /**
     * ✅ 모든 활성 세션 목록 반환
     */
    fun getActiveSessions(): List<Map<String, Any>> {
        return connectedSessions.values
            .filter { it.isActive }
            .map { sessionInfo ->
                val currentTime = System.currentTimeMillis()
                mapOf(
                    "sessionId" to sessionInfo.sessionId,
                    "connectionDuration" to (currentTime - sessionInfo.connectedTime),
                    "messagesSent" to sessionInfo.messagesSent.get(),
                    "messagesReceived" to sessionInfo.messagesReceived.get(),
                    "isBackground" to sessionInfo.isClientBackground,
                    "dataStreamActive" to sessionInfo.dataStreamActive,
                    "networkLatency" to if (sessionInfo.networkLatency > 0) "${sessionInfo.networkLatency}ms" else "측정안됨",
                    "timeSinceLastActivity" to (currentTime - sessionInfo.lastActivity.get()),
                    "streamErrorCount" to sessionInfo.streamErrorCount.get()
                )
            }
    }

    /**
     * ✅ 연결 상태 요약 반환
     */
    fun getConnectionSummary(): String {
        val stats = getConnectionStats()
        val activeSessions = getActiveSessions()

        return buildString {
            appendLine("=== WebSocket 연결 상태 요약 ===")
            appendLine("📊 전체 연결: ${stats["totalConnections"]}회")
            appendLine("🔗 활성 연결: ${stats["activeConnections"]}개")
            appendLine("📤 송신 메시지: ${stats["messagesSent"]}개")
            appendLine("📥 수신 메시지: ${stats["messagesReceived"]}개")
            appendLine("🧠 스마트 핑: ${stats["smartPingCount"]}회")
            appendLine("❌ 오류 발생: ${stats["errorCount"]}회")
            appendLine("🏥 건강한 세션: ${stats["healthySessions"]}개")
            appendLine("📱 백그라운드 세션: ${stats["backgroundSessions"]}개")
            appendLine("📊 데이터 활성 세션: ${stats["dataStreamActiveSessions"]}개")
            appendLine("⏱️ 평균 지연: ${stats["averageLatency"]}")
            appendLine("🔧 핑 전략 분포: ${stats["pingStrategies"]}")

            if (activeSessions.isNotEmpty()) {
                appendLine("\n=== 활성 세션 상세 ===")
                activeSessions.forEachIndexed { index, session ->
                    appendLine("${index + 1}. ${session["sessionId"]} - ${session["connectionHealth"]} (${session["networkLatency"]})")
                }
            }
        }
    }

    /**
     * ✅ 비활성 세션 정리
     */
    fun cleanupInactiveSessions(): Int {
        val currentTime = System.currentTimeMillis()
        var cleanedCount = 0

        val inactiveSessions = connectedSessions.values.filter { sessionInfo ->
            !sessionInfo.isActive ||
                    (currentTime - sessionInfo.lastActivity.get()) > (CLIENT_RESPONSE_TIMEOUT_MS * 2) // 1분 이상 비활성
        }

        inactiveSessions.forEach { sessionInfo ->
            connectedSessions.remove(sessionInfo.sessionId)
            cleanedCount++

            val inactiveDuration = currentTime - sessionInfo.lastActivity.get()
            logger.info("🧹 비활성 세션 정리: {} (비활성 시간: {}ms)", sessionInfo.sessionId, inactiveDuration)
        }

        if (cleanedCount > 0) {
            logger.info("🧹 총 {}개 비활성 세션 정리 완료", cleanedCount)
        }

        return cleanedCount
    }

    /**
     * ✅ 서비스 상태 체크
     */
    /**
     * ✅ 서비스 상태 체크 (완전 수정 버전)
     */
    fun isServiceHealthy(): Boolean {
        return try {
            val stats = getConnectionStats()

            // 안전한 타입 변환
            val messagesSentValue = when (val value = stats["messagesSent"]) {
                is AtomicLong -> value.get()
                is Long -> value
                is Number -> value.toLong()
                else -> 0L
            }

            val errorCountValue = when (val value = stats["errorCount"]) {
                is AtomicLong -> value.get()
                is Long -> value
                is Number -> value.toLong()
                else -> 0L
            }

            val activeConnectionsValue = when (val value = stats["activeConnections"]) {
                is AtomicInteger -> value.get()
                is Int -> value
                is Number -> value.toInt()
                else -> 0
            }

            val connectionErrorsValue = when (val value = stats["connectionErrors"]) {
                is AtomicLong -> value.get()
                is Long -> value
                is Number -> value.toLong()
                else -> 0L
            }

            // 오류율 계산
            val errorRate = if (messagesSentValue > 0) {
                errorCountValue.toDouble() / messagesSentValue.toDouble()
            } else {
                0.0
            }

            // 건강 상태 판단
            val isHealthy = errorRate < 0.1 && // 오류율 10% 미만
                    activeConnectionsValue >= 0 && // 활성 연결 존재 (0개도 정상)
                    connectionErrorsValue < 100 // 연결 오류 100회 미만

            logger.debug("🏥 서비스 건강 상태: {} (오류율: {:.2f}%, 활성연결: {}, 연결오류: {})",
                if (isHealthy) "건강" else "문제있음", errorRate * 100, activeConnectionsValue, connectionErrorsValue)

            isHealthy

        } catch (e: Exception) {
            logger.error("❌ 서비스 상태 체크 오류: {}", e.message, e)
            false // 오류 발생 시 비건강 상태로 간주
        }
    }

    /**
     * ✅ 디버그 정보 반환 (완전 수정 버전)
     */
    fun getDebugInfo(): Map<String, Any> {
        return try {
            mapOf(
                "className" to (this::class.simpleName ?: "PushDataController"),
                "realtimeDataInterval" to "${REALTIME_DATA_INTERVAL_MS}ms",
                "dataTimeoutThreshold" to "${DATA_TIMEOUT_THRESHOLD_MS}ms",
                "clientResponseTimeout" to "${CLIENT_RESPONSE_TIMEOUT_MS}ms",
                "backgroundPingInterval" to "${BACKGROUND_PING_INTERVAL_MS}ms",
                "heartbeatInterval" to "${HEARTBEAT_INTERVAL_MS}ms",
                "streamRetryCount" to STREAM_RETRY_COUNT,
                "backpressureBufferSize" to BACKPRESSURE_BUFFER_SIZE,
                "serviceHealthy" to isServiceHealthy(),
                "connectionStats" to getConnectionStats(),
                "jvmMemory" to mapOf(
                    "totalMemory" to Runtime.getRuntime().totalMemory(),
                    "freeMemory" to Runtime.getRuntime().freeMemory(),
                    "maxMemory" to Runtime.getRuntime().maxMemory(),
                    "usedMemory" to (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                ),
                "systemInfo" to mapOf(
                    "availableProcessors" to Runtime.getRuntime().availableProcessors(),
                    "javaVersion" to System.getProperty("java.version"),
                    "osName" to System.getProperty("os.name"),
                    "currentTime" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("❌ 디버그 정보 생성 오류: {}", e.message, e)
            mapOf(
                "error" to "디버그 정보 생성 실패",
                "errorMessage" to (e.message ?: "알 수 없는 오류"),
                "timestamp" to System.currentTimeMillis()
            )
        }
    }
}
