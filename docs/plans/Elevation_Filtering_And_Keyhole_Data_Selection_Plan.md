# Elevation 필터링 및 Keyhole 데이터 선택 개선 계획

---
**작성일**: 2024-12-05
**작성자**: GTL Systems
**상태**: 진행 중
**관련 이슈**: 
- 2축/3축 변환 시 Elevation 시작각도 불일치 문제
- Keyhole 여부에 따른 데이터 선택 미적용
- 백엔드/프론트엔드 데이터 불일치 (필터링 위치)
---

## 목표

1. **이론치 추출과 실제 추적 데이터 연결**: 이론치 생성과 실제 추적 명령 데이터가 일관되게 연결되도록 개선
2. **Keyhole 대응**: Keyhole 여부에 따라 적절한 DataType (`final_transformed` vs `keyhole_final_transformed`) 자동 선택
3. **백엔드 필터링**: `displayMinElevationAngle` 기준으로 백엔드에서 필터링하여 실제 추적 명령과 프론트엔드 표시 데이터 일치
4. **코드 정리**: 사용되지 않는 변환 함수 제거 또는 주석 처리

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

4. **사용되지 않는 코드**
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
  3. displayMinElevationAngle 기준으로 필터링
  4. 필터링된 데이터 반환

[4단계: 실제 추적 명령] ✅ 수정된 데이터 사용
moveToStartPosition() → getEphemerisTrackDtlByMstId() [필터링된 적절한 DataType]
sendInitialTrackingData() → getEphemerisTrackDtlByMstId() [필터링된 적절한 DataType]
sendAdditionalTrackingData() → getEphemerisTrackDtlByMstId() [필터링된 적절한 DataType]
calculateDataLength() → getEphemerisTrackDtlByMstId() [필터링된 데이터 길이]
createRealtimeTrackingData() → Keyhole 여부 확인 후 적절한 DataType + 필터링 + keyhole_final_transformed 데이터 추가
```

### 잠재적 문제점 및 예외 처리

#### 문제 1: 필터링 후 빈 데이터 처리

**위치**: `getEphemerisTrackDtlByMstId()` 수정 후
**시나리오**: `displayMinElevationAngle`이 너무 높아 필터링 후 데이터가 없는 경우

**현재 처리**:
```kotlin
if (filteredCount == 0 && totalCount > 0) {
    logger.warn("⚠️ 필터링 결과 데이터가 없습니다. displayMinElevationAngle(${displayMinElevation}°)가 너무 높을 수 있습니다.")
}
return filteredData  // 빈 리스트 반환
```

**영향 받는 함수**:
- `moveToStartPosition()`: 빈 리스트면 `passDetails.isEmpty()` 체크로 처리 ✅
- `sendInitialTrackingData()`: 빈 리스트면 `passDetails.isEmpty()` 체크로 처리 ✅
- `sendAdditionalTrackingData()`: 빈 리스트면 `passDetails.isEmpty()` 체크로 처리 ✅
- `calculateDataLength()`: 빈 리스트면 `0` 반환 → 헤더 전송 시 문제 가능 ⚠️

**해결 방안**:
- `calculateDataLength()`에서 빈 데이터일 경우 경고 로그 추가
- `sendHeaderTrackingData()`에서 데이터 길이가 0인 경우 추적 시작 중단

#### 문제 2: Keyhole 판단 시 MST 없음

**위치**: `getEphemerisTrackDtlByMstId()` Line 110-117
**시나리오**: `final_transformed` MST가 없는 경우 (데이터 생성 실패 등)

**현재 처리**:
```kotlin
if (finalMst == null) {
    logger.warn("⚠️ MST ID $mstId에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
    return emptyList()
}
```

**영향**: 모든 추적 함수에서 빈 데이터 반환 → 추적 불가

**해결 방안**: 현재 처리 방식 유지 (빈 리스트 반환 + 경고 로그)

#### 문제 3: Keyhole 발생 시 keyhole_final_transformed 데이터 없음

**위치**: `getEphemerisTrackDtlByMstId()` Line 125-131
**시나리오**: Keyhole로 판단되었지만 `keyhole_final_transformed` 데이터가 없는 경우

**현재 처리**:
```kotlin
val dataType = if (isKeyhole) {
    "keyhole_final_transformed"
} else {
    "final_transformed"
}
// 필터링 시 해당 DataType이 없으면 빈 리스트 반환
```

**영향**: Keyhole로 판단되었지만 데이터가 없어 추적 불가

**해결 방안**:
- Keyhole로 판단되었지만 `keyhole_final_transformed` 데이터가 없으면 `final_transformed`로 폴백
- 경고 로그 추가

#### 문제 4: calculateDataLength()와 getEphemerisTrackDtlByMstId() 길이 불일치

**위치**: `sendHeaderTrackingData()` Line 1635-1642
**시나리오**: 두 함수가 모두 필터링된 데이터를 반환하므로 항상 일치해야 함

**현재 코드**:
```kotlin
val totalLength = calculateDataLength(passId)
val actualDataCount = getEphemerisTrackDtlByMstId(passId).size
if (totalLength != actualDataCount) {
    logger.warn("데이터 길이 불일치: 계산된 길이=${totalLength}, 실제 길이=${actualDataCount}")
}
```

**수정 후**: 두 함수 모두 동일한 필터링 로직 사용 → 항상 일치해야 함 ✅

**해결 방안**: 불일치 경고는 제거하거나 더 구체적인 로그로 변경

#### 문제 5: createRealtimeTrackingData()에서 keyhole_final_transformed 데이터 없음

**위치**: `createRealtimeTrackingData()` Line 403-438
**시나리오**: Keyhole 발생 시 `keyhole_final_transformed` 데이터 조회 시 없을 경우

**현재 처리**:
```kotlin
"keyholeFinalTransformedAzimuth" to if (isKeyhole) {
    val keyholeFinalPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "keyhole_final_transformed")
    val keyholeFinalPoint = if (theoreticalIndex < keyholeFinalPassDetails.size) {
        keyholeFinalPassDetails[theoreticalIndex]
    } else {
        keyholeFinalPassDetails.lastOrNull()
    }
    (keyholeFinalPoint?.get("Azimuth") as? Double)?.toFloat() ?: null
} else null
```

**영향**: Keyhole 발생 시 `keyhole_final_transformed` 데이터가 없으면 `null` 반환 → CSV에서 빈 값 표시

**해결 방안**: 현재 처리 방식 유지 (`null` 반환 + 프론트엔드에서 빈 값 처리)

#### 문제 6: sendInitialTrackingData()에서 필터링된 데이터 인덱스 계산

**위치**: `sendInitialTrackingData()` Line 1703-1728
**시나리오**: 필터링된 데이터를 사용하므로 원본 인덱스와 다를 수 있음

**현재 코드**:
```kotlin
val timeDifferenceMs = Duration.between(startTime, calTime).toMillis()
val calculatedIndex = (timeDifferenceMs / 100).toInt()
val safeStartIndex = when {
    calculatedIndex < 0 -> 0
    calculatedIndex >= totalSize -> maxOf(0, totalSize - 50)
    else -> calculatedIndex
}
```

**영향**: 필터링된 데이터 기준으로 인덱스 계산하면 원본 데이터와 매칭 불가

**해결 방안**: 
- 필터링된 데이터에서 시간 기준으로 가장 가까운 데이터 찾기
- 또는 필터링 전 원본 데이터 기준으로 인덱스 계산 후 필터링된 데이터에서 매칭

#### 문제 7: 프론트엔드와 백엔드 데이터 불일치

**위치**: 프론트엔드 `ephemerisTrackStore.ts`와 백엔드 `getEphemerisTrackDtlByMstId()`
**시나리오**: 
- 백엔드: `getEphemerisTrackDtlByMstId()`에서 필터링된 데이터 반환
- 프론트엔드: `fetchEphemerisDetailData()`에서 전체 데이터 조회 후 필터링

**현재 처리**:
- 백엔드: `/ephemeris/detail/${mstId}` API에서 전체 데이터 반환 (필터링 없음)
- 프론트엔드: `rawDetailData`에 전체 데이터 저장, `filteredDetailData` computed로 필터링

**영향**: 
- 백엔드 추적 명령: 필터링된 데이터 사용
- 프론트엔드 표시: 필터링된 데이터 사용
- **일치함** ✅

**해결 방안**: 현재 구조 유지 (백엔드 추적 명령은 필터링된 데이터, 프론트엔드 표시는 computed로 필터링)

---

## Step 1: getEphemerisTrackDtlByMstId() 수정 - Keyhole 여부 확인 + 필터링 + 예외 처리

**목적**: 실제 추적 명령 데이터를 가져올 때 Keyhole 여부에 따라 적절한 DataType 선택하고, displayMinElevationAngle 기준으로 필터링, 예외 처리 추가

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 2326-2330

### 수정 후 코드 (예외 처리 포함)

```kotlin
/**
 * 특정 마스터 ID에 해당하는 세부 추적 데이터 조회 (실제 추적 명령용)
 * 
 * ✅ Keyhole 여부에 따라 적절한 DataType 자동 선택:
 *    - Keyhole 발생: keyhole_final_transformed (Train≠0, ±270°)
 *    - Keyhole 미발생: final_transformed (Train=0, ±270°)
 * 
 * ✅ displayMinElevationAngle 기준으로 필터링:
 *    - sourceMinElevationAngle = -20도로 넓게 추적했지만
 *    - 실제 추적 명령은 displayMinElevationAngle = 0도 이상만 사용
 *    - 백엔드와 프론트엔드 데이터 일치 보장
 * 
 * ✅ 예외 처리:
 *    - final_transformed MST 없음: 빈 리스트 반환 + 경고 로그
 *    - Keyhole 발생 시 keyhole_final_transformed 데이터 없음: final_transformed로 폴백 + 경고 로그
 *    - 필터링 후 데이터 없음: 빈 리스트 반환 + 경고 로그
 * 
 * @param mstId 마스터 ID
 * @return 필터링된 세부 추적 데이터 리스트 (실제 추적 명령에 사용)
 */
