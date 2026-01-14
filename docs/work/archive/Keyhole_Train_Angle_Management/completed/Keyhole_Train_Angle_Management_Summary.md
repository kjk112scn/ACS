# Keyhole Train 각도 관리 통합 개선 요약

## 개요

**목표**: Keyhole 위성 추적 시 Train 각도를 올바르게 관리하기 위해 데이터 변환 단계와 추적 단계의 문제를 해결

**핵심 문제**:
1. **데이터 변환 단계**: KEYHOLE=YES인데 Train=0.000000°인 문제
   - Train의 두 가지 용도가 혼재되어 사용됨
2. **추적 단계**: 위성 추적 시 Train 각도 설정 문제
   - `currentTrackingPass` 설정 시 DataType 필터링 없음
   - Train 각도가 0으로 고정 설정됨

**해결 방안**: 
- Part 1: Train의 두 가지 용도를 명확히 구분하고, 각 MST는 독립적으로 본인 기준에서 Keyhole 판단 및 RecommendedTrainAngle 계산
- Part 2: `getTrackingPassMst()` 헬퍼 함수 생성 및 Train 각도 동적 설정

---

## 주요 변경사항

### Part 1: 데이터 변환 단계

#### 1. Train의 두 가지 용도 구분
- **`trainAngleForTransformation`**: 3축 변환용 Train (forcedTrainAngle 또는 MST에서 읽음)
- **`recommendedTrainAngleForMst`**: MST 저장용 RecommendedTrainAngle (본인 기준으로 계산)

#### 2. 각 MST 독립성 원칙
- **Original MST**: 2축 기준으로 Keyhole 판단 및 RecommendedTrainAngle 계산
- **AxisTransformed MST**: 3축, Train=0, ±270도 제한 없음 기준으로 계산
- **FinalTransformed MST**: 3축, Train=0, ±270도 제한 있음 기준으로 계산

#### 3. finalTransformedMst 기준 사용
- `finalTransformedMst`의 `IsKeyhole` 값을 직접 참조 (재판단하지 않음)
- `finalTransformedMst`의 `RecommendedTrainAngle` 사용
- Train≠0 재계산 시 `finalTransformedMst`의 값 사용

### Part 2: 추적 단계

#### 1. getTrackingPassMst() 헬퍼 함수 생성
- Keyhole 여부에 따라 적절한 MST 선택
- Keyhole 발생: `keyhole_final_transformed` MST
- Keyhole 미발생: `final_transformed` MST

#### 2. currentTrackingPass 설정 개선
- `startEphemerisTracking()`: `getTrackingPassMst()` 사용
- `sendHeaderTrackingData()`: `getTrackingPassMst()` 사용

#### 3. Train 각도 동적 설정
- `MOVING_TRAIN_TO_ZERO` 상태: Keyhole 여부에 따라 Train 각도 설정
- `PreparingStep.MOVING_TRAIN` 상태: Keyhole 여부에 따라 Train 각도 설정
- Keyhole 발생: `RecommendedTrainAngle` 사용 (Train≠0)
- Keyhole 미발생: 0 사용 (Train=0)

#### 4. 로깅 메시지 개선
- "0도" → "${trainAngle}도"로 변경
- Keyhole 정보 로깅 추가

---

## 구현 결과

### Part 1: 데이터 변환 단계
- ✅ Step 0: Original MST에서 calculateTrainAngleMethodA/B 제거
- ✅ Step 1: AxisTransformed MST에서 Train의 두 가지 용도 구분
- ✅ Step 2: FinalTransformed MST에 RecommendedTrainAngle 계산 추가
- ✅ Step 3: Keyhole 판단 기준 변경 및 RecommendedTrainAngle 사용
- ✅ Step 4: EphemerisService.getAllEphemerisTrackMstMerged()에서 RecommendedTrainAngle 데이터 소스 수정

### Part 2: 추적 단계
- ✅ Step 5: getTrackingPassMst() 헬퍼 함수 생성
- ✅ Step 6: startEphemerisTracking()에서 currentTrackingPass 설정 개선
- ✅ Step 7: sendHeaderTrackingData()에서 currentTrackingPass 설정 개선
- ✅ Step 8: MOVING_TRAIN_TO_ZERO 상태에서 Train 각도 설정 개선
- ✅ Step 9: PassScheduleService.kt에서도 동일한 로직 적용

---

## 최종 결과

### ✅ 핵심 문제 해결

**Part 1: 데이터 변환 단계**
- **KEYHOLE=YES인데 Train=0.000000°인 문제 해결**
- Keyhole 발생 시 올바른 RecommendedTrainAngle 저장
- API 응답에서 Train 각도가 올바르게 표시됨

**Part 2: 추적 단계**
- **currentTrackingPass 설정 개선**: Keyhole 여부에 따라 적절한 MST 선택
- **Train 각도 동적 설정**: Keyhole 여부에 따라 `RecommendedTrainAngle` 사용
- **로깅 메시지 개선**: 실제 Train 각도 표시

### ✅ 코드 품질 향상
- 변수명 명확화 (`trainAngleForTransformation`, `recommendedTrainAngleForMst`)
- 각 MST 독립성 원칙 준수
- 데이터 일관성 유지
- KDOC 주석 포함
- Keyhole 여부에 따른 동적 MST 선택
- 폴백 로직 포함
- Smart cast 문제 해결

---

## 관련 파일

**Part 1: 데이터 변환 단계**
- `SatelliteTrackingProcessor.kt`: Train의 두 가지 용도 구분 및 각 MST RecommendedTrainAngle 계산
- `EphemerisService.kt`: API 응답에서 `finalTransformedMst`의 `RecommendedTrainAngle` 사용

**Part 2: 추적 단계**
- `EphemerisService.kt`: `getTrackingPassMst()`, `startEphemerisTracking()`, `sendHeaderTrackingData()`, `MOVING_TRAIN_TO_ZERO` 상태
- `PassScheduleService.kt`: `getTrackingPassMst()`, `PreparingStep.MOVING_TRAIN` 상태

---

## 결론

모든 계획 사항이 성공적으로 적용되었으며, 핵심 문제가 해결되었습니다. 

**Part 1**: 각 MST는 독립적으로 본인 기준에서 Keyhole 판단 및 RecommendedTrainAngle을 계산하며, `finalTransformedMst`의 값이 시스템의 주요 판단 기준으로 사용됩니다.

**Part 2**: Keyhole 위성 추적 시 Train 각도가 올바르게 설정되며, `currentTrackingPass`가 Keyhole 여부에 따라 적절한 MST를 가리킵니다.

