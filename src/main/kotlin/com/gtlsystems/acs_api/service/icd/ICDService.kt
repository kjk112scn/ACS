package com.gtlsystems.acs_api.service.icd

import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.model.SystemInfo
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.util.Crc16
import com.gtlsystems.acs_api.util.JKUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.BitSet

@Service
class ICDService {
    companion object {
        const val ICD_STX: Byte = 0x02
        const val ICD_ETX: Byte = 0x03
    }

    class Classify(private val dataStoreService: DataStoreService, private val acsEventBus: ACSEventBus) {
        private var lastPacketTime = System.nanoTime()
        private val logger = LoggerFactory.getLogger(Classify::class.java)

        // 패킷 타이밍 모니터링 메서드
        private fun monitorPacketTiming(data: ByteArray) {
            val now = System.nanoTime()
            val interval = (now - lastPacketTime) / 1_000_000.0 // ms로 변환
            if (interval > 80.0) { // 예상보다 지연된 경우
                 logger.warn("패킷 지연 감지: ${interval}ms")
            }
            lastPacketTime = now
        }

        fun receivedCmd(receiveData: ByteArray) {
            if (receiveData.size > 1 && receiveData[0] == 0x02.toByte()) {
                monitorPacketTiming(receiveData)
                if (receiveData[1] == 'R'.code.toByte()) {
                    //2.2 Read Status
                    if (receiveData[2] == 'R'.code.toByte()) {
                        val parsedData = ReadStatus.GetDataFrame.fromByteArray(receiveData)
                        parsedData?.let {
                            //logger.info("🔍 [ICD] elevationBoardStatusBits 수신: {}", it.elevationBoardStatusBits)
                            // 디버깅 로그 추가
                            /*logger.info("[ICD] 파싱된 각도 데이터: Azimuth={}, Elevation={}, Tilt={}",
                                it.azimuthAngle, it.elevationAngle, it.tiltAngle)*/
                            val newData = PushData.ReadData(
                                // Angle data
                                azimuthAngle = it.azimuthAngle,
                                elevationAngle = it.elevationAngle,
                                trainAngle = it.tiltAngle,

                                // Speed data
                                azimuthSpeed = it.azimuthSpeed,
                                elevationSpeed = it.elevationSpeed,
                                trainSpeed = it.tiltSpeed,

                                // Servo driver angle data
                                servoDriverAzimuthAngle = it.servoDriverAzimuthAngle,
                                servoDriverElevationAngle = it.servoDriverElevationAngle,
                                servoDriverTrainAngle = it.servoDriverTiltAngle,

                                // Torque data
                                torqueAzimuth = it.torqueAzimuth,
                                torqueElevation = it.torqueElevation,
                                torqueTrain = it.torqueTilt,

                                // Environmental data
                                windSpeed = it.windSpeed,
                                windDirection = it.windDirection,
                                rtdOne = it.rtdOne,
                                rtdTwo = it.rtdTwo,

                                // Status bits
                                modeStatusBits = it.modeStatusBits,
                                mainBoardProtocolStatusBits = it.mainBoardProtocolStatusBits,
                                mainBoardStatusBits = it.mainBoardStatusBits,
                                mainBoardMCOnOffBits = it.mainBoardMCOnOffBits,
                                mainBoardReserveBits = it.mainBoardReserveBits,
                                azimuthBoardServoStatusBits = it.azimuthBoardServoStatusBits,
                                azimuthBoardStatusBits = it.azimuthBoardStatusBits,
                                elevationBoardServoStatusBits = it.elevationBoardServoStatusBits,
                                elevationBoardStatusBits = it.elevationBoardStatusBits,
                                trainBoardServoStatusBits = it.tiltBoardServoStatusBits,
                                trainBoardStatusBits = it.tiltBoardStatusBits,
                                feedBoardETCStatusBits = it.feedBoardETCStatusBits,
                                feedSBoardStatusBits = it.feedSBoardStatusBits,
                                feedXBoardStatusBits = it.feedXBoardStatusBits,
                                feedKaBoardStatusBits = it.feedKaBoardStatusBits,

                                // Current and RSSI data
                                currentSBandLNALHCP = it.currentSBandLNALHCP,
                                currentSBandLNARHCP = it.currentSBandLNARHCP,
                                currentXBandLNALHCP = it.currentXBandLNALHCP,
                                currentXBandLNARHCP = it.currentXBandLNARHCP,
                                currentKaBandLNALHCP = it.currentKaBandLNALHCP,
                                currentKaBandLNARHCP = it.currentKaBandLNARHCP,
                                rssiSBandLNALHCP = it.rssiSBandLNALHCP,
                                rssiSBandLNARHCP = it.rssiSBandLNARHCP,
                                rssiXBandLNALHCP = it.rssiXBandLNALHCP,
                                rssiXBandLNARHCP = it.rssiXBandLNARHCP,
                                rssiKaBandLNALHCP = it.rssiKaBandLNALHCP,
                                rssiKaBandLNARHCP = it.rssiKaBandLNARHCP,

                                //각가속도, 최대 각가속도
                                azimuthAcceleration = it.azimuthAcceleration,
                                elevationAcceleration = it.elevationAcceleration,
                                trainAcceleration = it.tiltAcceleration,
                                azimuthMaxAcceleration = it.azimuthMaxAcceleration,
                                elevationMaxAcceleration = it.elevationMaxAcceleration,
                                trainMaxAcceleration = it.tiltMaxAcceleration,

                                //traking 정보
                                trackingAzimuthTime = it.trackingAzimuthTime,
                                trackingCMDAzimuthAngle = it.trackingCMDAzimuthAngle + GlobalData.Offset.azimuthPositionOffset,
                                trackingActualAzimuthAngle = it.trackingActualAzimuthAngle,
                                trackingElevationTime = it.trackingElevationTime,
                                trackingCMDElevationAngle = it.trackingCMDElevationAngle + GlobalData.Offset.elevationPositionOffset,
                                trackingActualElevationAngle = it.trackingActualElevationAngle,
                                trackingTrainTime = it.trackingTiltTime,
                                trackingCMDTrainAngle = it.trackingCMDTiltAngle + GlobalData.Offset.trainPositionOffset + GlobalData.Offset.trueNorthOffset,
                                trackingActualTrainAngle = it.trackingActualTiltAngle,

                            )

                            // PushService 대신 DataStoreService 사용
                            dataStoreService.updateDataFromUdp(newData)
                        }
                    }
                    //2.3 Read Positioner Status
                    else if (receiveData[2] == 'P'.code.toByte()) {

                    }
                    //2.4 Read Firmware Version/Serial Number Info
                    else if (receiveData[2] == 'F'.code.toByte()) {
                        val parsedData = ReadFwVerSerialNoStatus.GetDataFrame.fromByteArray(receiveData)
                        parsedData?.let {
                            // SystemInfo에 저장
                            val newData = SystemInfo.FirmwareVersionSerialNoData(
                                // Main Board F/W Version
                                mainFwVerBitAll = it.mainFwVerBitAll,
                                mainFwVerReserved = it.mainFwVerReserved,
                                mainFwVerOne = it.mainFwVerOne,
                                mainFwVerTwo = it.mainFwVerTwo,
                                mainFwVerThree = it.mainFwVerThree,
                                
                                // Azimuth Board F/W Version
                                azimuthFwVerBitAll = it.azimuthFwVerBitAll,
                                azimuthFwVerReserved = it.azimuthFwVerReserved,
                                azimuthFwVerOne = it.azimuthFwVerOne,
                                azimuthFwVerTwo = it.azimuthFwVerTwo,
                                azimuthFwVerThree = it.azimuthFwVerThree,
                                
                                // Elevation Board F/W Version
                                elevationFwVerBitAll = it.elevationFwVerBitAll,
                                elevationFwVerReserved = it.elevationFwVerReserved,
                                elevationFwVerOne = it.elevationFwVerOne,
                                elevationFwVerTwo = it.elevationFwVerTwo,
                                elevationFwVerThree = it.elevationFwVerThree,
                                
                                // Tilt Board F/W Version
                                trainFwVerBitAll = it.trainFwVerBitAll,
                                trainFwVerReserved = it.trainFwVerReserved,
                                trainFwVerOne = it.trainFwVerOne,
                                trainFwVerTwo = it.trainFwVerTwo,
                                trainFwVerThree = it.trainFwVerThree,
                                
                                // Feed Board F/W Version
                                feedFwVerBitAll = it.feedFwVerBitAll,
                                feedFwVerReserved = it.feedFwVerReserved,
                                feedFwVerOne = it.feedFwVerOne,
                                feedFwVerTwo = it.feedFwVerTwo,
                                feedFwVerThree = it.feedFwVerThree,
                                
                                // Main Board Serial Number
                                mainSerialBitAll = it.mainSerialBitAll,
                                mainSerialYear = it.mainSerialYear,
                                mainSerialMonth = it.mainSerialMonth,
                                mainSerialNumber = it.mainSerialNumber,
                                
                                // Azimuth Board Serial Number
                                azimuthSerialBitAll = it.azimuthSerialBitAll,
                                azimuthSerialYear = it.azimuthSerialYear,
                                azimuthSerialMonth = it.azimuthSerialMonth,
                                azimuthSerialNumber = it.azimuthSerialNumber,
                                
                                // Elevation Board Serial Number
                                elevationSerialBitAll = it.elevationSerialBitAll,
                                elevationSerialYear = it.elevationSerialYear,
                                elevationSerialMonth = it.elevationSerialMonth,
                                elevationSerialNumber = it.elevationSerialNumber,
                                
                                // Tilt Board Serial Number
                                trainSerialBitAll = it.trainSerialBitAll,
                                trainSerialYear = it.trainSerialYear,
                                trainSerialMonth = it.trainSerialMonth,
                                trainSerialNumber = it.trainSerialNumber,
                                
                                // Feed Board Serial Number
                                feedSerialBitAll = it.feedSerialBitAll,
                                feedSerialYear = it.feedSerialYear,
                                feedSerialMonth = it.feedSerialMonth,
                                feedSerialNumber = it.feedSerialNumber,
                            )
                            
                            // SystemInfo 업데이트
                            SystemInfo.FIRMWARE_VERSION_SERIAL_NO = newData
                            
                            logger.info("Read Firmware Version/Serial Number Info 파싱 완료")
                        }
                    }
                }
                //2.1 Default Info (TBD)
                else if (receiveData[1] == 'W'.code.toByte()) {
                    val parsedData = DefaultInfo.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        println("파싱된 ICD 데이터: $it")
                    }
                }
                //2.5 Write NTP Info
                else if (receiveData[1] == 'I'.code.toByte()) {
                    val parsedData = WriteNTP.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        println("파싱된 ICD 데이터: $it")
                    }
                }
                //2.6 ACU S/W Emergency Command
                else if (receiveData[1] == 'E'.code.toByte()) {
                    val parsedData = Emergency.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        println("파싱된 ICD 데이터: $it")
                    }
                }
                //2.7 Manual Controls Command(1-Axis)
                else if (receiveData[1] == 'M'.code.toByte()) {
                    val parsedData = SingleManualControl.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        // println("파싱된 ICD 데이터: $it")
                    }
                }
                //2.8 Manual Controls Command(Multi-Axis)
                else if (receiveData[1] == 'A'.code.toByte()) {
                    val parsedData = MultiManualControl.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        // println("파싱된 ICD 데이터: $it")
                    }
                }
                //2.9 Stop Command
                else if (receiveData[1] == 'S'.code.toByte()) {
                    val parsedData = Stop.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        println("파싱된 ICD 데이터: $it")
                    }
                }
                //2.10 Standby Command
                else if (receiveData[1] == 'B'.code.toByte()) {
                    val parsedData = Standby.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        println("파싱된 ICD 데이터: $it")
                    }
                }
                //2.11 Feed On/Off Control Command
                else if (receiveData[1] == 'F'.code.toByte()) {
                    val parsedData = FeedOnOff.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        println("파싱된 ICD 데이터: $it")
                    }
                }
                //2.12 Satellite Track Command
                else if (receiveData[1] == 'T'.code.toByte()) {
                    //2.12.1 위성 추적 해더 정보 송신
                    if (receiveData[2] == 'T'.code.toByte()) {
                        val parsedData = SatelliteTrackOne.GetDataFrame.fromByteArray(receiveData)
                        parsedData?.let {
                            println("파싱된 ICD 데이터: $it")
                            // 이벤트 발행
                            acsEventBus.publish(ACSEvent.ICDEvent.SatelliteTrackHeaderReceived(it))
                        }

                    }
                    //2.12.2 위성 추적 초기 제어 명령
                    else if (receiveData[2] == 'M'.code.toByte()) {
                        val parsedData = SatelliteTrackTwo.GetDataFrame.fromByteArray(receiveData)
                        parsedData?.let {
                            println("파싱된 ICD 데이터: $it")
                        }
                    }
                    //2.12.3 위성 추적 추가 데이터 요청
                    else if (receiveData[2] == 'R'.code.toByte()) {
                        val parsedData = SatelliteTrackThree.GetDataFrame.fromByteArray(receiveData)
                        parsedData?.let {
                            println("파싱된 ICD 데이터: $it")
                            // 데이터 요청 이벤트 발행
                            acsEventBus.publish(ACSEvent.ICDEvent.SatelliteTrackDataRequested(it))
                        }
                    }
                }
                //Offset Command
                else if (receiveData[1] == 'O'.code.toByte()) {
                    //2.13 Time Offset Command
                    if (receiveData[2] == 'T'.code.toByte()) {
                        val parsedData = TimeOffset.GetDataFrame.fromByteArray(receiveData)
                        parsedData?.let {
                            println("파싱된 ICD 데이터: $it")
                        }
                    }
                    // 2.14 Position Offset Command
                    else if (receiveData[2] == 'P'.code.toByte()) {
                        val parsedData = PositionOffset.GetDataFrame.fromByteArray(receiveData)
                        parsedData?.let {
                            println("파싱된 ICD 데이터: $it")
                        }
                    }
                }
                //Preset Command
                else if (receiveData[1] == 'P'.code.toByte()) {
                    //2.15 Servo Encoder Preset
                    if (receiveData[2] == 'P'.code.toByte()) {
                        val parsedData = ServoEncoderPreset.GetDataFrame.fromByteArray(receiveData)
                    }
                    //2.16 Servo Alarm Reset
                    else if (receiveData[2] == 'A'.code.toByte()) {

                    }
                }
                //2.17 M/C On/Off
                else if (receiveData[1] == 'C'.code.toByte()) {

                }
            }
        }
    }




    /**
     * 2.10 Standby Command
     * 대기 상태 정보를 송신하기 위한 프로토콜이다.
     * 주요 정보: 축별 대기 상태 정보
     * 주요 사용처: 시스템 대기 모드 전환
     */
    class Standby {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var axis: BitSet,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(6)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // BitSet을 바이트로 변환 (빈 BitSet은 0x00)
                val byteAxis = if (axis.isEmpty) {
                    0x00.toByte()
                } else {
                    axis.toByteArray().getOrElse(0) { 0x00.toByte() }
                }
                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = byteAxis

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[3] = crc16Buffer[0]
                dataFrame[4] = crc16Buffer[1]
                dataFrame[5] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 6

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            ack = data[2],
                            checkSum = crc16Check,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }
    /**
     * 2.12 Satellite Track Command
     * 위성 추적 정보를 송신하기 위한 프로토콜
     * 2.12.1 위성 추적 해더 정보 송신
     * 위성 추적 시 필요한 기본 정보를 송신하기 위한 프로토콜이다
     * 주요 정보 : 전체 데이터 길이, AOS 시간 정보, LOS 시간 정보
     */
    class SatelliteTrackOne {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdTwo: Char,
            var dataLen: UShort,
            var aosYear: UShort,
            var aosMonth: Byte,
            var aosDay: Byte,
            var aosHour: Byte,
            var aosMinute: Byte,
            var aosSecond: Byte,
            var aosMs: UShort,
            var losYear: UShort,
            var losMonth: Byte,
            var losDay: Byte,
            var losHour: Byte,
            var losMinute: Byte,
            var losSecond: Byte,
            var losMs: UShort,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(26)

                // 바이트 변환 (엔디안 변환 포함)
                val byteDataLength = JKUtil.JKConvert.Companion.ushortToByteArray(dataLen, false)
                val byteAosYear = JKUtil.JKConvert.Companion.ushortToByteArray(aosYear, false)
                val byteAosMs = JKUtil.JKConvert.Companion.ushortToByteArray(aosMs, false)
                val byteLosYear = JKUtil.JKConvert.Companion.ushortToByteArray(losYear, false)
                val byteLosMs = JKUtil.JKConvert.Companion.ushortToByteArray(losMs, false)

                // CRC 대상 복사를 위한 배열
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()

                // AOS 시간 정보
                dataFrame[3] = byteDataLength[0]
                dataFrame[4] = byteDataLength[1]
                dataFrame[5] = byteAosYear[0]
                dataFrame[6] = byteAosYear[1]
                dataFrame[7] = aosMonth
                dataFrame[8] = aosDay
                dataFrame[9] = aosHour
                dataFrame[10] = aosMinute
                dataFrame[11] = aosSecond
                dataFrame[12] = byteAosMs[0]
                dataFrame[13] = byteAosMs[1]

                // LOS 시간 정보
                dataFrame[14] = byteLosYear[0]
                dataFrame[15] = byteLosYear[1]
                dataFrame[16] = losMonth
                dataFrame[17] = losDay
                dataFrame[18] = losHour
                dataFrame[19] = losMinute
                dataFrame[20] = losSecond
                dataFrame[21] = byteLosMs[0]
                dataFrame[22] = byteLosMs[1]

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[23] = crc16Buffer[0]
                dataFrame[24] = crc16Buffer[1]
                dataFrame[25] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var cmdTwo: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 7

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        println("one: ${data[1].toInt().toChar()}")
                        println("two: ${data[2].toInt().toChar()}")

                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            cmdTwo = data[2],
                            ack = data[3],
                            checkSum = crc16Check,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.12.2 위성 추적 초기 제어 명령
     * 위성 추적 시 필요한 초기 제어 정보를 송신하기 위한 프로토콜이다.
     * 주요 정보: 전송 데이터 길이, NTP 시간 정보, Time Offset 정보
     * 설명: 위성 추적을 시작할 때 또는 TimeOffset이 발생했을 경우 1회만 전송한다.
     * ※ Time Offset이 발생하면 ACU S/W가 '2.12.2 위성 추적 기본 제어 명령'을 ACU F/W에 전달하고,
     *   ACU F/W가 반복해서 '2.12.3 위성 추적 데이터 요청'을 실시함
     */
    class SatelliteTrackTwo {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdTwo: Char,
            var dataLen: UShort,
            var ntpYear: UShort,
            var ntpMonth: Byte,
            var ntpDay: Byte,
            var ntpHour: Byte,
            var ntpMinute: Byte,
            var ntpSecond: Byte,
            var ntpMs: UShort,
            var timeOffset: Int,
            var satelliteTrackData: List<Triple<UInt, Float, Float>>, // Triple<count, elevationAngle, azimuthAngle>
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                // 위성 추적 정보를 제외한 순수 데이터 프레임 21
                // 위성 추적 데이터 satelliteTrackData.size에서 데이터 바이트 12를 곱함
                // 데이터 프레임 배열 사이즈 지정을 위함. 오해하지 말 것.
                val dataFrame = ByteArray(21 + (satelliteTrackData.size * 12))

                // 바이트 변환 (엔디안 변환 포함)
                val byteDataLength = JKUtil.JKConvert.Companion.ushortToByteArray(dataLen, false)
                val byteNtpYear = JKUtil.JKConvert.Companion.ushortToByteArray(ntpYear, false)
                val byteNtpMs = JKUtil.JKConvert.Companion.ushortToByteArray(ntpMs, false)
                val byteTimeOffset = JKUtil.JKConvert.Companion.intToByteArray(timeOffset, false)

                // CRC 대상 복사를 위한 배열
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()

                // AOS 시간 정보
                dataFrame[3] = byteDataLength[0]
                dataFrame[4] = byteDataLength[1]
                dataFrame[5] = byteNtpYear[0]
                dataFrame[6] = byteNtpYear[1]
                dataFrame[7] = ntpMonth
                dataFrame[8] = ntpDay
                dataFrame[9] = ntpHour
                dataFrame[10] = ntpMinute
                dataFrame[11] = ntpSecond
                dataFrame[12] = byteNtpMs[0]
                dataFrame[13] = byteNtpMs[1]

                // Time Offset
                dataFrame[14] = byteTimeOffset[0]
                dataFrame[15] = byteTimeOffset[1]
                dataFrame[16] = byteTimeOffset[2]
                dataFrame[17] = byteTimeOffset[3]

                // 위성 추적 데이터 추가
                var i = 18
                for (data in satelliteTrackData) {
                    val byteCountArray = JKUtil.JKConvert.Companion.uintToByteArray(data.first, false)
                    val byteAzimuthAngle = JKUtil.JKConvert.Companion.floatToByteArray(data.third, false)
                    val byteElevationAngle = JKUtil.JKConvert.Companion.floatToByteArray(data.second, false)

                    dataFrame[i++] = byteCountArray[0]
                    dataFrame[i++] = byteCountArray[1]
                    dataFrame[i++] = byteCountArray[2]
                    dataFrame[i++] = byteCountArray[3]

                    dataFrame[i++] = byteAzimuthAngle[0]
                    dataFrame[i++] = byteAzimuthAngle[1]
                    dataFrame[i++] = byteAzimuthAngle[2]
                    dataFrame[i++] = byteAzimuthAngle[3]

                    dataFrame[i++] = byteElevationAngle[0]
                    dataFrame[i++] = byteElevationAngle[1]
                    dataFrame[i++] = byteElevationAngle[2]
                    dataFrame[i++] = byteElevationAngle[3]
                }

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[i++] = crc16Buffer[0]
                dataFrame[i++] = crc16Buffer[1]
                dataFrame[i] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var cmdTwo: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 7

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        println("Two: ${data[1].toInt().toChar()}")
                        println("${data[2].toInt().toChar()}")

                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            cmdTwo = data[2],
                            ack = data[3],
                            checkSum = crc16Check,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.12.3 위성 추적 추가 데이터 요청
     * ACU F/W가 ACU S/W로 추가 위성 추적 데이터 요청을 하기 위한 프로토콜이다.
     * 주요 정보: 전송 Data 길이, 위성 추적 정보
     * 설명: 초기 정보 수신 후 전체 위성 추적 데이터를 수신할 때 까지 반복해서 데이터를 요청함.
     * ※ '2.12.1 위성 추적 헤더 정보 송신' → '2.12.2 위성 추적 기본 제어 명령' 이후 전체 데이터를 수신할 때 까지
     *    ACU F/W가 '2.12.3 위성 추적 데이터 요청'을 통해 데이터를 요청함.
     */
    class SatelliteTrackThree {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdTwo: Char,
            var dataLength: UShort,
            var satelliteTrackData: List<Triple<Int, Float, Float>>, // Triple<count, elevationAngle, azimuthAngle>
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                // 위성 추적 정보를 제외한 순수 데이터 프레임
                // 위성 추적 데이터 satelliteTrackData.size에서 데이터 바이트 12를 곱함
                val dataFrame = ByteArray(8 + (satelliteTrackData.size * 12))

                // 바이트 변환 (엔디안 변환 포함)
                val byteDataLength = JKUtil.JKConvert.Companion.ushortToByteArray(dataLength, false)

                // CRC 대상 복사를 위한 배열
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()

                // 데이터 길이 정보
                dataFrame[3] = byteDataLength[0]
                dataFrame[4] = byteDataLength[1]

                // 위성 추적 데이터 추가
                var i = 5
                for (data in satelliteTrackData) {
                    val byteCountArray = JKUtil.JKConvert.Companion.intToByteArray(data.first, false)
                    val byteAzimuthAngle = JKUtil.JKConvert.Companion.floatToByteArray(data.third, false)
                    val byteElevationAngle = JKUtil.JKConvert.Companion.floatToByteArray(data.second, false)

                    dataFrame[i++] = byteCountArray[0]
                    dataFrame[i++] = byteCountArray[1]
                    dataFrame[i++] = byteCountArray[2]
                    dataFrame[i++] = byteCountArray[3]

                    dataFrame[i++] = byteAzimuthAngle[0]
                    dataFrame[i++] = byteAzimuthAngle[1]
                    dataFrame[i++] = byteAzimuthAngle[2]
                    dataFrame[i++] = byteAzimuthAngle[3]

                    dataFrame[i++] = byteElevationAngle[0]
                    dataFrame[i++] = byteElevationAngle[1]
                    dataFrame[i++] = byteElevationAngle[2]
                    dataFrame[i++] = byteElevationAngle[3]
                }

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[i++] = crc16Buffer[0]
                dataFrame[i++] = crc16Buffer[1]
                dataFrame[i] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var cmdTwo: Byte = 0x00,
            var requestDataLength: UShort = 0u,
            var timeAcc: UInt = 0u,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 12

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        // 데이터 길이 추출
                        val requestDataLength = JKUtil.JKConvert.Companion.byteArrayToUShort(byteArrayOf(data[3], data[4]))
                        println("Main Board의 요청 시간 누적치: $requestDataLength")
                        // 시간 누적치 추출
                        val timeAcc = JKUtil.JKConvert.Companion.uintEndianConvert(data[5], data[6], data[7], data[8], false)
                        println("Main Board의 요청 시간 누적치: $timeAcc")

                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            cmdTwo = data[2],
                            requestDataLength = requestDataLength,
                            timeAcc = timeAcc,
                            checkSum = crc16Check,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.5 Write NTP Info
     * NTP 정보를 송신하기 위한 프로토콜이다.
     * 주요 정보: NTP 시간 정보, Time Offset
     * 설명: ACU S/W에서 ACU F/W로 NTP 시간 정보를 전송한다.
     */
    class WriteNTP {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmd: Char,
            var year: UShort,
            var month: Byte,
            var day: Byte,
            var hour: Byte,
            var minute: Byte,
            var second: Byte,
            var ms: UShort,
            var timeOffset: Float,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(18)

                // 바이트 변환 (엔디안 변환 포함)
                val byteYear = JKUtil.JKConvert.Companion.ushortToByteArray(year, false)
                val byteMs = JKUtil.JKConvert.Companion.ushortToByteArray(ms, false)
                val byteTimeOffset = JKUtil.JKConvert.Companion.floatToByteArray(timeOffset, false)

                // CRC 대상 복사를 위한 배열
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmd.code.toByte()
                dataFrame[2] = byteYear[0]
                dataFrame[3] = byteYear[1]
                dataFrame[4] = month
                dataFrame[5] = day
                dataFrame[6] = hour
                dataFrame[7] = minute
                dataFrame[8] = second

                // ms
                dataFrame[9] = byteMs[0]
                dataFrame[10] = byteMs[1]

                // Time_Offset
                dataFrame[11] = byteTimeOffset[0]
                dataFrame[12] = byteTimeOffset[1]
                dataFrame[13] = byteTimeOffset[2]
                dataFrame[14] = byteTimeOffset[3]

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[15] = crc16Buffer[0]
                dataFrame[16] = crc16Buffer[1]
                dataFrame[17] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var year: UShort = 0u,
            var month: Byte = 0,
            var day: Byte = 0,
            var hour: Byte = 0,
            var minute: Byte = 0,
            var sec: Byte = 0,
            var ms: UShort = 0u,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 14

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            year = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[2], data[3])),
                            month = data[4],
                            day = data[5],
                            hour = data[6],
                            minute = data[7],
                            sec = data[8],
                            ms = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[9], data[10])),
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.13 Time Offset Command
     * Time Offset 정보를 송신하기 위한 프로토콜이다.
     * 주요 정보: Time Offset
     * Time Offset 적용 대상: Ephemeris Designation, Pass Schedule, Sun Track
     * 설명: Request(ACU SW)는 ACU SW의 'NTP 시간 정보'를 전달하고, Response(ACU FW(Main Board))의' NTP 시간 정보'는 ACU FW의 NTP 시간 정보를 응답한다
     */
    class TimeOffset {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdTwo: Char,
            var year: UShort,
            var month: Byte,
            var day: Byte,
            var hour: Byte,
            var minute: Byte,
            var second: Byte,
            var ms: UShort,
            var timeOffset: Float,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(19)

                val byteYear = JKUtil.JKConvert.Companion.ushortToByteArray(year, false)
                val byteMs = JKUtil.JKConvert.Companion.ushortToByteArray(ms, false)
                val byteTimeOffset = JKUtil.JKConvert.Companion.floatToByteArray(timeOffset, false)

                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()
                dataFrame[3] = byteYear[0]
                dataFrame[4] = byteYear[1]
                dataFrame[5] = month
                dataFrame[6] = day
                dataFrame[7] = hour
                dataFrame[8] = minute
                dataFrame[9] = second

                // ms
                dataFrame[10] = byteMs[0]
                dataFrame[11] = byteMs[1]

                // Time_Offset
                dataFrame[12] = byteTimeOffset[0]
                dataFrame[13] = byteTimeOffset[1]
                dataFrame[14] = byteTimeOffset[2]
                dataFrame[15] = byteTimeOffset[3]

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[16] = crc16Check[0]
                dataFrame[17] = crc16Check[1]
                dataFrame[18] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var cmdTwo: Byte = 0x00,
            var year: UShort = 0u,
            var month: Byte = 0,
            var day: Byte = 0,
            var hour: Byte = 0,
            var minute: Byte = 0,
            var sec: Byte = 0,
            var ms: UShort = 0u,
            var timeOffset: Float = 0f,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 19

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)

                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()
                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            cmdTwo = data[2],
                            year = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[3], data[4])),
                            month = data[5],
                            day = data[6],
                            hour = data[7],
                            minute = data[8],
                            sec = data[9],
                            ms = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[10], data[11])),
                            timeOffset = JKUtil.JKConvert.byteArrayToFloat(
                                byteArrayOf(
                                    data[12],
                                    data[13],
                                    data[14],
                                    data[15]
                                )
                            ),
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.8 Manual Controls Command(Multi-Axis)
     * 여러 개의 축을 동시에 제어하기 위한 프로토콜이다.
     * 주요 정보: 3-Axis Positioner 이동 정보
     * 주요 사용처: Step, Slew, Pedestal Position, Ephemeris Designation, Pass Schdule, Sun Track
     */
    class MultiManualControl {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var axis: BitSet,
            var azimuthAngle: Float,
            var azimuthSpeed: Float,
            var elevationAngle: Float,
            var elevationSpeed: Float,
            var trainAngle: Float,
            var trainSpeed: Float,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(30)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // Float 값들을 바이트 배열로 변환 (엔디안 변환 포함)
                val byteAzimuthAngle = JKUtil.JKConvert.Companion.floatToByteArray(azimuthAngle, false)
                val byteAzimuthSpeed = JKUtil.JKConvert.Companion.floatToByteArray(azimuthSpeed, false)
                val byteElevationAngle = JKUtil.JKConvert.Companion.floatToByteArray(elevationAngle, false)
                val byteElevationSpeed = JKUtil.JKConvert.Companion.floatToByteArray(elevationSpeed, false)
                val byteTiltAngle = JKUtil.JKConvert.Companion.floatToByteArray(trainAngle, false)
                val byteTiltSpeed = JKUtil.JKConvert.Companion.floatToByteArray(trainSpeed, false)

                // BitSet을 바이트 배열로 변환
                val byteAxis = axis.toByteArray()[0]

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = byteAxis

                // Azimuth 축 각도 값
                dataFrame[3] = byteAzimuthAngle[0]
                dataFrame[4] = byteAzimuthAngle[1]
                dataFrame[5] = byteAzimuthAngle[2]
                dataFrame[6] = byteAzimuthAngle[3]

                // Azimuth 축 속도 값
                dataFrame[7] = byteAzimuthSpeed[0]
                dataFrame[8] = byteAzimuthSpeed[1]
                dataFrame[9] = byteAzimuthSpeed[2]
                dataFrame[10] = byteAzimuthSpeed[3]

                // Elevation 축 각도 값
                dataFrame[11] = byteElevationAngle[0]
                dataFrame[12] = byteElevationAngle[1]
                dataFrame[13] = byteElevationAngle[2]
                dataFrame[14] = byteElevationAngle[3]

                // Elevation 축 속도 값
                dataFrame[15] = byteElevationSpeed[0]
                dataFrame[16] = byteElevationSpeed[1]
                dataFrame[17] = byteElevationSpeed[2]
                dataFrame[18] = byteElevationSpeed[3]

                // Tilt 축 각도 값
                dataFrame[19] = byteTiltAngle[0]
                dataFrame[20] = byteTiltAngle[1]
                dataFrame[21] = byteTiltAngle[2]
                dataFrame[22] = byteTiltAngle[3]

                // Tilt 축 속도 값
                dataFrame[23] = byteTiltSpeed[0]
                dataFrame[24] = byteTiltSpeed[1]
                dataFrame[25] = byteTiltSpeed[2]
                dataFrame[26] = byteTiltSpeed[3]

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[27] = crc16Check[0]
                dataFrame[28] = crc16Check[1]
                dataFrame[29] = ICD_ETX
            
                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 6

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            ack = data[2],
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.11 Feed On/Off Control Command
     * Feed On/Off 제어 정보를 송신하기 위한 프로토콜이다.
     * 주요 정보: Feed On/Off 제어 정보
     * 주요 사용처: Feed
     */
    class FeedOnOff {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var feedOnOff: BitSet,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                /**
                 * 프로토콜 명세: Command Field는 2바이트(Unsigned Short)
                 * 프레임 구조: STX(1) + CMD(1) + Data(2) + CRC16(2) + ETX(1) = 7바이트
                 */
                val dataFrame = ByteArray(7)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // BitSet을 2바이트(Unsigned Short)로 변환 - 빅 엔디안 방식
                // BitSet.toByteArray()는 설정된 비트가 있는 만큼만 반환하므로, 항상 2바이트로 확장
                val byteArray = feedOnOff.toByteArray()
                val bytes = ByteArray(2) { 0x00 }  // 2바이트 배열 초기화
                byteArray.forEachIndexed { index, byte ->
                    if (index < 2) bytes[index] = byte
                }

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = bytes[1]  // High byte (Bits 15-8) - 빅 엔디안
                dataFrame[3] = bytes[0]  // Low byte (Bits 7-0) - 빅 엔디안

                // CRC 대상 복사 (CMD부터 Data까지)
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 빅 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[4] = crc16Buffer[0]
                dataFrame[5] = crc16Buffer[1]
                dataFrame[6] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = 0x00,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 6

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(
                        byteArrayOf(
                            data[FRAME_LENGTH - 3],
                            data[FRAME_LENGTH - 2]
                        )
                    )
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()
                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            ack = data[2],
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * Position Offset Command
     * 위치 오프셋 정보를 송신하기 위한 프로토콜
     */
    class PositionOffset {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdTwo: Char,
            var azimuthOffset: Float,
            var elevationOffset: Float,
            var trainOffset: Float,
            var crc16: UShort,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(18)

                // Float 값을 바이트 배열로 변환 (엔디안 변환 포함)
                val byteAzimuthOffset = JKUtil.JKConvert.Companion.floatToByteArray(azimuthOffset, false)
                val byteElevationOffset = JKUtil.JKConvert.Companion.floatToByteArray(elevationOffset, false)
                val byteTiltOffset = JKUtil.JKConvert.Companion.floatToByteArray(trainOffset, false)

                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()

                // Position Offset - Azimuth
                dataFrame[3] = byteAzimuthOffset[0]
                dataFrame[4] = byteAzimuthOffset[1]
                dataFrame[5] = byteAzimuthOffset[2]
                dataFrame[6] = byteAzimuthOffset[3]

                // Position Offset - Elevation
                dataFrame[7] = byteElevationOffset[0]
                dataFrame[8] = byteElevationOffset[1]
                dataFrame[9] = byteElevationOffset[2]
                dataFrame[10] = byteElevationOffset[3]

                // Position Offset - Tilt
                dataFrame[11] = byteTiltOffset[0]
                dataFrame[12] = byteTiltOffset[1]
                dataFrame[13] = byteTiltOffset[2]
                dataFrame[14] = byteTiltOffset[3]

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[15] = crc16Check[0]
                dataFrame[16] = crc16Check[1]
                dataFrame[17] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte,
            var cmdTwo: Byte,
            var azimuthOffset: Float,
            var elevationOffset: Float,
            var tiltOffset: Float,
            var checkSum: UShort,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 18

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()
                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        // Float 값 추출 (엔디안 변환 포함)
                        val azimuthOffset = JKUtil.JKConvert.byteArrayToFloat(
                            byteArrayOf(data[3], data[4], data[5], data[6])
                        )
                        val elevationOffset = JKUtil.JKConvert.byteArrayToFloat(
                            byteArrayOf(data[7], data[8], data[9], data[10])
                        )
                        val tiltOffset = JKUtil.JKConvert.byteArrayToFloat(
                            byteArrayOf(data[11], data[12], data[13], data[14])
                        )

                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            cmdTwo = data[2],
                            azimuthOffset = azimuthOffset,
                            elevationOffset = elevationOffset,
                            tiltOffset = tiltOffset,
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.9 Stop Command
     * 정지 정보를 송신 하기 위한 프로토콜이다
     * 설명: ACU S/W의 Stop 버튼
     * 주요 정보: 1-Axis 정지 정보 / 전체 Axis 정지
     * 주요 사용처: Step, Slew, Pedestal Position, Ephemeris Designation, Pass Schedule, Sun Track
     */
    class Stop {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var axis: BitSet,
            var crc16: UShort,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(6)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // BitSet을 바이트 배열로 변환
                val byteAxis = axis.toByteArray()[0]

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = byteAxis

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[3] = crc16Check[0]
                dataFrame[4] = crc16Check[1]
                dataFrame[5] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 6

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                        val frame = GetDataFrame()
                        frame.stx = buffer.get()
                        frame.cmdOne = buffer.get()
                        frame.ack = buffer.get()
                        frame.checkSum = rxChecksum
                        frame.etx = data.last()
                        return frame
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.6 ACU S/W Emergency Command
     * 비상 정지 정보를 송신하기 위한 프로토콜이다.
     * 주요 정보: ACU Emergency 버튼 정보
     * 주요 사용처: 상시
     */
    class Emergency {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdOnOff: Boolean,
            var crc16: UShort,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(6)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()

                // cmdOnOff에 따라 'E' 또는 'S' 설정
                val setData = if (cmdOnOff) 'E' else 'S'
                dataFrame[2] = setData.code.toByte()

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[3] = crc16Check[0]
                dataFrame[4] = crc16Check[1]
                dataFrame[5] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 5

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()
                    // CRC 체크섬 추출 (리틀 엔디안)
                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                        val frame = GetDataFrame()
                        frame.stx = buffer.get()
                        frame.cmdOne = buffer.get()
                        frame.checkSum = rxChecksum
                        frame.etx = data.last()
                        return frame

                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /// <summary>
    /// 2.1 DefaultInfo 송신 데이터 프레임 값 전송을 위한 배열 세팅 함수
    /// <br>총 30개 배열 데이터</br>
    /// </summary>
    /// <returns></returns>
    class DefaultInfo {
        data class SetDataFrame(
            var cmd: Char,
            var year: UShort,
            var month: Byte,
            var day: Byte,
            var hour: Byte,
            var minute: Byte,
            var second: Byte,
            var ms: UShort,
            var timeOffset: Float,
            var azimuthOffset: Float,
            var elevationOffset: Float,
            var trainOffset: Float,
            var crc16: UShort
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(30)
                val byteYear: ByteArray = JKUtil.JKConvert.ushortToByteArray(year, false)
                val byteMs: ByteArray = JKUtil.JKConvert.ushortToByteArray(ms, false)
                val byteTimeOffset: ByteArray = JKUtil.JKConvert.floatToByteArray(timeOffset, false)
                val byteAzimuthOffset: ByteArray = JKUtil.JKConvert.floatToByteArray(azimuthOffset, false);
                val byteElevationOffset: ByteArray = JKUtil.JKConvert.floatToByteArray(elevationOffset, false);
                val byteTiltOffset: ByteArray = JKUtil.JKConvert.floatToByteArray(trainOffset, false);
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmd.code.toByte()
                dataFrame[2] = byteYear[0]
                dataFrame[3] = byteYear[1]
                dataFrame[4] = month
                dataFrame[5] = day
                dataFrame[6] = hour
                dataFrame[7] = minute
                dataFrame[8] = second

                // ms
                dataFrame[9] = byteMs[0];
                dataFrame[10] = byteMs[1];

                // Time_Offset
                dataFrame[11] = byteTimeOffset[0];
                dataFrame[12] = byteTimeOffset[1];
                dataFrame[13] = byteTimeOffset[2];
                dataFrame[14] = byteTimeOffset[3];

                //Position Offset - Azimuth
                dataFrame[15] = byteAzimuthOffset[0];
                dataFrame[16] = byteAzimuthOffset[1];
                dataFrame[17] = byteAzimuthOffset[2];
                dataFrame[18] = byteAzimuthOffset[3];

                //Position Offset - Elevation
                dataFrame[19] = byteElevationOffset[0];
                dataFrame[20] = byteElevationOffset[1];
                dataFrame[21] = byteElevationOffset[2];
                dataFrame[22] = byteElevationOffset[3];

                //Position Offset - Tilt
                dataFrame[23] = byteTiltOffset[0];
                dataFrame[24] = byteTiltOffset[1];
                dataFrame[25] = byteTiltOffset[2];
                dataFrame[26] = byteTiltOffset[3];

                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size) // destinationOffset을 0으로 수정
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer
                // CRC16
                dataFrame[27] = crc16Check[0];
                dataFrame[28] = crc16Check[1];

                dataFrame[29] = ICD_ETX;

                return dataFrame;
            }
        }

        /// <summary>
        /// 수신 데이터 프레임 구조
        /// </summary>
        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            /// <summary>
            /// 수신 데이터 값 데이터 프레임에 입력함.
            /// </summary>
            /// <param name="data"></param>
            /// <returns></
            companion object {
                const val FRAME_LENGTH = 6

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                        val frame = GetDataFrame()
                        frame.stx = buffer.get()
                        frame.cmdOne = buffer.get()
                        frame.ack = buffer.get()
                        frame.checkSum = rxChecksum;
                        frame.etx = data.last()
                        return frame
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    //Read Status ICD Frame
    class ReadStatus {

        data class SetDataFrame(
            var cmdOne: Char = 'R',
            var cmdTwo: Char = 'R',
            var mode: Byte = 0x01,
            var crc16: UShort = 0u,
        ) {

            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(7)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()
                dataFrame[3] = mode

                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size) // destinationOffset을 0으로 수정
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 (Little Endian 가정)
                dataFrame[4] = crc16Check[0]
                dataFrame[5] = crc16Check[1]

                dataFrame[6] = ICD_ETX

                return dataFrame
            }
        }


        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var cmdTwo: Byte = 0x00,
            var modeStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var azimuthAngle: Float = 0f,
            var elevationAngle: Float = 0f,
            var tiltAngle: Float = 0f,
            var azimuthSpeed: Float = 0f,
            var elevationSpeed: Float = 0f,
            var tiltSpeed: Float = 0f,
            var servoDriverAzimuthAngle: Float = 0f,
            var servoDriverElevationAngle: Float = 0f,
            var servoDriverTiltAngle: Float = 0f,
            var torqueAzimuth: Float = 0f,
            var torqueElevation: Float = 0f,
            var torqueTilt: Float = 0f,
            var windSpeed: Float = 0f,
            var windDirection: UShort = 0u,
            var rtdOne: Float = 0f,
            var rtdTwo: Float = 0f,
            var mainBoardProtocolStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var mainBoardStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var mainBoardMCOnOffBits: String = "00000000",  // BitSet에서 String으로 변경
            var mainBoardReserveBits: String = "00000000",  // BitSet에서 String으로 변경
            var azimuthBoardServoStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var azimuthBoardStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var elevationBoardServoStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var elevationBoardStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var tiltBoardServoStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var tiltBoardStatusBits: String = "00000000",  // BitSet에서 String으로 변경
            var feedBoardETCStatusBits: String = "00000000",  // BitSet에서 String으로 변경 (Bits 7-0)
            var feedSBoardStatusBits: String = "00000000",  // BitSet에서 String으로 변경 (Bits 15-8)
            var feedXBoardStatusBits: String = "00000000",  // BitSet에서 String으로 변경 (Bits 23-16)
            var feedKaBoardStatusBits: String = "00000000",  // BitSet에서 String으로 변경 (Bits 31-24)
            var currentSBandLNALHCP: Float = 0f,
            var currentSBandLNARHCP: Float = 0f,
            var currentXBandLNALHCP: Float = 0f,
            var currentXBandLNARHCP: Float = 0f,
            var currentKaBandLNALHCP: Float = 0f,
            var currentKaBandLNARHCP: Float = 0f,
            var rssiSBandLNALHCP: Float = 0f,
            var rssiSBandLNARHCP: Float = 0f,
            var rssiXBandLNALHCP: Float = 0f,
            var rssiXBandLNARHCP: Float = 0f,
            var rssiKaBandLNALHCP: Float = 0f,
            var rssiKaBandLNARHCP: Float = 0f,
            var azimuthAcceleration: Float = 0f,
            var elevationAcceleration: Float = 0f,
            var tiltAcceleration: Float = 0f,
            var azimuthMaxAcceleration: Float = 0f,
            var elevationMaxAcceleration: Float = 0f,
            var tiltMaxAcceleration: Float = 0f,
            var trackingAzimuthTime: Float = 0f,
            var trackingCMDAzimuthAngle: Float = 0f,
            var trackingActualAzimuthAngle: Float = 0f,
            var trackingElevationTime: Float = 0f,
            var trackingCMDElevationAngle: Float = 0f,
            var trackingActualElevationAngle: Float = 0f,
            var trackingTiltTime: Float = 0f,
            var trackingCMDTiltAngle: Float = 0f,
            var trackingActualTiltAngle: Float = 0f,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            enum class Mode {
                standby,
                slew,
                step,
                pedestal,
                ephemeris,
                pass_Schedule,
                feed,
                sunTrack
            }

            enum class MainBoardProtocolStatus {
                elevaiton_Timeout,
                azimuth_Timeout,
                tilt_Timeout,
                feed_Timeout,
                reserve_Four,
                reserve_Five,
                reserve_Six,
                default_Receive_OK
            }

            enum class MainBoardStatus {
                surge_Protector_Error,
                negative_Phase_Relay_Error,
                emergency_ACU_Error,
                emergency_Positioner_Error,
                ip_Switch_Reset,
                reserve_Five,
                reserve_Six,
                reserve_Seven
            }

            enum class MainBoardMCStatus {
                mc_Tilt_ON,
                mc_Elevation_ON,
                mc_Azimuth_ON,
                reserve_Three,
                reserve_Four,
                reserve_Five,
                reserve_Six,
                reserve_Seven
            }

            enum class MainBoardReserve {
                reserve_Zero,
                reserve_One,
                reserve_Two,
                reserve_Three,
                reserve_Four,
                reserve_Five,
                reserve_Six,
                reserve_Seven
            }

            enum class AzimuthBoardServoStatus(val value: Int) {
                SERVO_ALARM_CODE1(0),
                SERVO_ALARM_CODE2(1),
                SERVO_ALARM_CODE3(2),
                SERVO_ALARM_CODE4(3),
                SERVO_ALARM_CODE5(4),
                SERVO_ALARM_ERROR(5),
                SERVO_BRAKE(6),
                SERVO_MOTOR_MOVE(7);
            }

            enum class AzimuthBoardStatus {
                limit_Switch_Max_ON,
                limit_Switch_Min_ON,
                reserve_Two,
                reserve_Three,
                stow_Pin_ON,
                reserve_Five,
                reserve_Six,
                encoder_Error
            }

            enum class ElevationBoardServoStatus(val value: Int) {
                SERVO_ALARM_CODE1(0),
                SERVO_ALARM_CODE2(1),
                SERVO_ALARM_CODE3(2),
                SERVO_ALARM_CODE4(3),
                SERVO_ALARM_CODE5(4),
                SERVO_ALARM_ERROR(5),
                SERVO_BRAKE(6),
                SERVO_MOTOR_MOVE(7);
            }

            enum class ElevationBoardStatus {
                limit_Switch_MaxOne_ON,
                limit_Switch_MaxTwo_ON,
                limit_Switch_MinOne_ON,
                limit_Switch_MinTwo_ON,
                stow_Pin_ON,
                reserve_Five,
                reserve_Six,
                encoder_Error
            }

            enum class TiltBoardServoStatus(val value: Int) {
                SERVO_ALARM_CODE1(0),
                SERVO_ALARM_CODE2(1),
                SERVO_ALARM_CODE3(2),
                SERVO_ALARM_CODE4(3),
                SERVO_ALARM_CODE5(4),
                SERVO_ALARM_ERROR(5),
                SERVO_BRAKE(6),
                SERVO_MOTOR_MOVE(7);
            }

            enum class TiltBoardStatus {
                limit_Switch_Max_ON,
                limit_Switch_Min_ON,
                reserve_Two,
                reserve_Three,
                stow_Pin_ON,
                reserve_Five,
                reserve_Six,
                encoder_Error
            }
            enum class FeedBoardETCStatus {
                s_Band_RF_LHCP_OR_RHCP_ON,
                s_Band_RF_LHCP_OR_RHCP_ON_Error,
                FAN_ON,
                FAN_ERROR,
                reserve_Four,
                reserve_Five,
                reserve_Six,
                reserve_Seven
            }
            enum class FeedBoardSBandStatus {
                s_Band_LHCP_ON,
                s_Band_LHCP_Error,
                s_Band_RHCP_ON,
                s_Band_RHCP_Error,
                reserve_Four,
                reserve_Five,
                reserve_Six,
                reserve_Seven
            }
            enum class FeedBoardXBandStatus {
                x_Band_LHCP_ON,
                x_Band_LHCP_Error,
                x_Band_RHCP_ON,
                x_Band_RHCP_Error,
                reserve_Four,
                reserve_Five,
                reserve_Six,
                reserve_Seven
            }
            enum class FeedBoardKaBandStatus {
                ka_Band_LHCP_ON,
                ka_Band_LHCP_Error,
                ka_Band_RHCP_ON,
                ka_Band_RHCP_Error,
                ka_Band_LHCP_BAND1_OR_BAND2_Selection,
                ka_Band_LHCP_Selection_Error,
                ka_Band_RHCP_BAND1_OR_BAND2_Selection,
                ka_Band_RHCP_BAND1_OR_BAND2_Selection_Error,
            }

            companion object {
                // FRAME_LENGTH: ETX를 제외한 프레임 길이 (실제 수신: 191바이트 = 190 + ETX 1바이트)
                const val FRAME_LENGTH = 190

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    // 최소 길이 체크 (STX + CMD + 최소 데이터 + CRC + ETX)
                    if (data.size < 10) {
                        println("수신 데이터 길이가 너무 짧습니다: ${data.size}")
                        return null
                    }

                    // ETX 확인 (마지막 바이트)
                    val etxByte = data.last()
                    val expectedEtx = ICD_ETX
                    if (etxByte != expectedEtx) {
                        println("ETX 불일치: 수신=0x${etxByte.toUByte().toString(16)}, 예상=0x${expectedEtx.toUByte().toString(16)}")
                        return null
                    }

                    // 실제 프레임 길이 계산 (ETX 제외)
                    val actualFrameLength = data.size - 1 // ETX 제외
                    
                    // CRC 위치: ETX 직전 2바이트 (빅 엔디안 - 다른 클래스들과 동일)
                    // data[actualFrameLength - 2], data[actualFrameLength - 1] = CRC
                    // data[actualFrameLength] = ETX
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[actualFrameLength - 2], data[actualFrameLength - 1]))
                        .short.toUShort()  // 기본값 BIG_ENDIAN 사용 (다른 클래스들과 동일)

                    // CRC 타겟: STX 다음부터 CRC 직전까지
                    val crc16Target = data.copyOfRange(1, actualFrameLength - 2)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증
                    if (rxChecksum != crc16Check) {
                        println("ReadStatus CRC 체크 실패:")
                        println("  수신 데이터 길이: ${data.size} (ETX 제외: $actualFrameLength)")
                        println("  STX: 0x${data[0].toUByte().toString(16)} (예상: 0x${ICD_STX.toUByte().toString(16)})")
                        println("  CMD1: 0x${data[1].toUByte().toString(16)} (예상: 'R'=0x${'R'.code.toString(16)})")
                        if (data.size > 2) {
                            println("  CMD2: 0x${data[2].toUByte().toString(16)} (예상: 'R'=0x${'R'.code.toString(16)})")
                        }
                        println("  수신 CRC 위치: data[${actualFrameLength - 2}], data[${actualFrameLength - 1}]")
                        println("  수신 CRC 바이트: 0x${data[actualFrameLength - 2].toUByte().toString(16)}, 0x${data[actualFrameLength - 1].toUByte().toString(16)}")
                        println("  수신 CRC (빅 엔디안): 0x${rxChecksum.toString(16)}")
                        println("  계산 CRC: 0x${crc16Check.toString(16)}")
                        println("  CRC 타겟 범위: data[1] ~ data[${actualFrameLength - 2}] (${actualFrameLength - 2}바이트)")
                        println("  CRC 타겟 HEX: ${JKUtil.JKConvert.Companion.byteArrayToHexString(crc16Target.take(20).toByteArray())}...")
                        println("  ETX: 0x${etxByte.toUByte().toString(16)} (예상: 0x${expectedEtx.toUByte().toString(16)})")
                        println("  전체 데이터 HEX (처음 50바이트): ${JKUtil.JKConvert.Companion.byteArrayToHexString(data.take(50).toByteArray())}...")
                        println("  전체 데이터 HEX (마지막 10바이트): ...${JKUtil.JKConvert.Companion.byteArrayToHexString(data.takeLast(10).toByteArray())}")
                        return null
                    }
                    
                    // CRC 검증 통과 시 프레임 파싱
                    if (true) {
                        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

                        val frame = GetDataFrame()
                        frame.stx = buffer.get()
                        frame.cmdOne = buffer.get()
                        frame.cmdTwo = buffer.get()
                        val modeStatusByte = buffer.get()
                        frame.modeStatusBits = JKUtil.JKConvert.Companion.byteToBinaryString(modeStatusByte)

                        frame.azimuthAngle = buffer.float
                        frame.elevationAngle = buffer.float
                        frame.tiltAngle = buffer.float
                        frame.azimuthSpeed = buffer.float
                        frame.elevationSpeed = buffer.float
                        frame.tiltSpeed = buffer.float

                        // Main Board Status (4 bytes)
                        val mainBoardReserveByte = buffer.get()
                        frame.mainBoardReserveBits = JKUtil.JKConvert.Companion.byteToBinaryString(mainBoardReserveByte)

                        val mainBoardMCOnOffByte = buffer.get()
                        frame.mainBoardMCOnOffBits = JKUtil.JKConvert.Companion.byteToBinaryString(mainBoardMCOnOffByte)

                        val mainBoardStatusByte = buffer.get()
                        frame.mainBoardStatusBits = JKUtil.JKConvert.Companion.byteToBinaryString(mainBoardStatusByte)

                        val mainBoardProtocolStatusByte = buffer.get()
                        frame.mainBoardProtocolStatusBits =
                            JKUtil.JKConvert.Companion.byteToBinaryString(mainBoardProtocolStatusByte)

                        // Azimuth Board Status (2 bytes)
                        val azimuthBoardStatusByte = buffer.get()
                        frame.azimuthBoardStatusBits =
                            JKUtil.JKConvert.Companion.byteToBinaryString(azimuthBoardStatusByte)

                        val azimuthBoardServoStatusByte = buffer.get()
                        frame.azimuthBoardServoStatusBits =
                            JKUtil.JKConvert.Companion.byteToBinaryString(azimuthBoardServoStatusByte)

                        // Elevation Board Status (2 bytes)
                        val elevationBoardStatusByte = buffer.get()
                        frame.elevationBoardStatusBits =
                            JKUtil.JKConvert.Companion.byteToBinaryString(elevationBoardStatusByte)

                        val elevationBoardServoStatusByte = buffer.get()
                        frame.elevationBoardServoStatusBits =
                            JKUtil.JKConvert.Companion.byteToBinaryString(elevationBoardServoStatusByte)

                        // Tilt Board Status (2 bytes)
                        val tiltBoardStatusByte = buffer.get()
                        frame.tiltBoardStatusBits = JKUtil.JKConvert.Companion.byteToBinaryString(tiltBoardStatusByte)

                        val tiltBoardServoStatusByte = buffer.get()
                        frame.tiltBoardServoStatusBits =
                            JKUtil.JKConvert.Companion.byteToBinaryString(tiltBoardServoStatusByte)

                        // Feed Board Status (4 bytes - Unsigned Long)
                        // ICD 문서: 32비트 Unsigned Long으로 전송
                        // Bit 31-24: Ka-Band Status
                        // Bit 23-16: X-Band Status
                        // Bit 15-8: S-Band Status
                        // Bit 7-0: ETC Status (Fan, S-Band TX RF Switch)
                        val feedBoardStatusLong = buffer.int.toLong() and 0xFFFFFFFFL // Unsigned Long 처리

                        // 각 8비트로 분리하여 저장
                        frame.feedBoardETCStatusBits = ((feedBoardStatusLong and 0xFF).toInt()).let {
                            JKUtil.JKConvert.Companion.byteToBinaryString(it.toByte())
                        } // Bits 7-0

                        frame.feedSBoardStatusBits = ((feedBoardStatusLong shr 8 and 0xFF).toInt()).let {
                            JKUtil.JKConvert.Companion.byteToBinaryString(it.toByte())
                        } // Bits 15-8

                        frame.feedXBoardStatusBits = ((feedBoardStatusLong shr 16 and 0xFF).toInt()).let {
                            JKUtil.JKConvert.Companion.byteToBinaryString(it.toByte())
                        } // Bits 23-16

                        frame.feedKaBoardStatusBits = ((feedBoardStatusLong shr 24 and 0xFF).toInt()).let {
                            JKUtil.JKConvert.Companion.byteToBinaryString(it.toByte())
                        } // Bits 31-24

                        // 나머지 필드들 처리...
                        frame.currentSBandLNALHCP = buffer.float
                        frame.currentSBandLNARHCP = buffer.float
                        frame.currentXBandLNALHCP = buffer.float
                        frame.currentXBandLNARHCP = buffer.float
                        frame.currentKaBandLNALHCP = buffer.float
                        frame.currentKaBandLNARHCP = buffer.float
                        frame.rssiSBandLNALHCP = buffer.float
                        frame.rssiSBandLNARHCP = buffer.float
                        frame.rssiXBandLNALHCP = buffer.float
                        frame.rssiXBandLNARHCP = buffer.float
                        frame.rssiKaBandLNALHCP = buffer.float
                        frame.rssiKaBandLNARHCP = buffer.float
                        frame.servoDriverAzimuthAngle = buffer.float
                        frame.servoDriverElevationAngle = buffer.float
                        frame.servoDriverTiltAngle = buffer.float
                        frame.torqueAzimuth = buffer.float
                        frame.torqueElevation = buffer.float
                        frame.torqueTilt = buffer.float
                        frame.windSpeed = buffer.float
                        frame.windDirection = buffer.short.toUShort()
                        frame.rtdOne = buffer.float
                        frame.rtdTwo = buffer.float
                        frame.azimuthAcceleration = buffer.float
                        frame.elevationAcceleration = buffer.float
                        frame.tiltAcceleration = buffer.float
                        frame.azimuthMaxAcceleration = buffer.float
                        frame.elevationMaxAcceleration = buffer.float
                        frame.tiltMaxAcceleration = buffer.float
                        frame.trackingAzimuthTime = buffer.float
                        frame.trackingCMDAzimuthAngle = buffer.float
                        frame.trackingActualAzimuthAngle = buffer.float
                        frame.trackingElevationTime = buffer.float
                        frame.trackingCMDElevationAngle = buffer.float
                        frame.trackingActualElevationAngle = buffer.float
                        frame.trackingTiltTime = buffer.float
                        frame.trackingCMDTiltAngle = buffer.float
                        frame.trackingActualTiltAngle = buffer.float
                        frame.checkSum = rxChecksum
                        frame.etx = data.last()
                        //println(" az :${frame.azimuthAngle}, el : ${frame.elevationAngle}, ti : ${frame.tiltAngle}")
                        return frame


                    } else {
                        // 상세한 디버깅 정보 출력 (ReadStatus CRC 체크 실패 시)
                        println("ReadStatus CRC 체크 실패 또는 ETX 불일치:")
                        println("  수신 데이터 길이: ${data.size} (예상: $FRAME_LENGTH)")
                        println("  STX: 0x${data[0].toUByte().toString(16)} (예상: 0x${ICD_STX.toUByte().toString(16)})")
                        println("  CMD1: 0x${data[1].toUByte().toString(16)} (예상: 'R'=0x${'R'.code.toString(16)})")
                        if (data.size > 2) {
                            println("  CMD2: 0x${data[2].toUByte().toString(16)} (예상: 'R'=0x${'R'.code.toString(16)})")
                        }
                        println("  수신 CRC 위치: data[${FRAME_LENGTH - 2}], data[${FRAME_LENGTH - 1}]")
                        println("  수신 CRC: 0x${rxChecksum.toString(16)}")
                        println("  계산 CRC: 0x${crc16Check.toString(16)}")
                        println("  CRC 타겟 범위: data[1] ~ data[${FRAME_LENGTH - 2}] (${FRAME_LENGTH - 3}바이트)")
                        println("  CRC 타겟 HEX: ${JKUtil.JKConvert.Companion.byteArrayToHexString(crc16Target.take(20).toByteArray())}...")
                        println("  ETX: 0x${etxByte.toUByte().toString(16)} (예상: 0x${expectedEtx.toUByte().toString(16)})")
                        println("  전체 데이터 HEX (처음 50바이트): ${JKUtil.JKConvert.Companion.byteArrayToHexString(data.take(50).toByteArray())}...")
                        println("  전체 데이터 HEX (마지막 10바이트): ...${JKUtil.JKConvert.Companion.byteArrayToHexString(data.takeLast(10).toByteArray())}")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.15 Servo Encoder Preset
     * 서보 엔코더 프리셋 정보를 송신하기 위한 프로토콜
     */
    class ServoEncoderPreset {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdTwo: Char,
            var axis: BitSet,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(7)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // BitSet을 바이트 배열로 변환
                val byteAxis = axis.toByteArray()[0]

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()
                dataFrame[3] = byteAxis

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[4] = crc16Check[0]
                dataFrame[5] = crc16Check[1]
                dataFrame[6] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var cmdTwo: Byte = 0x00,
            var axis: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 8

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            cmdTwo = data[2],
                            axis = data[3],
                            ack = data[4],
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.7 Manual Controls Command(1-Axis)
     * 하나의 축을 제어하기 위한 프로토콜이다.
     * 주요 정보: 1-Axis Positioner 각도/각속도 정보
     * 주요 사용처: Step, Slew, Pedestal Position
     */
    class SingleManualControl {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var axis: BitSet,
            var axisAngle: Float,
            var axisSpeed: Float,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(14)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // Float 값들을 바이트 배열로 변환 (엔디안 변환 포함)
                val byteAxisAngle = JKUtil.JKConvert.Companion.floatToByteArray(axisAngle, false)
                val byteAxisSpeed = JKUtil.JKConvert.Companion.floatToByteArray(axisSpeed, false)

                // BitSet을 바이트 배열로 변환
                val byteAxis = if (axis.isEmpty) {
                    0x00.toByte()
                } else {
                    axis.toByteArray().getOrElse(0) { 0x00.toByte() }
                }

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = byteAxis

                // 1축 각도 값
                dataFrame[3] = byteAxisAngle[0]
                dataFrame[4] = byteAxisAngle[1]
                dataFrame[5] = byteAxisAngle[2]
                dataFrame[6] = byteAxisAngle[3]

                // 1축 속도 값
                dataFrame[7] = byteAxisSpeed[0]
                dataFrame[8] = byteAxisSpeed[1]
                dataFrame[9] = byteAxisSpeed[2]
                dataFrame[10] = byteAxisSpeed[3]

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[11] = crc16Buffer[0]
                dataFrame[12] = crc16Buffer[1]
                dataFrame[13] = ICD_ETX
  
                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 6

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            ack = data[2],
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.17 M/C On/Off
     * 서보 모터의 구동 전원 제어용 M/C를 On/Off하기 위한 프로토콜이다.
     * 주요 정보: M/C On/Off 제어 정보
     * 주요 사용처: 서보 모터 전원 제어
     */
    class MCOnOff {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdOnOff: Boolean,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(6)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = if (cmdOnOff) 1 else 0

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[3] = crc16Buffer[0]
                dataFrame[4] = crc16Buffer[1]
                dataFrame[5] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var cmdOnOff: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 6

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            cmdOnOff = data[2],
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }

    /**
     * 2.16 Servo Alarm Reset
     * 서보 드라이버의 Alarm 초기화 명령을 송신하기 위한 프로토콜이다.
     * 주요 정보: 서보 알람 리셋 제어 정보
     * 주요 사용처: 서보 드라이버 알람 초기화
     */
    class ServoAlarmReset {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdTwo: Char,
            var axis: BitSet,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(7)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // BitSet을 바이트 배열로 변환
                val byteAxis = if (axis.isEmpty) {
                    0x00.toByte()
                } else {
                    axis.toByteArray().getOrElse(0) { 0x00.toByte() }
                }

                dataFrame[0] = ICD_STX
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()
                dataFrame[3] = byteAxis

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)

                // CRC16 값 설정
                dataFrame[4] = crc16Buffer[0]
                dataFrame[5] = crc16Buffer[1]
                dataFrame[6] = ICD_ETX

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Byte = 0x00,
            var cmdTwo: Byte = 0x00,
            var axis: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            companion object {
                const val FRAME_LENGTH = 8

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    // CRC 체크섬 추출 (리틀 엔디안)
                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
                        .short.toUShort()
                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    // CRC 검증 및 ETX 확인
                    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            cmdTwo = data[2],
                            axis = data[3],
                            ack = data[4],
                            checkSum = rxChecksum,
                            etx = data.last()
                        )
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }
    /**
     * 2.4 Read Firmware Version/Serial Number Info
     * 각 축의 Board F/W Version, Serial Number 정보를 수신 받기 위한 프로토콜이다.
     * 주요 정보: Board F/W Version, Serial Number
     * 주요 사용처: 설정모드
     */
    class ReadFwVerSerialNoStatus {
        data class SetDataFrame(
            var stx: Byte = ICD_STX,
            var cmdOne: Char,
            var cmdTwo: Char,
            var crc16: UShort = 0u,
            var etx: Byte = ICD_ETX
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(6)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = stx
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()

                System.arraycopy(dataFrame, 1, byteCrc16Target, 0, byteCrc16Target.size)
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKUtil.JKConvert.Companion.shortToByteArray(crc16s, false)
                
                dataFrame[3] = crc16Buffer[0]
                dataFrame[4] = crc16Buffer[1]
                dataFrame[5] = etx

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = 0,
            var commandOne: Byte = 0,
            var commandTwo: Byte = 0,
            
            // Main FW Version
            var mainFwVerBitAll: UInt = 0u,
            var mainFwVerReserved: Byte = 0,
            var mainFwVerOne: Byte = 0,
            var mainFwVerTwo: Byte = 0,
            var mainFwVerThree: Byte = 0,
            
            // Azimuth FW Version
            var azimuthFwVerBitAll: UInt = 0u,
            var azimuthFwVerReserved: Byte = 0,
            var azimuthFwVerOne: Byte = 0,
            var azimuthFwVerTwo: Byte = 0,
            var azimuthFwVerThree: Byte = 0,
            
            // Elevation FW Version
            var elevationFwVerBitAll: UInt = 0u,
            var elevationFwVerReserved: Byte = 0,
            var elevationFwVerOne: Byte = 0,
            var elevationFwVerTwo: Byte = 0,
            var elevationFwVerThree: Byte = 0,
            
            // Tilt FW Version
            var trainFwVerBitAll: UInt = 0u,
            var trainFwVerReserved: Byte = 0,
            var trainFwVerOne: Byte = 0,
            var trainFwVerTwo: Byte = 0,
            var trainFwVerThree: Byte = 0,
            
            // Feed FW Version
            var feedFwVerBitAll: UInt = 0u,
            var feedFwVerReserved: Byte = 0,
            var feedFwVerOne: Byte = 0,
            var feedFwVerTwo: Byte = 0,
            var feedFwVerThree: Byte = 0,
            
            // Main Serial Number
            var mainSerialBitAll: UInt = 0u,
            var mainSerialYear: Byte = 0,
            var mainSerialMonth: Byte = 0,
            var mainSerialNumber: UShort = 0u,
            
            // Azimuth Serial Number
            var azimuthSerialBitAll: UInt = 0u,
            var azimuthSerialYear: Byte = 0,
            var azimuthSerialMonth: Byte = 0,
            var azimuthSerialNumber: UShort = 0u,
            
            // Elevation Serial Number
            var elevationSerialBitAll: UInt = 0u,
            var elevationSerialYear: Byte = 0,
            var elevationSerialMonth: Byte = 0,
            var elevationSerialNumber: UShort = 0u,
            
            // Tilt Serial Number
            var trainSerialBitAll: UInt = 0u,
            var trainSerialYear: Byte = 0,
            var trainSerialMonth: Byte = 0,
            var trainSerialNumber: UShort = 0u,
            
            // Feed Serial Number
            var feedSerialBitAll: UInt = 0u,
            var feedSerialYear: Byte = 0,
            var feedSerialMonth: Byte = 0,
            var feedSerialNumber: UShort = 0u,
            
            var checkSum: UShort = 0u,
            var etx: Byte = 0
        ) {
            companion object {
                const val FRAME_LENGTH = 46

                fun fromByteArray(data: ByteArray): GetDataFrame? {
    if (data.size < FRAME_LENGTH) {
        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
        return null
    }

    // CRC 체크섬 추출 (리틀 엔디안)
    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 3], data[FRAME_LENGTH - 2]))
        .short.toUShort()
    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 3)
    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

    // CRC 검증 및 ETX 확인
    if (rxChecksum == crc16Check && data.last() == ICD_ETX) {
        // ✅ 올바른 펌웨어 버전 파싱
        val mainFwVer = (data[3].toUByte().toUInt() shl 24) or 
                       (data[4].toUByte().toUInt() shl 16) or 
                       (data[5].toUByte().toUInt() shl 8) or 
                       data[6].toUByte().toUInt()
        
        val azimuthFwVer = (data[7].toUByte().toUInt() shl 24) or 
                          (data[8].toUByte().toUInt() shl 16) or 
                          (data[9].toUByte().toUInt() shl 8) or 
                          data[10].toUByte().toUInt()
        
        val elevationFwVer = (data[11].toUByte().toUInt() shl 24) or 
                            (data[12].toUByte().toUInt() shl 16) or 
                            (data[13].toUByte().toUInt() shl 8) or 
                            data[14].toUByte().toUInt()
        
        val trainFwVer = (data[15].toUByte().toUInt() shl 24) or 
                       (data[16].toUByte().toUInt() shl 16) or 
                       (data[17].toUByte().toUInt() shl 8) or 
                       data[18].toUByte().toUInt()
        
        val feedFwVer = (data[19].toUByte().toUInt() shl 24) or 
                       (data[20].toUByte().toUInt() shl 16) or 
                       (data[21].toUByte().toUInt() shl 8) or 
                       data[22].toUByte().toUInt()
        
        // ✅ 올바른 시리얼 넘버 파싱
        val mainSerialNo = (data[23].toUByte().toUInt() shl 24) or
                          (data[24].toUByte().toUInt() shl 16) or
                          (data[25].toUByte().toUInt() shl 8) or
                          data[26].toUByte().toUInt()
        
        val azimuthSerialNo = (data[27].toUByte().toUInt() shl 24) or 
                             (data[28].toUByte().toUInt() shl 16) or 
                             (data[29].toUByte().toUInt() shl 8) or 
                             data[30].toUByte().toUInt()
        
        val elevationSerialNo = (data[31].toUByte().toUInt() shl 24) or 
                               (data[32].toUByte().toUInt() shl 16) or 
                               (data[33].toUByte().toUInt() shl 8) or 
                               data[34].toUByte().toUInt()
        
        val trainSerialNo = (data[35].toUByte().toUInt() shl 24) or 
                          (data[36].toUByte().toUInt() shl 16) or 
                          (data[37].toUByte().toUInt() shl 8) or 
                          data[38].toUByte().toUInt()
        
        val feedSerialNo = (data[39].toUByte().toUInt() shl 24) or 
                          (data[40].toUByte().toUInt() shl 16) or 
                          (data[41].toUByte().toUInt() shl 8) or 
                          data[42].toUByte().toUInt()
        
        return GetDataFrame(
            stx = data[0],
            commandOne = data[1],
            commandTwo = data[2],
            
            // ✅ 올바른 펌웨어 버전 설정
            mainFwVerBitAll = mainFwVer,
            mainFwVerReserved = data[3],
            mainFwVerOne = data[4],
            mainFwVerTwo = data[5],
            mainFwVerThree = data[6],
            
            azimuthFwVerBitAll = azimuthFwVer,
            azimuthFwVerReserved = data[7],
            azimuthFwVerOne = data[8],
            azimuthFwVerTwo = data[9],
            azimuthFwVerThree = data[10],
            
            elevationFwVerBitAll = elevationFwVer,
            elevationFwVerReserved = data[11],
            elevationFwVerOne = data[12],
            elevationFwVerTwo = data[13],
            elevationFwVerThree = data[14],
            
            trainFwVerBitAll = trainFwVer,
            trainFwVerReserved = data[15],
            trainFwVerOne = data[16],
            trainFwVerTwo = data[17],
            trainFwVerThree = data[18],
            
            feedFwVerBitAll = feedFwVer,
            feedFwVerReserved = data[19],
            feedFwVerOne = data[20],
            feedFwVerTwo = data[21],
            feedFwVerThree = data[22],
            
            // ✅ 올바른 시리얼 넘버 설정
            mainSerialBitAll = mainSerialNo,
            mainSerialYear = data[23],
            mainSerialMonth = data[24],
            mainSerialNumber = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[25], data[26])),
            
            azimuthSerialBitAll = azimuthSerialNo,
            azimuthSerialYear = data[27],
            azimuthSerialMonth = data[28],
            azimuthSerialNumber = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[29], data[30])),
            
            elevationSerialBitAll = elevationSerialNo,
            elevationSerialYear = data[31],
            elevationSerialMonth = data[32],
            elevationSerialNumber = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[33], data[34])),
            
            trainSerialBitAll = trainSerialNo,
            trainSerialYear = data[35],
            trainSerialMonth = data[36],
            trainSerialNumber = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[37], data[38])),
            
            feedSerialBitAll = feedSerialNo,
            feedSerialYear = data[39],
            feedSerialMonth = data[40],
            feedSerialNumber = JKUtil.JKConvert.byteArrayToUShort(byteArrayOf(data[41], data[42])),
            
            checkSum = rxChecksum,
            etx = data.last()
        )
    } else {
        println("CRC 체크 실패 또는 ETX 불일치")
        return null
    }
}
            }
        }
    }
}