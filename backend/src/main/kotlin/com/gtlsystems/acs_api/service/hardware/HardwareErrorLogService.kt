package com.gtlsystems.acs_api.service.hardware

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import com.gtlsystems.acs_api.model.PushData

/**
 * 하드웨어 에러 로그 관리 서비스 (메모리 기반)
 */
@Service
class HardwareErrorLogService {
    
        private val logger: Logger = LoggerFactory.getLogger(HardwareErrorLogService::class.java)
    
    // 이전 비트 상태 저장
    private val previousBitStates = ConcurrentHashMap<String, String>()
    
    // 에러 로그 저장소
    private val errorLogs = ConcurrentLinkedQueue<HardwareErrorLog>()
    
    // 팝업이 열린 클라이언트들 추적
    private val popupOpenClients = ConcurrentHashMap<String, PopupClientState>()
    
    /**
     * 안테나 데이터를 처리하여 에러 변화를 감지합니다
     */
    fun processAntennaData(data: PushData.ReadData): ErrorUpdateResult {
        val newErrors = mutableListOf<HardwareErrorLog>()
        var hasStateChanged = false
        
        try {
            // 🔍 디버깅: Elevation 관련 비트 값 상세 로그
            // logger.info("🔍 [DEBUG] elevationBoardServoStatusBits: '{}' (길이: {})", 
            //     data.elevationBoardServoStatusBits, data.elevationBoardServoStatusBits?.length ?: 0)
            // logger.info("🔍 [DEBUG] elevationBoardStatusBits: '{}' (길이: {})", 
            //     data.elevationBoardStatusBits, data.elevationBoardStatusBits?.length ?: 0)
            
            // 비트 타입들 정의
            val bitTypes = listOf(
                "mainBoardProtocolStatusBits",
                "mainBoardStatusBits", 
                "mainBoardMCOnOffBits",
                "mainBoardReserveBits",
                "azimuthBoardServoStatusBits",
                "azimuthBoardStatusBits",
                "elevationBoardServoStatusBits", 
                "elevationBoardStatusBits",
                "trainBoardServoStatusBits",
                "trainBoardStatusBits",
                "feedBoardETCStatusBits",
                "feedSBoardStatusBits",
                "feedXBoardStatusBits",
                "feedKaBoardStatusBits"
            )
            
            // 각 비트 타입별로 처리
            bitTypes.forEach { bitType ->
                val currentBits = getBitString(data, bitType)
                if (currentBits != null) {
                    val previousBits = previousBitStates[bitType]
                    
                    if (previousBits != null && previousBits != currentBits) {
                        logger.info("🔍 비트 변화 감지: {} - 이전: {}, 현재: {}", bitType, previousBits, currentBits)
                        
                        val errors = analyzeBitChanges(currentBits, previousBits, bitType)
                        newErrors.addAll(errors)
                        hasStateChanged = true
                    }
                    
                    // 현재 상태를 이전 상태로 저장
                    previousBitStates[bitType] = currentBits
                }
            }
            
        } catch (e: Exception) {
            logger.error("❌ 에러 처리 중 오류 발생: {}", e.message, e)
        }
        
        // 에러 로그 추가
        newErrors.forEach { addErrorLog(it) }
        
        return ErrorUpdateResult(
            hasStateChanged = hasStateChanged,
            newErrors = newErrors,
            latestError = newErrors.maxByOrNull { it.timestamp }
        )
    }
    
