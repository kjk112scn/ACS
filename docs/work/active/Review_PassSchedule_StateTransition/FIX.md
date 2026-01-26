# PassSchedule 상태 전이 + 하이라이트 버그 수정

<!-- @task-system: enabled -->

## 2026-01-26: Time Offset 변경 시 하이라이트 미갱신 (#R004)

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 Critical |
| **상태** | ✅ 수정 완료 |
| **Origin** | #R004 (Review_PassSchedule_StateTransition) |
| **수정일** | 2026-01-26 |

---

## 증상

| 현상 | 예상 동작 | 실제 동작 |
|------|----------|----------|
| 파란색 (next) | 다음 스케줄 표시 | ✅ 동작 |
| 녹색 (current) | 추적 중 스케줄 표시 | ❌ 안 됨 |
| Time Offset 변경 | TRACKING으로 전환 | ❌ 녹색 미표시 |
| 다음 스케줄 이동 | 파란색 다음으로 이동 | ❌ 안 됨 |

---

## 원인 (Root Cause)

### #R004-C1: handleTimeOffsetChange() 미호출

**위치**: `PassScheduleService.kt:683-704`

```kotlin
fun passScheduleTimeOffsetCommand(inputTimeOffset: Float) {
    Mono.fromCallable {
        GlobalData.Offset.TimeOffset = inputTimeOffset
        udpFwICDService.writeNTPCommand()
        // ❌ handleTimeOffsetChange() 호출 누락!
        dataStoreService.getCurrentTrackingMstId()?.let { mstId ->
            sendInitialTrackingData(mstId)
        }
        udpFwICDService.timeOffsetCommand(inputTimeOffset)
    }.subscribeOn(Schedulers.boundedElastic()).subscribe(...)
}
```

**영향**: Time Offset 변경 시 스케줄 큐 재평가 안 됨 → current/next MstId 미갱신

### #R004-C2: validTransitions 시간 점프 불허

**위치**: `PassScheduleService.kt:231-242`

```kotlin
private val validTransitions = mapOf(
    PassScheduleState.STOWING to setOf(PassScheduleState.STOWED),           // TRACKING 불가
    PassScheduleState.STOWED to setOf(PassScheduleState.MOVING_TRAIN),      // TRACKING 불가
    // ... 모든 준비 상태에서 TRACKING 직접 전환 불가
)
```

**영향**: 시간 점프 시 TRACKING 전환 거부 → ERROR 상태로 빠짐

---

## 수정 계획

### Task 1: #R004-C1 수정 - handleTimeOffsetChange() 호출 추가

**파일**: `PassScheduleService.kt:683-704`

```diff
fun passScheduleTimeOffsetCommand(inputTimeOffset: Float) {
    Mono.fromCallable {
        GlobalData.Offset.TimeOffset = inputTimeOffset
        udpFwICDService.writeNTPCommand()
+
+       // ✅ FIX #R004-C1: Time Offset 변경 시 스케줄 큐 재평가 및 mstId 갱신
+       handleTimeOffsetChange()
+
        dataStoreService.getCurrentTrackingMstId()?.let { mstId ->
            sendInitialTrackingData(mstId)
        }
        udpFwICDService.timeOffsetCommand(inputTimeOffset)
    }.subscribeOn(Schedulers.boundedElastic()).subscribe(...)
}
```

**Why**:
- Time Offset 변경 시 시간이 변경되므로 스케줄 큐 재평가 필요
- `handleTimeOffsetChange()`는 큐 재평가 + mstId 업데이트를 수행

---

### Task 2: #R004-C2 수정 - validTransitions 시간 점프 허용

**파일**: `PassScheduleService.kt:231-242`

