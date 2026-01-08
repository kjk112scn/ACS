# PassSchedule 상태 머신 리팩토링 분석 문서

> 작성일: 2026-01-08
> 목적: 다중 위성 순차 추적 시스템의 상태 관리 문제점 분석 및 개선 방안 도출

---

## 목차

1. [개요](#1-개요)
2. [요구사항 정의](#2-요구사항-정의)
3. [현재 구현 분석](#3-현재-구현-분석)
4. [EphemerisService와 비교](#4-ephemerisservice와-비교)
5. [발견된 문제점](#5-발견된-문제점)
6. [개선 방안](#6-개선-방안)

---

## 1. 개요

### 1.1 PassSchedule vs EphemerisService

| 항목 | EphemerisService | PassScheduleService |
|------|------------------|---------------------|
| 추적 대상 | 1개 위성, 1개 패스 | N개 위성, M개 패스 |
| 상태 구조 | 단일 열거형 + Phase | 이중 상태 (State + Step) |
| 스케줄 전환 | 없음 | 자동 순차 전환 |
| 복잡도 | 낮음 | 높음 |

### 1.2 핵심 기능

PassSchedule은 다음 기능을 자동으로 수행해야 함:

1. START 버튼 클릭 시 현재 시간 기준 추적 가능한 위성 조회
2. 추적 대기 (2분 이상 남음) → Stow 위치 대기
3. 추적 준비 (2분 이내) → 시작 위치로 자동 이동
4. 추적 시작 (시작 시간 도달) → 위성 추적 진행
5. 추적 종료 후 → 다음 스케줄 자동 판단 및 전환

---

## 2. 요구사항 정의

### 2.1 상태 전환 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PassSchedule 동작 시나리오                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [START 버튼 클릭]                                                  │
│       ↓                                                             │
│  현재 시간에서 추적 가능한 위성 조회 → 대상 위성 선정                 │
│       ↓                                                             │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 추적 대기 상태 (WAITING)                                      │   │
│  │ - 프론트: 현재 스케줄 파란색 표시                              │   │
│  │ - 백엔드: Stow 위치로 이동 후 대기 (시작 2분 이상 남음)        │   │
│  └──────────────────────────────────────────────────────────────┘   │
│       ↓ (시작 2분 이내 진입)                                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 추적 준비 상태 (PREPARING)                                    │   │
│  │ - 프론트: 현재 스케줄 녹색, 다음 스케줄 파란색                 │   │
│  │ - 백엔드: 시작 위치로 이동 (Train → Az/El)                    │   │
│  └──────────────────────────────────────────────────────────────┘   │
│       ↓ (시작 시간 도달)                                            │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 추적 중 상태 (TRACKING)                                       │   │
│  │ - 프론트: 현재 스케줄 녹색 유지                                │   │
│  │ - 백엔드: 위성 추적 진행 (TrackingCMD 전송)                    │   │
│  └──────────────────────────────────────────────────────────────┘   │
│       ↓ (추적 종료)                                                 │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 다음 스케줄 판단                                               │   │
│  │ - 다음 스케줄 2분 이내: 즉시 시작 위치로 이동 (PREPARING)     │   │
│  │ - 다음 스케줄 2분 이상: Stow 위치로 이동 (WAITING)            │   │
│  │ - 다음 스케줄 없음: Stow 위치로 이동 (COMPLETED)              │   │
│  └──────────────────────────────────────────────────────────────┘   │
│       ↓                                                             │
│  [다음 스케줄 반복...]                                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 프론트엔드 색상 규칙

| 상태 | 색상 | CSS 클래스 | 조건 |
|------|------|-----------|------|
| 현재 추적 중 | 녹색 (#c8e6c9) | `highlight-current-schedule` | currentTrackingMstId 일치 |
| 다음 대기 중 | 파란색 (#e3f2fd) | `highlight-next-schedule` | nextTrackingMstId 일치 |
| 일반 | 기본색 | - | 해당 없음 |

### 2.3 백엔드 상태 전환 규칙

| 현재 상태 | 조건 | 다음 상태 | 액션 |
|----------|------|----------|------|
| IDLE | START 버튼 | WAITING/PREPARING | 모니터링 시작 |
| WAITING | 2분 이내 진입 | PREPARING | 시작 위치 이동 |
| PREPARING | 시작 시간 도달 | TRACKING | 추적 시작 |
| TRACKING | 종료 시간 도달 | POST_TRACKING | 추적 종료 |
| POST_TRACKING | 다음 2분 이내 | PREPARING | 바로 시작 위치 |
| POST_TRACKING | 다음 2분 이상 | WAITING | Stow 이동 |
| POST_TRACKING | 다음 없음 | COMPLETED | Stow 이동 |

---

## 3. 현재 구현 분석

### 3.1 백엔드 상태 변수 (PassScheduleService.kt)

#### 상태 열거형
```kotlin
// 라인 69-84
enum class TrackingState {
    IDLE,       // 초기 대기 상태
    WAITING,    // 2분 이상 남음 - Stow 위치 대기
    PREPARING,  // 2분 이내 - 시작 위치로 이동
    TRACKING,   // 실제 추적 중
    COMPLETED   // 개별 스케줄 완료
}

// 라인 91-103 (이중 상태 문제!)
private enum class PreparingStep {
    INIT,           // 초기화
    MOVING_TRAIN,   // Train 회전 중
    WAITING_TRAIN,  // Train 안정화 대기
    MOVING_AZ_EL    // Az/El 이동 중
}
```

#### 핵심 변수들
| 변수명 | 타입 | 초기값 | 용도 |
|--------|------|--------|------|
| `currentTrackingState` | TrackingState | IDLE | 현재 전체 상태 |
| `currentPreparingStep` | PreparingStep | INIT | PREPARING 내부 단계 |
| `preparingPassId` | Long? | null | PREPARING 처리 중인 패스 ID |
| `targetAzimuth` | Float | 0f | 목표 방위각 |
| `targetElevation` | Float | 0f | 목표 고도각 |
| `trainStabilizationStartTime` | Long | 0 | Train 안정화 시작 시각 |
| `lastStateChangeTime` | Long | 0 | 상태 변경 시간 |
| `trackingCheckCount` | Long | 0 | 100ms 체크 카운트 |

#### 설정값
```kotlin
TRAIN_STABILIZATION_TIMEOUT = 3000L    // Train 안정화: 3초
MIN_STATE_CHANGE_INTERVAL = 500        // 상태 변경 간격: 0.5초
PREPARATION_TIME_MINUTES = 2L          // 준비 시간: 2분
```

### 3.2 핵심 함수 흐름

#### 100ms 타이머 (checkTrackingScheduleWithStateMachine)
```
checkTrackingScheduleWithStateMachine() [100ms 주기]
    ├─ calTime = GlobalData.Time.calUtcTimeOffsetTime
    ├─ currentSchedule = getCurrentSelectedTrackingPassWithTime(calTime)
    ├─ nextSchedule = getNextSelectedTrackingPassWithTime(calTime)
    ├─ newState = determineTrackingState(currentSchedule, nextSchedule, calTime)
    ├─ transitionToState(newState, currentSchedule, nextSchedule, calTime)
    └─ handleTrackingStateChangeSeparately(currentSchedule, calTime)
```

#### 상태 결정 로직 (determineTrackingState)
```kotlin
when {
    currentSchedule != null → TRACKING
    nextSchedule != null && isWithinPreparationTime() → PREPARING
    nextSchedule != null → WAITING
    else → COMPLETED
}
```

#### 상태 전환 (transitionToState)
```kotlin
// 핵심 문제점: PREPARING 상태 특별 처리
if (newState == PREPARING && currentTrackingState == PREPARING) {
    // 내부 단계 계속 진행
    executeStateAction(newState, ...)
    return
}

// 상태 동일 시 스킵
if (currentTrackingState == newState || !canChangeState()) {
    return
}

// 상태 변경
currentTrackingState = newState
executeStateAction(newState, ...)
```

### 3.3 프론트엔드 상태 관리

#### icdStore 상태 변수
| 변수 | 용도 |
|------|------|
| `currentTrackingMstId` | 현재 추적 중인 MstId |
| `currentTrackingDetailId` | 현재 추적 DetailId |
| `nextTrackingMstId` | 다음 추적 MstId |
| `nextTrackingDetailId` | 다음 추적 DetailId |
| `passScheduleTrackingState` | 백엔드 추적 상태 |

#### 하이라이트 로직 (PassSchedulePage.vue)
```typescript
// highlightedRows computed
const highlightedRows = computed(() => ({
  current: icdStore.currentTrackingMstId,
  currentDetailId: icdStore.currentTrackingDetailId,
  next: icdStore.nextTrackingMstId,
  nextDetailId: icdStore.nextTrackingDetailId
}))

// 매칭 조건
currentMatch = mstId 일치 && detailId 일치
nextMatch = mstId 일치 && detailId 일치
```

---

## 4. EphemerisService와 비교

### 4.1 EphemerisService 상태 구조 (좋은 설계)

```kotlin
// 단일 상태 열거형
enum class TrackingState {
    IDLE, PREPARING, WAITING, TRACKING, COMPLETED, ERROR
}

// PREPARING 세부 단계 (Phase)
enum class PreparingPhase {
    TRAIN_MOVING,       // Train 각도 이동 중
    TRAIN_STABILIZING,  // Train 안정화 대기 중
    MOVING_TO_TARGET    // 목표 Az/El로 이동 중
}
```

### 4.2 EphemerisService 상태 전환 (명확한 조건)

```
IDLE → PREPARING (startEphemerisTracking 호출)
    ↓
PREPARING/TRAIN_MOVING (moveTrainToZero)
    ↓ isTrainAtZero() == true
PREPARING/TRAIN_STABILIZING (3초 대기)
    ↓ 타임아웃
PREPARING/MOVING_TO_TARGET (moveToTargetAzEl)
    ↓ isAzElAtTarget() || 2분 타임아웃
WAITING (헤더 전송 완료)
    ↓ 시작 시간 도달
TRACKING (12.1/12.2 전송)
    ↓ 종료 시간 도달
COMPLETED
```

### 4.3 핵심 차이점

| 항목 | EphemerisService | PassScheduleService |
|------|------------------|---------------------|
| 상태 결정 | 내부 진행 기반 | 외부 시간 기반 |
| 완료 조건 | `isAzElAtTarget()` 명확 | 즉시 완료 처리 |
| 일회성 플래그 | `headerSent`, `initialDataSent` | 없음 |
| 중복 실행 방지 | 플래그로 명시적 관리 | 상태 전환 간격에 의존 |

---

## 5. 발견된 문제점

### 5.1 핵심 설계 문제

#### 문제 1: 외부 시간 vs 내부 상태 충돌
```
100ms마다 실행:
1. determineTrackingState() → "현재 시간" 기준으로 상태 결정
2. PREPARING 반환 (2분 이내)
3. executeStateAction(PREPARING) 실행
4. PreparingStep = MOVING_TRAIN 으로 설정

다음 100ms:
1. determineTrackingState() → 여전히 PREPARING 반환
2. transitionToState() → PREPARING == PREPARING 이므로 executeStateAction 호출
3. currentPreparingStep = INIT 이면 moveToStartPosition() 다시 호출!
```

**결과**: PREPARING 상태에서 무한 루프 또는 반복 명령 전송

#### 문제 2: PreparingStep 초기화 누락
```kotlin
// moveToStartPosition() 완료 후
PreparingStep.MOVING_AZ_EL -> {
    currentPreparingStep = PreparingStep.INIT  // INIT으로 복귀
    preparingPassId = null
}

// 다음 사이클에서 determineTrackingState() → PREPARING
// PreparingStep = INIT → moveToStartPosition() 다시 호출!
```

#### 문제 3: 스케줄 전환 시 상태 불일치
```
스케줄 A 추적 종료 (TRACKING → COMPLETED)
    ↓
determineTrackingState() → 다음 스케줄 B 있음, 2분 이내 → PREPARING
    ↓
executeStateAction(COMPLETED) 실행 → Stow 이동
executeStateAction(PREPARING) 실행 → 시작 위치 이동

결과: Stow와 시작 위치 명령이 동시에 전송됨
```

#### 문제 4: TRACKING 상태 중복 헤더 전송
```kotlin
TrackingState.TRACKING -> {
    if (currentSchedule != null) {
        prepareTrackingStart(mstId)  // 매번 호출됨!
    }
}
```

**문제**: `prepareTrackingStart()`에 내부 플래그가 있지만, 호출 자체가 불필요

### 5.2 프론트엔드 문제

#### 문제 5: 하이라이트 색상 불일치
- 백엔드 상태와 프론트엔드 색상이 동기화되지 않을 수 있음
- `currentTrackingMstId`가 업데이트되어도 테이블 렌더링 지연

#### 문제 6: timeRemaining 계산 오류
- 서버 시간(calTime)과 로컬 시간 차이
- CAL 시간 오프셋 적용 미비

---

## 6. 개선 방안

### 6.1 상태 머신 재설계 (권장)

#### 새로운 상태 열거형 (단일 통합)
```kotlin
enum class PassScheduleState {
    // 초기 상태
    IDLE,

    // 대기 상태
    STOWING,              // Stow 위치로 이동 중
    STOWED,               // Stow 위치 대기 중

    // 준비 상태 (PREPARING 세분화)
    MOVING_TRAIN,         // Train 이동 중
    TRAIN_STABILIZING,    // Train 안정화 대기 (3초)
    MOVING_TO_START,      // 시작 위치(Az/El)로 이동 중
    READY,                // 시작 위치 도달, 시작 시간 대기

    // 추적 상태
    TRACKING,             // 실시간 추적 중

    // 종료 상태
    POST_TRACKING,        // 추적 종료, 다음 스케줄 판단 중
    COMPLETED,            // 모든 스케줄 완료

    // 오류 상태
    ERROR
}
```

#### 상태 전환 규칙

```
[IDLE]
    │ startScheduleTracking()
    ▼
┌───────────────────────────────────────────┐
│           SCHEDULE_MONITORING             │
│                                           │
│   nextSchedule 없음 → [COMPLETED]         │
│   nextSchedule 2분 이상 → [STOWING]       │
│   nextSchedule 2분 이내 → [MOVING_TRAIN]  │
│                                           │
│   [STOWING]                               │
│       │ Stow 명령 전송                    │
│       ▼                                   │
│   [STOWED]                                │
│       │ 2분 이내 진입                     │
│       ▼                                   │
│   [MOVING_TRAIN]                          │
│       │ Train 각도 도달                   │
│       ▼                                   │
│   [TRAIN_STABILIZING]                     │
│       │ 3초 경과                          │
│       ▼                                   │
│   [MOVING_TO_START]                       │
│       │ Az/El 도달                        │
│       ▼                                   │
│   [READY]                                 │
│       │ 시작 시간 도달                    │
│       ▼                                   │
│   [TRACKING]                              │
│       │ 종료 시간 도달                    │
│       ▼                                   │
│   [POST_TRACKING]                         │
│       │ 다음 스케줄 판단                  │
│       ├─ 2분 이내 → [MOVING_TRAIN]        │
│       ├─ 2분 이상 → [STOWING]             │
│       └─ 없음 → [COMPLETED]               │
│                                           │
└───────────────────────────────────────────┘
```

### 6.2 구현 개선 방향

#### 상태 진입/퇴장 액션 분리
```kotlin
private fun enterState(state: PassScheduleState) {
    when (state) {
        STOWING -> {
            udpFwICDService.StowCommand()
        }
        MOVING_TRAIN -> {
            val trainAngle = calculateTrainAngle()
            moveTrainToZero(trainAngle)
        }
        TRAIN_STABILIZING -> {
            trainStabilizationStartTime = System.currentTimeMillis()
        }
        MOVING_TO_START -> {
            moveToTargetAzEl()
        }
        READY -> {
            // 헤더 전송 준비
        }
        TRACKING -> {
            sendHeaderTrackingData(currentPassId)
        }
        // ...
    }
}
```

#### 상태 체크 로직 분리
```kotlin
private fun checkStateCondition(state: PassScheduleState): PassScheduleState? {
    return when (state) {
        STOWING -> {
            if (isAtStowPosition()) STOWED else null
        }
        STOWED -> {
            if (isWithin2Minutes()) MOVING_TRAIN else null
        }
        MOVING_TRAIN -> {
            if (isTrainAtTarget()) TRAIN_STABILIZING else null
        }
        TRAIN_STABILIZING -> {
            if (isTrainStabilizationComplete()) MOVING_TO_START else null
        }
        MOVING_TO_START -> {
            if (isAzElAtTarget()) READY else null
        }
        READY -> {
            if (isStartTimeReached()) TRACKING else null
        }
        TRACKING -> {
            if (isEndTimeReached()) POST_TRACKING else null
        }
        // ...
        else -> null
    }
}
```

### 6.3 일회성 플래그 관리

```kotlin
data class ScheduleTrackingContext(
    val passId: Long,
    val detailId: Int,
    var stowCommandSent: Boolean = false,
    var trainMoveCommandSent: Boolean = false,
    var azElMoveCommandSent: Boolean = false,
    var headerSent: Boolean = false,
    var initialDataSent: Boolean = false
)
```

### 6.4 프론트엔드 개선

#### 상태와 색상 매핑 명확화
```typescript
const getScheduleColor = (state: PassScheduleState, mstId: number) => {
    if (state === 'TRACKING' && currentMstId === mstId) {
        return 'green'  // 현재 추적 중
    }
    if (['MOVING_TRAIN', 'TRAIN_STABILIZING', 'MOVING_TO_START', 'READY'].includes(state)
        && nextMstId === mstId) {
        return 'green'  // 준비 중 (현재)
    }
    if (nextMstId === mstId) {
        return 'blue'   // 다음 대기
    }
    return 'default'
}
```

---

## 다음 단계

1. **DESIGN.md 작성**: 상세 설계 문서
2. **구현**: 단계별 리팩토링
3. **테스트**: 시나리오별 검증
4. **문서화**: 완료 후 업데이트

---

## 관련 파일

- Backend: `backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- Frontend: `frontend/src/pages/mode/PassSchedulePage.vue`
- Store: `frontend/src/stores/passSchedule/usePassScheduleModeStore.ts`
- ICD Store: `frontend/src/stores/icd/icdStore.ts`
