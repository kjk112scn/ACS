# RFC-009: 접근성(Accessibility) 개선

> **버전**: 1.0.0 | **작성일**: 2026-01-13
> **상태**: Draft | **우선순위**: P2
> **역할**: 웹 접근성 (WCAG 준수, aria 속성, 키보드 네비게이션, 스크린 리더 지원)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0.0 | 2026-01-13 | 전수조사 결과 기반 신규 작성: aria 속성 0개 발견 |

---

## 1. 배경 (Context)

### 왜 이 변경이 필요한가?

전수조사 결과, ACS 프론트엔드에 **aria 속성이 전무**한 것으로 확인되었습니다.

| 현황 | 문제 |
|------|------|
| aria 속성 **0개** | 스크린 리더 사용 불가 |
| 키보드 네비게이션 미지원 | 마우스 없이 조작 불가 |
| 색상 대비 미검증 | 저시력자 사용 어려움 |

### 접근성이 중요한 이유

1. **법적 요구**: 공공기관/기업 웹 접근성 의무화 추세
2. **사용자 확대**: 장애인, 고령자 사용 가능
3. **UX 향상**: 키보드 네비게이션은 모든 사용자에게 유용
4. **SEO 개선**: 접근성 좋은 사이트가 검색 엔진에도 유리

---

## 2. 현재 상태 (Current State)

### 2.1 전수조사 결과

| 항목 | 현재 상태 | 심각도 |
|------|----------|--------|
| aria-label | **0개** | Critical |
| aria-describedby | **0개** | Critical |
| aria-live | **0개** | High |
| role 속성 | **0개** | High |
| tabindex 관리 | 미확인 | Medium |

### 2.2 주요 문제 영역

#### A. 대시보드 (DashboardPage.vue)

| 요소 | 문제 |
|------|------|
| 실시간 데이터 | aria-live 없음, 스크린 리더 인지 불가 |
| 차트 | 대체 텍스트 없음 |
| 상태 표시 LED | 색상만으로 정보 전달 |

#### B. 패스 스케줄 (PassSchedulePage.vue)

| 요소 | 문제 |
|------|------|
| 스케줄 테이블 | role="grid" 없음 |
| 선택 행 | aria-selected 없음 |
| 드래그 앤 드롭 | 키보드 대안 없음 |

#### C. 모드 전환

| 요소 | 문제 |
|------|------|
| 모드 버튼 | aria-pressed 없음 |
| 비활성 버튼 | aria-disabled 없음 |
| 현재 모드 | aria-current 없음 |

---

## 3. 제안 (Proposal)

### 3.1 Phase 1: 기본 aria 속성 추가 (P1)

#### A. 버튼 및 인터랙티브 요소

```vue
<!-- Before -->
<q-btn @click="startTracking">추적 시작</q-btn>

<!-- After -->
<q-btn
  @click="startTracking"
  aria-label="위성 추적 시작"
  :aria-disabled="isDisabled"
>
  추적 시작
</q-btn>
```

#### B. 실시간 데이터 영역

```vue
<!-- 실시간 업데이트 알림 -->
<div
  aria-live="polite"
  aria-atomic="true"
  class="sr-only"
>
  {{ statusMessage }}
</div>

<!-- 데이터 표시 -->
<div aria-label="안테나 방위각">
  {{ azimuth }}°
</div>
```

#### 체크리스트

- [ ] 모든 버튼에 aria-label 추가
- [ ] 인풋 필드에 aria-describedby 연결
- [ ] 실시간 영역에 aria-live 추가
- [ ] 토글 버튼에 aria-pressed 추가

### 3.2 Phase 2: 키보드 네비게이션 (P2)

#### A. 포커스 관리

```vue
<script setup lang="ts">
import { useFocusTrap } from '@vueuse/integrations/useFocusTrap'

// 모달 포커스 트랩
const { activate, deactivate } = useFocusTrap(modalRef)
</script>
```

#### B. 스킵 링크

```vue
<!-- 스킵 네비게이션 -->
<a href="#main-content" class="skip-link">
  본문으로 바로가기
</a>
```

#### 체크리스트

- [ ] 논리적 tabindex 순서
- [ ] 모달 포커스 트랩
- [ ] 스킵 링크 추가
- [ ] 키보드 단축키 문서화

### 3.3 Phase 3: 차트 접근성 (P2)

#### A. 대체 텍스트

```vue
<div
  role="img"
  :aria-label="chartDescription"
>
  <canvas ref="chartRef" aria-hidden="true" />
</div>

<div class="sr-only">
  <!-- 차트 데이터 텍스트 버전 -->
  <table>
    <caption>위성 추적 데이터</caption>
    <!-- ... -->
  </table>
</div>
```

#### B. 데이터 테이블 제공

| 차트 | 대안 |
|------|------|
| Position View | 좌표 데이터 테이블 |
| 스케줄 타임라인 | 스케줄 리스트 |
| 상태 게이지 | 텍스트 상태 표시 |

#### 체크리스트

