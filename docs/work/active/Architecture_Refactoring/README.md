# Architecture Refactoring

> **Status**: Active
> **Last Updated**: 2026-01-18
> **Version**: 5.0.0

---

## 전체 현황

| 영역 | 상태 | 완료율 | 상세 |
|-----|:----:|:-----:|------|
| BE 리팩토링 (Phase 1-5) | ✅ 완료 | 100% | [legacy/](legacy/) |
| FE 리팩토링 (Phase 2-3,6) | ✅ 완료 | 100% | [frontend/](frontend/) |
| CSS !important 정리 | ✅ 완료 | 100% | [css/](css/) |
| **DB Integration** | ✅ 코드완료 | 100% | [database/](database/) |
| DB 테스트 | ⬜ 대기 | 0% | 회사에서 실행 |

---

## 폴더 구조

```
Architecture_Refactoring/
├── README.md          ← 현재 문서 (마스터)
├── css/               ← CSS 리팩토링 문서
├── frontend/          ← FE 리팩토링 문서
├── database/          ← DB Integration 문서
└── legacy/            ← 완료된 과거 문서
```

---

## 레거시 vs 실제 검토 결과 (2026-01-17)

> 전문가 에이전트 검토 완료 - 레거시 RFC 문서 대비 **실제 필요 작업량 약 20%**

### BE (백엔드)

| 항목 | 레거시 | 실제 | 판정 | 비고 |
|------|:------:|:----:|:----:|------|
| !! 연산자 | 46건 | 10건 | ❌ 불필요 | null 체크 직후 사용, 안전 |
| subscribe() 핸들러 | 25건 | 0건 | ❌ 불필요 | **모두 에러 핸들러 있음** |
| mutableListOf | 65건 | 1건 | ❌ 불필요 | 대부분 로컬 변수/synchronized |
| println | 102건 | 68건 | ✅ 완료 | logger 변환 완료 |
| Thread.sleep | 2건 | 1건 | ⚠️ 선택적 | BatchStorageManager (100ms) |
| runBlocking | 1건 | 0건 | ✅ 완료 | 제거됨 |

### FE (프론트엔드)

| 항목 | 레거시 | 실제 | 판정 | 비고 |
|------|:------:|:----:|:----:|------|
| Offset Control 분산 | 3곳 | 0곳 | ✅ 완료 | **useOffsetControls로 통합됨** |
| 대형 파일 분리 | 5개 | 0개 긴급 | ❌ 불필요 | 이미 컴포넌트/composable 분리됨 |
| 하드코딩 색상 | 304건 | 50~80건 | ⚠️ 선택적 | 차트 예외, UI만 대상 |
| as 타입 단언 | 99건 | 20~30건 | ⚠️ 선택적 | icdStore WebSocket만 Type Guard 권장 |

---

## 완료된 작업 상세

### Sprint 0: 보안 Critical ✅

| 항목 | 파일 | 수정 내용 |
|------|------|----------|
| S0-1 Path Traversal | LoggingController.kt:172 | 파일명 검증 추가 (normalize + startsWith) |
| S0-2 CORS Wildcard | CorsConfig.kt:26 | `"*"` 제거 |
| S0-3 innerHTML XSS | windowUtils.ts | 4곳 → textContent/DOM API |

### Phase 1: BE 안정성 ✅

| 항목 | 상태 | 비고 |
|------|:----:|------|
| P1-1 !! 연산자 | ✅ | SunTrack 15건, PassSchedule 1건 제거 |
| P1-2 Thread.sleep | ✅ | 1건 남음 (100ms, 선택적) |
| P1-3 runBlocking | ✅ | 0건 |
| P1-4 GlobalData | ✅ | `@Volatile` 적용됨 |
| P1-5 subscribe() | ✅ | 에러 핸들러 모두 있음 확인 |
| P1-6 @PreDestroy | ✅ | 7개 서비스 적용됨 |

### Phase 2: FE 성능 ✅

| 항목 | 결과 |
|------|------|
| P2-1 deep watch | 34건 분석, 1건만 제거 가능 (HardwareErrorLogPanel) |
| P2-2 console.log | Production 빌드 자동 제거 설정 완료 |
| P2-3 icdStore | 3개 객체 shallowRef 변환 완료 |

### Phase 3: FE 파일 분리 ✅

| 분리 대상 | 결과물 |
|----------|--------|
| PassSchedulePage.vue | ScheduleTable, ScheduleInfoPanel, ScheduleChart |
| EphemerisDesignationPage.vue | SatelliteTrackingInfo, TleInputModal, TleDataSection, ScheduleSelectModal |
| icdStore.ts | useAntennaState, useBoardStatus, useTrackingState, icdTypes |

### Phase 4: 품질 개선 ✅

| 항목 | 완료 내용 |
|------|----------|
| @Valid 검증 | 20개 어노테이션 추가 (Controller 3+4+13) |
| catch(Exception) 구체화 | Controller 52건 완료, Service 보류 |

### Phase 5: BE 서비스 분리 ✅

| 분리 대상 | 결과물 |
|----------|--------|
| TLE 캐시 | EphemerisTLECache.kt, PassScheduleTLECache.kt |
| DataRepository | EphemerisDataRepository.kt (~280줄), PassScheduleDataRepository.kt (~280줄) |

### Phase 6: 키보드 단축키 ✅

| 항목 | 상태 | 비고 |
|------|:----:|------|
| useKeyboardNavigation.ts | ✅ | 255줄, 다양한 키 바인딩 지원 |
| 모달 ESC 닫기 | ✅ | q-dialog 기본 지원 |

### CSS !important 정리 ✅

