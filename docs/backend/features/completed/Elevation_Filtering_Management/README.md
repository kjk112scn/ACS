# Elevation Filtering Management

> Elevation 필터링 관리 통합 개선

---

## 📋 개요

Keyhole 여부에 따라 적절한 DataType을 자동 선택하고, displayMinElevationAngle 기준으로 백엔드에서 조건부 필터링하여 실제 추적 명령과 프론트엔드 표시 데이터를 일치시키며, 필터링을 동적으로 제어할 수 있도록 개선

**핵심 문제**:
1. Keyhole 미대응: `getEphemerisTrackDtlByMstId()`가 항상 `final_transformed`만 반환
2. 필터링 위치 불일치: 백엔드 추적은 -20도부터, 프론트엔드 표시는 0도부터 → 데이터 불일치
3. 하드코딩된 필터링: `displayMinElevationAngle` 필터링이 여러 위치에서 하드코딩
4. 필터링 제어 불가: 특정 상황에서 전체 데이터가 필요한 경우 필터링을 비활성화할 수 없음

**해결 방안**:
- Keyhole 여부에 따라 적절한 DataType 자동 선택
- `displayMinElevationAngle` 기준으로 백엔드에서 조건부 필터링
- 필터링 제어 설정 추가 (`enableDisplayMinElevationFiltering`)
- 필터링 비활성화 시에도 하드웨어 제한 각도 유지

## 📁 문서

- **[Elevation_Filtering_Management_plan.md](./Elevation_Filtering_Management_plan.md)**: 원본 계획 문서
- **[Final_Result.md](./completed/Final_Result.md)**: 최종 구현 결과
- **[Summary.md](./completed/Summary.md)**: 요약 문서

## ✅ 상태

- **완료일**: 2024-12
- **버전**: 1.0
- **통합 여부**: ✅ (3개 문서 통합)

---

**관련 파일**: 
- `src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
- `src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- `src/main/kotlin/com/gtlsystems/acs_api/service/system/settings/SettingsService.kt`
- `src/stores/mode/ephemerisTrackStore.ts`

