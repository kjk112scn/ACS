# PassSchedule 상태 머신 재설계 상세 설계서

> 작성일: 2026-01-08
> 버전: 1.1
> 상태: Ready for Implementation

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-01-08 | 초안 작성 |
| 1.1 | 2026-01-08 | 테크리드 검토 결과 반영 |

### 1.1 버전 변경 내용 (테크리드 검토)
- **타입 일관성**: 모든 시간 관련 파라미터를 `ZonedDateTime`으로 통일
- **플래그 리셋**: `resetFlags()` 함수 추가 및 스케줄 전환 시 호출
- **Time Offset 변경 감지**: `handleTimeOffsetChange()` 함수 추가
- **ERROR 복구 로직**: `handleErrorRecovery()` 및 자동 복구 메커니즘 추가
- **안전한 일괄 종료**: `safeBatchShutdown()` 함수 추가
- **상태 점프 처리**: TRACKING 진입 시 플래그 강제 완료 로직 추가

---

## 1. 설계 목표

### 1.1 해결해야 할 문제
1. 외부 시간 기반 상태 결정 → 내부 진행 기반으로 변경
2. 이중 상태(TrackingState + PreparingStep) → 단일 통합 상태로 변경
3. 반복 명령 전송 문제 → 일회성 플래그로 해결
4. 스케줄 전환 시 상태 불일치 → 명확한 전환 규칙 정의

### 1.2 설계 원칙
- **시간 기반 상태 결정 (최우선!)**: 매 사이클마다 calTime 기준으로 상태 결정
- **상태 점프 허용**: 준비 중이라도 추적 시간 도달 시 즉시 TRACKING
- **단일 상태 열거형**: 모든 상태를 하나의 enum으로 관리
- **진입 액션**: 상태 진입 시 1회만 실행되는 명령
- **컨텍스트 관리**: 스케줄별 일회성 플래그 추적
- **Time Offset 변경 시 재평가**: 스케줄 큐 및 상태 재결정

### 1.3 Time Offset 지원 (핵심!)

```
┌─────────────────────────────────────────────────────────────────────┐
│  ⚠️ 중요: 매 100ms마다 calTime 기준으로 상태를 결정!                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  calTime = GlobalData.Time.calUtcTimeOffsetTime                     │
│         = 실제 UTC 시간 + Time Offset (초)                          │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 시나리오 1: Time Offset으로 2분 이내 진입                   │    │
│  │ ─────────────────────────────────────────────────────────── │    │
│  │ 실제 시간: 14:00                                            │    │
│  │ 스케줄 시작: 15:00                                          │    │
│  │ Time Offset: +59분                                         │    │
│  │ calTime: 14:59                                             │    │
│  │                                                            │    │
│  │ → 1분 남음 (2분 이내)                                      │    │
│  │ → PREPARING 상태 진입, 시작 위치로 이동                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 시나리오 2: Time Offset으로 추적 시간 진입 (상태 점프!)     │    │
│  │ ─────────────────────────────────────────────────────────── │    │
│  │ 현재 상태: PREPARING (Train 이동 중)                        │    │
│  │ Time Offset: +61분 (추가로 2분 더 설정)                    │    │
│  │ calTime: 15:01 (스케줄 시작 시간 초과!)                    │    │
│  │                                                            │    │
│  │ → calTime이 추적 범위 내 (startTime ~ endTime)             │    │
│  │ → Train 이동 중단하고 즉시 TRACKING 상태로 전환!           │    │
│  │ → EphemerisService와 동일한 동작                           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 시나리오 3: Time Offset 변경으로 스케줄 큐 재평가           │    │
│  │ ─────────────────────────────────────────────────────────── │    │
│  │ Time Offset: -2시간 (과거로 이동)                          │    │
│  │ calTime: 12:59                                             │    │
│  │                                                            │    │
│  │ → 이전에 완료된 스케줄이 다시 "미래"가 됨                  │    │
│  │ → 스케줄 큐 재구성 필요                                    │    │
│  │ → 새로운 calTime 기준으로 상태 재결정                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**핵심 원칙 (EphemerisService 참조):**
```kotlin
// EphemerisService.kt:932-944
val calTime = GlobalData.Time.calUtcTimeOffsetTime
val isInTrackingTime = calTime.isAfter(startTime) && calTime.isBefore(endTime)

if (isInTrackingTime) {
    // ✅ 추적 시간 중 → 시작 위치 건너뛰고 바로 TRACKING 상태로 전환
}
```

**적용 대상:**
- 스케줄 시작/종료 시간 비교 (매 100ms)
- 상태 결정 (시간 기반 우선!)
- 스케줄 큐 재평가 (Time Offset 변경 시)

---

## 2. 상태 정의

### 2.1 PassScheduleState 열거형

```kotlin
/**
 * PassSchedule 통합 상태 머신
 *
 * 모든 상태를 단일 열거형으로 관리하여 이중 상태 문제 해결
 */
enum class PassScheduleState {
    // ===== 초기 상태 =====
    IDLE,                   // 시작 전 대기 상태

    // ===== 대기 상태 (2분 이상 남음) =====
    STOWING,                // Stow 위치로 이동 중
    STOWED,                 // Stow 위치 도달, 대기 중

    // ===== 준비 상태 (2분 이내) =====
    MOVING_TRAIN,           // Train 각도 이동 중 (키홀 대응)
    TRAIN_STABILIZING,      // Train 안정화 대기 중 (3초)
    MOVING_TO_START,        // 시작 위치(Az/El)로 이동 중
    READY,                  // 시작 위치 도달, 시작 시간 대기

    // ===== 추적 상태 =====
    TRACKING,               // 실시간 위성 추적 중

