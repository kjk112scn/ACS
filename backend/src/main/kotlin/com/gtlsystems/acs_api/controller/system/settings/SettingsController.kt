package com.gtlsystems.acs_api.controller.system.settings

import com.gtlsystems.acs_api.service.system.settings.SettingsService
import com.gtlsystems.acs_api.openapi.SettingsApiDescriptions
import com.gtlsystems.acs_api.openapi.OpenApiUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull

@RestController
@RequestMapping("/api/settings")
@Tag(name = "System - Settings", description = "시스템 설정 관리 API - 위치, 추적, 기타 설정값 관리")
@Transactional  // ← 이 어노테이션 추가 필요
class SettingsController(
    private val settingsService: SettingsService
) {
    private val logger = LoggerFactory.getLogger(SettingsController::class.java)

    /**
     * 위치 설정 조회
     */
    @GetMapping("/location")
    @Operation(
        operationId = "getLocation",
        tags = ["System - Settings"]
    )
    fun getLocation(): Map<String, Any> {
        return settingsService.getLocationSettings()
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
        @Valid @RequestBody request: LocationRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setLocation(
                lat = request.latitude,
                lng = request.longitude,
                alt = request.altitude
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "위치 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "latitude" to settingsService.latitude,
                    "longitude" to settingsService.longitude,
                    "altitude" to settingsService.altitude
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "위치 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("위치 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "위치 설정 변경 중 서버 오류가 발생했습니다."
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
        return settingsService.getTrackingSettings()
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
        @Valid @RequestBody request: TrackingRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setTracking(
                interval = request.msInterval,
                days = request.durationDays,
                minAngle = request.minElevationAngle
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "추적 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "msInterval" to settingsService.msInterval,
                    "durationDays" to settingsService.durationDays,
                    "minElevationAngle" to settingsService.minElevationAngle
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "추적 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("추적 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "추적 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Stow Angle 설정 조회
     */
    @GetMapping("/stow/angle")
    @Operation(
        operationId = "getStowAngle",
        tags = ["System - Settings"]
    )
    fun getStowAngle(): Map<String, Any> {
        return settingsService.getStowAngleSettings()
    }

    /**
     * Stow Angle 설정 변경
     */
    @PostMapping("/stow/angle")
    @Operation(
        operationId = "setStowAngle",
        tags = ["System - Settings"]
    )
    fun setStowAngle(
        @Valid @RequestBody request: StowAngleRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setStowAngles(
                azimuth = request.azimuth,
                elevation = request.elevation,
                train = request.train
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Stow Angle 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "azimuth" to settingsService.stowAngleAzimuth,
                    "elevation" to settingsService.stowAngleElevation,
                    "train" to settingsService.stowAngleTrain
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Stow Angle 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Stow Angle 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Stow Angle 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Stow Speed 설정 조회
     */
    @GetMapping("/stow/speed")
    @Operation(
        operationId = "getStowSpeed",
        tags = ["System - Settings"]
    )
    fun getStowSpeed(): Map<String, Any> {
        return settingsService.getStowSpeedSettings()
    }

    /**
     * Stow Speed 설정 변경
     */
    @PostMapping("/stow/speed")
    @Operation(
        operationId = "setStowSpeed",
        tags = ["System - Settings"]
    )
    fun setStowSpeed(
        @Valid @RequestBody request: StowSpeedRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setStowSpeeds(
                azimuth = request.azimuth,
                elevation = request.elevation,
                train = request.train
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Stow Speed 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "azimuth" to settingsService.stowSpeedAzimuth,
                    "elevation" to settingsService.stowSpeedElevation,
                    "train" to settingsService.stowSpeedTrain
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Stow Speed 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Stow Speed 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Stow Speed 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Stow 전체 설정 조회
     */
    @GetMapping("/stow/all")
    @Operation(
        operationId = "getStowAll",
        tags = ["System - Settings"]
    )
    fun getStowAll(): Map<String, Any> {
        return mapOf(
            "angle" to settingsService.getStowAngleSettings(),
            "speed" to settingsService.getStowSpeedSettings()
        )
    }

    /**
     * Stow 전체 설정 변경
     */
    @PostMapping("/stow/all")
    @Operation(
        operationId = "setStowAll",
        tags = ["System - Settings"]
    )
    fun setStowAll(
        @Valid @RequestBody request: StowAllRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setStowAll(
                angleAzimuth = request.angleAzimuth,
                angleElevation = request.angleElevation,
                angleTrain = request.angleTrain,
                speedAzimuth = request.speedAzimuth,
                speedElevation = request.speedElevation,
                speedTrain = request.speedTrain
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Stow 전체 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "angle" to mapOf(
                        "azimuth" to settingsService.stowAngleAzimuth,
                        "elevation" to settingsService.stowAngleElevation,
                        "train" to settingsService.stowAngleTrain
                    ),
                    "speed" to mapOf(
                        "azimuth" to settingsService.stowSpeedAzimuth,
                        "elevation" to settingsService.stowSpeedElevation,
                        "train" to settingsService.stowSpeedTrain
                    )
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Stow 전체 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Stow 전체 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Stow 전체 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * AntennaSpec 설정 조회
     */
    @GetMapping("/antennaspec")
    @Operation(
        operationId = "getAntennaSpec",
        tags = ["System - Settings"]
    )
    fun getAntennaSpec(): Map<String, Any> {
        return settingsService.getAntennaSpecSettings()
    }

    /**
     * AntennaSpec 설정 변경
     */
    @PostMapping("/antennaspec")
    @Operation(
        operationId = "setAntennaSpec",
        tags = ["System - Settings"]
    )
    fun setAntennaSpec(
        @Valid @RequestBody request: AntennaSpecRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setAntennaSpec(
                trueNorthOffsetAngle = request.trueNorthOffsetAngle,
                tiltAngle = request.tiltAngle
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "AntennaSpec 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "trueNorthOffsetAngle" to settingsService.trueNorthOffsetAngle,
                    "tiltAngle" to settingsService.tiltAngle
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "AntennaSpec 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("AntennaSpec 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "AntennaSpec 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Angle Limits 설정 조회
     */
    @GetMapping("/anglelimits")
    @Operation(
        operationId = "getAngleLimits",
        tags = ["System - Settings"]
    )
    fun getAngleLimits(): Map<String, Any> {
        return settingsService.getAngleLimitsSettings()
    }

    /**
     * Angle Limits 설정 변경
     */
    @PostMapping("/anglelimits")
    @Operation(
        operationId = "setAngleLimits",
        tags = ["System - Settings"]
    )
    fun setAngleLimits(
        @Valid @RequestBody request: AngleLimitsRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setAngleLimits(
                azimuthMin = request.azimuthMin,
                azimuthMax = request.azimuthMax,
                elevationMin = request.elevationMin,
                elevationMax = request.elevationMax,
                trainMin = request.trainMin,
                trainMax = request.trainMax
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Angle Limits 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "azimuthMin" to settingsService.angleAzimuthMin,
                    "azimuthMax" to settingsService.angleAzimuthMax,
                    "elevationMin" to settingsService.angleElevationMin,
                    "elevationMax" to settingsService.angleElevationMax,
                    "trainMin" to settingsService.angleTrainMin,
                    "trainMax" to settingsService.angleTrainMax
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Angle Limits 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Angle Limits 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Angle Limits 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Speed Limits 설정 조회
     */
    @GetMapping("/speedlimits")
    @Operation(
        operationId = "getSpeedLimits",
        tags = ["System - Settings"]
    )
    fun getSpeedLimits(): Map<String, Any> {
        return settingsService.getSpeedLimitsSettings()
    }

    /**
     * Speed Limits 설정 변경
     */
    @PostMapping("/speedlimits")
    @Operation(
        operationId = "setSpeedLimits",
        tags = ["System - Settings"]
    )
    fun setSpeedLimits(
        @Valid @RequestBody request: SpeedLimitsRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setSpeedLimits(
                azimuthMin = request.azimuthMin,
                azimuthMax = request.azimuthMax,
                elevationMin = request.elevationMin,
                elevationMax = request.elevationMax,
                trainMin = request.trainMin,
                trainMax = request.trainMax
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Speed Limits 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "azimuthMin" to settingsService.speedAzimuthMin,
                    "azimuthMax" to settingsService.speedAzimuthMax,
                    "elevationMin" to settingsService.speedElevationMin,
                    "elevationMax" to settingsService.speedElevationMax,
                    "trainMin" to settingsService.speedTrainMin,
                    "trainMax" to settingsService.speedTrainMax
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Speed Limits 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Speed Limits 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Speed Limits 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Angle Offset Limits 설정 조회
     */
    @GetMapping("/angleoffsetlimits")
    @Operation(
        operationId = "getAngleOffsetLimits",
        tags = ["System - Settings"]
    )
    fun getAngleOffsetLimits(): Map<String, Any> {
        return settingsService.getAngleOffsetLimitsSettings()
    }

    /**
     * Angle Offset Limits 설정 변경
     */
    @PostMapping("/angleoffsetlimits")
    @Operation(
        operationId = "setAngleOffsetLimits",
        tags = ["System - Settings"]
    )
    fun setAngleOffsetLimits(
        @Valid @RequestBody request: AngleOffsetLimitsRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setAngleOffsetLimits(
                azimuth = request.azimuth,
                elevation = request.elevation,
                train = request.train
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Angle Offset Limits 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "azimuth" to settingsService.angleOffsetAzimuth,
                    "elevation" to settingsService.angleOffsetElevation,
                    "train" to settingsService.angleOffsetTrain
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Angle Offset Limits 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Angle Offset Limits 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Angle Offset Limits 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Time Offset Limits 설정 조회
     */
    @GetMapping("/timeoffsetlimits")
    @Operation(
        operationId = "getTimeOffsetLimits",
        tags = ["System - Settings"]
    )
    fun getTimeOffsetLimits(): Map<String, Any> {
        return settingsService.getTimeOffsetLimitsSettings()
    }

    /**
     * Time Offset Limits 설정 변경
     */
    @PostMapping("/timeoffsetlimits")
    @Operation(
        operationId = "setTimeOffsetLimits",
        tags = ["System - Settings"]
    )
    fun setTimeOffsetLimits(
        @Valid @RequestBody request: TimeOffsetLimitsRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setTimeOffsetLimits(
                min = request.min,
                max = request.max
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Time Offset Limits 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "min" to settingsService.timeOffsetMin,
                    "max" to settingsService.timeOffsetMax
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Time Offset Limits 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Time Offset Limits 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Time Offset Limits 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Algorithm 설정 조회
     */
    @GetMapping("/algorithm")
    @Operation(
        operationId = "getAlgorithm",
        tags = ["System - Settings"]
    )
    fun getAlgorithm(): Map<String, Any> {
        return settingsService.getAlgorithmSettings()
    }

    /**
     * Algorithm 설정 변경
     */
    @PostMapping("/algorithm")
    @Operation(
        operationId = "setAlgorithm",
        tags = ["System - Settings"]
    )
    fun setAlgorithm(
        @Valid @RequestBody request: AlgorithmRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setAlgorithm(
                geoMinMotion = request.geoMinMotion
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Algorithm 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "geoMinMotion" to settingsService.geoMinMotion
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Algorithm 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Algorithm 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Algorithm 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * StepSizeLimit 설정 조회
     */
    @GetMapping("/stepsizelimit")
    @Operation(
        operationId = "getStepSizeLimit",
        tags = ["System - Settings"]
    )
    fun getStepSizeLimit(): Map<String, Any> {
        return settingsService.getStepSizeLimitSettings()
    }

    /**
     * StepSizeLimit 설정 변경
     */
    @PostMapping("/stepsizelimit")
    @Operation(
        operationId = "setStepSizeLimit",
        tags = ["System - Settings"]
    )
    fun setStepSizeLimit(
        @Valid @RequestBody request: StepSizeLimitRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            settingsService.setStepSizeLimit(
                min = request.min,
                max = request.max
            )
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "StepSizeLimit 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "min" to settingsService.stepSizeMin,
                    "max" to settingsService.stepSizeMax
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "StepSizeLimit 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("StepSizeLimit 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "StepSizeLimit 설정 변경 중 서버 오류가 발생했습니다."
            ))
        }
    }

    /**
     * Feed 설정 조회
     */
    @GetMapping("/feed")
    @Operation(
        operationId = "getFeed",
        tags = ["System - Settings"]
    )
    fun getFeed(): Map<String, Any> {
        return settingsService.getFeedSettings()
    }

    /**
     * Feed 설정 변경
     */
    @PostMapping("/feed")
    @Operation(
        operationId = "setFeed",
        tags = ["System - Settings"]
    )
    fun setFeed(
        @Parameter(
            description = "Feed 설정 변경 요청 데이터",
            required = true
        )
        @Valid @RequestBody request: FeedRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("📥 Feed 설정 변경 요청 수신: enabledBands = ${request.enabledBands}")
            settingsService.setFeed(request.enabledBands)
            val updatedBands = settingsService.getFeedSettings()["feed.enabledBands"]
            logger.info("✅ Feed 설정 변경 완료: enabledBands = $updatedBands")
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Feed 설정이 성공적으로 변경되었습니다.",
                "data" to mapOf(
                    "enabledBands" to updatedBands
                )
            ))
        } catch (e: IllegalArgumentException) {
            logger.warn("Feed 설정 변경 실패 - 잘못된 입력: ${e.message}")
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "Feed 설정 변경 실패: ${e.message}"
            ))
        } catch (e: Exception) {
            logger.error("Feed 설정 변경 중 오류 발생", e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Feed 설정 변경 중 서버 오류가 발생했습니다."
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
    @field:NotNull(message = "위도는 필수입니다")
    @field:DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
    val latitude: Double,
    
    @field:NotNull(message = "경도는 필수입니다")
    @field:DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
    val longitude: Double,
    
    @field:NotNull(message = "고도는 필수입니다")
    @field:DecimalMin(value = "0.0", message = "고도는 0미터 이상이어야 합니다")
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

/**
 * Stow Angle 설정 요청 데이터
 */
data class StowAngleRequest(
    val azimuth: Double,
    val elevation: Double,
    val train: Double
)

/**
 * Stow Speed 설정 요청 데이터
 */
data class StowSpeedRequest(
    val azimuth: Double,
    val elevation: Double,
    val train: Double
)

/**
 * Stow 전체 설정 요청 데이터
 */
data class StowAllRequest(
    val angleAzimuth: Double,
    val angleElevation: Double,
    val angleTrain: Double,
    val speedAzimuth: Double,
    val speedElevation: Double,
    val speedTrain: Double
)

/**
 * AntennaSpec 설정 요청 데이터
 */
data class AntennaSpecRequest(
    val trueNorthOffsetAngle: Double,
    val tiltAngle: Double
)

/**
 * Angle Limits 설정 요청 데이터
 */
data class AngleLimitsRequest(
    val azimuthMin: Double,
    val azimuthMax: Double,
    val elevationMin: Double,
    val elevationMax: Double,
    val trainMin: Double,
    val trainMax: Double
)

/**
 * Speed Limits 설정 요청 데이터
 */
data class SpeedLimitsRequest(
    val azimuthMin: Double,
    val azimuthMax: Double,
    val elevationMin: Double,
    val elevationMax: Double,
    val trainMin: Double,
    val trainMax: Double
)

/**
 * Angle Offset Limits 설정 요청 데이터
 */
data class AngleOffsetLimitsRequest(
    val azimuth: Double,
    val elevation: Double,
    val train: Double
)

/**
 * Time Offset Limits 설정 요청 데이터
 */
data class TimeOffsetLimitsRequest(
    val min: Double,
    val max: Double
)

/**
 * Algorithm 설정 요청 데이터
 */
data class AlgorithmRequest(
    val geoMinMotion: Double
)

/**
 * StepSizeLimit 설정 요청 데이터
 */
data class StepSizeLimitRequest(
    val min: Double,
    val max: Double
)

/**
 * Feed 설정 요청 데이터
 */
data class FeedRequest(
    val enabledBands: List<String>
)