package com.gtlsystems.acs_api.config

import com.gtlsystems.acs_api.controller.system.PerformanceController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class PerformanceFilter : WebFilter {

    @Autowired
    private lateinit var performanceController: PerformanceController

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val startTime = Instant.now()
        val path = exchange.request.path.toString()

        return chain.filter(exchange)
            .doFinally {
                val responseTime = Instant.now().toEpochMilli() - startTime.toEpochMilli()
                val statusCode = exchange.response.statusCode?.value() ?: 0
                val isError = statusCode >= 400

                // 성능 메트릭 기록
                performanceController.recordApiCall(path, responseTime, isError)
            }
    }
} 