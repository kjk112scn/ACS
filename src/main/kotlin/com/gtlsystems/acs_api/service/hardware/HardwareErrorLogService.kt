package com.gtlsystems.acs_api.service.hardware

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 하드웨어 에러 로그 관리 서비스 (메모리 기반)
 */
@Service
class HardwareErrorLogService {
    
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(HardwareErrorLogService::class.java)
    }
    
    // 이전 비트 상태 저장
    private val previousBitStates = ConcurrentHashMap<String, String>()
    
    // 에러 로그 메모리 저장소
    private val errorLogs = ConcurrentLinkedQueue<HardwareErrorLog>()
    
    /**
     * 안테나 데이터를 처리하여 에러를 감지하고 로그를 생성합니다
     */
    fun processAntennaData(antennaData: Map<String, Any>) {
        try {
            logger.info("🔍 하드웨어 로그 처리 시작 - 데이터 크기: {}", antennaData.size)
            
            // ✅ 모든 비트 타입 처리 (AllStatusContent.vue 기반)
            val bitTypes = listOf(
                "mainBoardProtocolStatusBits",
                "mainBoardStatusBits", 
                "mainBoardMCOnOffBits",
                "azimuthBoardServoStatusBits",
                "azimuthBoardStatusBits",
                "elevationBoardServoStatusBits", // ✅ Elevation Servo Alarm 포함
                "elevationBoardStatusBits",
                "trainBoardServoStatusBits",
                "trainBoardStatusBits",
                "feedSBoardStatusBits",
                "feedXBoardStatusBits"
            )
            
            bitTypes.forEach { bitType ->
                val currentBits = antennaData[bitType] as? String
                logger.info("🔍 {} 현재 비트: {}", bitType, currentBits)
                
                if (currentBits != null) {
                    val previousBits = previousBitStates[bitType]
                    
                    if (previousBits == null) {
                        // ✅ 첫 번째 수신 - 모든 비트를 현재 상태로 기록
                        logger.info("🆕 {} 첫 수신 - 현재 상태 기록", bitType)
                        previousBitStates[bitType] = currentBits
                        
                        // ✅ 첫 수신 시에도 현재 상태를 에러로 기록 (초기 상태 감지)
                        val initialErrors = analyzeBitChanges(currentBits, "00000000", bitType)
                        initialErrors.forEach { error ->
                            addErrorLog(error)
                            logger.info("📝 초기 에러 로그 추가: {}", error.message)
                        }
                    } else if (previousBits != currentBits) {
                        // ✅ 변화 감지
                        logger.info("✅ {} 비트 변화 감지: {} -> {}", bitType, previousBits, currentBits)
                        val errors = analyzeBitChanges(currentBits, previousBits, bitType)
                        errors.forEach { error ->
                            addErrorLog(error)
                            logger.info("📝 에러 로그 추가: {}", error.message)
                        }
                        previousBitStates[bitType] = currentBits
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("하드웨어 에러 로그 처리 중 오류 발생", e)
        }
    }
    
    /**
     * 비트 변화를 분석하여 에러를 감지합니다
     */
    private fun analyzeBitChanges(currentBits: String, previousBits: String, bitType: String): List<HardwareErrorLog> {
        val errors = mutableListOf<HardwareErrorLog>()
        
        // ✅ 디버깅 로그 추가
        logger.info("�� 비트 분석 시작: {} -> {} -> {}", bitType, previousBits, currentBits)
        
        val currentBitArray = currentBits.padStart(8, '0').split("").filter { it.isNotEmpty() }.reversed()
        val previousBitArray = previousBits.padStart(8, '0').split("").filter { it.isNotEmpty() }.reversed()
        
        logger.info("�� 비트 배열: 현재={}, 이전={}", currentBitArray, previousBitArray)
        
        val errorMappings = getErrorMappings(bitType)
        
        errorMappings.forEach { (bitPosition, errorConfig) ->
            val currentBit = currentBitArray.getOrNull(bitPosition) == "1"
            val previousBit = previousBitArray.getOrNull(bitPosition) == "1"
            
            logger.info("�� 비트 {}: 현재={}, 이전={}, 변화={}", bitPosition, currentBit, previousBit, currentBit != previousBit)
            
            // ✅ 비트 변화 감지
            if (currentBit != previousBit) {
                val error = HardwareErrorLog(
                    id = UUID.randomUUID().toString(),
                    timestamp = LocalDateTime.now().toString(),
                    category = errorConfig.category,
                    severity = if (currentBit) errorConfig.severity else "INFO",
                    message = if (currentBit) errorConfig.errorMessage else errorConfig.resolvedMessage,
                    component = errorConfig.component,
                    isResolved = !currentBit,
                    resolvedAt = if (!currentBit) LocalDateTime.now().toString() else null,
                    resolvedMessage = if (!currentBit) errorConfig.resolvedMessage else null
                )
                errors.add(error)
                logger.info("📝 에러 생성: {} - {}", errorConfig.component, error.message)
            }
        }
        
        logger.info("🔍 총 에러 개수: {}", errors.size)
        return errors
    }
    
    /**
     * 비트 타입별 에러 매핑 정의 (AllStatusContent.vue 기반)
     */
    private fun getErrorMappings(bitType: String): Map<Int, ErrorConfig> {
        return when (bitType) {
            // ✅ Main Board Protocol Status
            "mainBoardProtocolStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_ELEVATION_ERROR", "Elevation Protocol"),
                    1 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_AZIMUTH_ERROR", "Azimuth Protocol"),
                    2 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_TRAIN_ERROR", "Train Protocol"),
                    3 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_FEED_ERROR", "Feed Protocol")
                )
            }
            
            // ✅ Main Board Status (Power & Emergency)
            "mainBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("POWER", "ERROR", "POWER_SURGE_PROTECTOR_FAULT", "Power Surge Protector"),
                    1 to ErrorConfig("POWER", "ERROR", "POWER_REVERSE_PHASE_FAULT", "Power Reverse Phase"),
                    2 to ErrorConfig("EMERGENCY", "CRITICAL", "EMERGENCY_STOP_ACU", "Emergency Stop ACU"),
                    3 to ErrorConfig("EMERGENCY", "CRITICAL", "EMERGENCY_STOP_POSITIONER", "Emergency Stop Positioner")
                )
            }
            
            // ✅ Main Board MC On/Off Status
            "mainBoardMCOnOffBits" -> {
                mapOf(
                    0 to ErrorConfig("SERVO_POWER", "WARNING", "SERVO_TRAIN_POWER_OFF", "Train Servo Power"),
                    1 to ErrorConfig("SERVO_POWER", "WARNING", "SERVO_ELEVATION_POWER_OFF", "Elevation Servo Power"),
                    2 to ErrorConfig("SERVO_POWER", "WARNING", "SERVO_AZIMUTH_POWER_OFF", "Azimuth Servo Power")
                )
            }
            
            // ✅ Azimuth Board Servo Status
            "azimuthBoardServoStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE1", "Azimuth Servo Alarm Code 1"),
                    1 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE2", "Azimuth Servo Alarm Code 2"),
                    2 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE3", "Azimuth Servo Alarm Code 3"),
                    3 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE4", "Azimuth Servo Alarm Code 4"),
                    4 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE5", "Azimuth Servo Alarm Code 5"),
                    5 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM", "Azimuth Servo Alarm"),
                    6 to ErrorConfig("SERVO_POWER", "WARNING", "AZIMUTH_SERVO_BRAKE_ENGAGED", "Azimuth Servo Brake"),
                    7 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_MOTOR_ON", "Azimuth Servo Motor")
                )
            }
            
            // ✅ Azimuth Board Status (Positioner)
            "azimuthBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("POSITIONER", "WARNING", "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275", "Azimuth Limit Switch -275°"),
                    1 to ErrorConfig("POSITIONER", "WARNING", "AZIMUTH_LIMIT_SWITCH_POSITIVE_275", "Azimuth Limit Switch +275°"),
                    4 to ErrorConfig("STOW", "INFO", "AZIMUTH_STOW_PIN_ACTIVE", "Azimuth Stow Pin"),
                    7 to ErrorConfig("POSITIONER", "ERROR", "AZIMUTH_ENCODER_ERROR", "Azimuth Encoder")
                )
            }
            
            // ✅ Elevation Board Servo Status
            "elevationBoardServoStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE1", "Elevation Servo Alarm Code 1"),
                    1 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE2", "Elevation Servo Alarm Code 2"),
                    2 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE3", "Elevation Servo Alarm Code 3"),
                    3 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE4", "Elevation Servo Alarm Code 4"),
                    4 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM_CODE5", "Elevation Servo Alarm Code 5"),
                    5 to ErrorConfig("SERVO_POWER", "ERROR", "ELEVATION_SERVO_ALARM", "Elevation Servo Alarm"), // ✅ 이것이 5번째 비트
                    6 to ErrorConfig("SERVO_POWER", "WARNING", "ELEVATION_SERVO_BRAKE_ENGAGED", "Elevation Servo Brake"),
                    7 to ErrorConfig("SERVO_POWER", "INFO", "ELEVATION_SERVO_MOTOR_ON", "Elevation Servo Motor")
                )
            }
            
            // ✅ Elevation Board Status (Positioner)
            "elevationBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("POSITIONER", "WARNING", "ELEVATION_LIMIT_SWITCH_POSITIVE_180", "Elevation Limit Switch +180°"),
                    1 to ErrorConfig("POSITIONER", "WARNING", "ELEVATION_LIMIT_SWITCH_POSITIVE_185", "Elevation Limit Switch +185°"),
                    2 to ErrorConfig("POSITIONER", "WARNING", "ELEVATION_LIMIT_SWITCH_NEGATIVE_0", "Elevation Limit Switch -0°"),
                    3 to ErrorConfig("POSITIONER", "WARNING", "ELEVATION_LIMIT_SWITCH_NEGATIVE_5", "Elevation Limit Switch -5°"),
                    4 to ErrorConfig("STOW", "INFO", "ELEVATION_STOW_PIN_ACTIVE", "Elevation Stow Pin"),
                    7 to ErrorConfig("POSITIONER", "ERROR", "ELEVATION_ENCODER_ERROR", "Elevation Encoder")
                )
            }
            
            // ✅ Train Board Servo Status
            "trainBoardServoStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE1", "Train Servo Alarm Code 1"),
                    1 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE2", "Train Servo Alarm Code 2"),
                    2 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE3", "Train Servo Alarm Code 3"),
                    3 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE4", "Train Servo Alarm Code 4"),
                    4 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM_CODE5", "Train Servo Alarm Code 5"),
                    5 to ErrorConfig("SERVO_POWER", "ERROR", "TRAIN_SERVO_ALARM", "Train Servo Alarm"),
                    6 to ErrorConfig("SERVO_POWER", "WARNING", "TRAIN_SERVO_BRAKE_ENGAGED", "Train Servo Brake"),
                    7 to ErrorConfig("SERVO_POWER", "INFO", "TRAIN_SERVO_MOTOR_ON", "Train Servo Motor")
                )
            }
            
            // ✅ Train Board Status (Positioner) - 사진의 Tilt +275° 상태
            "trainBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("POSITIONER", "WARNING", "TRAIN_LIMIT_SWITCH_NEGATIVE_275", "Train Limit Switch -275°"),
                    1 to ErrorConfig("POSITIONER", "WARNING", "TRAIN_LIMIT_SWITCH_POSITIVE_275", "Train Limit Switch +275°"), // ✅ 이것이 사진의 Tilt +275° 상태
                    4 to ErrorConfig("STOW", "INFO", "TRAIN_STOW_PIN_ACTIVE", "Train Stow Pin"),
                    7 to ErrorConfig("POSITIONER", "ERROR", "TRAIN_ENCODER_ERROR", "Train Encoder")
                )
            }
            
            // ✅ Feed S-Band Board Status
            "feedSBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("FEED", "INFO", "S_BAND_LNA_LHCP_POWER_ON", "S-Band LNA LHCP Power"),
                    1 to ErrorConfig("FEED", "ERROR", "S_BAND_LNA_LHCP_ERROR", "S-Band LNA LHCP Error"),
                    2 to ErrorConfig("FEED", "INFO", "S_BAND_LNA_RHCP_POWER_ON", "S-Band LNA RHCP Power"),
                    3 to ErrorConfig("FEED", "ERROR", "S_BAND_LNA_RHCP_ERROR", "S-Band LNA RHCP Error"),
                    4 to ErrorConfig("FEED", "INFO", "S_BAND_RF_SWITCH_RHCP", "S-Band RF Switch Mode"),
                    5 to ErrorConfig("FEED", "ERROR", "S_BAND_RF_SWITCH_ERROR", "S-Band RF Switch Error")
                )
            }
            
            // ✅ Feed X-Band Board Status
            "feedXBoardStatusBits" -> {
                mapOf(
                    0 to ErrorConfig("FEED", "INFO", "X_BAND_LNA_LHCP_POWER_ON", "X-Band LNA LHCP Power"),
                    1 to ErrorConfig("FEED", "ERROR", "X_BAND_LNA_LHCP_ERROR", "X-Band LNA LHCP Error"),
                    2 to ErrorConfig("FEED", "INFO", "X_BAND_LNA_RHCP_POWER_ON", "X-Band LNA RHCP Power"),
                    3 to ErrorConfig("FEED", "ERROR", "X_BAND_LNA_RHCP_ERROR", "X-Band LNA RHCP Error"),
                    6 to ErrorConfig("FEED", "INFO", "FAN_POWER_ON", "Fan Power"),
                    7 to ErrorConfig("FEED", "ERROR", "FAN_ERROR", "Fan Error")
                )
            }
            
            else -> emptyMap<Int, ErrorConfig>()
        }
    }
    
    /**
     * 에러 로그를 메모리에 추가합니다
     */
    private fun addErrorLog(error: HardwareErrorLog) {
        errorLogs.offer(error)
        
        // 메모리 사용량 제한 (최대 1000개)
        if (errorLogs.size > 1000) {
            errorLogs.poll()
        }
    }
    
    /**
     * 모든 에러 로그를 가져옵니다
     */
    fun getAllErrorLogs(): List<HardwareErrorLog> {
        return errorLogs.toList()
    }
    
    /**
     * 활성 에러 로그를 가져옵니다
     */
    fun getActiveErrorLogs(): List<HardwareErrorLog> {
        return errorLogs.filter { !it.isResolved }
    }

    fun createTestErrorLog() {
        logger.info("�� createTestErrorLog() 메서드 호출됨")
        
        val testError = HardwareErrorLog(
            id = UUID.randomUUID().toString(),
            timestamp = LocalDateTime.now().toString(),
            category = "TEST",
            severity = "INFO",
            message = mapOf("ko" to "테스트 에러", "en" to "Test Error"),
            component = "Test Component",
            isResolved = false,
            resolvedAt = null,
            resolvedMessage = null
        )
        
        logger.info("🔍 테스트 에러 객체 생성됨: {}", testError.id)
        
        addErrorLog(testError)
        
        logger.info("�� addErrorLog() 호출 완료")
        logger.info("🔍 현재 에러 로그 개수: {}", errorLogs.size)
        logger.info("✅ 테스트 에러 로그 생성됨")
    }
}