    // ===== 종료 상태 =====
    POST_TRACKING,          // 추적 종료, 다음 스케줄 평가 중
    COMPLETED,              // 모든 스케줄 완료

    // ===== 오류 상태 =====
    ERROR                   // 오류 발생
}
```

### 2.2 상태별 설명

| 상태 | 설명 | 진입 조건 | 퇴장 조건 |
|------|------|-----------|-----------|
| IDLE | 초기 대기 | - | START 버튼 클릭 |
| STOWING | Stow 이동 중 | 다음 스케줄 2분+ | Stow 위치 도달 |
| STOWED | Stow 대기 | Stow 도달 | 2분 이내 진입 |
| MOVING_TRAIN | Train 이동 | 2분 이내 | Train 각도 도달 |
| TRAIN_STABILIZING | Train 안정화 | Train 도달 | 3초 경과 |
| MOVING_TO_START | Az/El 이동 | Train 안정화 완료 | 시작 위치 도달 |
| READY | 시작 대기 | 시작 위치 도달 | 시작 시간 도달 |
| TRACKING | 추적 중 | 시작 시간 도달 | 종료 시간 도달 |
| POST_TRACKING | 후처리 | 추적 종료 | 다음 상태 결정됨 |
| COMPLETED | 완료 | 다음 스케줄 없음 | STOP 또는 재시작 |
| ERROR | 오류 | 오류 발생 | 복구 또는 STOP |

---

## 3. 스케줄 컨텍스트

### 3.1 ScheduleTrackingContext 클래스

```kotlin
/**
 * 개별 스케줄 추적 컨텍스트
 *
 * 각 스케줄에 대한 일회성 플래그와 상태 정보를 관리
 *
 * ⚠️ 시간 타입: ZonedDateTime (GlobalData.Time.calUtcTimeOffsetTime과 동일)
 */
data class ScheduleTrackingContext(
    // ===== 스케줄 식별 =====
    val mstId: Long,
    val detailId: Int,
    val satelliteName: String,

    // ===== 시간 정보 (ZonedDateTime - 스케줄 고정값) =====
    val startTime: ZonedDateTime,      // 스케줄 시작 시간
    val endTime: ZonedDateTime,        // 스케줄 종료 시간

    // ===== 시작 위치 정보 =====
    val startAzimuth: Float,           // 시작 방위각 (radians)
    val startElevation: Float,         // 시작 고도각 (radians)
    val trainAngle: Float,             // Train 각도 (radians)

    // ===== 일회성 명령 플래그 (한 번만 전송 보장) =====
    var stowCommandSent: Boolean = false,
    var trainMoveCommandSent: Boolean = false,
    var azElMoveCommandSent: Boolean = false,
    var headerSent: Boolean = false,
    var initialTrackingDataSent: Boolean = false,

    // ===== 진행 완료 플래그 (상태 결정에 사용) =====
    var trainMoveCompleted: Boolean = false,       // Train 목표 도달 여부
    var trainStabilizationCompleted: Boolean = false,  // Train 안정화 완료 여부
    var azElMoveCompleted: Boolean = false,        // Az/El 목표 도달 여부

    // ===== 타이밍 정보 (ZonedDateTime - 진행 중 기록) =====
    var trainStabilizationStartTime: ZonedDateTime? = null,  // Train 안정화 시작 시점
    var stateEntryTime: ZonedDateTime? = null                // 상태 진입 시점
)
```

### 3.2 컨텍스트 생성 함수

```kotlin
private fun createScheduleContext(
    pass: PassScheduleTimeData,
    trackingData: List<PassScheduleTrackingDataDto>
): ScheduleTrackingContext {
    val firstPoint = trackingData
        .filter { it.dataType == "final_transformed" || it.dataType == "keyhole_final_transformed" }
        .minByOrNull { it.utcEpoch }

    return ScheduleTrackingContext(
        mstId = pass.mstId,
        detailId = pass.detailId,
        satelliteName = pass.sttSatelliteName ?: "Unknown",
        startTime = pass.startTimeEpoch,
        endTime = pass.endTimeEpoch,
        startAzimuth = firstPoint?.azimuth?.toFloat() ?: 0f,
        startElevation = firstPoint?.elevation?.toFloat() ?: 0f,
        trainAngle = calculateTrainAngle(firstPoint)
    )
}
```

---

## 4. 상태 머신 구현

### 4.1 핵심 변수

```kotlin
class PassScheduleService {
    // ===== 상태 관리 =====
    private var currentState: PassScheduleState = PassScheduleState.IDLE
    private var previousState: PassScheduleState = PassScheduleState.IDLE

    // ===== 컨텍스트 관리 =====
    private var currentContext: ScheduleTrackingContext? = null
    private var nextContext: ScheduleTrackingContext? = null

    // ===== 전체 스케줄 큐 =====
    private val scheduleQueue = mutableListOf<ScheduleTrackingContext>()

    // ===== 타이머 카운트 =====
    private var checkCount: Long = 0L

    // ===== 종료 플래그 =====
    private var isShuttingDown: Boolean = false

    // ===== 상수 =====
    companion object {
        const val PREPARATION_TIME_MS = 2 * 60 * 1000L  // 2분
        const val TRAIN_STABILIZATION_MS = 3000L        // 3초
        const val POSITION_TOLERANCE_RAD = 0.001f       // ~0.057도
        const val CHECK_INTERVAL_MS = 100L              // 100ms
    }
}
```

### 4.1.1 시간 기준 및 진행 상태 업데이트

```kotlin
/**
 * ⚠️ 중요: 모든 시간 연산은 calTime (ZonedDateTime) 기준
 */
private fun getCurrentCalTime(): ZonedDateTime {
    return GlobalData.Time.calUtcTimeOffsetTime
}

