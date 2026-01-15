# 위성 추적 (Satellite Tracking)

> ACS 시스템의 핵심 도메인: 위성의 위치를 계산하고 안테나를 지향

## 개요

```
TLE 데이터 → Orekit 궤도 전파 → 위성 위치 (ECI)
     ↓
Topocentric 변환 → Az/El → 3축 변환 → ±270° Limit → 포지셔너
```

## 핵심 개념

### TLE (Two-Line Element)

위성의 궤도 요소를 2줄로 표현한 형식

```
ISS (ZARYA)
1 25544U 98067A   24001.00000000  .00000000  00000-0  00000-0 0  0000
2 25544  51.6400 000.0000 0000000  00.0000 000.0000 00.00000000000000
```

**주요 요소:**
- NORAD ID: 위성 식별자 (25544 = ISS)
- 경사각(Inclination): 궤도면과 적도면의 각도
- 이심률(Eccentricity): 궤도의 타원 정도
- RAAN: 승교점 적경
- 평균 근점 이각: 궤도상 위치

### Orekit 라이브러리

Java 기반 우주 역학 라이브러리 (v13.0 사용)

**핵심 클래스:**
```kotlin
// 궤도 전파
TLEPropagator.selectExtrapolator(tle)

// 지상국 기준 좌표
TopocentricFrame(earthFrame, groundStation, "ACS")

// 시간 처리
AbsoluteDate(date, TimeScalesFactory.getUTC())
```

### 좌표계

| 좌표계 | 설명 | 용도 |
|-------|------|------|
| ECI (J2000) | 지구 중심 관성 좌표계 | 궤도 계산 |
| ECEF (ITRF) | 지구 고정 좌표계 | 지상 위치 |
| Topocentric | 지상국 기준 좌표계 | 안테나 지향 |

## 좌표 변환 파이프라인

### 전체 흐름

```
┌─────────────────────────────────────────────────────────────┐
│                    1. 궤도 전파 (OrekitCalculator)           │
│  TLE → SGP4/SDP4 → 위성 위치 (ECI)                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    2. 좌표 변환 (TopocentricFrame)           │
│  ECI → Topocentric → Azimuth/Elevation                      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                3. 3축 변환 (CoordinateTransformer)           │
│  2축 (Az/El) → 3축 (Az/El/Train)                            │
│  Train 각도 적용, 오프셋 보정                                 │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                4. 각도 제한 (LimitAngleCalculator)           │
│  ±270° 범위 변환, 최적 경로 계산                             │
│  Limit Crossing 감지                                         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    5. 명령 전송 (ICDService)                 │
│  최종 각도 → SetDataFrame → UDP 전송                         │
└─────────────────────────────────────────────────────────────┘
```

### 상세 변환 과정

#### 1. Azimuth/Elevation 계산

```kotlin
// OrekitCalculator.kt
fun calculateAzEl(tle: TLE, time: AbsoluteDate, groundStation: GeodeticPoint): AzEl {
    val propagator = TLEPropagator.selectExtrapolator(tle)
    val state = propagator.propagate(time)

    val topoFrame = TopocentricFrame(earthFrame, groundStation, "ACS")
    val topoCoords = state.getPVCoordinates(topoFrame)

    return AzEl(
        azimuth = topoCoords.position.alpha,    // 라디안
        elevation = topoCoords.position.delta   // 라디안
    )
}
```

#### 2. 3축 변환

```kotlin
// CoordinateTransformer.kt
fun transformToThreeAxis(azEl: AzEl, trainAngle: Double): ThreeAxisAngles {
    // Train 각도에 따른 Az/El 조정
    val adjustedAz = adjustAzimuthForTrain(azEl.azimuth, trainAngle)
    val adjustedEl = adjustElevationForTrain(azEl.elevation, trainAngle)

    return ThreeAxisAngles(
        azimuth = adjustedAz,
        elevation = adjustedEl,
        train = trainAngle
    )
}
```

#### 3. ±270° 변환

