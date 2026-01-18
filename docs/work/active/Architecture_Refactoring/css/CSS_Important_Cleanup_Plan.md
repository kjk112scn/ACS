# CSS !important 정리 계획

**작성일**: 2026-01-18
**총 개수**: 1,066개 (11개 파일)

---

## 요약

| 항목 | 수치 | 비율 |
|-----|------|------|
| 안전 제거 가능 | ~330개 | 31% |
| 주의 필요 | ~300개 | 28% |
| Quasar 오버라이드 (HIGH RISK) | ~200개 | 19% |
| 기타 | ~236개 | 22% |

---

## 1. 패턴 분류

| 카테고리 | 개수 | 비율 | 설명 |
|---------|------|------|------|
| 레이아웃 (height/width/min-max) | ~280개 | 26% | 고정 높이, Quasar 컴포넌트 크기 오버라이드 |
| Flexbox (display/flex/justify) | ~180개 | 17% | flex 레이아웃, 정렬, 방향 |
| 마진/패딩 | ~320개 | 30% | margin-bottom: 0, padding 리셋 |
| 색상/배경 | ~90개 | 8% | 테마 변수 오버라이드, 상태별 색상 |
| 기타 (font-size, line-height 등) | ~196개 | 19% | 폰트 크기, 라인 높이, overflow |

---

## 2. Phase 1: 안전한 정리 (~330개)

### 2.1 중복 margin-bottom: 0 통합 (~150개)

**대상 파일**: EphemerisDesignationPage.vue, PassSchedulePage.vue

**현재 상태**:
```css
/* 동일 요소에 여러 번 적용됨 */
.ephemeris-mode .row.q-col-gutter-md.main-content-row { margin-bottom: 0 !important; }
.ephemeris-mode .main-content-row>[class*="col-"]:nth-child(1) { margin-bottom: 0 !important; }
.ephemeris-mode .main-content-row>[class*="col-"]:nth-child(2) { margin-bottom: 0 !important; }
```

**변경 후**:
```css
/* 부모 선택자에서 한 번만 적용 */
.ephemeris-mode .main-content-row,
.ephemeris-mode .main-content-row > * {
  margin-bottom: 0;
}
```

**예상 제거**: ~80개

---

### 2.2 mode-common.scss와 중복 제거 (~80개)

**대상 파일**: StepPage.vue, SlewPage.vue, PedestalPositionPage.vue

**현재 상태**:
```css
/* 페이지별 CSS - mode-common.scss와 중복 */
.checkbox-label-group {
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
  justify-content: center !important;
  width: 100% !important;
}
```

**변경**: 페이지별 중복 선언 삭제 → mode-common.scss 사용

**예상 제거**: ~60개

---

### 2.3 scoped 내 불필요한 !important (~100개)

**대상 파일**: StandbyPage.vue, SunTrackPage.vue

**현재 상태**:
```css
/* scoped 내에서 Quasar 충돌 없는 자체 클래스 */
.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
  margin-bottom: 0.5rem !important;  /* 불필요 */
}
```

**변경**: !important 제거

**예상 제거**: ~20개

---

## 3. Phase 2: 중간 난이도 (~200개)

### 3.1 FeedPage.vue flex 레이아웃 정리 (~100개)

- 복잡한 그리드 레이아웃 (3열 구조)
- flex 관련 !important 다수
- 테스트 필수

### 3.2 컴포넌트 분리 파일 정리 (~30개)

- ScheduleTable.vue (29개)
- ScheduleChart.vue (3개)

### 3.3 Quasar dense prop 활용 (~70개)

**현재 상태**:
```css
.q-field__control { min-height: auto !important; }
.q-btn { min-height: auto !important; }
```

**변경**: Quasar 컴포넌트에 `dense` prop 추가

---

## 4. Phase 3: 주의 필요 (~400개)

### 4.1 Quasar 컴포넌트 오버라이드 (~200개) - HIGH RISK

**영향 범위**: q-card, q-field, q-btn, q-table 등

**대안**:
1. Quasar 변수 오버라이드 (`quasar.variables.scss`)
2. `:deep()` 선택자 + specificity 높이기
3. 컴포넌트 props 활용

**위험**: 제거 시 Quasar 기본 스타일이 적용되어 레이아웃 깨질 수 있음

### 4.2 인라인 스타일 !important (19개) - MEDIUM RISK

