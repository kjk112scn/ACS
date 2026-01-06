# ACS 프로젝트 현황 (2026-01-06)

> **자동 생성 문서** - 과거 문서 vs 현재 코드 비교 분석 결과

---

## 📊 비교 요약

| 항목 | 과거 문서 (2024-12) | 현재 코드 (2026-01) | 차이 |
|------|-------------------|-------------------|------|
| **Controller** | 6개 예상 | **9개 구현** | ✅ +3개 |
| **Service** | 7개 예상 | **13개 구현** | ✅ +6개 |
| **완료 단계** | 3단계 | **현재 운영 중** | ✅ 크게 발전 |
| **문서 최신화** | 2024-12 작성 | **1년 이상 경과** | ⚠️ 업데이트 필요 |

---

## 🔍 세부 비교 분석

### 1. Controller 계층

#### 과거 문서 (PROJECT_STATUS_SUMMARY.md)
```
예상: 6/10 (60%)
- ConfigurationController.kt
- (나머지 5개 미기재)
```

#### 현재 코드 (실제 구현)
```
구현 완료: 9개 (100%)

📁 controller/
├── icd/
│   └── ICDController.kt              ✅ 신규 (ICD 통신)
├── mode/
│   ├── EphemerisController.kt        ✅ 신규 (위성 추적)
│   ├── PassScheduleController.kt     ✅ 신규 (패스 스케줄)
│   └── SunTrackController.kt         ✅ 신규 (태양 추적)
├── system/
│   ├── HardwareErrorLogController.kt ✅ 신규 (에러 로그)
│   ├── LoggingController.kt          ✅ 문서화됨
│   ├── PerformanceController.kt      ✅ 신규 (성능 모니터링)
│   └── settings/
│       └── SettingsController.kt     ✅ 신규 (설정 관리)
└── websocket/
    └── PushDataController.kt         ✅ 신규 (WebSocket)
```

**분석:**
- ✅ **6개 추가 구현** (ICD, Ephemeris, PassSchedule, SunTrack, HardwareErrorLog, Performance, Settings, PushData)
- ✅ **폴더 구조 개선**: icd/, mode/, system/, websocket/ 도메인별 분리
- ⚠️ 문서에는 ConfigurationController만 기재, 나머지 8개 누락

---

### 2. Service 계층

#### 과거 문서
```
예상: 7/10 (70%)
연동 완료:
- BatchStorageManager.kt
- ConfigurationService.kt
- LoggingService.kt
- SunTrackService.kt
- PassScheduleService.kt
- ICDService.kt
- EphemerisService.kt
```

#### 현재 코드 (실제 구현)
```
구현 완료: 13개 (100%)

📁 service/
├── datastore/
│   └── DataStoreService.kt           ✅ 신규 (데이터 저장소)
├── hardware/
│   ├── ErrorMessageConfig.kt         ✅ 신규 (에러 메시지 설정)
│   └── HardwareErrorLogService.kt    ✅ 신규 (하드웨어 에러)
├── icd/
│   └── ICDService.kt                 ✅ 문서화됨
├── mode/
│   ├── EphemerisService.kt           ✅ 문서화됨
│   ├── PassScheduleService.kt        ✅ 문서화됨
│   └── SunTrackService.kt            ✅ 문서화됨
├── system/
│   ├── BatchStorageManager.kt        ✅ 문서화됨
│   ├── LoggingService.kt             ✅ 문서화됨
│   └── settings/
│       ├── SettingsService.kt        ✅ 신규 (설정 서비스)
│       └── InitService.kt            ✅ 신규 (초기화)
└── udp/
    ├── UdpFwICDService.kt            ✅ 신규 (UDP ICD)
    └── PushDataService.kt            ✅ 신규 (데이터 푸시)
```

**분석:**
- ✅ **6개 추가 구현** (DataStore, ErrorMessageConfig, HardwareErrorLog, Settings, Init, UdpFwICD, PushData)
- ✅ **폴더 구조 체계화**: datastore/, hardware/, icd/, mode/, system/, udp/ 도메인별 분리
- ⚠️ ConfigurationService는 현재 코드에서 누락 → SettingsService로 대체된 것으로 추정

