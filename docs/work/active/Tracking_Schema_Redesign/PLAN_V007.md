# Tracking Schema V007 버그픽스 계획

> **작성일**: 2026-01-23
> **목적**: V007 마이그레이션 + BE 코드 정리
> **사전 결정**: [ADR-007](../../../decisions/ADR-007-tracking-session-key-design.md) - mst_id + detail_id 구조 유지

---

## 1. 작업 요약

| Phase | 작업 | 상태 | 검토 필요 |
|-------|------|:----:|:--------:|
| **Phase 6** | V007 마이그레이션 SQL | ⏳ 대기 | ✅ |
| **Phase 7** | BE Entity/Repository 수정 | ⏳ 대기 | ✅ |
| **Phase 8** | DB 적용 + 검증 | ⏳ 대기 | - |

### 변경 목적

1. **position 컬럼 제거**: cmd/actual과 중복됨
2. **error 컬럼 제거**: 파생값 (엑셀에서 계산)
3. **used_data_type 추가**: 이론치-실측치 연계용

---

## 2. Phase 6: V007 마이그레이션

### 파일 생성

```
📁 backend/src/main/resources/db/migration/
   └─ V007__Tracking_result_cleanup.sql (신규)
```

### SQL 내용

```sql
-- ================================================================
-- V007__Tracking_result_cleanup.sql
-- 목적: tracking_result 테이블 정리 + used_data_type 추가
-- ================================================================

-- 1. position 컬럼 제거 (cmd/actual과 중복)
ALTER TABLE tracking_result
DROP COLUMN IF EXISTS position_azimuth,
DROP COLUMN IF EXISTS position_elevation,
DROP COLUMN IF EXISTS position_train;

-- 2. error 컬럼 제거 (파생값 - 엑셀에서 계산)
ALTER TABLE tracking_result
DROP COLUMN IF EXISTS azimuth_error,
DROP COLUMN IF EXISTS elevation_error,
DROP COLUMN IF EXISTS train_error,
DROP COLUMN IF EXISTS total_error;

-- 3. used_data_type 추가 (이론치-실측치 연계 핵심)
ALTER TABLE tracking_result
ADD COLUMN IF NOT EXISTS used_data_type VARCHAR(50) NOT NULL DEFAULT 'final_transformed';

-- 4. 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_tr_used_datatype ON tracking_result(used_data_type);
CREATE INDEX IF NOT EXISTS idx_tt_session_datatype ON tracking_trajectory(session_id, data_type);

-- 5. CHECK 제약조건 (유효한 data_type만 허용)
ALTER TABLE tracking_result
ADD CONSTRAINT IF NOT EXISTS chk_tr_used_datatype CHECK (
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

### 제거되는 컬럼 (7개)

| 컬럼명 | Entity 위치 | 이유 |
|--------|-------------|------|
| `position_azimuth` | L70-71 | `cmd_azimuth`/`actual_azimuth`와 중복 |
| `position_elevation` | L73-74 | `cmd_elevation`/`actual_elevation`와 중복 |
| `position_train` | L76-77 | `cmd_train`/`actual_train`와 중복 |
| `azimuth_error` | L108-109 | 파생값: cmd - actual |
| `elevation_error` | L111-112 | 파생값: cmd - actual |
| `train_error` | L114-115 | 파생값: cmd - actual |
| `total_error` | L117-118 | 파생값: sqrt(az² + el² + tr²) |

### 추가되는 컬럼 (1개)

| 컬럼명 | 타입 | 기본값 | 용도 |
|--------|------|--------|------|
| `used_data_type` | VARCHAR(50) | 'final_transformed' | 실측치가 어떤 이론치를 사용했는지 |

---

## 3. Phase 7: BE 코드 수정

### 3.1 TrackingResultEntity.kt

**경로**: `backend/src/main/kotlin/com/gtlsystems/acs_api/tracking/entity/TrackingResultEntity.kt`

#### 제거할 코드 (7개 필드)

```kotlin
// ❌ 제거: 라인 69-77 (위치값)
    // ===== 위치값 =====
    @Column("position_azimuth")
    val positionAzimuth: Double? = null,

    @Column("position_elevation")
    val positionElevation: Double? = null,

    @Column("position_train")
    val positionTrain: Double? = null,

// ❌ 제거: 라인 107-118 (오차)
    // ===== 오차 =====
    @Column("azimuth_error")
    val azimuthError: Double? = null,

    @Column("elevation_error")
    val elevationError: Double? = null,

    @Column("train_error")
    val trainError: Double? = null,

    @Column("total_error")
    val totalError: Double? = null,
