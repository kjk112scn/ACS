package com.gtlsystems.acs_api.tracking.repository

import com.gtlsystems.acs_api.tracking.entity.HardwareErrorLogEntity
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 하드웨어 에러 로그 Repository
 * - Hypertable (복합 PK) → DatabaseClient 사용
 * - 에러 로깅 및 조회
 */
@Repository
@ConditionalOnBean(DatabaseClient::class)
class HardwareErrorLogRepository(
    private val databaseClient: DatabaseClient
) {
    /**
     * 에러 로그 저장
     */
    fun save(entity: HardwareErrorLogEntity): Mono<Void> {
        return databaseClient.sql("""
            INSERT INTO hardware_error_log (
                timestamp, error_code, error_type, error_message,
                source, axis, severity, tracking_mode, session_id,
                raw_data, correlation_id, occurrence_count,
                resolved, resolved_at, resolved_by, resolution_note,
                is_initial_error
            ) VALUES (
                :timestamp, :errorCode, :errorType, :errorMessage,
                :source, :axis, :severity, :trackingMode, :sessionId,
                :rawData::jsonb, :correlationId, :occurrenceCount,
                :resolved, :resolvedAt, :resolvedBy, :resolutionNote,
                :isInitialError
            )
        """.trimIndent())
            .bind("timestamp", entity.timestamp)
            .bind("errorCode", entity.errorCode)
            .bind("errorType", entity.errorType)
            .bindNullable("errorMessage", entity.errorMessage)
            .bind("source", entity.source)
            .bindNullable("axis", entity.axis)
            .bind("severity", entity.severity)
            .bindNullable("trackingMode", entity.trackingMode)
            .bindNullable("sessionId", entity.sessionId)
            .bindNullable("rawData", entity.rawData)
            .bindNullable("correlationId", entity.correlationId)
            .bind("occurrenceCount", entity.occurrenceCount)
            .bind("resolved", entity.resolved)
            .bindNullable("resolvedAt", entity.resolvedAt)
            .bindNullable("resolvedBy", entity.resolvedBy)
            .bindNullable("resolutionNote", entity.resolutionNote)
            .bind("isInitialError", entity.isInitialError)
            .then()
    }

    /**
     * 배치 저장
     */
    fun saveAll(entities: List<HardwareErrorLogEntity>): Mono<Void> {
        return Flux.fromIterable(entities)
            .flatMap { save(it) }
            .then()
    }

    // ==================== 조회 ====================

    /**
     * 시간 범위로 조회
     */
    fun findByTimeRange(startTime: OffsetDateTime, endTime: OffsetDateTime): Flux<HardwareErrorLogEntity> {
        return databaseClient.sql("""
            SELECT * FROM hardware_error_log
            WHERE timestamp >= :startTime AND timestamp <= :endTime
            ORDER BY timestamp DESC
        """.trimIndent())
            .bind("startTime", startTime)
            .bind("endTime", endTime)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 최근 에러 조회
     */
    fun findRecent(limit: Int): Flux<HardwareErrorLogEntity> {
        return databaseClient.sql("""
            SELECT * FROM hardware_error_log
            ORDER BY timestamp DESC
            LIMIT :limit
        """.trimIndent())
            .bind("limit", limit)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 미해결 에러 조회
     */
    fun findUnresolved(limit: Int = 100): Flux<HardwareErrorLogEntity> {
        return databaseClient.sql("""
            SELECT * FROM hardware_error_log
            WHERE resolved = FALSE
            ORDER BY timestamp DESC
            LIMIT :limit
        """.trimIndent())
            .bind("limit", limit)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 심각도별 조회
     */
    fun findBySeverity(severity: String, limit: Int = 100): Flux<HardwareErrorLogEntity> {
        return databaseClient.sql("""
            SELECT * FROM hardware_error_log
            WHERE severity = :severity
            ORDER BY timestamp DESC
            LIMIT :limit
        """.trimIndent())
            .bind("severity", severity)
            .bind("limit", limit)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 에러 타입별 조회
     */
    fun findByErrorType(errorType: String, limit: Int = 100): Flux<HardwareErrorLogEntity> {
        return databaseClient.sql("""
            SELECT * FROM hardware_error_log
            WHERE error_type = :errorType
            ORDER BY timestamp DESC
            LIMIT :limit
        """.trimIndent())
            .bind("errorType", errorType)
            .bind("limit", limit)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 세션별 에러 조회
     */
    fun findBySessionId(sessionId: Long): Flux<HardwareErrorLogEntity> {
        return databaseClient.sql("""
            SELECT * FROM hardware_error_log
            WHERE session_id = :sessionId
            ORDER BY timestamp
        """.trimIndent())
            .bind("sessionId", sessionId)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 상관 ID로 연관 에러 조회
     */
    fun findByCorrelationId(correlationId: UUID): Flux<HardwareErrorLogEntity> {
        return databaseClient.sql("""
            SELECT * FROM hardware_error_log
            WHERE correlation_id = :correlationId
            ORDER BY timestamp
        """.trimIndent())
            .bind("correlationId", correlationId)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    // ==================== 통계 ====================

    /**
     * 전체 에러 수
     */
    fun count(): Mono<Long> {
        return databaseClient.sql("SELECT COUNT(*) FROM hardware_error_log")
            .map { row, _ -> row.get(0, Long::class.java)!! }
            .one()
    }

    /**
     * 미해결 에러 수
     */
    fun countUnresolved(): Mono<Long> {
        return databaseClient.sql("SELECT COUNT(*) FROM hardware_error_log WHERE resolved = FALSE")
            .map { row, _ -> row.get(0, Long::class.java)!! }
            .one()
    }

    /**
     * 심각도별 에러 수
     */
    fun countBySeverity(severity: String): Mono<Long> {
        return databaseClient.sql("SELECT COUNT(*) FROM hardware_error_log WHERE severity = :severity")
            .bind("severity", severity)
            .map { row, _ -> row.get(0, Long::class.java)!! }
            .one()
    }

    // ==================== 해결 처리 ====================

    /**
     * 에러 해결 처리
     */
    fun markResolved(timestamp: OffsetDateTime, resolvedBy: String, resolutionNote: String?): Mono<Long> {
        return databaseClient.sql("""
            UPDATE hardware_error_log
            SET resolved = TRUE, resolved_at = NOW(), resolved_by = :resolvedBy, resolution_note = :resolutionNote
            WHERE timestamp = :timestamp AND resolved = FALSE
        """.trimIndent())
            .bind("timestamp", timestamp)
            .bind("resolvedBy", resolvedBy)
            .bindNullable("resolutionNote", resolutionNote)
            .fetch()
            .rowsUpdated()
    }

    /**
     * 상관 ID로 일괄 해결 처리
     */
    fun markResolvedByCorrelationId(correlationId: UUID, resolvedBy: String, resolutionNote: String?): Mono<Long> {
        return databaseClient.sql("""
            UPDATE hardware_error_log
            SET resolved = TRUE, resolved_at = NOW(), resolved_by = :resolvedBy, resolution_note = :resolutionNote
            WHERE correlation_id = :correlationId AND resolved = FALSE
        """.trimIndent())
            .bind("correlationId", correlationId)
            .bind("resolvedBy", resolvedBy)
            .bindNullable("resolutionNote", resolutionNote)
            .fetch()
            .rowsUpdated()
    }

    // ==================== 매핑 ====================

    private fun mapRowToEntity(row: io.r2dbc.spi.Row): HardwareErrorLogEntity {
        // ✅ session_id null 안전 처리 (R2DBC Long::class.java는 primitive로 처리되어 null 불가)
        val sessionIdValue: Long? = try {
            row.get("session_id", java.lang.Long::class.java)?.toLong()
        } catch (e: Exception) {
            null
        }

        return HardwareErrorLogEntity(
            timestamp = row.get("timestamp", OffsetDateTime::class.java)!!,
            errorCode = row.get("error_code", String::class.java)!!,
            errorType = row.get("error_type", String::class.java)!!,
            errorMessage = row.get("error_message", String::class.java),
            source = row.get("source", String::class.java)!!,
            axis = row.get("axis", String::class.java),
            severity = row.get("severity", String::class.java)!!,
            trackingMode = row.get("tracking_mode", String::class.java),
            sessionId = sessionIdValue,
            rawData = row.get("raw_data", String::class.java),
            correlationId = row.get("correlation_id", UUID::class.java),
            occurrenceCount = row.get("occurrence_count", Int::class.java) ?: 1,
            resolved = row.get("resolved", Boolean::class.java) ?: false,
            resolvedAt = row.get("resolved_at", OffsetDateTime::class.java),
            resolvedBy = row.get("resolved_by", String::class.java),
            resolutionNote = row.get("resolution_note", String::class.java),
            isInitialError = row.get("is_initial_error", Boolean::class.java) ?: false
        )
    }

    /**
     * null 허용 바인딩 확장 함수
     */
    private fun <T : Any> DatabaseClient.GenericExecuteSpec.bindNullable(
        name: String,
        value: T?
    ): DatabaseClient.GenericExecuteSpec {
        return if (value != null) {
            this.bind(name, value)
        } else {
            this.bindNull(name, Any::class.java)
        }
    }
}
