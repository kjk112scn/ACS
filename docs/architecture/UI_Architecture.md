# UI 아키텍처

> 프론트엔드 화면 구성 및 컴포넌트 구조

---

## 1. 기술 스택

| 항목 | 기술 | 버전 |
|-----|------|------|
| 프레임워크 | Vue 3 + TypeScript | 3.x |
| UI 라이브러리 | Quasar | 2.16.0 |
| 상태 관리 | Pinia | 3.0.2 |
| 라우팅 | Vue Router | 4.0.12 |
| 국제화 | Vue i18n | 9.2.2 |
| 차트 | ECharts, Chart.js | 5.6.0, 4.4.9 |

---

## 2. 레이아웃 구조

### 2.1 레이아웃 파일
```
frontend/src/layouts/
├── MainLayout.vue    # 메인 레이아웃
└── LoginLayout.vue   # 로그인 레이아웃
```

### 2.2 MainLayout 구성
```
MainLayout
├── q-header (상단 헤더)
│   ├── 좌측: 메뉴 버튼 + GTL 로고
│   ├── 중앙: "Antenna Control System"
│   └── 우측: UTC/Local 시간 + 서버상태 + 설정버튼
├── q-drawer (좌측 메뉴)
├── q-page-container
│   └── <router-view />
├── SettingsModal (설정 모달)
└── error-status-bar (하단 에러 상태바)
```

---

## 3. 페이지 구조

### 3.1 라우팅
```
/
├── /login                    # 로그인
├── /dashboard               # 메인 (MainLayout)
│   ├── /dashboard/standby   # 대기 모드 (기본)
│   ├── /dashboard/step      # 스텝 모드
│   ├── /dashboard/slew      # 슬루 모드
│   ├── /dashboard/pedestal  # 페더스탈
│   ├── /dashboard/ephemeris # 에피메리스
│   ├── /dashboard/pass-schedule  # 패스 스케줄
│   ├── /dashboard/suntrack  # 태양 추적
│   └── /dashboard/feed      # 피드
├── /popup                   # 팝업 (인증 스킵)
│   ├── /popup/all-status
│   ├── /popup/system-info
│   ├── /popup/axis-transform-calculator
│   └── /popup/hardware-error-log
└── /:catchAll               # 404
```

### 3.2 모드별 페이지
| 모드 | 파일 | 설명 |
|-----|------|------|
| Standby | `StandbyPage.vue` | 축 선택 + 대기/Stow |
| Step | `StepPage.vue` | 각도/속도 입력 |
| Slew | `SlewPage.vue` | 속도 제어 (3패널) |
| Pedestal | `PedestalPositionPage.vue` | 위치 표시 |
| Ephemeris | `EphemerisDesignationPage.vue` | 궤도 지정 |
| PassSchedule | `PassSchedulePage.vue` | 스케줄 관리 |
| SunTrack | `SunTrackPage.vue` | 태양 추적 |
| Feed | `FeedPage.vue` | 피드 제어 |

---

## 4. 컴포넌트 계층

### 4.1 디렉토리 구조
```
frontend/src/components/
├── common/           # 공통 컴포넌트
│   └── EssentialLink.vue
├── content/          # 콘텐츠 컴포넌트
│   ├── AllStatusContent.vue
│   ├── SystemInfoContent.vue
│   ├── AxisTransformCalculator.vue
│   ├── SelectScheduleContent.vue
│   └── TLEUploadContent.vue
├── Settings/         # 설정 컴포넌트
│   ├── SettingsModal.vue      # 메인 설정 모달
│   ├── GeneralSettings.vue
│   ├── LanguageSettings.vue
│   ├── VersionInfoSettings.vue
│   ├── admin/                 # 관리자 설정
│   │   ├── AdminSettings.vue
│   │   ├── ServoAlarmResetSettings.vue
│   │   ├── ServoEncoderPresetSettings.vue
│   │   └── MCOffSettings.vue
│   └── system/                # 시스템 설정
│       ├── SystemSettings.vue
│       ├── AlgorithmSettings.vue
│       ├── AngleLimitsSettings.vue
│       ├── AntennaSpecSettings.vue
│       ├── LocationSettings.vue
│       ├── OffsetLimitsSettings.vue
│       ├── SpeedLimitsSettings.vue
│       └── StowSettings.vue
└── HardwareErrorLogPanel.vue
```

