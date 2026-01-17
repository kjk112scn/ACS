# Claude Code 프로젝트 템플릿

> ACS 프로젝트에서 검증된 구조를 다른 프로젝트에 적용하기 위한 가이드

## 폴더 구조

```
새프로젝트/
├── CLAUDE.md                      # 필수 - 프로젝트 설정 (아래 템플릿 참조)
├── .claude/
│   ├── skills/                    # 슬래시 명령어
│   │   ├── guide/SKILL.md         # /guide - 사용법 안내
│   │   ├── done/SKILL.md          # /done - 작업 완료
│   │   ├── health/SKILL.md        # /health - 빌드/품질 점검
│   │   └── plan/SKILL.md          # /plan - 계획 수립
│   ├── agents/                    # 도메인 전문가 (선택)
│   │   └── {domain}-expert.md     # 프로젝트별 전문가
│   └── templates/                 # 이 템플릿들
│       └── PROJECT_TEMPLATE.md
└── docs/
    ├── architecture/
    │   └── context/               # 도메인 지식 저장소
    │       ├── _INDEX.md          # 컨텍스트 맵
    │       ├── domain/            # 도메인 개념
    │       └── architecture/      # 아키텍처 설명
    └── work/
        ├── active/                # 진행중 작업
        └── archive/               # 완료된 작업
```

---

## CLAUDE.md 템플릿

```markdown
# {프로젝트명}

{한 줄 설명}

## 기술 스택

| 영역 | 기술 |
|-----|-----|
| Frontend | {예: Vue 3 + TypeScript} |
| Backend | {예: Kotlin + Spring Boot} |
| Database | {예: PostgreSQL} |

## 빌드 명령어

\`\`\`bash
# Frontend
cd frontend && npm run dev
cd frontend && npm run build

# Backend
cd backend && ./gradlew bootRun
cd backend && ./gradlew build
\`\`\`

## IMPORTANT - 핵심 규칙

| 규칙 | 설명 |
|------|------|
| {규칙1} | {설명} |
| {규칙2} | {설명} |

## PROHIBITED - 금지 사항

- 하드코딩 비밀번호/키
- console.log 남기기 (production)
- 광범위 catch (Exception)
- {프로젝트별 금지 사항}

## 컨텍스트 로딩

| 작업 | 참조 문서 |
|------|----------|
| {도메인A} 작업 | `@docs/architecture/context/domain/{도메인A}.md` |
| 아키텍처 이해 | `@docs/architecture/context/architecture/overview.md` |

## 작업 방식

모든 작업 완료 후 아래 가이드 표시:

\`\`\`
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 다음에 쓸 수 있는 명령어
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔧 워크플로우
  /plan      계획 수립
  /done      작업 완료 + 커밋
  /health    빌드/품질 점검

💡 /guide 로 상세 안내
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
\`\`\`
```

---

## 필수 스킬 (최소 구성)

### 1. /guide - 사용법 안내

`.claude/skills/guide/SKILL.md`:

```markdown
---
name: guide
description: 사용법 안내
model: haiku
---

# Guide - 사용법 안내

## 스킬 목록

| 스킬 | 용도 |
|------|------|
| /guide | 이 안내 |
| /plan | 계획 수립 |
| /done | 작업 완료 |
| /health | 빌드/품질 점검 |

## 협업 대화 패턴

### 기본 원칙

1. **모든 작업 전 확인** - AI가 먼저 이해한 내용 확인
2. **양방향 의견 교환** - AI도 의견 제시, 사용자도 피드백
3. **단계별 진행** - Research → Plan → Implement

### 작업 시작 시 AI가 확인하는 것

- 이해한 내용 요약
- 확인 필요 사항 질문
- 추천 방향 제시

### AI 의견 제시 패턴

- 추천 + 이유
- 대안 비교 테이블
- "어떻게 생각하세요?" 로 마무리

### 단계별 흐름

1. 요청 → 2. 확인 → 3. 계획 → 4. 합의 → 5. 실행 → 6. 완료
```

### 2. /done - 작업 완료

`.claude/skills/done/SKILL.md`:

```markdown
---
name: done
description: 작업 완료 + 커밋
---

# Done - 작업 완료

## 워크플로우

1. 변경사항 확인 (git status, git diff)
2. 빌드 확인
3. 커밋 메시지 생성
4. 커밋 실행

## 커밋 메시지 형식

\`\`\`
{type}({scope}): {subject}

{body}

Co-Authored-By: Claude <noreply@anthropic.com>
\`\`\`

type: feat, fix, refactor, docs, chore
```

### 3. /health - 빌드/품질 점검

`.claude/skills/health/SKILL.md`:

```markdown
---
name: health
description: 빌드/품질 점검
---

# Health - 프로젝트 상태 점검

## 체크 항목

1. 빌드 성공 여부
2. 타입 체크 (TypeScript)
3. 린트 에러
4. 테스트 통과

## 출력 형식

| 항목 | 상태 |
|------|:----:|
| 빌드 | ✅/❌ |
| 타입 | ✅/❌ |
| 린트 | ✅/❌ |
| 테스트 | ✅/❌ |
```

---

## 컨텍스트 문서 템플릿

`docs/architecture/context/_INDEX.md`:

```markdown
# Context Reference Index

> 도메인 지식 저장소

## 구조

\`\`\`
docs/architecture/context/
├── _INDEX.md              # 이 파일
├── domain/                # 도메인 지식
│   └── {도메인}.md
└── architecture/          # 아키텍처
    └── overview.md
\`\`\`

## 빠른 참조

- **핵심 도메인**: {요약}
- **아키텍처**: {요약}
```

---

## 새 프로젝트 시작 체크리스트

- [ ] CLAUDE.md 생성 (위 템플릿 기반)
- [ ] .claude/skills/guide/SKILL.md 생성
- [ ] .claude/skills/done/SKILL.md 생성
- [ ] .claude/skills/health/SKILL.md 생성
- [ ] docs/architecture/context/_INDEX.md 생성
- [ ] docs/work/active/ 폴더 생성
- [ ] (선택) 도메인 전문가 에이전트 추가

---

**버전**: 1.0.0
**출처**: ACS 프로젝트 (2026-01-17)
