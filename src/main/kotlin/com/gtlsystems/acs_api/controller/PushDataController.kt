package com.gtlsystems.acs_api.controller

import com.gtlsystems.acs_api.service.PushService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class PushDataController(private val pushReadStatusService: PushService) : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(PushDataController::class.java)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val sessionId = session.id
        logger.info("New client connected to WebSocket. Session ID: $sessionId")

        // 서비스에 시뮬레이션 시작 알림 (필요한 경우)
        pushReadStatusService.startSimulation()

        // 클라이언트로 데이터 전송
        val output = session.send(
            pushReadStatusService.getReadStatusDataStream()
                .map { message ->
                    logger.debug("Sending message to client $sessionId: ${message.take(50)}...")
                    session.textMessage(message)
                }
                .doOnError { error ->
                    logger.error("Error while sending message to client $sessionId: ${error.message}", error)
                }
                .onErrorResume { error ->
                    // 에러 발생 시 빈 스트림으로 대체하여 연결 유지
                    logger.error("Resuming from error for client $sessionId: ${error.message}")
                    Mono.empty()
                }
        )

        // 클라이언트로부터 메시지 수신 (필요한 경우 처리 추가)
        val input = session.receive()
            .doOnNext { message ->
                val payload = message.payloadAsText
                logger.debug("Received message from client $sessionId: $payload")
                // 필요한 경우 수신된 메시지 처리
            }
            .doOnComplete {
                logger.info("Client $sessionId input stream completed")
            }
            .then()

        // 연결 종료 시 처리
        return output.and(input)
            .doOnSubscribe {
                logger.info("WebSocket connection established with client $sessionId")
            }
            .doOnTerminate {
                logger.info("WebSocket connection terminated with client $sessionId")
            }
            .doOnError { error ->
                logger.error("WebSocket error with client $sessionId: ${error.message}", error)
            }
    }

}
