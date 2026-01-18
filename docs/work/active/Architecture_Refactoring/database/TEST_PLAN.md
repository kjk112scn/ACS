# DB Integration Test Plan

## 개요

Phase 6 DB 연동 작업에 대한 테스트 계획입니다.

**테스트 대상:**
1. TLE Cache → DB 연동 (Ephemeris, PassSchedule)
2. Schedule Data → DB 연동 (tracking_session, tracking_trajectory)
3. Tracking Result → DB 연동 (tracking_result - 배치 저장)
4. Hardware Error → DB 연동 (hardware_error_log)

---

## 사전 조건

### 1. PostgreSQL + TimescaleDB 설정

```bash
# Docker로 TimescaleDB 실행 (개발용)
docker run -d --name timescaledb \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=acs \
  timescale/timescaledb:latest-pg16

# 또는 기존 PostgreSQL에 TimescaleDB 확장 설치
```

### 2. 스키마 생성

```sql
-- 1. TimescaleDB 확장 활성화
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- 2. 테이블 생성 (docs/database/ 참조)
-- tle_cache, tracking_session, tracking_trajectory, tracking_result, hardware_error_log
```

### 3. application.yml 설정

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/acs
    username: postgres
    password: postgres
```

---

## 테스트 순서

### Phase 1: DB 연결 확인

**목적:** DB 연결 및 Repository Bean 생성 확인

```bash
# Backend 서버 시작
cd backend && ./gradlew bootRun
```

**확인 사항:**
- [ ] 서버 정상 기동
- [ ] `TleCacheRepository` Bean 생성 로그 확인
- [ ] `TrackingSessionRepository` Bean 생성 로그 확인
- [ ] `TrackingTrajectoryRepository` Bean 생성 로그 확인
- [ ] `TrackingResultRepository` Bean 생성 로그 확인
- [ ] `HardwareErrorLogRepository` Bean 생성 로그 확인

**예상 로그:**
```
🚀 EphemerisTLECache 초기화 완료
🚀 PassScheduleTLECache 초기화 완료: 0개 위성 로드
🚀 HardwareErrorLogService 초기화 완료: 0개 에러 로드
```

---

### Phase 2: TLE Cache → DB 연동 테스트

#### 2.1 Ephemeris TLE 테스트

**테스트 절차:**
1. Ephemeris 페이지 접속
2. TLE 입력 (NORAD ID 또는 TLE 직접 입력)
3. DB 확인

**API 테스트:**
```bash
# TLE 입력 (예: ISS)
curl -X POST http://localhost:8080/api/ephemeris/tle \
  -H "Content-Type: application/json" \
  -d '{
    "satelliteId": "25544",
    "tleLine1": "1 25544U 98067A   21275.52265625  .00001829  00000+0  42034-4 0  9996",
    "tleLine2": "2 25544  51.6445 138.8260 0003031 290.0989  96.7420 15.48816833305825"
  }'
```

**DB 확인:**
```sql
-- tle_cache 테이블 확인
SELECT * FROM tle_cache WHERE mode = 'EPHEMERIS' ORDER BY created_at DESC LIMIT 5;

-- 활성 TLE 확인
SELECT * FROM tle_cache WHERE mode = 'EPHEMERIS' AND is_active = TRUE;
```

**검증 항목:**
- [ ] TLE 저장 확인 (tle_cache)
- [ ] is_active = TRUE 확인
- [ ] 새 TLE 입력 시 이전 TLE가 is_active = FALSE로 변경 확인

#### 2.2 PassSchedule TLE 테스트

**테스트 절차:**
1. PassSchedule 페이지 접속
2. 위성 추가 (여러 개)
3. DB 확인

**API 테스트:**
```bash
# 위성 추가
curl -X POST http://localhost:8080/api/pass-schedule/satellites \
  -H "Content-Type: application/json" \
  -d '{
    "satelliteId": "25544",
    "satelliteName": "ISS",
    "tleLine1": "...",
    "tleLine2": "..."
  }'
```

**DB 확인:**
```sql
-- PassSchedule TLE 확인
SELECT * FROM tle_cache WHERE mode = 'PASS_SCHEDULE' ORDER BY created_at DESC;

