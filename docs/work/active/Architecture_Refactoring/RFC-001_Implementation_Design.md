# RFC-001 구현 설계 - Repository 추상화 및 Entity 설계

> **작성일**: 2026-01-14
> **목적**: RFC-001 DB 마이그레이션을 위한 구체적인 구현 설계

---

## 1. 패키지 구조

```
backend/src/main/kotlin/com/gtlsystems/acs_api/
├── entity/                          # Entity 클래스
│   ├── TrackingMasterEntity.kt
│   ├── TrackingDetailEntity.kt
│   ├── RealtimeResultEntity.kt
│   └── IcdRealtimeEntity.kt
│
├── repository/                      # Repository 인터페이스
│   ├── TrackingMasterRepository.kt
│   ├── TrackingDetailRepository.kt
│   ├── RealtimeResultRepository.kt
│   └── IcdRealtimeRepository.kt
│
├── port/                            # 저장소 추상화 (Port 패턴)
│   ├── TrackingDataStoragePort.kt   # 인터페이스
│   └── impl/
│       ├── MemoryTrackingStorage.kt
│       ├── DatabaseTrackingStorage.kt
│       └── HybridTrackingStorage.kt
│
├── mapper/                          # 변환기
│   └── TrackingDataMapper.kt
│
└── config/
    └── DatabaseConfig.kt            # DB 설정
```

---

## 2. Entity 클래스 설계

### 2.1 TrackingMasterEntity

```kotlin
package com.gtlsystems.acs_api.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime

/**
 * 추적 마스터 엔티티
 *
 * 각 패스(추적 세션)별 요약 정보를 저장합니다.
 * 하나의 위성 패스에 대해 8개의 DataType 레코드가 생성됩니다.
 */
@Table("tracking_master")
data class TrackingMasterEntity(
    @Id
    val id: Long? = null,

    @Column("mst_id")
    val mstId: Long,                     // 전역 고유 ID (AtomicLong에서 생성)

    @Column("detail_id")
    val detailId: Int,                   // 패스 인덱스

    @Column("satellite_id")
    val satelliteId: String,             // 위성 카탈로그 번호 (예: "25544")

    @Column("satellite_name")
    val satelliteName: String?,          // 위성 이름 (예: "ISS")

    @Column("tracking_mode")
    val trackingMode: TrackingMode,      // EPHEMERIS | PASS_SCHEDULE

    @Column("data_type")
    val dataType: DataType,              // 8가지 변환 타입

    @Column("start_time")
    val startTime: ZonedDateTime,

    @Column("end_time")
    val endTime: ZonedDateTime,

    @Column("duration")
    val duration: Int?,                  // 초 단위

    @Column("max_elevation")
    val maxElevation: Double?,

    @Column("max_azimuth_rate")
    val maxAzimuthRate: Double?,

    @Column("max_elevation_rate")
    val maxElevationRate: Double?,

    @Column("keyhole_detected")
    val keyholeDetected: Boolean = false,

    @Column("recommended_train_angle")
    val recommendedTrainAngle: Double?,

    @Column("total_points")
    val totalPoints: Int?,

    @Column("created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now()
) {
    /**
     * 추적 모드 열거형
     */
    enum class TrackingMode {
        EPHEMERIS,       // 단일 위성 즉시 추적
        PASS_SCHEDULE    // 다중 위성 스케줄 추적
    }

    /**
     * 데이터 타입 열거형 (8가지 변환 단계)
     */
    enum class DataType {
        ORIGINAL,                           // 원본 (2축)
        AXIS_TRANSFORMED,                   // 축변환 (3축)
        FINAL_TRANSFORMED,                  // 최종 변환 (제한 적용)
        KEYHOLE_AXIS_TRANSFORMED,           // Keyhole 축변환
        KEYHOLE_FINAL_TRANSFORMED,          // Keyhole 최종 변환
        KEYHOLE_OPTIMIZED_AXIS_TRANSFORMED, // Keyhole 최적화 축변환
        KEYHOLE_OPTIMIZED_FINAL_TRANSFORMED,// Keyhole 최적화 최종 변환
        SELECTED                            // 선택된 최종 데이터
    }
}
```

### 2.2 TrackingDetailEntity

```kotlin
package com.gtlsystems.acs_api.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime

/**
 * 추적 상세 엔티티 (이론 궤적)
 *
 * 100ms 간격의 이론적 위성 궤적 데이터를 저장합니다.
 * TimescaleDB Hypertable로 시계열 최적화됩니다.
 */
@Table("tracking_detail")
data class TrackingDetailEntity(
    @Id
    val id: Long? = null,

    @Column("master_id")
    val masterId: Long,                  // tracking_master FK

    @Column("mst_id")
    val mstId: Long,                     // 전역 고유 ID

    @Column("detail_id")
    val detailId: Int,                   // 패스 인덱스

    @Column("data_type")
    val dataType: String,                // 8가지 DataType 문자열

    @Column("timestamp")
    val timestamp: ZonedDateTime,        // 시계열 파티션 키

    @Column("index")
    val index: Int,                      // 데이터 포인트 인덱스

    // 각도 데이터
    @Column("azimuth")
    val azimuth: Double,

    @Column("elevation")
    val elevation: Double,

    @Column("train")
    val train: Double?,

    // 속도 데이터
    @Column("azimuth_rate")
    val azimuthRate: Double?,

    @Column("elevation_rate")
    val elevationRate: Double?,

    // 위성 위치
    @Column("range_km")
    val rangeKm: Double?,

    @Column("altitude_km")
    val altitudeKm: Double?,

    @Column("created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now()
)
```

