package com.gtlsystems.acs_api.algorithm.suntrack.factory

import com.gtlsystems.acs_api.algorithm.suntrack.impl.Grena3Calculator
import com.gtlsystems.acs_api.algorithm.suntrack.impl.SPACalculator
import com.gtlsystems.acs_api.algorithm.suntrack.interfaces.SunPositionCalculator
import org.springframework.stereotype.Component

/**
 * 태양 위치 계산 알고리즘 유형
 */
enum class SunPositionAlgorithm {
    SPA,
    GRENA3
}

/**
 * 태양 위치 계산기 팩토리 클래스
 */
@Component
class SunPositionCalculatorFactory {

    /**
     * 지정된 알고리즘 유형에 맞는 계산기를 생성합니다.
     *
     * @param algorithm 사용할 알고리즘 유형
     * @return 태양 위치 계산기 인스턴스
     */
    fun createCalculator(algorithm: SunPositionAlgorithm): SunPositionCalculator {
        return when (algorithm) {
            SunPositionAlgorithm.SPA -> SPACalculator()
            SunPositionAlgorithm.GRENA3 -> Grena3Calculator()
        }
    }
}