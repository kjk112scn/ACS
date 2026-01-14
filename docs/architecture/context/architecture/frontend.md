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

## 폴더 구조

```
frontend/src/
├── components/          # 재사용 컴포넌트
│   ├── common/          # 공통 컴포넌트
│   ├── chart/           # 차트 컴포넌트
│   └── antenna/         # 안테나 관련
├── pages/               # 페이지 컴포넌트
│   └── mode/            # 모드별 페이지
│       ├── StepPage.vue
│       ├── SlewPage.vue
│       ├── EphemerisDesignationPage.vue
│       ├── PassSchedulePage.vue
│       └── SunTrackPage.vue
├── stores/              # Pinia 스토어
│   ├── icdStore.ts      # ICD 상태 (핵심)
│   ├── modeStore.ts     # 모드 상태
│   └── ...
├── services/            # API 서비스
│   ├── api.ts           # Axios 인스턴스
│   └── *Service.ts      # 도메인 서비스
├── composables/         # Composition 함수
│   ├── useErrorHandler.ts
│   ├── useNotification.ts
│   └── useLoading.ts
├── types/               # TypeScript 타입
│   ├── api.d.ts
│   └── icd.d.ts
└── utils/               # 유틸리티
    ├── conversion.ts    # 단위 변환
    └── format.ts        # 포맷팅
```

## 코딩 규칙

### 컴포넌트 작성
```vue
<script setup lang="ts">
// 필수: setup + lang="ts"
import { ref, computed } from 'vue'
import { useIcdStore } from '@/stores/icdStore'

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
import { ref, computed } from 'vue'

export const useExampleStore = defineStore('example', () => {
  // State
  const data = ref<Data | null>(null)
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

### Composables 활용
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

## 핵심 스토어

### icdStore (가장 중요)
```typescript
// 30ms마다 업데이트되는 안테나 상태
// 성능 최적화 필수

interface AntennaState {
  azimuth: number
  elevation: number
  train: number
  azMotorStatus: MotorStatus
  elMotorStatus: MotorStatus
  trainMotorStatus: MotorStatus
}

// shallowRef로 배치 업데이트
const antennaState = shallowRef<AntennaState>(...)
```

### modeStore
```typescript
// 현재 운용 모드 관리
interface ModeState {
  currentMode: Mode
  previousMode: Mode | null
  isTransitioning: boolean
}
```

## API 통신

### REST API
```typescript
// services/ephemerisService.ts
import { api } from './api'

export const ephemerisService = {
  async getTle(noradId: string) {
    return api.get<TleData>(`/ephemeris/tle/${noradId}`)
  },

  async predictPass(params: PredictParams) {
    return api.post<PassData[]>('/ephemeris/predict', params)
  }
}
```

### WebSocket
```typescript
// stores/icdStore.ts
const ws = new WebSocket('ws://localhost:8080/icd')

ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  antennaState.value = { ...data }
}
```

## 성능 최적화

### 리렌더링 최소화
- `shallowRef` 사용 (대규모 객체)
- `computed` 캐싱 활용
- `v-memo` 사용 (리스트)

### 번들 최적화
- 동적 import (코드 분할)
- Tree-shaking 확인
- 이미지 최적화

## 주의사항

- **단위 변환**: API 라디안 → UI 도(°)
- **시간 변환**: API UTC → UI 로컬 시간
- **30ms 업데이트**: 성능 프로파일링 필수
- **테마 변수**: 색상 하드코딩 금지

## 참조

- [ICD 프로토콜](../domain/icd-protocol.md)
- [모드 시스템](../domain/mode-system.md)
- [Backend 아키텍처](backend.md)

---

**최종 수정:** 2026-01-14
