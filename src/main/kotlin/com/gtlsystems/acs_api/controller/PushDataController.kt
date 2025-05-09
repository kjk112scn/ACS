package com.gtlsystems.acs_api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.service.PushService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Component
class PushDataController(
    private val pushService: PushService,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(PushDataController::class.java)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val sessionId = session.id
        logger.info("새 WebSocket 클라이언트 연결: $sessionId")

        // 서비스에 시뮬레이션 시작 알림
        pushService.startSimulation()

        // 핑-퐁 처리를 위한 입력 스트림
        val input = session.receive()
            .doOnNext { message ->
                try {
                    if (message.type == WebSocketMessage.Type.TEXT) {
                        val payload = message.payloadAsText
                        val jsonNode = objectMapper.readTree(payload)

                        // 핑 메시지 처리
                        if (jsonNode.has("type") && jsonNode.get("type").asText() == "ping") {
                            val timestamp = if (jsonNode.has("timestamp"))
                                jsonNode.get("timestamp").asLong() else System.currentTimeMillis()

                            // 퐁 응답 생성
                            val pongMessage = mapOf(
                                "type" to "pong",
                                "timestamp" to timestamp,
                                "serverTime" to System.currentTimeMillis()
                            )

                            // 비동기로 퐁 응답 전송
                            session.send(Mono.just(session.textMessage(
                                objectMapper.writeValueAsString(pongMessage)
                            ))).subscribe()
                        }
                    }
                } catch (e: Exception) {
                    logger.error("메시지 처리 오류: ${e.message}", e)
                }
            }
            .then()

        // 클라이언트로 데이터 전송
        val output = session.send(
            pushService.getReadStatusDataStream()
                .publishOn(Schedulers.parallel())  // 병렬 처리
                .map { message ->
                    session.textMessage(message)
                }
                .doOnError { error ->
                    logger.error("클라이언트 $sessionId 로 메시지 전송 중 오류: ${error.message}", error)
                }
                .onErrorResume { error ->
                    // 오류 발생 시 빈 스트림으로 대체하여 연결 유지
                    logger.error("데이터 스트림 오류 복구: ${error.message}")
                    Flux.empty()
                }
        )

        // 연결 종료 시 로깅
        val close = session.closeStatus()
            .doOnNext { status ->
                logger.info("클라이언트 $sessionId 연결 종료: ${status.code} - ${status.reason ?: "이유 없음"}")
            }
            .then()

        // 입력과 출력 스트림 결합
        return Mono.zip(input, output, close).then()
    }
}