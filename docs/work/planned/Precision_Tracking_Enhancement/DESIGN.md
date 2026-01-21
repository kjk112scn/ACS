# 정밀 추적 시스템 상세 설계

## 1. 현재 아키텍처 분석

### 데이터 흐름

```
[TLE 데이터] → [Orekit] → [이론치 생성 (100ms 간격)]
                              ↓
                         [RAM 캐시]
                              ↓
[하드웨어] → [실측치 수신] → [매칭 로직] → [tracking_result DB]
                              ↑
                         [현재: 정수 인덱스]
```

### 현재 매칭 로직 위치

```
EphemerisService.kt
├── createRealtimeTrackingData()     # 라인 1775-1830
│   ├── timeDifferenceMs 계산        # 라인 1776
│   ├── theoreticalIndex 계산        # 라인 1779 ❌ 정수 변환
│   └── theoreticalPoint 선택        # 라인 1789 ❌ 보간 없음
│
├── calculateInterpolatedPosition()  # 라인 2027-2145 (미사용!)
│   ├── 소수점 인덱스 계산            # 라인 2035
│   ├── 선형 보간 적용               # 라인 2048-2051
│   └── interpolateValue()           # 라인 2143
│
└── interpolateValue()               # 라인 2143 (헬퍼)
    └── lower + (upper - lower) * fraction
```

### 문제점 상세

```kotlin
// 현재 코드 (EphemerisService.kt:1779)
val theoreticalIndex = if (originalPassDetails.isNotEmpty()) {
    (timeDifferenceMs / 100.0).toInt()  // ❌ .toInt()로 정밀도 손실
        .coerceIn(0, originalPassDetails.size - 1)
} else { ... }

// 예시:
// timeDifferenceMs = 15073ms
// theoreticalIndex = 150 (150.73이어야 함)
// 손실: 0.73 * 100ms = 73ms
// 각도 오차 (1°/s): 0.073°
```

## 2. 개선된 아키텍처

### 선형 보간 적용 흐름

```
[실측치 시간] → [소수점 인덱스 계산] → [하한/상한 포인트]
                    ↓                        ↓
               [fraction]              [선형 보간]
                    ↓                        ↓
             [메타데이터 저장]         [보간된 이론치]
                    ↓                        ↓
              [tracking_result]        [오차 계산]
```

### 핵심 코드 변경

```kotlin
// 개선된 createRealtimeTrackingData()
fun createRealtimeTrackingData(
    mstId: Long,
    detailId: Int,
    currentTime: ZonedDateTime,
    startTime: ZonedDateTime
): Map<String, Any?> {
    val timeDifferenceMs = Duration.between(startTime, currentTime).toMillis()

    // ✅ 소수점 인덱스 유지
    val exactIndex = timeDifferenceMs / 100.0

    // ✅ 하한/상한 인덱스
    val lowerIndex = exactIndex.toInt()
        .coerceIn(0, originalPassDetails.size - 1)
    val upperIndex = (lowerIndex + 1)
        .coerceIn(0, originalPassDetails.size - 1)

    // ✅ 보간 비율 (0.0 ~ 1.0)
    val fraction = exactIndex - lowerIndex

    // ✅ 두 포인트 가져오기
    val lowerPoint = originalPassDetails[lowerIndex]
    val upperPoint = originalPassDetails[upperIndex]

    // ✅ 선형 보간
    val interpolatedAzimuth = interpolateValue(
        lowerPoint["Azimuth"] as Double,
        upperPoint["Azimuth"] as Double,
        fraction
    )
    val interpolatedElevation = interpolateValue(
        lowerPoint["Elevation"] as Double,
        upperPoint["Elevation"] as Double,
        fraction
    )

    // ✅ 보간 메타데이터 포함
    return mapOf(
        "originalAzimuth" to interpolatedAzimuth.toFloat(),
        "originalElevation" to interpolatedElevation.toFloat(),
        // ...

        // ✅ 신규 메타데이터
        "theoreticalTimestamp" to lowerPoint["Time"],
        "timeOffsetMs" to (fraction * 100.0),  // ms 단위 오프셋
        "interpolationFraction" to fraction,
        "lowerTheoreticalIndex" to lowerIndex,
        "upperTheoreticalIndex" to upperIndex,
        "interpolationAccuracy" to (1.0 - fraction)  // 0.5가 최저
    )
}
```

