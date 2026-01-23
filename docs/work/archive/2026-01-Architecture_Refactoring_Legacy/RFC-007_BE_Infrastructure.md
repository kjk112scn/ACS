# RFC-007: BE 인프라 개선

> **버전**: 1.4.0 | **작성일**: 2026-01-14
> **상태**: Draft | **우선순위**: **P0**
> **역할**: BE 기반 인프라 (입력 검증, Repository 추상화, GlobalData 체계화, print/println 제거, 예외 처리 개선, 리소스 누수 수정, **동시성 안전성**)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.4.0 | 2026-01-14 | GlobalData 동시성 해결 방안 단일화: 옵션 A (개별 AtomicReference) 확정 |
| 1.3.0 | 2026-01-14 | GlobalData var 동시성 문제 추가, lateinit 초기화 검증 추가, Phase 7-8 추가 |
| 1.2.0 | 2026-01-14 | 전문가 검증 결과 반영: 우선순위 P0 상향, companion object 29개(안전), .subscribe() 19개 추가 |
| 1.1.0 | 2026-01-13 | 전수조사 결과 반영: Phase 5-6 추가 (광범위 catch, 리소스 누수) |
| 1.0.0 | 2026-01-13 | RFC-004 Phase 6에서 분리하여 신규 생성 |

---

## 1. 배경 (Context)

### 왜 이 문서가 분리되었는가?

기존 RFC-004 (API 표준화)에 Phase 6으로 포함되어 있던 BE 인프라 관련 내용을 별도 RFC로 분리했습니다.

| 기존 | 변경 후 |
|------|---------|
| RFC-004 Phase 6 | RFC-007 (이 문서) |

**분리 이유**:
- RFC-004는 "API 표준화"에 집중해야 함
- 입력 검증, Repository 추상화는 API보다 더 기반적인 인프라
- 문서 역할 명확화 (Single Responsibility)

---

## 2. 현재 상태 (Current State)

### 2.1 입력 검증 전무 (Critical!)

| 현황 | 리스크 |
|------|--------|
| @Valid, @NotNull 거의 없음 (9건만) | 악의적 입력으로 시스템 오류 가능 |
| ICDController: 30+ 파라미터, 검증 없음 | 위성 추적 데이터 오염 가능 |
| 각도 범위 (-360° ~ 360°) 미검증 | UDP 명령 오류 가능 |

### 2.2 Repository 추상화 부재 (High)

| 현황 | 리스크 |
|------|--------|
| ConcurrentHashMap 직접 사용 (5+ 서비스) | 테스트 불가 (목 대체 불가) |
| 메모리 ↔ DB 전환 시 전체 수정 필요 | DB 마이그레이션 어려움 |
| RFC-001에서 DB 제시했지만 추상화 없음 | 커버리지 1.5% 원인 |

### 2.3 GlobalData 비체계적 관리 (Medium → **Critical 상향**)

| 현황 | 리스크 |
|------|--------|
| GlobalData.kt에 모든 공유 데이터 집중 | 상태 변경 추적 어려움 |
| ~~companion object 다수 (128개 추정)~~ | ~~멀티스레드 race condition~~ |
| 변경 이력 없음 | 예상치 못한 부작용 |

> **2026-01-14 전문가 검증 결과**: companion object **29개** 전수조사 완료
> - var 사용: **0개** (모두 val 또는 const)
> - 동시성 위험: **없음**
> - **조치 불필요** ✅

### 2.3.1 GlobalData var 필드 동시성 문제 (**Critical - 신규**)

> **2026-01-14 추가 분석**: GlobalData 내 **18개 var 필드**가 synchronized 없이 다중 스레드에서 접근됨

| object | var 필드 | 용도 | 동시 접근 스레드 | 위험도 |
|--------|----------|------|-----------------|:------:|
| `Offset` | TimeOffset, azimuthPositionOffset, elevationPositionOffset, trainPositionOffset, trueNorthOffset (**5개**) | 오프셋 값 | UDP(30ms), 추적, WebSocket | **Critical** |
| `EphemerisTrakingAngle` | azimuthAngle, elevationAngle, trainAngle (**3개**) | 현재 추적 각도 | UDP(30ms), 추적 | **Critical** |
| `SunTrackingData` | azimuth/elevation/train Angle/Speed (**6개**) | 태양 추적 | UDP(30ms), SunTrack | **Critical** |
| `Time` | serverTimeZone, clientTimeZone (**2개**) | 시간대 설정 | 드물게 변경 | Medium |
| `Version` | apiVersion, buildDate (**2개**) | 버전 정보 | 초기화 시 1회 | Low |

