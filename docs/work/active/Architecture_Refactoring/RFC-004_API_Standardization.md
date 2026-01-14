# RFC-004: API 표준화

> **버전**: 1.6.0 | **작성일**: 2026-01-13
> **상태**: Draft | **우선순위**: P1
> **역할**: BE API 표준화 (응답 형식, 동기/비동기 통일, 에러 처리, Critical 버그 수정)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.6.0 | 2026-01-13 | 문서 역할 명확화: Phase 6 → RFC-007로 분리, Phase 4-5 FE 관련 → RFC-008로 이관 명시 |
| 1.5.0 | 2026-01-13 | Phase 6 추가: 놓친 BE 영역 (→ RFC-007로 분리됨) |
| 1.4.0 | 2026-01-13 | 관련 RFC 섹션 추가, RFC-008과의 중복 해소 명시, 실행 순서 정의 |
| 1.3.0 | 2026-01-13 | Phase 5 추가: BE 데이터 모델 개선 (displayValue), CMD/ACTUAL 중복 4개 파일로 확대 (300줄+) |
| 1.2.0 | 2026-01-13 | Phase 4 추가: console.log 정리 (988개), useAxisValue composable, health 체크 결과 반영 |
| 1.1.0 | 2026-01-13 | 동기/비동기 통일 상세 계획 추가: 전체 메서드 분석, SettingsController @Transactional 처리, Phase 2 체크리스트 상세화 |
| 1.0.0 | 2026-01-13 | 초기 작성 |

---

## 1. 배경 (Context)

### 왜 이 변경이 필요한가?

현재 ACS API는 다음과 같은 일관성 문제를 가지고 있습니다:

| 문제 | 영향 |
|-----|------|
| 응답 형식 혼재 | `status` vs `success` 필드 혼용 |
| 동기/비동기 혼합 | WebFlux 환경에서 `ResponseEntity` vs `Mono` 혼용 |
| 에러 처리 불일치 | 일부 컨트롤러 예외 처리 누락 |
| 보안 취약점 | Path Traversal 등 잠재적 위험 |
| 동시성 버그 | 스레드 안전하지 않은 코드 |

### 목표

- 응답 형식 **통일** (단일 표준 채택)
- 동기/비동기 패턴 **일관성** 확보
- Critical 이슈 **즉시 수정**
- API 품질 및 안정성 **고도화**

---

## 2. 현재 상태 (Current State)

### 2.1 컨트롤러 현황 (9개)

| 컨트롤러 | 줄 수 | 동기/비동기 | 주요 문제 |
|---------|------|------------|----------|
| PassScheduleController | 1,558 | 혼합 | 대형 파일, 매핑 없는 메서드 |
| EphemerisController | 1,092 | 혼합 | 동기 4개 / 비동기 10개 |
| ICDController | 664+ | 동기 | `"success":"true"` String |
| SunTrackController | ~100 | 비동기 | 예외 처리 없음 |
| HardwareErrorLogController | ~200 | 동기 | 응답 형식 불일치 |
| LoggingController | ~180 | 동기 | **Path Traversal 취약점** |
| SettingsController | ~300 | 동기 | 정상 |
| PushDataController | ~150 | WebSocket | 동기화 없는 변수 |
| PerformanceController | ~320 | 동기 | **동시성 버그** |

### 2.2 응답 형식 혼재

```kotlin
// 패턴 1: status 필드 (다수)
mapOf("status" to "success", "data" to result)

// 패턴 2: success 필드 (일부)
mapOf("success" to "true")  // ⚠️ Boolean이 아닌 String!

// 패턴 3: 직접 데이터 반환 (일부)
ResponseEntity.ok(resultData)
```

### 2.3 동기/비동기 패턴 분석

#### EphemerisController 상세

| 유형 | 개수 | 메서드 |
|-----|-----|-------|
| 비동기 (Mono) | 10 | startEphemerisTracking, stopEphemerisTracking 등 |
| 동기 (ResponseEntity) | 4 | setCurrentTrackingPassId, timeOffsetCommand, calculateAxisTransform |