- [ ] 모든 차트에 aria-label
- [ ] 데이터 테이블 대안 제공
- [ ] 차트 업데이트 알림

### 3.4 Phase 4: 색상 및 대비 (P2)

#### A. WCAG 대비율 준수

| 요소 | 최소 대비 |
|------|----------|
| 일반 텍스트 | 4.5:1 |
| 대형 텍스트 | 3:1 |
| UI 컴포넌트 | 3:1 |

#### B. 색상만으로 정보 전달 금지

```vue
<!-- Before (문제) -->
<span :style="{ color: isError ? 'red' : 'green' }">
  {{ status }}
</span>

<!-- After (개선) -->
<span :class="statusClass">
  <q-icon :name="statusIcon" />
  {{ statusText }}
</span>
```

#### 체크리스트

- [ ] 색상 대비 검증 (axe DevTools)
- [ ] 색상 외 정보 전달 방법 추가
- [ ] 고대비 테마 옵션 (선택)

---

## 4. 영향 분석 (Impact)

### 4.1 변경 범위

| 영역 | 변경 | 영향 |
|------|------|------|
| 모든 버튼 | aria 속성 추가 | 스크린 리더 지원 |
| 실시간 영역 | aria-live 추가 | 상태 변경 알림 |
| 차트 | 대체 텍스트 | 시각 장애인 지원 |
| 색상 | 대비 개선 | 저시력자 지원 |

### 4.2 위험 요소

| 위험 | 대응 |
|------|------|
| 기존 동작 영향 | aria 속성만 추가, 로직 변경 없음 |
| 개발 시간 | 점진적 적용, 주요 페이지 우선 |

---

## 5. 마이그레이션 (Migration)

### 5.1 단계별 적용

```
Phase 1: 기본 aria 속성 (P1)
├── 버튼 aria-label
├── 폼 aria-describedby
└── 실시간 aria-live

Phase 2: 키보드 네비게이션 (P2)
├── tabindex 정리
├── 포커스 트랩
└── 스킵 링크

Phase 3: 차트 접근성 (P2)
├── 대체 텍스트
└── 데이터 테이블

Phase 4: 색상 및 대비 (P2)
├── 대비율 검증
└── 색상 외 정보 전달
```

### 5.2 우선순위

| 작업 | 우선순위 | 이유 |
|------|----------|------|
| 버튼 aria-label | **P1** | 가장 기본적인 접근성 |
| 키보드 네비게이션 | **P1** | 마우스 없이 사용 가능 |
| 차트 대체 텍스트 | P2 | 복잡도 높음 |
| 색상 대비 | P2 | 디자인 변경 필요 |

---

## 6. 검증 (Verification)

### 6.1 테스트 도구

| 도구 | 용도 |
|------|------|
| axe DevTools | 자동 접근성 검사 |
| NVDA | 스크린 리더 테스트 (무료) |
| Lighthouse | 접근성 점수 |
| Contrast Checker | 색상 대비 검증 |

### 6.2 성공 기준

| 기준 | 목표 |
|------|------|
| axe 오류 | 0개 |
| Lighthouse 접근성 | 80점+ |
| WCAG Level | A 준수 |
| 키보드 네비게이션 | 모든 기능 접근 가능 |

### 6.3 체크리스트 요약

**Phase 1 (기본)**
- [ ] 버튼 aria-label (103개 파일)
- [ ] 인풋 aria-describedby
- [ ] 실시간 영역 aria-live
- [ ] 토글 aria-pressed
- [ ] axe DevTools 오류 0개

**Phase 2 (키보드)**
- [ ] tabindex 정리
- [ ] 포커스 트랩
- [ ] 스킵 링크
- [ ] Enter/Space 키 처리

**Phase 3 (차트)**
- [ ] 차트 aria-label
- [ ] 데이터 테이블 대안
- [ ] 차트 업데이트 알림

**Phase 4 (시각)**
- [ ] 색상 대비 4.5:1+
- [ ] 색상 외 정보 전달
- [ ] 포커스 표시 명확화

---

## 7. 관련 RFC

| RFC | 관계 | 설명 |
|-----|------|------|
| [RFC-008](./RFC-008_Frontend_Restructuring.md) | 연관 | FE 리팩토링 시 접근성 함께 적용 |
| RFC-005 (예정) | 후속 | 접근성 테스트 자동화 |

### 의존성 그래프

```
RFC-008 (FE 구조화)
    │
    ├── Phase 1-5 (기존)
    │
    └──→ RFC-009 (이 문서)
             │
             ├── Phase 1: aria 기본
             ├── Phase 2: 키보드
             ├── Phase 3: 차트
             └── Phase 4: 시각
```

---

## 8. 참고 자료

- [WCAG 2.1 Guidelines](https://www.w3.org/TR/WCAG21/)
- [MDN ARIA](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA)
- [Vue A11y](https://vue-a11y.com/)
- [Quasar Accessibility](https://quasar.dev/start/how-to-use-vue#accessibility)

---

**작성자**: Claude
**검토자**: -
**승인일**: -
