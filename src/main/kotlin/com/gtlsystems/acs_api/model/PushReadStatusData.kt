package com.gtlsystems.acs_api.model

import java.time.LocalDateTime

class PushReadStatusData {
    data class ReadData(
        val timestamp: LocalDateTime = LocalDateTime.now(),
        val value: Int
    )
}