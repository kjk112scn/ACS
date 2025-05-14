package com.gtlsystems.acs_api.algorithm.satellitetracker.factory

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.interfaces.SatellitePositionCalculator
import org.springframework.stereotype.Component

/**
 * 위성 위치 계산 알고리즘 유형
 */
enum class SatellitePositionAlgorithm {
    OREKIT_TLE
}

/**
 * 위성 위치 계산기 팩토리 클래스
 */
@Component
class SatellitePositionCalculatorFactory {

    /**
     * 지정된 알고리즘 유형에 맞는 계산기를 생성합니다.
     *
     * @param algorithm 사용할 알고리즘 유형
     * @return 위성 위치 계산기 인스턴스
     */
    fun createCalculator(algorithm: SatellitePositionAlgorithm): SatellitePositionCalculator {
        return when (algorithm) {
            SatellitePositionAlgorithm.OREKIT_TLE -> OrekitCalculator()
        }
    }
}