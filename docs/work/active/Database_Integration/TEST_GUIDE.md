# 회사 테스트 가이드

> **작성일**: 2026-01-18
> **목적**: DB Integration Phase 5-6 + Architecture Refactoring + Settings 검증

---

## 테스트 순서 개요

```
[1. DB 설치] → [2. 서버 시작] → [3. 리팩토링 검증] → [4. Settings 검증] → [5. 통합 테스트]
     │              │                │                    │                   │
 TimescaleDB    office 프로필     SunTrack 등         Dead 설정          실제 추적
   설치           연결 확인         모드 테스트          정리               데이터
```

---

## Part 1: DB Integration 테스트

### 1.1 TimescaleDB 설치 확인

```powershell
# PostgreSQL 16 버전 확인
psql -U postgres -c "SELECT version();"

# TimescaleDB 확장 확인
psql -U postgres -c "SELECT * FROM pg_extension WHERE extname = 'timescaledb';"
```

**TimescaleDB 미설치 시:**
```powershell
# Windows: https://docs.timescale.com/self-hosted/latest/install/installation-windows/
# 또는 PostgreSQL Extension Manager 사용
```

### 1.2 데이터베이스/사용자 생성

```powershell
# 1. 사용자 생성
psql -U postgres -c "CREATE USER acs_user WITH PASSWORD 'acs1234';"

# 2. 데이터베이스 생성
psql -U postgres -c "CREATE DATABASE acs OWNER acs_user;"

# 3. 권한 부여
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE acs TO acs_user;"

# 4. TimescaleDB 확장 활성화 (acs DB에서)
psql -U acs_user -d acs -c "CREATE EXTENSION IF NOT EXISTS timescaledb;"
```

### 1.3 스키마 적용

```powershell
# schema.sql 실행
psql -U acs_user -d acs -f "g:\Kyu\repo\ACS\docs\work\active\Database_Integration\sql\schema.sql"
```

### 1.4 테이블 확인

```powershell
# 테이블 목록 (6개)
psql -U acs_user -d acs -c "\dt"

# 예상 결과:
#  tracking_session     - 추적 세션
#  tracking_trajectory  - 추적 궤적 (Hypertable)
#  tracking_result      - 추적 결과 (Hypertable)
#  icd_status           - ICD 상태 (Hypertable)
#  settings             - 시스템 설정
#  setting_history      - 설정 변경 이력
```

### 1.5 Hypertable 확인

```powershell
# Hypertable 목록 (3개)
psql -U acs_user -d acs -c "SELECT hypertable_name, compression_enabled FROM timescaledb_information.hypertables;"

# 예상 결과:
#  tracking_trajectory | t
#  tracking_result     | t
#  icd_status          | t
```

### 1.6 압축/보관 정책 확인

```powershell
# 압축 정책
psql -U acs_user -d acs -c "SELECT hypertable_name, compress_after FROM timescaledb_information.jobs WHERE proc_name = 'policy_compression';"

# 보관 정책 (icd_status만)
psql -U acs_user -d acs -c "SELECT hypertable_name, drop_after FROM timescaledb_information.jobs WHERE proc_name = 'policy_retention';"
```

### 1.7 Settings 테이블 확인

```powershell
# settings 테이블 구조
psql -U acs_user -d acs -c "\d settings"

# setting_history 테이블 구조
psql -U acs_user -d acs -c "\d setting_history"
```

### 1.8 체크리스트

- [ ] PostgreSQL 16 설치 확인
- [ ] TimescaleDB 확장 설치
- [ ] acs_user 사용자 생성
- [ ] acs 데이터베이스 생성
- [ ] schema.sql 실행 성공
- [ ] 테이블 **6개** 확인 (tracking 4개 + settings 2개)
- [ ] Hypertable 3개 확인
- [ ] 압축 정책 확인 (7일)
- [ ] 보관 정책 확인 (icd_status 90일)

---

## Part 2: 서버 연동 테스트

### 2.1 서버 시작

```powershell
cd g:\Kyu\repo\ACS\backend
./gradlew bootRun --args='--spring.profiles.active=office'
```

### 2.2 연결 확인 (로그)

```
# 정상 시 로그:
R2DBC ConnectionFactory created
Database connected: r2dbc:postgresql://localhost:5432/acs
```

### 2.3 Mock 데이터 INSERT 테스트

```powershell
# 1. 세션 생성
psql -U acs_user -d acs -c "
INSERT INTO tracking_session
(mst_id, detail_id, satellite_id, tracking_mode, data_type, start_time, end_time)
VALUES (999, 1, 'TEST-SAT', 'EPHEMERIS', 'original', NOW(), NOW() + INTERVAL '10 min')
RETURNING id;
"

# 2. 궤적 데이터 (세션 ID 사용)
psql -U acs_user -d acs -c "
INSERT INTO tracking_trajectory
(timestamp, session_id, detail_id, data_type, index, azimuth, elevation)
VALUES (NOW(), 1, 1, 'original', 0, 180.0, 45.0);
"

# 3. ICD 상태 데이터
psql -U acs_user -d acs -c "
INSERT INTO icd_status (timestamp, azimuth_angle, elevation_angle, train_angle)
VALUES (NOW(), 180.5, 45.2, 0.0);
"
```

