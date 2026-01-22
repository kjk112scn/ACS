package com.gtlsystems.acs_api.algorithm.axislimitangle

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.abs

/**
 * 축 제한 각도 계산 클래스
 * 0~360도 방위각을 포지셔너 ±270도 범위로 변환 (회전 방향성 보장)
 * 
 * ✅ Spring Bean으로 등록하여 의존성 주입 가능
 */
@Service
class LimitAngleCalculator {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 회전 방향 열거형
     */
    enum class RotationDirection {
        CLOCKWISE,          // 시계방향
        COUNTER_CLOCKWISE,  // 반시계방향
        MIXED,              // 혼합
        UNKNOWN             // 불명
    }

    /**
     * 경계 통과 상태 열거형
     */
    enum class BoundaryCrossing {
        WITHIN_RANGE,           // 범위 내
        EXCEEDS_270,            // 270° 초과
        CROSSES_270_BOUNDARY    // 270° 경계 통과
    }

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
     * 
     * ✅ PassSchedule 데이터 구조 리팩토링: "No" → "Index", UInt → Long
     */
    private fun convertDetailData(ephemerisTrackDtl: List<Map<String, Any?>>): List<Map<String, Any?>> {
        if (ephemerisTrackDtl.isEmpty()) return ephemerisTrackDtl

        val convertedData = mutableListOf<Map<String, Any?>>()

        // V006: (MstId, DetailId) 쌍으로 그룹화하여 패스별로 개별 처리
        // P2-1 수정 후 동일 위성의 모든 패스가 같은 MstId를 가지므로, DetailId로 패스 구분 필요
        val groupedByMstIdAndDetailId = ephemerisTrackDtl.groupBy { dtl ->
            val mstId = (dtl["MstId"] as? Number)?.toLong() ?: 0L
            val detailId = (dtl["DetailId"] as? Number)?.toInt() ?: 0
            Pair(mstId, detailId)
        }

        groupedByMstIdAndDetailId.forEach { (key, dtlList) ->
            val (mstId, detailId) = key
            logger.debug("MstId=$mstId, DetailId=$detailId 처리 중 - ${dtlList.size}개 데이터 포인트")

            val convertedGroup = convertAzimuthPath(dtlList)
            convertedData.addAll(convertedGroup)
        }

        return convertedData
    }

    /**
     * ✅ 회전 방향성을 보장하는 방위각 경로 변환
     * 
     * ✅ PassSchedule 데이터 구조 리팩토링: "No" → "Index", UInt → Long
     */
    private fun convertAzimuthPath(dtlList: List<Map<String, Any?>>): List<Map<String, Any?>> {
        if (dtlList.isEmpty()) return dtlList

        val convertedList = mutableListOf<Map<String, Any?>>()

        // Index 순서로 정렬 (시간 순서 보장)
        // ✅ PassSchedule 데이터 구조 리팩토링: "No" → "Index", UInt → Int
        val sortedList = dtlList.sortedBy { (it["Index"] as? Number)?.toInt() ?: 0 }
        val originalAzimuths = sortedList.map { it["Azimuth"] as Double }

        // ✅ 회전 방향성을 보장하는 변환
        val convertedAzimuths = convertWithRotationDirection(originalAzimuths)

        // 변환된 데이터 생성
        sortedList.forEachIndexed { index, dtlRecord ->
            val originalAzimuth = originalAzimuths[index]
            val convertedAzimuth = convertedAzimuths[index]

            val convertedRecord = dtlRecord.toMutableMap()
            convertedRecord["OriginalAzimuth"] = originalAzimuth
            convertedRecord["Azimuth"] = convertedAzimuth

            convertedList.add(convertedRecord)
        }

        // 변환 결과 로깅
        // ✅ PassSchedule 데이터 구조 리팩토링: UInt → Long
        val mstId = (dtlList.firstOrNull()?.get("MstId") as? Number)?.toLong() ?: 0L
        logger.info("MstId $mstId 변환 완료: ${originalAzimuths.size}개 포인트")
        logger.info("  원본 범위: ${String.format("%.2f", originalAzimuths.minOrNull() ?: 0.0)}° ~ ${String.format("%.2f", originalAzimuths.maxOrNull() ?: 0.0)}°")
        logger.info("  변환 범위: ${String.format("%.2f", convertedAzimuths.minOrNull() ?: 0.0)}° ~ ${String.format("%.2f", convertedAzimuths.maxOrNull() ?: 0.0)}°")

        // 변환 결과 연속성 검증
        val isContinuous = validateConversionContinuity(convertedAzimuths, originalAzimuths)
        if (!isContinuous) {
            logger.warn("⚠️ MstId $mstId 변환 결과 연속성 문제 감지 - 추가 검토 필요")
        }

        // 변환 품질 평가
        val qualityScore = calculateConversionQuality(convertedAzimuths, originalAzimuths)
        logger.info("MstId $mstId 변환 품질 점수: ${String.format("%.1f", qualityScore)}/100")

        return convertedList
    }

