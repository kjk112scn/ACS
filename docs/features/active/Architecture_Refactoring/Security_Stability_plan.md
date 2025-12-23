# Security & Stability Plan (보안 및 안정성 계획서)

> 상위 문서: [Architecture_Refactoring_plan.md](./Architecture_Refactoring_plan.md)
>
> ⚠️ **Phase 4 (선택적)**: 로컬 환경에서는 적용 필요성 낮음. 외부 노출 시에만 적용 권장.

---

## 개요

ACS 프로젝트의 보안/안정성 강화 계획입니다.

**적용 환경 분류**:

| 환경 | 설명 | 보안 적용 |
|------|------|----------|
| 🏠 로컬 환경 | 내부 IP 접근, Windows + Nginx | ⚪ 선택적 |
| 🌐 외부 노출 | 인터넷 공개, 클라우드 배포 | 🔴 필수 |

**현재 ACS 환경**: 🏠 로컬 환경 (내부 IP 접근)
- Windows 환경, Nginx 웹서버
- 내부 네트워크에서만 접근
- **보안은 Phase 4 (선택적)로 분류됨**

**목표**:
- 24/7 무중단 운영 안정성 확보 (메모리 누수 방지)
- 실시간 데이터 처리 신뢰성 보장
- (선택) 외부 노출 시 보안 취약점 제거

---

## 1. 보안 취약점 (🟢 선택적 - 외부 노출 시 적용)

### 1.1 하드코딩 크리덴셜 제거

**현재 문제** (위험도: CRITICAL):

```typescript
// LoginPage.vue (라인 24) - 프론트엔드에 평문 저장
if (username.value === 'de' && password.value === 'de') {
  // 로그인 성공
}
```

```properties
# application-with-db.properties (라인 9)
spring.datasource.password=0000
```

**해결책**:

```typescript
// 1. 백엔드 인증 API 호출
const login = async () => {
  const response = await api.post('/auth/login', {
    username: username.value,
    password: password.value
  })
  if (response.data.token) {
    authStore.setToken(response.data.token)
  }
}
```

```yaml
# 환경 변수 사용
spring:
  datasource:
    password: ${DB_PASSWORD}
```

---

### 1.2 백엔드 인증 구현

**현재 문제** (위험도: CRITICAL):
- Spring Security 미적용
- 모든 API 인증 없이 접근 가능
- WebSocket 인증 없음

**해결책**:

```kotlin
// SecurityConfig.kt (신규)
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }  // WebSocket용
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/api/auth/**").permitAll()
                    .pathMatchers("/ws/**").authenticated()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { it.jwt {} }
            .build()
    }
}

// JwtTokenProvider.kt (신규)
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {
    fun createToken(username: String): String {
        val claims = Jwts.claims().setSubject(username)
        val now = Date()
        val validity = Date(now.time + expiration)

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
```

---

### 1.3 HTTPS 강제 적용

**현재 문제** (위험도: CRITICAL):
- HTTP만 사용 (포트 8080)
- 중간자 공격(MITM) 가능
- 데이터 평문 전송

**해결책**:

```yaml
# application.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
    key-alias: acs-server

# HTTP → HTTPS 리다이렉트
  http:
    port: 8080
```

```kotlin
// HttpsRedirectConfig.kt (신규)
@Configuration
class HttpsRedirectConfig {
    @Bean
    fun httpsRedirectWebFilter(): WebFilter {
        return WebFilter { exchange, chain ->
            if (exchange.request.uri.scheme == "http") {
                val httpsUri = UriComponentsBuilder.fromUri(exchange.request.uri)
                    .scheme("https")
                    .port(8443)
                    .build()
                    .toUri()
                exchange.response.statusCode = HttpStatus.MOVED_PERMANENTLY
                exchange.response.headers.location = httpsUri
                Mono.empty()
            } else {
                chain.filter(exchange)
            }
        }
    }
}
```

---

### 1.4 CORS 정책 수정

**현재 문제** (위험도: CRITICAL):

```kotlin
// CorsConfig.kt (라인 21-26)
allowedOrigins = listOf(
    "http://localhost:9000",
    "*"  // ❌ 와일드카드 허용
)
allowCredentials = true  // ❌ 와일드카드와 함께 사용 불가
```

**해결책**:

```kotlin
// CorsConfig.kt (수정)
@Configuration
class CorsConfig : WebFluxConfigurer {

    @Value("\${cors.allowed-origins}")
    private lateinit var allowedOrigins: List<String>

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(*allowedOrigins.toTypedArray())  // 명시적 도메인만
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("Authorization", "Content-Type")
            .allowCredentials(true)
            .maxAge(3600)

        registry.addMapping("/ws/**")
            .allowedOrigins(*allowedOrigins.toTypedArray())
            .allowCredentials(true)
    }
}
```

