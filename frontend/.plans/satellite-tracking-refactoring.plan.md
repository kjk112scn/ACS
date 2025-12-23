# 위성 추적 아키텍처 리팩토링 계획

## 🎯 목표

1. **OrekitCalculator 단순화**: 순수 2축 각도만 계산 (메타데이터 제거)
2. **Processor 도입**: 모든 변환 및 메타데이터 계산 중앙화
3. **EphemerisService 간소화**: Processor 활용하여 비즈니스 로직만 관리
4. **MstId 기반 구조 유지**: DataType별 저장 및 조회 구조 보존

## 📁 프로젝트 구조 변경

### 신규 패키지 및 파일

```
E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\
└── algorithm/
    └── satellitetracker/
        ├── impl/
        │   └── OrekitCalculator.kt                   # 🔄 수정 (단순화)
        ├── model/
        │   └── SatelliteTrackData.kt                 # 기존
        └── processor/                                # ✅ 신규 패키지
            ├── SatelliteTrackingProcessor.kt         # ✅ 신규 파일
            └── model/                                # ✅ 신규 패키지
                ├── ProcessedTrackingData.kt          # ✅ 신규 파일
                ├── DataTypeMetrics.kt                # ✅ 신규 파일
                └── KeyholeAnalysis.kt                # ✅ 신규 파일
```

### 수정할 기존 파일

```
algorithm/satellitetracker/impl/
└── OrekitCalculator.kt                               # 🔄 수정 (단순화)

service/mode/
└── EphemerisService.kt                               # 🔄 수정 (Processor 사용)
```

**폴더 구조 설계 원칙**:

- `satellitetracker/processor`: 위성 추적 전용 프로세서 (satellitetracker 패키지 내부)
- `satellitetracker/model`: 기본 데이터 모델 (SatelliteTrackData)
- `satellitetracker/processor/model`: 프로세서 전용 데이터 모델 (ProcessedTrackingData, DataTypeMetrics 등)

## 🚀 Phase 1: OrekitCalculator 개선

### 파일: `OrekitCalculator.kt`

#### 1.1 ElevationDetector 도입 (신규 함수)

**함수**: `detectVisibilityPeriods` (private)

```kotlin
/**
 * ElevationDetector를 사용한 가시성 기간 감지
 *
 * @param tleLine1 TLE 첫 번째 라인
 * @param tleLine2 TLE 두 번째 라인
 * @param startDate 시작 날짜
 * @param durationDays 기간 (일)
 * @param minElevation 최소 고도각 (도)
 * @param latitude 지상국 위도
 * @param longitude 지상국 경도
 * @param altitude 지상국 고도
 * @return 가시성 기간 목록
 */
private fun detectVisibilityPeriods(
    tleLine1: String,
    tleLine2: String,
    startDate: ZonedDateTime,
    durationDays: Int,
    minElevation: Float,
    latitude: Double,
    longitude: Double,
    altitude: Double
): List<VisibilityPeriod> {
    logger.info("🔍 ElevationDetector로 가시성 기간 감지")

    val visibilityPeriods = mutableListOf<VisibilityPeriod>()
    val tle = TLE(tleLine1, tleLine2)
    val propagator = TLEPropagator.selectExtrapolator(tle)

    // 지상국 설정
    val stationPosition = GeodeticPoint(
        FastMath.toRadians(latitude),
        FastMath.toRadians(longitude),
        altitude
    )
    val stationFrame = TopocentricFrame(earthModel, stationPosition, "GroundStation")

    // ✅ ElevationDetector 설정 (사용자 예제 기반)
    val elevationDetector = ElevationDetector(60.0, 1.0e-3, stationFrame)
        .withConstantElevation(FastMath.toRadians(minElevation.toDouble()))
        .withHandler(object : EventHandler<ElevationDetector> {
            override fun eventOccurred(
                s: SpacecraftState,
                detector: ElevationDetector,
                increasing: Boolean
            ): Action {
                val date = s.date
                val time = toZonedDateTime(date)

                if (increasing) {
                    logger.debug("📡 AOS (위성 상승 시작): $time")
                    visibilityPeriods.add(VisibilityPeriod(time, null))
                } else {
                    logger.debug("📡 LOS (위성 가시 종료): $time")
                    if (visibilityPeriods.isNotEmpty()) {
                        val lastPeriod = visibilityPeriods.last()
                        if (lastPeriod.endTime == null) {
                            visibilityPeriods[visibilityPeriods.size - 1] =
                                lastPeriod.copy(endTime = time)
                        }
                    }
                }
                return Action.CONTINUE
            }
        })

    propagator.addEventDetector(elevationDetector)

    // 시간 범위 propagate
    val startAbsoluteDate = toAbsoluteDate(startDate)
    val endAbsoluteDate = toAbsoluteDate(startDate.plusDays(durationDays.toLong()))

    try {
        propagator.propagate(startAbsoluteDate, endAbsoluteDate)
    } catch (e: Exception) {
        logger.warn("Propagation 중 예외 발생: ${e.message}")
    }

    // 마지막 가시성 기간 처리
    if (visibilityPeriods.isNotEmpty()) {
        val lastPeriod = visibilityPeriods.last()
        if (lastPeriod.endTime == null) {
            visibilityPeriods[visibilityPeriods.size - 1] =
                lastPeriod.copy(endTime = startDate.plusDays(durationDays.toLong()))
        }
    }

    logger.info("✅ ${visibilityPeriods.size}개 가시성 기간 감지 완료")
    return visibilityPeriods
}
```

