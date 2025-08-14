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

        // POST 요청 처리 (Sun Track Start UDP 명령 전송) - APIController에서 이동
        @PostMapping("/start-sun-track")
        @Operation(
            summary = "태양 추적 시작",
            description = """
                태양 추적을 시작하고 UDP 명령을 전송합니다.
                
                ## 기능 설명
                - **태양 추적**: 태양의 현재 위치를 계산하여 안테나 제어
                - **UDP 명령**: 외부 시스템에 태양 추적 시작 명령 전송
                - **속도 제어**: 각 축별 이동 속도 설정
                - **실시간 추적**: 태양 위치 변화에 따른 실시간 안테나 제어
                
                ## 입력 파라미터
                - **interval**: 추적 간격 (밀리초 단위)
                - **cmdAzimuthSpeed**: 방위각 축 이동 속도 (도/초)
                - **cmdElevationSpeed**: 고도각 축 이동 속도 (도/초)
                - **cmdTiltSpeed**: 기울기 축 이동 속도 (도/초)
                
                ## 태양 추적 원리
                - **천문학적 계산**: 태양의 적경/적위 계산
                - **좌표 변환**: 천구 좌표를 안테나 좌표로 변환
                - **각도 계산**: 방위각, 고도각, 기울기각 계산
                - **제어 명령**: 계산된 각도로 안테나 제어
                
                ## 사용 예시
                ```
                POST /api/sun-track/start-sun-track?interval=1000&cmdAzimuthSpeed=0.5&cmdElevationSpeed=0.3&cmdTiltSpeed=0.1
                ```
                
                ## 응답 예시
                ```
                Sun Track UDP 명령어 전송 요청 완료 (Command: 0.5, 0.3, 0.1)
                ```
            """,
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
            @RequestParam cmdTiltSpeed: Float
        ): Mono<String> {
            return Mono.fromCallable {
                sunTrackService.startSunTrack() // ✅ 새로운 메서드명 사용
                "UDP 명령어 전송 요청 완료 (Command: Sun Track)"
            }.thenReturn("Sun Track UDP 명령어 전송 요청 완료 (Command:  $cmdAzimuthSpeed, $cmdElevationSpeed, $cmdTiltSpeed)")
        }
        // POST 요청 처리 (Sun Track Stop UDP 명령 전송) - APIController에서 이동
        @PostMapping("/stop-sun-track")
        @Operation(
            summary = "태양 추적 중지",
            description = """
                태양 추적을 중지하고 UDP 명령을 전송합니다.
                
                ## 기능 설명
                - **추적 중지**: 태양 추적 동작을 안전하게 중지
                - **UDP 명령**: 외부 시스템에 태양 추적 중지 명령 전송
                - **안전 정지**: 안테나를 현재 위치에 고정
                - **리소스 정리**: 추적 관련 리소스 정리
                
                ## 처리 과정
                1. **추적 중지**: SunTrackService의 추적 동작 중지
                2. **UDP 전송**: 외부 시스템에 중지 명령 전송
                3. **안테나 고정**: 현재 위치에 안테나 고정
                4. **상태 반환**: 중지 완료 메시지 반환
                
                ## 사용 예시
                ```
                POST /api/sun-track/stop-sun-track
                ```
                
                ## 응답 예시
                ```
                Sun Track UDP 명령어 전송 요청 완료 (Command:)
                ```
                
                ## 주의사항
                - 추적 중지 시 안테나가 현재 위치에 고정됩니다
                - 긴급 상황이 아닌 경우 안전한 중지를 권장합니다
            """,
            tags = ["Mode - Sun Track"]
        )
        fun stopSunTrack(): Mono<String> {
            return Mono.fromCallable {
                sunTrackService.stopSunTrack() // ✅ 새로운 메서드명 사용
                "UDP 명령어 전송 요청 완료 (Command: Sun Track)"
            }.thenReturn("Sun Track UDP 명령어 전송 요청 완료 (Command:)")
    }
}