```yaml
# application.yml
cors:
  allowed-origins:
    - https://acs.gtlsystems.com
    - https://admin.gtlsystems.com
```

---

### 1.5 WebSocket 인증

**현재 문제** (위험도: 높음):
- 누구나 `/ws` 경로 접속 가능
- 세션 검증 없음

**해결책**:

```kotlin
// WebSocketAuthInterceptor.kt (신규)
@Component
class WebSocketAuthInterceptor(
    private val jwtTokenProvider: JwtTokenProvider
) : WebSocketHandlerDecoratorFactory {

    override fun decorate(handler: WebSocketHandler): WebSocketHandler {
        return WebSocketHandler { session ->
            val token = extractToken(session)
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                session.close(CloseStatus.POLICY_VIOLATION)
                return@WebSocketHandler Mono.empty()
            }
            handler.handle(session)
        }
    }

    private fun extractToken(session: WebSocketSession): String? {
        val query = session.handshakeInfo.uri.query
        return query?.split("&")
            ?.find { it.startsWith("token=") }
            ?.substringAfter("token=")
    }
}
```

---

### 1.6 입력 검증 강화

**현재 문제** (위험도: 중간):
- API 입력값 검증 부족
- XSS 취약점 가능성

**해결책**:

```kotlin
// ValidationConfig.kt (신규)
@Configuration
class ValidationConfig {

    @Bean
    fun validator(): Validator {
        return LocalValidatorFactoryBean()
    }
}

// DTO with validation
data class CommandRequest(
    @field:NotBlank(message = "Command type is required")
    @field:Pattern(regexp = "^[ES]$", message = "Command type must be E or S")
    val commandType: String,

    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val azimuth: Double?,

    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val elevation: Double?
)
```

---

## 2. 안정성 문제 (🟠 중요)

### 2.1 메모리 누수 방지

**현재 문제**:

```kotlin
// LoggingService.kt - 제한 없는 증가
private val performanceTimers = ConcurrentHashMap<String, Long>()  // ❌ 무한 증가
private val logStats = ConcurrentHashMap<String, AtomicLong>()     // ❌ 무한 증가
```

**해결책**:

```kotlin
// LoggingService.kt (수정)
@Service
class LoggingService {
    companion object {
        private const val MAX_PERFORMANCE_TIMERS = 1000
        private const val TIMER_EXPIRY_MS = 30 * 60 * 1000L  // 30분
    }

    private val performanceTimers = ConcurrentHashMap<String, TimerEntry>()

    data class TimerEntry(
        val startTime: Long,
        val createdAt: Long = System.currentTimeMillis()
    )

    // 주기적 정리 (1분마다)
    @Scheduled(fixedRate = 60000)
    fun cleanupExpiredTimers() {
        val now = System.currentTimeMillis()
        performanceTimers.entries.removeIf { (_, entry) ->
            now - entry.createdAt > TIMER_EXPIRY_MS
        }

        // 크기 제한
        if (performanceTimers.size > MAX_PERFORMANCE_TIMERS) {
            val oldestKeys = performanceTimers.entries
                .sortedBy { it.value.createdAt }
                .take(performanceTimers.size - MAX_PERFORMANCE_TIMERS)
                .map { it.key }
            oldestKeys.forEach { performanceTimers.remove(it) }
        }
    }
}
```

---

### 2.2 UDP 패킷 손실 감지

**현재 문제**:
- 패킷 손실 감지 없음
- 장애 시 수동 개입 필요

**해결책**:

```kotlin
// UdpHealthMonitor.kt (신규)
@Component
class UdpHealthMonitor(
    private val udpService: UdpFwICDService,
    private val eventBus: ACSEventBus
) {
    private val lastReceivedTime = AtomicLong(System.currentTimeMillis())
    private val consecutiveMisses = AtomicInteger(0)

    companion object {
        private const val EXPECTED_INTERVAL_MS = 10L
        private const val MAX_CONSECUTIVE_MISSES = 5
        private const val HEALTH_CHECK_INTERVAL_MS = 100L
    }

    @Scheduled(fixedRate = HEALTH_CHECK_INTERVAL_MS)
    fun checkUdpHealth() {
        val timeSinceLastPacket = System.currentTimeMillis() - lastReceivedTime.get()
        val expectedPackets = timeSinceLastPacket / EXPECTED_INTERVAL_MS

        if (expectedPackets > MAX_CONSECUTIVE_MISSES) {
            consecutiveMisses.incrementAndGet()
            logger.warn("UDP 패킷 손실 감지: ${consecutiveMisses.get()}회 연속")

            if (consecutiveMisses.get() >= 3) {
                logger.error("UDP 연결 불안정 - 재연결 시도")
                eventBus.publish(UdpReconnectEvent())
                udpService.reconnect()
            }
        } else {
            consecutiveMisses.set(0)
        }
    }

    fun onPacketReceived() {
        lastReceivedTime.set(System.currentTimeMillis())
        consecutiveMisses.set(0)
    }
}
```

