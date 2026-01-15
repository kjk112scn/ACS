# Backend Service 계층 심층 분석

> 분석 일시: 2026-01-15
> 분석 대상: backend/src/main/kotlin/.../service/
> 총 파일: 14개
> 총 코드: 11,633줄

## 1. EphemerisService (5,057줄)

### 1.1 클래스 구조

```kotlin
@Service
class EphemerisService(
  - orekitCalculator: OrekitCalculator
  - satelliteTrackingProcessor: SatelliteTrackingProcessor
  - acsEventBus: ACSEventBus
  - udpFwICDService: UdpFwICDService
  - dataStoreService: DataStoreService
  - threadManager: ThreadManager
  - batchStorageManager: BatchStorageManager
  - settingsService: SettingsService
)
```

### 1.2 상태 관리

**TrackingState (enum)**
```
IDLE → PREPARING → WAITING → TRACKING → COMPLETED / ERROR
```

**PreparingPhase (enum)**
```
TRAIN_MOVING → TRAIN_STABILIZING → MOVING_TO_TARGET
```

### 1.3 주요 내부 변수

```kotlin
private val satelliteTleCache = ConcurrentHashMap<String, Pair<String, String>>()
private val ephemerisTrackMstStorage = mutableListOf<Map<String, Any?>>()
private val ephemerisTrackDtlStorage = mutableListOf<Map<String, Any?>>()
private var currentTrackingPass: Map<String, Any?>? = null
private var currentTrackingPassId: Long? = null
private var trackingExecutor: ScheduledExecutorService? = null
private var modeTask: ScheduledFuture<*>? = null
```

### 1.4 핵심 메서드

| 메서드 | 역할 |
|------|------|
| `startGeostationaryTracking(tleLine1, tleLine2)` | 정지궤도 위성 3축 변환 추적 시작 |
| `startEphemerisTracking(mstId, detailId)` | 위성 추적 시작 (상태머신 제어) |
| `stopEphemerisTracking()` | 위성 추적 중지 + 배치 저장 완료 |
| `sendHeaderTrackingData(mstId, detailId)` | 2.12.1 헤더 프로토콜 전송 |
| `sendInitialTrackingData(mstId, detailId)` | 2.12.2 초기 제어 명령 전송 |
| `generateEphemerisDesignationTrackAsync(...)` | 위성 추적 데이터 비동기 생성 |
| `handleEphemerisTrackingDataRequest(...)` | 2.12.3 추가 데이터 요청 처리 |
| `sendAdditionalTrackingData(...)` | 추적 데이터 분할 전송 |
| `getCurrentSatellitePosition(tleLine1, tleLine2)` | 현재 위성 위치 계산 |
| `getCurrentGeostationaryPositionWith3AxisTransform(...)` | 3축 변환 적용 좌표 계산 |

### 1.5 데이터 흐름

1. **준비 단계**: 시작 위치 계산 → Train 이동 → Az/El 이동
2. **추적 단계**: 100ms 주기로 상태 확인 → 위치 계산 → 명령 전송
3. **종료**: 배치 저장 + 리소스 정리

### 1.6 특이사항

- Keyhole 위성 판단 (Azimuth 각속도 임계값)
- Angle Limit 적용하여 필터링된 데이터만 저장
- Train 안정화 3초 대기
- 실시간 데이터와 배치 데이터 이중 관리
- Time Offset 명령으로 추적 시간 조정

---

## 2. PassScheduleService (3,846줄)

### 2.1 클래스 구조

```kotlin
@Service
class PassScheduleService(
  - orekitCalculator: OrekitCalculator
  - satelliteTrackingProcessor: SatelliteTrackingProcessor
  - acsEventBus: ACSEventBus
  - udpFwICDService: UdpFwICDService
  - dataStoreService: DataStoreService
  - settingsService: SettingsService
  - threadManager: ThreadManager
)
```

### 2.2 상태머신 (v2.0 - 통합)

```
PassScheduleState: IDLE → STOWING → STOWED → MOVING_TRAIN →
  TRAIN_STABILIZING → MOVING_TO_START → READY → TRACKING →
  POST_TRACKING → COMPLETED / ERROR
```

