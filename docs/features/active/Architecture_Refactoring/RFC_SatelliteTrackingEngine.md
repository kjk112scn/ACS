# RFC: SatelliteTrackingEngine 통합 아키텍처

> **상위 문서**: [Architecture_Refactoring_plan.md](./Architecture_Refactoring_plan.md)

## 개요

| 항목 | 내용 |
|------|------|
| **상태** | Draft |
| **작성일** | 2026-01-07 |
| **버전** | 1.1.0 |
| **우선순위** | P0 (핵심 리팩토링) |
| **설계 원칙** | 소규모 팀 관리 가능, 오버엔지니어링 금지 |

---

## 1. 문제 정의

### 1.1 현재 상황

```
EphemerisService.kt (5,060줄)  ←─┐
                                  ├─→ 중복 코드 약 40%
PassScheduleService.kt (2,896줄) ←─┘
```

**백엔드 중복 코드**:
- `TrackingState` enum
- `moveTrainToZero()`, `moveToTargetAzEl()` - 축 이동 제어
- `isTrainAtZero()`, `isTrainStabilized()` - 상태 확인
- `sendHeaderTrackingData()`, `sendInitialTrackingData()` - ICD 통신

**프론트엔드 문제**:
- `icdStore.ts` (44,000+ 토큰) - 비대화
- `ephemerisTrackingState` + `passScheduleTrackingState` 분리 관리 → 동기화 문제

### 1.2 비즈니스 로직 차이

| 항목 | Ephemeris | PassSchedule |
|------|-----------|--------------|
| 위성 수 | 1개 | 다수 |
| 스케줄 수 | 1개 선택 | 다수 자동 실행 |
| 시작 방식 | 수동 (즉시 추적) | 자동 (2분 전 사전배치) |

**결론**: 데이터 스토어는 분리 유지, 추적 실행 로직만 통합

---

## 2. 설계 원칙

### 2.1 적용할 것

| 원칙 | 이유 |
|------|------|
| **단순한 enum 상태 머신** | 이해하기 쉽고 디버깅 용이 |
| **공통 클래스 1개 추출** | 중복 제거, 최소한의 변경 |
| **직접 메서드 호출** | 명확하고 추적 가능 |

### 2.2 제외할 것 (오버엔지니어링)

| 패턴 | 제외 이유 |
|------|----------|
| Port/Adapter 패턴 | 인터페이스 추가 = 파일 2배, 유지보수 부담 |
| Spring StateMachine | 현재 규모에 과함 |
| Event Sourcing/CQRS | 복잡도 대비 이점 없음 |
| Guard 조건 클래스화 | if문으로 충분 |
| TanStack Query | WebSocket 실시간 데이터에 부적합 |

---

## 3. 간소화된 아키텍처

### 3.1 백엔드 구조

```
변경 전:
├── EphemerisService.kt (5,060줄) - 모든 것 포함
└── PassScheduleService.kt (2,896줄) - 중복 코드 많음

변경 후:
├── EphemerisService.kt (~2,000줄) - 스케줄 관리만
├── PassScheduleService.kt (~1,500줄) - 스케줄 관리만
└── SatelliteTrackingEngine.kt (~800줄) - 공통 추적 로직
```

**SatelliteTrackingEngine 내용**:
```kotlin
@Component
class SatelliteTrackingEngine(
    private val icdService: ICDService,
    private val udpService: UdpFwICDService
) {
    // 1. 상태 머신
    enum class TrackingState {
        IDLE, PREPARING, WAITING, TRACKING, COMPLETED, ERROR
    }

    private val state = AtomicReference(TrackingState.IDLE)

    // 2. 축 제어 (중복 제거)
    fun moveTrainToZero(): Mono<Boolean> { ... }
    fun moveToTargetAzEl(az: Double, el: Double): Mono<Boolean> { ... }
    fun isTrainAtZero(): Boolean { ... }
    fun isAxisStabilized(): Boolean { ... }

    // 3. ICD 통신 (중복 제거)
    fun sendHeaderData(schedule: ScheduleInfo): Mono<Void> { ... }
    fun sendTrackingData(point: TrackingPoint): Mono<Void> { ... }

    // 4. 상태 전이
    fun transitionTo(newState: TrackingState): Boolean {
        val current = state.get()
        if (!isValidTransition(current, newState)) {
            log.warn("Invalid transition: $current → $newState")
            return false
        }
        return state.compareAndSet(current, newState)
    }
}
```

### 3.2 프론트엔드 구조

