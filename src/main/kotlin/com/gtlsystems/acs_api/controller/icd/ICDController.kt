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

@RestController
@RequestMapping("/api/icd")
@Tag(name = "ICD - Communication", description = "외부 시스템과의 ICD 통신 API - 서보 제어, 비상 정지, 통신 상태 모니터링")
class ICDController(private val udpFwICDService: UdpFwICDService) {

    private val logger = LoggerFactory.getLogger(ICDController::class.java)

    @PostMapping("/servo-preset-command")
    @Operation(
        summary = "서보 프리셋 명령",
        description = """
            안테나 서보 시스템의 프리셋 명령을 전송합니다.
            
            ## 기능 설명
            - **프리셋 설정**: 지정된 축의 서보를 프리셋 위치로 이동
            - **축별 제어**: 방위각, 고도각, 기울기 축을 개별적으로 제어 가능
            - **UDP 통신**: 외부 시스템과의 UDP 프로토콜을 통한 명령 전송
            
            ## 입력 파라미터
            - **azimuth**: 방위각 축 프리셋 여부 (기본값: false)
            - **elevation**: 고도각 축 프리셋 여부 (기본값: false)
            - **tilt**: 기울기 축 프리셋 여부 (기본값: false)
            
            ## 명령 처리
            - **비트맵 생성**: 각 축의 상태를 비트맵으로 변환
            - **UDP 전송**: UdpFwICDService를 통한 명령 전송
            - **응답 생성**: 명령 처리 결과 및 상태 정보 반환
            
            ## 사용 예시
            ```
            POST /api/icd/servo-preset-command?azimuth=true&elevation=true
            ```
            
            ## 응답 예시
            ```json
            {
              "status": "success",
              "message": "ServoPreset 명령이 성공적으로 전송되었습니다",
              "command": "ServoPreset",
              "axes": "AZIMUTH,ELEVATION"
            }
            ```
        """,
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
    @Operation(
        summary = "대기 모드 명령",
        description = """
            안테나 서보 시스템을 대기 모드로 전환하는 명령을 전송합니다.
            
            ## 기능 설명
            - **대기 모드**: 지정된 축의 서보를 안전한 대기 상태로 전환
            - **축별 제어**: 방위각, 고도각, 기울기 축을 개별적으로 제어 가능
            - **안전 기능**: 시스템을 안전한 대기 상태로 유지
            
            ## 입력 파라미터
            - **azStandby**: 방위각 축 대기 모드 여부 (기본값: false)
            - **elStandby**: 고도각 축 대기 모드 여부 (기본값: false)
            - **tiStandby**: 기울기 축 대기 모드 여부 (기본값: false)
            
            ## 명령 처리
            - **비트맵 생성**: 각 축의 대기 상태를 비트맵으로 변환
            - **UDP 전송**: UdpFwICDService를 통한 명령 전송
            - **응답 생성**: 명령 처리 결과 및 상태 정보 반환
            
            ## 사용 예시
            ```
            POST /api/icd/standby-command?azStandby=true&elStandby=true
            ```
            
            ## 응답 예시
            ```json
            {
              "status": "success",
              "message": "Standby 명령이 성공적으로 전송되었습니다",
              "command": "Standby",
              "axes": "AZIMUTH,ELEVATION"
            }
            ```
        """,
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
    @Operation(
        summary = "비상 정지 명령",
        description = """
            안테나 시스템에 비상 정지 명령을 전송합니다.
            
            ## 기능 설명
            - **비상 정지**: 시스템을 즉시 안전한 상태로 정지
            - **명령 타입**: 비상 정지(E) 또는 안전 정지(S) 구분
            - **즉시 실행**: 명령 수신 즉시 모든 동작 중단
            
            ## 입력 파라미터
            - **commandType**: 비상 정지 명령 타입
              - **'E'**: Emergency Stop (비상 정지)
              - **'S'**: Safe Stop (안전 정지)
            
            ## 명령 처리
            - **타입 검증**: 'E' 또는 'S' 값 검증
            - **UDP 전송**: UdpFwICDService를 통한 명령 전송
            - **응답 생성**: 명령 처리 결과 및 상태 정보 반환
            
            ## 안전 고려사항
            - **비상 정지(E)**: 모든 동작을 즉시 중단, 안전 장치 작동
            - **안전 정지(S)**: 안전한 순서로 동작을 중단, 데이터 보존
            
            ## 사용 예시
            ```
            POST /api/icd/on-emergency-stop-command?commandType=E
            ```
            
            ## 응답 예시
            ```json
            {
              "status": "success",
              "message": "Emergency Stop 명령이 성공적으로 전송되었습니다",
              "command": "Emergency Stop",
              "commandType": "E"
            }
            ```
        """,
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
    @Operation(
        summary = "통신 상태 확인",
        description = """
            외부 시스템과의 UDP 통신 상태를 확인합니다.
            
            ## 기능 설명
            - **통신 상태**: UDP 연결의 건강성 및 성능 상태 확인
            - **성능 통계**: 패킷 전송/수신 통계, 지연 시간 등
            - **연결 상태**: 외부 시스템과의 연결 상태 모니터링
            
            ## 제공 정보
            - **healthy**: 통신 연결 상태 (true/false)
            - **statistics**: UDP 성능 통계 정보
            - **timestamp**: 상태 확인 시간 (Unix timestamp)
            
            ## 성능 통계 항목
            - **패킷 전송**: 성공/실패 패킷 수
            - **지연 시간**: 평균/최대/최소 지연 시간
            - **연결 상태**: 연결 유지 시간, 재연결 횟수
            - **오류 정보**: 통신 오류 발생 횟수 및 유형
            
            ## 사용 예시
            ```
            POST /api/icd/communication-status
            ```
            
            ## 응답 예시
            ```json
            {
              "status": "success",
              "healthy": true,
              "statistics": {
                "packetsSent": 1000,
                "packetsReceived": 998,
                "averageLatency": 5.2,
                "connectionUptime": 3600
              },
              "timestamp": 1691928000000
            }
            ```
        """,
        tags = ["ICD - Communication"]
    )
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