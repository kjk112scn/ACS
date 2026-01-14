# RFC-008: 프론트엔드 구조화

> **버전**: 1.7.0 | **작성일**: 2026-01-14
> **상태**: Draft | **우선순위**: P2
> **역할**: FE 전체 구조화 (Composable 추출, 대형 파일 분리, console.log 정리, 코드 품질, 성능 최적화, icdStore 최적화, Dead Code 정리, 메모리 누수 수정, CSS 품질, **Offset Control 통합**)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.6.0 | 2026-01-14 | **신규 추가**: Offset Control 통합 (3곳 분산 → 1곳), 추가 Composables, FeedPage 분리 계획, 인라인 style 정리, windowUtils 분해 계획, 차트 최적화 |
| 1.5.0 | 2026-01-14 | 전문가 검증 결과 반영: 수치 통일(1,513개/520건/280건), modeStore 보류, icdStore ref 81개 명확화 |
| 1.4.0 | 2026-01-13 | 전수조사 결과 반영: Phase 6-8 추가 (Dead Code, 메모리 누수, CSS !important) |
| 1.3.0 | 2026-01-13 | 문서 역할 명확화: FE 관련 모든 작업 통합 (RFC-004 Phase 4-5 FE 작업 이관) |
| 1.2.0 | 2026-01-13 | icdStore 심층 성능 분석 추가: 175개 ref, 13개 비트 파싱 함수, 22개 computed, 최적화 방안 상세화 |
| 1.1.0 | 2026-01-13 | 심층 분석 결과 추가: 하드코딩 색상 300+건, as 타입 단언 80+건, 성능 이슈 (watch 62개, icdStore 100+ ref), 번들 최적화 항목, 관련 RFC 섹션 확장 |
| 1.0.0 | 2026-01-13 | 초기 작성: 대형 파일 분석, 중복 패턴 식별, Composable 추출 계획 |

---

## 1. 배경 (Context)

### 왜 이 변경이 필요한가?

현재 ACS 프론트엔드는 다음과 같은 구조적 문제를 가지고 있습니다:

| 문제 | 영향 |
|------|------|
| 초대형 파일 | 4,000줄+ 페이지 2개, 유지보수 어려움 |
| 코드 중복 | 동일 로직 23회+ 반복, 버그 수정 시 누락 위험 |
| 책임 혼재 | UI 로직과 비즈니스 로직 미분리 |
| 디버깅 코드 잔재 | console.log **1,513개**, 번들 크기 증가 |

### 목표

- 대형 파일 **3,000줄 이하**로 분리
- 중복 코드 **Composable로 추출**
- 디버깅 코드 **개발 모드 전용**으로 정리
- 코드 재사용성 및 테스트 용이성 향상

---

## 2. 현재 상태 (Current State)

### 2.1 대형 파일 현황

| 파일 | 줄 수 | 문제점 |
|------|-------|--------|
| PassSchedulePage.vue | 4,838 | 모든 로직 혼재, console.log 128개 |
| EphemerisDesignationPage.vue | 4,340 | 차트 로직 800줄+, console.log 63개 |
| icdStore.ts | 2,971 | WebSocket 처리 + 상태 관리 혼재 |
| DashboardPage.vue | 2,728 | CMD/ACTUAL 중복 150줄 |
| passScheduleStore.ts | 2,452 | console.log 103개 |

### 2.2 중복 패턴 분석

#### A. 추적 상태 확인 (23회+ 중복)

```typescript
// 4개 파일에서 동일 패턴 반복
const isActuallyTracking =
  icdStore.ephemerisTrackingState === 'TRACKING' ||
  icdStore.ephemerisTrackingState === 'IN_PROGRESS' ||
  icdStore.passScheduleTrackingState === 'TRACKING'
```

| 파일 | 횟수 |
|------|------|
| DashboardPage.vue | 10회 |
| EphemerisDesignationPage.vue | 8회 |
| PassSchedulePage.vue | 3회 |
| ephemerisTrackStore.ts | 2회 |

#### B. 값 유효성 검증 (14회 중복)

```typescript
// DashboardPage.vue에서 14회 반복
const numValue = Number(value)
return isNaN(numValue) ? 0 : numValue
```

#### C. 차트 초기화/업데이트 (20회)

```typescript
// 3개 페이지에서 유사 패턴
const chartInstance = echarts.init(chartRef.value)
chartInstance.setOption({ ... })
```

| 파일 | 횟수 |
|------|------|
| DashboardPage.vue | 9회 |
| EphemerisDesignationPage.vue | 7회 |
| PassSchedulePage.vue | 4회 |

#### D. safeToFixed 함수 (분산 정의)

```typescript
// SelectScheduleContent.vue에 정의
const safeToFixed = (value: unknown, decimals: number = 6): string => { ... }

// 다른 파일에서도 비슷한 함수 존재
```

### 2.3 console.log 분포 (전수조사 결과)

| 파일 | 개수 | 비율 |
|------|------|------|
| PassSchedulePage.vue | 128 | 8% |
| passScheduleStore.ts | 103 | 7% |
| SelectScheduleContent.vue | 80 | 5% |
| TLEUploadContent.vue | 64 | 4% |
| EphemerisDesignationPage.vue | 63 | 4% |
| DashboardPage.vue | 60 | 4% |
| 기타 | 1,015 | 68% |
| **합계** | **1,513** | 100% |

### 2.4 기존 Composables 현황

```
frontend/src/composables/
├── useErrorHandler.ts      ✅ 존재
├── useLoading.ts           ✅ 존재
├── useNotification.ts      ✅ 존재
├── useTheme.ts             ✅ 존재
├── useValidation.ts        ✅ 존재
├── useAxisValue.ts         ❌ 없음 (필요)
├── useTrackingState.ts     ❌ 없음 (필요)
├── useChartInstance.ts     ❌ 없음 (필요)
└── useSafeNumber.ts        ❌ 없음 (필요)
```

### 2.5 코드 품질 이슈 (전수조사 결과)

#### A. 하드코딩 색상 - **475건** (CLAUDE.md 위반!)

> **CLAUDE.md 규칙**: "색상은 테마 변수 사용: `var(--theme-*)`, 하드코딩 금지"

