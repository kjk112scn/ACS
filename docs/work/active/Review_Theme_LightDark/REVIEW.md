# Theme Light/Dark Mode Review (#R002)

> 다크모드/화이트모드 구현 심층 검토 결과

**Review ID**: #R002
**분석 일자**: 2026-01-27
**분석 깊이**: Deep
**분석 대상**: Frontend 테마 시스템 전체

---

## 요약

| 항목 | 수치 |
|------|------|
| 분석 파일 | 33개 |
| 발견 이슈 | 267건 |
| Critical | 2건 |
| High | 12건 |
| Medium | 35건 이상 |

---

## 발견된 이슈

### Critical (즉시 수정 필요)

| Issue ID | 파일 | 라인 | 문제 | 영향 |
|----------|------|------|------|------|
| **#R002-C1** | [DashboardPage.vue](../../../frontend/src/pages/DashboardPage.vue#L265) | 265 | `active-color="white"` 하드코딩 | **라이트모드에서 모드 탭 텍스트 안 보임** |
| **#R002-C2** | [theme-variables.scss](../../../frontend/src/css/theme-variables.scss) | - | 라이트모드 변수 35개 누락 | 다수 컴포넌트에서 테마 불일치 |

### High (당일 수정 권장)

| Issue ID | 파일 | 라인 | 문제 | 영향 |
|----------|------|------|------|------|
| #R002-H1 | DashboardPage.vue | 2529-2532 | hover 배경 `rgba(255,255,255,0.04)` | 라이트모드에서 hover 효과 안 보임 |
| #R002-H2 | DashboardPage.vue | 1703-1725 | 텍스트 색상 하드코딩 (`white`, `black`) | 테마 변수 미사용 |
| #R002-H3 | SelectScheduleContent.vue | 1627, 1640, 1672 | `color: white !important` 다수 | 라이트모드 가독성 |
| #R002-H4 | TLEUploadContent.vue | 966-1268 | `color: white` 하드코딩 | 라이트모드 가독성 |
| #R002-H5 | SystemInfoContent.vue | 677-1048 | `color: white` 하드코딩 | 라이트모드 가독성 |
| #R002-H6 | popupRouter.vue | 190-220 | `color: white` 하드코딩 | 팝업 가독성 |
| #R002-H7 | AllStatusContent.vue | 2297-2334 | LED 색상 하드코딩 (`#00ff00`, `#ff0000`) | 테마 변수 미사용 |
| #R002-H8 | PassSchedulePage.vue | 652-660 | JS 내 색상 하드코딩 | 테마 불일치 |
| #R002-H9 | PassSchedulePage.vue | 3443-3745 | `rgba(255,255,255,0.8)` 하드코딩 | 라이트모드 가독성 |
| #R002-H10 | EphemerisDesignationPage.vue | 3349, 3720 | `color: white` 하드코딩 | 라이트모드 가독성 |
| #R002-H11 | ScheduleTable.vue | 155-318 | 상태 배경색 하드코딩 | 테마 변수 미사용 |
| #R002-H12 | MainLayout.vue | 483 | `color: white` 하드코딩 | 라이트모드 가독성 |

### Medium (이번 주 수정 권장)

| 카테고리 | 건수 | 영향 |
|----------|------|------|
| `rgba(255,255,255,*)` 기반 border/shadow | 89건 | 라이트모드에서 효과 미약 |
| 차트 옵션 내 색상 하드코딩 | 25건 | ECharts 테마 불일치 |
| Settings 컴포넌트 border/shadow | 15건 | 일관성 |

---

## 핵심 문제 상세 분석

### #R002-C1: 모드 탭 텍스트 안 보임

**문제 코드** (`DashboardPage.vue:265`):
```vue
<q-tabs v-model="currentMode" class="text-primary compact-tabs" active-color="white"
  indicator-color="transparent" align="left" narrow-indicator dense>
```

**원인**:
- Quasar `q-tabs`의 `active-color="white"` prop이 선택된 탭의 텍스트를 강제로 흰색 지정
- CSS에서 `var(--theme-text)` 사용해도 Quasar 인라인 스타일이 우선
- 라이트모드: 밝은 배경 + 흰색 텍스트 = **대비 1:1 (안 보임)**

