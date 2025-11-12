# Keyhole Train 각도 관리 통합 개선 계획

---
**작성일**: 2024-12-15  
**작성자**: GTL Systems  
**상태**: 최종 심층 분석 완료  
**관련 이슈**: 
- KEYHOLE=YES인데 Train=0.000000°인 문제
- 위성 추적 시 Train 각도 설정 문제

---

## 목표

Keyhole 위성 추적 시 Train 각도를 올바르게 관리하기 위해 다음 두 가지 문제를 해결합니다:

1. **데이터 변환 단계**: 각 MST는 독립적으로 본인 기준에서 Keyhole을 판단하고 본인 기준에서 `RecommendedTrainAngle`을 계산해야 하며, `finalTransformedMst`에서 Keyhole로 판단되면 해당 MST의 `RecommendedTrainAngle`을 사용하여 Train≠0 재변환을 수행하도록 수정

2. **추적 단계**: 위성 추적 시 Keyhole 위성인 경우 Train 각도를 `RecommendedTrainAngle`로 설정하고, `currentTrackingPass`를 Keyhole 여부에 따라 적절한 MST로 설정하도록 개선

---

## Part 1: 데이터 변환 단계 - Train의 두 가지 용도 구분 및 RecommendedTrainAngle 계산

### 핵심 문제: Train의 두 가지 용도 혼재

#### 문제 발견

**현재 코드에서 혼재되어 사용되는 Train 값**:

1. **3축 변환 시 사용하는 Train**: 항상 0이어야 함 (axis_transformed, final_transformed 생성 시)
2. **Keyhole 발생하면 회전해야 하는 값**: recommendedTrainAngle (MST에 저장되는 값, 나중에 keyhole_* 생성 시 사용)

**현재 코드 (Line 344-407)**:
```kotlin
val recommendedTrainAngle = forcedTrainAngle ?: (mstData["RecommendedTrainAngle"] as? Double ?: 0.0)
// forcedTrainAngle=0.0이면 → recommendedTrainAngle = 0.0 (3축 변환용)

// ... 3축 변환에 사용 ...
trainAngle = recommendedTrainAngle  // ← 3축 변환에 사용 (0.0)

// ... MST 저장 ...
RecommendedTrainAngle = recommendedTrainAngle  // ← MST 저장 ❌ 문제: 3축 변환용 값(0.0)을 MST에 저장
```

**문제점**:
- Line 344의 `recommendedTrainAngle`은 3축 변환용 Train 값 (forcedTrainAngle=0.0이면 0.0)
- Line 362: 3축 변환에 0.0 사용 ✅ (정상)
- Line 407: MST에 저장할 때도 같은 0.0 값 사용 ❌ (문제!)
- **결과**: 
  - Keyhole이 발생해도 RecommendedTrainAngle이 0.0으로 저장됨
  - Train≠0 재계산 시 0.0 값 사용 → Keyhole 처리가 안됨
  - **KEYHOLE=YES인데 Train=0.000000°인 문제 발생**

#### 변수명 분리 도입 이유

**현재 문제 예시**:
```kotlin
// forcedTrainAngle=0.0일 때
val recommendedTrainAngle = 0.0  // ← 3축 변환용 (0.0)

// 3축 변환
trainAngle = 0.0  // ✅ 정상 (Train=0으로 변환)

// MST 저장
RecommendedTrainAngle = 0.0  // ❌ 문제! Keyhole이어도 0.0 저장
```

**변수명 분리 후**:
```kotlin
// forcedTrainAngle=0.0일 때
val trainAngleForTransformation = 0.0  // ← 3축 변환용 (0.0)

// 3축 변환
trainAngle = 0.0  // ✅ 정상

// MST 저장용은 별도로 계산
val recommendedTrainAngleForMst = if (isKeyhole) {
    calculateTrainAngle(maxAzRateAzimuth)  // ← 본인 기준으로 계산 (예: -167.4°)
} else {
    0.0
}

// MST 저장
RecommendedTrainAngle = -167.4°  // ✅ 정상! Keyhole 처리 가능
```

**도입 효과**:
1. ✅ **명확한 용도 구분**: 3축 변환용과 MST 저장용이 명확히 분리
2. ✅ **버그 해결**: Keyhole 발생 시 올바른 RecommendedTrainAngle 저장
3. ✅ **코드 가독성 향상**: 변수명만 봐도 용도 파악 가능
4. ✅ **유지보수 용이**: 나중에 수정 시 실수 방지

---

### Train의 두 가지 용도 명확화

#### 1. 3축 변환용 Train (trainAngleForTransformation)

**용도**: `applyAxisTransformation()`에서 3축 변환에 사용

**값**:
- `forcedTrainAngle = 0.0`: 항상 0.0 (axis_transformed, final_transformed 생성 시)
- `forcedTrainAngle = null`: MST에서 읽은 값 (keyhole_* 생성 시)

**사용 위치**:
- Line 362: `transformCoordinatesWithTrain(trainAngle = recommendedTrainAngle)`
- Line 372: `Train = recommendedTrainAngle` (DTL에 저장)

**변수명 제안**: `trainAngleForTransformation` (명확한 용도 표시)

#### 2. MST 저장용 RecommendedTrainAngle

**용도**: 각 MST에 저장되는 값, Keyhole 발생 시 회전해야 하는 Train 각도

**값**:
- 각 MST는 본인의 데이터로 계산한 RecommendedTrainAngle
- Keyhole이면 본인 기준으로 계산
- Keyhole 아니면 0.0

**사용 위치**:
- Line 407: `RecommendedTrainAngle = recommendedTrainAngle` ← **문제: 3축 변환용 값 사용**
- Line 110: `keyholeOriginalMst` 업데이트 시 사용
- Line 117: `applyAxisTransformation(keyholeOriginalMst)` → MST에서 읽음

