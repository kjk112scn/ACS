# Refactoring Tracker (검증 완료)

> **Last Updated**: 2026-01-15
> **Status**: Sprint 0 Ready
> **Target**: 1/15~21 (전체 리팩토링 완료)

---

## Progress Overview

| Phase | Description | Status | Items | Progress |
|-------|-------------|--------|-------|----------|
| Sprint 0 | 보안 Critical | **Ready** | 3건 | 0/3 |
| Phase 1 | BE 안정성 | Pending | 6 tasks | 0% |
| Phase 2 | FE 성능 | Pending | 3 tasks | 0% |
| Phase 4 | 품질 개선 | Pending | 2 tasks | 0% |
| Phase 3 | FE 파일 분리 | Pending | 3 tasks | 0% |
| Phase 5 | 키보드 단축키 | Pending | 2 tasks | 0% |
| Phase 3 | BE 서비스 분리 | 🎯 1/20~21 | 1 task | 0% |
| 장기 | 테스트/인증/Docker | 📅 개발 완료 후 | 4 tasks | - |

---

## Sprint 0: 보안 Critical (2시간)

### S0-1. Path Traversal 수정

| Item | File | Line | Status |
|------|------|------|--------|
| [ ] | LoggingController.kt | 172-173 | Not Started |

**수정 내용**: 파일명 검증 추가
```kotlin
val normalizedPath = Paths.get(LOGS_DIRECTORY, fileName).normalize()
if (!normalizedPath.startsWith(Paths.get(LOGS_DIRECTORY).normalize())) {
    throw IllegalArgumentException("Invalid file path")
}
```

---

### S0-2. CORS Wildcard 제거

| Item | File | Line | Status |
|------|------|------|--------|
| [ ] | CorsConfig.kt | 26 | Not Started |

**수정 내용**: `"*"` 제거

---

### S0-3. innerHTML XSS 수정

| Item | File | Line | Status |
|------|------|------|--------|
| [ ] | windowUtils.ts | 709 | Not Started |
| [ ] | windowUtils.ts | 821 | Not Started |
| [ ] | windowUtils.ts | 847 | Not Started |
| [ ] | windowUtils.ts | 869 | Not Started |

**수정 내용**: `innerHTML` → `textContent` 또는 DOM API

---

## Phase 1: BE 안정성 (8-12시간)

### P1-1. !! 연산자 제거 (7건)

| Item | File | Line | Code | Status |
|------|------|------|------|--------|
| [ ] | SunTrackService.kt | 103 | `modeTask!!.isCancelled` | Not Started |
| [ ] | SunTrackService.kt | 424 | `getTrainOffsetCalculator()!!` | Not Started |
| [ ] | SunTrackService.kt | 462 | `getTrainOffsetCalculator()!!` | Not Started |
| [ ] | PassScheduleService.kt | 719 | `preparingPassId!!` | Not Started |
| [ ] | PassScheduleService.kt | 923 | `lastDisplayedSchedule!!` | Not Started |
| [ ] | PassScheduleService.kt | 929 | `lastDisplayedSchedule!!` | Not Started |
| [ ] | PassScheduleService.kt | 936-937 | `lastDisplayedSchedule!!` | Not Started |
| [ ] | EphemerisService.kt | 1113 | `modeTask!!.isCancelled` | Not Started |
| [ ] | EphemerisService.kt | 2717 | `currentTrackingPass!!` | Not Started |
| [ ] | EphemerisService.kt | 2718 | `currentTrackingPass!!` | Not Started |
| [ ] | EphemerisService.kt | 2720 | `currentTrackingPass!!` | Not Started |

---

### P1-2. Thread.sleep → Mono.delay

| Item | File | Line | Current | Status |
|------|------|------|---------|--------|
| [ ] | UdpFwICDService.kt | 1109 | `Thread.sleep(1000)` | Not Started |
| [ ] | BatchStorageManager.kt | 294 | `Thread.sleep(100)` | Not Started |

---

### P1-3. runBlocking 제거

