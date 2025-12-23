# Frontend Architecture Refactoring (프론트엔드 리팩토링) 계획서

> 상위 문서: [Architecture_Refactoring_plan.md](./Architecture_Refactoring_plan.md)

---

## 현황 분석

### 통계

| 항목 | 수치 |
|------|------|
| 컴포넌트 | 43개 |
| 스토어 | 25개 |
| 테스트 | 0개 (0%) |
| 거대 파일 (300줄+) | 18개 |

---

## 1. 구조 문제점

### 1.1 애매한 파일 위치 (폴더 밖에 있는 파일들)

| 파일 | 현재 위치 | 문제 | 해결 |
|------|----------|------|------|
| `HardwareErrorLogPanel.vue` | `components/` 루트 | 폴더 안에 있어야 함 | `components/hardware-error/`로 이동 |
| `hardwareErrorLogStore.ts` | `stores/` 루트 | 폴더 안에 있어야 함 | `stores/hardware-error/`로 이동 |
| `example-store.ts` | `stores/` 루트 | 예제 파일 | 삭제 |

### 1.2 중복/혼란스러운 파일

| 문제 | 파일 위치 | 해결 |
|------|----------|------|
| `modeStore.ts` 2개 | `stores/common/` + `stores/icd/` | 하나로 통합 |
| `settingsStore.ts` 2개 | `stores/api/` + `stores/api/settings/` | 역할 명확히 분리 |

---

## 2. 거대한 파일 목록 (300줄 이상)

### 2.1 Pages (5개)

| 파일 | 줄 수 | 분해 방향 |
|------|-------|----------|
| `PassSchedulePage.vue` | 4,841 | 컴포넌트 5개 + composable 3개 |
| `EphemerisDesignationPage.vue` | 4,376 | 컴포넌트 5개 + composable 3개 |
| `DashboardPage.vue` | 2,728 | 컴포넌트 4개 + composable 2개 |
| `FeedPage.vue` | 2,531 | 밴드별 컴포넌트 분리 |
| `SunTrackPage.vue` | 1,289 | 컴포넌트 3개 + composable 1개 |

### 2.2 Components (6개)

| 파일 | 줄 수 | 분해 방향 |
|------|-------|----------|
| `AllStatusContent.vue` | 2,381 | 섹션별 컴포넌트 분리 |
| `SelectScheduleContent.vue` | 2,270 | 목록/상세 컴포넌트 분리 |
| `TLEUploadContent.vue` | 1,678 | 업로드/목록/상세 분리 |
| `SystemInfoContent.vue` | 1,561 | 탭별 컴포넌트 분리 |
| `MaintenanceSettings.vue` | 902 | 기능별 분리 |
| `HardwareErrorLogPanel.vue` | 767 | 필터/테이블/페이징 분리 |

### 2.3 Stores (4개)

| 파일 | 줄 수 | 분해 방향 |
|------|-------|----------|
| `icdStore.ts` | 2,971 | 도메인별 그룹화 (angles, status, feed) |
| `passScheduleStore.ts` | 2,452 | 상태/액션 분리 |
| `ephemerisTrackStore.ts` | 1,367 | 상태/액션 분리 |
| `settingsStore.ts` | 786 | 이미 분리된 스토어들 활용 |

### 2.4 Services (3개)

| 파일 | 줄 수 | 분해 방향 |
|------|-------|----------|
| `ephemerisTrackService.ts` | 1,192 | API/로직 분리 |
| `passScheduleService.ts` | 1,117 | API/로직 분리 |
| `icdService.ts` | 873 | 명령별 분리 |

---

## 3. 권장 폴더 구조

```
frontend/src/
├── components/
│   ├── common/                    ← OK (유지)
│   ├── content/                   ← 파일들 분해 필요
│   │   ├── all-status/            ← AllStatusContent 분해 (신규)
│   │   ├── schedule/              ← SelectScheduleContent 분해 (신규)
│   │   ├── tle-upload/            ← TLEUploadContent 분해 (신규)
│   │   └── system-info/           ← SystemInfoContent 분해 (신규)
│   ├── Settings/                  ← OK (유지)
│   ├── hardware-error/            ← HardwareErrorLogPanel 이동 (신규)
│   ├── dashboard/                 ← DashboardPage 분해용 (신규)
│   ├── pass-schedule/             ← PassSchedulePage 분해용 (신규)
│   ├── ephemeris/                 ← EphemerisPage 분해용 (신규)
│   ├── feed/                      ← FeedPage 분해용 (신규)
│   └── sun-track/                 ← SunTrackPage 분해용 (신규)
│
├── composables/
│   ├── common/                    ← 기존 composables 이동 (신규)
│   │   ├── useTheme.ts
│   │   ├── useValidation.ts
│   │   ├── useErrorHandler.ts
│   │   ├── useDialog.ts
│   │   ├── useLoading.ts
│   │   ├── useNotification.ts
│   │   ├── useI18n.ts
│   │   └── useSharedStore.ts
│   ├── settings/                  ← Settings 관련 (신규)
│   │   └── useSettingsForm.ts
│   ├── pass-schedule/             ← PassSchedule 로직 (신규)
│   ├── ephemeris/                 ← Ephemeris 로직 (신규)
│   └── dashboard/                 ← Dashboard 로직 (신규)
│
├── stores/
│   ├── api/settings/              ← OK (유지)
│   ├── common/                    ← modeStore만 유지
│   ├── icd/                       ← icdStore만 유지 (modeStore 삭제)
│   ├── mode/                      ← OK (유지)
│   ├── ui/                        ← OK (유지)
│   └── hardware-error/            ← hardwareErrorLogStore 이동 (신규)
│
├── services/
│   ├── api/                       ← OK (유지)
│   └── mode/                      ← OK (유지, 파일 분해는 별도)
│
├── pages/                         ← 페이지는 조합만 (300줄 이하 목표)
│
└── types/                         ← OK (유지)
```

