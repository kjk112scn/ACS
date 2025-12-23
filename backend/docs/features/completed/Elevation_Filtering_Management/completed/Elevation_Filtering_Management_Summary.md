# Elevation 필터링 관리 통합 개선 요약

## 개요

**목표**: Keyhole 여부에 따라 적절한 DataType을 자동 선택하고, displayMinElevationAngle 기준으로 백엔드에서 조건부 필터링하여 실제 추적 명령과 프론트엔드 표시 데이터를 일치시키며, 필터링을 동적으로 제어할 수 있도록 개선

**핵심 문제**:
1. Keyhole 미대응: `getEphemerisTrackDtlByMstId()`가 항상 `final_transformed`만 반환
2. 필터링 위치 불일치: 백엔드 추적은 -20도부터, 프론트엔드 표시는 0도부터 → 데이터 불일치
3. 이론치와 실제 추적 데이터 분리: Keyhole 여부 확인 로직이 실제 추적 로직에 없음
4. 하드코딩된 필터링: `displayMinElevationAngle` 필터링이 여러 위치에서 하드코딩되어 있음
5. 필터링 제어 불가: 특정 상황에서 전체 데이터가 필요한 경우 필터링을 비활성화할 수 없음

**해결 방안**: 
- Keyhole 여부에 따라 적절한 DataType 자동 선택 (`keyhole_final_transformed` vs `final_transformed`)
- `displayMinElevationAngle` 기준으로 백엔드에서 조건부 필터링 (`enableDisplayMinElevationFiltering` 설정에 따라)
- 예외 처리 추가 (MST 없음, 데이터 없음, 필터링 결과 없음)
- 필터링 제어 설정 추가 (`enableDisplayMinElevationFiltering`)
- 필터링 비활성화 시에도 하드웨어 제한 각도(`elevationMin`) 유지

---

## 주요 변경사항

### 1. Keyhole 대응
- Keyhole 여부에 따라 적절한 DataType 자동 선택
- keyhole_final_transformed 데이터 추가 (Keyhole 발생 시)
- 폴백 로직 (Keyhole 발생 시 keyhole_final_transformed 데이터 없으면 final_transformed로 폴백)

### 2. 필터링 위치 일치
- displayMinElevationAngle 기준으로 백엔드에서 조건부 필터링
- 백엔드 추적 = 프론트엔드 표시 (동일한 필터링 기준)
- 필터링 활성화/비활성화 동적 제어

### 3. 필터링 제어 설정 추가
- `enableDisplayMinElevationFiltering` 설정 추가
- 모든 필터링 위치에 조건부 필터링 적용
- 필터링 활성화 시: `displayMinElevationAngle` 기준
- 필터링 비활성화 시: `elevationMin` (하드웨어 제한 각도) 기준

### 4. 하드웨어 제한 각도 유지
- 필터링 비활성화 시에도 `elevationMin` 이상 데이터만 사용
- 음수 Elevation 데이터는 실제 추적 명령에 포함되지 않음

### 5. 예외 처리
- MST 없음, 데이터 없음, 필터링 결과 없음 처리
- 필터링 후 데이터가 없으면 추적 시작 중단

---

## 구현 결과

### ✅ Phase 0-1: 준비 및 설정 설명 업데이트
- 현재 코드 상태 확인
- `sourceMinElevationAngle` 설정 설명 업데이트

### ✅ Phase 2: getEphemerisTrackDtlByMstId() 수정 (핵심 함수)
- Keyhole 여부 확인 및 DataType 선택
- displayMinElevationAngle 기준으로 조건부 필터링
- 예외 처리 및 폴백 로직 포함

### ✅ Phase 3: createRealtimeTrackingData() 수정
- Keyhole 대응 및 조건부 필터링
- keyhole_final_transformed 데이터 추가

### ✅ Phase 4: sendHeaderTrackingData() 수정
- 필터링 후 빈 데이터 체크 추가
- 데이터 길이 검증 로직 개선

### ✅ Phase 5: sendInitialTrackingData() 수정
- 필터링된 데이터에서 시간 기준으로 가장 가까운 데이터 찾기
- 필터링된 데이터 기준으로 인덱스 계산