**권장 수정**:
```vue
<!-- Before -->
<q-tabs ... active-color="white" ...>

<!-- After - 옵션 1: active-color 제거 -->
<q-tabs v-model="currentMode" class="text-primary compact-tabs"
  indicator-color="transparent" align="left" narrow-indicator dense>

<!-- After - 옵션 2: 동적 색상 -->
<q-tabs ... :active-color="$q.dark.isActive ? 'white' : 'primary'" ...>
```

### #R002-C2: 라이트모드 변수 누락

**`:root`에만 정의 (`.body--light` 누락)**:

| 카테고리 | 누락 변수 | 영향 |
|----------|----------|------|
| 상태 색상 | `--theme-positive`, `--theme-negative`, `--theme-warning`, `--theme-info` | 상태 표시 |
| LED 색상 | `--theme-led-normal`, `--theme-led-error`, `--theme-led-inactive` | LED 표시 |
| 버튼 색상 | `--theme-button-primary`, `--theme-button-secondary` 등 | 버튼 스타일 |
| 확장 색상 | `--theme-positive-bg`, `--theme-info-bg` 등 18개 | 배경/hover |

**권장 수정** (`theme-variables.scss`):
```scss
.body--light {
  // 기존 변수...

  // 누락된 hover/효과 변수 추가
  --theme-hover-overlay: rgba(0, 0, 0, 0.04);
  --theme-active-overlay: rgba(25, 118, 210, 0.15);

  // 테두리/구분선 (어두운 색 기반)
  --theme-border-focus: rgba(0, 0, 0, 0.2);
}
```

---

## 영향받는 파일 목록

### 테마 핵심 파일
- `frontend/src/css/theme-variables.scss`
- `frontend/src/css/mode-common.scss`
- `frontend/src/boot/dark-mode.ts`

### 하드코딩 수정 필요 파일 (우선순위순)

| 순위 | 파일 | 위반 건수 | 주요 문제 |
|:----:|------|:--------:|----------|
| 1 | DashboardPage.vue | 28+ | **모드 탭 + 텍스트** |
| 2 | SelectScheduleContent.vue | 45 | 테이블/탭 텍스트 |
| 3 | PassSchedulePage.vue | 55 | 차트 + 상태 색상 |
| 4 | SystemInfoContent.vue | 25 | LED + 헤더 |
| 5 | TLEUploadContent.vue | 33 | 테이블 헤더 |
| 6 | AllStatusContent.vue | 8 | LED 색상 |
| 7 | EphemerisDesignationPage.vue | 25 | 버튼/레이블 |
| 8 | ScheduleTable.vue | 6 | 상태 배경 |
| 9 | popupRouter.vue | 4 | 헤더/버튼 |
| 10 | MainLayout.vue | 1 | 시간 표시 |

---

## 권장 수정 계획

### Phase 1: Critical 즉시 수정
1. **DashboardPage.vue** - `active-color` 제거/동적 변경
2. **DashboardPage.vue** - 라이트모드 탭 스타일 추가

### Phase 2: High 수정
1. `theme-variables.scss` - 누락 변수 추가
2. 주요 컴포넌트 하드코딩 → 테마 변수 변환

### Phase 3: Medium 정리
1. 나머지 컴포넌트 일괄 정리
2. 차트 테마 적용

---

## 연계 스킬

| 이슈 | 권장 스킬 | 이유 |
|------|----------|------|
| #R002-C1 | `/bugfix` | 즉시 수정 필요 |
| #R002-C2 | `/refactor` | 변수 추가 및 구조 개선 |
| H1-H12 | `/cleanup` | 하드코딩 일괄 정리 |

---

## 결론

사용자가 보고한 "화이트모드에서 모드 탭 글자 안 보임" 문제의 **정확한 원인**은 `DashboardPage.vue:265`의 `active-color="white"` 하드코딩입니다.

추가로 전체 프론트엔드에서 **267건의 하드코딩 색상**이 발견되어 라이트모드 지원이 불완전합니다.

**권장 조치**: `/bugfix #R002-C1` 실행하여 Critical 이슈부터 즉시 수정

---

**Review 완료**: 2026-01-27
**Review ID**: #R002
