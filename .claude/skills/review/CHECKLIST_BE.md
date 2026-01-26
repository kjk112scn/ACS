# Backend 코드 리뷰 체크리스트

> Kotlin + Spring Boot 3.x + WebFlux 기반 백엔드 분석

---

## 1. WebFlux 비동기 패턴

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **BE-W01** | `block()` 사용 | Critical | 리액티브 체인에서 `block()` 호출 |
| **BE-W02** | Mono/Flux 미구독 | High | 반환값 무시하여 실행 안 됨 |
| **BE-W03** | 스케줄러 미지정 | Medium | 블로킹 작업에 `Schedulers.boundedElastic()` 없음 |
| **BE-W04** | 에러 전파 누락 | High | `onErrorResume`으로 삼켜버림 |
| **BE-W05** | 무한 Flux 메모리 누수 | Critical | `buffer()` 없이 무한 스트림 수집 |
| **BE-W06** | Hot Publisher 공유 오류 | High | `share()` 없이 여러 구독자 |

### 탐지 코드 패턴

```kotlin
// BE-W01: block() 사용 (BAD)
fun getData(): Data {
    return monoData.block() // 블로킹!
}

// BE-W02: Mono 미구독 (BAD)
fun saveData(data: Data) {
    repository.save(data) // 반환값 무시 → 실행 안 됨!
}
// GOOD
fun saveData(data: Data): Mono<Void> {
    return repository.save(data).then()
}

// BE-W03: 스케줄러 미지정 (BAD)
Mono.fromCallable { fileSystem.read() } // 블로킹 IO
// GOOD
Mono.fromCallable { fileSystem.read() }
    .subscribeOn(Schedulers.boundedElastic())
```

---

## 2. 상태머신 전환 로직

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **BE-M01** | 잘못된 상태 전이 | Critical | 허용되지 않은 상태 전환 |
| **BE-M02** | 상태 전이 로그 누락 | Medium | 상태 변경 시 로깅 없음 |
| **BE-M03** | 중복 상태 변수 | High | 동일 의미의 여러 플래그 |
| **BE-M04** | 상태 초기화 누락 | High | 재시작 시 이전 상태 잔존 |
| **BE-M05** | 비정상 종료 처리 누락 | Critical | ERROR 상태로 전이 안 됨 |
| **BE-M06** | 상태 전이 원자성 부재 | Critical | 상태 변경 중 인터럽트 |

### ACS 상태머신 검증

```kotlin
// PassScheduleService 상태 전이 규칙
enum class PassScheduleState {
    IDLE, STOWING, STOWED,
    MOVING_TRAIN, TRAIN_STABILIZING, MOVING_TO_START, READY,
    TRACKING, POST_TRACKING, COMPLETED, ERROR
}

// BE-M01: 허용된 전이만 검증
val validTransitions = mapOf(
    IDLE to setOf(STOWING, MOVING_TRAIN, ERROR),
    STOWING to setOf(STOWED, MOVING_TRAIN, ERROR),
    STOWED to setOf(MOVING_TRAIN, ERROR),
    MOVING_TRAIN to setOf(TRAIN_STABILIZING, ERROR),
    // ... 전체 전이 맵 검증
)

// BE-M06: 상태 전이 원자성 보장
private fun transitionTo(newState: PassScheduleState) {
    synchronized(stateLock) {
        previousState = currentState
        currentState = newState
        logger.info("State: $previousState → $newState")
    }
}
```

---

## 3. 타이밍/동시성 이슈

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **BE-C01** | Race Condition | Critical | 공유 변수 동시 접근 |
| **BE-C02** | Deadlock 가능성 | Critical | 여러 락 순서 불일치 |
| **BE-C03** | 원자적 연산 미사용 | High | `count++` 대신 `AtomicLong` |
| **BE-C04** | ConcurrentHashMap 복합 연산 | High | `if (!map.contains) map.put` 패턴 |
| **BE-C05** | 타이머 정밀도 문제 | High | 30ms/100ms 타이머 드리프트 |
| **BE-C06** | 스케줄러 스레드 블로킹 | Critical | 타이머 콜백에서 긴 작업 |

### 탐지 코드 패턴