```
변경 전:
└── stores/icd/icdStore.ts - 모든 상태 혼재 (44,000+ 토큰)

변경 후:
├── stores/icd/icdStore.ts - 실시간 각도 데이터만 (경량화)
└── stores/tracking/trackingStateStore.ts - 추적 상태만 (~200줄, 신규)
```

**trackingStateStore 내용**:
```typescript
export const useTrackingStateStore = defineStore('trackingState', () => {
  // 통합 추적 상태
  const activeMode = ref<'ephemeris' | 'passSchedule' | null>(null)
  const trackingState = ref<TrackingState>('IDLE')
  const currentSchedule = ref<ScheduleInfo | null>(null)
  const nextSchedule = ref<ScheduleInfo | null>(null)

  // 표시용 computed
  const stateDisplay = computed(() => {
    const displays = {
      IDLE: { label: '대기', color: 'grey' },
      PREPARING: { label: '준비 중', color: 'orange' },
      WAITING: { label: '대기 중', color: 'blue' },
      TRACKING: { label: '추적 중', color: 'green' },
      COMPLETED: { label: '완료', color: 'primary' },
      ERROR: { label: '오류', color: 'red' }
    }
    return displays[trackingState.value]
  })

  // WebSocket 메시지 처리
  function handleTrackingMessage(message: TrackingStatusMessage) {
    trackingState.value = message.state
    currentSchedule.value = message.currentSchedule
    nextSchedule.value = message.nextSchedule
  }

  return {
    activeMode: readonly(activeMode),
    trackingState: readonly(trackingState),
    currentSchedule: readonly(currentSchedule),
    nextSchedule: readonly(nextSchedule),
    stateDisplay,
    handleTrackingMessage
  }
})
```

---

## 4. 구현 계획

### 4.1 작업 목록 (총 3개 파일)

| 순서 | 작업 | 파일 | 예상 |
|------|------|------|------|
| 1 | SatelliteTrackingEngine 추출 | `engine/SatelliteTrackingEngine.kt` | 2일 |
| 2 | EphemerisService 리팩토링 | 기존 파일 수정 | 1일 |
| 3 | PassScheduleService 리팩토링 | 기존 파일 수정 | 1일 |
| 4 | trackingStateStore 생성 | `stores/tracking/trackingStateStore.ts` | 1일 |
| 5 | icdStore에서 상태 분리 | 기존 파일 수정 | 0.5일 |
| 6 | 테스트 작성 | 상태 전이 로직 집중 | 1일 |

### 4.2 테스트 전략 (최소한)

```kotlin
// 상태 전이 로직만 집중 테스트
@Test
fun `IDLE에서 PREPARING으로 전이 가능`() {
    val engine = SatelliteTrackingEngine(mockIcdService, mockUdpService)
    assertTrue(engine.transitionTo(TrackingState.PREPARING))
}

@Test
fun `IDLE에서 TRACKING으로 직접 전이 불가`() {
    val engine = SatelliteTrackingEngine(mockIcdService, mockUdpService)
    assertFalse(engine.transitionTo(TrackingState.TRACKING))
}
```

---

## 5. 예상 효과

| 지표 | 현재 | 목표 |
|------|------|------|
| EphemerisService | 5,060줄 | ~2,000줄 |
| PassScheduleService | 2,896줄 | ~1,500줄 |
| 코드 중복률 | ~40% | <10% |
| 신규 파일 수 | - | 2개 (BE 1, FE 1) |

---

## 6. 설계 검토 결과

### 6.1 업계 표준 비교

| 영역 | 평가 | 비고 |
|------|------|------|
| 상태 머신 | ⭐⭐⭐⭐ | enum + transition map 적절 |
| 계층 구조 | ⭐⭐⭐⭐ | 3계층 충분, Port/Adapter 불필요 |
| FE 상태 관리 | ⭐⭐⭐⭐ | Pinia 유지, 스토어 분리만 |
| 복잡도 | ⭐⭐⭐⭐⭐ | 소규모 팀 관리 가능 수준 |

### 6.2 결론

**한 줄 요약**: 중복 코드를 공유 클래스 1개로 추출하고, 프론트 스토어 1개 분리하면 완료.

- 새 패키지/계층 만들지 않음
- 복잡한 디자인 패턴 도입하지 않음
- 기존 구조 최대한 유지하면서 중복만 제거

---

## 7. 실시간 성능 최적화

### 7.1 백엔드 블로킹 코드 제거

