package com.gtlsystems.acs_api.config

import com.gtlsystems.acs_api.controller.PushDataController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy

@Configuration
class WebSocketConfig(private val pushDataController: PushDataController) : WebFluxConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("*")  // 모든 오리진 허용 (프로덕션에서는 제한 필요)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600)  // 1시간 캐싱
    }

    @Bean
    fun webSocketHandlerMapping(): HandlerMapping {
        val map = mapOf(
            "/ws/push-data" to pushDataController
        )

        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.urlMap = map
        handlerMapping.order = -1  // 높은 우선순위
        return handlerMapping
    }

    @Bean
    fun handlerAdapter(webSocketService: WebSocketService): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter(webSocketService)
    }

    @Bean
    fun webSocketService(): WebSocketService {
        // Reactor Netty 기반 WebSocket 서비스 설정
        val strategy = ReactorNettyRequestUpgradeStrategy()
        strategy.maxFramePayloadLength = 1024 * 1024  // 1MB로 프레임 크기 제한
        return HandshakeWebSocketService(strategy)
    }
}