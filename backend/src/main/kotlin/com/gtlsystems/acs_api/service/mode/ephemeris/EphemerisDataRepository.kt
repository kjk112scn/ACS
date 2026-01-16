package com.gtlsystems.acs_api.service.mode.ephemeris

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * Ephemeris 추적 데이터 저장소
 *
 * 위성 추적 마스터(MST) 및 세부(DTL) 데이터를 관리합니다.
 * 모든 데이터 접근에 로그를 기록하여 검증 가능성을 보장합니다.
 *
 * @since Phase 5 - BE 서비스 분리
 */
@Component
class EphemerisDataRepository {

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
}
