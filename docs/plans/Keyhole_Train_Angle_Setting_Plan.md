# Keyhole 위성 추적 Train 각도 설정 개선 계획

## 개요

위성 추적 시 Keyhole 위성인 경우 Train 각도를 `RecommendedTrainAngle`로 설정하고, `currentTrackingPass`를 Keyhole 여부에 따라 적절한 MST로 설정하도록 개선합니다.

## 현재 상태 분석

### 1. 위성 추적 워크플로우

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

### 2. Keyhole 판단 로직 분석

#### 2.1 getEphemerisTrackDtlByMstId() (2574줄)
- `final_transformed` MST에서 `IsKeyhole` 확인 (2577-2587줄)
- Keyhole 여부에 따라 DataType 선택 (2592-2608줄)
  - Keyhole 발생: `keyhole_final_transformed` (Train≠0)
  - Keyhole 미발생: `final_transformed` (Train=0)

#### 2.2 createRealtimeTrackingData() (1145줄)
- 동일한 로직으로 Keyhole 여부 확인 및 DataType 선택 (1161-1190줄)
- 추적 데이터 생성 시 Keyhole 여부에 따라 적절한 데이터 사용 ✅

#### 2.3 getAllEphemerisTrackMstMerged() (2253줄)
- `final_transformed` MST에서 `IsKeyhole` 확인 (2273-2276줄)
- `RecommendedTrainAngle`은 `final_transformed` MST에서 가져옴 (2359줄)

### 3. currentTrackingPass 사용 위치 분석

1. **getCurrentTrackingPassTimes()** (2175줄)
   - `StartTime`, `EndTime` 접근
   - 사용 위치: `trackingSatelliteStateCheck()` (1009줄), `sendInitialTrackingData()` (1869줄)

2. **handleEphemerisTrackingDataRequest()** (2034줄)
   - `passId` 추출: `currentTrackingPass!!["No"]` (2041줄)

3. **sendAdditionalTrackingData()** (2055줄)
   - null 체크만 수행 (2057줄)

4. **getCurrentTrackingPass()** (2236줄)
   - 외부 API로 현재 추적 패스 정보 반환

**결론**: 모든 사용 위치는 Keyhole 여부와 무관하게 동작하지만, 올바른 MST를 가리켜야 `StartTime`, `EndTime`, `IsKeyhole`, `RecommendedTrainAngle` 정보를 정확히 가져올 수 있음

### 4. 2.12.1, 2.12.2, 2.12.3 프로토콜 관련 함수 분석

#### 4.1 sendHeaderTrackingData() (1774줄) - 2.12.1 위성 추적 해더 정보 송신 프로토콜
- **역할**: 추적 시작 시 헤더 정보 전송 (StartTime, EndTime, 데이터 길이)
- **currentTrackingPass 설정** (1779줄): 문제 - DataType 필터링 없음 ❌
- **getEphemerisTrackDtlByMstId() 사용** (1803줄): Keyhole 여부에 따라 적절한 데이터 사용 ✅
- **calculateDataLength() 사용** (1802줄): getEphemerisTrackDtlByMstId() 사용 ✅
- **calculateDataByteSize() 사용** (1843줄): getEphemerisTrackDtlByMstId() 사용 ✅
- **호출 위치**: `handleInProgress()` (1074줄)에서 호출

#### 4.2 sendInitialTrackingData() (1859줄) - 2.12.2 위성 추적 초기 제어 명령 프로토콜
- **역할**: 추적 시작 시 초기 제어 명령 전송 (최대 50개 데이터 포인트)
- **currentTrackingPass null 체크** (1861줄)
- **getEphemerisTrackDtlByMstId() 사용** (1866줄): Keyhole 여부에 따라 적절한 데이터 사용 ✅
- **getCurrentTrackingPassTimes() 사용** (1869줄): currentTrackingPass에서 StartTime, EndTime 가져옴
- **이벤트**: SatelliteTrackHeaderReceived 이벤트 수신 시 호출 (115줄)

