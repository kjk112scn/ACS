# Train 각도 계산 알고리즘 설계 문서

**작성일**: 2025-10-24  
**목적**: Keyhole 위성 추적 시 안테나 Train 각도 최적화 알고리즘 설계

---

## 📋 목차
1. [물리적 배치 및 좌표계](#1-물리적-배치-및-좌표계)
2. [문제 정의](#2-문제-정의)
3. [알고리즘 설계](#3-알고리즘-설계)
4. [검증 예제](#4-검증-예제)
5. [구현 가이드](#5-구현-가이드)
6. [체크리스트](#6-체크리스트)

---

## 1. 물리적 배치 및 좌표계

### 1.1 안테나 구조 (하단 → 상단)
```
┌─────────────────┐
│  Elevation축    │  ← 최상단 (고도각 회전)
├─────────────────┤
│  Azimuth축      │  ← 중단 (방위각 회전)
├─────────────────┤
│  Train축        │  ← 최하단 (안테나 전체 회전)
└─────────────────┘
```

### 1.2 좌표계
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
  서쪽(+7°) ← [안테나] → 동쪽(-7°)
    좌측              우측
```

### 1.3 안테나 기울기
- **동쪽 방향**: -7° (사용자 기준 오른쪽)
- **서쪽 방향**: +7° (사용자 기준 왼쪽)
- **시스템 tiltAngle**: 7.0

### 1.4 Train 회전 기준
- **Train 0°**: 안테나 앞면이 북쪽(0°)을 향함
  - 서쪽(+7°) 위치: 270°
- **Train 90°**: 안테나 앞면이 동쪽(90°)을 향함
  - 서쪽(+7°) 위치: 0° (360°)
- **Train 180°**: 안테나 앞면이 남쪽(180°)을 향함
  - 서쪽(+7°) 위치: 90°
- **Train -90° (270°)**: 안테나 앞면이 서쪽(270°)을 향함
  - 서쪽(+7°) 위치: 180°

### 1.5 Train 각도 범위 제한
- **±270° 범위**: -270° ≤ Train ≤ +270° (하드웨어 제한)

---

## 2. 문제 정의

### 2.1 Keyhole 현상
- 위성이 천정(90° 근처)을 빠르게 지나갈 때 Azimuth 각속도가 급격히 증가
- Azimuth 축만으로는 추적 불가능한 상황 발생

### 2.2 해결 방안
- **Train 축 활용**: 안테나 전체를 회전시켜 Azimuth 부담 감소
- **최적 방향**: 안테나 서쪽(+7°)이 위성을 향하도록 Train 회전
- **목표**: 서쪽(+7°) 방향이 위성 Azimuth와 일치

### 2.3 제약 조건
- Train 각도 범위: ±270°
- 최단 거리 회전 선택 (에너지 효율)
- 범위를 벗어나는 경로는 선택 불가

---

## 3. 알고리즘 설계

### 3.1 핵심 원리

**Train 0°일 때**: 서쪽(+7°) = 270°  
**목표**: 서쪽(+7°)을 위성 Azimuth 방향으로 이동

**두 가지 경로 계산**:
1. **Option 1**: Azimuth - 270° (기본 계산)
2. **Option 2**: Option 1의 반대 방향 (Option 1 ± 360°)

**선택 기준**:
- 두 옵션 중 ±270° 범위 내 유효한 옵션만 선택
- 유효한 옵션 중 절댓값이 작은 것 선택 (최단 거리)

### 3.2 알고리즘 코드

```kotlin
/**
 * Train 각도 계산 (최단 거리, ±270° 범위)
 * 
 * 안테나 서쪽(+7°)이 위성을 향하도록 Train 각도 계산
 * 270° 기준으로 최단 경로 선택하되, ±270° 범위 제한 준수
 * 
 * @param azimuth 목표 방위각
 * @return 정규화된 Train 각도 (±270° 범위)
 */
private fun calculateTrainAngle(azimuth: Double): Double {
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
    val validOptions = mutableListOf<Double>()
    
    if (option1 >= -270.0 && option1 <= 270.0) {
        validOptions.add(option1)
    }
    if (option2 >= -270.0 && option2 <= 270.0) {
        validOptions.add(option2)
    }
    
    // 유효한 옵션 중 절댓값이 작은 것 선택
    return validOptions.minByOrNull { Math.abs(it) } ?: option1
}
```

### 3.3 로그 출력 예시

```kotlin
logger.info("=".repeat(60))
logger.info("🔍 Train 각도 계산 상세")
logger.info("-".repeat(60))
logger.info("📊 입력:")
logger.info("  - 목표 Azimuth: ${String.format("%.6f", azimuth)}°")
logger.info("")
logger.info("📊 경로 계산:")
logger.info("  - Option 1: ${String.format("%.6f", option1)}° (범위: ${if (option1 in -270.0..270.0) "✓ OK" else "✗ NG"})")
logger.info("  - Option 2: ${String.format("%.6f", option2)}° (범위: ${if (option2 in -270.0..270.0) "✓ OK" else "✗ NG"})")
logger.info("")
logger.info("✅ 선택된 Train 각도: ${String.format("%.6f", trainAngle)}°")
logger.info("   회전량: ${String.format("%.6f", Math.abs(trainAngle))}° (${if (trainAngle >= 0) "시계" else "반시계"})")
logger.info("=".repeat(60))
```

---

## 4. 검증 예제

### 4.1 양수 Azimuth (10개)

| # | Azimuth | Option 1 | Option 2 | 범위 확인 | 유효 옵션 | 선택 Train | 검증 |
|---|---------|---------|---------|---------|----------|-----------|------|
| 1 | 10° | -260° | 100° | 둘 다 OK | 100° (작음) | **100°** | (100+270)%360=10° ✓ |
| 2 | 45° | -225° | 135° | 둘 다 OK | 135° (작음) | **135°** | (135+270)%360=45° ✓ |
| 3 | 90° | -180° | 180° | 둘 다 OK | -180° (같음) | **-180°** | -180+270=90° ✓ |
| 4 | 135° | -135° | 225° | 둘 다 OK | -135° (작음) | **-135°** | -135+270=135° ✓ |
| 5 | 180° | -90° | 270° | 둘 다 OK | -90° (작음) | **-90°** | -90+270=180° ✓ |
| 6 | 225° | -45° | 315° | option1만 OK | -45° | **-45°** | -45+270=225° ✓ |
| 7 | 257.197° | -12.803° | 347.197° | option1만 OK | -12.803° | **-12.803°** | -12.803+270=257.197° ✓ |
| 8 | 270° | 0° | -360° | option1만 OK | 0° | **0°** | 0+270=270° ✓ |
| 9 | 315° | 45° | -315° | option1만 OK | 45° | **45°** | (45+270)%360=315° ✓ |
| 10 | 350° | 80° | -280° | option1만 OK | 80° | **80°** | (80+270)%360=350° ✓ |

**참고**: 9번(315°), 10번(350°)은 option2가 -270° 미만이므로 범위를 벗어남. option1(45°, 80°)만 유효하여 선택.

### 4.2 음수 Azimuth (10개)

| # | Azimuth | 정규화 (0-360) | Option 1 | Option 2 | 범위 확인 | 선택 Train | 검증 |
|---|---------|--------------|---------|---------|---------|-----------|------|
| 11 | -10° | 350° | 80° | -280° | option1만 OK | **80°** | (80+270)%360=350° ✓ |
| 12 | -20° | 340° | 70° | -290° | option1만 OK | **70°** | (70+270)%360=340° ✓ |
| 13 | -40° | 320° | 50° | -310° | option1만 OK | **50°** | (50+270)%360=320° ✓ |
| 14 | -45° | 315° | 45° | -315° | option1만 OK | **45°** | (45+270)%360=315° ✓ |
| 15 | -60° | 300° | 30° | -330° | option1만 OK | **30°** | (30+270)%360=300° ✓ |
| 16 | -90° | 270° | 0° | -360° | option1만 OK | **0°** | 0+270=270° ✓ |
| 17 | -120° | 240° | -30° | 330° | option1만 OK | **-30°** | -30+270=240° ✓ |
| 18 | -135° | 225° | -45° | 315° | option1만 OK | **-45°** | -45+270=225° ✓ |
| 19 | -180° | 180° | -90° | 270° | 둘 다 OK | **-90°** (작음) | -90+270=180° ✓ |
| 20 | -225° | 135° | -135° | 225° | 둘 다 OK | **-135°** (작음) | -135+270=135° ✓ |

### 4.3 특수 케이스 분석

#### Case A: 둘 다 유효 (1~5번, 19~20번)
- 두 경로 모두 ±270° 범위 내
- 절댓값이 작은 것 선택 (최단 거리)

#### Case B: Option 1만 유효 (6~18번)
- Option 2가 범위 밖 (±270° 초과)
- Option 1만 선택 가능

#### Case C: 270° 초과 Azimuth (9~16번)
- Azimuth > 270°인 경우
- Option 2가 -270° 미만으로 범위 밖
- Option 1(양수)만 유효하여 선택

---

## 5. 구현 가이드

### 5.1 수정 파일

#### 파일 1: `SatelliteTrackingProcessor.kt`
**위치**: `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\algorithm\satellitetracker\processor\SatelliteTrackingProcessor.kt`

**수정 위치**:
- **Line 351-360**: `calculateTrainAngle` 함수 (알고리즘 교체)
- **Line 145-177**: Train 각도 계산 로그 추가
- **Line 184-242**: `applyAxisTransformation` 확인 (tiltAngle = 7.0)

### 5.2 구현 단계

#### Step 1: calculateTrainAngle 함수 수정
```kotlin
// 기존 코드 (Line 351-360)
private fun calculateTrainAngle(azimuth: Double): Double {
    var trainAngle = azimuth - 90.0  // ← 변경 필요
    while (trainAngle > 270.0) trainAngle -= 360.0
    while (trainAngle < -270.0) trainAngle += 360.0
    return trainAngle
}

// 새 코드 (위 3.2절 알고리즘 코드 참조)
```

#### Step 2: 로그 메시지 추가
```kotlin
// Line 145-177 부근에 추가
logger.info("Train 각도 계산 상세:")
logger.info("  - 목표 Azimuth: ${String.format("%.6f", maxAzRateAzimuth)}°")
logger.info("  - Option 1: ${String.format("%.6f", option1)}° (범위: ${if (option1 in -270.0..270.0) "OK" else "NG"})")
logger.info("  - Option 2: ${String.format("%.6f", option2)}° (범위: ${if (option2 in -270.0..270.0) "OK" else "NG"})")
logger.info("  - 선택된 Train: ${String.format("%.6f", selectedTrain)}°")
```

#### Step 3: tiltAngle 확인
```kotlin
// applyAxisTransformation 함수 내부 확인
val tiltAngle = 7.0  // ← 확인 필요 (현재 값이 맞는지)

CoordinateTransformer.transformCoordinatesWithTrain(
    azimuth = originalAzimuth,
    elevation = originalElevation,
    tiltAngle = tiltAngle,  // ← 7.0이 맞는지 확인
    trainAngle = recommendedTrainAngle
)
```

### 5.3 CoordinateTransformer 사용 방식

#### Keyhole이 아닌 경우
```kotlin
CoordinateTransformer.transformCoordinatesWithTrain(
    azimuth = originalAzimuth,
    elevation = originalElevation,
    tiltAngle = 7.0,
    trainAngle = 0.0  // ← Train 회전 없음
)
```

#### Keyhole인 경우
```kotlin
val trainAngle = calculateTrainAngle(maxAzRateAzimuth)

CoordinateTransformer.transformCoordinatesWithTrain(
    azimuth = originalAzimuth,  // ← 원본 Azimuth 그대로
    elevation = originalElevation,
    tiltAngle = 7.0,
    trainAngle = trainAngle  // ← 계산된 Train 각도 적용
)
```

### 5.4 기대 결과 (패스 6번 예시)

**입력**:
- MaxAzRate 시점 Azimuth: 257.197469°

**계산**:
```
Option 1: 257.197469 - 270 = -12.802531° (범위 OK)
Option 2: -12.802531 + 360 = 347.197469° (범위 NG, >270°)
선택: -12.802531° (option1만 유효)
```

**출력**:
- Train 각도: -12.802531°
- 서쪽(+7°) 위치: -12.802531 + 270 = 257.197469° ✓

---

## 6. 체크리스트

### 6.1 구현 전 확인

- [ ] `SatelliteTrackingProcessor.kt` 파일 백업
- [ ] 현재 `calculateTrainAngle` 함수 로직 확인
- [ ] 현재 `tiltAngle` 설정값 확인
- [ ] CoordinateTransformer 사용 방식 확인

### 6.2 구현 체크리스트

- [ ] `calculateTrainAngle` 함수 수정 완료
  - [ ] 두 옵션(option1, option2) 계산
  - [ ] ±270° 범위 검증
  - [ ] 유효 옵션 중 최소값 선택
- [ ] 로그 메시지 추가 완료
  - [ ] Azimuth 입력값 로그
  - [ ] Option 1, 2 계산 결과 로그
  - [ ] 범위 확인 결과 로그
  - [ ] 최종 선택 Train 각도 로그
- [ ] `applyAxisTransformation` 확인 완료
  - [ ] tiltAngle = 7.0 설정 확인
  - [ ] Keyhole 시에만 trainAngle 적용 확인
  - [ ] 원본 Azimuth 사용 확인
- [ ] 컴파일 성공 확인
  - [ ] `gradlew.bat compileKotlin` 실행
  - [ ] 에러 없음 확인

### 6.3 테스트 체크리스트

- [ ] 단위 테스트
  - [ ] Azimuth 10° → Train 100° 확인
  - [ ] Azimuth 257.197° → Train -12.803° 확인
  - [ ] Azimuth 315° → Train 45° 확인
  - [ ] Azimuth 350° → Train 80° 확인
- [ ] 통합 테스트
  - [ ] 실제 위성 데이터로 테스트
  - [ ] Keyhole 패스 로그 확인
  - [ ] Train 각도 범위 ±270° 확인
- [ ] 결과 검증
  - [ ] 서쪽(+7°) 위치 = Azimuth 확인
  - [ ] MaxAzRate 감소 확인

### 6.4 완료 후 확인

- [ ] 로그 파일 확인 (상세 출력 확인)
- [ ] 성능 테스트 (여러 위성으로 테스트)
- [ ] 문서 업데이트 (결과 기록)
- [ ] 코드 리뷰 요청

---

## 7. 참고 자료

### 7.1 관련 파일
- `SatelliteTrackingProcessor.kt`: Train 각도 계산 메인 로직
- `CoordinateTransformer.kt`: 3축 좌표 변환 (Train, Tilt 적용)
- `LimitAngleCalculator.kt`: 각도 정규화 (±270° 범위)

### 7.2 핵심 개념
- **Keyhole**: 위성이 천정 근처를 지나가며 Azimuth 각속도가 급증하는 현상
- **Train 축**: 안테나 전체를 회전시켜 Azimuth 부담 감소
- **서쪽(+7°)**: 안테나가 기울어진 방향으로, 이 방향이 위성을 향하도록 최적화

### 7.3 주의사항
- Train 각도는 **±270° 범위**를 절대 벗어나면 안 됨 (하드웨어 제한)
- Azimuth는 **원본 그대로** CoordinateTransformer에 전달 (Train 회전은 내부에서 처리)
- tiltAngle은 **7.0** (서쪽으로 기울어진 양수 값)

---

**작성자**: AI Assistant  
**검토자**: (검토 필요)  
**승인자**: (승인 필요)  

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 2025-10-24 | 1.0 | 초안 작성 | AI Assistant |


