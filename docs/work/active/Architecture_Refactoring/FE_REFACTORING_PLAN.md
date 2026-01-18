# Frontend Refactoring Plan

> **생성일**: 2026-01-18
> **근거**: FE_REVIEW_2026-01-18.md 검토 결과
> **목표**: FE 품질 65점 → 85점

---

## 작업 우선순위

```
[P0] 테마 변수 확장 (선행 작업)
  ↓
[P1] 하드코딩 색상 교체 (304개)
  ↓
[P2] Composables 활용 (useErrorHandler, useLoading)
  ↓
[P3] !important 정리 (1,481개) - 선택적
  ↓
[P4] 공용 컴포넌트 생성 - 선택적
```

---

## P0: 테마 변수 확장 (선행 필수)

### 목표
하드코딩 색상을 교체하기 전에 필요한 테마 변수 추가

### P0-A: 다크모드 변수 추가 (12개)

**파일**: `frontend/src/css/theme-variables.scss`

```scss
// ========== 다크모드 신규 추가 (.body--dark) ==========

// 차트용 (5개)
--theme-chart-line: #555;
--theme-chart-grid: #333;
--theme-chart-label: #999;
--theme-chart-tooltip-bg: rgba(0, 0, 0, 0.8);
--theme-chart-axis: #666;

// 테이블용 (4개)
--theme-table-row-hover: rgba(255, 255, 255, 0.05);
--theme-table-row-selected: rgba(33, 150, 243, 0.2);
--theme-table-row-even: rgba(255, 255, 255, 0.02);
--theme-table-header-bg: #1a1a2e;

// 스크롤바용 (3개)
--theme-scrollbar-track: #1a1a2e;
--theme-scrollbar-thumb: #555;
--theme-scrollbar-thumb-hover: #666;
```

### P0-B: 라이트모드 변수 추가 (12개)

**전문가 검토 결과**: 현재 라이트모드 변수가 13개만 정의됨 (다크모드 37개 대비 부족)

```scss
// ========== 라이트모드 신규 추가 (.body--light) ==========

// 차트용 (5개)
--theme-chart-line: #aaa;
--theme-chart-grid: #ddd;
--theme-chart-label: #666;
--theme-chart-tooltip-bg: rgba(255, 255, 255, 0.95);
--theme-chart-axis: #888;

// 테이블용 (4개)
--theme-table-row-hover: rgba(0, 0, 0, 0.04);
--theme-table-row-selected: rgba(33, 150, 243, 0.15);
--theme-table-row-even: rgba(0, 0, 0, 0.02);
--theme-table-header-bg: #f5f5f5;

// 스크롤바용 (3개)
--theme-scrollbar-track: #f0f0f0;
--theme-scrollbar-thumb: #bbb;
--theme-scrollbar-thumb-hover: #999;
```

### P0-C: useChartTheme Composable 생성

**전문가 권장**: ECharts는 CSS 변수 직접 사용 불가 → Composable로 캐싱 필요

**파일**: `frontend/src/composables/useChartTheme.ts`

```typescript
import { ref, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'

export function useChartTheme() {
  const $q = useQuasar()

  const colors = ref({
    line: '',
    grid: '',
    label: '',
    axis: '',
    tooltipBg: '',
    positive: '',
    negative: '',
    warning: '',
    info: '',
    azimuth: '',
    elevation: ''
  })

  function refreshColors() {
    const style = getComputedStyle(document.documentElement)
    colors.value = {
      line: style.getPropertyValue('--theme-chart-line').trim() || '#555',
      grid: style.getPropertyValue('--theme-chart-grid').trim() || '#333',
      label: style.getPropertyValue('--theme-chart-label').trim() || '#999',
      axis: style.getPropertyValue('--theme-chart-axis').trim() || '#666',
      tooltipBg: style.getPropertyValue('--theme-chart-tooltip-bg').trim() || 'rgba(0,0,0,0.8)',
      positive: style.getPropertyValue('--theme-positive').trim() || '#4caf50',
      negative: style.getPropertyValue('--theme-negative').trim() || '#f44336',
      warning: style.getPropertyValue('--theme-warning').trim() || '#ff9800',
      info: style.getPropertyValue('--theme-info').trim() || '#2196f3',
      azimuth: style.getPropertyValue('--theme-azimuth-color').trim() || '#ff5722',
      elevation: style.getPropertyValue('--theme-elevation-color').trim() || '#4fc3f7'
    }
  }

  watch(() => $q.dark.isActive, () => {
    setTimeout(refreshColors, 0) // DOM 업데이트 후 실행
  })

  onMounted(refreshColors)

  return { colors, refreshColors }
}
```

### 예상 작업량
- 파일: 2개 (theme-variables.scss, useChartTheme.ts)
- 변수: 24개 추가 (다크 12 + 라이트 12)
- Composable: 1개 신규

---

## P1: 하드코딩 색상 교체 (304개)

### 작업 순서 (파일별)