### ✅ Phase 6: exportMstDataToCsv() 수정
- displayMinElevationAngle 기준으로 조건부 필터링
- Keyhole 여부에 따라 DataType 선택
- keyhole_final_transformed 데이터 추가

### ✅ Phase 7: SettingsService에 필터링 활성화/비활성화 설정 추가
- `enableDisplayMinElevationFiltering` 설정 추가
- 기본값: `false` (백엔드), `true` (프론트엔드 기본값)

### ✅ Phase 8: getAllEphemerisTrackMstMerged() 수정
- 스케줄 목록 필터링 시 조건부 필터링 적용

### ✅ Phase 9: 프론트엔드 설정 조회 함수 추가
- `getEnableDisplayMinElevationFiltering()` 함수 추가

### ✅ Phase 10: 프론트엔드 스토어 수정
- `enableDisplayMinElevationFiltering` 상태 추가
- `filteredDetailData` computed에 조건부 필터링 로직 추가
- `updateEnableDisplayMinElevationFiltering()` 함수 추가

### ✅ Phase 11: 프론트엔드 설정 UI 추가 (선택사항)
- 필터링 활성화/비활성화 토글 추가

### ✅ Phase 12: 프론트엔드 CSV 다운로드 함수 개선
- 백엔드 API 응답 구조 변경으로 인해 자동으로 keyhole_final_transformed 데이터 사용 가능

---

## 최종 결과

### ✅ 핵심 문제 해결
- **Keyhole 대응**: Keyhole 여부에 따라 적절한 DataType 자동 선택
- **필터링 위치 일치**: displayMinElevationAngle 기준으로 백엔드에서 조건부 필터링
- **필터링 제어 가능**: `enableDisplayMinElevationFiltering` 설정으로 동적 제어
- **하드웨어 제한 각도 유지**: 필터링 비활성화 시에도 `elevationMin` 이상 데이터만 사용
- **예외 처리**: MST 없음, 데이터 없음, 필터링 결과 없음 처리

### ✅ 코드 품질 향상
- 조건부 필터링 로직 일관성 유지
- 로그에 필터링 상태 및 기준 각도 명확히 표시
- 예외 처리 및 폴백 로직 포함
- KDOC 주석 포함

---

## 관련 파일

### 백엔드
- `SettingsService.kt`: `enableDisplayMinElevationFiltering` 설정 추가, `sourceMinElevationAngle` 설정 설명 업데이트
- `EphemerisService.kt`: 모든 필터링 위치에 Keyhole 대응 및 조건부 필터링 적용
  - `getEphemerisTrackDtlByMstId()`: Keyhole 여부 확인 및 DataType 선택
  - `createRealtimeTrackingData()`: Keyhole 대응 및 조건부 필터링
  - `sendHeaderTrackingData()`: 데이터 길이 검증 개선
  - `sendInitialTrackingData()`: 필터링된 데이터 인덱스 처리
  - `exportMstDataToCsv()`: 필터링 및 Keyhole 대응
  - `getAllEphemerisTrackMstMerged()`: 스케줄 목록 필터링
- `PassScheduleService.kt`: 스케줄 목록 필터링에 조건부 필터링 적용

### 프론트엔드
- `ephemerisTrackService.ts`: 설정 조회 함수 추가
- `ephemerisTrackStore.ts`: 필터링 활성화/비활성화 로직 추가
- `TrackingSettings.vue`: 필터링 활성화/비활성화 토글 추가

---

## 결론

모든 계획 사항이 성공적으로 적용되었으며, 핵심 문제가 해결되었습니다. Keyhole 여부에 따라 적절한 DataType을 자동 선택하고, displayMinElevationAngle 기준으로 백엔드에서 조건부 필터링하여 실제 추적 명령과 프론트엔드 표시 데이터가 일치하며, 필터링을 동적으로 제어할 수 있습니다.

**주요 성과**:
1. Keyhole 대응 완료
2. 필터링 위치 일치 완료
3. 필터링 제어 설정 추가 완료
4. 하드웨어 제한 각도 유지 완료
5. 예외 처리 및 폴백 로직 포함 완료

