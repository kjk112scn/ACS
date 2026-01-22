package com.gtlsystems.acs_api.tracking.repository

import com.gtlsystems.acs_api.tracking.entity.TrackingSessionEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * 추적 세션 Repository
 * - ReactiveCrudRepository 기반 (PRIMARY KEY 있음)
 * - V006: UNIQUE 제약이 (mst_id, detail_id, tracking_mode)로 변경됨
 */
@Repository
interface TrackingSessionRepository : ReactiveCrudRepository<TrackingSessionEntity, Long> {

    /**
     * 위성 ID로 세션 조회
     */
    fun findBySatelliteId(satelliteId: String): Flux<TrackingSessionEntity>

    /**
     * 추적 모드로 세션 조회
     */
    fun findByTrackingMode(trackingMode: String): Flux<TrackingSessionEntity>

    /**
     * MST ID와 데이터 타입으로 조회 (V006 이전 호환용)
     */
    fun findByMstIdAndDataType(mstId: Long, dataType: String): Flux<TrackingSessionEntity>

    /**
     * V006: MST ID, Detail ID, Tracking Mode로 조회 (UNIQUE 키)
     * 1 Pass = 1 Session을 보장하기 위한 조회
     */
    fun findByMstIdAndDetailIdAndTrackingMode(
        mstId: Long,
        detailId: Int,
        trackingMode: String
    ): Mono<TrackingSessionEntity>

    /**
     * V006: Detail ID로 조회
     */
    fun findByDetailId(detailId: Int): Flux<TrackingSessionEntity>

    /**
     * 시간 범위로 조회
     */
    @Query("SELECT * FROM tracking_session WHERE start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC")
    fun findByTimeRange(startTime: OffsetDateTime, endTime: OffsetDateTime): Flux<TrackingSessionEntity>

    /**
     * 최근 N개 세션 조회
     */
    @Query("SELECT * FROM tracking_session ORDER BY start_time DESC LIMIT :limit")
    fun findRecentSessions(limit: Int): Flux<TrackingSessionEntity>

    /**
     * 오래된 세션 삭제 (365일 이전)
     */
    @Query("DELETE FROM tracking_session WHERE created_at < :cutoffDate")
    fun deleteOlderThan(cutoffDate: OffsetDateTime): Mono<Long>
}
