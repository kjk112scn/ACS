package com.gtlsystems.acs_api.algorithm.axislimitangle

import org.slf4j.LoggerFactory
import kotlin.math.abs

/**
 * 축 제한 각도 계산 클래스
 * 0~360도 방위각을 포지셔너 ±270도 범위로 변환 (연속성 보장)
 */
class LimitAngleCalculator {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 마스터 데이터와 세부 데이터를 입력받아 ±270도 범위로 변환된 결과를 리턴
     */
    fun convertTrackingData(
        ephemerisTrackMst: List<Map<String, Any?>>,
        ephemerisTrackDtl: List<Map<String, Any?>>
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {

        logger.info("포지셔너 각도 변환 시작 - 마스터: ${ephemerisTrackMst.size}개, 세부: ${ephemerisTrackDtl.size}개")

        // 세부 데이터 변환
        val convertedDtlData = convertDetailData(ephemerisTrackDtl)

        // 마스터 데이터 변환 (세부 데이터 기반으로 StartAzimuth, EndAzimuth 계산)
        val convertedMstData = convertMasterData(ephemerisTrackMst, convertedDtlData)

        logger.info("포지셔너 각도 변환 완료")

        return Pair(convertedMstData, convertedDtlData)
    }

    /**
     * 세부 추적 데이터의 방위각을 ±270도 범위로 변환
     */
    private fun convertDetailData(ephemerisTrackDtl: List<Map<String, Any?>>): List<Map<String, Any?>> {
        if (ephemerisTrackDtl.isEmpty()) return ephemerisTrackDtl

        val convertedData = mutableListOf<Map<String, Any?>>()

        // MstId별로 그룹화하여 처리
        val groupedByMstId = ephemerisTrackDtl.groupBy { it["MstId"] as UInt }

        groupedByMstId.forEach { (mstId, dtlList) ->
            logger.debug("MstId $mstId 처리 중 - ${dtlList.size}개 데이터 포인트")

            val convertedGroup = convertAzimuthPath(dtlList)
            convertedData.addAll(convertedGroup)
        }

        return convertedData
    }

    /**
     * 연속된 방위각 경로를 ±270도 범위로 변환 (연속성 보장)
     */
    private fun convertAzimuthPath(dtlList: List<Map<String, Any?>>): List<Map<String, Any?>> {
        if (dtlList.isEmpty()) return dtlList

        val convertedList = mutableListOf<Map<String, Any?>>()

        // No 순서로 정렬 (시간 순서 보장)
        val sortedList = dtlList.sortedBy { it["No"] as UInt }
        val originalAzimuths = sortedList.map { it["Azimuth"] as Double }

        // ✅ 연속성을 보장하는 변환
        val convertedAzimuths = convertContinuousPath(originalAzimuths)

        // 변환된 데이터 생성
        sortedList.forEachIndexed { index, dtlRecord ->
            val originalAzimuth = originalAzimuths[index]
            val convertedAzimuth = convertedAzimuths[index]

            val convertedRecord = dtlRecord.toMutableMap()
            convertedRecord["OriginalAzimuth"] = originalAzimuth
            convertedRecord["Azimuth"] = convertedAzimuth

            convertedList.add(convertedRecord)
        }

        // ✅ 변환 결과 요약 로깅
        val mstId = dtlList.firstOrNull()?.get("MstId") as? UInt ?: 0u
        logger.info("MstId $mstId 변환 완료: ${originalAzimuths.size}개 포인트")
        logger.info("  원본 범위: ${String.format("%.2f", originalAzimuths.minOrNull() ?: 0.0)}° ~ ${String.format("%.2f", originalAzimuths.maxOrNull() ?: 0.0)}°")
        logger.info("  변환 범위: ${String.format("%.2f", convertedAzimuths.minOrNull() ?: 0.0)}° ~ ${String.format("%.2f", convertedAzimuths.maxOrNull() ?: 0.0)}°")

        return convertedList
    }

    /**
     * ✅ 완전히 개선된 연속성 기반 변환 (270° 경계 문제 완전 해결)
     */
    private fun convertContinuousPath(originalAzimuths: List<Double>): List<Double> {
        if (originalAzimuths.isEmpty()) return emptyList()

        // 🔍 패스 특성 분석
        val firstAngle = originalAzimuths.first()
        val lastAngle = originalAzimuths.last()
        val minAngle = originalAzimuths.minOrNull() ?: 0.0
        val maxAngle = originalAzimuths.maxOrNull() ?: 0.0
        val over180Count = originalAzimuths.count { it >= 180.0 }

        logger.info("패스 분석: ${String.format("%.2f", firstAngle)}° → ${String.format("%.2f", lastAngle)}° (범위: ${String.format("%.2f", minAngle)}° ~ ${String.format("%.2f", maxAngle)}°)")
        logger.info("180° 이상 각도: ${over180Count}/${originalAzimuths.size}개 (${String.format("%.1f", over180Count * 100.0 / originalAzimuths.size)}%)")

        // 🔍 패스 유형 분류
        val passType = when {
            maxAngle >= 270.0 -> "고각도 패스 (270°+)"
            over180Count > originalAzimuths.size * 0.6 -> "후반부 집중 패스"
            minAngle < 90.0 && maxAngle > 270.0 -> "270° 경계 패스"
            else -> "일반 패스"
        }
        logger.info("패스 유형: $passType")

        val result = mutableListOf<Double>()

        // ✅ 핵심 수정: 첫 번째 각도 변환 로직 개선
        val firstConverted = determineFirstAngleConversion(firstAngle, minAngle, maxAngle)
        result.add(firstConverted)

        logger.info("변환 시작: ${String.format("%.2f", firstAngle)}° → ${String.format("%.2f", firstConverted)}°")

        // 나머지 각도들을 연속성을 유지하며 변환
        var boundaryCrossings = 0
        for (i in 1 until originalAzimuths.size) {
            val currentOriginal = originalAzimuths[i]
            val previousOriginal = originalAzimuths[i - 1]
            val previousConverted = result[i - 1]

            // 원본 데이터의 변화량 계산 (360°/0° 경계 고려)
            val originalDelta = calculateAngleDelta(previousOriginal, currentOriginal)

            // 이전 변환값에 동일한 변화량 적용
            val expectedConverted = previousConverted + originalDelta

            // ✅ 스마트 정규화 (270° 경계 특별 처리)
            val finalConverted = smartNormalizeFor270Boundary(expectedConverted, previousConverted, currentOriginal)

            result.add(finalConverted)

            // 경계 통과 지점 로깅
            if (abs(originalDelta) > 180.0) {
                boundaryCrossings++
                logger.info("360°/0° 경계 통과 #{}: ${String.format("%.2f", currentOriginal)}° → ${String.format("%.2f", finalConverted)}° (Δ${String.format("%.2f", originalDelta)}°)",
                    boundaryCrossings)
            }

            // 큰 점프 감지
            val actualJump = abs(finalConverted - previousConverted)
            if (actualJump > 100.0) {
                logger.warn("⚠️ 큰 점프 감지: 원본 ${String.format("%.2f", previousOriginal)}° → ${String.format("%.2f", currentOriginal)}°, 변환 ${String.format("%.2f", previousConverted)}° → ${String.format("%.2f", finalConverted)}° (점프: ${String.format("%.2f", actualJump)}°)")
            }
        }

        // 🔍 변환 결과 분석
        val convertedMin = result.minOrNull() ?: 0.0
        val convertedMax = result.maxOrNull() ?: 0.0
        logger.info("변환 완료: ${String.format("%.2f", firstAngle)}° → ${String.format("%.2f", result.first())}° ~ ${String.format("%.2f", lastAngle)}° → ${String.format("%.2f", result.last())}°")
        logger.info("변환 범위: ${String.format("%.2f", convertedMin)}° ~ ${String.format("%.2f", convertedMax)}°")
        logger.info("경계 통과 횟수: ${boundaryCrossings}회")

        return result
    }

    /**
     * ✅ 첫 번째 각도 변환 결정 (패스 전체 범위 고려)
     */
    private fun determineFirstAngleConversion(firstAngle: Double, minAngle: Double, maxAngle: Double): Double {
        // 1️⃣ 패스가 270°를 넘는 경우 음수 영역으로 변환
        if (maxAngle > 270.0) {
            // 시작 각도가 180° 이상이면 음수로 변환
            if (firstAngle >= 180.0) {
                val converted = firstAngle - 360.0
                logger.info("270° 초과 패스 감지: 시작 각도를 음수 영역으로 변환 (${String.format("%.2f", firstAngle)}° → ${String.format("%.2f", converted)}°)")
                return converted
            }
        }

        // 2️⃣ 일반적인 경우: 270° 이하는 그대로, 초과는 음수로
        return if (firstAngle <= 270.0) {
            firstAngle
        } else {
            firstAngle - 360.0
        }
    }

    /**
     * ✅ 270° 경계 특별 처리 정규화 (개선된 버전)
     */
    private fun smartNormalizeFor270Boundary(expectedAngle: Double, previousAngle: Double, originalAngle: Double): Double {
        // 1️⃣ 기본 정규화
        var normalized = expectedAngle
        while (normalized > 270.0) normalized -= 360.0
        while (normalized < -270.0) normalized += 360.0

        // 2️⃣ 연속성 체크: 이전 각도와의 차이가 비정상적으로 크면 보정
        val jumpSize = abs(normalized - previousAngle)
        if (jumpSize > 300.0) {
            logger.debug("연속성 문제 감지: 이전=${String.format("%.2f", previousAngle)}°, 현재=${String.format("%.2f", normalized)}°, 점프=${String.format("%.2f", jumpSize)}°")

            // 3️⃣ 대안 각도 시도
            val alternative1 = normalized + 360.0
            val alternative2 = normalized - 360.0

            val candidates = listOf(normalized, alternative1, alternative2)
                .filter { it >= -270.0 && it <= 270.0 }

            if (candidates.isNotEmpty()) {
                val bestCandidate = candidates.minByOrNull { abs(it - previousAngle) }
                if (bestCandidate != null && abs(bestCandidate - previousAngle) < jumpSize) {
                    logger.debug("연속성 보정 적용: ${String.format("%.2f", normalized)}° → ${String.format("%.2f", bestCandidate)}°")
                    normalized = bestCandidate
                }
            }
        }

        return normalized
    }

    /**
     * ✅ 각도 변화량 계산 (360°/0° 경계 고려)
     */
    private fun calculateAngleDelta(fromAngle: Double, toAngle: Double): Double {
        var delta = toAngle - fromAngle

        // 360°/0° 경계를 넘는 경우 보정
        when {
            delta > 180.0 -> delta -= 360.0    // 예: 359° → 1° = -358° → +2°
            delta < -180.0 -> delta += 360.0   // 예: 1° → 359° = +358° → -2°
        }

        return delta
    }

    /**
     * ✅ ±270° 범위로 정규화
     */
    private fun normalizeToRange(angle: Double): Double {
        var normalized = angle

        while (normalized > 270.0) {
            normalized -= 360.0
        }
        while (normalized < -270.0) {
            normalized += 360.0
        }

        return normalized
    }

    /**
     * 마스터 데이터의 StartAzimuth, EndAzimuth를 변환된 세부 데이터 기반으로 업데이트
     */
    private fun convertMasterData(
        ephemerisTrackMst: List<Map<String, Any?>>,
        convertedDtlData: List<Map<String, Any?>>
    ): List<Map<String, Any?>> {

        val convertedMstData = mutableListOf<Map<String, Any?>>()

        ephemerisTrackMst.forEach { mstRecord ->
            val mstId = mstRecord["No"] as UInt

            // 해당 MstId의 세부 데이터 찾기
            val relatedDtlData = convertedDtlData.filter { it["MstId"] == mstId }
                .sortedBy { it["No"] as UInt }

            if (relatedDtlData.isNotEmpty()) {
                // 첫 번째와 마지막 방위각 추출
                val startAzimuth = relatedDtlData.first()["Azimuth"] as Double
                val endAzimuth = relatedDtlData.last()["Azimuth"] as Double

                // 원본 값 보존하면서 새로운 값으로 업데이트
                val updatedMstRecord = mstRecord.toMutableMap()
                updatedMstRecord["OriginalStartAzimuth"] = mstRecord["StartAzimuth"]
                updatedMstRecord["OriginalEndAzimuth"] = mstRecord["EndAzimuth"]
                updatedMstRecord["StartAzimuth"] = startAzimuth
                updatedMstRecord["EndAzimuth"] = endAzimuth

                convertedMstData.add(updatedMstRecord)

                logger.debug("MstId $mstId - 원본: ${mstRecord["StartAzimuth"]}°~${mstRecord["EndAzimuth"]}°, " +
                        "변환: ${String.format("%.2f", startAzimuth)}°~${String.format("%.2f", endAzimuth)}°")
            } else {
                convertedMstData.add(mstRecord)
                logger.warn("MstId $mstId 에 해당하는 세부 데이터가 없습니다.")
            }
        }

        return convertedMstData
    }

    /**
     * ✅ 개선된 변환 결과 검증
     */
    fun validateConversion(
        originalMst: List<Map<String, Any?>>,
        originalDtl: List<Map<String, Any?>>,
        convertedMst: List<Map<String, Any?>>,
        convertedDtl: List<Map<String, Any?>>
    ): ValidationResult {

        val issues = mutableListOf<String>()
        var outOfRangeCount = 0
        var maxJump = 0.0
        var totalBoundaryCrossings = 0

        // 세부 데이터 검증
        convertedDtl.groupBy { it["MstId"] as UInt }.forEach { (mstId, dtlList) ->
            val sortedList = dtlList.sortedBy { it["No"] as UInt }

            for (i in 1 until sortedList.size) {
                val prevAz = sortedList[i-1]["Azimuth"] as Double
                val currentAz = sortedList[i]["Azimuth"] as Double

                // 범위 체크
                if (currentAz < -270.0 || currentAz > 270.0) {
                    outOfRangeCount++
                    issues.add("MstId $mstId: 방위각 범위 초과 ${String.format("%.2f", currentAz)}°")
                }

                // ✅ 개선된 점프 체크 (연속성 기반)
                val jump = abs(currentAz - prevAz)
                maxJump = maxOf(maxJump, jump)

                // ✅ 임계값 조정: 30° → 5° (연속성이 보장되어야 함)
                if (jump > 5.0) {
                    // ✅ 360°/0° 경계 통과인지 확인
                    val originalPrev = sortedList[i-1]["OriginalAzimuth"] as? Double
                    val originalCurrent = sortedList[i]["OriginalAzimuth"] as? Double

                    val isBoundaryCrossing = if (originalPrev != null && originalCurrent != null) {
                        abs(originalPrev - originalCurrent) > 180.0
                    } else false

                    if (isBoundaryCrossing) {
                        // 경계 통과는 정상 - INFO 레벨로 기록
                        totalBoundaryCrossings++
                        logger.info("MstId $mstId: 360°/0° 경계 통과 - 원본: ${String.format("%.2f", originalPrev ?: 0.0)}° → ${String.format("%.2f", originalCurrent ?: 0.0)}°, 변환: ${String.format("%.2f", prevAz)}° → ${String.format("%.2f", currentAz)}°")
                    } else {
                        // 실제 비정상적인 점프
                        issues.add("MstId $mstId: 비정상적인 각도 점프 ${String.format("%.2f", jump)}° (${String.format("%.2f", prevAz)}° → ${String.format("%.2f", currentAz)}°)")
                    }
                }
            }
        }

        // ✅ 마스터 데이터 검증
        convertedMst.forEach { mstRecord ->
            val mstId = mstRecord["No"] as UInt
            val startAz = mstRecord["StartAzimuth"] as? Double
            val endAz = mstRecord["EndAzimuth"] as? Double

            if (startAz != null && (startAz < -270.0 || startAz > 270.0)) {
                issues.add("MstId $mstId: 시작 방위각 범위 초과 ${String.format("%.2f", startAz)}°")
            }

            if (endAz != null && (endAz < -270.0 || endAz > 270.0)) {
                issues.add("MstId $mstId: 종료 방위각 범위 초과 ${String.format("%.2f", endAz)}°")
            }
        }

        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            outOfRangeCount = outOfRangeCount,
            maxJump = maxJump,
            boundaryCrossings = totalBoundaryCrossings
        )
    }

