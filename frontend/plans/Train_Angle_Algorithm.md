# Train 각도 알고리즘 설계 문서

## 1. 개요

### 목적
위성 추적 시 Keyhole 영역(Azimuth ±270° 근처) 회피를 위한 Train 각도 최적화 알고리즘

### 핵심 기능
- 2축 원본 데이터 → 3축 변환 (Train 각도 적용)
- Keyhole 판단 및 Train 각도 계산
- 각도 제한 (±270° 범위)
- 6가지 DataType별 데이터 관리

### 주요 특징
- Train=0과 Train≠0 데이터 분리 관리
- Keyhole 발생 시에만 Train≠0 데이터 생성
- 동적 CSV 출력 (Keyhole 여부에 따라 헤더/데이터 변경)

---

## 2. 물리적 배치 및 좌표계

### 2.1 안테나 구조
```
┌─────────────────┐
│  Elevation축    │  ← 최상단 (고도각 회전)
├─────────────────┤
│  Azimuth축      │  ← 중단 (방위각 회전)
├─────────────────┤
│  Train축        │  ← 최하단 (안테나 전체 회전)
└─────────────────┘
```

### 2.2 좌표계
```
        👤 사용자 (북쪽에서 남쪽을 바라봄)
          
        북쪽 (0°)
          ↑
          |
서쪽 ←----●----→ 동쪽
(270°)  안테나   (90°)
          |
          ↓
        남쪽 (180°)

[안테나 정면 - 사용자 관점]
  동쪽(-7°) ← [안테나] → 서쪽(+7°)
    좌측              우측
```

### 2.3 ±270° 제한 이유
- 기계적 안전: 포지셔너 물리적 제한
- Gimbal Lock 방지: Keyhole 영역 회피
- 안정적 추적: ±270° 범위 내에서만 동작

---

## 3. Keyhole 개념

### 3.1 Keyhole 정의
**Keyhole** = Azimuth가 ±270° 근처를 통과하는 위성

#### 발생 조건
- 위성 궤도가 Azimuth 260° → 280° 이동하는 경우
- 270° 기계적 한계 통과 → **Gimbal Lock 위험**
- 포지셔너 물리적 제한으로 추적 불가

#### 해결 방법
- Train 각도로 회전하여 ±270° 영역 회피
- 예: Train=-90° 적용 시 260° → -170° (270° 회피)

### 3.2 Keyhole 판단 기준
**판단 데이터**: `final_transformed` (Train=0)
```kotlin
val train0MaxAzRate = finalMst["MaxAzRate"] as Double
val threshold = 10.0  // 사용자 설정 (기본값 3.0)
val isKeyhole = train0MaxAzRate >= threshold
```

**판단 로직**:
- `final_transformed`의 MaxAzRate 계산 (Train=0 적용 상태)
- MaxAzRate가 임계값 이상이면 Keyhole로 판단
- Keyhole 발생 시 Train≠0 재계산 진행

### 3.3 임계값의 의미

| 임계값 | 효과 | 사용 시나리오 |
|--------|------|-------------|
| 1.0°/s | 과도한 Train 적용 | 테스트 (비권장, 역효과 가능) |
| 3.0°/s | 보수적 판단 | 안전 우선 |
| 10.0°/s | 공격적 판단 | 진짜 위험한 위성만 Train 적용 (권장) |

**최근 로그 분석 결과**:
```
패스 #8: MaxAzRate = 1.099°/s
임계값 1.0°/s → Keyhole 판단 → Train 적용
→ 결과: 각속도 증가 (1.099 → 3.188°/s) ← 역효과!

임계값 10.0°/s → Keyhole 미발생 → Train=0 유지
→ 결과: 최적 (1.099°/s 유지)
```

**결론**: 높은 임계값(10.0)이 더 효과적

---

## 4. 데이터 흐름 아키텍처

