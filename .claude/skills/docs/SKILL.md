---
name: documentation
description: 문서화 스킬. 기존 코드 분석, 협의, 문서 자동 생성. "문서화", "docs", "문서 작성", "문서 정리", "코드 분석해서 문서" 키워드에 반응.
---

# Documentation - 문서화 스킬

## 역할

기존 코드를 분석하고 협의를 통해 체계적인 문서를 생성합니다.

**핵심 가치:**
- 코드 기반 자동 분석
- 협의 내용 기록/추적
- 문서 초안 자동 생성
- 승인 기반 반영

## 워크플로우

```
[1. 영역 선택] → [2. 코드 분석] → [3. 협의] → [4. 초안 작성] → [5. 승인] → [6. 적용]
      │              │              │              │              │           │
   대상 지정      자동 탐색      Q&A 진행     DRAFT.md      확인 받기   실제 반영
```

## 실행 단계

### Step 1: 영역 선택

```yaml
입력: 사용자 요청 분석
유형:
  - 시스템 전체: "시스템 아키텍처 문서화"
  - 특정 모듈: "EphemerisService 문서화"
  - 특정 기능: "모드 시스템 문서화"
  - 데이터 흐름: "API 데이터 흐름 문서화"

출력:
  - 문서화 주제 (영문 PascalCase)
  - 문서화 범위
  - 예상 분석 대상 파일
```

### Step 2: 폴더 및 문서 생성

```bash
# 폴더 생성
mkdir -p docs/documentation/active/{주제명}/

# 문서 생성
touch docs/documentation/active/{주제명}/README.md      # 문서화 목표
touch docs/documentation/active/{주제명}/ANALYSIS.md    # 코드 분석 결과
touch docs/documentation/active/{주제명}/DISCUSSION.md  # 협의 내용 기록
touch docs/documentation/active/{주제명}/DRAFT.md       # 문서 초안
```

### Step 3: README.md 작성

```markdown
# {주제명} 문서화

## 목표

**대상**: {문서화 대상}
**시작일**: {YYYY-MM-DD}
**상태**: 🔍 분석중 | 💬 협의중 | 📝 초안작성 | ✅ 완료

## 문서화 범위

| 영역 | 포함 여부 | 비고 |
|------|----------|------|
| 아키텍처 | ✅/❌ | |
| 데이터 흐름 | ✅/❌ | |
| API | ✅/❌ | |
| 알고리즘 | ✅/❌ | |

## 분석 대상 파일

- `{파일1}`
- `{파일2}`

## 최종 문서 위치 (예정)

- `docs/concepts/{카테고리}/{문서명}.md`
```

### Step 4: 코드 분석 (ANALYSIS.md)

```markdown
# {주제명} 코드 분석

## 분석일: {YYYY-MM-DD}

## 1. 파일 구조

```
분석된 파일 트리
```

## 2. 핵심 컴포넌트

### 2.1 {컴포넌트1}

**위치**: `{파일 경로}`
**역할**: {한줄 설명}

```kotlin/typescript
// 핵심 코드 발췌
```

**분석 결과**:
- {발견 사항 1}
- {발견 사항 2}

**확인 필요**:
- ❓ {질문 1}
- ❓ {질문 2}

### 2.2 {컴포넌트2}
...

## 3. 데이터 흐름

```
{컴포넌트A} → {컴포넌트B} → {컴포넌트C}
```

## 4. 협의 필요 사항

| 번호 | 항목 | 질문 |
|------|------|------|
| Q1 | | |
| Q2 | | |
```

### Step 5: 협의 진행 (DISCUSSION.md)

```markdown
# {주제명} 협의 기록

## 협의 세션

### Session 1: {YYYY-MM-DD}

#### Q1: {질문}
**Claude 분석**: {분석 내용}
**사용자 답변**: {답변}
**결론**: {최종 결론}

#### Q2: {질문}
...

### Session 2: {YYYY-MM-DD}
...

## 합의된 내용

| 항목 | 내용 | 세션 |
|------|------|------|
| {항목1} | {합의 내용} | Session 1 |
| {항목2} | {합의 내용} | Session 2 |

## 미해결 항목

- [ ] {미해결 1}
- [ ] {미해결 2}
```

### Step 6: 초안 작성 (DRAFT.md)

```markdown
# {문서 제목}

> 이 문서는 협의 기반으로 자동 생성된 초안입니다.

## 개요

{시스템/모듈 개요}

## 아키텍처

{협의된 아키텍처 설명}

## 핵심 컴포넌트

### {컴포넌트1}

{역할 및 동작 설명}

### {컴포넌트2}
...

## 데이터 흐름

{협의된 데이터 흐름 설명}

## Why (설계 의도)

{왜 이렇게 설계되었는가}

---

**문서화 협의**: [DISCUSSION.md](DISCUSSION.md)
**코드 분석**: [ANALYSIS.md](ANALYSIS.md)
```

### Step 7: 승인 및 적용

