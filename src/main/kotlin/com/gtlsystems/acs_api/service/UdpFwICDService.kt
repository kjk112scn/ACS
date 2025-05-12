package com.gtlsystems.acs_api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.model.PushData.CMD
import com.gtlsystems.acs_api.util.JKUtil
import com.gtlsystems.acs_api.util.JKUtil.JKConvert.Companion.byteArrayToHexString
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
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
    private val objectMapper: ObjectMapper,
    private val pushService: PushService,
    @Value("\${firmware.udp.ip}") private val firmwareIp: String,
    @Value("\${firmware.udp.port}") private val firmwarePort: Int,
    @Value("\${server.udp.ip}") private val serverIp: String,
    @Value("\${server.udp.port}") private val serverPort: Int
) {
    private val icdService = ICDService.Classify(objectMapper, pushService)
    private val sunTrackService = SunTrackService()
    private lateinit var channel: DatagramChannel
    private val receiveBuffer = ByteBuffer.allocate(4096)
    val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)

    private var readData: PushData.ReadData = PushData.ReadData()
    // 주기적 작업을 관리하기 위한 Disposable 객체 저장 변수
    private var sunTrackCommandSubscription: Disposable? = null
    // Stow Command 실행 중인지 추적하기 위한 변수
    private var stowCommandDisposable: Disposable? = null
    // 최신 데이터를 저장할 변수
    @PostConstruct
    fun init() {
        initializeUdpChannel()
    }

    private fun initializeUdpChannel() {
        try {
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

    private fun scheduleReconnection() {
        Mono.delay(Duration.ofSeconds(5))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe {
                println("UDP 연결 재시도 중...")
                initializeUdpChannel()
            }
    }

    /**
     * 송신 부 반복 수행 시작
     *
     * @param interval 명령 전송 간격 (밀리초)
     * @param cmdAzimuthSpeed 방위각 속도
     * @param cmdElevationSpeed 고도각 속도
     * @param cmdTiltSpeed 틸트 속도
     * @return 생성된 Disposable 객체 (중지 시 사용)
     */
    fun startSunTrackCommandPeriodically(
        interval: Long,
        cmdAzimuthSpeed: Float,
        cmdElevationSpeed: Float,
        cmdTiltSpeed: Float
    ): Disposable {
        // 이미 실행 중인 경우 중지
        stopSunTrackCommandPeriodically()

        // 새로운 주기적 작업 시작
        sunTrackCommandSubscription = Flux.interval(Duration.ofMillis(interval))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe {
                sunTrackingStartCommand(cmdAzimuthSpeed, cmdElevationSpeed, cmdTiltSpeed)
            }

        println("태양 추적 명령 주기적 전송 시작 (간격: ${interval}ms)")
        return sunTrackCommandSubscription!!
    }

    /**
     * 송신 부 반복 수행 중지
     *
     * @return 중지 성공 여부
     */
    fun stopSunTrackCommandPeriodically(): Boolean {
        return try {
            if (sunTrackCommandSubscription != null && !sunTrackCommandSubscription!!.isDisposed) {
                sunTrackCommandSubscription!!.dispose()
                sunTrackCommandSubscription = null

                // 모든 축(azimuth, elevation, tilt)을 정지시키는 BitSet 생성
                val allAxes = BitSet()
                allAxes.set(0)  // azimuth
                allAxes.set(1)  // elevation
                allAxes.set(2)  // tilt

                // 모든 축을 정지시키는 stopCommand 호출
                stopCommand(allAxes)

                println("태양 추적 명령 주기적 전송 중지됨 (모든 축 정지)")
                true
            } else {
                println("태양 추적 명령 주기적 전송이 이미 중지되었거나 실행 중이 아님")
                false
            }
        } catch (e: Exception) {
            println("태양 추적 명령 주기적 전송 중지 중 오류 발생: ${e.message}")
            false
        }
    }
    /**
     * 현재 태양 추적 명령 주기적 전송 상태 확인
     *
     * @return 실행 중이면 true, 그렇지 않으면 false
     */
    fun isSunTrackCommandRunning(): Boolean {
        return sunTrackCommandSubscription != null && !sunTrackCommandSubscription!!.isDisposed
    }

    // 송신 부 반복 수행
    private fun startSendingCommandPeriodically() {
        Flux.interval(Duration.ofMillis(25))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe { sendReadStatusCommand() }
    }

    // Read Status 송신 부 로직
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

    // 수신 부 반복 수행
    private fun startReceivingDataPeriodically() {
        Flux.interval(Duration.ofMillis(50))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap {
                Mono.fromCallable {
                    receiveBuffer.clear() // 버퍼 초기화 필수
                    try {
                        val address = channel.receive(receiveBuffer)
                        if (address != null) {
                            receiveBuffer.flip()
                            val receivedData = ByteArray(receiveBuffer.remaining())
                            receiveBuffer.get(receivedData)
                            //icdService.receivedCmd(receivedData)
                            Pair(address as InetSocketAddress, receivedData)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        println("UDP 수신 중 오류: ${e.message}")
                        null
                    }
                }
            }
            .filter { it != null }
            .subscribe(
                { pair ->
                    pair?.let { (clientAddress, receivedData) ->
                        val dataString = String(receivedData)
                        //println("UDP 데이터 수신: $dataString from $clientAddress")
                        processICDData(receivedData)
                        // processFirmwareDataAndPush(dataString)
                    }
                },
                { error ->
                    println("UDP 데이터 수신 중 오류 발생: ${error.message}")
                    error.printStackTrace()
                }
            )
    }

    // 수신 부 분류 로직
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
    fun sunTrackingStartCommand(
        cmdAzimuthSpeed: Float,
        cmdElevationSpeed: Float,
        cmdTiltSpeed: Float
    ) {
        try {
            val sunTrackData = sunTrackService.getCurrentSunPositionAPI(
                GlobalData.Time.resultSunTrackTimeOffsetCalTime,
                GlobalData.Location.latitude,
                GlobalData.Location.longitude,
                // 추가적인 위치 정보 필요한 경우 여기에 추가

            )
            val cmdTiltAngle = CMD.cmdTiltAngle
            val multiAxis = BitSet()
            multiAxis.set(0)
            multiAxis.set(1)
            multiManualCommand(
                multiAxis,
                sunTrackData.azimuth,  // null이면 0.0f 사용
                cmdAzimuthSpeed,
                sunTrackData.elevation,
                cmdElevationSpeed,
                cmdTiltAngle ?: 0.0f,
                cmdTiltSpeed
            )

        } catch (e: Exception) {
            println("SunTracking 명령어 전송 오류: ${e.message}")
        }

    }

    // Emergency Command 전송 함수
    fun onEmergencyCommand(commandChar: Char) {
        var cmdOnOffValue = false
        try {
            if (commandChar == 'E') {
                cmdOnOffValue = true
            } else if (commandChar == 'S') {
                cmdOnOffValue = false
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
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (Emergency): ${e.message}")
        }
    }

    // TimeOffsetCommand 전송 함수
    fun timeOffsetCommand(inputTimeOffset: Float) {
        try {
            var localTime = JKUtil.JKTime.calLocalTime
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
    /*
    Sun Track, Ephemeris Designation, Pass Schedule을 Stop 버튼을 선택 시 정지하기 위해 작성.
     */
    fun stopAllCommand() {
        try {
            //태양 추적 중지.
            stopSunTrackCommandPeriodically()
        }
        catch (e: Exception) {
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
