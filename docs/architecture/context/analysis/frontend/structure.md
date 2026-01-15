# Frontend 코드 구조 분석

> 분석 일시: 2026-01-15
> 분석 대상: frontend/src/
> 총 파일: 93개 (43 .vue + 50 .ts)
> 총 코드: 30,000줄+

## 1. 폴더 구조 (전체 계층도)

```
frontend/src/
├── App.vue                          (14줄) 루트 컴포넌트
│
├── boot/                            (3개) Quasar 부트 파일
│   ├── axios.ts                     (92줄) Axios 인터셉터
│   ├── dark-mode.ts                 다크모드
│   └── i18n.ts                      다국어 초기화
│
├── components/                      (30개)
│   ├── common/
│   │   ├── EssentialLink.vue
│   │   ├── ExampleComponent.vue
│   │   └── models.ts
│   ├── content/
│   │   ├── AllStatusContent.vue     실시간 상태
│   │   ├── AxisTransformCalculator.vue 좌표 변환
│   │   ├── SelectScheduleContent.vue
│   │   ├── SystemInfoContent.vue
│   │   └── TLEUploadContent.vue
│   ├── Settings/
│   │   ├── SettingsModal.vue        (~150줄) 메인 설정
│   │   ├── GeneralSettings.vue
│   │   ├── LanguageSettings.vue
│   │   ├── VersionInfoSettings.vue
│   │   ├── admin/                   (5개)
│   │   │   ├── AdminSettings.vue
│   │   │   ├── MaintenanceSettings.vue
│   │   │   ├── MCOffSettings.vue
│   │   │   ├── ServoAlarmResetSettings.vue
│   │   │   └── ServoEncoderPresetSettings.vue
│   │   └── system/                  (10개)
│   │       ├── AlgorithmSettings.vue
│   │       ├── AngleLimitsSettings.vue
│   │       ├── AntennaSpecSettings.vue
│   │       ├── FeedSettings.vue
│   │       ├── LocationSettings.vue
│   │       ├── OffsetLimitsSettings.vue
│   │       ├── SpeedLimitsSettings.vue
│   │       ├── StepSizeLimitSettings.vue
│   │       ├── StowSettings.vue
│   │       ├── SystemSettings.vue
│   │       └── TrackingSettings.vue
│   ├── HardwareErrorLogPanel.vue
│   └── index.ts                     (18줄)
│
├── composables/                     (8개)
│   ├── useDialog.ts
│   ├── useErrorHandler.ts           (231줄) 에러 관리
│   ├── useI18n.ts
│   ├── useLoading.ts
│   ├── useNotification.ts           (116줄)
│   ├── useSharedStore.ts
│   ├── useTheme.ts
│   └── useValidation.ts             (242줄) 폼 검증
│
├── config/
│   └── components.ts
│
├── i18n/                            (3개)
│   ├── index.ts
│   ├── en-US/index.ts
│   └── ko-KR/index.ts
│
├── layouts/                         (2개)
│   ├── LoginLayout.vue
│   └── MainLayout.vue               (~180줄)
│
├── pages/                           (12개)
│   ├── DashboardPage.vue            (1000줄+) ⭐ 핵심
│   ├── LoginPage.vue                (~80줄)
│   ├── ErrorNotFound.vue
│   ├── mode/                        (8개)
│   │   ├── StandbyPage.vue
│   │   ├── StepPage.vue             (~300줄)
│   │   ├── SlewPage.vue             (~300줄)
│   │   ├── EphemerisDesignationPage.vue (800줄+)
│   │   ├── PassSchedulePage.vue     (800줄+)
│   │   ├── SunTrackPage.vue
│   │   ├── FeedPage.vue
│   │   └── PedestalPositionPage.vue
│   └── popup/
│       └── popupRouter.vue
│
├── router/                          (2개)
│   ├── index.ts
│   └── routes.ts                    (100줄)
│
├── services/                        (5개)
│   ├── index.ts                     (22줄)
│   ├── api/
│   │   ├── icdService.ts            (400줄+) ⭐ WebSocket
│   │   └── settingsService.ts
│   └── mode/
│       ├── ephemerisTrackService.ts
│       └── passScheduleService.ts
│
├── stores/                          (25개)
│   ├── index.ts                     (40줄)
│   ├── example-store.ts
│   ├── hardwareErrorLogStore.ts
│   ├── common/
│   │   ├── auth.ts                  (65줄)
│   │   └── modeStore.ts
│   ├── api/
│   │   ├── settingsStore.ts         (160줄)
│   │   └── settings/                (10개)
│   │       ├── algorithmSettingsStore.ts
│   │       ├── angleLimitsSettingsStore.ts
│   │       ├── antennaSpecSettingsStore.ts
│   │       ├── locationSettingsStore.ts
│   │       ├── offsetLimitsSettingsStore.ts
│   │       ├── speedLimitsSettingsStore.ts
│   │       ├── stepSizeLimitSettingsStore.ts
│   │       ├── stowSettingsStore.ts
│   │       ├── trackingSettingsStore.ts
│   │       └── settingsStore.ts
│   ├── icd/
│   │   ├── icdStore.ts              (400줄+) ⭐ 핵심
│   │   └── modeStore.ts
│   ├── mode/                        (6개)
│   │   ├── ephemerisTrackStore.ts
│   │   ├── passScheduleStore.ts     (200줄+)
│   │   ├── pedestalPositionStore.ts
│   │   ├── slewStore.ts
│   │   ├── standbyStore.ts
│   │   └── stepStore.ts
│   └── ui/
│       └── feedSettingsStore.ts
│
├── types/                           (4개)
│   ├── index.ts
│   ├── i18n.ts
│   ├── ephemerisTrack.ts
│   └── hardwareError.ts             (58줄)
│
├── utils/                           (5개)
│   ├── api-config.ts
│   ├── connectionManager.ts         (50줄)
│   ├── errorHandler.ts
│   ├── times.ts
│   └── windowUtils.ts
│
├── workers/
│   └── trackingPathWorker.ts        Web Worker
│
└── env.d.ts                         타입 정의
```

