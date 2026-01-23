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

## 2026-01-23: P6 created_at 등록 건 그룹핑

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 MEDIUM |
| **상태** | ✅ 수정 완료 |

### 증상

TLE 등록 후 Select Schedule에서 "해당 TLE 정보만" 표시되는 것처럼 보임.
실제로는 `replaceAll()`이 이전 데이터를 삭제하는 **부작용**으로 동작하는 것.

**문제점**:
- 여러 위성을 한 번에 등록해도 그룹핑 기준 없음
- `created_at`이 각 INSERT마다 밀리초 차이 → 정확한 그룹핑 불가
- 이력 보존 시 "같은 등록 건"을 식별할 방법 없음

### 원인

| 항목 | 현재 상태 | 문제 |
|------|----------|------|
| **created_at 설정** | DB `DEFAULT NOW()` | 각 row마다 다른 시간 |
| **등록 건 식별** | 없음 | 그룹핑 불가 |
| **mstId** | 위성별 고유 ID | 등록 건 그룹이 아님 |

```
현재 동작:
  ISS        → created_at = 14:30:05.123
  Starlink   → created_at = 14:30:05.456  ← 밀리초 차이
  Hubble     → created_at = 14:30:05.789

→ "이 3개가 같은 등록 건"이라는 기준 없음
```

### 수정 방안

**선택한 방안**: 이력 보존 + created_at 기준 최신 조회

#### 대안 비교

| 방안 | 장점 | 단점 | 선택 |
|------|------|------|:----:|
| A. replaceAll 유지 (삭제) | 간단 | 이력 없음 | ❌ |
| **B. 이력 보존 + 날짜 필터** | 이력 조회 가능, 그룹핑 명확 | 조회 로직 추가 | ✅ |

### 변경 내용

#### 1. EphemerisDataRepository.kt - 저장 (이력 보존)

```kotlin
// replaceAll() → clear() 제거, 누적 저장으로 변경
fun replaceAll(mstData: List<Map<String, Any?>>, dtlData: List<Map<String, Any?>>) {
    val registrationTime = OffsetDateTime.now(ZoneOffset.UTC)  // 한 번만 생성

    // ❌ 삭제: mstStorage.clear(), dtlStorage.clear()
    // ✅ 누적: mstStorage.addAll(), dtlStorage.addAll()

    saveToDatabase(mstData, dtlData, opId, registrationTime)
}
```

```kotlin
// mapMstToSession() - registrationTime 추가
private fun mapMstToSession(
    mst: Map<String, Any?>,
    dtlCount: Int = 0,
    registrationTime: OffsetDateTime? = null
): TrackingSessionEntity {
    return TrackingSessionEntity(
        // ...
        createdAt = registrationTime  // 명시적 지정
    )
}
```

#### 2. 조회 로직 - 최신 등록 건만

```kotlin
// getAllMst() 수정 - 최신 created_at만 반환
fun getAllMst(): List<Map<String, Any?>> {
    val latestCreatedAt = mstStorage.maxOfOrNull {
        it["CreatedAt"] as? OffsetDateTime
    }
    return mstStorage.filter {
        it["CreatedAt"] == latestCreatedAt
    }
}
```

또는 DB 조회 시:
```sql
SELECT * FROM tracking_session
WHERE created_at = (SELECT MAX(created_at) FROM tracking_session WHERE tracking_mode = 'EPHEMERIS')
```

#### 3. PassScheduleDataRepository.kt

동일한 패턴으로 수정

### 테스트 계획

#### 수정 확인
- [x] 한 번 등록 시 모든 row의 created_at 동일 확인
- [x] 이전 등록 건 DB에 유지 확인
- [x] 조회 시 최신 등록 건만 반환 확인
- [ ] 이력 조회 (기간 확장) 가능 확인

#### 회귀 테스트
- [x] Ephemeris 모드 TLE 등록 정상
- [ ] PassSchedule 모드 등록 정상
- [x] Select Schedule 표시 정상 (최신만)
- [x] 빌드 성공

---

## 2026-01-23: P6-1 조회 로직 필터링 추가

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 CRITICAL |
| **상태** | ✅ 수정 완료 |

### 증상

"이론치 다운로드" 버튼 클릭 시 데이터가 표시되지 않음

### 원인

P6에서 `clear()` 제거 후 `getAllMst()`, `getAllDtl()` 등이 **모든 누적 데이터**를 반환.
Frontend에서 데이터가 중복/혼란되어 제대로 표시되지 않음.

### 수정 내용

