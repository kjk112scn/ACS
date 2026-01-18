# CSS !important Phase 3 세부 실행 계획

**작성일**: 2026-01-18
**대상**: EphemerisDesignationPage.vue, PassSchedulePage.vue

---

## 1. 전체 요약

| 파일 | 현재 !important | 예상 제거 | 난이도 |
|-----|:---------------:|:---------:|:------:|
| EphemerisDesignationPage.vue | 347개 | ~80개 | 🔴 HIGH |
| PassSchedulePage.vue | 321개 | ~80개 | 🔴 HIGH |
| **합계** | **668개** | **~160개** | |

**주요 발견**:
- 두 파일 간 **90% 이상 동일한 CSS** 구조
- OffsetControls.vue와 중복되는 offset 스타일
- 파일 내 동일 선택자 중복 정의

---

## 2. 안전 제거 대상 (Step 1-3)

### Step 1: OffsetControls.vue와 중복 제거

**EphemerisDesignationPage.vue** (라인 2575-2655, ~0개 !important)
**PassSchedulePage.vue** (라인 2434-2514, ~0개 !important)

두 파일에서 다음 CSS 블록은 OffsetControls.vue에 이미 정의되어 있음:

```scss
// OffsetControls.vue에서 정의됨 - 삭제 가능
.flexible-offset-layout { ... }
.offset-group { ... }
.position-offset-label { ... }  // 기본 스타일만
.cal-time-field { ... }
.vertical-button-group { ... }
.vertical-buttons { ... }
.offset-input { ... }
.cal-time-input { ... }
@media (max-width: 1900px) { .flexible-offset-layout, .offset-group, .position-offset-label, .cal-time-field }
@media (min-width: 1901px) { .flexible-offset-layout, .offset-group, .position-offset-label }
```

**위험도**: 🟢 LOW (OffsetControls가 scoped 컴포넌트로 분리됨)
**제거 개수**: ~0개 !important (스타일 블록 정리, !important 없는 중복)

---

### Step 2: 파일 내 중복 선택자 통합

#### EphemerisDesignationPage.vue

| 선택자 | 중복 횟수 | 통합 후 |
|-------|:--------:|:-------:|
| `.position-offset-label` | 4회 | 1회 |
| `.position-offset-label .text-subtitle2` | 3회 | 1회 |
| `.ephemeris-mode .main-content-row>[class*="col-"]` | 3회 | 1회 |
| `.ephemeris-mode .main-content-row>[class*="col-"] .q-card` | 3회 | 1회 |
| `.ephemeris-mode .main-content-row>[class*="col-"] .q-card-section` | 3회 | 1회 |
| `.compact-control .row.q-gutter-none` | 2회 | 1회 |

**제거 예상**: ~40개 !important

#### PassSchedulePage.vue

| 선택자 | 중복 횟수 | 통합 후 |
|-------|:--------:|:-------:|
| `.position-offset-label` | 3회 | 1회 |
| `.position-offset-label .text-subtitle2` | 2회 | 1회 |
| `.pass-schedule-mode .main-content-row>[class*="col-"]` | 2회 | 1회 |
| `.compact-control .row.q-gutter-none` | 2회 | 1회 |

**제거 예상**: ~30개 !important

---

### Step 3: scoped 내 불필요한 !important 제거

#### 분류 기준

| 구분 | 설명 | 조치 |
|-----|------|-----|
| A. scoped 유일 선언 | 외부 충돌 없음 | ✅ !important 제거 |
| B. Quasar 오버라이드 | `:deep()` 사용 | ⚠️ 유지 필요 |
| C. 상태 기반 스타일 | hover, active 등 | ⚠️ 유지 필요 |
| D. 반응형 오버라이드 | @media 내부 | ⚠️ 검토 필요 |

#### EphemerisDesignationPage.vue 안전 제거 대상 (~40개)

```scss
// scoped 내 유일한 선언 - !important 제거 가능
.section-title { ... }               // 라인 2696
.ephemeris-form { ... }              // 라인 3100-3109
.form-row { ... }                    // 라인 3111-3121
.schedule-header { ... }             // 라인 3123-3133
.schedule-table { ... }              // 라인 3388-3392
.schedule-info { ... }               // 라인 3395-3407
.info-row { ... }                    // 라인 3409-3422
.tle-editor { ... }                  // 라인 3341-3344
.full-width { ... }                  // 라인 3337-3339
```

#### PassSchedulePage.vue 안전 제거 대상 (~40개)

```scss
// scoped 내 유일한 선언 - !important 제거 가능
.section-title { ... }               // 라인 2654
.schedule-container { ... }          // 라인 2646-2652
.control-card { ... }                // 라인 2929-2933
.compact-control-row { ... }         // 라인 3169-3175
.control-input { ... }               // 라인 3187-3195
.control-buttons { ... }             // 라인 3207-3221
```

---

## 3. 유지 필요 영역

### 반드시 유지해야 하는 !important

| 파일 | 선택자 | 이유 |
|-----|-------|------|
| 양쪽 | `.mode .main-content-row` | Quasar row 오버라이드 |
| 양쪽 | `.control-section` | Quasar card 오버라이드 |
| 양쪽 | `.chart-area > div` | ECharts 동적 생성 요소 |
| 양쪽 | `:deep(.q-table...)` | Quasar 내부 오버라이드 |
| 양쪽 | `:deep(.q-btn...)` | Quasar 버튼 오버라이드 |
| 양쪽 | `@media` 내 반응형 | 우선순위 보장 |

**예상 유지**: ~450개 (Quasar 오버라이드 필수)

---

## 4. 실행 순서

| Step | 작업 | 대상 | 예상 제거 | 난이도 |
|------|-----|-----|:--------:|:------:|
| 1 | OffsetControls 중복 제거 | 양쪽 | 0개 | 🟢 |
| 2-A | 선택자 중복 통합 | Ephemeris | ~40개 | 🟡 |
| 2-B | 선택자 중복 통합 | PassSchedule | ~30개 | 🟡 |
| 3-A | scoped 불필요 제거 | Ephemeris | ~40개 | 🟡 |
| 3-B | scoped 불필요 제거 | PassSchedule | ~40개 | 🟡 |
| 4 | 빌드 검증 | - | - | - |

---

## 5. 롤백 계획

```bash
# 각 Step 후 빌드 검증
cd frontend && npm run build

# 문제 발생 시 롤백
git checkout -- frontend/src/pages/mode/EphemerisDesignationPage.vue
git checkout -- frontend/src/pages/mode/PassSchedulePage.vue
```

---

## 6. Phase 3 후 예상 현황

| 파일 | 이전 | 이후 | 감소 |
|-----|:----:|:----:|:----:|
| EphemerisDesignationPage.vue | 347개 | ~270개 | ~80개 |
| PassSchedulePage.vue | 321개 | ~250개 | ~70개 |
| **Phase 3 총합** | **668개** | **~520개** | **~150개** |

---

## 7. 전체 프로젝트 현황 (Phase 1-3 완료 후)

| Phase | 제거 | 상태 |
|-------|:----:|:----:|
| Phase 1 | 87개 | ✅ 완료 |
| Phase 2 | 124개 | ✅ 완료 |
| Phase 3 | ~150개 | 📋 실행 대기 |
| **총합** | **~361개** | |

**남은 !important**: ~670개 (대부분 Quasar 오버라이드 필수)

---

**작성자**: Claude Opus 4.5
**다음 단계**: Step 1부터 순차 실행