-- 활성 위성 목록
SELECT satellite_id, satellite_name, is_active, created_at
FROM tle_cache
WHERE mode = 'PASS_SCHEDULE' AND is_active = TRUE;
```

**검증 항목:**
- [ ] 여러 위성 TLE 저장 확인
- [ ] 모든 위성 is_active = TRUE 확인
- [ ] 위성 제거 시 is_active = FALSE로 변경 확인

---

### Phase 3: Schedule Data → DB 연동 테스트

#### 3.1 Ephemeris Schedule 테스트

**테스트 절차:**
1. Ephemeris 페이지에서 TLE 입력 후 스케줄 계산
2. DB 확인

**DB 확인:**
```sql
-- tracking_session 확인
SELECT * FROM tracking_session WHERE mode = 'EPHEMERIS' ORDER BY created_at DESC LIMIT 5;

-- tracking_trajectory 확인 (특정 세션)
SELECT COUNT(*) FROM tracking_trajectory WHERE session_id = <SESSION_ID>;

-- trajectory 샘플 데이터
SELECT * FROM tracking_trajectory WHERE session_id = <SESSION_ID> ORDER BY time LIMIT 10;
```

**검증 항목:**
- [ ] tracking_session 레코드 생성 확인
- [ ] tracking_trajectory 레코드 생성 확인 (DTL 데이터)
- [ ] 시간, 각도 데이터 정확성 확인

#### 3.2 PassSchedule Schedule 테스트

**테스트 절차:**
1. PassSchedule 페이지에서 위성 추가 후 스케줄 계산
2. DB 확인

**DB 확인:**
```sql
-- PassSchedule 세션 확인
SELECT * FROM tracking_session WHERE mode = 'PASS_SCHEDULE' ORDER BY created_at DESC LIMIT 10;

-- 특정 위성의 trajectory
SELECT ts.satellite_id, ts.pass_number, COUNT(tt.time) as point_count
FROM tracking_session ts
JOIN tracking_trajectory tt ON ts.id = tt.session_id
WHERE ts.mode = 'PASS_SCHEDULE'
GROUP BY ts.id, ts.satellite_id, ts.pass_number;
```

**검증 항목:**
- [ ] 위성별 세션 생성 확인
- [ ] 패스별 trajectory 데이터 확인
- [ ] 여러 패스 데이터 정확성 확인

---

### Phase 4: Tracking Result → DB 연동 테스트 (배치 저장)

**테스트 절차:**
1. Ephemeris 또는 PassSchedule 모드에서 추적 시작
2. 실제 추적 진행 (또는 시뮬레이션)
3. 추적 종료 후 DB 확인

**주의:** 이 테스트는 실제 하드웨어 연결 또는 시뮬레이션 환경 필요

**DB 확인:**
```sql
-- tracking_result 확인
SELECT * FROM tracking_result ORDER BY time DESC LIMIT 100;

-- 특정 세션의 결과
SELECT COUNT(*), MIN(time), MAX(time)
FROM tracking_result
WHERE session_id = <SESSION_ID>;

-- 배치 저장 간격 확인 (보통 1초 간격)
SELECT time, cmd_azimuth, cmd_elevation, act_azimuth, act_elevation
FROM tracking_result
WHERE session_id = <SESSION_ID>
ORDER BY time
LIMIT 50;
```

**검증 항목:**
- [ ] 추적 데이터 실시간 저장 확인
- [ ] 배치 저장 동작 확인 (로그: `💾 [DB] 추적 결과 저장`)
- [ ] 명령 값 / 실제 값 정확성 확인

---

### Phase 5: Hardware Error → DB 연동 테스트

**테스트 절차:**
1. 테스트 에러 생성 API 호출
2. DB 확인

**API 테스트:**
```bash
# 테스트 에러 생성
curl -X POST http://localhost:8080/api/hardware-error-log/test-error

# 테스트 해결 에러 생성
curl -X POST http://localhost:8080/api/hardware-error-log/test-resolved-error
```

**DB 확인:**
```sql
-- hardware_error_log 확인
SELECT * FROM hardware_error_log ORDER BY timestamp DESC LIMIT 20;

