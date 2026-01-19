# Changelog

> ACS (Antenna Control System) 변경 이력
>
> 형식: [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)

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
