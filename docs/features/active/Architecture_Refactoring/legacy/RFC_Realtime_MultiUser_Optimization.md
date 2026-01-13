# RFC: 실시간성 및 다중 사용자 최적화

> **RFC (Request for Comments)** - 고도화 리팩토링 제안서
> **상위 문서**: [Architecture_Refactoring_plan.md](./Architecture_Refactoring_plan.md)

---

## 개요

| 항목 | 내용 |
|------|------|
| **목표** | 실시간 안테나 제어 시스템의 고도화 |
| **핵심 가치** | 실시간성, 다중 사용자 지원, 확장성 |
| **우선순위** | P1 (핵심 인프라) |
| **상태** | 검토 중 |
| **작성일** | 2026-01-07 |

---

## 1. 현재 아키텍처 분석

### 1.1 잘 설계된 부분 (유지)

| 구성 요소 | 설명 | 평가 |
|----------|------|------|
| **ThreadManager** | 하드웨어 자동 감지, 성능 등급별 스레드 풀 관리 | ✅ 우수 |
| **실시간 파이프라인** | UDP(10ms) → BE → WebSocket(30ms) → FE | ✅ 우수 |
| **우선순위 체계** | CRITICAL > HIGH > NORMAL > LOW | ✅ 우수 |
| **이벤트 버스** | ACSEventBus - 비동기 이벤트 발행/구독 | ✅ 우수 |
| **계층 분리** | Controller → Service → Algorithm | ✅ 양호 |

### 1.2 개선이 필요한 부분

| 영역 | 현재 상태 | 문제점 | 개선 방향 |
|------|----------|--------|----------|
| **대형 서비스** | 4,986줄 (EphemerisService) | 단일 책임 위반 | 도메인별 분해 |
| **캐싱 부재** | Orekit 계산 매번 실행 | CPU 낭비 | 결과 캐싱 |
| **다중 사용자** | 세션 분리 미흡 | 상태 충돌 가능 | 세션 격리 |
| **모니터링** | 성능 지표 부족 | 병목 파악 어려움 | 메트릭 수집 |

---

## 2. 실시간성 최적화

### 2.1 데이터 흐름 현황

```
[하드웨어] ─UDP 10ms─> [UdpFwICDService] ─> [ICDService] ─> [PushDataService]
                                                                    │
                                                           WebSocket 30ms
                                                                    ↓
                                                            [Frontend icdStore]
```

**현재 성능:**
- UDP 수신: 10ms 주기 (100 TPS)
- WebSocket 전송: 30ms 주기 (33 TPS)
- 총 지연: ~40-50ms

### 2.2 캐싱 전략

#### 2.2.1 현재 아키텍처: DataStoreService RAM 버퍼링 (이미 구현됨)

**현재 시스템은 이미 효과적인 실시간 캐싱을 사용 중:**

```kotlin
// DataStoreService.kt - 현재 구현
@Service
class DataStoreService {
    // AtomicReference로 최신 데이터 버퍼링 (이미 효과적인 RAM 캐시)
    private val latestData = AtomicReference(PushData.ReadData())

    // UDP 10ms 주기로 업데이트
    fun updateDataFromUdp(newData: PushData.ReadData, forceUpdate: Boolean = false) {
        latestData.set(mergedData)  // 항상 최신 데이터 유지
    }

    // WebSocket 30ms 주기로 조회
    fun getLatestData(): PushData.ReadData {
        return latestData.get()  // 즉시 반환 (O(1))
    }
}
```

**현재 데이터 흐름:**
```
UDP (10ms) → DataStoreService.updateDataFromUdp() → AtomicReference
                                                           ↓
WebSocket (30ms) ← PushDataController.generateAndBroadcastData() ← getLatestData()
```

**이 방식의 장점:**
- TTL 없음: 항상 최신 데이터 (10ms 이내)
- 무잠금 읽기: AtomicReference.get()은 블로킹 없음
- 다중 사용자 지원: 모든 클라이언트가 동일 데이터 공유

#### 2.2.2 캐싱 적용 가이드라인

| 데이터 유형 | 캐싱 | TTL | 이유 |
|------------|------|-----|------|
| 실시간 위치 (추적 중) | ❌ 금지 | - | 100ms 갱신 필수 |
| 실시간 상태 데이터 | ✅ RAM 버퍼 | 10ms (UDP 주기) | DataStoreService가 처리 |
| TLE 목록 | ✅ 권장 | 5분 | 자주 변경 안됨 |
| 설정/설정값 | ✅ 권장 | 변경 시 무효화 | 변경 빈도 낮음 |
| API 응답 (조회) | ✅ 권장 | 1-5분 | 부하 감소 |

**⚠️ 주의: Orekit 위성 위치 계산 캐싱**

```kotlin
// ❌ 잘못된 예: 1초 TTL (추적 중 100ms 요구사항 위반)
private val cache = CacheBuilder.newBuilder()
    .expireAfterWrite(1, TimeUnit.SECONDS)  // 추적 모드에서 사용 금지
    .build<CacheKey, SatellitePosition>()

// ✅ 올바른 예: 다중 사용자 최적화 (동일 100ms 구간 내에서만 공유)
private val cache = CacheBuilder.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(100, TimeUnit.MILLISECONDS)  // 추적 주기에 맞춤
    .build<CacheKey, SatellitePosition>()

fun calculatePosition(tle: TLE, time: Instant): SatellitePosition {
    // 100ms 단위로 시간 정규화 → 같은 100ms 구간의 다중 요청 공유
    val key = CacheKey(tle.noradId, time.truncatedTo(100, ChronoUnit.MILLIS))
    return cache.get(key) { orekitCalculator.calculate(tle, time) }
}
```

#### 2.2.3 API 응답 캐싱 (권장)

