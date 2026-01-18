# CSS !important 근본 해결 전략

**작성일**: 2026-01-18
**목적**: Quasar 프레임워크와의 스타일 충돌을 근본적으로 해결
**대상**: 현재 "절대 유지 필요"로 분류된 519개 !important

---

## 1. 현재 문제

### 1.1 원인
- Quasar 프레임워크가 **높은 CSS specificity**로 기본 스타일 적용
- 커스텀 스타일 적용 시 !important로 강제 오버라이드 필요
- 결과: 1,066개의 !important 중 519개(49%)가 제거 불가

### 1.2 영향 범위

| 파일 | !important 개수 | 원인 |
|-----|----------------|------|
| EphemerisDesignationPage.vue | 190개 | 차트, 카드, 테이블 오버라이드 |
| PassSchedulePage.vue | 161개 | 테이블, 하이라이트 행 |
| FeedPage.vue | 40개 | 버튼, 레이아웃 |
| mode-common.scss | ~120개 | 전역 모드 스타일 |
| 기타 | 8개 | ScheduleTable, ScheduleChart |

---

## 2. 해결 전략

### 2.1 전략 1: Quasar SCSS 변수 오버라이드 (권장)

**원리**: Quasar 빌드 시점에 변수를 변경하여 기본값 자체를 수정

**파일**: `frontend/src/css/quasar.variables.scss`

```scss
// ============================================
// Quasar 컴포넌트 기본값 오버라이드
// ============================================

// Card
$card-padding: 0.8rem;
$card-border-radius: 8px;

// Field (Input, Select)
$field-dense-height: 32px;
$field-standout-bg: transparent;

// Button
$btn-dense-min-height: 28px;
$btn-padding: 4px 12px;

// Table
$table-thead-bg: var(--theme-table-header-bg);
$table-border-radius: 0;
$table-dense-padding: 4px 8px;

// ============================================
// 프로젝트 전용 변수
// ============================================

// 모드 페이지 레이아웃
$mode-card-min-height: 360px;
$mode-card-padding: 0.8rem;

// 차트 컨테이너
$chart-container-height: 360px;
$position-chart-size: 500px;

// 테이블
$schedule-table-height: 210px;
$schedule-table-row-height: 50px;
```

**적용 효과**:
- `min-height: 360px !important` → `min-height: $mode-card-min-height` (변수 사용)
- Quasar 기본값이 변경되어 !important 불필요

**예상 제거**: ~200개 (39%)

---

### 2.2 전략 2: CSS Layers (최신 CSS)

**원리**: CSS 캐스케이드 레이어로 우선순위 명시적 제어

**파일**: `frontend/src/css/app.scss`

```scss
// 레이어 순서 정의 (뒤가 높은 우선순위)
@layer reset, quasar, app, overrides;

// Quasar 스타일 (자동 적용)
@layer quasar {
  // Quasar가 여기 삽입됨
}

// 앱 기본 스타일
@layer app {
  .mode-shell { /* ... */ }
  .axis-panel { /* ... */ }
}

// 오버라이드 (최상위 우선순위)
@layer overrides {
  .position-view-card {
    min-height: 360px;  // !important 없이 적용
  }

  .schedule-table {
    height: 210px;  // !important 없이 적용
  }
}
```

**브라우저 지원**:
| 브라우저 | 지원 버전 |
|---------|----------|
| Chrome | 99+ (2022.03) |
| Firefox | 97+ (2022.02) |
| Safari | 15.4+ (2022.03) |
| Edge | 99+ (2022.03) |

**주의**: IE11 미지원, 구형 브라우저 확인 필요

**예상 제거**: ~150개 (29%)

---

### 2.3 전략 3: 커스텀 Quasar 컴포넌트 래퍼

**원리**: Quasar 컴포넌트를 래핑하여 스타일 격리

**예시**: `AcsCard.vue`

```vue
<template>
  <q-card class="acs-card" v-bind="$attrs">
    <slot />
  </q-card>
</template>

<script setup lang="ts">
defineOptions({ inheritAttrs: false })
</script>

<style scoped>
.acs-card {
  min-height: v-bind('minHeight');
  border-radius: 8px;
}

.acs-card :deep(.q-card__section) {
  padding: 0.8rem;
}
</style>
```

**사용**:
```vue
<!-- 변경 전 -->
<q-card style="min-height: 360px !important;">

<!-- 변경 후 -->
<AcsCard min-height="360px">
```

**장점**:
- scoped 스타일로 충돌 방지
- props로 동적 제어 가능
- 재사용성 높음

**예상 제거**: ~100개 (19%)

---

### 2.4 전략 4: :where() 선택자 활용