### 4.2 설정 모달 탭 구조
| 탭 | 컴포넌트 | 내용 |
|---|---------|------|
| General | GeneralSettings | 다크모드 |
| Connection | - | WebSocket/API 주소 |
| System | SystemSettings | 시스템 세부 설정 |
| Language | LanguageSettings | 언어 선택 |
| Admin | AdminSettings | 관리자 전용 |
| Version | VersionInfoSettings | 버전 정보 |

---

## 5. 상태 관리 (Pinia)

### 5.1 스토어 구조
```
frontend/src/stores/
├── api/settings/      # 설정 스토어들
│   ├── settingsStore.ts         # 마스터
│   ├── algorithmSettingsStore.ts
│   ├── angleLimitsSettingsStore.ts
│   ├── locationSettingsStore.ts
│   ├── speedLimitsSettingsStore.ts
│   └── stowSettingsStore.ts
├── common/            # 공통 스토어
│   ├── auth.ts        # 인증
│   └── modeStore.ts   # 모드 데이터 매핑
├── icd/               # ICD 통신
│   └── icdStore.ts    # WebSocket, 에러 관리
├── mode/              # 모드별 상태
│   ├── standbyStore.ts
│   ├── stepStore.ts
│   ├── slewStore.ts
│   ├── ephemerisTrackStore.ts
│   └── passScheduleStore.ts
└── hardwareErrorLogStore.ts
```

### 5.2 핵심 스토어

#### useICDStore (ICD 통신)
```typescript
// 상태
serverTime: string
cmdAzimuthAngle: string
cmdElevationAngle: string
cmdTrainAngle: string      // Train (UI: Tilt)
isConnected: boolean
errorStatusBarData: ErrorStatusData

// 에러 상태
errorStatusBarData: {
  latestError: {
    message: string
    severity: 'ERROR' | 'CRITICAL' | 'WARNING' | 'INFO'
  }
  activeErrorCount: number
}
```

#### useModeStore (모드 데이터 매핑)
```typescript
// 축 매핑: 내부 train → API tiltAngle
MODE_DATA_MAPPINGS: {
  ephemeris: {
    train: { cmd: 'trackingCMDTiltAngle', actual: 'trackingActualTiltAngle' }
  }
  step: {
    train: { cmd: 'cmdTiltAngle', actual: 'tiltAngle' }
  }
}
```

---

## 6. 용어 매핑 (Train ↔ Tilt)

### 6.1 변환 규칙

| 위치 | 용어 | 예시 |
|-----|------|------|
| **코드 변수** | `train` | `trainAngle`, `trainSpeed` |
| **API 필드** | `tilt` | `tiltAngle`, `cmdTiltAngle` |
| **UI 라벨** | `Tilt` | 버튼, 체크박스, 입력 필드 |
| **i18n 한글** | `기울기` | 화면 표시 |

### 6.2 변환 위치

```vue
<!-- StandbyPage.vue -->
<q-checkbox v-model="trainChecked" label="Tilt" />

<!-- 내부 모델 -->
const trainChecked = computed({
  get: () => standbyStore.selectedAxes.train,
  set: (value) => standbyStore.updateAxis('train', value)
})
```

```vue
<!-- SlewPage.vue -->
<div class="axis-title">Tilt</div>
<q-input v-model="slewStore.speeds.train" />
```

### 6.3 i18n 키
```typescript
// en-US
settings.admin.axes.tilt: 'Tilt'

// ko-KR
settings.admin.axes.tilt: '기울기'
```

---

## 7. 테마 시스템

### 7.1 테마 파일
```
frontend/src/
├── composables/useTheme.ts      # 테마 관리
├── css/theme-variables.scss     # CSS 변수
└── boot/dark-mode.ts            # 초기화
```

### 7.2 테마 종류
| 테마 | 배경색 | 텍스트 |
|-----|--------|--------|
| Dark | `#0F1419` | `#FFFFFF` |
| Light | `#FFFFFF` | `#212121` |
| ACS | `#0F1419` | `#FFFFFF` |