**문제점**: WebFlux 환경에서 동기 메서드는 스레드 블로킹 위험

### 2.4 Critical 이슈 상세

#### Issue #1: String 타입 Boolean (ICDController.kt:664)

```kotlin
// 현재 (문제)
mapOf("success" to "true")  // String

// 수정
mapOf("success" to true)    // Boolean
```

#### Issue #2: 미사용 파라미터 (SunTrackController.kt:29)

```kotlin
// 현재
@RequestParam interval: Int
// interval 변수가 함수 내에서 사용되지 않음
```

#### Issue #3: 예외 처리 누락 (SunTrackController.kt:48-65)

```kotlin
// 현재 - try-catch 없음
fun startSunTracking(): Mono<Map<String, Any>> {
    return sunTrackService.startTracking()  // 예외 시 500 에러
}
```

#### Issue #4: Path Traversal (LoggingController.kt:172-173) ⚠️ 보안

```kotlin
// 현재 (취약)
@GetMapping("/download/{filename}")
fun downloadLog(@PathVariable filename: String): ResponseEntity<Resource> {
    val file = File("logs/$filename")  // ../../../etc/passwd 가능!
}
```

#### Issue #5: 동시성 버그 (PerformanceController.kt:299-305)

```kotlin
// 현재 (문제)
val metrics = HashMap<String, Any>()  // 스레드 안전하지 않음
// 여러 스레드에서 동시 접근 시 ConcurrentModificationException
```

#### Issue #6: 동기화 없는 가변 변수 (PushDataController.kt:57-58)

```kotlin
// 현재
private var sessions = mutableListOf<WebSocketSession>()
// 동시 접근 시 문제 가능
```

#### Issue #7: 매핑 없는 메서드 (PassScheduleController.kt:1356)

```kotlin
// @GetMapping 등 매핑 어노테이션 없이 public 메서드 존재
// 의도적인지 실수인지 확인 필요
```

---

## 3. 제안 (Proposal)

### 3.1 즉시 수정 (Critical Issues)

| 이슈 | 수정 방법 | 긴급도 |
|-----|----------|--------|
| Path Traversal | 파일명 검증 로직 추가 | **P0** |
| 동시성 버그 | ConcurrentHashMap 사용 | P1 |
| String Boolean | true → Boolean 타입 | P1 |
| 예외 처리 | onErrorResume 추가 | P1 |
| 동기화 | CopyOnWriteArrayList 사용 | P2 |

#### Path Traversal 수정 예시

```kotlin
// 수정 후
@GetMapping("/download/{filename}")
fun downloadLog(@PathVariable filename: String): ResponseEntity<Resource> {
    // 보안: 파일명 검증
    if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
        return ResponseEntity.badRequest().build()
    }
    val file = File("logs/$filename")
    // ...
}
```

### 3.2 응답 형식 표준화

#### 표준 응답 구조

```kotlin
// 성공 응답
data class ApiResponse<T>(
    val status: String = "success",
    val data: T? = null,
    val message: String? = null
)

// 에러 응답
data class ApiErrorResponse(
    val status: String = "error",
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)
```

#### 적용 예시

```kotlin
// Before
mapOf("success" to "true", "result" to data)

// After
ApiResponse(data = data)
```

### 3.3 동기/비동기 패턴 통일

#### 변환 목적

> **중요**: 비동기 변환은 **성능 최적화**가 아닌 **일관성 확보**가 목적입니다.
> 현재 동기 메서드들은 대부분 가벼운 메모리 작업이므로 실질적 성능 향상은 미미합니다.

#### 권장 패턴 (WebFlux 환경)

```kotlin
// 모든 컨트롤러 메서드는 Mono/Flux 반환
@GetMapping("/example")
fun example(): Mono<ApiResponse<ResultData>> {
    return service.process()
        .map { ApiResponse(data = it) }
        .onErrorResume { e ->
            Mono.just(ApiResponse(status = "error", message = e.message))
        }
}
```

