# 모드 시스템 체크리스트 (ACS 특화)

> ACS 안테나 제어 모드 시스템 분석

---

## 1. 모드 전환 검증

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **MODE-01** | 전환 전 상태 저장 | High | 이전 모드 명령 완료 확인 |
| **MODE-02** | Standby 경유 규칙 | Medium | 추적 모드 간 직접 전환 금지 |
| **MODE-03** | 비상 정지 우선권 | Critical | 모든 상태에서 Standby 전환 |
| **MODE-04** | 모드별 리소스 정리 | High | WebSocket, 타이머 정리 |
| **MODE-05** | 모드 상태 UI 동기화 | Medium | FE/BE 모드 상태 일치 |
| **MODE-06** | 모드 전환 로깅 | Medium | 상태 변경 이력 추적 |

### 모드 전환 검증 흐름

```
모드 전환 요청
    ↓
[MODE-01] 이전 명령 완료 확인
    ↓
[MODE-02] 전환 규칙 검증
    ↓
[MODE-04] 이전 모드 리소스 정리
    ↓
[MODE-05] FE/BE 상태 동기화
    ↓
새 모드 시작
```

### ACS 모드 목록

| 모드 | 설명 | 허용 전환 |
|------|------|----------|
| **Standby** | 대기 | → 모든 모드 |
| **Step** | 수동 이동 | → Standby |
| **Slew** | 자동 이동 | → Standby |
| **EphemerisDesignation** | 위성 궤도 지정 | → Standby |
| **PassSchedule** | 패스 스케줄 | → Standby |
| **SunTrack** | 태양 추적 | → Standby |

---

## 2. PassSchedule 특화

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **PS-01** | 스케줄 큐 동기화 | High | queueLock 사용 여부 |
| **PS-02** | 컨텍스트 플래그 리셋 | High | 스케줄 전환 시 resetFlags() |
| **PS-03** | 2분 기준 시간 계산 | High | 시작 시간 2분 전 준비 |
| **PS-04** | Train 안정화 3초 대기 | Medium | TRAIN_STABILIZING 상태 |
| **PS-05** | Time Offset 적용 | High | calTime 우선 사용 |
| **PS-06** | 다음 스케줄 자동 전환 | High | 현재 완료 → 다음 시작 |
| **PS-07** | 색상 구분 로직 | Medium | 현재/다음 스케줄 시각 구분 |

### PassSchedule 상태머신

```
IDLE
  ↓ Start 명령
STOWING (Stow 필요 시)
  ↓
STOWED
  ↓
MOVING_TRAIN (Train 이동)
  ↓
TRAIN_STABILIZING (3초 대기)
  ↓
MOVING_TO_START (AOS 위치로)
  ↓
READY (대기)
  ↓ AOS 도달
TRACKING (추적 중)
  ↓ LOS 도달
POST_TRACKING
  ↓
COMPLETED
  ↓ 다음 스케줄 있음
IDLE (반복)
```

### 검증 코드 패턴

```kotlin
// PS-02: 컨텍스트 플래그 리셋
fun resetFlags() {
    isStowed = false
    trainMoveStarted = false
    trainStabilized = false
    readyForTracking = false
    aosReached = false
    losReached = false
}

// PS-03: 2분 전 준비 시작
val prepareTime = startTime.minusMinutes(2)
if (now.isAfter(prepareTime)) {
    startPreparation()
}

// PS-05: Time Offset 적용
val effectiveTime = calTime ?: (trackTime + timeOffset)
```

---

## 3. EphemerisDesignation 특화

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **ED-01** | TLE 캐시 갱신 | Medium | 만료된 TLE 사용 방지 |
| **ED-02** | Keyhole 감지/대응 | High | 고고도 패스 Train 조정 |
| **ED-03** | 추적 데이터 배치 저장 | Medium | 100개 버퍼 플러시 |
| **ED-04** | Orekit 초기화 확인 | Critical | DataContext 설정 |
| **ED-05** | 전파자 스레드 안전성 | High | Propagator 재사용 문제 |
| **ED-06** | 좌표계 변환 정확성 | Critical | TEME → ITRF |

### Keyhole 감지 패턴

```kotlin
// ED-02: Keyhole 감지
fun detectKeyhole(elevation: Double, trainAngle: Double): Boolean {
    // 고고도 패스에서 Keyhole 발생 가능
    return elevation > 80.0 && abs(trainAngle) < 5.0
}

fun handleKeyhole() {
    // Train 각도 조정
    adjustTrainForKeyhole()
}
```

---

