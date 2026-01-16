package com.gtlsystems.acs_api.controller.icd

import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.service.udp.UdpFwICDService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.BitSet
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import com.gtlsystems.acs_api.model.SystemInfo
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/icd")
@Tag(name = "ICD - Communication", description = "외부 시스템과의 ICD 통신 API - 서보 제어, 비상 정지, 통신 상태 모니터링")
class ICDController(private val udpFwICDService: UdpFwICDService) {

    private val logger = LoggerFactory.getLogger(ICDController::class.java)

    @PostMapping("/servo-preset-command")
    @Operation(
        operationId = "servoPresetCommand",
        tags = ["ICD - Communication"]
    )
    fun servoPresetCommand(
        @Parameter(
            description = "방위각 축 프리셋 여부",
            example = "true",
            required = false
        )
        @RequestParam azimuth: Boolean = false,
        @Parameter(
            description = "고도각 축 프리셋 여부",
            example = "true",
            required = false
        )
        @RequestParam elevation: Boolean = false,
        @Parameter(
            description = "기울기 축 프리셋 여부",
            example = "false",
            required = false
        )
        @RequestParam train: Boolean = false
    ): ResponseEntity<Map<String, String>> {
        return try {
            val bitAxis = BitSet()
            if (azimuth) bitAxis.set(0)
            if (elevation) bitAxis.set(1)
            if (train) bitAxis.set(2)

            val axesStr = listOfNotNull(
                if (azimuth) "AZIMUTH" else null,
                if (elevation) "ELEVATION" else null,
                if (train) "TRAIN" else null
            ).joinToString(",")

            udpFwICDService.servoPresetCommand(bitAxis)

            logger.info("ServoPreset 명령 요청 완료: {}", axesStr)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "ServoPreset 명령이 성공적으로 전송되었습니다",
                    "command" to "ServoPreset",
                    "axes" to axesStr
                )
            )
        } catch (e: java.io.IOException) {
            logger.error("ServoPreset 명령 통신 오류: {}", e.message, e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "error",
                    "message" to "ServoPreset 명령 전송 실패: 하드웨어 통신 오류"
                )
            )
        } catch (e: Exception) {
            logger.error("ServoPreset 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "ServoPreset 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/standby-command")
    @Operation(
        operationId = "standbyCommand",
        tags = ["ICD - Communication"]
    )
    fun standbyCommand(
        @Parameter(
            description = "방위각 축 대기 모드 여부",
            example = "true",
            required = false
        )
        @RequestParam azStandby: Boolean = false,
        @Parameter(
            description = "고도각 축 대기 모드 여부",
            example = "true",
            required = false
        )
        @RequestParam elStandby: Boolean = false,
        @Parameter(
            description = "기울기 축 대기 모드 여부",
            example = "false",
            required = false
        )
        @RequestParam trainStandby: Boolean = false
    ): ResponseEntity<Map<String, String>> {
        return try {
            val bitStandby = BitSet()
            if (azStandby) bitStandby.set(0)
            if (elStandby) bitStandby.set(1)
            if (trainStandby) bitStandby.set(2)

            val axesStr = listOfNotNull(
                if (azStandby) "AZIMUTH" else null,
                if (elStandby) "ELEVATION" else null,
                if (trainStandby) "Train" else null
            ).joinToString(",")

            udpFwICDService.standbyCommand(bitStandby)

            logger.info("Standby 명령 요청 완료: {}", axesStr)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "Standby 명령이 성공적으로 전송되었습니다",
                    "command" to "Standby",
                    "axes" to axesStr
                )
            )
        } catch (e: java.io.IOException) {
            logger.error("Standby 명령 통신 오류: {}", e.message, e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "error",
                    "message" to "Standby 명령 전송 실패: 하드웨어 통신 오류"
                )
            )
        } catch (e: Exception) {
            logger.error("Standby 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "Standby 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/on-emergency-stop-command")
    @Operation(
        operationId = "onEmergencyStopCommand",
        tags = ["ICD - Communication"]
    )
    fun onEmergencyStopCommand(
        @Parameter(
            description = "비상 정지 명령 타입 (E: Emergency, S: Safe)",
            example = "E",
            required = true
        )
        @RequestParam commandType: Char
    ): ResponseEntity<Map<String, String>> {
        return try {
            // commandType이 'E' 또는 'S'인지 검증
            if (commandType != 'E' && commandType != 'S') {
                logger.warn("유효하지 않은 비상 명령 타입: {}", commandType)
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "status" to "error",
                        "message" to "명령 타입은 'E' 또는 'S'여야 합니다"
                    )
                )
            }

            udpFwICDService.onEmergencyCommand(commandType)

            val status = if (commandType == 'E') "활성화" else "비활성화"
            logger.info("비상 명령 요청 완료: {}", status)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "비상 명령이 성공적으로 ${status} 되었습니다",
                    "command" to "Emergency",
                    "type" to commandType.toString()
                )
            )
        } catch (e: java.io.IOException) {
            logger.error("비상 명령 통신 오류: {}", e.message, e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "error",
                    "message" to "비상 명령 전송 실패: 하드웨어 통신 오류"
                )
            )
        } catch (e: Exception) {
            logger.error("비상 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "비상 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/time-offset-command")
    @Operation(
        operationId = "timeOffsetCommand",
        tags = ["ICD - Communication"]
    )
    fun timeOffsetCommand(@RequestParam inputTimeOffset: Float): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.timeOffsetCommand(inputTimeOffset)

            logger.info("TimeOffset 명령 요청 완료: {}s", inputTimeOffset)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "TimeOffset 명령이 성공적으로 전송되었습니다",
                    "command" to "TimeOffset",
                    "timeOffset" to inputTimeOffset.toString()
                )
            )
        } catch (e: java.io.IOException) {
            logger.error("TimeOffset 명령 통신 오류: {}", e.message, e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "error",
                    "message" to "TimeOffset 명령 전송 실패: 하드웨어 통신 오류"
                )
            )
        } catch (e: Exception) {
            logger.error("TimeOffset 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "TimeOffset 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/multi-control-command")
    @Operation(
        operationId = "multiControlCommand",
        tags = ["ICD - Communication"]
    )
    fun multiManualControlCommand(
        @RequestParam azimuth: Boolean = false,
        @RequestParam elevation: Boolean = false,
        @RequestParam train: Boolean = false,
        @RequestParam stow: Boolean = false,
        @RequestParam azAngle: Float,
        @RequestParam azSpeed: Float,
        @RequestParam elAngle: Float,
        @RequestParam elSpeed: Float,
        @RequestParam trainAngle: Float,
        @RequestParam trainSpeed: Float
    ): ResponseEntity<Map<String, String>> {
        return try {
            val multiAxis = BitSet()
            if (azimuth) multiAxis.set(0)
            if (elevation) multiAxis.set(1)
            if (train) multiAxis.set(2)
            if (stow) multiAxis.set(7)

            val axesStr = listOfNotNull(
                if (azimuth) "AZIMUTH" else null,
                if (elevation) "ELEVATION" else null,
                if (train) "Train" else null,
                if (stow) "STOW" else null
            ).joinToString(",")

            udpFwICDService.multiManualCommand(
                multiAxis, azAngle, azSpeed, elAngle, elSpeed, trainAngle, trainSpeed
            )

            logger.info("MultiManual 제어 명령 요청 완료: {}", axesStr)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "MultiManual 제어 명령이 성공적으로 전송되었습니다",
                    "command" to "MultiManualControl",
                    "axes" to axesStr,
                    "angles" to "Az:${azAngle}°, El:${elAngle}°, Train:${trainAngle}°"
                )
            )
        } catch (e: java.io.IOException) {
            logger.error("MultiManual 제어 명령 통신 오류: {}", e.message, e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "error",
                    "message" to "MultiManual 제어 명령 전송 실패: 하드웨어 통신 오류"
                )
            )
        } catch (e: Exception) {
            logger.error("MultiManual 제어 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "MultiManual 제어 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/feed-on-off-command")
    @Operation(
        operationId = "feedOnOffCommand",
        tags = ["ICD - Communication"]
    )
    fun feedOnOffCommand(
        @RequestParam sLHCP: Boolean = false,
        @RequestParam sRHCP: Boolean = false,
        @RequestParam sRFSwitch: Boolean = false, // false: RHCP, true: LHCP
        @RequestParam xLHCP: Boolean = false,
        @RequestParam xRHCP: Boolean = false,
        @RequestParam fan: Boolean = false,
        // Ka-Band 파라미터 추가
        @RequestParam kaLHCP: Boolean = false,
        @RequestParam kaRHCP: Boolean = false,
        @RequestParam kaSelectionRHCP: Boolean = false, // false: Band1, true: Band2
        @RequestParam kaSelectionLHCP: Boolean = false  // false: Band1, true: Band2
    ): ResponseEntity<Map<String, String>> {
        return try {
            /**
             * 프로토콜 명세에 따른 비트 매핑 (2바이트 Unsigned Short):
             * Bit 15-12: Reserved
             * Bit 11: Ka-Band RHCP Selection (0: Band1, 1: Band2)
             * Bit 10: Ka-Band LHCP Selection (0: Band1, 1: Band2)
             * Bit 9: Ka-Band RHCP (0: Off, 1: On)
             * Bit 8: Ka-Band LHCP (0: Off, 1: On)
             * Bit 7-6: Reserved
             * Bit 5: Fan (0: Off, 1: On)
             * Bit 4: S Band RF Switch (0: RHCP, 1: LHCP)
             * Bit 3: X-Band RHCP (0: Off, 1: On)
             * Bit 2: X-Band LHCP (0: Off, 1: On)
             * Bit 1: S-Band RHCP (0: Off, 1: On)
             * Bit 0: S-Band LHCP (0: Off, 1: On)
             */
            val bitFeedOnoff = BitSet(16) // 16비트(2바이트) BitSet 생성
            
            // Bit 0: S-Band LHCP
            if (sLHCP) bitFeedOnoff.set(0)
            
            // Bit 1: S-Band RHCP
            if (sRHCP) bitFeedOnoff.set(1)
            
            // Bit 2: X-Band LHCP
            if (xLHCP) bitFeedOnoff.set(2)
            
            // Bit 3: X-Band RHCP
            if (xRHCP) bitFeedOnoff.set(3)
            
            // Bit 4: S Band RF Switch (false: RHCP, true: LHCP)
            if (sRFSwitch) bitFeedOnoff.set(4)
            
            // Bit 5: Fan
            if (fan) bitFeedOnoff.set(5)
            
            // Bit 8: Ka-Band LHCP
            if (kaLHCP) bitFeedOnoff.set(8)
            
            // Bit 9: Ka-Band RHCP
            if (kaRHCP) bitFeedOnoff.set(9)
            
            // Bit 10: Ka-Band LHCP Selection (false: Band1, true: Band2)
            if (kaSelectionLHCP) bitFeedOnoff.set(10)
            
            // Bit 11: Ka-Band RHCP Selection (false: Band1, true: Band2)
            if (kaSelectionRHCP) bitFeedOnoff.set(11)

            val bitStr = listOfNotNull(
                if (sLHCP) "S-Band LHCP" else null,
                if (sRHCP) "S-Band RHCP" else null,
                if (sRFSwitch) "S-Band RF Switch(LHCP)" else null,
                if (xLHCP) "X-Band LHCP" else null,
                if (xRHCP) "X-Band RHCP" else null,
                if (fan) "FAN" else null,
                if (kaLHCP) "Ka-Band LHCP" else null,
                if (kaRHCP) "Ka-Band RHCP" else null,
                if (kaSelectionLHCP) "Ka-Selection LHCP(Band2)" else if (kaLHCP) "Ka-Selection LHCP(Band1)" else null,
                if (kaSelectionRHCP) "Ka-Selection RHCP(Band2)" else if (kaRHCP) "Ka-Selection RHCP(Band1)" else null
            ).joinToString(",")

            udpFwICDService.feedOnOffCommand(bitFeedOnoff)

            logger.info("FeedOnOff 명령 요청 완료: {}", bitStr)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "FeedOnOff 명령이 성공적으로 전송되었습니다",
                    "command" to "FeedOnOff",
                    "feeds" to bitStr
                )
            )
        } catch (e: Exception) {
            logger.error("FeedOnOff 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "FeedOnOff 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/position-offset-command")
    @Operation(
        operationId = "positionOffsetCommand",
        tags = ["ICD - Communication"]
    )
    fun positionOffsetCommand(
        @RequestParam azOffset: Float,
        @RequestParam elOffset: Float,
        @RequestParam tiOffset: Float
    ): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.positionOffsetCommand(azOffset, elOffset, tiOffset)

            logger.info("PositionOffset 명령 요청 완료: Az={}°, El={}°, Ti={}°", azOffset, elOffset, tiOffset)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "PositionOffset 명령이 성공적으로 전송되었습니다",
                    "command" to "PositionOffset",
                    "offsets" to "Az:${azOffset}°, El:${elOffset}°, Ti:${tiOffset}°"
                )
            )
        } catch (e: Exception) {
            logger.error("PositionOffset 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "PositionOffset 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/stop-command")
    @Operation(
        operationId = "stopCommand",
        tags = ["ICD - Communication"]
    )
    fun stopCommand(
        @RequestParam azStop: Boolean = false,
        @RequestParam elStop: Boolean = false,
        @RequestParam tiStop: Boolean = false
    ): ResponseEntity<Map<String, String>> {
        return try {
            val bitStop = BitSet()
            if (azStop) bitStop.set(0)
            if (elStop) bitStop.set(1)
            if (tiStop) bitStop.set(2)

            val bitStr = listOfNotNull(
                if (azStop) "Azimuth Stop" else null,
                if (elStop) "Elevation Stop" else null,
                if (tiStop) "TRAIN Stop" else null
            ).joinToString(",")

            udpFwICDService.stopCommand(bitStop)

            logger.info("Stop 명령 요청 완료: {}", bitStr)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "Stop 명령이 성공적으로 전송되었습니다",
                    "command" to "Stop",
                    "axes" to bitStr
                )
            )
        } catch (e: java.io.IOException) {
            logger.error("Stop 명령 통신 오류: {}", e.message, e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "error",
                    "message" to "Stop 명령 전송 실패: 하드웨어 통신 오류"
                )
            )
        } catch (e: Exception) {
            logger.error("Stop 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "Stop 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/default-info-command")
    @Operation(
        operationId = "defaultInfoCommand",
        tags = ["ICD - Communication"]
    )
    fun defaultInfoCommand(
    ): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.defaultInfoCommand()
            logger.info("DefaultInfo 명령 요청 완료")

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "DefaultInfo 명령이 성공적으로 전송되었습니다",
                    "command" to "DefaultInfo",
                    "utcTime" to GlobalData.Time.utcNow.toString(),
                    "timeOffset" to GlobalData.Offset.TimeOffset.toString(),
                    "offsets" to "Az:${GlobalData.Offset.azimuthPositionOffset}°, El:${GlobalData.Offset.elevationPositionOffset}°, Ti:${GlobalData.Offset.trainPositionOffset}°"
                )
            )
        } catch (e: java.io.IOException) {
            logger.error("DefaultInfo 명령 통신 오류: {}", e.message, e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "error",
                    "message" to "DefaultInfo 명령 전송 실패: 하드웨어 통신 오류"
                )
            )
        } catch (e: Exception) {
            logger.error("DefaultInfo 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "DefaultInfo 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/write-ntp-command")
    @Operation(
        operationId = "writeNtpCommand",
        tags = ["ICD - Communication"]
    )
    fun writeNtpCommand(
    ): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.writeNTPCommand()
            logger.info("writeNTPCommand 명령 요청 완료")

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "writeNTPCommand 명령이 성공적으로 전송되었습니다",
                    "command" to "writeNTPCommand",
                    "timeOffset" to GlobalData.Offset.TimeOffset.toString(),
                )
            )
        } catch (e: Exception) {
            logger.error("writeNTPCommand 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "writeNTPCommand 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/stow-command")
    @Operation(
        operationId = "stowCommand",
        tags = ["ICD - Communication"]
    )
    fun stowCommand(): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.StowCommand()

            logger.info("Stow 명령 요청 완료")

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "Stow 명령이 성공적으로 시작되었습니다",
                    "command" to "Stow"
                )
            )
        } catch (e: java.io.IOException) {
            logger.error("Stow 명령 통신 오류: {}", e.message, e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "error",
                    "message" to "Stow 명령 시작 실패: 하드웨어 통신 오류"
                )
            )
        } catch (e: Exception) {
            logger.error("Stow 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "Stow 명령 시작 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/mc-on-off-command")
    @Operation(
        operationId = "mcOnOffCommand",
        tags = ["ICD - Communication"]
    )
    fun mcOnOffCommand(
        @Parameter(
            description = "M/C On/Off 상태 (true: ON, false: OFF)",
            example = "true",
            required = true
        )
        @RequestParam onOff: Boolean
    ): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.mcOnOffCommand(onOff)

            val status = if (onOff) "ON" else "OFF"
            logger.info("M/C On/Off 명령 요청 완료: {}", status)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "message" to "M/C On/Off 명령이 성공적으로 전송되었습니다",
                    "command" to "MCOnOff",
                    "state" to status
                )
            )
        } catch (e: Exception) {
            logger.error("M/C On/Off 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "M/C On/Off 명령 전송 실패: ${e.message}"
                )
            )
        }
    }

    // === 추가 유틸리티 엔드포인트들 ===

    @PostMapping("/communication-status")
    @Operation(
        operationId = "communicationStatus",
        tags = ["ICD - Communication"]
    )
    fun getCommunicationStatus(): ResponseEntity<Map<String, Any>> {
        return try {
            val stats = udpFwICDService.getUdpPerformanceStats()
            val isHealthy = udpFwICDService.isCommunicationHealthy()

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "healthy" to isHealthy,
                    "statistics" to stats,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("통신 상태 조회 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "status" to "error",
                    "message" to "통신 상태 조회 실패: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/servo-alarm-reset")
    @Operation(
        operationId = "servoAlarmResetCommand",
        tags = ["ICD - Communication"]
    )
    fun servoAlarmResetCommand(
        @Parameter(
            description = "Azimuth 축 알람 리셋 여부",
            example = "false",
            required = true
        )
        @RequestParam azimuth: Boolean,
        @Parameter(
            description = "Elevation 축 알람 리셋 여부",
            example = "false",
            required = true
        )
        @RequestParam elevation: Boolean,
        @Parameter(
            description = "Train 축 알람 리셋 여부",
            example = "false",
            required = true
        )
        @RequestParam train: Boolean
    ): ResponseEntity<Map<String, String>> {
        return try {
            val bitAxis = BitSet()
            if (azimuth) bitAxis.set(0)
            if (elevation) bitAxis.set(1)
            if (train) bitAxis.set(2)

            udpFwICDService.servoAlarmResetCommand(bitAxis)

            val axisList = mutableListOf<String>()
            if (azimuth) axisList.add("Azimuth")
            if (elevation) axisList.add("Elevation")
            if (train) axisList.add("Train")

            ResponseEntity.ok(
                mapOf(
                    "success" to "true",
                    "message" to "Servo Alarm Reset 명령 전송 완료: ${axisList.joinToString(", ")}"
                )
            )
        } catch (e: Exception) {
            logger.error("Servo Alarm Reset 명령 실패", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "success" to "false",
                    "message" to "Servo Alarm Reset 명령 실패: ${e.message}"
                )
            )
        }
    }

    @GetMapping("/firmware-version-serial-no")
    @Operation(
        operationId = "getFirmwareVersionSerialNo",
        tags = ["ICD - Communication"]
    )
    fun getFirmwareVersionSerialNo(): Mono<ResponseEntity<Map<String, Any>>> {
        return udpFwICDService.readFwVerSerialNoStatusCommand()
            .map { result ->
                // SystemInfo에서 실제 데이터 조회
                val firmwareData = SystemInfo.FIRMWARE_VERSION_SERIAL_NO
                
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "Firmware Version/Serial Number 조회 성공",
                    "data" to mapOf(
                        "scanAvailable" to true,
                        "firmwareData" to firmwareData,
                        "commandResult" to result
                    )
                ))
            }
            .onErrorResume { e ->
                logger.error("Firmware Version/Serial Number 조회 실패", e)
                Mono.just(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                        "success" to false,
                        "message" to "Firmware Version/Serial Number 조회 실패: ${e.message}"
                    ))
                )
            }
    }
}
