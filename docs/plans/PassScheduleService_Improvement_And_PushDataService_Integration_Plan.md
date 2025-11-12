# PassScheduleService 개선 및 PushDataService 통합 계획

## 개요

PassScheduleService에 SatelliteTrackingProcessor를 통합하여 EphemerisService와 동일한 수준의 데이터 처리(2축, 3축, Keyhole 계산)를 지원하고, 상태머신에 Train 각도 설정 로직을 추가하여 Keyhole 위성을 올바르게 추적할 수 있도록 개선합니다. 또한 PushDataService에 개선된 데이터를 반영합니다.

## 현재 상태 분석

### PassScheduleService.kt (개선 필요)

**상태머신 구조**:
- TrackingState: IDLE, WAITING, PREPARING, TRACKING, COMPLETED
- `checkTrackingScheduleWithStateMachine()`: 100ms 주기로 상태 체크 (259줄)
- `executeStateAction()`: 상태별 액션 실행 (368-425줄)
  - TRACKING: `prepareTrackingStart()` 호출 (383줄)
  - PREPARING: `moveToStartPosition()` 호출 (398줄) - Train=0 하드코딩 (650줄)
  - WAITING/COMPLETED: `moveToStowPosition()` 호출 (390, 417줄)

**데이터 처리**:
- `generatePassScheduleTrackingDataAsync()` (1319-1513줄):
  - `OrekitCalculator`로 2축 데이터만 생성 (1331-1340줄)
  - `LimitAngleCalculator`로 ±270도 변환만 수행 (1444-1447줄)
  - `SatelliteTrackingProcessor` 미사용
  - Keyhole 계산 로직 없음
  - `IsKeyhole`, `RecommendedTrainAngle` 필드 없음
  - 단일 DataType만 저장 (변환된 데이터만, 1469-1470줄)

**조회 메서드**:
- `getSelectedTrackMstByMstId()` (1656-1662줄): Keyhole 정보 없음
- `getSelectedTrackDtlByMstId()` (1664-1670줄): Keyhole 정보 없음
- `getTrackingPassMst()` 헬퍼 함수 없음

**문제점**:
1. `moveToStartPosition()`에서 Train 각도가 0으로 하드코딩됨 (650줄)
2. 상태머신에 Train 각도 설정 로직이 없음 (EphemerisService의 `MOVING_TRAIN_TO_ZERO` 상태와 유사한 로직 필요)
3. Keyhole 계산 및 다중 DataType 저장 미지원

### EphemerisService.kt (개선 완료, 참고용)

**데이터 처리**:
- `SatelliteTrackingProcessor` 사용 (46줄)
- 5가지 DataType 저장: `original`, `axis_transformed`, `final_transformed`, `keyhole_axis_transformed`, `keyhole_final_transformed` (425-447줄)
- Keyhole 계산 및 `RecommendedTrainAngle` 포함
- `getTrackingPassMst()`: Keyhole 여부에 따라 동적으로 MST 선택 (2796-2845줄)

**상태머신**:
- `MOVING_TRAIN_TO_ZERO` 상태에서 Keyhole 여부에 따라 Train 각도 동적 설정 (983-1021줄)

### SatelliteTrackingProcessor.kt

- Keyhole 계산 로직 포함 (`MaxAzRate` 기반)
- `RecommendedTrainAngle` 계산
- 모든 변환 파이프라인 제공 (`processFullTransformation()`)

### PushDataService.kt (현재 상태)

- `PassScheduleService` 의존성 없음
- Keyhole 정보 미포함
- 다중 DataType 지원 없음

## 개선 목표

1. **PassScheduleService.kt 개선**
   - `SatelliteTrackingProcessor` 통합
   - Keyhole 계산 및 다중 DataType 저장 지원
   - 상태머신에 Train 각도 설정 로직 추가
   - `EphemerisService.kt`와 동일한 수준의 데이터 처리

2. **PushDataService.kt 개선**
   - `PassScheduleService`의 개선된 데이터 활용
   - Keyhole 정보 및 다중 DataType 지원
   - 실시간 데이터 품질 향상

## 구현 계획

### Phase 1: PassScheduleService.kt 개선

#### 1.1 SatelliteTrackingProcessor 주입

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`

**생성자 수정** (49-56줄):
```kotlin
@Service
class PassScheduleService(
    private val orekitCalculator: OrekitCalculator,
    private val satelliteTrackingProcessor: SatelliteTrackingProcessor, // ✅ 추가
    private val acsEventBus: ACSEventBus,
    private val udpFwICDService: UdpFwICDService,
    private val dataStoreService: DataStoreService,
    private val settingsService: SettingsService,
    private val threadManager: ThreadManager
)
```

#### 1.2 generatePassScheduleTrackingDataAsync() 개선

**현재 (1319-1513줄)**:
- `OrekitCalculator`로 2축 데이터 생성 (1331-1340줄)
- `LimitAngleCalculator`로 ±270도 변환만 수행 (1444-1447줄)
- 단일 DataType 저장 (1469-1470줄)

**개선 후**:
- `OrekitCalculator`로 2축 데이터 생성 (유지)
- `SatelliteTrackingProcessor.processFullTransformation()` 호출
- 5가지 DataType 저장:
  - `original`
  - `axis_transformed`
  - `final_transformed`
  - `keyhole_axis_transformed`
  - `keyhole_final_transformed`

**저장소 구조 개선**:
- 현재: `passScheduleTrackMstStorage[satelliteId] = convertedMst` (단일 DataType, 1469줄)
- 개선: DataType 필드 추가하여 5가지 DataType 모두 저장
- Keyhole 정보 포함 (`IsKeyhole`, `RecommendedTrainAngle`)

**구현 예시**:
```kotlin
// OrekitCalculator로 2축 데이터 생성 (유지)
val schedule = orekitCalculator.generateSatelliteTrackingSchedule(...)

// SatelliteTrackingProcessor로 모든 변환 수행
val processedData = satelliteTrackingProcessor.processFullTransformation(
    schedule,
    satelliteName
)

// 5가지 DataType 저장
passScheduleTrackMstStorage[satelliteId] = processedData.originalMst
passScheduleTrackMstStorage[satelliteId] = processedData.axisTransformedMst
passScheduleTrackMstStorage[satelliteId] = processedData.finalTransformedMst
passScheduleTrackMstStorage[satelliteId] = processedData.keyholeAxisTransformedMst
passScheduleTrackMstStorage[satelliteId] = processedData.keyholeFinalTransformedMst
// DTL도 동일하게 저장
```

#### 1.3 조회 메서드 개선

**getTrackingPassMst() 헬퍼 함수 추가**:
- `EphemerisService.kt`의 `getTrackingPassMst()` (2796-2845줄) 참고
- Keyhole 여부에 따라 동적으로 MST 선택
- `final_transformed` MST에서 `IsKeyhole` 확인
- Keyhole 발생 시: `keyhole_final_transformed` MST 반환
- Keyhole 미발생 시: `final_transformed` MST 반환
- **참고**: `selectedTrackMstStorage`를 사용하는 함수들과 달리, `passScheduleTrackMstStorage`에서 직접 조회

**getSelectedTrackMstByMstId() 개선** (1655-1661줄):
- 현재: `selectedTrackMstStorage`에서 단순 조회 (Keyhole 정보 없음)
- 개선: `getTrackingPassMst()`를 사용하도록 변경하거나, `selectedTrackMstStorage`에 Keyhole 정보 포함
- **참고**: `selectedTrackMstStorage`는 `generateSelectedTrackingData()`에서 생성되므로, 해당 함수도 개선 필요

**getSelectedTrackDtlByMstId() 개선** (1663-1669줄):
- 현재: `selectedTrackMstStorage`에서 MST 조회 후 `passScheduleTrackDtlStorage`에서 DTL 조회 (Keyhole 정보 없음)
- 개선: Keyhole 여부에 따라 적절한 DataType 반환
- `EphemerisService.kt`의 `getEphemerisTrackDtlByMstId()` (2637-2723줄) 참고
- **참고**: `selectedTrackMstStorage`를 사용하므로, `generateSelectedTrackingData()`에서 5가지 DataType 모두 필터링 필요

**generateSelectedTrackingData() 개선** (1617-1645줄):
- 현재: `passScheduleTrackMstStorage`에서 선택된 MST ID만 필터링하여 `selectedTrackMstStorage`에 저장 (단일 DataType)
- 개선: 5가지 DataType 모두 필터링하여 `selectedTrackMstStorage`에 저장
- **영향**: `selectedTrackMstStorage`를 사용하는 모든 함수가 Keyhole 정보를 포함하도록 개선됨

**구현 예시**:
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
 * @see getSelectedTrackDtlByMstId 동일한 Keyhole 판단 로직 사용 (DTL 데이터 반환)
 * @see getAllPassScheduleTrackMstMerged Keyhole 판단 기준과 일치
 *
 * @note 이 함수는 passScheduleTrackMstStorage에서 직접 조회합니다.
 * @note selectedTrackMstStorage를 사용하는 함수들과 달리, 전체 저장소에서 조회합니다.
 * @note DataType은 정해져 있지 않고, Keyhole 여부에 따라 동적으로 선택됩니다.
 */
private fun getTrackingPassMst(passId: UInt): Map<String, Any?>? {
    // 1. final_transformed MST에서 IsKeyhole 확인
    // final_transformed MST에 IsKeyhole 정보가 저장되어 있음
    val finalMst = passScheduleTrackMstStorage.values.flatten().find {
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
        val keyholeMstExists = passScheduleTrackMstStorage.values.flatten().any {
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
    val selectedMst = passScheduleTrackMstStorage.values.flatten().find {
        it["No"] == passId && it["DataType"] == dataType
    }
    
    if (selectedMst == null) {
        logger.error("❌ 패스 ID ${passId}: 선택된 DataType($dataType)의 MST를 찾을 수 없습니다.")
        return null
    }
    
    logger.info("📊 패스 ID ${passId} MST 선택: Keyhole=${if (isKeyhole) "YES" else "NO"}, DataType=${dataType}")
    
    return selectedMst
}

/**
 * 선택된 추적 데이터를 생성합니다.
 *
 * 이 함수는 사용자가 선택한 패스만 필터링하여 selectedTrackMstStorage에 저장합니다.
 * trackingTargetList에 있는 MST ID만 필터링하며, 5가지 DataType 모두 처리합니다.
 *
 * @note 이 함수는 passScheduleTrackMstStorage에서 5가지 DataType 모두 필터링합니다.
 * @note selectedTrackMstStorage를 사용하는 모든 함수가 Keyhole 정보를 포함하도록 개선됩니다.
 */
fun generateSelectedTrackingData() {
    synchronized(trackingTargetList) {
        if (trackingTargetList.isEmpty()) {
            logger.warn("추적 대상 목록이 비어있습니다.")
            selectedTrackMstStorage.clear()
            return
        }

        logger.info("선별된 추적 데이터 생성 시작: ${trackingTargetList.size}개 대상")

        selectedTrackMstStorage.clear()
        val targetMstIds = trackingTargetList.map { it.mstId }.toSet()

        // ✅ 5가지 DataType 모두 필터링
        val dataTypes = listOf(
            "original",
            "axis_transformed",
            "final_transformed",
            "keyhole_axis_transformed",
            "keyhole_final_transformed"
        )

        passScheduleTrackMstStorage.forEach { (satelliteId, allMstData) ->
            val selectedMstData = mutableListOf<Map<String, Any?>>()
            
            // 각 DataType별로 필터링
            dataTypes.forEach { dataType ->
                val filteredByDataType = allMstData.filter { mstRecord ->
                    val mstId = mstRecord["No"] as? UInt
                    val recordDataType = mstRecord["DataType"] as? String
                    mstId != null && targetMstIds.contains(mstId) && recordDataType == dataType
                }
                selectedMstData.addAll(filteredByDataType)
            }

            if (selectedMstData.isNotEmpty()) {
                selectedTrackMstStorage[satelliteId] = selectedMstData
                logger.info("위성 $satelliteId 선별된 패스: ${selectedMstData.size}개 (5가지 DataType 포함)")
            }
        }

        val totalSelectedPasses = selectedTrackMstStorage.values.sumOf { it.size }
        logger.info("선별된 추적 데이터 생성 완료: ${selectedTrackMstStorage.size}개 위성, ${totalSelectedPasses}개 패스 (5가지 DataType 포함)")
    }
}

/**
 * 선택된 패스의 DTL 데이터를 조회합니다.
 *
 * 이 함수는 Keyhole 여부에 따라 적절한 DataType의 DTL 데이터를 반환합니다.
 * selectedTrackMstStorage에서 MST를 조회한 후, Keyhole 여부를 확인하여 적절한 DataType의 DTL을 반환합니다.
 *
 * @param mstId MST ID (패스 ID)
 * @return Keyhole 여부에 따라 선택된 DataType의 DTL 데이터 리스트
 *
 * @see getTrackingPassMst 동일한 Keyhole 판단 로직 사용 (MST 데이터 반환)
 * @see getEphemerisTrackDtlByMstId EphemerisService의 동일한 로직 참고
 */
fun getSelectedTrackDtlByMstId(mstId: UInt): List<Map<String, Any?>> {
    // 1. selectedTrackMstStorage에서 MST 조회
    val selectedMst = getSelectedTrackMstByMstId(mstId) ?: return emptyList()
    
    // 2. final_transformed MST에서 IsKeyhole 확인
    val finalMst = selectedTrackMstStorage.values.flatten().find {
        it["No"] == mstId && it["DataType"] == "final_transformed"
    }
    
    if (finalMst == null) {
        logger.warn("⚠️ MST ID ${mstId}에 해당하는 final_transformed MST 데이터를 찾을 수 없습니다.")
        return emptyList()
    }
    
    // Keyhole 여부 확인 (final_transformed MST의 IsKeyhole 필드 사용)
    val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
    
    // 3. Keyhole 여부에 따라 DataType 선택
    val dataType = if (isKeyhole) {
        // Keyhole 발생 시 keyhole_final_transformed 데이터 존재 여부 확인
        val keyholeDataExists = passScheduleTrackDtlStorage.values.flatten().any {
            it["MstId"] == mstId && it["DataType"] == "keyhole_final_transformed"
        }
        
        if (!keyholeDataExists) {
            logger.warn("⚠️ MST ID ${mstId}: Keyhole로 판단되었으나 keyhole_final_transformed 데이터가 없습니다. final_transformed로 폴백합니다.")
            "final_transformed"  // 폴백
        } else {
            logger.debug("🔑 MST ID ${mstId}: Keyhole 발생 → keyhole_final_transformed 사용")
            "keyhole_final_transformed"
        }
    } else {
        logger.debug("✅ MST ID ${mstId}: Keyhole 미발생 → final_transformed 사용")
        "final_transformed"
    }
    
    // 4. 선택된 DataType의 DTL 데이터 조회
    val satelliteId = selectedMst["SatelliteID"] as? String ?: return emptyList()
    val allDtlData = passScheduleTrackDtlStorage[satelliteId] ?: return emptyList()
    
    val filteredDtl = allDtlData.filter {
        it["MstId"] == mstId && it["DataType"] == dataType
    }
    
    logger.info("📊 MST ID ${mstId} DTL 조회: Keyhole=${if (isKeyhole) "YES" else "NO"}, DataType=${dataType}, ${filteredDtl.size}개 포인트")
    
    return filteredDtl
}
```