**대상**: EphemerisDesignationPage.vue, PassSchedulePage.vue

```html
<q-card-section
  style="min-height: 360px !important; height: 100% !important;">
```

**대안**: 클래스로 추출하여 scoped CSS로 이동

### 4.3 테마 색상 오버라이드 (~70개) - MEDIUM RISK

**대상**: 상태별 색상 (status-active, status-error 등)

**대안**: CSS 변수 우선순위 조정

---

## 5. 파일별 현황

| 파일 | 개수 | 난이도 | Phase |
|-----|------|--------|-------|
| EphemerisDesignationPage.vue | 347개 | HIGH | 1, 3 |
| PassSchedulePage.vue | 321개 | HIGH | 1, 3 |
| FeedPage.vue | 279개 | MEDIUM | 2 |
| ScheduleTable.vue | 29개 | MEDIUM | 2 |
| SlewPage.vue | 20개 | LOW | 1 |
| PedestalPositionPage.vue | 20개 | LOW | 1 |
| SunTrackPage.vue | 18개 | LOW | 1 |
| StepPage.vue | 17개 | LOW | 1 |
| OffsetControls.vue | 11개 | LOW | 1 |
| ScheduleChart.vue | 3개 | LOW | 2 |
| StandbyPage.vue | 1개 | LOW | 1 |

---

## 6. 작업 순서 권장

### Phase 1 (안전)
1. StandbyPage.vue (1개) - 연습용
2. SunTrackPage.vue (18개)
3. StepPage.vue, SlewPage.vue, PedestalPositionPage.vue (57개)
4. OffsetControls.vue (11개)

### Phase 2 (중간)
1. ScheduleChart.vue (3개)
2. ScheduleTable.vue (29개)
3. FeedPage.vue (279개) - 테스트 집중

### Phase 3 (주의)
1. EphemerisDesignationPage.vue (347개)
2. PassSchedulePage.vue (321개)

---

## 7. 검증 체크리스트

### 각 파일 수정 후
- [ ] 브라우저에서 해당 페이지 로드
- [ ] 반응형 (1024px 이하) 확인
- [ ] Quasar 컴포넌트 기능 동작 확인
- [ ] 다크/라이트 테마 전환 확인
- [ ] 차트 렌더링 정상 확인 (해당 시)

### 전체 완료 후
- [ ] 전체 페이지 순회 테스트
- [ ] DevTools Lighthouse 성능 비교
- [ ] CSS 번들 사이즈 비교

---

## 8. 리팩토링 전략 제안

### 전략 1: CSS 유틸리티 클래스 도입

```css
/* flex-utilities.scss */
.flex-col { display: flex; flex-direction: column; }
.flex-center { justify-content: center; align-items: center; }
.h-full { height: 100%; }
.mb-0 { margin-bottom: 0; }
```

### 전략 2: CSS Custom Properties 확장

```css
/* quasar.variables.scss */
:root {
  --q-card-section-padding: 0.8rem;
  --q-field-min-height: auto;
  --q-btn-dense-min-height: 32px;
}
```

### 전략 3: CSS 계층 구조 개선

```
css/
  app.scss                    # 전역 리셋
  quasar.variables.scss       # Quasar 변수 오버라이드
  mode-common.scss            # 모드 페이지 공통
  components/
    card-layout.scss          # q-card 유틸리티
    flex-utilities.scss       # flex 유틸리티
    table-styles.scss         # 테이블 스타일
```

---

## 9. Phase 1 상세 분석 (전문가 검토 완료)

**검토일**: 2026-01-18
**총 제거 가능**: 87개 (100%)

### 9.1 파일별 상세

| 파일 | 총 !important | 제거 가능 | 방법 |
|-----|--------------|----------|------|
| StandbyPage.vue | 1 | 1 | 블록 제거 |
| SunTrackPage.vue | 18 | 18 | 블록 제거 + !important 제거 |
| StepPage.vue | 17 | 17 | 블록 제거 |
| SlewPage.vue | 20 | 20 | 블록 제거 + !important 제거 |
| PedestalPositionPage.vue | 20 | 20 | 블록 제거 + !important 제거 |
| OffsetControls.vue | 11 | 11 | inline → class 변환 |

### 9.2 StandbyPage.vue (1개)

