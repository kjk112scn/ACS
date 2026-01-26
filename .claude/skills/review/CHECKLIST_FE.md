# Frontend 코드 리뷰 체크리스트

> Vue 3 + TypeScript + Pinia + Quasar 기반 프론트엔드 분석

---

## 1. 반응성 (Reactivity) 문제

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **FE-R01** | `ref()` vs `shallowRef()` 오용 | High | 30ms 업데이트 데이터에 `ref()` 사용 시 성능 저하 |
| **FE-R02** | `watchEffect` 무한 루프 | Critical | 내부에서 반응형 값 변경 시 무한 트리거 |
| **FE-R03** | `computed` 내 사이드이펙트 | High | computed 내에서 API 호출, 상태 변경 |
| **FE-R04** | 불필요한 `watch` deep 옵션 | Medium | `{ deep: true }` 남용으로 성능 저하 |
| **FE-R05** | 반응형 손실 (destructuring) | High | `const { value } = store` 패턴으로 반응성 끊김 |
| **FE-R06** | `toRefs` 누락 | Medium | store 값을 template에서 사용 시 반응성 유지 |
| **FE-R07** | shallowRef 부분 업데이트 | High | `state.value.field = x` (무시됨) |

### 탐지 코드 패턴

```typescript
// FE-R01: 30ms 데이터에 ref 사용 (BAD)
const antennaPosition = ref<AntennaPosition>({})
// GOOD
const antennaPosition = shallowRef<AntennaPosition>({})

// FE-R02: watchEffect 무한 루프 (BAD)
watchEffect(() => {
  someRef.value = calculation(someRef.value) // INFINITE LOOP
})

// FE-R05: 반응형 손실 (BAD)
const { currentMode } = modeStore // 반응성 없음
// GOOD
const currentMode = computed(() => modeStore.currentMode)

// FE-R07: shallowRef 부분 업데이트 (BAD)
state.value.field = newValue // 무시됨!
// GOOD
state.value = { ...state.value, field: newValue }
```

---

## 2. 상태 관리 (Pinia)

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **FE-S01** | Store 간 순환 참조 | Critical | storeA가 storeB 참조, storeB가 storeA 참조 |
| **FE-S02** | Action 내 동기화 누락 | High | 여러 상태 변경 시 중간 상태 노출 |
| **FE-S03** | Getter의 무거운 계산 | Medium | computed 대신 매번 계산 |
| **FE-S04** | Store 외부에서 직접 수정 | High | `store.state = newValue` 직접 할당 |
| **FE-S05** | 비동기 Action 에러 처리 누락 | High | try-catch 없이 API 호출 |
| **FE-S06** | Store 초기화 순서 의존성 | High | A store가 B store 초기화 전 접근 |

### ACS 특화 패턴

```typescript
// FE-S01: Store 순환 참조 (ACS 실제 위험)
// icdStore.ts
const passStore = usePassScheduleModeStore() // passScheduleStore 참조

// passScheduleStore.ts
const icdStore = useICDStore() // icdStore 참조 → 순환!

// 해결: 이벤트 버스 또는 composable로 분리
```

---

## 3. 컴포넌트 생명주기

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **FE-L01** | WebSocket 정리 누락 | Critical | `onUnmounted`에서 `ws.close()` 없음 |
| **FE-L02** | setInterval 정리 누락 | Critical | 타이머 ID 저장 안 함 |
| **FE-L03** | EventListener 정리 누락 | High | `removeEventListener` 누락 |
| **FE-L04** | 비동기 작업 후 unmount 체크 | High | unmount 후 상태 변경 시도 |
| **FE-L05** | keep-alive 상태 관리 | Medium | `onActivated`/`onDeactivated` 누락 |

### 탐지 코드 패턴

```typescript
// FE-L01: WebSocket 정리 누락 (BAD)
onMounted(() => {
  ws = new WebSocket(url)
})
// onUnmounted 누락!

// GOOD
onUnmounted(() => {
  ws?.close()
})

// FE-L04: unmount 후 상태 변경 (BAD)
onMounted(async () => {
  const data = await fetchData() // 3초 소요
  state.value = data // 이미 unmount 됐으면?
})

// GOOD
let isMounted = true
onMounted(async () => {
  const data = await fetchData()
  if (isMounted) {
    state.value = data
  }
})
onUnmounted(() => { isMounted = false })
```

---

## 4. 타입 안전성

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **FE-T01** | `any` 타입 사용 | High | `: any` 검색 |
| **FE-T02** | 타입 단언 남용 | Medium | `as SomeType` 검색 |
| **FE-T03** | Optional chaining 누락 | High | `obj.prop.nested` null 가능 시 |
| **FE-T04** | API 응답 타입 불일치 | High | BE 응답과 FE 타입 정의 차이 |
| **FE-T05** | 숫자/문자열 혼동 | Medium | `"123"` vs `123` 비교 |

---

## 5. ACS 도메인 특화

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **FE-D01** | 라디안/도 변환 누락 | Critical | API 라디안 → UI 도 변환 필수 |
| **FE-D02** | UTC/로컬 시간 혼동 | High | 시간 표시 시 변환 누락 |
| **FE-D03** | Train/Tilt 명명 불일치 | Low | 변수명 `train`, UI `Tilt` |
| **FE-D04** | 하드코딩 색상 | Medium | `#fff`, `rgb()` 대신 `var(--theme-*)` |

### ACS 단위 변환 검증

```typescript
// API 응답 (라디안)
const azimuthRad = response.azimuth

// UI 표시 (도) - 변환 필수!
const azimuthDeg = azimuthRad * (180 / Math.PI)

// 시간 변환 (UTC → 로컬)
const localTime = new Date(utcTime).toLocaleString()
```

---

## 6. 성능

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **FE-P01** | 과도한 렌더링 | Medium | computed 미사용, 불필요한 watch |
| **FE-P02** | 대형 리스트 가상화 미적용 | Medium | 1000+ 아이템 v-for |
| **FE-P03** | 불필요한 재계산 | Medium | 동일 값 반복 계산 |
| **FE-P04** | 이미지 최적화 미적용 | Low | lazy loading 없음 |
| **FE-P05** | 번들 크기 | Low | 불필요한 import |

---

## 7. 보안

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **FE-SEC01** | XSS 취약점 | Critical | v-html 사용 |
| **FE-SEC02** | 민감 정보 노출 | High | 콘솔에 토큰/비밀번호 출력 |
| **FE-SEC03** | 하드코딩 자격증명 | Critical | API 키 코드에 직접 |

---

## 검사 명령 예시

```bash
# FE-R01: ref vs shallowRef 확인
grep -r "ref<.*>" --include="*.ts" --include="*.vue"

# FE-T01: any 타입 검색
grep -r ": any" --include="*.ts" --include="*.vue"

# FE-D04: 하드코딩 색상 검색
grep -rE "#[0-9a-fA-F]{3,6}|rgb\(" --include="*.vue"

# FE-L01: onUnmounted 없는 WebSocket 검색
# (수동 확인 필요)
```

---

**버전:** 1.0.0
**작성일:** 2026-01-26
