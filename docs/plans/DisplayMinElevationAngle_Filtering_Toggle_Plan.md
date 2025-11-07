# DisplayMinElevationAngle 필터링 활성화/비활성화 개선 계획

---
**작성일**: 2024-12-15
**작성자**: GTL Systems
**상태**: 협의 중
**관련 이슈**: 
- displayMinElevationAngle 필터링을 동적으로 제어할 수 없는 문제
- 필터링 비활성화 시 모든 위치를 수정해야 하는 불편함
- 필터링 활성화/비활성화에 따른 데이터 흐름 불일치 가능성
---

## 목표

1. **필터링 제어 설정 추가**: `displayMinElevationAngle` 필터링을 활성화/비활성화할 수 있는 설정 추가
2. **조건부 필터링 적용**: 모든 필터링 위치에 설정 기반 조건부 적용
3. **데이터 일관성 보장**: 필터링 활성화/비활성화 상태에 따른 백엔드/프론트엔드 데이터 일치 보장
4. **안전성 강화**: 필터링 비활성화 시에도 하드웨어 제한 각도는 유지

## 배경

### 현재 문제점

1. **하드코딩된 필터링**
   - `displayMinElevationAngle` 필터링이 여러 위치에서 하드코딩되어 있음
   - 필터링을 비활성화하려면 모든 위치를 수정해야 함
   - 설정 변경이 어려움

2. **필터링 위치 분산**
   - 백엔드: `getEphemerisTrackDtlByMstId()`, `createRealtimeTrackingData()`, `exportMstDataToCsv()`, `getAllEphemerisTrackMst()`
   - 프론트엔드: `ephemerisTrackStore.ts`의 `filteredDetailData` computed
   - 각 위치마다 동일한 필터링 로직이 중복되어 있음

3. **필터링 제어 불가**
   - 특정 상황에서 전체 데이터가 필요한 경우 필터링을 비활성화할 수 없음
   - 데이터 분석 시 필터링된 데이터만 사용 가능

### 현재 데이터 흐름

```
[1단계: 이론치 생성]
OrekitCalculator.generateSatelliteTrackingSchedule()
  └─ sourceMinElevationAngle = -20도로 넓게 추적
  └─ elevation >= sourceMinElevationAngle 필터링으로 2축 데이터 생성

[2단계: 변환 및 저장]
SatelliteTrackingProcessor.processFullTransformation()
  └─ 모든 데이터 저장 (필터링 없음)
  └─ original, axis_transformed, final_transformed, keyhole_final_transformed

[3단계: 실제 추적 명령 데이터 선택] ✅ 필터링 적용 (하드코딩)
getEphemerisTrackDtlByMstId(mstId):
  1. Keyhole 여부 확인
  2. 적절한 DataType 선택
  3. displayMinElevationAngle 기준으로 필터링 ✅ (하드코딩)
  4. 필터링된 데이터 반환

[4단계: 실제 추적 명령] ✅ 필터링된 데이터 사용
moveToStartPosition() → getEphemerisTrackDtlByMstId() [필터링된 데이터]
sendInitialTrackingData() → getEphemerisTrackDtlByMstId() [필터링된 데이터]
sendAdditionalTrackingData() → getEphemerisTrackDtlByMstId() [필터링된 데이터]
calculateDataLength() → getEphemerisTrackDtlByMstId() [필터링된 데이터 길이]

[5단계: 실시간 추적 데이터] ✅ 필터링 적용 (하드코딩)
createRealtimeTrackingData():
  1. displayMinElevationAngle 기준으로 필터링 ✅ (하드코딩)
  2. 필터링된 데이터에서 이론치 매칭
  3. 실시간 추적 데이터 생성

[6단계: 프론트엔드 표시] ✅ 필터링 적용 (하드코딩)
ephemerisTrackStore.filteredDetailData:
  1. rawDetailData에서 displayMinElevationAngle 기준으로 필터링 ✅ (하드코딩)
  2. 차트 및 테이블에 표시
```

---

## 심층 분석: 필터링이 적용되는 모든 위치 및 영향

### 백엔드 - EphemerisService.kt

#### 1. getEphemerisTrackDtlByMstId() - 실제 추적 명령 데이터 조회

