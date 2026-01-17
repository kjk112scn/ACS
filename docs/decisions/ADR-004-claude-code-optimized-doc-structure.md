---
번호: ADR-004
제목: Claude Code 최적화 문서 구조
상태: 제안됨
날짜: 2026-01-17
---

# ADR-004: Claude Code 최적화 문서 구조

## 상태

**제안됨** - 검토 대기

## 컨텍스트

### 현재 상황

ACS 프로젝트는 129개의 마크다운 문서가 분산되어 있으며, Claude Code와의 연계에서 다음 문제가 발생:

| 문제 | 현황 | 영향 |
|------|------|------|
| **CLAUDE.md 비대화** | 169줄 (권장 100-200줄) | 매 대화 시 토큰 소모 |
| **문서 중복** | SYSTEM_OVERVIEW vs data-flow (70% 중복) | 불일치 위험 |
| **work/ 폴더 혼란** | legacy/ 내 34개 파일, 분류 불명확 | 탐색 비용 증가 |
| **analysis/ 활용도 저조** | 생성 후 참조 안 됨 | 유지보수 낭비 |
| **계층적 CLAUDE.md 부재** | 루트만 존재 | 하위 폴더 컨텍스트 누락 |

### 모범 사례 기준

1. **CLAUDE.md 100-200줄 유지**: 초과 시 하위 폴더로 분리
2. **계층적 CLAUDE.md**: 루트 -> frontend/ -> backend/
3. **Self-contained 페이지**: 각 문서가 독립적으로 이해 가능
4. **예측 가능한 패턴**: 일관된 구조
5. **llms.txt 표준**: LLM이 읽기 쉬운 형식

### 문서 현황 분석

```
현재 분포 (129개 *.md):
├── docs/ (96개)
│   ├── work/active/ (47개) - 대부분 legacy
│   ├── work/archive/ (19개)
│   ├── architecture/context/ (20개)
│   └── 기타 (10개)
├── .claude/ (45개)
│   ├── agents/ (17개)
│   ├── skills/ (16개)
│   └── templates/ (6개)
└── CLAUDE.md (1개)
```

## 결정

### 1. 개선된 폴더 구조

```
ACS/
├── CLAUDE.md                    # 루트 (80줄 이내)
├── llms.txt                     # LLM 탐색용 인덱스 (신규)
│
├── frontend/
│   ├── CLAUDE.md                # FE 전용 규칙 (신규)
│   └── src/
│
├── backend/
│   ├── CLAUDE.md                # BE 전용 규칙 (신규)
│   └── src/
│
├── .claude/                     # Claude Code 확장 (유지)
│   ├── README.md
│   ├── agents/
│   ├── skills/
│   └── templates/
│
└── docs/
    ├── README.md                # 문서 허브 (간략화)
    │
    ├── core/                    # 핵심 참조 문서 (신규 통합)
    │   ├── SYSTEM.md            # SYSTEM_OVERVIEW 축소판
    │   ├── DATA_FLOW.md         # 데이터 흐름 (중복 제거)
    │   ├── DOMAIN.md            # 도메인 개념 통합
    │   └── CONVENTIONS.md       # 코딩 규칙 + Train/Tilt 등
    │
    ├── api/                     # API 문서 (유지)
    │   └── *.md
    │
    ├── decisions/               # ADR (유지)
    │   └── ADR-*.md
    │
    ├── guides/                  # 가이드 (유지, 정리)
    │   └── *.md
    │
    └── work/                    # 작업 문서 (구조 개선)
        ├── README.md            # 작업 인덱스
        ├── active/              # 진행 중
        │   └── {작업명}/
        │       ├── README.md    # 작업 개요 (필수)
        │       └── *.md         # 상세 문서
        └── archive/             # 완료됨
            └── {작업명}/
                └── SUMMARY.md   # 결과 요약만 보관
```

### 2. 계층적 CLAUDE.md 전략

