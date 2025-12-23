# PassSchedule 데이터 구조 리팩토링 계획

---
**작성일**: 2025-11-28
**작성자**: GTL Systems
**상태**: 진행 중
**위치**: `docs/features/active/PassSchedule_Data_Structure_Refactoring/PassSchedule_Data_Structure_Refactoring_plan.md`
**관련 기능**: [PassSchedule_Workflow](../PassSchedule_Workflow/PassSchedule_Workflow.md)
---

## 목표

PassSchedule의 MST/DTL 데이터 구조를 전역 고유 ID 기반으로 재설계하여 데이터 식별 및 조회의 일관성과 확장성을 확보합니다.

## 배경 및 문제점

### 현재 구조의 문제점

1. **위성별 인덱스 중복 문제**
   - 기존 `No` 필드가 위성별로 1부터 시작하여 전역적으로 고유하지 않음
   - 예: AQUA의 패스1 (No=1), AURA의 패스1 (No=1) → 중복 발생
   - 프론트엔드에서 여러 위성의 스케줄을 선택할 때 구분 불가

2. **데이터 식별자 혼용**
   - `No`, `MstId`, `index` 등 여러 필드가 혼용되어 사용
   - 백엔드와 프론트엔드 간 필드명 불일치
   - C# 레거시 코드와 Kotlin 코드 간 구조 차이

3. **DetailId 필드 부재**
   - 현재 DetailId가 명시적으로 관리되지 않음
   - 향후 여러 Detail 타입 확장 시 구조 변경 필요

4. **SatelliteID/SatelliteName 구분 불명확**
   - SatelliteID가 카탈로그 번호인지 위성 이름인지 혼동
   - 실제로는 SatelliteID = 카탈로그 번호 (예: "27424")
   - SatelliteName = 위성 이름 (예: "AQUA")

### C# 레거시 코드 분석

기존 C# 코드 구조:
```csharp
public uint master_index = 0;        // 위성별 인덱스
public uint all_master_cnt = 0;      // 전역 마스터 카운터
uint detail_index = 0;                // 위성 내 패스 인덱스
uint detail_count = 0;                // 패스 내 100ms 포인트 순번

// MST 생성
index = all_master_cnt++;            // 전역 고유 ID
master_index = master_index;          // 위성별 인덱스
detail_index = detail_index;         // 패스 인덱스

// DTL 생성
master_index = master_index;
detail_index = detail_index;
detail_count = ++detail_count;       // 100ms 포인트 순번
```

**핵심 발견**: C# 코드에서 `all_master_cnt`가 전역 넘버링 역할을 수행

---

## 새로운 데이터 구조 설계

### 필드 정의

| 필드명 | 타입 | 의미 | 범위 | 역할 |
|--------|------|------|------|------|
| `MstId` | Long | 전역 고유 MST ID | `1, 2, 3, 4, 5...` | Primary Key (전역 넘버링) |
| `DetailId` | Int | 위성 내 패스 인덱스 | `0, 1, 2, 3...` | Detail 구분자 (위성 내 패스 인덱스, 향후 확장 가능) |
| `Index` | Int | 100ms 포인트 순번 | `0, 1, 2, 3...` | DetailId 내 포인트 순번 |
| `SatelliteID` | String | 카탈로그 번호 | 예: `"27424"` | 위성 고유 식별자 (NORAD) |
| `SatelliteName` | String | 위성 이름 | 예: `"AQUA"` | 위성 표시명 |

### MST (Master) 구조

```kotlin
data class PassScheduleMaster {
    // ✅ Primary Key
    val mstId: Long               // 전역 고유 ID (1, 2, 3, 4, 5...)
    
    // ✅ Detail 구분
    val detailId: Int             // 위성 내 패스 인덱스 (0, 1, 2...)
    
    // ✅ 위성 정보
    val satelliteID: String       // 카탈로그 번호 ("27424")
    val satelliteName: String    // 위성 이름 ("AQUA")
    
    // ✅ 시간 정보
    val startTime: ZonedDateTime
    val endTime: ZonedDateTime
    val duration: Duration
    val maxElevation: Double
    val maxElevationTime: ZonedDateTime
    
    // ✅ 각도 정보
    val startAzimuth: Double
    val startElevation: Double
    val endAzimuth: Double
    val endElevation: Double
    
    // ✅ 메타데이터
    val maxAzRate: Double
    val maxElRate: Double
    val maxAzAccel: Double
    val maxElAccel: Double
    
    // ✅ Keyhole 정보
    val isKeyhole: Boolean
    val recommendedTrainAngle: Double
    
    // ✅ 메타 정보
    val creationDate: ZonedDateTime
    val creator: String
    val dataType: String         // "original", "final_transformed" 등
}
```

### DTL (Detail) 구조

```kotlin
data class PassScheduleDetail {
    // ✅ Foreign Keys
    val mstId: Long               // FK → MST.mstId (전역 고유 ID)
    val detailId: Int             // FK → MST.detailId
    
    // ✅ Primary Key (복합키)
    // (mstId, detailId, index) 조합으로 고유성 보장
    
    // ✅ 100ms 포인트 순번
    val index: Int                // 0, 1, 2, 3, 4... (DetailId 내)
    
    // ✅ 추적 포인트 데이터
    val time: ZonedDateTime       // 100ms 간격
    val azimuth: Double
    val elevation: Double
    val range: Double
    val altitude: Double
    
    // ✅ 메타 정보
    val dataType: String         // "original", "final_transformed" 등
}
```

### 데이터 예시

```json
{
  "masters": [
    {
      "mstId": 1,
      "detailId": 0,
      "satelliteID": "27424",
      "satelliteName": "AQUA",
      "startTime": "2025-11-28T10:00:00Z",
      "endTime": "2025-11-28T10:15:00Z",
      "maxElevation": 45.5,
      ...
    },
    {
      "mstId": 2,
      "detailId": 1,
      "satelliteID": "27424",
      "satelliteName": "AQUA",
      ...
    },
    {
      "mstId": 3,
      "detailId": 0,
      "satelliteID": "27421",
      "satelliteName": "AURA",
      ...
    }
  ],
  "details": [
    {
      "mstId": 1,
      "detailId": 0,
      "index": 0,
      "time": "2025-11-28T10:00:00.000Z",
      "azimuth": 180.5,
      "elevation": 10.2,
      ...
    },
    {
      "mstId": 1,
      "detailId": 0,
      "index": 1,
      "time": "2025-11-28T10:00:00.100Z",
      ...
    }
  ]
}
```

---

## 관계도

```
┌─────────────────────────────────────────────────────────┐
│ PassScheduleMaster (MST)                                │
├─────────────────────────────────────────────────────────┤
│ PK: mstId (Long)             1, 2, 3, 4, 5...           │
│     detailId (Int)            0, 1, 2... (위성 내 패스 인덱스) │
│     satelliteID (String)     "27424"                   │
│     satelliteName (String)    "AQUA"                   │
│     [메타데이터...]                                      │
└─────────────────────────────────────────────────────────┘
                          │
                          │ 1:N
                          │ (mstId, detailId) → (mstId, detailId, index)
                          ▼
┌─────────────────────────────────────────────────────────┐
│ PassScheduleDetail (DTL)                                │
├─────────────────────────────────────────────────────────┤
│ FK: mstId (Long)             → MST.mstId                │
│ FK: detailId (Int)            → MST.detailId             │
│ PK: index (Int)                0, 1, 2, 3...           │
│     [추적 포인트 데이터...]                             │
└─────────────────────────────────────────────────────────┘
```

---

## 구현 계획

### Phase 1: 백엔드 데이터 구조 변경

#### 1.1 전역 MstId 카운터 추가

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`

**변경 위치**:
- 클래스 필드 선언부 (약 200라인 근처)
- `generateAllPassScheduleTrackingDataAsync()` 함수 (약 1489라인)
- `generatePassScheduleTrackingDataAsync()` 함수 (약 1521라인)

**변경 내용**:
```kotlin
@Service
class PassScheduleService(...) {
    // ✅ 전역 MstId 카운터 추가 (기존 globalMstId 대체)
    private val mstIdCounter = AtomicLong(0)
    
    fun generateAllPassScheduleTrackingDataAsync(): Mono<...> {
        // ✅ 전체 생성 시작 시 카운터 초기화
        mstIdCounter.set(0)
        
        return Flux.fromIterable(allTleIds).flatMap { satelliteId ->
            val tleData = passScheduleTleCache[satelliteId]
            if (tleData != null) {
                val (tleLine1, tleLine2, satelliteName) = tleData
                
                // ✅ 현재 카운터 값을 시작 MstId로 전달
                val startMstId = mstIdCounter.get()
                
                generatePassScheduleTrackingDataAsync(
                    satelliteId, tleLine1, tleLine2, satelliteName, startMstId
                ).map { (mstData, dtlData) ->
                    // ✅ 생성된 MST 개수만큼 카운터 증가
                    val passCount = mstData.size
                    mstIdCounter.addAndGet(passCount.toLong())
                    
                    satelliteId to (mstData to dtlData)
                }
            } else {
                Mono.empty()
            }
        }.collectMap(...)
    }
    
    fun generatePassScheduleTrackingDataAsync(
        satelliteId: String, 
        tleLine1: String, 
        tleLine2: String, 
        satelliteName: String? = null,
        startMstId: Long = 0  // ✅ 전역 시작 MstId 파라미터 추가
    ): Mono<Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>> {
        return Mono.fromCallable {
            // ... 기존 로직 ...
            val schedule = orekitCalculator.generateSatelliteTrackingSchedule(...)
            val processedData = try {
                satelliteTrackingProcessor.processFullTransformation(
                    schedule,
                    actualSatelliteName,
                    startMstId  // ✅ 전달
                )
            } catch (e: Exception) {
                logger.error("❌ 위성 추적 데이터 처리 실패: ${e.message}", e)
                throw e
            }
            // ...
        }
    }
}
```

**주의사항**:
- 기존 `globalMstId` 변수가 있다면 제거 또는 대체
- `clearAllPassScheduleTrackingData()` 함수에서도 카운터 초기화 필요 (약 1767라인)

#### 1.2 SatelliteTrackingProcessor 수정

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/processor/SatelliteTrackingProcessor.kt`

**변경 위치**:
- `processFullTransformation()` 함수 (약 280라인)
- `structureOriginalData()` 함수 (약 367라인)
- 모든 DataType 변환 함수들 (axis_transformed, final_transformed, keyhole_axis_transformed, keyhole_final_transformed)

**변경 내용**:
```kotlin
@Service
class SatelliteTrackingProcessor(...) {
    
    fun processFullTransformation(
        schedule: OrekitCalculator.SatelliteTrackingSchedule,
        satelliteName: String? = null,
        startMstId: Long = 0  // ✅ 전역 시작 MstId 파라미터 추가
    ): ProcessedTrackingData {
        // ...
        val (originalMst, originalDtl) = structureOriginalData(
            schedule,
            satelliteId,
            actualSatelliteName,
            startMstId  // ✅ 전달
        )
        
        // ✅ 모든 변환 함수에도 startMstId 전달 필요
        val (axisMst, axisDtl) = structureAxisTransformedData(..., startMstId)
        val (finalMst, finalDtl) = structureFinalTransformedData(..., startMstId)
        val (keyholeAxisMst, keyholeAxisDtl) = structureKeyholeAxisTransformedData(..., startMstId)
        val (keyholeFinalMst, keyholeFinalDtl) = structureKeyholeFinalTransformedData(..., startMstId)
        // ...
    }
    
    private fun structureOriginalData(
        schedule: OrekitCalculator.SatelliteTrackingSchedule,
        satelliteId: String,
        satelliteName: String,
        startMstId: Long = 0  // ✅ 전역 시작 MstId 파라미터 추가
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        
        val originalMst = mutableListOf<Map<String, Any?>>()
        val originalDtl = mutableListOf<Map<String, Any?>>()
        
        schedule.trackingPasses.forEachIndexed { index, pass ->
            // ✅ 전역 고유 MstId 생성 (기존: val mstId = (index + 1).toUInt())
            val mstId = startMstId + index + 1  // Long 타입
            
            // ✅ DetailId는 위성 내 패스 인덱스로 설정 (0, 1, 2...)
            val detailId = index
            
            // ✅ DTL은 100ms 간격 추적 포인트의 Index (기존: "No" to (dtlIndex + 1).toUInt())
            pass.trackingData.forEachIndexed { dtlIndex, data ->
                originalDtl.add(
                    mapOf(
                        "MstId" to mstId,              // ✅ 전역 고유 ID (기존: "MstId" to mstId)
                        "DetailId" to detailId,        // ✅ Detail 구분자 (신규 추가)
                        "Index" to dtlIndex,           // ✅ 100ms 포인트 순번 (기존: "No" to (dtlIndex + 1).toUInt())
                        "Time" to data.timestamp,
                        "Azimuth" to data.azimuth,
                        "Elevation" to data.elevation,
                        "Range" to data.range,
                        "Altitude" to data.altitude,
                        "DataType" to "original"
                    )
                )
            }
            
            // ✅ 상세 데이터에서 메타데이터 계산
            val passDtl = originalDtl.filter { 
                it["MstId"] == mstId && it["DetailId"] == detailId 
            }
            val metrics = calculateMetrics(passDtl)
            
            // ✅ 마스터 데이터 생성 (기존: "No" to mstId)
            originalMst.add(
                mapOf(
                    "MstId" to mstId,                  // ✅ 전역 고유 ID (기존: "No" to mstId)
                    "DetailId" to detailId,            // ✅ Detail 구분자 (신규 추가)
                    "SatelliteID" to satelliteId,      // ✅ 카탈로그 번호
                    "SatelliteName" to satelliteName,   // ✅ 위성 이름
                    "StartTime" to metrics["StartTime"],
                    "EndTime" to metrics["EndTime"],
                    // ... 나머지 메타데이터
                    "DataType" to "original"
                )
            )
        }
        
        return Pair(originalMst, originalDtl)
    }
    
    // ✅ 다른 변환 함수들도 동일하게 수정 필요:
    // - structureAxisTransformedData()
    // - structureFinalTransformedData()
    // - structureKeyholeAxisTransformedData()
    // - structureKeyholeFinalTransformedData()
}
```

