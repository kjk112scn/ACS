# Tracking Schema 심층 검토 보고서 (V007)

> **작성일**: 2026-01-22
> **검토자**: db-master, architect, fullstack-helper
> **목적**: 3개 테이블 연계 구조 + 7가지 DataType 설계 검증

---

## 1. 요약

### 핵심 질문과 답변

| 질문 | 답변 |
|------|------|
| master/detail 구조가 TimescaleDB와 호환되나? | ✅ 완벽 호환 (권장 패턴) |
| 7가지 DataType을 어떻게 저장하나? | ✅ tracking_trajectory에 data_type 컬럼으로 구분 |
| 실측치가 어떤 이론치를 사용했는지 어떻게 알 수 있나? | ✅ tracking_result.used_data_type으로 추적 |

### V007 필요 변경사항

| 변경 | 이유 |
|------|------|
| position 컬럼 제거 | cmd/actual 중복 |
| error 컬럼 제거 | 파생값 (엑셀에서 계산) |
| used_data_type 컬럼 추가 | 이론치-실측치 연계 핵심 |

---

## 2. 7가지 DataType 정의

### 변환 파이프라인

```
TLE → Orekit → 2축 (Az/El)
        ↓
┌───────────────────────────────────────────────────────────────┐
│               CoordinateTransformer (3축 변환)                 │
├───────────────────────────────────────────────────────────────┤
│  Train=0 경로                │  Train≠0 경로 (Keyhole)        │
│  ─────────────                │  ───────────────────────       │
│  1. original                  │  4. keyhole_axis_transformed   │
│  2. axis_transformed          │  5. keyhole_final_transformed  │
│  3. final_transformed         │  6. keyhole_optimized_axis     │
│                               │  7. keyhole_optimized_final    │
└───────────────────────────────────────────────────────────────┘
        ↓
   LimitAngleCalculator (±270° 변환)
        ↓
   포지셔너 명령 전송
```

### 7가지 DataType 상세

| # | data_type | Train | 각도 제한 | 용도 |
|---|-----------|-------|----------|------|
| 1 | `original` | N/A | ❌ | 원본 2축 (Az/El) - Orekit 출력 |
| 2 | `axis_transformed` | 0 | ❌ | 3축 변환 (Train=0 고정) |
| 3 | `final_transformed` | 0 | ✅ | 최종 명령값 (각도 제한 적용) |
| 4 | `keyhole_axis_transformed` | ≠0 | ❌ | Keyhole 회피 3축 변환 |
| 5 | `keyhole_final_transformed` | ≠0 | ✅ | Keyhole 회피 최종 명령값 |
| 6 | `keyhole_optimized_axis` | 최적화 | ❌ | Keyhole 최적화 3축 변환 |
| 7 | `keyhole_optimized_final` | 최적화 | ✅ | Keyhole 최적화 최종 명령값 |

### 코드 참조

```kotlin
// ProcessedTrackingData.kt:23-38
data class ProcessedTrackingData(
    val originalMst: List<Map<String, Any?>>,           // 1
    val originalDtl: List<Map<String, Any?>>,
    val axisTransformedMst: List<Map<String, Any?>>,    // 2
    val axisTransformedDtl: List<Map<String, Any?>>,
    val finalTransformedMst: List<Map<String, Any?>>,   // 3
    val finalTransformedDtl: List<Map<String, Any?>>,
    val keyholeAxisTransformedMst: List<Map<String, Any?>>,     // 4
    val keyholeAxisTransformedDtl: List<Map<String, Any?>>,
    val keyholeFinalTransformedMst: List<Map<String, Any?>>,    // 5
    val keyholeFinalTransformedDtl: List<Map<String, Any?>>,
    val keyholeOptimizedAxisTransformedMst: List<Map<String, Any?>>,  // 6
    val keyholeOptimizedAxisTransformedDtl: List<Map<String, Any?>>,
    val keyholeOptimizedFinalTransformedMst: List<Map<String, Any?>>, // 7
    val keyholeOptimizedFinalTransformedDtl: List<Map<String, Any?>>
)
```

---

## 3. 테이블 연계 구조 (ERD)

