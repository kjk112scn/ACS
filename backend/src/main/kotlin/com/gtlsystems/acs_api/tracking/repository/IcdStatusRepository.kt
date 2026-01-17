package com.gtlsystems.acs_api.tracking.repository

import com.gtlsystems.acs_api.tracking.entity.IcdStatusEntity
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

/**
 * ICD 상태 Repository
 * - Hypertable (PRIMARY KEY 없음) → DatabaseClient 사용
 * - 100ms 주기 대용량 데이터 처리
 */
@Repository
class IcdStatusRepository(
    private val databaseClient: DatabaseClient
) {
    /**
     * ICD 상태 데이터 저장
     */
    fun save(entity: IcdStatusEntity): Mono<Void> {
        return databaseClient.sql("""
            INSERT INTO icd_status (
                timestamp,
                azimuth_angle, elevation_angle, train_angle,
                servo_driver_azimuth_angle, servo_driver_elevation_angle, servo_driver_train_angle,
                azimuth_speed, elevation_speed, train_speed,
                torque_azimuth, torque_elevation, torque_train,
                azimuth_acceleration, elevation_acceleration, train_acceleration,
                azimuth_max_acceleration, elevation_max_acceleration, train_max_acceleration,
                wind_speed, wind_direction, rtd_one, rtd_two,
                mode_status_bits, main_board_protocol_status, main_board_status,
                main_board_mc_onoff, main_board_reserve,
                azimuth_servo_status, azimuth_board_status,
                elevation_servo_status, elevation_board_status,
                train_servo_status, train_board_status, feed_board_etc_status,
                feed_s_board_status, feed_x_board_status, feed_ka_board_status,
                current_sband_lna_lhcp, current_sband_lna_rhcp,
                current_xband_lna_lhcp, current_xband_lna_rhcp,
                current_kaband_lna_lhcp, current_kaband_lna_rhcp,
                rssi_sband_lna_lhcp, rssi_sband_lna_rhcp,
                rssi_xband_lna_lhcp, rssi_xband_lna_rhcp,
                rssi_kaband_lna_lhcp, rssi_kaband_lna_rhcp,
                tracking_azimuth_time, tracking_cmd_azimuth, tracking_actual_azimuth,
                tracking_elevation_time, tracking_cmd_elevation, tracking_actual_elevation,
                tracking_train_time, tracking_cmd_train, tracking_actual_train
            ) VALUES (
                :timestamp,
                :azimuthAngle, :elevationAngle, :trainAngle,
                :servoDriverAzimuthAngle, :servoDriverElevationAngle, :servoDriverTrainAngle,
                :azimuthSpeed, :elevationSpeed, :trainSpeed,
                :torqueAzimuth, :torqueElevation, :torqueTrain,
                :azimuthAcceleration, :elevationAcceleration, :trainAcceleration,
                :azimuthMaxAcceleration, :elevationMaxAcceleration, :trainMaxAcceleration,
                :windSpeed, :windDirection, :rtdOne, :rtdTwo,
                :modeStatusBits, :mainBoardProtocolStatus, :mainBoardStatus,
                :mainBoardMcOnoff, :mainBoardReserve,
                :azimuthServoStatus, :azimuthBoardStatus,
                :elevationServoStatus, :elevationBoardStatus,
                :trainServoStatus, :trainBoardStatus, :feedBoardEtcStatus,
                :feedSBoardStatus, :feedXBoardStatus, :feedKaBoardStatus,
                :currentSbandLnaLhcp, :currentSbandLnaRhcp,
                :currentXbandLnaLhcp, :currentXbandLnaRhcp,
                :currentKabandLnaLhcp, :currentKabandLnaRhcp,
                :rssiSbandLnaLhcp, :rssiSbandLnaRhcp,
                :rssiXbandLnaLhcp, :rssiXbandLnaRhcp,
                :rssiKabandLnaLhcp, :rssiKabandLnaRhcp,
                :trackingAzimuthTime, :trackingCmdAzimuth, :trackingActualAzimuth,
                :trackingElevationTime, :trackingCmdElevation, :trackingActualElevation,
                :trackingTrainTime, :trackingCmdTrain, :trackingActualTrain
            )
        """.trimIndent())
            .bind("timestamp", entity.timestamp)
            .bindNullable("azimuthAngle", entity.azimuthAngle)
            .bindNullable("elevationAngle", entity.elevationAngle)
            .bindNullable("trainAngle", entity.trainAngle)
            .bindNullable("servoDriverAzimuthAngle", entity.servoDriverAzimuthAngle)
            .bindNullable("servoDriverElevationAngle", entity.servoDriverElevationAngle)
            .bindNullable("servoDriverTrainAngle", entity.servoDriverTrainAngle)
            .bindNullable("azimuthSpeed", entity.azimuthSpeed)
            .bindNullable("elevationSpeed", entity.elevationSpeed)
            .bindNullable("trainSpeed", entity.trainSpeed)
            .bindNullable("torqueAzimuth", entity.torqueAzimuth)
            .bindNullable("torqueElevation", entity.torqueElevation)
            .bindNullable("torqueTrain", entity.torqueTrain)
            .bindNullable("azimuthAcceleration", entity.azimuthAcceleration)
            .bindNullable("elevationAcceleration", entity.elevationAcceleration)
            .bindNullable("trainAcceleration", entity.trainAcceleration)
            .bindNullable("azimuthMaxAcceleration", entity.azimuthMaxAcceleration)
            .bindNullable("elevationMaxAcceleration", entity.elevationMaxAcceleration)
            .bindNullable("trainMaxAcceleration", entity.trainMaxAcceleration)
            .bindNullable("windSpeed", entity.windSpeed)
            .bindNullable("windDirection", entity.windDirection)
            .bindNullable("rtdOne", entity.rtdOne)
            .bindNullable("rtdTwo", entity.rtdTwo)
            .bindNullable("modeStatusBits", entity.modeStatusBits)
            .bindNullable("mainBoardProtocolStatus", entity.mainBoardProtocolStatus)
            .bindNullable("mainBoardStatus", entity.mainBoardStatus)
            .bindNullable("mainBoardMcOnoff", entity.mainBoardMcOnoff)
            .bindNullable("mainBoardReserve", entity.mainBoardReserve)
            .bindNullable("azimuthServoStatus", entity.azimuthServoStatus)
            .bindNullable("azimuthBoardStatus", entity.azimuthBoardStatus)
            .bindNullable("elevationServoStatus", entity.elevationServoStatus)
            .bindNullable("elevationBoardStatus", entity.elevationBoardStatus)
            .bindNullable("trainServoStatus", entity.trainServoStatus)
            .bindNullable("trainBoardStatus", entity.trainBoardStatus)
            .bindNullable("feedBoardEtcStatus", entity.feedBoardEtcStatus)
            .bindNullable("feedSBoardStatus", entity.feedSBoardStatus)
            .bindNullable("feedXBoardStatus", entity.feedXBoardStatus)
            .bindNullable("feedKaBoardStatus", entity.feedKaBoardStatus)
            .bindNullable("currentSbandLnaLhcp", entity.currentSbandLnaLhcp)
            .bindNullable("currentSbandLnaRhcp", entity.currentSbandLnaRhcp)
            .bindNullable("currentXbandLnaLhcp", entity.currentXbandLnaLhcp)
            .bindNullable("currentXbandLnaRhcp", entity.currentXbandLnaRhcp)
            .bindNullable("currentKabandLnaLhcp", entity.currentKabandLnaLhcp)
            .bindNullable("currentKabandLnaRhcp", entity.currentKabandLnaRhcp)
            .bindNullable("rssiSbandLnaLhcp", entity.rssiSbandLnaLhcp)
            .bindNullable("rssiSbandLnaRhcp", entity.rssiSbandLnaRhcp)
            .bindNullable("rssiXbandLnaLhcp", entity.rssiXbandLnaLhcp)
            .bindNullable("rssiXbandLnaRhcp", entity.rssiXbandLnaRhcp)
            .bindNullable("rssiKabandLnaLhcp", entity.rssiKabandLnaLhcp)
            .bindNullable("rssiKabandLnaRhcp", entity.rssiKabandLnaRhcp)
            .bindNullable("trackingAzimuthTime", entity.trackingAzimuthTime)
            .bindNullable("trackingCmdAzimuth", entity.trackingCmdAzimuth)
            .bindNullable("trackingActualAzimuth", entity.trackingActualAzimuth)
            .bindNullable("trackingElevationTime", entity.trackingElevationTime)
            .bindNullable("trackingCmdElevation", entity.trackingCmdElevation)
            .bindNullable("trackingActualElevation", entity.trackingActualElevation)
            .bindNullable("trackingTrainTime", entity.trackingTrainTime)
            .bindNullable("trackingCmdTrain", entity.trackingCmdTrain)
            .bindNullable("trackingActualTrain", entity.trackingActualTrain)
            .then()
    }

    /**
     * 배치 저장 (100ms 데이터 고속 저장)
     */
    fun saveAll(entities: List<IcdStatusEntity>): Mono<Void> {
        return Flux.fromIterable(entities)
            .flatMap { save(it) }
            .then()
    }

    /**
     * 시간 범위로 조회
     */
    fun findByTimeRange(startTime: OffsetDateTime, endTime: OffsetDateTime): Flux<IcdStatusEntity> {
        return databaseClient.sql("""
            SELECT * FROM icd_status
            WHERE timestamp >= :startTime AND timestamp <= :endTime
            ORDER BY timestamp
        """.trimIndent())
            .bind("startTime", startTime)
            .bind("endTime", endTime)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 최근 N개 데이터 조회
     */
    fun findRecent(limit: Int): Flux<IcdStatusEntity> {
        return databaseClient.sql("""
            SELECT * FROM icd_status
            ORDER BY timestamp DESC
            LIMIT :limit
        """.trimIndent())
            .bind("limit", limit)
            .map { row, _ -> mapRowToEntity(row) }
            .all()
    }

    /**
     * 데이터 개수 조회
     */
    fun count(): Mono<Long> {
        return databaseClient.sql("SELECT COUNT(*) FROM icd_status")
            .map { row, _ -> row.get(0, Long::class.java)!! }
            .one()
    }

    private fun mapRowToEntity(row: io.r2dbc.spi.Row): IcdStatusEntity {
        return IcdStatusEntity(
            timestamp = row.get("timestamp", OffsetDateTime::class.java)!!,
            azimuthAngle = row.get("azimuth_angle", Float::class.java),
            elevationAngle = row.get("elevation_angle", Float::class.java),
            trainAngle = row.get("train_angle", Float::class.java),
            servoDriverAzimuthAngle = row.get("servo_driver_azimuth_angle", Float::class.java),
            servoDriverElevationAngle = row.get("servo_driver_elevation_angle", Float::class.java),
            servoDriverTrainAngle = row.get("servo_driver_train_angle", Float::class.java),
            azimuthSpeed = row.get("azimuth_speed", Float::class.java),
            elevationSpeed = row.get("elevation_speed", Float::class.java),
            trainSpeed = row.get("train_speed", Float::class.java),
            torqueAzimuth = row.get("torque_azimuth", Float::class.java),
            torqueElevation = row.get("torque_elevation", Float::class.java),
            torqueTrain = row.get("torque_train", Float::class.java),
            azimuthAcceleration = row.get("azimuth_acceleration", Float::class.java),
            elevationAcceleration = row.get("elevation_acceleration", Float::class.java),
            trainAcceleration = row.get("train_acceleration", Float::class.java),
            azimuthMaxAcceleration = row.get("azimuth_max_acceleration", Float::class.java),
            elevationMaxAcceleration = row.get("elevation_max_acceleration", Float::class.java),
            trainMaxAcceleration = row.get("train_max_acceleration", Float::class.java),
            windSpeed = row.get("wind_speed", Float::class.java),
            windDirection = row.get("wind_direction", Short::class.java),
            rtdOne = row.get("rtd_one", Float::class.java),
            rtdTwo = row.get("rtd_two", Float::class.java),
            modeStatusBits = row.get("mode_status_bits", String::class.java),
            mainBoardProtocolStatus = row.get("main_board_protocol_status", String::class.java),
            mainBoardStatus = row.get("main_board_status", String::class.java),
            mainBoardMcOnoff = row.get("main_board_mc_onoff", String::class.java),
            mainBoardReserve = row.get("main_board_reserve", String::class.java),
            azimuthServoStatus = row.get("azimuth_servo_status", String::class.java),
            azimuthBoardStatus = row.get("azimuth_board_status", String::class.java),
            elevationServoStatus = row.get("elevation_servo_status", String::class.java),
            elevationBoardStatus = row.get("elevation_board_status", String::class.java),
            trainServoStatus = row.get("train_servo_status", String::class.java),
            trainBoardStatus = row.get("train_board_status", String::class.java),
            feedBoardEtcStatus = row.get("feed_board_etc_status", String::class.java),
            feedSBoardStatus = row.get("feed_s_board_status", String::class.java),
            feedXBoardStatus = row.get("feed_x_board_status", String::class.java),
            feedKaBoardStatus = row.get("feed_ka_board_status", String::class.java),
            currentSbandLnaLhcp = row.get("current_sband_lna_lhcp", Float::class.java),
            currentSbandLnaRhcp = row.get("current_sband_lna_rhcp", Float::class.java),
            currentXbandLnaLhcp = row.get("current_xband_lna_lhcp", Float::class.java),
            currentXbandLnaRhcp = row.get("current_xband_lna_rhcp", Float::class.java),
            currentKabandLnaLhcp = row.get("current_kaband_lna_lhcp", Float::class.java),
            currentKabandLnaRhcp = row.get("current_kaband_lna_rhcp", Float::class.java),
            rssiSbandLnaLhcp = row.get("rssi_sband_lna_lhcp", Float::class.java),
            rssiSbandLnaRhcp = row.get("rssi_sband_lna_rhcp", Float::class.java),
            rssiXbandLnaLhcp = row.get("rssi_xband_lna_lhcp", Float::class.java),
            rssiXbandLnaRhcp = row.get("rssi_xband_lna_rhcp", Float::class.java),
            rssiKabandLnaLhcp = row.get("rssi_kaband_lna_lhcp", Float::class.java),
            rssiKabandLnaRhcp = row.get("rssi_kaband_lna_rhcp", Float::class.java),
            trackingAzimuthTime = row.get("tracking_azimuth_time", Float::class.java),
            trackingCmdAzimuth = row.get("tracking_cmd_azimuth", Float::class.java),
            trackingActualAzimuth = row.get("tracking_actual_azimuth", Float::class.java),
            trackingElevationTime = row.get("tracking_elevation_time", Float::class.java),
            trackingCmdElevation = row.get("tracking_cmd_elevation", Float::class.java),
            trackingActualElevation = row.get("tracking_actual_elevation", Float::class.java),
            trackingTrainTime = row.get("tracking_train_time", Float::class.java),
            trackingCmdTrain = row.get("tracking_cmd_train", Float::class.java),
            trackingActualTrain = row.get("tracking_actual_train", Float::class.java)
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
