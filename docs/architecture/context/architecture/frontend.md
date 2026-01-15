# Frontend 아키텍처

> Vue 3 + Quasar + TypeScript + Pinia 기반 SPA

## 기술 스택

| 기술 | 버전 | 용도 |
|-----|------|------|
| Vue | 3.x | 프레임워크 |
| Quasar | 2.x | UI 컴포넌트 |
| TypeScript | 5.x | 타입 시스템 |
| Pinia | 2.x | 상태 관리 |
| Vite | 5.x | 빌드 도구 |
| ECharts | 5.x | 차트 렌더링 |

## 코드 현황

| 영역 | 파일 수 | 코드 줄 |
|-----|--------|---------|
| 전체 | 93개 (.vue, .ts) | 30,000줄+ |
| 대형 파일 (300줄+) | 12개 | 18,000줄+ |

## 폴더 구조

```
frontend/src/
├── components/           # 재사용 컴포넌트
│   ├── common/           # 공통 컴포넌트
│   ├── chart/            # 차트 컴포넌트
│   └── antenna/          # 안테나 관련
├── pages/                # 페이지 컴포넌트
│   └── mode/             # 모드별 페이지
│       ├── StepPage.vue
│       ├── SlewPage.vue
│       ├── EphemerisDesignationPage.vue (4,340줄)
│       ├── PassSchedulePage.vue (4,838줄)
│       ├── SunTrackPage.vue
│       ├── PedestalPositionPage.vue
│       └── StandbyPage.vue
├── stores/               # Pinia 스토어
│   ├── icd/
│   │   └── icdStore.ts (2,971줄)  # ICD 상태 (핵심!)
│   ├── mode/
│   │   ├── passScheduleStore.ts (2,452줄)
│   │   ├── ephemerisTrackStore.ts (1,287줄)
│   │   ├── stepStore.ts
│   │   ├── slewStore.ts
│   │   ├── standbyStore.ts
│   │   └── pedestalPositionStore.ts
│   ├── settings/
│   │   └── settingsStore.ts
│   └── auth/
│       └── authStore.ts
├── services/             # API 서비스
│   ├── api.ts            # Axios 인스턴스
│   ├── icdService.ts (814줄)
│   └── passScheduleService.ts (1,117줄)
├── composables/          # Composition 함수
│   ├── useErrorHandler.ts
│   ├── useNotification.ts
│   └── useLoading.ts
├── types/                # TypeScript 타입
│   ├── api.d.ts
│   └── icd.d.ts
├── workers/              # Web Workers
│   └── trackingWorker.ts # 궤적 계산 최적화
└── utils/                # 유틸리티
    ├── conversion.ts     # 단위 변환
    └── format.ts         # 포맷팅
```

## 코딩 규칙

### 컴포넌트 작성
```vue
<script setup lang="ts">
// 필수: setup + lang="ts"
import { ref, computed } from 'vue'
import { useIcdStore } from '@/stores/icd/icdStore'

// Props 타입 정의
interface Props {
  title: string
  value?: number
}

const props = withDefaults(defineProps<Props>(), {
  value: 0
})

// Emits 타입 정의
const emit = defineEmits<{
  update: [value: number]
}>()

// 스토어 사용
const icdStore = useIcdStore()

// 반응형 데이터
const localValue = ref(props.value)

// computed 활용
const displayValue = computed(() =>
  `${localValue.value.toFixed(2)}°`
)
</script>

<template>
  <div class="component">
    {{ displayValue }}
  </div>
</template>

<style scoped>
/* scoped 필수, 테마 변수 사용 */
.component {
  color: var(--theme-text-primary);
}
</style>
```

### 스토어 패턴 (Setup Store)
```typescript
// stores/exampleStore.ts
import { defineStore } from 'pinia'
import { ref, computed, shallowRef } from 'vue'

export const useExampleStore = defineStore('example', () => {
  // State - 대용량 데이터는 shallowRef
  const data = shallowRef<Data | null>(null)
  const isLoading = ref(false)

  // Getters
  const hasData = computed(() => data.value !== null)

  // Actions
  async function fetchData() {
    isLoading.value = true
    try {
      data.value = await api.getData()
    } finally {
      isLoading.value = false
    }
  }

  return {
    data,
    isLoading,
    hasData,
    fetchData
  }
})
```

