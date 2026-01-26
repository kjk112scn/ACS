# ACS (Antenna Control System)

위성/태양 추적 안테나 제어 시스템

## ⚠️ CRITICAL - 미해결 이슈 (매 세션 확인)

> **이 섹션은 개발 완료 전 반드시 해결해야 할 이슈입니다.**

| 이슈 | 심각도 | 영향 | 상태 |
|------|--------|------|------|
| **인증/인가 미구현** | CRITICAL | 모든 API 무단 접근 가능 | ⏳ 대기 |
| **테스트 커버리지 0%** | CRITICAL | 회귀 버그 위험 | ⏳ 대기 |

**조치 계획:**
- 인증: Spring Security + JWT (개발 80% 완료 후)
- 테스트: 핵심 Service/Algorithm 단위 테스트 우선

**리마인더:** 새 기능 개발 시 이 이슈들의 우선순위를 고려하세요.

---

## IMPORTANT - 핵심 규칙

| 규칙 | 설명 |
|------|------|
| **각도 단위** | 내부 라디안, 표시 도(°) |
| **시간** | 내부 UTC, 표시 로컬 |
| **Train = Tilt** | 변수명 `train`, UI는 `Tilt` |
| **Orekit** | 초기화 필요 (orekit-data) |

## PROHIBITED - 금지 사항

```
!! 연산자       → ?: 또는 requireNotNull()
catch(Exception) → 구체적 예외 타입
하드코딩 색상    → var(--theme-*)
println()       → logger.debug()
```

<!-- @common-rules: start -->
<!-- version: 1.1.0 - 안전 패턴 4개 추가 (Scope Guard, Risk, Uncertainty, Verification) -->
## Claude 행동 규칙

| 상황 | 규칙 |
|------|------|
| **작업 시작 전** | 한줄 안내 → 바로 실행 *(예외: 🔴High, 5개+파일, [확인필요])* |
| **방향 결정 질문** | 바로 답 X → 분석/검토 먼저 |
| **스킬/에이전트 사용** | 아래 안내 형식 **MUST** 준수 |
| **문서 생성** | 기존 구조 확인 → 통합 우선 (새 파일 최소화) |
| **결과 보고** | 표 형식 선호, 간결하게 |
| **긴 설명 필요 시** | 먼저 요약 → 상세는 접어서 |
| **작업 완료 시** | 세션 요약 **MUST** 출력 |

### ⚠️ MUST - 필수 출력 형식

#### 1. 스킬 실행 시 (MUST)
```
🔧 스킬 실행: /done
   └─ 워크플로우: 빌드검증 → 문서정리 → 커밋

📊 Task 현황: 3/5 완료
   ├─ ✅ T001 설계 완료
   ├─ ✅ T002 구현 완료
   ├─ 🔄 T003 테스트 (진행중)
   ├─ ⏳ T004 검증 [depends: T003]
   └─ ⏳ T005 배포 [depends: T004]
```

#### 2. 에이전트 호출 시 (MUST)
```
🤖 에이전트 호출: architect, doc-syncer (병렬)
   └─ 목적: 문서 구조 검토

📊 병렬 실행 상태:
   ├─ 🔄 architect: 분석중...
   ├─ 🔄 doc-syncer: 분석중...
   └─ ⏳ tech-lead: 대기 [depends: architect]
```

#### 3. 전문가 검토 시 (MUST)
```
📋 전문가 검토 시작
   ├─ architect: 스킬 시스템 검토
   ├─ doc-syncer: 문서 구조 검토
   └─ tech-lead: 워크플로우 검토

📊 병렬 실행: 3개 동시 진행
```

#### 4. 작업 완료 시 세션 요약 (MUST)

> **모든 작업 완료 후 반드시 출력. 사용자가 위로 스크롤하지 않도록!**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📝 세션 요약
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔹 질문: [사용자가 요청한 내용 1줄 요약]
🔹 작업: [클로드가 수행한 작업 요약]
🔹 결과: [최종 결과물 - 파일 경로, 상태, 변경사항 등]

📊 Task 최종 상태: [완료/진행중] (X/Y)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 🛡️ 안전 패턴 (Task Management 호환)

#### 1. Scope Guard - 범위 보호

> **원칙:** "bonus" 개선 금지, 필요한 연쇄 수정은 보고 후 진행

| 상황 | 행동 |
|------|------|
| 요청된 파일만 수정 필요 | ✅ 바로 진행 |
| 연관 파일 수정 발견 | ⚠️ Task에 추가 + 보고 후 진행 |
| 문서 통합 (기존에 추가) | ✅ 바로 진행 *(문서 규칙 우선)* |
| PROGRESS.md, STATUS 업데이트 | ✅ 바로 진행 *(추적 파일 예외)* |
| "bonus" 리팩토링 유혹 | ❌ 금지 (별도 요청 필요) |
| 미사용 코드 정리 유혹 | ❌ 금지 (/cleanup으로 별도 요청) |