```kotlin
// LimitAngleCalculator.kt
fun convertToLimitRange(angle: Double): Double {
    // -270° ~ +270° 범위로 변환
    var result = angle % 360.0
    if (result > 270.0) result -= 360.0
    if (result < -270.0) result += 360.0
    return result
}

fun calculateOptimalPath(current: Double, target: Double): OptimalPath {
    // 최단 경로 또는 Limit 회피 경로 계산
    val direct = target - current
    val wrapped = if (direct > 0) direct - 360 else direct + 360

    return if (abs(direct) <= abs(wrapped)) {
        OptimalPath(target, PathType.DIRECT)
    } else {
        OptimalPath(current + wrapped, PathType.WRAPPED)
    }
}
```

## 코드 위치

### Algorithm 계층

| 파일 | 줄 수 | 역할 |
|------|------|------|
| `algorithm/satellitetracker/OrekitCalculator.kt` | 627 | 궤도 전파, Az/El 계산 |
| `algorithm/satellitetracker/SatelliteTrackingProcessor.kt` | 1,387 | 데이터 처리 파이프라인 |
| `algorithm/axistransformation/CoordinateTransformer.kt` | - | 2축→3축 변환 |
| `algorithm/axislimitangle/LimitAngleCalculator.kt` | - | ±270° 변환 |

### Service 계층

| 파일 | 줄 수 | 역할 |
|------|------|------|
| `service/mode/EphemerisService.kt` | 5,057 | Ephemeris 모드 상태머신 + 추적 |
| `service/mode/PassScheduleService.kt` | 3,846 | PassSchedule 모드 상태머신 + 추적 |

## 주요 서비스 상세

### EphemerisService (추적 기능)

```kotlin
// service/mode/EphemerisService.kt
@Service
class EphemerisService(
    private val orekitCalculator: OrekitCalculator,
    private val processor: SatelliteTrackingProcessor,
    private val icdService: ICDService
) {
    // TLE 캐시
    private val satelliteTleCache = ConcurrentHashMap<String, TLECacheData>()

    // 상태머신
    enum class TrackingState {
        IDLE, PREPARING, WAITING, TRACKING, COMPLETED, ERROR
    }

    enum class PreparingPhase {
        TRAIN_MOVING, TRAIN_STABILIZING, MOVING_TO_TARGET
    }

    /**
     * 추적 시작
     */
    suspend fun trackStart(satelliteId: String, schedule: TrackingSchedule) {
        state = TrackingState.PREPARING
        // Train 이동 → 목표 위치 이동 → 추적 시작
    }

    /**
     * 30ms 타이머 콜백
     */
    fun handleTimerTick() {
        when (state) {
            TrackingState.PREPARING -> handlePreparing()
            TrackingState.WAITING -> handleWaiting()
            TrackingState.TRACKING -> handleTracking()
            else -> { }
        }
    }

    /**
     * 실시간 위치 계산 및 전송
     */
    private fun handleTracking() {
        val currentTime = getCurrentUTC()
        val azEl = orekitCalculator.calculateAzEl(tle, currentTime, groundStation)
        val threeAxis = processor.applyAxisTransformation(azEl, trainAngle)
        val limitedAngles = processor.convertToLimitAngles(threeAxis)

        icdService.sendTrackingCommand(limitedAngles)
    }
}
```

### SatelliteTrackingProcessor

```kotlin
// algorithm/satellitetracker/SatelliteTrackingProcessor.kt (1,387줄)
class SatelliteTrackingProcessor {

    /**
     * 위성 추적 데이터 처리
     */
    fun processSatelliteTracking(rawPosition: RawPosition): ProcessedPosition {
        val azEl = calculateAzEl(rawPosition)
        val threeAxis = applyAxisTransformation(azEl)
        val limited = convertToLimitAngle(threeAxis)
        return ProcessedPosition(limited, calculateMetrics(limited))
    }

    /**
     * 3축 변환 적용
     */
    fun applyAxisTransformation(azEl: AzEl, trainAngle: Double): AxisAngles {
        return CoordinateTransformer.transformToThreeAxis(azEl, trainAngle)
    }

    /**
     * ±270° 범위 변환
     */
    fun convertToLimitAngle(angle: Double): Double {
        return LimitAngleCalculator.convertToLimitRange(angle)
    }

    /**
     * Keyhole 감지
     */
    fun detectKeyhole(elevation: Double): Boolean {
        return elevation > KEYHOLE_THRESHOLD  // 예: 85°
    }

    /**
     * 추적 메트릭 계산
     */
    fun calculateMetrics(path: List<TrackingPoint>): TrackingMetrics {
        return TrackingMetrics(
            maxElevation = path.maxOf { it.elevation },
            duration = path.last().time - path.first().time,
            keyholeDetected = path.any { detectKeyhole(it.elevation) }
        )
    }
}
```