```kotlin
// Spring Cache 적용 - 실시간 데이터가 아닌 경우만
@Cacheable(value = ["tleList"], key = "#root.methodName")
suspend fun getTLEList(): List<TLEInfo> {
    // 5분 TTL - TLE는 자주 변경되지 않음
}

@Cacheable(value = ["settings"], key = "#category")
suspend fun getSettings(category: String): Settings {
    // 설정은 변경 시에만 갱신
}

// ❌ 실시간 데이터에는 사용 금지
// @Cacheable  // 사용하면 안됨
suspend fun getCurrentPosition(): SatellitePosition {
    // 실시간 추적 데이터
}
```

### 2.3 WebSocket 최적화

> **✅ 검토 완료 (2026-01-07)**: 아래 최적화는 현재 불필요. 성능 문제 발생 시 재검토.
> - 현재 30ms 고정 주기가 안정적으로 동작 중
> - 10명 이하 클라이언트, LAN 환경에서 2-5KB 전송은 부담 없음
> - 복잡성 추가로 인한 불안정 위험 > 최적화 이득

#### 2.3.1 메시지 압축 *(보류)*

```kotlin
// 현재: 전체 상태 전송 (2-5KB)
data class PushData(
    val time: TimeData,
    val angles: AngleData,
    val speeds: SpeedData,
    val status: StatusData,
    // ... 전체 필드
)

// 개선: 변경분만 전송 (Delta Compression)
data class DeltaPushData(
    val changes: Map<String, Any>,  // 변경된 필드만
    val sequence: Long              // 시퀀스 번호
)

@Service
class DeltaCompressor {
    private var lastState: PushData? = null

    fun compress(current: PushData): DeltaPushData {
        val changes = mutableMapOf<String, Any>()
        lastState?.let { prev ->
            if (current.angles != prev.angles) changes["angles"] = current.angles
            if (current.speeds != prev.speeds) changes["speeds"] = current.speeds
            // ... 변경 감지
        } ?: run {
            // 첫 전송은 전체 상태
            return DeltaPushData(mapOf("full" to current), 0)
        }
        lastState = current
        return DeltaPushData(changes, System.currentTimeMillis())
    }
}
```

**예상 효과:**
- 메시지 크기 70% 감소 (2-5KB → 0.5-1KB)
- 네트워크 대역폭 절약
- 프론트엔드 파싱 부하 감소

#### 2.3.2 적응형 업데이트 주기 *(보류)*

```kotlin
// 상황에 따라 업데이트 주기 조정
object AdaptiveUpdateConfig {
    fun getUpdateInterval(mode: ACSMode, clientCount: Int): Long {
        return when {
            mode == ACSMode.TRACKING && clientCount <= 3 -> 30   // 추적 중 + 적은 클라이언트
            mode == ACSMode.TRACKING -> 50                       // 추적 중 + 많은 클라이언트
            mode == ACSMode.STANDBY -> 500                       // 대기 모드
            else -> 100                                          // 기본값
        }
    }
}
```

---

## 3. 다중 사용자 지원

### 3.1 설계 원칙

```
┌─────────────────────────────────────────────────────────────┐
│                  동일 화면 공유 모델                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [Viewer]   ──┐                                             │
│  [Viewer]   ──┼──> 동일한 안테나 상태 브로드캐스트 수신        │
│  [Operator] ──┤    (세션별 데이터 분리 불필요)                │
│  [Admin]    ──┘                                             │
│                                                             │
│  ✅ 브로드캐스트 방식 유지 - 모두 같은 안테나를 봄            │
│  ✅ 제어권만 배타적 관리 - 1명만 제어 가능                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 역할 및 제어권 정책

| 역할 | 설명 | 제어권 획득 | 접속 방식 |
|------|------|------------|----------|
| **VIEWER** | 관찰자 | ❌ 불가 | 로컬/원격 모두 가능 |
| **OPERATOR** | 제어권한자 | ✅ 로컬 + API | 로컬/원격 모두 가능 |
| **ADMIN** | 관리자 | ✅ 로컬만 | localhost 필수 |

**제어권 규칙:**
- 제어권은 **1명만** 보유 가능 (배타적)
- 제어권을 **해제해야** 다른 사람이 획득 가능
- 제어권 보유자가 연결 해제 시 자동 해제

### 3.3 제어권 상태 표시 (LED)

```
┌─────────────────────────────────────────────────────────────┐
│                    제어권 LED 표시                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ⚠️ 항상 1개만 표시됨 (제어권은 1명만 보유 가능)              │
│                                                             │
│  ⚫ IDLE    - 제어권 없음 (누구도 제어 중 아님)              │
│  🟢 LOCAL   - 로컬 제어 중 (localhost 접속자가 제어권 보유)  │
│  🔵 REMOTE  - 원격 제어 중 (API 접속자가 제어권 보유)        │
│                                                             │
│  상황별 표시 (3가지 중 택1):                                 │
│                                                             │
│  ┌────────────────────────────────┐                        │
│  │ ⚫ IDLE                        │  ← 아무도 제어 안함     │
│  └────────────────────────────────┘                        │
│                       또는                                  │
│  ┌────────────────────────────────┐                        │
│  │ 🟢 LOCAL  운용자1 제어 중       │  ← 로컬에서 제어 중    │
│  └────────────────────────────────┘                        │
│                       또는                                  │
│  ┌────────────────────────────────┐                        │
│  │ 🔵 REMOTE 운용자2 제어 중       │  ← 원격에서 제어 중    │
│  └────────────────────────────────┘                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.4 Backend 구현

