package com.gtlsystems.acs_api.util

import java.nio.ByteOrder
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.BitSet

class JKUtil {
    class JKConvert {
        companion object {
            fun uintToByteArray(value: UInt, littleEndian: Boolean = true): ByteArray {
                return if (littleEndian) {
                    byteArrayOf(
                        value.toByte(),
                        (value shr 8).toByte(),
                        (value shr 16).toByte(),
                        (value shr 24).toByte(),
                        value.toByte()
                    )
                } else {
                    byteArrayOf(
                        (value shr 24).toByte(),
                        (value shr 16).toByte(),
                        (value shr 8).toByte(),
                        value.toByte()
                    )
                }
            }
            fun byteArrayToUInt(bytes: ByteArray, littleEndian: Boolean = true): Long {
                require(bytes.size == 4)
                val value = if (littleEndian) {
                    ((bytes[3].toLong() and 0xFF) shl 24) or
                            ((bytes[2].toLong() and 0xFF) shl 16) or
                            ((bytes[1].toLong() and 0xFF) shl 8) or
                            (bytes[0].toLong() and 0xFF)
                } else {
                    ((bytes[0].toLong() and 0xFF) shl 24) or
                            ((bytes[1].toLong() and 0xFF) shl 16) or
                            ((bytes[2].toLong() and 0xFF) shl 8) or
                            (bytes[3].toLong() and 0xFF)

                }
                return value and 0xFFFFFFFFL
            }
            /**
             * 바이트 배열을 INT 변환합니다 (리틀 엔디안 방식)
             * @param bytes 변환할 바이트 배열 (4바이트 길이)
             * @return 변환된 INT 값
             */
            fun byteArrayToInt(bytes: ByteArray, littleEndian: Boolean = true): Int {
                require(bytes.size == 4) { "바이트 배열의 크기는 4여야 합니다." }
                return if (littleEndian) {
                    ((bytes[3].toInt() and 0xFF) shl 24) or
                            ((bytes[2].toInt() and 0xFF) shl 16) or
                            ((bytes[1].toInt() and 0xFF) shl 8) or
                            (bytes[0].toInt() and 0xFF)
                } else {
                    ((bytes[0].toInt() and 0xFF) shl 24) or
                            ((bytes[1].toInt() and 0xFF) shl 16) or
                            ((bytes[2].toInt() and 0xFF) shl 8) or
                            (bytes[3].toInt() and 0xFF)

                }
            }
            /**
             * 바이트 배열을 UShort로 변환합니다 (리틀 엔디안 방식)
             * @param bytes 변환할 바이트 배열 (2바이트 길이)
             * @return 변환된 UShort 값
             */
            fun byteArrayToUShort(bytes: ByteArray): UShort {
                require(bytes.size >= 2) { "UShort 변환을 위해서는 최소 2바이트가 필요합니다." }

                return ByteBuffer.wrap(bytes)
                    .order(ByteOrder.BIG_ENDIAN)
                    .short.toUShort()
            }
            /**
             * Float 값을 ByteArray로 변환합니다.
             * @param value 변환할 Float 값
             * @param littleEndian true면 리틀 엔디안, false면 빅 엔디안 방식 사용
             * @return 4바이트 길이의 ByteArray
             */
            fun floatToByteArray(value: Float, littleEndian: Boolean = true): ByteArray {
                val buffer = ByteBuffer.allocate(4)
                buffer.order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
                buffer.putFloat(value)
                return buffer.array()
            }

            /**
             * ByteArray를 Float로 변환합니다.
             * @param bytes 변환할 바이트 배열 (4바이트 길이)
             * @param startIndex 시작 인덱스
             * @param littleEndian true면 리틀 엔디안, false면 빅 엔디안 방식 사용
             * @return 변환된 Float 값
             */
            fun byteArrayToFloat(bytes: ByteArray, startIndex: Int = 0, littleEndian: Boolean = true): Float {
                val buffer = ByteBuffer.allocate(4)
                buffer.order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
                buffer.put(bytes, startIndex, 4)
                buffer.flip()
                return buffer.float
            }

            /**
             * UShort를 ByteArray로 변환합니다.
             * @param value 변환할 UShort 값
             * @param littleEndian true면 리틀 엔디안, false면 빅 엔디안 방식 사용
             * @return 2바이트 길이의 ByteArray
             */
            fun ushortToByteArray(value: UShort, littleEndian: Boolean = true): ByteArray {
                val buffer = ByteBuffer.allocate(2)
                buffer.order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
                buffer.putShort(value.toShort())
                return buffer.array()
            }
            /**
             * Short를 ByteArray로 변환합니다.
             * @param value 변환할 Short 값
             * @param littleEndian true면 리틀 엔디안, false면 빅 엔디안 방식 사용
             * @return 2바이트 길이의 ByteArray
             */
            fun shortToByteArray(value: Short, littleEndian: Boolean = true): ByteArray {
                val buffer = ByteBuffer.allocate(2)
                buffer.order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
                buffer.putShort(value)
                return buffer.array()
            }
            // JKUtil.JKConvert 클래스에 다음 메서드 추가
            fun intToByteArray(value: Int, littleEndian: Boolean): ByteArray {
                val buffer = ByteBuffer.allocate(4)
                if (littleEndian) {
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                } else {
                    buffer.order(ByteOrder.BIG_ENDIAN)
                }
                buffer.putInt(value)
                return buffer.array()
            }
            fun uintEndianConvert(
                dataOne: Byte,
                dataTwo: Byte,
                dataThree: Byte,
                dataFour: Byte,
                littleEndian: Boolean
            ): UInt {
                val arr = byteArrayOf(dataOne, dataTwo, dataThree, dataFour)
                val buffer = ByteBuffer.wrap(arr)
                buffer.order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
                return buffer.int.toUInt()
            }

            fun intEndianConvert(
                dataOne: Byte,
                dataTwo: Byte,
                dataThree: Byte,
                dataFour: Byte,
                littleEndian: Boolean  = true
            ): Int {
                val arr = byteArrayOf(dataOne, dataTwo, dataThree, dataFour)
                return if (littleEndian) {
                    // 리틀 엔디안으로 읽기
                    ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).int
                } else {
                    // 빅 엔디안으로 읽기
                    ByteBuffer.wrap(arr).order(ByteOrder.BIG_ENDIAN).int

                }
            }
            // Hex 형태로 값 변경
            fun byteArrayToHexString(byteArray: ByteArray): String {
                return byteArray.joinToString("") { "%02X".format(it) }
            }
            private fun BitSet.toBinaryString(): String {
                val length = this.length()
                val sb = StringBuilder()
                for (i in (length - 1) downTo 0) {
                    sb.append(if (this[i]) '1' else '0')
                }
                return sb.toString().padStart(8, '0') // 최소 8자리로 패딩
            }
            fun byteToBinaryString(byte: Byte): String {
                return byte.toInt().and(0xFF).toString(2).padStart(8, '0')
            }
            fun byteArrayToBinaryString(byteArray: ByteArray): String {
                return byteArray.joinToString("") { byteToBinaryString(it) }
            }
        }
    }

    class JKTime {
        companion object {
            /**
             * UTC 시간에 지정된 시간(시간 단위)을 더한 결과를 반환합니다.
             *
             * @param utcTime UTC 기준 시간
             * @param hoursToAdd 더할 시간 (시간 단위)
             * @return 계산된 시간
             */
            fun addHoursToUtc(utcTime: ZonedDateTime, hoursToAdd: Int): ZonedDateTime {
                // 입력 시간이 UTC가 아니면 UTC로 변환
                val normalizedUtc = if (utcTime.zone != ZoneOffset.UTC) {
                    utcTime.withZoneSameInstant(ZoneOffset.UTC)
                } else {
                    utcTime
                }

                // 시간 더하기
                return normalizedUtc.plusHours(hoursToAdd.toLong())
            }

            /**
             * UTC 시간에 지정된 시간(초 단위)을 더한 결과를 반환합니다.
             *
             * @param utcTime UTC 기준 시간
             * @param secondsToAdd 더할 시간 (초 단위)
             * @return 계산된 시간
             */
            fun addSecondsToUtc(utcTime: ZonedDateTime, secondsToAdd: Int): ZonedDateTime {
                // 입력 시간이 UTC가 아니면 UTC로 변환
                val normalizedUtc = if (utcTime.zone != ZoneOffset.UTC) {
                    utcTime.withZoneSameInstant(ZoneOffset.UTC)
                } else {
                    utcTime
                }

                // 초 더하기
                return normalizedUtc.plusSeconds(secondsToAdd.toLong())
            }

            /**
             * UTC 시간에 지정된 시간(시간, 분, 초)을 더한 결과를 반환합니다.
             *
             * @param utcTime UTC 기준 시간
             * @param hours 더할 시간
             * @param minutes 더할 분
             * @param seconds 더할 초
             * @return 계산된 시간
             */
            fun addTimeToUtc(
                utcTime: ZonedDateTime,
                hours: Int = 0,
                minutes: Int = 0,
                seconds: Int = 0
            ): ZonedDateTime {
                // 입력 시간이 UTC가 아니면 UTC로 변환
                val normalizedUtc = if (utcTime.zone != ZoneOffset.UTC) {
                    utcTime.withZoneSameInstant(ZoneOffset.UTC)
                } else {
                    utcTime
                }

                // 시간, 분, 초 더하기
                return normalizedUtc
                    .plusHours(hours.toLong())
                    .plusMinutes(minutes.toLong())
                    .plusSeconds(seconds.toLong())
            }

            /**
             * 현재 UTC 시간에 지정된 시간을 더한 결과를 반환합니다.
             *
             * @param hoursToAdd 더할 시간 (시간 단위)
             * @return 계산된 시간
             */
            fun getCurrentTimeWithOffset(hoursToAdd: Int): ZonedDateTime {
                val currentUtc = ZonedDateTime.now(ZoneOffset.UTC)
                return currentUtc.plusHours(hoursToAdd.toLong())
            }

            /**
             * 시간 문자열을 지정된 오프셋이 적용된 ZonedDateTime으로 변환합니다.
             *
             * @param timeString ISO 형식의 시간 문자열 (예: "2025-05-21T03:42:42.4Z")
             * @param hoursToAdd 더할 시간 (시간 단위)
             * @return 계산된 ZonedDateTime
             */
            fun parseTimeWithOffset(timeString: String, hoursToAdd: Int): ZonedDateTime {
                // ISO 문자열을 UTC ZonedDateTime으로 파싱
                val utcTime = ZonedDateTime.parse(timeString)

                // 시간 더하기
                return utcTime.plusHours(hoursToAdd.toLong())
            }
        }
    }
}
