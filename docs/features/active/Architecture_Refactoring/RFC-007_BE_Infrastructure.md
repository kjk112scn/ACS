# RFC-007: BE 인프라 개선

> **버전**: 1.1.0 | **작성일**: 2026-01-13
> **상태**: Draft | **우선순위**: P2
> **역할**: BE 기반 인프라 (입력 검증, Repository 추상화, GlobalData 체계화, print/println 제거, 예외 처리 개선, 리소스 누수 수정)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
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

### 2.3 GlobalData 비체계적 관리 (Medium)

| 현황 | 리스크 |
|------|--------|
| GlobalData.kt에 모든 공유 데이터 집중 | 상태 변경 추적 어려움 |
| companion object 다수 (128개 추정) | 멀티스레드 race condition |
| 변경 이력 없음 | 예상치 못한 부작용 |

### 2.4 print/println 잔재 (Medium)

| 현황 | 리스크 |
|------|--------|
| System.out.print/println **102건** | 프로덕션 노출 |
| 주요 파일: ElevationCalculator, InitService, ICDService | 로그 분석 불가 |

### 2.5 전수조사 결과 (2026-01-13) [신규]

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
