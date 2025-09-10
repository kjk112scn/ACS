package com.gtlsystems.acs_api.controller.system.settings

import com.gtlsystems.acs_api.service.system.settings.SettingsService
import com.gtlsystems.acs_api.openapi.SettingsApiDescriptions
import com.gtlsystems.acs_api.openapi.OpenApiUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/settings")
@Tag(name = "System - Settings", description = "시스템 설정 관리 API - 위치, 추적, 기타 설정값 관리")
class SettingsController(
    private val settingsService: SettingsService
) {

    /**
     * 위치 설정 조회
     */
    @GetMapping("/location")
    @Operation(
        operationId = "getLocation",
        tags = ["System - Settings"]
    )
    fun getLocation(): Map<String, Any> {
        return mapOf(
            "latitude" to settingsService.latitude,
            "longitude" to settingsService.longitude,
            "altitude" to settingsService.altitude
        )
    }

    /**
     * 위치 설정 변경
     */
    @PostMapping("/location")
    @Operation(
        operationId = "setLocation",
        tags = ["System - Settings"]
    )
    fun setLocation(
        @Parameter(
            description = "위치 설정 변경 요청 데이터",
            required = true
        )
        @RequestBody request: LocationRequest
    ): ResponseEntity<Map<String, String>> {
        return try {
            settingsService.setLocation(
                lat = request.latitude,
                lng = request.longitude,
                alt = request.altitude
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "위치 설정이 성공적으로 변경되었습니다.",
                "latitude" to request.latitude.toString(),
                "longitude" to request.longitude.toString(),
                "altitude" to request.altitude.toString()
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "위치 설정 변경 실패: ${e.message}"
            ))
        }
    }

    /**
     * 추적 설정 조회
     */
    @GetMapping("/tracking")
    @Operation(
        operationId = "getTracking",
        tags = ["System - Settings"]
    )
    fun getTracking(): Map<String, Any> {
        return mapOf(
            "msInterval" to settingsService.msInterval,
            "durationDays" to settingsService.durationDays,
            "minElevationAngle" to settingsService.minElevationAngle
        )
    }

    /**
     * 추적 설정 변경
     */
    @PostMapping("/tracking")
    @Operation(
        operationId = "setTracking",
        tags = ["System - Settings"]
    )
    fun setTracking(
        @Parameter(
            description = "추적 설정 변경 요청 데이터",
            required = true
        )
        @RequestBody request: TrackingRequest
    ): ResponseEntity<Map<String, String>> {
        return try {
            settingsService.setTracking(
                interval = request.msInterval,
                days = request.durationDays,
                minAngle = request.minElevationAngle
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "추적 설정이 성공적으로 변경되었습니다.",
                "msInterval" to request.msInterval.toString(),
                "durationDays" to request.durationDays.toString(),
                "minElevationAngle" to request.minElevationAngle.toString()
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "추적 설정 변경 실패: ${e.message}"
            ))
        }
    }

    /**
     * 전체 설정 조회
     */
    @GetMapping
    @Operation(
        operationId = "getAllSettings",
        tags = ["System - Settings"]
    )
    fun getAllSettings(): Map<String, Any> {
        return settingsService.getAll()
    }
}

/**
 * 위치 설정 요청 데이터
 */
data class LocationRequest(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double
)

/**
 * 추적 설정 요청 데이터
 */
data class TrackingRequest(
    val msInterval: Int,
    val durationDays: Long,
    val minElevationAngle: Float
)