## 2. 영역별 상세

### 2.1 Pages (12개)

| 파일 | 줄 수 | 역할 | 복잡도 |
|-----|------|------|--------|
| **DashboardPage.vue** | **1000+** | 실시간 3축 모니터링, 차트 | 🔴 매우높음 |
| **PassSchedulePage.vue** | **800+** | 패스 스케줄 관리 | 🔴 매우높음 |
| **EphemerisDesignationPage.vue** | **800+** | 위성 지정 추적 | 🔴 매우높음 |
| StepPage.vue | 300+ | 스텝 이동 제어 | 🟠 높음 |
| SlewPage.vue | 300+ | 슬루 이동 제어 | 🟠 높음 |
| PedestalPositionPage.vue | 200+ | 페데스탈 위치 | 🟡 중간 |
| SunTrackPage.vue | 200+ | 태양 추적 | 🟡 중간 |
| StandbyPage.vue | 100 | 대기 모드 | 🟢 낮음 |
| LoginPage.vue | 80 | 로그인 | 🟢 낮음 |
| FeedPage.vue | - | 피드 제어 | 🟢 낮음 |
| ErrorNotFound.vue | 20 | 404 페이지 | 🟢 낮음 |
| popupRouter.vue | - | 팝업 라우터 | 🟢 낮음 |

### 2.2 Stores (25개)

