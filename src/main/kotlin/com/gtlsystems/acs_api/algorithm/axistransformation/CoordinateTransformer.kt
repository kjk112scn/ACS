package com.gtlsystems.acs_api.algorithm.axistransformation

import kotlin.math.*

/**
 * 좌표계 변환을 위한 유틸리티 클래스
 */
class CoordinateTransformer {

    companion object {
        /**
         * 표준 방위각/고도각 좌표를 기울어진 회전체 기구물의 좌표계로 변환합니다.
         *
         * @param azimuth 표준 방위각 (도, 북쪽이 0도, 동쪽이 90도)
         * @param elevation 표준 고도각 (도, 수평이 0도, 천정이 90도)
         * @param tiltAngle 기구물의 기울기 각도 (도, 양수는 오른쪽, 음수는 왼쪽)
         * @param rotatorAngle 회전체의 회전 각도 (도, 0도는 북쪽, 90도는 동쪽)
         * @return 기울어진 회전체 좌표계에서의 방위각과 고도각 쌍 (도)
         */
        fun transformCoordinatesWithRotator(
            azimuth: Double,
            elevation: Double,
            tiltAngle: Double,
            rotatorAngle: Double
        ): Pair<Double, Double> {
            // 1. 방위각과 고도각을 라디안으로 변환
            val azRad = Math.toRadians(azimuth)
            val elRad = Math.toRadians(elevation)
            val tiltRad = Math.toRadians(tiltAngle)
            val rotatorRad = Math.toRadians(rotatorAngle)

            // 2. 방위각과 고도각을 3D 직교 좌표로 변환
            // 표준 좌표계: x(동), y(북), z(상)
            val x = cos(elRad) * sin(azRad)
            val y = cos(elRad) * cos(azRad)
            val z = sin(elRad)

            // 3. 먼저 Z축(수직축) 기준 회전 적용 (회전체 회전)
            val xRotated = x * cos(rotatorRad) - y * sin(rotatorRad)
            val yRotated = x * sin(rotatorRad) + y * cos(rotatorRad)
            val zRotated = z

            // 4. 그 다음 Y축(회전체의 수평축) 기준 회전 적용 (기울기)
            val xFinal = xRotated * cos(tiltRad) + zRotated * sin(tiltRad)
            val yFinal = yRotated
            val zFinal = -xRotated * sin(tiltRad) + zRotated * cos(tiltRad)

            // 5. 직교 좌표를 다시 방위각과 고도각으로 변환
            var azimuthFinal = Math.toDegrees(atan2(xFinal, yFinal))
            val elevationFinal = Math.toDegrees(asin(zFinal))

            // 6. 방위각을 0-360도 범위로 조정
            if (azimuthFinal < 0) {
                azimuthFinal += 360.0
            }

            return Pair(azimuthFinal, elevationFinal)
        }

        /**
         * 기울어진 회전체 기구물의 좌표계에서 표준 방위각/고도각 좌표로 역변환합니다.
         *
         * @param azimuthFinal 기울어진 회전체 좌표계의 방위각 (도)
         * @param elevationFinal 기울어진 회전체 좌표계의 고도각 (도)
         * @param tiltAngle 기구물의 기울기 각도 (도, 양수는 오른쪽, 음수는 왼쪽)
         * @param rotatorAngle 회전체의 회전 각도 (도, 0도는 북쪽, 90도는 동쪽)
         * @return 표준 좌표계에서의 방위각과 고도각 쌍 (도)
         */
        fun inverseTransformCoordinatesWithRotator(
            azimuthFinal: Double,
            elevationFinal: Double,
            tiltAngle: Double,
            rotatorAngle: Double
        ): Pair<Double, Double> {
            // 1. 기울어진 방위각과 고도각을 라디안으로 변환
            val azFinalRad = Math.toRadians(azimuthFinal)
            val elFinalRad = Math.toRadians(elevationFinal)
            val tiltRad = Math.toRadians(tiltAngle)
            val rotatorRad = Math.toRadians(rotatorAngle)

            // 2. 기울어진 방위각과 고도각을 3D 직교 좌표로 변환
            val xFinal = cos(elFinalRad) * sin(azFinalRad)
            val yFinal = cos(elFinalRad) * cos(azFinalRad)
            val zFinal = sin(elFinalRad)

            // 3. Y축 기준 역회전 적용 (기울기 역변환)
            val xRotated = xFinal * cos(-tiltRad) + zFinal * sin(-tiltRad)
            val yRotated = yFinal
            val zRotated = -xFinal * sin(-tiltRad) + zFinal * cos(-tiltRad)

            // 4. Z축 기준 역회전 적용 (회전체 회전 역변환)
            val x = xRotated * cos(-rotatorRad) - yRotated * sin(-rotatorRad)
            val y = xRotated * sin(-rotatorRad) + yRotated * cos(-rotatorRad)
            val z = zRotated

            // 5. 직교 좌표를 다시 방위각과 고도각으로 변환
            var azimuth = Math.toDegrees(atan2(x, y))
            val elevation = Math.toDegrees(asin(z))

            // 6. 방위각을 0-360도 범위로 조정
            if (azimuth < 0) {
                azimuth += 360.0
            }

            return Pair(azimuth, elevation)
        }

        /**
         * 표준 방위각/고도각 좌표를 기울어진 기구물의 좌표계로 변환합니다. (회전체 없음)
         *
         * @param azimuth 표준 방위각 (도, 북쪽이 0도, 동쪽이 90도)
         * @param elevation 표준 고도각 (도, 수평이 0도, 천정이 90도)
         * @param tiltAngle 기구물의 기울기 각도 (도, 양수는 오른쪽, 음수는 왼쪽)
         * @return 기울어진 좌표계에서의 방위각과 고도각 쌍 (도)
         */
        fun transformCoordinates(azimuth: Double, elevation: Double, tiltAngle: Double): Pair<Double, Double> {
            return transformCoordinatesWithRotator(azimuth, elevation, tiltAngle, 0.0)
        }

        /**
         * 기울어진 기구물의 좌표계에서 표준 방위각/고도각 좌표로 역변환합니다. (회전체 없음)
         *
         * @param azimuthTilted 기울어진 좌표계의 방위각 (도)
         * @param elevationTilted 기울어진 좌표계의 고도각 (도)
         * @param tiltAngle 기구물의 기울기 각도 (도, 양수는 오른쪽, 음수는 왼쪽)
         * @return 표준 좌표계에서의 방위각과 고도각 쌍 (도)
         */
        fun inverseTransformCoordinates(azimuthTilted: Double, elevationTilted: Double, tiltAngle: Double): Pair<Double, Double> {
            return inverseTransformCoordinatesWithRotator(azimuthTilted, elevationTilted, tiltAngle, 0.0)
        }

        /**
         * 회전체 각도에 따른 방위각/고도각 변화를 계산하여 테이블로 생성합니다.
         *
         * @param azimuth 표준 방위각 (도)
         * @param elevation 표준 고도각 (도)
         * @param tiltAngle 기구물의 기울기 각도 (도)
         * @param rotatorStepDegrees 회전체 각도 간격 (도)
         * @return 회전체 각도별 방위각/고도각 테이블
         */
        fun generateRotatorAngleTable(
            azimuth: Double,
            elevation: Double,
            tiltAngle: Double,
            rotatorStepDegrees: Double = 0.0
        ): List<Triple<Double, Double, Double>> {
            val result = mutableListOf<Triple<Double, Double, Double>>()

            var rotatorAngle = 0.0
            while (rotatorAngle < 360.0) {
                val (transformedAz, transformedEl) = transformCoordinatesWithRotator(
                    azimuth, elevation, tiltAngle, rotatorAngle
                )

                result.add(Triple(rotatorAngle, transformedAz, transformedEl))
                rotatorAngle += rotatorStepDegrees
            }

            return result
        }

    }
}
