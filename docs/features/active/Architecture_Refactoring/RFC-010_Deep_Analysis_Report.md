# RFC-010: 심층 분석 보고서 (기존 RFC 보완)

> **버전**: 1.0.0 | **작성일**: 2026-01-13
> **목적**: 기존 RFC-001~009에서 누락된 이슈 식별 및 보완

---

## 1. 보안 취약점 (Critical - 기존 RFC-004 보완)

### 1.1 CORS 설정 과도하게 허용 (**Critical**)

**파일**: `backend/src/main/kotlin/.../config/CorsConfig.kt:21-27`

```kotlin
allowedOriginPatterns = listOf(
    "http://localhost:9000",
    "http://127.0.0.1:9000",
    "http://localhost:*",
    "http://127.0.0.1:*",
    "*"  // ← Critical: 모든 Origin 허용!
)
```

| 이슈 | 현재 상태 | 권장 |
|------|----------|------|
| 와일드카드 Origin | `"*"` 허용 | 프로덕션에서 제거 필수 |
| 개발 모드 분리 | 없음 | Profile 별 CORS 설정 |

### 1.2 XSS 취약점 - innerHTML 사용 (**High**)

**파일**: `frontend/src/utils/windowUtils.ts`

| 라인 | 코드 | 위험도 |
|------|------|--------|
| 709 | `button.innerHTML = ...` | High |
| 821 | `title.innerHTML = ...` | High |
| 847 | `popupButton.innerHTML = ...` | High |
| 869 | `modalButton.innerHTML = ...` | High |

**권장**: `textContent` 또는 `createElement` + `appendChild` 사용

### 1.3 입력 검증 전무 (**Critical**)

**현재 상태**:
- `@RequestBody/@RequestParam` 사용: **106건**
- `@Valid/@Validated` 사용: **0건**
- `@NotNull/@Min/@Max/@Size` 사용: **0건**

**영향받는 컨트롤러**:
| Controller | API 엔드포인트 수 | 검증 | 상태 |
|------------|------------------|------|------|
| ICDController | 38개 | 0 | Critical |
| EphemerisController | 21개 | 0 | Critical |
| PassScheduleController | 18개 | 0 | Critical |
| SettingsController | 13개 | 0 | Critical |
| HardwareErrorLogController | 9개 | 0 | High |
| SunTrackController | 4개 | 0 | High |
| LoggingController | 3개 | 0 | Medium |

**권장 조치**:
```kotlin
// 변경 전
@PostMapping("/schedule")
fun createSchedule(@RequestBody dto: ScheduleDto): Mono<...>

// 변경 후
@PostMapping("/schedule")
fun createSchedule(@Valid @RequestBody dto: ScheduleDto): Mono<...>
```

### 1.4 인증 가드 부재 (**Critical**)

**파일**: `frontend/src/router/routes.ts`, `frontend/src/router/index.ts`

| 이슈 | 현재 | 권장 |
|------|------|------|
| beforeEnter 가드 | 0건 | 모든 보호 라우트에 적용 |
| 인증 체크 | 없음 | JWT/세션 검증 |
| 권한 체크 | 없음 | 역할 기반 접근 제어 |

**현재 상태**: `/dashboard/*` 모든 페이지가 인증 없이 접근 가능

---

## 2. 비동기 처리 불일치 (기존 RFC-003, RFC-004 보완)

### 2.1 WebFlux 프로젝트에서 suspend 함수 미활용 (**High**)

| 지표 | 값 | 비고 |
|------|-----|------|
| 총 Controller 메서드 | 142개 | |
| suspend fun 사용 | 3개 | 2.1% |
| 일반 fun 사용 | 139개 | 97.9% |

**영향**: WebFlux의 Non-blocking I/O 장점을 살리지 못함

**권장**: 점진적으로 suspend fun으로 전환 (RFC-004 Phase 2와 연계)

### 2.2 sealed class 미활용 (**Medium**)

**현재**: `ACSEvent.kt` 1개 파일에서만 사용

**부재 영역**:
- API 응답 결과 (Success/Error)
- 상태 표현 (TrackingState, ScheduleState)
- 오류 유형 분류

---

## 3. 메모리 관리 (기존 RFC-008 보완)

### 3.1 타이머/이벤트 정리 불균형 (**High**)

