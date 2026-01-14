# Elevation 필터링 관리 통합 개선 계획

---
**작성일**: 2024-12-05 (최초 작성), 2024-12-15 (필터링 제어 기능 추가)  
**작성자**: GTL Systems  
**상태**: 최종 통합 계획  
**관련 이슈**: 
- 2축/3축 변환 시 Elevation 시작각도 불일치 문제
- Keyhole 여부에 따른 데이터 선택 미적용
- 백엔드/프론트엔드 데이터 불일치 (필터링 위치)
- displayMinElevationAngle 필터링을 동적으로 제어할 수 없는 문제
---

## 목표

1. **이론치 추출과 실제 추적 데이터 연결**: 이론치 생성과 실제 추적 명령 데이터가 일관되게 연결되도록 개선
2. **Keyhole 대응**: Keyhole 여부에 따라 적절한 DataType (`final_transformed` vs `keyhole_final_transformed`) 자동 선택
3. **백엔드 필터링**: `displayMinElevationAngle` 기준으로 백엔드에서 필터링하여 실제 추적 명령과 프론트엔드 표시 데이터 일치
4. **필터링 제어**: `displayMinElevationAngle` 필터링을 동적으로 활성화/비활성화할 수 있는 설정 추가
5. **코드 정리**: 사용되지 않는 변환 함수 제거 또는 주석 처리

## 배경

### 현재 문제점

1. **이론치와 실제 추적 데이터 분리**
   - 이론치 생성: `SatelliteTrackingProcessor`에서 모든 변환 수행
   - 실제 추적 명령: `EphemerisService`에서 별도로 조회하여 사용
   - Keyhole 여부 확인 로직이 실제 추적 로직에 없음

2. **Keyhole 미대응**
   - `getEphemerisTrackDtlByMstId()`: 항상 `final_transformed`만 반환
   - Keyhole일 경우 `keyhole_final_transformed`를 사용해야 하지만 미적용
   - 실제 추적 명령이 잘못된 데이터 사용

3. **필터링 위치 불일치**
   - `sourceMinElevationAngle = -20도`로 넓게 추적 (이론치 생성)
   - 실제 추적 시 `displayMinElevationAngle = 0도` 기준 필터링 없음
   - 백엔드 추적: -20도부터, 프론트엔드 표시: 0도부터 → 데이터 불일치

4. **하드코딩된 필터링**
   - `displayMinElevationAngle` 필터링이 여러 위치에서 하드코딩되어 있음
   - 필터링을 비활성화하려면 모든 위치를 수정해야 함
   - 설정 변경이 어려움

5. **필터링 위치 분산**
   - 백엔드: `getEphemerisTrackDtlByMstId()`, `createRealtimeTrackingData()`, `exportMstDataToCsv()`, `getAllEphemerisTrackMst()`
   - 프론트엔드: `ephemerisTrackStore.ts`의 `filteredDetailData` computed
   - 각 위치마다 동일한 필터링 로직이 중복되어 있음

6. **필터링 제어 불가**
   - 특정 상황에서 전체 데이터가 필요한 경우 필터링을 비활성화할 수 없음
   - 데이터 분석 시 필터링된 데이터만 사용 가능

7. **사용되지 않는 코드**
   - `applyAxisTransformation()` (Line 475) - `SatelliteTrackingProcessor`에서 이미 수행
   - `applyAngleLimitTransformation()` (Line 674) - `SatelliteTrackingProcessor`에서 이미 수행
   - `saveAllTransformationData()` (Line 719) - 사용 안 함

### 현재 데이터 흐름

