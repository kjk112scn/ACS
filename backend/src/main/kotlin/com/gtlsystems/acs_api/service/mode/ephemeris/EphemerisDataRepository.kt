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
 * V006 Fix: 서버 재시작 시 DB에서 메모리로 로딩 추가
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
     * ✅ V006 Fix: 서버 시작 시 DB에서 기존 세션 + 궤적을 메모리로 로드
     * TLE 등록 후 서버 재시작해도 스케줄 목록이 유지됨
     * ✅ P6 Fix: DTL(trajectory)도 함께 로드
     */
    @PostConstruct
    fun initFromDatabase() {
        if (sessionRepository == null) {
            logger.warn("SessionRepository가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        sessionRepository.findByTrackingMode("EPHEMERIS")
            .collectList()
            .doOnSuccess { sessions: List<TrackingSessionEntity> ->
                if (sessions.isEmpty()) {
                    logger.info("📥 [DB→메모리] Ephemeris 세션 없음")
                    return@doOnSuccess
                }

                logger.info("📥 [DB→메모리] ${sessions.size}개 Ephemeris 세션 로딩 시작")

                // 세션을 MST 형식으로 변환하여 메모리에 저장
                val mstData = mutableListOf<Map<String, Any?>>()

                sessions.forEach { session ->
                    // ✅ 'original' 타입의 MST 생성
                    val originalMst = mapSessionToMst(session, "original")
                    mstData.add(originalMst)

                    // ✅ 'final_transformed' 타입의 MST 생성 (getAllEphemerisTrackMstMerged에서 필요)
                    val finalMst = mapSessionToMst(session, "final_transformed")
                    mstData.add(finalMst)

                    // ✅ P6 Fix: 해당 세션의 DTL(trajectory)도 로드
                    if (trajectoryRepository != null && session.id != null) {
                        loadTrajectoryForSession(session)
                    }
                }

                synchronized(mstStorage) {
                    mstStorage.addAll(mstData)
                }

                logger.info("📥 [DB→메모리] ${sessions.size}개 세션 → ${mstData.size}개 MST 로드 완료")
            }
            .doOnError { e ->
                logger.error("❌ [DB→메모리] Ephemeris 세션 로딩 실패: ${e.message}")
            }
            .subscribe()
    }

    /**
     * ✅ P6 Fix: 세션별 trajectory를 DB에서 로드하여 dtlStorage에 추가
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
                    mapTrajectoryToDtl(session, traj)
                }

                synchronized(dtlStorage) {
                    dtlStorage.addAll(dtlData)
                }

                logger.debug("📥 [DB→메모리] 세션 ${session.id} → ${dtlData.size}개 DTL 로드")
            }
            .doOnError { e ->
                logger.error("❌ [DB→메모리] 세션 ${session.id} trajectory 로딩 실패: ${e.message}")
            }
            .subscribe()
    }

    /**
     * TrackingTrajectoryEntity → DTL Map 변환
     * ✅ P6-2 Fix: OffsetDateTime → ZonedDateTime 변환 (이전 RAM 형식과 동일하게)
     */
    private fun mapTrajectoryToDtl(session: TrackingSessionEntity, traj: TrackingTrajectoryEntity): Map<String, Any?> {
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
            "CreatedAt" to traj.createdAt  // ✅ P6: CreatedAt 유지
        )
    }

    /**
     * TrackingSessionEntity를 MST Map으로 변환
     * ✅ P6-2 Fix: OffsetDateTime → ZonedDateTime 변환 (이전 RAM 형식과 동일하게)
     */
    private fun mapSessionToMst(session: TrackingSessionEntity, dataType: String): Map<String, Any?> {
        // OffsetDateTime → ZonedDateTime (UTC) 변환 (이전 RAM 형식과 동일)
        val startTimeZoned = session.startTime.atZoneSameInstant(ZoneOffset.UTC)
        val endTimeZoned = session.endTime.atZoneSameInstant(ZoneOffset.UTC)

        return mutableMapOf<String, Any?>(
            "MstId" to session.mstId,
            "DetailId" to session.detailId,
            "DataType" to dataType,
            "SatelliteID" to session.satelliteId,
            "SatelliteName" to session.satelliteName,
            "StartTime" to startTimeZoned,
            "EndTime" to endTimeZoned,
            "Duration" to session.duration,
            "MaxElevation" to session.maxElevation,
            "MaxAzRate" to session.maxAzimuthRate,
            "MaxElRate" to session.maxElevationRate,
            "IsKeyhole" to session.keyholeDetected,
            "RecommendedTrainAngle" to session.recommendedTrainAngle,
            "TotalPoints" to session.totalPoints,
            // TLE 정보
            "TleCacheId" to session.tleCacheId,
            "TleLine1" to session.tleLine1,
            "TleLine2" to session.tleLine2,
            "TleEpoch" to session.tleEpoch,
            // DataType별 메타데이터
            "StartAzimuth" to when (dataType) {
                "original" -> session.originalStartAzimuth ?: session.startAzimuth
                "final_transformed" -> session.finalStartAzimuth ?: session.startAzimuth
                else -> session.startAzimuth
            },
            "EndAzimuth" to when (dataType) {
                "original" -> session.originalEndAzimuth ?: session.endAzimuth
                "final_transformed" -> session.finalEndAzimuth ?: session.endAzimuth
                else -> session.endAzimuth
            },
            "StartElevation" to when (dataType) {
                "final_transformed" -> session.finalStartElevation ?: session.startElevation
                else -> session.startElevation
            },
            "EndElevation" to when (dataType) {
                "final_transformed" -> session.finalEndElevation ?: session.endElevation
                else -> session.endElevation
            },
            // ✅ P6 Fix: DB에서 로드 시 CreatedAt 필드 추가 (필터링 정상화)
            "CreatedAt" to session.createdAt
        )
    }

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
        // ✅ P6: 등록 건 그룹핑을 위한 동일 timestamp 생성
        val registrationTime = OffsetDateTime.now(ZoneOffset.UTC)
        logger.info("📝 [WRITE #$opId] replaceAll 시작 - MST: ${mstData.size}개, DTL: ${dtlData.size}개, registrationTime: $registrationTime")

        // ✅ P6: 모든 데이터에 동일한 CreatedAt 추가 (그룹핑용)
        val mstWithCreatedAt = mstData.map { it + ("CreatedAt" to registrationTime) }
        val dtlWithCreatedAt = dtlData.map { it + ("CreatedAt" to registrationTime) }

        // ✅ V006 디버깅: final_transformed MST의 DetailId 검증
        val finalTransformedMst = mstWithCreatedAt.filter { it["DataType"] == "final_transformed" }
        logger.info("🔍 [WRITE #$opId] final_transformed MST 검증:")
        finalTransformedMst.forEach { mst ->
            val mstId = mst["MstId"]
            val detailId = mst["DetailId"]
            val detailIdType = detailId?.let { it::class.simpleName } ?: "null"
            logger.info("   - MstId=$mstId, DetailId=$detailId (타입: $detailIdType)")
        }

        // ✅ P6: clear() 제거 → 이력 보존 (누적 저장)
        synchronized(mstStorage) {
            val beforeSize = mstStorage.size
            mstStorage.addAll(mstWithCreatedAt)
            logger.info("📝 [WRITE #$opId] MST 추가 완료: $beforeSize → ${mstStorage.size} (이력 보존)")
        }

        synchronized(dtlStorage) {
            val beforeSize = dtlStorage.size
            dtlStorage.addAll(dtlWithCreatedAt)
            logger.info("📝 [WRITE #$opId] DTL 추가 완료: $beforeSize → ${dtlStorage.size} (이력 보존)")
        }

        logStorageSummary(opId)

        // DB 저장 (Write-through) - ✅ P6: registrationTime 전달
        saveToDatabase(mstWithCreatedAt, dtlWithCreatedAt, opId, registrationTime)
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
     * ✅ P6: 가장 최근 등록 건(CreatedAt)만 반환 (이력 보존 + 최신 조회)
     */
    fun getAllMst(): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(mstStorage) {
            // 가장 최근 CreatedAt 찾기
            val latestCreatedAt = mstStorage
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            if (latestCreatedAt == null) {
                mstStorage.toList()
            } else {
                mstStorage.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
            }
        }
        logger.debug("📖 [READ #$opId] getAllMst() → ${result.size}개 (최근 등록 건만)")
        return result
    }

    /**
     * 모든 세부 데이터를 반환합니다.
     * ✅ P6: 가장 최근 등록 건(CreatedAt)만 반환 (이력 보존 + 최신 조회)
     */
    fun getAllDtl(): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(dtlStorage) {
            // 가장 최근 CreatedAt 찾기
            val latestCreatedAt = dtlStorage
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            if (latestCreatedAt == null) {
                dtlStorage.toList()
            } else {
                dtlStorage.filter { (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt }
            }
        }
        logger.debug("📖 [READ #$opId] getAllDtl() → ${result.size}개 (최근 등록 건만)")
        return result
    }

    /**
     * 데이터 타입별 마스터 데이터를 반환합니다.
     * ✅ P6: 가장 최근 등록 건(CreatedAt)만 반환
     *
     * @param dataType 데이터 타입 (original, axis_transformed, final_transformed 등)
     */
    fun getMstByDataType(dataType: String): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(mstStorage) {
            // 가장 최근 CreatedAt 찾기
            val latestCreatedAt = mstStorage
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            val filtered = if (latestCreatedAt == null) {
                mstStorage.filter { it["DataType"] == dataType }
            } else {
                mstStorage.filter {
                    it["DataType"] == dataType && (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt
                }
            }
            filtered
        }
        logger.debug("📖 [READ #$opId] getMstByDataType($dataType) → ${result.size}개 (최근 등록 건만)")
        return result
    }

    /**
     * 데이터 타입별 세부 데이터를 반환합니다.
     * ✅ P6: 가장 최근 등록 건(CreatedAt)만 반환
     *
     * @param dataType 데이터 타입
     */
    fun getDtlByDataType(dataType: String): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(dtlStorage) {
            // 가장 최근 CreatedAt 찾기
            val latestCreatedAt = dtlStorage
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            val filtered = if (latestCreatedAt == null) {
                dtlStorage.filter { it["DataType"] == dataType }
            } else {
                dtlStorage.filter {
                    it["DataType"] == dataType && (it["CreatedAt"] as? OffsetDateTime) == latestCreatedAt
                }
            }
            filtered
        }
        logger.debug("📖 [READ #$opId] getDtlByDataType($dataType) → ${result.size}개 (최근 등록 건만)")
        return result
    }

    /**
     * MstId로 마스터 데이터를 검색합니다.
     * ✅ P6: 가장 최근 등록 건(CreatedAt)만 검색
     *
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입 (선택적)
     */
    fun findMstById(mstId: Long, dataType: String? = null): Map<String, Any?>? {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(mstStorage) {
            // ✅ P6: 가장 최근 CreatedAt 찾기
            val latestCreatedAt = mstStorage
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            mstStorage.find {
                val dataMstId = (it["MstId"] as? Number)?.toLong()
                val createdAt = it["CreatedAt"] as? OffsetDateTime
                val matchesCreatedAt = latestCreatedAt == null || createdAt == latestCreatedAt
                val matches = dataMstId == mstId && matchesCreatedAt
                if (dataType != null) {
                    matches && it["DataType"] == dataType
                } else {
                    matches
                }
            }
        }
        logger.debug("📖 [READ #$opId] findMstById($mstId, $dataType) → ${if (result != null) "found" else "null"} (최근 등록 건만)")
        return result
    }

    /**
     * MstId와 데이터 타입으로 세부 데이터를 검색합니다.
     * ✅ P6: 가장 최근 등록 건(CreatedAt)만 검색
     *
     * @param mstId 마스터 ID
     * @param dataType 데이터 타입
     * @param detailId 세부 ID (기본값: 0)
     */
    fun findDtlByMstIdAndDataType(mstId: Long, dataType: String, detailId: Int = 0): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(dtlStorage) {
            // ✅ P6: 가장 최근 CreatedAt 찾기
            val latestCreatedAt = dtlStorage
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            dtlStorage.filter {
                val dataMstId = (it["MstId"] as? Number)?.toLong()
                val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
                val itDataType = it["DataType"] as? String
                val createdAt = it["CreatedAt"] as? OffsetDateTime
                val matchesCreatedAt = latestCreatedAt == null || createdAt == latestCreatedAt
                dataMstId == mstId && dataDetailId == detailId && itDataType == dataType && matchesCreatedAt
            }
        }

        if (result.isEmpty()) {
            logger.warn("⚠️ [READ #$opId] findDtlByMstIdAndDataType($mstId, $dataType, $detailId) → 0개 (최근 등록 건에 없음)")
            // 디버깅용: 해당 MstId와 DataType으로 존재하는 DetailId 목록 (최근 등록 건에서)
            val availableDetailIds = synchronized(dtlStorage) {
                val latestCreatedAt = dtlStorage
                    .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                    .maxOrNull()
                dtlStorage.filter {
                    val dataMstId = (it["MstId"] as? Number)?.toLong()
                    val itDataType = it["DataType"] as? String
                    val createdAt = it["CreatedAt"] as? OffsetDateTime
                    val matchesCreatedAt = latestCreatedAt == null || createdAt == latestCreatedAt
                    dataMstId == mstId && itDataType == dataType && matchesCreatedAt
                }.mapNotNull { (it["DetailId"] as? Number)?.toInt() ?: 0 }.distinct()
            }
            if (availableDetailIds.isNotEmpty()) {
                logger.warn("⚠️ [READ #$opId] 사용 가능한 DetailId: $availableDetailIds")
            }
        } else {
            logger.debug("📖 [READ #$opId] findDtlByMstIdAndDataType($mstId, $dataType, $detailId) → ${result.size}개 (최근 등록 건만)")
        }

        return result
    }

    /**
     * MstId로 모든 데이터 타입의 세부 데이터를 검색합니다.
     * ✅ P6: 가장 최근 등록 건(CreatedAt)만 검색
     *
     * @param mstId 마스터 ID
     * @param detailId 세부 ID (기본값: 0)
     */
    fun findAllDtlByMstId(mstId: Long, detailId: Int = 0): List<Map<String, Any?>> {
        val opId = readCounter.incrementAndGet()
        val result = synchronized(dtlStorage) {
            // ✅ P6: 가장 최근 CreatedAt 찾기
            val latestCreatedAt = dtlStorage
                .mapNotNull { it["CreatedAt"] as? OffsetDateTime }
                .maxOrNull()

            dtlStorage.filter {
                val dataMstId = (it["MstId"] as? Number)?.toLong()
                val dataDetailId = (it["DetailId"] as? Number)?.toInt() ?: 0
                val createdAt = it["CreatedAt"] as? OffsetDateTime
                val matchesCreatedAt = latestCreatedAt == null || createdAt == latestCreatedAt
                dataMstId == mstId && dataDetailId == detailId && matchesCreatedAt
            }
        }
        logger.debug("📖 [READ #$opId] findAllDtlByMstId($mstId, $detailId) → ${result.size}개 (최근 등록 건만)")
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
     * V006 P0 Fix: mstId와 detailId로 tracking_session의 id를 조회합니다.
     *
     * @param mstId 마스터 ID
     * @param detailId 패스 구분자
     * @param trackingMode 추적 모드 (기본값: ephemeris_designation)
     * @return 세션 ID (없으면 null)
     */
    fun getSessionIdByMstAndDetail(
        mstId: Long,
        detailId: Int,
        trackingMode: String = "EPHEMERIS"  // ✅ P0 Fix: DB 저장값과 일치 (mapMstToSession 참조)
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
     *
     * V006: 1 Pass = 1 Session 정책
     * - (mst_id, detail_id, tracking_mode)가 UNIQUE 키
     * - data_type별로 7개 세션 생성하지 않음
     * - 같은 (mstId, detailId) 그룹에서 대표 세션 1개만 저장
     *
     * P6: registrationTime으로 등록 건 그룹핑
     * - 한 번의 등록 작업에서 모든 row가 동일한 created_at을 가짐
     */
    private fun saveToDatabase(
        mstData: List<Map<String, Any?>>,
        dtlData: List<Map<String, Any?>>,
        opId: Long,
        registrationTime: OffsetDateTime
    ) {
        if (sessionRepository == null || trajectoryRepository == null) {
            logger.warn("DB Repository가 없습니다. 메모리 전용 모드로 동작합니다.")
            return
        }

        // V006: (mstId, detailId) 기준으로 그룹화하여 1 Pass = 1 Session 보장
        val groupedMst = mstData.groupBy { mst ->
            val mstId = (mst["MstId"] as? Number)?.toLong() ?: 0L
            val detailId = (mst["DetailId"] as? Number)?.toInt() ?: 0
            Pair(mstId, detailId)
        }

        logger.info("📝 [DB #$opId] MST ${mstData.size}개 → ${groupedMst.size}개 세션으로 그룹화")

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

                // 세션 저장 (중복 체크 후 UPSERT) - ✅ P6: registrationTime 전달
                val session = mapMstToSession(representativeMst, allDtlForSession.size, registrationTime)
                saveOrUpdateSession(session, allDtlForSession, opId, registrationTime)
            } catch (e: RuntimeException) {
                logger.error("❌ [DB #$opId] MST($mstId, $detailId) 저장 실패: ${e.message}")
            }
        }

        logger.info("📝 [DB #$opId] Ephemeris 스케줄 DB 저장 요청 완료 (${groupedMst.size}개 세션)")
    }

    /**
     * V006: 세션 UPSERT (존재하면 스킵, 없으면 INSERT)
     * P6: registrationTime 추가
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
                    // Trajectory 저장 - ✅ P6: registrationTime 전달
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
     * P6: registrationTime 추가
     */
    private fun saveTrajectories(
        sessionId: Long,
        dtlData: List<Map<String, Any?>>,
        opId: Long,
        registrationTime: OffsetDateTime
    ) {
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
     *
     * ✅ 키 이름 매핑 (SatelliteTrackingProcessor와 일치):
     * - SatelliteID (대문자 ID) → satellite_id
     * - MaxAzRate → max_azimuth_rate
     * - MaxElRate → max_elevation_rate
     * - IsKeyhole → keyhole_detected
     * - Duration (ISO String) → duration (초)
     *
     * V006 추가:
     * - TLE 연동 (tleCacheId, tleLine1, tleLine2, tleEpoch)
     * - data_type은 호환성 유지 (nullable)
     *
     * @param dtlCount DTL 데이터 개수 (total_points 계산용)
     */
    private fun mapMstToSession(
        mst: Map<String, Any?>,
        dtlCount: Int = 0,
        registrationTime: OffsetDateTime? = null  // P6: 등록 건 그룹핑용 통일 시간
    ): TrackingSessionEntity {
        val mstId = (mst["MstId"] as? Number)?.toLong() ?: 0L
        val detailId = (mst["DetailId"] as? Number)?.toInt() ?: 0
        // ✅ SatelliteID (대문자) 우선, 없으면 SatelliteId 시도
        val satelliteId = mst["SatelliteID"] as? String
            ?: mst["SatelliteId"] as? String ?: ""
        val satelliteName = mst["SatelliteName"] as? String
        // V006: data_type은 호환성 유지 (nullable)
        val dataType = mst["DataType"] as? String

        // ===== 시간 파싱 =====
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val startTime = parseTime(mst["StartTime"]) ?: now
        val endTime = parseTime(mst["EndTime"]) ?: now
        val duration = parseDurationToSeconds(mst["Duration"], startTime, endTime)
        val maxElevationTime = parseTime(mst["MaxElevationTime"])

        // ===== 기본 각도 정보 (P5 수정: 누락 필드 추가) =====
        val startAzimuth = (mst["StartAzimuth"] as? Number)?.toDouble()
            ?: (mst["StartAzimuthAngle"] as? Number)?.toDouble()
        val endAzimuth = (mst["EndAzimuth"] as? Number)?.toDouble()
            ?: (mst["EndAzimuthAngle"] as? Number)?.toDouble()
        val startElevation = (mst["StartElevation"] as? Number)?.toDouble()
            ?: (mst["StartElevationAngle"] as? Number)?.toDouble()
        val endElevation = (mst["EndElevation"] as? Number)?.toDouble()
            ?: (mst["EndElevationAngle"] as? Number)?.toDouble()
        val trainAngle = (mst["Train"] as? Number)?.toDouble()
            ?: (mst["TrainAngle"] as? Number)?.toDouble()

        // ===== 기본 Peak 값 =====
        val maxElevation = (mst["MaxElevation"] as? Number)?.toDouble()
        val maxAzimuthRate = (mst["MaxAzRate"] as? Number)?.toDouble()
            ?: (mst["MaxAzimuthRate"] as? Number)?.toDouble()
        val maxElevationRate = (mst["MaxElRate"] as? Number)?.toDouble()
            ?: (mst["MaxElevationRate"] as? Number)?.toDouble()
        val maxAzimuthAccel = (mst["MaxAzAccel"] as? Number)?.toDouble()
            ?: (mst["MaxAzimuthAccel"] as? Number)?.toDouble()
        val maxElevationAccel = (mst["MaxElAccel"] as? Number)?.toDouble()
            ?: (mst["MaxElevationAccel"] as? Number)?.toDouble()

        val keyholeDetected = mst["IsKeyhole"] as? Boolean
            ?: mst["KeyholeDetected"] as? Boolean ?: false
        val recommendedTrainAngle = (mst["RecommendedTrainAngle"] as? Number)?.toDouble()
        val totalPoints = (mst["TotalPoints"] as? Number)?.toInt()
            ?: if (dtlCount > 0) dtlCount else null

        // ===== Original (2축) 메타데이터 =====
        val originalStartAzimuth = (mst["OriginalStartAzimuth"] as? Number)?.toDouble()
        val originalEndAzimuth = (mst["OriginalEndAzimuth"] as? Number)?.toDouble()
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

        // V006: TLE 연동 (FK + 스냅샷)
        val tleCacheId = (mst["TleCacheId"] as? Number)?.toLong()
            ?: (mst["tleCacheId"] as? Number)?.toLong()
        val tleLine1 = mst["TleLine1"] as? String
            ?: mst["tleLine1"] as? String
            ?: mst["tle_line_1"] as? String
        val tleLine2 = mst["TleLine2"] as? String
            ?: mst["tleLine2"] as? String
            ?: mst["tle_line_2"] as? String
        val tleEpoch = parseTime(mst["TleEpoch"])
            ?: parseTime(mst["tleEpoch"])
            ?: parseTime(mst["tle_epoch"])

        return TrackingSessionEntity(
            mstId = mstId,
            detailId = detailId,
            satelliteId = satelliteId,
            satelliteName = satelliteName,
            trackingMode = "EPHEMERIS",
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
            // V006: TLE 연동
            tleCacheId = tleCacheId,
            tleLine1 = tleLine1,
            tleLine2 = tleLine2,
            tleEpoch = tleEpoch,
            // P6: 등록 건 그룹핑용 통일 시간
            createdAt = registrationTime
        )
    }

    /**
     * DTL Map을 TrackingTrajectoryEntity로 변환합니다.
     *
     * @param registrationTime P6: 등록 건 그룹핑용 통일 시간
     */
    private fun mapDtlToTrajectory(
        sessionId: Long,
        dtl: Map<String, Any?>,
        registrationTime: OffsetDateTime? = null  // P6: 등록 건 그룹핑용 통일 시간
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
