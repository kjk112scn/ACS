# CSS !important Phase 2 세부 실행 계획

**작성일**: 2026-01-18
**전문가 검토 완료**: FeedPage, ScheduleTable, ScheduleChart

---

## 1. 전체 요약

| 파일 | 총 개수 | 안전 제거 | 수정 필요 | 유지 필요 |
|-----|:------:|:--------:|:--------:|:--------:|
| FeedPage.vue | 217개 | 78개 | 72개 | 67개 |
| ScheduleTable.vue | 24개 | 6개 | 2개 | 16개 |
| ScheduleChart.vue | 17개 | 0개 | 17개 | 0개 |
| **합계** | **258개** | **84개** | **91개** | **83개** |

**예상 결과**: !important 258개 → 83개 (68% 감소)

---

## 2. 실행 순서

### Step 1: FeedPage.vue - mode-common.scss 중복 제거 (11개)

**대상**: 라인 1206-1227

```scss
// 제거할 !important (mode-common.scss와 중복)
q-page .feed-mode,
.feed-mode,
[class*="feed-mode"],
div.feed-mode {
  padding: 0 !important;           // ❌ 제거
  margin: 0 !important;            // ❌ 제거
  margin-bottom: 0 !important;     // ❌ 제거
  padding-bottom: 0 !important;    // ❌ 제거
  display: flex !important;        // ❌ 제거
  flex-direction: column !important; // ❌ 제거
  gap: 0 !important;               // ❌ 제거
  row-gap: 0 !important;           // ❌ 제거
  column-gap: 0 !important;        // ❌ 제거
  overflow: visible !important;    // ✅ 유지 (mode-common과 다른 값)
}
```

**위험도**: 🟢 LOW
**테스트**: 페이지 로드 후 레이아웃 확인

---

### Step 2: FeedPage.vue - scoped 내 불필요한 !important 제거 (40개)

**대상 선택자**:

| 라인 | 선택자 | 제거 이유 |
|-----|-------|---------|
| 1247 | `.feed-container` | scoped 내 유일한 선언 |
| 1255-1257 | `.feed-container .row` | scoped 내 유일한 선언 |
| 1263-1265 | `.feed-container .row>[class*="col-"]` | scoped 내 유일한 선언 |
| 1280-1302 | `.feed-row-single/double/triple .q-card` | scoped 내 유일한 선언 |
| 1394-1419 | `.q-card`, `.q-card-section` | scoped 내 유일한 선언 |
| 1435-1436 | `.row:last-child` | scoped 내 유일한 선언 |
| 1440-1444 | `.control-section` | scoped 충분한 특이성 |
| 1485, 1491-1492 | `.feed-path-section` | scoped 내 유일한 선언 |
| 1538, 1544, 1571 | 마진 0 선언들 | scoped 내 유일한 선언 |
| 1618-1628 | `.feed-path-wrapper` | scoped 내 유일한 선언 |
| 1687-1704 | `.fan-section-card` | scoped 내 유일한 선언 |
| 2306-2323 | `.path-output` | scoped 내 유일한 선언 |
| 2386-2388 | `.legend-icon` | scoped 내 유일한 선언 |
| 2406-2407 | `.status-message` | scoped 내 유일한 선언 |

**위험도**: 🟢 LOW
**테스트**: 각 섹션 레이아웃 확인

---

### Step 3: FeedPage.vue - 중복 선언 통합 (27개 → 9개)

**대상 1**: `.feed-container .row` 관련 (3개 규칙 → 1개)
- 라인 1255-1257 유지
- 라인 1271-1273 제거 (중복)

**대상 2**: `.feed-path-section` 관련 (3개 규칙 → 1개)
- 라인 1515-1521 유지
- 라인 1527-1531 제거 (중복)

**대상 3**: `.feed-path-wrapper` 관련 (3개 규칙 → 1개)
- 라인 1618-1628 유지
- 라인 1633-1636 제거 (중복)
- 라인 1642-1645 제거 (중복)

**대상 4**: `.path-label` 관련 (2개 규칙 → 1개)
- 라인 1733-1749 유지
- 라인 2001-2018 제거 (중복)

**위험도**: 🟡 MEDIUM
**테스트**: RF Switch 경로, Fan 섹션 확인

---

### Step 4: ScheduleTable.vue - 안전 제거 (6개)

**대상**: 셀 패딩/정렬

| 라인 | 선택자 | 변경 |
|-----|-------|-----|
| 322 | `.satellite-info-cell` | `padding: 8px 6px;` (!important 제거) |
| 350 | `.time-range-cell` | `padding: 8px 6px;` |
| 379-380 | `.azimuth-range-cell` | `padding`, `vertical-align` |
| 411-412 | `.elevation-info-cell` | `padding`, `vertical-align` |

**위험도**: 🟢 LOW
**테스트**: 테이블 셀 정렬 확인

---

### Step 5: ScheduleTable.vue - inline style 중복 해결 (2개)

