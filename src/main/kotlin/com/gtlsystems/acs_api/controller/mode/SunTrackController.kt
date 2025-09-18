package com.gtlsystems.acs_api.controller.mode

import com.gtlsystems.acs_api.service.mode.SunTrackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/sun-track")
@Tag(name = "Mode - Sun Track", description = "태양 추적 API - 태양 위치 계산, 추적 시작/중지, UDP 명령 전송")
class SunTrackController(private val sunTrackService: SunTrackService) {

    @PostMapping("/start-sun-track")
    @Operation(
        operationId = "startsuntrack",
        tags = ["Mode - Sun Track"]
    )
    fun startSunTrack(
        @Parameter(
            description = "추적 간격 (밀리초)",
            example = "1000",
            required = true
        )
        @RequestParam interval: Long,
        @Parameter(
            description = "방위각 축 이동 속도 (도/초)",
            example = "0.5",
            required = true
        )
        @RequestParam cmdAzimuthSpeed: Float,
        @Parameter(
            description = "고도각 축 이동 속도 (도/초)",
            example = "0.3",
            required = true
        )
        @RequestParam cmdElevationSpeed: Float,
        @Parameter(
            description = "기울기 축 이동 속도 (도/초)",
            example = "0.1",
            required = true
        )
        @RequestParam cmdTrainSpeed: Float
    ): Mono<String> {
        return Mono.fromCallable {
            sunTrackService.startSunTrack(cmdAzimuthSpeed, cmdElevationSpeed, cmdTrainSpeed)
            "UDP 명령어 전송 요청 완료 (Command: Sun Track)"
        }.thenReturn("Sun Track UDP 명령어 전송 요청 완료 (Command: $cmdAzimuthSpeed, $cmdElevationSpeed, $cmdTrainSpeed)")
    }

    @PostMapping("/stop-sun-track")
    @Operation(
        operationId = "stopsuntrack",
        tags = ["Mode - Sun Track"]
    )
    fun stopSunTrack(): Mono<String> {
        return Mono.fromCallable {
            sunTrackService.stopSunTrack()
            "UDP 명령어 전송 요청 완료 (Command: Sun Track)"
        }.thenReturn("Sun Track UDP 명령어 전송 요청 완료 (Command:)")
    }
}