```
[1단계: 이론치 생성]
OrekitCalculator.generateSatelliteTrackingSchedule()
  └─ sourceMinElevationAngle = -20도로 가시성 기간 감지
  └─ elevation >= sourceMinElevationAngle 필터링으로 2축 데이터 생성

[2단계: 변환 및 저장]
SatelliteTrackingProcessor.processFullTransformation()
  └─ original (2축 원본)
  └─ axis_transformed (Train=0, 3축 변환)
  └─ final_transformed (Train=0, ±270°)
  └─ keyhole_final_transformed (Train≠0, ±270°) [Keyhole 발생 시만]
  └─ ephemerisTrackMstStorage, ephemerisTrackDtlStorage에 저장

[3단계: 실제 추적 명령] ❌ 문제점
EphemerisService의 실제 추적 함수들:
  ├─ moveToStartPosition() → getEphemerisTrackDtlByMstId() [항상 final_transformed]
  ├─ sendInitialTrackingData() → getEphemerisTrackDtlByMstId() [항상 final_transformed]
  ├─ sendAdditionalTrackingData() → getEphemerisTrackDtlByMstId() [항상 final_transformed]
  └─ createRealtimeTrackingData() → getEphemerisTrackDtlByMstIdAndDataType("final_transformed")
      └─ displayMinElevationAngle 필터링 없음
      └─ Keyhole 여부 확인 없음
```

---

## 전체 프로세스 흐름 분석 및 잠재적 문제점

### 전체 데이터 흐름 (수정 후)

```
[1단계: 이론치 생성] (변경 없음)
OrekitCalculator.generateSatelliteTrackingSchedule()
  └─ sourceMinElevationAngle = -20도로 넓게 추적
  └─ elevation >= sourceMinElevationAngle 필터링으로 2축 데이터 생성

[2단계: 변환 및 저장] (변경 없음)
SatelliteTrackingProcessor.processFullTransformation()
  └─ original (2축 원본) - 모든 데이터 저장 (필터링 없음)
  └─ axis_transformed (Train=0, 3축 변환) - 모든 데이터 저장
  └─ final_transformed (Train=0, ±270°) - 모든 데이터 저장
  └─ keyhole_final_transformed (Train≠0, ±270°) - Keyhole 발생 시만 저장
  └─ ephemerisTrackMstStorage, ephemerisTrackDtlStorage에 저장

[3단계: 실제 추적 명령 데이터 선택] ✅ 수정
getEphemerisTrackDtlByMstId(mstId):
  1. MST에서 Keyhole 여부 확인 (final_transformed MST의 IsKeyhole 필드)
  2. Keyhole 여부에 따라 DataType 선택:
     - Keyhole: keyhole_final_transformed
     - 일반: final_transformed
  3. displayMinElevationAngle 기준으로 조건부 필터링 (enableDisplayMinElevationFiltering 설정에 따라)
  4. 필터링된 데이터 반환

[4단계: 실제 추적 명령] ✅ 수정된 데이터 사용
moveToStartPosition() → getEphemerisTrackDtlByMstId() [필터링된 적절한 DataType]
sendInitialTrackingData() → getEphemerisTrackDtlByMstId() [필터링된 적절한 DataType]
sendAdditionalTrackingData() → getEphemerisTrackDtlByMstId() [필터링된 적절한 DataType]
calculateDataLength() → getEphemerisTrackDtlByMstId() [필터링된 데이터 길이]
createRealtimeTrackingData() → Keyhole 여부 확인 후 적절한 DataType + 조건부 필터링 + keyhole_final_transformed 데이터 추가
```

---

## Step별 구현 계획

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
3. displayMinElevationAngle 조건부 필터링 추가 (enableDisplayMinElevationFiltering 설정에 따라)
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

**목적**: 실시간 추적 데이터 생성 시 Keyhole 대응 및 조건부 필터링

**파일**: 
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

**수정 위치**: Line 1128-1269

**의존성**: Step 1 완료 필요

**작업 내용**:
1. Keyhole 여부 확인 로직 추가
2. DataType 자동 선택 로직 추가
3. displayMinElevationAngle 조건부 필터링 추가 (enableDisplayMinElevationFiltering 설정에 따라)
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
3. displayMinElevationAngle 조건부 필터링 추가 (enableDisplayMinElevationFiltering 설정에 따라)
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

### Phase 7: Step 6 - SettingsService에 필터링 활성화/비활성화 설정 추가

