# Database Integration Progress

> **Started**: 2026-01-17
> **Status**: 진행 중

---

## 환경 분리 전략

> **핵심**: 집/회사 모두 DB 설치, 환경별 포트 분리

| 환경 | DB 방식 | 포트 | 테스트 범위 |
|:----:|--------|:----:|------------|
| 🏠 집 | Docker | **5433** | Mock 데이터로 기본 테스트 |
| 🏢 회사 | Native | 5432 | 실제 ICD 데이터 통합 테스트 |

### 장점

1. 집에서 코드 작성 후 **바로 테스트 가능**
2. 회사 가기 전에 **버그 미리 발견**
3. 개발 사이클이 빨라짐

### 집에서 테스트 가능한 것

| 항목 | 가능 | 비고 |
|------|:----:|------|
| DB 연결 | ✅ | Docker |
| Entity 저장/조회 | ✅ | Mock 데이터 |
| Repository CRUD | ✅ | 단위 테스트 |
| Service 로직 | ✅ | Mock 데이터 |
| ICD 100ms 저장 | ⚠️ | Mock 시뮬레이션 |
| 실제 추적 데이터 | ❌ | 회사에서만 |

---

## Phase Overview

| Phase | Description | Status | 환경 |
|:-----:|-------------|:------:|:----:|
| 1 | Docker DB 설치 | ✅ 완료 | 🏠 집 |
| 2 | Entity/Repository (Tracking) | ✅ 완료 | 🏠 집 |
| 3 | Service 연동 | ✅ 완료 | 🏠 집 |
| 4 | 기본 테스트 | ✅ 완료 | 🏠 집 |
| **4.5** | **Settings R2DBC 마이그레이션** | ✅ 완료 | 🏠 집 |
| 5 | Native DB 설치 | - | 🏢 회사 |
| 6 | 통합 테스트 | - | 🏢 회사 |

---

## 🏠 Phase 1: Docker DB 설치 (집)

### Step 1: Docker 컨테이너 생성

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

### Step 2: 설치 확인

```powershell
docker ps
docker exec -it acs-timescaledb psql -U acs_user -d acs -c "SELECT version();"
```

### Step 3: 테이블 생성

```powershell
docker cp "g:\Kyu\repo\ACS\docs\work\active\Database_Integration\sql\schema.sql" acs-timescaledb:/tmp/
docker exec -it acs-timescaledb psql -U acs_user -d acs -f /tmp/schema.sql
```

### Step 4: 테이블 확인

```powershell
docker exec -it acs-timescaledb psql -U acs_user -d acs -c "\dt"
```

---

## 🏠 Phase 2: Entity/Repository (집)

### R2DBC 의존성 추가 (build.gradle.kts)

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
    runtimeOnly("org.postgresql:postgresql:42.7.1")
}
```

### application.yml 환경 분리

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILE:home}

---
spring:
  config:
    activate:
      on-profile: home
  r2dbc:
    url: r2dbc:postgresql://localhost:5433/acs
    username: acs_user
    password: acs1234

---
spring:
  config:
    activate:
      on-profile: office
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/acs
    username: acs_user
    password: acs1234
```

### Entity/Repository 파일

```
backend/src/main/kotlin/.../
├── entity/
│   ├── TrackingSessionEntity.kt
│   ├── TrackingTrajectoryEntity.kt
│   ├── TrackingResultEntity.kt
│   └── IcdStatusEntity.kt
└── repository/
    ├── TrackingSessionRepository.kt
    ├── TrackingTrajectoryRepository.kt
    ├── TrackingResultRepository.kt
    └── IcdStatusRepository.kt
```

---

## 🏢 Phase 5: Native DB 설치 (회사)

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

## 체크리스트

### 🏠 Phase 1: Docker DB 설치 (집) ✅ 완료