| 순서 | 파일 | 개수 | 난이도 | 비고 |
|:----:|------|:----:|:------:|------|
| 1 | PassSchedulePage.vue | 76 | 중 | 테이블/차트 |
| 2 | AllStatusContent.vue | 38 | 하 | LED/상태 |
| 3 | SystemInfoContent.vue | 35 | 하 | 상태/그래프 |
| 4 | FeedPage.vue | 32 | 중 | SVG/상태 |
| 5 | EphemerisDesignationPage.vue | 28 | 중 | 차트/상태 |
| 6 | DashboardPage.vue | 27 | 중 | ECharts |
| 7 | SelectScheduleContent.vue | 21 | 하 | 테이블 |
| 8 | TLEUploadContent.vue | 14 | 하 | 테마 |
| 9 | 기타 컴포넌트 | 33 | 하 | 분산 |

### 색상 매핑 규칙

```typescript
// 변환 규칙
'#4caf50' → 'var(--theme-positive)'      // 성공/활성
'#f44336' → 'var(--theme-negative)'      // 에러
'#ff9800' → 'var(--theme-warning)'       // 경고
'#2196f3' → 'var(--theme-info)'          // 정보
'#ff5722' → 'var(--theme-azimuth-color)' // Azimuth
'#4fc3f7' → 'var(--theme-elevation-color)' // Elevation
'#555'    → 'var(--theme-chart-line)'    // 차트 라인
'#999'    → 'var(--theme-text-muted)'    // 라벨
'#666'    → 'var(--theme-chart-axis)'    // 차트 축
```

### ECharts 특수 처리

```typescript
// ECharts는 CSS 변수 직접 사용 불가
// computed로 테마 색상 가져와서 적용

const chartColors = computed(() => ({
  line: getComputedStyle(document.documentElement)
    .getPropertyValue('--theme-chart-line').trim(),
  grid: getComputedStyle(document.documentElement)
    .getPropertyValue('--theme-chart-grid').trim(),
  // ...
}))
```

---

## P2: Composables 활용

### P2-1: useErrorHandler 적용

**대상**: API 호출하는 모든 컴포넌트

```typescript
// Before
try {
  const response = await api.getData()
} catch (error) {
  console.error('에러:', error)
  $q.notify({ type: 'negative', message: '실패' })
}

// After
import { useErrorHandler } from '@/composables/useErrorHandler'
const { handleError } = useErrorHandler()

try {
  const response = await api.getData()
} catch (error) {
  handleError(error, '데이터 로드 실패')
}
```

### P2-2: useLoading 적용

**대상**: 로딩 상태가 있는 컴포넌트

```typescript
// Before
const loading = ref(false)
loading.value = true
// ... 작업
loading.value = false

// After
import { useLoading } from '@/composables/useLoading'
const { isLoading, withLoading } = useLoading()

await withLoading(async () => {
  // ... 작업
})
```

---

## P3: !important 정리 (선택적)

### 우선순위

| 순서 | 파일 | 개수 | 전략 |
|:----:|------|:----:|------|
| 1 | EphemerisDesignationPage.vue | 347 | CSS 우선순위 재설계 |
| 2 | PassSchedulePage.vue | 321 | Quasar deep 선택자 활용 |
| 3 | mode-common.scss | 다수 | 공용 스타일 분리 |

### 접근 방식

```scss
// Before: !important 남용
.my-table .q-table__container {
  background: #1a1a2e !important;
}

// After: 구체적 선택자
.mode-page .my-table :deep(.q-table__container) {
  background: var(--theme-surface);
}
```

---

## P4: 공용 컴포넌트 생성 (선택적)

### 후보 컴포넌트

| 컴포넌트 | 용도 | 재사용처 |
|---------|------|---------|
| AxisControlPanel.vue | 축 제어 (AZ/EL/Tilt) | 모드 페이지들 |
| StatusIndicator.vue | 상태 표시 (LED/색상) | 전체 |
| ChartContainer.vue | ECharts 래퍼 | 차트 있는 페이지 |
| SettingsActionBar.vue | 저장/취소 버튼 | 설정 페이지들 |

---

## 실행 계획

### 즉시 실행 (오늘)

- [x] FE_REVIEW_2026-01-18.md 생성
- [x] FE_REFACTORING_PLAN.md 생성 (현재 문서)
- [x] P0-A: 다크모드 테마 변수 12개 추가
- [x] P0-B: 라이트모드 테마 변수 12개 추가
- [x] P0-C: useChartTheme.ts composable 생성
- [x] FE 빌드 검증 완료

### 단기 (회사 복귀 후)

- [ ] P1: 하드코딩 색상 교체 (304개)
- [ ] P2: Composables 활용

### 중기 (선택적)

- [ ] P3: !important 정리
- [ ] P4: 공용 컴포넌트 생성

---

## 체크리스트

### P0 완료 조건 (2026-01-18 완료)
- [x] P0-A: 다크모드 차트/테이블/스크롤바 변수 12개 추가
- [x] P0-B: 라이트모드 차트/테이블/스크롤바 변수 12개 추가
- [x] P0-C: useChartTheme.ts composable 생성
- [x] FE 빌드 성공
- [ ] 다크/라이트 모드 전환 테스트 (런타임 검증 필요)

### P1 완료 조건
- [ ] 304개 하드코딩 색상 모두 변수로 교체
- [ ] FE 빌드 성공
- [ ] 다크/라이트 모드 전환 테스트

### P2 완료 조건
- [ ] useErrorHandler 사용률 50% 이상
- [ ] useLoading 사용률 50% 이상

---

**Last Updated**: 2026-01-18 (P0 완료)