```kotlin
/**
 * 제어권 접속 유형
 */
enum class ControlAccessType {
    LOCAL,   // localhost 접속
    REMOTE   // 원격 접속 (API)
}

/**
 * 제어권 보유자 정보
 */
data class ControlHolder(
    val sessionId: String,
    val role: SessionRole,
    val displayName: String,
    val accessType: ControlAccessType,  // LOCAL or REMOTE
    val acquiredAt: Instant
)

/**
 * 브로드캐스트용 제어권 정보
 */
data class ControlInfo(
    val hasController: Boolean,
    val accessType: ControlAccessType?,  // LED 표시용: LOCAL/REMOTE
    val controllerName: String?,
    val controllerRole: String?,
    val acquiredAt: Instant?
)

@Service
class ControlLockService {
    private val controlLock = AtomicReference<ControlHolder?>(null)

    /**
     * 제어권 요청
     */
    fun requestControl(
        sessionId: String,
        role: SessionRole,
        displayName: String,
        isLocal: Boolean
    ): ControlResult {
        // VIEWER는 제어권 요청 불가
        if (role == SessionRole.VIEWER) {
            return ControlResult.InsufficientPermission
        }

        // ADMIN은 로컬 접속만 가능
        if (role == SessionRole.ADMIN && !isLocal) {
            return ControlResult.AdminRequiresLocal
        }

        val accessType = if (isLocal) ControlAccessType.LOCAL else ControlAccessType.REMOTE
        val holder = ControlHolder(sessionId, role, displayName, accessType, Instant.now())

        // 원자적 제어권 획득 시도
        return if (controlLock.compareAndSet(null, holder)) {
            broadcastControlChange()  // 모든 클라이언트에게 LED 상태 알림
            ControlResult.Granted(accessType)
        } else {
            val current = controlLock.get()!!
            ControlResult.LockedBy(current.displayName, current.accessType)
        }
    }

    /**
     * 제어권 해제
     */
    fun releaseControl(sessionId: String): Boolean {
        val current = controlLock.get()
        if (current?.sessionId == sessionId) {
            controlLock.set(null)
            broadcastControlChange()  // LED 상태 변경 알림
            return true
        }
        return false
    }

    /**
     * 명령 실행 가능 여부
     */
    fun canExecuteCommand(sessionId: String): Boolean {
        return controlLock.get()?.sessionId == sessionId
    }

    /**
     * 현재 제어권 정보 (브로드캐스트 데이터에 포함)
     */
    fun getControlInfo(): ControlInfo {
        val holder = controlLock.get()
        return ControlInfo(
            hasController = holder != null,
            accessType = holder?.accessType,
            controllerName = holder?.displayName,
            controllerRole = holder?.role?.name,
            acquiredAt = holder?.acquiredAt
        )
    }
}

sealed class ControlResult {
    data class Granted(val accessType: ControlAccessType) : ControlResult()
    object InsufficientPermission : ControlResult()
    object AdminRequiresLocal : ControlResult()
    data class LockedBy(val name: String, val accessType: ControlAccessType) : ControlResult()
}
```

### 3.5 Frontend 제어권 LED 컴포넌트

```vue
<!-- components/common/ControlStatusLED.vue -->
<template>
  <div class="control-led-container">
    <!-- LED 표시 -->
    <q-chip
      :color="ledColor"
      :text-color="textColor"
      :icon="ledIcon"
    >
      <span class="led-label">{{ statusLabel }}</span>
    </q-chip>

    <!-- 제어자 정보 -->
    <span v-if="controlInfo.hasController" class="controller-info">
      {{ controlInfo.controllerName }} 제어 중
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useIcdStore } from '@/stores/icdStore'

const icdStore = useIcdStore()
const controlInfo = computed(() => icdStore.controlInfo)

const ledColor = computed(() => {
  if (!controlInfo.value.hasController) return 'grey-6'
  return controlInfo.value.accessType === 'LOCAL' ? 'green' : 'blue'
})

const textColor = computed(() => {
  if (!controlInfo.value.hasController) return 'white'
  return 'white'
})

const ledIcon = computed(() => {
  if (!controlInfo.value.hasController) return 'radio_button_unchecked'
  return controlInfo.value.accessType === 'LOCAL' ? 'computer' : 'wifi'
})

const statusLabel = computed(() => {
  if (!controlInfo.value.hasController) return 'IDLE'
  return controlInfo.value.accessType  // 'LOCAL' or 'REMOTE'
})
</script>

<style scoped>
.control-led-container {
  display: flex;
  align-items: center;
  gap: 8px;
}
.led-label {
  font-weight: bold;
  font-size: 12px;
}
.controller-info {
  font-size: 12px;
  color: var(--theme-text-secondary);
}
</style>
```

### 3.6 브로드캐스트 데이터에 제어권 정보 추가

```kotlin
// PushDataService.kt - 기존 브로드캐스트에 제어권 정보 추가
fun generateRealtimeData(): String {
    val currentData = dataStoreService.getLatestData()
    val controlInfo = controlLockService.getControlInfo()  // 추가

    val dataWithInfo = mapOf(
        "data" to currentData,
        "trackingStatus" to PushData.TRACKING_STATUS,
        // ... 기존 필드들 ...
        "controlInfo" to controlInfo  // 제어권 정보 추가
    )
    return """{"topic":"read","data":${objectMapper.writeValueAsString(dataWithInfo)}}"""
}
```

```typescript
// stores/icdStore.ts - 제어권 정보 처리
interface ControlInfo {
  hasController: boolean
  accessType: 'LOCAL' | 'REMOTE' | null
  controllerName: string | null
  controllerRole: string | null
  acquiredAt: string | null
}

export const useIcdStore = defineStore('icd', () => {
  const controlInfo = ref<ControlInfo>({
    hasController: false,
    accessType: null,
    controllerName: null,
    controllerRole: null,
    acquiredAt: null
  })

  // WebSocket 메시지 처리 시 제어권 정보 업데이트
  const handleMessage = (message: PushDataMessage) => {
    // 기존 데이터 처리...
    if (message.controlInfo) {
      controlInfo.value = message.controlInfo
    }
  }

  return { controlInfo, handleMessage }
})
```

---

## 4. 확장성 개선

### 4.1 서비스 분해 전략

기존 계획([Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md))의 분해 전략에 추가:

