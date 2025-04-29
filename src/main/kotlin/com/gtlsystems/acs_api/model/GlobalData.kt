package com.gtlsystems.acs_api.model

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class GlobalData {
    data class Time(
        val utcNow: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        val localNow: LocalDateTime = LocalDateTime.now(),
        val addLocalTime: Int = 0
    )
}