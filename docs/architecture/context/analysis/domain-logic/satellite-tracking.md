# 위성 추적 알고리즘 분석

> ACS Backend Algorithm 계층 심층 분석 문서

**최종 업데이트**: 2026-01-15
**분석 대상**: algorithm/ 디렉토리 (약 4,500줄)

---

## 개요

ACS의 Algorithm 계층은 위성/태양 추적에 필요한 모든 좌표 계산 및 변환 로직을 담당합니다.

### 알고리즘 구조도

```
algorithm/
├── satellitetracker/          # 위성 추적
│   ├── impl/
│   │   └── OrekitCalculator.kt      # TLE 기반 궤도 계산 (628줄)
│   ├── processor/
│   │   ├── SatelliteTrackingProcessor.kt  # 데이터 변환/분석 (1,388줄)
│   │   └── model/ProcessedTrackingData.kt # 처리 결과 모델
│   └── model/SatelliteTrackData.kt  # 추적 데이터 모델
├── suntrack/                  # 태양 추적
│   ├── impl/
│   │   ├── SolarOrekitCalculator.kt # Orekit 기반 (891줄)
│   │   ├── SPACalculator.kt         # SPA 알고리즘 (352줄)
│   │   └── Grena3Calculator.kt      # Grena3 알고리즘 (90줄)
│   ├── interfaces/
│   │   └── SunPositionCalculator.kt # 공통 인터페이스
│   └── model/SunTrackData.kt        # 태양 위치 모델
├── axistransformation/        # 좌표 변환
│   └── CoordinateTransformer.kt     # 3축 좌표 변환 (167줄)
├── axislimitangle/            # 각도 제한
│   └── LimitAngleCalculator.kt      # +/- 270도 변환 (739줄)
└── elevation/                 # 고도 계산
    └── ElevationCalculator.kt       # 지형 고도 조회 (258줄)
```

---

## 1. OrekitCalculator

### 1.1 개요

| 항목 | 내용 |
|-----|------|
| **위치** | `algorithm/satellitetracker/impl/OrekitCalculator.kt` |
| **크기** | 628줄 |
| **역할** | TLE 데이터 기반 위성 위치 계산 및 가시성 기간 탐지 |
| **라이브러리** | Orekit 13.0.2 |

### 1.2 입력/출력

**입력**:
- TLE 데이터 (Line1, Line2)
- 지상국 위치 (위도, 경도, 고도)
- 시간 (ZonedDateTime)
- 최소 고도각 (minElevation)

**출력**:
- `SatelliteTrackData`: 방위각, 고도각, 거리, 위성 고도, 타임스탬프
- `SatelliteTrackingSchedule`: 패스 스케줄 (여러 패스의 추적 데이터 포함)

### 1.3 TLE 처리

```kotlin
// TLE 파싱 및 전파기 생성
val tle = TLE(tleLine1, tleLine2)
val propagator = TLEPropagator.selectExtrapolator(tle)
```

TLEPropagator는 내부적으로 **SGP4/SDP4 알고리즘**을 사용:
- **SGP4**: 저궤도 위성 (주기 < 225분)
- **SDP4**: 심우주 위성 (주기 >= 225분)

### 1.4 위성 위치 계산

#### 좌표 변환 과정

```
1. TLE → 궤도 요소
2. 궤도 전파 (propagate) → ECI 좌표
3. ECI → 지상국 기준 (TopocentricFrame)
4. 직교 좌표 → 방위각/고도각
```

#### 핵심 코드

```kotlin
// 지상국 기준 좌표계 설정
val stationPosition = GeodeticPoint(
    FastMath.toRadians(latitude),
    FastMath.toRadians(longitude),
    altitude
)
val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")

// 위성 위치 계산
val state = propagator.propagate(date)
val pvInStation = state.getPVCoordinates(stationFrame)
val posInStation = pvInStation.position

// 방위각/고도각 계산
val elevation = FastMath.toDegrees(FastMath.asin(z / distance))
val azimuth = FastMath.toDegrees(FastMath.atan2(x, y))
```

#### 수학적 원리