**변수명 제안**: `recommendedTrainAngleForMst` (명확한 용도 표시)

---

### 수정 원칙

1. **Train의 두 가지 용도 명확히 구분**
   - `trainAngleForTransformation`: 3축 변환용 Train (forcedTrainAngle 또는 MST에서 읽음)
   - `recommendedTrainAngleForMst`: MST 저장용 RecommendedTrainAngle (본인 기준으로 계산)

2. **각 MST는 독립적으로 본인 기준에서 Keyhole 판단 및 RecommendedTrainAngle 계산**
   - Original MST: 2축 기준
   - AxisTransformed MST: 3축, Train=0, ±270도 제한 없음 기준
   - FinalTransformed MST: 3축, Train=0, ±270도 제한 있음 기준

3. **Train 각도 계산**
   - 각 MST의 `calculateMetrics()`로 계산된 `MaxAzRateAzimuth`를 직접 사용
   - `calculateTrainAngle(metrics["MaxAzRateAzimuth"])` 직접 호출
   - `calculateTrainAngleMethodA()`나 `calculateTrainAngleMethodB()`는 사용하지 않음

4. **Keyhole 판단 및 Train≠0 재계산**
   - `finalTransformedMst`의 `IsKeyhole` 값을 직접 참조 (재판단하지 않음)
   - `finalTransformedMst`의 `RecommendedTrainAngle`을 사용

---

## Part 1 구현 단계

### Step 0: Original MST에서 calculateTrainAngleMethodA/B 제거

**목적**: Original MST에서 불필요한 래퍼 함수 제거하고 직접 계산

**파일**: `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\algorithm\satellitetracker\processor\SatelliteTrackingProcessor.kt`

**수정 위치**: Line 227-278 (`structureOriginalData()` 함수 내부)

**현재 코드**:
```kotlin
// ============================================================
// Train 계산 방법 선택
// 방법 A: calculateTrainAngleMethodA(metrics)  - 2축 기준
// 방법 B: calculateTrainAngleMethodB(passDtl) - 최종 기준
// ============================================================
val currentMethod = "B"  // ← 여기만 변경 (A 또는 B)

// Keyhole인 경우 최적 Train 각도 계산
val recommendedTrainAngle = if (isKeyhole) {
    val trainAngle = if (currentMethod == "A") {
        calculateTrainAngleMethodA(metrics)
    } else {
        calculateTrainAngleMethodB(passDtl)
    }
    // ... 상세 로그 ...
    trainAngle
} else {
    0.0
}
```

**수정 후 코드**:
```kotlin
// Keyhole인 경우 최적 Train 각도 계산 (MST 저장용)
val recommendedTrainAngle = if (isKeyhole) {
    // ✅ 본인의 DTL 데이터로 calculateMetrics() 호출 → MaxAzRateAzimuth 얻기
    // 이미 위에서 calculateMetrics(passDtl) 호출했으므로 metrics 사용
    val maxAzRateAzimuth = metrics["MaxAzRateAzimuth"] as? Double ?: 0.0
    calculateTrainAngle(maxAzRateAzimuth)  // ✅ 직접 호출
} else {
    0.0
}
```

**참고**: 
- `calculateTrainAngleMethodA()`는 단순히 `metrics["MaxAzRateAzimuth"]`를 가져와서 `calculateTrainAngle()` 호출하는 래퍼 함수
- `calculateTrainAngleMethodB()`는 `simulateTrainApplication()`을 호출해서 통계용
- 불필요한 래퍼 함수 없이 직접 계산

**검증 방법**:
- Original MST에서 `calculateTrainAngleMethodA()` 또는 `calculateTrainAngleMethodB()` 사용하지 않는지 확인
- `calculateTrainAngle()` 직접 호출하는지 확인

---

### Step 1: AxisTransformed MST에서 Train의 두 가지 용도 구분

**목적**: 3축 변환용 Train과 MST 저장용 RecommendedTrainAngle을 명확히 구분

**파일**: `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\algorithm\satellitetracker\processor\SatelliteTrackingProcessor.kt`

**수정 위치**: Line 342-412 (`applyAxisTransformation()` 함수 내부)

**현재 코드**:
```kotlin
originalMst.forEach { mstData ->
    val mstId = mstData["No"] as UInt
    val recommendedTrainAngle = forcedTrainAngle ?: (mstData["RecommendedTrainAngle"] as? Double ?: 0.0)

    logger.debug("패스 #$mstId 3축 변환 중 (Train: ${recommendedTrainAngle}°${if (forcedTrainAngle != null) " [강제 적용]" else " [MST에서 읽음]"})")

    // 해당 패스의 상세 데이터 조회
    val passDtl = originalDtl.filter { it["MstId"] == mstId }

    // 각 좌표에 3축 변환 적용
    passDtl.forEachIndexed { index, point ->
        // ...
        val (transformedAz, transformedEl) = CoordinateTransformer.transformCoordinatesWithTrain(
            azimuth = originalAz,
            elevation = originalEl,
            tiltAngle = settingsService.tiltAngle,
            trainAngle = recommendedTrainAngle  // ← 3축 변환용
        )
        // ...
    }

    // ✅ 변환 후 메타데이터 재계산
    val transformedPassDtl = axisTransformedDtl.filter { it["MstId"] == mstId }
    val metrics = calculateMetrics(transformedPassDtl)

    // Keyhole 재분석
    val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
    val threshold = settingsService.keyholeAzimuthVelocityThreshold
    val isKeyhole = maxAzRate >= threshold

    axisTransformedMst.add(
        mapOf(
            // ...
            "RecommendedTrainAngle" to recommendedTrainAngle,  // ← ❌ 문제: 3축 변환용 값 사용
            // ...
        )
    )
}
```

