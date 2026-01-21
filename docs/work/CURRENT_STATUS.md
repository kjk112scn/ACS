# 현재 작업 상태

> **새 세션 시작 시:** "CURRENT_STATUS.md 읽고 이어서 진행해줘" 또는 `/status`

**마지막 업데이트:** 2026-01-21

---

## 🚧 진행 중 작업

### 1. Timezone_Handling_Standardization (95%) ⭐ 오늘 작업
- **상태:** 구현 완료, 수동 테스트 대기
- **문서:** `docs/work/active/Timezone_Handling_Standardization/`
- **ADR:** ADR-006-timezone-handling-architecture.md
- **내일 할 일:** 다양한 timezone 선택 후 시간 표시 확인

| 영역 | 상태 | 변경 내용 |
|------|:----:|----------|
| BE 하드코딩 제거 | ✅ | SunTrackService, GlobalExceptionHandler 등 5개 파일 |
| FE 인프라 | ✅ | timezoneStore, useTimezone composable 신규 |
| FE 하드코딩 수정 | ✅ | MainLayout, HardwareErrorLogPanel 등 4개 파일 |
| 설정 UI | ✅ | GeneralSettings에 timezone 설정 추가 |
| 빌드 검증 | ✅ | FE/BE 모두 성공 |
| 수동 테스트 | ⏳ | 다양한 timezone 선택 테스트 필요 |

**핵심 변경:**
- 사용자가 설정 > 일반 설정에서 timezone 선택 가능
- 자동 감지 (브라우저) / 수동 선택 (IANA timezone) 지원
- 폐쇄망 환경에서도 동작 (브라우저 내장 Intl API)

---

### 2. HW_Error_System_Integration (90%)
- **상태:** 구현 완료, DB 테스트 대기
- **문서:** `docs/work/active/HW_Error_System_Integration/`
- **내일 할 일:** BE 재시작 → 에러 발생 → DB 저장 검증

| 컬럼 | 상태 | 설명 |
|------|:----:|------|
| tracking_mode | ✅ | 에러 발생 시 추적 모드 저장 |
| correlation_id | ✅ | 동시 에러 그룹화 UUID |
| raw_data | ✅ | 비트 데이터 JSON |
| session_id | ⏸️ | 보류 (복잡도 높음) |

### 3. Tracking_Session_Data_Enrichment (90%) ✅
- **상태:** 구현 완료, BE 재시작 후 DB 검증 필요
- **문서:** `docs/work/active/Tracking_Session_Data_Enrichment/`
- **근본 원인:** 키 이름 불일치 (SatelliteID vs SatelliteId 등)

| 컬럼 | 수정 내용 |
|------|----------|
| satellite_id | ✅ SatelliteID/SatelliteId 양쪽 지원 |
| duration | ✅ ISO String 파싱 + 시간차 계산 |
| max_azimuth_rate | ✅ MaxAzRate/MaxAzimuthRate 양쪽 지원 |
| max_elevation_rate | ✅ MaxElRate/MaxElevationRate 양쪽 지원 |
| total_points | ✅ DTL 개수 폴백 추가 |

### 4. V003 DB 코멘트 마이그레이션
- **상태:** 파일 생성 완료, Flyway 적용 대기
- **파일:** `V003__Add_table_column_comments.sql`
- **내일 할 일:** BE 재시작 시 자동 적용 확인

---

### 5. Architecture Refactoring (70%)
- **상태:** BE/FE/CSS 완료, PassSchedule 88%, DB 테스트 대기
- **문서:** `docs/work/active/Architecture_Refactoring/`

| 영역 | 완료율 | 상태 |
|------|:------:|:----:|
| BE/FE/CSS 리팩토링 | 100% | ✅ 완료 |
| DB Integration 코드 | 100% | ✅ 코드완료 |
| DB 스키마 재설계 | 100% | ✅ 8개 테이블 + ICD 저장 |
| DB 테스트 | 100% | ✅ 연결 성공 |
| PassSchedule 상태머신 | 88% | ⏳ 수동 테스트 대기 |
| UI/UX 리팩토링 | 10% | 계획만 완료 |

### 6. 다국어 지원 경량화 (시스템 완료, 적용 대기)
- **상태:** vue-i18n → TS 상수 전환 완료, 하드코딩 정리 필요
- **남은 작업:** ~1,770개 하드코딩 → `T.xxx` 교체 (Phase 3)
- **중요:** 교체해야 언어 전환 시 영어로 표시됨
- **관련 파일:** `frontend/src/texts/`, ADR-005