### 색상 규칙
```css
/* ✅ 올바른 사용 */
.element {
  color: var(--theme-text-primary);
  background: var(--theme-bg-secondary);
  border-color: var(--theme-border);
}

/* ❌ 금지: 하드코딩 색상 */
.element {
  color: #333333;
  background: white;
}
```

## 핵심 스토어

### icdStore.ts (2,971줄) - 가장 중요!

**역할:** 안테나 실시간 상태 관리 (30ms 업데이트)

```typescript
// stores/icd/icdStore.ts
export const useIcdStore = defineStore('icd', () => {
  // 실시간 데이터 - shallowRef로 배치 업데이트
  const antennaPosition = shallowRef<AntennaPosition>({
    azimuth: { cmd: 0, actual: 0, speed: 0 },
    elevation: { cmd: 0, actual: 0, speed: 0 },
    train: { cmd: 0, actual: 0, speed: 0 }
  })

  // 보드 상태 비트
  const boardStatus = shallowRef<BoardStatus>({...})

  // 추적 상태
  const trackingState = shallowRef<TrackingState>({...})

  // WebSocket 연결
  function connectWebSocket() {
    ws = new WebSocket('ws://localhost:8080/ws/icd')
    ws.onmessage = handleMessage
  }

  // 30ms마다 호출
  function handleMessage(event: MessageEvent) {
    const data = JSON.parse(event.data)
    // shallowRef이므로 전체 객체 교체
    antennaPosition.value = { ...data.position }
    boardStatus.value = { ...data.board }
  }

  return { antennaPosition, boardStatus, trackingState, connectWebSocket }
})
```

### passScheduleStore.ts (2,452줄)

**역할:** 패스 스케줄 상태 관리

```typescript
// stores/mode/passScheduleStore.ts
export const usePassScheduleModeStore = defineStore('passScheduleMode', () => {
  // 스케줄 목록
  const scheduleList = ref<Schedule[]>([])
  const selectedSchedule = ref<Schedule | null>(null)

  // 추적 상태
  const trackingState = ref<TrackingState>('idle')

  // Web Worker로 궤적 계산 (성능 최적화)
  const worker = new Worker(new URL('@/workers/trackingWorker.ts', import.meta.url))

  async function calculatePath(schedule: Schedule) {
    return new Promise((resolve) => {
      worker.postMessage({ type: 'calculate', schedule })
      worker.onmessage = (e) => resolve(e.data)
    })
  }

  return { scheduleList, selectedSchedule, trackingState, calculatePath }
})
```

### ephemerisTrackStore.ts (1,287줄)

**역할:** Ephemeris Designation 모드 상태 관리

```typescript
export const useEphemerisTrackStore = defineStore('ephemerisTrack', () => {
  // TLE 데이터
  const tleData = ref<TLEData | null>(null)

  // 추적 스케줄
  const trackingSchedule = ref<TrackingSchedule[]>([])

  // 현재 상태
  const currentState = ref<'idle' | 'preparing' | 'tracking'>('idle')

  return { tleData, trackingSchedule, currentState }
})
```

## 대형 페이지 컴포넌트

### DashboardPage.vue (2,728줄)

```
dashboard-container
├── 상단: axis-grid (3축 모니터링)
│   ├── axis-card (Azimuth) - 차트 + 데이터
│   ├── axis-card (Elevation) - 차트 + 데이터
│   ├── axis-card (Tilt) - 차트 + 데이터
│   ├── control-container
│   │   ├── emergency-card + 모달
│   │   └── control-card (LED)
│   └── status-card (LED 상태)
├── 중앙: mode-selection-section
│   └── q-tabs (8개 모드)
└── 하단: mode-content-section
    └── router-view + keep-alive
```

### PassSchedulePage.vue (4,838줄)

```
pass-schedule-mode
├── 1행: offset-control-row
│   └── Offset Controls (Azimuth/Elevation/Tilt/Time)
├── 2행: main-content-row (3열)
│   ├── Position View (차트)
│   ├── Schedule Information
│   └── Schedule Control
│       ├── 스케줄 테이블
│       ├── TLE Upload 버튼
│       ├── Select Schedule 버튼
│       └── Start/Stop/Stow 버튼
```