#### 1.4 PREPARING 상태에서 Train 회전 로직 추가 (별도 상태 추가 없이)

**사용자 요구사항**: PREPARING 상태에서 Train을 먼저 회전하고, 도착하면 Az/El을 이동하는 로직 필요 (한 번에 움직이면 안됨)

**EphemerisService의 Train 회전 로직 (참고)**:
1. `moveToStartPosition()`: `targetAzimuth`, `targetElevation` 설정 후 `MOVING_TRAIN_TO_ZERO` 상태로 전환
2. `MOVING_TRAIN_TO_ZERO` 상태: Train 먼저 회전 (`moveTrainToZero()` - Train 축만 활성화)
3. `WAITING_FOR_TRAIN_STABILIZATION` 상태: Train 안정화 대기
4. `MOVING_TO_TARGET` 상태: Az/El 이동 (`moveToTargetAzEl()` - Az, El 축만 활성화)

**PassScheduleService 현재 (643-653줄)**:
- `moveToStartPosition()`에서 `moveStartAnglePosition()` 호출하여 Az, El, Train을 동시에 설정
- Train 회전과 Az/El 이동이 분리되지 않음

**개선 후 (사용자 요구사항 반영)**:
- `TrackingState`에 별도 상태 추가하지 않음
- `PREPARING` 상태 내에서 Train 회전 → 안정화 대기 → Az/El 이동을 순차적으로 처리
- 내부 플래그(`preparingStep`, `trainStabilizationStartTime` 등)로 진행 단계 관리
- `checkTrackingScheduleWithStateMachine()`이 100ms 주기로 호출되므로, `PREPARING` 상태에서 단계별 체크 가능

**구현 예시**:
```kotlin
// 내부 플래그 추가 (별도 상태 추가 없이)
private enum class PreparingStep {
    INIT,           // 초기화
    MOVING_TRAIN,   // Train 회전 중
    WAITING_TRAIN,  // Train 안정화 대기
    MOVING_AZ_EL    // Az/El 이동 중
}

private var preparingStep = PreparingStep.INIT
private var preparingPassId: UInt? = null
private var targetAzimuth: Float = 0f
private var targetElevation: Float = 0f
private var trainStabilizationStartTime: Long = 0
private val TRAIN_STABILIZATION_TIMEOUT = 3L // 3초

// moveToStartPosition() 개선
private fun moveToStartPosition(passId: UInt) {
    // ✅ Keyhole 여부에 따라 적절한 MST 선택
    val selectedPass = getTrackingPassMst(passId)
    
    if (selectedPass == null) {
        logger.error("패스 ID ${passId}에 해당하는 데이터를 찾을 수 없습니다.")
        return
    }
    
    // DTL 데이터 조회 (Keyhole 여부에 따라 적절한 DataType)
    val passDetails = getSelectedTrackDtlByMstId(passId)
    
    if (passDetails.isNotEmpty()) {
        val startPoint = passDetails.first()
        targetAzimuth = (startPoint["Azimuth"] as Double).toFloat()
        targetElevation = (startPoint["Elevation"] as Double).toFloat()
        
        // ✅ PREPARING 상태 내에서 Train 회전 시작
        preparingPassId = passId
        preparingStep = PreparingStep.MOVING_TRAIN
        logger.info("📍 시작 위치 이동 준비: Az=${targetAzimuth}°, El=${targetElevation}°")
    }
}

// Train 회전 함수 추가 (EphemerisService 참고)
private fun moveTrainToZero(trainAngle: Float) {
    val multiAxis = BitSet()
    multiAxis.set(2)  // Train 축만 활성화
    udpFwICDService.singleManualCommand(
        multiAxis, trainAngle, 5f
    )
    logger.info("🔄 Train 각도 이동 시작: ${trainAngle}°")
}

// 목표 Az/El로 이동 함수 추가 (EphemerisService 참고)
private fun moveToTargetAzEl() {
    val multiAxis = BitSet()
    multiAxis.set(0)  // Azimuth
    multiAxis.set(1)  // Elevation
    udpFwICDService.multiManualCommand(
        multiAxis, targetAzimuth, 5f, targetElevation, 5f, 0f, 0f
    )
    logger.info("🔄 목표 Az/El로 이동: Az=${targetAzimuth}°, El=${targetElevation}°")
}

// Train 도달 확인 함수 추가
private fun isTrainAtZero(): Boolean {
    val cmdTrain = PushData.CMD.cmdTrainAngle ?: 0f
    val currentTrain = dataStoreService.getLatestData().trainAngle ?: 0.0
    return kotlin.math.abs(cmdTrain - currentTrain.toFloat()) <= 0.1f
}

// Train 안정화 확인 함수 추가
private fun isTrainStabilized(): Boolean {
    val cmdTrain = PushData.CMD.cmdTrainAngle ?: 0f
    val currentTrain = dataStoreService.getLatestData().trainAngle ?: 0.0
    return kotlin.math.abs(cmdTrain - currentTrain.toFloat()) <= 0.1f
}

// executeStateAction() 수정: PREPARING 상태에서 단계별 처리
private fun executeStateAction(
    state: TrackingState,
    currentSchedule: Map<String, Any?>?,
    nextSchedule: Map<String, Any?>?,
    calTime: ZonedDateTime
) {
    when (state) {
        TrackingState.PREPARING -> {
            // ✅ PREPARING 상태 내에서 단계별 처리
            val nextMstId = nextSchedule?.get("No") as? UInt
            
            when (preparingStep) {
                PreparingStep.INIT -> {
                    // 초기화: moveToStartPosition() 호출
                    if (nextMstId != null) {
                        logger.info("[ACTION] PREPARING 상태 - 시작 위치로 이동 (2분 이내)")
                        moveToStartPosition(nextMstId)
                    }
                }
                
                PreparingStep.MOVING_TRAIN -> {
                    // Train 회전 중
                    if (preparingPassId != null) {
                        val selectedPass = getTrackingPassMst(preparingPassId!!)
                        val isKeyhole = selectedPass?.get("IsKeyhole") as? Boolean ?: false
                        val recommendedTrainAngle = selectedPass?.get("RecommendedTrainAngle") as? Double ?: 0.0
                        
                        val trainAngle = if (isKeyhole) {
                            recommendedTrainAngle.toFloat()
                        } else {
                            0f
                        }
                        
                        // Train 각도 이동 명령 전송 (한 번만)
                        moveTrainToZero(trainAngle)
                        
                        // Train 각도 도달 확인
                        if (isTrainAtZero()) {
                            preparingStep = PreparingStep.WAITING_TRAIN
                            trainStabilizationStartTime = System.currentTimeMillis()
                            logger.info("✅ Train가 ${trainAngle}도에 도달, 안정화 대기 시작")
                        }
                    }
                }
                
                PreparingStep.WAITING_TRAIN -> {
                    // Train 안정화 대기
                    if (System.currentTimeMillis() - trainStabilizationStartTime >= TRAIN_STABILIZATION_TIMEOUT && isTrainStabilized()) {
                        moveToTargetAzEl()
                        preparingStep = PreparingStep.MOVING_AZ_EL
                        logger.info("✅ Train 안정화 완료, 목표 Az/El로 이동 시작")
                    }
                }
                
                PreparingStep.MOVING_AZ_EL -> {
                    // Az/El 이동 완료 (목표 위치 도달 체크는 생략, 즉시 완료)
                    preparingStep = PreparingStep.INIT
                    preparingPassId = null
                    logger.info("✅ 목표 위치 이동 완료")
                }
            }
        }
        
        // ... 기타 상태
    }
}
```

#### 1.5 sendHeaderTrackingData() 개선

**현재 (715-759줄)**:
- `getSelectedTrackMstByMstId()` 사용 (718줄) - Keyhole 정보 없음

**개선 후**:
- `getTrackingPassMst()` 사용 (Keyhole 정보 포함)
- `EphemerisService.kt`의 `sendHeaderTrackingData()` (1774-1853줄) 참고

**구현 예시**:
```kotlin
fun sendHeaderTrackingData(passId: UInt) {
    try {
        udpFwICDService.writeNTPCommand()
        
        // ✅ Keyhole 여부에 따라 적절한 MST 선택
        val selectedPass = getTrackingPassMst(passId)
        
        if (selectedPass == null) {
            logger.error("선택된 패스 ID($passId)에 해당하는 데이터를 찾을 수 없습니다.")
            return
        }
        
        // Keyhole 정보 로깅
        val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
        val recommendedTrainAngle = selectedPass["RecommendedTrainAngle"] as? Double ?: 0.0
        logger.info("📊 헤더 전송 패스 정보: Keyhole=${if (isKeyhole) "YES" else "NO"}, RecommendedTrainAngle=${recommendedTrainAngle}°")
        
        // 나머지 로직은 동일
        val startTime = (selectedPass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
        val endTime = (selectedPass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
        // ...
    }
}
```

#### 1.6 sendInitialTrackingData() 개선

**현재 (761-864줄)**:
- `getSelectedTrackDtlByMstId()` 사용 (776줄) - Keyhole 정보 없음

**개선 후**:
- Keyhole 여부에 따라 적절한 DataType 반환
- `EphemerisService.kt`의 `sendInitialTrackingData()` (1859-1995줄) 참고

**구현 예시**:
```kotlin
fun sendInitialTrackingData(passId: UInt) {
    try {
        // ✅ Keyhole 여부에 따라 적절한 MST 선택
        val selectedPass = getTrackingPassMst(passId)
        
        if (selectedPass == null) {
            logger.error("선택된 패스 ID($passId)에 해당하는 데이터를 찾을 수 없습니다.")
            return
        }
        
        // Keyhole 정보 확인
        val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
        
        // ✅ Keyhole 여부에 따라 적절한 DataType의 DTL 조회
        val passDetails = getSelectedTrackDtlByMstId(passId) // 내부에서 Keyhole 여부에 따라 적절한 DataType 반환
        
        // 나머지 로직은 동일
        // ...
    }
}
```

#### 1.7 sendAdditionalTrackingData() 개선 (함수 이름 개선 및 비동기/동기 처리 최적화)

**현재 (874-895줄)**:
- `sendAdditionalTrackingDataOptimized()`: 비동기 처리, 캐시 우선
- `sendFromCache()`: 캐시에서 전송 (최적화된 배열 구조)
- `sendFromDatabase()`: 메모리 저장소에서 전송 (현재는 DB 사용 안 함, 추후 DB 연계 예정)
- `sendAdditionalTrackingDataLegacy()`: 폴백용 (중복)
- `getSelectedTrackDtlByMstId()` 사용 (954줄) - Keyhole 정보 없음

**참고**: `sendFromCache()`와 `sendFromDatabase()`의 차이
- `sendFromCache()`: 최적화된 캐시 구조(`TrackingDataCache`의 `Array<TrackingPoint>`)에서 빠르게 접근
- `sendFromDatabase()`: 일반 메모리 저장소(`ConcurrentHashMap`)에서 가져옴 (현재는 DB 사용 안 함)
- 둘 다 메모리에서 가져오지만, 데이터 구조와 성능이 다름
- 추후 DB 연계 시: `getSelectedTrackDtlByMstId()`만 수정하면 `sendAdditionalTrackingDataFromDatabase()`가 자동으로 DB 연계됨

**문제점**:
1. 함수 이름이 불명확함 (`Optimized`, `FromCache`, `FromDatabase`, `Legacy`)
2. 비동기 처리가 느려서 동기 처리가 필요했음
3. `sendAdditionalTrackingDataLegacy()`와 `sendFromDatabase()`가 중복

**개선 후**:
1. **함수 이름 개선**:
   - `sendAdditionalTrackingDataOptimized()` → `sendAdditionalTrackingData()` (메인 함수)
   - `sendFromCache()` → `sendAdditionalTrackingDataFromCache()` (명확하게)
   - `sendFromDatabase()` → `sendAdditionalTrackingDataFromDatabase()` (명확하게)
   - `sendAdditionalTrackingDataLegacy()` → **제거** (중복이므로)

2. **비동기/동기 처리 최적화**:
   - 캐시 있으면: 동기 처리 (빠름, 즉시 전송)
   - 캐시 없으면: 비동기 처리 (DB 조회는 느릴 수 있으므로 블로킹 방지)
   - 예외 발생 시: 동기 처리로 폴백

3. **Keyhole-aware 데이터 사용**:
   - `getSelectedTrackDtlByMstId()` 사용 (이미 Keyhole-aware로 개선됨)
   - `EphemerisService.kt`의 `sendAdditionalTrackingData()` (2055-2102줄) 참고

### Phase 2: Controller API 개선 (PushDataService 의존성 추가하지 않음)

#### 2.1 getAllPassScheduleTrackMstMerged() 함수 추가

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`

**역할**:
- `EphemerisService.kt`의 `getAllEphemerisTrackMstMerged()` (2316-2461줄) 참고
- 5가지 DataType의 MST 데이터를 병합하여 Keyhole 정보 포함
- 프론트엔드에서 `/pass-schedule/tracking/master` API로 조회 가능

**구현 예시**:
```kotlin
/**
 * 모든 PassSchedule MST 데이터를 병합하여 반환합니다.
 *
 * 이 함수는 5가지 DataType(original, axis_transformed, final_transformed,
 * keyhole_axis_transformed, keyhole_final_transformed)의 MST 데이터를 병합하여
 * Keyhole 정보를 포함한 단일 리스트로 반환합니다.
 *
 * 병합된 데이터에는 다음 정보가 포함됩니다:
 * - Original (2축) 메타데이터: OriginalMaxElevation, OriginalMaxAzRate, OriginalMaxElRate 등
 * - FinalTransformed (3축, Train=0, ±270°) 메타데이터: FinalTransformedMaxAzRate, FinalTransformedMaxElRate 등
 * - KeyholeAxisTransformed (3축, Train≠0) 메타데이터: KeyholeAxisTransformedMaxAzRate 등
 * - KeyholeFinalTransformed (3축, Train≠0, ±270°) 메타데이터: KeyholeFinalTransformedMaxAzRate 등
 * - Keyhole 정보: IsKeyhole, RecommendedTrainAngle
 * - 필터링된 MaxElevation: displayMinElevationAngle 기준으로 필터링된 데이터의 MaxElevation
 *
 * @return 병합된 MST 데이터 리스트 (Keyhole 정보 포함)
 *
 * @see getAllEphemerisTrackMstMerged EphemerisService의 동일한 로직 참고
 * @see getTrackingPassMst Keyhole 판단 기준과 일치
 */