**고도각 (Elevation)**:
```
El = arcsin(z / r)
where r = sqrt(x^2 + y^2 + z^2)
```

**방위각 (Azimuth)**:
```
Az = atan2(x, y)
정규화: Az < 0 이면 Az += 360
```

### 1.5 가시성 기간 탐지 (ElevationDetector)

Orekit의 **ElevationDetector**를 사용하여 위성이 최소 고도각을 넘는 시점(AOS/LOS)을 정확히 탐지.

```kotlin
val elevationDetector = ElevationDetector(stationFrame)
    .withConstantElevation(FastMath.toRadians(minElevation.toDouble()))
    .withMaxCheck(60.0)      // 최대 체크 간격 1분
    .withThreshold(1.0e-3)   // 이벤트 시점 정밀도 1ms
    .withHandler(customHandler)

propagator.addEventDetector(elevationDetector)
propagator.propagate(startAbsoluteDate, endAbsoluteDate)
```

**이벤트 타입**:
- **AOS (Acquisition of Signal)**: 위성이 지평선 위로 올라옴 (increasing = true)
- **LOS (Loss of Signal)**: 위성이 지평선 아래로 내려감 (increasing = false)

### 1.6 Orekit 클래스 사용

| Orekit 클래스 | 용도 |
|--------------|------|
| `TLE` | TLE 데이터 파싱 |
| `TLEPropagator` | SGP4/SDP4 궤도 전파 |
| `AbsoluteDate` | Orekit 시간 표현 |
| `TimeScalesFactory` | UTC 시간 척도 |
| `GeodeticPoint` | 지리 좌표 (위도/경도/고도) |
| `TopocentricFrame` | 지상국 기준 좌표계 |
| `OneAxisEllipsoid` | 지구 타원체 모델 |
| `ElevationDetector` | 고도각 이벤트 탐지 |
| `SpacecraftState` | 우주선 상태 (위치, 속도) |

---

## 2. SatelliteTrackingProcessor

### 2.1 개요

| 항목 | 내용 |
|-----|------|
| **위치** | `algorithm/satellitetracker/processor/SatelliteTrackingProcessor.kt` |
| **크기** | 1,388줄 |
| **역할** | OrekitCalculator 결과를 3축 변환 및 각도 제한 적용 |
| **의존성** | CoordinateTransformer, LimitAngleCalculator, SettingsService |

### 2.2 데이터 처리 파이프라인

```
OrekitCalculator (2축 원본)
        │
        ▼
┌──────────────────────────────────────────┐
│     structureOriginalData()              │
│     - Mst/Dtl 구조화                      │
│     - 메타데이터 계산 (MaxElevation 등)   │
│     - Keyhole 판단                        │
└──────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────┐
│     applyAxisTransformation()            │
│     - 3축 좌표 변환 (Train=0)             │
│     - CoordinateTransformer 사용          │
└──────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────┐
│     applyAngleLimitTransformation()      │
│     - +/- 270도 범위 변환                 │
│     - LimitAngleCalculator 사용           │
└──────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────┐
│     Keyhole 발생 시                       │
│     - Train != 0 재계산                   │
│     - findOptimalTrainAngle() 최적화      │
└──────────────────────────────────────────┘
        │
        ▼
     ProcessedTrackingData (8가지 DataType)
```

### 2.3 DataType 분류

| DataType | Train | 각도 제한 | 설명 |
|----------|-------|----------|------|
| `original` | N/A | X | 원본 2축 데이터 |
| `axis_transformed` | 0 | X | 3축 변환 (기본) |
| `final_transformed` | 0 | O | 최종 변환 데이터 |
| `keyhole_axis_transformed` | !=0 | X | Keyhole 3축 변환 |
| `keyhole_final_transformed` | !=0 | O | Keyhole 최종 변환 |
| `keyhole_optimized_axis_transformed` | 최적화 | X | Keyhole 최적화 3축 |
| `keyhole_optimized_final_transformed` | 최적화 | O | Keyhole 최적화 최종 |

### 2.4 Keyhole 판단 로직

