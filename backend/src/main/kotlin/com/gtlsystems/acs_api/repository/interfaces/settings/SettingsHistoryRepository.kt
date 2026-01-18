package com.gtlsystems.acs_api.repository.interfaces.settings

import com.gtlsystems.acs_api.settings.entity.SettingHistory
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.OffsetDateTime

/**
 * 설정 변경 이력 Repository (R2DBC)
 * - 감사 로그 저장/조회
 */
@Repository
interface SettingsHistoryRepository : ReactiveCrudRepository<SettingHistory, Long> {

    /**
     * 설정 키로 이력 조회
     */
    fun findBySettingKey(settingKey: String): Flux<SettingHistory>

    /**
     * 변경자로 이력 조회
     */
    fun findByChangedBy(changedBy: String): Flux<SettingHistory>

    /**
     * 시간 범위로 이력 조회
     */
    @Query("SELECT * FROM setting_history WHERE created_at >= :start AND created_at <= :end ORDER BY created_at DESC")
    fun findByCreatedAtBetween(start: OffsetDateTime, end: OffsetDateTime): Flux<SettingHistory>

    /**
     * 최근 이력 조회
     */
    @Query("SELECT * FROM setting_history ORDER BY created_at DESC LIMIT :limit")
    fun findRecentHistory(limit: Int): Flux<SettingHistory>

    /**
     * 특정 설정의 최근 이력 조회
     */
    @Query("SELECT * FROM setting_history WHERE setting_key = :key ORDER BY created_at DESC LIMIT :limit")
    fun findRecentBySettingKey(key: String, limit: Int): Flux<SettingHistory>
}