fun getAllPassScheduleTrackMstMerged(): List<Map<String, Any?>> {
    try {
        logger.info("📊 Original, FinalTransformed, KeyholeAxisTransformed, KeyholeFinalTransformed 데이터 병합 시작")
        
        // 5가지 DataType 모두 조회
        val originalMst = passScheduleTrackMstStorage.values.flatten().filter { it["DataType"] == "original" }
        val finalMst = passScheduleTrackMstStorage.values.flatten().filter { it["DataType"] == "final_transformed" }
        val keyholeAxisMst = passScheduleTrackMstStorage.values.flatten().filter { it["DataType"] == "keyhole_axis_transformed" }
        val keyholeMst = passScheduleTrackMstStorage.values.flatten().filter { it["DataType"] == "keyhole_final_transformed" }
        
        if (finalMst.isEmpty()) {
            logger.warn("⚠️ FinalTransformed 데이터가 없습니다")
            return emptyList()
        }
        
        // final_transformed MST 기준으로 병합
        val mergedData = finalMst.map { final ->
            val mstId = final["No"] as UInt
            val original = originalMst.find { it["No"] == mstId }
            val keyholeAxis = keyholeAxisMst.find { it["No"] == mstId }
            val keyhole = keyholeMst.find { it["No"] == mstId }
            
            // Keyhole 판단: final_transformed (Train=0) 기준으로 판단
            val train0MaxAzRate = final["MaxAzRate"] as? Double ?: 0.0
            val threshold = settingsService.keyholeAzimuthVelocityThreshold
            val isKeyhole = train0MaxAzRate >= threshold
            
            // 병합된 데이터 생성 (EphemerisService와 동일한 구조)
            final.toMutableMap().apply {
                // Original (2축) 메타데이터 추가
                put("OriginalMaxElevation", original?.get("MaxElevation"))
                put("OriginalMaxAzRate", original?.get("MaxAzRate"))
                put("OriginalMaxElRate", original?.get("MaxElRate"))
                
                // FinalTransformed 속도 (Train=0, ±270°)
                put("FinalTransformedMaxAzRate", final["MaxAzRate"])
                put("FinalTransformedMaxElRate", final["MaxElRate"])
                
                // Keyhole Axis Transformed 데이터 추가 (각도 제한 ❌, Train≠0)
                if (keyholeAxis != null && isKeyhole) {
                    put("KeyholeAxisTransformedMaxAzRate", keyholeAxis["MaxAzRate"])
                    put("KeyholeAxisTransformedMaxElRate", keyholeAxis["MaxElRate"])
                }
                
                // Keyhole Final Transformed 데이터 추가 (각도 제한 ✅, Train≠0)
                if (keyhole != null && isKeyhole) {
                    put("KeyholeFinalTransformedMaxAzRate", keyhole["MaxAzRate"])
                    put("KeyholeFinalTransformedMaxElRate", keyhole["MaxElRate"])
                }
                
                // Keyhole 정보
                put("IsKeyhole", isKeyhole)
                put("RecommendedTrainAngle", final.get("RecommendedTrainAngle") as? Double ?: 0.0)
            }
        }
        
        logger.info("✅ 병합 완료: ${mergedData.size}개 MST 레코드 (KeyholeAxis + KeyholeFinal 데이터 포함)")
        return mergedData
        
    } catch (error: Exception) {
        logger.error("❌ 데이터 병합 실패: ${error.message}", error)
        return emptyList()
    }
}
```

#### 2.2 Controller API 개선

**파일**: `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/controller/mode/PassScheduleController.kt`

**현재 (727줄)**:
- `getAllPassScheduleTrackMst()` 사용 (Keyhole 정보 없음)
- 반환 형식: `ResponseEntity<Map<String, Any>>` (위성별 그룹화된 구조)
- 프론트엔드: `passScheduleStore.ts`의 `fetchScheduleDataFromServer()`에서 사용 (946-968줄)
- 프론트엔드 타입: `PassScheduleMasterData` 인터페이스 (Keyhole 정보 없음, 95-116줄)
- 프론트엔드 매핑: `ScheduleItem` 인터페이스로 변환 (Keyhole 정보 없음, 15-36줄)

**EphemerisController 비교 (210-218줄)**:
- `getAllEphemerisTrackMstMerged()` 사용 (Keyhole 정보 포함)
- 반환 형식: `Mono<List<Map<String, Any?>>>` (단일 리스트)
- 프론트엔드: `ephemerisTrackService.ts`의 `fetchEphemerisMasterData()`에서 사용 (414-480줄)
- 프론트엔드 타입: `ScheduleItem` 인터페이스 (Keyhole 정보 포함, 41-148줄)
- 프론트엔드 매핑: `FinalTransformedMaxAzRate`, `FinalTransformedMaxElRate`, `IsKeyhole`, `RecommendedTrainAngle`, `KeyholeFinalTransformedMaxAzRate`, `KeyholeFinalTransformedMaxElRate` 등 포함

**개선 후**:
- `getAllPassScheduleTrackMstMerged()` 사용 (Keyhole 정보 포함)
- 반환 형식: `ResponseEntity<Map<String, Any>>` (위성별 그룹화된 구조 유지, 하위 호환성)
- 프론트엔드: `passScheduleStore.ts`의 `fetchScheduleDataFromServer()`에서 매핑 개선 필요
- 프론트엔드 타입: `PassScheduleMasterData` 인터페이스에 Keyhole 정보 필드 추가 필요
- 프론트엔드 매핑: `ScheduleItem` 인터페이스에 Keyhole 정보 필드 추가 필요

**구현 예시**:
```kotlin
/**
 * 전체 PassSchedule 마스터 데이터를 조회합니다.
 *
 * 이 함수는 5가지 DataType의 MST 데이터를 병합하여 Keyhole 정보를 포함한 데이터를 반환합니다.
 * EphemerisController의 `/ephemeris/master` API와 동일한 수준의 정보를 제공합니다.
 *
 * @return ResponseEntity<Map<String, Any>> 위성별로 그룹화된 MST 데이터 (Keyhole 정보 포함)
 *
 * @see EphemerisController.getAllEphemerisTrackMst EphemerisService의 동일한 로직 참고
 * @see PassScheduleService.getAllPassScheduleTrackMstMerged 병합된 데이터 제공
 */
