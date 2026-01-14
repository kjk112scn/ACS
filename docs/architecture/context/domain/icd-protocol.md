# ICD 통신 프로토콜 (ICD Protocol)

> ACS 시스템의 하드웨어 통신: 안테나 컨트롤러와의 UDP 기반 데이터 교환

## 개요

```
ACS Backend ←─ UDP (30ms) ─→ 안테나 컨트롤러
     │
     └─ WebSocket (30ms) ─→ ACS Frontend
```

## 통신 구조

### 레이어
```
┌─────────────────────────────────────┐
│         Frontend (Vue)              │
│         WebSocket Client            │
└───────────────┬─────────────────────┘
                │ WebSocket
┌───────────────▼─────────────────────┐
│         Backend (Kotlin)            │
│         ICDService                  │
└───────────────┬─────────────────────┘
                │ UDP
┌───────────────▼─────────────────────┐
│       안테나 컨트롤러 (HW)           │
└─────────────────────────────────────┘
```

### 통신 주기
| 구간 | 주기 | 프로토콜 |
|-----|------|---------|
| Backend ↔ HW | 30ms | UDP |
| Backend → FE | 30ms | WebSocket |

## 메시지 형식

### 명령 메시지 (Backend → HW)
```kotlin
data class ICDCommand(
    val header: Int,           // 0xAA55
    val commandType: Int,      // 명령 유형
    val azimuth: Double,       // 라디안
    val elevation: Double,     // 라디안
    val train: Double,         // 라디안
    val mode: Int,             // 모드 코드
    val timestamp: Long,       // UTC 밀리초
    val checksum: Int          // CRC
)
```

### 상태 메시지 (HW → Backend)
```kotlin
data class ICDStatus(
    val header: Int,           // 0x55AA
    val statusType: Int,       // 상태 유형

    // 현재 위치
    val currentAzimuth: Double,
    val currentElevation: Double,
    val currentTrain: Double,

    // 목표 위치
    val targetAzimuth: Double,
    val targetElevation: Double,
    val targetTrain: Double,

    // 모터 상태
    val azMotorStatus: MotorStatus,
    val elMotorStatus: MotorStatus,
    val trainMotorStatus: MotorStatus,

    // 시스템 상태
    val systemStatus: Int,
    val errorCode: Int,
    val timestamp: Long,
    val checksum: Int
)
```

### 모터 상태
```kotlin
enum class MotorStatus {
    IDLE,           // 정지
    MOVING,         // 이동 중
    BRAKING,        // 감속 중
    ERROR,          // 에러
    LIMIT_REACHED   // 리밋 도달
}
```

## 데이터 파싱

### 바이트 순서
- **Endianness**: Little Endian
- **Float**: IEEE 754 Single Precision
- **Double**: IEEE 754 Double Precision

### 파싱 예시
```kotlin
// ICDParser.kt
fun parseStatus(buffer: ByteBuffer): ICDStatus {
    buffer.order(ByteOrder.LITTLE_ENDIAN)

    val header = buffer.getInt()
    if (header != 0x55AA) throw InvalidHeaderException()

    return ICDStatus(
        header = header,
        statusType = buffer.getInt(),
        currentAzimuth = buffer.getDouble(),
        currentElevation = buffer.getDouble(),
        currentTrain = buffer.getDouble(),
        // ...
    )
}
```

## Frontend 연동

### WebSocket 메시지
```typescript
// icdStore.ts
interface ICDMessage {
  type: 'status' | 'command' | 'error'
  timestamp: number
  data: AntennaStatus | CommandAck | ErrorInfo
}
```

### 상태 업데이트 (30ms)
```typescript
// stores/icdStore.ts
const antennaState = shallowRef<AntennaState>({
  azimuth: 0,
  elevation: 0,
  train: 0,
  // ...
})

// WebSocket 메시지 수신 시
ws.onmessage = (event) => {
  const msg = JSON.parse(event.data)
  antennaState.value = { ...msg.data }
}
```

## 코드 위치

### Backend
| 기능 | 파일 |
|------|------|
| UDP 통신 | `service/ICDService.kt` |
| 메시지 파싱 | `algorithm/icd/ICDParser.kt` |
| 메시지 생성 | `algorithm/icd/ICDBuilder.kt` |
| WebSocket | `config/WebSocketConfig.kt` |

### Frontend
| 기능 | 파일 |
|------|------|
| WebSocket 연결 | `stores/icdStore.ts` |
| 상태 표시 | `components/AntennaStatus.vue` |

## 에러 처리

### 통신 에러
| 상황 | 처리 |
|-----|------|
| UDP 타임아웃 | 재연결 시도 (3회) |
| 잘못된 체크섬 | 메시지 폐기, 로그 |
| WebSocket 끊김 | 자동 재연결 |

### 시스템 에러 코드
| 코드 | 설명 |
|-----|------|
| 0x0000 | 정상 |
| 0x0001 | 통신 에러 |
| 0x0002 | 리밋 에러 |
| 0x0003 | 모터 에러 |
| 0x0004 | 인터락 에러 |

**상세 에러:** `docs/guides/Hardware_Error_Messages.md`

## 성능 고려사항

### icdStore 최적화
- **현재 문제**: 개별 ref 175개 → 30ms마다 175회 리렌더링
- **해결책**: shallowRef 그룹화 → 배치 업데이트

```typescript
// Before (비효율)
const azimuth = ref(0)
const elevation = ref(0)
// ... 173개 더

// After (최적화)
const antennaState = shallowRef<AntennaState>({...})
```

### UDP 버퍼
- 수신 버퍼: 64KB
- 송신 버퍼: 16KB
- 패킷 손실 시: 다음 주기 데이터 사용

## 주의사항

- **동기화**: 시스템 간 시간 동기화 필수 (NTP)
- **단위**: 프로토콜은 라디안, 변환 필요
- **바이트 정렬**: 구조체 패딩 주의
- **체크섬**: CRC-16 사용

## 참조

- [안테나 제어](antenna-control.md)
- [모드 시스템](mode-system.md)
- [FE 아키텍처](../architecture/frontend.md)

---

**최종 수정:** 2026-01-14
