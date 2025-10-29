# 코딩 표준 및 개발 가이드

---
**문서 버전**: 1.0.0  
**최종 업데이트**: 2024-12  
**작성자**: GTL Systems
---

## 📋 목차
1. [프로젝트 폴더 구조](#1-프로젝트-폴더-구조)
2. [계층별 역할 및 규칙](#2-계층별-역할-및-규칙)
3. [개발 프로세스](#3-개발-프로세스)
4. [코드 예시](#4-코드-예시)

---

## 1. 프로젝트 폴더 구조

### 전체 루트 구조
```
ACS_API/
├── docs/                  # 문서화
├── src/                   # 소스 코드
│   ├── main/
│   │   ├── kotlin/
│   │   └── resources/
│   └── test/
├── build/                 # 빌드 결과물
├── gradle/                # Gradle 설정
├── logs/                  # 로그 파일
├── orekit-data/           # Orekit 데이터
├── csv_exports/           # CSV 내보내기
├── .cursorrules           # 프로젝트 규칙
├── build.gradle.kts       # Gradle 빌드 스크립트
└── settings.gradle.kts    # Gradle 설정
```

### 소스 코드 계층
```
src/main/kotlin/com/gtlsystems/acs_api/
├── AcsApiApplication.kt   # 메인 애플리케이션 (진입점)
│
├── controller/            # API 엔드포인트 (HTTP 요청/응답)
│   ├── icd/              # ICD 통신 API
│   ├── mode/             # 모드별 API (Ephemeris, Sun, PassSchedule)
│   ├── websocket/        # WebSocket API
│   └── system/           # 시스템 관리 API
│
├── service/              # 비즈니스 로직 (핵심 기능)
│   ├── icd/             # ICD 프로토콜 처리
│   ├── mode/            # 모드별 서비스
│   ├── websocket/       # WebSocket 서비스
│   ├── udp/             # UDP 통신 서비스
│   ├── datastore/       # 데이터 저장 서비스
│   ├── system/          # 시스템 관리 서비스
│   └── logging/         # 로깅 서비스
│
├── algorithm/            # 위성/태양 추적 알고리즘 (순수 계산)
│   ├── axistransformation/    # 축 변환 알고리즘
│   ├── satellitetracker/      # 위성 추적 알고리즘
│   ├── axislimitangle/        # 축 제한각 계산
│   ├── elevation/             # 고도각 계산
│   └── suntrack/              # 태양 추적 알고리즘
│
├── model/                # 도메인 모델 (비즈니스 객체)
│   ├── GlobalData.kt
│   ├── PushData.kt
│   └── SatelliteTrackData.kt
│
├── config/               # 설정 및 구성 (시스템 설정)
│   ├── ThreadManager.kt
│   ├── OrekitConfig.kt
│   ├── WebSocketConfig.kt
│   ├── CorsConfig.kt
│   ├── GlobalExceptionHandler.kt
│   ├── Language.kt
│   └── OpenApiConfiguration.kt
│
├── event/                # 이벤트 처리 (시스템 이벤트)
│   ├── ACSEvent.kt
│   ├── ACSEventBus.kt
│   └── settings/
│
├── util/                 # 유틸리티 (공통 도구)
│   ├── CRC16Table.kt
│   ├── JKUtil.kt
│   └── ApiDescriptions.kt
│
├── openapi/              # OpenAPI 다국어 설명
│   ├── EphemerisApiDescriptions.kt
│   ├── PassScheduleApiDescriptions.kt
│   ├── SunTrackApiDescriptions.kt
│   ├── ICDApiDescriptions.kt
│   ├── SettingsApiDescriptions.kt
│   └── OpenApiUtils.kt
│
├── dto/                  # 데이터 전송 객체 (API 계층)
│   ├── request/
│   └── response/
│
├── repository/           # 데이터 액세스 (데이터 저장/조회)
│   └── interfaces/
│
└── settings/             # 설정 관련 엔티티
    └── entity/
```

### 리소스 계층
```
src/main/resources/
├── application.properties          # 애플리케이션 설정
├── db/migration/                   # 데이터베이스 마이그레이션
│   ├── V001Create_settings_tables.sql
│   └── V002Insert_default_settings.sql
├── logback-spring.xml              # 로깅 설정
├── static/                         # 정적 리소스
│   └── swagger-ui/
└── orekit-data-main/               # Orekit 데이터
    ├── CSSI-Space-Weather-Data/
    ├── DE-440-ephemerides/
    ├── Earth-Orientation-Parameters/
    ├── MSAFE/
    └── Potential/
```

---

## 2. 계층별 역할 및 규칙

### Controller 계층
**역할**: HTTP 요청/응답 처리, 클라이언트와의 통신

**규칙**:
- 비즈니스 로직 금지
- 데이터 변환만 담당
- `operationId` 기반 다국어 설명 자동 적용
- 요청 검증 (Validation)

**파일 배치**: 새로운 API는 적절한 하위 폴더에 배치

**예시**:
```kotlin
@RestController
@RequestMapping("/api/ephemeris")
class EphemerisController(
    private val ephemerisService: EphemerisService
) {
    @PostMapping("/calculate")
    @Operation(operationId = "calculateEphemeris")
    fun calculate(@RequestBody request: TrackingRequest): Mono<TrackingResponse> {
        return ephemerisService.calculate(request)
    }
}
```

---

### Service 계층
**역할**: 핵심 비즈니스 로직, 알고리즘 실행, 데이터 처리

**규칙**:
- 데이터 액세스 금지 (Repository 사용)
- 순수 비즈니스 로직만
- 트랜잭션 관리
- 에러 처리 및 로깅

**파일 배치**: 새로운 비즈니스 로직은 적절한 하위 폴더에 배치

**예시**:
```kotlin
@Service
class EphemerisService(
    private val satelliteTracker: SatelliteTracker,
    private val dataStore: DataStoreService
) {
    fun calculate(request: TrackingRequest): Mono<TrackingResponse> {
        return Mono.fromCallable {
            val result = satelliteTracker.track(request.tle)
            dataStore.save(result)
            TrackingResponse(result)
        }.subscribeOn(Schedulers.boundedElastic())
    }
}
```

---

### Algorithm 계층
**역할**: 순수 알고리즘, 수학적 계산, 도메인 특화 로직

**규칙**:
- 외부 의존성 최소화
- 순수 함수형 프로그래밍
- 상태 변경 금지 (Immutable)
- 단위 테스트 필수

**파일 배치**: 새로운 알고리즘은 적절한 하위 폴더에 배치

**예시**:
```kotlin
class CoordinateTransformer {
    fun transformCoordinatesWithTrain(
        azimuth: Double,
        elevation: Double,
        trainAngle: Double
    ): Pair<Double, Double> {
        // 순수 계산 로직
        val x = cos(elevation) * sin(azimuth)
        val y = cos(elevation) * cos(azimuth)
        val z = sin(elevation)
        
        // Train 회전 적용
        val rotatedX = x * cos(trainAngle) - y * sin(trainAngle)
        val rotatedY = x * sin(trainAngle) + y * cos(trainAngle)
        
        // 새로운 Azimuth, Elevation 계산
        val newAzimuth = atan2(rotatedX, rotatedY)
        val newElevation = asin(z)
        
        return Pair(newAzimuth, newElevation)
    }
}
```

---

### Repository 계층
**역할**: 데이터 저장/조회, 데이터베이스 연동, 파일 시스템 접근

**규칙**:
- 비즈니스 로직 금지
- 순수 데이터 액세스만
- 트랜잭션 경계 설정

**파일 배치**: 새로운 데이터 액세스는 repository/ 하위에 배치

---

### DTO 계층
**역할**: API 계층 데이터 전송, 클라이언트-서버 간 데이터 교환

**규칙**:
- 도메인 로직 금지
- 순수 데이터 전송만
- Validation 애노테이션 사용

**파일 배치**: dto/ 하위에 request/response 구분하여 배치

---

### Model 계층
**역할**: 비즈니스 객체, 도메인 엔티티, 핵심 데이터 구조

**규칙**:
- 순수 데이터 모델
- 비즈니스 로직 금지
- 불변성 권장 (data class)

**파일 배치**: 새로운 도메인 모델은 model/ 하위에 배치

---

### Config 계층
**역할**: 시스템 설정, 환경 구성, 외부 설정 관리

**규칙**:
- 설정값 제공
- 비즈니스 로직 금지
- Bean 정의

**파일 배치**: 새로운 설정 클래스는 config/ 하위에 배치

---

### Event 계층
**역할**: 시스템 이벤트, 비동기 처리, 컴포넌트 간 통신

**규칙**:
- 이벤트 정의 및 발행
- 비즈니스 로직 금지
- 느슨한 결합 유지

**파일 배치**: 새로운 이벤트는 event/ 하위에 배치

---

### Util 계층
**역할**: 공통 도구, 헬퍼 함수, 재사용 가능한 기능

**규칙**:
- 순수 함수
- 외부 의존성 최소화
- 정적 메서드 권장

**파일 배치**: 새로운 유틸리티는 util/ 하위에 배치

---

### OpenAPI 계층
**역할**: API 다국어 설명 관리, OpenAPI 문서화

**규칙**:
- operationId 기반 설명
- 한국어 우선 작성
- HTML 형식 지원

**파일 배치**: 새로운 API 설명은 openapi/ 하위에 배치

---

## 3. 개발 프로세스

### 작업 순서
1. **요구사항 분석** → 계획 문서 작성 (`docs/plans/`)
2. **폴더 구조** 생성 및 정리
3. **API 설명** 작성 (한국어 우선)
4. **인터페이스 설계** (Controller, Service, Repository)
5. **알고리즘 구현** (순수 계산 로직)
6. **비즈니스 로직** 구현 (Service)
7. **데이터 액세스** 구현 (Repository)
8. **API 엔드포인트** 구현 (Controller)
9. **테스트** 작성 및 실행
10. **문서화** 완료 (`docs/completed/`)

### 코드 리뷰 기준
- ✅ User Rules 준수 여부
- ✅ Project Rules 준수 여부
- ✅ 다국어 API 규칙 준수 여부
- ✅ 문서화 완성도
- ✅ 테스트 커버리지
- ✅ 성능 영향도

### 품질 체크
- ✅ **컴파일**: Kotlin 컴파일 오류 없음
- ✅ **린팅**: 코드 스타일 규칙 준수
- ✅ **다국어**: API 설명 자동 적용 확인
- ✅ **테스트**: 핵심 기능 동작 확인
- ✅ **문서**: API 문서 자동 생성 확인

---

## 4. 코드 예시

### 좋은 예시: 다국어 API 설명

#### Controller
```kotlin
/**
 * 정지궤도 위성 각도 계산 API
 */
@PostMapping("/3axis/tracking/geostationary/calculate-angles")
@Operation(
    operationId = "calculategeostationaryangles",  // ✅ 고유한 operationId
    tags = ["Mode - Ephemeris"]
)
fun calculateGeostationaryAngles(
    @RequestBody request: GeostationaryTrackingRequest
): Mono<Map<String, Any>> {
    return ephemerisService.calculateGeostationaryAngles(request)
}
```

#### API Descriptions
```kotlin
// EphemerisApiDescriptions.kt
object EphemerisApiDescriptions {
    fun applyDescriptions(operation: Operation, operationId: String, language: Language) {
        when (operationId.lowercase()) {
            "calculategeostationaryangles" -> {
                when (language) {
                    Language.KOREAN -> {
                        operation.summary = "정지궤도 위성 각도 계산"
                        operation.description = """
                            <h4>정지궤도 위성의 경도를 기반으로 안테나 추적 각도를 계산합니다.</h4>
                            <h4>계산되는 각도:</h4>
                            <ul>
                                <li>방위각: 북쪽 기준 수평각 (0° ~ 360°)</li>
                                <li>고도각: 지평선 기준 수직각 (0° ~ 90°)</li>
                            </ul>
                        """.trimIndent()
                    }
                    Language.ENGLISH -> {
                        operation.summary = "Calculate Geostationary Satellite Angles"
                        operation.description = """
                            <h4>Calculates antenna tracking angles based on geostationary satellite longitude.</h4>
                            <h4>Calculated Angles:</h4>
                            <ul>
                                <li>Azimuth: Horizontal angle from North (0° ~ 360°)</li>
                                <li>Elevation: Vertical angle from horizon (0° ~ 90°)</li>
                            </ul>
                        """.trimIndent()
                    }
                }
            }
        }
    }
}
```

### 나쁜 예시: 계층 분리 위반

```kotlin
// ❌ Controller에 비즈니스 로직
@PostMapping("/calculate")
fun calculate(@RequestBody request: TrackingRequest): Mono<TrackingResponse> {
    // ❌ Controller에서 직접 계산
    val result = satelliteTracker.track(request.tle)
    dataStore.save(result)
    return Mono.just(TrackingResponse(result))
}

// ❌ Service에서 데이터 액세스
class EphemerisService {
    fun calculate(request: TrackingRequest): TrackingResponse {
        // ❌ Service에서 직접 DB 접근
        val data = jdbcTemplate.query("SELECT * FROM tracking")
        return process(data)
    }
}
```

---

## 📚 관련 문서

- **전체 개발 가이드**: [docs/Development_Guide.md](../../Development_Guide.md)
- **설정 시스템**: [Settings_Development_Guide.md](Settings_Development_Guide.md)
- **API 참조**: [docs/references/api/](../api/)

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2024-12  
**유지 관리자**: GTL Systems

