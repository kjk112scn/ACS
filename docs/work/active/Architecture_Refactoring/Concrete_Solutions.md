# ACS v2.0 리팩토링 구체적 해결 방안

> **작성일**: 2026-01-14
> **목적**: 전문가 분석 결과를 바탕으로 문제별 구체적인 해결 코드 제시

---

## 목차

1. [Frontend 해결 방안](#1-frontend-해결-방안)
2. [Backend 해결 방안](#2-backend-해결-방안)
3. [추가 검토 필요 항목](#3-추가-검토-필요-항목)

---

## 1. Frontend 해결 방안

### 1.1 shallowRef 적용 (Phase 1 - Day 2)

#### 대상 파일: `frontend/src/stores/icd/icdStore.ts`

#### 변환 대상 및 우선순위

| 우선순위 | 변수명 | 라인 | 효과 | 난이도 |
|:-------:|--------|:----:|:----:|:-----:|
| 1 | `latestDataBuffer` | 1268 | **높음** | 쉬움 |
| 2 | `messageDelayStats` | 1302 | 중간 | 중간 |
| 3 | `updateIntervalStats` | 1313 | 중간 | 중간 |
| 4 | `performanceHistory` | 1561 | 중간 | 중간 |
| 5 | `timerStats` | 2219 | 낮음 | 쉬움 |

#### 코드 변환 예시

**1. import 수정**
```typescript
// 변경 전
import { ref, computed, onScopeDispose, readonly } from 'vue'

// 변경 후
import { ref, shallowRef, triggerRef, computed, onScopeDispose, readonly } from 'vue'
```

**2. latestDataBuffer 변환 (가장 중요)**
```typescript
// 변경 전 (라인 1268)
const latestDataBuffer = ref<MessageData | null>(null)

// 변경 후 - 코드 변경 없이 바로 적용 가능
const latestDataBuffer = shallowRef<MessageData | null>(null)
// 사용 패턴이 전체 교체이므로 안전함
```

**3. messageDelayStats 변환**
```typescript
// 변경 전 (라인 1302-1308)
const messageDelayStats = ref({
  min: Number.MAX_VALUE, max: 0, total: 0, count: 0, average: 0,
})

// 변경 후
const messageDelayStats = shallowRef({
  min: Number.MAX_VALUE, max: 0, total: 0, count: 0, average: 0,
})

// 업데이트 로직 수정 필요 (라인 1790-1796)
// 변경 전: 속성 직접 수정 (shallowRef에서 동작 안 함)
messageDelayStats.value.min = Math.min(...)
messageDelayStats.value.count++

// 변경 후: 객체 전체 교체
const newStats = {
  min: Math.min(messageDelayStats.value.min, messageDelay.value),
  max: Math.max(messageDelayStats.value.max, messageDelay.value),
  total: messageDelayStats.value.total + messageDelay.value,
  count: messageDelayStats.value.count + 1,
  average: 0,
}
newStats.average = newStats.total / newStats.count
messageDelayStats.value = newStats
```

**4. performanceHistory 변환**
```typescript
// 변경 전 (라인 1561)
const performanceHistory = ref<number[]>([])

// 변경 후
const performanceHistory = shallowRef<number[]>([])

// 업데이트 로직 수정 필요 (라인 1609-1612)
// 변경 전
performanceHistory.value.push(currentInterval)
if (performanceHistory.value.length > 20) {
  performanceHistory.value.shift()
}

// 변경 후: 새 배열 생성
const newHistory = [...performanceHistory.value, currentInterval]
if (newHistory.length > 20) {
  newHistory.shift()
}
performanceHistory.value = newHistory
```

#### 예상 효과
- CPU 사용량: **~40% 감소** (깊은 반응성 추적 제거)
- 30ms마다 발생하는 Proxy 객체 생성 오버헤드 제거

---

### 1.2 Dead Code 삭제 (Day 1)

#### 삭제 대상 (총 69줄)

| 파일 | 라인 수 | 삭제 방법 |
|------|:-------:|----------|
| `ExampleComponent.vue` | 37줄 | 파일 전체 삭제 |
| `example-store.ts` | 22줄 | 파일 전체 삭제 |
| `models.ts` | 9줄 | 사용되지 않는 타입 삭제 |

```bash
# 삭제 명령어
rm frontend/src/components/ExampleComponent.vue
rm frontend/src/stores/example-store.ts
# models.ts는 수동 확인 후 미사용 타입만 삭제
```

---

### 1.3 하드코딩 색상 수정 (475건)

#### 수정 패턴

```vue
<!-- 변경 전 -->
<div style="background-color: #1e1e1e;">

<!-- 변경 후 -->
<div :style="{ backgroundColor: 'var(--theme-bg-primary)' }">
```

```css
/* 변경 전 */
.panel {
  background-color: #2d2d2d;
  border: 1px solid #404040;
}

/* 변경 후 */
.panel {
  background-color: var(--theme-bg-secondary);
  border: 1px solid var(--theme-border);
}
```

#### 주요 테마 변수

| 변수명 | 용도 |
|--------|------|
| `--theme-bg-primary` | 주 배경색 |
| `--theme-bg-secondary` | 보조 배경색 |
| `--theme-text-primary` | 주 텍스트 |
| `--theme-border` | 테두리 |
| `--theme-accent` | 강조색 |

---

## 2. Backend 해결 방안

### 2.1 !! 연산자 P0 수정 (3건 - Day 1)

#### (1) EphemerisService.kt:2717

```kotlin
// 변경 전 (라인 2717-2720)
val mstId = (currentTrackingPass!!["MstId"] as? Number)?.toLong()
    ?: (currentTrackingPass!!["No"] as? Number)?.toLong()
    ?: throw IllegalStateException("MstId가 없습니다")

// 변경 후 - 로컬 변수 캡처로 스레드 안전성 확보
val pass = currentTrackingPass
if (pass == null) {
    logger.error("현재 추적 중인 패스가 없습니다.")
    return
}

val mstId = (pass["MstId"] as? Number)?.toLong()
    ?: (pass["No"] as? Number)?.toLong()
    ?: run {
        logger.error("MstId 또는 No 필드가 없습니다: pass=$pass")
        return
    }
```

#### (2) SunTrackService.kt:636

```kotlin
// 변경 전 (라인 636)
String.format("%.3f", targetTrainAngle?.toFloat()!!)

// 변경 후 - let 스코프 함수 사용
targetTrainAngle?.let { trainAngle ->
    logger.info("[CalTime] 3축 변환 후: Az={}deg, El={}deg (Tilt={}deg, Train={}deg)",
        String.format("%.3f", transformedAz),
        String.format("%.3f", transformedEl),
        String.format("%.3f", settingsService.tiltAngle),
        String.format("%.3f", trainAngle))
} ?: logger.warn("Train 각도가 null입니다")
```

#### (3) PassScheduleService.kt:3729

```kotlin
// 변경 전 (라인 3729)
val timeToStart = Duration.between(calTime, currentScheduleContext!!.startTime)

// 변경 후 - firstOrNull() 사용
val firstContext = scheduleContextQueue.firstOrNull()
if (firstContext == null) {
    logger.error("[V2-START] 첫 스케줄을 가져올 수 없습니다")
    return@fromCallable false
}

currentScheduleContext = firstContext
val timeToStart = Duration.between(calTime, firstContext.startTime)
```

---

### 2.2 .subscribe() 에러 핸들러 추가 (2건 - Day 1)

#### (1) UdpFwICDService.kt:933

```kotlin
// 변경 전
Mono.delay(Duration.ofMillis(100))
    .subscribeOn(Schedulers.boundedElastic())
    .subscribe { this.run() }

// 변경 후
Mono.delay(Duration.ofMillis(100))
    .subscribeOn(Schedulers.boundedElastic())
    .subscribe(
        { this.run() },
        { error ->
            logger.error("틸트 안정화 재시도 중 오류: {}", error.message, error)
            sink.error(error)
        }
    )
```

#### (2) UdpFwICDService.kt:195

```kotlin
// 변경 전
Mono.delay(Duration.ofSeconds(5)).subscribeOn(Schedulers.boundedElastic()).subscribe {
    logger.info("UDP 연결 재시도 중...")
    initializeUdpChannel()
}

// 변경 후 - 재시도 로직 포함
private fun scheduleReconnection() {
    Mono.delay(Duration.ofSeconds(5))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            {
                logger.info("UDP 연결 재시도 중...")
                try {
                    initializeUdpChannel()
                } catch (e: Exception) {
                    logger.error("UDP 재연결 실패: {}", e.message, e)
                    scheduleReconnection()  // 재시도 스케줄링
                }
            },
            { error ->
                logger.error("UDP 재연결 스케줄링 중 오류: {}", error.message, error)
                scheduleReconnection()
            }
        )
}
```

---

### 2.3 Thread.sleep → Mono.delay (Day 1)

#### UdpFwICDService.kt:1109 (forceReconnect)

```kotlin
// 변경 전
fun forceReconnect() {
    Mono.fromCallable {
        isUdpRunning.set(false)
        if (::channel.isInitialized && channel.isOpen) {
            channel.close()
        }
        Thread.sleep(1000)  // 블로킹!
        initializeUdpChannel()
    }.subscribeOn(Schedulers.boundedElastic()).subscribe(...)
}

// 변경 후 - 논블로킹 체이닝
fun forceReconnect() {
    logger.warn("강제 재연결 시도...")

    Mono.fromRunnable<Unit> {
        isUdpRunning.set(false)
        if (::channel.isInitialized && channel.isOpen) {
            channel.close()
        }
    }
    .then(Mono.delay(Duration.ofSeconds(1)))  // 논블로킹 대기
    .then(Mono.fromRunnable<Unit> {
        initializeUdpChannel()
        logger.info("강제 재연결 완료")
    })
    .subscribeOn(Schedulers.boundedElastic())
    .subscribe(
        { /* 성공 */ },
        { error -> logger.error("강제 재연결 실패: {}", error.message, error) }
    )
}
```

---

### 2.4 runBlocking 제거 (Day 1)

#### ElevationCalculator.kt:78

```kotlin
// 변경 전
fun getElevationComparisonBlocking(...): ElevationComparison =
    kotlinx.coroutines.runBlocking {
        getElevationComparison(...)
    }

// 변경 후 - Mono 반환 (WebFlux 환경 권장)
import kotlinx.coroutines.reactor.mono

fun getElevationComparisonMono(
    latitude: Double,
    longitude: Double,
    googleApiKey: String? = null
): Mono<ElevationComparison> = mono(Dispatchers.IO) {
    getElevationComparison(latitude, longitude, googleApiKey)
}

// 기존 함수는 @Deprecated 처리
@Deprecated("Use getElevationComparisonMono instead")
fun getElevationComparisonBlocking(...): ElevationComparison = ...
```

---

### 2.5 테스트 코드 이동 (Day 1)

```bash
# OrekitCalculatorTest.kt 이동
mkdir -p backend/src/test/kotlin/com/gtlsystems/acs_api/algorithm/orbit
mv backend/src/main/kotlin/com/gtlsystems/acs_api/algorithm/orbit/OrekitCalculatorTest.kt \
   backend/src/test/kotlin/com/gtlsystems/acs_api/algorithm/orbit/

# 패키지 선언 확인 (변경 필요 없음 - 경로만 변경)
```

---

## 3. 추가 검토 필요 항목

### 3.1 웹 검색 기반 누락 체크리스트

| 항목 | 설명 | 우선순위 | 상태 |
|------|------|:--------:|:----:|
| **v-memo 디렉티브** | 대형 리스트 메모이제이션 | P2 | 미반영 |
| **SAST 도구 도입** | 정적 보안 분석 (SonarQube, Snyk) | P2 | 미반영 |
| **Rate Limiting** | API 요청 제한 | P2 | 미반영 |
| **API 버저닝** | 하위 호환성 관리 | P3 | 미반영 |
| **Circuit Breaker** | 장애 격리 (Resilience4j) | P3 | 미반영 |

### 3.2 추가 FE 최적화 (Phase 2-4 대상)

| 항목 | 대상 | 효과 |
|------|------|------|
| deep watch 제거 | 34개 위치 | 렌더링 최적화 |
| computed 분리 | 대형 computed | 캐싱 효율 |
| 대형 컴포넌트 분리 | 3개 페이지 | 유지보수성 |

### 3.3 추가 BE 최적화

| 항목 | 대상 | 효과 |
|------|------|------|
| @Valid 입력 검증 | Controllers | 보안 강화 |
| 광범위 catch 정리 | 180+건 | 에러 가시성 |
| !! 연산자 나머지 | 43개 | Null 안전성 |

---

## 검증 방법

### Frontend 빌드 확인
```bash
cd frontend && npm run build
```

### Backend 빌드 확인
```bash
cd backend && ./gradlew clean build -x test
```

### 기능 테스트 체크리스트
- [ ] ICD 연결 후 30ms 실시간 업데이트 확인
- [ ] 위성 추적 시작/중지 정상 동작
- [ ] 태양 추적 시작/중지 정상 동작
- [ ] 패스 스케줄 선택/실행 정상 동작
- [ ] UDP 강제 재연결 테스트

---

**작성자**: Claude (전문가 에이전트 분석 결과 종합)
**최종 업데이트**: 2026-01-14
