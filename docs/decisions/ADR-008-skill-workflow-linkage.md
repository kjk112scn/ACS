# ADR-008: 스킬 연계 워크플로우 설계

**상태**: 승인됨
**날짜**: 2026-01-26
**의사결정자**: architect, doc-syncer, tech-lead

## 컨텍스트

`/review` 스킬로 코드 분석 후 발견된 이슈를 `/bugfix`, `/refactor` 등으로 해결할 때:

1. **이력 추적 부재**: 어떤 Review에서 시작해서 어떻게 해결되었는지 추적 불가
2. **폴더 구조 혼란**: Phase별 하위 폴더 vs 플랫 구조 기준 없음
3. **연계 메커니즘 없음**: 스킬 간 데이터 전달 표준 없음

## 결정

### 1. Review ID 체계 도입

```yaml
형식: #R{NNN}-{심각도}{번호}
예시: #R001-C1, #R001-H2

심각도 코드:
  C: Critical (즉시)
  H: High (당일)
  M: Medium (이번 주)
  L: Low (백로그)
  F: Feature (기능 제안)
```

### 2. 하이브리드 폴더 구조

```
docs/work/active/{Issue_Name}/
├── README.md           # 현재 상태 + 워크플로우 요약
├── PROGRESS.md         # Task System
└── phases/             # 스킬 3개+ 연계 시만 사용
    ├── 01_review.md
    ├── 02_bugfix.md
    └── 03_refactor.md
```

### 3. Origin 추적 메커니즘

```markdown
## 버그 수정

**Origin**: #R001-C1
**Review**: [01_review.md](phases/01_review.md)
```

### 4. WORKFLOW.md (선택적)

- 스킬 3개 이상 연계 시 자동 생성
- 전체 타임라인 + Phase 전환 기록

## 대안 검토

| 대안 | 장점 | 단점 | 결정 |
|------|------|------|:----:|
| Phase 번호 하위 폴더 (`01_review/`) | 명확한 분리 | depth 증가, 검색 불편 | ❌ |
| 완전 플랫 구조 | 단순함 | 복잡한 연계 시 혼란 | △ |
| **하이브리드** | 유연성 | 판단 기준 필요 | ✅ |

## 결과

### 적용 파일

- `.claude/rules/workflow-linkage.md` (신규)
- `CLAUDE.md` (업데이트)
- `.claude/skills/review/SKILL.md` (업데이트 예정)
- `.claude/skills/bugfix/SKILL.md` (업데이트 예정)

### 기대 효과

1. **이력 추적**: Review → Bugfix → Done 전체 흐름 문서화
2. **일관성**: 폴더 구조 기준 명확화
3. **자동화**: 스킬 실행 시 origin 자동 기록

## 참조

- architect 검토: 하이브리드 구조 권장
- doc-syncer 검토: WORKFLOW.md 자동화 포인트 제안
- tech-lead 검토: ID 체계 및 연계 규칙 설계