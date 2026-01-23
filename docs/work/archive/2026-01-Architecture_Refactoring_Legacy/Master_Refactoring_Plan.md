# ACS 종합 리팩토링 마스터 플랜

> **버전**: 1.0.0 | **작성일**: 2026-01-13
> **상태**: 분석 완료, 실행 계획 수립 중

---

## 프로젝트 현황 요약

### 코드베이스 규모

| 영역 | 파일 수 | 라인 수 | 상태 |
|------|--------|--------|------|
| **Frontend** | 60+ | 51,824 | 빌드 성공 |
| **Backend** | 66 | 17,297+ (서비스만) | 빌드 성공 |
| **문서** | 50+ | - | 체계화됨 |
| **Total** | 170+ | 70,000+ | 운영 중 |

### 기술 스택

| 계층 | 기술 | 버전 |
|------|------|------|
| **FE Framework** | Vue 3 + Quasar 2 | 3.4.18 / 2.16.0 |
| **FE State** | Pinia | 3.0.2 |
| **FE i18n** | vue-i18n | 9.2.2 |
| **BE Framework** | Spring Boot + WebFlux | 3.4.4 |
| **BE Language** | Kotlin | 1.9.25 |
| **Algorithm** | Orekit + solarpositioning | 13.0.2 / 2.0.3 |
| **DB** | PostgreSQL | - |
| **통신** | REST + WebSocket + UDP | - |

---

## 1. 영역별 현황

### 1.1 Frontend 구조

```
frontend/src/ (51,824줄)
├── pages/           14,661줄  # 9개 페이지
├── components/       9,456줄  # 28개 컴포넌트
├── stores/           8,610줄  # 25개 스토어
├── services/         3,122줄  # API 서비스
├── composables/      1,614줄  # 8개 훅
├── i18n/             1,342줄  # 다국어 (한/영)
├── css/             10,735줄  # 스타일
└── types/              351줄  # 타입 정의
```

**핵심 파일 (라인 수 기준)**:

| 파일 | 라인 수 | 역할 | 문제점 |
|------|--------|------|--------|
| PassSchedulePage.vue | 4,838 | 패스 스케줄 UI | 너무 큼, 분리 필요 |
| EphemerisDesignationPage.vue | 4,340 | TLE/위성 지정 | 너무 큼, 분리 필요 |
| icdStore.ts | 2,971 | 실시간 ICD 데이터 | 100+ ref, 구조화 필요 |
| passScheduleStore.ts | 2,452 | 패스 스케줄 상태 | 비즈니스 로직 과다 |
| DashboardPage.vue | 2,728 | 대시보드 | 적정 |
| FeedPage.vue | 2,531 | Feed 제어 | 적정 |

### 1.2 Backend 구조

```
backend/src/main/kotlin/ (66파일)
├── controller/      5,994줄  # 97개 API
├── service/        17,297줄  # 13개 서비스
├── algorithm/       5,224줄  # 13개 알고리즘
├── config/            586줄  # 8개 설정
├── dto/               200줄  # 3개 DTO
├── model/             300줄  # 3개 모델
└── 기타               500줄  # event, util, openapi
```

**핵심 파일 (라인 수 기준)**:

| 파일 | 라인 수 | 메서드 수 | 문제점 |
|------|--------|----------|--------|
| EphemerisService.kt | 5,057 | 95 | 초대형, 상태머신 중복 |
| PassScheduleService.kt | 3,846 | 112 | 초대형, 상태머신 중복 |
| ICDService.kt | 2,788 | - | 대형, 프로토콜 복잡 |
| PassScheduleController.kt | 1,557 | 23 API | 대형 |
| UdpFwICDService.kt | 1,228 | - | 중형 |
| SatelliteTrackingProcessor.kt | 1,387 | - | 중형 |

---

## 2. 다국어 지원 (i18n) 현황

### 2.1 Frontend - 완성도 높음

| 항목 | 상태 | 설명 |
|------|------|------|
| **지원 언어** | 한국어(ko-KR), 영어(en-US) | 완전 지원 |
| **라이브러리** | vue-i18n 9.2.2 | 최신 |
| **번역 파일** | ko-KR.ts (656줄), en-US.ts (657줄) | 동등 |
| **저장소** | localStorage (preferred-language) | 유지됨 |
| **UI 변경** | LanguageSettings.vue | 드롭다운 |
| **개발 도구** | i18n Ally (VSCode) | 지원 |