| 항목 | 건수 | 비고 |
|------|------|------|
| setInterval/setTimeout 사용 | **66건** | 28개 파일 |
| onBeforeUnmount/onUnmounted | **21건** | 10개 파일 |
| 정리 비율 | ~32% | **68%가 미정리 가능성** |

**주요 위험 파일**:
| 파일 | 타이머 | 정리 | 차이 |
|------|--------|------|------|
| PassSchedulePage.vue | 13 | 3 | -10 |
| EphemerisDesignationPage.vue | 6 | 2 | -4 |
| passScheduleStore.ts | 5 | 0 | -5 |
| DashboardPage.vue | 4 | 2 | -2 |

### 3.2 shallowRef 미사용 (**Critical**)

**현재**: `shallowRef` 사용 **0건**

**영향**: icdStore의 175개 ref가 각각 깊은 반응성 추적
- 30ms마다 175개 객체의 전체 깊이 추적
- CPU 사용률 증가의 근본 원인

**권장** (RFC-008 Phase 5 보완):
```typescript
// 변경 전
const azimuthAngle = ref(0)

// 변경 후
const azimuthAngle = shallowRef(0)
// 또는 그룹화
const axisData = shallowRef<AxisData>({ azimuth: 0, elevation: 0, train: 0 })
```

### 3.3 addEventListener 정리 누락

| 항목 | 건수 |
|------|------|
| addEventListener 사용 | 22건 |
| removeEventListener 확인 필요 | 6개 파일 |

---

## 4. 테스트 인프라 부재 (RFC-005 작성 필요)

### 4.1 현재 테스트 상태

| 영역 | 파일 수 | 상태 |
|------|---------|------|
| Backend 테스트 | 1개 | `AcsApiApplicationTests.kt` (기본 컨텍스트 테스트) |
| Frontend 테스트 | 0개 | 테스트 프레임워크 미설정 |
| E2E 테스트 | 0개 | 없음 |

### 4.2 테스트 불가능 코드 패턴

| 패턴 | 위치 | 문제 |
|------|------|------|
| 테스트 코드 main 혼재 | `OrekitCalcuatorTest.kt` | 595줄 (이미 RFC-003에서 식별) |
| static 의존성 | GlobalData 접근 | Mocking 어려움 |
| 하드코딩된 경로 | Orekit 초기화 | 환경 의존성 |

---

## 5. 의존성 문제 (신규)

### 5.1 중복 차트 라이브러리 (**High**)

**파일**: `frontend/package.json`

| 라이브러리 | 크기 | 상태 |
|------------|------|------|
| echarts | ~500KB | 사용 중 |
| chart.js | ~200KB | 미사용 또는 최소 사용 |
| vue-chartjs | ~50KB | chart.js 래퍼 |

**낭비**: 약 250KB (압축 시 ~80KB)

**권장**: chart.js/vue-chartjs 제거, echarts로 통일

### 5.2 JPA + WebFlux 혼용 (**Medium**)

**파일**: `backend/build.gradle.kts`

```kotlin
implementation("org.springframework.boot:spring-boot-starter-webflux")  // 비동기
implementation("org.springframework.boot:spring-boot-starter-data-jpa") // 블로킹
```

| 문제 | 영향 |
|------|------|
| JPA는 블로킹 API | WebFlux의 Non-blocking 이점 상쇄 |
| 혼용 시 스레드 풀 고갈 가능 | 고부하 시 성능 저하 |

**권장**: R2DBC로 전환 (RFC-001과 연계)

---

## 6. 에러 핸들링 보완 (RFC-007 보완)

### 6.1 GlobalExceptionHandler 단순화 (**Medium**)

**현재** (`GlobalExceptionHandler.kt`):
- `Exception` 핸들러: 1개
- `ResponseStatusException` 핸들러: 1개
- 비즈니스 예외: 0개

**부재 항목**:
| 예외 유형 | 필요성 | 권장 |
|----------|--------|------|
| ValidationException | 입력 검증 실패 | 400 응답 |
| NotFoundException | 리소스 없음 | 404 응답 |
| ConflictException | 상태 충돌 | 409 응답 |
| UnauthorizedException | 인증 실패 | 401 응답 |
| ForbiddenException | 권한 없음 | 403 응답 |
| ICDCommunicationException | ICD 통신 오류 | 503 응답 |

### 6.2 복원력 패턴 (신규)

