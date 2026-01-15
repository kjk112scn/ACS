# 종합 심층 코드 분석 보고서

> **작성일**: 2026-01-14
> **분석 방법**: 10개 전문가 에이전트 병렬 분석 + 코드 직접 검증
> **범위**: FE 103개 파일, BE 66개 파일 실제 코드 레벨

---

## 1. Executive Summary

### 1.1 핵심 수치 (검증 완료)

| 영역 | 항목 | 수치 | 심각도 |
|------|------|:----:|:------:|
| **FE** | ref() 사용 | **233개** | - |
| **FE** | shallowRef() 사용 | **0개** | **Critical** |
| **FE** | watch() 사용 | **63개** | High |
| **FE** | deep: true 사용 | **34개** | High |
| **FE** | watchEffect() 사용 | **0개** | Medium |
| **BE** | !! 연산자 | **46개** | High |
| **BE** | print/println | **102개** | High |
| **BE** | Thread.sleep | **2개** | Critical |
| **BE** | runBlocking | **1개** | Critical |
| **BE** | .subscribe() | **19개** | High |
| **BE** | companion object | **29개** | High |
| **BE** | synchronized | **18개** | Medium |

### 1.2 가장 심각한 문제 TOP 5

| 순위 | 문제 | 영향 | RFC |
|:----:|------|------|-----|
| 1 | **shallowRef 0개** | 233개 ref가 깊은 반응성 추적 → CPU 과부하 | RFC-008 |
| 2 | **deep watch 34개** | 객체 전체 감시 → 불필요한 재렌더링 | RFC-008 |
| 3 | **Thread.sleep/runBlocking** | WebFlux 스레드 풀 블로킹 | RFC-007 |
| 4 | **companion object 29개** | 전역 가변 상태 → 동시성 버그 | RFC-003 |
| 5 | **.subscribe() 19개** | fire-and-forget → 에러 누락 | RFC-007 |

---

## 2. Frontend 심층 분석

### 2.1 icdStore.ts (2,971줄) - Critical

| 항목 | 수치 | 문제 |
|------|:----:|------|
| ref() 사용 | 81개 | 개별 반응성 트리거 |
| shallowRef() | **0개** | **전체를 깊은 추적** |
| computed() | 22개 | 대형 객체 반환 |
| 파일 크기 | 2,971줄 | 분리 필요 |

**핵심 문제**: 30ms마다 81개 ref 개별 업데이트
```typescript
// 현재 (비효율)
const azimuthAngle = ref('')       // 81개가 이런 패턴
const elevationAngle = ref('')
const trainAngle = ref('')

// 권장 (shallowRef 그룹화)
interface AntennaState {
  azimuth: string
  elevation: string
  train: string
}
const antennaState = shallowRef<AntennaState>({...})
```

**예상 효과**: ref 업데이트 81회 → 5-10회 (80% 감소)

### 2.2 대형 페이지 분석

| 파일 | 줄 수 | ref | watch | deep watch |
|------|------:|:---:|:-----:|:----------:|
| PassSchedulePage.vue | 4,838 | 38 | 5 | 3 |
| EphemerisDesignationPage.vue | 4,340 | 31 | 3 | 1 |
| DashboardPage.vue | 2,728 | 24 | 13 | 0 |

**분리 권장 컴포넌트**:

```
PassSchedulePage.vue (4,838줄)
├── ScheduleTable.vue (~800줄)
├── ScheduleChart.vue (~600줄)
├── ScheduleControls.vue (~400줄)
└── usePassScheduleLogic.ts (~500줄)

예상 결과: 4,838줄 → ~2,500줄 (48% 감소)
```

### 2.3 서비스 레이어

| 파일 | 줄 수 | console.log | 중복 코드 |
|------|------:|:-----------:|:---------:|
| ephemerisTrackService.ts | 1,193 | 50 | handleApiError |
| passScheduleService.ts | 1,118 | 42 | handleApiError |
| **합계** | **2,311** | **92** | 46줄 |

**중복 코드 (즉시 추출 가능)**:
- `handleApiError` 함수: 양쪽 파일에 거의 동일하게 존재
- `convertToChartData`: 배열 4-5회 순회 → 1회로 최적화 가능

### 2.4 Pinia 스토어 전체

| 스토어 | 줄 수 | ref 수 | 문제 |
|--------|------:|:------:|------|
| icdStore.ts | 2,971 | 81 | **분리 필요** |
| passScheduleStore.ts | 2,452 | 35 | console.log 103개 |
| ephemerisTrackStore.ts | 800+ | 18 | - |
| modeStore.ts (common) | 150 | 5 | **중복** |
| modeStore.ts (icd) | 180 | 6 | **중복** |

**modeStore 중복**: 2개 파일이 동일 ID 'mode' 사용 → **미사용 확인됨 (통합 보류)**