/**
 * 매 100ms마다 진행 상태 업데이트
 *
 * 하드웨어 위치 확인 후 컨텍스트 플래그 갱신
 */
private fun updateProgressFlags(calTime: ZonedDateTime) {
    val ctx = currentContext ?: return

    // Train 이동 완료 체크
    if (ctx.trainMoveCommandSent && !ctx.trainMoveCompleted) {
        if (isTrainAtTarget(ctx.trainAngle)) {
            ctx.trainMoveCompleted = true
            ctx.trainStabilizationStartTime = calTime  // 안정화 시작
            logger.info("✅ Train 목표 도달, 안정화 시작")
        }
    }

    // Train 안정화 완료 체크 (3초 경과)
    if (ctx.trainMoveCompleted && !ctx.trainStabilizationCompleted) {
        val stabilizationStart = ctx.trainStabilizationStartTime
        if (stabilizationStart != null) {
            val elapsed = Duration.between(stabilizationStart, calTime)
            if (elapsed.toMillis() >= TRAIN_STABILIZATION_MS) {
                ctx.trainStabilizationCompleted = true
                logger.info("✅ Train 안정화 완료 (3초 경과)")
            }
        }
    }

    // Az/El 이동 완료 체크
    if (ctx.azElMoveCommandSent && !ctx.azElMoveCompleted) {
        if (isAzElAtTarget(ctx.startAzimuth, ctx.startElevation)) {
            ctx.azElMoveCompleted = true
            logger.info("✅ Az/El 목표 도달")
        }
    }
}
```

### 4.2 메인 타이머 루프

```kotlin
/**
 * 100ms 주기 상태 체크 함수
 *
 * ⭐ 순서 중요:
 * 0. 종료 중 체크 (isShuttingDown)
 * 1. 진행 상태 업데이트 (하드웨어 위치 확인)
 * 2. ERROR 상태 복구 시도
 * 3. 시간 기반 상태 결정 (calTime 우선!)
 * 4. 상태 전환 시 진입 액션 실행
 * 5. 주기적 작업 (추적 데이터 전송 등)
 */
private fun checkStateMachine() {
    // 0️⃣ 종료 중이면 아무 작업도 하지 않음
    if (isShuttingDown) {
        return
    }

    checkCount++

    val calTime = GlobalData.Time.calUtcTimeOffsetTime

    // 10초마다 상태 로깅
    if (checkCount % 100L == 0L) {
        logger.info("[STATE] 현재: $currentState, 스케줄: ${currentContext?.satelliteName}, calTime: $calTime")
    }

    // 1️⃣ 진행 상태 업데이트 (Train/Az/El 위치 확인)
    updateProgressFlags(calTime)

    // 2️⃣ ERROR 상태 복구 시도
    if (currentState == PassScheduleState.ERROR) {
        handleErrorRecovery(calTime)
        return  // ERROR 복구 중에는 다른 처리 스킵
    }

    // 3️⃣ 시간 기반 상태 결정
    val nextState = evaluateTransition(calTime)

    // 4️⃣ 상태 전환
    if (nextState != null && nextState != currentState) {
        transitionTo(nextState, calTime)
    }

    // 5️⃣ 상태별 주기적 작업 (추적 데이터 전송 등)
    executePeriodicAction(calTime)
}
```

### 4.3 상태 전환 조건 평가

```kotlin
/**
 * ⭐ 핵심: 시간 기반 상태 결정 (Time Offset 지원)
 *
 * 매 100ms마다 calTime 기준으로 상태를 결정
 * 현재 내부 상태와 무관하게 시간이 우선!
 *
 * EphemerisService 참조:
 * - calTime이 추적 범위 내면 즉시 TRACKING
 * - 준비 중이라도 시간 도달하면 상태 점프
 */
private fun determineStateByTime(calTime: ZonedDateTime): PassScheduleState {
    val ctx = currentContext ?: return PassScheduleState.COMPLETED

    val startTime = ctx.startTime  // ZonedDateTime
    val endTime = ctx.endTime      // ZonedDateTime

    // 1️⃣ 최우선: 추적 시간 범위 체크 (EphemerisService와 동일)
    val isInTrackingTime = calTime.isAfter(startTime) && calTime.isBefore(endTime)
    if (isInTrackingTime) {
        logger.info("🎯 calTime이 추적 범위 내 → 즉시 TRACKING")
        return PassScheduleState.TRACKING
    }

    // 2️⃣ 추적 종료 체크
    if (calTime.isAfter(endTime)) {
        logger.info("⏹️ 추적 종료 시간 경과 → POST_TRACKING")
        return PassScheduleState.POST_TRACKING
    }

    // 3️⃣ 추적 시작 전: 남은 시간으로 상태 결정
    val timeToStart = Duration.between(calTime, startTime)
    val minutesToStart = timeToStart.toMinutes()

    return when {
        minutesToStart <= 2 -> {
            // 2분 이내: PREPARING (시작 위치로 이동)
            // 내부 진행 상태에 따라 세부 상태 결정
            determinePreparingSubState(calTime)
        }
        else -> {
            // 2분 이상: WAITING (Stow 대기)
            PassScheduleState.STOWED
        }
    }
}

/**
 * PREPARING 내부 세부 상태 결정
 *
 * 2분 이내일 때 Train → Az/El 순서로 진행
 * 단, 시간이 도달하면 상위 함수에서 TRACKING으로 점프됨
 */
