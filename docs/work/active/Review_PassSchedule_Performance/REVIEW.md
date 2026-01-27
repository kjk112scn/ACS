# Review: PassScheduleService 성능 분석 (#R003)

> **Review ID**: #R003
> **대상**: PassScheduleService.kt 성능 및 데이터 흐름
> **분석일**: 2026-01-27
> **비교 대상**: EphemerisService.kt (동일 패턴 검토)

---

## 분석 요약

PassScheduleService는 **EphemerisService와 다른 아키텍처**를 사용하고 있어, 동일한 성능 이슈가 없습니다.

### 핵심 차이점

| 항목 | EphemerisService | PassScheduleService |
|------|------------------|---------------------|
| **데이터 조회** | 매 100ms DB 쿼리 | **메모리 캐시** (passScheduleTrackDtlStorage) |
| **실시간 데이터** | 100ms 타이머에서 직접 생성 | **이벤트 기반** (하드웨어 요청 시만) |
| **배치 저장** | BatchStorageManager 사용 | **미사용** (이미 메모리에 저장) |
| **캐시 전략** | 미캐싱 → 캐싱 추가 | **초기부터 캐시 구조** |

---

## 발견된 이슈

| Issue ID | 심각도 | 문제 | 위치 | 상태 |
|----------|:------:|------|------|:----:|
| #R003-M1 | 🟡 Medium | 스케줄 시작 시 DTL 조회 | `PassScheduleService.kt:3556` | 📋 검토 |
| #R003-L1 | 🟢 Low | getSelectedTrackingSchedule 반복 호출 | `PassScheduleService.kt:2421` | 📋 참고 |
| #R003-L2 | 🟢 Low | 과도한 디버깅 로그 (주석 처리됨) | `PassScheduleService.kt:2428` | ✅ 해결됨 |

---

## 상세 분석

### ✅ 긍정적 설계 (EphemerisService 대비)

#### 1. 메모리 캐시 기반 구조

```kotlin
// PassScheduleService.kt:254-257
private val passScheduleTrackMstStorage: ConcurrentHashMap<String, List<Map<String, Any?>>>
    get() = ConcurrentHashMap(passScheduleDataRepository.getAllMst())
private val passScheduleTrackDtlStorage: ConcurrentHashMap<String, List<Map<String, Any?>>>
    get() = ConcurrentHashMap(passScheduleDataRepository.getAllDtl())
```

**장점:**
- 데이터가 **서비스 시작 시 로드**되어 메모리에 유지
- 100ms 타이머에서 DB 쿼리 없음
- ConcurrentHashMap으로 **스레드 안전**

#### 2. 이벤트 기반 데이터 전송

```kotlin
// PassScheduleService.kt:3140-3147
PassScheduleState.TRACKING -> {
    val ctx = currentScheduleContext ?: return
    val calTimeEpoch = calTime.toInstant().toEpochMilli()
    // 추적 데이터는 기존 메서드를 통해 전송됨 (이벤트 기반)
}
```

**장점:**
- 100ms 타이머에서 **데이터 생성 안 함**
- 하드웨어 요청(`SatelliteTrackDataRequested`) 시에만 데이터 전송
- CPU 부하 최소화

#### 3. 추적 데이터 캐시 시스템

```kotlin
// PassScheduleService.kt:272-299
private val trackingDataCache = ConcurrentHashMap<Long, TrackingDataCache>()

data class TrackingDataCache(
    val passId: Long,
    val trackingPoints: Array<TrackingPoint>,  // ✅ Array로 O(1) 접근
    val totalSize: Int,
    val createdAt: Long = System.currentTimeMillis()
)
```

**장점:**
- 자주 사용하는 패스 데이터를 **Array로 변환** (고속 접근)
- 캐시 만료 시스템 (1시간)
- 캐시 miss 시에만 메모리 저장소 조회

---

### 🟡 #R003-M1: 스케줄 시작 시 DTL 조회 (Medium)

**위치**: `PassScheduleService.kt:3556`

```kotlin
// buildScheduleContextsFromSelected() 함수
val trackingDetails = getSelectedTrackDtlByMstId(mstId)
val firstPoint = trackingDetails.firstOrNull()
```

**문제:**
- 스케줄 컨텍스트 생성 시 **모든 DTL 데이터 조회** 후 첫 번째 포인트만 사용
- 스케줄 개수가 많을 경우 초기 로드 시간 증가

**개선안:**
```kotlin
// 필요한 첫 번째 포인트만 조회하는 함수 추가
fun getFirstTrackingPointByMstId(mstId: Long): Map<String, Any?>? {
    val selectedMst = getSelectedTrackMstByMstId(mstId) ?: return null
    val satelliteId = selectedMst["SatelliteID"] as? String ?: return null
    val dataType = determineKeyholeDataType(mstId, selectedTrackMstStorage) ?: return null

    return passScheduleTrackDtlStorage[satelliteId]?.firstOrNull {
        (it["MstId"] as? Number)?.toLong() == mstId && it["DataType"] == dataType
    }
}
```

**영향도:** 낮음 (스케줄 시작 시 1회만 호출)
**권장:** 개선 불필요, 참고용

---

### 🟢 #R003-L1: getSelectedTrackingSchedule 반복 호출 (Low)

**위치**: `PassScheduleService.kt:2421`

```kotlin
fun getSelectedTrackingSchedule(): List<Map<String, Any?>> {
    val allSelectedPasses = mutableListOf<Map<String, Any?>>()
    selectedTrackMstStorage.values.forEach { mstDataList ->
        allSelectedPasses.addAll(mstDataList)
    }
    return allSelectedPasses  // 매번 새 리스트 생성
}
```

**현재 상태:**
- 디버깅 로그 주석 처리됨 (100ms 호출 문제 인지)
- 메모리 기반이라 성능 영향 미미

**개선안 (선택):**
```kotlin
// 캐싱 추가
private var cachedSelectedSchedules: List<Map<String, Any?>>? = null
private var selectedSchedulesVersion = 0

fun getSelectedTrackingSchedule(): List<Map<String, Any?>> {
    return cachedSelectedSchedules ?: run {
        selectedTrackMstStorage.values.flatten().also {
            cachedSelectedSchedules = it
        }
    }
}
```

**권장:** 현재 수준 유지 (성능 영향 미미)

---

## EphemerisService와 비교 결론

| 항목 | EphemerisService | PassScheduleService |
|------|:----------------:|:-------------------:|
| 100ms 타이머 DB 쿼리 | ❌ 문제 | ✅ 없음 |
| 배열 무제한 증가 | ❌ 문제 | ✅ 고정 크기 |
| 캐시 시스템 | ⚠️ 수동 추가 필요 | ✅ 내장 |
| 배치 저장 | ⚠️ 설정 문제 | ✅ 해당 없음 |

---

## 권장 조치

**즉시 수행 필요: 없음**

PassScheduleService는 이미 **최적화된 구조**로 설계되어 있습니다.

참고로 EphemerisService에서 발견된 문제들:
1. ✅ `createRealtimeTrackingData()` 내 DB 쿼리 → **PassSchedule은 메모리 캐시 사용**
2. ✅ 배치 저장 100ms 간격 → **PassSchedule은 배치 저장 미사용**
3. ✅ trackingPath 무제한 증가 → **PassSchedule은 고정 데이터 사용**

---

## 추가 분석 필요 시

프론트엔드 성능 리뷰가 필요하다면:
```
/review frontend/src/pages/mode/PassSchedulePage.vue
```