**제거 대상** (139-145줄):
```css
/* 제거 - mode-common.scss에서 이미 정의됨 */
.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
  margin-bottom: 0.5rem !important;
}
```

### 9.3 SunTrackPage.vue (18개)

**제거 대상 1** (174-197줄): `div.sun-track-mode` 블록 전체 제거
**제거 대상 2** (199-216줄): router-view, `> *`, `:last-child` 블록 전체 제거
**수정 대상** (218-223줄): `.offset-control-row`에서 !important 제거

### 9.4 StepPage.vue (17개)

**제거 대상**:
- `.section-title` 블록 (312-317줄)
- `.checkbox-label-group` 블록 (337-347줄)
- `.axis-header` 블록 (349-358줄)
- `.text-subtitle2` 관련 블록 (366-378줄)
- `.q-field` 관련 블록 (380-388줄)

### 9.5 SlewPage.vue (20개)

**제거 대상**:
- `.section-title` 블록 (441-446줄)
- `.checkbox-label-group` 블록 (466-476줄)
- `.text-subtitle2` 관련 블록 (502-514줄)

**수정 대상** (!important 제거, 값 유지):
- `.axis-header` 블록 (478-488줄) - margin-bottom: 2rem 유지
- `.axis-checkbox` 블록 (490-494줄)
- `.q-field` 관련 블록 (516-536줄)

### 9.6 PedestalPositionPage.vue (20개)

**제거 대상**:
- `.section-title` 블록 (332-338줄)

**수정 대상** (!important 제거, 커스텀 값 유지):
- `.q-card-section` 블록 (365-369줄)
- `.checkbox-label-group` 블록 (371-383줄) - margin, margin-top 유지
- `.axis-header` 블록 (385-397줄) - margin-bottom, margin-top 유지
- `.axis-checkbox` 블록 (399-404줄)
- `.q-field` 관련 블록 (416-425줄)
- `.text-subtitle2` 블록 (427-436줄)

### 9.7 OffsetControls.vue (11개)

**수정 대상**: inline style → CSS class 변환

```html
<!-- 변경 전 -->
style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;"

<!-- 변경 후 -->
class="offset-input"
```

**추가할 CSS 클래스**:
```css
.offset-input {
  width: 110px;
  min-width: 110px;
  max-width: 110px;
}

.cal-time-input {
  min-width: 190px;
  max-width: 220px;
}
```

---

## 10. Phase 1 테스트 체크리스트

### 레이아웃 테스트

| 파일 | 테스트 항목 | 확인 방법 | 결과 |
|-----|------------|----------|------|
| StandbyPage | 섹션 제목 마진 | Standby 모드 진입 후 레이아웃 확인 | ⬜ |
| SunTrackPage | 전체 레이아웃 높이/마진 | 카드 높이, 마진 확인 | ⬜ |
| SunTrackPage | Offset Control Row 간격 | Offset과 Speed 섹션 간격 확인 | ⬜ |
| StepPage | 축 패널 체크박스 정렬 | Az/El/Tilt 패널 헤더 정렬 확인 | ⬜ |
| StepPage | 입력 필드 간격 | Angle/Speed 입력 필드 간격 확인 | ⬜ |
| SlewPage | 축 헤더 마진 (2rem) | 체크박스와 Speed 간격 확인 | ⬜ |
| SlewPage | Speed 입력창 높이 (2rem) | 입력창 높이 유지 확인 | ⬜ |
| PedestalPositionPage | 체크박스 위치 (-0.5rem) | 체크박스 위로 올라간 위치 확인 | ⬜ |
| PedestalPositionPage | 카드 섹션 패딩 | 상단 최소/하단 최대 패딩 확인 | ⬜ |
| OffsetControls | 입력 필드 너비 (110px) | 모든 Offset 입력 너비 동일 확인 | ⬜ |
| OffsetControls | Cal Time 필드 너비 | 190-220px 범위 유지 확인 | ⬜ |

### 반응형 테스트

| 뷰포트 | 확인 항목 | 결과 |
|--------|----------|------|
| Desktop (1920px) | 모든 페이지 레이아웃 | ⬜ |
| Tablet (1024px) | 카드 레이아웃 변경 | ⬜ |
| Mobile (768px) | 세로 배치 전환 | ⬜ |

### 기능 테스트