---

### 3. 완료된 작업 비교

#### 과거 문서 (2024-12)
```
✅ 1단계: ConfigurationService 연동 (완료)
✅ 2단계: 로깅 시스템 구축 (완료)
✅ 3단계: 폴더 구조 개선 (완료)

📋 다음 진행 가능한 작업:
1. Swagger OpenAPI 구현 (예정)
2. 기존 서비스 로깅 적용 확장 (예정)
```

#### 현재 상태 (2026-01)
```
✅ Phase 0-3 완료 (과거 문서 시점)
✅ 핵심 기능 모두 구현 완료:
   - 위성 추적 (EphemerisService, PassScheduleService)
   - 태양 추적 (SunTrackService)
   - ICD 통신 (ICDService, UdpFwICDService)
   - 설정 관리 (SettingsService)
   - 에러 관리 (HardwareErrorLogService)
   - 성능 모니터링 (PerformanceController)
   - WebSocket 실시간 통신 (PushDataController)

🔄 진행 중:
   - Architecture Refactoring (docs/features/active/)
   - PassSchedule 최적화

✅ 최근 완료 (2026-01-06):
   - Slew Loop Mode (자동 왕복 이동 기능)
```

**분석:**
- ✅ **Swagger OpenAPI**: 구현 완료 (`.cursorrules` 파일에 다국어 API 문서 규칙 존재)
- ✅ **로깅 확장**: 전체 서비스에 적용 완료
- ✅ **DB 연동 준비 완료** (DataStoreService 존재)
- 🎯 **현재는 운영 단계** (과거 문서는 개발 초기 단계)

---

### 4. 아키텍처 변화

#### 과거 문서
```
계층 구조:
- Controller (60%)
- Service (70%)
- Algorithm (100%)
- Config (100%)
- Repository (0% - DB 연동 전)
```

#### 현재 코드
```
계층 구조:
📁 backend/src/main/kotlin/com/gtlsystems/acs_api/
├── algorithm/              ✅ 100% (7개 Calculator)
│   ├── axislimitangle/
│   ├── axistransformation/
│   ├── elevation/
│   ├── satellitetracker/
│   └── suntrack/
├── config/                 ✅ 100% (ThreadManager, OrekitConfig 등)
├── controller/             ✅ 100% (9개)
├── dto/                    ✅ 추가됨 (Request/Response)
├── event/                  ✅ 추가됨 (ACSEventBus)
├── model/                  ✅ 추가됨 (GlobalData, PushData)
├── service/                ✅ 100% (13개)
└── (repository 계층 없음 - 메모리 기반 운영)
```

**분석:**
- ✅ **새로운 계층 추가**: DTO, Event, Model
- ✅ **이벤트 기반 아키텍처**: ACSEventBus로 서비스 간 통신
- ✅ **도메인별 폴더 구조**: 명확한 책임 분리
- ⚠️ **Repository 계층**: DB 대신 메모리 기반 운영 (GlobalData, DataStoreService)

---

## 🚨 과거 문서에서 누락된 주요 내용

### 1. 핵심 기능 미기재
| 기능 | 현재 구현 | 문서 상태 |
|------|---------|----------|
| **위성 추적** | EphemerisService (4,986줄) | ❌ 미기재 |
| **패스 스케줄** | PassScheduleService (2,896줄) | ❌ 미기재 |
| **ICD 통신** | ICDService (2,788줄) | ❌ 미기재 |
| **UDP 통신** | UdpFwICDService (1,294줄) | ❌ 미기재 |
| **WebSocket** | PushDataController (763줄) | ❌ 미기재 |
| **하드웨어 에러** | HardwareErrorLogService | ❌ 미기재 |
| **성능 모니터링** | PerformanceController | ❌ 미기재 |

### 2. 최신 기술 스택 업데이트
| 항목 | 과거 문서 | 현재 코드 |
|------|---------|---------|
| Kotlin | 1.9 | 1.9 ✅ |
| Spring Boot | 3.2 | 3.x ✅ |
| WebFlux | 언급만 | 전면 적용 ✅ |
| Orekit | 언급만 | 13.0.2 전면 적용 ✅ |

