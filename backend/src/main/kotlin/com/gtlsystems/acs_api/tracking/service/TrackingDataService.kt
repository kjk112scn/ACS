package com.gtlsystems.acs_api.tracking.service

import com.gtlsystems.acs_api.tracking.entity.IcdStatusEntity
import com.gtlsystems.acs_api.tracking.entity.TrackingResultEntity
import com.gtlsystems.acs_api.tracking.entity.TrackingSessionEntity
import com.gtlsystems.acs_api.tracking.entity.TrackingTrajectoryEntity
import com.gtlsystems.acs_api.tracking.repository.IcdStatusRepository
import com.gtlsystems.acs_api.tracking.repository.TrackingResultRepository
import com.gtlsystems.acs_api.tracking.repository.TrackingSessionRepository
import com.gtlsystems.acs_api.tracking.repository.TrackingTrajectoryRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 추적 데이터 저장/조회 서비스
 * - 세션, 궤적, 결과 데이터 DB 저장
 * - 오래된 데이터 자동 삭제 (365일)
 */
@Service
@ConditionalOnProperty(
    prefix = "spring.r2dbc",
    name = ["url"],
    matchIfMissing = false
)
class TrackingDataService(
    private val sessionRepository: TrackingSessionRepository,
    private val trajectoryRepository: TrackingTrajectoryRepository,
    private val resultRepository: TrackingResultRepository,
    private val icdStatusRepository: IcdStatusRepository
) {
    private val logger = LoggerFactory.getLogger(TrackingDataService::class.java)

    @Value("\${acs.database.retention-days:365}")
    private var retentionDays: Long = 365

    // ==================== Session ====================

    /**
     * 세션 저장
     */
    fun saveSession(session: TrackingSessionEntity): Mono<TrackingSessionEntity> {
        return sessionRepository.save(session)
            .doOnSuccess { logger.info("세션 저장 완료: mstId={}, mode={}", it.mstId, it.trackingMode) }
            .doOnError { logger.error("세션 저장 실패: {}", it.message) }
    }

    /**
     * 세션 ID로 조회
     */
    fun findSessionById(id: Long): Mono<TrackingSessionEntity> {
        return sessionRepository.findById(id)
    }

    /**
     * 위성 ID로 세션 조회
     */
    fun findSessionsBySatellite(satelliteId: String): Flux<TrackingSessionEntity> {
        return sessionRepository.findBySatelliteId(satelliteId)
    }

    /**
     * 최근 세션 조회
     */
    fun findRecentSessions(limit: Int = 10): Flux<TrackingSessionEntity> {
        return sessionRepository.findRecentSessions(limit)
    }

    /**
     * 시간 범위로 세션 조회
     */
    fun findSessionsByTimeRange(start: OffsetDateTime, end: OffsetDateTime): Flux<TrackingSessionEntity> {
        return sessionRepository.findByTimeRange(start, end)
    }

    // ==================== Trajectory ====================

    /**
     * 궤적 데이터 저장
     */
    fun saveTrajectory(trajectory: TrackingTrajectoryEntity): Mono<Void> {
        return trajectoryRepository.save(trajectory)
            .doOnError { logger.error("궤적 저장 실패: {}", it.message) }
    }

    /**
     * 궤적 데이터 배치 저장
     */
    fun saveTrajectories(trajectories: List<TrackingTrajectoryEntity>): Mono<Void> {
        return trajectoryRepository.saveAll(trajectories)
            .doOnSuccess { logger.info("궤적 배치 저장 완료: {}건", trajectories.size) }
            .doOnError { logger.error("궤적 배치 저장 실패: {}", it.message) }
    }

    /**
     * 세션 ID로 궤적 조회
     */
    fun findTrajectoriesBySession(sessionId: Long): Flux<TrackingTrajectoryEntity> {
        return trajectoryRepository.findBySessionId(sessionId)
    }

    // ==================== Result ====================

    /**
     * 결과 데이터 저장
     */
    fun saveResult(result: TrackingResultEntity): Mono<Void> {
        return resultRepository.save(result)
            .doOnError { logger.error("결과 저장 실패: {}", it.message) }
    }

    /**
     * 결과 데이터 배치 저장
     */
    fun saveResults(results: List<TrackingResultEntity>): Mono<Void> {
        return resultRepository.saveAll(results)
            .doOnSuccess { logger.info("결과 배치 저장 완료: {}건", results.size) }
            .doOnError { logger.error("결과 배치 저장 실패: {}", it.message) }
    }

    /**
     * 세션 ID로 결과 조회
     */
    fun findResultsBySession(sessionId: Long): Flux<TrackingResultEntity> {
        return resultRepository.findBySessionId(sessionId)
    }

    // ==================== ICD Status ====================

    /**
     * ICD 상태 데이터 저장
     */
    fun saveIcdStatus(status: IcdStatusEntity): Mono<Void> {
        return icdStatusRepository.save(status)
            .doOnError { logger.error("ICD 상태 저장 실패: {}", it.message) }
    }

    /**
     * ICD 상태 배치 저장 (100ms 데이터 고속 저장)
     */
    fun saveIcdStatuses(statuses: List<IcdStatusEntity>): Mono<Void> {
        return icdStatusRepository.saveAll(statuses)
            .doOnError { logger.error("ICD 배치 저장 실패: {}", it.message) }
    }

    /**
     * 최근 ICD 상태 조회
     */
    fun findRecentIcdStatuses(limit: Int = 100): Flux<IcdStatusEntity> {
        return icdStatusRepository.findRecent(limit)
    }

    /**
     * 시간 범위로 ICD 상태 조회
     */
    fun findIcdStatusesByTimeRange(start: OffsetDateTime, end: OffsetDateTime): Flux<IcdStatusEntity> {
        return icdStatusRepository.findByTimeRange(start, end)
    }

    /**
     * ICD 상태 개수 조회
     */
    fun countIcdStatuses(): Mono<Long> {
        return icdStatusRepository.count()
    }

    // ==================== 데이터 정리 ====================

    /**
     * 오래된 세션 삭제 (365일 이전)
     * - CASCADE로 trajectory, result도 함께 삭제됨
     * - 매일 새벽 3시 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    fun cleanupOldSessions() {
        val cutoffDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays)
        sessionRepository.deleteOlderThan(cutoffDate)
            .doOnSuccess { count ->
                if (count != null && count > 0) {
                    logger.info("오래된 세션 삭제 완료: {}건 (기준일: {})", count, cutoffDate)
                }
            }
            .doOnError { logger.error("오래된 세션 삭제 실패: {}", it.message) }
            .subscribe()
    }

    /**
     * DB 연결 상태 확인
     */
    fun checkConnection(): Mono<Boolean> {
        return sessionRepository.count()
            .map { true }
            .onErrorReturn(false)
    }
}