| 파일 | 함수 | 변경 내용 |
|------|------|----------|
| `EphemerisDataRepository.kt` | `getAllMst()` | 가장 최근 CreatedAt 필터링 추가 |
| `EphemerisDataRepository.kt` | `getAllDtl()` | 가장 최근 CreatedAt 필터링 추가 |
| `EphemerisDataRepository.kt` | `getMstByDataType()` | 가장 최근 CreatedAt 필터링 추가 |
| `EphemerisDataRepository.kt` | `getDtlByDataType()` | 가장 최근 CreatedAt 필터링 추가 |
| `EphemerisDataRepository.kt` | `findMstById()` | 가장 최근 CreatedAt 필터링 추가 |
| `EphemerisDataRepository.kt` | `findDtlByMstIdAndDataType()` | 가장 최근 CreatedAt 필터링 추가 |
| `EphemerisDataRepository.kt` | `findAllDtlByMstId()` | 가장 최근 CreatedAt 필터링 추가 |

```kotlin
// getAllMst() - 가장 최근 등록 건만 반환
fun getAllMst(): List<Map<String, Any?>> {
    synchronized(mstStorage) {
        val latestCreatedAt = mstStorage
            .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
            .maxOrNull()

        if (latestCreatedAt == null) {
            mstStorage.toList()
        } else {
            mstStorage.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
        }
    }
}
```

### 재발 방지

| 대책 | 적용 |
|------|:----:|
| DB DEFAULT NOW() 의존 금지 | ✅ |
| 그룹 작업 시 명시적 timestamp 사용 패턴 | ✅ |
| 조회 시 created_at 필터 패턴 | ✅ |

---

## 2026-01-23: P6-2 서버 재시작 시 DTL 로드 누락

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 CRITICAL |
| **상태** | ✅ 수정 완료 |

### 증상

서버 재시작 후 스케줄 선택 시 DTL 데이터가 없음:
```
MST ID 2 의 원본 DTL 데이터를 찾을 수 없습니다 (DetailId=4)
```

### 원인

`initFromDatabase()`가 **MST만 로드하고 DTL(trajectory)을 로드하지 않음**

```kotlin
// 기존 코드 (문제)
@PostConstruct
fun initFromDatabase() {
    sessionRepository.findByTrackingMode("EPHEMERIS")
        .collectList()
        .doOnSuccess { sessions ->
            // MST만 로드 ❌
            sessions.forEach { session ->
                mstStorage.add(mapSessionToMst(session))
            }
            // DTL 로드 없음! ❌
        }
}
```

### 수정 내용

| 파일 | 변경 내용 |
|------|----------|
| `EphemerisDataRepository.kt` | `initFromDatabase()` - DTL 로드 추가 |
| `EphemerisDataRepository.kt` | `loadTrajectoryForSession()` - 세션별 trajectory 로드 함수 추가 |
| `EphemerisDataRepository.kt` | `mapTrajectoryToDtl()` - Entity → Map 변환 함수 추가 |

```kotlin
// 수정 후
@PostConstruct
fun initFromDatabase() {
    sessionRepository.findByTrackingMode("EPHEMERIS")
        .collectList()
        .doOnSuccess { sessions ->
            sessions.forEach { session ->
                // MST 로드
                mstStorage.add(mapSessionToMst(session))

                // ✅ P6-2 Fix: DTL도 로드
                if (session.id != null) {
                    loadTrajectoryForSession(session)
                }
            }
        }
}

private fun loadTrajectoryForSession(session: TrackingSessionEntity) {
    trajectoryRepository.findBySessionId(session.id)
        .collectList()
        .doOnSuccess { trajectories ->
            val dtlData = trajectories.map { traj ->
                mapTrajectoryToDtl(session, traj)
            }
            dtlStorage.addAll(dtlData)
        }
        .subscribe()
}
```

### 테스트 계획

- [ ] 서버 재시작 후 DTL 조회 정상 확인
- [ ] 스케줄 선택 → 이론치 다운로드 정상 확인
- [x] 빌드 성공

---

---

## 2026-01-23: P7 PassSchedule Backend P6 미적용

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 CRITICAL |
| **상태** | ✅ **수정 완료** |

### 증상

PassSchedule 모드에서 서버 재시작 시 스케줄 데이터 손실:
- DB에 저장된 데이터가 메모리로 로드되지 않음
- `initFromDatabase()` 미구현

### 원인

Ephemeris에 적용된 P6, P6-1, P6-2 수정이 PassSchedule에 미적용:

| 기능 | EphemerisDataRepository | PassScheduleDataRepository |
|------|:------------------------:|:----------------------------:|
| `@PostConstruct initFromDatabase()` | ✅ 구현됨 | ✅ **구현 완료** |
| `loadTrajectoryForSession()` | ✅ 구현됨 | ✅ **구현 완료** |
| `mapSessionToMstForLoad()` | ✅ 구현됨 | ✅ **구현 완료** |
| `mapTrajectoryToDtlForLoad()` | ✅ 구현됨 | ✅ **구현 완료** |
| CreatedAt 필터링 (getAllMst 등) | ✅ 7개 함수 | ✅ **9개 함수 적용** |
| OffsetDateTime→ZonedDateTime 변환 | ✅ 적용됨 | ✅ **적용 완료** |
| 이력 보존 (saveSatelliteData 누적) | ✅ 적용됨 | ✅ **적용 완료** |

