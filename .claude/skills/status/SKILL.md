---
name: reporting-status
description: 프로젝트 현황 보고 스킬. 코드 메트릭스, 진행 상황, 기술 부채 분석. "현황", "status", "상태", "진행 상황", "어디까지" 키워드에 반응.
---

# Reporting Status - 프로젝트 현황 보고 스킬

## 역할

프로젝트의 현재 상태를 다각도로 분석하고 보고합니다.

**핵심 가치:**
- 실시간 코드 메트릭스
- 진행 상황 가시화
- 기술 부채 추적
- 문서↔코드 일치도

## 보고서 종류

| 보고서 | 명령 | 내용 |
|--------|------|------|
| 전체 현황 | `/status` | 종합 보고서 |
| 코드 메트릭스 | `/status code` | 파일 수, 라인 수 |
| 진행 상황 | `/status progress` | 진행중/완료 작업 |
| 문서 상태 | `/status docs` | 문서↔코드 일치도 |
| 기술 부채 | `/status debt` | 개선 필요 항목 |

## 전체 현황 보고서

```markdown
# ACS 프로젝트 현황 보고

**보고일**: YYYY-MM-DD HH:MM

---

## 1. 코드 메트릭스

### Backend (Kotlin)
| 항목 | 수량 | 비고 |
|------|------|------|
| Controller | N개 | |
| Service | N개 | |
| Algorithm | N개 | |
| DTO | N개 | |
| Config | N개 | |
| 총 라인 수 | N줄 | .kt 파일 |

### Frontend (Vue/TypeScript)
| 항목 | 수량 | 비고 |
|------|------|------|
| Pages | N개 | |
| Components | N개 | |
| Stores | N개 | |
| Services | N개 | |
| Composables | N개 | |
| 총 라인 수 | N줄 | .vue + .ts |

### 대형 파일 (1000줄+)
| 파일 | 줄 수 | 리팩토링 필요 |
|------|-------|--------------|
| EphemerisService.kt | 4,986 | 🟠 권장 |
| PassSchedulePage.vue | 4,841 | 🟠 권장 |
| icdStore.ts | 2,971 | 🟡 검토 |

---

## 2. 진행 상황

### 진행 중 작업
| 작업 | 위치 | 진행률 | 담당 |
|------|------|--------|------|
| {작업1} | work/active/ | N% | |
| {작업2} | work/active/ | N% | |

### 최근 완료 (7일)
| 작업 | 완료일 | 유형 |
|------|--------|------|
| {작업1} | YYYY-MM-DD | Feature |
| {작업2} | YYYY-MM-DD | Bugfix |

### 대기 중
- 📋 {예정 작업 1}
- 📋 {예정 작업 2}

---

## 3. 문서 상태

### 문서↔코드 일치도
| 영역 | 일치율 | 상태 |
|------|--------|------|
| API 문서 | N% | ✅/⚠️ |
| 아키텍처 문서 | N% | ✅/⚠️ |
| 알고리즘 문서 | N% | ✅/⚠️ |

### 누락 문서
- ❌ {문서화 안 된 컴포넌트}

### 오래된 문서
- ⚠️ {30일+ 미업데이트 문서}

---

## 4. 기술 부채

### 높음 (즉시 처리)
- 🔴 {항목}

### 중간 (이번 달 처리)
- 🟠 {항목}

### 낮음 (백로그)
- 🟡 {항목}

---

## 5. ADR 현황

| ADR | 상태 | 관련 |
|-----|------|------|
| ADR-001 | 승인됨 | Train/Tilt |
| ADR-002 | 승인됨 | Orekit |

---

## 6. 권장 조치

### 즉시
1. {권장 사항 1}

### 이번 주
1. {권장 사항 2}

### 이번 달
1. {권장 사항 3}
```

## 메트릭스 수집 방법

### 코드 메트릭스

