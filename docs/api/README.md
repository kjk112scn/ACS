# Protocols - 통신 프로토콜 문서

> 펌웨어(ACU)와의 ICD 통신 프로토콜 상세 문서

## 핵심 서비스

### ICDService (ICD 프로토콜 구현)

**파일**: `backend/src/.../service/icd/ICDService.kt` (약 2,800줄)

| 기능 | 설명 |
|------|------|
| 명령 프레임 생성 | ICD 규격 바이트 배열 생성 |
| 응답 프레임 파싱 | 펌웨어 응답 데이터 파싱 |
| CRC16 검증 | 데이터 무결성 검증 |
| 상태 데이터 업데이트 | DataStoreService 연동 |

**상세 문서**: [ICDService.md](ICDService.md)

## 프레임 구조

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
| Command | ASCII | 명령 식별자 |
| Data | Variable | 명령별 데이터 |
| CRC16 | UShort | Little Endian |
| ETX | 0x03 | End of Text |

## 주요 명령 코드

| 코드 | 명령 | 방향 | 설명 |
|------|------|------|------|
| W | DefaultInfo | BE→FW | 기본 정보 송신 |
| RR | ReadStatus | FW→BE | 상태 읽기 (190B) |
| E | Emergency | BE→FW | 비상 정지 |
| M | SingleManualControl | BE→FW | 단축 수동 제어 |
| A | MultiManualControl | BE→FW | 다축 수동 제어 |
| S | Stop | BE→FW | 정지 |
| B | Standby | BE→FW | 대기 모드 |
| F | FeedOnOff | BE→FW | RF Feed 제어 |
| TT | SatelliteTrackOne | BE→FW | 추적 헤더 (12.1) |
| TM | SatelliteTrackTwo | BE→FW | 추적 초기 데이터 (12.2) |
| TR | SatelliteTrackThree | FW→BE | 추적 데이터 요청 (12.3) |

## 문서 목록

| 문서 | 설명 |
|------|------|
| [ICDService.md](ICDService.md) | ICD 서비스 전체 문서 |

## 통신 흐름

```
┌─────────────────┐      UDP 10ms     ┌─────────────────┐
│    Firmware     │ ◄───────────────► │   ICDService    │
│   (안테나 ACU)   │                   │  (프로토콜 처리) │
└─────────────────┘                   └─────────────────┘
                                              │
                                      ┌───────┴───────┐
                                      │               │
                              ┌───────▼───────┐  ┌────▼─────┐
                              │DataStoreService│  │ACSEventBus│
                              │  (상태 저장)   │  │ (이벤트)  │
                              └───────────────┘  └──────────┘
```

## UDP 설정

```properties
# backend/src/main/resources/application.properties
firmware.udp.ip=192.168.0.202
firmware.udp.port=1001
server.udp.ip=192.168.0.200
server.udp.port=1000
```

## 에러 처리

- **CRC 검증 실패**: 패킷 무시, 로그 기록
- **프레임 길이 오류**: 패킷 무시, 경고 로그
- **패킷 지연 (80ms+)**: 경고 로그, 모니터링

## 관련 문서

- [EphemerisService](../algorithms/EphemerisService.md) - 위성 추적 (TT/TM/TR 사용)
- [SYSTEM_OVERVIEW](../architecture/SYSTEM_OVERVIEW.md) - 전체 통신 아키텍처
- [Hardware_Error_Messages](../Hardware_Error_Messages.md) - 에러 메시지 관리

---

**최종 업데이트**: 2026-01-07
