# Changelog

> ACS (Antenna Control System) 변경 이력
>
> 형식: [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)

---

## [2026-01-23] - PassSchedule P6 동기화 (P7 Fix)

### Fixed
- PassScheduleDataRepository: @PostConstruct initFromDatabase() 추가 (서버 재시작 시 DB→메모리 로드)
- PassScheduleDataRepository: CreatedAt 기반 최신 등록 건 필터링 (9개 조회 함수)
- PassScheduleDataRepository: 이력 보존 로직 (덮어쓰기 → 누적 저장)

### Changed
- EphemerisDataRepository: P6-2 CSV 다운로드 시 최신 등록 건만 반환 수정

### Why
- Ephemeris에 적용된 P6 (initFromDatabase, CreatedAt 필터링, 이력 보존) 패턴이 PassSchedule에는 미적용
- 서버 재시작 시 PassSchedule 스케줄 목록이 0개로 표시되는 문제

### Details
- [Tracking_Schema_Redesign/FIX.md](docs/work/active/Tracking_Schema_Redesign/FIX.md)

---

## [2026-01-22] - V006 MstId/DetailId 버그픽스

### Fixed
- EphemerisService: currentTrackingDetailId 변수 추가 (MstId만으로는 패스 구분 불가)
- EphemerisDataRepository: 서버 재시작 시 DB→메모리 로딩 추가 (@PostConstruct)
- FE formatDuration: 숫자(초)/ISO 8601 문자열 모두 처리 (5개 파일)

### Changed
- EphemerisController: setCurrentTrackingPassId API에 detailId 파라미터 추가
- getTrackingPassMst(): (MstId, DetailId) 복합키로 검색

### Why
- V006 스키마 변경 후 동일 위성의 여러 패스가 같은 MstId 공유
- DetailId 없이는 패스 구분 불가 → 스케줄 선택 시 전체 선택되는 버그
- 서버 재시작 후 스케줄 목록 0개 반환 문제

### Details
- [Tracking_Schema_Redesign](docs/work/active/Tracking_Schema_Redesign/README.md)

---

## [2026-01-22] - 문서 관리 체계 표준화

### Added
- 문서 관리 가이드 신규 (docs/guides/documentation-management.md)
- 작업 폴더 템플릿 4개 (README, PROGRESS, DESIGN, FIX)
- docs/work/archive/ 폴더 생성

### Changed
- CLAUDE.md에 문서 관리 규칙 4줄 추가
- /done 스킬에 문서 헬스체크 단계 추가 (아카이브 후보 탐색)
- DEEP_REVIEW_V007.md → DESIGN.md 이름 표준화

### Why
- 작업 폴더 구조 표준화로 일관된 문서 관리
- 완료된 작업 자동 감지 + 아카이브 제안
- 세션 간 컨텍스트 유지 강화

### Details
- [documentation-management.md](docs/guides/documentation-management.md)

---

## [2026-01-21] - Tracking Schema Redesign (V006)

### Changed
- tracking_session: UNIQUE 제약 변경 (data_type → detail_id) + TLE 연동 (FK + 스냅샷)
- tracking_trajectory: +4컬럼 (train_rate, satellite_range/altitude/velocity)
- tracking_result: -17컬럼 (이론치 제거), +15컬럼 (ICD 추적 데이터 + 정밀 추적 메타데이터)

### Why
- 1 Pass = 7 Sessions 문제 해결 → 1 Pass = 1 Session으로 정규화
- 이론치는 tracking_trajectory에서 JOIN 조회 (중복 제거)
- TLE 데이터 추적성 확보 (FK로 히스토리, 스냅샷으로 계산 시점 기록)

### Details
- [Tracking_Schema_Redesign](docs/work/active/Tracking_Schema_Redesign/README.md)
- [V006 Migration](backend/src/main/resources/db/migration/V006__Schema_redesign_tracking_tables.sql)

---

## [2026-01-21] - Admin Panel 분리 + UX 개선

### Added
- Admin Panel 분리 (Settings에서 하드웨어 제어 기능 분리)
- AdminPanel.vue 신규 컴포넌트 (경고 배너 포함)
- 헤더에 Admin 버튼 추가 (Settings 버튼 우측)

### Fixed
- useDialog.ts Quasar API 수정 (ok/cancel handler → onOk/onCancel 체이닝)
- quasar.config.ts Dialog 플러그인 등록

### Changed
- 시간대 선택 UX 개선 (behavior="menu", clearable 추가)
- 시간대 목록 50개 제한 제거 (전체 428개 표시)

