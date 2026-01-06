# Claude Code Configuration

> ACS 프로젝트 Claude Code 설정 및 확장

## 구조

```
.claude/
├── README.md           # 이 파일
├── agents/             # 서브에이전트 정의
├── skills/             # 스킬 정의
├── templates/          # 문서 템플릿
└── settings.json       # Claude Code 설정 (있는 경우)
```

## 스킬 (Skills)

스킬은 특정 워크플로우를 자동화하는 기능입니다.

| 스킬 | 파일 | 역할 |
|------|------|------|
| `/sync` | `skills/sync/SKILL.md` | 코드↔문서 동기화 |
| `/feature` | `skills/feature/SKILL.md` | 신규 기능 개발 |
| `/bugfix` | `skills/bugfix/SKILL.md` | 버그 수정 |
| `/done` | `skills/done/SKILL.md` | 작업 마무리 |
| `/adr` | `skills/adr/SKILL.md` | ADR 생성 |
| `/plan` | `skills/plan/SKILL.md` | 작업 계획 |
| `/status` | `skills/status/SKILL.md` | 현황 보고 |
| `/docs` | `skills/docs/SKILL.md` | 코드 분석 기반 문서화 |

### 스킬 구조

각 스킬 폴더는 다음 구조를 따릅니다:

```
skills/{스킬명}/
├── SKILL.md        # 메인 스킬 정의 (YAML frontmatter)
├── *_RULES.md      # 상세 규칙 (선택)
└── *.md            # 참조 문서 (선택)
```

### 스킬 발견 (Discovery)

스킬은 다음 키워드로 자동 발견됩니다:

- `/sync` → "동기화", "sync", "문서 업데이트"
- `/feature` → "기능 추가", "새 기능", "feature"
- `/bugfix` → "버그 수정", "에러 해결", "bugfix"
- `/done` → "완료", "done", "마무리"
- `/adr` → "결정 기록", "adr", "왜"
- `/plan` → "계획", "plan", "설계"
- `/status` → "현황", "status", "상태"
- `/docs` → "문서화", "docs", "문서 작성", "코드 분석해서 문서"

## 에이전트 (Agents)

에이전트는 특정 역할에 특화된 AI 어시스턴트입니다.

### 조율 에이전트

| 에이전트 | 역할 |
|---------|------|
| `tech-lead` | 기술 총괄, 에이전트 조율 |

### 설계 에이전트

| 에이전트 | 역할 |
|---------|------|
| `architect` | 설계 문서, ADR |
| `project-manager` | 작업 계획, 문서 관리 |

### 개발 에이전트

| 에이전트 | 역할 |
|---------|------|
| `backend-dev` | Kotlin/Spring 개발 |
| `frontend-dev` | Vue/TypeScript 개발 |
| `algorithm-expert` | Orekit 알고리즘 |
| `fullstack-helper` | 풀스택 통합 |

### 품질 에이전트

| 에이전트 | 역할 |
|---------|------|
| `code-reviewer` | 코드 리뷰 |
| `test-expert` | 테스트 |
| `debugger` | 디버깅 |
| `refactorer` | 리팩토링 |

### 문서 에이전트

| 에이전트 | 역할 |
|---------|------|
| `doc-syncer` | 문서 동기화 |

### 에이전트 형식

각 에이전트 파일은 YAML frontmatter를 포함합니다:

```yaml
---
name: agent-name
description: 에이전트 설명. 키워드 포함.
tools: Read, Grep, Glob, Edit, Write, Bash
model: sonnet
---
```

## 템플릿 (Templates)

문서 템플릿들입니다:

| 템플릿 | 용도 |
|--------|------|
| `ADR_TEMPLATE.md` | 아키텍처 결정 기록 |
| `DESIGN_TEMPLATE.md` | 기능 설계 문서 |
| `FEATURE_README_TEMPLATE.md` | 기능 개요 |
| `BUGFIX_README_TEMPLATE.md` | 버그 정보 |
| `ANALYSIS_TEMPLATE.md` | 버그 원인 분석 |
| `DAILY_LOG_TEMPLATE.md` | 일일 작업 로그 |

## 참고 자료

- [Claude Code Skills Best Practices](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices)
- [Claude Code Subagents](https://code.claude.com/docs/en/sub-agents)
- [ACS 개발 라이프사이클 설계](../docs/references/development/ACS_Development_Lifecycle_System.md)