| 파일 | 건수 | 예시 |
|------|------|------|
| PassSchedulePage.vue | 80+ | `color: '#FF6B6B'` |
| EphemerisDesignationPage.vue | 70+ | `backgroundColor: 'rgba(66, 133, 244, 0.1)'` |
| DashboardPage.vue | 65+ | `borderColor: '#4CAF50'` |
| SelectScheduleContent.vue | 55+ | `fill: '#2196F3'` |
| 기타 | 250+ | - |

**수정 방안**: CSS 변수로 전환

```typescript
// Before (위반)
style: { color: '#FF6B6B' }

// After (준수)
style: { color: 'var(--theme-error)' }
```

#### B. `as` 타입 단언 - **313건**

| 파일 | 건수 | 위험도 |
|------|------|--------|
| icdStore.ts | 45건 | High (런타임 오류 위험) |
| passScheduleStore.ts | 35건 | Medium |
| EphemerisDesignationPage.vue | 30건 | Medium |
| PassSchedulePage.vue | 28건 | Medium |
| 기타 | 142건 | Low |

**수정 방안**: Type Guard 패턴 사용

```typescript
// Before (위험)
const data = response as TrackingData

// After (안전)
function isTrackingData(obj: unknown): obj is TrackingData {
  return typeof obj === 'object' && obj !== null && 'status' in obj
}
if (isTrackingData(response)) {
  const data = response  // 타입 추론됨
}
```

### 2.6 성능 이슈 (심층 분석 결과)

#### A. `watch` 과다 사용 - 62개

| 파일 | 개수 | 문제 |
|------|------|------|
| PassSchedulePage.vue | 18개 | 여러 watch가 같은 데이터 감시 |
| EphemerisDesignationPage.vue | 14개 | deep watch 성능 저하 |
| DashboardPage.vue | 12개 | 연쇄 업데이트 |
| icdStore.ts | 8개 | computed로 대체 가능 |
| 기타 | 10개 | - |

**수정 방안**:
- `computed`로 대체 가능한 watch 변환
- 여러 watch를 `watchEffect` 하나로 통합
- deep watch에 debounce 적용

```typescript
// Before (과다한 watch)
watch(() => icdStore.azimuth, () => { ... })
watch(() => icdStore.elevation, () => { ... })

// After (통합)
watchEffect(() => {
  const { azimuth, elevation } = icdStore
  // 통합 처리
})
```

#### B. icdStore 성능 - 100+ 개별 ref, 30ms 업데이트

| 문제 | 현재 | 영향 |
|------|------|------|
| 개별 ref 100+ | `ref(0)` 각각 | 30ms마다 100+ 반응성 트리거 |
| 객체 분해 | 개별 속성 노출 | 불필요한 리렌더링 |

**수정 방안**: `shallowRef` + 청크 업데이트

```typescript
// Before (성능 저하)
const azimuth = ref(0)
const elevation = ref(0)
const train = ref(0)
// ... 100+ 개별 ref

// After (최적화)
interface ICDData {
  azimuth: number
  elevation: number
  train: number
  // ...
}
const icdData = shallowRef<ICDData>({ ... })

// 업데이트 시 전체 교체
icdData.value = { ...icdData.value, ...newData }
```

#### C. 번들 크기 이슈

| 항목 | 크기 | 상태 |
|------|------|------|
| ECharts 전체 import | ~500KB | 트리 쉐이킹 필요 |
| chart.js | ~200KB | 미사용, 제거 대상 |
| moment.js (있다면) | ~70KB | date-fns로 교체 권장 |

### 2.7 Service 레이어 성능 이슈 (심층 분석 추가)

> **사용자 지적**: Position View 차트의 실시간 추적 기능이 매우 느리고 부하가 많음

#### A. 대형 Service 파일

| 파일 | 줄 수 | console.log | 상태 |
|------|-------|-------------|------|
| ephemerisTrackService.ts | **1,193줄** | 50개 | 리팩토링 대상 |
| passScheduleService.ts | **1,118줄** | 42개 | 리팩토링 대상 |
| **합계** | **2,311줄** | **92개** | - |

#### B. Position View 차트 성능 병목 (Critical!)

`convertToChartData` 메서드 (passScheduleService.ts:1025-1114):

| 병목 | 현재 | 문제 |
|------|------|------|
| 배열 순회 | 4-5회 | 정렬+필터+reduce+샘플링 각각 순회 |
| Date 객체 | 매번 `new Date()` | O(2n) 불필요한 객체 생성 |
| 메모리 | 전체 배열 복사 | `[...trackingPoints]` 비효율 |
| 360° 정규화 | while 루프 2개 | 매 포인트마다 반복 |

**성능 개선 방안**:
```typescript
// Before: 4-5회 순회
const sorted = [...points].sort(...)  // 1회
const filtered = sorted.filter(...)   // 2회
const transformed = filtered.reduce(...)  // 3회
const sampled = transformed.filter(...)  // 4회

// After: 단일 순회로 통합
const chartData = optimizeChartData(points)  // 1회
```

#### C. Service 간 중복 코드 (92줄+)

| 중복 항목 | ephemerisTrackService | passScheduleService |
|----------|----------------------|---------------------|
| `handleApiError` | L362-385 (23줄) | L272-294 (23줄) **완전 동일** |
| Schedule 타입 | `ScheduleItem` | `PassScheduleMasterData` **거의 동일** |
| 에러 클래스 | `TLEParseError`, `ApiError` | `TLEApiError` |
| 추적 시작/중지 | `start/stopEphemerisTracking` | `start/stopScheduleTracking` |
| 시간 오프셋 | `sendTimeOffsetCommand` | `sendTimeOffsetCommand` **동일** |

**수정 방안**: 공통 유틸리티 추출
```typescript
// services/utils/apiErrorHandler.ts
export function handleApiError(error: unknown, defaultMessage: string): never { ... }

// services/utils/chartDataOptimizer.ts
export function optimizeChartData(points: TrackingPoint[]): ChartData[] { ... }

// types/schedule.ts
export interface BaseScheduleItem { ... }  // 공통 타입 정의
```

**수정 방안**:

```typescript
// Before (전체 import)
import * as echarts from 'echarts'

// After (트리 쉐이킹)
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent])
```

### 2.8 icdStore 심층 성능 분석 (Critical!) [신규]

