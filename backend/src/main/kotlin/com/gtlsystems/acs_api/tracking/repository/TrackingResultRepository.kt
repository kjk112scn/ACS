package com.gtlsystems.acs_api.tracking.repository

import com.gtlsystems.acs_api.tracking.entity.TrackingResultEntity
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * 실측 추적 결과 Repository
 * - Hypertable (PRIMARY KEY 없음) → DatabaseClient 사용
 *
 * V006 재설계:
 * - 이론치 컬럼 제거 (trajectory에서 JOIN 조회)
 * - ICD 추적 데이터 추가
 * - 정밀 추적 메타데이터 추가
 */
@Repository
@ConditionalOnBean(DatabaseClient::class)
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
             theoretical_timestamp, time_offset_ms, interpolation_fraction,
             lower_theoretical_index, upper_theoretical_index,
             cmd_azimuth, cmd_elevation, cmd_train,
             actual_azimuth, actual_elevation, actual_train,
             position_azimuth, position_elevation, position_train,
             tracking_azimuth_time, tracking_cmd_azimuth, tracking_actual_azimuth,
             tracking_elevation_time, tracking_cmd_elevation, tracking_actual_elevation,
             tracking_train_time, tracking_cmd_train, tracking_actual_train,
             azimuth_error, elevation_error, train_error, total_error,
             keyhole_active, keyhole_optimized, tracking_quality, interpolation_accuracy,
             kalman_azimuth, kalman_elevation, kalman_gain, created_at)
            VALUES
            (:timestamp, :sessionId, :index, :theoreticalIndex,
             :theoreticalTimestamp, :timeOffsetMs, :interpolationFraction,
             :lowerTheoreticalIndex, :upperTheoreticalIndex,
             :cmdAzimuth, :cmdElevation, :cmdTrain,
             :actualAzimuth, :actualElevation, :actualTrain,
             :positionAzimuth, :positionElevation, :positionTrain,
             :trackingAzimuthTime, :trackingCmdAzimuth, :trackingActualAzimuth,
             :trackingElevationTime, :trackingCmdElevation, :trackingActualElevation,
             :trackingTrainTime, :trackingCmdTrain, :trackingActualTrain,
             :azimuthError, :elevationError, :trainError, :totalError,
             :keyholeActive, :keyholeOptimized, :trackingQuality, :interpolationAccuracy,
             :kalmanAzimuth, :kalmanElevation, :kalmanGain, :createdAt)
        """.trimIndent())
            .bind("timestamp", entity.timestamp)
            .bind("sessionId", entity.sessionId)
            .bind("index", entity.index)
            .bindNullable("theoreticalIndex", entity.theoreticalIndex)
            // 정밀 추적 메타데이터 (V006)
            .bindNullable("theoreticalTimestamp", entity.theoreticalTimestamp)
            .bindNullable("timeOffsetMs", entity.timeOffsetMs)
            .bindNullable("interpolationFraction", entity.interpolationFraction)
            .bindNullable("lowerTheoreticalIndex", entity.lowerTheoreticalIndex)
            .bindNullable("upperTheoreticalIndex", entity.upperTheoreticalIndex)
            // 명령값
            .bindNullable("cmdAzimuth", entity.cmdAzimuth)
            .bindNullable("cmdElevation", entity.cmdElevation)
            .bindNullable("cmdTrain", entity.cmdTrain)
            // 실측값
            .bindNullable("actualAzimuth", entity.actualAzimuth)
            .bindNullable("actualElevation", entity.actualElevation)
            .bindNullable("actualTrain", entity.actualTrain)
            // 위치값
            .bindNullable("positionAzimuth", entity.positionAzimuth)
            .bindNullable("positionElevation", entity.positionElevation)
            .bindNullable("positionTrain", entity.positionTrain)
            // ICD 추적 데이터 (V006)
            .bindNullable("trackingAzimuthTime", entity.trackingAzimuthTime)
            .bindNullable("trackingCmdAzimuth", entity.trackingCmdAzimuth)
            .bindNullable("trackingActualAzimuth", entity.trackingActualAzimuth)
            .bindNullable("trackingElevationTime", entity.trackingElevationTime)
            .bindNullable("trackingCmdElevation", entity.trackingCmdElevation)
            .bindNullable("trackingActualElevation", entity.trackingActualElevation)
            .bindNullable("trackingTrainTime", entity.trackingTrainTime)
            .bindNullable("trackingCmdTrain", entity.trackingCmdTrain)
            .bindNullable("trackingActualTrain", entity.trackingActualTrain)
            // 오차
            .bindNullable("azimuthError", entity.azimuthError)
            .bindNullable("elevationError", entity.elevationError)
            .bindNullable("trainError", entity.trainError)
            .bindNullable("totalError", entity.totalError)
            // 상태
            .bind("keyholeActive", entity.keyholeActive)
            .bind("keyholeOptimized", entity.keyholeOptimized)
            .bindNullable("trackingQuality", entity.trackingQuality)
            .bindNullable("interpolationAccuracy", entity.interpolationAccuracy)
            // 칼만 필터 (V006)
            .bindNullable("kalmanAzimuth", entity.kalmanAzimuth)
            .bindNullable("kalmanElevation", entity.kalmanElevation)
            .bindNullable("kalmanGain", entity.kalmanGain)
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
            // 정밀 추적 메타데이터 (V006)
            theoreticalTimestamp = row.get("theoretical_timestamp", OffsetDateTime::class.java),
            timeOffsetMs = row.get("time_offset_ms", Double::class.java),
            interpolationFraction = row.get("interpolation_fraction", Double::class.java),
            lowerTheoreticalIndex = row.get("lower_theoretical_index", Int::class.java),
            upperTheoreticalIndex = row.get("upper_theoretical_index", Int::class.java),
            // 명령값
            cmdAzimuth = row.get("cmd_azimuth", Double::class.java),
            cmdElevation = row.get("cmd_elevation", Double::class.java),
            cmdTrain = row.get("cmd_train", Double::class.java),
            // 실측값
            actualAzimuth = row.get("actual_azimuth", Double::class.java),
            actualElevation = row.get("actual_elevation", Double::class.java),
            actualTrain = row.get("actual_train", Double::class.java),
            // 위치값
            positionAzimuth = row.get("position_azimuth", Double::class.java),
            positionElevation = row.get("position_elevation", Double::class.java),
            positionTrain = row.get("position_train", Double::class.java),
            // ICD 추적 데이터 (V006)
            trackingAzimuthTime = row.get("tracking_azimuth_time", Float::class.java),
            trackingCmdAzimuth = row.get("tracking_cmd_azimuth", Float::class.java),
            trackingActualAzimuth = row.get("tracking_actual_azimuth", Float::class.java),
            trackingElevationTime = row.get("tracking_elevation_time", Float::class.java),
            trackingCmdElevation = row.get("tracking_cmd_elevation", Float::class.java),
            trackingActualElevation = row.get("tracking_actual_elevation", Float::class.java),
            trackingTrainTime = row.get("tracking_train_time", Float::class.java),
            trackingCmdTrain = row.get("tracking_cmd_train", Float::class.java),
            trackingActualTrain = row.get("tracking_actual_train", Float::class.java),
            // 오차
            azimuthError = row.get("azimuth_error", Double::class.java),
            elevationError = row.get("elevation_error", Double::class.java),
            trainError = row.get("train_error", Double::class.java),
            totalError = row.get("total_error", Double::class.java),
            // 상태
            keyholeActive = row.get("keyhole_active", Boolean::class.java) ?: false,
            keyholeOptimized = row.get("keyhole_optimized", Boolean::class.java) ?: false,
            trackingQuality = row.get("tracking_quality", String::class.java),
            interpolationAccuracy = row.get("interpolation_accuracy", Double::class.java),
            // 칼만 필터 (V006)
            kalmanAzimuth = row.get("kalman_azimuth", Double::class.java),
            kalmanElevation = row.get("kalman_elevation", Double::class.java),
            kalmanGain = row.get("kalman_gain", Double::class.java),
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