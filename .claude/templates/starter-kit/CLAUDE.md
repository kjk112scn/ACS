# {프로젝트명}

{한 줄 설명}

## ⚠️ CRITICAL - 미해결 이슈 (매 세션 확인)

> **이 섹션은 개발 완료 전 반드시 해결해야 할 이슈입니다.**

| 이슈 | 심각도 | 영향 | 상태 |
|------|--------|------|------|
| {이슈1} | CRITICAL | {영향} | ⏳ 대기 |
| {이슈2} | HIGH | {영향} | ⏳ 대기 |

**리마인더:** 새 기능 개발 시 이 이슈들의 우선순위를 고려하세요.

---

## 기술 스택

| 영역 | 기술 |
|-----|-----|
| Frontend | {프레임워크} |
| Backend | {언어/프레임워크} |
| Database | {DB} |
| 통신 | REST API, WebSocket |

## 빌드 명령어

```bash
# Frontend
cd frontend && npm run dev      # 개발 서버
cd frontend && npm run build    # 빌드

# Backend
cd backend && ./gradlew bootRun              # 실행
cd backend && ./gradlew clean build -x test  # 빌드
```

---

## IMPORTANT - 핵심 규칙

| 규칙 | 설명 |
|------|------|
| {규칙1} | {설명} |
| {규칙2} | {설명} |

## PROHIBITED - 금지 사항

```
하드코딩 비밀번호/키  → 환경변수 사용
console.log          → logger 사용 (production)
광범위 catch         → 구체적 예외 타입
```

---

## Claude 행동 규칙

| 상황 | 규칙 |
|------|------|
| **작업 시작 전** | 뭘 할지 먼저 한줄 안내 → 바로 실행 |
| **방향 결정 질문** | 바로 답 X → 분석/검토 먼저 |
| **스킬/에이전트 사용** | 아래 안내 형식 준수 |
| **문서 생성** | 기존 구조 확인 → 통합 우선 (새 파일 최소화) |
| **결과 보고** | 표 형식 선호, 간결하게 |
| **긴 설명 필요 시** | 먼저 요약 → 상세는 접어서 |

### 스킬/에이전트 실행 안내

**스킬 실행 시:**
```
🔧 스킬 실행: /done
   └─ 워크플로우: 빌드검증 → 문서정리 → 커밋
```

**에이전트 호출 시:**
```
🤖 에이전트 호출: architect, doc-syncer (병렬)
   └─ 목적: 문서 구조 검토
```

---

## 컨텍스트 로딩 (작업 전 참조)

| 작업 유형 | 필수 참조 문서 |
|----------|---------------|
| BE 수정 | `docs/architecture/context/architecture/backend.md` |
| FE 수정 | `docs/architecture/context/architecture/frontend.md` |
| 도메인 작업 | `docs/architecture/context/domain/{domain}.md` |
| 진행중 작업 | `docs/work/active/` |

---

## 문서 관리 규칙

| 규칙 | 설명 |
|------|------|
| **Single Source** | `CURRENT_STATUS.md`만 현재 상태 관리 |
| **작업 폴더** | `docs/work/active/{Feature_Name}/` 표준 구조 사용 |
| **필수 파일** | README.md, PROGRESS.md (DESIGN.md, FIX.md는 필요시) |
| **완료 시** | `/done` 실행 → CHANGELOG, logs, CURRENT_STATUS 자동 업데이트 |
| **아카이브** | 100% 완료 → `/archive` 또는 자동 제안 |

### 문서 구조

```
docs/
├── work/
│   ├── CURRENT_STATUS.md    # 📍 현재 상태 (Single Source)
│   ├── active/              # 진행 중 작업
│   └── archive/             # 완료된 작업
├── logs/
│   └── YYYY-MM-DD.md        # 일일 작업 로그
└── CHANGELOG.md             # 버전별 변경 요약
```

> **상세:** `docs/guides/UNIFIED_WORKFLOW.md`

---

## 효과적인 요청 패턴

**Research → Plan → Implement** 순서로 진행

```
# 좋은 요청 (구체적 + 컨텍스트 지정)
"{기능} 추가해줘.
먼저 {관련문서}.md 읽고,
{관련파일} 구조 파악 후 계획 세워줘"

# 피해야 할 요청 (모호함)
"기능 추가해줘"
```

---

## 스킬 명령어

### 워크플로우

| 스킬 | 설명 |
|------|------|
| `/feature` | 새 기능 개발 (폴더 생성 → 문서화 → 구현) |
| `/bugfix` | 버그 수정 워크플로우 (분석 → 수정 → 검증) |
| `/refactor` | 리팩토링 (파일 분리, 구조 개선) |
| `/optimize` | 성능 최적화 |
| `/cleanup` | 코드 정리 (console.log, 하드코딩 등) |
| `/done` | 작업 완료 + 문서화 + 커밋 |

### 상태 확인

| 스킬 | 설명 |
|------|------|
| `/health` | 빌드/품질/문서 종합 점검 |
| `/status` | 프로젝트 현황 보고 |
| `/sync` | 문서 동기화 + 링크 검증 |

### 문서화

| 스킬 | 설명 |
|------|------|
| `/plan` | 작업 계획 수립 |
| `/adr` | 아키텍처 결정 기록 |
| `/docs` | 코드 분석 기반 문서 생성 |

### 문서 관리

| 스킬 | 설명 |
|------|------|
| `/archive` | 완료 작업 아카이브 |
| `/revive` | 아카이브 문서 부활 |

### 도움말

| 스킬 | 설명 |
|------|------|
| `/guide` | 스킬/에이전트 사용법 안내 |

---

## 작업 완료 시

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 다음에 쓸 수 있는 명령어
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔧 워크플로우
  /feature   새 기능 개발
  /bugfix    버그 수정
  /refactor  리팩토링
  /done      작업 완료 + 커밋

📊 상태 확인
  /health    빌드/품질 점검
  /status    프로젝트 현황
  /sync      문서 동기화

📝 문서화
  /plan      계획 수립
  /adr       아키텍처 결정 기록

📦 문서 관리
  /archive   완료 작업 보관
  /revive    아카이브 부활

💡 /guide 로 상세 안내
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```