private fun determinePreparingSubState(calTime: ZonedDateTime): PassScheduleState {
    val ctx = currentContext ?: return PassScheduleState.ERROR

    return when {
        // Train 이동 완료 + 안정화 완료 + Az/El 도달
        ctx.azElMoveCompleted && isAzElAtTarget(ctx.startAzimuth, ctx.startElevation) -> {
            PassScheduleState.READY
        }
        // Train 이동 완료 + 안정화 완료
        ctx.trainMoveCompleted && ctx.trainStabilizationCompleted -> {
            PassScheduleState.MOVING_TO_START
        }
        // Train 이동 완료 (안정화 대기)
        ctx.trainMoveCompleted && isTrainAtTarget(ctx.trainAngle) -> {
            PassScheduleState.TRAIN_STABILIZING
        }
        // Train 이동 중
        ctx.trainMoveCommandSent -> {
            PassScheduleState.MOVING_TRAIN
        }
        // 아직 시작 안함
        else -> {
            PassScheduleState.MOVING_TRAIN
        }
    }
}

/**
 * 메인 상태 평가 함수
 *
 * 시간 기반 상태 + 현재 상태를 비교하여 전환 결정
 */
private fun evaluateTransition(calTime: ZonedDateTime): PassScheduleState? {
    // IDLE 상태는 START 버튼에 의해서만 변경
    if (currentState == PassScheduleState.IDLE) {
        return null
    }

    // 시간 기반으로 결정된 상태
    val timeBasedState = determineStateByTime(calTime)

    // 현재 상태와 다르면 전환
    return if (timeBasedState != currentState) {
        timeBasedState
    } else {
        null
    }
}
```

### 4.4 다음 스케줄 평가

```kotlin
/**
 * 추적 완료 후 다음 스케줄 평가
 *
 * ⚠️ 플래그 리셋: 새 스케줄로 전환 시 반드시 resetFlags() 호출
 */
private fun evaluateNextSchedule(calTime: ZonedDateTime): PassScheduleState {
    // 다음 스케줄 가져오기 (아직 종료되지 않은 것)
    val nextSchedule = scheduleQueue
        .filter { it.endTime.isAfter(calTime) }
        .minByOrNull { it.startTime }

    if (nextSchedule == null) {
        logger.info("[SCHEDULE] 다음 스케줄 없음 → COMPLETED")
        currentContext = null
        nextContext = null
        return PassScheduleState.COMPLETED
    }

    // ⚠️ 플래그 리셋하여 새 컨텍스트로 전환
    currentContext = nextSchedule.resetFlags()
    nextContext = scheduleQueue.getOrNull(scheduleQueue.indexOf(nextSchedule) + 1)

    val timeToStart = Duration.between(calTime, nextSchedule.startTime)

    return if (timeToStart.toMinutes() <= 2) {
        logger.info("[SCHEDULE] 다음 스케줄 2분 이내 → MOVING_TRAIN")
        PassScheduleState.MOVING_TRAIN
    } else {
        logger.info("[SCHEDULE] 다음 스케줄 2분 이상 → STOWING")
        PassScheduleState.STOWING
    }
}

/**
 * ⚠️ 플래그 리셋 함수
 *
 * 스케줄 전환 시 모든 일회성/진행 플래그를 초기화
 */
fun ScheduleTrackingContext.resetFlags(): ScheduleTrackingContext {
    return this.copy(
        // 일회성 명령 플래그 리셋
        stowCommandSent = false,
        trainMoveCommandSent = false,
        azElMoveCommandSent = false,
        headerSent = false,
        initialTrackingDataSent = false,
        // 진행 완료 플래그 리셋
        trainMoveCompleted = false,
        trainStabilizationCompleted = false,
        azElMoveCompleted = false,
        // 타이밍 정보 리셋
        trainStabilizationStartTime = null,
        stateEntryTime = null
    )
}
```

### 4.5 상태 전환 실행

```kotlin
/**
 * 상태 전환 및 진입 액션 실행
 *
 * @param newState 새 상태
 * @param calTime 현재 calTime (ZonedDateTime)
 */
private fun transitionTo(newState: PassScheduleState, calTime: ZonedDateTime) {
    val ctx = currentContext

    logger.info("═══════════════════════════════════════════════")
    logger.info("[TRANSITION] $currentState → $newState")
    logger.info("  - 스케줄: ${ctx?.satelliteName} (mstId: ${ctx?.mstId})")
    logger.info("  - calTime: $calTime")
    logger.info("═══════════════════════════════════════════════")

    // 이전 상태 저장
    previousState = currentState
    currentState = newState

    // ⚠️ 진입 시간 기록 (calTime 기준 - ZonedDateTime)
    ctx?.stateEntryTime = calTime

    // 진입 액션 실행
    executeEnterAction(newState, ctx, calTime)

    // 프론트엔드 상태 전송
    sendStateToFrontend(newState, ctx)
}
```

### 4.6 상태 진입 액션

```kotlin
/**
 * 상태 진입 시 1회 실행되는 액션
 *
 * @param state 새 상태
 * @param ctx 현재 스케줄 컨텍스트
 * @param calTime 현재 calTime (ZonedDateTime)
 */
