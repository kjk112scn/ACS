# RFC-001: 데이터베이스 전략

> **버전**: 1.1.0 | **작성일**: 2026-01-13
> **상태**: 검토 완료 (Reviewed)
> **우선순위**: P0 (Critical)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.1.0 | 2026-01-13 | 전문가 검토 반영: 타입 수정(mst_id→BIGINT, detail_id→INTEGER), data_type 필수화, 복합 인덱스 추가, 청크 크기 설정, 압축 정책 추가 |
| 1.0.0 | 2026-01-13 | 초기 작성 |

---

## 1. 배경 (Context)

### 왜 이 변경이 필요한가?

현재 ACS 시스템의 모든 추적 데이터는 메모리(ConcurrentHashMap)에 저장됩니다.

**문제점**:
1. 서버 재시작 시 모든 데이터 소실
2. 다른 위성 추적 시작하면 이전 추적 데이터 사라짐
3. 과거 추적 이력 조회 불가
4. 데이터 분석/백업 불가

**요구사항**:
- 추적 데이터 30일 보관
- 과거 이력 조회 및 다운로드
- 서버 재시작 시 오늘 추적 예정 위성 데이터 복원

---

## 2. 현재 상태 (Current State)

### 2.1 메모리 저장소 현황

```kotlin
// EphemerisService.kt
private val ephemerisTrackMstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()
private val ephemerisTrackDtlStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()

// PassScheduleService.kt
private val passScheduleTrackMstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()
private val passScheduleTrackDtlStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()
private val selectedTrackMstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()
```

### 2.2 데이터 흐름

```
TLE 데이터 → SatelliteTrackingProcessor → Map<String, Any?> → 메모리 저장
                                                              ↓
                                                         FE 조회
                                                              ↓
                                                    서버 재시작 시 소실 ❌
```

### 2.3 ICD 실시간 데이터

```kotlin
// UdpFwICDService.kt
// 100ms마다 55개 필드 수신 → PushData.READ_DATA에 저장 (단일 값만 유지)
// 히스토리 없음
```

---

## 3. 제안 (Proposal)

### 3.1 기술 스택

| 구성요소 | 선택 | 이유 |
|---------|------|------|
| DBMS | PostgreSQL 16 | 안정성, 성능, 무료 |
| 확장 | TimescaleDB | 시계열 최적화, 자동 압축/삭제 |
| 드라이버 | Spring Data JDBC | 단순, 안정적, TimescaleDB 완전 지원 |

### 3.2 테이블 설계

#### 3.2.1 tracking_master (패스별 요약)

```sql
CREATE TABLE tracking_master (
    id                      BIGSERIAL PRIMARY KEY,
    mst_id                  BIGINT NOT NULL,           -- 코드와 일치 (Long)
    detail_id               INTEGER NOT NULL,          -- 코드와 일치 (Int)
    satellite_id            VARCHAR(20) NOT NULL,
    satellite_name          VARCHAR(100),
    tracking_mode           VARCHAR(20) NOT NULL,      -- 'EPHEMERIS' | 'PASS_SCHEDULE'
    data_type               VARCHAR(50) NOT NULL,      -- 'original', 'axisTransformed', etc.

    -- 시간 정보
    start_time              TIMESTAMPTZ NOT NULL,
    end_time                TIMESTAMPTZ NOT NULL,
    duration                INTEGER,                   -- 초 단위

    -- 각도 정보 (Peak 값)
    max_elevation           DOUBLE PRECISION,
    max_azimuth_rate        DOUBLE PRECISION,
    max_elevation_rate      DOUBLE PRECISION,
    keyhole_detected        BOOLEAN DEFAULT FALSE,     -- 코드의 IsKeyhole 매핑

    -- 추가 메타데이터
    recommended_train_angle DOUBLE PRECISION,          -- 코드의 RecommendedTrainAngle
    total_points            INTEGER,

    -- 시스템 필드
    created_at              TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uk_tracking_master UNIQUE (mst_id, data_type, tracking_mode)
);

-- 인덱스
CREATE INDEX idx_tm_satellite ON tracking_master(satellite_id);
CREATE INDEX idx_tm_start_time ON tracking_master(start_time DESC);
CREATE INDEX idx_tm_mode ON tracking_master(tracking_mode);
CREATE INDEX idx_tm_mst_datatype ON tracking_master(mst_id, data_type);  -- 복합 인덱스
CREATE INDEX idx_tm_mode_satellite_time ON tracking_master(tracking_mode, satellite_id, start_time DESC);
```

