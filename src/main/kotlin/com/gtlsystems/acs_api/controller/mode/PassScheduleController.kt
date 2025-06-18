package com.gtlsystems.acs_api.controller.mode

import com.gtlsystems.acs_api.service.mode.PassScheduleService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 패스 스케줄링 TLE 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/pass-schedule")
class PassScheduleController(
    private val passScheduleService: PassScheduleService
) {
    private val logger = LoggerFactory.getLogger(PassScheduleController::class.java)

    /**
     * TLE 데이터 추가
     */
    @PostMapping("/tle")
    fun addTle(@RequestBody request: AddTleRequest): ResponseEntity<Map<String, Any>> {
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
                request.tleLine2
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
    fun getTle(@PathVariable satelliteId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val tleData = passScheduleService.getPassScheduleTle(satelliteId)

            if (tleData != null) {
                logger.info("TLE 데이터 조회 성공: 위성 ID = $satelliteId")
                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "message" to "TLE 데이터 조회 성공",
                        "data" to mapOf(
                            "satelliteId" to satelliteId,
                            "tleLine1" to tleData.first,
                            "tleLine2" to tleData.second
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
    fun getAllTles(): ResponseEntity<Map<String, Any>> {
        return try {
            val satelliteIds = passScheduleService.getAllPassScheduleTleIds()
            val tleList = mutableListOf<Map<String, Any>>()

            satelliteIds.forEach { satelliteId ->
                val tleData = passScheduleService.getPassScheduleTle(satelliteId)
                if (tleData != null) {
                    tleList.add(
                        mapOf(
                            "satelliteId" to satelliteId,
                            "tleLine1" to tleData.first,
                            "tleLine2" to tleData.second
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
                request.tleLine2
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
}

/**
 * TLE 추가 요청 데이터 클래스
 * satelliteId는 선택적 (null 가능), tleLine1/tleLine2는 필수
 */
data class AddTleRequest(
    val satelliteId: String? = null,  // 선택적 필드 - null 허용
    val tleLine1: String,             // 필수 필드
    val tleLine2: String              // 필수 필드
)

/**
 * TLE 업데이트 요청 데이터 클래스
 */
data class UpdateTleRequest(
    val tleLine1: String,
    val tleLine2: String
)