```diff
private val validTransitions = mapOf(
    PassScheduleState.IDLE to setOf(PassScheduleState.STOWING, PassScheduleState.MOVING_TRAIN),
-   PassScheduleState.STOWING to setOf(PassScheduleState.STOWED),
+   PassScheduleState.STOWING to setOf(PassScheduleState.STOWED, PassScheduleState.TRACKING),
-   PassScheduleState.STOWED to setOf(PassScheduleState.MOVING_TRAIN),
+   PassScheduleState.STOWED to setOf(PassScheduleState.MOVING_TRAIN, PassScheduleState.TRACKING),
-   PassScheduleState.MOVING_TRAIN to setOf(PassScheduleState.TRAIN_STABILIZING),
+   PassScheduleState.MOVING_TRAIN to setOf(PassScheduleState.TRAIN_STABILIZING, PassScheduleState.TRACKING),
-   PassScheduleState.TRAIN_STABILIZING to setOf(PassScheduleState.MOVING_TO_START),
+   PassScheduleState.TRAIN_STABILIZING to setOf(PassScheduleState.MOVING_TO_START, PassScheduleState.TRACKING),
-   PassScheduleState.MOVING_TO_START to setOf(PassScheduleState.READY),
+   PassScheduleState.MOVING_TO_START to setOf(PassScheduleState.READY, PassScheduleState.TRACKING),
    PassScheduleState.READY to setOf(PassScheduleState.TRACKING),
    // ... 나머지
)
```

**Why**:
- 시간 점프(Time Offset 변경)로 추적 시간에 도달하면 TRACKING으로 직접 전환 필요
- 기존 순차 전환만 허용하면 ERROR 상태로 빠짐

---

## 테스트 계획

### 수정 확인
- [ ] Time Offset 변경 시 녹색 (current) 하이라이트 표시
- [ ] Time Offset 변경 시 파란색 (next) 다음 스케줄로 이동
- [ ] STOWING 상태에서 시간 점프 시 TRACKING 전환 성공
- [ ] ERROR 상태로 빠지지 않음

### 회귀 테스트
- [ ] 정상 흐름 (IDLE → STOWING → ... → TRACKING) 동작
- [x] 빌드 성공 ✅

---

## 수정 파일 요약

| 파일 | 변경 | Task |
|------|------|------|
| `PassScheduleService.kt:683-704` | handleTimeOffsetChange() 호출 추가 | Task 1 |
| `PassScheduleService.kt:231-242` | validTransitions에 TRACKING 추가 | Task 2 |
| `PassScheduleService.kt:3256-3278` | nextScheduleContext 업데이트 추가 | Task 3 |
| `PassScheduleService.kt:3227-3240` | DataStore mstId/detailId 업데이트 추가 | Task 3 |

---

## 2026-01-26: mstId/detailId 업데이트 누락 (#R004-C3)

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 Critical |
| **상태** | ✅ 수정 완료 |
| **Origin** | #R004-C3 |

### 증상

- Start 누를 때는 하이라이트 정상
- Time Offset 변경 시 하이라이트 안 바뀜

### 원인

```
handleTimeOffsetChange()
    ↓
reevaluateScheduleQueue()
    ↓ currentScheduleContext 업데이트 ✅
    ↓ nextScheduleContext 미업데이트 ❌
    ↓
dataStoreService.setCurrentTrackingMstId(mstId, detailId) 미호출 ❌
dataStoreService.setNextTrackingMstId(mstId, detailId) 미호출 ❌
    ↓
WebSocket: 이전 mstId/detailId 그대로 전송
    ↓
FE: 하이라이트 안 바뀜
```

### 변경 내용

#### 1. reevaluateScheduleQueue()에 nextScheduleContext 업데이트 추가

```kotlin
// ✅ FIX #R004-C3: 다음 스케줄 컨텍스트도 재설정
val nextSchedule = activeSchedules
    .filter { it.startTime.isAfter(currentSchedule?.endTime ?: calTime) }
    .minByOrNull { it.startTime }

if (nextSchedule != null && nextSchedule.mstId != nextScheduleContext?.mstId) {
    logger.info("[V2-TIME_OFFSET] 다음 스케줄 변경: ${nextScheduleContext?.satelliteName} → ${nextSchedule.satelliteName}")
    nextScheduleContext = nextSchedule
} else if (nextSchedule == null && nextScheduleContext != null) {
    logger.info("[V2-TIME_OFFSET] 다음 스케줄 없음 (이전: ${nextScheduleContext?.satelliteName})")
    nextScheduleContext = null
}
```

#### 2. handleTimeOffsetChange()에 DataStore mstId/detailId 업데이트 추가

