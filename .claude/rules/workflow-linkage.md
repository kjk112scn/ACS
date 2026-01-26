# Workflow Linkage 규칙

> 스킬 간 연계 및 이력 추적 규칙 (2026-01-26 제정)

## 1. Review ID 체계

### 형식

```yaml
Review ID: #R{NNN}                    # 예: #R001
Issue ID:  #R{NNN}-{심각도}{번호}      # 예: #R001-C1

심각도 코드:
  C: Critical (즉시 수정)
  H: High (당일)
  M: Medium (이번 주)
  L: Low (백로그)
  F: Feature (기능 제안)
```

### 예시

```markdown
## 발견된 이슈

| ID | 심각도 | 설명 | 상태 |
|----|:------:|------|:----:|
| #R001-C1 | Critical | 상태 전이 원자성 | ⏳ |
| #R001-H1 | High | 큐 동기화 불일치 | ⏳ |
| #R001-M1 | Medium | console.log 잔존 | ⏳ |
```

---

## 2. 폴더 구조 (하이브리드)

### 기본 구조

```
docs/work/active/
└── {Issue_Name}/                    # 고정 이름 (Phase 접두사 ❌)
    ├── README.md                    # 현재 상태 + 워크플로우 요약
    ├── PROGRESS.md                  # Task System (Phase 섹션 포함)
    └── phases/                      # 상세 문서 (선택적)
        ├── 01_review.md             # /review 결과
        ├── 02_bugfix.md             # /bugfix 기록
        └── 03_refactor.md           # /refactor 기록 (필요시)
```

### 언제 phases/ 사용?

| 조건 | 구조 |
|------|------|
| 스킬 2개 이하 연계 | 플랫 (FIX.md, REVIEW.md 직접) |
| 스킬 3개 이상 연계 | 하이브리드 (phases/ 사용) |
| 복잡한 이력 추적 필요 | 하이브리드 + WORKFLOW.md |

---

## 3. Origin 추적

### 스킬 연계 시 origin 필수

```markdown
## 버그 수정

**Origin**: #R001-C1
**Review**: [01_review.md](phases/01_review.md)
```

### 연계 명령어 형식

```bash
/bugfix #R001-C1        # 특정 이슈 수정
/bugfix origin:#R001    # Review 전체 연계
/refactor origin:#R001  # Review 기반 리팩토링
```

---

## 4. README.md 워크플로우 섹션

### 필수 섹션

```markdown
## 워크플로우

| 단계 | 스킬 | 날짜 | 결과 | 상태 |
|:----:|------|------|------|:----:|
| 1 | /review | 01-26 | #R001 | ✅ |
| 2 | /bugfix | 01-26 | #R001-C1 수정 | 🔄 |
| 3 | /refactor | - | 대기 | ⏳ |

## 이슈 추적

| Origin | Task | 설명 | 상태 |
|--------|------|------|:----:|
| #R001-C1 | #F001 | 상태 전이 원자성 | 🔄 |
| #R001-H1 | #F002 | 큐 동기화 | ⏳ |
```

---

## 5. WORKFLOW.md (선택적)

### 생성 조건

| 조건 | WORKFLOW.md |
|------|:-----------:|
| 스킬 3개 이상 연계 | 자동 생성 |
| 복잡한 의사결정 이력 | 수동 생성 |
| 단순 버그 수정 | 불필요 |

### 구조

```markdown
# {Issue_Name} - 작업 이력

## 타임라인

| # | 일시 | Phase | 스킬 | Origin | 결과 | 상태 |
|:-:|------|-------|------|--------|------|:----:|
| 1 | 01-26 10:30 | Review | /review | - | #R001 | ✅ |
| 2 | 01-26 11:00 | Bugfix | /bugfix | #R001-C1 | FIX.md | 🔄 |

## Phase 전환 기록

### Review → Bugfix (01-26)
- **트리거**: Critical #R001-C1 발견
- **결정**: 즉시 수정 필요
- **이관**: C1, H1, H2
```

---

## 6. /done 완료 처리

### 연계 작업 완료 시

```yaml
/done 실행 시:
  1. origin이 있으면:
     - 원본 REVIEW.md 상태 업데이트
     - "✅ 완료 (2026-01-26)"

  2. 모든 이슈 완료 시:
     - Review 아카이브 제안
     - WORKFLOW.md 최종 정리

  3. 아카이브:
     - docs/work/archive/{YYYY-MM}/{폴더명}/
```

---

## 7. 예외 케이스

### Review 없이 직접 Bugfix

```yaml
허용: Yes
차이점:
  - origin: "direct" 또는 생략
  - ID 체계: #F{NNN} (Review ID 없음)

추후 /review 실행 시:
  - 기존 FIX.md 스캔
  - 중복 이슈 발견 시 연결 제안
```

### 병렬 작업 충돌

```yaml
같은 파일 수정 시:
  경고: "⚠️ #R001-H2는 #R001-H1과 같은 파일 수정"
  권장: 순차 처리 또는 의존성 추가
```

---

**규칙 버전:** 1.0.0
**제정일:** 2026-01-26
**분류:** [CORE] - DevKit 동기화 대상
