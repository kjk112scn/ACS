package com.gtlsystems.acs_api.controller

import com.gtlsystems.acs_api.service.PushReadStatusService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class PushReadStatusController (private val pushReadstatusService: PushReadStatusService) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        println("New client connected to WebSocket.")
        return session.send(
            pushReadstatusService.getReadStatusDataStream()
                .map { message -> session.textMessage(message) }
        ).and(session.receive().then())
    }
    }