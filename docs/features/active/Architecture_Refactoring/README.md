# Architecture Refactoring (아키텍처 리팩토링)

> **버전**: 7.2.0 | **최종 수정**: 2026-01-13

## 개요

ACS v2.0 고도화를 위한 체계적 리팩토링 프로젝트입니다.

**설계 원칙**: 소규모 팀 관리 가능, 오버엔지니어링 금지

## 핵심 목표

| 목표 | 현재 | 목표 상태 |
|------|------|----------|
| 추적 데이터 영속성 | RAM (휘발성) | PostgreSQL + TimescaleDB |
| 외부 시스템 인증 | 없음 | API Key + JWT |
| 테스트 커버리지 | 1.5% | 50%+ |
| 코드 품질 | 대형 파일, 중복 | 모듈화, DRY |

## RFC 문서 체계

| RFC# | 제목 | 역할 | 우선순위 | 상태 |
|------|------|------|:--------:|------|
| [RFC-001](./RFC-001_Database_Strategy.md) | 데이터베이스 전략 | DB 설정 | **P0** | v1.1.0 |
| [RFC-002](./RFC-002_Logging_System.md) | 로깅 시스템 고도화 | 로그 설정 | **P0** | v1.0.0 |
| [RFC-003](./RFC-003_State_Machine_Extraction.md) | BE 코드 품질 개선 | !! 연산자, 매직넘버, 동시성 | P2 | v1.2.0 |
| [RFC-004](./RFC-004_API_Standardization.md) | API 표준화 | 응답 형식, 동기/비동기, Critical 버그 | **P1** | v1.6.0 |
| RFC-005 | 테스트 전략 | 테스트 커버리지 | P2 | 작성 필요 |
| RFC-006 | CI/CD 파이프라인 | 빌드 자동화 | P1 | 작성 필요 |
| [RFC-007](./RFC-007_BE_Infrastructure.md) | BE 인프라 개선 | 입력 검증, Repository 추상화, 예외 처리 | **P0** | v1.1.0 |
| [RFC-008](./RFC-008_Frontend_Restructuring.md) | 프론트엔드 구조화 | FE 전체 (Composable, 성능, icdStore, Dead Code) | P2 | v1.4.0 |
| [RFC-009](./RFC-009_Accessibility.md) | 접근성 개선 | WCAG 준수, aria 속성, 키보드 네비게이션 | P2 | v1.0.0 |
| [RFC-010](./RFC-010_Deep_Analysis_Report.md) | 심층 분석 보고서 | 기존 RFC 누락 사항 (CORS, XSS, 입력검증, shallowRef) | **P0** | v1.0.0 |

### 검토 문서

| 문서 | 역할 |
|------|------|
| [Quality_Review_Report.md](./Quality_Review_Report.md) | 문서 품질 검토 (일관성, 타당성, 추적 가능성) |

## 실행 순서

```
Phase 0: RFC-001 (DB)                        ← 모든 것의 기반
    │
    ▼
Phase 1: RFC-002 (로깅) + RFC-004 (API)      ← 병렬 진행
    │
    ▼
Phase 2: RFC-006 (CI/CD) + RFC-007 (BE 인프라) ← 병렬 진행
    │
    ▼
Phase 3: RFC-003, 005, 008                   ← 점진적 개선
```

### 영역별 분류

| 영역 | RFC |
|------|-----|
| **Backend** | 001, 002, 003, 004, **007** |
| **Frontend** | **008** |
| **Infra** | 006 |
| **Test** | 005 |

## 관련 문서

| 문서 | 역할 |
|------|------|
| [Execution_Plan.md](./Execution_Plan.md) | **종합 실행 계획서** - Sprint별 작업 계획 |
| [Execution_Checklist.md](./Execution_Checklist.md) | **상세 체크리스트** - 모든 작업 항목 |
| [Phase_Completion_Report.md](./Phase_Completion_Report.md) | 완료된 Phase 이력 (참고용) |
| [Vision_System_Upgrade.md](./Vision_System_Upgrade.md) | 비전 및 요구사항 확정 |
| [legacy/](./legacy/) | 기존 리팩토링 문서 (참고용) |

### legacy/ 폴더 내용 (11개)

| 문서 | 설명 |
|------|------|
| Master_Refactoring_Plan.md | 기존 마스터 계획 |
| RFC_SatelliteTrackingEngine.md | RFC-003 상세 원본 |
| Backend_Refactoring_plan.md | BE 파일 목록 |
| Frontend_Refactoring_plan.md | FE 파일 목록 |
| Expert_Analysis_Report.md | 전문가 분석 보고서 |
| 기타 6개 | 설정/UI/보안 등 |

## 상태

진행 중 - Phase B (깊은 분석) 완료, Phase C (실행 준비) 대기

### 작성 완료 RFC

| RFC | 버전 | 역할 |
|-----|------|------|
| RFC-001 | v1.1.0 | DB 전략 (PostgreSQL + TimescaleDB) |
| RFC-002 | v1.0.0 | 로깅 (5GB 제한, 30일 보관) |
| RFC-003 | v1.2.0 | BE 코드 품질 (!! 46건, 매직넘버 40+, 동시성 3건) |
| RFC-004 | v1.6.0 | API 표준화 (응답 형식, 동기/비동기, Critical 버그) |
| RFC-007 | v1.1.0 | BE 인프라 (입력 검증, Repository 추상화, 예외 처리 180+건) |
| RFC-008 | v1.4.0 | FE 전체 구조화 (Composable, 성능, icdStore 175개 ref, Dead Code) |
| RFC-009 | v1.0.0 | 접근성 (aria 0개, WCAG 준수 계획) |
| RFC-010 | v1.0.0 | 심층 분석 (CORS, XSS, 입력검증, shallowRef 0건, 인증 가드 부재) |

### 전수조사 결과 (2026-01-13)

> **조사 완료**: FE 103개 파일, BE 66개 파일 전수조사

| 영역 | 발견 사항 | 건수 | 심각도 | RFC |
|------|----------|------|--------|-----|
| **FE 접근성** | aria 속성 | **0개** | Critical | RFC-009 |
| FE 미사용 코드 | ExampleComponent, example-store | 3건 | Critical | RFC-008 |
| FE 코드 품질 | 하드코딩 색상 | **520건** | High | RFC-008 |
| FE 코드 품질 | as 타입 단언 | **280건** | High | RFC-008 |
| FE 코드 품질 | !important 과다 | **1,690개** | Medium | RFC-008 |
| FE 디버깅 | console.log | **1,513개** | Medium | RFC-008 |
| FE 성능 | icdStore 개별 ref | **175개** | Critical | RFC-008 |
| FE 메모리 | 이벤트 리스너 미정리 | 2건 | High | RFC-008 |
| FE 구조 | modeStore 중복 | 2개 파일 | High | RFC-008 |
| **BE 테스트** | 테스트 코드 main 혼재 | 595줄 | High | 즉시 정리 |
| BE 코드 품질 | !! 연산자 | 46건 | High | RFC-003 |
| BE 코드 품질 | 매직 넘버 | 40+건 | Medium | RFC-003 |
| BE 인프라 | 입력 검증 전무 | Critical | Critical | RFC-007 |
| BE 인프라 | print/println | 102건 | High | RFC-007 |
| BE 예외 처리 | 광범위 catch | **180+건** | High | RFC-007 |
| BE 리소스 | 누수 가능성 | 2건 | Medium | RFC-007 |
