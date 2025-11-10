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

#### 1.7 sendAdditionalTrackingDataOptimized() 개선

**현재 (874-895줄)**:
- `getSelectedTrackDtlByMstId()` 사용 (954줄) - Keyhole 정보 없음

**개선 후**:
- Keyhole 여부에 따라 적절한 DataType 반환
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

## 구현 순서

1. ✅ 현재 상태 분석 (완료)
2. ⏳ PassScheduleService에 SatelliteTrackingProcessor 주입 및 processFullTransformation() 통합
3. ⏳ PassScheduleService 저장소 구조 개선: 5가지 DataType 저장 및 Keyhole 정보 포함
4. ⏳ PassScheduleService 조회 메서드 개선: Keyhole 여부에 따라 적절한 DataType 반환, getTrackingPassMst() 헬퍼 함수 추가
   - getTrackingPassMst() 헬퍼 함수 추가 (passScheduleTrackMstStorage에서 직접 조회)
   - getSelectedTrackMstByMstId() 개선 (selectedTrackMstStorage 사용, generateSelectedTrackingData() 개선 필요)
   - getSelectedTrackDtlByMstId() 개선 (Keyhole 여부에 따라 적절한 DataType 반환)
   - generateSelectedTrackingData() 개선 (5가지 DataType 모두 필터링하여 selectedTrackMstStorage에 저장)
5. ⏳ PassScheduleService PREPARING 상태 개선: Train 회전 로직 추가 (별도 상태 추가 없이)
   - 내부 플래그(PreparingStep enum)로 진행 단계 관리
   - moveToStartPosition() 개선: targetAzimuth, targetElevation 설정 후 PreparingStep.MOVING_TRAIN으로 전환
   - executeStateAction()의 PREPARING 상태에서 단계별 처리 (Train 회전 → 안정화 대기 → Az/El 이동)
   - moveTrainToZero(), moveToTargetAzEl(), isTrainAtZero(), isTrainStabilized() 함수 추가
   - checkTrackingScheduleWithStateMachine()이 100ms 주기로 호출되므로, PREPARING 상태에서 단계별 체크 가능
7. ⏳ PassScheduleService ICD 프로토콜 함수 개선: sendHeaderTrackingData(), sendInitialTrackingData(), sendAdditionalTrackingDataOptimized()
   - sendHeaderTrackingData(): getTrackingPassMst() 사용 (Keyhole 정보 포함)
   - sendInitialTrackingData(): getSelectedTrackDtlByMstId() 사용 (Keyhole 여부에 따라 적절한 DataType 반환)
   - sendAdditionalTrackingDataOptimized(): getSelectedTrackDtlByMstId() 사용 (Keyhole 여부에 따라 적절한 DataType 반환)
   - sendFromDatabase(): getSelectedTrackDtlByMstId() 사용 (Keyhole 여부에 따라 적절한 DataType 반환)
   - sendAdditionalTrackingDataLegacy(): getSelectedTrackDtlByMstId() 사용 (Keyhole 여부에 따라 적절한 DataType 반환)
8. ⏳ PassScheduleService Controller API 개선: getAllPassScheduleTrackMstMerged() 함수 추가 및 Keyhole 정보 포함
   - Service: getAllPassScheduleTrackMstMerged() 함수 추가 (EphemerisService의 getAllEphemerisTrackMstMerged() 참고)
   - Controller: /pass-schedule/tracking/master API에서 getAllPassScheduleTrackMstMerged() 사용
   - Keyhole 정보 포함: IsKeyhole, RecommendedTrainAngle 필드 추가
   - 프론트엔드 타입 개선: PassScheduleMasterData 인터페이스에 Keyhole 정보 필드 추가
   - 프론트엔드 타입 개선: ScheduleItem 인터페이스에 Keyhole 정보 필드 추가
   - 프론트엔드 매핑 개선: fetchScheduleDataFromServer()에서 Keyhole 정보 매핑 추가
   - 프론트엔드 UI 개선: SelectScheduleContent.vue에 Keyhole 정보 컬럼 추가 (EphemerisDesignationPage 참고)
9. ⏳ PassScheduleService 캐시 관련 함수 개선: preloadTrackingDataCache(), calculateDataLength()
   - preloadTrackingDataCache(): getSelectedTrackDtlByMstId() 사용 (Keyhole 여부에 따라 적절한 DataType 반환)
   - calculateDataLength(): getSelectedTrackDtlByMstId() 사용 (Keyhole 여부에 따라 적절한 DataType 반환)
10. ⏳ 테스트 및 검증

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
- **개선 필요**: 모든 ICD 프로토콜 함수에서 Keyhole 정보 활용

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