/**
 * 에러 설정 데이터 클래스
 */
data class ErrorConfig(
    val category: String,
    val severity: String,
    val messageKey: String,
    val component: String
) {
    val errorMessage: Map<String, String>
        get() = mapOf(
            "ko" to getErrorMessage(messageKey, "ko"),
            "en" to getErrorMessage(messageKey, "en")
        )
    
    val resolvedMessage: Map<String, String>
        get() = when (severity) {
            "ERROR" -> mapOf(
                "ko" to getErrorMessage(messageKey.replace("_OFF", "_ON").replace("_ACTIVE", "_RESOLVED").replace("_ERROR", "_NORMAL"), "ko"),
                "en" to getErrorMessage(messageKey.replace("_OFF", "_ON").replace("_ACTIVE", "_RESOLVED").replace("_ERROR", "_NORMAL"), "en")
            )
            "CRITICAL" -> mapOf(
                "ko" to getErrorMessage(messageKey.replace("_ACTIVE", "_RESOLVED"), "ko"),
                "en" to getErrorMessage(messageKey.replace("_ACTIVE", "_RESOLVED"), "en")
            )
            "WARNING" -> mapOf(
                "ko" to getErrorMessage(messageKey.replace("_ACTIVE", "_INACTIVE").replace("_ON", "_OFF"), "ko"),
                "en" to getErrorMessage(messageKey.replace("_ACTIVE", "_INACTIVE").replace("_ON", "_OFF"), "en")
            )
            "INFO" -> mapOf(
                "ko" to getErrorMessage(messageKey.replace("_ON", "_OFF").replace("_ACTIVE", "_INACTIVE"), "ko"),
                "en" to getErrorMessage(messageKey.replace("_ON", "_OFF").replace("_ACTIVE", "_INACTIVE"), "en")
            )
            else -> mapOf("ko" to "상태 변경", "en" to "Status Changed")
        }
}

