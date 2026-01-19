# Native DB 설치 로그

> **시작일**: 2026-01-19
> **완료일**: 2026-01-19
> **환경**: Windows 10, PostgreSQL 16.11, TimescaleDB 2.24.0
> **상태**: ✅ **Part 1 완료**

---

## 진행 상황

| 단계 | 항목 | 상태 | 비고 |
|------|------|:----:|------|
| 1.1 | PostgreSQL 16 설치 확인 | ✅ | 16.2 → 16.11 업그레이드 |
| 1.1 | TimescaleDB 파일 복사 | ✅ | 2.24.0 버전 |
| 1.1 | postgresql.conf 설정 | ✅ | shared_preload_libraries 추가 |
| 1.1 | 서비스 재시작 | ✅ | 정상 작동 |
| 1.2 | acs_user 사용자 생성 | ✅ | PASSWORD 'acs1234' |
| 1.2 | acs 데이터베이스 생성 | ✅ | OWNER acs_user |
| 1.2 | TimescaleDB 확장 활성화 | ✅ | CREATE EXTENSION 성공 |
| 1.3 | schema.sql 실행 | ✅ | 8개 테이블 생성 |
| 1.4 | 테이블 확인 | ✅ | 8개 테이블 |
| 1.5 | Hypertable 확인 | ✅ | 4개 Hypertable |

---

## 해결한 이슈

### 1. TimescaleDB DLL 로드 오류

```
오류: "C:/Program Files/PostgreSQL/16/lib/timescaledb-2.24.0.dll"
라이브러리를 불러 올 수 없음: The specified procedure could not be found.
```

**원인:** PostgreSQL 16.2와 TimescaleDB 2.24.0 빌드 불일치

**해결:** PostgreSQL 16.2 → 16.11 업그레이드

### 2. schema.sql 인코딩 오류

```
오류: 0xec 0x8b 바이트로 조합된 문자(인코딩: "UHC")와 대응되는 문자 코드가 "UTF8" 인코딩에는 없습니다
```

**원인:** Windows PowerShell 인코딩 문제 (한글 주석)

**해결:** `SET client_encoding TO 'UTF8';` 추가하여 실행

---

## 최종 설치 상태

### 접속 정보

| 항목 | 값 |
|------|-----|
| Host | localhost |
| Port | 5432 |
| Database | acs |
| User | acs_user |
| Password | acs1234 |

### 테이블 (8개)

| 테이블 | 타입 | 용도 |
|--------|------|------|
| tracking_session | 일반 | 추적 세션 메타데이터 |
| tracking_trajectory | Hypertable | 이론 궤적 |
| tracking_result | Hypertable | 실측 결과 |
| icd_status | Hypertable | ICD 100ms 데이터 |
| settings | 일반 | 시스템 설정 |
| setting_history | 일반 | 설정 변경 이력 |
| tle_cache | 일반 | TLE 캐시 |
| hardware_error_log | Hypertable | 하드웨어 에러 로그 |

---

## 다음 단계: Part 2 (서버 연동 테스트)

TEST_GUIDE.md Part 2 참조:
1. 서버 시작 (`./gradlew bootRun`)
2. DB 연결 로그 확인
3. Mock 데이터 INSERT 테스트
4. Settings DB 연동 테스트

---

**Last Updated**: 2026-01-19 12:00