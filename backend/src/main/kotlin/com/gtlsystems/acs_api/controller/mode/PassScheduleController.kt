package com.gtlsystems.acs_api.controller.mode

import com.gtlsystems.acs_api.service.mode.PassScheduleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.ZonedDateTime

/**
 * 패스 스케줄링 TLE 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/pass-schedule")
@Tag(name = "Mode - Pass Schedule", description = "패스 스케줄링 TLE 관리 API - 위성 궤도 데이터 관리, 추적 스케줄 생성, 모니터링")
class PassScheduleController(
    private val passScheduleService: PassScheduleService
) {
    private val logger = LoggerFactory.getLogger(PassScheduleController::class.java)

    @PostMapping("/time-offset-command")
    fun timeOffsetCommand(@RequestParam inputTimeOffset: Float): ResponseEntity<Map<String, String>> {
        return try {
            passScheduleService.passScheduleTimeOffsetCommand(inputTimeOffset)
            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "TimeOffset 명령이 성공적으로 전송되었습니다",
                    "command" to "TimeOffset",
                    "timeOffset" to inputTimeOffset.toString()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "TimeOffset 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    /**
     * TLE 데이터 추가
     */
    @PostMapping("/tle")
    @Operation(
        operationId = "addtle",
        tags = ["Mode - Pass Schedule"]
    )
    fun addTle(
        @Parameter(
            description = "TLE 데이터 추가 요청",
            required = true
        )
        @RequestBody request: AddTleRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // satelliteId 결정: 요청에서 제공되면 사용, 없으면 TLE Line1에서 추출
            val finalSatelliteId = if (request.satelliteId.isNullOrBlank()) {
                try {
                    // TLE Line1에서 위성 번호 추출 (3-7번째 문자)
                    request.tleLine1.substring(2, 7).trim()
                } catch (e: Exception) {
                    return ResponseEntity.badRequest().body(
                        mapOf(
                            "success" to false,
                            "message" to "satelliteId가 제공되지 않았고, TLE Line1에서 위성 번호를 추출할 수 없습니다.",
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                }
            } else {
                request.satelliteId
            }

            if (finalSatelliteId.isBlank()) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "success" to false,
                        "message" to "유효한 위성 ID를 확인할 수 없습니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            if (request.tleLine1.isBlank() || request.tleLine2.isBlank()) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "success" to false,
                        "message" to "TLE Line1과 Line2는 필수입니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            // TLE 형식 기본 검증
            if (request.tleLine1.length != 69 || request.tleLine2.length != 69) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "success" to false,
                        "message" to "TLE 형식이 올바르지 않습니다. 각 라인은 69자여야 합니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            passScheduleService.addPassScheduleTle(
                finalSatelliteId,  // request.satelliteId 대신 finalSatelliteId 사용
                request.tleLine1,
                request.tleLine2,
                request.satelliteName  // 위성 이름 추가
            )

            logger.info("TLE 데이터 추가 성공: 위성 ID = ${request.satelliteId}")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "TLE 데이터가 성공적으로 추가되었습니다.",
                    "data" to mapOf(
                        "satelliteId" to finalSatelliteId,
                        "tleLine1" to request.tleLine1,
                        "tleLine2" to request.tleLine2,
                        "added" to true,
                        "satelliteIdSource" to if (request.satelliteId.isNullOrBlank()) "extracted_from_tle" else "provided"
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("TLE 데이터 추가 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "TLE 데이터 추가 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 특정 위성의 TLE 데이터 조회
     */
    @GetMapping("/tle/{satelliteId}")
    @Operation(
        operationId = "gettle",
        tags = ["Mode - Pass Schedule"]
    )
    fun getTle(@PathVariable satelliteId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val tleData = passScheduleService.getPassScheduleTleWithName(satelliteId)

            if (tleData != null) {
                val (tleLine1, tleLine2, satelliteName) = tleData
                logger.info("TLE 데이터 조회 성공: 위성 ID = $satelliteId")
                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "TLE 데이터 조회 성공",
                        "data" to mapOf(
                            "satelliteId" to satelliteId,
                            "satelliteName" to satelliteName,
                            "tleLine1" to tleLine1,
                            "tleLine2" to tleLine2
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                logger.warn("TLE 데이터 없음: 위성 ID = $satelliteId")
                ResponseEntity.status(404).body(
                    mapOf(
                        "success" to false,
                        "message" to "해당 위성 ID의 TLE 데이터를 찾을 수 없습니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("TLE 데이터 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "TLE 데이터 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 전체 TLE 데이터 조회
     */
    @GetMapping("/tle")
    @Operation(
        operationId = "gettlelist",
        tags = ["Mode - Pass Schedule"]
    )
    fun getAllTles(): ResponseEntity<Map<String, Any>> {
        return try {
            val satelliteIds = passScheduleService.getAllPassScheduleTleIds()
            val tleList = mutableListOf<Map<String, Any>>()

            satelliteIds.forEach { satelliteId ->
                val tleData = passScheduleService.getPassScheduleTleWithName(satelliteId)
                if (tleData != null) {
                    val (tleLine1, tleLine2, satelliteName) = tleData
                    tleList.add(
                        mapOf(
                            "satelliteId" to satelliteId,
                            "satelliteName" to satelliteName,
                            "tleLine1" to tleLine1,
                            "tleLine2" to tleLine2
                        )
                    )
                }
            }

            logger.info("전체 TLE 데이터 조회 성공: ${tleList.size}개")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "전체 TLE 데이터 조회 성공",
                    "data" to mapOf(
                        "totalCount" to tleList.size,
                        "tleList" to tleList
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("전체 TLE 데이터 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "전체 TLE 데이터 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 특정 위성의 TLE 데이터 삭제
     */
    @DeleteMapping("/tle/{satelliteId}")
    @Operation(
        operationId = "deletetle",
        tags = ["Mode - Pass Schedule"]
    )
    fun deleteTle(@PathVariable satelliteId: String): ResponseEntity<Map<String, Any>> {
        return try {
            // 삭제 전 존재 여부 확인
            val existingTle = passScheduleService.getPassScheduleTle(satelliteId)
            if (existingTle == null) {
                logger.warn("삭제할 TLE 데이터 없음: 위성 ID = $satelliteId")
                return ResponseEntity.status(404).body(
                    mapOf(
                        "success" to false,
                        "message" to "해당 위성 ID의 TLE 데이터를 찾을 수 없습니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            passScheduleService.removePassScheduleTle(satelliteId)
            logger.info("TLE 데이터 삭제 성공: 위성 ID = $satelliteId")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "TLE 데이터가 성공적으로 삭제되었습니다.",
                    "data" to mapOf(
                        "satelliteId" to satelliteId,
                        "deleted" to true
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("TLE 데이터 삭제 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "TLE 데이터 삭제 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 전체 TLE 데이터 삭제
     */
    @DeleteMapping("/tle")
    @Operation(
        operationId = "deletealltles",
        tags = ["Mode - Pass Schedule"]
    )
    fun deleteAllTles(): ResponseEntity<Map<String, Any>> {
        return try {
            val beforeCount = passScheduleService.getCacheSize()
            passScheduleService.clearCache()

            logger.info("전체 TLE 데이터 삭제 성공: ${beforeCount}개 삭제")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "전체 TLE 데이터가 성공적으로 삭제되었습니다.",
                    "data" to mapOf(
                        "deletedCount" to beforeCount,
                        "remainingCount" to 0
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("전체 TLE 데이터 삭제 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "전체 TLE 데이터 삭제 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * TLE 캐시 상태 조회
     */
    @GetMapping("/status")
    @Operation(
        operationId = "gettlestatus",
        tags = ["Mode - Pass Schedule"]
    )
    fun getCacheStatus(): ResponseEntity<Map<String, Any>> {
        return try {
            val cacheSize = passScheduleService.getCacheSize()
            val satelliteIds = passScheduleService.getAllPassScheduleTleIds()

            logger.info("TLE 캐시 상태 조회 성공: ${cacheSize}개")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "TLE 캐시 상태 조회 성공",
                    "data" to mapOf(
                        "totalCount" to cacheSize,
                        "satelliteIds" to satelliteIds,
                        "isEmpty" to (cacheSize == 0),
                        "cacheInfo" to mapOf(
                            "type" to "ConcurrentHashMap",
                            "description" to "위성 카탈로그 ID -> TLE Line1, Line2"
                        )
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("TLE 캐시 상태 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "TLE 캐시 상태 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * TLE 데이터 업데이트 (기존 데이터 덮어쓰기)
     */
    @PutMapping("/tle/{satelliteId}")
    @Operation(
        operationId = "updatetle",
        tags = ["Mode - Pass Schedule"]
    )
    fun updateTle(
        @PathVariable satelliteId: String,
        @RequestBody request: UpdateTleRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // 입력 검증
            if (request.tleLine1.isBlank() || request.tleLine2.isBlank()) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "success" to false,
                        "message" to "TLE Line1과 Line2는 필수입니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            // TLE 형식 기본 검증
            if (request.tleLine1.length != 69 || request.tleLine2.length != 69) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "success" to false,
                        "message" to "TLE 형식이 올바르지 않습니다. 각 라인은 69자여야 합니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            // 기존 데이터 존재 여부 확인
            val existingTle = passScheduleService.getPassScheduleTle(satelliteId)
            val isUpdate = existingTle != null

            passScheduleService.addPassScheduleTle(
                satelliteId,
                request.tleLine1,
                request.tleLine2,
                request.satelliteName  // 위성 이름 추가
            )

            val message = if (isUpdate) {
                "TLE 데이터가 성공적으로 업데이트되었습니다."
            } else {
                "TLE 데이터가 성공적으로 추가되었습니다."
            }

            logger.info("TLE 데이터 ${if (isUpdate) "업데이트" else "추가"} 성공: 위성 ID = $satelliteId")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to message,
                    "data" to mapOf(
                        "satelliteId" to satelliteId,
                        "isUpdate" to isUpdate,
                        "operation" to if (isUpdate) "updated" else "created"
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("TLE 데이터 업데이트 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "TLE 데이터 업데이트 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    // ==================== 추적 데이터 생성 및 관리 API ====================

    /**
     * 모든 TLE 데이터에 대해 위성 추적 정보를 생성합니다 (비동기)
     */
    @PostMapping("/tracking/generate-all")
    @Operation(
        operationId = "generateallpassscheduletracking",
        tags = ["Mode - Pass Schedule"]
    )
    fun generateAllTrackingData(): Mono<ResponseEntity<Map<String, Any>>> {
        logger.info("모든 위성 추적 데이터 생성 요청 수신")
        return passScheduleService.generateAllPassScheduleTrackingDataAsync()
            .map { results ->
                logger.info("모든 위성 추적 데이터 생성 완료: ${results.size}개 위성")

                val summary = results.map { (satelliteId, data) ->
                    val (mstData, dtlData) = data
                    mapOf(
                        "satelliteId" to satelliteId,
                        "passCount" to mstData.size,
                        "trackingPointCount" to dtlData.size,
                        "satelliteName" to (mstData.firstOrNull()?.get("SatelliteName") ?: "Unknown")
                    )
                }

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "모든 위성 추적 데이터 생성 완료",
                        "data" to mapOf(
                            "processedSatellites" to results.size,
                            "totalPasses" to summary.sumOf { it["passCount"] as Int },
                            "totalTrackingPoints" to summary.sumOf { it["trackingPointCount"] as Int },
                            "satellites" to summary
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
            .onErrorResume { error ->
                logger.error("모든 위성 추적 데이터 생성 실패: ${error.message}", error)
                Mono.just(
                    ResponseEntity.internalServerError().body(
                        mapOf(
                            "success" to false,
                            "message" to "모든 위성 추적 데이터 생성 중 오류가 발생했습니다: ${error.message}",
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                )
            }
    }

    /**
     * 특정 위성의 추적 데이터를 생성합니다 (비동기)
     */
    @PostMapping("/tracking/generate/{satelliteId}")
    fun generateTrackingData(@PathVariable satelliteId: String): Mono<ResponseEntity<Map<String, Any>>> {
        logger.info("위성 $satelliteId 추적 데이터 생성 요청 수신")

        return Mono.fromCallable {
            val tleData = passScheduleService.getPassScheduleTle(satelliteId)
            if (tleData == null) {
                throw IllegalArgumentException("위성 ID $satelliteId 의 TLE 데이터를 찾을 수 없습니다.")
            }
            tleData
        }
            .flatMap { tleData ->
                val (tleLine1, tleLine2) = tleData
                // ✅ startMstId를 null로 전달하면 자동으로 전역 카운터에서 할당됨
                passScheduleService.generatePassScheduleTrackingDataAsync(satelliteId, tleLine1, tleLine2, null, null)
            }
            .map { (mstData, dtlData) ->
                logger.info("위성 $satelliteId 추적 데이터 생성 완료: ${mstData.size}개 패스, ${dtlData.size}개 추적 포인트")

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "위성 추적 데이터 생성 완료",
                        "data" to mapOf(
                            "satelliteId" to satelliteId,
                            "satelliteName" to (mstData.firstOrNull()?.get("SatelliteName") ?: "Unknown"),
                            "passCount" to mstData.size,
                            "trackingPointCount" to dtlData.size,
                            "passes" to mstData.map { pass ->
                                mapOf(
                                    // ✅ "No" → "MstId" 변경
                                    "passId" to pass["MstId"],  // ✅ 전역 고유 MstId
                                    "mstId" to pass["MstId"],    // ✅ 명시적으로 mstId 필드 추가
                                    "detailId" to (pass["DetailId"] ?: 0),  // ✅ DetailId 필드 추가
                                    "startTime" to pass["StartTime"],
                                    "endTime" to pass["EndTime"],
                                    "duration" to pass["Duration"],
                                    "maxElevation" to pass["MaxElevation"]
                                )
                            }
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
            .onErrorResume { error ->
                logger.error("위성 $satelliteId 추적 데이터 생성 실패: ${error.message}", error)
                Mono.just(
                    ResponseEntity.badRequest().body(
                        mapOf(
                            "success" to false,
                            "message" to "위성 추적 데이터 생성 중 오류가 발생했습니다: ${error.message}",
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                )
            }
    }

    /**
     * 특정 위성의 패스 스케줄 마스터 데이터를 조회합니다.
     */
    @GetMapping("/tracking/master/{satelliteId}")
    @Operation(
        operationId = "getpassschedulemasterdata",
        tags = ["Mode - Pass Schedule"]
    )
    fun getTrackingMasterData(@PathVariable satelliteId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val mstData = passScheduleService.getPassScheduleTrackMstBySatelliteId(satelliteId)

            if (mstData != null && mstData.isNotEmpty()) {
                logger.info("위성 $satelliteId 마스터 데이터 조회 성공: ${mstData.size}개 패스")

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "위성 마스터 데이터 조회 성공",
                        "data" to mapOf(
                            "satelliteId" to satelliteId,
                            "passCount" to mstData.size,
                            "passes" to mstData
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                logger.warn("위성 $satelliteId 마스터 데이터 없음")
                ResponseEntity.status(404).body(
                    mapOf(
                        "success" to false,
                        "message" to "해당 위성의 추적 데이터를 찾을 수 없습니다. 먼저 추적 데이터를 생성해주세요.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("위성 $satelliteId 마스터 데이터 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "마스터 데이터 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 특정 위성의 패스 스케줄 세부 데이터를 조회합니다.
     */
    @GetMapping("/tracking/detail/{satelliteId}")
    @Operation(
        operationId = "getpassscheduledetaildata",
        tags = ["Mode - Pass Schedule"]
    )
    fun getTrackingDetailData(@PathVariable satelliteId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val dtlData = passScheduleService.getPassScheduleTrackDtlBySatelliteId(satelliteId)

            if (dtlData != null && dtlData.isNotEmpty()) {
                logger.info("위성 $satelliteId 세부 데이터 조회 성공: ${dtlData.size}개 추적 포인트")

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "위성 세부 데이터 조회 성공",
                        "data" to mapOf(
                            "satelliteId" to satelliteId,
                            "trackingPointCount" to dtlData.size,
                            "trackingPoints" to dtlData
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                logger.warn("위성 $satelliteId 세부 데이터 없음")
                ResponseEntity.status(404).body(
                    mapOf(
                        "success" to false,
                        "message" to "해당 위성의 추적 세부 데이터를 찾을 수 없습니다. 먼저 추적 데이터를 생성해주세요.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("위성 $satelliteId 세부 데이터 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "세부 데이터 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 특정 위성의 특정 패스에 대한 세부 데이터를 조회합니다.
     */
    @GetMapping("/tracking/detail/{mstId}/pass/{detailId}")
    @Operation(
        operationId = "getpassscheduledetailbypass",
        tags = ["Mode - Pass Schedule"]
    )
    /**
     * MstId와 DetailId로 세부 데이터를 조회하는 함수
     * 
     * 기존 경로 구조 유지: /tracking/detail/{satelliteId}/pass/{passId}
     * → /tracking/detail/{mstId}/pass/{detailId}로 변경
     * 
     * @param mstId 전역 고유 패스 ID (Long 타입, 기존 satelliteId 자리)
     * @param detailId 패스 인덱스 (Int 타입, 기존 passId 자리)
     * @param dataType DataType (optional)
     * @return 세부 데이터 응답
     */
    fun getTrackingDetailDataByPass(
        @PathVariable mstId: Long,  // ✅ 기존 satelliteId 자리
        @PathVariable detailId: Int,  // ✅ 기존 passId 자리
        @RequestParam(required = false) dataType: String? = null  // ✅ DataType 파라미터 추가
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // ✅ DataType 파라미터를 서비스에 전달 (satelliteId 제거)
            val dtlData = passScheduleService.getPassScheduleTrackDtlByMstIdAndDetailId(mstId, detailId, dataType)

            if (dtlData.isNotEmpty()) {
                // ✅ mstId와 detailId만 사용 (satelliteId 제거)
                logger.info("mstId=$mstId, detailId=$detailId 세부 데이터 조회 성공: ${dtlData.size}개 추적 포인트 (DataType: ${dataType ?: "auto"})")

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "패스별 세부 데이터 조회 성공",
                        "data" to mapOf(
                            "mstId" to mstId,
                            "detailId" to detailId,
                            "trackingPointCount" to dtlData.size,
                            "trackingPoints" to dtlData,
                            "dataType" to (dataType ?: "auto")  // ✅ 반환된 DataType 정보 추가
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                logger.warn("mstId=$mstId, detailId=$detailId 세부 데이터 없음 (DataType: ${dataType ?: "auto"})")
                ResponseEntity.status(404).body(
                    mapOf(
                        "success" to false,
                        "message" to "해당 MstId와 DetailId에 대한 추적 세부 데이터를 찾을 수 없습니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("mstId=$mstId, detailId=$detailId 세부 데이터 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "패스별 세부 데이터 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 모든 위성의 패스 스케줄 마스터 데이터를 조회합니다.
     */
    @GetMapping("/tracking/master")
    @Operation(
        operationId = "getallpassschedulemasterdata",
        tags = ["Mode - Pass Schedule"]
    )
    /**
     * 전체 PassSchedule 마스터 데이터를 조회합니다.
     *
     * 이 함수는 5가지 DataType의 MST 데이터를 병합하여 Keyhole 정보를 포함한 데이터를 반환합니다.
     * EphemerisController의 `/ephemeris/master` API와 동일한 수준의 정보를 제공합니다.
     *
     * @return ResponseEntity<Map<String, Any>> 위성별로 그룹화된 MST 데이터 (Keyhole 정보 포함)
     *
     * @see EphemerisController.getAllEphemerisTrackMst EphemerisService의 동일한 로직 참고
     * @see PassScheduleService.getAllPassScheduleTrackMstMerged 병합된 데이터 제공
     */
    fun getAllTrackingMasterData(): ResponseEntity<Map<String, Any>> {
        return try {
            // ✅ getAllPassScheduleTrackMstMerged() 사용 (Keyhole 정보 포함)
            val allMstData = passScheduleService.getAllPassScheduleTrackMstMerged()

            if (allMstData.isNotEmpty()) {
                // 위성별로 그룹화 (기존 구조 유지, 하위 호환성)
                val satellites = allMstData.groupBy { it["SatelliteID"] as String }
                val totalPasses = allMstData.size

                logger.info("전체 마스터 데이터 조회 성공: ${satellites.size}개 위성, ${totalPasses}개 패스 (Keyhole 정보 포함)")

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "전체 마스터 데이터 조회 성공",
                        "data" to mapOf(
                            "satelliteCount" to satellites.size,
                            "totalPassCount" to totalPasses,
                            "satellites" to satellites
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                logger.warn("전체 마스터 데이터 없음")
                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "추적 데이터가 없습니다. 먼저 추적 데이터를 생성해주세요.",
                        "data" to mapOf(
                            "satelliteCount" to 0,
                            "totalPassCount" to 0,
                            "satellites" to emptyMap<String, Any>()
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("전체 마스터 데이터 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "전체 마스터 데이터 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 추적 데이터 통계 정보를 조회합니다.
     */
    @GetMapping("/tracking/statistics")
    @Operation(
        operationId = "getpassschedulestatistics",
        tags = ["Mode - Pass Schedule"]
    )
    fun getTrackingStatistics(): ResponseEntity<Map<String, Any>> {
        return try {
            val statistics = passScheduleService.getTrackingDataStatistics()
            logger.info("추적 데이터 통계 조회 성공")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "추적 데이터 통계 조회 성공",
                    "data" to statistics,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("추적 데이터 통계 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "추적 데이터 통계 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 특정 위성의 추적 데이터를 삭제합니다.
     */
    @DeleteMapping("/tracking/{satelliteId}")
    @Operation(
        operationId = "deletepassscheduledata",
        tags = ["Mode - Pass Schedule"]
    )
    fun deleteTrackingData(@PathVariable satelliteId: String): ResponseEntity<Map<String, Any>> {
        return try {
            // 삭제 전 존재 여부 확인
            val mstData = passScheduleService.getPassScheduleTrackMstBySatelliteId(satelliteId)
            val dtlData = passScheduleService.getPassScheduleTrackDtlBySatelliteId(satelliteId)

            if (mstData == null && dtlData == null) {
                logger.warn("삭제할 추적 데이터 없음: 위성 ID = $satelliteId")
                return ResponseEntity.status(404).body(
                    mapOf(
                        "success" to false,
                        "message" to "해당 위성의 추적 데이터를 찾을 수 없습니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            val deletedPassCount = mstData?.size ?: 0
            val deletedTrackingPointCount = dtlData?.size ?: 0

            passScheduleService.clearPassScheduleTrackingData(satelliteId)
            logger.info("위성 $satelliteId 추적 데이터 삭제 성공: ${deletedPassCount}개 패스, ${deletedTrackingPointCount}개 추적 포인트")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "위성 추적 데이터가 성공적으로 삭제되었습니다.",
                    "data" to mapOf(
                        "satelliteId" to satelliteId,
                        "deletedPassCount" to deletedPassCount,
                        "deletedTrackingPointCount" to deletedTrackingPointCount,
                        "deleted" to true
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("위성 $satelliteId 추적 데이터 삭제 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "추적 데이터 삭제 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 모든 위성의 추적 데이터를 삭제합니다.
     */
    @DeleteMapping("/tracking")
    @Operation(
        operationId = "deleteallpassscheduledata",
        tags = ["Mode - Pass Schedule"]
    )
    fun deleteAllTrackingData(): ResponseEntity<Map<String, Any>> {
        return try {
            val statistics = passScheduleService.getTrackingDataStatistics()
            val beforeSatelliteCount = statistics["totalSatellites"] as Int
            val beforePassCount = statistics["totalPasses"] as Int
            val beforeTrackingPointCount = statistics["totalTrackingPoints"] as Int

            passScheduleService.clearAllPassScheduleTrackingData()
            logger.info("전체 추적 데이터 삭제 성공: ${beforeSatelliteCount}개 위성, ${beforePassCount}개 패스, ${beforeTrackingPointCount}개 추적 포인트")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "전체 추적 데이터가 성공적으로 삭제되었습니다.",
                    "data" to mapOf(
                        "deletedSatelliteCount" to beforeSatelliteCount,
                        "deletedPassCount" to beforePassCount,
                        "deletedTrackingPointCount" to beforeTrackingPointCount,
                        "remainingSatelliteCount" to 0,
                        "remainingPassCount" to 0,
                        "remainingTrackingPointCount" to 0
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("전체 추적 데이터 삭제 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "전체 추적 데이터 삭제 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * TLE 데이터 추가와 동시에 추적 데이터를 생성합니다 (원스톱 API)
     */
    @PostMapping("/tle-and-tracking")
    @Operation(
        operationId = "addtleandgeneratetracking",
        tags = ["Mode - Pass Schedule"]
    )
    fun addTleAndGenerateTracking(@RequestBody request: AddTleRequest): Mono<ResponseEntity<Map<String, Any>>> {
        logger.info("TLE 추가 및 추적 데이터 생성 요청 수신")

        return Mono.fromCallable {
            // satelliteId 결정
            val finalSatelliteId = if (request.satelliteId.isNullOrBlank()) {
                try {
                    request.tleLine1.substring(2, 7).trim()
                } catch (e: Exception) {
                    throw IllegalArgumentException("satelliteId가 제공되지 않았고, TLE Line1에서 위성 번호를 추출할 수 없습니다.")
                }
            } else {
                request.satelliteId
            }

            if (finalSatelliteId.isBlank()) {
                throw IllegalArgumentException("유효한 위성 ID를 확인할 수 없습니다.")
            }

            if (request.tleLine1.isBlank() || request.tleLine2.isBlank()) {
                throw IllegalArgumentException("TLE Line1과 Line2는 필수입니다.")
            }

            // TLE 형식 기본 검증
            if (request.tleLine1.length != 69 || request.tleLine2.length != 69) {
                throw IllegalArgumentException("TLE 형식이 올바르지 않습니다. 각 라인은 69자여야 합니다.")
            }

            // TLE 데이터 추가
            passScheduleService.addPassScheduleTle(
                finalSatelliteId,
                request.tleLine1,
                request.tleLine2,
                request.satelliteName  // 위성 이름 추가
            )

            val finalSatelliteName = passScheduleService.getPassScheduleSatelliteName(finalSatelliteId)
                ?: request.satelliteName
                ?: "Satellite-$finalSatelliteId"

            Triple(finalSatelliteId, finalSatelliteName, request)
        }
            .flatMap { (satelliteId, satelliteName, request) ->
                // 추적 데이터 생성
                // ✅ startMstId를 null로 전달하면 자동으로 전역 카운터에서 할당됨
                passScheduleService.generatePassScheduleTrackingDataAsync(
                    satelliteId,
                    request.tleLine1,
                    request.tleLine2,
                    satelliteName,  // 위성 이름 전달
                    null  // ✅ null이면 자동으로 전역 카운터에서 MstId 할당
                ).map { (mstData, dtlData) ->
                    Triple(satelliteId, mstData, dtlData)
                }
            }
            .map { (satelliteId, mstData, dtlData) ->
                logger.info("TLE 추가 및 추적 데이터 생성 완료: 위성 ID = $satelliteId, ${mstData.size}개 패스, ${dtlData.size}개 추적 포인트")

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "TLE 데이터 추가 및 추적 데이터 생성이 완료되었습니다.",
                        "data" to mapOf(
                            "satelliteId" to satelliteId,
                            "satelliteName" to (mstData.firstOrNull()?.get("SatelliteName") ?: "Unknown"),
                            "tleLine1" to request.tleLine1,
                            "tleLine2" to request.tleLine2,
                            "passCount" to mstData.size,
                            "trackingPointCount" to dtlData.size,
                            "satelliteIdSource" to if (request.satelliteId.isNullOrBlank()) "extracted_from_tle" else "provided",
                            "passes" to mstData.map { pass ->
                                mapOf(
                                    // ✅ "No" → "MstId" 변경
                                    "passId" to pass["MstId"],  // ✅ 전역 고유 MstId
                                    "mstId" to pass["MstId"],    // ✅ 명시적으로 mstId 필드 추가
                                    "detailId" to (pass["DetailId"] ?: 0),  // ✅ DetailId 필드 추가
                                    "startTime" to pass["StartTime"],
                                    "endTime" to pass["EndTime"],
                                    "duration" to pass["Duration"],
                                    "maxElevation" to pass["MaxElevation"]
                                )
                            }
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
            .onErrorResume { error ->
                logger.error("TLE 추가 및 추적 데이터 생성 실패: ${error.message}", error)
                Mono.just(
                    ResponseEntity.badRequest().body(
                        mapOf(
                            "success" to false,
                            "message" to "TLE 추가 및 추적 데이터 생성 중 오류가 발생했습니다: ${error.message}",
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                )
            }
    }

    // ==================== 위성 추적 스케줄 대상 목록 관리 API ====================

    /**
     * ✅ 위성 추적 스케줄 대상 목록을 설정합니다.
     */
    @PostMapping("/tracking-targets")
    @Operation(
        operationId = "setpassscheduletargets",
        tags = ["Mode - Pass Schedule"]
    )
    fun setTrackingTargets(@RequestBody request: SetTrackingTargetsRequest): ResponseEntity<Map<String, Any>> {
        return try {
            // 입력 검증
            if (request.targets.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "success" to false,
                        "message" to "추적 대상 목록이 비어있습니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            // 각 대상의 유효성 검증
            val invalidTargets = mutableListOf<String>()
            request.targets.forEachIndexed { index, target ->
                if (target.satelliteId.isBlank()) {
                    invalidTargets.add("인덱스 $index: satelliteId가 비어있습니다.")
                }
                // ✅ UInt → Long 변경에 따라 0u → 0L로 변경
                if (target.mstId == 0L) {
                    invalidTargets.add("인덱스 $index: mstId가 0입니다.")
                }
                if (target.startTime.isAfter(target.endTime)) {
                    invalidTargets.add("인덱스 $index: 시작 시간이 종료 시간보다 늦습니다.")
                }
                if (target.maxElevation < 0 || target.maxElevation > 90) {
                    invalidTargets.add("인덱스 $index: 최대 고도각이 유효하지 않습니다. (0-90도)")
                }
            }

            if (invalidTargets.isNotEmpty()) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "success" to false,
                        "message" to "유효하지 않은 추적 대상이 있습니다.",
                        "errors" to invalidTargets,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }

            // TrackingTarget 객체로 변환
            val trackingTargets = request.targets.map { target ->
                PassScheduleService.TrackingTarget(
                    mstId = target.mstId,
                    satelliteId = target.satelliteId,
                    satelliteName = target.satelliteName,
                    startTime = target.startTime,
                    endTime = target.endTime,
                    maxElevation = target.maxElevation
                )
            }

            // 서비스에 설정
            passScheduleService.setTrackingTargetList(trackingTargets)

            logger.info("위성 추적 스케줄 대상 목록 설정 성공: ${trackingTargets.size}개 대상")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "위성 추적 스케줄 대상 목록이 성공적으로 설정되었습니다.",
                    "data" to mapOf(
                        "totalTargets" to trackingTargets.size,
                        "uniqueSatellites" to trackingTargets.map { it.satelliteId }.distinct().size,
                        "targets" to trackingTargets.map { target ->
                            mapOf(
                                "mstId" to target.mstId,
                                "satelliteId" to target.satelliteId,
                                "satelliteName" to target.satelliteName,
                                "startTime" to target.startTime,
                                "endTime" to target.endTime,
                                "maxElevation" to target.maxElevation
                            )
                        }
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("위성 추적 스케줄 대상 목록 설정 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "위성 추적 스케줄 대상 목록 설정 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * ✅ 위성 추적 스케줄 대상 목록을 조회합니다.
     */
    @GetMapping("/tracking-targets")
    @Operation(
        operationId = "getpassscheduletargets",
        tags = ["Mode - Pass Schedule"]
    )
    fun getTrackingTargets(): ResponseEntity<Map<String, Any>> {
        return try {
            val trackingTargets = passScheduleService.getTrackingTargetList()

            logger.info("위성 추적 스케줄 대상 목록 조회 성공: ${trackingTargets.size}개 대상")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "위성 추적 스케줄 대상 목록 조회 성공",
                    "data" to mapOf(
                        "totalTargets" to trackingTargets.size,
                        "targets" to trackingTargets.map { target ->
                            mapOf(
                                "mstId" to target.mstId,
                                "satelliteId" to target.satelliteId,
                                "satelliteName" to target.satelliteName,
                                "startTime" to target.startTime,
                                "endTime" to target.endTime,
                                "maxElevation" to target.maxElevation,
                                "createdAt" to target.createdAt
                            )
                        }
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("위성 추적 스케줄 대상 목록 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "위성 추적 스케줄 대상 목록 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * ✅ 특정 위성의 추적 대상 목록을 조회합니다.
     */
    @GetMapping("/tracking-targets/satellite/{satelliteId}")
    @Operation(
        operationId = "getpassscheduletargetsbysatellite",
        tags = ["Mode - Pass Schedule"]
    )
    fun getTrackingTargetsBySatellite(@PathVariable satelliteId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val trackingTargets = passScheduleService.getTrackingTargetsBySatelliteId(satelliteId)

            logger.info("위성 $satelliteId 추적 대상 목록 조회 성공: ${trackingTargets.size}개 대상")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "위성별 추적 대상 목록 조회 성공",
                    "data" to mapOf(
                        "satelliteId" to satelliteId,
                        "totalTargets" to trackingTargets.size,
                        "targets" to trackingTargets.map { target ->
                            mapOf(
                                "mstId" to target.mstId,
                                "satelliteId" to target.satelliteId,
                                "satelliteName" to target.satelliteName,
                                "startTime" to target.startTime,
                                "endTime" to target.endTime,
                                "maxElevation" to target.maxElevation,
                                "createdAt" to target.createdAt
                            )
                        }
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("위성 $satelliteId 추적 대상 목록 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "위성별 추적 대상 목록 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * ✅ 특정 MST ID의 추적 대상을 조회합니다.
     */
    @GetMapping("/tracking-targets/mst/{mstId}")
    @Operation(
        operationId = "getpassscheduletargetbymstid",
        tags = ["Mode - Pass Schedule"]
    )
    /**
     * 특정 MST ID의 추적 대상을 조회하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param mstId 전역 고유 MstId (Long 타입)
     * @return 추적 대상 응답
     */
    fun getTrackingTargetByMstId(@PathVariable mstId: Long): ResponseEntity<Map<String, Any>> {  // ✅ UInt → Long 변경
        return try {
            val trackingTarget = passScheduleService.getTrackingTargetByMstId(mstId)

            if (trackingTarget != null) {
                logger.info("MST ID $mstId 추적 대상 조회 성공")

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "MST ID별 추적 대상 조회 성공",
                        "data" to mapOf(
                            "mstId" to trackingTarget.mstId,
                            "satelliteId" to trackingTarget.satelliteId,
                            "satelliteName" to trackingTarget.satelliteName,
                            "startTime" to trackingTarget.startTime,
                            "endTime" to trackingTarget.endTime,
                            "maxElevation" to trackingTarget.maxElevation,
                            "createdAt" to trackingTarget.createdAt
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                logger.warn("MST ID $mstId 추적 대상을 찾을 수 없음")
                ResponseEntity.status(404).body(
                    mapOf(
                        "success" to false,
                        "message" to "해당 MST ID의 추적 대상을 찾을 수 없습니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("MST ID $mstId 추적 대상 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "MST ID별 추적 대상 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * ✅ 추적 대상 목록을 초기화합니다.
     */
    @DeleteMapping("/tracking-targets")
    @Operation(
        operationId = "clearpassscheduletargets",
        tags = ["Mode - Pass Schedule"]
    )
    fun clearTrackingTargets(): ResponseEntity<Map<String, Any>> {
        return try {
            val beforeCount = passScheduleService.getTrackingTargetList().size
            passScheduleService.clearTrackingTargetList()

            logger.info("추적 대상 목록 초기화 성공: ${beforeCount}개 삭제")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "추적 대상 목록이 성공적으로 초기화되었습니다.",
                    "data" to mapOf(
                        "deletedCount" to beforeCount,
                        "remainingCount" to 0
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("추적 대상 목록 초기화 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "추적 대상 목록 초기화 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    // ==================== 선별된 추적 데이터 조회 API ====================

    /**
     * ✅ 특정 MST ID의 선별된 세부 데이터를 조회합니다.
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param mstId 전역 고유 MstId (Long 타입)
     * @return 선별된 세부 데이터 응답
     */
    fun getSelectedTrackingDetailByMstId(@PathVariable mstId: Long): ResponseEntity<Map<String, Any>> {  // ✅ UInt → Long 변경
        return try {
            val selectedDtlData = passScheduleService.getSelectedTrackDtlByMstId(mstId)

            if (selectedDtlData.isNotEmpty()) {
                logger.info("MST ID $mstId 선별된 세부 데이터 조회 성공: ${selectedDtlData.size}개 추적 포인트")

                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "선별된 세부 데이터 조회 성공",
                        "data" to mapOf(
                            "mstId" to mstId,
                            "trackingPointCount" to selectedDtlData.size,
                            "trackingPoints" to selectedDtlData
                        ),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                logger.warn("MST ID $mstId 선별된 세부 데이터 없음")
                ResponseEntity.status(404).body(
                    mapOf(
                        "success" to false,
                        "message" to "해당 MST ID의 선별된 추적 세부 데이터를 찾을 수 없습니다.",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("MST ID $mstId 선별된 세부 데이터 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "선별된 세부 데이터 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    // ==================== 추적 모니터링 제어 API ====================

    /**
     * ✅ 추적 모니터링 시작 (100ms 주기)
     */
    @PostMapping("/tracking/start")
    @Operation(
        operationId = "startpassscheduletracking",
        tags = ["Mode - Pass Schedule"]
    )
    fun startScheduleTracking(): ResponseEntity<Map<String, Any>> {
        return try {
            passScheduleService.startScheduleTracking()

            logger.info("추적 모니터링 시작 API 호출 성공")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "추적 모니터링이 성공적으로 시작되었습니다.",
                    "data" to mapOf(
                        "monitoringInterval" to "100ms",
                        "timeReference" to "GlobalData.Time.calUtcTimeOffsetTime",
                        "threadName" to "tracking-monitor",
                        "isRunning" to true
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("추적 모니터링 시작 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "추적 모니터링 시작 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * ✅ 추적 모니터링 중지
     */
    @PostMapping("/tracking/stop")
    @Operation(
        operationId = "stoppassscheduletracking",
        tags = ["Mode - Pass Schedule"]
    )
    fun stopScheduleTracking(): ResponseEntity<Map<String, Any>> {
        return try {
            passScheduleService.stopScheduleTracking()

            logger.info("추적 모니터링 중지 API 호출 성공")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "추적 모니터링이 성공적으로 중지되었습니다.",
                    "data" to mapOf(
                        "isRunning" to false,
                        "stoppedAt" to System.currentTimeMillis(),
                        "resourcesCleaned" to true
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("추적 모니터링 중지 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "추적 모니터링 중지 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * ✅ 추적 모니터링 상태 조회
     */
    @GetMapping("/tracking-monitor/status")
    @Operation(
        operationId = "getpassschedulemonitorstatus",
        tags = ["Mode - Pass Schedule"]
    )
    fun getTrackingMonitorStatus(): ResponseEntity<Map<String, Any>> {
        return try {
            val status = passScheduleService.getTrackingMonitorStatus()

            logger.info("추적 모니터링 상태 조회 성공")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "추적 모니터링 상태 조회 성공",
                    "data" to status,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("추적 모니터링 상태 조회 실패: ${e.message}", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "추적 모니터링 상태 조회 중 오류가 발생했습니다: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

}

/**
 * TLE 추가 요청 데이터 클래스
 * satelliteId는 선택적 (null 가능), tleLine1/tleLine2는 필수
 */
data class AddTleRequest(
    val satelliteId: String? = null,  // 선택적 필드 - null 허용
    val satelliteName: String? = null, // 새로 추가된 선택적 필드
    val tleLine1: String,             // 필수 필드
    val tleLine2: String              // 필수 필드
)

/**
 * TLE 업데이트 요청 데이터 클래스
 */
data class UpdateTleRequest(
    val satelliteName: String? = null, // 새로 추가된 선택적 필드
    val tleLine1: String,
    val tleLine2: String
)

/**
 * ✅ 추적 대상 설정 요청 데이터 클래스
 */
data class SetTrackingTargetsRequest(
    val targets: List<TrackingTargetRequest>
)

/**
 * ✅ 추적 대상 요청 데이터 클래스
 * 
 * PassSchedule 데이터 구조 리팩토링에 따라 mstId 타입을 UInt → Long으로 변경.
 * 
 * @param mstId 전역 고유 MstId (Long 타입)
 * @param satelliteId 위성 카탈로그 번호
 * @param satelliteName 위성 이름 (선택적)
 * @param startTime 추적 시작 시간
 * @param endTime 추적 종료 시간
 * @param maxElevation 최대 고도각
 */
data class TrackingTargetRequest(
    val mstId: Long,  // ✅ UInt → Long 변경
    val satelliteId: String,
    val satelliteName: String? = null,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val maxElevation: Double
)