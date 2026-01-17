# Architecture Decision Records (ADR)

> 모든 중요한 아키텍처 결정을 기록하는 공간입니다.

## ADR이란?

Architecture Decision Record는 아키텍처 결정의 컨텍스트, 결정 내용, 결과를 기록하는 짧은 문서입니다.

## ADR 작성 시점

다음 상황에서 ADR을 작성합니다:

| 상황 | 예시 |
|------|------|
| 새 라이브러리/프레임워크 도입 | Orekit 라이브러리 선택 |
| 아키텍처 패턴 변경 | 이벤트 기반 아키텍처 도입 |
| API 인터페이스 변경 | REST → WebSocket 변경 |
| 데이터 구조 변경 | 새 Model 도입 |
| 알고리즘 변경 | 추적 알고리즘 변경 |
| 성능 최적화 방법 결정 | 캐싱 전략 결정 |

## ADR 상태

| 상태 | 설명 |
|------|------|
| 제안됨 (Proposed) | 새로 작성, 검토 대기 |
| 승인됨 (Accepted) | 적용 결정 |
| 폐기됨 (Deprecated) | 더 이상 유효하지 않음 |
| 대체됨 (Superseded) | 새 ADR로 대체됨 |

## ADR 목록

| 번호 | 제목 | 상태 | 날짜 |
|------|------|------|------|
| ADR-001 | [Train vs Tilt 명칭 결정](ADR-001-train-vs-tilt.md) | 승인됨 | - |
| ADR-002 | [Orekit 라이브러리 선택](ADR-002-orekit-selection.md) | 승인됨 | - |
| ADR-003 | [Performance Analyzer 에이전트 도입](ADR-003-performance-analyzer-agent.md) | 승인됨 | 2026-01-07 |
| ADR-004 | [Claude Code 최적화 문서 구조](ADR-004-claude-code-optimized-doc-structure.md) | 제안됨 | 2026-01-17 |

## 새 ADR 작성

1. `/adr` 스킬 사용 또는 수동 생성
2. 템플릿: [`.claude/templates/ADR_TEMPLATE.md`](../../.claude/templates/ADR_TEMPLATE.md)
3. 파일명: `ADR-NNN-{kebab-case-제목}.md`

## 참고 자료

- [Spotify ADR](https://engineering.atspotify.com/2020/04/when-should-i-write-an-architecture-decision-record)
- [AWS ADR Process](https://docs.aws.amazon.com/prescriptive-guidance/latest/architectural-decision-records/adr-process.html)
