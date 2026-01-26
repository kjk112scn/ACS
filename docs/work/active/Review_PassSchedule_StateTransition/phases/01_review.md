# PassSchedule 상태 전이 + 하이라이트 심층 리뷰

**Review ID**: #R004
**분석일**: 2026-01-26
**심각도**: Critical

---

## 1. 증상

| 현상 | 예상 동작 | 실제 동작 |
|------|----------|----------|
| 파란색 (next) | 다음 스케줄 표시 | ✅ 동작 |
| 녹색 (current) | 추적 중 스케줄 표시 | ❌ 안 됨 |
| Time Offset 변경 | TRACKING으로 전환 | ❌ 녹색 미표시 |
| 다음 스케줄 이동 | 파란색 다음으로 이동 | ❌ 안 됨 |

---

## 2. 근본 원인 분석

### 문제 1: Time Offset 변경 시 handleTimeOffsetChange() 미호출

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

**영향**:
```
[FE] Time Offset 변경 (+300초 등)
      ↓
[BE] GlobalData.Offset.TimeOffset 설정 ✅
[BE] handleTimeOffsetChange() 호출 안 됨 ❌
      ↓
[BE] 스케줄 큐 재평가 없음, mstId 미갱신
      ↓
[FE] WebSocket으로 수신하는 mstId 그대로
      ↓
[FE] 하이라이트 색상 그대로 (파란색 유지)
```

### 문제 2: validTransitions 엄격한 순차 전환만 허용

**위치**: `PassScheduleService.kt:231-242`

```kotlin
private val validTransitions = mapOf(
    PassScheduleState.STOWING to setOf(PassScheduleState.STOWED),           // TRACKING 불가
    PassScheduleState.STOWED to setOf(PassScheduleState.MOVING_TRAIN),      // TRACKING 불가
    PassScheduleState.MOVING_TRAIN to setOf(PassScheduleState.TRAIN_STABILIZING),  // TRACKING 불가
    PassScheduleState.TRAIN_STABILIZING to setOf(PassScheduleState.MOVING_TO_START),
    PassScheduleState.MOVING_TO_START to setOf(PassScheduleState.READY),
    PassScheduleState.READY to setOf(PassScheduleState.TRACKING),  // 여기서만 TRACKING 가능
)
```

**영향**:
```
시나리오: STOWING 상태에서 Time Offset으로 추적 시간 도달

1. 현재 상태: STOWING
2. determineStateByTime() → TRACKING 반환
3. transitionTo() 검사: validTransitions[STOWING] = {STOWED}
4. TRACKING ∉ {STOWED} → ❌ 전환 거부
5. ERROR 상태로 전환
6. current/next MstId 미갱신
```

---

## 3. 발견된 이슈

| ID | 심각도 | 문제 | 위치 | 상태 |
|----|:------:|------|------|:----:|
| #R004-C1 | 🔴 Critical | `handleTimeOffsetChange()` 미호출 | PassScheduleService.kt:683-704 | ⏳ |
| #R004-C2 | 🔴 Critical | validTransitions 시간 점프 불허 | PassScheduleService.kt:231-242 | ⏳ |
| #R004-H1 | 🟠 High | 잘못된 전환 시 ERROR 상태 | PassScheduleService.kt:2960-2966 | ⏳ |
| #R004-M1 | 🟡 Medium | `isAfter(startTime)` 시작 시간 미포함 | PassScheduleService.kt:2770-2780 | ⏳ |

---

## 4. 수정 방안

### #R004-C1: handleTimeOffsetChange() 호출 추가

```diff
fun passScheduleTimeOffsetCommand(inputTimeOffset: Float) {
    Mono.fromCallable {
        GlobalData.Offset.TimeOffset = inputTimeOffset
        udpFwICDService.writeNTPCommand()
+
+       // ✅ FIX: Time Offset 변경 시 스케줄 큐 재평가 및 mstId 갱신
+       handleTimeOffsetChange()
+
        dataStoreService.getCurrentTrackingMstId()?.let { mstId ->
            sendInitialTrackingData(mstId)
        }
        udpFwICDService.timeOffsetCommand(inputTimeOffset)
    }.subscribeOn(Schedulers.boundedElastic()).subscribe(...)
}
```

### #R004-C2: validTransitions 시간 점프 허용

**Option A**: 모든 준비 상태에서 TRACKING 직접 전환 허용

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
    // ...
)
```

**Option B**: transitionTo()에 시간 기반 예외 로직 추가

```kotlin
private fun transitionTo(newState: PassScheduleState) {
    val currentState = currentPassScheduleState
    val allowed = validTransitions[currentState] ?: emptySet()

    // ✅ 시간 기반 TRACKING 전환은 항상 허용
    val isTimeBasedTracking = newState == PassScheduleState.TRACKING &&
        currentState in setOf(STOWING, STOWED, MOVING_TRAIN, TRAIN_STABILIZING, MOVING_TO_START, READY)

    if (newState !in allowed && !isTimeBasedTracking) {
        logger.error("[V2-INVALID] $currentState → $newState 잘못된 전환")
        transitionToError("Invalid transition: $currentState → $newState")
        return
    }
    // ... 전환 진행
}
```

**권장**: Option A (명시적, 유지보수 용이)

---

## 5. 데이터 흐름 (수정 후)

```
[FE] Time Offset 변경
      ↓
[BE] passScheduleTimeOffsetCommand()
      ↓
[BE] GlobalData.Offset.TimeOffset = newOffset
[BE] handleTimeOffsetChange() ✅ 추가
      ↓
[BE] reevaluateScheduleQueue() → 시간 기반 재평가
[BE] updateTrackingMstIds() → current/next 업데이트
      ↓
[BE] transitionTo(TRACKING) ✅ validTransitions에 허용됨
[BE] sendStateToFrontend() → currentMstId, nextMstId 설정
      ↓
[FE] WebSocket 수신: currentTrackingMstId=4, nextTrackingMstId=5
      ↓
[FE] highlightedRows 업데이트
[FE] ScheduleTable 녹색/파란색 표시 ✅
```

---

## 6. 테스트 계획

### 수정 확인
- [ ] Time Offset 변경 시 녹색 (current) 하이라이트 표시
- [ ] Time Offset 변경 시 파란색 (next) 다음 스케줄로 이동
- [ ] STOWING 상태에서 시간 점프 시 TRACKING 전환 성공
- [ ] ERROR 상태로 빠지지 않음

### 회귀 테스트
- [ ] 정상 흐름 (IDLE → STOWING → ... → TRACKING) 동작
- [ ] 빌드 성공

---

## 7. 관련 리뷰

| Review ID | 주제 | 연관성 |
|-----------|------|--------|
| #R001 | 상태머신 이슈 | 상태 전이 원자성 |
| #R002 | MstId 데이터 흐름 | mstId fallback 문제 (수정 완료) |
| #R004 | 상태 전이 + 하이라이트 | 본 리뷰 |
