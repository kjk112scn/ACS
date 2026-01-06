---
name: completing-work
description: 작업 마무리 및 문서화 스킬. 변경사항 분석, 문서 자동 업데이트, 폴더 이동, 커밋 생성. "완료", "done", "마무리", "커밋", "작업 끝" 키워드에 반응.
---

# Completing Work - 작업 마무리 스킬

## 역할

작업 완료 시 자동으로 문서를 정리하고 코드와 문서의 동기화를 보장합니다.

**핵심 가치:**
- 변경사항 자동 분석
- 문서 자동 업데이트
- Why 기록 자동화
- 일관된 완료 처리

## 워크플로우

```
[1. 변경 분석] → [2. 문서 업데이트] → [3. 폴더 이동] → [4. 동기화] → [5. 로그 기록] → [6. 커밋]
       │              │                │              │            │              │
   git diff      IMPLEMENTATION     active/ →      /sync        daily/        모든 변경
    분석          .md 생성        completed/      호출          기록          포함 커밋
```

**중요:** `/sync`는 커밋 **전에** 실행되어 모든 문서 변경이 단일 커밋에 포함됩니다.

## 실행 단계

### Step 1: 변경사항 분석

```bash
# 변경된 파일 목록
git status

# 상세 변경 내용
git diff --stat

# 변경 파일별 분석
git diff --name-only
```

**분석 항목:**
```yaml
변경 분석:
  - 수정된 파일 수
  - 추가된 파일 수
  - 삭제된 파일 수
  - 영향 받는 영역 (Backend/Frontend/Algorithm)
  - 관련 문서 식별
```

### Step 2: 문서 자동 업데이트

#### 2.1 IMPLEMENTATION.md 생성

```markdown
# {기능명/버그명} 구현 결과

## 개요

**완료일**: YYYY-MM-DD
**작업 유형**: Feature / Bugfix / Enhancement
**상태**: ✅ 완료

## 구현 내용

### 변경 파일 목록

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| {파일1} | 수정/추가/삭제 | {변경 내용} |
| {파일2} | | |

### 주요 변경사항

#### Backend
```kotlin
// 주요 변경 코드 또는 설명
```

#### Frontend
```typescript
// 주요 변경 코드 또는 설명
```

## Why 기록

### 왜 이렇게 구현했는가
{설계 의도 및 결정 이유}

### 대안과 비교
| 대안 | 채택 여부 | 이유 |
|------|----------|------|
| | | |

## 테스트 결과

- ✅ 빌드 성공
- ✅ 단위 테스트 통과
- ✅ 기능 검증 완료

## 후속 작업 (해당시)

- [ ] 관련 문서 업데이트
- [ ] 추가 테스트 필요
- [ ] 성능 모니터링

## 관련 문서

- [DESIGN.md](DESIGN.md) - 원본 설계
- ADR-XXX - 관련 결정 기록
```

#### 2.2 CHANGELOG.md 업데이트

```markdown
## [날짜] - {기능명}

### Added
- {새로 추가된 기능}

### Changed
- {변경된 기능}

### Fixed
- {수정된 버그}

### Why
- {변경 이유}
```

#### 2.3 concepts/ 문서 업데이트 (해당시)

```yaml
자동 업데이트 대상:
  - 새 Algorithm 추가 → concepts/algorithms/ 업데이트
  - API 변경 → docs/api/ 업데이트
  - 아키텍처 변경 → concepts/architecture/ 업데이트
```

### Step 3: 폴더 이동

```yaml
Feature 완료:
  FROM: docs/features/active/{기능명}/
  TO:   docs/features/completed/{기능명}/

Bugfix 완료:
  FROM: docs/bugfixes/active/{버그명}/
  TO:   docs/bugfixes/completed/{버그명}/

이동 후 정리:
  - PROGRESS.md 삭제 (IMPLEMENTATION.md로 대체)
  - 불필요한 임시 파일 삭제
```

### Step 4: /sync 문서 동기화

**커밋 전에 실행하여 모든 문서 변경을 포함**

```yaml
수행:
  - 코드↔문서 일치 확인
  - 불일치 항목 자동 수정
  - PROJECT_STATUS.md 업데이트
  - concepts/ 문서 업데이트 (해당시)
```

### Step 5: 일일 로그 기록

**파일:** `docs/daily/YYYY-MM-DD.md`