fun getEphemerisTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
    // 1. MST에서 Keyhole 여부 확인
    // final_transformed MST에 IsKeyhole 정보가 저장되어 있음
    val finalMst = ephemerisTrackMstStorage.find { 
        it["No"] == mstId && it["DataType"] == "final_transformed" 
    }
    
    if (finalMst == null) {
        logger.warn("⚠️ MST ID $mstId에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
        return emptyList()
    }
    
    // Keyhole 여부 확인 (final_transformed MST의 IsKeyhole 필드 사용)
    val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
    
    // 2. Keyhole 여부에 따라 DataType 선택
    // Keyhole 발생 시: keyhole_final_transformed (Train≠0으로 재계산된 데이터)
    // Keyhole 미발생 시: final_transformed (Train=0 데이터)
    val dataType = if (isKeyhole) {
        // ✅ Keyhole 발생 시 keyhole_final_transformed 데이터 존재 여부 확인
        val keyholeDataExists = ephemerisTrackDtlStorage.any {
            it["MstId"] == mstId && it["DataType"] == "keyhole_final_transformed"
        }
        
        if (!keyholeDataExists) {
            logger.warn("⚠️ MST ID $mstId: Keyhole로 판단되었으나 keyhole_final_transformed 데이터가 없습니다. final_transformed로 폴백합니다.")
            "final_transformed"  // ✅ 폴백
        } else {
            logger.debug("🔑 MST ID $mstId: Keyhole 발생 → keyhole_final_transformed 사용")
            "keyhole_final_transformed"
        }
    } else {
        logger.debug("✅ MST ID $mstId: Keyhole 미발생 → final_transformed 사용")
        "final_transformed"
    }
    
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
    
    // 필터링 결과 로깅
    val totalCount = ephemerisTrackDtlStorage.count { 
        it["MstId"] == mstId && it["DataType"] == dataType 
    }
    val filteredCount = filteredData.size
    
    logger.info("📊 MST ID $mstId 데이터 조회:")
    logger.info("   - Keyhole 여부: ${if (isKeyhole) "YES" else "NO"}")
    logger.info("   - 사용 DataType: $dataType")
    logger.info("   - 필터 기준: displayMinElevationAngle = ${displayMinElevation}°")
    logger.info("   - 전체 데이터: $totalCount개")
    logger.info("   - 필터링 후: $filteredCount개")
    
    if (filteredCount == 0 && totalCount > 0) {
        logger.warn("⚠️ 필터링 결과 데이터가 없습니다. displayMinElevationAngle(${displayMinElevation}°)가 너무 높을 수 있습니다.")
    }
    
    if (filteredCount == 0) {
        logger.error("❌ MST ID $mstId: 필터링 후 데이터가 없어 추적을 시작할 수 없습니다.")
    }
    
    return filteredData
}
```

---

## Step 2: createRealtimeTrackingData() 수정 - Keyhole 대응 + 필터링 + keyhole_final_transformed 데이터 추가

**목적**: 
1. 실시간 추적 데이터 생성 시 Keyhole 여부에 따라 적절한 DataType 사용하고, displayMinElevationAngle 기준으로 필터링
2. 프론트엔드 이론치 다운로드 CSV 파일에 keyhole_final_transformed 데이터 추가 (Keyhole 발생 시)

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 1128-1269

### 수정 후 코드 (예외 처리 포함)

```kotlin
/**
 * ✅ 실시간 추적 데이터 생성 (개선된 버전 - Keyhole 대응 + 필터링 + keyhole_final_transformed 추가)
 * 
 * Keyhole 여부에 따라 적절한 DataType 사용:
 * - Keyhole 발생: keyhole_final_transformed (Train≠0)
 * - Keyhole 미발생: final_transformed (Train=0)
 * 
 * displayMinElevationAngle 기준으로 필터링:
 * - 실제 추적 명령은 displayMinElevationAngle 이상만 사용
 * 
 * ✅ 예외 처리:
 * - final_transformed MST 없음: 빈 Map 반환
 * - 필터링 후 데이터 없음: 빈 Map 반환
 * - Keyhole 발생 시 keyhole_final_transformed 데이터 없음: null 반환
 * 
 * @param passId 패스 ID (MST ID)
 * @param currentTime 현재 시간
 * @param startTime 추적 시작 시간
 * @return 실시간 추적 데이터 Map
 */