#### 전체 동기 메서드 분석 결과

| 분류 | 개수 | 설명 |
|-----|-----|------|
| **필수 변환** | 2개 | 파일 I/O 블로킹 |
| **권장 변환** | 4개 | CPU 집약적 계산 |
| **선택적 변환** | ~60개 | 메모리 작업 (효과 미미) |

#### 필수 변환 대상 (파일 I/O)

| 컨트롤러 | 메서드 | 이유 | 변환 방법 |
|---------|-------|------|----------|
| LoggingController | `getLogFiles` | `Files.list()`, `Files.walk()` 블로킹 | `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())` |
| LoggingController | `downloadLogFile` | `Files.readAllBytes()` 블로킹 | `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())` |

#### 권장 변환 대상

| 컨트롤러 | 메서드 | 이유 | 변환 방법 |
|---------|-------|------|----------|
| EphemerisController | `setCurrentTrackingPassId` | 일관성 | `Mono.fromCallable()` |
| EphemerisController | `timeOffsetCommand` | 일관성 | `Mono.fromCallable()` |
| EphemerisController | `calculateAxisTransform` | CPU 계산 (Orekit) | `Mono.fromCallable()` |

#### 선택적 변환 대상 (전체 통일 시)

| 컨트롤러 | 동기 메서드 수 | 내부 동작 | 변환 효과 |
|---------|--------------|----------|----------|
| ICDController | 14개 | UDP fire-and-forget (내부 이미 비동기) | 미미 |
| HardwareErrorLogController | 5개 | 순수 메모리 작업 | 미미 |
| SettingsController | 18개 | 메모리 캐시 + 선택적 DB | 중간 |
| PerformanceController | 6개 | JMX/메모리 조회 | 미미 |
| PassScheduleController | 25개 | 메모리 작업 | 미미 |

#### SettingsController 특별 처리

```kotlin
// 현재: 컨트롤러와 서비스 모두 @Transactional
@Transactional  // ← 제거 대상 (중복)
class SettingsController

@Transactional  // ← 유지 (실제 DB 작업)
class SettingsService
```

**조치사항**:
1. SettingsController의 `@Transactional` 제거 (중복)
2. 트랜잭션은 SettingsService에서만 관리
3. 컨트롤러는 비동기 변환 가능

#### 변환 예시 코드

```kotlin
// Before (동기)
@GetMapping("/files")
fun getLogFiles(): ResponseEntity<List<LogFileInfo>> {
    val files = loggingService.getLogFiles()  // Files.list() 블로킹
    return ResponseEntity.ok(files)
}

// After (비동기)
@GetMapping("/files")
fun getLogFiles(): Mono<ResponseEntity<List<LogFileInfo>>> {
    return Mono.fromCallable {
        loggingService.getLogFiles()
    }
    .subscribeOn(Schedulers.boundedElastic())  // I/O 작업용 스케줄러
    .map { files -> ResponseEntity.ok(files) }
    .onErrorResume { e ->
        Mono.just(ResponseEntity.status(500).body(emptyList()))
    }
}
```

### 3.4 에러 처리 표준화

```kotlin
// GlobalExceptionHandler.kt
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): Mono<ResponseEntity<ApiErrorResponse>> {
        return Mono.just(ResponseEntity.badRequest().body(
            ApiErrorResponse(code = "BAD_REQUEST", message = e.message ?: "Invalid request")
        ))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): Mono<ResponseEntity<ApiErrorResponse>> {
        logger.error("Unhandled exception", e)
        return Mono.just(ResponseEntity.status(500).body(
            ApiErrorResponse(code = "INTERNAL_ERROR", message = "Internal server error")
        ))
    }
}
```

---

## 4. 대안 (Alternatives)

### 4.1 응답 형식: status vs success

| 옵션 | 장점 | 단점 |
|-----|------|------|
| **status 필드 (채택)** | 명확함 (success/error/pending) | 현재 다수 사용 중 |
| success 필드 | 간결함 (true/false) | Boolean만 표현 가능 |

