# Backend 아키텍처

> Kotlin + Spring Boot 3 + WebFlux 기반 리액티브 서버

## 기술 스택

| 기술 | 버전 | 용도 |
|-----|------|------|
| Kotlin | 1.9.x | 언어 |
| Spring Boot | 3.x | 프레임워크 |
| WebFlux | - | 리액티브 웹 |
| Orekit | 13.0 | 궤도 역학 |
| Netty | - | UDP 통신 |

## 코드 현황

| 영역 | 파일 수 | 코드 줄 |
|-----|--------|---------|
| 전체 | 67개 (.kt) | 33,284줄 |
| 대형 파일 (500줄+) | 8개 | 17,137줄 |

## 폴더 구조

```
backend/src/main/kotlin/.../
├── controller/               # REST API 엔드포인트
│   ├── icd/                  # ICDController.kt
│   ├── mode/                 # EphemerisController, PassScheduleController, SunTrackController
│   ├── system/               # SettingsController, LoggingController, PerformanceController
│   └── websocket/            # PushDataController.kt
├── service/                  # 비즈니스 로직
│   ├── datastore/            # DataStoreService.kt (H2 데이터 저장)
│   ├── hardware/             # HardwareErrorLogService.kt
│   ├── icd/                  # ICDService.kt (2,788줄)
│   ├── mode/                 # EphemerisService (5,057줄), PassScheduleService (3,846줄), SunTrackService
│   ├── system/               # SettingsService (1,183줄), BatchStorageManager, LoggingService
│   ├── udp/                  # UdpFwICDService.kt (1,228줄) - UDP 포워딩
│   └── websocket/            # PushDataService.kt
├── algorithm/                # 순수 계산 로직
│   ├── axislimitangle/       # LimitAngleCalculator.kt (±270° 변환)
│   ├── axistransformation/   # CoordinateTransformer.kt (2축→3축 변환)
│   ├── elevation/            # ElevationCalculator.kt
│   ├── satellitetracker/     # OrekitCalculator (627줄), SatelliteTrackingProcessor (1,387줄)
│   └── suntrack/             # SPA, Grena3, SolarOrekit 알고리즘
├── dto/                      # 데이터 전송 객체
├── model/                    # 도메인 모델
├── config/                   # 설정
├── event/                    # ACSEventBus (이벤트 버스 시스템)
├── openapi/                  # OpenAPI/Swagger 문서
├── repository/               # JPA Repository
└── settings/entity/          # JPA Entity
```

## 계층 구조

```
┌─────────────────────────────────────┐
│           Controller                │  ← REST API
├─────────────────────────────────────┤
│            Service                  │  ← 비즈니스 로직 + 상태머신
├─────────────────────────────────────┤
│           Algorithm                 │  ← 순수 계산
├─────────────────────────────────────┤
│       Repository / External         │  ← 데이터/UDP
└─────────────────────────────────────┘
```

### 계층별 책임

| 계층 | 책임 | 주요 파일 |
|-----|------|----------|
| Controller | HTTP 처리, 검증, 라우팅 | EphemerisController (1,091줄), PassScheduleController (1,557줄) |
| Service | 비즈니스 로직, 상태머신, 배치 처리 | EphemerisService (5,057줄), ICDService (2,788줄) |
| Algorithm | 순수 계산 (좌표 변환, 궤도 계산) | OrekitCalculator (627줄), SatelliteTrackingProcessor (1,387줄) |
| Repository | 데이터 접근 | DataStoreService, JPA Repositories |

## 핵심 서비스 상세

### EphemerisService.kt (5,057줄)

**역할:** 위성 궤도 지정(Ephemeris Designation) 모드 관리

```kotlin
// 상태머신 정의
enum class TrackingState {
    IDLE, PREPARING, WAITING, TRACKING, COMPLETED, ERROR
}

enum class PreparingPhase {
    TRAIN_MOVING,       // Train 각도 이동
    TRAIN_STABILIZING,  // Train 안정화 대기
    MOVING_TO_TARGET    // 목표 위치로 이동
}

// 핵심 기능
- trackStart(): 추적 시작 → 상태머신 시작
- handleTimerTick(): 30ms 타이머로 상태 전이
- calculateAndSendPosition(): 궤도 계산 + UDP 전송
- batchSave(): 배치 저장 (100개 버퍼)
```

**TLE 캐시:**
```kotlin
private val satelliteTleCache = ConcurrentHashMap<String, TLECacheData>()
```

### PassScheduleService.kt (3,846줄)

**역할:** 패스 스케줄 모드 관리

```kotlin
// 상태머신 정의 (v2.0)
enum class ScheduleTrackingState {
    IDLE, MONITORING, PRE_TRACK, TRACKING, POST_TRACK
}

enum class PreTrackPhase {
    WAITING_START,
    MOVING_TO_START,
    STABILIZING
}

// 핵심 기능
- startMonitoring(): 스케줄 모니터링 시작
- handleMonitoringTick(): 100ms 타이머로 스케줄 감시
- executeTracking(): 실제 추적 실행
- ScheduleTrackingContext: 추적 컨텍스트 관리
```

### ICDService.kt (2,788줄)

**역할:** 하드웨어 ICD 프로토콜 처리

```kotlin
// 내부 중첩 클래스 구조
ICDService
├── class Classify           // 수신 패킷 분류
├── class ReadStatus         // 상태 읽기
├── class SatelliteTrackOne  // 추적 명령 #1
├── class SatelliteTrackTwo  // 추적 명령 #2
├── class SatelliteTrackThree // 추적 명령 #3
├── class Standby            // 대기 명령
├── class Stop               // 정지 명령
├── class Emergency          // 비상 정지
├── class MultiManualControl // 멀티 매뉴얼
├── class SingleManualControl // 싱글 매뉴얼
├── class FeedCmd            // 피드 제어
└── ... (19개 내부 클래스)
```