```markdown
# 2026-01-06 작업 로그

## 완료된 작업

### {기능명/버그명}
- **유형**: Feature / Bugfix
- **변경 파일**: N개
- **영향 범위**: Backend, Frontend

### 변경 요약
{1-2문장 요약}

### Why
{왜 이 작업을 했는가}

---

이전 로그: [2026-01-05](2026-01-05.md)
```

### Step 6: 커밋 생성

```yaml
커밋 메시지 형식:
  <type>(<scope>): <subject>

  <body>

  <footer>

예시:
  feat(passschedule): 차트 줌 기능 추가

  - Quasar Q-Chart 확대/축소 옵션 활성화
  - 줌 레벨 저장 기능 추가

  Closes #123
```

**타입 분류:**
| 타입 | 설명 |
|------|------|
| feat | 새 기능 |
| fix | 버그 수정 |
| docs | 문서 변경 |
| style | 포맷팅 |
| refactor | 리팩토링 |
| perf | 성능 개선 |
| test | 테스트 |
| chore | 빌드/설정 |


## 참조 파일

- **문서 동기화:** [../sync/SKILL.md](../sync/SKILL.md)
- **기능 개발:** [../feature/SKILL.md](../feature/SKILL.md)
- **버그 수정:** [../bugfix/SKILL.md](../bugfix/SKILL.md)

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `project-manager` | 문서 구조 관리 | 폴더 이동 |
| `doc-syncer` | 문서 동기화 | /sync 호출 |
| `code-reviewer` | 변경 검토 | 선택적 |

## 자동 감지 항목

### 문서 업데이트 필요 감지

```yaml
Backend 변경 시:
  - *Controller.kt 추가 → api/ 문서 업데이트 필요
  - *Service.kt 변경 → 해당 concepts/ 업데이트 필요
  - algorithm/*.kt 변경 → concepts/algorithms/ 업데이트 필요

Frontend 변경 시:
  - *Page.vue 추가 → UI_Architecture.md 업데이트 필요
  - *Store.ts 변경 → Store_Architecture.md 업데이트 필요
```

### ADR 생성 권장 감지

```yaml
트리거:
  - 새 라이브러리 import 추가
  - 아키텍처 패턴 변경
  - API 인터페이스 변경

출력:
  "ADR 생성을 권장합니다: {결정 내용}"
```

## 사용 예시

### 예시 1: Feature 완료

```
사용자: "/done" 또는 "이제 완료야"

→ /done 워크플로우 시작:

[변경 분석]
  - 수정: 3개 파일
  - 영역: Frontend (PassSchedulePage.vue, passScheduleStore.ts)
  - 관련 문서: docs/features/active/PassSchedule_Chart_Zoom/

[문서 생성]
  - IMPLEMENTATION.md 생성
  - CHANGELOG.md 업데이트

[폴더 이동]
  active/PassSchedule_Chart_Zoom/ → completed/PassSchedule_Chart_Zoom/

[로그 기록]
  docs/daily/2026-01-06.md 업데이트

[동기화]
  /sync 호출 → PROJECT_STATUS.md, concepts/ 업데이트

[커밋]
  feat(passschedule): 차트 줌 기능 구현
  (코드 + 모든 문서 변경 포함)
```

### 예시 2: Bugfix 완료

```
사용자: "버그 수정 끝났어"

→ /done 워크플로우:

[분석] 변경 파일: 2개 (Frontend)

[문서]
  - IMPLEMENTATION.md 생성 (수정 결과)
  - FIX.md에 "구현 완료" 표시

[이동]
  bugfixes/active/Chart_Performance/ → completed/Chart_Performance/

[커밋]
  fix(passschedule): 차트 렌더링 성능 개선
```

### 예시 3: 문서만 수정

```
사용자: "문서 업데이트 완료"

→ /done 워크플로우:

[분석] 변경 파일: docs/ 내 파일만

[로그]
  docs/daily/2026-01-06.md 기록

[커밋]
  docs: SYSTEM_OVERVIEW.md 업데이트
```

## 트러블슈팅

### Q: 관련 feature/bugfix 폴더가 없어요
A: 수동으로 IMPLEMENTATION.md만 생성하고 daily/ 로그에 기록.

### Q: 커밋하고 싶지 않아요
A: `/done --no-commit` 옵션 사용 (문서 정리만 수행)

### Q: 여러 작업을 한번에 완료하고 싶어요
A: 각 작업별로 /done 개별 실행 권장. 커밋은 마지막에 한번.

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-06
**호환:** ACS 프로젝트 전용