**주의사항**:
- 모든 DataType 변환 함수에서 동일한 `startMstId` 사용
- DTL의 `Index`는 0부터 시작 (기존: 1부터 시작)
- MST의 `MstId`는 전역 고유 (기존: 위성별 인덱스)

#### 1.3 PassScheduleService의 모든 조회/필터링 로직 수정

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`

**변경 위치 및 내용**:

1. **`getAllPassScheduleTrackMstMerged()` 함수 (약 1636라인)**
   - `it["No"] == mstId` → `it["MstId"] == mstId.toUInt()` 변경
   - 모든 `No` 필드 참조를 `MstId`로 변경

2. **`getPassScheduleTrackDtlByMstId()` 함수 (약 1607라인)**
   - `it["MstId"] == mstId` 필터링 로직은 이미 `MstId` 사용 중 (유지)
   - 파라미터 타입 확인: `mstId: UInt` → `Long`으로 변경 검토 필요

3. **`getSelectedTrackDtlByMstId()` 함수 (약 2014라인)**
   - `it["MstId"] == mstId` 필터링 로직은 이미 `MstId` 사용 중 (유지)
   - `it["No"] == mstId` 참조가 있다면 `it["MstId"] == mstId.toUInt()`로 변경

4. **`getSelectedTrackMstByMstId()` 함수 (약 1994라인)**
   - `it["No"] == mstId` → `it["MstId"] == mstId.toUInt()` 변경

5. **`determineKeyholeDataType()` 함수 (약 1921라인)**
   - `it["No"] == passId` → `it["MstId"] == passId.toUInt()` 변경

6. **`getTrackingPassMst()` 함수 (약 1974라인)**
   - `it["No"] == passId` → `it["MstId"] == passId.toUInt()` 변경

7. **`executeStateAction()` 함수 (약 420라인)**
   - `currentSchedule["No"]` → `currentSchedule["MstId"]` 변경
   - `nextSchedule?.get("No")` → `nextSchedule?.get("MstId")` 변경

8. **`updateTrackingMstIds()` 함수 (약 674라인)**
   - `currentSchedule?.get("No")` → `currentSchedule?.get("MstId")` 변경
   - `nextSchedule?.get("No")` → `nextSchedule?.get("MstId")` 변경

9. **`handleTrackingStateChangeSeparately()` 함수 (약 639라인)**
   - `lastDisplayedSchedule!!["No"]` → `lastDisplayedSchedule!!["MstId"]` 변경
   - `currentSchedule["No"]` → `currentSchedule["MstId"]` 변경

10. **`sendAdditionalTrackingDataFromDatabase()` 함수 (약 1184라인)**
    - `getSelectedTrackDtlByMstId(passId)` 호출은 이미 `MstId` 사용 (유지)

11. **`preloadTrackingDataCache()` 함수 (약 2267라인)**
    - `getSelectedTrackDtlByMstId(passId)` 호출은 이미 `MstId` 사용 (유지)

12. **`clearAllPassScheduleTrackingData()` 함수 (약 1767라인)**
    - `globalMstId = 0` → `mstIdCounter.set(0)` 변경

#### 1.4 DataStoreService 타입 변경

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/common/DataStoreService.kt` (또는 해당 위치)

**변경 위치**:
- `currentTrackingMstId` 필드 타입 변경
- `nextTrackingMstId` 필드 타입 변경
- 관련 getter/setter 메서드 타입 변경

**변경 내용**:
```kotlin
// 기존
private var currentTrackingMstId: UInt? = null
private var nextTrackingMstId: UInt? = null

fun setCurrentTrackingMstId(mstId: UInt?) { ... }
fun getCurrentTrackingMstId(): UInt? { ... }
fun setNextTrackingMstId(mstId: UInt?) { ... }
fun getNextTrackingMstId(): UInt? { ... }

// 변경 후
private var currentTrackingMstId: Long? = null
private var nextTrackingMstId: Long? = null

fun setCurrentTrackingMstId(mstId: Long?) { ... }
fun getCurrentTrackingMstId(): Long? { ... }
fun setNextTrackingMstId(mstId: Long?) { ... }
fun getNextTrackingMstId(): Long? { ... }
```

**영향받는 코드**:
- `PassScheduleService.kt`의 `updateTrackingMstIds()` 함수 (약 674라인)
- `PassScheduleService.kt`의 `updateTrackingMstIdsAfterTargetSet()` 함수 (약 1797라인)
- `PassScheduleService.kt`의 `executeStateAction()` 함수 (약 420라인)
- 모든 `currentSchedule?.get("MstId")` → `Long` 타입으로 캐스팅

#### 1.5 모든 함수 파라미터 타입 변경 (UInt → Long)

**변경 위치 및 내용**:

1. **`PassScheduleService.kt`의 모든 passId/mstId 파라미터**:
   ```kotlin
   // 기존
   fun sendInitialTrackingData(passId: UInt) { ... }
   fun handleTrackingDataRequest(passId: UInt, ...) { ... }
   fun sendAdditionalTrackingData(passId: UInt, ...) { ... }
   fun prepareTrackingStart(mstId: UInt?) { ... }
   fun moveToStartPosition(passId: UInt) { ... }
   fun sendHeaderTrackingData(passId: UInt) { ... }
   fun preloadTrackingDataCache(passId: UInt) { ... }
   fun getTrackingPassMst(passId: UInt): Map<String, Any?>? { ... }
   fun getSelectedTrackMstByMstId(mstId: UInt): Map<String, Any?>? { ... }
   fun getSelectedTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> { ... }
   fun getPassScheduleTrackDtlByMstId(satelliteId: String, passId: UInt, ...): List<Map<String, Any?>> { ... }
   fun determineKeyholeDataType(passId: UInt, ...): String? { ... }
   fun getTrackingTargetByMstId(mstId: UInt): TrackingTarget? { ... }
   
   // 변경 후
   fun sendInitialTrackingData(passId: Long) { ... }
   fun handleTrackingDataRequest(passId: Long, ...) { ... }
   fun sendAdditionalTrackingData(passId: Long, ...) { ... }
   fun prepareTrackingStart(mstId: Long?) { ... }
   fun moveToStartPosition(passId: Long) { ... }
   fun sendHeaderTrackingData(passId: Long) { ... }
   fun preloadTrackingDataCache(passId: Long) { ... }
   fun getTrackingPassMst(passId: Long): Map<String, Any?>? { ... }
   fun getSelectedTrackMstByMstId(mstId: Long): Map<String, Any?>? { ... }
   fun getSelectedTrackDtlByMstId(mstId: Long): List<Map<String, Any?>> { ... }
   fun getPassScheduleTrackDtlByMstId(satelliteId: String, passId: Long, ...): List<Map<String, Any?>> { ... }
   fun determineKeyholeDataType(passId: Long, ...): String? { ... }
   fun getTrackingTargetByMstId(mstId: Long): TrackingTarget? { ... }
   ```

2. **`TrackingDataCache` 데이터 클래스**:
   ```kotlin
   // 기존
   data class TrackingDataCache(
       val passId: UInt,
       ...
   )
   
   // 변경 후
   data class TrackingDataCache(
       val passId: Long,
       ...
   )
   ```

3. **`TrackingTarget` 데이터 클래스**:
   ```kotlin
   // 기존
   data class TrackingTarget(
       val mstId: UInt,
       ...
   )
   
   // 변경 후
   data class TrackingTarget(
       val mstId: Long,
       ...
   )
   ```

4. **`PassScheduleController.kt`의 API 엔드포인트**:
   ```kotlin
   // 기존
   @GetMapping("/tracking-targets/mst/{mstId}")
   fun getTrackingTargetByMstId(@PathVariable mstId: UInt): ResponseEntity<...> { ... }
   
   @GetMapping("/selected-tracking/detail/mst/{mstId}")
   fun getSelectedTrackingDetailByMstId(@PathVariable mstId: UInt): ResponseEntity<...> { ... }
   
   // 변경 후
   @GetMapping("/tracking-targets/mst/{mstId}")
   fun getTrackingTargetByMstId(@PathVariable mstId: Long): ResponseEntity<...> { ... }
   
   @GetMapping("/selected-tracking/detail/mst/{mstId}")
   fun getSelectedTrackingDetailByMstId(@PathVariable mstId: Long): ResponseEntity<...> { ... }
   ```

5. **이벤트 구독 로직**:
   ```kotlin
   // 기존
   val passId = schedule["No"] as? UInt
   
   // 변경 후
   val passId = (schedule["MstId"] as? Number)?.toLong()
   ```

#### 1.6 필드명 변경 사항 요약

**변경 전**:
```kotlin
// MST
"No" to mstId                    // 위성별 인덱스 (1, 2, 3... per satellite)

// DTL
"MstId" to mstId                  // 위성별 인덱스 (FK)
"No" to (dtlIndex + 1).toUInt()  // DTL 순번 (1, 2, 3...)
```

**변경 후**:
```kotlin
// MST
"MstId" to mstId                  // 전역 고유 ID (1, 2, 3, 4, 5... globally)
"DetailId" to detailId           // Detail 구분자 (현재는 항상 0)

// DTL
"MstId" to mstId                  // 전역 고유 ID (FK)
"DetailId" to detailId           // Detail 구분자 (FK)
"Index" to dtlIndex              // 100ms 포인트 순번 (0, 1, 2, 3...)
```

### Phase 2: API 응답 구조 변경

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/controller/mode/PassScheduleController.kt`

#### 2.1 API 응답 필드명 변경

**변경 위치**:
- `getAllTrackingMaster()` 엔드포인트 (약 1000라인 근처)
- `setTrackingTargets()` 엔드포인트 (약 1184라인)
- 모든 MST/DTL 응답 매핑

**변경 내용**:
```kotlin
// 기존 응답
{
  "No": 1,
  "SatelliteID": "27424",
  "SatelliteName": "AQUA",
  ...
}

// 변경 후 응답
{
  "MstId": 1,              // ✅ 전역 고유 ID
  "DetailId": 0,           // ✅ Detail 구분자 (신규)
  "SatelliteID": "27424",  // ✅ 카탈로그 번호
  "SatelliteName": "AQUA", // ✅ 위성 이름
  ...
}

// DTL 응답
{
  "MstId": 1,              // ✅ 전역 고유 ID
  "DetailId": 0,           // ✅ Detail 구분자 (신규)
  "Index": 0,              // ✅ 100ms 포인트 순번 (기존: "No": 1)
  "Time": "...",
  "Azimuth": 180.5,
  "Elevation": 10.2,
  ...
}
```

**주의사항**:
- `No` 필드는 완전히 제거하고 `MstId`만 사용
- 한 번에 모든 변경 사항 적용 (하위 호환성 고려 없음)

#### 2.2 SetTrackingTargetsRequest 타입 변경

**변경 위치**: `PassScheduleController.kt`의 `SetTrackingTargetsRequest` 데이터 클래스

**변경 내용**:
```kotlin
// 기존
data class TrackingTarget(
    val mstId: UInt,  // ✅ 이미 MstId 사용 중 (유지)
    val satelliteId: String,
    val satelliteName: String,
    ...
)

// 변경 없음 (이미 MstId 사용 중)
// 단, 타입을 UInt → Long으로 변경 검토 필요
```

### Phase 3: 프론트엔드 인터페이스 변경

#### 3.1 TypeScript 인터페이스 수정

**파일**: 
- `ACS/src/services/mode/passScheduleService.ts` (약 100라인)
- `ACS/src/stores/mode/passScheduleStore.ts` (약 15라인)

**변경 내용**:
```typescript
// 기존
export interface PassScheduleMasterData {
  No: number
  SatelliteID: string
  SatelliteName: string
  ...
}

export interface ScheduleItem {
  no: number
  satelliteId?: string
  satelliteName: string
  index?: number
  ...
}

export interface TrackingDetailItem {
  Time: string
  Azimuth: number
  Elevation: number
  [key: string]: string | number | boolean | null | undefined
}

// 변경 후
export interface PassScheduleMasterData {
  MstId: number              // ✅ 전역 고유 ID (기존: No)
  DetailId?: number          // ✅ Detail 구분자 (신규, 기본값: 0)
  SatelliteID: string        // ✅ 카탈로그 번호
  SatelliteName: string      // ✅ 위성 이름
  // ❌ No 필드 제거
  ...
}