> **사용자 지적**: ICD 부분이 엄청 코드도 많고 복잡한데 최적화 안되나?

#### A. 개별 ref 과다 선언 - **175개**

| 위치 | 항목 | ref 개수 |
|------|------|----------|
| L49-57 | 기본 상태 | 9개 |
| L109-284 | 안테나 데이터 | **81개** |
| L162-264 | 보드 상태 비트 | **72개** |
| L294-306 | 추적 상태 | 13개 |
| **합계** | - | **~175개** |

**문제**: 30ms마다 175회 개별 반응성 트리거

```typescript
// 현재 (L109-116): 30ms마다 각각 트리거
const modeStatusBits = ref('')
const azimuthAngle = ref('')
const elevationAngle = ref('')
// ... 175개 반복
```

#### B. 비트 파싱 함수 - **13개 함수, 72개 ref 업데이트**

| 함수 | 위치 | 업데이트 ref |
|------|------|-------------|
| parseProtocolStatusBits | L352-365 | 6개 |
| parseMainBoardStatusBits | L367-379 | 6개 |
| parseMainBoardMCOnOffBits | L381-393 | 6개 |
| parseAzimuthBoardServoStatusBits | L395-407 | 6개 |
| parseAzimuthBoardStatusBits | L410-422 | 6개 |
| parseElevationBoardServoStatusBits | L424-436 | 6개 |
| parseElevationBoardStatusBits | L438-450 | 6개 |
| parseTrainBoardServoStatusBits | L452-464 | 6개 |
| parseTrainBoardStatusBits | L466-478 | 6개 |
| parseFeedSBoardStatusBits | L487-500 | 6개 |
| parseFeedXBoardStatusBits | L510-521 | 5개 |
| parseFeedBoardETCStatusBits | L531-539 | 4개 |
| parseFeedKaBoardStatusBits | L552-563 | 3개 |

**문제**: 13개 함수가 매 30ms마다 호출되어 72개 ref 개별 업데이트

#### C. updataAntennaData 비효율성 (L1879-2216)

| 항목 | 현재 |
|------|------|
| 함수 크기 | **338줄** |
| 조건문 | ~70개 |
| safeToString 호출 | ~70회/30ms |

```typescript
// 비효율적 패턴 (70번 반복)
if (antennaData.modeStatusBits !== undefined && antennaData.modeStatusBits !== null) {
  modeStatusBits.value = safeToString(antennaData.modeStatusBits)
}
// ... 70번 반복
```

#### D. Computed 속성 과다 - **22개 대형 객체**

| computed | 위치 | 문제 |
|----------|------|------|
| protocolStatusInfo | L566-597 | 중첩 객체, filter 연산 |
| azimuthBoardServoStatusInfo | L665-720 | 80줄, 매번 객체 재생성 |
| elevationBoardStatusInfo | L837-917 | 80줄, 매번 배열 생성 |
| feedKaBoardStatusInfo | L1183-1260 | 77줄 |
| ... 기타 18개 | - | 유사 패턴 |

**문제**: 22개 computed가 각각 대형 중첩 객체 반환, 연관 ref 변경 시 전체 재계산

#### E. 동적 import 오버헤드 (L1466)

```typescript
// 매 에러 메시지마다 동적 import (성능 저하)
const { useHardwareErrorLogStore } = await import('@/stores/hardwareErrorLogStore')
```

#### F. 최적화 방안

> ⚠️ **2026-01-14 전문가 검증 결과**: shallowRef 그룹화는 **원시 타입에 무효**
>
> - icdStore의 175개 ref 중 **~140개가 원시 타입** (string/number/boolean)
> - `ref('')`와 `shallowRef('')`는 동일 동작 (원시 타입은 깊은 반응성 없음)
> - **22개 computed**가 개별 ref 직접 참조 → 그룹화 시 전체 재작성 필요
> - **실제 병목**: `updateAntennaData` 함수의 70개 조건문

**즉시 (P0) - 객체 타입만 shallowRef 적용** (2건):
```typescript
// 효과 있는 경우: 중첩 객체 타입
const errorStatusBarData = shallowRef<ErrorStatusBarData>({ ... })  // 객체
const errorPopupData = shallowRef<ErrorPopupData>({ ... })           // 객체

// 효과 없음 (변경 불필요): 원시 타입 ~140개
const azimuthAngle = ref('')     // string - shallowRef와 동일
const isConnected = ref(false)   // boolean - shallowRef와 동일
```

**즉시 (P0) - updateAntennaData 최적화** (실제 병목):
```typescript
// Before: 70개 조건문 개별 체크 (338줄)
if (antennaData.modeStatusBits !== undefined && antennaData.modeStatusBits !== null) {
  modeStatusBits.value = safeToString(antennaData.modeStatusBits)
}
// ... 70번 반복

// After: 필드 매핑 + 일괄 처리
const fieldMappings = [
  { key: 'modeStatusBits', ref: modeStatusBits },
  { key: 'azimuthAngle', ref: azimuthAngle },
  // ...
] as const

fieldMappings.forEach(({ key, ref }) => {
  const value = antennaData[key]
  if (value !== undefined && value !== null) {
    ref.value = safeToString(value)
  }
})
```

**단기 (P1) - 비트 파싱 배치 처리**:
```typescript
// 13개 함수를 1개로 통합
function parseAllBoardStatus(data: Record<string, string>) {
  // 배치 처리 (13회 호출 → 1회)
  parseProtocolStatusBits(data.protocolStatus)
  parseMainBoardStatusBits(data.mainBoardStatus)
  // ... 나머지도 동일 함수 내에서 순차 호출
}
```

**장기 (P2)**:
- icdStore 파일 분리 (2,971줄 → 5개 파일)
- Worker Thread로 비트 파싱 분리 (검토만)

#### G. 예상 효과 (수정됨)

| 항목 | 현재 | 최적화 후 | 비고 |
|------|------|----------|------|
| shallowRef 적용 | 0개 | **2개만** | 객체 타입만 효과 |
| updateAntennaData | 70개 조건문 | 필드 매핑 1개 | **실제 병목 해결** |
| 비트 파싱 함수 호출 | 13회/30ms | 1회/30ms | 배치 처리 |
| computed 재계산 | 전체 22개 | 변경 없음 | 그룹화 안 함 |
| 예상 CPU 감소 | - | **40-50%** | 현실적 추정 |

