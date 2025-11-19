# DisplayMinElevationAngle 설정 제거

> DisplayMinElevationAngle 및 EnableDisplayMinElevationFiltering 설정 제거 및 필터링 로직 정리

---

## 📋 개요

3축 변환 후 필터링 기준으로 사용되던 `displayMinElevationAngle` 설정을 제거하고, 순수 2축 `sourceMinElevationAngle` 기준으로만 판단하도록 변경. 관련 필터링 활성화/비활성화 설정(`enableDisplayMinElevationFiltering`)도 함께 제거.

**핵심 문제**:
1. 계산 오류: `displayMinElevationAngle` 기준 필터링으로 인해 계산이 잘못되고 있음
2. 필터링 기준 혼재: 2축 `sourceMinElevationAngle`과 3축 변환 후 `displayMinElevationAngle` 기준이 혼재되어 있음
3. 불필요한 복잡성: 화면 표시용 필터링이 백엔드 로직에 포함되어 있어 복잡도 증가

**해결 방안**:
- `displayMinElevationAngle` 설정 완전 제거
- `enableDisplayMinElevationFiltering` 설정 완전 제거
- 모든 필터링을 `sourceMinElevationAngle` 또는 `angleElevationMin` (하드웨어 제한) 기준으로만 처리
- 백엔드와 프론트엔드에서 관련 로직 일괄 제거

## 📁 문서

- **[Remove_DisplayMinElevationAngle_plan.md](./Remove_DisplayMinElevationAngle_plan.md)**: 원본 계획 문서
- **[Summary.md](./completed/DisplayMinElevationAngle_Removal_Summary.md)**: 요약 문서
- **[Final_Result.md](./completed/DisplayMinElevationAngle_Removal_Final_Result.md)**: 최종 구현 결과

## ✅ 상태

- **완료일**: 2024-12
- **버전**: 1.0
- **통합 여부**: ✅ (3개 문서 통합)

---

**관련 파일**: 
- `src/main/kotlin/com/gtlsystems/acs_api/service/system/settings/SettingsService.kt`
- `src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
- `src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- `src/services/mode/ephemerisTrackService.ts`
- `src/stores/mode/ephemerisTrackStore.ts`
- `src/types/ephemerisTrack.ts`