export interface ScheduleItem {
  mstId: number              // ✅ 전역 고유 ID (기존: no)
  detailId?: number          // ✅ Detail 구분자 (신규, 기본값: 0)
  satelliteID?: string       // ✅ 카탈로그 번호 (기존: satelliteId)
  satelliteName: string      // ✅ 위성 이름
  no?: number                // ✅ UI 표시용 재순번 (1, 2, 3...)
  index?: number             // ✅ mstId와 연계된 인덱스 (mstId와 동일)
  ...
}

export interface TrackingDetailItem {
  MstId: number              // ✅ MST 참조 (신규)
  DetailId?: number          // ✅ Detail 참조 (신규)
  Index: number              // ✅ 100ms 포인트 순번 (기존: No)
  Time: string
  Azimuth: number
  Elevation: number
  // ❌ No 필드 제거
  [key: string]: string | number | boolean | null | undefined
}

export interface TrackingTarget {
  mstId: number              // ✅ 전역 고유 ID (유지)
  // ❌ no 필드 제거
  satelliteId: string
  satelliteName: string
  ...
}
```

#### 3.2 데이터 매핑 로직 수정

**파일**: `ACS/src/stores/mode/passScheduleStore.ts`

**변경 위치 및 내용**:

1. **`fetchScheduleDataFromServer()` 함수 (약 200라인)**
   ```typescript
   // 기존
   const scheduleItem: ScheduleItem = {
     no: pass.No,
     satelliteId: pass.SatelliteID,
     satelliteName: pass.SatelliteName,
     ...
   }
   
   // 변경 후
   const scheduleItem: ScheduleItem = {
     mstId: pass.MstId,                          // ✅ 전역 고유 ID (필수)
     detailId: pass.DetailId ?? 0,               // ✅ Detail 구분자
     satelliteID: pass.SatelliteID,              // ✅ 카탈로그 번호
     satelliteName: pass.SatelliteName,          // ✅ 위성 이름
     no: index + 1,                              // ✅ UI 표시용 재순번 (1, 2, 3...)
     index: pass.MstId,                          // ✅ mstId와 연계된 인덱스 (mstId와 동일)
     ...
   }
   ```

2. **`setTrackingTargets()` 함수 (약 1321라인)**
   ```typescript
   // 기존
   const mstId = schedule.no
   
   // 변경 후
   const trackingTargets: TrackingTarget[] = schedules.map((schedule) => ({
     mstId: schedule.mstId,                      // ✅ 전역 고유 ID (필수)
     satelliteId: schedule.satelliteID || schedule.satelliteId || '',
     satelliteName: schedule.satelliteName,
     ...
   }))
   ```

3. **`loadTrackingDetailData()` 함수 (약 600라인)**
   ```typescript
   // DTL 데이터 매핑 시 Index 필드 사용
   const detailItem = {
     mstId: item.MstId,                          // ✅ 전역 고유 ID (필수)
     detailId: item.DetailId ?? 0,               // ✅ Detail 구분자
     index: item.Index,                          // ✅ 100ms 포인트 순번 (필수)
     time: item.Time,
     azimuth: item.Azimuth,
     elevation: item.Elevation,
     ...
   }
   ```

#### 3.3 SelectScheduleContent.vue 수정

**파일**: `ACS/src/components/content/SelectScheduleContent.vue`

**변경 위치 및 내용**:

1. **`scheduleData` computed (약 200라인)**
   ```typescript
   // 기존: no는 원본, index는 표시용
   // 변경 후: mstId는 전역 고유 ID, index는 표시용
   const scheduleData = computed(() => {
     const rawData = passScheduleStore.scheduleData
     if (rawData.length === 0) return []
     
     const sortedData = [...rawData].sort((a, b) => {
       return new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
     })
     
     return sortedData.map((item, sortedIndex) => ({
       ...item,
       mstId: item.mstId,                       // ✅ 전역 고유 ID (필수)
       detailId: item.detailId ?? 0,           // ✅ Detail 구분자
       index: item.mstId,                      // ✅ mstId와 연계된 인덱스 (mstId와 동일)
       no: sortedIndex + 1,                    // ✅ UI 표시용 재순번 (1, 2, 3...)
     }))
   })
   ```

2. **`isScheduleSelected()` 함수 (약 400라인)**
   ```typescript
   // 기존: index 기준 비교
   // 변경 후: mstId 기준 비교
   const isScheduleSelected = (schedule: ScheduleItem): boolean => {
     return selectedRows.value.some(selected => 
       selected.mstId === schedule.mstId  // ✅ mstId 기준 비교
     )
   }
   ```

3. **`handleSelect()` 함수 (약 986라인)**
   ```typescript
   // 기존: index를 no로 덮어쓰기
   // 변경 후: mstId 사용
   const schedulesWithMstId = selectedRows.value.map(s => ({
     ...s,
     mstId: s.mstId,                            // ✅ 전역 고유 ID (필수)
     detailId: s.detailId ?? 0,                 // ✅ Detail 구분자
   }))
   
   const success = await passScheduleStore.replaceSelectedSchedules(schedulesWithMstId)
   ```

4. **`onMounted()` 복원 로직 (약 1200라인)**
   ```typescript
   // 기존: index 기준 복원
   // 변경 후: mstId 기준 복원
   const savedMstIds = passScheduleStore.loadSelectedScheduleMstIdsFromLocalStorage()
   
   scheduleData.value.forEach((schedule) => {
     const isSelected = savedMstIds.includes(schedule.mstId)  // ✅ mstId 기준 복원
     if (isSelected && canSelectSchedule(schedule)) {
       selectedRows.value.push({ ...schedule })
     }
   })
   ```

5. **localStorage 저장 로직 (약 1050라인)**
   ```typescript
   // 기존: selectedIndexes, selectedNos 저장
   // 변경 후: selectedMstIds 추가 저장
   const selectedMstIds = sortedSelected.map(s => s.mstId)  // ✅ mstId만 저장
   
   const dataToSave = {
     selectedMstIds,        // ✅ 전역 고유 ID만 저장
     savedAt: Date.now()
   }
   ```

#### 3.4 PassSchedulePage.vue 수정

**파일**: `ACS/src/pages/mode/PassSchedulePage.vue`

**변경 위치 및 내용**:

1. **`displaySchedule` computed (약 200라인)**
   ```typescript
   // 기존: no 기준 표시
   // 변경 후: mstId 기준 표시 (하위 호환성 유지)
   const displaySchedule = computed(() => {
     const schedule = autoSelectedSchedule.value || 
                     passScheduleStore.selectedSchedule || 
                     selectedSchedule.value
     
     if (schedule) {
       return {
         ...schedule,
         mstId: schedule.mstId,                  // ✅ 전역 고유 ID (필수)
       }
     }
     return null
   })
   ```

2. **차트 표시 로직 (약 500라인)**
   ```typescript
   // 기존: index 기준 매칭
   // 변경 후: mstId 기준 매칭
   const newSchedule = sortedScheduleList.value.find(s => 
     Number(s.mstId) === Number(newMstId)  // ✅ mstId 기준 매칭
   )
   ```

3. **DOM 직접 조작 로직 (약 816라인)**
   ```typescript
   // 기존: index 기준 색상 적용
   // 변경 후: mstId 기준 색상 적용
   const indexValue = indexCell?.textContent?.trim()
   const mstIdValue = mstIdCell?.textContent?.trim()  // mstId 컬럼 추가 필요
   const mstIdNumber = Number(mstIdValue ?? indexValue)
   
   if (current !== null && mstIdNumber === current) {
     // 현재 스케줄 하이라이트
   }
   ```

4. **`getRowClass()` 함수 (약 750라인)**
   ```typescript
   // 기존: index/no 기준 매칭
   // 변경 후: mstId 기준 매칭
   const isCurrentMatch = currentMstId !== null &&
     schedule.mstId === currentMstId  // ✅ mstId 기준 매칭
   ```

#### 3.5 localStorage 저장/복원 로직 수정

**파일**: `ACS/src/stores/mode/passScheduleStore.ts`

**변경 위치 및 내용**:

1. **`saveSelectedScheduleNosToLocalStorage()` 함수 (약 1000라인)**
   ```typescript
   // 기존: selectedNos만 저장
   // 변경 후: selectedMstIds도 저장
   const saveSelectedScheduleNosToLocalStorage = () => {
     try {
       const storageKey = 'pass-schedule-selected-nos'
       const selectedMstIds = selectedScheduleList.value.map((s) => s.mstId)  // ✅ mstId만 저장
       
       const dataToSave = {
         selectedMstIds,        // ✅ 전역 고유 ID만 저장
         savedAt: Date.now(),
       }
       localStorage.setItem(storageKey, JSON.stringify(dataToSave))
     } catch (error) {
       console.error('❌ 선택된 스케줄 번호 저장 실패:', error)
     }
   }
   ```

2. **`loadSelectedScheduleNosFromLocalStorage()` 함수 (약 1050라인)**
   ```typescript
   // 기존: selectedNos만 로드
   // 변경 후: selectedMstIds 우선 로드
   const loadSelectedScheduleNosFromLocalStorage = (): number[] => {
     try {
       const storageKey = 'pass-schedule-selected-nos'
       const savedData = localStorage.getItem(storageKey)
       
       if (!savedData) {
         return []
       }
       
       const parsed = JSON.parse(savedData) as {
         selectedMstIds?: number[]    // ✅ 전역 고유 ID
         savedAt?: number
       }
       
       // ✅ selectedMstIds만 사용
       if (parsed.selectedMstIds && Array.isArray(parsed.selectedMstIds)) {
         return parsed.selectedMstIds
       } else if (parsed.selectedIndexes && Array.isArray(parsed.selectedIndexes)) {
         return parsed.selectedIndexes
       }
       
       return []
     } catch (error) {
       console.error('❌ 선택된 스케줄 번호 복원 실패:', error)
       return []
     }
   }
   ```

### Phase 4: ICD 통신 및 상태 관리 수정

#### 4.1 ICD 통신 로직 수정

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`

**변경 위치**:
- `sendInitialTrackingData()` 함수 (약 1000라인)
- `handleTrackingDataRequest()` 함수 (약 1050라인)
- 모든 `passId` 파라미터 사용 부분

**변경 내용**:
```kotlin
// 기존: passId는 위성별 인덱스 (UInt)
// 변경 후: passId는 전역 고유 ID (Long으로 변경 검토)

// ICD 통신에서는 이미 mstId를 사용하므로 큰 변경 없음
// 단, 타입 일관성을 위해 UInt → Long 검토 필요
```

#### 4.2 DataStoreService의 mstId 관리 수정

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/common/DataStoreService.kt`

**변경 위치**:
- `setCurrentTrackingMstId()` 함수
- `setNextTrackingMstId()` 함수
- `getCurrentTrackingMstId()` 함수
- `getNextTrackingMstId()` 함수

**변경 내용**:
```kotlin
// 기존: UInt 타입
// 변경 후: Long 타입 (전역 고유 ID 지원)

fun setCurrentTrackingMstId(mstId: Long?) {  // ✅ UInt → Long
    currentTrackingMstId = mstId
}

fun setNextTrackingMstId(mstId: Long?) {     // ✅ UInt → Long
    nextTrackingMstId = mstId
}
```

#### 4.3 WebSocket 전송 데이터 수정

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`

**변경 위치**:
- `updateTrackingMstIdsAfterTargetSet()` 함수 (약 1797라인)
- WebSocket 전송 로직

**변경 내용**:
```kotlin
// 기존: No 필드 전송
// 변경 후: MstId 필드 전송 (No는 Deprecated로 함께 전송)
```

### Phase 5: 수동 검증 체크리스트

#### 5.1 백엔드 검증 체크리스트

**전역 MstId 생성 검증**:
- [ ] 여러 위성의 TLE 업로드 후 전역 MstId가 1부터 시작하여 연속적으로 생성되는지 확인
- [ ] 위성별로 MstId가 겹치지 않는지 확인
- [ ] 모든 MST의 MstId가 전역적으로 고유한지 확인

**DTL-MST 참조 무결성 검증**:
- [ ] 모든 DTL의 MstId가 MST에 존재하는지 확인
- [ ] DTL의 (MstId, DetailId) 조합이 MST에 존재하는지 확인
- [ ] DTL의 Index가 0부터 시작하는지 확인

**DetailId 및 Index 검증**:
- [ ] 모든 MST의 DetailId가 0인지 확인
- [ ] 각 MST의 DTL Index가 0부터 시작하여 연속적인지 확인

**API 응답 검증**:
- [ ] MST API 응답에 `MstId`, `DetailId` 필드 포함 확인
- [ ] DTL API 응답에 `MstId`, `DetailId`, `Index` 필드 포함 확인
- [ ] 하위 호환성을 위한 `No` 필드 포함 확인 (Deprecated)

#### 5.2 프론트엔드 검증 체크리스트

**데이터 매핑 검증**:
- [ ] API 응답에서 `MstId` 정상 매핑 확인
- [ ] 하위 호환성을 위한 `No` 필드 폴백 동작 확인
- [ ] `DetailId` 기본값(0) 설정 확인

**localStorage 저장/복원 검증**:
- [ ] 선택된 스케줄의 `MstId`가 localStorage에 저장되는지 확인
- [ ] 페이지 재접근 시 `MstId` 기준으로 복원되는지 확인
- [ ] 레거시 `No` 기반 저장 데이터 복원 확인

