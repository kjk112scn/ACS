# 안테나 구조 및 Train 각도 개념 문서

## 1. 개요

### 목적
이 문서는 ACS(Antenna Control System) 안테나의 물리적 구조와 Train 각도 개념을 명확히 정의하고, 좌표계 변환 및 Train 각도 계산 원리를 설명합니다.

### 핵심 개념
- 안테나 물리적 구조: 3축 계층 구조 (Train → Azimuth → Elevation)
- 좌표계: 진북 기준 좌표계에서 서쪽(+7°), 동쪽(-7°) 방향 정의
- Train 각도: 안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 각도

---

## 2. 안테나 물리적 구조

### 2.1 3축 계층 구조

안테나는 **아래에서 위로 쌓여있는 3축 계층 구조**로 구성되어 있습니다:

```
┌─────────────────┐
│  Elevation축    │  ← 최상단 (고도각 회전)
│                 │
│  (Elevation 회전) │
├─────────────────┤
│  Azimuth축      │  ← 중단 (방위각 회전)
│                 │
│  (Azimuth 회전)  │
├─────────────────┤
│  Train축        │  ← 최하단 (안테나 전체 회전)
│                 │
│  (Train 회전)    │
└─────────────────┘
```

### 2.2 축별 역할

| 축 | 위치 | 역할 | 회전 범위 |
|---|------|------|----------|
| **Train** | 최하단 | 안테나 전체를 진북 기준으로 회전 | ±270° |
| **Azimuth** | 중단 | Train 회전 후 방위각 회전 | ±270° |
| **Elevation** | 최상단 | 고도각 회전 | 0° ~ 180° |

### 2.3 회전 순서

1. **Train 회전** (최하단): 안테나 전체를 진북 기준으로 회전
2. **Azimuth 회전** (중단): Train 회전 후 방위각 회전
3. **Elevation 회전** (최상단): 고도각 회전

**중요**: 회전 순서는 아래에서 위로 진행되며, 각 축의 회전은 다음 축에 영향을 줍니다.

---

## 3. 좌표계 정의

### 3.1 진북 기준 좌표계

안테나는 **진북(True North, 0°)**을 기준으로 좌표계를 정의합니다.

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
```

### 3.2 안테나 기울기 방향

안테나 정면에서 보았을 때:

```
[안테나 정면 - 사용자 관점]
  동쪽(-7°) ← [안테나] → 서쪽(+7°)
    좌측              우측
