package com.gtlsystems.acs_api.controller.mode

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.model.SatelliteTrackData
import com.gtlsystems.acs_api.service.mode.EphemerisService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ? �߰� �ʿ��� import��
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/ephemeris")
@Tag(name = "Mode - Ephemeris", description = "���� �˵� ���� API - TLE ���� ���� ��ġ ����, ���� ���� ����, ���ü� �м�")
class EphemerisController(
    private val orekitCalculator: OrekitCalculator,
    private val ephemerisService: EphemerisService
)
{
    private val logger = LoggerFactory.getLogger(EphemerisController::class.java)

    @PostMapping("/3axis/tracking/geostationary/start")
    @Operation(
        tags = ["Mode - Ephemeris"]
    )
    fun startGeostationaryTracking(
        @Parameter(
            description = "�����˵� ���� ���� ��û ������",
            required = true
        )
        @RequestBody request: GeostationaryTrackingRequest
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                ephemerisService.startGeostationaryTracking(request.tleLine1, request.tleLine2)
                
                mapOf(
                    "message" to "�����˵� ���� ������ ���۵Ǿ����ϴ�.",
                    "satelliteId" to request.tleLine1.substring(2, 7).trim(),
                    "trackingType" to "geostationary"
                )
            } catch (e: Exception) {
                mapOf(
                    "message" to "�����˵� ���� ���� ���� ����: ${e.message}",
                    "error" to (e.message ?: "�� �� ���� ����")
                )
            }
        }
    }

    @PostMapping("/3axis/tracking/geostationary/calculate-angles")
    @Operation(
        operationId = "calculategeostationaryangles",
        tags = ["Mode - Ephemeris"]
    )
    fun calculateGeostationaryAngles(
        @Parameter(
            description = "�����˵� ���� ���� ���� ��û ������",
            required = true
        )
        @RequestBody request: GeostationaryTrackingRequest
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val geoPosition = ephemerisService.getCurrentGeostationaryPositionWith3AxisTransform(
                    request.tleLine1, 
                    request.tleLine2
                )
                
                mapOf(
                    "message" to "�����˵� ���� ���� �Ϸ�",
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
                    "message" to "�����˵� ���� ���� ����: ${e.message}",
                    "error" to (e.message ?: "�� �� ���� ����")
                )
            }
        }
    }

    /**
     * �ǽð� ���� ������ ��ȸ (JSON)
     */
    @GetMapping("/tracking/realtime-data")
    @Operation(
        operationId = "getephemerisdata",
        tags = ["Mode - Ephemeris"]
    )
    fun getRealtimeTrackingData(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val realtimeData = ephemerisService.getRealtimeTrackingData()
            val stats = ephemerisService.getRealtimeTrackingStats()

            mapOf(
                "totalCount" to realtimeData.size,
                "data" to realtimeData,
                "statistics" to stats
            )
        }
    }
    /**
     * �ǽð� ���� ������ �ʱ�ȭ
     */
    @PostMapping("/realtime-data/clear")
    @Operation(
        operationId = "clearephemerisdata",
        tags = ["Mode - Ephemeris"]
    )
    fun clearRealtimeTrackingData(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            ephemerisService.clearRealtimeTrackingData()
            mapOf(
                "message" to "�ǽð� ���� �����Ͱ� �ʱ�ȭ�Ǿ����ϴ�",
                "status" to "cleared"
            )
        }
    }

    @PostMapping("/set-current-tracking-pass-id")
    fun setCurrentTrackingPassId(@RequestParam passId: UInt?): ResponseEntity<Map<String, String>> {
        return try {
            ephemerisService.setCurrentTrackingPassId(passId)
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "setCurrentTrackingPassId ������ ���������� ���۵Ǿ����ϴ�",
                "command" to "setCurrentTrackingPassId",
                "passId" to passId.toString()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "setCurrentTrackingPassId ���� ���� ����: ${e.message}"
            ))
        }
    }
    @PostMapping("/time-offset-command")
    fun timeOffsetCommand(@RequestParam inputTimeOffset: Float): ResponseEntity<Map<String, String>> {
        return try {
            ephemerisService.ephemerisTimeOffsetCommand(inputTimeOffset)
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "TimeOffset ������ ���������� ���۵Ǿ����ϴ�",
                "command" to "TimeOffset",
                "timeOffset" to inputTimeOffset.toString()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "TimeOffset ���� ���� ����: ${e.message}"
            ))
        }
    }
    /**
     * ���� �ð��� ���� ��ġ�� �����մϴ�.
     */
    @PostMapping("/position/current")
    @Operation(
        operationId = "calculateephemerisangles",
        tags = ["Mode - Ephemeris"]
    )
    fun getCurrentPosition(
        @Parameter(
            description = "���� ��ġ ���� ��û ������",
            required = true
        )
        @RequestBody request: SatellitePositionRequest
    ): Mono<SatelliteTrackData> {
        return Mono.fromCallable {
            orekitCalculator.getCurrentPosition(
                request.tleLine1,
                request.tleLine2,
                request.latitude,
                request.longitude,
                request.altitude
            )
        }
    }

    /**
     * ������ �ð��� ���� ��ġ�� �����մϴ�.
     */
    @PostMapping("/position/at-time")
    @Operation(
        operationId = "calculateephemerisanglesattime",
        tags = ["Mode - Ephemeris"]
    )
    fun getPositionAtTime(
        @Parameter(
            description = "���� �ð� ���� ��ġ ���� ��û ������",
            required = true
        )
        @RequestBody request: SatellitePositionTimeRequest
    ): Mono<SatelliteTrackData> {
        return Mono.fromCallable {
            orekitCalculator.calculatePosition(
                request.tleLine1,
                request.tleLine2,
                request.dateTime,
                request.latitude,
                request.longitude,
                request.altitude
            )
        }
    }
    /**
     * ���� ���� �������� �����մϴ�.
     */
    @PostMapping("/tracking/schedule")
    @Operation(
        operationId = "generateephemeristrackingschedule",
        tags = ["Mode - Ephemeris"]
    )
    fun generateTrackingSchedule(
        @Parameter(
            description = "���� ���� ������ ���� ��û ������",
            required = true
        )
        @RequestBody request: SatelliteTrackingScheduleRequest
    ): Mono<OrekitCalculator.SatelliteTrackingSchedule> {
        return Mono.fromCallable {
            orekitCalculator.generateSatelliteTrackingSchedule(
                request.tleLine1,
                request.tleLine2,
                request.startDate,
                request.durationDays,
                request.minElevation,
                request.latitude,
                request.longitude,
                request.altitude,
                request.trackingIntervalMs
            )
        }
    }
    /**
     * TLE �����ͷ� ���� �˵� ���� �����͸� �����մϴ�.
     */
    @PostMapping("/tracking/generate")
    @Operation(
        operationId = "generateephemeristrack",
        tags = ["Mode - Ephemeris"]
    )
    fun generateEphemerisTrack(
        @Parameter(
            description = "���� �˵� ���� ������ ���� ��û",
            required = true
        )
        @RequestBody request: EphemerisTrackRequest
    ): Mono<Map<String, Any>> {
        return ephemerisService.generateEphemerisDesignationTrackAsync(
            request.tleLine1,
            request.tleLine2,
            request.satelliteName
        )
            .map { (mstData, dtlData) ->
                mapOf<String, Any>(  // ? ������ Ÿ�� ����
                    "message" to "���� �˵� ���� ������ ���� �Ϸ�",
                    "mstCount" to mstData.size,
                    "dtlCount" to dtlData.size
                )
            }
            .onErrorReturn(
                mapOf<String, Any>(  // ? ������ Ÿ�� ����
                    "message" to "���� �˵� ���� ������ ���� ����",
                    "error" to "���� �� ������ �߻��߽��ϴ�"
                )
            )
    }


    /**
     * ���� ���� ���� ������ �����͸� ��ȸ�մϴ�.
     */
    @GetMapping("/master")
    @Operation(
        operationId = "getephemerislist",
        tags = ["Mode - Ephemeris"]
    )
    fun getAllEphemerisTrackMst(): Mono<List<Map<String, Any?>>> {
        return Mono.fromCallable {
            ephemerisService.getFinalTransformedEphemerisTrackMst()
        }
    }

    /**
     * Ư�� ������ ID�� �ش��ϴ� ���� ���� �����͸� ��ȸ�մϴ�.
     */
    @GetMapping("/detail/{mstId}")
    @Operation(
        operationId = "getephemerisdetail",
        tags = ["Mode - Ephemeris"]
    )
    fun getEphemerisTrackDtlByMstId(
        @Parameter(
            description = "������ ������ ID",
            example = "1",
            required = true
        )
        @PathVariable mstId: UInt
    ): Mono<List<Map<String, Any?>>> {
        return Mono.fromCallable {
            ephemerisService.getEphemerisTrackDtlByMstId(mstId)
        }
    }

    /**
     * ���� ������ �����մϴ�.
     * ���� ���� ���� �� �ʱ� ���� ������ ������ �����մϴ�.
     */
    @PostMapping("/tracking/start/{passId}")
    @Operation(
        operationId = "startephemeristrack",
        tags = ["Mode - Ephemeris"]
    )
    fun startEphemerisTracking(
        @Parameter(
            description = "���� ���� ID",
            example = "1",
            required = true
        )
        @PathVariable passId: UInt
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            // ���� ���� ���� (���� ���� ����)
            ephemerisService.startEphemerisTracking(passId)
            // �ʱ� ���� ������ ����
            //ephemerisService.sendInitialTrackingData(passId)

            mapOf(
                "message" to "���� ������ ���۵Ǿ����ϴ�.",
                "passId" to passId,
                "status" to "tracking"
            )
        }
    }

    /**
     * ���� ������ �����մϴ�.
     */
    @PostMapping("/tracking/stop")
    @Operation(
        operationId = "stopephemeristrack",
        tags = ["Mode - Ephemeris"]
    )
    fun stopEphemerisTracking(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            ephemerisService.stopEphemerisTracking()
            mapOf(
                "message" to "���� ������ �����Ǿ����ϴ�.",
                "status" to "stopped"
            )
        }
    }



    /**
     * ���� ���� ���¸� Ȯ���մϴ�.
     */
    @GetMapping("/tracking/status")
    @Operation(
        operationId = "getephemerisstatus",
        tags = ["Mode - Ephemeris"]
    )
    fun getTrackingStatus(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val isTracking = ephemerisService.isTracking()
            val currentPass = ephemerisService.getCurrentTrackingPass()

            if (isTracking && currentPass != null) {
                mapOf(
                    "status" to "tracking",
                    "passId" to (currentPass["No"] ?: "unknown"),
                    "satelliteName" to (currentPass["SatelliteName"] ?: "unknown"),
                    "startTime" to (currentPass["StartTime"] ?: "unknown"),
                    "endTime" to (currentPass["EndTime"] ?: "unknown")
                )
            } else {
                mapOf(
                    "status" to "idle"
                )
            }
        }
    }

    /**
     * 3�� ��ȯ ���� API
     */
    @PostMapping("/calculate-axis-transform")
    @Operation(
        operationId = "calculateaxistransform",
        tags = ["Mode - Ephemeris"]
    )
    fun calculateAxisTransform(
        @Parameter(
            description = "3�� ��ȯ ���� ��û ������ (azimuth, elevation, tilt, rotator)",
            required = true
        )
        @RequestBody request: Map<String, Double>
    ): ResponseEntity<Map<String, Any>> {
        try {
            val azimuth = request["azimuth"] ?: 0.0
            val elevation = request["elevation"] ?: 0.0
            val tilt = request["tilt"] ?: 0.0
            val rotator = request["rotator"] ?: 0.0

            val result = ephemerisService.calculateAxisTransform(azimuth, elevation, tilt, rotator)
            
            return ResponseEntity.ok(result)
        } catch (error: Exception) {
            logger.error("3�� ��ȯ ���� API ����: ${error.message}")
            return ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "error" to (error.message ?: "�� �� ���� ����"),
                "message" to "3�� ��ȯ ���� API ȣ�⿡ �����߽��ϴ�"
            ))
        }
    }

    // ? ���ο� ������ Ÿ�Ժ� ��ȸ API�� �߰�

    /**
     * ���� ������ ������ ��ȸ API
     * ��ȯ �� ���� ���� ���� �����͸� ��ȸ�մϴ�.
     */
    @GetMapping("/master/original")
    @Operation(
        operationId = "getoriginalephemerisdata",
        tags = ["Mode - Ephemeris"]
    )
    fun getOriginalEphemerisTrackMst(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val originalMst = ephemerisService.getOriginalEphemerisTrackMst()
            mapOf(
                "dataType" to "original",
                "description" to "��ȯ �� ���� ���� ���� ������",
                "count" to originalMst.size,
                "data" to originalMst
            )
        }
    }

    /**
     * �ຯȯ ������ ������ ��ȸ API
     * ������ ��ȯ�� ������ ���� ���� �����͸� ��ȸ�մϴ�.
     */
    @GetMapping("/master/axis-transformed")
    @Operation(
        operationId = "getaxistransformedephemerisdata",
        tags = ["Mode - Ephemeris"]
    )
    fun getAxisTransformedEphemerisTrackMst(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val axisTransformedMst = ephemerisService.getAxisTransformedEphemerisTrackMst()
            mapOf(
                "dataType" to "axis_transformed",
                "description" to "������ ��ȯ�� ������ ���� ���� ������",
                "count" to axisTransformedMst.size,
                "data" to axisTransformedMst,
                "transformationInfo" to mapOf(
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "axis_transform"
                )
            )
        }
    }

    /**
     * ���� ��ȯ ������ ������ ��ȸ API
     * ������ ��ȯ���� ������ ���� ���� ���� �����͸� ��ȸ�մϴ�.
     */
    @GetMapping("/master/final-transformed")
    @Operation(
        operationId = "getfinaltransformedephemerisdata",
        tags = ["Mode - Ephemeris"]
    )
    fun getFinalTransformedEphemerisTrackMst(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val finalTransformedMst = ephemerisService.getFinalTransformedEphemerisTrackMst()
            mapOf(
                "dataType" to "final_transformed",
                "description" to "������ ��ȯ���� ������ ���� ���� ���� ������",
                "count" to finalTransformedMst.size,
                "data" to finalTransformedMst,
                "transformationInfo" to mapOf(
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "final_transform",
                    "angleLimit" to "��270��"
                )
            )
        }
    }

    /**
     * Ư�� ������ ID�� ���� ���� ������ ��ȸ API
     */
    @GetMapping("/detail/{mstId}/original")
    @Operation(
        operationId = "getoriginalephemerisdetail",
        tags = ["Mode - Ephemeris"]
    )
    fun getOriginalEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val originalDtl = ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "original")
            mapOf(
                "mstId" to mstId,
                "dataType" to "original",
                "description" to "��ȯ �� ���� ���� ���� ���� ������",
                "count" to originalDtl.size,
                "data" to originalDtl
            )
        }
    }

    /**
     * Ư�� ������ ID�� �ຯȯ ���� ������ ��ȸ API
     */
    @GetMapping("/detail/{mstId}/axis-transformed")
    @Operation(
        operationId = "getaxistransformedephemerisdetail",
        tags = ["Mode - Ephemeris"]
    )
    fun getAxisTransformedEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val axisTransformedDtl = ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "axis_transformed")
            mapOf(
                "mstId" to mstId,
                "dataType" to "axis_transformed",
                "description" to "������ ��ȯ�� ������ ���� ���� ���� ������",
                "count" to axisTransformedDtl.size,
                "data" to axisTransformedDtl,
                "transformationInfo" to mapOf(
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "axis_transform"
                )
            )
        }
    }

    /**
     * Ư�� ������ ID�� ���� ��ȯ ���� ������ ��ȸ API
     */
    @GetMapping("/detail/{mstId}/final-transformed")
    @Operation(
        operationId = "getfinaltransformedephemerisdetail",
        tags = ["Mode - Ephemeris"]
    )
    fun getFinalTransformedEphemerisTrackDtlByMstId(@PathVariable mstId: UInt): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val finalTransformedDtl = ephemerisService.getEphemerisTrackDtlByMstIdAndDataType(mstId, "final_transformed")
            mapOf(
                "mstId" to mstId,
                "dataType" to "final_transformed",
                "description" to "������ ��ȯ���� ������ ���� ���� ���� ���� ������",
                "count" to finalTransformedDtl.size,
                "data" to finalTransformedDtl,
                "transformationInfo" to mapOf(
                    "tiltAngle" to -6.98,
                    "rotatorAngle" to 0.0,
                    "transformationType" to "final_transform",
                    "angleLimit" to "��270��"
                )
            )
        }
    }

    /**
     * ������ Ÿ�Ժ� ������ ������ ��ȸ API (����)
     */
    @GetMapping("/master/by-type/{dataType}")
    @Operation(
        operationId = "getephemerislistbytype",
        tags = ["Mode - Ephemeris"]
    )
    fun getEphemerisTrackMstByDataType(@PathVariable dataType: String): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val mstData = ephemerisService.getEphemerisTrackMstByDataType(dataType)
            mapOf(
                "dataType" to dataType,
                "count" to mstData.size,
                "data" to mstData,
                "availableDataTypes" to listOf("original", "axis_transformed", "final_transformed")
            )
        }
    }

    /**
     * ������ Ÿ�Ժ� ���� ������ ��ȸ API (����)
     */
    @GetMapping("/detail/by-type/{dataType}")
    @Operation(
        operationId = "getephemerisdetailbytype",
        tags = ["Mode - Ephemeris"]
    )
    fun getEphemerisTrackDtlByDataType(@PathVariable dataType: String): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val dtlData = ephemerisService.getEphemerisTrackDtlByDataType(dataType)
            mapOf(
                "dataType" to dataType,
                "count" to dtlData.size,
                "data" to dtlData,
                "availableDataTypes" to listOf("original", "axis_transformed", "final_transformed")
            )
        }
    }

    /**
     * ���� ��ȯ �ܰ躰 ������ ���� ��ȸ API
     */
    @GetMapping("/summary/all-transformations")
    @Operation(
        operationId = "getalltransformationsummary",
        tags = ["Mode - Ephemeris"]
    )
    fun getAllTransformationSummary(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val originalMst = ephemerisService.getOriginalEphemerisTrackMst()
            val axisTransformedMst = ephemerisService.getAxisTransformedEphemerisTrackMst()
            val finalTransformedMst = ephemerisService.getFinalTransformedEphemerisTrackMst()

            mapOf(
                "summary" to mapOf(
                    "original" to mapOf(
                        "mstCount" to originalMst.size,
                        "description" to "��ȯ �� ���� ������"
                    ),
                    "axisTransformed" to mapOf(
                        "mstCount" to axisTransformedMst.size,
                        "description" to "������ ��ȯ�� ������ ������",
                        "tiltAngle" to -6.98,
                        "rotatorAngle" to 0.0
                    ),
                    "finalTransformed" to mapOf(
                        "mstCount" to finalTransformedMst.size,
                        "description" to "������ ��ȯ���� ������ ���� ������",
                        "angleLimit" to "��270��"
                    )
                ),
                "totalMstCount" to (originalMst.size + axisTransformedMst.size + finalTransformedMst.size),
                "transformationSteps" to listOf(
                    "1. ���� ������ ����",
                    "2. �ຯȯ ���� (������ -6.98��)",
                    "3. ������ ��ȯ (��270�� ����)",
                    "4. ���� ������ ����"
                )
            )
        }
    }

    /**
     * ���� MST �����͸� CSV ���Ϸ� �������� API
     */
    @PostMapping("/export/csv/all")
    @Operation(
        operationId = "exportallephemerisdata",
        tags = ["Mode - Ephemeris"]
    )
    fun exportAllMstDataToCsv(
        @Parameter(
            description = "CSV ���� ���� �����丮 (�⺻��: csv_exports)",
            example = "csv_exports",
            required = false
        )
        @RequestParam(defaultValue = "csv_exports") outputDirectory: String
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val result = ephemerisService.exportAllMstDataToCsv(outputDirectory)
                
                if (result["success"] == true) {
                    mapOf(
                        "success" to true,
                        "message" to "���� MST �����Ͱ� CSV ���Ϸ� ���������� �����������ϴ�.",
                        "totalMstCount" to (result["totalMstCount"] ?: 0),
                        "successCount" to (result["successCount"] ?: 0),
                        "errorCount" to (result["errorCount"] ?: 0),
                        "createdFiles" to (result["createdFiles"] ?: emptyList<String>()),
                        "outputDirectory" to (result["outputDirectory"] ?: outputDirectory)
                    )
                } else {
                    mapOf(
                        "success" to false,
                        "message" to "CSV �������� ����: ${result["error"] ?: "�� �� ���� ����"}",
                        "error" to (result["error"] ?: "�� �� ���� ����")
                    )
                }
            } catch (e: Exception) {
                mapOf(
                    "success" to false,
                    "message" to "CSV �������� �� ���� �߻�: ${e.message ?: "�� �� ���� ����"}",
                    "error" to (e.message ?: "�� �� ���� ����")
                )
            }
        }
    }

    /**
     * Ư�� MST ID�� �����͸� CSV ���Ϸ� �������� API
     */
    @PostMapping("/export/csv/{mstId}")
    @Operation(
        operationId = "exportephemerisdata",
        tags = ["Mode - Ephemeris"]
    )
    fun exportMstDataToCsv(
        @Parameter(
            description = "������ ������ ID",
            example = "1",
            required = true
        )
        @PathVariable mstId: Int,
        @Parameter(
            description = "CSV ���� ���� �����丮 (�⺻��: csv_exports)",
            example = "csv_exports",
            required = false
        )
        @RequestParam(defaultValue = "csv_exports") outputDirectory: String
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val result = ephemerisService.exportMstDataToCsv(mstId, outputDirectory)
                
                if (result["success"] == true) {
                    mapOf(
                        "success" to true,
                        "message" to "MST ID $mstId �����Ͱ� CSV ���Ϸ� ���������� �����������ϴ�.",
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
                        "message" to "CSV �������� ����: ${result["error"] ?: "�� �� ���� ����"}",
                        "error" to (result["error"] ?: "�� �� ���� ����")
                    )
                }
            } catch (e: Exception) {
                mapOf(
                    "success" to false,
                    "message" to "CSV �������� �� ���� �߻�: ${e.message ?: "�� �� ���� ����"}",
                    "error" to (e.message ?: "�� �� ���� ����")
                )
            }
        }
    }
}
/**
 * CSV ������ ���� ���� �޼��� (������ ���� - createRealtimeTrackingData�� ����)
 */