#### 3.2.2 tracking_detail (이론 궤적)

```sql
CREATE TABLE tracking_detail (
    id                      BIGSERIAL PRIMARY KEY,
    master_id               BIGINT REFERENCES tracking_master(id) ON DELETE CASCADE,
    detail_id               INTEGER NOT NULL,          -- 코드와 일치 (Int)
    data_type               VARCHAR(50) NOT NULL,      -- 8가지 DataType

    -- 시간
    timestamp               TIMESTAMPTZ NOT NULL,
    index                   INTEGER NOT NULL,

    -- 각도
    azimuth                 DOUBLE PRECISION NOT NULL,
    elevation               DOUBLE PRECISION NOT NULL,
    train                   DOUBLE PRECISION,

    -- 속도
    azimuth_rate            DOUBLE PRECISION,
    elevation_rate          DOUBLE PRECISION,

    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- Hypertable 변환 (시계열 최적화)
SELECT create_hypertable('tracking_detail', 'timestamp');

-- 청크 크기 설정 (7일 단위)
SELECT set_chunk_time_interval('tracking_detail', INTERVAL '7 days');

-- 압축 정책 (7일 후 압축)
ALTER TABLE tracking_detail SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'master_id'
);
SELECT add_compression_policy('tracking_detail', INTERVAL '7 days');

-- 인덱스
CREATE INDEX idx_td_master ON tracking_detail(master_id);
CREATE INDEX idx_td_master_timestamp ON tracking_detail(master_id, timestamp);
```

#### 3.2.3 realtime_result (실측 추적 결과)

```sql
CREATE TABLE realtime_result (
    id                      BIGSERIAL PRIMARY KEY,
    master_id               BIGINT REFERENCES tracking_master(id) ON DELETE CASCADE,

    -- 인덱스/시간
    index                   INTEGER NOT NULL,
    theoretical_index       INTEGER,
    timestamp               TIMESTAMPTZ NOT NULL,

    -- 원본 각도 (2축)
    original_azimuth        DOUBLE PRECISION,
    original_elevation      DOUBLE PRECISION,

    -- 변환된 각도 (3축)
    transformed_azimuth     DOUBLE PRECISION,
    transformed_elevation   DOUBLE PRECISION,
    transformed_train       DOUBLE PRECISION,

    -- 최종 각도 (제한 적용)
    final_azimuth           DOUBLE PRECISION,
    final_elevation         DOUBLE PRECISION,
    final_train             DOUBLE PRECISION,

    -- 실제 측정값
    actual_azimuth          DOUBLE PRECISION,
    actual_elevation        DOUBLE PRECISION,
    actual_train            DOUBLE PRECISION,

    -- 오차
    azimuth_error           DOUBLE PRECISION,
    elevation_error         DOUBLE PRECISION,
    train_error             DOUBLE PRECISION,
    total_error             DOUBLE PRECISION,

    -- 속도
    azimuth_rate            DOUBLE PRECISION,
    elevation_rate          DOUBLE PRECISION,
    train_rate              DOUBLE PRECISION,

    -- 가속도
    azimuth_acceleration    DOUBLE PRECISION,
    elevation_acceleration  DOUBLE PRECISION,
    train_acceleration      DOUBLE PRECISION,

    -- 상태
    keyhole_active          BOOLEAN DEFAULT FALSE,
    keyhole_optimized       BOOLEAN DEFAULT FALSE,
    tracking_quality        VARCHAR(20),           -- 'EXCELLENT', 'GOOD', 'POOR'

    -- 보간 정보
    interpolation_type      VARCHAR(20),
    interpolation_accuracy  DOUBLE PRECISION,

    -- 위성 위치 (참조)
    satellite_range         DOUBLE PRECISION,
    satellite_altitude      DOUBLE PRECISION,
    satellite_velocity      DOUBLE PRECISION,

    -- 추가 필드 (57개 중 나머지)
    cmd_azimuth             DOUBLE PRECISION,
    cmd_elevation           DOUBLE PRECISION,
    cmd_train               DOUBLE PRECISION,
    position_azimuth        DOUBLE PRECISION,
    position_elevation      DOUBLE PRECISION,
    position_train          DOUBLE PRECISION,

    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- Hypertable 변환
SELECT create_hypertable('realtime_result', 'timestamp');

-- 청크 크기 설정 (1일 단위 - 100ms 데이터 대량)
SELECT set_chunk_time_interval('realtime_result', INTERVAL '1 day');

-- 압축 정책 (7일 후 압축)
ALTER TABLE realtime_result SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'master_id'
);
SELECT add_compression_policy('realtime_result', INTERVAL '7 days');

-- 인덱스
CREATE INDEX idx_rr_master ON realtime_result(master_id);
CREATE INDEX idx_rr_timestamp ON realtime_result(timestamp DESC);
CREATE INDEX idx_rr_quality ON realtime_result(tracking_quality);
```

