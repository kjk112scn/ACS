# 분석 메타정보

> ACS 코드베이스 심층 분석 현황

## 기본 정보

| 항목 | 값 |
|-----|---|
| 분석 일시 | 2026-01-15 |
| 분석 버전 | 1.0.0 |
| 분석 범위 | 전체 (Full Scan) |

## 분석 대상

| 영역 | 경로 | 파일 수 | 총 줄 수 |
|-----|------|--------|---------|
| Backend | `backend/src/main/kotlin/` | 67개 (.kt) | 33,284줄 |
| Frontend | `frontend/src/` | 93개 (.vue, .ts) | 30,000줄+ |
| **합계** | - | **160개** | **63,000줄+** |

## 핵심 대형 파일

### Backend (500줄+)
| 파일 | 줄 수 | 분류 |
|-----|------|------|
| EphemerisService.kt | 5,057 | service/mode |
| PassScheduleService.kt | 3,846 | service/mode |
| ICDService.kt | 2,788 | service/icd |
| PassScheduleController.kt | 1,557 | controller/mode |
| SatelliteTrackingProcessor.kt | 1,387 | algorithm |
| UdpFwICDService.kt | 1,228 | service/udp |
| SettingsService.kt | 1,183 | service/system |
| EphemerisController.kt | 1,091 | controller/mode |

### Frontend (300줄+)
| 파일 | 줄 수 | 분류 |
|-----|------|------|
| DashboardPage.vue | 1,000+ | pages |
| PassSchedulePage.vue | 800+ | pages/mode |
| EphemerisDesignationPage.vue | 800+ | pages/mode |
| icdStore.ts | 400+ | stores/icd |
| icdService.ts | 400+ | services/api |

## 분석 Phase 진행 상황

| Phase | 상태 | 완료일 | 산출물 |
|-------|------|-------|--------|
| Phase 1: 전수조사 | ✅ 완료 | 2026-01-15 | backend/structure.md, frontend/structure.md |
| Phase 2: 심층분석 | ✅ 완료 | 2026-01-15 | services.md, stores.md, pages.md |
| Phase 3: 도메인분석 | ✅ 완료 | 2026-01-15 | domain-logic/satellite-tracking.md |
| Phase 4: Gap분석 | ✅ 완료 | 2026-01-15 | synthesis/gap-report.md, refactoring-hints.md |
| Phase 5: 문서통합 | ✅ 완료 | 2026-01-15 | context/*.md 업데이트 (4개 문서) |

## 변경 이력

| 일시 | 버전 | 내용 | 담당 |
|-----|------|------|------|
| 2026-01-15 | 1.4.0 | Phase 5 완료: Context 문서 통합 (backend.md, frontend.md, icd-protocol.md, satellite-tracking.md) | Analysis Team |
| 2026-01-15 | 1.3.0 | Phase 4 완료: Gap 분석 및 리팩토링 힌트 | Analysis Team |
| 2026-01-15 | 1.2.0 | Phase 2 완료: BE/FE 심층 분석 (services.md, stores.md, pages.md) | BE/FE Expert |
| 2026-01-15 | 1.1.0 | Algorithm 계층 심층 분석 (Phase 3 완료) | Algorithm Expert |
| 2026-01-15 | 1.0.0 | 초기 전수 분석 (Phase 1 완료) | BE Expert, FE Expert |

---

**분석 완료**: 모든 Phase 완료. 리팩토링 시 `synthesis/refactoring-hints.md` 참조.
