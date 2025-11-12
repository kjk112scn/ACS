package com.gtlsystems.acs_api.algorithm.satellitetracker.processor.model

/**
 * 처리된 위성 추적 데이터
 * 
 * ✅ 8가지 DataType의 Mst/Dtl 데이터를 담는 컨테이너
 * 
 * @property originalMst 원본 2축 데이터 (Mst)
 * @property originalDtl 원본 2축 데이터 (Dtl)
 * @property axisTransformedMst 3축 변환 데이터 (Train=0, 각도 제한 ❌) (Mst)
 * @property axisTransformedDtl 3축 변환 데이터 (Train=0, 각도 제한 ❌) (Dtl)
 * @property finalTransformedMst 최종 각도 제한 변환 데이터 (Train=0, 각도 제한 ✅) (Mst)
 * @property finalTransformedDtl 최종 각도 제한 변환 데이터 (Train=0, 각도 제한 ✅) (Dtl)
 * @property keyholeAxisTransformedMst Keyhole 3축 변환 데이터 (Train≠0, 각도 제한 ❌, Keyhole 발생 시만) (Mst)
 * @property keyholeAxisTransformedDtl Keyhole 3축 변환 데이터 (Train≠0, 각도 제한 ❌, Keyhole 발생 시만) (Dtl)
 * @property keyholeFinalTransformedMst Keyhole 최종 변환 데이터 (Train≠0, 각도 제한 ✅, Keyhole 발생 시만) (Mst)
 * @property keyholeFinalTransformedDtl Keyhole 최종 변환 데이터 (Train≠0, 각도 제한 ✅, Keyhole 발생 시만) (Dtl)
 * @property keyholeOptimizedAxisTransformedMst Keyhole 최적화 3축 변환 데이터 (Train≠0 최적화, 각도 제한 ❌, Keyhole 발생 시만) (Mst)
 * @property keyholeOptimizedAxisTransformedDtl Keyhole 최적화 3축 변환 데이터 (Train≠0 최적화, 각도 제한 ❌, Keyhole 발생 시만) (Dtl)
 * @property keyholeOptimizedFinalTransformedMst Keyhole 최적화 최종 변환 데이터 (Train≠0 최적화, 각도 제한 ✅, Keyhole 발생 시만) (Mst)
 * @property keyholeOptimizedFinalTransformedDtl Keyhole 최적화 최종 변환 데이터 (Train≠0 최적화, 각도 제한 ✅, Keyhole 발생 시만) (Dtl)
 */
data class ProcessedTrackingData(
    val originalMst: List<Map<String, Any?>>,
    val originalDtl: List<Map<String, Any?>>,
    val axisTransformedMst: List<Map<String, Any?>>,
    val axisTransformedDtl: List<Map<String, Any?>>,
    val finalTransformedMst: List<Map<String, Any?>>,
    val finalTransformedDtl: List<Map<String, Any?>>,
    val keyholeAxisTransformedMst: List<Map<String, Any?>>,     // ✅ 추가: Train≠0, 각도 제한 ❌
    val keyholeAxisTransformedDtl: List<Map<String, Any?>>,     // ✅ 추가: Train≠0, 각도 제한 ❌
    val keyholeFinalTransformedMst: List<Map<String, Any?>>,
    val keyholeFinalTransformedDtl: List<Map<String, Any?>>,
    val keyholeOptimizedAxisTransformedMst: List<Map<String, Any?>> = emptyList(),     // ✅ 추가: Train≠0 최적화, 각도 제한 ❌
    val keyholeOptimizedAxisTransformedDtl: List<Map<String, Any?>> = emptyList(),     // ✅ 추가: Train≠0 최적화, 각도 제한 ❌
    val keyholeOptimizedFinalTransformedMst: List<Map<String, Any?>> = emptyList(),    // ✅ 추가: Train≠0 최적화, 각도 제한 ✅
    val keyholeOptimizedFinalTransformedDtl: List<Map<String, Any?>> = emptyList()     // ✅ 추가: Train≠0 최적화, 각도 제한 ✅
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