#### 1.2 generateDetailedTrackingData 유지 (기존 함수)

**현재 함수 유지** - 순수 2축 각도만 생성하도록 확인

#### 1.3 generateSatelliteTrackingSchedule 개선

**수정 내용**:

1. `calculateVisibilityPeriodsWithMaxElevation` 호출 제거
2. `detectVisibilityPeriods` 호출 추가
3. `SatelliteTrackingPass` 생성 시 메타데이터 제거

```kotlin
fun generateSatelliteTrackingSchedule(
    tleLine1: String,
    tleLine2: String,
    startDate: ZonedDateTime,
    durationDays: Int = 1,
    minElevation: Float = 0.0f,
    latitude: Double,
    longitude: Double,
    altitude: Double = 0.0,
    trackingIntervalMs: Int = 100
): SatelliteTrackingSchedule {
    logger.info("🚀 위성 추적 스케줄 생성 시작 (ElevationDetector 사용)")

    // 1️⃣ ElevationDetector로 가시성 기간 감지
    val visibilityPeriods = detectVisibilityPeriods(
        tleLine1, tleLine2, startDate, durationDays,
        minElevation, latitude, longitude, altitude
    )

    logger.info("✅ ${visibilityPeriods.size}개 가시성 기간 감지 완료")

    // 2️⃣ 각 가시성 기간에 대해 상세 데이터 생성
    val trackingPasses = visibilityPeriods.mapIndexed { index, period ->
        logger.debug("패스 ${index + 1}/${visibilityPeriods.size} 상세 데이터 생성")

        val detailedData = generateDetailedTrackingData(
            tleLine1, tleLine2,
            period.startTime!!, period.endTime!!,
            trackingIntervalMs, latitude, longitude, altitude, minElevation
        )

        logger.debug("패스 ${index + 1} 데이터 생성 완료: ${detailedData.size}개 포인트")

        SatelliteTrackingPass(
            startTime = period.startTime,
            endTime = period.endTime,
            trackingData = detailedData
            // ✅ 메타데이터 없음! Processor에서 계산
        )
    }

    logger.info("✅ ${trackingPasses.size}개 패스 생성 완료")

    return SatelliteTrackingSchedule(
        satelliteTle1 = tleLine1,
        satelliteTle2 = tleLine2,
        startDate = startDate,
        endDate = startDate.plusDays(durationDays.toLong()),
        stationLatitude = latitude,
        stationLongitude = longitude,
        stationAltitude = altitude,
        minElevation = minElevation,
        trackingIntervalMs = trackingIntervalMs,
        trackingPasses = trackingPasses
    )
}
```

#### 1.4 SatelliteTrackingPass 단순화

**수정 내용**: 메타데이터 필드 제거

```kotlin
data class SatelliteTrackingPass(
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val trackingData: List<SatelliteTrackData>
    // ✅ maxElevation, maxAzimuthRate 등 메타데이터 제거!
) {
    val duration: Duration = Duration.between(startTime, endTime)
    val dataPointCount: Int = trackingData.size
}
```

#### 1.5 VisibilityPeriod 단순화

**신규 데이터 클래스**:

```kotlin
/**
 * 가시성 기간 정보 (간단한 데이터 클래스)
 */
data class VisibilityPeriod(
    val startTime: ZonedDateTime?,
    val endTime: ZonedDateTime?
)
```

#### 1.6 제거할 함수/클래스

- ❌ `calculateVisibilityPeriodsWithMaxElevation` 함수 전체 제거
- ❌ 기존 `VisibilityPeriod` 데이터 클래스 제거 (새로운 단순 버전으로 대체)

#### 1.7 Helper 함수 추가

