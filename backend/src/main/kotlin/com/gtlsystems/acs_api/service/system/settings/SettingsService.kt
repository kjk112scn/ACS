package com.gtlsystems.acs_api.service.system.settings

import com.gtlsystems.acs_api.settings.entity.Setting
import com.gtlsystems.acs_api.settings.entity.SettingHistory
import com.gtlsystems.acs_api.settings.entity.SettingType
import com.gtlsystems.acs_api.repository.interfaces.settings.SettingsHistoryRepository
import com.gtlsystems.acs_api.repository.interfaces.settings.SettingsRepository
import com.gtlsystems.acs_api.event.settings.SettingsChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import jakarta.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
 *
 * DB 사용 여부:
 * - no-db 프로필: RAM만 사용, Repository는 null
 * - office/home 프로필: R2DBC 사용, Repository 활성화
 */
@Service
class SettingsService(
    private val settingsRepository: SettingsRepository?,
    private val settingsHistoryRepository: SettingsHistoryRepository?,
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
        "tracking.minElevationAngle" to SettingDefinition("tracking.minElevationAngle", 0f, SettingType.FLOAT, "최소 고도각"),
        
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
        "stepsizelimit.max" to SettingDefinition("stepsizelimit.max", 50, SettingType.DOUBLE, "스텝 사이즈 최대값"),
        
        // Feed 설정
        "feed.enabledBands" to SettingDefinition("feed.enabledBands", "[\"s\",\"x\"]", SettingType.STRING, "피드 밴드 표시 설정 (S-Band, X-Band, Ka-Band)"),

        // 기존 설정들 뒤에 추가할 시스템 설정들
        // === ConfigurationService에서 가져올 시스템 설정 ===
        // UDP 통신 설정
        "system.udp.receiveInterval" to SettingDefinition("system.udp.receiveInterval", 10L, SettingType.LONG, "UDP 수신 간격"),
        "system.udp.sendInterval" to SettingDefinition("system.udp.sendInterval", 10L, SettingType.LONG, "UDP 전송 간격"),
        "system.udp.timeout" to SettingDefinition("system.udp.timeout", 25L, SettingType.LONG, "UDP 타임아웃"),
        "system.udp.reconnectInterval" to SettingDefinition("system.udp.reconnectInterval", 1000L, SettingType.LONG, "UDP 재연결 간격"),
        "system.udp.maxBufferSize" to SettingDefinition("system.udp.maxBufferSize", 1024, SettingType.INTEGER, "UDP 최대 버퍼 크기"),
        "system.udp.commandDelay" to SettingDefinition("system.udp.commandDelay", 100L, SettingType.LONG, "UDP 명령 지연"),

        // 추적 설정
        "system.tracking.interval" to SettingDefinition("system.tracking.interval", 100L, SettingType.LONG, "추적 간격"),
        "system.tracking.transmissionInterval" to SettingDefinition("system.tracking.transmissionInterval", 100L, SettingType.LONG, "전송 간격"),
        "system.tracking.fineInterval" to SettingDefinition("system.tracking.fineInterval", 100L, SettingType.LONG, "정밀 계산 간격"),
        "system.tracking.coarseInterval" to SettingDefinition("system.tracking.coarseInterval", 1000L, SettingType.LONG, "일반 계산 간격"),
        "system.tracking.stabilizationTimeout" to SettingDefinition("system.tracking.stabilizationTimeout", 5000L, SettingType.LONG, "안정화 타임아웃"),

        // 데이터 저장 설정
        "system.storage.batchSize" to SettingDefinition("system.storage.batchSize", 1000, SettingType.INTEGER, "배치 크기"),
        "system.storage.saveInterval" to SettingDefinition("system.storage.saveInterval", 100L, SettingType.LONG, "저장 간격"),
        "system.storage.progressLogInterval" to SettingDefinition("system.storage.progressLogInterval", 1000, SettingType.INTEGER, "진행률 로깅 간격"),

        // === 태양 추적 정확도 임계값 설정 ===
        "system.suntrack.highAccuracyThreshold" to SettingDefinition("system.suntrack.highAccuracyThreshold", 0.000278, SettingType.DOUBLE, "태양 추적 높은 정확도 임계값"),
        "system.suntrack.mediumAccuracyThreshold" to SettingDefinition("system.suntrack.mediumAccuracyThreshold", 0.002778, SettingType.DOUBLE, "태양 추적 중간 정확도 임계값"),
        "system.suntrack.lowAccuracyThreshold" to SettingDefinition("system.suntrack.lowAccuracyThreshold", 0.016667, SettingType.DOUBLE, "태양 추적 낮은 정확도 임계값"),
        "system.suntrack.searchHours" to SettingDefinition("system.suntrack.searchHours", 48.0, SettingType.DOUBLE, "태양 추적 검색 시간"),

        // WebSocket 전송 간격 설정
        "system.websocket.transmissionInterval" to SettingDefinition("system.websocket.transmissionInterval", 30L, SettingType.LONG, "WebSocket 전송 간격"),
        "system.performance.threshold" to SettingDefinition("system.performance.threshold", 100L, SettingType.LONG, "성능 임계값"),

        // 성능 등급 기준 설정
        "system.performance.ultraCores" to SettingDefinition("system.performance.ultraCores", 8, SettingType.INTEGER, "ULTRA 등급 최소 CPU 코어 수"),
        "system.performance.highCores" to SettingDefinition("system.performance.highCores", 6, SettingType.INTEGER, "HIGH 등급 최소 CPU 코어 수"),
        "system.performance.mediumCores" to SettingDefinition("system.performance.mediumCores", 4, SettingType.INTEGER, "MEDIUM 등급 최소 CPU 코어 수"),
        "system.performance.ultraMemory" to SettingDefinition("system.performance.ultraMemory", 8L, SettingType.LONG, "ULTRA 등급 최소 메모리(GB)"),
        "system.performance.highMemory" to SettingDefinition("system.performance.highMemory", 4L, SettingType.LONG, "HIGH 등급 최소 메모리(GB)"),
        "system.performance.mediumMemory" to SettingDefinition("system.performance.mediumMemory", 2L, SettingType.LONG, "MEDIUM 등급 최소 메모리(GB)"),

        // JVM 튜닝 설정
        "system.jvm.gcPause" to SettingDefinition("system.jvm.gcPause", 10L, SettingType.LONG, "GC 일시정지 시간 (ms)"),
        "system.jvm.heapRegionSize" to SettingDefinition("system.jvm.heapRegionSize", 16L, SettingType.LONG, "힙 영역 크기 (MB)"),
        "system.jvm.concurrentThreads" to SettingDefinition("system.jvm.concurrentThreads", 4L, SettingType.LONG, "동시 스레드 수"),
        "system.jvm.parallelThreads" to SettingDefinition("system.jvm.parallelThreads", 8L, SettingType.LONG, "병렬 스레드 수"),

        // === Ephemeris Tracking 설정 ===
        "ephemeris.tracking.sourceMinElevationAngle" to SettingDefinition("ephemeris.tracking.sourceMinElevationAngle", 0.0, SettingType.DOUBLE, "원본 2축 위성 추적 데이터 생성 시 최소 Elevation 각도 (도). Orekit 계산 시 사용되는 2축 좌표계 기준. Tilt 각도 보정을 위해 음수 값 허용. 권장 공식: -abs(tiltAngle) - 15도 (예: Tilt -7° → -abs(-7) - 15 = -22.0°). 사용자가 수동으로 계산하여 설정해야 함."),
        "ephemeris.tracking.keyholeAzimuthVelocityThreshold" to SettingDefinition("ephemeris.tracking.keyholeAzimuthVelocityThreshold", 10.0, SettingType.DOUBLE, "KEYHOLE 위성 판단을 위한 Azimuth 각속도 임계값 (도/초). 전체 추적 구간에서 최대 Azimuth 각속도가 이 값 이상이면 KEYHOLE 위성으로 판단. KEYHOLE 위성은 Train 각도를 적용하여 ±270° 영역을 회피함. 권장값: 3.0~10.0 (낮을수록 보수적)."),
    )

    // 기본값과 타입 매핑 자동 생성
    private val defaultSettings = settingDefinitions.mapValues { it.value.defaultValue }
    private val settingTypes = settingDefinitions.mapValues { it.value.type }

    @PostConstruct
    fun initialize() {
        // 1. 기본값으로 메모리 초기화
        settings.putAll(defaultSettings)

        // 2. DB에서 설정값 조회하여 메모리 업데이트 (DB 사용 시에만)
        if (settingsRepository != null) {
            loadSettingsFromDatabase()
            logger.info("설정 초기화 완료 (DB 모드)")
        } else {
            logger.info("설정 초기화 완료 (RAM 전용 모드)")
        }
    }

    /**
     * DB에서 설정값을 조회하여 메모리에 로드 (R2DBC)
     */
    private fun loadSettingsFromDatabase() {
        val repo = settingsRepository ?: run {
            logger.warn("DB 미사용 모드: 기본값 사용")
            return
        }

        try {
            val dbSettings = repo.findAll().collectList().block() ?: emptyList()
            dbSettings.forEach { setting ->
                val value = convertStringToValue(setting.value, setting.type)
                settings[setting.key] = value
                logger.info("DB에서 설정 로드: ${setting.key} = $value")
            }
            logger.info("DB에서 설정 ${dbSettings.size}개 로드 완료")
        } catch (e: Exception) {
            logger.warn("DB에서 설정 로드 실패, 기본값 사용: ${e.message}")
        }
    }

    /**
     * 설정값을 DB에 저장 (R2DBC)
     */
    private fun saveSettingToDatabase(key: String, value: Any) {
        val repo = settingsRepository ?: run {
            logger.debug("DB 미사용 모드: 설정은 RAM에만 저장됨")
            return
        }

        try {
            val type = settingTypes[key] ?: SettingType.STRING
            val description = settingDefinitions[key]?.description
            val stringValue = value.toString()
            val now = OffsetDateTime.now(ZoneOffset.UTC)

            // 기존 설정 조회
            val existingSetting = repo.findByKey(key).block()

            if (existingSetting != null) {
                // 기존 설정 업데이트
                val updatedSetting = existingSetting.copy(
                    value = stringValue,
                    updatedAt = now
                )
                repo.save(updatedSetting).block()
            } else {
                // 새 설정 생성
                val newSetting = Setting(
                    key = key,
                    value = stringValue,
                    type = type,
                    description = description,
                    isSystemSetting = false,
                    createdAt = now,
                    updatedAt = now
                )
                repo.save(newSetting).block()
            }
            logger.info("DB에 설정 저장: $key = $stringValue")
        } catch (e: Exception) {
            logger.error("DB에 설정 저장 실패: $key = $value, ${e.message}")
        }
    }

    /**
     * 설정 변경 이력 저장 (R2DBC)
     */
    private fun saveSettingHistory(key: String, oldValue: Any?, newValue: Any) {
        val historyRepo = settingsHistoryRepository ?: return

        try {
            val history = SettingHistory(
                settingKey = key,
                oldValue = oldValue?.toString(),
                newValue = newValue.toString(),
                changedBy = "system",
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            historyRepo.save(history).subscribe(
                { logger.debug("설정 변경 이력 저장: $key") },
                { e -> logger.error("설정 변경 이력 저장 실패: ${e.message}") }
            )
        } catch (e: Exception) {
            logger.error("설정 변경 이력 저장 실패: ${e.message}")
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
        saveSettingHistory(key, oldValue, newValue)  // 변경 이력 저장
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

    // === 위치 관련 설정 ===
    /**
     * 위도 (도)
     * 시스템의 위도 좌표를 설정합니다.
     * 기본값: 35.317540
     */
    var latitude: Double by createSettingProperty("location.latitude", "위도")

    /**
     * 경도 (도)
     * 시스템의 경도 좌표를 설정합니다.
     * 기본값: 128.608510
     */
    var longitude: Double by createSettingProperty("location.longitude", "경도")

    /**
     * 고도 (미터)
     * 시스템의 고도 좌표를 설정합니다.
     * 기본값: 0.0
     */
    var altitude: Double by createSettingProperty("location.altitude", "고도")

    // === 추적 관련 설정 ===
    /**
     * 추적 간격 (밀리초)
     * 위성/태양 추적 계산 주기를 설정합니다.
     * 기본값: 100ms
     */
    var msInterval: Int by createSettingProperty("tracking.msInterval", "추적 간격")

    /**
     * 추적 기간 (일)
     * 추적을 수행할 기간을 설정합니다.
     * 기본값: 1일
     */
    var durationDays: Long by createSettingProperty("tracking.durationDays", "추적 기간")

    /**
     * 최소 고도각 (도)
     * 추적 시 고려할 최소 고도각을 설정합니다.
     * 기본값: -7.0도
     *
     * TODO [보류] 현재 이 설정은 실제 로직에서 사용되지 않음
     * - Pass 필터링에 적용 필요 (SatelliteService)
     * - sourceMinElevationAngle과의 역할 구분 필요
     * @see sourceMinElevationAngle 원본 데이터 생성 시 사용되는 설정
     */
    var minElevationAngle: Float by createSettingProperty("tracking.minElevationAngle", "최소 고도각")

    // === Stow Angle 설정 ===
    /**
     * Stow 방위각 (도)
     * Stow 위치의 방위각을 설정합니다.
     * 기본값: 0.0도
     */
    var stowAngleAzimuth: Double by createSettingProperty("stow.angle.azimuth", "Stow 방위각")

    /**
     * Stow 고도각 (도)
     * Stow 위치의 고도각을 설정합니다.
     * 기본값: 90.0도
     */
    var stowAngleElevation: Double by createSettingProperty("stow.angle.elevation", "Stow 고도각")

    /**
     * Stow Train각 (도)
     * Stow 위치의 Train각을 설정합니다.
     * 기본값: 0.0도
     */
    var stowAngleTrain: Double by createSettingProperty("stow.angle.train", "Stow Train각")

    // === Stow Speed 설정 ===
    /**
     * Stow 방위각 속도 (도/초)
     * Stow 이동 시 방위각 속도를 설정합니다.
     * 기본값: 5.0도/초
     */
    var stowSpeedAzimuth: Double by createSettingProperty("stow.speed.azimuth", "Stow 방위각 속도")

    /**
     * Stow 고도각 속도 (도/초)
     * Stow 이동 시 고도각 속도를 설정합니다.
     * 기본값: 5.0도/초
     */
    var stowSpeedElevation: Double by createSettingProperty("stow.speed.elevation", "Stow 고도각 속도")

    /**
     * Stow Train각 속도 (도/초)
     * Stow 이동 시 Train각 속도를 설정합니다.
     * 기본값: 5.0도/초
     */
    var stowSpeedTrain: Double by createSettingProperty("stow.speed.train", "Stow Train각 속도")

    // === AntennaSpec 설정 ===
    /**
     * True North Offset Angle (도)
     * True North 기준 오프셋 각도를 설정합니다.
     * 기본값: 0.0도
     */
    var trueNorthOffsetAngle: Double by createSettingProperty("antennaspec.trueNorthOffsetAngle", "True North Offset Angle")

    /**
     * Tilt Angle (도)
     * 안테나 틸트 각도를 설정합니다.
     * 기본값: -7.0도
     */
    var tiltAngle: Double by createSettingProperty("antennaspec.tiltAngle", "Tilt Angle")

    // === Angle Limits 설정 ===
    /**
     * Azimuth 최소각 (도)
     * 방위각의 최소 제한값을 설정합니다.
     * 기본값: -270.0도
     */
    var angleAzimuthMin: Double by createSettingProperty("anglelimits.azimuthMin", "Azimuth 최소각")

    /**
     * Azimuth 최대각 (도)
     * 방위각의 최대 제한값을 설정합니다.
     * 기본값: 270.0도
     */
    var angleAzimuthMax: Double by createSettingProperty("anglelimits.azimuthMax", "Azimuth 최대각")

    /**
     * Elevation 최소각 (도)
     * 고도각의 최소 제한값을 설정합니다.
     * 기본값: 0.0도
     */
    var angleElevationMin: Double by createSettingProperty("anglelimits.elevationMin", "Elevation 최소각")

    /**
     * Elevation 최대각 (도)
     * 고도각의 최대 제한값을 설정합니다.
     * 기본값: 180.0도
     */
    var angleElevationMax: Double by createSettingProperty("anglelimits.elevationMax", "Elevation 최대각")

    /**
     * Train 최소각 (도)
     * Train각의 최소 제한값을 설정합니다.
     * 기본값: -270.0도
     */
    var angleTrainMin: Double by createSettingProperty("anglelimits.trainMin", "Train 최소각")

    /**
     * Train 최대각 (도)
     * Train각의 최대 제한값을 설정합니다.
     * 기본값: 270.0도
     */
    var angleTrainMax: Double by createSettingProperty("anglelimits.trainMax", "Train 최대각")

    // === Speed Limits 설정 ===
    /**
     * Azimuth 최소속도 (도/초)
     * 방위각의 최소 속도를 설정합니다.
     * 기본값: 0.1도/초
     */
    var speedAzimuthMin: Double by createSettingProperty("speedlimits.azimuthMin", "Azimuth 최소속도")

    /**
     * Azimuth 최대속도 (도/초)
     * 방위각의 최대 속도를 설정합니다.
     * 기본값: 15.0도/초
     */
    var speedAzimuthMax: Double by createSettingProperty("speedlimits.azimuthMax", "Azimuth 최대속도")

    /**
     * Elevation 최소속도 (도/초)
     * 고도각의 최소 속도를 설정합니다.
     * 기본값: 0.1도/초
     */
    var speedElevationMin: Double by createSettingProperty("speedlimits.elevationMin", "Elevation 최소속도")

    /**
     * Elevation 최대속도 (도/초)
     * 고도각의 최대 속도를 설정합니다.
     * 기본값: 10.0도/초
     */
    var speedElevationMax: Double by createSettingProperty("speedlimits.elevationMax", "Elevation 최대속도")

    /**
     * Train 최소속도 (도/초)
     * Train각의 최소 속도를 설정합니다.
     * 기본값: 0.1도/초
     */
    var speedTrainMin: Double by createSettingProperty("speedlimits.trainMin", "Train 최소속도")

    /**
     * Train 최대속도 (도/초)
     * Train각의 최대 속도를 설정합니다.
     * 기본값: 5.0도/초
     */
    var speedTrainMax: Double by createSettingProperty("speedlimits.trainMax", "Train 최대속도")

    // === Angle Offset Limits 설정 ===
    /**
     * Azimuth 오프셋 제한 (도)
     * 방위각 오프셋의 최대 제한값을 설정합니다.
     * 기본값: 50.0도
     */
    var angleOffsetAzimuth: Double by createSettingProperty("angleoffsetlimits.azimuth", "Azimuth 오프셋 제한")

    /**
     * Elevation 오프셋 제한 (도)
     * 고도각 오프셋의 최대 제한값을 설정합니다.
     * 기본값: 50.0도
     */
    var angleOffsetElevation: Double by createSettingProperty("angleoffsetlimits.elevation", "Elevation 오프셋 제한")

    /**
     * Train 오프셋 제한 (도)
     * Train각 오프셋의 최대 제한값을 설정합니다.
     * 기본값: 50.0도
     */
    var angleOffsetTrain: Double by createSettingProperty("angleoffsetlimits.train", "Train 오프셋 제한")

    // === Time Offset Limits 설정 ===
    /**
     * 시간 오프셋 최소값 (초)
     * 시간 오프셋의 최소 제한값을 설정합니다.
     * 기본값: 0.1초
     */
    var timeOffsetMin: Double by createSettingProperty("timeoffsetlimits.min", "시간 오프셋 최소값")

    /**
     * 시간 오프셋 최대값 (초)
     * 시간 오프셋의 최대 제한값을 설정합니다.
     * 기본값: 99999초
     */
    var timeOffsetMax: Double by createSettingProperty("timeoffsetlimits.max", "시간 오프셋 최대값")

    // === Algorithm 설정 ===
    /**
     * Geo Min Motion (도/초)
     * 지구 동기 궤도 최소 모션을 설정합니다.
     * 기본값: 1.1도/초
     */
    var geoMinMotion: Double by createSettingProperty("algorithm.geoMinMotion", "Geo Min Motion")

    // === StepSizeLimit 설정 ===
    /**
     * 스텝 사이즈 최소값 (도)
     * 스텝 사이즈의 최소 제한값을 설정합니다.
     * 기본값: 50도
     */
    var stepSizeMin: Double by createSettingProperty("stepsizelimit.min", "스텝 사이즈 최소값")

    /**
     * 스텝 사이즈 최대값 (도)
     * 스텝 사이즈의 최대 제한값을 설정합니다.
     * 기본값: 50도
     */
    var stepSizeMax: Double by createSettingProperty("stepsizelimit.max", "스텝 사이즈 최대값")

    // === 시스템 설정 프로퍼티들 ===
    /**
     * 시스템 UDP 수신 간격 (밀리초)
     * UDP 패킷 수신 간격을 설정합니다.
     * 기본값: 10ms
     */
    var systemUdpReceiveInterval: Long by createSettingProperty("system.udp.receiveInterval", "UDP 수신 간격")

    /**
     * 시스템 UDP 전송 간격 (밀리초)
     * UDP 패킷 전송 간격을 설정합니다.
     * 기본값: 10ms
     */
    var systemUdpSendInterval: Long by createSettingProperty("system.udp.sendInterval", "UDP 전송 간격")

    /**
     * 시스템 UDP 타임아웃 (밀리초)
     * UDP 통신의 타임아웃 시간을 설정합니다.
     * 기본값: 25ms
     */
    var systemUdpTimeout: Long by createSettingProperty("system.udp.timeout", "UDP 타임아웃")

    /**
     * 시스템 UDP 재연결 간격 (밀리초)
     * UDP 연결 실패 시 재연결 시도 간격을 설정합니다.
     * 기본값: 1000ms
     */
    var systemUdpReconnectInterval: Long by createSettingProperty("system.udp.reconnectInterval", "UDP 재연결 간격")

    /**
     * 시스템 UDP 최대 버퍼 크기 (바이트)
     * UDP 통신에서 사용할 최대 버퍼 크기를 설정합니다.
     * 기본값: 1024
     */
    var systemUdpMaxBufferSize: Int by createSettingProperty("system.udp.maxBufferSize", "UDP 최대 버퍼 크기")

    /**
     * 시스템 UDP 명령 지연 (밀리초)
     * UDP 명령 전송 후 대기 시간을 설정합니다.
     * 기본값: 100ms
     */
    var systemUdpCommandDelay: Long by createSettingProperty("system.udp.commandDelay", "UDP 명령 지연")

    /**
     * 시스템 추적 간격 (밀리초)
     * 위성/태양 추적 계산 주기를 설정합니다.
     * 기본값: 100ms
     */
    var systemTrackingInterval: Long by createSettingProperty("system.tracking.interval", "추적 간격")

    /**
     * 시스템 추적 전송 간격 (밀리초)
     * WebSocket을 통한 실시간 데이터 전송 주기를 설정합니다.
     * 기본값: 100ms
     */
    var systemWebsocketTransmissionInterval: Long by createSettingProperty("system.websocket.transmissionInterval", "WebSocket 전송 간격")

    /**
     * 시스템 추적 정밀 계산 간격 (밀리초)
     * 정밀한 추적 계산을 수행하는 주기를 설정합니다.
     * 기본값: 100ms
     */
    var systemTrackingFineInterval: Long by createSettingProperty("system.tracking.fineInterval", "정밀 계산 간격")

    /**
     * 시스템 추적 일반 계산 간격 (밀리초)
     * 일반적인 추적 계산을 수행하는 주기를 설정합니다.
     * 기본값: 1000ms
     */
    var systemTrackingCoarseInterval: Long by createSettingProperty("system.tracking.coarseInterval", "일반 계산 간격")

    /**
     * 시스템 추적 성능 임계값 (밀리초)
     * 데이터 처리 시 허용되는 최대 시간을 설정합니다.
     * 기본값: 100ms
     */
    var systemPerformanceThreshold: Long by createSettingProperty("system.performance.threshold", "성능 임계값")

    /**
     * 시스템 추적 안정화 타임아웃 (밀리초)
     * 추적 안정화를 위한 대기 시간을 설정합니다.
     * 기본값: 5000ms
     */
    var systemTrackingStabilizationTimeout: Long by createSettingProperty("system.tracking.stabilizationTimeout", "안정화 타임아웃")

    /**
     * 시스템 저장 배치 크기 (개수)
     * 데이터 저장 시 한 번에 처리할 배치 크기를 설정합니다.
     * 기본값: 1000
     */
    var systemStorageBatchSize: Int by createSettingProperty("system.storage.batchSize", "배치 크기")

    /**
     * 시스템 저장 간격 (밀리초)
     * 데이터 저장을 수행하는 주기를 설정합니다.
     * 기본값: 100ms
     */
    var systemStorageSaveInterval: Long by createSettingProperty("system.storage.saveInterval", "저장 간격")

    /**
     * 시스템 저장 진행률 로깅 간격 (개수)
     * 저장 진행률을 로깅하는 간격을 설정합니다.
     * 기본값: 1000
     */
    var systemStorageProgressLogInterval: Int by createSettingProperty("system.storage.progressLogInterval", "진행률 로깅 간격")

    // === 태양 추적 정확도 임계값 프로퍼티들 ===
    /**
     * 태양 추적 높은 정확도 임계값 (도)
     * 태양 위치 계산 시 높은 정확도를 요구하는 임계값을 설정합니다.
     * 기본값: 0.000278도 (1 arcsec)
     */
    var systemSuntrackHighAccuracyThreshold: Double by createSettingProperty("system.suntrack.highAccuracyThreshold", "태양 추적 높은 정확도 임계값")

    /**
     * 태양 추적 중간 정확도 임계값 (도)
     * 태양 위치 계산 시 중간 정확도를 요구하는 임계값을 설정합니다.
     * 기본값: 0.002778도 (10 arcsec)
     */
    var systemSuntrackMediumAccuracyThreshold: Double by createSettingProperty("system.suntrack.mediumAccuracyThreshold", "태양 추적 중간 정확도 임계값")

    /**
     * 태양 추적 낮은 정확도 임계값 (도)
     * 태양 위치 계산 시 낮은 정확도를 요구하는 임계값을 설정합니다.
     * 기본값: 0.016667도 (60 arcsec)
     */
    var systemSuntrackLowAccuracyThreshold: Double by createSettingProperty("system.suntrack.lowAccuracyThreshold", "태양 추적 낮은 정확도 임계값")

    /**
     * 태양 추적 검색 시간 (시간)
     * 태양 위치 검색을 수행할 시간을 설정합니다.
     * 기본값: 48시간
     */
    var systemSuntrackSearchHours: Double by createSettingProperty("system.suntrack.searchHours", "태양 추적 검색 시간")

    // 성능 등급 기준 설정
    /**
     * 성능 등급 기준 설정들
     */
    var systemPerformanceUltraCores: Int by createSettingProperty("system.performance.ultraCores", "ULTRA 등급 최소 CPU 코어 수")
    var systemPerformanceHighCores: Int by createSettingProperty("system.performance.highCores", "HIGH 등급 최소 CPU 코어 수")
    var systemPerformanceMediumCores: Int by createSettingProperty("system.performance.mediumCores", "MEDIUM 등급 최소 CPU 코어 수")
    var systemPerformanceUltraMemory: Long by createSettingProperty("system.performance.ultraMemory", "ULTRA 등급 최소 메모리(GB)")
    var systemPerformanceHighMemory: Long by createSettingProperty("system.performance.highMemory", "HIGH 등급 최소 메모리(GB)")
    var systemPerformanceMediumMemory: Long by createSettingProperty("system.performance.mediumMemory", "MEDIUM 등급 최소 메모리(GB)")

    // JVM 튜닝 설정 프로퍼티들
    /**
     * JVM GC 일시정지 시간 (밀리초)
     * 가비지 컬렉션 시 허용되는 최대 일시정지 시간을 설정합니다.
     * 기본값: 10ms
     */
    var systemJvmGcPause: Long by createSettingProperty("system.jvm.gcPause", "GC 일시정지 시간")

    /**
     * JVM 힙 영역 크기 (MB)
     * 힙 메모리 영역의 크기를 설정합니다.
     * 기본값: 16MB
     */
    var systemJvmHeapRegionSize: Long by createSettingProperty("system.jvm.heapRegionSize", "힙 영역 크기")

    /**
     * JVM 동시 스레드 수
     * 동시에 실행될 수 있는 스레드 수를 설정합니다.
     * 기본값: 4
     */
    var systemJvmConcurrentThreads: Long by createSettingProperty("system.jvm.concurrentThreads", "동시 스레드 수")

    /**
     * JVM 병렬 스레드 수
     * 병렬 처리에 사용될 스레드 수를 설정합니다.
     * 기본값: 8
     */
    var systemJvmParallelThreads: Long by createSettingProperty("system.jvm.parallelThreads", "병렬 스레드 수")

    // === Feed 설정 프로퍼티 ===
    /**
     * 활성화된 피드 밴드 목록
     * 기본값: ["s", "x"]
     * S-Band, X-Band, Ka-Band 중 표시할 밴드를 선택합니다.
     * JSON 문자열로 저장되므로 커스텀 getter/setter 사용
     */
    var feedEnabledBands: List<String>
        get() {
            val value = settings["feed.enabledBands"] as? String ?: "[\"s\",\"x\"]"
            return try {
                // JSON 문자열 파싱: ["s","x"] -> ["s", "x"]
                value.removePrefix("[").removeSuffix("]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() }
            } catch (e: Exception) {
                logger.warn("피드 설정 파싱 실패, 기본값 사용: $value, ${e.message}")
                listOf("s", "x")
            }
        }
        set(value) {
            // 유효성 검사: 최소 하나의 밴드는 활성화되어 있어야 함
            if (value.isEmpty()) {
                throw IllegalArgumentException("최소 하나의 밴드는 선택되어 있어야 합니다.")
            }
            // 유효성 검사: 허용된 밴드만 포함
            val validBands = value.filter { it in listOf("s", "x", "ka") }
            if (validBands.isEmpty()) {
                throw IllegalArgumentException("유효한 밴드를 선택해주세요. (s, x, ka)")
            }
            // JSON 문자열로 변환하여 저장: ["s", "x"] -> ["s","x"]
            val jsonValue = validBands.joinToString(",", "[", "]") { "\"$it\"" }
            updateSetting("feed.enabledBands", jsonValue)
        }

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

    // Feed 설정 일괄 변경
    fun setFeed(enabledBands: List<String>) {
        logger.info("🔄 Feed 설정 변경 시작: enabledBands = $enabledBands")
        // 유효성 검사: 최소 하나의 밴드는 활성화되어 있어야 함
        if (enabledBands.isEmpty()) {
            logger.error("❌ Feed 설정 변경 실패: 최소 하나의 밴드는 선택되어 있어야 합니다.")
            throw IllegalArgumentException("최소 하나의 밴드는 선택되어 있어야 합니다.")
        }
        // 유효성 검사: 허용된 밴드만 포함
        val validBands = enabledBands.filter { it in listOf("s", "x", "ka") }
        if (validBands.isEmpty()) {
            logger.error("❌ Feed 설정 변경 실패: 유효한 밴드를 선택해주세요. (s, x, ka)")
            throw IllegalArgumentException("유효한 밴드를 선택해주세요. (s, x, ka)")
        }
        // JSON 문자열로 변환하여 저장
        val jsonValue = validBands.joinToString(",", "[", "]") { "\"$it\"" }
        logger.info("💾 Feed 설정 저장: feed.enabledBands = $jsonValue")
        updateSetting("feed.enabledBands", jsonValue)
        logger.info("✅ Feed 설정 변경 완료: enabledBands = $validBands")
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
    fun getFeedSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("feed.") }

    // === 시스템 설정 그룹별 조회 메서드들 ===
    /**
     * 시스템 UDP 설정 조회
     * UDP 통신 관련 모든 설정을 반환합니다.
     * @return UDP 설정 맵
     */
    fun getSystemUdpSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("system.udp.") }

    /**
     * 시스템 추적 설정 조회
     * 추적 관련 모든 설정을 반환합니다.
     * @return 추적 설정 맵
     */
    fun getSystemTrackingSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("system.tracking.") }

    /**
     * 시스템 저장 설정 조회
     * 데이터 저장 관련 모든 설정을 반환합니다.
     * @return 저장 설정 맵
     */
    fun getSystemStorageSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("system.storage.") }

    /**
     * 시스템 태양 추적 설정 조회
     * 태양 추적 관련 모든 설정을 반환합니다.
     * @return 태양 추적 설정 맵
     */
    fun getSystemSuntrackSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("system.suntrack.") }

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
            // === 시스템 설정 단위 추가 ===
            event.key.startsWith("system.udp.") -> "ms"
            event.key.startsWith("system.tracking.") -> "ms"
            event.key.startsWith("system.storage.") -> "ms"
            event.key.startsWith("system.suntrack.") -> "도"
            event.key.startsWith("system.jvm.") -> "ms" // JVM 튜닝 설정 단위 추가
            event.key.startsWith("ephemeris.tracking.") -> when {
                event.key.contains("VelocityThreshold") -> "도/초"
                event.key.contains("ElevationAngle") -> "도"
                else -> "도"
            }
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
            event.key.startsWith("feed.") -> {
                logger.info("Feed 설정 변경: ${event.key} = ${event.value}")
            }
            // === 시스템 설정 그룹들 추가 ===
            event.key.startsWith("system.udp.") -> {
                // 시스템 UDP 설정 변경 시 처리 로직
            }
            event.key.startsWith("system.storage.") -> {
                // 시스템 저장 설정 변경 시 처리 로직
            }
            event.key.startsWith("system.suntrack.") -> {
                // 시스템 태양 추적 설정 변경 시 처리 로직
            }
            event.key.startsWith("system.jvm.") -> { // JVM 튜닝 설정 변경 시 처리 로직
                // 시스템 JVM 설정 변경 시 처리 로직
            }
            event.key.startsWith("ephemeris.tracking.") -> {
                // Ephemeris Tracking 설정 변경 시 처리 로직
                logger.info("Ephemeris Tracking 설정 변경: ${event.key} = ${event.value}")
            }
        }
    }

    // === Ephemeris Tracking 개별 설정 프로퍼티들 ===
    
    /**
     * 원본 2축 위성 추적 데이터 생성 시 최소 Elevation 각도 (도)
     * 
     * ## 용도
     * - Orekit 계산 시 사용되는 2축 좌표계 기준 최소 고도각
     * - Tilt 각도 보정을 고려하여 음수 값 허용
     * 
     * ## 기본값 계산 로직
     * - 자동 계산 권장: -abs(tiltAngle)
     * - 예: Tilt가 -7°인 경우 → sourceMinElevationAngle = -7.0°
     * 
     * ## 사용자 수동 조정
     * - 필요 시 사용자가 직접 값을 변경 가능
     * - 범위: -90.0° ~ 90.0°
     * 
     * @see OrekitCalculator.generateSatelliteTrackingSchedule
     */
    val sourceMinElevationAngle: Double by createSettingProperty("ephemeris.tracking.sourceMinElevationAngle", "원본 2축 위성 추적 데이터 생성 시 최소 Elevation 각도")

    /**
     * KEYHOLE 위성 판단을 위한 Azimuth 각속도 임계값 (도/초)
     * 
     * ## 용도
     * - LEO 위성의 고속 통과(KEYHOLE) 자동 판단 기준
     * - 전체 추적 구간에서 최대 Azimuth 각속도 비교
     * 
     * ## 판단 로직
     * - maxAzimuthRate >= keyholeAzimuthVelocityThreshold → KEYHOLE 위성
     * - KEYHOLE 위성: Train 각도를 최대 Elevation 지점의 Azimuth로 설정
     * 
     * ## 기본값
     * - 10.0 도/초 (초당 10도 이상 회전)
     * 
     * @see EphemerisService.analyzeKeyholeStatus
     * @see SatelliteTrackingPass.maxAzimuthRate
     */
    val keyholeAzimuthVelocityThreshold: Double by createSettingProperty("ephemeris.tracking.keyholeAzimuthVelocityThreshold", "KEYHOLE 위성 판단을 위한 Azimuth 각속도 임계값")

    /**
     * Ephemeris Tracking 설정 그룹 조회
     * @return Ephemeris Tracking 설정 맵
     */
    fun getEphemerisTrackingSettings(): Map<String, Any> = settings.filterKeys { it.startsWith("ephemeris.tracking.") }
}