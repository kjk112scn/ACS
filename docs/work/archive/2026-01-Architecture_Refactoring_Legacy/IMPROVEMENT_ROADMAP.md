# ACS 종합 개선 로드맵

> **작성일**: 2026-01-15
> **기반**: 7개 전문가 관점 분석 결과

---

## Executive Summary

| 전문가 영역 | 현재 수준 | 목표 수준 | 시점 |
|-------------|-----------|-----------|------|
| 성능 | 양호 | 최적화 | **현재** |
| 아키텍처 | 6.8/10 | 8.5/10 | **현재** |
| 테스트/품질 | 2 tests | 60% coverage | 중기 |
| UX/접근성 | 부분적 | WCAG Level A | 중기 |
| 도메인 알고리즘 | 4.0/5 | 4.5/5 | 중기 |
| 보안 | Critical | Production Ready | **장기** |
| DevOps | 42/100 | 80/100 | **장기** |

---

## Phase 1: 성능 최적화 (현재)

### 1.1 Frontend shallowRef 적용

**현재**: icdStore.ts에서 `ref()` 81개 사용

**최적화 대상**:
```typescript
// 변경 전
const antennaPosition = ref<AntennaPosition>({ az: 0, el: 0, train: 0 })

// 변경 후
const antennaPosition = shallowRef<AntennaPosition>({ az: 0, el: 0, train: 0 })
```

**적용 대상 ref**:
| 카테고리 | ref 수 | shallowRef 전환 |
|----------|--------|-----------------|
| 객체 타입 | 23 | 전체 |
| 배열 타입 | 8 | 전체 |
| 원시 타입 | 50 | 유지 |

---

### 1.2 deep watch 제거

**Critical**:
| File | Line | Issue |
|------|------|-------|
| PassSchedulePage.vue | 1209 | **무한 루프 위험** - 즉시 수정 |

**수정 방법**:
```typescript
// 변경 전
watch(scheduleData, handler, { deep: true })

// 변경 후 - 특정 속성만 감시
watch(() => scheduleData.value.status, handler)
// 또는 computed 사용
const scheduleStatus = computed(() => scheduleData.value.status)
watch(scheduleStatus, handler)
```

---

### 1.3 Backend 동시성 수정

**GlobalData.kt 수정**:
```kotlin
// 변경 전
object GlobalData {
    var serverTimeZone: String = "UTC"  // Thread-unsafe
}

// 변경 후
object GlobalData {
    @Volatile
    private var _serverTimeZone = AtomicReference("UTC")

    var serverTimeZone: String
        get() = _serverTimeZone.get()
        set(value) = _serverTimeZone.set(value)
}
```

---

### 1.4 Thread.sleep 제거

| File | Line | 변경 |
|------|------|------|
| UdpFwICDService.kt | 1109 | `Mono.delay(Duration.ofSeconds(1))` |
| BatchStorageManager.kt | 294 | `Mono.delay(Duration.ofMillis(100))` |

---

## Phase 2: 아키텍처 개선 (현재)

### 2.1 대형 파일 분리

**우선순위 1 - EphemerisService.kt (5,057줄)**:
```
EphemerisService.kt
    ↓ 분할
├── EphemerisService.kt (~1,500줄) - 오케스트레이션
├── EphemerisTrackingStateMachine.kt (~1,000줄) - 상태 전이
├── EphemerisTLEManager.kt (~500줄) - TLE 캐시
├── EphemerisDataBatcher.kt (~500줄) - 배치 저장
└── EphemerisCommandSender.kt (~800줄) - UDP 명령
```

**우선순위 2 - PassScheduleService.kt (3,846줄)**:
```
PassScheduleService.kt
    ↓ 분할
├── PassScheduleService.kt (~1,200줄) - 스케줄 관리
├── PassScheduleStateMachine.kt (~800줄) - 상태 전이
├── PassScheduleMonitor.kt (~600줄) - 타이머 모니터링
└── PassScheduleTracker.kt (~700줄) - 실시간 추적
```

---

### 2.2 Frontend 컴포넌트 분리

**PassSchedulePage.vue (4,838줄)**:
```
PassSchedulePage.vue
    ↓ 분할
├── PassSchedulePage.vue (~1,500줄) - 레이아웃
├── ScheduleTable.vue (~500줄) - 테이블
├── ScheduleInfoPanel.vue (~300줄) - 정보 패널
├── ScheduleChart.vue (~400줄) - 차트
├── ScheduleControls.vue (~300줄) - 제어
└── usePassScheduleTracking.ts (~600줄) - Composable
```

---

### 2.3 Store 분리