```kotlin
/**
 * Orekit의 AbsoluteDate를 ZonedDateTime으로 변환
 */
private fun toZonedDateTime(absoluteDate: AbsoluteDate): ZonedDateTime {
    val components = absoluteDate.getComponents(utcTimeScale)
    return ZonedDateTime.of(
        components.date.year,
        components.date.month,
        components.date.day,
        components.time.hour,
        components.time.minute,
        components.time.second,
        (components.time.secondsInUTCDay % 1 * 1e9).toInt(),
        ZoneOffset.UTC
    )
}
```

## 🔄 Phase 2: SatelliteTrackingProcessor 생성

### 2.1 신규 패키지 및 파일 생성

#### 파일: `algorithm/satellitetracker/processor/SatelliteTrackingProcessor.kt`

```kotlin
package com.gtlsystems.acs_api.algorithm.satellitetracker.processor

import com.gtlsystems.acs_api.algorithm.axistransformation.CoordinateTransformer
import com.gtlsystems.acs_api.algorithm.axislimitangle.LimitAngleCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.processor.model.*
import com.gtlsystems.acs_api.service.system.settings.SettingsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.abs

/**
 * 위성 추적 데이터 처리기
 *
 * OrekitCalculator가 생성한 순수 2축 데이터를 받아
 * 모든 좌표 변환 및 메타데이터 계산을 수행합니다.
 *
 * @property coordinateTransformer 3축 좌표 변환기
 * @property limitAngleCalculator 각도 제한 계산기 (±270°)
 * @property settingsService 설정 서비스
 */
@Service
class SatelliteTrackingProcessor(
    private val coordinateTransformer: CoordinateTransformer,
    private val limitAngleCalculator: LimitAngleCalculator,
    private val settingsService: SettingsService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * OrekitCalculator의 순수 2축 데이터를 받아 모든 변환 및 분석 수행
     *
     * @param schedule OrekitCalculator가 생성한 위성 추적 스케줄
     * @param satelliteName 위성 이름 (선택)
     * @return 모든 DataType의 Mst/Dtl 데이터
     *
     * ✅ MstId 기반 연결 구조 유지
     * ✅ DataType별 저장 (original, axis_transformed, final_transformed)
     */
    fun processFullTransformation(
        schedule: OrekitCalculator.SatelliteTrackingSchedule,
        satelliteName: String? = null
    ): ProcessedTrackingData {
        logger.info("🔄 위성 추적 데이터 변환 및 분석 시작")

        val satelliteId = schedule.satelliteTle1.substring(2, 7).trim()
        val actualSatelliteName = satelliteName ?: satelliteId

        // 1️⃣ Original (2축) 데이터 구조화
        val (originalMst, originalDtl) = structureOriginalData(
            schedule,
            satelliteId,
            actualSatelliteName
        )
        logger.info("✅ Original 데이터 구조화 완료: ${originalMst.size}개 마스터, ${originalDtl.size}개 상세")

        // 2️⃣ 3축 변환
        val (axisTransformedMst, axisTransformedDtl) = applyAxisTransformation(
            originalMst,
            originalDtl
        )
        logger.info("✅ 3축 변환 완료: ${axisTransformedMst.size}개 마스터, ${axisTransformedDtl.size}개 상세")

        // 3️⃣ ±270° 변환
        val (finalTransformedMst, finalTransformedDtl) = applyAngleLimitTransformation(
            axisTransformedMst,
            axisTransformedDtl
        )
        logger.info("✅ 각도제한 변환 완료: ${finalTransformedMst.size}개 마스터, ${finalTransformedDtl.size}개 상세")

        logger.info("🎉 변환 및 분석 완료")

        return ProcessedTrackingData(
            originalMst = originalMst,
            originalDtl = originalDtl,
            axisTransformedMst = axisTransformedMst,
            axisTransformedDtl = axisTransformedDtl,
            finalTransformedMst = finalTransformedMst,
            finalTransformedDtl = finalTransformedDtl
        )
    }

    /**
     * Original 데이터 구조화 (순수 2축 → Mst/Dtl 구조)
     *
     * ✅ MstId로 연결 (1개 MstId에 모든 DataType 연결)
     * ✅ 메타데이터는 상세 데이터에서 계산
     */
    private fun structureOriginalData(
        schedule: OrekitCalculator.SatelliteTrackingSchedule,
        satelliteId: String,
        satelliteName: String
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {

        val originalMst = mutableListOf<Map<String, Any?>>()
        val originalDtl = mutableListOf<Map<String, Any?>>()

        schedule.trackingPasses.forEachIndexed { index, pass ->
            val mstId = (index + 1).toUInt()  // ✅ MstId (1, 2, 3, ...)

            // ✅ 상세 데이터 먼저 생성 (MstId로 연결)
            pass.trackingData.forEachIndexed { dtlIndex, data ->
                originalDtl.add(
                    mapOf(
                        "No" to (dtlIndex + 1).toUInt(),
                        "MstId" to mstId,  // ← 마스터와 연결!
                        "Time" to data.timestamp,
                        "Azimuth" to data.azimuth,
                        "Elevation" to data.elevation,
                        "Range" to data.range,
                        "Altitude" to data.altitude,
                        "DataType" to "original"
                    )
                )
            }

            // ✅ 상세 데이터에서 메타데이터 계산
            val passDtl = originalDtl.filter { it["MstId"] == mstId }
            val metrics = calculateMetrics(passDtl)
            val keyholeAnalysis = analyzeKeyhole(metrics, passDtl)

            // ✅ 마스터 데이터 생성
            originalMst.add(
                mapOf(
                    "No" to mstId,  // ← 마스터 ID (DataType 구분)
                    "SatelliteID" to satelliteId,
                    "SatelliteName" to satelliteName,
                    "StartTime" to metrics.startTime,
                    "EndTime" to metrics.endTime,
                    "Duration" to Duration.between(metrics.startTime, metrics.endTime).toString(),
                    "MaxElevation" to metrics.maxElevation,
                    "MaxElevationTime" to metrics.maxElevationTime,
                    "MaxAzimuth" to metrics.maxAzimuth,
                    "MaxAzRate" to metrics.maxAzimuthRate,
                    "MaxElRate" to metrics.maxElevationRate,
                    "MaxAzAccel" to metrics.maxAzimuthAccel,
                    "MaxElAccel" to metrics.maxElevationAccel,
                    "IsKeyhole" to keyholeAnalysis.isKeyhole,
                    "RecommendedTrainAngle" to keyholeAnalysis.recommendedTrainAngle,
                    "CreationDate" to ZonedDateTime.now(),
                    "Creator" to "System",
                    "DataType" to "original"  // ← DataType으로 구분
                )
            )
        }

        return Pair(originalMst, originalDtl)
    }

    /**
     * 3축 변환 적용
     *
     * ✅ MstId 기반 연결 유지
     * ✅ 변환 후 메타데이터 재계산
     */
    private fun applyAxisTransformation(
        originalMst: List<Map<String, Any?>>,
        originalDtl: List<Map<String, Any?>>
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {

        val axisTransformedMst = mutableListOf<Map<String, Any?>>()
        val axisTransformedDtl = mutableListOf<Map<String, Any?>>()

        originalMst.forEach { mstData ->
            val mstId = mstData["No"] as UInt
            val recommendedTrainAngle = mstData["RecommendedTrainAngle"] as? Double ?: 0.0

            logger.debug("패스 #$mstId 3축 변환 중 (Train: ${recommendedTrainAngle}°)")

            // 해당 패스의 상세 데이터 조회 (MstId로 필터링!)
            val passDtl = originalDtl.filter { it["MstId"] == mstId }

            // 각 좌표에 3축 변환 적용
            passDtl.forEachIndexed { index, point ->
                val originalAz = point["Azimuth"] as Double
                val originalEl = point["Elevation"] as Double
                val time = point["Time"] as ZonedDateTime

                val transformed = coordinateTransformer.transform(
                    azimuth = originalAz,
                    elevation = originalEl,
                    trainAngle = recommendedTrainAngle
                )

                axisTransformedDtl.add(
                    mapOf(
                        "No" to (index + 1).toUInt(),
                        "MstId" to mstId,  // ← 마스터와 연결 유지!
                        "Time" to time,
                        "Azimuth" to transformed.azimuth,
                        "Elevation" to transformed.elevation,
                        "Train" to transformed.train,
                        "DataType" to "axis_transformed"
                    )
                )
            }

            // ✅ 변환된 데이터에서 메타데이터 재계산
            val transformedPassDtl = axisTransformedDtl.filter { it["MstId"] == mstId }
            val metrics = calculateMetrics(transformedPassDtl)

            // ✅ 마스터 데이터 생성 (원본에서 일부 복사)
            axisTransformedMst.add(
                mstData.toMutableMap().apply {
                    put("MaxElevation", metrics.maxElevation)
                    put("MaxElevationTime", metrics.maxElevationTime)
                    put("MaxAzimuth", metrics.maxAzimuth)
                    put("MaxAzRate", metrics.maxAzimuthRate)
                    put("MaxElRate", metrics.maxElevationRate)
                    put("MaxAzAccel", metrics.maxAzimuthAccel)
                    put("MaxElAccel", metrics.maxElevationAccel)
                    put("DataType", "axis_transformed")
                }
            )
        }

        return Pair(axisTransformedMst, axisTransformedDtl)
    }

    /**
     * ±270° 변환 적용
     *
     * ✅ MstId 기반 연결 유지
     * ✅ 변환 후 메타데이터 재계산
     */
    private fun applyAngleLimitTransformation(
        axisTransformedMst: List<Map<String, Any?>>,
        axisTransformedDtl: List<Map<String, Any?>>
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {

        val finalTransformedMst = mutableListOf<Map<String, Any?>>()
        val finalTransformedDtl = mutableListOf<Map<String, Any?>>()

        axisTransformedMst.forEach { mstData ->
            val mstId = mstData["No"] as UInt

            logger.debug("패스 #$mstId 각도제한 변환 중 (±270°)")

            // 해당 패스의 상세 데이터 조회 (MstId로 필터링!)
            val passDtl = axisTransformedDtl.filter { it["MstId"] == mstId }

            // ±270° 변환 적용
            val limitedAngles = limitAngleCalculator.calculateLimitedAngles(
                passDtl.map {
                    mapOf(
                        "Azimuth" to (it["Azimuth"] as Double),
                        "Train" to (it["Train"] as Double)
                    )
                }
            )

            passDtl.forEachIndexed { index, point ->
                val limited = limitedAngles[index]

                finalTransformedDtl.add(
                    mapOf(
                        "No" to (index + 1).toUInt(),
                        "MstId" to mstId,  // ← 마스터와 연결 유지!
                        "Time" to point["Time"],
                        "Azimuth" to limited["Azimuth"],
                        "Elevation" to point["Elevation"],
                        "Train" to limited["Train"],
                        "DataType" to "final_transformed"
                    )
                )
            }

            // ✅ 변환된 데이터에서 메타데이터 재계산
            val finalPassDtl = finalTransformedDtl.filter { it["MstId"] == mstId }
            val metrics = calculateMetrics(finalPassDtl)

            // ✅ 마스터 데이터 생성
            finalTransformedMst.add(
                mstData.toMutableMap().apply {
                    put("MaxElevation", metrics.maxElevation)
                    put("MaxElevationTime", metrics.maxElevationTime)
                    put("MaxAzimuth", metrics.maxAzimuth)
                    put("MaxAzRate", metrics.maxAzimuthRate)
                    put("MaxElRate", metrics.maxElevationRate)
                    put("MaxAzAccel", metrics.maxAzimuthAccel)
                    put("MaxElAccel", metrics.maxElevationAccel)
                    put("DataType", "final_transformed")
                }
            )
        }

        return Pair(finalTransformedMst, finalTransformedDtl)
    }

    /**
     * 단일 DataType의 모든 메타데이터 계산
     *
     * ✅ StartTime, EndTime, MaxElevation, MaxElevationTime,
     *    MaxAzRate, MaxElRate, MaxAzAccel, MaxElAccel 모두 계산!
     *
     * @param data 상세 데이터 (MstId로 필터링된 단일 패스)
     * @return 계산된 메타데이터
     */
    private fun calculateMetrics(data: List<Map<String, Any?>>): DataTypeMetrics {
        if (data.isEmpty()) return DataTypeMetrics.empty()

        val startTime = data.first()["Time"] as ZonedDateTime
        val endTime = data.last()["Time"] as ZonedDateTime

        var maxElevation = -90.0
        var maxElevationTime: ZonedDateTime? = null
        var maxElevationAzimuth = 0.0
        var maxAzimuthRate = 0.0
        var maxElevationRate = 0.0
        var maxAzimuthAccel = 0.0
        var maxElevationAccel = 0.0

        var prevAzimuth: Double? = null
        var prevElevation: Double? = null
        var prevTime: ZonedDateTime? = null
        var prevAzRate: Double? = null
        var prevElRate: Double? = null

        // ✅ 단일 순회로 모든 메타데이터 계산
        data.forEach { point ->
            val azimuth = point["Azimuth"] as Double
            val elevation = point["Elevation"] as Double
            val time = point["Time"] as ZonedDateTime

            // MaxElevation 계산
            if (elevation > maxElevation) {
                maxElevation = elevation
                maxElevationTime = time
                maxElevationAzimuth = azimuth
            }

            // 속도 계산
            if (prevAzimuth != null && prevElevation != null && prevTime != null) {
                val timeDiff = Duration.between(prevTime, time).toMillis() / 1000.0

                if (timeDiff > 0) {
                    var azDiff = azimuth - prevAzimuth
                    if (azDiff > 180) azDiff -= 360
                    if (azDiff < -180) azDiff += 360

                    val azRate = azDiff / timeDiff
                    val elRate = (elevation - prevElevation) / timeDiff

                    maxAzimuthRate = maxOf(maxAzimuthRate, abs(azRate))
                    maxElevationRate = maxOf(maxElevationRate, abs(elRate))

                    // 가속도 계산
                    if (prevAzRate != null && prevElRate != null) {
                        val azAccel = (azRate - prevAzRate) / timeDiff
                        val elAccel = (elRate - prevElRate) / timeDiff

                        maxAzimuthAccel = maxOf(maxAzimuthAccel, abs(azAccel))
                        maxElevationAccel = maxOf(maxElevationAccel, abs(elAccel))
                    }

                    prevAzRate = azRate
                    prevElRate = elRate
                }
            }

            prevAzimuth = azimuth
            prevElevation = elevation
            prevTime = time
        }

        return DataTypeMetrics(
            startTime = startTime,
            endTime = endTime,
            maxElevation = maxElevation,
            maxElevationTime = maxElevationTime,
            maxAzimuth = maxElevationAzimuth,
            maxAzimuthRate = maxAzimuthRate,
            maxElevationRate = maxElevationRate,
            maxAzimuthAccel = maxAzimuthAccel,
            maxElevationAccel = maxElevationAccel
        )
    }

    /**
     * KEYHOLE 분석 + TrainAngle 계산
     *
     * @param metrics 계산된 메타데이터
     * @param originalData Original 상세 데이터
     * @return KEYHOLE 분석 결과
     */
    private fun analyzeKeyhole(
        metrics: DataTypeMetrics,
        originalData: List<Map<String, Any?>>
    ): KeyholeAnalysis {
        val threshold = settingsService.keyholeAzimuthVelocityThreshold
        val isKeyhole = metrics.maxAzimuthRate >= threshold

        val recommendedTrainAngle = if (isKeyhole && metrics.maxElevationTime != null) {
            originalData
                .filter { it["Time"] == metrics.maxElevationTime }
                .firstOrNull()
                ?.get("Azimuth") as? Double ?: 0.0
        } else {
            0.0
        }

        return KeyholeAnalysis(
            isKeyhole = isKeyhole,
            recommendedTrainAngle = recommendedTrainAngle
        )
    }
}
```

