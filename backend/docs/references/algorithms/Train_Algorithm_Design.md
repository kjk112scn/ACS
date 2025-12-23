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
  동쪽(-7°) ← [안테나] → 서쪽(+7°)
    좌측              우측
```

### 1.3 안테나 기울기
- **동쪽 방향**: -7° (사용자 기준 좌측)
- **서쪽 방향**: +7° (사용자 기준 우측)
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

### 4.1 양수 Azimuth (0° ~ 360°, 20개)

| # | Azimuth | Option 1<br>(Az-270) | Option 2 | 범위 확인 | 절댓값 비교 | 선택 Train | 검증 |
|---|---------|---------------------|---------|---------|----------|-----------|------|
| 1 | 0° | -270° | 90° | 둘 다 OK | 270 vs **90** | **90°** | (90+270)%360=0° ✓ |
| 2 | 10° | -260° | 100° | 둘 다 OK | 260 vs **100** | **100°** | (100+270)%360=10° ✓ |
| 3 | 30° | -240° | 120° | 둘 다 OK | 240 vs **120** | **120°** | (120+270)%360=30° ✓ |
| 4 | 45° | -225° | 135° | 둘 다 OK | 225 vs **135** | **135°** | (135+270)%360=45° ✓ |
| 5 | 60° | -210° | 150° | 둘 다 OK | 210 vs **150** | **150°** | (150+270)%360=60° ✓ |
| 6 | 90° | -180° | 180° | 둘 다 OK | **180** vs 180 | **-180°** | -180+270=90° ✓ |
| 7 | 120° | -150° | 210° | 둘 다 OK | **150** vs 210 | **-150°** | -150+270=120° ✓ |
| 8 | 135° | -135° | 225° | 둘 다 OK | **135** vs 225 | **-135°** | -135+270=135° ✓ |
| 9 | 150° | -120° | 240° | 둘 다 OK | **120** vs 240 | **-120°** | -120+270=150° ✓ |
| 10 | 180° | -90° | 270° | 둘 다 OK | **90** vs 270 | **-90°** | -90+270=180° ✓ |
| 11 | 210° | -60° | 300° | option1만 OK | **60** | **-60°** | -60+270=210° ✓ |
| 12 | 225° | -45° | 315° | option1만 OK | **45** | **-45°** | -45+270=225° ✓ |
| 13 | 240° | -30° | 330° | option1만 OK | **30** | **-30°** | -30+270=240° ✓ |
| 14 | 257.197° | -12.803° | 347.197° | option1만 OK | **12.803** | **-12.803°** | -12.803+270=257.197° ✓ |
| 15 | 270° | 0° | -360° | option1만 OK | **0** | **0°** | 0+270=270° ✓ |
| 16 | 300° | 30° | -330° | option1만 OK | **30** | **30°** | 30+270=300° ✓ |
| 17 | 315° | 45° | -315° | option1만 OK | **45** | **45°** | 45+270=315° ✓ |
| 18 | 330° | 60° | -300° | option1만 OK | **60** | **60°** | 60+270=330° ✓ |
| 19 | 350° | 80° | -280° | option1만 OK | **80** | **80°** | 80+270=350° ✓ |
| 20 | 360° | 90° | -270° | 둘 다 OK | **90** vs 270 | **90°** | (90+270)%360=0° ✓ |

### 4.2 음수 Azimuth (0° ~ -360°, 20개)

| # | Azimuth | 정규화<br>(0-360) | Option 1<br>(Az-270) | Option 2 | 범위 확인 | 절댓값 비교 | 선택 Train | 검증 |
|---|---------|------------------|---------------------|---------|---------|----------|-----------|------|
| 21 | 0° | 0° | -270° | 90° | 둘 다 OK | 270 vs **90** | **90°** | (90+270)%360=0° ✓ |
| 22 | -10° | 350° | 80° | -280° | option1만 OK | **80** | **80°** | 80+270=350° ✓ |
| 23 | -30° | 330° | 60° | -300° | option1만 OK | **60** | **60°** | 60+270=330° ✓ |
| 24 | -45° | 315° | 45° | -315° | option1만 OK | **45** | **45°** | 45+270=315° ✓ |
| 25 | -60° | 300° | 30° | -330° | option1만 OK | **30** | **30°** | 30+270=300° ✓ |
| 26 | -90° | 270° | 0° | -360° | option1만 OK | **0** | **0°** | 0+270=270° ✓ |
| 27 | -120° | 240° | -30° | 330° | option1만 OK | **30** | **-30°** | -30+270=240° ✓ |
| 28 | -135° | 225° | -45° | 315° | option1만 OK | **45** | **-45°** | -45+270=225° ✓ |
| 29 | -150° | 210° | -60° | 300° | option1만 OK | **60** | **-60°** | -60+270=210° ✓ |
| 30 | -180° | 180° | -90° | 270° | 둘 다 OK | **90** vs 270 | **-90°** | -90+270=180° ✓ |
| 31 | -210° | 150° | -120° | 240° | 둘 다 OK | **120** vs 240 | **-120°** | -120+270=150° ✓ |
| 32 | -225° | 135° | -135° | 225° | 둘 다 OK | **135** vs 225 | **-135°** | -135+270=135° ✓ |
| 33 | -240° | 120° | -150° | 210° | 둘 다 OK | **150** vs 210 | **-150°** | -150+270=120° ✓ |
| 34 | -257.197° | 102.803° | -167.197° | 192.803° | 둘 다 OK | **167.197** vs 192.803 | **-167.197°** | -167.197+270=102.803° ✓ |
| 35 | -270° | 90° | -180° | 180° | 둘 다 OK | **180** vs 180 | **-180°** | -180+270=90° ✓ |
| 36 | -300° | 60° | -210° | 150° | 둘 다 OK | 210 vs **150** | **150°** | (150+270)%360=60° ✓ |
| 37 | -315° | 45° | -225° | 135° | 둘 다 OK | 225 vs **135** | **135°** | (135+270)%360=45° ✓ |
| 38 | -330° | 30° | -240° | 120° | 둘 다 OK | 240 vs **120** | **120°** | (120+270)%360=30° ✓ |
| 39 | -350° | 10° | -260° | 100° | 둘 다 OK | 260 vs **100** | **100°** | (100+270)%360=10° ✓ |
| 40 | -360° | 0° | -270° | 90° | 둘 다 OK | 270 vs **90** | **90°** | (90+270)%360=0° ✓ |

### 4.3 특수 케이스 분석

#### Case A: 둘 다 유효, Option 2 선택 (1~5번, 21번, 36~40번)
- **Azimuth 범위**: 0° ~ 60° (정규화 후)
- 두 경로 모두 ±270° 범위 내
- Option 1(음수)의 절댓값 > Option 2(양수)
- **최단 거리**: Option 2(양수) 선택

#### Case B: 둘 다 유효, Option 1 선택 (6~10번, 30~35번)
- **Azimuth 범위**: 90° ~ 180° (정규화 후)
- 두 경로 모두 ±270° 범위 내
- Option 1의 절댓값 ≤ Option 2
- **최단 거리**: Option 1 선택

#### Case C: Option 1만 유효 (11~20번, 22~29번)
- **Azimuth 범위**: 210° ~ 360° (정규화 후)
- Option 2가 ±270° 범위 밖 (초과 또는 미만)
- **Option 1만 선택 가능**

#### 구간별 Train 각도 패턴

| Azimuth 구간 | Train 범위 | 특징 |
|-------------|----------|------|
| **0° ~ 90°** | **90° ~ -180°** | Option 2 선택 (양수, 최단) |
| **90° ~ 180°** | **-180° ~ -90°** | Option 1 선택 (음수) |
| **180° ~ 270°** | **-90° ~ 0°** | Option 1만 유효 (음수) |
| **270° ~ 360°** | **0° ~ 90°** | Option 1만 유효 (양수) |

#### 특수 지점

| Azimuth | Train | 의미 |
|---------|-------|------|
| **0° / 360° / -360°** | **90°** | 북쪽 → 동쪽으로 90° 회전 |
| **90° / -270°** | **-180°** | 동쪽 → 남쪽으로 180° 회전 |
| **180° / -180°** | **-90°** | 남쪽 → 서쪽으로 90° 회전 (반시계) |
| **270° / -90°** | **0°** | 서쪽 → 회전 없음 (정렬 상태) |

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

- [ ] 단위 테스트 (40개 예제)
  - [ ] Azimuth 0° → Train 90° 확인 (최단 거리)
  - [ ] Azimuth 10° → Train 100° 확인 (최단 거리)
  - [ ] Azimuth 90° → Train -180° 확인 (최단 거리)
  - [ ] Azimuth 180° → Train -90° 확인 (최단 거리)
  - [ ] Azimuth 257.197° → Train -12.803° 확인
  - [ ] Azimuth 270° → Train 0° 확인
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

## 8. 각속도 표시 개선 계획

### 8.1 현재 상태 분석

#### 백엔드 데이터 흐름
```
1. Original 데이터
   ↓ calculateMetrics(originalDtl)
   - OriginalMaxAzRate (초당 각도, 2축)
   - OriginalMaxElRate
   