**icdStore.ts (2,971줄)**:
```
stores/icd/
├── index.ts (~100줄) - re-export
├── icdStore.ts (~800줄) - 핵심 상태
├── icdAntennaState.ts (~600줄) - 안테나 상태
├── icdBoardStatus.ts (~700줄) - 보드 상태
└── icdTrackingState.ts (~400줄) - 추적 상태
```

---

## Phase 3: 테스트 인프라 구축 (중기)

### 3.1 Backend 테스트

**현재**: 2개 테스트 파일만 존재

**목표 구조**:
```
backend/src/test/kotlin/
├── service/
│   ├── EphemerisServiceTest.kt
│   ├── PassScheduleServiceTest.kt
│   └── ICDServiceTest.kt
├── algorithm/
│   ├── OrekitCalculatorTest.kt (기존)
│   ├── LimitAngleCalculatorTest.kt
│   └── CoordinateTransformerTest.kt
├── controller/
│   └── EphemerisControllerTest.kt
└── integration/
    └── TrackingFlowIntegrationTest.kt
```

**우선 작성 테스트**:
| 파일 | 테스트 케이스 | 우선순위 |
|------|--------------|----------|
| EphemerisServiceTest.kt | 상태 전이 검증 | High |
| PassScheduleServiceTest.kt | 스케줄 CRUD | High |
| CoordinateTransformerTest.kt | 좌표 변환 정확도 | High |

---

### 3.2 Frontend 테스트

**현재**: 0개

**Vitest 설정**:
```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    coverage: {
      reporter: ['text', 'html'],
      exclude: ['node_modules/', 'src/**/*.d.ts']
    }
  }
})
```

**우선 작성 테스트**:
| 파일 | 테스트 대상 | 우선순위 |
|------|------------|----------|
| icdStore.spec.ts | WebSocket 상태 관리 | High |
| passScheduleStore.spec.ts | 스케줄 상태 | High |
| useAxisFormatter.spec.ts | 각도 포맷팅 | Medium |

---

### 3.3 테스트 커버리지 목표

| 단계 | Backend | Frontend | 전체 |
|------|---------|----------|------|
| 초기 | 30% | 20% | 25% |
| 중기 | 50% | 40% | 45% |
| 목표 | 60% | 50% | 55% |

---

## Phase 4: UX/접근성 개선 (중기)

### 4.1 aria 속성 추가

```vue
<!-- 변경 전 -->
<button @click="startTracking">Start</button>

<!-- 변경 후 -->
<button
  @click="startTracking"
  :aria-label="$t('tracking.start')"
  :aria-disabled="isDisabled"
  role="button"
>
  Start
</button>
```

---

### 4.2 키보드 네비게이션

```typescript
// composables/useKeyboardNavigation.ts
export function useKeyboardNavigation() {
  const handleKeyDown = (e: KeyboardEvent) => {
    switch (e.key) {
      case 'Escape':
        closeModal()
        break
      case 'Enter':
        if (e.ctrlKey) submitForm()
        break
    }
  }

  onMounted(() => document.addEventListener('keydown', handleKeyDown))
  onUnmounted(() => document.removeEventListener('keydown', handleKeyDown))
}
```

---

### 4.3 색상 대비 검증

| 요소 | 현재 대비 | WCAG AA (4.5:1) |
|------|----------|-----------------|
| 일반 텍스트 | 확인 필요 | 4.5:1 이상 |
| 큰 텍스트 | 확인 필요 | 3:1 이상 |
| UI 컴포넌트 | 확인 필요 | 3:1 이상 |

---

## Phase 5: 도메인 알고리즘 개선 (중기)

### 5.1 태양 대기 굴절 보정

```kotlin
// SunTrackService.kt 개선
fun calculateRefraction(elevation: Double): Double {
    // 대기 굴절 보정 (1/tan 공식)
    if (elevation < 0) return 0.0
    val elevationDeg = Math.toDegrees(elevation)
    return 1.0 / Math.tan(Math.toRadians(elevationDeg + 7.31 / (elevationDeg + 4.4)))
}
```

---

### 5.2 TLE 유효성 검증 강화

```kotlin
fun validateTLE(tle: TLE): ValidationResult {
    val issues = mutableListOf<String>()

    // Epoch 검증
    val age = ChronoUnit.DAYS.between(tle.epoch, ZonedDateTime.now())
    if (age > 14) issues.add("TLE is ${age} days old (> 14 days)")

    // 궤도 요소 검증
    if (tle.eccentricity >= 1.0) issues.add("Invalid eccentricity: ${tle.eccentricity}")
    if (tle.meanMotion <= 0) issues.add("Invalid mean motion: ${tle.meanMotion}")

    return ValidationResult(issues.isEmpty(), issues)
}
```

---

## Phase 6: 코드 품질 도구 (중기)

### 6.1 Backend - ktlint

