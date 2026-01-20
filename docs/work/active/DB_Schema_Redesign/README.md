# DB Schema Redesign

## 개요

**목적**: Flyway 마이그레이션 완전 재구성 - 새 PC 설치 시 자동 테이블 생성
**요청일**: 2026-01-20
**상태**: ✅ 완료

## 문제점

| 문제 | 심각도 | 설명 |
|------|--------|------|
| 마이그레이션 누락 | CRITICAL | 8개 테이블 중 2개만 CREATE TABLE 존재 |
| 타입 불일치 가능성 | HIGH | Entity와 SQL 컬럼 동기화 안 됨 |
| 새 PC 설치 실패 | CRITICAL | 테이블 없어서 앱 시작 실패 |

## 해결

### 마이그레이션 재설계

| 파일 | 내용 | 테이블 수 |
|------|------|:--------:|
| V001__Create_all_tables.sql | 8개 테이블 CREATE + TimescaleDB 설정 | 8 |
| V002__Insert_default_settings.sql | 기본 설정값 63개 | - |

### 테이블 목록

| 테이블 | 유형 | 압축 | 삭제 정책 |
|--------|------|------|----------|
| settings | 일반 | - | 영구 보관 |
| setting_history | 일반 | - | 영구 보관 |
| tracking_session | 일반 | - | 365일 (Spring) |
| tracking_trajectory | Hypertable | 7일 | CASCADE |
| tracking_result | Hypertable | 7일 | CASCADE |
| icd_status | Hypertable | 7일 | 90일 |
| tle_cache | 일반 | - | Soft Delete |
| hardware_error_log | Hypertable | 30일 | 365일 |

### Entity-SQL 타입 매칭

- 총 168개 @Column 필드 검증 완료
- 175개 SQL 컬럼 (id 포함) 모두 일치
- 불일치 항목: 0개

## 검증 결과

- [x] Backend 빌드 성공
- [x] Entity-SQL 타입 100% 매칭
- [x] Hypertable 설정 완료 (4개)
- [x] Index 및 FK 정의 완료

## 관련 파일

- [V001__Create_all_tables.sql](../../../backend/src/main/resources/db/migration/V001__Create_all_tables.sql)
- [V002__Insert_default_settings.sql](../../../backend/src/main/resources/db/migration/V002__Insert_default_settings.sql)
- [IcdStatusStorageService.kt](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/tracking/service/IcdStatusStorageService.kt)

## 다음 단계

1. **DB 초기화**: 기존 DB 삭제 후 Flyway 재실행
2. **테스트**: 앱 시작 및 테이블 생성 확인

## ICD 저장 로직 (추가 완료)

| 구성요소 | 설명 |
|---------|------|
| IcdStatusStorageService.kt | UDP → DB 배치 저장 서비스 |
| saveInterval | 100ms (설정 가능) |
| batchSize | 1000건 (설정 가능) |

**동작 방식:**
1. `DataStoreService`에서 100ms마다 ICD 상태 수집
2. 버퍼에 축적 후 1초마다 DB 배치 저장
3. UDP 연결 시에만 저장 (불필요한 null 방지)