| 페이지 | 확인 항목 | 결과 |
|--------|----------|------|
| 모든 모드 페이지 | Go/Stop/Stow 버튼 동작 | ⬜ |
| 모든 모드 페이지 | Offset 입력/버튼 동작 | ⬜ |
| SlewPage | Loop 기능 동작 | ⬜ |

---

## 11. Phase 2 상세 분석 (전문가 검토 완료)

**검토일**: 2026-01-18
**대상 파일**: FeedPage.vue (279개), ScheduleTable.vue (29개), ScheduleChart.vue (3개)
**총 311개**

### 11.1 분석 요약

| 분류 | 개수 | 비율 | 설명 |
|-----|------|------|------|
| 제거 가능 | 120개 | 39% | scoped 내 불필요, 중복 선언 |
| 수정 필요 | 143개 | 46% | !important 제거, 값 유지 (특이성 조정) |
| 유지 필요 | 48개 | 15% | Quasar 오버라이드 필수 |

### 11.2 FeedPage.vue (279개)

#### 제거 가능 (114개, 41%)

**A. 일반 마진/패딩 (34개)**
- 라인 1206-1235: `.feed-mode > *` margin/padding 0
- 라인 1538-1544, 1654: 반복되는 `margin-bottom: 0`

**B. 중복 flex 선언 (48개)**
- 라인 1255-1302: `.feed-main-row.*` 카드의 flex 속성
- 라인 1440-1492: `.control-section` 기본 flex 속성
- 라인 1515-1530: 계층 중복 선택자

**C. 고정 치수 변수화 가능 (32개)**
- 라인 1822-1992: 80px, 115px, 120px 등 반복
- 제안: `--feed-icon-size: 80px;` 변수 도입

#### 수정 필요 (!important 제거, 값 유지) (125개, 45%)

- 라인 21-102 (mode-common.scss): calc() 높이 계산 - 특이성 조정
- 라인 1243-1257: `.feed-container` max-width/width - 선택자 특이성
- 라인 1780-1802: `.path-content` 정렬 - 부모 설정으로 대체

#### 유지 필요 - Quasar 오버라이드 (40개, 14%)

| 라인 범위 | 개수 | 이유 |
|----------|------|------|
| 1215-1281 | 12 | q-page, q-card display 오버라이드 |
| 1308-1380 | 8 | 고정 높이 강제 (min-height/max-height) |
| 2214-2230 | 8 | `.fan-button` Quasar 버튼 크기 |
| 2270-2290 | 8 | 로딩/클릭 상태 transform |
| 1250, 1580, 1628 | 4 | box-sizing: border-box |

### 11.3 ScheduleTable.vue (29개)

#### 제거 가능 (6개, 21%)
- 라인 221-229: 중복 height/max-height
- 라인 259-266: 테이블 셀 정렬 (CSS 상속 충분)

#### 수정 필요 (18개, 62%)
- 라인 243-247: `.q-table__bottom/control` display: none - 특이성 증가
- 라인 297-317: 하이라이트 행 스타일 - 값 유지

#### 유지 필요 (5개, 17%)
- 라인 221-222, 228-229: 테이블 고정 높이 210px
- 라인 269: 헤더 height 50px

### 11.4 ScheduleChart.vue (3개)

**모두 유지 필요 (100%)** - 인라인 스타일

| 라인 | 용도 | 권장 |
|-----|------|------|
| 4 | q-card min-height/flex | 클래스로 추출 권장 |
| 8 | q-card-section 패딩 | 클래스로 추출 권장 |
| 14 | 차트 영역 높이 | ECharts 필수 |

---

## 12. Phase 3 상세 분석 (전문가 검토 완료)

**검토일**: 2026-01-18
**대상 파일**: EphemerisDesignationPage.vue (347개), PassSchedulePage.vue (321개)
**총 668개** - **HIGH RISK**

### 12.1 분석 요약

| 분류 | 개수 | 비율 | 설명 |
|-----|------|------|------|
| 안전 제거 가능 | 140개 | 21% | 마진/패딩 일괄, 폰트 스타일 |
| Quasar Props 대체 | 177개 | 26% | dense, flat, size props |
| 유지 필요 (HIGH RISK) | 351개 | 53% | 차트, 테이블, 레이아웃 |

### 12.2 EphemerisDesignationPage.vue (347개)

#### 패턴 분류

