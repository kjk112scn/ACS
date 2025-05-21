package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.GlobalData.Time.calUtcTimeOffsetTime
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.util.JKUtil
import com.gtlsystems.acs_api.util.JKUtil.JKConvert.Companion.byteArrayToHexString
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.time.Duration
import java.util.BitSet

@Service
class UdpFwICDService(
    private val pushService: PushService,
    private val environment: Environment,
    private val eventBus: ACSEventBus // 이벤트 버스 주입
) {

    private val icdService = ICDService.Classify(pushService,eventBus)

    private lateinit var channel: DatagramChannel
    private val receiveBuffer = ByteBuffer.allocate(512)

    private var readData: PushData.ReadData = PushData.ReadData()

    // Stow Command 실행 중인지 추적하기 위한 변수
    private var stowCommandDisposable: Disposable? = null

    // 프로퍼티 값을 생성자에서 주입받는 대신 @Value 어노테이션 사용
    @Value("\${firmware.udp.ip:127.0.0.1}")
    private lateinit var firmwareIp: String

    @Value("\${firmware.udp.port:8080}")
    private var firmwarePort: Int = 0

    @Value("\${server.udp.ip:127.0.0.1}")
    private lateinit var serverIp: String

    @Value("\${server.udp.port:8081}")
    private var serverPort: Int = 0

    // 초기화 전에는 임시 주소 사용
    var firmwareAddress = InetSocketAddress("127.0.0.1", 8080)

    // 최신 데이터를 저장할 변수
    @PostConstruct
    fun init() {
        initializeUdpChannel()
    }
    private fun initializeUdpChannel() {
        try {
            firmwareIp = environment.getProperty("firmware.udp.ip") ?: "127.0.0.1"
            firmwarePort = environment.getProperty("firmware.udp.port")?.toInt() ?: 8080
            serverIp = environment.getProperty("server.udp.ip") ?: "127.0.0.1"
            serverPort = environment.getProperty("server.udp.port")?.toInt() ?: 8081
            firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)

            // 단일 채널 설정 (송수신 모두 사용)
            channel = DatagramChannel.open()
            val serverAddress = InetSocketAddress(serverIp, serverPort)
            channel.bind(serverAddress)
            channel.configureBlocking(false)

            println("UDP 채널 시작: $serverIp:$serverPort (포트: ${channel.localAddress})")

            // 주기적인 수신 및 송신 시작
            startReceivingDataThread()
            startSendingCommandPeriodically()
        } catch (e: Exception) {
            println("UDP 초기화 실패: ${e.message}, 5초 후 재시도합니다.")
            scheduleReconnection()
        }
    }
    /**
     * 보드와 통신이 되지 않는다면 지정된 시간 후 재연결 시도
     */
    private fun scheduleReconnection() {
        Mono.delay(Duration.ofSeconds(5))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe {
                println("UDP 연결 재시도 중...")
                initializeUdpChannel()
            }
    }

    /**
     * 주기적으로 데이터를 송신하기 위한  설정 함수
     */
    private fun startSendingCommandPeriodically() {
        Flux.interval(Duration.ofMillis(10))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe { sendReadStatusCommand() }
    }

    /**
     * Read Status 정보 요청을 위한 송신 부 로직
     */
    private fun sendReadStatusCommand() {
        try {
            val setDataFrameInstance = ICDService.ReadStatus.SetDataFrame()
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            //println("UDP ReadStatus 명령어 전송: $firmwareIp:$firmwarePort")
            //println("UDP Send Data : ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            //println("send 에러 ${e.message}")
        }
    }

    /**
     * 실시간을 유지하기 위해 쓰레드 실행
     * Thread.sleep(1)을 사용할 수도 있으나 제일 중요한 부분이라 우선 없음.
     * 다른 부분에는 쓰레드 사용하지 않는 것을 원칙으로 함.
     */
    private fun startReceivingDataThread() {
        Thread {
            while (true) {
                receiveBuffer.clear()
                val address = channel.receive(receiveBuffer)
                if (address != null) {
                    receiveBuffer.flip()
                    val receivedData = ByteArray(receiveBuffer.remaining())
                    receiveBuffer.get(receivedData)
                    processICDData(receivedData)
                }
                // 필요시 Thread.sleep(1) 등으로 CPU 사용 조절
            }
        }.start()
    }

    /**
     * 수신 데이터를 처리하는 부분
     */
    private fun processICDData(receivedData: ByteArray) {
        try {
            icdService.receivedCmd(receivedData)
            // ICDService에서 처리한 최신 데이터를 가져와 readData 업데이트
            val latestData = pushService.getLatestData()
            if (latestData != null) {
                readData = latestData
            }
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류: ${e.message}")
        }
    }


    /**
     * 12.1 위성 추적 헤더 정보 전송
     */
    fun sendSatelliteTrackHeader(headerFrame: ICDService.SatelliteTrackOne.SetDataFrame) {
        try {
            val dataToSend = headerFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            println("UDP 위성 추적 헤더 정보 전송: $firmwareIp:$firmwarePort")
            println("UDP Send Data: ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("위성 추적 헤더 정보 전송 오류: ${e.message}")
            throw e
        }
    }

    /**
     * 12.2 위성 추적 초기 제어 명령 전송
     */
    fun sendSatelliteTrackInitialControl(controlFrame: ICDService.SatelliteTrackTwo.SetDataFrame) {
        try {
            val dataToSend = controlFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            println("UDP 위성 추적 초기 제어 명령 전송: $firmwareIp:$firmwarePort")
            println("UDP Send Data: ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("위성 추적 초기 제어 명령 전송 오류: ${e.message}")
            throw e
        }
    }

    /**
     * 12.3 위성 추적 추가 데이터 전송
     */
    fun sendSatelliteTrackAdditionalData(dataFrame: ICDService.SatelliteTrackThree.SetDataFrame) {
        try {
            val dataToSend = dataFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            println("UDP 위성 추적 추가 데이터 전송: $firmwareIp:$firmwarePort")
            println("UDP Send Data: ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("위성 추적 추가 데이터 전송 오류: ${e.message}")
            throw e
        }
    }

    // Emergency Command 전송 함수 - 수정된 버전
    fun onEmergencyCommand(commandChar: Char) {
        var cmdOnOffValue = false
        try {
            if (commandChar == 'E') {
                cmdOnOffValue = true
                println("비상 모드 활성화됨")
            } else if (commandChar == 'S') {
                cmdOnOffValue = false
                println("비상 모드 비활성화됨")
            } else {
                println("유효하지 않은 명령 문자: $commandChar")
                return
            }

            val setDataFrameInstance = ICDService.Emergency.SetDataFrame(
                stx = 0x02,
                cmdOne = 'E',
                cmdOnOff = cmdOnOffValue,
                crc16 = 0u,
                etx = 0x03
            )
            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            println("UDP Emergency 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - Emergency): ${byteArrayToHexString(dataToSend)}")

            // 비상 상태 변경 이벤트 발행
            //eventBus.publish(ACSEvent.EmergencyEvent(cmdOnOffValue))
        } catch (e: Exception) {
            println("비상 명령 처리 오류: ${e.message}")
            throw e  // 컨트롤러로 예외 전파
        }
    }

    // TimeOffsetCommand 전송 함수
    fun timeOffsetCommand(inputTimeOffset: Float) {
        try {
            var localTime = calUtcTimeOffsetTime
            val setDataFrameInstance = ICDService.TimeOffset.SetDataFrame(
                stx = 0x02,
                cmdOne = 'O',
                cmdTwo = 'T',
                year = localTime.year.toUShort(),
                month = localTime.month.value.toByte(),
                day = localTime.dayOfMonth.toByte(),
                hour = localTime.hour.toByte(),
                minute = localTime.minute.toByte(),
                second = localTime.second.toByte(),
                ms = (localTime.nano / 1000000).toUShort(),
                timeOffset = inputTimeOffset,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            GlobalData.Offset.TimeOffset = inputTimeOffset
            println("UDP TimeOffset 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - timeOffsetCommand): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (timeOffsetCommand): ${e.message}")
        }
    }

    fun multiManualCommand(
        multiAxis: BitSet,
        azAngle: Float, azSpeed: Float,
        elAngle: Float, elSpeed: Float,
        tiAngle: Float, tiSpeed: Float
    ) {
        try {
            val setDataFrameInstance = ICDService.MultiManualControl.SetDataFrame(
                stx = 0x02,
                cmdOne = 'A',
                axis = multiAxis,
                azimuthAngle = azAngle,
                azimuthSpeed = azSpeed,
                elevationAngle = elAngle,
                elevationSpeed = elSpeed,
                tiltAngle = tiAngle,
                tiltSpeed = tiSpeed,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (Emergency): ${e.message}")
        }
    }


    fun StowCommand() {
        // 이미 실행 중인 Stow Command가 있다면 취소
        stowCommandDisposable?.dispose()
        stopAllCommand();
        // Stow 명령에 사용할 기본값 설정
        val stowTiltAngle = 0.0f  // 틸트 각도 0도
        val stowTiltSpeed = 5.0f  // 틸트 속도

        val stowAzimuthAngle = 0.0f  // 방위각 0도
        val stowAzimuthSpeed = 5.0f  // 방위각 속도

        val stowElevationAngle = 90.0f  // 고도각 90도 (천정 방향)
        val stowElevationSpeed = 5.0f  // 고도각 속도

        // 먼저 틸트 축 제어 시작
        val tiltAxis = BitSet()
        tiltAxis.set(2)  // 틸트 축 설정
        tiltAxis.set(7)  // STOW 비트 설정

        stowTiltCommand(tiltAxis, stowTiltAngle, stowTiltSpeed)
        println("Stow 명령 시작: 틸트 축 제어 중 (목표 각도: $stowTiltAngle)")

        // 안정화 시간 추적을 위한 변수
        val stableTimeTracker = StableTimeTracker()

        stowCommandDisposable = Flux.interval(Duration.ofMillis(100))
            .takeUntil {
                // 현재 틸트 각도 확인
                val currentTiltAngle = readData.tiltAngle ?: 0.0f
                val isInTargetRange = Math.abs(currentTiltAngle - stowTiltAngle) <= 0.1f

                val currentTime = System.currentTimeMillis()

                if (isInTargetRange) {
                    if (stableTimeTracker.startTime == null) {
                        // 처음으로 목표 범위에 들어옴
                        stableTimeTracker.startTime = currentTime
                        println("틸트 축이 목표 범위에 진입 (현재 각도: $currentTiltAngle)")
                        false
                    } else {
                        // 목표 범위에 1초 이상 머물렀는지 확인
                        val stableTime = currentTime - stableTimeTracker.startTime!!
                        if (stableTime >= 1000) {
                            // 1초 이상 안정적으로 유지됨 - 방위각/고도각 제어 시작
                            val azElAxis = BitSet()
                            azElAxis.set(0)  // 방위각 축 설정
                            azElAxis.set(1)  // 고도각 축 설정
                            azElAxis.set(7)  // STOW 비트 설정

                            stowAzElCommand(
                                azElAxis,
                                stowAzimuthAngle, stowAzimuthSpeed,
                                stowElevationAngle, stowElevationSpeed
                            )
                            println("틸트 축이 목표 위치에 안정적으로 도달하여 방위각/고도각 제어를 시작합니다.")
                            true  // 스트림 종료
                        } else {
                            false
                        }
                    }
                } else {
                    // 목표 범위를 벗어남 - 타이머 리셋
                    if (stableTimeTracker.startTime != null) {
                        println("틸트 축이 목표 범위를 벗어남 (현재 각도: $currentTiltAngle)")
                        stableTimeTracker.startTime = null
                    }
                    false
                }
            }
            .subscribe(
                { /* 각 간격마다 실행되는 코드 */ },
                { error -> println("Stow 명령 실행 중 오류 발생: ${error.message}") },
                { println("Stow 명령 실행 완료") }
            )
    }

    // 안정화 시간을 추적하기 위한 클래스
    private class StableTimeTracker {
        var startTime: Long? = null
    }

    private fun stowTiltCommand(
        multiAxis: BitSet,
        tiAngle: Float,
        tiSpeed: Float
    ) {
        try {
            val setDataFrameInstance = ICDService.MultiManualControl.SetDataFrame(
                stx = 0x02,
                cmdOne = 'A',
                axis = multiAxis,
                azimuthAngle = readData.azimuthAngle ?: 0.0f,
                azimuthSpeed = 0.0f,  // 틸트만 제어하므로 다른 축 속도는 0
                elevationAngle = readData.elevationAngle ?: 0.0f,
                elevationSpeed = 0.0f,  // 틸트만 제어하므로 다른 축 속도는 0
                tiltAngle = tiAngle,
                tiltSpeed = tiSpeed,
                crc16 = 0u,
                etx = 0x03
            )
            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
        } catch (e: Exception) {
            println("틸트 축 제어 명령 처리 오류: ${e.message}")
        }
    }

    private fun stowAzElCommand(
        multiAxis: BitSet,
        azAngle: Float,
        azSpeed: Float,
        elAngle: Float,
        elSpeed: Float
    ) {
        try {
            val setDataFrameInstance = ICDService.MultiManualControl.SetDataFrame(
                stx = 0x02,
                cmdOne = 'A',
                axis = multiAxis,
                azimuthAngle = azAngle,
                azimuthSpeed = azSpeed,
                elevationAngle = elAngle,
                elevationSpeed = elSpeed,
                tiltAngle = readData.tiltAngle ?: 0.0f,
                tiltSpeed = 0.0f,  // 방위각/고도각만 제어하므로 틸트 속도는 0
                crc16 = 0u,
                etx = 0x03
            )
            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
        } catch (e: Exception) {
            println("방위각/고도각 제어 명령 처리 오류: ${e.message}")
        }
    }

    fun feedOnOffCommand(bitFeedOnOff: BitSet) {
        try {
            val setDataFrameInstance = ICDService.FeedOnOff.SetDataFrame(
                stx = 0x02,
                cmdOne = 'F',
                feedOnOff = bitFeedOnOff,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            println("UDP feedOnOffCommand 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - feedOnOffCommand): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (Emergency): ${e.message}")
        }
    }

    fun positionOffsetCommand(azOffset: Float, elOffset: Float, tiOffset: Float) {
        try {
            val setDataFrameInstance = ICDService.PositionOffset.SetDataFrame(
                stx = 0x02,
                cmdOne = 'O',
                cmdTwo = 'P',
                azimuthOffset = azOffset,
                elevationOffset = elOffset,
                tiltOffset = tiOffset,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            GlobalData.Offset.azimuthPositionOffset = azOffset
            GlobalData.Offset.elevationPositionOffset = elOffset
            GlobalData.Offset.tiltPositionOffset = tiOffset
            println("UDP positionOffsetCommand 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - positionOffsetCommand): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (Emergency): ${e.message}")
        }
    }

    fun stopCommand(bitStop: BitSet) {
        try {
            // 1. StowCommand가 실행 중이면 중단
            if (stowCommandDisposable != null && !stowCommandDisposable!!.isDisposed) {
                stowCommandDisposable!!.dispose()
                stowCommandDisposable = null
                // 필요하다면 로그 남기기
                println("StowCommand 중단됨: stop 명령 실행")
            }
            val setDataFrameInstance = ICDService.Stop.SetDataFrame(
                stx = 0x02,
                cmdOne = 'S',
                axis = bitStop,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            stopAllCommand()
            println("UDP stopCommand 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - stopCommand): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (stopCommand): ${e.message}")
        }
    }
    fun servoPresetCommand(bitStop: BitSet) {
        try {
            // 1. StowCommand가 실행 중이면 중단
            if (stowCommandDisposable != null && !stowCommandDisposable!!.isDisposed) {
                stowCommandDisposable!!.dispose()
                stowCommandDisposable = null
                // 필요하다면 로그 남기기
                println("servoPresetCommand 중단됨: servoPresetCommand 명령 실행")
            }
            val setDataFrameInstance = ICDService.ServoEncoderPreset.SetDataFrame(
                stx = 0x02,
                cmdOne = 'P',
                cmdTwo ='P',
                axis = bitStop,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            println("UDP servoPresetCommand 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - servoPresetCommand): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (servoPresetCommand): ${e.message}")
        }
    }
    /*
    Sun Track, Ephemeris Designation, Pass Schedule을 Stop 버튼을 선택 시 정지하기 위해 작성.
     */
    fun stopAllCommand() {
        try {
            //태양 추적 중지.
            //sunTrackService.stopSunTrackCommandPeriodically()
            eventBus.publish(ACSEvent.TrackingEvent.StopAllTracking)
            println("모든 추적 중지 이벤트 발행됨")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (stopAllCommand): ${e.message}")
        }
    }
    fun defaultInfoCommand(timeOffset: Float, azOffset: Float, elOffset: Float, tiOffset: Float) {
        try {
            var localTime = JKUtil.JKTime.calLocalTime
            val setDataFrameInstance = ICDService.DefaultInfo.SetDataFrame(
                cmd = 'W',
                year = localTime.year.toUShort(),
                month = localTime.month.value.toByte(),
                day = localTime.dayOfMonth.toByte(),
                hour = localTime.hour.toByte(),
                minute = localTime.minute.toByte(),
                second = localTime.second.toByte(),
                ms = (localTime.nano / 1000000).toUShort(),
                timeOffset = timeOffset,
                azimuthOffset = azOffset,
                elevationOffset = elOffset,
                tiltOffset = tiOffset,
                crc16 = 0u,
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            println("UDP defaultInfoCommand 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - defaultInfoCommand): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (defaultInfoCommand): ${e.message}")
        }
    }

    @PreDestroy
    fun stop() {
        if (::channel.isInitialized && channel.isOpen) {
            channel.close()
            println("UDP 채널 닫힘")
        }
    }
}