## 3. 시간 기반 이진 검색 (고급)

100ms 간격이 정확하지 않을 경우 사용:

```kotlin
/**
 * 시간 기반으로 정확한 인접 포인트 찾기
 */
fun findTimeBasedRange(
    passDetails: List<Map<String, Any?>>,
    targetTime: ZonedDateTime
): Pair<Int, Int> {
    var low = 0
    var high = passDetails.size - 1

    while (low < high - 1) {
        val mid = (low + high) / 2
        val midTime = passDetails[mid]["Time"] as ZonedDateTime

        if (midTime.isBefore(targetTime) || midTime == targetTime) {
            low = mid
        } else {
            high = mid
        }
    }

    return Pair(low, high)
}

/**
 * 실제 시간 기반 보간
 */
fun interpolateByTime(
    passDetails: List<Map<String, Any?>>,
    targetTime: ZonedDateTime
): InterpolatedResult {
    val (lowerIdx, upperIdx) = findTimeBasedRange(passDetails, targetTime)

    val lowerPoint = passDetails[lowerIdx]
    val upperPoint = passDetails[upperIdx]

    val lowerTime = lowerPoint["Time"] as ZonedDateTime
    val upperTime = upperPoint["Time"] as ZonedDateTime

    // 실제 시간 차이 기반 fraction
    val totalMs = Duration.between(lowerTime, upperTime).toMillis()
    val offsetMs = Duration.between(lowerTime, targetTime).toMillis()
    val fraction = if (totalMs > 0) offsetMs.toDouble() / totalMs else 0.0

    return InterpolatedResult(
        azimuth = interpolateValue(
            lowerPoint["Azimuth"] as Double,
            upperPoint["Azimuth"] as Double,
            fraction
        ),
        elevation = interpolateValue(
            lowerPoint["Elevation"] as Double,
            upperPoint["Elevation"] as Double,
            fraction
        ),
        theoreticalTimestamp = lowerTime.plusNanos((offsetMs * 1_000_000).toLong()),
        timeOffsetMs = offsetMs.toDouble() * fraction,
        fraction = fraction,
        lowerIndex = lowerIdx,
        upperIndex = upperIdx
    )
}

data class InterpolatedResult(
    val azimuth: Double,
    val elevation: Double,
    val theoreticalTimestamp: ZonedDateTime,
    val timeOffsetMs: Double,
    val fraction: Double,
    val lowerIndex: Int,
    val upperIndex: Int
)
```

## 4. 칼만 필터 (향후 확장)

### 개념

```
예측 단계:
  x̂ₖ⁻ = Fₖ * x̂ₖ₋₁  (이전 상태로 현재 예측)

업데이트 단계:
  Kₖ = 칼만 이득 (예측 vs 측정 신뢰도)
  x̂ₖ = x̂ₖ⁻ + Kₖ * (zₖ - H * x̂ₖ⁻)  (측정값으로 보정)

결과:
  - 노이즈 감소
  - 예측-측정 융합
  - 실시간 적응
```

### 구현 스케치

```kotlin
class SimpleKalmanFilter(
    private var estimate: Double = 0.0,
    private var errorEstimate: Double = 1.0,
    private val processNoise: Double = 0.01,  // 시스템 노이즈
    private val measurementNoise: Double = 0.1  // 측정 노이즈
) {
    fun update(measurement: Double): Pair<Double, Double> {
        // 예측 단계
        val prediction = estimate
        val predictionError = errorEstimate + processNoise

        // 칼만 이득 계산
        val gain = predictionError / (predictionError + measurementNoise)

        // 업데이트 단계
        estimate = prediction + gain * (measurement - prediction)
        errorEstimate = (1 - gain) * predictionError

        return Pair(estimate, gain)
    }
}

// 사용 예시
val azimuthFilter = SimpleKalmanFilter()
val elevationFilter = SimpleKalmanFilter()

fun applyKalmanCorrection(
    theoretical: InterpolatedResult,
    actual: ActualPosition
): KalmanCorrectedResult {
    val (correctedAz, azGain) = azimuthFilter.update(actual.azimuth)
    val (correctedEl, elGain) = elevationFilter.update(actual.elevation)

    return KalmanCorrectedResult(
        azimuth = correctedAz,
        elevation = correctedEl,
        gain = (azGain + elGain) / 2
    )
}
```

