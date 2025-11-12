# Train Algorithm

> Train 각도 최적화 알고리즘

---

## 📋 개요

위성 추적 시 Keyhole 영역(Azimuth ±270° 근처) 회피를 위한 Train 각도 최적화 알고리즘 구현

**주요 기능**:
- 2축 원본 데이터 → 3축 변환 (Train 각도 적용)
- Keyhole 판단 및 Train 각도 계산
- 각도 제한 (±270° 범위) 적용
- 6가지 DataType별 데이터 관리
- 동적 CSV 출력 (Keyhole 여부에 따라 헤더/데이터 변경)
- 프론트엔드 API 연동

## 📁 문서

- **[Train_Algorithm_plan.md](./Train_Algorithm_plan.md)**: 원본 계획 문서
- **[Final_Result.md](./completed/Final_Result.md)**: 최종 구현 결과
- **[Summary.md](./completed/Summary.md)**: 요약 문서

## ✅ 상태

- **완료일**: 2024-12
- **버전**: 1.0

---

**관련 파일**: 
- `src/main/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/processor/SatelliteTrackingProcessor.kt`
- `src/main/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/impl/OrekitCalculator.kt`

