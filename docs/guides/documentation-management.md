# 문서 관리 가이드

> 프로젝트 문서화 체계와 작업 폴더 관리 표준

## 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **Single Source** | `CURRENT_STATUS.md`가 유일한 현재 상태 소스 |
| **계층적 참조** | CHANGELOG → logs → work/active |
| **표준화된 구조** | 모든 작업 폴더는 동일한 파일 구조 |
| **완료 시 아카이브** | 완료된 작업은 archive/로 이동 |

---

## 문서 구조

```
docs/
├── work/
│   ├── CURRENT_STATUS.md    # 현재 상태 (Single Source)
│   ├── active/              # 진행 중 작업
│   │   └── {Feature_Name}/  # 표준 작업 폴더
│   └── archive/             # 완료된 작업
├── logs/
│   └── YYYY-MM-DD.md        # 일일 로그
├── guides/                  # 개발 가이드
├── architecture/            # 아키텍처 문서
│   ├── adr/                 # Architecture Decision Records
│   └── context/             # 컨텍스트 문서
└── handbook/                # 팀 핸드북
```

---

## 표준 작업 폴더 구조

### 필수 파일 (4개)

| 파일 | 용도 | 필수 |
|------|------|:----:|
| **README.md** | 개요, 요구사항, 현재 상태 요약 | O |
| **DESIGN.md** | 설계 결정, 심층 분석, Why | △ |
| **PROGRESS.md** | 체크리스트, 일일 로그 | O |
| **FIX.md** | 버그 수정 기록 | △ |

> △ = 필요시 생성

### 폴더 이름 규칙

```
{Category}_{Feature_Name}

예시:
- Tracking_Schema_Redesign
- HW_Error_System_Integration
- Timezone_Handling_Standardization
- Admin_Panel_Separation
```

**Category 예시:**
- `Feature_` - 새 기능
- `Bugfix_` - 버그 수정
- `Refactor_` - 리팩토링
- `Optimize_` - 성능 최적화

---

## 파일별 템플릿

### README.md

```markdown
# {작업명}

## 개요

**목적**: {한 줄 설명}
**요청일**: YYYY-MM-DD
**상태**: {상태 이모지 + 텍스트}
**접근법**: {간략한 접근 방식}

## 요구사항

- [ ] 요구사항 1
- [ ] 요구사항 2

## 진행 상황

| Phase | 상태 | 내용 |
|-------|:----:|------|
| Phase 1 | ⏳ | {내용} |

## 관련 문서

- [PROGRESS.md](PROGRESS.md)
- [DESIGN.md](DESIGN.md)
```

### DESIGN.md

```markdown
# {작업명} 설계 문서

## 문제 정의

{현재 문제점 상세 설명}

## 설계 결정

### 결정 1: {제목}

**선택**: {선택한 옵션}
**이유**: {왜 이 옵션을 선택했는지}

| 옵션 | 장점 | 단점 |
|------|------|------|
| A | ... | ... |
| B | ... | ... |

## 구현 계획

{단계별 구현 계획}

## 롤백 계획

{문제 발생 시 복구 방법}
```

### PROGRESS.md

```markdown
# {작업명} 진행 상황

## 진행률: {N}%

## 작업 체크리스트

### Phase 1: {이름}
- [ ] 작업 1
- [ ] 작업 2

### Phase 2: {이름}
- [ ] 작업 3

## 일일 로그

### YYYY-MM-DD
- [x] 완료한 작업
- [ ] 다음 할 일
```

### FIX.md

```markdown
# {작업명} 버그 수정 기록

## 수정 목록

### FIX-001: {버그 제목}

**발견일**: YYYY-MM-DD
**상태**: 수정 완료 / 진행 중

**증상**: {어떤 문제가 발생했는지}
**원인**: {근본 원인}
**수정**: {어떻게 수정했는지}

**변경 파일**:
- `path/to/file.ts` - {변경 내용}
```

---

## 상태 표기

| 이모지 | 상태 |
|:------:|------|
| ✅ | 완료 |
| ⏳ | 진행 중 |
| ⏸️ | 보류 |
| ❌ | 취소 |
| 📋 | 계획됨 |

---

## 작업 라이프사이클

```
1. 작업 시작
   └─ docs/work/active/{Feature_Name}/ 폴더 생성
   └─ README.md, PROGRESS.md 작성

2. 설계 (필요시)
   └─ DESIGN.md 작성
   └─ ADR 필요하면 docs/architecture/adr/에 생성

3. 구현
   └─ PROGRESS.md 업데이트 (체크리스트)
   └─ 버그 발생 시 FIX.md에 기록

4. 완료
   └─ PROGRESS.md: 100% 업데이트
   └─ CURRENT_STATUS.md: "최근 완료" 섹션에 추가
   └─ docs/logs/YYYY-MM-DD.md: 일일 로그에 기록

5. 아카이브
   └─ /done 실행 시 자동 감지 + 제안
   └─ 폴더를 docs/work/archive/로 이동
```

---

## 아카이브 기준

