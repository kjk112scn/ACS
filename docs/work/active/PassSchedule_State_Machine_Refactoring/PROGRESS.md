# PassSchedule 상태머신 리팩토링 진행 현황

> **최종 수정:** 2026-01-19
> **상태:** 🔴 In Progress - Phase 1 준비 중

---

## 개요

| 항목 | 내용 |
|------|------|
| **목표** | PassSchedule 시작 함수 통합, 시퀀스 반복 정상화, BE-FE 연계 수정 |
| **시작일** | 2026-01-08 |
| **관련 문서** | [ANALYSIS.md](./ANALYSIS.md), [DESIGN.md](./DESIGN.md) |

---

## 진행 상황 요약

```
Phase 1: 시작 동작 복구     [ ] 0% ━━━━━━━━━━ 대기
Phase 2: 시간 기준 수정     [ ] 0% ━━━━━━━━━━ 대기
Phase 3: FE 연계 수정       [ ] 0% ━━━━━━━━━━ 대기
Phase 4: 안정성 개선        [ ] 0% ━━━━━━━━━━ 대기
Phase 5: 문서 동기화        [ ] 0% ━━━━━━━━━━ 대기
```

---

## 발견된 문제 목록 (2026-01-19 전문가 검토)

### Critical (즉시 수정 필요)

| # | 문제 | 위치 | 상태 | 비고 |
|---|------|------|:----:|------|
| **C1** | 시작 함수에서 스케줄 큐 빌드 누락 | `startScheduleTracking()` L369 | ⬜ | 타이머만 시작, 큐 비어있음 |
| **C2** | 2분 하드코딩 (Settings 4분 무시) | `determineStateByTime()` L2745 | ⬜ | `minutesToStart <= 2` |
| **C3** | FE에 detailId 미전달 | `sendStateToFrontend()` L2995 | ⬜ | mstId만 전달 |

### High (시퀀스 무결성 - 2026-01-19 심층 분석)

| # | 문제 | 위치 | 상태 | 비고 |
|---|------|------|:----:|------|
| **H1** | Train 이동 타임아웃 없음 | `MOVING_TRAIN` 상태 | ⬜ | 명령 실패 시 무한 대기 |
| **H2** | Az/El 이동 타임아웃 없음 | `MOVING_TO_START` 상태 | ⬜ | 명령 실패 시 무한 대기 |
| **H3** | ERROR 시 다음 위성 스킵 불가 | `handleErrorRecovery()` | ⬜ | 30초 실패 시 전체 중단 |
| **H4** | Time Jump 시 플래그 불일치 | `TRACKING` 진입 | ⬜ | 물리적 위치와 소프트웨어 플래그 불일치 |

### Medium (개선 필요)

| # | 문제 | 위치 | 상태 | 비고 |
|---|------|------|:----:|------|
| **M1** | nextTrackingMstId 미설정 | `sendStateToFrontend()` | ⬜ | "다음 예정" 표시 안됨 |
| **M2** | STOWING → STOWED 위치 확인 없음 | `determineStateByTime()` | ⬜ | 시간만 기준, 실제 도달 미확인 |
| **M3** | 시간 정밀도 (분 단위) | `determineStateByTime()` L2742 | ⬜ | 최대 59초 오차 |
| **M4** | DOM 직접 조작 타이밍 이슈 | `applyRowColors()` FE | ⬜ | Vue 반응성 우회 |
| **M5** | 전체 큐 길이/완료 수 FE 미전달 | `sendStateToFrontend()` | ⬜ | 진행률 표시 불가 |

---

## 스케줄 전환 검증 결과 (2026-01-19)

### 시나리오별 테스트

| 시나리오 | 조건 | 예상 동작 | 실제 동작 | 결과 |
|---------|------|----------|----------|:----:|
| 즉시 전환 | 위성 B 시작까지 ≤4분 | MOVING_TRAIN | ✅ 정상 | OK |
| Stow 후 대기 | 위성 B 시작까지 >4분 | STOWING | ✅ 정상 | OK |
| 마지막 완료 | 다음 스케줄 없음 | COMPLETED | ✅ 정상 | OK |
| 플래그 리셋 | 새 스케줄 시작 | `resetFlags()` | ✅ 정상 | OK |

### 상태 시퀀스 흐름도