private fun executeEnterAction(
    state: PassScheduleState,
    ctx: ScheduleTrackingContext?,
    calTime: ZonedDateTime
) {
    when (state) {
        PassScheduleState.STOWING -> {
            if (ctx?.stowCommandSent != true) {
                logger.info("[ACTION] Stow 명령 전송")
                udpFwICDService.StowCommand()
                ctx?.stowCommandSent = true
            }
        }

        PassScheduleState.STOWED -> {
            logger.info("[ACTION] Stow 위치 도달, 대기 시작")
        }

        PassScheduleState.MOVING_TRAIN -> {
            if (ctx != null && !ctx.trainMoveCommandSent) {
                logger.info("[ACTION] Train 이동 명령: ${Math.toDegrees(ctx.trainAngle.toDouble())}°")
                val trainDeg = Math.toDegrees(ctx.trainAngle.toDouble()).toFloat()
                udpFwICDService.moveToStartPosition(0f, 0f, trainDeg)
                ctx.trainMoveCommandSent = true
            }
        }

        PassScheduleState.TRAIN_STABILIZING -> {
            // ⚠️ calTime 기준으로 안정화 시작 시간 기록 (ZonedDateTime)
            ctx?.trainStabilizationStartTime = calTime
            logger.info("[ACTION] Train 안정화 시작 (3초 대기, calTime 기준)")
        }

        PassScheduleState.MOVING_TO_START -> {
            if (ctx != null && !ctx.azElMoveCommandSent) {
                val azDeg = Math.toDegrees(ctx.startAzimuth.toDouble()).toFloat()
                val elDeg = Math.toDegrees(ctx.startElevation.toDouble()).toFloat()
                logger.info("[ACTION] Az/El 이동 명령: Az=$azDeg°, El=$elDeg°")
                udpFwICDService.moveToStartPosition(azDeg, elDeg, null)
                ctx.azElMoveCommandSent = true
            }
        }

        PassScheduleState.READY -> {
            if (ctx != null && !ctx.headerSent) {
                logger.info("[ACTION] 헤더 전송 준비 완료")
                sendHeaderTrackingData(ctx.mstId)
                ctx.headerSent = true
            }
        }

        PassScheduleState.TRACKING -> {
            // ⚠️ 상태 점프 대응: calTime이 추적 범위로 점프한 경우
            //    Train/Az/El 준비가 완료되지 않았더라도 플래그 강제 완료
            if (ctx != null) {
                if (!ctx.trainMoveCompleted) {
                    logger.warn("[ACTION] ⚡ 상태 점프로 인해 Train 이동 강제 완료 처리")
                    ctx.trainMoveCompleted = true
                    ctx.trainStabilizationCompleted = true
                }
                if (!ctx.azElMoveCompleted) {
                    logger.warn("[ACTION] ⚡ 상태 점프로 인해 Az/El 이동 강제 완료 처리")
                    ctx.azElMoveCompleted = true
                }

                if (!ctx.initialTrackingDataSent) {
                    logger.info("[ACTION] 추적 시작 - 초기 데이터 전송")
                    sendInitialTrackingData(ctx.mstId)
                    ctx.initialTrackingDataSent = true
                }
            }
        }

        PassScheduleState.POST_TRACKING -> {
            logger.info("[ACTION] 추적 종료 - 다음 스케줄 평가 중")
            // 다음 스케줄 평가 및 상태 전환은 evaluateNextSchedule()에서 처리
        }

        PassScheduleState.COMPLETED -> {
            logger.info("[ACTION] 모든 스케줄 완료 - Stow 이동")
            udpFwICDService.StowCommand()
        }

        PassScheduleState.ERROR -> {
            logger.error("[ACTION] 오류 상태 진입")
            // ERROR 상태에서는 안전을 위해 Stow로 이동
            udpFwICDService.StowCommand()
        }

        else -> {}
    }
}
```

### 4.7 주기적 액션 (추적 데이터 전송)

```kotlin
/**
 * 매 100ms마다 실행되는 주기적 액션
 *
 * @param calTime 현재 calTime (ZonedDateTime)
 */
private fun executePeriodicAction(calTime: ZonedDateTime) {
    when (currentState) {
        PassScheduleState.TRACKING -> {
            val ctx = currentContext ?: return

            // 추적 데이터 전송 (기존 로직 유지)
            // calTime을 epoch millis로 변환하여 데이터 조회
            val calTimeEpoch = calTime.toInstant().toEpochMilli()
            val trackingData = getTrackingDataForTime(ctx.mstId, calTimeEpoch)
            if (trackingData != null) {
                udpFwICDService.sendTrackingCommand(trackingData)
            }
        }

        PassScheduleState.POST_TRACKING -> {
            // POST_TRACKING 상태에서 다음 스케줄 평가
            val nextState = evaluateNextSchedule(calTime)
            if (nextState != currentState) {
                transitionTo(nextState, calTime)
            }
        }

        else -> {}
    }
}
```

---

## 5. 프론트엔드 연동

### 5.1 상태 전송 인터페이스

```kotlin
/**
 * 프론트엔드로 상태 전송
 */
private fun sendStateToFrontend(state: PassScheduleState, ctx: ScheduleTrackingContext?) {
    val stateMessage = PassScheduleStateMessage(
        state = state.name,
        currentMstId = ctx?.mstId,
        currentDetailId = ctx?.detailId,
        nextMstId = nextContext?.mstId,
        nextDetailId = nextContext?.detailId,
        timestamp = System.currentTimeMillis()
    )

    webSocketHandler.sendToAll(stateMessage)
}

data class PassScheduleStateMessage(
    val state: String,
    val currentMstId: Long?,
    val currentDetailId: Int?,
    val nextMstId: Long?,
    val nextDetailId: Int?,
    val timestamp: Long
)
```

### 5.2 프론트엔드 상태 처리 (icdStore.ts)

```typescript
// 상태별 색상 매핑
export const getScheduleRowColor = (
  state: string,
  mstId: number,
  detailId: number,
  currentMstId: number | null,
  currentDetailId: number | null,
  nextMstId: number | null,
  nextDetailId: number | null
): 'green' | 'blue' | 'default' => {

  // 현재 추적 또는 준비 중인 스케줄 (녹색)
  const preparingStates = ['MOVING_TRAIN', 'TRAIN_STABILIZING', 'MOVING_TO_START', 'READY', 'TRACKING'];
  if (preparingStates.includes(state) && mstId === currentMstId && detailId === currentDetailId) {
    return 'green';
  }

  // 다음 대기 중인 스케줄 (파란색)
  if (mstId === nextMstId && detailId === nextDetailId) {
    return 'blue';
  }

  return 'default';
};
```

### 5.3 PassSchedulePage.vue 하이라이트 로직

```vue
<script setup lang="ts">
import { computed, watch } from 'vue';
import { useIcdStore } from '@/stores/icd/icdStore';