```kotlin
// build.gradle.kts
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

ktlint {
    version.set("1.1.1")
    android.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
}
```

### 6.2 Frontend - ESLint 강화

```json
// .eslintrc.json 추가 규칙
{
  "rules": {
    "no-console": "warn",
    "@typescript-eslint/no-explicit-any": "error",
    "vue/multi-word-component-names": "error"
  }
}
```

---

# 장기 (개발 완료 후)

> 아래 항목들은 핵심 기능 개발이 완료된 후 진행합니다.

---

## Long-Term 1: 보안 강화

### LT1.1 인증/인가 도입

**현재 문제**: 모든 API 엔드포인트가 인증 없이 접근 가능

```kotlin
// 필요한 구현
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }  // SPA용
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

**작업 항목**:
| Task | File |
|------|------|
| Spring Security 의존성 추가 | build.gradle.kts |
| SecurityConfig 생성 | config/SecurityConfig.kt |
| JWT 토큰 검증 구현 | - |
| 로그인 API 구현 | AuthController.kt |

---

### LT1.2 하드코딩 자격증명 제거

```kotlin
// 현재 (위험)
private val DB_PASSWORD = "admin123"

// 개선 (환경변수)
@Value("\${db.password}")
private lateinit var dbPassword: String
```

**작업 항목**:
| Task | Action |
|------|--------|
| 하드코딩 비밀번호 검색 | `grep -r "password\|secret\|key" backend/` |
| application.yml 외부화 | 환경변수 참조로 변경 |
| .env.example 생성 | 필수 환경변수 문서화 |

---

### LT1.3 CORS Wildcard 제거

**현재**: `CorsConfig.kt:26` - `allowedOrigins("*")`

```kotlin
// 수정
.allowedOrigins(
    "http://localhost:9000",
    "http://192.168.x.x:9000"  // 실제 운영 IP
)
```

---

### LT1.4 Path Traversal 수정

**현재**: `LoggingController.kt:172-173`

```kotlin
// 수정 코드
val normalizedPath = Paths.get(LOGS_DIRECTORY, fileName).normalize()
if (!normalizedPath.startsWith(Paths.get(LOGS_DIRECTORY).normalize())) {
    throw IllegalArgumentException("Invalid file path")
}
```

---

## Long-Term 2: Docker 컨테이너화

### LT2.1 Backend Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/acs-backend-*.jar app.jar
COPY orekit-data /app/orekit-data
ENV OREKIT_DATA_PATH=/app/orekit-data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### LT2.2 Frontend Dockerfile

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist/spa /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
```

### LT2.3 docker-compose.yml

```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    volumes:
      - ./orekit-data:/app/orekit-data

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
```

---

## Long-Term 3: CI/CD 파이프라인 (GitLab 전환 시)

> GitHub Actions 대신 회사 GitLab으로 전환 시 적용

### LT3.1 GitLab CI 예시

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

## 실행 일정 요약

| Phase | 작업 | 시점 |
|-------|------|------|
| **Phase 1** | 성능 최적화 | 현재 |
| **Phase 2** | 아키텍처 개선 | 현재 |
| **Phase 3** | 테스트 인프라 | 중기 |
| **Phase 4** | UX/접근성 | 중기 |
| **Phase 5** | 알고리즘 개선 | 중기 |
| **Phase 6** | 코드 품질 도구 | 중기 |
| **LT1** | 보안 강화 | **개발 완료 후** |
| **LT2** | Docker | **개발 완료 후** |
| **LT3** | CI/CD | **GitLab 전환 시** |

---

## 완료 기준 (Definition of Done)

### 현재 Phase 완료 조건
- [ ] shallowRef 적용 완료
- [ ] deep watch 무한루프 수정
- [ ] 대형 파일 2,000줄 이하 분리

### 중기 Phase 완료 조건
- [ ] 테스트 커버리지 55%+
- [ ] aria 속성 추가
- [ ] 알고리즘 개선

### 장기 완료 조건 (개발 완료 후)
- [ ] 모든 API 인증 필요
- [ ] 하드코딩 자격증명 0건
- [ ] Docker 이미지 빌드 성공
- [ ] CI/CD 파이프라인 동작

---

## 참고: XSS 관련 검토 결과

`windowUtils.ts`의 innerHTML 사용은 분석 결과 **실제 보안 위험 없음**으로 확인됨:
- 삽입되는 값들이 모두 `components.ts`에 하드코딩된 상수
- 사용자 입력이 전혀 들어가지 않음
- 코드 품질 개선 시 Vue 컴포넌트로 리팩토링 권장 (선택사항)

---

**문서 버전**: 2.0.0
**작성자**: 7-Expert Analysis Integration
**최종 검토**: 2026-01-15
