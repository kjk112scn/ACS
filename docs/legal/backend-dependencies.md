# Backend Dependencies 라이선스 상세

백엔드(Kotlin + Spring Boot) 프로젝트의 모든 Gradle 의존성 라이선스 상세 정보입니다.

**검토일**: 2026-01-19
**빌드 도구**: Gradle (Kotlin DSL)
**설정 파일**: `backend/build.gradle.kts`

---

## Core Framework

### Spring Boot 3.4.4
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) 2012-2024 Pivotal, Inc.
- **용도**: 애플리케이션 프레임워크
- **GitHub**: https://github.com/spring-projects/spring-boot
- **상업적 사용**: ✅ 허용
- **의무사항**:
  - 라이선스 텍스트 포함
  - NOTICE 파일 포함

#### 포함 모듈
| 모듈 | 용도 |
|------|------|
| spring-boot-starter-webflux | 반응형 웹 서버 |
| spring-boot-starter-data-r2dbc | 반응형 데이터베이스 |
| spring-boot-starter-validation | 데이터 검증 |

---

### Kotlin 2.1.0
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) 2010-2024 JetBrains s.r.o.
- **용도**: 프로그래밍 언어
- **GitHub**: https://github.com/JetBrains/kotlin
- **상업적 사용**: ✅ 허용

#### 관련 패키지
| 패키지 | 버전 | 라이선스 |
|--------|------|---------|
| kotlin-stdlib | 2.1.0 | Apache 2.0 |
| kotlin-reflect | 2.1.0 | Apache 2.0 |
| kotlinx-coroutines-core | 1.10.1 | Apache 2.0 |
| kotlinx-coroutines-reactor | 1.10.1 | Apache 2.0 |

---

## Scientific Computing

### Orekit 13.0.2
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) 2002-2024 CS GROUP
- **용도**: 위성 궤도 역학 라이브러리
- **웹사이트**: https://www.orekit.org/
- **GitHub**: https://github.com/CS-SI/Orekit
- **상업적 사용**: ✅ 허용
- **의무사항**:
  - 라이선스 텍스트 포함
  - NOTICE 파일 포함
- **프로젝트 내 사용**:
  - `SolarOrekitCalculator.kt` - 고정밀 태양 위치 계산
  - 위성 궤도 추적 및 예측
- **데이터 파일**: `orekit-data-main/` (Public Domain)

#### Orekit 데이터 파일
| 데이터 | 라이선스 | 출처 |
|--------|---------|------|
| DE-440 Ephemerides | Public Domain | NASA JPL |
| Earth Orientation Parameters | Public Domain | IERS |
| Space Weather Data | Public Domain | CSSI |
| TAI-UTC Data | Public Domain | IERS |

---

### Solar Positioning 2.0.3
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) Klaus Brunner
- **용도**: 태양 위치 계산 (SPA/GRENA3 알고리즘)
- **GitHub**: https://github.com/klausbrunner/solarpositioning
- **상업적 사용**: ✅ 허용
- **프로젝트 내 사용**:
  - `SPACalculator.kt` - Solar Position Algorithm
  - `Grena3Calculator.kt` - Grena3 알고리즘
- **알고리즘 출처**:
  - SPA: NREL (National Renewable Energy Laboratory)
  - GRENA3: Roberto Grena, 2008

---

## Database

### PostgreSQL JDBC Driver 42.7.5
- **라이선스**: BSD-2-Clause
- **저작권**: Copyright (c) 1997, PostgreSQL Global Development Group
- **용도**: PostgreSQL 데이터베이스 연결
- **GitHub**: https://github.com/pgjdbc/pgjdbc
- **상업적 사용**: ✅ 허용
- **의무사항**: 라이선스 텍스트 포함

### R2DBC PostgreSQL 1.0.7
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) 2018-2024 R2DBC Contributors
- **용도**: 반응형 PostgreSQL 드라이버
- **GitHub**: https://github.com/r2dbc/r2dbc-postgresql
- **상업적 사용**: ✅ 허용

---

## JSON Processing

### Jackson (Spring Boot 내장)
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) FasterXML
- **용도**: JSON 직렬화/역직렬화
- **GitHub**: https://github.com/FasterXML/jackson
- **상업적 사용**: ✅ 허용

---

## API Documentation

### SpringDoc OpenAPI 2.8.6
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) 2019-2024 springdoc.org
- **용도**: OpenAPI 3.0 문서 자동 생성
- **GitHub**: https://github.com/springdoc/springdoc-openapi
- **상업적 사용**: ✅ 허용
- **포함 컴포넌트**:
  - Swagger UI (Apache 2.0)
  - OpenAPI Specification

---

## Testing (devDependencies)

### JUnit 5
- **라이선스**: Eclipse Public License 2.0
- **용도**: 단위 테스트 프레임워크
- **상업적 사용**: ✅ 허용
- **참고**: EPL 2.0은 상업적 사용 허용

### MockK
- **라이선스**: Apache License 2.0
- **용도**: Kotlin 모킹 라이브러리
- **상업적 사용**: ✅ 허용

### Kotest
- **라이선스**: Apache License 2.0
- **용도**: Kotlin 테스트 프레임워크
- **상업적 사용**: ✅ 허용

---

## 라이선스 분포 요약

| 라이선스 | 개수 | 비율 |
|----------|------|------|
| Apache 2.0 | 12+ | 85% |
| BSD-2-Clause | 1 | 7% |
| Public Domain | 1 | 7% |
| EPL 2.0 | 1 | <1% (테스트만) |

---

## 데이터 파일 라이선스

### Orekit Data (`backend/src/main/resources/orekit-data-main/`)

| 디렉토리 | 내용 | 라이선스 | 출처 |
|----------|------|---------|------|
| `DE-440-ephemerides/` | 행성 위치 데이터 | Public Domain | NASA JPL |
| `Earth-Orientation-Parameters/` | 지구 자전 파라미터 | Public Domain | IERS |
| `CSSI-Space-Weather-Data/` | 우주 날씨 데이터 | Public Domain | CSSI |
| `tai-utc.dat` | 시간 스케일 변환 | Public Domain | IERS |
| `fes2004_Cnm-Snm.dat` | 지구 중력장 모델 | Public Domain | - |

**참고**: NASA CDDIS 데이터는 Public Domain으로 상업적 사용에 제한이 없습니다.
출처 표기 권장: "Contains data from NASA's Crustal Dynamics Data Information System"

---

## 의무사항 체크리스트

### Apache 2.0 패키지 (Spring, Kotlin, Orekit 등)
- [x] 라이선스 텍스트 포함 (`docs/legal/license-texts/Apache-2.0.txt`)
- [ ] NOTICE 파일 포함 (배포 시)
- [ ] 수정 사항 문서화 (수정 시)

### BSD-2-Clause 패키지 (PostgreSQL Driver)
- [x] 라이선스 텍스트 포함 (`docs/legal/license-texts/BSD-3-Clause.txt`)

### Public Domain 데이터 (Orekit Data)
- [x] 제한 없음
- [ ] 출처 표기 권장

---

## Gradle 의존성 확인 명령어

```bash
# 모든 의존성 출력
cd backend && ./gradlew dependencies

# 특정 의존성 상세
./gradlew dependencyInsight --dependency orekit

# 라이선스 리포트 (플러그인 필요)
./gradlew generateLicenseReport
```

---

## 업데이트 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-01-19 | 최초 작성 |
