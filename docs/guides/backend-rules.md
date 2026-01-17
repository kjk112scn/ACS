# Backend 코딩 규칙

> Kotlin 1.9 + Spring Boot 3.x + WebFlux

## Kotlin 규칙

### 필수
- Kotlin idiom 준수 (apply, let, also 활용)
- KDoc 주석 필수 (public 함수)
- data class 활용

### Null 안전
```kotlin
// Early Return
fun process(data: Data?) {
    val d = data ?: return
    // d는 null 아님 보장
}

// let 블록
currentPass?.let { pass ->
    updateStatus(pass)
}

// Elvis + 기본값
val name = user?.name ?: "Unknown"
```

## 계층 구조

```
Controller → Service → Algorithm → Repository
```

| 계층 | 책임 |
|------|------|
| Controller | 요청/응답 변환만 |
| Service | 비즈니스 로직 |
| Algorithm | 순수 계산 (외부 의존성 금지) |
| Repository | 데이터 접근 |

## 리액티브 (WebFlux)

### 필수 규칙
- Mono, Flux 반환
- suspend 함수 활용
- **블로킹 코드 금지** (runBlocking 등)

### 에러 처리
```kotlin
findUser(id)
    .switchIfEmpty(Mono.error(NotFoundException("User not found")))
    .onErrorResume(DbException::class.java) {
        logger.error("DB 오류", it)
        Mono.error(InternalException("Database error"))
    }
```

### subscribe 규칙
```kotlin
// 항상 에러 핸들러 포함
eventBus.subscribe(
    { event -> handle(event) },
    { error -> logger.error("처리 실패", error) }
)
```

## 예외 처리

- `GlobalExceptionHandler` 활용
- **광범위 `catch (Exception)` 금지** → 구체적 예외
- `.subscribe()` 에러 핸들러 필수

## 보안

- 입력 검증: `@Valid`, `@NotNull` 사용
- Path Traversal 주의 (파일 경로 검증)
- 하드코딩 비밀번호/키 금지

## 파일 구조

```
service/xxx/
├── XxxService.kt         # 메인 서비스
├── XxxStateMachine.kt    # 상태 관리 (필요 시)
└── XxxDataBatcher.kt     # 배치 처리 (필요 시)
```

## 네이밍

| 타입 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `EphemerisService` |
| 함수 | camelCase | `calculatePosition()` |
| 상수 | UPPER_SNAKE | `MAX_RETRY_COUNT` |
| DTO | PascalCase, 용도 접미사 | `TrackingRequest`, `TLEResponse` |

## 테스트

```bash
./gradlew test          # JUnit 테스트
```

---

**참조**: [ACS 패턴](../handbook/project/acs-patterns.md)
