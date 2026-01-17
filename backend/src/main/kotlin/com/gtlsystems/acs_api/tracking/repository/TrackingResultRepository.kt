package com.gtlsystems.acs_api.tracking.repository

import com.gtlsystems.acs_api.tracking.entity.TrackingResultEntity
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * 실측 추적 결과 Repository
 * - Hypertable (PRIMARY KEY 없음) → DatabaseClient 사용
 */
@Repository
class TrackingResultRepository(
    private val databaseClient: DatabaseClient
) {
    /**
     * 결과 데이터 저장
     */
    fun save(entity: TrackingResultEntity): Mono<Void> {
        return databaseClient.sql("""
            INSERT INTO tracking_result
            (timestamp, session_id, index, theoretical_index,
             original_azimuth, original_elevation,
             transformed_azimuth, transformed_elevation, transformed_train,
             final_azimuth, final_elevation, final_train,
             actual_azimuth, actual_elevation, actual_train,
             azimuth_error, elevation_error, train_error, total_error,
             azimuth_rate, elevation_rate, train_rate,
             azimuth_acceleration, elevation_acceleration, train_acceleration,
             keyhole_active, keyhole_optimized, tracking_quality,
             interpolation_type, interpolation_accuracy,
             satellite_range, satellite_altitude, satellite_velocity,
             cmd_azimuth, cmd_elevation, cmd_train,
             position_azimuth, position_elevation, position_train, created_at)
            VALUES
            (:timestamp, :sessionId, :index, :theoreticalIndex,
             :originalAzimuth, :originalElevation,
             :transformedAzimuth, :transformedElevation, :transformedTrain,
             :finalAzimuth, :finalElevation, :finalTrain,
             :actualAzimuth, :actualElevation, :actualTrain,
             :azimuthError, :elevationError, :trainError, :totalError,
             :azimuthRate, :elevationRate, :trainRate,
             :azimuthAcceleration, :elevationAcceleration, :trainAcceleration,
             :keyholeActive, :keyholeOptimized, :trackingQuality,
             :interpolationType, :interpolationAccuracy,
             :satelliteRange, :satelliteAltitude, :satelliteVelocity,
             :cmdAzimuth, :cmdElevation, :cmdTrain,
             :positionAzimuth, :positionElevation, :positionTrain, :createdAt)
        """.trimIndent())
            .bind("timestamp", entity.timestamp)
            .bind("sessionId", entity.sessionId)
            .bind("index", entity.index)
            .bindNullable("theoreticalIndex", entity.theoreticalIndex)
            .bindNullable("originalAzimuth", entity.originalAzimuth)
            .bindNullable("originalElevation", entity.originalElevation)
            .bindNullable("transformedAzimuth", entity.transformedAzimuth)
            .bindNullable("transformedElevation", entity.transformedElevation)
            .bindNullable("transformedTrain", entity.transformedTrain)
            .bindNullable("finalAzimuth", entity.finalAzimuth)
            .bindNullable("finalElevation", entity.finalElevation)
            .bindNullable("finalTrain", entity.finalTrain)
            .bindNullable("actualAzimuth", entity.actualAzimuth)
            .bindNullable("actualElevation", entity.actualElevation)
            .bindNullable("actualTrain", entity.actualTrain)
            .bindNullable("azimuthError", entity.azimuthError)
            .bindNullable("elevationError", entity.elevationError)
            .bindNullable("trainError", entity.trainError)
            .bindNullable("totalError", entity.totalError)
            .bindNullable("azimuthRate", entity.azimuthRate)
            .bindNullable("elevationRate", entity.elevationRate)
            .bindNullable("trainRate", entity.trainRate)
            .bindNullable("azimuthAcceleration", entity.azimuthAcceleration)
            .bindNullable("elevationAcceleration", entity.elevationAcceleration)
            .bindNullable("trainAcceleration", entity.trainAcceleration)
            .bind("keyholeActive", entity.keyholeActive)
            .bind("keyholeOptimized", entity.keyholeOptimized)
            .bindNullable("trackingQuality", entity.trackingQuality)
            .bindNullable("interpolationType", entity.interpolationType)
            .bindNullable("interpolationAccuracy", entity.interpolationAccuracy)
            .bindNullable("satelliteRange", entity.satelliteRange)
            .bindNullable("satelliteAltitude", entity.satelliteAltitude)
            .bindNullable("satelliteVelocity", entity.satelliteVelocity)
            .bindNullable("cmdAzimuth", entity.cmdAzimuth)
            .bindNullable("cmdElevation", entity.cmdElevation)
            .bindNullable("cmdTrain", entity.cmdTrain)
            .bindNullable("positionAzimuth", entity.positionAzimuth)
            .bindNullable("positionElevation", entity.positionElevation)
            .bindNullable("positionTrain", entity.positionTrain)
            .bind("createdAt", entity.createdAt ?: OffsetDateTime.now())
            .then()
    }

    /**
     * 배치 저장
     */
    fun saveAll(entities: List<TrackingResultEntity>): Mono<Void> {
        return Flux.fromIterable(entities)
            .flatMap { save(it) }
            .then()
    }

    /**
     * 세션 ID로 조회
     */
    fun findBySessionId(sessionId: Long): Flux<TrackingResultEntity> {
        return databaseClient.sql("""
            SELECT * FROM tracking_result
            WHERE session_id = :sessionId
            ORDER BY timestamp
        """.trimIndent())
            .bind("sessionId", sessionId)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 시간 범위로 조회
     */
    fun findByTimeRange(startTime: OffsetDateTime, endTime: OffsetDateTime): Flux<TrackingResultEntity> {
        return databaseClient.sql("""
            SELECT * FROM tracking_result
            WHERE timestamp >= :startTime AND timestamp <= :endTime
            ORDER BY timestamp
        """.trimIndent())
            .bind("startTime", startTime)
            .bind("endTime", endTime)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    private fun mapRowToEntity(row: io.r2dbc.spi.Row): TrackingResultEntity {
        return TrackingResultEntity(
            timestamp = row.get("timestamp", OffsetDateTime::class.java)!!,
            sessionId = row.get("session_id", Long::class.java)!!,
            index = row.get("index", Int::class.java)!!,
            theoreticalIndex = row.get("theoretical_index", Int::class.java),
            originalAzimuth = row.get("original_azimuth", Double::class.java),
            originalElevation = row.get("original_elevation", Double::class.java),
            transformedAzimuth = row.get("transformed_azimuth", Double::class.java),
            transformedElevation = row.get("transformed_elevation", Double::class.java),
            transformedTrain = row.get("transformed_train", Double::class.java),
            finalAzimuth = row.get("final_azimuth", Double::class.java),
            finalElevation = row.get("final_elevation", Double::class.java),
            finalTrain = row.get("final_train", Double::class.java),
            actualAzimuth = row.get("actual_azimuth", Double::class.java),
            actualElevation = row.get("actual_elevation", Double::class.java),
            actualTrain = row.get("actual_train", Double::class.java),
            azimuthError = row.get("azimuth_error", Double::class.java),
            elevationError = row.get("elevation_error", Double::class.java),
            trainError = row.get("train_error", Double::class.java),
            totalError = row.get("total_error", Double::class.java),
            azimuthRate = row.get("azimuth_rate", Double::class.java),
            elevationRate = row.get("elevation_rate", Double::class.java),
            trainRate = row.get("train_rate", Double::class.java),
            azimuthAcceleration = row.get("azimuth_acceleration", Double::class.java),
            elevationAcceleration = row.get("elevation_acceleration", Double::class.java),
            trainAcceleration = row.get("train_acceleration", Double::class.java),
            keyholeActive = row.get("keyhole_active", Boolean::class.java) ?: false,
            keyholeOptimized = row.get("keyhole_optimized", Boolean::class.java) ?: false,
            trackingQuality = row.get("tracking_quality", String::class.java),
            interpolationType = row.get("interpolation_type", String::class.java),
            interpolationAccuracy = row.get("interpolation_accuracy", Double::class.java),
            satelliteRange = row.get("satellite_range", Double::class.java),
            satelliteAltitude = row.get("satellite_altitude", Double::class.java),
            satelliteVelocity = row.get("satellite_velocity", Double::class.java),
            cmdAzimuth = row.get("cmd_azimuth", Double::class.java),
            cmdElevation = row.get("cmd_elevation", Double::class.java),
            cmdTrain = row.get("cmd_train", Double::class.java),
            positionAzimuth = row.get("position_azimuth", Double::class.java),
            positionElevation = row.get("position_elevation", Double::class.java),
            positionTrain = row.get("position_train", Double::class.java),
            createdAt = row.get("created_at", OffsetDateTime::class.java)
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
