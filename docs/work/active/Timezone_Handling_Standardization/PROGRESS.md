# Timezone Handling Standardization - 진행 상황

## 진행률: 100% (구현 완료)

---

## Phase 1: 준비 (완료)

- [x] 요구사항 분석
- [x] 전문가 검토
  - [x] Backend 하드코딩 전수조사 (15개 발견)
  - [x] Frontend 시간표시 전수조사 (49개 발견)
  - [x] 알고리즘 영향 분석 (영향 없음 확인)
  - [x] UI/UX 배치 검토 (일반 설정 탭에 통합)
- [x] ADR-006 작성
- [x] 설계 문서 작성 (DESIGN.md)

---

## Phase 2: Backend 수정 (완료)

### 긴급 (CRITICAL)
- [x] `SunTrackService.kt:271` - Asia/Seoul 하드코딩 제거 → `formatUtcTime()` 함수로 변경

### 높음 (HIGH)
- [x] `GlobalData.kt:26-30` - 문서화 주석 추가, clientTimeZone deprecated 표시
- [x] `LoggingController.kt:108,129` - systemDefault() → ZoneOffset.UTC

### 중간 (MEDIUM)
- [x] `GlobalExceptionHandler.kt` - LocalDateTime → OffsetDateTime(UTC)
- [x] `application-with-db.properties` - timezone 정책 주석 추가

---

## Phase 3: Frontend 인프라 (완료)

### 신규 파일
- [x] `stores/common/timezoneStore.ts` 생성 - Pinia store
- [x] `composables/useTimezone.ts` 생성 - timezone 옵션 및 필터링

### 유틸리티 수정
- [x] `utils/times.ts` - `getCurrentTimezone()`, `formatToUserTimezone()` 추가

---

## Phase 4: Frontend 하드코딩 수정 (완료)

### 필수 (CRITICAL)
- [x] `times.ts:102` - formatWithTimezone() 수정 → getCurrentTimezone() 사용
- [x] `HardwareErrorLogPanel.vue:624` - formatToUserTimezone() 사용

### 권장 (ENHANCEMENT)
- [x] `AllStatusContent.vue:1483,1573` - formatToUserTimezone() 사용
- [x] `MainLayout.vue:130-136` - timezoneStore 연동, KST 하드코딩 제거

---

## Phase 5: 설정 UI (완료)

- [x] `GeneralSettings.vue` 에 Timezone 설정 추가
  - 자동 감지 토글
  - 수동 timezone 선택 (검색 가능한 드롭다운)
  - 현재 timezone 표시 배너

---

## Phase 6: 검증 (완료)

- [x] Frontend 빌드 확인 - 성공
- [x] Backend 빌드 확인 - 성공
- [ ] 수동 테스트 (다양한 timezone) - 사용자 테스트 필요
- [ ] 폐쇄망 환경 테스트 - 사용자 테스트 필요

---

## Phase 7: 완료 (대기)

- [ ] 문서 업데이트
- [ ] /done 스킬 실행
- [ ] 커밋

---

## 변경된 파일 목록

### Backend (5개)
| 파일 | 변경 내용 |
|------|---------|
| `SunTrackService.kt` | utcToKst → formatUtcTime, Asia/Seoul 제거 |
| `GlobalExceptionHandler.kt` | LocalDateTime → OffsetDateTime(UTC) |
| `LoggingController.kt` | systemDefault() → ZoneOffset.UTC |
| `GlobalData.kt` | 문서화 주석 추가, @deprecated |
| `application-with-db.properties` | timezone 정책 주석 |

### Frontend (7개)
| 파일 | 변경 내용 |
|------|---------|
| `stores/common/timezoneStore.ts` | 신규 - Pinia store |
| `composables/useTimezone.ts` | 신규 - 헬퍼 composable |
| `utils/times.ts` | getCurrentTimezone(), formatToUserTimezone() 추가 |
| `layouts/MainLayout.vue` | timezoneStore 연동, 동적 offset 표시 |
| `components/Settings/GeneralSettings.vue` | Timezone 설정 UI 추가 |
| `components/HardwareErrorLogPanel.vue` | formatToUserTimezone() 사용 |
| `components/content/AllStatusContent.vue` | formatToUserTimezone() 사용 |

---

## 일일 로그

### 2026-01-20

**전체 구현 완료**

| 시간 | 작업 |
|------|------|
| - | ADR-006 작성 완료 |
| - | 전문가 검토 완료 (BE/FE/알고리즘/UI) |
| - | Phase 2: Backend 하드코딩 제거 |
| - | Phase 3: Frontend 인프라 구축 (timezoneStore, useTimezone) |
| - | Phase 4: Frontend 하드코딩 수정 |
| - | Phase 5: 설정 UI 구현 (GeneralSettings에 통합) |
| - | Phase 6: 빌드 검증 성공 (FE/BE 모두) |

**결과:**
- BE: 5개 파일 수정
- FE: 7개 파일 수정/생성
- 빌드: FE/BE 모두 성공
- 테스트: 사용자 확인 필요