- [x] Docker 컨테이너 생성 (acs-timescaledb)
- [x] PostgreSQL 버전 확인 (16.11)
- [x] schema.sql 복사 및 실행
- [x] 테이블 4개 생성 확인
- [x] Hypertable 변환 확인 (3개: trajectory, result, icd_status)
- [x] 압축 정책 확인 (7일 후)
- [x] 보관 정책 확인 (icd_status: 90일)

### 🏠 Phase 2: Entity/Repository (집) ✅ 완료

- [x] R2DBC 의존성 추가 (build.gradle.kts)
- [x] application-home.properties (포트 5433)
- [x] application-office.properties (포트 5432)
- [x] TrackingSessionEntity.kt
- [x] TrackingTrajectoryEntity.kt
- [x] TrackingResultEntity.kt
- [x] IcdStatusEntity.kt
- [x] TrackingSessionRepository.kt (ReactiveCrudRepository)
- [x] TrackingTrajectoryRepository.kt (DatabaseClient)
- [x] TrackingResultRepository.kt (DatabaseClient)
- [x] IcdStatusRepository.kt (DatabaseClient)
- [x] 빌드 확인 (`./gradlew build -x test`)

### 🏠 Phase 3: Service 연동 (집) ✅ 완료

- [x] R2dbcConfig.kt (설정 클래스)
- [x] TrackingDataService.kt (저장/조회 로직)
- [x] 빌드 확인

> **Phase 6에서 진행**: EphemerisService, PassScheduleService, IcdBatchSaveService 연동

### 🏠 Phase 4: 기본 테스트 (집) ✅ 완료

- [x] Docker 컨테이너 실행 확인
- [x] tracking_session INSERT/SELECT 테스트
- [x] tracking_trajectory INSERT/SELECT 테스트 (Hypertable)
- [x] icd_status INSERT/SELECT 테스트 (Hypertable)

> **서버 시작 테스트**: JPA/R2DBC 혼합 설정 필요 → Phase 6에서 진행

### 🏢 Phase 5: Native DB 설치 (회사)

- [ ] PostgreSQL 16 설치 확인
- [ ] TimescaleDB 확장 설치
- [ ] 데이터베이스/사용자 생성
- [ ] schema.sql 실행
- [ ] 테이블 4개 확인
- [ ] Hypertable 3개 확인

#### Phase 5 테스트 명령어

```powershell
# 1. TimescaleDB 확장 확인
psql -U postgres -c "SELECT * FROM pg_extension WHERE extname = 'timescaledb';"

# 2. DB/사용자 생성 (없으면)
psql -U postgres -c "CREATE USER acs_user WITH PASSWORD 'acs1234';"
psql -U postgres -c "CREATE DATABASE acs OWNER acs_user;"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE acs TO acs_user;"

# 3. schema.sql 실행
psql -U acs_user -d acs -f "g:\Kyu\repo\ACS\docs\work\active\Database_Integration\sql\schema.sql"

# 4. 테이블 확인
psql -U acs_user -d acs -c "\dt"

# 5. Hypertable 확인
psql -U acs_user -d acs -c "SELECT hypertable_name, compression_enabled FROM timescaledb_information.hypertables;"
```

### 🏢 Phase 6: 통합 테스트 (회사)

- [ ] 서버 시작 (office 프로필)
- [ ] DB 연결 로그 확인
- [ ] Mock 데이터 INSERT 테스트
- [ ] 실제 추적 데이터 저장 연동 (선택)

#### Phase 6 테스트 명령어