### 2.2 Processor 데이터 모델 생성

#### 파일: `algorithm/satellitetracker/processor/model/ProcessedTrackingData.kt`

```kotlin
package com.gtlsystems.acs_api.algorithm.satellitetracker.processor.model

/**
 * 처리된 위성 추적 데이터
 *
 * 모든 DataType의 마스터/상세 데이터를 포함합니다.
 */
data class ProcessedTrackingData(
    val originalMst: List<Map<String, Any?>>,
    val originalDtl: List<Map<String, Any?>>,
    val axisTransformedMst: List<Map<String, Any?>>,
    val axisTransformedDtl: List<Map<String, Any?>>,
    val finalTransformedMst: List<Map<String, Any?>>,
    val finalTransformedDtl: List<Map<String, Any?>>
)
```

#### 파일: `algorithm/satellitetracker/processor/model/DataTypeMetrics.kt`

```kotlin
package com.gtlsystems.acs_api.algorithm.satellitetracker.processor.model

import java.time.ZonedDateTime

/**
 * DataType별 메타데이터
 *
 * 단일 패스의 모든 메타데이터를 포함합니다.
 */
data class DataTypeMetrics(
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val maxElevation: Double,
    val maxElevationTime: ZonedDateTime?,
    val maxAzimuth: Double,
    val maxAzimuthRate: Double,
    val maxElevationRate: Double,
    val maxAzimuthAccel: Double,
    val maxElevationAccel: Double
) {
    companion object {
        fun empty() = DataTypeMetrics(
            startTime = ZonedDateTime.now(),
            endTime = ZonedDateTime.now(),
            maxElevation = 0.0,
            maxElevationTime = null,
            maxAzimuth = 0.0,
            maxAzimuthRate = 0.0,
            maxElevationRate = 0.0,
            maxAzimuthAccel = 0.0,
            maxElevationAccel = 0.0
        )
    }
}
```