2. AxisTransformed 데이터 (Train 적용 후)
   ↓ calculateMetrics(axisTransformedDtl)
   - AxisTransformedMaxAzRate (3축 변환 후)
   - AxisTransformedMaxElRate
   
3. FinalTransformed 데이터 (±270° 제한 후)
   ↓ calculateMetrics(finalTransformedDtl)
   - FinalTransformedMaxAzRate (최종 각속도)
   - FinalTransformedMaxElRate
```

#### 프론트엔드 현재 상태
**파일**: `ACS/src/pages/mode/EphemerisDesignationPage.vue`

**현재 표시 데이터**:
- Line 817-818: `FinalTransformedMaxAzRate`, `FinalTransformedMaxElRate`만 저장
- Line 876-877: CSV에서 `FinalTransformedMaxAzRate`, `FinalTransformedMaxElRate`만 사용
- **문제**: Original (2축) 각속도가 표시되지 않음

---

### 8.2 개선 목표

#### 표시할 데이터

| 항목 | 데이터 소스 | 의미 | 표시 위치 |
|------|-----------|------|---------|
| **2축 최대 Az 속도** | `OriginalMaxAzRate` | Train 적용 전 원본 각속도 | 🎯 중요 (Keyhole 판단 기준) |
| **최종 최대 Az 속도** | `FinalTransformedMaxAzRate` | 각도 제한 후 최종 각속도 | 📊 참고용 (결과 확인) |
| **2축 최대 El 속도** | `OriginalMaxElRate` | Train 적용 전 원본 각속도 | 🎯 중요 |
| **최종 최대 El 속도** | `FinalTransformedMaxElRate` | 각도 제한 후 최종 각속도 | 📊 참고용 |

---

### 8.3 백엔드 수정 계획

#### 파일 1: `SatelliteTrackingProcessor.kt`
**위치**: Line 185-210 (Original Mst 생성)

**현재 코드**:
```kotlin
originalMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "MaxAzRate" to metrics["MaxAzRate"],  // ← Original MaxAzRate
        "MaxElRate" to metrics["MaxElRate"],  // ← Original MaxElRate
        // ... 기존 필드들 ...
        "DataType" to "original"
    )
)
```

**수정 계획**:
```kotlin
originalMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "OriginalMaxAzRate" to metrics["MaxAzRate"],  // ✅ 이름 변경
        "OriginalMaxElRate" to metrics["MaxElRate"],  // ✅ 이름 변경
        "MaxAzRate" to metrics["MaxAzRate"],          // ← 호환성 유지
        "MaxElRate" to metrics["MaxElRate"],          // ← 호환성 유지
        // ... 기존 필드들 ...
        "DataType" to "original"
    )
)
```

#### 파일 2: `SatelliteTrackingProcessor.kt`
**위치**: Line 275-300 (AxisTransformed Mst 생성)

**현재 코드**:
```kotlin
axisTransformedMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "MaxAzRate" to metrics["MaxAzRate"],  // ← AxisTransformed
        "MaxElRate" to metrics["MaxElRate"],
        // ... 기존 필드들 ...
        "DataType" to "axis_transformed"
    )
)
```

**수정 계획**:
```kotlin
axisTransformedMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "AxisTransformedMaxAzRate" to metrics["MaxAzRate"],  // ✅ 추가
        "AxisTransformedMaxElRate" to metrics["MaxElRate"],  // ✅ 추가
        "MaxAzRate" to metrics["MaxAzRate"],                 // ← 호환성 유지
        "MaxElRate" to metrics["MaxElRate"],                 // ← 호환성 유지
        // ... 기존 필드들 ...
        "DataType" to "axis_transformed"
    )
)
```

#### 파일 3: `SatelliteTrackingProcessor.kt`
**위치**: Line 345-370 (FinalTransformed Mst 생성)

**현재 코드**:
```kotlin
finalTransformedMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "MaxAzRate" to metrics["MaxAzRate"],
        "MaxElRate" to metrics["MaxElRate"],
        // ... 기존 필드들 ...
        "DataType" to "final_transformed"
    )
)
```

**수정 계획**:
```kotlin
// ✅ Original 데이터에서 원본 각속도 가져오기
val originalMstData = originalMst.find { it["No"] == mstId }
val originalMaxAzRate = originalMstData?.get("OriginalMaxAzRate") as? Double ?: 0.0
val originalMaxElRate = originalMstData?.get("OriginalMaxElRate") as? Double ?: 0.0