**Keyhole**: 위성이 천정 부근을 지나갈 때 방위각이 급격히 변하는 현상

```kotlin
// Keyhole 판단 기준: 최대 방위각 각속도
val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
val threshold = settingsService.keyholeAzimuthVelocityThreshold
val isKeyhole = maxAzRate >= threshold
```

**각속도 계산** (10개 구간 누적):
```kotlin
// 1초간 (10개 x 100ms) Azimuth 변화량 누적
for (j in (i - 9)..i) {
    var azDiff = currentAz - prevAz
    // 360도 경계 처리
    if (azDiff > 180) azDiff -= 360
    if (azDiff < -180) azDiff += 360
    azSum += abs(azDiff)
}
val currentAzRate = azSum  // deg/s
```

### 2.5 Train 각도 계산

**목표**: 안테나 서쪽(+7도) 방향을 위성 Azimuth로 회전

```kotlin
private fun calculateTrainAngle(azimuth: Double): Double {
    // Azimuth를 0-360 범위로 정규화
    var normalizedAz = azimuth % 360.0
    if (normalizedAz < 0) normalizedAz += 360.0

    // 두 가지 경로 계산
    val option1 = normalizedAz - 270.0  // 기본 계산
    val option2 = if (option1 < 0) option1 + 360.0 else option1 - 360.0

    // +/- 270도 범위 내 유효한 옵션 중 최소 절댓값 선택
    return validOptions.minByOrNull { Math.abs(it) } ?: option1
}
```

### 2.6 하이브리드 3단계 그리드 서치

Keyhole 발생 시 최적의 Train 각도를 찾는 알고리즘:

```
1단계 (초기값): 기존 방식으로 초기 Train 계산
        │
        ▼
2단계 (대략적 탐색): 초기값 +/- 90도, 10도 간격
        - 약 19회 계산
        │
        ▼
3단계 (정밀 탐색): 최적 구간 +/- 5도, 0.5도 간격
        - 약 21회 계산
        │
        ▼
      최적 Train 각도 (총 약 41회 계산, 정밀도 0.5도)
```

---

## 3. CoordinateTransformer

### 3.1 개요

| 항목 | 내용 |
|-----|------|
| **위치** | `algorithm/axistransformation/CoordinateTransformer.kt` |
| **크기** | 167줄 |
| **역할** | 2축 좌표를 기울어진 3축 회전체 좌표계로 변환 |
| **알고리즘** | 3D 회전 행렬 (Z축 -> Y축 순서) |

### 3.2 변환 원리

```
표준 좌표계 (Az, El)
        │
        ▼
3D 직교 좌표 변환 (x, y, z)
        │
        ▼
Z축 회전 (Train 각도) - 수직축 기준 회전
        │
        ▼
Y축 회전 (Tilt 각도) - 수평축 기준 기울기
        │
        ▼
직교 좌표 → 방위각/고도각
```

### 3.3 수학적 공식

**1. 구면 좌표 -> 직교 좌표**:
```
x = cos(El) * sin(Az)
y = cos(El) * cos(Az)
z = sin(El)
```

**2. Z축 회전 (Train)**:
```
x' = x * cos(Train) - y * sin(Train)
y' = x * sin(Train) + y * cos(Train)
z' = z
```

**3. Y축 회전 (Tilt)**:
```
x'' = x' * cos(Tilt) + z' * sin(Tilt)
y'' = y'
z'' = -x' * sin(Tilt) + z' * cos(Tilt)
```

**4. 직교 좌표 -> 구면 좌표**:
```
Az' = atan2(x'', y'')
El' = asin(z'')
```

### 3.4 역변환

`inverseTransformCoordinatesWithRotator()`: 역순서로 역회전 적용
- Y축 역회전 (-Tilt)
- Z축 역회전 (-Train)

---

## 4. LimitAngleCalculator

### 4.1 개요

| 항목 | 내용 |
|-----|------|
| **위치** | `algorithm/axislimitangle/LimitAngleCalculator.kt` |
| **크기** | 739줄 |
| **역할** | 0-360도 방위각을 +/- 270도 포지셔너 범위로 변환 |
| **핵심 과제** | 회전 방향성 보존, 연속성 유지 |

