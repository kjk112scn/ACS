# DisplayMinElevationAngle 설정 제거 요약

## 개요

**목표**: 3축 변환 후 필터링 기준으로 사용되던 `displayMinElevationAngle` 설정을 제거하고, 순수 2축 `sourceMinElevationAngle` 기준으로만 판단하도록 변경. 관련 필터링 활성화/비활성화 설정(`enableDisplayMinElevationFiltering`)도 함께 제거.

**핵심 문제**:
1. 계산 오류: `displayMinElevationAngle` 기준 필터링으로 인해 계산이 잘못되고 있음
2. 필터링 기준 혼재: 2축 `sourceMinElevationAngle`과 3축 변환 후 `displayMinElevationAngle` 기준이 혼재되어 있음
3. 불필요한 복잡성: 화면 표시용 필터링이 백엔드 로직에 포함되어 있어 복잡도 증가
4. MaxElevation 재계산 오류: 필터링된 데이터로 MaxElevation을 계산하여 스케줄 선택에 영향을 줌

**해결 방안**: 
- `displayMinElevationAngle` 설정 완전 제거
- `enableDisplayMinElevationFiltering` 설정 완전 제거
- 모든 필터링을 `sourceMinElevationAngle` 또는 `angleElevationMin` (하드웨어 제한) 기준으로만 처리
- 백엔드와 프론트엔드에서 관련 로직 일괄 제거
- MaxElevation 재계산 로직을 하드웨어 제한 각도 기준으로 수정

---

## 주요 변경사항

### 1. 백엔드 설정 제거
- `ephemeris.tracking.displayMinElevationAngle` 설정 정의 제거
- `ephemeris.tracking.enableDisplayMinElevationFiltering` 설정 정의 제거
- 관련 프로퍼티 제거

### 2. 백엔드 필터링 로직 정리
- **핵심 메서드**: `getEphemerisTrackDtlByMstId()` (74곳에서 호출)
  - `enableFiltering`, `displayMinElevation` 변수 제거
  - 필터링 로직을 `angleElevationMin` (하드웨어 제한) 기준으로만 처리
- **MaxElevation 재계산**: 필터링된 데이터 대신 하드웨어 제한 각도 기준 데이터로 재계산
- **기타 메서드**: 모든 필터링 위치에서 `displayMinElevationAngle` 관련 로직 제거
  - `getRealtimeTrackingData()`
  - `mergeEphemerisTrackMstData()`
  - `getTrackingDataForPass()`
  - `downloadTheoreticalDataAsCsv()`
  - `getAllPassScheduleTrackMst()`

### 3. 프론트엔드 설정 제거
- `getDisplayMinElevationAngle()` API 호출 메서드 제거
- `getEnableDisplayMinElevationFiltering()` API 호출 메서드 제거
- `displayMinElevation` 상태 제거
- `enableDisplayMinElevationFiltering` 상태 제거
- `filteredDetailData` computed 속성에서 필터링 로직 제거, `rawDetailData` 직접 반환
- `updateEnableDisplayMinElevationFiltering()` 메서드 제거

### 4. 타입 정의 업데이트
- `ScheduleDetailItem` 인터페이스의 `Elevation` 필드 주석 수정
- displayMinElevationAngle 언급 제거, 하드웨어 제한 각도 기준으로 명시

---

## 구현 결과

### ✅ Phase 1: 백엔드 설정 제거
- `SettingsService.kt`에서 설정 정의 및 프로퍼티 제거 완료

### ✅ Phase 2: 백엔드 필터링 로직 정리
- `EphemerisService.kt`의 모든 필터링 위치에서 `displayMinElevationAngle` 관련 로직 제거
- `PassScheduleService.kt`의 필터링 로직 정리
- MaxElevation 재계산 로직 수정

### ✅ Phase 3: 프론트엔드 설정 제거
- `ephemerisTrackService.ts`에서 API 호출 메서드 제거
- `ephemerisTrackStore.ts`에서 상태 및 필터링 로직 제거
- `types/ephemerisTrack.ts`에서 타입 정의 업데이트

---

## 최종 결과

### ✅ 핵심 문제 해결
- **계산 오류 해결**: `displayMinElevationAngle` 기준 필터링 제거로 계산 정확도 향상
- **필터링 기준 통일**: 모든 필터링을 `sourceMinElevationAngle` 또는 `angleElevationMin` 기준으로 통일
- **복잡도 감소**: 불필요한 필터링 로직 제거로 코드 복잡도 감소
- **MaxElevation 정확도 향상**: 하드웨어 제한 각도 기준으로 재계산하여 스케줄 선택 정확도 향상

### ✅ 코드 품질 향상
- 필터링 로직 일관성 유지
- 하드웨어 제한 각도(`angleElevationMin`) 기준으로 통일
- 불필요한 설정 및 로직 제거로 유지보수성 향상

---

## 관련 파일

### 백엔드
- `SettingsService.kt`: `displayMinElevationAngle`, `enableDisplayMinElevationFiltering` 설정 제거
- `EphemerisService.kt`: 모든 필터링 위치에서 `displayMinElevationAngle` 관련 로직 제거
  - `getEphemerisTrackDtlByMstId()`: 핵심 메서드 (74곳 호출)
  - `getRealtimeTrackingData()`: 실시간 추적 데이터 필터링
  - `mergeEphemerisTrackMstData()`: MST 데이터 병합 필터링
  - `getTrackingDataForPass()`: 패스별 추적 데이터 필터링
  - `downloadTheoreticalDataAsCsv()`: CSV 다운로드 필터링
- `PassScheduleService.kt`: 스케줄 목록 필터링 및 MaxElevation 재계산 로직 수정

### 프론트엔드
- `ephemerisTrackService.ts`: 설정 조회 API 호출 메서드 제거
- `ephemerisTrackStore.ts`: 상태 및 필터링 로직 제거
- `types/ephemerisTrack.ts`: 타입 정의 주석 업데이트

---

## 결론

모든 계획 사항이 성공적으로 적용되었으며, 핵심 문제가 해결되었습니다. `displayMinElevationAngle`과 `enableDisplayMinElevationFiltering` 설정이 완전히 제거되었고, 모든 필터링이 `sourceMinElevationAngle` 또는 `angleElevationMin` (하드웨어 제한) 기준으로만 처리되도록 통일되었습니다.

**주요 성과**:
1. 계산 오류 해결 완료
2. 필터링 기준 통일 완료
3. 코드 복잡도 감소 완료
4. MaxElevation 정확도 향상 완료


