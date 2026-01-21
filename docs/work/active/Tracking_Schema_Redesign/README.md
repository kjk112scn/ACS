# Tracking Schema Redesign (스키마 재설계)

## 개요

**목적**: tracking_session, tracking_trajectory, tracking_result 3개 테이블 역할 정상화
**요청일**: 2026-01-21
**상태**: 🚧 진행 중
**접근법**: 클린 리셋 (개발 환경 - 기존 데이터 삭제 후 재구성)

## 요구사항

- [x] tracking_session: 1 Pass = 1 Session (data_type별 분리 → mst_id+detail_id로 그룹화)
- [x] tracking_trajectory: 누락된 컬럼 추가 (train_rate, range, altitude, velocity)
- [x] tracking_result: 이론치 제거, ICD 실측치 + 정밀추적 컬럼 추가
- [x] tracking_session ↔ tle_cache 연동 (FK + 스냅샷)

## 진행 상황

| Phase | 상태 | 내용 |
|-------|:----:|------|
| Phase 0 | ✅ | V006 마이그레이션 작성 |
| Phase 1 | ⏳ | Entity 업데이트 |
| Phase 2 | ⏳ | Repository 매핑 수정 |
| Phase 3 | ⏳ | 저장 로직 수정 |
| 검증 | ⏳ | BE 재시작 + DB 테스트 |

## 문제 정의

### 현재 구조 문제

| 테이블 | 의도된 역할 | 현재 상태 | 문제 |
|--------|------------|----------|------|
| **tracking_session** | 1 Pass = 1 Session | 1 Pass = 7 Sessions | data_type별 분리 |
| **tracking_trajectory** | 이론치 전체 | 기본 6개만 | range, altitude 누락 |
| **tracking_result** | 실측치만 | 이론치 20개 + 실측치 | 역할 혼합 |
| **detail_id** | Pass 구분자 | 항상 0 | 미사용 |
| **tle_cache 연동** | 어떤 TLE로 계산했는지 | 연결 없음 | 추적 불가 |

### 올바른 데이터 흐름 (재설계 후)

```
[TLE 데이터] → [Orekit] → [RAM 캐시]
                              ↓
                         [DB 저장]
                              ↓
    ┌─────────────────────────────────────┐
    │ tracking_session (1개/Pass)         │
    │   - mst_id로 그룹화                 │
    │   - detail_id로 Pass 구분           │
    │   - tle_cache_id (FK) + 스냅샷      │
    └─────────────────────────────────────┘
    ┌─────────────────────────────────────┐
    │ tracking_trajectory (이론치 전체)   │
    │   - 7개 data_type × 각 필드         │
    │   - azimuth, elevation, train       │
    │   - range, altitude, velocity       │
    └─────────────────────────────────────┘
    ┌─────────────────────────────────────┐
    │ tracking_result (실측치만)          │
    │   - ICD: tracking_cmd_*, actual_*   │
    │   - 오차: azimuth_error, etc.       │
    │   - 정밀추적: interpolation_*       │
    └─────────────────────────────────────┘
```

## V006 마이그레이션 요약

**파일**: `V006__Schema_redesign_tracking_tables.sql`

### 1. tracking_trajectory 변경

| 작업 | 컬럼 |
|------|------|
| **추가 (+4)** | train_rate, satellite_range, satellite_altitude, satellite_velocity |

### 2. tracking_session 변경

| 작업 | 내용 |
|------|------|
| **UNIQUE 변경** | `(mst_id, data_type, tracking_mode)` → `(mst_id, detail_id, tracking_mode)` |
| **data_type** | nullable로 변경 (deprecated) |
| **TLE 연동 (+4)** | tle_cache_id (FK), tle_line_1, tle_line_2, tle_epoch |

### 3. tracking_result 변경

| 작업 | 컬럼 |
|------|------|
| **제거 (-17)** | original_*, transformed_*, final_*, *_rate, *_acceleration, satellite_*, interpolation_type |
| **추가 (+15)** | theoretical_timestamp, time_offset_ms, interpolation_fraction, lower/upper_theoretical_index, tracking_*_time, tracking_cmd_*, tracking_actual_*, kalman_* |

## 다음 단계

### Phase 1: Entity 업데이트

| Entity | 변경 내용 |
|--------|----------|
| TrackingSessionEntity.kt | +4 필드 (tle_cache_id, tle_line_1, tle_line_2, tle_epoch) |
| TrackingTrajectoryEntity.kt | +4 필드 (train_rate, satellite_*) |
| TrackingResultEntity.kt | -17 필드 (이론치), +15 필드 (ICD+정밀추적) |

### Phase 2: Repository 매핑 수정

| Repository | 변경 내용 |
|------------|----------|
| TrackingSessionRepository.kt | TLE 연동 컬럼 매핑 추가 |
| TrackingTrajectoryRepository.kt | 새 컬럼 매핑 추가 |
| TrackingResultRepository.kt | 이론치 매핑 제거, ICD 매핑 추가 |

### Phase 3: 저장 로직 수정

| 파일 | 함수 | 변경 내용 |
|------|------|----------|
| EphemerisDataRepository.kt | mapMstToSession() | TLE 정보 매핑 추가 |
| EphemerisDataRepository.kt | mapDtlToTrajectory() | range, altitude, velocity 매핑 |
| BatchStorageManager.kt | mapToTrackingResult() | 이론치 제거, ICD 데이터 매핑 |

## 롤백 계획

```sql
-- V006 롤백 (필요시)
-- 주의: 데이터 복구 불가 (클린 리셋이므로)

-- tracking_trajectory 컬럼 제거
ALTER TABLE tracking_trajectory
DROP COLUMN IF EXISTS train_rate,
DROP COLUMN IF EXISTS satellite_range,
DROP COLUMN IF EXISTS satellite_altitude,
DROP COLUMN IF EXISTS satellite_velocity;

-- tracking_session TLE 컬럼 제거
ALTER TABLE tracking_session
DROP COLUMN IF EXISTS tle_cache_id,
DROP COLUMN IF EXISTS tle_line_1,
DROP COLUMN IF EXISTS tle_line_2,
DROP COLUMN IF EXISTS tle_epoch;

-- tracking_session UNIQUE 복원
ALTER TABLE tracking_session
DROP CONSTRAINT IF EXISTS uk_tracking_session;
ALTER TABLE tracking_session
ADD CONSTRAINT uk_tracking_session UNIQUE (mst_id, data_type, tracking_mode);
ALTER TABLE tracking_session
ALTER COLUMN data_type SET NOT NULL;

-- tracking_result 컬럼 복원 (V001 참조)
```

## 관련 문서

- [DESIGN.md](DESIGN.md) - 설계 문서
- [PROGRESS.md](PROGRESS.md) - 진행 상황
- [V006 마이그레이션](../../../backend/src/main/resources/db/migration/V006__Schema_redesign_tracking_tables.sql)