#### 4.1.1 EphemerisService 분해 (상세화)

```
service/mode/ephemeris/
├── EphemerisService.kt          # 조율자 (Facade)
│   ├── Orchestration only
│   └── 300줄 이하
│
├── tracking/
│   ├── SatelliteTracker.kt      # 추적 상태 관리
│   │   ├── startTracking()
│   │   ├── stopTracking()
│   │   └── getTrackingStatus()
│   │
│   ├── TrackingScheduler.kt     # 스케줄 관리
│   │   ├── scheduleNextPass()
│   │   └── cancelSchedule()
│   │
│   └── TrackingCommandSender.kt # UDP 명령 전송
│       ├── sendAngleCommand()
│       └── sendModeCommand()
│
├── calculation/
│   ├── PositionCalculator.kt    # 위치 계산 (캐싱 적용)
│   │   └── calculatePosition()
│   │
│   ├── KeyholeDetector.kt       # Keyhole 판정
│   │   ├── isInKeyhole()
│   │   └── predictKeyhole()
│   │
│   └── PathPredictor.kt         # 경로 예측
│       └── predictPath()
│
├── state/
│   ├── TrackingState.kt         # sealed class
│   │   ├── Idle
│   │   ├── Initializing
│   │   ├── MovingToPosition
│   │   ├── Tracking
│   │   └── Error
│   │
│   └── StateTransitionManager.kt
│       └── transition()
│
└── event/
    └── TrackingEventPublisher.kt
        └── publish(TrackingEvent)
```

#### 4.1.2 상태 머신 패턴 적용

```kotlin
// 상태 전이 정의
sealed class TrackingState {
    object Idle : TrackingState()
    data class Initializing(val satellite: String) : TrackingState()
    data class MovingToPosition(val target: AnglePosition) : TrackingState()
    data class Tracking(val satellite: String, val position: SatellitePosition) : TrackingState()
    data class Error(val message: String, val cause: Throwable?) : TrackingState()

    // 허용된 전이 정의
    fun canTransitionTo(next: TrackingState): Boolean {
        return when (this) {
            is Idle -> next is Initializing || next is Error
            is Initializing -> next is MovingToPosition || next is Error || next is Idle
            is MovingToPosition -> next is Tracking || next is Error || next is Idle
            is Tracking -> next is MovingToPosition || next is Error || next is Idle
            is Error -> next is Idle
        }
    }
}

@Service
class TrackingStateMachine {
    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    fun transition(newState: TrackingState): Result<Unit> {
        val current = _state.value
        return if (current.canTransitionTo(newState)) {
            _state.value = newState
            eventBus.publish(StateChangedEvent(current, newState))
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateTransition(current, newState))
        }
    }
}
```

### 4.2 이벤트 기반 아키텍처 강화

```kotlin
// 이벤트 정의
sealed class ACSEvent {
    // 시스템 이벤트
    data class SystemStarted(val timestamp: Instant) : ACSEvent()
    data class SystemStopped(val reason: String) : ACSEvent()

    // 추적 이벤트
    data class TrackingStarted(val satellite: String) : ACSEvent()
    data class TrackingStopped(val reason: String) : ACSEvent()
    data class PositionUpdated(val position: AnglePosition) : ACSEvent()

    // 에러 이벤트
    data class HardwareError(val component: String, val error: String) : ACSEvent()
    data class CommunicationError(val target: String, val error: String) : ACSEvent()

    // 사용자 이벤트
    data class UserConnected(val sessionId: String) : ACSEvent()
    data class UserDisconnected(val sessionId: String) : ACSEvent()
    data class ControlTransferred(val from: String?, val to: String) : ACSEvent()
}

// 이벤트 핸들러 등록
@Component
class TrackingEventHandler(
    private val pushDataService: PushDataService,
    private val loggingService: LoggingService
) {
    @EventListener
    suspend fun onTrackingStarted(event: ACSEvent.TrackingStarted) {
        loggingService.logInfo("Tracking started: ${event.satellite}")
        pushDataService.broadcastEvent(event)
    }

    @EventListener
    suspend fun onHardwareError(event: ACSEvent.HardwareError) {
        loggingService.logError("Hardware error: ${event.component} - ${event.error}")
        pushDataService.broadcastAlert(event)
    }
}
```

---

## 5. 모니터링 및 관측 가능성

### 5.1 메트릭 수집

```kotlin
@Component
class ACSMetrics(
    private val meterRegistry: MeterRegistry
) {
    // 카운터
    private val commandCounter = Counter.builder("acs.commands.total")
        .description("Total commands sent")
        .register(meterRegistry)

    // 게이지
    private val connectedClients = Gauge.builder("acs.clients.connected") {
        sessionManager.getActiveSessionCount().toDouble()
    }.register(meterRegistry)

    // 타이머
    private val calculationTimer = Timer.builder("acs.calculation.duration")
        .description("Satellite position calculation time")
        .register(meterRegistry)

    // 히스토그램
    private val messageSize = DistributionSummary.builder("acs.websocket.message.size")
        .description("WebSocket message size in bytes")
        .register(meterRegistry)

    fun recordCommand(type: String) {
        commandCounter.increment()
    }

    fun <T> timeCalculation(block: () -> T): T {
        return calculationTimer.record(block)
    }

    fun recordMessageSize(size: Long) {
        messageSize.record(size.toDouble())
    }
}
```

### 5.2 헬스 체크