### 4.1 전체 데이터 흐름
```
┌─────────────────────────────────────────────────────────────┐
│ Original (2축)                                              │
│   - Azimuth: 257.197°                                       │
│   - Train: 0° (저장만)                                       │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ Axis Transformed (Train=0, 각도 제한 ❌)                    │
│   - Azimuth: ~267° (0-360° 범위)                            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ Final Transformed (Train=0, 각도 제한 ✅)                    │
│   - Azimuth: ~267° (±270° 범위)                             │
│   - MaxAzRate: 4.493°/s ← Keyhole 판단 기준                │
└─────────────────────────────────────────────────────────────┘
                              ↓
                    [Keyhole 판단]
                              ↓
                    ┌───────┴───────┐
                    │               │
         Keyhole 미발생    Keyhole 발생
            (종료)             │
                              ↓
         ┌───────────────────────────────────────────────┐
         │ Keyhole Axis Transformed                     │
         │   - Train≠0 (예: -167.4°)                    │
         │   - 각도 제한 ❌ (0-360° 범위)                │
         └───────────────────────────────────────────────┘
                              ↓
         ┌───────────────────────────────────────────────┐
         │ Keyhole Final Transformed                    │
         │   - Train≠0                                  │
         │   - 각도 제한 ✅ (±270° 범위)                 │
         │   - MaxAzRate: 2.663°/s ← 최적화 완료       │
         └───────────────────────────────────────────────┘
```

### 4.2 6가지 DataType 정의

| DataType | Train | 각도 제한 | 저장 | 용도 |
|----------|-------|----------|------|------|
| `original` | N/A | N/A | ✓ | 2축 원본 데이터 (위성 좌표) |
| `axis_transformed` | 0° | ❌ | ✓ | 3축 변환 중간 (Train=0, 0-360°) |
| `final_transformed` | 0° | ✅ | ✓ | 최종 데이터 (Train=0, ±270°), **Keyhole 판단 기준** |
| `keyhole_axis_transformed` | ≠0 | ❌ | ✓ | Keyhole 3축 중간 (Train≠0, 0-360°) |
| `keyhole_final_transformed` | ≠0 | ✅ | ✓ | Keyhole 최종 (Train≠0, ±270°), **실제 사용** |

### 4.3 Azimuth 변환 과정
```
위성 좌표계 (2축)
  ↓ applyAxisTransformation()
3D 좌표 변환 (Train 회전)
  ↓ CoordinateTransformer.transformCoordinatesWithTrain()
안테나 좌표계 (0-360° 범위)
  ↓ applyAngleLimitTransformation()
포지셔너 좌표계 (±270° 범위)
```

**예시**:
```
2축 원본: 257.197° (위성의 실제 방위각)
  ↓ Train=0 적용
3축 변환: 267.123° (안테나 기준)
  ↓ 각도 제한
최종 출력: 267.123° (±270° 범위 보장)
```

---

## 5. 핵심 알고리즘

### 5.1 Train 각도 계산
**방법**: 최종 최대 각속도 시점 기준 (방법 B)

#### 계산 과정
1. `final_transformed` (Train=0) 데이터 생성
2. 최대 각속도 시점의 Azimuth 추출
3. Train 각도 계산: `trainAngle = -azimuthAtMaxRate`
4. Keyhole 발생 시 Train≠0으로 재변환

#### 공식
```kotlin
// 최단 거리로 Train 각도 계산
val azimuthAtMaxRate = finalMst["MaxAzRateAzimuth"]  // 예: 102.6°
val trainAngle = -azimuthAtMaxRate  // 예: -102.6°
// 102.6°를 0° 근처로 이동 → 최단 거리 회전
```

### 5.2 좌표 변환
**함수**: `applyAxisTransformation()`

#### 파라미터
- `forcedTrainAngle`: Train 각도 강제 설정
  - `forcedTrainAngle = 0.0`: Train=0 강제 (axis_transformed, final_transformed 생성 시)
  - `forcedTrainAngle = null`: MST에서 읽음 (keyhole_* 생성 시)

#### 내부 동작
```kotlin
// CoordinateTransformer.kt
transformCoordinatesWithTrain(
    azimuth: Double,
    elevation: Double,
    trainAngle: Double
): Pair<Double, Double>

// 3D 좌표계 회전
// Line 42-44: Train 회전 적용
// Line 53: atan2()로 새로운 Azimuth 계산
// Line 57-59: 0-360° 범위로 변환
```