**수정 후 코드**:
```kotlin
originalMst.forEach { mstData ->
    val mstId = mstData["No"] as UInt
    
    // ✅ 3축 변환용 Train (명확한 변수명)
    val trainAngleForTransformation = forcedTrainAngle ?: (mstData["RecommendedTrainAngle"] as? Double ?: 0.0)

    logger.debug("패스 #$mstId 3축 변환 중 (Train: ${trainAngleForTransformation}°${if (forcedTrainAngle != null) " [강제 적용]" else " [MST에서 읽음]"})")

    // 해당 패스의 상세 데이터 조회
    val passDtl = originalDtl.filter { it["MstId"] == mstId }

    // 각 좌표에 3축 변환 적용
    passDtl.forEachIndexed { index, point ->
        // ...
        val (transformedAz, transformedEl) = CoordinateTransformer.transformCoordinatesWithTrain(
            azimuth = originalAz,
            elevation = originalEl,
            tiltAngle = settingsService.tiltAngle,
            trainAngle = trainAngleForTransformation  // ✅ 3축 변환용
        )
        // ...
        axisTransformedDtl.add(
            mapOf(
                // ...
                "Train" to trainAngleForTransformation,  // ✅ 3축 변환용
                // ...
            )
        )
    }

    // ✅ 변환 후 메타데이터 재계산
    val transformedPassDtl = axisTransformedDtl.filter { it["MstId"] == mstId }
    val metrics = calculateMetrics(transformedPassDtl)

    // Keyhole 재분석
    val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
    val threshold = settingsService.keyholeAzimuthVelocityThreshold
    val isKeyhole = maxAzRate >= threshold

    // ✅ MST 저장용 RecommendedTrainAngle (본인 기준으로 계산)
    val recommendedTrainAngleForMst = if (isKeyhole) {
        // 이미 calculateMetrics()로 MaxAzRateAzimuth를 계산했으므로, 이를 사용하여 Train 각도 계산
        val maxAzRateAzimuth = metrics["MaxAzRateAzimuth"] as? Double ?: 0.0
        calculateTrainAngle(maxAzRateAzimuth)  // ✅ 본인 기준으로 계산
    } else {
        0.0
    }

    axisTransformedMst.add(
        mapOf(
            // ...
            "IsKeyhole" to isKeyhole,
            "RecommendedTrainAngle" to recommendedTrainAngleForMst,  // ✅ 본인 기준에서 계산된 값
            // ...
        )
    )
}
```

**핵심 변경사항**:
1. Line 344: `recommendedTrainAngle` → `trainAngleForTransformation` (명확한 용도 표시)
2. Line 362: `trainAngle = trainAngleForTransformation` (3축 변환용)
3. Line 407: `RecommendedTrainAngle = recommendedTrainAngleForMst` (본인 기준으로 계산)

**검증 방법**:
- AxisTransformed MST에서 Keyhole 발생 시 `RecommendedTrainAngle`이 0.0이 아닌지 확인
- `trainAngleForTransformation`과 `recommendedTrainAngleForMst`가 다른지 확인 (forcedTrainAngle=0.0일 때)

---

### Step 2: FinalTransformed MST에 RecommendedTrainAngle 계산 추가

**목적**: FinalTransformed MST에서 Keyhole이면 본인 기준에서 `RecommendedTrainAngle`을 계산

**파일**: `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\algorithm\satellitetracker\processor\SatelliteTrackingProcessor.kt`

**수정 위치**: Line 455-489 (`applyAngleLimitTransformation()` 함수 내부)

**현재 코드**:
```kotlin
// ✅ 변환 후 메타데이터 재계산
val finalPassDtl = finalTransformedDtl.filter { it["MstId"] == mstId }
val metrics = calculateMetrics(finalPassDtl)

// Keyhole 재분석
val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
val threshold = settingsService.keyholeAzimuthVelocityThreshold
val isKeyhole = maxAzRate >= threshold

finalTransformedMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "IsKeyhole" to isKeyhole,
        "RecommendedTrainAngle" to mstData["RecommendedTrainAngle"],  // ← AxisTransformed MST의 값 사용
        // ...
    )
)
```

**수정 후 코드**:
```kotlin
// ✅ 변환 후 메타데이터 재계산
val finalPassDtl = finalTransformedDtl.filter { it["MstId"] == mstId }
val metrics = calculateMetrics(finalPassDtl)

// Keyhole 재분석
val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
val threshold = settingsService.keyholeAzimuthVelocityThreshold
val isKeyhole = maxAzRate >= threshold

// ✅ MST 저장용 RecommendedTrainAngle (본인 기준으로 계산)
val recommendedTrainAngle = if (isKeyhole) {
    // 이미 calculateMetrics()로 MaxAzRateAzimuth를 계산했으므로, 이를 사용하여 Train 각도 계산
    val maxAzRateAzimuth = metrics["MaxAzRateAzimuth"] as? Double ?: 0.0
    calculateTrainAngle(maxAzRateAzimuth)  // ✅ 본인 기준으로 계산
} else {
    0.0
}

finalTransformedMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "IsKeyhole" to isKeyhole,
        "RecommendedTrainAngle" to recommendedTrainAngle,  // ✅ 본인 기준에서 계산된 값
        // ...
    )
)
```

**참고**: 
- `calculateMetrics()` 함수는 이미 `MaxAzRateAzimuth`를 반환함 (Line 689)
- `calculateTrainAngle()` 함수는 `MaxAzRateAzimuth`를 받아서 Train 각도를 계산함 (Line 536-561)

**검증 방법**:
- FinalTransformed MST에서 Keyhole 발생 시 `RecommendedTrainAngle`이 0.0이 아닌지 확인
- AxisTransformed MST의 값과 다른지 확인 (본인 기준으로 계산되었는지)

---

### Step 3: Keyhole 판단 기준 변경 및 RecommendedTrainAngle 사용

**목적**: `finalTransformedMst`의 `IsKeyhole` 값을 직접 참조하고, `finalTransformedMst`의 `RecommendedTrainAngle`을 사용