    /**
     * 비트 문자열을 데이터에서 추출합니다
     */
    private fun getBitString(data: PushData.ReadData, bitType: String): String? {
        return when (bitType) {
            "mainBoardProtocolStatusBits" -> data.mainBoardProtocolStatusBits
            "mainBoardStatusBits" -> data.mainBoardStatusBits
            "mainBoardMCOnOffBits" -> data.mainBoardMCOnOffBits
            "mainBoardReserveBits" -> data.mainBoardReserveBits
            "azimuthBoardServoStatusBits" -> data.azimuthBoardServoStatusBits
            "azimuthBoardStatusBits" -> data.azimuthBoardStatusBits
            "elevationBoardServoStatusBits" -> data.elevationBoardServoStatusBits
            "elevationBoardStatusBits" -> data.elevationBoardStatusBits
            "trainBoardServoStatusBits" -> data.trainBoardServoStatusBits
            "trainBoardStatusBits" -> data.trainBoardStatusBits
            "feedBoardETCStatusBits" -> data.feedBoardETCStatusBits
            "feedSBoardStatusBits" -> data.feedSBoardStatusBits
            "feedXBoardStatusBits" -> data.feedXBoardStatusBits
            "feedKaBoardStatusBits" -> data.feedKaBoardStatusBits
            else -> null
        }
    }
    
    /**
     * 비트 변화를 분석하여 에러 로그를 생성합니다
     */
    private fun analyzeBitChanges(currentBits: String, previousBits: String, bitType: String): List<HardwareErrorLog> {
        val errors = mutableListOf<HardwareErrorLog>()
        val errorMappings = getErrorMappings(bitType)
        
        // ✅ 비트 문자열을 뒤집어서 icdStore.ts와 동일한 방식으로 처리
        val reversedCurrentBits = currentBits.padStart(8, '0').reversed()
        val reversedPreviousBits = previousBits.padStart(8, '0').reversed()
        
        for (bitPosition in 0 until minOf(reversedCurrentBits.length, reversedPreviousBits.length, 8)) {
            val currentBit = reversedCurrentBits.getOrNull(bitPosition)?.toString() ?: "0"
            val previousBit = reversedPreviousBits.getOrNull(bitPosition)?.toString() ?: "0"
            
            logger.info("🔍 비트 {}: 현재={}, 이전={}, 변화={}", bitPosition, currentBit, previousBit, currentBit != previousBit)
            
            // 비트 변화 감지
            if (currentBit != previousBit) {
                val errorConfig = errorMappings[bitPosition]
                if (errorConfig != null) {
                val error = HardwareErrorLog(
                    id = "${bitType}-${bitPosition}-${System.currentTimeMillis()}",
                    timestamp = LocalDateTime.now().toString(),
                    category = errorConfig.category,
                    severity = if (currentBit == "1") errorConfig.severity else "INFO",
                    errorKey = errorConfig.errorKey,  // ✅ 에러 키만 저장
                    component = errorConfig.component,
                    isResolved = currentBit == "0",
                    resolvedAt = if (currentBit == "0") LocalDateTime.now().toString() else null
                    // message, resolvedMessage 제거 - 프론트엔드에서 처리
                )
                errors.add(error)
                logger.info("📝 에러 생성: {} - {}", errorConfig.component, errorConfig.errorKey)
                }
            }
        }
        
        logger.info("🔍 총 에러 개수: {}", errors.size)
        return errors
    }
    