```
┌─────────────────────────────────────────────────────┐
│  CLAUDE.md (루트)                                   │
│  - 프로젝트 개요 (10줄)                              │
│  - 기술 스택 (10줄)                                  │
│  - 빌드 명령어 (10줄)                                │
│  - 핵심 주의사항 (15줄)                              │
│  - 하위 CLAUDE.md 참조 포인터 (5줄)                  │
│  - 작업 가이드 블록 (20줄)                           │
│  ─────────────────────────────────────────          │
│  총 70-80줄                                         │
└──────────────────────┬──────────────────────────────┘
                       │
       ┌───────────────┼───────────────┐
       │               │               │
       ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ frontend/    │ │ backend/     │ │ docs/core/   │
│ CLAUDE.md    │ │ CLAUDE.md    │ │ CONVENTIONS  │
│              │ │              │ │ .md          │
│ - Vue 규칙   │ │ - Kotlin규칙 │ │              │
│ - Pinia패턴  │ │ - WebFlux    │ │ - Train/Tilt │
│ - 컴포넌트   │ │ - 계층 구조  │ │ - 각도/시간  │
│   구조       │ │ - 예외처리   │ │ - 네이밍     │
│              │ │              │ │              │
│ (50줄)       │ │ (50줄)       │ │ (80줄)       │
└──────────────┘ └──────────────┘ └──────────────┘
```

#### 참조 흐름

1. Claude Code가 루트 CLAUDE.md 자동 로드
2. 작업 영역에 따라 해당 하위 CLAUDE.md 추가 참조
3. 도메인 규칙 필요 시 docs/core/CONVENTIONS.md 참조

### 3. 문서 간 연결 정책

#### 상호 참조 규칙

| 규칙 | 설명 | 예시 |
|------|------|------|
| **단방향 참조** | 상위 → 하위만 허용 | CLAUDE.md → frontend/CLAUDE.md |
| **Self-contained 원칙** | 각 문서는 독립 이해 가능 | 필수 배경 포함 |
| **명시적 링크** | 상대 경로로 명확히 | `[참조](../core/SYSTEM.md)` |
| **링크 최소화** | 필수 링크만 유지 | 3개 이하 권장 |

#### 참조 계층

```
Level 0: CLAUDE.md (진입점)
    │
Level 1: frontend/CLAUDE.md, backend/CLAUDE.md
    │
Level 2: docs/core/*.md (핵심 참조)
    │
Level 3: docs/api/*.md, docs/guides/*.md (상세)
    │
Level 4: docs/work/active/*/ (작업 문서)
```

### 4. 가드레일 설계 (이상 행동 방지)

#### 4.1 CLAUDE.md 가드레일

```markdown
## 금지 사항 (PROHIBITED)

- 이 파일에 100줄 이상 추가 금지
- 코드 예제 5줄 이상 금지 (docs/ 참조로 대체)
- 에이전트/스킬 상세 설명 금지 (.claude/ 참조)
- 아키텍처 다이어그램 금지 (docs/core/SYSTEM.md 참조)
```

#### 4.2 문서 생성 가드레일

| 규칙 | 적용 위치 | 검증 방법 |
|------|----------|----------|
| README.md 필수 | docs/work/active/{작업}/ | /feature, /bugfix 스킬 검사 |
| 10일 이상 미변경 시 archive 권고 | docs/work/active/ | /sync 스킬 경고 |
| legacy/ 접근 경고 | docs/work/*/legacy/ | "DEPRECATED" 헤더 추가 |
| 중복 문서 생성 금지 | 전체 | /docs 스킬에서 유사 문서 검색 |

#### 4.3 스킬 가드레일 (추가 구현 필요)

```yaml
# .claude/skills/sync/SKILL.md 에 추가
guardrails:
  - name: doc_count_check
    condition: "docs/work/active/**/*.md > 50"
    action: "경고: active 문서가 50개 초과. 정리 필요."

  - name: duplicate_check
    condition: "유사도 > 70% 문서 존재"
    action: "경고: {파일1}과 {파일2} 중복 의심"

  - name: orphan_check
    condition: "docs/**/*.md 중 어디서도 참조 안 됨"
    action: "경고: {파일} 고아 문서. 삭제 또는 연결 필요."
