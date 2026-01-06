# ACS 코드 구조 참조

## Backend 구조

```
backend/src/main/kotlin/com/gtlsystems/acs_api/
├── algorithm/                    # 알고리즘 계산
│   ├── axislimitangle/           # 한계각 계산
│   │   └── LimitAngleCalculator.kt
│   ├── axistransformation/       # 좌표 변환
│   │   └── CoordinateTransformer.kt
│   ├── elevation/                # 고도각 계산
│   │   └── ElevationCalculator.kt
│   ├── satellitetracker/         # 위성 추적
│   │   ├── impl/
│   │   │   └── OrekitCalculator.kt
│   │   ├── model/
│   │   └── processor/
│   │       └── SatelliteTrackingProcessor.kt
│   └── suntrack/                 # 태양 추적
│       ├── impl/
│       │   ├── Grena3Calculator.kt
│       │   ├── SPACalculator.kt
│       │   └── SolarOrekitCalculator.kt
│       └── interfaces/
│
├── config/                       # 설정
│   ├── CorsConfig.kt
│   ├── OrekitConfig.kt
│   ├── ThreadManager.kt
│   └── WebSocketConfig.kt
│
├── controller/                   # REST API
│   ├── icd/
│   │   └── ICDController.kt
│   ├── mode/
│   │   ├── EphemerisController.kt
│   │   ├── PassScheduleController.kt
│   │   └── SunTrackController.kt
│   ├── system/
│   │   ├── HardwareErrorLogController.kt
│   │   ├── LoggingController.kt
│   │   ├── PerformanceController.kt
│   │   └── settings/
│   │       └── SettingsController.kt
│   └── websocket/
│       └── PushDataController.kt
│
├── dto/                          # 데이터 전송 객체
│   ├── request/
│   └── response/
│
├── event/                        # 이벤트 시스템
│   ├── ACSEvent.kt
│   └── ACSEventBus.kt
│
├── model/                        # 도메인 모델
│   ├── GlobalData.kt
│   ├── PushData.kt
│   └── SystemInfo.kt
│
└── service/                      # 비즈니스 로직
    ├── datastore/
    │   └── DataStoreService.kt
    ├── hardware/
    │   ├── ErrorMessageConfig.kt
    │   └── HardwareErrorLogService.kt
    ├── icd/
    │   └── ICDService.kt
    ├── mode/
    │   ├── EphemerisService.kt
    │   ├── PassScheduleService.kt
    │   └── SunTrackService.kt
    ├── system/
    │   ├── BatchStorageManager.kt
    │   ├── LoggingService.kt
    │   └── settings/
    │       ├── InitService.kt
    │       └── SettingsService.kt
    └── udp/
        ├── PushDataService.kt
        └── UdpFwICDService.kt
```

## Frontend 구조

```
frontend/src/
├── components/                   # Vue 컴포넌트
│   ├── common/                   # 공통 컴포넌트
│   ├── content/                  # 콘텐츠 컴포넌트
│   │   ├── AllStatusContent.vue
│   │   ├── SelectScheduleContent.vue
│   │   ├── SystemInfoContent.vue
│   │   └── TLEUploadContent.vue
│   ├── Settings/                 # 설정 컴포넌트
│   └── HardwareErrorLogPanel.vue
│
├── composables/                  # Vue Composition 함수
│   ├── useErrorHandler.ts
│   ├── useLoading.ts
│   └── useNotification.ts
│
├── pages/                        # 페이지
│   └── mode/                     # 모드별 페이지
│       ├── DashboardPage.vue
│       ├── EphemerisDesignationPage.vue
│       ├── FeedPage.vue
│       ├── PassSchedulePage.vue
│       └── SunTrackPage.vue
│
├── services/                     # API 서비스
│   ├── ephemerisTrackService.ts
│   ├── icdService.ts
│   └── passScheduleService.ts
│
├── stores/                       # Pinia 스토어
│   ├── api/
│   │   └── settingsStore.ts
│   ├── common/
│   │   └── modeStore.ts
│   ├── icd/
│   │   └── icdStore.ts
│   ├── ephemerisTrackStore.ts
│   └── passScheduleStore.ts
│
└── types/                        # TypeScript 타입
    └── *.ts
```

