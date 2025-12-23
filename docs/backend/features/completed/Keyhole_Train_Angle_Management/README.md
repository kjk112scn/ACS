# Keyhole Train Angle Management

> Keyhole Train 각도 관리 통합 개선

---

## 📋 개요

Keyhole 위성 추적 시 Train 각도를 올바르게 관리하기 위해 데이터 변환 단계와 추적 단계의 문제를 해결

**핵심 문제**:
1. 데이터 변환 단계: KEYHOLE=YES인데 Train=0.000000°인 문제
2. 추적 단계: 위성 추적 시 Train 각도 설정 문제

**해결 방안**:
- Train의 두 가지 용도를 명확히 구분
- 각 MST는 독립적으로 본인 기준에서 Keyhole 판단 및 RecommendedTrainAngle 계산
- `getTrackingPassMst()` 헬퍼 함수 생성 및 Train 각도 동적 설정

## 📁 문서

- **[Keyhole_Train_Angle_Management_plan.md](./Keyhole_Train_Angle_Management_plan.md)**: 원본 계획 문서
- **[Final_Result.md](./completed/Final_Result.md)**: 최종 구현 결과
- **[Summary.md](./completed/Summary.md)**: 요약 문서

## ✅ 상태

- **완료일**: 2024-12
- **버전**: 1.0
- **통합 여부**: ✅ (2개 문서 통합)

---

**관련 파일**: 
- `src/main/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/processor/SatelliteTrackingProcessor.kt`
- `src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
- `src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`