**현재 문제점**:
| 파일 | 위치 | 문제 |
|------|------|------|
| `PassScheduleController.kt` | :1944 | `Thread.sleep(100)` |
| `BatchStorageManager.kt` | :294 | `Thread.sleep(100)` |
| `UdpFwICDService.kt` | :1074, :1148 | `Thread.sleep(1000)` |
| `ElevationCalculator.kt` | :78 | `runBlocking` |

**개선 방안**:
```kotlin
// ❌ 현재 (블로킹)
Thread.sleep(100)

// ✅ 권장 (논블로킹)
Mono.delay(Duration.ofMillis(100)).then(...)

// 또는 Scheduler 활용
executor.schedule({ ... }, 100, TimeUnit.MILLISECONDS)
```

### 7.2 프론트엔드 차트 컴포넌트 분리

**현재 문제점**:
| 파일 | 줄 수 | 차트 로직 |
|------|------|----------|
| PassSchedulePage.vue | 4,841 | ~800줄 혼재 |
| EphemerisDesignationPage.vue | 4,376 | ~600줄 혼재 |
| DashboardPage.vue | 2,728 | ~400줄 혼재 |

**개선 방안**:
```
변경 전:
└── pages/mode/EphemerisDesignationPage.vue (4,376줄)
    └── 차트 로직 직접 포함

변경 후:
├── pages/mode/EphemerisDesignationPage.vue (~2,500줄)
├── components/charts/PositionViewChart.vue (~500줄, 신규)
└── composables/useChartUpdate.ts (~150줄, 신규)
```

### 7.3 차트 업데이트 최적화

**현재 패턴** (각 페이지에 중복):
```typescript
// 30ms setInterval + 다수의 watch
setInterval(() => updateChart(), 30)
watch(() => store.trackingPath, () => { ... })
watch(() => store.trackingState, () => { ... })
```

**권장 패턴**:
```typescript
// composables/useChartUpdate.ts
export function useChartUpdate(chartRef: Ref<ECharts | null>) {
  const pendingData = ref<ChartData | null>(null)

  // WebSocket 데이터 수신 시 버퍼에 저장만
  function onData(data: ChartData) {
    pendingData.value = data
  }

  // requestAnimationFrame으로 렌더링 (~16ms)
  function startUpdateLoop() {
    function update() {
      if (pendingData.value && chartRef.value) {
        chartRef.value.setOption({
          series: [{ data: pendingData.value }]
        }, { animation: false, silent: true })
        pendingData.value = null
      }
      requestAnimationFrame(update)
    }
    requestAnimationFrame(update)
  }

  return { onData, startUpdateLoop }
}
```

### 7.4 watch 정리

**현재 상태**:
- DashboardPage: 13개 watch (대부분 디버깅용)
- EphemerisDesignationPage: 5개+ watch
- PassSchedulePage: 6개+ watch

**정리 기준**:
| 유형 | 처리 |
|------|------|
| 디버깅용 console.log | 삭제 또는 개발 모드만 |
| 상태 변경 반응 | 유지 (최소화) |
| 중복 watch | 통합 |

### 7.5 잘 되어 있는 부분 (유지)

| 영역 | 구현 | 평가 |
|------|------|------|
| ThreadManager | 우선순위별 Executor 분리 | ✅ 유지 |
| WebSocket 브로드캐스트 | 30ms scheduleAtFixedRate | ✅ 유지 |
| 차트 옵션 | animation: false, silent: true | ✅ 유지 |
| 공유 데이터 버퍼 | AtomicReference | ✅ 유지 |

---

## 8. 전체 구현 계획 (통합)

### Phase 1: 체계 수립 (2일)
- 폴더 구조 정리
- 코딩 컨벤션 문서화

### Phase 2: 백엔드 리팩토링 (5일)
- SatelliteTrackingEngine 추출 (2일)
- EphemerisService/PassScheduleService 정리 (2일)
- 블로킹 코드 제거 (1일)

### Phase 3: 프론트엔드 리팩토링 (5일)
- trackingStateStore 생성 (1일)
- PositionViewChart 컴포넌트 분리 (2일)
- useChartUpdate composable 생성 (1일)
- watch 정리 및 디버깅 코드 제거 (1일)

### Phase 4: 테스트 및 검증 (2일)
- 상태 전이 테스트
- 실시간 성능 테스트
- BE-FE 통합 테스트

**총 예상 기간: 14일**

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0.0 | 2026-01-07 | 최초 작성 |
| 1.1.0 | 2026-01-07 | 소규모 팀 관점 간소화, 오버엔지니어링 제거 |
| 1.2.0 | 2026-01-07 | 실시간 성능 최적화 섹션 추가, 전체 구현 계획 통합 |
