# Frontend Store/Service 심층 분석

> 분석 일시: 2026-01-15
> 분석 대상: frontend/src/stores/, frontend/src/services/
> 총 코드: 8,546줄

## 1. icdStore.ts (2,971줄)

### 1.1 목적 및 역할

- **실시간 하드웨어 상태 관리**: WebSocket을 통해 백엔드에서 수신한 ICD 메시지 처리
- **다국어 에러 메시지 변환**: 하드웨어 에러 키를 i18n으로 번역
- **전체 안테나 시스템 상태 관리**: 약 100+ 상태 변수로 구성

### 1.2 상태 구조

#### 기본 상태

```typescript
// 서버 관련
serverTime: string                    // 서버 시간
resultTimeOffsetCalTime: string       // 결과 시간 오프셋 계산 시간
messageDelay: number                  // WebSocket 메시지 지연 시간(ms)
error: string                         // 에러 메시지
isConnected: boolean                  // 연결 상태

// 명령 관련 상태
cmdAzimuthAngle: string               // 방위각 명령값
cmdElevationAngle: string             // 고도각 명령값
cmdTrainAngle: string                 // 틸트각 명령값
cmdTime: string                       // 명령 시간
```

#### 안테나 위치 데이터

```typescript
// 일반 위치 (Normal)
azimuthAngle: string                  // 현재 방위각
elevationAngle: string                // 현재 고도각
trainAngle: string                    // 현재 틸트각

// 서보 드라이버 위치 (Servo Driver)
servoDriverAzimuthAngle: string
servoDriverElevationAngle: string
servoDriverTrainAngle: string

// 속도 정보
azimuthSpeed: string
elevationSpeed: string
trainSpeed: string
```

#### 추적 관련 상태

```typescript
// 위성 궤도 추적 (Ephemeris Tracking)
ephemerisStatus: boolean | null
ephemerisTrackingState: string | null    // 'TRACKING' | 'IDLE' | ...
trackingAzimuthTime: string
trackingCMDAzimuthAngle: string
trackingActualAzimuthAngle: string
trackingElevationTime: string
trackingCMDElevationAngle: string
trackingActualElevationAngle: string
trackingTrainTime: string
trackingCMDTrainAngle: string
trackingActualTrainAngle: string

// 패스 스케줄 추적
passScheduleStatus: boolean | null
passScheduleTrackingState: string | null

// 태양 추적
sunTrackStatus: boolean | null
sunTrackTrackingState: string | null
```

#### 보드별 상태 비트

```typescript
// 메인 보드
mainBoardProtocolStatusBits: string
mainBoardStatusBits: string
mainBoardMCOnOffBits: string
mainBoardReserveBits: string

// 축별 보드 (방위각, 고도각, 틸트)
azimuthBoardServoStatusBits: string
azimuthBoardStatusBits: string
elevationBoardServoStatusBits: string
elevationBoardStatusBits: string
trainBoardServoStatusBits: string
trainBoardStatusBits: string

// 피드 보드 (대역별)
feedSBoardStatusBits: string
feedXBoardStatusBits: string
feedKaBoardStatusBits: string
feedBoardETCStatusBits: string
```

### 1.3 주요 Computed Properties

```typescript
const elevationBoardServoStatusInfo = computed(() => ({
  raw: elevationBoardServoStatusBits.value,

  // 개별 상태
  servoAlarmCode1/2/3/4/5: boolean
  servoAlarm: boolean
  servoBrake: boolean
  servoMotor: boolean

  // 활성화된 알람 목록
  activeAlarmCodes: string[]
  activeServoStatuses: ['Alarm', 'Brake', 'Motor']

  // 통합 요약
  summary: {
    totalAlarmCodes: number
    hasAnyAlarmCode: boolean
    hasServoAlarm: boolean
    isBrakeActive: boolean
    isMotorActive: boolean
    overallStatus: 'ALARM' | 'ACTIVE' | 'STANDBY'
  }
}))
```

### 1.4 WebSocket 통신 구조

