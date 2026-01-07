# Architecture Refactoring (아키텍처 리팩토링)

> **버전**: 3.4.0 | **최종 수정**: 2026-01-07

## 개요

ACS 프로젝트의 위성 추적 시스템(Ephemeris, PassSchedule) 중심 아키텍처 리팩토링입니다.

**설계 원칙**: 소규모 팀 관리 가능, 오버엔지니어링 금지

## 목표

1. **코드 중복 제거**: EphemerisService + PassScheduleService 40% 중복 → 10% 이하
2. **상태 관리 통합**: BE SatelliteTrackingEngine + FE trackingStateStore
3. **실시간 성능**: 블로킹 코드 제거, 차트 최적화
4. **거대 파일 분해**: 5,000줄+ 파일 분리

## 작업 단계 (총 15일)

| Phase | 내용 | 기간 |
|-------|------|------|
| **Phase 0** | 사전 준비 (하드웨어 안전 테스트) | 1일 |
| **Phase 1** | 체계 수립 (폴더 구조, 컨벤션) | 2일 |
| **Phase 2** | BE 리팩토링 (SatelliteTrackingEngine 추출) | 5일 |
| **Phase 3** | FE 리팩토링 (trackingStateStore, 차트 분리) | 5일 |
| **Phase 4** | 테스트 작성 및 검증 | 3일 |

## 문서 구조

| 문서 | 역할 |
|------|------|
| **[Architecture_Refactoring_plan.md](./Architecture_Refactoring_plan.md)** | 메인 허브 |
| **[Expert_Analysis_Report.md](./Expert_Analysis_Report.md)** | 전문가 분석 보고서 (우선순위/권장사항) |
| **[RFC_SatelliteTrackingEngine.md](./RFC_SatelliteTrackingEngine.md)** | 핵심 리팩토링 상세 |
| **[RFC_Database_Strategy.md](./RFC_Database_Strategy.md)** | DB 저장 전략 (실시간 데이터) |
| **[RFC_Configuration_Management.md](./RFC_Configuration_Management.md)** | **설정 관리 개선 (FE-BE 동기화)** |
| **[RFC_UIUX_Consistency.md](./RFC_UIUX_Consistency.md)** | **UI/UX 일관성 개선** |
| [Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md) | BE 파일 목록/현황 |
| [Frontend_Refactoring_plan.md](./Frontend_Refactoring_plan.md) | FE 파일 목록/현황 |
| [RFC_Realtime_MultiUser_Optimization.md](./RFC_Realtime_MultiUser_Optimization.md) | 다중 사용자/WebSocket |
| [Security_Stability_plan.md](./Security_Stability_plan.md) | 보안/안정성 (선택적) |

## 상태

🔄 진행 중 (Draft)
