# RFC-001 데이터베이스 전략 - 심층 분석 보고서

> **분석일**: 2026-01-14
> **기반 문서**: RFC-001_Database_Strategy.md v1.1.0
> **분석 목적**: RFC-001 구현 전 현재 상태 심층 분석 및 마이그레이션 계획 구체화

---

## 1. 현재 데이터 저장 방식 분석

### 1.1 메모리 저장소 현황

| 저장소 위치 | 자료구조 | 저장 데이터 | 생명주기 |
|------------|---------|------------|---------|
| `EphemerisService.kt` | `mutableListOf<Map<String, Any?>>()` | ephemerisTrackMst/Dtl | 새 계산 시 clear |
| `PassScheduleService.kt` | `ConcurrentHashMap<String, List<Map<String, Any?>>>()` | passScheduleTrackMst/Dtl | 위성별 키로 유지 |
| `DataStoreService.kt` | `AtomicReference<PushData.ReadData>()` | 실시간 ICD 데이터 (55개 필드) | 최신 값만 유지 |
| `BatchStorageManager.kt` | `mutableListOf<Map<String, Any?>>()` | 실시간 추적 배치 데이터 | 배치 처리 후 유지 |

### 1.2 데이터 흐름 상세

```
┌─────────────────────────────────────────────────────────────────────┐
│                        TLE 데이터 입력                               │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│              SatelliteTrackingProcessor (Orekit)                    │
│  - 100ms 간격 궤적 계산                                              │
│  - 8가지 DataType 생성                                               │
└─────────────────────────────────────────────────────────────────────┘
                                   │
           ┌───────────────────────┼───────────────────────┐
           ▼                       ▼                       ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│ ephemerisTrackMst   │ │ ephemerisTrackDtl   │ │ passSchedule*       │
│ Storage             │ │ Storage             │ │ Storage             │
│ (List<Map>)         │ │ (List<Map>)         │ │ (ConcurrentHashMap) │
└─────────────────────┘ └─────────────────────┘ └─────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     실시간 추적 (100ms 주기)                          │
│  createRealtimeTrackingData() → BatchStorageManager.addToBatch()    │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    DataStoreService (ICD 데이터)                     │
│  - UDP 100ms 수신 → 55개 필드 업데이트                               │
│  - 히스토리 없음 (최신 값만)                                          │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 저장되는 데이터 구조

#### 1.3.1 MST (Master) 데이터 구조

```kotlin
// EphemerisService.kt 기준
Map<String, Any?> = mapOf(
    "MstId" to Long,                    // 전역 고유 ID
    "DataType" to String,               // 8가지: original, axis_transformed, final_transformed,
                                        // keyhole_axis_transformed, keyhole_final_transformed,
                                        // keyhole_optimized_axis_transformed, keyhole_optimized_final_transformed
    "SatelliteId" to String,            // 위성 카탈로그 번호
    "SatelliteName" to String?,         // 위성 이름
    "StartTime" to ZonedDateTime,       // 추적 시작 시간
    "EndTime" to ZonedDateTime,         // 추적 종료 시간
    "Duration" to Int?,                 // 초 단위
    "MaxElevation" to Double?,          // 최대 고도
    "IsKeyhole" to Boolean,             // Keyhole 발생 여부
    "RecommendedTrainAngle" to Double?, // 권장 Train 각도
    "TotalPoints" to Int?               // 전체 포인트 수
)
```

#### 1.3.2 DTL (Detail) 데이터 구조

```kotlin
// EphemerisService.kt 기준
Map<String, Any?> = mapOf(
    "MstId" to Long,                    // Master FK
    "DetailId" to Int,                  // 패스 인덱스
    "DataType" to String,               // 8가지 DataType
    "Index" to Int,                     // 데이터 포인트 인덱스
    "Time" to ZonedDateTime,            // 타임스탬프
    "Azimuth" to Double,                // 방위각 (도)
    "Elevation" to Double,              // 고도각 (도)
    "Train" to Double?,                 // Train 각도 (도)
    "AzimuthRate" to Double?,           // 방위각 변화율
    "ElevationRate" to Double?,         // 고도각 변화율
    "Range" to Double?,                 // 거리 (km)
    "Altitude" to Double?               // 고도 (km)
)
```

#### 1.3.3 실시간 추적 데이터 구조 (57개 필드)

```kotlin
// createRealtimeTrackingData() 반환값
Map<String, Any?> = mapOf(
    // 인덱스/시간 (3개)
    "index" to Int,
    "theoreticalIndex" to Int,
    "timestamp" to ZonedDateTime,

    // 원본 데이터 (4개)
    "originalAzimuth" to Float,
    "originalElevation" to Float,
    "originalRange" to Float,
    "originalAltitude" to Float,

    // 축변환 데이터 (4개)
    "axisTransformedAzimuth" to Float,
    "axisTransformedElevation" to Float,
    "axisTransformedRange" to Float,
    "axisTransformedAltitude" to Float,

    // 최종 변환 데이터 (4개)
    "finalTransformedAzimuth" to Float,
    "finalTransformedElevation" to Float,
    "finalTransformedRange" to Float,
    "finalTransformedAltitude" to Float,

    // Keyhole Final 변환 (4개)
    "keyholeFinalTransformedAzimuth" to Float?,
    "keyholeFinalTransformedElevation" to Float?,
    "keyholeFinalTransformedRange" to Float?,
    "keyholeFinalTransformedAltitude" to Float?,

    // CMD/Actual (10개)
    "cmdAz" to Float,
    "cmdEl" to Float,
    "actualAz" to Float?,
    "actualEl" to Float?,
    "trackingCMDAzimuthAngle" to Float?,
    "trackingActualAzimuthAngle" to Float?,
    "trackingCMDElevationAngle" to Float?,
    "trackingActualElevationAngle" to Float?,
    "trackingCMDTrainAngle" to Float?,
    "trackingActualTrainAngle" to Float?,

    // 시간 관련 (4개)
    "elapsedTimeSeconds" to Float,
    "trackingAzimuthTime" to Float?,
    "trackingElevationTime" to Float?,
    "trackingTrainTime" to Float?,

    // 오차 (9개)
    "azimuthError" to Float,
    "elevationError" to Float,
    "originalToAxisTransformationError" to Float,
    "axisToFinalTransformationError" to Float,
    "totalTransformationError" to Float,
    "azCmdAccuracy" to Float,
    "azActAccuracy" to Float,
    "elCmdAccuracy" to Float,
    "elActAccuracy" to Float,

    // 메타데이터 (10개)
    "passId" to Long,
    "mstId" to Long,
    "detailId" to Int,
    "hasValidData" to Boolean,
    "dataSource" to String,
    "tiltAngle" to Double,
    "transformationType" to String,
    "isKeyhole" to Boolean,
    "finalDataType" to String,
    "hasTransformation" to Boolean,

    // 보간 정보 (2개)
    "interpolationMethod" to String,
    "interpolationAccuracy" to Double
)
```

#### 1.3.4 ICD 실시간 데이터 구조 (55개 필드)

```kotlin
// PushData.ReadData (DataStoreService에서 관리)
data class ReadData(
    // 각도 (10개)
    val azimuthAngle: Float?,
    val elevationAngle: Float?,
    val trainAngle: Float?,
    val servoDriverAzimuthAngle: Float?,
    val servoDriverElevationAngle: Float?,
    val servoDriverTrainAngle: Float?,
    val trackingCMDAzimuthAngle: Float?,
    val trackingCMDElevationAngle: Float?,
    val trackingCMDTrainAngle: Float?,
    val trackingActualAzimuthAngle: Float?,
    val trackingActualElevationAngle: Float?,
    val trackingActualTrainAngle: Float?,

    // 속도 (3개)
    val azimuthSpeed: Float?,
    val elevationSpeed: Float?,
    val trainSpeed: Float?,

    // 가속도 (6개)
    val azimuthAcceleration: Float?,
    val elevationAcceleration: Float?,
    val trainAcceleration: Float?,
    val azimuthMaxAcceleration: Float?,
    val elevationMaxAcceleration: Float?,
    val trainMaxAcceleration: Float?,

    // 토크 (3개)
    val torqueAzimuth: Float?,
    val torqueElevation: Float?,
    val torqueTrain: Float?,

    // 환경 (4개)
    val windSpeed: Float?,
    val windDirection: UShort?,
    val rtdOne: Float?,
    val rtdTwo: Float?,

    // 상태 비트 (15개)
    val modeStatusBits: String?,
    val mainBoardProtocolStatusBits: String?,
    val mainBoardStatusBits: String?,
    val mainBoardMCOnOffBits: String?,
    val mainBoardReserveBits: String?,
    val azimuthBoardServoStatusBits: String?,
    val azimuthBoardStatusBits: String?,
    val elevationBoardServoStatusBits: String?,
    val elevationBoardStatusBits: String?,
    val trainBoardServoStatusBits: String?,
    val trainBoardStatusBits: String?,
    val feedBoardETCStatusBits: String?,
    val feedSBoardStatusBits: String?,
    val feedXBoardStatusBits: String?,
    val feedKaBoardStatusBits: String?,

    // LNA 전류/RSSI (10개)
    val currentSBandLNALHCP: Float?,
    val currentSBandLNARHCP: Float?,
    val currentXBandLNALHCP: Float?,
    val currentXBandLNARHCP: Float?,
    val currentKaBandLNALHCP: Float?,
    val currentKaBandLNARHCP: Float?,
    val rssiSBandLNALHCP: Float?,
    val rssiSBandLNARHCP: Float?,
    val rssiXBandLNALHCP: Float?,
    val rssiXBandLNARHCP: Float?,

    // 추적 시간 (3개)
    val trackingAzimuthTime: Float?,
    val trackingElevationTime: Float?,
    val trackingTrainTime: Float?
)
```

---

## 2. 데이터 볼륨 추정

### 2.1 일별 데이터 발생량

| 데이터 유형 | 계산 기준 | 일일 레코드 수 | 레코드 크기 | 일일 용량 |
|------------|----------|---------------|------------|----------|
| **tracking_detail** | 5패스 x 10분 x 600포인트 x 8 DataType | ~24,000 | ~200B | ~4.8MB |
| **realtime_result** | 5패스 x 10분 x 10/s | ~30,000 | ~500B | ~15MB |
| **icd_realtime** | 24시간 x 60분 x 60초 x 10/s | ~864,000 | ~400B | ~346MB |

### 2.2 월별/30일 예상 용량

| 테이블 | 30일 레코드 수 | 30일 원본 용량 | 압축 후 예상 (70% 감소) |
|--------|---------------|---------------|----------------------|
| tracking_master | ~1,200 | ~1MB | ~0.3MB |
| tracking_detail | ~720,000 | ~144MB | ~43MB |
| realtime_result | ~900,000 | ~450MB | ~135MB |
| icd_realtime | ~25,920,000 | ~10.4GB | ~3.1GB |
| **합계** | ~27.5M | ~11GB | ~3.3GB |

### 2.3 실제 운영 시나리오

```yaml
보수적 시나리오 (위성 3개, 일 3패스):
  tracking_detail: ~14,400 레코드/일 (~2.9MB/일)
  realtime_result: ~18,000 레코드/일 (~9MB/일)
  icd_realtime: ~864,000 레코드/일 (~346MB/일)
  30일 총량: ~2GB (압축 후)