| Item | File | Line | Status |
|------|------|------|--------|
| [ ] | ElevationCalculator.kt | 78 | Not Started |

---

### P1-4. GlobalData 동시성 (18필드)

| Object | Fields | Status |
|--------|--------|--------|
| [ ] Time | serverTimeZone, clientTimeZone | Not Started |
| [ ] Offset | TimeOffset, azimuthPositionOffset, elevationPositionOffset, trainPositionOffset, trueNorthOffset | Not Started |
| [ ] EphemerisTrakingAngle | azimuthAngle, elevationAngle, trainAngle | Not Started |
| [ ] SunTrackingData | azimuthAngle, azimuthSpeed, elevationAngle, elevationSpeed, trainAngle, trainSpeed | Not Started |
| [ ] Version | apiVersion, buildDate | Not Started |

---

### P1-5. subscribe() 에러 핸들러 (6건)

| Item | File | Line | Status |
|------|------|------|--------|
| [ ] | PassScheduleService.kt | 405 | Not Started |
| [ ] | PassScheduleService.kt | 417 | Not Started |
| [ ] | EphemerisService.kt | 135 | Not Started |
| [ ] | EphemerisService.kt | 148 | Not Started |
| [ ] | UdpFwICDService.kt | 195 | Not Started |
| [ ] | UdpFwICDService.kt | 933 | Not Started |

---

### P1-6. Graceful Shutdown 완성 (2건)

| Item | File | Description | Status |
|------|------|-------------|--------|
| [ ] | ThreadManager.kt | `@PreDestroy` 추가 | Not Started |
| [ ] | BatchStorageManager.kt | `@PreDestroy` cleanup 추가 | Not Started |

---

## Phase 2: FE 성능 (12-18시간)

### P2-1. deep watch 최적화 (34건)

**Critical**:
| Item | File | Line | Note | Status |
|------|------|------|------|--------|
| [ ] | PassSchedulePage.vue | 1209 | **무한 루프 위험** | Not Started |
| [ ] | PassSchedulePage.vue | 1354 | - | Not Started |

**High**:
| Item | File | Line | Status |
|------|------|------|--------|
| [ ] | EphemerisDesignationPage.vue | 2804 | Not Started |

**Medium** (Settings 컴포넌트들 - 27건):
| File | Count | Status |
|------|-------|--------|
| [ ] MaintenanceSettings.vue | 4 | Not Started |
| [ ] OffsetLimitsSettings.vue | 4 | Not Started |
| [ ] StowSettings.vue | 4 | Not Started |
| [ ] AlgorithmSettings.vue | 2 | Not Started |
| [ ] AntennaSpecSettings.vue | 2 | Not Started |
| [ ] AngleLimitsSettings.vue | 2 | Not Started |
| [ ] LocationSettings.vue | 2 | Not Started |
| [ ] SpeedLimitsSettings.vue | 2 | Not Started |
| [ ] StepSizeLimitSettings.vue | 2 | Not Started |
| [ ] TrackingSettings.vue | 2 | Not Started |

**Low** (기타 - 4건):
| File | Count | Status |
|------|-------|--------|
| [ ] SunTrackPage.vue | 1 | Not Started |
| [ ] HardwareErrorLogPanel.vue | 1 | Not Started |
| [ ] SelectScheduleContent.vue | 1 | Not Started |
| [ ] AllStatusContent.vue | 1 | Not Started |

---

### P2-2. console.log 정리 (988건)

| Priority | File | Count | Status |
|----------|------|-------|--------|
| High | PassSchedulePage.vue | 128 | Not Started |
| High | passScheduleStore.ts | 103 | Not Started |
| High | EphemerisDesignationPage.vue | 63 | Not Started |
| High | DashboardPage.vue | 60 | Not Started |
| Medium | TLEUploadContent.vue | 64 | Not Started |
| Medium | windowUtils.ts | 46 | Not Started |
| Low | 기타 | 524 | Not Started |

**해결책**: `devLog` 유틸리티 생성 후 일괄 교체

---

### P2-3. icdStore 최적화