**목적**: 필터링을 제어할 수 있는 설정 추가

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/system/settings/SettingsService.kt`
**수정 위치**: Line 156 근처 (displayMinElevationAngle 설정 다음)

**수정 내용**:

```kotlin
// === Ephemeris Tracking 설정 ===
"ephemeris.tracking.sourceMinElevationAngle" to SettingDefinition(
    "ephemeris.tracking.sourceMinElevationAngle", 
    -10.0, 
    SettingType.DOUBLE, 
    "원본 2축 위성 추적 데이터 생성 시 최소 Elevation 각도 (도). Orekit 계산 시 사용되는 2축 좌표계 기준. Tilt 각도 보정을 위해 음수 값 허용. 권장 공식: -abs(tiltAngle) - 15도 (예: Tilt -7° → -abs(-7) - 15 = -22.0°). 사용자가 수동으로 계산하여 설정해야 함."
),
"ephemeris.tracking.displayMinElevationAngle" to SettingDefinition(
    "ephemeris.tracking.displayMinElevationAngle", 
    0.0, 
    SettingType.DOUBLE, 
    "3축 변환 및 방위각 제한 완료 후 화면에 표시할 최소 Elevation 각도 (도). 이 값 미만의 데이터는 차트 및 테이블에 표시되지 않음. 백엔드는 모든 데이터를 저장하며, 프론트엔드에서만 필터링됨."
),
// ✅ 새로 추가: 필터링 활성화/비활성화 설정
"ephemeris.tracking.enableDisplayMinElevationFiltering" to SettingDefinition(
    "ephemeris.tracking.enableDisplayMinElevationFiltering",
    false,  // 기본값: 비활성화 (백엔드), 프론트엔드는 true
    SettingType.BOOLEAN,
    "displayMinElevationAngle 기준 필터링 활성화/비활성화. true: 필터링 적용 (displayMinElevationAngle 이상 데이터만 사용), false: 모든 데이터 반환 (sourceMinElevationAngle 기준 전체 데이터). 필터링 비활성화 시에도 하드웨어 제한 각도(elevationMin)는 유지됨."
),
```

**검증 방법**:
- 설정 조회 API에서 새 설정 확인
- 기본값이 `false`인지 확인 (백엔드)
- 설정 변경 시 값이 올바르게 저장되는지 확인

**예상 소요 시간**: 10분

---

### Phase 8: Step 7 - EphemerisService.getAllEphemerisTrackMst() 수정

**목적**: 스케줄 목록 필터링 시 조건부 필터링 적용

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 2350-2355

**의존성**: Phase 7 완료 필요

**작업 내용**:
1. enableDisplayMinElevationFiltering 설정 조회
2. 필터링 활성화 시: displayMinElevationAngle 기준 필터링
3. 필터링 비활성화 시: elevationMin 기준 필터링 (하드웨어 제한 유지)

**예상 소요 시간**: 15분

---

### Phase 9: Step 8 - 프론트엔드 설정 조회 함수 추가

**목적**: 프론트엔드에서 필터링 활성화 여부 조회

**파일**: `ACS/src/services/mode/ephemerisTrackService.ts`
**수정 위치**: `getDisplayMinElevationAngle()` 함수 근처 (Line 826)

**추가할 함수**:

```typescript
/**
 * enableDisplayMinElevationFiltering 설정값 조회
 *
 * SettingsService에서 displayMinElevationAngle 필터링 활성화/비활성화 여부를 조회합니다.
 *
 * @returns enableDisplayMinElevationFiltering 값 (boolean)
 */
async getEnableDisplayMinElevationFiltering(): Promise<boolean> {
  try {
    const response = await api.get('/settings')

    const setting = response.data.find(
      (s: SettingItem) => s.key === 'ephemeris.tracking.enableDisplayMinElevationFiltering',
    )

    const value = setting?.value ? setting.value === 'true' || setting.value === true : true // 기본값: true

    console.log(`⚙️ enableDisplayMinElevationFiltering 설정값: ${value}`)

    return value
  } catch (error) {
    console.error('❌ 설정값 조회 실패, 기본값 true 사용:', error)
    return true // 기본값: 활성화
  }
}
```

**예상 소요 시간**: 10분

---

### Phase 10: Step 9 - 프론트엔드 스토어 수정

**목적**: 프론트엔드에서 필터링 활성화/비활성화 로직 추가

**파일**: `ACS/src/stores/mode/ephemerisTrackStore.ts`
**수정 위치**: 
- Line 142 (displayMinElevation 다음)
- Line 152-154 (filteredDetailData computed)

**수정 내용**:

```typescript
/**
 * 필터링 활성화/비활성화 여부
 * SettingsService.enableDisplayMinElevationFiltering 값
 */