**대상**: 라인 15 template + 라인 221-222 style

```vue
<!-- 변경 전 (라인 15) -->
<q-table style="height: 210px; max-height: 210px;">

<!-- 변경 후 -->
<q-table class="schedule-table">
```

CSS에서만 높이 관리:
```scss
.schedule-table {
  height: 210px !important;      // 유지 (Quasar 오버라이드)
  max-height: 210px !important;  // 유지
}
```

**위험도**: 🟢 LOW
**테스트**: 테이블 높이 210px 유지 확인

---

### Step 6: ScheduleChart.vue - inline → scoped 이동 (17개)

**대상**: 라인 4, 8, 14의 inline style

**변경 전**:
```vue
<q-card style="min-height: 360px !important; height: 100% !important; ...">
```

**변경 후**:
```vue
<q-card class="position-view-card">
```

```scss
/* scoped style 추가 */
.position-view-card {
  min-height: 360px !important;
  height: 100% !important;
  display: flex !important;
  flex-direction: column !important;
}

.position-view-card :deep(.q-card-section) {
  min-height: 360px !important;
  height: 100% !important;
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  padding-top: 16px !important;
  padding-bottom: 0 !important;
}

.chart-area {
  min-height: 340px !important;
  height: 100% !important;
  flex: 1 !important;
  padding: 0 !important;
  margin-bottom: 0 !important;
}
```

**위험도**: 🟡 MEDIUM
**테스트**: ECharts 차트 렌더링, 360px 높이 유지

---

## 3. 유지 필요 영역 (건드리지 말 것)

### FeedPage.vue (67개)

| 라인 범위 | 선택자 | 이유 |
|----------|-------|------|
| 2270-2272 | `.lna-disabled` | 상태 기반 스타일 우선순위 |
| 2281, 2285, 2290 | `:active`, `:hover` | 인터랙션 상태 오버라이드 |
| 2214-2230 | `.fan-button.q-btn` | Quasar q-btn 오버라이드 |

### ScheduleTable.vue (16개)

| 라인 범위 | 선택자 | 이유 |
|----------|-------|------|
| 228-229 | `:deep(.q-table__container)` | Quasar 내부 오버라이드 |
| 243, 247 | `:deep(.q-table__bottom/control)` | pagination 숨김 |
| 259-269 | `:deep(.q-table thead th)` | sticky 헤더 |
| 283 | `:deep(.q-table tbody tr:hover)` | hover 배경 |
| 297-317 | `.highlight-*-schedule` | 하이라이트 행 |

---

## 4. 테스트 체크리스트

### FeedPage.vue

| # | 테스트 항목 | 확인 방법 |
|---|------------|----------|
| 1 | 3열 그리드 정렬 | S/X/Ka 밴드 카드 동일 높이 |
| 2 | RF Switch 경로 표시 | SVG 아이콘 오버플로우 없음 |
| 3 | LNA 아이콘 크기 | 80px 유지 |
| 4 | Fan 섹션 높이 | 115px 유지 |
| 5 | 범례 그리드 정렬 | 하단 범례 정상 |
| 6 | hover/active 상태 | 아이콘 scale 효과 |

### ScheduleTable.vue

| # | 테스트 항목 | 확인 방법 |
|---|------------|----------|
| 1 | 테이블 높이 | 210px 고정 |
| 2 | 헤더 sticky | 스크롤 시 고정 |
| 3 | 하이라이트 행 | 초록(현재)/노란(다음) |
| 4 | 셀 정렬 | 중앙 정렬 유지 |

### ScheduleChart.vue

| # | 테스트 항목 | 확인 방법 |
|---|------------|----------|
| 1 | 차트 높이 | 360px 유지 |
| 2 | ECharts 렌더링 | 원형 차트 정상 |
| 3 | 카드 패딩 | 내용 잘림 없음 |

---

## 5. 실행 일정

| Step | 대상 | 제거 개수 | 난이도 | 예상 작업 |
|------|-----|:--------:|:-----:|:--------:|
| 1 | FeedPage mode-common 중복 | 11개 | 🟢 | 5분 |
| 2 | FeedPage scoped 불필요 | 40개 | 🟢 | 15분 |
| 3 | FeedPage 중복 통합 | 18개 | 🟡 | 10분 |
| 4 | ScheduleTable 안전 제거 | 6개 | 🟢 | 5분 |
| 5 | ScheduleTable inline 해결 | 2개 | 🟢 | 5분 |
| 6 | ScheduleChart inline→scoped | 17개 | 🟡 | 10분 |
| **합계** | | **94개** | | |

---

## 6. 롤백 계획

각 Step 완료 후 빌드 확인:
```bash
cd frontend && npm run build
```

문제 발생 시:
```bash
git checkout -- <파일명>
```

---

**작성자**: Claude Opus 4.5 + 전문가 에이전트
**다음 단계**: Step 1부터 순차 실행
