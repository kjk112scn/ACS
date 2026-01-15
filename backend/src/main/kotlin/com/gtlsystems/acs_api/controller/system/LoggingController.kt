package com.gtlsystems.acs_api.controller.system

import com.gtlsystems.acs_api.service.system.LoggingService
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * ACS 시스템의 로그 관리를 위한 REST API 컨트롤러
 * 
 * 주요 기능:
 * - 로그 레벨 동적 변경
 * - 로그 파일 다운로드
 * - 로그 통계 조회
 * - 비즈니스 로그 조회
 * - 로그 초기화
 */
@RestController
@RequestMapping("/api/logging")
@Tag(name = "logging-controller", description = "로깅 관리 - 시스템 로깅 및 로그 레벨 제어")
class LoggingController(
    private val loggingService: LoggingService
) {
    
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LoggingController::class.java)
        private const val LOGS_DIRECTORY = "logs"
        private const val LOG_ARCHIVE_DIRECTORY = "logs/archive"
    }
    
    /**
     * 로그 레벨 동적 변경
     */
    @PostMapping("/level")
    fun changeLogLevel(
        @RequestBody request: ChangeLogLevelRequest
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            logger.info("로그 레벨 변경 요청: 패키지=${request.packageName}, 레벨=${request.level}")
            
            // 실제 로그 레벨 변경은 Logback 설정을 통해 처리
            // 여기서는 요청을 받아서 로깅만 수행
            loggingService.logBusinessAction(
                userId = request.userId,
                action = "CHANGE_LOG_LEVEL",
                details = "패키지: ${request.packageName}, 레벨: ${request.level}",
                result = "SUCCESS"
            )
            
            val response = ApiResponse(
                success = true,
                message = "로그 레벨 변경 요청이 처리되었습니다.",
                data = "패키지: ${request.packageName}, 레벨: ${request.level}",
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("로그 레벨 변경 중 오류 발생", e)
            
            val response = ApiResponse<String>(
                success = false,
                message = "로그 레벨 변경 중 오류가 발생했습니다: ${e.message}",
                data = null,
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * 로그 파일 목록 조회
     */
    @GetMapping("/files")
    fun getLogFiles(): ResponseEntity<ApiResponse<List<LogFileInfo>>> {
        return try {
            logger.info("로그 파일 목록 조회 요청")
            
            val logFiles = mutableListOf<LogFileInfo>()
            val logsDir = Paths.get(LOGS_DIRECTORY)
            val archiveDir = Paths.get(LOG_ARCHIVE_DIRECTORY)
            
            // 메인 로그 파일들 조회
            if (Files.exists(logsDir)) {
                Files.list(logsDir)
                    .filter { it.toString().endsWith(".log") }
                    .forEach { path ->
                        val file = path.toFile()
                        logFiles.add(
                            LogFileInfo(
                                fileName = file.name,
                                filePath = file.absolutePath,
                                fileSize = file.length(),
                                lastModified = LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(path).toInstant(),
                                    java.time.ZoneId.systemDefault()
                                ),
                                fileType = "MAIN"
                            )
                        )
                    }
            }
            
            // 아카이브 로그 파일들 조회
            if (Files.exists(archiveDir)) {
                Files.walk(archiveDir)
                    .filter { it.toString().endsWith(".log") }
                    .forEach { path ->
                        val file = path.toFile()
                        logFiles.add(
                            LogFileInfo(
                                fileName = file.name,
                                filePath = file.absolutePath,
                                fileSize = file.length(),
                                lastModified = LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(path).toInstant(),
                                    java.time.ZoneId.systemDefault()
                                ),
                                fileType = "ARCHIVE"
                            )
                        )
                    }
            }
            
            // 파일 크기 순으로 정렬 (최신 파일이 위에 오도록)
            logFiles.sortByDescending { it.lastModified }
            
            val response = ApiResponse(
                success = true,
                message = "로그 파일 목록을 성공적으로 조회했습니다.",
                data = logFiles.toList(),
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("로그 파일 목록 조회 중 오류 발생", e)
            
            val response = ApiResponse<List<LogFileInfo>>(
                success = false,
                message = "로그 파일 목록 조회 중 오류가 발생했습니다: ${e.message}",
                data = emptyList(),
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * 로그 파일 다운로드
     */
    @GetMapping("/download/{fileName}")
    fun downloadLogFile(
        @PathVariable fileName: String
    ): ResponseEntity<ByteArray> {
        return try {
            logger.info("로그 파일 다운로드 요청: $fileName")

            // Path Traversal 공격 방지: 파일명 검증
            val logsBaseDir = Paths.get(LOGS_DIRECTORY).normalize().toAbsolutePath()
            val archiveBaseDir = Paths.get(LOG_ARCHIVE_DIRECTORY).normalize().toAbsolutePath()

            val filePath = Paths.get(LOGS_DIRECTORY, fileName).normalize().toAbsolutePath()
            val archivePath = Paths.get(LOG_ARCHIVE_DIRECTORY, fileName).normalize().toAbsolutePath()

            val targetPath = when {
                Files.exists(filePath) && filePath.startsWith(logsBaseDir) -> filePath
                Files.exists(archivePath) && archivePath.startsWith(archiveBaseDir) -> archivePath
                filePath.startsWith(logsBaseDir) || archivePath.startsWith(archiveBaseDir) ->
                    throw IllegalArgumentException("로그 파일을 찾을 수 없습니다: $fileName")
                else -> throw IllegalArgumentException("잘못된 파일 경로입니다: $fileName")
            }
            
            val fileBytes = Files.readAllBytes(targetPath)
            
            // 비즈니스 로그 기록
            loggingService.logBusinessAction(
                userId = null,
                action = "DOWNLOAD_LOG_FILE",
                details = "파일명: $fileName, 크기: ${fileBytes.size} bytes",
                result = "SUCCESS"
            )
            
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            headers.setContentDispositionFormData("attachment", fileName)
            
            ResponseEntity.ok()
                .headers(headers)
                .body(fileBytes)
                
        } catch (e: Exception) {
            logger.error("로그 파일 다운로드 중 오류 발생: $fileName", e)
            
            // 비즈니스 로그 기록
            loggingService.logBusinessAction(
                userId = null,
                action = "DOWNLOAD_LOG_FILE",
                details = "파일명: $fileName",
                result = "FAILED: ${e.message}"
            )
            
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
    
    /**
     * 로그 통계 조회
     */
    @GetMapping("/statistics")
    fun getLogStatistics(): ResponseEntity<ApiResponse<Map<String, Long>>> {
        return try {
            logger.info("로그 통계 조회 요청")
            
            val statistics = loggingService.getLogStatistics()
            
            val response = ApiResponse(
                success = true,
                message = "로그 통계를 성공적으로 조회했습니다.",
                data = statistics,
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("로그 통계 조회 중 오류 발생", e)
            
            val response = ApiResponse<Map<String, Long>>(
                success = false,
                message = "로그 통계 조회 중 오류가 발생했습니다: ${e.message}",
                data = emptyMap(),
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * 비즈니스 로그 조회
     */
    @GetMapping("/business")
    fun getBusinessLogs(
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<ApiResponse<List<LoggingService.BusinessLogEntry>>> {
        return try {
            logger.info("비즈니스 로그 조회 요청: limit=$limit")
            
            val businessLogs = loggingService.getRecentBusinessLogs(limit)
            
            val response = ApiResponse(
                success = true,
                message = "비즈니스 로그를 성공적으로 조회했습니다.",
                data = businessLogs,
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("비즈니스 로그 조회 중 오류 발생", e)
            
            val response = ApiResponse<List<LoggingService.BusinessLogEntry>>(
                success = false,
                message = "비즈니스 로그 조회 중 오류가 발생했습니다: ${e.message}",
                data = emptyList(),
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * 로그 통계 초기화
     */
    @PostMapping("/statistics/reset")
    fun resetLogStatistics(): ResponseEntity<ApiResponse<String>> {
        return try {
            logger.info("로그 통계 초기화 요청")
            
            loggingService.resetLogStatistics()
            
            // 비즈니스 로그 기록
            loggingService.logBusinessAction(
                userId = null,
                action = "RESET_LOG_STATISTICS",
                details = "로그 통계 초기화 완료",
                result = "SUCCESS"
            )
            
            val response = ApiResponse(
                success = true,
                message = "로그 통계가 성공적으로 초기화되었습니다.",
                data = "로그 통계 초기화 완료",
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("로그 통계 초기화 중 오류 발생", e)
            
            val response = ApiResponse<String>(
                success = false,
                message = "로그 통계 초기화 중 오류가 발생했습니다: ${e.message}",
                data = null,
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * 비즈니스 로그 초기화
     */
    @PostMapping("/business/clear")
    fun clearBusinessLogs(): ResponseEntity<ApiResponse<String>> {
        return try {
            logger.info("비즈니스 로그 초기화 요청")
            
            loggingService.clearBusinessLogs()
            
            // 비즈니스 로그 기록
            loggingService.logBusinessAction(
                userId = null,
                action = "CLEAR_BUSINESS_LOGS",
                details = "비즈니스 로그 초기화 완료",
                result = "SUCCESS"
            )
            
            val response = ApiResponse(
                success = true,
                message = "비즈니스 로그가 성공적으로 초기화되었습니다.",
                data = "비즈니스 로그 초기화 완료",
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("비즈니스 로그 초기화 중 오류 발생", e)
            
            val response = ApiResponse<String>(
                success = false,
                message = "비즈니스 로그 초기화 중 오류가 발생했습니다: ${e.message}",
                data = null,
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * 시스템 상태 확인 (헬스체크)
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<ApiResponse<String>> {
        return try {
            logger.debug("로그 시스템 헬스체크 요청")
            
            val response = ApiResponse(
                success = true,
                message = "로그 시스템이 정상적으로 동작하고 있습니다.",
                data = "OK",
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("로그 시스템 헬스체크 중 오류 발생", e)
            
            val response = ApiResponse<String>(
                success = false,
                message = "로그 시스템에 문제가 발생했습니다: ${e.message}",
                data = "ERROR",
                timestamp = LocalDateTime.now()
            )
            
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
        }
    }
    
    // Data classes
    
    /**
     * 로그 레벨 변경 요청 데이터
     */
    data class ChangeLogLevelRequest(
        val packageName: String,
        val level: String,
        val userId: String? = null
    )
    
    /**
     * 로그 파일 정보 데이터
     */
    data class LogFileInfo(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val lastModified: LocalDateTime,
        val fileType: String
    )
    
    /**
     * API 응답 공통 데이터
     */
    data class ApiResponse<T>(
        val success: Boolean,
        val message: String,
        val data: T?,
        val timestamp: LocalDateTime
    )
} 