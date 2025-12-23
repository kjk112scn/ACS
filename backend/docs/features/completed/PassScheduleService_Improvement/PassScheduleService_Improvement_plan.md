# PassScheduleService 개선 계획

## 개요

PassScheduleService에 SatelliteTrackingProcessor를 통합하여 EphemerisService와 동일한 수준의 데이터 처리(2축, 3축, Keyhole 계산)를 지원하고, 상태머신에 Train 각도 설정 로직을 추가하여 Keyhole 위성을 올바르게 추적할 수 있도록 개선합니다.

> **참고**: 이 문서는 원본 계획 문서(`PassScheduleService_Improvement_And_PushDataService_Integration_Plan.md`)와 검증 계획 문서(`PassScheduleService_Improvement_Final_Verification.md`)를 통합한 것입니다.

---

## Part 1: PassScheduleService 개선 및 PushDataService 통합 계획

### 현재 상태 분석

#### PassScheduleService.kt (개선 필요)

**상태머신 구조**:
- TrackingState: IDLE, WAITING, PREPARING, TRACKING, COMPLETED
- `checkTrackingScheduleWithStateMachine()`: 100ms 주기로 상태 체크 (259줄)
- `executeStateAction()`: 상태별 액션 실행 (368-425줄)
  - TRACKING: `prepareTrackingStart()` 호출 (383줄)
  - PREPARING: `moveToStartPosition()` 호출 (398줄) - Train=0 하드코딩 (650줄)
  - WAITING/COMPLETED: `moveToStowPosition()` 호출 (390, 417줄)

**데이터 처리**:
- `generatePassScheduleTrackingDataAsync()` (1319-1513줄):
  - `OrekitCalculator`로 2축 데이터만 생성 (1331-1340줄)
  - `LimitAngleCalculator`로 ±270도 변환만 수행 (1444-1447줄)
  - `SatelliteTrackingProcessor` 미사용
  - Keyhole 계산 로직 없음
  - `IsKeyhole`, `RecommendedTrainAngle` 필드 없음
  - 단일 DataType만 저장 (변환된 데이터만, 1469-1470줄)

**조회 메서드**:
- `getSelectedTrackMstByMstId()` (1656-1662줄): Keyhole 정보 없음
- `getSelectedTrackDtlByMstId()` (1664-1670줄): Keyhole 정보 없음
- `getTrackingPassMst()` 헬퍼 함수 없음

**문제점**:
1. `moveToStartPosition()`에서 Train 각도가 0으로 하드코딩됨 (650줄)
2. 상태머신에 Train 각도 설정 로직이 없음 (EphemerisService의 `MOVING_TRAIN_TO_ZERO` 상태와 유사한 로직 필요)
3. Keyhole 계산 및 다중 DataType 저장 미지원

### EphemerisService.kt (개선 완료, 참고용)

**데이터 처리**:
- `SatelliteTrackingProcessor` 사용 (46줄)
- 5가지 DataType 저장: `original`, `axis_transformed`, `final_transformed`, `keyhole_axis_transformed`, `keyhole_final_transformed` (425-447줄)
- Keyhole 계산 및 `RecommendedTrainAngle` 포함
- `getTrackingPassMst()`: Keyhole 여부에 따라 동적으로 MST 선택 (2796-2845줄)

**상태머신**:
- `MOVING_TRAIN_TO_ZERO` 상태에서 Keyhole 여부에 따라 Train 각도 동적 설정 (983-1021줄)

### 개선 목표

1. **PassScheduleService.kt 개선**
   - `SatelliteTrackingProcessor` 통합
   - Keyhole 계산 및 다중 DataType 저장 지원
   - 상태머신에 Train 각도 설정 로직 추가
   - `EphemerisService.kt`와 동일한 수준의 데이터 처리

2. **PushDataService.kt 개선** (계획에 포함되었으나 미구현)
   - `PassScheduleService`의 개선된 데이터 활용
   - Keyhole 정보 및 다중 DataType 지원
   - 실시간 데이터 품질 향상
   - **최종**: Controller API를 통한 프론트엔드 연동으로 대체

### 구현 계획