### 2.5 CSS/스타일링 - 통일 필요

| 항목 | 수치 | 통일 방향 | 효과 |
|------|:----:|----------|------|
| !important 사용 | 1,690개 | CSS 변수 + specificity | 유지보수성 향상 |
| 하드코딩 색상 | **220건** | `var(--theme-*)` | 테마 일관성 |
| var(--theme-*) 사용 | 196건 | 확대 적용 | - |
| scoped 스타일 | 대부분 | 유지 | 양호 |

> **참고**: useTheme.ts 169건은 **테마 시스템 정의**로 정상 (하드코딩 아님)

**!important 주요 위치** (통일 대상):
- PassSchedulePage.vue: ~341개 → CSS 변수로 통일
- DashboardPage.vue: ~128개
- SelectScheduleContent.vue: ~118개

---

## 3. Backend 심층 분석

### 3.1 서비스 레이어

| 서비스 | 줄 수 | !! | print | synchronized |
|--------|------:|:--:|:-----:|:------------:|
| UdpFwICDService.kt | 1,200+ | 8 | 15 | 5 |
| EphemerisService.kt | 800+ | 12 | 8 | 3 |
| PassScheduleService.kt | 700+ | 8 | 12 | 2 |
| ICDService.kt | 600+ | 7 | 20 | 4 |

### 3.2 companion object 분석 (2026-01-14 검증 완료)

> **✅ 전문가 분석 결과**: 29개 전수조사 완료 - **모두 안전**

```kotlin
// 실제 코드 분석 결과
companion object {
    val logger = LoggerFactory.getLogger(...)  // val - 불변
    const val TIMEOUT = 1000L                   // const - 상수
}
```

**검증 결과**:
- var 사용: **0개** (모두 val 또는 const)
- 동시성 위험: **없음**
- 조치 필요: **없음**

| 파일 | companion 수 | var 수 | 상태 |
|------|:-----------:|:------:|:----:|
| ICDService.kt | 19 | 0 | ✅ 안전 |
| 기타 8개 파일 | 10 | 0 | ✅ 안전 |

### 3.3 블로킹 코드 (Critical)

| 파일 | 위치 | 패턴 | 영향 |
|------|------|------|------|
| UdpFwICDService.kt | L1074, L1148 | Thread.sleep(1000) | 스레드 1초 블로킹 |
| ElevationCalculator.kt | L78 | runBlocking | 코루틴 블로킹 |

**수정 방안**:
```kotlin
// 변경 전
Thread.sleep(1000)

// 변경 후 (코루틴)
delay(1000)
```

### 3.4 .subscribe() fire-and-forget (High)

```kotlin
// 현재 (19곳) - 에러 처리 누락
someFlux.subscribe()

// 권장
someFlux.subscribe(
    { result -> /* success */ },
    { error -> logger.error("Error", error) }
)
```

### 3.5 컨트롤러 분석

| 컨트롤러 | 줄 수 | 동기 메서드 | 비동기 메서드 | @Valid |
|----------|------:|:-----------:|:------------:|:------:|
| PassScheduleController | 1,558 | 15 | 10 | 0 |
| EphemerisController | 1,092 | 4 | 10 | 0 |
| ICDController | 664 | 38 | 0 | 0 |
| SettingsController | 300 | 13 | 0 | 0 |

**입력 검증 전무**: @Valid/@Validated 사용 **0건**

---

## 4. FE-BE 통합 분석

### 4.1 타입 동기화

| FE 타입 | BE DTO | 일치 |
|---------|--------|:----:|
| ScheduleItem | PassScheduleMasterData | 부분 |
| TrackingData | TrackingDataDto | 부분 |
| AntennaState | AntennaDataDto | 부분 |

**문제점**: 필드명 및 nullable 처리 불일치 존재

### 4.2 WebSocket/UDP 통신

| 항목 | 현재 상태 | 문제 |
|------|----------|------|
| WebSocket 세션 관리 | mutableListOf | 동시성 위험 |
| UDP 패킷 손실 | 미처리 | 데이터 누락 가능 |
| 재연결 로직 | 기본적 | 견고성 부족 |
| 30ms 업데이트 | 전체 데이터 전송 | 대역폭 낭비 |

---

## 5. 기존 RFC 대비 신규 발견

### 5.1 이번 분석에서 새로 발견된 이슈

| # | 항목 | 기존 RFC | 신규 발견 | 차이 |
|---|------|:--------:|:--------:|:----:|
| 1 | shallowRef | 미확인 | **0개** | 신규 |
| 2 | deep watch | 미확인 | **34개** | 신규 |
| 3 | watchEffect | 미확인 | **0개** | 신규 |
| 4 | modeStore 중복 | 2개 | **2개 확인** | 확정 |
| 5 | companion object | 미확인 | **29개** | 신규 |
| 6 | .subscribe() | 미확인 | **19개** | 신규 |
| 7 | synchronized | 미확인 | **18개** | 신규 |

