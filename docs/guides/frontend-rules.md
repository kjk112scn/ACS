# Frontend 코딩 규칙

> Vue 3 + Quasar + TypeScript + Pinia

## 컴포넌트

### 필수 규칙
- `<script setup lang="ts">` 필수
- Props/Emits 타입 명시
- 컴포넌트명 PascalCase

### 스타일
- 색상: `var(--theme-*)` 사용, **하드코딩 금지**
- CSS-in-JS 금지, Quasar 클래스 활용
- scoped 스타일 권장

## 상태 관리 (Pinia)

### Setup Store 패턴
```typescript
export const useXxxStore = defineStore('xxx', () => {
  // State
  const data = ref<Data | null>(null)
  const loading = ref(false)

  // Getters (computed)
  const isEmpty = computed(() => data.value === null)

  // Actions (함수)
  async function fetchData() { ... }

  return { data, loading, isEmpty, fetchData }
})
```

### 주의사항
- Store 간 직접 참조 최소화
- 큰 Store는 기능별 분리

## Composables

### 필수 Composables
| Composable | 용도 |
|------------|------|
| `useErrorHandler` | 에러 처리 + 사용자 알림 |
| `useNotification` | 토스트/알림 표시 |
| `useLoading` | 로딩 상태 관리 |

### 패턴
```typescript
import { useErrorHandler } from '@/composables/useErrorHandler'

const { handleError } = useErrorHandler()

async function doSomething() {
  try {
    await api.call()
  } catch (error) {
    handleError(error, '작업 실패')
  }
}
```

## 에러 처리

- `useErrorHandler` composable 사용
- try-catch 시 **사용자 알림 필수**
- 조용한 실패 금지

## 파일 구조

```
pages/mode/xxxPage/
├── XxxPage.vue           # 메인 페이지
├── components/           # 페이지 전용 컴포넌트
│   ├── XxxTable.vue
│   └── XxxChart.vue
└── composables/          # 페이지 전용 로직
    └── useXxxTracking.ts
```

## 네이밍

| 타입 | 규칙 | 예시 |
|------|------|------|
| 컴포넌트 | PascalCase | `ScheduleTable.vue` |
| composable | camelCase, use 접두사 | `useErrorHandler.ts` |
| 스토어 | camelCase, Store 접미사 | `icdStore.ts` |
| 타입 | PascalCase | `AntennaStatus` |

## 테스트

```bash
npm run test           # Vitest 단위 테스트
npx vue-tsc --noEmit   # 타입 체크
```

---

**참조**: [ACS 패턴](../handbook/project/acs-patterns.md)