**위치**: Line 2541-2610
**용도**: 실제 추적 명령에 사용되는 데이터 반환
**현재 필터링**: Line 2580-2587
```kotlin
val displayMinElevation = settingsService.displayMinElevationAngle
val filteredData = ephemerisTrackDtlStorage.filter {
    it["MstId"] == mstId && 
    it["DataType"] == dataType &&
    (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
}
```

**영향받는 함수들**:
- `moveToStartPosition()` (Line 1741-1751): 시작 위치 이동
- `sendHeaderTrackingData()` (Line 1757-1836): 헤더 전송, Line 1786에서 데이터 길이 검증
- `sendInitialTrackingData()` (Line 1842-1978): 초기 추적 데이터 전송, Line 1849에서 사용
- `sendAdditionalTrackingData()`: 추가 추적 데이터 전송
- `calculateDataLength()`: 데이터 길이 계산

**중요성**: ⚠️ 매우 높음 - 실제 추적 명령에 직접 사용되는 데이터

#### 2. createRealtimeTrackingData() - 실시간 추적 데이터 생성

**위치**: Line 1145-1440
**용도**: 실시간 추적 데이터 생성 및 저장
**현재 필터링**: Line 1195-1205
```kotlin
val displayMinElevation = settingsService.displayMinElevationAngle
val filteredFinalTransformed = finalTransformedPassDetails.filter {
    (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
}
```

**추가 검증**: Line 1264-1268에서 Elevation 재검증
```kotlin
if (finalTransformedElevation < displayMinElevation) {
    logger.warn("⚠️ 실시간 추적 데이터: Elevation(${finalTransformedElevation}°) < displayMinElevationAngle(${displayMinElevation}°)")
    return emptyMap()
}
```

**중요성**: ⚠️ 높음 - 실시간 추적 데이터의 정확성 보장

#### 3. exportMstDataToCsv() - CSV 다운로드

**위치**: Line 3303-3870
**용도**: 이론치 데이터 CSV 파일 생성
**현재 필터링**: 
- Line 3345-3347: final_transformed 필터링
- Line 3356-3358: keyhole_final_transformed 필터링
- Line 3447-3454: original, axis_transformed 시간 기준 매칭 필터링
- Line 3462-3469: keyhole_axis_transformed 시간 기준 매칭 필터링

**중요성**: ⚠️ 중간 - 사용자 분석용 데이터

#### 4. getAllEphemerisTrackMst() - 스케줄 목록 필터링

**위치**: Line 2200-2365
**용도**: 스케줄 목록 반환 (SelectSchedule 화면)
**현재 필터링**: 
- Line 2326: `getEphemerisTrackDtlByMstId()` 호출로 필터링된 데이터 기준 MaxElevation 계산
- Line 2350-2355: 스케줄 목록에서 MaxElevation < displayMinElevationAngle인 스케줄 필터링
```kotlin
val filteredMergedData = mergedData.filter { item ->
    val maxElevation = item["MaxElevation"] as? Double
    maxElevation != null && maxElevation >= displayMinElevation
}
```

**중요성**: ⚠️ 높음 - 프론트엔드 스케줄 목록 표시

### 프론트엔드

#### 1. ephemerisTrackStore.ts - filteredDetailData computed

**위치**: Line 152-154
**용도**: 차트 및 테이블에 표시할 필터링된 데이터
**현재 필터링**:
```typescript
const filteredDetailData = computed(() => {
  return rawDetailData.value.filter((item) => item.Elevation >= displayMinElevation.value)
})
```

**중요성**: ⚠️ 높음 - 프론트엔드 UI 표시

---

## 잠재적 문제점 및 위험 분석

### 위험 1: 음수 Elevation 데이터로 인한 추적 오류

**위치**: 모든 추적 명령 함수
**시나리오**: 필터링 비활성화 시 음수 Elevation 데이터가 추적 명령에 포함됨
**영향**: 
- 안테나가 물리적으로 불가능한 각도로 이동 시도
- 하드웨어 오류 또는 손상 가능성
**대응 방안**:
- 필터링 비활성화 시에도 하드웨어 제한 각도(`anglelimits.elevationMin`)는 유지
- 음수 Elevation 데이터는 경고 로그만 출력하고 실제 명령에는 포함하지 않음
- 또는 필터링 비활성화 시에도 `elevationMin` 이상만 사용하도록 별도 검증 추가