```

#### 추가할 코드 (1개 필드)

```kotlin
// ✅ 추가: 라인 67 근처 (실측값 다음)
    // ===== V007: 이론치 연계용 =====
    @Column("used_data_type")
    val usedDataType: String = "final_transformed",
```

#### 체크리스트

- [ ] L69-77: `positionAzimuth`, `positionElevation`, `positionTrain` 제거
- [ ] L107-118: `azimuthError`, `elevationError`, `trainError`, `totalError` 제거
- [ ] L67 근처: `usedDataType` 필드 추가

---

### 3.2 TrackingResultRepository.kt

**경로**: `backend/src/main/kotlin/com/gtlsystems/acs_api/tracking/repository/TrackingResultRepository.kt`

#### INSERT 쿼리 수정 (L29-55)

```diff
 // 라인 36 (position 컬럼) 제거
- position_azimuth, position_elevation, position_train,

 // 라인 40 (error 컬럼) 제거
- azimuth_error, elevation_error, train_error, total_error,

 // 라인 36 근처 추가
+ used_data_type,

 // VALUES 부분 (L49, L53)
- :positionAzimuth, :positionElevation, :positionTrain,
- :azimuthError, :elevationError, :trainError, :totalError,
+ :usedDataType,
```

#### bind 구문 수정 (L57-103)

```diff
 // 라인 76-78 제거
-           // 위치값
-           .bindNullable("positionAzimuth", entity.positionAzimuth)
-           .bindNullable("positionElevation", entity.positionElevation)
-           .bindNullable("positionTrain", entity.positionTrain)

 // 라인 90-93 제거
-           // 오차
-           .bindNullable("azimuthError", entity.azimuthError)
-           .bindNullable("elevationError", entity.elevationError)
-           .bindNullable("trainError", entity.trainError)
-           .bindNullable("totalError", entity.totalError)

 // 라인 74 근처 추가
+           // V007: 이론치 연계용
+           .bind("usedDataType", entity.usedDataType)
```

#### mapRowToEntity() 수정 (L145-194)

```diff
 // 라인 166-168 제거
-           positionAzimuth = row.get("position_azimuth", Double::class.java),
-           positionElevation = row.get("position_elevation", Double::class.java),
-           positionTrain = row.get("position_train", Double::class.java),

 // 라인 180-183 제거
-           azimuthError = row.get("azimuth_error", Double::class.java),
-           elevationError = row.get("elevation_error", Double::class.java),
-           trainError = row.get("train_error", Double::class.java),
-           totalError = row.get("total_error", Double::class.java),

 // 라인 165 근처 추가
+           usedDataType = row.get("used_data_type", String::class.java) ?: "final_transformed",
```

#### 체크리스트

- [ ] L36: INSERT 컬럼에서 `position_*` 3개 제거
- [ ] L40: INSERT 컬럼에서 `*_error` 4개 제거
- [ ] L36 근처: INSERT 컬럼에 `used_data_type` 추가
- [ ] L49: VALUES에서 `:position*` 3개 제거
- [ ] L53: VALUES에서 `:*Error` 4개 제거
- [ ] L49 근처: VALUES에 `:usedDataType` 추가
- [ ] L76-78: bind에서 position 3개 제거
- [ ] L90-93: bind에서 error 4개 제거
- [ ] L74 근처: bind에 `.bind("usedDataType", ...)` 추가
- [ ] L166-168: mapRowToEntity에서 position 3개 제거
- [ ] L180-183: mapRowToEntity에서 error 4개 제거
- [ ] L165 근처: mapRowToEntity에 `usedDataType` 추가

---

### 3.3 BatchStorageManager.kt

**경로**: `backend/src/main/kotlin/com/gtlsystems/acs_api/service/system/BatchStorageManager.kt`

#### mapToTrackingResult() 수정 (L229-304)

```diff
 // 라인 272-275 제거
-           // ===== 위치값 =====
-           positionAzimuth = (data["positionAzimuth"] as? Number)?.toDouble(),
-           positionElevation = (data["positionElevation"] as? Number)?.toDouble(),
-           positionTrain = (data["positionTrain"] as? Number)?.toDouble(),

 // 라인 288-292 제거
-           // ===== 오차 =====
-           azimuthError = (data["azimuthError"] as? Number)?.toDouble(),
-           elevationError = (data["elevationError"] as? Number)?.toDouble(),
-           trainError = (data["trainError"] as? Number)?.toDouble(),
-           totalError = (data["totalError"] as? Number)?.toDouble(),

 // 라인 272 근처 추가
