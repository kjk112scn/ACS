package com.gtlsystems.acs_api.algorithm.axislimitangle

import org.slf4j.LoggerFactory
import kotlin.math.abs

/**
 * 축 제한 각도 계산 클래스
 * 0~360도 방위각을 포지셔너 ±270도 범위로 변환
 */
class limitAngleCalcualte {

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
     * 연속된 방위각 경로를 ±270도 범위로 변환
     */
    private fun convertAzimuthPath(dtlList: List<Map<String, Any?>>): List<Map<String, Any?>> {
        if (dtlList.isEmpty()) return dtlList

        val convertedList = mutableListOf<Map<String, Any?>>()

        // No 순서로 정렬 (시간 순서 보장)
        val sortedList = dtlList.sortedBy { it["No"] as UInt }
        val originalAzimuths = sortedList.map { it["Azimuth"] as Double }

        // ✅ 포지셔너 방향성을 고려한 연속 경로 변환
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

        return convertedList
    }

    /**
     * ✅ 각 패스별 최적 오프셋 결정
     */
    private fun convertContinuousPath(originalAzimuths: List<Double>): List<Double> {
        if (originalAzimuths.isEmpty()) return emptyList()

        val firstAngle = originalAzimuths.first()
        val lastAngle = originalAzimuths.last()
        val minAngle = originalAzimuths.minOrNull() ?: 0.0
        val maxAngle = originalAzimuths.maxOrNull() ?: 0.0

        logger.info("패스 분석: ${String.format("%.2f", firstAngle)}° → ${String.format("%.2f", lastAngle)}° (범위: ${String.format("%.2f", minAngle)}° ~ ${String.format("%.2f", maxAngle)}°)")

        // ✅ 패스별 최적 오프셋 결정
        val selectedOffset = when {
            // 케이스 1: 최대값이 270도 이상 → 음수 범위
            maxAngle >= 270.0 -> {
                logger.info("최대각 270° 이상 (${String.format("%.2f", maxAngle)}°) → 음수 범위 사용")
                -360.0
            }
            // 케이스 2: 180도 이상 각도가 많은 경우 → 음수 범위
            originalAzimuths.count { it >= 180.0 } > originalAzimuths.size * 0.6 -> {
                val over180Count = originalAzimuths.count { it >= 180.0 }
                logger.info("180° 이상 각도 다수 (${over180Count}/${originalAzimuths.size}) → 음수 범위 사용")
                -360.0
            }
            // 케이스 3: 범위가 270도를 넘나드는 경우
            minAngle < 90.0 && maxAngle > 270.0 -> {
                logger.info("270° 경계 넘나듦 (${String.format("%.2f", minAngle)}° ~ ${String.format("%.2f", maxAngle)}°) → 음수 범위 사용")
                -360.0
            }
            // 기본: 범위 내
            else -> {
                logger.info("범위 내 회전 → 기본 범위 사용")
                0.0
            }
        }

        // ✅ 전체 패스에 동일한 오프셋 적용
        val result = originalAzimuths.map { angle ->
            val converted = angle + selectedOffset

            // 최종 범위 체크 및 조정
            when {
                converted > 270.0 -> converted - 360.0
                converted < -270.0 -> converted + 360.0
                else -> converted
            }
        }

        logger.info("선택된 오프셋: ${selectedOffset}°")
        logger.info("변환 결과: ${String.format("%.2f", result.first())}° → ${String.format("%.2f", result.last())}°")

        return result
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
     * 변환 결과 검증
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

                // 점프 체크
                val jump = abs(currentAz - prevAz)
                maxJump = maxOf(maxJump, jump)

                if (jump > 30.0) {
                    issues.add("MstId $mstId: 큰 각도 점프 ${String.format("%.2f", jump)}°")
                }
            }
        }

        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            outOfRangeCount = outOfRangeCount,
            maxJump = maxJump
        )
    }

    /**
     * 검증 결과 데이터 클래스
     */
    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<String>,
        val outOfRangeCount: Int,
        val maxJump: Double
    )
}
