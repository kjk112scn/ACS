# ACS 리팩토링 실행 요약서

> **버전**: 1.1.0 | **최종 수정**: 2026-01-13
> **상태**: 진행 중 (Phase 0 완료)

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2026-01-13 | 1.1.0 | Phase 0 완료, Thread.sleep 우선순위 재평가, 모니터링 통합 추가 |
| 2026-01-13 | 1.0.0 | 초기 분석 완료, 작업 목록 수립 |

### 주요 의사결정 기록

#### Thread.sleep P0 → 제외 (2026-01-13)

**원래 판단**: 4곳의 Thread.sleep이 실시간 UDP 통신을 100사이클 블로킹 → P0 Critical

**실제 분석 결과**:
| 위치 | 분석 결과 | 조치 |
|------|----------|------|
| UdpFwICDService.kt (2곳) | `checkRealtimeCommunication()` 메서드 자체가 FE에서 미사용 | 메서드 삭제 |
| PassScheduleController.kt | `/tracking-monitor/restart` API가 FE에서 미사용 | API 삭제 |
| BatchStorageManager.kt | `@PreDestroy` 종료 시에만 실행, 실시간 영향 없음 | 유지 |

**결론**: Thread.sleep 자체가 문제가 아니라, 해당 코드가 죽은 코드였음. 삭제로 해결.

#### 모니터링 기능 통합 결정 (2026-01-13)

**문제**: 시스템 상태 확인 기능이 여러 서비스에 분산
- UdpFwICDService: `getCommunicationStatusReport()`, `getArchitectureInfo()`
- PushDataController: WebSocket 상태
- ThreadManager: 스레드 풀 상태
- PerformanceController: 메모리/성능

**검토한 방안**:
| 방안 | 설명 | 단점 |
|------|------|------|
| A. SystemHealthService 신설 | 모든 상태를 통합 관리 | 의존성 복잡, 장애 전파 위험 |
| B. PerformanceController 확장 | 기존 컨트롤러에 통합 API 추가 | - |
| C. 현상 유지 | 분산된 상태 유지 | 관리 어려움 |

**채택**: B+ (절충안) - PerformanceController에 경량 요약 API + 개별 상세 API
- `/health/summary`: 전체 상태 한눈에 조회
- `/thread-stats`: 스레드 풀 상세
- `/websocket-stats`: WebSocket 상세

---

## 완료된 작업

### Phase 0: 코드 정리 (2026-01-13)

| 작업 | 상태 | 삭제/추가 |
|------|------|----------|
| BE 죽은 코드 삭제 (UdpFwICDService) | **완료** | -3 메서드 |
| BE 미사용 API 삭제 (PassScheduleController) | **완료** | -8 엔드포인트 |
| FE 죽은 코드 삭제 (icdService.ts) | **완료** | -3 메서드 |
| 모니터링 통합 API 추가 (PerformanceController) | **완료** | +3 엔드포인트 |
| 빌드 검증 (FE + BE) | **완료** | BUILD SUCCESS |

**삭제된 BE 코드 상세**:

```
UdpFwICDService.kt:
  - getCommunicationStatusReport()  # 미사용
  - checkRealtimeCommunication()    # 미사용 + Thread.sleep 포함
  - getArchitectureInfo()           # 미사용

PassScheduleController.kt:
  - /tracking-monitor/restart       # 미사용
  - /tracking-monitor/current-schedule
  - /tracking/summary/{satelliteId}
  - /tracking/summary
  - /selected-tracking/master/{satelliteId}
  - /selected-tracking/master
  - /selected-tracking/current
  - /selected-tracking/next
```

**삭제된 FE 코드 상세**:

```
icdService.ts:
  - setPosition()                   # 미사용
  - getRealtimeData()               # 미사용
  - sendReadPositionerStatusCommand() # 미사용
```

**추가된 API**:

```
PerformanceController.kt:
  + GET /api/system/performance/health/summary   # 통합 헬스체크
  + GET /api/system/performance/thread-stats     # 스레드 풀 상태
  + GET /api/system/performance/websocket-stats  # WebSocket 상태
```

---

## 현재 우선순위

### P0: 즉시 수정 (Critical) - 재평가됨

| # | 작업 | 상태 | 비고 |
|---|------|------|------|
| ~~1~~ | ~~Thread.sleep 제거~~ | **제외** | 죽은 코드 삭제로 해결 |
| 2 | 핵심 알고리즘 테스트 | 미착수 | Keyhole Detection, 좌표 변환 |
| 3 | SatelliteTrackingEngine 추출 | 미착수 | RFC 문서 참조 |

### P1: 단기 개선 (High)

| # | 작업 | 파일 | 상태 |
|---|------|------|------|
| 4 | 하드코딩 색상 → 테마 변수 | DashboardPage.vue 외 | 미착수 |
| 5 | console.log 정리 | 46개 파일 (990개) | 미착수 |
| 6 | icdStore 구조화 | icdStore.ts (2,971줄) | 미착수 |
| 7 | API 응답 형식 표준화 | controller/ 전체 | 미착수 |

### P2: 중기 개선 (Medium)

