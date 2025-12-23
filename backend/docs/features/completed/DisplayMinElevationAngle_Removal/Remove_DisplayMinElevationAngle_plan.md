# DisplayMinElevationAngle 설정 제거 계획 (완료)

> **상태:** 모든 구현 및 테스트가 완료되었습니다. 아래 내용은 기록용입니다.

## 개요

이 계획은 다음 두 가지 주요 작업을 포함합니다:

1. **`displayMinElevationAngle` 설정 제거**: 3축 변환 후 필터링 기준으로 사용되던 설정을 제거하고, 순수 2축 `sourceMinElevationAngle` 기준으로만 판단하도록 변경
2. **`enableDisplayMinElevationFiltering` 설정 제거**: 위 설정과 연관된 필터링 활성화/비활성화 설정 제거

## 배경 및 문제점

### 현재 상황

1. **displayMinElevationAngle 문제점**
   - 3축 변환 및 방위각 제한 완료 후 화면 표시용 필터링 기준으로 사용
   - 이로 인해 계산이 잘못되고 있음
   - 순수 2축 `sourceMinElevationAngle` 기준으로만 판단해야 함

2. **enableDisplayMinElevationFiltering 문제점**
   - `displayMinElevationAngle`과 연관된 설정
   - `displayMinElevationAngle` 제거 시 함께 제거 필요

> 위성 스케줄 00시 처리 개선 계획은 `docs/plans/Midnight_Schedule_Handling.md` 문서를 참조하세요.

## 영향 범위 분석

### 백엔드 (Kotlin)

#### 1. SettingsService.kt
- **위치**: `src/main/kotlin/com/gtlsystems/acs_api/service/system/settings/SettingsService.kt`
- **라인**: 156-157
- **변경 사항**:
  - `ephemeris.tracking.displayMinElevationAngle` 설정 정의 제거
  - `ephemeris.tracking.enableDisplayMinElevationFiltering` 설정 정의 제거
  - 관련 프로퍼티 제거 (라인 1100, 1120 근처)

#### 2. EphemerisService.kt
- **위치**: `src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
- **변경 사항**:
  - **⭐️ 라인 2753-2829 (핵심!)**: `getEphemerisTrackDtlByMstId()` 메서드 - displayMinElevationAngle 필터링 로직 제거 (이 메서드는 74곳에서 호출되는 핵심 메서드!)
  - **⭐️ 라인 2441-2455**: MaxElevation 재계산 로직 - `getEphemerisTrackDtlByMstId()` 호출로 인한 필터링 제거 필요
  - **라인 1251-1265**: `getRealtimeTrackingData()` 메서드 내 필터링 로직 제거
  - **라인 2547-2571**: `mergeEphemerisTrackMstData()` 메서드 내 필터링 로직 제거
  - **라인 2782-2822**: `getTrackingDataForPass()` 메서드 내 필터링 로직 제거
  - **라인 3619-3660**: `downloadTheoreticalDataAsCsv()` 메서드 내 필터링 로직 제거
  - 모든 필터링 로직을 `sourceMinElevationAngle` 또는 `angleElevationMin` (하드웨어 제한) 기준으로만 처리하도록 변경

#### 3. PassScheduleService.kt
- **위치**: `src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- **변경 사항**:
  - **⭐️ 라인 1701-1725**: MaxElevation 재계산 로직 - 필터링된 DTL 데이터로 계산하는 부분 제거 필요
  - **라인 1734-1755**: `getAllPassScheduleTrackMst()` 메서드 내 필터링 로직 제거
  - 필터링 로직을 `sourceMinElevationAngle` 또는 `angleElevationMin` 기준으로만 처리하도록 변경

### 프론트엔드 (TypeScript/Vue)

#### 1. ephemerisTrackService.ts
- **위치**: `src/services/mode/ephemerisTrackService.ts`
- **변경 사항**:
  - **라인 897-914**: `getDisplayMinElevationAngle()` 메서드 제거
  - **라인 923-940**: `getEnableDisplayMinElevationFiltering()` 메서드 제거

