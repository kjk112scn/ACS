# PassSchedule 상태 머신 리팩토링

> **작성일**: 2026-01-08 → **수정**: 2026-01-19
> **상태**: 진행 중 - 전문가 검토 완료

---

## 1. 현황 요약

### 1.1 완료된 작업

| 작업 | 상태 |
|------|:----:|
| V1.0 코드 완전 제거 | ✅ |
| FE passScheduleTrackingStateInfo 추가 | ✅ |
| L2745 하드코딩 → Settings 연동 | ✅ |
| FE 상태 동기화 (11개 상태) | ✅ |

### 1.2 V2.0 상태 열거형 (현재 구현)

```kotlin
enum class PassScheduleState {
    IDLE, STOWING, STOWED,
    MOVING_TRAIN, TRAIN_STABILIZING, MOVING_TO_START, READY,
    TRACKING, POST_TRACKING, COMPLETED, ERROR
}
```

---

## 2. 발견된 버그 (2026-01-19 전문가 종합 검토)

### 2.1 Critical (P0 - 즉시 수정)

| # | 문제 | 위치 | 영향 |
|---|------|------|------|
| C1 | **STOWING→STOWED 위치 확인 없음** | determineStateByTime() L2751-2755 | Stow 이동 중인데 STOWED로 잘못 판정 |
| C2 | **sendStateToFrontend()에서 detailId 미전달** | L3001-3005 | FE 테이블 하이라이트 매칭 실패 |
| C3 | **resetFlags() 결과가 큐에 미반영** | evaluateNextSchedule() L2827 | 다중 위성 추적 시 플래그 초기화 실패 |

### 2.2 High (P1 - 조기 수정)

| # | 문제 | 위치 | 영향 |
|---|------|------|------|
| H1 | executeExitAction 미구현 | 전체 | 상태 퇴장 시 정리 로직 누락 |
| H2 | IDLE 상태 명시적 처리 없음 | executeEnterAction() L2880-2965 | 리소스 정리 누락 |
| H3 | reevaluateScheduleQueue 큐 미업데이트 | L3042-3058 | Time Offset 변경 시 플래그 불일치 |
| H4 | nextScheduleContext 정보 FE 미전달 | sendStateToFrontend() | 다음 스케줄 하이라이트 불가 |
| H5 | **상태별 타임아웃 누락** | STOWING, MOVING_* | 무한 대기 가능 (데드락 위험) |
| H6 | **ANY→ERROR 명시적 전환 누락** | 전체 | 오류 상태 진입 경로 불명확 |

### 2.3 Medium

| # | 문제 | 위치 |
|---|------|------|
| M1 | updateProgressFlags 컨텍스트 직접 수정 | L2682-2708 | data class 불변성 위반, 동시성 위험 |
| M2 | startStateMachineTracking 스레드 안전성 | L3164-3181 | 동시 호출 시 레이스 컨디션 |
| M3 | isShuttingDown 동기화 없음 | L192 | AtomicBoolean 필요 |

### 2.4 Low

| # | 문제 | 위치 |
|---|------|------|
| L1 | 10초마다 INFO 로깅 | L2648-2650 | 장시간 운용 시 로그 과다 |

---

## 3. 코드 품질 이슈 (CLAUDE.md 위반)

> **검증일**: 2026-01-19 (실제 코드 grep 기준)

### 3.1 catch(Exception) - 17개

| 클러스터 | 라인 | 개수 |
|----------|------|:----:|
| 데이터 처리 | 570, 598, 630, 719 | 4 |
| 추적 명령 | 837, 878, 883, 893, 899 | 5 |
| 통신/ICD | 1489, 1807, 2603 | 3 |
| Stow 동작 | 3103, 3107, 3196, 3231, 3281 | 5 |

**권장**: IOException, TimeoutException, IllegalStateException 등 구체적 예외로 분리

### 3.2 V1 잔재 - "No" 필드 3개

| 라인 | 코드 |
|------|------|
| 1342, 1357, 1377 | `schedule["No"] as? UInt` |

**권장**: `schedule["MstId"] as? Number)?.toLong()` 으로 변경

### 3.3 Dead Code

| 라인 | 코드 | 이유 |
|------|------|------|
| 2615-2618 | `handleTrackingPreparation()` | @Deprecated, 미사용 |
| 356-361 | `trackingMonitorThreadFactory` | ThreadManager 사용으로 불필요 |

---

## 4. 수정 계획 (전문가 검토 반영)

### Phase A: Critical 수정 (P0 - 즉시)

**A1. STOWING→STOWED 위치 확인 추가**