#### 파일: `algorithm/satellitetracker/processor/model/KeyholeAnalysis.kt`

```kotlin
package com.gtlsystems.acs_api.algorithm.satellitetracker.processor.model

/**
 * KEYHOLE 분석 결과
 */
data class KeyholeAnalysis(
    val isKeyhole: Boolean,
    val recommendedTrainAngle: Double
)
```

## 🔧 Phase 3: EphemerisService 리팩토링

### 파일: `service/mode/EphemerisService.kt`

#### 3.1 Processor 의존성 주입

**추가 필드**:

```kotlin
@Service
class EphemerisService(
    private val orekitCalculator: OrekitCalculator,
    private val satelliteTrackingProcessor: SatelliteTrackingProcessor,  // ✅ 추가
    private val acsEventBus: ACSEventBus,
    // ... 기존 필드들
)
```

#### 3.2 generateEphemerisDesignationTrackSync 간소화

**수정 내용**:

```kotlin
fun generateEphemerisDesignationTrackSync(
    tleLine1: String,
    tleLine2: String,
    satelliteName: String? = null
): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
    try {
        logger.info("🚀 위성 궤도 추적 시작")

        // 1️⃣ OrekitCalculator: 순수 2축 각도만 생성
        val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val sourceMinEl = settingsService.sourceMinElevationAngle.toFloat()

        logger.info("📡 OrekitCalculator 호출 중...")
        val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
            tleLine1 = tleLine1,
            tleLine2 = tleLine2,
            startDate = today.withZoneSameInstant(ZoneOffset.UTC),
            durationDays = 2,
            minElevation = sourceMinEl,
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            altitude = locationData.altitude
        )
        logger.info("✅ OrekitCalculator 완료: ${schedule.trackingPasses.size}개 패스")

        // 2️⃣ Processor: 모든 변환 및 메타데이터 계산
        logger.info("🔄 SatelliteTrackingProcessor 호출 중...")
        val processedData = satelliteTrackingProcessor.processFullTransformation(
            schedule,
            satelliteName
        )
        logger.info("✅ Processor 완료")

        // 3️⃣ ephemerisTrackMstStorage, ephemerisTrackDtlStorage에 저장
        // ✅ MstId 기반 연결 구조 유지
        logger.info("💾 저장소에 데이터 저장 중...")
        ephemerisTrackMstStorage.clear()
        ephemerisTrackDtlStorage.clear()

        // Original
        ephemerisTrackMstStorage.addAll(processedData.originalMst)
        ephemerisTrackDtlStorage.addAll(processedData.originalDtl)

        // AxisTransformed
        ephemerisTrackMstStorage.addAll(processedData.axisTransformedMst)
        ephemerisTrackDtlStorage.addAll(processedData.axisTransformedDtl)

        // FinalTransformed
        ephemerisTrackMstStorage.addAll(processedData.finalTransformedMst)
        ephemerisTrackDtlStorage.addAll(processedData.finalTransformedDtl)

        logger.info("✅ 저장 완료: ${ephemerisTrackMstStorage.size}개 마스터, ${ephemerisTrackDtlStorage.size}개 상세")

        // 4️⃣ UI에는 FinalTransformed 데이터 반환
        return Pair(processedData.finalTransformedMst, processedData.finalTransformedDtl)

    } catch (e: Exception) {
        logger.error("❌ 오류: ${e.message}", e)
        throw e
    }
}
```

