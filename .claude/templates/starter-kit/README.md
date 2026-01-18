# Claude Code 스타터 키트

> ACS 프로젝트에서 검증된 AI 협업 구조

## 빠른 시작

### 1. 새 프로젝트에 복사

```bash
# 전체 복사
cp -r .claude/templates/starter-kit/* 새프로젝트/

# 또는 수동 복사
mkdir -p 새프로젝트/.claude/skills/{guide,done,health,plan}
mkdir -p 새프로젝트/docs/architecture/context
```

### 2. CLAUDE.md 수정

```markdown
# {프로젝트명}  ← 수정

## 기술 스택
| Frontend | Vue 3 |  ← 수정

## IMPORTANT - 핵심 규칙
| 규칙 | 설명 |  ← 추가
```

### 3. 컨텍스트 문서 작성 (선택)

복잡한 프로젝트라면:

```bash
# 도메인 문서 추가
echo "# {도메인} 설명" > docs/architecture/context/domain/{도메인}.md
```

## 포함된 파일

```
starter-kit/
├── CLAUDE.md                           # 프로젝트 설정 템플릿
├── README.md                           # 이 파일
├── .claude/
│   ├── skills/                         # 슬래시 명령어
│   │   ├── guide/SKILL.md              # /guide - 사용법
│   │   ├── done/SKILL.md               # /done - 커밋
│   │   ├── health/SKILL.md             # /health - 점검
│   │   └── plan/SKILL.md               # /plan - 계획
│   └── agents/                         # 전문가 에이전트
│       ├── tech-lead.md                # 기술 방향 결정
│       ├── fe-expert.md                # 프론트엔드 (Vue 3)
│       ├── be-expert.md                # 백엔드 (Kotlin)
│       ├── db-expert.md                # 데이터베이스 (PostgreSQL)
│       ├── test-expert.md              # 테스트 작성/실행
│       ├── debugger.md                 # 버그 분석/수정
│       ├── refactorer.md               # 리팩토링
│       ├── code-reviewer.md            # 코드 품질 검증
│       └── doc-syncer.md               # 문서 동기화
└── docs/
    └── architecture/
        └── context/
            └── _INDEX.md               # 컨텍스트 맵
```

## 핵심 기능

### 협업 대화 패턴 (/guide)

```
1. 모든 작업 전 AI가 먼저 확인
2. AI가 의견 제시 (추천 + 대안)
3. 합의 후 실행
```

### 스킬 목록 (사용자 호출)

| 스킬 | 용도 |
|------|------|
| `/guide` | 사용법 안내 |
| `/plan` | 계획 수립 |
| `/done` | 작업 완료 + 커밋 |
| `/health` | 빌드/품질 점검 |

### 에이전트 목록 (기본값: Vue + Kotlin + PostgreSQL)

| 에이전트 | 역할 | 모델 |
|---------|------|:----:|
| `tech-lead` | 기술 방향 결정 | Opus |
| `fe-expert` | Vue 3 / Pinia / Quasar | Opus |
| `be-expert` | Kotlin / Spring WebFlux | Opus |
| `db-expert` | PostgreSQL / Flyway | Opus |
| `test-expert` | 테스트 작성/실행 | Opus |
| `debugger` | 버그 분석/수정 | Opus |
| `refactorer` | 리팩토링/분리 | Opus |
| `code-reviewer` | 코드 품질 검증 | Opus |
| `doc-syncer` | 문서 동기화 | Sonnet |

## 커스터마이징

### 프로젝트별 스킬 추가

```bash
mkdir -p .claude/skills/새스킬/
cat > .claude/skills/새스킬/SKILL.md << 'EOF'
---
name: 새스킬
description: 설명
---

# 스킬 내용
EOF
```

### 도메인 전문가 에이전트 추가

```bash
mkdir -p .claude/agents/
cat > .claude/agents/domain-expert.md << 'EOF'
# Domain Expert

{도메인} 전문가

## 역할
- {역할 설명}

## 참조 문서
- [도메인](../../docs/architecture/context/domain/)
EOF
```

## 출처

ACS (Antenna Control System) 프로젝트에서 추출
- 위성 추적 안테나 제어 시스템
- Vue 3 + Kotlin + Spring WebFlux

---

**버전**: 1.0.0
**날짜**: 2026-01-17