# ICDService

## 개요

ICDService는 ACS(Antenna Control System) 백엔드에서 펌웨어(ACU)와의 **ICD(Interface Control Document) 프로토콜**을 구현하는 핵심 서비스입니다. UDP를 통해 안테나 제어 명령을 송신하고 상태 데이터를 수신합니다.

**파일 위치**: `backend/src/main/kotlin/com/gtlsystems/acs_api/service/icd/ICDService.kt`

**파일 크기**: 약 2,789줄 (대형 서비스)

## 핵심 기능

| 기능 | 설명 |
|------|------|
| 명령 프레임 생성 | ICD 규격에 맞는 바이트 배열 명령 프레임 생성 |
| 응답 프레임 파싱 | 펌웨어로부터 수신된 바이트 배열을 데이터 객체로 변환 |
| CRC16 검증 | 데이터 무결성 검증을 위한 CRC16 체크섬 계산 및 검증 |
| 상태 데이터 업데이트 | 수신된 상태 데이터를 DataStoreService에 전달 |
| 이벤트 발행 | 특정 명령 수신 시 ACSEventBus를 통해 이벤트 발행 |

## 통신 프로토콜

### 프레임 구조

```
+-----+----------+----------+--------+-----+
| STX | Command  |   Data   | CRC16  | ETX |
+-----+----------+----------+--------+-----+
| 1B  | 1-2B     | Variable | 2B     | 1B  |
+-----+----------+----------+--------+-----+
```

| 필드 | 값 | 설명 |
|------|-----|------|
| STX | 0x02 | Start of Text |
| Command | ASCII | 명령 식별자 (1-2 바이트) |
| Data | Variable | 명령별 데이터 |
| CRC16 | UShort | 체크섬 (Little Endian) |
| ETX | 0x03 | End of Text |

### 엔디안

- **기본**: Big Endian (네트워크 바이트 순서)
- **CRC16**: Little Endian 저장
- **Float/Int**: Big Endian

## 명령 체계

### 명령 코드 목록

| 코드 | 클래스 | 설명 | 프레임 길이 |
|------|--------|------|-------------|
| W | DefaultInfo | 기본 정보 송신 | 30B |
| RR | ReadStatus | 상태 읽기 | 191B (수신) |
| RF | ReadFwVerSerialNoStatus | 펌웨어 버전/시리얼 | 46B |
| I | WriteNTP | NTP 시간 정보 송신 | 18B |
| E | Emergency | 비상 정지 명령 | 6B |
| M | SingleManualControl | 단축 수동 제어 | 14B |
| A | MultiManualControl | 다축 수동 제어 | 30B |
| S | Stop | 정지 명령 | 6B |
| B | Standby | 대기 모드 | 6B |
| F | FeedOnOff | Feed On/Off 제어 | 7B |
| TT | SatelliteTrackOne | 위성 추적 헤더 | 26B |
| TM | SatelliteTrackTwo | 위성 추적 초기 제어 | 가변 |
| TR | SatelliteTrackThree | 위성 추적 데이터 요청 | 가변 |
| OT | TimeOffset | 시간 오프셋 | 19B |
| OP | PositionOffset | 위치 오프셋 | 18B |
| PP | ServoEncoderPreset | 서보 엔코더 프리셋 | 7B |
| PA | ServoAlarmReset | 서보 알람 리셋 | 7B |
| C | MCOnOff | M/C On/Off | 6B |

## 주요 메서드

### Classify (명령 분류기)

| 메서드 | 파라미터 | 설명 |
|--------|---------|------|
| `receivedCmd` | ByteArray | 수신된 바이트 배열 분석 후 적절한 파서 호출 |
| `monitorPacketTiming` | ByteArray | 패킷 수신 타이밍 모니터링 (80ms 초과 시 경고) |

### SetDataFrame (송신용 프레임 생성)

| 클래스 | 주요 필드 | 반환 |
|--------|----------|------|
| `Standby.SetDataFrame` | axis: BitSet | ByteArray |
| `MultiManualControl.SetDataFrame` | axis, angles, speeds | ByteArray |
| `Emergency.SetDataFrame` | cmdOnOff: Boolean | ByteArray |
| `FeedOnOff.SetDataFrame` | feedOnOff: BitSet | ByteArray |
| `SatelliteTrackOne.SetDataFrame` | AOS/LOS 시간 정보 | ByteArray |
| `SatelliteTrackTwo.SetDataFrame` | NTP 정보, 추적 데이터 | ByteArray |

### GetDataFrame (수신 프레임 파싱)

| 클래스 | 주요 반환 필드 | FRAME_LENGTH |
|--------|---------------|--------------|
| `ReadStatus.GetDataFrame` | angles, speeds, status bits | 190B |
| `ReadFwVerSerialNoStatus.GetDataFrame` | FW versions, serial numbers | 46B |
| `SatelliteTrackThree.GetDataFrame` | requestDataLength, timeAcc | 12B |