#### 설정 스토어 (11개)
| 스토어 | 역할 | 연동 API |
|-------|------|---------|
| settingsStore.ts (api) | 통합 설정 로드/저장 | /api/settings/* |
| algorithmSettingsStore.ts | 알고리즘 설정 | 위성 추적 알고리즘 |
| angleLimitsSettingsStore.ts | 각도 제한값 | Az/El/Tilt 제한 |
| antennaSpecSettingsStore.ts | 안테나 사양 | 기계적 스펙 |
| locationSettingsStore.ts | 위치 설정 | 위도/경도/고도 |
| offsetLimitsSettingsStore.ts | 오프셋 제한값 | 추적 오프셋 |
| speedLimitsSettingsStore.ts | 속도 제한값 | 모터 속도 |
| stepSizeLimitSettingsStore.ts | 스텝 크기 | Step 모드 |
| stowSettingsStore.ts | Stow 설정 | 안전 위치 |
| trackingSettingsStore.ts | 추적 설정 | 추적 파라미터 |

#### 모드 스토어 (6개)
| 스토어 | 역할 | 연동 페이지 |
|-------|------|-----------|
| ephemerisTrackStore.ts | 위성 궤도 추적 | EphemerisDesignationPage |
| passScheduleStore.ts | 패스 스케줄 관리 | PassSchedulePage |
| pedestalPositionStore.ts | 페데스탈 위치 | PedestalPositionPage |
| slewStore.ts | 슬루 제어 | SlewPage |
| standbyStore.ts | 대기 모드 | StandbyPage |
| stepStore.ts | 스텝 이동 | StepPage |

#### ICD/공통 스토어 (8개)
| 스토어 | 줄 수 | 역할 |
|-------|------|------|
| **icdStore.ts** | **400+** | 실시간 WebSocket 데이터 (핵심) |
| icd/modeStore.ts | - | ICD 모드 정보 |
| auth.ts | 65 | 로그인/로그아웃 |
| common/modeStore.ts | - | 현재 모드 선택 |
| hardwareErrorLogStore.ts | - | 하드웨어 에러 |
| feedSettingsStore.ts | - | 피드 UI 상태 |

### 2.3 Components (30개)

#### Settings 컴포넌트 (18개)
```
SettingsModal.vue (~150줄) - 6탭 설정 모달
├── GeneralSettings.vue
├── LanguageSettings.vue
├── VersionInfoSettings.vue
├── admin/
│   ├── AdminSettings.vue
│   ├── MaintenanceSettings.vue
│   ├── MCOffSettings.vue
│   ├── ServoAlarmResetSettings.vue
│   └── ServoEncoderPresetSettings.vue
└── system/
    ├── AlgorithmSettings.vue
    ├── AngleLimitsSettings.vue
    ├── AntennaSpecSettings.vue
    ├── FeedSettings.vue
    ├── LocationSettings.vue
    ├── OffsetLimitsSettings.vue
    ├── SpeedLimitsSettings.vue
    ├── StepSizeLimitSettings.vue
    ├── StowSettings.vue
    └── TrackingSettings.vue
```

#### Content 컴포넌트 (5개)
| 컴포넌트 | 역할 |
|---------|------|
| AllStatusContent.vue | 모든 상태 표시 |
| AxisTransformCalculator.vue | 좌표 변환 계산기 |
| SelectScheduleContent.vue | 스케줄 선택 UI |
| SystemInfoContent.vue | 시스템 정보 |
| TLEUploadContent.vue | TLE 데이터 업로드 |

### 2.4 Composables (8개)

| 이름 | 줄 수 | 역할 | 사용처 |
|-----|------|------|--------|
| useErrorHandler.ts | 231 | 에러 수집/알림 | 모든 API 호출 |
| useValidation.ts | 242 | 폼 검증 | 입력 폼 |
| useNotification.ts | 116 | Quasar 알림 래퍼 | 사용자 피드백 |
| useDialog.ts | - | 다이얼로그 관리 | 확인/취소 |
| useI18n.ts | - | 다국어 함수 | 텍스트 표시 |
| useLoading.ts | - | 로딩 상태 | 비동기 작업 |
| useSharedStore.ts | - | 공유 스토어 접근 | 스토어 연동 |
| useTheme.ts | - | 다크모드 관리 | UI 테마 |

### 2.5 Services (5개)

| 서비스 | 줄 수 | 역할 | 통신 방식 |
|-------|------|------|----------|
| **icdService.ts** | **400+** | 실시간 통신 | WebSocket + REST |
| settingsService.ts | 300+ | 설정 API | REST |
| ephemerisTrackService.ts | 200+ | 위성 추적 | REST |
| passScheduleService.ts | 300+ | 패스 스케줄 | REST |
| index.ts | 22 | Export | - |

## 3. 핵심 파일 (Phase 2 심층 분석 대상)

### 3.1 대형 파일 (300줄+)

| 순위 | 파일 | 줄 수 | 분류 | 우선순위 |
|-----|-----|------|------|---------|
| 1 | DashboardPage.vue | 1000+ | Page | P1 |
| 2 | PassSchedulePage.vue | 800+ | Page | P1 |
| 3 | EphemerisDesignationPage.vue | 800+ | Page | P1 |
| 4 | icdStore.ts | 400+ | Store | P1 |
| 5 | icdService.ts | 400+ | Service | P1 |
| 6 | settingsService.ts | 300+ | Service | P2 |
| 7 | passScheduleService.ts | 300+ | Service | P2 |
| 8 | StepPage.vue | 300+ | Page | P2 |
| 9 | SlewPage.vue | 300+ | Page | P2 |
| 10 | useValidation.ts | 242 | Composable | P3 |
| 11 | useErrorHandler.ts | 231 | Composable | P3 |
| 12 | passScheduleStore.ts | 200+ | Store | P2 |

### 3.2 핵심 데이터 흐름

```
WebSocket (ICD)
    ↓
icdService.ts (subscribe/connect)
    ↓
icdStore.ts (상태 저장)
    ↓
DashboardPage.vue (차트 렌더링)
```

```
사용자 입력 (Mode Pages)
    ↓
*Store.ts (상태 관리)
    ↓
*Service.ts (API 호출)
    ↓
Backend REST API
```

## 4. 설정 파일 및 의존성

### package.json 주요 의존성

| 패키지 | 버전 | 용도 |
|-------|------|------|
| vue | 3.4.18 | 프레임워크 |
| quasar | 2.16.0 | UI 컴포넌트 |
| pinia | 3.0.2 | 상태 관리 |
| typescript | 5.5.3 | 타입 시스템 |
| vue-router | 4.0.12 | 라우팅 |
| axios | 1.9.0 | HTTP 클라이언트 |
| vue-i18n | 9.2.2 | 다국어 |
| echarts | 5.6.0 | 차트 |
| vue-chartjs | 5.3.2 | Chart.js 래퍼 |

### tsconfig.json 경로 별칭

```json
{
  "paths": {
    "@/*": ["src/*"],
    "@/types/*": ["src/types/*"],
    "@/services/*": ["src/services/*"],
    "@/stores/*": ["src/stores/*"],
    "@/components/*": ["src/components/*"],
    "@/composables/*": ["src/composables/*"]
  }
}
```

## 5. 아키텍처 특징

### 계층 구조
```
Pages (라우팅)
    ↓
Layouts (레이아웃)
    ↓
Components (UI)
    ↓
Stores (상태) ↔ Services (API)
    ↓
Composables (재사용 로직)
    ↓
Utils (유틸리티)
```

### 주요 패턴

1. **Setup Store 패턴** (Pinia)
```typescript
defineStore('storeName', () => {
  const state = ref(...)
  const actions = () => { ... }
  return { state, actions }
})
```

2. **Composable 패턴**
```typescript
export const useErrorHandler = () => {
  const handleError = (error) => { ... }
  return { handleError }
}
```

3. **WebSocket 싱글톤**
```typescript
// icdService.ts
let instance: WebSocket | null = null
export const connect = () => { ... }
```

### 상태 관리 흐름
```
Component → Store Action → Service API → Backend
                ↑
         Store State ← WebSocket
```

## 6. 특이사항 및 발견점

### 긍정적
1. **명확한 폴더 구조** - 기능별 분리 우수
2. **타입 안전성** - TypeScript 일관 적용
3. **Setup Store 패턴** - Pinia 모범 사례
4. **Composable 활용** - 재사용성 높음
5. **다국어 지원** - i18n 한/영

### 개선 필요
1. **DashboardPage 과대** (1000줄+) → 컴포넌트 분리 필요
2. **중복 modeStore** - common/, icd/ 2개 존재 → 통합 검토
3. **설정 스토어 11개** - 공통 패턴 추출 가능
4. **대형 페이지들** - 800줄+ 페이지 분할 권장

### 주의사항
- **Train vs Tilt**: 변수명 `train`, UI 표시 `Tilt`
- **각도 단위**: API 라디안, UI 도(°)
- **시간대**: API UTC, UI 로컬

---

**다음**: Phase 2에서 핵심 스토어/페이지 심층 분석