**문제 코드 예시** (GlobalData.kt:49-53):
```kotlin
// 현재 (동시성 위험)
object Offset {
    var TimeOffset: Float = 0.0f           // ❌ 동기화 없음
    var azimuthPositionOffset: Float = 0.0f // ❌ 동기화 없음
    var elevationPositionOffset: Float = 0.0f
    var trainPositionOffset: Float = 0.0f
    var trueNorthOffset: Float = 0.0f
}
```

**위험 시나리오**:
1. UDP 스레드 (30ms 간격)가 각도 읽기
2. 추적 스레드가 각도 쓰기
3. race condition으로 중간 값 읽힘 → 펌웨어에 잘못된 각도 전송

### 2.3.2 lateinit var 초기화 검증 부재 (**High - 신규**)

| 파일 | 변수 | 타입 | 초기화 시점 | 미초기화 시 |
|------|------|------|------------|------------|
| UdpFwICDService.kt:45 | `channel` | DatagramChannel | @PostConstruct | **서버 크래시** |
| UdpFwICDService.kt:54 | `firmwareIp` | String | @Value | 빈 문자열로 fallback |
| UdpFwICDService.kt:60 | `serverIp` | String | @Value | 빈 문자열로 fallback |
| OrekitConfig.kt:31 | `orekitDataPath` | String | @Value | 계산 불가 |
| SolarOrekitCalculator.kt:37 | `groundStation` | TopocentricFrame | initialize() | NPE |
| PerformanceFilter.kt:16 | `performanceController` | PerformanceController | @Autowired | NPE |

**문제점**: `::channel.isInitialized` 검증 없이 접근하면 `UninitializedPropertyAccessException` 발생

### 2.4 .subscribe() fire-and-forget (**High**) [신규]

| 현황 | 건수 | 리스크 |
|------|:----:|--------|
| `.subscribe()` 에러 핸들러 누락 | **19개** | 에러 무시, 디버깅 불가 |
| Critical 위치 | **2건** | UdpFwICDService.kt:933, :195 |

```kotlin
// 현재 (문제) - 에러 처리 누락
someFlux.subscribe()

// 수정 필요
someFlux.subscribe(
    { result -> /* success */ },
    { error -> logger.error("Error", error) }
)
```

### 2.5 print/println 잔재 (Medium)

| 현황 | 리스크 |
|------|--------|
| System.out.print/println **102건** | 프로덕션 노출 |
| 주요 파일: ElevationCalculator, InitService, ICDService | 로그 분석 불가 |

### 2.6 전수조사 결과 (2026-01-13)

> **조사 범위**: BE 66개 파일 (33,284줄)

#### A. 광범위 catch (Broad Exception Catch) - **High**

| 현황 | 건수 | 리스크 |
|------|------|--------|
| `catch (Exception e)` | **180+건** | 예외 정보 손실, 디버깅 어려움 |
| 빈 catch 블록 | 다수 | 오류 무시, 예상치 못한 동작 |

```kotlin
// 현재 (문제)
try {
    doSomething()
} catch (e: Exception) {
    // 너무 광범위, 어떤 예외인지 알 수 없음
}

// 개선 필요
try {
    doSomething()
} catch (e: SpecificException) {
    logger.error("명확한 예외 처리", e)
    throw CustomException("사용자 친화적 메시지", e)
}
```

#### B. 리소스 누수 가능성 - **Medium**

| 파일 | 위치 | 문제 |
|------|------|------|
| EphemerisService.kt | HttpURLConnection | **close() 주석 처리됨** |
| ICDService.kt | DatagramChannel | **close() 주석 처리됨** |

```kotlin
// 현재 (누수 위험)
val connection = url.openConnection() as HttpURLConnection
// ... 사용
// connection.disconnect()  // 주석 처리됨!

// 수정 필요
url.openConnection().use { connection ->
    // ... 사용
}  // 자동 close
```

#### C. 테스트 코드 혼재 - **High**

