# ACS 데이터 흐름

> Frontend ↔ Backend ↔ 안테나 간 데이터가 어떻게 흐르는지

## 전체 데이터 흐름

```
┌──────────────────────────────────────────────────────────────────┐
│                         Frontend (Vue)                            │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│  │ 컴포넌트     │ ←→ │ Pinia Store │ ←→ │ WebSocket   │           │
│  │ (화면)      │    │ (상태)      │    │ (실시간)    │           │
│  └─────────────┘    └─────────────┘    └─────────────┘           │
└──────────────────────────────────────────────────────────────────┘
         │ 사용자 액션              │ 30ms마다 데이터
         ▼                         ▼
┌──────────────────────────────────────────────────────────────────┐
│                    REST API / WebSocket                           │
└──────────────────────────────────────────────────────────────────┘
         │                         │
         ▼                         ▼
┌──────────────────────────────────────────────────────────────────┐
│                        Backend (Spring)                           │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│  │ Controller  │ →  │ Service     │ →  │ Algorithm   │           │
│  │ (요청처리)   │    │ (로직)      │    │ (계산)      │           │
│  └─────────────┘    └─────────────┘    └─────────────┘           │
│                            │                                      │
│                     ┌──────┴──────┐                               │
│                     ▼             ▼                               │
│              ┌───────────┐  ┌───────────┐                        │
│              │ ICD통신    │  │ WebSocket │                        │
│              │ (UDP)     │  │ Push      │                        │
│              └───────────┘  └───────────┘                        │
└──────────────────────────────────────────────────────────────────┘
                     │
                    UDP
                     │
                     ▼
┌──────────────────────────────────────────────────────────────────┐
│                    안테나 제어장비 (ACU)                           │
└──────────────────────────────────────────────────────────────────┘
```

---

## 주요 데이터 흐름 시나리오

### 1. 실시간 안테나 상태 표시

```
[30ms마다 반복]

안테나(ACU) → UDP 패킷 전송
    │
    ▼
Backend: UdpFwICDService
    │ 바이너리 파싱
    ▼
Backend: GlobalData 업데이트
    │
    ▼
Backend: PushDataController
    │ WebSocket으로 브로드캐스트
    ▼
Frontend: icdStore (WebSocket 수신)
    │ shallowRef 업데이트
    ▼
Frontend: 컴포넌트 (화면 갱신)
    │ Vue 반응형
    ▼
사용자: 화면에서 Az/El/속도 확인
```

**관련 코드**:
- `UdpFwICDService.kt` - UDP 수신 및 파싱
- `PushDataController.kt` - WebSocket 브로드캐스트
- `icdStore.ts` - 상태 저장 및 반응형

---

### 2. 위성 추적 시작 명령

```
사용자: "추적 시작" 버튼 클릭
    │
    ▼
Frontend: 컴포넌트
    │ @click 이벤트
    ▼
Frontend: API 호출
    │ axios.post('/api/ephemeris/track/start')
    ▼
Backend: EphemerisController
    │ @PostMapping
    ▼
Backend: EphemerisService
    │ 추적 상태 변경
    │ 궤도 계산 시작
    ▼
Backend: Algorithm (Orekit)
    │ TLE → 위치 계산
    ▼
Backend: UdpFwICDService
    │ 안테나 명령 생성
    ▼
안테나(ACU): UDP 명령 수신
    │ 안테나 이동 시작
    ▼
안테나 → Backend → Frontend
    │ 실시간 위치 피드백
    ▼
사용자: 화면에서 추적 상태 확인
```

**관련 코드**:
- `EphemerisDesignationPage.vue` - UI
- `EphemerisController.kt` - API 엔드포인트
- `EphemerisService.kt` - 추적 로직
- `OrbitCalculator.kt` - Orekit 계산

---

### 3. 설정 저장

```
사용자: 설정 값 변경 → "저장" 클릭
    │
    ▼
Frontend: SettingsModal.vue
    │ v-model로 바인딩된 값
    ▼
Frontend: settingsStore
    │ saveSettings() 액션
    ▼
REST API: POST /api/settings
    │ JSON body
    ▼
Backend: SettingsController
    │ @RequestBody @Valid
    ▼
Backend: SettingsService
    │ 검증 + 저장
    ▼
Backend: 파일 시스템 (settings.json)
    │
    ▼
Frontend: 성공 응답 수신
    │
    ▼
사용자: "저장 완료" 알림
```

