# Active Features (진행 중 작업)

> **최종 수정**: 2026-01-20

현재 진행 중인 기능 개발 및 리팩토링 작업 목록입니다.

---

## 진행 현황 Dashboard

| 프로젝트 | 진행률 | 우선순위 | 상태 |
|----------|:------:|:--------:|:----:|
| [PassSchedule 상태머신](#1-passschedule-상태머신-리팩토링) | 88% | P0 | ✅ A-D |
| [Architecture Refactoring](#2-architecture-refactoring-아키텍처-리팩토링) | 20% | P1 | 🚧 |
| [Keyhole Display](#3-기타-진행중-작업) | 50% | P2 | 🚧 |

---

## 1. PassSchedule 상태머신 리팩토링

> **우선순위**: P0 (핵심) | **상태**: ✅ Phase A-D 완료 (C2-C3 보류)
> **진행률**: 15/17 (88%)

PassScheduleService.kt 상태 머신 버그 수정 및 코드 품질 개선

**완료 항목:**
| Phase | 내용 | 상태 |
|:-----:|------|:----:|
| A | Critical 버그 3개 (isAtStowPosition, detailId, resetFlags) | ✅ |
| B | High 버그 6개 (ExitAction, IDLE, validTransitions, Timeout) | ✅ |
| C1 | isShuttingDown → AtomicBoolean | ✅ |
| D | 코드 품질 4개 (Deprecated 삭제, 로그, 예외, 필드명) | ✅ |

**보류 항목:**
- C2-C3: 동시성 개선 (상세 분석 필요)
- T1-T30: 수동 테스트

**문서**: [Architecture_Refactoring/passschedule/](./Architecture_Refactoring/passschedule/)

---

## 2. Architecture Refactoring (아키텍처 리팩토링)

> **우선순위**: P1 | **상태**: 진행 중

위성 추적 시스템(Ephemeris, PassSchedule) 중심의 전체 아키텍처 리팩토링

**핵심 목표**:
- BE: SatelliteTrackingEngine 추출 (코드 중복 40% → 10%)
- FE: trackingStateStore 통합, 차트 컴포넌트 분리
- 실시간 성능 최적화 (블로킹 코드 제거)

**문서**: [Architecture_Refactoring/](./Architecture_Refactoring/)

---

## 3. 기타 진행중 작업

| 작업 | 상태 | 비고 |
|------|:----:|------|
| [Keyhole_Display_Enhancement](./PassSchedule_Keyhole_Display_Enhancement/) | 🚧 | Keyhole 표시 개선 |
| [Data_Structure_Refactoring](./PassSchedule_Data_Structure_Refactoring/) | ⏸️ | MST/DTL 데이터 구조 재설계 |
| [Chart_Optimization](./PassSchedule_Chart_Optimization_plan.md) | ⏸️ | 차트 분리 (Architecture와 중복) |

---

## 분류 기준

| 분류 | 설명 | 예시 |
|------|------|------|
| **Architecture** | 코드 구조, 패턴, 성능 개선 | Architecture_Refactoring |
| **Feature** | 새로운 기능 추가 | Keyhole_Display_Enhancement |
| **Documentation** | 문서화 작업 | PassSchedule_Workflow |
| **Bugfix** | 버그 수정 | → `docs/work/active/` 이동 |

---

## 작업 완료 시

1. 해당 폴더를 `docs/work/archive/`로 이동
2. `archive/` 하위에 결과 요약 문서 작성
3. 이 README에서 해당 항목 제거