---

## 4. Phase 0: 폴더 구조 정리

### Task 0.1: 루트 파일 정리

**목표**: 폴더 밖에 있는 파일들을 적절한 위치로 이동

**작업 목록**:
- [ ] `components/HardwareErrorLogPanel.vue` → `components/hardware-error/HardwareErrorLogPanel.vue`
- [ ] `stores/hardwareErrorLogStore.ts` → `stores/hardware-error/hardwareErrorLogStore.ts`
- [ ] `stores/example-store.ts` → 삭제
- [ ] 이동 후 import 경로 수정

**영향 파일**:
- `MainLayout.vue` (HardwareErrorLogPanel import)
- `stores/index.ts` (store export)

---

### Task 0.2: 중복 파일 정리

**목표**: 중복된 modeStore 통합

**현재 상태**:
```
stores/common/modeStore.ts   ← 214줄
stores/icd/modeStore.ts      ← 206줄  (중복?)
```

**작업 목록**:
- [ ] 두 파일 비교하여 역할 파악
- [ ] 중복이면 하나로 통합
- [ ] 다른 역할이면 이름 변경으로 구분

---

### Task 0.3: composables 폴더 정리

**목표**: composables를 용도별로 분류

**현재 상태**:
```
composables/
├── useTheme.ts         (872줄 - 너무 큼)
├── useValidation.ts    (241줄)
├── useErrorHandler.ts  (230줄)
├── useDialog.ts        (141줄)
├── useLoading.ts       (119줄)
├── useNotification.ts  (115줄)
├── useI18n.ts          (57줄)
└── useSharedStore.ts   (39줄)
```

**작업 목록**:
- [ ] `composables/common/` 폴더 생성
- [ ] 기존 파일들 이동
- [ ] `useTheme.ts` 분해 검토 (872줄)
- [ ] import 경로 수정

---

## 5. Phase 1: Settings Composable 통합

### Task 1.1: useSettingsForm 생성

**목표**: 11개 Settings 컴포넌트의 중복 로직을 1개 composable로 통합

**현재 문제**:
```typescript
// 11개 파일에서 반복되는 패턴 (각 150줄)
const localSettings = ref<T>()
const originalSettings = ref<T>()
const hasUnsavedChanges = computed(() => ...)
watch(localSettings, ...)
const save = async () => { ... }
const reset = () => { ... }
```

**해결책**:
```typescript
// frontend/src/composables/settings/useSettingsForm.ts (신규)
export function useSettingsForm<T>(options: {
  store: SettingsStore<T>
  validateFn?: (settings: T) => ValidationResult
}) {
  const localSettings = ref<T>()
  const originalSettings = ref<T>()

  const hasUnsavedChanges = computed(() =>
    JSON.stringify(localSettings.value) !== JSON.stringify(originalSettings.value)
  )

  const save = async () => {
    if (options.validateFn) {
      const result = options.validateFn(localSettings.value)
      if (!result.valid) return result
    }
    await options.store.save(localSettings.value)
    originalSettings.value = { ...localSettings.value }
  }

  const reset = () => {
    localSettings.value = { ...originalSettings.value }
  }

  const load = async () => {
    await options.store.load()
    localSettings.value = { ...options.store.settings }
    originalSettings.value = { ...options.store.settings }
  }

  return { localSettings, originalSettings, hasUnsavedChanges, save, reset, load }
}
```