### 7.3 CSS 변수
```scss
// 기본 색상
--theme-primary: #091d24
--theme-background: #0F1419
--theme-card-background: #091d24
--theme-text: #FFFFFF
--theme-border: #37474F

// 축 색상
--theme-dashboard-tiltColor: #4CAF50
--theme-dashboard-azimuthColor: #FF5722
--theme-dashboard-elevationColor: #2196F3

// 상태 색상
--theme-positive: #00E676
--theme-negative: #F44336
```

### 7.4 테마 변경
```typescript
const { setTheme, initializeTheme } = useTheme()
setTheme('dark')  // 테마 변경
```

---

## 8. 국제화 (i18n)

### 8.1 파일 구조
```
frontend/src/i18n/
├── index.ts          # 초기화
├── en-US/index.ts    # 영어
└── ko-KR/index.ts    # 한국어 (기본)
```

### 8.2 번역 카테고리
| 카테고리 | 내용 |
|---------|------|
| common | 공통 버튼/텍스트 |
| dashboard | 대시보드 (Azimuth, Elevation, Tilt) |
| modes | 모드명 |
| settings | 설정 관련 |
| errors | 에러 메시지 |
| hardwareErrors | 하드웨어 에러 |

### 8.3 사용법
```typescript
const { t } = useI18n()
t('dashboard.azimuth')  // "방위각" 또는 "Azimuth"
```

---

## 9. 서비스 구조

### 9.1 파일 위치
```
frontend/src/services/
├── api/
│   ├── icdService.ts      # WebSocket, REST API
│   └── settingsService.ts # 설정 API
└── mode/
    ├── ephemerisTrackService.ts
    └── passScheduleService.ts
```

### 9.2 WebSocket 통신

```typescript
// icdService.ts
class WebSocketService {
  connect(url)           // 연결
  disconnect()           // 종료
  subscribe(key, handler) // 메시지 구독
  send(message)          // 메시지 전송
  reconnect()            // 재연결 (최대 5회)
}
```

### 9.3 WebSocket 메시지 구조
```typescript
interface MessageData {
  topic: string
  azimuthAngle: number
  elevationAngle: number
  trainAngle: number      // Tilt 각도
  cmdAzimuthAngle: number
  cmdElevationAngle: number
  cmdTrainAngle: number   // 명령 Tilt 각도
  serverTime: string
  trackingStatus: TrackingStatus
}
```

---

## 10. Composables

### 10.1 목록
```
frontend/src/composables/
├── useTheme.ts         # 테마 관리
├── useErrorHandler.ts  # 에러 처리
├── useNotification.ts  # 알림 (toast)
├── useLoading.ts       # 로딩 상태
├── useDialog.ts        # 다이얼로그
└── useValidation.ts    # 입력 검증
```

### 10.2 주요 Composables
```typescript
// useTheme
const { currentTheme, isDarkMode, setTheme } = useTheme()

// useNotification
const { success, error } = useNotification()
success('저장되었습니다')

// useErrorHandler
const { handleApiError } = useErrorHandler()
```

---

## 11. 데이터 흐름

### 11.1 실시간 데이터
```
WebSocket (Server, 30ms)
    ↓
icdService (WebSocketService)
    ↓
icdStore (useICDStore)
    ↓
Vue 컴포넌트 (computed 바인딩)
    ↓
UI 렌더링
```

### 11.2 명령 전송
```
모드 페이지 (Go 버튼)
    ↓
icdStore.slewCommand()
    ↓
icdService.sendCommand()
    ↓
WebSocket 전송
    ↓
Server → Firmware
```

### 11.3 설정 변경
```
SettingsModal
    ↓
개별 Settings 컴포넌트
    ↓
개별 스토어 (변경 추적)
    ↓
settingsService (API 호출)
    ↓
Backend API
```

---

## 12. 파일 통계

| 분류 | 개수 |
|-----|------|
| 컴포넌트 | ~35개 |
| 스토어 | ~25개 |
| 페이지 | 12개 |
| 레이아웃 | 2개 |
| 서비스 | 5개 |
| Composables | 8개 |

---

**문서 버전**: 1.0.0
**최종 업데이트**: 2024-12
