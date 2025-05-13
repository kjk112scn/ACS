package com.gtlsystems.acs_api.controller

import com.gtlsystems.acs_api.service.SunTrackService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["http://localhost:9000"]) // 프론트엔드 도메인 허용
class SunTrackController {
    @RestController
    @RequestMapping("/api/sun-track") // 새로운 API 경로 설정 (선택 사항)
    @CrossOrigin(origins = ["http://localhost:9000"]) // 프론트엔드 도메인 허용
    class SunTracking(private val sunTrackService: SunTrackService) {

        // POST 요청 처리 (Sun Track Start UDP 명령 전송) - APIController에서 이동
        @PostMapping("/start-sun-track") // 경로 변수 제거
        fun startSunTrack(@RequestParam interval: Long, cmdAzimuthSpeed: Float , cmdElevationSpeed: Float, cmdTiltSpeed: Float): Mono<String> { // 요청 파라미터 추가
            return Mono.fromCallable {
                sunTrackService.startSunTrackCommandPeriodically(interval,cmdAzimuthSpeed, cmdElevationSpeed, cmdTiltSpeed) // 서비스 함수에 변수 전달
                "UDP 명령어 전송 요청 완료 (Command: Sun Track)"
            }.thenReturn("Sun Track UDP 명령어 전송 요청 완료 (Command:  $cmdAzimuthSpeed, $cmdElevationSpeed, $cmdTiltSpeed)")
        }
        // POST 요청 처리 (Sun Track Stop UDP 명령 전송) - APIController에서 이동
        @PostMapping("/stop-sun-track") // 경로 변수 제거
        fun stopSunTrack(): Mono<String> { // 요청 파라미터 추가
            return Mono.fromCallable {
                sunTrackService.stopSunTrackCommandPeriodically() // 서비스 함수에 변수 전달
                "UDP 명령어 전송 요청 완료 (Command: Sun Track)"
            }.thenReturn("Sun Track UDP 명령어 전송 요청 완료 (Command:)")
        }
    }
}