@GetMapping("/tracking/master")
@Operation(
    operationId = "getallpassschedulemasterdata",
    tags = ["Mode - Pass Schedule"]
)
fun getAllTrackingMasterData(): ResponseEntity<Map<String, Any>> {
    return try {
        // ✅ getAllPassScheduleTrackMstMerged() 사용 (Keyhole 정보 포함)
        val allMstData = passScheduleService.getAllPassScheduleTrackMstMerged()

        if (allMstData.isNotEmpty()) {
            // 위성별로 그룹화 (기존 구조 유지, 하위 호환성)
            val satellites = allMstData.groupBy { it["SatelliteID"] as String }
            val totalPasses = allMstData.size

            logger.info("전체 마스터 데이터 조회 성공: ${satellites.size}개 위성, ${totalPasses}개 패스 (Keyhole 정보 포함)")

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "전체 마스터 데이터 조회 성공",
                    "data" to mapOf(
                        "satelliteCount" to satellites.size,
                        "totalPassCount" to totalPasses,
                        "satellites" to satellites
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } else {
            logger.warn("전체 마스터 데이터 없음")
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "추적 데이터가 없습니다. 먼저 추적 데이터를 생성해주세요.",
                    "data" to mapOf(
                        "satelliteCount" to 0,
                        "totalPassCount" to 0,
                        "satellites" to emptyMap<String, Any>()
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    } catch (e: Exception) {
        logger.error("전체 마스터 데이터 조회 실패: ${e.message}", e)
        ResponseEntity.internalServerError().body(
            mapOf(
                "success" to false,
                "message" to "전체 마스터 데이터 조회 중 오류가 발생했습니다: ${e.message}",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}
```

#### 2.3 프론트엔드 타입 및 매핑 개선

**파일**: `ACS/src/services/mode/passScheduleService.ts`

**PassScheduleMasterData 인터페이스 개선** (95-116줄):
- 현재: Keyhole 정보 없음
- 개선: Keyhole 정보 필드 추가
- EphemerisService의 `ScheduleItem` 인터페이스 (41-148줄) 참고

**구현 예시**:
```typescript
/**
 * PassSchedule 마스터 데이터 인터페이스
 *
 * EphemerisService의 ScheduleItem과 동일한 수준의 정보를 포함합니다.
 * Keyhole 정보 및 축 변환 정보를 포함합니다.
 */
export interface PassScheduleMasterData {
  No: number
  SatelliteID: string
  SatelliteName: string
  StartTime: string
  EndTime: string
  Duration: string
  MaxElevation: number
  MaxElevationTime: string
  StartAzimuth: number
  StartElevation: number
  EndAzimuth: number
  EndElevation: number
  MaxAzRate: number
  MaxElRate: number
  MaxAzAccel: number
  MaxElAccel: number
  CreationDate: string
  Creator: string
  OriginalStartAzimuth: number
  OriginalEndAzimuth: number

  // ✅ Keyhole 정보 추가
  IsKeyhole: boolean
  RecommendedTrainAngle: number

  // ✅ Original (2축) 메타데이터 추가
  OriginalMaxElevation?: number
  OriginalMaxAzRate?: number
  OriginalMaxElRate?: number

  // ✅ FinalTransformed (3축, Train=0, ±270°) 메타데이터 추가
  FinalTransformedMaxAzRate?: number
  FinalTransformedMaxElRate?: number
  FinalTransformedStartAzimuth?: number
  FinalTransformedEndAzimuth?: number
  FinalTransformedStartElevation?: number
  FinalTransformedEndElevation?: number
  FinalTransformedMaxElevation?: number

  // ✅ KeyholeAxisTransformed (3축, Train≠0) 메타데이터 추가
  KeyholeAxisTransformedMaxAzRate?: number
  KeyholeAxisTransformedMaxElRate?: number

  // ✅ KeyholeFinalTransformed (3축, Train≠0, ±270°) 메타데이터 추가
  KeyholeFinalTransformedMaxAzRate?: number
  KeyholeFinalTransformedMaxElRate?: number
  KeyholeFinalTransformedStartAzimuth?: number
  KeyholeFinalTransformedEndAzimuth?: number
  KeyholeFinalTransformedStartElevation?: number
  KeyholeFinalTransformedEndElevation?: number
  KeyholeFinalTransformedMaxElevation?: number
}
```

**파일**: `ACS/src/stores/mode/passScheduleStore.ts`

**ScheduleItem 인터페이스 개선** (15-36줄):
- 현재: Keyhole 정보 없음
- 개선: Keyhole 정보 필드 추가
- EphemerisService의 `ScheduleItem` 인터페이스 참고

**구현 예시**:
```typescript
export interface ScheduleItem {
  no: number
  index?: number
  satelliteId?: string
  satelliteName: string
  startTime: string
  endTime: string
  startAzimuthAngle: number
  endAzimuthAngle: number
  startElevationAngle: number
  endElevationAngle: number
  train: number
  duration: string
  maxAzimuthRate?: number
  maxElevationRate?: number
  maxAzimuthAccel?: number
  maxElevationAccel?: number
  originalStartAzimuth?: number
  originalEndAzimuth?: number
  maxElevation?: number
  maxElevationTime?: string

  // ✅ Keyhole 정보 추가
  isKeyhole?: boolean
  IsKeyhole?: boolean // 백엔드 응답 호환성
  recommendedTrainAngle?: number
  RecommendedTrainAngle?: number // 백엔드 응답 호환성

  // ✅ Original (2축) 메타데이터 추가
  OriginalMaxElevation?: number
  OriginalMaxAzRate?: number
  OriginalMaxElRate?: number

  // ✅ FinalTransformed (3축, Train=0, ±270°) 메타데이터 추가
  FinalTransformedMaxAzRate?: number
  FinalTransformedMaxElRate?: number
  FinalTransformedStartAzimuth?: number
  FinalTransformedEndAzimuth?: number
  FinalTransformedStartElevation?: number
  FinalTransformedEndElevation?: number
  FinalTransformedMaxElevation?: number

  // ✅ KeyholeAxisTransformed (3축, Train≠0) 메타데이터 추가
  KeyholeAxisTransformedMaxAzRate?: number
  KeyholeAxisTransformedMaxElRate?: number

  // ✅ KeyholeFinalTransformed (3축, Train≠0, ±270°) 메타데이터 추가
  KeyholeFinalTransformedMaxAzRate?: number
  KeyholeFinalTransformedMaxElRate?: number
  KeyholeFinalTransformedStartAzimuth?: number
  KeyholeFinalTransformedEndAzimuth?: number
  KeyholeFinalTransformedStartElevation?: number
  KeyholeFinalTransformedEndElevation?: number
  KeyholeFinalTransformedMaxElevation?: number
}
```

**fetchScheduleDataFromServer() 매핑 개선** (946-968줄):
- 현재: Keyhole 정보 매핑 없음
- 개선: Keyhole 정보 및 축 변환 정보 매핑 추가
- EphemerisService의 `fetchEphemerisMasterData()` (414-480줄) 참고

**구현 예시**:
```typescript
passes.forEach((pass: PassScheduleMasterData) => {
  try {
    const scheduleItem: ScheduleItem = {
      no: pass.No,
      satelliteId: pass.SatelliteID || satelliteId,
      satelliteName: pass.SatelliteName || satelliteId,
      startTime: pass.StartTime || '',
      endTime: pass.EndTime || '',
      duration: pass.Duration || '00:00:00',
      startAzimuthAngle: pass.StartAzimuth || 0,
      endAzimuthAngle: pass.EndAzimuth || 0,
      startElevationAngle: pass.StartElevation || 0,
      endElevationAngle: pass.EndElevation || 0,
      train: 0,
      maxElevation: pass.MaxElevation || 0,
      maxElevationTime: pass.MaxElevationTime || '',
      maxAzimuthRate: pass.MaxAzRate || 0,
      maxElevationRate: pass.MaxElRate || 0,
      maxAzimuthAccel: pass.MaxAzAccel || 0,
      maxElevationAccel: pass.MaxElAccel || 0,
      originalStartAzimuth: pass.OriginalStartAzimuth || 0,
      originalEndAzimuth: pass.OriginalEndAzimuth || 0,

      // ✅ Keyhole 정보 매핑
      isKeyhole: pass.IsKeyhole || false,
      IsKeyhole: pass.IsKeyhole,
      recommendedTrainAngle: pass.RecommendedTrainAngle || 0,
      RecommendedTrainAngle: pass.RecommendedTrainAngle,

      // ✅ Original (2축) 메타데이터 매핑
      OriginalMaxElevation: pass.OriginalMaxElevation,
      OriginalMaxAzRate: pass.OriginalMaxAzRate,
      OriginalMaxElRate: pass.OriginalMaxElRate,

      // ✅ FinalTransformed (3축, Train=0, ±270°) 메타데이터 매핑
      FinalTransformedMaxAzRate: pass.FinalTransformedMaxAzRate,
      FinalTransformedMaxElRate: pass.FinalTransformedMaxElRate,
      FinalTransformedStartAzimuth: pass.FinalTransformedStartAzimuth,
      FinalTransformedEndAzimuth: pass.FinalTransformedEndAzimuth,
      FinalTransformedStartElevation: pass.FinalTransformedStartElevation,
      FinalTransformedEndElevation: pass.FinalTransformedEndElevation,
      FinalTransformedMaxElevation: pass.FinalTransformedMaxElevation,

      // ✅ KeyholeAxisTransformed (3축, Train≠0) 메타데이터 매핑
      KeyholeAxisTransformedMaxAzRate: pass.KeyholeAxisTransformedMaxAzRate,
      KeyholeAxisTransformedMaxElRate: pass.KeyholeAxisTransformedMaxElRate,

      // ✅ KeyholeFinalTransformed (3축, Train≠0, ±270°) 메타데이터 매핑
      KeyholeFinalTransformedMaxAzRate: pass.KeyholeFinalTransformedMaxAzRate,
      KeyholeFinalTransformedMaxElRate: pass.KeyholeFinalTransformedMaxElRate,
      KeyholeFinalTransformedStartAzimuth: pass.KeyholeFinalTransformedStartAzimuth,
      KeyholeFinalTransformedEndAzimuth: pass.KeyholeFinalTransformedEndAzimuth,
      KeyholeFinalTransformedStartElevation: pass.KeyholeFinalTransformedStartElevation,
      KeyholeFinalTransformedEndElevation: pass.KeyholeFinalTransformedEndElevation,
      KeyholeFinalTransformedMaxElevation: pass.KeyholeFinalTransformedMaxElevation,
    }

    allSchedules.push(scheduleItem)
  } catch (itemError) {
    console.error(`❌ 스케줄 아이템 생성 실패:`, itemError)
  }
})
```

#### 2.4 SelectScheduleContent.vue 컬럼 추가

**파일**: `ACS/src/components/content/SelectScheduleContent.vue`

**현재 (448-493줄)**:
- 컬럼: index, no, satelliteId, satelliteName, startTime, endTime, duration, maxElevation, azimuthAngles
- Keyhole 정보 컬럼 없음
- 축 변환 정보 컬럼 없음

**EphemerisDesignationPage 비교 (658-811줄)**:
- 컬럼: No, SatelliteName, StartTime, EndTime, Duration, OriginalMaxElevation, Train0MaxElevation, MaxElevation, OriginalMaxAzRate, Train0MaxAzRate, FinalTransformedMaxAzRate, OriginalMaxElRate, Train0MaxElRate, FinalTransformedMaxElRate, isKeyhole, recommendedTrainAngle
- Keyhole 정보 컬럼: `isKeyhole`, `recommendedTrainAngle`
- 축 변환 정보 컬럼: `OriginalMaxAzRate`, `FinalTransformedMaxAzRate`, `KeyholeFinalTransformedMaxAzRate` (템플릿에서 동적 표시)

**개선 후**:
- Keyhole 정보 컬럼 추가: `isKeyhole`, `recommendedTrainAngle`
- 2축 정보 컬럼 추가: `OriginalMaxElevation`, `OriginalMaxAzRate`, `OriginalMaxElRate`
- 3축 정보 컬럼 추가 (Train=0, ±270°): `Train0MaxElevation`, `Train0MaxAzRate`, `Train0MaxElRate`
- 최종 정보 컬럼 추가 (Keyhole에 따라 동적): `MaxElevation`, `FinalTransformedMaxAzRate`, `FinalTransformedMaxElRate`
- EphemerisDesignationPage와 동일한 수준의 정보 표시 (모든 컬럼 및 템플릿 포함)

**구현 예시** (EphemerisDesignationPage.vue의 scheduleColumns 참고):
```typescript
const scheduleColumns: QTableColumn[] = [
  // 기본 정보
  { name: 'index', label: 'Index', field: 'index', align: 'left' as const, sortable: true, style: 'width: 70px' },
  { name: 'no', label: 'No', field: 'no', align: 'left' as const, sortable: true, style: 'width: 60px' },
  { name: 'satelliteId', label: '위성 ID', field: 'satelliteId', align: 'center' as const, sortable: true, style: 'width: 100px' },
  { name: 'satelliteName', label: '위성명', field: 'satelliteName', align: 'left' as const, sortable: true },
  {
    name: 'startTime',
    label: '시작 시간',
    field: 'startTime',
    align: 'left' as const,
    sortable: true,
    style: 'width: 150px',
    format: (val: string) => formatToLocalTime(val)
  },
  {
    name: 'endTime',
    label: '종료 시간',
    field: 'endTime',
    align: 'left' as const,
    sortable: true,
    style: 'width: 150px',
    format: (val: string) => formatToLocalTime(val)
  },
  {
    name: 'duration',
    label: '지속 시간',
    field: 'duration',
    align: 'center' as const,
    sortable: true,
    style: 'width: 80px',
    format: (val: string) => formatDuration(val)
  },
  // ✅ 2축 최대 고도 (Original)
  {
    name: 'OriginalMaxElevation',
    label: '2축 최대 고도 (°)',
    field: 'OriginalMaxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 120px',
    format: (val: number | undefined) => val?.toFixed(6) || '-'
  },
  // ✅ 3축 최대 고도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxElevation',
    label: '3축 최대 고도 (°)',
    field: 'FinalTransformedMaxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 120px',
    format: (val: number | undefined) => val?.toFixed(6) || '0.000000'
  },
  // ✅ FinalTransformed 최대 고도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'MaxElevation',
    label: '최대 고도 (°)',
    field: 'FinalTransformedMaxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 100px',
    format: (val: number | undefined) => val?.toFixed(6) || '0.000000'
  },
  // ✅ 2축 최대 Az 속도
  {
    name: 'OriginalMaxAzRate',
    label: '2축 최대 Az 속도 (°/s)',
    field: 'OriginalMaxAzRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 140px',
    format: (val: number | undefined) => val?.toFixed(6) || '-'
  },
  // ✅ 3축 최대 Az 속도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxAzRate',
    label: '3축 최대 Az 속도 (°/s)',
    field: 'FinalTransformedMaxAzRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 140px',
    format: (val: number | undefined) => val?.toFixed(6) || '0.000000'
  },
  // ✅ FinalTransformed 최대 Az 속도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'FinalTransformedMaxAzRate',
    label: '최대 Az 속도 (°/s)',
    field: 'FinalTransformedMaxAzRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 130px',
    format: (val: number | undefined) => val?.toFixed(6) || '0.000000'
  },
  // ✅ 2축 최대 El 속도
  {
    name: 'OriginalMaxElRate',
    label: '2축 최대 El 속도 (°/s)',
    field: 'OriginalMaxElRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 140px',
    format: (val: number | undefined) => val?.toFixed(6) || '-'
  },
  // ✅ 3축 최대 El 속도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxElRate',
    label: '3축 최대 El 속도 (°/s)',
    field: 'FinalTransformedMaxElRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 140px',
    format: (val: number | undefined) => val?.toFixed(6) || '0.000000'
  },
  // ✅ FinalTransformed 최대 El 속도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'FinalTransformedMaxElRate',
    label: '최대 El 속도 (°/s)',
    field: 'FinalTransformedMaxElRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 130px',
    format: (val: number | undefined) => val?.toFixed(6) || '0.000000'
  },
  // ✅ Keyhole 정보 컬럼 추가
  {
    name: 'isKeyhole',
    label: 'KEYHOLE',
    field: 'isKeyhole',
    align: 'center' as const,
    sortable: true,
    style: 'width: 80px',
    format: (val: boolean) => val ? 'YES' : 'NO'
  },
  {
    name: 'recommendedTrainAngle',
    label: 'Train 각도 (°)',
    field: 'recommendedTrainAngle',
    align: 'center' as const,
    sortable: true,
    style: 'width: 100px',
    format: (val: number | undefined, row: ScheduleItem) => row.isKeyhole ? (val?.toFixed(6) || '-') : '-'
  },
]
```

**템플릿 개선** (EphemerisDesignationPage.vue의 템플릿 참고):
```vue
<!-- ✅ 2축 최대 고도 템플릿 (Original) -->
<template v-slot:body-cell-OriginalMaxElevation="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold text-blue-3">
        {{ safeToFixed(props.value, 6) }}°
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ 3축 최대 고도 템플릿 (Train=0, ±270°, 항상 고정) -->
<template v-slot:body-cell-Train0MaxElevation="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold text-green-3">
        {{ safeToFixed(props.value, 6) }}°
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ FinalTransformed 최대 고도 템플릿 (Keyhole에 따라 다른 값 표시) -->
<template v-slot:body-cell-MaxElevation="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold" :class="props.row?.isKeyhole ? 'text-red' : 'text-green-3'">
        {{ safeToFixed(
          props.row?.isKeyhole
            ? (props.row?.KeyholeFinalTransformedMaxElevation ?? props.value ?? 0)
            : (props.value ?? 0),
          6
        ) }}°
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ 2축 최대 Az 속도 템플릿 -->
<template v-slot:body-cell-OriginalMaxAzRate="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold text-blue-3">
        {{ safeToFixed(props.value, 6) }}°/s
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ 3축 최대 Az 속도 템플릿 (Train=0, ±270°, 항상 고정) -->
<template v-slot:body-cell-Train0MaxAzRate="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold text-green-3">
        {{ safeToFixed(props.value, 6) }}°/s
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ FinalTransformed 최대 Az 속도 템플릿 (Keyhole에 따라 다른 값 표시) -->
<template v-slot:body-cell-FinalTransformedMaxAzRate="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold" :class="props.row?.isKeyhole ? 'text-red' : 'text-green-3'">
        {{ safeToFixed(
          props.row?.isKeyhole
            ? (props.row?.KeyholeFinalTransformedMaxAzRate ?? props.value ?? 0)
            : (props.value ?? 0),
          6
        ) }}°/s
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ 2축 최대 El 속도 템플릿 -->
<template v-slot:body-cell-OriginalMaxElRate="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold text-blue-3">
        {{ safeToFixed(props.value, 6) }}°/s
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ 3축 최대 El 속도 템플릿 (Train=0, ±270°, 항상 고정) -->
<template v-slot:body-cell-Train0MaxElRate="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold text-green-3">
        {{ safeToFixed(props.value, 6) }}°/s
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ FinalTransformed 최대 El 속도 템플릿 (Keyhole에 따라 다른 값 표시) -->
<template v-slot:body-cell-FinalTransformedMaxElRate="props">
  <q-td :props="props">
    <div class="text-center">
      <div class="text-weight-bold" :class="props.row?.isKeyhole ? 'text-red' : 'text-green-3'">
        {{ safeToFixed(
          props.row?.isKeyhole
            ? (props.row?.KeyholeFinalTransformedMaxElRate ?? props.value ?? 0)
            : (props.value ?? 0),
          6
        ) }}°/s
      </div>
    </div>
  </q-td>
</template>

<!-- ✅ KEYHOLE 배지 템플릿 추가 -->
<template v-slot:body-cell-satelliteName="props">
  <q-td :props="props">
    <div class="flex items-center">
      <span>{{ props.value || props.row?.satelliteId || '이름 없음' }}</span>
      <q-badge v-if="props.row?.isKeyhole" color="red" class="q-ml-sm" label="KEYHOLE" />
    </div>
  </q-td>
</template>

<!-- ✅ Train 각도 템플릿 추가 -->
<template v-slot:body-cell-recommendedTrainAngle="props">
  <q-td :props="props">
    <span v-if="props.row?.isKeyhole" class="text-positive">
      {{ safeToFixed(props.value, 6) }}°
    </span>
    <span v-else class="text-grey">-</span>
  </q-td>
</template>
```

## 데이터 흐름

### 현재 (PassScheduleService)

```
OrekitCalculator (2축)
  ↓
LimitAngleCalculator (±270도 변환)
  ↓
단일 DataType 저장
  ↓
상태머신 (Train=0 하드코딩)
```

### 개선 후 (PassScheduleService)

```
OrekitCalculator (2축)
  ↓
SatelliteTrackingProcessor.processFullTransformation()
  ├─ Original (2축)
  ├─ Axis Transformed (3축, Train=0)
  ├─ Final Transformed (3축, Train=0, ±270도)
  ├─ Keyhole Axis Transformed (3축, Train≠0)
  └─ Keyhole Final Transformed (3축, Train≠0, ±270도)
  ↓
5가지 DataType 저장 (Keyhole 정보 포함)
  ↓
상태머신 (Keyhole 여부에 따라 Train 각도 동적 설정)
```

## 주의사항

1. **하위 호환성**: 기존 API는 유지하되, 내부적으로 개선된 데이터 사용
2. **성능**: 다중 DataType 저장으로 인한 메모리 사용량 증가 고려
3. **테스트**: Keyhole 계산 로직 검증 필수
4. **상태머신**: Train 각도 설정 시점 확인 필요 (질문 1 참고)

## 구현 순서 (컴파일 확인 포함)

### 수행 가이드

**전체 프로세스**:
1. 각 Phase를 순서대로 진행 (Phase 1 → Phase 8)
2. 각 Step을 순서대로 진행 (의존성 확인 필수)
3. 각 Step 완료 후 반드시 컴파일 확인
4. 컴파일 성공 후 다음 Step 진행
5. 모든 함수에 KDOC 주석 작성 (각 Step의 예시 참고)

**컴파일 확인 방법**:
- 백엔드: `./gradlew compileKotlin` (각 Step마다 수행)
- 프론트엔드: `npm run build` (Phase 8만 수행)
- 전체 빌드: 최종 확인 시 `./gradlew build` 및 `npm run build`

**KDOC 주석 작성 규칙**:
- 모든 새로 추가되는 함수에는 반드시 KDOC 주석 작성
- 각 Step의 "KDOC 주석 예시" 섹션 참고
- 필수 항목:
  - 함수 역할 설명 (한 줄 요약 + 상세 설명)
  - `@param`: 모든 파라미터 설명
  - `@return`: 반환값 설명
  - `@see`: 관련 함수 참조
  - `@note`: 주의사항 또는 특이사항

**문제 발생 시**:
- 컴파일 오류: 해당 Step의 구현을 다시 검토
- 런타임 오류: 로그 확인 및 디버깅
- 의존성 문제: 앞 단계 완료 여부 확인

### Phase 1: 기본 인프라 구축 (컴파일 확인 필수)

#### Step 1.1: SatelliteTrackingProcessor 주입
**파일**: `PassScheduleService.kt` (49-56줄)

**작업 내용**:
- 생성자에 `satelliteTrackingProcessor: SatelliteTrackingProcessor` 추가
- `LimitAngleCalculator`는 유지 (SatelliteTrackingProcessor가 ±270도 변환도 포함하지만, 기존 코드와의 호환성을 위해 유지)

**컴파일 확인**:
```bash
# Gradle 빌드 실행
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (SatelliteTrackingProcessor는 이미 Spring Bean으로 등록되어 있음)

