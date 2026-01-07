# ACS 시스템 통합 개요

> 펌웨어 ↔ 백엔드 ↔ 프론트엔드 연계 구조

---

## 시스템 아키텍처

```
┌─────────────────┐      UDP        ┌─────────────────┐    WebSocket    ┌─────────────────┐
│    Firmware     │ ◄─────────────► │  Backend API    │ ◄─────────────► │   Frontend UI   │
│  (안테나 제어)   │    10ms 주기     │ (Spring Boot)   │    30ms 주기     │  (Vue/Quasar)   │
└─────────────────┘                 └─────────────────┘    REST API     └─────────────────┘
        │                                   │                                   │
   물리적 제어                          비즈니스 로직                         사용자 인터페이스
   - 모터 제어                          - 데이터 처리                        - 상태 표시
   - 센서 읽기                          - 알고리즘 계산                      - 명령 입력
   - 상태 보고                          - 명령 변환                          - 차트/그래프
```

---

## 통신 프로토콜

| 구간 | 프로토콜 | 주기 | 용도 |
|------|---------|------|------|
| FW → BE | UDP | 10ms | 상태 데이터 수신 |
| BE → FW | UDP | 즉시 | 명령 전송 |
| BE → FE | WebSocket | 30ms | 실시간 데이터 브로드캐스트 |
| FE → BE | REST API | 요청시 | 명령/설정 전송 |

---

## 1. 펌웨어 ↔ 백엔드 (UDP/ICD)

### 통신 설정
```properties
# backend/src/main/resources/application.properties
firmware.udp.ip=192.168.0.202
firmware.udp.port=1001
server.udp.ip=192.168.0.200
server.udp.port=1000
```

### 수신 데이터 (FW → BE)

| 카테고리 | 항목 |
|---------|------|
| 각도 | Azimuth, Elevation, Train(Tilt) |
| 속도 | Azimuth/Elevation/Train Speed |
| 환경 | Wind Speed, Wind Direction, RTD Temperature |
| 상태 | 서보 드라이버, 토크, 각 축 상태 비트 |
| RF | S/X/Ka Band LNA Current, RSSI |

### 송신 명령 (BE → FW)

| 명령 | 설명 |
|-----|------|
| ServoPreset | 각 축 서보 프리셋 |
| Standby | 대기 모드 설정 |
| EmergencyStop | 비상 정지 (E/S) |
| MultiControl | 3축 동시 제어 (각도/속도) |
| FeedOnOff | RF Feed 제어 (16비트) |
| PositionOffset | 위치 오프셋 |
| Stop/Stow | 정지/수납 |

### 핵심 파일
```
backend/.../
├── service/udp/UdpFwICDService.kt    # UDP 통신 처리
├── service/icd/ICDService.kt          # ICD 프로토콜 파싱
├── controller/icd/ICDController.kt    # REST → UDP 변환
└── model/PushData.kt                  # 데이터 모델
```

**관련 문서**: [ICDService 상세](../protocols/ICDService.md)

---

## 2. 백엔드 ↔ 프론트엔드 (WebSocket)

### 엔드포인트
```
WebSocket: ws://localhost:8080/ws
전송 주기: 30ms (33.3 msg/s)
```

### 전송 데이터 구조
```typescript
interface WebSocketMessage {
  topic: "read"
  data: {
    data: {
      azimuthAngle: number
      elevationAngle: number
      trainAngle: number
      azimuthSpeed: number
      elevationSpeed: number
      trainSpeed: number
      windSpeed: number
      windDirection: number
      // ... 40개 이상 상태 필드
    }
    trackingStatus: {
      ephemerisStatus: boolean
      passScheduleStatus: boolean
      sunTrackStatus: boolean
    }
    serverTime: string
    cmdAzimuthAngle: number
    cmdElevationAngle: number
    cmdTrainAngle: number
    udpConnected: boolean
    errorData: { ... }
  }
}
```

