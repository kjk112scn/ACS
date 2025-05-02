package com.gtlsystems.acs_api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.model.PushReadStatusData
import com.gtlsystems.acs_api.util.JKUtil
import com.gtlsystems.acs_api.util.JKUtil.JKConvert.Companion.byteArrayToHexString
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.time.LocalDateTime
import java.time.Duration
import java.util.BitSet

@Service
class UdpFwICDService(
    private val objectMapper: ObjectMapper,
    private val pushReadStatusService: PushReadStatusService,
    @Value("\${firmware.udp.ip}") private val firmwareIp: String,
    @Value("\${firmware.udp.port}") private val firmwarePort: Int,
    @Value("\${server.udp.ip}") private val serverIp: String,
    @Value("\${server.udp.port}") private val serverPort: Int
) {

    private val icdService = ICDService.Classify(objectMapper,pushReadStatusService)
    private lateinit var channel: DatagramChannel
    private val receiveBuffer = ByteBuffer.allocate(256)
    val firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)

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

    // 송신 부 반복 수행
    private fun startSendingCommandPeriodically() {
        Flux.interval(Duration.ofMillis(50))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe { sendReadStatusCommand() }
    }

    // Read Status 송신 부 로직
    private fun sendReadStatusCommand() {
        try {
            val setDataFrameInstance = ICDService.ReadStatus.SetDataFrame()
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            println("UDP ReadStatus 명령어 전송: $firmwareIp:$firmwarePort")
            println("UDP Send Data : ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("send 에러 ${e.message}")
        }
    }

    // 수신 부 반복 수행
    private fun startReceivingDataPeriodically() {
        Flux.interval(Duration.ofMillis(25))
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
                        println("UDP 데이터 수신: $dataString from $clientAddress")
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
/*

    // Front로 데이터 푸쉬 로직
    private fun processFirmwareDataAndPush(udpData: String) {
        try {
            val parts = udpData.split(",")
            if (parts.size == 2) {
                val timestamp = LocalDateTime.now().toString()
                val value = parts[1].trim().toIntOrNull()
                if (value != null) {
                    val sensorData = mapOf("timestamp" to timestamp, "value" to value)
                    val sensorDataJson = objectMapper.writeValueAsString(sensorData)
                    pushReadStatusService.publish(sensorDataJson)
                } else {
                    println("UDP 데이터 파싱 오류: 값 형식이 잘못됨 - ${parts[1].trim()}")
                }
            } else {
                println("UDP 데이터 형식 오류: 예상 형식 'timestamp,value' - $udpData")
            }
        } catch (e: Exception) {
            println("UDP 데이터 처리 오류: ${e.message}")
        }
    }
*/
/*private fun processFirmwareDataAndPush(udpData: String) {
    try {
        val parts = udpData.split(",")
        if (parts.size >= 1) { // ReadData 클래스의 필드 수 + timestamp
            val readData = PushReadStatusData.ReadData(
                modeStatusBits = BitSet.valueOf(byteArrayOf(parts[1].toByte())),
                azimuthAngle = parts[2].toFloatOrNull() ?: 0f,
                elevationAngle = parts[3].toFloatOrNull() ?: 0f,
                tiltAngle = parts[4].toFloatOrNull() ?: 0f,
                azimuthSpeed = parts[5].toFloatOrNull() ?: 0f,
                elevationSpeed = parts[6].toFloatOrNull() ?: 0f,
                tiltSpeed = parts[7].toFloatOrNull() ?: 0f,
                servoDriverAzimuthAngle = parts[8].toFloatOrNull() ?: 0f,
                servoDriverElevationAngle = parts[9].toFloatOrNull() ?: 0f,
                servoDriverTiltAngle = parts[10].toFloatOrNull() ?: 0f,
                torqueAzimuth = parts[11].toFloatOrNull() ?: 0f,
                torqueElevation = parts[12].toFloatOrNull() ?: 0f,
                torqueTilt = parts[13].toFloatOrNull() ?: 0f,
                windSpeed = parts[14].toFloatOrNull() ?: 0f,
                windDirection = parts[15].toUShortOrNull() ?: 0u,
                rtdOne = parts[16].toFloatOrNull() ?: 0f,
                rtdTwo = parts[17].toFloatOrNull() ?: 0f,
                mainBoardProtocolStatusBits = BitSet.valueOf(byteArrayOf(parts[18].toByte())),
                mainBoardStatusBits = BitSet.valueOf(byteArrayOf(parts[19].toByte())),
                mainBoardMCOnOffBits = BitSet.valueOf(byteArrayOf(parts[20].toByte())),
                mainBoardReserveBits = BitSet.valueOf(byteArrayOf(parts[21].toByte())),
                azimuthBoardServoStatusBits = BitSet.valueOf(byteArrayOf(parts[22].toByte())),
                azimuthBoardStatusBits = BitSet.valueOf(byteArrayOf(parts[23].toByte())),
                elevationBoardServoStatusBits = BitSet.valueOf(byteArrayOf(parts[24].toByte())),
                elevationBoardStatusBits = BitSet.valueOf(byteArrayOf(parts[25].toByte())),
                tiltBoardServoStatusBits = BitSet.valueOf(byteArrayOf(parts[26].toByte())),
                tiltBoardStatusBits = BitSet.valueOf(byteArrayOf(parts[27].toByte())),
                feedSBoardStatusBits = BitSet.valueOf(byteArrayOf(parts[28].toByte())),
                feedXBoardStatusBits = BitSet.valueOf(byteArrayOf(parts[29].toByte())),
                currentSBandLNA_LHCP = parts[30].toFloatOrNull() ?: 0f,
                currentSBandLNA_RHCP = parts[31].toFloatOrNull() ?: 0f,
                currentXBandLNA_LHCP = parts[32].toFloatOrNull() ?: 0f,
                currentXBandLNA_RHCP = parts[33].toFloatOrNull() ?: 0f,
                rssiSBandLNA_LHCP = parts[34].toFloatOrNull() ?: 0f,
                rssiSBandLNA_RHCP = parts[35].toFloatOrNull() ?: 0f,
                rssiXBandLNA_LHCP = parts[36].toFloatOrNull() ?: 0f,
                rssiXBandLNA_RHCP = parts[37].toFloatOrNull() ?: 0f
            )
            val sensorDataJson = objectMapper.writeValueAsString(readData)
            pushReadStatusService.publish(sensorDataJson)
        } else {
            println("UDP 데이터 형식 오류: 예상되는 데이터 필드 수(${PushReadStatusData.ReadData::class.java.declaredFields.size + 1})보다 적음 - $udpData")
        }
    } catch (e: NumberFormatException) {
        println("UDP 데이터 파싱 오류: 숫자 형식 오류 - ${e.message} - $udpData")
    } catch (e: Exception) {
        println("UDP 데이터 처리 오류: ${e.message} - $udpData")
    }
}*/

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
            println("UDP Send Data (API - Emergency): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (Emergency): ${e.message}")
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
            println("UDP TimeOffset 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - Emergency): ${byteArrayToHexString(dataToSend)}")
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
            println("UDP TimeOffset 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - Emergency): ${byteArrayToHexString(dataToSend)}")
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
            println("UDP TimeOffset 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - Emergency): ${byteArrayToHexString(dataToSend)}")
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
            println("UDP TimeOffset 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - Emergency): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (Emergency): ${e.message}")
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
            println("UDP TimeOffset 명령어 전송 (API): $firmwareIp:$firmwarePort")
            println("UDP Send Data (API - Emergency): ${byteArrayToHexString(dataToSend)}")
        } catch (e: Exception) {
            println("ICD 데이터 처리 오류 (Emergency): ${e.message}")
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
