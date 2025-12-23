package com.gtlsystems.acs_api.algorithm.suntrack.model

import java.time.ZonedDateTime

/**
 * 태양 위치 데이터를 담는 모델 클래스
 */
data class SunTrackData(
    // 기본 태양 위치 정보
    val azimuth: Float,  // 방위각 (도)
    val elevation: Float, // 고도각 (도)

    // 추적 명령 관련 정보 (선택적)
    val timestamp: ZonedDateTime? = null,
    val azimuthRate: Float? = null,
    val elevationRate: Float? = null,
    val trackingMode: String? = null,

    // 추적 경로 관련 정보 (선택적)
    val startTime: ZonedDateTime? = null,
    val endTime: ZonedDateTime? = null,
    val interval: Int? = null,
    val positions: List<Pair<ZonedDateTime, SunTrackData>>? = null
) {
    /**
     * 현재 태양 위치가 지평선 위에 있는지 확인
     */
    val isAboveHorizon: Boolean
        get() = elevation > 0

    companion object {
        /**
         * 간단한 위치 데이터만 포함하는 객체 생성
         */
        fun createSimplePosition(azimuth: Float, elevation: Float): SunTrackData {
            return SunTrackData(azimuth, elevation)
        }
    }
}