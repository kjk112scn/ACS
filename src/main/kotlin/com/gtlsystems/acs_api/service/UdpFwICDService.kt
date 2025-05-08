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
import java.time.ZonedDateTime
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
    private val receiveBuffer = ByteBuffer.allocate(512)
    val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)

    // 주기적 작업을 관리하기 위한 Disposable 객체 저장 변수
    private var sunTrackCommandSubscription: Disposable? = null

    @PostConstruct
    fun init() {
        try {
            // 단일 채널 설정 (송수신 모두 사용)
            channel = DatagramChannel.open()
            val serverAddress = InetSocketAddress(serverIp, serverPort)
            channel.bind(serverAddress)
            channel.configureBlocking(false)

            println("UDP 채널 시작: $serverIp:$serverPort (포트: ${channel.localAddress})")

            // 주기적인 수신 및 송신 시작
            startReceivingDataPeriodically()
            startSendingCommandPeriodically()
        } catch (e: Exception) {
            println("UDP 초기화 실패: ${e.message}")
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
                println("태양 추적 명령 주기적 전송 중지됨")
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
        Flux.interval(Duration.ofMillis(100))
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
                GlobalData.Time.resultTimeOffsetCalTime,
                GlobalData.Location.latitude,
                GlobalData.Location.longitude
            )
            CMD.apply {
                cmdAzimuthAngle = sunTrackData.azimuth
                cmdElevationAngle = sunTrackData.elevation
                cmdTiltAngle = 0.0f  // 기본값 또는 필요에 따라 설정
                cmdTime = ZonedDateTime.now()
            }
            //val cmdAzimuth = CMD.cmdAzimuthAngle
            //val cmdElevationAngle = CMD.cmdElevationAngle
            val cmdTiltAngle = CMD.cmdTiltAngle
            //val cmdTime = CMD.cmdTime
            //print("azimuthAngle: $cmdAzimuth, elevationAngle: $cmdElevationAngle, times: $cmdTime")
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
            println("UDP positionOffsetCommand 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - positionOffsetCommand): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (Emergency): ${e.message}")
        }
    }

    fun stopCommand(bitStop: BitSet) {
        try {
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
            println("UDP stopCommand 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - stopCommand): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (stopCommand): ${e.message}")
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