    /**
     * ✅ 회전 방향성을 보장하는 변환 (핵심 로직)
     */
    private fun convertWithRotationDirection(originalAzimuths: List<Double>): List<Double> {
        if (originalAzimuths.isEmpty()) return emptyList()

        val result = mutableListOf<Double>()

        // 1️⃣ 회전 방향 분석
        val rotationDirection = analyzeRotationDirection(originalAzimuths)
        logger.info("회전 방향 분석: $rotationDirection")

        // 2️⃣ 270° 경계 통과 여부 확인
        val crossesBoundary = checkBoundaryCrossing(originalAzimuths)
        logger.info("270° 경계 통과: $crossesBoundary")

        // 3️⃣ 시작 각도 결정 (회전 방향과 경계 통과를 고려)
        val firstAngle = originalAzimuths.first()
        val startAngle = determineStartAngle(firstAngle, originalAzimuths, rotationDirection, crossesBoundary)
        result.add(startAngle)

        logger.info("시작 각도 결정: ${String.format("%.2f", firstAngle)}° → ${String.format("%.2f", startAngle)}°")

        // 4️⃣ 나머지 각도들을 회전 방향을 유지하며 변환
        var boundaryCrossings = 0
        for (i in 1 until originalAzimuths.size) {
            val currentOriginal = originalAzimuths[i]
            val previousOriginal = originalAzimuths[i - 1]
            val previousConverted = result[i - 1]

            // 원본 데이터의 회전량 계산 (방향 고려)
            val rotationAmount = calculateRotationAmount(previousOriginal, currentOriginal, rotationDirection)

            // 이전 변환값에 동일한 회전량 적용
            val nextConverted = previousConverted + rotationAmount

            // ±270° 범위로 정규화 (방향성 유지)
            val normalizedAngle = normalizeWithDirectionPreservation(nextConverted, previousConverted, rotationDirection)

            result.add(normalizedAngle)

            // 경계 통과 지점 로깅
            if (abs(rotationAmount) > 180.0) {
                boundaryCrossings++
                logger.info("360°/0° 경계 통과 #{}: ${String.format("%.2f", currentOriginal)}° → ${String.format("%.2f", normalizedAngle)}° (회전량: ${String.format("%.2f", rotationAmount)}°)",
                    boundaryCrossings)
            }

            // 큰 점프 감지 및 로깅
            val actualRotation = abs(normalizedAngle - previousConverted)
            if (actualRotation > 100.0) {
                logger.warn("⚠️ 큰 회전 감지: 원본 ${String.format("%.2f", previousOriginal)}° → ${String.format("%.2f", currentOriginal)}°, 변환 ${String.format("%.2f", previousConverted)}° → ${String.format("%.2f", normalizedAngle)}° (회전량: ${String.format("%.2f", actualRotation)}°)")
            }
        }

        // 변환 결과 분석
        val convertedMin = result.minOrNull() ?: 0.0
        val convertedMax = result.maxOrNull() ?: 0.0
        logger.info("변환 완료: ${String.format("%.2f", firstAngle)}° → ${String.format("%.2f", result.first())}° ~ ${String.format("%.2f", originalAzimuths.last())}° → ${String.format("%.2f", result.last())}°")
        logger.info("변환 범위: ${String.format("%.2f", convertedMin)}° ~ ${String.format("%.2f", convertedMax)}°")
        logger.info("경계 통과 횟수: ${boundaryCrossings}회")

        return result
    }