const enableDisplayMinElevationFiltering = ref<boolean>(true) // 기본값: 활성화

/**
 * 화면에 표시할 필터링된 상세 데이터
 * displayMinElevation 기준으로 필터링 (조건부)
 */
const filteredDetailData = computed(() => {
  if (enableDisplayMinElevationFiltering.value) {
    // 필터링 활성화 시: displayMinElevation 기준으로 필터링
    return rawDetailData.value.filter((item) => item.Elevation >= displayMinElevation.value)
  } else {
    // 필터링 비활성화 시: 모든 데이터 반환 (하드웨어 제한 각도는 백엔드에서 처리)
    return rawDetailData.value
  }
})
```

**예상 소요 시간**: 20분

---

### Phase 11: Step 10 - 프론트엔드 설정 UI 추가 (선택사항)

**목적**: 사용자가 필터링 활성화/비활성화를 UI에서 제어

**파일**: `ACS/src/components/settings/system/TrackingSettings.vue`

**추가 내용**:

```vue
<template>
  <div class="tracking-settings">
    <!-- 기존 설정들... -->
    
    <!-- ✅ 필터링 활성화/비활성화 토글 추가 -->
    <q-toggle
      v-model="localSettings.enableDisplayMinElevationFiltering"
      label="DisplayMinElevation 필터링 활성화"
      :loading="loadingStates.tracking"
      hint="true: displayMinElevationAngle 이상 데이터만 사용, false: 전체 데이터 사용 (하드웨어 제한 각도는 유지)"
      @update:model-value="onFilteringToggleChange"
    />
    
    <!-- displayMinElevationAngle 설정 (기존) -->
    <q-input
      v-model.number="localSettings.displayMinElevationAngle"
      label="Display Min Elevation Angle (도)"
      type="number"
      :rules="displayMinElevationRules"
      outlined
      :loading="loadingStates.tracking"
      hint="화면에 표시할 최소 Elevation 각도"
      suffix="°"
      :disable="!localSettings.enableDisplayMinElevationFiltering"
    />
  </div>