### 2.3 RealtimeResultEntity

```kotlin
package com.gtlsystems.acs_api.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime

/**
 * 실시간 추적 결과 엔티티 (57개 필드)
 *
 * 실제 추적 중 수집된 CMD/Actual 값과 오차 분석 데이터입니다.
 * 100ms 간격으로 저장되며, TimescaleDB Hypertable로 최적화됩니다.
 */
@Table("realtime_result")
data class RealtimeResultEntity(
    @Id
    val id: Long? = null,

    @Column("master_id")
    val masterId: Long?,                 // tracking_master FK (nullable)

    // 인덱스/시간
    @Column("index")
    val index: Int,

    @Column("theoretical_index")
    val theoreticalIndex: Int?,

    @Column("timestamp")
    val timestamp: ZonedDateTime,        // 시계열 파티션 키

    // ===== 원본 데이터 (변환 전) =====
    @Column("original_azimuth")
    val originalAzimuth: Float?,

    @Column("original_elevation")
    val originalElevation: Float?,

    @Column("original_range")
    val originalRange: Float?,

    @Column("original_altitude")
    val originalAltitude: Float?,

    // ===== 축변환 데이터 =====
    @Column("axis_transformed_azimuth")
    val axisTransformedAzimuth: Float?,

    @Column("axis_transformed_elevation")
    val axisTransformedElevation: Float?,

    @Column("axis_transformed_range")
    val axisTransformedRange: Float?,

    @Column("axis_transformed_altitude")
    val axisTransformedAltitude: Float?,

    // ===== 최종 변환 데이터 =====
    @Column("final_azimuth")
    val finalAzimuth: Float?,

    @Column("final_elevation")
    val finalElevation: Float?,

    @Column("final_range")
    val finalRange: Float?,

    @Column("final_altitude")
    val finalAltitude: Float?,

    // ===== Keyhole 변환 데이터 =====
    @Column("keyhole_final_azimuth")
    val keyholeFinalAzimuth: Float?,

    @Column("keyhole_final_elevation")
    val keyholeFinalElevation: Float?,

    @Column("keyhole_final_range")
    val keyholeFinalRange: Float?,

    @Column("keyhole_final_altitude")
    val keyholeFinalAltitude: Float?,

    // ===== CMD/Actual 데이터 =====
    @Column("cmd_azimuth")
    val cmdAzimuth: Float?,

    @Column("cmd_elevation")
    val cmdElevation: Float?,

    @Column("actual_azimuth")
    val actualAzimuth: Float?,

    @Column("actual_elevation")
    val actualElevation: Float?,

    @Column("tracking_cmd_azimuth")
    val trackingCmdAzimuth: Float?,

    @Column("tracking_actual_azimuth")
    val trackingActualAzimuth: Float?,

    @Column("tracking_cmd_elevation")
    val trackingCmdElevation: Float?,

    @Column("tracking_actual_elevation")
    val trackingActualElevation: Float?,

    @Column("tracking_cmd_train")
    val trackingCmdTrain: Float?,

    @Column("tracking_actual_train")
    val trackingActualTrain: Float?,

    // ===== 시간 데이터 =====
    @Column("elapsed_time_seconds")
    val elapsedTimeSeconds: Float?,

    @Column("tracking_azimuth_time")
    val trackingAzimuthTime: Float?,

    @Column("tracking_elevation_time")
    val trackingElevationTime: Float?,

    @Column("tracking_train_time")
    val trackingTrainTime: Float?,

    // ===== 오차 데이터 =====
    @Column("azimuth_error")
    val azimuthError: Float?,

    @Column("elevation_error")
    val elevationError: Float?,

    @Column("original_to_axis_error")
    val originalToAxisError: Float?,

    @Column("axis_to_final_error")
    val axisToFinalError: Float?,

    @Column("total_transformation_error")
    val totalTransformationError: Float?,

    // ===== 정확도 분석 =====
    @Column("time_accuracy")
    val timeAccuracy: Float?,

    @Column("az_cmd_accuracy")
    val azCmdAccuracy: Float?,

    @Column("az_act_accuracy")
    val azActAccuracy: Float?,

    @Column("az_final_accuracy")
    val azFinalAccuracy: Float?,

    @Column("el_cmd_accuracy")
    val elCmdAccuracy: Float?,

    @Column("el_act_accuracy")
    val elActAccuracy: Float?,

    @Column("el_final_accuracy")
    val elFinalAccuracy: Float?,

    // ===== 메타데이터 =====
    @Column("mst_id")
    val mstId: Long,

    @Column("detail_id")
    val detailId: Int,

    @Column("has_valid_data")
    val hasValidData: Boolean = false,

    @Column("data_source")
    val dataSource: String?,

    @Column("tilt_angle")
    val tiltAngle: Double?,

    @Column("transformation_type")
    val transformationType: String?,

    @Column("is_keyhole")
    val isKeyhole: Boolean = false,

    @Column("final_data_type")
    val finalDataType: String?,

    @Column("has_transformation")
    val hasTransformation: Boolean = false,

    // ===== 보간 정보 =====
    @Column("interpolation_method")
    val interpolationMethod: String?,

    @Column("interpolation_accuracy")
    val interpolationAccuracy: Double?,

    @Column("created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now()
)
```

