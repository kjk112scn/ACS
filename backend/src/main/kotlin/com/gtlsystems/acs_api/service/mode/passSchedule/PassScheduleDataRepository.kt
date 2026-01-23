package com.gtlsystems.acs_api.service.mode.passSchedule

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
     * ✅ P7 Fix: 서버 시작 시 DB에서 기존 세션 + 궤적을 메모리로 로드
     * TLE 등록 후 서버 재시작해도 스케줄 목록이 유지됨
     */
    @PostConstruct
    fun initFromDatabase() {
        if (sessionRepository == null) {
            logger.warn("SessionRepository가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        sessionRepository.findByTrackingMode("PASS_SCHEDULE")
            .collectList()
            .doOnSuccess { sessions: List<TrackingSessionEntity> ->
                if (sessions.isEmpty()) {
                    logger.info("📥 [DB→메모리] PassSchedule 세션 없음")
                    return@doOnSuccess
                }

                logger.info("📥 [DB→메모리] ${sessions.size}개 PassSchedule 세션 로딩 시작")

                // 위성별로 그룹화하여 저장
                val sessionsBySatellite = sessions.groupBy { it.satelliteId }

                sessionsBySatellite.forEach { (satelliteId, satelliteSessions) ->
                    val mstData = mutableListOf<Map<String, Any?>>()

                    satelliteSessions.forEach { session ->
                        // MST 변환
                        val mst = mapSessionToMstForLoad(session)
                        mstData.add(mst)

                        // ✅ P7: 해당 세션의 DTL(trajectory)도 로드
                        if (trajectoryRepository != null && session.id != null) {
                            loadTrajectoryForSession(session)
                        }
                    }

                    // 위성별로 저장
                    val existingMst = mstStorage[satelliteId] ?: emptyList()
                    mstStorage[satelliteId] = existingMst + mstData
                }

                logger.info("📥 [DB→메모리] ${sessionsBySatellite.size}개 위성, ${sessions.size}개 세션 로드 완료")
            }
            .doOnError { e ->
                logger.error("❌ [DB→메모리] PassSchedule 세션 로딩 실패: ${e.message}")
            }
            .subscribe()
    }

    /**
     * ✅ P7 Fix: 세션별 trajectory를 DB에서 로드하여 dtlStorage에 추가
     */
    private fun loadTrajectoryForSession(session: TrackingSessionEntity) {
        if (trajectoryRepository == null || session.id == null) return

        trajectoryRepository.findBySessionId(session.id)
            .collectList()
            .doOnSuccess { trajectories ->
                if (trajectories.isEmpty()) {
                    logger.debug("📥 [DB→메모리] 세션 ${session.id} (mstId=${session.mstId}, detailId=${session.detailId})의 trajectory 없음")
                    return@doOnSuccess
                }

                // Trajectory → DTL Map 형식으로 변환
                val dtlData = trajectories.map { traj ->
                    mapTrajectoryToDtlForLoad(session, traj)
                }

                // 위성별로 저장
                val satelliteId = session.satelliteId
                val existingDtl = dtlStorage[satelliteId] ?: emptyList()
                dtlStorage[satelliteId] = existingDtl + dtlData

                logger.debug("📥 [DB→메모리] 세션 ${session.id} → ${dtlData.size}개 DTL 로드")
            }
            .doOnError { e ->
                logger.error("❌ [DB→메모리] 세션 ${session.id} trajectory 로딩 실패: ${e.message}")
            }
            .subscribe()
    }

    /**
     * ✅ P7 Fix: TrackingSessionEntity → MST Map 변환 (DB 로드용)
     * OffsetDateTime → ZonedDateTime 변환 (이전 RAM 형식과 동일하게)
     */
    private fun mapSessionToMstForLoad(session: TrackingSessionEntity): Map<String, Any?> {
        // OffsetDateTime → ZonedDateTime (UTC) 변환 (이전 RAM 형식과 동일)
        val startTimeZoned = session.startTime.atZoneSameInstant(ZoneOffset.UTC)
        val endTimeZoned = session.endTime.atZoneSameInstant(ZoneOffset.UTC)
        val maxElevationTimeZoned = session.maxElevationTime?.atZoneSameInstant(ZoneOffset.UTC)

        return mutableMapOf<String, Any?>(
            "MstId" to session.mstId,
            "DetailId" to session.detailId,
            "DataType" to session.dataType,
            "SatelliteID" to session.satelliteId,
            "SatelliteName" to session.satelliteName,
            // 시간 정보 (ZonedDateTime으로 변환)
            "StartTime" to startTimeZoned,
            "EndTime" to endTimeZoned,
            "Duration" to session.duration,
            "MaxElevationTime" to maxElevationTimeZoned,
            // 기본 각도 정보
            "StartAzimuthAngle" to session.startAzimuth,
            "EndAzimuthAngle" to session.endAzimuth,
            "StartElevationAngle" to session.startElevation,
            "EndElevationAngle" to session.endElevation,
            "Train" to session.trainAngle,
            // 기본 Peak 값
            "MaxElevation" to session.maxElevation,
            "MaxAzRate" to session.maxAzimuthRate,
            "MaxElRate" to session.maxElevationRate,
            "MaxAzimuthAccel" to session.maxAzimuthAccel,
            "MaxElevationAccel" to session.maxElevationAccel,
            // 키홀
            "IsKeyhole" to session.keyholeDetected,
            "RecommendedTrainAngle" to session.recommendedTrainAngle,
            "TotalPoints" to session.totalPoints,
            // Original (2축)
            "OriginalStartAzimuth" to session.originalStartAzimuth,
            "OriginalEndAzimuth" to session.originalEndAzimuth,
            "OriginalMaxElevation" to session.originalMaxElevation,
            "OriginalMaxAzRate" to session.originalMaxAzRate,
            "OriginalMaxElRate" to session.originalMaxElRate,
            // FinalTransformed (3축, Train=0)
            "FinalTransformedStartAzimuth" to session.finalStartAzimuth,
            "FinalTransformedEndAzimuth" to session.finalEndAzimuth,
            "FinalTransformedStartElevation" to session.finalStartElevation,
            "FinalTransformedEndElevation" to session.finalEndElevation,
            "FinalTransformedMaxElevation" to session.finalMaxElevation,
            "FinalTransformedMaxAzRate" to session.finalMaxAzRate,
            "FinalTransformedMaxElRate" to session.finalMaxElRate,
            // KeyholeAxisTransformed
            "KeyholeAxisTransformedMaxAzRate" to session.keyholeAxisMaxAzRate,
            "KeyholeAxisTransformedMaxElRate" to session.keyholeAxisMaxElRate,
            // KeyholeFinalTransformed
            "KeyholeFinalTransformedStartAzimuth" to session.keyholeFinalStartAzimuth,
            "KeyholeFinalTransformedEndAzimuth" to session.keyholeFinalEndAzimuth,
            "KeyholeFinalTransformedStartElevation" to session.keyholeFinalStartElevation,
            "KeyholeFinalTransformedEndElevation" to session.keyholeFinalEndElevation,
            "KeyholeFinalTransformedMaxElevation" to session.keyholeFinalMaxElevation,
            "KeyholeFinalTransformedMaxAzRate" to session.keyholeFinalMaxAzRate,
            "KeyholeFinalTransformedMaxElRate" to session.keyholeFinalMaxElRate,
            // KeyholeOptimizedFinalTransformed
            "KeyholeOptimizedFinalTransformedStartAzimuth" to session.keyholeOptStartAzimuth,
            "KeyholeOptimizedFinalTransformedEndAzimuth" to session.keyholeOptEndAzimuth,
            "KeyholeOptimizedFinalTransformedStartElevation" to session.keyholeOptStartElevation,
            "KeyholeOptimizedFinalTransformedEndElevation" to session.keyholeOptEndElevation,
            "KeyholeOptimizedFinalTransformedMaxElevation" to session.keyholeOptMaxElevation,
            "KeyholeOptimizedFinalTransformedMaxAzRate" to session.keyholeOptMaxAzRate,
            "KeyholeOptimizedFinalTransformedMaxElRate" to session.keyholeOptMaxElRate,
            // P7: CreatedAt 유지
            "CreatedAt" to session.createdAt
        )
    }

    /**
     * ✅ P7 Fix: TrackingTrajectoryEntity → DTL Map 변환 (DB 로드용)
     * OffsetDateTime → ZonedDateTime 변환 (이전 RAM 형식과 동일하게)
     */
    private fun mapTrajectoryToDtlForLoad(session: TrackingSessionEntity, traj: TrackingTrajectoryEntity): Map<String, Any?> {
        // OffsetDateTime → ZonedDateTime (UTC) 변환 (이전 RAM 형식과 동일)
        val zonedTime = traj.timestamp.atZoneSameInstant(ZoneOffset.UTC)

        return mutableMapOf<String, Any?>(
            "MstId" to session.mstId,
            "DetailId" to traj.detailId,
            "DataType" to traj.dataType,
            "Index" to traj.index,
            "Time" to zonedTime,
            "Timestamp" to zonedTime,
            "Azimuth" to traj.azimuth,
            "Elevation" to traj.elevation,
            "Train" to traj.train,
            "AzimuthRate" to traj.azimuthRate,
            "ElevationRate" to traj.elevationRate,
            // P7: CreatedAt 유지
            "CreatedAt" to traj.createdAt
        )
    }

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
     * ✅ P7 Fix: 이력 보존 (기존 데이터에 추가, 덮어쓰기 X)
     *
     * @param satelliteId 위성 ID
     * @param mstData 마스터 데이터 리스트
     * @param dtlData 세부 데이터 리스트
     */
    fun saveSatelliteData(satelliteId: String, mstData: List<Map<String, Any?>>, dtlData: List<Map<String, Any?>>) {
        val opId = writeCounter.incrementAndGet()
        // ✅ P6: 등록 건 그룹핑을 위한 동일 timestamp 생성
        val registrationTime = OffsetDateTime.now(ZoneOffset.UTC)
        logger.info("📝 [WRITE #$opId] saveSatelliteData($satelliteId) - MST: ${mstData.size}개, DTL: ${dtlData.size}개, registrationTime: $registrationTime")

        // ✅ P6: 모든 데이터에 동일한 CreatedAt 추가 (그룹핑용)
        val mstWithCreatedAt = mstData.map { it + ("CreatedAt" to registrationTime) }
        val dtlWithCreatedAt = dtlData.map { it + ("CreatedAt" to registrationTime) }

        // ✅ P7 Fix: 이력 보존 (기존 데이터에 추가)
        val existingMst = mstStorage[satelliteId] ?: emptyList()
        mstStorage[satelliteId] = existingMst + mstWithCreatedAt

        val existingDtl = dtlStorage[satelliteId] ?: emptyList()
        dtlStorage[satelliteId] = existingDtl + dtlWithCreatedAt

        logStorageSummary(opId)

        // DB 저장 (Write-through) - ✅ P6: registrationTime 전달
        saveToDatabase(satelliteId, mstWithCreatedAt, dtlWithCreatedAt, opId, registrationTime)
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
     * ✅ P7 Fix: 최신 등록 건만 반환 (CreatedAt 필터링)
     *
     * @param satelliteId 위성 ID
     */
    fun getMstBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        val opId = readCounter.incrementAndGet()
        val allData = mstStorage[satelliteId] ?: return null

        // P7: 최신 CreatedAt 필터링
        val latestCreatedAt = allData
            .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
            .maxOrNull()

        val result = if (latestCreatedAt == null) {
            allData
        } else {
            allData.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
        }

        logger.debug("📖 [READ #$opId] getMstBySatelliteId($satelliteId) → ${result.size}개 (전체: ${allData.size}개)")
        return result
    }

    /**
     * 특정 위성의 DTL 데이터를 반환합니다.
     * ✅ P7 Fix: 최신 등록 건만 반환 (CreatedAt 필터링)
     *
     * @param satelliteId 위성 ID
     */
    fun getDtlBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        val opId = readCounter.incrementAndGet()
        val allData = dtlStorage[satelliteId] ?: return null

        // P7: 최신 CreatedAt 필터링
        val latestCreatedAt = allData
            .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
            .maxOrNull()

        val result = if (latestCreatedAt == null) {
            allData
        } else {
            allData.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
        }

        logger.debug("📖 [READ #$opId] getDtlBySatelliteId($satelliteId) → ${result.size}개 (전체: ${allData.size}개)")
        return result
    }

    /**
     * 모든 위성의 MST 데이터를 반환합니다.
     * ✅ P7 Fix: 각 위성별로 최신 등록 건만 반환 (CreatedAt 필터링)
     */
    fun getAllMst(): Map<String, List<Map<String, Any?>>> {
        val opId = readCounter.incrementAndGet()

        // P7: 각 위성별로 최신 CreatedAt 필터링
        val result = mstStorage.mapValues { (_, dataList) ->
            val latestCreatedAt = dataList
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            if (latestCreatedAt == null) {
                dataList
            } else {
                dataList.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
            }
        }

        logger.debug("📖 [READ #$opId] getAllMst() → ${result.size}개 위성")
        return result
    }

    /**
     * 모든 위성의 DTL 데이터를 반환합니다.
     * ✅ P7 Fix: 각 위성별로 최신 등록 건만 반환 (CreatedAt 필터링)
     */
    fun getAllDtl(): Map<String, List<Map<String, Any?>>> {
        val opId = readCounter.incrementAndGet()

        // P7: 각 위성별로 최신 CreatedAt 필터링
        val result = dtlStorage.mapValues { (_, dataList) ->
            val latestCreatedAt = dataList
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            if (latestCreatedAt == null) {
                dataList
            } else {
                dataList.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
            }
        }

        logger.debug("📖 [READ #$opId] getAllDtl() → ${result.size}개 위성")
        return result
    }

    /**
     * 모든 위성의 MST 데이터를 플랫하게 반환합니다.
     * ✅ P7 Fix: 최신 등록 건만 반환 (CreatedAt 필터링)
     */
    fun getAllMstFlattened(): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val allData = mstStorage.values.flatten()

        // P7: 전체에서 최신 CreatedAt 필터링
        val latestCreatedAt = allData
            .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
            .maxOrNull()

        val result = if (latestCreatedAt == null) {
            allData
        } else {
            allData.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
        }

        logger.debug("📖 [READ #$opId] getAllMstFlattened() → ${result.size}개 (전체: ${allData.size}개)")
        return result
    }

    /**
     * 모든 위성의 DTL 데이터를 플랫하게 반환합니다.
     * ✅ P7 Fix: 최신 등록 건만 반환 (CreatedAt 필터링)
     */
    fun getAllDtlFlattened(): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val allData = dtlStorage.values.flatten()

        // P7: 전체에서 최신 CreatedAt 필터링
        val latestCreatedAt = allData
            .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
            .maxOrNull()

        val result = if (latestCreatedAt == null) {
            allData
        } else {
            allData.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
        }

        logger.debug("📖 [READ #$opId] getAllDtlFlattened() → ${result.size}개 (전체: ${allData.size}개)")
        return result
    }

    /**
     * MstId로 MST 데이터를 검색합니다 (모든 위성에서).
     * ✅ P7 Fix: 최신 등록 건에서만 검색 (CreatedAt 필터링)
     *
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입 (선택적)
     */
    fun findMstById(mstId: Long, dataType: String? = null): Map<String, Any?>? {
        val opId = readCounter.incrementAndGet()
        val allData = mstStorage.values.flatten()

        // P7: 최신 CreatedAt 필터링
        val latestCreatedAt = allData
            .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
            .maxOrNull()

        val filteredData = if (latestCreatedAt == null) {
            allData
        } else {
            allData.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
        }

        val result = filteredData.find {
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
     * ✅ P7 Fix: 최신 등록 건에서만 검색 (CreatedAt 필터링)
     *
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입
     * @param detailId 세부 ID (기본값: 0)
     */
    fun findDtlByMstIdAndDataType(mstId: Long, dataType: String, detailId: Int = 0): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val allData = dtlStorage.values.flatten()

        // P7: 최신 CreatedAt 필터링
        val latestCreatedAt = allData
            .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
            .maxOrNull()

        val filteredData = if (latestCreatedAt == null) {
            allData
        } else {
            allData.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
        }

        val result = filteredData.filter {
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
     * ✅ P7 Fix: 최신 등록 건에서만 검색 (CreatedAt 필터링)
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

        // P7: 최신 CreatedAt 필터링
        val latestCreatedAt = dtlData
            .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
            .maxOrNull()

        val filteredData = if (latestCreatedAt == null) {
            dtlData
        } else {
            dtlData.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
        }

        val result = filteredData.filter {
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
     * V006 P1 Fix: mstId와 detailId로 tracking_session의 id를 조회합니다.
     *
     * @param mstId 마스터 ID
     * @param detailId 패스 구분자
     * @param trackingMode 추적 모드 (기본값: PASS_SCHEDULE)
     * @return 세션 ID (없으면 null)
     */
    fun getSessionIdByMstAndDetail(
        mstId: Long,
        detailId: Int,
        trackingMode: String = "PASS_SCHEDULE"  // ✅ mapMstToSession과 일치
    ): Long? {
        return try {
            sessionRepository?.findByMstIdAndDetailIdAndTrackingMode(mstId, detailId, trackingMode)
                ?.block()
                ?.id
        } catch (e: Exception) {
            logger.warn("⚠️ SessionId 조회 실패: mstId=$mstId, detailId=$detailId, error=${e.message}")
            null
        }
    }

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
        opId: Long,
        registrationTime: OffsetDateTime  // P6: 등록 건 그룹핑용 통일 시간
    ) {
        if (sessionRepository == null || trajectoryRepository == null) {
            logger.warn("DB Repository가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        // ✅ V006 P1 Fix: (mstId, detailId) 기준으로 그룹화하여 1 Pass = 1 Session 보장
        val groupedMst = mstData.groupBy { mst ->
            val mstId = (mst["MstId"] as? Number)?.toLong() ?: 0L
            val detailId = (mst["DetailId"] as? Number)?.toInt() ?: 0
            Pair(mstId, detailId)
        }

        logger.info("📝 [DB #$opId] MST ${mstData.size}개 → ${groupedMst.size}개 세션으로 그룹화 (V006 정책)")

        // 각 그룹에서 대표 세션 1개만 저장
        groupedMst.forEach { (key, mstGroup) ->
            val (mstId, detailId) = key
            try {
                // 대표 MST 선택: 'original' 우선, 없으면 첫 번째
                val representativeMst = mstGroup.find { it["DataType"] == "original" }
                    ?: mstGroup.firstOrNull()
                    ?: return@forEach

                // 모든 data_type의 DTL 데이터 합산 (total_points용)
                val allDtlForSession = dtlData.filter { dtl ->
                    val dtlMstId = (dtl["MstId"] as? Number)?.toLong()
                    val dtlDetailId = (dtl["DetailId"] as? Number)?.toInt() ?: 0
                    dtlMstId == mstId && dtlDetailId == detailId
                }

                // 세션 저장 (중복 체크 후 UPSERT) - P6: registrationTime 전달
                val session = mapMstToSession(satelliteId, representativeMst, allDtlForSession.size, registrationTime)
                saveOrUpdateSession(session, allDtlForSession, opId, registrationTime)
            } catch (e: RuntimeException) {
                logger.error("❌ [DB #$opId] MST($mstId, $detailId) 저장 실패: ${e.message}")
            }
        }

        logger.info("📝 [DB #$opId] PassSchedule 스케줄 DB 저장 요청 완료 (위성: $satelliteId, ${groupedMst.size}개 세션)")
    }

    /**
     * V006 P1 Fix: 세션 UPSERT (존재하면 스킵, 없으면 INSERT)
     *
     * @param registrationTime P6: 등록 건 그룹핑용 통일 시간
     */
    private fun saveOrUpdateSession(
        session: TrackingSessionEntity,
        dtlData: List<Map<String, Any?>>,
        opId: Long,
        registrationTime: OffsetDateTime
    ) {
        sessionRepository?.findByMstIdAndDetailIdAndTrackingMode(
            session.mstId,
            session.detailId,
            session.trackingMode
        )?.hasElement()
            ?.flatMap { exists ->
                if (exists) {
                    logger.debug("📝 [DB #$opId] Session 이미 존재: mstId=${session.mstId}, detailId=${session.detailId} (스킵)")
                    reactor.core.publisher.Mono.empty()
                } else {
                    sessionRepository.save(session)
                }
            }
            ?.doOnSuccess { saved: TrackingSessionEntity? ->
                if (saved != null) {
                    logger.debug("📝 [DB #$opId] Session 저장: id=${saved.id}, mstId=${saved.mstId}, detailId=${saved.detailId}")
                    // Trajectory 저장 - P6: registrationTime 전달
                    if (dtlData.isNotEmpty() && saved.id != null) {
                        saveTrajectories(saved.id, dtlData, opId, registrationTime)
                    }
                }
            }
            ?.doOnError { e: Throwable ->
                logger.error("❌ [DB #$opId] Session 저장 실패: ${e.message}")
            }
            ?.subscribe()
    }

    /**
     * DTL 데이터를 trajectory로 저장합니다.
     *
     * @param registrationTime P6: 등록 건 그룹핑용 통일 시간
     */
    private fun saveTrajectories(sessionId: Long, dtlData: List<Map<String, Any?>>, opId: Long, registrationTime: OffsetDateTime) {
        if (trajectoryRepository == null) return

        val trajectories = dtlData.mapNotNull { dtl ->
            try {
                mapDtlToTrajectory(sessionId, dtl, registrationTime)
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
     * Select Schedule에서 표시하는 모든 변환 단계별 메타데이터를 매핑합니다.
     *
     * @param satelliteId 위성 ID
     * @param mst MST 데이터
     * @param dtlCount DTL 데이터 개수 (totalPoints 폴백용)
     * @param registrationTime P6: 등록 건 그룹핑용 통일 시간
     */
    private fun mapMstToSession(
        satelliteId: String,
        mst: Map<String, Any?>,
        dtlCount: Int = 0,
        registrationTime: OffsetDateTime? = null
    ): TrackingSessionEntity {
        val mstId = (mst["MstId"] as? Number)?.toLong() ?: 0L
        val detailId = (mst["DetailId"] as? Number)?.toInt() ?: 0
        val satelliteName = mst["SatelliteName"] as? String
        val dataType = mst["DataType"] as? String ?: "original"

        // ===== 시간 파싱 =====
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val startTime = parseTime(mst["StartTime"]) ?: now
        val endTime = parseTime(mst["EndTime"]) ?: now
        val duration = parseDurationToSeconds(mst["Duration"], startTime, endTime)
        val maxElevationTime = parseTime(mst["MaxElevationTime"])

        // ===== 기본 각도 정보 (최종 사용값) =====
        val startAzimuth = (mst["StartAzimuthAngle"] as? Number)?.toDouble()
            ?: (mst["startAzimuthAngle"] as? Number)?.toDouble()
        val endAzimuth = (mst["EndAzimuthAngle"] as? Number)?.toDouble()
            ?: (mst["endAzimuthAngle"] as? Number)?.toDouble()
        val startElevation = (mst["StartElevationAngle"] as? Number)?.toDouble()
            ?: (mst["startElevationAngle"] as? Number)?.toDouble()
        val endElevation = (mst["EndElevationAngle"] as? Number)?.toDouble()
            ?: (mst["endElevationAngle"] as? Number)?.toDouble()
        val trainAngle = (mst["Train"] as? Number)?.toDouble()
            ?: (mst["train"] as? Number)?.toDouble()

        // ===== 기본 Peak 값 =====
        val maxElevation = (mst["MaxElevation"] as? Number)?.toDouble()
        val maxAzimuthRate = (mst["MaxAzRate"] as? Number)?.toDouble()
            ?: (mst["MaxAzimuthRate"] as? Number)?.toDouble()
        val maxElevationRate = (mst["MaxElRate"] as? Number)?.toDouble()
            ?: (mst["MaxElevationRate"] as? Number)?.toDouble()
        val maxAzimuthAccel = (mst["MaxAzimuthAccel"] as? Number)?.toDouble()
            ?: (mst["maxAzimuthAccel"] as? Number)?.toDouble()
        val maxElevationAccel = (mst["MaxElevationAccel"] as? Number)?.toDouble()
            ?: (mst["maxElevationAccel"] as? Number)?.toDouble()

        val keyholeDetected = mst["IsKeyhole"] as? Boolean
            ?: mst["KeyholeDetected"] as? Boolean ?: false
        val recommendedTrainAngle = (mst["RecommendedTrainAngle"] as? Number)?.toDouble()
        val totalPoints = (mst["TotalPoints"] as? Number)?.toInt()
            ?: if (dtlCount > 0) dtlCount else null

        // ===== Original (2축) 메타데이터 =====
        val originalStartAzimuth = (mst["OriginalStartAzimuth"] as? Number)?.toDouble()
            ?: (mst["originalStartAzimuth"] as? Number)?.toDouble()
        val originalEndAzimuth = (mst["OriginalEndAzimuth"] as? Number)?.toDouble()
            ?: (mst["originalEndAzimuth"] as? Number)?.toDouble()
        val originalMaxElevation = (mst["OriginalMaxElevation"] as? Number)?.toDouble()
        val originalMaxAzRate = (mst["OriginalMaxAzRate"] as? Number)?.toDouble()
        val originalMaxElRate = (mst["OriginalMaxElRate"] as? Number)?.toDouble()

        // ===== FinalTransformed (3축, Train=0, ±270°) =====
        val finalStartAzimuth = (mst["FinalTransformedStartAzimuth"] as? Number)?.toDouble()
        val finalEndAzimuth = (mst["FinalTransformedEndAzimuth"] as? Number)?.toDouble()
        val finalStartElevation = (mst["FinalTransformedStartElevation"] as? Number)?.toDouble()
        val finalEndElevation = (mst["FinalTransformedEndElevation"] as? Number)?.toDouble()
        val finalMaxElevation = (mst["FinalTransformedMaxElevation"] as? Number)?.toDouble()
        val finalMaxAzRate = (mst["FinalTransformedMaxAzRate"] as? Number)?.toDouble()
        val finalMaxElRate = (mst["FinalTransformedMaxElRate"] as? Number)?.toDouble()

        // ===== KeyholeAxisTransformed (3축, Train≠0, 각도 제한 전) =====
        val keyholeAxisMaxAzRate = (mst["KeyholeAxisTransformedMaxAzRate"] as? Number)?.toDouble()
        val keyholeAxisMaxElRate = (mst["KeyholeAxisTransformedMaxElRate"] as? Number)?.toDouble()

        // ===== KeyholeFinalTransformed (3축, Train≠0, ±270°) =====
        val keyholeFinalStartAzimuth = (mst["KeyholeFinalTransformedStartAzimuth"] as? Number)?.toDouble()
        val keyholeFinalEndAzimuth = (mst["KeyholeFinalTransformedEndAzimuth"] as? Number)?.toDouble()
        val keyholeFinalStartElevation = (mst["KeyholeFinalTransformedStartElevation"] as? Number)?.toDouble()
        val keyholeFinalEndElevation = (mst["KeyholeFinalTransformedEndElevation"] as? Number)?.toDouble()
        val keyholeFinalMaxElevation = (mst["KeyholeFinalTransformedMaxElevation"] as? Number)?.toDouble()
        val keyholeFinalMaxAzRate = (mst["KeyholeFinalTransformedMaxAzRate"] as? Number)?.toDouble()
        val keyholeFinalMaxElRate = (mst["KeyholeFinalTransformedMaxElRate"] as? Number)?.toDouble()

        // ===== KeyholeOptimizedFinalTransformed (최적화 Train, ±270°) =====
        val keyholeOptStartAzimuth = (mst["KeyholeOptimizedFinalTransformedStartAzimuth"] as? Number)?.toDouble()
        val keyholeOptEndAzimuth = (mst["KeyholeOptimizedFinalTransformedEndAzimuth"] as? Number)?.toDouble()
        val keyholeOptStartElevation = (mst["KeyholeOptimizedFinalTransformedStartElevation"] as? Number)?.toDouble()
        val keyholeOptEndElevation = (mst["KeyholeOptimizedFinalTransformedEndElevation"] as? Number)?.toDouble()
        val keyholeOptMaxElevation = (mst["KeyholeOptimizedFinalTransformedMaxElevation"] as? Number)?.toDouble()
        val keyholeOptMaxAzRate = (mst["KeyholeOptimizedFinalTransformedMaxAzRate"] as? Number)?.toDouble()
        val keyholeOptMaxElRate = (mst["KeyholeOptimizedFinalTransformedMaxElRate"] as? Number)?.toDouble()

        return TrackingSessionEntity(
            mstId = mstId,
            detailId = detailId,
            satelliteId = satelliteId,
            satelliteName = satelliteName,
            trackingMode = "PASS_SCHEDULE",
            dataType = dataType,
            // 시간 정보
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            maxElevationTime = maxElevationTime,
            // 기본 각도 정보
            startAzimuth = startAzimuth,
            endAzimuth = endAzimuth,
            startElevation = startElevation,
            endElevation = endElevation,
            trainAngle = trainAngle,
            // 기본 Peak 값
            maxElevation = maxElevation,
            maxAzimuthRate = maxAzimuthRate,
            maxElevationRate = maxElevationRate,
            maxAzimuthAccel = maxAzimuthAccel,
            maxElevationAccel = maxElevationAccel,
            keyholeDetected = keyholeDetected,
            recommendedTrainAngle = recommendedTrainAngle,
            totalPoints = totalPoints,
            // Original (2축)
            originalStartAzimuth = originalStartAzimuth,
            originalEndAzimuth = originalEndAzimuth,
            originalMaxElevation = originalMaxElevation,
            originalMaxAzRate = originalMaxAzRate,
            originalMaxElRate = originalMaxElRate,
            // FinalTransformed (3축, Train=0)
            finalStartAzimuth = finalStartAzimuth,
            finalEndAzimuth = finalEndAzimuth,
            finalStartElevation = finalStartElevation,
            finalEndElevation = finalEndElevation,
            finalMaxElevation = finalMaxElevation,
            finalMaxAzRate = finalMaxAzRate,
            finalMaxElRate = finalMaxElRate,
            // KeyholeAxisTransformed
            keyholeAxisMaxAzRate = keyholeAxisMaxAzRate,
            keyholeAxisMaxElRate = keyholeAxisMaxElRate,
            // KeyholeFinalTransformed
            keyholeFinalStartAzimuth = keyholeFinalStartAzimuth,
            keyholeFinalEndAzimuth = keyholeFinalEndAzimuth,
            keyholeFinalStartElevation = keyholeFinalStartElevation,
            keyholeFinalEndElevation = keyholeFinalEndElevation,
            keyholeFinalMaxElevation = keyholeFinalMaxElevation,
            keyholeFinalMaxAzRate = keyholeFinalMaxAzRate,
            keyholeFinalMaxElRate = keyholeFinalMaxElRate,
            // KeyholeOptimizedFinalTransformed
            keyholeOptStartAzimuth = keyholeOptStartAzimuth,
            keyholeOptEndAzimuth = keyholeOptEndAzimuth,
            keyholeOptStartElevation = keyholeOptStartElevation,
            keyholeOptEndElevation = keyholeOptEndElevation,
            keyholeOptMaxElevation = keyholeOptMaxElevation,
            keyholeOptMaxAzRate = keyholeOptMaxAzRate,
            keyholeOptMaxElRate = keyholeOptMaxElRate,
            // P6: 등록 건 그룹핑용 통일 시간
            createdAt = registrationTime
        )
    }

    /**
     * Duration 값을 초 단위로 파싱합니다.
     * ISO 8601 Duration 문자열 또는 숫자를 지원합니다.
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
                    java.time.Duration.parse(durationValue).seconds.toInt()
                } catch (e: Exception) {
                    java.time.Duration.between(startTime, endTime).seconds.toInt()
                }
            }
            else -> {
                java.time.Duration.between(startTime, endTime).seconds.toInt()
            }
        }
    }

    /**
     * DTL Map을 TrackingTrajectoryEntity로 변환합니다.
     *
     * @param registrationTime P6: 등록 건 그룹핑용 통일 시간
     */
    private fun mapDtlToTrajectory(
        sessionId: Long,
        dtl: Map<String, Any?>,
        registrationTime: OffsetDateTime? = null
    ): TrackingTrajectoryEntity {
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
            elevationRate = elevationRate,
            // P6: 등록 건 그룹핑용 통일 시간
            createdAt = registrationTime
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
