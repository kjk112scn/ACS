---
name: feature
description: 신규 기능 개발 워크플로우. 폴더 생성, 문서화, Phase 분류, 구현 관리. "기능 추가", "새 기능", "feature" 키워드에 반응.
model: opus
---

# Feature - 신규 기능 개발 스킬

## 역할

신규 기능 개발의 전체 라이프사이클을 관리합니다.

**핵심 가치:**
- 작업 폴더 + 문서 자동 생성
- Phase 분류로 우선순위 관리 (대형 작업)
- 진행 상황 추적 (PROGRESS.md)

---

## 워크플로우

```
[1. 분석] → [2. 폴더생성] → [3. 문서작성] → [4. 구현] → [5. 완료]
     │           │              │            │           │
  요청 분석   active/      README +       단계별      /done
  RFC 필요?   {Feature}/   PROGRESS      구현        호출
```

---

## Step 1: 요청 분석

```yaml
출력:
  - 기능명 (PascalCase_Underscore)
  - 기능 설명
  - 영향 범위 (Frontend / Backend / Both)
  - 복잡도 (단순 / 중간 / 대형)
```

**복잡도 판단:**
| 조건 | 복잡도 | Phase 분류 |
|------|--------|:----------:|
| 단일 파일 수정 | 단순 | 불필요 |
| 2-4개 파일, 단일 영역 | 중간 | 선택 |
| 5개+ 파일, 여러 영역 | 대형 | 필요 |

---

## Step 2: 폴더 생성

```bash
mkdir -p docs/work/active/{Feature_Name}/
```

**폴더명 규칙:** `{Category}_{Name}`
- `Feature_` - 새 기능
- `Bugfix_` - 버그 수정
- `Refactor_` - 리팩토링
- `Optimize_` - 성능 최적화

---

## Step 3: 문서 작성

### README.md (필수)

```markdown
# {기능명}

## 개요

**목적**: {한 줄 설명}
**요청일**: YYYY-MM-DD
**상태**: 🚧 진행 중

## 요구사항

- [ ] 요구사항 1
- [ ] 요구사항 2

## 영향 범위

| 영역 | 파일/컴포넌트 |
|------|--------------|
| Frontend | |
| Backend | |

## 관련 문서

- [PROGRESS.md](PROGRESS.md)
- [DESIGN.md](DESIGN.md) (있으면)
```

### PROGRESS.md (필수)

```markdown
# {기능명} 진행 상황

## 진행률: 0%

## 작업 체크리스트

### Phase 1: 준비
- [ ] 요구사항 분석
- [ ] 설계 검토

### Phase 2: 구현
- [ ] {구현 항목 1}
- [ ] {구현 항목 2}

### Phase 3: 검증
- [ ] 빌드 확인
- [ ] 테스트

## 일일 로그

### YYYY-MM-DD
- 작업 시작
```

### DESIGN.md (복잡한 작업만)

```markdown
# {기능명} 설계 문서

## Why (왜 이렇게 설계했는가)

{설계 결정의 이유}

## 대안 분석

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|:----:|
| A | | | ❌ |
| B | | | ✅ |

## 구현 계획

{단계별 구현 계획}
```

---

## Step 4: 구현

```yaml
진행 중:
  - PROGRESS.md 체크리스트 업데이트
  - 진행률 갱신 (예: 30%, 60%, 100%)

Phase 분류 (대형 작업):
  - Phase A: Critical (즉시 필요)
  - Phase B: High (중요)
  - Phase C: Medium (개선)
  - Phase D: Low (나중에)
```

---

## Step 5: 완료

**자동 호출:** `/done` 스킬

- 빌드 검증
- 계층적 문서화
- CURRENT_STATUS.md 업데이트
- 아카이브 제안 (100% 완료 시)

---

## 실행 예시

```
사용자: "로그인 기능 추가해줘"

→ /feature 워크플로우 시작:

[분석]
  기능명: Feature_Login
  영향: Frontend + Backend
  복잡도: 중간

[폴더 생성]
  docs/work/active/Feature_Login/
  ├── README.md
  └── PROGRESS.md

[문서 작성]
  README.md - 요구사항, 영향 범위 정리
  PROGRESS.md - 체크리스트 생성

[구현 시작]
  Phase 1: LoginForm 컴포넌트
  Phase 2: useAuth composable
  Phase 3: API 연동

[완료]
  /done 호출 → 문서화 + 커밋
```

---

## 관련 스킬

- `/plan` - 복잡한 계획 수립
- `/done` - 작업 완료
- `/archive` - 완료 후 아카이브

---

**스킬 버전:** 2.0.0
