package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.util.Crc16
import com.gtlsystems.acs_api.util.JKUtil.JKConvert
import com.gtlsystems.acs_api.util.JKUtil
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.BitSet
import kotlin.UShortArray

@Service
class ICDService {
    companion object {
        var stx: Byte = 0x02
        var etx: Byte = 0x03
    }

    class Classify {
        fun receivedCmd(receiveData: ByteArray) {
            if (receiveData.size > 1 && receiveData[0] == 0x02.toByte()) {
                if (receiveData[1] == 'R'.code.toByte()) {
                    //2.2 Read Status
                    if (receiveData[2] == 'R'.code.toByte()) {
                        val parsedData = ReadStatus.GetDataFrame.fromByteArray(receiveData)
                        parsedData?.let {
                            println("파싱된 ICD 데이터: $it")
                        }
                    }
                    //2.3 Read Positioner Status
                    else if (receiveData[2] == 'P'.code.toByte()) {

                    }
                    //2.4 Read Firmware Version/Serial Number Info
                    else if (receiveData[2] == 'F'.code.toByte()) {

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

                }
                //2.8 Manual Controls Command(1-Axis)
                else if (receiveData[1] == 'A'.code.toByte()) {
                    val parsedData = MultiManualControl.GetDataFrame.fromByteArray(receiveData)
                    parsedData?.let {
                        println("파싱된 ICD 데이터: $it")
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

                    }
                    //2.12.2 위성 추적 초기 제어 명령
                    else if (receiveData[2] == 'M'.code.toByte()) {

                    }
                    //2.12.3 위성 추적 추가 데이터 요청
                    else if (receiveData[2] == 'R'.code.toByte()) {

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
     * 2.13 Time Offset Command
     * Time Offset 정보를 송신하기 위한 프로토콜이다.
     * 주요 정보: Time Offset
     * Time Offset 적용 대상: Ephemeris Designation, Pass Schedule, Sun Track
     * 설명: Request(ACU SW)는 ACU SW의 'NTP 시간 정보'를 전달하고, Response(ACU FW(Main Board))의' NTP 시간 정보'는 ACU FW의 NTP 시간 정보를 응답한다
     */
    class TimeOffset {
        data class SetDataFrame(
            var stx: Byte,
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
            var etx: Byte
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(19)

                val byteYear = JKConvert.ushortToByteArray(year, false)
                val byteMs = JKConvert.ushortToByteArray(ms, false)
                val byteTimeOffset = JKConvert.floatToByteArray(timeOffset, false)

                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = stx
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
                val crc16Buffer = JKConvert.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[16] = crc16Check[0]
                dataFrame[17] = crc16Check[1]
                dataFrame[18] = etx

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = 0x00,
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
            var etx: Byte = 0x00
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
                    if (rxChecksum == crc16Check && data.last() == etx) {
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
                            etx = data[FRAME_LENGTH - 1]
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
            var stx: Byte = ICDService.stx,
            var cmdOne: Char,
            var axis: BitSet,
            var azimuthAngle: Float,
            var azimuthSpeed: Float,
            var elevationAngle: Float,
            var elevationSpeed: Float,
            var tiltAngle: Float,
            var tiltSpeed: Float,
            var crc16: UShort = 0u,
            var etx: Byte = ICDService.etx
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(30)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // Float 값들을 바이트 배열로 변환 (엔디안 변환 포함)
                val byteAzimuthAngle = JKConvert.floatToByteArray(azimuthAngle, false)
                val byteAzimuthSpeed = JKConvert.floatToByteArray(azimuthSpeed, false)
                val byteElevationAngle = JKConvert.floatToByteArray(elevationAngle, false)
                val byteElevationSpeed = JKConvert.floatToByteArray(elevationSpeed, false)
                val byteTiltAngle = JKConvert.floatToByteArray(tiltAngle, false)
                val byteTiltSpeed = JKConvert.floatToByteArray(tiltSpeed, false)

                // BitSet을 바이트 배열로 변환
                val byteAxis = axis.toByteArray()[0]

                dataFrame[0] = stx
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
                val crc16Buffer = JKConvert.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[27] = crc16Check[0]
                dataFrame[28] = crc16Check[1]
                dataFrame[29] = etx

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = 0x00,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = 0x00
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
                    if (rxChecksum == crc16Check && data.last() == ICDService.etx) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            ack = data[2],
                            checkSum = rxChecksum,
                            etx = data[FRAME_LENGTH - 1]
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
            var stx: Byte = ICDService.stx,
            var cmdOne: Char,
            var feedOnOff: BitSet,
            var crc16: UShort = 0u,
            var etx: Byte = ICDService.etx
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(6)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // BitSet을 바이트 배열로 변환
                val byteAxis = feedOnOff.toByteArray()[0]

                dataFrame[0] = stx
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = byteAxis

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKConvert.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[3] = crc16Check[0]
                dataFrame[4] = crc16Check[1]
                dataFrame[5] = etx

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = 0x00,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = 0x00
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
                    if (rxChecksum == crc16Check && data.last() == etx) {
                        return GetDataFrame(
                            stx = data[0],
                            cmdOne = data[1],
                            ack = data[2],
                            checkSum = rxChecksum,
                            etx = data[FRAME_LENGTH - 1]
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
            var stx: Byte,
            var cmdOne: Char,
            var cmdTwo: Char,
            var azimuthOffset: Float,
            var elevationOffset: Float,
            var tiltOffset: Float,
            var crc16: UShort,
            var etx: Byte
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(18)

                // Float 값을 바이트 배열로 변환 (엔디안 변환 포함)
                val byteAzimuthOffset = JKConvert.floatToByteArray(azimuthOffset, false)
                val byteElevationOffset = JKConvert.floatToByteArray(elevationOffset, false)
                val byteTiltOffset = JKConvert.floatToByteArray(tiltOffset, false)

                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = stx
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
                val crc16Buffer = JKConvert.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[15] = crc16Check[0]
                dataFrame[16] = crc16Check[1]
                dataFrame[17] = etx

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte,
            var cmdOne: Byte,
            var cmdTwo: Byte = 0x00,
            var azimuthOffset: Float,
            var elevationOffset: Float,
            var tiltOffset: Float,
            var checkSum: UShort,
            var etx: Byte = 0x00
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
                    if (rxChecksum == crc16Check && data.last() == etx) {
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
                            etx = data[FRAME_LENGTH - 1]
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
            var stx: Byte,
            var cmdOne: Char,
            var axis: BitSet,
            var crc16: UShort,
            var etx: Byte
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(6)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                // BitSet을 바이트 배열로 변환
                val byteAxis = axis.toByteArray()[0]

                dataFrame[0] = stx
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = byteAxis

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKConvert.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[3] = crc16Check[0]
                dataFrame[4] = crc16Check[1]
                dataFrame[5] = etx

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = 0x00,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = 0x00
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
                    if (rxChecksum == crc16Check && data.last() == etx) {
                        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                        val frame = GetDataFrame()
                        frame.stx = buffer.get()
                        frame.cmdOne = buffer.get()
                        frame.ack = buffer.get()
                        frame.checkSum = rxChecksum
                        etx = buffer.get()
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
            var stx: Byte,
            var cmdOne: Char,
            var cmdOnOff: Boolean,
            var crc16: UShort,
            var etx: Byte
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(6)
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = stx
                dataFrame[1] = cmdOne.code.toByte()

                // cmdOnOff에 따라 'E' 또는 'S' 설정
                val setData = if (cmdOnOff) 'E' else 'S'
                dataFrame[2] = setData.code.toByte()

                // CRC 대상 복사
                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size)

                // CRC16 계산 및 엔디안 변환
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKConvert.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 값 설정
                dataFrame[3] = crc16Check[0]
                dataFrame[4] = crc16Check[1]
                dataFrame[5] = etx

                return dataFrame
            }
        }

        data class GetDataFrame(
            var stx: Byte = 0x00,
            var cmdOne: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = 0x00,
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
                    if (rxChecksum == crc16Check && data.last() == etx) {
                        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                        val frame = GetDataFrame()
                        frame.stx = buffer.get()
                        frame.cmdOne = buffer.get()
                        frame.checkSum = rxChecksum
                        etx = buffer.get()
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
    /// 송신 데이터 프레임 값 전송을 위한 배열 세팅 함수
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
            var tiltOffset: Float,
            var crc16: UShort
        ) {
            fun setDataFrame(): ByteArray {
                val dataFrame = ByteArray(30)
                val byteYear: ByteArray = JKUtil.JKConvert.ushortToByteArray(year, false)
                val byteMs: ByteArray = JKUtil.JKConvert.ushortToByteArray(ms, false)
                val byteTimeOffset: ByteArray = JKUtil.JKConvert.floatToByteArray(timeOffset, false)
                val byteAzimuthOffset: ByteArray = JKUtil.JKConvert.floatToByteArray(azimuthOffset, false);
                val byteElevationOffset: ByteArray = JKUtil.JKConvert.floatToByteArray(elevationOffset, false);
                val byteTiltOffset: ByteArray = JKUtil.JKConvert.floatToByteArray(tiltOffset, false);
                val byteCrc16Target = ByteArray(dataFrame.size - 4)

                dataFrame[0] = stx
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
                val crc16Buffer = JKConvert.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer
                // CRC16
                dataFrame[27] = crc16Check[0];
                dataFrame[28] = crc16Check[1];

                dataFrame[29] = etx;

                return dataFrame;
            }
        }

        /// <summary>
        /// 수신 데이터 프레임 구조
        /// </summary>
        data class GetDataFrame(
            var stx: Byte = 0x00,
            var cmdOne: Byte = 0x00,
            var ack: Byte = 0x00,
            var checkSum: UShort = 0u,
            var etx: Byte = 0x00
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
                    if (rxChecksum == crc16Check && data.last() == etx) {
                        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                        val frame = GetDataFrame()
                        frame.stx = buffer.get()
                        frame.cmdOne = buffer.get()
                        frame.ack = buffer.get()
                        frame.checkSum = rxChecksum;
                        etx = buffer.get()
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

                dataFrame[0] = stx
                dataFrame[1] = cmdOne.code.toByte()
                dataFrame[2] = cmdTwo.code.toByte()
                dataFrame[3] = mode

                dataFrame.copyInto(byteCrc16Target, 0, 1, 1 + byteCrc16Target.size) // destinationOffset을 0으로 수정
                val crc16s = Crc16.computeCrc(byteCrc16Target)
                val crc16Buffer = JKConvert.shortToByteArray(crc16s, false)
                val crc16Check = crc16Buffer

                // CRC16 (Little Endian 가정)
                dataFrame[4] = crc16Check[0]
                dataFrame[5] = crc16Check[1]

                dataFrame[6] = etx

                return dataFrame
            }
        }


        data class GetDataFrame(
            var stx: Byte = 0x00,
            var cmdOne: Byte = 0x00,
            var cmdTwo: Byte = 0x00,
            var modeStatusBits: BitSet = BitSet(8),
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
            var mainBoardProtocolStatusBits: BitSet = BitSet(8),
            var mainBoardStatusBits: BitSet = BitSet(8),
            var mainBoardMCOnOffBits: BitSet = BitSet(8),
            var mainBoardReserveBits: BitSet = BitSet(8),
            var azimuthBoardServoStatusBits: BitSet = BitSet(8),
            var azimuthBoardStatusBits: BitSet = BitSet(8),
            var elevationBoardServoStatusBits: BitSet = BitSet(8),
            var elevationBoardStatusBits: BitSet = BitSet(8),
            var tiltBoardServoStatusBits: BitSet = BitSet(8),
            var tiltBoardStatusBits: BitSet = BitSet(8),
            var feedSBoardStatusBits: BitSet = BitSet(8),
            var feedXBoardStatusBits: BitSet = BitSet(8),
            var currentSBandLNA_LHCP: Float = 0f,
            var currentSBandLNA_RHCP: Float = 0f,
            var currentXBandLNA_LHCP: Float = 0f,
            var currentXBandLNA_RHCP: Float = 0f,
            var rssiSBandLNA_LHCP: Float = 0f,
            var rssiSBandLNA_RHCP: Float = 0f,
            var rssiXBandLNA_LHCP: Float = 0f,
            var rssiXBandLNA_RHCP: Float = 0f,
            var checkSum: UShort = 0u,
            var etx: Byte = 0x00
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

            enum class FeedBoardSBandStatus {
                s_Band_LHCP_ON,
                s_Band_LHCP_Error,
                s_Band_RHCP_ON,
                s_Band_RHCP_Error,
                s_Band_RF_ON,
                s_Band_RF_Error
            }

            enum class FeedBoardXBandStatus(val value: Int) {
                X_BAND_LHCP_ON(0),
                X_BAND_LHCP_ERROR(1),
                X_BAND_RHCP_ON(2),
                X_BAND_RHCP_ERROR(3),
                RESERVE1(4),
                RESERVE2(5),
                FAN_ON(6),
                FAN_ERROR(7);
            }

            companion object {
                const val FRAME_LENGTH = 172

                fun fromByteArray(data: ByteArray): GetDataFrame? {
                    if (data.size < FRAME_LENGTH) {
                        println("수신 데이터 길이가 프레임 길이보다 짧습니다: ${data.size} < $FRAME_LENGTH")
                        return null
                    }

                    val rxChecksum = ByteBuffer.wrap(byteArrayOf(data[FRAME_LENGTH - 2], data[FRAME_LENGTH - 1]))
                        .short.toUShort()

                    val crc16Target = data.copyOfRange(1, FRAME_LENGTH - 2)
                    val crc16Check = Crc16.computeCrc(crc16Target).toUShort()

                    if (rxChecksum == crc16Check && data.last() == etx) {
                        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

                        val frame = GetDataFrame()
                        frame.stx = buffer.get()
                        frame.cmdOne = buffer.get()
                        frame.cmdTwo = buffer.get()

                        val modeStatusByte = buffer.get()
                        frame.modeStatusBits =
                            BitSet.valueOf(byteArrayOf(modeStatusByte))

                        frame.azimuthAngle = buffer.float
                        frame.elevationAngle = buffer.float
                        frame.tiltAngle = buffer.float
                        frame.azimuthSpeed = buffer.float
                        frame.elevationSpeed = buffer.float
                        frame.tiltSpeed = buffer.float

                        // Main Board Status (4 bytes) - 순서 확인 필요
                        val mainBoardReserveByte = buffer.get()
                        frame.mainBoardReserveBits =
                            BitSet.valueOf(byteArrayOf(mainBoardReserveByte))
                        val mainBoardMCOnOffByte = buffer.get()
                        frame.mainBoardMCOnOffBits =
                            BitSet.valueOf(byteArrayOf(mainBoardMCOnOffByte))
                        val mainBoardStatusByte = buffer.get()
                        frame.mainBoardStatusBits =
                            BitSet.valueOf(byteArrayOf(mainBoardStatusByte))
                        val mainBoardProtocolStatusByte = buffer.get()
                        frame.mainBoardProtocolStatusBits =
                            BitSet.valueOf(byteArrayOf(mainBoardProtocolStatusByte))

                        // Azimuth Board Status (2 bytes)
                        val azimuthBoardStatusByte = buffer.get()
                        frame.azimuthBoardStatusBits =
                            BitSet.valueOf(byteArrayOf(azimuthBoardStatusByte))
                        val azimuthBoardServoStatusByte = buffer.get()
                        frame.azimuthBoardServoStatusBits =
                            BitSet.valueOf(byteArrayOf(azimuthBoardServoStatusByte))

                        // Elevation Board Status (2 bytes)
                        val elevationBoardStatusByte = buffer.get()
                        frame.elevationBoardStatusBits =
                            BitSet.valueOf(byteArrayOf(elevationBoardStatusByte))
                        val elevationBoardServoStatusByte = buffer.get()
                        frame.elevationBoardServoStatusBits =
                            BitSet.valueOf(byteArrayOf(elevationBoardServoStatusByte))

                        // Tilt Board Status (2 bytes)
                        val tiltBoardStatusByte = buffer.get()
                        frame.tiltBoardStatusBits =
                            BitSet.valueOf(byteArrayOf(tiltBoardStatusByte))
                        val tiltBoardServoStatusByte = buffer.get()
                        frame.tiltBoardServoStatusBits =
                            BitSet.valueOf(byteArrayOf(tiltBoardServoStatusByte))

                        // Feed Board Status (2 bytes) - 순서 및 데이터 해석 방식 확인 필요
                        val feedXBoardStatusByte = buffer.get()
                        frame.feedXBoardStatusBits =
                            BitSet.valueOf(byteArrayOf(feedXBoardStatusByte))
                        val feedSBoardStatusByte = buffer.get()
                        frame.feedSBoardStatusBits =
                            BitSet.valueOf(byteArrayOf(feedSBoardStatusByte))

                        frame.currentSBandLNA_LHCP = buffer.float
                        frame.currentSBandLNA_RHCP = buffer.float
                        frame.currentXBandLNA_LHCP = buffer.float
                        frame.currentXBandLNA_RHCP = buffer.float
                        frame.rssiSBandLNA_LHCP = buffer.float
                        frame.rssiSBandLNA_RHCP = buffer.float
                        frame.rssiXBandLNA_LHCP = buffer.float
                        frame.rssiXBandLNA_RHCP = buffer.float

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

                        frame.checkSum = rxChecksum
                        frame.etx = buffer.get()

                        return frame
                    } else {
                        println("CRC 체크 실패 또는 ETX 불일치")
                        return null
                    }
                }
            }
        }
    }
}