### 4.2 회전 방향 분석

```kotlin
enum class RotationDirection {
    CLOCKWISE,          // 시계방향
    COUNTER_CLOCKWISE,  // 반시계방향
    MIXED,              // 혼합
    UNKNOWN             // 불명
}
```

**분석 로직**:
```kotlin
for (i in 1 until azimuths.size) {
    val rawDelta = current - prev
    val normalizedDelta = when {
        rawDelta > 180.0 -> rawDelta - 360.0   // 반시계방향 경계 통과
        rawDelta < -180.0 -> rawDelta + 360.0  // 시계방향 경계 통과
        else -> rawDelta
    }
    // 양수: 시계방향, 음수: 반시계방향
}
```

### 4.3 경계 통과 상태

```kotlin
enum class BoundaryCrossing {
    WITHIN_RANGE,           // 0-270도 범위 내
    EXCEEDS_270,            // 270도 초과
    CROSSES_270_BOUNDARY    // 270도/0도 경계 통과
}
```

### 4.4 변환 알고리즘

```
1. 회전 방향 분석
        │
        ▼
2. 270도 경계 통과 여부 확인
        │
        ▼
3. 시작 각도 결정 (회전 방향/경계 고려)
        │
        ▼
4. 각 포인트 순차 변환 (방향성 유지)
        │
        ▼
5. +/- 270도 범위 정규화
        │
        ▼
6. 연속성 검증 및 품질 평가
```

### 4.5 정규화 로직

```kotlin
private fun normalizeWithDirectionPreservation(angle: Double, ...): Double {
    var normalized = angle

    // 기본 +/- 270도 범위 정규화
    while (normalized > 270.0) normalized -= 360.0
    while (normalized < -270.0) normalized += 360.0

    // 비정상적인 방향 전환 감지 및 보정
    if (abs(actualDelta) > 300.0) {
        // 대안 각도 중 연속성 유지하는 최적 후보 선택
    }

    return normalized
}
```

### 4.6 품질 평가

| 항목 | 배점 | 기준 |
|-----|------|------|
| 범위 준수 | 30점 | 범위 초과 개수 |
| 연속성 | 40점 | 10도 이상 점프 개수 |
| 변화량 보존 | 30점 | 원본과 변환 delta 차이 |

---

## 5. 태양 추적

### 5.1 SolarOrekitCalculator

| 항목 | 내용 |
|-----|------|
| **위치** | `algorithm/suntrack/impl/SolarOrekitCalculator.kt` |
| **크기** | 891줄 |
| **역할** | Orekit 기반 고정밀 태양 위치 계산 |
| **특징** | UT1/UTC 시간 척도 비교, EOP 데이터 활용 |

#### Orekit 클래스 사용

| 클래스 | 용도 |
|--------|------|
| `CelestialBody` (sun) | 태양 천체 객체 |
| `TopocentricFrame` | 지상국 기준 좌표계 |
| `OneAxisEllipsoid` | 지구 타원체 |
| `TimeScalesFactory` | UTC/UT1 시간 척도 |
| `IERSConventions` | 지구 회전 파라미터 규약 |

#### 태양 위치 계산

```kotlin
private fun calculateSunPosition(date: AbsoluteDate): SunPosition {
    val sunPV: PVCoordinates = sun.getPVCoordinates(date, groundStation)
    val sunPosition: Vector3D = sunPV.position

    val range = sunPosition.norm
    val elevation = FastMath.asin(sunPosition.z / range)
    var azimuth = FastMath.atan2(sunPosition.x, sunPosition.y)

    // 방위각 0-360 정규화
    if (azimuth < 0) azimuth += 2 * FastMath.PI

    return SunPosition(
        azimuthDegrees = FastMath.toDegrees(azimuth),
        elevationDegrees = FastMath.toDegrees(elevation),
        rangeKm = range / 1000.0,
        dateTime = absoluteDateToLocalDateTime(date)
    )
}
```

#### 일출/일몰 탐지

