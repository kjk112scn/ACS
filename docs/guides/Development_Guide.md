# ACS API 개발 가이드

---
**문서 버전**: 1.1.0  
**최종 업데이트**: 2024-12  
**작성자**: GTL Systems
---

> **목적**: ACS 프로젝트 전체 개발 흐름 및 핵심 기능 개요

---

## 📋 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [시작하기](#2-시작하기)
3. [아키텍처](#3-아키텍처)
4. [핵심 기능](#4-핵심-기능)
5. [API 가이드](#5-api-가이드)
6. [배포](#6-배포)
7. [변경 이력](#7-변경-이력)
8. [문서 구조](#8-문서-구조)

---

## 1. 프로젝트 개요

### 목적
위성 및 태양 추적을 위한 안테나 제어 시스템(ACS) 백엔드

### 기술 스택
- **언어**: Kotlin 1.9
- **프레임워크**: Spring Boot 3.2 + WebFlux
- **데이터베이스**: In-Memory Storage (PostgreSQL 연동 준비)
- **외부 라이브러리**: Orekit (위성 궤도 계산)

### 프로젝트 구조
```
src/main/kotlin/com/gtlsystems/acs_api/
├── controller/      # API 엔드포인트
├── service/         # 비즈니스 로직
├── algorithm/       # 계산 알고리즘
├── config/          # 시스템 설정
└── model/           # 데이터 모델
```

---

## 2. 시작하기

### 환경 설정
```bash
# 필수 요구사항
- JDK 17+
- Gradle 8.0+
- Orekit 데이터 (src/main/resources/orekit-data-main/)

# 빌드 및 실행
./gradlew bootRun
```

### 기본 설정
- **포트**: 8080
- **WebSocket**: `ws://localhost:8080/ws`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

---

## 3. 아키텍처

### 계층 구조
```
Controller (HTTP/WebSocket)
    ↓
Service (비즈니스 로직)
    ↓
Algorithm (순수 계산)
    ↓
DataStore (In-Memory)
```

### 데이터 흐름
```
클라이언트 요청
    ↓
Controller (요청 검증)
    ↓
Service (비즈니스 로직)
    ↓
Algorithm (계산 수행)
    ↓
DataStore (결과 저장)
    ↓
응답 반환
```

📖 **상세**: [Data Flow](../architecture/context/architecture/data-flow.md)

---

## 4. 핵심 기능

### 4.1 위성 추적 (Ephemeris Tracking)

**목적**: TLE(Two-Line Element) 기반 위성 궤도 추적

**핵심 기능**:
- TLE 파싱 및 궤도 계산
- 2축(Az/El) → 3축(Train/Az/El) 좌표 변환
- 각도 제한 (±270°) 적용
- Pass Schedule 생성 및 관리
- CSV 내보내기

**주요 API**:
- `POST /api/ephemeris/tracking/calculate` - 궤도 계산
- `GET /api/ephemeris/tracking/mst/merged` - 전체 데이터 조회
- `GET /api/ephemeris/tracking/csv/{mstId}` - CSV 내보내기

**데이터 타입**:
- `original` - 2축 원본 데이터
- `axis_transformed` - 3축 변환 (Train=0, 각도 제한 ❌)
- `final_transformed` - 3축 최종 (Train=0, 각도 제한 ✅)

---

### 4.2 Train 알고리즘 ⭐ (Keyhole 회피)

**목적**: Keyhole 영역(±270° 근처) 회피를 위한 Train 각도 최적화

**도입 배경**:
- **Gimbal Lock 방지**: Azimuth ±270° 근처에서 발생하는 특이점 회피
- **포지셔너 제한**: 물리적 회전 한계 준수
- **추적 안정성**: 급격한 각속도 변화 최소화

#### 물리적 배치 및 좌표계

**안테나 구조**:
```
┌─────────────────┐
│  Elevation축    │  ← 최상단 (고도각 회전)
├─────────────────┤
│  Azimuth축      │  ← 중단 (방위각 회전)
├─────────────────┤
│  Train축        │  ← 최하단 (안테나 전체 회전)
└─────────────────┘
```

**좌표계**:
```
        👤 사용자 (북쪽에서 남쪽을 바라봄)
          
        북쪽 (0°)
          ↑
          |
서쪽 ←----●----→ 동쪽
(270°)  안테나   (90°)
          |
          ↓
        남쪽 (180°)
```

**±270° 제한 이유**:
- 기계적 안전: 포지셔너 물리적 제한
- Gimbal Lock 방지: Keyhole 영역 회피
- 안정적 추적: ±270° 범위 내에서만 동작

#### Keyhole 개념

**Keyhole 정의**: Azimuth가 ±270° 근처를 통과하는 위성

**발생 조건**:
- 위성 궤도가 Azimuth 260° → 280° 이동하는 경우
- 270° 기계적 한계 통과 → **Gimbal Lock 위험**
- 포지셔너 물리적 제한으로 추적 불가

**해결 방법**:
- Train 각도로 회전하여 ±270° 영역 회피
- 예: Train=-90° 적용 시 260° → -170° (270° 회피)

#### Keyhole 판단 기준

**판단 데이터**: `final_transformed` (Train=0)의 MaxAzRate

**판단 로직**:
```kotlin
val train0MaxAzRate = finalMst["MaxAzRate"] as Double
val threshold = 10.0  // 기본값 10.0°/s
val isKeyhole = train0MaxAzRate >= threshold
```

**판단 과정**:
1. `final_transformed` (Train=0) 데이터 생성
2. MaxAzRate 계산 (각속도 계산 방법: 10-point cumulative sum)
3. MaxAzRate >= 10.0°/s → Keyhole 발생
4. Keyhole 발생 시 Train≠0 재계산 진행

#### 임계값 선택 가이드

| 임계값 | 효과 | 사용 시나리오 |
|--------|------|-------------|
| 1.0°/s | 과도한 Train 적용 | 테스트 (비권장, 역효과 가능) |
| 3.0°/s | 보수적 판단 | 안전 우선 |
| **10.0°/s** | **공격적 판단** | **진짜 위험한 위성만 Train 적용 (권장)** |

**역효과 사례**:
```
패스 #8: MaxAzRate = 1.099°/s
임계값 1.0°/s → Keyhole 판단 → Train 적용
→ 결과: 각속도 증가 (1.099 → 3.188°/s) ← 역효과!

임계값 10.0°/s → Keyhole 미발생 → Train=0 유지
→ 결과: 최적 (1.099°/s 유지)
```

**결론**: 높은 임계값(10.0°/s)이 더 효과적

#### 6가지 DataType

| DataType | Train | 각도 제한 | 저장 | 용도 |
|----------|-------|----------|------|------|
| `original` | N/A | N/A | ✓ | 2축 원본 데이터 (위성 좌표) |
| `axis_transformed` | 0° | ❌ | ✓ | 3축 변환 중간 (Train=0, 0-360°) |
| `final_transformed` | 0° | ✅ | ✓ | 최종 데이터 (Train=0, ±270°), **Keyhole 판단 기준** |
| `keyhole_axis_transformed` | ≠0 | ❌ | ✓ | Keyhole 3축 중간 (Train≠0, 0-360°) |
| `keyhole_final_transformed` | ≠0 | ✅ | ✓ | Keyhole 최종 (Train≠0, ±270°), **실제 사용** |

#### Train 각도 계산

**방법**: 최종 최대 각속도 시점 기준 (방법 B)

**계산 과정**:
1. `final_transformed` (Train=0) 데이터 생성
2. 최대 각속도 시점의 Azimuth 추출
3. Train 각도 계산: `trainAngle = -azimuthAtMaxRate`
4. Keyhole 발생 시 Train≠0으로 재변환

**공식**:
```kotlin
// 최단 거리로 Train 각도 계산
val azimuthAtMaxRate = finalMst["MaxAzRateAzimuth"]  // 예: 102.6°
val trainAngle = -azimuthAtMaxRate  // 예: -102.6°
// 102.6°를 0° 근처로 이동 → 최단 거리 회전
```

**Train 각도의 목적**:
- ✅ **Keyhole 회피**: ±270° 영역 통과 방지 (주 목적)
- ❌ **각속도 최소화 아님** (부차적 효과)

#### 각속도 계산 방법

**방법**: 10-point cumulative sum method

**계산식**:
```kotlin
// 1초간 (10개 포인트) 총 변화량
for (i in 9 until dtl.size) {
    var sum = 0.0
    for (j in (i - 9)..i) {
        val diff = dtl[j] - dtl[j-1]  // 변화량
        sum += abs(diff)  // 누적 (시간으로 나누지 않음)
    }
    maxVelocity = maxOf(maxVelocity, sum)
}
```

**특징**:
- **단위**: °/s (도/초)
- **정밀도**: 10개 포인트 평활화
- **시간 분할**: 1초 단위 (100ms × 10)

#### 데이터 흐름

```
┌─────────────────────────────────────────────────────────────┐
│ Original (2축)                                              │
│   - Azimuth: 257.197°                                       │
│   - Train: 0° (저장만)                                       │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ Axis Transformed (Train=0, 각도 제한 ❌)                    │
│   - Azimuth: ~267° (0-360° 범위)                            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ Final Transformed (Train=0, 각도 제한 ✅)                    │
│   - Azimuth: ~267° (±270° 범위)                             │
│   - MaxAzRate: 4.493°/s ← Keyhole 판단 기준                │
└─────────────────────────────────────────────────────────────┘
                              ↓
                    [Keyhole 판단]
                              ↓
                    ┌───────┴───────┐
                    │               │
         Keyhole 미발생    Keyhole 발생
            (종료)             │
                              ↓
         ┌───────────────────────────────────────────────┐
         │ Keyhole Axis Transformed                     │
         │   - Train≠0 (예: -167.4°)                    │
         │   - 각도 제한 ❌ (0-360° 범위)                │
         └───────────────────────────────────────────────┘
                              ↓
         ┌───────────────────────────────────────────────┐
         │ Keyhole Final Transformed                    │
         │   - Train≠0                                  │
         │   - 각도 제한 ✅ (±270° 범위)                 │
         │   - MaxAzRate: 2.663°/s ← 최적화 완료       │
         └───────────────────────────────────────────────┘
```

**데이터 저장**:
- **Keyhole 미발생**: `original`, `axis_transformed`, `final_transformed`
- **Keyhole 발생**: 위 3개 + `keyhole_axis_transformed`, `keyhole_final_transformed`

**완료 상태**: ✅ 구현 완료 (2024-12)

📖 **상세**: [Train Algorithm Design](../architecture/algorithms/Train_Algorithm_Design.md)

---

### 4.3 설정 관리 (Settings Management)

**목적**: 동적 설정 변경 및 관리 (재시작 불필요)

**핵심 기능**:
- 타입별 설정 관리 (DOUBLE, LONG, STRING, BOOLEAN)
- 실시간 설정 변경
- 설정 변경 이벤트 발행
- 설정 유효성 검증

**주요 설정**:
- `keyholeAzimuthVelocityThreshold` (10.0°/s) - Keyhole 판단 임계값 (기본값)
  - 임계값 선택 가이드: 3.0°/s (보수적), 10.0°/s (권장)
- `tiltAngle` (0.0°) - 안테나 Tilt 각도
- `sourceMinElevationAngle` (5.0°) - 최소 고도각

**주요 API**:
- `GET /api/settings` - 전체 설정 조회
- `PUT /api/settings/{key}` - 설정 변경

---

### 4.4 태양 추적 (Sun Tracking)

**목적**: 태양 위치 실시간 추적 및 계산

**계산 방법**:
- **Orekit** (기본) - 고정밀 계산
- **SPA** (Solar Position Algorithm) - NREL 표준
- **Grena3** - 고속 근사 계산

**주요 API**:
- `GET /api/sun/position` - 현재 태양 위치
- `GET /api/sun/track` - 태양 추적 데이터

---

### 4.5 Pass Schedule 관리

**목적**: 위성 가시 구간(Pass) 스케줄링

**핵심 기능**:
- Pass 자동 감지
- 시작/종료 시간 계산
- 최대 고도각 추출
- Pass 데이터 저장 및 조회

**주요 API**:
- `GET /api/ephemeris/tracking/passes` - Pass 목록
- `GET /api/ephemeris/tracking/pass/{passId}` - Pass 상세

---

## 5. API 가이드

### REST API
- **Base URL**: `http://localhost:8080/api`
- **인증**: 없음 (내부 시스템)
- **응답 형식**: JSON
- **에러 처리**: 표준 HTTP 상태 코드

### WebSocket
- **URL**: `ws://localhost:8080/ws`
- **프로토콜**: STOMP over WebSocket
- **업데이트 주기**: 30ms (설정 가능)
- **토픽**: `/topic/tracking`, `/topic/status`

### API 문서
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **다국어 지원**: 한국어/영어
- **실시간 테스트**: Swagger UI에서 직접 테스트 가능

---

## 6. 배포

### 로컬 개발
```bash
# 개발 서버 실행
./gradlew bootRun

# 특정 포트로 실행
./gradlew bootRun --args='--server.port=9090'
```

### 프로덕션 빌드
```bash
# JAR 빌드
./gradlew clean bootJar

# 빌드 결과
build/libs/acs_api-0.0.1-SNAPSHOT.jar
```

### 실행
```bash
# 기본 실행
java -jar build/libs/acs_api-0.0.1-SNAPSHOT.jar

# 프로파일 지정
java -jar build/libs/acs_api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 메모리 설정
java -Xms512m -Xmx2048m -jar build/libs/acs_api-0.0.1-SNAPSHOT.jar
```

### Docker (준비 중)
```bash
# 이미지 빌드
docker build -t acs-api:1.1.0 .

# 컨테이너 실행
docker run -d -p 8080:8080 --name acs-api acs-api:1.1.0
```

---

## 7. 변경 이력

| 날짜 | 버전 | 주요 변경 |
|------|------|----------|
| 2024-12 | 1.1.0 | Train 알고리즘 추가 (Keyhole 회피) |
| 2024-12 | 1.0.1 | 설정 관리 개선 |
| 2024-11 | 1.0.0 | 초기 릴리즈 |

### v1.1.0 (2024-12) - Train 알고리즘
- ✨ Keyhole 회피를 위한 Train 각도 최적화
- ✨ 6가지 DataType 지원 (original, axis, final, keyhole_axis, keyhole_final)
- ✨ CSV 내보내기에 Keyhole 데이터 포함
- 🔧 Keyhole 판단 임계값 설정 (keyholeAzimuthVelocityThreshold)
  - 기본값: 10.0°/s (권장)
  - 임계값 선택 가이드: 1.0°/s (테스트, 비권장), 3.0°/s (보수적), 10.0°/s (공격적, 권장)
  - 역효과 사례 분석 및 로그 검증 완료
- 📝 완료 문서 작성

### v1.0.1 (2024-12) - 설정 관리
- 🔧 동적 설정 변경 기능 강화
- 📝 설정 관리 가이드 작성

### v1.0.0 (2024-11) - 초기 릴리즈
- 🎉 ACS API 초기 버전
- ✨ 위성 추적 기본 기능
- ✨ 태양 추적 기능
- ✨ WebSocket 실시간 업데이트

---

## 8. 문서 구조

```
docs/
├── README.md                      ← 문서 시스템 설명
├── architecture/                  ← 🏗️ 아키텍처/알고리즘
│   ├── SYSTEM_OVERVIEW.md
│   ├── algorithms/
│   └── context/
├── api/                           ← 📡 API 명세
├── guides/                        ← 📖 개발 가이드 (현재 문서)
├── work/                          ← 🔧 진행중/완료 작업
│   ├── active/
│   └── archive/
└── logs/                          ← 📅 일일 로그
```

### 문서 탐색 가이드

#### 새로운 기능 시작
1. `work/active/` 확인 - 진행 중인 계획
2. 없으면 새 계획 작성

#### 완료된 기능 확인
1. `work/archive/` 확인 - 구현 완료 문서

#### 상세 기술 문서
1. `architecture/` 확인 - 아키텍처 및 알고리즘

📖 **상세**: [docs/README.md](../README.md)

---

## 🔗 주요 링크

### 외부 문서
- [Orekit 공식 문서](https://www.orekit.org/site-orekit-12.1/index.html)
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Kotlin 공식 문서](https://kotlinlang.org/docs/home.html)

### 프로젝트 자료
- Frontend Repository: `ACS/` (Vue + Quasar)
- API Swagger: `http://localhost:8080/swagger-ui.html`

---

## 📞 지원

### 문의
- **프로젝트**: GTL ACS
- **관리자**: GTL Systems

### 기여
1. 새로운 기능 제안 → `work/active/` 계획 문서 작성
2. 코드 구현 및 테스트
3. `work/archive/` 완료 문서 작성
4. 관련 문서 업데이트

---

**문서 버전**: 1.1.0  
**최종 업데이트**: 2024-12  
**유지 관리자**: GTL Systems