finalTransformedMst.add(
    mapOf(
        // ... 기존 필드들 ...
        "OriginalMaxAzRate" to originalMaxAzRate,           // ✅ 추가 (2축)
        "OriginalMaxElRate" to originalMaxElRate,           // ✅ 추가 (2축)
        "FinalTransformedMaxAzRate" to metrics["MaxAzRate"], // ✅ 추가 (최종)
        "FinalTransformedMaxElRate" to metrics["MaxElRate"], // ✅ 추가 (최종)
        "MaxAzRate" to metrics["MaxAzRate"],                 // ← 호환성 유지
        "MaxElRate" to metrics["MaxElRate"],                 // ← 호환성 유지
        // ... 기존 필드들 ...
        "DataType" to "final_transformed"
    )
)
```

---

### 8.4 프론트엔드 수정 계획

#### 파일: `EphemerisDesignationPage.vue`

**위치 1**: Line 814-819 (selectedScheduleInfo 데이터 매핑)

**현재 코드**:
```typescript
isKeyhole: selected.IsKeyhole || false,
recommendedTrainAngle: selected.RecommendedTrainAngle || 0,
FinalTransformedMaxAzRate: selected.FinalTransformedMaxAzRate || 0,
FinalTransformedMaxElRate: selected.FinalTransformedMaxElRate || 0,
```

**수정 계획**:
```typescript
isKeyhole: selected.IsKeyhole || false,
recommendedTrainAngle: selected.RecommendedTrainAngle || 0,
// ✅ 2축 각속도 추가
OriginalMaxAzRate: selected.OriginalMaxAzRate || 0,
OriginalMaxElRate: selected.OriginalMaxElRate || 0,
// ✅ 최종 각속도 (기존)
FinalTransformedMaxAzRate: selected.FinalTransformedMaxAzRate || 0,
FinalTransformedMaxElRate: selected.FinalTransformedMaxElRate || 0,
```

**위치 2**: Line 837-842 (기본값 정의)

**현재 코드**:
```typescript
isKeyhole: false,
recommendedTrainAngle: 0,
FinalTransformedMaxAzRate: 0,
FinalTransformedMaxElRate: 0,
```

**수정 계획**:
```typescript
isKeyhole: false,
recommendedTrainAngle: 0,
// ✅ 2축 각속도 기본값 추가
OriginalMaxAzRate: 0,
OriginalMaxElRate: 0,
// ✅ 최종 각속도 기본값 (기존)
FinalTransformedMaxAzRate: 0,
FinalTransformedMaxElRate: 0,
```

**위치 3**: KEYHOLE 정보 표시 부분 (템플릿 수정 필요)

**추가할 표시 영역**:
```vue
<template v-if="selectedScheduleInfo.isKeyhole">
  <div class="keyhole-info">
    <h6>🚀 KEYHOLE 정보</h6>
    <div class="info-grid">
      <!-- 2축 각속도 (중요) -->
      <div class="info-item important">
        <span class="label">2축 최대 Az 속도:</span>
        <span class="value">{{ safeToFixed(selectedScheduleInfo.OriginalMaxAzRate, 6) }}°/s</span>
      </div>
      <div class="info-item important">
        <span class="label">2축 최대 El 속도:</span>
        <span class="value">{{ safeToFixed(selectedScheduleInfo.OriginalMaxElRate, 6) }}°/s</span>
      </div>
      
      <!-- Train 각도 -->
      <div class="info-item">
        <span class="label">추천 Train 각도:</span>
        <span class="value">{{ safeToFixed(selectedScheduleInfo.recommendedTrainAngle, 6) }}°</span>
      </div>
      
      <!-- 최종 각속도 (참고용) -->
      <div class="info-item reference">
        <span class="label">최종 최대 Az 속도:</span>
        <span class="value">{{ safeToFixed(selectedScheduleInfo.FinalTransformedMaxAzRate, 6) }}°/s</span>
        <span class="badge">참고</span>
      </div>
      <div class="info-item reference">
        <span class="label">최종 최대 El 속도:</span>
        <span class="value">{{ safeToFixed(selectedScheduleInfo.FinalTransformedMaxElRate, 6) }}°/s</span>
        <span class="badge">참고</span>
      </div>
    </div>
  </div>