| # | 작업 | 파일 | 상태 |
|---|------|------|------|
| 8 | 거대 Vue 파일 분리 | PassSchedulePage.vue | 미착수 |
| 9 | 거대 Vue 파일 분리 | EphemerisDesignationPage.vue | 미착수 |
| 10 | watch → computed 전환 | 21곳 | 미착수 |
| 11 | chart.js 의존성 제거 | package.json | 미착수 |

---

## 핵심 발견 사항 (갱신)

| 카테고리 | 발견 수 | 심각도 | 상태 |
|----------|---------|--------|------|
| ~~Thread.sleep 블로킹~~ | ~~4곳~~ | ~~P0~~ | **해결** (죽은 코드 삭제) |
| 테스트 커버리지 | BE 1.5%, FE 0% | **P0 Critical** | 미착수 |
| 상태 머신 중복 | 40% (2,000줄) | **P0 Critical** | 미착수 |
| 하드코딩 색상 | 400+ 곳 | P1 High | 미착수 |
| console.log | 990개 | P1 High | 미착수 |
| watch 과다 사용 | 61회 | P1 Medium | 미착수 |
| 거대 파일 | 5개 (4,000줄+) | P1 High | 미착수 |
| API 응답 형식 불일치 | 3가지 형식 | P1 Medium | 미착수 |
| 모니터링 기능 분산 | 4곳 | P1 Medium | **해결** (B+ 패턴) |

---

## 빌드 상태

| 영역 | 상태 | 최종 확인 |
|------|------|----------|
| Frontend | **성공** | 2026-01-13 |
| Backend | **성공** | 2026-01-13 |

---

## 아키텍처 이슈

### 상태 머신 중복 (P0) - 미착수

**문제**: EphemerisService와 PassScheduleService에서 40% 코드 중복

```kotlin
// 두 서비스에서 동일한 상태 머신 반복
enum class TrackingState { IDLE, PREPARING, WAITING, TRACKING, COMPLETED, ERROR }
enum class PreparingPhase { TRAIN_MOVING, TRAIN_STABILIZING, MOVING_TO_TARGET }
```

**해결 계획**: RFC_SatelliteTrackingEngine.md에 따라 공통 엔진 추출

### API 응답 형식 불일치 (P1) - 미착수

**현재 (3가지 형식 혼재):**
```typescript
// 형식 1: { status: "success" | "error", message: string }
// 형식 2: { success: true | false, message: string, data?: any }
// 형식 3: { totalCount: number, data: [...] }
```

**권장 (표준화):**
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 거대 파일 현황

### Backend

| 파일 | 현재 | 목표 | 우선순위 |
|------|------|------|----------|
| EphemerisService.kt | 5,057줄 | ~2,000줄 | P0 |
| PassScheduleService.kt | 3,847줄 | ~1,500줄 | P0 |
| ICDService.kt | 2,788줄 | ~2,000줄 | P1 |

### Frontend

| 파일 | 현재 | 목표 | 우선순위 |
|------|------|------|----------|
| PassSchedulePage.vue | 4,838줄 | ~3,500줄 | P2 |
| EphemerisDesignationPage.vue | 4,340줄 | ~3,000줄 | P2 |
| icdStore.ts | 2,971줄 | ~2,500줄 | P1 |

---

## 다음 단계 (Phase 1 권장)

### 옵션 A: 테스트 우선
- 핵심 알고리즘 테스트 작성 (Keyhole Detection, 좌표 변환)
- 리팩토링 안전망 확보 후 대규모 변경

### 옵션 B: 상태 머신 추출 우선
- SatelliteTrackingEngine 추출
- 40% 중복 제거로 가장 큰 ROI

### 옵션 C: 코드 품질 개선 우선
- console.log 정리 (990개)
- 하드코딩 색상 제거

---

## 관련 문서

| 문서 | 역할 |
|------|------|
| [RFC_SatelliteTrackingEngine.md](./RFC_SatelliteTrackingEngine.md) | 상태 머신 추출 상세 계획 |
| [Expert_Analysis_Report.md](./Expert_Analysis_Report.md) | 전문가 분석 보고서 |
| [Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md) | BE 파일 목록/현황 |
| [Frontend_Refactoring_plan.md](./Frontend_Refactoring_plan.md) | FE 파일 목록/현황 |

---

## ROI 분석 (갱신)

| 작업 | 작업량 | 효과 | ROI | 상태 |
|------|--------|------|-----|------|
| ~~Thread.sleep 제거~~ | ~~0.5일~~ | ~~실시간성~~ | ~~★★★★★~~ | **완료** |
| 죽은 코드 정리 | 0.5일 | 유지보수성 향상 | ★★★★ | **완료** |
| 모니터링 통합 | 0.5일 | 운영 효율성 | ★★★★ | **완료** |
| 핵심 알고리즘 테스트 | 1일 | 리팩토링 안전망 | ★★★★★ | 미착수 |
| SatelliteTrackingEngine | 2일 | 40% 중복 제거 | ★★★★★ | 미착수 |
| 하드코딩 색상 제거 | 1일 | 유지보수성 | ★★★ | 미착수 |

---

**작성자**: Claude
**분석 기준**: full-review, performance-analyzer, architect 에이전트 결과 + 실제 코드 분석
**현재 상태**: Phase 0 완료, Phase 1 방향 결정 필요