### 2.4 IcdRealtimeEntity

```kotlin
package com.gtlsystems.acs_api.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime

/**
 * ICD 실시간 데이터 엔티티 (55개 필드)
 *
 * 하드웨어에서 100ms마다 수신되는 UDP 데이터입니다.
 * Primary Key 없이 timestamp만으로 파티셔닝됩니다.
 */
@Table("icd_realtime")
data class IcdRealtimeEntity(
    @Column("timestamp")
    val timestamp: ZonedDateTime,        // 시계열 파티션 키 (PK 대체)

    // ===== 각도 (10개) =====
    @Column("azimuth_angle")
    val azimuthAngle: Float?,

    @Column("elevation_angle")
    val elevationAngle: Float?,

    @Column("train_angle")
    val trainAngle: Float?,

    @Column("servo_driver_azimuth_angle")
    val servoDriverAzimuthAngle: Float?,

    @Column("servo_driver_elevation_angle")
    val servoDriverElevationAngle: Float?,

    @Column("servo_driver_train_angle")
    val servoDriverTrainAngle: Float?,

    @Column("tracking_cmd_azimuth")
    val trackingCmdAzimuth: Float?,

    @Column("tracking_cmd_elevation")
    val trackingCmdElevation: Float?,

    @Column("tracking_cmd_train")
    val trackingCmdTrain: Float?,

    @Column("tracking_actual_azimuth")
    val trackingActualAzimuth: Float?,

    @Column("tracking_actual_elevation")
    val trackingActualElevation: Float?,

    @Column("tracking_actual_train")
    val trackingActualTrain: Float?,

    // ===== 속도 (3개) =====
    @Column("azimuth_speed")
    val azimuthSpeed: Float?,

    @Column("elevation_speed")
    val elevationSpeed: Float?,

    @Column("train_speed")
    val trainSpeed: Float?,

    // ===== 가속도 (6개) =====
    @Column("azimuth_acceleration")
    val azimuthAcceleration: Float?,

    @Column("elevation_acceleration")
    val elevationAcceleration: Float?,

    @Column("train_acceleration")
    val trainAcceleration: Float?,

    @Column("azimuth_max_acceleration")
    val azimuthMaxAcceleration: Float?,

    @Column("elevation_max_acceleration")
    val elevationMaxAcceleration: Float?,

    @Column("train_max_acceleration")
    val trainMaxAcceleration: Float?,

    // ===== 토크 (3개) =====
    @Column("torque_azimuth")
    val torqueAzimuth: Float?,

    @Column("torque_elevation")
    val torqueElevation: Float?,

    @Column("torque_train")
    val torqueTrain: Float?,

    // ===== 환경 (4개) =====
    @Column("wind_speed")
    val windSpeed: Float?,

    @Column("wind_direction")
    val windDirection: Short?,

    @Column("rtd_one")
    val rtdOne: Float?,

    @Column("rtd_two")
    val rtdTwo: Float?,

    // ===== 상태 비트 (15개) =====
    @Column("mode_status_bits")
    val modeStatusBits: String?,

    @Column("main_board_protocol_status")
    val mainBoardProtocolStatus: String?,

    @Column("main_board_status")
    val mainBoardStatus: String?,

    @Column("main_board_mc_onoff")
    val mainBoardMcOnoff: String?,

    @Column("main_board_reserve")
    val mainBoardReserve: String?,

    @Column("azimuth_servo_status")
    val azimuthServoStatus: String?,

    @Column("azimuth_board_status")
    val azimuthBoardStatus: String?,

    @Column("elevation_servo_status")
    val elevationServoStatus: String?,

    @Column("elevation_board_status")
    val elevationBoardStatus: String?,

    @Column("train_servo_status")
    val trainServoStatus: String?,

    @Column("train_board_status")
    val trainBoardStatus: String?,

    @Column("feed_board_etc_status")
    val feedBoardEtcStatus: String?,

    @Column("feed_s_board_status")
    val feedSBoardStatus: String?,

    @Column("feed_x_board_status")
    val feedXBoardStatus: String?,

    @Column("feed_ka_board_status")
    val feedKaBoardStatus: String?,

    // ===== LNA 전류 (6개) =====
    @Column("current_sband_lna_lhcp")
    val currentSbandLnaLhcp: Float?,

    @Column("current_sband_lna_rhcp")
    val currentSbandLnaRhcp: Float?,

    @Column("current_xband_lna_lhcp")
    val currentXbandLnaLhcp: Float?,

    @Column("current_xband_lna_rhcp")
    val currentXbandLnaRhcp: Float?,

    @Column("current_kaband_lna_lhcp")
    val currentKabandLnaLhcp: Float?,

    @Column("current_kaband_lna_rhcp")
    val currentKabandLnaRhcp: Float?,

    // ===== RSSI (4개) =====
    @Column("rssi_sband_lna_lhcp")
    val rssiSbandLnaLhcp: Float?,

    @Column("rssi_sband_lna_rhcp")
    val rssiSbandLnaRhcp: Float?,

    @Column("rssi_xband_lna_lhcp")
    val rssiXbandLnaLhcp: Float?,

    @Column("rssi_xband_lna_rhcp")
    val rssiXbandLnaRhcp: Float?,

    // ===== 추적 시간 (3개) =====
    @Column("tracking_azimuth_time")
    val trackingAzimuthTime: Float?,

    @Column("tracking_elevation_time")
    val trackingElevationTime: Float?,

    @Column("tracking_train_time")
    val trackingTrainTime: Float?
)
```