```
┌──────────────────────────────────────────────────────────────────┐
│                      tracking_session                             │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│  id (PK)                                                         │
│  mst_id          ← 위성별 그룹 ID                                │
│  detail_id       ← Pass 구분자                                   │
│  satellite_id    ← NORAD ID                                      │
│  satellite_name                                                  │
│  tracking_mode   ← 'EPHEMERIS' | 'PASS_SCHEDULE'                 │
│  start_time, end_time                                            │
│  max_elevation, keyhole_detected                                 │
│  tle_cache_id (FK) → tle_cache                                   │
│  tle_line_1, tle_line_2, tle_epoch  ← TLE 스냅샷                 │
│                                                                  │
│  UNIQUE(mst_id, detail_id, tracking_mode)                        │
└──────────────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┴──────────────────┐
           ▼                                     ▼
┌─────────────────────────────────┐  ┌─────────────────────────────────┐
│    tracking_trajectory          │  │      tracking_result            │
│    (Hypertable)                 │  │      (Hypertable)               │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │  │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│  timestamp (PK)                 │  │  timestamp (PK)                 │
│  session_id (FK)  ──────────────┼──┼─ session_id (FK)                │
│  data_type ← 7가지 중 1개       │  │  used_data_type ← ✅ 핵심 추가  │
│  index                          │  │  theoretical_index ← JOIN 키    │
│                                 │  │  index                          │
│  azimuth                        │  │  ─────────────────────────────  │
│  elevation                      │  │  tracking_cmd_azimuth           │
│  train                          │  │  tracking_cmd_elevation         │
│  azimuth_rate                   │  │  tracking_cmd_train             │
│  elevation_rate                 │  │  tracking_actual_azimuth        │
│  train_rate ← V006 추가         │  │  tracking_actual_elevation      │
│  satellite_range ← V006 추가    │  │  tracking_actual_train          │
│  satellite_altitude ← V006 추가 │  │  tracking_quality               │
│  satellite_velocity ← V006 추가 │  │                                 │
└─────────────────────────────────┘  └─────────────────────────────────┘
```

---

## 4. PassSchedule 시나리오 검증

### 시나리오: 위성 2개, 각 3개 스케줄

```
                     PassSchedule 모드
                     ────────────────
                            │
           ┌────────────────┼────────────────┐
           ▼                ▼                ▼
       위성 A            위성 B          위성 C
      (mst_id=1)       (mst_id=2)      (mst_id=3)
           │                │                │
    ┌──────┼──────┐   ┌─────┼─────┐    ┌─────┼─────┐
    ▼      ▼      ▼   ▼     ▼     ▼    ▼     ▼     ▼
  Pass1  Pass2  Pass3 Pass1 Pass2 Pass3 Pass1 Pass2 Pass3
  (d=0)  (d=1)  (d=2) (d=0) (d=1) (d=2) (d=0) (d=1) (d=2)
```

### 데이터 예시

**tracking_session (9개)**

| id | mst_id | detail_id | satellite_id | tracking_mode |
|----|--------|-----------|--------------|---------------|
| 1 | 1 | 0 | 25544 | PASS_SCHEDULE |
| 2 | 1 | 1 | 25544 | PASS_SCHEDULE |
| 3 | 1 | 2 | 25544 | PASS_SCHEDULE |
| 4 | 2 | 0 | 43013 | PASS_SCHEDULE |
| ... | ... | ... | ... | ... |

**tracking_trajectory (각 Pass별 7 DataType × N포인트)**

| timestamp | session_id | data_type | index | azimuth | elevation | train |
|-----------|------------|-----------|-------|---------|-----------|-------|
| 09:00:00 | 1 | original | 0 | 45.0 | 5.0 | NULL |
| 09:00:00 | 1 | axis_transformed | 0 | 45.0 | 5.0 | 0.0 |
| 09:00:00 | 1 | final_transformed | 0 | 45.0 | 5.0 | 0.0 |
| 09:00:00 | 1 | keyhole_axis_transformed | 0 | 45.0 | 5.0 | 15.0 |
| ... | ... | ... | ... | ... | ... | ... |

**tracking_result (실측치)**