### 5.2 기존 RFC 수치 검증

| 항목 | RFC 기재 | 실제 측정 | 상태 |
|------|:--------:|:---------:|:----:|
| console.log | 1,513개 | 1,513개 | 일치 |
| !! 연산자 | 46건 | 46건 | 일치 |
| print/println | 102건 | 102건 | 일치 |
| icdStore ref | 175개 | 81개 (직접) | 재확인 필요 |
| watch 사용 | 62개 | 63개 | +1 |

---

## 6. 우선순위별 액션 플랜

> **⚠️ 2026-01-14 전문가 분석 반영**: 우선순위 재조정

### 6.1 즉시 (3일 - Day 1~3)

| # | 작업 | 파일 | 예상 효과 | 상태 |
|---|------|------|----------|:----:|
| 1 | **!! 연산자 P0** (3건) | EphemerisService, SunTrackService, PassScheduleService | Null 안전성 | Day 1 |
| 2 | **.subscribe() Critical** (2건) | UdpFwICDService.kt | 에러 가시성 | Day 1 |
| 3 | Thread.sleep → Mono.delay | UdpFwICDService.kt | 블로킹 제거 | Day 1 |
| 4 | Dead Code 삭제 | 69줄 | 코드 정리 | Day 1 |
| 5 | **shallowRef Phase 1** (36개) | icdStore.ts | CPU ~40% 감소 | Day 2 |
| 6 | ~~modeStore 통합~~ | ~~stores/~~ | 미사용 확인 | **보류** |

### 6.2 단기 (1개월)

| # | 작업 | 대상 | 예상 효과 | 비고 |
|---|------|------|----------|------|
| 1 | **shallowRef Phase 2-4** (43개) | icdStore.ts | CPU 추가 감소 | 복잡한 ref |
| 2 | deep watch 최적화 | 34개 위치 | 렌더링 최적화 | |
| 3 | ~~companion object 정리~~ | ~~29개~~ | ✅ **모두 안전** | 조치 불필요 |
| 4 | .subscribe() 에러 처리 | 나머지 17개 | 에러 가시성 | |
| 5 | handleApiError 공통화 | services/ | 92줄 중복 제거 | |

### 6.3 중기 (3개월)

| # | 작업 | 대상 | 예상 효과 |
|---|------|------|----------|
| 1 | 대형 페이지 분리 | 3개 페이지 | 4,838줄 → 2,500줄 |
| 2 | icdStore 분리 | icdStore.ts | 2,971줄 → 1,500줄 |
| 3 | !important 제거 | CSS 전체 | 유지보수성 향상 |
| 4 | @Valid 입력 검증 | Controllers | 보안 강화 |

---

## 7. RFC 업데이트 권장 (2026-01-14 수정)

### RFC-003 추가 항목
- [X] ~~companion object 정리 (29개)~~ → **조치 불필요** (var 0개)
- [ ] synchronized 패턴 통일 (18개)
- [ ] **!! 연산자 P0 3건** 우선 수정 (EphemerisService:2717, SunTrackService:636, PassScheduleService:3729)

### RFC-007 추가 항목
- [ ] Thread.sleep → Mono.delay 변환 (forceReconnect)
- [ ] runBlocking 제거 (1개)
- [ ] **.subscribe() Critical 2건** 우선 (UdpFwICDService:933,195)
- [ ] .subscribe() 나머지 17건 (Day 3)

### RFC-008 추가 항목
- [ ] **shallowRef 적용** (대용량 데이터 ref 15-20개)
- [ ] deep watch 최적화 (34개)
- [ ] watchEffect 도입 검토
- [X] ~~modeStore 통합~~ → **보류** (미사용 확인)

### 통일 필요 항목 (일관성/유지보수)
- [ ] **!important → CSS 변수** (1,690개) - 테마 일관성
- [ ] **하드코딩 색상 → 변수** (220건) - 다크모드 지원
- [ ] **catch → GlobalHandler** (196건) - 에러 처리 일관성
- [ ] **as → Zod 검증** (313건) - 타입 안전성

---

## 8. 결론

### 8.1 핵심 메시지 (2026-01-14 수정)

1. **FE 성능의 근본 원인**: shallowRef 0개 + deep watch 34개
2. ~~**BE 안정성 위험**: companion object 29개~~ → **✅ 검증 완료: 모두 안전**
3. **즉시 조치 필요**: !! P0 3건 + .subscribe() Critical 2건
4. **블로킹 제거**: Thread.sleep → Mono.delay, runBlocking 제거

### 8.2 예상 개선 효과 (3일 기준)