---

## 3. Repository 인터페이스

### 3.1 TrackingMasterRepository

```kotlin
package com.gtlsystems.acs_api.repository

import com.gtlsystems.acs_api.entity.TrackingMasterEntity
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface TrackingMasterRepository : CrudRepository<TrackingMasterEntity, Long> {

    /**
     * 날짜 범위로 추적 마스터 조회
     */
    fun findByStartTimeBetween(
        startFrom: ZonedDateTime,
        startTo: ZonedDateTime
    ): List<TrackingMasterEntity>

    /**
     * 위성 ID와 날짜로 조회
     */
    fun findBySatelliteIdAndStartTimeBetween(
        satelliteId: String,
        startFrom: ZonedDateTime,
        startTo: ZonedDateTime
    ): List<TrackingMasterEntity>

    /**
     * MstId와 DataType으로 조회
     */
    fun findByMstIdAndDataType(
        mstId: Long,
        dataType: TrackingMasterEntity.DataType
    ): TrackingMasterEntity?

    /**
     * 추적 모드별 조회
     */
    fun findByTrackingModeAndStartTimeBetween(
        trackingMode: TrackingMasterEntity.TrackingMode,
        startFrom: ZonedDateTime,
        startTo: ZonedDateTime
    ): List<TrackingMasterEntity>

    /**
     * 30일 이전 데이터 삭제
     */
    @Query("DELETE FROM tracking_master WHERE start_time < :cutoffDate")
    fun deleteByStartTimeBefore(cutoffDate: ZonedDateTime): Int
}
```

### 3.2 TrackingDetailRepository

```kotlin
package com.gtlsystems.acs_api.repository

import com.gtlsystems.acs_api.entity.TrackingDetailEntity
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface TrackingDetailRepository : CrudRepository<TrackingDetailEntity, Long> {

    /**
     * Master ID로 상세 데이터 조회
     */
    fun findByMasterId(masterId: Long): List<TrackingDetailEntity>

    /**
     * MstId와 DetailId로 조회
     */
    fun findByMstIdAndDetailId(mstId: Long, detailId: Int): List<TrackingDetailEntity>

    /**
     * MstId, DetailId, DataType으로 조회
     */
    fun findByMstIdAndDetailIdAndDataType(
        mstId: Long,
        detailId: Int,
        dataType: String
    ): List<TrackingDetailEntity>

    /**
     * 배치 INSERT (성능 최적화)
     */
    @Query("""
        INSERT INTO tracking_detail
        (master_id, mst_id, detail_id, data_type, timestamp, index,
         azimuth, elevation, train, azimuth_rate, elevation_rate,
         range_km, altitude_km, created_at)
        VALUES (:masterId, :mstId, :detailId, :dataType, :timestamp, :index,
                :azimuth, :elevation, :train, :azimuthRate, :elevationRate,
                :rangeKm, :altitudeKm, NOW())
    """)
    fun insertBatch(entities: List<TrackingDetailEntity>)
}
```

### 3.3 RealtimeResultRepository

```kotlin
package com.gtlsystems.acs_api.repository

import com.gtlsystems.acs_api.entity.RealtimeResultEntity
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface RealtimeResultRepository : CrudRepository<RealtimeResultEntity, Long> {

    /**
     * Master ID로 실시간 결과 조회
     */
    fun findByMasterId(masterId: Long): List<RealtimeResultEntity>

    /**
     * MstId로 조회
     */
    fun findByMstId(mstId: Long): List<RealtimeResultEntity>

    /**
     * 시간 범위로 조회
     */
    fun findByTimestampBetween(
        from: ZonedDateTime,
        to: ZonedDateTime
    ): List<RealtimeResultEntity>

    /**
     * MstId와 시간 범위로 조회
     */
    fun findByMstIdAndTimestampBetween(
        mstId: Long,
        from: ZonedDateTime,
        to: ZonedDateTime
    ): List<RealtimeResultEntity>

    /**
     * 유효한 데이터만 조회
     */
    fun findByMstIdAndHasValidDataTrue(mstId: Long): List<RealtimeResultEntity>

    /**
     * 통계 조회 (평균 오차)
     */
    @Query("""
        SELECT
            AVG(azimuth_error) as avg_azimuth_error,
            AVG(elevation_error) as avg_elevation_error,
            COUNT(*) as total_count
        FROM realtime_result
        WHERE mst_id = :mstId AND has_valid_data = true
    """)
    fun getStatsByMstId(mstId: Long): Map<String, Any>
}
```

---

## 4. 저장소 추상화 (Port 패턴)

### 4.1 TrackingDataStoragePort

