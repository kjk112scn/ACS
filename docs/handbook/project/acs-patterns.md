# ACS 프로젝트 코드 패턴

> 이 프로젝트에서 자주 사용되는 패턴 모음

## Frontend 패턴

### 1. Composable 패턴

재사용 가능한 로직을 분리하는 Vue 3 패턴

```typescript
// composables/useOffsetControls.ts
export function useOffsetControls() {
  const offset = ref(0)

  const increment = () => { offset.value++ }
  const decrement = () => { offset.value-- }

  return { offset, increment, decrement }
}

// 사용
const { offset, increment } = useOffsetControls()
```

**ACS 예시**: `useErrorHandler`, `useNotification`, `useLoading`

---

### 2. Store 패턴 (Pinia Setup Store)

```typescript
// stores/xxx/xxxStore.ts
export const useXxxStore = defineStore('xxx', () => {
  // State
  const data = ref<Data | null>(null)
  const loading = ref(false)

  // Getters (computed)
  const isEmpty = computed(() => data.value === null)

  // Actions (함수)
  async function fetchData() {
    loading.value = true
    try {
      data.value = await api.getData()
    } finally {
      loading.value = false
    }
  }

  return { data, loading, isEmpty, fetchData }
})
```

**ACS 예시**: `icdStore`, `passScheduleStore`

---

### 3. 테마 색상 사용

```vue
<!-- ❌ 하드코딩 -->
<div style="color: #ff5722">

<!-- ✅ 테마 변수 -->
<div :style="{ color: 'var(--theme-accent-color)' }">
```

**사용 가능한 변수**: [_variables.scss](../../../frontend/src/css/_variables.scss) 참조

---

### 4. 에러 처리 패턴

```typescript
import { useErrorHandler } from '@/composables/useErrorHandler'

const { handleError, showError } = useErrorHandler()

async function doSomething() {
  try {
    await api.call()
  } catch (error) {
    handleError(error, '작업 실패')  // 사용자 알림 + 로깅
  }
}
```

---

## Backend 패턴

### 1. 계층 구조

```
Controller → Service → Repository
    ↓           ↓
   DTO       Algorithm (순수 함수)
```

```kotlin
// Controller: 요청/응답 변환
@RestController
class EphemerisController(private val service: EphemerisService) {
    @GetMapping("/tle/{id}")
    fun getTLE(@PathVariable id: String): Mono<TLEResponse> {
        return service.getTLE(id).map { TLEResponse.from(it) }
    }
}

// Service: 비즈니스 로직
@Service
class EphemerisService(
    private val calculator: OrbitCalculator  // Algorithm
) {
    fun getTLE(id: String): Mono<TLE> { ... }
}

// Algorithm: 순수 계산 (외부 의존 없음)
class OrbitCalculator {
    fun calculatePosition(tle: TLE, time: Instant): Position { ... }
}
```

---

### 2. Null 안전 패턴

```kotlin
// Early Return
fun process(data: Data?) {
    val d = data ?: return
    // d는 null 아님 보장
}

// let 블록
currentPass?.let { pass ->
    // pass는 null 아님 보장
    updateStatus(pass)
}

// Elvis + 기본값
val name = user?.name ?: "Unknown"
```

---

### 3. 리액티브 에러 처리

```kotlin
findUser(id)
    .switchIfEmpty(Mono.error(NotFoundException("User not found")))
    .onErrorResume(DbException::class.java) {
        logger.error("DB 오류", it)
        Mono.error(InternalException("Database error"))
    }
```

---

### 4. subscribe 에러 핸들러

```kotlin
// 항상 에러 핸들러 포함
eventBus.subscribe(
    { event -> handle(event) },
    { error -> logger.error("처리 실패: {}", error.message, error) }
)
```

---

### 5. Graceful Shutdown

```kotlin
@Service
class MyService {
    @PreDestroy
    fun cleanup() {
        logger.info("정리 시작")
        saveRemainingData()
        closeConnections()
        logger.info("정리 완료")
    }
}
```

---

## 공통 패턴

### 1. 각도 단위 변환

```kotlin
// 내부: 라디안
val azimuthRad = 1.5708  // π/2

// 표시: 도
val azimuthDeg = Math.toDegrees(azimuthRad)  // 90.0
```

```typescript
// TypeScript
const azimuthDeg = azimuthRad * (180 / Math.PI)
```

---

### 2. 시간 처리

```kotlin
// 내부: UTC
val utc = Instant.now()

// 표시: 로컬
val local = utc.atZone(ZoneId.systemDefault())
```

---

### 3. Train/Tilt 구분

```kotlin
// 변수명: train
val trainAngle = calculateTrainAngle()

// UI 표시: Tilt
// "Tilt: ${trainAngle}°"
```

---

## 파일 구조 패턴

### Frontend
```
pages/mode/xxxPage/
├── XxxPage.vue           # 메인 페이지
├── components/           # 페이지 전용 컴포넌트
│   ├── XxxTable.vue
│   └── XxxChart.vue
└── composables/          # 페이지 전용 로직
    └── useXxxTracking.ts
```

### Backend
```
service/xxx/
├── XxxService.kt         # 메인 서비스
├── XxxStateMachine.kt    # 상태 관리 (필요 시)
└── XxxDataBatcher.kt     # 배치 처리 (필요 시)
```

---

## 네이밍 컨벤션

### Frontend (TypeScript)
| 타입 | 규칙 | 예시 |
|------|------|------|
| 컴포넌트 | PascalCase | `ScheduleTable.vue` |
| composable | camelCase, use 접두사 | `useErrorHandler.ts` |
| 스토어 | camelCase, Store 접미사 | `icdStore.ts` |
| 타입 | PascalCase | `AntennaStatus` |

### Backend (Kotlin)
| 타입 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `EphemerisService` |
| 함수 | camelCase | `calculatePosition()` |
| 상수 | UPPER_SNAKE | `MAX_RETRY_COUNT` |
| DTO | PascalCase, 용도 접미사 | `TrackingRequest`, `TLEResponse` |

---

**업데이트**: 새로운 패턴 발견 시 이 문서에 추가
