---
name: completing-work
description: 작업 마무리 및 문서화 스킬. 변경사항 분석, 문서 자동 업데이트, 폴더 이동, 커밋 생성. "완료", "done", "마무리", "커밋", "작업 끝" 키워드에 반응.
model: opus
---

# Completing Work - 작업 마무리 스킬

## 역할

작업 완료 시 **계층적 문서화**를 보장합니다.

**핵심 원칙:**
- **상위 문서에 요약 + 하위 문서 링크** (임의 파일 생성 금지)
- 빌드 검증 후 문서화
- 일관된 구조 유지
- Why 기록 필수

---

## 문서 계층 구조 (필수 준수)

```
CHANGELOG.md                         ← 1단계: 전체 변경 이력 (요약 + 링크)
│
├── docs/logs/YYYY-MM-DD.md          ← 2단계: 일일 로그 (요약 + 링크)
│
└── docs/work/active/{Feature}/
    └── README.md                    ← 3단계: 작업별 마스터 (상세 + 하위 링크)
        ├── {topic}/                 ← 하위 폴더로 분류
        └── legacy/                  ← 완료된 상세 문서
```

**규칙:**
1. 새 파일 생성 최소화 - 기존 README.md 업데이트 우선
2. 같은 레벨에 파일 5개 초과 시 → 하위 폴더로 분류
3. 상위 문서에 요약, 하위 문서에 상세

---

## 워크플로우

```
[0. 빌드검증] → [1. 변경분석] → [2. 계층문서화] → [3. 로그기록] → [4. 커밋]
      │              │              │                │            │
  실패시 중단    git diff      CHANGELOG →       daily/       모든 변경
                               README →           기록        포함
                               하위문서
```

---

## 필수 체크리스트

**모든 단계 완료 확인 - 건너뛰기 금지**

```
□ Step 0: 빌드 검증
  □ Backend: ./gradlew clean build -x test
  □ Frontend: npx vue-tsc --noEmit
  □ 실패 시 /done 중단

□ Step 1: 변경 분석
  □ git status
  □ git diff --stat
  □ 영향 영역 파악

□ Step 2: 계층적 문서화 (3단계)
  □ CHANGELOG.md 업데이트 (1단계)
  □ 일일 로그 업데이트 (2단계)
  □ 작업 폴더 README.md 업데이트 (3단계)

□ Step 3: 커밋 생성
  □ 모든 변경사항 포함
```

---

## Step 0: 빌드 검증 (필수)

```bash
# Backend
cd backend && ./gradlew clean build -x test

# Frontend
cd frontend && npx vue-tsc --noEmit
```

**실패 시:**
```
❌ 빌드 실패. /done을 중단합니다.
먼저 빌드 오류를 수정해주세요.
```

---

## Step 1: 변경 분석

```bash
git status
git diff --stat
```

**출력 형식:**
```
📊 변경 분석:
- Backend: N개 파일
- Frontend: N개 파일
- Docs: N개 파일
- 관련 작업: docs/work/active/{Feature}/
```

---

## Step 2: 계층적 문서화

### 2.1 CHANGELOG.md 업데이트 (1단계 - 최상위)

**위치:** `/CHANGELOG.md`

```markdown
## [YYYY-MM-DD] - {작업명}

### Added/Changed/Fixed
- {1-2줄 요약}

### Why
- {왜 이 작업을 했는가}

### Details
- [{상세 문서}](docs/work/active/{Feature}/README.md)
```

**규칙:**
- 날짜별 역순 (최신이 위)
- 요약만 작성, 상세는 Details 링크로

### 2.2 일일 로그 업데이트 (2단계)

**위치:** `docs/logs/YYYY-MM-DD.md`

```markdown
# YYYY-MM-DD 작업 로그

## 완료된 작업

### {작업명}
- **유형**: Feature / Bugfix / Refactor
- **영향**: Backend, Frontend
- **요약**: {1-2문장}

### Why
{왜 이 작업을 했는가}

### 상세
- [README.md](../work/active/{Feature}/README.md)

---
이전: [YYYY-MM-DD](YYYY-MM-DD.md)
```

### 2.3 작업 폴더 README.md 업데이트 (3단계)

**위치:** `docs/work/active/{Feature}/README.md`

```markdown
# {Feature명}

> **Status**: Active/Completed
> **Updated**: YYYY-MM-DD

## 현황

| 항목 | 상태 | 상세 |
|-----|:----:|------|
| {작업1} | ✅ | [{문서}]({하위경로}) |
| {작업2} | ⬜ | 대기 |

## 폴더 구조

{Feature}/
├── README.md  ← 현재 (마스터)
├── {topic1}/  ← 하위 문서
└── legacy/    ← 완료된 문서

## 관련 문서
- [CHANGELOG.md](../../../CHANGELOG.md)
- [일일 로그](../../logs/)
```

---

## Step 3: 커밋 생성

```yaml
형식: <type>(<scope>): <subject>

타입:
  feat     - 새 기능
  fix      - 버그 수정
  docs     - 문서
  refactor - 리팩토링
  chore    - 빌드/설정
```

```bash
git add .
git commit -m "<type>(<scope>): <subject>

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## 실행 예시

```
사용자: "/done"

→ 워크플로우 시작:

[Step 0: 빌드 검증]
  $ ./gradlew clean build -x test
  ✅ Backend 빌드 성공
  $ npx vue-tsc --noEmit
  ✅ Frontend 타입 체크 성공

[Step 1: 변경 분석]
  📊 Backend: 3개
  📊 Docs: 2개
  📊 관련: docs/work/active/Architecture_Refactoring/

[Step 2: 계층적 문서화]
  📝 CHANGELOG.md 업데이트
  📝 docs/logs/2026-01-18.md 업데이트
  📝 Architecture_Refactoring/README.md 업데이트

[Step 3: 커밋]
  feat(database): DB Integration Phase 6 완료
```

---

## 옵션

| 옵션 | 설명 |
|------|------|
| `--no-commit` | 문서만 정리, 커밋 안 함 |
| `--skip-build` | 빌드 검증 생략 (비권장) |

---

## 폴더 정리 규칙

### 파일이 많아질 때

**Before (나쁜 예):**
```
Feature/
├── README.md
├── PLAN.md
├── TRACKER.md
├── CSS_Plan1.md
├── CSS_Plan2.md
├── FE_Plan.md
└── ...
```

**After (좋은 예):**
```
Feature/
├── README.md      ← 마스터 (현황 + 링크만)
├── css/           ← 주제별 폴더
├── frontend/
└── legacy/        ← 완료된 문서
```

### 작업 완료 시

```yaml
진행 중:
  docs/work/active/{Feature}/

완료 후:
  docs/work/archive/{Feature}/
  (또는 active 내 legacy/ 폴더로)
```

---

## 트러블슈팅

### Q: 문서가 너무 많아요
A: README.md에 요약만, 나머지는 하위 폴더로 정리

### Q: 어디에 무엇을 써야 해요?
A: CHANGELOG=전체요약, 일일로그=오늘작업, README=상세

### Q: 커밋하고 싶지 않아요
A: `/done --no-commit`

---

**버전:** 2.0.0
**수정일:** 2026-01-18
**변경:** 계층적 문서화 구조 강제, 빌드 검증 추가, 폴더 정리 규칙 추가