### 핵심 파일
```
backend/.../
├── controller/websocket/PushDataController.kt  # WebSocket 핸들러
├── service/websocket/PushDataService.kt        # 데이터 생성
└── config/WebSocketConfig.kt                   # 설정

frontend/src/
├── services/api/icdService.ts      # WebSocket 클라이언트
├── stores/icd/icdStore.ts          # 상태 관리
└── utils/connectionManager.ts      # 연결 관리
```

---

## 3. 프론트엔드 → 백엔드 (REST API)

### 주요 API 엔드포인트

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/icd/servo-preset-command` | POST | 서보 프리셋 |
| `/api/icd/standby-command` | POST | 대기 모드 |
| `/api/icd/on-emergency-stop-command` | POST | 비상 정지 |
| `/api/icd/multi-control-command` | POST | 멀티 제어 |
| `/api/icd/feed-on-off-command` | POST | Feed 제어 |
| `/api/icd/position-offset-command` | POST | 위치 오프셋 |
| `/api/icd/stop-command` | POST | 정지 |
| `/api/icd/stow-command` | POST | 수납 |

---

## 4. 데이터 흐름

### 실시간 사이클 (30ms)

```
1. Firmware
   └─► UDP 패킷 전송 (10ms 주기)

2. Backend UdpFwICDService
   └─► ByteArray → PushData.ReadData 파싱
   └─► 오프셋 적용
   └─► DataStoreService 저장

3. Backend PushDataService (30ms)
   └─► 최신 데이터 조회
   └─► 추적 상태, 에러 정보 추가
   └─► JSON 변환

4. Backend PushDataController
   └─► WebSocket 브로드캐스트

5. Frontend icdService
   └─► WebSocket 메시지 수신
   └─► icdStore 상태 업데이트
   └─► Vue 컴포넌트 리렌더링
```

### 명령 전송

```
1. Frontend 사용자 입력
   └─► icdService 메서드 호출

2. REST API POST 요청
   └─► ICDController 처리

3. Backend UdpFwICDService
   └─► 명령 객체 생성
   └─► UDP로 펌웨어 전송
```

---

## 5. 연결 상태 관리

### 백엔드
| 변수 | 타입 | 용도 |
|-----|------|------|
| `isUdpRunning` | AtomicBoolean | UDP 연결 상태 |
| `connectedSessions` | ConcurrentHashMap | WebSocket 세션 |
| `isBroadcastActive` | AtomicBoolean | 브로드캐스트 활성화 |

### 프론트엔드
| 상태 | 값 | 설명 |
|-----|-----|------|
| CONNECTING | 0 | 연결 중 |
| OPEN | 1 | 연결됨 |
| CLOSING | 2 | 닫는 중 |
| CLOSED | 3 | 닫힘 |

- 자동 재연결: 최대 5회, 3초 간격
- 백엔드 재시작 감지: 5초 이상 끊김 시

---

## 6. 성능 특성

| 항목 | 값 |
|-----|-----|
| WebSocket 주기 | 30ms (33.3 msg/s) |
| UDP 수신 주기 | 10ms (100 msg/s) |
| 에러율 목표 | < 1% |
| 세션 타임아웃 | 30초 |

### 모니터링
```kotlin
// 성능 확인
PushDataController.getRealtimeStats()
PushDataController.checkRealtimePerformance()
PushDataController.diagnoseBroadcastHealth()
```

---

## 7. 용어 규칙 (Train/Tilt)

### 중요: 축 명칭과 UI 표시 구분

| 맥락 | 용어 | 사용 위치 |
|-----|------|----------|
| **UI 표시** | **Tilt** | 프론트엔드 화면, 사용자 문서 |
| **축 제어** | **Train** | 백엔드, ICD, 펌웨어, 변수명 |
| **알고리즘** | **Tilt** | 기울기 계산 적용 시 |

### 데이터 흐름에서의 변환
```
┌─────────────┐     trainAngle      ┌─────────────┐    "Tilt: 45.0°"   ┌─────────────┐
│  Firmware   │ ──────────────────► │   Backend   │ ─────────────────► │  Frontend   │
│  Train 축   │                     │  trainAngle │   표시 시 Tilt     │   UI 표시   │
└─────────────┘                     └─────────────┘                    └─────────────┘
```

### 코드 규칙
```typescript
// 백엔드/프론트엔드 변수명: train 사용
trainAngle: number
trainSpeed: number
cmdTrainAngle: number