```
⚠️ 추가 파일 발견
   파일: PassScheduleDTO.kt
   이유: 타입 변경으로 인한 연쇄 수정
   → Task에 추가됨. 진행할까요?
```

#### 2. 위험도 표시 - Risk Indicator

| 레벨 | 트리거 조건 | 행동 |
|:----:|------------|------|
| 🟢 Low | 1-2개 파일, 로직 변경 없음 | 바로 진행 |
| 🟡 Medium | 3-5개 파일, 로직 변경 있음 | 요약 후 진행 |
| 🔴 High | 6개+ 파일, DB/API 변경, 삭제 | **확인 대기** |

**High 위험 작업 목록 (확인 필수):**
```
- git push --force, git reset --hard
- DROP TABLE, DELETE FROM (조건 없음)
- rm -rf, 폴더 전체 삭제
- API 엔드포인트 삭제/이름 변경
- 프로덕션 설정 파일 수정
```

```
🔴 HIGH RISK 작업 감지
   작업: API 엔드포인트 /api/schedule 삭제
   영향: FE 3개 파일에서 호출 중
   → 진행하려면 "YES" 입력
```

#### 3. 불확실성 태그 - Uncertainty Markers

| 태그 | 의미 | 사용 시점 |
|------|------|----------|
| `[확실]` | 검증됨, 문서/코드 확인함 | 기본 (생략 가능) |
| `[추정]` | 코드 패턴 기반 추론 | 문서 없이 코드만 본 경우 |
| `[확인필요]` | 여러 해석 가능 | 사용자 결정 필요 |
| `[미검증]` | 실행/테스트 안 함 | 빌드 전 제안 |

```
[추정] 이 함수는 UTC 시간을 반환하는 것으로 보입니다.
       근거: 변수명 `utcTime`, 유사 함수 패턴

[확인필요] 삭제할 파일이 2개 있습니다:
  1. OldService.kt - 참조 0개 [추정]
  2. LegacyDTO.kt - 참조 0개 [추정]
  → 삭제 전 확인해주세요.
```

#### 4. Verification Loop - 변경 전 확인

| 조건 | 확인 방식 |
|------|----------|
| 파일 5개 이상 수정 | 변경 파일 목록 먼저 제시 |
| 100줄 이상 변경 | diff 요약 제공 |
| 삭제 작업 | 삭제 대상 명시 + 확인 |
| 실패 2회 연속 | 다른 접근법 제안 |

```
📋 변경 예정 (7개 파일)
   ├─ PassScheduleService.kt (+15, -8)
   ├─ PassScheduleDTO.kt (+5, -2)
   ├─ ...
   └─ 총 변경: +45줄, -23줄

   → 진행할까요?
```

**실패 복구:**
```
⚠️ 2회 연속 실패
   시도 1: 타입 변환 오류
   시도 2: null 참조 오류

   다른 접근법 제안:
   1. [권장] 기존 타입 유지 + 어댑터 추가
   2. 전체 리팩토링 (영향 범위 큼)
   → 어떤 방식으로 진행할까요?
```
<!-- @common-rules: end -->

## 컨텍스트 로딩 (작업 전 참조)

| 작업 유형 | 필수 참조 문서 |
|----------|---------------|
| BE 수정 | `docs/architecture/context/architecture/backend.md` |
| FE 수정 | `docs/architecture/context/architecture/frontend.md` |
| 위성 추적 | `docs/architecture/context/domain/satellite-tracking.md` |
| ICD 통신 | `docs/architecture/context/domain/icd-protocol.md` |
| 모드 시스템 | `docs/architecture/context/domain/mode-system.md` |
| 진행중 작업 | `docs/work/active/` |

## 문서 관리 규칙

| 규칙 | 설명 |
|------|------|
| **Single Source** | `CURRENT_STATUS.md`만 현재 상태 관리 |
| **작업 폴더** | `docs/work/active/{Issue_Name}/` 표준 구조 사용 |
| **필수 파일** | README.md, PROGRESS.md (DESIGN.md, FIX.md는 필요시) |
| **완료 시** | `/done` 실행 → CHANGELOG, logs, CURRENT_STATUS 자동 업데이트 |

### 스킬 연계 규칙