```typescript
// icdService 임포트 (싱글톤 패턴)
import { icdService, type MessageData } from '@/services'

// WebSocket 메시지 핸들러 등록
async function setupICDConnection() {
  await icdService.connectWebSocket(url, (message: MessageData) => {
    updateStateFromMessage(message)
  })
}
```

### 1.5 에러 처리 (i18n 기반)

```typescript
const translateHardwareError = (errorKey: string, isResolved: boolean): string => {
  const i18nKey = `hardwareErrors.${key}`
  const translatedMessage = t(i18nKey)

  if (translatedMessage === i18nKey) {
    console.warn(`하드웨어 에러 메시지 번역 실패: ${i18nKey}`)
    return errorKey
  }
  return translatedMessage
}
```

---

## 2. passScheduleStore.ts (2,452줄)

### 2.1 목적 및 역할

- **위성 패스 스케줄 관리**: TLE 데이터 기반 패스 데이터 관리
- **실시간 추적 경로 업데이트**: Web Worker 사용 최적화
- **Keyhole 위성 필터링**: Train 각도 추천
- **추적 경로 시각화**: 예측 vs 실제 경로 비교

### 2.2 상태 구조

#### 마스터 데이터

```typescript
interface ScheduleItem {
  mstId: number                         // 전역 고유 마스터 ID (필수)
  detailId: number                      // 패스 인덱스 (현재 항상 0)
  no: number                            // UI 표시용 재순번

  // 위성 정보
  satelliteId?: string
  satelliteName: string

  // 시간 정보
  startTime: string                     // ISO 8601 형식
  endTime: string
  duration: string

  // 각도 정보
  startAzimuthAngle: number
  endAzimuthAngle: number
  startElevationAngle: number
  endElevationAngle: number
  train: number

  // 최대값
  maxAzimuthRate?: number
  maxElevationRate?: number
  maxElevation?: number

  // Keyhole 정보
  isKeyhole?: boolean
  recommendedTrainAngle?: number
}
```

#### 추적 관련 상태

```typescript
const scheduleData = ref<ScheduleItem[]>([])
const selectedScheduleList = ref<ScheduleItem[]>([])
const selectedSchedule = ref<ScheduleItem | null>(null)

// 추적 상세 데이터
const trackingDetailData = ref<TrackingDetailItem[]>([])
const predictedTrackingPath = ref<[number, number][]>([])
const actualTrackingPath = ref<[number, number][]>([])

// 현재 추적 위치
const currentTrackingPosition = ref<{
  azimuth: number
  elevation: number
}>({ azimuth: 0, elevation: 0 })
```

### 2.3 Worker 기반 성능 최적화

```typescript
const ADAPTIVE_CONFIG = {
  maxPoints: 0,                         // 0 = 무제한
  threshold: 0.1,                       // 중복 포인트 제거 임계값 (도)
  memoryLimit: 50000,                   // 최대 포인트 수
  cleanupThreshold: 40000,              // 정리 시작 기준
}

const workerStats = ref({
  totalJobs: number
  successfulJobs: number
  failedJobs: number
  averageProcessingTime: number
  currentPathPoints: number
  lastProcessingTime: number
  isProcessing: boolean
})
```

### 2.4 메모리 관리

```typescript
const cleanupOldPoints = (path: [number, number][]): [number, number][] => {
  if (path.length > ADAPTIVE_CONFIG.cleanupThreshold) {
    const removeCount = Math.floor(path.length * 0.1)
    return path.slice(removeCount)
  }
  return path
}
```

### 2.5 주요 Actions

| Action | 역할 |
|--------|------|
| `addTleAndGenerateTracking()` | TLE 업로드 및 추적 계획 생성 |
| `selectSchedule()` | 스케줄 선택 및 상세 데이터 로드 |
| `startTracking()` | 추적 시작 |
| `stopTracking()` | 추적 중지 |

---

## 3. ephemerisTrackStore.ts (1,287줄)

### 3.1 목적 및 역할

- **TLE 기반 위성 궤도 추적**: Ephemeris 알고리즘 계산
- **실시간 추적 경로 시각화**: Worker 기반 고성능 업데이트
- **정지궤도 위성 추적**: 정지궤도 위성 각도 계산