**의존성**: 없음 (가장 먼저 수행)

---

#### Step 1.2: determineKeyholeDataType() 헬퍼 함수 추가
**파일**: `PassScheduleService.kt` (새로 추가, getTrackingPassMst() 앞에 배치)

**작업 내용**:
- Keyhole 판단 로직을 공통 헬퍼 함수로 추출
- `passScheduleTrackMstStorage`와 `selectedTrackMstStorage` 모두에서 사용 가능하도록 구현

**KDOC 주석 예시**:
```kotlin
/**
 * Keyhole 여부를 확인하고 적절한 DataType을 반환합니다.
 *
 * 이 함수는 final_transformed MST에서 IsKeyhole 정보를 확인하여,
 * Keyhole 발생 시 keyhole_final_transformed, 미발생 시 final_transformed를 반환합니다.
 *
 * @param passId 패스 ID (MST ID)
 * @param storage 조회할 저장소 (passScheduleTrackMstStorage 또는 selectedTrackMstStorage)
 * @return Keyhole 여부에 따라 선택된 DataType ("keyhole_final_transformed" 또는 "final_transformed"), 없으면 null
 *
 * @see getTrackingPassMst 이 함수에서 사용하여 MST 선택
 * @see getSelectedTrackDtlByMstId 이 함수에서 사용하여 DTL 선택
 *
 * @note final_transformed MST에 IsKeyhole 정보가 저장되어 있어야 함
 * @note keyhole_final_transformed 데이터가 없으면 final_transformed로 폴백
 */
private fun determineKeyholeDataType(
    passId: UInt,
    storage: Map<String, List<Map<String, Any?>>>
): String? {
    // 구현 내용...
}
```

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (독립적인 함수이므로 다른 코드에 영향 없음)

**의존성**: Step 1.1 완료 필요

---

### Phase 2: 데이터 생성 및 저장 개선 (컴파일 확인 필수)

#### Step 2.1: generatePassScheduleTrackingDataAsync() 개선
**파일**: `PassScheduleService.kt` (1319-1513줄)

**작업 내용**:
- `OrekitCalculator`로 2축 데이터 생성 (유지)
- `SatelliteTrackingProcessor.processFullTransformation()` 호출
- `LimitAngleCalculator` 제거 (SatelliteTrackingProcessor가 ±270도 변환 포함)
- 5가지 DataType 모두 저장 (저장소 구조 변경)

**저장소 구조 변경**:
```kotlin
// 현재 (1469-1470줄)
passScheduleTrackMstStorage[satelliteId] = convertedMst  // 단일 DataType

// 개선 후
val allMstData = mutableListOf<Map<String, Any?>>()
allMstData.addAll(processedData.originalMst)
allMstData.addAll(processedData.axisTransformedMst)
allMstData.addAll(processedData.finalTransformedMst)
allMstData.addAll(processedData.keyholeAxisTransformedMst)
allMstData.addAll(processedData.keyholeFinalTransformedMst)
passScheduleTrackMstStorage[satelliteId] = allMstData  // 5가지 DataType 모두 저장

// DTL도 동일하게 저장
val allDtlData = mutableListOf<Map<String, Any?>>()
allDtlData.addAll(processedData.originalDtl)
allDtlData.addAll(processedData.axisTransformedDtl)
allDtlData.addAll(processedData.finalTransformedDtl)
allDtlData.addAll(processedData.keyholeAxisTransformedDtl)
allDtlData.addAll(processedData.keyholeFinalTransformedDtl)
passScheduleTrackDtlStorage[satelliteId] = allDtlData
```

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (저장소 타입은 `List<Map<String, Any?>>`이므로 변경 없음)

**의존성**: Step 1.1, Step 1.2 완료 필요

**주의사항**: 
- `LimitAngleCalculator` 사용하는 다른 부분이 있는지 확인 필요
- 현재는 `generatePassScheduleTrackingDataAsync()`에서만 사용하므로 제거 가능

---

### Phase 3: 조회 메서드 개선 (컴파일 확인 필수)

#### Step 3.1: getTrackingPassMst() 헬퍼 함수 추가
**파일**: `PassScheduleService.kt` (새로 추가, getSelectedTrackMstByMstId() 앞에 배치)

**작업 내용**:
- `EphemerisService.kt`의 `getTrackingPassMst()` (2796-2845줄) 참고
- `passScheduleTrackMstStorage`에서 직접 조회 (위성별 리스트 구조 고려)
- `determineKeyholeDataType()` 사용

**KDOC 주석**: 
- 계획 파일의 1.3 섹션 (166-239줄)에 상세한 KDOC 주석 예시가 포함되어 있음
- 반드시 해당 예시를 참고하여 작성할 것

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (독립적인 함수이므로 다른 코드에 영향 없음)

**의존성**: Step 1.2, Step 2.1 완료 필요

---

#### Step 3.2: generateSelectedTrackingData() 개선
**파일**: `PassScheduleService.kt` (1618-1646줄)

**작업 내용**:
- 5가지 DataType 모두 필터링하여 `selectedTrackMstStorage`에 저장
- DataType 필드 확인 추가

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (저장소 타입 변경 없음)

**의존성**: Step 2.1 완료 필요

**주의사항**: 
- `selectedTrackMstStorage`를 사용하는 모든 함수에 영향
- `getCurrentSelectedTrackingPassWithTime()`에서 DataType 필터링 필요할 수 있음

---

#### Step 3.3: getSelectedTrackDtlByMstId() 개선
**파일**: `PassScheduleService.kt` (1664-1670줄)

**작업 내용**:
- `determineKeyholeDataType()` 사용하여 Keyhole 여부 확인
- Keyhole 여부에 따라 적절한 DataType 반환
- `EphemerisService.kt`의 `getEphemerisTrackDtlByMstId()` (2637-2723줄) 참고

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (반환 타입 변경 없음)

**의존성**: Step 1.2, Step 3.2 완료 필요

**주의사항**: 
- 이 함수를 사용하는 모든 곳에서 Keyhole-aware 데이터를 받게 됨
- `sendInitialTrackingData()`, `sendAdditionalTrackingDataOptimized()` 등에 영향

---

### Phase 4: 상태머신 개선 (컴파일 확인 필수)

#### Step 4.1: PreparingStep enum 추가
**파일**: `PassScheduleService.kt` (새로 추가, TrackingState enum 근처)

**작업 내용**:
- `PreparingStep` enum 추가: `INIT`, `MOVING_TRAIN`, `WAITING_TRAIN`, `MOVING_AZ_EL`
- 내부 변수 추가: `private var currentPreparingStep: PreparingStep = PreparingStep.INIT`

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (독립적인 enum이므로 다른 코드에 영향 없음)

**의존성**: 없음

---

#### Step 4.2: Train 회전 관련 헬퍼 함수 추가
**파일**: `PassScheduleService.kt` (새로 추가, moveToStartPosition() 근처)

**작업 내용**:
- `moveTrainToZero(trainAngle: Float)`: Train 축만 활성화하여 회전
- `moveToTargetAzEl()`: Azimuth, Elevation 축만 활성화하여 이동
- `isTrainAtZero()`: Train 각도 도달 확인
- `isTrainStabilized()`: Train 각도 안정화 확인
- `EphemerisService.kt`의 동일한 함수들 참고

**KDOC 주석 예시**:
```kotlin
/**
 * Train 축만 활성화하여 목표 각도로 회전합니다.
 *
 * 이 함수는 PREPARING 상태에서 Train을 먼저 회전하기 위해 사용됩니다.
 * Train 축만 활성화하여 다른 축(Az, El)에는 영향을 주지 않습니다.
 *
 * @param trainAngle 목표 Train 각도 (도 단위, Float)
 *
 * @see moveToTargetAzEl Train 회전 후 Az/El 이동
 * @see isTrainAtZero Train 각도 도달 확인
 */
private fun moveTrainToZero(trainAngle: Float) {
    // 구현 내용...
}

/**
 * Azimuth와 Elevation 축만 활성화하여 목표 위치로 이동합니다.
 *
 * 이 함수는 Train 회전 및 안정화 완료 후 Az/El을 이동하기 위해 사용됩니다.
 * Az와 El 축만 활성화하여 Train 축에는 영향을 주지 않습니다.
 *
 * @see moveTrainToZero Train 회전 먼저 수행
 * @see isTrainStabilized Train 안정화 확인
 */
private fun moveToTargetAzEl() {
    // 구현 내용...
}

/**
 * Train 각도가 목표 각도에 도달했는지 확인합니다.
 *
 * @return Train 각도가 목표 각도에 도달했으면 true, 아니면 false
 *
 * @see moveTrainToZero Train 회전 명령 후 확인
 */
private fun isTrainAtZero(): Boolean {
    // 구현 내용...
}

/**
 * Train 각도가 안정화되었는지 확인합니다.
 *
 * @return Train 각도가 안정화되었으면 true, 아니면 false
 *
 * @see isTrainAtZero Train 각도 도달 확인 후 안정화 확인
 */
private fun isTrainStabilized(): Boolean {
    // 구현 내용...
}
```

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (독립적인 함수이므로 다른 코드에 영향 없음)

**의존성**: Step 3.1 완료 필요 (getTrackingPassMst()로 Keyhole 정보 확인)

---

#### Step 4.3: moveToStartPosition() 개선
**파일**: `PassScheduleService.kt` (643-652줄)

**작업 내용**:
- `getSelectedTrackDtlByMstId()` 사용 (이미 Keyhole-aware)
- `getTrackingPassMst()`로 Keyhole 정보 확인
- `targetAzimuth`, `targetElevation` 설정
- `currentPreparingStep = PreparingStep.MOVING_TRAIN` 설정
- Train 각도 동적 설정 (Keyhole 여부에 따라)

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (함수 시그니처 변경 없음)

**의존성**: Step 3.1, Step 3.3, Step 4.1, Step 4.2 완료 필요

---

#### Step 4.4: executeStateAction() PREPARING 상태 개선
**파일**: `PassScheduleService.kt` (393-402줄)

**작업 내용**:
- PREPARING 상태에서 `currentPreparingStep`에 따라 단계별 처리
- `MOVING_TRAIN`: Train 회전 명령 전송
- `WAITING_TRAIN`: Train 안정화 대기
- `MOVING_AZ_EL`: Az/El 이동 명령 전송

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (when 문 확장)

**의존성**: Step 4.1, Step 4.2, Step 4.3 완료 필요

---

### Phase 5: ICD 프로토콜 함수 개선 (컴파일 확인 필수)

**참고**: `sendAdditionalTrackingData()` 함수 이름 개선 및 비동기/동기 처리 최적화 포함

#### Step 5.1: sendHeaderTrackingData() 개선
**파일**: `PassScheduleService.kt` (715-759줄)

**작업 내용**:
- `getSelectedTrackMstByMstId()` → `getTrackingPassMst()` 변경
- Keyhole 정보 로깅 추가

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (반환 타입 동일)

**의존성**: Step 3.1 완료 필요

---

#### Step 5.2: sendInitialTrackingData() 개선
**파일**: `PassScheduleService.kt` (761-864줄)

**작업 내용**:
- `getSelectedTrackDtlByMstId()` 사용 (이미 Keyhole-aware로 개선됨)
- Keyhole 정보 로깅 추가

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (함수 시그니처 변경 없음)

**의존성**: Step 3.3 완료 필요

---

#### Step 5.3: sendAdditionalTrackingData() 함수 이름 개선 및 비동기/동기 처리 최적화
**파일**: `PassScheduleService.kt` (874-1006줄)

**작업 내용**:
1. **함수 이름 개선**:
   - `sendAdditionalTrackingDataOptimized()` → `sendAdditionalTrackingData()` (메인 함수이므로 간단하게)
   - `sendFromCache()` → `sendAdditionalTrackingDataFromCache()` (명확하게)
   - `sendFromDatabase()` → `sendAdditionalTrackingDataFromDatabase()` (명확하게)
   - `sendAdditionalTrackingDataLegacy()` → **제거** (중복이므로)

2. **비동기/동기 처리 최적화**:
   - **문제**: 비동기 처리(`CompletableFuture.runAsync`)가 느려서 동기 처리가 필요했음
   - **원인**: `batchExecutor`는 LOW 우선순위 스레드 풀이므로 작업 대기 시간 발생
   - **해결**: 조건부 비동기 처리
     - 캐시 있으면: 동기 처리 (빠름, 즉시 전송)
     - 캐시 없으면: 비동기 처리 (DB 조회는 느릴 수 있으므로 블로킹 방지)
   - **예외 처리**: `sendAdditionalTrackingDataFromDatabase()`를 try-catch로 감싸서 직접 호출 (Legacy 제거)

3. **Keyhole-aware 데이터 사용**:
   - `getSelectedTrackDtlByMstId()` 사용 (이미 Keyhole-aware로 개선됨)
   - `sendAdditionalTrackingDataFromDatabase()` 내부에서도 `getSelectedTrackDtlByMstId()` 사용 확인

