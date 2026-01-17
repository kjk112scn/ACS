# 문서 구조 개선 설계

## 1. 설계 의도

### Why (왜 이렇게 설계했는가)

Claude Code의 컨텍스트 윈도우 효율성을 위해:

1. **CLAUDE.md 분리**: 169줄 단일 파일 -> 80줄 + 50줄 + 50줄 계층 구조
2. **중복 제거**: SYSTEM_OVERVIEW와 data-flow의 70% 중복 해소
3. **예측 가능한 구조**: 일관된 패턴으로 Claude Code가 필요한 문서 빠르게 탐색

### 대안 분석

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| A. Monorepo 분리 | 완전 독립 | 마이그레이션 비용 높음 | X |
| B. 코드 주석 중심 | 유지 부담 감소 | 도메인 지식 전달 어려움 | X |
| C. **계층적 CLAUDE.md** | 점진적 적용, 토큰 절감 | 초기 작업 필요 | O |

## 2. 구현 계획

### 2.1 루트 CLAUDE.md 축소

**현재 구조** (169줄):
```
- 기술 스택
- 빌드 명령어
- 프로젝트 구조
- 핵심 코딩 규칙 (FE+BE)
- 문서 위치
- 모드 시스템
- 테스트
- 에러 처리
- 보안
- 주의사항
- 작업 가이드
- 에이전트 목록
```

**개선 구조** (80줄):
```
- 프로젝트 소개 (5줄)
- 기술 스택 (10줄)
- 빌드 명령어 (10줄)
- 구조 개요 (10줄)
- 핵심 주의사항 (15줄)
- 하위 문서 참조 (10줄)
- 작업 가이드 블록 (20줄)
```

**이동할 내용**:
| 내용 | 이동 대상 |
|------|----------|
| FE 코딩 규칙 | `frontend/CLAUDE.md` |
| BE 코딩 규칙 | `backend/CLAUDE.md` |
| 에러 처리 상세 | 각 하위 CLAUDE.md |
| 에이전트 목록 | `.claude/README.md` (이미 있음) |
| 모드 시스템 상세 | `docs/core/DOMAIN.md` |

### 2.2 frontend/CLAUDE.md (신규)

```markdown
# Frontend (Vue 3 + Quasar + TypeScript)

## 핵심 규칙

### 컴포넌트
- `<script setup lang="ts">` 필수
- Props/Emits 타입 명시

### 스타일
- 색상: `var(--theme-*)` 사용, 하드코딩 금지
- CSS-in-JS 금지, Quasar 클래스 활용

### 상태 관리
- Pinia Setup Store 패턴
- Store 간 직접 참조 최소화

### Composables
- useErrorHandler - 에러 처리
- useNotification - 알림
- useLoading - 로딩 상태

## 구조

```
src/
├── components/     # 재사용 컴포넌트
├── pages/          # 라우트 페이지
│   └── mode/       # 모드별 페이지
├── stores/         # Pinia 스토어
├── services/       # API 호출
├── composables/    # Composition 함수
└── types/          # TypeScript 타입
```

## 참조

- [도메인 개념](../docs/core/DOMAIN.md)
- [데이터 흐름](../docs/core/DATA_FLOW.md)
```

### 2.3 backend/CLAUDE.md (신규)

```markdown
# Backend (Kotlin + Spring Boot 3 + WebFlux)

## 핵심 규칙

### Kotlin
- Idiom 준수 (apply, let, also 활용)
- KDoc 주석 필수 (public 함수)
- data class 활용

### 계층 구조
Controller -> Service -> Algorithm -> Repository
- Controller: 요청/응답 변환만
- Service: 비즈니스 로직
- Algorithm: 순수 계산 (외부 의존성 금지)

### 리액티브 (WebFlux)
- Mono, Flux 반환
- suspend 함수 활용
- 블로킹 코드 금지 (runBlocking 등)

### 예외 처리
- GlobalExceptionHandler 활용
- 광범위 catch(Exception) 금지
- .subscribe() 에러 핸들러 필수

## 구조

```
src/main/kotlin/.../
├── controller/     # REST 엔드포인트
├── service/        # 비즈니스 로직
├── algorithm/      # 계산 알고리즘
├── dto/            # 데이터 전송 객체
├── model/          # 도메인 모델
└── config/         # 설정
```

## 참조

- [ICD 프로토콜](../docs/api/ICDService.md)
- [알고리즘](../docs/architecture/algorithms/)
```

### 2.4 docs/core/ 폴더

#### SYSTEM.md (핵심 개요)

```markdown
# ACS 시스템 개요

## 아키텍처

```
Frontend (Vue) <--WebSocket/REST--> Backend (Spring) <--UDP--> Firmware
```

## 통신

| 구간 | 프로토콜 | 주기 |
|------|---------|------|
| FW -> BE | UDP | 10ms |
| BE -> FE | WebSocket | 30ms |
| FE -> BE | REST | 요청시 |

## 핵심 서비스

- EphemerisService: 위성 추적
- ICDService: 펌웨어 통신
- PassScheduleService: 스케줄 관리

→ 상세: [DATA_FLOW.md](DATA_FLOW.md)
```

#### CONVENTIONS.md (규칙 통합)

```markdown
# 코딩 규칙 및 컨벤션

## 네이밍

### Train vs Tilt
| 맥락 | 용어 |
|------|------|
| 코드/변수 | train |
| UI 표시 | Tilt |

### 각도
- 내부: 라디안
- API/표시: 도(degree)

### 시간
- 내부: UTC
- 표시: 로컬 시간대

## 모드 시스템

| 모드 | 설명 |
|------|------|
| Standby | 대기 |
| Step | 스텝 이동 |
| Slew | 슬루 이동 |
| EphemerisDesignation | 위성 추적 |
| PassSchedule | 스케줄 |
| SunTrack | 태양 추적 |
```

### 2.5 llms.txt (신규)

```
# ACS (Antenna Control System)

## Overview
Satellite/Sun tracking antenna control system
Tech: Vue 3 + Kotlin/Spring Boot + Orekit

## Entry Points
- /CLAUDE.md - Project rules and conventions
- /frontend/CLAUDE.md - Frontend specific rules
- /backend/CLAUDE.md - Backend specific rules

## Documentation
- /docs/core/SYSTEM.md - System architecture
- /docs/core/DATA_FLOW.md - Data flow
- /docs/core/DOMAIN.md - Domain concepts
- /docs/core/CONVENTIONS.md - Coding conventions
- /docs/api/ - API documentation
- /docs/decisions/ - Architecture Decision Records

## Active Work
- /docs/work/active/ - Current development

## Claude Code Extensions
- /.claude/agents/ - Subagents
- /.claude/skills/ - Workflow skills
```

## 3. API 변경

해당 없음 (문서 구조 변경만)

## 4. 테스트 계획

### 검증 항목

| 항목 | 검증 방법 |
|------|----------|
| CLAUDE.md 줄 수 | `wc -l CLAUDE.md` < 100 |
| 깨진 링크 | `/sync` 스킬 실행 |
| 중복 문서 | 수동 검토 |
| Claude Code 동작 | 실제 대화 테스트 |

### 롤백 계획

기존 파일 삭제하지 않고 이동만 하므로, 필요시 복원 가능.

## 5. 관련 ADR

- [ADR-004: Claude Code 최적화 문서 구조](../../decisions/ADR-004-claude-code-optimized-doc-structure.md)

---

**작성일**: 2026-01-17