**번역 범위**:
```typescript
// 번역 키 구조
{
  common: { ... },      // 공통 UI
  login: { ... },       // 로그인
  dashboard: { ... },   // 대시보드
  modes: {              // 모드별
    standby, step, slew, pedestal,
    ephemeris, passSchedule, sunTrack, feed
  },
  settings: { ... },    // 설정
  errors: { ... }       // 에러 메시지
}
```

### 2.2 Backend - 부분 지원

| 항목 | 상태 | 설명 |
|------|------|------|
| **API 문서** | 한국어/영어 분리 | Swagger 드롭다운 |
| **에러 메시지** | 한국어/영어 맵 | ErrorMessageConfig.kt |
| **런타임 i18n** | 미구현 | Accept-Language 미처리 |
| **Message Bundle** | 미사용 | properties 파일 없음 |

**개선 필요**:
- Accept-Language 헤더 기반 응답 언어 선택
- messages.properties (ko, en) 도입
- API 응답 메시지 다국어화

---

## 3. 기술 부채 종합

### 3.1 P0: 즉시 수정 (Critical)

| # | 항목 | 영역 | 현황 | 영향 |
|---|------|------|------|------|
| 1 | 상태 머신 중복 | BE | 40% 코드 중복 (2,000줄) | 유지보수 어려움 |
| 2 | 테스트 커버리지 | 전체 | BE 1.5%, FE 0% | 리팩토링 위험 |
| 3 | 초대형 서비스 | BE | 5,057줄, 3,846줄 | 이해 불가 |
| 4 | 초대형 페이지 | FE | 4,838줄, 4,340줄 | 유지보수 어려움 |

### 3.2 P1: 단기 개선 (High)

| # | 항목 | 영역 | 현황 | 영향 |
|---|------|------|------|------|
| 5 | icdStore 구조화 | FE | 100+ ref 변수 | 성능 저하 |
| 6 | 하드코딩 색상 | FE | 400+ 곳 | 테마 변경 불가 |
| 7 | console.log | FE | 990개 | 성능/보안 |
| 8 | API 응답 형식 | BE | 3가지 혼재 | 일관성 없음 |
| 9 | 보안 미구현 | BE | 인증/인가 없음 | 보안 취약 |
| 10 | CI/CD 미구현 | 인프라 | 수동 배포 | 자동화 부재 |

### 3.3 P2: 중기 개선 (Medium)

| # | 항목 | 영역 | 현황 | 영향 |
|---|------|------|------|------|
| 11 | watch 과다 | FE | 61회 | 불필요 연산 |
| 12 | chart.js 중복 | FE | ECharts와 중복 | 번들 200KB |
| 13 | 런타임 i18n | BE | 미구현 | 다국어 제한 |
| 14 | 테스트 위치 | BE | src/main에 테스트 | 비표준 |
| 15 | 환경변수 | 인프라 | 하드코딩 credentials | 보안 |

---

## 4. 영역별 고도화 계획

### 4.1 Frontend 고도화

#### A. 페이지 분리 (P0)

```
PassSchedulePage.vue (4,838줄)
├── PassScheduleHeader.vue      # 헤더/필터
├── PassScheduleTable.vue       # 테이블
├── PassScheduleChart.vue       # 차트
├── PassScheduleControls.vue    # 제어 버튼
└── composables/
    ├── usePassScheduleData.ts  # 데이터 로직
    └── usePassScheduleChart.ts # 차트 로직
```

#### B. 스토어 구조화 (P1)

```typescript
// Before: icdStore.ts (2,971줄, 100+ ref)
const azimuthAngle = ref('')
const elevationAngle = ref('')
// ... 100개 더

// After: 구조화된 스토어
const antennaState = reactive<AntennaState>({
  angles: { azimuth: 0, elevation: 0, train: 0 },
  speeds: { azimuth: 0, elevation: 0, train: 0 },
  status: { ... }
})

// 또는 스토어 분리
stores/
├── icd/
│   ├── antennaStore.ts      # 안테나 상태
│   ├── motorStore.ts        # 모터 상태
│   └── trackingStore.ts     # 추적 상태
```

#### C. 테마 표준화 (P1)

```scss
// Before: 하드코딩
.chart { color: #ff5722; }

// After: CSS 변수
.chart { color: var(--theme-axis-azimuth); }
```

### 4.2 Backend 고도화

#### A. SatelliteTrackingEngine 추출 (P0)

