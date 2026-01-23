---
name: done
description: 작업 마무리 및 문서화 스킬. 빌드 검증, 계층적 문서화, 아카이브 감지, 커밋. "완료", "done", "마무리", "커밋" 키워드에 반응.
model: opus
---

# Done - 작업 완료 스킬

## 역할

작업 완료 시 **계층적 문서화 + 진행 상황 추적**을 보장합니다.

**핵심 원칙:**
- 빌드 검증 후 문서화
- 상위 문서에 요약 + 하위 문서 링크
- CURRENT_STATUS.md 업데이트 (세션 간 컨텍스트)
- 완료된 작업 아카이브 제안

---

## 워크플로우

```
[0. 빌드검증] → [1. 변경분석] → [2. 문서화] → [3. 상태업데이트] → [4. 아카이브검토] → [5. 커밋]
      │              │              │              │                   │              │
  실패시 중단    git diff      CHANGELOG →   CURRENT_STATUS     완료 감지 →      모든 변경
                             logs → README   업데이트          제안              포함
```

---

## 체크리스트 (모든 단계 완료 필수)

```
□ Step 0: 빌드 검증
  □ {빌드 명령어 실행}
  □ 실패 시 /done 중단

□ Step 1: 변경 분석
  □ git status / git diff
  □ 영향 영역 파악
  □ 관련 작업 폴더 식별

□ Step 2: 계층적 문서화
  □ CHANGELOG.md 업데이트 (요약 + 링크)
  □ docs/logs/YYYY-MM-DD.md 업데이트
  □ 작업 폴더 README.md 업데이트 (있으면)

□ Step 3: 상태 업데이트
  □ CURRENT_STATUS.md 업데이트

□ Step 4: 아카이브 검토
  □ 완료된 작업 감지 (진행률 100% 또는 "✅ 완료")
  □ 아카이브 여부 질문

□ Step 5: 커밋 생성
```

---

## Step 0: 빌드 검증

```bash
# 프로젝트 빌드 명령어 실행
{빌드 명령어}
```

**실패 시:**
```
❌ 빌드 실패. /done을 중단합니다.
먼저 빌드 오류를 수정해주세요.
```

---

## Step 2: 계층적 문서화

### 문서 계층 구조

```
CHANGELOG.md                         ← 1단계: 전체 변경 이력
│
├── docs/logs/YYYY-MM-DD.md          ← 2단계: 일일 로그
│
└── docs/work/active/{Feature}/
    └── README.md                    ← 3단계: 작업별 상세
```

### CHANGELOG.md 형식

```markdown
## [YYYY-MM-DD] - {작업명}

### Added/Changed/Fixed
- {1-2줄 요약}

### Why
- {왜 이 작업을 했는가}

### Details
- [{상세 문서}](docs/work/active/{Feature}/README.md)
```

### 일일 로그 형식

```markdown
# YYYY-MM-DD 작업 로그

## 완료된 작업

### {작업명}
- **유형**: Feature / Bugfix / Refactor
- **영향**: {영향 범위}
- **요약**: {1-2문장}

### Why
{왜 이 작업을 했는가}

### 상세
- [README.md](../work/active/{Feature}/README.md)

---
이전: [YYYY-MM-DD](YYYY-MM-DD.md)
```

---

## Step 3: CURRENT_STATUS.md 업데이트

세션 간 컨텍스트 유지용. 다음 세션에서 "CURRENT_STATUS.md 읽고 이어서 해줘"로 빠른 복귀.

**업데이트 항목:**
- 진행 중 작업 → 완료로 이동
- 최근 완료 테이블에 커밋 정보 추가
- 다음 작업 후보 업데이트

---

## Step 4: 아카이브 검토

### 완료 감지 조건 (하나라도 만족 시)

```yaml
조건 1: PROGRESS.md에 "진행률: 100%" 포함
조건 2: README.md에 "상태: ✅ 완료" 포함
조건 3: README.md에 "Status: Completed" 포함
```

### 아카이브 제안

```
📦 아카이브 후보 발견

| 폴더 | 상태 |
|------|------|
| {Feature_Name} | 진행률 100% |

아카이브할까요? [y/n]
```

### 승인 시 실행

```bash
git mv docs/work/active/{Feature} docs/work/archive/{Feature}
```

---

## Step 5: 커밋 생성

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

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 실행 예시

```
사용자: "/done"

→ 워크플로우 시작:

[Step 0: 빌드 검증]
  $ {빌드 명령어}
  ✅ 빌드 성공

[Step 1: 변경 분석]
  📊 변경된 파일: 5개
  📊 관련 작업: docs/work/active/{Feature}/

[Step 2: 계층적 문서화]
  📝 CHANGELOG.md 업데이트
  📝 docs/logs/2026-01-22.md 업데이트
  📝 {Feature}/README.md 업데이트

[Step 3: 상태 업데이트]
  📝 CURRENT_STATUS.md 업데이트

[Step 4: 아카이브 검토]
  📦 완료된 작업 감지: {Feature} (100%)
  → 아카이브할까요? [y/n]

[Step 5: 커밋]
  feat({scope}): {subject}
```

---

## 옵션

| 옵션 | 설명 |
|------|------|
| `--no-commit` | 문서만 정리, 커밋 안 함 |
| `--skip-build` | 빌드 검증 생략 (비권장) |

---

**스킬 버전:** 2.0.0
**기반:** ACS 프로젝트 검증 체계