### 위험 2: 데이터 길이 불일치

**위치**: `sendHeaderTrackingData()` Line 1785-1801
**시나리오**: 
- `calculateDataLength()`와 `getEphemerisTrackDtlByMstId()`가 서로 다른 필터링 로직 사용 시
- 필터링 활성화/비활성화 전환 시 데이터 길이 불일치
**영향**: 추적 시작 실패 또는 데이터 전송 오류
**대응 방안**:
- `calculateDataLength()`도 `getEphemerisTrackDtlByMstId()`를 사용하도록 통일
- 필터링 활성화/비활성화 상태를 동일하게 적용

### 위험 3: 인덱스 계산 오류

**위치**: `sendInitialTrackingData()` Line 1860-1902
**시나리오**: 필터링된 데이터와 원본 데이터의 인덱스 불일치
**영향**: 잘못된 추적 위치로 이동
**대응 방안**: 
- 시간 기준으로 매칭하는 로직은 필터링 여부와 관계없이 동일하게 작동
- 필터링 비활성화 시에도 시간 기준 매칭 유지

### 위험 4: 프론트엔드와 백엔드 데이터 불일치

**위치**: 프론트엔드 차트 표시 vs 백엔드 추적 명령
**시나리오**: 필터링 활성화/비활성화 상태가 프론트엔드와 백엔드에서 다를 경우
**영향**: 차트에 표시된 데이터와 실제 추적 명령 데이터 불일치
**대응 방안**:
- 프론트엔드에서 필터링 활성화 여부를 백엔드와 동기화
- 설정 변경 시 즉시 반영

### 위험 5: CSV 다운로드 데이터 불일치

**위치**: `exportMstDataToCsv()`
**시나리오**: 필터링 비활성화 시 CSV에 음수 Elevation 데이터 포함
**영향**: 사용자 혼란, 분석 오류
**대응 방안**:
- CSV 파일에 필터링 상태 명시
- 필터링 비활성화 시에도 경고 메시지 포함

---

## Step 1: SettingsService에 필터링 활성화/비활성화 설정 추가

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
    true,  // 기본값: 활성화
    SettingType.BOOLEAN,
    "displayMinElevationAngle 기준 필터링 활성화/비활성화. true: 필터링 적용 (displayMinElevationAngle 이상 데이터만 사용), false: 모든 데이터 반환 (sourceMinElevationAngle 기준 전체 데이터). 필터링 비활성화 시에도 하드웨어 제한 각도(elevationMin)는 유지됨."
),
"ephemeris.tracking.keyholeAzimuthVelocityThreshold" to SettingDefinition(
    "ephemeris.tracking.keyholeAzimuthVelocityThreshold", 
    10.0, 
    SettingType.DOUBLE, 
    "KEYHOLE 위성 판단을 위한 Azimuth 각속도 임계값 (도/초). 전체 추적 구간에서 최대 Azimuth 각속도가 이 값 이상이면 KEYHOLE 위성으로 판단. KEYHOLE 위성은 Train 각도를 적용하여 ±270° 영역을 회피함. 권장값: 3.0~10.0 (낮을수록 보수적)."
),
```

**검증 방법**:
- 설정 조회 API에서 새 설정 확인
- 기본값이 `true`인지 확인
- 설정 변경 시 값이 올바르게 저장되는지 확인

---

## Step 2: EphemerisService.getEphemerisTrackDtlByMstId() 수정

**목적**: 필터링 활성화/비활성화 조건 추가

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 2577-2607

**수정 전 코드**:
```kotlin
// 3. displayMinElevationAngle 기준으로 필터링
// sourceMinElevationAngle = -20도로 넓게 추적했지만
// 실제 추적 명령은 displayMinElevationAngle = 0도 이상만 사용
val displayMinElevation = settingsService.displayMinElevationAngle