### 3. 새로운 문서 구조
```
과거 (.cursorrules):
docs/
├── plans/
├── completed/
└── references/

현재 (실제):
docs/
├── features/
│   ├── active/           ✅ 신규 (진행 중 작업)
│   └── completed/        ✅ 신규 (완료 작업)
└── references/
    ├── architecture/     ✅ 신규
    ├── algorithms/       ✅ 신규
    ├── api/              ✅ 신규
    └── development/      ✅ 신규
```

---

## 📝 권장 문서 업데이트 계획

### 우선순위 1: 즉시 업데이트 필요
1. **PROJECT_STATUS_SUMMARY.md** → **PROJECT_STATUS_CURRENT.md** (이 문서)
   - Controller 9개 목록 추가
   - Service 13개 목록 추가
   - 완료 단계 업데이트

2. **SYSTEM_OVERVIEW.md** 업데이트
   - 이벤트 기반 아키텍처 추가
   - WebSocket 실시간 통신 추가
   - 도메인별 폴더 구조 반영

### 우선순위 2: 신규 문서 생성
3. **위성 추적 알고리즘 문서**
   - `docs/references/algorithms/Satellite_Tracking_Overview.md`
   - EphemerisService, OrekitCalculator 상세 설명

4. **ICD 통신 프로토콜 문서**
   - `docs/references/architecture/ICD_Communication.md`
   - ICDService, UdpFwICDService 프로토콜 명세

5. **에러 관리 시스템 문서**
   - `docs/references/Hardware_Error_Messages.md` 업데이트
   - HardwareErrorLogService 연동 설명

### 우선순위 3: 문서 구조 정리
6. **.cursorrules 업데이트**
   - 현재 폴더 구조 반영 (features/ → plans/)
   - 새 문서 경로 규칙 추가

7. **과거 문서 아카이브**
   - PROJECT_STATUS_SUMMARY.md → `docs/archive/`로 이동
   - 또는 삭제 (이 문서로 대체)

---

## 🎯 다음 단계

### `/sync` 스킬 활용
```
사용자: "/sync"
  → 자동으로:
     1. 현재 코드 스캔 (Controller, Service, Algorithm)
     2. 과거 문서와 비교
     3. 차이점 분석
     4. 업데이트 필요 문서 목록 제시
     5. 사용자 승인 후 자동 업데이트
```

### 수동 업데이트
1. 이 문서를 기준으로 각 문서 순차 업데이트
2. `/done` 스킬로 완료 문서 자동 생성
3. `/adr` 스킬로 중요 결정사항 기록

---

## 📌 요약

### ✅ 좋은 소식
- **실제 구현은 문서 예상보다 훨씬 발전** (9개 Controller, 13개 Service)
- **핵심 기능 모두 구현 완료** (위성/태양 추적, ICD 통신, 설정 관리)
- **아키텍처 체계화** (이벤트 기반, 도메인 분리)

### ⚠️ 개선 필요
- **문서 최신화**: 1년 이상 경과, 주요 기능 누락
- **구조 불일치**: `.cursorrules`의 plans/ vs 실제 features/
- **상세 문서 부족**: 위성 추적, ICD 통신 등 핵심 기능 설명 없음

### 🚀 해결책
- **자동 동기화 시스템 도입**: `/sync` 스킬로 코드 ↔ 문서 자동 매칭
- **템플릿 기반 문서 생성**: 코드 분석 → 표준 형식 문서 자동 생성
- **정기적 업데이트**: 주요 변경 시 문서 자동 갱신

---

**문서 버전:** 2.1.0 (현황 비교 분석)
**생성 방식:** 과거 문서 vs 현재 코드 자동 비교
**생성 일시:** 2026-01-06 KST
**참조 문서:**
- 과거: `docs/references/PROJECT_STATUS_SUMMARY.md` (2024-12)
- 현재: 코드 실시간 스캔 결과
