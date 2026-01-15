# Frontend Page 컴포넌트 심층 분석

> 분석 일시: 2026-01-15
> 분석 대상: frontend/src/pages/
> 총 코드: ~12,000줄 (7개 주요 페이지)

## 1. DashboardPage.vue (2,728줄)

### 1.1 컴포넌트 구조

**Script Setup 구성:**
- Vue 3 Composition API 기반 (`<script setup lang="ts">`)
- 주요 import:
  - `echarts` - 3개 축별 차트 렌더링
  - `useICDStore` - ICD 실시간 데이터
  - `useRouter/useRoute` - 라우팅
  - `useTheme` - 테마 관리

**상태 관리 (ref/computed 86개 이상):**
- 차트 관련: `azimuthChartRef`, `elevationChartRef`, `trainChartRef`, `chartsInitialized`
- 비상 정지: `acsEmergencyActive`, `emergencyModal`
- 에러 상태 (6개 computed):
  - `errorEmergencyActive`
  - `errorPositionerActive`
  - `errorFeedActive`
  - `errorProtocolActive`
  - `errorPowerActive`
  - `statusCardHasError`
- 모터 상태 (3개 computed): `azimuthMotorState`, `elevationMotorState`, `trainMotorState`

### 1.2 템플릿 구조

```
dashboard-container
├── 상단: axis-grid (3축 모니터링)
│   ├── axis-card (Azimuth) - 차트 + 데이터 표시
│   ├── axis-card (Elevation) - 차트 + 데이터 표시
│   ├── axis-card (Tilt) - 차트 + 데이터 표시
│   ├── control-container
│   │   ├── emergency-card + 모달
│   │   └── control-card (Control/Monitoring LED)
│   └── status-card (Emergency/Positioner/Feed/Protocol/Power/Stow LED)
├── 중앙: mode-selection-section
│   └── q-tabs (8개 모드)
└── 하단: mode-content-section
    └── router-view + keep-alive
```

### 1.3 주요 Quasar 컴포넌트

- `q-card`, `q-card-section`
- `q-tabs`, `q-tab` (모드 선택)
- `q-btn` (Emergency, Control Request, All Status)
- `q-dialog` (Emergency 해제 모달)
- `q-badge`, `q-chip` (상태 표시기)

### 1.4 분할 제안

**독립 분리 가능 컴포넌트 3개:**

1. **AxisCard.vue** (~300줄)
   - Props: axis, motorState, cmdValue, actualValue, speed
   - Slots: chart 영역

2. **EmergencyControl.vue** (~150줄)
   - Props: isActive
   - Emits: emergency-clicked, emergency-released

3. **StatusIndicator.vue** (~100줄)
   - Props: statusMap (LED 상태)

**분할 후 크기:** DashboardPage ~1,500줄 (45% 감소)

---

## 2. PassSchedulePage.vue (4,838줄)

### 2.1 컴포넌트 구조

**Script Setup 구성:**
- `usePassScheduleModeStore` - 스케줄 데이터
- `useICDStore` - 실시간 추적 정보
- ECharts 차트
- `PassChartUpdatePool` 클래스 (최적화된 데이터 풀)

**주요 상태:**
- 차트: `chartRef`, `passChart`, `isChartInitialized`, `updateTimer`
- 스케줄: `scheduleData`, `sortedScheduleList`, `selectedSchedule`
- 폼: `inputs[]` (4개 Offset), `outputs[]` (4개 Output)
- 시간: `formattedCalTime`, `timeRemaining`

### 2.2 템플릿 구조

```
pass-schedule-mode
├── 1행: offset-control-row
│   └── Offset Controls (Azimuth/Elevation/Tilt/Time)
├── 2행: main-content-row (3열)
│   ├── Position View (col-3) - 차트
│   ├── Schedule Information (col-3)
│   └── Schedule Control (col-6)
│       ├── 스케줄 테이블
│       ├── TLE Upload 버튼
│       ├── Select Schedule 버튼
│       └── Start/Stop/Stow 버튼
```

### 2.3 TLE 업로드 흐름

1. TLE Upload 버튼 → 모달 열기
2. TLE 입력 → API 호출
3. Select Schedule → 스케줄 선택

### 2.4 차트 최적화

```typescript
class PassChartUpdatePool {
  updatePosition(elevation, azimuth)      // 현재 위치
  updateTrackingPath(newPath)            // 실시간 추적 경로
  updatePredictedPath(newPath)           // 예측 경로
  getUpdateOption()                      // 업데이트 옵션 반환
}
```

### 2.5 분할 제안

**독립 분리 가능 컴포넌트 4개:**

