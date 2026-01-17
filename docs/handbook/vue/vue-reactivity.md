# Vue 반응형 시스템

> 데이터가 바뀌면 화면이 자동으로 바뀌는 마법

## 핵심 개념

Vue는 **Proxy**를 사용해서 데이터 변경을 감지합니다.
변경이 감지되면 관련 UI가 자동으로 업데이트됩니다.

```
데이터 변경 → Vue가 감지 → DOM 자동 업데이트
```

---

## ref vs reactive

### `ref()` - 단일 값

```typescript
import { ref } from 'vue'

const count = ref(0)          // 숫자
const name = ref('Kim')       // 문자열
const user = ref({ age: 30 }) // 객체도 가능

// 접근/수정 시 .value 필요
console.log(count.value)  // 0
count.value++             // 1
```

**템플릿에서는 .value 불필요**:
```vue
<template>
  <div>{{ count }}</div>  <!-- 자동 unwrap -->
</template>
```

---

### `reactive()` - 객체 전용

```typescript
import { reactive } from 'vue'

const state = reactive({
  count: 0,
  user: { name: 'Kim' }
})

// .value 불필요
console.log(state.count)  // 0
state.count++             // 1
state.user.name = 'Lee'   // 중첩 객체도 반응형
```

**⚠️ 주의**: 재할당하면 반응형 잃음
```typescript
// ❌ 나쁜 예
state = { count: 1 }  // 반응형 끊김!

// ✅ 좋은 예
Object.assign(state, { count: 1 })
```

---

### 언제 뭘 쓸까?

| 상황 | 선택 | 이유 |
|------|------|------|
| 단일 값 (숫자, 문자열) | `ref` | primitive는 reactive 못 씀 |
| 폼 데이터, 상태 객체 | `reactive` | .value 안 써도 됨 |
| API 응답 저장 | `ref` | 객체 전체 교체 용이 |
| 컴포넌트 props처럼 | `ref` | 재할당 가능 |

**팁**: 헷갈리면 그냥 `ref` 쓰세요. 항상 됩니다.

---

## shallowRef - 성능 최적화

깊은 반응형이 필요 없을 때 사용

```typescript
import { shallowRef } from 'vue'

const data = shallowRef({ nested: { value: 1 } })

// ❌ 반응 안 함 (깊은 변경)
data.value.nested.value = 2

// ✅ 반응함 (최상위 교체)
data.value = { nested: { value: 2 } }
```

**ACS에서 사용하는 이유**:
- icdStore: 30ms마다 데이터 업데이트
- 81개 ref가 매번 깊은 비교하면 성능 저하
- shallowRef로 최상위만 비교 → 빠름

---

## computed - 계산된 값

다른 반응형 값에서 **파생**되는 값

```typescript
import { ref, computed } from 'vue'

const price = ref(100)
const quantity = ref(3)

// 자동 계산 + 캐싱
const total = computed(() => price.value * quantity.value)

console.log(total.value)  // 300
price.value = 200
console.log(total.value)  // 600 (자동 재계산)
```

**특징**:
- 의존 값이 안 바뀌면 재계산 안 함 (캐싱)
- 읽기 전용 (기본)

---

## watch - 변경 감지

값이 바뀔 때 **부수 효과** 실행

### 기본 사용
```typescript
import { ref, watch } from 'vue'

const query = ref('')

watch(query, (newVal, oldVal) => {
  console.log(`${oldVal} → ${newVal}`)
  // API 호출, 로깅 등
})
```

### 즉시 실행
```typescript
watch(query, (val) => {
  fetchData(val)
}, { immediate: true })  // 선언 즉시 1회 실행
```

### 깊은 감시 (deep)
```typescript
const user = ref({ profile: { name: 'Kim' } })

// ❌ profile.name 변경 감지 못함
watch(user, () => { ... })

// ✅ 내부까지 감시
watch(user, () => { ... }, { deep: true })
```

**⚠️ deep watch 주의**:
- 큰 객체면 성능 저하
- 무한 루프 위험 (watch 안에서 같은 값 변경 시)

### 특정 필드만 감시 (권장)
```typescript
// deep 대신 getter 사용
watch(
  () => user.value.profile.name,
  (newName) => { ... }
)

// 여러 필드
watch(
  [() => user.value.name, () => user.value.age],
  ([name, age]) => { ... }
)
```

---

## watchEffect - 자동 의존성

의존성을 자동으로 추적

```typescript
import { ref, watchEffect } from 'vue'

const count = ref(0)
const name = ref('Kim')

watchEffect(() => {
  // count와 name 모두 자동 추적
  console.log(`${name.value}: ${count.value}`)
})

count.value++  // 트리거됨
name.value = 'Lee'  // 트리거됨
```

---

## ACS 프로젝트 실제 예시

### icdStore에서
```typescript
// stores/icd/icdStore.ts
const antennaStatus = shallowRef<AntennaStatus>({...})

// 30ms마다 업데이트
function updateFromWebSocket(data: any) {
  // 전체 객체 교체 (shallowRef니까)
  antennaStatus.value = { ...data }
}
```

### 컴포넌트에서
```typescript
// DashboardPage.vue
const { azimuth, elevation } = storeToRefs(useIcdStore())

// computed로 파생 값
const azimuthDegrees = computed(() =>
  (azimuth.value * 180 / Math.PI).toFixed(2)
)

// 특정 값 변경 감시
watch(
  () => icdStore.connectionStatus,
  (status) => {
    if (status === 'disconnected') {
      showWarning('연결 끊김')
    }
  }
)
```

---

## 흔한 실수

### ❌ reactive 재할당
```typescript
let state = reactive({ count: 0 })
state = reactive({ count: 1 })  // 반응형 끊김!
```

### ❌ 구조분해 시 반응형 손실
```typescript
const state = reactive({ count: 0 })
const { count } = state  // ❌ count는 그냥 숫자
count++  // 반응 안 함

// ✅ toRefs 사용
const { count } = toRefs(state)
count.value++  // 반응함
```

### ❌ watch 안에서 무한 루프
```typescript
watch(user, () => {
  user.value.updated = Date.now()  // ❌ 무한 루프!
}, { deep: true })
```

---

## 요약 표

| API | 용도 | .value | 깊은 반응형 |
|-----|------|:------:|:---------:|
| `ref()` | 모든 타입 | ✅ | ✅ |
| `reactive()` | 객체만 | ❌ | ✅ |
| `shallowRef()` | 성능 최적화 | ✅ | ❌ |
| `computed()` | 파생 값 | ✅ | - |
| `watch()` | 부수효과 | - | 선택 |

---

**다음 학습**: [vue-composables.md](./vue-composables.md) - Composable 패턴