---

## ✅ 최근 완료

| 날짜 | 작업 | 커밋 |
|------|------|------|
| 2026-01-21 | Admin Panel 분리 + UX 개선 | `a9a6af4` |
| 2026-01-20 | Tracking_Session 키 매핑 수정 | `aa0f7cb` |
| 2026-01-20 | HW_Error 컨텍스트 추가 | `aa0f7cb` |
| 2026-01-20 | V003 DB 코멘트 마이그레이션 | `aa0f7cb` |
| 2026-01-20 | Timezone 처리 표준화 (ADR-006) | 미커밋 |
| 2026-01-20 | DB 스키마 재설계 + ICD 저장 | `a9d7bf1` |
| 2026-01-20 | DB 테스트 성공 + i18n 3페이지 | `docs: DB 테스트 완료 및 UX 개선 계획 수립` |
| 2026-01-20 | PassSchedule Phase A-D 구현 | `refactor(passschedule): Phase D 완료` |
| 2026-01-20 | vue-i18n → TS 상수 객체 | `refactor(i18n): vue-i18n → TypeScript 상수 객체 마이그레이션` |

---

## 🔧 다음 작업 (실행 순서)

> **상세 로드맵:** `docs/work/active/ROADMAP.md`
> **원칙:** 모든 코드 변경 완료 후 수동 테스트 (한 번에 검증)

### Phase 1: 코드 개선 ✅ 완료
| 순서 | 작업 | 예상 | 체크 |
|:---:|------|:----:|:----:|
| 1-1 | PassSchedule C2-C3 동시성 | 2-3시간 | ✅ |
| 1-2 | DB 테스트 (TimescaleDB) | 1-2시간 | ✅ |

### Phase 2: DB + UX 개선 ⭐
| 순서 | 작업 | 예상 | 체크 |
|:---:|------|:----:|:----:|
| 2-0 | DB 초기화 후 Flyway 테스트 | 30분 | ☐ |
| 2-1 | 헤더 HW 상태 추가 | 1일 | ⏳ 전문가 검토 |
| 2-2 | 모드 페이지 통일 (mode-shell) | 3-4일 | ⏳ 전문가 검토 |
| 2-3 | Drawer 네비게이션 | 미룸 | 📅 데이터 분석 화면 때 |

> **발견:** `/api/icd/communication-status` API 이미 존재 (HW 상태 확인용)

### Phase 3: 다국어 완성 (미룸)
| 순서 | 작업 | 예상 | 체크 |
|:---:|------|:----:|:----:|
| 3-1 | i18n 하드코딩 정리 | 일괄 작업 | 📅 미룸 |

**완료된 페이지 (3개):**
- ✅ StandbyPage.vue (6개)
- ✅ SunTrackPage.vue (7개)
- ✅ LoginPage.vue (1개)

**남은 페이지 (~1,770개):** 나중에 일괄 작업 예정

### Phase 4: 최종 검증 (마지막)
| 순서 | 작업 | 예상 | 체크 |
|:---:|------|:----:|:----:|
| 4-1 | PassSchedule 수동 테스트 (T1-T30) | 2-3시간 | ☐ |
| 4-2 | 전체 기능 통합 테스트 | 1-2시간 | ☐ |
| 4-3 | 버그 수정 (발견 시) | 가변 | ☐ |

### 기타 (우선순위 낮음)
- LoggingService 정리 - 특수 기능 미사용

---

## 📊 프로젝트 건강 상태

**최근 체크:** 2026-01-20 | **점수:** 82/100 (양호)

| 항목 | 상태 |
|------|------|
| 빌드 | ✅ BE/FE 성공 |
| TypeScript | ✅ 오류 없음 |
| ESLint | ⚠️ 1개 경고 |

**상세:** `docs/work/health-checks/2026-01-20.md`

---

## 💡 새 세션에서 이어서 하기

```
# 방법 1: 직접 요청
"CURRENT_STATUS.md 읽고 이어서 진행해줘"

# 방법 2: 스킬 사용
/status

# 방법 3: 특정 작업 지정
"i18n 마이그레이션 이어서 해줘"
"Architecture Refactoring 계속해줘"
```