    /**
     * 비트 타입별 에러 매핑 정의
     */
    private fun getErrorMappings(bitType: String): Map<Int, ErrorConfig> {
        return when (bitType) {
            "mainBoardProtocolStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_ELEVATION_ERROR", "Elevation Protocol"),
                    1 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_AZIMUTH_ERROR", "Azimuth Protocol"),
                    2 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_TRAIN_ERROR", "Train Protocol"),
                    3 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_FEED_ERROR", "Feed Protocol")
                )
            }
            
            "mainBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("POWER", "ERROR", "POWER_SURGE_PROTECTOR_FAULT", "Power Surge Protector"),
                    1 to ErrorConfig("POWER", "ERROR", "POWER_REVERSE_PHASE_FAULT", "Power Reverse Phase"),
                    2 to ErrorConfig("EMERGENCY", "CRITICAL", "EMERGENCY_STOP_ACU", "Emergency Stop ACU"),
                    3 to ErrorConfig("EMERGENCY", "CRITICAL", "EMERGENCY_STOP_POSITIONER", "Emergency Stop Positioner")
                )
            }
            
            "mainBoardMCOnOffBits" -> {
                mapOf(
                    0 to ErrorConfig("SERVO_POWER", "WARNING", "SERVO_TRAIN_POWER_OFF", "Train Servo Power"),
                    1 to ErrorConfig("SERVO_POWER", "WARNING", "SERVO_ELEVATION_POWER_OFF", "Elevation Servo Power"),
                    2 to ErrorConfig("SERVO_POWER", "WARNING", "SERVO_AZIMUTH_POWER_OFF", "Azimuth Servo Power")
                )
            }
            
            "azimuthBoardServoStatusBits" -> {
                mapOf(
                    // ICDService.kt의 AzimuthBoardServoStatus enum 순서대로 (reverse 적용 후)
                    0 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE1", "Azimuth Servo Alarm Code 1"), // SERVO_ALARM_CODE1(0)
                    1 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE2", "Azimuth Servo Alarm Code 2"), // SERVO_ALARM_CODE2(1)
                    2 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE3", "Azimuth Servo Alarm Code 3"), // SERVO_ALARM_CODE3(2)
                    3 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE4", "Azimuth Servo Alarm Code 4"), // SERVO_ALARM_CODE4(3)
                    4 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE5", "Azimuth Servo Alarm Code 5"), // SERVO_ALARM_CODE5(4)
                    5 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM", "Azimuth Servo Alarm"), // SERVO_ALARM_ERROR(5)
                    6 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_BRAKE_ENGAGED", "Azimuth Servo Brake") // SERVO_BRAKE(6)
                    // 7 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_MOTOR_ON", "Azimuth Servo Motor") // SERVO_MOTOR_MOVE(7) - 알림 비활성화
                )
            }
            
            "azimuthBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("POSITIONER", "WARNING", "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275", "Azimuth Limit Switch -275°"),
                    1 to ErrorConfig("POSITIONER", "WARNING", "AZIMUTH_LIMIT_SWITCH_POSITIVE_275", "Azimuth Limit Switch +275°"),
                    4 to ErrorConfig("STOW", "INFO", "AZIMUTH_STOW_PIN_ACTIVE", "Azimuth Stow Pin"),
                    7 to ErrorConfig("POSITIONER", "ERROR", "AZIMUTH_ENCODER_ERROR", "Azimuth Encoder")
                )
            }
            
            "elevationBoardServoStatusBits" -> {
                mapOf(
                    // ICDService.kt의 ElevationBoardServoStatus enum 순서대로 (reverse 적용 후)
                    0 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE1", "Elevation Servo Alarm Code 1"), // SERVO_ALARM_CODE1(0)
                    1 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE2", "Elevation Servo Alarm Code 2"), // SERVO_ALARM_CODE2(1)
                    2 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE3", "Elevation Servo Alarm Code 3"), // SERVO_ALARM_CODE3(2)
                    3 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE4", "Elevation Servo Alarm Code 4"), // SERVO_ALARM_CODE4(3)
                    4 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE5", "Elevation Servo Alarm Code 5"), // SERVO_ALARM_CODE5(4)
                    5 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM", "Elevation Servo Alarm"), // SERVO_ALARM_ERROR(5)
                    6 to ErrorConfig("SERVO_POWER", "INFO", "ELEVATION_SERVO_BRAKE_ENGAGED", "Elevation Servo Brake") // SERVO_BRAKE(6)
                    // 7 to ErrorConfig("SERVO_POWER", "INFO", "ELEVATION_SERVO_MOTOR_ON", "Elevation Servo Motor") // SERVO_MOTOR_MOVE(7) - 알림 비활성화
                )
            }
            
            "elevationBoardStatusBits" -> {
                mapOf(
                    // ICDService.kt의 ElevationBoardStatus enum 순서를 정확히 따라 매핑
                    0 to ErrorConfig("POSITIONER", "WARNING", "ELEVATION_LIMIT_SWITCH_POSITIVE_180", "Elevation Limit Switch +180°"), // limit_Switch_MaxOne_ON
                    1 to ErrorConfig("POSITIONER", "WARNING", "ELEVATION_LIMIT_SWITCH_POSITIVE_185", "Elevation Limit Switch +185°"), // limit_Switch_MaxTwo_ON
                    2 to ErrorConfig("POSITIONER", "WARNING", "ELEVATION_LIMIT_SWITCH_NEGATIVE_0", "Elevation Limit Switch -0°"), // limit_Switch_MinOne_ON
                    3 to ErrorConfig("POSITIONER", "WARNING", "ELEVATION_LIMIT_SWITCH_NEGATIVE_5", "Elevation Limit Switch -5°"), // limit_Switch_MinTwo_ON
                    4 to ErrorConfig("STOW", "INFO", "ELEVATION_STOW_PIN_ACTIVE", "Elevation Stow Pin"), // stow_Pin_ON
                    5 to ErrorConfig("SYSTEM", "INFO", "ELEVATION_RESERVE_FIVE", "Elevation Reserve Five"), // reserve_Five
                    6 to ErrorConfig("SYSTEM", "INFO", "ELEVATION_RESERVE_SIX", "Elevation Reserve Six"), // reserve_Six
                    7 to ErrorConfig("POSITIONER", "ERROR", "ELEVATION_ENCODER_ERROR", "Elevation Encoder") // encoder_Error
                )
            }
            
            "trainBoardServoStatusBits" -> {
                mapOf(
                    // ICDService.kt의 TiltBoardServoStatus enum 순서대로 (reverse 적용 후)
                    0 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE1", "Train Servo Alarm Code 1"), // SERVO_ALARM_CODE1(0)
                    1 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE2", "Train Servo Alarm Code 2"), // SERVO_ALARM_CODE2(1)
                    2 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE3", "Train Servo Alarm Code 3"), // SERVO_ALARM_CODE3(2)
                    3 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE4", "Train Servo Alarm Code 4"), // SERVO_ALARM_CODE4(3)
                    4 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE5", "Train Servo Alarm Code 5"), // SERVO_ALARM_CODE5(4)
                    5 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM", "Train Servo Alarm"), // SERVO_ALARM_ERROR(5)
                    6 to ErrorConfig("SERVO_POWER", "INFO", "TRAIN_SERVO_BRAKE_ENGAGED", "Train Servo Brake") // SERVO_BRAKE(6)
                    // 7 to ErrorConfig("SERVO_POWER", "INFO", "TRAIN_SERVO_MOTOR_ON", "Train Servo Motor") // SERVO_MOTOR_MOVE(7) - 알림 비활성화
                )
            }
            
            "trainBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("POSITIONER", "WARNING", "TRAIN_LIMIT_SWITCH_NEGATIVE_275", "Train Limit Switch -275°"),
                    1 to ErrorConfig("POSITIONER", "WARNING", "TRAIN_LIMIT_SWITCH_POSITIVE_275", "Train Limit Switch +275°"),
                    4 to ErrorConfig("STOW", "INFO", "TRAIN_STOW_PIN_ACTIVE", "Train Stow Pin"),
                    7 to ErrorConfig("POSITIONER", "ERROR", "TRAIN_ENCODER_ERROR", "Train Encoder")
                )
            }
            
            "feedSBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("FEED", "ERROR", "S_BAND_LNA_LHCP_ERROR", "S-Band LNA LHCP"),
                    1 to ErrorConfig("FEED", "ERROR", "S_BAND_LNA_RHCP_ERROR", "S-Band LNA RHCP"),
                    2 to ErrorConfig("FEED", "ERROR", "S_BAND_RF_SWITCH_ERROR", "S-Band RF Switch"),
                    3 to ErrorConfig("FEED", "INFO", "S_BAND_LNA_LHCP_POWER_ON", "S-Band LNA LHCP Power"),
                    4 to ErrorConfig("FEED", "INFO", "S_BAND_LNA_RHCP_POWER_ON", "S-Band LNA RHCP Power"),
                    5 to ErrorConfig("FEED", "INFO", "S_BAND_RF_SWITCH_RHCP", "S-Band RF Switch RHCP")
                )
            }
            
            "feedXBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("FEED", "ERROR", "X_BAND_LNA_LHCP_ERROR", "X-Band LNA LHCP"),
                    1 to ErrorConfig("FEED", "ERROR", "X_BAND_LNA_RHCP_ERROR", "X-Band LNA RHCP"),
                    2 to ErrorConfig("FEED", "ERROR", "FAN_ERROR", "Fan"),
                    3 to ErrorConfig("FEED", "INFO", "X_BAND_LNA_LHCP_POWER_ON", "X-Band LNA LHCP Power"),
                    4 to ErrorConfig("FEED", "INFO", "X_BAND_LNA_RHCP_POWER_ON", "X-Band LNA RHCP Power"),
                    5 to ErrorConfig("FEED", "INFO", "FAN_POWER_ON", "Fan Power")
                )
            }
            
            else -> emptyMap()
        }
    }
    
    /**
     * 에러 로그를 추가합니다
     */
    private fun addErrorLog(error: HardwareErrorLog) {
        errorLogs.offer(error)
        
        // 최대 1000개로 제한
        while (errorLogs.size > 1000) {
            errorLogs.poll()
        }
        
        logger.info("📝 에러 로그 추가: {} - {}", error.component, error.errorKey)
    }
    
    /**
     * 모든 에러 로그를 가져옵니다
     */
    fun getAllErrorLogs(): List<HardwareErrorLog> {
        return errorLogs.toList()
    }
    
    /**
     * 활성 에러 로그들을 반환합니다 (해결되지 않은 에러들)
     */
    fun getActiveErrorLogs(): List<HardwareErrorLog> {
        return errorLogs.filter { !it.isResolved }
    }

    /**
     * 페이징된 에러 로그 조회 (하이브리드 방식)
     */
    fun getErrorLogsPaginated(
        page: Int,
        size: Int,
        startDate: String?,
        endDate: String?,
        category: String?,
        severity: String?,
        resolvedStatus: String?
    ): Map<String, Any> {
        // 1. 필터링된 로그 목록 생성
        var filteredLogs = errorLogs.toList()
        
        // 날짜 필터링
        if (startDate != null) {
            val start = java.time.LocalDateTime.parse("${startDate}T00:00:00")
            filteredLogs = filteredLogs.filter { 
                java.time.LocalDateTime.parse(it.timestamp) >= start 
            }
        }
        if (endDate != null) {
            val end = java.time.LocalDateTime.parse("${endDate}T23:59:59")
            filteredLogs = filteredLogs.filter { 
                java.time.LocalDateTime.parse(it.timestamp) <= end 
            }
        }
        
        // 카테고리 필터링
        if (category != null) {
            filteredLogs = filteredLogs.filter { it.category == category }
        }
        
        // 심각도 필터링
        if (severity != null) {
            filteredLogs = filteredLogs.filter { it.severity == severity }
        }
        
        // 해결 상태 필터링
        if (resolvedStatus != null) {
            when (resolvedStatus) {
                "resolved" -> filteredLogs = filteredLogs.filter { it.isResolved }
                "unresolved" -> filteredLogs = filteredLogs.filter { !it.isResolved }
            }
        }
        
        // 2. 최신순 정렬
        filteredLogs = filteredLogs.sortedByDescending { it.timestamp }
        
        // 3. 페이징 계산
        val totalElements = filteredLogs.size
        val totalPages = (totalElements + size - 1) / size
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, totalElements)
        
        // 4. 현재 페이지 데이터 추출
        val content = if (startIndex < totalElements) {
            filteredLogs.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        // 5. 페이징 정보 반환
        return mapOf(
            "content" to content,
            "pageable" to mapOf(
                "pageNumber" to page,
                "pageSize" to size,
                "sort" to mapOf("sorted" to true, "unsorted" to false)
            ),
            "totalElements" to totalElements,
            "totalPages" to totalPages,
            "first" to (page == 0),
            "last" to (page >= totalPages - 1),
            "numberOfElements" to content.size,
            "size" to size,
            "number" to page
        )
    }

    /**
     * 팝업 상태를 설정합니다
     */
    fun setPopupState(clientId: String, isOpen: Boolean): PopupResponse? {
        return if (isOpen) {
            val clientState = PopupClientState(
                isOpen = true,
                lastReceivedLogId = null,
                lastUpdateTime = System.currentTimeMillis()
            )
            popupOpenClients[clientId] = clientState
            
            PopupResponse(
                isInitialLoad = true,
                allLogs = errorLogs.toList(),
                statusBarData = getStatusBarData()
            )
        } else {
            popupOpenClients.remove(clientId)
            null
        }
    }
    
    /**
     * 클라이언트별 데이터를 생성합니다
     */
    fun getClientData(clientId: String): ClientErrorData {
        val statusBarData = getStatusBarData()
        val popupData = if (popupOpenClients.containsKey(clientId)) {
            val clientState = popupOpenClients[clientId]
            val newLogs = if (clientState?.lastReceivedLogId != null) {
                getNewLogsSince(clientState.lastReceivedLogId!!)
            } else {
                emptyList()
            }
            
            // 마지막 로그 ID 업데이트
            if (newLogs.isNotEmpty()) {
                clientState?.lastReceivedLogId = newLogs.maxByOrNull { it.timestamp }?.id
                clientState?.lastUpdateTime = System.currentTimeMillis()
            }
            
            ErrorPopupData(
                isInitialLoad = false,
                newLogs = newLogs,
                totalLogCount = errorLogs.size,
                lastUpdateTime = System.currentTimeMillis()
            )
        } else {
            null
        }
        
        // WebSocket 전송용 데이터 반환 (이미 메시지가 제거된 상태)
        return ClientErrorData(statusBarData, popupData)
    }
    
    /**
     * 특정 로그 ID 이후의 새로운 로그들을 가져옵니다
     */
    private fun getNewLogsSince(lastLogId: String): List<HardwareErrorLog> {
        val allLogs = errorLogs.toList().sortedBy { it.timestamp }
        val lastLogIndex = allLogs.indexOfFirst { it.id == lastLogId }
        
        return if (lastLogIndex >= 0 && lastLogIndex < allLogs.size - 1) {
            allLogs.subList(lastLogIndex + 1, allLogs.size)
        } else {
            emptyList()
        }
    }
    
    /**
     * 상태바 데이터를 생성합니다
     */
    private fun getStatusBarData(): ErrorStatusBarData {
        val activeErrors = getActiveErrorLogs()
        val latestError = errorLogs.maxByOrNull { it.timestamp }
        
        // 최신 에러가 해결된 에러인 경우, 해결된 지 30초 이내인지 확인
        val shouldShowResolvedError: Boolean = if (latestError?.isResolved == true) {
            val resolvedTime = latestError.resolvedAt?.let { 
                try { LocalDateTime.parse(it) } catch (e: Exception) { null }
            }
            resolvedTime?.let { 
                Duration.between(it, LocalDateTime.now()).seconds <= 30 
            } ?: false
        } else {
            false
        }
        
        return ErrorStatusBarData(
            activeErrorCount = activeErrors.size,
            latestError = if (shouldShowResolvedError) latestError else activeErrors.maxByOrNull { it.timestamp },
            hasNewErrors = activeErrors.isNotEmpty() || shouldShowResolvedError
        )
    }
    
    /**
     * 테스트용 에러 로그 생성
     */
    fun createTestErrorLog() {
        logger.info("🔍 createTestErrorLog() 메서드 호출됨")
        
        val testError = HardwareErrorLog(
            id = UUID.randomUUID().toString(),
            timestamp = LocalDateTime.now().toString(),
            category = "TEST",
            severity = "ERROR",
            errorKey = "TEST_ERROR",  // ✅ 에러 키 추가
            component = "Test Component",
            isResolved = false,
            resolvedAt = null
        )
        
        logger.info("🔍 테스트 에러 객체 생성됨: {}", testError.id)
        
        addErrorLog(testError)
        
        logger.info("🔍 addErrorLog() 호출 완료")
        logger.info("🔍 현재 에러 로그 개수: {}", errorLogs.size)
        logger.info("✅ 테스트 에러 로그 생성됨")
    }
    
    /**
     * 테스트용 해결된 에러 로그 생성
     */
    fun createTestResolvedErrorLog() {
        logger.info("🔍 createTestResolvedErrorLog() 메서드 호출됨")
        
        val testResolvedError = HardwareErrorLog(
            id = UUID.randomUUID().toString(),
            timestamp = LocalDateTime.now().toString(),
            category = "TEST",
            severity = "INFO",
            errorKey = "TEST_ERROR_RESOLVED",  // ✅ 에러 키 추가
            component = "Test Component",
            isResolved = true,
            resolvedAt = LocalDateTime.now().toString()
        )
        
        logger.info("🔍 테스트 해결 에러 객체 생성됨: {}", testResolvedError.id)
        
        addErrorLog(testResolvedError)
        
        logger.info("🔍 addErrorLog() 호출 완료")
        logger.info("🔍 현재 에러 로그 개수: {}", errorLogs.size)
        logger.info("✅ 테스트 해결 에러 로그 생성됨")
    }
}

