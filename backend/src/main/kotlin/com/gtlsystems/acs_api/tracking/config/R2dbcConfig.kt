package com.gtlsystems.acs_api.tracking.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * R2DBC 설정
 * - home/office 프로필에서만 활성화
 * - Repository 스캔 설정
 */
@Configuration
@EnableR2dbcRepositories(basePackages = ["com.gtlsystems.acs_api.tracking.repository"])
@EnableR2dbcAuditing
@EnableScheduling
@ConditionalOnProperty(
    prefix = "spring.r2dbc",
    name = ["url"],
    matchIfMissing = false
)
class R2dbcConfig {

    /**
     * DatabaseClient Bean
     * - Hypertable Repository에서 사용
     */
    @Bean
    fun databaseClient(connectionFactory: ConnectionFactory): DatabaseClient {
        return DatabaseClient.create(connectionFactory)
    }
}
