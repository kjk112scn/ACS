# Database Integration Progress

> **Started**: -
> **Status**: 대기 (설계 완료)

---

## Phase Overview

| Phase | Description | Status | Notes |
|-------|-------------|:------:|-------|
| Phase 1 | 인프라 설정 | - | PostgreSQL + TimescaleDB |
| Phase 2 | Entity/Repository | - | 4개 테이블 |
| Phase 3 | Service 연동 | - | 저장 로직 |
| Phase 4 | Export/UI | - | CSV, Settings |

---

## Phase 1: 인프라 설정

- [ ] PostgreSQL 16 설치
- [ ] TimescaleDB 확장 설치
- [ ] 데이터베이스 생성 (`acs`)
- [ ] 사용자 생성 (`acs_user`)
- [ ] 테이블 생성 (sql/schema.sql 실행)
- [ ] R2DBC 의존성 추가 (build.gradle.kts)
- [ ] application.yml DB 설정 추가

---

## Phase 2: Entity/Repository

- [ ] TrackingSessionEntity.kt
- [ ] TrackingTrajectoryEntity.kt
- [ ] TrackingResultEntity.kt
- [ ] IcdStatusEntity.kt
- [ ] TrackingSessionRepository.kt
- [ ] TrackingTrajectoryRepository.kt
- [ ] TrackingResultRepository.kt
- [ ] IcdStatusRepository.kt
- [ ] 빌드 테스트

---

## Phase 3: Service 연동

- [ ] TrackingDataLoader.kt (초기 로딩)
- [ ] EphemerisService DB 저장 연동
- [ ] PassScheduleService DB 저장 연동
- [ ] UdpFwICDService ICD 배치 저장
- [ ] 통합 테스트

---

## Phase 4: Export/UI

- [ ] TrackingExportService.kt (CSV 내보내기)
- [ ] 과거 이력 조회 API
- [ ] Settings 모달에 보관 기간 설정 추가
- [ ] FE 이력 조회 UI (선택적)

---

## Execution Log

| Date | Task | Result | Notes |
|------|------|--------|-------|
| 2026-01-17 | 설계 문서 생성 | Done | DESIGN.md, schema.sql |

---

**Last Updated**: 2026-01-17