#### 3.3 제거할 함수들

- ❌ `generateOriginalTrackingData()` → Processor로 이동
- ❌ `applyAxisTransformation()` → Processor로 이동
- ❌ `applyAngleLimitTransformation()` → Processor로 이동
- ❌ `analyzeKeyholeStatus()` → Processor로 이동

## 🧪 Phase 4: 테스트 및 검증

### 4.1 기본 동작 테스트

**테스트 시나리오**:

1. UI에서 위성 선택 후 "Generate Tracking Data" 버튼 클릭
2. 로그 확인:
   - OrekitCalculator 호출 로그
   - Processor 호출 로그
   - 저장소 저장 로그
3. UI에서 스케줄 테이블 확인
4. MaxElevation, MaxAzRate 값 확인

### 4.2 MstId 연결 구조 검증

**검증 코드** (임시 테스트 함수):

```kotlin
fun verifyMstIdStructure() {
    val mstId = 1u

    // Original 데이터 조회
    val originalMst = ephemerisTrackMstStorage
        .filter { it["No"] == mstId && it["DataType"] == "original" }
        .firstOrNull()

    val originalDtl = ephemerisTrackDtlStorage
        .filter { it["MstId"] == mstId && it["DataType"] == "original" }

    // FinalTransformed 데이터 조회
    val finalMst = ephemerisTrackMstStorage
        .filter { it["No"] == mstId && it["DataType"] == "final_transformed" }
        .firstOrNull()

    val finalDtl = ephemerisTrackDtlStorage
        .filter { it["MstId"] == mstId && it["DataType"] == "final_transformed" }

    // 로그 출력
    logger.info("🔍 MstId=$mstId 검증:")
    logger.info("  - Original Mst: ${originalMst != null}")
    logger.info("  - Original Dtl: ${originalDtl.size}개")
    logger.info("  - Final Mst: ${finalMst != null}")
    logger.info("  - Final Dtl: ${finalDtl.size}개")

    // MaxElevation 비교
    val originalMaxEl = originalMst?.get("MaxElevation") as? Double
    val finalMaxEl = finalMst?.get("MaxElevation") as? Double
    logger.info("  - Original MaxEl: $originalMaxEl")
    logger.info("  - Final MaxEl: $finalMaxEl")
}
```

