# Task Management Integration 설계 문서

## 1. 설계 의도

### Why (왜 이렇게 설계했는가)

Claude Code v2.1.16에서 새로운 Task Management System이 도입되었으나,
현재 ACS 스킬들은 마크다운 문서 기반으로 작업을 추적합니다.

**문제점:**
- 체크박스 수동 관리 → 누락 가능
- 병렬 실행 불가 → 비효율
- 의존성 암묵적 → 순서 오류 가능

**해결책:**
- 문서 기반 유지 (기존 워크플로우 호환)
- 파싱 레이어 추가 → Task System 연동
- 별도 tasks.json 불필요

### 대안 분석

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|:----:|
| tasks.json 별도 관리 | 구조화됨 | 이중 관리, 동기화 필요 | ❌ |
| PROGRESS.md 확장 | 기존 호환, 단일 소스 | 파싱 로직 필요 | ✅ |
| 완전 새 시스템 | 최적화 가능 | 기존 문서 폐기 | ❌ |

---

## 2. 아키텍처

### 2.1 전체 흐름

```
┌─────────────────────────────────────────────────────────────┐
│                      스킬 실행 (/feature, /bugfix, /plan)     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    PROGRESS.md 생성/업데이트                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ ### Phase 1: 준비 [parallel: false]                 │    │
│  │ - [ ] #T001 요구사항 분석                            │    │
│  │ - [ ] #T002 설계 문서 [depends: T001]               │    │
│  │                                                     │    │
│  │ ### Phase 2: 구현 [parallel: true]                  │    │
│  │ - [ ] #T003 Backend [depends: T002] @be-expert      │    │
│  │ - [ ] #T004 Frontend [depends: T002] @fe-expert     │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Task Parser                              │
│  - 체크박스 파싱                                              │
│  - 의존성 그래프 구축                                         │
│  - 병렬 실행 가능 Task 식별                                   │
│  - 에이전트 태그 추출 (@be-expert, @fe-expert)                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   TodoWrite 자동 호출                        │
│  - Task 상태 동기화 (pending, in_progress, completed)        │
│  - 진행률 계산                                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Background Agent Executor (선택적)              │
│  - 병렬 가능 Task 감지                                       │
│  - Task 도구 + run_in_background: true                      │
│  - 완료 시 PROGRESS.md 자동 업데이트                         │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 PROGRESS.md 확장 문법

```markdown
# {기능명} 진행 상황

<!-- @task-system: enabled -->
<!-- @auto-sync: true -->

## 진행률: 0/5 (0%)

## Tasks

### Phase 1: 준비 [parallel: false]
- [ ] #T001 요구사항 분석
- [ ] #T002 설계 문서 작성 [depends: T001]

### Phase 2: 구현 [parallel: true]
- [ ] #T003 Backend 구현 [depends: T002] @be-expert
- [ ] #T004 Frontend 구현 [depends: T002] @fe-expert

### Phase 3: 검증 [parallel: false]
- [ ] #T005 테스트 실행 [depends: T003, T004] @test-expert

## 실행 로그

### 2026-01-26
- T001 시작
```

### 2.3 태그 정의

| 태그 | 위치 | 의미 | 예시 |
|------|------|------|------|
| `#T001` | 체크박스 앞 | Task ID | `- [ ] #T001 설계` |
| `[depends: T001]` | 체크박스 뒤 | 의존성 | `[depends: T001, T002]` |
| `[parallel: true]` | Phase 제목 뒤 | 병렬 실행 가능 | `### Phase 2 [parallel: true]` |
| `@agent-name` | 체크박스 뒤 | 담당 에이전트 | `@be-expert` |
| `<!-- @task-system: enabled -->` | 문서 상단 | Task System 활성화 | - |
| `<!-- @auto-sync: true -->` | 문서 상단 | 자동 동기화 | - |

---

## 3. 구현 상세

### 3.1 Task Parser 로직 (스킬 내 임베드)

```yaml
파싱 단계:
  1. PROGRESS.md 읽기
  2. @task-system: enabled 확인
  3. 체크박스 라인 추출
  4. 각 라인에서:
     - Task ID (#T001) 추출
     - 상태 ([ ] / [x]) 확인
     - 의존성 ([depends: ...]) 파싱
     - 에이전트 (@agent) 추출
  5. 의존성 그래프 구축
  6. 실행 가능 Task 식별

실행 가능 조건:
  - 상태가 pending ([ ])
  - 모든 의존 Task가 completed ([x])
  - Phase가 parallel: true면 동시 실행 가능
```

