# Initial_HW_Error_Detection 설계 문서

## 1. 설계 의도

### Why (왜 이렇게 설계했는가)

현재 `HardwareErrorLogService.processAntennaData()`는 비트 **변화**만 감지합니다:
- `previousBits != currentBits` 조건에서만 에러 분석
- 첫 번째 수신 시(`previousBits == null`)는 상태 저장만 수행

이로 인해 BE 시작 전에 이미 HW 에러 상태인 경우:
- 비트 변화가 없으므로 에러로 인식되지 않음
- 운영자가 에러 상태를 인지하지 못함

### 대안 분석

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|:----:|
| A. 첫 수신 시 에러 체크 추가 | 간단, 기존 구조 유지 | - | ✅ |
| B. 별도 초기화 서비스 생성 | 관심사 분리 | 복잡도 증가 | ❌ |
| C. Polling으로 초기 상태 확인 | - | 불필요한 네트워크 사용 | ❌ |

## 2. 구현 계획

### 2.1 HardwareErrorLog 엔티티 수정

```kotlin
// HardwareErrorLog.kt
data class HardwareErrorLog(
    val id: String,
    val timestamp: String,
    val category: String,
    val severity: String,
    val errorKey: String,
    val component: String,
    val isResolved: Boolean,
    val resolvedAt: String?,
    val isInitialError: Boolean = false  // ✨ 추가: 초기 에러 구분
)
```

### 2.2 HardwareErrorLogService 수정

```kotlin
// HardwareErrorLogService.kt:97-113 수정

val previousBits = previousBitStates[bitType]

if (previousBits == null) {
    // ✨ 첫 번째 수신: 초기 에러 상태 확인
    logger.info("📍 {} 첫 수신 - 초기 에러 상태 확인: {}", bitType, currentBits)
    previousBitStates[bitType] = currentBits

    // 초기 상태에서 에러 비트가 활성화되어 있는지 확인
    val errorMappings = getErrorMappings(bitType)
    val reversedBits = currentBits.padStart(8, '0').reversed()

    errorMappings.forEach { (bitPosition, errorConfig) ->
        val bitValue = reversedBits.getOrNull(bitPosition)?.toString() ?: "0"

        if (bitValue == "1") {
            val error = HardwareErrorLog(
                id = "${bitType}-${bitPosition}-${System.currentTimeMillis()}",
                timestamp = LocalDateTime.now().toString(),
                category = errorConfig.category,
                severity = errorConfig.severity,
                errorKey = errorConfig.errorKey,
                component = errorConfig.component,
                isResolved = false,
                resolvedAt = null,
                isInitialError = true  // ✨ 초기 에러 마킹
            )
            newErrors.add(error)
            logger.info("📍 초기 에러 감지: {} - {}", errorConfig.component, errorConfig.errorKey)
        }
    }
    hasStateChanged = newErrors.isNotEmpty()

} else if (previousBits != currentBits) {
    // 기존 로직: 비트 변화 감지
    val errors = analyzeBitChanges(currentBits, previousBits, bitType)
    newErrors.addAll(errors)
    hasStateChanged = true
    previousBitStates[bitType] = currentBits
}
```

### 2.3 DB 스키마 변경 (선택)

```sql
-- hardware_error_logs 테이블
ALTER TABLE hardware_error_logs
ADD COLUMN is_initial_error BOOLEAN DEFAULT false;
```

### 2.4 Frontend 변경 (선택)

```typescript
// hardwareErrorLogStore.ts
interface HardwareErrorLog {
  // ... 기존 필드
  isInitialError?: boolean  // 초기 에러 구분
}

// 상태바에서 초기 에러 표시 (선택)
// "[시작 시 감지]" 또는 아이콘으로 구분
```

## 3. 데이터 흐름

```
BE 시작
    ↓
UDP 서비스 초기화 (T+2초)
    ↓
첫 번째 UDP 응답 수신 (T+2.01초)
    ↓
processAntennaData() 호출
    ↓
previousBits == null 확인
    ↓
┌─────────────────────────────────────────┐
│ 초기 에러 비트 확인                      │
│   └─ 비트 = 1 → HardwareErrorLog 생성   │
│       └─ isInitialError = true          │
│       └─ DB 저장                        │
│       └─ WebSocket 푸시                 │
└─────────────────────────────────────────┘
    ↓
FE 에러 상태바 표시
```

## 4. 테스트 계획

### 4.1 단위 테스트

- [ ] `processAntennaData()` - previousBits == null 시 에러 감지
- [ ] `isInitialError` 플래그 정상 설정
- [ ] 에러 매핑 정상 동작

### 4.2 통합 테스트

- [ ] BE 시작 → UDP 수신 → 초기 에러 DB 저장
- [ ] WebSocket 전송 → FE 수신

### 4.3 수동 테스트

| 테스트 | 절차 | 기대 결과 |
|--------|------|----------|
| T1 | HW 에러 상태에서 BE 시작 | FE 에러 상태바에 표시됨 |
| T2 | HW 정상 상태에서 BE 시작 | 에러 없음 |
| T3 | BE 시작 후 HW 에러 발생 | 런타임 에러로 표시 (`isInitialError=false`) |
| T4 | DB 확인 | 초기 에러에 `is_initial_error=true` |

## 5. 중복 방지 고려

**문제:** BE 재시작 시 같은 초기 에러가 중복 저장될 수 있음

**해결책:**
1. 초기 에러 저장 전 DB에서 동일 에러 확인
2. 또는 초기 에러는 DB 저장하지 않고 메모리만 (선택)

```kotlin
// 중복 방지 로직 (옵션)
if (isInitialError && existsUnresolvedError(errorKey, component)) {
    logger.info("이미 존재하는 초기 에러 - 저장 생략: {}", errorKey)
    return
}
```

## 6. 관련 ADR

- 필요 시 ADR 생성: "초기 HW 에러 감지 방식 결정"