/**
 * 에러 설정 데이터 클래스
 */
data class ErrorConfig(
    val category: String,
    val severity: String,
    val errorKey: String,
    val component: String
)

/**
 * 하드웨어 에러 로그 데이터 클래스
 */
data class HardwareErrorLog(
    val id: String,
    val timestamp: String,
    val category: String,
    val severity: String,
    val errorKey: String,        // ✅ 에러 키만 유지
    val component: String,
    val isResolved: Boolean,
    val resolvedAt: String?
)

/**
 * 에러 업데이트 결과 데이터 클래스
 */
data class ErrorUpdateResult(
    val hasStateChanged: Boolean,
    val newErrors: List<HardwareErrorLog>,
    val latestError: HardwareErrorLog?
)

/**
 * 팝업 클라이언트 상태 데이터 클래스
 */
data class PopupClientState(
    val isOpen: Boolean,
    var lastReceivedLogId: String?,
    var lastUpdateTime: Long
)

/**
 * 팝업 응답 데이터 클래스
 */
data class PopupResponse(
    val isInitialLoad: Boolean,
    val allLogs: List<HardwareErrorLog>,
    val statusBarData: ErrorStatusBarData
)

/**
 * 클라이언트 에러 데이터 클래스
 */
data class ClientErrorData(
    val statusBarData: ErrorStatusBarData,
    val popupData: ErrorPopupData?
)

/**
 * 에러 상태바 데이터 클래스
 */
data class ErrorStatusBarData(
    val activeErrorCount: Int,
    val latestError: HardwareErrorLog?,
    val hasNewErrors: Boolean
)

/**
 * 에러 팝업 데이터 클래스
 */
data class ErrorPopupData(
    val isInitialLoad: Boolean,
    val newLogs: List<HardwareErrorLog>,
    val totalLogCount: Int,
    val lastUpdateTime: Long
)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   