```powershell
# 1. 서버 시작 (office 프로필)
cd backend
./gradlew bootRun --args='--spring.profiles.active=office'

# 2. DB 연결 확인 (로그에서)
# "R2DBC 연결 성공" 또는 "ConnectionFactory" 로그 확인

# 3. Mock 데이터 테스트 (psql)
psql -U acs_user -d acs -c "INSERT INTO tracking_session (mst_id, detail_id, satellite_id, tracking_mode, data_type, start_time, end_time) VALUES (999, 1, 'TEST', 'EPHEMERIS', 'original', NOW(), NOW() + INTERVAL '10 min') RETURNING id;"

psql -U acs_user -d acs -c "INSERT INTO icd_status (timestamp, azimuth_angle, elevation_angle) VALUES (NOW(), 45.0, 30.0);"

# 4. 데이터 확인
psql -U acs_user -d acs -c "SELECT COUNT(*) FROM tracking_session;"
psql -U acs_user -d acs -c "SELECT COUNT(*) FROM icd_status;"

# 5. 테스트 데이터 정리
psql -U acs_user -d acs -c "DELETE FROM tracking_session WHERE satellite_id = 'TEST';"
```

### 선택적: UI

- [ ] Settings 모달에 보관 기간 설정
- [ ] FE 이력 조회 UI

---

## 🏢 회사에서 할 전체 테스트 목록

> **Architecture Refactoring 테스트 + DB Integration 테스트**

### 1. 리팩토링 검증 테스트 (TRACKER.md 참조)

#### P1-1 !! 연산자 제거 검증
- [ ] **SunTrack 모드**
  - [ ] 모드 시작/중지 정상 동작
  - [ ] Train 각도 초기화 및 이동
  - [ ] Offset 변경 시 실시간 반영
  - [ ] 안정화 단계 전환 (IDLE → INITIAL_TRAIN → STABILIZING → TRACKING)
- [ ] **PassSchedule 모드**
  - [ ] 스케줄 로드 정상 동작
  - [ ] 첫 스케줄 선택 및 상태 전환
  - [ ] 추적 시작/중지

#### CP3/CP4 (FE 리팩토링 검증)
- [ ] FE 재시작 후 deep watch 동작 확인
- [ ] icdStore shallowRef 변경 영향 확인
- [ ] 실시간 데이터 표시 정상 여부

#### Phase 5 BE 서비스 분리 검증
- [ ] EphemerisTLECache 동작 확인
- [ ] PassScheduleTLECache 동작 확인
- [ ] EphemerisDataRepository 로그 확인
- [ ] PassScheduleDataRepository 로그 확인

### 2. DB Integration 테스트

#### Phase 5: Native DB 설치
- [ ] PostgreSQL 16 + TimescaleDB 설치 확인
- [ ] 데이터베이스/사용자 생성
- [ ] schema.sql 실행
- [ ] 테이블 4개 / Hypertable 3개 확인

#### Phase 6: 서버 연동 테스트
- [ ] 서버 시작 (office 프로필)
- [ ] DB 연결 로그 확인
- [ ] Mock 데이터 INSERT 테스트

---

## 기술 결정 사항 (2026-01-17 검토)

### TimescaleDB 선택 이유

- PostgreSQL 확장으로 동작 (별도 DB 아님)
- 기존 PostgreSQL 문법 그대로 사용 가능
- 시계열 데이터 최적화 (자동 파티셔닝, 압축)
- 기존 AurenDB (포트 5432) 영향 없음

### Hypertable vs 일반 테이블

| 테이블 | 타입 | 이유 |
|--------|------|------|
| tracking_session | 일반 테이블 | FK 부모, 저용량 (~100건/일) |
| tracking_trajectory | **Hypertable** | 시계열, 대용량 (~10,000건/세션) |
| tracking_result | **Hypertable** | 시계열, 대용량 (~10,000건/세션) |
| icd_status | **Hypertable** | 시계열, 초대용량 (864,000건/일) |
| settings | 일반 테이블 | 시스템 설정 영속화 (~50건) |
| setting_history | 일반 테이블 | 설정 변경 감사 로그 |

> **Hypertable 제약**: PRIMARY KEY를 가진 테이블은 Hypertable 변환 불가
> → trajectory, result 테이블에서 BIGSERIAL PRIMARY KEY 제거