### 2.4 데이터 조회 확인

```powershell
# 세션 조회
psql -U acs_user -d acs -c "SELECT * FROM tracking_session WHERE satellite_id = 'TEST-SAT';"

# 궤적 조회
psql -U acs_user -d acs -c "SELECT * FROM tracking_trajectory ORDER BY timestamp DESC LIMIT 5;"

# ICD 상태 조회
psql -U acs_user -d acs -c "SELECT * FROM icd_status ORDER BY timestamp DESC LIMIT 5;"
```

### 2.5 테스트 데이터 정리

```powershell
psql -U acs_user -d acs -c "DELETE FROM tracking_session WHERE satellite_id = 'TEST-SAT';"
```

### 2.6 Settings DB 연동 테스트

```powershell
# 1. 서버 시작 후 Settings 로그 확인
# 정상 시: "설정 초기화 완료 (DB 모드)"
# 비정상 시: "설정 초기화 완료 (RAM 전용 모드)"

# 2. FE에서 설정 변경 후 DB 확인
psql -U acs_user -d acs -c "SELECT key, value, updated_at FROM settings ORDER BY updated_at DESC LIMIT 10;"

# 3. 변경 이력 확인
psql -U acs_user -d acs -c "SELECT setting_key, old_value, new_value, created_at FROM setting_history ORDER BY created_at DESC LIMIT 10;"
```

**테스트 시나리오:**
1. FE Settings 모달 열기
2. 위도/경도 값 변경 → 저장
3. DB에서 settings 테이블 확인
4. 서버 재시작
5. FE에서 변경된 값 유지 확인 ← **영속성 검증**

### 2.7 체크리스트

- [ ] 서버 시작 (office 프로필)
- [ ] DB 연결 로그 확인
- [ ] tracking_session INSERT 성공
- [ ] tracking_trajectory INSERT 성공
- [ ] icd_status INSERT 성공
- [ ] **Settings DB 저장 확인**
- [ ] **서버 재시작 후 Settings 복원 확인**
- [ ] 데이터 조회 성공
- [ ] 테스트 데이터 정리

---

## Part 3: Architecture Refactoring 검증

### 3.1 SunTrack 모드 테스트

| 테스트 항목 | 확인 방법 | 결과 |
|------------|----------|------|
| 모드 시작 | SunTrack 페이지 → Start | [ ] |
| Train 초기화 | Train 각도 0도로 이동 | [ ] |
| 안정화 전환 | IDLE → INITIAL_TRAIN → STABILIZING → TRACKING | [ ] |
| Offset 변경 | Az/El Offset 변경 시 실시간 반영 | [ ] |
| 모드 중지 | Stop 버튼 → 정상 종료 | [ ] |

### 3.2 PassSchedule 모드 테스트

| 테스트 항목 | 확인 방법 | 결과 |
|------------|----------|------|
| 스케줄 로드 | 위성 선택 → 스케줄 표시 | [ ] |
| 첫 스케줄 선택 | 목록에서 선택 → 상태 전환 | [ ] |
| 추적 시작 | Start → 추적 동작 | [ ] |
| 추적 중지 | Stop → 정상 종료 | [ ] |

### 3.3 FE 리팩토링 검증 (CP3/CP4)

| 테스트 항목 | 확인 방법 | 결과 |
|------------|----------|------|
| deep watch 동작 | FE 재시작 후 실시간 데이터 확인 | [ ] |
| shallowRef 영향 | icdStore 데이터 변경 감지 | [ ] |
| 차트 렌더링 | PassSchedule 차트 정상 표시 | [ ] |

### 3.4 BE 서비스 분리 검증 (Phase 5)

| 테스트 항목 | 확인 방법 | 결과 |
|------------|----------|------|
| EphemerisTLECache | 로그: "TLE 캐시 로드" | [ ] |
| PassScheduleTLECache | 로그: "PassSchedule TLE 캐시" | [ ] |
| EphemerisDataRepository | 로그: "궤적 데이터 저장" | [ ] |
| PassScheduleDataRepository | 로그: "PassSchedule 데이터" | [ ] |

---

## Part 4: Settings 검증 및 정리

### 4.1 분석 결과 요약

| 상태 | 개수 | 설명 |
|------|------|------|
| ✅ 정상 연동 | 13개 | FE-BE 연동 + 로직에서 사용 |
| ⚠️ 미사용 | 15개 | 정의만 존재, 로직에서 미사용 |
| ⚠️ 부분 사용 | 12개 | API 응답만, 로직 미사용 |

### 4.2 정상 연동 설정 (13개) - 확인만

| 설정 키 | 사용 위치 | 확인 |
|---------|----------|------|
| `location.latitude` | SunTrackService, TrackingService | [ ] |
| `location.longitude` | SunTrackService, TrackingService | [ ] |
| `location.altitude` | SunTrackService, TrackingService | [ ] |
| `tracking.msInterval` | TrackingService (WebSocket) | [ ] |
| `antennaspec.tiltAngle` | TrackingService (좌표변환) | [ ] |
| `anglelimits.elevationMin` | SatelliteService (Pass 필터링) | [ ] |
| `ephemeris.tracking.*` | TrackingService | [ ] |
| `system.suntrack.*` | SunTrackService | [ ] |
| `feed.enabledBands` | FeedService | [ ] |