| timestamp | session_id | used_data_type | index | cmd_az | actual_az |
|-----------|------------|----------------|-------|--------|-----------|
| 09:00:00.030 | 1 | final_transformed | 0 | 45.0 | 44.98 |
| 09:00:00.060 | 1 | final_transformed | 1 | 45.5 | 45.48 |
| 09:00:01.000 | 1 | keyhole_final_transformed | 30 | 85.0 | 84.95 |

---

## 5. 핵심 JOIN 쿼리

### 이론치 + 실측치 비교 조회

```sql
-- 특정 Pass의 이론치와 실측치를 함께 조회
SELECT
    r.timestamp AS actual_timestamp,
    r.index AS result_index,
    r.used_data_type,
    r.tracking_cmd_azimuth,
    r.tracking_actual_azimuth,
    t.azimuth AS theoretical_azimuth,
    t.elevation AS theoretical_elevation,
    t.train AS theoretical_train
FROM tracking_result r
JOIN tracking_trajectory t
    ON r.session_id = t.session_id
   AND r.used_data_type = t.data_type
   AND r.theoretical_index = t.index
WHERE r.session_id = :sessionId
ORDER BY r.timestamp;
```

### 위성별 전체 Pass 조회 (분석 화면용)

```sql
-- 위성 A의 모든 Pass 조회
SELECT
    s.id,
    s.mst_id,
    s.detail_id,
    s.satellite_name,
    s.start_time,
    s.end_time,
    s.max_elevation,
    s.keyhole_detected,
    s.tle_line_1,
    s.tle_line_2,
    COUNT(DISTINCT t.data_type) AS trajectory_types,
    COUNT(r.index) AS result_count
FROM tracking_session s
LEFT JOIN tracking_trajectory t ON s.id = t.session_id
LEFT JOIN tracking_result r ON s.id = r.session_id
WHERE s.satellite_id = :satelliteId
  AND s.tracking_mode = 'PASS_SCHEDULE'
GROUP BY s.id
ORDER BY s.start_time DESC;
```

---

## 6. TimescaleDB 최적화 분석

### 현재 구조 평가

| 항목 | 상태 | 설명 |
|------|------|------|
| Hypertable 파티셔닝 | ✅ | timestamp 기준, 최적 |
| Narrow Table | ✅ | 압축 효율 80%+ |
| 세그먼트 키 | ✅ | session_id 기준 압축 |
| master/detail 호환 | ✅ | FK 참조 정상 작동 |

### 압축 설정 (기존 유지)

```sql
ALTER TABLE tracking_trajectory SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'session_id'
);

ALTER TABLE tracking_result SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'session_id'
);
```

### 인덱스 전략

```sql
-- 기존 인덱스 (유지)
CREATE INDEX idx_tt_session ON tracking_trajectory(session_id);
CREATE INDEX idx_tt_session_timestamp ON tracking_trajectory(session_id, timestamp);
CREATE INDEX idx_tr_session ON tracking_result(session_id);

-- V007 추가 권장
CREATE INDEX idx_tt_session_datatype ON tracking_trajectory(session_id, data_type);
CREATE INDEX idx_tr_used_datatype ON tracking_result(used_data_type);
```

---

## 7. V007 마이그레이션 요구사항

### 제거할 컬럼

**tracking_result**
```sql
-- position 컬럼 (cmd/actual과 중복)
DROP COLUMN IF EXISTS position_azimuth;
DROP COLUMN IF EXISTS position_elevation;
DROP COLUMN IF EXISTS position_train;

-- error 컬럼 (파생값 - 엑셀에서 계산)
DROP COLUMN IF EXISTS azimuth_error;
DROP COLUMN IF EXISTS elevation_error;
DROP COLUMN IF EXISTS train_error;
DROP COLUMN IF EXISTS total_error;
```

### 추가할 컬럼

**tracking_result**
```sql
-- 이론치-실측치 연계를 위한 핵심 컬럼
ADD COLUMN used_data_type VARCHAR(50) NOT NULL DEFAULT 'final_transformed';
```

### V007 마이그레이션 초안

