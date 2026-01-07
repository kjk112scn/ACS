# Active Features (진행 중 작업)

> **최종 수정**: 2026-01-07

현재 진행 중인 기능 개발 및 리팩토링 작업 목록입니다.

---

## 1. Architecture Refactoring (아키텍처 리팩토링)

> **우선순위**: P0 (핵심) | **상태**: 진행 중

위성 추적 시스템(Ephemeris, PassSchedule) 중심의 전체 아키텍처 리팩토링

**핵심 목표**:
- BE: SatelliteTrackingEngine 추출 (코드 중복 40% → 10%)
- FE: trackingStateStore 통합, 차트 컴포넌트 분리
- 실시간 성능 최적화 (블로킹 코드 제거)

**문서**: [Architecture_Refactoring/](./Architecture_Refactoring/)

---

## 2. PassSchedule 개선 (추후 정리 예정)

> **상태**: 분류 대기 | Architecture_Refactoring 완료 후 정리

PassSchedule 관련 개별 작업들. 아키텍처 리팩토링과 일부 중복되므로 추후 통합/정리 필요.

| 작업 | 상태 | 비고 |
|------|------|------|
| [PassSchedule_Chart_Optimization](./PassSchedule_Chart_Optimization_plan.md) | 보류 | Architecture_Refactoring 차트 분리와 중복 |
| [PassSchedule_Data_Structure_Refactoring](./PassSchedule_Data_Structure_Refactoring/) | 진행중 | MST/DTL 데이터 구조 재설계 |
| [PassSchedule_Keyhole_Display_Enhancement](./PassSchedule_Keyhole_Display_Enhancement/) | 진행중 | Keyhole 표시 개선 (독립 기능) |
| [PassSchedule_Workflow](./PassSchedule_Workflow/) | 진행중 | 워크플로우 문서화 |

---

## 분류 기준

| 분류 | 설명 | 예시 |
|------|------|------|
| **Architecture** | 코드 구조, 패턴, 성능 개선 | Architecture_Refactoring |
| **Feature** | 새로운 기능 추가 | Keyhole_Display_Enhancement |
| **Documentation** | 문서화 작업 | PassSchedule_Workflow |
| **Bugfix** | 버그 수정 | → `docs/bugfixes/active/` 이동 |

---

## 작업 완료 시

1. 해당 폴더를 `docs/features/completed/`로 이동
2. `completed/` 하위에 결과 요약 문서 작성
3. 이 README에서 해당 항목 제거