```

### 5. 문서 통합 및 제거 계획

#### 즉시 통합 (Phase 1)

| 기존 문서 | 통합 대상 | 이유 |
|----------|----------|------|
| `SYSTEM_OVERVIEW.md` | `docs/core/SYSTEM.md` | 축소 + 핵심만 |
| `context/architecture/data-flow.md` | `docs/core/DATA_FLOW.md` | 중복 제거 |
| `context/domain/*.md` (4개) | `docs/core/DOMAIN.md` | 통합 |
| `guides/Coding_Standards.md` | `docs/core/CONVENTIONS.md` | 통합 |

#### legacy 폴더 정리 (Phase 2)

```
docs/work/active/Architecture_Refactoring/legacy/ (34개)
→ 완료된 RFC 및 분석 문서
→ 조치: 전체 삭제 또는 zip 압축 보관
```

#### analysis/ 재설계 (Phase 3)

```
현재: docs/architecture/context/analysis/ (10개)
  - 생성 후 거의 참조 안 됨
  - synthesis/*.md만 유용

조치:
  1. synthesis/gap-report.md → docs/work/active/로 이동 (작업용)
  2. synthesis/refactoring-hints.md → docs/work/active/로 이동
  3. 나머지 → 삭제 또는 .archive/ 이동
```

## 대안

### 대안 A: Monorepo 스타일 분리

```
apps/
├── frontend/
│   ├── CLAUDE.md
│   └── docs/
└── backend/
    ├── CLAUDE.md
    └── docs/
```

**장점**: 완전 분리, 독립 배포 가능
**단점**: 기존 구조와 충돌, 마이그레이션 비용 높음
**선택**: 기각 - ACS는 단일 제품, 분리 불필요

### 대안 B: docs/ 최소화 (코드 주석 중심)

```
docs/
├── README.md
├── decisions/
└── work/  # 작업 문서만
```

**장점**: 문서 유지 부담 감소
**단점**: 복잡한 도메인 지식 전달 어려움
**선택**: 기각 - 위성 추적 도메인은 문서 필수

### 대안 C: 현재 구조 유지 + 정리만

**장점**: 변경 최소화
**단점**: 근본 문제 해결 안 됨 (중복, 계층 부재)
**선택**: 기각 - 장기적 기술 부채 증가

## 결과

### 긍정적 영향

| 항목 | 기대 효과 |
|------|----------|
| 토큰 절감 | CLAUDE.md 169줄 → 80줄 (53% 감소) |
| 탐색 시간 | 문서 수 129개 → ~60개 (54% 감소) |
| 중복 제거 | context/ 20개 → core/ 4개 |
| 컨텍스트 정확도 | 계층적 CLAUDE.md로 영역별 규칙 적용 |

### 부정적 영향

| 항목 | 완화 방안 |
|------|----------|
| 마이그레이션 비용 | Phase별 점진적 적용 |
| 기존 링크 깨짐 | /sync 스킬로 자동 수정 |
| 학습 곡선 | 새 구조 가이드 문서 작성 |

### 구현 우선순위

```
Phase 1 (즉시, 2시간)
├── 1. frontend/CLAUDE.md 생성
├── 2. backend/CLAUDE.md 생성
└── 3. 루트 CLAUDE.md 축소

Phase 2 (단기, 4시간)
├── 1. docs/core/ 폴더 생성 및 통합
├── 2. llms.txt 생성
└── 3. context/ → core/ 마이그레이션

Phase 3 (중기, 1일)
├── 1. work/active/*/legacy/ 정리
├── 2. 가드레일 스킬 구현
└── 3. 깨진 링크 수정

Phase 4 (장기, 지속)
├── 1. /sync 가드레일 검증
└── 2. 월간 문서 건강도 점검
```

## 참조

- [Claude Code Skills Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)
- [llms.txt 표준](https://llmstxt.org/)
- [ADR Process](https://adr.github.io/)
- [기존 문서 구조](./../README.md)

---

**작성자**: architect 에이전트
**검토자**: -
**승인자**: -
