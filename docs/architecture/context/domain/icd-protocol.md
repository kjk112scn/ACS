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
│         icdStore.ts (2,971줄)       │
└───────────────┬─────────────────────┘
                │ WebSocket
┌───────────────▼─────────────────────┐
│         Backend (Kotlin)            │
│         ICDService.kt (2,788줄)     │
│         UdpFwICDService.kt (1,228줄)│
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

## 프레임 형식

### STX/ETX 프레임 구조 (실제 구현)

```
┌─────┬──────────┬─────────┬───────┬─────┐
│ STX │ 명령코드 │ 데이터  │ CRC16 │ ETX │
│ 1B  │   1B     │  가변   │  2B   │ 1B  │
└─────┴──────────┴─────────┴───────┴─────┘
```

**프레임 상수:**
```kotlin
companion object {
    const val ICD_STX: Byte = 0x02  // 시작 바이트
    const val ICD_ETX: Byte = 0x03  // 종료 바이트
}
```

### 명령 타입 (Backend → HW)

| 클래스 | 명령 코드 | 설명 |
|-------|----------|------|
| `SatelliteTrackOne` | - | 위성 추적 명령 #1 (위치) |
| `SatelliteTrackTwo` | - | 위성 추적 명령 #2 (속도) |
| `SatelliteTrackThree` | - | 위성 추적 명령 #3 (보조) |
| `Standby` | - | 대기 명령 |
| `Stop` | - | 정지 명령 |
| `Emergency` | - | 비상 정지 |
| `MultiManualControl` | - | 멀티축 수동 제어 |
| `SingleManualControl` | - | 단일축 수동 제어 |
| `FeedCmd` | - | 피드 제어 |

### 상태 읽기 (HW → Backend)

```kotlin
// ICDService.kt 내부 클래스
class ReadStatus {
    // 상태 데이터 파싱
    fun parseGetDataFrame(buffer: ByteBuffer): GetDataFrame {
        // STX 확인
        // 데이터 파싱
        // CRC 검증
        // ETX 확인
    }
}
```

## ICDService 내부 구조

ICDService.kt (2,788줄)는 19개 내부 중첩 클래스로 구성:

```kotlin
@Service
class ICDService {
    // 패킷 분류
    inner class Classify {
        fun classifyPacket(buffer: ByteBuffer): PacketType
    }

    // 상태 읽기
    inner class ReadStatus {
        fun parse(buffer: ByteBuffer): StatusData
    }

    // 위성 추적 명령
    inner class SatelliteTrackOne {
        fun buildFrame(position: Position): SetDataFrame
    }
    inner class SatelliteTrackTwo {
        fun buildFrame(velocity: Velocity): SetDataFrame
    }
    inner class SatelliteTrackThree {
        fun buildFrame(auxiliary: AuxData): SetDataFrame
    }

    // 기본 제어 명령
    inner class Standby { ... }
    inner class Stop { ... }
    inner class Emergency { ... }

    // 매뉴얼 제어
    inner class MultiManualControl { ... }
    inner class SingleManualControl { ... }

    // 피드 제어
    inner class FeedCmd { ... }

    // CRC 계산
    companion object {
        fun calculateCRC16(data: ByteArray): Int
    }
}
```

## UdpFwICDService

```kotlin
// service/udp/UdpFwICDService.kt (1,228줄)
@Service
class UdpFwICDService(
    private val nettyConfig: NettyConfig
) {
    private var udpChannel: DatagramChannel? = null

    /**
     * UDP 채널 초기화
     */
    fun initialize() {
        udpChannel = DatagramChannel.open()
        udpChannel?.configureBlocking(false)
    }

    /**
     * 명령 프레임 전송
     */
    suspend fun sendFrame(frame: SetDataFrame) {
        val buffer = buildPacket(frame)
        udpChannel?.send(buffer, targetAddress)
    }

    /**
     * 상태 프레임 수신
     */
    suspend fun receiveFrame(): GetDataFrame {
        val buffer = ByteBuffer.allocate(MAX_PACKET_SIZE)
        udpChannel?.receive(buffer)
        return parsePacket(buffer)
    }

    private fun buildPacket(frame: SetDataFrame): ByteBuffer {
        // STX + 명령코드 + 데이터 + CRC16 + ETX
    }
}
```

## CRC-16 계산