### 3.2 상태 구조

```typescript
const masterData = ref<ScheduleItem[]>([])
const detailData = ref<ScheduleDetailItem[]>([])
const rawDetailData = ref<ScheduleDetailItem[]>([])

interface TrackingPath {
  rawPath: [number, number][]
  sampledPath: [number, number][]
  lastUpdateTime: number
}

const trackingPath = ref<TrackingPath>({
  rawPath: [],
  sampledPath: [],
  lastUpdateTime: 0,
})

// 정지궤도 추적 정보
const geostationaryAngles = ref({
  azimuth: 0,
  elevation: 0,
  satelliteName: '',
  tleLine1: '',
  tleLine2: '',
  isSet: boolean,
})
```

### 3.3 Worker 기반 경로 업데이트

```typescript
const createInlineWorker = (): Worker => {
  const workerScript = `
    self.onmessage = function(e) {
      const { azimuth, elevation, currentPath, maxPoints, threshold } = e.data

      // 입력 검증 및 정규화
      const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
      const normalizedEl = Math.max(0, Math.min(90, elevation))

      // 중복 체크 (임계값 기준)
      if (currentPath.length > 0) {
        const lastPoint = currentPath[currentPath.length - 1]
        const azDiff = Math.abs(lastPoint[1] - normalizedAz)
        const elDiff = Math.abs(lastPoint[0] - normalizedEl)

        if (azDiff < threshold && elDiff < threshold) {
          return
        }
      }

      // 새 포인트 추가
      const updatedPath = [...currentPath, [normalizedEl, normalizedAz]]
      self.postMessage({ updatedPath, ... })
    }
  `

  const blob = new Blob([workerScript], { type: 'application/javascript' })
  return new Worker(URL.createObjectURL(blob))
}
```

### 3.4 localStorage 기반 상태 복원

```typescript
const dataToSave = {
  trajectoryPath: [number, number][],
  trackingPath: [number, number][],
  selectedSchedule: ScheduleItem | null,
  tleDisplayData: TLEData,
  savedAt: number,
}
```

---

## 4. settingsStore.ts

### 4.1 목적

시스템 전체 설정을 계층화된 구조로 관리:
- Location Settings
- Tracking Settings
- Stow Settings
- Antenna Spec Settings
- Angle Limits
- Speed Limits
- Offset Limits
- Algorithm Settings

### 4.2 변경사항 추적

```typescript
const hasUnsavedChanges = ref({
  location: boolean
  tracking: boolean
  stowAngle: boolean
  stowSpeed: boolean
  // ...
})

const pendingChanges = ref({
  location: LocationSettings | null
  tracking: TrackingSettings | null
  // ...
})
```

---

## 5. icdService.ts (814줄)

### 5.1 WebSocket 통신 핵심

```typescript
class WebSocketService {
  private static instance: WebSocketService | null = null
  private websocket: WebSocket | null = null
  private messageHandler: WebSocketMessageHandler | null = null

  // 재연결 관리
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000

  // 구독자 관리 (Pub/Sub 패턴)
  private subscribers = new Map<string, WebSocketMessageHandler[]>()
}
```

### 5.2 연결 생명주기

```typescript
async connect(url: string, onMessage: WebSocketMessageHandler): Promise<void>
private attemptReconnect(): void  // 지수 백오프
disconnect(): void
```

### 5.3 Pub/Sub 패턴

```typescript
subscribe(key: string, handler: WebSocketMessageHandler): void
private broadcastMessage(message: MessageData): void
```

### 5.4 REST API 명령어

| 메서드 | 역할 |
|--------|------|
| `sendEmergency()` | 긴급 정지 |
| `sendWriteNTP()` | 시간 동기화 |
| `startSunTrack()` | 태양 추적 시작 |
| `stopSunTrack()` | 태양 추적 중지 |
| `standbyCommand()` | Standby 명령 |
| `stopCommand()` | 정지 명령 |
| `stowCommand()` | Stow 명령 |
| `sendMultiControlCommand()` | 멀티 컨트롤 |

