# Unified Workflow (통합 워크플로우)

> **Version**: 2.0.0
> **검증 사례**: PassSchedule 상태머신 리팩토링 (15/17 완료, 88%)
> **적용 스킬**: /refactor, /bugfix, /feature, /plan

---

## 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **문서 기반** | 모든 작업은 문서로 추적 |
| **Phase 분류** | 우선순위별 Phase A-D 분류 |
| **전문가 검토** | 에이전트를 통한 자동 검토 |
| **점진적 진행** | Phase 단위 실행 + 빌드 검증 |

---

## 통합 워크플로우

```
[1. 조사] → [2. 검토] → [3. 계획] → [4. 승인] → [5. 실행] → [6. 완료]
     │           │           │           │           │           │
  코드 분석   전문가      PLAN.md    사용자      단계별    /done
  이슈 목록   에이전트    작성       확인       구현      호출
     │           │           │                    │
     ▼           ▼           ▼                    ▼
  ANALYSIS.md  tech-lead   Phase A-D    PROGRESS.md
              code-reviewer 분류        실시간 업데이트
```

---

## Phase 분류 기준

| Phase | 우선순위 | 기준 | 예시 |
|:-----:|:--------:|------|------|
| **A** | P0 (Critical) | 시스템 다운, 데이터 손실, 핵심 기능 불가 | 상태 전환 실패, 데이터 유실 |
| **B** | P1 (High) | 주요 기능 제한, 사용자 영향 큼 | 타임아웃 없음, UX 심각한 문제 |
| **C** | P2 (Medium) | 기능 제한 있으나 우회 가능 | 동시성 이슈, 성능 저하 |
| **D** | P3 (Low) | 코드 품질, 기술 부채 | 예외 처리, 로그 레벨, 명명 규칙 |

---

## 문서 구조 (자동 생성)

```
docs/work/active/{Feature}/
├── README.md                      ← 마스터 (현황 + 링크)
├── ANALYSIS.md (또는 STATE_MACHINE.md)  ← 분석 결과 (이슈 목록)
├── PLAN.md                        ← 실행 계획 (Phase 분류)
├── IMPLEMENTATION_PROGRESS.md     ← 진행 상황
└── TEST_SCENARIOS.md              ← 테스트 시나리오 (선택)
```

---

## 단계별 상세

### Step 1: 조사 (Research)

**수행 항목:**
```yaml
자동 분석:
  - 대상 파일/코드 전체 읽기
  - 이슈/버그 목록 작성
  - CLAUDE.md 규칙 위반 체크
  - 영향 범위 분석

출력: ANALYSIS.md
  - 이슈 목록 (우선순위별)
  - 수정 코드 예시 (가능한 경우)
  - 영향 범위 분석
```

**ANALYSIS.md 템플릿:**
```markdown
# {대상} 분석

## 이슈 목록

| # | 이슈 | 심각도 | 위치 |
|---|------|:------:|------|
| 1 | {이슈 설명} | P0 | L1234 |
| 2 | {이슈 설명} | P1 | L2345 |

## 영향 범위

| 영역 | 영향 | 설명 |
|------|:----:|------|
| Backend | ✅ | |
| Frontend | ❌ | |

## CLAUDE.md 위반 사항

| 규칙 | 위반 개수 | 위치 |
|------|:--------:|------|
| catch(Exception) | 13 | L593, L621, ... |
```

### Step 2: 전문가 검토 (Expert Review)

**호출 에이전트:**
```yaml
병렬 호출:
  - tech-lead: 전체 방향 검토, 우선순위 결정
  - code-reviewer: 코드 품질, CLAUDE.md 준수
  - algorithm-expert: 알고리즘 로직 (필요시)
  - architect: 아키텍처 영향 (필요시)
  - debugger: 원인 분석 (버그 수정 시)

수집 결과:
  - 우선순위 조정
  - 누락 이슈 추가
  - 구현 방향 제안
```

### Step 3: 계획 수립 (PLAN.md)

**PLAN.md 템플릿:**
```markdown
# {대상} 실행 계획

> **작성일**: YYYY-MM-DD
> **상태**: 계획 수립 완료

## 작업 범위

| Phase | 내용 | 이슈 수 | 우선순위 |
|:-----:|------|:------:|:--------:|
| A | Critical 이슈 | N | P0 |
| B | High 이슈 | N | P1 |
| C | Medium 이슈 | N | P2 |
| D | 코드 품질 | N | P3 |

## Phase A: Critical (P0)

### A1. {이슈명}
| 항목 | 내용 |
|------|------|
| **위치** | L1234-1250 |
| **문제** | {문제 설명} |
| **해결** | {해결 방법} |

## 실행 순서

Phase A → 빌드 → Phase B → 빌드 → Phase C → 빌드 → Phase D → 빌드

## 검증 체크리스트

### Phase A 완료 조건
- [ ] A1 구현 완료
- [ ] 빌드 성공
```

