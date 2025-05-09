package com.gtlsystems.acs_api

import com.gtlsystems.acs_api.service.ICDService
import com.gtlsystems.acs_api.service.UdpFwICDService
import org.hipparchus.geometry.euclidean.oned.Interval
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.BitSet

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["http://localhost:9000"]) // 프론트엔드 도메인 허용
class APIController(private val udpFwICDService: UdpFwICDService) {
    // GET 요청 처리
    @GetMapping("/resource/{id}")
    fun getResource(@PathVariable id: String): Mono<String> {
        // 특정 코드를 실행하고 결과를 Mono<String> 형태로 반환
        return Mono.just("GET 요청 처리 결과: ID = $id")
    }

    // POST 요청 처리
    @PostMapping("/resource")
    fun createResource(@RequestBody requestBody: String): Mono<String> {
        // 특정 코드를 실행하고 결과를 Mono<String> 형태로 반환
        return Mono.just("POST 요청 처리 결과: 요청 본문 = $requestBody")
    }
    // 특정 API 엔드포인트를 통해 UDP 데이터 전송 (매개변수 없음)
}

@RestController
@RequestMapping("/api/sun-track") // 새로운 API 경로 설정 (선택 사항)
@CrossOrigin(origins = ["http://localhost:9000"]) // 프론트엔드 도메인 허용
class SunTracking(private val udpFwICDService: UdpFwICDService) {

    // POST 요청 처리 (Sun Track Start UDP 명령 전송) - APIController에서 이동
    @PostMapping("/start-sun-track") // 경로 변수 제거
    fun startSunTrack(@RequestParam interval: Long, cmdAzimuthSpeed: Float , cmdElevationSpeed: Float, cmdTiltSpeed: Float): Mono<String> { // 요청 파라미터 추가
        return Mono.fromCallable {
            udpFwICDService.startSunTrackCommandPeriodically(interval,cmdAzimuthSpeed, cmdElevationSpeed, cmdTiltSpeed) // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (Command: Sun Track)"
        }.thenReturn("Sun Track UDP 명령어 전송 요청 완료 (Command:  $cmdAzimuthSpeed, $cmdElevationSpeed, $cmdTiltSpeed)")
    }
    // POST 요청 처리 (Sun Track Stop UDP 명령 전송) - APIController에서 이동
    @PostMapping("/stop-sun-track") // 경로 변수 제거
    fun stopSunTrack(): Mono<String> { // 요청 파라미터 추가
        return Mono.fromCallable {
            udpFwICDService.stopSunTrackCommandPeriodically() // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (Command: Sun Track)"
        }.thenReturn("Sun Track UDP 명령어 전송 요청 완료 (Command:)")
    }
}

@RestController
@RequestMapping("/api/icd") // 새로운 API 경로 설정 (선택 사항)
@CrossOrigin(origins = ["http://localhost:9000"]) // 프론트엔드 도메인 허용
class ICD(private val udpFwICDService: UdpFwICDService) {

    // POST 요청 처리 (Emergency UDP 명령 전송) - APIController에서 이동
    @PostMapping("/on-emergency-stop-command") // 경로 변수 제거
    fun onEmergencyStopCommand(@RequestParam commandChar: Char): Mono<String> { // 요청 파라미터 추가
        return Mono.fromCallable {
            udpFwICDService.onEmergencyCommand(commandChar) // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (Command: $commandChar)"
        }.thenReturn("Emergency UDP 명령어 전송 요청 완료 (Command: $commandChar)")
    }

    @PostMapping("/time-offset-command") // 경로 변수 제거
    fun timeOffsetCommand(@RequestParam inputTimeOffset: Float): Mono<String> { // 요청 파라미터 추가
        return Mono.fromCallable {
            udpFwICDService.timeOffsetCommand(inputTimeOffset) // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (Command: $inputTimeOffset)"
        }.thenReturn("timeOffsetCommand UDP 명령어 전송 요청 완료 (Command: $inputTimeOffset)")
    }