```kotlin
// Before: EphemerisService, PassScheduleService에 중복
enum class TrackingState { IDLE, PREPARING, WAITING, TRACKING, ... }
// 각각 2,000줄+ 중복

// After: 공통 엔진
package algorithm.tracking

class SatelliteTrackingEngine(
    private val stateManager: TrackingStateManager,
    private val commandSender: TrackingCommandSender,
    private val dataProvider: TrackingDataProvider
) {
    fun startTracking(target: TrackingTarget): TrackingResult
    fun stopTracking(): TrackingResult
    fun getState(): TrackingState
}

// Service에서 사용
class EphemerisService(
    private val trackingEngine: SatelliteTrackingEngine
) {
    fun startEphemerisTracking(...) {
        trackingEngine.startTracking(EphemerisTarget(...))
    }
}
```

#### B. API 응답 표준화 (P1)

```kotlin
// 표준 응답 클래스
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val errorCode: String? = null
)

// 사용
@GetMapping("/example")
fun example(): ResponseEntity<ApiResponse<ExampleData>> {
    return ResponseEntity.ok(
        ApiResponse(
            success = true,
            message = "조회 성공",
            data = service.getData()
        )
    )
}
```

#### C. 런타임 다국어 (P2)

```kotlin
// MessageSource 설정
@Configuration
class MessageConfig {
    @Bean
    fun messageSource(): MessageSource {
        val source = ReloadableResourceBundleMessageSource()
        source.setBasename("classpath:messages")
        source.setDefaultEncoding("UTF-8")
        return source
    }
}

// 사용
@GetMapping("/example")
fun example(
    @RequestHeader("Accept-Language") locale: Locale
): ApiResponse<String> {
    val message = messageSource.getMessage("success.example", null, locale)
    return ApiResponse(success = true, message = message)
}
```

### 4.3 인프라 고도화

#### A. CI/CD 구축 (P1)

```yaml
# .github/workflows/build.yml
name: Build and Test
on: [push, pull_request]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
      - run: ./gradlew clean build

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci && npm run build
```

#### B. 보안 추가 (P1)

```kotlin
// Spring Security + JWT
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("io.jsonwebtoken:jjwt:0.11.5")

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    // JWT 토큰 검증
    // 역할 기반 접근 제어
}
```

---

## 5. 실행 로드맵

### Phase 1: 안전망 구축 (1주)

| 작업 | 설명 | 산출물 |
|------|------|--------|
| 핵심 알고리즘 테스트 | Keyhole, 좌표변환 테스트 | 테스트 코드 |
| 스냅샷 테스트 | 주요 API 응답 스냅샷 | 스냅샷 파일 |
| 문서 정리 | 현재 아키텍처 문서화 | 문서 갱신 |

### Phase 2: 공통 엔진 추출 (2주)

| 작업 | 설명 | 산출물 |
|------|------|--------|
| TrackingEngine 설계 | 인터페이스 정의 | RFC 문서 |
| TrackingEngine 구현 | 공통 로직 추출 | 신규 클래스 |
| 마이그레이션 | 기존 서비스 연동 | 리팩토링 |

### Phase 3: Frontend 구조화 (2주)

| 작업 | 설명 | 산출물 |
|------|------|--------|
| 페이지 분리 | 대형 페이지 컴포넌트화 | 신규 컴포넌트 |
| 스토어 구조화 | icdStore 분리 | 신규 스토어 |
| 테마 표준화 | CSS 변수 적용 | 스타일 정리 |

### Phase 4: 품질 개선 (1주)

| 작업 | 설명 | 산출물 |
|------|------|--------|
| console.log 정리 | 990개 제거/변환 | 코드 정리 |
| API 표준화 | 응답 형식 통일 | 리팩토링 |
| 다국어 완성 | BE 런타임 i18n | 신규 기능 |

### Phase 5: 인프라 (1주)

| 작업 | 설명 | 산출물 |
|------|------|--------|
| CI/CD | GitHub Actions | 워크플로우 |
| 보안 | JWT 인증 | 보안 설정 |
| 환경변수 | credentials 외부화 | 설정 파일 |

---

## 6. 파일별 상세 분석

### 6.1 Frontend 주요 파일

