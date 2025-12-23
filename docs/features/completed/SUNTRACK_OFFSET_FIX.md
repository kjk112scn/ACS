# SunTrack Train Offset 문제 수정

## 문제 상황
태양 추적(Sun Track) 시작 시 Train 축에 offset을 주면 다음과 같은 비정상 동작이 발생:
1. Train이 먼저 0도로 이동
2. 그 후 다시 목표 지점 + offset 값으로 이동

## 원인 분석

### 핵심 문제
`processInitialTrainMovement()` 메서드에서 **두 가지 불일치** 발생:

1. **CMD.cmdTrainAngle vs sendTrainMovementCommand 파라미터 불일치**
   - `CMD.cmdTrainAngle`에는 `getTrainOffsetCalculator()`로 **offset이 적용된 값** 설정
   - 하지만 `sendTrainMovementCommand()`에는 **offset이 적용되지 않은** `targetTrainAngle`을 직접 전달

2. **동작 시나리오 (offset = 10도 가정)**
   - 첫 번째 사이클: `targetTrainAngle = null`
     * Train 각도 계산: `targetTrainAngle = 180.0`
     * `CMD.cmdTrainAngle = 190.0` (180 + 10 offset)
     * **실제 명령 전송**: `sendTrainMovementCommand(180.0)` ❌ **offset 미적용!**
     * 결과: Train이 **180도**로 이동
   
   - 두 번째 사이클 이후: `targetTrainAngle != null`  
     * 목표 각도 도달 확인: `currentTrainAngle (180.0)` vs `getTrainOffsetCalculator() (190.0)`
     * 차이가 10도이므로 계속 INITIAL_Train 상태 유지
     * 시스템이 CMD.cmdTrainAngle(190.0)을 참조하여 다시 이동

## 수정 내역

### 1. processInitialTrainMovement() 수정 (Line 377-449)

#### 변경 전:
```kotlin
targetTrainAngle = trainResult.angle
CMD.cmdTrainAngle = getTrainOffsetCalculator()!!.toFloat()

// ❌ offset 미적용
GlobalData.SunTrackingData.trainAngle = targetTrainAngle?.toFloat()!!
sendTrainMovementCommand(targetTrainAngle?.toFloat()!!, trainSpeed)
```

#### 변경 후:
```kotlin
targetTrainAngle = trainResult.angle

// ✅ offset 적용된 각도 계산
val offsetAppliedAngle = getTrainOffsetCalculator()!!.toFloat()
CMD.cmdTrainAngle = offsetAppliedAngle

logger.info("🎯 [TRAIN_INIT] Offset 적용 완료:")
logger.info("  - 기준 각도: {}°", String.format("%.3f", targetTrainAngle))
logger.info("  - Train Position Offset: {}°", String.format("%.3f", GlobalData.Offset.trainPositionOffset))
logger.info("  - True North Offset: {}°", String.format("%.3f", GlobalData.Offset.trueNorthOffset))
logger.info("  - 최종 명령 각도: {}°", String.format("%.3f", offsetAppliedAngle))

// ✅ Train 이동 명령 전송 - offset 적용된 값 사용
GlobalData.SunTrackingData.trainAngle = offsetAppliedAngle
sendTrainMovementCommand(offsetAppliedAngle, trainSpeed)
```

### 2. 디버그 로그 추가

#### processInitialTrainMovement()
- `🔧 [TRAIN_INIT]`: Train 초기화 시작
- `📐 [TRAIN_INIT]`: 계산된 Train 기준 각도
- `🎯 [TRAIN_INIT]`: Offset 적용 완료 (상세 정보)
- `📊 [TRAIN_MOVING]`: Train 목표 각도 확인 중
- `🎯 [TRAIN_ARRIVED]`: Train 목표 각도 도달
- `❌ [TRAIN_INIT_ERROR]`: 오류 발생

#### processTrainStabilization() (Line 454-519)
- `⏱️ [TRAIN_STABILIZING]`: 안정화 체크 (디버그 레벨)
- `⏳ [TRAIN_STABILIZING]`: 안정화 대기 중 (5초마다)
- `✅ [TRAIN_STABLE]`: Train 안정화 완료
- `⚠️ [TRAIN_STABILIZING_TIMEOUT]`: 안정화 타임아웃 (5분)
- `🚀 [TRACKING_START]`: 실시간 추적 상태로 전환
- `❌ [TRAIN_DATA_ERROR]`: Train 각도 데이터 없음
- `❌ [TRAIN_STABILIZING_ERROR]`: 안정화 처리 중 오류

#### getTrainOffsetCalculator() (Line 621-642)
- `🧮 [OFFSET_CALC]`: Train Offset 계산 상세 정보
  * 기준 각도
  * Train Position Offset
  * True North Offset
  * 최종 계산 각도
- `⚠️ [OFFSET_CALC]`: targetTrainAngle null 경고

## 테스트 방법

### 1. 로그 확인
SunTrack 시작 시 다음 로그가 순서대로 출력되는지 확인:

```
🔧 [TRAIN_INIT] Train 초기화 시작
📐 [TRAIN_INIT] 계산된 Train 기준 각도: 180.000° (단순화 로직 (동남서 경로))
🧮 [OFFSET_CALC] Train Offset 계산:
  - 기준 각도: 180.000°
  - Train Position Offset: 10.000°
  - True North Offset: 0.000°
  - 최종 계산 각도: 190.000°
🎯 [TRAIN_INIT] Offset 적용 완료:
  - 기준 각도: 180.000°
  - Train Position Offset: 10.000°
  - True North Offset: 0.000°
  - 최종 명령 각도: 190.000°
Train 이동 명령 전송: 190.000000도
✅ [TRAIN_INIT] Train 이동 명령 전송 완료, 안정화 단계 진입
```

### 2. 동작 확인
1. **Offset 없이** SunTrack 시작
   - Train이 계산된 기준 각도로 한 번에 이동
   
2. **Offset 10도 설정 후** SunTrack 시작  
   - Train이 (기준 각도 + 10도)로 한 번에 이동
   - **중간에 0도나 다른 각도로 이동하지 않음**

## 파일 위치
- 수정 파일: `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\service\mode\SunTrackService.kt`
- 백업 파일: `/tmp/suntrack_original.kt`

## 수정 일자
2025-12-20
