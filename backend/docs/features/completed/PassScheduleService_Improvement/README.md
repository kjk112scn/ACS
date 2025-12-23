# PassScheduleService Improvement

> PassScheduleService 개선 및 PushDataService 통합

---

## 📋 개요

PassScheduleService에 SatelliteTrackingProcessor를 통합하여 EphemerisService와 동일한 수준의 데이터 처리(2축, 3축, Keyhole 계산)를 지원하고, 상태머신에 Train 각도 설정 로직을 추가하여 Keyhole 위성을 올바르게 추적할 수 있도록 개선

**주요 개선 사항**:
- SatelliteTrackingProcessor 통합
- 5가지 DataType 저장 및 관리
- Keyhole 계산 및 Train 각도 자동 계산
- 상태머신에 Train 각도 설정 로직 추가
- PushDataService 통합

## 📁 문서

- **[PassScheduleService_Improvement_plan.md](./PassScheduleService_Improvement_plan.md)**: 원본 계획 문서
- **[Final_Result.md](./completed/Final_Result.md)**: 최종 구현 결과
- **[Summary.md](./completed/Summary.md)**: 요약 문서

## ✅ 상태

- **완료일**: 2024-12
- **버전**: 1.0
- **통합 여부**: ✅ (2개 문서 통합)

---

**관련 파일**: 
- `src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`

