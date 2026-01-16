# Database Integration Design

> **Version**: 1.0.0 | **Date**: 2026-01-17
> **Status**: 설계 완료 (Ready for Implementation)
> **Reference**: `Architecture_Refactoring/legacy/RFC-001_Database_Strategy.md`

---

## 1. 개요

### 목적
- 추적 데이터 영구 저장 (서버 재시작 시 보존)
- 과거 이력 조회 및 CSV 내보내기
- ICD 상태 데이터 100ms 주기 저장

### 기술 스택

| 구성요소 | 선택 | 이유 |
|---------|------|------|
| DBMS | PostgreSQL 16 | 안정성, 성능, 무료 |
| 확장 | TimescaleDB | 시계열 최적화, 자동 압축/삭제 |
| 드라이버 | Spring Data R2DBC | 논블로킹 I/O, WebFlux 통합 |

---

## 2. 테이블 설계

### 2.1 테이블 구조

| 테이블 | 설명 | 데이터량 |
|--------|------|---------|
| `tracking_session` | 추적 세션 (패스별 요약) | ~100건/일 |
| `tracking_trajectory` | 이론 궤적 데이터 | ~10,000건/세션 |
| `tracking_result` | 실측 추적 결과 | ~10,000건/세션 |
| `icd_status` | ICD 제어 상태 (100ms) | 864,000건/일 |

### 2.2 관계도

```
tracking_session (1) ─┬─ (N) tracking_trajectory
                      └─ (N) tracking_result

icd_status (독립 - 시계열)
```

### 2.3 보관 정책

| 테이블 | 보관 기간 | 삭제 방식 | 압축 |
|--------|----------|----------|------|
| tracking_session | 30일 | Spring Scheduler | 없음 |
| tracking_trajectory | 30일 | CASCADE | TimescaleDB (7일 후) |
| tracking_result | 30일 | CASCADE | TimescaleDB (7일 후) |
| icd_status | 30일 | TimescaleDB retention | TimescaleDB (7일 후) |

---

## 3. 구현 파일

### Backend 신규 파일

```
backend/src/main/kotlin/.../
├── entity/
│   ├── TrackingSessionEntity.kt
│   ├── TrackingTrajectoryEntity.kt
│   ├── TrackingResultEntity.kt
│   └── IcdStatusEntity.kt
├── repository/
│   ├── TrackingSessionRepository.kt
│   ├── TrackingTrajectoryRepository.kt
│   ├── TrackingResultRepository.kt
│   └── IcdStatusRepository.kt
└── service/
    ├── TrackingDataLoader.kt      # 초기 로딩
    └── TrackingExportService.kt   # CSV 내보내기
```

### 기존 파일 수정

| 파일 | 변경 내용 |
|------|----------|
| `build.gradle.kts` | R2DBC 의존성 추가 |
| `application.yml` | DB 연결 설정 추가 |
| `EphemerisService.kt` | DB 저장 호출 추가 |
| `PassScheduleService.kt` | DB 저장 호출 추가 |
| `UdpFwICDService.kt` | ICD 배치 저장 추가 |

---

## 4. 의존성

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
    runtimeOnly("org.postgresql:postgresql:42.7.1")
}
```

---

## 5. 설정

```yaml
# application.yml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/acs
    username: acs_user
    password: ${DB_PASSWORD}
    pool:
      initial-size: 5
      max-size: 20
      max-idle-time: 30m

acs:
  database:
    retention-days: 30  # Settings 모달에서 변경 가능
```

---

## 6. 구현 순서

```
Phase 1: 인프라 (1일)
├── PostgreSQL + TimescaleDB 설치
├── 테이블 생성 (sql/schema.sql)
└── R2DBC 의존성 + 설정

Phase 2: Entity/Repository (1일)
├── Entity 클래스 4개
├── Repository 인터페이스 4개
└── 빌드 테스트

Phase 3: Service 연동 (2일)
├── TrackingDataLoader (초기 로딩)
├── EphemerisService DB 연동
├── PassScheduleService DB 연동
└── UdpFwICDService 배치 저장

Phase 4: Export/UI (1일)
├── TrackingExportService (CSV)
├── Settings 모달에 보관 기간 설정
└── 과거 이력 조회 API
```

---

## References

- [schema.sql](sql/schema.sql) - 테이블 생성 SQL
- [PROGRESS.md](PROGRESS.md) - 구현 진행 상황
- [RFC-001](../Architecture_Refactoring/legacy/RFC-001_Database_Strategy.md) - 원본 설계 문서

---

**Last Updated**: 2026-01-17
