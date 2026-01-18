package com.gtlsystems.acs_api.service.mode.passSchedule

import com.gtlsystems.acs_api.tracking.entity.TrackingSessionEntity
import com.gtlsystems.acs_api.tracking.entity.TrackingTrajectoryEntity
import com.gtlsystems.acs_api.tracking.repository.TrackingSessionRepository
import com.gtlsystems.acs_api.tracking.repository.TrackingTrajectoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * PassSchedule 추적 데이터 저장소 르
 *
 * 위성별 패스 스케줄 마스터(MST) 및 세부(DTL) 데이터를 관리합니다.
 * ConcurrentHashMap 기반으로 위성 ID별 데이터 저장.
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
class PassScheduleDataRepository(
    private val sessionRepository: TrackingSessionRepository?,
    private val trajectoryRepository: TrackingTrajectoryRepository?
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 위성별 패스 스케줄 마스터 데이터 저장소
     * Key: satelliteId
     * Value: List<Map<String, Any?>> - 해당 위성의 모든 패스 MST 데이터
     */
    private val mstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()

    /**
     * 위성별 패스 스케줄 세부 데이터 저장소
     * Key: satelliteId
     * Value: List<Map<String, Any?>> - 해당 위성의 모든 패스 DTL 데이터
     */
    private val dtlStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()

    /**
     * 데이터 변경 카운터 (검증용)
     */
    private val writeCounter = AtomicLong(0)
    private val readCounter = AtomicLong(0)

    // ========================================
    // 저장소 쓰기 작업 (Write Operations)
    // ========================================

    /**
     * 위성별 MST/DTL 데이터를 저장합니다.
     *
     * @param satelliteId 위성 ID
     * @param mstData 마스터 데이터 리스트
     * @param dtlData 세부 데이터 리스트
     */
    fun saveSatelliteData(satelliteId: String, mstData: List<Map<String, Any?>>, dtlData: List<Map<String, Any?>>) {
        val opId = writeCounter.incrementAndGet()
        logger.info("📝 [WRITE #$opId] saveSatelliteData($satelliteId) - MST: ${mstData.size}개, DTL: ${dtlData.size}개")

        mstStorage[satelliteId] = mstData
        dtlStorage[satelliteId] = dtlData

        logStorageSummary(opId)

        // DB 저장 (Write-through)
        saveToDatabase(satelliteId, mstData, dtlData, opId)
    }

    /**
     * 특정 위성의 데이터를 삭제합니다.
     *
     * @param satelliteId 위성 ID
     */
    fun removeSatelliteData(satelliteId: String) {
        val opId = writeCounter.incrementAndGet()
        val mstSize = mstStorage[satelliteId]?.size ?: 0
        val dtlSize = dtlStorage[satelliteId]?.size ?: 0

        mstStorage.remove(satelliteId)
        dtlStorage.remove(satelliteId)

        logger.info("📝 [WRITE #$opId] removeSatelliteData($satelliteId) - MST: ${mstSize}개 삭제, DTL: ${dtlSize}개 삭제")
    }

    /**
     * 모든 데이터를 초기화합니다.
     */
    fun clear() {
        val opId = writeCounter.incrementAndGet()
        val mstCount = mstStorage.size
        val dtlCount = dtlStorage.values.sumOf { it.size }

        mstStorage.clear()
        dtlStorage.clear()

        logger.info("📝 [WRITE #$opId] 저장소 초기화 완료 - 위성: ${mstCount}개 삭제, DTL 포인트: ${dtlCount}개 삭제")
    }

    // ========================================
    // 저장소 읽기 작업 (Read Operations)
    // ========================================

    /**
     * 특정 위성의 MST 데이터를 반환합니다.
     *
     * @param satelliteId 위성 ID
     */
    fun getMstBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        val opId = readCounter.incrementAndGet()
        val result = mstStorage[satelliteId]
        logger.debug("📖 [READ #$opId] getMstBySatelliteId($satelliteId) → ${result?.size ?: "null"}")
        return result
    }

    /**
     * 특정 위성의 DTL 데이터를 반환합니다.
     *
     * @param satelliteId 위성 ID
     */
    fun getDtlBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        val opId = readCounter.incrementAndGet()
        val result = dtlStorage[satelliteId]
        logger.debug("📖 [READ #$opId] getDtlBySatelliteId($satelliteId) → ${result?.size ?: "null"}")
        return result
    }

    /**
     * 모든 위성의 MST 데이터를 반환합니다.
     */
    fun getAllMst(): Map<String, List<Map<String, Any?>>> {
        val opId = readCounter.incrementAndGet()
        val result = mstStorage.toMap()
        logger.debug("📖 [READ #$opId] getAllMst() → ${result.size}개 위성")
        return result
    }

    /**
     * 모든 위성의 DTL 데이터를 반환합니다.
     */
    fun getAllDtl(): Map<String, List<Map<String, Any?>>> {
        val opId = readCounter.incrementAndGet()
        val result = dtlStorage.toMap()
        logger.debug("📖 [READ #$opId] getAllDtl() → ${result.size}개 위성")
        return result
    }

    /**
     * 모든 위성의 MST 데이터를 플랫하게 반환합니다.
     */
    fun getAllMstFlattened(): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = mstStorage.values.flatten()
        logger.debug("📖 [READ #$opId] getAllMstFlattened() → ${result.size}개")
        return result
    }

    /**
     * 모든 위성의 DTL 데이터를 플랫하게 반환합니다.
     */
    fun getAllDtlFlattened(): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = dtlStorage.values.flatten()
        logger.debug("📖 [READ #$opId] getAllDtlFlattened() → ${result.size}개")
        return result
    }

    /**
     * MstId로 MST 데이터를 검색합니다 (모든 위성에서).
     *
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입 (선택적)
     */
    fun findMstById(mstId: Long, dataType: String? = null): Map<String, Any?>? {
        val opId = readCounter.incrementAndGet()
        val result = mstStorage.values.flatten().find {
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            val matches = dataMstId == mstId
            if (dataType != null) {
                matches && it["DataType"] == dataType
            } else {
                matches
            }
        }
        logger.debug("📖 [READ #$opId] findMstById($mstId, $dataType) → ${if (result != null) "found" else "null"}")
        return result
    }

    /**
     * MstId와 데이터 타입으로 DTL 데이터를 검색합니다.
     *
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입
     * @param detailId 세부 ID (기본값: 0)
     */
    fun findDtlByMstIdAndDataType(mstId: Long, dataType: String, detailId: Int = 0): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = dtlStorage.values.flatten().filter {
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
            val itDataType = it["DataType"] as? String
            dataMstId == mstId && dataDetailId == detailId && itDataType == dataType
        }

        if (result.isEmpty()) {
            logger.warn("⚠️ [READ #$opId] findDtlByMstIdAndDataType($mstId, $dataType, $detailId) → 0개 (데이터 없음)")
        } else {
            logger.debug("📖 [READ #$opId] findDtlByMstIdAndDataType($mstId, $dataType, $detailId) → ${result.size}개")
        }

        return result
    }

    /**
     * 특정 위성의 DTL 데이터에서 MstId와 DataType으로 검색합니다.
     *
     * @param satelliteId 위성 ID
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입
     * @param detailId 세부 ID (기본값: 0)
     */
    fun findDtlBySatelliteAndMstId(
        satelliteId: String,
        mstId: Long,
        dataType: String,
        detailId: Int = 0
    ): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val dtlData = dtlStorage[satelliteId] ?: return emptyList()

        val result = dtlData.filter {
            val dataMstId = (it["MstId"] as? Number)?.toLong()
            val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
            val itDataType = it["DataType"] as? String
            dataMstId == mstId && dataDetailId == detailId && itDataType == dataType
        }

        logger.debug("📖 [READ #$opId] findDtlBySatelliteAndMstId($satelliteId, $mstId, $dataType, $detailId) → ${result.size}개")
        return result
    }

    // ========================================
    // 통계 및 디버깅
    // ========================================

    /**
     * 저장된 위성 수를 반환합니다.
     */
    fun getSatelliteCount(): Int = mstStorage.size

    /**
     * 특정 위성이 저장되어 있는지 확인합니다.
     */
    fun containsSatellite(satelliteId: String): Boolean = mstStorage.containsKey(satelliteId)

    /**
     * 저장소 상태 요약을 반환합니다.
     */
    fun getStorageSummary(): Map<String, Any> {
        val totalSatellites = mstStorage.size
        val totalPasses = mstStorage.values.sumOf { it.size }
        val totalTrackingPoints = dtlStorage.values.sumOf { it.size }

        val satelliteStats = mstStorage.map { (satelliteId, mstData) ->
            val dtlData = dtlStorage[satelliteId] ?: emptyList()
            mapOf(
                "satelliteId" to satelliteId,
                "passCount" to mstData.size,
                "trackingPointCount" to dtlData.size
            )
        }

        return mapOf(
            "totalSatellites" to totalSatellites,
            "totalPasses" to totalPasses,
            "totalTrackingPoints" to totalTrackingPoints,
            "satelliteStats" to satelliteStats,
            "writeOperations" to writeCounter.get(),
            "readOperations" to readCounter.get()
        )
    }

    /**
     * 저장소 상태 요약 로그 출력
     */
    private fun logStorageSummary(opId: Long) {
        val summary = getStorageSummary()
        logger.info("📊 [WRITE #$opId] 저장소 상태:")
        logger.info("   - 총 위성: ${summary["totalSatellites"]}개")
        logger.info("   - 총 패스: ${summary["totalPasses"]}개")
        logger.info("   - 총 추적 포인트: ${summary["totalTrackingPoints"]}개")
    }

    /**
     * 전체 상태 덤프 (디버깅용)
     */
    fun dumpState(): String {
        val sb = StringBuilder()
        sb.appendLine("=== PassScheduleDataRepository State ===")
        sb.appendLine("Satellites: ${getSatelliteCount()}")
        sb.appendLine("Total Passes: ${mstStorage.values.sumOf { it.size }}")
        sb.appendLine("Total Tracking Points: ${dtlStorage.values.sumOf { it.size }}")
        sb.appendLine("Write Operations: ${writeCounter.get()}")
        sb.appendLine("Read Operations: ${readCounter.get()}")
        sb.appendLine()

        mstStorage.forEach { (satelliteId, mstData) ->
            val dtlData = dtlStorage[satelliteId] ?: emptyList()
            sb.appendLine("Satellite $satelliteId: ${mstData.size} passes, ${dtlData.size} tracking points")
        }

        return sb.toString()
    }

    // ========================================
    // DB 연동 (Write-through)
    // ========================================

    /**
     * DB에 위성별 스케줄 데이터를 저장합니다.
     * MST → tracking_session, DTL → tracking_trajectory
     */
    private fun saveToDatabase(
        satelliteId: String,
        mstData: List<Map<String, Any?>>,
        dtlData: List<Map<String, Any?>>,
        opId: Long
    ) {
        if (sessionRepository == null || trajectoryRepository == null) {
            logger.warn("DB Repository가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        // MST 데이터 → TrackingSession 저장
        mstData.forEach { mst ->
            try {
                val session = mapMstToSession(satelliteId, mst)
                sessionRepository.save(session)
                    .doOnSuccess { saved: TrackingSessionEntity ->
                        logger.debug("📝 [DB #$opId] Session 저장: id=${saved.id}, satelliteId=$satelliteId, mstId=${saved.mstId}")
                        // 해당 세션의 DTL 데이터 저장
                        val sessionDtlData = dtlData.filter { dtl ->
                            val dtlMstId = (dtl["MstId"] as? Number)?.toLong()
                            val dtlDataType = dtl["DataType"] as? String
                            dtlMstId == saved.mstId && dtlDataType == saved.dataType
                        }
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

        logger.info("📝 [DB #$opId] PassSchedule 스케줄 DB 저장 요청 완료 (위성: $satelliteId, MST: ${mstData.size}개)")
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
     */
    private fun mapMstToSession(satelliteId: String, mst: Map<String, Any?>): TrackingSessionEntity {
        val mstId = (mst["MstId"] as? Number)?.toLong() ?: 0L
        val detailId = (mst["DetailId"] as? Number)?.toInt() ?: 0
        val satelliteName = mst["SatelliteName"] as? String
        val dataType = mst["DataType"] as? String ?: "original"

        // 시간 파싱
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val startTime = parseTime(mst["StartTime"]) ?: now
        val endTime = parseTime(mst["EndTime"]) ?: now
        val duration = (mst["Duration"] as? Number)?.toInt()

        // 각도 정보
        val maxElevation = (mst["MaxElevation"] as? Number)?.toDouble()
        val maxAzimuthRate = (mst["MaxAzimuthRate"] as? Number)?.toDouble()
        val maxElevationRate = (mst["MaxElevationRate"] as? Number)?.toDouble()
        val keyholeDetected = mst["KeyholeDetected"] as? Boolean ?: false
        val recommendedTrainAngle = (mst["RecommendedTrainAngle"] as? Number)?.toDouble()
        val totalPoints = (mst["TotalPoints"] as? Number)?.toInt()

        return TrackingSessionEntity(
            mstId = mstId,
            detailId = detailId,
            satelliteId = satelliteId,
            satelliteName = satelliteName,
            trackingMode = "PASS_SCHEDULE",
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
}