### 4.3 DataType별 저장 검증

**검증 코드**:

```kotlin
fun verifyDataTypeSeparation() {
    val originalCount = ephemerisTrackMstStorage.count { it["DataType"] == "original" }
    val axisTransformedCount = ephemerisTrackMstStorage.count { it["DataType"] == "axis_transformed" }
    val finalTransformedCount = ephemerisTrackMstStorage.count { it["DataType"] == "final_transformed" }

    logger.info("📊 DataType별 개수:")
    logger.info("  - Original: $originalCount")
    logger.info("  - AxisTransformed: $axisTransformedCount")
    logger.info("  - FinalTransformed: $finalTransformedCount")

    if (originalCount == axisTransformedCount && axisTransformedCount == finalTransformedCount) {
        logger.info("✅ DataType별 개수 일치")
    } else {
        logger.error("❌ DataType별 개수 불일치!")
    }
}
```

### 4.4 MaxElevation 정확성 검증

**사용자 수동 검증**:

1. UI에서 표시되는 MaxElevation 값 확인
2. CSV 다운로드
3. CSV에서 `FinalTransformed_Elevation` 최대값 찾기
4. UI 값과 CSV 최대값 비교

## 📋 체크리스트

### Phase 1: OrekitCalculator

- [ ] `detectVisibilityPeriods` 함수 작성 (ElevationDetector 사용)
- [ ] `generateSatelliteTrackingSchedule` 수정
- [ ] `SatelliteTrackingPass` 단순화 (메타데이터 제거)
- [ ] `VisibilityPeriod` 단순화
- [ ] `toZonedDateTime` helper 함수 추가
- [ ] `calculateVisibilityPeriodsWithMaxElevation` 제거
- [ ] 컴파일 에러 확인