// 선택된 DataType의 데이터 조회 및 필터링
val filteredData = ephemerisTrackDtlStorage.filter {
    it["MstId"] == mstId && 
    it["DataType"] == dataType &&
    (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
}
```

**수정 후 코드**:
```kotlin
// 3. displayMinElevationAngle 기준으로 필터링 (조건부)
// sourceMinElevationAngle = -20도로 넓게 추적했지만
// 실제 추적 명령은 displayMinElevationAngle = 0도 이상만 사용 (필터링 활성화 시)
val enableFiltering = settingsService.enableDisplayMinElevationFiltering
val displayMinElevation = settingsService.displayMinElevationAngle

// 선택된 DataType의 데이터 조회
val allData = ephemerisTrackDtlStorage.filter {
    it["MstId"] == mstId && it["DataType"] == dataType
}

// 필터링 활성화 여부에 따라 조건부 필터링
val filteredData = if (enableFiltering) {
    allData.filter {
        (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
    }
} else {
    // 필터링 비활성화 시에도 하드웨어 제한 각도는 유지
    val elevationMin = settingsService.elevationMin
    allData.filter {
        (it["Elevation"] as? Double ?: 0.0) >= elevationMin
    }
}
```

**로그 수정**:
```kotlin
logger.info("📊 MST ID ${mstId} 데이터 조회:")
logger.info("   - Keyhole 여부: ${if (isKeyhole) "YES" else "NO"}")
logger.info("   - 사용 DataType: ${dataType}")
logger.info("   - 필터링 활성화: ${if (enableFiltering) "YES" else "NO"}")
if (enableFiltering) {
    logger.info("   - 필터 기준: displayMinElevationAngle = ${displayMinElevation}°")
} else {
    logger.info("   - 필터 기준: elevationMin (하드웨어 제한) = ${settingsService.elevationMin}°")
}
logger.info("   - 전체 데이터: ${totalCount}개")
logger.info("   - 필터링 후: ${filteredCount}개")
```

**검증 방법**:
- 필터링 활성화 시: displayMinElevationAngle 이상 데이터만 반환 확인
- 필터링 비활성화 시: elevationMin 이상 데이터만 반환 확인 (하드웨어 제한 유지)
- 로그에서 필터링 상태 확인

---

## Step 3: EphemerisService.createRealtimeTrackingData() 수정

**목적**: 실시간 추적 데이터 생성 시 필터링 활성화/비활성화 조건 추가

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 1195-1205, Line 1264-1268

**수정 전 코드**:
```kotlin
// ✅ displayMinElevationAngle 기준으로 필터링
val displayMinElevation = settingsService.displayMinElevationAngle
val filteredFinalTransformed = finalTransformedPassDetails.filter {
    (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
}

// 필터링된 데이터가 비어있으면 로깅
if (filteredFinalTransformed.isEmpty()) {
    logger.warn("⚠️ 패스 ID ${passId}: displayMinElevationAngle(${displayMinElevation}°) 필터링 결과 데이터가 없습니다.")
    return emptyMap()
}
```

**수정 후 코드**:
```kotlin
// ✅ displayMinElevationAngle 기준으로 필터링 (조건부)
val enableFiltering = settingsService.enableDisplayMinElevationFiltering
val displayMinElevation = settingsService.displayMinElevationAngle

val filteredFinalTransformed = if (enableFiltering) {
    finalTransformedPassDetails.filter {
        (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
    }
} else {
    // 필터링 비활성화 시에도 하드웨어 제한 각도는 유지
    val elevationMin = settingsService.elevationMin
    finalTransformedPassDetails.filter {
        (it["Elevation"] as? Double ?: 0.0) >= elevationMin
    }
}

// 필터링된 데이터가 비어있으면 로깅
if (filteredFinalTransformed.isEmpty()) {
    val filterThreshold = if (enableFiltering) displayMinElevation else settingsService.elevationMin
    logger.warn("⚠️ 패스 ID ${passId}: 필터링 결과 데이터가 없습니다. (기준: ${filterThreshold}°)")
    return emptyMap()
}
```

**추가 검증 수정** (Line 1264-1268):
```kotlin
// ✅ 필터링 기준 확인 (조건부)
val filterThreshold = if (enableFiltering) {
    displayMinElevation
} else {
    settingsService.elevationMin
}

if (finalTransformedElevation < filterThreshold) {
    logger.warn("⚠️ 실시간 추적 데이터: Elevation(${finalTransformedElevation}°) < 필터 기준(${filterThreshold}°)")
    return emptyMap()
}
```

**검증 방법**:
- 필터링 활성화 시: displayMinElevationAngle 이상 데이터만 사용 확인
- 필터링 비활성화 시: elevationMin 이상 데이터만 사용 확인
- 실시간 추적 데이터 생성 시 필터링 상태 확인

---

## Step 4: EphemerisService.exportMstDataToCsv() 수정

**목적**: CSV 다운로드 시 필터링 활성화/비활성화 조건 추가

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 3336-3361, Line 3447-3469

**수정 내용**:

```kotlin
// ✅ displayMinElevationAngle 기준으로 필터링 (조건부)
val enableFiltering = settingsService.enableDisplayMinElevationFiltering
val displayMinElevation = settingsService.displayMinElevationAngle

// 원본 데이터 조회 (필터링 없음 - 비교용)
val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "original")
val axisTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "axis_transformed")

// ✅ 필터링된 final_transformed 데이터 조회 (조건부)
val finalTransformedDtlAll = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "final_transformed")
val finalTransformedDtl = if (enableFiltering) {
    finalTransformedDtlAll.filter {
        (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
    }
} else {
    // 필터링 비활성화 시에도 하드웨어 제한 각도는 유지
    val elevationMin = settingsService.elevationMin
    finalTransformedDtlAll.filter {
        (it["Elevation"] as? Double ?: 0.0) >= elevationMin
    }
}

// ✅ 필터링된 keyhole_final_transformed 데이터 조회 (Keyhole 발생 시만, 조건부)
val keyholeFinalDtlAll = if (isKeyhole) {
    getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "keyhole_final_transformed")
} else {
    emptyList()
}
val keyholeFinalDtl = if (isKeyhole) {
    if (enableFiltering) {
        keyholeFinalDtlAll.filter {
            (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
        }
    } else {
        // 필터링 비활성화 시에도 하드웨어 제한 각도는 유지
        val elevationMin = settingsService.elevationMin
        keyholeFinalDtlAll.filter {
            (it["Elevation"] as? Double ?: 0.0) >= elevationMin
        }
    }
} else {
    emptyList()
}
```

**로그 수정**:
```kotlin
logger.info("📊 MST ID ${mstId} CSV 생성:")
logger.info("   - Keyhole 여부: ${if (isKeyhole) "YES" else "NO"}")
logger.info("   - 사용 DataType: ${finalDataType}")
logger.info("   - 필터링 활성화: ${if (enableFiltering) "YES" else "NO"}")
if (enableFiltering) {
    logger.info("   - 필터 기준: displayMinElevationAngle = ${displayMinElevation}°")
} else {
    logger.info("   - 필터 기준: elevationMin (하드웨어 제한) = ${settingsService.elevationMin}°")
}
```

**검증 방법**:
- 필터링 활성화 시: CSV에 displayMinElevationAngle 이상 데이터만 포함 확인
- 필터링 비활성화 시: CSV에 elevationMin 이상 데이터만 포함 확인
- CSV 파일에 필터링 상태 정보 포함 확인

---

## Step 5: EphemerisService.getAllEphemerisTrackMst() 수정

**목적**: 스케줄 목록 필터링 시 필터링 활성화/비활성화 조건 추가

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 2350-2355

**수정 전 코드**:
```kotlin
// ✅ Step 2: Select Schedule 목록에서 MaxElevation < displayMinElevationAngle인 스케줄 필터링
val displayMinElevation = settingsService.displayMinElevationAngle
val filteredMergedData = mergedData.filter { item ->
    val maxElevation = item["MaxElevation"] as? Double
    maxElevation != null && maxElevation >= displayMinElevation
}
```

**수정 후 코드**:
```kotlin
// ✅ Step 2: Select Schedule 목록에서 스케줄 필터링 (조건부)
val enableFiltering = settingsService.enableDisplayMinElevationFiltering
val displayMinElevation = settingsService.displayMinElevationAngle

val filteredMergedData = if (enableFiltering) {
    // 필터링 활성화 시: displayMinElevationAngle 기준으로 필터링
    mergedData.filter { item ->
        val maxElevation = item["MaxElevation"] as? Double
        maxElevation != null && maxElevation >= displayMinElevation
    }
} else {
    // 필터링 비활성화 시: 모든 스케줄 반환 (하드웨어 제한 각도는 유지)
    val elevationMin = settingsService.elevationMin
    mergedData.filter { item ->
        val maxElevation = item["MaxElevation"] as? Double
        maxElevation != null && maxElevation >= elevationMin
    }
}
```

**로그 수정**:
```kotlin
logger.info("✅ 병합 완료: ${mergedData.size}개 MST 레코드 (KeyholeAxis + KeyholeFinal 데이터 포함)")
if (enableFiltering) {
    logger.info("✅ 필터링 완료: ${mergedData.size}개 → ${filteredMergedData.size}개 (displayMinElevationAngle=${displayMinElevation}° 기준)")
} else {
    logger.info("✅ 필터링 완료: ${mergedData.size}개 → ${filteredMergedData.size}개 (elevationMin=${settingsService.elevationMin}° 기준)")
}
```

**검증 방법**:
- 필터링 활성화 시: displayMinElevationAngle 이상 MaxElevation을 가진 스케줄만 반환 확인
- 필터링 비활성화 시: elevationMin 이상 MaxElevation을 가진 스케줄만 반환 확인
- 프론트엔드 스케줄 목록에서 필터링 상태 확인

---

## Step 6: 프론트엔드 설정 조회 함수 추가

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

**검증 방법**:
- 설정 조회 함수 호출 시 올바른 값 반환 확인
- 기본값이 `true`인지 확인

---

## Step 7: 프론트엔드 스토어 수정

**목적**: 프론트엔드에서 필터링 활성화/비활성화 로직 추가

**파일**: `ACS/src/stores/mode/ephemerisTrackStore.ts`
**수정 위치**: 
- Line 142 (displayMinElevation 다음)
- Line 152-154 (filteredDetailData computed)

**수정 내용**:

```typescript
// ===== 새로운 상태: 전체 데이터 저장 및 필터링 =====

/**
 * 전체 스케줄 상세 데이터 (필터링 전)
 * 백엔드에서 받은 모든 데이터 저장 (음수 Elevation 포함)
 */
const rawDetailData = ref<ScheduleDetailItem[]>([])

/**
 * 화면 표시용 최소 Elevation 각도 (도)
 * SettingsService.displayMinElevationAngle 값
 */
const displayMinElevation = ref<number>(0.0)

/**
 * 필터링 활성화/비활성화 여부
 * SettingsService.enableDisplayMinElevationFiltering 값
 */
const enableDisplayMinElevationFiltering = ref<boolean>(true) // 기본값: 활성화

// ===== 계산된 속성 =====
const hasValidData = computed(() => masterData.value.length > 0)
const isTrackingActive = computed(() => trackingStatus.value === 'active')

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

**selectSchedule() 함수 수정** (Line 620-652):

```typescript
const selectSchedule = async (schedule: ScheduleItem) => {
  selectedSchedule.value = schedule
  currentTrackingPassId.value = schedule.No

  try {
    await ephemerisTrackService.setCurrentTrackingPassId(schedule.No)

    // 1. 백엔드에서 전체 데이터 조회 (필터링 없음)
    const allData = await ephemerisTrackService.fetchEphemerisDetailData(schedule.No)

    // 2. 전체 데이터 저장
    rawDetailData.value = allData

    // 3. displayMinElevation 설정값 조회 및 저장
    displayMinElevation.value = await ephemerisTrackService.getDisplayMinElevationAngle()

    // ✅ 4. 필터링 활성화 여부 조회 및 저장
    enableDisplayMinElevationFiltering.value = await ephemerisTrackService.getEnableDisplayMinElevationFiltering()

    // 5. 기존 detailData도 업데이트 (호환성 유지)
    detailData.value = filteredDetailData.value

    console.log(`✅ 스케줄 데이터 로드 완료:
      - 전체 데이터: ${rawDetailData.value.length}개
      - 표시 데이터: ${filteredDetailData.value.length}개
      - 필터링 활성화: ${enableDisplayMinElevationFiltering.value ? 'YES' : 'NO'}
      - 필터 기준: ${displayMinElevation.value}°
      - KEYHOLE: ${schedule.IsKeyhole ? 'YES' : 'NO'}
      - Train 각도: ${schedule.RecommendedTrainAngle}°
    `)

    return filteredDetailData.value
  } catch (err) {
    error.value = 'Failed to select schedule'
    throw err
  }
}
```

**updateDisplayMinElevation() 함수 수정** (Line 660-667):

```typescript
/**
 * displayMinElevation 설정값 업데이트
 * 설정 변경 시 호출하여 즉시 필터링 반영
 *
 * @param newValue 새로운 최소 Elevation 값 (도)
 */
const updateDisplayMinElevation = (newValue: number) => {
  displayMinElevation.value = newValue
  // 기존 detailData도 업데이트 (호환성 유지)
  detailData.value = filteredDetailData.value
  console.log(
    `🔄 표시 필터 업데이트: ${newValue}° (표시 데이터: ${filteredDetailData.value.length}개)`,
  )
}

/**
 * 필터링 활성화/비활성화 업데이트
 * 설정 변경 시 호출하여 즉시 필터링 반영
 *
 * @param newValue 필터링 활성화 여부
 */
const updateEnableDisplayMinElevationFiltering = (newValue: boolean) => {
  enableDisplayMinElevationFiltering.value = newValue
  // 기존 detailData도 업데이트 (호환성 유지)
  detailData.value = filteredDetailData.value
  console.log(
    `🔄 필터링 상태 업데이트: ${newValue ? '활성화' : '비활성화'} (표시 데이터: ${filteredDetailData.value.length}개)`,
  )
}
```

**return 문 수정** (Line 969-1025):

```typescript
return {
  // ... 기존 상태들 ...

  // ✅ 새로운 필터링 관련 상태
  rawDetailData: readonly(rawDetailData),
  displayMinElevation: readonly(displayMinElevation),
  enableDisplayMinElevationFiltering: readonly(enableDisplayMinElevationFiltering),

  // ... 기존 계산된 속성들 ...
  filteredDetailData, // 필터링된 데이터

  // ... 기존 액션들 ...

  // ✅ 새로운 필터링 관련 액션
  updateDisplayMinElevation,
  updateEnableDisplayMinElevationFiltering,
}
```

**검증 방법**:
- 필터링 활성화 시: displayMinElevation 기준으로 필터링 확인
- 필터링 비활성화 시: 전체 데이터 반환 확인
- 설정 변경 시 즉시 반영 확인

---

## Step 8: 프론트엔드 설정 UI 추가 (선택사항)

**목적**: 사용자가 필터링 활성화/비활성화를 UI에서 제어

**파일**: `ACS/src/components/settings/system/TrackingSettings.vue` 또는 적절한 설정 컴포넌트

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

<script setup lang="ts">
// ... 기존 코드 ...

const onFilteringToggleChange = (value: boolean) => {
  // 필터링 상태 변경 시 즉시 반영
  if (ephemerisStore.selectedSchedule) {
    ephemerisStore.updateEnableDisplayMinElevationFiltering(value)
  }
}
</script>
```

**검증 방법**:
- 토글 스위치로 필터링 활성화/비활성화 확인
- 설정 저장 후 반영 확인
- displayMinElevationAngle 입력 필드가 필터링 비활성화 시 비활성화되는지 확인

---

## 예상 결과

### 필터링 활성화 시 (기본값)

- **기존 동작과 동일**: displayMinElevationAngle 이상 데이터만 사용
- **백엔드 추적 명령**: 필터링된 데이터 (displayMinElevationAngle 기준)
- **프론트엔드 표시**: 필터링된 데이터 (displayMinElevationAngle 기준)
- **CSV 다운로드**: 필터링된 데이터 (displayMinElevationAngle 기준)
- **스케줄 목록**: 필터링된 스케줄만 표시 (displayMinElevationAngle 기준)

### 필터링 비활성화 시

- **모든 데이터 사용**: sourceMinElevationAngle 기준으로 생성된 전체 데이터
- **백엔드 추적 명령**: 전체 데이터 (하드웨어 제한 각도는 유지)
- **프론트엔드 표시**: 전체 데이터
- **CSV 다운로드**: 전체 데이터 (하드웨어 제한 각도는 유지)
- **스케줄 목록**: 모든 스케줄 표시 (하드웨어 제한 각도는 유지)

---

## 주의사항

1. **하드웨어 제한 각도 유지**: 필터링 비활성화 시에도 `elevationMin` (하드웨어 제한 각도)는 항상 유지되어야 함
2. **설정 동기화**: 프론트엔드와 백엔드의 필터링 활성화/비활성화 상태가 항상 일치해야 함
3. **로그 명확성**: 모든 로그에 필터링 활성화/비활성화 상태를 명확히 표시
4. **데이터 일관성**: 필터링 활성화/비활성화 전환 시 데이터 일관성 유지
5. **추적 중 설정 변경**: 추적 중에는 설정 변경을 제한하거나 추적 재시작 필요

---

## 검증 방법

### 백엔드 검증

1. **설정 추가 확인**
   - SettingsService에 `enableDisplayMinElevationFiltering` 설정 추가 확인
   - 기본값이 `true`인지 확인

2. **필터링 활성화 시**
   - `getEphemerisTrackDtlByMstId()`: displayMinElevationAngle 이상 데이터만 반환 확인
   - `createRealtimeTrackingData()`: displayMinElevationAngle 이상 데이터만 사용 확인
   - `exportMstDataToCsv()`: CSV에 displayMinElevationAngle 이상 데이터만 포함 확인
   - `getAllEphemerisTrackMst()`: displayMinElevationAngle 이상 MaxElevation을 가진 스케줄만 반환 확인

3. **필터링 비활성화 시**
   - `getEphemerisTrackDtlByMstId()`: elevationMin 이상 데이터만 반환 확인 (하드웨어 제한 유지)
   - `createRealtimeTrackingData()`: elevationMin 이상 데이터만 사용 확인
   - `exportMstDataToCsv()`: CSV에 elevationMin 이상 데이터만 포함 확인
   - `getAllEphemerisTrackMst()`: elevationMin 이상 MaxElevation을 가진 스케줄만 반환 확인

4. **로그 확인**
   - 모든 로그에 필터링 활성화/비활성화 상태 표시 확인
   - 필터링 기준 각도 명확히 표시 확인

### 프론트엔드 검증

1. **설정 조회 확인**
   - `getEnableDisplayMinElevationFiltering()` 함수가 올바른 값 반환 확인
   - 기본값이 `true`인지 확인

2. **필터링 활성화 시**
   - `filteredDetailData`: displayMinElevation 기준으로 필터링 확인
   - 차트에 필터링된 데이터만 표시 확인
   - 데이터 개수가 필터링된 개수와 일치 확인

3. **필터링 비활성화 시**
   - `filteredDetailData`: 전체 데이터 반환 확인
   - 차트에 전체 데이터 표시 확인
   - 데이터 개수가 전체 개수와 일치 확인

4. **설정 변경 시**
   - 필터링 활성화/비활성화 전환 시 즉시 반영 확인
   - 차트 데이터가 즉시 업데이트되는지 확인

---

## 리스크 및 대응 방안

### 리스크 1: 음수 Elevation 데이터로 인한 추적 오류

**대응**: 필터링 비활성화 시에도 `elevationMin` (하드웨어 제한 각도)는 항상 유지

### 리스크 2: 데이터 길이 불일치

**대응**: `calculateDataLength()`도 `getEphemerisTrackDtlByMstId()`를 사용하도록 통일

### 리스크 3: 인덱스 계산 오류

**대응**: 시간 기준으로 매칭하는 로직은 필터링 여부와 관계없이 동일하게 작동

### 리스크 4: 프론트엔드와 백엔드 데이터 불일치

**대응**: 프론트엔드에서 필터링 활성화 여부를 백엔드와 동기화

### 리스크 5: 추적 중 설정 변경

**대응**: 추적 중에는 설정 변경을 제한하거나 추적 재시작 필요

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2024-12-15