## Algorithm 계층

### OrekitCalculator.kt (627줄)

```kotlin
/**
 * Orekit 기반 궤도 계산
 */
class OrekitCalculator {
    fun calculateSatellitePositionAndVelocity(tle: TLE, time: AbsoluteDate): PVCoordinates
    fun detectVisibilityPeriods(tle: TLE, start: AbsoluteDate, end: AbsoluteDate): List<VisibilityPeriod>
    fun generateSatelliteTrackingSchedule(...): List<TrackingPoint>
    fun parseUTCString(utcString: String): AbsoluteDate
}
```

### SatelliteTrackingProcessor.kt (1,387줄)

```kotlin
/**
 * 위성 추적 데이터 처리 파이프라인
 */
class SatelliteTrackingProcessor {
    fun processSatelliteTracking(rawPosition: RawPosition): ProcessedPosition
    fun applyAxisTransformation(azEl: AzEl, trainAngle: Double): AxisAngles
    fun convertToLimitAngle(angle: Double): Double  // ±270° 변환
    fun calculateMetrics(path: List<TrackingPoint>): TrackingMetrics
    fun detectKeyhole(elevation: Double): Boolean
}
```

### CoordinateTransformer.kt

```kotlin
/**
 * 좌표 변환기 (2축 → 3축 변환)
 */
object CoordinateTransformer {
    fun transformToThreeAxis(azimuth: Double, elevation: Double, trainAngle: Double): ThreeAxisAngles
    fun applyOffsets(angles: ThreeAxisAngles, offsets: Offsets): ThreeAxisAngles
}
```

### LimitAngleCalculator.kt

```kotlin
/**
 * 각도 제한 계산기 (±270° 범위)
 */
object LimitAngleCalculator {
    fun convertToLimitRange(angle: Double): Double  // -270° ~ +270°
    fun detectLimitCrossing(current: Double, target: Double): CrossingType
    fun calculateOptimalPath(current: Double, target: Double): OptimalPath
}
```

## 리액티브 패턴

### Mono/Flux 사용
```kotlin
// 단일 값
fun getTle(noradId: String): Mono<TLE>

// 다중 값 (스트림)
fun predictPasses(params: PredictParams): Flux<Pass>

// suspend 함수 (코루틴)
suspend fun getTle(noradId: String): TLE
```

### Flow 사용
```kotlin
suspend fun streamStatus(): Flow<AntennaStatus> = flow {
    while (true) {
        emit(getStatus())
        delay(30)
    }
}
```

## UDP 통신 (ICD)

```kotlin
@Service
class UdpFwICDService(
    private val nettyConfig: NettyConfig
) {
    // STX/ETX 프레임 기반 프로토콜
    companion object {
        const val ICD_STX: Byte = 0x02  // 시작 바이트
        const val ICD_ETX: Byte = 0x03  // 종료 바이트
    }

    /**
     * 명령 전송 (30ms 주기)
     */
    suspend fun sendCommand(frame: SetDataFrame) {
        val packet = buildPacket(frame)  // STX + 명령 + CRC16 + ETX
        udpChannel.send(packet)
    }

    /**
     * 상태 수신 (30ms 주기)
     */
    fun receiveStatus(): Flow<GetDataFrame> = flow {
        while (true) {
            val packet = udpChannel.receive()
            val frame = parsePacket(packet)
            emit(frame)
        }
    }
}
```

## 이벤트 버스

```kotlin
// event/ACSEventBus.kt
object ACSEventBus {
    private val listeners = ConcurrentHashMap<String, MutableList<EventListener>>()

    fun publish(event: ACSEvent)
    fun subscribe(eventType: String, listener: EventListener)
    fun unsubscribe(eventType: String, listener: EventListener)
}
```

## WebSocket 설정

```kotlin
@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(pushDataHandler(), "/ws/icd")
            .setAllowedOrigins("*")
    }
}
```

## 예외 처리

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(e: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(e.message))
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(e: ValidationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.message))
    }
}
```

## 대형 파일 현황 (500줄+)

| 파일 | 줄 수 | 분류 | 리팩토링 우선순위 |
|-----|------|------|-----------------|
| EphemerisService.kt | 5,057 | service/mode | P1 (상태머신 분리) |
| PassScheduleService.kt | 3,846 | service/mode | P1 (v1.0/v2.0 분리) |
| ICDService.kt | 2,788 | service/icd | P2 (명령별 분리) |
| PassScheduleController.kt | 1,557 | controller/mode | - |
| SatelliteTrackingProcessor.kt | 1,387 | algorithm | - |
| UdpFwICDService.kt | 1,228 | service/udp | - |
| SettingsService.kt | 1,183 | service/system | - |
| EphemerisController.kt | 1,091 | controller/mode | - |

## 주의사항

- **KDoc 필수**: 모든 공개 함수에 문서화
- **Logger 사용**: println 금지, SLF4J 사용
- **예외 세분화**: catch(Exception) 지양 → 구체적 예외 타입
- **null 안전**: !! 연산자 최소화
- **Orekit 초기화**: DataContext 설정 필수
- **상태머신**: EphemerisService, PassScheduleService의 상태 전이 로직 주의

## 참조

- [위성 추적](../domain/satellite-tracking.md)
- [ICD 프로토콜](../domain/icd-protocol.md)
- [Frontend 아키텍처](frontend.md)
- [심층 분석: BE Services](../analysis/backend/services.md)

---

**최종 수정:** 2026-01-15
**분석 기반:** Phase 1-4 코드베이스 심층 분석