```kotlin
// ✅ FIX #R004-C3: DataStore에 mstId/detailId 업데이트 (WebSocket 전송용)
currentScheduleContext?.let { ctx ->
    dataStoreService.setCurrentTrackingMstId(ctx.mstId, ctx.detailId)
} ?: dataStoreService.setCurrentTrackingMstId(null, null)

nextScheduleContext?.let { next ->
    dataStoreService.setNextTrackingMstId(next.mstId, next.detailId)
} ?: dataStoreService.setNextTrackingMstId(null, null)

logger.info("[V2-TIME_OFFSET] mstId/detailId 업데이트 완료: current={}/{}, next={}/{}",
    currentScheduleContext?.mstId, currentScheduleContext?.detailId,
    nextScheduleContext?.mstId, nextScheduleContext?.detailId)
```

### 테스트 계획

- [ ] Time Offset 변경 시 녹색 (current) 하이라이트 표시
- [ ] Time Offset 변경 시 파란색 (next) 다음 스케줄로 이동
- [x] BE 빌드 성공 ✅

---

## 2026-01-26: 전문가 종합 검토 (#R005)

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 Critical |
| **상태** | ✅ 수정 완료 |
| **Origin** | #R005 (종합 검토) |
| **수정일** | 2026-01-26 |

### 배경

#R004 수정 후에도 Time Offset 변경 시 하이라이트 미갱신 문제 지속.
BE/FE 병렬 전문가 분석으로 추가 문제점 발견.

### #R005-C1: IDLE 상태에서 early return

**위치**: `PassScheduleService.kt:3214-3232`

**문제**:
```kotlin
fun handleTimeOffsetChange() {
    if (currentPassScheduleState == PassScheduleState.IDLE) {
        logger.info("[V2-TIME_OFFSET] IDLE 상태에서는 스케줄 큐 재평가 건너뜀")
        return  // ❌ 아무것도 안 함!
    }
    // ...
}
```

**영향**: IDLE 상태에서 Time Offset 변경 시 currentScheduleContext/nextScheduleContext 업데이트 안 됨

**수정**:
```kotlin
fun handleTimeOffsetChange() {
    val calTime = GlobalData.Time.calUtcTimeOffsetTime

    // ✅ FIX #R005-C1: IDLE 상태에서도 스케줄 큐 재평가 및 DataStore 업데이트
    if (currentPassScheduleState == PassScheduleState.IDLE) {
        logger.info("[V2-TIME_OFFSET] IDLE 상태에서 Time Offset 변경 - DataStore만 업데이트")
        reevaluateScheduleQueue(calTime)

        // DataStore 업데이트 (FE WebSocket 전송용)
        currentScheduleContext?.let { ctx ->
            dataStoreService.setCurrentTrackingMstId(ctx.mstId, ctx.detailId)
        } ?: dataStoreService.setCurrentTrackingMstId(null, null)

        nextScheduleContext?.let { next ->
            dataStoreService.setNextTrackingMstId(next.mstId, next.detailId)
        } ?: dataStoreService.setNextTrackingMstId(null, null)

        return  // 상태 전환은 하지 않음
    }
    // ...
}
```

---

### #R005-C2: detailId 비교 누락

**위치**: `PassScheduleService.kt:3269, 3285`

**문제**:
```kotlin
// reevaluateScheduleQueue()
if (currentSchedule.mstId != currentScheduleContext?.mstId) {  // ❌ mstId만 비교
    currentScheduleContext = currentSchedule
}

if (nextSchedule.mstId != nextScheduleContext?.mstId) {  // ❌ mstId만 비교
    nextScheduleContext = nextSchedule
}
```

**영향**: 같은 위성의 다른 패스(같은 mstId, 다른 detailId) → 변경 감지 안 됨

**수정**:
```kotlin
// ✅ FIX #R005-C2: mstId와 detailId 모두 비교
if (currentSchedule != null &&
    (currentSchedule.mstId != currentScheduleContext?.mstId ||
     currentSchedule.detailId != currentScheduleContext?.detailId)) {
    logger.info("[V2-TIME_OFFSET] 현재 스케줄 변경: ... → ${currentSchedule.satelliteName}(${currentSchedule.detailId})")
    currentScheduleContext = currentSchedule
}

if (nextSchedule != null &&
    (nextSchedule.mstId != nextScheduleContext?.mstId ||
     nextSchedule.detailId != nextScheduleContext?.detailId)) {
    logger.info("[V2-TIME_OFFSET] 다음 스케줄 변경: ... → ${nextSchedule.satelliteName}(${nextSchedule.detailId})")
    nextScheduleContext = nextSchedule
}
```