</template>
```

**위치 4**: CSV 다운로드 (Line 876-910)

**현재 코드**:
```typescript
const maxAzimuthRate = selectedSchedule?.FinalTransformedMaxAzRate || 0
const maxElevationRate = selectedSchedule?.FinalTransformedMaxElRate || 0

// CSV 헤더
'IsKeyhole', 'RecommendedTrainAngle(°)', 'MaxAzimuthRate(°/s)', 'MaxElevationRate(°/s)'
```

**수정 계획**:
```typescript
// ✅ 2축 각속도 추가
const originalMaxAzRate = selectedSchedule?.OriginalMaxAzRate || 0
const originalMaxElRate = selectedSchedule?.OriginalMaxElRate || 0
const finalMaxAzRate = selectedSchedule?.FinalTransformedMaxAzRate || 0
const finalMaxElRate = selectedSchedule?.FinalTransformedMaxElRate || 0

// CSV 헤더
'IsKeyhole', 'RecommendedTrainAngle(°)',
'OriginalMaxAzRate(°/s)', 'OriginalMaxElRate(°/s)',  // ✅ 추가
'FinalMaxAzRate(°/s)', 'FinalMaxElRate(°/s)'         // ✅ 추가
```

---

### 8.5 스타일 가이드

#### 중요도 표시
```scss
.keyhole-info {
  .info-item {
    &.important {
      border-left: 3px solid #ff9800; // 오렌지 (중요)
      background-color: rgba(255, 152, 0, 0.1);
      
      .label {
        font-weight: 600;
        color: #ff9800;
      }
    }
    
    &.reference {
      border-left: 3px solid #2196f3; // 파랑 (참고)
      background-color: rgba(33, 150, 243, 0.1);
      
      .badge {
        background-color: #2196f3;
        color: white;
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 0.75rem;
      }
    }
  }
}
```

---

### 8.6 구현 순서

#### Step 1: 백엔드 수정
1. ✅ `SatelliteTrackingProcessor.kt` 수정
   - Original Mst에 `OriginalMaxAzRate`, `OriginalMaxElRate` 추가
   - AxisTransformed Mst에 `AxisTransformedMaxAzRate`, `AxisTransformedMaxElRate` 추가
   - Final Mst에 모든 각속도 데이터 포함

#### Step 2: API 응답 확인
1. ✅ 브라우저 개발자 도구에서 응답 데이터 확인
2. ✅ `OriginalMaxAzRate`, `FinalTransformedMaxAzRate` 값 확인

#### Step 3: 프론트엔드 수정
1. ✅ `EphemerisDesignationPage.vue` 타입 정의 수정
2. ✅ KEYHOLE 정보 표시 템플릿 추가
3. ✅ CSV 다운로드 헤더/데이터 수정

#### Step 4: 스타일 적용
1. ✅ 중요도에 따른 시각적 구분
2. ✅ 반응형 레이아웃 적용

#### Step 5: 테스트
1. ✅ Keyhole 위성 선택 시 데이터 표시 확인
2. ✅ CSV 다운로드 시 모든 각속도 포함 확인
3. ✅ 소수점 6자리 표시 확인

---

### 8.7 기대 효과

#### 사용자 관점
- **Keyhole 판단 근거 명확화**: Original 각속도를 보고 왜 Keyhole인지 이해
- **Train 효과 확인**: Original vs Final 각속도 비교로 Train 적용 효과 확인
- **의사결정 지원**: 2개의 각속도 데이터로 더 나은 판단 가능

#### 시스템 관점
- **디버깅 용이**: 각 단계별 각속도 추적 가능
- **검증 강화**: Train 적용 전후 비교로 알고리즘 검증
- **데이터 투명성**: 모든 변환 단계의 각속도 기록

---

## 9. CSV 출력 형식 개선

### 9.1 CSV 헤더 포맷 규칙

- **Train=0**: 소수점 없이 `0`으로 표시
  ```
  FinalTransformed_train0_Azimuth
  FinalTransformed_train0_Elevation
  ```

- **Train≠0**: 소수점 6자리 표시
  ```
  FinalTransformed_train167.867131_Azimuth
  FinalTransformed_train-11.346704_Azimuth
  ```

### 9.2 CSV 헤더 예시

**Keyhole 패스 (Train=167.867131°):**
```csv
Index,Time,Original_Azimuth,Original_Elevation,...,
AxisTransformed_Azimuth,AxisTransformed_Elevation,...,
FinalTransformed_train0_Azimuth,FinalTransformed_train0_Elevation,FinalTransformed_train0_Azimuth_Velocity,FinalTransformed_train0_Elevation_Velocity,
FinalTransformed_train167.867131_Azimuth,FinalTransformed_train167.867131_Elevation,FinalTransformed_train167.867131_Azimuth_Velocity,FinalTransformed_train167.867131_Elevation_Velocity,
Azimuth_Transformation_Error,Elevation_Transformation_Error
```

**일반 패스 (Train=0°):**
```csv
Index,Time,Original_Azimuth,Original_Elevation,...,
AxisTransformed_Azimuth,AxisTransformed_Elevation,...,
FinalTransformed_train0_Azimuth,FinalTransformed_train0_Elevation,FinalTransformed_train0_Azimuth_Velocity,FinalTransformed_train0_Elevation_Velocity,
Azimuth_Transformation_Error,Elevation_Transformation_Error
```

**참고**: Train=0일 때는 `FinalTransformed_train0` 컬럼만 출력하고, `FinalTransformed_train0` 중복 출력하지 않음

### 9.3 구현 단계

#### Step 1: MST에서 recommendedTrainAngle 가져오기

**파일**: `EphemerisService.kt` (Line 2936)

```kotlin
fun exportMstDataToCsv(mstId: Int, outputDirectory: String = "csv_exports"): Map<String, Any?> {
    val mstInfo = getAllEphemerisTrackMst().find { it["No"] == mstId.toUInt() }
    val recommendedTrainAngle = mstInfo?.get("RecommendedTrainAngle") as? Double ?: 0.0
    
    // Train=0일 때는 소수점 제거
    val trainAngleFormatted = if (recommendedTrainAngle == 0.0) {
        "0"
    } else {
        String.format("%.6f", recommendedTrainAngle)
    }
    
    logger.info("📊 Train 각도: $trainAngleFormatted°")
}
```

#### Step 2: CSV 헤더 업데이트

```kotlin
writer.write("Index,Time,")
writer.write("Original_Azimuth,Original_Elevation,Original_Azimuth_Velocity,Original_Elevation_Velocity,")
writer.write("Original_Range,Original_Altitude,")
writer.write("AxisTransformed_Azimuth,AxisTransformed_Elevation,AxisTransformed_Azimuth_Velocity,AxisTransformed_Elevation_Velocity,")