> **핵심 변경**: shallowRef 그룹화 전략 폐기 → updateAntennaData 최적화에 집중

### 2.9 전수조사 결과 (2026-01-13) [신규]

> **조사 범위**: FE 103개 파일 (33,000줄+)

#### A. 미사용 코드 (Dead Code) - **Critical!**

| 파일 | 위치 | 문제 | 심각도 | 상태 |
|------|------|------|--------|:----:|
| ExampleComponent.vue | components/ | 미사용 예제 컴포넌트 | Critical | 삭제 대상 |
| example-store.ts | stores/ | 미사용 예제 스토어 | Critical | 삭제 대상 |
| ~~modeStore 중복~~ | stores/ | ~~2개 파일 동일 역할~~ | ~~High~~ | **보류** |

> **2026-01-14 전문가 검증 결과**: modeStore 2개 파일 모두 **미사용 확인** → 통합 불필요, 보류

#### B. 메모리 누수 - **High**

| 파일 | 위치 | 문제 |
|------|------|------|
| windowUtils.ts | utils/ | **이벤트 리스너 미정리** (onBeforeUnmount 누락) |

```typescript
// 현재 (누수 위험)
window.addEventListener('resize', handler)
// onBeforeUnmount에서 removeEventListener 누락

// 수정 필요
onBeforeUnmount(() => {
  window.removeEventListener('resize', handler)
})
```

#### C. CSS 품질 - **Medium**

| 항목 | 건수 | 문제 |
|------|------|------|
| !important 과다 | **1,690개** | CSS 우선순위 관리 불가, 유지보수 어려움 |

```css
/* 현재 (과다한 !important) */
.button {
  color: red !important;
  background: blue !important;
}

/* 개선 방향 */
.button {
  color: red;  /* CSS 특이성으로 해결 */
  background: blue;
}
```

#### D. 전수조사 수치 업데이트

| 항목 | 기존 분석 | 전수조사 | 증감 |
|------|----------|---------|------|
| console.log | 988개 | **1,513개** | +525 |
| 타입 단언(as) | 80+건 | **313건** | +233 |
| 하드코딩 색상 | 300+건 | **475건** | +175 |

### 2.10 Offset Control 분산 관리 문제 (Critical!) [신규]

> **사용자 지적**: Offset은 안테나에서 하나인데, 모드별로 따로 관리되고 있어 통합 필요

#### A. 현재 상태 - **3곳에서 중복 관리**

| 위치 | 상태 변수 | 구조 |
|------|----------|------|
| ephemerisTrackStore.ts:86 | `offsetValues` | `{ azimuth, elevation, train, time, timeResult }` |
| passScheduleStore.ts:131 | `offsetValues` | `{ azimuth, elevation, train, time, timeResult }` (동일) |
| SunTrackPage.vue | `offsetCals`, `outputs` | 로컬 ref (다른 구조) |

```typescript
// ephemerisTrackStore.ts:86-92
const offsetValues = ref({
  azimuth: '0.00',
  elevation: '0.00',
  train: '0.00',
  time: '0.00',
  timeResult: '0.00',
})

// passScheduleStore.ts:131-137 (완전 동일!)
const offsetValues = ref({
  azimuth: '0.00',
  elevation: '0.00',
  train: '0.00',
  time: '0.00',
  timeResult: '0.00',
})

// SunTrackPage.vue (다른 구조)
const offsetCals = ref(['0', '0', '0'])
const outputs = ref(['0.00', '0.00', '0.00'])
```

#### B. 문제점

| 문제 | 영향 |
|------|------|
| **데이터 불일치** | 모드 전환 시 Offset 값이 공유되지 않음 (각 모드별 별도 상태) |
| **중복 코드** | 동일 로직 3곳에서 반복 |
| **유지보수 어려움** | 수정 시 3곳 모두 변경 필요 |
| **버그 위험** | 한 곳만 수정하면 불일치 발생 |
| **잘못된 초기화** | ephemerisTrackStore:975에서 clearAllData() 호출 시 offset 초기화 (사용자 의도와 무관) |

#### B-1. 핵심 요구사항

> **사용자 지적**: Offset은 안테나에서 하나이므로:
> - 모드 전환 시 Offset 값 **유지** (초기화 X)
> - **사용자만** Offset 값 변경 가능 (시스템 자동 초기화 금지)
> - 안테나 1개 → Offset 상태 1개

#### C. 해결 방안 - **offsetStore 통합**

```
변경 전:
├── ephemerisTrackStore.ts (offsetValues)
├── passScheduleStore.ts (offsetValues)
└── SunTrackPage.vue (offsetCals, outputs)

변경 후:
├── stores/common/offsetStore.ts (통합 상태)
├── components/common/OffsetControlPanel.vue (통합 UI)
└── 각 페이지에서 offsetStore 참조
```

#### D. UI 컴포넌트 중복 (900줄+)

3개 페이지에서 **거의 동일한 Offset Control UI** 반복:

| 페이지 | 위치 | 추정 줄 수 |
|--------|------|----------|
| PassSchedulePage.vue | L3-100 | ~300줄 |
| EphemerisDesignationPage.vue | L3-100 | ~300줄 |
| SunTrackPage.vue | L3-100 | ~300줄 |

```vue
<!-- 3개 페이지에서 동일 패턴 반복 -->
<div class="offset-group">
  <div class="position-offset-label">Azimuth<br>Offset</div>
  <q-input type="number" step="0.01" class="offset-input" />
  <q-btn @click="increment" />
  <q-btn @click="decrement" />
  <q-btn @click="reset" />
  <div class="output">{{ output }}</div>
</div>
<!-- Elevation, Tilt도 동일 패턴 -->
```

**해결**: `OffsetControlPanel.vue` 컴포넌트 추출 → **~900줄 중복 제거**

### 2.11 추가 발견 이슈 [신규]

#### A. FeedPage.vue 분리 계획 부재

| 파일 | 줄 수 | 상태 |
|------|-------|------|
| FeedPage.vue | 2,531 | 분리 계획 없음 |

#### B. 인라인 style 속성 (62건)

