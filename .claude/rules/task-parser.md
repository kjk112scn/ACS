# Task Parser 로직

> 스킬 실행 시 PROGRESS.md를 파싱하여 Task System과 연동하는 로직

## 1. 파싱 단계

### Step 1: 활성화 확인

```markdown
<!-- @task-system: enabled -->
```

이 태그가 없으면 Task System 비활성화 (기존 방식 유지)

### Step 2: Task 추출

```regex
Pattern: - \[([ x])\] #(T\d{3}) (.+?)(?:\[depends: ([^\]]+)\])?(?:\s+@(\S+))?$
```

**캡처 그룹:**
1. 체크 상태 (` ` or `x`)
2. Task ID (`T001`)
3. Task 설명
4. 의존성 (선택)
5. 에이전트 (선택)

### Step 3: Phase 추출

```regex
Pattern: ### (.+?) \[parallel: (true|false)\]
```

**캡처 그룹:**
1. Phase 이름
2. 병렬 실행 여부

---

## 2. TodoWrite 동기화

### 변환 규칙

```yaml
PROGRESS.md → TodoWrite:
  - [ ] #T001 ... → { content: "#T001 ...", status: "pending" }
  - [x] #T001 ... → { content: "#T001 ...", status: "completed" }
  현재 실행 중     → { content: "#T001 ...", status: "in_progress" }
```

### 동기화 시점

| 이벤트 | 동작 |
|--------|------|
| 스킬 시작 | PROGRESS.md 읽기 → TodoWrite 초기화 |
| Task 시작 | status → `in_progress` |
| Task 완료 | status → `completed`, 체크박스 `[x]` |
| 스킬 종료 | 최종 상태 PROGRESS.md에 저장 |

---

## 3. 의존성 처리

### 실행 가능 여부 판단

```
isExecutable(task):
  if task.depends is empty:
    return true

  for each depId in task.depends:
    dep = findTask(depId)
    if dep.status != "completed":
      return false

  return true
```

### 순환 의존성 검출

```
detectCycle(taskId, visited = []):
  if taskId in visited:
    return true  # 순환 발견!

  visited.add(taskId)
  task = findTask(taskId)

  for each depId in task.depends:
    if detectCycle(depId, visited):
      return true

  return false
```

---

## 4. 병렬 실행

### Phase [parallel: true] 처리

```
executePhase(phase):
  if phase.parallel:
    executableTasks = phase.tasks.filter(t => isExecutable(t))

    for each task in executableTasks:
      # 백그라운드로 실행
      Task tool 호출:
        - subagent_type: task.agent or "general-purpose"
        - run_in_background: true
        - prompt: task.description

    # 완료 대기 및 다음 Task 실행
    while not allCompleted(phase.tasks):
      wait and check
  else:
    # 순차 실행
    for each task in phase.tasks:
      if isExecutable(task):
        execute(task)
```

### 최대 병렬 수

```yaml
권장: 3개 (리소스 고려)
설정: <!-- @max-parallel: 3 -->
```

---

## 5. 에이전트 매핑

### 태그 → subagent_type 변환

| 태그 | subagent_type |
|------|---------------|
| `@be-expert` | `be-expert` (없으면 `general-purpose`) |
| `@fe-expert` | `fe-expert` (없으면 `general-purpose`) |
| `@test-expert` | `test-expert` |
| `@debugger` | `debugger` |
| `@architect` | `architect` |
| `@doc-syncer` | `doc-syncer` |
| `@refactorer` | `refactorer` |
| (없음) | `general-purpose` |

---

## 6. 진행률 계산

### 자동 업데이트

```
updateProgress():
  completed = tasks.count(t => t.status == "completed")
  total = tasks.count()
  percent = (completed / total) * 100

  # PROGRESS.md 업데이트
  "## 진행률: {completed}/{total} ({percent}%)"
```

---

## 7. 스킬별 적용

### /feature 실행 시

```yaml
1. 폴더 생성
2. PROGRESS_TASK_SYSTEM.md 템플릿으로 PROGRESS.md 생성
3. Task ID 자동 부여 (#T001~)
4. TodoWrite 호출하여 초기화
5. Phase별 실행
```

### /bugfix 실행 시

```yaml
1. FIX.md에 Task 섹션 추가
2. 분석 → 수정 → 테스트 의존성 자동 설정
3. 심각도에 따른 우선순위 조정
```

### /plan 실행 시

```yaml
1. 계획 문서에 Task ID 자동 부여
2. WBS 기반 의존성 자동 생성
3. 의존성 그래프 시각화 (선택)
```

---

## 8. 오류 처리

### 일반 오류

| 오류 | 대응 |
|------|------|
| 중복 Task ID | 경고 + 새 ID 자동 생성 |
| 순환 의존성 | 오류 + 관련 Task 표시 |
| 없는 ID 참조 | 경고 + 의존성 무시 |
| 에이전트 없음 | `general-purpose` 대체 |

### 실행 실패

```yaml
Task 실패 시:
  1. status: "in_progress" 유지 (completed 아님)
  2. 실행 로그에 오류 기록
  3. 의존 Task 실행 보류
  4. 사용자에게 알림
```

---

## 9. 확장 태그 (선택)

| 태그 | 설명 | 예시 |
|------|------|------|
| `@priority` | 우선순위 | `@priority:high` |
| `@estimate` | 예상 시간 | `@estimate:30m` |
| `@owner` | 담당자 | `@owner:claude` |

```markdown
- [ ] #T001 긴급 버그 수정 @priority:high @debugger
```

---

**규칙 버전:** 1.0.0
**작성일:** 2026-01-26
**분류:** [CORE] - DevKit 동기화 대상