#### 3.2.4 icd_realtime (ICD 실시간 데이터)

```sql
CREATE TABLE icd_realtime (
    timestamp                       TIMESTAMPTZ NOT NULL,

    -- 각도 (6개)
    azimuth_angle                   REAL,
    elevation_angle                 REAL,
    train_angle                     REAL,
    servo_driver_azimuth_angle      REAL,
    servo_driver_elevation_angle    REAL,
    servo_driver_train_angle        REAL,

    -- 속도 (3개)
    azimuth_speed                   REAL,
    elevation_speed                 REAL,
    train_speed                     REAL,

    -- 토크 (3개)
    torque_azimuth                  REAL,
    torque_elevation                REAL,
    torque_train                    REAL,

    -- 가속도 (6개)
    azimuth_acceleration            REAL,
    elevation_acceleration          REAL,
    train_acceleration              REAL,
    azimuth_max_acceleration        REAL,
    elevation_max_acceleration      REAL,
    train_max_acceleration          REAL,

    -- 환경 (3개)
    wind_speed                      REAL,
    wind_direction                  SMALLINT,
    rtd_one                         REAL,
    rtd_two                         REAL,

    -- 상태 비트 (12개)
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

    -- Feed 상태 (3개)
    feed_s_board_status             VARCHAR(32),
    feed_x_board_status             VARCHAR(32),
    feed_ka_board_status            VARCHAR(32),

    -- LNA 전류 (6개)
    current_sband_lna_lhcp          REAL,
    current_sband_lna_rhcp          REAL,
    current_xband_lna_lhcp          REAL,
    current_xband_lna_rhcp          REAL,
    current_kaband_lna_lhcp         REAL,
    current_kaband_lna_rhcp         REAL,

    -- RSSI (6개)
    rssi_sband_lna_lhcp             REAL,
    rssi_sband_lna_rhcp             REAL,
    rssi_xband_lna_lhcp             REAL,
    rssi_xband_lna_rhcp             REAL,
    rssi_kaband_lna_lhcp            REAL,
    rssi_kaband_lna_rhcp            REAL,

    -- 추적 CMD/실측 (9개)
    tracking_azimuth_time           REAL,
    tracking_cmd_azimuth            REAL,
    tracking_actual_azimuth         REAL,
    tracking_elevation_time         REAL,
    tracking_cmd_elevation          REAL,
    tracking_actual_elevation       REAL,
    tracking_train_time             REAL,
    tracking_cmd_train              REAL,
    tracking_actual_train           REAL
);

-- Hypertable 변환 (Primary Key 없음, timestamp가 파티션 키)
SELECT create_hypertable('icd_realtime', 'timestamp');

-- 청크 크기 설정 (1일 단위)
SELECT set_chunk_time_interval('icd_realtime', INTERVAL '1 day');

-- 압축 정책 (7일 후 압축)
ALTER TABLE icd_realtime SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = ''
);
SELECT add_compression_policy('icd_realtime', INTERVAL '7 days');

-- 보관 정책 (30일 후 삭제)
SELECT add_retention_policy('icd_realtime', INTERVAL '30 days');
```

### 3.3 보관 정책

| 테이블 | 보관 기간 | 삭제 방식 | 압축 |
|--------|----------|----------|------|
| tracking_master | 30일 | Spring Scheduler | 없음 |
| tracking_detail | 30일 | CASCADE (master 삭제 시) | TimescaleDB |
| realtime_result | 30일 | CASCADE (master 삭제 시) | TimescaleDB |
| icd_realtime | 30일 | TimescaleDB retention_policy | TimescaleDB (7일 후) |

### 3.4 초기 로딩 전략