### OrekitCalculator

```kotlin
// algorithm/satellitetracker/OrekitCalculator.kt (627줄)
class OrekitCalculator {

    /**
     * 위성 위치/속도 계산
     */
    fun calculateSatellitePositionAndVelocity(
        tle: TLE,
        time: AbsoluteDate
    ): PVCoordinates {
        val propagator = TLEPropagator.selectExtrapolator(tle)
        return propagator.propagate(time).pvCoordinates
    }

    /**
     * 가시 구간 탐지
     */
    fun detectVisibilityPeriods(
        tle: TLE,
        start: AbsoluteDate,
        end: AbsoluteDate,
        groundStation: GeodeticPoint,
        minElevation: Double = 0.0
    ): List<VisibilityPeriod> {
        val propagator = TLEPropagator.selectExtrapolator(tle)
        val detector = ElevationDetector(groundStation)
            .withConstantElevation(minElevation)

        // 이벤트 감지 로직
        return detectEvents(propagator, detector, start, end)
    }

    /**
     * 추적 스케줄 생성
     */
    fun generateSatelliteTrackingSchedule(
        tle: TLE,
        visibility: VisibilityPeriod,
        intervalMs: Long = 1000
    ): List<TrackingPoint> {
        val points = mutableListOf<TrackingPoint>()
        var time = visibility.start

        while (time.isBefore(visibility.end)) {
            val azEl = calculateAzEl(tle, time, groundStation)
            points.add(TrackingPoint(time, azEl))
            time = time.shiftedBy(intervalMs / 1000.0)
        }

        return points
    }
}
```

## Keyhole 처리

### Keyhole 정의

고도각(Elevation)이 매우 높을 때 (예: >85°) 발생하는 특수 상황

```
          ↑ 위성
          │
          │  Keyhole Zone (El > 85°)
    ┌─────┴─────┐
    │           │
────┼───────────┼──── 수평면
    │  안테나   │
    └───────────┘
```

### Keyhole 처리 로직

```kotlin
// SatelliteTrackingProcessor.kt
fun handleKeyhole(currentPos: Position, targetPos: Position): Position {
    if (detectKeyhole(targetPos.elevation)) {
        // Keyhole 진입 시 최적 Train 각도 계산
        val optimalTrain = calculateOptimalTrainForKeyhole(targetPos)
        return Position(
            azimuth = targetPos.azimuth,
            elevation = targetPos.elevation,
            train = optimalTrain
        )
    }
    return targetPos
}
```

## 관련 모드

### EphemerisDesignation 모드

- TLE 입력 → 스케줄 생성 → 수동 추적 시작
- 상태머신: IDLE → PREPARING → WAITING → TRACKING → COMPLETED

### PassSchedule 모드

- 사전 등록된 스케줄 기반 자동 추적
- 상태머신: IDLE → MONITORING → PRE_TRACK → TRACKING → POST_TRACK

## 주의사항

- **각도 단위**: 내부 계산은 라디안, UI 표시는 도(°)
- **시간대**: 내부 UTC, 표시 로컬 시간
- **Orekit 초기화**: orekit-data 경로 필수 설정
- **TLE 유효기간**: 일반적으로 1-2주, 정밀도 저하
- **Limit 범위**: 포지셔너는 ±270° 범위만 이동 가능
- **Train 각도**: Keyhole 회피를 위한 3축 제어

## 참조

- [Orekit 공식 문서](https://www.orekit.org/)
- [Celestrak TLE](https://celestrak.org/)
- [안테나 제어](antenna-control.md)
- [모드 시스템](mode-system.md)
- [심층 분석: Algorithm](../analysis/domain-logic/satellite-tracking.md)

---

**최종 수정:** 2026-01-15
**분석 기반:** Phase 1-4 코드베이스 심층 분석

**변경 이력:**
- 2026-01-15: Gap 분석 반영 - 좌표 변환 파이프라인 상세화, LimitAngleCalculator/CoordinateTransformer 추가