| 파일 | 건수 | 문제 |
|------|------|------|
| PassSchedulePage.vue | 20+ | `style="width: 110px !important"` |
| EphemerisDesignationPage.vue | 20+ | 동일 패턴 |
| 기타 | 20+ | - |

#### C. windowUtils.ts 분해 필요

| 파일 | 줄 수 | 문제 |
|------|-------|------|
| windowUtils.ts | 1,057 | 단일 파일에 여러 관심사 혼재 |

#### D. 차트 추가 최적화 필요

| 항목 | 상태 | 문제 |
|------|------|------|
| 리사이즈 디바운스 | ❌ 없음 | 창 크기 변경 시 과도한 리렌더링 |
| 차트 데이터 캐싱 | ❌ 없음 | 동일 데이터 반복 계산 |

---

## 3. 제안 (Proposal)

### 3.0 Phase 0: Offset Control 통합 (P0 - 최우선!) [신규]

> **핵심**: 안테나 Offset은 하나이므로 상태도 하나로 통합 관리

#### A. offsetStore.ts 신규 생성

```typescript
// stores/common/offsetStore.ts
import { defineStore } from 'pinia'
import { ref, readonly } from 'vue'
import { useICDStore } from '@/stores/icd/icdStore'

export const useOffsetStore = defineStore('offset', () => {
  const icdStore = useICDStore()

  // ===== 상태 (단일 소스) =====
  const offsetValues = ref({
    azimuth: '0.00',
    elevation: '0.00',
    train: '0.00',
    time: '0.00',
    timeResult: '0.00',
  })

  // ===== 액션 =====
  const updateOffset = (type: keyof typeof offsetValues.value, value: string) => {
    offsetValues.value[type] = value
  }

  const sendPositionOffset = async () => {
    const azOffset = parseFloat(offsetValues.value.azimuth) || 0
    const elOffset = parseFloat(offsetValues.value.elevation) || 0
    const tiOffset = parseFloat(offsetValues.value.train) || 0

    return await icdStore.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)
  }

  const sendTimeOffset = async () => {
    const timeValue = parseFloat(offsetValues.value.time) || 0
    // Time offset 명령 전송
    // ...
  }

  const resetAll = async () => {
    offsetValues.value = {
      azimuth: '0.00',
      elevation: '0.00',
      train: '0.00',
      time: '0.00',
      timeResult: '0.00',
    }
    await sendPositionOffset()
  }

  return {
    offsetValues: readonly(offsetValues),
    updateOffset,
    sendPositionOffset,
    sendTimeOffset,
    resetAll,
  }
})
```

#### B. OffsetControlPanel.vue 컴포넌트 추출

```vue
<!-- components/common/OffsetControlPanel.vue -->
<template>
  <div class="offset-control-panel">
    <div class="offset-group" v-for="axis in axes" :key="axis.key">
      <div class="offset-label">{{ axis.label }}<br>Offset</div>
      <q-input
        v-model="localValues[axis.key]"
        type="number"
        step="0.01"
        class="offset-input"
        @blur="handleBlur(axis.key)"
      />
      <q-btn flat dense @click="increment(axis.key)">+</q-btn>
      <q-btn flat dense @click="decrement(axis.key)">-</q-btn>
      <q-btn flat dense @click="reset(axis.key)">R</q-btn>
      <div class="offset-output">{{ offsetStore.offsetValues[axis.key] }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useOffsetStore } from '@/stores/common/offsetStore'

const offsetStore = useOffsetStore()

const axes = [
  { key: 'azimuth', label: 'Azimuth' },
  { key: 'elevation', label: 'Elevation' },
  { key: 'train', label: 'Tilt' },
] as const

const localValues = ref({ ... })

const increment = (key: string) => { ... }
const decrement = (key: string) => { ... }
const reset = (key: string) => { ... }
const handleBlur = (key: string) => { ... }
</script>
```

#### C. 기존 스토어에서 offsetValues 제거

```typescript
// ephemerisTrackStore.ts - 제거
// const offsetValues = ref({ ... })  ← 삭제

// passScheduleStore.ts - 제거
// const offsetValues = ref({ ... })  ← 삭제

// 대신 offsetStore 사용
import { useOffsetStore } from '@/stores/common/offsetStore'
const offsetStore = useOffsetStore()
```

#### D. 각 페이지에서 컴포넌트 사용

```vue
<!-- PassSchedulePage.vue -->
<template>
  <!-- 기존 300줄 Offset UI 대신 -->
  <OffsetControlPanel :show-time="true" />
</template>

<!-- EphemerisDesignationPage.vue -->
<template>
  <OffsetControlPanel :show-time="true" />
</template>

<!-- SunTrackPage.vue -->
<template>
  <OffsetControlPanel :show-time="false" />
</template>
```

#### E. 예상 효과

| 항목 | 현재 | 개선 후 |
|------|------|---------|
| Offset 상태 정의 | 3곳 | **1곳** |
| Offset UI 코드 | ~900줄 | **~150줄** |
| 모드 전환 시 | 값 불일치 | **값 유지** |
| 버그 수정 시 | 3곳 수정 | **1곳 수정** |

### 3.1 Phase 1: Composable 추출

#### A. useTrackingState

```typescript
// composables/useTrackingState.ts
import { computed } from 'vue'
import { useICDStore } from '@/stores/icd/icdStore'

export function useTrackingState() {
  const icdStore = useICDStore()

  const isEphemerisTracking = computed(() =>
    icdStore.ephemerisTrackingState === 'TRACKING' ||
    icdStore.ephemerisTrackingState === 'IN_PROGRESS'
  )

  const isPassScheduleTracking = computed(() =>
    icdStore.passScheduleTrackingState === 'TRACKING'
  )

  const isAnyTracking = computed(() =>
    isEphemerisTracking.value || isPassScheduleTracking.value
  )

  const trackingMode = computed<'ephemeris' | 'passSchedule' | null>(() => {
    if (isEphemerisTracking.value) return 'ephemeris'
    if (isPassScheduleTracking.value) return 'passSchedule'
    return null
  })

  return {
    isEphemerisTracking,
    isPassScheduleTracking,
    isAnyTracking,
    trackingMode
  }
}
```

**적용 효과**: 23회 중복 → 1곳 정의

#### B. useAxisValue