---

### 수정 파일 요약

| 파일 | 변경 | Issue |
|------|------|-------|
| `PassScheduleService.kt:3214-3232` | IDLE 상태 처리 추가 | #R005-C1 |
| `PassScheduleService.kt:3269` | current 비교에 detailId 추가 | #R005-C2 |
| `PassScheduleService.kt:3285` | next 비교에 detailId 추가 | #R005-C2 |

### 테스트 계획

- [ ] IDLE 상태에서 Time Offset 변경 시 하이라이트 업데이트
- [ ] 같은 위성의 다른 패스로 이동 시 하이라이트 업데이트
- [x] BE 빌드 성공 ✅

---

## 2026-01-26: FE 잘못된 API 호출 (#R005-C4)

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 Critical |
| **상태** | ✅ 수정 완료 |
| **Origin** | #R005-C4 |
| **수정일** | 2026-01-26 |

### 증상

- BE 로그: `[V2-TIME_OFFSET] Time Offset 변경 감지!` 출력 안 됨
- 실제 로그: `ICDController - TimeOffset 명령 요청 완료` (잘못된 Controller)

### 원인

```
FE → /api/icd/time-offset-command → ICDController (handleTimeOffsetChange ❌)
     └─ 기대: /api/pass-schedule/time-offset-command → PassScheduleController (handleTimeOffsetChange ✅)

useOffsetControls.ts가 모든 모드에서 icdStore.sendTimeOffsetCommand() 사용
  → ICDController 호출
  → handleTimeOffsetChange() 미호출
  → 스케줄 큐 재평가 안 됨
  → 하이라이트 안 바뀜
```

### 수정 내용

#### 1. passScheduleStore.ts - 전용 API 호출

```diff
const sendTimeOffset = async (timeOffset: number) => {
  try {
-   return await useICDStore().sendTimeOffsetCommand(timeOffset)
+   // ✅ FIX #R005-C4: PassSchedule 전용 API 호출 (handleTimeOffsetChange 포함)
+   return await passScheduleService.sendTimeOffsetCommand(timeOffset)
  } catch (err) {
    error.value = 'Failed to send time offset'
    throw err
  }
}
```

#### 2. useOffsetControls.ts - 모드별 API 분기

```typescript
// ✅ FIX #R005-C4: PassSchedule 모드 여부 확인
const isPassScheduleMode = computed(() => {
  return route.path.includes('pass-schedule')
})

// ✅ FIX #R005-C4: 모드별 Time Offset 명령 전송
const sendTimeOffsetByMode = async (timeOffset: number) => {
  if (isPassScheduleMode.value) {
    // PassSchedule 모드: 전용 API (handleTimeOffsetChange 포함)
    return await passScheduleStore.sendTimeOffset(timeOffset)
  } else {
    // 그 외 모드: 기존 ICD API
    return await icdStore.sendTimeOffsetCommand(timeOffset)
  }
}
```

### 수정 파일 요약

| 파일 | 변경 | Issue |
|------|------|-------|
| `passScheduleStore.ts:2104-2111` | passScheduleService 직접 호출 | #R005-C4 |
| `useOffsetControls.ts:17-30` | 모드별 API 분기 로직 추가 | #R005-C4 |
| `useOffsetControls.ts:103,166,211` | sendTimeOffsetByMode 사용 | #R005-C4 |

### 테스트 계획

- [ ] Start 후 Time Offset 변경 시 BE 로그 `[V2-TIME_OFFSET]` 출력 확인
- [ ] Time Offset 변경 시 녹색 (current) 하이라이트 업데이트
- [ ] Time Offset 변경 시 파란색 (next) 다음 스케줄 이동
- [x] FE 빌드 성공 ✅

---

## 관련 문서

- [phases/01_review.md](phases/01_review.md) - 원인 분석
- [#R002 수정](../Bugfix_PassSchedule_Highlight_MstId_Mismatch/FIX.md) - MstId fallback 수정 (완료)