```kotlin
package com.gtlsystems.acs_api.port

import java.time.ZonedDateTime

/**
 * 추적 데이터 저장소 포트 (인터페이스)
 *
 * 저장소 구현체를 추상화하여 메모리/DB/하이브리드 전환을 용이하게 합니다.
 */
interface TrackingDataStoragePort {

    /**
     * MST 데이터 저장
     */
    suspend fun saveMst(mstData: List<Map<String, Any?>>)

    /**
     * DTL 데이터 저장
     */
    suspend fun saveDtl(dtlData: List<Map<String, Any?>>)

    /**
     * 실시간 추적 데이터 저장 (배치)
     */
    suspend fun saveRealtimeResult(data: List<Map<String, Any?>>)

    /**
     * ICD 데이터 저장 (배치)
     */
    suspend fun saveIcdRealtime(data: List<Map<String, Any?>>)

    // ===== 조회 =====

    /**
     * MST 데이터 조회
     */
    fun getMstByMstIdAndDataType(mstId: Long, dataType: String): Map<String, Any?>?

    /**
     * DTL 데이터 조회
     */
    fun getDtlByMstIdAndDetailIdAndDataType(
        mstId: Long,
        detailId: Int,
        dataType: String
    ): List<Map<String, Any?>>

    /**
     * 전체 MST 조회
     */
    fun getAllMst(): List<Map<String, Any?>>

    /**
     * 날짜 범위 조회
     */
    fun getMstByDateRange(from: ZonedDateTime, to: ZonedDateTime): List<Map<String, Any?>>

    // ===== 관리 =====

    /**
     * 데이터 초기화
     */
    fun clear()

    /**
     * 저장소 모드
     */
    fun getStorageMode(): StorageMode

    enum class StorageMode {
        MEMORY,
        DATABASE,
        HYBRID
    }
}
```

### 4.2 MemoryTrackingStorage

```kotlin
package com.gtlsystems.acs_api.port.impl

import com.gtlsystems.acs_api.port.TrackingDataStoragePort
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 메모리 기반 추적 데이터 저장소
 *
 * 기존 EphemerisService, PassScheduleService의 저장 로직을 그대로 유지합니다.
 */
@Component
class MemoryTrackingStorage : TrackingDataStoragePort {

    private val mstStorage = mutableListOf<Map<String, Any?>>()
    private val dtlStorage = mutableListOf<Map<String, Any?>>()
    private val realtimeStorage = mutableListOf<Map<String, Any?>>()

    override suspend fun saveMst(mstData: List<Map<String, Any?>>) {
        synchronized(mstStorage) {
            mstStorage.addAll(mstData)
        }
    }

    override suspend fun saveDtl(dtlData: List<Map<String, Any?>>) {
        synchronized(dtlStorage) {
            dtlStorage.addAll(dtlData)
        }
    }

    override suspend fun saveRealtimeResult(data: List<Map<String, Any?>>) {
        synchronized(realtimeStorage) {
            realtimeStorage.addAll(data)
        }
    }

    override suspend fun saveIcdRealtime(data: List<Map<String, Any?>>) {
        // 메모리 모드에서는 ICD 데이터 저장하지 않음 (최신 값만 DataStoreService에서 유지)
    }

    override fun getMstByMstIdAndDataType(mstId: Long, dataType: String): Map<String, Any?>? {
        return synchronized(mstStorage) {
            mstStorage.find {
                (it["MstId"] as? Number)?.toLong() == mstId && it["DataType"] == dataType
            }
        }
    }

    override fun getDtlByMstIdAndDetailIdAndDataType(
        mstId: Long,
        detailId: Int,
        dataType: String
    ): List<Map<String, Any?>> {
        return synchronized(dtlStorage) {
            dtlStorage.filter {
                (it["MstId"] as? Number)?.toLong() == mstId &&
                (it["DetailId"] as? Number)?.toInt() == detailId &&
                it["DataType"] == dataType
            }
        }
    }

    override fun getAllMst(): List<Map<String, Any?>> {
        return synchronized(mstStorage) {
            mstStorage.toList()
        }
    }

    override fun getMstByDateRange(from: ZonedDateTime, to: ZonedDateTime): List<Map<String, Any?>> {
        return synchronized(mstStorage) {
            mstStorage.filter { mst ->
                val startTime = mst["StartTime"] as? ZonedDateTime
                startTime != null && !startTime.isBefore(from) && !startTime.isAfter(to)
            }
        }
    }

    override fun clear() {
        synchronized(mstStorage) { mstStorage.clear() }
        synchronized(dtlStorage) { dtlStorage.clear() }
        synchronized(realtimeStorage) { realtimeStorage.clear() }
    }

    override fun getStorageMode() = TrackingDataStoragePort.StorageMode.MEMORY
}
```

### 4.3 HybridTrackingStorage