**UI 표시 검증**:
- [ ] 스케줄 테이블에 `MstId` 표시 확인
- [ ] 현재 추적 중인 스케줄 하이라이트 확인 (MstId 기준)
- [ ] 다음 추적 예정 스케줄 하이라이트 확인 (MstId 기준)

**스케줄 선택 검증**:
- [ ] 여러 위성의 동일한 `no` 값을 가진 스케줄을 `mstId`로 구분하여 선택할 수 있는지 확인
- [ ] 선택된 스케줄이 추적 대상으로 정상 설정되는지 확인

#### 5.3 통합 워크플로우 검증

**전체 워크플로우 테스트**:
1. [ ] TLE 업로드 (AQUA, AURA)
2. [ ] 스케줄 생성 확인 (전역 MstId 확인)
3. [ ] 여러 위성의 스케줄 선택 (mstId 기준)
4. [ ] 추적 대상 설정
5. [ ] localStorage 저장 확인 (selectedMstIds)
6. [ ] 페이지 새로고침
7. [ ] localStorage 복원 확인
8. [ ] PassSchedulePage에서 현재/다음 스케줄 하이라이트 확인 (mstId 기준)

**하위 호환성 테스트**:
1. [ ] 레거시 데이터 (selectedNos만 있음) 로드
2. [ ] No 필드를 MstId로 매핑하여 복원
3. [ ] 정상 동작 확인

**여러 위성 동시 선택 테스트**:
1. [ ] AQUA의 패스 1 (mstId=1, no=1) 선택
2. [ ] AURA의 패스 1 (mstId=3, no=1) 선택
3. [ ] 두 스케줄이 정상적으로 구분되어 선택되는지 확인
4. [ ] 추적 대상 설정 시 두 스케줄 모두 포함되는지 확인

---

## 📋 프론트엔드 통합 검증 재분석 요약

### 핵심 발견 사항

1. **현재 코드 상태**: 계획서와 실제 코드 간 불일치 존재
   - `passScheduleStore.ts`: `no`를 `mstId`로 사용 중 (위성별 인덱스)
   - `PassSchedulePage.vue`: `index`를 사용하여 하이라이트 (mstId 아님)
   - `localStorage`: `selectedMstIds` 저장 없음 (계획만 있음)

2. **검증 방법**: 수동 검증 체크리스트 사용
   - 테스트 코드 작성 대신 수동 검증 체크리스트로 검증
   - Phase 5의 검증 체크리스트 참조

### 구현 전 필수 체크리스트

- [ ] 백엔드 전역 MstId 카운터 구현
- [ ] 프론트엔드 `schedule.no` → `schedule.mstId` 변경
- [ ] localStorage `selectedMstIds` 저장 로직 추가
- [ ] PassSchedulePage `index` → `mstId` 기준 하이라이트 변경
- [ ] 하위 호환성 폴백 메커니즘 구현

## 현재 코드 상태 분석 및 주의사항

### ⚠️ 현재 코드와 계획서의 불일치 사항

#### 1. 프론트엔드 현재 상태

**`passScheduleStore.ts` (약 1328라인)**:
```typescript
// 현재 코드: no를 mstId로 사용 중 (위성별 인덱스)
const mstId = schedule.no  // ❌ 위성별 인덱스 (전역 고유 ID 아님)
```

**문제점**:
- `schedule.no`는 위성별 인덱스 (1, 2, 3... per satellite)
- 전역 고유 ID가 아님
- 여러 위성의 동일한 `no` 값을 구분할 수 없음

**`PassSchedulePage.vue` (약 750라인)**:
```typescript
// 현재 코드: index를 사용하여 하이라이트
const getRowClass = (props: { row: ScheduleItem }) => {
  const tableIndex = schedule.index  // ❌ index 사용 (mstId 아님)
  // ...
}
```

**문제점**:
- `index`는 표시용 순번 (1, 2, 3...)
- `mstId`와 다를 수 있음
- 하이라이트 로직이 잘못된 필드 사용

**`localStorage` 저장 로직 (약 1009라인)**:
```typescript
// 현재 코드: selectedNos만 저장
const saveSelectedScheduleNosToLocalStorage = () => {
  const selectedNos = selectedScheduleList.value.map((s) => s.no)
  // ❌ selectedMstIds 저장 없음
}
```

**문제점**:
- `selectedMstIds` 저장 계획이지만 실제로는 `selectedNos`만 저장
- 전역 고유 ID 기반 복원 불가능

#### 2. 백엔드 현재 상태

**`SatelliteTrackingProcessor.kt` (약 381라인)**:
```kotlin
// 현재 코드: 위성별 인덱스 사용
val mstId = (index + 1).toUInt()  // ❌ 위성별 인덱스 (1, 2, 3... per satellite)
```

**문제점**:
- 위성별로 1부터 시작하는 인덱스
- 전역 고유 ID가 아님
- 여러 위성의 동일한 인덱스 구분 불가능

#### 3. 구현 시 주의사항

**⚠️ 필수 수정 사항**:
1. **백엔드**: 전역 MstId 카운터 구현 필수
2. **프론트엔드**: `schedule.no` → `schedule.mstId` 변경 필수
3. **localStorage**: `selectedMstIds` 저장 로직 추가 필수
4. **PassSchedulePage**: `index` → `mstId` 기준 하이라이트 변경 필수

**⚠️ 하위 호환성 고려사항**:
- 기존 `no` 필드는 Deprecated로 유지
- localStorage 복원 시 `selectedMstIds` 우선, `selectedNos` 폴백
- API 응답에 `No` 필드도 함께 반환 (Deprecated)

## 검증 계획

### 1. 데이터 일관성 검증

#### 1.1 전역 MstId 고유성 검증

**검증 방법**:
```kotlin
// PassScheduleServiceTest.kt
@Test
fun testGlobalMstIdUniqueness() {
    // 1. 여러 위성의 TLE 업로드
    val aquaTle = TLEItem("27424", "AQUA", ...)
    val auraTle = TLEItem("27421", "AURA", ...)
    
    // 2. 스케줄 생성
    passScheduleService.addTleAndGenerateTrackingData(aquaTle)
    passScheduleService.addTleAndGenerateTrackingData(auraTle)
    
    // 3. 모든 MST의 MstId 수집
    val allMstIds = passScheduleService.getAllPassScheduleTrackMstMerged()
        .map { it["MstId"] as Long }
    
    // 4. 중복 확인
    val uniqueIds = allMstIds.toSet()
    assertEquals(allMstIds.size, uniqueIds.size, "MstId는 전역적으로 고유해야 함")
    
    // 5. 연속성 확인 (1부터 시작하여 연속)
    val sortedIds = allMstIds.sorted()
    assertEquals(1L, sortedIds.first(), "MstId는 1부터 시작해야 함")
    for (i in sortedIds.indices) {
        assertEquals(i + 1L, sortedIds[i], "MstId는 연속적이어야 함")
    }
}
```

**검증 항목**:
- [ ] 모든 위성의 MstId가 전역적으로 고유한지 확인
- [ ] MstId가 1부터 시작하여 연속적인지 확인
- [ ] 위성별로 MstId가 겹치지 않는지 확인

#### 1.2 DTL-MST 참조 무결성 검증

**검증 방법**:
```kotlin
@Test
fun testDtlMstReferenceIntegrity() {
    val allMst = passScheduleService.getAllPassScheduleTrackMstMerged()
    val allDtl = passScheduleService.getAllPassScheduleTrackDtl()
    
    // 모든 DTL의 MstId가 MST에 존재하는지 확인
    val mstIds = allMst.map { it["MstId"] as Long }.toSet()
    
    allDtl.values.flatten().forEach { dtl ->
        val dtlMstId = dtl["MstId"] as Long
        assertTrue(mstIds.contains(dtlMstId), 
            "DTL의 MstId($dtlMstId)가 MST에 존재해야 함")
        
        val dtlDetailId = dtl["DetailId"] as? Int ?: 0
        val mst = allMst.find { 
            it["MstId"] == dtlMstId && 
            (it["DetailId"] as? Int ?: 0) == dtlDetailId 
        }
        assertNotNull(mst, "DTL의 (MstId, DetailId) 조합이 MST에 존재해야 함")
    }
}
```

**검증 항목**:
- [ ] 모든 DTL의 MstId가 MST에 존재하는지 확인
- [ ] DTL의 (MstId, DetailId) 조합이 MST에 존재하는지 확인
- [ ] DTL의 Index가 0부터 시작하는지 확인

#### 1.3 DetailId 및 Index 검증

**검증 방법**:
```kotlin
@Test
fun testDetailIdAndIndex() {
    val allMst = passScheduleService.getAllPassScheduleTrackMstMerged()
    val allDtl = passScheduleService.getAllPassScheduleTrackDtl()
    
    // 모든 MST의 DetailId가 0인지 확인
    allMst.forEach { mst ->
        val detailId = mst["DetailId"] as? Int ?: 0
        assertEquals(0, detailId, "현재 DetailId는 항상 0이어야 함")
    }
    
    // 각 MST의 DTL Index가 0부터 시작하는지 확인
    allMst.forEach { mst ->
        val mstId = mst["MstId"] as Long
        val detailId = mst["DetailId"] as? Int ?: 0
        
        val dtlForMst = allDtl.values.flatten()
            .filter { 
                it["MstId"] == mstId && 
                (it["DetailId"] as? Int ?: 0) == detailId 
            }
            .sortedBy { it["Index"] as Int }
        
        // Index가 0부터 시작하여 연속적인지 확인
        dtlForMst.forEachIndexed { index, dtl ->
            val dtlIndex = dtl["Index"] as Int
            assertEquals(index, dtlIndex, 
                "DTL의 Index는 0부터 시작하여 연속적이어야 함")
        }
    }
}
```

**검증 항목**:
- [ ] 모든 MST의 DetailId가 0인지 확인
- [ ] 각 MST의 DTL Index가 0부터 시작하는지 확인
- [ ] DTL Index가 연속적인지 확인

### 2. API 호환성 검증

#### 2.1 API 응답 구조 검증

**검증 항목**:
- [ ] MST API 응답에 `MstId`, `DetailId` 필드 포함 확인
- [ ] DTL API 응답에 `MstId`, `DetailId`, `Index` 필드 포함 확인
- [ ] 하위 호환성을 위한 `No` 필드 포함 확인 (Deprecated)
- [ ] 필드 타입이 올바른지 확인 (MstId: number, DetailId: number, Index: number)

#### 2.2 추적 대상 설정 API 검증

**검증 항목**:
- [ ] `setTrackingTargets` API가 `MstId`를 정상적으로 수신하는지 확인
- [ ] 여러 위성의 스케줄을 선택할 때 MstId 고유성 확인
- [ ] API 응답에 `MstId` 필드 포함 확인

### 3. 성능 검증

#### 3.1 전역 카운터 동시성 검증

**검증 항목**:
- [ ] 동시성 환경에서 MstId 고유성 보장 확인 (여러 위성 동시 업로드 시나리오)
- [ ] AtomicLong의 thread-safety 확인
- [ ] 대량 데이터 생성 시 성능 확인

#### 3.2 조회 성능 검증

**검증 항목**:
- [ ] 대량 데이터 조회 성능 확인 (100개 위성 기준, 1초 이내)
- [ ] MstId 기준 필터링 성능 확인 (10ms 이내)
- [ ] DTL 조회 성능 확인

### 4. 프론트엔드 통합 검증

#### 4.1 데이터 매핑 검증

**검증 항목**:
- [ ] API 응답에서 `MstId` 정상 매핑 확인
- [ ] 하위 호환성을 위한 `No` 필드 폴백 동작 확인
- [ ] `DetailId` 기본값(0) 설정 확인

#### 4.2 localStorage 저장/복원 검증

**검증 항목**:
- [ ] 선택된 스케줄의 `MstId`가 localStorage에 저장되는지 확인
- [ ] 페이지 재접근 시 `MstId` 기준으로 복원되는지 확인
- [ ] 레거시 `No` 기반 저장 데이터 복원 확인

#### 4.3 UI 표시 검증

**검증 항목**:
- [ ] 스케줄 테이블에 `MstId` 표시 확인
- [ ] 현재 추적 중인 스케줄 하이라이트 확인 (MstId 기준)
- [ ] 다음 추적 예정 스케줄 하이라이트 확인 (MstId 기준)

---

## 마이그레이션 계획

### 1. 데이터 마이그레이션

#### 1.1 기존 데이터 처리 전략

**옵션 A: 데이터 재생성 (권장)**
- 기존 데이터 삭제 후 TLE 재업로드
- 새로운 전역 MstId 자동 생성
- **장점**: 깔끔하고 일관성 보장
- **단점**: 사용자가 TLE를 다시 업로드해야 함

**옵션 B: 데이터 마이그레이션 스크립트**
- 기존 `No` 필드를 기반으로 전역 MstId 재생성
- 위성별로 그룹화하여 순차적으로 MstId 할당
- **장점**: 기존 데이터 유지
- **단점**: 복잡한 마이그레이션 로직 필요

