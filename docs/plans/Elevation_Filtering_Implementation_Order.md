# Elevation 필터링 및 Keyhole 데이터 선택 구현 순서

---
**작성일**: 2024-12-05
**작성자**: GTL Systems
**상태**: 준비 완료
**관련 계획서**: `Elevation_Filtering_And_Keyhole_Data_Selection_Plan.md`
---

## 목표

계획서의 모든 Step을 의존성 관계와 컴파일 순서를 고려하여 단계적으로 구현하여 완벽하게 구동되도록 합니다.

## 구현 순서 및 의존성 관계

### 의존성 다이어그램

```
Step 7-1 (sourceMinElevationAngle) [독립적]
  └─ 독립적으로 먼저 완료 가능

Step 1 (getEphemerisTrackDtlByMstId) [핵심 함수]
  ├─ Step 2 (createRealtimeTrackingData) 의존
  ├─ Step 3 (sendHeaderTrackingData) 의존
  ├─ Step 4 (sendInitialTrackingData) 의존
  └─ Step 5 (exportMstDataToCsv) 의존

Step 7-2 (프론트엔드 CSV) [독립적]
  └─ 백엔드 완료 후 진행 가능
```

## 단계별 구현 계획

### Phase 0: 준비 단계

**목적**: 작업 전 현재 상태 확인 및 백업

**작업 내용**:
1. 현재 코드 상태 확인
2. Git 브랜치 생성 (선택사항)
3. 계획서 최종 확인

**체크리스트**:
- [ ] `EphemerisService.kt` 파일 열기
- [ ] `SettingsService.kt` 파일 열기
- [ ] `EphemerisDesignationPage.vue` 파일 위치 확인
- [ ] 현재 프로젝트 컴파일 상태 확인

**예상 소요 시간**: 5분

---

### Phase 1: Step 7-1 - sourceMinElevationAngle 설정 설명 업데이트

**목적**: 가장 단순하고 독립적인 작업부터 시작