1. **OffsetControlPanel.vue** (~400줄) - 3개 페이지 공유
2. **ScheduleTable.vue** (~300줄)
3. **PositionViewChart.vue** (~200줄)
4. **ScheduleInfoPanel.vue** (~150줄)

**분할 후 크기:** PassSchedulePage ~2,500줄 (50% 감소)

---

## 3. EphemerisDesignationPage.vue (4,340줄)

### 3.1 컴포넌트 구조

**Script Setup 구성:**
- `useEphemerisDesignationStore` - 위성/스케줄 데이터
- `useICDStore` - 실시간 추적
- ECharts 차트

**주요 상태:**
- 선택 스케줄: `selectedScheduleInfo`, `displaySchedule`
- TLE 데이터: `tleData`, `tempTLEData`, `showTLEModal`
- 스케줄 선택: `selectedSchedule`, `showScheduleModal`
- Offset: `inputs[]`, `outputs[]`

### 3.2 오프셋 입력 섹션

PassSchedulePage와 동일 구조:
- Azimuth/Elevation/Tilt Offset
- Time Offset + Cal Time

**차이점:**
- Ephemeris: "Axis Calculator" 버튼 추가
- KEYHOLE 정보 섹션 (권장 Train 각도, 속도 비교)

### 3.3 KEYHOLE 정보

- 권장 Train 각도
- 2축/3축/최적화 Az 속도
- 2축/3축/최적화 El 속도

### 3.4 분할 제안

**독립 분리 가능 컴포넌트 4개:**

1. **OffsetControlPanel.vue** (공유)
2. **TLEInputDialog.vue** (~200줄)
3. **ScheduleSelectionModal.vue** (~250줄)
4. **SatelliteInfoPanel.vue** (~200줄)

**분할 후 크기:** EphemerisDesignationPage ~2,500줄 (40% 감소)

---

## 4. Mode Pages (Step, Slew, SunTrack, Pedestal)

### 4.1 공통 패턴

**템플릿 구조:**
```
mode-shell
├── 축별 제어 패널 (3개)
│   ├── Azimuth
│   ├── Elevation
│   └── Tilt
└── 제어 버튼
    ├── Go
    ├── Stop
    └── Stow
```

### 4.2 Step vs Slew 차이점

| 항목 | Step | Slew |
|------|------|------|
| 입력 | 각도 + 속도 | 속도만 |
| 계산 | CMD + 입력 = 목표 | 속도부호로 방향 결정 |
| Loop | X | O (선택적) |
| 코드 라인 | ~435줄 | ~600줄 |

### 4.3 SunTrack 특성

- Offset Controls (Azimuth/Elevation/Tilt/Time)
- Speed Settings 섹션
- 차트 없음

### 4.4 PedestalPosition 특성

- Current Position (읽기만)
- Target Position (입력)
- Target Speed (입력)
- 절대 위치 입력

### 4.5 분할 제안

**공통 AxisInputPanel.vue** (~250줄)
- Step, Slew, Pedestal에서 재사용

---

## 5. 컴포넌트 분할 권장사항

### Phase 1: 공통 컴포넌트 추출 (즉시 가능)

#### 1. OffsetControlPanel.vue

```typescript
interface Props {
  modelValue: {
    azimuth: string | number
    elevation: string | number
    tilt: string | number
    time: string | number
  }
  outputs: {
    azimuth: string | number
    elevation: string | number
    tilt: string | number
    calTime: string
  }
  showCalTime?: boolean
}
```

**공유 페이지:** PassSchedule, Ephemeris, SunTrack

#### 2. AxisInputPanel.vue

```typescript
interface Props {
  axis: 'azimuth' | 'elevation' | 'train'
  label: string
  value: AxisValue
  disabled: boolean
  inputType: 'angle' | 'speed' | 'position'
}
```

**공유 페이지:** Step, Slew, Pedestal

#### 3. AxisCard.vue

```typescript
interface Props {
  axis: AxisType
  cmdValue: number | string
  actualValue: number | string
  speed: number | string
  motorState: MotorState
  hasChart?: boolean
}
```

**공유 페이지:** Dashboard

### Phase 2: 큰 컴포넌트 분할 (중기)

#### PassSchedulePage 분할 후

```
PassSchedulePage.vue (1,500줄)
├── OffsetControlPanel.vue (공통)
├── PositionViewChart.vue (200줄)
├── ScheduleInfoPanel.vue (150줄)
├── ScheduleTable.vue (300줄)
└── ScheduleControlSection.vue (200줄)
```

#### EphemerisDesignationPage 분할 후

