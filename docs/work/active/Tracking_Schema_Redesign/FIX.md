# Tracking Schema V006 버그 수정 계획

## 수정 완료 (2026-01-22)

| 우선순위 | 버그 | 심각도 | 상태 |
|:--------:|------|:------:|:----:|
| **P0** | sessionId 연동 버그 | CRITICAL | ✅ 완료 |
| **P0-1** | trackingMode 불일치 | CRITICAL | ✅ 완료 |
| **P1** | PassSchedule V006 미반영 | HIGH | ✅ 완료 |
| **P1-1** | PassSchedule sessionId 조회 | HIGH | ✅ 완료 |

## 신규 발견 이슈 (2026-01-22 오후) - 수정 완료

| 우선순위 | 버그 | 심각도 | 상태 |
|:--------:|------|:------:|:----:|
| **P2** | mst_id = detail_id 동일 | 🟡 HIGH | ✅ 수정 완료 |
| **P2-1** | mst_id 의미 오류 (동일 위성에 다른 mstId) | 🔴 CRITICAL | ✅ 수정 완료 |
| **P2-2** | LimitAngleCalculator 그룹화 오류 | 🟡 HIGH | ✅ 수정 완료 |
| **P2-3** | calculateMaxAzRateForTrainAngle() DetailId 누락 | 🟡 HIGH | ✅ 수정 완료 |
| **P2-4** | validateConversion() 그룹화 오류 | 🟡 HIGH | ✅ 수정 완료 |
| **P2-5** | No → Index 필드명 불일치 | 🟡 MEDIUM | ✅ 수정 완료 |
| **P3** | TLE 컬럼 전부 NULL | 🟡 HIGH | ✅ 수정 완료 |
| **P4** | 7가지 DataType 중 original만 저장 | 🔴 CRITICAL | ✅ 오인 (정상 동작) |

### P4 분석 결과

DB 검증 결과, 3가지 DataType 모두 정상 저장됨:
- original: 33,314개
- axis_transformed: 33,314개
- final_transformed: 33,314개

KEYHOLE 위성이 없어서 keyhole_* 4가지는 0개 (정상 동작).

---

## P2: mst_id = detail_id 동일

### 원인 분석

```
EphemerisService.generateEphemerisDesignationTrackSync()
┌─────────────────────────────────────────────────────────────────┐
│ val processedData = satelliteTrackingProcessor                  │
│     .processFullTransformation(                                 │
│         schedule,                                               │
│         satelliteName                                           │
│         // ❌ startMstId 미전달 → 기본값 0 사용                 │
│     )                                                           │
└─────────────────────────────────────────────────────────────────┘

SatelliteTrackingProcessor.structureOriginalData()
┌─────────────────────────────────────────────────────────────────┐
│ schedule.trackingPasses.forEachIndexed { index, pass ->         │
│     val mstId = startMstId + index   // 0 + 0 = 0, 0 + 1 = 1   │
│     val detailId = index             // 0, 1, 2, ...            │
│     // → mstId = detailId ❌                                    │
│ }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

**Root Cause**: `EphemerisService.kt:457`에서 `startMstId`를 전달하지 않음

### 영향

- tracking_session.mst_id = tracking_session.detail_id
- 데이터 무결성 영향은 적음 (기능 정상 작동)
- 그러나 V006 설계 의도(전역 고유 ID)와 불일치

### 수정 방안 ✅ 적용 완료 (2026-01-22)

**방안 A: EphemerisService에 mstIdCounter 추가** (PassScheduleService와 동일)

```kotlin
// EphemerisService.kt - 수정 완료 ✅
import java.util.concurrent.atomic.AtomicLong

// ✅ P2 Fix: 전역 고유 MstId 생성용 카운터
private val mstIdCounter = AtomicLong(0)