### 4.3 미사용 설정 (Dead Settings) - 정리 필요

#### 우선순위 HIGH (로직 연결 필요)

| 설정 키 | 현재 상태 | 조치 방안 |
|---------|----------|----------|
| `tracking.durationDays` | 2일 하드코딩 | 설정값 사용하도록 수정 |
| `tracking.minElevationAngle` | 미사용 | Pass 필터링에 적용 |

**수정 위치:**
```kotlin
// backend/.../service/SatelliteService.kt
// 현재: val durationDays = 2
// 변경: @Value("\${acs.settings.tracking.duration-days:2}") var durationDays: Int
```

#### 우선순위 MEDIUM (기능 미구현)

| 설정 키 | 상태 | 비고 |
|---------|------|------|
| `stow.azimuth` | Stow 모드 미구현 | 추후 구현 시 연결 |
| `stow.elevation` | Stow 모드 미구현 | 추후 구현 시 연결 |
| `stow.train` | Stow 모드 미구현 | 추후 구현 시 연결 |
| `speedlimits.*` (6개) | 모터 제어 미구현 | ICD 명령에서 사용 예정 |

#### 우선순위 LOW (삭제 검토)

| 설정 키 | 상태 | 조치 |
|---------|------|------|
| `stepsizelimit.*` | Step 모드에서 미사용 | 삭제 또는 연결 |
| `algorithm.geoMinMotion` | 알고리즘에서 미사용 | 삭제 검토 |

### 4.4 부분 사용 설정 - 검토 필요

| 설정 키 | 현재 상태 | 필요 조치 |
|---------|----------|----------|
| `anglelimits.azimuthMin/Max` | API 응답만 | 안테나 한계 체크에 사용? |
| `anglelimits.elevationMax` | API 응답만 | 안테나 한계 체크에 사용? |
| `anglelimits.trainMin/Max` | API 응답만 | 안테나 한계 체크에 사용? |

**질문:** 이 값들을 실제 명령 전송 시 한계 체크에 사용해야 하는가?

### 4.5 Settings 정리 체크리스트

#### 즉시 수정 (HIGH)
- [ ] `tracking.durationDays` → SatelliteService에서 설정값 사용
- [ ] `tracking.minElevationAngle` → Pass 필터링에 적용

#### 추후 연결 (MEDIUM)
- [ ] `stow.*` → Stow 모드 구현 시
- [ ] `speedlimits.*` → ICD 모터 명령 구현 시

#### 검토 후 결정 (LOW)
- [ ] `stepsizelimit.*` → 삭제 또는 Step 모드에 연결
- [ ] `anglelimits.*` → 한계 체크 로직 추가 여부

---

## Part 5: 통합 테스트 (선택)

### 5.1 실제 추적 데이터 저장 테스트

**조건:** ICD 장비 연결 상태

```yaml
테스트 시나리오:
  1. EphemerisDesignation 모드 시작
  2. 추적 시작 → 궤적 데이터 생성
  3. DB에 tracking_session 저장 확인
  4. DB에 tracking_trajectory 저장 확인
  5. 추적 종료
```

### 5.2 ICD 100ms 데이터 저장 테스트

```yaml
테스트 시나리오:
  1. ICD 연결 상태 확인
  2. 10초간 데이터 수신
  3. icd_status 테이블 확인 (약 100건)
  4. 타임스탬프 간격 확인 (100ms)
```

---

## 문제 해결

### DB 연결 실패

```
오류: Connection refused
원인: PostgreSQL 서비스 미실행

해결:
1. services.msc → PostgreSQL 서비스 시작
2. 또는: net start postgresql-x64-16
```

### Hypertable 생성 실패

```
오류: cannot create a unique index without the column "timestamp"
원인: PRIMARY KEY가 있는 테이블

해결:
1. DROP TABLE 후 재생성
2. schema.sql에서 PRIMARY KEY 제거 확인
```

### 서버 시작 실패 (R2DBC)

```
오류: R2dbcConnectionFactory not found
원인: 프로필 설정 오류

해결:
1. --spring.profiles.active=office 확인
2. application-office.properties 존재 확인
```

---

## 완료 체크리스트

### Phase 5: Native DB 설치
- [ ] TimescaleDB 설치
- [ ] 사용자/DB 생성
- [ ] 스키마 적용
- [ ] Hypertable 확인

### Phase 6: 서버 연동
- [ ] 서버 시작 (office 프로필)
- [ ] DB 연결 확인
- [ ] CRUD 테스트

### Refactoring 검증
- [ ] SunTrack 모드
- [ ] PassSchedule 모드
- [ ] FE deep watch
- [ ] BE 서비스 분리

### Settings 정리
- [ ] HIGH 우선순위 수정
- [ ] MEDIUM 검토
- [ ] LOW 결정

---

**Last Updated**: 2026-01-18