```kotlin
@Component
class ACSHealthIndicator(
    private val icdService: ICDService,
    private val orekitConfig: OrekitConfig
) : HealthIndicator {

    override fun health(): Health {
        val builder = Health.Builder()
        val details = mutableMapOf<String, Any>()

        // UDP 연결 상태
        val udpStatus = checkUdpConnection()
        details["udp"] = udpStatus

        // Orekit 상태
        val orekitStatus = checkOrekit()
        details["orekit"] = orekitStatus

        // WebSocket 클라이언트 수
        details["websocket_clients"] = sessionManager.getActiveSessionCount()

        // 마지막 데이터 수신 시간
        details["last_data_received"] = icdService.getLastReceivedTime()

        return if (udpStatus == "UP" && orekitStatus == "UP") {
            builder.up().withDetails(details).build()
        } else {
            builder.down().withDetails(details).build()
        }
    }

    private fun checkUdpConnection(): String {
        val lastReceived = icdService.getLastReceivedTime()
        val threshold = Instant.now().minusSeconds(5)
        return if (lastReceived.isAfter(threshold)) "UP" else "DOWN"
    }

    private fun checkOrekit(): String {
        return try {
            orekitConfig.isInitialized()
            "UP"
        } catch (e: Exception) {
            "DOWN: ${e.message}"
        }
    }
}
```

### 5.3 로깅 표준화

```kotlin
// 구조화된 로깅
@Component
class StructuredLogger {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun logEvent(event: String, details: Map<String, Any?>) {
        val json = objectMapper.writeValueAsString(details)
        logger.info("event={} details={}", event, json)
    }

    fun logCommand(command: String, sessionId: String, result: String) {
        logEvent("COMMAND", mapOf(
            "command" to command,
            "sessionId" to sessionId,
            "result" to result,
            "timestamp" to Instant.now()
        ))
    }

    fun logPerformance(operation: String, durationMs: Long) {
        if (durationMs > 100) {  // 100ms 이상만 로깅
            logEvent("SLOW_OPERATION", mapOf(
                "operation" to operation,
                "durationMs" to durationMs
            ))
        }
    }
}
```

---

## 6. 구현 우선순위

### Phase A: 기반 구축 (1주)

| 순서 | 작업 | 영향도 | 리스크 |
|------|------|--------|--------|
| A.1 | 캐싱 인프라 구축 | 중간 | 낮음 |
| A.2 | 메트릭 수집 추가 | 낮음 | 낮음 |
| A.3 | 세션 관리 기본 구조 | 중간 | 낮음 |

### Phase B: 실시간 최적화 (2주)

| 순서 | 작업 | 영향도 | 리스크 |
|------|------|--------|--------|
| B.1 | Orekit 캐싱 적용 | 높음 | 중간 |
| B.2 | Delta Compression | 중간 | 중간 |
| B.3 | 적응형 업데이트 주기 | 중간 | 낮음 |

### Phase C: 다중 사용자 (2주)

| 순서 | 작업 | 영향도 | 리스크 |
|------|------|--------|--------|
| C.1 | 세션 역할 기반 권한 | 높음 | 중간 |
| C.2 | 제어권 관리 | 높음 | 중간 |
| C.3 | Frontend 제어권 UI | 중간 | 낮음 |

### Phase D: 서비스 분해 (기존 계획과 통합)

기존 [Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md)의 Phase 3과 통합

---

## 7. 성능 목표

| 지표 | 현재 | 목표 | 측정 방법 |
|------|------|------|----------|
| WebSocket 지연 | ~40-50ms | < 30ms | 타임스탬프 비교 |
| 동시 접속자 | 테스트 안됨 | 10명 이상 | 부하 테스트 |
| 위치 계산 | 10-50ms | < 5ms (캐시 hit) | 메트릭 |
| 메시지 크기 | 2-5KB | < 1KB | 메트릭 |
| 메모리 사용 | 측정 안됨 | < 512MB | JVM 모니터링 |

---

## 8. 리스크 평가

| 리스크 | 확률 | 영향 | 완화 전략 |
|--------|------|------|----------|
| 캐싱 일관성 문제 | 중간 | 높음 | TTL 짧게, 무효화 트리거 |
| 제어권 데드락 | 낮음 | 높음 | 타임아웃, 자동 해제 |
| 세션 관리 복잡성 | 중간 | 중간 | 점진적 적용 |
| 기존 기능 회귀 | 중간 | 높음 | 테스트 커버리지 확보 |

---

## 9. 결정 필요 사항

### 9.1 캐싱 전략
- [ ] 인메모리 캐시 (Caffeine) vs 분산 캐시 (Redis)
- [ ] TTL 기본값 (1초 vs 5초)

### 9.2 다중 사용자
- [ ] 제어권 자동 해제 시간 (5분 vs 10분)
- [ ] 역할 기반 접근 제어 범위

### 9.3 모니터링
- [ ] 메트릭 저장소 (Prometheus vs InfluxDB)
- [ ] 대시보드 도구 (Grafana)

---

## 10. 관련 문서

| 문서 | 설명 |
|------|------|
| [Architecture_Refactoring_plan.md](./Architecture_Refactoring_plan.md) | 전체 리팩토링 계획 |
| [Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md) | 백엔드 상세 계획 |
| [Frontend_Refactoring_plan.md](./Frontend_Refactoring_plan.md) | 프론트엔드 상세 계획 |
| [SYSTEM_OVERVIEW.md](../../references/architecture/SYSTEM_OVERVIEW.md) | 시스템 개요 |

---

## 11. Ktor vs Spring WebFlux 성능 비교 분석

### 11.1 벤치마크 결과 (Senacor 연구 기준)

| 항목 | Ktor-Netty | Spring WebFlux | 비고 |
|------|-----------|----------------|------|
| **처리량** | ~900 req/s | ~500 req/s | Ktor 80% 우위 |
| **메모리 사용** | 510 MiB | 1.05 GiB | Ktor 50% 절약 |
| **Cold Start** | 빠름 | 상대적 느림 | Ktor 유리 |
| **Kotlin 친화성** | Native | Good | Ktor 우위 |

### 11.2 ACS 프로젝트 적용 분석

#### 현재 Spring WebFlux의 장점 (유지 권장)