**적용 대상**:
- [ ] `LocationSettings.vue`
- [ ] `AlgorithmSettings.vue`
- [ ] `AngleLimitsSettings.vue`
- [ ] `AntennaSpecSettings.vue`
- [ ] `OffsetLimitsSettings.vue`
- [ ] `SpeedLimitsSettings.vue`
- [ ] `StepSizeLimitSettings.vue`
- [ ] `StowSettings.vue`
- [ ] `TrackingSettings.vue`
- [ ] `FeedSettings.vue`
- [ ] `SystemSettings.vue`

**예상 효과**: 1,650줄 → 300줄 (80% 감소)

---

### Task 1.2: Debug 로그 정리

**목표**: 프로덕션에 불필요한 `console.log` 제거

**현재 문제**:
- 프론트엔드: 829개 console.log (34개 파일)
- 주요 위치:
  - `PassSchedulePage.vue`: 208개
  - `DashboardPage.vue`: 68개
  - `HardwareErrorLogPanel.vue`: 36개

**해결책**:
```typescript
// frontend/src/utils/logger.ts (신규)
const isDev = import.meta.env.DEV

export const logger = {
  debug: (...args: unknown[]) => isDev && console.log('[DEBUG]', ...args),
  info: (...args: unknown[]) => console.info('[INFO]', ...args),
  warn: (...args: unknown[]) => console.warn('[WARN]', ...args),
  error: (...args: unknown[]) => console.error('[ERROR]', ...args),
}
```

**예상 효과**: 프로덕션 콘솔 로그 90% 감소

---

## 6. Phase 2: 스토어/서비스 개선

### Task 2.1: icdStore 구조 개선

**목표**: 100+ ref 변수를 도메인별 그룹화된 reactive 객체로 변환

**현재 문제**:
```typescript
// icdStore.ts - 100개 이상의 ref가 나열됨
const azimuthAngle = ref('')
const elevationAngle = ref('')
const trainAngle = ref('')
// ... 100개 더
```

**해결책**:
```typescript
// types/antenna.ts (신규)
export interface AntennaData {
  time: { server: string; offset: string; cmd: string }
  angles: { azimuth: number; elevation: number; train: number }
  speeds: { azimuth: number; elevation: number; train: number }
  torques: { azimuth: number; elevation: number; train: number }
  cmd: { azimuth: number; elevation: number; train: number; time: string }
  status: {
    mode: string
    mainBoard: MainBoardStatus
    azimuthBoard: BoardStatus
    elevationBoard: BoardStatus
    trainBoard: BoardStatus
    feed: FeedStatus
  }
  environment: { windSpeed: number; windDirection: number; temperature: { rtd1: number; rtd2: number } }
}

// icdStore.ts (개선)
export const useICDStore = defineStore('icd', () => {
  const antennaData = reactive<AntennaData>({ ... })

  const updateFromMessage = (data: MessageData) => {
    antennaData.angles.azimuth = parseFloat(data.azimuthAngle)
    // ...
  }

  return { antennaData, updateFromMessage }
})
```

**예상 효과**: 코드 가독성 70% 향상, 타입 안전성 강화

---

### Task 2.2: 필터/페이징 Composables

**목표**: HardwareErrorLogPanel 등에서 반복되는 필터/페이징 로직 추출

```typescript
// composables/common/usePagination.ts
export function usePagination<T>(options: {
  fetchFn: (page: number, size: number) => Promise<PaginatedResponse<T>>
  pageSize?: number
}) {
  const items = ref<T[]>([])
  const currentPage = ref(0)
  const totalPages = ref(0)
  const loading = ref(false)

  const loadPage = async (page: number) => { ... }

  return { items, currentPage, totalPages, loading, loadPage }
}

// composables/common/useFilters.ts
export function useFilters<T, F>(options: {
  initialFilters: F
  debounceMs?: number
}) {
  const filters = reactive<F>(options.initialFilters)
  // ...
  return { filters, applyFilters }
}
```

---

## 7. 완료 기준

### Phase 0 완료 기준
- [ ] 루트에 있던 파일들 적절한 폴더로 이동
- [ ] `example-store.ts` 삭제
- [ ] 중복 `modeStore.ts` 정리
- [ ] `composables/common/` 폴더 생성 및 파일 이동
- [ ] 모든 import 경로 수정 완료
- [ ] 빌드 성공 확인

### Phase 1 완료 기준
- [ ] `useSettingsForm.ts` composable 생성
- [ ] 11개 Settings 컴포넌트에 적용
- [ ] logger 유틸리티 생성 및 적용
- [ ] 기존 기능 정상 동작 확인

### Phase 2 완료 기준
- [ ] icdStore 구조 개선 완료
- [ ] 필터/페이징 composables 생성
- [ ] 테스트 커버리지 40% 달성

---

## 8. Phase 1: Quasar 컴포넌트 표준화

### 8.1 현재 문제점

#### 문제 1: q-btn 속성 불일치

