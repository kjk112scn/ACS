# PassSchedule Refactoring

> **Status**: ✅ Phase A-B 구현 완료
> **Last Updated**: 2026-01-20
> **Version**: 3.0

---

## 개요

PassSchedule 서비스의 상태 머신, 워크플로우, 데이터 구조를 종합적으로 개선하는 리팩토링 프로젝트입니다.

---

## 문서 구조

| 문서 | 설명 | 상태 |
|------|------|:----:|
| [STATE_MACHINE.md](./STATE_MACHINE.md) | 상태 머신 분석 및 버그 목록 (Master) | ✅ 완료 |
| [PLAN.md](./PLAN.md) | 실행 계획 (Phase A-D) | ✅ A-B 완료 |
| [IMPLEMENTATION_PROGRESS.md](./IMPLEMENTATION_PROGRESS.md) | 구현 진행 상황 | ✅ 완료 |
| [TEST_SCENARIOS.md](./TEST_SCENARIOS.md) | 테스트 시나리오 (30개) | ✅ 완료 |
| [WORKFLOW.md](./WORKFLOW.md) | BE-FE 워크플로우 문서 | ✅ 완료 |
| [DATA_STRUCTURE.md](./DATA_STRUCTURE.md) | 데이터 구조 개선 계획 | ⬜ 대기 |

---

## 현황 요약 (2026-01-20)

### 구현 완료 (Phase A-B)

| Phase | 작업 | 상태 |
|:-----:|------|:----:|
| A1 | isAtStowPosition() 함수 추가 | ✅ |
| A2 | sendStateToFrontend() detailId 전달 | ✅ |
| A3 | resetFlags() 큐 원본 업데이트 | ✅ |
| B1 | executeExitAction 구현 | ✅ |
| B2 | IDLE 상태 명시적 처리 | ✅ |
| B3 | reevaluateScheduleQueue 큐 업데이트 | ✅ |
| B4 | ERROR 전환 + validTransitions | ✅ |
| B5 | 상태별 타임아웃 (4분/2분/4분) | ✅ |

### 미완료 (Phase C-D)

| 우선순위 | 개수 | 상세 |
|:--------:|:----:|------|
| **P2** (Medium) | 3 | [STATE_MACHINE.md](./STATE_MACHINE.md) §2.3 |
| **P3** (Low) | 22+ | [STATE_MACHINE.md](./STATE_MACHINE.md) §3 |

### 코드 품질 이슈 (실제 코드 기준 2026-01-19)

| 항목 | 개수 | CLAUDE.md | 상세 |
|------|:----:|:---------:|------|
| catch(Exception) | 17 | ❌ 위반 | [STATE_MACHINE.md](./STATE_MACHINE.md) §3.1 |
| "No" 필드 (V1 잔재) | 3 | - | [STATE_MACHINE.md](./STATE_MACHINE.md) §3.2 |
| @Deprecated Dead Code | 1 | - | L2614 |
| isShuttingDown 동기화 | 1 | ⚠️ | AtomicBoolean 필요 |

---

## 상태 다이어그램 (V2.0)

```
START ─► IDLE ─┬─(>4분)─► STOWING ─► STOWED ─┐
               │                              │
               └─(≤4분)──────────────────────►┤
                                              ▼
                                        MOVING_TRAIN
                                              │
                                        (Train 도달)
                                              ▼
                                       TRAIN_STABILIZING
                                              │
                                         (3초 경과)
                                              ▼
                                       MOVING_TO_START
                                              │
                                        (Az/El 도달)
                                              ▼
                                            READY
                                              │
                                        (시작 시간)
                                              ▼
                                          TRACKING
                                              │
                                        (종료 시간)
                                              ▼
                                       POST_TRACKING
                                              │
            ┌─────────────────────────────────┼───────────────┐
            │                                 │               │
      (다음 >4분)                       (다음 ≤4분)      (다음 없음)
            ▼                                 ▼               ▼
         STOWING                        MOVING_TRAIN     COMPLETED
            │                                 │
            └─────────── (반복) ──────────────┘
```

---

## 관련 파일

### Backend
- `backend/.../service/mode/PassScheduleService.kt` (3,296줄)
- `backend/.../controller/mode/PassScheduleController.kt`

### Frontend
- `frontend/src/pages/mode/PassSchedulePage.vue`
- `frontend/src/pages/mode/passSchedule/components/ScheduleInfoPanel.vue`
- `frontend/src/stores/icd/icdStore.ts`
- `frontend/src/stores/passSchedule/usePassScheduleModeStore.ts`

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2025-11-28 | 1.0 | 데이터 구조 리팩토링 계획 수립 |
| 2026-01-08 | 1.1 | 상태 머신 분석 및 설계 |
| 2026-01-19 | 2.0 | 문서 통합, 전문가 검토 결과 반영 |
| 2026-01-19 | 2.2 | 2차 전문가 검토: P0 4개, P1 6개로 확장 |
| 2026-01-19 | 2.3 | 3차 협의: C4 제거, 타임아웃 4분/2분/4분 확정 |
| 2026-01-19 | 2.4 | 4차 전문가 검토: Float→Double, T8/T9 테스트 추가, 승인 완료 |
| 2026-01-20 | 3.0 | Phase A-B 구현 완료, 테스트 시나리오 30개 작성 |