```yaml
현재 장점:
  1. 안정성:
     - 10년+ 생산 환경 검증
     - 대규모 커뮤니티 지원
     - 풍부한 문서화

  2. 생태계:
     - Spring Security 통합
     - Actuator 모니터링
     - 다양한 스타터 패키지

  3. 팀 친숙도:
     - 기존 코드베이스 호환
     - 학습 곡선 없음
```

#### Ktor 전환 시 고려사항

```yaml
장점:
  - 높은 처리량 (실시간 시스템에 유리)
  - 낮은 메모리 사용량
  - Kotlin Coroutines 네이티브 지원

단점:
  - 전체 코드 재작성 필요
  - 생태계 상대적 부족
  - 보안/모니터링 직접 구현 필요

전환 비용 예상:
  - 기간: 8-12주
  - 위험도: 높음 (제어 시스템 안정성 중요)
```

### 11.3 권장 결정

```
┌─────────────────────────────────────────────────────────────┐
│                    권장: Spring WebFlux 유지                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  이유:                                                       │
│  1. ✅ 안테나 제어 시스템은 "안정성 > 성능"                      │
│  2. ✅ 현재 성능 (~40-50ms)이 요구사항 충족                     │
│  3. ✅ 캐싱 최적화로 추가 성능 확보 가능                        │
│  4. ✅ 전환 리스크 대비 이득 불명확                            │
│                                                             │
│  대안:                                                       │
│  - 성능 병목 구간에만 Ktor 모듈 도입 (점진적)                   │
│  - UDP Handler를 Ktor로 분리 (가장 성능 민감)                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 12. 코드 품질 및 예외처리 기준

### 12.1 예외 계층 구조 (Sealed Class)

```kotlin
/**
 * ACS 통합 예외 계층
 * - 모든 도메인 예외는 이 계층을 따름
 */
sealed class ACSException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause) {

    // === 통신 관련 ===
    sealed class Communication(message: String, cause: Throwable? = null)
        : ACSException(message, cause) {

        data class UdpTimeout(
            val target: String,
            val timeoutMs: Long
        ) : Communication("UDP timeout to $target after ${timeoutMs}ms")

        data class WebSocketError(
            val sessionId: String,
            val reason: String
        ) : Communication("WebSocket error for session $sessionId: $reason")
    }

    // === 하드웨어 관련 ===
    sealed class Hardware(message: String, cause: Throwable? = null)
        : ACSException(message, cause) {

        data class LimitExceeded(
            val axis: String,
            val value: Double,
            val limit: Double
        ) : Hardware("$axis limit exceeded: $value > $limit")

        data class EmergencyStop(
            val reason: String
        ) : Hardware("Emergency stop: $reason")
    }

    // === 추적 관련 ===
    sealed class Tracking(message: String, cause: Throwable? = null)
        : ACSException(message, cause) {

        data class SatelliteNotFound(
            val noradId: String
        ) : Tracking("Satellite not found: $noradId")

        data class InvalidTLE(
            val reason: String
        ) : Tracking("Invalid TLE: $reason")

        data class KeyholeViolation(
            val azimuth: Double,
            val elevation: Double
        ) : Tracking("Keyhole violation at Az=$azimuth, El=$elevation")
    }

    // === 상태 관련 ===
    sealed class State(message: String, cause: Throwable? = null)
        : ACSException(message, cause) {

        data class InvalidTransition(
            val from: String,
            val to: String
        ) : State("Invalid state transition: $from -> $to")

        data class OperationNotAllowed(
            val operation: String,
            val currentState: String
        ) : State("Operation '$operation' not allowed in state '$currentState'")
    }
}
```

### 12.2 Result 패턴 (성공/실패 명시적 처리)

```kotlin
/**
 * 도메인 작업 결과 래퍼
 */
sealed class OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>()

    sealed class Failure : OperationResult<Nothing>() {
        data class ValidationError(val errors: List<String>) : Failure()
        data class NotFound(val resource: String) : Failure()
        data class Conflict(val reason: String) : Failure()
        data class SystemError(val exception: Throwable) : Failure()
    }

    fun <R> map(transform: (T) -> R): OperationResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    suspend fun <R> flatMap(transform: suspend (T) -> OperationResult<R>): OperationResult<R> =
        when (this) {
            is Success -> transform(data)
            is Failure -> this
        }
}

// 사용 예시
class SatelliteService {
    suspend fun findSatellite(noradId: String): OperationResult<Satellite> {
        return try {
            val satellite = repository.findByNoradId(noradId)
            if (satellite != null) {
                OperationResult.Success(satellite)
            } else {
                OperationResult.Failure.NotFound("Satellite with ID $noradId")
            }
        } catch (e: Exception) {
            OperationResult.Failure.SystemError(e)
        }
    }
}
```

### 12.3 글로벌 예외 핸들러

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ACSException::class)
    fun handleACSException(ex: ACSException): ResponseEntity<ErrorResponse> {
        val (status, code) = when (ex) {
            is ACSException.Communication -> HttpStatus.SERVICE_UNAVAILABLE to "COMM_ERROR"
            is ACSException.Hardware -> HttpStatus.CONFLICT to "HW_ERROR"
            is ACSException.Tracking -> HttpStatus.BAD_REQUEST to "TRACK_ERROR"
            is ACSException.State -> HttpStatus.CONFLICT to "STATE_ERROR"
        }

        logger.error("ACS Exception: ${ex.javaClass.simpleName}", ex)

        return ResponseEntity
            .status(status)
            .body(ErrorResponse(
                code = code,
                message = ex.message,
                timestamp = Instant.now()
            ))
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant,
    val details: Map<String, Any>? = null
)
```

### 12.4 코드 품질 체크리스트

