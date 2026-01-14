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

## 폴더 구조

```
backend/src/main/kotlin/.../
├── controller/           # REST API 엔드포인트
│   ├── EphemerisController.kt
│   ├── PassScheduleController.kt
│   ├── StepController.kt
│   ├── SlewController.kt
│   └── SunTrackController.kt
├── service/              # 비즈니스 로직
│   ├── EphemerisService.kt    # 궤도 계산
│   ├── PassScheduleService.kt # 패스 스케줄
│   ├── ICDService.kt          # ICD 통신
│   └── TrackingService.kt     # 추적 제어
├── algorithm/            # 순수 계산 로직
│   ├── ephemeris/        # 궤도 알고리즘
│   ├── icd/              # ICD 파싱/생성
│   └── coordinate/       # 좌표 변환
├── dto/                  # 데이터 전송 객체
│   ├── request/          # 요청 DTO
│   └── response/         # 응답 DTO
├── model/                # 도메인 모델
│   ├── TLE.kt
│   ├── Pass.kt
│   └── AntennaState.kt
├── config/               # 설정
│   ├── WebSocketConfig.kt
│   ├── OrekitConfig.kt
│   └── CorsConfig.kt
└── exception/            # 예외 처리
    └── GlobalExceptionHandler.kt
```

## 계층 구조

```
┌─────────────────────────────────────┐
│           Controller                │  ← REST API
├─────────────────────────────────────┤
│            Service                  │  ← 비즈니스 로직
├─────────────────────────────────────┤
│           Algorithm                 │  ← 순수 계산
├─────────────────────────────────────┤
│         Repository/External         │  ← 데이터/외부
└─────────────────────────────────────┘
```

### 계층별 책임

| 계층 | 책임 | 의존성 |
|-----|------|--------|
| Controller | HTTP 처리, 검증 | Service |
| Service | 비즈니스 로직, 트랜잭션 | Algorithm, Repository |
| Algorithm | 순수 계산 | 없음 (순수 함수) |
| Repository | 데이터 접근 | DB/외부 |

## 코딩 규칙

### Controller
```kotlin
@RestController
@RequestMapping("/api/ephemeris")
class EphemerisController(
    private val ephemerisService: EphemerisService
) {
    /**
     * TLE 데이터 조회
     * @param noradId NORAD 카탈로그 ID
     * @return TLE 데이터
     */
    @GetMapping("/tle/{noradId}")
    suspend fun getTle(@PathVariable noradId: String): TleResponse {
        return ephemerisService.getTle(noradId)
    }

    /**
     * 패스 예측
     */
    @PostMapping("/predict")
    suspend fun predictPass(@Valid @RequestBody request: PredictRequest): Flux<PassResponse> {
        return ephemerisService.predictPass(request)
    }
}
```

### Service
```kotlin
@Service
class EphemerisService(
    private val tleRepository: TleRepository,
    private val orekitCalculator: OrekitCalculator
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 패스 예측 수행
     *
     * @param request 예측 파라미터
     * @return 예측된 패스 목록
     */
    suspend fun predictPass(request: PredictRequest): Flow<Pass> {
        logger.info("패스 예측 시작: {}", request.noradId)

        val tle = tleRepository.findByNoradId(request.noradId)
            ?: throw NotFoundException("TLE not found: ${request.noradId}")

        return orekitCalculator.predictPasses(tle, request.startTime, request.endTime)
    }
}
```

### Algorithm (순수 함수)
```kotlin
/**
 * Orekit 기반 궤도 계산기
 *
 * 순수 함수로 구현 - 외부 의존성 없음
 */
object OrekitCalculator {
    /**
     * 특정 시각의 위성 위치 계산
     *
     * @param tle TLE 데이터
     * @param time 계산 시각 (UTC)
     * @return 위성 위치 (ECI 좌표)
     */
    fun calculatePosition(tle: TLE, time: AbsoluteDate): Vector3D {
        val propagator = TLEPropagator.selectExtrapolator(tle)
        val state = propagator.propagate(time)
        return state.position
    }

    /**
     * 안테나 지향각 계산
     *
     * @param satellitePosition 위성 위치 (ECI)
     * @param groundStation 지상국 위치
     * @return 안테나 지향각 (Az, El)
     */
    fun calculatePointingAngle(
        satellitePosition: Vector3D,
        groundStation: GeodeticPoint
    ): PointingAngle {
        // 좌표 변환 및 계산
        // ...
        return PointingAngle(azimuth, elevation)
    }
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
class ICDService(
    private val udpClient: UdpClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 안테나 명령 전송
     */
    suspend fun sendCommand(command: AntennaCommand) {
        val packet = ICDBuilder.buildCommand(command)
        udpClient.send(packet)
        logger.debug("명령 전송: {}", command)
    }

    /**
     * 상태 수신 (30ms 주기)
     */
    fun receiveStatus(): Flow<AntennaStatus> = flow {
        while (true) {
            val packet = udpClient.receive()
            val status = ICDParser.parseStatus(packet)
            emit(status)
        }
    }
}
```

## WebSocket 설정

```kotlin
@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(icdWebSocketHandler(), "/ws/icd")
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

## 주요 서비스

| 서비스 | 역할 | 크기 |
|-------|------|------|
| EphemerisService | 궤도 계산, TLE 관리 | 4,986줄 |
| ICDService | 안테나 통신 | 2,788줄 |
| PassScheduleService | 스케줄 관리 | 1,500줄 |
| TrackingService | 실시간 추적 | 1,200줄 |

## 주의사항

- **KDoc 필수**: 모든 공개 함수에 문서화
- **Logger 사용**: println 금지, SLF4J 사용
- **예외 세분화**: catch(Exception) 지양
- **null 안전**: !! 연산자 최소화
- **Orekit 초기화**: DataContext 설정 필수

## 참조

- [위성 추적](../domain/satellite-tracking.md)
- [ICD 프로토콜](../domain/icd-protocol.md)
- [Frontend 아키텍처](frontend.md)

---

**최종 수정:** 2026-01-14
