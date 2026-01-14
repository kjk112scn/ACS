# PassScheduleService 개선 요약

---

**완료일**: 2024-12  
**작성자**: GTL Systems  
**상태**: ✅ 구현 완료 및 검증 완료

---

## 개요

PassScheduleService에 SatelliteTrackingProcessor를 통합하여 EphemerisService와 동일한 수준의 데이터 처리(2축, 3축, Keyhole 계산)를 지원하고, 상태머신에 Train 각도 설정 로직을 추가하여 Keyhole 위성을 올바르게 추적할 수 있도록 개선했습니다.

> **참고**: 이 문서는 원본 계획 문서(`PassScheduleService_Improvement_And_PushDataService_Integration_Plan.md`)와 검증 문서(`PassScheduleService_Improvement_Final_Verification.md`)를 통합한 요약입니다.

---

## 주요 개선 사항

### 1. 백엔드 개선

#### 1.1 SatelliteTrackingProcessor 통합
- `SatelliteTrackingProcessor` 주입 (53줄)
- `processFullTransformation()` 호출하여 5가지 DataType 생성
- Keyhole 계산 및 Train 각도 자동 계산

#### 1.2 5가지 DataType 저장
- `original`: 2축 원본 데이터
- `axis_transformed`: 3축 변환 중간 (Train=0, 0-360°)
- `final_transformed`: 최종 데이터 (Train=0, ±270°), **Keyhole 판단 기준**
- `keyhole_axis_transformed`: Keyhole 3축 중간 (Train≠0, 0-360°)
- `keyhole_final_transformed`: Keyhole 최종 (Train≠0, ±270°), **실제 사용**

#### 1.3 Keyhole-aware 조회 메서드
- `determineKeyholeDataType()`: Keyhole 여부 확인 및 적절한 DataType 반환
- `getTrackingPassMst()`: Keyhole 여부에 따라 동적으로 MST 선택
- `getSelectedTrackDtlByMstId()`: Keyhole 여부에 따라 적절한 DataType의 DTL 반환
- `generateSelectedTrackingData()`: 5가지 DataType 모두 필터링

#### 1.4 상태머신 Train 회전 로직
- `PreparingStep` enum 추가: INIT, MOVING_TRAIN, WAITING_TRAIN, MOVING_AZ_EL
- PREPARING 상태 내에서 Train 회전 → 안정화 대기 → Az/El 이동 순차 처리
- Train 회전과 Az/El 이동 분리 (한 번에 움직이지 않음)
- Keyhole 여부에 따라 Train 각도 동적 설정

#### 1.5 ICD 프로토콜 함수 개선
- `sendHeaderTrackingData()`: `getTrackingPassMst()` 사용 (Keyhole 정보 포함)
- `sendInitialTrackingData()`: Keyhole-aware 데이터 사용
- `sendAdditionalTrackingData()`: 함수 이름 개선 및 비동기/동기 처리 최적화
  - 캐시 있으면: 동기 처리 (빠름, 즉시 전송)
  - 캐시 없으면: 비동기 처리 (DB 조회는 느릴 수 있으므로 블로킹 방지)

#### 1.6 Controller API 개선
- `getAllPassScheduleTrackMstMerged()` 함수 추가: 5가지 DataType 병합하여 Keyhole 정보 포함
- `PassScheduleController.kt` 개선: `/pass-schedule/tracking/master` API에 Keyhole 정보 포함

---

### 2. 프론트엔드 개선

#### 2.1 SelectScheduleContent.vue UI 개선
- Keyhole 정보 컬럼 추가: `isKeyhole`, `recommendedTrainAngle`
- 2축/3축/최종 데이터 컬럼 추가:
  - 2축: `OriginalMaxElevation`, `OriginalMaxAzRate`, `OriginalMaxElRate`
  - 3축: `Train0MaxElevation`, `Train0MaxAzRate`, `Train0MaxElRate`
  - 최종: `MaxElevation`, `FinalTransformedMaxAzRate`, `FinalTransformedMaxElRate` (Keyhole에 따라 동적)
- Azimuth/Elevation 각도 컬럼 추가: Keyhole 여부에 따라 동적 값 표시
- 가독성 개선: 테이블 높이, 컬럼 너비, 폰트 크기, 패딩 증가

---

## 주요 변경 파일

### 백엔드
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/controller/mode/PassScheduleController.kt`

### 프론트엔드
- `ACS/src/components/content/SelectScheduleContent.vue`
- `ACS/src/services/mode/passScheduleService.ts`
- `ACS/src/stores/mode/passScheduleStore.ts`

---

## 구현 중 발생한 문제 및 해결

### 문제 1: Smart Cast 에러 (EphemerisService.kt)
- **원인**: Kotlin의 smart cast는 mutable property에 대해 작동하지 않음
- **해결**: `getTrackingPassMst()` 결과를 로컬 변수에 먼저 할당한 후 사용

### 문제 2: SelectScheduleContent.vue UI 개선 요청
- **요구사항**: EphemerisDesignationPage 수준의 상세한 정보 표시
- **해결**: 2축/3축/최종 데이터 컬럼 추가 및 가독성 개선

### 문제 3: Elevation 각도 컬럼 추가 요청
- **요구사항**: Azimuth 각도 옆에 Elevation 각도 컬럼 추가
- **해결**: Keyhole 여부에 따라 동적 값 표시하는 Elevation 각도 컬럼 추가

---

## 미구현 기능

### PushDataService 연동
- **계획**: `PushDataService`에 `PassScheduleService` 의존성 추가 및 Keyhole 정보 활용
- **최종**: 사용자 요구사항 변경으로 인해 미구현
- **대안**: Controller API를 통한 프론트엔드 연동으로 대체

---

## 검증 결과

### ✅ 모든 Phase 완료
- Phase 1: 기본 인프라 구축 ✅
- Phase 2: 데이터 생성 및 저장 개선 ✅
- Phase 3: 조회 메서드 개선 ✅
- Phase 4: 상태머신 개선 ✅
- Phase 5: ICD 프로토콜 함수 개선 ✅
- Phase 6: Controller API 개선 ✅
- Phase 7: 캐시 관련 함수 개선 ✅
- Phase 8: 프론트엔드 개선 ✅

### ✅ 계획 파일 요구사항 충족
1. ✅ SatelliteTrackingProcessor 통합
2. ✅ 5가지 DataType 저장
3. ✅ Keyhole 계산 및 저장
4. ✅ 상태머신 Train 각도 설정 로직
5. ✅ Keyhole-aware 조회 메서드
6. ✅ ICD 프로토콜 함수 개선
7. ✅ Controller API 개선
8. ✅ 프론트엔드 타입 및 UI 개선

### ✅ 코드 품질
- ✅ KDOC 주석 포함
- ✅ 예외 처리 포함
- ✅ 중복 코드 제거
- ✅ 함수 이름 개선
- ✅ 비동기/동기 처리 최적화

### ✅ 컴파일 및 기능 검증
- ✅ 백엔드 컴파일 성공
- ✅ 프론트엔드 컴파일 성공
- ✅ 5가지 DataType 저장 확인
- ✅ Keyhole 계산 로직 확인
- ✅ 상태머신 Train 회전 로직 확인
- ✅ Controller API Keyhole 정보 포함 확인
- ✅ 프론트엔드 UI 개선 확인

---

## 참고 문서

- 원본 계획: `PassScheduleService_Improvement_Original_Plan.md`
- 최종 결과: `PassScheduleService_Improvement_Final_Result.md`

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2024-12