적극적 시나리오 (위성 10개, 일 10패스):
  tracking_detail: ~48,000 레코드/일 (~9.6MB/일)
  realtime_result: ~60,000 레코드/일 (~30MB/일)
  icd_realtime: ~864,000 레코드/일 (~346MB/일)
  30일 총량: ~4GB (압축 후)
```

---

## 3. RFC-001 테이블 설계 검토

### 3.1 현재 설계 평가

| 항목 | RFC-001 설계 | 평가 | 개선 제안 |
|------|-------------|------|----------|
| mst_id 타입 | BIGINT | 적절 | 코드와 일치 (Long) |
| detail_id 타입 | INTEGER | 적절 | 코드와 일치 (Int) |
| data_type 필수화 | NOT NULL | 적절 | 8가지 타입 명시 필요 |
| 복합 인덱스 | (mst_id, data_type) | 적절 | 조회 패턴에 맞음 |
| 청크 크기 | 7일 | 재검토 필요 | icd_realtime은 1일이 적절 |
| 압축 정책 | 7일 후 | 적절 | segmentby 설정 검증 필요 |

### 3.2 누락된 필드 분석

#### realtime_result 테이블 누락 필드

현재 RFC-001에 정의된 필드: ~40개
실제 createRealtimeTrackingData() 필드: 57개

**추가 필요 필드:**

```sql
-- 축변환 데이터 (RFC-001에 없음)
axis_transformed_azimuth      DOUBLE PRECISION,
axis_transformed_elevation    DOUBLE PRECISION,
axis_transformed_range        DOUBLE PRECISION,
axis_transformed_altitude     DOUBLE PRECISION,