**retry 패턴 사용**: 3개 파일에서 부분적 사용
- `UdpFwICDService.kt`
- `EphemerisService.kt`
- `PassScheduleService.kt`

**부재**:
| 패턴 | 현재 | 권장 |
|------|------|------|
| Circuit Breaker | 없음 | ICD/외부 통신에 적용 |
| Timeout | 부분적 | 모든 외부 호출에 적용 |
| Fallback | 없음 | 핵심 기능에 적용 |

---

## 7. 국제화 상태 (신규)

### 7.1 i18n 설정 현황

| 항목 | 상태 |
|------|------|
| vue-i18n 설정 | 존재 |
| ko-KR 번역 | 656줄 (상당히 완성) |
| en-US 번역 | 존재 (확인 필요) |

### 7.2 하드코딩 문자열 잔존

**FE 컴포넌트에서 하드코딩된 한글**: 다수 존재 (별도 조사 필요)

**권장**: i18n 적용률 점검 후 점진적 전환

---

## 8. 라우팅 구조 (RFC-008 보완)

### 8.1 현재 라우트 구조

```
/login          → LoginPage
/dashboard/
  ├── standby   → StandbyPage
  ├── step      → StepPage
  ├── slew      → SlewPage
  ├── pedestal  → PedestalPositionPage
  ├── ephemeris → EphemerisDesignationPage (4,340줄)
  ├── pass-schedule → PassSchedulePage (4,838줄)
  ├── suntrack  → SunTrackPage
  └── feed      → FeedPage
/popup/*        → 팝업 컴포넌트들
```

### 8.2 개선 필요 사항

| 이슈 | 현재 | 권장 |
|------|------|------|
| 레이지 로딩 | 적용됨 | 유지 |
| 인증 가드 | 없음 | 추가 필요 |
| 404 페이지 | 있음 | 유지 |
| 대형 페이지 분리 | 없음 | RFC-008 Phase 2 |

---

## 9. 우선순위 매트릭스 (기존 RFC 보완 통합)

### 9.1 즉시 조치 필요 (Sprint 0 추가)

| 이슈 | RFC | 예상 시간 |
|------|-----|----------|
| CORS `"*"` 제거 | RFC-004 | 10분 |
| innerHTML → textContent | RFC-008 | 30분 |
| @Valid 추가 (필수 API만) | RFC-007 | 2시간 |

### 9.2 신규 RFC 작성 필요

| RFC# | 제목 | 우선순위 |
|------|------|:--------:|
| RFC-005 | 테스트 전략 | P2 |
| RFC-010 | 보안 강화 | **P0** |

### 9.3 기존 RFC 보완 항목

| RFC | 추가 항목 |
|-----|----------|
| RFC-003 | sealed class 활용, suspend fun 전환 |
| RFC-004 | CORS 설정, 입력 검증 |
| RFC-007 | 비즈니스 예외 클래스, 복원력 패턴 |
| RFC-008 | shallowRef 적용, 타이머 정리, 인증 가드 |

---

## 10. 발견사항 요약

### 10.1 심각도별 분류

| 심각도 | 신규 발견 | 기존 RFC 누락 |
|--------|----------|--------------|
| **Critical** | CORS `"*"`, 입력 검증 전무, 인증 가드 부재, shallowRef 0건 | 4건 |
| **High** | innerHTML XSS, 타이머 미정리 68%, suspend 2.1%, chart.js 중복 | 4건 |
| **Medium** | JPA+WebFlux 혼용, sealed class 미활용, GlobalExceptionHandler 단순 | 3건 |

### 10.2 정량적 지표

| 지표 | 현재 | 목표 |
|------|------|------|
| 입력 검증 커버리지 | 0% | 100% |
| suspend fun 비율 | 2.1% | 50%+ |
| 타이머 정리 비율 | 32% | 100% |
| shallowRef 사용 | 0건 | 필요 영역 100% |
| 테스트 커버리지 | ~0% | 50%+ |
| 인증 가드 적용 | 0% | 100% |

---

## 11. 다음 단계

1. **RFC-010 (보안 강화)** 신규 작성
2. **RFC-005 (테스트 전략)** 신규 작성
3. 기존 RFC 업데이트 (위 보완 항목 반영)
4. Execution_Checklist.md 업데이트

---

**작성자**: Claude (17개 전문가 에이전트 병렬 분석)
**검토자**: -
**승인일**: -
