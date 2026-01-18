package com.gtlsystems.acs_api.algorithm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import com.gtlsystems.acs_api.service.system.settings.SettingsService
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL

class ElevationCalculator(
    private val settingsService: SettingsService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ElevationCalculator::class.java)
    }

    /**
     * 두 API 모두 호출해서 결과 비교
     * @param latitude 위도
     * @param longitude 경도
     * @param googleApiKey Google Maps API 키 (선택사항)
     */
    suspend fun getElevationComparison(
        latitude: Double,
        longitude: Double,
        googleApiKey: String? = null
    ): ElevationComparison = withContext(Dispatchers.IO) {

        logger.debug("두 API 동시 호출 시작: 위도={}, 경도={}", latitude, longitude)

        // 두 API를 동시에 호출 (병렬 처리)
        val googleDeferred = async {
            if (!googleApiKey.isNullOrEmpty()) {
                logger.debug("Google API 호출 중...")
                getElevationFromGoogle(latitude, longitude, googleApiKey)
            } else {
                logger.debug("Google API 키가 없어 건너뜀")
                null
            }
        }

        val openElevationDeferred = async {
            logger.debug("Open-Elevation API 호출 중...")
            getElevationFromOpenElevation(latitude, longitude)
        }

        // 결과 수집
        val googleResult = try {
            googleDeferred.await()
        } catch (e: Exception) {
            logger.warn("Google API 오류: {}", e.message)
            null
        }

        val openElevationResult = try {
            openElevationDeferred.await()
        } catch (e: Exception) {
            logger.warn("Open-Elevation API 오류: {}", e.message)
            null
        }

        // 근사값도 계산
        val approximateElevation = getApproximateElevation(latitude, longitude)

        ElevationComparison(
            googleElevation = googleResult,
            openElevation = openElevationResult,
            approximateElevation = approximateElevation,
            latitude = latitude,
            longitude = longitude
        )
    }

    // 기존 함수들...
    suspend fun getElevationFromGoogle(
        latitude: Double,
        longitude: Double,
        apiKey: String
    ): Double? = withContext(Dispatchers.IO) {
        try {
            val url = "https://maps.googleapis.com/maps/api/elevation/json" +
                    "?locations=$latitude,$longitude" +
                    "&key=$apiKey"

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val connectTimeout = settingsService.systemUdpTimeout * 400  // 25ms * 400 = 10초
            val readTimeout = settingsService.systemUdpTimeout * 400     // 25ms * 400 = 10초
            connection.connectTimeout = connectTimeout.toInt()
            connection.readTimeout = readTimeout.toInt()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                parseGoogleElevationResponse(response)
            } else {
                logger.warn("Google API HTTP Error: {}", connection.responseCode)
                null
            }
        } catch (e: Exception) {
            logger.warn("Google Elevation API 호출 실패: {}", e.message)
            null
        }
    }

    suspend fun getElevationFromOpenElevation(
        latitude: Double,
        longitude: Double
    ): Double? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-elevation.com/api/v1/lookup" +
                    "?locations=$latitude,$longitude"

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                parseOpenElevationResponse(response)
            } else {
                logger.warn("Open-Elevation API HTTP Error: {}", connection.responseCode)
                null
            }
        } catch (e: Exception) {
            logger.warn("Open-Elevation API 호출 실패: {}", e.message)
            null
        }
    }

    private fun parseGoogleElevationResponse(response: String): Double? {
        return try {
            val statusRegex = "\"status\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val statusMatch = statusRegex.find(response)
            if (statusMatch?.groupValues?.get(1) != "OK") {
                logger.warn("Google Elevation API Error: {}", statusMatch?.groupValues?.get(1))
                return null
            }

            val elevationRegex = "\"elevation\"\\s*:\\s*([\\d.-]+)".toRegex()
            val elevationMatch = elevationRegex.find(response)
            elevationMatch?.groupValues?.get(1)?.toDoubleOrNull()
        } catch (e: Exception) {
            logger.warn("Google API 응답 파싱 실패: {}", e.message)
            null
        }
    }

    private fun parseOpenElevationResponse(response: String): Double? {
        return try {
            val elevationRegex = "\"elevation\"\\s*:\\s*([\\d.-]+)".toRegex()
            val elevationMatch = elevationRegex.find(response)
            elevationMatch?.groupValues?.get(1)?.toDoubleOrNull()
        } catch (e: Exception) {
            logger.warn("Open-Elevation API 응답 파싱 실패: {}", e.message)
            null
        }
    }

    fun getApproximateElevation(latitude: Double, longitude: Double): Double {
        return when {
            // 서울 근처 (37.4-37.7°N, 126.8-127.1°E)
            latitude in 37.4..37.7 && longitude in 126.8..127.1 -> 50.0
            // 부산 근처 (35.0-35.3°N, 128.9-129.2°E)
            latitude in 35.0..35.3 && longitude in 128.9..129.2 -> 10.0
            // 대구 근처 (35.7-36.0°N, 128.5-128.8°E)
            latitude in 35.7..36.0 && longitude in 128.5..128.8 -> 50.0
            // 인천 근처 (37.3-37.6°N, 126.6-126.9°E)
            latitude in 37.3..37.6 && longitude in 126.6..126.9 -> 20.0
            // 광주 근처 (35.1-35.2°N, 126.8-127.0°E)
            latitude in 35.1..35.2 && longitude in 126.8..127.0 -> 60.0
            // 대전 근처 (36.3-36.4°N, 127.3-127.5°E)
            latitude in 36.3..36.4 && longitude in 127.3..127.5 -> 80.0
            // 울산 근처 (35.5-35.6°N, 129.2-129.4°E)
            latitude in 35.5..35.6 && longitude in 129.2..129.4 -> 30.0
            // 제주도 근처 (33.2-33.6°N, 126.1-126.9°E)
            latitude in 33.2..33.6 && longitude in 126.1..126.9 -> 200.0
            // 강원도 산간지역 (대략적)
            latitude in 37.0..38.5 && longitude in 127.5..129.0 -> 500.0
            // 경북 산간지역 (대략적)
            latitude in 35.5..37.0 && longitude in 128.0..129.5 -> 300.0
            // 전남 해안지역
            latitude in 34.0..35.5 && longitude in 126.0..127.5 -> 50.0
            // 기본값 (해수면 기준)
            else -> 0.0
        }
    }

    /**
     * 두 API 결과 비교를 담는 데이터 클래스
     */
    data class ElevationComparison(
        val googleElevation: Double?,      // Google API 결과
        val openElevation: Double?,        // Open-Elevation API 결과
        val approximateElevation: Double,  // 근사값
        val latitude: Double,
        val longitude: Double
    ) {

        /**
         * 가장 신뢰할 만한 고도값 반환
         */
        fun getBestElevation(): Double {
            return googleElevation ?: openElevation ?: approximateElevation
        }

        /**
         * 두 API 결과의 차이 계산
         */
        fun getDifference(): Double? {
            return if (googleElevation != null && openElevation != null) {
                kotlin.math.abs(googleElevation - openElevation)
            } else null
        }

        /**
         * 결과 요약 문자열
         */
        fun getSummary(): String {
            val sb = StringBuilder()
            sb.append("=== 고도 비교 결과 ===\n")
            sb.append("위치: 위도 $latitude, 경도 $longitude\n")

            if (googleElevation != null) {
                sb.append("Google API: ${String.format("%.1f", googleElevation)}m\n")
            } else {
                sb.append("Google API: 실패 또는 API 키 없음\n")
            }

            if (openElevation != null) {
                sb.append("Open-Elevation API: ${String.format("%.1f", openElevation)}m\n")
            } else {
                sb.append("Open-Elevation API: 실패\n")
            }

            sb.append("근사값: ${String.format("%.1f", approximateElevation)}m\n")

            getDifference()?.let { diff ->
                sb.append("API 간 차이: ${String.format("%.1f", diff)}m\n")
            }

            sb.append("권장값: ${String.format("%.1f", getBestElevation())}m")

            return sb.toString()
        }

        override fun toString(): String = getSummary()
    }
}