// Train=0 데이터
writer.write("FinalTransformed_train0_Azimuth,FinalTransformed_train0_Elevation,FinalTransformed_train0_Azimuth_Velocity,FinalTransformed_train0_Elevation_Velocity,")

// Train≠0일 때만 추가 컬럼 출력
if (recommendedTrainAngle != 0.0) {
    writer.write("FinalTransformed_train${trainAngleFormatted}_Azimuth,FinalTransformed_train${trainAngleFormatted}_Elevation,FinalTransformed_train${trainAngleFormatted}_Azimuth_Velocity,FinalTransformed_train${trainAngleFormatted}_Elevation_Velocity,")
}

writer.write("Azimuth_Transformation_Error,Elevation_Transformation_Error\n")
```

#### Step 3: 데이터 행 작성

```kotlin
writer.write("$i,${originalTime ?: ""},")
writer.write("$originalAz,$originalEl,$originalAzimuthVelocity,$originalElevationVelocity,")
writer.write("$originalRange,$originalAltitude,")
writer.write("$axisTransformedAz,$axisTransformedEl,$axisTransformedAzimuthVelocity,$axisTransformedElevationVelocity,")

// Train=0 데이터
writer.write("${train0Point["Azimuth"] ?: 0.0},${train0Point["Elevation"] ?: 0.0},$train0AzimuthVelocity,$train0ElevationVelocity,")