### 5.3 각도 제한
**함수**: `applyAngleLimitTransformation()`

#### 목적
±270° 범위로 제한하여 포지셔너 물리적 제한 준수

#### 적용 시점
- `final_transformed`: Train=0 + 각도 제한 ✅
- `keyhole_final_transformed`: Train≠0 + 각도 제한 ✅

#### 제한 이유
```
Train 적용 후 Azimuth 범위: 0-360°
포지셔너 제한 범위: ±270°
→ 범위 초과 시 물리적 손상 가능
→ 반드시 ±270°로 제한 필요
```

### 5.4 각속도 계산
**방법**: 10-point cumulative sum method

#### 계산식
```kotlin
// 1초간 (10개 포인트) 총 변화량
for (i in 9 until dtl.size) {
    var sum = 0.0
    for (j in (i - 9)..i) {
        val diff = dtl[j] - dtl[j-1]  // 변화량
        sum += abs(diff)  // 누적 (시간으로 나누지 않음)
    }
    maxVelocity = maxOf(maxVelocity, sum)
}
```

#### 특징
- **단위**: °/s (도/초)
- **정밀도**: 10개 포인트 평활화
- **시간 분할**: 1초 단위 (100ms × 10)

---

## 6. 구현 상세

### 6.1 데이터 구조

**파일**: `ProcessedTrackingData.kt` (Line 19-30)

```kotlin
data class ProcessedTrackingData(
    val originalMst: List<Map<String, Any?>>,
    val originalDtl: List<Map<String, Any?>>,
    val axisTransformedMst: List<Map<String, Any?>>,     // Train=0
    val axisTransformedDtl: List<Map<String, Any?>>,
    val finalTransformedMst: List<Map<String, Any?>>,     // Train=0 + 각도제한
    val finalTransformedDtl: List<Map<String, Any?>>,
    val keyholeAxisTransformedMst: List<Map<String, Any?>>,  // Train≠0 (중간)
    val keyholeAxisTransformedDtl: List<Map<String, Any?>>,
    val keyholeFinalTransformedMst: List<Map<String, Any?>>,  // Train≠0 + 각도제한
    val keyholeFinalTransformedDtl: List<Map<String, Any?>>
)
```

### 6.2 핵심 함수

| 함수 | 파일 | 역할 | 참조 위치 |
|------|------|------|-----------|
| `processFullTransformation()` | `SatelliteTrackingProcessor.kt` | 전체 변환 관리 | Line 44-170 |
| `applyAxisTransformation()` | `SatelliteTrackingProcessor.kt` | 3축 변환 | Line 314-418 |
| `applyAngleLimitTransformation()` | `SatelliteTrackingProcessor.kt` | 각도 제한 | Line 421-480 |
| `transformCoordinatesWithTrain()` | `CoordinateTransformer.kt` | Train 회전 | Line 24-62 |
| `convertTrackingData()` | `LimitAngleCalculator.kt` | ±270° 제한 | Line 39-55 |

### 6.3 DB 저장 구조

#### Keyhole 미발생
```
✓ original
✓ axis_transformed
✓ final_transformed
```

#### Keyhole 발생
```
✓ original
✓ axis_transformed
✓ final_transformed
✓ keyhole_axis_transformed      // Train≠0, 각도 제한 ❌
✓ keyhole_final_transformed     // Train≠0, 각도 제한 ✅
```

### 6.4 CSV 출력

#### 헤더 (Keyhole 미발생)
```csv
Index,Time,
Original_Azimuth,Original_Elevation,Original_Azimuth_Velocity,Original_Elevation_Velocity,
Original_Range,Original_Altitude,
AxisTransformed_Azimuth,AxisTransformed_Elevation,AxisTransformed_Azimuth_Velocity,AxisTransformed_Elevation_Velocity,
FinalTransformed_train0_Azimuth,FinalTransformed_train0_Elevation,FinalTransformed_train0_Azimuth_Velocity,FinalTransformed_train0_Elevation_Velocity,
Azimuth_Transformation_Error,Elevation_Transformation_Error
```