```kotlin
@Service
class TrackingDataLoader(
    private val trackingMasterRepository: TrackingMasterRepository
) {

    @PostConstruct
    fun loadTodayData() {
        val today = LocalDate.now()

        // 1. 오늘 예정된 추적 데이터 로드
        val todayPasses = trackingMasterRepository.findByStartTimeBetween(
            today.atStartOfDay(ZoneOffset.UTC),
            today.plusDays(1).atStartOfDay(ZoneOffset.UTC)
        )

        // 2. 메모리에 캐시 (기존 로직과 호환)
        todayPasses.forEach { master ->
            // 기존 메모리 저장소에 복원
            restoreToMemoryStorage(master)
        }

        logger.info("Loaded ${todayPasses.size} tracking passes for today")
    }
}
```

### 3.5 데이터 내보내기 API

```kotlin
@RestController
@RequestMapping("/api/tracking")
class TrackingExportController(
    private val trackingExportService: TrackingExportService
) {

    /**
     * 추적 데이터 CSV 내보내기
     *
     * @param satelliteId 위성 ID
     * @param date 날짜 (yyyy-MM-dd)
     * @param type 데이터 타입 (master, detail, realtime)
     */
    @GetMapping("/export")
    fun exportToCsv(
        @RequestParam satelliteId: String,
        @RequestParam date: LocalDate,
        @RequestParam(defaultValue = "realtime") type: String
    ): ResponseEntity<ByteArray> {
        val csv = trackingExportService.exportToCsv(satelliteId, date, type)

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"tracking_${satelliteId}_${date}.csv\"")
            .header("Content-Type", "text/csv; charset=UTF-8")
            .body(csv.toByteArray(Charsets.UTF_8))
    }
}
```

---

## 4. 대안 (Alternatives)

### 4.1 검토한 대안들

| 대안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| MongoDB | 스키마 유연성 | 시계열 비효율, 학습 비용 | ❌ |
| InfluxDB | 시계열 특화 | PostgreSQL과 별도 운영 | ❌ |
| JSONB 컬럼 | 스키마 변경 용이 | 인덱싱 제한, 타입 안전성 없음 | ❌ |
| **PostgreSQL + TimescaleDB** | 시계열 최적화 + SQL 호환 | 확장 설치 필요 | ✅ |

### 4.2 CSV 백업 vs DB 내보내기

| 방식 | 현재 | 변경 후 |
|------|------|---------|
| CSV 별도 저장 | ✅ 사용 중 | ❌ 제거 |
| DB 내보내기 API | ❌ 없음 | ✅ 추가 |

**결론**: DB 저장으로 CSV 별도 저장 불필요. 필요 시 API로 내보내기.

---

## 5. 영향 분석 (Impact)

### 5.1 변경되는 파일

| 파일 | 변경 내용 |
|------|----------|
| EphemerisService.kt | 메모리 저장 → DB 저장 호출 추가 |
| PassScheduleService.kt | 메모리 저장 → DB 저장 호출 추가 |
| UdpFwICDService.kt | ICD 데이터 배치 저장 추가 |
| BatchStorageManager.kt | DB 배치 INSERT 구현 |
| (신규) TrackingMasterEntity.kt | Entity 클래스 |
| (신규) TrackingDetailEntity.kt | Entity 클래스 |
| (신규) RealtimeResultEntity.kt | Entity 클래스 |
| (신규) IcdRealtimeEntity.kt | Entity 클래스 |
| (신규) TrackingRepository.kt | Repository 인터페이스 |
| (신규) TrackingExportService.kt | CSV 내보내기 |

### 5.2 성능 영향

| 영역 | 영향 | 대응 |
|------|------|------|
| 추적 중 저장 | 100ms마다 INSERT | 배치 INSERT (1초마다 10건) |
| ICD 저장 | 100ms마다 INSERT | 배치 INSERT (1초마다 10건) |
| 조회 | 메모리 → DB | 오늘 데이터는 메모리 캐시 유지 |
| 디스크 | 30일 약 3.7GB | 512GB 기준 0.7% |

### 5.3 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.postgresql:postgresql:42.7.1")
    // TimescaleDB는 PostgreSQL 확장이므로 별도 의존성 불필요
}
```

---

## 6. 마이그레이션 (Migration)

### 6.1 단계별 전환

```
Phase 1: 인프라 준비 (1일)
├── PostgreSQL + TimescaleDB 설치
├── 테이블 생성
└── Entity/Repository 클래스 작성

Phase 2: 병행 저장 (2일)
├── 메모리 저장 유지
├── DB 저장 추가 (비동기)
└── 저장 검증

Phase 3: 조회 전환 (1일)
├── 과거 데이터 조회 → DB
├── 오늘 데이터 조회 → 메모리 (캐시)
└── 내보내기 API 추가