### 2.3 핵심 컨텍스트 클래스

```kotlin
data class ScheduleTrackingContext(
  val mstId: Long
  val detailId: Int
  val satelliteName: String
  val startTime: ZonedDateTime
  val endTime: ZonedDateTime
  val startAzimuth: Float
  val startElevation: Float
  val trainAngle: Float

  // 일회성 명령 플래그
  var stowCommandSent: Boolean = false
  var trainMoveCommandSent: Boolean = false
  var azElMoveCommandSent: Boolean = false
  var headerSent: Boolean = false
  var initialTrackingDataSent: Boolean = false

  // 진행 완료 플래그
  var trainMoveCompleted: Boolean = false
  var trainStabilizationCompleted: Boolean = false
  var azElMoveCompleted: Boolean = false
)
```

### 2.4 주요 메서드

| 메서드 | 역할 |
|------|------|
| `startScheduleTracking()` | 패스 스케줄 모니터링 시작 (100ms 주기) |
| `stopScheduleTracking()` | 모니터링 중지 + 상태 정리 |
| `generateAllPassScheduleTrackingDataAsync()` | 모든 위성 패스 스케줄 데이터 생성 (병렬) |
| `generatePassScheduleTrackingDataAsync(satelliteId, ...)` | 개별 위성 패스 데이터 생성 |
| `sendHeaderTrackingData(passId)` | 2.12.1 헤더 전송 |
| `sendInitialTrackingData(passId)` | 2.12.2 초기 데이터 전송 |
| `handleTrackingDataRequest(passId, ...)` | 2.12.3 데이터 요청 처리 |
| `moveStartAnglePosition(mstId, ...)` | 시작 위치로 이동 명령 |
| `passScheduleTimeOffsetCommand(offset)` | 추적 시간 오프셋 적용 |
| `getCurrentSelectedTrackingPass()` | 현재 추적 스케줄 조회 |
| `getNextSelectedTrackingPass()` | 다음 추적 스케줄 조회 |

### 2.5 핵심 특징

- 100ms 정밀 타이머로 스케줄 모니터링
- 2분 기준 Stow/시작 위치 자동 이동
- ScheduleTrackingContext로 스케줄별 상태 독립 관리
- 전역 유일 MstId 카운터로 ID 충돌 방지 (AtomicLong)
- Reactor Mono/Flux로 비동기 데이터 생성

### 2.6 TLE 캐시 관리

```kotlin
private val passScheduleTleCache = ConcurrentHashMap<String, Triple<String, String, String>>()
// satelliteId → (tleLine1, tleLine2, satelliteName)
```

---

## 3. ICDService (2,788줄)

### 3.1 클래스 구조

```kotlin
@Service
class ICDService {
  companion object {
    const val ICD_STX: Byte = 0x02
    const val ICD_ETX: Byte = 0x03
  }

  class Classify(dataStoreService, acsEventBus)
  class ReadStatus
  class SatelliteTrackOne
  class SatelliteTrackTwo
  class SatelliteTrackThree
  class WriteNTP
  class TimeOffset
  // ... 다수의 프로토콜 클래스
}
```

### 3.2 내부 클래스 목록

| 클래스 | 프로토콜 | 역할 |
|------|--------|------|
| `Classify` | - | UDP 수신 데이터 분류 |
| `ReadStatus` | 2.2 | 하드웨어 상태/각도 수신 |
| `SatelliteTrackOne` | 2.12.1 | 위성 추적 헤더 전송 |
| `SatelliteTrackTwo` | 2.12.2 | 위성 추적 초기 데이터 전송 |
| `SatelliteTrackThree` | 2.12.3 | 위성 추적 추가 데이터 전송 |
| `TimeOffset` | 2.11 | 시간 오프셋 명령 |
| `WriteNTP` | 2.15 | NTP 동기화 |
| `Stop` | 2.7 | 정지 명령 |
| `Emergency` | - | 비상 정지 |
| `Standby` | 2.3 | Standby 명령 |
| `MultiManualControl` | 2.5 | 멀티 매뉴얼 제어 |
| `SingleManualControl` | 2.6 | 싱글 매뉴얼 제어 |
| `FeedOnOff` | 2.8 | 피드 On/Off |
| `PositionOffset` | 2.9 | 위치 오프셋 |
| `MCOnOff` | 2.14 | MC On/Off |
| `ServoAlarmReset` | 2.16 | 서보 알람 리셋 |
| `ServoEncoderPreset` | 2.17 | 서보 엔코더 프리셋 |
| `DefaultInfo` | - | 기본 정보 |

