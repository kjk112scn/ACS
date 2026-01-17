# Kotlin Null 안전 처리

> Java의 NullPointerException 지옥에서 탈출하기

## 핵심 개념

Kotlin은 **컴파일 타임에 null을 체크**합니다.
변수 선언 시 null 가능 여부를 명시해야 합니다.

```kotlin
val name: String = "Kim"      // null 불가 (Non-null)
val name: String? = null      // null 가능 (Nullable)
```

---

## 연산자 정리

### 1. `?.` (Safe Call)
null이면 **전체 표현식이 null** 반환

```kotlin
val length = name?.length
// name이 null이면 → length도 null
// name이 "Kim"이면 → length는 3
```

**체이닝 가능**:
```kotlin
user?.address?.city?.name
// 중간에 하나라도 null이면 → 전체가 null
```

---

### 2. `?:` (Elvis 연산자)
null일 때 **기본값** 제공

```kotlin
val length = name?.length ?: 0
// name이 null이면 → 0
// name이 "Kim"이면 → 3

// early return에 활용
val user = getUser() ?: return
val user = getUser() ?: throw IllegalStateException("User not found")
```

**이름 유래**: `?:` 모양이 엘비스 프레슬리 머리 같다고...

---

### 3. `!!` (Non-null Assertion)
"**이거 절대 null 아니야!**"라고 컴파일러에게 강제

```kotlin
val length = name!!.length
// name이 null이면 → NullPointerException 터짐!
```

**⚠️ 주의**: 가능하면 사용하지 말 것. 사용해야 한다면:
- null 체크 직후
- 테스트 코드
- 정말 확실할 때만

---

### 4. `?.let { }` (Scope Function)
null이 **아닐 때만** 블록 실행

```kotlin
// Before (Java 스타일)
if (user != null) {
    sendEmail(user.email)
}

// After (Kotlin 스타일)
user?.let {
    sendEmail(it.email)  // it = user (null 아님 보장)
}

// 이름 지정 가능
user?.let { u ->
    sendEmail(u.email)
}
```

---

## 실전 패턴

### 패턴 1: Early Return
```kotlin
fun processUser(userId: String?) {
    val id = userId ?: return  // null이면 함수 종료
    val user = findUser(id) ?: return

    // 여기서는 user가 null 아님 보장
    doSomething(user)
}
```

### 패턴 2: 기본값과 함께
```kotlin
fun getDisplayName(user: User?): String {
    return user?.name ?: "Unknown"
}
```

### 패턴 3: 체이닝 + let
```kotlin
user?.address?.let { address ->
    println("주소: ${address.city}")
}
```

### 패턴 4: also (부수 효과)
```kotlin
user?.also {
    logger.info("User found: ${it.name}")
}?.process()
```

---

## ACS 프로젝트 실제 예시

```kotlin
// EphemerisService.kt에서
val pass = currentTrackingPass ?: return  // null이면 early return
val mstId = (pass["MstId"] as? Number)?.toLong()  // 안전한 캐스팅 + null 처리

// SunTrackService.kt에서
modeTask?.isCancelled ?: false  // null이면 false
getTrainOffsetCalculator()?.let { calc ->
    // calculator가 있을 때만 실행
}
```

---

## Java와 비교

| 상황 | Java | Kotlin |
|------|------|--------|
| null 체크 | `if (x != null)` | `x?.let { }` |
| 기본값 | `x != null ? x : default` | `x ?: default` |
| Optional | `Optional.ofNullable(x).map(...)` | `x?.transform()` |
| 강제 언래핑 | `x.get()` (Optional) | `x!!` |

---

## 흔한 실수

### ❌ 불필요한 !!
```kotlin
// 나쁜 예
if (user != null) {
    doSomething(user!!)  // 이미 체크했는데 !! 왜?
}

// 좋은 예
user?.let { doSomething(it) }
```

### ❌ 과도한 ?.
```kotlin
// 나쁜 예: 매번 체크
user?.name?.length?.toString()?.toInt()

// 좋은 예: 한 번에 처리
user?.let { u ->
    u.name.length.toString().toInt()
}
```

---

## 연습 문제

다음 Java 코드를 Kotlin으로 변환해보세요:

```java
// Java
String city = null;
if (user != null && user.getAddress() != null) {
    city = user.getAddress().getCity();
}
if (city == null) {
    city = "Unknown";
}
```

<details>
<summary>정답 보기</summary>

```kotlin
val city = user?.address?.city ?: "Unknown"
```
</details>

---

**다음 학습**: [kotlin-reactive.md](./kotlin-reactive.md) - WebFlux/리액티브 프로그래밍