## 5. Entity 변경

### TrackingResultEntity.kt 추가 필드

```kotlin
// 보간 상세
@Column("theoretical_timestamp")
val theoreticalTimestamp: OffsetDateTime? = null,

@Column("time_offset_ms")
val timeOffsetMs: Double? = null,

@Column("interpolation_fraction")
val interpolationFraction: Double? = null,

@Column("lower_theoretical_index")
val lowerTheoreticalIndex: Int? = null,

@Column("upper_theoretical_index")
val upperTheoreticalIndex: Int? = null,

// 칼만 필터 (향후)
@Column("kalman_azimuth")
val kalmanAzimuth: Double? = null,

@Column("kalman_elevation")
val kalmanElevation: Double? = null,

@Column("kalman_gain")
val kalmanGain: Double? = null,
```

### TrackingTrajectoryEntity.kt 추가 필드

```kotlin
@Column("resolution_ms")
val resolutionMs: Int = 1000,

@Column("satellite_range")
val satelliteRange: Double? = null,

@Column("satellite_altitude")
val satelliteAltitude: Double? = null,
```

## 6. 정밀도 예상치

| 각속도 | 현재 오차 | 선형 보간 후 | 시간 기반 + 칼만 |
|--------|----------|-------------|-----------------|
| 0.5°/s | ~0.025° | ~0.001° | ~0.0005° |
| 1.0°/s | ~0.050° | ~0.002° | ~0.001° |
| 2.0°/s | ~0.100° | ~0.004° | ~0.002° |

**참고**: 0.001° = 약 17m (위성 고도 1000km 기준)

## 7. 테스트 계획

```kotlin
// 단위 테스트
@Test
fun `interpolateValue should calculate correct linear interpolation`() {
    val result = interpolateValue(10.0, 20.0, 0.3)
    assertEquals(13.0, result, 0.001)
}

@Test
fun `findTimeBasedRange should find correct indices`() {
    val passDetails = listOf(
        mapOf("Time" to parse("10:00:00.000")),
        mapOf("Time" to parse("10:00:00.100")),
        mapOf("Time" to parse("10:00:00.200"))
    )
    val (low, high) = findTimeBasedRange(passDetails, parse("10:00:00.150"))
    assertEquals(1, low)
    assertEquals(2, high)
}

// 통합 테스트
@Test
fun `createRealtimeTrackingData should include interpolation metadata`() {
    val result = createRealtimeTrackingData(mstId, detailId, now, startTime)
    assertNotNull(result["interpolationFraction"])
    assertNotNull(result["lowerTheoreticalIndex"])
}
```

## 8. 롤백 계획

```sql
-- V006 롤백 (필요시)
ALTER TABLE tracking_result
    DROP COLUMN IF EXISTS theoretical_timestamp,
    DROP COLUMN IF EXISTS time_offset_ms,
    DROP COLUMN IF EXISTS interpolation_fraction,
    DROP COLUMN IF EXISTS lower_theoretical_index,
    DROP COLUMN IF EXISTS upper_theoretical_index,
    DROP COLUMN IF EXISTS kalman_azimuth,
    DROP COLUMN IF EXISTS kalman_elevation,
    DROP COLUMN IF EXISTS kalman_gain;

ALTER TABLE tracking_trajectory
    DROP COLUMN IF EXISTS resolution_ms,
    DROP COLUMN IF EXISTS satellite_range,
    DROP COLUMN IF EXISTS satellite_altitude;

DROP INDEX IF EXISTS idx_tr_theoretical_ts;
DROP INDEX IF EXISTS idx_tt_resolution;
```