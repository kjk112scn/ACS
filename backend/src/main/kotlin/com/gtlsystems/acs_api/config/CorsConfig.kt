package com.gtlsystems.acs_api.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    private val logger = LoggerFactory.getLogger(CorsConfig::class.java)

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        logger.info("🚀 WebFlux CORS 설정 초기화 중...")

        val corsConfig = CorsConfiguration().apply {
            // 허용할 Origin 설정
            allowedOriginPatterns = listOf(
                "http://localhost:9000",
                "http://127.0.0.1:9000",
                "http://localhost:*",
                "http://127.0.0.1:*",
                "*"
            )

            // 허용할 HTTP 메서드
            allowedMethods = listOf(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
            )

            // 허용할 헤더
            allowedHeaders = listOf("*")

            // 인증 정보 허용
            allowCredentials = true

            // Preflight 요청 캐시 시간 (초)
            maxAge = 3600L

            // 응답에서 노출할 헤더
            exposedHeaders = listOf(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Access-Control-Allow-Methods",
                "Access-Control-Allow-Headers"
            )
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", corsConfig)
        }

        logger.info("✅ WebFlux CORS 설정 완료!")
        return CorsWebFilter(source)
    }
}