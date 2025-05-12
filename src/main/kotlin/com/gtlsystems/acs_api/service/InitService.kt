package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.model.GlobalData
import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.time.DateUtils.addHours
import java.time.ZonedDateTime
import org.springframework.stereotype.Service

@Service  // 이 어노테이션을 추가하여 Spring Bean으로 등록
class InitService {
    @PostConstruct
    /*
    DB 서버에서 초기 설정 정보를 입력하기 위함
    초기 동작해야하는 로직이 있으면 이곳에 추가
     */
    fun init() {
        val addHour = 9
        setGlobalData(addHour)
        GlobalData.Location.latitude = 37.566535
        GlobalData.Location.longitude = 126.9779692
        println("InitService init() called")
        println("utcNow: ${GlobalData.Time.utcNow}")
        println("localNow: ${GlobalData.Time.localNow}")
        println("addLocalTime: ${GlobalData.Time.addLocalTime}")
        println("ServerTime: ${GlobalData.Time.serverTime}")
        println("resultTimeOffsetCalTime: ${GlobalData.Time.resultTimeOffsetCalTime}")
    }

    fun setGlobalData(addHour: Int) {
        GlobalData.Time.addLocalTime = addHour
    }
}