fun generateEphemerisDesignationTrackSync(...) {
    // ✅ P2 Fix: 전역 고유 MstId 생성
    val passCount = schedule.trackingPasses.size
    val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1
    logger.debug("📊 startMstId: $startMstId (passCount: $passCount)")

    val processedData = satelliteTrackingProcessor.processFullTransformation(
        schedule,
        satelliteName,
        startMstId  // ✅ P2 Fix: startMstId 전달
    )
}
```

---

## P2-1: mst_id 의미 오류 (CRITICAL) ✅ 수정 완료 (2026-01-22)

### 증상

DB 검증 결과, 동일 위성(AQUA)의 패스들이 각각 다른 mst_id를 가짐:

```
mst_id=1, detail_id=0, AQUA (패스 1)
mst_id=2, detail_id=1, AQUA (패스 2)
mst_id=3, detail_id=2, AQUA (패스 3)
...
```

### 올바른 설계 (V006)

```
mst_id: 위성별 그룹 ID (동일 위성 = 동일 mst_id)
detail_id: Pass 구분자 (0, 1, 2, ...)
```

**예상 결과:**
```
mst_id=1, detail_id=0, AQUA (패스 1)
mst_id=1, detail_id=1, AQUA (패스 2)
mst_id=1, detail_id=2, AQUA (패스 3)
mst_id=2, detail_id=0, LANDSAT (패스 1)  ← 다른 위성은 다른 mst_id
```

### 원인

P2 수정 시 mst_id 의미를 "각 패스의 전역 고유 ID"로 잘못 이해함.

**잘못된 코드:**
```kotlin
// SatelliteTrackingProcessor.kt
val mstId = startMstId + index  // 각 패스마다 mstId 증가 ❌