```kotlin
package com.gtlsystems.acs_api.port.impl

import com.gtlsystems.acs_api.port.TrackingDataStoragePort
import com.gtlsystems.acs_api.repository.*
import com.gtlsystems.acs_api.mapper.TrackingDataMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

/**
 * 하이브리드 추적 데이터 저장소
 *
 * 메모리와 DB를 병행하여 저장합니다.
 * - 메모리: 실시간 조회 (기존 동작 유지)
 * - DB: 영속성 보장 (비동기 저장)
 */
@Component
class HybridTrackingStorage(
    private val memoryStorage: MemoryTrackingStorage,
    private val trackingMasterRepository: TrackingMasterRepository,
    private val trackingDetailRepository: TrackingDetailRepository,
    private val realtimeResultRepository: RealtimeResultRepository,
    private val icdRealtimeRepository: IcdRealtimeRepository,
    private val mapper: TrackingDataMapper,
    @Value("\${acs.storage.mode:hybrid}") private val storageMode: String
) : TrackingDataStoragePort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun saveMst(mstData: List<Map<String, Any?>>) {
        // 1. 메모리 저장 (즉시)
        memoryStorage.saveMst(mstData)

        // 2. DB 저장 (비동기)
        if (storageMode != "memory") {
            withContext(Dispatchers.IO) {
                try {
                    val entities = mstData.map { mapper.toMasterEntity(it) }
                    trackingMasterRepository.saveAll(entities)
                    logger.debug("DB에 MST 데이터 저장 완료: ${entities.size}건")
                } catch (e: Exception) {
                    logger.error("DB MST 저장 실패: ${e.message}", e)
                }
            }
        }
    }

    override suspend fun saveDtl(dtlData: List<Map<String, Any?>>) {
        // 1. 메모리 저장 (즉시)
        memoryStorage.saveDtl(dtlData)

        // 2. DB 저장 (비동기)
        if (storageMode != "memory") {
            withContext(Dispatchers.IO) {
                try {
                    val entities = dtlData.map { mapper.toDetailEntity(it) }
                    trackingDetailRepository.saveAll(entities)
                    logger.debug("DB에 DTL 데이터 저장 완료: ${entities.size}건")
                } catch (e: Exception) {
                    logger.error("DB DTL 저장 실패: ${e.message}", e)
                }
            }
        }
    }

    override suspend fun saveRealtimeResult(data: List<Map<String, Any?>>) {
        // 1. 메모리 저장 (즉시)
        memoryStorage.saveRealtimeResult(data)

        // 2. DB 저장 (비동기)
        if (storageMode != "memory") {
            withContext(Dispatchers.IO) {
                try {
                    val entities = data.map { mapper.toRealtimeResultEntity(it) }
                    realtimeResultRepository.saveAll(entities)
                    logger.debug("DB에 실시간 결과 저장 완료: ${entities.size}건")
                } catch (e: Exception) {
                    logger.error("DB 실시간 결과 저장 실패: ${e.message}", e)
                }
            }
        }
    }

    override suspend fun saveIcdRealtime(data: List<Map<String, Any?>>) {
        // ICD 데이터는 DB에만 저장 (메모리에는 DataStoreService가 관리)
        if (storageMode != "memory") {
            withContext(Dispatchers.IO) {
                try {
                    val entities = data.map { mapper.toIcdRealtimeEntity(it) }
                    icdRealtimeRepository.saveAll(entities)
                    logger.debug("DB에 ICD 데이터 저장 완료: ${entities.size}건")
                } catch (e: Exception) {
                    logger.error("DB ICD 저장 실패: ${e.message}", e)
                }
            }
        }
    }

    // ===== 조회는 메모리 우선 =====

    override fun getMstByMstIdAndDataType(mstId: Long, dataType: String): Map<String, Any?>? {
        // 메모리에서 먼저 조회
        val memoryResult = memoryStorage.getMstByMstIdAndDataType(mstId, dataType)
        if (memoryResult != null) return memoryResult

        // 메모리에 없으면 DB 조회
        if (storageMode != "memory") {
            return try {
                val entity = trackingMasterRepository.findByMstIdAndDataType(
                    mstId,
                    TrackingMasterEntity.DataType.valueOf(dataType.uppercase())
                )
                entity?.let { mapper.toMap(it) }
            } catch (e: Exception) {
                logger.error("DB MST 조회 실패: ${e.message}")
                null
            }
        }
        return null
    }

    override fun getDtlByMstIdAndDetailIdAndDataType(
        mstId: Long,
        detailId: Int,
        dataType: String
    ): List<Map<String, Any?>> {
        // 메모리에서 먼저 조회
        val memoryResult = memoryStorage.getDtlByMstIdAndDetailIdAndDataType(mstId, detailId, dataType)
        if (memoryResult.isNotEmpty()) return memoryResult

        // 메모리에 없으면 DB 조회
        if (storageMode != "memory") {
            return try {
                val entities = trackingDetailRepository.findByMstIdAndDetailIdAndDataType(mstId, detailId, dataType)
                entities.map { mapper.toMap(it) }
            } catch (e: Exception) {
                logger.error("DB DTL 조회 실패: ${e.message}")
                emptyList()
            }
        }
        return emptyList()
    }

    override fun getAllMst(): List<Map<String, Any?>> {
        return memoryStorage.getAllMst()
    }

    override fun getMstByDateRange(from: ZonedDateTime, to: ZonedDateTime): List<Map<String, Any?>> {
        // 오늘 데이터는 메모리에서
        val today = ZonedDateTime.now().toLocalDate()
        if (from.toLocalDate() == today) {
            return memoryStorage.getMstByDateRange(from, to)
        }

        // 과거 데이터는 DB에서
        if (storageMode != "memory") {
            return try {
                val entities = trackingMasterRepository.findByStartTimeBetween(from, to)
                entities.map { mapper.toMap(it) }
            } catch (e: Exception) {
                logger.error("DB 날짜 범위 조회 실패: ${e.message}")
                emptyList()
            }
        }
        return emptyList()
    }

    override fun clear() {
        memoryStorage.clear()
    }

    override fun getStorageMode() = TrackingDataStoragePort.StorageMode.HYBRID
}
```

---

## 5. 설정 파일

### 5.1 application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/acs
    username: acs
    password: ${DB_PASSWORD:acs_password}
    driver-class-name: org.postgresql.Driver

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

acs:
  storage:
    mode: hybrid  # memory | database | hybrid
    batch-size: 10
    save-interval: 1000  # ms

  retention:
    days: 30
    cleanup-cron: "0 0 2 * * ?"  # 매일 02:00에 정리