**파일**: `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\algorithm\satellitetracker\processor\SatelliteTrackingProcessor.kt`

**수정 위치**: Line 84-156 (`processFullTransformation()` 함수 내부)

**현재 코드**:
```kotlin
finalTransformedMst.forEachIndexed { index, mstData ->
    val mstId = mstData["No"] as UInt

    // Train=0 기준 MaxAzRate로 Keyhole 판단
    val train0MaxAzRate = mstData["MaxAzRate"] as? Double ?: 0.0
    val threshold = settingsService.keyholeAzimuthVelocityThreshold
    val isKeyhole = train0MaxAzRate >= threshold  // ← 재판단

    logger.info("패스 #$mstId: Train=0 MaxAzRate = ${String.format("%.6f", train0MaxAzRate)}°/s")
    logger.info("   Keyhole 임계값: $threshold°/s")
    logger.info("   판단 결과: ${if (isKeyhole) "✅ Keyhole 발생" else "✅ Keyhole 미발생"}")

    // Keyhole 발생 시 Train≠0 재계산
    if (isKeyhole) {
        val recommendedTrainAngle = originalMst[index]["RecommendedTrainAngle"] as? Double ?: 0.0  // ← Original MST의 값 사용
        val maxAzRateAzimuth = mstData["MaxAzRateAzimuth"] as? Double ?: 0.0

        logger.info("   계산된 Train 각도: ${String.format("%.6f", recommendedTrainAngle)}°")
        logger.info("🔄 Train=${String.format("%.6f", recommendedTrainAngle)}°로 재변환 시작...")

        // 해당 패스의 Original DTL 추출
        val passOriginalDtl = originalDtl.filter { it["MstId"] == mstId }

        // Original MST를 Train≠0으로 업데이트
        val keyholeOriginalMst = listOf(originalMst[index].toMutableMap().apply {
            put("RecommendedTrainAngle", recommendedTrainAngle)
            put("IsKeyhole", true)
        })
        // ... (기존 로직 유지)
    }
}
```

**수정 후 코드**:
```kotlin
finalTransformedMst.forEachIndexed { index, mstData ->
    val mstId = mstData["No"] as UInt

    // ✅ finalTransformedMst의 IsKeyhole 값을 직접 참조 (재판단하지 않음)
    val isKeyhole = mstData["IsKeyhole"] as? Boolean ?: false
    val train0MaxAzRate = mstData["MaxAzRate"] as? Double ?: 0.0

    logger.info("패스 #$mstId: Train=0 MaxAzRate = ${String.format("%.6f", train0MaxAzRate)}°/s")
    logger.info("   Keyhole 판단 결과 (finalTransformedMst): ${if (isKeyhole) "✅ Keyhole 발생" else "✅ Keyhole 미발생"}")

    // Keyhole 발생 시 Train≠0 재계산
    if (isKeyhole) {
        // ✅ finalTransformedMst의 RecommendedTrainAngle 사용
        val recommendedTrainAngle = mstData["RecommendedTrainAngle"] as? Double ?: 0.0
        
        logger.info("   계산된 Train 각도 (finalTransformedMst): ${String.format("%.6f", recommendedTrainAngle)}°")
        logger.info("🔄 Train=${String.format("%.6f", recommendedTrainAngle)}°로 재변환 시작...")

        // 해당 패스의 Original DTL 추출
        val passOriginalDtl = originalDtl.filter { it["MstId"] == mstId }

        // Original MST를 Train≠0으로 업데이트
        val keyholeOriginalMst = listOf(originalMst[index].toMutableMap().apply {
            put("RecommendedTrainAngle", recommendedTrainAngle)  // ✅ finalTransformedMst의 값 사용
            put("IsKeyhole", true)
        })
        // ... (기존 로직 유지)
    }
}
```

**참고**: 
- `finalTransformedMst`의 `IsKeyhole` 값은 이미 `applyAngleLimitTransformation()`에서 계산됨
- 재판단하지 않고 직접 참조
- `finalTransformedMst`의 `RecommendedTrainAngle`은 이미 본인 기준으로 계산됨
- `keyholeOriginalMst` 업데이트 시 `finalTransformedMst`의 값을 사용

**검증 방법**:
- `finalTransformedMst`의 `IsKeyhole` 값을 직접 참조하는지 확인
- `finalTransformedMst`의 `RecommendedTrainAngle`을 사용하는지 확인

---

### Step 4: EphemerisService.getAllEphemerisTrackMstMerged()에서 RecommendedTrainAngle 데이터 소스 수정

**목적**: API 응답에서 `RecommendedTrainAngle`을 `finalTransformedMst`에서 가져오도록 수정

**파일**: `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\service\mode\EphemerisService.kt`

**수정 위치**: Line 2341 (`getAllEphemerisTrackMstMerged()` 함수 내부)

**문제 분석**:
- 현재 Keyhole 판단은 `finalTransformedMst` 기준으로 수행 (Line 2256-2259)
- 하지만 `RecommendedTrainAngle`은 `originalMst`에서 가져옴 (Line 2341)
- 이로 인해 Keyhole=YES인데 Train 각도가 0.000000°로 표시됨
- `finalTransformedMst`의 `RecommendedTrainAngle`이 이미 계산되어 있지만 사용되지 않음

**현재 코드**:
```kotlin
// ✅ Keyhole 관련 정보
put("IsKeyhole", isKeyhole)
put("RecommendedTrainAngle", original?.get("RecommendedTrainAngle") ?: 0.0)  // ← ❌ originalMst에서 가져옴
```

**수정 후 코드**:
```kotlin
// ✅ Keyhole 관련 정보
put("IsKeyhole", isKeyhole)
put("RecommendedTrainAngle", final.get("RecommendedTrainAngle") as? Double ?: 0.0)  // ← ✅ finalTransformedMst에서 가져옴
```