| 카테고리 | 개수 | 주요 라인 |
|---------|------|---------|
| Flexbox 레이아웃 | 68 | 20-29, 2677-2887 |
| 마진/패딩 제거 | 142 | 2668-2876, 2711-2814 |
| 높이 제한 | 94 | 2882-2970, 3044-3062 |
| 위치 조정 | 28 | 2956-2960, 3168 |
| 색상/배경 | 8 | 3787-3835 |
| 폰트 스타일 | 5 | 3206-3207, 3282 |

#### 안전 제거 가능 (72개, 21%)

**A. 마진/패딩 일괄 (52개)**
- 라인 2711-2876: `.ephemeris-mode > *` margin/padding 0
- 대체: Quasar 유틸리티 `q-mb-none`

**B. 폰트 스타일 (5개)**
- 라인 3206-3207: `font-size: 0.9rem`
- 대체: CSS 변수 `--theme-font-size-sm`

**C. Gap/Display 기본값 (15개)**
- 라인 2677-2685, 3161-3162

#### Quasar Props 대체 가능 (85개, 24%)

| 라인 범위 | 개수 | 대체 방법 |
|----------|------|---------|
| 3248-3275 | 16 | `q-btn size="sm" dense` |
| 3625-3657 | 12 | `q-input dense` |
| 2903-2904, 2746 | 12 | `q-card padding` prop |
| 3600-3640 | 28 | 테이블 행 클래스 단순화 |

#### 유지 필요 - HIGH RISK (190개, 55%)

**⚠️ 절대 제거 금지 영역**

| 영역 | 라인 범위 | 개수 | 이유 |
|-----|----------|------|------|
| **차트 중앙 위치** | 2956-3010 | 18 | position absolute + transform |
| **카드 높이 시스템** | 2882-2900, 3018-3062 | 42 | 3개 카드 동일 높이 |
| **TLE 에디터 높이** | 3180-3211 | 12 | 테이블과 높이 차이 보상 |
| **테이블 오버라이드** | 3495-3528 | 68 | :deep() 헤더 sticky |
| **테마 색상** | 3787-3835 | 8 | var(--theme-*) 필수 |

### 12.3 PassSchedulePage.vue (321개)

#### 패턴 분류

| 카테고리 | 개수 | 주요 라인 |
|---------|------|---------|
| Flexbox 레이아웃 | 52 | 2568-2796, 3023-3081 |
| 마진/패딩 제거 | 138 | 2525-2644, 2809-2858 |
| 높이 제한 | 78 | 2725-2787, 3436-3448 |
| 테이블 스타일 | 42 | 3495-3530, 3582-3620 |
| 색상/배경 | 4 | 3582-3620 |

#### 안전 제거 가능 (68개, 21%)

**A. 마진/패딩 일괄 (48개)**
- 라인 2525-2644, 2809-2858
- 대체: `q-mb-none`, `q-pb-none`

**B. Min-Height 자동 (14개)**
- 라인 3076-3125: `.q-input`, `.q-btn` min-height: auto
- 대체: `dense` prop

#### Quasar Props 대체 가능 (92개, 29%)

| 라인 범위 | 개수 | 대체 방법 |
|----------|------|---------|
| 3138-3203 | 10 | padding prop, CSS 변수 |
| 3216-3237 | 12 | `q-btn size="sm" dense` |
| 3489-3620 | 62 | 테이블 :deep() 최소화 |

#### 유지 필요 - HIGH RISK (161개, 50%)

**⚠️ 절대 제거 금지 영역**

| 영역 | 라인 범위 | 개수 | 이유 |
|-----|----------|------|------|
| **테이블 높이 210px** | 3436-3448 | 8 | 3개 행만 표시 |
| **테이블 Deep 오버라이드** | 3472-3571 | 68 | 페이지네이션 숨김, 헤더 sticky |
| **하이라이트 행 스타일** | 3581-3640 | 34 | current/next/highlight 색상 |
| **반응형 미디어 쿼리** | 3178-3237 | 15 | 모바일/데스크톱 레이아웃 |
| **입력 필드 높이 40px** | 3193-3245 | 28 | 컴팩트 제어 행 |

---

## 13. 전체 제거 우선순위 매트릭스

### 즉시 안전 제거 가능 (총 227개)

| Phase | 파일 | 개수 | 작업 |
|-------|-----|------|------|
| 1 | 6개 파일 (LOW) | 87개 | 블록 제거, inline→class |
| 2 | FeedPage | 114개 | 중복 선언 제거 |
| 3 | EphemerisDesignation/PassSchedule | ~20개 | 마진/패딩 일괄 |

