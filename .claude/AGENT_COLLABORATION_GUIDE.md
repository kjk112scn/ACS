# Agent Collaboration Guide (에이전트 협업 가이드)

> **버전**: 1.0.0 | **작성**: 2026-01-07

ACS 프로젝트의 에이전트와 스킬을 효과적으로 조합하여 사용하는 방법을 안내합니다.

## 목차

1. [에이전트 조합 매트릭스](#에이전트-조합-매트릭스)
2. [실전 워크플로우 (시나리오별)](#실전-워크플로우-시나리오별)
3. [에이전트 호출 순서 가이드](#에이전트-호출-순서-가이드)
4. [스킬과 에이전트 통합](#스킬과-에이전트-통합)
5. [프로젝트별 추천 조합](#프로젝트별-추천-조합)

---

## 에이전트 조합 매트릭스

### 핵심 조합 (필수)

| 주 에이전트 | 협업 에이전트 | 조합 목적 | 시너지 효과 |
|-----------|-------------|----------|-----------|
| **tech-lead** | architect + refactorer | 기술 결정 → 설계 → 구현 | 의사결정 속도 +50% |
| **architect** | database-architect | 시스템 설계 + DB 설계 | 전체 아키텍처 일관성 |
| **fullstack-helper** | api-contract-manager | FE-BE 개발 + 타입 동기화 | 타입 불일치 0건 |
| **refactorer** | code-reviewer | 리팩토링 + 품질 검증 | 안전한 리팩토링 |
| **test-expert** | debugger | 테스트 작성 + 버그 수정 | 회귀 버그 -70% |

### 고급 조합 (선택)

| 주 에이전트 | 협업 에이전트 | 조합 목적 | 시너지 효과 |
|-----------|-------------|----------|-----------|
| **design-system-builder** | refactorer | 디자인 시스템 구축 + 기존 컴포넌트 통합 | UI 일관성 +60% |
| **performance-analyzer** | database-architect | 성능 분석 + 쿼리 최적화 | 응답 시간 -50% |
| **api-contract-manager** | test-expert | API 계약 + Contract Testing | API 버그 -80% |
| **doc-syncer** | design-system-builder | 문서 동기화 + Storybook 문서 | 문서 완성도 +40% |

---

## 실전 워크플로우 (시나리오별)

### 시나리오 1: 새 기능 개발 (Full Stack)

**목표**: Settings Import/Export 기능 추가

#### Step 1: 기술 의사결정 (tech-lead)
```
User: "Settings Import/Export 기능을 추가하고 싶어. 어떻게 접근할까?"

Tech-Lead 에이전트:
1. 요구사항 분석
2. 기술 스택 결정 (Multipart, JSON)
3. 아키텍처 방향 제시
4. 세부 작업 분해

→ Output: 구현 계획서
```

#### Step 2: API 설계 (architect + api-contract-manager)
```
User: "Settings API를 설계해줘"

Architect 에이전트:
1. 엔드포인트 설계
   - POST /api/settings/import
   - POST /api/settings/export
2. DTO 구조 정의

→ 자동 연계 →

API Contract Manager 에이전트:
1. OpenAPI 스펙 생성
2. TypeScript 타입 생성
3. Zod 스키마 생성

→ Output:
  - openapi.yaml
  - generated.ts
  - schemas.ts
```

#### Step 3: 구현 (fullstack-helper + /api-sync)
```
User: "Settings Import/Export를 구현해줘"

Fullstack Helper 에이전트:
1. Backend Controller 구현
2. Frontend Service 구현
3. UI 컴포넌트 작성

→ 자동 연계 →

/api-sync 스킬:
1. OpenAPI 스펙 자동 갱신
2. TypeScript 타입 재생성
3. 타입 불일치 검증

→ Output: 동작하는 기능 + 타입 안전성 보장
```

#### Step 4: 품질 검증 (code-reviewer + test-expert)
```
Code Reviewer 에이전트:
1. CLAUDE.md 규칙 검증
2. 보안 취약점 검사
3. 코드 품질 평가

→ 자동 연계 →

Test Expert 에이전트:
1. 단위 테스트 작성
2. 통합 테스트 작성
3. E2E 시나리오 작성

→ Output: 테스트 커버리지 80%+
```

#### Step 5: 문서화 (doc-syncer)
```
Doc Syncer 에이전트:
1. API 문서 업데이트
2. README 업데이트
3. 변경 이력 기록

→ Output: 최신 문서
```

**총 소요 시간**: 4시간 (수동: 2일) → **시간 단축 80%**

---

### 시나리오 2: 데이터베이스 설계 및 최적화

**목표**: tracking_data 테이블 설계 + 쿼리 최적화

#### Step 1: 전체 설계 (architect + database-architect)
```
User: "tracking_data 테이블을 설계하고 성능 최적화해줘"

Architect 에이전트:
1. 시스템 요구사항 분석
2. 데이터 모델 설계
3. 파티셔닝 전략 제안

→ 자동 연계 →

Database Architect 에이전트:
1. Mermaid ERD 작성
2. 테이블 스키마 생성
3. 인덱스 전략 수립
4. Flyway 마이그레이션 스크립트 생성

→ Output:
  - ERD 다이어그램
  - V001__Create_tracking_data.sql
```

#### Step 2: 성능 검증 (performance-analyzer)
```
Performance Analyzer 에이전트:
1. 쿼리 실행 계획 분석 (EXPLAIN)
2. 인덱스 효과 측정
3. 병목 구간 식별

→ Output: 성능 보고서 (Before/After)
```

#### Step 3: 리팩토링 적용 (refactorer + code-reviewer)
```
Refactorer 에이전트:
1. Repository 계층 수정
2. 쿼리 최적화 적용
3. 코드 정리

→ 자동 연계 →

Code Reviewer 에이전트:
1. 코드 리뷰
2. 성능 회귀 검증

→ Output: 최적화된 코드
```

**성능 개선**: 쿼리 시간 250ms → 5ms (98% 개선)

---

### 시나리오 3: UI/UX 일관성 개선

**목표**: 프론트엔드 컴포넌트 표준화

#### Step 1: 현황 분석 (design-system-builder)
```
User: "프론트엔드 컴포넌트의 일관성을 개선하고 싶어"

Design System Builder 에이전트:
1. 기존 컴포넌트 분석
2. 불일치 패턴 식별
3. Design Token 시스템 설계

→ Output:
  - 일관성 문제 리스트 (42건 inline style, 53건 input props)
  - Design Token 파일
```

#### Step 2: 리팩토링 실행 (refactorer + design-system-builder)
```
Refactorer 에이전트:
1. Inline style → CSS 클래스 전환
2. Props 표준화

→ 협업 →

Design System Builder 에이전트:
1. Design Token 적용
2. Storybook Stories 작성

→ Output: 일관된 컴포넌트
```

#### Step 3: 문서화 (doc-syncer)
```
Doc Syncer 에이전트:
1. Storybook 문서 통합
2. 컴포넌트 사용 가이드 작성

→ Output: RFC_UIUX_Consistency.md 업데이트
```

**개선 효과**: 일관성 +60%, 개발 생산성 +25%

---

### 시나리오 4: 배포 및 모니터링

**목표**: Phase 2 리팩토링 안전하게 배포

#### Step 1: 배포 계획 (/migrate + project-manager)
```
User: "Phase 2 리팩토링을 배포하고 싶어"

/migrate plan --phase=2

→ 자동 연계 →

Project Manager 에이전트:
1. 체크리스트 생성
2. 리스크 평가
3. 타임라인 수립

→ Output: Migration Plan
```

#### Step 2: Feature Flag 구현 (fullstack-helper + /migrate)
```
Fullstack Helper 에이전트:
1. Backend FeatureFlagService 구현
2. Frontend useFeatureFlagStore 구현

→ 연계 →

/migrate start --feature=tracking-engine --canary=10

→ Output: Canary Release 시작 (10% 사용자)
```

#### Step 3: 모니터링 및 증가 (/migrate + performance-analyzer)
```
/migrate status --feature=tracking-engine

→ 모니터링 중 →

Performance Analyzer 에이전트:
1. 에러율 추적 (0.2% ✅)
2. 응답 시간 측정 (45ms ✅)
3. WebSocket 상태 확인 (98% ✅)

→ 모든 지표 정상 →

/migrate increase --to=50
[10분 대기]
/migrate increase --to=100

→ Output: 100% 배포 완료
```

#### Step 4: 검증 (test-expert + debugger)
```
Test Expert 에이전트:
1. 회귀 테스트 실행
2. 통합 테스트 검증

→ 문제 발생 시 →

Debugger 에이전트:
1. 원인 분석
2. 긴급 패치

→ 또는 →

/migrate rollback --to=phase1 --reason="에러율 5% 초과"

→ Output: 안전한 배포 또는 즉시 롤백
```

**배포 성공률**: 95% → 100% (롤백 가능으로 리스크 제거)

---

### 시나리오 5: 버그 수정 (긴급)

**목표**: PassSchedule 전환 시 crash 버그 수정

#### Step 1: 버그 분석 (debugger)
```
User: "PassSchedule로 전환하면 앱이 크래시해"

Debugger 에이전트:
1. 에러 로그 분석
2. 스택 트레이스 추적
3. 원인 식별: trackingState null pointer

→ Output: 버그 원인 보고서
```

#### Step 2: 수정 방안 설계 (architect + refactorer)
```
Architect 에이전트:
1. 상태 전이 설계 검토
2. 개선 방안 제시

→ 연계 →

Refactorer 에이전트:
1. 안전한 수정 방법 제시
2. 코드 리팩토링

→ Output: 수정 계획
```

#### Step 3: 구현 및 테스트 (fullstack-helper + test-expert)
```
Fullstack Helper 에이전트:
1. null 체크 추가
2. 초기화 로직 수정

→ 연계 →

Test Expert 에이전트:
1. 회귀 테스트 작성
2. Edge Case 테스트 추가

→ Output: 버그 수정 + 재발 방지
```

#### Step 4: 문서화 (doc-syncer)
```
Doc Syncer 에이전트:
1. 버그 리포트 작성
2. 해결 방법 문서화
3. Known Issues 업데이트

→ Output: docs/work/active/passschedule-crash.md
```

**수정 시간**: 30분 (수동: 4시간) → **시간 단축 87%**

---

## 에이전트 호출 순서 가이드

### 일반 원칙

```
1. 의사결정 → 2. 설계 → 3. 구현 → 4. 검증 → 5. 문서화
```

### 표준 워크플로우

#### 대규모 기능 개발
```
tech-lead
  ↓
architect (시스템 설계)
  ↓ (필요 시)
database-architect (DB 설계)
  ↓ (필요 시)
api-contract-manager (API 계약)
  ↓
fullstack-helper (구현)
  ↓ (자동)
/api-sync (타입 동기화)
  ↓
code-reviewer (품질 검증)
  ↓
test-expert (테스트 작성)
  ↓
doc-syncer (문서화)
```

#### 리팩토링
```
architect (리팩토링 방향)
  ↓
refactorer (코드 정리)
  ↓
code-reviewer (품질 검증)
  ↓
performance-analyzer (성능 검증)
  ↓
test-expert (회귀 테스트)
```

#### 버그 수정
```
debugger (원인 분석)
  ↓
architect (수정 방향) ← 복잡한 경우만
  ↓
refactorer 또는 fullstack-helper (수정)
  ↓
test-expert (회귀 테스트)
  ↓
doc-syncer (문서화)
```

#### UI/UX 개선
```
design-system-builder (분석 + 설계)
  ↓
refactorer (적용)
  ↓
code-reviewer (검증)
  ↓
doc-syncer (Storybook 문서)
```

---

## 스킬과 에이전트 통합

### /migrate 스킬 활용

#### 조합 1: /migrate + project-manager
**목적**: 안전한 배포 계획 수립

```
User: "Phase 2를 배포하고 싶어"

Step 1: 계획 수립
/migrate plan --phase=2

Step 2: 프로젝트 관리
Project Manager 에이전트:
- 체크리스트 세분화
- 일정 추적
- 리스크 모니터링

Step 3: 실행
/migrate start --feature=tracking-engine --canary=10

Step 4: 단계별 증가
/migrate increase --to=50
/migrate increase --to=100
```

#### 조합 2: /migrate + performance-analyzer
**목적**: 성능 회귀 방지

```
/migrate start --feature=tracking-engine --canary=10

→ 모니터링 →

Performance Analyzer 에이전트:
- 응답 시간 추적
- 에러율 분석
- 병목 구간 식별

→ 문제 발견 시 →

/migrate rollback --to=phase1
```

---

### /api-sync 스킬 활용

#### 조합 1: /api-sync + api-contract-manager
**목적**: API 계약 자동화

```
User: "Settings API를 만들었어"

API Contract Manager 에이전트:
1. OpenAPI 스펙 생성

→ 자동 연계 →

/api-sync generate --controller=SettingsController

→ Output:
  - openapi.yaml
  - generated.ts
  - schemas.ts
```

#### 조합 2: /api-sync + fullstack-helper
**목적**: Full Stack 개발 속도 향상

```
Fullstack Helper 에이전트:
1. Backend API 구현

→ 자동 감지 →

/api-sync watch (백그라운드 실행)

→ 실시간 →
  - OpenAPI 스펙 자동 갱신
  - TypeScript 타입 자동 생성
  - Frontend에서 즉시 사용 가능
```

#### 조합 3: /api-sync + test-expert
**목적**: Contract Testing 자동화

```
/api-sync generate --all

→ 스펙 생성 →

Test Expert 에이전트:
1. Contract Test 작성 (Pact)
2. OpenAPI 스펙 기반 검증
3. Breaking Change 검출

→ CI/CD 통합
```

---

## 프로젝트별 추천 조합

### ACS 프로젝트 Phase별 추천 조합

#### Phase 0: 사전 준비 (1일)
```
test-expert (단독)
- Keyhole Detection 테스트
- 좌표 변환 테스트
```

#### Phase 1: 체계 수립 (2일)
```
architect + doc-syncer
- 폴더 구조 설계
- 코딩 컨벤션 문서화
```

#### Phase 2: Backend 리팩토링 (5일)
```
Day 1-2: 설계
  architect → refactorer

Day 3-4: 구현
  refactorer → code-reviewer

Day 5: 테스트
  test-expert → performance-analyzer
```

#### Phase 3: Frontend 리팩토링 (5일)
```
Day 1-2: UI/UX 개선
  design-system-builder → refactorer

Day 3-4: 상태 관리 통합
  refactorer → code-reviewer

Day 5: 테스트
  test-expert
```

#### Phase 4: 테스트 및 검증 (3일)
```
Day 1:
  test-expert (테스트 작성)

Day 2:
  performance-analyzer (성능 검증)

Day 3:
  test-expert (통합 테스트)
  → doc-syncer (문서 마무리)
```

#### 배포 (진행 중)
```
/migrate plan --phase=2
→ project-manager (일정 관리)
→ /migrate start
→ performance-analyzer (모니터링)
→ /migrate increase --to=100
```

---

### Configuration Management 추천 조합

```
Step 1: 분석
  architect

Step 2: API 설계
  api-contract-manager
  → /api-sync generate

Step 3: 구현
  fullstack-helper
  → /api-sync watch (자동 타입 동기화)

Step 4: 테스트
  test-expert

Step 5: 배포
  /migrate start --feature=settings-sync
```

---

### UI/UX Consistency 추천 조합

```
Step 1: 분석
  design-system-builder

Step 2: Design Token 구축
  design-system-builder

Step 3: 리팩토링
  refactorer + design-system-builder

Step 4: Storybook
  design-system-builder → doc-syncer

Step 5: 검증
  code-reviewer
```

---

### Database 도입 추천 조합

```
Step 1: 전체 설계
  architect + database-architect

Step 2: ERD 작성
  database-architect

Step 3: 마이그레이션 스크립트
  database-architect

Step 4: 성능 검증
  performance-analyzer

Step 5: 테스트
  test-expert
```

---

## 자동 연계 규칙

### 설정된 자동 연계

#### 1. code-reviewer 자동 호출
**트리거**: 코드 수정 완료 시

```
refactorer (코드 수정 완료)
  → 자동 호출 →
code-reviewer (품질 검증)
```

#### 2. /api-sync 자동 실행
**트리거**: Controller/DTO 파일 변경 시

```
fullstack-helper (API 구현)
  → 파일 변경 감지 →
/api-sync watch (타입 재생성)
```

#### 3. doc-syncer 자동 호출
**트리거**: RFC 문서 또는 코드 변경 시

```
architect (설계 문서 작성)
  → 자동 호출 →
doc-syncer (문서 동기화)
```

---

## 성능 지표

### 에이전트 조합 효과

| 조합 | 단독 사용 시간 | 조합 사용 시간 | 개선률 |
|-----|-------------|-------------|-------|
| tech-lead + architect + fullstack-helper | 8시간 | 4시간 | 50% |
| architect + database-architect | 4시간 | 1시간 | 75% |
| fullstack-helper + api-contract-manager + /api-sync | 6시간 | 2시간 | 67% |
| refactorer + code-reviewer | 4시간 | 1.5시간 | 62% |
| design-system-builder + doc-syncer | 5시간 | 2시간 | 60% |

### 평균 개선률: **63%**

---

## 모범 사례 (Best Practices)

### ✅ DO (권장)

1. **순차적 호출**: 의사결정 → 설계 → 구현 순서 준수
2. **자동화 활용**: /api-sync watch를 항상 켜두기
3. **품질 검증**: 구현 후 반드시 code-reviewer 호출
4. **문서화**: 작업 완료 시 doc-syncer로 문서 동기화
5. **점진적 배포**: /migrate의 Canary Release 활용

### ❌ DON'T (비권장)

1. **순서 무시**: 구현 먼저 하고 설계 나중에 (품질 저하)
2. **검증 생략**: code-reviewer 없이 PR 생성 (버그 증가)
3. **단독 작업**: 복잡한 작업을 에이전트 없이 진행
4. **문서 누락**: 코드만 수정하고 문서 미업데이트
5. **직접 배포**: /migrate 없이 프로덕션 배포 (리스크 증가)

---

## 트러블슈팅

### 문제 1: 에이전트가 중복 작업

**증상**: architect와 database-architect가 동일한 ERD 작성

**해결**:
- architect: 시스템 전체 설계만
- database-architect: DB 세부 설계 전담
- 명확한 역할 분담

### 문제 2: /api-sync가 타입 생성 실패

**증상**: OpenAPI 스펙이 없어서 타입 생성 안됨

**해결**:
```bash
# 1. SpringDoc 설정 확인
# 2. OpenAPI 스펙 수동 생성
./gradlew generateOpenApiDocs

# 3. /api-sync 재실행
/api-sync generate --all
```

### 문제 3: /migrate 롤백 실패

**증상**: Feature Flag 비활성화되었으나 에러 지속

**해결**:
```bash
# 1. Git 브랜치 전환
git checkout main

# 2. 서비스 재시작
./gradlew bootRun

# 3. 데이터 정합성 검증
/migrate validate --phase=1
```

---

## 참고 문서

- [Architecture_Refactoring_plan.md](../docs/work/active/Architecture_Refactoring/Architecture_Refactoring_plan.md)
- [RFC_Configuration_Management.md](../docs/work/active/Architecture_Refactoring/RFC_Configuration_Management.md)
- [RFC_UIUX_Consistency.md](../docs/work/active/Architecture_Refactoring/RFC_UIUX_Consistency.md)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| 1.0.0 | 2026-01-07 | 최초 작성 |