| 파일 | 라인 | 역할 | 개선 필요 | 우선순위 |
|------|------|------|----------|---------|
| PassSchedulePage.vue | 4,838 | 패스 스케줄 UI | 컴포넌트 분리 | P0 |
| EphemerisDesignationPage.vue | 4,340 | TLE/위성 지정 | 컴포넌트 분리 | P0 |
| icdStore.ts | 2,971 | 실시간 데이터 | 구조화 | P1 |
| passScheduleStore.ts | 2,452 | 패스 상태 | 서비스 분리 | P1 |
| DashboardPage.vue | 2,728 | 대시보드 | 유지 | - |
| FeedPage.vue | 2,531 | Feed 제어 | 유지 | - |
| SelectScheduleContent.vue | 2,324 | 스케줄 선택 | 유지 | - |
| AllStatusContent.vue | 2,381 | 상태 표시 | 유지 | - |
| TLEUploadContent.vue | 1,678 | TLE 업로드 | 유지 | - |
| ephemerisTrackStore.ts | 1,300 | 위성 추적 | 유지 | - |
| SunTrackPage.vue | 1,289 | 태양 추적 | 유지 | - |
| ephemerisTrackService.ts | 1,192 | 위성 서비스 | 유지 | - |
| passScheduleService.ts | 1,117 | 패스 서비스 | 유지 | - |
| useTheme.ts | 872 | 테마 관리 | 분리 고려 | P2 |
| icdService.ts | 814 | ICD 통신 | 유지 | - |
| settingsStore.ts | 786 | 설정 통합 | 유지 | - |

### 6.2 Backend 주요 파일

| 파일 | 라인 | 메서드 | 개선 필요 | 우선순위 |
|------|------|--------|----------|---------|
| EphemerisService.kt | 5,057 | 95 | 엔진 추출 | P0 |
| PassScheduleService.kt | 3,846 | 112 | 엔진 추출 | P0 |
| ICDService.kt | 2,788 | - | 문서화 | P2 |
| PassScheduleController.kt | 1,557 | 23 | API 정리 완료 | 완료 |
| SatelliteTrackingProcessor.kt | 1,387 | - | 유지 | - |
| UdpFwICDService.kt | 1,228 | - | 정리 완료 | 완료 |
| SettingsService.kt | 1,183 | - | 유지 | - |
| EphemerisController.kt | 1,091 | 16 | 유지 | - |
| SunTrackService.kt | 979 | - | 유지 | - |
| SolarOrekitCalculator.kt | 890 | - | 유지 | - |
| SettingsController.kt | 788 | 24 | 유지 | - |
| LimitAngleCalculator.kt | 738 | - | 문서화 | P2 |
| ICDController.kt | 710 | 15 | 유지 | - |
| OrekitCalculator.kt | 627 | - | 유지 | - |
| DataStoreService.kt | 621 | - | 유지 | - |
| HardwareErrorLogService.kt | 624 | - | 유지 | - |
| ThreadManager.kt | 586 | - | 유지 | - |

---

## 7. 성공 기준

### Phase 완료 기준

| Phase | 기준 |
|-------|------|
| Phase 1 | 핵심 알고리즘 테스트 100% 통과 |
| Phase 2 | EphemerisService, PassScheduleService 각 3,000줄 이하 |
| Phase 3 | 페이지 3,500줄 이하, icdStore 2,000줄 이하 |
| Phase 4 | console.log 0개, API 응답 형식 1가지 |
| Phase 5 | CI/CD 자동화, JWT 인증 동작 |

### 최종 목표

| 지표 | 현재 | 목표 |
|------|------|------|
| 테스트 커버리지 | 1.5% | 50%+ |
| 최대 파일 크기 | 5,057줄 | 2,000줄 |
| 코드 중복률 | 40% | 10% |
| console.log | 990개 | 0개 |
| API 응답 형식 | 3가지 | 1가지 |
| 하드코딩 색상 | 400+ | 0개 |

---

## 8. 관련 문서

| 문서 | 역할 | 상태 |
|------|------|------|
| [Refactoring_Execution_Summary.md](./Refactoring_Execution_Summary.md) | 실행 진행 상황 | 활성 |
| [RFC_SatelliteTrackingEngine.md](./RFC_SatelliteTrackingEngine.md) | 엔진 추출 상세 | 활성 |
| [Expert_Analysis_Report.md](./Expert_Analysis_Report.md) | 전문가 분석 | 참조 |
| [Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md) | BE 파일 목록 | 참조 |
| [Frontend_Refactoring_plan.md](./Frontend_Refactoring_plan.md) | FE 파일 목록 | 참조 |

---

**작성자**: Claude
**분석 기준**: FE/BE/인프라 전체 코드베이스 분석
**다음 단계**: Phase 1 (안전망 구축) 착수 결정 필요
