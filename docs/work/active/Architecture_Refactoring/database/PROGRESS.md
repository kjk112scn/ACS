# Database Integration Progress

> **Started**: 2026-01-17
> **Status**: Phase 6 완료

---

## 설치 가이드

### 1. PostgreSQL + TimescaleDB 설치

```powershell
# Docker 사용 (권장)
docker run -d `
  --name acs-timescaledb `
  -p 5432:5432 `
  -e POSTGRES_USER=acs_user `
  -e POSTGRES_PASSWORD=acs1234 `
  -e POSTGRES_DB=acs `
  -v acs_pgdata:/var/lib/postgresql/data `
  timescale/timescaledb:latest-pg16
```

### 2. 스키마 실행

```powershell
# schema.sql 복사 후 실행
docker cp "g:\Kyu\repo\ACS\docs\work\active\Database_Integration\sql\schema.sql" acs-timescaledb:/tmp/
docker exec -it acs-timescaledb psql -U acs_user -d acs -f /tmp/schema.sql
```

### 3. 테이블 확인

```powershell
docker exec -it acs-timescaledb psql -U acs_user -d acs -c "\dt"
```

### 4. 서버 실행

```powershell
cd backend && ./gradlew bootRun
```

---

## 접속 정보

| 항목 | 값 |
|------|-----|
| Host | localhost |
| Port | 5432 |
| Database | acs |
| User | acs_user |
| Password | acs1234 |

---

## Phase Overview

| Phase | Description | Status |
|:-----:|-------------|:------:|
| 1 | DB 설치 | ✅ 완료 |
| 2 | Entity/Repository 생성 | ✅ 완료 |
| 3 | Service 연동 | ✅ 완료 |
| 4 | 기본 테스트 | ✅ 완료 |
| 4.5 | Settings R2DBC 마이그레이션 | ✅ 완료 |
| 4.6 | Schema v2 (tle_cache, error_log) | ✅ 완료 |
| **6** | **서비스 연동 (Write-through)** | ✅ 완료 |

---

## 테이블 현황 (8개)

| 테이블 | 타입 | 용도 |
|--------|------|------|
| tracking_session | 일반 | 추적 세션 메타데이터 |
| tracking_trajectory | Hypertable | 이론 궤적 |
| tracking_result | Hypertable | 실측 결과 |
| icd_status | Hypertable | ICD 100ms 데이터 |
| settings | 일반 | 시스템 설정 |
| setting_history | 일반 | 설정 변경 이력 |
| tle_cache | 일반 | TLE 캐시 (Ephemeris/PassSchedule) |
| hardware_error_log | Hypertable | 하드웨어 에러 로그 |

---

## Phase 6 완료 내역 (서비스 연동)

### Write-through 패턴 적용

| 연동 대상 | 파일 | 저장 테이블 |
|----------|------|------------|
| Ephemeris TLE | `EphemerisTLECache.kt` | tle_cache |
| PassSchedule TLE | `PassScheduleTLECache.kt` | tle_cache |
| Ephemeris 스케줄 | `EphemerisDataRepository.kt` | tracking_session, tracking_trajectory |
| PassSchedule 스케줄 | `PassScheduleDataRepository.kt` | tracking_session, tracking_trajectory |
| 추적 결과 (배치) | `BatchStorageManager.kt` | tracking_result |
| 하드웨어 에러 | `HardwareErrorLogService.kt` | hardware_error_log |

### 서버 시작 시 자동 복원

- `@PostConstruct`로 DB → 메모리 캐시 로드
- TLE, 에러 로그 등 자동 복원

---

## 압축/보관 정책

| 테이블 | 압축 | 삭제 |
|--------|------|------|
| tracking_trajectory | 7일 후 | CASCADE (session 삭제 시) |
| tracking_result | 7일 후 | CASCADE (session 삭제 시) |
| icd_status | 7일 후 | 90일 (TimescaleDB) |
| hardware_error_log | 30일 후 | 365일 (TimescaleDB) |

---

## 테스트 체크리스트

- [ ] DB 연결 확인 (서버 시작 로그)
- [ ] TLE 저장/조회 테스트
- [ ] 스케줄 저장/조회 테스트
- [ ] 추적 결과 배치 저장 테스트
- [ ] 에러 로깅 테스트
- [ ] 서버 재시작 후 데이터 복원 확인

---

## R2DBC 설정 (application.yml)

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/acs
    username: acs_user
    password: acs1234
```

---

## 프로필별 동작

| 프로필 | DB | 용도 |
|--------|:--:|------|
| (기본) | ❌ | DB 없이 실행 |
| `use-db` | ✅ | DB 연동 실행 |

```powershell
# DB 없이 실행 (기본)
./gradlew bootRun

# DB 연동 실행
./gradlew bootRun --args='--spring.profiles.active=use-db'
```

---

**Last Updated**: 2026-01-18