```

#### 방향 정의
- **서쪽 방향**: +7° (사용자 기준 우측)
- **동쪽 방향**: -7° (사용자 기준 좌측)
- **진북 기준**: 0° (북쪽)

#### 안테나 기울기 (Tilt Angle)
- **기본값**: `-7.0°` (SettingsService.kt Line 74)
- **의미**: 안테나가 동쪽 방향으로 7° 기울어져 있음
- **좌표 변환**: `transformCoordinatesWithTrain()` 함수에서 사용

### 3.3 안테나 정면 방향

Train 0°일 때:
- **안테나 정면**: 북쪽(0°)을 향함
- **서쪽(+7°) 방향**: 270° (서쪽)
- **동쪽(-7°) 방향**: 90° (동쪽)

---

## 4. Train 각도 개념

### 4.1 Train 각도의 목적

**Train 각도**는 안테나 전체를 진북 기준으로 회전시키는 각도입니다.

#### 주요 목적
1. **Keyhole 회피**: Azimuth ±270° 영역 통과 방지
2. **최적 추적**: 위성 Azimuth 방향으로 안테나 정렬
3. **기계적 안전**: 포지셔너 물리적 제한 준수

### 4.2 Train 각도 계산 원리

#### 핵심 원리
**안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 Train 각도**를 계산합니다.

#### 계산 공식
```kotlin
// SatelliteTrackingProcessor.kt Line 620-644
fun calculateTrainAngle(azimuth: Double): Double {
    // Azimuth를 0-360 범위로 정규화
    var normalizedAz = azimuth % 360.0
    if (normalizedAz < 0) normalizedAz += 360.0
    
    // 두 가지 경로 계산
    val option1 = normalizedAz - 270.0  // 기본 계산
    val option2 = if (option1 < 0) {
        option1 + 360.0  // 음수면 시계 방향
    } else {
        option1 - 360.0  // 양수면 반시계 방향
    }
    
    // ±270° 범위 내 유효한 옵션만 선택
    // 유효한 옵션 중 절댓값이 작은 것 선택
    return validOptions.minByOrNull { Math.abs(it) } ?: option1
}
```

#### 계산 예시

**예시 1**: 위성 Azimuth = 102.6°
```
Train 0°일 때 서쪽(+7°) 위치: 270°
목표: 서쪽(+7°)을 102.6°로 이동
계산: 102.6° - 270° = -167.4°
결과: Train = -167.4° (반시계 방향 회전)
```

**예시 2**: 위성 Azimuth = 257.2°
```
Train 0°일 때 서쪽(+7°) 위치: 270°
목표: 서쪽(+7°)을 257.2°로 이동
계산: 257.2° - 270° = -12.8°
결과: Train = -12.8° (반시계 방향 회전)
```

### 4.3 Train 각도 적용

#### 3축 좌표 변환
```kotlin
// CoordinateTransformer.kt Line 24-62
transformCoordinatesWithTrain(
    azimuth: Double,        // 위성 방위각
    elevation: Double,      // 위성 고도각
    tiltAngle: Double,      // 안테나 기울기 (-7.0°)
    trainAngle: Double      // Train 회전 각도
): Pair<Double, Double>
```

#### 변환 과정
1. **Train 회전**: Z축(수직축) 기준 회전 (안테나 전체 회전)
2. **Tilt 회전**: Y축(회전체의 수평축) 기준 회전 (안테나 기울기)
3. **결과**: 안테나 좌표계에서의 Azimuth와 Elevation

---

## 5. 좌표계 변환 상세

### 5.1 2축 → 3축 변환

#### 원본 데이터 (2축)
- **Azimuth**: 위성의 실제 방위각 (0° ~ 360°)
- **Elevation**: 위성의 실제 고도각 (0° ~ 180°)
- **Train**: 0° (저장만, 변환에 사용하지 않음)

#### 변환 후 데이터 (3축)
- **Azimuth**: 안테나 좌표계에서의 방위각 (0° ~ 360°)
- **Elevation**: 안테나 좌표계에서의 고도각 (0° ~ 180°)
- **Train**: 적용된 Train 각도

### 5.2 각도 제한 (±270°)

#### 목적
포지셔너 물리적 제한 준수

#### 제한 범위
- **Azimuth**: ±270° 범위
- **Elevation**: 0° ~ 180° (변경 없음)
- **Train**: ±270° 범위

#### 제한 적용
```kotlin
// LimitAngleCalculator.kt
while (azimuth > 270.0) azimuth -= 360.0
while (azimuth < -270.0) azimuth += 360.0
```

---

## 6. Keyhole 회피

### 6.1 Keyhole 정의

**Keyhole** = Azimuth가 ±270° 근처를 통과하는 위성

#### 발생 조건
- 위성 궤도가 Azimuth 260° → 280° 이동하는 경우
- 270° 기계적 한계 통과 → **Gimbal Lock 위험**
- 포지셔너 물리적 제한으로 추적 불가

### 6.2 Keyhole 판단

#### 판단 기준
```kotlin
// SatelliteTrackingProcessor.kt Line 414-416
val maxAzRate = metrics["MaxAzRate"] as? Double ?: 0.0
val threshold = settingsService.keyholeAzimuthVelocityThreshold  // 기본값: 10.0°/s
val isKeyhole = maxAzRate >= threshold
```

#### 판단 데이터
- **데이터 소스**: `final_transformed` (Train=0, ±270° 제한 적용)
- **판단 기준**: MaxAzRate (최대 Azimuth 각속도)
- **임계값**: 10.0°/s (설정 가능)

### 6.3 Keyhole 해결 방법

#### Train 각도 적용
Keyhole 발생 시 Train≠0으로 재변환하여 ±270° 영역 회피

#### 해결 과정
1. **Keyhole 판단**: `final_transformed`에서 MaxAzRate 확인
2. **Train 각도 계산**: `calculateTrainAngle(MaxAzRateAzimuth)` 호출
3. **재변환**: Train≠0으로 3축 변환 및 각도 제한 적용
4. **결과**: `keyhole_final_transformed` 데이터 생성

---

## 7. 데이터 흐름

### 7.1 전체 데이터 흐름

```
2축 원본 데이터 (위성 좌표)
  ↓
Train=0 적용 (3축 변환)
  ↓
각도 제한 (±270°)
  ↓
Keyhole 판단
  ├─ Keyhole 미발생: 종료
  └─ Keyhole 발생:
       ↓
      Train≠0 계산 (안테나 서쪽(+7°) → 위성 Azimuth)
       ↓
      Train≠0 적용 (3축 변환)
       ↓
      각도 제한 (±270°)
       ↓
      최적화 완료
