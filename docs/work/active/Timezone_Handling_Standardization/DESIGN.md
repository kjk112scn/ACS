# Timezone Handling Standardization - 상세 설계

## 1. 설계 의도

### Why (왜 이렇게 설계했는가)

1. **정확성**: IANA timezone은 DST(서머타임)를 자동 처리
2. **폐쇄망 지원**: 브라우저 내장 Intl API로 인터넷 불필요
3. **전 세계 지원**: 설치 위치 불명확 → 모든 timezone 지원 필요
4. **CLAUDE.md 준수**: "내부 UTC, 표시 로컬" 규칙 완전 적용

### 대안 분석

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| UTC Offset (+9) | 구현 간단 | DST 미지원, 수동 변경 | ❌ |
| IANA + 자동감지 | DST 자동, 전체 지원 | 선택지 많음 | ✅ |
| 서버에서 변환 | FE 단순화 | 인증 필요, 서버 부하 | ❌ |

---

## 2. 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│  데이터 흐름                                                             │
│                                                                         │
│  [DB]              [Backend]              [API]             [Frontend]  │
│                                                                         │
│  TIMESTAMPTZ  →   OffsetDateTime(UTC) →  ISO 8601 UTC  →   사용자 TZ   │
│  (UTC 저장)        (UTC 처리)             (Z suffix)       (Intl 변환)  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Backend 변경사항

### 3.1 긴급 수정 (Asia/Seoul 하드코딩 제거)

**파일**: `SunTrackService.kt:271`

```kotlin
// Before (❌)
.withZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))

// After (✅)
.withZoneSameInstant(ZoneOffset.UTC)
// 또는 설정에서 로드
// .withZoneSameInstant(GlobalData.Time.displayTimeZone)
```

### 3.2 systemDefault() 제거

**파일**: `GlobalData.kt`

```kotlin
// Before (❌)
@Volatile var serverTimeZone: ZoneId = ZoneId.systemDefault()
@Volatile var clientTimeZone: ZoneId = serverTimeZone

// After (✅)
// 서버는 항상 UTC
val serverTimeZone: ZoneId = ZoneOffset.UTC
// 클라이언트 표시용은 설정에서 로드 (또는 제거)
```

**파일**: `LoggingController.kt`

```kotlin
// Before (❌)
LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

// After (✅)
LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
```

### 3.3 GlobalExceptionHandler 수정

```kotlin
// Before (❌)
"timestamp" to LocalDateTime.now().toString()

// After (✅)
"timestamp" to OffsetDateTime.now(ZoneOffset.UTC).toString()
```

### 3.4 DB 연결 설정 추가

**파일**: `application-with-db.properties`

```properties
# Before
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/acs

# After
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/acs?timezone=UTC
```

---

## 4. Frontend 변경사항

### 4.1 신규 파일

#### stores/common/timezoneStore.ts

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useTimezoneStore = defineStore('timezone', () => {
  // 자동감지 사용 여부
  const useAutoDetect = ref(
    localStorage.getItem('timezoneAutoDetect') !== 'false'
  )

  // 감지된 timezone
  const detectedTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone

  // 수동 선택 timezone
  const manualTimezone = ref(
    localStorage.getItem('userTimezone') || detectedTimezone
  )

  // 현재 사용 timezone
  const currentTimezone = computed(() =>
    useAutoDetect.value ? detectedTimezone : manualTimezone.value
  )

  // timezone 설정
  function setTimezone(tz: string) {
    manualTimezone.value = tz
    localStorage.setItem('userTimezone', tz)
  }

  // 자동감지 토글
  function setAutoDetect(auto: boolean) {
    useAutoDetect.value = auto
    localStorage.setItem('timezoneAutoDetect', auto.toString())
  }

  return {
    useAutoDetect,
    detectedTimezone,
    manualTimezone,
    currentTimezone,
    setTimezone,
    setAutoDetect
  }
})
```

#### composables/useTimezone.ts

```typescript
import { computed } from 'vue'