**원리**: specificity를 0으로 만들어 쉽게 오버라이드

**Quasar 측 적용 불가** (프레임워크 소스 수정 필요)

**앱 측 활용**:
```scss
// 기본 스타일 (specificity 0)
:where(.mode-card) {
  min-height: 300px;
}

// 페이지별 오버라이드 (쉽게 덮어씀)
.ephemeris-mode .mode-card {
  min-height: 360px;  // !important 없이 적용
}
```

**한계**: Quasar 기본 스타일에는 적용 불가

---

## 3. 권장 구현 순서

### Phase A: 즉시 적용 (1주)

1. **quasar.variables.scss 확장**
   - 카드, 필드, 버튼 기본값 오버라이드
   - 예상 효과: ~100개 제거

2. **CSS 변수 통합**
   - 반복되는 치수(360px, 210px, 500px)를 변수화
   - 예상 효과: ~50개 제거

### Phase B: 중기 개선 (2-3주)

1. **커스텀 컴포넌트 래퍼 생성**
   ```
   components/common/
   ├── AcsCard.vue
   ├── AcsTable.vue
   ├── AcsInput.vue
   └── AcsButton.vue
   ```

2. **기존 페이지에 래퍼 적용**
   - EphemerisDesignationPage
   - PassSchedulePage

### Phase C: 장기 아키텍처 (1개월+)

1. **CSS Layers 도입** (브라우저 지원 확인 후)
2. **Quasar 테마 시스템 완전 커스터마이징**
3. **Design Token 시스템 구축**

---

## 4. 파일별 적용 계획

### 4.1 EphemerisDesignationPage.vue (190개 → ~80개)

| 영역 | 현재 | 해결 전략 | 예상 잔여 |
|-----|-----|----------|----------|
| 카드 높이 | 42개 | SCSS 변수 | 0개 |
| 차트 위치 | 18개 | 래퍼 컴포넌트 | 0개 |
| 테이블 헤더 | 68개 | AcsTable 래퍼 | ~40개 |
| TLE 에디터 | 12개 | CSS 변수 | 0개 |
| 테마 색상 | 8개 | 유지 필요 | 8개 |
| 기타 | 42개 | 특이성 조정 | ~32개 |

### 4.2 PassSchedulePage.vue (161개 → ~60개)

| 영역 | 현재 | 해결 전략 | 예상 잔여 |
|-----|-----|----------|----------|
| 테이블 높이 | 8개 | SCSS 변수 | 0개 |
| 테이블 Deep | 68개 | AcsTable 래퍼 | ~30개 |
| 하이라이트 행 | 34개 | CSS 변수 + 클래스 | ~10개 |
| 반응형 | 15개 | 미디어 쿼리 정리 | ~10개 |
| 입력 필드 | 28개 | SCSS 변수 | ~10개 |

### 4.3 mode-common.scss (~120개 → ~40개)

| 영역 | 현재 | 해결 전략 | 예상 잔여 |
|-----|-----|----------|----------|
| calc() 높이 | ~40개 | CSS 변수 | ~10개 |
| flex 레이아웃 | ~30개 | 유틸리티 클래스 | ~10개 |
| Quasar 오버라이드 | ~50개 | SCSS 변수 | ~20개 |

---

## 5. 검증 체크리스트

### 구현 전 확인
- [ ] 브라우저 지원 범위 확인 (CSS Layers 사용 시)
- [ ] Quasar 버전 확인 (SCSS 변수 호환성)
- [ ] 기존 테마 시스템과의 충돌 검토

### 구현 후 확인
- [ ] 다크/라이트 테마 전환 정상
- [ ] 반응형 레이아웃 유지
- [ ] 차트 렌더링 정상
- [ ] 테이블 기능 (정렬, 스크롤, 하이라이트) 정상
- [ ] 빌드 사이즈 비교

---

## 6. 예상 결과

### Before (현재)
```
총 !important: 1,066개
├── 제거 가능: 547개 (51%)
└── 유지 필요: 519개 (49%)  ← 이 부분 개선
```

### After (전략 적용 후)
```
총 !important: ~300개 (72% 감소)
├── 남은 Quasar 오버라이드: ~200개
└── 테마/상태 관련 필수: ~100개
```

---

## 7. 참고 자료

- [Quasar SCSS Variables](https://quasar.dev/style/sass-scss-variables)
- [CSS Cascade Layers](https://developer.mozilla.org/en-US/docs/Web/CSS/@layer)
- [CSS Specificity](https://developer.mozilla.org/en-US/docs/Web/CSS/Specificity)

---

**문서 버전**: 1.0
**다음 검토**: Phase A 완료 후