-- 정확도 분석 필드 (RFC-001에 없음)
time_accuracy                 REAL,
az_cmd_accuracy               REAL,
az_act_accuracy               REAL,
az_final_accuracy             REAL,
el_cmd_accuracy               REAL,
el_act_accuracy               REAL,
el_final_accuracy             REAL,

-- 변환 메타데이터 (RFC-001에 없음)
tilt_angle                    DOUBLE PRECISION,
transformation_type           VARCHAR(20),
final_data_type               VARCHAR(50)
```

### 3.3 개선된 테이블 설계

#### 3.3.1 realtime_result 개선안

```sql
CREATE TABLE realtime_result (
    -- PK (변경 없음)
    id                          BIGSERIAL PRIMARY KEY,
    master_id                   BIGINT REFERENCES tracking_master(id) ON DELETE CASCADE,

    -- 인덱스/시간 (변경 없음)
    index                       INTEGER NOT NULL,
    theoretical_index           INTEGER,
    timestamp                   TIMESTAMPTZ NOT NULL,

    -- 원본 데이터 (추가)
    original_azimuth            REAL,
    original_elevation          REAL,
    original_range              REAL,
    original_altitude           REAL,

    -- 축변환 데이터 (추가)
    axis_transformed_azimuth    REAL,
    axis_transformed_elevation  REAL,
    axis_transformed_range      REAL,
    axis_transformed_altitude   REAL,

    -- 최종 변환 데이터 (기존)
    final_azimuth               REAL,
    final_elevation             REAL,
    final_train                 REAL,

    -- Keyhole 변환 데이터 (기존)
    keyhole_final_azimuth       REAL,
    keyhole_final_elevation     REAL,
    keyhole_final_range         REAL,
    keyhole_final_altitude      REAL,

    -- CMD/Actual (기존)
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

    -- 시간 (기존)
    elapsed_time_seconds        REAL,
    tracking_azimuth_time       REAL,
    tracking_elevation_time     REAL,
    tracking_train_time         REAL,

    -- 오차 (확장)
    azimuth_error               REAL,
    elevation_error             REAL,
    original_to_axis_error      REAL,
    axis_to_final_error         REAL,
    total_transformation_error  REAL,
    az_cmd_accuracy             REAL,
    az_act_accuracy             REAL,
    az_final_accuracy           REAL,
    el_cmd_accuracy             REAL,
    el_act_accuracy             REAL,
    el_final_accuracy           REAL,

    -- 메타데이터 (확장)
    mst_id                      BIGINT NOT NULL,
    detail_id                   INTEGER NOT NULL,
    has_valid_data              BOOLEAN DEFAULT FALSE,
    data_source                 VARCHAR(50),
    tilt_angle                  DOUBLE PRECISION,
    transformation_type         VARCHAR(20),
    is_keyhole                  BOOLEAN DEFAULT FALSE,
    final_data_type             VARCHAR(50),
    has_transformation          BOOLEAN DEFAULT FALSE,

    -- 보간 정보 (기존)
    interpolation_method        VARCHAR(20),
    interpolation_accuracy      DOUBLE PRECISION,

    created_at                  TIMESTAMPTZ DEFAULT NOW()
);