```yaml
필수 적용:
  1. Null Safety:
     - !! 사용 금지 (테스트 제외)
     - ?. 와 ?: 적극 활용
     - requireNotNull() 대신 구체적 예외 사용

  2. 불변성:
     - data class 기본 사용
     - var 대신 val 우선
     - 컬렉션은 불변 타입 우선 (List, Map)

  3. 순수 함수:
     - Algorithm 계층은 외부 상태 접근 금지
     - 같은 입력 → 같은 출력 보장
     - 부수 효과 없음

  4. 문서화:
     - public 함수는 KDoc 필수
     - 복잡한 로직은 인라인 주석
     - 단위(도/라디안, UTC/로컬) 명시
```

---

## 13. 아키텍처 일관성 기준

### 13.1 백엔드 계층 규칙

```
┌──────────────────────────────────────────────────────────────┐
│                        Controller Layer                       │
│  ─ HTTP 요청/응답 처리                                         │
│  ─ 입력 검증 (Validation)                                      │
│  ─ DTO ↔ Domain 변환                                          │
│  ─ 직접 계산/비즈니스 로직 금지 ❌                               │
└───────────────────────────┬──────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                        Service Layer                          │
│  ─ 비즈니스 로직 조율                                           │
│  ─ 트랜잭션 관리                                               │
│  ─ 여러 서비스/알고리즘 조합                                     │
│  ─ 직접 수학 계산 금지 ❌                                        │
└───────────────────────────┬──────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                       Algorithm Layer                         │
│  ─ 순수 계산 함수                                              │
│  ─ 외부 의존성 최소화                                           │
│  ─ 테스트 용이성 최대화                                         │
│  ─ 입/출력 단위 명시 필수                                       │
└───────────────────────────┬──────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                       Repository Layer                        │
│  ─ 데이터 접근 추상화                                           │
│  ─ 캐싱 적용 가능                                              │
│  ─ SQL/NoSQL 구현 분리                                        │
└──────────────────────────────────────────────────────────────┘
```

### 13.2 프론트엔드 컴포넌트 규칙

```
┌──────────────────────────────────────────────────────────────┐
│                          Pages                                │
│  ─ 라우트 진입점                                               │
│  ─ 레이아웃 구성                                               │
│  ─ 페이지별 상태 초기화                                         │
│  ─ 비즈니스 로직 최소화 (Store 위임)                            │
└───────────────────────────┬──────────────────────────────────┘
                            │ uses
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                       Components                              │
│  ─ 재사용 가능한 UI 조각                                        │
│  ─ Props로 데이터 수신                                         │
│  ─ Emit으로 이벤트 전달                                        │
│  ─ 직접 API 호출 금지 ❌                                        │
└───────────────────────────┬──────────────────────────────────┘
                            │ uses
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                       Composables                             │
│  ─ 재사용 로직 캡슐화                                           │
│  ─ 상태 관리 헬퍼                                              │
│  ─ 공통 동작 추상화 (useLoading, useErrorHandler)              │
└───────────────────────────┬──────────────────────────────────┘
                            │ uses
                            ▼
┌───────────────────────────┬──────────────────────────────────┐
│          Stores           │          Services                 │
│  ─ 전역 상태 관리          │  ─ API 통신 담당                  │
│  ─ 비즈니스 로직           │  ─ 데이터 변환                    │
│  ─ 캐싱/동기화             │  ─ 에러 핸들링                    │
└───────────────────────────┴──────────────────────────────────┘
```

### 13.3 네이밍 컨벤션

```yaml
Backend (Kotlin):
  클래스:
    - Service: *Service (SatelliteTrackingService)
    - Controller: *Controller (EphemerisController)
    - Algorithm: *Calculator, *Predictor (PositionCalculator)
    - Repository: *Repository (TLERepository)

  함수:
    - 조회: get*, find*, search*
    - 생성: create*, add*
    - 수정: update*, modify*
    - 삭제: delete*, remove*
    - 계산: calculate*, compute*
    - 검증: validate*, check*, is*

  변수:
    - 불변: val (기본)
    - 가변: var (명시적 필요 시)
    - 각도: *Deg (도), *Rad (라디안) 접미사 권장

Frontend (TypeScript/Vue):
  파일:
    - 컴포넌트: PascalCase.vue (ControlPanel.vue)
    - 스토어: camelCaseStore.ts (trackingStore.ts)
    - 서비스: camelCaseService.ts (ephemerisService.ts)
    - Composables: use*.ts (useLoading.ts)

  함수:
    - 컴포저블: use* (useErrorHandler)
    - 이벤트 핸들러: handle*, on* (handleClick, onSubmit)
    - 계산: compute*, calculate*

  상수:
    - UPPER_SNAKE_CASE (MAX_RETRY_COUNT)
```

---

## 14. 위성 추적 알고리즘 보호 가이드라인

### 14.1 수정 금지 타이밍 상수

```kotlin
/**
 * ⚠️ 경고: 아래 상수는 하드웨어 동기화 및 제어 안정성에 직결됩니다.
 * 변경 시 시스템 오동작 또는 안테나 손상 위험이 있습니다.
 *
 * 변경이 필요한 경우:
 * 1. 하드웨어 팀과 사전 협의 필수
 * 2. 시뮬레이션 환경에서 충분한 테스트
 * 3. 실 장비에서 감시 하에 테스트
 */
object CriticalTimingConstants {
    // === 절대 변경 금지 ===

    /** UDP 통신 주기 (하드웨어 동기화) */
    const val UDP_INTERVAL_MS = 10L

    /** WebSocket 전송 주기 (프론트엔드 동기화) */
    const val WEBSOCKET_INTERVAL_MS = 30L

    /** 추적 모니터링 주기 */
    const val TRACKING_MONITOR_INTERVAL_MS = 100L

    /** 상태 변경 최소 간격 (채터링 방지) */
    const val STATE_CHANGE_MIN_INTERVAL_MS = 500L

    /** 대기 로그 출력 주기 */
    const val WAITING_LOG_INTERVAL_MS = 5000L

    // === 조정 가능 (주의 필요) ===

    /** 캐시 TTL - 성능에만 영향 */
    const val CACHE_TTL_SECONDS = 1L

    /** 세션 타임아웃 - UX에만 영향 */
    const val SESSION_TIMEOUT_MINUTES = 5L
}
```

