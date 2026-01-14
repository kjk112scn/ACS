# 파일 구조 (File Structure)

> ACS 프로젝트의 현재 파일 구조 (자동 업데이트 대상)

<!-- AUTO-GENERATED: START -->
## 전체 구조

```
ACS/
├── frontend/              # Vue 3 + Quasar 프론트엔드
├── backend/               # Kotlin + Spring Boot 백엔드
├── docs/                  # 프로젝트 문서
└── .claude/               # Claude 설정
    ├── agents/            # 서브 에이전트
    ├── skills/            # 스킬 (슬래시 명령어)
    └── context/           # 컨텍스트 참조 문서
```

## Frontend 구조

```
frontend/src/
├── components/            # Vue 컴포넌트
│   ├── common/            # 공통 컴포넌트
│   │   ├── AppHeader.vue
│   │   ├── AppSidebar.vue
│   │   └── ErrorBoundary.vue
│   ├── chart/             # 차트 컴포넌트
│   │   ├── PassChart.vue
│   │   └── TrackingChart.vue
│   └── antenna/           # 안테나 관련
│       ├── AntennaStatus.vue
│       └── AntennaControl.vue
├── pages/                 # 페이지
│   ├── IndexPage.vue
│   └── mode/              # 모드별 페이지
│       ├── StepPage.vue
│       ├── SlewPage.vue
│       ├── EphemerisDesignationPage.vue   # 4,340줄
│       ├── PassSchedulePage.vue           # 4,838줄
│       └── SunTrackPage.vue
├── stores/                # Pinia 스토어
│   ├── icdStore.ts        # 2,971줄 (ICD 상태)
│   ├── modeStore.ts
│   ├── ephemerisStore.ts
│   └── passScheduleStore.ts
├── services/              # API 서비스
│   ├── api.ts
│   ├── ephemerisService.ts
│   └── passScheduleService.ts
├── composables/           # Composition 함수
│   ├── useErrorHandler.ts
│   ├── useNotification.ts
│   └── useLoading.ts
├── types/                 # TypeScript 타입
│   ├── api.d.ts
│   └── icd.d.ts
└── utils/                 # 유틸리티
    ├── conversion.ts
    └── format.ts
```

## Backend 구조

```
backend/src/main/kotlin/.../
├── controller/            # REST API
│   ├── EphemerisController.kt
│   ├── PassScheduleController.kt
│   ├── StepController.kt
│   ├── SlewController.kt
│   ├── SunTrackController.kt
│   └── TrackingController.kt
├── service/               # 비즈니스 로직
│   ├── EphemerisService.kt    # 4,986줄
│   ├── ICDService.kt          # 2,788줄
│   ├── PassScheduleService.kt
│   ├── TrackingService.kt
│   └── SunTrackService.kt
├── algorithm/             # 계산 알고리즘
│   ├── ephemeris/
│   │   ├── OrekitCalculator.kt
│   │   ├── TLEParser.kt
│   │   └── PassPredictor.kt
│   ├── icd/
│   │   ├── ICDParser.kt
│   │   └── ICDBuilder.kt
│   └── coordinate/
│       └── CoordinateConverter.kt
├── dto/                   # DTO
│   ├── request/
│   └── response/
├── model/                 # 도메인 모델
│   ├── TLE.kt
│   ├── Pass.kt
│   └── AntennaState.kt
├── config/                # 설정
│   ├── WebSocketConfig.kt
│   ├── OrekitConfig.kt
│   └── CorsConfig.kt
└── exception/             # 예외
    └── GlobalExceptionHandler.kt
```

## 문서 구조

```
docs/
├── references/            # 참조 문서
│   ├── architecture/      # 아키텍처
│   │   └── SYSTEM_OVERVIEW.md
│   ├── api/               # API 명세
│   ├── algorithms/        # 알고리즘
│   └── development/       # 개발 가이드
├── features/              # 기능 문서
│   ├── active/            # 진행 중
│   └── completed/         # 완료
├── decisions/             # ADR
└── daily/                 # 일일 로그
```
<!-- AUTO-GENERATED: END -->

## 대형 파일 목록

### 리팩토링 대상 (Frontend)

| 파일 | 줄 수 | 분리 방향 |
|------|------|----------|
| PassSchedulePage.vue | 4,838 | Table, Chart, Controls |
| EphemerisDesignationPage.vue | 4,340 | PositionView, Info, Selector |
| icdStore.ts | 2,971 | WebSocket, Parser, State |

### 리팩토링 대상 (Backend)

| 파일 | 줄 수 | 분리 방향 |
|------|------|----------|
| EphemerisService.kt | 4,986 | Calculator, Loader, Converter |
| ICDService.kt | 2,788 | Parser, Sender, Handler |

## 파일 통계

<!-- AUTO-GENERATED: START -->
### Frontend
- Vue 파일: ~43개
- TypeScript 파일: ~25개
- 총 줄 수: ~45,000줄

### Backend
- Kotlin 파일: ~60개
- 총 줄 수: ~35,000줄
<!-- AUTO-GENERATED: END -->

## 참조

- [핵심 컴포넌트](key-components.md)
- [FE 아키텍처](../architecture/frontend.md)
- [BE 아키텍처](../architecture/backend.md)

---

**최종 수정:** 2026-01-14
**업데이트:** /sync 자동
