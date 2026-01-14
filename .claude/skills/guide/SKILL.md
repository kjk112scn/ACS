---
name: guide
description: 에이전트/스킬 사용법 빠른 안내. "가이드", "guide", "사용법", "어떻게 써", "도움말" 키워드에 반응. haiku 모델로 빠르게 응답.
model: haiku
---

# Guide - 사용법 빠른 안내

> 빠른 모델(haiku)로 즉시 응답

## 역할

에이전트와 스킬 사용법을 빠르게 안내합니다.

## 📋 스킬 목록 (슬래시 명령어)

### 워크플로우 스킬
| 스킬 | 용도 | 예시 |
|------|------|------|
| `/feature` | 새 기능 개발 | `/feature 필터 기능 추가` |
| `/bugfix` | 버그 수정 | `/bugfix 차트 안 그려져` |
| `/refactor` | 리팩토링 | `/refactor PassSchedulePage.vue` |
| `/optimize` | 성능 최적화 | `/optimize icdStore` |
| `/cleanup` | 코드 정리 | `/cleanup console` |
| `/done` | 작업 완료 + 커밋 | `/done` |

### 관리/분석 스킬
| 스킬 | 용도 | 예시 |
|------|------|------|
| `/plan` | 계획 수립 | `/plan 리팩토링 어떻게?` |
| `/status` | 현황 보기 | `/status` |
| `/health` | 빌드/품질 점검 | `/health` |
| `/sync` | 문서 동기화 | `/sync` |
| `/api-sync` | FE-BE 타입 동기화 | `/api-sync generate` |
| `/adr` | 아키텍처 결정 기록 | `/adr 왜 이렇게?` |
| `/docs` | 코드 → 문서화 | `/docs Service 문서화` |
| `/guide` | 이 안내 | `/guide` |

## 🤖 에이전트 목록 (자동 호출)

### 핵심 에이전트 (Opus)
| 에이전트 | 역할 | 키워드 |
|---------|------|--------|
| `tech-lead` | 복잡한 요청 분석, 방향 결정 | "어떻게 해야", "방향" |
| `fe-expert` | Vue/TS/Pinia, icdStore | "프론트", "Vue", "스토어" |
| `be-expert` | Kotlin/Spring/WebFlux | "백엔드", "API", "서비스" |
| `architect` | 설계, ADR | "설계해줘", "구조" |
| `algorithm-expert` | Orekit, 좌표변환 | "위성", "Orekit", "계산" |
| `api-contract` | OpenAPI, 타입 동기화 | "API 스펙", "타입 동기화" |
| `code-reviewer` | 품질 검증 | "리뷰해줘", "검토" |

### 유틸리티 에이전트 (Sonnet/Haiku)
| 에이전트 | 역할 | 모델 |
|---------|------|------|
| `doc-syncer` | 문서 동기화 + 컨텍스트 관리 | opus |
| `code-counter` | 파일/라인 카운팅 | haiku |

## 🚀 일상 패턴

```
작업 시작
└── /health (빌드/상태 확인)

새 기능 개발
├── /plan (계획 수립)
├── /feature (워크플로우 시작)
├── 구현 (fe-expert / be-expert)
└── /done (품질점검 + 완료 + 커밋)

버그 수정
├── /bugfix (워크플로우 시작)
├── 분석 + 수정
└── /done

리팩토링
├── /refactor (파일 분리)
├── /optimize (성능 개선)
├── /cleanup (코드 정리)
└── /done

상태 확인
├── /status (현황)
├── /health (빌드/품질)
└── /sync (문서 동기화 - 별도 실행 가능)
```

**참고:** `/done`은 자동으로 `/sync`를 먼저 실행합니다. `/sync`만 별도 실행도 가능.

## 💡 자주 쓰는 명령

```
# 프로젝트 상태 한눈에
/health

# console.log 정리
/cleanup console

# icdStore 최적화
/optimize icdStore

# 대형 파일 분리
/refactor PassSchedulePage.vue

# 작업 완료 후 커밋
/done
```

## 상세 문서

[전체 가이드](../../../docs/guides/AGENT_SKILL_USAGE_GUIDE.md)
