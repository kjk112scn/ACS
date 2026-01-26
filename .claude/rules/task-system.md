# Task System 규칙

> Claude Code Task Management System과 문서 기반 워크플로우 통합 규칙

## 1. 개요

스킬 실행 시 PROGRESS.md 문서를 기반으로 Claude Code의 Task Management System과 자동 연동합니다.

**핵심 원칙:**
- 별도 `tasks.json` 불필요 (문서가 Single Source)
- 의존성 명시로 순서 보장
- 병렬 실행으로 효율성 향상

---

## 2. 활성화

PROGRESS.md 상단에 다음 주석이 있으면 Task System 활성화:

```markdown
<!-- @task-system: enabled -->
<!-- @auto-sync: true -->
```

| 태그 | 기본값 | 설명 |
|------|:------:|------|
| `@task-system` | disabled | Task System 활성화 |
| `@auto-sync` | false | 체크박스 변경 시 자동 동기화 |

---

## 3. Task ID 규칙

### 형식

```
#T + 3자리 숫자
```

### 예시

```markdown
- [ ] #T001 요구사항 분석
- [ ] #T002 설계 문서 작성
- [ ] #T010 통합 테스트
```

### 규칙

| 규칙 | 설명 |
|------|------|
| 유일성 | 문서 내 동일 ID 금지 |
| 순차성 | 001 → 002 → 003 순서 권장 |
| 재사용 금지 | 삭제된 ID 재사용 금지 |

---

## 4. 의존성 규칙

### 형식

```markdown
[depends: T001]
[depends: T001, T002, T003]
```

### 예시

```markdown
- [ ] #T001 요구사항 분석
- [ ] #T002 설계 문서 [depends: T001]
- [ ] #T003 구현 [depends: T001, T002]
```

### 규칙

| 규칙 | 설명 |
|------|------|
| 순환 금지 | A→B→A 형태 금지 |
| 존재 확인 | 없는 ID 참조 금지 |
| 빈 의존성 | 태그 없으면 독립 Task |

### 의존성 그래프 예시

```
T001 ─┬─→ T002 ─→ T004
      │
      └─→ T003 ─→ T004
```

---

## 5. 병렬 실행 규칙

### Phase 레벨 선언

```markdown
### Phase 2: 구현 [parallel: true]
- [ ] #T003 Backend 구현 @be-expert
- [ ] #T004 Frontend 구현 @fe-expert
```

### 조건

| 조건 | 설명 |
|------|------|
| Phase 태그 | `[parallel: true]` 필수 |
| 의존성 충족 | 모든 의존 Task 완료 |
| 에이전트 태그 | `@agent-name` 권장 |

### 동작

```
Phase [parallel: true] 진입 시:
  1. 의존성 충족된 Task 모두 수집
  2. 각 Task를 백그라운드로 실행
  3. 완료 시 다음 Task 자동 시작
```

---

## 6. 에이전트 태그

### 형식

```markdown
@agent-name
```

### 사용 가능 에이전트

| 태그 | 에이전트 | 역할 |
|------|---------|------|
| `@be-expert` | be-expert | Backend 구현 |
| `@fe-expert` | fe-expert | Frontend 구현 |
| `@debugger` | debugger | 원인 분석 |
| `@test-expert` | test-expert | 테스트 작성/실행 |
| `@architect` | architect | 설계 |
| `@refactorer` | refactorer | 리팩토링 |
| `@doc-syncer` | doc-syncer | 문서 동기화 |

### 예시

```markdown
- [ ] #T003 Backend API 구현 [depends: T002] @be-expert
- [ ] #T004 Frontend 컴포넌트 [depends: T002] @fe-expert
- [ ] #T005 테스트 [depends: T003, T004] @test-expert
```

---

## 7. 상태 동기화

### 체크박스 ↔ TodoWrite 매핑

| PROGRESS.md | TodoWrite status |
|-------------|------------------|
| `- [ ] #T001` | `pending` |
| `- [x] #T001` | `completed` |
| 현재 실행 중 | `in_progress` |

### 진행률 계산

```markdown
## 진행률: 3/7 (43%)
```

```
진행률 = 완료된 Task 수 / 전체 Task 수 * 100
```

---

## 8. 전체 문법 예시

```markdown
# Feature_Name 진행 상황

<!-- @task-system: enabled -->
<!-- @auto-sync: true -->

## 진행률: 2/6 (33%)

## Tasks

### Phase 1: 준비 [parallel: false]
- [x] #T001 요구사항 분석
- [x] #T002 설계 문서 작성 [depends: T001]

### Phase 2: 구현 [parallel: true]
- [ ] #T003 Backend 구현 [depends: T002] @be-expert
- [ ] #T004 Frontend 구현 [depends: T002] @fe-expert

### Phase 3: 검증 [parallel: false]
- [ ] #T005 테스트 실행 [depends: T003, T004] @test-expert
- [ ] #T006 빌드 검증 [depends: T005]

## 실행 로그

### 2026-01-26
- #T001 완료
- #T002 완료
- #T003, #T004 병렬 실행 시작
```

---

## 9. 스킬 연동

### /feature 실행 시

1. `docs/work/active/{Feature}/PROGRESS.md` 생성
2. Task System 활성화 태그 자동 삽입
3. Phase별 Task ID 자동 부여
4. TodoWrite 자동 호출

### /bugfix 실행 시

1. FIX.md에 Task 섹션 추가
2. 분석 → 수정 → 테스트 의존성 자동 설정

### /plan 실행 시

1. 계획 문서에 Task ID 자동 부여
2. 의존성 그래프 시각화

---

## 10. 주의사항

| 주의 | 설명 |
|------|------|
| ID 중복 | 자동 생성 시 기존 ID 확인 |
| 순환 의존성 | 그래프 검증 후 경고 |
| 수동 편집 | 태그 형식 유지 필수 |
| 병렬 실행 | 리소스 고려 (최대 3개 권장) |

---

**규칙 버전:** 1.0.0
**작성일:** 2026-01-26
**분류:** [CORE] - DevKit 동기화 대상