### 3.2 TodoWrite 연동

```yaml
PROGRESS.md → TodoWrite 매핑:
  - [ ] #T001 설계 → { content: "#T001 설계", status: "pending" }
  - [x] #T001 설계 → { content: "#T001 설계", status: "completed" }
  - 현재 실행 중 → { status: "in_progress" }

동기화 시점:
  - 스킬 시작 시
  - Task 완료 시
  - 사용자가 체크박스 수동 변경 시 (감지 가능하면)
```

### 3.3 백그라운드 에이전트 실행

```yaml
조건:
  - Phase가 [parallel: true]
  - 의존성 충족된 Task가 2개 이상
  - @agent 태그가 있음

실행:
  Task 도구 호출:
    - subagent_type: @agent 값
    - run_in_background: true
    - prompt: Task 내용 + 컨텍스트

완료 처리:
  - PROGRESS.md 체크박스 업데이트 ([x])
  - TodoWrite 상태 업데이트
  - 다음 의존 Task 확인 → 자동 시작
```

---

## 4. 스킬 수정 계획

### 4.1 feature/SKILL.md 수정

```diff
### Step 5: PROGRESS.md 작성

+ <!-- @task-system: enabled -->
+ <!-- @auto-sync: true -->

# {기능명} 진행 상황

- ## 진행률: 0%
+ ## 진행률: 0/N (0%)

- ### Phase 1: 준비
+ ### Phase 1: 준비 [parallel: false]
- - [ ] 요구사항 분석
+ - [ ] #T001 요구사항 분석
- - [ ] 설계 문서 작성
+ - [ ] #T002 설계 문서 작성 [depends: T001]

+ ### Step 5.1: Task System 초기화 (신규)
+
+ PROGRESS.md 생성 후 자동 실행:
+ 1. Task 파싱
+ 2. TodoWrite 호출
+ 3. 병렬 가능 Task 안내
```

### 4.2 bugfix/SKILL.md 수정

```diff
### Step 4: 문서 정리 (FIX.md)

+ FIX.md에 Task 태그 추가:
+ - [ ] #T001 원인 분석 @debugger
+ - [ ] #T002 수정 구현 [depends: T001]
+ - [ ] #T003 회귀 테스트 [depends: T002] @test-expert
```

### 4.3 plan/SKILL.md 수정

```diff
### Step 5: 계획 출력

+ 계획 생성 시 Task ID 자동 부여:
+ - [ ] #T001 요구사항 확정
+ - [ ] #T002 기존 코드 분석 [depends: T001]
```

---

## 5. 새 파일: TASK_SYSTEM.md

`.claude/rules/task-system.md` 생성:

```markdown
# Task System 규칙

## 활성화 조건

PROGRESS.md 상단에 다음 주석이 있으면 활성화:
<!-- @task-system: enabled -->

## Task ID 규칙

- 형식: #T + 3자리 숫자 (예: #T001)
- 문서 내 유일해야 함
- 순차적으로 부여

## 의존성 규칙

- 형식: [depends: T001] 또는 [depends: T001, T002]
- 순환 의존성 금지
- 없으면 독립 실행 가능

## 병렬 실행 규칙

- Phase 레벨에서 [parallel: true] 선언
- 의존성 충족된 Task만 병렬 실행
- 에이전트 태그 필수 (@agent-name)

## 자동 동기화

<!-- @auto-sync: true --> 있으면:
- 체크박스 변경 시 TodoWrite 자동 업데이트
- 진행률 자동 계산
```

---

## 6. 테스트 계획

### 6.1 단위 테스트

- [ ] Task ID 파싱 정확성
- [ ] 의존성 그래프 구축
- [ ] 병렬 실행 가능 Task 식별
- [ ] 순환 의존성 감지

### 6.2 통합 테스트

- [ ] /feature 실행 → Task System 연동
- [ ] /bugfix 실행 → Task System 연동
- [ ] 백그라운드 에이전트 실행 및 완료 처리

### 6.3 E2E 테스트

- [ ] 전체 워크플로우: feature → 구현 → done

---

## 7. DevKit 반영 계획

| 파일 | 분류 | 설명 |
|------|------|------|
| `.claude/rules/task-system.md` | [CORE] | Task System 규칙 |
| 스킬 템플릿 (feature, bugfix, plan) | [CORE] | Task 태그 포함 버전 |
| `docs/templates/PROGRESS.md` | [CORE] | 확장된 템플릿 |

---

**설계 버전:** 1.0.0
**작성일:** 2026-01-26