| 조건 | 설명 |
|------|------|
| **진행률 100%** | PROGRESS.md에서 100% 표시 |
| **30일 미업데이트** | 마지막 수정일 기준 |
| **명시적 완료** | README.md 상태가 "✅ 완료" |

**자동 감지 조건 (하나라도 만족 시):**
```yaml
조건 1: PROGRESS.md에 "진행률: 100%" 포함
조건 2: README.md에 "상태: ✅ 완료" 포함
조건 3: README.md에 "Status: Completed" 포함
```

**아카이브 시점:**
- `/done` 실행 시 자동 감지
- 아카이브 여부 질문 (예: "Admin_Panel_Separation 완료됨. 아카이브할까요?")
- 승인 시 `git mv`로 `archive/`로 자동 이동
- 커밋에 포함

**아카이브 후:**
- `archive/` 폴더에서 참조 가능
- CURRENT_STATUS.md에서 해당 작업 제거
- 검색/참조용으로 유지 (삭제 안 함)

---

## 문서 재활성화 (Archive → Active)

완료된 기능에 버그 발생 또는 추가 작업 시 이전 문서를 재활성화합니다.

**사용 방법:**
```
/bugfix "Admin Panel에서 다이얼로그 안 열림"
/feature "Admin Panel에 로그아웃 버튼 추가"
```

**자동 동작:**
1. 키워드로 `archive/` 폴더 검색
2. 매칭되는 문서 발견 시 재활성화 질문
3. 승인 시 `archive/` → `active/`로 이동
4. FIX.md 또는 PROGRESS.md 자동 업데이트

**재활성화 후 문서 상태:**
```
docs/work/active/Admin_Panel_Separation/
├── README.md      ← 상태: "🚧 진행 중"으로 변경
├── PROGRESS.md    ← 새 Phase 추가
├── DESIGN.md      ← 기존 유지
└── FIX.md         ← 새 버그 항목 추가
```

**FIX.md 자동 추가 형식:**
```markdown
### FIX-002: {버그 제목}

**발견일**: YYYY-MM-DD
**상태**: 진행 중

**증상**: {버그 설명}
```

---

## 자동화 지원

### /done 스킬 실행 시

1. 빌드 검증 (FE/BE)
2. 변경사항 분석
3. 문서 업데이트:
   - CHANGELOG.md
   - docs/logs/YYYY-MM-DD.md
   - CURRENT_STATUS.md
4. 커밋 생성

### /health 스킬 실행 시

- 깨진 링크 검사
- 아카이브 후보 탐색 (30일 이상 미업데이트)
- 문서 일관성 검사

---

## 빠른 참조

### 새 작업 시작

```bash
# 1. 폴더 생성
mkdir docs/work/active/{Feature_Name}

# 2. 템플릿 복사
cp docs/work/templates/README.template.md docs/work/active/{Feature_Name}/README.md
cp docs/work/templates/PROGRESS.template.md docs/work/active/{Feature_Name}/PROGRESS.md

# 3. 필요시 추가 템플릿
cp docs/work/templates/DESIGN.template.md docs/work/active/{Feature_Name}/DESIGN.md
cp docs/work/templates/FIX.template.md docs/work/active/{Feature_Name}/FIX.md
```

### 템플릿 위치

```
docs/work/templates/
├── README.template.md
├── PROGRESS.template.md
├── DESIGN.template.md
└── FIX.template.md
```

### 완료 후

```
/done  # 자동으로 문서 업데이트 + 커밋
```

### 상태 확인

```
/status  # CURRENT_STATUS.md 기반 현황 보고
```

---

## 고도화 방안 (적용 예정)

> 이전 전문가 검토에서 도출된 개선 방안

| 항목 | 상태 | 설명 |
|------|:----:|------|
| CLAUDE.md 규칙 추가 | ✅ | 4줄 문서 관리 규칙 |
| /done 헬스체크 | ✅ | 아카이브 후보 + 링크 검사 |
| 템플릿 폴더 | ✅ | docs/work/templates/ |
| 자동 아카이브 | ✅ | 완료 감지 → 질문 → 승인 시 이동 |
| 문서 재활성화 | ✅ | /bugfix 시 archive → active 이동 |
| /sync → /health 통합 | ⏳ | 문서 동기화를 health에 포함 |
| legacy/ 정리 | ⏳ | archive/historical/로 이동 |

### 향후 고려 사항

1. **대형 프로젝트 관리**
   - `active/{Project}/completed/` 서브폴더로 Phase별 완료 관리
   - 예: PassSchedule처럼 17개 이슈가 있는 경우

2. **자동 문서 생성**
   - `/feature`, `/bugfix` 실행 시 자동으로 작업 폴더 생성
   - 템플릿 기반 README.md, PROGRESS.md 초기화

3. **주기적 정리 알림**
   - `/health` 실행 시 오래된 active 폴더 알림
   - 30일 이상 미업데이트 항목 리스트

---

## 관련 문서

- [CURRENT_STATUS.md](../work/CURRENT_STATUS.md)
- [CLAUDE.md](../../CLAUDE.md) - 문서 규칙 섹션
- [UNIFIED_WORKFLOW.md](../../.claude/skills/UNIFIED_WORKFLOW.md) - /done 상세