#### Phase 1: 기본 인프라 구축

##### Step 1.1: SatelliteTrackingProcessor 주입
- 생성자에 `satelliteTrackingProcessor: SatelliteTrackingProcessor` 추가

##### Step 1.2: determineKeyholeDataType() 헬퍼 함수 추가
- Keyhole 판단 로직을 공통 헬퍼 함수로 추출
- `passScheduleTrackMstStorage`와 `selectedTrackMstStorage` 모두에서 사용 가능하도록 구현

#### Phase 2: 데이터 생성 및 저장 개선

##### Step 2.1: generatePassScheduleTrackingDataAsync() 개선
- `OrekitCalculator`로 2축 데이터 생성 (유지)
- `SatelliteTrackingProcessor.processFullTransformation()` 호출
- 5가지 DataType 저장:
  - `original`
  - `axis_transformed`
  - `final_transformed`
  - `keyhole_axis_transformed`
  - `keyhole_final_transformed`

#### Phase 3: 조회 메서드 개선

##### Step 3.1: getTrackingPassMst() 헬퍼 함수 추가
- Keyhole 여부에 따라 동적으로 MST 선택
- `final_transformed` MST에서 `IsKeyhole` 확인
- Keyhole 발생 시: `keyhole_final_transformed` MST 반환
- Keyhole 미발생 시: `final_transformed` MST 반환

##### Step 3.2: generateSelectedTrackingData() 개선
- 5가지 DataType 모두 필터링하여 `selectedTrackMstStorage`에 저장

##### Step 3.3: getSelectedTrackDtlByMstId() 개선
- Keyhole 여부에 따라 적절한 DataType 반환

#### Phase 4: 상태머신 개선

##### Step 4.1: PreparingStep enum 추가
- `PreparingStep` enum 추가: `INIT`, `MOVING_TRAIN`, `WAITING_TRAIN`, `MOVING_AZ_EL`

##### Step 4.2: Train 회전 관련 헬퍼 함수 추가
- `moveTrainToZero(trainAngle: Float)`: Train 축만 활성화하여 회전
- `moveToTargetAzEl()`: Azimuth, Elevation 축만 활성화하여 이동
- `isTrainAtZero()`: Train 각도 도달 확인
- `isTrainStabilized()`: Train 각도 안정화 확인

##### Step 4.3: moveToStartPosition() 개선
- `getTrackingPassMst()` 사용 (Keyhole-aware)
- `getSelectedTrackDtlByMstId()` 사용 (Keyhole-aware)
- `targetAzimuth`, `targetElevation` 설정
- `currentPreparingStep = PreparingStep.MOVING_TRAIN` 설정

##### Step 4.4: executeStateAction() PREPARING 상태 개선
- PREPARING 상태에서 `currentPreparingStep`에 따라 단계별 처리
- `MOVING_TRAIN`: Train 회전 명령 전송
- `WAITING_TRAIN`: Train 안정화 대기
- `MOVING_AZ_EL`: Az/El 이동 명령 전송

#### Phase 5: ICD 프로토콜 함수 개선

##### Step 5.1: sendHeaderTrackingData() 개선
- `getTrackingPassMst()` 사용 (Keyhole 정보 포함)

##### Step 5.2: sendInitialTrackingData() 개선
- Keyhole 여부에 따라 적절한 DataType 반환

##### Step 5.3: sendAdditionalTrackingData() 함수 이름 개선 및 비동기/동기 처리 최적화
- 함수 이름 개선:
  - `sendAdditionalTrackingDataOptimized()` → `sendAdditionalTrackingData()`
  - `sendFromCache()` → `sendAdditionalTrackingDataFromCache()`
  - `sendFromDatabase()` → `sendAdditionalTrackingDataFromDatabase()`
  - `sendAdditionalTrackingDataLegacy()` → 제거
- 비동기/동기 처리 최적화:
  - 캐시 있으면: 동기 처리 (빠름, 즉시 전송)
  - 캐시 없으면: 비동기 처리 (DB 조회는 느릴 수 있으므로 블로킹 방지)

#### Phase 6: Controller API 개선