#### 4.3 sendAdditionalTrackingData() (2055줄) - 2.12.3 위성 추적 추가 데이터 요청에 대한 응답
- **역할**: ACU F/W로부터 추가 데이터 요청 시 응답 (최대 25개 데이터 포인트)
- **currentTrackingPass null 체크** (2057줄)
- **getEphemerisTrackDtlByMstId() 사용** (2063줄): Keyhole 여부에 따라 적절한 데이터 사용 ✅
- **이벤트**: SatelliteTrackDataRequested 이벤트 수신 시 handleEphemerisTrackingDataRequest() 호출 (126줄)

#### 4.4 handleEphemerisTrackingDataRequest() (2034줄)
- **역할**: ACU F/W로부터 추가 데이터 요청 처리
- **currentTrackingPass에서 passId 추출** (2041줄): `currentTrackingPass!!["No"]`
- **sendAdditionalTrackingData() 호출** (2047줄)

#### 4.5 데이터 계산 함수들
- **calculateDataLength()** (2220줄): getEphemerisTrackDtlByMstId() 사용 ✅
- **calculateDataByteSize()** (2200줄): getEphemerisTrackDtlByMstId() 사용 ✅
- **calculateInitialDataByteSize()** (2208줄): 데이터 포인트 개수 기반 계산

**결론**: 
- 모든 프로토콜 함수들이 `getEphemerisTrackDtlByMstId()`를 사용하므로 Keyhole 여부에 따라 적절한 데이터를 사용함 ✅
- 하지만 `sendHeaderTrackingData()`에서 `currentTrackingPass` 설정 시 DataType 필터링이 없어 문제 발생 ❌
- `currentTrackingPass`가 올바른 MST를 가리켜야 `StartTime`, `EndTime`, `IsKeyhole`, `RecommendedTrainAngle` 정보를 정확히 가져올 수 있음

### 5. 문제점 상세 분석

#### 문제 1: currentTrackingPass 설정 (797줄, 1779줄)
```kotlin
// 현재 코드
currentTrackingPass = ephemerisTrackMstStorage.find { it["No"] == passId }
```
- **문제**: DataType 필터링 없이 첫 번째로 저장된 데이터 반환
- **저장 순서**: original → axis_transformed → final_transformed → keyhole_axis_transformed → keyhole_final_transformed (419-447줄)
- **결과**: `original` 데이터가 반환될 가능성이 높음
- **영향**: `IsKeyhole`, `RecommendedTrainAngle` 정보가 없어 Train 각도 설정 불가

#### 문제 2: Train 각도 설정 (963줄)
```kotlin
// 현재 코드
var trainAngle = 0f  // 무조건 0으로 설정
```
- **문제**: Keyhole 위성인 경우 `RecommendedTrainAngle`을 사용해야 함
- **영향**: Keyhole 위성 추적 시 Train 각도가 0으로 설정되어 추적 실패 가능

### 6. 데이터 흐름 검증

#### 6.1 MST 데이터 구조
- `final_transformed` MST: `IsKeyhole`, `RecommendedTrainAngle` 정보 포함 (2358-2359줄)
- `keyhole_final_transformed` MST: Keyhole 발생 시 생성 (Train≠0 데이터)

#### 6.2 추적 데이터 흐름
- `getEphemerisTrackDtlByMstId()`: Keyhole 여부에 따라 적절한 DTL 데이터 반환 ✅
- `createRealtimeTrackingData()`: Keyhole 여부에 따라 적절한 DataType 사용 ✅
- `moveToStartPosition()`: Keyhole 여부에 따라 적절한 시작 위치 설정 ✅
- **부족**: Train 각도 설정 시 Keyhole 여부 미반영 ❌

## 개선 계획

### 1. 헬퍼 함수 생성

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

### 2. currentTrackingPass 설정 개선