```kotlin
// BE-C01: Race Condition (BAD)
private var trackingState = TrackingState.IDLE // 스레드 안전 X
// GOOD
private val trackingState = AtomicReference(TrackingState.IDLE)

// BE-C04: ConcurrentHashMap 복합 연산 위험 (BAD)
if (!cache.containsKey(key)) {
    cache[key] = expensiveComputation() // 경쟁 조건!
}
// GOOD
cache.computeIfAbsent(key) { expensiveComputation() }

// BE-C05: 타이머 정밀도 (ACS 30ms 요구)
scheduler.scheduleAtFixedRate(
    { handleTick() },
    0, 30, TimeUnit.MILLISECONDS // 드리프트 가능
)
// 권장: 절대 시간 기반 보정 로직 추가
```

---

## 4. Kotlin 특화

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **BE-K01** | `!!` 연산자 사용 | High | NPE 위험 |
| **BE-K02** | `catch(Exception)` | Medium | 구체적 예외 타입 권장 |
| **BE-K03** | println 사용 | Low | logger 사용 필수 |
| **BE-K04** | KDoc 누락 | Low | public 함수 문서화 필수 |
| **BE-K05** | data class의 var 사용 | Medium | val 권장 (불변성) |
| **BE-K06** | lateinit 오용 | High | 초기화 전 접근 위험 |

### 탐지 코드 패턴

```kotlin
// BE-K01: !! 연산자 (BAD)
val value = nullableValue!!
// GOOD
val value = nullableValue ?: defaultValue
val value = requireNotNull(nullableValue) { "message" }

// BE-K02: catch(Exception) (BAD)
try {
    riskyOperation()
} catch (e: Exception) { // 너무 광범위
    logger.error("Error", e)
}
// GOOD
try {
    riskyOperation()
} catch (e: IOException) {
    logger.error("IO Error", e)
} catch (e: IllegalStateException) {
    logger.error("State Error", e)
}
```

---

## 5. 에러 처리

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **BE-E01** | 예외 삼킴 | High | catch 블록에서 아무것도 안 함 |
| **BE-E02** | 불명확한 에러 메시지 | Medium | "Error occurred" 같은 메시지 |
| **BE-E03** | 스택 트레이스 미로깅 | Medium | `logger.error(message)` (예외 객체 없음) |
| **BE-E04** | 복구 로직 누락 | High | 실패 후 상태 복원 없음 |
| **BE-E05** | 재시도 로직 미구현 | Medium | 일시적 오류에 재시도 없음 |

---

## 6. Orekit 특화

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **BE-O01** | DataContext 미초기화 | Critical | Orekit 사용 전 초기화 확인 |
| **BE-O02** | TLE 만료 체크 누락 | High | 오래된 TLE 데이터 사용 |
| **BE-O03** | 좌표계 변환 오류 | Critical | TEME → ITRF 변환 누락 |
| **BE-O04** | 시간대 혼동 | High | UTC vs AbsoluteDate |
| **BE-O05** | 전파자 재사용 문제 | Medium | Propagator 스레드 안전성 |

### Orekit 검증 패턴

```kotlin
// BE-O01: DataContext 초기화 확인
@PostConstruct
fun initOrekit() {
    val orekitData = File("orekit-data")
    DataContext.getDefault().dataProvidersManager
        .addProvider(DirectoryCrawler(orekitData))
}

// BE-O03: 좌표계 변환
val pvTEME = propagator.propagate(date).pvCoordinates
val transform = FramesFactory.getTEME().getTransformTo(
    FramesFactory.getITRF(IERSConventions.IERS_2010, true), date
)
val pvITRF = transform.transformPVCoordinates(pvTEME)
```

---

## 7. 보안

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **BE-SEC01** | SQL Injection | Critical | 문자열 연결로 쿼리 생성 |
| **BE-SEC02** | 민감 정보 로깅 | High | 비밀번호, 토큰 로그 출력 |
| **BE-SEC03** | 하드코딩 자격증명 | Critical | 코드에 비밀번호 직접 |
| **BE-SEC04** | 권한 검사 누락 | High | 인가 없이 자원 접근 |

---

## 검사 명령 예시

```bash
# BE-K01: !! 연산자 검색
grep -r "!!" --include="*.kt"

# BE-K02: catch(Exception) 검색
grep -r "catch.*Exception" --include="*.kt"

# BE-K03: println 검색
grep -r "println" --include="*.kt"

# BE-W01: block() 검색
grep -r "\.block()" --include="*.kt"
```

---

**버전:** 1.0.0
**작성일:** 2026-01-26