## 문서 구조

```
docs/
├── features/                     # 기능 개발 문서
│   ├── active/                   # 진행 중
│   │   ├── Architecture_Refactoring/
│   │   ├── PassSchedule_Chart_Optimization/
│   │   └── README.md
│   └── completed/                # 완료
│       ├── Train_Algorithm/
│       ├── Keyhole_Train_Angle_Management/
│       └── ...
│
└── references/                   # 참조 문서
    ├── architecture/             # 아키텍처 설계
    │   ├── SYSTEM_OVERVIEW.md
    │   └── UI_Architecture.md
    ├── algorithms/               # 알고리즘 설계
    │   ├── Train_Angle_Algorithm.md
    │   └── Antenna_Structure_And_Train_Angle_Concept.md
    ├── api/                      # API 명세
    │   └── README.md
    ├── development/              # 개발 가이드
    │   ├── Development_Guide.md
    │   ├── Coding_Standards.md
    │   └── Keyhole_Processing_Logic.md
    ├── AGENT.md                  # 에이전트 정의
    ├── PROJECT_STATUS_SUMMARY.md # 과거 현황 (2024-12)
    ├── PROJECT_STATUS_CURRENT.md # 현재 현황 (최신)
    └── Hardware_Error_Messages.md
```

## 핵심 파일 통계

### 대형 파일 (1000줄+)

| 영역 | 파일 | 줄 수 | 문서화 상태 |
|------|------|-------|------------|
| **Backend** | | | |
| Service | EphemerisService.kt | 4,986 | 필요 |
| Service | PassScheduleService.kt | 2,896 | 필요 |
| Service | ICDService.kt | 2,788 | 필요 |
| Service | UdpFwICDService.kt | 1,294 | 필요 |
| Algorithm | SatelliteTrackingProcessor.kt | 1,387 | 필요 |
| Controller | PassScheduleController.kt | 2,021 | 필요 |
| Controller | EphemerisController.kt | 1,091 | 필요 |
| **Frontend** | | | |
| Page | PassSchedulePage.vue | 4,841 | 필요 |
| Page | EphemerisDesignationPage.vue | 4,376 | 필요 |
| Store | icdStore.ts | 2,971 | 필요 |
| Store | passScheduleStore.ts | 2,452 | 필요 |
| Component | AllStatusContent.vue | 2,381 | 필요 |

### 파일 수 통계

| 영역 | 수량 |
|------|------|
| **Backend** | |
| Controller | 9개 |
| Service | 13개 |
| Algorithm | 7개 Calculator |
| Config | 7개 |
| **Frontend** | |
| Pages | 5개 주요 페이지 |
| Stores | 25개 |
| Components | 43개 |
| Composables | 3개+ |

## 6개 모드 시스템

| 모드 | 설명 | 주요 파일 |
|------|------|----------|
| Standby | 대기 모드 | - |
| Step | 스텝 이동 | StepPage.vue |
| Slew | 슬루 이동 | SlewPage.vue |
| EphemerisDesignation | 위성 궤도 지정 | EphemerisDesignationPage.vue, EphemerisService.kt |
| PassSchedule | 패스 스케줄 | PassSchedulePage.vue, PassScheduleService.kt |
| SunTrack | 태양 추적 | SunTrackPage.vue, SunTrackService.kt |

## 핵심 의존성

```
EphemerisService
├── OrekitCalculator (위성 위치 계산)
├── SatelliteTrackingProcessor (데이터 처리)
├── CoordinateTransformer (좌표 변환)
├── LimitAngleCalculator (한계각 검증)
├── ICDService (ICD 통신)
├── UdpFwICDService (UDP 통신)
├── DataStoreService (데이터 저장)
├── ThreadManager (쓰레드 관리)
├── BatchStorageManager (배치 저장)
└── SettingsService (설정 관리)
```

## 기술 스택

| 영역 | 기술 | 버전 |
|------|------|------|
| Backend | Kotlin | 1.9 |
| Backend | Spring Boot | 3.x |
| Backend | WebFlux | - |
| Backend | Orekit | 13.0.2 |
| Frontend | Vue | 3 |
| Frontend | Quasar | 2.x |
| Frontend | TypeScript | 5.x |
| Frontend | Pinia | - |
