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

### 환경별 설치 방법

| 환경 | 방식 | 포트 |
|------|------|------|
| 집 PC | Docker | 5433 |
| 회사 PC | Native | 5432 |

---

### 옵션 A: Docker 설치 (집 PC)

#### 1. 컨테이너 생성

```powershell
docker run -d `
  --name acs-timescaledb `
  -p 5433:5432 `
  -e POSTGRES_USER=acs_user `
  -e POSTGRES_PASSWORD=acs1234 `
  -e POSTGRES_DB=acs `
  -v acs_pgdata:/var/lib/postgresql/data `
  timescale/timescaledb:latest-pg16
```

#### 2. 확인

```powershell
docker ps
docker exec -it acs-timescaledb psql -U acs_user -d acs -c "SELECT version();"
```

#### 3. 테이블 생성

```powershell
docker cp "g:\Kyu\repo\ACS\docs\work\active\Database_Integration\sql\schema.sql" acs-timescaledb:/tmp/
docker exec -it acs-timescaledb psql -U acs_user -d acs -f /tmp/schema.sql
```

---

### 옵션 B: Native 설치 (회사 PC)

#### 1. PostgreSQL 16에 TimescaleDB 확장 추가

```sql
-- psql 접속 후
CREATE EXTENSION IF NOT EXISTS timescaledb;
```

> TimescaleDB 미설치 시: https://docs.timescale.com/self-hosted/latest/install/installation-windows/

#### 2. 데이터베이스/사용자 생성

```sql
CREATE USER acs_user WITH PASSWORD 'acs1234';
CREATE DATABASE acs OWNER acs_user;
GRANT ALL PRIVILEGES ON DATABASE acs TO acs_user;
```

#### 3. 테이블 생성

```powershell
psql -U acs_user -d acs -f "g:\Kyu\repo\ACS\docs\work\active\Database_Integration\sql\schema.sql"
```

---

### 접속 정보

| 항목 | Docker | Native |
|------|--------|--------|
| Host | localhost | localhost |
| Port | **5433** | 5432 |
| Database | acs | acs |
| User | acs_user | acs_user |
| Password | acs1234 | acs1234 |

---

### 체크리스트

- [ ] PostgreSQL 16 + TimescaleDB 설치
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