private fun createRealtimeTrackingData(
    passId: UInt,
    currentTime: ZonedDateTime,
    startTime: ZonedDateTime
): Map<String, Any?> {
    val elapsedTimeSeconds = Duration.between(startTime, currentTime).toMillis() / 1000.0f

    // 1. 이론치 데이터 타입별로 분리해서 가져오기
    val originalPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "original")
    val axisTransformedPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "axis_transformed")
    
    if (originalPassDetails.isEmpty()) {
        logger.debug("원본 이론치 데이터가 없어 실시간 데이터 저장을 건너뜁니다.")
        return emptyMap()
    }
    
    // ✅ Keyhole 여부 확인 (final_transformed MST에서)
    val finalMst = ephemerisTrackMstStorage.find { 
        it["No"] == passId && it["DataType"] == "final_transformed" 
    }
    
    if (finalMst == null) {
        logger.warn("⚠️ 패스 ID $passId에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
        return emptyMap()
    }
    
    val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
    
    // ✅ Keyhole 여부에 따라 DataType 선택
    val finalDataType = if (isKeyhole) {
        // ✅ Keyhole 발생 시 keyhole_final_transformed 데이터 존재 여부 확인
        val keyholeDataExists = ephemerisTrackDtlStorage.any {
            it["MstId"] == passId && it["DataType"] == "keyhole_final_transformed"
        }
        
        if (!keyholeDataExists) {
            logger.warn("⚠️ 패스 ID $passId: Keyhole로 판단되었으나 keyhole_final_transformed 데이터가 없습니다. final_transformed로 폴백합니다.")
            "final_transformed"  // ✅ 폴백
        } else {
            logger.debug("🔑 실시간 추적: 패스 ID $passId Keyhole 발생 → keyhole_final_transformed 사용")
            "keyhole_final_transformed"
        }
    } else {
        logger.debug("✅ 실시간 추적: 패스 ID $passId Keyhole 미발생 → final_transformed 사용")
        "final_transformed"
    }
    
    // 선택된 DataType의 데이터 조회
    val finalTransformedPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, finalDataType)
    
    // ✅ displayMinElevationAngle 기준으로 필터링
    val displayMinElevation = settingsService.displayMinElevationAngle
    val filteredFinalTransformed = finalTransformedPassDetails.filter {
        (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
    }
    
    // 필터링된 데이터가 비어있으면 로깅
    if (filteredFinalTransformed.isEmpty()) {
        logger.warn("⚠️ 패스 ID $passId: displayMinElevationAngle(${displayMinElevation}°) 필터링 결과 데이터가 없습니다.")
        return emptyMap()
    }

    // 2. ✅ 시간 기반으로 정확한 이론치 인덱스 계산
    val timeDifferenceMs = Duration.between(startTime, currentTime).toMillis()
    val theoreticalIndex = (timeDifferenceMs / 100.0).toInt().coerceIn(0, originalPassDetails.size - 1)

    // 3. ✅ 해당 인덱스의 실제 이론치 데이터 가져오기 (보간 없이 직접 매칭)
    val theoreticalPoint = if (theoreticalIndex < originalPassDetails.size) {
        originalPassDetails[theoreticalIndex]
    } else {
        originalPassDetails.last()
    }

    val theoreticalAxisPoint = if (theoreticalIndex < axisTransformedPassDetails.size) {
        axisTransformedPassDetails[theoreticalIndex]
    } else {
        axisTransformedPassDetails.last()
    }

    // ✅ 필터링된 final_transformed 데이터에서 인덱스 찾기
    val theoreticalFinalPoint = if (filteredFinalTransformed.isNotEmpty()) {
        val targetTime = theoreticalPoint["Time"] as? ZonedDateTime
        if (targetTime != null) {
            filteredFinalTransformed.minByOrNull { point ->
                val pointTime = point["Time"] as? ZonedDateTime
                if (pointTime != null) {
                    abs(Duration.between(targetTime, pointTime).toMillis())
                } else {
                    Long.MAX_VALUE
                }
            } ?: filteredFinalTransformed.first()
        } else {
            val filteredIndex = (theoreticalIndex * filteredFinalTransformed.size / originalPassDetails.size)
                .coerceIn(0, filteredFinalTransformed.size - 1)
            filteredFinalTransformed[filteredIndex]
        }
    } else {
        emptyMap<String, Any?>()
    }

    // 4. ✅ 정확한 이론치 값 추출 (보간 없이 직접 매칭)
    val originalAzimuth = (theoreticalPoint["Azimuth"] as? Double)?.toFloat() ?: 0.0f
    val originalElevation = (theoreticalPoint["Elevation"] as? Double)?.toFloat() ?: 0.0f
    val originalRange = (theoreticalPoint["Range"] as? Double)?.toFloat() ?: 0.0f
    val originalAltitude = (theoreticalPoint["Altitude"] as? Double)?.toFloat() ?: 0.0f

    val axisTransformedAzimuth = (theoreticalAxisPoint["Azimuth"] as? Double)?.toFloat() ?: originalAzimuth
    val axisTransformedElevation = (theoreticalAxisPoint["Elevation"] as? Double)?.toFloat() ?: originalElevation
    val axisTransformedRange = (theoreticalAxisPoint["Range"] as? Double)?.toFloat() ?: originalRange
    val axisTransformedAltitude = (theoreticalAxisPoint["Altitude"] as? Double)?.toFloat() ?: originalAltitude

    // ✅ 필터링된 final_transformed 데이터에서 값 추출
    val finalTransformedAzimuth = (theoreticalFinalPoint["Azimuth"] as? Double)?.toFloat() ?: axisTransformedAzimuth
    val finalTransformedElevation = (theoreticalFinalPoint["Elevation"] as? Double)?.toFloat() ?: axisTransformedElevation
    val finalTransformedRange = (theoreticalFinalPoint["Range"] as? Double)?.toFloat() ?: axisTransformedRange
    val finalTransformedAltitude = (theoreticalFinalPoint["Altitude"] as? Double)?.toFloat() ?: axisTransformedAltitude

    // ✅ displayMinElevationAngle 필터링 확인
    if (finalTransformedElevation < displayMinElevation) {
        logger.warn("⚠️ 실시간 추적 데이터: Elevation(${finalTransformedElevation}°) < displayMinElevationAngle(${displayMinElevation}°)")
        return emptyMap()
    }

    // ✅ Keyhole Final 변환 데이터 추출 (Keyhole 발생 시만)
    val keyholeFinalTransformedAzimuth = if (isKeyhole) {
        val keyholeFinalPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "keyhole_final_transformed")
        if (keyholeFinalPassDetails.isNotEmpty()) {
            val keyholeFinalPoint = if (theoreticalIndex < keyholeFinalPassDetails.size) {
                keyholeFinalPassDetails[theoreticalIndex]
            } else {
                keyholeFinalPassDetails.lastOrNull()
            }
            (keyholeFinalPoint?.get("Azimuth") as? Double)?.toFloat()
        } else {
            logger.warn("⚠️ 패스 ID $passId: Keyhole 발생 시 keyhole_final_transformed 데이터가 없습니다.")
            null
        }
    } else null
    
    val keyholeFinalTransformedElevation = if (isKeyhole) {
        val keyholeFinalPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "keyhole_final_transformed")
        if (keyholeFinalPassDetails.isNotEmpty()) {
            val keyholeFinalPoint = if (theoreticalIndex < keyholeFinalPassDetails.size) {
                keyholeFinalPassDetails[theoreticalIndex]
            } else {
                keyholeFinalPassDetails.lastOrNull()
            }
            (keyholeFinalPoint?.get("Elevation") as? Double)?.toFloat()
        } else {
            null
        }
    } else null
    
    val keyholeFinalTransformedRange = if (isKeyhole) {
        val keyholeFinalPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "keyhole_final_transformed")
        if (keyholeFinalPassDetails.isNotEmpty()) {
            val keyholeFinalPoint = if (theoreticalIndex < keyholeFinalPassDetails.size) {
                keyholeFinalPassDetails[theoreticalIndex]
            } else {
                keyholeFinalPassDetails.lastOrNull()
            }
            (keyholeFinalPoint?.get("Range") as? Double)?.toFloat()
        } else {
            null
        }
    } else null
    
    val keyholeFinalTransformedAltitude = if (isKeyhole) {
        val keyholeFinalPassDetails = getEphemerisTrackDtlByMstIdAndDataType(passId, "keyhole_final_transformed")
        if (keyholeFinalPassDetails.isNotEmpty()) {
            val keyholeFinalPoint = if (theoreticalIndex < keyholeFinalPassDetails.size) {
                keyholeFinalPassDetails[theoreticalIndex]
            } else {
                keyholeFinalPassDetails.lastOrNull()
            }
            (keyholeFinalPoint?.get("Altitude") as? Double)?.toFloat()
        } else {
            null
        }
    } else null

    // 변환 정보 추출
    val tiltAngle = settingsService.tiltAngle
    val transformationType = theoreticalAxisPoint["TransformationType"] as? String ?: "none"

    // ✅ 변경: PushData 대신 DataStoreService에서 데이터 가져오기
    val currentData = dataStoreService.getLatestData()

    // ✅ DataStoreService에서 추적 관련 데이터만 별도로 가져오기
    val trackingOnlyData = dataStoreService.getTrackingOnlyData()

    val trackingCmdAzimuthTime = trackingOnlyData["trackingAzimuthTime"]
    val trackingCmdElevationTime = trackingOnlyData["trackingElevationTime"]
    val trackingCmdTrainTime = trackingOnlyData["trackingTiltTime"]

    val trackingCmdAzimuth = trackingOnlyData["trackingCMDAzimuthAngle"]
    val trackingActualAzimuth = trackingOnlyData["trackingActualAzimuthAngle"]
    val trackingCmdElevation = trackingOnlyData["trackingCMDElevationAngle"]
    val trackingActualElevation = trackingOnlyData["trackingActualElevationAngle"]
    val trackingCmdTrain = trackingOnlyData["trackingCMDTrainAngle"]
    val trackingActualTrain = trackingOnlyData["trackingActualTrainAngle"]

    // ✅ 데이터 유효성 검사
    val hasValidData =
        trackingCmdAzimuth != null || trackingActualAzimuth != null || trackingCmdElevation != null || trackingActualElevation != null

    if (!hasValidData && trackingDataIndex % 50 == 0) {
        logger.warn("⚠️ DataStoreService에서 유효한 추적 데이터를 받지 못하고 있습니다.")
        debugDataStoreStatus()
    }

    // 실시간 추적 데이터 생성 (원본, 축변환, 최종 변환, keyhole_final_transformed 데이터 모두 포함)
    return mapOf(
        "index" to trackingDataIndex,
        "theoreticalIndex" to theoreticalIndex,
        "timestamp" to currentTime,

        // ✅ 원본 데이터 (변환 전)
        "originalAzimuth" to originalAzimuth,
        "originalElevation" to originalElevation,
        "originalRange" to originalRange,
        "originalAltitude" to originalAltitude,

        // ✅ 축변환 데이터 (기울기 변환 적용)
        "axisTransformedAzimuth" to axisTransformedAzimuth,
        "axisTransformedElevation" to axisTransformedElevation,
        "axisTransformedRange" to axisTransformedRange,
        "axisTransformedAltitude" to axisTransformedAltitude,

        // ✅ 최종 변환 데이터 (±270도 제한 적용, Train=0)
        "finalTransformedAzimuth" to finalTransformedAzimuth,
        "finalTransformedElevation" to finalTransformedElevation,
        "finalTransformedRange" to finalTransformedRange,
        "finalTransformedAltitude" to finalTransformedAltitude,
        
        // ✅ Keyhole Final 변환 데이터 (±270도 제한 적용, Train≠0) [Keyhole 발생 시만]
        "keyholeFinalTransformedAzimuth" to keyholeFinalTransformedAzimuth,
        "keyholeFinalTransformedElevation" to keyholeFinalTransformedElevation,
        "keyholeFinalTransformedRange" to keyholeFinalTransformedRange,
        "keyholeFinalTransformedAltitude" to keyholeFinalTransformedAltitude,

        // ✅ 실제 추적 명령 데이터
        "cmdAz" to finalTransformedAzimuth,
        "cmdEl" to finalTransformedElevation,
        "actualAz" to currentData.azimuthAngle,
        "actualEl" to currentData.elevationAngle,

        "elapsedTimeSeconds" to elapsedTimeSeconds,
        "trackingAzimuthTime" to trackingCmdAzimuthTime,
        "trackingCMDAzimuthAngle" to trackingCmdAzimuth,
        "trackingActualAzimuthAngle" to trackingActualAzimuth,
        "trackingElevationTime" to trackingCmdElevationTime,
        "trackingCMDElevationAngle" to trackingCmdElevation,
        "trackingActualElevationAngle" to trackingActualElevation,
        "trackingTrainTime" to trackingCmdTrainTime,
        "trackingCMDTrainAngle" to trackingCmdTrain,
        "trackingActualTrainAngle" to trackingActualTrain,
        "passId" to passId,

        // ✅ 변환 오차 계산
        "originalToAxisTransformationError" to (axisTransformedAzimuth - originalAzimuth),
        "axisToFinalTransformationError" to (finalTransformedAzimuth - axisTransformedAzimuth),
        "totalTransformationError" to (finalTransformedAzimuth - originalAzimuth),

        // ✅ 실제 추적 오차
        "azimuthError" to ((trackingCmdAzimuth ?: 0.0f) - (trackingActualAzimuth ?: 0.0f)),
        "elevationError" to ((trackingCmdElevation ?: 0.0f) - (trackingActualElevation ?: 0.0f)),

        // ✅ 정확도 분석
        "timeAccuracy" to (elapsedTimeSeconds - (trackingCmdAzimuthTime as? Float ?: 0.0f)),
        "azCmdAccuracy" to (finalTransformedAzimuth - (trackingCmdAzimuth as? Float ?: 0.0f)),
        "azActAccuracy" to ((trackingCmdAzimuth as? Float ?: 0.0f) - (trackingActualAzimuth as? Float ?: 0.0f)),
        "azFinalAccuracy" to (finalTransformedAzimuth - (trackingActualAzimuth as? Float ?: 0.0f)),
        "elCmdAccuracy" to (finalTransformedElevation - (trackingCmdElevation as? Float ?: 0.0f)),
        "elActAccuracy" to ((trackingCmdElevation as? Float ?: 0.0f) - (trackingActualElevation as? Float ?: 0.0f)),
        "elFinalAccuracy" to (finalTransformedElevation - (trackingActualElevation as? Float ?: 0.0f)),

        "hasValidData" to hasValidData,
        "dataSource" to "DataStoreService",

        // ✅ 변환 정보
        "tiltAngle" to tiltAngle,
        "transformationType" to transformationType,
        "isKeyhole" to isKeyhole,
        "finalDataType" to finalDataType,

        // ✅ 변환 적용 여부
        "hasTransformation" to (transformationType != "none"),

        // ✅ 보간 정보
        "interpolationMethod" to "direct_matching",
        "interpolationAccuracy" to 1.0
    )
}
```

---

## Step 3: sendHeaderTrackingData() 수정 - 데이터 길이 검증 개선

**목적**: 필터링된 데이터 길이 검증 개선

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 1634-1642

### 수정 후 코드

```kotlin
// 전체 데이터 길이 검증
val totalLength = calculateDataLength(passId)
val actualDataCount = getEphemerisTrackDtlByMstId(passId).size
logger.info("전체 데이터 길이: ${totalLength}개")
logger.info("실제 데이터 개수: ${actualDataCount}개")

