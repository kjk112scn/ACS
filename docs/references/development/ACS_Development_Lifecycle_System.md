# ACS 개발 라이프사이클 관리 시스템

> **설계 문서** - 스킬 및 서브에이전트 통합 설계안
> **작성일**: 2026-01-05
> **버전**: 1.0.0

---

## 1. 시스템 개요

### 1.1 목표

```yaml
핵심 목표:
  1. 개발 설계부터 유지보수까지 전체 라이프사이클 자동 관리
  2. 문서와 코드의 완벽한 동기화 유지
  3. 모든 변경사항의 이유(Why)를 자동 기록
  4. 문서만으로 시스템 이해 가능한 수준 유지
  5. 신규 기능/버그 수정 시 관련 설계 문서 자동 생성
```

### 1.2 전문가 Best Practices 적용

| 출처 | 적용 내용 |
|------|----------|
| [PubNub Best Practices](https://www.pubnub.com/blog/best-practices-for-claude-code-sub-agents/) | 3단계 파이프라인 (Spec → Architect → Implement) |
| [Spotify ADR](https://engineering.atspotify.com/2020/04/when-should-i-write-an-architecture-decision-record) | 모든 아키텍처 결정 ADR로 기록 |
| [Pragmatic Engineer RFC](https://newsletter.pragmaticengineer.com/p/rfcs-and-design-docs) | RFC → ADR 순차 프로세스 |
| [Anthropic Skills Guide](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices) | Progressive Disclosure, Feedback Loop |
| [AWS ADR Process](https://docs.aws.amazon.com/prescriptive-guidance/latest/architectural-decision-records/adr-process.html) | ADR 라이프사이클 6단계 |

---

## 2. 문서 구조 설계

### 2.1 제안 폴더 구조

```
docs/
├── README.md                          # 문서 시스템 안내
│
├── concepts/                          # 🆕 개념 문서 (영구 보존)
│   ├── architecture/                  # 시스템 아키텍처
│   │   ├── SYSTEM_OVERVIEW.md
│   │   ├── UI_Architecture.md
│   │   └── Data_Flow.md
│   ├── algorithms/                    # 알고리즘 설계
│   │   ├── Satellite_Tracking.md
│   │   ├── Sun_Tracking.md
│   │   └── Train_Angle_Calculation.md
│   ├── protocols/                     # 통신 프로토콜
│   │   ├── ICD_Protocol.md
│   │   └── WebSocket_Protocol.md
│   └── domain/                        # 도메인 지식
│       ├── Antenna_Control_Basics.md
│       └── Coordinate_Systems.md
│
├── decisions/                         # 🆕 ADR (Architecture Decision Records)
│   ├── README.md                      # ADR 작성 가이드
│   ├── ADR-001-train-vs-tilt.md
│   ├── ADR-002-orekit-selection.md
│   └── ADR-template.md
│
├── features/                          # 기능 개발 문서
│   ├── active/                        # 진행 중
│   │   ├── {기능명}/
│   │   │   ├── README.md              # 개요
│   │   │   ├── RFC.md                 # 🆕 Request for Comments
│   │   │   ├── DESIGN.md              # 설계 문서
│   │   │   └── PROGRESS.md            # 진행 상황
│   │   └── ...
│   └── completed/                     # 완료
│       └── {기능명}/
│           ├── README.md
│           ├── DESIGN.md
│           ├── IMPLEMENTATION.md      # 🆕 구현 결과
│           └── CHANGELOG.md           # 변경 이력
│
├── bugfixes/                          # 🆕 버그 수정 문서
│   ├── active/
│   │   └── {버그명}/
│   │       ├── README.md
│   │       ├── ANALYSIS.md            # 원인 분석
│   │       └── FIX.md                 # 수정 내용
│   └── completed/
│
├── api/                               # API 문서 (자동 생성)
│   ├── README.md
│   ├── controllers/
│   └── websocket/
│
├── guides/                            # 가이드 문서
│   ├── development/                   # 개발 가이드
│   │   ├── Development_Guide.md
│   │   ├── Coding_Standards.md
│   │   └── Setup_Guide.md
│   ├── operations/                    # 운영 가이드
│   │   └── Deployment_Guide.md
│   └── user/                          # 사용자 가이드
│       └── User_Manual.md
│
├── daily/                             # 🆕 일일 로그
│   └── YYYY-MM-DD.md
│
└── status/                            # 🆕 현황 문서
    ├── PROJECT_STATUS.md              # 프로젝트 현황 (자동 업데이트)
    ├── CODE_METRICS.md                # 코드 메트릭스 (자동 생성)
    └── TECHNICAL_DEBT.md              # 기술 부채 추적
```

### 2.2 문서 라이프사이클

```
[RFC 작성] → [RFC 승인] → [ADR 생성] → [DESIGN 작성] → [개발] → [완료]
     │            │            │            │           │        │
features/    decisions/   decisions/   features/   features/  features/
active/RFC   ADR-NNN      ADR-NNN      active/     active/    completed/
                                       DESIGN      PROGRESS
```

---

## 3. 스킬 체계 설계

### 3.1 스킬 목록 (12개)

| 카테고리 | 스킬 | 트리거 | 역할 |
|---------|------|--------|------|
| **워크플로우** | `/feature` | "기능 추가", "새 기능" | 신규 기능 개발 전체 흐름 |
| | `/bugfix` | "버그 수정", "에러 해결" | 버그 수정 전체 흐름 |
| | `/done` | "완료", "커밋" | 작업 마무리 + 문서화 |
| **문서화** | `/sync` | "동기화", "문서 업데이트" | 코드↔문서 동기화 |
| | `/adr` | "결정 기록", "왜" | ADR 생성 |
| | `/rfc` | "제안", "검토 요청" | RFC 생성 |
| **개발 지원** | `/plan` | "계획", "설계" | 작업 계획 수립 |
| | `/impl` | "구현", "개발" | 코드 구현 |
| | `/test` | "테스트", "빌드" | 빌드 및 테스트 |
| | `/review` | "리뷰", "검토" | 코드 리뷰 |
| **분석** | `/analyze` | "분석", "파악" | 코드베이스 분석 |
| | `/status` | "현황", "상태" | 프로젝트 현황 보고 |

### 3.2 워크플로우 스킬 상세

#### `/feature` - 신규 기능 개발

```yaml
워크플로우:
  1. RFC 작성 (선택적)
     - 큰 기능: RFC 필수
     - 작은 기능: 스킵 가능

  2. ADR 생성 (자동)
     - 주요 기술 결정 기록
     - 대안 분석 포함

  3. DESIGN 문서 작성
     - 설계 의도
     - 영향 범위
     - 구현 계획

  4. 구현 (impl → test → review)
     - 코드 작성
     - 빌드/테스트
     - 코드 리뷰

  5. 완료 처리 (/done)
     - active/ → completed/ 이동
     - IMPLEMENTATION.md 생성
     - CHANGELOG.md 업데이트
     - concepts/ 문서 업데이트 (해당 시)
```

#### `/bugfix` - 버그 수정

```yaml
워크플로우:
  1. ANALYSIS.md 생성
     - 증상 기록
     - 원인 분석
     - 영향 범위 파악

  2. FIX.md 작성
     - 수정 방안
     - 테스트 계획

  3. 구현 + 검증
     - 코드 수정
     - 회귀 테스트

  4. 완료 처리 (/done)
     - 관련 문서 업데이트
     - 재발 방지 대책 (해당 시)
```

#### `/done` - 작업 마무리

```yaml
자동 실행:
  1. 변경 파일 분석
     - git diff로 변경 사항 파악
     - 영향받는 문서 식별

  2. 문서 자동 업데이트
     - IMPLEMENTATION.md 생성
     - CHANGELOG.md 업데이트

  3. 문서 이동
     - features/active/ → completed/
     - bugfixes/active/ → completed/

  4. /sync 호출 (커밋 전!)
     - 코드↔문서 동기화
     - PROJECT_STATUS.md 업데이트
     - concepts/ 문서 업데이트

  5. 일일 로그 업데이트
     - daily/YYYY-MM-DD.md

  6. 커밋 생성
     - 의미 있는 커밋 메시지
     - 코드 + 모든 문서 변경 포함
```

### 3.3 스킬 간 연계

```
[/feature] 또는 [/bugfix]
     │
     ├── [/plan] → 작업 계획
     │
     ├── [/adr] → 결정 기록 (자동 호출)
     │
     ├── [/impl] → 구현
     │      │
     │      └── [/test] → 검증
     │             │
     │             └── [/review] → 리뷰
     │
     └── [/done] → 마무리
            │
            └── [/sync] → 문서 동기화 (자동 호출)
```

---

## 4. 서브에이전트 체계 설계

### 4.1 에이전트 역할 분류

```yaml
조율자 (Orchestrator):
  tech-lead: 전체 기술 방향 결정, 에이전트 조율

계획 팀 (Planning):
  spec-writer: RFC/요구사항 작성
  architect: 설계 문서 작성, ADR 생성

개발 팀 (Development):
  backend-dev: Kotlin/Spring 개발
  frontend-dev: Vue/TypeScript 개발
  algorithm-expert: Orekit 알고리즘 전문

품질 팀 (Quality):
  code-reviewer: 코드 리뷰
  test-runner: 빌드/테스트 실행
  debugger: 디버깅 및 에러 해결

문서 팀 (Documentation):
  doc-writer: 문서 작성/업데이트
  doc-syncer: 코드↔문서 동기화
```

### 4.2 에이전트 정의

#### tech-lead (조율자)

```yaml
name: tech-lead
description: |
  기술 총괄 지휘자. 복잡한 요청 시 자동 호출.
  다른 에이전트들을 조율하고 기술 결정을 내림.
  "설계", "아키텍처", "방향", "결정" 키워드에 반응.
tools: Read, Grep, Glob, Edit, Bash
model: sonnet

역할:
  - 요청 분석 및 작업 분해
  - 적절한 에이전트 선택
  - 기술 결정 최종 판단
  - ADR 생성 지시
```

#### architect (설계)

```yaml
name: architect
description: |
  시스템 설계 전문가. 설계 문서 및 ADR 작성.
  "설계 문서", "ADR", "아키텍처 결정" 키워드에 반응.
tools: Read, Grep, Glob, Edit
model: sonnet

역할:
  - DESIGN.md 작성
  - ADR 생성
  - 영향 범위 분석
  - 대안 비교 분석
```

#### backend-dev (백엔드 개발)

```yaml
name: backend-dev
description: |
  Kotlin/Spring Boot 백엔드 개발 전문가.
  Controller, Service, Algorithm 코드 작성.
  "백엔드", "API", "서비스", "컨트롤러" 키워드에 반응.
tools: Read, Grep, Glob, Edit, Bash
model: sonnet

역할:
  - Kotlin 코드 작성
  - KDoc 주석 작성
  - Spring WebFlux 패턴 적용
  - Orekit 연동
```

#### frontend-dev (프론트엔드 개발)

```yaml
name: frontend-dev
description: |
  Vue 3/TypeScript 프론트엔드 개발 전문가.
  컴포넌트, 스토어, 페이지 코드 작성.
  "프론트엔드", "Vue", "컴포넌트", "페이지" 키워드에 반응.
tools: Read, Grep, Glob, Edit, Bash
model: sonnet

역할:
  - Vue 컴포넌트 작성
  - Pinia 스토어 작성
  - TypeScript 타입 정의
  - Quasar 컴포넌트 활용
```

#### algorithm-expert (알고리즘 전문)

```yaml
name: algorithm-expert
description: |
  위성/태양 추적 알고리즘 전문가. Orekit 라이브러리 활용.
  좌표 변환, 궤도 계산, 추적 알고리즘 구현.
  "알고리즘", "Orekit", "위성 추적", "좌표" 키워드에 반응.
tools: Read, Grep, Glob, Edit
model: sonnet

역할:
  - Orekit 기반 계산 로직
  - 좌표계 변환
  - 추적 알고리즘 최적화
  - 알고리즘 문서화
```

#### code-reviewer (코드 리뷰)

```yaml
name: code-reviewer
description: |
  코드 품질 검토 전문가. 코드 리뷰 및 개선 제안.
  "리뷰", "검토", "품질" 키워드에 반응.
tools: Read, Grep, Glob
model: sonnet

역할:
  - 코드 품질 검사
  - 패턴 일관성 확인
  - 보안 취약점 검토
  - 개선 제안
```

#### doc-syncer (문서 동기화)

```yaml
name: doc-syncer
description: |
  코드와 문서 동기화 전문가. /sync 스킬의 핵심 에이전트.
  코드 변경 감지 및 문서 자동 업데이트.
  "동기화", "문서 업데이트" 키워드에 반응.
tools: Read, Grep, Glob, Edit, Bash
model: sonnet

역할:
  - 코드↔문서 비교
  - 차이점 분석
  - 자동 문서 업데이트
  - concepts/ 문서 관리
```

### 4.3 에이전트 협업 패턴

#### 패턴 1: 신규 기능 개발

```
사용자: "위성 추적 정확도 개선해줘"
     │
     ▼
[tech-lead] 요청 분석
     │
     ├── [architect] DESIGN.md 작성, ADR 생성
     │
     ├── [algorithm-expert] 알고리즘 개선
     │
     ├── [backend-dev] 서비스 코드 수정
     │
     ├── [code-reviewer] 코드 리뷰
     │
     └── [doc-syncer] 문서 동기화
```

#### 패턴 2: 버그 수정

```
사용자: "PassSchedule 차트가 느려"
     │
     ▼
[tech-lead] 문제 분석
     │
     ├── [debugger] 원인 분석, ANALYSIS.md 작성
     │
     ├── [frontend-dev] 코드 수정
     │
     ├── [test-runner] 빌드/테스트
     │
     └── [doc-syncer] 버그픽스 문서 생성
```

---

## 5. 자동화 규칙

### 5.1 ADR 자동 생성 조건

```yaml
자동 생성 트리거:
  - 새로운 라이브러리/프레임워크 도입
  - 아키텍처 패턴 변경
  - API 인터페이스 변경
  - 데이터 구조 변경
  - 알고리즘 변경
  - 성능 최적화 방법 결정

ADR 템플릿:
  ---
  번호: ADR-NNN
  제목: {결정 제목}
  상태: 제안됨 | 승인됨 | 폐기됨 | 대체됨
  날짜: YYYY-MM-DD
  ---

  ## 컨텍스트
  {왜 이 결정이 필요한가}

  ## 결정
  {무엇을 결정했는가}

  ## 대안
  {고려한 다른 옵션들}

  ## 결과
  {이 결정의 영향}
```

### 5.2 문서 자동 이동 규칙

```yaml
/done 실행 시:
  - features/active/{기능}/ → features/completed/{기능}/
  - bugfixes/active/{버그}/ → bugfixes/completed/{버그}/
  - PROGRESS.md → IMPLEMENTATION.md로 변환
  - 관련 concepts/ 문서 업데이트

/sync 실행 시:
  - 코드 변경 → api/ 문서 자동 업데이트
  - 새 Controller → api/controllers/ 문서 생성
  - 새 Algorithm → concepts/algorithms/ 문서 업데이트
```

### 5.3 Why 기록 자동화

```yaml
모든 변경에 Why 기록:
  - git commit 메시지에 이유 포함
  - ADR에 컨텍스트 기록
  - CHANGELOG.md에 변경 이유 기록
  - daily/ 로그에 작업 이유 기록

예시:
  ## 2026-01-05 변경 사항

  ### EphemerisService 수정
  **Why**: 추적 정확도가 0.1도에서 0.01도로 개선 필요
  **What**: OrekitCalculator 계산 주기를 100ms → 50ms로 변경
  **Impact**: CPU 사용량 10% 증가, 정확도 10배 향상
```

---

## 6. 구현 계획

### 6.1 Phase 1: 기반 구축 (1주)

```yaml
작업:
  1. 문서 폴더 구조 생성
     - docs/concepts/, decisions/, bugfixes/, daily/, status/

  2. 핵심 스킬 3개 구현
     - /sync (완료)
     - /feature
     - /done

  3. 핵심 에이전트 3개 정의
     - tech-lead
     - doc-syncer
     - code-reviewer
```

### 6.2 Phase 2: 확장 (2주)

```yaml
작업:
  1. 추가 스킬 구현
     - /bugfix, /adr, /plan, /impl, /test, /review

  2. 추가 에이전트 정의
     - architect, backend-dev, frontend-dev, algorithm-expert

  3. 템플릿 생성
     - ADR 템플릿
     - RFC 템플릿
     - DESIGN 템플릿
```

### 6.3 Phase 3: 자동화 (2주)

```yaml
작업:
  1. 자동 문서 생성 규칙 구현
  2. 스킬 간 연계 자동화
  3. ADR 자동 생성 로직
  4. concepts/ 문서 자동 업데이트
```

---

## 7. 기대 효과

### 7.1 정량적 효과

| 항목 | Before | After |
|------|--------|-------|
| 문서 최신화 소요 시간 | 30분+ (수동) | 5분 (/sync) |
| 기능 개발 문서화율 | 30% | 100% |
| ADR 작성률 | 0% | 100% (자동) |
| 버그 원인 분석 기록 | 없음 | 100% |
| 코드↔문서 불일치 | 많음 | 0% (자동 동기화) |

### 7.2 정성적 효과

```yaml
개발자 경험:
  - 문서 작성 부담 감소 (자동화)
  - 일관된 문서 품질
  - 명확한 개발 워크플로우

지식 관리:
  - 모든 결정 이유 기록
  - 시스템 이해도 향상
  - 신규 팀원 온보딩 용이

유지보수:
  - 버그 재발 방지 (원인 분석 기록)
  - 기술 부채 추적
  - 코드 히스토리 완벽 보존
```

---

## 8. 참조 문서

### 업계 Best Practices

- [PubNub - Best practices for Claude Code sub-agents](https://www.pubnub.com/blog/best-practices-for-claude-code-sub-agents/)
- [Anthropic - Skill authoring best practices](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices)
- [Spotify - When Should I Write an ADR](https://engineering.atspotify.com/2020/04/when-should-i-write-an-architecture-decision-record)
- [Pragmatic Engineer - RFCs and Design Docs](https://newsletter.pragmaticengineer.com/p/rfcs-and-design-docs)
- [AWS - ADR Process](https://docs.aws.amazon.com/prescriptive-guidance/latest/architectural-decision-records/adr-process.html)

### Claude Code 공식 문서

- [Claude Code Skills](https://code.claude.com/docs/en/skills)
- [Claude Code Subagents](https://code.claude.com/docs/en/sub-agents)
- [Claude Code Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)

---

**문서 버전**: 1.0.0
**작성자**: Claude Code
**승인 대기**: 사용자 검토 필요