| 규칙 | 설명 |
|------|------|
| **Review ID** | `#R{NNN}-{C/H/M/L}{N}` 형식 (예: #R001-C1) |
| **Origin 추적** | `/bugfix #R001-C1` → origin 자동 기록 |
| **하이브리드 구조** | 스킬 3개+ 연계 시 `phases/` 폴더 사용 |
| **WORKFLOW.md** | 복잡한 연계 시 선택적 생성 |

> **상세:** `docs/guides/documentation-management.md`
> **Task System:** `.claude/rules/task-system.md` *(PROGRESS.md 기반 추적)*
> **스킬 연계:** `.claude/rules/workflow-linkage.md` *(Review → Bugfix 흐름)*

## 효과적인 요청 패턴

**Research → Plan → Implement** 순서로 진행

```
# 좋은 요청 (구체적 + 컨텍스트 지정)
"PassSchedule 기능 추가해줘.
먼저 satellite-tracking.md 읽고,
PassScheduleService.kt 구조 파악 후 계획 세워줘"

# 피해야 할 요청 (모호함)
"기능 추가해줘"
```

## 빌드 명령어

```bash
cd frontend && npm run build              # FE 빌드
cd backend && ./gradlew clean build -x test  # BE 빌드
```

## 기술 스택

| 영역 | 기술 |
|-----|-----|
| Frontend | Vue 3 + Quasar 2.x + TypeScript + Pinia |
| Backend | Kotlin 1.9 + Spring Boot 3.x + WebFlux |
| 알고리즘 | Orekit 13.0 (위성), solarpositioning (태양) |

## 모드 시스템

| 모드 | 설명 |
|-----|-----|
| Standby | 대기 |
| Step / Slew | 이동 |
| EphemerisDesignation | 위성 궤도 지정 |
| PassSchedule | 패스 스케줄 |
| SunTrack | 태양 추적 |

## 상세 문서

| 문서 | 경로 |
|-----|-----|
| 개발 가이드 | `docs/guides/` |
| 팀 핸드북 | `docs/handbook/` |
| 시스템 개요 | `docs/architecture/SYSTEM_OVERVIEW.md` |

## 팀 핸드북 구조 (docs/handbook/)

> 기술 스택 학습 및 참조용 문서

```
handbook/
├── README.md           # 목차
├── overview/           # 시스템 전체 (FE+BE 통합)
│   ├── tech-stack.md   # 기술 스택 + 선택 이유
│   ├── data-flow.md    # FE ↔ BE ↔ HW 데이터 흐름
│   └── glossary.md     # 용어 사전
├── kotlin/             # BE 문법/패턴
│   ├── kotlin-null-safety.md
│   ├── kotlin-reactive.md
│   └── spring-annotations.md
├── vue/                # FE 문법/패턴
│   └── vue-reactivity.md
└── project/            # ACS 특화 패턴
    └── acs-patterns.md
```

**용도**:
- 새 기술 학습 시 참조
- 코드 리뷰 기준
- 문서 추가 요청 시 해당 폴더에 생성

---

## 스킬 명령어 (21개)

### 워크플로우

| 스킬 | 설명 |
|------|------|
| `/feature` | 새 기능 개발 (RFC → 설계 → 구현) |
| `/bugfix` | 버그 수정 워크플로우 (+ 관련 아카이브 자동 검색) |
| `/refactor` | 리팩토링 (파일 분리, 구조 개선) |
| `/optimize` | 성능 최적화 |
| `/cleanup` | 코드 정리 (console.log, 하드코딩 등) |
| `/done` | 작업 완료 + 문서화 + 커밋 (+ 아카이브 제안) |

### 문서 관리

| 스킬 | 설명 |
|------|------|
| `/archive` | 완료된 작업 아카이브 (수동 또는 /done에서 자동) |
| `/revive` | 아카이브 문서 부활 (버그픽스 시 자동 검색) |

### 상태 확인

| 스킬 | 설명 |
|------|------|
| `/health` | 빌드/품질/문서 종합 점검 |
| `/status` | 프로젝트 현황 보고 |
| `/sync` | 문서 동기화 + 링크 검증 |
| `/review` | 코드 심층 분석 (상태머신, 타이밍, 경쟁조건) |

### 문서화

| 스킬 | 설명 |
|------|------|
| `/plan` | 작업 계획 수립 |
| `/adr` | 아키텍처 결정 기록 |
| `/docs` | 코드 분석 기반 문서 생성 |

### 고급 자동화

| 스킬 | 설명 |
|------|------|
| `/api-sync` | FE-BE 타입 동기화 |
| `/migrate` | 마이그레이션 관리 |
| `/publish` | 스킬을 claude-dev-kit 레포에 동기화 |
| `/import` | claude-dev-kit에서 공통 규칙/스킬 가져오기 |

### 도움말

| 스킬 | 설명 |
|------|------|
| `/guide` | 스킬/에이전트 사용법 안내 |

