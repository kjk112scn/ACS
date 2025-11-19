package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import java.time.temporal.ChronoUnit

/**
 * 자정 경계에서 잘린 패스를 제거하여 스케줄을 정제합니다.
 *
 * Orekit 계산이 00:00에 시작되면 바로 직전에 진행 중이던 패스가
 * 잘린 상태로 남을 수 있으므로 첫 번째 패스를 제거하여
 * 불완전한 데이터가 UI에 노출되지 않도록 합니다.
 */
internal fun OrekitCalculator.SatelliteTrackingSchedule.removeLeadingMidnightPass(): OrekitCalculator.SatelliteTrackingSchedule {
    if (trackingPasses.isEmpty()) {
        return this
    }

    val midnightBoundary = startDate.truncatedTo(ChronoUnit.MINUTES)
    val firstPass = trackingPasses.first()
    val firstPassStart = firstPass.startTime.truncatedTo(ChronoUnit.MINUTES)

    val shouldDropFirstPass = firstPassStart.isEqual(midnightBoundary)
    if (!shouldDropFirstPass) {
        return this
    }

    val remainingPasses = trackingPasses.drop(1)
    return copy(trackingPasses = remainingPasses)
}