```yaml
승인 요청:
  - DRAFT.md 내용 검토 요청
  - 수정 사항 협의
  - 최종 승인

적용:
  1. DRAFT.md → docs/concepts/{카테고리}/{문서명}.md 복사
  2. documentation/active/ → completed/ 이동
  3. /sync 호출 (PROJECT_STATUS.md 업데이트)
  4. daily/ 로그 기록
```

## 문서화 유형별 가이드

### 유형 1: 시스템 아키텍처

```yaml
분석 대상:
  - 전체 프로젝트 구조
  - Controller/Service/Repository 계층
  - Frontend 컴포넌트 구조
  - 통신 방식

출력 문서:
  - docs/concepts/architecture/SYSTEM_OVERVIEW.md
```

### 유형 2: 특정 모듈

```yaml
분석 대상:
  - 단일 Service 또는 컴포넌트
  - 관련 DTO, Model
  - 의존성

출력 문서:
  - docs/concepts/{카테고리}/{모듈명}.md
```

### 유형 3: 알고리즘

```yaml
분석 대상:
  - algorithm/ 패키지
  - 수학적 계산 로직
  - Orekit 활용 코드

출력 문서:
  - docs/concepts/algorithms/{알고리즘명}.md
```

### 유형 4: API 명세

```yaml
분석 대상:
  - Controller 엔드포인트
  - Request/Response DTO
  - WebSocket 핸들러

출력 문서:
  - docs/concepts/api/{API그룹}.md
```

## 참조 파일

- **완료 처리:** [../done/SKILL.md](../done/SKILL.md)
- **문서 동기화:** [../sync/SKILL.md](../sync/SKILL.md)

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `project-manager` | 문서 구조 관리 | 폴더 생성, 이동 |
| `architect` | 아키텍처 분석 | 시스템 구조 분석 |
| `doc-syncer` | 문서 동기화 | 완료 후 sync |

## 사용 예시

### 예시 1: 시스템 아키텍처 문서화

```
사용자: "/docs 시스템 아키텍처"

→ /docs 워크플로우 시작:

[영역 선택]
  주제: System_Architecture
  범위: 전체 시스템 구조

[폴더 생성]
  docs/documentation/active/System_Architecture/
  - README.md
  - ANALYSIS.md
  - DISCUSSION.md
  - DRAFT.md

[코드 분석]
  Backend: 15개 Controller, 20개 Service 발견
  Frontend: 12개 Page, 8개 Store 발견

  ANALYSIS.md 작성:
  - 계층 구조 분석
  - 데이터 흐름 파악
  - 질문 목록 생성

[협의]
  Claude: "모드 시스템이 6개인데, 각 모드의 진입 조건이 있나요?"
  사용자: "네, Standby에서만 다른 모드로 전환 가능해요"
  → DISCUSSION.md에 기록

[초안 작성]
  협의 내용 기반 DRAFT.md 생성

[승인]
  사용자: "좋아, 이대로 적용해"

[적용]
  DRAFT.md → docs/concepts/architecture/SYSTEM_OVERVIEW.md
  active/ → completed/ 이동
```

### 예시 2: 특정 서비스 문서화

```
사용자: "/docs EphemerisService 문서화해줘"

→ /docs 워크플로우:

[분석]
  대상: EphemerisService.kt (4,986줄)
  관련: EphemerisController, TLE 모델

[코드 분석]
  - 주요 메서드 20개 식별
  - Orekit 의존성 분석
  - 데이터 흐름 파악

[협의]
  Q: "calculateSatellitePosition 메서드에서 Train 각도 변환 로직이 있는데,
      이것이 Tilt 표시와 어떻게 연결되나요?"
  A: "내부는 Train, UI 표시만 Tilt로 변환합니다"

[초안 → 승인 → 적용]
  → docs/concepts/algorithms/Ephemeris_Calculation.md
```

## 협의 가이드라인

### Claude가 질문할 때

```yaml
형식:
  - 구체적으로 질문 (예/아니오로 답변 가능하게)
  - 분석 결과와 함께 제시
  - 대안이 있다면 함께 제시

예시:
  ❌ "이 코드는 어떻게 동작하나요?"
  ✅ "이 코드가 Train 각도를 계산하는 것 같은데,
      입력값이 라디안이고 출력도 라디안인가요?"
```

### 사용자가 답변할 때

```yaml
권장:
  - 간단한 확인: "네" / "아니오"
  - 추가 정보: "네, 그리고 {추가 내용}"
  - 수정: "아니요, 실제로는 {정정 내용}"
```

## 트러블슈팅

### Q: 코드가 너무 많아서 분석이 오래 걸려요
A: 범위를 좁혀서 진행. 예: "EphemerisService 전체" → "calculatePosition 메서드만"

### Q: 협의 내용을 수정하고 싶어요
A: DISCUSSION.md의 해당 세션에 "수정" 표시하고 새 내용 추가

### Q: 초안이 마음에 안 들어요
A: 구체적인 수정 요청 → DRAFT.md 업데이트 → 재승인

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-06
**호환:** ACS 프로젝트 전용
