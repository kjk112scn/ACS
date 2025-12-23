# Architecture Refactoring (아키텍처 리팩토링) 메인 계획서

## 개요

- **목적**: ACS 프로젝트의 코드 품질, 구조, 성능을 혁신적으로 개선
- **우선순위**: 🟠 P1
- **예상 규모**: 대형 (5주)

---

## 세부 계획서

| 문서 | 설명 |
|------|------|
| [Frontend_Refactoring_plan.md](./Frontend_Refactoring_plan.md) | 프론트엔드 상세 계획 |
| [Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md) | 백엔드 상세 계획 |
| [Security_Stability_plan.md](./Security_Stability_plan.md) | 🟢 보안 및 안정성 (선택적 - 로컬 환경) |

---

## 현황 요약

### 잘 설계된 부분 (유지)

| 구성 요소 | 설명 |
|----------|------|
| ThreadManager | 하드웨어 자동 감지, 성능 등급별 스레드 풀 관리 |
| 실시간 통신 | UDP(10ms) → BE → WebSocket(30ms) → FE 파이프라인 |
| 우선순위 체계 | CRITICAL(UDP) > HIGH(WebSocket) > NORMAL(Tracking) > LOW(Batch) |

### 개선이 필요한 부분

| 영역 | 문제 | 대표 파일 |
|------|------|----------|
| FE 구조 | 애매한 파일 위치, 중복 파일 | `modeStore.ts` 2개 |
| FE 중복 코드 | 11개 Settings 컴포넌트에 동일 로직 | `Settings/system/*.vue` |
| FE 거대 파일 | Pages, Components, Stores | `icdStore.ts` (2,971줄) |
| BE 거대 서비스 | 단일 파일에 너무 많은 책임 | `EphemerisService.kt` (4,986줄) |
| BE 하드코딩 | 에러 매핑이 코드에 직접 작성 | `HardwareErrorLogService.kt` |
| 테스트 부재 | BE 1.5%, FE 0% | 전체 |

---

## Phase 개요

| Phase | 영역 | 내용 | 리스크 |
|-------|------|------|--------|
| **Phase 0** | FE | 폴더 구조 정리 (파일 이동, 중복 제거) | 🟢 낮음 |
| **Phase 1** | FE+BE | Settings Composable, 에러매핑 YAML, 로거 | 🟢 낮음 |
| **Phase 2** | FE+BE | icdStore 개선, SunTrackService 분해 | 🟡 중간 |
| **Phase 3** | BE | EphemerisService 분해, 테스트 | 🔴 높음 |
| **Phase 4** | FE+BE | 보안/안정성 (선택적 - 외부 노출 시) | 🟢 선택 |

---

## 거대 파일 목록

### 프론트엔드 (18개)

| 분류 | 파일 | 줄 수 |
|------|------|-------|
| Pages | PassSchedulePage.vue | 4,841 |
| Pages | EphemerisDesignationPage.vue | 4,376 |
| Pages | DashboardPage.vue | 2,728 |
| Pages | FeedPage.vue | 2,531 |
| Pages | SunTrackPage.vue | 1,289 |
| Components | AllStatusContent.vue | 2,381 |
| Components | SelectScheduleContent.vue | 2,270 |
| Components | TLEUploadContent.vue | 1,678 |
| Components | SystemInfoContent.vue | 1,561 |
| Stores | icdStore.ts | 2,971 |
| Stores | passScheduleStore.ts | 2,452 |
| Stores | ephemerisTrackStore.ts | 1,367 |
| Services | ephemerisTrackService.ts | 1,192 |
| Services | passScheduleService.ts | 1,117 |

### 백엔드 (6개 핵심)

| 분류 | 파일 | 줄 수 |
|------|------|-------|
| Service | EphemerisService.kt | 4,986 |
| Service | PassScheduleService.kt | 2,896 |
| Service | ICDService.kt | 2,788 |
| Service | UdpFwICDService.kt | 1,294 |
| Service | SunTrackService.kt | 979 |
| Controller | PushDataController.kt | 763 |

---

## 일정

| 주차 | 작업 | 상세 문서 |
|------|------|----------|
| **0주차** | Phase 0 (FE 폴더 구조 정리) | [Frontend_Refactoring_plan.md](./Frontend_Refactoring_plan.md) |
| 1주차 | Phase 1 (Settings, 에러매핑, 로거) | FE + BE 문서 |
| 2주차 | Phase 2 (icdStore, SunTrackService) | FE + BE 문서 |
| 3주차 | Phase 2 (Composables, 테스트) | [Frontend_Refactoring_plan.md](./Frontend_Refactoring_plan.md) |
| 4주차 | Phase 3 (EphemerisService, 테스트) | [Backend_Refactoring_plan.md](./Backend_Refactoring_plan.md) |

---

## 완료 기준 요약

### Phase 0
- [ ] FE 루트 파일 정리
- [ ] 중복 파일 통합
- [ ] 빌드 성공

### Phase 1
- [ ] `useSettingsForm.ts` 생성 및 적용
- [ ] `error-mappings.yml` 생성 및 적용
- [ ] `logger.ts` 생성 및 적용

### Phase 2
- [ ] icdStore 구조 개선
- [ ] SunTrackService 분해
- [ ] 필터/페이징 composables 생성

### Phase 3
- [ ] EphemerisService 분해
- [ ] 테스트 커버리지: BE 60%, FE 40%

### Phase 4 (선택적 - 외부 노출 시 적용)
- [ ] 하드코딩된 인증 정보 제거 (LoginPage.vue)
- [ ] JWT 기반 인증 시스템 구현
- [ ] CORS 정책 강화
- [ ] HTTPS/TLS 설정
- [ ] 메모리 누수 cleanup 추가 (장시간 운영 시)

---

## 롤백 계획

각 Phase는 독립적인 Git 브랜치에서 작업:
- `feature/phase0-folder-structure`
- `feature/phase1-settings-composable`
- `feature/phase1-error-mapping`
- `feature/phase2-icdstore-refactor`
- `feature/phase2-suntrack-refactor`
- `feature/phase3-ephemeris-refactor`

문제 발생 시 해당 브랜치만 롤백

---

**문서 버전**: 2.1.0
**작성일**: 2024-12
**작성자**: Claude Code

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0.0 | 2024-12 | 최초 작성 |
| 1.1.0 | 2024-12 | 백엔드 계획 상세화 |
| 2.0.0 | 2024-12 | 보안/안정성 계획 추가 (Security_Stability_plan.md) |
| 2.1.0 | 2024-12 | Security를 Phase 4 (선택적)로 변경 - 로컬 환경 기준 |
