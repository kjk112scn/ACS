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
            logger.info("🔍 [DEBUG] elevationBoardServoStatusBits: '{}' (길이: {})", 
                data.elevationBoardServoStatusBits, data.elevationBoardServoStatusBits?.length ?: 0)
            logger.info("🔍 [DEBUG] elevationBoardStatusBits: '{}' (길이: {})", 
                data.elevationBoardStatusBits, data.elevationBoardStatusBits?.length ?: 0)
            
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
                "feedSBoardStatusBits",
                "feedXBoardStatusBits"
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
            "feedSBoardStatusBits" -> data.feedSBoardStatusBits
            "feedXBoardStatusBits" -> data.feedXBoardStatusBits
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
                        message = if (currentBit == "1") errorConfig.errorMessage else errorConfig.resolvedMessage,
                    component = errorConfig.component,
                        isResolved = currentBit == "0",
                        resolvedAt = if (currentBit == "0") LocalDateTime.now().toString() else null,
                        resolvedMessage = if (currentBit == "0") errorConfig.resolvedMessage else null
                )
                errors.add(error)
                logger.info("📝 에러 생성: {} - {}", errorConfig.component, 
                               if (currentBit == "1") errorConfig.errorMessage["ko"] else errorConfig.resolvedMessage["ko"])
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
                    6 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_BRAKE_ENGAGED", "Azimuth Servo Brake"), // SERVO_BRAKE(6)
                    7 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_MOTOR_ON", "Azimuth Servo Motor") // SERVO_MOTOR_MOVE(7)
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
                    6 to ErrorConfig("SERVO_POWER", "INFO", "ELEVATION_SERVO_BRAKE_ENGAGED", "Elevation Servo Brake"), // SERVO_BRAKE(6)
                    7 to ErrorConfig("SERVO_POWER", "INFO", "ELEVATION_SERVO_MOTOR_ON", "Elevation Servo Motor") // SERVO_MOTOR_MOVE(7)
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
                    6 to ErrorConfig("SERVO_POWER", "INFO", "TRAIN_SERVO_BRAKE_ENGAGED", "Train Servo Brake"), // SERVO_BRAKE(6)
                    7 to ErrorConfig("SERVO_POWER", "INFO", "TRAIN_SERVO_MOTOR_ON", "Train Servo Motor") // SERVO_MOTOR_MOVE(7)
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
        
        logger.info("📝 에러 로그 추가: {} - {}", error.component, error.message["ko"])
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
            message = mapOf("ko" to "테스트 에러 발생", "en" to "Test Error Occurred"),
            component = "Test Component",
            isResolved = false,
            resolvedAt = null,
            resolvedMessage = null
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
            message = mapOf("ko" to "테스트 에러 해결됨", "en" to "Test Error Resolved"),
            component = "Test Component",
            isResolved = true,
            resolvedAt = LocalDateTime.now().toString(),
            resolvedMessage = mapOf("ko" to "테스트 에러가 해결되었습니다", "en" to "Test error has been resolved")
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
) {
    val errorMessage: Map<String, String>
        get() = mapOf(
            "ko" to getErrorMessage(errorKey, "ko"),
            "en" to getErrorMessage(errorKey, "en")
        )
    
    val resolvedMessage: Map<String, String>
        get() = mapOf(
            "ko" to getResolvedMessage(errorKey, "ko"),
            "en" to getResolvedMessage(errorKey, "en")
        )
    
    private fun getErrorMessage(key: String, lang: String): String {
        return when (lang) {
            "ko" -> when (key) {
                "ELEVATION_SERVO_ALARM" -> "Elevation 서보 알람"
                "ELEVATION_SERVO_ALARM_CODE1" -> "Elevation 서보 알람 코드 1"
                "ELEVATION_SERVO_ALARM_CODE2" -> "Elevation 서보 알람 코드 2"
                "ELEVATION_SERVO_ALARM_CODE3" -> "Elevation 서보 알람 코드 3"
                "ELEVATION_SERVO_ALARM_CODE4" -> "Elevation 서보 알람 코드 4"
                "ELEVATION_SERVO_ALARM_CODE5" -> "Elevation 서보 알람 코드 5"
                "AZIMUTH_SERVO_ALARM" -> "Azimuth 서보 알람"
                "AZIMUTH_SERVO_ALARM_CODE1" -> "Azimuth 서보 알람 코드 1"
                "AZIMUTH_SERVO_ALARM_CODE2" -> "Azimuth 서보 알람 코드 2"
                "AZIMUTH_SERVO_ALARM_CODE3" -> "Azimuth 서보 알람 코드 3"
                "AZIMUTH_SERVO_ALARM_CODE4" -> "Azimuth 서보 알람 코드 4"
                "AZIMUTH_SERVO_ALARM_CODE5" -> "Azimuth 서보 알람 코드 5"
                "TRAIN_SERVO_ALARM" -> "Train 서보 알람"
                "TRAIN_SERVO_ALARM_CODE1" -> "Train 서보 알람 코드 1"
                "TRAIN_SERVO_ALARM_CODE2" -> "Train 서보 알람 코드 2"
                "TRAIN_SERVO_ALARM_CODE3" -> "Train 서보 알람 코드 3"
                "TRAIN_SERVO_ALARM_CODE4" -> "Train 서보 알람 코드 4"
                "TRAIN_SERVO_ALARM_CODE5" -> "Train 서보 알람 코드 5"
                "ELEVATION_ENCODER_ERROR" -> "Elevation 인코더 에러"
                "AZIMUTH_ENCODER_ERROR" -> "Azimuth 인코더 에러"
                "TRAIN_ENCODER_ERROR" -> "Train 인코더 에러"
                "POWER_SURGE_PROTECTOR_FAULT" -> "전력 서지 보호기 고장"
                "POWER_REVERSE_PHASE_FAULT" -> "전력 역상 고장"
                "EMERGENCY_STOP_ACU" -> "비상 정지 ACU"
                "EMERGENCY_STOP_POSITIONER" -> "비상 정지 포지셔너"
                "ELEVATION_SERVO_BRAKE_ENGAGED" -> "Elevation 서보 브레이크 작동"
                "AZIMUTH_SERVO_BRAKE_ENGAGED" -> "Azimuth 서보 브레이크 작동"
                "TRAIN_SERVO_BRAKE_ENGAGED" -> "Train 서보 브레이크 작동"
                "ELEVATION_SERVO_MOTOR_ON" -> "Elevation 서보 모터 켜짐"
                "AZIMUTH_SERVO_MOTOR_ON" -> "Azimuth 서보 모터 켜짐"
                "TRAIN_SERVO_MOTOR_ON" -> "Train 서보 모터 켜짐"
                "ELEVATION_LIMIT_SWITCH_POSITIVE_180" -> "Elevation 한계 스위치 +180°"
                "ELEVATION_LIMIT_SWITCH_POSITIVE_185" -> "Elevation 한계 스위치 +185°"
                "ELEVATION_LIMIT_SWITCH_NEGATIVE_0" -> "Elevation 한계 스위치 -0°"
                "ELEVATION_LIMIT_SWITCH_NEGATIVE_5" -> "Elevation 한계 스위치 -5°"
                "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275" -> "Azimuth 한계 스위치 -275°"
                "AZIMUTH_LIMIT_SWITCH_POSITIVE_275" -> "Azimuth 한계 스위치 +275°"
                "TRAIN_LIMIT_SWITCH_NEGATIVE_275" -> "Train 한계 스위치 -275°"
                "TRAIN_LIMIT_SWITCH_POSITIVE_275" -> "Train 한계 스위치 +275°"
                "ELEVATION_STOW_PIN_ACTIVE" -> "Elevation 스토우 핀 활성"
                "AZIMUTH_STOW_PIN_ACTIVE" -> "Azimuth 스토우 핀 활성"
                "TRAIN_STOW_PIN_ACTIVE" -> "Train 스토우 핀 활성"
                "SERVO_TRAIN_POWER_OFF" -> "Train 서보 전원 꺼짐"
                "SERVO_ELEVATION_POWER_OFF" -> "Elevation 서보 전원 꺼짐"
                "SERVO_AZIMUTH_POWER_OFF" -> "Azimuth 서보 전원 꺼짐"
                "PROTOCOL_ELEVATION_ERROR" -> "Elevation 프로토콜 에러"
                "PROTOCOL_AZIMUTH_ERROR" -> "Azimuth 프로토콜 에러"
                "PROTOCOL_TRAIN_ERROR" -> "Train 프로토콜 에러"
                "PROTOCOL_FEED_ERROR" -> "Feed 프로토콜 에러"
                "S_BAND_LNA_LHCP_ERROR" -> "S-Band LNA LHCP 에러"
                "S_BAND_LNA_RHCP_ERROR" -> "S-Band LNA RHCP 에러"
                "S_BAND_RF_SWITCH_ERROR" -> "S-Band RF 스위치 에러"
                "X_BAND_LNA_LHCP_ERROR" -> "X-Band LNA LHCP 에러"
                "X_BAND_LNA_RHCP_ERROR" -> "X-Band LNA RHCP 에러"
                "FAN_ERROR" -> "팬 에러"
                "S_BAND_LNA_LHCP_POWER_ON" -> "S-Band LNA LHCP 전원 켜짐"
                "S_BAND_LNA_RHCP_POWER_ON" -> "S-Band LNA RHCP 전원 켜짐"
                "S_BAND_RF_SWITCH_RHCP" -> "S-Band RF 스위치 RHCP"
                "X_BAND_LNA_LHCP_POWER_ON" -> "X-Band LNA LHCP 전원 켜짐"
                "X_BAND_LNA_RHCP_POWER_ON" -> "X-Band LNA RHCP 전원 켜짐"
                "FAN_POWER_ON" -> "팬 전원 켜짐"
                else -> "알 수 없는 에러"
            }
            "en" -> when (key) {
                "ELEVATION_SERVO_ALARM" -> "Elevation Servo Alarm"
                "ELEVATION_SERVO_ALARM_CODE1" -> "Elevation Servo Alarm Code 1"
                "ELEVATION_SERVO_ALARM_CODE2" -> "Elevation Servo Alarm Code 2"
                "ELEVATION_SERVO_ALARM_CODE3" -> "Elevation Servo Alarm Code 3"
                "ELEVATION_SERVO_ALARM_CODE4" -> "Elevation Servo Alarm Code 4"
                "ELEVATION_SERVO_ALARM_CODE5" -> "Elevation Servo Alarm Code 5"
                "AZIMUTH_SERVO_ALARM" -> "Azimuth Servo Alarm"
                "AZIMUTH_SERVO_ALARM_CODE1" -> "Azimuth Servo Alarm Code 1"
                "AZIMUTH_SERVO_ALARM_CODE2" -> "Azimuth Servo Alarm Code 2"
                "AZIMUTH_SERVO_ALARM_CODE3" -> "Azimuth Servo Alarm Code 3"
                "AZIMUTH_SERVO_ALARM_CODE4" -> "Azimuth Servo Alarm Code 4"
                "AZIMUTH_SERVO_ALARM_CODE5" -> "Azimuth Servo Alarm Code 5"
                "TRAIN_SERVO_ALARM" -> "Train Servo Alarm"
                "TRAIN_SERVO_ALARM_CODE1" -> "Train Servo Alarm Code 1"
                "TRAIN_SERVO_ALARM_CODE2" -> "Train Servo Alarm Code 2"
                "TRAIN_SERVO_ALARM_CODE3" -> "Train Servo Alarm Code 3"
                "TRAIN_SERVO_ALARM_CODE4" -> "Train Servo Alarm Code 4"
                "TRAIN_SERVO_ALARM_CODE5" -> "Train Servo Alarm Code 5"
                "ELEVATION_ENCODER_ERROR" -> "Elevation Encoder Error"
                "AZIMUTH_ENCODER_ERROR" -> "Azimuth Encoder Error"
                "TRAIN_ENCODER_ERROR" -> "Train Encoder Error"
                "POWER_SURGE_PROTECTOR_FAULT" -> "Power Surge Protector Fault"
                "POWER_REVERSE_PHASE_FAULT" -> "Power Reverse Phase Fault"
                "EMERGENCY_STOP_ACU" -> "Emergency Stop ACU"
                "EMERGENCY_STOP_POSITIONER" -> "Emergency Stop Positioner"
                "ELEVATION_SERVO_BRAKE_ENGAGED" -> "Elevation Servo Brake Engaged"
                "AZIMUTH_SERVO_BRAKE_ENGAGED" -> "Azimuth Servo Brake Engaged"
                "TRAIN_SERVO_BRAKE_ENGAGED" -> "Train Servo Brake Engaged"
                "ELEVATION_SERVO_MOTOR_ON" -> "Elevation Servo Motor On"
                "AZIMUTH_SERVO_MOTOR_ON" -> "Azimuth Servo Motor On"
                "TRAIN_SERVO_MOTOR_ON" -> "Train Servo Motor On"
                "ELEVATION_LIMIT_SWITCH_POSITIVE_180" -> "Elevation Limit Switch +180°"
                "ELEVATION_LIMIT_SWITCH_POSITIVE_185" -> "Elevation Limit Switch +185°"
                "ELEVATION_LIMIT_SWITCH_NEGATIVE_0" -> "Elevation Limit Switch -0°"
                "ELEVATION_LIMIT_SWITCH_NEGATIVE_5" -> "Elevation Limit Switch -5°"
                "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275" -> "Azimuth Limit Switch -275°"
                "AZIMUTH_LIMIT_SWITCH_POSITIVE_275" -> "Azimuth Limit Switch +275°"
                "TRAIN_LIMIT_SWITCH_NEGATIVE_275" -> "Train Limit Switch -275°"
                "TRAIN_LIMIT_SWITCH_POSITIVE_275" -> "Train Limit Switch +275°"
                "ELEVATION_STOW_PIN_ACTIVE" -> "Elevation Stow Pin Active"
                "AZIMUTH_STOW_PIN_ACTIVE" -> "Azimuth Stow Pin Active"
                "TRAIN_STOW_PIN_ACTIVE" -> "Train Stow Pin Active"
                "SERVO_TRAIN_POWER_OFF" -> "Train Servo Power Off"
                "SERVO_ELEVATION_POWER_OFF" -> "Elevation Servo Power Off"
                "SERVO_AZIMUTH_POWER_OFF" -> "Azimuth Servo Power Off"
                "PROTOCOL_ELEVATION_ERROR" -> "Elevation Protocol Error"
                "PROTOCOL_AZIMUTH_ERROR" -> "Azimuth Protocol Error"
                "PROTOCOL_TRAIN_ERROR" -> "Train Protocol Error"
                "PROTOCOL_FEED_ERROR" -> "Feed Protocol Error"
                "S_BAND_LNA_LHCP_ERROR" -> "S-Band LNA LHCP Error"
                "S_BAND_LNA_RHCP_ERROR" -> "S-Band LNA RHCP Error"
                "S_BAND_RF_SWITCH_ERROR" -> "S-Band RF Switch Error"
                "X_BAND_LNA_LHCP_ERROR" -> "X-Band LNA LHCP Error"
                "X_BAND_LNA_RHCP_ERROR" -> "X-Band LNA RHCP Error"
                "FAN_ERROR" -> "Fan Error"
                "S_BAND_LNA_LHCP_POWER_ON" -> "S-Band LNA LHCP Power On"
                "S_BAND_LNA_RHCP_POWER_ON" -> "S-Band LNA RHCP Power On"
                "S_BAND_RF_SWITCH_RHCP" -> "S-Band RF Switch RHCP"
                "X_BAND_LNA_LHCP_POWER_ON" -> "X-Band LNA LHCP Power On"
                "X_BAND_LNA_RHCP_POWER_ON" -> "X-Band LNA RHCP Power On"
                "FAN_POWER_ON" -> "Fan Power On"
                else -> "Unknown Error"
            }
            else -> "알 수 없는 에러"
        }
    }
    
    private fun getResolvedMessage(key: String, lang: String): String {
        return when (lang) {
            "ko" -> when (key) {
                "ELEVATION_SERVO_ALARM" -> "Elevation 서보 알람 해제"
                "ELEVATION_SERVO_ALARM_CODE1" -> "Elevation 서보 알람 코드 1 해제"
                "ELEVATION_SERVO_ALARM_CODE2" -> "Elevation 서보 알람 코드 2 해제"
                "ELEVATION_SERVO_ALARM_CODE3" -> "Elevation 서보 알람 코드 3 해제"
                "ELEVATION_SERVO_ALARM_CODE4" -> "Elevation 서보 알람 코드 4 해제"
                "ELEVATION_SERVO_ALARM_CODE5" -> "Elevation 서보 알람 코드 5 해제"
                "AZIMUTH_SERVO_ALARM" -> "Azimuth 서보 알람 해제"
                "AZIMUTH_SERVO_ALARM_CODE1" -> "Azimuth 서보 알람 코드 1 해제"
                "AZIMUTH_SERVO_ALARM_CODE2" -> "Azimuth 서보 알람 코드 2 해제"
                "AZIMUTH_SERVO_ALARM_CODE3" -> "Azimuth 서보 알람 코드 3 해제"
                "AZIMUTH_SERVO_ALARM_CODE4" -> "Azimuth 서보 알람 코드 4 해제"
                "AZIMUTH_SERVO_ALARM_CODE5" -> "Azimuth 서보 알람 코드 5 해제"
                "TRAIN_SERVO_ALARM" -> "Train 서보 알람 해제"
                "TRAIN_SERVO_ALARM_CODE1" -> "Train 서보 알람 코드 1 해제"
                "TRAIN_SERVO_ALARM_CODE2" -> "Train 서보 알람 코드 2 해제"
                "TRAIN_SERVO_ALARM_CODE3" -> "Train 서보 알람 코드 3 해제"
                "TRAIN_SERVO_ALARM_CODE4" -> "Train 서보 알람 코드 4 해제"
                "TRAIN_SERVO_ALARM_CODE5" -> "Train 서보 알람 코드 5 해제"
                "ELEVATION_ENCODER_ERROR" -> "Elevation 인코더 에러 해결됨"
                "AZIMUTH_ENCODER_ERROR" -> "Azimuth 인코더 에러 해결됨"
                "TRAIN_ENCODER_ERROR" -> "Train 인코더 에러 해결됨"
                "POWER_SURGE_PROTECTOR_FAULT" -> "전력 서지 보호기 정상"
                "POWER_REVERSE_PHASE_FAULT" -> "전력 역상 정상"
                "EMERGENCY_STOP_ACU" -> "비상 정지 ACU 해제됨"
                "EMERGENCY_STOP_POSITIONER" -> "비상 정지 포지셔너 해제됨"
                "ELEVATION_SERVO_BRAKE_ENGAGED" -> "Elevation 서보 브레이크 해제됨"
                "AZIMUTH_SERVO_BRAKE_ENGAGED" -> "Azimuth 서보 브레이크 해제됨"
                "TRAIN_SERVO_BRAKE_ENGAGED" -> "Train 서보 브레이크 해제됨"
                "ELEVATION_SERVO_MOTOR_ON" -> "Elevation 서보 모터 꺼짐"
                "AZIMUTH_SERVO_MOTOR_ON" -> "Azimuth 서보 모터 꺼짐"
                "TRAIN_SERVO_MOTOR_ON" -> "Train 서보 모터 꺼짐"
                "ELEVATION_LIMIT_SWITCH_POSITIVE_180" -> "Elevation 한계 스위치 +180° 비활성"
                "ELEVATION_LIMIT_SWITCH_POSITIVE_185" -> "Elevation 한계 스위치 +185° 비활성"
                "ELEVATION_LIMIT_SWITCH_NEGATIVE_0" -> "Elevation 한계 스위치 -0° 비활성"
                "ELEVATION_LIMIT_SWITCH_NEGATIVE_5" -> "Elevation 한계 스위치 -5° 비활성"
                "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275" -> "Azimuth 한계 스위치 -275° 비활성"
                "AZIMUTH_LIMIT_SWITCH_POSITIVE_275" -> "Azimuth 한계 스위치 +275° 비활성"
                "TRAIN_LIMIT_SWITCH_NEGATIVE_275" -> "Train 한계 스위치 -275° 비활성"
                "TRAIN_LIMIT_SWITCH_POSITIVE_275" -> "Train 한계 스위치 +275° 비활성"
                "ELEVATION_STOW_PIN_ACTIVE" -> "Elevation 스토우 핀 비활성"
                "AZIMUTH_STOW_PIN_ACTIVE" -> "Azimuth 스토우 핀 비활성"
                "TRAIN_STOW_PIN_ACTIVE" -> "Train 스토우 핀 비활성"
                "SERVO_TRAIN_POWER_OFF" -> "Train 서보 전원 켜짐"
                "SERVO_ELEVATION_POWER_OFF" -> "Elevation 서보 전원 켜짐"
                "SERVO_AZIMUTH_POWER_OFF" -> "Azimuth 서보 전원 켜짐"
                "PROTOCOL_ELEVATION_ERROR" -> "Elevation 프로토콜 정상"
                "PROTOCOL_AZIMUTH_ERROR" -> "Azimuth 프로토콜 정상"
                "PROTOCOL_TRAIN_ERROR" -> "Train 프로토콜 정상"
                "PROTOCOL_FEED_ERROR" -> "Feed 프로토콜 정상"
                "S_BAND_LNA_LHCP_ERROR" -> "S-Band LNA LHCP 정상"
                "S_BAND_LNA_RHCP_ERROR" -> "S-Band LNA RHCP 정상"
                "S_BAND_RF_SWITCH_ERROR" -> "S-Band RF 스위치 정상"
                "X_BAND_LNA_LHCP_ERROR" -> "X-Band LNA LHCP 정상"
                "X_BAND_LNA_RHCP_ERROR" -> "X-Band LNA RHCP 정상"
                "FAN_ERROR" -> "팬 정상"
                "S_BAND_LNA_LHCP_POWER_ON" -> "S-Band LNA LHCP 전원 꺼짐"
                "S_BAND_LNA_RHCP_POWER_ON" -> "S-Band LNA RHCP 전원 꺼짐"
                "S_BAND_RF_SWITCH_RHCP" -> "S-Band RF 스위치 LHCP"
                "X_BAND_LNA_LHCP_POWER_ON" -> "X-Band LNA LHCP 전원 꺼짐"
                "X_BAND_LNA_RHCP_POWER_ON" -> "X-Band LNA RHCP 전원 꺼짐"
                "FAN_POWER_ON" -> "팬 전원 꺼짐"
                else -> "에러가 해결되었습니다"
            }
            "en" -> when (key) {
                "ELEVATION_SERVO_ALARM" -> "Elevation Servo Alarm Resolved"
                "ELEVATION_SERVO_ALARM_CODE1" -> "Elevation Servo Alarm Code 1 Resolved"
                "ELEVATION_SERVO_ALARM_CODE2" -> "Elevation Servo Alarm Code 2 Resolved"
                "ELEVATION_SERVO_ALARM_CODE3" -> "Elevation Servo Alarm Code 3 Resolved"
                "ELEVATION_SERVO_ALARM_CODE4" -> "Elevation Servo Alarm Code 4 Resolved"
                "ELEVATION_SERVO_ALARM_CODE5" -> "Elevation Servo Alarm Code 5 Resolved"
                "AZIMUTH_SERVO_ALARM" -> "Azimuth Servo Alarm Resolved"
                "AZIMUTH_SERVO_ALARM_CODE1" -> "Azimuth Servo Alarm Code 1 Resolved"
                "AZIMUTH_SERVO_ALARM_CODE2" -> "Azimuth Servo Alarm Code 2 Resolved"
                "AZIMUTH_SERVO_ALARM_CODE3" -> "Azimuth Servo Alarm Code 3 Resolved"
                "AZIMUTH_SERVO_ALARM_CODE4" -> "Azimuth Servo Alarm Code 4 Resolved"
                "AZIMUTH_SERVO_ALARM_CODE5" -> "Azimuth Servo Alarm Code 5 Resolved"
                "TRAIN_SERVO_ALARM" -> "Train Servo Alarm Resolved"
                "TRAIN_SERVO_ALARM_CODE1" -> "Train Servo Alarm Code 1 Resolved"
                "TRAIN_SERVO_ALARM_CODE2" -> "Train Servo Alarm Code 2 Resolved"
                "TRAIN_SERVO_ALARM_CODE3" -> "Train Servo Alarm Code 3 Resolved"
                "TRAIN_SERVO_ALARM_CODE4" -> "Train Servo Alarm Code 4 Resolved"
                "TRAIN_SERVO_ALARM_CODE5" -> "Train Servo Alarm Code 5 Resolved"
                "ELEVATION_ENCODER_ERROR" -> "Elevation Encoder Error Resolved"
                "AZIMUTH_ENCODER_ERROR" -> "Azimuth Encoder Error Resolved"
                "TRAIN_ENCODER_ERROR" -> "Train Encoder Error Resolved"
                "POWER_SURGE_PROTECTOR_FAULT" -> "Power Surge Protector Normal"
                "POWER_REVERSE_PHASE_FAULT" -> "Power Reverse Phase Normal"
                "EMERGENCY_STOP_ACU" -> "Emergency Stop ACU Released"
                "EMERGENCY_STOP_POSITIONER" -> "Emergency Stop Positioner Released"
                "ELEVATION_SERVO_BRAKE_ENGAGED" -> "Elevation Servo Brake Disengaged"
                "AZIMUTH_SERVO_BRAKE_ENGAGED" -> "Azimuth Servo Brake Disengaged"
                "TRAIN_SERVO_BRAKE_ENGAGED" -> "Train Servo Brake Disengaged"
                "ELEVATION_SERVO_MOTOR_ON" -> "Elevation Servo Motor Off"
                "AZIMUTH_SERVO_MOTOR_ON" -> "Azimuth Servo Motor Off"
                "TRAIN_SERVO_MOTOR_ON" -> "Train Servo Motor Off"
                "ELEVATION_LIMIT_SWITCH_POSITIVE_180" -> "Elevation Limit Switch +180° Inactive"
                "ELEVATION_LIMIT_SWITCH_POSITIVE_185" -> "Elevation Limit Switch +185° Inactive"
                "ELEVATION_LIMIT_SWITCH_NEGATIVE_0" -> "Elevation Limit Switch -0° Inactive"
                "ELEVATION_LIMIT_SWITCH_NEGATIVE_5" -> "Elevation Limit Switch -5° Inactive"
                "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275" -> "Azimuth Limit Switch -275° Inactive"
                "AZIMUTH_LIMIT_SWITCH_POSITIVE_275" -> "Azimuth Limit Switch +275° Inactive"
                "TRAIN_LIMIT_SWITCH_NEGATIVE_275" -> "Train Limit Switch -275° Inactive"
                "TRAIN_LIMIT_SWITCH_POSITIVE_275" -> "Train Limit Switch +275° Inactive"
                "ELEVATION_STOW_PIN_ACTIVE" -> "Elevation Stow Pin Inactive"
                "AZIMUTH_STOW_PIN_ACTIVE" -> "Azimuth Stow Pin Inactive"
                "TRAIN_STOW_PIN_ACTIVE" -> "Train Stow Pin Inactive"
                "SERVO_TRAIN_POWER_OFF" -> "Train Servo Power On"
                "SERVO_ELEVATION_POWER_OFF" -> "Elevation Servo Power On"
                "SERVO_AZIMUTH_POWER_OFF" -> "Azimuth Servo Power On"
                "PROTOCOL_ELEVATION_ERROR" -> "Elevation Protocol Normal"
                "PROTOCOL_AZIMUTH_ERROR" -> "Azimuth Protocol Normal"
                "PROTOCOL_TRAIN_ERROR" -> "Train Protocol Normal"
                "PROTOCOL_FEED_ERROR" -> "Feed Protocol Normal"
                "S_BAND_LNA_LHCP_ERROR" -> "S-Band LNA LHCP Normal"
                "S_BAND_LNA_RHCP_ERROR" -> "S-Band LNA RHCP Normal"
                "S_BAND_RF_SWITCH_ERROR" -> "S-Band RF Switch Normal"
                "X_BAND_LNA_LHCP_ERROR" -> "X-Band LNA LHCP Normal"
                "X_BAND_LNA_RHCP_ERROR" -> "X-Band LNA RHCP Normal"
                "FAN_ERROR" -> "Fan Normal"
                "S_BAND_LNA_LHCP_POWER_ON" -> "S-Band LNA LHCP Power Off"
                "S_BAND_LNA_RHCP_POWER_ON" -> "S-Band LNA RHCP Power Off"
                "S_BAND_RF_SWITCH_RHCP" -> "S-Band RF Switch LHCP"
                "X_BAND_LNA_LHCP_POWER_ON" -> "X-Band LNA LHCP Power Off"
                "X_BAND_LNA_RHCP_POWER_ON" -> "X-Band LNA RHCP Power Off"
                "FAN_POWER_ON" -> "Fan Power Off"
                else -> "Error has been resolved"
            }
            else -> "에러가 해결되었습니다"
        }
    }
}

/**
 * 하드웨어 에러 로그 데이터 클래스
 */
data class HardwareErrorLog(
    val id: String,
    val timestamp: String,
    val category: String,
    val severity: String,
    val message: Map<String, String>,
    val component: String,
    val isResolved: Boolean,
    val resolvedAt: String?,
    val resolvedMessage: Map<String, String>?
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