+           // ===== V007: 이론치 연계용 =====
+           // ✅ 수정: isKeyhole 하나로만 결정 (EphemerisService L1768-1774 참조)
+           // - Keyhole 미발생: final_transformed
+           // - Keyhole 발생: keyhole_optimized_final_transformed (항상 최적화 버전)
+           usedDataType = if (data["keyholeActive"] as? Boolean == true) {
+               "keyhole_optimized_final_transformed"
+           } else {
+               "final_transformed"
+           },
```

#### 체크리스트

- [ ] L272-275: position 매핑 제거 (3줄)
- [ ] L288-292: error 매핑 제거 (4줄)
- [ ] L272 근처: `usedDataType` 매핑 추가 (keyholeActive 기반만 - 2가지 케이스)

---

## 4. Phase 8: 검증

### 빌드 확인

```bash
cd backend && ./gradlew clean build -x test
```

### DB 마이그레이션 확인

```bash
# BE 재시작 → Flyway V007 자동 적용
```

### 스키마 검증

```sql
-- 제거된 컬럼 확인 (0 rows 예상)
SELECT column_name FROM information_schema.columns
WHERE table_name = 'tracking_result'
AND column_name IN (
    'position_azimuth', 'position_elevation', 'position_train',
    'azimuth_error', 'elevation_error', 'train_error', 'total_error'
);

-- 추가된 컬럼 확인 (1 row 예상)
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tracking_result'
AND column_name = 'used_data_type';
```

### 데이터 저장 테스트

```sql
-- 새 데이터 저장 후 확인
SELECT session_id, used_data_type, COUNT(*)
FROM tracking_result
WHERE timestamp > NOW() - INTERVAL '1 hour'
GROUP BY session_id, used_data_type;
```

---

## 5. 수정 파일 요약 (정확한 라인)

| # | 파일 | 수정 라인 |
|---|------|----------|
| 1 | `V007__Tracking_result_cleanup.sql` | **신규** |
| 2 | `tracking/entity/TrackingResultEntity.kt` | L69-77 제거, L107-118 제거, L67 추가 |
| 3 | `tracking/repository/TrackingResultRepository.kt` | L36/40/49/53 (SQL), L74-78/90-93 (bind), L165-168/180-183 (map) |
| 4 | `service/system/BatchStorageManager.kt` | L272-275 제거, L288-292 제거, L272 추가 |

---

## 6. 리스크 및 롤백

### 리스크

| 리스크 | 영향 | 대응 |
|--------|------|------|
| 기존 데이터 position/error 손실 | 낮음 | cmd/actual에서 계산 가능 |
| Flyway 마이그레이션 실패 | 중간 | V006만 적용하고 V007 보류 |

### 롤백 방법

```sql
-- V007 롤백 (필요시)
ALTER TABLE tracking_result
ADD COLUMN IF NOT EXISTS position_azimuth DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS position_elevation DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS position_train DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS azimuth_error DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS elevation_error DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS train_error DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS total_error DOUBLE PRECISION;

ALTER TABLE tracking_result
DROP COLUMN IF EXISTS used_data_type;
```

---

## 7. 검토 요청 사항

### 검토자 확인 필요

- [x] **Phase 6 SQL**: 컬럼 제거/추가 적절한가? ✅
- [x] **Phase 7 Entity L67**: usedDataType 기본값 `final_transformed` 적절한가? ✅
- [x] **Phase 7 BatchStorageManager L272**: usedDataType 결정 로직 ✅

### usedDataType 결정 로직 (검토 완료)

**EphemerisService.kt L1768-1774 분석 결과:**

```kotlin
val finalDataType = if (isKeyhole) {
    "keyhole_optimized_final_transformed"  // Keyhole이면 항상 최적화 버전
} else {
    "final_transformed"  // 기본값
}
```

| 조건 | usedDataType | 비고 |
|------|-------------|------|
| Keyhole 미발생 | `final_transformed` | 기본 |
| Keyhole 발생 | `keyhole_optimized_final_transformed` | 항상 최적화 버전 |

**참고:** `keyhole_final_transformed`는 **중간 계산용**으로만 사용되며, 실제 안테나 명령(tracking_result)에는 저장되지 않음.

### 결정 완료

| 항목 | 결정 | 근거 |
|------|------|------|
| V007 적용 시점 | 지금 | 데이터 정리 필요 |
| used_data_type 결정 로직 | keyholeActive만 | EphemerisService 코드 분석 (isKeyhole 하나로 결정) |

---

**최종 수정**: 2026-01-23
**참조**: [DESIGN.md](DESIGN.md), [ADR-007](../../../decisions/ADR-007-tracking-session-key-design.md)