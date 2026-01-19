---
name: refactor
description: 리팩토링 워크플로우. 대형 파일 분리, 구조 개선, 코드 품질 향상. "리팩토링", "refactor", "분리", "구조 개선", "정리" 키워드에 반응.
model: opus
---

# Refactor - 리팩토링 스킬 (v2.0)

> **검증 사례**: PassSchedule 상태머신 리팩토링 (15/17 완료, 88%)

## 역할

코드 구조 개선과 대형 파일 분리를 체계적으로 수행합니다.

**핵심 가치:**
- 자동 분석 + 전문가 검토
- 문서 기반 추적 관리
- 안전한 단계별 실행
- 진행 상황 실시간 추적

## 워크플로우 (v2.0 - PassSchedule 검증)

```
[1. 조사] → [2. 검토] → [3. 계획] → [4. 승인] → [5. 실행] → [6. 완료]
     │           │           │           │           │           │
  코드 분석   전문가      PLAN.md    사용자      단계별    /done
  버그 목록   에이전트    작성       확인       구현      호출
     │           │           │                    │
     ▼           ▼           ▼                    ▼
  STATE_     tech-lead    Phase A-D    PROGRESS.md
  MACHINE.md code-reviewer 분류        실시간 업데이트
```

## 사용법

| 명령 | 설명 |
|------|------|
| `/refactor {파일}` | 특정 파일 리팩토링 |
| `/refactor analyze` | 리팩토링 대상 분석 |
| `/refactor plan` | 리팩토링 계획 수립 |

---

## 실행 단계 (v2.0)

### Step 1: 조사 (Research)

**자동 수행 항목:**
```yaml
수행:
  - 대상 파일 전체 읽기
  - 버그/이슈 목록 작성
  - 코드 품질 이슈 식별
  - CLAUDE.md 규칙 위반 체크

출력: STATE_MACHINE.md (또는 ANALYSIS.md)
  - 버그 목록 (우선순위별)
  - 수정 코드 예시
  - 영향 범위 분석
```

**예시 (PassSchedule):**
```markdown
## 버그 목록

| # | 버그 | 심각도 | 위치 |
|---|------|:------:|------|
| 1 | isAtStowPosition() 누락 | P0 | L2751 |
| 2 | detailId 미전달 | P0 | L3001 |
| 3 | catch(Exception) | P3 | 13개 |
```

### Step 2: 전문가 검토 (Expert Review)

**호출 에이전트:**
```yaml
병렬 호출:
  - tech-lead: 전체 방향 검토
  - code-reviewer: 코드 품질 검토
  - algorithm-expert: 알고리즘 검토 (필요시)
  - architect: 아키텍처 검토 (필요시)

피드백 수집:
  - 우선순위 조정
  - 누락 이슈 추가
  - 구현 방향 제안
```

### Step 3: 계획 수립 (PLAN.md)

**문서 생성:**
```markdown
# {대상} 리팩토링 실행 계획

## 작업 범위

| Phase | 내용 | 버그 수 | 우선순위 |
|:-----:|------|:------:|:--------:|
| A | Critical 버그 | N | P0 |
| B | High 버그 | N | P1 |
| C | Medium 버그 | N | P2 |
| D | 코드 품질 | N | P3 |

## Phase A: Critical (P0)

### A1. {버그명}
| 항목 | 내용 |
|------|------|
| **위치** | L1234-1250 |
| **문제** | {문제 설명} |
| **해결** | {해결 방법} |

## 실행 순서

Phase A (병렬) → 빌드 → Phase B → 빌드 → ...
```

### Step 4: 사용자 승인

**확인 항목:**
- Phase 분류 동의
- 우선순위 확인
- 실행 순서 확인

### Step 5: 단계별 실행

**원칙:**
```yaml
구현:
  - Phase 단위로 진행
  - 각 Phase 완료 후 빌드 검증
  - PROGRESS.md 실시간 업데이트

진행 상황 표시:
  Phase A [✅✅✅] 3/3 완료!
  Phase B [✅⬜⬜] 1/3 진행중
```

### Step 6: 완료 (/done)

**자동 수행:**
- 빌드 검증
- CHANGELOG 업데이트
- 일일 로그 업데이트
- 커밋 생성

---

## 문서 구조 (자동 생성)

```
docs/work/active/{Feature}/
├── README.md              ← 마스터 (현황 + 링크)
├── STATE_MACHINE.md       ← 분석 결과 (버그 목록)
├── PLAN.md                ← 실행 계획 (Phase 분류)
├── IMPLEMENTATION_PROGRESS.md  ← 진행 상황
└── TEST_SCENARIOS.md      ← 테스트 시나리오 (선택)
```