**수정 이유**:
1. **데이터 일관성**: Keyhole 판단을 `finalTransformedMst` 기준으로 하므로, `RecommendedTrainAngle`도 같은 소스에서 가져와야 함
2. **정확한 값**: `finalTransformedMst`의 `RecommendedTrainAngle`은 `applyAngleLimitTransformation()`에서 계산된 값으로, ±270° 제한이 적용된 상태에서 계산된 정확한 값임
3. **요구사항 준수**: 사용자 요구사항에 따라 `finalTransformedMst`의 `IsKeyhole`과 `RecommendedTrainAngle`을 시스템의 주요 판단 기준으로 사용해야 함

**예상 결과**:
- Keyhole=YES인 경우, Train 각도가 0.000000°가 아닌 계산된 값으로 표시됨
- `finalTransformedMst`의 `RecommendedTrainAngle` 값이 프론트엔드로 전달됨

**검증 방법**:
- `getAllEphemerisTrackMstMerged()`에서 `RecommendedTrainAngle`을 `final`에서 가져오는지 확인
- Keyhole=YES인 경우 Train 각도가 0이 아닌지 확인

---

## Part 2: 추적 단계 - Train 각도 설정 및 currentTrackingPass 관리

### 현재 상태 분석

#### 위성 추적 워크플로우

```
startEphemerisTracking(passId) (791줄)
  ↓
currentTrackingPass 설정 (797줄) - 문제: DataType 필터링 없음
  ↓
moveToStartPosition(passId) (804줄)
  - getEphemerisTrackDtlByMstId() 호출 (Keyhole 여부에 따라 적절한 데이터 반환) ✅
  - targetAzimuth, targetElevation 설정 (1762-1763줄)
  - Train 각도는 설정하지 않음 (정상 - moveToStartPosition은 Az/El만 제어)
  ↓
startModeTimer() (805줄)
  ↓
trackingSatelliteStateCheck() (100ms 주기, 952줄)
  ↓
MOVING_TRAIN_TO_ZERO (960줄)
  - trainAngle = 0f (문제: Keyhole 여부에 따라 설정해야 함) ❌
  - moveTrainToZero(trainAngle) 호출 (965줄)
  ↓
WAITING_FOR_TRAIN_STABILIZATION (975줄)
  - Train 안정화 대기 (3초, TRAIN_STABILIZATION_TIMEOUT)
  ↓
MOVING_TO_TARGET (986줄)
  - moveToTargetAzEl() 호출 (980줄)
  ↓
TRACKING_ACTIVE (997줄)
  - saveRealtimeTrackingData() 호출 (1033줄)
  - createRealtimeTrackingData() 내부에서 Keyhole 여부에 따라 적절한 DataType 사용 (1161-1190줄) ✅
```

#### 문제점 상세 분석

**문제 1: currentTrackingPass 설정 (797줄, 1779줄)**
```kotlin
// 현재 코드
currentTrackingPass = ephemerisTrackMstStorage.find { it["No"] == passId }
```
- **문제**: DataType 필터링 없이 첫 번째로 저장된 데이터 반환
- **저장 순서**: original → axis_transformed → final_transformed → keyhole_axis_transformed → keyhole_final_transformed (419-447줄)
- **결과**: `original` 데이터가 반환될 가능성이 높음
- **영향**: `IsKeyhole`, `RecommendedTrainAngle` 정보가 없어 Train 각도 설정 불가

**문제 2: Train 각도 설정 (963줄)**
```kotlin
// 현재 코드
var trainAngle = 0f  // 무조건 0으로 설정
```
- **문제**: Keyhole 위성인 경우 `RecommendedTrainAngle`을 사용해야 함
- **영향**: Keyhole 위성 추적 시 Train 각도가 0으로 설정되어 추적 실패 가능

---

## Part 2 구현 단계

### Step 5: getTrackingPassMst() 헬퍼 함수 생성

**위치**: `getEphemerisTrackDtlByMstId()` 함수 근처 (약 2708줄, `getEphemerisTrackDtlByMstIdAndDataType()` 함수 다음)

**함수명**: `getTrackingPassMst(passId: UInt): Map<String, Any?>?`

**역할**: 
- passId로 MST 데이터 조회
- Keyhole 여부에 따라 DataType을 **동적으로 선택** (정해져 있지 않음)
  - Keyhole 발생: `keyhole_final_transformed` MST
  - Keyhole 미발생: `final_transformed` MST
- `getEphemerisTrackDtlByMstId()` 함수와 동일한 Keyhole 판단 로직 사용

**현재 상태**: 
- 현재 이 함수는 **존재하지 않음**
- passId로 MST를 조회하는 함수가 없음
- `getEphemerisTrackMstByDataType()`은 DataType별 조회만 가능 (passId 필터링 없음)
- `getEphemerisTrackDtlByMstId()`는 DTL 데이터 반환 (MST가 아님)

**KDOC 주석 포함 구현**:

```kotlin
/**
 * Keyhole 여부에 따라 적절한 MST(Master) 데이터를 반환합니다.
 * 
 * 이 함수는 위성 추적 시작 시 currentTrackingPass를 설정하기 위해 사용됩니다.
 * passId로 조회하며, Keyhole 여부에 따라 DataType을 **동적으로 선택**합니다:
 * - Keyhole 발생: keyhole_final_transformed MST (Train≠0, ±270° 제한 적용)
 * - Keyhole 미발생: final_transformed MST (Train=0, ±270° 제한 적용)
 * 
 * 선택된 MST에는 다음 정보가 포함됩니다:
 * - IsKeyhole: Keyhole 여부 (Boolean)
 * - RecommendedTrainAngle: 권장 Train 각도 (Double, Keyhole 발생 시만 0이 아님)
 * - StartTime, EndTime: 추적 시작/종료 시간
 * - 기타 추적 메타데이터
 * 
 * @param passId 패스 ID (MST ID)
 * @return Keyhole 여부에 따라 선택된 MST 데이터, 없으면 null
 * 
 * @see getEphemerisTrackDtlByMstId 동일한 Keyhole 판단 로직 사용 (DTL 데이터 반환)
 * @see getAllEphemerisTrackMstMerged Keyhole 판단 기준과 일치
 * 
 * @note 이 함수는 현재 존재하지 않으며, 새로 생성해야 합니다.
 * @note DataType은 정해져 있지 않고, Keyhole 여부에 따라 동적으로 선택됩니다.
 */
private fun getTrackingPassMst(passId: UInt): Map<String, Any?>? {
    // 1. final_transformed MST에서 IsKeyhole 확인
    // final_transformed MST에 IsKeyhole 정보가 저장되어 있음
    val finalMst = ephemerisTrackMstStorage.find { 
        it["No"] == passId && it["DataType"] == "final_transformed" 
    }
    
    if (finalMst == null) {
        logger.warn("⚠️ 패스 ID ${passId}에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
        return null
    }
    
    // Keyhole 여부 확인 (final_transformed MST의 IsKeyhole 필드 사용)
    val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
    
    // 2. Keyhole 여부에 따라 MST 선택
    // Keyhole 발생 시: keyhole_final_transformed MST (Train≠0으로 재계산된 데이터)
    // Keyhole 미발생 시: final_transformed MST (Train=0 데이터)
    val dataType = if (isKeyhole) {
        // Keyhole 발생 시 keyhole_final_transformed MST 존재 여부 확인
        val keyholeMstExists = ephemerisTrackMstStorage.any {
            it["No"] == passId && it["DataType"] == "keyhole_final_transformed"
        }
        
        if (!keyholeMstExists) {
            logger.warn("⚠️ 패스 ID ${passId}: Keyhole로 판단되었으나 keyhole_final_transformed MST가 없습니다. final_transformed MST로 폴백합니다.")
            "final_transformed"  // 폴백
        } else {
            logger.debug("🔑 패스 ID ${passId}: Keyhole 발생 → keyhole_final_transformed MST 사용")
            "keyhole_final_transformed"
        }
    } else {
        logger.debug("✅ 패스 ID ${passId}: Keyhole 미발생 → final_transformed MST 사용")
        "final_transformed"
    }
    
    // 3. 선택된 DataType의 MST 반환
    val selectedMst = ephemerisTrackMstStorage.find {
        it["No"] == passId && it["DataType"] == dataType
    }
    
    if (selectedMst == null) {
        logger.error("❌ 패스 ID ${passId}: 선택된 DataType($dataType)의 MST를 찾을 수 없습니다.")
        return null
    }
    
    logger.info("📊 패스 ID ${passId} MST 선택: Keyhole=${if (isKeyhole) "YES" else "NO"}, DataType=${dataType}")
    
    return selectedMst
}
```

---

### Step 6: startEphemerisTracking()에서 currentTrackingPass 설정 개선

**KDOC 주석 포함 구현**:

```kotlin
/**
 * 위성 추적 시작
 * 
 * 위성 추적을 시작하고 상태머신을 초기화합니다.
 * Keyhole 여부에 따라 적절한 MST를 currentTrackingPass에 설정합니다.
 * 
 * @param passId 추적할 패스 ID (MST ID)
 * 
 * @see getTrackingPassMst Keyhole 여부에 따라 적절한 MST 선택
 * @see moveToStartPosition 시작 위치로 이동
 * @see startModeTimer 모드 타이머 시작
 */
fun startEphemerisTracking(passId: UInt) {
    logger.info("🚀 위성 추적 시작: 패스 ID = {}", passId)
    stopModeTimer()
    executedActions.clear()
    logger.info("🔄 실행 플래그 초기화 완료")
    currentTrackingPassId = passId
    
    // ✅ Keyhole 여부에 따라 적절한 MST 선택
    // Keyhole 발생: keyhole_final_transformed MST
    // Keyhole 미발생: final_transformed MST
    currentTrackingPass = getTrackingPassMst(passId)
    
    if (currentTrackingPass == null) {
        logger.error("패스 ID {}에 해당하는 데이터를 찾을 수 없습니다", passId)
        return
    }
    
    // Keyhole 정보 로깅
    val isKeyhole = currentTrackingPass["IsKeyhole"] as? Boolean ?: false
    val recommendedTrainAngle = currentTrackingPass["RecommendedTrainAngle"] as? Double ?: 0.0
    logger.info("📊 추적 패스 정보: Keyhole=${if (isKeyhole) "YES" else "NO"}, RecommendedTrainAngle=${recommendedTrainAngle}°")
    
    logger.info("✅ ephemeris 추적 준비 완료 (실제 추적 시작 전)")
    // 상태머신 진입
    moveToStartPosition(passId)
    startModeTimer()
    logger.info("✅ 위성 추적 및 통합 모드 타이머 시작 완료")
}
```

---

### Step 7: sendHeaderTrackingData()에서 currentTrackingPass 설정 개선

**KDOC 주석 포함 구현**:

```kotlin
/**
 * 위성 추적 시작 - 헤더 정보 전송
 * 
 * 2.12.1 위성 추적 해더 정보 송신 프로토콜 사용
 * Keyhole 여부에 따라 적절한 MST를 currentTrackingPass에 설정합니다.
 * 
 * @param passId 추적할 패스 ID (MST ID)
 * 
 * @see getTrackingPassMst Keyhole 여부에 따라 적절한 MST 선택
 */
fun sendHeaderTrackingData(passId: UInt) {
    try {
        udpFwICDService.writeNTPCommand()
        currentTrackingPassId = passId
        
        // ✅ Keyhole 여부에 따라 적절한 MST 선택
        // Keyhole 발생: keyhole_final_transformed MST
        // Keyhole 미발생: final_transformed MST
        val selectedPass = getTrackingPassMst(passId)
        
        if (selectedPass == null) {
            logger.error("선택된 패스 ID($passId)에 해당하는 데이터를 찾을 수 없습니다.")
            return
        }
        
        // 현재 추적 중인 패스 설정
        currentTrackingPass = selectedPass
        
        // Keyhole 정보 로깅
        val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
        val recommendedTrainAngle = selectedPass["RecommendedTrainAngle"] as? Double ?: 0.0
        logger.info("📊 헤더 전송 패스 정보: Keyhole=${if (isKeyhole) "YES" else "NO"}, RecommendedTrainAngle=${recommendedTrainAngle}°")
        
        // 패스 시작 및 종료 시간 가져오기
        val startTime = (selectedPass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
        val endTime = (selectedPass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
        
        // ... 나머지 코드 동일 ...
    } catch (e: Exception) {
        // ... 에러 처리 동일 ...
    }
}
```

---

### Step 8: MOVING_TRAIN_TO_ZERO 상태에서 Train 각도 설정 개선

**KDOC 주석 포함 구현**:

```kotlin
TrackingState.MOVING_TRAIN_TO_ZERO -> {
    // ✅ Tilt 시작 위치로 이동 상태 표시
    trackingStatus.ephemerisTrackingState = "TRAIN_MOVING_TO_ZERO"
    
    // ✅ Keyhole 여부에 따라 Train 각도 설정
    // currentTrackingPass는 getTrackingPassMst()를 통해 설정되었으므로
    // Keyhole 여부에 따라 적절한 MST를 가리킴
    val recommendedTrainAngle = currentTrackingPass?.get("RecommendedTrainAngle") as? Double ?: 0.0
    val isKeyhole = currentTrackingPass?.get("IsKeyhole") as? Boolean ?: false
    
    // Keyhole 여부에 따라 Train 각도 설정
    // Keyhole 발생: RecommendedTrainAngle 사용 (Train≠0)
    // Keyhole 미발생: 0 사용 (Train=0)
    val trainAngle = if (isKeyhole) {
        recommendedTrainAngle.toFloat()
    } else {
        0f
    }
    
    // GlobalData에 Train 각도 설정
    GlobalData.EphemerisTrakingAngle.trainAngle = trainAngle
    
    // Train 각도 이동 명령 전송
    moveTrainToZero(trainAngle)
    
    // Train 각도 설정 정보 로깅
    logger.info("🔄 Train 각도 설정: Keyhole=${if (isKeyhole) "YES" else "NO"}, Train=${trainAngle}°")
    if (isKeyhole) {
        logger.info("   - RecommendedTrainAngle: ${recommendedTrainAngle}°")
    }
    
    // Train 각도 도달 확인
    if (isTrainAtZero()) {
        currentTrackingState = TrackingState.WAITING_FOR_TRAIN_STABILIZATION
        stabilizationStartTime = System.currentTimeMillis()
        // ✅ Tilt ${trainAngle}도 이동 완료, 안정화 대기 상태로 업데이트
        trackingStatus.ephemerisTrackingState = "TRAIN_STABILIZING"
        logger.info("✅ Train가 ${trainAngle}도에 도달, 안정화 대기 시작")
    }
}
```

---

## 수정 단계 요약

### Part 1: 데이터 변환 단계
1. **Step 0: Original MST에서 calculateTrainAngleMethodA/B 제거** (필수)
   - Line 227-278: 불필요한 래퍼 함수 제거
   - `metrics["MaxAzRateAzimuth"]`를 직접 사용하여 `calculateTrainAngle()` 호출

2. **Step 1: AxisTransformed MST에서 Train의 두 가지 용도 구분** (필수)
   - Line 342-412: 3축 변환용 Train과 MST 저장용 RecommendedTrainAngle 구분
   - `trainAngleForTransformation`: 3축 변환용 (forcedTrainAngle 또는 MST에서 읽음)
   - `recommendedTrainAngleForMst`: MST 저장용 (본인 기준으로 계산)

3. **Step 2: FinalTransformed MST에 RecommendedTrainAngle 계산 추가** (필수)
   - Line 455-489: Keyhole이면 본인 기준에서 `RecommendedTrainAngle` 계산
   - `calculateMetrics()`로 이미 계산된 `MaxAzRateAzimuth`를 사용하여 `calculateTrainAngle()` 직접 호출

4. **Step 3: Keyhole 판단 기준 변경 및 RecommendedTrainAngle 사용** (필수)
   - Line 84-156: `finalTransformedMst`의 `IsKeyhole` 값 직접 참조
   - `finalTransformedMst`의 `RecommendedTrainAngle` 사용

5. **Step 4: EphemerisService.getAllEphemerisTrackMstMerged()에서 RecommendedTrainAngle 데이터 소스 수정** (필수)
   - Line 2341: `RecommendedTrainAngle`을 `finalTransformedMst`에서 가져오도록 수정
   - API 응답에서 Keyhole=YES인 경우 Train 각도가 올바르게 표시되도록 수정

### Part 2: 추적 단계
6. **Step 5: getTrackingPassMst() 헬퍼 함수 생성** (필수)
   - 약 2708줄 근처: Keyhole 여부에 따라 적절한 MST 선택하는 헬퍼 함수 생성

7. **Step 6: startEphemerisTracking()에서 currentTrackingPass 설정 개선** (필수)
   - Line 797: `getTrackingPassMst()` 사용

8. **Step 7: sendHeaderTrackingData()에서 currentTrackingPass 설정 개선** (필수)
   - Line 1779: `getTrackingPassMst()` 사용

9. **Step 8: MOVING_TRAIN_TO_ZERO 상태에서 Train 각도 설정 개선** (필수)
   - Line 960-972: Keyhole 여부에 따라 Train 각도 동적 설정

