# Architecture Refactoring - 마스터 문서

> **Last Updated**: 2026-01-18
> **Status**: Active

---

## 전체 진행 현황

| 구분 | 상태 | 완료율 |
|-----|:----:|:-----:|
| Phase 1-4 (BE/FE 기본) | ✅ 완료 | 100% |
| Phase 5 (컴포넌트 분리) | ✅ 완료 | 100% |
| **추가 리팩토링** | 🔄 진행중 | 50% |
| CSS !important Phase 1 | ✅ 완료 | 100% |
| CSS !important Phase 2 | ✅ 완료 | 100% |
| CSS !important Phase 3 | ✅ 완료 | 100% |
| Quasar 근본 개선 | 📋 계획수립 | 0% |

---

## 문서 구조

### 핵심 문서

| 문서 | 설명 | 상태 |
|-----|------|:----:|
| [PLAN.md](./PLAN.md) | 통합 리팩토링 계획 | ✅ |
| [TRACKER.md](./TRACKER.md) | 실행 체크리스트 | ✅ |
| [IMPROVEMENT_ROADMAP.md](./IMPROVEMENT_ROADMAP.md) | 7-Expert 종합 로드맵 | ✅ |

### FE 리팩토링

| 문서 | 설명 | 상태 |
|-----|------|:----:|
| [FE_REFACTORING_PLAN.md](./FE_REFACTORING_PLAN.md) | FE 리팩토링 상세 계획 | ✅ |
| [FE_REVIEW_2026-01-18.md](./FE_REVIEW_2026-01-18.md) | FE 전문가 리뷰 결과 | ✅ |
| [FE_Refactoring_Test_Checklist.md](./FE_Refactoring_Test_Checklist.md) | 통합 테스트 체크리스트 (79개) | 📋 |

### CSS 관련

| 문서 | 설명 | 상태 |
|-----|------|:----:|
| [CSS_Important_Cleanup_Plan.md](./CSS_Important_Cleanup_Plan.md) | !important 정리 계획 (Phase 1: 87개 완료) | ✅ P1 |
| [CSS_Quasar_Override_Strategy.md](./CSS_Quasar_Override_Strategy.md) | Quasar 근본 해결 전략 | 📋 |

### 기타

| 문서 | 설명 | 상태 |
|-----|------|:----:|
| [PHASE5_SEPARATION_PLAN.md](./PHASE5_SEPARATION_PLAN.md) | 대형 파일 분리 계획 | ✅ |
| [EXPERT_REVIEW_2026-01-18.md](./EXPERT_REVIEW_2026-01-18.md) | 전문가 종합 리뷰 | ✅ |

---

## 추후 작업 목록 (TODO)

### 1. 코드 수정 완료 - 테스트 필요

| 작업 | 파일 | 테스트 문서 |
|-----|-----|-----------|
| useErrorHandler 적용 | FeedPage, EphemerisDesignation, PassSchedule | [Test Checklist](./FE_Refactoring_Test_Checklist.md) Part 1 |
| defineComponent 제거 | StepPage, SlewPage, StandbyPage | [Test Checklist](./FE_Refactoring_Test_Checklist.md) Part 1 |
| ControlButtonBar 공용화 | 4개 모드 페이지 | [Test Checklist](./FE_Refactoring_Test_Checklist.md) Part 1 |
| ModeCard/ModeLayout 삭제 | components/common/ | 빌드 확인 |

### 2. CSS !important 정리

| Phase | 대상 | 제거 | 난이도 | 상태 |
|-------|-----|:----:|:-----:|:----:|
| Phase 1 | 6개 파일 (LOW) | 87개 | 🟢 | ✅ 완료 |
| Phase 2 | FeedPage 등 3개 | 124개 | 🟡 | ✅ 완료 |
| Phase 3 | Ephemeris/PassSchedule | 24개 | 🔴 | ✅ 완료 |

**현황**:
- ✅ Phase 1 완료: 87개 제거
- ✅ Phase 2 완료: 124개 제거 (FeedPage 106, ScheduleTable 1, ScheduleChart 17)
- ✅ Phase 3 완료: 24개 제거 (EphemerisDesignation 22, PassSchedule 2)
- 총 제거: **235개**
- **남은 !important**: ~778개
- 상세: [CSS_Phase2_Execution_Plan.md](./CSS_Phase2_Execution_Plan.md), [CSS_Phase3_Execution_Plan.md](./CSS_Phase3_Execution_Plan.md)

### 3. Quasar 근본 개선 (장기)

| 전략 | 효과 | 작업량 | 예상 제거 |
|-----|:---:|:-----:|:--------:|
| SCSS 변수 오버라이드 | 높음 | 중간 | ~200개 |
| CSS Layers | 높음 | 낮음 | ~150개 |
| 컴포넌트 래퍼 | 중간 | 높음 | ~100개 |

**상세**: [CSS_Quasar_Override_Strategy.md](./CSS_Quasar_Override_Strategy.md)

---

## 우선순위 권장

### 즉시 (테스트만)
1. ✅ useErrorHandler/catch 블록 테스트 (Part 1: 41개)
2. ✅ 빌드 확인 (`npm run build`)
3. ✅ CSS Phase 1 실행 (87개 제거)
4. ✅ CSS Phase 2 실행 (124개 제거)
5. ✅ CSS Phase 3 실행 (24개 제거)
6. ✅ CSS Phase 1-3 수동 테스트 완료

### 단기 (1-2주)
1. 📋 Quasar SCSS 변수 확장

### 중기 (2-4주)
1. 📋 컴포넌트 래퍼 도입

### 장기 (1개월+)
1. 📋 CSS Layers 검토
2. 📋 Design Token 시스템 구축

---

## 검증 체크리스트

### 코드 수정 후 필수 확인

- [ ] `npm run build` 성공
- [ ] `npx vue-tsc --noEmit` 타입 체크 통과
- [ ] 각 페이지 로드 정상
- [ ] 다크/라이트 테마 전환
- [ ] 반응형 레이아웃 (1024px, 768px)

### CSS 수정 후 필수 확인

- [ ] 레이아웃 깨짐 없음
- [ ] 차트 렌더링 정상
- [ ] 테이블 기능 (스크롤, 헤더 고정, 하이라이트)
- [ ] Quasar 컴포넌트 동작

---

## 관련 컨텍스트 문서

| 문서 | 경로 |
|-----|-----|
| FE 아키텍처 | `docs/architecture/context/architecture/frontend.md` |
| BE 아키텍처 | `docs/architecture/context/architecture/backend.md` |
| 리팩토링 힌트 | `docs/architecture/context/analysis/synthesis/refactoring-hints.md` |

---

## 히스토리

| 날짜 | 작업 | 상태 |
|-----|-----|:----:|
| 2026-01-15 | Phase 1-4 완료 | ✅ |
| 2026-01-17 | Phase 5 완료 | ✅ |
| 2026-01-18 | FE 추가 리팩토링 (catch, defineComponent) | ✅ |
| 2026-01-18 | CSS !important 분석 완료 | ✅ |
| 2026-01-18 | Quasar 근본 해결 전략 수립 | 📋 |
| 2026-01-18 | **CSS Phase 1 실행 완료 (87개 제거)** | ✅ |
| 2026-01-18 | **CSS Phase 2 실행 완료 (124개 제거)** | ✅ |
| 2026-01-18 | **CSS Phase 3 완료 (24개 제거)** | ✅ |

---

**Note**: `legacy/` 폴더에는 이전 문서들이 보관되어 있습니다. 필요 시 참고하세요.
