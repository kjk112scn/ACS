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
            
            // ============================================================
            // Train 계산 방법 선택
            // 방법 A: calculateTrainAngleMethodA(metrics)  - 2축 기준
            // 방법 B: calculateTrainAngleMethodB(passDtl) - 최종 기준
            // ============================================================
            val currentMethod = "B"  // ← 여기만 변경 (A 또는 B)
            
            // Keyhole인 경우 최적 Train 각도 계산
            val recommendedTrainAngle = if (isKeyhole) {
                val trainAngle = if (currentMethod == "A") {
                    calculateTrainAngleMethodA(metrics)
                } else {
                    calculateTrainAngleMethodB(passDtl)
                }
                
                // 상세 Train 각도 계산 로그
                val maxAzRateAzimuth = metrics["MaxAzRateAzimuth"] as? Double ?: 0.0
                val maxAzRateTime = metrics["MaxAzRateTime"] as? ZonedDateTime
                
                logger.info("=".repeat(60))
                logger.info("🔍 패스 #${index + 1} ($satelliteName) Train 각도 계산 [방법 $currentMethod]")
                logger.info("-".repeat(60))
                
                if (currentMethod == "A") {
                    logger.info("📊 입력 데이터:")
                    logger.info("  - Original MaxAzRate: ${String.format("%.6f", maxAzRate)}°/s")
                    logger.info("  - 2축 최대 각속도 시점: $maxAzRateTime")
                    logger.info("  - 해당 시점 Azimuth: ${String.format("%.6f", maxAzRateAzimuth)}°")
                    logger.info("")
                    logger.info("📊 Train 각도 계산 (방법 A: 2축 각속도 시점 기준):")
                    logger.info("  - 2축 최대 각속도 시점 Azimuth로 Train 각도 계산 (최단 거리)")
                    logger.info("")
                } else {
                    logger.info("📊 입력 데이터:")
                    logger.info("  - Original MaxAzRate: ${String.format("%.6f", maxAzRate)}°/s")
                    logger.info("  - 2축 최대 각속도 시점: $maxAzRateTime")
                    logger.info("  - 2축 해당 시점 Azimuth: ${String.format("%.6f", maxAzRateAzimuth)}°")
                    logger.info("")
                    logger.info("📊 Train 각도 계산 (방법 B: 최종 각속도 시점 기준):")
                    logger.info("  - Train=0으로 최종 변환 후 최대 각속도 시점 Azimuth 추출")
                    logger.info("  - 해당 Azimuth로 Train 각도 계산 (최단 거리)")
                    logger.info("")
                }
                
                logger.info("✅ 선택된 Train 각도: ${String.format("%.6f", trainAngle)}°")
                logger.info("   회전량: ${String.format("%.6f", Math.abs(trainAngle))}° (${if (trainAngle >= 0) "시계 방향" else "반시계 방향"})")
                logger.info("=".repeat(60))
                
                trainAngle
            } else {
                0.0
            }

            // ============================================================
            // 새로 추가: 별도 분석 함수 호출 (기존 로직에 영향 없음)
            // ============================================================
            if (isKeyhole) {
                // currentMethod는 위에서 정의됨 (Line 137)
                analyzeTrainOptimization(
                    satelliteName = satelliteName,
                    passIndex = index,
                    originalDtl = passDtl,
                    originalMetrics = metrics,
                    currentTrainAngle = recommendedTrainAngle,
                    currentMethod = currentMethod
                )
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
     * 방법 A: 2축 최대 각속도 시점 기준으로 Train 각도 계산
     * 
     * Original 데이터의 최대 각속도 시점 Azimuth를 사용
     * 
     * @param originalMetrics 원본 메트릭
     * @return Train 각도
     */
    private fun calculateTrainAngleMethodA(
        originalMetrics: Map<String, Any?>
    ): Double {
        val maxAzRateAzimuth = originalMetrics["MaxAzRateAzimuth"] as? Double ?: 0.0
        return calculateTrainAngle(maxAzRateAzimuth)
    }

    /**
     * 방법 B: 최종 최대 각속도 시점 기준으로 Train 각도 계산
     * 
     * Train=0으로 최종 변환 후 최대 각속도 시점 Azimuth를 사용
     * 
     * @param originalDtl 원본 상세 데이터
     * @return Train 각도
     */
    private fun calculateTrainAngleMethodB(
        originalDtl: List<Map<String, Any?>>
    ): Double {
        // Train=0으로 임시 변환하여 최종 각속도 시점 찾기
        val finalMetrics0 = simulateTrainApplication(originalDtl, 0.0)
        val finalMaxAzRateAzimuth0 = finalMetrics0["MaxAzRateAzimuth"] as? Double ?: 0.0
        return calculateTrainAngle(finalMaxAzRateAzimuth0)
    }

    /**
     * Train 각도 계산 (최단 거리, ±270° 범위)
     * 
     * 안테나 서쪽(+7°)이 위성을 향하도록 Train 각도 계산
     * 270° 기준으로 최단 경로 선택하되, ±270° 범위 제한 준수
     * 
     * @param azimuth 목표 방위각
     * @return 정규화된 Train 각도 (±270° 범위)
     */
    private fun calculateTrainAngle(azimuth: Double): Double {
        // Azimuth를 0-360 범위로 정규화
        var normalizedAz = azimuth % 360.0
        if (normalizedAz < 0) normalizedAz += 360.0
        
        // 두 가지 경로 계산
        val option1 = normalizedAz - 270.0  // 기본 계산
        val option2 = if (option1 < 0) {
            option1 + 360.0  // 음수면 시계 방향
        } else {
            option1 - 360.0  // 양수면 반시계 방향
        }
        
        // ±270° 범위 내 유효한 옵션만 선택
        val validOptions = mutableListOf<Double>()
        
        if (option1 >= -270.0 && option1 <= 270.0) {
            validOptions.add(option1)
        }
        if (option2 >= -270.0 && option2 <= 270.0) {
            validOptions.add(option2)
        }
        
        // 유효한 옵션 중 절댓값이 작은 것 선택
        return validOptions.minByOrNull { Math.abs(it) } ?: option1
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
        var maxAzRate = 0.0//최대 Elevation 각도 시점의 Azimuth 각도
        var maxAzRateAzimuth = 0.0  // 추가: 최대 각속도 시점의 Azimuth
        var maxAzRateTime: ZonedDateTime? = null  // 추가: 시간 (디버깅용)
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

                    // ✅ 최대 각속도 갱신 시 Azimuth 저장
                    if (abs(azRate) > maxAzRate) {
                        maxAzRate = abs(azRate)
                        maxAzRateAzimuth = az  // 현재 시점의 Azimuth 저장
                        maxAzRateTime = time
                    }
                    
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
            "MaxAzRateAzimuth" to maxAzRateAzimuth,  // ✅ 추가
            "MaxAzRateTime" to maxAzRateTime,  // ✅ 추가
            "MaxElRate" to maxElRate,
            "MaxAzAccel" to maxAzAccel,
            "MaxElAccel" to maxElAccel
        )
    }

    /**
     * Keyhole Train 최적화 비교 분석 (로그 전용, 기존 로직에 영향 없음)
     * 
     * 방법 A (2축 기준) vs 방법 B (최종 기준) 비교하여 로그 출력
     * 
     * @param satelliteName 위성 이름
     * @param passIndex 패스 인덱스
     * @param originalDtl 원본 상세 데이터
     * @param originalMetrics 원본 메트릭
     * @param currentTrainAngle 현재 선택된 Train 각도 (기존 로직)
     */
    private fun analyzeTrainOptimization(
        satelliteName: String,
        passIndex: Int,
        originalDtl: List<Map<String, Any?>>,
        originalMetrics: Map<String, Any?>,
        currentTrainAngle: Double,
        currentMethod: String  // "A" or "B"
    ) {
        logger.info("")
        logger.info("=".repeat(61))
        logger.info("🔬 Train 최적화 비교 분석 (참고용)")
        logger.info("=".repeat(61))
        logger.info("위성: $satelliteName")
        logger.info("패스 번호: #${passIndex + 1}")
        logger.info("현재 적용 Train: ${String.format("%.6f", currentTrainAngle)}°")
        logger.info("")
        
        try {
            // ========================================================
            // 방법 A: 2축 최대 각속도 시점 기준 (현재 방식)
            // ========================================================
            val originalMaxAzRate = originalMetrics["MaxAzRate"] as? Double ?: 0.0
            val originalMaxAzRateAzimuth = originalMetrics["MaxAzRateAzimuth"] as? Double ?: 0.0
            val originalMaxAzRateTime = originalMetrics["MaxAzRateTime"] as? ZonedDateTime
            
            val trainAngleA = calculateTrainAngleMethodA(originalMetrics)
            val finalMetricsA = simulateTrainApplication(originalDtl, trainAngleA)
            val finalMaxAzRateA = finalMetricsA["MaxAzRate"] as? Double ?: 0.0
            
            logger.info("-".repeat(61))
            if (currentMethod == "A") {
                logger.info("📊 방법 A: 2축 최대 각속도 시점 기준 (✅ 현재 적용)")
            } else {
                logger.info("📊 방법 A: 2축 최대 각속도 시점 기준 (대안)")
            }
            logger.info("-".repeat(61))
            logger.info("[1단계] 2축 데이터 분석:")
            logger.info("  - Original MaxAzRate: ${String.format("%.6f", originalMaxAzRate)}°/s")
            logger.info("  - 최대 각속도 시점: $originalMaxAzRateTime")
            logger.info("  - 해당 시점 Azimuth: ${String.format("%.6f", originalMaxAzRateAzimuth)}°")
            logger.info("")
            logger.info("[2단계] Train 각도 계산:")
            logger.info("  - 입력 Azimuth: ${String.format("%.6f", originalMaxAzRateAzimuth)}°")
            logger.info("  - 계산된 Train: ${String.format("%.6f", trainAngleA)}° (최단 거리)")
            logger.info("")
            logger.info("[3단계] Train 적용 후 최종 결과:")
            logger.info("  - Final MaxAzRate: ${String.format("%.6f", finalMaxAzRateA)}°/s")
            logger.info("")
            
            // ========================================================
            // 방법 B: 최종 최대 각속도 시점 기준 (대안)
            // ========================================================
            // Train=0으로 임시 변환
            val finalMetrics0 = simulateTrainApplication(originalDtl, 0.0)
            val finalMaxAzRate0 = finalMetrics0["MaxAzRate"] as? Double ?: 0.0
            val finalMaxAzRateAzimuth0 = finalMetrics0["MaxAzRateAzimuth"] as? Double ?: 0.0
            val finalMaxAzRateTime0 = finalMetrics0["MaxAzRateTime"] as? ZonedDateTime
            
            val trainAngleB = calculateTrainAngleMethodB(originalDtl)
            val finalMetricsB = simulateTrainApplication(originalDtl, trainAngleB)
            val finalMaxAzRateB = finalMetricsB["MaxAzRate"] as? Double ?: 0.0
            
            logger.info("-".repeat(61))
            if (currentMethod == "B") {
                logger.info("📊 방법 B: 최종 최대 각속도 시점 기준 (✅ 현재 적용)")
            } else {
                logger.info("📊 방법 B: 최종 최대 각속도 시점 기준 (대안)")
            }
            logger.info("-".repeat(61))
            logger.info("[1단계] Train=0으로 최종 변환:")
            logger.info("  - Final MaxAzRate: ${String.format("%.6f", finalMaxAzRate0)}°/s (Train 미적용)")
            logger.info("  - 최대 각속도 시점: $finalMaxAzRateTime0")
            logger.info("  - 해당 시점 Azimuth: ${String.format("%.6f", finalMaxAzRateAzimuth0)}°")
            logger.info("")
            logger.info("[2단계] 새로운 Train 각도 계산:")
            logger.info("  - 입력 Azimuth: ${String.format("%.6f", finalMaxAzRateAzimuth0)}°")
            logger.info("  - 계산된 Train: ${String.format("%.6f", trainAngleB)}° (최단 거리)")
            logger.info("")
            logger.info("[3단계] 새 Train 적용 후 최종 결과:")
            logger.info("  - Final MaxAzRate: ${String.format("%.6f", finalMaxAzRateB)}°/s")
            logger.info("")
            
            // ========================================================
            // 비교 결과
            // ========================================================
            logger.info("-".repeat(61))
            logger.info("📈 비교 결과")
            logger.info("-".repeat(61))
            if (currentMethod == "A") {
                logger.info("방법 A (✅ 현재 적용):")
            } else {
                logger.info("방법 A:")
            }
            logger.info("  - Train 각도: ${String.format("%.6f", trainAngleA)}°")
            logger.info("  - Final MaxAzRate: ${String.format("%.6f", finalMaxAzRateA)}°/s")
            logger.info("")
            if (currentMethod == "B") {
                logger.info("방법 B (✅ 현재 적용):")
            } else {
                logger.info("방법 B:")
            }
            logger.info("  - Train 각도: ${String.format("%.6f", trainAngleB)}°")
            logger.info("  - Final MaxAzRate: ${String.format("%.6f", finalMaxAzRateB)}°/s")
            logger.info("")
            
            val diff = finalMaxAzRateA - finalMaxAzRateB
            val betterMethod = if (finalMaxAzRateA <= finalMaxAzRateB) "방법 A" else "방법 B"
            
            logger.info("차이: ${String.format("%.6f", Math.abs(diff))}°/s")
            logger.info("더 나은 방법: $betterMethod ${if (diff > 0) "(방법 B가 ${String.format("%.2f", (diff/finalMaxAzRateA)*100)}% 낮음)" else "(방법 A가 ${String.format("%.2f", (Math.abs(diff)/finalMaxAzRateB)*100)}% 낮음)"}")
            logger.info("")
            logger.info("⚠️  참고: 실제 적용은 방법 $currentMethod 사용 중")
            
        } catch (e: Exception) {
            logger.error("❌ Train 최적화 분석 중 오류 발생: ${e.message}", e)
        }
        
        logger.info("=".repeat(61))
        logger.info("")
    }

    /**
     * Train 적용 시뮬레이션 (분석용, 실제 데이터 변경 없음)
     * 
     * @param originalDtl 원본 상세 데이터
     * @param trainAngle Train 각도
     * @return 최종 변환 후 메트릭
     */
    private fun simulateTrainApplication(
        originalDtl: List<Map<String, Any?>>,
        trainAngle: Double
    ): Map<String, Any?> {
        val tempTransformedDtl = mutableListOf<Map<String, Any?>>()
        
        // 3축 변환 적용
        originalDtl.forEach { point ->
            val originalAz = point["Azimuth"] as Double
            val originalEl = point["Elevation"] as Double
            val time = point["Time"] as ZonedDateTime
            
            val (transformedAz, transformedEl) = CoordinateTransformer.transformCoordinatesWithTrain(
                azimuth = originalAz,
                elevation = originalEl,
                tiltAngle = settingsService.tiltAngle,
                trainAngle = trainAngle
            )
            
            tempTransformedDtl.add(
                mapOf(
                    "Time" to time,
                    "Azimuth" to transformedAz,
                    "Elevation" to transformedEl
                )
            )
        }
        
        // 각도 제한 적용
        val finalDtl = tempTransformedDtl.map { point ->
            val az = point["Azimuth"] as Double
            val el = point["Elevation"] as Double
            val time = point["Time"] as ZonedDateTime
            
            var limitedAz = az
            while (limitedAz > 270.0) limitedAz -= 360.0
            while (limitedAz < -270.0) limitedAz += 360.0
            
            mapOf(
                "Time" to time,
                "Azimuth" to limitedAz,
                "Elevation" to el
            )
        }
        
        // 메트릭 계산
        return calculateMetrics(finalDtl)
    }
}