---

### 2.3 GlobalData 스레드 안전성

**현재 문제**:

```kotlin
// GlobalData.kt - 동기화 없음
object Offset {
    var TimeOffset: Float = 0.0f           // ❌ var 직접 접근
    var azimuthPositionOffset: Float = 0.0f
    var elevationPositionOffset: Float = 0.0f
}
```

**해결책**:

```kotlin
// GlobalData.kt (수정)
object GlobalData {

    // 스레드 안전한 Offset 관리
    object Offset {
        private val _timeOffset = AtomicReference(0.0f)
        private val _azimuthOffset = AtomicReference(0.0f)
        private val _elevationOffset = AtomicReference(0.0f)
        private val _trainOffset = AtomicReference(0.0f)
        private val _trueNorthOffset = AtomicReference(0.0f)

        var timeOffset: Float
            get() = _timeOffset.get()
            set(value) = _timeOffset.set(value)

        var azimuthPositionOffset: Float
            get() = _azimuthOffset.get()
            set(value) = _azimuthOffset.set(value)

        // 원자적 업데이트 (여러 값 동시 변경)
        fun updateAll(
            azimuth: Float,
            elevation: Float,
            train: Float
        ) {
            synchronized(this) {
                _azimuthOffset.set(azimuth)
                _elevationOffset.set(elevation)
                _trainOffset.set(train)
            }
        }

        // 원자적 읽기 (일관된 스냅샷)
        fun getSnapshot(): OffsetSnapshot {
            synchronized(this) {
                return OffsetSnapshot(
                    azimuth = _azimuthOffset.get(),
                    elevation = _elevationOffset.get(),
                    train = _trainOffset.get(),
                    trueNorth = _trueNorthOffset.get()
                )
            }
        }
    }

    data class OffsetSnapshot(
        val azimuth: Float,
        val elevation: Float,
        val train: Float,
        val trueNorth: Float
    )
}
```

---

### 2.4 재시도 정책 표준화

**현재 문제**:
- 재시도 횟수 제한 없음
- 백오프 전략 없음

**해결책**:

```kotlin
// RetryPolicy.kt (신규)
@Component
class RetryPolicy {
    companion object {
        const val MAX_RETRIES = 3
        val BACKOFF_DELAYS = listOf(1000L, 5000L, 25000L)  // 지수 백오프
    }

    suspend fun <T> withRetry(
        operation: String,
        action: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return Result.success(action())
            } catch (e: Exception) {
                lastException = e
                logger.warn("$operation 실패 (시도 ${attempt + 1}/$MAX_RETRIES): ${e.message}")

                if (attempt < MAX_RETRIES - 1) {
                    delay(BACKOFF_DELAYS[attempt])
                }
            }
        }

        logger.error("$operation 최종 실패: ${lastException?.message}")
        return Result.failure(lastException!!)
    }
}

// 사용 예시
class UdpFwICDService(private val retryPolicy: RetryPolicy) {

    suspend fun sendCommand(command: ByteArray) {
        retryPolicy.withRetry("UDP 명령 전송") {
            channel.send(ByteBuffer.wrap(command), targetAddress)
        }.onFailure { error ->
            eventBus.publish(UdpErrorEvent(error))
        }
    }
}
```

---

## 3. 성능 최적화 (🟡 개선)

### 3.1 프론트엔드: 선택적 상태 업데이트

**현재 문제**:

```typescript
// AllStatusContent.vue - 매번 전체 복사
realtimeData.value = {
  updateCount: icdStore.updateCount,
  serverTime: icdStore.serverTime,
  // ... 50개 이상의 필드 매번 복사
}
```

**해결책**:

```typescript
// useSelectiveUpdate.ts (신규)
export function useSelectiveUpdate<T extends object>(
  source: T,
  target: Ref<T>
) {
  const prevValues = new Map<keyof T, unknown>()

  const update = () => {
    let hasChanges = false
    const updates: Partial<T> = {}

    for (const key of Object.keys(source) as (keyof T)[]) {
      const newValue = source[key]
      const prevValue = prevValues.get(key)

      if (!Object.is(newValue, prevValue)) {
        updates[key] = newValue
        prevValues.set(key, newValue)
        hasChanges = true
      }
    }

    if (hasChanges) {
      Object.assign(target.value, updates)
    }
  }

  return { update }
}

// 사용
const { update } = useSelectiveUpdate(icdStore, realtimeData)
setInterval(update, 100)
```

### 3.2 비트 파싱 최적화

**현재 문제**:

```typescript
// 비효율적인 문자열 파싱
const bits = bitString.padStart(8, '0').split('').reverse()
protocolElevationStatus.value = bits[0] === '1'
```