**진짜 일출**: 이전 고도 < 0 AND 현재 고도 >= 0
**진짜 일몰**: 이전 고도 >= 0 AND 현재 고도 < 0

```kotlin
if (prevPos != null &&
    !prevPos.isSunVisible() &&
    currentPosition.isSunVisible()) {
    return currentPosition  // 일출
}
```

### 5.2 SPACalculator vs Grena3Calculator

| 항목 | SPA | Grena3 |
|-----|-----|--------|
| **정확도** | ~0.0003도 (2000-6000년) | ~0.01도 (2010-2110년) |
| **계산 속도** | 느림 | 빠름 |
| **용도** | 고정밀 계산 | 실시간 추적 |
| **라이브러리** | solarpositioning | solarpositioning |

#### 공통 인터페이스

```kotlin
interface SunPositionCalculator {
    fun calculatePosition(dateTime, latitude, longitude, elevation): SunTrackData
    fun calculateSunrise(date, latitude, longitude): ZonedDateTime
    fun calculateSunset(date, latitude, longitude): ZonedDateTime
}
```

#### 천정각 -> 고도각 변환

```kotlin
// 라이브러리 출력: 천정각 (Zenith Angle)
// 시스템 사용: 고도각 (Elevation)
val elevation = 90.0 - zenithAngle
```

---

## 핵심 데이터 타입

### SatelliteTrackData

```kotlin
data class SatelliteTrackData(
    val azimuth: Double,           // 방위각 (도)
    val elevation: Double,         // 고도각 (도)
    val timestamp: ZonedDateTime?, // 시간
    val range: Double?,            // 지상국-위성 거리 (km)
    val altitude: Double?          // 위성 고도 (km)
)
```

### SunTrackData

```kotlin
data class SunTrackData(
    val azimuth: Float,            // 방위각 (도)
    val elevation: Float,          // 고도각 (도)
    val timestamp: ZonedDateTime?, // 시간
    val azimuthRate: Float?,       // 방위각 변화율 (도/분)
    val elevationRate: Float?,     // 고도각 변화율 (도/분)
    val trackingMode: String?      // 추적 모드 (CURRENT/RATE)
)
```

### ProcessedTrackingData

```kotlin
data class ProcessedTrackingData(
    val originalMst: List<Map<String, Any?>>,
    val originalDtl: List<Map<String, Any?>>,
    val axisTransformedMst: List<Map<String, Any?>>,
    val axisTransformedDtl: List<Map<String, Any?>>,
    val finalTransformedMst: List<Map<String, Any?>>,
    val finalTransformedDtl: List<Map<String, Any?>>,
    // ... Keyhole 관련 4가지 추가 타입
)
```

---

## 단위 변환 규칙

### 각도

| 컨텍스트 | 내부 단위 | 표시 단위 |
|---------|----------|----------|
| Orekit 입력 | 라디안 | - |
| Orekit 출력 | 라디안 | - |
| 데이터 저장 | 도 (degree) | - |
| UI 표시 | - | 도 (degree) |

**변환 함수**:
```kotlin
FastMath.toRadians(degrees)  // 도 -> 라디안
FastMath.toDegrees(radians)  // 라디안 -> 도
```

### 거리

| 항목 | 내부 단위 | 표시 단위 |
|-----|----------|----------|
| 지상국-위성 거리 | m (미터) | km |
| 위성 고도 | m (미터) | km |

### 시간

| 컨텍스트 | 시간대 |
|---------|--------|
| Orekit 계산 | UTC |
| 데이터 저장 | UTC (ZonedDateTime) |
| UI 표시 | 로컬 시간 |

**Orekit 시간 변환**:
```kotlin
// ZonedDateTime -> AbsoluteDate
fun toAbsoluteDate(dateTime: ZonedDateTime): AbsoluteDate {
    return AbsoluteDate(
        dateTime.year, dateTime.monthValue, dateTime.dayOfMonth,
        dateTime.hour, dateTime.minute, dateTime.second + dateTime.nano / 1e9,
        TimeScalesFactory.getUTC()
    )
}

// AbsoluteDate -> ZonedDateTime
fun toZonedDateTime(absoluteDate: AbsoluteDate): ZonedDateTime {
    val components = absoluteDate.getComponents(utcTimeScale)
    // ... 변환 로직
}
```