| Task | Target | Status |
|------|--------|--------|
| [ ] shallowRef 적용 (객체 타입) | ~10개 ref | Not Started |
| [ ] 상태 그룹화 검토 | 81개 → 5개 그룹 | Not Started |

---

## Phase 3: FE 파일 분리 (17시간) - 1/18~19

### P3-1. PassSchedulePage.vue (4,838줄)

| Task | Target | Status |
|------|--------|--------|
| [ ] ScheduleTable.vue 추출 | ~500줄 | Not Started |
| [ ] ScheduleInfoPanel.vue 추출 | ~300줄 | Not Started |
| [ ] ScheduleChart.vue 추출 | ~400줄 | Not Started |
| [ ] ScheduleControls.vue 추출 | ~300줄 | Not Started |
| [ ] usePassScheduleTracking.ts 추출 | ~600줄 | Not Started |

---

### P3-2. EphemerisDesignationPage.vue (4,340줄)

| Task | Target | Status |
|------|--------|--------|
| [ ] SatelliteInfoPanel.vue 추출 | ~300줄 | Not Started |
| [ ] TLEInputDialog.vue 추출 | ~250줄 | Not Started |
| [ ] TrackingChart.vue 추출 | ~400줄 | Not Started |
| [ ] KeyholeSection.vue 추출 | ~200줄 | Not Started |
| [ ] useEphemerisTracking.ts 추출 | ~500줄 | Not Started |

---

### P3-3. icdStore.ts (2,971줄)

| Task | Target | Status |
|------|--------|--------|
| [ ] icdAntennaState.ts 분리 | ~600줄 | Not Started |
| [ ] icdBoardStatus.ts 분리 | ~700줄 | Not Started |
| [ ] icdTrackingState.ts 분리 | ~400줄 | Not Started |
| [ ] index.ts re-export 구성 | ~50줄 | Not Started |

---

### P3-4. BE 서비스 분리 (🎯 1/20~21)

**EphemerisService.kt (5,057줄)**:
| Task | Target | Status |
|------|--------|--------|
| [ ] EphemerisStateMachine.kt 추출 | ~1,000줄 | 1/20 |
| [ ] EphemerisTLEManager.kt 추출 | ~500줄 | 1/20 |
| [ ] EphemerisDataBatcher.kt 추출 | ~500줄 | 1/20 |
| [ ] EphemerisCommandSender.kt 추출 | ~800줄 | 1/20 |

**PassScheduleService.kt (3,846줄)**:
| Task | Target | Status |
|------|--------|--------|
| [ ] PassScheduleStateMachine.kt 추출 | ~800줄 | 1/20 |
| [ ] PassScheduleMonitor.kt 추출 | ~600줄 | 1/20 |
| [ ] PassScheduleTracker.kt 추출 | ~700줄 | 1/20 |

---

## Phase 4: 품질 개선 (10시간)

### P4-1. @Valid 검증 추가

| Controller | @RequestBody Count | Status |
|------------|-------------------|--------|
| [ ] EphemerisController | 4 | Not Started |
| [ ] PassScheduleController | 4 | Not Started |
| [ ] SettingsController | 9 | Not Started |

---

### P4-2. catch(Exception) 구체화 (88건)

| Priority | Files | Status |
|----------|-------|--------|
| [ ] Critical Controller 먼저 | EphemerisController, PassScheduleController | Not Started |
| [ ] Service 순차 | - | Not Started |

---

## Phase 5: 키보드 단축키 (2시간)

### P5-1. useKeyboardNavigation composable 생성

| Item | Description | Status |
|------|-------------|--------|
| [ ] | frontend/src/composables/useKeyboardNavigation.ts 생성 | Not Started |

