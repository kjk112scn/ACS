# Kotlin 리액티브 프로그래밍 (WebFlux)

> 스레드를 효율적으로 쓰는 비동기 프로그래밍

## 왜 리액티브?

### 전통적인 방식 (블로킹)
```
요청 1 → 스레드 1 (DB 대기중...) → 응답
요청 2 → 스레드 2 (DB 대기중...) → 응답
요청 3 → 스레드 3 (DB 대기중...) → 응답
```
- 동시 요청 100개 = 스레드 100개 필요
- 스레드가 **대기만 하면서** 메모리 차지

### 리액티브 방식 (논블로킹)
```
요청 1 → 스레드 1 (DB 요청 던지고 반환)
요청 2 → 스레드 1 (DB 요청 던지고 반환)  ← 같은 스레드!
요청 3 → 스레드 1 (DB 요청 던지고 반환)
         ↓
      DB 응답 오면 그때 처리
```
- 스레드가 **놀지 않고** 계속 일함
- 적은 스레드로 많은 요청 처리

---

## Mono와 Flux

### Mono - 0~1개 결과
```kotlin
// 사용자 1명 조회
fun findUser(id: Long): Mono<User>

// 결과 없으면 Mono.empty()
// 결과 있으면 Mono.just(user)
```

### Flux - 0~N개 결과
```kotlin
// 모든 사용자 조회
fun findAllUsers(): Flux<User>

// 스트림처럼 하나씩 방출
// [User1] → [User2] → [User3] → 완료
```

---

## 기본 연산자

### map - 변환
```kotlin
Mono.just(user)
    .map { it.name }  // User → String
```

### flatMap - 비동기 체이닝
```kotlin
findUser(id)
    .flatMap { user -> findOrders(user.id) }  // Mono<User> → Mono<List<Order>>
```

**map vs flatMap**:
- `map`: 동기 변환 (A → B)
- `flatMap`: 비동기 변환 (A → Mono<B>)

### filter - 조건 필터
```kotlin
Flux.fromIterable(users)
    .filter { it.age >= 18 }
```

### doOnNext - 부수효과 (로깅 등)
```kotlin
findUser(id)
    .doOnNext { logger.info("Found: ${it.name}") }
```

### onErrorResume - 에러 처리
```kotlin
findUser(id)
    .onErrorResume { Mono.just(defaultUser) }  // 에러 시 기본값
```

---

## subscribe() - 실행 트리거

**중요**: subscribe()를 호출해야 실제로 실행됩니다!

```kotlin
// ❌ 아무것도 안 함 (선언만)
findUser(id).map { it.name }

// ✅ 실행됨
findUser(id)
    .map { it.name }
    .subscribe()
```

### 에러 핸들러 필수
```kotlin
// ❌ 에러 무시 (위험)
flux.subscribe { data -> process(data) }

// ✅ 에러 처리
flux.subscribe(
    { data -> process(data) },      // onNext
    { error -> handleError(error) } // onError
)
```

---

## 지연 실행

### Mono.delay - 논블로킹 대기
```kotlin
// ❌ 블로킹 (스레드 멈춤)
Thread.sleep(1000)

// ✅ 논블로킹 (스레드 반환)
Mono.delay(Duration.ofSeconds(1))
    .then(doSomething())
```

### Mono.defer - 지연 생성
```kotlin
// 매번 새로운 타임스탬프
Mono.defer { Mono.just(System.currentTimeMillis()) }
```

---

## 스케줄러 (실행 스레드)

```kotlin
Mono.just(data)
    .subscribeOn(Schedulers.boundedElastic())  // 처음 실행 스레드
    .publishOn(Schedulers.parallel())           // 이후 실행 스레드
```

| 스케줄러 | 용도 |
|---------|------|
| `parallel()` | CPU 집약 작업 |
| `boundedElastic()` | I/O 작업 (DB, 파일) |
| `single()` | 순차 처리 필요 시 |

---

## 실전 패턴

### 패턴 1: API 응답
```kotlin
@GetMapping("/users/{id}")
fun getUser(@PathVariable id: Long): Mono<User> {
    return userService.findById(id)
        .switchIfEmpty(Mono.error(NotFoundException()))
}
```

### 패턴 2: 여러 호출 병렬 실행
```kotlin
Mono.zip(
    findUser(id),
    findOrders(id),
    findPayments(id)
) { user, orders, payments ->
    UserDetail(user, orders, payments)
}
```

### 패턴 3: 조건부 실행
```kotlin
findUser(id)
    .filter { it.isActive }
    .flatMap { sendNotification(it) }
    .switchIfEmpty(Mono.empty())  // 비활성 사용자면 스킵
```

### 패턴 4: 타임아웃
```kotlin
callExternalApi()
    .timeout(Duration.ofSeconds(5))
    .onErrorResume(TimeoutException::class.java) {
        Mono.just(fallbackResponse)
    }
```

---

## ACS 프로젝트 실제 예시

### EphemerisController.kt
```kotlin
@GetMapping("/tle/{satelliteId}")
fun getTLE(@PathVariable satelliteId: String): Mono<TLEResponse> {
    return ephemerisService.getTLE(satelliteId)
        .map { tle -> TLEResponse(tle.line1, tle.line2) }
}
```

### PassScheduleService.kt
```kotlin
// 이벤트 구독 (에러 핸들러 포함)
eventBus.subscribe(
    { event -> handleScheduleEvent(event) },
    { error -> logger.error("이벤트 처리 실패", error) }
)
```

### UdpFwICDService.kt
```kotlin
// 지연 실행 (논블로킹)
Mono.delay(Duration.ofSeconds(1))
    .doOnNext { logger.info("1초 후 실행") }
    .then(sendCommand())
    .subscribe()
```

---

## 블로킹 vs 논블로킹 비교

| 작업 | 블로킹 | 논블로킹 |
|------|--------|----------|
| 대기 | `Thread.sleep()` | `Mono.delay()` |
| DB 조회 | `repository.findById()` | `repository.findById()` (R2DBC) |
| HTTP 호출 | `RestTemplate` | `WebClient` |
| 파일 읽기 | `File.readText()` | `AsynchronousFileChannel` |

---

## 흔한 실수

### ❌ block() 남용
```kotlin
// 리액티브의 의미가 없어짐
val user = findUser(id).block()  // 블로킹!
```

### ❌ subscribe 없이 끝남
```kotlin
// 아무것도 실행 안 됨
findUser(id).map { it.name }
```

### ❌ 에러 핸들러 없는 subscribe
```kotlin
flux.subscribe { }  // 에러 발생하면 조용히 실패
```

---

## 요약

| 개념 | 설명 |
|------|------|
| `Mono<T>` | 0~1개 결과 |
| `Flux<T>` | 0~N개 결과 |
| `map` | 동기 변환 |
| `flatMap` | 비동기 변환 |
| `subscribe()` | 실행 시작 |
| `Mono.delay()` | 논블로킹 대기 |

---

**다음 학습**: [spring-annotations.md](./spring-annotations.md) - Spring 어노테이션