-- 미해결 에러
SELECT * FROM hardware_error_log WHERE resolved = FALSE ORDER BY timestamp DESC;

-- 심각도별 통계
SELECT severity, COUNT(*)
FROM hardware_error_log
GROUP BY severity;
```

**검증 항목:**
- [ ] 에러 로그 저장 확인
- [ ] error_code, error_type, severity 정확성 확인
- [ ] 해결 상태 (resolved) 저장 확인

---

### Phase 6: 서버 재시작 테스트

**목적:** Write-through 캐시 + DB 복원 검증

**테스트 절차:**
1. 모든 Phase 테스트 후 데이터 확인
2. 서버 종료
3. 서버 재시작
4. 메모리 캐시에 DB 데이터 복원 확인

**확인 사항:**
```bash
# 서버 시작 로그 확인
grep -E "(TLECache|초기화|로드)" backend.log
```

**예상 로그:**
```
📥 [DB→캐시] Ephemeris TLE 로드: satelliteId=25544
🚀 EphemerisTLECache 초기화 완료
📥 [DB→캐시] PassSchedule TLE 로드: satelliteId=25544, name=ISS
🚀 PassScheduleTLECache 초기화 완료: 3개 위성 로드
🚀 HardwareErrorLogService 초기화 완료: 15개 에러 로드
```

**검증 항목:**
- [ ] Ephemeris TLE 복원 확인
- [ ] PassSchedule TLE 목록 복원 확인
- [ ] Hardware Error 로그 복원 확인
- [ ] 복원된 데이터로 정상 동작 확인

---

## Unit Test (JUnit)

### 테스트 파일 위치
```
backend/src/test/kotlin/com/gtlsystems/acs_api/
├── tracking/
│   ├── repository/
│   │   ├── TleCacheRepositoryTest.kt
│   │   ├── TrackingSessionRepositoryTest.kt
│   │   ├── TrackingTrajectoryRepositoryTest.kt
│   │   ├── TrackingResultRepositoryTest.kt
│   │   └── HardwareErrorLogRepositoryTest.kt
│   └── service/
│       └── TrackingDataServiceTest.kt
└── service/
    ├── mode/
    │   ├── ephemeris/
    │   │   └── EphemerisTLECacheTest.kt
    │   └── passSchedule/
    │       └── PassScheduleTLECacheTest.kt
    └── hardware/
        └── HardwareErrorLogServiceTest.kt
```

### 테스트 실행

```bash
# 전체 테스트
cd backend && ./gradlew test

# 특정 테스트만
./gradlew test --tests "*TleCacheRepositoryTest"
./gradlew test --tests "*HardwareErrorLogServiceTest"
```

---

## 트러블슈팅

### 1. DB 연결 실패

```
Error: Connection refused to localhost:5432
```

**해결:** PostgreSQL 서버 실행 확인, 포트/호스트 설정 확인

### 2. Repository Bean 생성 안됨

```
No qualifying bean of type 'TleCacheRepository'
```

**해결:** `@ConditionalOnBean(DatabaseClient::class)` 조건 확인, R2DBC 설정 확인

### 3. Hypertable 관련 에러

```
Error: relation "tracking_trajectory" does not exist as hypertable
```

**해결:** TimescaleDB 확장 설치 및 hypertable 변환 SQL 실행

### 4. 메모리 전용 모드

DB 연결 없이 실행 시 정상 동작 확인:
```
⚠️ TleCacheRepository가 없습니다. 메모리 전용 모드로 동작합니다.
```

---

## 체크리스트 요약

| 테스트 항목 | 상태 |
|------------|------|
| DB 연결 확인 | [ ] |
| Ephemeris TLE → DB | [ ] |
| PassSchedule TLE → DB | [ ] |
| Ephemeris Schedule → DB | [ ] |
| PassSchedule Schedule → DB | [ ] |
| Tracking Result → DB | [ ] |
| Hardware Error → DB | [ ] |
| 서버 재시작 복원 | [ ] |
| Unit Tests | [ ] |

---

**작성일:** 2026-01-18
**버전:** 1.0