    /**
     * ✅ 회전 방향 분석
     */
    private fun analyzeRotationDirection(azimuths: List<Double>): RotationDirection {
        if (azimuths.size < 2) return RotationDirection.UNKNOWN

        var clockwiseCount = 0
        var counterClockwiseCount = 0

        for (i in 1 until azimuths.size) {
            val prev = azimuths[i - 1]
            val current = azimuths[i]

            val rawDelta = current - prev
            val normalizedDelta = when {
                rawDelta > 180.0 -> rawDelta - 360.0  // 360°/0° 경계 통과 (반시계방향)
                rawDelta < -180.0 -> rawDelta + 360.0 // 0°/360° 경계 통과 (시계방향)
                else -> rawDelta
            }

            when {
                normalizedDelta > 0 -> clockwiseCount++
                normalizedDelta < 0 -> counterClockwiseCount++
            }
        }

        return when {
            clockwiseCount > counterClockwiseCount -> RotationDirection.CLOCKWISE
            counterClockwiseCount > clockwiseCount -> RotationDirection.COUNTER_CLOCKWISE
            else -> RotationDirection.MIXED
        }
    }

    /**
     * ✅ 270° 경계 통과 확인
     */
    private fun checkBoundaryCrossing(azimuths: List<Double>): BoundaryCrossing {
        val minAngle = azimuths.minOrNull() ?: 0.0
        val maxAngle = azimuths.maxOrNull() ?: 0.0

        return when {
            maxAngle > 270.0 && minAngle < 90.0 -> BoundaryCrossing.CROSSES_270_BOUNDARY
            maxAngle > 270.0 -> BoundaryCrossing.EXCEEDS_270
            else -> BoundaryCrossing.WITHIN_RANGE
        }
    }

    /**
     * ✅ 시작 각도 결정 (회전 방향과 경계 통과 고려)
     */
    private fun determineStartAngle(
        firstAngle: Double,
        allAzimuths: List<Double>,
        direction: RotationDirection,
        crossing: BoundaryCrossing
    ): Double {

        when (crossing) {
            BoundaryCrossing.WITHIN_RANGE -> {
                // 270° 범위 내에 있으면 변환 불필요
                logger.info("패스가 0°~270° 범위 내에 있음: 변환 불필요")
                return firstAngle
            }

            BoundaryCrossing.EXCEEDS_270 -> {
                // 270° 초과하는 경우 음수 영역으로 이동
                logger.info("270° 초과 패스: 음수 영역으로 변환")
                return firstAngle - 360.0
            }

            BoundaryCrossing.CROSSES_270_BOUNDARY -> {
                // 270° 경계를 넘나드는 경우
                val lastAngle = allAzimuths.last()

                when (direction) {
                    RotationDirection.CLOCKWISE -> {
                        // 시계방향: 180° → 270° → 360° → 30° 패턴
                        // 시작을 음수로 하여 연속성 보장: -180° → -90° → 0° → 30°
                        if (firstAngle >= 180.0) {
                            logger.info("시계방향 270° 경계 통과: 시작각도를 음수로 변환")
                            return firstAngle - 360.0
                        }
                        return firstAngle
                    }

                    RotationDirection.COUNTER_CLOCKWISE -> {
                        // 반시계방향: 30° → 360° → 270° → 180° 패턴
                        if (lastAngle >= 180.0) {
                            logger.info("반시계방향 270° 경계 통과: 기본 변환")
                            return firstAngle
                        }
                        return firstAngle
                    }

                    else -> {
                        // 혼합 또는 불명확한 경우 기본 로직
                        logger.info("혼합/불명 방향: 기본 변환 로직 적용")
                        return if (firstAngle >= 180.0) firstAngle - 360.0 else firstAngle
                    }
                }
            }
        }
    }

