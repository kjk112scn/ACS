# Precision_Tracking_Enhancement

## 개요

**목적**: 이론치-실측치 매칭 정밀도 향상 (0.05° → 0.01°)
**상태**: 📋 계획됨 (시스템 안정화 후 적용)
**선행 작업**: Tracking_Session_Data_Enrichment 100% 완료

## 문제 정의

### 현재 상황

```
위성 추적 시:
- 이론치: 100ms 간격 계산된 위성 위치 (azimuth, elevation)
- 실측치: 하드웨어에서 실시간 수신되는 안테나 위치

문제: 이론치와 실측치 시간이 정확히 일치하지 않음
→ 현재 방식: 정수 인덱스 반올림으로 가장 가까운 이론치 선택
→ 결과: 최대 50ms 오차 → 약 0.05° 각도 오차 (1°/s 각속도 기준)
```

### 현재 코드 (문제점)

```kotlin
// EphemerisService.kt:1775-1786
val timeDifferenceMs = Duration.between(startTime, currentTime).toMillis()
val theoreticalIndex = (timeDifferenceMs / 100.0).toInt()  // ❌ 정수 변환으로 정밀도 손실
val theoreticalPoint = originalPassDetails[theoreticalIndex]  // ❌ 보간 없이 직접 매칭
```

### 목표

| 항목 | 현재 | 목표 |
|------|------|------|
| 인덱스 계산 | 정수 반올림 | 소수점 + 선형 보간 |
| 시간 오차 | 최대 50ms | 1ms 이하 |
| 각도 오차 (1°/s) | ~0.05° | **~0.001°** |
| 추가 기능 | - | 칼만 필터 준비 |

## 기술적 접근

### 1. 선형 보간 (Linear Interpolation)

```
현재: theoreticalIndex = 150 (정수)
개선: theoreticalIndex = 150.73 (소수점)
     → lowerIndex = 150, upperIndex = 151
     → fraction = 0.73
     → 보간값 = lower * (1-0.73) + upper * 0.73
```

```kotlin
// 개선된 보간 로직
fun interpolateTheoretical(
    passDetails: List<Map<String, Any?>>,
    targetTime: ZonedDateTime,
    startTime: ZonedDateTime
): InterpolatedResult {
    val timeDiffMs = Duration.between(startTime, targetTime).toMillis()
    val exactIndex = timeDiffMs / 100.0  // 소수점 유지

    val lowerIndex = exactIndex.toInt()
    val upperIndex = (lowerIndex + 1).coerceAtMost(passDetails.size - 1)
    val fraction = exactIndex - lowerIndex

    val lower = passDetails[lowerIndex]
    val upper = passDetails[upperIndex]

    return InterpolatedResult(
        azimuth = interpolate(lower["Azimuth"], upper["Azimuth"], fraction),
        elevation = interpolate(lower["Elevation"], upper["Elevation"], fraction),
        fraction = fraction,
        lowerIndex = lowerIndex,
        upperIndex = upperIndex
    )
}
```

### 2. 시간 기반 이진 검색 (더 정확)

이론치 데이터가 정확히 100ms 간격이 아닐 수 있으므로, 실제 타임스탬프 기반 검색:

```kotlin
fun findTimeBasedInterpolation(
    passDetails: List<Map<String, Any?>>,
    targetTime: ZonedDateTime
): InterpolatedResult {
    // 이진 검색으로 targetTime 전후 포인트 찾기
    val (lowerIdx, upperIdx) = binarySearchTimeRange(passDetails, targetTime)

    val lowerTime = passDetails[lowerIdx]["Time"] as ZonedDateTime
    val upperTime = passDetails[upperIdx]["Time"] as ZonedDateTime

    // 실제 시간 차이 기반 보간 비율
    val totalMs = Duration.between(lowerTime, upperTime).toMillis()
    val offsetMs = Duration.between(lowerTime, targetTime).toMillis()
    val fraction = offsetMs.toDouble() / totalMs

    return InterpolatedResult(/* ... */)
}
```

### 3. 칼만 필터 (향후 확장)

실시간 오차 피드백으로 예측 정확도 향상:

```
예측(이론치) → 측정(실측치) → 보정 → 다음 예측 개선
```

## DB 스키마 변경

### V006 마이그레이션 (미작성)