##### Step 6.1: getAllPassScheduleTrackMstMerged() 함수 추가
- 5가지 DataType의 MST 데이터를 병합하여 Keyhole 정보 포함

##### Step 6.2: PassScheduleController.kt 개선
- `getAllPassScheduleTrackMstMerged()` 사용 (Keyhole 정보 포함)

#### Phase 7: 캐시 관련 함수 개선

##### Step 7.1: preloadTrackingDataCache() 개선
- `getSelectedTrackDtlByMstId()` 사용 (이미 Keyhole-aware로 개선됨)

##### Step 7.2: calculateDataLength() 개선
- `getSelectedTrackDtlByMstId()` 사용 (이미 Keyhole-aware로 개선됨)

#### Phase 8: 프론트엔드 개선

##### Step 8.1: 프론트엔드 타입 개선
- Keyhole 정보 필드 추가: `IsKeyhole`, `RecommendedTrainAngle`
- 축 변환 정보 필드 추가

##### Step 8.2: 프론트엔드 매핑 개선
- Keyhole 정보 매핑 추가
- 축 변환 정보 매핑 추가

##### Step 8.3: 프론트엔드 UI 개선
- Keyhole 정보 컬럼 추가
- 2축/3축/최종 데이터 컬럼 추가
- Elevation 각도 컬럼 추가 (Keyhole 여부에 따라 동적 값 표시)
- 가독성 개선

---

## Part 2: PassScheduleService 개선 최종 검증 계획

### 검증 목표

계획 파일(`PassScheduleService_Improvement_And_PushDataService_Integration_Plan.md`)에 명시된 모든 Phase 및 Step의 구현 상태를 검증합니다.

### 검증 범위

- Phase 1: 기본 인프라 구축
- Phase 2: 데이터 생성 및 저장 개선
- Phase 3: 조회 메서드 개선
- Phase 4: 상태머신 개선
- Phase 5: ICD 프로토콜 함수 개선
- Phase 6: Controller API 개선
- Phase 7: 캐시 관련 함수 개선
- Phase 8: 프론트엔드 개선

### 검증 항목

1. ✅ SatelliteTrackingProcessor 통합
2. ✅ 5가지 DataType 저장
3. ✅ Keyhole 계산 및 저장
4. ✅ 상태머신 Train 각도 설정 로직
5. ✅ Keyhole-aware 조회 메서드
6. ✅ ICD 프로토콜 함수 개선
7. ✅ Controller API 개선
8. ✅ 프론트엔드 타입 및 UI 개선

### 코드 품질 검증

- ✅ KDOC 주석 포함
- ✅ 예외 처리 포함
- ✅ 중복 코드 제거
- ✅ 함수 이름 개선
- ✅ 비동기/동기 처리 최적화

---

## 구현 중 발생한 문제 및 해결

### 문제 1: Smart Cast 에러 (EphemerisService.kt)

**발생 위치**: `EphemerisService.kt:821:25`

**에러 메시지**:
```
Smart cast to 'Map<String, Any?>' is impossible, because 'currentTrackingPass' is a mutable property that could have been changed by this time
```

**원인**: Kotlin의 smart cast는 mutable property에 대해 작동하지 않음

**해결**: `getTrackingPassMst()` 결과를 로컬 변수(`selectedPass`)에 먼저 할당한 후 사용

### 문제 2: SelectScheduleContent.vue UI 개선 요청

**요구사항**: EphemerisDesignationPage 수준의 상세한 정보 표시

**해결**: 2축/3축/최종 데이터 컬럼 추가 및 가독성 개선

### 문제 3: Elevation 각도 컬럼 추가 요청

**요구사항**: Azimuth 각도 옆에 Elevation 각도 컬럼 추가

**해결**: Keyhole 여부에 따라 동적 값 표시하는 Elevation 각도 컬럼 추가

---

## 참고 파일

- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/processor/SatelliteTrackingProcessor.kt`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/controller/mode/PassScheduleController.kt`
- `ACS/src/components/content/SelectScheduleContent.vue`
- `ACS/src/services/mode/passScheduleService.ts`
- `ACS/src/stores/mode/passScheduleStore.ts`