</template>
```

**예상 소요 시간**: 20분

---

### Phase 12: Step 7-2 - 프론트엔드 CSV 다운로드 함수 개선

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

## 예상 결과

### 수정 전

- Keyhole 여부와 관계없이 항상 `final_transformed` 사용
- displayMinElevationAngle 필터링 없음 (또는 하드코딩)
- 백엔드 추적: -20도부터, 프론트엔드 표시: 0도부터 → 데이터 불일치
- CSV 파일에 keyhole_final_transformed 데이터 없음
- 필터링 제어 불가

### 수정 후

- Keyhole 여부에 따라 적절한 DataType 자동 선택
- displayMinElevationAngle 기준으로 조건부 필터링 (enableDisplayMinElevationFiltering 설정에 따라)
- 백엔드 추적 = 프론트엔드 표시 (동일한 필터링 기준)
- 실제 추적 명령과 프론트엔드 표시 데이터 일치
- CSV 파일에 keyhole_final_transformed 데이터 포함 (Keyhole 발생 시)
- 필터링 활성화/비활성화 동적 제어 가능
- 예외 처리 추가 (MST 없음, 데이터 없음, 필터링 결과 없음)

---

## 리스크 및 대응 방안

### 리스크 1: 필터링 후 데이터가 비어있을 경우

**대응**: 
- 빈 데이터 반환 + 경고 로그
- `sendHeaderTrackingData()`에서 데이터 길이 0인 경우 추적 시작 중단

### 리스크 2: Keyhole 판단 시 MST 없음

**대응**: 
- 빈 리스트 반환 + 경고 로그
- 추적 시작 불가 (정상 동작)

### 리스크 3: Keyhole 발생 시 keyhole_final_transformed 데이터 없음

**대응**: 
- `final_transformed`로 폴백 + 경고 로그
- 추적은 가능하지만 최적화되지 않은 데이터 사용

### 리스크 4: 필터링된 데이터 인덱스 계산 오류

**대응**: 
- 시간 기준으로 가장 가까운 데이터 찾기
- 시간 정보가 없으면 원본 방식 사용

### 리스크 5: 프론트엔드와 백엔드 데이터 불일치

**대응**: 
- 현재 구조 유지 (백엔드 추적 명령은 필터링된 데이터, 프론트엔드 표시는 computed로 필터링)
- 둘 다 동일한 `displayMinElevationAngle` 기준 사용
- 필터링 활성화/비활성화 상태 동기화

### 리스크 6: 음수 Elevation 데이터로 인한 추적 오류

**대응**: 
- 필터링 비활성화 시에도 하드웨어 제한 각도(`elevationMin`)는 유지
- 음수 Elevation 데이터는 경고 로그만 출력하고 실제 명령에는 포함하지 않음

### 리스크 7: 데이터 길이 불일치

**대응**: 
- `calculateDataLength()`도 `getEphemerisTrackDtlByMstId()`를 사용하도록 통일
- 필터링 활성화/비활성화 상태를 동일하게 적용

### 리스크 8: 추적 중 설정 변경

**대응**: 
- 추적 중에는 설정 변경을 제한하거나 추적 재시작 필요

---

## 통합 테스트 체크리스트

### Phase 2 (Step 1) 완료 후 테스트

**목적**: 핵심 함수 수정 후 기본 동작 확인

**테스트 항목**:
- [ ] `getEphemerisTrackDtlByMstId()` 함수 호출 시 정상 동작
- [ ] Keyhole 미발생 경우: `final_transformed` 데이터 반환
- [ ] Keyhole 발생 경우: `keyhole_final_transformed` 데이터 반환 (데이터 있을 경우)
- [ ] Keyhole 발생 경우 데이터 없으면: `final_transformed`로 폴백
- [ ] 필터링 활성화 시: displayMinElevationAngle 이상 데이터만 반환 확인
- [ ] 필터링 비활성화 시: elevationMin 이상 데이터만 반환 확인 (하드웨어 제한 유지)
- [ ] 로그 메시지 확인

### Phase 3-6 완료 후 통합 테스트

**목적**: 모든 백엔드 수정 완료 후 전체 기능 테스트

**테스트 항목**:
- [ ] 스케줄 선택 시 올바른 데이터 반환
- [ ] 실시간 추적 데이터 생성 정상 동작
- [ ] 추적 시작 시 헤더 데이터 정상 전송
- [ ] 초기 추적 데이터 정상 전송
- [ ] 이론치 다운로드 CSV 정상 생성
- [ ] Keyhole 발생 시 올바른 데이터 사용
- [ ] 필터링 활성화/비활성화 시 올바른 동작

### Phase 7-12 완료 후 전체 테스트

**목적**: 프론트엔드까지 모든 수정 완료 후 최종 테스트

**테스트 항목**:
- [ ] 프론트엔드 차트에 올바른 데이터 표시
- [ ] 프론트엔드 CSV 다운로드 정상 동작
- [ ] Keyhole 발생 시 차트에 올바른 데이터 표시
- [ ] 백엔드와 프론트엔드 데이터 일치 확인
- [ ] 필터링 활성화/비활성화 전환 시 즉시 반영 확인

---

## 예상 전체 소요 시간

- **Phase 0**: 5분
- **Phase 1**: 10분
- **Phase 2**: 30분
- **Phase 3**: 40분
- **Phase 4**: 15분
- **Phase 5**: 25분
- **Phase 6**: 60분
- **Phase 7**: 10분
- **Phase 8**: 15분
- **Phase 9**: 10분
- **Phase 10**: 20분
- **Phase 11**: 20분 (선택사항)
- **Phase 12**: 30분
- **통합 테스트**: 60분

**총 예상 시간**: 약 5-6시간

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

5. **필터링 상태 동기화**
   - 프론트엔드와 백엔드의 필터링 활성화/비활성화 상태가 항상 일치해야 함
   - 설정 변경 시 즉시 반영

6. **하드웨어 제한 각도 유지**
   - 필터링 비활성화 시에도 `elevationMin` (하드웨어 제한 각도)는 항상 유지되어야 함

---

**문서 버전**: 3.0.0 (통합 버전)  
**최종 업데이트**: 2024-12-15

