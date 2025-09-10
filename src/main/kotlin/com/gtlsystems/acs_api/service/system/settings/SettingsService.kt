package com.gtlsystems.acs_api.service.system.settings  
  
import com.gtlsystems.acs_api.settings.entity.Setting  
import com.gtlsystems.acs_api.settings.entity.SettingType  
import com.gtlsystems.acs_api.repository.interfaces.settings.SettingsHistoryRepository
import com.gtlsystems.acs_api.repository.interfaces.settings.SettingsRepository
import com.gtlsystems.acs_api.event.settings.SettingsChangedEvent  
import org.slf4j.LoggerFactory  
import org.springframework.stereotype.Service
import org.springframework.context.ApplicationEventPublisher  
import org.springframework.context.event.EventListener
import jakarta.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap  

// LocationData 클래스를 SettingsService 밖으로 이동
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double
)

/**
 * 설정 관리 서비스
 * 프론트엔드에서 관리 가능한 설정값들을 관리
 */
@Service
class SettingsService(
    private val settingsRepository: SettingsRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(SettingsService::class.java)
    private val settings = ConcurrentHashMap<String, Any>()

    // 위치 설정 키
    object Location {
        const val LATITUDE = "location.latitude"
        const val LONGITUDE = "location.longitude"
        const val ALTITUDE = "location.altitude"
    }

    // 추적 설정 키
    object Tracking {
        const val MS_INTERVAL = "tracking.msInterval"
        const val DURATION_DAYS = "tracking.durationDays"
        const val MIN_ELEVATION_ANGLE = "tracking.minElevationAngle"
    }

    // 기본값 정의
    private val defaultSettings = mapOf(
        Location.LATITUDE to 35.317540,
        Location.LONGITUDE to 128.608510,
        Location.ALTITUDE to 0.0,
        Tracking.MS_INTERVAL to 1000,
        Tracking.DURATION_DAYS to 7L,
        Tracking.MIN_ELEVATION_ANGLE to -7f
    )

    // 설정 타입 매핑
    private val settingTypes = mapOf(
        Location.LATITUDE to SettingType.DOUBLE,
        Location.LONGITUDE to SettingType.DOUBLE,
        Location.ALTITUDE to SettingType.DOUBLE,
        Tracking.MS_INTERVAL to SettingType.INTEGER,
        Tracking.DURATION_DAYS to SettingType.LONG,
        Tracking.MIN_ELEVATION_ANGLE to SettingType.FLOAT
    )

    @PostConstruct
    fun initialize() {
        // 1. 기본값으로 메모리 초기화
        settings.putAll(defaultSettings)

        // 2. DB에서 설정값 조회하여 메모리 업데이트
        loadSettingsFromDatabase()

        logger.info("설정 초기화 완료")
    }

    /**
     * DB에서 설정값을 조회하여 메모리에 로드
     */
    private fun loadSettingsFromDatabase() {
        try {
            val dbSettings = settingsRepository.findAll()
            dbSettings.forEach { setting ->
                val value = convertStringToValue(setting.value, setting.type)
                settings[setting.key] = value
                logger.info("DB에서 설정 로드: ${setting.key} = $value")
            }
        } catch (e: Exception) {
            logger.warn("DB에서 설정 로드 실패, 기본값 사용: ${e.message}")
        }
    }

    /**
     * 설정값을 DB에 저장
     */
    private fun saveSettingToDatabase(key: String, value: Any) {
        try {
            val type = settingTypes[key] ?: SettingType.STRING
            val existingSetting = settingsRepository.findByKey(key)
            val stringValue = value.toString()

            if (existingSetting != null) {
                // 기존 설정 업데이트
                existingSetting.value = stringValue
                settingsRepository.save(existingSetting)
            } else {
                // 새 설정 생성
                val newSetting = Setting(
                    key = key,
                    value = stringValue,
                    type = type,
                    isSystemSetting = false
                )
                settingsRepository.save(newSetting)
            }
            logger.info("DB에 설정 저장: $key = $stringValue")
        } catch (e: Exception) {
            logger.error("DB에 설정 저장 실패: $key = $value, ${e.message}")
        }
    }

    /**
     * 설정값 변경 (모든 설정에 공통 적용)
     * 값이 실제로 변경되었을 때만 DB 저장 및 이벤트 발행
     */
    private fun updateSetting(key: String, newValue: Any) {
        val oldValue = settings[key]

        // 디버그 로그 추가
        logger.info("값 비교: key=$key, oldValue=$oldValue (${oldValue?.javaClass}), newValue=$newValue (${newValue.javaClass})")

        if (oldValue == newValue) {
            logger.info("설정값 변경 없음: $key = $newValue (동일한 값)")
            return
        }

        logger.info("설정값 변경됨: $key = $oldValue → $newValue")
        settings[key] = newValue
        saveSettingToDatabase(key, newValue)
        publishSettingChangedEvent(key, oldValue, newValue)
    }
    // SettingsService.kt에서 이벤트 발행 부분 수정
    private fun publishSettingChangedEvent(key: String, oldValue: Any?, newValue: Any) {
        try {
            val event = SettingsChangedEvent(
                key = key,
                value = newValue,  // 기존 이벤트 구조에 맞춤
                userId = "system", // 시스템에서 변경된 경우
                timestamp = System.currentTimeMillis()
            )
            eventPublisher.publishEvent(event)
            logger.info("설정 변경 이벤트 발행: $key = $oldValue → $newValue")
        } catch (e: Exception) {
            logger.error("설정 변경 이벤트 발행 실패: ${e.message}")
        }
    }

    /**
     * 문자열을 적절한 타입으로 변환
     */
    private fun convertStringToValue(value: String, type: SettingType): Any {
        return when (type) {
            SettingType.STRING -> value
            SettingType.INTEGER -> value.toInt()
            SettingType.LONG -> value.toLong()
            SettingType.FLOAT -> value.toFloat()
            SettingType.DOUBLE -> value.toDouble()
            SettingType.BOOLEAN -> value.toBoolean()
        }
    }

    // 위치 관련 설정
    var latitude: Double
        get() = settings[Location.LATITUDE] as Double
        set(value) {
            updateSetting(Location.LATITUDE, value)
            logger.info("위도 변경: $value")
        }

    var longitude: Double
        get() = settings[Location.LONGITUDE] as Double
        set(value) {
            updateSetting(Location.LONGITUDE, value)
            logger.info("경도 변경: $value")
        }

    var altitude: Double
        get() = settings[Location.ALTITUDE] as Double
        set(value) {
            updateSetting(Location.ALTITUDE, value)
            logger.info("고도 변경: $value")
        }

    // 추적 관련 설정
    var msInterval: Int
        get() = settings[Tracking.MS_INTERVAL] as Int
        set(value) {
            updateSetting(Tracking.MS_INTERVAL, value)
            logger.info("추적 간격 변경: ${value}ms")
        }

    var durationDays: Long
        get() = settings[Tracking.DURATION_DAYS] as Long
        set(value) {
            updateSetting(Tracking.DURATION_DAYS, value)
            logger.info("추적 기간 변경: ${value}일")
        }

    var minElevationAngle: Float
        get() = settings[Tracking.MIN_ELEVATION_ANGLE] as Float
        set(value) {
            updateSetting(Tracking.MIN_ELEVATION_ANGLE, value)
            logger.info("최소 고도각 변경: ${value}도")
        }

    // LocationData 객체 제공
    val locationData: LocationData
        get() = LocationData(
            latitude = this.latitude,
            longitude = this.longitude,
            altitude = this.altitude
        )

    // 일괄 설정 메서드
    fun setLocation(lat: Double, lng: Double, alt: Double) {
        latitude = lat
        longitude = lng
        altitude = alt
        logger.info("위치 설정 변경: lat=$lat, lng=$lng, alt=$alt")
    }

    fun setTracking(interval: Int, days: Long, minAngle: Float) {
        msInterval = interval
        durationDays = days
        minElevationAngle = minAngle
        logger.info("추적 설정 변경: interval=${interval}ms, days=$days, minAngle=$minAngle")
    }

    // 모든 설정 조회
    fun getAll(): Map<String, Any> = settings.toMap()

    // 이벤트 리스너 (오류 수정)
    @EventListener
    fun handleSettingsChanged(event: SettingsChangedEvent) {
        when (event.key) {
            Location.LATITUDE, Location.LONGITUDE, Location.ALTITUDE -> {
                logger.info("위치 설정 변경됨: ${event.key} = ${event.value}")  // newValue → value
                // 위치 변경 시 다른 서비스에 알림 로직
            }

            Tracking.MS_INTERVAL -> {
                logger.info("추적 간격 변경됨: ${event.value}ms")  // newValue → value
                // 추적 간격 변경 시 처리 로직
            }

            Tracking.DURATION_DAYS -> {
                logger.info("추적 기간 변경됨: ${event.value}일")  // newValue → value
                // 추적 기간 변경 시 처리 로직
            }

            Tracking.MIN_ELEVATION_ANGLE -> {
                logger.info("최소 고도각 변경됨: ${event.value}도")  // newValue → value
                // 최소 고도각 변경 시 처리 로직
            }
        }
    }
}