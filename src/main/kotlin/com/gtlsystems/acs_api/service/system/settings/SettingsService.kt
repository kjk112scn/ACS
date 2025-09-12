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
import kotlin.reflect.KProperty
import kotlin.properties.ReadWriteProperty

// LocationData 클래스를 SettingsService 밖으로 이동
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double
)

// 설정 정의를 위한 데이터 클래스
data class SettingDefinition(
    val key: String,
    val defaultValue: Any,
    val type: SettingType,
    val description: String
)

/**
 * 설정 관리 서비스
 * 프론트엔드에서 관리 가능한 설정값들을 관리
 */
@Service
class SettingsService(
    private val settingsRepository: SettingsRepository,
    private val settingsHistoryRepository: SettingsHistoryRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(SettingsService::class.java)
    private val settings = ConcurrentHashMap<String, Any>()

    // 설정 정의 통합 관리
    private val settingDefinitions = mapOf(
        // 위치 설정
        "location.latitude" to SettingDefinition("location.latitude", 35.317540, SettingType.DOUBLE, "위도"),
        "location.longitude" to SettingDefinition("location.longitude", 128.608510, SettingType.DOUBLE, "경도"),
        "location.altitude" to SettingDefinition("location.altitude", 0.0, SettingType.DOUBLE, "고도"),
        
        // 추적 설정
        "tracking.msInterval" to SettingDefinition("tracking.msInterval", 100, SettingType.INTEGER, "추적 간격"),
        "tracking.durationDays" to SettingDefinition("tracking.durationDays", 1L, SettingType.LONG, "추적 기간(일)"),
        "tracking.minElevationAngle" to SettingDefinition("tracking.minElevationAngle", -7f, SettingType.FLOAT, "최소 고도각"),
        
        // Stow Angle 설정
        "stow.angle.azimuth" to SettingDefinition("stow.angle.azimuth", 0.0, SettingType.DOUBLE, "Stow 방위각"),
        "stow.angle.elevation" to SettingDefinition("stow.angle.elevation", 90.0, SettingType.DOUBLE, "Stow 고도각"),
        "stow.angle.train" to SettingDefinition("stow.angle.train", 0.0, SettingType.DOUBLE, "Stow Train각"),
        
        // Stow Speed 설정
        "stow.speed.azimuth" to SettingDefinition("stow.speed.azimuth", 5.0, SettingType.DOUBLE, "Stow 방위각 속도"),
        "stow.speed.elevation" to SettingDefinition("stow.speed.elevation", 5.0, SettingType.DOUBLE, "Stow 고도각 속도"),
        "stow.speed.train" to SettingDefinition("stow.speed.train", 5.0, SettingType.DOUBLE, "Stow Train각 속도"),
        
        // AntennaSpec 설정
        "antennaspec.trueNorthOffsetAngle" to SettingDefinition("antennaspec.trueNorthOffsetAngle", 0.0, SettingType.DOUBLE, "True North Offset Angle"),
        "antennaspec.tiltAngle" to SettingDefinition("antennaspec.tiltAngle", -7.0, SettingType.DOUBLE, "Tilt Angle"),
        
        // Angle Limits 설정
        "anglelimits.azimuthMin" to SettingDefinition("anglelimits.azimuthMin", -270.0, SettingType.DOUBLE, "Azimuth 최소각"),
        "anglelimits.azimuthMax" to SettingDefinition("anglelimits.azimuthMax", 270.0, SettingType.DOUBLE, "Azimuth 최대각"),
        "anglelimits.elevationMin" to SettingDefinition("anglelimits.elevationMin", 0.0, SettingType.DOUBLE, "Elevation 최소각"),
        "anglelimits.elevationMax" to SettingDefinition("anglelimits.elevationMax", 180.0, SettingType.DOUBLE, "Elevation 최대각"),
        "anglelimits.trainMin" to SettingDefinition("anglelimits.trainMin", -270.0, SettingType.DOUBLE, "Train 최소각"),
        "anglelimits.trainMax" to SettingDefinition("anglelimits.trainMax", 270.0, SettingType.DOUBLE, "Train 최대각"),
        
        // Speed Limits 설정
        "speedlimits.azimuthMin" to SettingDefinition("speedlimits.azimuthMin", 0.1, SettingType.DOUBLE, "Azimuth 최소속도"),
        "speedlimits.azimuthMax" to SettingDefinition("speedlimits.azimuthMax", 15.0, SettingType.DOUBLE, "Azimuth 최대속도"),
        "speedlimits.elevationMin" to SettingDefinition("speedlimits.elevationMin", 0.1, SettingType.DOUBLE, "Elevation 최소속도"),
        "speedlimits.elevationMax" to SettingDefinition("speedlimits.elevationMax", 10.0, SettingType.DOUBLE, "Elevation 최대속도"),
        "speedlimits.trainMin" to SettingDefinition("speedlimits.trainMin", 0.1, SettingType.DOUBLE, "Train 최소속도"),
        "speedlimits.trainMax" to SettingDefinition("speedlimits.trainMax", 5.0, SettingType.DOUBLE, "Train 최대속도"),
        
        // Angle Offset Limits 설정
        "angleoffsetlimits.azimuth" to SettingDefinition("angleoffsetlimits.azimuth", 50.0, SettingType.DOUBLE, "Azimuth 오프셋 제한"),
        "angleoffsetlimits.elevation" to SettingDefinition("angleoffsetlimits.elevation", 50.0, SettingType.DOUBLE, "Elevation 오프셋 제한"),
        "angleoffsetlimits.train" to SettingDefinition("angleoffsetlimits.train", 50.0, SettingType.DOUBLE, "Train 오프셋 제한"),
        
        // Time Offset Limits 설정
        "timeoffsetlimits.min" to SettingDefinition("timeoffsetlimits.min", 0.1, SettingType.DOUBLE, "시간 오프셋 최소값"),
        "timeoffsetlimits.max" to SettingDefinition("timeoffsetlimits.max", 99999, SettingType.DOUBLE, "시간 오프셋 최대값"),
        
        // Algorithm 설정
        "algorithm.geoMinMotion" to SettingDefinition("algorithm.geoMinMotion", 1.1, SettingType.DOUBLE, "Geo Min Motion"),
        
        // StepSizeLimit 설정
        "stepsizelimit.min" to SettingDefinition("stepsizelimit.min", 50, SettingType.DOUBLE, "스텝 사이즈 최소값"),
        "stepsizelimit.max" to SettingDefinition("stepsizelimit.max", 50, SettingType.DOUBLE, "스텝 사이즈 최대값")
    )

    // 기본값과 타입 매핑 자동 생성
    private val defaultSettings = settingDefinitions.mapValues { it.value.defaultValue }
    private val settingTypes = settingDefinitions.mapValues { it.value.type }

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
                existingSetting.value = stringValue
                settingsRepository.save(existingSetting)
            } else {
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
     */
    private fun updateSetting(key: String, newValue: Any) {
        val oldValue = settings[key]

        if (oldValue == newValue) {
            logger.info("설정값 변경 없음: $key = $newValue (동일한 값)")
            return
        }

        logger.info("설정값 변경됨: $key = $oldValue → $newValue")
        settings[key] = newValue
        saveSettingToDatabase(key, newValue)
        publishSettingChangedEvent(key, oldValue, newValue)
    }

    /**
     * 설정 변경 이벤트 발행
     */
    private fun publishSettingChangedEvent(key: String, oldValue: Any?, newValue: Any) {
        try {
            val event = SettingsChangedEvent(
                key = key,
                value = newValue,
                userId = "system",
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

    /**
     * 제네릭 설정 프로퍼티 생성
     */
    private fun <T> createSettingProperty(key: String, description: String): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            @Suppress("UNCHECKED_CAST")
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return settings[key] as T
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                updateSetting(key, value as Any)
                logger.info("$description 변경: $value")
            }
        }
    }

    // 위치 관련 설정
    var latitude: Double by createSettingProperty("location.latitude", "위도")
    var longitude: Double by createSettingProperty("location.longitude", "경도")
    var altitude: Double by createSettingProperty("location.altitude", "고도")

    // 추적 관련 설정
    var msInterval: Int by createSettingProperty("tracking.msInterval", "추적 간격")
    var durationDays: Long by createSettingProperty("tracking.durationDays", "추적 기간")
    var minElevationAngle: Float by createSettingProperty("tracking.minElevationAngle", "최소 고도각")

    // Stow Angle 설정
    var stowAngleAzimuth: Double by createSettingProperty("stow.angle.azimuth", "Stow 방위각")
    var stowAngleElevation: Double by createSettingProperty("stow.angle.elevation", "Stow 고도각")
    var stowAngleTrain: Double by createSettingProperty("stow.angle.train", "Stow Train각")

    // Stow Speed 설정
    var stowSpeedAzimuth: Double by createSettingProperty("stow.speed.azimuth", "Stow 방위각 속도")
    var stowSpeedElevation: Double by createSettingProperty("stow.speed.elevation", "Stow 고도각 속도")
    var stowSpeedTrain: Double by createSettingProperty("stow.speed.train", "Stow Train각 속도")

    // AntennaSpec 설정
    var trueNorthOffsetAngle: Double by createSettingProperty("antennaspec.trueNorthOffsetAngle", "True North Offset Angle")
    var tiltAngle: Double by createSettingProperty("antennaspec.tiltAngle", "Tilt Angle")

    // Angle Limits 설정 (그룹명 포함으로 변경)
    var angleAzimuthMin: Double by createSettingProperty("anglelimits.azimuthMin", "Azimuth 최소각")
    var angleAzimuthMax: Double by createSettingProperty("anglelimits.azimuthMax", "Azimuth 최대각")
    var angleElevationMin: Double by createSettingProperty("anglelimits.elevationMin", "Elevation 최소각")
    var angleElevationMax: Double by createSettingProperty("anglelimits.elevationMax", "Elevation 최대각")
    var angleTrainMin: Double by createSettingProperty("anglelimits.trainMin", "Train 최소각")
    var angleTrainMax: Double by createSettingProperty("anglelimits.trainMax", "Train 최대각")

    // Speed Limits 설정
    var speedAzimuthMin: Double by createSettingProperty("speedlimits.azimuthMin", "Azimuth 최소속도")
    var speedAzimuthMax: Double by createSettingProperty("speedlimits.azimuthMax", "Azimuth 최대속도")
    var speedElevationMin: Double by createSettingProperty("speedlimits.elevationMin", "Elevation 최소속도")
    var speedElevationMax: Double by createSettingProperty("speedlimits.elevationMax", "Elevation 최대속도")
    var speedTrainMin: Double by createSettingProperty("speedlimits.trainMin", "Train 최소속도")
    var speedTrainMax: Double by createSettingProperty("speedlimits.trainMax", "Train 최대속도")

    // Angle Offset Limits 설정
    var angleOffsetAzimuth: Double by createSettingProperty("angleoffsetlimits.azimuth", "Azimuth 오프셋 제한")
    var angleOffsetElevation: Double by createSettingProperty("angleoffsetlimits.elevation", "Elevation 오프셋 제한")
    var angleOffsetTrain: Double by createSettingProperty("angleoffsetlimits.train", "Train 오프셋 제한")

    // Time Offset Limits 설정
    var timeOffsetMin: Double by createSettingProperty("timeoffsetlimits.min", "시간 오프셋 최소값")
    var timeOffsetMax: Double by createSettingProperty("timeoffsetlimits.max", "시간 오프셋 최대값")

    // Algorithm 설정
    var geoMinMotion: Double by createSettingProperty("algorithm.geoMinMotion", "Geo Min Motion")

    // StepSizeLimit 설정
    var stepSizeMin: Double by createSettingProperty("stepsizelimit.min", "스텝 사이즈 최소값")
    var stepSizeMax: Double by createSettingProperty("stepsizelimit.max", "스텝 사이즈 최대값")

    // LocationData 객체 제공
    val locationData: LocationData
        get() = LocationData(
            latitude = this.latitude,
            longitude = this.longitude,
            altitude = this.altitude
        )

    /**
     * 제네릭 일괄 설정 메서드
     */
    private fun setMultipleSettings(vararg pairs: Pair<String, Any>) {
        pairs.forEach { (key, value) ->
            updateSetting(key, value)
        }
        val keyNames = pairs.map { settingDefinitions[it.first]?.description ?: it.first }
        logger.info("설정 일괄 변경: ${keyNames.joinToString(", ")}")
    }

    // 일괄 설정 메서드들
    fun setLocation(lat: Double, lng: Double, alt: Double) {
        setMultipleSettings(
            "location.latitude" to lat,
            "location.longitude" to lng,
            "location.altitude" to alt
        )
    }

    fun setTracking(interval: Int, days: Long, minAngle: Float) {
        setMultipleSettings(
            "tracking.msInterval" to interval,
            "tracking.durationDays" to days,
            "tracking.minElevationAngle" to minAngle
        )
    }

    fun setStowAngles(azimuth: Double, elevation: Double, train: Double) {
        setMultipleSettings(
            "stow.angle.azimuth" to azimuth,
            "stow.angle.elevation" to elevation,
            "stow.angle.train" to train
        )
    }

    fun setStowSpeeds(azimuth: Double, elevation: Double, train: Double) {
        setMultipleSettings(
            "stow.speed.azimuth" to azimuth,
            "stow.speed.elevation" to elevation,
            "stow.speed.train" to train
        )
    }

    fun setStowAll(angleAzimuth: Double, angleElevation: Double, angleTrain: Double,
                   speedAzimuth: Double, speedElevation: Double, speedTrain: Double) {
        setMultipleSettings(
            "stow.angle.azimuth" to angleAzimuth,
            "stow.angle.elevation" to angleElevation,
            "stow.angle.train" to angleTrain,
            "stow.speed.azimuth" to speedAzimuth,
            "stow.speed.elevation" to speedElevation,
            "stow.speed.train" to speedTrain
        )
    }

    // AntennaSpec 설정 일괄 변경
    fun setAntennaSpec(trueNorthOffsetAngle: Double, tiltAngle: Double) {
        setMultipleSettings(
            "antennaspec.trueNorthOffsetAngle" to trueNorthOffsetAngle,
            "antennaspec.tiltAngle" to tiltAngle
        )
    }

    // Angle Limits 설정 일괄 변경
    fun setAngleLimits(azimuthMin: Double, azimuthMax: Double, elevationMin: Double, elevationMax: Double, trainMin: Double, trainMax: Double) {
        setMultipleSettings(
            "anglelimits.azimuthMin" to azimuthMin,
            "anglelimits.azimuthMax" to azimuthMax,
            "anglelimits.elevationMin" to elevationMin,
            "anglelimits.elevationMax" to elevationMax,
            "anglelimits.trainMin" to trainMin,
            "anglelimits.trainMax" to trainMax
        )
    }

    // 개별 축별 Angle Limits 설정 메서드
    fun setAngleAzimuthLimits(min: Double, max: Double) {
        setMultipleSettings(
            "anglelimits.azimuthMin" to min,
            "anglelimits.azimuthMax" to max
        )
    }

    fun setAngleElevationLimits(min: Double, max: Double) {
        setMultipleSettings(
            "anglelimits.elevationMin" to min,
            "anglelimits.elevationMax" to max
        )
    }

    fun setAngleTrainLimits(min: Double, max: Double) {
        setMultipleSettings(
            "anglelimits.trainMin" to min,
            "anglelimits.trainMax" to max
        )
    }

    // Speed Limits 설정 일괄 변경
    fun setSpeedLimits(azimuthMin: Double, azimuthMax: Double, elevationMin: Double, elevationMax: Double, trainMin: Double, trainMax: Double) {
        setMultipleSettings(
            "speedlimits.azimuthMin" to azimuthMin,
            "speedlimits.azimuthMax" to azimuthMax,
            "speedlimits.elevationMin" to elevationMin,
            "speedlimits.elevationMax" to elevationMax,
            "speedlimits.trainMin" to trainMin,
            "speedlimits.trainMax" to trainMax
        )
    }

    // 개별 축별 Speed Limits 설정 메서드
    fun setSpeedAzimuthLimits(min: Double, max: Double) {
        setMultipleSettings(
            "speedlimits.azimuthMin" to min,
            "speedlimits.azimuthMax" to max
        )
    }

    fun setSpeedElevationLimits(min: Double, max: Double) {
        setMultipleSettings(
            "speedlimits.elevationMin" to min,
            "speedlimits.elevationMax" to max
        )
    }

    fun setSpeedTrainLimits(min: Double, max: Double) {
        setMultipleSettings(
            "speedlimits.trainMin" to min,
            "speedlimits.trainMax" to max
        )
    }

    // Angle Offset Limits 설정 일괄 변경
    fun setAngleOffsetLimits(azimuth: Double, elevation: Double, train: Double) {
        setMultipleSettings(
            "angleoffsetlimits.azimuth" to azimuth,
            "angleoffsetlimits.elevation" to elevation,
            "angleoffsetlimits.train" to train
        )
    }

    // 개별 축별 Angle Offset Limits 설정 메서드
    fun setAngleOffsetAzimuthLimit(offset: Double) {
        setMultipleSettings(
            "angleoffsetlimits.azimuth" to offset
        )
    }

    fun setAngleOffsetElevationLimit(offset: Double) {
        setMultipleSettings(
            "angleoffsetlimits.elevation" to offset
        )
    }

    fun setAngleOffsetTrainLimit(offset: Double) {
        setMultipleSettings(
            "angleoffsetlimits.train" to offset
        )
    }

    // Time Offset Limits 설정 일괄 변경
    fun setTimeOffsetLimits(min: Double, max: Double) {
        setMultipleSettings(
            "timeoffsetlimits.min" to min,
            "timeoffsetlimits.max" to max
        )
    }

    // 개별 Time Offset Limits 설정 메서드
    fun setTimeOffsetMinLimit(min: Double) {
        setMultipleSettings(
            "timeoffsetlimits.min" to min
        )
    }

    fun setTimeOffsetMaxLimit(max: Double) {
        setMultipleSettings(
            "timeoffsetlimits.max" to max
        )
    }

    // Algorithm 설정 일괄 변경
    fun setAlgorithm(geoMinMotion: Double) {
        setMultipleSettings(
            "algorithm.geoMinMotion" to geoMinMotion
        )
    }

    // 개별 Algorithm 설정 메서드
    fun setGeoMinMotionLimit(value: Double) {
        setMultipleSettings(
            "algorithm.geoMinMotion" to value
        )
    }

    // StepSizeLimit 설정 일괄 변경
    fun setStepSizeLimit(min: Double, max: Double) {
        setMultipleSettings(
            "stepsizelimit.min" to min,
            "stepsizelimit.max" to max
        )
    }

    // 개별 StepSizeLimit 설정 메서드
    fun setStepSizeMinLimit(min: Double) {
        setMultipleSettings(
            "stepsizelimit.min" to min
        )
    }

    fun setStepSizeMaxLimit(max: Double) {
        setMultipleSettings(
            "stepsizelimit.max" to max
        )
    }

    // 모든 설정 조회
    fun getAll(): Map<String, Any> = settings.toMap()

    /**
     * 설정 그룹별 조회
     */
    fun getLocationSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("location.") }
    fun getTrackingSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("tracking.") }
    fun getStowAngleSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("stow.angle.") }
    fun getStowSpeedSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("stow.speed.") }
    fun getAntennaSpecSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("antennaspec.") }
    fun getAngleLimitsSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("anglelimits.") }
    fun getSpeedLimitsSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("speedlimits.") }
    fun getAngleOffsetLimitsSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("angleoffsetlimits.") }
    fun getTimeOffsetLimitsSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("timeoffsetlimits.") }
    fun getAlgorithmSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("algorithm.") }
    fun getStepSizeLimitSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("stepsizelimit.") }

    // 이벤트 리스너
    @EventListener
    fun handleSettingsChanged(event: SettingsChangedEvent) {
        val description = settingDefinitions[event.key]?.description ?: event.key
        val unit = when {
            event.key.contains("speed") || event.key.startsWith("speedlimits.") -> "도/초"
            event.key.contains("angle") || event.key.contains("elevation") || event.key.contains("azimuth") || event.key.contains("tilt") || event.key.contains("offset") || event.key.contains("min") || event.key.contains("max") -> "도"
            event.key.contains("msInterval") -> "ms"
            event.key.contains("durationDays") -> "일"
            event.key.startsWith("timeoffsetlimits.") -> "초"
            event.key.startsWith("algorithm.") -> "도/초"
            event.key.startsWith("stepsizelimit.") -> "도"
            else -> ""
        }
        
        logger.info("$description 변경됨: ${event.value}$unit")
        
        // 그룹별 처리 로직
        when {
            event.key.startsWith("location.") -> {
                // 위치 변경 시 처리 로직
            }
            event.key.startsWith("tracking.") -> {
                // 추적 설정 변경 시 처리 로직
            }
            event.key.startsWith("stow.") -> {
                // Stow 설정 변경 시 처리 로직
            }
            event.key.startsWith("antennaspec.") -> {
                // AntennaSpec 설정 변경 시 처리 로직
            }
            event.key.startsWith("anglelimits.") -> {
                // Angle Limits 설정 변경 시 처리 로직
            }
            event.key.startsWith("speedlimits.") -> {
                // Speed Limits 설정 변경 시 처리 로직
            }
            event.key.startsWith("angleoffsetlimits.") -> {
                // Angle Offset Limits 설정 변경 시 처리 로직
            }
            event.key.startsWith("timeoffsetlimits.") -> {
                // Time Offset Limits 설정 변경 시 처리 로직
            }
            event.key.startsWith("algorithm.") -> {
                // Algorithm 설정 변경 시 처리 로직
            }
            event.key.startsWith("stepsizelimit.") -> {
                // StepSizeLimit 설정 변경 시 처리 로직
            }
        }
    }
}