```vue
<!-- LoginPage.vue -->
<q-btn label="Login" color="primary" class="full-width" />

<!-- TLEUploadContent.vue -->
<q-btn icon="upload_file" color="primary" size="md" class="toolbar-btn" />
<q-btn icon="download" color="info" size="md" />
<q-btn icon="delete" color="negative" size="md" />

<!-- SlewPage.vue -->
<q-btn label="Go" color="positive" icon="play_arrow" size="lg" />
<q-btn label="Stop" color="negative" icon="stop" size="lg" />

<!-- DashboardPage.vue -->
<q-btn flat label="닫기" color="grey-7" v-close-popup />
```

**문제**: `color`, `size`, `flat/dense/outline` 속성이 파일마다 제각각

#### 문제 2: q-input/q-select 스타일 불일치

```vue
<!-- LocationSettings.vue -->
<q-input v-model.number="..." label="위도" type="number" outlined />

<!-- AxisTransformCalculator.vue -->
<q-input v-model.number="..." type="number" outlined dense step="0.01" />

<!-- HardwareErrorLogPanel.vue -->
<q-select outlined style="min-width: 150px" clearable />
```

**문제**: `outlined/filled`, `dense` 여부, 너비 설정이 불일치

#### 문제 3: 색상 하드코딩

```scss
/* TLEUploadContent.vue */
.tle-name { color: #64b5f6; }
.tle-lines { border-left: 3px solid #64b5f6; }

/* MainLayout.vue */
.body--dark .custom-header { background-color: #091d24 !important; }
.text-positive { color: #4caf50 !important; }
```

**문제**: 테마 변수(`var(--theme-*)`)가 있는데도 직접 색상 코드 사용

#### 문제 4: !important 과다 사용

```vue
<q-input style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
<div style="height: 220px !important; min-height: 220px !important;"></div>
```

**문제**: 스타일 우선순위 충돌을 !important로 해결 → 유지보수 어려움

#### 문제 5: 커스텀 래퍼 컴포넌트 부재

현재 모든 파일에서 Quasar 컴포넌트를 직접 사용 → 일관성 유지 불가능

---

### 8.2 해결책: UI 컴포넌트 래퍼 생성

#### Task 1.3: AppButton 컴포넌트

```vue
<!-- components/common/AppButton.vue -->
<template>
  <q-btn
    :label="label"
    :icon="icon"
    :color="buttonColor"
    :size="size"
    :flat="variant === 'flat'"
    :outline="variant === 'outline'"
    :dense="dense"
    :disable="disable"
    :loading="loading"
    v-bind="$attrs"
  >
    <slot />
  </q-btn>
</template>

<script setup lang="ts">
type ButtonVariant = 'default' | 'flat' | 'outline'
type ButtonIntent = 'primary' | 'secondary' | 'danger' | 'warning' | 'success' | 'info'
type ButtonSize = 'sm' | 'md' | 'lg'

const props = withDefaults(defineProps<{
  label?: string
  icon?: string
  intent?: ButtonIntent
  variant?: ButtonVariant
  size?: ButtonSize
  dense?: boolean
  disable?: boolean
  loading?: boolean
}>(), {
  intent: 'primary',
  variant: 'default',
  size: 'md',
  dense: false,
  disable: false,
  loading: false
})

// intent를 Quasar color로 매핑
const colorMap: Record<ButtonIntent, string> = {
  primary: 'primary',
  secondary: 'grey-7',
  danger: 'negative',
  warning: 'warning',
  success: 'positive',
  info: 'info'
}

const buttonColor = computed(() => colorMap[props.intent])
</script>
```

**사용 예시**:
```vue
<!-- Before -->
<q-btn label="저장" color="primary" />
<q-btn label="삭제" color="negative" />
<q-btn flat label="취소" color="grey-7" />

<!-- After -->
<AppButton label="저장" intent="primary" />
<AppButton label="삭제" intent="danger" />
<AppButton label="취소" intent="secondary" variant="flat" />
```

#### Task 1.4: AppInput 컴포넌트

```vue
<!-- components/common/AppInput.vue -->
<template>
  <q-input
    v-model="modelValue"
    :label="label"
    :type="type"
    :outlined="variant === 'outlined'"
    :filled="variant === 'filled'"
    :dense="dense"
    :readonly="readonly"
    :disable="disable"
    :style="widthStyle"
    v-bind="$attrs"
  >
    <slot />
  </q-input>
</template>

<script setup lang="ts">
type InputVariant = 'outlined' | 'filled'
type InputWidth = 'auto' | 'sm' | 'md' | 'lg' | 'full'

const props = withDefaults(defineProps<{
  modelValue: string | number
  label?: string
  type?: string
  variant?: InputVariant
  width?: InputWidth
  dense?: boolean
  readonly?: boolean
  disable?: boolean
}>(), {
  variant: 'outlined',
  width: 'auto',
  dense: true,  // 기본값 통일
  type: 'text'
})

const widthMap: Record<InputWidth, string> = {
  auto: 'auto',
  sm: '100px',
  md: '150px',
  lg: '200px',
  full: '100%'
}

const widthStyle = computed(() => ({
  width: widthMap[props.width],
  minWidth: widthMap[props.width]
}))
</script>
```

