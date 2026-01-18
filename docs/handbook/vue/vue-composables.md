# Vue Composables 패턴

> 재사용 가능한 로직을 함수로 분리하는 방법

## Composable이란?

React의 Custom Hook과 비슷한 개념입니다.
**상태 + 로직**을 함수로 묶어서 여러 컴포넌트에서 재사용합니다.

```typescript
// 일반 함수와의 차이
function add(a, b) { return a + b }  // 일반 함수: 상태 없음

function useCounter() {               // Composable: 상태 + 로직
  const count = ref(0)               // 반응형 상태
  const increment = () => count.value++
  return { count, increment }
}
```

---

## 기본 구조

```typescript
// composables/useXxx.ts
import { ref, computed, onMounted, onUnmounted } from 'vue'

export function useXxx(options?: XxxOptions) {
  // 1. 상태 정의
  const state = ref<State>(initialValue)
  const loading = ref(false)
  const error = ref<Error | null>(null)

  // 2. 계산된 값
  const derived = computed(() => transform(state.value))

  // 3. 메서드
  function doSomething() {
    // ...
  }

  // 4. 생명주기 (필요시)
  onMounted(() => {
    // 초기화
  })

  onUnmounted(() => {
    // 정리
  })

  // 5. 반환
  return {
    // 상태
    state,
    loading,
    error,
    // 계산값
    derived,
    // 메서드
    doSomething
  }
}
```

---

## ACS 프로젝트의 Composables

### 1. useErrorHandler
에러 처리 + 사용자 알림

```typescript
// composables/useErrorHandler.ts
export function useErrorHandler() {
  const { notify } = useQuasar()

  function handleError(error: unknown, message?: string) {
    const errorMessage = message || getErrorMessage(error)

    // 사용자 알림
    notify({
      type: 'negative',
      message: errorMessage,
      position: 'top'
    })

    // 콘솔 로깅
    console.error('[Error]', error)
  }

  function showError(message: string) {
    notify({
      type: 'negative',
      message,
      position: 'top'
    })
  }

  function showSuccess(message: string) {
    notify({
      type: 'positive',
      message,
      position: 'top'
    })
  }

  return { handleError, showError, showSuccess }
}
```

**사용**:
```vue
<script setup>
const { handleError, showSuccess } = useErrorHandler()

async function save() {
  try {
    await api.save(data)
    showSuccess('저장 완료')
  } catch (error) {
    handleError(error, '저장 실패')
  }
}
</script>
```

---

### 2. useLoading
로딩 상태 관리

```typescript
// composables/useLoading.ts
export function useLoading() {
  const isLoading = ref(false)

  async function withLoading<T>(fn: () => Promise<T>): Promise<T> {
    isLoading.value = true
    try {
      return await fn()
    } finally {
      isLoading.value = false
    }
  }

  return { isLoading, withLoading }
}
```

**사용**:
```vue
<script setup>
const { isLoading, withLoading } = useLoading()

async function fetchData() {
  const result = await withLoading(() => api.getData())
  // isLoading이 자동으로 true → false
}
</script>

<template>
  <q-spinner v-if="isLoading" />
  <div v-else>{{ data }}</div>
</template>
```

---

### 3. useNotification
알림 메시지

```typescript
// composables/useNotification.ts
export function useNotification() {
  const { notify } = useQuasar()

  function success(message: string) {
    notify({ type: 'positive', message, position: 'top' })
  }

  function error(message: string) {
    notify({ type: 'negative', message, position: 'top' })
  }

  function warning(message: string) {
    notify({ type: 'warning', message, position: 'top' })
  }

  function info(message: string) {
    notify({ type: 'info', message, position: 'top' })
  }

  return { success, error, warning, info }
}
```

---

### 4. useKeyboardNavigation
키보드 단축키

```typescript
// composables/useKeyboardNavigation.ts
interface KeyboardOptions {
  enabled?: Ref<boolean>  // 활성화 조건
}

export function useKeyboardNavigation(options?: KeyboardOptions) {
  const handlers = new Map<string, () => void>()

  function onEscape(handler: () => void) {
    handlers.set('Escape', handler)
  }

  function onEnter(handler: () => void) {
    handlers.set('Enter', handler)
  }

  function onCtrlEnter(handler: () => void) {
    handlers.set('Ctrl+Enter', handler)
  }

  function bind(key: string, handler: () => void) {
    handlers.set(key, handler)
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (options?.enabled && !options.enabled.value) return

    const key = event.ctrlKey ? `Ctrl+${event.key}` : event.key
    const handler = handlers.get(key)

    if (handler) {
      event.preventDefault()
      handler()
    }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleKeyDown)
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeyDown)
  })

  return { onEscape, onEnter, onCtrlEnter, bind }
}
```

**사용**:
```vue
<script setup>
const dialogVisible = ref(false)

const { onEscape, onCtrlEnter } = useKeyboardNavigation({
  enabled: dialogVisible  // 다이얼로그 열렸을 때만
})

onEscape(() => dialogVisible.value = false)
onCtrlEnter(() => submitForm())
</script>
```

---

### 5. useOffsetControls
오프셋 제어 로직 (ACS 특화)