```
START ─► IDLE ─┬─(>4분)─► STOWING ─► STOWED ─┐
               │                              │
               └─(≤4분)──────────────────────►┤
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
            ┌─────────────────────────────────┼───────────────┐
            │                                 │               │
      (다음 >4분)                       (다음 ≤4분)      (다음 없음)
            ▼                                 ▼               ▼
         STOWING                        MOVING_TRAIN     COMPLETED
            │                                 │
            └─────────── (반복) ──────────────┘
```

**결론:** 기본 시퀀스 흐름은 정상. 예외 처리(타임아웃, 에러 복구)만 보완 필요.

---

## Phase 1: 시작 동작 복구

### 목표
`startScheduleTracking()` 호출 시 정상적으로 상태머신 시작

### 작업 항목

- [ ] **1.1** `startScheduleTracking()` 수정
  - [ ] `buildScheduleQueue(calTime)` 호출 추가
  - [ ] `scheduleContextQueue.addAll()` 추가
  - [ ] 빈 큐 검증 및 early return
  - [ ] `currentScheduleContext = first()` 설정
  - [ ] `nextScheduleContext` 설정
  - [ ] 초기 상태 결정 로직 추가
  - [ ] `transitionTo(initialState, calTime)` 호출 추가

- [ ] **1.2** `startStateMachineTracking()` 정리
  - [ ] 중복 로직 확인
  - [ ] 필요시 deprecated 처리 또는 삭제

- [ ] **1.3** 테스트
  - [ ] 시작 버튼 클릭 → 상태 전환 로그 확인
  - [ ] 스케줄 큐 로드 확인 (N개 스케줄)
  - [ ] 초기 상태 (STOWING 또는 MOVING_TRAIN) 확인

### 수정 코드 (예정)

```kotlin
// PassScheduleService.kt:369
fun startScheduleTracking() {
    // ... 기존 검증 코드 ...

    dataStoreService.stopAllTracking()
    resetTrackingState()

    // ===== 추가 코드 시작 =====
    val calTime = GlobalData.Time.calUtcTimeOffsetTime

    // 1. 스케줄 큐 빌드
    val allContexts = buildScheduleQueue(calTime)
    scheduleContextQueue.addAll(allContexts)

    if (scheduleContextQueue.isEmpty()) {
        logger.error("❌ 추적 가능한 스케줄 없음")
        return
    }

    // 2. 첫 스케줄 컨텍스트 설정
    currentScheduleContext = scheduleContextQueue.first()
    nextScheduleContext = scheduleContextQueue.getOrNull(1)

    // 3. 초기 상태 결정 및 전환
    val timeToStart = Duration.between(calTime, currentScheduleContext!!.startTime)
    val prepMinutes = settingsService.preparationTimeMinutes  // 4분 설정값 사용
    val initialState = if (timeToStart.toMinutes() <= prepMinutes) {
        PassScheduleState.MOVING_TRAIN
    } else {
        PassScheduleState.STOWING
    }

    transitionTo(initialState, calTime)
    // ===== 추가 코드 끝 =====

    // 타이머 시작
    trackingExecutor = threadManager.getTrackingExecutor()
    trackingMonitorTask = trackingExecutor?.scheduleAtFixedRate(
        { checkStateMachine() }, 0, 100, TimeUnit.MILLISECONDS
    )

    isTrackingMonitorRunning.set(true)
    logger.info("✅ 추적 시작 완료 (초기 상태: $initialState)")
}
```

---

## Phase 2: 시간 기준 수정

### 목표
Settings의 `preparationTimeMinutes` (4분) 값 사용

### 작업 항목

- [ ] **2.1** `determineStateByTime()` L2745 수정
  - [ ] `minutesToStart <= 2` → `minutesToStart <= prepMinutes`
  - [ ] `val prepMinutes = settingsService.preparationTimeMinutes` 추가

- [ ] **2.2** 관련 로직 일관성 확인
  - [ ] `evaluateNextSchedule()` 확인 (이미 prepMinutes 사용 중)
  - [ ] 문서 "2분" → "Settings 준비 시간" 으로 수정

---

## Phase 3: FE 연계 수정

### 목표
프론트엔드에서 현재/다음 스케줄 하이라이트 정상 동작

### 작업 항목

- [ ] **3.1** `sendStateToFrontend()` 수정
  - [ ] `ctx.detailId` 전달 추가
  - [ ] `nextScheduleContext?.mstId` 전달 추가
  - [ ] `nextScheduleContext?.detailId` 전달 추가