export const useTimezone = () => {
  // 자주 사용하는 timezone
  const favoriteTimezones = [
    { value: 'UTC', label: 'UTC', offset: 'UTC+0' },
    { value: 'Asia/Seoul', label: 'Asia/Seoul', offset: 'UTC+9' },
    { value: 'Asia/Tokyo', label: 'Asia/Tokyo', offset: 'UTC+9' },
    { value: 'America/New_York', label: 'America/New_York', offset: 'UTC-5' },
    { value: 'Europe/London', label: 'Europe/London', offset: 'UTC+0' },
  ]

  // 전체 timezone 목록 (브라우저 내장)
  const allTimezones = computed(() => {
    return Intl.supportedValuesOf('timeZone').map(tz => ({
      value: tz,
      label: tz,
      offset: getTimezoneOffset(tz)
    }))
  })

  // UTC offset 계산
  function getTimezoneOffset(tz: string): string {
    const now = new Date()
    const formatter = new Intl.DateTimeFormat('en', {
      timeZone: tz,
      timeZoneName: 'shortOffset'
    })
    const parts = formatter.formatToParts(now)
    return parts.find(p => p.type === 'timeZoneName')?.value || ''
  }

  return { favoriteTimezones, allTimezones, getTimezoneOffset }
}
```

### 4.2 수정 파일

#### utils/times.ts 확장

```typescript
import { useTimezoneStore } from '@/stores/common/timezoneStore'

// 기존 함수에 timezone 인자 추가
export const formatToLocalTime = (
  dateString: string,
  timezone?: string
): string => {
  const tz = timezone || useTimezoneStore().currentTimezone
  const date = new Date(dateString)

  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: tz,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date)
}
```

### 4.3 하드코딩 수정

| 파일 | 현재 | 수정 |
|------|------|------|
| `times.ts:102` | `'ko-KR'` | `getCurrentLanguage()` 기반 |
| `logger.ts:48` | `'ko-KR'` | `getCurrentLanguage()` 기반 |
| `HardwareErrorLogPanel.vue:624` | `'en-US'` | `getCurrentLanguage()` 기반 |

---

## 5. UI 설계

### 5.1 배치 위치

**설정 > 지역 설정** (Language 탭 확장)

```
┌─────────────────────────────────────────────────────────────┐
│ 지역 설정 (Regional Settings)                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ▼ 언어 (Language)                                           │
│ ┌─────────────────────┐ ┌─────────────────────┐             │
│ │ 한국어         [✓] │ │ English        [ ] │             │
│ └─────────────────────┘ └─────────────────────┘             │
│                                                             │
│ ─────────────────────────────────────────────────────────── │
│                                                             │
│ ▼ 시간대 (Timezone)                                         │
│                                                             │
│ [✓] 자동 감지 (브라우저 시간대 사용)                          │
│     현재: Asia/Seoul (UTC+9)                                │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🔍 시간대 검색...                                    ▼ │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ [i] 시스템 시간은 UTC로 저장되며, 선택한 시간대로 표시됩니다   │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 검색형 Select

- Quasar `q-select` with `use-input`
- 자주 쓰는 timezone 상단 고정 (★ 표시)
- 대륙별 그룹화 (Asia, America, Europe...)
- 실시간 offset 표시

---

## 6. 테스트 계획

### 단위 테스트
- [ ] `timezoneStore` 상태 관리
- [ ] `useTimezone` composable 함수
- [ ] `times.ts` 유틸 함수 (timezone 인자)

### 통합 테스트
- [ ] API UTC 응답 → FE timezone 변환
- [ ] localStorage 저장/복원
- [ ] 자동감지 ↔ 수동선택 전환

### E2E 테스트
- [ ] timezone 변경 → 전체 UI 반영
- [ ] 브라우저 새로고침 후 설정 유지

### 수동 테스트
- [ ] 다양한 timezone 설정 후 시간 표시 확인
- [ ] 폐쇄망 환경 (인터넷 없이) 동작 확인

---

## 7. 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-01-20 | 초기 설계 |