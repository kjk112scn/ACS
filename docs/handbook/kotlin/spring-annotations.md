# Spring 어노테이션 가이드

> `@` 붙은 것들이 뭔지 알아보자

## 어노테이션이란?

코드에 **메타데이터**를 붙이는 것. Spring이 이걸 읽고 자동으로 처리합니다.

```kotlin
@Service  // "이건 서비스야" 라고 Spring에게 알려줌
class UserService { ... }
```

---

## 컴포넌트 등록

Spring이 관리하는 객체(Bean)로 등록

| 어노테이션 | 용도 | 예시 |
|-----------|------|------|
| `@Component` | 일반 컴포넌트 | 유틸리티 클래스 |
| `@Service` | 비즈니스 로직 | UserService |
| `@Repository` | 데이터 접근 | UserRepository |
| `@Controller` | 웹 요청 처리 | UserController |
| `@RestController` | REST API | `@Controller` + `@ResponseBody` |
| `@Configuration` | 설정 클래스 | DatabaseConfig |

```kotlin
@Service  // Spring이 자동으로 객체 생성하고 관리
class EphemerisService(
    private val repository: TLERepository  // 자동 주입
) { ... }
```

---

## 의존성 주입 (DI)

### 생성자 주입 (권장)
```kotlin
@Service
class UserService(
    private val repository: UserRepository,  // 자동 주입
    private val emailService: EmailService   // 자동 주입
)
```

### @Autowired (명시적)
```kotlin
@Service
class UserService {
    @Autowired
    private lateinit var repository: UserRepository
}
```

**Kotlin에서는 생성자 주입이 더 깔끔!**

---

## REST API

### 기본 구조
```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(private val service: UserService) {

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): Mono<User> {
        return service.findById(id)
    }

    @PostMapping
    fun createUser(@RequestBody @Valid request: CreateUserRequest): Mono<User> {
        return service.create(request)
    }
}
```

### HTTP 메서드 매핑

| 어노테이션 | HTTP 메서드 | 용도 |
|-----------|-------------|------|
| `@GetMapping` | GET | 조회 |
| `@PostMapping` | POST | 생성 |
| `@PutMapping` | PUT | 전체 수정 |
| `@PatchMapping` | PATCH | 부분 수정 |
| `@DeleteMapping` | DELETE | 삭제 |

### 파라미터 바인딩

```kotlin
@GetMapping("/search")
fun search(
    @PathVariable id: Long,           // /users/{id}
    @RequestParam name: String,       // ?name=kim
    @RequestParam(required = false) age: Int?,  // 선택적
    @RequestBody request: SearchRequest,  // JSON body
    @RequestHeader("X-Token") token: String  // 헤더
): Flux<User>
```

---

## 검증 (Validation)

### @Valid
```kotlin
@PostMapping
fun create(@RequestBody @Valid request: CreateUserRequest): Mono<User>
```

### 검증 어노테이션
```kotlin
data class CreateUserRequest(
    @field:NotNull
    @field:Size(min = 2, max = 50)
    val name: String,

    @field:Email
    val email: String,

    @field:Min(0)
    @field:Max(150)
    val age: Int
)
```

| 어노테이션 | 검증 내용 |
|-----------|----------|
| `@NotNull` | null 불가 |
| `@NotBlank` | null, 빈 문자열, 공백만 불가 |
| `@Size(min, max)` | 길이 제한 |
| `@Min`, `@Max` | 숫자 범위 |
| `@Email` | 이메일 형식 |
| `@Pattern` | 정규표현식 |

---

## 생명주기

### @PostConstruct
Bean 생성 후 **초기화** 시 실행

```kotlin
@Service
class OrekitService {
    @PostConstruct
    fun init() {
        // 서버 시작 시 1회 실행
        loadOrekitData()
    }
}
```

### @PreDestroy
Bean 소멸 전 **정리** 시 실행

```kotlin
@Service
class UdpService {
    @PreDestroy
    fun cleanup() {
        // 서버 종료 시 실행
        closeConnection()
        saveRemainingData()
    }
}
```

---

## 스케줄링

### @Scheduled
```kotlin
@Service
class BatchService {
    @Scheduled(fixedRate = 5000)  // 5초마다
    fun processQueue() { ... }

    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    fun dailyCleanup() { ... }
}
```

| 옵션 | 설명 |
|------|------|
| `fixedRate` | 고정 간격 (이전 시작 기준) |
| `fixedDelay` | 고정 지연 (이전 완료 기준) |
| `cron` | Cron 표현식 |

**활성화 필요**:
```kotlin
@EnableScheduling
@SpringBootApplication
class Application
```

---

## ACS 프로젝트 실제 예시

### EphemerisController.kt
```kotlin
@RestController
@RequestMapping("/api/ephemeris")
class EphemerisController(
    private val ephemerisService: EphemerisService
) {
    @GetMapping("/tle/{satelliteId}")
    fun getTLE(@PathVariable satelliteId: String): Mono<TLEResponse> {
        return ephemerisService.getTLE(satelliteId)
    }

    @PostMapping("/track/start")
    fun startTracking(@RequestBody @Valid request: TrackingRequest): Mono<Unit> {
        return ephemerisService.startTracking(request)
    }
}
```

### EphemerisService.kt
```kotlin
@Service
class EphemerisService(
    private val tleRepository: TLERepository,
    private val commandSender: CommandSender
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        logger.info("EphemerisService 초기화")
        loadCache()
    }

    @PreDestroy
    fun cleanup() {
        logger.info("EphemerisService 정리")
        saveState()
    }
}
```

### CorsConfig.kt
```kotlin
@Configuration
class CorsConfig {
    @Bean
    fun corsWebFilter(): CorsWebFilter {
        // CORS 설정
    }
}
```

---

## 자주 쓰는 조합

### REST API 기본
```kotlin
@RestController
@RequestMapping("/api/xxx")
class XxxController(private val service: XxxService) {
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long) = service.findById(id)

    @PostMapping
    fun create(@RequestBody @Valid req: CreateRequest) = service.create(req)
}
```

### 서비스 기본
```kotlin
@Service
class XxxService(
    private val repository: XxxRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() { ... }

    @PreDestroy
    fun cleanup() { ... }
}
```

---

## 요약 표

| 카테고리 | 어노테이션 | 용도 |
|---------|-----------|------|
| **Bean 등록** | `@Service`, `@Controller` | Spring 관리 객체 |
| **API 매핑** | `@GetMapping`, `@PostMapping` | HTTP 요청 처리 |
| **파라미터** | `@PathVariable`, `@RequestBody` | 요청 데이터 바인딩 |
| **검증** | `@Valid`, `@NotNull` | 입력 검증 |
| **생명주기** | `@PostConstruct`, `@PreDestroy` | 초기화/정리 |
| **스케줄** | `@Scheduled` | 주기적 실행 |

---

**다음 학습**: [kotlin-coroutines.md](./kotlin-coroutines.md) - 코루틴/비동기
