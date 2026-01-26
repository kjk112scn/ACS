# {기능명} 진행 상황

<!-- @task-system: enabled -->
<!-- @auto-sync: true -->

## 진행률: 0/{total} (0%)

---

## Tasks

### Phase 1: 준비 [parallel: false]
- [ ] #T001 요구사항 분석
- [ ] #T002 기존 코드 파악 [depends: T001]
- [ ] #T003 설계 검토 [depends: T001]

### Phase 2: 구현 [parallel: true]
- [ ] #T004 {구현 항목 1} [depends: T003] @be-expert
- [ ] #T005 {구현 항목 2} [depends: T003] @fe-expert
- [ ] #T006 {구현 항목 3} [depends: T003]

### Phase 3: 검증 [parallel: false]
- [ ] #T007 빌드 확인 [depends: T004, T005, T006]
- [ ] #T008 테스트 실행 [depends: T007] @test-expert
- [ ] #T009 코드 리뷰 [depends: T008]

---

## 태그 설명

| 태그 | 예시 | 설명 |
|------|------|------|
| Task ID | `#T001` | 유니크 식별자 |
| 의존성 | `[depends: T001, T002]` | 선행 Task |
| 병렬 | `[parallel: true]` | Phase 병렬 실행 |
| 에이전트 | `@be-expert` | 담당 에이전트 |

---

## 실행 로그

### YYYY-MM-DD
- #T001 시작
- {진행 내용}

---

## 에이전트 목록

| 태그 | 역할 |
|------|------|
| `@be-expert` | Backend 구현 |
| `@fe-expert` | Frontend 구현 |
| `@test-expert` | 테스트 작성/실행 |
| `@debugger` | 디버깅 |
| `@architect` | 설계 |
| `@doc-syncer` | 문서 동기화 |

---

## 규칙 참조

상세 규칙: `.claude/rules/task-system.md`
