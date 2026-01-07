# ACS Documentation System

> ACS(Antenna Control System) 프로젝트 문서 관리 시스템

## 문서 구조

```
docs/
├── concepts/           # 개념 문서 (영구 보존)
│   ├── architecture/   # 시스템 아키텍처
│   ├── algorithms/     # 알고리즘 설계
│   ├── protocols/      # 통신 프로토콜
│   └── domain/         # 도메인 지식
│
├── decisions/          # ADR (Architecture Decision Records)
│   └── ADR-NNN-*.md    # 아키텍처 결정 기록
│
├── features/           # 기능 개발 문서
│   ├── active/         # 진행 중
│   └── completed/      # 완료
│
├── bugfixes/           # 버그 수정 문서
│   ├── active/         # 진행 중
│   └── completed/      # 완료
│
├── documentation/      # 문서화 작업 (/docs 스킬)
│   ├── active/         # 진행 중인 문서화
│   └── completed/      # 완료된 문서화
│
├── guides/             # 가이드 문서
│   ├── development/    # 개발 가이드
│   ├── operations/     # 운영 가이드
│   └── user/           # 사용자 가이드
│
├── daily/              # 일일 작업 로그
│   └── YYYY-MM-DD.md
│
├── status/             # 프로젝트 현황
│   ├── PROJECT_STATUS.md
│   ├── CODE_METRICS.md
│   └── TECHNICAL_DEBT.md
│
└── references/         # 참조 문서 (레거시)
    └── ...
```

## 스킬 명령어 (12개)

| 스킬 | 역할 | 버전 |
|------|------|------|
| `/feature` | 신규 기능 개발 워크플로우 | 1.x |
| `/bugfix` | 버그 수정 워크플로우 | 1.x |
| `/done` | 작업 마무리 + 문서화 | 1.x |
| `/sync` | 코드↔문서 동기화, 구조 점검 | 2.0 |
| `/adr` | 아키텍처 결정 기록 | 1.x |
| `/plan` | 작업 계획 수립 | 1.x |
| `/status` | 프로젝트 현황 보고 | 1.x |
| `/docs` | 코드 분석 기반 협의형 문서화 | 1.x |
| `/health` | 빌드, 타입체크, 기술부채 점검 | 1.x |
| `/guide` | 에이전트/스킬 사용법 빠른 안내 | 1.x |
| **`/migrate`** | **마이그레이션 관리 (Feature Flag, Canary)** | **2.0** |
| **`/api-sync`** | **API 자동 동기화 (OpenAPI -> TypeScript)** | **2.0** |

## 핵심 원칙

### 1. 코드가 Single Source of Truth

- 문서는 코드를 반영하는 View
- 코드 변경 시 문서 자동 동기화

### 2. Why 기록 필수

- 모든 결정에 이유 기록
- ADR로 아키텍처 결정 추적
- 일일 로그로 작업 이유 기록

### 3. 자동화 우선

- `/sync`로 자동 동기화
- `/done`으로 자동 문서화
- 템플릿 기반 일관성 유지

## 문서 마커

### 자동 생성 영역

```markdown
<!-- AUTO-GENERATED: START -->
...자동 생성 내용...
<!-- AUTO-GENERATED: END -->
```

### 수동 작성 영역 (보존)

```markdown
<!-- MANUAL: 섹션명 -->
...보존 내용...
<!-- MANUAL END -->
```

## 빠른 시작

### 새 기능 개발

```
1. /feature 실행
2. docs/features/active/{기능명}/ 폴더 생성됨
3. DESIGN.md 작성
4. 구현
5. /done 실행 → completed/로 이동
```

### 버그 수정

```
1. /bugfix 실행
2. docs/bugfixes/active/{버그명}/ 폴더 생성됨
3. ANALYSIS.md로 원인 분석
4. FIX.md로 수정 계획
5. 구현 + 검증
6. /done 실행 → completed/로 이동
```

### 문서 동기화

```
1. /sync 실행
2. 코드↔문서 차이 분석
3. 업데이트 제안 확인
4. 승인 시 자동 적용
```

### 코드 기반 문서화

```
1. /docs {대상} 실행 (예: /docs 시스템 아키텍처)
2. docs/documentation/active/{주제}/ 폴더 생성됨
3. 코드 자동 분석 → ANALYSIS.md
4. 협의 진행 → DISCUSSION.md에 Q&A 기록
5. 초안 생성 → DRAFT.md
6. 승인 후 concepts/에 최종 문서 적용
```

## 템플릿 위치

모든 템플릿은 `.claude/templates/`에 있습니다:

- `ADR_TEMPLATE.md`
- `DESIGN_TEMPLATE.md`
- `FEATURE_README_TEMPLATE.md`
- `BUGFIX_README_TEMPLATE.md`
- `ANALYSIS_TEMPLATE.md`
- `DAILY_LOG_TEMPLATE.md`

## 관련 문서

- [개발 라이프사이클 시스템 설계](references/development/ACS_Development_Lifecycle_System.md)
- [프로젝트 현황](status/PROJECT_STATUS.md)
- [ADR 목록](decisions/README.md)
- [일일 작업 로그](daily/README.md)
- [에이전트/스킬 사용 가이드](guides/AGENT_SKILL_USAGE_GUIDE.md)
- [참조 문서](references/README.md)