---

## 통신 방식별 용도

### REST API (HTTP)
```
용도: 일회성 요청/응답
├── 설정 조회/저장
├── TLE 업로드
├── 패스 스케줄 CRUD
└── 시스템 정보 조회

특징:
├── 요청 → 응답 → 연결 종료
├── 상태 없음 (Stateless)
└── 캐싱 가능
```

### WebSocket
```
용도: 실시간 양방향 통신
├── 안테나 상태 (30ms 주기)
├── 추적 진행 상태
├── 알람/이벤트 푸시
└── 로그 스트리밍

특징:
├── 연결 유지 (Persistent)
├── 서버 → 클라이언트 푸시 가능
└── 낮은 오버헤드
```

### UDP (ICD)
```
용도: 안테나 장비 통신
├── 위치 명령 전송
├── 상태 데이터 수신
├── 제어 명령 (MC On/Off 등)
└── 실시간 피드백

특징:
├── 빠름 (TCP보다)
├── 연결 없음 (Connectionless)
├── 패킷 손실 가능
└── 바이너리 프로토콜 (ICD 규격)
```

---

## 상태 동기화 흐름

### Frontend 상태 관리

```
┌─────────────────────────────────────────────────────┐
│                    Pinia Stores                      │
├─────────────────────────────────────────────────────┤
│  icdStore          │ 실시간 안테나 데이터 (WebSocket) │
│  passScheduleStore │ 패스 스케줄 목록                │
│  settingsStore     │ 시스템 설정                     │
│  modeStore         │ 현재 모드 상태                  │
└─────────────────────────────────────────────────────┘
          │
          │ storeToRefs()로 반응형 연결
          ▼
┌─────────────────────────────────────────────────────┐
│               Vue 컴포넌트                           │
│  computed(), watch()로 파생/감시                     │
└─────────────────────────────────────────────────────┘
```

### Backend 상태 관리

```
┌─────────────────────────────────────────────────────┐
│                   GlobalData (object)                │
├─────────────────────────────────────────────────────┤
│  Offset       │ 오프셋 값들                          │
│  Time         │ 시간 정보                            │
│  TrackingData │ 추적 각도                            │
└─────────────────────────────────────────────────────┘
          │
          │ 서비스에서 읽기/쓰기
          ▼
┌─────────────────────────────────────────────────────┐
│               Services                               │
│  EphemerisService, PassScheduleService, ...         │
└─────────────────────────────────────────────────────┘
```

---

## 에러 처리 흐름

### Frontend 에러 처리

```
API 호출 실패
    │
    ▼
catch 블록 또는 onError
    │
    ▼
useErrorHandler composable
    │
    ├── 사용자 알림 (q-notify)
    ├── 콘솔 로깅
    └── (필요시) 에러 리포팅
```

### Backend 에러 처리

```
예외 발생
    │
    ▼
GlobalExceptionHandler
    │
    ├── 예외 타입별 HTTP 상태 코드
    ├── 에러 응답 포맷팅
    └── 로깅
    │
    ▼
클라이언트에 에러 응답
```

---

## 실시간 데이터 갱신 주기

| 데이터 | 주기 | 소스 |
|--------|------|------|
| 안테나 위치 (Az/El) | 30ms | ACU → UDP |
| 추적 상태 | 100ms | Service 계산 |
| 패스 예측 | 1초 | Orekit 계산 |
| 시스템 상태 | 1초 | 내부 모니터링 |

---

## 에러 처리 흐름

### Frontend 에러 처리 상세