### EphemerisDesignationPage.vue (4,340줄)

PassSchedulePage와 유사한 구조 + KEYHOLE 정보 섹션

## API 통신

### REST API
```typescript
// services/passScheduleService.ts
import { api } from './api'

export const passScheduleService = {
  async getSchedules() {
    return api.get<Schedule[]>('/pass-schedule/list')
  },

  async uploadTLE(tleData: string) {
    return api.post('/pass-schedule/tle', { tle: tleData })
  },

  async startTracking(scheduleId: number) {
    return api.post(`/pass-schedule/${scheduleId}/start`)
  }
}
```

### WebSocket
```typescript
// stores/icd/icdStore.ts
const ws = new WebSocket('ws://localhost:8080/ws/icd')

ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  antennaPosition.value = { ...data.position }
}

ws.onerror = (error) => {
  console.error('WebSocket error:', error)
  reconnect()
}
```

## 성능 최적화

### 30ms 업데이트 대응

**문제:**
- 30ms마다 데이터 수신 → 33Hz 리렌더링
- 개별 ref 사용 시 175회 업데이트

**해결책:**
```typescript
// ❌ 비효율: 개별 ref
const azimuth = ref(0)
const elevation = ref(0)
// ... 173개 더

// ✅ 효율: shallowRef 그룹화
const antennaState = shallowRef<AntennaState>({
  azimuth: 0,
  elevation: 0,
  train: 0,
  // ...
})

// 전체 객체 교체로 단일 업데이트
antennaState.value = { ...newData }
```

### Web Worker 활용

```typescript
// workers/trackingWorker.ts
self.onmessage = (e) => {
  const { type, schedule } = e.data

  if (type === 'calculate') {
    const path = calculateTrackingPath(schedule)
    self.postMessage({ type: 'result', path })
  }
}
```

### 차트 최적화

```typescript
// ECharts 배치 업데이트
class PassChartUpdatePool {
  private pendingUpdates: ChartUpdate[] = []

  updatePosition(el: number, az: number) {
    this.pendingUpdates.push({ type: 'position', el, az })
  }

  flush() {
    chart.setOption(this.buildOption())
    this.pendingUpdates = []
  }
}
```

## Composables 활용

```typescript
// 에러 처리
const { handleError } = useErrorHandler()

// 알림
const { notify } = useNotification()

// 로딩
const { isLoading, withLoading } = useLoading()

await withLoading(async () => {
  await api.saveData(data)
  notify.success('저장 완료')
})
```

## 대형 파일 현황 (300줄+)

| 파일 | 줄 수 | 분류 | 리팩토링 우선순위 |
|-----|------|------|-----------------|
| PassSchedulePage.vue | 4,838 | pages/mode | P1 (6개 컴포넌트 분리) |
| EphemerisDesignationPage.vue | 4,340 | pages/mode | P1 (7개 컴포넌트 분리) |
| icdStore.ts | 2,971 | stores/icd | P2 (5개 파일 분리) |
| DashboardPage.vue | 2,728 | pages | P2 (5개 컴포넌트 분리) |
| passScheduleStore.ts | 2,452 | stores/mode | - |
| ephemerisTrackStore.ts | 1,287 | stores/mode | - |
| passScheduleService.ts | 1,117 | services | - |
| icdService.ts | 814 | services | - |

## 주의사항

- **단위 변환**: API 라디안 → UI 도(°)
- **시간 변환**: API UTC → UI 로컬 시간
- **30ms 업데이트**: shallowRef 필수, 성능 프로파일링
- **테마 변수**: 색상 하드코딩 금지
- **Train/Tilt**: 변수명은 `train`, UI 표시는 `Tilt`

## 참조

- [ICD 프로토콜](../domain/icd-protocol.md)
- [모드 시스템](../domain/mode-system.md)
- [Backend 아키텍처](backend.md)
- [심층 분석: FE Stores](../analysis/frontend/stores.md)
- [심층 분석: FE Pages](../analysis/frontend/pages.md)

---

**최종 수정:** 2026-01-15
**분석 기반:** Phase 1-4 코드베이스 심층 분석