| 파일 | 위치 | 문제 |
|------|------|------|
| OrekitCalculatorTest.kt | main/ 폴더 | **595줄 테스트 코드가 main에 있음** |

**즉시 조치 필요**: test/ 폴더로 이동

---

## 3. 제안 (Proposal)

### 3.1 Phase 1: 입력 검증 추가 (Critical!)

#### Request DTO 생성 + 검증 어노테이션

```kotlin
// Request DTO 생성 + 검증 어노테이션
data class TrackingCommandRequest(
    @field:Min(-360) @field:Max(360)
    val azimuth: Double,

    @field:Min(-90) @field:Max(90)
    val elevation: Double,

    @field:Min(-360) @field:Max(360)
    val train: Double
)

// 커스텀 검증 어노테이션
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AngleRangeValidator::class])
annotation class AngleRange(val min: Double, val max: Double)
```

#### 체크리스트

- [ ] ICDController 파라미터 검증 추가
  - [ ] 각도 범위 (-360° ~ 360°)
  - [ ] 속도 범위 (음수 방지)
- [ ] EphemerisController 파라미터 검증 추가
- [ ] PassScheduleController 파라미터 검증 추가
- [ ] GlobalExceptionHandler에 MethodArgumentNotValidException 처리

### 3.2 Phase 2: Repository 추상화

#### Repository 인터페이스 정의

```kotlin
// Repository 인터페이스 정의
interface TrackingDataRepository {
    suspend fun save(data: TrackingData): TrackingData
    suspend fun findByPassId(passId: Long): List<TrackingData>
    suspend fun findByDateRange(start: LocalDateTime, end: LocalDateTime): List<TrackingData>
}

// 메모리 구현 (현재 동작 유지)
class MemoryTrackingRepository : TrackingDataRepository {
    private val storage = ConcurrentHashMap<Long, TrackingData>()
    // ...
}

// DB 구현 (RFC-001)
class DatabaseTrackingRepository(
    private val r2dbcTemplate: R2dbcEntityTemplate
) : TrackingDataRepository {
    // ...
}
```

#### 체크리스트

- [ ] TrackingDataRepository 인터페이스 정의
- [ ] MemoryTrackingRepository 구현
- [ ] EphemerisService에 DI
- [ ] PassScheduleService에 DI
- [ ] 단위 테스트용 MockRepository 준비

### 3.3 Phase 3: GlobalData 체계화

#### ConfigurationService로 캡슐화

```kotlin
// ConfigurationService로 캡슐화
@Service
class ConfigurationService(
    private val eventPublisher: ApplicationEventPublisher
) {
    private val offsets = AtomicReference(OffsetConfig())

    fun getOffset(): OffsetConfig = offsets.get()

    fun setOffset(newOffset: OffsetConfig) {
        val old = offsets.getAndSet(newOffset)
        eventPublisher.publishEvent(OffsetChangedEvent(old, newOffset))
    }
}

// 이벤트 기반 알림
data class OffsetChangedEvent(
    val oldValue: OffsetConfig,
    val newValue: OffsetConfig
)
```

#### 체크리스트

- [ ] ConfigurationService 생성
- [ ] GlobalData.Offset 마이그레이션
- [ ] 변경 이벤트 발행
- [ ] 구독자 패턴 적용

### 3.4 Phase 4: print/println 제거 (102건)

#### 체크리스트

- [ ] ElevationCalculator.kt print → logger.debug
- [ ] InitService.kt print → logger.info
- [ ] ICDService.kt print → logger.debug
- [ ] 기타 파일 순차 정리

### 3.5 Phase 5: 예외 처리 개선 (180+건) [전수조사 추가]

#### 개선 전략

```kotlin
// 1. 구체적 예외 타입으로 변경
catch (e: IOException) { ... }
catch (e: IllegalArgumentException) { ... }

// 2. 커스텀 예외 정의
sealed class AcsException(message: String, cause: Throwable?) : Exception(message, cause) {
    class TrackingException(message: String, cause: Throwable? = null) : AcsException(message, cause)
    class CommunicationException(message: String, cause: Throwable? = null) : AcsException(message, cause)
}

// 3. GlobalExceptionHandler에서 통합 처리
@ExceptionHandler(AcsException::class)
fun handleAcsException(e: AcsException): ResponseEntity<ErrorResponse>
```

#### 체크리스트