```
┌──────────────────────────────────────────────────────────────────┐
│                    Frontend 에러 처리 아키텍처                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐          │
│  │ API 호출     │ →  │ try/catch   │ →  │ useError    │          │
│  │ axios       │    │ 블록        │    │ Handler     │          │
│  └─────────────┘    └─────────────┘    └─────────────┘          │
│         │                                     │                  │
│         ▼                                     ▼                  │
│  ┌─────────────┐                      ┌─────────────┐           │
│  │ Interceptor │                      │ q-notify    │           │
│  │ (전역 처리)  │                      │ (사용자 알림)│           │
│  └─────────────┘                      └─────────────┘           │
│         │                                     │                  │
│         ▼                                     ▼                  │
│  ┌─────────────────────────────────────────────────┐            │
│  │              console.error (개발용 로깅)          │            │
│  └─────────────────────────────────────────────────┘            │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**에러 유형별 처리**:
```typescript
// services/api.ts - Axios Interceptor
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      // 서버 응답 에러 (4xx, 5xx)
      switch (error.response.status) {
        case 400: // Bad Request
          console.error('잘못된 요청:', error.response.data)
          break
        case 404: // Not Found
          console.error('리소스 없음')
          break
        case 500: // Server Error
          console.error('서버 에러')
          break
      }
    } else if (error.request) {
      // 네트워크 에러 (응답 없음)
      console.error('네트워크 연결 실패')
    }
    return Promise.reject(error)
  }
)
```

**컴포넌트에서 에러 처리**:
```typescript
// composables/useErrorHandler.ts 사용
const { handleError, showError, showSuccess } = useErrorHandler()

async function saveSettings() {
  try {
    await api.saveSettings(data)
    showSuccess('설정 저장 완료')
  } catch (error) {
    handleError(error, '설정 저장 실패')
    // handleError 내부:
    // 1. 사용자에게 q-notify로 알림
    // 2. console.error로 상세 로깅
  }
}
```

---

### Backend 에러 처리 상세

```
┌──────────────────────────────────────────────────────────────────┐
│                    Backend 에러 처리 아키텍처                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐          │
│  │ Controller  │ →  │ Service     │ →  │ Repository  │          │
│  │ 예외 발생   │    │ 예외 발생   │    │ 예외 발생   │          │
│  └─────────────┘    └─────────────┘    └─────────────┘          │
│         │                 │                   │                  │
│         └────────────────┼───────────────────┘                  │
│                          ▼                                       │
│              ┌─────────────────────┐                            │
│              │ GlobalException     │                            │
│              │ Handler             │                            │
│              │ (@ControllerAdvice) │                            │
│              └─────────────────────┘                            │
│                          │                                       │
│          ┌───────────────┼───────────────┐                      │
│          ▼               ▼               ▼                      │
│   ┌───────────┐   ┌───────────┐   ┌───────────┐                │
│   │ 400       │   │ 404       │   │ 500       │                │
│   │ BadRequest│   │ NotFound  │   │ Internal  │                │
│   └───────────┘   └───────────┘   └───────────┘                │
│                          │                                       │
│                          ▼                                       │
│              ┌─────────────────────┐                            │
│              │ 표준 에러 응답 반환   │                            │
│              │ { error, message }  │                            │
│              └─────────────────────┘                            │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**GlobalExceptionHandler 예시**:
```kotlin
@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Bad Request: ${e.message}")
        return ResponseEntity.badRequest()
            .body(ErrorResponse("BAD_REQUEST", e.message ?: "잘못된 요청"))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ErrorResponse> {
        logger.warn("Not Found: ${e.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("NOT_FOUND", e.message ?: "리소스를 찾을 수 없음"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Internal Error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "서버 내부 오류"))
    }
}

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now()
)
```

---

## 로깅 흐름

### Frontend 로깅

```
┌──────────────────────────────────────────────────────────────────┐
│                    Frontend 로깅 전략                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  로그 레벨:                                                        │
│  ├── console.error()  → 에러, 예외                                │
│  ├── console.warn()   → 경고, 비권장 사용                          │
│  ├── console.info()   → 중요 정보                                  │
│  └── console.log()    → 디버그 (production에서 제거)              │
│                                                                   │
│  로깅 위치:                                                        │
│  ├── API 호출 실패 → useErrorHandler                              │
│  ├── WebSocket 연결/끊김 → icdStore                               │
│  ├── 상태 변경 → 개발 모드에서만                                   │
│  └── 사용자 액션 → 중요 액션만                                     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**로깅 예시**:
```typescript
// WebSocket 연결 상태 로깅
function connectWebSocket() {
  socket.onopen = () => {
    console.info('[WebSocket] 연결됨')
  }

  socket.onclose = (event) => {
    console.warn('[WebSocket] 연결 끊김:', event.code, event.reason)
  }

  socket.onerror = (error) => {
    console.error('[WebSocket] 에러:', error)
  }
}