```

### 5.2 schema.sql

```sql
-- TimescaleDB 확장 활성화
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- tracking_master 테이블
CREATE TABLE IF NOT EXISTS tracking_master (
    id                      BIGSERIAL PRIMARY KEY,
    mst_id                  BIGINT NOT NULL,
    detail_id               INTEGER NOT NULL,
    satellite_id            VARCHAR(20) NOT NULL,
    satellite_name          VARCHAR(100),
    tracking_mode           VARCHAR(20) NOT NULL,
    data_type               VARCHAR(50) NOT NULL,
    start_time              TIMESTAMPTZ NOT NULL,
    end_time                TIMESTAMPTZ NOT NULL,
    duration                INTEGER,
    max_elevation           DOUBLE PRECISION,
    max_azimuth_rate        DOUBLE PRECISION,
    max_elevation_rate      DOUBLE PRECISION,
    keyhole_detected        BOOLEAN DEFAULT FALSE,
    recommended_train_angle DOUBLE PRECISION,
    total_points            INTEGER,
    created_at              TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uk_tracking_master UNIQUE (mst_id, data_type, tracking_mode)
);

CREATE INDEX IF NOT EXISTS idx_tm_satellite ON tracking_master(satellite_id);
CREATE INDEX IF NOT EXISTS idx_tm_start_time ON tracking_master(start_time DESC);
CREATE INDEX IF NOT EXISTS idx_tm_mode ON tracking_master(tracking_mode);
CREATE INDEX IF NOT EXISTS idx_tm_mst_datatype ON tracking_master(mst_id, data_type);

-- tracking_detail 테이블
CREATE TABLE IF NOT EXISTS tracking_detail (
    id                      BIGSERIAL,
    master_id               BIGINT REFERENCES tracking_master(id) ON DELETE CASCADE,
    mst_id                  BIGINT NOT NULL,
    detail_id               INTEGER NOT NULL,
    data_type               VARCHAR(50) NOT NULL,
    timestamp               TIMESTAMPTZ NOT NULL,
    index                   INTEGER NOT NULL,
    azimuth                 DOUBLE PRECISION NOT NULL,
    elevation               DOUBLE PRECISION NOT NULL,
    train                   DOUBLE PRECISION,
    azimuth_rate            DOUBLE PRECISION,
    elevation_rate          DOUBLE PRECISION,
    range_km                DOUBLE PRECISION,
    altitude_km             DOUBLE PRECISION,
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- Hypertable 변환
SELECT create_hypertable('tracking_detail', 'timestamp', if_not_exists => TRUE);
SELECT set_chunk_time_interval('tracking_detail', INTERVAL '7 days');

-- 압축 정책
ALTER TABLE tracking_detail SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'master_id'
);
SELECT add_compression_policy('tracking_detail', INTERVAL '7 days', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_td_master ON tracking_detail(master_id);
CREATE INDEX IF NOT EXISTS idx_td_master_timestamp ON tracking_detail(master_id, timestamp);

-- realtime_result 테이블 (확장된 버전)
CREATE TABLE IF NOT EXISTS realtime_result (
    id                          BIGSERIAL,
    master_id                   BIGINT REFERENCES tracking_master(id) ON DELETE CASCADE,
    index                       INTEGER NOT NULL,
    theoretical_index           INTEGER,
    timestamp                   TIMESTAMPTZ NOT NULL,

    -- 원본/축변환/최종 변환 데이터
    original_azimuth            REAL,
    original_elevation          REAL,
    original_range              REAL,
    original_altitude           REAL,
    axis_transformed_azimuth    REAL,
    axis_transformed_elevation  REAL,
    axis_transformed_range      REAL,
    axis_transformed_altitude   REAL,
    final_azimuth               REAL,
    final_elevation             REAL,
    final_range                 REAL,
    final_altitude              REAL,

    -- Keyhole 변환 데이터
    keyhole_final_azimuth       REAL,
    keyhole_final_elevation     REAL,
    keyhole_final_range         REAL,
    keyhole_final_altitude      REAL,

    -- CMD/Actual
    cmd_azimuth                 REAL,
    cmd_elevation               REAL,
    actual_azimuth              REAL,
    actual_elevation            REAL,
    tracking_cmd_azimuth        REAL,
    tracking_actual_azimuth     REAL,
    tracking_cmd_elevation      REAL,
    tracking_actual_elevation   REAL,
    tracking_cmd_train          REAL,
    tracking_actual_train       REAL,

    -- 시간
    elapsed_time_seconds        REAL,
    tracking_azimuth_time       REAL,
    tracking_elevation_time     REAL,
    tracking_train_time         REAL,

    -- 오차
    azimuth_error               REAL,
    elevation_error             REAL,
    original_to_axis_error      REAL,
    axis_to_final_error         REAL,
    total_transformation_error  REAL,

    -- 정확도 분석
    time_accuracy               REAL,
    az_cmd_accuracy             REAL,
    az_act_accuracy             REAL,
    az_final_accuracy           REAL,
    el_cmd_accuracy             REAL,
    el_act_accuracy             REAL,
    el_final_accuracy           REAL,

    -- 메타데이터
    mst_id                      BIGINT NOT NULL,
    detail_id                   INTEGER NOT NULL,
    has_valid_data              BOOLEAN DEFAULT FALSE,
    data_source                 VARCHAR(50),
    tilt_angle                  DOUBLE PRECISION,
    transformation_type         VARCHAR(20),
    is_keyhole                  BOOLEAN DEFAULT FALSE,
    final_data_type             VARCHAR(50),
    has_transformation          BOOLEAN DEFAULT FALSE,

    -- 보간 정보
    interpolation_method        VARCHAR(20),
    interpolation_accuracy      DOUBLE PRECISION,

    created_at                  TIMESTAMPTZ DEFAULT NOW()
);

-- Hypertable 변환
SELECT create_hypertable('realtime_result', 'timestamp', if_not_exists => TRUE);
SELECT set_chunk_time_interval('realtime_result', INTERVAL '1 day');

-- 압축 정책
ALTER TABLE realtime_result SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'master_id, mst_id'
);
SELECT add_compression_policy('realtime_result', INTERVAL '7 days', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_rr_master ON realtime_result(master_id);
CREATE INDEX IF NOT EXISTS idx_rr_mstid ON realtime_result(mst_id);
CREATE INDEX IF NOT EXISTS idx_rr_timestamp ON realtime_result(timestamp DESC);

-- icd_realtime 테이블
CREATE TABLE IF NOT EXISTS icd_realtime (
    timestamp                       TIMESTAMPTZ NOT NULL,

    -- 각도
    azimuth_angle                   REAL,
    elevation_angle                 REAL,
    train_angle                     REAL,
    servo_driver_azimuth_angle      REAL,
    servo_driver_elevation_angle    REAL,
    servo_driver_train_angle        REAL,
    tracking_cmd_azimuth            REAL,
    tracking_cmd_elevation          REAL,
    tracking_cmd_train              REAL,
    tracking_actual_azimuth         REAL,
    tracking_actual_elevation       REAL,
    tracking_actual_train           REAL,

    -- 속도/가속도/토크
    azimuth_speed                   REAL,
    elevation_speed                 REAL,
    train_speed                     REAL,
    azimuth_acceleration            REAL,
    elevation_acceleration          REAL,
    train_acceleration              REAL,
    azimuth_max_acceleration        REAL,
    elevation_max_acceleration      REAL,
    train_max_acceleration          REAL,
    torque_azimuth                  REAL,
    torque_elevation                REAL,
    torque_train                    REAL,

    -- 환경
    wind_speed                      REAL,
    wind_direction                  SMALLINT,
    rtd_one                         REAL,
    rtd_two                         REAL,

    -- 상태 비트
    mode_status_bits                VARCHAR(32),
    main_board_protocol_status      VARCHAR(32),
    main_board_status               VARCHAR(32),
    main_board_mc_onoff             VARCHAR(32),
    main_board_reserve              VARCHAR(32),
    azimuth_servo_status            VARCHAR(32),
    azimuth_board_status            VARCHAR(32),
    elevation_servo_status          VARCHAR(32),
    elevation_board_status          VARCHAR(32),
    train_servo_status              VARCHAR(32),
    train_board_status              VARCHAR(32),
    feed_board_etc_status           VARCHAR(32),
    feed_s_board_status             VARCHAR(32),
    feed_x_board_status             VARCHAR(32),
    feed_ka_board_status            VARCHAR(32),

    -- LNA/RSSI
    current_sband_lna_lhcp          REAL,
    current_sband_lna_rhcp          REAL,
    current_xband_lna_lhcp          REAL,
    current_xband_lna_rhcp          REAL,
    current_kaband_lna_lhcp         REAL,
    current_kaband_lna_rhcp         REAL,
    rssi_sband_lna_lhcp             REAL,
    rssi_sband_lna_rhcp             REAL,
    rssi_xband_lna_lhcp             REAL,
    rssi_xband_lna_rhcp             REAL,

    -- 추적 시간
    tracking_azimuth_time           REAL,
    tracking_elevation_time         REAL,
    tracking_train_time             REAL
);

-- Hypertable 변환 (PK 없음)
SELECT create_hypertable('icd_realtime', 'timestamp', if_not_exists => TRUE);
SELECT set_chunk_time_interval('icd_realtime', INTERVAL '1 day');

-- 압축 정책
ALTER TABLE icd_realtime SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = ''
);
SELECT add_compression_policy('icd_realtime', INTERVAL '7 days', if_not_exists => TRUE);