```typescript
// composables/useAxisValue.ts
import { computed, type Ref, type ComputedRef } from 'vue'
import { useTrackingState } from './useTrackingState'

export function useAxisValue(
  trackingValue: Ref<string | number> | ComputedRef<string | number>,
  fallbackValue: Ref<string | number> | ComputedRef<string | number>
) {
  const { isAnyTracking } = useTrackingState()

  return computed(() => {
    const tracking = Number(trackingValue.value)
    const fallback = Number(fallbackValue.value)

    // 추적 중이고 tracking 값이 유효하면 사용
    if (isAnyTracking.value && !isNaN(tracking) && tracking !== 0) {
      return tracking
    }

    // 아니면 fallback 값 사용
    return isNaN(fallback) ? 0 : fallback
  })
}
```

**적용 효과**: 300줄+ → 30줄

#### C. useSafeNumber

```typescript
// composables/useSafeNumber.ts
export function useSafeNumber() {
  const toSafeNumber = (value: unknown, defaultValue = 0): number => {
    const num = Number(value)
    return isNaN(num) ? defaultValue : num
  }

  const safeToFixed = (value: unknown, decimals = 6): string => {
    const num = Number(value)
    return isNaN(num) ? '0' : num.toFixed(decimals)
  }

  return { toSafeNumber, safeToFixed }
}
```

**적용 효과**: 분산된 유틸 함수 통합

#### D. useChartInstance

```typescript
// composables/useChartInstance.ts
import { ref, onMounted, onUnmounted, type Ref } from 'vue'
import * as echarts from 'echarts'

export function useChartInstance(containerRef: Ref<HTMLElement | null>) {
  const chartInstance = ref<echarts.ECharts | null>(null)

  const initChart = (options: echarts.EChartsOption) => {
    if (!containerRef.value) return
    chartInstance.value = echarts.init(containerRef.value)
    chartInstance.value.setOption(options)
  }

  const updateChart = (options: echarts.EChartsOption, notMerge = false) => {
    chartInstance.value?.setOption(options, { notMerge })
  }

  const resizeChart = () => {
    chartInstance.value?.resize()
  }

  onMounted(() => {
    window.addEventListener('resize', resizeChart)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', resizeChart)
    chartInstance.value?.dispose()
  })

  return { chartInstance, initChart, updateChart, resizeChart }
}
```

**적용 효과**: 차트 생명주기 관리 통합

#### E. useFormattedAngle [신규]

```typescript
// composables/useFormattedAngle.ts
export function useFormattedAngle() {
  const formatDegrees = (value: number | string, decimals = 4): string => {
    const num = Number(value)
    if (isNaN(num)) return '0.0000°'
    return `${num.toFixed(decimals)}°`
  }

  const formatDMS = (degrees: number): string => {
    const d = Math.floor(degrees)
    const m = Math.floor((degrees - d) * 60)
    const s = ((degrees - d) * 60 - m) * 60
    return `${d}° ${m}′ ${s.toFixed(2)}″`
  }

  const normalizeAngle = (angle: number): number => {
    let normalized = angle % 360
    if (normalized < 0) normalized += 360
    return normalized
  }

  return { formatDegrees, formatDMS, normalizeAngle }
}
```

**적용 효과**: 분산된 각도 포맷팅 로직 통합

#### F. useRealtimeData [신규]

```typescript
// composables/useRealtimeData.ts
import { ref, onMounted, onUnmounted } from 'vue'
import { useICDStore } from '@/stores/icd/icdStore'

export function useRealtimeData<T>(
  selector: (store: ReturnType<typeof useICDStore>) => T,
  options?: { debounceMs?: number }
) {
  const icdStore = useICDStore()
  const data = ref<T>(selector(icdStore))

  // 디바운스된 업데이트 로직
  // ...

  return { data }
}
```

**적용 효과**: icdStore 의존성 패턴 표준화

### 3.2 Phase 2: 대형 파일 분리

#### PassSchedulePage.vue (4,838줄 → ~2,500줄)

```
변경 전:
└── PassSchedulePage.vue (4,838줄)

변경 후:
├── PassSchedulePage.vue (~2,500줄)
├── components/passSchedule/
│   ├── ScheduleTable.vue (~500줄)
│   ├── ScheduleChart.vue (~600줄)
│   └── ScheduleControls.vue (~400줄)
└── composables/passSchedule/
    └── usePassScheduleChart.ts (~300줄)
```

#### EphemerisDesignationPage.vue (4,340줄 → ~2,500줄)

```
변경 전:
└── EphemerisDesignationPage.vue (4,340줄)

변경 후:
├── EphemerisDesignationPage.vue (~2,500줄)
├── components/ephemeris/
│   ├── PositionViewChart.vue (~500줄)
│   ├── TrackingInfoPanel.vue (~400줄)
│   └── ScheduleSelector.vue (~300줄)
└── composables/ephemeris/
    └── useEphemerisChart.ts (~300줄)
```

#### icdStore.ts (2,971줄 → ~1,500줄)

```
변경 전:
└── icdStore.ts (2,971줄)

변경 후:
├── icdStore.ts (~1,500줄) - 상태만
├── icdWebSocket.ts (~500줄) - WebSocket 처리
└── icdDataParser.ts (~400줄) - 데이터 파싱
```

#### FeedPage.vue (2,531줄 → ~1,500줄) [신규]

```
변경 전:
└── FeedPage.vue (2,531줄)

변경 후:
├── FeedPage.vue (~1,500줄) - 메인 페이지
├── components/feed/
│   ├── FeedControlPanel.vue (~400줄) - 제어 패널
│   ├── FeedStatusDisplay.vue (~300줄) - 상태 표시
│   └── FeedBandSelector.vue (~200줄) - 밴드 선택
└── composables/feed/
    └── useFeedControl.ts (~200줄) - 제어 로직
```

#### windowUtils.ts (1,057줄 → 4개 파일) [신규]

```
변경 전:
└── utils/windowUtils.ts (1,057줄)

변경 후:
├── utils/window/
│   ├── windowPosition.ts (~300줄) - 창 위치 관리
│   ├── windowResize.ts (~250줄) - 리사이즈 핸들링
│   ├── windowState.ts (~300줄) - 창 상태 관리
│   └── index.ts (~50줄) - 통합 export
```