// ✅ 필터링 후 데이터가 없으면 추적 시작 중단
if (actualDataCount == 0) {
    logger.error("❌ 패스 ID $passId: 필터링 후 데이터가 없어 추적을 시작할 수 없습니다.")
    dataStoreService.setEphemerisTracking(false)
    return
}

// ✅ 두 함수 모두 동일한 필터링 로직 사용하므로 항상 일치해야 함
if (totalLength != actualDataCount) {
    logger.warn("⚠️ 데이터 길이 불일치: 계산된 길이=${totalLength}, 실제 길이=${actualDataCount}")
    logger.warn("   이는 예상치 못한 상황입니다. 두 함수가 동일한 필터링 로직을 사용하므로 항상 일치해야 합니다.")
}
```

---

## Step 4: sendInitialTrackingData() 수정 - 필터링된 데이터 인덱스 처리

**목적**: 필터링된 데이터를 사용하므로 시간 기준으로 가장 가까운 데이터 찾기

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 1703-1728

### 수정 후 코드 (시간 기준 매칭)

```kotlin
TimeRangeStatus.IN_RANGE -> {
    logger.info("🎯 현재 시간이 추적 범위 내에 있습니다 - 실시간 추적 모드")

    // ✅ 실시간 추적: 필터링된 데이터에서 현재 시간에 가장 가까운 데이터 찾기
    val timeDifferenceMs = Duration.between(startTime, calTime).toMillis()
    
    // 필터링된 데이터에서 시간 기준으로 가장 가까운 데이터 찾기
    val closestPoint = passDetails.minByOrNull { point ->
        val pointTime = point["Time"] as? ZonedDateTime
        if (pointTime != null) {
            abs(Duration.between(startTime, pointTime).toMillis())
        } else {
            Long.MAX_VALUE
        }
    }
    
    val calculatedIndex = if (closestPoint != null) {
        passDetails.indexOf(closestPoint)
    } else {
        // 시간 정보가 없으면 원본 방식 사용
        (timeDifferenceMs / 100).toInt()
    }

    val totalSize = passDetails.size
    val safeStartIndex = when {
        calculatedIndex < 0 -> 0
        calculatedIndex >= totalSize -> maxOf(0, totalSize - 50)
        else -> calculatedIndex
    }
    val actualCount = minOf(50, totalSize - safeStartIndex)
    val progressPercentage = if (totalSize > 0) {
        (safeStartIndex.toDouble() / totalSize.toDouble()) * 100.0
    } else 0.0

    logger.info(
        "실시간 추적 정보: 진행률=${progressPercentage}%, 인덱스=${safeStartIndex}/${totalSize}, 추출=${actualCount}개"
    )

    initialTrackingData =
        passDetails.drop(safeStartIndex).take(actualCount).mapIndexed { index, point ->
            Triple(
                ((safeStartIndex + index) * 100).toUInt(),
                (point["Elevation"] as Double).toFloat(),
                (point["Azimuth"] as Double).toFloat()
            )
        }
    // ... (나머지 동일)
}
```

---

## Step 5: 이론치 다운로드 CSV 함수 개선 - displayMinElevationAngle 필터링 적용

**목적**: 
1. 이론치 다운로드 CSV 파일에 displayMinElevationAngle 필터링 적용
2. Keyhole 여부에 따라 적절한 DataType 사용 (final_transformed vs keyhole_final_transformed)
3. keyhole_final_transformed 데이터 추가 (Keyhole 발생 시)

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
**수정 위치**: Line 3009-3467 (exportMstDataToCsv 함수)

### 현재 문제점

1. **필터링 없음**: `exportMstDataToCsv()` 함수가 `getEphemerisTrackDtlByMstIdAndDataType()`를 직접 호출하여 필터링 없이 모든 데이터 사용
2. **Keyhole 미대응**: Keyhole 여부와 관계없이 항상 `final_transformed` 데이터 사용
3. **데이터 불일치**: 이론치 다운로드 CSV에는 필터링 전 데이터(-20도부터) 포함, 실제 추적은 필터링된 데이터(0도부터) 사용

### 수정 후 코드

```kotlin
/**
 * ✅ MST 데이터를 CSV 파일로 내보내기 (개선된 버전 - 필터링 + Keyhole 대응)
 * 
 * ✅ displayMinElevationAngle 기준으로 필터링:
 *    - sourceMinElevationAngle = -20도로 넓게 추적했지만
 *    - 이론치 다운로드 CSV에는 displayMinElevationAngle = 0도 이상만 포함
 *    - 실제 추적 명령과 일치하는 데이터 제공
 * 
 * ✅ Keyhole 여부에 따라 적절한 DataType 사용:
 *    - Keyhole 발생: keyhole_final_transformed (Train≠0, ±270°)
 *    - Keyhole 미발생: final_transformed (Train=0, ±270°)
 * 
 * @param mstId 마스터 ID
 * @param outputDirectory 출력 디렉토리
 * @return CSV 파일 생성 결과
 */