### Phase 2: Processor

- [ ] `algorithm/processor` 패키지 생성
- [ ] `algorithm/processor/model` 패키지 생성
- [ ] `SatelliteTrackingProcessor.kt` 작성
- [ ] `ProcessedTrackingData.kt` 작성
- [ ] `DataTypeMetrics.kt` 작성
- [ ] `KeyholeAnalysis.kt` 작성
- [ ] 컴파일 에러 확인

### Phase 3: EphemerisService

- [ ] `SatelliteTrackingProcessor` 의존성 주입
- [ ] `generateEphemerisDesignationTrackSync` 간소화
- [ ] 기존 함수들 제거 (generateOriginalTrackingData 등)
- [ ] 컴파일 에러 확인

### Phase 4: 테스트

- [ ] 기본 동작 테스트 (UI에서 스케줄 생성)
- [ ] MstId 연결 구조 검증
- [ ] DataType별 저장 검증
- [ ] MaxElevation 정확성 검증 (CSV 비교)
- [ ] 로그 확인 (각 단계별 로그 출력)

## 🚀 구현 순서

1. **Phase 1 - OrekitCalculator 개선** (가장 먼저)
2. **Phase 2 - Processor 생성** (OrekitCalculator 완료 후)
3. **Phase 3 - EphemerisService 리팩토링** (Processor 완료 후)
4. **Phase 4 - 테스트 및 검증** (모든 구현 완료 후)

## 📊 예상 효과

1. **코드 간소화**: OrekitCalculator는 2축 계산만, Processor는 변환만
2. **재사용성**: PassScheduleService에서도 동일한 Processor 사용 가능
3. **테스트 용이성**: 각 레이어별 독립 테스트 가능
4. **유지보수성**: 역할 분리로 버그 추적 용이
5. **정확성**: 각 DataType별로 메타데이터 재계산하여 정확도 향상

## 🔄 PassScheduleService 적용 (추후)

EphemerisService 완료 및 검증 후, 동일한 패턴으로 PassScheduleService 개선:

1. PassScheduleService에서 `SatelliteTrackingProcessor` 의존성 주입
2. `passScheduleMstStorage`, `passScheduleDtlStorage` 사용
3. 동일한 MstId 기반 구조 적용
