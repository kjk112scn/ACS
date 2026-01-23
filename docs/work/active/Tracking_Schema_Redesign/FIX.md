# Tracking Schema V006 버그 수정 기록

> **통합 문서**: 시간순 버그 수정 기록
> **마지막 업데이트**: 2026-01-23

---

## 2026-01-22 오전: P0~P1 (sessionId 연동)

### P0: sessionId 연동 버그

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 CRITICAL |
| **상태** | ✅ 수정 완료 |

**증상**: tracking_result.session_id = 0 저장

**원인**: `createRealtimeTrackingData()`에서 sessionId를 Map에 포함하지 않음

**수정**:
- EphemerisService.kt: getSessionIdByMstAndDetail() 메서드 추가
- createRealtimeTrackingData()에 sessionId 조회/추가

### P0-1: trackingMode 불일치

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 CRITICAL |
| **상태** | ✅ 수정 완료 |

**증상**: sessionId 조회 항상 실패

**원인**: "ephemeris_designation" vs "EPHEMERIS" 불일치

**수정**: trackingMode 기본값 "EPHEMERIS"로 수정

### P1: PassSchedule V006 미반영

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 HIGH |
| **상태** | ✅ 수정 완료 |

**증상**: 1 Pass = 7 Sessions (data_type별 분리)

**수정**:
- PassScheduleDataRepository.kt: (mstId, detailId) 그룹화 로직 추가
- saveOrUpdateSession() UPSERT 로직 추가

---

## 2026-01-22 오후: P2~P4 (mstId/detailId 구조)

### P2: mst_id = detail_id 동일

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 HIGH |
| **상태** | ✅ 수정 완료 |

**원인**: startMstId 미전달 → 기본값 0

**수정**: EphemerisService.kt에 mstIdCounter 추가, processFullTransformation()에 startMstId 전달

### P2-1: mst_id 의미 오류 (CRITICAL)

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 CRITICAL |
| **상태** | ✅ 수정 완료 |

**증상**: 동일 위성(AQUA)의 각 패스가 다른 mst_id (1,2,3,4,5,6)

**올바른 설계**:
- mst_id: 위성별 그룹 ID (동일 위성 = 동일 mst_id)
- detail_id: Pass 구분자 (0, 1, 2, ...)

**수정**:
- SatelliteTrackingProcessor.kt: `val mstId = startMstId` (index 제거)
- EphemerisService.kt: `mstIdCounter.getAndAdd(1)` (passCount 대신)
- PassScheduleService.kt: 동일 수정 (2곳)

### P2-2 ~ P2-5: LimitAngleCalculator 그룹화

| 이슈 | 심각도 | 수정 내용 |
|------|--------|----------|
| P2-2 | 🟡 HIGH | (MstId, DetailId) 쌍으로 그룹화 |
| P2-3 | 🟡 HIGH | calculateMaxAzRate()에 DetailId 추가 |
| P2-4 | 🟡 HIGH | validateConversion() 그룹화 수정 |
| P2-5 | 🟡 MEDIUM | "No" → "Index" 필드명 변경 |

### P3: TLE 컬럼 NULL

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 HIGH |
| **상태** | ✅ 수정 완료 |

**수정**: SatelliteTrackingProcessor.kt structureOriginalData()에 TleLine1, TleLine2 추가

### P4: 7가지 DataType 누락

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 CRITICAL |
| **상태** | ✅ 오인 (정상) |

DB 검증 결과 3가지 DataType 모두 정상 저장:
- original: 33,314개
- axis_transformed: 33,314개
- final_transformed: 33,314개

---

## 2026-01-22 저녁: 추가 버그픽스

| 이슈 | 심각도 | 수정 내용 |
|------|--------|----------|
| currentTrackingDetailId 누락 | 🔴 CRITICAL | EphemerisService에 변수 추가 |
| 서버 재시작 시 스케줄 0개 | 🔴 CRITICAL | EphemerisDataRepository @PostConstruct 추가 |
| FE formatDuration 에러 | 🟡 HIGH | 숫자/문자열 모두 처리 (5개 파일) |

---

## 2026-01-23: Select Schedule 선택 버그

### PassSchedule: uid 필드 추가

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 HIGH |
| **상태** | ✅ 수정 완료 |

**증상**: 1개 패스 선택 시 동일 위성 전체 선택

**원인**: row-key를 함수 형태로 설정 → Quasar 내부 selection 오작동

**수정**:
- `passScheduleStore.ts`: ScheduleItem에 uid 필드 추가
- `SelectScheduleContent.vue`: row-key 함수 → 문자열 "uid"

### Ephemeris: No 순차 생성

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 HIGH |
| **상태** | ✅ 수정 완료 |

**증상**: 모든 AQUA 패스의 No 값이 1로 동일

**원인 1**: BE에서 `put("No", mstId)` → MstId를 No로 사용
**원인 2**: FE에서 `No: (item.MstId ?? item.No)` → BE 응답을 MstId로 덮어씀