- [ ] 우선순위 분류 (Critical: API 레이어, Medium: Service, Low: 기타)
- [ ] 커스텀 예외 클래스 정의
- [ ] catch 블록별 적절한 예외 타입 적용
- [ ] 로깅 추가 (최소 logger.error)
- [ ] GlobalExceptionHandler 확장

### 3.6 Phase 6: 리소스 누수 수정 [전수조사 추가]

#### 수정 대상

| 파일 | 리소스 | 수정 방법 |
|------|--------|----------|
| EphemerisService.kt | HttpURLConnection | `.use { }` 블록 적용 |
| ICDService.kt | DatagramChannel | `.use { }` 블록 적용 |

#### 코드 예시

```kotlin
// Before (누수)
val connection = url.openConnection() as HttpURLConnection
connection.requestMethod = "GET"
val response = connection.inputStream.bufferedReader().readText()
// connection.disconnect() 주석 처리됨

// After (안전)
(url.openConnection() as HttpURLConnection).use { connection ->
    connection.requestMethod = "GET"
    connection.inputStream.bufferedReader().use { reader ->
        reader.readText()
    }
}
```

#### 체크리스트

- [ ] EphemerisService.kt HttpURLConnection → use 적용
- [ ] ICDService.kt DatagramChannel → use 적용
- [ ] 다른 리소스 누수 지점 확인
- [ ] OrekitCalculatorTest.kt → test/ 폴더로 이동

### 3.7 Phase 7: GlobalData 동시성 안전화 (**Critical - 신규**)

> **우선순위**: P0 - 추적 중 잘못된 각도가 펌웨어로 전송될 수 있음

#### 수정 전략

**✅ 확정: 개별 AtomicReference 적용**

> **결정 사유**: 30ms UDP 갱신 주기로 인해 일시적 불일치가 자동 보정됨.
> 불변 data class 방식(옵션 B)은 API 변경 및 복잡도 증가로 불필요함.

```kotlin
// 변경 전 (위험)
object Offset {
    var TimeOffset: Float = 0.0f
    var azimuthPositionOffset: Float = 0.0f
    // ...
}

// 변경 후 (안전) - 개별 AtomicReference 사용
object Offset {
    private val _timeOffset = AtomicReference(0.0f)
    private val _azimuthPositionOffset = AtomicReference(0.0f)
    private val _elevationPositionOffset = AtomicReference(0.0f)
    private val _trainPositionOffset = AtomicReference(0.0f)
    private val _trueNorthOffset = AtomicReference(0.0f)

    var TimeOffset: Float
        get() = _timeOffset.get()
        set(value) = _timeOffset.set(value)

    var azimuthPositionOffset: Float
        get() = _azimuthPositionOffset.get()
        set(value) = _azimuthPositionOffset.set(value)

    // ... 나머지 동일 패턴 (총 5개)
}

// EphemerisTrakingAngle (3개), SunTrackingData (6개)도 동일 패턴 적용
```

**💡 참고: 불변 data class 방식 (적용 안 함)**

> 원자적 스냅샷이 필요한 경우를 위해 기록만 남김. 현재 ACS에서는 불필요.

<details>
<summary>접기/펼치기</summary>

```kotlin
// 전체를 불변 객체로 묶어서 원자적 교체 (미적용)
data class OffsetConfig(
    val timeOffset: Float = 0.0f,
    val azimuthPositionOffset: Float = 0.0f,
    val elevationPositionOffset: Float = 0.0f,
    val trainPositionOffset: Float = 0.0f,
    val trueNorthOffset: Float = 0.0f
)
```

</details>

#### 체크리스트

- [ ] **Offset** 5개 필드 AtomicReference 적용
- [ ] **EphemerisTrakingAngle** 3개 필드 AtomicReference 적용
- [ ] **SunTrackingData** 6개 필드 AtomicReference 적용
- [ ] Time (2개) - 우선순위 낮음, 드물게 변경
- [ ] Version (2개) - 초기화 후 변경 없음, 스킵 가능
- [ ] 기존 호출 코드 호환성 테스트

### 3.8 Phase 8: lateinit 초기화 검증 추가 (**High - 신규**)

#### 수정 대상