### 압축 정책

- **적용 대상**: Hypertable 3개 (trajectory, result, icd_status)
- **압축 시점**: 7일 후 자동 압축
- **장점**: 디스크 공간 최대 90% 절약
- **단점**: 압축된 청크는 INSERT/UPDATE 불가 (조회만 가능)

```sql
-- 적용된 압축 정책
ALTER TABLE tracking_trajectory SET (timescaledb.compress, timescaledb.compress_segmentby = 'session_id');
SELECT add_compression_policy('tracking_trajectory', INTERVAL '7 days');
```

### 보관(삭제) 정책

| 테이블 | 삭제 방식 | 보관 기간 | 비고 |
|--------|----------|----------|------|
| tracking_session | Spring Scheduler | **365일** | 부모 삭제 시 자식 CASCADE |
| tracking_trajectory | CASCADE | - | session 삭제 시 자동 삭제 |
| tracking_result | CASCADE | - | session 삭제 시 자동 삭제 |
| icd_status | TimescaleDB retention | **90일** | 자동 삭제 |

> **CASCADE**: 부모 테이블(tracking_session) 레코드 삭제 시 자식 테이블(trajectory, result) 레코드 자동 삭제

```sql
-- icd_status 보관 정책
SELECT add_retention_policy('icd_status', INTERVAL '90 days');
```

### 설정 변경 가능 여부

| 항목 | 변경 가능 | 방법 |
|------|:--------:|------|
| 압축 시점 (7일) | ✅ | `remove_compression_policy` → `add_compression_policy` |
| 보관 기간 (90일) | ✅ | `remove_retention_policy` → `add_retention_policy` |
| Hypertable 전환 | ❌ | 테이블 재생성 필요 |

---

## Execution Log

| Date | Task | Result | 환경 | Notes |
|------|------|--------|:----:|-------|
| 2026-01-17 | 설계 문서 생성 | Done | - | DESIGN.md, schema.sql |
| 2026-01-17 | 환경 분리 전략 수립 | Done | - | 집(Docker)/회사(Native) |
| 2026-01-17 | Docker TimescaleDB 설치 | Done | 🏠 집 | PostgreSQL 16.11, 포트 5433 |
| 2026-01-17 | schema.sql 실행 | Done | 🏠 집 | 4개 테이블, Hypertable 3개 |
| 2026-01-17 | 압축/보관 정책 적용 | Done | 🏠 집 | 압축 7일, icd_status 90일 |
| 2026-01-17 | R2DBC 의존성 추가 | Done | 🏠 집 | build.gradle.kts |
| 2026-01-17 | 프로필 설정 생성 | Done | 🏠 집 | home(5433), office(5432) |
| 2026-01-17 | Entity 4개 생성 | Done | 🏠 집 | tracking 패키지 |
| 2026-01-17 | Repository 4개 생성 | Done | 🏠 집 | 빌드 성공 확인 |
| 2026-01-17 | TrackingDataService 생성 | Done | 🏠 집 | R2dbcConfig 포함 |
| 2026-01-17 | DB INSERT/SELECT 테스트 | Done | 🏠 집 | 4개 테이블 모두 성공 |
| 2026-01-18 | Settings R2DBC 마이그레이션 | Done | 🏠 집 | settings, setting_history 테이블 추가 |
| 2026-01-18 | Settings Entity/Repository 전환 | Done | 🏠 집 | JPA → R2DBC, 빌드 성공 |

---

## 접속 정보

| 항목 | 🏠 집 (Docker) | 🏢 회사 (Native) |
|------|---------------|-----------------|
| Host | localhost | localhost |
| Port | **5433** | 5432 |
| Database | acs | acs |
| User | acs_user | acs_user |
| Password | acs1234 | acs1234 |
| Profile | `home` | `office` |

---

**Last Updated**: 2026-01-17 (Phase 1~4 완료, 집 작업 완료)