### 14.2 알고리즘 수정 체크리스트

```yaml
수정 전 확인사항:
  □ 기존 테스트 모두 통과하는가?
  □ 수정이 타이밍에 영향을 주는가?
  □ 단위 변환(도/라디안)이 올바른가?
  □ Orekit 데이터 경로가 유효한가?
  □ 좌표계 변환이 정확한가?

수정 후 검증:
  □ 단위 테스트 추가/업데이트
  □ 시뮬레이션 환경 테스트
  □ 성능 벤치마크 (속도 저하 없음)
  □ 코드 리뷰 통과
```

### 14.3 보호 대상 파일 목록

```yaml
High Risk (하드웨어 제어 직접 연관):
  - backend/service/UdpFwICDService.kt
  - backend/service/ICDService.kt
  - backend/algorithm/position/*Calculator.kt
  - backend/algorithm/tracking/*Tracker.kt

Medium Risk (추적 로직):
  - backend/service/mode/ephemeris/EphemerisService.kt
  - backend/service/mode/suntrack/SunTrackService.kt
  - backend/algorithm/sun/*Solar*.kt
  - backend/algorithm/satellite/*Satellite*.kt

Low Risk (표시/UI):
  - frontend/stores/icdStore.ts
  - frontend/pages/mode/*.vue
```

### 14.4 변경 승인 프로세스

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ 변경 요청    │ ──> │ 영향 분석    │ ──> │ 코드 리뷰    │
│ (개발자)    │     │ (tech-lead) │     │ (code-rev)  │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                    ┌──────────────────────────┘
                    ▼
     ┌─────────────────────────────────────────────────┐
     │                시뮬레이션 테스트                  │
     │  - 정상 동작 시나리오                            │
     │  - 엣지 케이스 (키홀, 리밋)                      │
     │  - 성능 벤치마크                                │
     └───────────────────────┬─────────────────────────┘
                             │ 통과
                             ▼
     ┌─────────────────────────────────────────────────┐
     │              실 장비 테스트 (선택적)              │
     │  - 감시 하에 진행                               │
     │  - 비상 정지 준비                               │
     └───────────────────────┬─────────────────────────┘
                             │ 승인
                             ▼
                    ┌─────────────┐
                    │   머지       │
                    └─────────────┘
```

---

## 15. 결정 필요 사항 (업데이트)

### 15.1 캐싱 전략 ✅ 결정됨

| 항목 | 결정 | 이유 |
|------|------|------|
| 캐시 라이브러리 | **Caffeine** | 단일 서버, ≤10 사용자, Redis 불필요 |
| TTL 전략 | **이벤트 기반** | TTL 없음, `@CacheEvict`로 변경 시 초기화 |
| 실시간 데이터 | **AtomicReference** 유지 | DataStoreService 기존 패턴 유지 |
| 정적 데이터 | **Caffeine + PostgreSQL** | DB 도입 시 적용 예정 |

```kotlin
// Caffeine 설정 예시
@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        return CaffeineCacheManager().apply {
            setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)        // 메모리 보호
                .recordStats()           // 모니터링용
                // TTL 없음 - @CacheEvict로만 관리
            )
        }
    }
}
```

### 15.2 다중 사용자 ✅ 결정됨

| 항목 | 결정 | 이유 |
|------|------|------|
| 세션 관리 | **불필요** | 동일 화면 공유, 브로드캐스트 유지 |
| 제어권 모델 | **배타적 1인** | 제어권 해제 후 다른 사람 획득 가능 |
| VIEWER | 제어권 ❌ | 관찰만 가능 |
| OPERATOR | 로컬+API ✅ | 원격 제어 가능 |
| ADMIN | 로컬만 ✅ | localhost 접속 필수 |
| LED 표시 | LOCAL(🟢) / REMOTE(🔵) / IDLE(⚫) | 제어 접속 유형 시각화 |

- [ ] 제어권 자동 해제 시간 (5분 vs 10분) - 미결정

### 15.3 모니터링
- [ ] 메트릭 저장소 (Prometheus vs InfluxDB)
- [ ] 대시보드 도구 (Grafana)

### 15.4 프레임워크 (신규)
- [x] **결정: Spring WebFlux 유지** - 안정성 우선
- [ ] 성능 병목 시 Ktor 부분 도입 검토

### 15.5 코드 품질 (신규)
- [ ] ACSException 계층 구현 시점 (Phase 1 권장)
- [ ] Result 패턴 전면 적용 vs 점진적 적용

### 15.6 WebSocket 최적화 ✅ 결정됨

| 항목 | 결정 | 이유 |
|------|------|------|
| Delta Compression | **보류** | 현재 2-5KB 전송량은 LAN 환경에서 부담 없음 |
| 적응형 업데이트 주기 | **보류** | 모드 전환 시 불안정 위험, 30ms 고정이 안정적 |

**재검토 조건**: 클라이언트 10명 초과 또는 네트워크 병목 실제 발생 시

---

**문서 버전**: 2.3.0
**작성일**: 2026-01-07
**최종 수정**: 2026-01-07
**상태**: RFC (검토 진행 중)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0.0 | 2026-01-07 | 최초 작성 - 실시간성/다중사용자 최적화 |
| 2.0.0 | 2026-01-07 | Ktor 분석, 코드품질 기준, 알고리즘 보호 가이드 추가 |
| 2.1.0 | 2026-01-07 | 캐싱 전략 결정 (Caffeine, 이벤트 기반, TTL 없음) |
| 2.2.0 | 2026-01-07 | WebSocket 최적화 검토 완료 - Delta Compression/적응형 주기 보류 결정 |
| 2.3.0 | 2026-01-07 | 다중 사용자 섹션 재설계 - 브로드캐스트 유지, 제어권 LED 표시 추가 |
