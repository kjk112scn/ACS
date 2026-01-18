package com.gtlsystems.acs_api.tracking.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 하드웨어 에러 로그 엔티티 (Hypertable)
 * - ICD 통신 에러, 서보 에러, 비상 정지 등 로깅
 * - 복합 PK (timestamp, id) → @Id 없음
 * - 압축: 30일 후, 삭제: 365일 후
 */
@Table("hardware_error_log")
data class HardwareErrorLogEntity(
    @Column("timestamp")
    val timestamp: OffsetDateTime,

    // 에러 식별
    @Column("error_code")
    val errorCode: String,  // PROTOCOL_AZIMUTH_ERROR, SERVO_TRAIN_ALARM 등

    @Column("error_type")
    val errorType: String,  // 'PROTOCOL' | 'SERVO' | 'EMERGENCY' | 'INTERLOCK' | 'SYSTEM'

    @Column("error_message")
    val errorMessage: String? = null,

    // 소스 정보
    @Column("source")
    val source: String,  // 'ACU' | 'AZIMUTH' | 'ELEVATION' | 'TRAIN' | 'FEED'

    @Column("axis")
    val axis: String? = null,  // 'AZIMUTH' | 'ELEVATION' | 'TRAIN' | null

    // 심각도
    @Column("severity")
    val severity: String = SEVERITY_WARNING,  // 'CRITICAL' | 'ERROR' | 'WARNING' | 'INFO'

    // 컨텍스트
    @Column("tracking_mode")
    val trackingMode: String? = null,  // 현재 추적 모드

    @Column("session_id")
    val sessionId: Long? = null,  // FK to tracking_session

    // 에러 상세 (JSONB)
    @Column("raw_data")
    val rawData: String? = null,  // JSON 문자열로 저장

    // 연속 에러 그룹화
    @Column("correlation_id")
    val correlationId: UUID? = null,  // 동일 원인 에러 묶음

    @Column("occurrence_count")
    val occurrenceCount: Int = 1,

    // 해결 상태
    @Column("resolved")
    val resolved: Boolean = false,

    @Column("resolved_at")
    val resolvedAt: OffsetDateTime? = null,

    @Column("resolved_by")
    val resolvedBy: String? = null,

    @Column("resolution_note")
    val resolutionNote: String? = null
) {
    companion object {
        // Error Types
        const val TYPE_PROTOCOL = "PROTOCOL"
        const val TYPE_SERVO = "SERVO"
        const val TYPE_EMERGENCY = "EMERGENCY"
        const val TYPE_INTERLOCK = "INTERLOCK"
        const val TYPE_SYSTEM = "SYSTEM"

        // Severities
        const val SEVERITY_CRITICAL = "CRITICAL"
        const val SEVERITY_ERROR = "ERROR"
        const val SEVERITY_WARNING = "WARNING"
        const val SEVERITY_INFO = "INFO"

        // Sources
        const val SOURCE_ACU = "ACU"
        const val SOURCE_AZIMUTH = "AZIMUTH"
        const val SOURCE_ELEVATION = "ELEVATION"
        const val SOURCE_TRAIN = "TRAIN"
        const val SOURCE_FEED = "FEED"

        // Axes
        const val AXIS_AZIMUTH = "AZIMUTH"
        const val AXIS_ELEVATION = "ELEVATION"
        const val AXIS_TRAIN = "TRAIN"
    }
}
