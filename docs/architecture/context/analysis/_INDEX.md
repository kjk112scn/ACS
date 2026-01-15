# Analysis 문서 인덱스

> 코드베이스 심층 분석 결과물 저장소

## 개요

이 폴더는 ACS 코드베이스를 심층 분석한 결과물을 저장합니다.
최종 context 문서(domain/, architecture/, codebase/)의 **근거 자료**로 활용됩니다.

## 문서 구조

```
analysis/
├── _INDEX.md              # 이 파일
├── _META.md               # 분석 메타정보 (일시, 버전, 범위)
│
├── backend/               # 백엔드 코드 분석
│   ├── structure.md       # 폴더/파일 구조 + 역할
│   ├── services.md        # Service 계층 상세
│   ├── controllers.md     # Controller 계층 상세
│   └── models.md          # Model/DTO 분석
│
├── frontend/              # 프론트엔드 코드 분석
│   ├── structure.md       # 폴더/파일 구조 + 역할
│   ├── stores.md          # Pinia Store 상세
│   ├── pages.md           # 페이지 컴포넌트 상세
│   └── components.md      # 공통 컴포넌트 분석
│
├── domain-logic/          # 도메인 로직 심층 분석
│   ├── satellite-tracking.md  # 위성 추적 알고리즘
│   ├── icd-protocol.md        # ICD 프로토콜 상세
│   ├── coordinate-system.md   # 좌표계/단위 변환
│   └── mode-transitions.md    # 모드 전환 로직
│
└── synthesis/             # 종합 분석
    ├── data-flow-actual.md    # 실제 데이터 흐름 추적
    ├── gap-report.md          # 문서↔코드 불일치
    └── refactoring-hints.md   # 리팩토링 포인트
```

## 문서별 역할

### backend/
| 문서 | 역할 | 참조하는 최종 문서 |
|-----|------|------------------|
| structure.md | 전체 파일/폴더 구조 | codebase/file-structure.md |
| services.md | Service 계층 상세 분석 | architecture/backend.md |
| controllers.md | REST API 엔드포인트 분석 | architecture/backend.md |
| models.md | 데이터 모델 분석 | domain/*.md |

### frontend/
| 문서 | 역할 | 참조하는 최종 문서 |
|-----|------|------------------|
| structure.md | 전체 파일/폴더 구조 | codebase/file-structure.md |
| stores.md | Pinia Store 상세 분석 | architecture/frontend.md |
| pages.md | 페이지 컴포넌트 분석 | architecture/frontend.md |
| components.md | 공통 컴포넌트 분석 | codebase/key-components.md |

### domain-logic/
| 문서 | 역할 | 참조하는 최종 문서 |
|-----|------|------------------|
| satellite-tracking.md | Orekit 기반 궤도 계산 | domain/satellite-tracking.md |
| icd-protocol.md | UDP 프로토콜 상세 | domain/icd-protocol.md |
| coordinate-system.md | 좌표 변환 알고리즘 | domain/antenna-control.md |
| mode-transitions.md | 모드 전환 상태머신 | domain/mode-system.md |

### synthesis/
| 문서 | 역할 | 활용 |
|-----|------|------|
| data-flow-actual.md | 실제 코드 기반 데이터 흐름 | architecture/data-flow.md 검증 |
| gap-report.md | 문서↔코드 불일치 목록 | 최종 문서 업데이트 기준 |
| refactoring-hints.md | 리팩토링 대상 및 방향 | 리팩토링 계획 수립 |

## 활용 방법

### 최종 문서 작성/수정 시
```
1. analysis/ 문서에서 근거 확인
2. 코드 변경사항 반영
3. 최종 문서 업데이트
```

### 리팩토링 시
```
1. synthesis/refactoring-hints.md 참조
2. domain-logic/*.md로 도메인 이해
3. backend/, frontend/ 구조 파악
```

### 문서 동기화 (/sync) 시
```
1. gap-report.md 기준으로 불일치 확인
2. 변경된 코드 분석
3. 최종 문서 + 분석 문서 동시 업데이트
```

---

**최종 수정**: 2026-01-15
**관리**: doc-syncer 에이전트