// 개발 모드 전용 로깅
if (import.meta.env.DEV) {
  console.log('[Debug] 상태 변경:', newState)
}
```

---

### Backend 로깅

```
┌──────────────────────────────────────────────────────────────────┐
│                    Backend 로깅 아키텍처 (SLF4J + Logback)         │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  로그 레벨:                                                        │
│  ├── ERROR  → 심각한 에러, 즉시 대응 필요                          │
│  ├── WARN   → 경고, 잠재적 문제                                   │
│  ├── INFO   → 중요 비즈니스 이벤트                                 │
│  ├── DEBUG  → 상세 디버깅 정보                                    │
│  └── TRACE  → 매우 상세한 정보 (거의 사용 안 함)                   │
│                                                                   │
│  출력 대상:                                                        │
│  ├── Console (개발)                                               │
│  ├── File (운영) → logs/acs.log                                   │
│  └── Rolling → 일별/크기별 로테이션                                │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**로깅 예시**:
```kotlin
@Service
class EphemerisService {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun startTracking(satelliteId: String) {
        logger.info("위성 추적 시작: satelliteId={}", satelliteId)

        try {
            val satellite = findSatellite(satelliteId)
            logger.debug("위성 정보: {}", satellite)

            // 추적 로직...

            logger.info("추적 시작 완료: {}", satelliteId)
        } catch (e: Exception) {
            logger.error("추적 시작 실패: satelliteId={}", satelliteId, e)
            throw e
        }
    }
}
```

**logback-spring.xml 설정**:
```xml
<configuration>
    <!-- 콘솔 출력 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 파일 출력 (Rolling) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/acs.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/acs.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 로그 레벨 설정 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>

    <!-- 패키지별 레벨 -->
    <logger name="com.acs" level="DEBUG" />
    <logger name="org.springframework" level="WARN" />
</configuration>
```

---

## WebSocket 연결 관리

```
┌──────────────────────────────────────────────────────────────────┐
│                    WebSocket 생명주기                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. 연결 시도                                                      │
│     Frontend → ws://backend:8080/ws/icd                          │
│                                                                   │
│  2. 연결 성공                                                      │
│     ├── icdStore: isConnected = true                             │
│     └── Backend: 클라이언트 세션 등록                              │
│                                                                   │
│  3. 데이터 수신 (30ms 주기)                                        │
│     ├── Backend → JSON 메시지 전송                                │
│     └── Frontend → shallowRef 업데이트                            │
│                                                                   │
│  4. 연결 끊김                                                      │
│     ├── 네트워크 문제 / 서버 재시작                                 │
│     ├── icdStore: isConnected = false                            │
│     └── 자동 재연결 시도 (3초 후)                                  │
│                                                                   │
│  5. 재연결                                                         │
│     └── 성공 시 1번부터 반복                                       │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**재연결 로직**:
```typescript
// stores/icdStore.ts
let reconnectTimeout: ReturnType<typeof setTimeout> | null = null
const RECONNECT_DELAY = 3000

function connect() {
  const socket = new WebSocket('ws://localhost:8080/ws/icd')

  socket.onopen = () => {
    isConnected.value = true
    console.info('[WS] 연결됨')
  }

  socket.onclose = () => {
    isConnected.value = false
    console.warn('[WS] 연결 끊김, 재연결 시도...')

    // 자동 재연결
    reconnectTimeout = setTimeout(() => {
      connect()
    }, RECONNECT_DELAY)
  }

  socket.onmessage = (event) => {
    icdData.value = JSON.parse(event.data)
  }
}

// 컴포넌트 언마운트 시 정리
onUnmounted(() => {
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout)
  }
})
```

---

**이전**: [tech-stack.md](./tech-stack.md) - 기술 스택
**다음**: [glossary.md](./glossary.md) - 용어 사전