## 4. SunTrack 특화

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **ST-01** | 일출/일몰 시간 계산 | High | 정확한 태양 위치 |
| **ST-02** | 야간 모드 전환 | Medium | 해가 진 후 대기 |
| **ST-03** | 태양 위치 업데이트 주기 | Medium | 1초 주기 권장 |
| **ST-04** | 구름/장애물 대응 | Low | 일시적 추적 손실 |

---

## 5. 공통 상태머신 패턴

### 상태 전이 원자성

```kotlin
// GOOD: 원자적 상태 전이
private val stateLock = ReentrantLock()
private var currentState = State.IDLE

fun transitionTo(newState: State) {
    stateLock.withLock {
        val oldState = currentState
        if (!isValidTransition(oldState, newState)) {
            throw IllegalStateException("Invalid: $oldState → $newState")
        }
        currentState = newState
        logger.info("State: $oldState → $newState")
        notifyStateChange(oldState, newState)
    }
}
```

### 상태 전이 검증 맵

```kotlin
// MODE-02: 허용된 전이 정의
val validTransitions = mapOf(
    State.IDLE to setOf(State.STOWING, State.MOVING_TRAIN, State.ERROR),
    State.STOWING to setOf(State.STOWED, State.ERROR),
    State.STOWED to setOf(State.MOVING_TRAIN, State.ERROR),
    State.MOVING_TRAIN to setOf(State.TRAIN_STABILIZING, State.ERROR),
    State.TRAIN_STABILIZING to setOf(State.MOVING_TO_START, State.ERROR),
    State.MOVING_TO_START to setOf(State.READY, State.ERROR),
    State.READY to setOf(State.TRACKING, State.ERROR),
    State.TRACKING to setOf(State.POST_TRACKING, State.ERROR),
    State.POST_TRACKING to setOf(State.COMPLETED, State.ERROR),
    State.COMPLETED to setOf(State.IDLE),
    State.ERROR to setOf(State.IDLE)
)

fun isValidTransition(from: State, to: State): Boolean {
    return validTransitions[from]?.contains(to) == true
}
```

---

## 6. 타이밍 요구사항

| 항목 | 주기 | 허용 오차 | 체크 |
|------|------|----------|------|
| ICD 데이터 수신 | 30ms | ±5ms | |
| 상태 업데이트 | 100ms | ±10ms | |
| 위치 계산 | 100ms | ±20ms | |
| UI 갱신 | 100ms | ±50ms | |

### 타이밍 검증

```kotlin
// 타이밍 모니터링
class TimingMonitor {
    private var lastTick = System.nanoTime()

    fun checkTiming(expectedMs: Long) {
        val now = System.nanoTime()
        val actualMs = (now - lastTick) / 1_000_000
        val drift = actualMs - expectedMs

        if (abs(drift) > expectedMs * 0.2) { // 20% 이상 오차
            logger.warn("Timing drift: ${drift}ms (expected: ${expectedMs}ms)")
        }
        lastTick = now
    }
}
```

---

## 7. 에러 복구

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **ERR-01** | ERROR 상태 진입 조건 | High | 어떤 오류가 ERROR로 가는지 |
| **ERR-02** | ERROR 복구 경로 | High | ERROR → IDLE 전이 |
| **ERR-03** | 부분 실패 처리 | Medium | 스케줄 일부 실패 시 |
| **ERR-04** | 재시도 로직 | Medium | 일시적 오류 재시도 |
| **ERR-05** | 비상 정지 동작 | Critical | 안테나 즉시 정지 |

### 에러 복구 패턴

```kotlin
// ERR-02: ERROR 복구
fun handleError(error: Exception) {
    logger.error("Error in state $currentState", error)

    // 1. 상태를 ERROR로 전이
    transitionTo(State.ERROR)

    // 2. 리소스 정리
    cleanup()

    // 3. 사용자에게 알림
    notifyError(error)

    // 4. 일정 시간 후 IDLE로 복구 시도
    scheduler.schedule({
        if (currentState == State.ERROR) {
            transitionTo(State.IDLE)
        }
    }, 5, TimeUnit.SECONDS)
}
```

---

## 검사 명령 예시

```bash
# 상태 전이 검색
grep -r "transitionTo\|setState\|currentState" --include="*.kt"

# 플래그 리셋 확인
grep -r "resetFlags\|reset()" --include="*.kt"

# 타이밍 관련 코드
grep -r "scheduleAtFixedRate\|Timer\|delay" --include="*.kt"
```

---

**버전:** 1.0.0
**작성일:** 2026-01-26