**구현 예시**:
```kotlin
// 메인 함수 이름 변경
fun handleTrackingDataRequest(passId: UInt, timeAcc: UInt, requestDataLength: UShort) {
    val startIndex = timeAcc.toInt()
    sendAdditionalTrackingData(passId, startIndex, requestDataLength.toInt())  // Optimized 제거
}

// 메인 함수 (조건부 비동기 처리)
private fun sendAdditionalTrackingData(passId: UInt, startIndex: Int, requestDataLength: Int = 25) {
    val cache = trackingDataCache[passId]
    
    if (cache != null && !cache.isExpired()) {
        // ✅ 캐시 있으면 동기 처리 (빠름, 즉시 전송)
        val processingStart = System.nanoTime()
        try {
            sendAdditionalTrackingDataFromCache(cache, startIndex, requestDataLength, processingStart)
        } catch (e: Exception) {
            logger.error("캐시에서 추적 데이터 전송 실패: passId=$passId, ${e.message}", e)
            // 폴백: DB에서 동기 처리로 재시도
            try {
                sendAdditionalTrackingDataFromDatabase(passId, startIndex, requestDataLength, processingStart)
            } catch (fallbackError: Exception) {
                logger.error("폴백 전송도 실패: passId=$passId, ${fallbackError.message}", fallbackError)
            }
        }
    } else {
        // ✅ 캐시 없으면 비동기 처리 (DB 조회는 느릴 수 있으므로 블로킹 방지)
        CompletableFuture.runAsync({
            try {
                val processingStart = System.nanoTime()
                sendAdditionalTrackingDataFromDatabase(passId, startIndex, requestDataLength, processingStart)
            } catch (e: Exception) {
                logger.error("추적 데이터 전송 실패: passId=$passId, ${e.message}", e)
                // 폴백: 동기 처리로 재시도
                try {
                    val processingStart = System.nanoTime()
                    sendAdditionalTrackingDataFromDatabase(passId, startIndex, requestDataLength, processingStart)
                } catch (fallbackError: Exception) {
                    logger.error("폴백 전송도 실패: passId=$passId, ${fallbackError.message}", fallbackError)
                }
            }
        }, batchExecutor)
    }
}

// 헬퍼 함수 1: 캐시에서 전송 (이름 변경)
private fun sendAdditionalTrackingDataFromCache(
    cache: TrackingDataCache,
    startIndex: Int,
    requestDataLength: Int,
    processingStart: Long
) {
    // 기존 sendFromCache() 로직 유지
    // ...
}

// 헬퍼 함수 2: 메모리 저장소에서 전송 (이름 변경, 추후 DB 연계 예정)
private fun sendAdditionalTrackingDataFromDatabase(
    passId: UInt,
    startIndex: Int,
    requestDataLength: Int,
    processingStart: Long
) {
    // ✅ Keyhole-aware 데이터 사용
    // 현재: getSelectedTrackDtlByMstId()는 메모리 저장소(passScheduleTrackDtlStorage)에서 조회
    // 추후: getSelectedTrackDtlByMstId() 내부를 DB 조회로 변경하면 자동으로 DB 연계됨
    val passDetails = getSelectedTrackDtlByMstId(passId)
    // 기존 sendFromDatabase() 로직 유지
    // ...
}

// Legacy 함수 제거
// sendAdditionalTrackingDataLegacy() 삭제
```

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (함수 시그니처 변경 없음, 이름만 변경)

**의존성**: Step 3.3 완료 필요

**주의사항**:
- `sendAdditionalTrackingDataLegacy()` 호출하는 곳이 있는지 확인 필요 (892줄에서만 호출됨)
- 함수 이름 변경 시 모든 호출부 업데이트 필요
- 비동기/동기 처리 최적화로 성능 개선 예상
- **중요**: `sendAdditionalTrackingDataFromDatabase()`는 현재 메모리 저장소(`passScheduleTrackDtlStorage`)를 사용하지만, 함수 이름은 "FromDatabase"로 명명 (추후 DB 연계 예정)
  - 현재: `getSelectedTrackDtlByMstId()`가 메모리 저장소에서 조회
  - 추후: `getSelectedTrackDtlByMstId()` 내부를 DB 조회로 변경하면 자동으로 DB 연계됨
  - **결론**: `sendAdditionalTrackingDataFromDatabase()` 함수는 수정 불필요, `getSelectedTrackDtlByMstId()`만 수정하면 됨

---

### Phase 6: Controller API 개선 (컴파일 확인 필수)

#### Step 6.1: getAllPassScheduleTrackMstMerged() 함수 추가
**파일**: `PassScheduleService.kt` (새로 추가, getAllPassScheduleTrackMst() 근처)

**작업 내용**:
- `EphemerisService.kt`의 `getAllEphemerisTrackMstMerged()` (2316-2461줄) 참고
- 5가지 DataType 병합하여 Keyhole 정보 포함

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (새로운 함수 추가)

**의존성**: Step 2.1 완료 필요

---

#### Step 6.2: PassScheduleController.kt 개선
**파일**: `PassScheduleController.kt` (727-772줄)

**작업 내용**:
- `getAllPassScheduleTrackMst()` → `getAllPassScheduleTrackMstMerged()` 변경
- 응답 구조 변경 (위성별 그룹화 유지, Keyhole 정보 포함)

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (반환 타입 변경 없음, 내부 구조만 변경)

**의존성**: Step 6.1 완료 필요

---

### Phase 7: 캐시 관련 함수 개선 (컴파일 확인 필수)

#### Step 7.1: preloadTrackingDataCache() 개선
**파일**: `PassScheduleService.kt` (1820-1890줄)

**작업 내용**:
- `getSelectedTrackDtlByMstId()` 사용 (이미 Keyhole-aware로 개선됨)

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (함수 시그니처 변경 없음)

**의존성**: Step 3.3 완료 필요

---

#### Step 7.2: calculateDataLength() 개선
**파일**: `PassScheduleService.kt` (1774-1778줄)

**작업 내용**:
- `getSelectedTrackDtlByMstId()` 사용 (이미 Keyhole-aware로 개선됨)

**컴파일 확인**:
```bash
./gradlew compileKotlin
```
**예상 결과**: 컴파일 성공 (함수 시그니처 변경 없음)

**의존성**: Step 3.3 완료 필요

---

### Phase 8: 프론트엔드 개선 (컴파일 확인 필수)

#### Step 8.1: 프론트엔드 타입 개선
**파일**: 
- `ACS/src/services/mode/passScheduleService.ts` (PassScheduleMasterData 인터페이스)
- `ACS/src/stores/mode/passScheduleStore.ts` (ScheduleItem 인터페이스)

**작업 내용**:
- Keyhole 정보 필드 추가: `IsKeyhole`, `RecommendedTrainAngle`
- 축 변환 정보 필드 추가: `OriginalMaxElevation`, `FinalTransformedMaxAzRate`, `KeyholeFinalTransformedMaxAzRate` 등

**컴파일 확인**:
```bash
# 프론트엔드 빌드
npm run build
# 또는
npm run type-check
```
**예상 결과**: 컴파일 성공 (타입 추가만 하므로 기존 코드에 영향 없음)

**의존성**: Step 6.2 완료 필요

---

#### Step 8.2: 프론트엔드 매핑 개선
**파일**: `ACS/src/stores/mode/passScheduleStore.ts` (fetchScheduleDataFromServer())

**작업 내용**:
- Keyhole 정보 매핑 추가
- 축 변환 정보 매핑 추가

**컴파일 확인**:
```bash
npm run build
```
**예상 결과**: 컴파일 성공 (매핑 로직 추가만 하므로 기존 코드에 영향 없음)

**의존성**: Step 8.1 완료 필요

---

#### Step 8.3: 프론트엔드 UI 개선
**파일**: `ACS/src/components/content/SelectScheduleContent.vue`

**작업 내용**:
- Keyhole 정보 컬럼 추가
- Train 각도 컬럼 추가
- **2축/3축/최종 데이터 컬럼 추가** (EphemerisDesignationPage.vue 수준):
  - 2축 최대 고도 (OriginalMaxElevation)
  - 3축 최대 고도 (Train0MaxElevation)
  - 최종 최대 고도 (MaxElevation - Keyhole에 따라 동적)
  - 2축 최대 Az 속도 (OriginalMaxAzRate)
  - 3축 최대 Az 속도 (Train0MaxAzRate)
  - 최종 최대 Az 속도 (FinalTransformedMaxAzRate - Keyhole에 따라 동적)
  - 2축 최대 El 속도 (OriginalMaxElRate)
  - 3축 최대 El 속도 (Train0MaxElRate)
  - 최종 최대 El 속도 (FinalTransformedMaxElRate - Keyhole에 따라 동적)
- **가독성 개선**:
  - 테이블 높이 증가 (400px → 500px)
  - 컬럼 너비 증가
  - 폰트 크기 증가 (13px)
  - 패딩 증가
- `safeToFixed` 함수 추가 (안전한 숫자 포맷팅)
- `EphemerisDesignationPage.vue` 참고

**컴파일 확인**:
```bash
npm run build
```
**예상 결과**: 컴파일 성공 (컬럼 추가만 하므로 기존 코드에 영향 없음)

**의존성**: Step 8.2 완료 필요

**참고**: 문제 2 (SelectScheduleContent.vue UI 개선 요청) 섹션 참고

---

## 컴파일 확인 체크리스트

각 Phase 완료 후 다음을 확인:

1. **백엔드 컴파일**:
   ```bash
   ./gradlew compileKotlin
   ```

2. **프론트엔드 컴파일** (Phase 8만):
   ```bash
   npm run build
   ```

3. **전체 빌드** (최종 확인):
   ```bash
   # 백엔드
   ./gradlew build
   
   # 프론트엔드
   npm run build
   ```

## 적용 순서 요약

1. **Phase 1**: 기본 인프라 구축 (Step 1.1 → Step 1.2)
2. **Phase 2**: 데이터 생성 및 저장 개선 (Step 2.1)
3. **Phase 3**: 조회 메서드 개선 (Step 3.1 → Step 3.2 → Step 3.3)
4. **Phase 4**: 상태머신 개선 (Step 4.1 → Step 4.2 → Step 4.3 → Step 4.4)
5. **Phase 5**: ICD 프로토콜 함수 개선 (Step 5.1 → Step 5.2 → Step 5.3)
6. **Phase 6**: Controller API 개선 (Step 6.1 → Step 6.2)
7. **Phase 7**: 캐시 관련 함수 개선 (Step 7.1 → Step 7.2)
8. **Phase 8**: 프론트엔드 개선 (Step 8.1 → Step 8.2 → Step 8.3)

## 주의사항

1. **각 Phase 완료 후 반드시 컴파일 확인**
   - 각 Step마다 컴파일 확인 섹션이 있으므로 반드시 수행할 것
   - 컴파일 오류 발생 시 해당 Step의 구현을 다시 검토할 것
2. **의존성 순서 준수** (앞 단계 완료 후 다음 단계 진행)
   - 각 Step의 "의존성" 섹션을 확인하여 순서대로 진행할 것
3. **저장소 구조 변경 시 영향 범위 확인** (Step 2.1)
   - 저장소 구조 변경은 다른 함수들에 영향을 줄 수 있으므로 주의할 것
4. **조회 함수 변경 시 사용처 확인** (Step 3.3)
   - `getSelectedTrackDtlByMstId()` 변경 시 모든 사용처를 확인할 것
5. **상태머신 변경 시 동작 확인** (Phase 4)
   - 상태머신 로직 변경은 추적 동작에 직접적인 영향을 주므로 신중하게 구현할 것
6. **KDOC 주석 작성 필수**
   - 모든 새로 추가되는 함수에는 반드시 KDOC 주석을 작성할 것
   - 각 Step의 "KDOC 주석 예시" 섹션을 참고하여 작성할 것
   - 함수 역할, 파라미터, 반환값, 참고 함수를 명확히 작성할 것

## 사용자 협의 사항

**질문 1 답변**: `moveToStartPosition()` 내부에서 `moveStartAnglePosition()` 호출 시 Train 각도를 동적으로 설정
- PREPARING 상태에서 `moveToStartPosition()` 호출 시 Train 각도도 함께 설정
- 별도의 Train 각도 설정 상태는 불필요 (EphemerisService와 다름)
- 상태머신은 변경하지 않음

**질문 2 답변 (수정)**: PREPARING 상태 내에서 Train 회전 로직 필요
- EphemerisService와 동일하게 Train을 먼저 회전하고, 도착하면 Az/El을 이동하는 로직 필요
- 별도 상태 추가하지 않고, PREPARING 상태 내에서 내부 플래그로 진행 단계 관리
- 한 번에 움직이면 안됨 (Train 회전 → 안정화 대기 → Az/El 이동 순서)

## 전체 상관관계 검토 결과

### 1. 데이터 독립성 확인

**저장소 구조**:
- `EphemerisService`: `ephemerisTrackMstStorage` (mutableList), `ephemerisTrackDtlStorage` (mutableList)
- `PassScheduleService`: `passScheduleTrackMstStorage` (ConcurrentHashMap<String, List>), `passScheduleTrackDtlStorage` (ConcurrentHashMap<String, List>)
- **결론**: 완전히 분리된 저장소로 데이터 독립성 보장

**SatelliteTrackingProcessor 공유**:
- `SatelliteTrackingProcessor`는 stateless (내부 상태 없음)
- `processFullTransformation()`은 입력을 받아 변환만 수행하고 반환
- **결론**: 여러 서비스가 같은 인스턴스를 공유해도 문제 없음

### 2. 저장소 구조 차이점

**EphemerisService**:
- `mutableList<Map<String, Any?>>` - 단일 리스트에 모든 DataType 저장
- DataType 필드로 구분 (original, axis_transformed, final_transformed, keyhole_axis_transformed, keyhole_final_transformed)

**PassScheduleService (현재)**:
- `ConcurrentHashMap<String, List<Map<String, Any?>>>` - 위성별로 리스트 저장
- 단일 DataType만 저장 (변환된 데이터만)
- **개선 필요**: DataType 필드 추가하여 5가지 DataType 모두 저장

### 3. 조회 메서드 차이점

**EphemerisService**:
- `getTrackingPassMst()`: Keyhole 여부에 따라 동적으로 MST 선택
- `getEphemerisTrackDtlByMstId()`: Keyhole 여부에 따라 적절한 DataType 반환

**PassScheduleService (현재)**:
- `getSelectedTrackMstByMstId()`: 단순 조회, Keyhole 정보 없음
- `getSelectedTrackDtlByMstId()`: 단순 조회, Keyhole 정보 없음
- **개선 필요**: `getTrackingPassMst()` 헬퍼 함수 추가 및 조회 메서드 개선

### 4. 상태머신 차이점

**EphemerisService**:
- `MOVING_TRAIN_TO_ZERO` 상태: Train 각도 먼저 설정 → 안정화 대기 → 목표 위치 이동
- `WAITING_FOR_TRAIN_STABILIZATION` 상태: Train 각도 안정화 대기

**PassScheduleService (현재)**:
- `PREPARING` 상태: 바로 `moveToStartPosition()` 호출 (Azimuth, Elevation, Train 동시 설정)

**PassScheduleService (개선 후)**:
- `PREPARING` 상태: `moveToStartPosition()` 호출 → 내부 플래그 `PreparingStep.MOVING_TRAIN`으로 전환
- `PREPARING` 상태 내 `MOVING_TRAIN` 단계: Train 먼저 회전 (`moveTrainToZero()` - Train 축만 활성화)
- `PREPARING` 상태 내 `WAITING_TRAIN` 단계: Train 안정화 대기
- `PREPARING` 상태 내 `MOVING_AZ_EL` 단계: Az/El 이동 (`moveToTargetAzEl()` - Az, El 축만 활성화)
- **결론**: 별도 상태 추가 없이 PREPARING 상태 내에서 순차 처리 (사용자 요구사항 확인)

### 5. ICD 프로토콜 함수 차이점

**EphemerisService**:
- `sendHeaderTrackingData()`: `getTrackingPassMst()` 사용 (Keyhole 정보 포함)
- `sendInitialTrackingData()`: Keyhole 여부에 따라 적절한 DataType 반환
- `sendAdditionalTrackingData()`: Keyhole 여부에 따라 적절한 DataType 반환

