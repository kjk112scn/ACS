# Frontend 리팩토링 통합 테스트 체크리스트

**작성일**: 2026-01-18
**총 테스트 항목**: 79개

---

## 테스트 범위

| 구분 | 항목 수 | 상태 |
|-----|--------|------|
| Part 1: useErrorHandler/catch 블록 | 41개 | 코드 완료 |
| Part 2: CSS !important Phase 1 | 17개 | 분석 완료 |
| Part 3: CSS !important Phase 2-3 | 21개 | 분석 완료 |
| **총계** | **79개** | |

---

# Part 1: useErrorHandler / catch 블록 리팩토링

**변경 내용**: handleApiError(error, context) 패턴으로 통일

## 1.1 FeedPage.vue (8개)

| # | 테스트 항목 | 방법 | 결과 |
|---|------------|------|------|
| 1 | S-Band LNA RHCP 토글 | 아이콘 클릭 → 에러 시 알림 표시 | ⬜ |
| 2 | S-Band LNA LHCP 토글 | 아이콘 클릭 → 에러 시 알림 표시 | ⬜ |
| 3 | X-Band LNA RHCP 토글 | 아이콘 클릭 → 에러 시 알림 표시 | ⬜ |
| 4 | X-Band LNA LHCP 토글 | 아이콘 클릭 → 에러 시 알림 표시 | ⬜ |
| 5 | Ka-Band Selection RHCP 토글 | 아이콘 클릭 → 에러 시 알림 표시 | ⬜ |
| 6 | Ka-Band Selection LHCP 토글 | 아이콘 클릭 → 에러 시 알림 표시 | ⬜ |
| 7 | RF Switch 토글 | 아이콘 클릭 → 에러 시 알림 표시 | ⬜ |
| 8 | FAN 토글 | 아이콘 클릭 → 에러 시 알림 표시 | ⬜ |

---

## 1.2 EphemerisDesignationPage.vue (9개)

**버그 수정 포함**: Stop/Stow 버튼 에러 알림이 이전에는 표시되지 않았음

| # | 테스트 항목 | 방법 | 결과 |
|---|------------|------|------|
| 1 | 스케줄 데이터 로드 | 페이지 로드 시 스케줄 목록 표시 | ⬜ |
| 2 | 스케줄 선택 | 스케줄 선택 모달에서 항목 선택 | ⬜ |
| 3 | TLE 데이터 처리 | TLE 입력 모달에서 데이터 추가 | ⬜ |
| 4 | **추적 시작 (Go 버튼)** | 에러 시 알림 표시 확인 | ⬜ |
| 5 | **추적 중지 (Stop 버튼)** | **버그 수정됨** - 에러 알림 확인 | ⬜ |
| 6 | **Stow 명령** | **버그 수정됨** - 에러 알림 확인 | ⬜ |
| 7 | ATC 팝업 열기 | 3축 변환 계산기 팝업 | ⬜ |
| 8 | 실시간 추적 데이터 CSV | CSV 다운로드 버튼 클릭 | ⬜ |
| 9 | 이론치 CSV 다운로드 | CSV 다운로드 버튼 클릭 | ⬜ |

---

## 1.3 PassSchedulePage.vue (6개)

| # | 테스트 항목 | 방법 | 결과 |
|---|------------|------|------|
| 1 | TLE 업로드 모달 열기 | TLE 업로드 버튼 클릭 | ⬜ |
| 2 | 스케줄 데이터 로드 | 스케줄 선택 시 데이터 로드 | ⬜ |
| 3 | 스케줄 선택 모달 열기 | 스케줄 선택 버튼 클릭 | ⬜ |
| 4 | ACS Start 명령 (Go) | 에러 시 알림 표시 확인 | ⬜ |
| 5 | 정지 명령 (Stop) | 에러 시 알림 표시 확인 | ⬜ |
| 6 | Stow 명령 | 에러 시 알림 표시 확인 | ⬜ |

---

## 1.4 모드 페이지 공통 (18개)

### StepPage.vue

| # | 테스트 항목 | 방법 | 결과 |
|---|------------|------|------|
| 1 | Go 버튼 | 에러 시 알림 표시 | ⬜ |
| 2 | Stop 버튼 | 에러 시 알림 표시 | ⬜ |
| 3 | Stow 버튼 | 에러 시 알림 표시 | ⬜ |
| 4 | ControlButtonBar 렌더링 | 버튼 3개 정상 표시 | ⬜ |

### SlewPage.vue

| # | 테스트 항목 | 방법 | 결과 |
|---|------------|------|------|
| 1 | Go 버튼 | 에러 시 알림 표시 | ⬜ |
| 2 | Stop 버튼 | 에러 시 알림 표시 | ⬜ |
| 3 | Stow 버튼 | 에러 시 알림 표시 | ⬜ |
| 4 | ControlButtonBar 렌더링 | 버튼 3개 정상 표시 | ⬜ |

### SunTrackPage.vue