#### Task 1.5: ConfirmDialog 컴포넌트

```vue
<!-- components/common/ConfirmDialog.vue -->
<template>
  <q-dialog v-model="isOpen" persistent>
    <q-card style="min-width: 350px">
      <q-card-section class="row items-center">
        <q-avatar :icon="icon" :color="iconColor" text-color="white" />
        <span class="q-ml-sm text-h6">{{ title }}</span>
      </q-card-section>

      <q-card-section v-if="message">
        {{ message }}
      </q-card-section>

      <q-card-actions align="right">
        <AppButton
          :label="cancelLabel"
          intent="secondary"
          variant="flat"
          @click="onCancel"
        />
        <AppButton
          :label="confirmLabel"
          :intent="confirmIntent"
          variant="flat"
          @click="onConfirm"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
type DialogType = 'confirm' | 'warning' | 'danger'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title: string
  message?: string
  type?: DialogType
  confirmLabel?: string
  cancelLabel?: string
}>(), {
  type: 'confirm',
  confirmLabel: '확인',
  cancelLabel: '취소'
})

const iconMap: Record<DialogType, string> = {
  confirm: 'help',
  warning: 'warning',
  danger: 'delete'
}

const colorMap: Record<DialogType, string> = {
  confirm: 'primary',
  warning: 'warning',
  danger: 'negative'
}

const intentMap: Record<DialogType, ButtonIntent> = {
  confirm: 'primary',
  warning: 'warning',
  danger: 'danger'
}
</script>
```

**사용 예시**:
```vue
<!-- Before (TLEUploadContent.vue) -->
<q-dialog v-model="confirmDialog" persistent>
  <q-card>
    <q-card-section class="row items-center">
      <q-avatar icon="delete" color="negative" text-color="white" />
      <span class="q-ml-sm">삭제 확인</span>
    </q-card-section>
    <q-card-actions align="right">
      <q-btn flat label="취소" color="primary" v-close-popup />
      <q-btn flat label="삭제" color="negative" @click="..." v-close-popup />
    </q-card-actions>
  </q-card>
</q-dialog>

<!-- After -->
<ConfirmDialog
  v-model="confirmDialog"
  title="삭제 확인"
  message="정말 삭제하시겠습니까?"
  type="danger"
  confirm-label="삭제"
  @confirm="onConfirmDelete"
/>
```

---

### 8.3 테마 변수 통합

#### Task 1.6: 하드코딩 색상 제거

**현재 테마 변수** (`css/theme-variables.scss`):
```scss
:root {
  --theme-primary: #091d24;
  --theme-secondary: #26a69a;
  --theme-positive: #00e676;
  --theme-negative: #f44336;
  --theme-info: #00bcd4;
  --theme-warning: #ffc107;
  --theme-background: #15282f;
  --theme-card-background: #091d24;
  --theme-text: #ffffff;
  --theme-text-secondary: #b0bec5;
  --theme-border: #37474f;
}
```

**수정 필요 파일**:
| 파일 | 하드코딩 색상 | 변경 |
|------|-------------|------|
| `TLEUploadContent.vue` | `#64b5f6`, `#e0e0e0`, `#90caf9` | `var(--theme-info)`, `var(--theme-text-secondary)` |
| `MainLayout.vue` | `#091d24`, `#1976d2`, `#4caf50` | `var(--theme-primary)`, `var(--theme-positive)` |
| `HardwareErrorLogPanel.vue` | 여러 색상 | 테마 변수로 교체 |

#### Task 1.7: !important 제거

**전략**:
1. 컴포넌트 scoped 스타일 사용
2. CSS 특이도(specificity) 조정
3. Quasar 변수 오버라이드 사용

```scss
/* Before */
.my-input {
  width: 110px !important;
  min-width: 110px !important;
}

/* After - Quasar 변수 사용 */
.my-input {
  --q-field-width: 110px;
}

/* 또는 더 구체적인 선택자 */
.my-component .q-input.my-input {
  width: 110px;
}
```

---

### 8.4 권장 컴포넌트 구조

```
components/
├── common/
│   ├── AppButton.vue          ← 신규
│   ├── AppInput.vue           ← 신규
│   ├── AppSelect.vue          ← 신규
│   ├── AppTable.vue           ← 신규 (q-table 래퍼)
│   ├── ConfirmDialog.vue      ← 신규
│   ├── LoadingOverlay.vue     ← 신규
│   └── index.ts               ← 전역 등록용
```