-- Hypertable 변환
SELECT create_hypertable('realtime_result', 'timestamp');

-- 청크 크기 (1일)
SELECT set_chunk_time_interval('realtime_result', INTERVAL '1 day');

-- 압축 정책
ALTER TABLE realtime_result SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'master_id, mst_id'
);
SELECT add_compression_policy('realtime_result', INTERVAL '7 days');
```

---

## 4. 구현 단계별 계획

### 4.1 Phase 1: 인프라 준비 (1일)

```yaml
작업:
  1. PostgreSQL 16 + TimescaleDB 설치
     - Docker 또는 네이티브 설치
     - 확장 활성화: CREATE EXTENSION IF NOT EXISTS timescaledb;

  2. 테이블 생성 (수정된 스키마)
     - tracking_master
     - tracking_detail
     - realtime_result (확장된 버전)
     - icd_realtime

  3. Spring Boot 의존성 추가
     implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
     implementation("org.postgresql:postgresql:42.7.1")

  4. application.yml 설정
     spring:
       datasource:
         url: jdbc:postgresql://localhost:5432/acs
         username: acs
         password: ${DB_PASSWORD}
       sql:
         init:
           mode: always

산출물:
  - PostgreSQL + TimescaleDB 실행 환경
  - 4개 테이블 생성 완료
  - Spring Data JDBC 연결 확인