#### 헤더 (Keyhole 발생)
```csv
Index,Time,
Original_*,
AxisTransformed_*,
FinalTransformed_train0_*,
KeyholeAxisTransformed_train{angle}_Azimuth,KeyholeAxisTransformed_train{angle}_Elevation,
KeyholeAxisTransformed_train{angle}_Azimuth_Velocity,KeyholeAxisTransformed_train{angle}_Elevation_Velocity,
KeyholeFinalTransformed_train{angle}_Azimuth,KeyholeFinalTransformed_train{angle}_Elevation,
KeyholeFinalTransformed_train{angle}_Azimuth_Velocity,KeyholeFinalTransformed_train{angle}_Elevation_Velocity,
Azimuth_Transformation_Error,Elevation_Transformation_Error
```

**특징**:
- Train 각도는 소수점 6자리 (예: `train165.551039`)
- KeyholeAxis와 KeyholeFinal 컬럼 동적 생성
- Keyhole 미발생 시 해당 컬럼 없음

---

## 7. 프론트엔드 연계

### 7.1 API 엔드포인트
```
GET /api/ephemeris/tracking/mst/merged
```

**기능**: Original + FinalTransformed + KeyholeFinalTransformed 병합

### 7.2 응답 데이터 구조

#### Keyhole 미발생
```json
{
  "No": 3,
  "SatelliteName": "TERRA",
  "OriginalMaxAzRate": 1.234567,
  "OriginalMaxElRate": 0.234567,
  "FinalTransformedMaxAzRate": 1.234567,
  "FinalTransformedMaxElRate": 0.234567,
  "KeyholeFinalTransformedMaxAzRate": 1.234567,
  "KeyholeFinalTransformedMaxElRate": 0.234567,
  "IsKeyhole": false,
  "RecommendedTrainAngle": 0.0
}
```

#### Keyhole 발생
```json
{
  "No": 6,
  "SatelliteName": "AQUA",
  "OriginalMaxAzRate": 4.399377,
  "OriginalMaxElRate": 0.523456,
  "FinalTransformedMaxAzRate": 4.493264,
  "FinalTransformedMaxElRate": 0.523456,
  "KeyholeFinalTransformedMaxAzRate": 2.663722,
  "KeyholeFinalTransformedMaxElRate": 0.523456,
  "IsKeyhole": true,
  "RecommendedTrainAngle": 165.551039
}
```

### 7.3 응답 필드 설명

| 필드 | 설명 | Keyhole 미발생 | Keyhole 발생 |
|------|------|---------------|-------------|
| `OriginalMaxAzRate` | 2축 원본 최대 Az 속도 | 2축 값 | 2축 값 |
| `FinalTransformedMaxAzRate` | Train=0 최대 Az 속도 | Train=0 값 | Train=0 값 |
| **`KeyholeFinalTransformedMaxAzRate`** | **실제 사용 최대 Az 속도** | Train=0 값 | **Train≠0 값 (최적화)** |

### 7.4 그리드 표시
- **2축 Az/El**: `OriginalMaxAzRate`, `OriginalMaxElRate`
- **Train0 Az/El**: `FinalTransformedMaxAzRate`, `FinalTransformedMaxElRate`
- **TrainOK Az/El**: `KeyholeFinalTransformedMaxAzRate`, `KeyholeFinalTransformedMaxElRate`

---

## 8. 코드 참조

### 8.1 주요 파일

| 파일 | 역할 | 주요 라인 |
|------|------|-----------|
| `ProcessedTrackingData.kt` | 데이터 구조 정의 | 19-30 |
| `SatelliteTrackingProcessor.kt` | 전체 변환 로직 | 44-170 (전체), 123-134 (KeyholeAxis), 142-153 (KeyholeFinal) |
| `EphemerisService.kt` | CSV 생성 및 API 연동 | 2984-3000 (조회), 3093-3094 (헤더), 3274-3293 (데이터) |
| `CoordinateTransformer.kt` | 3D 변환 | 24-62 |
| `LimitAngleCalculator.kt` | 각도 제한 | 39-55 |
| `SettingsService.kt` | 임계값 설정 | 157 |

### 8.2 함수 시그니처