```

### 7.2 6가지 DataType

| DataType | Train | 각도 제한 | 용도 |
|----------|-------|----------|------|
| `original` | N/A | N/A | 2축 원본 데이터 |
| `axis_transformed` | 0° | ❌ | 3축 변환 중간 (0-360°) |
| `final_transformed` | 0° | ✅ | 최종 데이터 (Train=0, ±270°), **Keyhole 판단 기준** |
| `keyhole_axis_transformed` | ≠0 | ❌ | Keyhole 3축 중간 (Train≠0, 0-360°) |
| `keyhole_final_transformed` | ≠0 | ✅ | Keyhole 최종 (Train≠0, ±270°), **실제 사용** |

---

## 8. 구현 상세

### 8.1 주요 함수

#### Train 각도 계산
- **파일**: `SatelliteTrackingProcessor.kt`
- **함수**: `calculateTrainAngle(azimuth: Double)` (Line 620-644)
- **역할**: 안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 Train 각도 계산

#### 3축 좌표 변환
- **파일**: `CoordinateTransformer.kt`
- **함수**: `transformCoordinatesWithTrain()` (Line 24-62)
- **역할**: 2축 좌표를 3축 좌표로 변환 (Train, Tilt 적용)

#### 각도 제한
- **파일**: `LimitAngleCalculator.kt`
- **함수**: `convertTrackingData()` (Line 39-55)
- **역할**: Azimuth를 ±270° 범위로 제한

### 8.2 설정 값

#### Tilt Angle
- **키**: `antennaspec.tiltAngle`
- **기본값**: `-7.0°`
- **설명**: 안테나 기울기 각도 (동쪽 방향으로 7° 기울어짐)

#### Keyhole 임계값
- **키**: `ephemeris.tracking.keyholeAzimuthVelocityThreshold`
- **기본값**: `10.0°/s`
- **설명**: Keyhole 판단 기준 (MaxAzRate 임계값)

---

## 9. 좌표계 변환 예시

### 9.1 Train 0°일 때

**입력**:
- 위성 Azimuth: 257.2°
- 위성 Elevation: 45.0°
- Train: 0°
- Tilt: -7.0°

**변환 과정**:
1. Train 회전: 0° (회전 없음)
2. Tilt 회전: -7.0° 적용
3. 결과: 안테나 좌표계에서의 Azimuth와 Elevation

**출력**:
- 안테나 Azimuth: ~267.1° (변환 후)
- 안테나 Elevation: ~45.0° (변환 후)

### 9.2 Train -167.4°일 때

**입력**:
- 위성 Azimuth: 257.2°
- 위성 Elevation: 45.0°
- Train: -167.4° (Keyhole 회피용)
- Tilt: -7.0°

**변환 과정**:
1. Train 회전: -167.4° (반시계 방향 회전)
2. Tilt 회전: -7.0° 적용
3. 결과: 안테나 좌표계에서의 Azimuth와 Elevation

**출력**:
- 안테나 Azimuth: ~102.6° (±270° 범위 내)
- 안테나 Elevation: ~45.0° (변환 후)

---

## 10. 참고사항

### 10.1 안테나 구조의 중요성

안테나 구조는 **아래에서 위로 쌓여있는 형태**로, 각 축의 회전이 다음 축에 영향을 줍니다:
- **Train 회전**: 안테나 전체를 회전시켜 Azimuth와 Elevation 회전축 방향 변경
- **Azimuth 회전**: Train 회전 후 방위각 회전
- **Elevation 회전**: 최상단에서 고도각 회전

### 10.2 좌표계의 중요성

**진북 기준 좌표계**에서:
- **서쪽 = 우측 = +7°**: 안테나 정면에서 보았을 때 우측 방향
- **동쪽 = 좌측 = -7°**: 안테나 정면에서 보았을 때 좌측 방향
- **Tilt Angle = -7.0°**: 안테나가 동쪽 방향으로 7° 기울어져 있음

### 10.3 Train 각도 계산의 중요성

**Train 각도**는 안테나 서쪽(+7°) 방향을 위성 Azimuth로 회전시키는 각도로:
- **Keyhole 회피**: ±270° 영역 통과 방지
- **최적 추적**: 위성 방향으로 안테나 정렬
- **기계적 안전**: 포지셔너 물리적 제한 준수

---

## 11. 관련 파일

### 백엔드 (Kotlin)
- `SatelliteTrackingProcessor.kt`: Train 각도 계산 및 Keyhole 판단
- `CoordinateTransformer.kt`: 3축 좌표 변환
- `LimitAngleCalculator.kt`: 각도 제한 (±270°)
- `SettingsService.kt`: Tilt Angle 및 Keyhole 임계값 설정

### 프론트엔드 (TypeScript/Vue)
- `EphemerisDesignationPage.vue`: 위성 궤도 지정 및 Train 각도 표시
- `icdStore.ts`: 안테나 각도 데이터 관리
- `settingsStore.ts`: 안테나 사양 설정 (Tilt Angle)

### 문서
- `Train_Algorithm_Completed.md`: Train 각도 알고리즘 설계 문서
- `Keyhole_And_Train_Angle_Recalculation_Plan.md`: Keyhole 판단 및 Train 각도 재계산 계획

---

## 12. 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 2024-12 | 1.0 | 초안 작성 - 안테나 구조 및 Train 각도 개념 정리 | GTL Systems |

---

**문서 작성일**: 2024-12  
**버전**: 1.0  
**상태**: 완료