**구현 코드**:
```typescript
import { onMounted, onUnmounted } from 'vue'

interface KeyboardOptions {
  onEscape?: () => void
  onEnter?: () => void
  onCtrlEnter?: () => void
}

export function useKeyboardNavigation(options: KeyboardOptions) {
  const handleKeyDown = (e: KeyboardEvent) => {
    switch (e.key) {
      case 'Escape':
        options.onEscape?.()
        break
      case 'Enter':
        if (e.ctrlKey) {
          options.onCtrlEnter?.()
        } else {
          options.onEnter?.()
        }
        break
    }
  }
  onMounted(() => document.addEventListener('keydown', handleKeyDown))
  onUnmounted(() => document.removeEventListener('keydown', handleKeyDown))
}
```

---

### P5-2. 모달/다이얼로그에 적용

| Item | Target | Status |
|------|--------|--------|
| [ ] | 모든 q-dialog 컴포넌트 → ESC로 닫기 | Not Started |
| [ ] | 확인 다이얼로그 → Enter로 확인 | Not Started |

---

## 장기: 개발 완료 후 진행

### 장기-1. 테스트 추가

| Category | Target | Status |
|----------|--------|--------|
| [ ] BE Service Tests | EphemerisServiceTest.kt, PassScheduleServiceTest.kt, ICDServiceTest.kt | 장기 |
| [ ] BE Algorithm Tests | LimitAngleCalculatorTest.kt, CoordinateTransformerTest.kt | 장기 |
| [ ] FE Store Tests | icdStore.spec.ts | 장기 |

---

### 장기-2. 보안 강화 (인증/인가)

| Task | Description | Status |
|------|-------------|--------|
| [ ] Spring Security | 의존성 추가, SecurityConfig 생성 | 장기 |
| [ ] JWT 토큰 | 검증 구현 | 장기 |
| [ ] 로그인 API | AuthController.kt 구현 | 장기 |

---

### 장기-3. Docker 컨테이너화

| Task | Description | Status |
|------|-------------|--------|
| [ ] Backend Dockerfile | eclipse-temurin:21-jre-alpine | 장기 |
| [ ] Frontend Dockerfile | node:20-alpine + nginx:alpine | 장기 |
| [ ] docker-compose.yml | 통합 구성 | 장기 |

---

### 장기-4. CI/CD 파이프라인

| Task | Description | Status |
|------|-------------|--------|
| [ ] .gitlab-ci.yml | test, build, deploy stages | 장기 |

---

## Execution Log

| Date | Phase | Task | Result | Notes |
|------|-------|------|--------|-------|
| 2026-01-15 | Setup | 문서 재구성 | Done | Legacy 이동, 새 문서 생성 |
| 2026-01-15 | Analysis | 코드 검증 | Done | Legacy 46건 → 실제 7건 확인 |
| - | - | - | - | - |

---

## Blockers & Issues

| ID | Description | Status | Resolution |
|----|-------------|--------|------------|
| 1 | PassSchedulePage.vue deep watch 무한 루프 | Open | P2-1에서 해결 예정 |

---

## Metrics

### Before Refactoring (검증 완료)

| Metric | Reported | Verified |
|--------|----------|----------|
| !! operators | 46 | **7** |
| Thread.sleep | 2 | **2** |
| runBlocking | 1 | **1** |
| GlobalData 동시성 | 18필드 | **18필드** |
| subscribe() 에러 핸들러 | 4 | **6** |
| Graceful Shutdown 누락 | - | **2** |
| console.log | 1,513 | **988** |
| shallowRef usage | 0 | **0** |
| deep watch | 34 | **34** |
| Path Traversal | 1 | **1 (Critical)** |
| CORS Wildcard | 1 | **1 (Critical)** |
| innerHTML XSS | 4 | **4 (High)** |
| Tests | 1 | **2 (BE)** |

### After Refactoring (Target)

| Metric | Target |
|--------|--------|
| !! operators | 0 |
| Thread.sleep | 0 |
| runBlocking | 0 |
| subscribe() 에러 핸들러 | 모두 추가 |
| Graceful Shutdown | 완료 |
| console.log (prod) | 0 |
| Security issues | 0 |
| Max file size | < 2,000 lines |
| Tests | 10+ (장기) |

---

**Last Updated**: 2026-01-15 (PLAN.md v3.6.0 동기화 - 전체 일정 1/21 완료)