**해결책**:

```typescript
// utils/bitParser.ts (신규)
export function parseBits(value: number): boolean[] {
  return [
    (value & 0x01) !== 0,  // bit 0
    (value & 0x02) !== 0,  // bit 1
    (value & 0x04) !== 0,  // bit 2
    (value & 0x08) !== 0,  // bit 3
    (value & 0x10) !== 0,  // bit 4
    (value & 0x20) !== 0,  // bit 5
    (value & 0x40) !== 0,  // bit 6
    (value & 0x80) !== 0,  // bit 7
  ]
}

// 또는 비트마스크 상수 사용
export const BIT_MASKS = {
  PROTOCOL_ELEVATION: 0x01,
  PROTOCOL_AZIMUTH: 0x02,
  PROTOCOL_TRAIN: 0x04,
  // ...
} as const

export function hasBit(value: number, mask: number): boolean {
  return (value & mask) !== 0
}
```

---

## 4. 장기 운영 안정성

### 4.1 로그 관리

```yaml
# logback-spring.xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/acs.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/acs.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>3GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

### 4.2 데이터 아카이빙

```kotlin
// DataArchiveService.kt (신규)
@Service
class DataArchiveService(
    private val batchStorageManager: BatchStorageManager
) {
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정
    fun archiveTrackingData() {
        val data = batchStorageManager.getRealtimeTrackingData()
        if (data.isNotEmpty()) {
            val filename = "tracking_${LocalDate.now()}.json"
            saveToArchive(filename, data)
            batchStorageManager.clearRealtimeTrackingData()
        }
    }
}
```

### 4.3 Health Check 엔드포인트

```kotlin
// HealthController.kt (신규)
@RestController
@RequestMapping("/api/health")
class HealthController(
    private val udpService: UdpFwICDService,
    private val dataStoreService: DataStoreService
) {
    @GetMapping
    fun health(): HealthStatus {
        return HealthStatus(
            status = "UP",
            udpConnected = udpService.isConnected(),
            lastDataReceived = dataStoreService.getLastUpdateTime(),
            activeWebSocketSessions = pushDataController.getSessionCount(),
            memoryUsage = getMemoryUsage(),
            uptime = getUptime()
        )
    }

    @GetMapping("/ready")
    fun readiness(): ResponseEntity<String> {
        return if (udpService.isConnected()) {
            ResponseEntity.ok("READY")
        } else {
            ResponseEntity.status(503).body("NOT_READY")
        }
    }

    @GetMapping("/live")
    fun liveness(): ResponseEntity<String> {
        return ResponseEntity.ok("ALIVE")
    }
}
```

---

## 5. 완료 기준

> ⚠️ 이 문서는 Architecture_Refactoring_plan.md의 **Phase 4 (선택적)**에 해당합니다.
> 로컬 환경에서는 안정성 항목만 우선 적용하고, 보안은 외부 노출 시 적용합니다.

### 안정성 (🟠 권장 - 장시간 운영 시)
- [ ] 메모리 누수 방지 (LoggingService cleanup)
- [ ] Health Check 구현

### 보안 (🟢 선택적 - 외부 노출 시)
- [ ] 하드코딩 크리덴셜 제거
- [ ] HTTPS 구성
- [ ] CORS 정책 수정
- [ ] DB 비밀번호 환경 변수화
- [ ] Spring Security 적용
- [ ] JWT 인증 구현
- [ ] WebSocket 인증 추가
- [ ] 입력 검증 강화

### 성능 최적화 (🟡 선택적)
- [ ] 선택적 상태 업데이트
- [ ] 비트 파싱 최적화
- [ ] 로그 관리 체계화

---

## 6. 위험도 매트릭스

```
┌─────────────────────────────────────┬──────────┬──────────┐
│ 항목                                │ 현재     │ 목표     │
├─────────────────────────────────────┼──────────┼──────────┤
│ 인증/인가                           │ 🔴 극도로│ 🟢 낮음  │
│ 데이터 보호 (HTTPS)                 │ 🔴 극도로│ 🟢 낮음  │
│ CORS 정책                           │ 🔴 극도로│ 🟢 낮음  │
│ 메모리 누수                         │ 🟠 중간  │ 🟢 낮음  │
│ 실시간 안정성                       │ 🟠 중간  │ 🟢 낮음  │
│ 동시성/경쟁 조건                    │ 🟠 중간  │ 🟢 낮음  │
│ 성능                                │ 🟡 낮음  │ 🟢 최적  │
└─────────────────────────────────────┴──────────┴──────────┘
```

---

**문서 버전**: 1.1.0
**작성일**: 2024-12

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0.0 | 2024-12 | 최초 작성 |
| 1.1.0 | 2024-12 | 로컬 환경 기준으로 우선순위 재조정 (Phase 4 선택적)