const icdStore = useIcdStore();

// 하이라이트 상태 computed
const scheduleHighlight = computed(() => ({
  state: icdStore.passScheduleState,
  currentMstId: icdStore.currentTrackingMstId,
  currentDetailId: icdStore.currentTrackingDetailId,
  nextMstId: icdStore.nextTrackingMstId,
  nextDetailId: icdStore.nextTrackingDetailId
}));

// 행 색상 결정 함수
const getRowClass = (row: ScheduleItem): string => {
  const { state, currentMstId, currentDetailId, nextMstId, nextDetailId } = scheduleHighlight.value;

  const color = getScheduleRowColor(
    state,
    row.mstId,
    row.detailId,
    currentMstId,
    currentDetailId,
    nextMstId,
    nextDetailId
  );

  switch (color) {
    case 'green':
      return 'highlight-current-schedule';
    case 'blue':
      return 'highlight-next-schedule';
    default:
      return '';
  }
};
</script>

<style scoped>
.highlight-current-schedule {
  background-color: #c8e6c9 !important;  /* 녹색 */
}

.highlight-next-schedule {
  background-color: #e3f2fd !important;  /* 파란색 */
}
</style>
```

---

## 6. 시작/정지 API

### 6.1 startScheduleTracking

```kotlin
/**
 * 스케줄 추적 시작
 *
 * 1. 스케줄 큐 생성
 * 2. 첫 스케줄 선택
 * 3. 상태 머신 시작
 */
fun startScheduleTracking(): Mono<Boolean> {
    return mono {
        try {
            logger.info("════════════════════════════════════════")
            logger.info("[START] 스케줄 추적 시작")
            logger.info("════════════════════════════════════════")

            val calTime = GlobalData.Time.calUtcTimeOffsetTime

            // 1. 스케줄 큐 생성
            scheduleQueue.clear()
            val allSchedules = buildScheduleQueue(calTime)
            scheduleQueue.addAll(allSchedules)

            if (scheduleQueue.isEmpty()) {
                logger.warn("[START] 추적 가능한 스케줄 없음")
                return@mono false
            }

            logger.info("[START] ${scheduleQueue.size}개 스케줄 로드됨")
            scheduleQueue.forEach { ctx ->
                logger.info("  - ${ctx.satelliteName}: ${formatTime(ctx.startTime)} ~ ${formatTime(ctx.endTime)}")
            }

            // 2. 첫 스케줄 선택
            currentContext = scheduleQueue.first()
            nextContext = scheduleQueue.getOrNull(1)

            // 3. 초기 상태 결정
            val timeToStart = currentContext!!.startTime - calTime
            val initialState = if (timeToStart <= PREPARATION_TIME_MS) {
                PassScheduleState.MOVING_TRAIN
            } else {
                PassScheduleState.STOWING
            }

            // 4. 상태 전환
            transitionTo(initialState, calTime)

            // 5. 타이머 시작
            startCheckTimer()

            true
        } catch (e: Exception) {
            logger.error("[START] 시작 실패: ${e.message}", e)
            false
        }
    }
}
```

### 6.2 stopScheduleTracking

```kotlin
/**
 * 스케줄 추적 정지
 *
 * ⚠️ safeBatchShutdown()을 사용하여 안전한 일괄 종료 수행
 */
fun stopScheduleTracking(): Mono<Boolean> {
    return mono {
        try {
            logger.info("════════════════════════════════════════")
            logger.info("[STOP] 스케줄 추적 정지")
            logger.info("════════════════════════════════════════")

            // 1. 타이머 정지 (먼저 정지하여 추가 명령 방지)
            stopCheckTimer()

            // 2. 안전한 일괄 종료 (진행 중인 작업 정리 + Stow)
            safeBatchShutdown()

            // 3. 상태 초기화
            currentState = PassScheduleState.IDLE
            previousState = PassScheduleState.IDLE

            // 4. 컨텍스트 초기화
            currentContext = null
            nextContext = null
            scheduleQueue.clear()

            // 5. 프론트엔드 알림
            sendStateToFrontend(PassScheduleState.IDLE, null)

            true
        } catch (e: Exception) {
            logger.error("[STOP] 정지 실패: ${e.message}", e)
            false
        }
    }
}
```

### 6.3 Time Offset 변경 감지

```kotlin
/**
 * Time Offset 변경 시 호출되는 핸들러
 *
 * ⚠️ GlobalData.Time.calUtcTimeOffsetTime이 변경될 때마다 호출
 *
 * Time Offset이 변경되면:
 * 1. 스케줄 큐 재평가 (완료된 스케줄이 다시 미래가 될 수 있음)
 * 2. 현재 상태 재결정 (즉시 TRACKING으로 점프 가능)
 * 3. 프론트엔드에 상태 동기화
 */
fun handleTimeOffsetChange() {
    if (currentState == PassScheduleState.IDLE) {
        return  // IDLE 상태에서는 처리 불필요
    }

    val calTime = GlobalData.Time.calUtcTimeOffsetTime

    logger.info("═══════════════════════════════════════════════")
    logger.info("[TIME_OFFSET] Time Offset 변경 감지!")
    logger.info("  - 새 calTime: $calTime")
    logger.info("  - 현재 상태: $currentState")
    logger.info("═══════════════════════════════════════════════")

    // 1. 스케줄 큐 재평가
    reevaluateScheduleQueue(calTime)

    // 2. 현재 상태 재결정 (시간 기반)
    val newState = determineStateByTime(calTime)
    if (newState != currentState) {
        logger.info("[TIME_OFFSET] 상태 전환: $currentState → $newState")
        transitionTo(newState, calTime)
    }
}

