# Changelog

> ACS (Antenna Control System) 변경 이력
>
> 형식: [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)

---

## [2026-01-20] - PassSchedule Phase C-D 부분 완료

### Changed
- **Phase C (Medium)**: 1개 완료
  - C1: `isShuttingDown` → `AtomicBoolean` (스레드 안전성)

- **Phase D (Low)**: 2개 완료
  - D1: `@Deprecated handleTrackingPreparation()` 삭제
  - D2: 10초 상태 로깅 INFO → DEBUG

### Why
- 동시성 버그 예방 (AtomicBoolean)
- 불필요한 로그 노이즈 감소

### Details
- [passschedule/README.md](docs/work/active/Architecture_Refactoring/passschedule/README.md)

---

## [2026-01-20] - PassSchedule Phase A-B 구현 완료

### Fixed
- **Phase A (Critical)**: 3개 버그 수정
  - A1: STOWING→STOWED 위치 확인 (`isAtStowPosition()` 추가)
  - A2: FE 테이블 하이라이트 (`detailId` 전달)
  - A3: 다중 위성 추적 플래그 초기화 (큐 원본 업데이트)

- **Phase B (High)**: 6개 버그 수정
  - B1: `executeExitAction()` 구현
  - B2: IDLE 상태 명시적 처리
  - B3: Time Offset 변경 시 큐 업데이트
  - B4: 상태 전환 유효성 검사 (`validTransitions`)
  - B5: 상태별 타임아웃 (4분/2분/4분)

### Added
- 테스트 시나리오 30개 ([TEST_SCENARIOS.md](docs/work/active/Architecture_Refactoring/passschedule/TEST_SCENARIOS.md))

### Why
- 다중 위성 연속 추적 시 플래그 초기화 실패 버그 해결
- 상태 전환 안전성 확보 (데드락 방지)

### Details
- [passschedule/README.md](docs/work/active/Architecture_Refactoring/passschedule/README.md)

---

## [2026-01-19] - PassSchedule 리팩토링 계획 완료 + 스킬 수정

### Added
- **PassSchedule 상태머신 4차 전문가 검토 완료**
  - P0 Critical: 3개 (A1-A3)
  - P1 High: 6개 (B1-B5, H4는 A2에서 해결)
  - P2 Medium: 3개 (C1-C3)
  - 타임아웃 확정: 4분/2분/4분 (STOWING/MOVING_TRAIN/MOVING_TO_START)

### Changed
- **문서 구조 통합**: 흩어진 5개 폴더 → `passschedule/` 단일 폴더
- **스킬 name 수정**: 7개 스킬 폴더명↔name 불일치 해결

### Why
- 문서 분산으로 인한 관리 어려움 해소
- 스킬 명령어 (`/done`, `/plan` 등) 정상 동작 보장

### Details
- [passschedule/README.md](docs/work/active/Architecture_Refactoring/passschedule/README.md)

---

## [2026-01-19] - License Change

### Changed
- MIT License → GTL Proprietary License
- package.json에 license 필드 추가

### Why
- 기업 소유 비공개 소프트웨어에 MIT 라이센스 부적합
- 무단 복제/배포/사용 금지 명시 필요

---

## [2026-01-18] - Architecture Refactoring Phase 6

### Added
- **DB Integration (Write-through 패턴)**
  - TLE Cache → DB 연동 (Ephemeris, PassSchedule)
  - Schedule Data → DB 연동 (tracking_session, tracking_trajectory)
  - Tracking Result → DB 배치 저장
  - Hardware Error → DB 연동

### Changed
- 프로필 기반 DB 연동: `use-db` 프로필로 DB 모드 활성화
- 서버 재시작 시 @PostConstruct로 DB 데이터 자동 복원

### Why
- 서버 재시작 시 데이터 유실 방지
- Write-through로 메모리 성능 + DB 영속성 동시 확보

### Details
- [Database Integration](docs/work/active/Architecture_Refactoring/database/PROGRESS.md)

---

## [2026-01-18] - CSS !important Cleanup

### Changed
- CSS !important Phase 1-3 완료 (233개 제거)
- 테마 변수 기반 스타일링으로 전환

### Details
- [CSS Cleanup Plan](docs/work/active/Architecture_Refactoring/css/CSS_Important_Cleanup_Plan.md)

---

## [2026-01-17] - Architecture Refactoring Phase 1-6

### Added
- R2DBC 기반 추적 데이터 저장 레이어 구현
- TimescaleDB Hypertable 스키마 설계

### Changed
- Backend 서비스 계층 분리 (Phase 5)
- Frontend Composable 패턴 분리 (Phase 3)

### Details
- [Architecture Refactoring](docs/work/active/Architecture_Refactoring/README.md)

---

## [2026-01-16] - Frontend Refactoring

### Changed
- PassSchedulePage 컴포넌트 분리
- icdStore.ts Composable 패턴 분리
- OffsetControls 공용 모듈화

### Details
- [FE Refactoring Plan](docs/work/active/Architecture_Refactoring/frontend/FE_REFACTORING_PLAN.md)

---

## [2026-01-15] - Sprint 0 + Phase 1-2

### Added
- Ephemeris 이론치 CSV 브라우저 다운로드 기능

### Changed
- 아키텍처 리팩토링 Sprint 0 시작
- 컨텍스트 문서 통합

---

## 이전 이력

> 2026-01-14 이전 변경사항은 [docs/work/archive/](docs/work/archive/) 참조

---

**유지 규칙:**
1. 날짜별 역순 정렬 (최신이 위)
2. Added/Changed/Fixed/Why/Details 구조 유지
3. Details에 상세 문서 링크 필수
