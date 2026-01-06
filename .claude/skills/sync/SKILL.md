---
name: syncing-docs
description: 코드와 문서 자동 동기화. 과거 문서와 현재 코드를 비교하여 차이점 분석, 문서 최신화 제안. "문서 업데이트", "sync", "동기화", "최신화", "문서 정리" 키워드에 반응.
---

# Syncing Docs - 문서 자동 동기화 스킬

## 역할

코드 변경사항을 분석하여 관련 문서를 자동으로 검증하고 업데이트합니다.

**핵심 가치:**
- 과거 문서 vs 현재 코드 차이점 자동 분석
- 누락된 기능/파일 자동 감지
- 문서 드리프트(drift) 방지

## 워크플로우

```
[1. 스캔] → [2. 비교] → [3. 분석] → [4. 제안] → [5. 적용]
    │           │           │           │           │
코드 구조     과거 문서    차이점      업데이트    사용자
  파악        로드        식별        제안       승인
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
| `project-manager` | 문서 구조 관리 | sonnet |
| `fullstack-helper` | 코드 분석 | sonnet |

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

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-05
**호환:** ACS 프로젝트 전용