**PassScheduleService (현재)**:
- `sendHeaderTrackingData()`: `getSelectedTrackMstByMstId()` 사용 (Keyhole 정보 없음)
- `sendInitialTrackingData()`: `getSelectedTrackDtlByMstId()` 사용 (Keyhole 정보 없음)
- `sendAdditionalTrackingDataOptimized()`: `getSelectedTrackDtlByMstId()` 사용 (Keyhole 정보 없음)
- `sendAdditionalTrackingDataLegacy()`: 폴백용 (중복, 제거 예정)
- **개선 필요**: 모든 ICD 프로토콜 함수에서 Keyhole 정보 활용
- **추가 개선**: 함수 이름 개선 및 비동기/동기 처리 최적화

### 6. Controller API 개선 (Keyhole 정보 포함)

**EphemerisService의 경우**:
- `PushDataService`에 `EphemerisService` 의존성 없음
- Controller에서 `/ephemeris/master` API 제공 (`getAllEphemerisTrackMstMerged()`)
- 프론트엔드가 API로 Keyhole 정보 조회 (`IsKeyhole`, `RecommendedTrainAngle` 포함)
- `PushDataService`는 실시간 데이터만 전송하고, Keyhole 정보는 포함하지 않음

**PassScheduleService의 경우 (사용자 요구사항 반영)**:
- `PushDataService`에 `PassScheduleService` 의존성 추가하지 않음
- Controller의 `/pass-schedule/tracking/master` API에 Keyhole 정보 포함하도록 개선
- 프론트엔드가 API로 Keyhole 정보 조회 (EphemerisService와 동일한 방식)

**현재 상태**:
- Controller: `/pass-schedule/tracking/master` API 제공 (727줄)
- Service: `getAllPassScheduleTrackMst()` 반환 (1528줄)
- Keyhole 정보 미포함 (2축 데이터만 저장)

**개선 후**:
- Service: `getAllPassScheduleTrackMstMerged()` 함수 추가 (EphemerisService의 `getAllEphemerisTrackMstMerged()` 참고)
- Keyhole 정보 포함: `IsKeyhole`, `RecommendedTrainAngle` 필드 추가
- Controller: `/pass-schedule/tracking/master` API에서 `getAllPassScheduleTrackMstMerged()` 사용
- 프론트엔드: API 응답에 Keyhole 정보 포함되어 자동으로 사용 가능

### 7. 누락된 부분 검토

**확인 완료**:
1. ✅ 데이터 독립성: 저장소 완전 분리
2. ✅ SatelliteTrackingProcessor 공유: stateless이므로 문제 없음
3. ✅ 저장소 구조 차이: PassScheduleService는 ConcurrentHashMap 사용 (위성별 관리)
4. ✅ 조회 메서드 차이: `getTrackingPassMst()` 헬퍼 함수 추가 필요
5. ✅ 상태머신 차이: PassScheduleService는 별도 Train 각도 설정 상태 불필요
6. ✅ ICD 프로토콜 함수: 모든 함수에서 Keyhole 정보 활용 필요
7. ✅ PushDataService 연동: PassScheduleService 의존성 추가 및 Keyhole 정보 활용

**추가 검토 사항 (심층 검토 완료)**:
- `selectedTrackMstStorage`: 선택된 패스만 저장하는 별도 저장소 (106줄, 1637줄)
  - **역할**: `passScheduleTrackMstStorage`에서 사용자가 선택한 패스만 필터링하여 저장
  - **사용처**: `getCurrentSelectedTrackingPassWithTime()`, `getNextSelectedTrackingPassWithTime()`, `getSelectedTrackMstByMstId()`, `getSelectedTrackDtlByMstId()` 등 모든 선택된 패스 조회 함수에서 사용
  - **문제**: 현재는 단일 DataType만 필터링하므로, 5가지 DataType 모두 필터링하도록 개선 필요
  - **영향**: `selectedTrackMstStorage`를 사용하는 모든 함수가 Keyhole 정보를 포함하도록 개선 필요
- `generateSelectedTrackingData()`: 선택된 패스 데이터 생성 (1617-1645줄)
  - **역할**: `passScheduleTrackMstStorage`에서 `trackingTargetList`에 있는 MST ID만 필터링하여 `selectedTrackMstStorage`에 저장
  - **문제**: 현재는 단일 DataType만 필터링하므로, 5가지 DataType 모두 필터링하도록 개선 필요
  - **해결**: `generateSelectedTrackingData()`에서 5가지 DataType 모두 필터링하여 `selectedTrackMstStorage`에 저장하도록 개선

### 8. 최종 검토 결과

**모든 상관관계 확인 완료**:
- 데이터 독립성: ✅ 문제 없음
- 저장소 구조: ✅ 개선 계획 수립 완료
- 조회 메서드: ✅ 개선 계획 수립 완료
- 상태머신: ✅ 사용자 답변 반영 완료 (PREPARING 상태 내에서 순차 처리)
- ICD 프로토콜: ✅ 개선 계획 수립 완료
- Controller API: ✅ 개선 계획 수립 완료 (Keyhole 정보 포함)
- `selectedTrackMstStorage`: ✅ 개선 계획 수립 완료 (5가지 DataType 모두 필터링)

**추가 확인 필요 (해결됨)**:
- `selectedTrackMstStorage` 및 `generateSelectedTrackingData()`의 DataType 처리 방식 확인 필요
  - **해결**: `generateSelectedTrackingData()`에서 5가지 DataType 모두 필터링하도록 개선 계획 수립 완료

## 참고 파일

- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/processor/SatelliteTrackingProcessor.kt`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/controller/mode/PassScheduleController.kt`

## 심층 검토 완료 사항

### 1. 저장소 구조 완전 분석
- `passScheduleTrackMstStorage`: 전체 패스 데이터 저장 (위성별 관리)
- `passScheduleTrackDtlStorage`: 전체 패스 상세 데이터 저장 (위성별 관리)
- `selectedTrackMstStorage`: 선택된 패스만 저장 (위성별 관리, generateSelectedTrackingData()에서 생성)
- **관계**: `selectedTrackMstStorage`는 `passScheduleTrackMstStorage`의 부분집합

### 2. 함수 연관 관계 완전 분석
- `generateSelectedTrackingData()`: `passScheduleTrackMstStorage` → `selectedTrackMstStorage` (필터링)
- `getCurrentSelectedTrackingPassWithTime()`: `selectedTrackMstStorage` 사용
- `getNextSelectedTrackingPassWithTime()`: `selectedTrackMstStorage` 사용
- `getSelectedTrackMstByMstId()`: `selectedTrackMstStorage` 사용
- `getSelectedTrackDtlByMstId()`: `selectedTrackMstStorage` + `passScheduleTrackDtlStorage` 사용
- `getTrackingPassMst()`: `passScheduleTrackMstStorage` 직접 사용 (새로 추가)
- `sendHeaderTrackingData()`: `getSelectedTrackMstByMstId()` 또는 `getTrackingPassMst()` 사용
- `sendInitialTrackingData()`: `getSelectedTrackDtlByMstId()` 사용
- `sendAdditionalTrackingDataOptimized()`: `getSelectedTrackDtlByMstId()` 사용
- `preloadTrackingDataCache()`: `getSelectedTrackDtlByMstId()` 사용
- `calculateDataLength()`: `getSelectedTrackDtlByMstId()` 사용

### 3. 누락된 부분 모두 확인
- ✅ `generateSelectedTrackingData()`: 5가지 DataType 모두 필터링 필요
- ✅ `selectedTrackMstStorage`: DataType 필드 포함 필요
- ✅ `getSelectedTrackDtlByMstId()`: Keyhole 여부에 따라 적절한 DataType 반환 필요
- ✅ `getTrackingPassMst()`: 새로 추가 필요
- ✅ `getAllPassScheduleTrackMstMerged()`: 새로 추가 필요
- ✅ 모든 ICD 프로토콜 함수: Keyhole 정보 활용 필요
- ✅ 캐시 관련 함수: Keyhole 정보 활용 필요

### 4. KDOC 주석 추가 완료
- 모든 주요 함수에 KDOC 주석 추가
- 함수 역할, 파라미터, 반환값, 참고 함수 명시
- Keyhole 판단 로직 및 DataType 선택 기준 명시

### 5. Controller 및 프론트엔드 연동 완전 분석
- **EphemerisController**: `/ephemeris/master` API 제공, `getAllEphemerisTrackMstMerged()` 사용, `Mono<List<Map<String, Any?>>>` 반환
- **PassScheduleController (현재)**: `/pass-schedule/tracking/master` API 제공, `getAllPassScheduleTrackMst()` 사용, `ResponseEntity<Map<String, Any>>` 반환 (위성별 그룹화)
- **PassScheduleController (개선 후)**: `/pass-schedule/tracking/master` API 제공, `getAllPassScheduleTrackMstMerged()` 사용, `ResponseEntity<Map<String, Any>>` 반환 (위성별 그룹화 유지, Keyhole 정보 포함)
- **프론트엔드 타입**: `PassScheduleMasterData` 인터페이스에 Keyhole 정보 필드 추가 필요
- **프론트엔드 매핑**: `fetchScheduleDataFromServer()`에서 Keyhole 정보 매핑 추가 필요
- **프론트엔드 UI**: `SelectScheduleContent.vue`에 Keyhole 정보 컬럼 추가 필요 (EphemerisDesignationPage 참고)

## 최종 심층 검토 결과 (중복/누락/최적화/예외 처리)

### 1. DataType 필드 저장 확인
- ✅ **확인 완료**: `SatelliteTrackingProcessor.processFullTransformation()`이 반환하는 모든 MST/DTL 데이터에는 이미 `DataType` 필드가 포함되어 있음
  - `original`: `put("DataType", "original")` (SatelliteTrackingProcessor.kt 242줄)
  - `axis_transformed`: `put("DataType", "axis_transformed")` (EphemerisService.kt 590줄 참고)
  - `final_transformed`: `put("DataType", "final_transformed")` (SatelliteTrackingProcessor.kt 507줄)
  - `keyhole_axis_transformed`: `put("DataType", "keyhole_axis_transformed")` (SatelliteTrackingProcessor.kt 155줄, 161줄)
  - `keyhole_final_transformed`: `put("DataType", "keyhole_final_transformed")` (SatelliteTrackingProcessor.kt 174줄, 180줄)
- ✅ **결론**: `PassScheduleService`에서 `SatelliteTrackingProcessor.processFullTransformation()`을 호출하면 자동으로 `DataType` 필드가 포함된 데이터를 받을 수 있음
- ✅ **주의사항**: `generatePassScheduleTrackingDataAsync()`에서 `SatelliteTrackingProcessor.processFullTransformation()` 반환값을 그대로 저장하면 됨 (추가 작업 불필요)

### 2. 예외 처리 완전 분석
- ✅ **generatePassScheduleTrackingDataAsync()**: 
  - 현재: `Mono.fromCallable` 내부에서 예외 발생 시 `doOnError`로 처리 (1503-1504줄)
  - 개선 필요: `SatelliteTrackingProcessor.processFullTransformation()` 호출 시 try-catch 추가 필요
  - **구현 예시**:
    ```kotlin
    try {
        val processedData = satelliteTrackingProcessor.processFullTransformation(
            schedule,
            actualSatelliteName
        )
        // 5가지 DataType 저장
        passScheduleTrackMstStorage[satelliteId] = processedData.originalMst
        // ...
    } catch (e: Exception) {
        logger.error("❌ 위성 추적 데이터 처리 실패: ${e.message}", e)
        throw e
    }
    ```
- ✅ **getTrackingPassMst()**: 
  - null 체크는 있지만, 예외 처리 로직 추가 필요
  - **구현 예시**: try-catch로 감싸서 예외 발생 시 null 반환 및 로깅
- ✅ **getAllPassScheduleTrackMstMerged()**: 
  - 현재: try-catch로 예외 처리 (694-696줄)
  - 개선 필요: 더 구체적인 예외 처리 (예: 각 DataType 조회 시 예외 처리)
- ✅ **getSelectedTrackDtlByMstId()**: 
  - null 체크는 있지만, 예외 처리 로직 추가 필요
  - **구현 예시**: try-catch로 감싸서 예외 발생 시 emptyList 반환 및 로깅
- ✅ **preloadTrackingDataCache()**: 
  - 현재: try-catch로 예외 처리 (1881-1883줄)
  - 개선 필요: `getSelectedTrackDtlByMstId()` 호출 시 예외 처리 추가
- ✅ **calculateDataLength()**: 
  - 현재: 예외 처리 없음
  - 개선 필요: try-catch로 감싸서 예외 발생 시 0 반환 및 로깅

### 3. 중복 코드 최적화
- ✅ **Keyhole 판단 로직 중복**: 
  - `getTrackingPassMst()`와 `getSelectedTrackDtlByMstId()`에서 Keyhole 판단 로직이 중복됨
  - **최적화 방안**: 공통 로직을 헬퍼 함수로 추출
  - **구현 예시**:
    ```kotlin
    /**
     * Keyhole 여부를 확인하고 적절한 DataType을 반환합니다.
     *
     * @param passId 패스 ID (MST ID)
     * @param storage 조회할 저장소 (passScheduleTrackMstStorage 또는 selectedTrackMstStorage)
     * @return Keyhole 여부에 따라 선택된 DataType ("keyhole_final_transformed" 또는 "final_transformed"), 없으면 null
     */
    private fun determineKeyholeDataType(
        passId: UInt,
        storage: Map<String, List<Map<String, Any?>>>
    ): String? {
        // final_transformed MST에서 IsKeyhole 확인
        val allMstData = storage.values.flatten()
        val finalMst = allMstData.find {
            it["No"] == passId && it["DataType"] == "final_transformed"
        } ?: return null
        
        val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
        
        return if (isKeyhole) {
            // Keyhole 발생 시 keyhole_final_transformed 데이터 존재 여부 확인
            val keyholeDataExists = allMstData.any {
                it["No"] == passId && it["DataType"] == "keyhole_final_transformed"
            }
            
            if (!keyholeDataExists) {
                logger.warn("⚠️ 패스 ID ${passId}: Keyhole로 판단되었으나 keyhole_final_transformed 데이터가 없습니다. final_transformed로 폴백합니다.")
                "final_transformed"  // 폴백
            } else {
                "keyhole_final_transformed"
            }
        } else {
            "final_transformed"
        }
    }
    ```
- ✅ **결론**: 공통 로직을 헬퍼 함수로 추출하여 중복 제거 및 유지보수성 향상

### 4. null 안전성 개선
- ✅ **안전한 캐스팅**: 
  - 현재: `as?` 연산자 사용 (좋음)
  - 개선 필요: 더 명확한 null 처리 (예: `as? UInt ?: return null`)
