package com.gtlsystems.acs_api.controller

import com.gtlsystems.acs_api.service.UdpFwICDService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.BitSet

@RestController
@RequestMapping("/api/icd")
@CrossOrigin(origins = ["http://localhost:9000"]) // 프론트엔드 도메인 허용
class ICDController(private val udpFwICDService: UdpFwICDService) {

    @PostMapping("/servo-preset-command")
    fun servoPresetCommand(
        @RequestParam azimuth: Boolean = false,
        @RequestParam elevation: Boolean = false,
        @RequestParam tilt: Boolean = false
    ): Mono<String> {
        val bitAxis = BitSet()
        if (azimuth) bitAxis.set(0)
        if (elevation) bitAxis.set(1)
        if (tilt) bitAxis.set(2)

        val axesStr = listOfNotNull(
            if (azimuth) "AZIMUTH" else null,
            if (elevation) "ELEVATION" else null,
            if (tilt) "TILT" else null
        ).joinToString(",")

        return Mono.fromCallable {
            udpFwICDService.servoPresetCommand(bitAxis)
            "UDP 명령어 전송 요청 완료 (Command: $axesStr)"
        }.thenReturn("servoPresetCommand UDP 명령어 전송 요청 완료 (Command: $axesStr, $bitAxis)")
    }
    @PostMapping("/on-emergency-stop-command")
    fun onEmergencyStopCommand(@RequestParam commandType: Char): ResponseEntity<Map<String, String>> {
        // commandType이 'E' 또는 'S'인지 검증
        if (commandType != 'E' && commandType != 'S') {
            return ResponseEntity.badRequest().body(mapOf("error" to "명령 타입은 'E' 또는 'S'여야 합니다"))
        }

        // 서비스 메소드 호출
        udpFwICDService.onEmergencyCommand(commandType)

        val status = if (commandType == 'E') "활성화" else "비활성화"
        return ResponseEntity.ok(mapOf("message" to "비상 명령이 성공적으로 $status 되었습니다"))
    }

    @PostMapping("/time-offset-command")
    fun timeOffsetCommand(@RequestParam inputTimeOffset: Float): Mono<String> {
        return Mono.fromCallable {
            udpFwICDService.timeOffsetCommand(inputTimeOffset)
            "UDP 명령어 전송 요청 완료 (Command: $inputTimeOffset)"
        }.thenReturn("timeOffsetCommand UDP 명령어 전송 요청 완료 (Command: $inputTimeOffset)")
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
    ): Mono<String> {
        val multiAxis = BitSet()
        if (azimuth) multiAxis.set(0)
        if (elevation) multiAxis.set(1)
        if (tilt) multiAxis.set(2)
        if (stow) multiAxis.set(7)

        val axesStr = listOfNotNull(
            if (azimuth) "AZIMUTH" else null,
            if (elevation) "ELEVATION" else null,
            if (tilt) "TILT" else null,
            if (stow) "STOW" else null,
        ).joinToString(",")

        return Mono.fromCallable {
            udpFwICDService.multiManualCommand(
                multiAxis,
                azAngle,
                azSpeed,
                elAngle,
                elSpeed,
                tiAngle,
                tiSpeed
            )
            "UDP 명령어 전송 요청 완료 (multiManualControlCommand: $axesStr, $multiAxis, $azAngle,$azSpeed,$elAngle,$elSpeed,$tiAngle,$tiSpeed)"
        }.thenReturn("multiManualControlCommand UDP 명령어 전송 요청 완료 (Command: $multiAxis, $azAngle,$azSpeed,$elAngle,$elSpeed,$tiAngle,$tiSpeed)")
    }

    @PostMapping("/feed-on-off-command")
    fun feedOnOffCommand(
        @RequestParam sLHCP: Boolean = false,
        @RequestParam sRHCP: Boolean = false,
        @RequestParam sRFSwitch: Boolean = false,
        @RequestParam xLHCP: Boolean = false,
        @RequestParam xRHCP: Boolean = false,
        @RequestParam fan: Boolean = false,
    ): Mono<String> {
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
            if (fan) "FAN" else null,
        ).joinToString(",")

        return Mono.fromCallable {
            udpFwICDService.feedOnOffCommand(bitFeedOnoff)
            "UDP 명령어 전송 요청 완료 (Command: $bitStr, $sLHCP, $sRHCP,$sRFSwitch,$xLHCP,$xRHCP,$fan)"
        }.thenReturn("feedOnOffCommand UDP 명령어 전송 요청 완료 (Command: $bitStr, $sLHCP, $sRHCP,$sRFSwitch,$xLHCP,$xRHCP,$fan)")
    }

    @PostMapping("/position-offset-command")
    fun positionOffsetCommand(
        @RequestParam azOffset: Float,
        @RequestParam elOffset: Float,
        @RequestParam tiOffest: Float
    ): Mono<String> {
        return Mono.fromCallable {
            udpFwICDService.positionOffsetCommand(azOffset, elOffset, tiOffest)
            "UDP 명령어 전송 요청 완료 (Command: $azOffset, $elOffset, $tiOffest)"
        }.thenReturn("positionOffsetCommand UDP 명령어 전송 요청 완료 (Command: $azOffset, $elOffset, $tiOffest)")
    }

    @PostMapping("/stop-command")
    fun stopCommand(
        @RequestParam azStop: Boolean = false,
        @RequestParam elStop: Boolean = false,
        @RequestParam tiStop: Boolean = false,
    ): Mono<String> {
        val bitStop = BitSet()
        if (azStop) bitStop.set(0)
        if (elStop) bitStop.set(1)
        if (tiStop) bitStop.set(2)

        val bitStr = listOfNotNull(
            if (azStop) "Azimuth Stop" else null,
            if (elStop) "Elevation Stop" else null,
            if (tiStop) "Tilt Stop" else null,
        ).joinToString(",")

        return Mono.fromCallable {
            udpFwICDService.stopCommand(bitStop)
            "UDP 명령어 전송 요청 완료 (Command: $bitStr, $azStop, $elStop,$tiStop)"
        }.thenReturn("stopCommand UDP 명령어 전송 요청 완료 (Command: $bitStr, $azStop, $elStop,$tiStop)")
    }

    @PostMapping("/default-info-command")
    fun defaultInfoCommand(
        @RequestParam timeOffset: Float,
        @RequestParam azOffset: Float,
        @RequestParam elOffset: Float,
        @RequestParam tiOffest: Float
    ): Mono<String> {
        return Mono.fromCallable {
            udpFwICDService.defaultInfoCommand(timeOffset, azOffset, elOffset, tiOffest)
            "UDP 명령어 전송 요청 완료 (Command: $timeOffset, $azOffset, $elOffset, $tiOffest)"
        }.thenReturn("defaultInfoCommand UDP 명령어 전송 요청 완료 (Command: $timeOffset, $azOffset, $elOffset, $tiOffest)")
    }

    // 새로운 StowCommand 엔드포인트
    @PostMapping("/stow-command")
    fun stowCommand(): Mono<String> {
        return Mono.fromCallable {
            udpFwICDService.StowCommand()
            "Stow 명령 시작됨"
        }.thenReturn("StowCommand UDP 명령어 전송 요청 완료")
    }
}