```sql
-- V007__Tracking_result_cleanup.sql

-- 1. position 컬럼 제거 (중복)
ALTER TABLE tracking_result
DROP COLUMN IF EXISTS position_azimuth,
DROP COLUMN IF EXISTS position_elevation,
DROP COLUMN IF EXISTS position_train;

-- 2. error 컬럼 제거 (파생값)
ALTER TABLE tracking_result
DROP COLUMN IF EXISTS azimuth_error,
DROP COLUMN IF EXISTS elevation_error,
DROP COLUMN IF EXISTS train_error,
DROP COLUMN IF EXISTS total_error;

-- 3. used_data_type 추가 (핵심)
ALTER TABLE tracking_result
ADD COLUMN IF NOT EXISTS used_data_type VARCHAR(50) NOT NULL DEFAULT 'final_transformed';

-- 4. 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_tr_used_datatype ON tracking_result(used_data_type);
CREATE INDEX IF NOT EXISTS idx_tt_session_datatype ON tracking_trajectory(session_id, data_type);

-- 5. 제약조건
ALTER TABLE tracking_result
ADD CONSTRAINT chk_tr_used_datatype CHECK (
    used_data_type IN (
        'original',
        'axis_transformed',
        'final_transformed',
        'keyhole_axis_transformed',
        'keyhole_final_transformed',
        'keyhole_optimized_axis',
        'keyhole_optimized_final'
    )
);
```

---

## 8. 백엔드 수정 필요사항

### Entity 수정

```kotlin
// TrackingResultEntity.kt
data class TrackingResultEntity(
    // ... 기존 필드 ...

    // 추가
    val usedDataType: String,  // 어떤 이론치를 사용했는지

    // 제거
    // val positionAzimuth: Double?
    // val positionElevation: Double?
    // val positionTrain: Double?
    // val azimuthError: Double?
    // val elevationError: Double?
    // val trainError: Double?
    // val totalError: Double?
)
```

### Repository 수정

```kotlin
// TrackingResultRepository.kt
// INSERT 쿼리에 used_data_type 추가
// SELECT 쿼리에서 position/error 컬럼 제거
```

### 저장 로직 수정

```kotlin
// BatchStorageManager.kt
fun mapToTrackingResult(icdData: ICDData, keyholeActive: Boolean): TrackingResultEntity {
    return TrackingResultEntity(
        // ...
        usedDataType = if (keyholeActive) "keyhole_final_transformed" else "final_transformed"
    )
}
```

---

## 9. 검증 체크리스트

### 기능 검증

- [ ] 7가지 DataType 모두 tracking_trajectory에 저장되는가?
- [ ] used_data_type으로 이론치-실측치 JOIN이 가능한가?
- [ ] PassSchedule 다중 위성/스케줄 시나리오 정상 동작하는가?
- [ ] 분석 화면에서 TLE 정보, 이론치, 실측치 조회 가능한가?

### 성능 검증

- [ ] Hypertable 압축 정상 동작하는가?
- [ ] 인덱스가 쿼리 플랜에 사용되는가?
- [ ] 대량 데이터 INSERT 성능 문제 없는가?

---

## 10. 결론

### 설계 검증 결과

| 항목 | 결과 |
|------|------|
| 3개 테이블 연계 | ✅ 정상 (session_id + data_type + index) |
| 7가지 DataType | ✅ tracking_trajectory에 data_type 컬럼으로 저장 |
| 이론치-실측치 연계 | ✅ used_data_type + theoretical_index로 JOIN |
| TimescaleDB 호환 | ✅ master/detail 구조 완벽 호환 |
| PassSchedule 지원 | ✅ mst_id + detail_id로 다중 위성/스케줄 구분 |

### 다음 단계

1. V007 마이그레이션 작성 및 적용
2. Entity/Repository 수정
3. 저장 로직에 usedDataType 매핑 추가
4. 분석 화면 개발 시 JOIN 쿼리 활용

---

## 11. 발견된 이슈 (CRITICAL)

### 11.1 sessionId 연동 버그 🔴

**심각도**: CRITICAL
**영향**: tracking_result와 tracking_trajectory JOIN 불가

**문제**:
```
EphemerisService.saveRealtimeTrackingData()
    ↓
createRealtimeTrackingData()  ← mstId, detailId만 있음 (sessionId 없음!)
    ↓
Map { mstId, detailId, ... }  (sessionId 누락)
    ↓
batchStorageManager.addToBatch(realtimeData)
    ↓
mapToTrackingResult()
    ↓
val sessionId = (data["sessionId"] as? Number)?.toLong() ?: 0L  ← 0L로 저장!
```