// 프론트엔드 UI 표시: Tilt로 변환
<span>Tilt: {{ trainAngle }}°</span>
```

### 이유
- **Train**: 안테나의 물리적 축 이름 (기계적 명칭)
- **Tilt**: 사용자가 이해하기 쉬운 "기울기" 개념

---

## 8. 설정 파일 위치

| 구분 | 파일 | 내용 |
|-----|------|------|
| BE UDP | `application.properties` | IP, Port 설정 |
| BE WebSocket | `WebSocketConfig.kt` | 엔드포인트 설정 |
| FE API | `api-config.ts` | API URL 설정 |
| FE HTTP | `axios.ts` | 타임아웃 설정 |

---

## 9. 관련 문서

### 핵심 서비스 문서

| 서비스 | 문서 | 설명 |
|--------|------|------|
| EphemerisService | [EphemerisService.md](../algorithms/EphemerisService.md) | 위성 궤도 추적 (5,000줄) |
| ICDService | [ICDService.md](../protocols/ICDService.md) | ICD 프로토콜 처리 (2,800줄) |

### 참조 문서

- [알고리즘 문서](../algorithms/README.md)
- [프로토콜 문서](../protocols/README.md)
- [참조 문서 인덱스](../README.md)

---

## 10. Claude Code 확장 시스템 (v2.0.0)

### 개요

ACS 프로젝트는 Claude Code v2.0.0 확장 시스템을 활용하여 개발 생산성을 극대화합니다.

### 에이전트 (15개)

| 분류 | 에이전트 | 역할 |
|------|---------|------|
| 조율 | tech-lead | 기술 총괄, 에이전트 조율 |
| 설계 | architect, database-architect, project-manager | 시스템 설계, DB 설계, 작업 관리 |
| 개발 | backend-dev, frontend-dev, algorithm-expert, fullstack-helper | 각 영역별 개발 지원 |
| 개발 | api-contract-manager, design-system-builder | API 계약, 디자인 시스템 |
| 품질 | code-reviewer, test-expert, debugger, refactorer, performance-analyzer | 코드 품질 관리 |
| 문서 | doc-syncer | 문서 관리 총괄 |

### 스킬 (12개)

| 스킬 | 역할 |
|------|------|
| `/sync` | 코드-문서 동기화 |
| `/feature` | 신규 기능 개발 |
| `/bugfix` | 버그 수정 |
| `/done` | 작업 마무리 |
| `/adr` | ADR 생성 |
| `/plan` | 작업 계획 |
| `/status` | 현황 보고 |
| `/docs` | 문서화 |
| `/health` | 건강 상태 점검 |
| `/guide` | 사용법 안내 |
| `/migrate` | 마이그레이션 관리 (v2.0) |
| `/api-sync` | API 자동 동기화 (v2.0) |

### 참조

- [.claude/README.md](../../../.claude/README.md) - Claude Code 전체 구성
- [QUICK_START.md](../../../.claude/QUICK_START.md) - 빠른 시작 가이드
- [AGENT_COLLABORATION_GUIDE.md](../../../.claude/AGENT_COLLABORATION_GUIDE.md) - 에이전트 협업 가이드

---

**문서 버전**: 1.2.0
**최종 업데이트**: 2026-01-07
**변경사항**: Claude Code v2.0.0 섹션 추가
