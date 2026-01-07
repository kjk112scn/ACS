# Claude Code Configuration

> **버전**: 2.0.0 | **최종 수정**: 2026-01-07
>
> ACS 프로젝트 Claude Code 설정 및 확장

## 빠른 시작

- **처음 사용**: [QUICK_START.md](./QUICK_START.md) - 실전 예시로 빠르게 시작
- **협업 가이드**: [AGENT_COLLABORATION_GUIDE.md](./AGENT_COLLABORATION_GUIDE.md) - 에이전트 조합 방법

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

| 스킬 | 파일 | 역할 | 버전 |
|------|------|------|------|
| `/sync` | `skills/sync/SKILL.md` | 코드↔문서 동기화 | 1.x |
| `/feature` | `skills/feature/SKILL.md` | 신규 기능 개발 | 1.x |
| `/bugfix` | `skills/bugfix/SKILL.md` | 버그 수정 | 1.x |
| `/done` | `skills/done/SKILL.md` | 작업 마무리 | 1.x |
| `/adr` | `skills/adr/SKILL.md` | ADR 생성 | 1.x |
| `/plan` | `skills/plan/SKILL.md` | 작업 계획 | 1.x |
| `/status` | `skills/status/SKILL.md` | 현황 보고 | 1.x |
| `/docs` | `skills/docs/SKILL.md` | 코드 분석 기반 문서화 | 1.x |
| `/health` | `skills/health/SKILL.md` | 프로젝트 건강 상태 점검 | 1.x |
| `/guide` | `skills/guide/SKILL.md` | 에이전트/스킬 사용법 안내 (haiku) | 1.x |
| **`/migrate`** ⭐ | **`skills/migrate/SKILL.md`** | **마이그레이션 관리 (Feature Flag, Canary Release)** | **2.0** |
| **`/api-sync`** ⭐ | **`skills/api-sync/SKILL.md`** | **API 자동 동기화 (OpenAPI → TypeScript)** | **2.0** |

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
- `/health` → "건강", "health", "빌드 점검", "상태 점검"
- `/guide` → "가이드", "guide", "사용법", "어떻게 써"
- `/migrate` → "마이그레이션", "migrate", "배포", "Feature Flag", "Canary"
- `/api-sync` → "API 동기화", "api-sync", "타입 생성", "OpenAPI"

## 에이전트 (Agents)

에이전트는 특정 역할에 특화된 AI 어시스턴트입니다.

### 조율 에이전트

| 에이전트 | 역할 |
|---------|------|
| `tech-lead` | 기술 총괄, 에이전트 조율 |

### 설계 에이전트

| 에이전트 | 역할 | 버전 |
|---------|------|------|
| `architect` | 시스템 설계, ADR | 1.x |
| **`database-architect`** ⭐ | **DB 설계 (ERD, 마이그레이션, 인덱스 최적화)** | **2.0** |
| `project-manager` | 작업 계획, 문서 관리 | 1.x |

### 개발 에이전트

| 에이전트 | 역할 | 버전 |
|---------|------|------|
| `backend-dev` | Kotlin/Spring 개발 | 1.x |
| `frontend-dev` | Vue/TypeScript 개발 | 1.x |
| `algorithm-expert` | Orekit 알고리즘 | 1.x |
| `fullstack-helper` | 풀스택 통합 | 1.x |
| **`api-contract-manager`** ⭐ | **API 계약 관리 (OpenAPI, FE-BE 타입 동기화)** | **2.0** |
| **`design-system-builder`** ⭐ | **디자인 시스템 (Storybook, Design Token, 컴포넌트 문서화)** | **2.0** |

### 품질 에이전트

| 에이전트 | 역할 | 실행 |
|---------|------|------|
| `code-reviewer` | **코드 품질 게이트** - CLAUDE.md 규칙 검증 | **자동** |
| `test-expert` | 테스트 | 수동 |
| `debugger` | 디버깅 | 수동 |
| `refactorer` | 리팩토링 | 수동 |
| `performance-analyzer` | 성능 분석 및 최적화 | 수동 |

> **자동 실행**: `code-reviewer`는 코드 수정/구현 후 자동으로 호출됩니다.
> CLAUDE.md 규칙 위반 시 🔴 Critical로 경고합니다.

### 문서 에이전트 (문서 관리 총괄)

| 에이전트 | 역할 |
|---------|------|
| `doc-syncer` | **문서 관리 총괄** - 동기화, 구조/링크 관리, README 유지 |

> `doc-syncer`가 문서 관련 모든 작업의 책임자입니다.
> `/sync` 스킬 실행 시 자동으로 문서 건강 상태를 점검합니다.

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
- [에이전트/스킬 사용 가이드](../docs/guides/AGENT_SKILL_USAGE_GUIDE.md)