#### 2.1 startEphemerisTracking() (797줄)

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

#### 2.2 sendHeaderTrackingData() (1779줄)

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

### 3. MOVING_TRAIN_TO_ZERO 상태에서 Train 각도 설정 (963줄)

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

## 검증 사항

### 1. 기능 검증
- [ ] Keyhole 위성 추적 시 Train 각도가 RecommendedTrainAngle로 설정되는지 확인
- [ ] Keyhole 미발생 위성 추적 시 Train 각도가 0으로 설정되는지 확인
- [ ] currentTrackingPass가 올바른 MST를 가리키는지 확인
- [ ] 추적 데이터가 Keyhole 여부에 따라 적절한 DataType을 사용하는지 확인 (이미 구현됨)

### 2. 데이터 흐름 검증
- [ ] getTrackingPassMst()가 올바른 MST를 반환하는지 확인
- [ ] currentTrackingPass에서 IsKeyhole, RecommendedTrainAngle 정보를 정확히 가져오는지 확인
- [ ] Train 각도 설정 시 로깅이 정확한지 확인

### 3. 호환성 검증
- [ ] 기존 추적 로직과의 호환성 유지 확인
- [ ] 정지궤도 추적과의 호환성 확인 (정지궤도는 Train=0 사용)

## 영향 범위

### 수정 파일
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

### 수정 위치
1. 헬퍼 함수 추가: 약 2708줄 근처 (`getEphemerisTrackDtlByMstIdAndDataType()` 함수 다음)
2. `startEphemerisTracking()`: 797줄
3. `sendHeaderTrackingData()`: 1779줄
4. `trackingSatelliteStateCheck()`: 960-972줄

### 영향받는 기능
- 위성 추적 시작
- Train 각도 설정
- 추적 헤더 전송
- 상태머신 흐름

## 주의사항

1. `currentTrackingPass`가 null일 수 있으므로 null 체크 필요
2. `RecommendedTrainAngle`이 null일 경우 0.0 사용
3. `IsKeyhole`이 null일 경우 false 사용
4. 기존 로직과의 호환성 유지
5. 정지궤도 추적은 Train=0을 사용하므로 영향 없음

## 구현 순서

1. 헬퍼 함수 `getTrackingPassMst()` 생성
2. `startEphemerisTracking()`에서 `currentTrackingPass` 설정 개선
3. `sendHeaderTrackingData()`에서 `currentTrackingPass` 설정 개선
4. `MOVING_TRAIN_TO_ZERO` 상태에서 Train 각도 설정 개선
5. 로깅 추가 및 검증

## 참고 사항

### 함수 비교

| 함수명 | 반환 타입 | Keyhole 판단 | DataType 선택 | passId 필터링 |
|--------|----------|-------------|--------------|--------------|
| `getEphemerisTrackDtlByMstId()` | List<Map> (DTL) | ✅ | ✅ 동적 선택 | ✅ |
| `getTrackingPassMst()` | Map? (MST) | ✅ | ✅ 동적 선택 | ✅ |
| `getEphemerisTrackMstByDataType()` | List<Map> (MST) | ❌ | ❌ 고정 (파라미터) | ❌ |
| `getAllEphemerisTrackMst()` | List<Map> (MST) | ❌ | ❌ 없음 (전체) | ❌ |

### 주요 사항

- `getTrackingPassMst()` 함수는 **현재 존재하지 않음** - 새로 생성 필요
- `getEphemerisTrackDtlByMstId()` 함수와 동일한 Keyhole 판단 로직 사용
- `getAllEphemerisTrackMstMerged()` 함수의 Keyhole 판단 기준과 일치
- 모든 프로토콜 함수들(2.12.1, 2.12.2, 2.12.3)은 `getEphemerisTrackDtlByMstId()`를 사용하므로 Keyhole 여부에 따라 적절한 데이터 사용 ✅
- DataType은 정해져 있지 않고, Keyhole 여부에 따라 동적으로 선택됨

