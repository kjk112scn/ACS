# Timezone Handling Standardization

## 개요

**목적**: 시간대(Timezone) 처리 아키텍처 표준화 - 내부 UTC, 표시 로컬 원칙 완전 적용
**요청일**: 2026-01-20
**상태**: 🚧 진행중

## 요구사항

### 필수
- [ ] Backend 하드코딩된 timezone 제거 (Asia/Seoul 등)
- [ ] Frontend 사용자 timezone 선택 기능 추가
- [ ] 자동감지 (브라우저) + 수동선택 지원
- [ ] 전체 IANA timezone 지원 (~400개)
- [ ] 폐쇄망 환경 완벽 지원 (인터넷 불필요)

### 권장
- [ ] 자주 쓰는 timezone 상단 고정
- [ ] 검색형 Select UI
- [ ] 설정 localStorage 저장

## 영향 범위

### Backend (수정 필요: 6개 파일)

| 파일 | 문제 | 심각도 |
|------|------|--------|
| `SunTrackService.kt:271` | `"Asia/Seoul"` 하드코딩 | 긴급 |
| `GlobalData.kt:26-30` | `systemDefault()` 사용 | 높음 |
| `LoggingController.kt:108,129` | `systemDefault()` 사용 | 높음 |
| `GlobalExceptionHandler.kt` | `LocalDateTime.now()` | 중간 |
| `application-with-db.properties` | timezone 설정 없음 | 중간 |

### Frontend (수정 필요: 8개 파일)

| 파일 | 문제 | 심각도 |
|------|------|--------|
| `times.ts:102` | `'ko-KR'` 하드코딩 | 필수 |
| `logger.ts:48` | `'ko-KR'` 하드코딩 | 필수 |
| `HardwareErrorLogPanel.vue:624` | `'en-US'` 하드코딩 | 필수 |
| `AllStatusContent.vue` | `.toLocaleTimeString()` | 권장 |
| `MaintenanceSettings.vue` | `.toLocaleTimeString()` | 권장 |
| `DashboardPage.vue` | `.toLocaleTimeString()` | 권장 |
| `icdStore.ts` | `.toLocaleTimeString()` | 권장 |
| `SystemInfoContent.vue` | `.toLocaleString()` | 권장 |

### 알고리즘 (수정 불필요)

| 모듈 | 현재 상태 | 영향 |
|------|----------|------|
| OrekitCalculator | UTC 고정 | 없음 |
| SolarOrekitCalculator | UTC 고정 | 없음 |
| CoordinateTransformer | 시간 무관 | 없음 |

## 관련 문서

- [ADR-006](../../../decisions/ADR-006-timezone-handling-architecture.md) - 아키텍처 결정
- [DESIGN.md](DESIGN.md) - 상세 설계
- [PROGRESS.md](PROGRESS.md) - 진행 상황

## 기술 결정 요약

| 항목 | 결정 |
|------|------|
| Timezone 표준 | IANA (Asia/Seoul 형식) |
| 자동감지 | `Intl.DateTimeFormat().resolvedOptions().timeZone` |
| 전체 목록 | `Intl.supportedValuesOf('timeZone')` |
| FE 저장 | localStorage |
| FE 라이브러리 | date-fns-tz (2KB) |
| UI 위치 | 설정 > 지역 설정 (Language와 통합) |