fun exportMstDataToCsv(mstId: Int, outputDirectory: String = "csv_exports"): Map<String, Any?> {
    try {
        // ✅ MST 정보 조회 및 Keyhole 여부 확인
        val finalMst = getAllEphemerisTrackMst().find { 
            it["No"] == mstId.toUInt() && it["DataType"] == "final_transformed" 
        }
        
        if (finalMst == null) {
            logger.error("❌ MST ID $mstId에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
            return mapOf<String, Any?>("success" to false, "error" to "MST 데이터를 찾을 수 없습니다")
        }
        
        val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
        
        // ✅ Keyhole 여부에 따라 DataType 선택
        val finalDataType = if (isKeyhole) {
            val keyholeDataExists = ephemerisTrackDtlStorage.any {
                it["MstId"] == mstId.toUInt() && it["DataType"] == "keyhole_final_transformed"
            }
            if (!keyholeDataExists) {
                logger.warn("⚠️ MST ID $mstId: Keyhole로 판단되었으나 keyhole_final_transformed 데이터가 없습니다. final_transformed로 폴백합니다.")
                "final_transformed"
            } else {
                logger.info("🔑 MST ID $mstId: Keyhole 발생 → keyhole_final_transformed 사용")
                "keyhole_final_transformed"
            }
        } else {
            logger.info("✅ MST ID $mstId: Keyhole 미발생 → final_transformed 사용")
            "final_transformed"
        }
        
        // ✅ displayMinElevationAngle 기준으로 필터링
        val displayMinElevation = settingsService.displayMinElevationAngle
        
        // 원본 데이터 조회 (필터링 없음 - 비교용)
        val originalDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "original")
        val axisTransformedDtl = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "axis_transformed")
        
        // ✅ 필터링된 final_transformed 데이터 조회
        val finalTransformedDtlAll = getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "final_transformed")
        val finalTransformedDtl = finalTransformedDtlAll.filter {
            (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
        }
        
        // ✅ 필터링된 keyhole_final_transformed 데이터 조회 (Keyhole 발생 시만)
        val keyholeFinalDtlAll = if (isKeyhole) {
            getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "keyhole_final_transformed")
        } else {
            emptyList()
        }
        val keyholeFinalDtl = if (isKeyhole) {
            keyholeFinalDtlAll.filter {
                (it["Elevation"] as? Double ?: 0.0) >= displayMinElevation
            }
        } else {
            emptyList()
        }
        
        // ✅ Keyhole Axis 데이터 조회 (필터링 없음 - 중간 단계 데이터)
        val keyholeAxisDtl = if (isKeyhole) {
            try {
                getEphemerisTrackDtlByMstIdAndDataType(mstId.toUInt(), "keyhole_axis_transformed")
            } catch (e: Exception) {
                logger.warn("⚠️ Keyhole Axis 데이터 조회 실패: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
        
        // 필터링 결과 로깅
        logger.info("📊 MST ID $mstId CSV 생성:")
        logger.info("   - Keyhole 여부: ${if (isKeyhole) "YES" else "NO"}")
        logger.info("   - 사용 DataType: $finalDataType")
        logger.info("   - 필터 기준: displayMinElevationAngle = ${displayMinElevation}°")
        logger.info("   - Original 데이터: ${originalDtl.size}개")
        logger.info("   - AxisTransformed 데이터: ${axisTransformedDtl.size}개")
        logger.info("   - FinalTransformed 전체: ${finalTransformedDtlAll.size}개")
        logger.info("   - FinalTransformed 필터링 후: ${finalTransformedDtl.size}개")
        if (isKeyhole) {
            logger.info("   - KeyholeFinal 전체: ${keyholeFinalDtlAll.size}개")
            logger.info("   - KeyholeFinal 필터링 후: ${keyholeFinalDtl.size}개")
        }
        
        if (originalDtl.isEmpty()) {
            logger.error("❌ MST ID $mstId 의 원본 데이터를 찾을 수 없습니다")
            return mapOf<String, Any?>("success" to false, "error" to "원본 데이터를 찾을 수 없습니다")
        }
        
        // ✅ 필터링된 데이터가 없으면 경고
        if (finalTransformedDtl.isEmpty()) {
            logger.warn("⚠️ MST ID $mstId: displayMinElevationAngle(${displayMinElevation}°) 필터링 결과 데이터가 없습니다.")
            return mapOf<String, Any?>("success" to false, "error" to "필터링 후 데이터가 없습니다")
        }
        
        val mstInfo = getAllEphemerisTrackMst().find { it["No"] == mstId.toUInt() }
        val satelliteName = mstInfo?.get("SatelliteName") as? String ?: "Unknown"
        val startTime = mstInfo?.get("StartTime") as? java.time.ZonedDateTime
        val endTime = mstInfo?.get("EndTime") as? java.time.ZonedDateTime
        
        // ✅ Train 각도 가져오기 및 포맷팅
        val recommendedTrainAngle = mstInfo?.get("RecommendedTrainAngle") as? Double ?: 0.0
        val trainAngleFormatted = if (recommendedTrainAngle == 0.0) {
            "0"
        } else {
            String.format("%.6f", recommendedTrainAngle)
        }
        
        // ✅ 파일명 개선
        val dateOnly = startTime?.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) ?: "unknown"
        val filename = "MST${mstId}_${satelliteName}_${dateOnly}.csv"
        val filePath = "$outputDirectory/$filename"
        
        // ✅ Train=0 데이터는 필터링된 finalTransformedDtl 사용
        val train0Dtl = finalTransformedDtl.map { point ->
            val az = point["Azimuth"] as Double
            val el = point["Elevation"] as Double
            val time = point["Time"] as java.time.ZonedDateTime
            
            mapOf(
                "Time" to time,
                "Azimuth" to az,
                "Elevation" to el
            )
        }
        logger.info("📊 Train=0 데이터 생성 완료: ${train0Dtl.size}개 (필터링된 finalTransformedDtl 사용)")
        
        // ✅ 필터링된 final_transformed 데이터 기준으로 original과 axis_transformed도 필터링
        // 필터링된 final_transformed 데이터의 시간을 기준으로 매칭
        val filteredFinalTransformedTimes = finalTransformedDtl.map { it["Time"] as? java.time.ZonedDateTime }.toSet()
        
        // ✅ 필터링된 final_transformed의 시간에 해당하는 original과 axis_transformed만 선택
        val filteredOriginalDtl = originalDtl.filter { 
            val time = it["Time"] as? java.time.ZonedDateTime
            time != null && filteredFinalTransformedTimes.contains(time)
        }
        val filteredAxisTransformedDtl = axisTransformedDtl.filter { 
            val time = it["Time"] as? java.time.ZonedDateTime
            time != null && filteredFinalTransformedTimes.contains(time)
        }
        
        // ✅ 필터링된 keyhole_final_transformed의 시간에 해당하는 keyhole_axis_transformed도 필터링
        val filteredKeyholeFinalTransformedTimes = if (isKeyhole) {
            keyholeFinalDtl.map { it["Time"] as? java.time.ZonedDateTime }.toSet()
        } else {
            emptySet()
        }
        val filteredKeyholeAxisDtl = if (isKeyhole) {
            keyholeAxisDtl.filter { 
                val time = it["Time"] as? java.time.ZonedDateTime
                time != null && filteredKeyholeFinalTransformedTimes.contains(time)
            }
        } else {
            emptyList()
        }
        
        logger.info("📊 필터링된 데이터 매칭:")
        logger.info("   - Original 필터링 후: ${filteredOriginalDtl.size}개")
        logger.info("   - AxisTransformed 필터링 후: ${filteredAxisTransformedDtl.size}개")
        logger.info("   - FinalTransformed 필터링 후: ${finalTransformedDtl.size}개")
        if (isKeyhole) {
            logger.info("   - KeyholeAxis 필터링 후: ${filteredKeyholeAxisDtl.size}개")
            logger.info("   - KeyholeFinal 필터링 후: ${keyholeFinalDtl.size}개")
        }
        
        // ✅ 필터링된 데이터 기준으로 CSV 생성
        // 필터링된 데이터 기준으로 최대 크기 계산
        val maxSize = maxOf(
            filteredOriginalDtl.size,
            filteredAxisTransformedDtl.size,
            finalTransformedDtl.size,
            if (isKeyhole) keyholeFinalDtl.size else 0
        )
        
        // ✅ CSV 생성 로직 (필터링된 데이터 사용)
        java.io.FileWriter(filePath).use { writer ->
            // CSV 헤더 작성 (기존과 동일)
            writer.write("Index,Time,")
            writer.write("Original_Azimuth,Original_Elevation,Original_Azimuth_Velocity,Original_Elevation_Velocity,")
            writer.write("Original_Range,Original_Altitude,")
            writer.write("AxisTransformed_Azimuth,AxisTransformed_Elevation,AxisTransformed_Azimuth_Velocity,AxisTransformed_Elevation_Velocity,")
            writer.write("FinalTransformed_train0_Azimuth,FinalTransformed_train0_Elevation,FinalTransformed_train0_Azimuth_Velocity,FinalTransformed_train0_Elevation_Velocity,")
            
            // Keyhole 발생 시만 Keyhole 컬럼 추가
            if (isKeyhole) {
                writer.write("KeyholeAxisTransformed_train${trainAngleFormatted}_Azimuth,KeyholeAxisTransformed_train${trainAngleFormatted}_Elevation,KeyholeAxisTransformed_train${trainAngleFormatted}_Azimuth_Velocity,KeyholeAxisTransformed_train${trainAngleFormatted}_Elevation_Velocity,")
                writer.write("KeyholeFinalTransformed_train${trainAngleFormatted}_Azimuth,KeyholeFinalTransformed_train${trainAngleFormatted}_Elevation,KeyholeFinalTransformed_train${trainAngleFormatted}_Azimuth_Velocity,KeyholeFinalTransformed_train${trainAngleFormatted}_Elevation_Velocity,")
            }
            
            writer.write("Azimuth_Transformation_Error,Elevation_Transformation_Error\n")
            
            // ✅ 필터링된 데이터 기준으로 CSV 데이터 생성
            // 시간 기준으로 매칭하여 인덱스 불일치 방지
            for (i in 0 until maxSize) {
                // ✅ 필터링된 final_transformed 데이터 기준으로 매칭
                val finalTransformedPoint = if (i < finalTransformedDtl.size) finalTransformedDtl[i] else null
                val finalTransformedTime = finalTransformedPoint?.get("Time") as? java.time.ZonedDateTime
                
                // ✅ 시간 기준으로 original과 axis_transformed 매칭
                val originalPoint = if (finalTransformedTime != null) {
                    filteredOriginalDtl.find { it["Time"] == finalTransformedTime }
                } else {
                    if (i < filteredOriginalDtl.size) filteredOriginalDtl[i] else null
                }
                
                val axisTransformedPoint = if (finalTransformedTime != null) {
                    filteredAxisTransformedDtl.find { it["Time"] == finalTransformedTime }
                } else {
                    if (i < filteredAxisTransformedDtl.size) filteredAxisTransformedDtl[i] else null
                }
                
                // ✅ Keyhole 데이터 매칭 (Keyhole 발생 시만)
                val keyholeFinalPoint = if (isKeyhole && finalTransformedTime != null) {
                    keyholeFinalDtl.find { it["Time"] == finalTransformedTime }
                } else {
                    null
                }
                
                val keyholeAxisPoint = if (isKeyhole && finalTransformedTime != null) {
                    filteredKeyholeAxisDtl.find { it["Time"] == finalTransformedTime }
                } else {
                    null
                }
                
                // ... (나머지 CSV 데이터 생성 로직은 동일)
            }
        }
    } catch (e: Exception) {
        logger.error("❌ CSV 파일 생성 중 오류: ${e.message}", e)
        return mapOf<String, Any?>(
            "success" to false,
            "error" to e.message
        )
    }
}
```

### 검증 방법

- 이론치 다운로드 CSV 파일에서 final_transformed Elevation이 displayMinElevationAngle 이상인지 확인
- Keyhole 발생 시 keyhole_final_transformed 데이터 포함 확인
- Keyhole 미발생 시 final_transformed 데이터만 포함 확인
- CSV 파일의 Elevation 시작 값이 displayMinElevationAngle 이상인지 확인

### 예상 결과

#### Keyhole 미발생 경우
- **displayMinElevationAngle = 0도**로 설정 시:
  - CSV 파일의 `FinalTransformed_train0_Elevation` 시작 값: 0도 이상 (예: 0.0도, 3.0도, 7.0도 등)
  - 3축 변환 시 Tilt 각도로 인해 정확히 0도부터 시작하지 않을 수 있음
  - 하지만 displayMinElevationAngle(0도) 이상인 데이터만 포함됨
  - 종료 값: displayMinElevationAngle 이상인 마지막 값

#### Keyhole 발생 경우
- **displayMinElevationAngle = 0도**로 설정 시:
  - CSV 파일의 `FinalTransformed_train0_Elevation`: 0도 이상 (비교용)
  - CSV 파일의 `KeyholeFinalTransformed_train{angle}_Elevation`: 0도 이상 (실제 사용 데이터)
  - Keyhole Final 데이터가 실제 추적에 사용되는 데이터

### 주의 사항

1. **Elevation 시작 값이 정확히 0도가 아닐 수 있음**
   - 3축 변환 시 Tilt 각도(-7도)로 인해 2축 Elevation이 0도여도 3축 변환 후에는 다른 값이 될 수 있음
   - 예: 2축 Elevation = -7도 → 3축 변환 후 Elevation = 0도
   - 예: 2축 Elevation = 0도 → 3축 변환 후 Elevation = 7도 (Tilt 각도에 따라)
   - 따라서 CSV 파일의 Elevation 시작 값은 displayMinElevationAngle 이상이지만, 정확히 0도가 아닐 수 있음

2. **필터링 기준**
   - displayMinElevationAngle = 0도로 설정하면
   - final_transformed Elevation >= 0도인 데이터만 CSV에 포함
   - 실제 추적 명령과 동일한 데이터 제공

---

## ✅ sourceMinElevationAngle = -20도 전략 검토

### 사용자의 전략

**목적**: 3축 변환 후에도 `displayMinElevationAngle = 0도` 이상인 데이터를 확보하기 위해 2축 데이터를 넓게 스캔

**전략**:
- `sourceMinElevationAngle = -20도`: 2축 데이터를 -20도부터 생성 (넓은 범위)
- `displayMinElevationAngle = 0도`: 3축 변환 후 필터링하여 0도 이상만 표시

### 개념 검토 결과

#### ✅ 올바른 개념

**이유**:
1. **3축 변환의 비선형성**
   - 3축 변환 공식은 복잡한 삼각함수 기반 변환 (Line 36-54: CoordinateTransformer.kt)
   - `zFinal = -xRotated * sin(tiltRad) + zRotated * cos(tiltRad)`
   - 여기서 `zRotated = sin(elRad)` (원래 elevation)
   - `xRotated`는 azimuth와 elevation 모두에 의존
   - **단순 덧셈이 아닌 복잡한 변환**이므로, 2축 Elevation = 0도라도 3축 변환 후에는 다른 값이 될 수 있음

2. **Tilt 각도(-7도)의 영향**
   - Tilt 각도가 -7도일 때, 2축 Elevation이 0도여도 3축 변환 후 Elevation은 0도가 아닐 수 있음
   - 예: 2축 Elevation = 0도, Azimuth = 90도 → 3축 변환 후 Elevation ≈ 7도
   - 예: 2축 Elevation = -7도, Azimuth = 90도 → 3축 변환 후 Elevation ≈ 0도
   - **azimuth에 따라 변환 결과가 다름**

3. **넓은 범위 스캔의 필요성**
   - `sourceMinElevationAngle = -20도`로 넓게 스캔하면
   - 다양한 azimuth에 대해 3축 변환 후 Elevation = 0도 이상인 데이터를 확보할 수 있음
   - `displayMinElevationAngle = 0도`로 필터링하면
   - 실제 추적 명령과 동일한 데이터만 제공

#### 📊 예상 변환 결과 (Tilt = -7도 기준)

| 2축 Elevation | Azimuth | 3축 변환 후 Elevation (근사값) |
|--------------|---------|-------------------------------|
| -20도 | 90도 | ≈ -13도 |
| -15도 | 90도 | ≈ -8도 |
| -10도 | 90도 | ≈ -3도 |
| **-7도** | **90도** | **≈ 0도** ✅ |
| 0도 | 90도 | ≈ 7도 |
| 5도 | 90도 | ≈ 12도 |

**결론**:
- `sourceMinElevationAngle = -20도`로 넓게 스캔하면
- 대부분의 azimuth에 대해 3축 변환 후 Elevation = 0도 이상인 데이터를 확보할 수 있음
- `displayMinElevationAngle = 0도`로 필터링하면
- 실제 추적 명령과 동일한 데이터만 제공

#### ⚠️ 주의 사항

1. **sourceMinElevationAngle 수동 설정**
   - **권장 공식**: `sourceMinElevationAngle = -abs(tiltAngle) - 15도`
   - 예: Tilt = -7도 → `sourceMinElevationAngle = -abs(-7) - 15 = -22.0도`
   - 사용자가 직접 계산하여 설정해야 함
   - 자동 계산 로직 없음

2. **Azimuth에 따른 변환 차이**
   - 3축 변환은 azimuth에 따라 변환 결과가 다름
   - 일부 azimuth에서는 `sourceMinElevationAngle = -20도`로도 부족할 수 있음
   - 하지만 대부분의 경우 충분할 것으로 예상

3. **실제 검증 필요**
   - 실제 위성 추적 데이터로 검증 필요
   - 3축 변환 후 Elevation = 0도 이상인 데이터가 충분히 확보되는지 확인

### 최종 결론

**✅ 사용자의 개념은 올바릅니다**

1. **전략이 타당함**
   - `sourceMinElevationAngle = -20도`로 넓게 스캔
   - `displayMinElevationAngle = 0도`로 필터링
   - 실제 추적 명령과 동일한 데이터 제공

2. **수동 설정 권장**
   - **권장 공식**: `sourceMinElevationAngle = -abs(tiltAngle) - 15도`
   - 사용자가 직접 계산하여 설정
   - 자동 계산 로직 없음

3. **실제 검증 필요**
   - 실제 위성 추적 데이터로 검증 필요
   - 3축 변환 후 Elevation = 0도 이상인 데이터가 충분히 확보되는지 확인

---

## Step 7: sourceMinElevationAngle 설정 설명 업데이트

**목적**: `sourceMinElevationAngle` 설정에 권장 공식 정보 추가 (자동 계산 없음, 사용자 수동 설정)

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/system/settings/SettingsService.kt`