// EphemerisService.kt / PassScheduleService.kt
val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1  // passCount만큼 증가 ❌
```

### 수정 내용

#### 1. SatelliteTrackingProcessor.kt - mstId 생성 로직 수정

```diff
// structureOriginalData()
schedule.trackingPasses.forEachIndexed { index, pass ->
-   val mstId = startMstId + index   // ❌ 각 패스마다 mstId 증가
+   val mstId = startMstId           // ✅ 동일 위성 = 동일 mstId
    val detailId = index             // ✅ 패스 구분은 detailId로
```

#### 2. EphemerisService.kt - mstIdCounter 로직 수정

```diff
// generateEphemerisDesignationTrackSync()
- val passCount = schedule.trackingPasses.size
- val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1
+ val startMstId = mstIdCounter.getAndAdd(1) + 1  // ✅ 위성당 1씩만 증가
```

#### 3. PassScheduleService.kt - 동일 수정 (2곳)

```diff
// processScheduleInternal() - 라인 1446
- val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1
+ val startMstId = mstIdCounter.getAndAdd(1) + 1

// processMultiSatelliteSchedule() - 라인 1605
- val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1
+ val startMstId = mstIdCounter.getAndAdd(1) + 1
```

### 검증 방법

```sql
-- 동일 위성의 모든 패스가 같은 mst_id를 갖는지 확인
SELECT satellite_id, mst_id, COUNT(*) as pass_count
FROM tracking_session
WHERE tracking_mode IN ('EPHEMERIS', 'PASS_SCHEDULE')
GROUP BY satellite_id, mst_id
ORDER BY satellite_id, mst_id;

-- 예상 결과: 동일 satellite_id = 동일 mst_id
```

---

## P2-2: LimitAngleCalculator 그룹화 오류 ✅ 수정 완료 (2026-01-22)

### 증상

P2-1 수정 후 LimitAngleCalculator에서 "큰 회전 감지" 경고가 대량 발생:

```
WARN - ⚠️ 큰 회전 감지: 원본 311.71° → 133.15°, 변환 -48.29° → -226.85° (회전량: 178.56°)
WARN - ⚠️ 큰 회전 감지: 원본 133.15° → 244.41°, 변환 -226.85° → -115.59° (회전량: 111.26°)
WARN - ⚠️ 큰 회전 감지: 원본 244.41° → 27.00°, 변환 -115.59° → 27.00° (회전량: 142.59°)
```

### 원인

P2-1 수정 후 동일 위성의 모든 패스가 **같은 MstId**를 가지게 됨. LimitAngleCalculator에서 MstId만으로 그룹화하면서 서로 다른 패스의 데이터가 병합됨.

| 구분 | 수정 전 | 수정 후 |
|------|---------|---------|
| MstId 할당 | 패스마다 고유 (1, 2, 3...) | 위성마다 동일 (1, 1, 1...) |
| LimitAngleCalculator | 각 패스가 개별 처리 | 모든 패스가 **하나로 병합** |
| 결과 | 연속 경로 (311→27) | 비연속 점프 (311→133→244→27) |

### 수정 내용

```diff
// LimitAngleCalculator.kt - convertDetailData()

- // MstId별로 그룹화하여 처리
- val groupedByMstId = ephemerisTrackDtl.groupBy { (it["MstId"] as? Number)?.toLong() ?: 0L }
-
- groupedByMstId.forEach { (mstId, dtlList) ->
-     logger.debug("MstId $mstId 처리 중 - ${dtlList.size}개 데이터 포인트")

+ // V006: (MstId, DetailId) 쌍으로 그룹화하여 패스별로 개별 처리
+ val groupedByMstIdAndDetailId = ephemerisTrackDtl.groupBy { dtl ->
+     val mstId = (dtl["MstId"] as? Number)?.toLong() ?: 0L
+     val detailId = (dtl["DetailId"] as? Number)?.toInt() ?: 0
+     Pair(mstId, detailId)
+ }
+
+ groupedByMstIdAndDetailId.forEach { (key, dtlList) ->
+     val (mstId, detailId) = key
+     logger.debug("MstId=$mstId, DetailId=$detailId 처리 중 - ${dtlList.size}개 데이터 포인트")
```

---

## P2-3: calculateMaxAzRateForTrainAngle() DetailId 누락 ✅ 수정 완료 (2026-01-22)

### 원인 분석

```kotlin
// SatelliteTrackingProcessor.kt - calculateMaxAzRateForTrainAngle() L1063-1071
transformedDtl.map { dtl ->
    mapOf(
        "MstId" to dtl["MstId"],
        "No" to dtl["No"],      // ❌ "Index"로 변경 필요
        "Time" to dtl["Time"],
        "Azimuth" to dtl["Azimuth"],
        "Elevation" to dtl["Elevation"]
        // ❌ "DetailId" 누락!
    )
}
```

**Root Cause**: LimitAngleCalculator에 데이터 전달 시 "DetailId" 누락, "No" 대신 "Index" 사용해야 함

### 수정 내용

```diff
// SatelliteTrackingProcessor.kt - calculateMaxAzRateForTrainAngle()
- "MstId" to dtl["MstId"],      // ✅ 그룹화용 (convertDetailData Line 66)
- "No" to dtl["No"],            // ✅ 정렬용 (convertAzimuthPath Line 87)
- "Time" to dtl["Time"],        // ✅ 시간 정보
- "Azimuth" to dtl["Azimuth"],  // ✅ 변환 대상 (convertAzimuthPath Line 88)
- "Elevation" to dtl["Elevation"] // ✅ 고도 정보

+ "MstId" to dtl["MstId"],          // ✅ 그룹화용 (V006: MstId + DetailId)
+ "DetailId" to dtl["DetailId"],   // ✅ 그룹화용 (V006: 패스 구분자)
+ "Index" to dtl["Index"],          // ✅ 정렬용 (convertAzimuthPath Line 98)
+ "Time" to dtl["Time"],            // ✅ 시간 정보
+ "Azimuth" to dtl["Azimuth"],      // ✅ 변환 대상
+ "Elevation" to dtl["Elevation"]  // ✅ 고도 정보
```

---

## P2-4: validateConversion() 그룹화 오류 ✅ 수정 완료 (2026-01-22)

### 원인 분석

```kotlin
// LimitAngleCalculator.kt - validateConversion() L510
convertedDtl.groupBy { it["MstId"] as UInt }  // ❌ MstId만 그룹화
val sortedList = dtlList.sortedBy { it["No"] as UInt }  // ❌ "No" 대신 "Index"
```

**Root Cause**: validateConversion()도 convertDetailData()와 동일하게 (MstId, DetailId) 쌍으로 그룹화해야 함

### 수정 내용

```diff
// LimitAngleCalculator.kt - validateConversion()
- convertedDtl.groupBy { it["MstId"] as UInt }.forEach { (mstId, dtlList) ->
-     val sortedList = dtlList.sortedBy { it["No"] as UInt }

+ // ✅ V006: (MstId, DetailId) 쌍으로 그룹화하여 패스별 개별 검증
+ convertedDtl.groupBy { dtl ->
+     val mstId = (dtl["MstId"] as? Number)?.toLong() ?: 0L
+     val detailId = (dtl["DetailId"] as? Number)?.toInt() ?: 0
+     Pair(mstId, detailId)
+ }.forEach { (key, dtlList) ->
+     val (mstId, detailId) = key
+     val sortedList = dtlList.sortedBy { (it["Index"] as? Number)?.toInt() ?: 0 }
```

---

## P2-5: No → Index 필드명 불일치 ✅ 수정 완료 (2026-01-22)

### 원인 분석

LimitAngleCalculator에서 여러 곳에서 구 필드명 "No"를 사용:
- L467: convertMasterData()
- L713: debugConversionDetails()

**Root Cause**: V006 리팩토링 시 "No" → "Index" 변경이 누락된 위치들

### 수정 내용

```diff
// LimitAngleCalculator.kt - convertMasterData() L467
- val relatedDtlData = convertedDtlData.filter { it["MstId"] == mstId }
-     .sortedBy { it["No"] as UInt }

+ // ✅ "No" → "Index" 변경 (V006 리팩토링)
+ val relatedDtlData = convertedDtlData.filter { it["MstId"] == mstId }
+     .sortedBy { (it["Index"] as? Number)?.toInt() ?: 0 }

// LimitAngleCalculator.kt - debugConversionDetails() L713
- val passDetails = convertedDtl.filter { it["MstId"] == mstId }
-     .sortedBy { it["No"] as UInt }

+ // ✅ "No" → "Index" 변경 (V006 리팩토링)
+ val passDetails = convertedDtl.filter { it["MstId"] == mstId }
+     .sortedBy { (it["Index"] as? Number)?.toInt() ?: 0 }
```

---

## P3: TLE 컬럼 전부 NULL

### 원인 분석

```kotlin
// SatelliteTrackingProcessor.kt:493-517 - originalMst 생성
originalMst.add(
    mapOf(
        "MstId" to mstId,
        "DetailId" to detailId,
        "SatelliteID" to satelliteId,
        // ... 기타 필드 ...
        "DataType" to "original"
        // ❌ TLE 정보 없음: TleCacheId, TleLine1, TleLine2, TleEpoch
    )
)
```

**Root Cause**: `structureOriginalData()`에서 TLE 정보를 MST에 포함하지 않음

### 수정 방안 ✅ 적용 완료 (2026-01-22)

```kotlin
// SatelliteTrackingProcessor.kt - structureOriginalData 수정 ✅
originalMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "Creator" to "System",
        "TleLine1" to schedule.satelliteTle1,  // ✅ P3 Fix: TLE Line 1
        "TleLine2" to schedule.satelliteTle2,  // ✅ P3 Fix: TLE Line 2
        "DataType" to "original"
    )
)
```

**Note**: TleEpoch는 TLE 라인에서 파싱 가능하므로 별도 추가하지 않음.

---

## P4: 7가지 DataType 중 original만 저장

### 원인 분석 (진행 중)

코드 분석 결과:
1. `EphemerisService.kt:470-503`: 7가지 DataType 모두 allDtlData에 추가 ✅
2. `EphemerisDataRepository.kt:401-405`: DataType 무관하게 MstId/DetailId로 필터링 ✅

코드상으로는 문제가 없어 보이나, 실제 DB에는 original만 저장됨.

### 가설

1. **processedData의 다른 DataType이 비어있음**
   - SatelliteTrackingProcessor.processFullTransformation() 반환값 확인 필요
   - 로그: `3축 변환 준비: X Mst, Y Dtl` 확인 필요

2. **변환 과정에서 데이터 누락**
   - applyAxisTransformation() 결과 확인
   - applyAngleLimitTransformation() 결과 확인

### 검증 방법

BE 로그에서 다음 확인:
```
📊 Original 준비: X Mst, Y Dtl
📊 3축 변환 준비: X Mst, Y Dtl
📊 최종 변환 준비: X Mst, Y Dtl
📊 Keyhole Axis 준비: X Mst, Y Dtl
📊 Keyhole Final 준비: X Mst, Y Dtl
📊 Keyhole Optimized Axis 준비: X Mst, Y Dtl
📊 Keyhole Optimized Final 준비: X Mst, Y Dtl
```

모든 Y 값이 > 0 이어야 함.

## 발견된 버그 (원본)

| 우선순위 | 버그 | 심각도 | 영향 |
|:--------:|------|:------:|------|
| **P0** | sessionId 연동 버그 | CRITICAL | tracking_result.session_id = 0 저장 |
| **P0-1** | trackingMode 불일치 | CRITICAL | sessionId 조회 항상 실패 |
| **P1** | PassSchedule V006 미반영 | HIGH | 1 Pass = 7 Sessions |
| **P1-1** | PassSchedule sessionId 조회 없음 | HIGH | PassSchedule 모드 누락 |

---

## P0: sessionId 연동 버그

### 원인 분석

```
스케줄 생성 시점:
┌─────────────────────────────────────────────────────────────────┐
│ EphemerisDataRepository.saveToDatabase()                        │
│   └─ saveOrUpdateSession()                                      │
│       └─ sessionRepository.save(session) → sessionId 생성 ✅    │
│           └─ saveTrajectories(sessionId, dtlData) ✅            │
└─────────────────────────────────────────────────────────────────┘

실시간 추적 시점:
┌─────────────────────────────────────────────────────────────────┐
│ EphemerisService.saveRealtimeTrackingData()                     │
│   └─ createRealtimeTrackingData(mstId, detailId, ...)           │
│       └─ return mapOf("mstId" to mstId, ...) ❌ sessionId 없음  │
│   └─ batchStorageManager.addTrackingResult(realtimeData)        │
│       └─ mapToTrackingResult(data)                              │
│           └─ data["sessionId"] → null → 0L ❌                   │
└─────────────────────────────────────────────────────────────────┘
```

**Root Cause**: `createRealtimeTrackingData()`에서 `sessionId`를 Map에 포함하지 않음

### 데이터 흐름 (현재)

```kotlin
// EphemerisService.kt:1984-1986
return mapOf(
    "mstId" to mstId,      // ✅ 있음
    "detailId" to detailId, // ✅ 있음
    // ❌ "sessionId" 누락!
)

// BatchStorageManager.kt:236
val sessionId = (data["sessionId"] as? Number)?.toLong() ?: 0L  // → 항상 0L
```

### 수정 방안

**방안 A: createRealtimeTrackingData에서 DB 조회** (선택)
- 장점: 명확한 데이터 연결, 기존 구조 유지
- 단점: DB 조회 추가 (약간의 성능 오버헤드)

**방안 B: EphemerisService에서 세션 ID 캐싱**
- 장점: DB 조회 없음
- 단점: 캐시 동기화 문제 발생 가능

### 수정 내용

#### 1. EphemerisService.kt - sessionId 조회 메서드 추가

```kotlin
// 파일: backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt
// 위치: createRealtimeTrackingData() 함수 근처

/**
 * mstId, detailId로 tracking_session의 id를 조회합니다.
 */
private fun getSessionIdByMstAndDetail(mstId: Long, detailId: Int): Long? {
    return sessionRepository?.findByMstIdAndDetailIdAndTrackingMode(
        mstId, detailId, "ephemeris_designation"
    )?.blockFirst()?.id
}
```

#### 2. createRealtimeTrackingData() 수정

```diff
// 파일: backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt
// 위치: 라인 1702 (createRealtimeTrackingData 함수)

private fun createRealtimeTrackingData(
    mstId: Long,
    detailId: Int = 0,
    currentTime: ZonedDateTime,
    startTime: ZonedDateTime
): Map<String, Any?> {
+   // ✅ P0 Fix: sessionId 조회
+   val sessionId = getSessionIdByMstAndDetail(mstId, detailId)

    // ... 기존 코드 ...

    return mapOf(
        // ... 기존 필드들 ...
        "mstId" to mstId,
        "detailId" to detailId,
+       "sessionId" to sessionId,  // ✅ sessionId 추가
        // ... 나머지 필드들 ...
    )
}
```

#### 3. EphemerisService에 sessionRepository 주입

```diff
// 파일: backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt
// 위치: 클래스 생성자

@Service
class EphemerisService(
    private val dataStoreService: DataStoreService,
    private val settingsService: SettingsService,
    private val batchStorageManager: BatchStorageManager,
+   private val sessionRepository: TrackingSessionRepository?,  // ✅ 추가
    // ... 기존 의존성 ...
) {
```

### 검증 방법

1. **BE 빌드 확인**
   ```bash
   cd backend && ./gradlew clean build -x test
   ```

2. **DB 데이터 확인**
   ```sql
   -- 수정 전: session_id = 0
   SELECT session_id, COUNT(*)
   FROM tracking_result
   GROUP BY session_id;

   -- 수정 후: session_id > 0
   SELECT tr.session_id, ts.mst_id, ts.detail_id
   FROM tracking_result tr
   JOIN tracking_session ts ON tr.session_id = ts.id
   LIMIT 10;
   ```

---

## P1: PassSchedule V006 정책 미반영

### 원인 분석

```kotlin
// EphemerisDataRepository.kt - V006 적용됨 ✅
val groupedMst = mstData.groupBy { mst ->
    val mstId = (mst["MstId"] as? Number)?.toLong() ?: 0L
    val detailId = (mst["DetailId"] as? Number)?.toInt() ?: 0
    Pair(mstId, detailId)  // ✅ 그룹화
}

// PassScheduleDataRepository.kt - V006 미적용 ❌
mstData.forEach { mst ->
    val session = mapMstToSession(satelliteId, mst, sessionDtlData.size)
    sessionRepository.save(session)  // ❌ data_type별로 각각 저장
}
```

**Root Cause**: PassScheduleDataRepository의 saveToDatabase()가 EphemerisDataRepository와 다르게 구현됨

### 수정 내용

#### PassScheduleDataRepository.kt 수정

```diff
// 파일: backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/passSchedule/PassScheduleDataRepository.kt
// 위치: saveToDatabase() 함수

private fun saveToDatabase(
    satelliteId: String,
    mstData: List<Map<String, Any?>>,
    dtlData: List<Map<String, Any?>>,
    opId: Long
) {
-   mstData.forEach { mst ->
-       try {
-           val session = mapMstToSession(satelliteId, mst, sessionDtlData.size)
-           sessionRepository?.save(session)
-               ?.doOnSuccess { saved ->
-                   if (saved.id != null && sessionDtlData.isNotEmpty()) {
-                       saveTrajectories(saved.id, sessionDtlData, opId)
-                   }
-               }
-               ?.subscribe()
-       } catch (e: RuntimeException) {
-           logger.error("❌ [DB #$opId] MST 저장 실패: ${e.message}")
-       }
-   }

+   // V006: (mstId, detailId) 기준으로 그룹화하여 1 Pass = 1 Session 보장
+   val groupedMst = mstData.groupBy { mst ->
+       val mstId = (mst["MstId"] as? Number)?.toLong() ?: 0L
+       val detailId = (mst["DetailId"] as? Number)?.toInt() ?: 0
+       Pair(mstId, detailId)
+   }
+
+   logger.info("📝 [DB #$opId] MST ${mstData.size}개 → ${groupedMst.size}개 세션으로 그룹화")
+
+   groupedMst.forEach { (key, mstGroup) ->
+       val (mstId, detailId) = key
+       try {
+           // 대표 MST 선택: 'original' 우선
+           val representativeMst = mstGroup.find { it["DataType"] == "original" }
+               ?: mstGroup.firstOrNull()
+               ?: return@forEach
+
+           // 모든 data_type의 DTL 데이터 합산
+           val allDtlForSession = dtlData.filter { dtl ->
+               val dtlMstId = (dtl["MstId"] as? Number)?.toLong()
+               val dtlDetailId = (dtl["DetailId"] as? Number)?.toInt() ?: 0
+               dtlMstId == mstId && dtlDetailId == detailId
+           }
+
+           val session = mapMstToSession(satelliteId, representativeMst, allDtlForSession.size)
+           saveOrUpdateSession(session, allDtlForSession, opId)
+       } catch (e: RuntimeException) {
+           logger.error("❌ [DB #$opId] MST($mstId, $detailId) 저장 실패: ${e.message}")
+       }
+   }
}
```

#### saveOrUpdateSession 메서드 추가 (EphemerisDataRepository와 동일)

```kotlin
/**
 * V006: 세션 UPSERT (존재하면 스킵, 없으면 INSERT)
 */
private fun saveOrUpdateSession(
    session: TrackingSessionEntity,
    dtlData: List<Map<String, Any?>>,
    opId: Long
) {
    sessionRepository?.findByMstIdAndDetailIdAndTrackingMode(
        session.mstId,
        session.detailId,
        session.trackingMode
    )?.hasElement()
        ?.flatMap { exists ->
            if (exists) {
                logger.debug("📝 [DB #$opId] Session 이미 존재: mstId=${session.mstId}, detailId=${session.detailId} (스킵)")
                reactor.core.publisher.Mono.empty()
            } else {
                sessionRepository.save(session)
            }
        }
        ?.doOnSuccess { saved: TrackingSessionEntity? ->
            if (saved != null && saved.id != null && dtlData.isNotEmpty()) {
                saveTrajectories(saved.id, dtlData, opId)
            }
        }
        ?.doOnError { e: Throwable ->
            logger.error("❌ [DB #$opId] Session 저장 실패: ${e.message}")
        }
        ?.subscribe()
}
```

### 검증 방법

```sql
-- 수정 전: 같은 mst_id에 여러 세션
SELECT mst_id, detail_id, COUNT(*) as session_count
FROM tracking_session
WHERE tracking_mode = 'pass_schedule'
GROUP BY mst_id, detail_id
HAVING COUNT(*) > 1;

-- 수정 후: 0 rows (1 Pass = 1 Session)
```

---

## 테스트 계획

### 수정 확인
- [ ] BE 빌드 성공
- [ ] Ephemeris 모드에서 tracking_result.session_id > 0
- [ ] PassSchedule 모드에서 1 Pass = 1 Session

### 회귀 테스트
- [ ] 기존 Ephemeris 추적 기능 정상
- [ ] 기존 PassSchedule 추적 기능 정상
- [ ] WebSocket 데이터 전송 정상

### 엣지 케이스
- [ ] 세션이 없는 상태에서 추적 시도 시 에러 처리
- [ ] 동시 다중 위성 추적 시 세션 ID 혼동 없음

---

## 수정 우선순위

| 순서 | 작업 | 파일 | 예상 난이도 |
|:----:|------|------|:-----------:|
| 1 | sessionId 조회 메서드 추가 | EphemerisService.kt | 낮음 |
| 2 | createRealtimeTrackingData에 sessionId 추가 | EphemerisService.kt | 낮음 |
| 3 | PassSchedule 그룹화 로직 추가 | PassScheduleDataRepository.kt | 중간 |
| 4 | saveOrUpdateSession 추가 | PassScheduleDataRepository.kt | 낮음 |

---

## 관련 파일

| 파일 | 수정 내용 |
|------|----------|
| EphemerisService.kt | sessionId 조회 + createRealtimeTrackingData 수정 |
| PassScheduleDataRepository.kt | V006 그룹화 로직 적용 |

## 참조 문서

- [ANALYSIS.md → DEEP_REVIEW_V007.md §11](DEEP_REVIEW_V007.md#11-발견된-이슈-critical)
- [PROGRESS.md](PROGRESS.md)