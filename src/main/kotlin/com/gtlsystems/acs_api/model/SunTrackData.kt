package com.gtlsystems.acs_api.model

import java.time.ZonedDateTime

/**
 * 태양 위치 및 추적 정보를 담는 통합 데이터 클래스
 *
 * @property azimuth 태양의 방위각(도, 0-360, 북쪽에서 시계 방향)
 * @property elevation 태양의 고도각(도, -90-90, 수평선 위로 양수)
 * @property timestamp 데이터 생성 시간 (선택적)
 * @property azimuthRate 방위각 변화율(도/분) (선택적)
 * @property elevationRate 고도각 변화율(도/분) (선택적)
 * @property trackingMode 추적 모드 (CURRENT: 현재 위치, RATE: 변화율 기반) (선택적)
 * @property startTime 추적 경로 시작 시간 (선택적)
 * @property endTime 추적 경로 종료 시간 (선택적)
 * @property interval 계산 간격 (분) (선택적)
 * @property positions 시간별 태양 위치 목록 (선택적)
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
     * 현재 위치 정보만 포함된 간단한 SolarPositionData 객체를 생성합니다.
     */
    companion object {
        fun createSimplePosition(azimuth: Float, elevation: Float): SunTrackData {
            return SunTrackData(azimuth, elevation)
        }

        /**
         * 추적 명령 정보가 포함된 SolarPositionData 객체를 생성합니다.
         */
        fun createTrackingCommand(
            azimuth: Float,
            elevation: Float,
            timestamp: ZonedDateTime,
            azimuthRate: Float = 0.0f,
            elevationRate: Float = 0.0f,
            trackingMode: String = "CURRENT"
        ): SunTrackData {
            return SunTrackData(
                azimuth = azimuth,
                elevation = elevation,
                timestamp = timestamp,
                azimuthRate = azimuthRate,
                elevationRate = elevationRate,
                trackingMode = trackingMode
            )
        }

        /**
         * 추적 경로 정보가 포함된 SolarPositionData 객체를 생성합니다.
         */
        fun createTrackingPath(
            startTime: ZonedDateTime,
            endTime: ZonedDateTime,
            interval: Int,
            positions: List<Pair<ZonedDateTime, SunTrackData>>
        ): SunTrackData {
            // 첫 번째 위치의 방위각과 고도각을 사용
            val firstPosition = positions.firstOrNull()?.second ?:
            SunTrackData(0.0f, 0.0f)

            return SunTrackData(
                azimuth = firstPosition.azimuth,
                elevation = firstPosition.elevation,
                startTime = startTime,
                endTime = endTime,
                interval = interval,
                positions = positions
            )
        }
    }
}