## 의존성

| 의존성 | 용도 |
|--------|------|
| `DataStoreService` | 수신된 상태 데이터 저장 |
| `ACSEventBus` | 이벤트 발행 (위성 추적 관련) |
| `GlobalData.Offset` | 위치/시간 오프셋 적용 |
| `PushData.ReadData` | 상태 데이터 모델 |
| `SystemInfo` | 펌웨어 버전 정보 저장 |
| `Crc16` | CRC16 체크섬 계산 |
| `JKUtil.JKConvert` | 바이트 변환 유틸리티 |

## 상태 데이터 구조 (ReadStatus)

### 각도/속도 데이터

| 필드 | 타입 | 설명 |
|------|------|------|
| `azimuthAngle` | Float | 방위각 |
| `elevationAngle` | Float | 고도각 |
| `tiltAngle` | Float | Train(Tilt) 각도 |
| `azimuthSpeed` | Float | 방위각 속도 |
| `elevationSpeed` | Float | 고도각 속도 |
| `tiltSpeed` | Float | Train 속도 |

### 환경 데이터

| 필드 | 타입 | 설명 |
|------|------|------|
| `windSpeed` | Float | 풍속 |
| `windDirection` | UShort | 풍향 |
| `rtdOne` | Float | RTD 온도 센서 1 |
| `rtdTwo` | Float | RTD 온도 센서 2 |

### 상태 비트

| 필드 | 설명 |
|------|------|
| `modeStatusBits` | 현재 운용 모드 |
| `mainBoardStatusBits` | 메인보드 상태 |
| `mainBoardMCOnOffBits` | M/C On/Off 상태 |
| `azimuthBoardServoStatusBits` | 방위각 서보 상태 |
| `elevationBoardServoStatusBits` | 고도각 서보 상태 |
| `tiltBoardServoStatusBits` | Train 서보 상태 |
| `feedSBoardStatusBits` | S-Band 상태 |
| `feedXBoardStatusBits` | X-Band 상태 |
| `feedKaBoardStatusBits` | Ka-Band 상태 |

## 사용 예시

### 명령 송신 (MultiManualControl)

```kotlin
// 3축 동시 제어 명령 생성
val axis = BitSet().apply {
    set(0)  // Azimuth
    set(1)  // Elevation
    set(2)  // Train
}

val frame = ICDService.MultiManualControl.SetDataFrame(
    cmdOne = 'A',
    axis = axis,
    azimuthAngle = 45.0f,
    azimuthSpeed = 5.0f,
    elevationAngle = 30.0f,
    elevationSpeed = 3.0f,
    trainAngle = 0.0f,
    trainSpeed = 2.0f
)

val byteArray = frame.setDataFrame()
// UDP로 전송
```

### 응답 수신 (Classify)

```kotlin
// DataStoreService와 ACSEventBus 주입
val classify = ICDService.Classify(dataStoreService, acsEventBus)

// UDP로 수신된 바이트 배열
val receivedData: ByteArray = ...

// 자동 분류 및 처리
classify.receivedCmd(receivedData)
// ReadStatus -> DataStoreService.updateDataFromUdp()
// SatelliteTrack -> ACSEventBus.publish()
```

## 에러 처리

### CRC 검증 실패

```kotlin
if (rxChecksum != crc16Check) {
    println("CRC 체크 실패:")
    println("  수신 CRC: 0x${rxChecksum.toString(16)}")
    println("  계산 CRC: 0x${crc16Check.toString(16)}")
    return null
}
```

### 프레임 길이 검증

```kotlin
if (data.size < FRAME_LENGTH) {
    println("수신 데이터 길이 부족: ${data.size} < $FRAME_LENGTH")
    return null
}
```

### 패킷 타이밍 모니터링

```kotlin
if (interval > 80.0) {  // 80ms 초과 시 경고
    logger.warn("패킷 지연 감지: ${interval}ms")
}
```

## Train/Tilt 명명 규칙

- **ICD 프로토콜 내부**: `tiltAngle`, `tiltSpeed` (원본 변수명)
- **데이터 모델**: `trainAngle`, `trainSpeed` (표준화)
- **UI 표시**: "Tilt" (사용자 친화적)

## 관련 서비스

| 서비스 | 역할 |
|--------|------|
| `UdpFwICDService` | UDP 통신 처리, ICDService 호출 |
| `ICDController` | REST API -> ICD 명령 변환 |
| `DataStoreService` | 상태 데이터 저장소 |
| `PushDataService` | WebSocket 브로드캐스트 |

## 관련 문서

- [시스템 통합 개요](../architecture/SYSTEM_OVERVIEW.md)
- [EphemerisService](../algorithms/EphemerisService.md)
- [에러 메시지 관리](../Hardware_Error_Messages.md)

---

**문서 버전**: 1.0.0
**최종 업데이트**: 2026-01-07