| # | 테스트 항목 | 방법 | 결과 |
|---|------------|------|------|
| 1 | Go 버튼 | 에러 시 알림 표시 | ⬜ |
| 2 | Stop 버튼 | 에러 시 알림 표시 | ⬜ |
| 3 | Stow 버튼 | 에러 시 알림 표시 | ⬜ |
| 4 | ControlButtonBar 렌더링 | 버튼 3개 정상 표시 | ⬜ |

### StandbyPage.vue

| # | 테스트 항목 | 방법 | 결과 |
|---|------------|------|------|
| 1 | 페이지 정상 로드 | 에러 없음 | ⬜ |
| 2 | 기능 동작 | 기본 기능 확인 | ⬜ |

### PedestalPositionPage.vue

| # | 테스트 항목 | 방법 | 결과 |
|---|------------|------|------|
| 1 | Go 버튼 | 에러 시 알림 표시 | ⬜ |
| 2 | Stop 버튼 | 에러 시 알림 표시 | ⬜ |
| 3 | Stow 버튼 | 에러 시 알림 표시 | ⬜ |
| 4 | ControlButtonBar 렌더링 | 버튼 3개 정상 표시 | ⬜ |

---

# Part 2: CSS !important Phase 1 (미구현 - 분석만 완료)

**대상**: StandbyPage, SunTrackPage, StepPage, SlewPage, PedestalPositionPage, OffsetControls
**상태**: 코드 미수정, 분석만 완료

## 2.1 Phase 1 작업 후 레이아웃 테스트 (11개)

| # | 파일 | 테스트 항목 | 확인 방법 | 결과 |
|---|-----|------------|----------|------|
| 1 | StandbyPage | 섹션 제목 마진 | Standby 모드 레이아웃 | ⬜ |
| 2 | SunTrackPage | 전체 레이아웃 높이 | 카드 높이, 마진 확인 | ⬜ |
| 3 | SunTrackPage | Offset Control Row 간격 | Offset/Speed 섹션 간격 | ⬜ |
| 4 | StepPage | 축 패널 체크박스 정렬 | Az/El/Tilt 헤더 정렬 | ⬜ |
| 5 | StepPage | 입력 필드 간격 | Angle/Speed 필드 간격 | ⬜ |
| 6 | SlewPage | 축 헤더 마진 (2rem) | 체크박스-Speed 간격 | ⬜ |
| 7 | SlewPage | Speed 입력창 높이 | 2rem 높이 유지 | ⬜ |
| 8 | PedestalPositionPage | 체크박스 위치 | 위로 -0.5rem 위치 | ⬜ |
| 9 | PedestalPositionPage | 카드 섹션 패딩 | 상단/하단 패딩 확인 | ⬜ |
| 10 | OffsetControls | 입력 필드 너비 | 110px 동일 너비 | ⬜ |
| 11 | OffsetControls | Cal Time 필드 너비 | 190-220px 범위 | ⬜ |

## 2.2 Phase 1 반응형 테스트 (3개)

| # | 뷰포트 | 확인 항목 | 결과 |
|---|--------|----------|------|
| 1 | Desktop (1920px) | 모든 페이지 레이아웃 | ⬜ |
| 2 | Tablet (1024px) | 카드 레이아웃 변경 | ⬜ |
| 3 | Mobile (768px) | 세로 배치 전환 | ⬜ |

## 2.3 Phase 1 기능 테스트 (3개)

| # | 페이지 | 확인 항목 | 결과 |
|---|--------|----------|------|
| 1 | 모든 모드 페이지 | Go/Stop/Stow 버튼 | ⬜ |
| 2 | 모든 모드 페이지 | Offset 입력/버튼 | ⬜ |
| 3 | SlewPage | Loop 기능 | ⬜ |

---

# Part 3: CSS !important Phase 2-3 (미구현 - 분석만 완료)

**대상**: FeedPage, ScheduleTable, ScheduleChart, EphemerisDesignationPage, PassSchedulePage
**상태**: 코드 미수정, 분석만 완료

## 3.1 Phase 2 테스트 (13개)

### FeedPage.vue

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 3열 그리드 정렬 | S/X/Ka 밴드 카드 동일 높이 | ⬜ |
| 2 | RF Switch 경로 표시 | SVG 오버플로우 없음 | ⬜ |
| 3 | LNA 아이콘 크기 | 모든 아이콘 80px | ⬜ |
| 4 | Fan 섹션 높이 | 115px 고정 | ⬜ |
| 5 | 범례 그리드 정렬 | 하단 범례 정상 | ⬜ |
| 6 | 3밴드 모드 레이아웃 | Ka 밴드 추가 시 | ⬜ |

### ScheduleTable.vue

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 테이블 높이 210px | 스크롤바 정상 | ⬜ |
| 2 | 헤더 sticky | 스크롤 시 고정 | ⬜ |
| 3 | 현재 스케줄 하이라이트 | 초록색 배경 | ⬜ |
| 4 | 다음 스케줄 하이라이트 | 노란색 배경 | ⬜ |
| 5 | 페이지네이션 숨김 | 하단 컨트롤 숨김 | ⬜ |