## 깊은 분석 결과

### 1. 추가 확인 사항

#### 1.1 isTrainAtZero() 함수 (778줄)
- **현재 구현**: `PushData.CMD.cmdTrainAngle`과 현재 Train 각도를 비교하여 0.1도 이내면 true 반환
- **Keyhole 호환성**: ✅ 정상 작동
  - Keyhole의 경우 `moveTrainToZero(RecommendedTrainAngle)` 호출 시 `PushData.CMD.cmdTrainAngle`에 RecommendedTrainAngle 값이 설정됨
  - 현재 Train 각도와 비교하므로 0이 아닌 값(RecommendedTrainAngle)에도 정상 작동
- **결론**: 수정 불필요 ✅

#### 1.2 isTrainStabilized() 함수 (785줄)
- **현재 구현**: `isTrainAtZero()`와 동일한 로직 사용
- **Keyhole 호환성**: ✅ 정상 작동
  - Keyhole의 경우 RecommendedTrainAngle 값에 도달했는지 확인 가능
- **결론**: 수정 불필요 ✅

#### 1.3 moveTrainToZero() 함수 (753줄)
- **현재 구현**: 파라미터로 받은 `TrainAngle`을 사용하여 Train 축 이동 명령 전송
- **Keyhole 호환성**: ✅ 정상 작동
  - Keyhole의 경우 `moveTrainToZero(RecommendedTrainAngle)` 호출 시 RecommendedTrainAngle 값이 전달됨
- **결론**: 수정 불필요 ✅

#### 1.4 정지궤도 추적 (startGeostationaryTracking, 162줄)
- **현재 구현**: `trainAngle = 0.0` 고정 사용
- **Keyhole 영향**: ❌ 영향 없음
  - 정지궤도는 항상 Train=0 사용
  - Keyhole 판단 로직과 무관
- **결론**: 수정 불필요 ✅

#### 1.5 ephemerisTimeOffsetCommand() 함수 (2108줄)
- **현재 구현**: `currentTrackingPassId`만 사용, `currentTrackingPass` 미사용
- **Keyhole 영향**: ❌ 영향 없음
  - `currentTrackingPassId`는 passId만 저장하므로 Keyhole 여부와 무관
- **결론**: 수정 불필요 ✅

#### 1.6 exportMstDataToCsv() 함수 (3352줄)
- **현재 구현**: CSV 내보내기용, 추적 로직과 무관
- **Keyhole 영향**: ❌ 영향 없음
  - 이미 Keyhole 여부에 따라 적절한 DataType 선택 로직 구현됨 (3366-3383줄)
- **결론**: 수정 불필요 ✅

### 2. 로깅 메시지 확인

#### 2.1 현재 로깅 메시지 (971줄)
```kotlin
logger.info("✅ Train가 0도에 도달, 안정화 대기 시작")
```
- **문제**: Keyhole의 경우 "0도"가 아닌 실제 Train 각도 표시 필요
- **수정 필요**: ✅ 플랜에 반영됨 (382줄)

#### 2.2 플랜의 로깅 메시지 (382줄)
```kotlin
logger.info("✅ Train가 ${trainAngle}도에 도달, 안정화 대기 시작")
```
- **상태**: ✅ 이미 플랜에 반영됨

### 3. 모든 currentTrackingPass 사용 위치 재확인

#### 3.1 startEphemerisTracking() (797줄)
- **현재**: `ephemerisTrackMstStorage.find { it["No"] == passId }` - DataType 필터링 없음 ❌
- **수정 필요**: ✅ 플랜에 반영됨

#### 3.2 sendHeaderTrackingData() (1779줄)
- **현재**: `ephemerisTrackMstStorage.find { it["No"] == passId }` - DataType 필터링 없음 ❌
- **수정 필요**: ✅ 플랜에 반영됨

