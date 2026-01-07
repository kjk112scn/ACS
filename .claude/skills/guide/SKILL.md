---
name: guide
description: 에이전트/스킬 사용법 빠른 안내. "가이드", "guide", "사용법", "어떻게 써", "도움말" 키워드에 반응. haiku 모델로 빠르게 응답.
model: haiku
---

# Guide - 사용법 빠른 안내

> 빠른 모델(haiku)로 즉시 응답

## 역할

에이전트와 스킬 사용법을 빠르게 안내합니다.

## 스킬 목록

| 스킬 | 용도 | 예시 |
|------|------|------|
| `/feature` | 새 기능 개발 | `/feature 필터 기능 추가` |
| `/bugfix` | 버그 수정 | `/bugfix 차트 안 그려져` |
| `/done` | 작업 완료 | `/done` |
| `/sync` | 문서 동기화 | `/sync` |
| `/status` | 현황 보기 | `/status` |
| `/health` | 빌드 점검 | `/health` |
| `/plan` | 계획 수립 | `/plan 리팩토링 어떻게?` |
| `/adr` | 결정 기록 | `/adr 왜 이렇게 했는지` |
| `/docs` | 코드 문서화 | `/docs Service 문서화` |
| `/guide` | 이 안내 | `/guide` |

## 에이전트 (자동 호출)

| 키워드 | 호출되는 에이전트 |
|--------|------------------|
| "설계해줘" | architect |
| "구현해줘" | fullstack-helper |
| "느려", "성능" | performance-analyzer |
| "에러", "안 돼" | debugger |
| "테스트" | test-expert |
| "리팩토링" | refactorer |
| "위성", "Orekit" | algorithm-expert |
| "리뷰" | code-reviewer |

## 일상 패턴

```
작업 시작: /health
새 기능: /feature → 구현 → /done
버그 수정: /bugfix → 수정 → /done
정리: /sync, /status
```

## 상세 문서

[전체 가이드](../../../docs/guides/AGENT_SKILL_USAGE_GUIDE.md)