```
EphemerisDesignationPage.vue (1,500줄)
├── OffsetControlPanel.vue (공통)
├── PositionViewChart.vue (공통)
├── SatelliteInfoPanel.vue (200줄)
├── TLEInputDialog.vue (150줄)
└── ScheduleSelectionModal.vue (250줄)
```

#### DashboardPage 분할 후

```
DashboardPage.vue (1,200줄)
├── AxisCard.vue (공통)
├── EmergencyControl.vue (150줄)
├── ControlStatus.vue (100줄)
├── ModeSelector.vue (100줄)
└── ModeContentSection.vue (100줄)
```

### Phase 3: Composables 추출 (장기)

#### useAxisFormatter.ts

```typescript
export function useAxisFormatter() {
  const formatSpeed = (axis: AxisKey, value: string) => { ... }
  const formatAngle = (axis: AxisKey, value: string) => { ... }
  const formatPosition = (axis: AxisKey, value: string) => { ... }
  return { formatSpeed, formatAngle, formatPosition }
}
```

#### useChartRendering.ts

```typescript
export function useChartRendering(chartRef: Ref<HTMLElement>) {
  const initChart = (options: EChartsOption) => { ... }
  const updateChart = (data: ChartData) => { ... }
  const disposeChart = () => { ... }
  return { initChart, updateChart, disposeChart }
}
```

#### useAxisControl.ts

```typescript
export function useAxisControl(icdStore: ICDStore) {
  const sendGoCommand = (axes: SelectedAxes, ...params) => { ... }
  const sendStopCommand = (axes: SelectedAxes) => { ... }
  const sendStowCommand = () => { ... }
  return { sendGoCommand, sendStopCommand, sendStowCommand }
}
```

---

## 6. 재사용 가능 패턴

### 6.1 축 패널 패턴

```vue
<q-card class="axis-panel" :class="{ 'disabled-panel': !selected }">
  <q-checkbox v-model="selected" />
  <q-input v-model="angle" @update:model-value="formatAngle" />
  <q-input v-model="speed" @update:model-value="formatSpeed" />
</q-card>
```

### 6.2 차트 렌더링 패턴

```typescript
const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | undefined = undefined

onMounted(() => {
  chart = echarts.init(chartRef.value)
  chart.setOption(defaultOption)
})

watch(() => icdStore.data, () => {
  if (chart) chart.setOption(updateOption)
})

onUnmounted(() => {
  chart?.dispose()
})
```

### 6.3 Offset 계산 패턴

```typescript
const inputs = ref(['0', '0', '0', '0'])
const outputs = ref(['0', '0', '0', '0'])

const onInputChange = (index: number, val: string) => {
  inputs.value[index] = val
  updateOutput(index)
}
```

### 6.4 모달 패턴

```vue
<q-dialog v-model="showModal" persistent>
  <q-card>
    <q-card-section class="bg-primary">
      <div class="text-h6">제목</div>
    </q-card-section>
    <q-card-section><!-- 내용 --></q-card-section>
    <q-card-actions align="right">
      <q-btn flat label="닫기" v-close-popup />
      <q-btn flat label="확인" @click="handleConfirm" />
    </q-card-actions>
  </q-card>
</q-dialog>
```

---

## 7. 현재 상태 요약

| 메트릭 | 값 |
|--------|-----|
| **총 코드 라인** | ~12,000줄 (7개 페이지) |
| **평균 페이지 크기** | ~1,700줄 |
| **최대 페이지** | PassSchedulePage (4,838줄) |
| **컴포넌트 재사용도** | 낮음 (복제 코드 30%) |
| **공통 패턴** | 4가지 (축패널, 차트, Offset, 모달) |

---

## 8. 리팩토링 로드맵

### Week 1: 공통 컴포넌트
- OffsetControlPanel.vue 추출
- AxisInputPanel.vue 추출
- 예상 코드 감소: ~800줄

### Week 2: 대형 페이지 분할
- PassSchedulePage 분할 (5개 컴포넌트)
- EphemerisDesignationPage 분할 (5개 컴포넌트)
- 예상 코드 감소: ~2,000줄

### Week 3: Composables 추출
- useAxisFormatter, useChartRendering, useAxisControl
- Dashboard, Mode Pages 마이그레이션

### Week 4: 통합 테스트
- 모든 페이지 기능 테스트
- E2E 테스트 추가
- 성능 최적화

**예상 결과:**
- 페이지 코드 40-50% 감소 (~5,000-6,000줄)
- 컴포넌트 수 증가 (7 → 25개)
- 유지보수성 대폭 향상

---

**문서 버전**: 1.0.0
**작성자**: FE Expert Agent
**최종 검토**: 2026-01-15
