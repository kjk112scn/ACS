# HW_Error_System_Integration 설계 문서

## 1. 설계 의도

### Why (왜 이렇게 설계했는가)

hardware_error_log 테이블에 4개 컬럼이 항상 null로 저장되고 있음:
- `tracking_mode` - 에러 발생 시 추적 모드 알 수 없음
- `correlation_id` - 동시 발생 에러 연관성 파악 불가
- `raw_data` - 에러 원인 디버깅 정보 없음
- `session_id` - 어떤 세션에서 발생했는지 알 수 없음

이 정보들을 시스템과 연계하면:
- 에러 분석 및 디버깅 용이
- 연관 에러 일괄 처리 가능
- 특정 모드/세션의 에러 패턴 분석 가능

### 대안 분석

| 대안 | 장점 | 단점 | 선택 여부 |
|------|------|------|----------|
| **DataStoreService 의존성** | 이미 추적 상태 관리 중, 순환 참조 없음 | - | ✅ 선택 |
| ModeService 새로 생성 | 명확한 책임 분리 | 불필요한 클래스 추가 | ❌ |
| EphemerisService 직접 주입 | 상세 상태 접근 | 순환 참조 위험, 과도한 결합 | ❌ |

## 2. 구현 계획

### 2.1 우선순위

| 순위 | 컬럼 | 복잡도 | 가치 | 상태 |
|------|------|--------|------|------|
| 1 | tracking_mode | LOW | HIGH | 구현 예정 |
| 2 | correlation_id | LOW | MEDIUM | 구현 예정 |
| 3 | raw_data | MEDIUM | HIGH | 구현 예정 |
| 4 | session_id | HIGH | LOW | 보류 |

### 2.2 데이터 흐름 변경

```
현재:
processAntennaData() → analyzeBitChanges() → HardwareErrorLog 생성
                                                    ↓
                                            mapErrorLogToEntity()
                                                    ↓
                                            trackingMode = null
                                            correlationId = null
                                            rawData = null

변경 후:
processAntennaData(data)
    │
    ├─ correlationId = UUID.randomUUID()  ← 호출 시작점
    ├─ trackingMode = dataStoreService.getActiveTrackingMode()
    │
    └─► analyzeBitChanges(currentBits, previousBits, bitType, fullData, correlationId)
            │
            └─► HardwareErrorLog 생성 (rawData, correlationId 포함)
                    │
                    └─► mapErrorLogToEntity()
                            │
                            └─► trackingMode = trackingMode
                                correlationId = correlationId
                                rawData = rawDataJson
```

### 2.3 Backend 변경사항

#### HardwareErrorLogService.kt 수정

```kotlin
@Service
class HardwareErrorLogService(
    private val hardwareErrorLogRepository: HardwareErrorLogRepository?,
    private val dataStoreService: DataStoreService  // ✅ 추가
) {
    fun processAntennaData(data: PushData.ReadData): ErrorUpdateResult {
        // ✅ 호출 시작점에서 correlation_id 생성
        val correlationId = UUID.randomUUID()

        // ✅ 현재 추적 모드 조회
        val trackingMode = dataStoreService.getActiveTrackingMode()

        val newErrors = mutableListOf<HardwareErrorLog>()

        bitTypes.forEach { bitType ->
            val currentBits = getBitString(data, bitType) ?: return@forEach
            val previousBits = previousBitStates[bitType]

            val errors = when {
                previousBits == null -> analyzeInitialErrors(
                    currentBits, bitType, data, correlationId, trackingMode
                )
                previousBits != currentBits -> analyzeBitChanges(
                    currentBits, previousBits, bitType, data, correlationId, trackingMode
                )
                else -> emptyList()
            }

            newErrors.addAll(errors)
            previousBitStates[bitType] = currentBits
        }

        // 단독 에러는 correlation_id null
        if (newErrors.size < 2) {
            newErrors.forEach { it.correlationId = null }
        }

        newErrors.forEach { addErrorLog(it) }
        return ErrorUpdateResult(...)
    }
}
```

#### HardwareErrorLog data class 수정

```kotlin
data class HardwareErrorLog(
    val id: String,
    val timestamp: String,
    val category: String,
    val severity: String,
    val errorKey: String,
    val component: String,
    val isResolved: Boolean,
    val resolvedAt: String?,
    val isInitialError: Boolean = false,
    // ✅ 추가 필드
    val rawData: String? = null,
    val correlationId: UUID? = null,
    val trackingMode: String? = null
)
```

#### raw_data JSON 스키마

```json
{
  "version": "1.0",
  "bitType": "elevationBoardServoStatusBits",
  "currentBits": "00100000",
  "previousBits": "00000000",
  "changedPosition": 5,
  "changeType": "0→1",
  "antennaState": {
    "azimuth": 45.5,
    "elevation": 30.2,
    "train": 0.0
  }
}
```

### 2.4 isInitialError 구분 확인

| 상황 | 동작 | isInitialError |
|------|------|----------------|
| BE 시작 후 첫 UDP 수신 | `previousBits == null` → `analyzeInitialErrors()` | `true` |
| 이후 비트 변화 감지 | `previousBits != currentBits` → `analyzeBitChanges()` | `false` |

## 3. 테스트 계획

- [ ] tracking_mode: 각 모드(Standby/Ephemeris/PassSchedule/SunTrack)에서 에러 발생 시 값 확인
- [ ] correlation_id: 동일 UDP에서 여러 에러 발생 시 같은 UUID 확인
- [ ] raw_data: JSON 파싱 가능 여부, 필수 필드 존재 확인
- [ ] isInitialError: BE 재시작 시 기존 에러 true로 마킹 확인

## 4. 관련 ADR

- 해당 없음 (기존 아키텍처 범위 내)