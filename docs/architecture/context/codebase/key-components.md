# 핵심 컴포넌트 (Key Components)

> ACS 시스템의 핵심 컴포넌트와 그 역할

## Frontend 핵심 컴포넌트

### icdStore.ts ⭐
**가장 중요한 스토어 - 안테나 상태 관리**

```typescript
// 역할: 안테나 실시간 상태 관리
// 주기: 30ms마다 WebSocket으로 업데이트
// 문제: 개별 ref 175개 → 성능 이슈

// 핵심 상태
const antennaState = shallowRef<AntennaState>({
  azimuth: 0,
  elevation: 0,
  train: 0,
  // ... 기타 상태
})

// WebSocket 연결
const ws = new WebSocket(...)
ws.onmessage = (event) => {
  antennaState.value = { ...parseMessage(event.data) }
}
```

**최적화 포인트:**
- shallowRef 사용 (배치 업데이트)
- 불필요한 리렌더링 방지
- computed 캐싱

### PassSchedulePage.vue
**패스 스케줄 관리 페이지**

```
역할:
- 위성 패스 스케줄 표시
- 차트 시각화
- 스케줄 편집

구성 요소 (분리 대상):
├── PassScheduleTable     # 스케줄 테이블
├── PassScheduleChart     # 시각화 차트
└── PassScheduleControls  # 제어 버튼
```

### EphemerisDesignationPage.vue
**위성 지정 추적 페이지**

```
역할:
- 위성 선택 및 TLE 표시
- 위성 위치 실시간 표시
- 추적 시작/정지

구성 요소 (분리 대상):
├── SatellitePositionView  # 위치 표시
├── SatelliteInfoPanel     # 정보 패널
└── SatelliteSelector      # 선택기
```

### 공통 Composables

| 이름 | 역할 | 사용처 |
|-----|------|--------|
| useErrorHandler | 에러 처리 | 모든 API 호출 |
| useNotification | 알림 표시 | 사용자 피드백 |
| useLoading | 로딩 상태 | 비동기 작업 |

## Backend 핵심 컴포넌트

### EphemerisService.kt ⭐
**궤도 계산의 핵심**

```kotlin
// 역할: TLE 관리, 궤도 전파, 패스 예측
// 의존: Orekit 라이브러리

주요 메서드:
├── getTle(noradId)           # TLE 조회
├── predictPasses(params)     # 패스 예측
├── calculatePosition(time)   # 위치 계산
└── getPointingAngle(time)    # 지향각 계산
```

**Orekit 연동:**
```kotlin
// TLE로 전파기 생성
val propagator = TLEPropagator.selectExtrapolator(tle)

// 특정 시각 위치 계산
val state = propagator.propagate(time)
val position = state.position  // ECI 좌표
```

### ICDService.kt ⭐
**안테나 통신의 핵심**

```kotlin
// 역할: UDP 통신, 명령 전송, 상태 수신
// 주기: 30ms

주요 메서드:
├── sendCommand(command)     # 명령 전송 (UDP)
├── receiveStatus()          # 상태 수신 (UDP)
├── parseMessage(buffer)     # 메시지 파싱
└── broadcastStatus(status)  # WebSocket 브로드캐스트
```

**통신 흐름:**
```
Backend ─── UDP ───▶ 안테나 HW
   │
   └─── WebSocket ───▶ Frontend
```

### TrackingService.kt
**실시간 추적 제어**

```kotlin
// 역할: 추적 모드 관리, 실시간 명령 생성

주요 메서드:
├── startTracking(satellite)  # 추적 시작
├── stopTracking()            # 추적 중지
├── updateTarget(position)    # 목표 갱신
└── getTrackingStatus()       # 상태 조회
```

### PassScheduleService.kt
**스케줄 관리**

```kotlin
// 역할: 패스 스케줄 관리, 자동 추적 전환

주요 메서드:
├── createSchedule(passes)    # 스케줄 생성
├── activateSchedule(id)      # 스케줄 활성화
├── getActiveSchedule()       # 활성 스케줄 조회
└── handlePassTransition()    # 패스 전환 처리
```

### Algorithm 컴포넌트

| 컴포넌트 | 역할 | 특징 |
|---------|------|------|
| OrekitCalculator | 궤도 계산 | 순수 함수 |
| TLEParser | TLE 파싱 | 포맷 검증 |
| PassPredictor | 패스 예측 | 시간 범위 계산 |
| ICDParser | ICD 파싱 | 바이트 처리 |
| ICDBuilder | ICD 생성 | 패킷 조립 |
| CoordinateConverter | 좌표 변환 | ECI↔Topocentric |

## 컴포넌트 간 관계

```
┌───────────────────────────────────────────────────────────┐
│                        Frontend                            │
│                                                            │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐       │
│  │   Pages    │───▶│   Stores   │◀───│ Composables│       │
│  │  (*.vue)   │    │   (*.ts)   │    │   (use*)   │       │
│  └─────┬──────┘    └─────┬──────┘    └────────────┘       │
│        │                 │                                 │
│        └────────┬────────┘                                 │
│                 │ REST API / WebSocket                     │
└─────────────────┼─────────────────────────────────────────┘
                  │
┌─────────────────┼─────────────────────────────────────────┐
│                 ▼          Backend                         │
│  ┌────────────────┐    ┌────────────────┐                 │
│  │   Controller   │───▶│    Service     │                 │
│  └────────────────┘    └───────┬────────┘                 │
│                                │                           │
│                                ▼                           │
│                        ┌────────────────┐                 │
│                        │   Algorithm    │                 │
│                        │ (순수 함수)    │                 │
│                        └────────────────┘                 │
└───────────────────────────────────────────────────────────┘
```

## 의존성 그래프

### Frontend
```
icdStore ◀─── 모든 페이지
    │
    └─── WebSocket ◀─── Backend

modeStore ◀─── 모드 페이지들
ephemerisStore ◀─── EphemerisDesignationPage
passScheduleStore ◀─── PassSchedulePage
```

### Backend
```
Controller ───▶ Service ───▶ Algorithm
                   │
                   └───▶ Repository (DB)
                   │
                   └───▶ ICDService (UDP)
```

## 참조

- [파일 구조](file-structure.md)
- [FE 아키텍처](../architecture/frontend.md)
- [BE 아키텍처](../architecture/backend.md)

---

**최종 수정:** 2026-01-14