```kotlin
// determineStateByTime() L2751-2755 수정
else -> {
    if (isAtStowPosition()) {
        PassScheduleState.STOWED
    } else {
        PassScheduleState.STOWING
    }
}

// 새 함수 추가 (4차 전문가 검토 반영: Train 축, 허용오차 1.0, Double 타입)
private fun isAtStowPosition(): Boolean {
    val latestData = dataStoreService.getLatestData()
    val currentAz = latestData.azimuthAngle ?: return false
    val currentEl = latestData.elevationAngle ?: return false
    val currentTrain = latestData.trainAngle ?: return false

    val stowAz = settingsService.stowAngleAzimuth  // Double 유지
    val stowEl = settingsService.stowAngleElevation
    val stowTrain = settingsService.stowAngleTrain

    val tolerance = 1.0  // 안테나 정밀도 고려 (Double)

    return kotlin.math.abs(currentAz - stowAz) <= tolerance &&
           kotlin.math.abs(currentEl - stowEl) <= tolerance &&
           kotlin.math.abs(currentTrain - stowTrain) <= tolerance
}
```

**A2. sendStateToFrontend() detailId + nextSchedule 전달**

```kotlin
// L3001-3005 수정
private fun sendStateToFrontend(state: PassScheduleState, ctx: ScheduleTrackingContext?) {
    PushData.TRACKING_STATUS.passScheduleTrackingState = state.name

    if (ctx != null) {
        dataStoreService.setCurrentTrackingMstId(ctx.mstId, ctx.detailId)  // detailId 추가

        nextScheduleContext?.let { next ->
            dataStoreService.setNextTrackingMstId(next.mstId, next.detailId)
        } ?: dataStoreService.setNextTrackingMstId(null, null)
    } else {
        dataStoreService.clearTrackingMstIds()
    }
}
```

**A3. resetFlags() 큐 원본 업데이트**

```kotlin
// evaluateNextSchedule() L2827 수정
val currentIdx = scheduleContextQueue.indexOf(nextSchedule)
val resetContext = nextSchedule.resetFlags()

if (currentIdx >= 0) {
    scheduleContextQueue[currentIdx] = resetContext  // 큐 원본도 업데이트
}
currentScheduleContext = resetContext
```

### Phase B: High 수정 (P1)

**B1. executeExitAction 구현**

```kotlin
private fun executeExitAction(state: PassScheduleState, ctx: ScheduleTrackingContext?, calTime: ZonedDateTime) {
    when (state) {
        PassScheduleState.TRACKING -> {
            logger.info("[V2-EXIT] 추적 종료")
        }
        else -> {}
    }
}

// transitionTo()에서 호출
executeExitAction(currentPassScheduleState, ctx, calTime)  // 퇴장 액션
currentPassScheduleState = newState
executeEnterAction(newState, ctx, calTime)  // 진입 액션
```

**B2. IDLE 상태 명시적 처리**

```kotlin
PassScheduleState.IDLE -> {
    logger.info("[V2-ACTION] IDLE 상태 진입 - 리소스 정리")
    dataStoreService.setPassScheduleTracking(false)
    trackingDataCache.clear()
}
```

**B3. reevaluateScheduleQueue 큐 업데이트**

```kotlin
// L3042-3058 수정
val idx = scheduleContextQueue.indexOf(currentSchedule)
val resetContext = currentSchedule.resetFlags()
if (idx >= 0) {
    scheduleContextQueue[idx] = resetContext
}
currentScheduleContext = resetContext
```

**B4. ERROR 전환 함수 + 상태 전환 유효성 검사**

```kotlin
// 유효한 상태 전환 맵 (순서 위반 시 ERROR)
private val validTransitions = mapOf(
    PassScheduleState.IDLE to setOf(STOWING, MOVING_TRAIN),
    PassScheduleState.STOWING to setOf(STOWED),
    PassScheduleState.STOWED to setOf(MOVING_TRAIN),
    PassScheduleState.MOVING_TRAIN to setOf(TRAIN_STABILIZING),
    PassScheduleState.TRAIN_STABILIZING to setOf(MOVING_TO_START),
    PassScheduleState.MOVING_TO_START to setOf(READY),
    PassScheduleState.READY to setOf(TRACKING),
    PassScheduleState.TRACKING to setOf(POST_TRACKING),
    PassScheduleState.POST_TRACKING to setOf(STOWING, MOVING_TRAIN, COMPLETED),
    PassScheduleState.COMPLETED to setOf(IDLE),
    PassScheduleState.ERROR to setOf(IDLE)  // ERROR에서는 IDLE로만 복귀
)

// 오류 상태로 전환 (절차 위반, 타임아웃 등)
private fun transitionToError(reason: String) {
    logger.error("[V2-ERROR] {} → ERROR: {}", currentPassScheduleState, reason)
    currentPassScheduleState = PassScheduleState.ERROR
    sendStateToFrontend(PassScheduleState.ERROR, currentScheduleContext)
    // 현재 스케줄 스킵하고 다음으로
    evaluateNextSchedule(ZonedDateTime.now())
}

// transitionTo에서 유효성 검사 추가
private fun transitionTo(newState: PassScheduleState, ctx: ScheduleTrackingContext?, calTime: ZonedDateTime) {
    // ERROR로 가는 건 항상 허용
    if (newState == PassScheduleState.ERROR) {
        executeExitAction(currentPassScheduleState, ctx, calTime)
        currentPassScheduleState = newState
        executeEnterAction(newState, ctx, calTime)
        return
    }

    // 상태 전환 유효성 검사
    val allowed = validTransitions[currentPassScheduleState] ?: emptySet()
    if (newState !in allowed) {
        logger.error("[V2-INVALID] {} → {} 잘못된 전환", currentPassScheduleState, newState)
        transitionToError("Invalid transition: $currentPassScheduleState → $newState")
        return
    }

    // 정상 전환 진행
    executeExitAction(currentPassScheduleState, ctx, calTime)
    currentPassScheduleState = newState
    executeEnterAction(newState, ctx, calTime)
    sendStateToFrontend(newState, ctx)
}
```