**결정**: `status` 필드 사용 (다수 API에서 이미 사용 중)

### 4.2 동기 유지 vs 비동기 통일

| 옵션 | 장점 | 단점 |
|-----|------|------|
| 현상 유지 | 변경 없음 | 일관성 없음, 성능 저하 위험 |
| **비동기 통일 (채택)** | WebFlux 최적화, 일관성 | 변환 작업 필요 |

**결정**: 점진적으로 비동기 통일 (리팩토링 시 적용)

---

## 5. 영향 분석 (Impact)

### 5.1 변경 범위

| 영역 | 변경 내용 | 영향 |
|-----|----------|------|
| Critical 이슈 | 7개 즉시 수정 | 안정성 향상 |
| 응답 형식 | ApiResponse 클래스 도입 | 일관성 확보 |
| 에러 처리 | GlobalExceptionHandler | 중앙화 |

### 5.2 Frontend 영향

```typescript
// 변경 전: 다양한 응답 형식 처리
if (response.success === "true") { ... }
if (response.status === "success") { ... }

// 변경 후: 단일 형식
if (response.status === "success") {
    const data = response.data;
}
```

### 5.3 위험 요소

| 위험 | 대응 |
|-----|------|
| Frontend 호환성 | 점진적 적용, 기존 API 유지 기간 설정 |
| 테스트 부족 | Critical 이슈 수정 후 수동 테스트 |

---

## 6. 마이그레이션 (Migration)

### 6.1 단계별 적용

```
Phase 0: Critical 이슈 즉시 수정 (1-2일)
├── Path Traversal 보안 패치
├── 동시성 버그 수정
├── String Boolean 수정
└── 예외 처리 추가

Phase 1: 응답 클래스 도입 (점진적)
├── ApiResponse, ApiErrorResponse 클래스 생성
├── 새로운 API부터 적용
└── 기존 API 점진적 마이그레이션

Phase 2: 동기 → 비동기 통일 (전체)
├── Step 1: SettingsController @Transactional 제거
├── Step 2: 필수 변환 (LoggingController 2개 - 파일 I/O)
├── Step 3: 권장 변환 (EphemerisController 3개)
├── Step 4: 전체 통일 (ICDController 14개)
├── Step 5: 전체 통일 (HardwareErrorLogController 5개)
├── Step 6: 전체 통일 (SettingsController 18개)
├── Step 7: 전체 통일 (PerformanceController 6개)
├── Step 8: 전체 통일 (PassScheduleController 25개)
└── 빌드 및 테스트

Phase 3: GlobalExceptionHandler 적용
├── 공통 예외 처리기 구현
├── 컨트롤러별 try-catch 제거
└── 에러 응답 표준화

Phase 4: 프론트엔드 코드 정리 (P1)
├── console.log 정리 (988개 → 개발 모드만)
├── useAxisValue composable 생성 (CMD/ACTUAL 중복 제거)
│   ├── DashboardPage.vue (10회 중복)
│   ├── EphemerisDesignationPage.vue (8회 중복)
│   ├── PassSchedulePage.vue (3회 중복)
│   └── ephemerisTrackStore.ts (2회 중복)
└── 디버깅 코드 정리

Phase 5: BE 데이터 모델 개선 (P2)
├── displayCmdValue/displayActualValue 필드 추가 (BE)
├── FE에서 displayValue 직접 사용
└── 기존 trackingCMD vs cmd 로직 제거
```

### 6.2 롤백 계획

- Phase 0: git revert로 즉시 롤백
- Phase 1-3: 점진적 적용이므로 영향 최소화

---

## 7. 검증 (Verification)

### 7.1 체크리스트

**Phase 0 (Critical)**
- [ ] LoggingController Path Traversal 수정
- [ ] PerformanceController 동시성 버그 수정
- [ ] ICDController String Boolean 수정
- [ ] SunTrackController 예외 처리 추가
- [ ] PushDataController 동기화 추가
- [ ] 빌드 성공 확인
- [ ] 기본 기능 테스트

