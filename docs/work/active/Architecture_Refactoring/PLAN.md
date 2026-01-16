# ACS Refactoring Plan

> **Version**: 3.6.0 | **Date**: 2026-01-15
> **Status**: 실행 준비 완료
> **Target**: Sprint 0 ~ Phase 5 완료 (BE 서비스 분리만 1/20 이후)

---

## 난이도 범례

| 표시 | 난이도 | 예상 시간 | 설명 |
|:----:|:------:|:---------:|------|
| 🟢 | 쉬움 | ~30분 | 패턴 적용, 단순 수정 |
| 🟡 | 중간 | 1-3시간 | 분석 필요, 테스트 필수 |
| 🔴 | 높음 | 5시간+ | 영향 범위 큼, 주의 필요 |
| 🔴🔴 | 매우 높음 | 8시간+ | 복잡한 의존성, 신중히 진행 |

---

## ⚠️ 핵심 우려사항 TOP 3

| 순위 | 항목 | 위험 | 대응 방법 |
|:----:|------|------|----------|
| 1️⃣ | **P2-1 deep watch** | 무한 루프로 브라우저 멈춤 | 한 개씩 수정 후 즉시 테스트 |
| 2️⃣ | **P2-3 icdStore** | shallowRef 변환 시 UI 반응 안 함 | 컴포넌트별 영향 확인 필수 |
| 3️⃣ | **P3-1 PassSchedulePage** | 분리 후 기능 동작 안 함 | 작은 단위로 점진적 분리 |

---

## 검증 결과 요약

| 카테고리 | Legacy 보고 | 실제 확인 | 상태 |
|----------|-------------|-----------|------|
| !! 연산자 | 46건 | **7건** | ✅ 확인 |
| Thread.sleep | 2건 | **2건** | ✅ 확인 |
| runBlocking | 1건 | **1건** | ✅ 확인 |
| Path Traversal | 1건 | **1건** | ✅ **Critical** |
| CORS Wildcard | 1건 | **1건** | ✅ **Critical** |
| GlobalData 동시성 | 18필드 | **18필드** | ✅ 확인 |
| console.log | 1,513건 | **988건** | ✅ 확인 |
| shallowRef | 0건 | **0건** | ✅ 확인 |
| deep watch | 34건 | **34건** | ✅ 확인 |
| innerHTML (XSS) | 4건 | **4건** | ✅ **High** |
| Graceful Shutdown | - | **2건 누락** | ✅ 확인 |
| subscribe() 에러 핸들러 | 4건 | **6건** | ✅ 확인 |
| 테스트 파일 | 1건 | **2건 (BE)** | ✅ 확인 |

---

## 1. 우선순위 체계

```
Sprint 0: 보안 Critical (1/15 오전)
    ↓
Phase 1: BE 안정성 (1/15)
    ↓
Phase 2: FE 성능 (1/16~17)
    ↓
Phase 4: 품질 개선 (1/17~18)
    ↓
Phase 3: FE 파일 분리 (1/18~19)
    ↓
Phase 5: 키보드 단축키 (1/19)
    ↓
Phase 3: BE 서비스 분리 (1/20~) ← P3-4
    ↓
장기: 테스트, 인증, Docker, CI/CD (개발 완료 후)
```

---

## 실행 방식

> **Phase별 일괄 수정 → 빌드/테스트 → 문제 발생 시 즉시 수정**

| 단계 | 작업 | 설명 |
|:----:|------|------|
| 1 | **일괄 수정** | Phase 내 모든 항목 한 번에 수정 |
| 2 | **빌드** | BE: `./gradlew build` / FE: `npm run build` |
| 3 | **테스트** | 서버 실행, 주요 기능 확인 |
| 4 | **디버깅** | 문제 발생 시 증상 공유 → 즉시 수정 |

**이유**: 우려 항목들은 증상이 명확하고, 원인 파악 후 수정이 빠름 (5~15분)

| 우려 항목 | 증상 | 수정 시간 |
|----------|------|:---------:|
| deep watch 무한 루프 | 브라우저 멈춤 | 5분 |
| shallowRef UI 안 바뀜 | 화면 값 고정 | 10분 |
| 파일 분리 기능 안됨 | 버튼 반응 없음 | 15분 |
| BE 순환 의존성 | 서버 시작 실패 | 10분 |

---

## 수동 테스트 체크포인트 (협의 완료)

> **작업 흐름**: 코드 수정 → 빌드 → 🛑 체크포인트 → 사용자 테스트 → 피드백 → 다음 단계

