# Unified Workflow - 통합 작업 흐름

> 모든 작업 유형에 적용되는 표준 워크플로우

---

## 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **Single Source of Truth** | `CURRENT_STATUS.md`가 현재 상태의 유일한 진실 |
| **계층적 문서화** | CHANGELOG → logs/ → work/active/ |
| **진행률 추적** | PROGRESS.md로 세션 간 연속성 유지 |
| **아카이브 활용** | 과거 작업 재활용으로 효율 증대 |

---

## 작업 라이프사이클

```
[요청] → [계획] → [실행] → [완료] → [아카이브]
   │        │        │        │          │
   ↓        ↓        ↓        ↓          ↓
 분석    PROGRESS   코드    /done      /archive
        .md 생성   작성    문서화      보관
```

---

## Phase 1: 작업 시작

### 1.1 요청 분석

```yaml
확인 항목:
  - 작업 유형: Feature / Bugfix / Refactor / Optimize
  - 복잡도: 단순(1-2파일) / 중간(3-4파일) / 대형(5개+)
  - 영향 범위: Frontend / Backend / Both
```

### 1.2 아카이브 검색 (자동)

```
🔍 관련 과거 작업 검색 중...

검색 키워드: {작업 키워드}
검색 위치: docs/work/archive/

결과:
- Feature_Similar_Work (2024-01) - 참고 가능
- Bugfix_Related_Issue (2024-02) - 해결책 재사용
```

### 1.3 폴더 생성

```bash
docs/work/active/{Category}_{Name}/
├── README.md      # 개요, 요구사항
└── PROGRESS.md    # 진행률, 체크리스트
```

**폴더명 규칙:**
- `Feature_` - 새 기능
- `Bugfix_` - 버그 수정
- `Refactor_` - 리팩토링
- `Optimize_` - 성능 최적화

---

## Phase 2: 작업 진행

### 2.1 진행률 관리

**PROGRESS.md 업데이트:**
```markdown
## 진행률: 60%

### Phase 1: 준비 ✅
- [x] 요구사항 분석
- [x] 설계 검토

### Phase 2: 구현 🚧
- [x] 컴포넌트 A 완료
- [ ] 컴포넌트 B 진행 중
- [ ] API 연동

### Phase 3: 검증 ⏳
- [ ] 테스트
- [ ] 빌드 확인
```

### 2.2 우선순위 분류 (대형 작업)

| Phase | 설명 | 기준 |
|-------|------|------|
| **A** | Critical | 핵심 기능, 즉시 필요 |
| **B** | High | 중요하지만 A 이후 |
| **C** | Medium | 개선 사항 |
| **D** | Low | Nice-to-have |

---

## Phase 3: 작업 완료

### 3.1 완료 체크리스트

```yaml
필수:
  - [ ] 빌드 성공 (FE + BE)
  - [ ] 기능 동작 확인
  - [ ] PROGRESS.md 100% 업데이트

권장:
  - [ ] 코드 리뷰
  - [ ] 테스트 통과
```

### 3.2 /done 실행

```
/done

→ 자동 실행:
  1. 빌드 검증
  2. 변경 분석
  3. 계층적 문서화
     - CHANGELOG.md 업데이트
     - logs/YYYY-MM-DD.md 생성/업데이트
     - work/active/ 문서 완료 표시
  4. CURRENT_STATUS.md 업데이트
  5. 아카이브 검토 (100% 완료 시)
  6. 커밋 생성
```

---

## Phase 4: 아카이브

### 4.1 아카이브 조건

| 조건 | 확인 방법 |
|------|----------|
| 진행률 100% | PROGRESS.md 파싱 |
| 상태 완료 | README.md "✅ 완료" |

### 4.2 아카이브 실행

```
/archive {폴더명}

→ 실행:
  1. active/ → archive/ 이동
  2. CURRENT_STATUS.md에서 제거
  3. archive/README.md에 기록 추가
```

---

## 문서 구조

```
docs/
├── work/
│   ├── CURRENT_STATUS.md    # 📍 Single Source of Truth
│   ├── active/              # 진행 중 작업
│   │   └── {Feature_Name}/
│   │       ├── README.md
│   │       └── PROGRESS.md
│   └── archive/             # 완료된 작업 보관
│       └── {YYYY-MM}_{Feature_Name}/
│
├── logs/
│   └── YYYY-MM-DD.md        # 일일 작업 로그
│
└── CHANGELOG.md             # 버전별 변경 요약
```

---

## 상태 표기 규칙

| 이모지 | 의미 |
|--------|------|
| 🚧 | 진행 중 |
| ✅ | 완료 |
| ⏳ | 대기/예정 |
| ❌ | 취소/실패 |
| 🔴 | Critical |
| 🟠 | High |
| 🟡 | Medium |
| 🟢 | Low |

---

## 세션 연속성

### 새 세션 시작 시

```
1. CURRENT_STATUS.md 확인
2. 진행 중 작업의 PROGRESS.md 확인
3. 마지막 체크포인트에서 이어서 작업
```

### 진행률 파싱

```yaml
파싱 대상:
  - "진행률: {N}%" 패턴
  - 체크리스트 완료율 계산
  - 마지막 작업 항목 식별

목적:
  - 세션 간 컨텍스트 유지
  - 중복 작업 방지
  - 정확한 재개 지점 파악
```

---

## 스킬 연계

| 스킬 | 역할 | 시점 |
|------|------|------|
| `/feature` | 기능 작업 시작 | 요청 시 |
| `/bugfix` | 버그 수정 시작 | 버그 발견 시 |
| `/plan` | 복잡한 계획 수립 | 대형 작업 |
| `/done` | 작업 완료 처리 | 완료 시 |
| `/archive` | 아카이브 이동 | 100% 완료 |
| `/revive` | 아카이브 부활 | 관련 작업 시 |
| `/status` | 현황 파악 | 세션 시작 |
| `/sync` | 문서 동기화 | 정기/필요시 |

---

## 예시: Feature 작업 전체 흐름

```
[Day 1]
사용자: "사용자 프로필 기능 추가해줘"

→ /feature 시작
→ archive/ 검색: 관련 문서 없음
→ 폴더 생성: docs/work/active/Feature_User_Profile/
→ README.md, PROGRESS.md 생성
→ Phase A 작업 시작: 기본 UI 구현
→ 진행률: 30%

[Day 2]
사용자: "프로필 계속해줘"

→ CURRENT_STATUS.md 확인
→ PROGRESS.md: 30%, Phase A 완료, Phase B 진행 중
→ Phase B 이어서: API 연동
→ 진행률: 70%

[Day 3]
→ Phase C: 테스트 및 검증
→ 진행률: 100%
→ /done 실행
  - 빌드 검증 ✅
  - CHANGELOG 업데이트
  - logs/2024-01-22.md 생성
  - CURRENT_STATUS 업데이트
→ /archive 제안
→ archive/로 이동 완료
```

---

**문서 버전:** 2.0.0