---

## 예상 결과

수정 후:
1. Train의 두 가지 용도가 명확히 구분됨
   - `trainAngleForTransformation`: 3축 변환용 (forcedTrainAngle=0.0이면 0.0)
   - `recommendedTrainAngleForMst`: MST 저장용 (본인 기준으로 계산)
2. 각 MST는 독립적으로 본인 기준에서 Keyhole 판단 및 `RecommendedTrainAngle` 계산
3. `finalTransformedMst`에서 Keyhole로 판단되면 해당 MST의 `RecommendedTrainAngle` 사용
4. Train≠0 재계산이 정상적으로 수행됨
5. KEYHOLE=YES인데 Train=0인 문제 해결
6. 위성 추적 시 Keyhole 여부에 따라 Train 각도가 올바르게 설정됨
7. `currentTrackingPass`가 Keyhole 여부에 따라 적절한 MST를 가리킴

**예상 로그 출력**:
```
패스 #6: Train=0 MaxAzRate = 15.654204°/s
   Keyhole 판단 결과 (finalTransformedMst): ✅ Keyhole 발생
   계산된 Train 각도 (finalTransformedMst): -167.400000°
🔄 Train=-167.400000°로 재변환 시작...
📊 추적 패스 정보: Keyhole=YES, RecommendedTrainAngle=-167.400000°
🔄 Train 각도 설정: Keyhole=YES, Train=-167.4°
```

---

## 리스크

1. **Train의 두 가지 용도 구분**
   - `trainAngleForTransformation`과 `recommendedTrainAngleForMst`가 명확히 구분되는지 확인 필요
   - 대응: 변수명을 명확하게 하고 주석 추가

2. **각 MST의 RecommendedTrainAngle 계산 검증**
   - 각 MST에서 본인 기준으로 `RecommendedTrainAngle`을 올바르게 계산하는지 확인 필요
   - `calculateMetrics()`가 `MaxAzRateAzimuth`를 올바르게 반환하는지 확인 필요
   - 대응: 기존 로직이 정상 동작하는지 테스트 필요

3. **기존 데이터와의 호환성**
   - 이미 저장된 MST에는 이전 방식으로 계산된 `RecommendedTrainAngle`이 있을 수 있음
   - 대응: 새로 생성되는 데이터부터 적용되므로 문제 없음

4. **currentTrackingPass null 체크**
   - `currentTrackingPass`가 null일 수 있으므로 null 체크 필요
   - 대응: 모든 사용 위치에서 null 체크 추가

---

## 구현 Todo 목록

### Part 1: 데이터 변환 단계
- [ ] Step 0: Original MST에서 calculateTrainAngleMethodA/B 제거
- [ ] Step 1: AxisTransformed MST에서 Train의 두 가지 용도 구분
- [ ] Step 2: FinalTransformed MST에 RecommendedTrainAngle 계산 추가
- [ ] Step 3: Keyhole 판단 기준 변경 및 RecommendedTrainAngle 사용
- [ ] Step 4: EphemerisService.getAllEphemerisTrackMstMerged()에서 RecommendedTrainAngle 데이터 소스 수정

### Part 2: 추적 단계
- [ ] Step 5: getTrackingPassMst() 헬퍼 함수 생성
- [ ] Step 6: startEphemerisTracking()에서 currentTrackingPass 설정 개선
- [ ] Step 7: sendHeaderTrackingData()에서 currentTrackingPass 설정 개선
- [ ] Step 8: MOVING_TRAIN_TO_ZERO 상태에서 Train 각도 설정 개선

---

## 관련 파일

### SatelliteTrackingProcessor.kt
- `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\algorithm\satellitetracker\processor\SatelliteTrackingProcessor.kt`
  - Line 44-170: `processFullTransformation()` 함수 (전체 흐름 관리)
  - Line 220-320: `structureOriginalData()` 함수 (Original MST 생성) ⚠️ 수정 필요
  - Line 333-416: `applyAxisTransformation()` 함수 (3축 변환) ❌ 수정 필요 (Train의 두 가지 용도 구분)
  - Line 424-493: `applyAngleLimitTransformation()` 함수 (각도 제한) ❌ 수정 필요
  - Line 84-156: Keyhole 판단 및 Train≠0 재계산 ❌ 수정 필요
  - Line 505-510: `calculateTrainAngleMethodA()` 함수 (⚠️ 불필요한 래퍼 함수)
  - Line 520-527: `calculateTrainAngleMethodB()` 함수 (⚠️ 통계용)
  - Line 536-561: `calculateTrainAngle()` 함수 (✅ 직접 사용)
  - Line 597-695: `calculateMetrics()` 함수 (✅ MaxAzRateAzimuth 반환)
  - Line 839-886: `simulateTrainApplication()` 함수 (⚠️ 통계용)

### EphemerisService.kt
- `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\service\mode\EphemerisService.kt`
  - Line 2236-2364: `getAllEphemerisTrackMstMerged()` 함수 (API 응답 생성) ❌ 수정 필요
  - Line 2341: `RecommendedTrainAngle`을 `originalMst`에서 가져옴 (❌ 문제)
  - Line 2256-2259: Keyhole 판단은 `finalTransformedMst` 기준으로 수행
  - Line 797: `startEphemerisTracking()` 함수 ❌ 수정 필요
  - Line 1779: `sendHeaderTrackingData()` 함수 ❌ 수정 필요
  - Line 960-972: `trackingSatelliteStateCheck()` 함수 ❌ 수정 필요
  - 약 2708줄: `getTrackingPassMst()` 함수 생성 필요

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 2024-12-15 | 1.0 | 초안 작성 (두 문서 통합) | GTL Systems |
| 2024-12-15 | 1.1 | Part 1과 Part 2로 구분하여 구조화 | GTL Systems |

