package com.gtlsystems.acs_api.controller.icd

import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.service.udp.UdpFwICDService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.BitSet

@RestController
@RequestMapping("/api/icd")
class ICDController(private val udpFwICDService: UdpFwICDService) {

    private val logger = LoggerFactory.getLogger(ICDController::class.java)

    @PostMapping("/servo-preset-command")
    fun servoPresetCommand(
        @RequestParam azimuth: Boolean = false,
        @RequestParam elevation: Boolean = false,
        @RequestParam tilt: Boolean = false
    ): ResponseEntity<Map<String, String>> {
        return try {
            val bitAxis = BitSet()
            if (azimuth) bitAxis.set(0)
            if (elevation) bitAxis.set(1)
            if (tilt) bitAxis.set(2)

            val axesStr = listOfNotNull(
                if (azimuth) "AZIMUTH" else null,
                if (elevation) "ELEVATION" else null,
                if (tilt) "TILT" else null
            ).joinToString(",")

            udpFwICDService.servoPresetCommand(bitAxis)

            logger.info("ServoPreset 명령 요청 완료: {}", axesStr)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "ServoPreset 명령이 성공적으로 전송되었습니다",
                "command" to "ServoPreset",
                "axes" to axesStr
            ))
        } catch (e: Exception) {
            logger.error("ServoPreset 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "ServoPreset 명령 전송 실패: ${e.message}"
            ))
        }
    }
    @PostMapping("/standby-command")
    fun standbyCommand(
        @RequestParam azStandby: Boolean = false,
        @RequestParam elStandby: Boolean = false,
        @RequestParam tiStandby: Boolean = false
    ): ResponseEntity<Map<String, String>> {
        return try {
            val bitStandby = BitSet()
            if (azStandby) bitStandby.set(0)
            if (elStandby) bitStandby.set(1)
            if (tiStandby) bitStandby.set(2)

            val axesStr = listOfNotNull(
                if (azStandby) "AZIMUTH" else null,
                if (elStandby) "ELEVATION" else null,
                if (tiStandby) "TILT" else null
            ).joinToString(",")

            udpFwICDService.standbyCommand(bitStandby)

            logger.info("Standby 명령 요청 완료: {}", axesStr)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Standby 명령이 성공적으로 전송되었습니다",
                "command" to "Standby",
                "axes" to axesStr
            ))
        } catch (e: Exception) {
            logger.error("Standby 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Standby 명령 전송 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/on-emergency-stop-command")
    fun onEmergencyStopCommand(@RequestParam commandType: Char): ResponseEntity<Map<String, String>> {
        return try {
            // commandType이 'E' 또는 'S'인지 검증
            if (commandType != 'E' && commandType != 'S') {
                logger.warn("유효하지 않은 비상 명령 타입: {}", commandType)
                return ResponseEntity.badRequest().body(mapOf(
                    "status" to "error",
                    "message" to "명령 타입은 'E' 또는 'S'여야 합니다"
                ))
            }

            udpFwICDService.onEmergencyCommand(commandType)

            val status = if (commandType == 'E') "활성화" else "비활성화"
            logger.info("비상 명령 요청 완료: {}", status)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "비상 명령이 성공적으로 ${status} 되었습니다",
                "command" to "Emergency",
                "type" to commandType.toString()
            ))
        } catch (e: Exception) {
            logger.error("비상 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "비상 명령 전송 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/time-offset-command")
    fun timeOffsetCommand(@RequestParam inputTimeOffset: Float): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.timeOffsetCommand(inputTimeOffset)

            logger.info("TimeOffset 명령 요청 완료: {}s", inputTimeOffset)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "TimeOffset 명령이 성공적으로 전송되었습니다",
                "command" to "TimeOffset",
                "timeOffset" to inputTimeOffset.toString()
            ))
        } catch (e: Exception) {
            logger.error("TimeOffset 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "TimeOffset 명령 전송 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/multi-control-command")
    fun multiManualControlCommand(
        @RequestParam azimuth: Boolean = false,
        @RequestParam elevation: Boolean = false,
        @RequestParam tilt: Boolean = false,
        @RequestParam stow: Boolean = false,
        @RequestParam azAngle: Float,
        @RequestParam azSpeed: Float,
        @RequestParam elAngle: Float,
        @RequestParam elSpeed: Float,
        @RequestParam tiAngle: Float,
        @RequestParam tiSpeed: Float
    ): ResponseEntity<Map<String, String>> {
        return try {
            val multiAxis = BitSet()
            if (azimuth) multiAxis.set(0)
            if (elevation) multiAxis.set(1)
            if (tilt) multiAxis.set(2)
            if (stow) multiAxis.set(7)

            val axesStr = listOfNotNull(
                if (azimuth) "AZIMUTH" else null,
                if (elevation) "ELEVATION" else null,
                if (tilt) "TILT" else null,
                if (stow) "STOW" else null
            ).joinToString(",")

            udpFwICDService.multiManualCommand(
                multiAxis, azAngle, azSpeed, elAngle, elSpeed, tiAngle, tiSpeed
            )

            logger.info("MultiManual 제어 명령 요청 완료: {}", axesStr)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "MultiManual 제어 명령이 성공적으로 전송되었습니다",
                "command" to "MultiManualControl",
                "axes" to axesStr,
                "angles" to "Az:${azAngle}°, El:${elAngle}°, Ti:${tiAngle}°"
            ))
        } catch (e: Exception) {
            logger.error("MultiManual 제어 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "MultiManual 제어 명령 전송 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/feed-on-off-command")
    fun feedOnOffCommand(
        @RequestParam sLHCP: Boolean = false,
        @RequestParam sRHCP: Boolean = false,
        @RequestParam sRFSwitch: Boolean = false,
        @RequestParam xLHCP: Boolean = false,
        @RequestParam xRHCP: Boolean = false,
        @RequestParam fan: Boolean = false
    ): ResponseEntity<Map<String, String>> {
        return try {
            val bitFeedOnoff = BitSet()
            if (sLHCP) bitFeedOnoff.set(0)
            if (sRHCP) bitFeedOnoff.set(1)
            if (sRFSwitch) bitFeedOnoff.set(2)
            if (xLHCP) bitFeedOnoff.set(4)
            if (xRHCP) bitFeedOnoff.set(5)
            if (fan) bitFeedOnoff.set(7)

            val bitStr = listOfNotNull(
                if (sLHCP) "S-Band LHCP" else null,
                if (sRHCP) "S-Band RHCP" else null,
                if (sRFSwitch) "S-RFSwitch" else null,
                if (xLHCP) "X-Band LHCP" else null,
                if (xRHCP) "X-Band RHCP" else null,
                if (fan) "FAN" else null
            ).joinToString(",")

            udpFwICDService.feedOnOffCommand(bitFeedOnoff)

            logger.info("FeedOnOff 명령 요청 완료: {}", bitStr)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "FeedOnOff 명령이 성공적으로 전송되었습니다",
                "command" to "FeedOnOff",
                "feeds" to bitStr
            ))
        } catch (e: Exception) {
            logger.error("FeedOnOff 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "FeedOnOff 명령 전송 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/position-offset-command")
    fun positionOffsetCommand(
        @RequestParam azOffset: Float,
        @RequestParam elOffset: Float,
        @RequestParam tiOffset: Float
    ): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.positionOffsetCommand(azOffset, elOffset, tiOffset)

            logger.info("PositionOffset 명령 요청 완료: Az={}°, El={}°, Ti={}°", azOffset, elOffset, tiOffset)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "PositionOffset 명령이 성공적으로 전송되었습니다",
                "command" to "PositionOffset",
                "offsets" to "Az:${azOffset}°, El:${elOffset}°, Ti:${tiOffset}°"
            ))
        } catch (e: Exception) {
            logger.error("PositionOffset 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "PositionOffset 명령 전송 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/stop-command")
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
                if (tiStop) "Tilt Stop" else null
            ).joinToString(",")

            udpFwICDService.stopCommand(bitStop)

            logger.info("Stop 명령 요청 완료: {}", bitStr)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Stop 명령이 성공적으로 전송되었습니다",
                "command" to "Stop",
                "axes" to bitStr
            ))
        } catch (e: Exception) {
            logger.error("Stop 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Stop 명령 전송 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/default-info-command")
    fun defaultInfoCommand(
    ): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.defaultInfoCommand()
            logger.info("DefaultInfo 명령 요청 완료")

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "DefaultInfo 명령이 성공적으로 전송되었습니다",
                "command" to "DefaultInfo",
                "timeOffset" to GlobalData.Offset.TimeOffset.toString(),
                "offsets" to "Az:${GlobalData.Offset.azimuthPositionOffset}°, El:${GlobalData.Offset.elevationPositionOffset}°, Ti:${GlobalData.Offset.tiltPositionOffset}°"
            ))
        } catch (e: Exception) {
            logger.error("DefaultInfo 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "DefaultInfo 명령 전송 실패: ${e.message}"
            ))
        }
    }
    @PostMapping("/write-ntp-command")
    fun writeNtpCommand(
    ): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.writeNTPCommand()
            logger.info("writeNTPCommand 명령 요청 완료")

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "writeNTPCommand 명령이 성공적으로 전송되었습니다",
                "command" to "writeNTPCommand",
                "timeOffset" to GlobalData.Offset.TimeOffset.toString(),
            ))
        } catch (e: Exception) {
            logger.error("writeNTPCommand 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "writeNTPCommand 명령 전송 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/stow-command")
    fun stowCommand(): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.StowCommand()

            logger.info("Stow 명령 요청 완료")

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Stow 명령이 성공적으로 시작되었습니다",
                "command" to "Stow"
            ))
        } catch (e: Exception) {
            logger.error("Stow 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "Stow 명령 시작 실패: ${e.message}"
            ))
        }
    }

    // === 추가 유틸리티 엔드포인트들 ===

    @PostMapping("/communication-status")
    fun getCommunicationStatus(): ResponseEntity<Map<String, Any>> {
        return try {
            val stats = udpFwICDService.getUdpPerformanceStats()
            val isHealthy = udpFwICDService.isCommunicationHealthy()

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "healthy" to isHealthy,
                "statistics" to stats,
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            logger.error("통신 상태 조회 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "통신 상태 조회 실패: ${e.message}"
            ))
        }
    }

    @PostMapping("/test-command")
    fun sendTestCommand(): ResponseEntity<Map<String, String>> {
        return try {
            udpFwICDService.sendTestCommand()

            logger.info("테스트 명령 요청 완료")

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "테스트 명령이 성공적으로 전송되었습니다",
                "command" to "Test"
            ))
        } catch (e: Exception) {
            logger.error("테스트 명령 요청 실패: {}", e.message, e)
            ResponseEntity.internalServerError().body(mapOf(
                "status" to "error",
                "message" to "테스트 명령 전송 실패: ${e.message}"
            ))
        }
    }
}