```kotlin
// CRC16.kt
object CRC16 {
    private val CRC_TABLE = intArrayOf(/* 256 entries */)

    fun calculate(data: ByteArray): Int {
        var crc = 0xFFFF
        for (byte in data) {
            val index = (crc xor byte.toInt()) and 0xFF
            crc = (crc shr 8) xor CRC_TABLE[index]
        }
        return crc
    }
}
```

## Frontend 연동

### WebSocket 메시지

```typescript
// stores/icd/icdStore.ts
interface ICDMessage {
  type: 'status' | 'command' | 'error'
  timestamp: number
  data: AntennaStatus | CommandAck | ErrorInfo
}
```

### 상태 업데이트 (30ms)

```typescript
// stores/icd/icdStore.ts (2,971줄)
export const useIcdStore = defineStore('icd', () => {
  // shallowRef로 배치 업데이트
  const antennaPosition = shallowRef<AntennaPosition>({
    azimuth: { cmd: 0, actual: 0, speed: 0 },
    elevation: { cmd: 0, actual: 0, speed: 0 },
    train: { cmd: 0, actual: 0, speed: 0 }
  })

  const boardStatus = shallowRef<BoardStatus>({...})

  // WebSocket 연결
  function connectWebSocket() {
    ws = new WebSocket('ws://localhost:8080/ws/icd')
    ws.onmessage = handleMessage
  }

  // 30ms마다 호출
  function handleMessage(event: MessageEvent) {
    const data = JSON.parse(event.data)
    antennaPosition.value = { ...data.position }
    boardStatus.value = { ...data.board }
  }

  return { antennaPosition, boardStatus, connectWebSocket }
})
```

## 코드 위치

### Backend

| 기능 | 파일 | 줄 수 |
|------|------|------|
| ICD 프로토콜 처리 | `service/icd/ICDService.kt` | 2,788 |
| UDP 통신 | `service/udp/UdpFwICDService.kt` | 1,228 |
| WebSocket 브로드캐스트 | `service/websocket/PushDataService.kt` | - |
| WebSocket 설정 | `config/WebSocketConfig.kt` | - |

### Frontend

| 기능 | 파일 | 줄 수 |
|------|------|------|
| ICD 상태 관리 | `stores/icd/icdStore.ts` | 2,971 |
| ICD API 서비스 | `services/icdService.ts` | 814 |

## 에러 처리

### 통신 에러

| 상황 | 처리 |
|-----|------|
| UDP 타임아웃 | 재연결 시도 (3회) |
| CRC 불일치 | 메시지 폐기, 로그 |
| STX/ETX 오류 | 프레임 재동기화 |
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

### Backend 최적화

- **ByteBuffer 재사용**: 메모리 할당 최소화
- **비동기 UDP**: Netty 기반 논블로킹 I/O
- **배치 처리**: 여러 명령 묶어서 전송

### Frontend 최적화 (icdStore)

```typescript
// ❌ 비효율: 개별 ref 175개
const azimuth = ref(0)
const elevation = ref(0)
// ... 30ms마다 175회 업데이트 발생

// ✅ 효율: shallowRef 그룹화
const antennaState = shallowRef<AntennaState>({
  azimuth: 0,
  elevation: 0,
  train: 0,
  // ...
})
// 단일 객체 교체로 1회 업데이트
```

### UDP 버퍼

- 수신 버퍼: 64KB
- 송신 버퍼: 16KB
- 패킷 손실 시: 다음 주기 데이터 사용

## 주의사항

- **동기화**: 시스템 간 시간 동기화 필수 (NTP)
- **단위**: 프로토콜은 라디안, 표시 시 도(°) 변환 필요
- **바이트 순서**: Little Endian
- **체크섬**: CRC-16 사용
- **프레임 구분**: STX (0x02) / ETX (0x03)

## 참조

- [안테나 제어](antenna-control.md)
- [모드 시스템](mode-system.md)
- [FE 아키텍처](../architecture/frontend.md)
- [BE 아키텍처](../architecture/backend.md)

---

**최종 수정:** 2026-01-15
**분석 기반:** Phase 1-4 코드베이스 심층 분석

**변경 이력:**
- 2026-01-15: Gap 분석 반영 - 프로토콜 구조 전면 수정 (0xAA55 헤더 → STX/ETX 프레임)