| 파일 | 변수 | 수정 방법 |
|------|------|----------|
| UdpFwICDService.kt | channel | `::channel.isInitialized` 검증 추가 |
| SolarOrekitCalculator.kt | groundStation | `lazy` 패턴으로 변경 권장 |

#### 코드 예시

```kotlin
// 변경 전 (위험)
private lateinit var channel: DatagramChannel

fun sendData() {
    channel.send(data)  // UninitializedPropertyAccessException 가능
}

// 변경 후 (안전)
private lateinit var channel: DatagramChannel

fun sendData() {
    if (!::channel.isInitialized) {
        logger.error("채널이 초기화되지 않았습니다")
        return
    }
    channel.send(data)
}

// 또는 lazy 패턴 (권장)
private val channel: DatagramChannel by lazy {
    DatagramChannel.open().apply {
        configureBlocking(false)
    }
}
```

#### 체크리스트

- [ ] UdpFwICDService.kt channel 검증 추가
- [ ] SolarOrekitCalculator.kt groundStation → lazy 변환
- [ ] 기타 lateinit 사용 위치 검토

---

## 4. 영향 분석 (Impact)

### 4.1 변경 범위

| 영역 | 변경 | 영향 |
|------|------|------|
| 입력 검증 | Request DTO + @Valid | 안정성 향상 |
| Repository | 인터페이스 추상화 | 테스트 가능성 향상 |
| GlobalData | Service 캡슐화 | 상태 관리 명확화 |
| print/println | logger 교체 | 운영 로그 품질 향상 |

### 4.2 위험 요소

| 위험 | 대응 |
|------|------|
| 기존 API 동작 변경 | 기존 동작 유지, 검증만 추가 |
| Repository 교체 복잡도 | 메모리 구현부터, 점진적 |

---

## 5. 마이그레이션 (Migration)

### 5.1 단계별 적용

```
Phase 1: 입력 검증 (P0 - 보안 Critical)
├── ICDController 검증 추가
├── EphemerisController 검증 추가
└── GlobalExceptionHandler 확장

Phase 2: Repository 추상화 (P1)
├── 인터페이스 정의
├── 메모리 구현
└── 서비스 DI

Phase 3: GlobalData 체계화 (P2)
├── ConfigurationService 생성
└── 마이그레이션

Phase 4: print/println 제거 (P2)
└── 파일별 순차 정리

Phase 5: 예외 처리 개선 (P2) [전수조사 추가]
├── 커스텀 예외 클래스 정의
├── 광범위 catch → 구체적 예외
└── GlobalExceptionHandler 확장

Phase 6: 리소스 누수 수정 (P1) [전수조사 추가]
├── HttpURLConnection use 적용
├── DatagramChannel use 적용
└── OrekitCalculatorTest.kt 이동

Phase 7: GlobalData 동시성 안전화 (P0) [신규]
├── Offset 5개 필드 AtomicReference 적용
├── EphemerisTrakingAngle 3개 필드 적용
├── SunTrackingData 6개 필드 적용
└── 기존 호출 코드 호환성 테스트

Phase 8: lateinit 초기화 검증 (P1) [신규]
├── UdpFwICDService channel 검증
├── SolarOrekitCalculator groundStation → lazy
└── 기타 lateinit 검토
```

---

## 6. 관련 RFC

| RFC | 관계 | 설명 |
|-----|------|------|
| [RFC-001](./RFC-001_Database_Strategy.md) | 연관 | Repository 인터페이스 + DB 구현 연계 |
| [RFC-002](./RFC-002_Logging_System.md) | 연관 | print/println → logger 교체 |
| [RFC-004](./RFC-004_API_Standardization.md) | 분리됨 | 기존 Phase 6이 이 RFC로 분리됨 |
| RFC-005 (예정) | 후속 | Repository 추상화 → 테스트 작성 가능 |

### 의존성 그래프

```
RFC-001 (DB) ──────────────────────────┐
    │                                  │
    ▼                                  ▼
RFC-002 (로깅) ←───────────────→ RFC-007 (이 문서)
                                       │
                                       ├── Phase 1: 입력 검증 (P0)
                                       ├── Phase 2: Repository 추상화 (P1)
                                       ├── Phase 3: GlobalData (P2)
                                       └── Phase 4: print/println (P2)
                                               │
                                               ▼
                                       RFC-005 (테스트)
```

---

**작성자**: Claude
**검토자**: -
**승인일**: -