### 3.3 Phase 3: console.log 정리

#### 개발 모드 전용 로거

```typescript
// utils/logger.ts
const isDev = import.meta.env.DEV

export const devLog = {
  log: (...args: unknown[]) => isDev && console.log(...args),
  warn: (...args: unknown[]) => isDev && console.warn(...args),
  error: (...args: unknown[]) => console.error(...args), // 에러는 항상 출력
  table: (...args: unknown[]) => isDev && console.table(...args),
}

// 사용
import { devLog } from '@/utils/logger'
devLog.log('디버깅 정보')  // 개발 모드에서만 출력
```

#### 정리 방법

| 유형 | 처리 |
|------|------|
| 디버깅용 | devLog로 교체 또는 삭제 |
| 에러 로그 | devLog.error 유지 |
| 상태 변경 로그 | 필요시 devLog로 교체 |

---

## 4. 대안 (Alternatives)

### 4.1 상태 관리 라이브러리 변경

| 옵션 | 장점 | 단점 |
|------|------|------|
| Pinia 유지 (채택) | 현재 사용 중, 변경 없음 | - |
| Vuex 5 | - | 마이그레이션 비용 |
| TanStack Query | 서버 상태 관리 | WebSocket 부적합 |

**결정**: Pinia 유지 (WebSocket 실시간 데이터에 적합)

### 4.2 컴포넌트 분리 전략

| 옵션 | 장점 | 단점 |
|------|------|------|
| 기능별 분리 (채택) | 명확한 책임 | 파일 수 증가 |
| 도메인별 분리 | 응집도 높음 | 과도한 분리 위험 |

**결정**: 기능별 분리 (차트, 테이블, 컨트롤)

---

## 5. 영향 분석 (Impact)

### 5.1 변경 범위

| 영역 | 변경 | 영향 |
|------|------|------|
| Composables | 4개 신규 | 재사용성 향상 |
| 페이지 | 2개 분리 | 유지보수성 향상 |
| 스토어 | 1개 분리 | 테스트 용이성 향상 |
| console.log | 988개 정리 | 번들 크기 감소 |

### 5.2 위험 요소

| 위험 | 대응 |
|------|------|
| 분리 시 동작 오류 | 단계별 분리, 즉시 테스트 |
| import 경로 변경 | IDE 리팩토링 기능 활용 |
| 차트 상태 손실 | Composable에서 상태 유지 |

---

## 6. 마이그레이션 (Migration)

### 6.1 단계별 적용

```
Phase 0: Offset Control 통합 (P0 - 최우선!) [신규]
├── offsetStore.ts 생성 (통합 상태 관리)
├── OffsetControlPanel.vue 생성 (통합 UI 컴포넌트)
├── ephemerisTrackStore.ts에서 offsetValues 제거
├── passScheduleStore.ts에서 offsetValues 제거
├── SunTrackPage.vue에서 로컬 offset 제거
└── 3개 페이지에서 OffsetControlPanel 사용

Phase 1: Composable 추출 (P1)
├── useTrackingState 생성 및 적용
├── useAxisValue 생성 및 적용
├── useSafeNumber 생성 및 적용
├── useChartInstance 생성 및 적용
├── useFormattedAngle 생성 및 적용 [신규]
└── useRealtimeData 생성 및 적용 [신규]

Phase 2: 대형 파일 분리 (P2)
├── PassSchedulePage 컴포넌트 분리
├── EphemerisDesignationPage 컴포넌트 분리
├── icdStore 분리
├── FeedPage 컴포넌트 분리 [신규]
└── windowUtils.ts 모듈 분해 [신규]

Phase 3: console.log 정리 (P1)
├── devLog 유틸 생성
├── 파일별 순차 정리
└── 빌드 크기 확인

Phase 4: 코드 품질 개선 (P2) [신규]
├── 하드코딩 색상 → CSS 변수 (475건)
├── as 타입 단언 → Type Guard (313건)
├── 인라인 style → CSS 클래스 (62건) [신규]
└── 테마 변수 추가 정의 (필요시)

Phase 5: 성능 최적화 (P2) [신규]
├── watch 과다 사용 정리 (62개)
├── icdStore shallowRef 적용
├── ECharts 트리 쉐이킹
├── 차트 리사이즈 디바운스 추가 [신규]
├── 차트 데이터 캐싱 [신규]
└── 미사용 의존성 제거 (chart.js 등)

Phase 6: Dead Code 정리 (P1) [전수조사 추가]
├── ExampleComponent.vue 삭제
├── example-store.ts 삭제
└── ~~modeStore 중복 통합~~ → **보류** (미사용 확인)

Phase 7: 메모리 누수 수정 (P1) [전수조사 추가]
├── windowUtils.ts 이벤트 리스너 정리
└── onBeforeUnmount 추가

Phase 8: CSS 품질 개선 (P2) [전수조사 추가]
├── !important 제거 (1,690개)
├── CSS 특이성 재설계
└── Quasar 변수 활용
```

### 6.2 우선순위

| 작업 | 우선순위 | 이유 |
|------|----------|------|
| console.log 정리 | **P1** | 번들 크기 즉시 감소 |
| useTrackingState | **P1** | 가장 많은 중복 해소 |
| useAxisValue | **P1** | Phase 5 (BE 개선) 전제 조건 |
| 대형 파일 분리 | P2 | 리스크 있음, P1 이후 |

---

## 7. 검증 (Verification)

### 7.1 체크리스트

**Phase 0 (Offset Control 통합)** [신규 - 최우선!]
- [ ] stores/common/offsetStore.ts 생성
- [ ] components/common/OffsetControlPanel.vue 생성
- [ ] ephemerisTrackStore.ts에서 offsetValues 제거
- [ ] passScheduleStore.ts에서 offsetValues 제거
- [ ] SunTrackPage.vue에서 offsetCals/outputs 제거
- [ ] PassSchedulePage.vue에서 OffsetControlPanel 적용
- [ ] EphemerisDesignationPage.vue에서 OffsetControlPanel 적용
- [ ] SunTrackPage.vue에서 OffsetControlPanel 적용
- [ ] 모드 전환 시 Offset 값 유지 확인
- [ ] 빌드 성공 확인
- [ ] 기능 테스트 (3개 모드 Offset 동작)