### 3.3 Classify 클래스

```kotlin
class Classify(dataStoreService, acsEventBus) {
  fun receivedCmd(receiveData: ByteArray)
  // 바이트 배열 파싱 → PushData 변환 → DataStoreService 업데이트
}
```

### 3.4 데이터 프레임 구조

```kotlin
class SatelliteTrackOne {
  class GetDataFrame {
    fun fromByteArray(data: ByteArray): GetDataFrame?
  }
  class SetDataFrame {
    fun setDataFrame(): ByteArray
  }
}
```

### 3.5 특징

- STX(0x02) + 명령코드 + 데이터 + CRC16 + ETX(0x03) 프로토콜
- 패킷 타이밍 모니터링 (80ms 이상 지연 시 경고)
- ByteBuffer 기반 바이너리 파싱/생성
- 타입 안전한 GetDataFrame/SetDataFrame 클래스 구조

---

## 4. SunTrackService (979줄)

### 4.1 클래스 구조

```kotlin
@Service
class SunTrackService(
  - udpFwICDService: UdpFwICDService
  - dataStoreService: DataStoreService
  - threadManager: ThreadManager
  - solarOrekitCalculator: SolarOrekitCalculator
  - settingsService: SettingsService
)
```

### 4.2 상태 관리

```kotlin
enum class SunTrackState {
  IDLE,           // 대기
  INITIAL_TRAIN,  // 초기 Train 이동
  STABILIZING,    // Train 안정화
  TRACKING        // 실시간 태양 추적
}
```

### 4.3 주요 메서드

| 메서드 | 역할 |
|------|------|
| `startSunTrack(azSpeed, elSpeed, trainSpeed)` | 태양 추적 시작 |
| `stopSunTrack()` | 태양 추적 중지 |
| `isSunTrackActive(): Boolean` | 활성 여부 확인 |
| `getPerformanceInfo(): Map<String, Any>` | 성능 정보 |
| `getTodaySunInfo(): Map<String, Any>` | 오늘 태양 정보 |
| `getSunInfoForDate(date): Map<String, Any>` | 특정 날짜 태양 정보 |
| `getTodaySunrise()` | 일출 정보 |
| `getTodaySunset()` | 일몰 정보 |

### 4.4 특징

- 100ms 주기 타이머 기반 추적
- Train 각도 정규화 (±270도)
- 태양 고도각 음수 값 처리
- 성능 모니터링 (50ms 이상 처리 시 경고)

---

## 5. SettingsService (1,183줄)

### 5.1 구조

```kotlin
@Service
@Transactional
class SettingsService(
  - settingsRepository: SettingsRepository?
  - settingsHistoryRepository: SettingsHistoryRepository?
  - eventPublisher: ApplicationEventPublisher
)
```

### 5.2 설정 범주

| 범주 | 설정명 | 예시 |
|------|------|------|
| **위치** | location.* | latitude, longitude, altitude |
| **추적** | tracking.* | msInterval, durationDays, minElevationAngle |
| **Stow** | stow.angle.*, stow.speed.* | 각도, 속도 |
| **안테나** | antennaspec.* | trueNorthOffsetAngle, tiltAngle |
| **각도 제한** | anglelimits.* | min/max for az/el/train |
| **속도 제한** | speedlimits.* | min/max for az/el/train |
| **오프셋 제한** | angleoffsetlimits.* | az/el/train offset limits |
| **시간 오프셋** | timeoffsetlimits.* | min/max time offset |
| **알고리즘** | algorithm.* | geoMinMotion |
| **시스템** | system.* | UDP, 추적, 저장소, 성능 설정 |
| **태양 추적** | system.suntrack.* | 정확도 임계값, 검색 시간 |
| **위성 추적** | ephemeris.tracking.* | sourceMinElevationAngle, keyholeThreshold |