private fun generateRealtimeTrackingCsv(data: List<Map<String, Any?>>): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

    try {
        // UTF-8 BOM �߰� (Excel���� �ѱ� ���� ����)
        outputStream.write(0xEF)
        outputStream.write(0xBB)
        outputStream.write(0xBF)

        // CSV ���� �ۼ� (createRealtimeTrackingData�� ���� �ʵ� ����)
        val headers = listOf(
            "Index", "TheoreticalIndex", "Timestamp", "PassId", "ElapsedTime(s)",
            
            // ���� ������ (��ȯ ��)
            "OriginalAzimuth(��)", "OriginalElevation(��)", "OriginalRange(km)", "OriginalAltitude(km)",
            
            // �ຯȯ ������ (������ ��ȯ ����)
            "AxisTransformedAzimuth(��)", "AxisTransformedElevation(��)", "AxisTransformedRange(km)", "AxisTransformedAltitude(km)",
            
            // ���� ��ȯ ������ (��270�� ���� ����)
            "FinalTransformedAzimuth(��)", "FinalTransformedElevation(��)", "FinalTransformedRange(km)", "FinalTransformedAltitude(km)",
            
            // ���� �� ���� ������
            "CmdAzimuth(��)", "CmdElevation(��)", "ActualAzimuth(��)", "ActualElevation(��)",
            
            // ���� ���� ������
            "TrackingAzimuthTime", "TrackingCMDAzimuth(��)", "TrackingActualAzimuth(��)",
            "TrackingElevationTime", "TrackingCMDElevation(��)", "TrackingActualElevation(��)",
            "TrackingTiltTime", "TrackingCMDTilt(��)", "TrackingActualTilt(��)",
            
            // ���� �м�
            "AzimuthError(��)", "ElevationError(��)",
            "OriginalToAxisTransformationError(��)", "AxisToFinalTransformationError(��)", "TotalTransformationError(��)",
            
            // ��Ȯ�� �м� (���� �߰��� �ʵ���)
            "�ð���Ȯ��(s)", "Az_CMD��Ȯ��(��)", "Az_Act��Ȯ��(��)", "Az_������Ȯ��(��)",
            "El_CMD��Ȯ��(��)", "El_Act��Ȯ��(��)", "El_������Ȯ��(��)",
            
            // ��ȯ ����
            "TiltAngle(��)", "TransformationType", "HasTransformation", "InterpolationMethod", "InterpolationAccuracy",
            "HasValidData", "DataSource"
        )

        writer.write(headers.joinToString(","))
        writer.write("\n")

        // ������ �� �ۼ�
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        data.forEach { record ->
            val row = listOf(
                record["index"]?.toString() ?: "",
                record["theoreticalIndex"]?.toString() ?: "",  // ? �̷�ġ ������ �ε��� �߰�
                (record["timestamp"] as? ZonedDateTime)?.format(dateFormatter) ?: "",
                record["passId"]?.toString() ?: "",
                String.format("%.3f", record["elapsedTimeSeconds"] as? Float ?: 0.0f),
                
                // ���� ������
                String.format("%.6f", record["originalAzimuth"] as? Float ?: 0.0f),
                String.format("%.6f", record["originalElevation"] as? Float ?: 0.0f),
                String.format("%.6f", record["originalRange"] as? Float ?: 0.0f),
                String.format("%.6f", record["originalAltitude"] as? Float ?: 0.0f),
                
                // �ຯȯ ������
                String.format("%.6f", record["axisTransformedAzimuth"] as? Float ?: 0.0f),
                String.format("%.6f", record["axisTransformedElevation"] as? Float ?: 0.0f),
                String.format("%.6f", record["axisTransformedRange"] as? Float ?: 0.0f),
                String.format("%.6f", record["axisTransformedAltitude"] as? Float ?: 0.0f),
                
                // ���� ��ȯ ������
                String.format("%.6f", record["finalTransformedAzimuth"] as? Float ?: 0.0f),
                String.format("%.6f", record["finalTransformedElevation"] as? Float ?: 0.0f),
                String.format("%.6f", record["finalTransformedRange"] as? Float ?: 0.0f),
                String.format("%.6f", record["finalTransformedAltitude"] as? Float ?: 0.0f),
                
                // ���� �� ���� ������
                String.format("%.6f", record["cmdAz"] as? Float ?: 0.0f),
                String.format("%.6f", record["cmdEl"] as? Float ?: 0.0f),
                String.format("%.6f", record["actualAz"] as? Float ?: 0.0f),
                String.format("%.6f", record["actualEl"] as? Float ?: 0.0f),
                
                // ���� ���� ������
                record["trackingAzimuthTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDAzimuthAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualAzimuthAngle"] as? Float ?: 0.0f),
                record["trackingElevationTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDElevationAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualElevationAngle"] as? Float ?: 0.0f),
                record["trackingTiltTime"]?.toString() ?: "",
                String.format("%.6f", record["trackingCMDTiltAngle"] as? Float ?: 0.0f),
                String.format("%.6f", record["trackingActualTiltAngle"] as? Float ?: 0.0f),
                
                // ���� �м�
                String.format("%.6f", record["azimuthError"] as? Float ?: 0.0f),
                String.format("%.6f", record["elevationError"] as? Float ?: 0.0f),
                String.format("%.6f", record["originalToAxisTransformationError"] as? Float ?: 0.0f),
                String.format("%.6f", record["axisToFinalTransformationError"] as? Float ?: 0.0f),
                String.format("%.6f", record["totalTransformationError"] as? Float ?: 0.0f),
                
                // ��Ȯ�� �м� (���� �߰��� �ʵ���)
                String.format("%.6f", record["timeAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["azCmdAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["azActAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["azFinalAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["elCmdAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["elActAccuracy"] as? Float ?: 0.0f),
                String.format("%.6f", record["elFinalAccuracy"] as? Float ?: 0.0f),
                
                // ��ȯ ����
                String.format("%.6f", record["tiltAngle"] as? Double ?: 0.0),
                "\"${record["transformationType"] ?: ""}\"",
                (record["hasTransformation"] as? Boolean ?: false).toString(),
                "\"${record["interpolationMethod"] ?: ""}\"",
                String.format("%.6f", record["interpolationAccuracy"] as? Double ?: 0.0),
                (record["hasValidData"] as? Boolean ?: false).toString(),
                "\"${record["dataSource"] ?: ""}\""
            )

            writer.write(row.joinToString(","))
            writer.write("\n")
        }

        writer.flush()
        return outputStream.toByteArray()

    } finally {
        writer.close()
        outputStream.close()
    }
}
/**
 * ���� ��ġ ��û ����
 */
data class SatellitePositionRequest(
    val tleLine1: String,
    val tleLine2: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
)

/**
 * Ư�� �ð��� ���� ��ġ ��û ����
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
 * ���� ���� ���� ��û ����
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
 * ���� ���ü� ��û ����
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
 * ���� ���ü� �Ⱓ ���� ����
 */
data class VisibilityPeriod(
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime
)

/**
 * ���� ���� ������ ��û ����
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
    val trackingIntervalMs: Int = 100 // �⺻�� 100ms
)

/**
 * ���� �˵� ���� ��û ����
 */
data class EphemerisTrackRequest(
    val tleLine1: String,
    val tleLine2: String,
    val satelliteName: String? = null
)

/**
 * ������ ��ȯ�� ������ ���� �˵� ���� ��û ����
 */
data class EphemerisTrackWithTiltRequest(
    val tleLine1: String,
    val tleLine2: String,
    val satelliteName: String? = null,
    val tiltAngle: Double = -6.98  // �⺻ ������ ����
)

/**
 * �����˵� ���� ���� ��û ����
 */
data class GeostationaryTrackingRequest(
    val tleLine1: String,
    val tleLine2: String
)