#### 3.3 getCurrentTrackingPassTimes() (2175줄)
- **현재**: `currentTrackingPass`에서 `StartTime`, `EndTime` 접근
- **Keyhole 영향**: ✅ 정상 작동
  - `currentTrackingPass`가 올바른 MST를 가리키면 정상 작동
  - 플랜의 수정으로 해결됨

#### 3.4 handleEphemerisTrackingDataRequest() (2041줄)
- **현재**: `currentTrackingPass!!["No"]`로 passId 추출
- **Keyhole 영향**: ✅ 정상 작동
  - passId는 Keyhole 여부와 무관

#### 3.5 sendAdditionalTrackingData() (2057줄)
- **현재**: `currentTrackingPass` null 체크만 수행
- **Keyhole 영향**: ✅ 정상 작동

#### 3.6 getCurrentTrackingPass() (2236줄)
- **현재**: `currentTrackingPass` 반환
- **Keyhole 영향**: ✅ 정상 작동
  - 올바른 MST를 반환하므로 플랜의 수정으로 해결됨

### 4. 상태머신 흐름 확인

#### 4.1 MOVING_TRAIN_TO_ZERO 상태 (960줄)
- **현재**: `trainAngle = 0f` 고정 ❌
- **수정 필요**: ✅ 플랜에 반영됨

#### 4.2 WAITING_FOR_TRAIN_STABILIZATION 상태 (975줄)
- **현재**: `isTrainStabilized()` 사용
- **Keyhole 호환성**: ✅ 정상 작동 (1.2 참고)

#### 4.3 MOVING_TO_TARGET 상태 (986줄)
- **현재**: `moveToTargetAzEl()` 호출
- **Keyhole 영향**: ❌ 영향 없음
  - Azimuth, Elevation만 제어

#### 4.4 TRACKING_ACTIVE 상태 (997줄)
- **현재**: `saveRealtimeTrackingData()` 호출
- **Keyhole 영향**: ✅ 이미 구현됨
  - `createRealtimeTrackingData()` 내부에서 Keyhole 여부에 따라 적절한 DataType 사용 (1161-1190줄)

### 5. 최종 검증 결과

#### ✅ 수정 불필요한 부분
1. `isTrainAtZero()` 함수 - 이미 Keyhole 호환
2. `isTrainStabilized()` 함수 - 이미 Keyhole 호환
3. `moveTrainToZero()` 함수 - 이미 Keyhole 호환
4. 정지궤도 추적 - Train=0 고정 사용
5. `ephemerisTimeOffsetCommand()` - currentTrackingPass 미사용
6. `exportMstDataToCsv()` - 추적 로직과 무관
7. 프로토콜 함수들(2.12.1, 2.12.2, 2.12.3) - 이미 Keyhole 대응 구현됨

#### ✅ 플랜에 반영된 수정 사항
1. `getTrackingPassMst()` 헬퍼 함수 생성
2. `startEphemerisTracking()`에서 currentTrackingPass 설정 개선
3. `sendHeaderTrackingData()`에서 currentTrackingPass 설정 개선
4. `MOVING_TRAIN_TO_ZERO` 상태에서 Train 각도 설정 개선
5. 로깅 메시지 개선 (0도 → ${trainAngle}도)

#### ✅ 추가 확인 사항
- 모든 currentTrackingPass 사용 위치 확인 완료
- 상태머신 흐름 확인 완료
- 호환성 검증 완료

### 6. 결론

**플랜이 완전합니다.** 모든 필요한 수정 사항이 포함되어 있으며, 추가로 수정할 부분은 없습니다. 구현 시 다음 순서로 진행하면 됩니다:

1. 헬퍼 함수 `getTrackingPassMst()` 생성
2. `startEphemerisTracking()` 수정
3. `sendHeaderTrackingData()` 수정
4. `MOVING_TRAIN_TO_ZERO` 상태 수정
5. 로깅 메시지 수정