**ERROR 진입 조건:**

| 조건 | 설명 |
|------|------|
| 타임아웃 | STOWING 4분, MOVING_TRAIN 2분, MOVING_TO_START 4분 초과 |
| 잘못된 전환 | 상태 순서 위반 (예: IDLE → TRACKING 직접 시도) |

**B5. 상태별 타임아웃 추가**

```kotlin
// 타임아웃 상수 (협의 결과: Train 2분, 나머지 4분)
private const val STOWING_TIMEOUT_MS = 240_000L         // 4분 (Az/El/Train 모두 이동)
private const val MOVING_TRAIN_TIMEOUT_MS = 120_000L    // 2분 (Train 축만 이동)
private const val MOVING_TO_START_TIMEOUT_MS = 240_000L // 4분 (Az/El 이동)

// executePeriodicAction()에서 타임아웃 체크
private fun checkStateTimeout(ctx: ScheduleTrackingContext?, calTime: ZonedDateTime): Boolean {
    val entryTime = ctx?.stateEntryTime ?: return false
    val elapsed = Duration.between(entryTime, calTime).toMillis()

    val timeout = when (currentPassScheduleState) {
        PassScheduleState.STOWING -> STOWING_TIMEOUT_MS
        PassScheduleState.MOVING_TRAIN -> MOVING_TRAIN_TIMEOUT_MS
        PassScheduleState.MOVING_TO_START -> MOVING_TO_START_TIMEOUT_MS
        else -> Long.MAX_VALUE
    }

    if (elapsed > timeout) {
        logger.error("[V2-TIMEOUT] {} 상태에서 {}ms 타임아웃", currentPassScheduleState, elapsed)
        transitionToError("State timeout: ${currentPassScheduleState}")
        return true
    }
    return false
}
```

### Phase C: Medium 수정

**C1. isShuttingDown → AtomicBoolean**

```kotlin
private val isShuttingDown = AtomicBoolean(false)

// 사용 시
if (isShuttingDown.get()) return
isShuttingDown.set(true)
```

### Phase D: 코드 품질 개선

- [ ] catch(Exception) 17개 → 구체적 예외 타입 (IOException, IllegalStateException 등)
- [ ] "No" 필드 3개 → "MstId" 변경
- [ ] @Deprecated handleTrackingPreparation() 삭제
- [ ] 로그 레벨 조정 (10초 INFO → DEBUG)

---

## 5. 상태 전환 다이어그램

```
START ─► IDLE ─┬─(>prepMinutes)─► STOWING ─► STOWED ─┐
               │                                      │
               └─(≤prepMinutes)──────────────────────►┤
                                                      ▼
                                                MOVING_TRAIN
                                                      │
                                                (Train 도달)
                                                      ▼
                                               TRAIN_STABILIZING
                                                      │
                                                 (3초 경과)
                                                      ▼
                                               MOVING_TO_START
                                                      │
                                                (Az/El 도달)
                                                      ▼
                                                    READY
                                                      │
                                                (시작 시간)
                                                      ▼
                                                  TRACKING
                                                      │
                                                (종료 시간)
                                                      ▼
                                               POST_TRACKING
                                                      │
                ┌─────────────────────────────────────┼───────────────┐
                │                                     │               │
          (다음 >prepMinutes)               (다음 ≤prepMinutes)  (다음 없음)
                ▼                                     ▼               ▼
             STOWING                            MOVING_TRAIN     COMPLETED
```

---

## 6. 관련 파일

### Backend
- `PassScheduleService.kt` (3,300줄)
- `PassScheduleController.kt`

### Frontend
- `icdStore.ts` - passScheduleTrackingStateInfo (L2464-2498)
- `PassSchedulePage.vue`
- `ScheduleInfoPanel.vue`

---

## 7. 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-01-08 | 초안 작성 |
| 2026-01-19 | L2745 수정, 전문가 검토 결과 반영 |
| 2026-01-19 | 수치 검증 (코드 grep 기준), 문서 통합 |
| 2026-01-19 | 2차 전문가 검토: H5-H6(타임아웃/ERROR전환), A1 코드 수정 |
| 2026-01-19 | 3차 협의: C4(Standby) 제거, 타임아웃 4분/2분/4분 확정 |
| 2026-01-19 | B4 확장: 상태 전환 유효성 검사 (validTransitions) 추가 |
| 2026-01-19 | 4차 검토: A1 Float→Double, T8/T9 테스트 시나리오 추가 |