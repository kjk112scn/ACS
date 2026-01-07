---
name: sync
description: 문서 관리 총괄 스킬. 코드↔문서 동기화, 문서 구조/링크 점검, README 유지. "문서 업데이트", "sync", "동기화", "최신화", "문서 정리", "링크 점검" 키워드에 반응.
---

# Sync - 문서 관리 총괄 스킬

## 역할

코드 변경사항을 분석하고, **문서 구조와 링크 무결성**을 점검하여 문서를 자동으로 관리합니다.

**핵심 가치:**
- 과거 문서 vs 현재 코드 차이점 자동 분석
- 누락된 기능/파일 자동 감지
- 문서 드리프트(drift) 방지
- **문서 구조/링크 건강 상태 점검**
- **README 파일 유지 관리**

## 워크플로우

```
[1. 스캔] → [2. 비교] → [3. 분석] → [4. 구조점검] → [5. 제안] → [6. 적용]
    │           │           │           │              │           │
코드 구조     과거 문서    차이점      README/링크    업데이트    사용자
  파악        로드        식별        무결성 검증     제안       승인
```

## 문서 구조 점검 (Step 4)

### README 파일 존재 확인

```bash
# 필수 README 파일 점검
ls docs/README.md
ls docs/references/README.md
ls docs/references/algorithms/README.md
ls docs/references/protocols/README.md
ls docs/references/architecture/README.md
ls docs/guides/README.md
ls docs/daily/README.md
ls docs/decisions/README.md
```

### 링크 무결성 검증

```bash
# 마크다운 링크 추출 및 검증
grep -r "\[.*\](.*\.md)" docs/ --include="*.md"
```

**점검 항목:**
- 상대 경로 링크 유효성
- 상호 참조 (A→B일 때 B→A 존재 여부)
- 관련 문서 섹션 존재 여부

### 구조 점검 결과 형식

```markdown
## 문서 구조 점검 결과

### README 파일 상태
| 폴더 | README | 상태 |
|------|--------|------|
| docs/references/ | ✅ 있음 | OK |
| docs/guides/ | ❌ 없음 | 생성 필요 |

### 링크 무결성
| 문서 | 깨진 링크 | 조치 |
|------|----------|------|
| SYSTEM_OVERVIEW.md | 0개 | OK |
| EphemerisService.md | 1개 | 수정 필요 |

### 누락된 역링크
- ICDService.md → SYSTEM_OVERVIEW.md (추가 권장)
```

## 동기화 규칙

### Backend → API 문서

| 코드 위치 | 문서 위치 | 추출 내용 |
|----------|----------|----------|
| `controller/**/*.kt` | `docs/references/api/` | 엔드포인트 목록, KDoc |
| `service/**/*.kt` | `docs/references/architecture/` | 서비스 목록, 의존성 |
| `algorithm/**/*.kt` | `docs/references/algorithms/` | 계산 로직, KDoc |

### Frontend → UI 문서

| 코드 위치 | 문서 위치 | 추출 내용 |
|----------|----------|----------|
| `pages/**/*.vue` | `docs/references/architecture/UI_Architecture.md` | 페이지 목록 |
| `stores/**/*.ts` | `docs/references/architecture/` | 스토어 목록, 상태 |
| `composables/**/*.ts` | `docs/references/development/` | 함수 시그니처 |

### 기능 문서

| 이벤트 | 작업 | 대상 |
|-------|------|------|
| 기능 완료 | active/ → completed/ 이동 | `docs/features/` |
| 코드 변경 | 관련 문서 업데이트 제안 | 해당 문서 |

## 실행 단계

### Step 1: 코드 스캔

```bash
# Backend 스캔
find backend/src -name "*.kt" -type f | wc -l
find backend/src -path "*controller*" -name "*.kt"
find backend/src -path "*service*" -name "*.kt"
find backend/src -path "*algorithm*" -name "*.kt"

# Frontend 스캔
find frontend/src -name "*.vue" -type f | wc -l
find frontend/src -name "*.ts" -type f | wc -l
```

### Step 2: 문서 로드 및 비교

```bash
# 기존 문서 확인
find docs -name "*.md" -type f

# 비교 대상
# - docs/references/PROJECT_STATUS_SUMMARY.md (과거)
# - docs/references/PROJECT_STATUS_CURRENT.md (현재)
# - docs/references/architecture/SYSTEM_OVERVIEW.md
```

### Step 3: 차이점 분석

**자동 추출 항목:**
1. Controller 수 (문서 vs 실제)
2. Service 수 (문서 vs 실제)
3. 신규 추가 파일 목록
4. 삭제된 파일 목록
5. KDoc 주석 변경 사항