### 수정 내용

**설명 문자열만 수정**: 자동 계산 문구 제거, 권장 공식 정보 추가

### 수정 코드

```kotlin
// SettingDefinition (Line 155)
"ephemeris.tracking.sourceMinElevationAngle" to SettingDefinition(
    "ephemeris.tracking.sourceMinElevationAngle", 
    -7.0,  // 기본값
    SettingType.DOUBLE, 
    "원본 2축 위성 추적 데이터 생성 시 최소 Elevation 각도 (도). Orekit 계산 시 사용되는 2축 좌표계 기준. Tilt 각도 보정을 위해 음수 값 허용. 권장 공식: -abs(tiltAngle) - 15도 (예: Tilt -7° → -abs(-7) - 15 = -22.0°). 사용자가 수동으로 계산하여 설정해야 함."
)
```

### 동작 방식

1. **사용자 수동 설정**: 사용자가 `-abs(tiltAngle) - 15도` 공식을 사용하여 직접 계산하여 설정
2. **tiltAngle 변경 시**: 자동 재계산되지 않음, 사용자가 직접 조정 필요
3. **권장 값**: Tilt -7도인 경우 → -abs(-7) - 15 = -22.0도

---

## Step 6: 프론트엔드 차트 데이터 분석 및 개선

**목적**: `EphemerisDesignationPage.vue`에서 표시되는 차트가 Keyhole 여부에 따라 올바른 데이터를 사용하는지 분석 및 개선

