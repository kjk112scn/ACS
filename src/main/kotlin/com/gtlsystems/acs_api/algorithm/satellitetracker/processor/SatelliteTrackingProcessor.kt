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

        // 2️⃣ 3축 변환 (Train=0 강제)
        val (axisTransformedMst, axisTransformedDtl) = applyAxisTransformation(
            originalMst,
            originalDtl,
            forcedTrainAngle = 0.0  // ✅ Train=0 강제 (DataType: axis_transformed)
        )
        logger.info("✅ 3축 변환 완료 (Train=0 적용): ${axisTransformedMst.size}개 마스터, ${axisTransformedDtl.size}개 상세")

        // 3️⃣ ±270° 변환
        val (finalTransformedMst, finalTransformedDtl) = applyAngleLimitTransformation(
            axisTransformedMst,
            axisTransformedDtl
        )
        logger.info("✅ 각도제한 변환 완료: ${finalTransformedMst.size}개 마스터, ${finalTransformedDtl.size}개 상세")

        // 4️⃣ Keyhole 판단 및 Train≠0 재계산
        logger.info("📊 Keyhole 판단 및 Train≠0 데이터 생성 시작...")

        val keyholeAxisTransformedMst = mutableListOf<Map<String, Any?>>()
        val keyholeAxisTransformedDtl = mutableListOf<Map<String, Any?>>()
        val keyholeFinalTransformedMst = mutableListOf<Map<String, Any?>>()
        val keyholeFinalTransformedDtl = mutableListOf<Map<String, Any?>>()

        finalTransformedMst.forEachIndexed { index, mstData ->
            val mstId = mstData["No"] as UInt

            /**
             * Keyhole 판단 및 Train≠0 재계산
             * 
             * finalTransformedMst의 IsKeyhole 값을 직접 참조함 (재판단하지 않음).
             * applyAngleLimitTransformation()에서 이미 계산된 값임.
             * 
             * Keyhole 발생 시 finalTransformedMst의 RecommendedTrainAngle을 사용함.
             * 이 값은 finalTransformedMst 기준으로 본인 데이터로 계산된 값임.
             * 
             * 중요: originalMst의 RecommendedTrainAngle을 사용하지 않음.
             * - originalMst: 2축 기준으로 계산된 값
             * - finalTransformedMst: 3축, Train=0, ±270도 제한 있음 기준으로 계산된 값 (시스템의 주요 판단 기준)
             */
            // ✅ finalTransformedMst의 IsKeyhole 값을 직접 참조 (재판단하지 않음)
            val isKeyhole = mstData["IsKeyhole"] as? Boolean ?: false
            val train0MaxAzRate = mstData["MaxAzRate"] as? Double ?: 0.0

            logger.info("패스 #$mstId: Train=0 MaxAzRate = ${String.format("%.6f", train0MaxAzRate)}°/s")
            logger.info("   Keyhole 판단 결과 (finalTransformedMst): ${if (isKeyhole) "✅ Keyhole 발생" else "✅ Keyhole 미발생"}")

            // Keyhole 발생 시 Train≠0 재계산
            if (isKeyhole) {
                /**
                 * finalTransformedMst의 RecommendedTrainAngle 사용
                 * 
                 * 이 값은 finalTransformedMst 기준으로 본인 데이터로 계산된 값임.
                 * 안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 Train 각도임.
                 * 
                 * @param mstData finalTransformedMst의 MST 데이터
                 * @return RecommendedTrainAngle (Keyhole 발생 시 계산된 Train 각도)
                 */
                // ✅ finalTransformedMst의 RecommendedTrainAngle 사용
                val recommendedTrainAngle = mstData["RecommendedTrainAngle"] as? Double ?: 0.0
                
                logger.info("   계산된 Train 각도 (finalTransformedMst): ${String.format("%.6f", recommendedTrainAngle)}°")
                logger.info("🔄 Train=${String.format("%.6f", recommendedTrainAngle)}°로 재변환 시작...")

                // 해당 패스의 Original DTL 추출
                val passOriginalDtl = originalDtl.filter { it["MstId"] == mstId }

                /**
                 * Original MST를 Train≠0으로 업데이트
                 * 
                 * finalTransformedMst의 RecommendedTrainAngle을 사용하여 keyholeOriginalMst를 생성함.
                 * 이 값은 이후 applyAxisTransformation()에서 trainAngleForTransformation으로 사용됨.
                 * 
                 * @param originalMst[index] Original MST 데이터
                 * @param recommendedTrainAngle finalTransformedMst의 RecommendedTrainAngle
                 * @return keyholeOriginalMst Train≠0으로 업데이트된 Original MST
                 */
                // Original MST를 Train≠0으로 업데이트
                val keyholeOriginalMst = listOf(originalMst[index].toMutableMap().apply {
                    put("RecommendedTrainAngle", recommendedTrainAngle)  // ✅ finalTransformedMst의 값 사용
                    put("IsKeyhole", true)
                })

                // 정규 절차로 재변환
                logger.info("   📊 Original DTL 필터링: ${passOriginalDtl.size}개")
                
                val (keyholeAxisMst, keyholeAxisDtl) = applyAxisTransformation(
                    keyholeOriginalMst,
                    passOriginalDtl
                )
                logger.info("   📊 Keyhole Axis 변환 완료: MST=${keyholeAxisMst.size}개, DTL=${keyholeAxisDtl.size}개")

                // ✅ Keyhole Axis 데이터 저장 (각도 제한 ❌)
                keyholeAxisDtl.forEach { dtl ->
                    keyholeAxisTransformedDtl.add(dtl.toMutableMap().apply {
                        put("DataType", "keyhole_axis_transformed")
                    })
                }

                keyholeAxisMst.forEach { mst ->
                    keyholeAxisTransformedMst.add(mst.toMutableMap().apply {
                        put("DataType", "keyhole_axis_transformed")
                    })
                }

                val (keyholeFinalMst, keyholeFinalDtl) = applyAngleLimitTransformation(
                    keyholeAxisMst,
                    keyholeAxisDtl
                )
                logger.info("   📊 Keyhole Final 변환 완료: MST=${keyholeFinalMst.size}개, DTL=${keyholeFinalDtl.size}개")

                // ✅ Keyhole Final 데이터 저장 (각도 제한 ✅)
                keyholeFinalDtl.forEach { dtl ->
                    keyholeFinalTransformedDtl.add(dtl.toMutableMap().apply {
                        put("DataType", "keyhole_final_transformed")
                    })
                }

                keyholeFinalMst.forEach { mst ->
                    keyholeFinalTransformedMst.add(mst.toMutableMap().apply {
                        put("DataType", "keyhole_final_transformed")
                    })
                }

                logger.info("✅ Keyhole 데이터 저장 완료: Axis=${keyholeAxisDtl.size}개, Final=${keyholeFinalDtl.size}개")
            }

            logger.info("")
        }

        logger.info("=".repeat(60))
        logger.info("🎉 전체 변환 완료")
        logger.info("   Original: ${originalDtl.size}개")
        logger.info("   Axis Transformed (Train=0): ${axisTransformedDtl.size}개")
        logger.info("   Final Transformed (Train=0): ${finalTransformedDtl.size}개")
        logger.info("   Keyhole Axis (Train≠0): ${keyholeAxisTransformedDtl.size}개")
        logger.info("   Keyhole Final (Train≠0): ${keyholeFinalTransformedDtl.size}개")
        logger.info("=".repeat(60))

        return ProcessedTrackingData(
            originalMst = originalMst,
            originalDtl = originalDtl,
            axisTransformedMst = axisTransformedMst,
            axisTransformedDtl = axisTransformedDtl,
            finalTransformedMst = finalTransformedMst,
            finalTransformedDtl = finalTransformedDtl,
            keyholeAxisTransformedMst = keyholeAxisTransformedMst,           // ✅ 추가
            keyholeAxisTransformedDtl = keyholeAxisTransformedDtl,           // ✅ 추가
            keyholeFinalTransformedMst = keyholeFinalTransformedMst,
            keyholeFinalTransformedDtl = keyholeFinalTransformedDtl
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
            
            /**
             * RecommendedTrainAngle 계산 (MST 저장용)
             * 
             * 각 MST는 본인 기준에서 Keyhole 판단 및 RecommendedTrainAngle을 계산해야 함.
             * Original MST는 2축 기준 데이터로 계산함.
             * 
             * 계산 방식:
             * 1. 본인의 DTL 데이터로 calculateMetrics() 호출하여 MaxAzRateAzimuth 획득
             * 2. calculateTrainAngle()을 직접 호출하여 안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 Train 각도 계산
             * 
             * @param isKeyhole Keyhole 발생 여부
             * @param metrics calculateMetrics()로 계산된 메타데이터 (MaxAzRateAzimuth 포함)
             * @return RecommendedTrainAngle (Keyhole이면 계산된 Train 각도, 아니면 0.0)
             */
            val recommendedTrainAngle = if (isKeyhole) {
                // 본인의 DTL 데이터로 calculateMetrics() 호출 → MaxAzRateAzimuth 얻기
                // 이미 위에서 calculateMetrics(passDtl) 호출했으므로 metrics 사용
                val maxAzRateAzimuth = metrics["MaxAzRateAzimuth"] as? Double ?: 0.0
                val maxAzRateTime = metrics["MaxAzRateTime"] as? ZonedDateTime
                
                // calculateTrainAngle() 직접 호출 (래퍼 함수 사용하지 않음)
                val trainAngle = calculateTrainAngle(maxAzRateAzimuth)
                
                // 상세 Train 각도 계산 로그
                logger.info("=".repeat(60))
                logger.info("🔍 패스 #${index + 1} ($satelliteName) Train 각도 계산 (2축 기준)")
                logger.info("-".repeat(60))
                logger.info("📊 입력 데이터:")
                logger.info("  - Original MaxAzRate: ${String.format("%.6f", maxAzRate)}°/s")
                logger.info("  - 2축 최대 각속도 시점: $maxAzRateTime")
                logger.info("  - 해당 시점 Azimuth: ${String.format("%.6f", maxAzRateAzimuth)}°")
                logger.info("")
                logger.info("📊 Train 각도 계산:")
                logger.info("  - 2축 최대 각속도 시점 Azimuth로 Train 각도 계산")
                logger.info("  - 안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 Train 각도")
                logger.info("")
                logger.info("✅ 선택된 Train 각도: ${String.format("%.6f", trainAngle)}°")
                logger.info("   회전량: ${String.format("%.6f", Math.abs(trainAngle))}° (${if (trainAngle >= 0) "시계 방향" else "반시계 방향"})")
                logger.info("=".repeat(60))
                
                trainAngle
            } else {
                0.0
            }

            // ============================================================
            // 별도 분석 함수 호출 (기존 로직에 영향 없음)
            // ============================================================
            if (isKeyhole) {
                analyzeTrainOptimization(
                    satelliteName = satelliteName,
                    passIndex = index,
                    originalDtl = passDtl,
                    originalMetrics = metrics,
                    currentTrainAngle = recommendedTrainAngle,
                    currentMethod = "A"  // 이제는 직접 계산하므로 "A" 방식으로 표시
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
        originalDtl: List<Map<String, Any?>>,
        forcedTrainAngle: Double? = null  // ✅ 추가: null이면 MST에서 읽고, 값이 있으면 강제 사용
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {

        val axisTransformedMst = mutableListOf<Map<String, Any?>>()
        val axisTransformedDtl = mutableListOf<Map<String, Any?>>()

        originalMst.forEach { mstData ->
            val mstId = mstData["No"] as UInt
            
            /**
             * 3축 변환용 Train 각도 (trainAngleForTransformation)
             * 
             * 이 값은 3축 변환에 사용되는 Train 각도임.
             * - forcedTrainAngle=0.0: 항상 0.0 (axis_transformed, final_transformed 생성 시)
             * - forcedTrainAngle=null: MST에서 읽은 RecommendedTrainAngle (keyhole_* 생성 시)
             * 
             * 주의: 이 값은 MST에 저장되는 RecommendedTrainAngle과는 별개임.
             * MST 저장용 RecommendedTrainAngle은 본인 기준으로 별도 계산됨.
             */
            val trainAngleForTransformation = forcedTrainAngle ?: (mstData["RecommendedTrainAngle"] as? Double ?: 0.0)

            logger.debug("패스 #$mstId 3축 변환 중 (Train: ${trainAngleForTransformation}°${if (forcedTrainAngle != null) " [강제 적용]" else " [MST에서 읽음]"})")

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
                    trainAngle = trainAngleForTransformation  // ✅ 3축 변환용 Train 사용
                )

                axisTransformedDtl.add(
                    mapOf(
                        "No" to (index + 1).toUInt(),
                        "MstId" to mstId,  // ← 마스터와 연결 유지!
                        "Time" to time,
                        "Azimuth" to transformedAz,
                        "Elevation" to transformedEl,
                        "Train" to trainAngleForTransformation,  // ✅ 3축 변환용 Train 저장
                        "DataType" to "axis_transformed"
                    )
                )
            }

            // ✅ 변환 후 메타데이터 재계산
            val transformedPassDtl = axisTransformedDtl.filter { it["MstId"] == mstId }
            val metrics = calculateMetrics(transformedPassDtl)

            // Keyhole 재분석 (본인 기준)
            val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
            val threshold = settingsService.keyholeAzimuthVelocityThreshold
            val isKeyhole = maxAzRate >= threshold

            /**
             * RecommendedTrainAngle 계산 (MST 저장용)
             * 
             * 각 MST는 본인 기준에서 Keyhole 판단 및 RecommendedTrainAngle을 계산해야 함.
             * AxisTransformed MST는 3축 변환 후 데이터(±270도 제한 없음, Train=0)로 계산함.
             * 
             * 계산 방식:
             * 1. 변환 후 DTL 데이터로 calculateMetrics() 호출하여 MaxAzRateAzimuth 획득
             * 2. calculateTrainAngle()을 직접 호출하여 안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 Train 각도 계산
             * 
             * 중요: 이 값은 3축 변환용 trainAngleForTransformation과는 별개로 계산됨.
             * - trainAngleForTransformation: 3축 변환에 사용 (forcedTrainAngle=0.0이면 0.0)
             * - recommendedTrainAngleForMst: MST 저장용 (본인 기준으로 계산, Keyhole이면 계산된 값, 아니면 0.0)
             * 
             * @param isKeyhole Keyhole 발생 여부
             * @param metrics calculateMetrics()로 계산된 메타데이터 (MaxAzRateAzimuth 포함)
             * @return RecommendedTrainAngle (Keyhole이면 계산된 Train 각도, 아니면 0.0)
             */
            val recommendedTrainAngleForMst = if (isKeyhole) {
                // 이미 calculateMetrics()로 MaxAzRateAzimuth를 계산했으므로, 이를 사용하여 Train 각도 계산
                val maxAzRateAzimuth = metrics["MaxAzRateAzimuth"] as? Double ?: 0.0
                calculateTrainAngle(maxAzRateAzimuth)  // ✅ 본인 기준으로 계산
            } else {
                0.0
            }

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
                    "RecommendedTrainAngle" to recommendedTrainAngleForMst,  // ✅ 본인 기준에서 계산된 값
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

            // Keyhole 재분석 (본인 기준)
            val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
            val threshold = settingsService.keyholeAzimuthVelocityThreshold
            val isKeyhole = maxAzRate >= threshold

            /**
             * RecommendedTrainAngle 계산 (MST 저장용)
             * 
             * 각 MST는 본인 기준에서 Keyhole 판단 및 RecommendedTrainAngle을 계산해야 함.
             * FinalTransformed MST는 ±270도 제한 적용 후 데이터(3축, Train=0, ±270도 제한 있음)로 계산함.
             * 
             * 계산 방식:
             * 1. ±270도 제한 적용 후 DTL 데이터로 calculateMetrics() 호출하여 MaxAzRateAzimuth 획득
             * 2. calculateTrainAngle()을 직접 호출하여 안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 Train 각도 계산
             * 
             * 중요: 이 값은 본인 기준으로 계산된 값임. AxisTransformed MST의 값과는 별개임.
             * - FinalTransformed MST는 ±270도 제한이 적용된 상태에서 계산하므로 다른 값이 될 수 있음.
             * 
             * @param isKeyhole Keyhole 발생 여부
             * @param metrics calculateMetrics()로 계산된 메타데이터 (MaxAzRateAzimuth 포함)
             * @return RecommendedTrainAngle (Keyhole이면 계산된 Train 각도, 아니면 0.0)
             */
            val recommendedTrainAngle = if (isKeyhole) {
                // 이미 calculateMetrics()로 MaxAzRateAzimuth를 계산했으므로, 이를 사용하여 Train 각도 계산
                val maxAzRateAzimuth = metrics["MaxAzRateAzimuth"] as? Double ?: 0.0
                calculateTrainAngle(maxAzRateAzimuth)  // ✅ 본인 기준으로 계산
            } else {
                0.0
            }

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
                    "MaxAzRateAzimuth" to metrics["MaxAzRateAzimuth"],  // ✅ Train 각도 재계산용
                    "MaxAzRateTime" to metrics["MaxAzRateTime"],  // ✅ 참고용
                    "MaxAzAccel" to metrics["MaxAzAccel"],
                    "MaxElAccel" to metrics["MaxElAccel"],
                    "IsKeyhole" to isKeyhole,
                    "RecommendedTrainAngle" to recommendedTrainAngle,  // ✅ 본인 기준에서 계산된 값
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

        // ✅ 각속도 계산 - 10개 구간(1초) 누적 방식
        var maxAzRate = 0.0  // 1초간 최대 누적 Azimuth 변화량
        var maxAzRateAzimuth = 0.0  // 최대 각속도 시점의 Azimuth
        var maxAzRateTime: ZonedDateTime? = null  // 최대 각속도 시점의 시간
        var maxElRate = 0.0  // 1초간 최대 누적 Elevation 변화량
        var maxAzAccel = 0.0
        var maxElAccel = 0.0

        var prevAzRate: Double? = null
        var prevElRate: Double? = null

        // 10개 구간 윈도우로 각속도 계산
        dtlData.forEachIndexed { i, point ->
            val az = point["Azimuth"] as? Double
            val el = point["Elevation"] as? Double
            val time = point["Time"] as? ZonedDateTime

            if (i >= 9 && az != null && el != null && time != null) {
                // 10개 구간(i-9부터 i까지)의 변화량 누적
                var azSum = 0.0
                var elSum = 0.0
                var totalTimeDiff = 0.0

                for (j in (i - 9)..i) {
                    if (j > 0) {
                        val prevPoint = dtlData[j - 1]
                        val currentPoint = dtlData[j]
                        
                        val prevAz = prevPoint["Azimuth"] as? Double
                        val currentAz = currentPoint["Azimuth"] as? Double
                        val prevEl = prevPoint["Elevation"] as? Double
                        val currentEl = currentPoint["Elevation"] as? Double
                        val prevTime = prevPoint["Time"] as? ZonedDateTime
                        val currentTime = currentPoint["Time"] as? ZonedDateTime

                        if (prevAz != null && currentAz != null && prevEl != null && 
                            currentEl != null && prevTime != null && currentTime != null) {
                            
                            // Azimuth 변화량 (360도 경계 처리)
                            var azDiff = currentAz - prevAz
                            if (azDiff > 180) azDiff -= 360
                            if (azDiff < -180) azDiff += 360
                            azSum += abs(azDiff)

                            // Elevation 변화량
                            elSum += abs(currentEl - prevEl)
                            
                            // 시간 간격 누적
                            totalTimeDiff += Duration.between(prevTime, currentTime).toMillis() / 1000.0
                        }
                    }
                }

                // 1초간 누적 각속도 (총 변화량 = deg/s로 해석)
                val currentAzRate = azSum  // 1초간 총 변화량
                val currentElRate = elSum

                // 최대값 갱신
                if (currentAzRate > maxAzRate) {
                    maxAzRate = currentAzRate
                    maxAzRateAzimuth = az  // 현재 시점의 Azimuth
                    maxAzRateTime = time
                }
                
                maxElRate = maxOf(maxElRate, currentElRate)

                // 각가속도 (필요시 계산, 현재는 사용 안 함)
                if (prevAzRate != null && prevElRate != null && totalTimeDiff > 0.001) {
                    val azAccel = (currentAzRate - prevAzRate!!) / totalTimeDiff
                    val elAccel = (currentElRate - prevElRate!!) / totalTimeDiff

                    maxAzAccel = maxOf(maxAzAccel, abs(azAccel))
                    maxElAccel = maxOf(maxElAccel, abs(elAccel))
                }

                prevAzRate = currentAzRate
                prevElRate = currentElRate
            }
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
     * ⚠️ 주의: 이 함수는 analyzeTrainOptimization()에서 분석 및 비교용으로만 사용됩니다.
     * 실제 데이터 변환은 applyAxisTransformation + applyAngleLimitTransformation을 사용하세요.
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

