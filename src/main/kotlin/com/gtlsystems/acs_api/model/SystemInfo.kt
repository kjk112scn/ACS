package com.gtlsystems.acs_api.model

/**
 * 시스템 정보 데이터 클래스
 * Firmware Version, Serial Number 등 설정 정보 저장
 */
object SystemInfo {
    data class FirmwareVersionSerialNoData(
        // Main Board F/W Version
        val mainFwVerBitAll: UInt? = null,
        val mainFwVerReserved: Byte? = null,
        val mainFwVerOne: Byte? = null,
        val mainFwVerTwo: Byte? = null,
        val mainFwVerThree: Byte? = null,
        
        // Azimuth Board F/W Version
        val azimuthFwVerBitAll: UInt? = null,
        val azimuthFwVerReserved: Byte? = null,
        val azimuthFwVerOne: Byte? = null,
        val azimuthFwVerTwo: Byte? = null,
        val azimuthFwVerThree: Byte? = null,
        
        // Elevation Board F/W Version
        val elevationFwVerBitAll: UInt? = null,
        val elevationFwVerReserved: Byte? = null,
        val elevationFwVerOne: Byte? = null,
        val elevationFwVerTwo: Byte? = null,
        val elevationFwVerThree: Byte? = null,
        
        // Tilt Board F/W Version
        val trainFwVerBitAll: UInt? = null,
        val trainFwVerReserved: Byte? = null,
        val trainFwVerOne: Byte? = null,
        val trainFwVerTwo: Byte? = null,
        val trainFwVerThree: Byte? = null,
        
        // Feed Board F/W Version
        val feedFwVerBitAll: UInt? = null,
        val feedFwVerReserved: Byte? = null,
        val feedFwVerOne: Byte? = null,
        val feedFwVerTwo: Byte? = null,
        val feedFwVerThree: Byte? = null,
        
        // Main Board Serial Number
        val mainSerialBitAll: UInt? = null,
        val mainSerialYear: Byte? = null,
        val mainSerialMonth: Byte? = null,
        val mainSerialNumber: UShort? = null,
        
        // Azimuth Board Serial Number
        val azimuthSerialBitAll: UInt? = null,
        val azimuthSerialYear: Byte? = null,
        val azimuthSerialMonth: Byte? = null,
        val azimuthSerialNumber: UShort? = null,
        
        // Elevation Board Serial Number
        val elevationSerialBitAll: UInt? = null,
        val elevationSerialYear: Byte? = null,
        val elevationSerialMonth: Byte? = null,
        val elevationSerialNumber: UShort? = null,
        
        // Tilt Board Serial Number
        val trainSerialBitAll: UInt? = null,
        val trainSerialYear: Byte? = null,
        val trainSerialMonth: Byte? = null,
        val trainSerialNumber: UShort? = null,
        
        // Feed Board Serial Number
        val feedSerialBitAll: UInt? = null,
        val feedSerialYear: Byte? = null,
        val feedSerialMonth: Byte? = null,
        val feedSerialNumber: UShort? = null,
    )

    // ✅ 인스턴스 객체
    var FIRMWARE_VERSION_SERIAL_NO = FirmwareVersionSerialNoData()
}

