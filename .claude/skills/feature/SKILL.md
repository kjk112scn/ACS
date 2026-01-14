---
name: creating-feature
description: 신규 기능 개발 워크플로우. RFC 작성, ADR 생성, DESIGN 문서화, 구현까지 전체 흐름 관리. "기능 추가", "새 기능", "feature", "신규 개발" 키워드에 반응.
---

# Creating Feature - 신규 기능 개발 스킬

## 역할

신규 기능 개발의 전체 라이프사이클을 관리합니다.

**핵심 가치:**
- 일관된 기능 개발 프로세스
- 설계 문서 자동 생성
- Why 기록 자동화
- ADR 자동 연동

## 워크플로우

```
[1. 분석] → [2. RFC] → [3. ADR] → [4. 설계] → [5. 구현] → [6. 완료]
    │          │          │          │          │          │
 요청 분석   제안서    결정 기록   DESIGN     코드 작성   /done
            (선택)    (자동)      작성                   호출
```

## 실행 단계

### Step 1: 요청 분석

```yaml
입력: 사용자 요청 분석
출력:
  - 기능명 (PascalCase)
  - 기능 설명
  - 영향 범위 예측
  - RFC 필요 여부 판단
```

**RFC 필요 여부 기준:**
| 조건 | RFC 필요 |
|------|---------|
| 여러 컴포넌트 영향 | 필요 |
| 아키텍처 변경 | 필요 |
| API 인터페이스 변경 | 필요 |
| 단일 파일 수정 | 불필요 |
| 버그 수정 | 불필요 |

### Step 2: 폴더 및 문서 생성

```bash
# 폴더 생성
mkdir -p docs/work/active/{기능명}/

# 필수 문서 생성
touch docs/work/active/{기능명}/README.md
touch docs/work/active/{기능명}/DESIGN.md
touch docs/work/active/{기능명}/PROGRESS.md

# 선택: RFC 필요시
touch docs/work/active/{기능명}/RFC.md
```

### Step 3: README.md 작성

```markdown
# {기능명}

## 개요

**목적**: {왜 이 기능이 필요한가}
**요청일**: {YYYY-MM-DD}
**상태**: 🚧 진행중

## 요구사항

- [ ] 요구사항 1
- [ ] 요구사항 2

## 영향 범위

| 영역 | 파일/컴포넌트 |
|------|--------------|
| Backend | |
| Frontend | |
| Algorithm | |

## 관련 문서

- [DESIGN.md](DESIGN.md) - 설계 문서
- [PROGRESS.md](PROGRESS.md) - 진행 상황
```

### Step 4: DESIGN.md 작성

```markdown
# {기능명} 설계 문서

## 1. 설계 의도

### Why (왜 이렇게 설계했는가)
{설계 결정의 이유}

### 대안 분석
| 대안 | 장점 | 단점 | 선택 여부 |
|------|------|------|----------|
| 대안 A | | | ❌ |
| 대안 B | | | ✅ 선택 |

## 2. 구현 계획

### 2.1 Backend 변경사항
```kotlin
// 예시 코드 또는 설명
```

### 2.2 Frontend 변경사항
```typescript
// 예시 코드 또는 설명
```

## 3. API 변경 (해당시)

| Method | Endpoint | 설명 |
|--------|----------|------|
| | | |

## 4. 테스트 계획

- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] E2E 테스트

## 5. 관련 ADR

- ADR-XXX: {결정 제목}
```

### Step 5: PROGRESS.md 작성

```markdown
# {기능명} 진행 상황

## 진행률: 0%

## 작업 체크리스트

### Phase 1: 준비
- [ ] 요구사항 분석
- [ ] 설계 문서 작성
- [ ] ADR 생성

### Phase 2: 구현
- [ ] Backend 구현
- [ ] Frontend 구현
- [ ] 테스트 작성

### Phase 3: 검증
- [ ] 빌드 확인
- [ ] 테스트 통과
- [ ] 코드 리뷰

## 일일 로그

### {YYYY-MM-DD}
- 작업 내용
```

### Step 6: ADR 자동 생성 (선택)

**트리거 조건:**
- 새로운 라이브러리 도입
- 아키텍처 패턴 변경
- API 인터페이스 변경

**생성 위치:** `docs/decisions/ADR-NNN-{제목}.md`

### Step 7: 구현 진행

```yaml
호출 에이전트:
  - be-expert: Backend 구현
  - fe-expert: Frontend 구현
  - algorithm-expert: 알고리즘 구현

진행 상황:
  - PROGRESS.md 실시간 업데이트
  - 완료된 체크박스 표시
```

### Step 8: 완료 처리

**자동 호출:** `/done` 스킬

```yaml
수행 작업:
  1. PROGRESS.md → IMPLEMENTATION.md 변환
  2. work/active/ → work/archive/ 이동
  3. architecture/context/ 문서 업데이트 (해당시)
  4. logs/ 로그 기록
```

## 참조 파일

- **스킬 연계:** [../done/SKILL.md](../done/SKILL.md), [../sync/SKILL.md](../sync/SKILL.md)
- **ADR 템플릿:** [../../templates/ADR_TEMPLATE.md](../../templates/ADR_TEMPLATE.md)

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `project-manager` | 문서 구조 관리 | 폴더/문서 생성 |
| `architect` | 설계 문서 작성 | DESIGN.md 작성 |
| `be-expert` | Backend 코드 | 구현 단계 |
| `fe-expert` | Frontend 코드 | 구현 단계 |
| `algorithm-expert` | 알고리즘 | 알고리즘 관련 시 |

## 사용 예시

### 예시 1: 새 기능 추가

```
사용자: "Pass Schedule에 차트 줌 기능 추가해줘"

→ /feature 워크플로우 시작:

[분석] 기능명: PassSchedule_Chart_Zoom
       영향 범위: Frontend (PassSchedulePage.vue)
       RFC 필요: 아니오

[폴더 생성] docs/work/active/PassSchedule_Chart_Zoom/
            - README.md
            - DESIGN.md
            - PROGRESS.md

[설계] DESIGN.md 작성
       - Why: 사용자가 특정 시간대를 자세히 보고 싶음
       - How: Quasar Q-Chart 확대 옵션 활용

[구현] fe-expert 에이전트 호출
       - PassSchedulePage.vue 수정

[완료] /done 자동 호출
       - work/active/ → work/archive/ 이동
       - logs/ 로그 기록
```

### 예시 2: 복잡한 기능 (RFC 필요)

```
사용자: "실시간 알람 시스템 추가해줘"

→ /feature 워크플로우 시작:

[분석] 기능명: Realtime_Alarm_System
       영향 범위: Backend + Frontend + 새 컴포넌트
       RFC 필요: 예

[RFC 작성] docs/work/active/Realtime_Alarm_System/RFC.md
           - 제안 내용
           - 대안 비교
           - 영향 분석

[ADR 생성] docs/decisions/ADR-003-alarm-notification-method.md
           - Context: 실시간 알림 필요
           - Decision: WebSocket 기반
           - Alternatives: Polling, SSE

[설계] DESIGN.md 작성...
...
```

## 트러블슈팅

### Q: 기능명을 어떻게 정하나요?
A: PascalCase + 언더스코어 조합. 예: `PassSchedule_Chart_Optimization`

### Q: RFC를 꼭 작성해야 하나요?
A: 단일 컴포넌트 수정은 스킵 가능. 여러 컴포넌트 영향시 권장.

### Q: 중간에 요구사항이 바뀌면?
A: DESIGN.md의 "변경 이력" 섹션에 기록하고 Why 업데이트.

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-06
**호환:** ACS 프로젝트 전용
