# Architecture Refactoring (아키텍처 리팩토링) 메인 계획서

> **버전**: 3.4.0 | **최종 수정**: 2026-01-07

## 개요

| 항목 | 내용 |
|------|------|
| **목적** | 버그 없고, 체계적이고, 일관된 시스템 구축 |
| **핵심 대상** | 위성 추적 (Ephemeris, PassSchedule) + 실시간 성능 |
| **설계 원칙** | 소규모 팀 관리 가능, 오버엔지니어링 금지 |
| **예상 기간** | 15일 (Phase 0: 1일 + Phase 1-4: 14일) |

---

## 문서 구조

### 핵심 문서 (실행 순서대로)

| 순서 | 문서 | 역할 | 상태 |
|------|------|------|------|
| **0** | **[Expert_Analysis_Report.md](./Expert_Analysis_Report.md)** | 전문가 분석 보고서 (우선순위/권장사항) | Completed |
| **1** | **[RFC_SatelliteTrackingEngine.md](./RFC_SatelliteTrackingEngine.md)** | 핵심 리팩토링 계획 (BE+FE 통합) | Draft |

### 부록 문서 (상세 현황)

| 문서 | 역할 | 비고 |
|------|------|------|
| [Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md) | BE 상세 현황, 파일 목록, 세부 작업 | 자체 Phase 0-3 보유 |
| [Frontend_Refactoring_plan.md](./Frontend_Refactoring_plan.md) | FE 상세 현황, 파일 목록, 세부 작업 | 자체 Phase 0-2 보유 |

> **참고**: BE/FE 계획서의 Phase는 각 영역 내 세부 작업 순서이며, 위 메인 Phase 1-4와 다릅니다.

### 보조 문서 (선택적)

| 문서 | 역할 | 상태 |
|------|------|------|
| [RFC_Database_Strategy.md](./RFC_Database_Strategy.md) | DB 저장 전략 (실시간 추적 데이터) | Draft |
| **[RFC_Configuration_Management.md](./RFC_Configuration_Management.md)** | **설정 관리 개선 (FE-BE 동기화, 실시간 설정)** | **Draft** |
| **[RFC_UIUX_Consistency.md](./RFC_UIUX_Consistency.md)** | **UI/UX 일관성 개선 (컴포넌트 표준화)** | **Draft** |
| [RFC_Realtime_MultiUser_Optimization.md](./RFC_Realtime_MultiUser_Optimization.md) | 다중 사용자 제어권, WebSocket 최적화 | 검토 중 |
| [Security_Stability_plan.md](./Security_Stability_plan.md) | 보안/안정성 (외부 노출 시) | 선택적 |

---

## 실행 계획 (Phase)

> **기준 문서**: [RFC_SatelliteTrackingEngine.md](./RFC_SatelliteTrackingEngine.md)

### Phase 0: 사전 준비 (1일, Phase 1 전)

| 작업 | 상세 |
|------|------|
| Keyhole Detection 테스트 | 안테나 물리적 한계 검증 테스트 작성 |
| 좌표 변환 경계값 테스트 | Az/El/Train 변환 정확성 테스트 작성 |

> **목적**: 하드웨어 안전 최소 보장 (리팩토링 전)

### Phase 1: 체계 수립 (2일)

| 작업 | 상세 |
|------|------|
| 폴더 구조 정리 | BE: `engine/` 폴더 생성, FE: `stores/tracking/` 생성 |
| 코딩 컨벤션 | CLAUDE.md에 네이밍 규칙 추가 |

### Phase 2: 백엔드 리팩토링 (5일)

| 작업 | 파일 | 상세 |
|------|------|------|
| SatelliteTrackingEngine 추출 | `engine/SatelliteTrackingEngine.kt` | 상태 머신 + 축 제어 + ICD 통신 |
| EphemerisService 정리 | 기존 파일 수정 | 5,060줄 → ~2,000줄 |
| PassScheduleService 정리 | 기존 파일 수정 | 2,896줄 → ~1,500줄 |
| 블로킹 코드 제거 | Thread.sleep → Mono.delay | 4곳 수정 |

### Phase 3: 프론트엔드 리팩토링 (5일)

| 작업 | 파일 | 상세 |
|------|------|------|
| trackingStateStore 생성 | `stores/tracking/trackingStateStore.ts` | 추적 상태 통합 (~200줄) |
| PositionViewChart 분리 | `components/charts/PositionViewChart.vue` | 차트 로직 분리 (~500줄) |
| useChartUpdate 생성 | `composables/useChartUpdate.ts` | 차트 업데이트 최적화 |
| watch 정리 | 기존 파일 수정 | 디버깅용 삭제, 중복 통합 |

### Phase 0: 사전 준비 (1일, Phase 1 전)