### Props/클래스로 대체 가능 (총 320개)

| Phase | 대상 | 개수 | 작업 |
|-------|-----|------|------|
| 2 | FeedPage | 125개 | 특이성 조정, 값 유지 |
| 2 | ScheduleTable | 18개 | 특이성 조정 |
| 3 | EphemerisDesignation | 85개 | dense, flat props |
| 3 | PassSchedule | 92개 | dense, flat props |

### 절대 유지 필요 (총 519개)

| Phase | 파일 | 개수 | 이유 |
|-------|-----|------|------|
| 2 | FeedPage | 40개 | Quasar 오버라이드 |
| 2 | ScheduleTable | 5개 | 테이블 높이 |
| 2 | ScheduleChart | 3개 | ECharts 필수 |
| 3 | EphemerisDesignation | 190개 | 차트, 테이블, 레이아웃 |
| 3 | PassSchedule | 161개 | 테이블, 하이라이트 |
| 미지정 | mode-common.scss 등 | 120개 | 전역 스타일 |

---

## 14. Phase 2 테스트 체크리스트

### FeedPage.vue 레이아웃 테스트

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 3열 그리드 정렬 | S/X/Ka 밴드 카드 동일 높이 | ⬜ |
| 2 | RF Switch 경로 표시 | SVG 아이콘 오버플로우 없음 | ⬜ |
| 3 | LNA 아이콘 크기 | 모든 아이콘 80px 유지 | ⬜ |
| 4 | Fan 섹션 높이 | 115px 고정 유지 | ⬜ |
| 5 | 범례 그리드 정렬 | 하단 범례 정상 표시 | ⬜ |
| 6 | 3밴드 모드 레이아웃 | Ka 밴드 추가 시 레이아웃 유지 | ⬜ |

### ScheduleTable.vue 테스트

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 테이블 높이 210px | 스크롤바 정상 표시 | ⬜ |
| 2 | 헤더 sticky | 스크롤 시 헤더 고정 | ⬜ |
| 3 | 현재 스케줄 하이라이트 | 초록색 배경 표시 | ⬜ |
| 4 | 다음 스케줄 하이라이트 | 노란색 배경 표시 | ⬜ |
| 5 | 페이지네이션 숨김 | 하단 컨트롤 안 보임 | ⬜ |

### ScheduleChart.vue 테스트

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 차트 높이 360px | ECharts 렌더링 정상 | ⬜ |
| 2 | 카드 섹션 패딩 | 차트 내용 잘림 없음 | ⬜ |

---

## 15. Phase 3 테스트 체크리스트

### EphemerisDesignationPage.vue 테스트

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 3개 카드 동일 높이 | Position View, Control, TLE 카드 | ⬜ |
| 2 | **차트 중앙 정렬** | Position View 500px 원형 차트 | ⬜ |
| 3 | TLE 에디터 높이 140px | 텍스트 영역 크기 | ⬜ |
| 4 | TLE 버튼 그룹 위치 | 에디터 하단 정렬 | ⬜ |
| 5 | 테이블 헤더 sticky | 스케줄 목록 스크롤 | ⬜ |
| 6 | 정렬 아이콘 위치 | 테이블 헤더 정렬 버튼 | ⬜ |
| 7 | 테마 색상 적용 | 다크/라이트 모드 전환 | ⬜ |

### PassSchedulePage.vue 테스트

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 테이블 높이 210px | 3개 행만 표시 | ⬜ |
| 2 | **현재 추적 행 하이라이트** | 초록색 배경 + 테두리 | ⬜ |
| 3 | **다음 추적 행 하이라이트** | 노란색 배경 | ⬜ |
| 4 | 컨트롤 입력 높이 40px | Offset, Reset 입력창 | ⬜ |
| 5 | 반응형 레이아웃 | 768px 미만에서 세로 배치 | ⬜ |
| 6 | 스케줄 폼 정렬 | 폼 요소 상단 정렬 | ⬜ |
| 7 | 페이지네이션 숨김 | 하단 컨트롤 안 보임 | ⬜ |

---

**작업 예상 일정**: Phase 1 → Phase 2 → Phase 3 순차 진행
**테스트 병행**: 각 Phase 완료 후 테스트 필수