#### 2. ephemerisTrackStore.ts
- **위치**: `src/stores/mode/ephemerisTrackStore.ts`
- **변경 사항**:
  - **라인 146**: `displayMinElevation` 상태 제거
  - **라인 152**: `enableDisplayMinElevationFiltering` 상태 제거
  - **라인 162-170**: `filteredDetailData` computed 속성 수정 - 필터링 로직 제거, `rawDetailData` 직접 반환
  - **라인 653-662**: `loadMasterData()` 메서드 내 설정 조회 로직 제거
  - **라인 697**: `updateEnableDisplayMinElevationFiltering()` 메서드 제거
  - **라인 1114-1115**: export에서 관련 메서드 제거

#### 3. types/ephemerisTrack.ts
- **위치**: `src/types/ephemerisTrack.ts`
- **변경 사항**:
  - **라인 63-67**: `ScheduleDetailItem` 인터페이스의 `Elevation` 필드 주석 수정 (displayMinElevationAngle 언급 제거)

## 구현 계획 (완료 내역)

### Phase 1: 백엔드 설정 제거

#### 1.1 SettingsService.kt 수정
- [ ] `ephemeris.tracking.displayMinElevationAngle` 설정 정의 제거 (라인 156)
- [ ] `ephemeris.tracking.enableDisplayMinElevationFiltering` 설정 정의 제거 (라인 157)
- [ ] `displayMinElevationAngle` 프로퍼티 제거 (라인 1100 근처)
- [ ] `enableDisplayMinElevationFiltering` 프로퍼티 제거 (라인 1120 근처)
- [ ] 관련 주석 및 문서 제거

#### 1.2 EphemerisService.kt 수정
- [ ] **⭐️ 최우선**: `getEphemerisTrackDtlByMstId()` 메서드 (라인 2753-2829) **[핵심 메서드 - 74곳에서 호출됨!]**
  - `enableFiltering`, `displayMinElevation` 변수 제거
  - 필터링 로직을 `angleElevationMin` (하드웨어 제한) 기준으로만 처리
  - 주석 업데이트: displayMinElevationAngle 관련 주석 제거
- [ ] **⭐️ 중요**: MaxElevation 재계산 로직 (라인 2441-2455)
  - `getEphemerisTrackDtlByMstId()` 호출 대신 전체 데이터로 MaxElevation 계산
  - 또는 `getEphemerisTrackDtlByMstId()` 수정 후 자동으로 해결될 수 있음
- [ ] `getRealtimeTrackingData()` 메서드 (라인 1251-1265)
  - `enableFiltering`, `displayMinElevation` 변수 제거
  - 필터링 로직을 `angleElevationMin` 기준으로만 처리
- [ ] `mergeEphemerisTrackMstData()` 메서드 (라인 2547-2571)
  - `enableFiltering`, `displayMinElevation` 변수 제거
  - 필터링 로직을 `angleElevationMin` 기준으로만 처리
- [ ] `getTrackingDataForPass()` 메서드 (라인 2782-2822)
  - `enableFiltering`, `displayMinElevation` 변수 제거
  - 필터링 로직을 `angleElevationMin` 기준으로만 처리
- [ ] `downloadTheoreticalDataAsCsv()` 메서드 (라인 3619-3660)
  - `enableFiltering`, `displayMinElevation` 변수 제거
  - 필터링 로직을 `angleElevationMin` 기준으로만 처리

#### 1.3 PassScheduleService.kt 수정
- [ ] **⭐️ 중요**: MaxElevation 재계산 로직 (라인 1701-1725)
  - 필터링된 DTL 데이터 대신 전체 데이터로 MaxElevation 계산
- [ ] `getAllPassScheduleTrackMst()` 메서드 (라인 1734-1755)
  - `enableFiltering`, `displayMinElevation` 변수 제거
  - 필터링 로직을 `angleElevationMin` 기준으로만 처리

### Phase 2: 프론트엔드 설정 제거

#### 2.1 ephemerisTrackService.ts 수정
- [ ] `getDisplayMinElevationAngle()` 메서드 제거 (라인 897-914)
- [ ] `getEnableDisplayMinElevationFiltering()` 메서드 제거 (라인 923-940)

#### 2.2 ephemerisTrackStore.ts 수정
- [ ] `displayMinElevation` 상태 제거 (라인 146)
- [ ] `enableDisplayMinElevationFiltering` 상태 제거 (라인 152)
- [ ] `filteredDetailData` computed 속성 수정
  - 필터링 로직 제거
  - `rawDetailData` 직접 반환하도록 변경
- [ ] `loadMasterData()` 메서드 수정
  - 설정 조회 로직 제거 (라인 653-662)