---

## 리팩토링 대상 (ACS 프로젝트)

### Frontend 대형 파일
| 파일 | 줄수 | 분리 방향 |
|------|------|----------|
| PassSchedulePage.vue | 4,838 | Table, Chart, Controls |
| EphemerisDesignationPage.vue | 4,340 | PositionView, Info, Selector |
| icdStore.ts | 2,971 | WebSocket, Parser, State |

### Backend 대형 파일
| 파일 | 줄수 | 분리 방향 |
|------|------|----------|
| EphemerisService.kt | 4,986 | Calculator, Loader, Converter |
| PassScheduleService.kt | 3,400 | ✅ 상태머신 리팩토링 완료 |
| ICDService.kt | 2,788 | Parser, Sender, Handler |

---

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `tech-lead` | 리팩토링 방향 결정 | 대규모 변경 시 |
| `fe-expert` | Frontend 리팩토링 | Vue/TS 파일 |
| `be-expert` | Backend 리팩토링 | Kotlin 파일 |
| `code-reviewer` | 품질 검증 | 완료 후 |
| `architect` | 구조 검토 | 아키텍처 변경 시 |

## 사용 예시

### 예시 1: Vue 페이지 분리
```
User: "/refactor PassSchedulePage.vue"

→ refactor 워크플로우:

[분석]
  - 4,838줄, 3개 주요 영역 식별
  - Table: 1,200줄
  - Chart: 1,500줄
  - Controls: 800줄

[계획]
  1. PassScheduleTable.vue 추출
  2. PassScheduleChart.vue 추출
  3. PassScheduleControls.vue 추출

[실행]
  fe-expert 호출 → 순차 추출

[검증]
  빌드 성공, 기능 정상

[완료]
  /done → 커밋
```

### 예시 2: Store 분리
```
User: "/refactor icdStore.ts"

→ refactor 워크플로우:

[분석]
  - 2,971줄
  - WebSocket 연결: 500줄
  - 데이터 파싱: 800줄
  - 상태 관리: 1,600줄

[계획]
  1. icdWebSocket.ts 추출
  2. icdParser.ts 추출
  3. icdStore.ts 정리

[실행]
  fe-expert 호출
```

## 주의사항

### ✅ 권장
- 작은 단위로 분리
- 각 단계 후 빌드 확인
- 기존 동작 유지

### ❌ 금지
- 한 번에 여러 파일 변경
- 기능 변경과 동시 진행
- 테스트 없이 대규모 변경

## 리팩토링 패턴

### 컴포넌트 추출 (Vue)
```typescript
// Before: 한 파일에 모든 로직
// PassSchedulePage.vue (4,838줄)

// After: 분리된 컴포넌트
// PassSchedulePage.vue (1,500줄)
// components/PassScheduleTable.vue (1,200줄)
// components/PassScheduleChart.vue (1,500줄)
// components/PassScheduleControls.vue (800줄)
```

### Store 분리 (Pinia)
```typescript
// Before
// stores/icdStore.ts (2,971줄)

// After
// stores/icd/icdStore.ts (메인 스토어)
// stores/icd/icdWebSocket.ts (WebSocket 로직)
// stores/icd/icdParser.ts (파싱 로직)
```

---

## 검증된 사례: PassSchedule 상태머신

> 이 워크플로우로 PassSchedule 리팩토링 88% 완료

**진행 결과:**
```
Phase A (Critical)  [✅✅✅✅] 4/4 완료!
Phase B (High)      [✅✅✅✅✅✅] 6/6 완료!
Phase C (Medium)    [✅⬜⬜] 1/3 (C2-C3 보류)
Phase D (Low)       [✅✅✅✅] 4/4 완료!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total               15/17 (88%)
```

**산출 문서:**
- [STATE_MACHINE.md](../../docs/work/active/Architecture_Refactoring/passschedule/STATE_MACHINE.md) - 버그 목록
- [PLAN.md](../../docs/work/active/Architecture_Refactoring/passschedule/PLAN.md) - 실행 계획
- [IMPLEMENTATION_PROGRESS.md](../../docs/work/active/Architecture_Refactoring/passschedule/IMPLEMENTATION_PROGRESS.md) - 진행 상황
- [TEST_SCENARIOS.md](../../docs/work/active/Architecture_Refactoring/passschedule/TEST_SCENARIOS.md) - 테스트 30개

---

**스킬 버전:** 2.0.0
**작성일:** 2026-01-20
**변경:** PassSchedule 검증 워크플로우 반영, 문서 구조 표준화
**호환:** ACS 프로젝트 전용