**권장 사항**: 옵션 A (데이터 재생성) 권장
- PassSchedule 데이터는 TLE 기반으로 생성되므로 재생성이 자연스러움
- 마이그레이션 스크립트의 복잡도와 위험도가 높음

#### 1.2 마이그레이션 스크립트 (옵션 B 선택 시)

```kotlin
@Service
class PassScheduleMigrationService {
    
    fun migrateExistingDataToGlobalMstId() {
        logger.info("🔄 PassSchedule 데이터 마이그레이션 시작")
        
        // 1. 모든 위성별 MST 데이터 수집
        val allSatellites = passScheduleTrackMstStorage.keys.toList()
        val allMstBySatellite = allSatellites.map { satelliteId ->
            satelliteId to passScheduleTrackMstStorage[satelliteId]!!
        }
        
        // 2. 위성별로 정렬 (일관된 순서 보장)
        val sortedSatellites = allMstBySatellite.sortedBy { it.first }
        
        // 3. 전역 MstId 할당
        var globalMstId = 1L
        val mstIdMapping = mutableMapOf<Pair<String, UInt>, Long>()  // (satelliteId, oldNo) -> newMstId
        
        sortedSatellites.forEach { (satelliteId, mstList) ->
            val sortedMst = mstList.sortedBy { 
                (it["No"] as? UInt) ?: 0u 
            }
            
            sortedMst.forEach { mst ->
                val oldNo = mst["No"] as? UInt ?: 0u
                val newMstId = globalMstId++
                
                mstIdMapping[satelliteId to oldNo] = newMstId
                
                // MST 업데이트
                mst["MstId"] = newMstId.toUInt()
                mst["DetailId"] = 0
                // No 필드는 Deprecated로 유지
            }
        }
        
        // 4. DTL 업데이트
        sortedSatellites.forEach { (satelliteId, _) ->
            val dtlList = passScheduleTrackDtlStorage[satelliteId] ?: emptyList()
            
            dtlList.forEach { dtl ->
                val oldMstId = dtl["MstId"] as? UInt ?: 0u
                val newMstId = mstIdMapping[satelliteId to oldMstId]
                
                if (newMstId != null) {
                    dtl["MstId"] = newMstId.toUInt()
                    dtl["DetailId"] = 0
                    // Index는 기존 No - 1로 변환 (1-based -> 0-based)
                    val oldNo = dtl["No"] as? UInt ?: 1u
                    dtl["Index"] = (oldNo - 1u).toInt()
                }
            }
        }
        
        logger.info("✅ PassSchedule 데이터 마이그레이션 완료: ${globalMstId - 1}개 MST 업데이트")
    }
}
```

### 2. 한 번에 변경하는 방식 (Big Bang Approach)

**전략**: 하위 호환성 고려 없이 한 번에 모든 변경 사항 적용

**핵심 원칙**:
1. **`No` 필드 완전 제거**: 백엔드와 프론트엔드 모두에서 `No` 필드 제거
2. **`MstId` 필드로 통일**: 모든 식별자 로직을 `MstId` 기반으로 변경
3. **폴백 메커니즘 제거**: `?? data.No` 같은 폴백 로직 제거
4. **localStorage 완전 교체**: 기존 `selectedNos` 저장 방식 제거, `selectedMstIds`만 사용

**필드 역할 명확화**:
- `mstId`: 실제 식별자 (백엔드 `MstId`와 일치, 전역 고유 ID)
- `no`: UI 표시용 재순번 (1, 2, 3... 프론트엔드에서만 사용)
- `index`: mstId와 연계된 인덱스 (mstId와 동일 값, 프론트엔드에서만 사용)

**API 응답 예시**:
```json
{
  "MstId": 1,              // ✅ 전역 고유 ID
  "DetailId": 0,           // ✅ Detail 구분자
  "SatelliteID": "27424",
  "SatelliteName": "AQUA",
  ...
}
```

**localStorage 저장 방식**:
```typescript
// 저장: selectedMstIds만 사용
const dataToSave = {
  selectedMstIds: schedules.map(s => s.mstId),  // ✅ MstId만 저장
  savedAt: Date.now()
}

// 복원: selectedMstIds만 사용
const saved = JSON.parse(localStorage.getItem(key)!)
const idsToRestore = saved.selectedMstIds ?? []  // MstId만 복원
```

#### 2.1 마이그레이션 체크리스트 (상세)

**백엔드 - Phase 1 (필수 선행)** ✅ 총 8개 항목:
- [ ] `PassScheduleService.kt`: `mstIdCounter: AtomicLong(0)` 필드 추가
- [ ] `PassScheduleService.kt`: `initializeMstIdCounter()` 함수 구현
- [ ] `SatelliteTrackingProcessor.kt`: `startMstId` 파라미터 추가 및 `MstId` 생성 로직
- [ ] `SatelliteTrackingProcessor.kt`: DTL의 `No` → `Index` (0-based) 변경
- [ ] `PassScheduleService.kt`: `preparingPassId` 타입 `UInt` → `Long`
- [ ] `PassScheduleService.kt`: `cleanupTrackingEnd()` 파라미터 타입 `UInt` → `Long`
- [ ] 모든 `passId`, `mstId` 파라미터/반환값 타입 `UInt` → `Long`
- [ ] Map에서 추출하는 로컬 변수 타입 캐스팅 수정 (`as? UInt` → `as? Number)?.toLong()`)

**백엔드 - Phase 2** ✅ 총 4개 항목:
- [ ] `DataStoreService.kt`: `currentTrackingMstId`, `nextTrackingMstId` 타입 `UInt` → `Long`
- [ ] `DataStoreService.kt`: 관련 setter/getter 타입 변경
- [ ] `PassScheduleController.kt`: API 응답에서 `MstId` 필드 추가, `No` 필드 제거
- [ ] `PushDataService.kt`: WebSocket 전송 시 Long 타입 처리 확인

**프론트엔드 - Phase 3 (인터페이스/서비스)** ✅ 총 6개 항목:
- [ ] `passScheduleService.ts`: `PassScheduleMasterData` 인터페이스에 `MstId: number` 추가
- [ ] `passScheduleService.ts`: `TrackingTarget` 인터페이스에서 `no` 필드 제거
- [ ] `passScheduleStore.ts`: `ScheduleItem` 인터페이스에 `mstId: number` 필수 필드 추가
- [ ] `passScheduleStore.ts`: `fetchScheduleDataFromServer()`에서 `pass.MstId` → `scheduleItem.mstId` 매핑
- [ ] `passScheduleStore.ts`: `setTrackingTargets()`에서 `schedule.mstId` 직접 사용
- [ ] `passScheduleStore.ts`: `loadTrackingDetailData()`에서 `mstId` 파라미터 사용

**프론트엔드 - Phase 4 (localStorage/컴포넌트)** ✅ 총 10개 항목:
- [ ] `passScheduleStore.ts`: `saveSelectedScheduleNosToLocalStorage()` → `selectedMstIds`만 저장
- [ ] `passScheduleStore.ts`: `loadSelectedScheduleNosFromLocalStorage()` → `selectedMstIds`만 복원
- [ ] `SelectScheduleContent.vue`: `scheduleData` computed에서 `mstId: item.MstId` 매핑
- [ ] `SelectScheduleContent.vue`: `isScheduleSelected()`, `handleSelect()`에서 `mstId` 사용
- [ ] `SelectScheduleContent.vue`: localStorage 저장/복원 로직에서 `selectedMstIds` 사용
- [ ] `PassSchedulePage.vue`: `autoSelectedSchedule`에서 `s.mstId` 사용
- [ ] `PassSchedulePage.vue`: `currentScheduleStatus`에서 `schedule.mstId` 사용
- [ ] `PassSchedulePage.vue`: `loadSelectedScheduleTrackingPath()`에서 `scheduleToLoad.mstId` 사용
- [ ] `PassSchedulePage.vue`: `applyRowColors()`에서 `mstId` 기준 하이라이트
- [ ] `PassSchedulePage.vue`: 모든 `s.index === ...` 비교를 `s.mstId === ...`로 변경 (6-8곳)

**프론트엔드 - 추가 확인** ✅ 총 3개 항목:
- [ ] `icdStore.ts`: `currentTrackingMstId`, `nextTrackingMstId` 타입 확인 (number | null 유지)
- [ ] `ephemerisTrackStore.ts`: `selectSchedule()`에서 `schedule.mstId` 사용 확인
- [ ] 하이라이트 DOM 조작 시 `mstId` 컬럼 또는 `data-mstid` 속성 추가

**배포**:
- [ ] 개발 환경에서 전체 테스트 완료
- [ ] 스테이징 환경에서 검증 완료
- [ ] 프로덕션 배포 계획 수립
- [ ] 롤백 계획 수립
- [ ] 사용자 가이드 작성 (TLE 재업로드 필요 안내)

---

## 향후 확장 계획

### DetailId 확장 시나리오

**현재 구조**:
- DetailId는 위성 내 패스 인덱스 (0, 1, 2...)
- 각 패스마다 고유한 MstId를 가짐
- 예: 위성 A의 첫 번째 패스 (MstId: 1, DetailId: 0), 두 번째 패스 (MstId: 2, DetailId: 1)

**향후 확장 가능**:
같은 패스에 여러 DetailId를 가질 수 있도록 확장 가능:

```kotlin
// 현재 구조
MST: { mstId: 1, detailId: 0, ... }  // 위성 A의 첫 번째 패스
MST: { mstId: 2, detailId: 1, ... }  // 위성 A의 두 번째 패스

// 향후 확장 가능 (같은 패스에 여러 Detail)
MST: { mstId: 1, detailId: 0, ... }  // 원본 데이터
MST: { mstId: 1, detailId: 1, ... }  // 필터링된 데이터 (elevation > 10°)
MST: { mstId: 1, detailId: 2, ... }  // 최적화된 데이터

DTL: { mstId: 1, detailId: 0, index: 0, ... }  // 원본 포인트
DTL: { mstId: 1, detailId: 1, index: 0, ... }  // 필터링된 포인트
DTL: { mstId: 1, detailId: 2, index: 0, ... }  // 최적화된 포인트
```

**주의**: 현재는 각 패스마다 고유한 MstId를 가지므로, DetailId는 위성 내 패스 인덱스로 사용됩니다.

---

## 관련 파일

### 백엔드 (총 3개 파일)

#### 핵심 서비스 파일
1. **`ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`**
   - 전역 MstId 카운터 추가 (약 200라인)
   - `generateAllPassScheduleTrackingDataAsync()` 수정 (약 1489라인)
   - `generatePassScheduleTrackingDataAsync()` 수정 (약 1521라인)
   - 모든 조회/필터링 함수 수정 (약 1607, 1636, 1921, 1974, 1994, 2014라인)
   - 상태 머신 로직 수정 (약 420, 639, 674라인)
   - 추적 데이터 전송 로직 수정 (약 1184, 2267라인)
   - 데이터 초기화 로직 수정 (약 1767라인)

2. **`ACS_API/src/main/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/processor/SatelliteTrackingProcessor.kt`**
   - `processFullTransformation()` 수정 (약 280라인)
   - `structureOriginalData()` 수정 (약 367라인)
   - 모든 DataType 변환 함수 수정 (axis_transformed, final_transformed, keyhole_axis_transformed, keyhole_final_transformed)

3. **`ACS_API/src/main/kotlin/com/gtlsystems/acs_api/controller/mode/PassScheduleController.kt`**
   - `getAllTrackingMaster()` 응답 구조 수정 (약 1000라인)
   - `setTrackingTargets()` 요청/응답 구조 확인 (약 1184라인)
   - 모든 API 응답 매핑 수정

#### 관련 서비스 파일 (타입 변경 검토)
4. **`ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/common/DataStoreService.kt`**
   - `setCurrentTrackingMstId()` 타입 변경 (UInt → Long) 검토
   - `setNextTrackingMstId()` 타입 변경 (UInt → Long) 검토

### 프론트엔드 (총 4개 파일)

#### 핵심 스토어 파일
1. **`ACS/src/stores/mode/passScheduleStore.ts`**
   - `ScheduleItem` 인터페이스 수정 (약 15라인)
   - `fetchScheduleDataFromServer()` 데이터 매핑 수정 (약 200라인)
   - `setTrackingTargets()` 로직 수정 (약 1321라인)
   - `loadTrackingDetailData()` 데이터 매핑 수정 (약 600라인)
   - `saveSelectedScheduleNosToLocalStorage()` 수정 (약 1000라인)
   - `loadSelectedScheduleNosFromLocalStorage()` 수정 (약 1050라인)

2. **`ACS/src/services/mode/passScheduleService.ts`**
   - `PassScheduleMasterData` 인터페이스 수정 (약 100라인)
   - `TrackingDetailItem` 인터페이스 수정 (약 187라인)
   - `TrackingTarget` 인터페이스 확인 (약 154라인)

#### UI 컴포넌트 파일
3. **`ACS/src/components/content/SelectScheduleContent.vue`**
   - `scheduleData` computed 수정 (약 200라인)
   - `isScheduleSelected()` 함수 수정 (약 400라인)
   - `handleSelect()` 함수 수정 (약 986라인)
   - `onMounted()` 복원 로직 수정 (약 1200라인)
   - localStorage 저장 로직 수정 (약 1050라인)