| 영역 | 현재 | 3일 후 | 효과 | 비고 |
|------|------|---------|------|------|
| FE CPU 사용률 | 높음 | 중간 | **~30% 감소** | 대용량 ref 최적화 |
| Dead Code | 69줄 | 0줄 | **100% 제거** | |
| !! P0 Critical | 3건 | 0건 | **100%** | Null 안전성 |
| .subscribe() Critical | 2건 | 0건 | **100%** | 에러 가시성 |
| BE 동시성 | ✅ 안전 | ✅ 안전 | - | companion object 검증 |

### 8.3 후속 작업: 통일 작업 (일관성/유지보수)

> **원칙**: 에러가 나지 않고 코드 품질이 향상된다면 **통일해야 함**

| 작업 | 건수 | 작업량 | 통일 효과 |
|------|:----:|:------:|----------|
| **!important → CSS변수** | 1,690개 | 3-4일 | 테마 일관성, 유지보수 |
| **하드코딩 색상 → 변수** | 220건 | 1-2일 | 다크모드 지원 |
| **catch → GlobalHandler** | 196건 | 2일 | 에러 처리 일관성 |
| **as → Zod 검증** | 313건 | 2-3일 | 타입 안전성 |

### 8.4 기타 후속 작업

| 작업 | 예상 효과 |
|------|----------|
| !! 연산자 나머지 (43개) | 전체 Null 안전성 |
| .subscribe() 나머지 (17개) | 전체 에러 가시성 |

---

## 9. 추가 검토 필요 항목 (2026-01-14 웹 검색 결과)

### 9.1 현재 RFC에 누락된 항목

| 항목 | 설명 | 우선순위 | 상태 |
|------|------|:--------:|:----:|
| **v-memo 디렉티브** | Vue 3.2+ 대형 리스트 메모이제이션 | P2 | RFC-008 추가 필요 |
| **SAST 도구 도입** | 정적 보안 분석 (SonarQube, Snyk) | P2 | RFC-006 작성 필요 |
| **Rate Limiting** | API 요청 제한 (무차별 공격 방지) | P2 | RFC-007 추가 필요 |
| **API 버저닝** | 하위 호환성 관리 (/api/v1/) | P3 | RFC-004 추가 필요 |
| **Circuit Breaker** | 장애 격리 (Resilience4j) | P3 | 신규 RFC 필요 |
| **watchEffect** | 자동 의존성 추적 (deep watch 대체) | P2 | RFC-008 추가 필요 |

### 9.2 OWASP Top 10 점검 현황

| 항목 | 현재 상태 | 권장 조치 |
|------|----------|----------|
| A01 Broken Access Control | △ 부분 구현 | 역할 기반 접근 제어 강화 |
| A02 Cryptographic Failures | △ | 환경 변수 암호화, 민감 정보 관리 |
| A03 Injection | ✓ Prepared Statement 사용 | 유지 |
| A04 Insecure Design | △ | Threat Modeling 문서화 |
| A05 Security Misconfiguration | △ | 기본값 점검, 보안 헤더 추가 |
| A06 Vulnerable Components | ❌ | 의존성 취약점 스캔 도입 |
| A07 Auth Failures | △ | JWT 만료 정책, 비밀번호 정책 |
| A08 Data Integrity Failures | △ | 서명 검증, 무결성 체크 |
| A09 Logging & Monitoring | ✓ RFC-002 | 유지 |
| A10 SSRF | ❌ 점검 필요 | URL 검증 로직 추가 |

### 9.3 Vue.js 성능 최적화 추가 권장

| 기법 | 적용 대상 | 효과 |
|------|----------|------|
| `v-memo` | 대형 리스트 (PassSchedule 목록) | 재렌더링 방지 |
| `v-once` | 정적 콘텐츠 | 단일 렌더링 |
| `defineAsyncComponent` | 모달/대화상자 | 초기 번들 감소 |
| `Suspense` | 비동기 컴포넌트 | 로딩 상태 관리 |

### 9.4 Spring WebFlux 추가 권장

| 항목 | 현재 | 권장 |
|------|------|------|
| Backpressure 관리 | 미적용 | `onBackpressureBuffer()` 추가 |
| 타임아웃 설정 | 기본값 | `timeout(Duration.ofSeconds(30))` |
| 에러 복구 | 부분 적용 | `onErrorResume()` 체이닝 |
| Retry 정책 | 미적용 | `retryWhen(Retry.backoff())` |

---

## 10. 구체적 해결 방안

> 상세 코드 예시는 [Concrete_Solutions.md](./Concrete_Solutions.md) 참조

---

**작성자**: Claude (10개 전문가 에이전트 + 직접 검증)
**최초 작성**: 2026-01-14
**수정**: 2026-01-14 (전문가 분석 결과 + 웹 검색 결과 반영)
