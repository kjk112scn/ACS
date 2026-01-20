package com.gtlsystems.acs_api.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

/**
 * Flyway 마이그레이션 설정
 *
 * WebFlux 환경에서는 DataSource가 자동 구성되지 않으므로
 * Flyway를 명시적으로 설정하고 실행해야 함
 *
 * with-db 프로필에서만 활성화 (spring.flyway.enabled=true)
 */
@Configuration
@ConditionalOnProperty(
    prefix = "spring.flyway",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class FlywayConfig(
    @Value("\${spring.flyway.url}") private val url: String,
    @Value("\${spring.flyway.user}") private val user: String,
    @Value("\${spring.flyway.password}") private val password: String,
    @Value("\${spring.flyway.locations:classpath:db/migration}") private val locations: String,
    @Value("\${spring.flyway.baseline-on-migrate:true}") private val baselineOnMigrate: Boolean
) {
    private val logger = LoggerFactory.getLogger(FlywayConfig::class.java)

    @PostConstruct
    fun migrate() {
        logger.info("=".repeat(60))
        logger.info("Flyway 마이그레이션 시작")
        logger.info("  URL: $url")
        logger.info("  User: $user")
        logger.info("  Locations: $locations")
        logger.info("  Baseline on Migrate: $baselineOnMigrate")
        logger.info("=".repeat(60))

        try {
            val flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .load()

            val result = flyway.migrate()

            logger.info("Flyway 마이그레이션 완료")
            logger.info("  실행된 마이그레이션: ${result.migrationsExecuted}")
            logger.info("  현재 버전: ${result.targetSchemaVersion}")
            if (result.warnings.isNotEmpty()) {
                result.warnings.forEach { logger.warn("  경고: $it") }
            }
            logger.info("=".repeat(60))
        } catch (e: Exception) {
            logger.error("Flyway 마이그레이션 실패: ${e.message}", e)
            throw e
        }
    }
}
