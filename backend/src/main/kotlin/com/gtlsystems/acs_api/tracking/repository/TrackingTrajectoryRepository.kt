package com.gtlsystems.acs_api.tracking.repository

import com.gtlsystems.acs_api.tracking.entity.TrackingTrajectoryEntity
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * 이론 궤적 Repository
 * - Hypertable (PRIMARY KEY 없음) → DatabaseClient 사용
 */
@Repository
@ConditionalOnBean(DatabaseClient::class)
class TrackingTrajectoryRepository(
    private val databaseClient: DatabaseClient
) {
    /**
     * 궤적 데이터 저장
     */
    fun save(entity: TrackingTrajectoryEntity): Mono<Void> {
        return databaseClient.sql("""
            INSERT INTO tracking_trajectory
            (timestamp, session_id, detail_id, data_type, index, azimuth, elevation, train, azimuth_rate, elevation_rate, created_at)
            VALUES (:timestamp, :sessionId, :detailId, :dataType, :index, :azimuth, :elevation, :train, :azimuthRate, :elevationRate, :createdAt)
        """.trimIndent())
            .bind("timestamp", entity.timestamp)
            .bind("sessionId", entity.sessionId)
            .bind("detailId", entity.detailId)
            .bind("dataType", entity.dataType)
            .bind("index", entity.index)
            .bind("azimuth", entity.azimuth)
            .bind("elevation", entity.elevation)
            .bindNull("train", entity.train, Double::class.java)
            .bindNull("azimuthRate", entity.azimuthRate, Double::class.java)
            .bindNull("elevationRate", entity.elevationRate, Double::class.java)
            .bind("createdAt", entity.createdAt ?: OffsetDateTime.now())
            .then()
    }

    /**
     * 배치 저장
     */
    fun saveAll(entities: List<TrackingTrajectoryEntity>): Mono<Void> {
        return Flux.fromIterable(entities)
            .flatMap { save(it) }
            .then()
    }

    /**
     * 세션 ID로 조회
     */
    fun findBySessionId(sessionId: Long): Flux<TrackingTrajectoryEntity> {
        return databaseClient.sql("""
            SELECT * FROM tracking_trajectory
            WHERE session_id = :sessionId
            ORDER BY timestamp
        """.trimIndent())
            .bind("sessionId", sessionId)
            .map { row, _ ->
                TrackingTrajectoryEntity(
                    timestamp = row.get("timestamp", OffsetDateTime::class.java)!!,
                    sessionId = row.get("session_id", Long::class.java)!!,
                    detailId = row.get("detail_id", Int::class.java)!!,
                    dataType = row.get("data_type", String::class.java)!!,
                    index = row.get("index", Int::class.java)!!,
                    azimuth = row.get("azimuth", Double::class.java)!!,
                    elevation = row.get("elevation", Double::class.java)!!,
                    // ✅ FIX: javaObjectType 사용 - primitive double은 null 불가
                    train = row.get("train", Double::class.javaObjectType),
                    azimuthRate = row.get("azimuth_rate", Double::class.javaObjectType),
                    elevationRate = row.get("elevation_rate", Double::class.javaObjectType),
                    createdAt = row.get("created_at", OffsetDateTime::class.java)
                )
            }
            .all()
    }

    /**
     * 세션 ID + Detail ID로 조회
     */
    fun findBySessionIdAndDetailId(sessionId: Long, detailId: Int): Flux<TrackingTrajectoryEntity> {
        return databaseClient.sql("""
            SELECT * FROM tracking_trajectory
            WHERE session_id = :sessionId AND detail_id = :detailId
            ORDER BY timestamp
        """.trimIndent())
            .bind("sessionId", sessionId)
            .bind("detailId", detailId)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 세션 ID + Data Type으로 조회
     */
    fun findBySessionIdAndDataType(sessionId: Long, dataType: String): Flux<TrackingTrajectoryEntity> {
        return databaseClient.sql("""
            SELECT * FROM tracking_trajectory
            WHERE session_id = :sessionId AND data_type = :dataType
            ORDER BY timestamp
        """.trimIndent())
            .bind("sessionId", sessionId)
            .bind("dataType", dataType)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 시간 범위로 조회
     */
    fun findByTimeRange(startTime: OffsetDateTime, endTime: OffsetDateTime): Flux<TrackingTrajectoryEntity> {
        return databaseClient.sql("""
            SELECT * FROM tracking_trajectory
            WHERE timestamp >= :startTime AND timestamp <= :endTime
            ORDER BY timestamp
        """.trimIndent())
            .bind("startTime", startTime)
            .bind("endTime", endTime)
            .map { row, _ ->
                TrackingTrajectoryEntity(
                    timestamp = row.get("timestamp", OffsetDateTime::class.java)!!,
                    sessionId = row.get("session_id", Long::class.java)!!,
                    detailId = row.get("detail_id", Int::class.java)!!,
                    dataType = row.get("data_type", String::class.java)!!,
                    index = row.get("index", Int::class.java)!!,
                    azimuth = row.get("azimuth", Double::class.java)!!,
                    elevation = row.get("elevation", Double::class.java)!!,
                    // ✅ FIX: javaObjectType 사용 - primitive double은 null 불가
                    train = row.get("train", Double::class.javaObjectType),
                    azimuthRate = row.get("azimuth_rate", Double::class.javaObjectType),
                    elevationRate = row.get("elevation_rate", Double::class.javaObjectType),
                    createdAt = row.get("created_at", OffsetDateTime::class.java)
                )
            }
            .all()
    }

    /**
     * Row → Entity 매핑 헬퍼
     */
    private fun mapRowToEntity(row: io.r2dbc.spi.Row): TrackingTrajectoryEntity {
        return TrackingTrajectoryEntity(
            timestamp = row.get("timestamp", OffsetDateTime::class.java)!!,
            sessionId = row.get("session_id", Long::class.java)!!,
            detailId = row.get("detail_id", Int::class.java)!!,
            dataType = row.get("data_type", String::class.java)!!,
            index = row.get("index", Int::class.java)!!,
            azimuth = row.get("azimuth", Double::class.java)!!,
            elevation = row.get("elevation", Double::class.java)!!,
            // ✅ FIX: javaObjectType 사용 - primitive double은 null 불가
            train = row.get("train", Double::class.javaObjectType),
            azimuthRate = row.get("azimuth_rate", Double::class.javaObjectType),
            elevationRate = row.get("elevation_rate", Double::class.javaObjectType),
            createdAt = row.get("created_at", OffsetDateTime::class.java)
        )
    }

    /**
     * null 값 바인딩 헬퍼
     */
    private fun <T> DatabaseClient.GenericExecuteSpec.bindNull(
        name: String,
        value: T?,
        type: Class<T>
    ): DatabaseClient.GenericExecuteSpec {
        return if (value != null) {
            this.bind(name, value)
        } else {
            this.bindNull(name, type)
        }
    }
}