### ScheduleChart.vue

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 차트 높이 360px | ECharts 렌더링 | ⬜ |
| 2 | 카드 섹션 패딩 | 잘림 없음 | ⬜ |

## 3.2 Phase 3 테스트 (14개)

### EphemerisDesignationPage.vue

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 3개 카드 동일 높이 | Position View, Control, TLE | ⬜ |
| 2 | **차트 중앙 정렬** | 500px 원형 차트 위치 | ⬜ |
| 3 | TLE 에디터 높이 | 140px 텍스트 영역 | ⬜ |
| 4 | TLE 버튼 그룹 위치 | 에디터 하단 정렬 | ⬜ |
| 5 | 테이블 헤더 sticky | 스케줄 목록 스크롤 | ⬜ |
| 6 | 정렬 아이콘 위치 | 헤더 정렬 버튼 | ⬜ |
| 7 | 테마 색상 적용 | 다크/라이트 전환 | ⬜ |

### PassSchedulePage.vue

| # | 테스트 항목 | 확인 방법 | 결과 |
|---|------------|----------|------|
| 1 | 테이블 높이 210px | 3개 행만 표시 | ⬜ |
| 2 | **현재 추적 행** | 초록색 배경 + 테두리 | ⬜ |
| 3 | **다음 추적 행** | 노란색 배경 | ⬜ |
| 4 | 컨트롤 입력 높이 | 40px 입력창 | ⬜ |
| 5 | 반응형 레이아웃 | 768px 미만 세로 배치 | ⬜ |
| 6 | 스케줄 폼 정렬 | 폼 요소 상단 정렬 | ⬜ |
| 7 | 페이지네이션 숨김 | 하단 컨트롤 숨김 | ⬜ |

---

# 테스트 방법

## 정상 케이스 테스트
1. 백엔드 서버 실행 상태에서 테스트
2. 각 기능 버튼 클릭 → 정상 동작 확인
3. 성공 알림(초록색 토스트) 표시 확인

## 에러 케이스 테스트
1. **백엔드 서버 중지** 상태에서 테스트
2. 각 기능 버튼 클릭 → 에러 알림(빨간색 토스트) 표시 확인
3. 콘솔에 에러 로그 출력 확인

## 레이아웃 테스트
1. 브라우저 개발자 도구 열기 (F12)
2. Elements 패널에서 치수 확인
3. 반응형 모드로 뷰포트 변경 테스트

---

# 중요 확인 사항

## Part 1 (코드 완료)
- **EphemerisDesignationPage Stop/Stow**: 이전에는 에러 알림 안 나옴 → 이제 나와야 함
- **ControlButtonBar**: Go/Stop/Stow 버튼이 공용 컴포넌트로 변경됨

## Part 2-3 (분석만 완료, 코드 미수정)
- CSS !important 수정 시 레이아웃 깨짐 주의
- **절대 제거 금지 영역**: 차트 위치, 테이블 높이, 테마 색상

---

# 변경된 파일 목록

## 코드 수정 완료

### catch 블록 리팩토링
- `EphemerisDesignationPage.vue` - 9개 catch + 버그 2개 수정
- `PassSchedulePage.vue` - 6개 catch
- `FeedPage.vue` - 5개 catch

### defineComponent 제거
- `StepPage.vue`
- `SlewPage.vue`
- `StandbyPage.vue`

### 공용 컴포넌트 적용
- `ControlButtonBar.vue` 생성
- StepPage, SlewPage, SunTrackPage, PedestalPositionPage에 적용

### 미사용 컴포넌트 삭제
- `ModeCard.vue` 삭제
- `ModeLayout.vue` 삭제

---

# 결과 요약

| 구분 | 총 항목 | 통과 | 실패 | 스킵 |
|------|--------|------|------|------|
| **Part 1: catch 블록** | 41 | | | |
| FeedPage | 8 | | | |
| EphemerisDesignationPage | 9 | | | |
| PassSchedulePage | 6 | | | |
| StepPage | 4 | | | |
| SlewPage | 4 | | | |
| SunTrackPage | 4 | | | |
| StandbyPage | 2 | | | |
| PedestalPositionPage | 4 | | | |
| **Part 2: Phase 1 CSS** | 17 | | | |
| 레이아웃 | 11 | | | |
| 반응형 | 3 | | | |
| 기능 | 3 | | | |
| **Part 3: Phase 2-3 CSS** | 21 | | | |
| FeedPage | 6 | | | |
| ScheduleTable | 5 | | | |
| ScheduleChart | 2 | | | |
| EphemerisDesignation | 7 | | | |
| PassSchedule | 7 | | | |
| **총계** | **79** | | | |

---

**테스트 완료일**: _______________
**테스트 담당자**: _______________
**비고**: _______________