**Phase 1 (Composable 추출)**
- [ ] useTrackingState.ts 생성
- [ ] DashboardPage.vue 적용 (10회 → 1회)
- [ ] EphemerisDesignationPage.vue 적용 (8회 → 1회)
- [ ] PassSchedulePage.vue 적용 (3회 → 1회)
- [ ] ephemerisTrackStore.ts 적용 (2회 → 1회)
- [ ] useAxisValue.ts 생성
- [ ] useSafeNumber.ts 생성
- [ ] useChartInstance.ts 생성
- [ ] 빌드 성공 확인
- [ ] 기능 테스트

**Phase 2 (대형 파일 분리)**
- [ ] PassSchedulePage 컴포넌트 분리
- [ ] EphemerisDesignationPage 컴포넌트 분리
- [ ] icdStore 분리
- [ ] 빌드 성공 확인
- [ ] 기능 테스트

**Phase 3 (console.log 정리)**
- [ ] devLog 유틸 생성
- [ ] PassSchedulePage.vue (128개)
- [ ] passScheduleStore.ts (103개)
- [ ] SelectScheduleContent.vue (80개)
- [ ] TLEUploadContent.vue (64개)
- [ ] EphemerisDesignationPage.vue (63개)
- [ ] DashboardPage.vue (60개)
- [ ] 기타 파일 (490개)
- [ ] 빌드 크기 비교

**Phase 6 (Dead Code 정리)** [전수조사 추가]
- [ ] ExampleComponent.vue 삭제
- [ ] example-store.ts 삭제
- [X] ~~modeStore 중복 분석~~ → **보류** (미사용 확인됨)
- [X] ~~modeStore 통합 (2개 → 1개)~~ → **보류**
- [ ] 빌드 성공 확인

**Phase 7 (메모리 누수 수정)** [전수조사 추가]
- [ ] windowUtils.ts 분석
- [ ] 이벤트 리스너 미정리 위치 파악 (2건)
- [ ] onBeforeUnmount에 removeEventListener 추가
- [ ] 다른 파일 동일 패턴 확인
- [ ] 기능 테스트

**Phase 8 (CSS 품질 개선)** [전수조사 추가]
- [ ] !important 사용 파일 목록화
- [ ] 우선순위별 정리 (Critical → Low)
- [ ] Quasar 컴포넌트 스타일 활용
- [ ] CSS 특이성 재설계
- [ ] 시각적 회귀 테스트

### 7.2 성공 기준

| 기준 | 측정 방법 |
|------|----------|
| 대형 파일 3,000줄 이하 | wc -l 확인 |
| 중복 코드 제거 | grep 검색 결과 |
| console.log 0개 (prod) | 빌드 후 검색 |
| 빌드 성공 | npm run build |

---

## 8. 결정 사항 요약

| 항목 | 결정 | 비고 |
|------|------|------|
| **Offset Control 통합** | **P0 최우선** | 3곳 분산 → offsetStore 1곳 [신규] |
| Composable 추출 | 6개 신규 생성 | +useFormattedAngle, useRealtimeData |
| 대형 파일 분리 | 기능별 분리 | +FeedPage, windowUtils [신규] |
| console.log | devLog로 통합 | 1,513개 → 개발 모드만 |
| Dead Code | 즉시 삭제 | 2건 (ExampleComponent, example-store) |
| ~~modeStore 통합~~ | **보류** | 미사용 확인됨 |
| 메모리 누수 | 즉시 수정 | 2건 (High) |
| CSS !important | 점진적 제거 | 1,690개 → CSS 특이성 |
| 인라인 style | CSS 클래스 전환 | 62건 [신규] |
| 차트 최적화 | 디바운스/캐싱 | 리사이즈, 데이터 [신규] |
| 상태 관리 | Pinia 유지 | 변경 없음 |

---

## 9. 관련 RFC

### 9.1 RFC 연계 현황

| RFC | 관계 | 설명 |
|-----|------|------|
| [RFC-003](./RFC-003_State_Machine_Extraction.md) | 연관 | icdStore 분리 시 영향, 상태 관리 패턴 통일 |
| [RFC-004](./RFC-004_API_Standardization.md) | **중복** | Phase 4-5 (FE 정리, BE 모델)가 RFC-008과 일부 중복 |
| RFC-005 (예정) | 후속 | 리팩토링 후 FE 테스트 작성 |
| RFC-006 (예정) | 연관 | CI/CD에서 번들 크기 모니터링 |

### 9.2 RFC-004와의 중복 해소

| RFC-004 Phase | RFC-008 Phase | 처리 |
|---------------|---------------|------|
| Phase 4: console.log | Phase 3 | **RFC-008에서 통합 관리** |
| Phase 4: useAxisValue | Phase 1 | **RFC-008에서 통합 관리** |
| Phase 5: BE displayValue | - | RFC-004 유지 (BE 작업) |

> **실행 원칙**: FE 관련 작업은 RFC-008에서 통합 실행, BE 관련 작업은 RFC-004에서 실행

### 9.3 의존성 그래프

```
RFC-001 (DB)
    │
    ▼
RFC-004 Phase 0-3 (API 표준화)
    │
    ├──→ RFC-004 Phase 5 (BE displayValue)
    │         │
    │         ▼
    │    RFC-008 Phase 1 (useAxisValue - displayValue 활용)
    │
    ▼
RFC-008 Phase 1-5 (FE 전체 구조화)
    │
    ▼
RFC-003 (상태 머신 - icdStore 분리 후)
    │
    ▼
RFC-005 (테스트)
```

### 9.4 체크리스트 통합 안내

RFC-008의 모든 체크리스트는 [Execution_Checklist.md](./Execution_Checklist.md)에 통합되어 있습니다.

| 항목 | 체크리스트 위치 |
|------|----------------|
| Composable 추출 | Phase 3 > RFC-008 |
| 대형 파일 분리 | Phase 3 > RFC-008 |
| console.log 정리 | Phase 1-B > RFC-004 Phase 4 (RFC-008으로 통합) |
| 코드 품질 (색상, 타입) | Phase 3 > RFC-008 Phase 4 |
| 성능 최적화 | Phase 3 > RFC-008 Phase 5 |

---

**작성자**: Claude
**검토자**: -
**승인일**: -