**파일**: 
- `ACS/src/pages/mode/EphemerisDesignationPage.vue`
- `ACS/src/stores/mode/ephemerisTrackStore.ts`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

### 현재 데이터 흐름 분석

#### 프론트엔드 차트 데이터 흐름

```
[1단계: 스케줄 선택]
EphemerisDesignationPage.vue.selectSchedule()
  └─ ephemerisStore.selectSchedule(selectedItem)

[2단계: 스토어에서 데이터 로드]
ephemerisTrackStore.ts.selectSchedule()
  └─ fetchEphemerisDetailData(schedule.No)  // 백엔드 API 호출
  └─ rawDetailData.value = allData  // 전체 데이터 저장
  └─ detailData.value = filteredDetailData.value  // 필터링된 데이터 사용

[3단계: 차트 업데이트]
EphemerisDesignationPage.vue
  └─ updateChartWithTrajectory([...ephemerisStore.detailData])
  └─ 차트에 표시되는 데이터: filteredDetailData (displayMinElevation 필터링 적용)
```

#### 백엔드 API 분석

**현재 상태**: `getEphemerisTrackDtlByMstId()` (Line 2326-2330)
```kotlin
fun getEphemerisTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
    return ephemerisTrackDtlStorage.filter {
        it["MstId"] == mstId && it["DataType"] == "final_transformed"
    }
}
```