/**
 * Time Offset 변경 시 스케줄 큐 재평가
 *
 * 과거로 시간이 이동하면 완료된 스케줄이 다시 활성화될 수 있음
 */
private fun reevaluateScheduleQueue(calTime: ZonedDateTime) {
    // 현재 calTime 기준으로 아직 종료되지 않은 스케줄 필터링
    val activeSchedules = scheduleQueue.filter { it.endTime.isAfter(calTime) }

    if (activeSchedules.isEmpty() && scheduleQueue.isNotEmpty()) {
        // 모든 스케줄이 과거로 갔다면 원본 큐에서 재조회
        logger.warn("[TIME_OFFSET] 스케줄 큐 재구성 필요")
        // 필요시 DB에서 재조회하거나 원본 데이터로 복원
    }

    // 현재/다음 컨텍스트 재설정
    val currentSchedule = activeSchedules
        .filter { it.startTime.isBefore(calTime) || it.startTime.isAfter(calTime.minusMinutes(2)) }
        .minByOrNull { it.startTime }

    if (currentSchedule != null && currentSchedule.mstId != currentContext?.mstId) {
        logger.info("[TIME_OFFSET] 현재 스케줄 변경: ${currentContext?.satelliteName} → ${currentSchedule.satelliteName}")
        currentContext = currentSchedule.resetFlags()
    }
}
```

### 6.4 안전한 일괄 종료 (safeBatchShutdown)

```kotlin
/**
 * 안전한 일괄 종료
 *
 * STOP 명령 시 모든 진행 중인 작업을 안전하게 종료:
 * 1. 현재 추적 데이터 전송 중지
 * 2. 헤더 전송 취소 (진행 중인 경우)
 * 3. 이동 명령 취소 (진행 중인 경우)
 * 4. Stow로 안전하게 이동
 */
private fun safeBatchShutdown() {
    logger.info("[SHUTDOWN] 일괄 종료 시작")

    try {
        // 1. 추적 데이터 전송 중지 플래그 설정
        // (100ms 타이머에서 이 플래그 확인하여 전송 중지)
        isShuttingDown = true

        // 2. 현재 이동 중이면 정지 명령
        // ACU에 정지 명령 전송 (구현에 따라)
        // udpFwICDService.stopMovement()

        // 3. 안전 지연 후 Stow 이동
        //    이동 중인 상태에서 바로 Stow 명령 시 문제 가능성
        //    짧은 지연 후 Stow 명령 전송
        kotlinx.coroutines.delay(100)

        // 4. Stow 명령 전송
        udpFwICDService.StowCommand()

        logger.info("[SHUTDOWN] 일괄 종료 완료, Stow 이동 시작")
    } catch (e: Exception) {
        logger.error("[SHUTDOWN] 일괄 종료 중 오류: ${e.message}", e)
        // 오류 발생해도 Stow는 시도
        try {
            udpFwICDService.StowCommand()
        } catch (stowError: Exception) {
            logger.error("[SHUTDOWN] Stow 명령 실패: ${stowError.message}", stowError)
        }
    } finally {
        isShuttingDown = false
    }
}

// 종료 중 플래그 (헤더에 추가 필요)
private var isShuttingDown: Boolean = false
```

### 6.5 ERROR 상태 복구

```kotlin
/**
 * ERROR 상태에서 복구 시도
 *
 * ERROR 상태 진입 조건:
 * - 하드웨어 통신 오류
 * - 데이터 조회 실패
 * - 예기치 않은 예외
 *
 * 복구 옵션:
 * 1. 자동 복구 (일시적 오류인 경우)
 * 2. IDLE로 전환 (사용자 재시작 필요)
 */
private fun handleErrorRecovery(calTime: ZonedDateTime) {
    if (currentState != PassScheduleState.ERROR) return

    val ctx = currentContext ?: return

    // ERROR 진입 후 경과 시간 확인
    val errorEntryTime = ctx.stateEntryTime ?: return
    val elapsed = Duration.between(errorEntryTime, calTime)

    // 5초 후 자동 복구 시도
    if (elapsed.seconds >= 5) {
        logger.info("[ERROR_RECOVERY] 자동 복구 시도 중...")

        // 통신 상태 확인
        val isCommOk = checkCommunicationStatus()

        if (isCommOk) {
            // 복구 가능: 현재 시간 기반으로 상태 재결정
            val recoveryState = determineStateByTime(calTime)
            logger.info("[ERROR_RECOVERY] 복구 성공, $recoveryState 상태로 전환")
            transitionTo(recoveryState, calTime)
        } else {
            // 복구 불가: IDLE로 전환하고 사용자에게 알림
            if (elapsed.seconds >= 30) {
                logger.error("[ERROR_RECOVERY] 30초 동안 복구 실패, IDLE로 전환")
                stopScheduleTracking()
                sendErrorNotificationToFrontend("스케줄 추적 오류: 통신 실패로 자동 정지됨")
            }
        }
    }
}

private fun checkCommunicationStatus(): Boolean {
    // ACU 통신 상태 확인 (구현에 따라)
    return GlobalData.ACU.connectionStatus == ConnectionStatus.CONNECTED
}