4. **`ACS/src/pages/mode/PassSchedulePage.vue`**
   - `displaySchedule` computed 수정 (약 200라인)
   - 차트 표시 로직 수정 (약 500라인)
   - DOM 직접 조작 로직 수정 (약 816라인)
   - `getRowClass()` 함수 수정 (약 750라인)


### 영향 범위 요약

**백엔드**:
- 총 **4개 핵심 파일** 수정 (PassScheduleService, SatelliteTrackingProcessor, PassScheduleController, DataStoreService)
- 약 **25개 함수** 수정
- 약 **100개 라인** 직접 수정
- 약 **200개 라인** 영향 받음 (참조/호출)

**프론트엔드**:
- 총 **6개 핵심 파일** 수정 (passScheduleStore, passScheduleService, PassSchedulePage, SelectScheduleContent, icdStore, ephemerisTrackStore)
- 약 **25개 함수** 수정
- 약 **80개 라인** 직접 수정
- 약 **120개 라인** 영향 받음 (참조/호출)

**검증**:
- 수동 검증 체크리스트로 검증 (Phase 5 참조)

---

## 누락된 변경 사항 체크리스트

### 백엔드 추가 변경 사항

#### 1. 이벤트 구독 로직 수정
**파일**: `PassScheduleService.kt` (약 209라인)
- `setupEventSubscriptions()` 함수에서 `schedule["No"]` → `schedule["MstId"]` 변경
- 타입 캐스팅: `as? UInt` → `(as? Number)?.toLong()`

#### 2. `updateTrackingMstIdsAfterTargetSet()` 함수 수정
**파일**: `PassScheduleService.kt` (약 1797라인)
- `currentSchedule?.get("No")` → `currentSchedule?.get("MstId")` 변경
- `nextSchedule?.get("No")` → `nextSchedule?.get("MstId")` 변경
- 타입 캐스팅: `as? UInt` → `(as? Number)?.toLong()`

#### 3. `getCurrentSelectedTrackingPassWithTime()` 함수 수정
**파일**: `PassScheduleService.kt`
- 내부에서 `No` 필드 사용하는 부분이 있다면 `MstId`로 변경

#### 4. `getNextSelectedTrackingPassWithTime()` 함수 수정
**파일**: `PassScheduleService.kt`
- 내부에서 `No` 필드 사용하는 부분이 있다면 `MstId`로 변경

#### 5. `generateSelectedTrackingData()` 함수 수정
**파일**: `PassScheduleService.kt`
- `No` 필드 사용하는 부분이 있다면 `MstId`로 변경

#### 6. `getAllPassScheduleTrackMstMerged()` 함수 상세 수정
**파일**: `PassScheduleService.kt` (약 1636라인)
- `val mstId = final["No"] as UInt` → `val mstId = (final["MstId"] as? Number)?.toLong()`
- `it["No"] == mstId` → `(it["MstId"] as? Number)?.toLong() == mstId`
- 모든 `No` 필드 참조를 `MstId`로 변경

#### 7. `determineKeyholeDataType()` 함수 상세 수정
**파일**: `PassScheduleService.kt` (약 1921라인)
- `it["No"] == passId` → `(it["MstId"] as? Number)?.toLong() == passId`
- `it["No"] == passId && it["DataType"] == "final_transformed"` → `(it["MstId"] as? Number)?.toLong() == passId && it["DataType"] == "final_transformed"`

#### 8. `getTrackingPassMst()` 함수 상세 수정
**파일**: `PassScheduleService.kt` (약 1974라인)
- `it["No"] == passId` → `(it["MstId"] as? Number)?.toLong() == passId`

#### 9. `getSelectedTrackMstByMstId()` 함수 상세 수정
**파일**: `PassScheduleService.kt` (약 1994라인)
- `it["No"] == mstId` → `(it["MstId"] as? Number)?.toLong() == mstId`

#### 10. `getSelectedTrackDtlByMstId()` 함수 상세 수정
**파일**: `PassScheduleService.kt` (약 2014라인)
- `it["No"] == mstId` 참조가 있다면 `(it["MstId"] as? Number)?.toLong() == mstId`로 변경

#### 11. `getPassScheduleTrackDtlByMstId()` 함수 상세 수정
**파일**: `PassScheduleService.kt` (약 2047라인)
- `it["MstId"] == mstId` 필터링은 이미 `MstId` 사용 중 (유지)
- 파라미터 타입: `passId: UInt` → `Long`으로 변경

#### 12. API 응답에서 `No` 필드 완전 제거
**파일**: `PassScheduleController.kt`
- `getAllTrackingMaster()` 응답에서 `pass["No"]` 제거
- `addTleAndGenerateTracking()` 응답에서 `pass["No"]` 제거
- 모든 MST/DTL 응답 매핑에서 `No` 필드 제거

#### 13. `preparingPassId` 클래스 필드 타입 변경
**파일**: `PassScheduleService.kt` (약 119라인)
- `private var preparingPassId: UInt? = null` → `private var preparingPassId: Long? = null`

#### 14. `cleanupTrackingEnd()` 함수 파라미터 타입 변경
**파일**: `PassScheduleService.kt` (약 511라인)
- `fun cleanupTrackingEnd(mstId: UInt, ...)` → `fun cleanupTrackingEnd(mstId: Long, ...)`

#### 15. Map에서 추출하는 로컬 변수 타입 캐스팅 수정
**파일**: `PassScheduleService.kt`
- `executeStateAction()` 함수 (약 420라인):
  - `val currentMstId = currentSchedule["No"] as? UInt` → `val currentMstId = (currentSchedule["MstId"] as? Number)?.toLong()`
  - `val nextMstId = nextSchedule?.get("No") as? UInt` → `val nextMstId = (nextSchedule?.get("MstId") as? Number)?.toLong()`
- `updateTrackingMstIds()` 함수 (약 674라인):
  - `val currentMstId = currentSchedule?.get("No") as? UInt` → `val currentMstId = (currentSchedule?.get("MstId") as? Number)?.toLong()`
  - `val nextMstId = nextSchedule?.get("No") as? UInt` → `val nextMstId = (nextSchedule?.get("MstId") as? Number)?.toLong()`
- `handleTrackingStateChangeSeparately()` 함수 (약 639라인):
  - `val completedMstId = lastDisplayedSchedule!!["No"] as? UInt` → `val completedMstId = (lastDisplayedSchedule!!["MstId"] as? Number)?.toLong()`
  - `val currentMstId = currentSchedule["No"] as? UInt` → `val currentMstId = (currentSchedule["MstId"] as? Number)?.toLong()`

#### 16. `getCurrentSelectedTrackingPassWithTime()`, `getNextSelectedTrackingPassWithTime()` 함수 수정
**파일**: `PassScheduleService.kt`
- 반환하는 Map에서 `No` 필드 → `MstId` 필드로 변경
- 반환 타입의 `mstId` 필드 타입: `UInt` → `Long`
- 내부에서 `No` 필드 사용하는 부분이 있다면 모두 `MstId`로 변경

### 프론트엔드 추가 변경 사항

#### 1. `fetchScheduleDataFromServer()` 함수 상세 수정
**파일**: `passScheduleStore.ts` (약 1157라인)
- `no: pass.No` → `mstId: pass.MstId` (필수)
- `no: index + 1` (UI 표시용 재순번)
- `index: pass.MstId` (mstId와 연계)

#### 2. `setTrackingTargets()` 함수 상세 수정
**파일**: `passScheduleStore.ts` (약 1321라인)
- `const mstId = schedule.no` → `const mstId = schedule.mstId` (필수)
- `no: schedule.no` 필드 제거 (TrackingTarget 인터페이스에서도 제거)

#### 3. `loadTrackingDetailData()` 함수 상세 수정
**파일**: `passScheduleStore.ts` (약 1444라인)
- DTL 데이터 매핑 시 `MstId`, `DetailId`, `Index` 필드 사용
- `No` 필드 참조 제거

#### 4. `saveSelectedScheduleNosToLocalStorage()` 함수 수정
**파일**: `passScheduleStore.ts` (약 1009라인)
- `selectedNos` 저장 제거
- `selectedMstIds`만 저장

#### 5. `loadSelectedScheduleNosFromLocalStorage()` 함수 수정
**파일**: `passScheduleStore.ts` (약 1025라인)
- `selectedNos` 복원 제거
- `selectedMstIds`만 복원
- 함수명 변경 고려: `loadSelectedScheduleMstIdsFromLocalStorage()`

#### 6. `loadSelectedScheduleIndexesFromLocalStorage()` 함수 제거 또는 수정
**파일**: `passScheduleStore.ts` (약 1052라인)
- `selectedIndexes` 복원 제거
- `selectedMstIds`만 사용하도록 통합

#### 7. `PassSchedulePage.vue`의 `loadSelectedScheduleTrackingPath()` 함수 수정
**파일**: `PassSchedulePage.vue` (약 1336라인)
- `const passId = scheduleToLoad.index` → `const passId = scheduleToLoad.mstId` (필수)
- `Number(s.index) === Number(currentTrackingMstId)` → `Number(s.mstId) === Number(currentTrackingMstId)`

#### 8. `PassSchedulePage.vue`의 `autoSelectedSchedule` computed 수정
**파일**: `PassSchedulePage.vue` (약 1224라인)
- `Number(s.index) === Number(current)` → `Number(s.mstId) === Number(current)`
- `Number(s.index) === Number(next)` → `Number(s.mstId) === Number(next)`

#### 9. `PassSchedulePage.vue`의 `applyRowColors()` 함수 수정
**파일**: `PassSchedulePage.vue` (약 816라인)
- `const indexValue = indexCell?.textContent?.trim()` → `const mstIdValue = mstIdCell?.textContent?.trim()`
- `mstId` 컬럼 추가 필요 (또는 `mstId` 값을 다른 방식으로 추출)

#### 10. `SelectScheduleContent.vue`의 `handleSelect()` 함수 수정
**파일**: `SelectScheduleContent.vue` (약 986라인)
- `index를 no로 덮어쓰기` 로직 제거
- `mstId` 기준으로 선택 상태 관리

#### 11. `SelectScheduleContent.vue`의 `onMounted()` 복원 로직 수정
**파일**: `SelectScheduleContent.vue` (약 1208라인)
- `savedIndex` → `savedMstId`로 변경
- `s.index === savedIndex` → `s.mstId === savedMstId`

#### 21. `PassSchedulePage.vue`의 `currentScheduleStatus` computed 수정
**파일**: `PassSchedulePage.vue` (라인 1261-1295)
- 현재: `const scheduleIndex = Number(schedule.index)`
- 변경: `const scheduleIndex = Number(schedule.mstId)`
- `scheduleIndex === Number(current)` → `Number(schedule.mstId) === Number(current)`
- `scheduleIndex === Number(next)` → `Number(schedule.mstId) === Number(next)`

#### 22. `passScheduleStore.ts`의 `ScheduleItem` 인터페이스 수정
**파일**: `passScheduleStore.ts` (라인 15-69)
```typescript
// 현재
export interface ScheduleItem {
  no: number              // ← 필수
  index?: number          // ← 선택적

// 변경 후
export interface ScheduleItem {
  mstId: number           // ← 필수, 전역 고유 ID (백엔드 MstId)
  no?: number             // ← 선택적, UI 표시용 (1, 2, 3...)
  index?: number          // ← 선택적, mstId와 동일 값으로 설정 (호환성)
```

#### 23. `passScheduleService.ts`의 `PassScheduleMasterData` 인터페이스 수정
**파일**: `passScheduleService.ts` (라인 101-153)
```typescript
// 현재
export interface PassScheduleMasterData {
  No: number
  SatelliteID: string
  ...

// 변경 후 (백엔드 응답에 맞춤)
export interface PassScheduleMasterData {
  MstId: number           // ← 추가 (전역 고유 ID)
  // No 필드는 제거 또는 deprecated
  SatelliteID: string
  ...
```

#### 24. 모든 `sortedScheduleList` 비교 로직 수정
**파일**: `PassSchedulePage.vue` (여러 곳)
- 찾기: `sortedScheduleList.value.find(s => Number(s.index) === ...)`
- 변경: `sortedScheduleList.value.find(s => Number(s.mstId) === ...)`
- 영향 범위: 약 6-8곳

#### 25. 하이라이트 관련 DOM 조작 수정
**파일**: `PassSchedulePage.vue` (약 816라인 `applyRowColors` 함수)
- 현재: `index` 컬럼 값으로 행 식별
- 변경: `mstId` 컬럼 값으로 행 식별 (또는 data attribute 사용)
- 고려사항: 테이블에 `mstId` 컬럼 추가 또는 `data-mstid` 속성 추가

#### 12. `SelectScheduleContent.vue`의 localStorage 저장 로직 수정
**파일**: `SelectScheduleContent.vue` (약 1043라인)
- `selectedIndexes`, `selectedNos` 저장 제거
- `selectedMstIds`만 저장

#### 13. `ScheduleItem` 인터페이스에 `mstId` 필드 추가
**파일**: `passScheduleStore.ts` (약 15라인)
- 현재: `no: number`만 있음
- 추가: `mstId: number` (필수 필드)
- 변경: `no?: number` (선택 필드, UI 표시용)

#### 14. `PassSchedulePage.vue`에서 `schedule.index` 사용하는 모든 곳 수정
**파일**: `PassSchedulePage.vue`
- `loadSelectedScheduleTrackingPath()` 함수 (약 1364라인):
  - `const passId = scheduleToLoad.index` → `const passId = scheduleToLoad.mstId`