    @PostMapping("/multi-control-command") // 경로 변수 제거
    fun multiManualControlCommand(
        @RequestParam azimuth: Boolean = false,
        @RequestParam elevation: Boolean = false,
        @RequestParam tilt: Boolean = false,
        @RequestParam stow: Boolean = false,
        azAngle: Float, azSpeed: Float,
        elAngle: Float, elSpeed: Float,
        tiAngle: Float, tiSpeed: Float
    ): Mono<String> { // 요청 파라미터 추가
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
            ) // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (multiManualControlCommand: $axesStr, $multiAxis, $azAngle,$azSpeed,$elAngle,$elSpeed,$tiAngle,$tiSpeed)"
        }
            .thenReturn("multiManualControlCommand UDP 명령어 전송 요청 완료 (Command: $multiAxis, $azAngle,$azSpeed,$elAngle,$elSpeed,$tiAngle,$tiSpeed)")
    }

    @PostMapping("/feed-on-off-command") // 경로 변수 제거
    fun feedOnOffCommand(
        @RequestParam sLHCP: Boolean = false,
        @RequestParam sRHCP: Boolean = false,
        @RequestParam sRFSwitch: Boolean = false,
        @RequestParam xLHCP: Boolean = false,
        @RequestParam xRHCP: Boolean = false,
        @RequestParam fan: Boolean = false,

        ): Mono<String> { // 요청 파라미터 추가
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
            udpFwICDService.feedOnOffCommand(
                bitFeedOnoff,

                ) // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (Command: $bitStr, $sLHCP, $sRHCP,$sRFSwitch,$xLHCP,$xRHCP,$fan)"
        }
            .thenReturn("feedOnOffCommand UDP 명령어 전송 요청 완료 (Command: $bitStr, $sLHCP, $sRHCP,$sRFSwitch,$xLHCP,$xRHCP,$fan)")
    }

    @PostMapping("/position-offset-command") // 경로 변수 제거
    fun positionOffsetCommand(
        azOffset: Float, elOffset: Float, tiOffest: Float
    ): Mono<String> { // 요청 파라미터 추가
        return Mono.fromCallable {
            udpFwICDService.positionOffsetCommand(
                azOffset, elOffset, tiOffest
            ) // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (Command: $azOffset, $elOffset, $tiOffest)"
        }
            .thenReturn("positionOffsetCommand UDP 명령어 전송 요청 완료 (Command: $azOffset, $elOffset, $tiOffest)")
    }

    @PostMapping("/stop-command") // 경로 변수 제거
    fun stopCommand(
        @RequestParam azStop: Boolean = false,
        @RequestParam elStop: Boolean = false,
        @RequestParam tiStop: Boolean = false,
    ): Mono<String> { // 요청 파라미터 추가
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
            udpFwICDService.stopCommand(
                bitStop
            ) // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (Command: $bitStr, $azStop, $elStop,$tiStop)"
        }
            .thenReturn("stopCommand UDP 명령어 전송 요청 완료 (Command: $bitStr, $azStop, $elStop,$tiStop)")
    }
    @PostMapping("/default-info-command") // 경로 변수 제거
    fun defaultOInfoCommand(
        timeOffset: Float, azOffset: Float, elOffset: Float, tiOffest: Float
    ): Mono<String> { // 요청 파라미터 추가
        return Mono.fromCallable {
            udpFwICDService.defaultInfoCommand(
               timeOffset, azOffset, elOffset, tiOffest
            ) // 서비스 함수에 변수 전달
            "UDP 명령어 전송 요청 완료 (Command: $timeOffset, $azOffset, $elOffset, $tiOffest)"
        }
            .thenReturn("defaultOInfoCommand UDP 명령어 전송 요청 완료 (Command: $timeOffset, $azOffset, $elOffset, $tiOffest)")
    }
    // 새로운 StowCommand 엔드포인트
    @PostMapping("/stow-command")
    fun stowCommand(): Mono<String> {
        return Mono.fromCallable {
            udpFwICDService.StowCommand()
            "Stow 명령 시작됨"
        }
            .thenReturn("StowCommand UDP 명령어 전송 요청 완료")
    }
}