| CP | 시점 | 테스트 내용 | 확인 방법 |
|:--:|------|-------------|-----------|
| **CP1** | Sprint 0 완료 후 | 보안 수정 확인 | Path Traversal 공격 차단, CORS 제한 |
| **CP2** | Phase 1 완료 후 | BE 서버 정상 동작 | 서버 시작, 기본 API 호출 |
| **CP3** | P2-1 deep watch 후 | 무한 루프 없음 | PassSchedule, Ephemeris 페이지 동작 |
| **CP4** | P2-3 icdStore 후 | UI 반응성 확인 | Dashboard에서 실시간 데이터 표시 |
| **CP5** | P3 각 파일 분리 후 | 기능 동작 확인 | 해당 페이지의 모든 버튼/기능 동작 |

**문제 발생 시**: 증상 공유 → 즉시 수정 (5~15분)

---

## Sprint 0: 보안 Critical (2시간)

| 항목 | 난이도 | 예상 시간 |
|------|:------:|:---------:|
| S0-1 Path Traversal | 🟢 | 15분 |
| S0-2 CORS Wildcard | 🟢 | 10분 |
| S0-3 innerHTML XSS | 🟡 | 1시간 |

### S0-1. Path Traversal 수정 🟢

**파일**: [LoggingController.kt:172-173](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/controller/system/LoggingController.kt#L172-L173)

```kotlin
// Before (취약)
val filePath = Paths.get(LOGS_DIRECTORY, fileName)

// After (안전)
val normalizedPath = Paths.get(LOGS_DIRECTORY, fileName).normalize()
if (!normalizedPath.startsWith(Paths.get(LOGS_DIRECTORY).normalize())) {
    throw IllegalArgumentException("Invalid file path")
}
```

**위험**: `GET /api/logging/download/../../../../etc/passwd` 공격 가능

---

### S0-2. CORS Wildcard 제거 🟢

**파일**: [CorsConfig.kt:26](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/config/CorsConfig.kt#L26)

```kotlin
// Before (취약)
allowedOriginPatterns = listOf(
    "http://localhost:9000",
    "http://127.0.0.1:*",
    "*"  // ← 삭제
)

// After (안전)
allowedOriginPatterns = listOf(
    "http://localhost:9000",
    "http://127.0.0.1:9000"
)
```

---

### S0-3. innerHTML XSS 수정 🟡

**파일**: [windowUtils.ts](../../../frontend/src/utils/windowUtils.ts) - 4곳

| 라인 | 현재 | 수정 |
|------|------|------|
| 709 | `button.innerHTML = ...` | `textContent` 또는 DOM API |
| 821 | `title.innerHTML = ...` | `textContent` 사용 |
| 847 | `popupButton.innerHTML = ...` | `createElement` 사용 |
| 869 | `modalButton.innerHTML = ...` | `createElement` 사용 |

---

## Phase 1: BE 안정성 (8-12시간)

| 항목 | 난이도 | 예상 시간 | 우려사항 |
|------|:------:|:---------:|----------|
| P1-1 !! 연산자 (7건) | 🟢 | 1시간 | 없음 |
| P1-2 Thread.sleep (2건) | 🟢 | 30분 | 없음 |
| P1-3 runBlocking (1건) | 🟡 | 2시간 | 호출 체인 추적 필요 |
| P1-4 GlobalData (18필드) | 🟡 | 3시간 | 테스트 필수 |
| P1-5 subscribe() (6건) | 🟢 | 30분 | 없음 |
| P1-6 Graceful Shutdown (2건) | 🟢 | 30분 | 없음 |

### P1-1. !! 연산자 제거 🟢

| 파일 | 라인 | 코드 | 수정 방법 |
|------|------|------|-----------|
| SunTrackService.kt | 103 | `modeTask!!.isCancelled` | `modeTask?.isCancelled ?: false` |
| SunTrackService.kt | 424 | `getTrainOffsetCalculator()!!` | `?.let {}` + early return |
| SunTrackService.kt | 462 | `getTrainOffsetCalculator()!!` | `?.let {}` + early return |
| PassScheduleService.kt | 719 | `preparingPassId!!` | null check + early return |
| PassScheduleService.kt | 923-937 | `lastDisplayedSchedule!!` | `?.let {}` 패턴 |
| EphemerisService.kt | 1113 | `modeTask!!.isCancelled` | `?: false` |
| EphemerisService.kt | 2717-2720 | `currentTrackingPass!!` | null check + early return |

**수정 패턴**:
```kotlin
// Before
val mstId = (currentTrackingPass!!["MstId"] as? Number)?.toLong()

// After
val pass = currentTrackingPass ?: return
val mstId = (pass["MstId"] as? Number)?.toLong()
```

---

### P1-2. Thread.sleep → Mono.delay 🟢

| 파일 | 라인 | 현재 | 변경 |
|------|------|------|------|
| [UdpFwICDService.kt](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/service/udp/UdpFwICDService.kt#L1109) | 1109 | `Thread.sleep(1000)` | `Mono.delay(Duration.ofSeconds(1))` |
| [BatchStorageManager.kt](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/service/system/BatchStorageManager.kt#L294) | 294 | `Thread.sleep(100)` | `Mono.delay(Duration.ofMillis(100))` |

---

### P1-3. runBlocking 제거 🟡

**파일**: [ElevationCalculator.kt:78](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/algorithm/elevation/ElevationCalculator.kt#L78)

```kotlin
// Before
fun getElevationComparisonBlocking(): ElevationComparison =
    kotlinx.coroutines.runBlocking { getElevationComparison() }

// After
suspend fun getElevationComparison(): ElevationComparison { ... }
// 호출부에서 suspend 또는 Mono로 변환
```

---

### P1-4. GlobalData 동시성 안전화 🟡

**파일**: [GlobalData.kt](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/model/GlobalData.kt)

**18개 var 필드** → AtomicReference 적용:

```kotlin
// Before
object Offset {
    var TimeOffset: Float = 0.0f
    var azimuthPositionOffset: Float = 0.0f
    // ... 5개 필드
}

// After
object Offset {
    private val _timeOffset = AtomicReference(0.0f)
    var TimeOffset: Float
        get() = _timeOffset.get()
        set(value) = _timeOffset.set(value)
    // ... 나머지 동일 패턴
}
```

**대상 객체**:
- `Time`: 2개 필드
- `Offset`: 5개 필드
- `EphemerisTrakingAngle`: 3개 필드
- `SunTrackingData`: 6개 필드
- `Version`: 2개 필드

---

### P1-5. subscribe() 에러 핸들러 추가 🟢

| 파일 | 라인 | 현재 |
|------|------|------|
| PassScheduleService.kt | 405 | `.subscribe { event -> }` |
| PassScheduleService.kt | 417 | `.subscribe { event -> }` |
| EphemerisService.kt | 135 | `.subscribe { event -> }` |
| EphemerisService.kt | 148 | `.subscribe { event -> }` |
| UdpFwICDService.kt | 195 | `.subscribe { }` (Mono.delay) |
| UdpFwICDService.kt | 933 | `.subscribe { this.run() }` |

```kotlin
// Before
.subscribe { event -> handleEvent(event) }

// After
.subscribe(
    { event -> handleEvent(event) },
    { error -> logger.error("Event handling failed", error) }
)
```

---

### P1-6. Graceful Shutdown 완성 🟢

**현재 상태**: 핵심 서비스 5개는 `@PreDestroy` 구현됨, 스레드 풀 정리 누락

| 파일 | 현재 | 수정 |
|------|------|------|
| [ThreadManager.kt](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/config/ThreadManager.kt) | `shutdown()` 있지만 호출 안됨 | `@PreDestroy` 추가 |
| [BatchStorageManager.kt](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/service/system/BatchStorageManager.kt) | 정리 코드 없음 | `@PreDestroy` cleanup 추가 |

**ThreadManager.kt 수정**:
```kotlin
// Before
fun shutdown() {
    logger.info("🔄 스레드 풀 정리 시작")
    // ...
}

// After
@PreDestroy
fun shutdown() {
    logger.info("🔄 스레드 풀 정리 시작")
    // ...
}
```

**BatchStorageManager.kt 수정**:
```kotlin
@PreDestroy
fun cleanup() {
    logger.info("🔄 BatchStorageManager 정리 시작")
    // 남은 배치 데이터 처리
    processBatch()
    logger.info("✅ BatchStorageManager 정리 완료")
}
```

**참고**: 이미 구현된 @PreDestroy (5개)
- PushDataController - WebSocket 세션 종료
- EphemerisService - 구독 해제 + 타이머 중지
- PassScheduleService - 구독 해제
- SunTrackService - 타이머 중지
- UdpFwICDService - UDP 통신 중지

---

## Phase 2: FE 성능 (12-18시간)

| 항목 | 난이도 | 예상 시간 | 우려사항 |
|------|:------:|:---------:|----------|
| P2-1 deep watch (34건) | 🔴 | 6시간 | ⚠️ 무한 루프 위험 |
| P2-2 console.log (988건) | 🟡 | 4시간 | 찾기/바꾸기 자동화 |
| P2-3 icdStore 최적화 | 🔴 | 6시간 | ⚠️ UI 깨짐 위험 |

### P2-1. deep watch 최적화 🔴

**Critical 발견**: PassSchedulePage.vue:1209에 주석:
> "이 Watch는 위 두 개와 완전히 중복 + deep: true로 인해 무한 루프 발생"

| 파일 | 건수 | 우선순위 |
|------|------|----------|
| PassSchedulePage.vue | 2 | **Critical** (무한 루프 위험) |
| EphemerisDesignationPage.vue | 1 | High |
| Settings 컴포넌트들 | 27 | Medium |
| 기타 | 4 | Low |

**수정 패턴**:
```typescript
// Before (deep watch)
watch(data, callback, { deep: true })

// After (명시적 필드 watch)
watch(() => data.specificField, callback)
// 또는
watch([() => data.field1, () => data.field2], callback)
```

---

### P2-2. console.log 정리 🟡

| 파일 | 건수 | 우선순위 |
|------|------|----------|
| PassSchedulePage.vue | 128 | High |
| TLEUploadContent.vue | 64 | Medium |
| EphemerisDesignationPage.vue | 63 | High |
| DashboardPage.vue | 60 | High |
| passScheduleStore.ts | 103 | High |
| windowUtils.ts | 46 | Medium |
| 기타 | 524 | Low |

**해결책**: 조건부 로깅 유틸리티 도입
```typescript
// utils/logger.ts
export const devLog = (...args: any[]) => {
  if (import.meta.env.DEV) console.log(...args)
}
```

---

### P2-3. icdStore 최적화 🔴

**파일**: [icdStore.ts](../../../frontend/src/stores/icd/icdStore.ts) (2,971줄, 81개 ref)

**현재 문제**: 30ms마다 81개 ref 개별 업데이트 → 과도한 반응성 트리거

**Phase 1**: 객체 타입 ref → shallowRef (즉시 적용)
```typescript
// Before
const antennaStatus = ref<AntennaStatus>({...})

// After
const antennaStatus = shallowRef<AntennaStatus>({...})
// 업데이트 시 객체 전체 교체
antennaStatus.value = { ...newData }
```

**Phase 2**: 상태 그룹화 (중기)
```typescript
// 81개 개별 ref → 5개 그룹
const positionState = shallowRef({ az, el, train, speeds... })
const boardState = shallowRef({ status1, status2... })
```

---

## Phase 3: 대형 파일 분리 (20-30시간) - 1/20 이후

| 항목 | 난이도 | 예상 시간 | 우려사항 |
|------|:------:|:---------:|----------|
| P3-1 PassSchedulePage.vue | 🔴🔴 | 8시간 | ⚠️ 가장 복잡, props drilling |
| P3-2 EphemerisPage.vue | 🔴 | 6시간 | 상태 공유 로직 |
| P3-3 icdStore.ts | 🔴 | 5시간 | WebSocket 연결 상태 |
| P3-4 BE 서비스 분리 | 🔴🔴 | 10시간 | ⚠️ 순환 의존성 위험 |

### P3-1. PassSchedulePage.vue 🔴🔴

```
현재: PassSchedulePage.vue (4,838줄)
    ↓ 분할
pages/mode/passSchedule/
├── PassSchedulePage.vue (~1,800줄)
│   └── 레이아웃 + 조합
├── components/
│   ├── ScheduleTable.vue (~500줄)
│   ├── ScheduleInfoPanel.vue (~300줄)
│   ├── ScheduleChart.vue (~400줄)
│   └── ScheduleControls.vue (~300줄)
└── composables/
    └── usePassScheduleTracking.ts (~600줄)
```

---

### P3-2. EphemerisDesignationPage.vue 🔴

```
현재: EphemerisDesignationPage.vue (4,340줄)
    ↓ 분할
pages/mode/ephemerisDesignation/
├── EphemerisDesignationPage.vue (~1,800줄)
├── components/
│   ├── SatelliteInfoPanel.vue (~300줄)
│   ├── TLEInputDialog.vue (~250줄)
│   ├── TrackingChart.vue (~400줄)
│   └── KeyholeSection.vue (~200줄)
└── composables/
    └── useEphemerisTracking.ts (~500줄)
```

---

### P3-3. icdStore.ts 🔴

```
현재: icdStore.ts (2,971줄)
    ↓ 분할
stores/icd/
├── index.ts (re-export)
├── icdStore.ts (~1,000줄) - 핵심 상태 + WebSocket
├── icdAntennaState.ts (~600줄) - 안테나 위치/속도
├── icdBoardStatus.ts (~700줄) - 보드 상태 비트
└── icdTrackingState.ts (~400줄) - 추적 상태
```

---

### P3-4. BE 대형 서비스 분리 🔴🔴

**EphemerisService.kt (5,057줄)**:
```
service/ephemeris/
├── EphemerisService.kt (~1,500줄) - 오케스트레이션
├── EphemerisStateMachine.kt (~1,000줄) - 상태 전이
├── EphemerisTLEManager.kt (~500줄) - TLE 캐시
├── EphemerisDataBatcher.kt (~500줄) - 배치 저장
└── EphemerisCommandSender.kt (~800줄) - UDP 명령
```

**PassScheduleService.kt (3,846줄)**:
```
service/passSchedule/
├── PassScheduleService.kt (~1,200줄) - CRUD
├── PassScheduleStateMachine.kt (~800줄) - 상태 전이
├── PassScheduleMonitor.kt (~600줄) - 모니터링
└── PassScheduleTracker.kt (~700줄) - 실시간 추적
```

---

## Phase 4: 품질 개선 (10시간) 🟡

| 항목 | 난이도 | 예상 시간 | 우려사항 |
|------|:------:|:---------:|----------|
| P4-1 @Valid 검증 (13개+) | 🟢 | 2시간 | 없음 |
| P4-2 catch(Exception) (88건) | 🟡 | 8시간 | 각 예외 분석 필요 |

### P4-1. @Valid 검증 추가 🟢

**현재**: 5개만 사용 (SettingsController)
**대상**: 모든 @RequestBody 파라미터 (13개+)

```kotlin
// Before
@PostMapping("/track")
fun startTracking(@RequestBody request: TrackingRequest)

// After
@PostMapping("/track")
fun startTracking(@RequestBody @Valid request: TrackingRequest)

data class TrackingRequest(
    @field:NotNull val satelliteId: String,
    @field:Min(0) @field:Max(360) val azimuth: Double
)
```

---

### P4-2. catch(Exception) 구체화 (88건) 🟡

```kotlin
// Before
catch (e: Exception) {
    logger.error("Error", e)
}

// After
catch (e: IllegalArgumentException) {
    logger.warn("Invalid input: ${e.message}")
    throw BadRequestException(e.message)
} catch (e: IOException) {
    logger.error("I/O error", e)
    throw InternalServerException("File operation failed")
}
```

---

## 장기: 개발 완료 후 진행

> 핵심 리팩토링 (Sprint 0 ~ Phase 3) 완료 후 진행하는 항목들

### 장기-1. 테스트 추가

**현재**: BE 2개, FE 0개

**목표**:
```
backend/src/test/
├── service/
│   ├── EphemerisServiceTest.kt
│   ├── PassScheduleServiceTest.kt
│   └── ICDServiceTest.kt
├── algorithm/
│   ├── LimitAngleCalculatorTest.kt
│   └── CoordinateTransformerTest.kt
└── controller/
    └── EphemerisControllerTest.kt

frontend/src/__tests__/
├── stores/
│   └── icdStore.spec.ts
├── composables/
│   └── useAxisFormatter.spec.ts
└── pages/
    └── DashboardPage.spec.ts
```

---

### 장기-2. 보안 강화 (인증/인가)

**현재**: 모든 API 엔드포인트가 인증 없이 접근 가능

**필요 작업**:
| Task | 파일 |
|------|------|
| Spring Security 의존성 추가 | build.gradle.kts |
| SecurityConfig 생성 | config/SecurityConfig.kt |
| JWT 토큰 검증 구현 | - |
| 로그인 API 구현 | AuthController.kt |

```kotlin
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/api/auth/**").permitAll()
                    .pathMatchers("/api/**").authenticated()
            }
            .oauth2ResourceServer { it.jwt {} }
            .build()
    }
}
```

---

### 장기-3. Docker 컨테이너화

**Backend Dockerfile**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/acs-backend-*.jar app.jar
COPY orekit-data /app/orekit-data
ENV OREKIT_DATA_PATH=/app/orekit-data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Frontend Dockerfile**:
```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist/spa /usr/share/nginx/html
EXPOSE 80
```

**docker-compose.yml**:
```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
```

---

### 장기-4. CI/CD 파이프라인 (GitLab 전환 시)

```yaml
# .gitlab-ci.yml
stages:
  - test
  - build
  - deploy

backend-test:
  stage: test
  image: eclipse-temurin:21
  script:
    - cd backend && ./gradlew test

frontend-test:
  stage: test
  image: node:20
  script:
    - cd frontend && npm ci && npm run test

build:
  stage: build
  script:
    - docker-compose build
  only:
    - main
```

---

### 장기 항목 요약

| 항목 | 설명 | 예상 시간 |
|------|------|:---------:|
| 장기-1 | 테스트 추가 (BE/FE) | 20h+ |
| 장기-2 | 인증/인가 (Spring Security) | 16h+ |
| 장기-3 | Docker 컨테이너화 | 8h+ |
| 장기-4 | CI/CD 파이프라인 | 8h+ |

---

## Phase 5: 키보드 단축키 (2시간) 🟢

> 이번 일정에 포함 (1/19)

### P5-1. useKeyboardNavigation composable 생성

**파일**: `frontend/src/composables/useKeyboardNavigation.ts`

```typescript
import { onMounted, onUnmounted } from 'vue'

interface KeyboardOptions {
  onEscape?: () => void
  onEnter?: () => void
  onCtrlEnter?: () => void
}

export function useKeyboardNavigation(options: KeyboardOptions) {
  const handleKeyDown = (e: KeyboardEvent) => {
    switch (e.key) {
      case 'Escape':
        options.onEscape?.()
        break
      case 'Enter':
        if (e.ctrlKey) {
          options.onCtrlEnter?.()
        } else {
          options.onEnter?.()
        }
        break
    }
  }

  onMounted(() => document.addEventListener('keydown', handleKeyDown))
  onUnmounted(() => document.removeEventListener('keydown', handleKeyDown))
}
```

### P5-2. 모달/다이얼로그에 적용

**적용 대상**:
- 모든 `q-dialog` 컴포넌트 → ESC로 닫기
- 확인 다이얼로그 → Enter로 확인

```vue
<script setup>
import { useKeyboardNavigation } from '@/composables/useKeyboardNavigation'

const dialogVisible = ref(false)

useKeyboardNavigation({
  onEscape: () => { dialogVisible.value = false },
  onCtrlEnter: () => { submitForm() }
})
</script>
```

---

## 실행 일정 (1/15 ~ 1/21)

### 가용 시간

> **평일**: 업무 8시간 (09:00~18:00) + 퇴근 후 4시간 (20:00~24:00) = **12시간**
> **주말**: 수면 제외 전일 작업 (14h+)

| 날짜 | 요일 | 시간대 | 가용 시간 |
|------|:----:|--------|:---------:|
| 1/15 | 목 | 09:00~18:00 + 20:00~24:00 | 12h |
| 1/16 | 금 | 09:00~18:00 + 20:00~24:00 | 12h |
| 1/17 | 토 | 12:00~02:00+ | 14h+ |
| 1/18 | 일 | 10:00~24:00+ | 14h+ |
| 1/19 | 월 | 09:00~18:00 + 20:00~24:00 | 12h |
| 1/20 | 화 | 09:00~18:00 + 20:00~24:00 | 12h |
| 1/21 | 수 | 09:00~18:00 + 20:00~24:00 | 12h |
| **총합** | | | **88h+** |

### 상세 일정

| 날짜 | 시간 | 작업 | 난이도 | 목표 |
|------|:----:|------|:------:|------|
| **1/15 (목)** | 12h | Sprint 0 (2h) + Phase 1 전체 (8h) | 🟢🟡 | ✅ Phase 1 완료 |
| **1/16 (금)** | 12h | P2-1 deep watch (6h) + P2-2 console.log (4h) | 🔴 | 🔄 Phase 2 시작 |
| **1/17 (토)** | 14h | P2-3 icdStore (6h) + P4-1 (2h) + P4-2 시작 (6h) | 🔴 | ⚠️ 집중 필요 |
| **1/18 (일)** | 14h | P4-2 완료 (2h) + P3-1 PassSchedulePage (8h) | 🔴🔴 | 🔄 Phase 3 시작 |
| **1/19 (월)** | 12h | P3-2 EphemerisPage (6h) + P3-3 icdStore 분리 (3h) + P5 키보드 (2h) | 🔴 | ✅ Phase 5 완료 |
| **1/20 (화)** | 12h | P3-4 BE 서비스 분리 (10h) | 🔴🔴 | 🔄 BE 분리 진행 |
| **1/21 (수)** | 12h | 버퍼 / 마무리 / 검증 | 🟡 | ✅ **전체 완료** |

### 마일스톤

| 마일스톤 | 예상 완료일 | Phase |
|----------|:-----------:|:-----:|
| 🔐 보안 수정 완료 | 1/15 (목) 오전 | Sprint 0 |
| 🔧 BE 안정화 완료 | 1/15 (목) 저녁 | Phase 1 |
| ⚡ FE 성능 개선 완료 | 1/17 (토) | Phase 2 |
| 🧪 품질 개선 완료 | 1/18 (일) 오전 | Phase 4 |
| 📦 FE 파일 분리 완료 | 1/19 (월) | Phase 3 (FE) |
| ⌨️ 키보드 단축키 완료 | 1/19 (월) | Phase 5 |
| 📦 BE 서비스 분리 완료 | 1/20~21 (화~수) | Phase 3 (BE) |
| ✅ **전체 리팩토링 완료** | **1/21 (수)** | - |

### Phase별 요약

| Phase | 작업 | 항목 수 | 예상 시간 | 상태 |
|-------|------|:-------:|:---------:|:----:|
| **Sprint 0** | 보안 Critical | 3건 | 2시간 | 🎯 1/15 (목) |
| **Phase 1** | BE 안정성 | 6항목 | 8시간 | 🎯 1/15 (목) |
| **Phase 2** | FE 성능 | 3항목 | 16시간 | 🎯 1/16~17 |
| **Phase 4** | 품질 개선 | 2항목 | 10시간 | 🎯 1/17~18 |
| **Phase 3** | FE 파일 분리 | 3항목 | 17시간 | 🎯 1/18~19 |
| **Phase 5** | 키보드 단축키 | 2항목 | 2시간 | 🎯 1/19 (월) |
| **Phase 3** | BE 서비스 분리 | 1항목 | 10시간 | 🎯 1/20~21 |
| **장기** | 테스트/인증/Docker | 4항목 | - | 📅 개발 완료 후 |

---

## 검증 체크리스트

### Sprint 0 완료 조건
- [ ] Path Traversal: `../../` 패턴 테스트 → 403/400 응답
- [ ] CORS: 외부 Origin 요청 → 차단 확인
- [ ] innerHTML: windowUtils.ts 4곳 → DOM API 전환

### Phase 1 완료 조건
- [ ] !! 연산자 0건 (`grep -r "!!" backend/` → 0)
- [ ] Thread.sleep 0건
- [ ] runBlocking 0건
- [ ] GlobalData AtomicReference 적용 확인
- [ ] subscribe() 에러 핸들러 6건 추가 확인
- [ ] Graceful Shutdown: ThreadManager, BatchStorageManager에 @PreDestroy 확인
- [ ] `./gradlew build` 성공

### Phase 2 완료 조건
- [ ] deep watch: 무한 루프 없음 확인 (특히 PassSchedulePage.vue)
- [ ] console.log 정리 완료 (devLog 유틸리티 적용)
- [ ] icdStore shallowRef 적용 확인
- [ ] `npm run dev` 성능 테스트 (CPU 20% 이하)
- [ ] `npm run build` 성공

### Phase 4 완료 조건
- [ ] @Valid: 모든 @RequestBody에 적용 확인
- [ ] catch(Exception): 구체적 예외로 변환 (88건)
- [ ] `./gradlew build` 성공

### Phase 3 완료 조건 (FE)
- [ ] PassSchedulePage.vue: 1,800줄 이하로 분리
- [ ] EphemerisDesignationPage.vue: 1,800줄 이하로 분리
- [ ] icdStore.ts: 1,000줄 이하로 분리
- [ ] `npm run build` 성공

### Phase 5 완료 조건
- [ ] useKeyboardNavigation.ts 생성 확인
- [ ] 모달/다이얼로그에서 ESC로 닫기 동작 확인
- [ ] `npm run build` 성공

---

## 보류 항목 (Backlog)

> 현재 리팩토링 범위에서 제외된 항목들. 추후 별도 요청 시 진행.

| ID | 항목 | 설명 | 우선순위 | 비고 |
|----|------|------|:--------:|------|
| BL-1 | **로깅 유틸리티 연계** | 기존 988개 console.log → logger.ts로 교체 | 낮음 | Production 자동 제거 설정 완료됨. 새 코드만 logger 사용 권장 |
| BL-2 | **Settings 실시간 제어 분리** | 메인터넌스 기능(실시간)과 일반 설정(적용 버튼) UI 분리 | 중간 | UX 개선 사항. 별도 아이콘/메뉴로 관리자 기능 분리 |
| BL-3 | **DB 설계 (RFC-001)** | PostgreSQL + TimescaleDB 위성 추적 데이터 저장 | 별도 | 4개 테이블 설계 완료. 구현은 별도 Feature로 |
| BL-4 | **코드 품질 Cleanup** | 하드코딩 색상 → 테마 변수, 중복 로직 통합, 인라인 스타일 정리 | 중간 | Phase 3 완료 후 일괄 진행. `/cleanup` 스킬 활용 |

### BL-1. 로깅 유틸리티 연계

**현재 상태**:
- ✅ `logger.ts` 생성 완료
- ✅ Production 빌드 시 console 자동 제거 설정 완료

**추가 작업 (선택)**:
```typescript
// 기존
console.log('데이터:', data)

// 변경
import { logger } from '@/utils/logger'
logger.debug('데이터:', data)
```

**효과**: 개발 시 로그 레벨 관리 (debug/info/warn/error)

---

### BL-2. Settings 실시간 제어 분리

**현재 구조**:
```
⚙️ Settings 모달
├── 일반 설정 (적용 버튼 방식)
├── 시스템 설정 (적용 버튼 방식)
└── 관리자 설정
    └── 메인터넌스 (실시간 적용) ← 혼재
```

**제안 구조**:
```
⚙️ Settings              🔧 Maintenance (관리자 전용)
├── 일반 설정             ├── 서보 테스트 (실시간)
├── 시스템 설정           ├── MC On/Off (실시간)
└── 관리자 설정           └── 인코더 프리셋 (실시간)
```

**구현 시 필요 작업**:
- 새 아이콘/메뉴 추가
- MaintenanceSettings 분리
- 권한 체크 로직 추가

---

### BL-3. DB 설계 (RFC-001)

**문서 위치**: `docs/work/active/Architecture_Refactoring/legacy/RFC-001_Database_Strategy.md`

**설계 완료 항목**:
- PostgreSQL 16 + TimescaleDB
- 4개 테이블: tracking_master, tracking_detail, realtime_result, icd_realtime

**결정**: 리팩토링 완료 후 진행

| 근거 | 설명 |
|------|------|
| 현재 기능 정상 | DB 없이도 기존 기능 모두 동작 |
| 신규 기능 성격 | 버그 수정이 아닌 기능 확장 |
| 코드 준비 | Phase 3 서비스 분리 후 DB 레이어 추가 용이 |

**실행 순서**:
```
1. 리팩토링 완료 (Phase 3까지)
   ↓
2. DB 구현 시작
   - PostgreSQL + TimescaleDB 설치
   - 테이블 생성 (4개)
   - Repository 레이어 추가
   - Service에 저장 로직 연동
```

---

### BL-4. 코드 품질 Cleanup

**대상 파일** (Phase 3 완료 후 일괄 진행):
- `passSchedule/components/*.vue` - 4개 컴포넌트
- `ephemerisDesignation/components/*.vue` - 분리 예정 컴포넌트들
- 기타 분리된 컴포넌트들

**발견된 이슈**:

| 유형 | 개수 | 위치 | 설명 |
|------|:----:|------|------|
| 하드코딩 색상 | 27개 | ScheduleTable, ScheduleChart 등 | `#ff5722`, `#2196f3` 등 → `var(--theme-*)` 변환 |
| 중복 로직 | 2곳 | ScheduleTable.vue | `getRowStyleDirect`/`getRowClass` 동일 매칭 로직 |
| 인라인 스타일 | 다수 | OffsetControls.vue | `style="width: 110px !important"` 반복 |

**작업 내용**:
```typescript
// Before
itemStyle: { color: '#ff5722' }

// After
itemStyle: { color: 'var(--theme-accent-color)' }
```

**실행 방법**: `/cleanup` 스킬 활용
```bash
/cleanup passSchedule/components
/cleanup ephemerisDesignation/components
```

**우선순위**: 중간 (기능 동작에 영향 없음, 유지보수성 개선)

---

## References

- [refactoring-hints.md](../../architecture/context/analysis/synthesis/refactoring-hints.md)
- [backend.md](../../architecture/context/architecture/backend.md)
- [frontend.md](../../architecture/context/architecture/frontend.md)
- [legacy/](./legacy/) - 기존 RFC 문서들

---

**Version**: 3.6.0
**Last Updated**: 2026-01-15
**Verified By**: Code Analysis Agents + Manual Review
**Target Completion**: 1/21 (수) - 전체 리팩토링 완료
