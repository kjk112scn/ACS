package com.gtlsystems.acs_api.algorithm.satellitetracker.processor.model

/**
 * 처리된 위성 추적 데이터
 * 
 * ✅ 계획 Phase 2.2: 모든 DataType의 Mst/Dtl 데이터를 담는 컨테이너
 * 
 * @property originalMst 원본 2축 데이터 (Mst)
 * @property originalDtl 원본 2축 데이터 (Dtl)
 * @property axisTransformedMst 3축 변환 데이터 (Mst)
 * @property axisTransformedDtl 3축 변환 데이터 (Dtl)
 * @property finalTransformedMst 최종 각도 제한 변환 데이터 (Mst)
 * @property finalTransformedDtl 최종 각도 제한 변환 데이터 (Dtl)
 */
data class ProcessedTrackingData(
    val originalMst: List<Map<String, Any?>>,
    val originalDtl: List<Map<String, Any?>>,
    val axisTransformedMst: List<Map<String, Any?>>,
    val axisTransformedDtl: List<Map<String, Any?>>,
    val finalTransformedMst: List<Map<String, Any?>>,
    val finalTransformedDtl: List<Map<String, Any?>>
)

/**
 * 단일 패스 처리 결과 (내부 사용)
 * 
 * @property transformedData 변환된 상세 데이터
 * @property metrics 계산된 메타데이터 (MaxElevation, MaxAzRate 등)
 */
internal data class SinglePassResult(
    val transformedData: List<Map<String, Any?>>,
    val metrics: Map<String, Any?>
)