---

## 6. passScheduleService.ts (1,117줄)

### 6.1 주요 인터페이스

```typescript
interface AddTleAndTrackingRequest {
  satelliteId?: string
  satelliteName?: string
  tleLine1: string
  tleLine2: string
}

interface TleAndTrackingResponse {
  success: boolean
  message: string
  data: {
    satelliteId: string
    satelliteName: string
    passCount: number
    trackingPointCount: number
    passes: PassInfo[]
  }
}
```

### 6.2 주요 API 메서드

| 메서드 | 역할 |
|--------|------|
| `fetchPassScheduleMasterData()` | 마스터 데이터 조회 |
| `fetchPassScheduleDetailData(mstId)` | 상세 데이터 조회 |
| `startPassScheduleTracking(mstId)` | 추적 시작 |
| `stopPassScheduleTracking()` | 추적 중지 |
| `addTleAndGenerateTracking()` | TLE 업로드 |

---

## 7. ephemerisTrackService.ts (1,192줄)

### 7.1 주요 인터페이스

```typescript
interface EphemerisTrackRequest {
  tleLine1: string
  tleLine2: string
  satelliteName?: string
  startTime: string
  endTime: string
  stepSize: number
}

interface GeostationaryTrackingRequest {
  tleLine1: string
  tleLine2: string
}
```

### 7.2 주요 API 메서드

| 메서드 | 역할 |
|--------|------|
| `fetchEphemerisMasterData()` | 마스터 데이터 조회 |
| `fetchEphemerisDetailData(mstId, detailId)` | 상세 데이터 조회 |
| `parseTLEData()` | TLE 텍스트 파싱 |
| `generateEphemerisTrack()` | 궤도 계산 |
| `startEphemerisTracking()` | 추적 시작 |
| `stopEphemerisTracking()` | 추적 중지 |
| `calculateGeostationaryAngles()` | 정지궤도 각도 계산 |
| `startGeostationaryTracking()` | 정지궤도 추적 시작 |

---

## 8. Store 간 의존성 및 데이터 흐름

### 8.1 의존성 그래프

```
┌─────────────────────────────────────────┐
│         icdStore (WebSocket)            │
│  (실시간 하드웨어 상태, 추적 위치)      │
└────────────┬────────────────────────────┘
             │ (trackingActualAzimuthAngle, etc.)
             ↓
┌─────────────────────────────────────────┐
│      passScheduleStore / ephemerisStore  │
│  (추적 경로 업데이트 및 시각화)         │
└─────────────────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│         settingsStore                    │
│  (추적 알고리즘 설정)                   │
└─────────────────────────────────────────┘
```

### 8.2 데이터 흐름 (실시간 추적)

```
1. WebSocket 메시지 수신 (icdService)
   ↓
2. icdStore 상태 업데이트
   ↓
3. passScheduleStore 반응성 (100ms 주기)
   ↓
4. Web Worker에서 경로 계산
   ↓
5. actualTrackingPath 업데이트
   ↓
6. 화면 렌더링 (Chart.js/SVG)
```

---

## 9. 설계 패턴

### 9.1 사용된 패턴

| 패턴 | 적용 위치 |
|------|----------|
| Setup Store Pattern | 모든 Pinia Store |
| Pub/Sub Pattern | WebSocket 구독자 관리 |
| Singleton Pattern | icdService |
| Adapter Pattern | 백엔드 응답 필드명 변환 |
| Worker Pattern | 병렬 처리 |

### 9.2 타입 안정성

- TypeScript 강타입 사용
- 인터페이스 정의 (API 요청/응답)
- Union Types 활용

### 9.3 성능 최적화 전략

1. **Web Worker 활용**: 메시지 파싱, 경로 계산
2. **메모리 관리**: 포인트 수 제한 (50,000개)
3. **중복 제거**: 임계값 이하 변화 무시
4. **타이머 최적화**: 100ms 주기 업데이트

---

**문서 버전**: 1.0.0
**작성자**: FE Expert Agent
**최종 검토**: 2026-01-15