- `autoSelectedSchedule` computed (약 1232, 1241라인):
  - `Number(s.index) === Number(current)` → `Number(s.mstId) === Number(current)`
  - `Number(s.index) === Number(next)` → `Number(s.mstId) === Number(next)`
- `getRowStyleDirect()` 함수 (약 686라인):
  - `const tableIndex = schedule.index` → `const tableIndex = schedule.mstId`
- `updateChartOnScheduleSelect()` 함수 (약 2155라인):
  - `const passId = selectedSchedule.value.index || selectedSchedule.value.no` → `const passId = selectedSchedule.value.mstId`
- watch 로직 (약 528, 578라인):
  - `Number(s.index) === Number(newMstId)` → `Number(s.mstId) === Number(newMstId)`
- `predictedPathToShow` computed (약 1987라인):
  - `const schedulePassId = currentSchedule.index` → `const schedulePassId = currentSchedule.mstId`

#### 15. `SelectScheduleContent.vue`에서 `schedule.index` 사용하는 모든 곳 수정
**파일**: `SelectScheduleContent.vue`
- `scheduleData` computed (약 340라인):
  - `index: sortedIndex + 1` → `index: item.mstId`, `no: sortedIndex + 1`
- `handleSelect()` 함수 (약 1013라인):
  - `no: s.index || s.no` → `mstId: s.mstId` (no는 제거)
- `onMounted()` 복원 로직 (약 1242라인):
  - `s.index === savedIndex` → `s.mstId === savedMstId`

#### 16. `ephemerisTrackStore.ts`의 `selectSchedule()` 함수 수정
**파일**: `ephemerisTrackStore.ts` (약 617라인)
- `currentTrackingPassId.value = schedule.No` → `currentTrackingPassId.value = schedule.mstId`
- `await ephemerisTrackService.setCurrentTrackingPassId(schedule.No)` → `await ephemerisTrackService.setCurrentTrackingPassId(schedule.mstId)`
- `await ephemerisTrackService.fetchEphemerisDetailData(schedule.No)` → `await ephemerisTrackService.fetchEphemerisDetailData(schedule.mstId)`

#### 17. `currentTrackingPathInfo.passId` 필드 값 설정 수정
**파일**: `passScheduleStore.ts` (약 164라인)
- `currentTrackingPathInfo.passId` 값 설정 시 `mstId` 사용
- 타입은 `number | null` 유지 (값만 `mstId` 사용)

#### 18. `icdStore`의 WebSocket 메시지 처리 확인
**파일**: `icdStore.ts` (약 1515, 1528라인)
- `currentTrackingMstId`, `nextTrackingMstId`는 `number | null` 타입 유지
- 백엔드에서 Long으로 전송되지만 프론트엔드는 number로 처리 (JavaScript number 범위 내)

#### 19. `passScheduleService.ts`의 `getTrackingDetailByPass()` 함수 확인
**파일**: `passScheduleService.ts` (약 928라인)
- 파라미터 `passId: number` 타입 유지 (값만 `mstId` 사용)
- 주석: `@param passId 패스 ID (MST ID)` → `@param passId 패스 ID (MstId)`

#### 20. `TrackingDetailResponse` 인터페이스 확인
**파일**: `passScheduleService.ts` (약 194라인)
- `passId: number` 필드 유지 (값만 `mstId` 사용)

### 타입 변경 사항

#### 1. 모든 `UInt` → `Long` 변경
- `PassScheduleService.kt`의 모든 `passId`, `mstId` 파라미터
- `PassScheduleService.kt`의 `preparingPassId` 클래스 필드
- `PassScheduleService.kt`의 `cleanupTrackingEnd()` 함수 파라미터
- `TrackingDataCache`의 `passId` 필드
- `TrackingTarget`의 `mstId` 필드
- `DataStoreService`의 `currentTrackingMstId`, `nextTrackingMstId` 필드
- `PassScheduleController.kt`의 API 엔드포인트 파라미터
- Map에서 추출하는 모든 로컬 변수 (`currentMstId`, `nextMstId`, `completedMstId` 등)

#### 2. 프론트엔드 타입 변경
- `ScheduleItem` 인터페이스에 `mstId: number` 필드 추가 (필수)
- `ScheduleItem` 인터페이스에서 `no` 필드는 선택적 (UI 표시용)
- `ScheduleItem` 인터페이스에서 `index` 필드는 `mstId`와 동일 값으로 설정
- `TrackingTarget` 인터페이스에서 `no` 필드 제거
- `TrackingDetailResponse` 인터페이스의 `passId` 필드 유지 (값만 `mstId` 사용)
- `currentTrackingPathInfo.passId` 필드 유지 (값만 `mstId` 사용)
- `icdStore`의 `currentTrackingMstId`, `nextTrackingMstId` 타입 유지 (`number | null`)

---

## 참고 자료

