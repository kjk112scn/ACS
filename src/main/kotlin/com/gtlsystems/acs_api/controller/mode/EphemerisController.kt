package com.gtlsystems.acs_api.controller.mode

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.service.mode.EphemerisService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.ZonedDateTime

@RestController
@RequestMapping("/api/ephemeris")
@Tag(name = "Mode - Ephemeris", description = "위성 궤도 제어 API - TLE 기반 위성 위치 계산, 궤도 추적 제어, 가시성 분석")
class EphemerisController(
    private val orekitCalculator: OrekitCalculator, private val ephemerisService: EphemerisService
) {
    private val logger = LoggerFactory.getLogger(EphemerisController::class.java)

    @PostMapping("/3axis/tracking/geostationary/start")
    @Operation(
        operationId = "startgeostationarytracking", 
        tags = ["Mode - Ephemeris"]
    )
    fun startGeostationaryTracking(
        @Parameter(
            description = "정지궤도 위성 추적 시작 요청 데이터", 
            required = true
        ) @RequestBody request: GeostationaryTrackingRequest
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                ephemerisService.startGeostationaryTracking(request.tleLine1, request.tleLine2)
                
                mapOf(
                    "status" to "success",
                    "message" to "정지궤도 위성 추적이 시작되었습니다.",
                    "satelliteId" to request.tleLine1.substring(2, 7).trim(),
                    "trackingType" to "geostationary"
                )
            } catch (e: Exception) {
                mapOf(
                    "status" to "error",
                    "message" to "정지궤도 위성 추적 시작 실패: ${e.message}", 
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }

    @PostMapping("/3axis/tracking/geostationary/calculate-angles")
    @Operation(
        operationId = "calculategeostationaryangles", tags = ["Mode - Ephemeris"]
    )
    fun calculateGeostationaryAngles(
        @Parameter(
            description = "정지궤도 위성 추적 각도 계산 요청 데이터", required = true
        ) @RequestBody request: GeostationaryTrackingRequest
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val geoPosition = ephemerisService.getCurrentGeostationaryPositionWith3AxisTransform(
                    request.tleLine1, request.tleLine2
                )

                mapOf(
                    "message" to "정지궤도 위성 각도가 계산되었습니다.",
                    "satelliteId" to request.tleLine1.substring(2, 7).trim(),
                    "azimuth" to (geoPosition["transformedAzimuth"] as Double),
                    "elevation" to (geoPosition["transformedElevation"] as Double),
                    "originalAzimuth" to (geoPosition["originalAzimuth"] as Double),
                    "originalElevation" to (geoPosition["originalElevation"] as Double),
                    "tiltAngle" to (geoPosition["tiltAngle"] as Double),
                    "rotatorAngle" to (geoPosition["rotatorAngle"] as Double),
                    "trackingType" to "geostationary"
                )
            } catch (e: Exception) {
                mapOf(
                    "message" to "정지궤도 위성 각도 계산 실패: ${e.message}", "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }

    /**
     * 실시간 추적 데이터를 조회 (JSON)
     */
    @GetMapping("/tracking/realtime-data")
    @Operation(
        operationId = "getephemerisdata", tags = ["Mode - Ephemeris"]
    )
    fun getRealtimeTrackingData(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val realtimeData = ephemerisService.getRealtimeTrackingData()
            val stats = ephemerisService.getRealtimeTrackingStats()

            mapOf(
                "totalCount" to realtimeData.size, "data" to realtimeData, "statistics" to stats
            )
        }
    }

    @PostMapping("/set-current-tracking-pass-id")
    @Operation(
        operationId = "setcurrenttrackingpassid", 
        tags = ["Mode - Ephemeris"]
    )
    fun setCurrentTrackingPassId(
        @Parameter(
            description = "추적할 위성의 Pass ID", 
            example = "1", 
            required = true
        ) @RequestParam passId: Long?  // ✅ UInt → Long 변경 (PassSchedule과 동일)
    ): ResponseEntity<Map<String, String>> {
        return try {
            ephemerisService.setCurrentTrackingPassId(passId)
            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "현재 추적 ID가 성공적으로 설정되었습니다.",
                    "command" to "setCurrentTrackingPassId",
                    "passId" to passId.toString()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error", 
                    "message" to "추적 ID 설정 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/time-offset-command")
    fun timeOffsetCommand(@RequestParam inputTimeOffset: Float): ResponseEntity<Map<String, String>> {
        return try {
            ephemerisService.ephemerisTimeOffsetCommand(inputTimeOffset)
            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "TimeOffset 명령이 성공적으로 처리되었습니다.",
                    "command" to "TimeOffset",
                    "timeOffset" to inputTimeOffset.toString()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error", "message" to "TimeOffset 명령 처리 실패: ${e.message}"
                )
            )
        }
    }


    /**
     * TLE 데이터를 기반으로 추적 데이터를 생성합니다.
     */
    @PostMapping("/tracking/generate")
    @Operation(
        operationId = "generateephemeristrack", tags = ["Mode - Ephemeris"]
    )
    fun generateEphemerisTrack(
        @Parameter(
            description = "추적 데이터 생성 요청 데이터", required = true
        ) @RequestBody request: EphemerisTrackRequest
    ): Mono<Map<String, Any>> {
        return ephemerisService.generateEphemerisDesignationTrackAsync(
            request.tleLine1, request.tleLine2, request.satelliteName
        ).map { (mstData, dtlData) ->
            // KEYHOLE 위성 분석 및 로깅
            val keyholeSchedules = mstData.filter { it["IsKeyhole"] == true }
            val totalSchedules = mstData.size
            
            logger.info("🚀 위성 추적 데이터 생성 완료:")
            logger.info("  - 총 스케줄: ${totalSchedules}개")
            logger.info("  - KEYHOLE 위성: ${keyholeSchedules.size}개")
            
            if (keyholeSchedules.isNotEmpty()) {
                logger.info("🔍 KEYHOLE 위성 상세 정보:")
                keyholeSchedules.forEach { schedule ->
                    val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
                    val maxAzRate = schedule["MaxAzRate"] as? Double ?: 0.0
                    val recommendedTrainAngle = schedule["RecommendedTrainAngle"] as? Double ?: 0.0
                    logger.info("  - $satelliteName: 최대 Az 속도=${maxAzRate}°/s, 권장 Train 각도=${recommendedTrainAngle}°")
                }
            }
            
            mapOf<String, Any>(
                "message" to "추적 데이터가 성공적으로 생성되었습니다.", 
                "mstCount" to mstData.size, 
                "dtlCount" to dtlData.size,
                "keyholeCount" to keyholeSchedules.size,
                "totalSchedules" to totalSchedules
            )
        }.onErrorReturn(
            mapOf<String, Any>(
                "message" to "추적 데이터 생성 실패", 
                "error" to "추적 데이터 생성에 실패했습니다."
            )
        )
    }

    /**
     * 추적 데이터 목록을 조회합니다.
     */
    @GetMapping("/master")
    @Operation(
        operationId = "getephemerislist", tags = ["Mode - Ephemeris"]
    )
    fun getAllEphemerisTrackMst(): Mono<List<Map<String, Any?>>> {
        return Mono.fromCallable {
            ephemerisService.getAllEphemerisTrackMstMerged()
        }
    }

    /**
     * 추적 데이터 상세 정보를 조회합니다.
     * ✅ mstId와 detailId를 사용하여 조회 (PassSchedule과 동일한 구조)
     */
    @GetMapping("/detail/{mstId}/pass/{detailId}")
    @Operation(
        operationId = "getephemerisdetail", tags = ["Mode - Ephemeris"]
    )
    fun getEphemerisTrackDtlByMstId(
        @Parameter(
            description = "추적 데이터 목록 ID", example = "1", required = true
        ) @PathVariable mstId: Long,  // ✅ UInt → Long 변경 (PassSchedule과 동일)
        @Parameter(
            description = "패스 인덱스", example = "0", required = true
        ) @PathVariable detailId: Int  // ✅ UInt → Int 변경 (PassSchedule과 동일)
    ): Mono<List<Map<String, Any?>>> {
        return Mono.fromCallable {
            ephemerisService.getEphemerisTrackDtlByMstIdAndDetailId(mstId, detailId)
        }
    }

    /**
     * 추적을 시작합니다.
     * ✅ mstId와 detailId를 사용하여 추적 시작 (PassSchedule과 동일한 구조)
     */
    @PostMapping("/tracking/start/{mstId}/pass/{detailId}")
    @Operation(
        operationId = "startephemeristrack", tags = ["Mode - Ephemeris"]
    )
    fun startEphemerisTracking(
        @Parameter(
            description = "추적 데이터 목록 ID", example = "1", required = true
        ) @PathVariable mstId: Long,  // ✅ UInt → Long 변경 (PassSchedule과 동일)
        @Parameter(
            description = "패스 인덱스", example = "0", required = true
        ) @PathVariable detailId: Int  // ✅ UInt → Int 변경 (PassSchedule과 동일)
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            // 추적 시작 (추적 데이터 시작)
            ephemerisService.startEphemerisTracking(mstId, detailId)
            // 초기 추적 데이터 전송
            //ephemerisService.sendInitialTrackingData(mstId, detailId)

            mapOf(
                "message" to "추적이 시작되었습니다.", "mstId" to mstId, "detailId" to detailId, "status" to "tracking"
            )
        }
    }

    /**
     * 추적을 중지합니다.
     */
    @PostMapping("/tracking/stop")
    @Operation(
        operationId = "stopephemeristrack", tags = ["Mode - Ephemeris"]
    )
    fun stopEphemerisTracking(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            ephemerisService.stopEphemerisTracking()
            mapOf(
                "message" to "추적이 중지되었습니다.", "status" to "stopped"
            )
        }
    }


    /**
     * 축 변환을 계산합니다.
     */
    @PostMapping("/calculate-axis-transform")
    @Operation(
        operationId = "calculateaxistransform", tags = ["Mode - Ephemeris"]
    )
    fun calculateAxisTransform(
        @Parameter(
            description = "축 변환 계산 요청 데이터 (azimuth, elevation, tilt, train)", required = true
        ) @RequestBody request: Map<String, Double>
    ): ResponseEntity<Map<String, Any>> {
        try {
            val azimuth = request["azimuth"] ?: 0.0
            val elevation = request["elevation"] ?: 0.0
            val tilt = request["tilt"] ?: 0.0
            val train = request["train"] ?: 0.0

            val result = ephemerisService.calculateAxisTransform(azimuth, elevation, tilt, train)

            return ResponseEntity.ok(result)
        } catch (error: Exception) {
            logger.error("축 변환 계산 API 오류: ${error.message}")
            return ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false, "error" to (error.message ?: "알 수 없는 오류"), "message" to "축 변환 계산 API 실패"
                )
            )
        }
    }

    

    /**
     * 특정 추적 데이터를 CSV 형식으로 내보냅니다.
     */
    @PostMapping("/export/csv/{mstId}")
    @Operation(
        operationId = "exportMstDataToCsv", tags = ["Mode - Ephemeris"]
    )
    fun exportMstDataToCsv(
        @Parameter(
            description = "추적 데이터 목록 ID", example = "1", required = true
        ) @PathVariable mstId: Int, 
        @Parameter(
            description = "CSV 내보내기 디렉토리 경로 (기본값: csv_exports)", example = "csv_exports", required = false
        ) @RequestParam(defaultValue = "csv_exports") outputDirectory: String
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val result = ephemerisService.exportMstDataToCsv(mstId, outputDirectory)

                if (result["success"] == true) {
                    mapOf(
                        "success" to true,
                        "message" to "MST ID $mstId 데이터가 CSV 형식으로 내보내졌습니다.",
                        "filename" to (result["filename"] ?: ""),
                        "filePath" to (result["filePath"] ?: ""),
                        "satelliteName" to (result["satelliteName"] ?: ""),
                        "originalDataCount" to (result["originalDataCount"] ?: 0),
                        "axisTransformedDataCount" to (result["axisTransformedDataCount"] ?: 0),
                        "finalTransformedDataCount" to (result["finalTransformedDataCount"] ?: 0)
                    )
                } else {
                    mapOf(
                        "success" to false,
                        "message" to "CSV 내보내기 실패: ${result["error"] ?: "알 수 없는 오류"}",
                        "error" to (result["error"] ?: "알 수 없는 오류")
                    )
                }
            } catch (e: Exception) {
                mapOf(
                    "success" to false,
                    "message" to "CSV 내보내기 실패: ${e.message}",
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }
    /**
     * 모든 추적 데이터를 CSV 형식으로 내보냅니다.
     */
    @PostMapping("/export/csv/all")
    @Operation(
        operationId = "exportallephemerisdata", tags = ["Mode - Ephemeris"]
    )
    fun exportAllMstDataToCsv(
        @Parameter(
            description = "CSV 내보내기 디렉토리 경로 (기본값: csv_exports)", example = "csv_exports", required = false
        ) @RequestParam(defaultValue = "csv_exports") outputDirectory: String
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val result = ephemerisService.exportAllMstDataToCsv(outputDirectory)

                if (result["success"] == true) {
                    mapOf(
                        "success" to true,
                        "message" to "모든 추적 데이터가 CSV 형식으로 내보내졌습니다.",
                        "totalMstCount" to (result["totalMstCount"] ?: 0),
                        "successCount" to (result["successCount"] ?: 0),
                        "errorCount" to (result["errorCount"] ?: 0),
                        "createdFiles" to (result["createdFiles"] ?: emptyList<String>()),
                        "outputDirectory" to (result["outputDirectory"] ?: outputDirectory)
                    )
                } else {
                    mapOf(
                        "success" to false,
                        "message" to "CSV 내보내기 실패: ${result["error"] ?: "알 수 없는 오류"}",
                        "error" to (result["error"] ?: "알 수 없는 오류")
                    )
                }
            } catch (e: Exception) {
                mapOf(
                    "success" to false,
                    "message" to "CSV 내보내기 중 오류 발생: ${e.message ?: "알 수 없는 오류"}",
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }

    // ❌ 현재 사용하지 않음.
    // @PostMapping("/realtime-data/clear")
    // @Operation(
    //     operationId = "clearephemerisdata", tags = ["Mode - Ephemeris"]
    // )
    // fun clearRealtimeTrackingData(): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         ephemerisService.clearRealtimeTrackingData()
    //         mapOf(
    //             "message" to "실시간 추적 데이터가 초기화되었습니다", "status" to "cleared"
    //         )
    //     }
    // }

    /**
     * 위성 위치를 계산합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @PostMapping("/position/current")
    // @Operation(
    //     operationId = "calculateephemerisangles", tags = ["Mode - Ephemeris"]
    // )
    // fun getCurrentPosition(
    //     @Parameter(
    //         description = "위성 위치 계산 요청 데이터", required = true
    //     ) @RequestBody request: SatellitePositionRequest
    // ): Mono<SatelliteTrackData> {
    //     return Mono.fromCallable {
    //         orekitCalculator.getCurrentPosition(
    //             request.tleLine1, request.tleLine2, request.latitude, request.longitude, request.altitude
    //         )
    //     }
    // }

    /**
     * 특정 시간의 위성 위치를 계산합니다.
     */
    // ❌ 현재 사용하지 않음.
    //  @PostMapping("/position/at-time")
    // @Operation(
    //     operationId = "calculateephemerisanglesattime", tags = ["Mode - Ephemeris"]
    // )
    // fun getPositionAtTime(
    //     @Parameter(
    //         description = "특정 시간 위성 위치 계산 요청 데이터", required = true
    //     ) @RequestBody request: SatellitePositionTimeRequest
    // ): Mono<SatelliteTrackData> {
    //     return Mono.fromCallable {
    //         orekitCalculator.calculatePosition(
    //             request.tleLine1,
    //             request.tleLine2,
    //             request.dateTime,
    //             request.latitude,
    //             request.longitude,
    //             request.altitude
    //         )
    //     }
    // }

    /**
     * 추적 스케줄을 생성합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @PostMapping("/tracking/schedule")
    // @Operation(
    //     operationId = "generateephemeristrackingschedule", tags = ["Mode - Ephemeris"]
    // )
    // fun generateTrackingSchedule(
    //     @Parameter(
    //         description = "추적 스케줄 생성 요청 데이터", required = true
    //     ) @RequestBody request: SatelliteTrackingScheduleRequest
    // ): Mono<OrekitCalculator.SatelliteTrackingSchedule> {
    //     return Mono.fromCallable {
    //         orekitCalculator.generateSatelliteTrackingSchedule(
    //             request.tleLine1,
    //             request.tleLine2,
    //             request.startDate,
    //             request.durationDays,
    //             request.minElevation,
    //             request.latitude,
    //             request.longitude,
    //             request.altitude,
    //             request.trackingIntervalMs
    //         )
    //     }
    // }

    /**
     * 추적 상태를 확인합니다.
     */
    // ❌ 현재 사용하지 않음.
    //  @GetMapping("/tracking/status")
    // @Operation(
    //     operationId = "getephemerisstatus", tags = ["Mode - Ephemeris"]
    // )
    // fun getTrackingStatus(): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val isTracking = ephemerisService.isTracking()
    //         val currentPass = ephemerisService.getCurrentTrackingPass()

    //         if (isTracking && currentPass != null) {
    //             mapOf(
    //                 "status" to "tracking",
    //                 "passId" to (currentPass["No"] ?: "unknown"),
    //                 "satelliteName" to (currentPass["SatelliteName"] ?: "unknown"),
    //                 "startTime" to (currentPass["StartTime"] ?: "unknown"),
    //                 "endTime" to (currentPass["EndTime"] ?: "unknown")
    //             )
    //         } else {
    //             mapOf(
    //                 "status" to "idle"
    //             )
    //         }
    //     }
    // }
    //참고용 데이터 조회 API 추가

    /**
     * 원본 추적 데이터를 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/master/original")
    // @Operation(
    //     operationId = "getoriginalephemerisdata", tags = ["Mode - Ephemeris"]
    // )
    // fun getOriginalEphemerisTrackMst(): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val originalMst = ephemerisService.getOriginalEphemerisTrackMst()
    //         mapOf(
    //             "dataType" to "original",
    //             "description" to "원본 추적 데이터 목록",
    //             "count" to originalMst.size,
    //             "data" to originalMst
    //         )
    //     }
    // }

    /**
     * 축 변환된 추적 데이터를 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/master/axis-transformed")
    // @Operation(
    //     operationId = "getaxistransformedephemerisdata", tags = ["Mode - Ephemeris"]
    // )
    // fun getAxisTransformedEphemerisTrackMst(): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val axisTransformedMst = ephemerisService.getAxisTransformedEphemerisTrackMst()
    //         mapOf(
    //             "dataType" to "axis_transformed",
    //             "description" to "축 변환된 추적 데이터 목록",
    //             "count" to axisTransformedMst.size,
    //             "data" to axisTransformedMst,
    //             "transformationInfo" to mapOf(
    //                 "tiltAngle" to -6.98, "rotatorAngle" to 0.0, "transformationType" to "axis_transform"
    //             )
    //         )
    //     }
    // }

    /**
     * 최종 변환된 추적 데이터를 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/master/final-transformed")
    // @Operation(
    //     operationId = "getfinaltransformedephemerisdata", tags = ["Mode - Ephemeris"]
    // )
    // fun getFinalTransformedEphemerisTrackMst(): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val finalTransformedMst = ephemerisService.getFinalTransformedEphemerisTrackMst()
    //         mapOf(
    //             "dataType" to "final_transformed",
    //             "description" to "최종 변환된 추적 데이터 목록",
    //             "count" to finalTransformedMst.size,
    //             "data" to finalTransformedMst,
    //             "transformationInfo" to mapOf(
    //                 "tiltAngle" to -6.98,
    //                 "rotatorAngle" to 0.0,
    //                 "transformationType" to "final_transform",
    //                 "angleLimit" to "270도"
    //             )
    //         )
    //     }
    // }

    /**
     * 추적 데이터 상세 정보를 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/detail/{mstId}/original")
    // @Operation(
    //     operationId = "getoriginalephemerisdetail", tags = ["Mode - Ephemeris"]
    // )
    // fun getOriginalEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val originalDtl = ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "original")
    //         mapOf(
    //             "mstId" to mstId,
    //             "dataType" to "original",
    //             "description" to "원본 추적 데이터 상세",
    //             "count" to originalDtl.size,
    //             "data" to originalDtl
    //         )
    //     }
    // }

    /**
     * 축 변환된 추적 데이터 상세 정보를 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/detail/{mstId}/axis-transformed")
    // @Operation(
    //     operationId = "getaxistransformedephemerisdetail", tags = ["Mode - Ephemeris"]
    // )
    // fun getAxisTransformedEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val axisTransformedDtl = ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "axis_transformed")
    //         mapOf(
    //             "mstId" to mstId,
    //             "dataType" to "axis_transformed",
    //             "description" to "축 변환된 추적 데이터 상세",
    //             "count" to axisTransformedDtl.size,
    //             "data" to axisTransformedDtl,
    //             "transformationInfo" to mapOf(
    //                 "tiltAngle" to -6.98, "rotatorAngle" to 0.0, "transformationType" to "axis_transform"
    //             )
    //         )
    //     }
    // }

    /**
     * 최종 변환된 추적 데이터 상세 정보를 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/detail/{mstId}/final-transformed")
    // @Operation(
    //     operationId = "getfinaltransformedephemerisdetail", tags = ["Mode - Ephemeris"]
    // )
    // fun getFinalTransformedEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val finalTransformedDtl =
    //             ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "final_transformed")
    //         mapOf(
    //             "mstId" to mstId,
    //             "dataType" to "final_transformed",
    //             "description" to "최종 변환된 추적 데이터 상세",
    //             "count" to finalTransformedDtl.size,
    //             "data" to finalTransformedDtl,
    //             "transformationInfo" to mapOf(
    //                 "tiltAngle" to -6.98,
    //                 "rotatorAngle" to 0.0,
    //                 "transformationType" to "final_transform",
    //                 "angleLimit" to "270도"
    //             )
    //         )
    //     }
    // }

    /**
     * 데이터 타입별 추적 목록을 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/master/by-type/{dataType}")
    // @Operation(
    //     operationId = "getephemerislistbytype", tags = ["Mode - Ephemeris"]
    // )
    // fun getEphemerisTrackMstByDataType(@PathVariable dataType: String): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val mstData = ephemerisService.getEphemerisTrackMstByDataType(dataType)
    //         mapOf(
    //             "dataType" to dataType,
    //             "count" to mstData.size,
    //             "data" to mstData,
    //             "availableDataTypes" to listOf("original", "axis_transformed", "final_transformed")
    //         )
    //     }
    // }

    /**
     * 데이터 타입별 추적 상세 정보를 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/detail/by-type/{dataType}")
    // @Operation(
    //     operationId = "getephemerisdetailbytype", tags = ["Mode - Ephemeris"]
    // )
    // fun getEphemerisTrackDtlByDataType(@PathVariable dataType: String): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val dtlData = ephemerisService.getEphemerisTrackDtlByDataType(dataType)
    //         mapOf(
    //             "dataType" to dataType,
    //             "count" to dtlData.size,
    //             "data" to dtlData,
    //             "availableDataTypes" to listOf("original", "axis_transformed", "final_transformed")
    //         )
    //     }
    // }

    /**
     * 모든 변환 요약을 조회합니다.
     */
    // ❌ 현재 사용하지 않음.
    // @GetMapping("/summary/all-transformations")
    // @Operation(
    //     operationId = "getalltransformationsummary", tags = ["Mode - Ephemeris"]
    // )
    // fun getAllTransformationSummary(): Mono<Map<String, Any>> {
    //     return Mono.fromCallable {
    //         val originalMst = ephemerisService.getOriginalEphemerisTrackMst()
    //         val axisTransformedMst = ephemerisService.getAxisTransformedEphemerisTrackMst()
    //         val finalTransformedMst = ephemerisService.getFinalTransformedEphemerisTrackMst()

    //         mapOf(
    //             "summary" to mapOf(
    //                 "original" to mapOf(
    //                     "mstCount" to originalMst.size, "description" to "원본 추적 목록"
    //                 ), "axisTransformed" to mapOf(
    //                     "mstCount" to axisTransformedMst.size,
    //                     "description" to "축 변환된 추적 목록",
    //                     "tiltAngle" to -6.98,
    //                     "rotatorAngle" to 0.0
    //                 ), "finalTransformed" to mapOf(
    //                     "mstCount" to finalTransformedMst.size, "description" to "최종 변환된 추적 목록", "angleLimit" to "270도"
    //                 )
    //             ),
    //             "totalMstCount" to (originalMst.size + axisTransformedMst.size + finalTransformedMst.size),
    //             "transformationSteps" to listOf(
    //                 "1. 추적 데이터 생성", "2. 축 변환 (회전각 -6.98도)", "3. 최종 변환 (270도 제한)", "4. 추적 데이터 저장"
    //             )
    //         )
    //     }
    // }

    /**
     * CSV 데이터 생성 함수 (실시간 추적 데이터 생성 시 사용)
     */
    // ❌ 현재 사용하지 않음.
    //private fun generateRealtimeTrackingCsv(data: List<Map<String, Any?>>): ByteArray {
    //    val outputStream = ByteArrayOutputStream()
    //    val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
    //
    //    try {
    //        // UTF-8 BOM 추가 (Excel에서 한글 깨짐 방지)
    //        outputStream.write(0xEF)
    //        outputStream.write(0xBB)
    //        outputStream.write(0xBF)
    //
    //        // CSV 헤더 작성 (createRealtimeTrackingData에서 사용된 필드 순서)
    //        val headers = listOf(
    //            "Index",
    //            "TheoreticalIndex",
    //            "Timestamp",
    //            "PassId",
    //            "ElapsedTime(s)",
    //
    //            // 원본 데이터 (변환 전)
    //            "OriginalAzimuth(도)",
    //            "OriginalElevation(도)",
    //            "OriginalRange(km)",
    //            "OriginalAltitude(km)",
    //
    //            // 축 변환된 데이터 (축 변환 후)
    //            "AxisTransformedAzimuth(도)",
    //            "AxisTransformedElevation(도)",
    //            "AxisTransformedRange(km)",
    //            "AxisTransformedAltitude(km)",
    //
    //            // 최종 변환된 데이터 (최종 변환 후)
    //            "FinalTransformedAzimuth(도)",
    //            "FinalTransformedElevation(도)",
    //            "FinalTransformedRange(km)",
    //            "FinalTransformedAltitude(km)",
    //
    //            // 명령 데이터
    //            "CmdAzimuth(도)",
    //            "CmdElevation(도)",
    //            "ActualAzimuth(도)",
    //            "ActualElevation(도)",
    //
    //            // 추적 데이터
    //            "TrackingAzimuthTime",
    //            "TrackingCMDAzimuth(도)",
    //            "TrackingActualAzimuth(도)",
    //            "TrackingElevationTime",
    //            "TrackingCMDElevation(도)",
    //            "TrackingActualElevation(도)",
    //            "TrackingTiltTime",
    //            "TrackingCMDTilt(도)",
    //            "TrackingActualTilt(도)",
    //
    //            // 오차 데이터
    //            "AzimuthError(도)",
    //            "ElevationError(도)",
    //            "OriginalToAxisTransformationError(도)",
    //            "AxisToFinalTransformationError(도)",
    //            "TotalTransformationError(도)",
    //
    //            // 정확도 데이터
    //            "TimeAccuracy(s)",
    //            "AzCmdAccuracy(도)",
    //            "AzActAccuracy(도)",
    //            "AzFinalAccuracy(도)",
    //            "ElCmdAccuracy(도)",
    //            "ElActAccuracy(도)",
    //            "ElFinalAccuracy(도)",
    //
    //            // 변환 정보
    //            "TiltAngle(도)",
    //            "TransformationType",
    //            "HasTransformation",
    //            "InterpolationMethod",
    //            "InterpolationAccuracy",
    //            "HasValidData",
    //            "DataSource"
    //        )
    //
    //        writer.write(headers.joinToString(","))
    //        writer.write("\n")
    //
    //        // 데이터 행 작성
    //        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    //
    //        data.forEach { record ->
    //            val row = listOf(
    //                record["index"]?.toString() ?: "",
    //                record["theoreticalIndex"]?.toString() ?: "",  // ? 데이터 타입 정의
    //                (record["timestamp"] as? ZonedDateTime)?.format(dateFormatter) ?: "",
    //                record["passId"]?.toString() ?: "",
    //                String.format("%.3f", record["elapsedTimeSeconds"] as? Float ?: 0.0f),
    //
    //                // 원본 데이터
    //                String.format("%.6f", record["originalAzimuth"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["originalElevation"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["originalRange"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["originalAltitude"] as? Float ?: 0.0f),
    //
    //                // 축 변환된 데이터
    //                String.format("%.6f", record["axisTransformedAzimuth"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["axisTransformedElevation"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["axisTransformedRange"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["axisTransformedAltitude"] as? Float ?: 0.0f),
    //
    //                // 최종 변환된 데이터
    //                String.format("%.6f", record["finalTransformedAzimuth"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["finalTransformedElevation"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["finalTransformedRange"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["finalTransformedAltitude"] as? Float ?: 0.0f),
    //
    //                // 명령 데이터
    //                String.format("%.6f", record["cmdAz"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["cmdEl"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["actualAz"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["actualEl"] as? Float ?: 0.0f),
    //
    //                // 추적 데이터
    //                record["trackingAzimuthTime"]?.toString() ?: "",
    //                String.format("%.6f", record["trackingCMDAzimuthAngle"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["trackingActualAzimuthAngle"] as? Float ?: 0.0f),
    //                record["trackingElevationTime"]?.toString() ?: "",
    //                String.format("%.6f", record["trackingCMDElevationAngle"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["trackingActualElevationAngle"] as? Float ?: 0.0f),
    //                record["trackingTiltTime"]?.toString() ?: "",
    //                String.format("%.6f", record["trackingCMDTiltAngle"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["trackingActualTiltAngle"] as? Float ?: 0.0f),
    //
    //                // 오차 데이터
    //                String.format("%.6f", record["azimuthError"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["elevationError"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["originalToAxisTransformationError"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["axisToFinalTransformationError"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["totalTransformationError"] as? Float ?: 0.0f),
    //
    //                // 정확도 데이터
    //                String.format("%.6f", record["timeAccuracy"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["azCmdAccuracy"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["azActAccuracy"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["azFinalAccuracy"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["elCmdAccuracy"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["elActAccuracy"] as? Float ?: 0.0f),
    //                String.format("%.6f", record["elFinalAccuracy"] as? Float ?: 0.0f),
    //
    //                // 변환 정보
    //                String.format("%.6f", record["tiltAngle"] as? Double ?: 0.0),
    //                "\"${record["transformationType"] ?: ""}\"",
    //                (record["hasTransformation"] as? Boolean ?: false).toString(),
    //                "\"${record["interpolationMethod"] ?: ""}\"",
    //                String.format("%.6f", record["interpolationAccuracy"] as? Double ?: 0.0),
    //                (record["hasValidData"] as? Boolean ?: false).toString(),
    //                "\"${record["dataSource"] ?: ""}\""
    //            )
    //
    //            writer.write(row.joinToString(","))
    //            writer.write("\n")
    //        }
    //
    //        writer.flush()
    //        return outputStream.toByteArray()
    //
    //    } finally {
    //        writer.close()
    //        outputStream.close()
    //    }
    //}

    @GetMapping("/tracking/mst/comparison")
    @Operation(
        operationId = "getAllEphemerisTrackMstWithComparison", 
        tags = ["Mode - Ephemeris"],
        summary = "Original과 Final Transformed 데이터 비교 조회",
        description = "UI에서 Original(2축)과 Final Transformed 데이터를 동시에 표시하기 위한 API"
    )
    fun getAllEphemerisTrackMstWithComparison(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val comparisonData = ephemerisService.getAllEphemerisTrackMstWithComparison()
                mapOf(
                    "status" to "success",
                    "data" to comparisonData,
                    "message" to "비교 데이터 조회 완료"
                )
            } catch (e: Exception) {
                mapOf(
                    "status" to "error",
                    "message" to "비교 데이터 조회 실패: ${e.message}",
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }

    /**
     * ✅ Original과 FinalTransformed 데이터를 병합하여 반환
     * UI 테이블에서 2축/최종변환 데이터를 동시에 표시하기 위한 API
     */
    @GetMapping("/tracking/mst/merged")
    @Operation(
        operationId = "getAllEphemerisTrackMstMerged",
        tags = ["Mode - Ephemeris"],
        summary = "Original과 FinalTransformed 병합 데이터 조회",
        description = "UI 테이블에서 2축(Original)과 최종변환(FinalTransformed) 메타데이터를 동시에 표시하기 위한 API"
    )
    fun getAllEphemerisTrackMstMerged(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val mergedData = ephemerisService.getAllEphemerisTrackMstMerged()
                
                // 🔍 디버깅: Keyhole이 있는 항목의 최적화 데이터 확인
                mergedData.forEach { item ->
                    val isKeyhole = item["IsKeyhole"] as? Boolean ?: false
                    if (isKeyhole) {
                        // ✅ No 필드 또는 MstId 필드에서 mstId 추출 (PassSchedule과 동일한 방식)
                        val mstId = when {
                            item["No"] != null -> (item["No"] as? Number)?.toLong()
                            item["MstId"] != null -> (item["MstId"] as? Number)?.toLong()
                            else -> null
                        }
                        logger.info("🔍 [API 응답] MST #$mstId 최적화 데이터:")
                        logger.info("   - IsKeyhole: $isKeyhole")
                        logger.info("   - KeyholeOptimizedRecommendedTrainAngle: ${item["KeyholeOptimizedRecommendedTrainAngle"]}")
                        logger.info("   - KeyholeOptimizedFinalTransformedMaxAzRate: ${item["KeyholeOptimizedFinalTransformedMaxAzRate"]}")
                        logger.info("   - KeyholeOptimizedFinalTransformedMaxElRate: ${item["KeyholeOptimizedFinalTransformedMaxElRate"]}")
                        logger.info("   - OptimizationImprovement: ${item["OptimizationImprovement"]}")
                        logger.info("   - OptimizationImprovementRate: ${item["OptimizationImprovementRate"]}")
                    }
                }
                
                mapOf(
                    "status" to "success",
                    "data" to mergedData,
                    "count" to mergedData.size,
                    "message" to "병합 데이터 조회 완료"
                )
            } catch (e: Exception) {
                logger.error("❌ API 응답 생성 실패: ${e.message}", e)
                mapOf(
                    "status" to "error",
                    "message" to "병합 데이터 조회 실패: ${e.message}",
                    "error" to (e.message ?: "알 수 없는 오류")
                )
            }
        }
    }

}