> ⚠️ **하드웨어 안전 최소 보장**

| 작업 | 상세 | 목적 |
|------|------|------|
| Keyhole Detection 테스트 | 안테나 물리적 한계 검증 | 하드웨어 손상 방지 (Critical) |
| 좌표 변환 경계값 테스트 | Az/El/Train 변환 정확성 | 오동작 방지 (Critical) |

**참고**: 전체 테스트는 리팩토링 완료 후 작성 (Phase 4)

### Phase 4: 테스트 및 검증 (3일)

> **리팩토링 완료 후 깔끔한 코드 기반 테스트 작성**

| 작업 | 상세 |
|------|------|
| **Day 1: 테스트 작성** | SatelliteTrackingEngine 상태 전이 테스트 |
| | trackingStateStore 통합 테스트 |
| | 차트 업데이트 로직 테스트 |
| **Day 2: 성능 검증** | 30ms WebSocket 지연 측정 |
| | 차트 렌더링 성능 측정 |
| | Thread.sleep 제거 효과 검증 |
| **Day 3: 통합 테스트** | BE-FE 스케줄 전환 시나리오 |
| | 실기기 연동 테스트 (가능 시) |
| | 회귀 테스트 (기존 기능 정상 동작) |

---

## 현황 요약

### 잘 설계된 부분 (유지)

| 구성 요소 | 설명 |
|----------|------|
| ThreadManager | 우선순위별 스레드 풀 관리 |
| 실시간 파이프라인 | UDP(10ms) → BE → WebSocket(30ms) → FE |
| 차트 옵션 | animation: false, silent: true |
| 공유 데이터 버퍼 | AtomicReference |

### 개선 대상

| 영역 | 문제 | 해결 |
|------|------|------|
| BE 코드 중복 | EphemerisService + PassScheduleService 40% 중복 | SatelliteTrackingEngine 추출 |
| FE 상태 분리 | ephemerisTrackingState + passScheduleTrackingState 분리 | trackingStateStore 통합 |
| FE 거대 파일 | 페이지에 차트 로직 혼재 (4,000줄+) | PositionViewChart 분리 |
| BE 블로킹 | Thread.sleep 4곳 | Mono.delay 전환 |

---

## 거대 파일 목록

### 백엔드 핵심 (리팩토링 대상)

| 파일 | 현재 | 목표 |
|------|------|------|
| EphemerisService.kt | 5,060줄 | ~2,000줄 |
| PassScheduleService.kt | 2,896줄 | ~1,500줄 |
| SatelliteTrackingEngine.kt | - | ~800줄 (신규) |

### 프론트엔드 핵심 (리팩토링 대상)

| 파일 | 현재 | 목표 |
|------|------|------|
| PassSchedulePage.vue | 4,841줄 | ~3,500줄 |
| EphemerisDesignationPage.vue | 4,376줄 | ~3,000줄 |
| icdStore.ts | 2,971줄 | ~2,500줄 |
| trackingStateStore.ts | - | ~200줄 (신규) |
| PositionViewChart.vue | - | ~500줄 (신규) |

---

## 완료 기준

### 필수

- [ ] SatelliteTrackingEngine 추출 완료
- [ ] trackingStateStore 생성 완료
- [ ] 스케줄 전환 버그 해결
- [ ] 코드 중복률 40% → 10% 이하

### 권장

- [ ] PositionViewChart 컴포넌트 분리
- [ ] 블로킹 코드 제거
- [ ] 상태 전이 테스트 작성

---

## 롤백 계획

각 Phase는 독립적인 Git 브랜치에서 작업:
```
feature/phase1-structure-setup
feature/phase2-tracking-engine
feature/phase3-frontend-refactor
feature/phase4-testing
```

문제 발생 시 해당 브랜치만 롤백

---

**문서 버전**: 3.0.0
**작성일**: 2026-01-07
**기준 문서**: RFC_SatelliteTrackingEngine.md

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0.0 | 2024-12 | 최초 작성 |
| 2.0.0 | 2024-12 | 보안/안정성 계획 추가 |
| 2.1.0 | 2024-12 | Security를 선택적으로 변경 |
| 3.0.0 | 2026-01-07 | RFC_SatelliteTrackingEngine 기준 전면 재구성 |
| 3.1.0 | 2026-01-07 | BE/FE 문서 역할 분리 명시, Phase 혼란 방지 |
| 3.2.0 | 2026-01-07 | Expert_Analysis_Report 추가 |
| 3.3.0 | 2026-01-07 | RFC_Database_Strategy 추가, 문서 구조 완성 |
| **3.4.0** | **2026-01-07** | **RFC_Configuration_Management, RFC_UIUX_Consistency 추가** |