```typescript
// composables/useOffsetControls.ts
export function useOffsetControls() {
  const icdStore = useIcdStore()

  // 상태
  const azOffset = ref(0)
  const elOffset = ref(0)
  const trainOffset = ref(0)

  // 계산값
  const totalOffset = computed(() => ({
    az: azOffset.value,
    el: elOffset.value,
    train: trainOffset.value
  }))

  // 오프셋 적용
  async function applyOffset(axis: 'az' | 'el' | 'train', value: number) {
    try {
      await api.setOffset(axis, value)
      if (axis === 'az') azOffset.value = value
      if (axis === 'el') elOffset.value = value
      if (axis === 'train') trainOffset.value = value
    } catch (error) {
      throw error
    }
  }

  // 오프셋 리셋
  async function resetOffset(axis?: 'az' | 'el' | 'train') {
    if (axis) {
      await applyOffset(axis, 0)
    } else {
      await Promise.all([
        applyOffset('az', 0),
        applyOffset('el', 0),
        applyOffset('train', 0)
      ])
    }
  }

  return {
    azOffset,
    elOffset,
    trainOffset,
    totalOffset,
    applyOffset,
    resetOffset
  }
}
```

---

## Composable 작성 가이드

### 네이밍 규칙
```typescript
// ✅ 좋은 예: use 접두사
useErrorHandler()
useLoading()
useKeyboardNavigation()

// ❌ 나쁜 예
errorHandler()
loadingState()
keyboardNav()
```

### 파일 위치
```
composables/
├── useErrorHandler.ts      # 에러 처리
├── useLoading.ts           # 로딩 상태
├── useNotification.ts      # 알림
├── useKeyboardNavigation.ts # 키보드
└── index.ts                # re-export
```

### index.ts로 re-export
```typescript
// composables/index.ts
export * from './useErrorHandler'
export * from './useLoading'
export * from './useNotification'
export * from './useKeyboardNavigation'
```

**사용**:
```typescript
import { useErrorHandler, useLoading } from '@/composables'
```

---

## 자주 쓰는 패턴

### 패턴 1: API 호출 래퍼
```typescript
export function useFetch<T>(url: string) {
  const data = ref<T | null>(null)
  const loading = ref(false)
  const error = ref<Error | null>(null)

  async function fetch() {
    loading.value = true
    error.value = null
    try {
      data.value = await api.get(url)
    } catch (e) {
      error.value = e as Error
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, fetch }
}
```

### 패턴 2: 디바운스/쓰로틀
```typescript
export function useDebounce<T>(value: Ref<T>, delay: number) {
  const debouncedValue = ref(value.value) as Ref<T>

  let timeout: ReturnType<typeof setTimeout>

  watch(value, (newValue) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => {
      debouncedValue.value = newValue
    }, delay)
  })

  return debouncedValue
}
```

### 패턴 3: 로컬 스토리지 동기화
```typescript
export function useLocalStorage<T>(key: string, defaultValue: T) {
  const stored = localStorage.getItem(key)
  const data = ref<T>(stored ? JSON.parse(stored) : defaultValue)

  watch(data, (newValue) => {
    localStorage.setItem(key, JSON.stringify(newValue))
  }, { deep: true })

  return data
}
```

### 패턴 4: 이벤트 리스너
```typescript
export function useWindowSize() {
  const width = ref(window.innerWidth)
  const height = ref(window.innerHeight)

  function update() {
    width.value = window.innerWidth
    height.value = window.innerHeight
  }

  onMounted(() => window.addEventListener('resize', update))
  onUnmounted(() => window.removeEventListener('resize', update))

  return { width, height }
}
```

---

## Composable vs Store (Pinia)

| 특성 | Composable | Store |
|------|-----------|-------|
| 상태 범위 | 컴포넌트 인스턴스별 | 전역 싱글톤 |
| 데이터 공유 | 각 호출마다 새 인스턴스 | 모든 컴포넌트에서 공유 |
| 용도 | 로직 재사용 | 전역 상태 관리 |
| 예시 | useLoading, useKeyboard | userStore, settingsStore |

**언제 뭘 쓸까?**
```
Composable 사용:
├── 여러 곳에서 같은 로직 필요 (에러 처리, 로딩)
├── 각 컴포넌트가 독립적인 상태 필요
└── 유틸리티성 기능

Store 사용:
├── 전역에서 같은 데이터 공유 (사용자 정보, 설정)
├── 컴포넌트 간 상태 동기화 필요
└── 앱 전체에 영향을 주는 상태
```

---

## 흔한 실수

### ❌ 반응형 잃음
```typescript
// 나쁜 예: 반응형 풀림
function useBad() {
  const state = ref({ count: 0 })
  return { count: state.value.count }  // 반응형 아님!
}

// 좋은 예: 반응형 유지
function useGood() {
  const state = ref({ count: 0 })
  return { state }  // 또는 toRefs(state.value)
}
```

### ❌ 클린업 안 함
```typescript
// 나쁜 예: 메모리 누수
function useBad() {
  onMounted(() => {
    window.addEventListener('resize', handler)
  })
  // 제거 안 함!
}

// 좋은 예: 정리
function useGood() {
  onMounted(() => {
    window.addEventListener('resize', handler)
  })
  onUnmounted(() => {
    window.removeEventListener('resize', handler)
  })
}
```

---

**이전**: [vue-reactivity.md](./vue-reactivity.md) - 반응형 시스템
**다음**: [typescript-patterns.md](./typescript-patterns.md) - TypeScript 패턴