    /**
     * ✅ 확장된 검증 결과 데이터 클래스
     */
    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<String>,
        val outOfRangeCount: Int,
        val maxJump: Double,
        val boundaryCrossings: Int = 0
    ) {
        /**
         * 검증 결과 요약 출력
         */
        fun getSummary(): String {
            return buildString {
                appendLine("=== 변환 검증 결과 ===")
                appendLine("✅ 검증 상태: ${if (isValid) "성공" else "실패"}")
                appendLine("📊 범위 초과: ${outOfRangeCount}개")
                appendLine("📈 최대 점프: ${String.format("%.2f", maxJump)}°")
                appendLine("🔄 경계 통과: ${boundaryCrossings}회")

                if (issues.isNotEmpty()) {
                    appendLine("⚠️ 발견된 문제:")
                    issues.forEach { issue ->
                        appendLine("  - $issue")
                    }
                }
            }
        }
    }

    /**
     * ✅ 변환 통계 정보 제공
     */
    fun getConversionStatistics(
        originalDtl: List<Map<String, Any?>>,
        convertedDtl: List<Map<String, Any?>>
    ): ConversionStatistics {

        val originalAngles = originalDtl.mapNotNull { it["Azimuth"] as? Double }
        val convertedAngles = convertedDtl.mapNotNull { it["Azimuth"] as? Double }

        val originalRange = if (originalAngles.isNotEmpty()) {
            (originalAngles.maxOrNull() ?: 0.0) - (originalAngles.minOrNull() ?: 0.0)
        } else 0.0

        val convertedRange = if (convertedAngles.isNotEmpty()) {
            (convertedAngles.maxOrNull() ?: 0.0) - (convertedAngles.minOrNull() ?: 0.0)
        } else 0.0

        // 경계 통과 횟수 계산
        var boundaryCrossings = 0
        for (i in 1 until originalAngles.size) {
            if (abs(originalAngles[i] - originalAngles[i-1]) > 180.0) {
                boundaryCrossings++
            }
        }

        return ConversionStatistics(
            totalPoints = originalAngles.size,
            originalMinAngle = originalAngles.minOrNull() ?: 0.0,
            originalMaxAngle = originalAngles.maxOrNull() ?: 0.0,
            originalRange = originalRange,
            convertedMinAngle = convertedAngles.minOrNull() ?: 0.0,
            convertedMaxAngle = convertedAngles.maxOrNull() ?: 0.0,
            convertedRange = convertedRange,
            boundaryCrossings = boundaryCrossings
        )
    }

    /**
     * ✅ 변환 통계 데이터 클래스
     */
    data class ConversionStatistics(
        val totalPoints: Int,
        val originalMinAngle: Double,
        val originalMaxAngle: Double,
        val originalRange: Double,
        val convertedMinAngle: Double,
        val convertedMaxAngle: Double,
        val convertedRange: Double,
        val boundaryCrossings: Int
    ) {
        /**
         * 통계 요약 출력
         */
        fun getSummary(): String {
            return buildString {
                appendLine("=== 변환 통계 ===")
                appendLine("📊 총 데이터 포인트: ${totalPoints}개")
                appendLine("📐 원본 범위: ${String.format("%.2f", originalMinAngle)}° ~ ${String.format("%.2f", originalMaxAngle)}° (${String.format("%.2f", originalRange)}°)")
                appendLine("🔄 변환 범위: ${String.format("%.2f", convertedMinAngle)}° ~ ${String.format("%.2f", convertedMaxAngle)}° (${String.format("%.2f", convertedRange)}°)")
                appendLine("🌐 경계 통과: ${boundaryCrossings}회")

                val compressionRatio = if (originalRange > 0) convertedRange / originalRange else 1.0
                appendLine("📉 범위 압축률: ${String.format("%.2f", compressionRatio * 100)}%")
            }
        }
    }

    /**
     * ✅ 디버깅용 상세 변환 정보 출력
     */
    fun debugConversionDetails(
        convertedDtl: List<Map<String, Any?>>,
        mstId: UInt,
        maxPoints: Int = 10
    ) {
        val passDetails = convertedDtl.filter { it["MstId"] == mstId }
            .sortedBy { it["No"] as UInt }

        if (passDetails.isEmpty()) {
            logger.warn("MstId $mstId 에 해당하는 데이터가 없습니다.")
            return
        }

        logger.info("=== MstId $mstId 변환 상세 정보 ===")
        logger.info("총 ${passDetails.size}개 포인트")

        // 처음과 마지막 몇 개 포인트만 출력
        val pointsToShow = minOf(maxPoints, passDetails.size)

        logger.info("처음 $pointsToShow 개 포인트:")
        passDetails.take(pointsToShow).forEachIndexed { index, point ->
            val original = point["OriginalAzimuth"] as? Double
            val converted = point["Azimuth"] as? Double
            logger.info("  #{}: {:.2f}° → {:.2f}°",
                index + 1, original ?: 0.0, converted ?: 0.0)
        }

        if (passDetails.size > pointsToShow * 2) {
            logger.info("... (중간 ${passDetails.size - pointsToShow * 2}개 포인트 생략) ...")
        }

        if (passDetails.size > pointsToShow) {
            logger.info("마지막 $pointsToShow 개 포인트:")
            passDetails.takeLast(pointsToShow).forEachIndexed { index, point ->
                val original = point["OriginalAzimuth"] as? Double
                val converted = point["Azimuth"] as? Double
                val actualIndex = passDetails.size - pointsToShow + index + 1
                logger.info("  #{}: {:.2f}° → {:.2f}°",
                    actualIndex, original ?: 0.0, converted ?: 0.0)
            }
        }
    }
}