| Phase | 제거 수 | 상태 |
|-------|:------:|:----:|
| Phase 1 | 87개 | ✅ |
| Phase 2 | 124개 | ✅ |
| Phase 3 | 22개 | ✅ |
| **합계** | **233개** | ✅ |

### DB Integration (Phase 6) ✅

| 항목 | 테이블 | 상태 |
|------|--------|:----:|
| TLE Cache | tle_cache | ✅ 코드완료 |
| Schedule Data | tracking_session, tracking_trajectory | ✅ 코드완료 |
| Tracking Result | tracking_result | ✅ 코드완료 |
| Hardware Error | hardware_error_log | ✅ 코드완료 |

---

## 작업 이력 (Execution Log)

| Date | Phase | Task | Result |
|------|-------|------|--------|
| 2026-01-15 | Sprint 0 | 보안 수정 (3건) | Done |
| 2026-01-15 | Phase 1 | BE 안정성 (6건) | Done |
| 2026-01-15 | Phase 2 | FE 성능 개선 | Done |
| 2026-01-16 | Phase 4 | @Valid, catch 구체화 | Done |
| 2026-01-16 | Phase 5 | TLE 캐시 분리 | Done |
| 2026-01-17 | Phase 5 | DataRepository 분리 | Done |
| 2026-01-17 | 검토 | 레거시 vs 실제 비교 | Done |
| 2026-01-18 | DB | Write-through 패턴 적용 | Done |
| 2026-01-18 | 품질 | println → logger 변환 (103건) | Done |

---

## Metrics

### Before → After

| Metric | Before | After | 상태 |
|--------|:------:|:-----:|:----:|
| !! operators | 46 | 0 | ✅ |
| Thread.sleep | 2 | 1 | ⚠️ 선택적 |
| runBlocking | 1 | 0 | ✅ |
| subscribe() 미처리 | 4 | 0 | ✅ |
| @PreDestroy 누락 | 2 | 0 | ✅ |
| console.log (prod) | 1,513 | 0 | ✅ 자동제거 |
| Path Traversal | 1 | 0 | ✅ |
| CORS Wildcard | 1 | 0 | ✅ |
| innerHTML XSS | 4 | 0 | ✅ |
| CSS !important | 233 | 0 | ✅ |

---

## 남은 작업

### 필수

- [ ] **DB 테스트**: 회사에서 TimescaleDB 설치 후 테스트
  - 상세: [database/TEST_PLAN.md](database/TEST_PLAN.md)

### 선택적 (P3)

| 항목 | 작업량 | 비고 |
|------|:------:|------|
| UI 하드코딩 색상 | 50~80건 | 차트 예외 |
| icdStore Type Guard | 20~30건 | 안정성 개선 |
| Thread.sleep 1건 | 1건 | BatchStorageManager |
| Quasar CSS 근본 개선 | - | [css/CSS_Quasar_Override_Strategy.md](css/CSS_Quasar_Override_Strategy.md) |

---

## 장기 계획

> 핵심 리팩토링 완료 후 진행

| 항목 | 설명 |
|------|------|
| 테스트 추가 | BE 2개 → 10+, FE 0개 → 추가 |
| 인증/인가 | Spring Security + JWT |
| Docker | Backend/Frontend 컨테이너화 |
| CI/CD | GitLab 전환 시 파이프라인 |

---

## 테스트 체크리스트

> 회사 복귀 후 실행

### Phase 1 검증

- [ ] SunTrack 모드 시작/중지
- [ ] Train 각도 초기화 및 이동
- [ ] Offset 변경 시 실시간 반영
- [ ] PassSchedule 스케줄 로드

### Phase 5 검증

- [ ] TLECache 동작 확인
- [ ] DataRepository 로그 확인

### DB Integration 검증

- [ ] TimescaleDB 연결
- [ ] Write-through 저장 확인
- [ ] 서버 재시작 후 복원 확인

---

## 하위 문서 참조

### CSS 리팩토링

| 문서 | 설명 |
|------|------|
| [CSS_Important_Cleanup_Plan.md](css/CSS_Important_Cleanup_Plan.md) | !important 정리 계획 |
| [CSS_Test_Checklist.md](css/CSS_Test_Checklist.md) | CSS 테스트 체크리스트 |
| [CSS_Quasar_Override_Strategy.md](css/CSS_Quasar_Override_Strategy.md) | Quasar 근본 해결 전략 |

### Frontend 리팩토링

| 문서 | 설명 |
|------|------|
| [FE_REFACTORING_PLAN.md](frontend/FE_REFACTORING_PLAN.md) | FE 리팩토링 상세 계획 |
| [FE_Refactoring_Test_Checklist.md](frontend/FE_Refactoring_Test_Checklist.md) | 통합 테스트 체크리스트 |

### Database Integration

| 문서 | 설명 |
|------|------|
| [DESIGN.md](database/DESIGN.md) | DB 설계 문서 |
| [PROGRESS.md](database/PROGRESS.md) | 진행 상황 + 설치 가이드 |
| [TEST_PLAN.md](database/TEST_PLAN.md) | 테스트 계획 |

### Legacy (과거 문서)

| 문서 | 설명 |
|------|------|
| [IMPROVEMENT_ROADMAP.md](legacy/IMPROVEMENT_ROADMAP.md) | 7-Expert 종합 로드맵 |
| [PHASE5_SEPARATION_PLAN.md](legacy/PHASE5_SEPARATION_PLAN.md) | 대형 파일 분리 계획 |

---

## 관련 문서

- [CHANGELOG.md](../../../CHANGELOG.md) - 전체 변경 이력
- [일일 로그](../../logs/) - 작업 로그