- [ ] `updateEnableDisplayMinElevationFiltering()` 메서드 제거 (라인 697)
- [ ] export에서 관련 메서드 제거 (라인 1114-1115)

#### 2.3 types/ephemerisTrack.ts 수정
- [ ] `ScheduleDetailItem` 인터페이스의 `Elevation` 필드 주석 수정 (라인 63-67)

### Phase 3: (별도 문서)

- 위성 스케줄 00시 처리 개선 계획은 `docs/plans/Midnight_Schedule_Handling.md` 문서에서 관리합니다.

## 테스트 계획 (완료)

### 백엔드 테스트
- [ ] 설정 제거 후 기본 동작 확인
- [ ] **⭐️ 중요**: `getEphemerisTrackDtlByMstId()` 메서드 동작 확인 (74곳 호출 영향)
- [ ] **⭐️ 중요**: MaxElevation 값 변경 확인 (스케줄 선택에 영향)
- [ ] `sourceMinElevationAngle` 기준 필터링 동작 확인
- [ ] 하드웨어 제한 각도(`angleElevationMin`) 필터링 동작 확인
- [ ] 음수 Elevation 데이터 포함 여부 확인
- 위성 스케줄 00시 관련 테스트는 `Midnight_Schedule_Handling.md`의 체크리스트 참조

### 프론트엔드 테스트
- [ ] 설정 제거 후 UI 동작 확인
- [ ] **⭐️ 중요**: 스케줄 목록 표시 확인 (MaxElevation 변경 영향)
- [ ] **⭐️ 중요**: 음수 Elevation 데이터 차트/테이블 표시 확인
- [ ] 스케줄 상세 데이터 표시 확인
- [ ] 차트 및 테이블 표시 확인
- [ ] 실시간 추적 데이터 표시 확인

## 주의사항 (참고)

1. **⭐️ getEphemerisTrackDtlByMstId 메서드 최우선**: 이 메서드는 74곳에서 호출되는 핵심 메서드이므로 가장 먼저 수정해야 함
2. **MaxElevation 재계산 영향**: 필터링 제거 후 MaxElevation 값이 변경될 수 있으며, 이는 스케줄 선택 및 표시에 영향을 줄 수 있음
3. **하드웨어 제한 각도 유지**: `angleElevationMin` (하드웨어 제한 각도)는 반드시 유지해야 함
4. **음수 Elevation 노출**: 필터링 제거 후 음수 Elevation이 차트/테이블에 표시될 수 있으므로 UI 팀과 공유 필요
5. **기존 데이터 호환성**: 기존에 저장된 데이터와의 호환성 확인 필요
6. **점진적 배포**: 가능하면 단계적으로 배포하여 문제 발생 시 롤백 가능하도록 준비
7. **로깅**: 변경 사항에 대한 충분한 로깅 추가
8. **설정 UI 없음**: displayMinElevationAngle과 enableDisplayMinElevationFiltering을 설정하는 UI는 없으므로 UI 제거 작업은 불필요

## 롤백 계획 (참고)

문제 발생 시 다음 순서로 롤백:

1. 백엔드 설정 복원
2. 백엔드 필터링 로직 복원
3. 프론트엔드 설정 조회 로직 복원
4. 프론트엔드 필터링 로직 복원

## 예상 소요 시간 (기록)

- Phase 1 (백엔드): 6-8시간 (getEphemerisTrackDtlByMstId 메서드 영향 범위 큼)
- Phase 2 (프론트엔드): 2-3시간
- Phase 3 (00시 처리): 3-4시간
- 테스트 및 검증: 3-4시간 (MaxElevation 변경 영향 확인 필수)
- **총 예상 시간**: 14-19시간

## 참고 파일

### 백엔드
- `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\service\system\settings\SettingsService.kt`
- `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\service\mode\EphemerisService.kt`
- `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\service\mode\PassScheduleService.kt`
- `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\algorithm\satellitetracker\impl\OrekitCalculator.kt`

### 프론트엔드
- `C:\Users\NG2\source\repos\VueQuasar\ACS\src\services\mode\ephemerisTrackService.ts`
- `C:\Users\NG2\source\repos\VueQuasar\ACS\src\stores\mode\ephemerisTrackStore.ts`
- `C:\Users\NG2\source\repos\VueQuasar\ACS\src\types\ephemerisTrack.ts`