Phase 4: 메모리 저장소 축소 (1일)
├── 오늘 데이터만 메모리 유지
├── 과거 데이터 메모리에서 제거
└── CSV 별도 저장 제거
```

### 6.2 롤백 전략

```kotlin
// application.yml
acs:
  storage:
    mode: hybrid  # memory | database | hybrid

// 문제 발생 시: mode: memory로 전환하면 기존 동작
```

---

## 7. 검증 (Verification)

### 7.1 기능 검증

| 테스트 | 검증 내용 |
|--------|----------|
| 저장 테스트 | 추적 완료 후 DB에 데이터 존재 |
| 재시작 테스트 | 서버 재시작 후 오늘 데이터 복원 |
| 조회 테스트 | 과거 추적 이력 조회 가능 |
| 내보내기 테스트 | CSV 다운로드 정상 |
| 보관 테스트 | 31일 지난 데이터 자동 삭제 |

### 7.2 성능 검증

| 테스트 | 기준 |
|--------|------|
| 배치 INSERT 지연 | < 100ms (10건 기준) |
| 조회 응답 시간 | < 500ms (1일 데이터) |
| 디스크 사용량 | < 5GB (30일 기준) |

### 7.3 성공 기준

- [ ] 서버 재시작 후 오늘 추적 예정 데이터 자동 로드
- [ ] 다른 위성 추적해도 이전 추적 데이터 유지
- [ ] 과거 30일 추적 이력 조회 가능
- [ ] CSV 다운로드 기능 동작
- [ ] 31일 지난 데이터 자동 삭제

---

## 8. Entity 클래스 설계

### 8.1 TrackingMasterEntity

```kotlin
package com.gtlsystems.acs_api.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime

@Table("tracking_master")
data class TrackingMasterEntity(
    @Id
    val id: Long? = null,
    val mstId: String,
    val detailId: String,
    val satelliteId: String,
    val satelliteName: String?,
    val trackingMode: String,  // EPHEMERIS | PASS_SCHEDULE

    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val duration: Int?,

    val maxElevation: Double?,
    val maxAzimuthRate: Double?,
    val maxElevationRate: Double?,
    val keyholeDetected: Boolean = false,

    val trainAngle: Double?,
    val dataType: String?,
    val totalPoints: Int?,

    val createdAt: ZonedDateTime = ZonedDateTime.now()
)
```

### 8.2 RealtimeResultEntity (57개 필드)

```kotlin
@Table("realtime_result")
data class RealtimeResultEntity(
    @Id
    val id: Long? = null,
    val masterId: Long,

    val index: Int,
    val theoreticalIndex: Int?,
    val timestamp: ZonedDateTime,

    // 원본 각도
    val originalAzimuth: Double?,
    val originalElevation: Double?,

    // 변환 각도
    val transformedAzimuth: Double?,
    val transformedElevation: Double?,
    val transformedTrain: Double?,

    // 최종 각도
    val finalAzimuth: Double?,
    val finalElevation: Double?,
    val finalTrain: Double?,

    // 실측값
    val actualAzimuth: Double?,
    val actualElevation: Double?,
    val actualTrain: Double?,

    // 오차
    val azimuthError: Double?,
    val elevationError: Double?,
    val trainError: Double?,
    val totalError: Double?,

    // 속도/가속도
    val azimuthRate: Double?,
    val elevationRate: Double?,
    val trainRate: Double?,
    val azimuthAcceleration: Double?,
    val elevationAcceleration: Double?,
    val trainAcceleration: Double?,

    // 상태
    val keyholeActive: Boolean = false,
    val keyholeOptimized: Boolean = false,
    val trackingQuality: String?,

    // ... 나머지 필드
    val createdAt: ZonedDateTime = ZonedDateTime.now()
)
```

---

## 9. 결정 요약

| 항목 | 결정 |
|------|------|
| DBMS | PostgreSQL 16 + TimescaleDB |
| 드라이버 | Spring Data JDBC |
| 보관 기간 | 30일 |
| 압축 | TimescaleDB 자동 (7일 후) |
| CSV 백업 | 제거 (DB 내보내기 API로 대체) |
| 초기 로딩 | 오늘 예정 데이터만 메모리 캐시 |
| 과거 조회 | DB 직접 조회 + CSV 내보내기 |

---

**작성자**: Claude
**검토자**: -
**승인일**: -
