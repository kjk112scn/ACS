package com.gtlsystems.acs_api.sandbox

fun main() {
    satelliteTransform()
}
fun satelliteTransform() {
    calculateRotatorAngle(265.0, 25.0, -6.98, 0.0)
}

fun satelliteTest() {
    try {

        calculateRotatorAngleTable(
            standardAzimuth = 177.796609998884,
            standardElevation = 46.4621529680836,
            tiltAngle = -6.98,
            rotatorStepDegrees = 356.62
        )
    } catch (e: Exception) {
        logger.error("satellite_Test 실행 중 오류 발생: ${e.message}", e)
    }
}

fun calculateRotatorAngle(
    standardAzimuth: Double, standardElevation: Double, tiltAngle: Double, rotatorStepDegrees: Double = 0.0
) {
    val (transformedAzimuth, transformedElevation) = CoordinateTransformer.transformCoordinatesWithRotator(
        standardAzimuth, standardElevation, tiltAngle, rotatorStepDegrees
    )
    logger.info("─────────────────────────────────────────────")
    logger.info("transformedAzimuth : $transformedAzimuth")
    logger.info("transformedElevation : $transformedElevation")
    logger.info("─────────────────────────────────────────────")
}

fun calculateRotatorAngleTable(
    standardAzimuth: Double, standardElevation: Double, tiltAngle: Double, rotatorStepDegrees: Double = 0.0
) {
    val table = CoordinateTransformer.generateRotatorAngleTable(
        standardAzimuth, standardElevation, tiltAngle, rotatorStepDegrees
    )

    logger.info("회전체 각도에 따른 방위각/고도각 변화 테이블")
    logger.info("표준 좌표: Az=${standardAzimuth}°, El=${standardElevation}°, 기울기=${tiltAngle}°")
    logger.info("─────────────────────────────────────────────")
    logger.info("│ 회전체 각도 │   방위각   │   고도각   │")
    logger.info("─────────────────────────────────────────────")

    table.forEach { (rotatorAngle, az, el) ->
        logger.info(
            "│ ${String.format("%20.8f", rotatorAngle)}° │ ${
                String.format(
                    "%20.8f", az
                )
            }° │ ${String.format("%20.8f", el)}° │"
        )
    }
    logger.info("─────────────────────────────────────────────")
}