// Train≠0일 때만 추가 데이터 출력
if (recommendedTrainAngle != 0.0) {
    writer.write("${trainAnglePoint["Azimuth"] ?: 0.0},${trainAnglePoint["Elevation"] ?: 0.0},$trainAngleAzimuthVelocity,$trainAngleElevationVelocity,")
}

writer.write("$azError,$elError\n")
```

### 9.4 테스트 계획

1. **Train=0° 테스트**
   - 소수점 없이 `FinalTransformed_train0_*` 형식 확인
   - 중복 컬럼 없음 확인
   - 컬럼 수가 정확한지 확인

2. **Train=167.867131° 테스트**
   - `FinalTransformed_train0_*` 컬럼 확인
   - `FinalTransformed_train167.867131_*` 컬럼 확인
   - 소수점 6자리 포맷 확인

3. **Train=-11.346704° 테스트**
   - `FinalTransformed_train0_*` 컬럼 확인
   - `FinalTransformed_train-11icycle704_*` 컬럼 확인
   - 음수 Train 각도 포맷 확인

### 9.5 체크리스트

- [ ] `trainAngleFormatted` 포맷팅 로직 구현 (Train=0일 때 소수점 제거)
- [ ] CSV 헤더 조건부 출력 구현 (Train=0일 때 중복 컬럼 제거)
- [ ] Train=0° 테스트 수행
- [ ] Train≠0° 테스트 수행
- [ ] 음수 Train 각도 테스트 수행
- [ ] CSV 파일 검증

---

**작성자**: AI Assistant  
**검토자**: (검토 필요)  
**승인자**: (승인 필요)  

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 2025-10-27 | 1.3 | CSV 출력 형식 개선 계획 추가 (9장) | AI Assistant |
| 2025-10-27 | 1.2 | 각속도 표시 개선 계획 추가 (8장) | AI Assistant |
| 2025-10-27 | 1.1 | 검증 예제 확장 (20개 → 40개), 최단 거리 알고리즘 검증 완료 | AI Assistant |
| 2025-10-24 | 1.0 | 초안 작성 | AI Assistant |