```sql
-- tracking_result 확장 (보간 상세)
ALTER TABLE tracking_result ADD COLUMN IF NOT EXISTS theoretical_timestamp TIMESTAMPTZ;
ALTER TABLE tracking_result ADD COLUMN IF NOT EXISTS time_offset_ms DOUBLE PRECISION;
ALTER TABLE tracking_result ADD COLUMN IF NOT EXISTS interpolation_fraction DOUBLE PRECISION;
ALTER TABLE tracking_result ADD COLUMN IF NOT EXISTS lower_theoretical_index INTEGER;
ALTER TABLE tracking_result ADD COLUMN IF NOT EXISTS upper_theoretical_index INTEGER;

-- 칼만 필터 (향후)
ALTER TABLE tracking_result ADD COLUMN IF NOT EXISTS kalman_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_result ADD COLUMN IF NOT EXISTS kalman_elevation DOUBLE PRECISION;
ALTER TABLE tracking_result ADD COLUMN IF NOT EXISTS kalman_gain DOUBLE PRECISION;

-- tracking_trajectory 확장
ALTER TABLE tracking_trajectory ADD COLUMN IF NOT EXISTS resolution_ms INTEGER DEFAULT 1000;
ALTER TABLE tracking_trajectory ADD COLUMN IF NOT EXISTS satellite_range DOUBLE PRECISION;
ALTER TABLE tracking_trajectory ADD COLUMN IF NOT EXISTS satellite_altitude DOUBLE PRECISION;
```

### 컬럼 설명

| 테이블 | 컬럼 | 타입 | 용도 |
|--------|------|------|------|
| tracking_result | theoretical_timestamp | TIMESTAMPTZ | 매칭된 이론치의 정확한 시간 |
| tracking_result | time_offset_ms | DOUBLE | 이론치-실측치 시간차 (ms) |
| tracking_result | interpolation_fraction | DOUBLE | 보간 비율 (0.0~1.0) |
| tracking_result | lower_theoretical_index | INTEGER | 보간 하한 인덱스 |
| tracking_result | upper_theoretical_index | INTEGER | 보간 상한 인덱스 |
| tracking_result | kalman_azimuth | DOUBLE | 칼만 보정 방위각 |
| tracking_result | kalman_elevation | DOUBLE | 칼만 보정 고도각 |
| tracking_result | kalman_gain | DOUBLE | 칼만 이득 (0~1) |
| tracking_trajectory | resolution_ms | INTEGER | 데이터 해상도 (ms) |
| tracking_trajectory | satellite_range | DOUBLE | 위성까지 거리 (km) |
| tracking_trajectory | satellite_altitude | DOUBLE | 위성 고도 (km) |

## 코드 변경 범위

| 파일 | 변경 내용 |
|------|----------|
| EphemerisService.kt | `createRealtimeTrackingData()` 선형 보간 적용 |
| EphemerisService.kt | `findTimeBasedInterpolation()` 함수 추가 |
| TrackingResultEntity.kt | 8개 필드 추가 |
| TrackingTrajectoryEntity.kt | 3개 필드 추가 |
| TrackingResultRepository.kt | 컬럼 매핑 업데이트 |
| TrackingTrajectoryRepository.kt | 컬럼 매핑 업데이트 |

## 전문가 검토 결과

### 호환성 검증 (2026-01-21)

| 항목 | 결과 | 비고 |
|------|------|------|
| 기존 데이터 호환 | ✅ 안전 | 모든 컬럼 NULL 허용 |
| V004→V005→V006 순서 | ✅ 안전 | 의존성 없음 |
| TimescaleDB Hypertable | ⚠️ 주의 | 압축 청크 해제 필요 |
| Entity 동기화 | ✅ 유연 | 실제 구현 시 추가 |
| 쿼리 성능 | ✅ 영향 없음 | NULL 컬럼 오버헤드 최소 |

### 주의사항

```sql
-- V006 적용 전 압축 청크 해제 필요 (TimescaleDB)
SELECT decompress_chunk(chunk_name, if_compressed => true)
FROM timescaledb_information.chunks
WHERE hypertable_name IN ('tracking_result', 'tracking_trajectory')
  AND is_compressed = true;
```

## 구현 순서

```
Phase 1: DB 스키마
  1. V006 마이그레이션 작성
  2. Entity 필드 추가
  3. Repository 매핑 업데이트

Phase 2: 선형 보간 구현
  1. createRealtimeTrackingData() 수정
  2. interpolateTheoretical() 함수 구현
  3. 보간 메타데이터 저장

Phase 3: 시간 기반 검색 (선택)
  1. binarySearchTimeRange() 구현
  2. findTimeBasedInterpolation() 구현

Phase 4: 칼만 필터 (향후)
  1. 칼만 필터 알고리즘 구현
  2. 실시간 보정 로직 적용
```

## 관련 문서

- [DESIGN.md](DESIGN.md) - 상세 설계
- [V006_MIGRATION.sql](V006_MIGRATION.sql) - 마이그레이션 초안
- `docs/work/active/Tracking_Session_Data_Enrichment/` - 선행 작업
- `backend/src/.../EphemerisService.kt:1775` - 현재 보간 로직 위치

## 실행 트리거

```
"정밀 추적 계획 진행해줘"
"V006 마이그레이션 작성해줘"
"선형 보간 구현해줘"
```