**Phase 1 (응답 표준화)**
- [ ] ApiResponse 클래스 생성
- [ ] ApiErrorResponse 클래스 생성
- [ ] 새 API에 적용

**Phase 2 (동기 → 비동기 통일)**
- [ ] SettingsController `@Transactional` 제거
- [ ] LoggingController.getLogFiles 비동기 변환
- [ ] LoggingController.downloadLogFile 비동기 변환
- [ ] EphemerisController 3개 메서드 비동기 변환
- [ ] ICDController 14개 메서드 비동기 변환
- [ ] HardwareErrorLogController 5개 메서드 비동기 변환
- [ ] SettingsController 18개 메서드 비동기 변환
- [ ] PerformanceController 6개 메서드 비동기 변환
- [ ] PassScheduleController 25개 메서드 비동기 변환
- [ ] 빌드 성공 확인
- [ ] 기본 기능 테스트

**Phase 3 (에러 처리)**
- [ ] GlobalExceptionHandler 구현
- [ ] 컨트롤러별 try-catch 정리
- [ ] Frontend 호환성 확인

**Phase 4 (프론트엔드 정리)**
- [ ] console.log 정리 (988개)
  - [ ] PassSchedulePage.vue (128개)
  - [ ] passScheduleStore.ts (103개)
  - [ ] SelectScheduleContent.vue (80개)
  - [ ] TLEUploadContent.vue (64개)
  - [ ] EphemerisDesignationPage.vue (63개)
  - [ ] DashboardPage.vue (60개)
  - [ ] 기타 파일들 (490개)
- [ ] useAxisValue composable 생성 (총 300줄+ 중복 제거)
  - [ ] DashboardPage.vue (10회 → composable 사용)
  - [ ] EphemerisDesignationPage.vue (8회 → composable 사용)
  - [ ] PassSchedulePage.vue (3회 → composable 사용)
  - [ ] ephemerisTrackStore.ts (2회 → composable 사용)
- [ ] 디버깅용 watch 정리

**Phase 5 (BE 데이터 모델 개선)**
- [ ] ICDService.kt에 displayValue 계산 로직 추가
- [ ] WebSocket 메시지에 displayCmdValue 필드 추가
- [ ] FE useAxisValue에서 displayValue 직접 사용
- [ ] 기존 trackingCMD/cmd 분기 로직 제거
- [ ] 빌드 및 테스트

### 7.2 성공 기준

| 기준 | 측정 방법 |
|-----|----------|
| Critical 이슈 0개 | 코드 리뷰 |
| 응답 형식 통일 | 새 API 100% 표준 준수 |
| 빌드 성공 | ./gradlew clean build |

---

## 8. 결정 사항 요약

| 항목 | 결정 | 비고 |
|-----|------|------|
| 응답 형식 | `status` 필드 통일 | success/error/pending |
| 동기/비동기 | 비동기(Mono) 통일 | 점진적 적용 |
| 에러 처리 | GlobalExceptionHandler | 중앙화 |
| Critical 수정 | **즉시 적용** | P0 우선순위 |

---

## 9. 부록: 전체 컨트롤러 분석 결과

### 9.1 컨트롤러별 상세

#### ICDController
- **위치**: controller/ICDController.kt
- **패턴**: 동기 (ResponseEntity)
- **이슈**: `"success" to "true"` String 사용 (664줄)
- **권장**: Boolean 타입으로 수정

#### EphemerisController
- **위치**: controller/mode/EphemerisController.kt (1,092줄)
- **패턴**: 혼합 (비동기 10개 / 동기 4개)
- **동기 메서드**:
  - setCurrentTrackingPassId (116줄)
  - timeOffsetCommand (138줄)
  - calculateAxisTransform (297줄)
- **권장**: Mono.fromCallable()로 비동기 변환