private fun sendErrorNotificationToFrontend(message: String) {
    val errorMessage = mapOf(
        "type" to "PASS_SCHEDULE_ERROR",
        "message" to message,
        "timestamp" to System.currentTimeMillis()
    )
    webSocketHandler.sendToAll(errorMessage)
}
```

---

## 7. 위치 판정 함수

### 7.1 Stow 위치 확인

```kotlin
private fun isAtStowPosition(): Boolean {
    val currentAz = GlobalData.ACU.Antenna.azCurrent ?: return false
    val currentEl = GlobalData.ACU.Antenna.elCurrent ?: return false

    val stowAz = 0f  // Stow 방위각
    val stowEl = 0f  // Stow 고도각 (또는 설정값)

    val azDiff = abs(currentAz - stowAz)
    val elDiff = abs(currentEl - stowEl)

    return azDiff < POSITION_TOLERANCE_RAD && elDiff < POSITION_TOLERANCE_RAD
}
```

### 7.2 Train 위치 확인

```kotlin
private fun isTrainAtTarget(targetTrain: Float): Boolean {
    val currentTrain = GlobalData.ACU.Antenna.tiltCurrent ?: return false
    return abs(currentTrain - targetTrain) < POSITION_TOLERANCE_RAD
}
```

### 7.3 Az/El 위치 확인

```kotlin
private fun isAzElAtTarget(targetAz: Float, targetEl: Float): Boolean {
    val currentAz = GlobalData.ACU.Antenna.azCurrent ?: return false
    val currentEl = GlobalData.ACU.Antenna.elCurrent ?: return false

    val azDiff = abs(currentAz - targetAz)
    val elDiff = abs(currentEl - targetEl)

    return azDiff < POSITION_TOLERANCE_RAD && elDiff < POSITION_TOLERANCE_RAD
}
```

---

## 8. 상태 전환 다이어그램 (최종)

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        PassSchedule 상태 전환 다이어그램                   │
└──────────────────────────────────────────────────────────────────────────┘

                              [START 버튼]
                                   │
                                   ▼
                               ┌──────┐
                               │ IDLE │
                               └──┬───┘
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
              2분 이상 남음                 2분 이내
                    │                           │
                    ▼                           ▼
              ┌─────────┐               ┌──────────────┐
              │ STOWING │               │ MOVING_TRAIN │◄────────────┐
              └────┬────┘               └──────┬───────┘             │
                   │                           │                     │
           Stow 도달                    Train 도달                   │
                   │                           │                     │
                   ▼                           ▼                     │
              ┌────────┐               ┌──────────────────┐          │
              │ STOWED │               │ TRAIN_STABILIZING│          │
              └────┬───┘               └────────┬─────────┘          │
                   │                            │                    │
            2분 이내 진입                    3초 경과                 │
                   │                            │                    │
                   └──────────┐                 ▼                    │
                              │        ┌────────────────┐            │
                              │        │ MOVING_TO_START│            │
                              │        └───────┬────────┘            │
                              │                │                     │
                              │         위치 도달                    │
                              │                │                     │
                              │                ▼                     │
                              │          ┌─────────┐                 │
                              └─────────►│  READY  │                 │
                                         └────┬────┘                 │
                                              │                      │
                                       시작 시간 도달                │
                                              │                      │
                                              ▼                      │
                                        ┌──────────┐                 │
                                        │ TRACKING │                 │
                                        └─────┬────┘                 │
                                              │                      │
                                       종료 시간 도달                │
                                              │                      │
                                              ▼                      │
                                     ┌───────────────┐               │
                                     │ POST_TRACKING │               │
                                     └───────┬───────┘               │
                                             │                       │
                       ┌─────────────────────┼─────────────────────┐ │
                       │                     │                     │ │
                  2분 이상             2분 이내               없음  │
                       │                     │                     │ │
                       ▼                     │                     ▼ │
                 ┌─────────┐                 │               ┌───────────┐
                 │ STOWING │                 └───────────────┤ COMPLETED │
                 └─────────┘                                 └───────────┘

```

---

## 9. 테스트 시나리오

### 9.1 정상 시나리오

| # | 시나리오 | 초기 조건 | 예상 결과 |
|---|---------|----------|----------|
| 1 | 단일 스케줄 2분 이내 시작 | 스케줄 A: 1분 후 시작 | IDLE → MOVING_TRAIN → ... → TRACKING → COMPLETED |
| 2 | 단일 스케줄 2분 이상 대기 | 스케줄 A: 5분 후 시작 | IDLE → STOWING → STOWED → MOVING_TRAIN → ... |
| 3 | 다중 스케줄 연속 | A: 5분, B: 15분 후 | A 추적 → POST_TRACKING → B 대기 → B 추적 |
| 4 | 다중 스케줄 연속 (2분 이내) | A: 5분, B: 7분 후 | A 종료 직후 B MOVING_TRAIN 시작 |

### 9.2 예외 시나리오

| # | 시나리오 | 초기 조건 | 예상 결과 |
|---|---------|----------|----------|
| 1 | 추적 가능 스케줄 없음 | 빈 스케줄 | IDLE 유지, 에러 메시지 |
| 2 | 위치 이동 타임아웃 | Az/El 도달 불가 | 2분 후 READY로 강제 전환 |
| 3 | 중간 정지 | 추적 중 STOP | 즉시 IDLE + Stow |

---

## 10. 마이그레이션 계획

### 10.1 단계별 진행

1. **1단계**: 새 상태 열거형 및 컨텍스트 클래스 추가
2. **2단계**: 새 상태 머신 로직 구현 (병렬 유지)
3. **3단계**: 기존 로직 비활성화, 새 로직 활성화
4. **4단계**: 프론트엔드 상태 처리 업데이트
5. **5단계**: 테스트 및 검증
6. **6단계**: 기존 코드 제거

### 10.2 롤백 계획

- 기존 코드는 즉시 복원 가능하도록 주석 처리
- Feature flag로 신/구 로직 전환 가능

---

## 관련 문서

- 분석 문서: [ANALYSIS.md](./ANALYSIS.md)
- 백엔드: `backend/src/main/kotlin/.../service/mode/PassScheduleService.kt`
- 프론트엔드: `frontend/src/pages/mode/PassSchedulePage.vue`