```

### 4.2 Phase 2: Repository 추상화 (2일)

```yaml
작업:
  1. Entity 클래스 작성 (6개)
     - TrackingMasterEntity.kt
     - TrackingDetailEntity.kt
     - RealtimeResultEntity.kt
     - IcdRealtimeEntity.kt
     - (DTO 변환) TrackingDataMapper.kt

  2. Repository 인터페이스 작성 (4개)
     - TrackingMasterRepository.kt
     - TrackingDetailRepository.kt
     - RealtimeResultRepository.kt
     - IcdRealtimeRepository.kt

  3. 추상화 계층 구현
     - TrackingDataStoragePort.kt (포트 인터페이스)
     - MemoryTrackingStorage.kt (기존 로직)
     - DatabaseTrackingStorage.kt (신규)
     - HybridTrackingStorage.kt (병행 저장)

산출물:
  - 완전한 Repository 계층
  - 저장소 추상화 (포트 패턴)
  - 단위 테스트
```

### 4.3 Phase 3: 병행 저장 (2일)

```yaml
작업:
  1. EphemerisService 수정
     - ephemerisTrackMstStorage → HybridTrackingStorage 호출
     - ephemerisTrackDtlStorage → HybridTrackingStorage 호출

  2. PassScheduleService 수정
     - passScheduleTrackMstStorage → HybridTrackingStorage 호출
     - passScheduleTrackDtlStorage → HybridTrackingStorage 호출

  3. BatchStorageManager 수정
     - realtimeTrackingDataList → DB 배치 INSERT 추가
     - 비동기 저장 (기존 메모리 동작 유지)

  4. UdpFwICDService 수정
     - ICD 데이터 배치 INSERT 추가
     - 100ms → 1초 배치 (10건)

검증:
  - 기존 메모리 동작 그대로 유지
  - DB에 데이터 저장 확인
  - 성능 저하 없음 확인 (100ms 추적 유지)
```

### 4.4 Phase 4: 조회 전환 (1일)

```yaml
작업:
  1. 과거 데이터 조회 API
     - GET /api/tracking/history?date=2026-01-14
     - GET /api/tracking/export?satelliteId=25544&date=2026-01-14

  2. 초기 로딩 구현 (TrackingDataLoader)
     - @PostConstruct에서 오늘 데이터 로드
     - 메모리 캐시 복원

  3. 조회 라우팅
     - 오늘 데이터 → 메모리 캐시
     - 과거 데이터 → DB 조회

산출물:
  - 과거 이력 조회 API
  - CSV 내보내기 API
  - 서버 재시작 후 데이터 복원
```

### 4.5 Phase 5: 최적화 및 정리 (1일)

```yaml
작업:
  1. 메모리 저장소 축소
     - 오늘 데이터만 메모리 유지
     - 과거 데이터 메모리에서 제거

  2. CSV 별도 저장 제거
     - 기존 CSV 저장 로직 제거
     - DB 내보내기로 대체

  3. 보관 정책 적용
     - 30일 후 자동 삭제 설정
     - Spring Scheduler 또는 TimescaleDB retention_policy

산출물:
  - 최적화된 메모리 사용
  - 자동 데이터 정리
  - 운영 가이드 문서