### 5.3 주요 메서드

| 메서드 | 역할 |
|------|------|
| `initialize()` | DB/RAM에서 설정값 로드 |
| `setSetting(key, value)` | 설정값 업데이트 + 변경 이벤트 발행 |
| `getSetting(key)` | 개별 설정값 조회 |
| `getAll()` | 모든 설정값 조회 |
| `setLocation/Tracking/StowAngles/...` | 범주별 대량 설정 |
| `getLocationSettings/...` | 범주별 설정값 조회 |

### 5.4 특징

- DB 미사용 시 RAM 전용 (no-db 프로필)
- ConcurrentHashMap으로 스레드 안전성 확보
- 설정 변경 시 ApplicationEvent 발행
- 타입별 자동 변환 (String → Double/Long/Integer 등)

---

## 6. 기타 서비스

### 6.1 DataStoreService (621줄)

- 실시간 안테나 상태 저장 (AtomicReference)
- UDP 연결 상태 관리
- 추적 상태별 데이터 병합 로직

### 6.2 UdpFwICDService (1,228줄)

- Netty 기반 UDP 통신
- 송/수신 스케줄러 관리
- CRC16 검증

### 6.3 HardwareErrorLogService (624줄)

- 하드웨어 에러 이벤트 처리
- 에러 로그 저장 및 조회
- CSV 내보내기

### 6.4 BatchStorageManager (313줄)

- 배치 데이터 저장 (MST/DTL)
- 청크 단위 저장으로 성능 최적화
- CSV 파일 생성

---

## 서비스 의존성 관계

```
┌─────────────────────────────────────┐
│     Frontend (Vue/Quasar)           │
└────────────────┬────────────────────┘
                 │ REST API / WebSocket
        ┌────────▼────────┐
        │   Controllers   │
        └────────┬────────┘
                 │
    ┌────────────┼────────────┐
    │            │            │
┌───▼────┐  ┌───▼─────┐  ┌──▼────┐
│Ephemeris│  │PassSched│  │SunTrack│
│Service  │  │Service  │  │Service │
└───┬─────┘  └───┬─────┘  └───┬────┘
    │            │            │
    │     ┌──────┴─────┬──────┘
    │     │            │
    └─────┼────────────┼─────────────┐
          │            │             │
      ┌───▼────────────▼────┐  ┌────▼────────┐
      │ DataStoreService   │  │Settings     │
      │ (실시간 데이터)     │  │Service      │
      └───┬────────────┬────┘  └─────┬──────┘
          │            │             │
    ┌─────▼──┐  ┌─────▼────────┐   │
    │ICD     │  │UDP/WebSocket │   │
    │Service │  │Services      │   │
    └────────┘  └──────────────┘   │
         │                          │
         └──────────┬───────────────┘
                    │
              ┌─────▼──────────┐
              │ Hardware/UDP   │
              │ (Positioner)   │
              └────────────────┘
```

---

## 리팩토링 권장사항

### 즉시 개선 필요

1. **EphemerisService 메서드 분할** (5,057줄)
   - 100줄 이상 메서드 분리
   - 상태머신 로직 별도 클래스로 추출

2. **PassScheduleService 상태머신 통합**
   - v1.0 + v2.0 상태머신 공존 → v2.0으로 통일

3. **TLE 캐시 관리 중앙화**
   - 각 Service가 독립적으로 관리 → `TLECacheManager` 도입

4. **데이터 저장 로직 분리**
   - 실시간 + 배치 + CSV 혼재 → `StorageManager` 계층 도입

5. **에러 처리 구체화**
   - 광범위 catch(Exception) 제거
   - 구체적 예외 타입 정의

---

**문서 버전**: 1.0.0
**작성자**: BE Expert Agent
**최종 검토**: 2026-01-15