#### SunTrackController
- **위치**: controller/mode/SunTrackController.kt
- **패턴**: 비동기 (Mono)
- **이슈**:
  - interval 파라미터 미사용 (29줄)
  - 예외 처리 완전 누락 (48-65줄)
- **권장**: onErrorResume 추가

#### LoggingController
- **위치**: controller/LoggingController.kt
- **패턴**: 동기
- **이슈**: **Path Traversal 보안 취약점** (172-173줄)
- **권장**: 즉시 수정 필수

#### PerformanceController
- **위치**: controller/PerformanceController.kt
- **패턴**: 동기
- **이슈**: **동시성 버그** - HashMap 사용 (299-305줄)
- **권장**: ConcurrentHashMap으로 교체

#### PushDataController
- **위치**: controller/PushDataController.kt
- **패턴**: WebSocket
- **이슈**: 동기화 없는 mutableList (57-58줄)
- **권장**: CopyOnWriteArrayList 사용

#### PassScheduleController
- **위치**: controller/mode/PassScheduleController.kt (1,558줄)
- **패턴**: 혼합
- **이슈**:
  - 대형 파일 (분리 필요)
  - 매핑 없는 메서드 (1356줄)
- **권장**: 파일 분리 검토

---

## 9-1. Phase 6: BE 인프라 → RFC-007로 분리됨

> **분리 완료**: 이 섹션의 내용은 [RFC-007_BE_Infrastructure.md](./RFC-007_BE_Infrastructure.md)로 이동했습니다.

이동된 항목:
- 입력 검증 (Critical!)
- Repository 추상화 (High)
- GlobalData 체계화 (Medium)
- print/println 제거 (Medium)

---

## 10. 관련 RFC

### 10.1 RFC 연계 현황

| RFC | 관계 | 설명 |
|-----|------|------|
| [RFC-001](./RFC-001_Database_Strategy.md) | **선행 필수** | DB 설정 완료 후 API 작업 가능 |
| [RFC-002](./RFC-002_Logging_System.md) | 병렬 | 로깅 설정과 API 표준화 병렬 진행 |
| [RFC-003](./RFC-003_State_Machine_Extraction.md) | 연관 | Thread.sleep/runBlocking 제거가 RFC-003 체크리스트에 포함 |
| [RFC-008](./RFC-008_Frontend_Restructuring.md) | **중복** | Phase 4-5 FE 작업은 RFC-008으로 통합 |
| RFC-007 (예정) | 후속 | 보안 인증/인가 추가 |

### 10.2 RFC-008과의 중복 해소

> **중요**: RFC-004 Phase 4-5의 FE 관련 작업은 RFC-008에서 통합 관리합니다.

| 이 문서 (RFC-004) | RFC-008 | 실행 담당 |
|-------------------|---------|-----------|
| Phase 4: console.log 정리 | Phase 3 | **RFC-008** |
| Phase 4: useAxisValue | Phase 1 | **RFC-008** |
| Phase 5: BE displayValue | - | **RFC-004** (이 문서) |

### 10.3 의존성 그래프

```
RFC-001 (DB) ─────────────────────────┐
    │                                 │
    ▼                                 ▼
RFC-002 (로깅) ←─────────────→ RFC-004 (API)
                                      │
                                      ├── Phase 0-3: BE API 표준화
                                      │
                                      └── Phase 5: BE displayValue
                                              │
                                              ▼
                                      RFC-008 Phase 1 (FE useAxisValue)
```

### 10.4 실행 순서

1. **RFC-001** (DB) - 모든 것의 기반
2. **RFC-004 Phase 0** (Critical 버그 수정) - 즉시 실행
3. **RFC-002** + **RFC-004 Phase 1-3** (병렬)
4. **RFC-008 Phase 1-3** (FE 구조화)
5. **RFC-004 Phase 5** (BE displayValue)
6. **RFC-008 Phase 4-5** (FE 품질/성능)
7. **RFC-003** (상태 머신 점진 개선)

---

**작성자**: Claude
**검토자**: -
**승인일**: -