**문제점**:
- ❌ Keyhole 여부와 관계없이 항상 `final_transformed`만 반환
- ❌ Keyhole 발생 시 `keyhole_final_transformed` 데이터를 반환하지 않음
- ❌ `displayMinElevationAngle` 필터링 없음
- ❌ 차트에 표시되는 데이터가 실제 추적 명령 데이터와 다를 수 있음

### 수정 필요 사항

#### Step 1 완료 시 해결됨

`getEphemerisTrackDtlByMstId()` 함수가 Step 1에서 수정되면:
- ✅ Keyhole 여부에 따라 적절한 DataType 자동 선택
- ✅ `displayMinElevationAngle` 기준으로 필터링
- ✅ 차트에 표시되는 데이터 = 실제 추적 명령 데이터 (일치)

#### 프론트엔드 검증 필요

1. **차트 데이터 검증**
   - `ephemerisStore.detailData`가 Keyhole 여부에 따라 올바른 데이터를 사용하는지 확인
   - 현재는 `filteredDetailData`를 사용하므로 Step 1 완료 시 자동으로 해결됨

2. **데이터 일치 확인**
   - 차트에 표시되는 데이터가 실제 추적 명령 데이터와 일치하는지 확인
   - 백엔드 `getEphemerisTrackDtlByMstId()` 수정 완료 시 자동으로 일치함

### 예상 결과

#### Step 1 완료 후 (수정 후)

**Keyhole 미발생 경우**:
- 백엔드: `final_transformed` 데이터 반환 (displayMinElevationAngle 필터링)
- 프론트엔드: `filteredDetailData`에 `final_transformed` 데이터 저장
- 차트: `final_transformed` 데이터 표시 ✅

**Keyhole 발생 경우**:
- 백엔드: `keyhole_final_transformed` 데이터 반환 (displayMinElevationAngle 필터링)
- 프론트엔드: `filteredDetailData`에 `keyhole_final_transformed` 데이터 저장
- 차트: `keyhole_final_transformed` 데이터 표시 ✅

### 주의 사항

1. **Step 1 우선 완료 필요**
   - `getEphemerisTrackDtlByMstId()` 함수 수정이 완료되어야 차트 데이터가 올바르게 표시됨
   - 현재는 Keyhole 발생 시에도 `final_transformed` 데이터만 표시됨

2. **데이터 일치 확인**
   - Step 1 완료 후 차트 데이터와 실제 추적 명령 데이터가 일치하는지 테스트 필요

---

## Step 7: 프론트엔드 CSV 다운로드 함수 개선

**목적**: 프론트엔드 실시간 추적 데이터 다운로드 CSV 파일에 keyhole_final_transformed 데이터 추가 (Keyhole 발생 시)

**파일**: `ACS/src/pages/mode/EphemerisDesignationPage.vue`
**수정 위치**: Line 974-1120 (downloadCSVWithTransformations 함수)

### 수정 내용

1. CSV 헤더에 `KeyholeFinalTransformed*` 필드 추가
2. CSV 데이터에 keyhole_final_transformed 데이터 추가 (Keyhole 발생 시만)
3. TypeScript 타입 정의에 `keyholeFinalTransformed*` 필드 추가

(이전 Step 2-2 내용과 동일)

---

## 예상 결과

### 수정 전

- Keyhole 여부와 관계없이 항상 `final_transformed` 사용
- displayMinElevationAngle 필터링 없음
- 백엔드 추적: -20도부터, 프론트엔드 표시: 0도부터 → 데이터 불일치
- CSV 파일에 keyhole_final_transformed 데이터 없음

### 수정 후

- Keyhole 여부에 따라 적절한 DataType 자동 선택
- displayMinElevationAngle 기준으로 백엔드에서 필터링
- 백엔드 추적 = 프론트엔드 표시 (동일한 필터링 기준)
- 실제 추적 명령과 프론트엔드 표시 데이터 일치
- CSV 파일에 keyhole_final_transformed 데이터 포함 (Keyhole 발생 시)
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

---

**문서 버전**: 2.0.0  
**최종 업데이트**: 2024-12-05