**결과**:
- `tracking_result.session_id = 0` 으로 저장됨
- 이론치(trajectory)와 실측치(result) JOIN 불가
- 분석 화면에서 세션별 데이터 조회 불가

**수정 필요**:
```kotlin
// EphemerisService.kt - createRealtimeTrackingData()에 sessionId 추가
return mapOf(
    "sessionId" to currentSessionId,  // ✅ 추가 필요
    "mstId" to mstId,
    "detailId" to detailId,
    // ...
)
```

### 11.2 PassSchedule V006 정책 미반영 🟡

**심각도**: HIGH
**영향**: PassSchedule에서 1 Pass = 7 Sessions로 저장됨

**현재 상태**:
| 모드 | 1 Pass = 1 Session | 상태 |
|------|-------------------|------|
| Ephemeris | ✅ V006 정책 적용 | 정상 |
| PassSchedule | ❌ data_type별 분리 저장 | 미반영 |

**EphemerisDataRepository.kt** (V006 반영됨):
```kotlin
// V006: (mstId, detailId) 기준으로 그룹화하여 1 Pass = 1 Session 보장
val groupedMst = mstData.groupBy { mst ->
    Pair(mstId, detailId)
}
```

**PassScheduleDataRepository.kt** (미반영):
```kotlin
// data_type별로 각각 저장 → 1 Pass = 7 Sessions
mstData.forEach { mst ->
    val session = mapMstToSession(satelliteId, mst, sessionDtlData.size)
    sessionRepository.save(session)
}
```

**수정 필요**:
- PassScheduleDataRepository에도 Ephemeris와 동일한 그룹화 로직 적용

---

## 12. 수정 우선순위

| # | 이슈 | 심각도 | 영향 | 우선순위 |
|---|------|--------|------|----------|
| 1 | sessionId 연동 버그 | CRITICAL | 데이터 연계 불가 | P0 |
| 2 | PassSchedule V006 미반영 | HIGH | 중복 세션 생성 | P1 |
| 3 | V007 position/error 제거 | MEDIUM | 데이터 정리 | P2 |
| 4 | used_data_type 추가 | MEDIUM | 이론치 추적 | P2 |

---

## 13. 데이터 흐름 전체 구조

### 이론치 저장 (스케줄 생성 시)

```
[스케줄 생성 버튼 클릭]
        ↓
SatelliteTrackingProcessor.processSatelliteData()
        ↓
ProcessedTrackingData (7가지 DataType × Mst/Dtl)
        ↓
EphemerisDataRepository.saveToDatabase()  ← Ephemeris 모드
PassScheduleDataRepository.saveToDatabase()  ← PassSchedule 모드
        ↓
┌─────────────────────────────────────────────────┐
│  tracking_session (1개/Pass)                    │
│  tracking_trajectory (7 DataType × N포인트)    │
└─────────────────────────────────────────────────┘
```

### 실측치 저장 (추적 시간)

```
[추적 시작 → 30ms 타이머 콜백]
        ↓
EphemerisService.handleTracking()
        ↓
saveRealtimeTrackingData()
        ↓
createRealtimeTrackingData()  ← ⚠️ sessionId 누락!
        ↓
BatchStorageManager.addToBatch()
        ↓
processBatch() → saveToDatabase()
        ↓
┌─────────────────────────────────────────────────┐
│  tracking_result (실측치)                       │
│  session_id = 0  ← ❌ 버그!                     │
└─────────────────────────────────────────────────┘
```

### 올바른 흐름 (수정 후)

```
[추적 시작]
        ↓
SessionID 생성 및 보관 (currentSessionId)
        ↓
[30ms 타이머 콜백]
        ↓
createRealtimeTrackingData(sessionId = currentSessionId)
        ↓
BatchStorageManager.addToBatch()
        ↓
┌─────────────────────────────────────────────────┐
│  tracking_result (실측치)                       │
│  session_id = currentSessionId  ← ✅ 정상      │
└─────────────────────────────────────────────────┘
```

---

**최종 수정:** 2026-01-22
**검토자:** db-master, architect, fullstack-helper, Explore agent