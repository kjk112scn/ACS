# 데이터 흐름 (Data Flow)

> ACS 시스템의 데이터 흐름: 사용자 입력부터 안테나 제어까지

## 전체 흐름도

```
┌────────────────────────────────────────────────────────────────┐
│                         Frontend                                │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│  │   UI     │───▶│  Store   │◀───│ WebSocket│◀──┐              │
│  │ (Vue)    │    │ (Pinia)  │    │  Client  │   │              │
│  └────┬─────┘    └────┬─────┘    └──────────┘   │              │
│       │               │                          │              │
└───────┼───────────────┼──────────────────────────┼──────────────┘
        │ REST API      │ REST API                 │ WebSocket
        ▼               ▼                          │
┌───────────────────────────────────────────────────┼──────────────┐
│                         Backend                   │              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    │              │
│  │Controller│───▶│ Service  │───▶│Algorithm │    │              │
│  └──────────┘    └────┬─────┘    └──────────┘    │              │
│                       │                          │              │
│                       ▼                          │              │
│                  ┌──────────┐    ┌──────────┐    │              │
│                  │   ICD    │───▶│ WebSocket│────┘              │
│                  │ Service  │    │  Server  │                   │
│                  └────┬─────┘    └──────────┘                   │
│                       │                                         │
└───────────────────────┼─────────────────────────────────────────┘
                        │ UDP (30ms)
                        ▼
               ┌──────────────────┐
               │   안테나 HW      │
               │   컨트롤러      │
               └──────────────────┘
```

## 주요 데이터 흐름

### 1. 위성 추적 명령 흐름

```
사용자: 위성 선택 → "추적 시작"
           │
           ▼
┌──────────────────────────────────────┐
│ Frontend                              │
│ 1. UI에서 위성 선택                   │
│ 2. ephemerisStore.startTracking()    │
│ 3. POST /api/tracking/start          │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│ Backend                               │
│ 1. TrackingController 수신           │
│ 2. TrackingService.startTracking()   │
│ 3. EphemerisService.calculatePosition│
│ 4. 30ms 타이머 시작                   │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│ 30ms 주기 루프                        │
│ 1. 현재 시각 위성 위치 계산           │
│ 2. 지향각(Az/El) 계산                │
│ 3. ICDService.sendCommand()          │
│ 4. UDP 패킷 전송                      │
└──────────────────────────────────────┘
```

### 2. 상태 모니터링 흐름

```
안테나 HW: 상태 응답 (30ms 주기)
           │
           ▼
┌──────────────────────────────────────┐
│ Backend                               │
│ 1. UDP 수신                          │
│ 2. ICDParser.parseStatus()           │
│ 3. WebSocket broadcast               │
└──────────────────┬───────────────────┘
                   │ WebSocket
                   ▼
┌──────────────────────────────────────┐
│ Frontend                              │
│ 1. WebSocket onmessage               │
│ 2. icdStore.updateState()            │
│ 3. 컴포넌트 자동 리렌더링             │
└──────────────────────────────────────┘
```

### 3. 패스 스케줄 흐름

```
사용자: 스케줄 설정 → 저장
           │
           ▼
┌──────────────────────────────────────┐
│ Frontend                              │
│ 1. PassSchedulePage.vue              │
│ 2. passScheduleStore.saveSchedule()  │
│ 3. POST /api/schedule                │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│ Backend                               │
│ 1. PassScheduleController            │
│ 2. PassScheduleService.save()        │
│ 3. 스케줄 저장                        │
│ 4. 타이머 설정 (패스 시작 시간)       │
└──────────────────────────────────────┘
                   │
                   │ 패스 시작 시간 도달
                   ▼
┌──────────────────────────────────────┐
│ 자동 추적 시작                        │
│ 1. 모드 전환: PassSchedule           │
│ 2. 위성 추적 시작                     │
│ 3. 패스 종료 시 다음 위성 전환        │
└──────────────────────────────────────┘
```

## 데이터 타입

### Frontend → Backend (요청)

```typescript
// 추적 시작 요청
interface StartTrackingRequest {
  noradId: string
  mode: 'ephemeris' | 'sun'
}

// 패스 예측 요청
interface PredictPassRequest {
  noradId: string
  startTime: string  // ISO 8601
  endTime: string    // ISO 8601
  minElevation: number  // 최소 고도각 (도)
}

// 수동 제어 요청
interface ManualControlRequest {
  azimuth: number    // 도
  elevation: number  // 도
  train: number      // 도
}
```

### Backend → Frontend (응답)

```typescript
// 안테나 상태 (WebSocket, 30ms)
interface AntennaStatus {
  currentAzimuth: number    // 도
  currentElevation: number  // 도
  currentTrain: number      // 도
  targetAzimuth: number
  targetElevation: number
  targetTrain: number
  azMotorStatus: MotorStatus
  elMotorStatus: MotorStatus
  trainMotorStatus: MotorStatus
  systemStatus: number
  errorCode: number
  timestamp: number  // Unix ms
}

// 패스 예측 결과
interface PassPrediction {
  noradId: string
  satelliteName: string
  aos: string       // 시작 시간
  los: string       // 종료 시간
  maxElevation: number
  maxElevationTime: string
  duration: number  // 초
}
```

### Backend ↔ HW (ICD)

```kotlin
// 명령 (Backend → HW)
data class ICDCommand(
    val azimuth: Double,     // 라디안
    val elevation: Double,   // 라디안
    val train: Double,       // 라디안
    val mode: Int,
    val timestamp: Long
)

// 상태 (HW → Backend)
data class ICDStatus(
    val currentAz: Double,   // 라디안
    val currentEl: Double,   // 라디안
    val currentTrain: Double,// 라디안
    val targetAz: Double,
    val targetEl: Double,
    val targetTrain: Double,
    val motorStatus: Int,
    val errorCode: Int,
    val timestamp: Long
)
```

## 단위 변환

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │     │   Backend   │     │     HW      │
│    (도)     │ ←→  │   (혼합)    │ ←→  │   (라디안)  │
└─────────────┘     └─────────────┘     └─────────────┘

API: 도(degree) 사용 - 사용자 친화적
내부 계산: 라디안 - 수학 연산 효율
ICD: 라디안 - 하드웨어 프로토콜
```

### 변환 유틸

```typescript
// Frontend
export const toRadians = (deg: number) => deg * Math.PI / 180
export const toDegrees = (rad: number) => rad * 180 / Math.PI
```

```kotlin
// Backend
fun Double.toRadians() = Math.toRadians(this)
fun Double.toDegrees() = Math.toDegrees(this)
```

## 시간 처리

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │     │   Backend   │     │   Orekit    │
│  (로컬시간) │ ←→  │    (UTC)    │ ←→  │ (AbsoluteDate)│
└─────────────┘     └─────────────┘     └─────────────┘

API: ISO 8601 문자열 (UTC)
내부: AbsoluteDate (Orekit)
표시: 로컬 시간대
```

## 주의사항

- **30ms 주기**: UDP/WebSocket 모두 30ms 주기 유지
- **단위 일관성**: 레이어별 단위 변환 주의
- **시간 동기화**: NTP로 시스템 간 동기화 필수
- **에러 전파**: 하위 에러는 상위로 적절히 변환

## 참조

- [ICD 프로토콜](../domain/icd-protocol.md)
- [Frontend 아키텍처](frontend.md)
- [Backend 아키텍처](backend.md)

---

**최종 수정:** 2026-01-14