    /**
     * ✅ 회전량 계산 (방향 고려)
     */
    private fun calculateRotationAmount(fromAngle: Double, toAngle: Double, direction: RotationDirection): Double {
        val rawDelta = toAngle - fromAngle

        // 360°/0° 경계 통과 보정
        val correctedDelta = when {
            rawDelta > 180.0 -> rawDelta - 360.0   // 반시계방향 경계 통과
            rawDelta < -180.0 -> rawDelta + 360.0  // 시계방향 경계 통과
            else -> rawDelta
        }

        return correctedDelta
    }

    /**
     * ✅ 방향성을 유지하는 정규화
     */
    private fun normalizeWithDirectionPreservation(
        angle: Double,
        previousAngle: Double,
        direction: RotationDirection
    ): Double {
        var normalized = angle

        // 기본 ±270° 범위 정규화
        while (normalized > 270.0) normalized -= 360.0
        while (normalized < -270.0) normalized += 360.0

        // 방향성 검증 및 보정
        val actualDelta = normalized - previousAngle

        // 비정상적인 방향 전환 감지 및 보정
        if (abs(actualDelta) > 300.0) {
            logger.debug("방향성 보정 필요: ${String.format("%.2f", previousAngle)}° → ${String.format("%.2f", normalized)}°")

            // 대안 각도 계산
            val alternative1 = normalized + 360.0
            val alternative2 = normalized - 360.0

            val candidates = listOf(normalized, alternative1, alternative2)
                .filter { it >= -270.0 && it <= 270.0 }

            if (candidates.isNotEmpty()) {
                val bestCandidate = candidates.minByOrNull { abs(it - previousAngle) }
                if (bestCandidate != null && abs(bestCandidate - previousAngle) < abs(actualDelta)) {
                    logger.debug("방향성 보정 적용: ${String.format("%.2f", normalized)}° → ${String.format("%.2f", bestCandidate)}°")
                    normalized = bestCandidate
                }
            }
        }

        // 최종 범위 검증
        if (normalized < -270.0 || normalized > 270.0) {
            logger.error("최종 검증 실패: ${String.format("%.2f", normalized)}° - 클램핑 적용")
            normalized = normalized.coerceIn(-270.0, 270.0)
        }

        return normalized
    }

    /**
     * ✅ 변환 후 연속성 검증 메서드
     */
    private fun validateConversionContinuity(convertedAzimuths: List<Double>, originalAzimuths: List<Double>): Boolean {
        if (convertedAzimuths.size != originalAzimuths.size || convertedAzimuths.size < 2) return false

        var continuityIssues = 0
        val totalPoints = convertedAzimuths.size - 1

        for (i in 1 until convertedAzimuths.size) {
            val originalDelta = calculateAngleDelta(originalAzimuths[i-1], originalAzimuths[i])
            val convertedDelta = convertedAzimuths[i] - convertedAzimuths[i-1]

            // 변화량의 차이가 5° 이상이면 연속성 문제
            val deltaError = abs(abs(originalDelta) - abs(convertedDelta))
            if (deltaError > 5.0) {
                continuityIssues++
                logger.debug("연속성 이슈 #{}: 원본Δ=${String.format("%.2f", originalDelta)}°, 변환Δ=${String.format("%.2f", convertedDelta)}°, 오차=${String.format("%.2f", deltaError)}°",
                    continuityIssues)
            }
        }

        val continuityRate = (totalPoints - continuityIssues).toDouble() / totalPoints
        logger.debug("연속성 검증: ${String.format("%.1f", continuityRate * 100)}% (${totalPoints - continuityIssues}/${totalPoints})")

        return continuityRate >= 0.95 // 95% 이상 연속성 유지
    }

