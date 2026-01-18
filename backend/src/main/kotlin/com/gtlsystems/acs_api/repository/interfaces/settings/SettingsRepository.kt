package com.gtlsystems.acs_api.repository.interfaces.settings

import com.gtlsystems.acs_api.settings.entity.Setting
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 설정 Repository (R2DBC)
 * - ReactiveCrudRepository 기반 (PRIMARY KEY 있음)
 */
@Repository
interface SettingsRepository : ReactiveCrudRepository<Setting, Long> {

    /**
     * 설정 키로 설정 조회
     */
    fun findByKey(key: String): Mono<Setting>

    /**
     * 시스템 설정 여부로 조회
     */
    fun findByIsSystemSetting(isSystemSetting: Boolean): Flux<Setting>

    /**
     * 설정 키가 존재하는지 확인
     */
    fun existsByKey(key: String): Mono<Boolean>

    /**
     * 설정 키 목록으로 조회
     */
    fun findByKeyIn(keys: List<String>): Flux<Setting>

    /**
     * 설정 키로 삭제
     */
    fun deleteByKey(key: String): Mono<Void>

    /**
     * 모든 설정 조회 (정렬)
     */
    @Query("SELECT * FROM settings ORDER BY key")
    fun findAllOrderByKey(): Flux<Setting>
}