/**
 * 다국어 에러 메시지 생성
 */
private fun getErrorMessage(messageKey: String, language: String): String {
    val messages = when (language) {
        "ko" -> mapOf(
            // Protocol Errors
            "PROTOCOL_ELEVATION_ERROR" to "Elevation 프로토콜 오류",
            "PROTOCOL_AZIMUTH_ERROR" to "Azimuth 프로토콜 오류", 
            "PROTOCOL_TRAIN_ERROR" to "Train 프로토콜 오류",
            "PROTOCOL_FEED_ERROR" to "Feed 프로토콜 오류",
            "PROTOCOL_ELEVATION_RESOLVED" to "Elevation 프로토콜 정상화",
            "PROTOCOL_AZIMUTH_RESOLVED" to "Azimuth 프로토콜 정상화",
            "PROTOCOL_TRAIN_RESOLVED" to "Train 프로토콜 정상화",
            "PROTOCOL_FEED_RESOLVED" to "Feed 프로토콜 정상화",
            
            // Power Errors
            "POWER_SURGE_PROTECTOR_FAULT" to "서지 보호기 고장",
            "POWER_REVERSE_PHASE_FAULT" to "역상 감지기 고장",
            "POWER_SURGE_PROTECTOR_NORMAL" to "서지 보호기 정상",
            "POWER_REVERSE_PHASE_NORMAL" to "역상 감지기 정상",
            
            // Emergency Errors
            "EMERGENCY_STOP_ACU" to "ACU 비상 정지 활성",
            "EMERGENCY_STOP_POSITIONER" to "포지셔너 비상 정지 활성",
            "EMERGENCY_STOP_ACU_RESOLVED" to "ACU 비상 정지 해제",
            "EMERGENCY_STOP_POSITIONER_RESOLVED" to "포지셔너 비상 정지 해제",
            
            // Servo Power
            "SERVO_TRAIN_POWER_OFF" to "Train 서보 전원 꺼짐",
            "SERVO_ELEVATION_POWER_OFF" to "Elevation 서보 전원 꺼짐",
            "SERVO_AZIMUTH_POWER_OFF" to "Azimuth 서보 전원 꺼짐",
            "SERVO_TRAIN_POWER_ON" to "Train 서보 전원 켜짐",
            "SERVO_ELEVATION_POWER_ON" to "Elevation 서보 전원 켜짐",
            "SERVO_AZIMUTH_POWER_ON" to "Azimuth 서보 전원 켜짐",
            
            // Servo Alarms
            "AZIMUTH_SERVO_ALARM" to "Azimuth 서보 알람",
            "ELEVATION_SERVO_ALARM" to "Elevation 서보 알람",
            "TRAIN_SERVO_ALARM" to "Train 서보 알람",
            "AZIMUTH_SERVO_ALARM_RESOLVED" to "Azimuth 서보 알람 해제",
            "ELEVATION_SERVO_ALARM_RESOLVED" to "Elevation 서보 알람 해제",
            "TRAIN_SERVO_ALARM_RESOLVED" to "Train 서보 알람 해제",
            
            // Servo Alarm Codes
            "AZIMUTH_SERVO_ALARM_CODE1" to "Azimuth 서보 알람 코드 1",
            "AZIMUTH_SERVO_ALARM_CODE2" to "Azimuth 서보 알람 코드 2",
            "AZIMUTH_SERVO_ALARM_CODE3" to "Azimuth 서보 알람 코드 3",
            "AZIMUTH_SERVO_ALARM_CODE4" to "Azimuth 서보 알람 코드 4",
            "AZIMUTH_SERVO_ALARM_CODE5" to "Azimuth 서보 알람 코드 5",
            "ELEVATION_SERVO_ALARM_CODE1" to "Elevation 서보 알람 코드 1",
            "ELEVATION_SERVO_ALARM_CODE2" to "Elevation 서보 알람 코드 2",
            "ELEVATION_SERVO_ALARM_CODE3" to "Elevation 서보 알람 코드 3",
            "ELEVATION_SERVO_ALARM_CODE4" to "Elevation 서보 알람 코드 4",
            "ELEVATION_SERVO_ALARM_CODE5" to "Elevation 서보 알람 코드 5",
            "TRAIN_SERVO_ALARM_CODE1" to "Train 서보 알람 코드 1",
            "TRAIN_SERVO_ALARM_CODE2" to "Train 서보 알람 코드 2",
            "TRAIN_SERVO_ALARM_CODE3" to "Train 서보 알람 코드 3",
            "TRAIN_SERVO_ALARM_CODE4" to "Train 서보 알람 코드 4",
            "TRAIN_SERVO_ALARM_CODE5" to "Train 서보 알람 코드 5",
            
            // Servo Status
            "AZIMUTH_SERVO_BRAKE_ENGAGED" to "Azimuth 서보 브레이크 작동",
            "AZIMUTH_SERVO_MOTOR_ON" to "Azimuth 서보 모터 켜짐",
            "ELEVATION_SERVO_BRAKE_ENGAGED" to "Elevation 서보 브레이크 작동",
            "ELEVATION_SERVO_MOTOR_ON" to "Elevation 서보 모터 켜짐",
            "TRAIN_SERVO_BRAKE_ENGAGED" to "Train 서보 브레이크 작동",
            "TRAIN_SERVO_MOTOR_ON" to "Train 서보 모터 켜짐",
            "AZIMUTH_SERVO_BRAKE_DISENGAGED" to "Azimuth 서보 브레이크 해제",
            "AZIMUTH_SERVO_MOTOR_OFF" to "Azimuth 서보 모터 꺼짐",
            "ELEVATION_SERVO_BRAKE_DISENGAGED" to "Elevation 서보 브레이크 해제",
            "ELEVATION_SERVO_MOTOR_OFF" to "Elevation 서보 모터 꺼짐",
            "TRAIN_SERVO_BRAKE_DISENGAGED" to "Train 서보 브레이크 해제",
            "TRAIN_SERVO_MOTOR_OFF" to "Train 서보 모터 꺼짐",
            
            // Positioner Status
            "AZIMUTH_LIMIT_SWITCH_POSITIVE_275" to "Azimuth +275° 리미트 스위치 활성",
            "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275" to "Azimuth -275° 리미트 스위치 활성",
            "ELEVATION_LIMIT_SWITCH_POSITIVE_180" to "Elevation +180° 리미트 스위치 활성",
            "ELEVATION_LIMIT_SWITCH_POSITIVE_185" to "Elevation +185° 리미트 스위치 활성",
            "ELEVATION_LIMIT_SWITCH_NEGATIVE_0" to "Elevation -0° 리미트 스위치 활성",
            "ELEVATION_LIMIT_SWITCH_NEGATIVE_5" to "Elevation -5° 리미트 스위치 활성",
            "TRAIN_LIMIT_SWITCH_POSITIVE_275" to "Train +275° 리미트 스위치 활성", // ✅ 사진의 Tilt +275° 상태
            "TRAIN_LIMIT_SWITCH_NEGATIVE_275" to "Train -275° 리미트 스위치 활성",
            "AZIMUTH_LIMIT_SWITCH_POSITIVE_275_INACTIVE" to "Azimuth +275° 리미트 스위치 비활성",
            "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275_INACTIVE" to "Azimuth -275° 리미트 스위치 비활성",
            "ELEVATION_LIMIT_SWITCH_POSITIVE_180_INACTIVE" to "Elevation +180° 리미트 스위치 비활성",
            "ELEVATION_LIMIT_SWITCH_POSITIVE_185_INACTIVE" to "Elevation +185° 리미트 스위치 비활성",
            "ELEVATION_LIMIT_SWITCH_NEGATIVE_0_INACTIVE" to "Elevation -0° 리미트 스위치 비활성",
            "ELEVATION_LIMIT_SWITCH_NEGATIVE_5_INACTIVE" to "Elevation -5° 리미트 스위치 비활성",
            "TRAIN_LIMIT_SWITCH_POSITIVE_275_INACTIVE" to "Train +275° 리미트 스위치 비활성",
            "TRAIN_LIMIT_SWITCH_NEGATIVE_275_INACTIVE" to "Train -275° 리미트 스위치 비활성",
            
            // Stow Status
            "AZIMUTH_STOW_PIN_ACTIVE" to "Azimuth Stow Pin 활성",
            "ELEVATION_STOW_PIN_ACTIVE" to "Elevation Stow Pin 활성",
            "TRAIN_STOW_PIN_ACTIVE" to "Train Stow Pin 활성",
            "AZIMUTH_STOW_PIN_INACTIVE" to "Azimuth Stow Pin 비활성",
            "ELEVATION_STOW_PIN_INACTIVE" to "Elevation Stow Pin 비활성",
            "TRAIN_STOW_PIN_INACTIVE" to "Train Stow Pin 비활성",
            
            // Encoder Errors
            "AZIMUTH_ENCODER_ERROR" to "Azimuth 인코더 오류",
            "ELEVATION_ENCODER_ERROR" to "Elevation 인코더 오류",
            "TRAIN_ENCODER_ERROR" to "Train 인코더 오류",
            "AZIMUTH_ENCODER_NORMAL" to "Azimuth 인코더 정상",
            "ELEVATION_ENCODER_NORMAL" to "Elevation 인코더 정상",
            "TRAIN_ENCODER_NORMAL" to "Train 인코더 정상",
            
            // Feed Errors
            "S_BAND_LNA_LHCP_ERROR" to "S-Band LNA LHCP 오류",
            "S_BAND_LNA_RHCP_ERROR" to "S-Band LNA RHCP 오류",
            "S_BAND_RF_SWITCH_ERROR" to "S-Band RF Switch 오류",
            "X_BAND_LNA_LHCP_ERROR" to "X-Band LNA LHCP 오류",
            "X_BAND_LNA_RHCP_ERROR" to "X-Band LNA RHCP 오류",
            "FAN_ERROR" to "Fan 오류",
            "S_BAND_LNA_LHCP_NORMAL" to "S-Band LNA LHCP 정상",
            "S_BAND_LNA_RHCP_NORMAL" to "S-Band LNA RHCP 정상",
            "S_BAND_RF_SWITCH_NORMAL" to "S-Band RF Switch 정상",
            "X_BAND_LNA_LHCP_NORMAL" to "X-Band LNA LHCP 정상",
            "X_BAND_LNA_RHCP_NORMAL" to "X-Band LNA RHCP 정상",
            "FAN_NORMAL" to "Fan 정상",
            
            // Feed Status
            "S_BAND_LNA_LHCP_POWER_ON" to "S-Band LNA LHCP 전원 켜짐",
            "S_BAND_LNA_RHCP_POWER_ON" to "S-Band LNA RHCP 전원 켜짐",
            "S_BAND_RF_SWITCH_RHCP" to "S-Band RF Switch RHCP 모드",
            "X_BAND_LNA_LHCP_POWER_ON" to "X-Band LNA LHCP 전원 켜짐",
            "X_BAND_LNA_RHCP_POWER_ON" to "X-Band LNA RHCP 전원 켜짐",
            "FAN_POWER_ON" to "Fan 전원 켜짐",
            "S_BAND_LNA_LHCP_POWER_OFF" to "S-Band LNA LHCP 전원 꺼짐",
            "S_BAND_LNA_RHCP_POWER_OFF" to "S-Band LNA RHCP 전원 꺼짐",
            "S_BAND_RF_SWITCH_LHCP" to "S-Band RF Switch LHCP 모드",
            "X_BAND_LNA_LHCP_POWER_OFF" to "X-Band LNA LHCP 전원 꺼짐",
            "X_BAND_LNA_RHCP_POWER_OFF" to "X-Band LNA RHCP 전원 꺼짐",
            "FAN_POWER_OFF" to "Fan 전원 꺼짐"
        )
        "en" -> mapOf(
            // Protocol Errors
            "PROTOCOL_ELEVATION_ERROR" to "Elevation Protocol Error",
            "PROTOCOL_AZIMUTH_ERROR" to "Azimuth Protocol Error",
            "PROTOCOL_TRAIN_ERROR" to "Train Protocol Error", 
            "PROTOCOL_FEED_ERROR" to "Feed Protocol Error",
            "PROTOCOL_ELEVATION_RESOLVED" to "Elevation Protocol Resolved",
            "PROTOCOL_AZIMUTH_RESOLVED" to "Azimuth Protocol Resolved",
            "PROTOCOL_TRAIN_RESOLVED" to "Train Protocol Resolved",
            "PROTOCOL_FEED_RESOLVED" to "Feed Protocol Resolved",
            
            // Power Errors
            "POWER_SURGE_PROTECTOR_FAULT" to "Surge Protector Fault",
            "POWER_REVERSE_PHASE_FAULT" to "Reverse Phase Fault",
            "POWER_SURGE_PROTECTOR_NORMAL" to "Surge Protector Normal",
            "POWER_REVERSE_PHASE_NORMAL" to "Reverse Phase Normal",
            
            // Emergency Errors
            "EMERGENCY_STOP_ACU" to "ACU Emergency Stop Active",
            "EMERGENCY_STOP_POSITIONER" to "Positioner Emergency Stop Active",
            "EMERGENCY_STOP_ACU_RESOLVED" to "ACU Emergency Stop Resolved",
            "EMERGENCY_STOP_POSITIONER_RESOLVED" to "Positioner Emergency Stop Resolved",
            
            // Servo Power
            "SERVO_TRAIN_POWER_OFF" to "Train Servo Power Off",
            "SERVO_ELEVATION_POWER_OFF" to "Elevation Servo Power Off",
            "SERVO_AZIMUTH_POWER_OFF" to "Azimuth Servo Power Off",
            "SERVO_TRAIN_POWER_ON" to "Train Servo Power On",
            "SERVO_ELEVATION_POWER_ON" to "Elevation Servo Power On",
            "SERVO_AZIMUTH_POWER_ON" to "Azimuth Servo Power On",
            
            // Servo Alarms
            "AZIMUTH_SERVO_ALARM" to "Azimuth Servo Alarm",
            "ELEVATION_SERVO_ALARM" to "Elevation Servo Alarm",
            "TRAIN_SERVO_ALARM" to "Train Servo Alarm",
            "AZIMUTH_SERVO_ALARM_RESOLVED" to "Azimuth Servo Alarm Resolved",
            "ELEVATION_SERVO_ALARM_RESOLVED" to "Elevation Servo Alarm Resolved",
            "TRAIN_SERVO_ALARM_RESOLVED" to "Train Servo Alarm Resolved",
            
            // Servo Alarm Codes
            "AZIMUTH_SERVO_ALARM_CODE1" to "Azimuth Servo Alarm Code 1",
            "AZIMUTH_SERVO_ALARM_CODE2" to "Azimuth Servo Alarm Code 2",
            "AZIMUTH_SERVO_ALARM_CODE3" to "Azimuth Servo Alarm Code 3",
            "AZIMUTH_SERVO_ALARM_CODE4" to "Azimuth Servo Alarm Code 4",
            "AZIMUTH_SERVO_ALARM_CODE5" to "Azimuth Servo Alarm Code 5",
            "ELEVATION_SERVO_ALARM_CODE1" to "Elevation Servo Alarm Code 1",
            "ELEVATION_SERVO_ALARM_CODE2" to "Elevation Servo Alarm Code 2",
            "ELEVATION_SERVO_ALARM_CODE3" to "Elevation Servo Alarm Code 3",
            "ELEVATION_SERVO_ALARM_CODE4" to "Elevation Servo Alarm Code 4",
            "ELEVATION_SERVO_ALARM_CODE5" to "Elevation Servo Alarm Code 5",
            "TRAIN_SERVO_ALARM_CODE1" to "Train Servo Alarm Code 1",
            "TRAIN_SERVO_ALARM_CODE2" to "Train Servo Alarm Code 2",
            "TRAIN_SERVO_ALARM_CODE3" to "Train Servo Alarm Code 3",
            "TRAIN_SERVO_ALARM_CODE4" to "Train Servo Alarm Code 4",
            "TRAIN_SERVO_ALARM_CODE5" to "Train Servo Alarm Code 5",
            
            // Servo Status
            "AZIMUTH_SERVO_BRAKE_ENGAGED" to "Azimuth Servo Brake Engaged",
            "AZIMUTH_SERVO_MOTOR_ON" to "Azimuth Servo Motor On",
            "ELEVATION_SERVO_BRAKE_ENGAGED" to "Elevation Servo Brake Engaged",
            "ELEVATION_SERVO_MOTOR_ON" to "Elevation Servo Motor On",
            "TRAIN_SERVO_BRAKE_ENGAGED" to "Train Servo Brake Engaged",
            "TRAIN_SERVO_MOTOR_ON" to "Train Servo Motor On",
            "AZIMUTH_SERVO_BRAKE_DISENGAGED" to "Azimuth Servo Brake Disengaged",
            "AZIMUTH_SERVO_MOTOR_OFF" to "Azimuth Servo Motor Off",
            "ELEVATION_SERVO_BRAKE_DISENGAGED" to "Elevation Servo Brake Disengaged",
            "ELEVATION_SERVO_MOTOR_OFF" to "Elevation Servo Motor Off",
            "TRAIN_SERVO_BRAKE_DISENGAGED" to "Train Servo Brake Disengaged",
            "TRAIN_SERVO_MOTOR_OFF" to "Train Servo Motor Off",
            
            // Positioner Status
            "AZIMUTH_LIMIT_SWITCH_POSITIVE_275" to "Azimuth +275° Limit Switch Active",
            "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275" to "Azimuth -275° Limit Switch Active",
            "ELEVATION_LIMIT_SWITCH_POSITIVE_180" to "Elevation +180° Limit Switch Active",
            "ELEVATION_LIMIT_SWITCH_POSITIVE_185" to "Elevation +185° Limit Switch Active",
            "ELEVATION_LIMIT_SWITCH_NEGATIVE_0" to "Elevation -0° Limit Switch Active",
            "ELEVATION_LIMIT_SWITCH_NEGATIVE_5" to "Elevation -5° Limit Switch Active",
            "TRAIN_LIMIT_SWITCH_POSITIVE_275" to "Train +275° Limit Switch Active", // ✅ 사진의 Tilt +275° 상태
            "TRAIN_LIMIT_SWITCH_NEGATIVE_275" to "Train -275° Limit Switch Active",
            "AZIMUTH_LIMIT_SWITCH_POSITIVE_275_INACTIVE" to "Azimuth +275° Limit Switch Inactive",
            "AZIMUTH_LIMIT_SWITCH_NEGATIVE_275_INACTIVE" to "Azimuth -275° Limit Switch Inactive",
            "ELEVATION_LIMIT_SWITCH_POSITIVE_180_INACTIVE" to "Elevation +180° Limit Switch Inactive",
            "ELEVATION_LIMIT_SWITCH_POSITIVE_185_INACTIVE" to "Elevation +185° Limit Switch Inactive",
            "ELEVATION_LIMIT_SWITCH_NEGATIVE_0_INACTIVE" to "Elevation -0° Limit Switch Inactive",
            "ELEVATION_LIMIT_SWITCH_NEGATIVE_5_INACTIVE" to "Elevation -5° Limit Switch Inactive",
            "TRAIN_LIMIT_SWITCH_POSITIVE_275_INACTIVE" to "Train +275° Limit Switch Inactive",
            "TRAIN_LIMIT_SWITCH_NEGATIVE_275_INACTIVE" to "Train -275° Limit Switch Inactive",
            
            // Stow Status
            "AZIMUTH_STOW_PIN_ACTIVE" to "Azimuth Stow Pin Active",
            "ELEVATION_STOW_PIN_ACTIVE" to "Elevation Stow Pin Active",
            "TRAIN_STOW_PIN_ACTIVE" to "Train Stow Pin Active",
            "AZIMUTH_STOW_PIN_INACTIVE" to "Azimuth Stow Pin Inactive",
            "ELEVATION_STOW_PIN_INACTIVE" to "Elevation Stow Pin Inactive",
            "TRAIN_STOW_PIN_INACTIVE" to "Train Stow Pin Inactive",
            
            // Encoder Errors
            "AZIMUTH_ENCODER_ERROR" to "Azimuth Encoder Error",
            "ELEVATION_ENCODER_ERROR" to "Elevation Encoder Error",
            "TRAIN_ENCODER_ERROR" to "Train Encoder Error",
            "AZIMUTH_ENCODER_NORMAL" to "Azimuth Encoder Normal",
            "ELEVATION_ENCODER_NORMAL" to "Elevation Encoder Normal",
            "TRAIN_ENCODER_NORMAL" to "Train Encoder Normal",
            
            // Feed Errors
            "S_BAND_LNA_LHCP_ERROR" to "S-Band LNA LHCP Error",
            "S_BAND_LNA_RHCP_ERROR" to "S-Band LNA RHCP Error",
            "S_BAND_RF_SWITCH_ERROR" to "S-Band RF Switch Error",
            "X_BAND_LNA_LHCP_ERROR" to "X-Band LNA LHCP Error",
            "X_BAND_LNA_RHCP_ERROR" to "X-Band LNA RHCP Error",
            "FAN_ERROR" to "Fan Error",
            "S_BAND_LNA_LHCP_NORMAL" to "S-Band LNA LHCP Normal",
            "S_BAND_LNA_RHCP_NORMAL" to "S-Band LNA RHCP Normal",
            "S_BAND_RF_SWITCH_NORMAL" to "S-Band RF Switch Normal",
            "X_BAND_LNA_LHCP_NORMAL" to "X-Band LNA LHCP Normal",
            "X_BAND_LNA_RHCP_NORMAL" to "X-Band LNA RHCP Normal",
            "FAN_NORMAL" to "Fan Normal",
            
            // Feed Status
            "S_BAND_LNA_LHCP_POWER_ON" to "S-Band LNA LHCP Power On",
            "S_BAND_LNA_RHCP_POWER_ON" to "S-Band LNA RHCP Power On",
            "S_BAND_RF_SWITCH_RHCP" to "S-Band RF Switch RHCP Mode",
            "X_BAND_LNA_LHCP_POWER_ON" to "X-Band LNA LHCP Power On",
            "X_BAND_LNA_RHCP_POWER_ON" to "X-Band LNA RHCP Power On",
            "FAN_POWER_ON" to "Fan Power On",
            "S_BAND_LNA_LHCP_POWER_OFF" to "S-Band LNA LHCP Power Off",
            "S_BAND_LNA_RHCP_POWER_OFF" to "S-Band LNA RHCP Power Off",
            "S_BAND_RF_SWITCH_LHCP" to "S-Band RF Switch LHCP Mode",
            "X_BAND_LNA_LHCP_POWER_OFF" to "X-Band LNA LHCP Power Off",
            "X_BAND_LNA_RHCP_POWER_OFF" to "X-Band LNA RHCP Power Off",
            "FAN_POWER_OFF" to "Fan Power Off"
        )
        else -> emptyMap()
    }
    
    return messages[messageKey] ?: messageKey
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
