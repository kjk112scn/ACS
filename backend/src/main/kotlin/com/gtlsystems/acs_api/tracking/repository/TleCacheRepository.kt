package com.gtlsystems.acs_api.tracking.repository

import com.gtlsystems.acs_api.tracking.entity.TleCacheEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * TLE 캐시 Repository
 * - ReactiveCrudRepository 기반 (PRIMARY KEY 있음)
 * - Ephemeris/PassSchedule TLE 관리
 */
@Repository
interface TleCacheRepository : ReactiveCrudRepository<TleCacheEntity, Long> {

    // ==================== 조회 ====================

    /**
     * 모드별 활성 TLE 조회
     */
    fun findByModeAndIsActiveTrue(mode: String): Flux<TleCacheEntity>

    /**
     * 위성 ID로 조회
     */
    fun findBySatelliteIdAndMode(satelliteId: String, mode: String): Flux<TleCacheEntity>

    /**
     * NORAD ID로 조회
     */
    fun findByNoradIdAndMode(noradId: Int, mode: String): Flux<TleCacheEntity>

    /**
     * 활성 TLE만 조회 (모드별)
     */
    @Query("SELECT * FROM tle_cache WHERE mode = :mode AND is_active = TRUE ORDER BY created_at DESC")
    fun findActiveTleByMode(mode: String): Flux<TleCacheEntity>

    /**
     * Ephemeris 활성 TLE 조회 (1개만)
     */
    @Query("SELECT * FROM tle_cache WHERE mode = 'EPHEMERIS' AND is_active = TRUE LIMIT 1")
    fun findActiveEphemerisTle(): Mono<TleCacheEntity>

    /**
     * PassSchedule 활성 TLE 목록 조회
     */
    @Query("SELECT * FROM tle_cache WHERE mode = 'PASS_SCHEDULE' AND is_active = TRUE ORDER BY satellite_name")
    fun findActivePassScheduleTles(): Flux<TleCacheEntity>

    /**
     * 최근 TLE 이력 조회 (비활성화 포함)
     */
    @Query("SELECT * FROM tle_cache WHERE mode = :mode ORDER BY created_at DESC LIMIT :limit")
    fun findRecentByMode(mode: String, limit: Int): Flux<TleCacheEntity>

    // ==================== 비활성화 (Soft Delete) ====================

    /**
     * 특정 TLE 비활성화
     */
    @Modifying
    @Query("UPDATE tle_cache SET is_active = FALSE, deactivated_at = NOW(), updated_at = NOW() WHERE id = :id")
    fun deactivateById(id: Long): Mono<Int>

    /**
     * 모드별 모든 활성 TLE 비활성화
     */
    @Modifying
    @Query("UPDATE tle_cache SET is_active = FALSE, deactivated_at = NOW(), updated_at = NOW() WHERE mode = :mode AND is_active = TRUE")
    fun deactivateAllByMode(mode: String): Mono<Int>

    /**
     * 위성 ID로 비활성화 (PassSchedule용)
     */
    @Modifying
    @Query("UPDATE tle_cache SET is_active = FALSE, deactivated_at = NOW(), updated_at = NOW() WHERE satellite_id = :satelliteId AND mode = :mode AND is_active = TRUE")
    fun deactivateBySatelliteIdAndMode(satelliteId: String, mode: String): Mono<Int>

    // ==================== 통계 ====================

    /**
     * 모드별 활성 TLE 개수
     */
    @Query("SELECT COUNT(*) FROM tle_cache WHERE mode = :mode AND is_active = TRUE")
    fun countActiveByMode(mode: String): Mono<Long>

    /**
     * 모드별 전체 TLE 개수 (이력 포함)
     */
    fun countByMode(mode: String): Mono<Long>
}
