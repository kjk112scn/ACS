package com.gtlsystems.acs_api.config

import com.gtlsystems.acs_api.controller.PushReadStatusController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler

@Configuration
class WebSocketConfig(private val pushReadStatusController: PushReadStatusController) {

    @Bean
    fun webSocketHandlerMapping(): SimpleUrlHandlerMapping {
        val map = HashMap<String, WebSocketHandler>()
        map["/ws/sensor-data"] = pushReadStatusController

        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.urlMap = map
        handlerMapping.order = 10
        return handlerMapping
    }
}