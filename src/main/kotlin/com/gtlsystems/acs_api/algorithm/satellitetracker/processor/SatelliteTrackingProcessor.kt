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
 * ✅ 계획 Phase 2: 모든 변환 및 메타데이터 계산 중앙화
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

            // Keyhole 분석
            val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
            val threshold = settingsService.keyholeAzimuthVelocityThreshold
            val isKeyhole = maxAzRate >= threshold
            
            // Keyhole인 경우 Train 각도 계산
            val recommendedTrainAngle = if (isKeyhole) {
                val maxElTime = metrics["MaxElevationTime"] as? ZonedDateTime
                maxElTime?.let { time ->
                    passDtl
                        .filter { it["Time"] != null }
                        .minByOrNull { dtl ->
                            val dtlTime = dtl["Time"] as ZonedDateTime
                            abs(Duration.between(dtlTime, time).toMillis())
                        }
                        ?.get("Azimuth") as? Double
                } ?: 0.0
            } else {
                0.0
            }

            // ✅ 마스터 데이터 생성
            originalMst.add(
                mapOf(
                    "No" to mstId,
                    "SatelliteID" to satelliteId,
                    "SatelliteName" to satelliteName,
                    "StartTime" to metrics["StartTime"],
                    "EndTime" to metrics["EndTime"],
                    "Duration" to metrics["Duration"],
                    "MaxElevation" to metrics["MaxElevation"],
                    "MaxElevationTime" to metrics["MaxElevationTime"],
                    "MaxAzimuth" to metrics["MaxAzimuth"],
                    "StartAzimuth" to metrics["StartAzimuth"],
                    "StartElevation" to metrics["StartElevation"],
                    "EndAzimuth" to metrics["EndAzimuth"],
                    "EndElevation" to metrics["EndElevation"],
                    "MaxAzRate" to metrics["MaxAzRate"],
                    "MaxElRate" to metrics["MaxElRate"],
                    "MaxAzAccel" to metrics["MaxAzAccel"],
                    "MaxElAccel" to metrics["MaxElAccel"],
                    "IsKeyhole" to isKeyhole,
                    "RecommendedTrainAngle" to recommendedTrainAngle,
                    "CreationDate" to ZonedDateTime.now(),
                    "Creator" to "System",
                    "DataType" to "original"
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

                // 3축 변환 적용
                val (transformedAz, transformedEl) = CoordinateTransformer.transformCoordinatesWithTrain(
                    azimuth = originalAz,
                    elevation = originalEl,
                    tiltAngle = settingsService.tiltAngle,
                    trainAngle = recommendedTrainAngle
                )

                axisTransformedDtl.add(
                    mapOf(
                        "No" to (index + 1).toUInt(),
                        "MstId" to mstId,  // ← 마스터와 연결 유지!
                        "Time" to time,
                        "Azimuth" to transformedAz,
                        "Elevation" to transformedEl,
                        "Train" to recommendedTrainAngle,
                        "DataType" to "axis_transformed"
                    )
                )
            }

            // ✅ 변환 후 메타데이터 재계산
            val transformedPassDtl = axisTransformedDtl.filter { it["MstId"] == mstId }
            val metrics = calculateMetrics(transformedPassDtl)

            // Keyhole 재분석
            val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
            val threshold = settingsService.keyholeAzimuthVelocityThreshold
            val isKeyhole = maxAzRate >= threshold

            axisTransformedMst.add(
                mapOf(
                    "No" to mstId,
                    "SatelliteID" to mstData["SatelliteID"],
                    "SatelliteName" to mstData["SatelliteName"],
                    "StartTime" to metrics["StartTime"],
                    "EndTime" to metrics["EndTime"],
                    "Duration" to metrics["Duration"],
                    "MaxElevation" to metrics["MaxElevation"],
                    "MaxElevationTime" to metrics["MaxElevationTime"],
                    "MaxAzimuth" to metrics["MaxAzimuth"],
                    "StartAzimuth" to metrics["StartAzimuth"],
                    "StartElevation" to metrics["StartElevation"],
                    "EndAzimuth" to metrics["EndAzimuth"],
                    "EndElevation" to metrics["EndElevation"],
                    "MaxAzRate" to metrics["MaxAzRate"],
                    "MaxElRate" to metrics["MaxElRate"],
                    "MaxAzAccel" to metrics["MaxAzAccel"],
                    "MaxElAccel" to metrics["MaxElAccel"],
                    "IsKeyhole" to isKeyhole,
                    "RecommendedTrainAngle" to recommendedTrainAngle,
                    "CreationDate" to mstData["CreationDate"],
                    "Creator" to mstData["Creator"],
                    "DataType" to "axis_transformed"
                )
            )
        }

        return Pair(axisTransformedMst, axisTransformedDtl)
    }

    /**
     * 각도 제한 변환 적용 (±270°)
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

            logger.debug("패스 #$mstId 각도제한 변환 중")

            // 해당 패스의 상세 데이터 조회
            val passDtl = axisTransformedDtl.filter { it["MstId"] == mstId }

            // LimitAngleCalculator로 각도 제한 적용
            val (_, convertedDtl) = limitAngleCalculator.convertTrackingData(
                emptyList(),  // Mst는 이미 있으므로 빈 리스트
                passDtl
            )

            // DataType을 final_transformed로 변경
            convertedDtl.forEach { dtl ->
                finalTransformedDtl.add(
                    dtl.toMutableMap().apply {
                        put("DataType", "final_transformed")
                    }
                )
            }

            // ✅ 변환 후 메타데이터 재계산
            val finalPassDtl = finalTransformedDtl.filter { it["MstId"] == mstId }
            val metrics = calculateMetrics(finalPassDtl)

            // Keyhole 재분석
            val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
            val threshold = settingsService.keyholeAzimuthVelocityThreshold
            val isKeyhole = maxAzRate >= threshold

            finalTransformedMst.add(
                mapOf(
                    "No" to mstId,
                    "SatelliteID" to mstData["SatelliteID"],
                    "SatelliteName" to mstData["SatelliteName"],
                    "StartTime" to metrics["StartTime"],
                    "EndTime" to metrics["EndTime"],
                    "Duration" to metrics["Duration"],
                    "MaxElevation" to metrics["MaxElevation"],
                    "MaxElevationTime" to metrics["MaxElevationTime"],
                    "MaxAzimuth" to metrics["MaxAzimuth"],
                    "StartAzimuth" to metrics["StartAzimuth"],
                    "StartElevation" to metrics["StartElevation"],
                    "EndAzimuth" to metrics["EndAzimuth"],
                    "EndElevation" to metrics["EndElevation"],
                    "MaxAzRate" to metrics["MaxAzRate"],
                    "MaxElRate" to metrics["MaxElRate"],
                    "MaxAzAccel" to metrics["MaxAzAccel"],
                    "MaxElAccel" to metrics["MaxElAccel"],
                    "IsKeyhole" to isKeyhole,
                    "RecommendedTrainAngle" to mstData["RecommendedTrainAngle"],
                    "CreationDate" to mstData["CreationDate"],
                    "Creator" to mstData["Creator"],
                    "DataType" to "final_transformed"
                )
            )
        }

        return Pair(finalTransformedMst, finalTransformedDtl)
    }

    /**
     * 상세 데이터에서 메타데이터 계산
     *
     * @param dtlData 상세 데이터 리스트
     * @return 계산된 메타데이터 Map
     */
    private fun calculateMetrics(dtlData: List<Map<String, Any?>>): Map<String, Any?> {
        if (dtlData.isEmpty()) {
            return emptyMap()
        }

        val firstPoint = dtlData.first()
        val lastPoint = dtlData.last()

        val startTime = firstPoint["Time"] as? ZonedDateTime
        val endTime = lastPoint["Time"] as? ZonedDateTime
        val duration = if (startTime != null && endTime != null) {
            Duration.between(startTime, endTime).toString()
        } else {
            "PT0S"
        }

        // 최대 고도각 및 시간
        val maxElPoint = dtlData.maxByOrNull { (it["Elevation"] as? Double) ?: -90.0 }
        val maxElevation = maxElPoint?.get("Elevation") as? Double ?: 0.0
        val maxElevationTime = maxElPoint?.get("Time") as? ZonedDateTime
        val maxAzimuth = maxElPoint?.get("Azimuth") as? Double ?: 0.0

        // 시작/종료 각도
        val startAzimuth = firstPoint["Azimuth"] as? Double ?: 0.0
        val startElevation = firstPoint["Elevation"] as? Double ?: 0.0
        val endAzimuth = lastPoint["Azimuth"] as? Double ?: 0.0
        val endElevation = lastPoint["Elevation"] as? Double ?: 0.0

        // 각속도 및 각가속도 계산
        var maxAzRate = 0.0
        var maxElRate = 0.0
        var maxAzAccel = 0.0
        var maxElAccel = 0.0

        var prevAz: Double? = null
        var prevEl: Double? = null
        var prevTime: ZonedDateTime? = null
        var prevAzRate: Double? = null
        var prevElRate: Double? = null

        dtlData.forEach { point ->
            val az = point["Azimuth"] as? Double
            val el = point["Elevation"] as? Double
            val time = point["Time"] as? ZonedDateTime

            if (az != null && el != null && time != null && prevAz != null && prevEl != null && prevTime != null) {
                val timeDiff = Duration.between(prevTime, time).toMillis() / 1000.0

                if (timeDiff > 0.001) {  // 최소 시간 간격 체크
                    // 방위각 변화 (360도 경계 처리)
                    // ✅ 스마트 캐스트 에러 해결: 명시적 언래핑 (!!)
                    var azDiff = az - prevAz!!
                    if (azDiff > 180) azDiff -= 360
                    if (azDiff < -180) azDiff += 360

                    val elDiff = el - prevEl!!

                    // 각속도
                    val azRate = azDiff / timeDiff
                    val elRate = elDiff / timeDiff

                    maxAzRate = maxOf(maxAzRate, abs(azRate))
                    maxElRate = maxOf(maxElRate, abs(elRate))

                    // 각가속도
                    if (prevAzRate != null && prevElRate != null) {
                        // ✅ 스마트 캐스트 에러 해결: 명시적 언래핑 (!!)
                        val azAccel = (azRate - prevAzRate!!) / timeDiff
                        val elAccel = (elRate - prevElRate!!) / timeDiff

                        maxAzAccel = maxOf(maxAzAccel, abs(azAccel))
                        maxElAccel = maxOf(maxElAccel, abs(elAccel))
                    }

                    prevAzRate = azRate
                    prevElRate = elRate
                }
            }

            prevAz = az
            prevEl = el
            prevTime = time
        }

        return mapOf(
            "StartTime" to startTime,
            "EndTime" to endTime,
            "Duration" to duration,
            "MaxElevation" to maxElevation,
            "MaxElevationTime" to maxElevationTime,
            "MaxAzimuth" to maxAzimuth,
            "StartAzimuth" to startAzimuth,
            "StartElevation" to startElevation,
            "EndAzimuth" to endAzimuth,
            "EndElevation" to endElevation,
            "MaxAzRate" to maxAzRate,
            "MaxElRate" to maxElRate,
            "MaxAzAccel" to maxAzAccel,
            "MaxElAccel" to maxElAccel
        )
    }
}