- ✅ **구현 예시**:
  ```kotlin
  // ✅ 개선 전
  val mstId = mstRecord["No"] as? UInt
  val recordDataType = mstRecord["DataType"] as? String
  
  // ✅ 개선 후
  val mstId = mstRecord["No"] as? UInt ?: continue  // 다음 항목으로 건너뛰기
  val recordDataType = mstRecord["DataType"] as? String ?: continue
  ```

### 5. 최적화 개선 사항
- ✅ **저장소 조회 최적화**: 
  - `passScheduleTrackMstStorage.values.flatten()` 반복 호출 최적화
  - **구현 예시**: 한 번만 조회하여 변수에 저장 후 재사용
  ```kotlin
  // ✅ 개선 전
  val finalMst = passScheduleTrackMstStorage.values.flatten().find { ... }
  val keyholeMstExists = passScheduleTrackMstStorage.values.flatten().any { ... }
  
  // ✅ 개선 후
  val allMstData = passScheduleTrackMstStorage.values.flatten()
  val finalMst = allMstData.find { ... }
  val keyholeMstExists = allMstData.any { ... }
  ```
- ✅ **캐시 활용**: 
  - `preloadTrackingDataCache()`에서 캐시된 데이터 활용 시 성능 향상
  - 현재: `getSelectedTrackDtlByMstId()` 호출 시마다 저장소 조회
  - 개선: 캐시된 데이터 우선 사용, 없을 경우에만 저장소 조회

### 6. 최종 검증 체크리스트
- ✅ DataType 필드 저장: `SatelliteTrackingProcessor`가 자동으로 포함하므로 추가 작업 불필요
- ✅ 예외 처리: 모든 주요 함수에 try-catch 추가 필요
- ✅ 중복 코드: Keyhole 판단 로직을 헬퍼 함수로 추출 필요
- ✅ null 안전성: 안전한 캐스팅 및 명확한 null 처리 필요
- ✅ 최적화: 저장소 조회 최적화 및 캐시 활용 필요
- ✅ 모든 함수 KDOC 주석: 완료
- ✅ 모든 함수 테스트: 구현 후 테스트 필요

---

## 구현 중 발생한 문제 및 해결

### 문제 1: Smart Cast 에러 (EphemerisService.kt)

**발생 위치**: `EphemerisService.kt:821:25`

**에러 메시지**:
```
Smart cast to 'Map<String, Any?>' is impossible, because 'currentTrackingPass' is a mutable property that could have been changed by this time
```

**원인**:
- Kotlin의 smart cast는 mutable property에 대해 작동하지 않음
- `currentTrackingPass`가 `var`로 선언된 mutable property이므로, null 체크 후에도 다른 스레드에서 변경될 수 있다고 가정
- 따라서 `currentTrackingPass == null` 체크 후에도 smart cast가 불가능

**해결 방법**:
1. `getTrackingPassMst()` 결과를 로컬 변수(`selectedPass`)에 먼저 할당
2. 로컬 변수에 대해 null 체크 수행
3. null 체크 통과 후 `currentTrackingPass`에 할당
4. 이후 로컬 변수(`selectedPass`)를 사용하여 데이터 접근

**수정 전**:
```kotlin
currentTrackingPass = getTrackingPassMst(passId)

if (currentTrackingPass == null) {
    logger.error("패스 ID {}에 해당하는 데이터를 찾을 수 없습니다", passId)
    return
}

// ❌ 에러: Smart cast 불가능
val isKeyhole = currentTrackingPass["IsKeyhole"] as? Boolean ?: false
```

**수정 후**:
```kotlin
val selectedPass = getTrackingPassMst(passId)

if (selectedPass == null) {
    logger.error("패스 ID {}에 해당하는 데이터를 찾을 수 없습니다", passId)
    return
}

// ✅ 로컬 변수에 할당하여 smart cast 문제 해결
currentTrackingPass = selectedPass

// ✅ 로컬 변수 사용 (smart cast 가능)
val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
val recommendedTrainAngle = selectedPass["RecommendedTrainAngle"] as? Double ?: 0.0
```

**적용 파일**: `EphemerisService.kt` (813-826줄)

**참고**: 
- 이 문제는 `PassScheduleService.kt`에는 발생하지 않음
- `PassScheduleService.kt`에서는 이미 로컬 변수 패턴을 사용하고 있음 (809-829줄)

**검증**: 컴파일 성공 확인

---

### 문제 2: SelectScheduleContent.vue UI 개선 요청

**발생 위치**: `SelectScheduleContent.vue` (스케줄 선택 화면)

**요구사항**:
1. 항목들이 너무 작아서 가독성이 떨어짐
2. `EphemerisDesignationPage.vue`의 Select Schedule 화면처럼 상세한 정보 표시 필요:
   - 2축 최대 고도 (OriginalMaxElevation)
   - 3축 최대 고도 (Train0MaxElevation / FinalTransformedMaxElevation)
   - 최종 최대 고도 (MaxElevation - Keyhole에 따라 동적)
   - 2축 최대 Az 속도 (OriginalMaxAzRate)
   - 3축 최대 Az 속도 (Train0MaxAzRate / FinalTransformedMaxAzRate)
   - 최종 최대 Az 속도 (FinalTransformedMaxAzRate - Keyhole에 따라 동적)
   - 2축 최대 El 속도 (OriginalMaxElRate)
   - 3축 최대 El 속도 (Train0MaxElRate / FinalTransformedMaxElRate)
   - 최종 최대 El 속도 (FinalTransformedMaxElRate - Keyhole에 따라 동적)

**해결 방법**:
1. **컬럼 추가**: `EphemerisDesignationPage.vue`의 `scheduleColumns` 참고하여 상세 컬럼 추가
2. **템플릿 추가**: 각 컬럼에 대한 템플릿 추가 (색상 구분: 2축=파란색, 3축=초록색, Keyhole=빨간색)
3. **가독성 개선**: 
   - 테이블 높이 증가 (400px → 500px)
   - 컬럼 너비 증가
   - 폰트 크기 증가 (13px)
   - 패딩 증가 (10px 8px)
4. **safeToFixed 함수 추가**: 안전한 숫자 포맷팅을 위한 헬퍼 함수 추가

**수정 내용**:

**컬럼 추가** (586-606줄):
```typescript
// ✅ 2축 최대 고도 (Original)
{
  name: 'OriginalMaxElevation',
  label: '2축 최대 고도 (°)',
  field: 'OriginalMaxElevation',
  align: 'center' as const,
  sortable: true,
  style: 'width: 130px'
},
// ✅ 3축 최대 고도 (Train=0, ±270°, 항상 고정)
{
  name: 'Train0MaxElevation',
  label: '3축 최대 고도 (°)',
  field: 'FinalTransformedMaxElevation',
  align: 'center' as const,
  sortable: true,
  style: 'width: 130px'
},
// ✅ FinalTransformed 최대 고도 (Keyhole 여부에 따라 동적 표시)
{
  name: 'MaxElevation',
  label: '최대 고도 (°)',
  field: 'FinalTransformedMaxElevation',
  align: 'center' as const,
  sortable: true,
  style: 'width: 120px'
},
// ✅ 2축 최대 Az 속도
{
  name: 'OriginalMaxAzRate',
  label: '2축 최대 Az 속도 (°/s)',
  field: 'OriginalMaxAzRate',
  align: 'center' as const,
  sortable: true,
  style: 'width: 150px'
},
// ✅ 3축 최대 Az 속도 (Train=0, ±270°, 항상 고정)
{
  name: 'Train0MaxAzRate',
  label: '3축 최대 Az 속도 (°/s)',
  field: 'FinalTransformedMaxAzRate',
  align: 'center' as const,
  sortable: true,
  style: 'width: 150px'
},
// ✅ FinalTransformed 최대 Az 속도 (Keyhole 여부에 따라 동적 표시)
{
  name: 'FinalTransformedMaxAzRate',
  label: '최대 Az 속도 (°/s)',
  field: 'FinalTransformedMaxAzRate',
  align: 'center' as const,
  sortable: true,
  style: 'width: 140px'
},
// ✅ 2축 최대 El 속도
{
  name: 'OriginalMaxElRate',
  label: '2축 최대 El 속도 (°/s)',
  field: 'OriginalMaxElRate',
  align: 'center' as const,
  sortable: true,
  style: 'width: 150px'
},
// ✅ 3축 최대 El 속도 (Train=0, ±270°, 항상 고정)
{
  name: 'Train0MaxElRate',
  label: '3축 최대 El 속도 (°/s)',
  field: 'FinalTransformedMaxElRate',
  align: 'center' as const,
  sortable: true,
  style: 'width: 150px'
},
// ✅ FinalTransformed 최대 El 속도 (Keyhole 여부에 따라 동적 표시)
{
  name: 'FinalTransformedMaxElRate',
  label: '최대 El 속도 (°/s)',
  field: 'FinalTransformedMaxElRate',
  align: 'center' as const,
  sortable: true,
  style: 'width: 140px'
},
```

**템플릿 추가** (132-244줄):
- 2축 최대 고도 템플릿 (파란색)
- 3축 최대 고도 템플릿 (초록색)
- FinalTransformed 최대 고도 템플릿 (Keyhole에 따라 빨간색/초록색)
- 2축/3축/최종 Az 속도 템플릿 (동일한 색상 구분)
- 2축/3축/최종 El 속도 템플릿 (동일한 색상 구분)

**가독성 개선**:
- 테이블 높이: 400px → 500px (22줄)
- 컬럼 너비: 기본 컬럼들 증가 (index: 70px → 80px, no: 60px → 70px, satelliteId: 100px → 120px)
- 폰트 크기: 13px (1048줄, 1060줄)
- 패딩: 8px → 10px 8px (1048줄)

**적용 파일**: `SelectScheduleContent.vue`

**참고**: 
- `EphemerisDesignationPage.vue`의 `scheduleColumns` (658-811줄) 및 템플릿 (360-472줄) 참고
- `safeToFixed` 함수는 `EphemerisDesignationPage.vue` (1977-1991줄) 참고

**검증**: 컴파일 성공 확인

---

### 문제 3: SelectScheduleContent.vue에 Elevation 각도 컬럼 추가 요청

**발생 위치**: `SelectScheduleContent.vue` (스케줄 선택 화면)

**요구사항**:
1. Azimuth 각도 옆에 Elevation 각도 컬럼 추가
2. Elevation 각도는 시작/종료 각도 표시
3. Keyhole이 아닐 경우: 3축 최종 변환 값 (FinalTransformedStartElevation/EndElevation)
4. Keyhole일 경우: Keyhole 최종 변환 값 (KeyholeFinalTransformedStartElevation/EndElevation)
5. EphemerisDesignationPage.vue와 동일한 로직 적용

**검토 결과**:
- ✅ `EphemerisService.kt`의 `getAllEphemerisTrackMstMerged()` (2393-2404줄):
  - `FinalTransformedStartElevation`, `FinalTransformedEndElevation` 제공
  - `KeyholeFinalTransformedStartElevation`, `KeyholeFinalTransformedEndElevation` 제공
- ✅ `EphemerisDesignationPage.vue` (911-916줄):
  - Keyhole일 경우: `KeyholeFinalTransformedStartElevation/EndElevation` 사용
  - Keyhole 아닐 경우: `FinalTransformedStartElevation/EndElevation` 사용
- ✅ `PassScheduleService.kt`의 `getAllPassScheduleTrackMstMerged()` (1688-1697줄):
  - 동일한 필드 제공 확인

**해결 방법**:
1. **Elevation 각도 컬럼 추가**: `azimuthAngles` 컬럼 옆에 `elevationAngles` 컬럼 추가
2. **Keyhole-aware 로직**: Keyhole 여부에 따라 적절한 필드 사용
3. **Azimuth 각도도 동일하게 수정**: Keyhole 여부에 따라 동적 값 표시하도록 개선

**수정 내용**:

**Azimuth 각도 컬럼 개선** (724-747줄):
```typescript
// ✅ Azimuth 각도 컬럼 (Keyhole 여부에 따라 동적 값 표시)
{
  name: 'azimuthAngles',
  label: 'Azimuth 각도',
  field: (row: ScheduleItem) => {
    // Keyhole일 경우: KeyholeFinalTransformed 값 사용
    // Keyhole 아닐 경우: FinalTransformed 값 사용
    const isKeyhole = row.IsKeyhole || row.isKeyhole || false
    if (isKeyhole) {
      return {
        start: row.KeyholeFinalTransformedStartAzimuth ?? row.FinalTransformedStartAzimuth ?? row.startAzimuthAngle ?? 0,
        end: row.KeyholeFinalTransformedEndAzimuth ?? row.FinalTransformedEndAzimuth ?? row.endAzimuthAngle ?? 0
      }
    } else {
      return {
        start: row.FinalTransformedStartAzimuth ?? row.startAzimuthAngle ?? 0,
        end: row.FinalTransformedEndAzimuth ?? row.endAzimuthAngle ?? 0
      }
    }
  },
  align: 'center' as const,
  sortable: false,
  style: 'width: 140px'
},
```

**Elevation 각도 컬럼 추가** (748-771줄):
```typescript
// ✅ Elevation 각도 컬럼 추가 (Keyhole 여부에 따라 동적 값 표시)
{
  name: 'elevationAngles',
  label: 'Elevation 각도',
  field: (row: ScheduleItem) => {
    // Keyhole일 경우: KeyholeFinalTransformed 값 사용
    // Keyhole 아닐 경우: FinalTransformed 값 사용
    const isKeyhole = row.IsKeyhole || row.isKeyhole || false
    if (isKeyhole) {
      return {
        start: row.KeyholeFinalTransformedStartElevation ?? row.FinalTransformedStartElevation ?? row.startElevationAngle ?? 0,
        end: row.KeyholeFinalTransformedEndElevation ?? row.FinalTransformedEndElevation ?? row.endElevationAngle ?? 0
      }
    } else {
      return {
        start: row.FinalTransformedStartElevation ?? row.startElevationAngle ?? 0,
        end: row.FinalTransformedEndElevation ?? row.endElevationAngle ?? 0
      }
    }
  },
  align: 'center' as const,
  sortable: false,
  style: 'width: 140px'
},
```

**템플릿 수정** (87-101줄):
- Azimuth 각도 템플릿: `props.value?.start`, `props.value?.end` 사용 (field 함수의 반환값 사용)

**템플릿 추가** (110-124줄):
- Elevation 각도 템플릿 추가 (Azimuth 각도와 동일한 형식)

**적용 파일**: `SelectScheduleContent.vue`

**참고**: 
- `EphemerisDesignationPage.vue`의 `selectedScheduleInfo` (905-916줄) 참고
- `EphemerisService.kt`의 `getAllEphemerisTrackMstMerged()` (2393-2404줄) 참고
- `PassScheduleService.kt`의 `getAllPassScheduleTrackMstMerged()` (1688-1697줄) 참고

**검증**: 컴파일 성공 확인