- [ ] **3.2** `dataStoreService` 연동 확인
  - [ ] `setCurrentTrackingMstId(mstId, detailId)` 호출
  - [ ] `setNextTrackingMstId(mstId, detailId)` 호출

- [ ] **3.3** FE 테스트
  - [ ] 테이블 행 초록색 (현재 추적)
  - [ ] 테이블 행 파란색 (다음 예정)

---

## Phase 4: 안정성 개선 (시퀀스 무결성)

### 목표
다중 위성 반복 추적 시 시퀀스가 끊기지 않도록 안정성 확보

### 작업 항목

- [ ] **4.1** 이동 타임아웃 추가 (H1, H2 해결)
  - [ ] `ScheduleTrackingContext`에 시작 시간 필드 추가
    ```kotlin
    var trainMoveStartTime: ZonedDateTime? = null
    var azElMoveStartTime: ZonedDateTime? = null
    ```
  - [ ] `updateProgressFlags()`에서 타임아웃 체크
  - [ ] Train 이동 타임아웃: 60초 (EphemerisService 참조)
  - [ ] Az/El 이동 타임아웃: 120초
  - [ ] 타임아웃 시 ERROR 상태 전환 + 로그

- [ ] **4.2** ERROR 복구 시 스킵 옵션 추가 (H3 해결)
  - [ ] `handleErrorRecovery()`에 스킵 로직 추가
  - [ ] 30초 실패 시 해당 스케줄만 스킵하고 다음 위성으로
  - [ ] 전체 중단은 3회 연속 실패 시에만

- [ ] **4.3** Time Jump 경고 로깅 (H4 해결)
  - [ ] `TRACKING` 진입 시 플래그 불일치 경고
  - [ ] 현재 위치 vs 목표 위치 차이 로깅

- [ ] **4.4** STOWING → STOWED 위치 확인 (M2 해결)
  - [ ] `isAtStowPosition()` 함수 구현
    ```kotlin
    private fun isAtStowPosition(): Boolean {
        val latestData = dataStoreService.getLatestData()
        val currentAz = latestData.azimuthAngle ?: return false
        val currentEl = latestData.elevationAngle ?: return false
        val stowAz = settingsService.stowAzimuth
        val stowEl = settingsService.stowElevation
        return abs(currentAz - stowAz) <= 0.5f &&
               abs(currentEl - stowEl) <= 0.5f
    }
    ```
  - [ ] `updateProgressFlags()`에서 Stow 위치 확인

- [ ] **4.5** 시간 정밀도 개선 (M3 해결)
  - [ ] `toMinutes()` → `toSeconds()` 변경 검토
  - [ ] 준비 시간: 분 → 초 단위 설정 지원

---

## Phase 5: 문서 동기화

### 작업 항목

- [ ] **5.1** mode-system.md 업데이트
  - [ ] V2 상태머신 반영 (11개 상태)
  - [ ] 상태 다이어그램 추가

- [ ] **5.2** satellite-tracking.md 업데이트
  - [ ] 타이머 주기 30ms → 100ms 수정

- [ ] **5.3** ANALYSIS.md 업데이트
  - [ ] 2026-01-19 검토 결과 반영

---

## 협의 사항

### 확인 완료

| 항목 | 결정 | 비고 |
|------|------|------|
| Train 안정화 시간 | 3초 | 적절 |
| 위치 허용 오차 | 0.05° | 적절 |
| 준비 시간 기본값 | 4분 (Settings) | 적절 |
| 타이머 주기 | 100ms | 적절 |

### 협의 필요

| 항목 | 현재 | 질문 |
|------|------|------|
| Stow vs 시작위치 결정 기준 | 4분 | 변경 필요? |
| 상태 점프 동작 | calTime이 추적 범위 내면 즉시 TRACKING | 정상 동작? |

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2026-01-08 | 1.0 | 초기 분석 및 설계 문서 작성 |
| 2026-01-19 | 1.1 | 전문가 검토 결과 반영, 시작 함수 문제 발견 |

---

## 관련 파일

### Backend
- `backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- `backend/src/main/kotlin/com/gtlsystems/acs_api/controller/mode/PassScheduleController.kt`

### Frontend
- `frontend/src/pages/mode/PassSchedulePage.vue`
- `frontend/src/stores/icd/icdStore.ts`

### 참조
- `backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt` (정상 동작 패턴)