**파일**: 
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/system/settings/SettingsService.kt`

**수정 위치**: Line 155 (SettingDefinition)

**작업 내용**:
1. `sourceMinElevationAngle` SettingDefinition 설명 문자열 수정
2. 자동 계산 관련 문구 제거
3. 권장 공식 정보 추가

**수정 코드**:
```kotlin
"ephemeris.tracking.sourceMinElevationAngle" to SettingDefinition(
    "ephemeris.tracking.sourceMinElevationAngle", 
    -7.0,
    SettingType.DOUBLE, 
    "원본 2축 위성 추적 데이터 생성 시 최소 Elevation 각도 (도). Orekit 계산 시 사용되는 2축 좌표계 기준. Tilt 각도 보정을 위해 음수 값 허용. 권장 공식: -abs(tiltAngle) - 15도 (예: Tilt -7° → -abs(-7) - 15 = -22.0°). 사용자가 수동으로 계산하여 설정해야 함."
)
```

**컴파일 체크**:
- [ ] `./gradlew compileKotlin` 실행
- [ ] 컴파일 에러 없음 확인

**체크리스트**:
- [ ] 파일 수정 완료
- [ ] 컴파일 성공
- [ ] 변경 사항 확인

**예상 소요 시간**: 10분

---

### Phase 2: Step 1 - getEphemerisTrackDtlByMstId() 수정 (핵심 함수)

**목적**: 모든 다른 Step의 기반이 되는 핵심 함수 수정

**파일**: 
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

**수정 위치**: Line 2326-2330

**의존성**: 없음 (하지만 다른 모든 Step이 이 함수에 의존)

**작업 내용**:
1. Keyhole 여부 확인 로직 추가
2. DataType 자동 선택 로직 추가
3. displayMinElevationAngle 필터링 추가
4. 예외 처리 추가
5. 로깅 추가

**주의사항**:
- 이 함수를 수정하면 다른 함수들의 동작이 변경됨
- 컴파일 후 즉시 테스트 필요

**컴파일 체크**:
- [ ] `./gradlew compileKotlin` 실행
- [ ] 컴파일 에러 없음 확인
- [ ] 함수 시그니처 변경 여부 확인

**기본 테스트**:
- [ ] 함수가 정상적으로 컴파일되는지 확인
- [ ] 함수 호출 시 빈 리스트 반환하지 않는지 확인 (로그 확인)

**체크리스트**:
- [ ] 함수 수정 완료
- [ ] 컴파일 성공
- [ ] 기본 테스트 통과

**예상 소요 시간**: 30분

---

### Phase 3: Step 2 - createRealtimeTrackingData() 수정

**목적**: 실시간 추적 데이터 생성 시 Keyhole 대응 및 필터링

**파일**: 
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

**수정 위치**: Line 1128-1269

**의존성**: Step 1 완료 필요

**작업 내용**:
1. Keyhole 여부 확인 로직 추가
2. DataType 자동 선택 로직 추가
3. displayMinElevationAngle 필터링 추가
4. keyhole_final_transformed 데이터 추가
5. 시간 기준 매칭 로직 개선

**컴파일 체크**:
- [ ] `./gradlew compileKotlin` 실행
- [ ] 컴파일 에러 없음 확인

**체크리스트**:
- [ ] 함수 수정 완료
- [ ] 컴파일 성공
- [ ] Step 1 함수 호출 확인

**예상 소요 시간**: 40분

---

### Phase 4: Step 3 - sendHeaderTrackingData() 수정

**목적**: 데이터 길이 검증 개선

**파일**: 
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

**수정 위치**: Line 1634-1642

**의존성**: Step 1 완료 필요

**작업 내용**:
1. 필터링 후 빈 데이터 체크 추가
2. 데이터 길이 검증 로직 개선
3. 경고 로그 추가

**컴파일 체크**:
- [ ] `./gradlew compileKotlin` 실행
- [ ] 컴파일 에러 없음 확인

**체크리스트**:
- [ ] 함수 수정 완료
- [ ] 컴파일 성공
- [ ] Step 1 함수 호출 확인

**예상 소요 시간**: 15분

---

### Phase 5: Step 4 - sendInitialTrackingData() 수정

**목적**: 필터링된 데이터 인덱스 처리 개선

**파일**: 
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

**수정 위치**: Line 1703-1728

**의존성**: Step 1 완료 필요

**작업 내용**:
1. 시간 기준으로 가장 가까운 데이터 찾기 로직 추가
2. 필터링된 데이터 인덱스 계산 개선
3. 예외 처리 추가

**컴파일 체크**:
- [ ] `./gradlew compileKotlin` 실행
- [ ] 컴파일 에러 없음 확인

**체크리스트**:
- [ ] 함수 수정 완료
- [ ] 컴파일 성공
- [ ] Step 1 함수 호출 확인

**예상 소요 시간**: 25분

---

### Phase 6: Step 5 - exportMstDataToCsv() 수정

**목적**: 이론치 다운로드 CSV 함수 개선

**파일**: 
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

**수정 위치**: Line 3009-3467

**의존성**: Step 1 완료 필요

**작업 내용**:
1. Keyhole 여부 확인 로직 추가
2. DataType 자동 선택 로직 추가
3. displayMinElevationAngle 필터링 추가
4. keyhole_final_transformed 데이터 추가
5. 시간 기준 매칭 로직 추가

**컴파일 체크**:
- [ ] `./gradlew compileKotlin` 실행
- [ ] 컴파일 에러 없음 확인

**체크리스트**:
- [ ] 함수 수정 완료
- [ ] 컴파일 성공
- [ ] Step 1 함수 호출 확인

**예상 소요 시간**: 60분

---

### Phase 7: Step 7-2 - 프론트엔드 CSV 다운로드 함수 개선

**목적**: 프론트엔드 CSV 다운로드에 keyhole_final_transformed 데이터 추가

**파일**: 
- `ACS/src/pages/mode/EphemerisDesignationPage.vue`

**수정 위치**: Line 974-1120 (downloadCSVWithTransformations 함수)

**의존성**: 백엔드 Step 1-5 완료 필요 (API 응답 구조 변경)

**작업 내용**:
1. CSV 헤더에 KeyholeFinalTransformed 필드 추가
2. CSV 데이터에 keyhole_final_transformed 데이터 추가
3. TypeScript 타입 정의 업데이트

**컴파일 체크**:
- [ ] `npm run build` 또는 `npm run type-check` 실행
- [ ] TypeScript 컴파일 에러 없음 확인

**체크리스트**:
- [ ] 함수 수정 완료
- [ ] TypeScript 컴파일 성공
- [ ] 타입 정의 확인

**예상 소요 시간**: 30분

---

## 통합 테스트 체크리스트

### Phase 2 (Step 1) 완료 후 테스트

**목적**: 핵심 함수 수정 후 기본 동작 확인

**테스트 항목**:
- [ ] `getEphemerisTrackDtlByMstId()` 함수 호출 시 정상 동작
- [ ] Keyhole 미발생 경우: `final_transformed` 데이터 반환
- [ ] Keyhole 발생 경우: `keyhole_final_transformed` 데이터 반환 (데이터 있을 경우)
- [ ] Keyhole 발생 경우 데이터 없으면: `final_transformed`로 폴백
- [ ] displayMinElevationAngle 필터링 정상 동작
- [ ] 로그 메시지 확인

**테스트 방법**:
1. 백엔드 서버 실행
2. 위성 궤도 추적 데이터 생성
3. API 호출: `GET /ephemeris/detail/{mstId}`
4. 응답 데이터 확인
5. 로그 확인

---

### Phase 3-6 완료 후 통합 테스트

**목적**: 모든 백엔드 수정 완료 후 전체 기능 테스트

**테스트 항목**:
- [ ] 스케줄 선택 시 올바른 데이터 반환
- [ ] 실시간 추적 데이터 생성 정상 동작
- [ ] 추적 시작 시 헤더 데이터 정상 전송
- [ ] 초기 추적 데이터 정상 전송
- [ ] 이론치 다운로드 CSV 정상 생성
- [ ] Keyhole 발생 시 올바른 데이터 사용

**테스트 방법**:
1. 백엔드 서버 실행
2. 위성 궤도 추적 데이터 생성 (Keyhole 발생/미발생 각각)
3. 프론트엔드에서 스케줄 선택
4. 차트 데이터 확인
5. 추적 시작 테스트
6. CSV 다운로드 테스트

---

### Phase 7 완료 후 전체 테스트

**목적**: 프론트엔드까지 모든 수정 완료 후 최종 테스트

**테스트 항목**:
- [ ] 프론트엔드 차트에 올바른 데이터 표시
- [ ] 프론트엔드 CSV 다운로드 정상 동작
- [ ] Keyhole 발생 시 차트에 올바른 데이터 표시
- [ ] 백엔드와 프론트엔드 데이터 일치 확인

**테스트 방법**:
1. 전체 시스템 실행
2. 위성 궤도 추적 데이터 생성
3. 프론트엔드에서 전체 플로우 테스트
4. 데이터 일치 확인

---

## 예외 상황 대응

### 컴파일 에러 발생 시

**대응 방법**:
1. 에러 메시지 확인
2. 관련 함수 호출 관계 확인
3. 타입 불일치 확인
4. Step 1 완료 여부 확인 (가장 중요)

**체크리스트**:
- [ ] 에러 메시지 분석
- [ ] 관련 함수 확인
- [ ] 의존성 확인
- [ ] 이전 Step 완료 여부 확인

---

### 런타임 에러 발생 시

**대응 방법**:
1. 로그 확인
2. 데이터 존재 여부 확인
3. Keyhole 여부 확인
4. 필터링 결과 확인

**체크리스트**:
- [ ] 로그 메시지 확인
- [ ] 데이터 존재 여부 확인
- [ ] MST 데이터 확인
- [ ] 필터링 결과 확인

---

## 예상 전체 소요 시간

- **Phase 0**: 5분
- **Phase 1**: 10분
- **Phase 2**: 30분
- **Phase 3**: 40분
- **Phase 4**: 15분
- **Phase 5**: 25분
- **Phase 6**: 60분
- **Phase 7**: 30분
- **통합 테스트**: 60분

**총 예상 시간**: 약 4-5시간

---

## 중요 참고사항

1. **Step 1 우선 완료 필수**
   - Step 1이 모든 다른 Step의 기반이 되므로 반드시 먼저 완료
   - Step 1 완료 후 컴파일 및 기본 테스트 필수

2. **컴파일 체크포인트**
   - 각 Phase 완료 후 즉시 컴파일 확인
   - 에러 발생 시 즉시 수정 후 진행

3. **로그 확인**
   - 각 Step 수정 후 로그 메시지 확인
   - 예상된 로그 메시지가 출력되는지 확인

4. **데이터 일치 확인**
   - 백엔드와 프론트엔드 데이터가 일치하는지 확인
   - Keyhole 발생 시 올바른 데이터 사용 확인

---

**문서 버전**: 1.0.0
**최종 업데이트**: 2024-12-05