```

---

## 5. 영향 범위 상세

### 5.1 Backend 변경 파일

| 파일 | 변경 내용 | 영향도 |
|------|----------|-------|
| `EphemerisService.kt` | Storage 호출 추가 | 중간 |
| `PassScheduleService.kt` | Storage 호출 추가 | 중간 |
| `BatchStorageManager.kt` | DB 배치 INSERT | 높음 |
| `UdpFwICDService.kt` | ICD 배치 저장 | 중간 |
| `DataStoreService.kt` | 변경 없음 | 없음 |
| (신규) `entity/*.kt` | Entity 클래스 | 신규 |
| (신규) `repository/*.kt` | Repository | 신규 |
| (신규) `port/*.kt` | 추상화 계층 | 신규 |

### 5.2 Frontend 변경

```yaml
영향 없음:
  - 기존 API 그대로 유지
  - 조회 응답 형식 동일

추가 기능 (선택):
  - 과거 이력 조회 UI
  - CSV 다운로드 버튼
```

### 5.3 성능 영향

| 작업 | 현재 | 변경 후 | 대응 |
|------|------|--------|------|
| 추적 데이터 저장 | 메모리 즉시 | 메모리 + 비동기 DB | 비동기 처리 |
| 조회 (오늘) | 메모리 | 메모리 (캐시) | 동일 |
| 조회 (과거) | 불가 | DB 조회 | 인덱스 최적화 |
| ICD 저장 | 없음 | 배치 INSERT | 1초 배치 |

---

## 6. 롤백 전략

### 6.1 설정 기반 전환

```yaml
# application.yml
acs:
  storage:
    mode: hybrid  # memory | database | hybrid

# 문제 발생 시: mode: memory
# 완전 전환 후: mode: database
```

### 6.2 단계별 롤백 지점

```
Phase 1 롤백: 테이블 삭제, 의존성 제거
Phase 2 롤백: Repository 클래스 제거
Phase 3 롤백: mode: memory 전환
Phase 4 롤백: 조회 API 비활성화
Phase 5 롤백: CSV 저장 복구
```

---

## 7. 검증 체크리스트

### 7.1 기능 검증

- [ ] 추적 완료 후 DB에 데이터 존재
- [ ] 서버 재시작 후 오늘 데이터 복원
- [ ] 과거 30일 추적 이력 조회 가능
- [ ] CSV 다운로드 정상
- [ ] 31일 지난 데이터 자동 삭제

### 7.2 성능 검증

- [ ] 배치 INSERT 지연 < 100ms (10건 기준)
- [ ] 조회 응답 시간 < 500ms (1일 데이터)
- [ ] 100ms 추적 주기 유지
- [ ] 메모리 사용량 증가 없음

### 7.3 안정성 검증

- [ ] DB 연결 실패 시 메모리 동작 유지
- [ ] 배치 저장 실패 시 재시도
- [ ] 서버 종료 시 배치 버퍼 안전 저장

---

## 8. 결론 및 권장사항

### 8.1 RFC-001 평가

| 항목 | 평가 | 비고 |
|------|------|------|
| 기술 스택 선정 | 적절 | PostgreSQL + TimescaleDB |
| 테이블 설계 | 보완 필요 | realtime_result 필드 추가 |
| 마이그레이션 계획 | 적절 | 병행 저장 전략 |
| 보관 정책 | 적절 | 30일 + 압축 |

### 8.2 권장 수정사항

1. **realtime_result 테이블 필드 확장**: 57개 필드 모두 매핑
2. **icd_realtime 청크 크기**: 7일 → 1일 (대용량 데이터)
3. **복합 인덱스 추가**: `(mst_id, detail_id, timestamp)`
4. **압축 segmentby 검증**: `master_id, mst_id` 병행

### 8.3 예상 일정

| Phase | 기간 | 담당 |
|-------|------|------|
| Phase 1 | 1일 | 인프라 |
| Phase 2 | 2일 | Backend |
| Phase 3 | 2일 | Backend |
| Phase 4 | 1일 | Backend |
| Phase 5 | 1일 | Backend + QA |
| **총계** | **7일** | - |

---

**분석자**: Claude (Architect Agent)
**검토자**: -
**승인일**: -
