package com.gtlsystems.acs_api.service.mode.ephemeris

import com.gtlsystems.acs_api.tracking.entity.TrackingSessionEntity
import com.gtlsystems.acs_api.tracking.entity.TrackingTrajectoryEntity
import com.gtlsystems.acs_api.tracking.repository.TrackingSessionRepository
import com.gtlsystems.acs_api.tracking.repository.TrackingTrajectoryRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicLong

/**
 * Ephemeris 추적 데이터 저장소
 *
 * 위성 추적 마스터(MST) 및 세부(DTL) 데이터를 관리합니다.
 * 모든 데이터 접근에 로그를 기록하여 검증 가능성을 보장합니다.
 *
 * Write-through 패턴:
 * - 메모리 캐시: 빠른 조회
 * - DB 저장: 영속성 보장 (tracking_session, tracking_trajectory)
 *
 * @since Phase 5 - BE 서비스 분리
 * @since Phase 6 - DB 연동 추가
 */
@Component
class EphemerisDataRepository(
    private val sessionRepository: TrackingSessionRepository?,
    private val trajectoryRepository: TrackingTrajectoryRepository?
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 위성 추적 마스터 데이터 저장소
     * Key: DataType (original, axis_transformed, final_transformed 등)
     */
    private val mstStorage = mutableListOf<Map<String, Any?>>()

    /**
     * 위성 추적 세부 데이터 저장소
     */
    private val dtlStorage = mutableListOf<Map<String, Any?>>()

    /**
     * 데이터 변경 카운터 (검증용)
     */
    private val writeCounter = AtomicLong(0)
    private val readCounter = AtomicLong(0)

    // ========================================
    // 저장소 쓰기 작업 (Write Operations)
    // ========================================

    /**
     * 모든 데이터를 초기화하고 새 데이터로 교체합니다.
     *
     * @param mstData 새 마스터 데이터
     * @param dtlData 새 세부 데이터
     */
    fun replaceAll(mstData: List<Map<String, Any?>>, dtlData: List<Map<String, Any?>>) {
        val opId = writeCounter.incrementAndGet()
        logger.info("📝 [WRITE #$opId] replaceAll 시작 - MST: ${mstData.size}개, DTL: ${dtlData.size}개")

        synchronized(mstStorage) {
            val oldMstSize = mstStorage.size
            mstStorage.clear()
            mstStorage.addAll(mstData)
            logger.info("📝 [WRITE #$opId] MST 교체 완료: $oldMstSize → ${mstStorage.size}")
        }

        synchronized(dtlStorage) {
            val oldDtlSize = dtlStorage.size
            dtlStorage.clear()
            dtlStorage.addAll(dtlData)
            logger.info("📝 [WRITE #$opId] DTL 교체 완료: $oldDtlSize → ${dtlStorage.size}")
        }

        logStorageSummary(opId)

        // DB 저장 (Write-through)
        saveToDatabase(mstData, dtlData, opId)
    }

    /**
     * 마스터 및 세부 데이터를 추가합니다.
     *
     * @param mstData 추가할 마스터 데이터
     * @param dtlData 추가할 세부 데이터
     */
    fun addAll(mstData: List<Map<String, Any?>>, dtlData: List<Map<String, Any?>>) {
        val opId = writeCounter.incrementAndGet()
        logger.info("📝 [WRITE #$opId] addAll 시작 - MST: ${mstData.size}개, DTL: ${dtlData.size}개")

        synchronized(mstStorage) {
            val beforeSize = mstStorage.size
            mstStorage.addAll(mstData)
            logger.info("📝 [WRITE #$opId] MST 추가 완료: $beforeSize → ${mstStorage.size}")
        }

        synchronized(dtlStorage) {
            val beforeSize = dtlStorage.size
            dtlStorage.addAll(dtlData)
            logger.info("📝 [WRITE #$opId] DTL 추가 완료: $beforeSize → ${dtlStorage.size}")
        }

        logStorageSummary(opId)
    }

    /**
     * 모든 데이터를 초기화합니다.
     */
    fun clear() {
        val opId = writeCounter.incrementAndGet()
        val mstSize = mstStorage.size
        val dtlSize = dtlStorage.size

        synchronized(mstStorage) {
            mstStorage.clear()
        }
        synchronized(dtlStorage) {
            dtlStorage.clear()
        }

        logger.info("📝 [WRITE #$opId] 저장소 초기화 완료 - MST: ${mstSize}개 삭제, DTL: ${dtlSize}개 삭제")
    }

    // ========================================
    // 저장소 읽기 작업 (Read Operations)
    // ========================================

    /**
     * 모든 마스터 데이터를 반환합니다.
     */
    fun getAllMst(): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(mstStorage) { mstStorage.toList() }
        logger.debug("📖 [READ #$opId] getAllMst() → ${result.size}개")
        return result
    }

    /**
     * 모든 세부 데이터를 반환합니다.
     */
    fun getAllDtl(): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(dtlStorage) { dtlStorage.toList() }
        logger.debug("📖 [READ #$opId] getAllDtl() → ${result.size}개")
        return result
    }

    /**
     * 데이터 타입별 마스터 데이터를 반환합니다.
     *
     * @param dataType 데이터 타입 (original, axis_transformed, final_transformed 등)
     */
    fun getMstByDataType(dataType: String): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(mstStorage) {
            mstStorage.filter { it["DataType"] == dataType }
        }
        logger.debug("📖 [READ #$opId] getMstByDataType($dataType) → ${result.size}개")
        return result
    }

    /**
     * 데이터 타입별 세부 데이터를 반환합니다.
     *
     * @param dataType 데이터 타입
     */
    fun getDtlByDataType(dataType: String): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(dtlStorage) {
            dtlStorage.filter { it["DataType"] == dataType }
        }
        logger.debug("📖 [READ #$opId] getDtlByDataType($dataType) → ${result.size}개")
        return result
    }

    /**
     * MstId로 마스터 데이터를 검색합니다.
     *
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입 (선택적)
     */
    fun findMstById(mstId: Long, dataType: String? = null): Map<String, Any?>? {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(mstStorage) {
            mstStorage.find {
                val dataMstId = (it["MstId"] as? Number)?.toLong()
                val matches = dataMstId == mstId
                if (dataType != null) {
                    matches && it["DataType"] == dataType
                } else {
                    matches
                }
            }
        }
        logger.debug("📖 [READ #$opId] findMstById($mstId, $dataType) → ${if (result != null) "found" else "null"}")
        return result
    }

    /**
     * MstId와 데이터 타입으로 세부 데이터를 검색합니다.
     *
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입
     * @param detailId 세부 ID (기본값: 0)
     */
    fun findDtlByMstIdAndDataType(mstId: Long, dataType: String, detailId: Int = 0): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(dtlStorage) {
            dtlStorage.filter {
                val dataMstId = (it["MstId"] as? Number)?.toLong()
                val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
                val itDataType = it["DataType"] as? String
                dataMstId == mstId && dataDetailId == detailId && itDataType == dataType
            }
        }

        if (result.isEmpty()) {
            logger.warn("⚠️ [READ #$opId] findDtlByMstIdAndDataType($mstId, $dataType, $detailId) → 0개 (데이터 없음)")
            // 디버깅용: 해당 MstId와 DataType으로 존재하는 DetailId 목록
            val availableDetailIds = synchronized(dtlStorage) {
                dtlStorage.filter {
                    val dataMstId = (it["MstId"] as? Number)?.toLong()
                    val itDataType = it["DataType"] as? String
                    dataMstId == mstId && itDataType == dataType
                }.mapNotNull { (it["DetailId"] as? Number)?.toInt() ?: 0 }.distinct()
            }
            if (availableDetailIds.isNotEmpty()) {
                logger.warn("⚠️ [READ #$opId] 사용 가능한 DetailId: $availableDetailIds")
            }
        } else {
            logger.debug("📖 [READ #$opId] findDtlByMstIdAndDataType($mstId, $dataType, $detailId) → ${result.size}개")
        }

        return result
    }

    /**
     * MstId로 모든 데이터 타입의 세부 데이터를 검색합니다.
     *
     * @param mstId 마스터 ID
     * @param detailId 세부 ID (기본값: 0)
     */
    fun findAllDtlByMstId(mstId: Long, detailId: Int = 0): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(dtlStorage) {
            dtlStorage.filter {
                val dataMstId = (it["MstId"] as? Number)?.toLong()
                val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
                dataMstId == mstId && dataDetailId == detailId
            }
        }
        logger.debug("📖 [READ #$opId] findAllDtlByMstId($mstId, $detailId) → ${result.size}개")
        return result
    }

    // ========================================
    // 통계 및 디버깅
    // ========================================

    /**
     * 저장소 크기를 반환합니다.
     */
    fun getMstSize(): Int = synchronized(mstStorage) { mstStorage.size }
    fun getDtlSize(): Int = synchronized(dtlStorage) { dtlStorage.size }

    /**
     * 저장소 상태 요약을 반환합니다.
     */
    fun getStorageSummary(): Map<String, Any> {
        return synchronized(mstStorage) {
            synchronized(dtlStorage) {
                val mstDataTypes = mstStorage.groupBy { it["DataType"] as? String ?: "unknown" }
                    .mapValues { it.value.size }
                val dtlDataTypes = dtlStorage.groupBy { it["DataType"] as? String ?: "unknown" }
                    .mapValues { it.value.size }

                mapOf(
                    "totalMst" to mstStorage.size,
                    "totalDtl" to dtlStorage.size,
                    "mstByDataType" to mstDataTypes,
                    "dtlByDataType" to dtlDataTypes,
                    "writeOperations" to writeCounter.get(),
                    "readOperations" to readCounter.get()
                )
            }
        }
    }

    /**
     * 저장소 상태 요약 로그 출력
     */
    private fun logStorageSummary(opId: Long) {
        val summary = getStorageSummary()
        logger.info("📊 [WRITE #$opId] 저장소 상태:")
        logger.info("   - 총 MST: ${summary["totalMst"]}개")
        logger.info("   - 총 DTL: ${summary["totalDtl"]}개")
        @Suppress("UNCHECKED_CAST")
        val mstByType = summary["mstByDataType"] as Map<String, Int>
        mstByType.forEach { (type, count) ->
            logger.info("   - MST[$type]: ${count}개")
        }
    }

    /**
     * 전체 상태 덤프 (디버깅용)
     */
    fun dumpState(): String {
        val sb = StringBuilder()
        sb.appendLine("=== EphemerisDataRepository State ===")
        sb.appendLine("MST Storage: ${getMstSize()} items")
        sb.appendLine("DTL Storage: ${getDtlSize()} items")
        sb.appendLine("Write Operations: ${writeCounter.get()}")
        sb.appendLine("Read Operations: ${readCounter.get()}")
        sb.appendLine()

        val summary = getStorageSummary()
        @Suppress("UNCHECKED_CAST")
        val mstByType = summary["mstByDataType"] as Map<String, Int>
        sb.appendLine("MST by DataType:")
        mstByType.forEach { (type, count) ->
            sb.appendLine("  - $type: $count")
        }

        @Suppress("UNCHECKED_CAST")
        val dtlByType = summary["dtlByDataType"] as Map<String, Int>
        sb.appendLine("DTL by DataType:")
        dtlByType.forEach { (type, count) ->
            sb.appendLine("  - $type: $count")
        }

        return sb.toString()
    }

    // ========================================
    // DB 연동 (Write-through)
    // ========================================

    /**
     * DB에 스케줄 데이터를 저장합니다.
     * MST → tracking_session, DTL → tracking_trajectory
     */
    private fun saveToDatabase(mstData: List<Map<String, Any?>>, dtlData: List<Map<String, Any?>>, opId: Long) {
        if (sessionRepository == null || trajectoryRepository == null) {
            logger.warn("DB Repository가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        // MST 데이터 → TrackingSession 저장
        mstData.forEach { mst ->
            try {
                // ✅ DTL 카운트 미리 계산 (total_points용)
                val mstId = (mst["MstId"] as? Number)?.toLong()
                val dataType = mst["DataType"] as? String
                val sessionDtlData = dtlData.filter { dtl ->
                    val dtlMstId = (dtl["MstId"] as? Number)?.toLong()
                    val dtlDataType = dtl["DataType"] as? String
                    dtlMstId == mstId && dtlDataType == dataType
                }

                // ✅ DTL 카운트 전달
                val session = mapMstToSession(mst, sessionDtlData.size)
                sessionRepository.save(session)
                    .doOnSuccess { saved: TrackingSessionEntity ->
                        logger.debug("📝 [DB #$opId] Session 저장: id=${saved.id}, mstId=${saved.mstId}, totalPoints=${saved.totalPoints}")
                        // 해당 세션의 DTL 데이터 저장
                        if (sessionDtlData.isNotEmpty() && saved.id != null) {
                            saveTrajectories(saved.id, sessionDtlData, opId)
                        }
                    }
                    .doOnError { e: Throwable ->
                        logger.error("❌ [DB #$opId] Session 저장 실패: ${e.message}")
                    }
                    .subscribe()
            } catch (e: Exception) {
                logger.error("❌ [DB #$opId] MST → Session 변환 실패: ${e.message}")
            }
        }

        logger.info("📝 [DB #$opId] Ephemeris 스케줄 DB 저장 요청 완료 (MST: ${mstData.size}개)")
    }

    /**
     * DTL 데이터를 trajectory로 저장합니다.
     */
    private fun saveTrajectories(sessionId: Long, dtlData: List<Map<String, Any?>>, opId: Long) {
        if (trajectoryRepository == null) return

        val trajectories = dtlData.mapNotNull { dtl ->
            try {
                mapDtlToTrajectory(sessionId, dtl)
            } catch (e: Exception) {
                logger.error("❌ [DB #$opId] DTL → Trajectory 변환 실패: ${e.message}")
                null
            }
        }

        if (trajectories.isNotEmpty()) {
            trajectoryRepository.saveAll(trajectories)
                .doOnSuccess {
                    logger.debug("📝 [DB #$opId] Trajectory 배치 저장 완료: ${trajectories.size}개")
                }
                .doOnError { e: Throwable ->
                    logger.error("❌ [DB #$opId] Trajectory 저장 실패: ${e.message}")
                }
                .subscribe()
        }
    }

    /**
     * MST Map을 TrackingSessionEntity로 변환합니다.
     *
     * ✅ 키 이름 매핑 (SatelliteTrackingProcessor와 일치):
     * - SatelliteID (대문자 ID) → satellite_id
     * - MaxAzRate → max_azimuth_rate
     * - MaxElRate → max_elevation_rate
     * - IsKeyhole → keyhole_detected
     * - Duration (ISO String) → duration (초)
     *
     * @param dtlCount DTL 데이터 개수 (total_points 계산용)
     */
    private fun mapMstToSession(mst: Map<String, Any?>, dtlCount: Int = 0): TrackingSessionEntity {
        val mstId = (mst["MstId"] as? Number)?.toLong() ?: 0L
        val detailId = (mst["DetailId"] as? Number)?.toInt() ?: 0
        // ✅ SatelliteID (대문자) 우선, 없으면 SatelliteId 시도
        val satelliteId = mst["SatelliteID"] as? String
            ?: mst["SatelliteId"] as? String ?: ""
        val satelliteName = mst["SatelliteName"] as? String
        val dataType = mst["DataType"] as? String ?: "original"

        // 시간 파싱
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val startTime = parseTime(mst["StartTime"]) ?: now
        val endTime = parseTime(mst["EndTime"]) ?: now
        // ✅ Duration: ISO String 파싱 또는 시간 차이 계산
        val duration = parseDurationToSeconds(mst["Duration"], startTime, endTime)

        // 각도 정보
        val maxElevation = (mst["MaxElevation"] as? Number)?.toDouble()
        // ✅ MaxAzRate 우선, 없으면 MaxAzimuthRate 시도
        val maxAzimuthRate = (mst["MaxAzRate"] as? Number)?.toDouble()
            ?: (mst["MaxAzimuthRate"] as? Number)?.toDouble()
        // ✅ MaxElRate 우선, 없으면 MaxElevationRate 시도
        val maxElevationRate = (mst["MaxElRate"] as? Number)?.toDouble()
            ?: (mst["MaxElevationRate"] as? Number)?.toDouble()
        // ✅ IsKeyhole 우선, 없으면 KeyholeDetected 시도
        val keyholeDetected = mst["IsKeyhole"] as? Boolean
            ?: mst["KeyholeDetected"] as? Boolean ?: false
        val recommendedTrainAngle = (mst["RecommendedTrainAngle"] as? Number)?.toDouble()
        // ✅ TotalPoints: MST에서 읽거나 DTL 카운트 사용
        val totalPoints = (mst["TotalPoints"] as? Number)?.toInt()
            ?: if (dtlCount > 0) dtlCount else null

        return TrackingSessionEntity(
            mstId = mstId,
            detailId = detailId,
            satelliteId = satelliteId,
            satelliteName = satelliteName,
            trackingMode = "EPHEMERIS",
            dataType = dataType,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            maxElevation = maxElevation,
            maxAzimuthRate = maxAzimuthRate,
            maxElevationRate = maxElevationRate,
            keyholeDetected = keyholeDetected,
            recommendedTrainAngle = recommendedTrainAngle,
            totalPoints = totalPoints
        )
    }

    /**
     * DTL Map을 TrackingTrajectoryEntity로 변환합니다.
     */
    private fun mapDtlToTrajectory(sessionId: Long, dtl: Map<String, Any?>): TrackingTrajectoryEntity {
        val detailId = (dtl["DetailId"] as? Number)?.toInt() ?: 0
        val dataType = dtl["DataType"] as? String ?: "original"
        val index = (dtl["Index"] as? Number)?.toInt() ?: 0

        // 시간 파싱
        val timestamp = parseTime(dtl["Time"]) ?: parseTime(dtl["Timestamp"])
            ?: OffsetDateTime.now(ZoneOffset.UTC)

        // 각도
        val azimuth = (dtl["Azimuth"] as? Number)?.toDouble() ?: 0.0
        val elevation = (dtl["Elevation"] as? Number)?.toDouble() ?: 0.0
        val train = (dtl["Train"] as? Number)?.toDouble()

        // 속도
        val azimuthRate = (dtl["AzimuthRate"] as? Number)?.toDouble()
        val elevationRate = (dtl["ElevationRate"] as? Number)?.toDouble()

        return TrackingTrajectoryEntity(
            timestamp = timestamp,
            sessionId = sessionId,
            detailId = detailId,
            dataType = dataType,
            index = index,
            azimuth = azimuth,
            elevation = elevation,
            train = train,
            azimuthRate = azimuthRate,
            elevationRate = elevationRate
        )
    }

    /**
     * 다양한 시간 형식을 OffsetDateTime으로 파싱합니다.
     */
    private fun parseTime(value: Any?): OffsetDateTime? {
        return when (value) {
            is OffsetDateTime -> value
            is ZonedDateTime -> value.toOffsetDateTime()
            is java.time.Instant -> value.atOffset(ZoneOffset.UTC)
            is String -> try {
                OffsetDateTime.parse(value)
            } catch (e: Exception) {
                try {
                    ZonedDateTime.parse(value).toOffsetDateTime()
                } catch (e2: Exception) {
                    null
                }
            }
            is Number -> OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(value.toLong()),
                ZoneOffset.UTC
            )
            else -> null
        }
    }

    /**
     * ✅ Duration 값을 초 단위 정수로 변환합니다.
     *
     * 지원 형식:
     * - Number: 그대로 정수 변환
     * - ISO 8601 Duration String (예: "PT5M30S"): 파싱 후 초로 변환
     * - 기타: startTime과 endTime 차이로 계산
     *
     * @param durationValue Duration 값 (Number, String 등)
     * @param startTime 시작 시간 (fallback 계산용)
     * @param endTime 종료 시간 (fallback 계산용)
     * @return 초 단위 정수 (null 가능)
     */
    private fun parseDurationToSeconds(
        durationValue: Any?,
        startTime: OffsetDateTime,
        endTime: OffsetDateTime
    ): Int? {
        return when (durationValue) {
            is Number -> durationValue.toInt()
            is String -> {
                try {
                    // ISO 8601 Duration 형식 파싱 (예: "PT5M30S")
                    java.time.Duration.parse(durationValue).seconds.toInt()
                } catch (e: Exception) {
                    // 파싱 실패 시 시간 차이로 계산
                    java.time.Duration.between(startTime, endTime).seconds.toInt()
                }
            }
            else -> {
                // Duration 값이 없으면 시간 차이로 계산
                java.time.Duration.between(startTime, endTime).seconds.toInt()
            }
        }
    }
}