```kotlin
// SatelliteTrackingProcessor.kt
fun applyAxisTransformation(
    originalMst: List<Map<String, Any?>>,
    originalDtl: List<Map<String, Any?>>,
    forcedTrainAngle: Double? = null  // null이면 MST에서 읽음
): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>

// 호출 예시
applyAxisTransformation(originalMst, originalDtl, forcedTrainAngle = 0.0)  // Train=0 강제
applyAxisTransformation(keyholeOriginalMst, passOriginalDtl)  // MST에서 읽음
```

---

## 9. 구현 완료 상태 (결론)

### 9.1 완료된 기능
✓ 6가지 DataType 처리 (original, axis, final, keyhole_axis, keyhole_final)
✓ Keyhole 판단 (final_transformed 기준)
✓ Train 각도 계산 (방법 B)
✓ `forcedTrainAngle` 파라미터로 Train=0/≠0 분리
✓ KeyholeAxis/Final DB 저장
✓ CSV 동적 헤더/데이터 출력
✓ 프론트엔드 API 병합

### 9.2 설정
- **임계값**: 기본 3.0°/s, 현재 10.0°/s 사용
- **각속도 계산**: 10-point cumulative sum method
- **각도 제한**: ±270°
- **Train 각도**: 최단 거리 계산

### 9.3 데이터 흐름 요약
```
2축 원본
  ↓ Train=0 적용
3축 변환 (각도 제한 ❌)
  ↓ ±270° 제한
최종 데이터 (각도 제한 ✅)
  ↓ Keyhole 판단
  ├─ 미발생: 종료
  └─ 발생:
       ↓ Train≠0 적용
      3축 변환 (각도 제한 ❌)
       ↓ ±270° 제한
      최적화 완료 (각도 제한 ✅)
```

### 9.4 핵심 개념 정리

**Train 각도의 목적**:
- ✅ **Keyhole 회피**: ±270° 영역 통과 방지
- ❌ **각속도 최소화 아님** (부차적 효과)

**Keyhole 판단**:
- 기준: `final_transformed` (Train=0)의 MaxAzRate
- 임계값: 10.0°/s (설정 가능)
- 판단 후 Keyhole 발생 시에만 Train≠0 적용

**데이터 저장**:
- Train=0: 항상 저장 (original, axis, final)
- Train≠0: Keyhole 발생 시만 저장 (keyhole_axis, keyhole_final)

### 9.5 참고 문서
- `ACS_API/docs/Train_Angle_Algorithm_Design.md`: 세부 알고리즘 설계
- `SettingsService.kt`: 임계값 설정 방법

---

## 10. Keyhole 개념 심화 (추후 검토용)

### 10.1 Train 회전의 효과

#### 목적
±270° 영역을 피하기 위해 좌표계 회전

#### 효과
```
원래 Azimuth 범위: 80° ~ 120° (좁은 범위)
Train=-167.4° 적용
→ 회전 후 범위: -87.4° ~ -47.4° (각도 제한 후)
→ 최종 범위: (360-87.4)° ~ (360-47.4)° = 272.6° ~ 312.6°
```

**문제**: 
- 최종 범위가 272.6° ~ 312.6°로 270° 초과
- 하지만 keyhole_final_transformed는 각도 제한 적용
- → 실제로는 ±270° 범위 내로 조정됨

#### 역효과 사례
```
패스 #8:
- 원래 범위: 90° ~ 120° (±270°와 멀리 떨어짐)
- Train 적용 불필요!
- 강제 적용 시: 각속도 증가 (1.099 → 3.188°/s)
```

**결론**: Keyhole 판단 기준(임계값)이 적절해야 함

### 10.2 임계값 선택 가이드

| 시나리오 | 권장 임계값 | 이유 |
|---------|-----------|------|
| 보수적 운영 | 3.0°/s | 더 많은 위성에 Train 적용 |
| 표준 운영 | 5.0°/s | 균형 잡힌 판단 |
| 공격적 운영 | 10.0°/s | 진짜 위험한 위성만 Train 적용 (권장) |

**현재 설정**: 10.0°/s (공격적 운영)

---

**문서 작성일**: 2024-12
**버전**: 1.0
**상태**: 구현 완료