-- 보관 정책 (30일)
SELECT add_retention_policy('icd_realtime', INTERVAL '30 days', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_icd_timestamp ON icd_realtime(timestamp DESC);
```

---

## 6. 마이그레이션 체크리스트

### Phase 1: 인프라 준비

- [ ] PostgreSQL 16 설치
- [ ] TimescaleDB 확장 설치
- [ ] schema.sql 실행
- [ ] Spring Data JDBC 의존성 추가
- [ ] application.yml 설정

### Phase 2: Repository 추상화

- [ ] Entity 클래스 4개 작성
- [ ] Repository 인터페이스 4개 작성
- [ ] TrackingDataMapper 작성
- [ ] TrackingDataStoragePort 인터페이스 작성
- [ ] MemoryTrackingStorage 구현
- [ ] HybridTrackingStorage 구현

### Phase 3: 병행 저장

- [ ] EphemerisService에 HybridStorage 주입
- [ ] PassScheduleService에 HybridStorage 주입
- [ ] BatchStorageManager 수정
- [ ] 저장 검증 테스트

### Phase 4: 조회 전환

- [ ] 과거 이력 조회 API
- [ ] CSV 내보내기 API
- [ ] TrackingDataLoader 구현

### Phase 5: 최적화

- [ ] 메모리 저장소 축소
- [ ] 보관 정책 스케줄러
- [ ] 성능 검증

---

**작성자**: Claude (Architect Agent)
**검토자**: -
**승인일**: -