**전역 등록** (`main.ts` 또는 `boot/components.ts`):
```typescript
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

app.component('AppButton', AppButton)
app.component('AppInput', AppInput)
app.component('ConfirmDialog', ConfirmDialog)
```

---

### 8.5 Notification 표준화

#### 현재 문제점

**문제 1: 호출 방식 불일치 (110+ 호출)**

```typescript
// 패턴 1: 직접 호출 (PassSchedulePage.vue, Settings 등)
$q.notify({
  type: 'positive',
  message: '저장되었습니다'
})

// 패턴 2: 존재 확인 (TLEUploadContent.vue)
if ($q && $q.notify) {
  $q.notify({ type: 'warning', message: '경고' })
}

// 패턴 3: typeof 체크 (SelectScheduleContent.vue)
if ($q && typeof $q.notify === 'function') {
  $q.notify({ ... })
} else {
  console.warn('$q.notify is not available:', message)
}
```

**문제 2: 옵션 불일치**

| 파일 | position | timeout | 다른 옵션 |
|------|----------|---------|----------|
| MainLayout.vue | `'top'` | - | - |
| TLEUploadContent.vue | - | - | - |
| Settings 컴포넌트들 | - | - | - |

**문제 3: 메시지 스타일 불일치**

```typescript
// 같은 의미, 다른 표현
'저장되었습니다' vs '저장 완료' vs '설정이 저장되었습니다'
'저장에 실패했습니다' vs '저장 실패' vs '에러가 발생했습니다'
```

#### Task 1.8: useNotify Composable 개선

```typescript
// composables/common/useNotify.ts
import { useQuasar } from 'quasar'

type NotifyType = 'success' | 'error' | 'warning' | 'info'

interface NotifyOptions {
  message: string
  type?: NotifyType
  position?: 'top' | 'bottom' | 'top-right' | 'bottom-right'
  timeout?: number
}

// 기본 설정
const defaults = {
  position: 'top-right' as const,
  timeout: 3000,
}

// type → Quasar type 매핑
const typeMap: Record<NotifyType, string> = {
  success: 'positive',
  error: 'negative',
  warning: 'warning',
  info: 'info'
}

export function useNotify() {
  const $q = useQuasar()

  const notify = (options: NotifyOptions) => {
    $q.notify({
      type: typeMap[options.type ?? 'info'],
      message: options.message,
      position: options.position ?? defaults.position,
      timeout: options.timeout ?? defaults.timeout,
    })
  }

  // 편의 메서드
  const success = (message: string) => notify({ message, type: 'success' })
  const error = (message: string) => notify({ message, type: 'error' })
  const warning = (message: string) => notify({ message, type: 'warning' })
  const info = (message: string) => notify({ message, type: 'info' })

  return { notify, success, error, warning, info }
}
```

**사용 예시**:
```vue
<script setup lang="ts">
import { useNotify } from '@/composables/common/useNotify'

const { success, error, warning } = useNotify()

const saveSettings = async () => {
  try {
    await store.save()
    success('설정이 저장되었습니다')  // 통일된 메시지
  } catch (e) {
    error('설정 저장에 실패했습니다')
  }
}
</script>
```

**Before vs After**:
```typescript
// Before (각 파일마다 다름)
if ($q && $q.notify) {
  $q.notify({
    type: 'positive',
    message: '저장 완료',
    position: 'top'
  })
}

// After (통일된 사용)
const { success } = useNotify()
success('설정이 저장되었습니다')
```

#### Task 1.9: 메시지 상수화 (선택사항)

```typescript
// constants/messages.ts
export const MESSAGES = {
  SAVE: {
    SUCCESS: '저장되었습니다',
    ERROR: '저장에 실패했습니다',
  },
  DELETE: {
    SUCCESS: '삭제되었습니다',
    ERROR: '삭제에 실패했습니다',
    CONFIRM: '정말 삭제하시겠습니까?',
    NO_SELECTION: '삭제할 항목을 선택하세요',
  },
  VALIDATION: {
    REQUIRED: '필수 입력 항목입니다',
    INVALID: '유효하지 않은 값입니다',
  },
} as const
```

---

## 9. 공통 패턴 미정립 문제

### 9.1 Console 로깅 불통일

**현재 문제**: 파일마다 로깅 방식이 다름

```typescript
// 패턴 1: 이모지 없음 (times.ts)
console.error('로컬 시간 포맷팅 오류:', error)

// 패턴 2: 이모지 있음 (connectionManager.ts)
console.error(`❌ localStorage 저장 실패 (${key}):`, error)
console.log(`✅ 연결 상태 저장: ${state}`)

// 패턴 3: 태그 사용 (settingsService.ts)
console.log('📡 Feed 설정 API 응답:', data)
```