/**
 * 위성 위치 요청 데이터
 */
data class SatellitePositionRequest(
    val tleLine1: String, val tleLine2: String, val latitude: Double, val longitude: Double, val altitude: Double = 0.0
)

/**
 * 위성 위치 요청 데이터
 */
data class SatellitePositionTimeRequest(
    val tleLine1: String,
    val tleLine2: String,
    val dateTime: ZonedDateTime,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
)

/**
 * 추적 경로 요청 데이터
 */
data class SatelliteTrackingPathRequest(
    val tleLine1: String,
    val tleLine2: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val interval: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
)

/**
 * 가시성 요청 데이터
 */
data class SatelliteVisibilityRequest(
    val tleLine1: String,
    val tleLine2: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val interval: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val minElevation: Float = 0.0f
)

/**
 * 가시성 기간 데이터
 */
data class VisibilityPeriod(
    val startTime: ZonedDateTime, val endTime: ZonedDateTime
)

/**
 * 추적 스케줄 요청 데이터
 */
data class SatelliteTrackingScheduleRequest(
    val tleLine1: String,
    val tleLine2: String,
    val startDate: ZonedDateTime = ZonedDateTime.now(),
    val durationDays: Int = 1,
    val minElevation: Float = 0.0f,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val trackingIntervalMs: Int = 100 // 기본값 100ms
)

/**
 * 추적 데이터 요청 데이터
 */
data class EphemerisTrackRequest(
    val tleLine1: String, val tleLine2: String, val satelliteName: String? = null
)

/**
 * 추적 데이터 요청 데이터
 */
data class EphemerisTrackWithTrainRequest(
    val tleLine1: String,
    val tleLine2: String,
    val satelliteName: String? = null,
    val tiltAngle: Double = -7.0  // 기본값 회전각
)

/**
 * 정지궤도 추적 요청 데이터
 */
data class GeostationaryTrackingRequest(
    val tleLine1: String, val tleLine2: String
)