### Why
- 일반 설정(다크모드, 언어)과 위험 하드웨어 제어를 분리하여 안전성 향상
- Quasar Dialog API 비호환으로 확인 다이얼로그 Promise 미resolve 문제

### Details
- [Admin_Panel_Separation](docs/work/active/Admin_Panel_Separation/README.md)

---

## [2026-01-20] - DB 데이터 품질 개선

### Fixed
- tracking_session 테이블 빈 컬럼 문제 해결 (키 이름 불일치)
- HW 에러 로깅에 컨텍스트 정보 추가 (tracking_mode, correlation_id, raw_data)

### Added
- V003 DB 코멘트 마이그레이션 (테이블/컬럼 설명)
- FlywayConfig 컴포넌트 분리

### Why
- MST 데이터 키 이름(SatelliteID)과 Repository 매핑(SatelliteId) 불일치로 null 저장됨
- HW 에러 발생 시 어떤 모드에서 발생했는지 추적 불가

### Details
- [Tracking_Session_Data_Enrichment](docs/work/active/Tracking_Session_Data_Enrichment/README.md)
- [HW_Error_System_Integration](docs/work/active/HW_Error_System_Integration/README.md)

---

## [2026-01-20] - DB 스키마 재설계 + ICD 저장 로직

### Changed
- Flyway 마이그레이션 완전 재구성 (8개 테이블)
- V001: 모든 테이블 CREATE + TimescaleDB 설정
- V002: 기본 설정값 63개 INSERT
- IcdStatusStorageService 추가 (UDP → DB 배치 저장)

### Why
- 기존 마이그레이션 누락 (8개 중 2개만 존재) → 새 PC 설치 시 실패
- icd_status 저장 로직 미연결 → DB 저장 안 됨

### Details
- [DB_Schema_Redesign](docs/work/active/DB_Schema_Redesign/README.md)

---

## [2026-01-20] - DB 테스트 및 i18n 진행

### Changed
- DB 테스트 완료 (Phase 1-2) - TimescaleDB 연결 성공
- i18n 하드코딩 정리 (LoginPage, StandbyPage, SunTrackPage)

### Why
- with-db 프로필 실제 연동 검증 필요
- 언어 전환 시 영어 표시 보장

### Details
- [일일 로그](docs/logs/2026-01-20.md)

---

## [2026-01-20] - 작업 추적 시스템 구축

### Added
- `docs/work/CURRENT_STATUS.md` - 세션 간 작업 상태 추적
- `docs/work/health-checks/` - 빌드/품질 점검 결과 저장
- `/done` 스킬 v2.1.0 - Step 3: CURRENT_STATUS.md 자동 업데이트

### Why
- 새 세션 시작 시 컨텍스트 빠른 복원 ("CURRENT_STATUS.md 읽고 이어서 해줘")
- 프로젝트 건강 상태 이력 관리
- 작업 완료 시 상태 자동 동기화

### Details
- [CURRENT_STATUS.md](docs/work/CURRENT_STATUS.md)
- [health-checks/README.md](docs/work/health-checks/README.md)

---

## [2026-01-20] - 통합 워크플로우 v2.0

### Changed
- 스킬 시스템 v2.0 업그레이드 (refactor, bugfix, feature, plan)
- 공통 워크플로우: 조사 → 검토 → 계획 → 승인 → 실행 → 완료
- Phase A-D 분류 표준화 (P0 Critical → P3 Low)
- 진행 상황 추적 표준화 (PROGRESS.md)

### Added
- [UNIFIED_WORKFLOW.md](.claude/skills/UNIFIED_WORKFLOW.md) - 통합 워크플로우 마스터 문서
- 작업 현황 Dashboard ([docs/work/active/README.md](docs/work/active/README.md))

### Why
- PassSchedule 리팩토링 검증된 워크플로우를 모든 스킬에 적용
- 자동화된 분석 + 전문가 검토 + 문서 기반 추적

### Details
- [UNIFIED_WORKFLOW.md](.claude/skills/UNIFIED_WORKFLOW.md)

---

## [2026-01-20] - PassSchedule Phase C-D 완료

### Changed
- **Phase C (Medium)**: 1개 완료
  - C1: `isShuttingDown` → `AtomicBoolean` (스레드 안전성)

- **Phase D (Low)**: 4개 완료
  - D1: `@Deprecated handleTrackingPreparation()` 삭제
  - D2: 10초 상태 로깅 INFO → DEBUG
  - D3: `catch(Exception)` → `catch(RuntimeException)` (13개)
  - D4: `["No"]` → `["MstId"]` (6개)

### Why
- 동시성 버그 예방 (AtomicBoolean)
- CLAUDE.md 코딩 규칙 준수
- V1 잔재 필드명 정리

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