**해결책**: `utils/logger.ts` 생성 (Task 1.2에 포함)

---

### 9.2 Try-Catch 에러 처리 불통일

**현재 문제**: 3가지 패턴이 혼재

```typescript
// 패턴 1: throw만 (settingsService.ts)
} catch (error) {
  console.error('위치 설정 조회 실패:', error)
  throw error
}

// 패턴 2: 기본값 반환 (times.ts)
} catch (error) {
  console.error('포맷팅 오류:', error)
  return dateString  // 원본 반환
}

// 패턴 3: 상세 처리 (axios.ts)
} catch (error) {
  let errorMessage = '네트워크 연결에 문제가 있습니다.'
  if (error.response) { /* 상세 처리 */ }
  return Promise.reject(new Error(errorMessage))
}
```

**해결책**: `useErrorHandler.ts` 확장

```typescript
// composables/common/useErrorHandler.ts
export function useErrorHandler() {
  const handleError = (error: unknown, options?: {
    context?: string
    fallback?: () => void
    rethrow?: boolean
    notify?: boolean
  }) => {
    const message = extractErrorMessage(error)
    logger.error(options?.context ?? 'Error', message)

    if (options?.notify) {
      notify.error(message)
    }

    if (options?.rethrow) {
      throw error
    }

    options?.fallback?.()
  }

  return { handleError }
}
```

---

### 9.3 API 호출 패턴 불일치

**현재 문제**: `boot/axios.ts`에 인스턴스가 있으나 서비스에서 직접 axios 사용

```typescript
// boot/axios.ts - 중앙 인스턴스 ✅
const api = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 10000,
})

// settingsService.ts - 직접 axios 사용 ❌
import axios from 'axios'
const API_BASE_URL = getApiBaseUrl()
const response = await axios.get(`${API_BASE_URL}/settings/location`)
```

**해결책**: 모든 서비스에서 중앙 인스턴스 사용

```typescript
// services/api/settingsService.ts
import { api } from '@/boot/axios'

async getLocationSettings(): Promise<LocationSettings> {
  const response = await api.get('/settings/location')  // baseURL 자동 적용
  return response.data
}
```

---

### 9.4 타이머/상수 관리 분산

**현재 문제**: 매직넘버가 파일마다 하드코딩

```typescript
// icdStore.ts
const UPDATE_INTERVAL = 30  // 30ms

// passScheduleStore.ts
const DEBUG_LOG_INTERVAL = 10000  // 10초
const updateThrottle = 100  // 100ms

// ephemerisTrackStore.ts
const INITIAL_DELAY_MS = 10000
const MAX_JUMP_THRESHOLD = 10

// hardwareErrorLogStore.ts
if (errorLogs.value.length > 1000) {  // 하드코딩
```

**해결책**: `constants/` 폴더 생성

```typescript
// constants/timing.ts
export const TIMING = {
  UPDATE_INTERVAL: 30,        // WebSocket 업데이트 주기 (ms)
  THROTTLE: 100,              // 스로틀 기본값 (ms)
  DEBUG_LOG_INTERVAL: 10000,  // 디버그 로그 주기 (ms)
  INITIAL_DELAY: 10000,       // 초기 지연 (ms)
  NOTIFY_TIMEOUT: 3000,       // 알림 표시 시간 (ms)
} as const

// constants/limits.ts
export const LIMITS = {
  ERROR_LOG_MAX: 1000,        // 에러 로그 최대 개수
  PATH_POINTS_MAX: 50000,     // 경로 포인트 최대 개수
  RETRY_MAX: 3,               // 최대 재시도 횟수
} as const

// constants/storage-keys.ts
export const STORAGE_KEYS = {
  CONNECTION_STATE: 'acs-connection-state',
  LAST_DISCONNECT: 'acs-last-disconnect-time',
  AUTH_STATUS: 'auth-status',
  AUTH_ACTIVITY: 'auth-last-activity',
  PASS_SCHEDULE: 'pass-schedule-data',
  EPHEMERIS_DATA: 'ephemeris-designation-data',
  ERROR_LOGS: 'hardware-error-logs',
} as const

// constants/index.ts
export * from './timing'
export * from './limits'
export * from './storage-keys'
```

---

### 9.5 LocalStorage 접근 방식 혼재

**현재 문제**: 래퍼 함수와 직접 접근이 혼재

```typescript
// connectionManager.ts - 래퍼 사용 ✅
const safeSetItem = (key: string, value: string): boolean => {
  try {
    localStorage.setItem(key, value)
    return true
  } catch (error) {
    console.error(`❌ localStorage 저장 실패 (${key}):`, error)
    return false
  }
}

// auth.ts - 직접 접근 ❌
localStorage.setItem('auth-status', 'logged-in')
localStorage.setItem('auth-last-activity', Date.now().toString())
```