**수정**:
- `EphemerisService.kt` L3025: `mapNotNull` → `withIndex().mapNotNull`
- `EphemerisService.kt` L3133: `put("No", mstId)` → `put("No", index + 1)`
- `ephemerisTrackService.ts` L472: `(item.MstId ?? item.No)` → `item.No`

---

## 2026-01-23: P5 tracking_session 매핑 누락

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 HIGH |
| **상태** | ✅ 수정 완료 |

### 증상

tracking_session 테이블에서 계산된 파라미터들이 null:
- `start_azimuth`, `end_azimuth`, `start_elevation`, `end_elevation`
- `train_angle`, `max_elevation_time`
- `max_azimuth_accel`, `max_elevation_accel`
- `original_*`, `final_*`, `keyhole_*` 관련 모든 필드

### 원인

**EphemerisDataRepository.mapMstToSession()** 함수에서 MST 데이터의 필드들을 TrackingSessionEntity로 매핑할 때 대부분의 필드가 누락됨.

| Repository | 매핑 필드 수 |
|------------|-------------|
| PassScheduleDataRepository | 52개 (전체) |
| EphemerisDataRepository | 17개 (기본만) |

### 수정 내용

| 파일 | 변경 내용 |
|------|----------|
| `EphemerisDataRepository.kt` | mapMstToSession()에 35개 필드 매핑 추가 |

**추가 필드 (35개)**:
- 기본 각도 (5개): startAzimuth, endAzimuth, startElevation, endElevation, trainAngle
- 시간/가속도 (3개): maxElevationTime, maxAzimuthAccel, maxElevationAccel
- Original (5개): originalStartAzimuth, originalEndAzimuth, originalMaxElevation, originalMaxAzRate, originalMaxElRate
- FinalTransformed (7개): final*
- KeyholeAxis (2개): keyholeAxisMax*
- KeyholeFinal (7개): keyholeFinal*
- KeyholeOptimized (7개): keyholeOpt*

### P5-1: PassSchedule 키 이름 불일치

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 MEDIUM |
| **상태** | ✅ 해결 (양쪽 키 모두 지원) |

**이슈**: PassScheduleDataRepository에서 `StartAzimuthAngle` 읽기 시도하나, MST는 `StartAzimuth`로 생성

**해결**: EphemerisDataRepository에서 `StartAzimuth` || `StartAzimuthAngle` 양쪽 모두 읽도록 수정

---

## 2026-01-23: P5-2 DataType 필드 누락

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 HIGH |
| **상태** | ✅ 수정 완료 |

### 증상

tracking_session.data_type이 항상 "original"로 저장됨

### 원인

EphemerisService.kt의 mergedData 생성 시 DataType 필드가 명시적으로 설정되지 않음

### 수정 내용

| 파일 | 변경 내용 |
|------|----------|
| `EphemerisService.kt` L3293 | DataType 동적 설정 추가 |

```kotlin
// ✅ P5-2: DataType 필드 추가
put("DataType", if (isKeyhole) "keyhole_optimized_final_transformed" else "final_transformed")
```

---

## 2026-01-23: P5-3 TLE 필드 누락

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 HIGH |
| **상태** | ✅ 수정 완료 |

### 증상

tracking_session.tle_line_1, tle_line_2, tle_epoch, tle_cache_id가 null

### 원인

EphemerisService.kt의 mergedData 생성 시 TLE 필드들이 original MST에서 복사되지 않음

### 수정 내용

| 파일 | 변경 내용 |
|------|----------|
| `EphemerisService.kt` L3296-3300 | TLE 필드 추가 |

```kotlin
// ✅ P5-3: TLE 필드 추가
put("TleLine1", original?.get("TleLine1"))
put("TleLine2", original?.get("TleLine2"))
put("TleEpoch", original?.get("TleEpoch"))
put("TleCacheId", original?.get("TleCacheId"))
```

---

## 수정 파일 요약

| 파일 | 수정 내용 |
|------|----------|
| EphemerisService.kt | sessionId 조회, mstIdCounter, No 순차 생성 |
| EphemerisDataRepository.kt | trackingMode 수정, @PostConstruct, **mapMstToSession 확장 (P5)** |
| PassScheduleService.kt | mstIdCounter.getAndAdd(1) |
| PassScheduleDataRepository.kt | V006 그룹화, saveOrUpdateSession |
| SatelliteTrackingProcessor.kt | mstId=startMstId, TLE 추가, DetailId 추가 |
| LimitAngleCalculator.kt | (MstId, DetailId) 그룹화, No→Index |
| passScheduleStore.ts | uid 필드 추가 |
| SelectScheduleContent.vue | row-key="uid" |
| ephemerisTrackService.ts | No 덮어쓰기 제거 (MstId → No 그대로) |

---

## 참조 문서

- [PROGRESS.md](PROGRESS.md) - 전체 진행 상황
- [ADR-007](../../../decisions/ADR-007-tracking-session-key-design.md) - mst_id + detail_id 구조 결정