```bash
# Backend 파일 수
find backend/src -name "*.kt" | wc -l

# Backend Controller 수
find backend/src -path "*controller*" -name "*.kt" | wc -l

# Backend Service 수
find backend/src -path "*service*" -name "*.kt" | wc -l

# Frontend 파일 수
find frontend/src -name "*.vue" | wc -l
find frontend/src -name "*.ts" | wc -l

# 대형 파일 (1000줄+)
find . -name "*.kt" -o -name "*.vue" -o -name "*.ts" | xargs wc -l | sort -rn | head -20

# 총 라인 수
find backend/src -name "*.kt" | xargs wc -l | tail -1
find frontend/src \( -name "*.vue" -o -name "*.ts" \) | xargs wc -l | tail -1
```

### 진행 상황

```bash
# 진행 중 작업
ls docs/work/active/

# 완료 작업 (최근 7일)
find docs/work/archive -mtime -7 -type d
```

### 문서 상태

```bash
# 문서 목록
find docs -name "*.md" | wc -l

# 오래된 문서 (30일+)
find docs -name "*.md" -mtime +30
```

## 출력 형식

### 간략 버전 (`/status brief`)

```
📊 ACS 현황 (2026-01-06)

Backend: 9 Controllers, 13 Services
Frontend: 5 Pages, 43 Components, 25 Stores

진행중: 2 features, 1 bugfix
완료(7일): 3 작업

문서 일치율: 85%
기술부채: 🔴 0 | 🟠 3 | 🟡 7
```

### 상세 버전 (`/status`)

전체 보고서 형식 (위 템플릿)

### JSON 형식 (`/status json`)

```json
{
  "date": "2026-01-06",
  "metrics": {
    "backend": {
      "controllers": 9,
      "services": 13,
      "algorithms": 7,
      "totalLines": 25000
    },
    "frontend": {
      "pages": 5,
      "components": 43,
      "stores": 25,
      "totalLines": 35000
    }
  },
  "progress": {
    "active": {
      "features": 2,
      "bugfixes": 1
    },
    "completedWeek": 3
  },
  "docs": {
    "matchRate": 85,
    "missing": 3,
    "stale": 2
  },
  "debt": {
    "high": 0,
    "medium": 3,
    "low": 7
  }
}
```

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `project-manager` | 진행 상황 | 메인 |
| `doc-syncer` | 문서 상태 | 문서 분석 |
| `code-reviewer` | 기술 부채 | 코드 분석 |

## 자동화

### 정기 보고서

```yaml
일일 보고서:
  - 시간: 매일 18:00
  - 저장: docs/logs/YYYY-MM-DD.md
  - 내용: 간략 현황 + 당일 작업

주간 보고서:
  - 시간: 매주 금요일 17:00
  - 저장: docs/status/weekly-YYYY-WNN.md
  - 내용: 상세 현황 + 주간 변화
```

### 변경 감지 보고

```yaml
트리거:
  - 새 Controller 추가
  - 대형 파일 증가
  - 문서 일치율 하락

알림:
  - "⚠️ 새 Controller 추가됨: XxxController.kt"
  - "⚠️ 문서 업데이트 필요: API 문서"
```

## 사용 예시

### 예시 1: 전체 현황

```
사용자: "/status" 또는 "현황 보여줘"

→ 전체 보고서 생성 및 표시
```

### 예시 2: 코드만

```
사용자: "/status code"

→ 코드 메트릭스만 표시:

📊 코드 메트릭스 (2026-01-06)

Backend: 9 Controllers, 13 Services, 7 Algorithms
         총 25,000줄

Frontend: 5 Pages, 43 Components, 25 Stores
          총 35,000줄

대형 파일 (1000줄+): 12개
```

### 예시 3: 진행 상황

```
사용자: "지금 뭐 하고 있는지 보여줘"

→ 진행 상황 보고:

🚧 진행 중 작업
1. Architecture_Refactoring (30%)
2. PassSchedule_Chart_Optimization (70%)

✅ 최근 완료
- Position_View_Update (2026-01-05)
```

## 트러블슈팅

### Q: 메트릭스가 정확하지 않아요
A: .gitignore 제외 파일 확인. node_modules, build 폴더 제외 필요.

### Q: 진행률 계산이 안 돼요
A: PROGRESS.md의 체크박스로 계산. 체크박스 형식 확인.

### Q: 문서 일치율 계산 방법은?
A: Controller 수 vs API 문서 수, Service 수 vs concepts 문서 수 비교.

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-06
**호환:** ACS 프로젝트 전용