    /**
     * ✅ 변환 품질 평가 메서드
     */
    private fun calculateConversionQuality(convertedAzimuths: List<Double>, originalAzimuths: List<Double>): Double {
        if (convertedAzimuths.size != originalAzimuths.size || convertedAzimuths.size < 2) return 0.0

        // 1. 범위 준수 검사 (30점)
        val outOfRangeCount = convertedAzimuths.count { it < -270.0 || it > 270.0 }
        val rangeScore = maxOf(0.0, 30.0 - (outOfRangeCount * 5.0))

        // 2. 연속성 검사 (40점)
        var continuityIssues = 0
        for (i in 1 until convertedAzimuths.size) {
            val jump = abs(convertedAzimuths[i] - convertedAzimuths[i-1])
            if (jump > 10.0) { // 10° 이상 점프를 연속성 문제로 간주
                val originalJump = abs(calculateAngleDelta(originalAzimuths[i-1], originalAzimuths[i]))
                if (originalJump < 10.0) { // 원본에서는 작은 변화였는데 변환에서 큰 점프
                    continuityIssues++
                }
            }
        }
        val continuityScore = maxOf(0.0, 40.0 - (continuityIssues * 5.0))

        // 3. 변화량 보존 검사 (30점)
        var deltaPreservationScore = 30.0
        for (i in 1 until convertedAzimuths.size) {
            val originalDelta = calculateAngleDelta(originalAzimuths[i-1], originalAzimuths[i])
            val convertedDelta = convertedAzimuths[i] - convertedAzimuths[i-1]
            val deltaError = abs(abs(originalDelta) - abs(convertedDelta))
            if (deltaError > 5.0) {
                deltaPreservationScore -= 2.0
            }
        }
        deltaPreservationScore = maxOf(0.0, deltaPreservationScore)

        val qualityScore = rangeScore + continuityScore + deltaPreservationScore

        logger.debug("품질 평가: 범위=${String.format("%.1f", rangeScore)}, 연속성=${String.format("%.1f", continuityScore)}, 변화량보존=${String.format("%.1f", deltaPreservationScore)}, 총점=${String.format("%.1f", qualityScore)}")

        return qualityScore
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
            // ✅ "No" → "Index" 변경 (V006 리팩토링)
            val relatedDtlData = convertedDtlData.filter { it["MstId"] == mstId }
                .sortedBy { (it["Index"] as? Number)?.toInt() ?: 0 }

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
     * ✅ 개선된 변환 결과 검증 (범위 검증 강화)
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
        // ✅ V006: (MstId, DetailId) 쌍으로 그룹화하여 패스별 개별 검증
        convertedDtl.groupBy { dtl ->
            val mstId = (dtl["MstId"] as? Number)?.toLong() ?: 0L
            val detailId = (dtl["DetailId"] as? Number)?.toInt() ?: 0
            Pair(mstId, detailId)
        }.forEach { (key, dtlList) ->
            val (mstId, detailId) = key
            val sortedList = dtlList.sortedBy { (it["Index"] as? Number)?.toInt() ?: 0 }

            sortedList.forEach { point ->
                val azimuth = point["Azimuth"] as Double

                // ✅ 강화된 범위 체크
                if (azimuth < -270.0 || azimuth > 270.0) {
                    outOfRangeCount++
                    issues.add("MstId=$mstId, DetailId=$detailId: 방위각 범위 초과 ${String.format("%.2f", azimuth)}°")
                    logger.error("범위 초과 감지: MstId=$mstId, DetailId=$detailId, 방위각 ${String.format("%.2f", azimuth)}°")
                }
            }

            // 연속성 검증
            for (i in 1 until sortedList.size) {
                val prevAz = sortedList[i-1]["Azimuth"] as Double
                val currentAz = sortedList[i]["Azimuth"] as Double
                val originalPrev = sortedList[i-1]["OriginalAzimuth"] as? Double
                val originalCurrent = sortedList[i]["OriginalAzimuth"] as? Double

                val jump = abs(currentAz - prevAz)
                maxJump = maxOf(maxJump, jump)

                if (jump > 5.0) {
                    val isBoundary = isBoundaryCrossing(prevAz, currentAz, originalPrev, originalCurrent)

                    if (isBoundary) {
                        totalBoundaryCrossings++
                        logger.debug("MstId=$mstId, DetailId=$detailId: 경계 통과 - ${String.format("%.2f", prevAz)}° → ${String.format("%.2f", currentAz)}°")
                    } else {
                        issues.add("MstId=$mstId, DetailId=$detailId: 비정상적인 각도 점프 ${String.format("%.2f", jump)}° (${String.format("%.2f", prevAz)}° → ${String.format("%.2f", currentAz)}°)")
                    }
                }
            }
        }

        // ✅ 마스터 데이터 검증 강화
        convertedMst.forEach { mstRecord ->
            val mstId = mstRecord["No"] as UInt
            val startAz = mstRecord["StartAzimuth"] as? Double
            val endAz = mstRecord["EndAzimuth"] as? Double

            if (startAz != null && (startAz < -270.0 || startAz > 270.0)) {
                issues.add("MstId $mstId: 시작 방위각 범위 초과 ${String.format("%.2f", startAz)}°")
                logger.error("마스터 데이터 범위 초과: MstId $mstId, 시작 방위각 ${String.format("%.2f", startAz)}°")
            }

            if (endAz != null && (endAz < -270.0 || endAz > 270.0)) {
                issues.add("MstId $mstId: 종료 방위각 범위 초과 ${String.format("%.2f", endAz)}°")
                logger.error("마스터 데이터 범위 초과: MstId $mstId, 종료 방위각 ${String.format("%.2f", endAz)}°")
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
     * ✅ 개선된 경계 통과 감지 메서드
     */
    private fun isBoundaryCrossing(prevAz: Double, currentAz: Double, originalPrev: Double?, originalCurrent: Double?): Boolean {
        // 1. 원본 데이터에서 경계 통과 확인
        val originalBoundaryCrossing = if (originalPrev != null && originalCurrent != null) {
            abs(originalPrev - originalCurrent) > 180.0
        } else false

        // 2. 변환된 데이터에서 경계 통과 패턴 확인
        val convertedJump = abs(currentAz - prevAz)

        // 3. 경계 통과 패턴들
        val isPositiveToNegativeCrossing = prevAz > 180.0 && currentAz < -180.0  // 270° → -90° 같은 경우
        val isNegativeToPositiveCrossing = prevAz < -180.0 && currentAz > 180.0  // -270° → 90° 같은 경우
        val isLargeJumpWithBoundary = convertedJump > 300.0  // 300° 이상의 큰 점프

        // 4. 연속성 기반 경계 통과 판단
        val isContinuityPreservingJump = originalBoundaryCrossing && convertedJump > 180.0

        return originalBoundaryCrossing || isPositiveToNegativeCrossing || isNegativeToPositiveCrossing ||
                isLargeJumpWithBoundary || isContinuityPreservingJump
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
        // ✅ "No" → "Index" 변경 (V006 리팩토링)
        val passDetails = convertedDtl.filter { it["MstId"] == mstId }
            .sortedBy { (it["Index"] as? Number)?.toInt() ?: 0 }

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
            logger.info("  #{}: ${String.format("%.2f", original ?: 0.0)}° → ${String.format("%.2f", converted ?: 0.0)}°",
                index + 1)
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
                logger.info("  #{}: ${String.format("%.2f", original ?: 0.0)}° → ${String.format("%.2f", converted ?: 0.0)}°",
                    actualIndex)
            }
        }
    }
}