---

## 좌표계 정리

### 사용 좌표계

| 좌표계 | 약어 | 설명 | 사용처 |
|--------|------|------|--------|
| 지구 중심 관성 | ECI | 지구 중심, 고정 방향 | 궤도 계산 |
| 지구 중심 고정 | ECEF | 지구 중심, 지구와 회전 | 위치 변환 |
| 지평 좌표계 | Topocentric | 지상국 기준 | 방위각/고도각 |
| 측지 좌표계 | Geodetic | 위도/경도/고도 | 지상국 위치 |

### 좌표 변환 흐름

```
TLE (궤도 요소)
     │
     ▼
궤도 전파 (SGP4/SDP4)
     │
     ▼
ECI 좌표 (x, y, z)
     │
     ▼ [Orekit 내부 변환]
     │
ECEF 좌표
     │
     ▼ [TopocentricFrame]
     │
지상국 기준 좌표 (x, y, z)
     │
     ▼ [수학적 변환]
     │
방위각 (Azimuth), 고도각 (Elevation)
     │
     ▼ [CoordinateTransformer]
     │
3축 좌표 (Tilt, Train 적용)
     │
     ▼ [LimitAngleCalculator]
     │
포지셔너 좌표 (+/- 270도)
```

---

## 성능 고려사항

### OrekitCalculator

| 작업 | 복잡도 | 비고 |
|-----|-------|------|
| TLE 파싱 | O(1) | 단일 파싱 |
| 궤도 전파 | O(1) per point | SGP4 계산 |
| 가시성 탐지 | O(n) | 이벤트 기반 최적화 |
| 상세 데이터 생성 | O(n) | n = 데이터 포인트 수 |

**최적화**:
- ElevationDetector: 이벤트 기반으로 필요한 시점만 계산
- propagator 재사용: 같은 TLE에 대해 propagator 캐싱 가능

### SatelliteTrackingProcessor

| 작업 | 복잡도 | 비고 |
|-----|-------|------|
| 좌표 변환 | O(n) | n = 데이터 포인트 |
| 각도 제한 변환 | O(n) | 순차 처리 |
| 메트릭 계산 | O(n) | 전체 순회 |
| 최적화 탐색 | O(41 * n) | 그리드 서치 |

**최적화**:
- 병렬 처리 가능 (패스별 독립 계산)
- Train 각도 최적화: 계산 횟수 제한 (약 41회)

### LimitAngleCalculator

| 작업 | 복잡도 | 비고 |
|-----|-------|------|
| 방향 분석 | O(n) | 전체 순회 |
| 변환 | O(n) | 순차 처리 |
| 품질 평가 | O(n) | 전체 순회 |

---

## 엣지 케이스

### 1. 극지방 위성 추적

- 방위각이 급격히 변하는 경우 발생
- 360도/0도 경계 통과 빈번
- LimitAngleCalculator의 방향성 보존 중요

### 2. 자정 경계

- ZonedDateTime 날짜 변경 처리
- Orekit AbsoluteDate 연속성 보장

### 3. Keyhole (천정 통과)

- 최대 고도각 90도 근처
- 방위각 각속도 급증
- Train 각도 조절로 완화

### 4. 일출/일몰 극지방

- 백야/극야 현상
- 일출/일몰 찾지 못하는 경우 기본값 반환

---

## 참고 문서

- [Orekit 공식 문서](https://www.orekit.org/static/apidocs/)
- [SGP4 알고리즘](https://celestrak.org/publications/AIAA/2006-6753/)
- [solarpositioning 라이브러리](https://github.com/klausbrunner/solarpositioning)
- [SPA 알고리즘 (NREL)](https://midcdmz.nrel.gov/spa/)

---

**문서 버전**: 1.0.0
**작성자**: Algorithm Expert Agent
**최종 검토**: 2026-01-15