**해결책**: `utils/storage.ts` 생성

```typescript
// utils/storage.ts
import { STORAGE_KEYS } from '@/constants'
import { logger } from '@/utils/logger'

export const storage = {
  get<T>(key: string, defaultValue?: T): T | null {
    try {
      const item = localStorage.getItem(key)
      return item ? JSON.parse(item) : (defaultValue ?? null)
    } catch (error) {
      logger.error('Storage', `Get failed: ${key}`, error)
      return defaultValue ?? null
    }
  },

  set(key: string, value: unknown): boolean {
    try {
      localStorage.setItem(key, JSON.stringify(value))
      return true
    } catch (error) {
      logger.error('Storage', `Set failed: ${key}`, error)
      return false
    }
  },

  remove(key: string): boolean {
    try {
      localStorage.removeItem(key)
      return true
    } catch (error) {
      logger.error('Storage', `Remove failed: ${key}`, error)
      return false
    }
  }
}
```

---

### 9.6 날짜/숫자 포맷팅 불일치

**현재 문제**:

```typescript
// times.ts에 함수가 있지만 컴포넌트에서 직접 구현
// HardwareErrorLogPanel.vue
const formatTimestamp = (timestamp: number): string => {
  return new Date(timestamp).toLocaleString('ko-KR')
}

// 숫자 소수점 자릿수 불일치
return num.toFixed(2)   // DashboardPage
return val?.toFixed(6)  // EphemerisDesignationPage
```

**해결책**: `utils/formatters.ts` 확장

```typescript
// utils/formatters.ts
export const formatters = {
  // 날짜
  date: (date: Date | string | number) => {
    return new Date(date).toLocaleString('ko-KR')
  },

  // 숫자 - 용도별 포맷
  angle: (value: number) => value.toFixed(2),           // 각도: 2자리
  coordinate: (value: number) => value.toFixed(6),      // 좌표: 6자리
  percent: (value: number) => value.toFixed(1),         // 퍼센트: 1자리

  // 단위 포함
  degree: (value: number) => `${value.toFixed(2)}°`,
  speed: (value: number) => `${value.toFixed(2)}°/s`,
  current: (value: number) => `${value.toFixed(3)} A`,
}
```

---

### 9.7 권장 폴더 구조 (추가)

```
frontend/src/
├── constants/                   ← 신규
│   ├── index.ts
│   ├── timing.ts               # 타이머/인터벌 상수
│   ├── limits.ts               # 제한값 상수
│   ├── storage-keys.ts         # localStorage 키
│   └── messages.ts             # UI 메시지 상수
│
├── utils/
│   ├── logger.ts               ← 신규 (콘솔 로깅 통일)
│   ├── storage.ts              ← 신규 (localStorage 래퍼)
│   ├── formatters.ts           ← 확장 (날짜/숫자 포맷)
│   ├── times.ts                # 기존 유지
│   ├── connectionManager.ts    # 기존 유지
│   └── windowUtils.ts          # 기존 유지
│
├── composables/
│   └── common/
│       ├── useErrorHandler.ts  ← 확장
│       ├── useNotify.ts        ← 확장
│       └── useApi.ts           ← 신규 (API 호출 래퍼)
```

---

### 9.8 완료 기준

- [ ] `constants/` 폴더 생성 및 상수 이동
- [ ] `utils/logger.ts` 생성 및 적용 (63개 파일)
- [ ] `utils/storage.ts` 생성 및 적용 (18개 파일)
- [ ] `utils/formatters.ts` 확장
- [ ] 서비스에서 중앙 axios 인스턴스 사용
- [ ] `useErrorHandler.ts` 확장 및 적용

---

## 10. 전체 완료 기준 요약

### Phase 0 완료 기준
- [ ] 루트 파일 이동 (HardwareErrorLogPanel, hardwareErrorLogStore)
- [ ] 중복 파일 정리 (modeStore, example-store)
- [ ] composables 폴더 정리
- [ ] 빌드 성공

### Phase 1 완료 기준
- [ ] `useSettingsForm.ts` 생성 및 11개 컴포넌트 적용
- [ ] `logger.ts` 생성 및 적용
- [ ] `AppButton/AppInput/AppSelect/ConfirmDialog` 생성
- [ ] `useNotify.ts` 개선
- [ ] `constants/` 폴더 생성
- [ ] `storage.ts` 생성
- [ ] 하드코딩 색상 → 테마 변수 교체
- [ ] 기존 기능 정상 동작

### Phase 2 완료 기준
- [ ] icdStore 구조 개선
- [ ] 필터/페이징 composables 생성
- [ ] 서비스 axios 인스턴스 통일
- [ ] 테스트 커버리지 40%

---

**문서 버전**: 3.0.0
**작성일**: 2024-12