**분석 출력 형식:**
```markdown
## 동기화 분석 결과

### 변경 감지
| 항목 | 문서 | 실제 | 차이 |
|------|------|------|------|
| Controller | 6개 | 9개 | +3개 |
| Service | 7개 | 13개 | +6개 |

### 누락 문서
- EphemerisService (4,986줄) - 문서 없음
- ICDService (2,788줄) - 문서 없음

### 업데이트 권장
1. PROJECT_STATUS_SUMMARY.md → 최신화 필요
2. SYSTEM_OVERVIEW.md → 신규 서비스 추가
```

### Step 4: 업데이트 제안

**자동 생성 구간 마커:**
```markdown
<!-- AUTO-GENERATED: START -->
...자동 생성 내용...
<!-- AUTO-GENERATED: END -->

<!-- MANUAL: 수동 작성 영역 -->
...보존 내용...
<!-- MANUAL END -->
```

### Step 5: 사용자 승인

```
변경 사항:
1. docs/references/PROJECT_STATUS_CURRENT.md 업데이트
2. docs/references/architecture/SYSTEM_OVERVIEW.md 업데이트

적용하시겠습니까? (y/n)
```

## 참조 파일

**상세 규칙:** [SYNC_RULES.md](SYNC_RULES.md)
**ACS 코드 구조:** [CODE_STRUCTURE.md](CODE_STRUCTURE.md)

## 호출 에이전트

| 에이전트 | 역할 | 모델 |
|---------|------|------|
| `doc-syncer` | **문서 관리 총괄** (코드↔문서 + 구조/링크) | opus |
| `fullstack-helper` | 코드 분석 지원 | sonnet |

## 사용 예시

### 예시 1: 전체 동기화

```
사용자: "/sync" 또는 "문서 최신화해줘"

→ Sync 워크플로우 시작:

[스캔] Backend: 9 controllers, 13 services
       Frontend: 43 components, 25 stores

[비교] PROJECT_STATUS_SUMMARY.md (2024-12) 로드
       → Controller: 6개 → 9개 (+3)
       → Service: 7개 → 13개 (+6)

[분석] 누락 문서 발견:
       - EphemerisService 상세 설명 없음
       - ICD 통신 프로토콜 문서 없음

[제안] 3개 문서 업데이트 권장:
       1. PROJECT_STATUS_CURRENT.md (신규)
       2. SYSTEM_OVERVIEW.md (업데이트)
       3. Satellite_Tracking_Overview.md (신규)

승인 후 문서 업데이트 완료
```

### 예시 2: 특정 영역만 동기화

```
사용자: "/sync controllers" 또는 "API 문서만 동기화해줘"

→ Controller만 스캔
→ docs/references/api/ 업데이트 제안
```

### 예시 3: 과거 문서 비교

```
사용자: "과거 문서랑 현재 코드 비교해줘"

→ 과거: PROJECT_STATUS_SUMMARY.md
→ 현재: 코드 실시간 스캔
→ 차이점 분석 테이블 생성
→ PROJECT_STATUS_CURRENT.md 자동 생성
```

## 보존 규칙

### 자동 업데이트하지 않는 섹션

다음 내용은 사용자가 수동 작성한 것으로 간주하고 보존:
- `## 설계 의도`
- `## 주의사항`
- `## 알려진 이슈`
- `<!-- MANUAL: -->` 태그 내부

### 자동 업데이트 대상

다음 섹션만 자동 업데이트:
- `## 파일 목록`
- `## 엔드포인트`
- `## 통계`
- `<!-- AUTO-GENERATED: -->` 태그 내부

## 트러블슈팅

### Q: 문서가 너무 오래되어 비교가 어려움
A: `PROJECT_STATUS_CURRENT.md`를 새로 생성하고, 과거 문서는 `docs/archive/`로 이동

### Q: 자동 생성 내용이 틀림
A: `<!-- AUTO-GENERATED: -->` 마커 내부만 수정됨. 틀린 부분을 `<!-- MANUAL: -->` 영역으로 이동하면 보존됨

### Q: 특정 파일만 동기화하고 싶음
A: `/sync {파일명}` 또는 "SYSTEM_OVERVIEW.md만 업데이트해줘"

### 예시 4: 문서 구조만 점검

```
사용자: "/sync structure" 또는 "문서 링크 점검해줘"

→ README 파일 존재 확인
→ 링크 무결성 검증
→ 누락된 역링크 감지
→ 수정 제안
```

---

**스킬 버전:** 2.0.0
**작성일:** 2026-01-07
**호환:** ACS 프로젝트 전용
**담당:** doc-syncer 에이전트