### 수정 방안

EphemerisDataRepository.kt의 P6 수정사항을 PassScheduleDataRepository.kt에 동일하게 적용

#### P7-1: initFromDatabase() 추가

```kotlin
@PostConstruct
fun initFromDatabase() {
    sessionRepository.findByTrackingMode("PASS_SCHEDULE")
        .collectList()
        .doOnSuccess { sessions ->
            sessions.forEach { session ->
                // 위성별로 그룹화하여 mstStorage에 추가
                val satelliteId = session.satelliteId
                val mst = mapSessionToMst(session)

                val existing = mstStorage[satelliteId] ?: emptyList()
                mstStorage[satelliteId] = existing + mst

                // ✅ DTL도 로드
                if (session.id != null) {
                    loadTrajectoryForSession(session)
                }
            }
        }
        .subscribe()
}
```

#### P7-2: loadTrajectoryForSession() 추가

```kotlin
private fun loadTrajectoryForSession(session: TrackingSessionEntity) {
    trajectoryRepository.findBySessionId(session.id)
        .collectList()
        .doOnSuccess { trajectories ->
            val dtlData = trajectories.map { traj ->
                mapTrajectoryToDtl(session, traj)
            }
            val satelliteId = session.satelliteId
            val existing = dtlStorage[satelliteId] ?: emptyList()
            dtlStorage[satelliteId] = existing + dtlData
        }
        .subscribe()
}
```

#### P7-3: mapSessionToMst() 추가

```kotlin
private fun mapSessionToMst(session: TrackingSessionEntity): Map<String, Any?> {
    val startTimeZoned = session.startTime.atZoneSameInstant(ZoneOffset.UTC)
    val endTimeZoned = session.endTime.atZoneSameInstant(ZoneOffset.UTC)

    return mutableMapOf<String, Any?>(
        "MstId" to session.mstId,
        "DetailId" to session.detailId,
        "DataType" to session.dataType,
        "SatelliteID" to session.satelliteId,
        "SatelliteName" to session.satelliteName,
        "StartTime" to startTimeZoned,
        "EndTime" to endTimeZoned,
        // ... 나머지 필드 매핑
        "CreatedAt" to session.createdAt
    )
}
```

#### P7-4: mapTrajectoryToDtl() 추가

```kotlin
private fun mapTrajectoryToDtl(session: TrackingSessionEntity, traj: TrackingTrajectoryEntity): Map<String, Any?> {
    val zonedTime = traj.timestamp.atZoneSameInstant(ZoneOffset.UTC)

    return mutableMapOf<String, Any?>(
        "MstId" to session.mstId,
        "DetailId" to traj.detailId,
        "DataType" to traj.dataType,
        "Time" to zonedTime,
        // ... 나머지 필드 매핑
        "CreatedAt" to traj.createdAt
    )
}
```

#### P7-5: CreatedAt 필터링 추가

다음 함수들에 최신 CreatedAt 필터링 로직 추가:
- `getMstBySatelliteId()`
- `getDtlBySatelliteId()`
- `getAllMst()`
- `getAllDtl()`
- `getAllMstFlattened()`
- `getAllDtlFlattened()`
- `findMstById()`
- `findDtlByMstIdAndDataType()`
- `findDtlBySatelliteAndMstId()`

#### P7-6: 이력 보존

`saveSatelliteData()`에서:
- ❌ 삭제: `mstStorage[satelliteId] = ...` (덮어쓰기)
- ✅ 변경: 기존 데이터 유지하면서 추가

### 영향 범위

| 영역 | 영향 | 설명 |
|------|:----:|------|
| Backend | ✅ | PassScheduleDataRepository.kt 수정 |
| Frontend | ❌ | 변경 없음 |
| DB | ❌ | 스키마 변경 없음 |

### 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `PassScheduleDataRepository.kt` | initFromDatabase, loadTrajectoryForSession, mapSessionToMst, mapTrajectoryToDtl 추가 |
| `PassScheduleDataRepository.kt` | 조회 함수 CreatedAt 필터링 추가 (9개 함수) |
| `PassScheduleDataRepository.kt` | 저장 로직 이력 보존으로 변경 |

### 테스트 계획

#### 수정 확인
- [ ] 서버 재시작 후 PassSchedule MST 로드 확인
- [ ] 서버 재시작 후 PassSchedule DTL 로드 확인
- [ ] 조회 시 최신 CreatedAt만 반환 확인

#### 회귀 테스트
- [ ] PassSchedule 등록 정상
- [ ] Select Schedule 표시 정상
- [ ] 이론치 다운로드 정상
- [ ] 빌드 성공

---

## 참조 문서

- [PROGRESS.md](PROGRESS.md) - 전체 진행 상황
- [ADR-007](../../../decisions/ADR-007-tracking-session-key-design.md) - mst_id + detail_id 구조 결정