package com.gtlsystems.acs_api.util

import java.nio.ByteOrder
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class JKUtil {
    class JKConvert {
        companion object {
            /**
             * 바이트 배열을 UShort로 변환합니다 (리틀 엔디안 방식)
             * @param bytes 변환할 바이트 배열 (2바이트 길이)
             * @return 변환된 UShort 값
             */
            fun byteArrayToUShort(bytes: ByteArray): UShort {
                require(bytes.size >= 2) { "UShort 변환을 위해서는 최소 2바이트가 필요합니다." }

                return ByteBuffer.wrap(bytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
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
            // Hex 형태로 값 변경
            fun byteArrayToHexString(byteArray: ByteArray): String {
                return byteArray.joinToString("") { "%02X".format(it) }
            }

        }
    }

    class JKTime {
        companion object {
            val utcNow: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)
            val localNow: LocalDateTime = LocalDateTime.now()
            val addLocalTime: Int = 0
            val calLocalTime = utcNow.plusHours(addLocalTime.toLong())

        }
    }
}