### Step 4: 사용자 승인

**확인 항목:**
- Phase 분류 동의
- 우선순위 확인
- 실행 순서 확인

### Step 5: 단계별 실행

**원칙:**
```yaml
구현:
  - Phase 단위로 진행
  - 각 Phase 완료 후 빌드 검증
  - PROGRESS.md 실시간 업데이트

빌드 명령:
  Backend: ./gradlew clean build -x test
  Frontend: npx vue-tsc --noEmit
```

**PROGRESS.md 템플릿:**
```markdown
# {대상} 구현 진행 상황

> **시작일**: YYYY-MM-DD
> **상태**: 진행중

## 전체 진행률

```
Phase A (Critical)  [✅✅✅] 3/3 완료!
Phase B (High)      [✅⬜⬜] 1/3 진행중
Phase C (Medium)    [⬜⬜] 0/2 대기
Phase D (Low)       [⬜⬜⬜] 0/3 대기
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total               4/11 (36%)
```

## Phase A: Critical (P0)

| # | 작업 | 상태 | 완료 |
|---|------|:----:|:----:|
| A1 | {작업명} | ✅ | ✅ |
| A2 | {작업명} | ✅ | ✅ |

## 작업 로그

### YYYY-MM-DD

| 시간 | 작업 | 결과 |
|------|------|------|
| - | Phase A 구현 | ✅ 빌드 성공 |
```

### Step 6: 완료 (/done)

**자동 수행:**
```yaml
1. 빌드 검증:
   - BE: ./gradlew clean build -x test
   - FE: npx vue-tsc --noEmit

2. 문서 업데이트:
   - CHANGELOG.md 업데이트
   - docs/logs/YYYY-MM-DD.md 업데이트
   - CURRENT_STATUS.md 업데이트
   - 작업 폴더 README/PROGRESS 업데이트

3. 문서 헬스체크 (신규):
   - 작업 폴더 표준 구조 검증 (README.md, PROGRESS.md 필수)
   - 깨진 링크 검사 (상대 경로)
   - 아카이브 후보 탐색 (진행률 100% 또는 30일 미업데이트)
   - 정리 필요 항목 알림

4. 커밋 생성
```

**문서 헬스체크 알림 예시:**
```
📋 문서 헬스체크 결과

✅ 표준 구조
   - README.md: 있음
   - PROGRESS.md: 있음

⚠️ 아카이브 후보 (2개)
   - Admin_Panel_Separation (진행률 100%)
   - DB_Schema_Redesign (30일 이상 미업데이트)

💡 정리 제안
   - Tracking_Schema_Redesign: DESIGN.md 파일명 비표준 → 수정 완료
```

**아카이브 워크플로우:**
```
완료 확인 → 아카이브 여부 질문 → docs/work/archive/로 이동
```

---

## 스킬별 차이점

| 스킬 | 주요 목적 | 분석 문서명 | 특화 에이전트 |
|------|----------|------------|--------------|
| `/refactor` | 코드 구조 개선 | STATE_MACHINE.md | code-reviewer |
| `/bugfix` | 버그 수정 | ANALYSIS.md (+ FIX.md) | debugger |
| `/feature` | 신규 기능 | DESIGN.md | architect |
| `/plan` | 계획 수립 | (PLAN.md만) | project-manager |

---

## 검증된 사례: PassSchedule

```
Phase A (Critical)  [✅✅✅✅] 4/4 완료!
Phase B (High)      [✅✅✅✅✅✅] 6/6 완료!
Phase C (Medium)    [✅⬜⬜] 1/3 (C2-C3 보류)
Phase D (Low)       [✅✅✅✅] 4/4 완료!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total               15/17 (88%)
```

**산출물:**
- [STATE_MACHINE.md](../docs/work/active/Architecture_Refactoring/passschedule/STATE_MACHINE.md)
- [PLAN.md](../docs/work/active/Architecture_Refactoring/passschedule/PLAN.md)
- [IMPLEMENTATION_PROGRESS.md](../docs/work/active/Architecture_Refactoring/passschedule/IMPLEMENTATION_PROGRESS.md)
- [TEST_SCENARIOS.md](../docs/work/active/Architecture_Refactoring/passschedule/TEST_SCENARIOS.md)

---

**버전:** 2.0.0
**작성일:** 2026-01-20
**변경:** PassSchedule 검증 워크플로우 기반 통합
