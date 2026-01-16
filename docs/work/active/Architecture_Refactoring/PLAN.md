# ACS Refactoring Plan (TODO)

> **Version**: 5.0.0 | **Date**: 2026-01-17
> **Status**: ✅ 리팩토링 완료 - DB 설계만 남음
> **완료 기록**: TRACKER.md 참조

---

## 📊 남은 작업 요약

| Phase | 상태 | 비고 |
|-------|:----:|------|
| Sprint 0~6 | ✅ 완료 | 보안, BE 안정성, FE 성능, 파일분리, 품질, 서비스분리, 키보드 |
| **DB 설계** | 📋 대기 | 전문가 검토 후 진행 |
| 장기 | 📅 | 테스트/인증/Docker (개발 완료 후) |

---

## 🔍 레거시 문서 vs 실제 검토 결과 (2026-01-17)

> 전문가 에이전트 검토 완료 - 레거시 RFC 문서 대비 실제 필요 작업량 약 20%

### BE (백엔드)

| 항목 | 레거시 | 실제 | 판정 | 비고 |
|------|:------:|:----:|:----:|------|
| !! 연산자 | 46건 | 10건 | ❌ 불필요 | null 체크 직후 사용, 안전 |
| subscribe() 핸들러 | 25건 | 0건 | ❌ 불필요 | **모두 에러 핸들러 있음** |
| mutableListOf | 65건 | 1건 | ❌ 불필요 | 대부분 로컬 변수/synchronized |
| println | 102건 | 68건 | ⚠️ 선택적 | ICDService 에러 로그만 logger 권장 |
| Thread.sleep | 2건 | 1건 | ⚠️ 선택적 | BatchStorageManager (100ms) |
| runBlocking | 1건 | 0건 | ✅ 완료 | 제거됨 |

### FE (프론트엔드)

| 항목 | 레거시 | 실제 | 판정 | 비고 |
|------|:------:|:----:|:----:|------|
| Offset Control 분산 | 3곳 | 0곳 | ✅ 완료 | **useOffsetControls로 통합됨** |
| 대형 파일 분리 | 5개 | 0개 긴급 | ❌ 불필요 | 이미 컴포넌트/composable 분리됨 |
| 하드코딩 색상 | 304건 | 50~80건 | ⚠️ 선택적 | 차트 예외, UI만 대상 |
| as 타입 단언 | 99건 | 20~30건 | ⚠️ 선택적 | icdStore WebSocket만 Type Guard 권장 |

### 선택적 개선 (필수 아님)

| 항목 | 작업량 | 우선순위 | 비고 |
|------|:------:|:--------:|------|
| ICDService println → logger | 68건 | P2 | 통신 오류 진단용 |
| UI 하드코딩 색상 | 50~80건 | P3 | CLAUDE.md 규칙 |
| icdStore Type Guard | 20~30건 | P3 | 안정성 개선 |
| Thread.sleep 1건 | 1건 | P3 | BatchStorageManager |
| GlobalData AtomicReference | 18필드 | P3 | @Volatile로 충분 |

---

## 난이도 범례

| 표시 | 난이도 | 설명 |
|:----:|:------:|------|
| 🟢 | 쉬움 | 패턴 적용, 단순 수정 |
| 🟡 | 중간 | 분석 필요, 테스트 필수 |
| 🔴 | 높음 | 영향 범위 큼, 주의 필요 |

---

## Phase 1: BE 안정성 (남은 작업: P1-2~6)

> P1-1 !! 연산자 제거 완료 (2026-01-17)

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

### P1-5. subscribe() 에러 핸들러 ✅ 완료 (검토 결과)

> **2026-01-17 전문가 검토**: 모든 25개 subscribe() 호출에 에러 핸들러가 **이미 있음** 확인

```kotlin
// 실제 코드 확인 결과 - 모두 이미 에러 핸들러 있음
.subscribe(
    { /* 성공 */ },
    { error -> logger.error("처리 오류: {}", error.message, error) }
)
```

**추가 작업 불필요**

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

## Phase 6: 키보드 단축키 🟢

> useKeyboardNavigation.ts 이미 생성됨 - 적용만 필요

### P6-1. useKeyboardNavigation composable ✅ 완료

**파일**: `frontend/src/composables/useKeyboardNavigation.ts` (255줄)

**제공 기능**:
- `onEscape()` - ESC 키 바인딩
- `onEnter()` - Enter 키 바인딩
- `onCtrlEnter()` - Ctrl+Enter 바인딩
- `onF5()` - F5 키 바인딩 (새로고침 방지)
- `bind()` - 커스텀 키 바인딩

### P6-2. 모달/다이얼로그에 적용

**적용 대상**:
- 모든 `q-dialog` 컴포넌트 → ESC로 닫기
- 확인 다이얼로그 → Enter로 확인

```vue
<script setup>
import { useKeyboardNavigation } from '@/composables/useKeyboardNavigation'

const dialogVisible = ref(false)

const { onEscape, onCtrlEnter } = useKeyboardNavigation({
  enabled: dialogVisible
})

onEscape(() => { dialogVisible.value = false })
onCtrlEnter(() => { submitForm() })
</script>
```

---

## 별도: DB 설계 (RFC-001) 🔴

> **사용자 검토 필요** - 시작 전 사용자와 함께 검토

**문서**: `docs/work/active/Architecture_Refactoring/legacy/RFC-001_Database_Strategy.md`

**설계 완료 항목**:
- PostgreSQL 16 + TimescaleDB
- 4개 테이블: tracking_master, tracking_detail, realtime_result, icd_realtime

**실행 순서**:
```
1. 사용자와 설계 검토
   ↓
2. PostgreSQL + TimescaleDB 설치
   ↓
3. 테이블 생성 (4개)
   ↓
4. Repository 레이어 추가
   ↓
5. Service에 저장 로직 연동
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

| 항목 | 설명 |
|------|------|
| 장기-1 | 테스트 추가 (BE/FE) |
| 장기-2 | 인증/인가 (Spring Security) |
| 장기-3 | Docker 컨테이너화 |
| 장기-4 | CI/CD 파이프라인 |

---

## 보류 항목 (Backlog)

| ID | 항목 | 설명 |
|----|------|------|
| BL-1 | 로깅 유틸리티 연계 | console.log → logger.ts 교체 (선택적) |
| BL-2 | Settings 실시간 제어 분리 | 메인터넌스 기능 UI 분리 |
| BL-4 | 코드 품질 Cleanup | 하드코딩 색상, 중복 로직 정리 |

---

## References

- [TRACKER.md](./TRACKER.md) - 완료 기록 및 테스트 체크리스트
- [legacy/](./legacy/) - 기존 RFC 문서들

---

**Version**: 4.0.0
**Last Updated**: 2026-01-17