- [C# 레거시 코드 분석](#c-레거시-코드-분석)
- [PassSchedule Workflow](../PassSchedule_Workflow/PassSchedule_Workflow.md)

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 2025-11-28 | 1.0 | 초안 작성 | GTL Systems |
| 2025-11-28 | 2.0 | 전체 파일 검토 완료, 상세 구현 계획 추가 | GTL Systems |
| 2025-11-28 | 2.1 | 검증 계획 및 마이그레이션 계획 상세화 | GTL Systems |
| 2025-11-28 | 2.2 | 실제 프로젝트 구조 반영, 프론트엔드 통합 테스트 시나리오 추가, 현재 코드 상태 분석 추가 | GTL Systems |
| 2025-11-28 | 2.3 | 테스트 코드 예시 제거, 수동 검증 체크리스트로 대체 | GTL Systems |
| 2025-11-28 | 2.4 | 하위 호환성 관련 내용 제거, 한 번에 변경하는 방식으로 수정, 전체 백엔드/프론트엔드 분석 결과 반영, 누락된 변경 사항 체크리스트 추가 | GTL Systems |
| 2025-11-28 | 2.5 | 전체 검수 완료, 백엔드 16개 항목, 프론트엔드 20개 항목 추가 누락 사항 반영 | GTL Systems |
| 2025-11-28 | 3.0 | **최종 전체 검수 완료** - 실제 코드와 문서 비교 분석, 추가 누락 사항 및 구현 상세 분석 반영 | GTL Systems |
| 2025-12-01 | 3.1 | **동시성 문제 해결 및 DB 연동 고려사항 추가** - MstId 생성 동시성 문제 해결, DB 연동 시나리오 문서화 | GTL Systems |

---

## ⚠️ 최종 전체 검수 결과 (v3.0)

### 실제 코드와 문서 비교 분석 결과

#### 백엔드 현재 코드 상태 (2025-11-28 검증)

**1. DataStoreService.kt** (`src/main/kotlin/.../service/datastore/DataStoreService.kt`)
```kotlin
// 현재 코드 (라인 187-210)
private val currentTrackingMstId = AtomicReference<UInt?>(null)  // ← UInt 타입
private val nextTrackingMstId = AtomicReference<UInt?>(null)     // ← UInt 타입

fun setCurrentTrackingMstId(mstId: UInt?) { ... }    // ← UInt 타입
fun setNextTrackingMstId(mstId: UInt?) { ... }       // ← UInt 타입
fun getCurrentTrackingMstId(): UInt? = ...           // ← UInt 타입
fun getNextTrackingMstId(): UInt? = ...              // ← UInt 타입
```
**✅ 상태**: 문서에 이미 반영됨 - UInt → Long 변경 필요

**2. PassScheduleService.kt** - 추가 확인 필요 항목
- `preparingPassId: UInt?` (라인 ~119)
- 모든 `passId`, `mstId` 파라미터들의 UInt 타입
- Map에서 추출하는 로컬 변수들 (`currentMstId`, `nextMstId` 등)

**3. PushDataService.kt** - 검토 필요
- WebSocket으로 `currentTrackingMstId`, `nextTrackingMstId` 전송 시 타입 확인
- 프론트엔드와의 타입 호환성 검증

---

#### 프론트엔드 현재 코드 상태 (2025-11-28 검증)

**1. passScheduleStore.ts** (`ACS/src/stores/mode/passScheduleStore.ts`)

**ScheduleItem 인터페이스 (라인 15-69)**:
```typescript
// 현재 코드
export interface ScheduleItem {
  no: number              // ← 필수 필드 (원본 백엔드 No)
  index?: number          // ← 선택적 필드 (표시용)
  // ⚠️ mstId 필드 없음 ← 추가 필요!
  ...
}
```
**⚠️ 누락**: `mstId: number` 필수 필드 추가 필요

**setTrackingTargets 함수 (라인 1321-1392)**:
```typescript
// 현재 코드
const trackingTargets: TrackingTarget[] = schedules.map((schedule, arrayIndex) => {
  const mstId = schedule.no  // ← no 사용 중
  return {
    mstId: Number(mstId),
    no: schedule.no,         // ← no 필드 전송
    ...
  }
})
```
**✅ 상태**: 현재 `no`를 `mstId`로 사용 중 - 리팩토링 후 `schedule.mstId` 직접 사용

**localStorage 함수들**:
```typescript
// 현재 코드 (라인 1009-1050)
const saveSelectedScheduleNosToLocalStorage = () => {
  const selectedNos = selectedScheduleList.value.map((s) => s.no)  // ← no만 저장
  // ⚠️ selectedMstIds 저장하지 않음!
}
```
**⚠️ 누락**: `selectedMstIds` 저장 로직 추가 필요

---

**2. passScheduleService.ts** (`ACS/src/services/mode/passScheduleService.ts`)

**TrackingTarget 인터페이스 (라인 154-162)**:
```typescript
// 현재 코드
export interface TrackingTarget {
  no: number           // ← 있음
  mstId: number        // ← 있음
  ...
}
```
**⚠️ 변경 필요**: `no` 필드 제거 (문서 명세)

**PassScheduleMasterData 인터페이스 (라인 101-153)**:
```typescript
// 현재 코드
export interface PassScheduleMasterData {
  No: number            // ← 있음
  // ⚠️ MstId 필드 없음! ← 백엔드 리팩토링 후 추가 필요
  SatelliteID: string
  SatelliteName: string
  ...
}
```
**⚠️ 누락**: `MstId: number` 필드 추가 필요

---

**3. SelectScheduleContent.vue** (`ACS/src/components/content/SelectScheduleContent.vue`)

**scheduleData computed (라인 310-346)**:
```typescript
// 현재 코드
const scheduleData = computed(() => {
  return sortedData.map((item, sortedIndex) => {
    return {
      ...item,
      // ⚠️ mstId 매핑 없음!
      index: sortedIndex + 1  // ← UI 표시용 index만 설정
    }
  })
})
```
**⚠️ 누락**: `mstId: item.MstId` 매핑 추가 필요

**localStorage 저장 (라인 357-391)**:
```typescript
// 현재 코드
const dataToSave = {
  selectedNos,        // ← no 저장
  selectedIndexes,    // ← index 저장
  // ⚠️ selectedMstIds 저장 안 함!
}
```
**⚠️ 누락**: `selectedMstIds` 저장 로직 추가 필요

---

**4. PassSchedulePage.vue** (`ACS/src/pages/mode/PassSchedulePage.vue`)

**autoSelectedSchedule computed (라인 1224-1253)**:
```typescript
// 현재 코드
const currentSchedule = schedules.find(s => Number(s.index) === Number(current))
```
**⚠️ 변경 필요**: `s.index` → `s.mstId`로 변경 필요

**loadSelectedScheduleTrackingPath 함수 (라인 1336-1410)**:
```typescript
// 현재 코드 (라인 1344)
const currentSchedule = sortedScheduleList.value.find(s => Number(s.index) === Number(currentTrackingMstId))

// 현재 코드 (라인 1364)
const passId = scheduleToLoad.index  // ← index 사용
```
**⚠️ 변경 필요**: 
- `s.index` → `s.mstId`
- `scheduleToLoad.index` → `scheduleToLoad.mstId`

---

### 추가 발견된 누락 변경 사항

#### 백엔드 추가 검토 항목

**21. PushDataService.kt WebSocket 전송 타입 확인**
**파일**: `PushDataService.kt`
- `currentTrackingMstId`, `nextTrackingMstId`를 WebSocket으로 전송할 때 타입 확인
- JSON 직렬화 시 Long 타입 처리 확인

**22. TrackingDataCache 클래스 구조 확인**
**파일**: 관련 캐시 클래스
- `passId` 필드 타입이 UInt인 경우 Long으로 변경
- 캐시 키 생성 로직 확인

**23. selectedTrackMstStorage 자료구조 확인**
**파일**: `PassScheduleService.kt`
- 저장되는 MST ID 타입 확인
- 조회 로직에서 타입 캐스팅 확인

---

#### 프론트엔드 추가 검토 항목

**21. PassScheduleMasterData 인터페이스 MstId 추가**
**파일**: `passScheduleService.ts` (라인 101)
- 현재: `No: number` 만 있음
- 추가: `MstId: number` 필드 추가 필요 (백엔드 응답에 맞춤)

**22. fetchScheduleDataFromServer 데이터 매핑 확인**
**파일**: `passScheduleStore.ts` (라인 1080)
- 현재: `no: pass.No` 매핑
- 추가: `mstId: pass.MstId` 매핑 필요

**23. currentScheduleStatus computed 수정**
**파일**: `PassSchedulePage.vue` (라인 1261-1295)
- 현재: `const scheduleIndex = Number(schedule.index)`
- 변경: `const scheduleIndex = Number(schedule.mstId)`

**24. 하이라이트 로직 전체 확인**
**파일**: `PassSchedulePage.vue`
- `applyRowColors` 함수에서 index 사용하는 모든 곳
- DOM 조작 시 `row.index` → `row.mstId` 변경

**25. sortedScheduleList에서 비교 로직 전체 수정**
**파일**: `PassSchedulePage.vue`
- 모든 `s.index === ...` 비교를 `s.mstId === ...`로 변경
- 약 6-8곳 존재

---

### 변경 영향도 요약 (최종)

| 항목 | 백엔드 | 프론트엔드 |
|------|--------|------------|
| 파일 수 | 4-5개 | 6-7개 |
| 함수/메서드 수 | ~30개 | ~30개 |
| 직접 수정 라인 | ~120라인 | ~100라인 |
| 영향 받는 라인 | ~250라인 | ~150라인 |
| 인터페이스/타입 변경 | 5-6개 | 4-5개 |

### 구현 우선순위

1. **백엔드 Phase 1** (필수 선행)
   - PassScheduleService.kt의 mstIdCounter 추가
   - SatelliteTrackingProcessor.kt의 MstId 생성 로직
   - 모든 UInt → Long 타입 변경

2. **백엔드 Phase 2**
   - PassScheduleController.kt API 응답 수정
   - DataStoreService.kt 타입 변경
   - PushDataService.kt WebSocket 전송 확인

3. **프론트엔드 Phase 3** (백엔드 완료 후)
   - passScheduleService.ts 인터페이스 수정
   - passScheduleStore.ts 데이터 매핑 및 localStorage 로직

4. **프론트엔드 Phase 4**
   - SelectScheduleContent.vue 수정
   - PassSchedulePage.vue 수정
   - icdStore.ts WebSocket 처리 확인

5. **통합 테스트 Phase 5**
   - 수동 검증 체크리스트 실행
   - E2E 시나리오 테스트

---

## 주의사항 및 리스크 관리

### 주요 리스크

1. **데이터 불일치 리스크**
   - **위험도**: 높음
   - **원인**: 기존 `No` 필드와 새로운 `MstId` 필드 혼용
   - **대응**: 하위 호환성 폴백 메커니즘 구현, 철저한 테스트

2. **동시성 문제 리스크**
   - **위험도**: 중간
   - **원인**: 전역 MstId 카운터의 동시 접근
   - **대응**: AtomicLong 사용, 동시성 테스트

3. **성능 저하 리스크**
   - **위험도**: 낮음
   - **원인**: 필드명 변경으로 인한 조회 로직 변경
   - **대응**: 성능 테스트, 필요 시 인덱스 추가

4. **하위 호환성 문제 리스크**
   - **위험도**: 중간
   - **원인**: 기존 클라이언트가 `No` 필드에 의존
   - **대응**: 점진적 마이그레이션, Deprecated 필드 유지

### 롤백 계획

1. **백엔드 롤백**
   - Git revert로 코드 롤백
   - 기존 `No` 필드 기반 로직 복원
   - 데이터베이스 변경 없음 (메모리 저장소만 사용)

2. **프론트엔드 롤백**
   - Git revert로 코드 롤백
   - localStorage 데이터는 자동 호환 (폴백 메커니즘)

3. **데이터 롤백**
   - TLE 재업로드로 데이터 재생성
   - 기존 데이터는 자동 삭제됨 (TLE 업로드 시)

### 체크리스트

**개발 전**:
- [ ] 전체 파일 검토 완료
- [ ] 영향 범위 파악 완료
- [ ] 테스트 계획 수립 완료
- [ ] 롤백 계획 수립 완료

**개발 중**:
- [ ] 백엔드 변경 사항 단계별 커밋
- [ ] 프론트엔드 변경 사항 단계별 커밋
- [ ] 각 Phase별 테스트 완료
- [ ] 코드 리뷰 완료

**배포 전**:
- [ ] 전체 테스트 통과
- [ ] 성능 테스트 통과
- [ ] 하위 호환성 테스트 통과
- [ ] 문서 업데이트 완료

**배포 후**:
- [ ] 모니터링 설정
- [ ] 사용자 피드백 수집
- [ ] 이슈 트래킹

---

## 🔧 동시성 문제 해결 (v3.1)

### 발견된 문제

**문제**: `generateAllPassScheduleTrackingDataAsync`에서 `flatMap`을 사용하여 병렬 처리 시, 여러 위성이 동시에 같은 `startMstId`를 읽어 MstId 중복이 발생합니다.

**원인**:
```kotlin
// 문제 코드
val startMstId = mstIdCounter.get()  // ⚠️ 여러 위성이 동시에 같은 값을 읽을 수 있음
generatePassScheduleTrackingDataAsync(...)  // 비동기 처리 (시간이 걸림)
    .map { (mstData, dtlData) ->
        mstIdCounter.addAndGet(passCount.toLong())  // ⚠️ 나중에 증가
    }
```

**시나리오**:
- 위성 A가 `startMstId = 0`을 읽음
- 위성 B도 동시에 `startMstId = 0`을 읽음
- 위성 A가 패스 3개 생성 → MstId: 1, 2, 3
- 위성 B도 패스 3개 생성 → MstId: 1, 2, 3 (중복!)

### 해결 방법

**방법**: `getAndAdd`를 사용하여 원자적으로 범위를 할당하고, schedule을 재사용하여 중복 계산을 방지합니다.

**구현**:
```kotlin
// ✅ 패스 개수만 먼저 계산 (빠른 계산)
val schedule = orekitCalculator.generateSatelliteTrackingSchedule(...)
val passCount = schedule.trackingPasses.size

// ✅ 원자적으로 범위 할당 (동시성 문제 해결)
val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1

// ✅ 계산된 schedule을 재사용하여 실제 데이터 생성 (중복 계산 없음)
generatePassScheduleTrackingDataAsyncWithSchedule(..., schedule)
```

**장점**:
- ✅ 병렬 처리 유지 (빠름)
- ✅ 동시성 문제 해결 (원자적 연산)
- ✅ 중복 계산 없음 (schedule 재사용)

**파일**: `PassScheduleService.kt` (라인 1618-1757)
- `generateAllPassScheduleTrackingDataAsync` 함수 수정
- `generatePassScheduleTrackingDataAsyncWithSchedule` 함수 추가

---

## 🗄️ DB 연동 고려사항 (v3.1)

### 현재 구조 (RAM 기반)

**MstId 생성**:
```kotlin
private val mstIdCounter = AtomicLong(0)

// 패스 개수만큼 원자적으로 범위 할당
val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1
```

### DB 연동 시 전략

#### 옵션 1: DB 시퀀스 사용 (권장)

**PostgreSQL 예시**:
```kotlin
@Entity
@Table(name = "pass_schedule_mst")
data class PassScheduleMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mst_id_seq")
    @SequenceGenerator(
        name = "mst_id_seq",
        sequenceName = "mst_id_sequence",
        allocationSize = 1  // ✅ 1씩 증가
    )
    val mstId: Long,  // ✅ 전역 고유 ID (DB Primary Key)
    
    @Column(nullable = false)
    val detailId: Int = 0,  // ✅ Detail 구분자
    
    // ... 나머지 필드
)
```

**장점**:
- ✅ DB 레벨에서 고유성 보장
- ✅ 동시성 문제 자동 해결
- ✅ 서버 재시작 시에도 연속성 유지

**단점**:
- ⚠️ DB 의존성 증가
- ⚠️ 성능 오버헤드 (시퀀스 조회)

#### 옵션 2: 애플리케이션 레벨 관리 (현재 방식 유지)

**구조**:
```kotlin
// RAM에서 생성
private val mstIdCounter = AtomicLong(0)
val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1

// DB 저장 시 기존 MstId 유지
@Entity
@Table(name = "pass_schedule_mst")
data class PassScheduleMaster(
    @Id
    val mstId: Long,  // ✅ RAM에서 생성한 값 그대로 사용
    
    @Column(nullable = false)
    val detailId: Int = 0,
    
    // ... 나머지 필드
)
```

**장점**:
- ✅ DB 의존성 없음 (빠름)
- ✅ 현재 구조와 호환
- ✅ 서버 재시작 시 카운터만 초기화

**단점**:
- ⚠️ 서버 재시작 시 MstId가 1부터 다시 시작
- ⚠️ 여러 서버 인스턴스 시 동기화 필요

#### 옵션 3: 하이브리드 방식

**구조**:
```kotlin
// RAM에서 생성 (빠른 처리)
private val mstIdCounter = AtomicLong(0)
val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1

// DB 저장 시 DB 시퀀스로 재생성 (선택적)
// 또는 기존 MstId 유지
```

**권장**: 옵션 2 (현재 방식 유지)
- PassSchedule 데이터는 TLE 기반으로 생성되므로 재생성이 자연스러움
- DB는 데이터 저장소로만 사용
- 서버 재시작 시 TLE 재업로드로 데이터 재생성

### DB 저장 구조

#### MST 테이블
```sql
CREATE TABLE pass_schedule_mst (
    mst_id BIGSERIAL PRIMARY KEY,  -- ✅ 전역 고유 ID
    detail_id INTEGER NOT NULL DEFAULT 0,  -- ✅ Detail 구분자
    satellite_id VARCHAR(50) NOT NULL,  -- ✅ 카탈로그 번호
    satellite_name VARCHAR(100),  -- ✅ 위성 이름
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    -- ... 나머지 필드
    UNIQUE(mst_id, detail_id)  -- ✅ 복합 유니크 제약
);
```

#### DTL 테이블
```sql
CREATE TABLE pass_schedule_dtl (
    mst_id BIGINT NOT NULL,  -- ✅ FK → MST.mstId
    detail_id INTEGER NOT NULL,  -- ✅ FK → MST.detailId
    index INTEGER NOT NULL,  -- ✅ 100ms 포인트 순번
    time TIMESTAMP WITH TIME ZONE,
    azimuth DOUBLE PRECISION,
    elevation DOUBLE PRECISION,
    -- ... 나머지 필드
    PRIMARY KEY (mst_id, detail_id, index),  -- ✅ 복합키
    FOREIGN KEY (mst_id, detail_id) REFERENCES pass_schedule_mst(mst_id, detail_id)
);
```

### 마이그레이션 전략

**RAM → DB 전환 시**:

1. **기존 MstId 유지** (권장)
   - RAM에서 생성한 MstId를 그대로 DB에 저장
   - 서버 재시작 시 카운터 초기화는 문제 없음 (TLE 재업로드)

2. **DB 시퀀스로 재생성**
   - DB 저장 시 DB 시퀀스로 새로 생성
   - 기존 MstId 무시
   - 프론트엔드 localStorage와 불일치 가능

3. **하이브리드**
   - RAM에서는 AtomicLong 사용
   - DB 저장 시 기존 MstId 유지 또는 DB 시퀀스 사용 선택

**권장**: 옵션 1 (기존 MstId 유지)
- 프론트엔드 localStorage와 호환
- 데이터 일관성 유지

### 동시성 고려사항

**현재 해결 방법**:
- `AtomicLong.getAndAdd()` 사용으로 원자적 범위 할당
- 병렬 처리 시에도 MstId 중복 방지

**DB 연동 시**:
- DB 시퀀스 사용 시 자동으로 동시성 문제 해결
- 애플리케이션 레벨 관리 시 현재 방식 유지

---

## 📋 DetailId 확장 계획 명확화 (v3.1)

### 현재 구조

**DetailId 설정**:
```kotlin
val detailId = index  // 위성 내 패스 인덱스 (0, 1, 2...)
```

**의도**:
- MstId: 전역 고유 ID (1, 2, 3, 4, 5...)
- DetailId: 위성 내 패스 인덱스 (0, 1, 2...)
- 각 패스마다 고유한 MstId를 가지므로, DetailId는 위성 내에서만 고유

**현재 동작**:
- 같은 위성의 여러 패스는 각각 다른 MstId를 가짐
- DetailId는 위성 내 패스 인덱스로 증가 (0, 1, 2...)

### 향후 확장 시나리오

**현재**: DetailId는 위성 내 패스 인덱스
**향후**: 같은 MstId에 여러 DetailId를 가질 수 있도록 확장 가능

**예시**:
```kotlin
// 현재 구조
MST: { mstId: 1, detailId: 0, ... }  // 위성 A의 첫 번째 패스
MST: { mstId: 2, detailId: 1, ... }  // 위성 A의 두 번째 패스

// 향후 확장 가능 (같은 패스에 여러 Detail)
MST: { mstId: 1, detailId: 0, ... }  // 원본 데이터
MST: { mstId: 1, detailId: 1, ... }  // 필터링된 데이터 (elevation > 10°)
MST: { mstId: 1, detailId: 2, ... }  // 최적화된 데이터
```

**현재는 각 패스마다 고유한 MstId를 가지므로 DetailId는 위성 내 패스 인덱스로 사용**

---
