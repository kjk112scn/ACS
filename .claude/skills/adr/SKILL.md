---
name: adr
description: ADR(Architecture Decision Record) 생성 스킬. 아키텍처 결정 기록, Why 문서화, 대안 분석. "결정 기록", "adr", "왜", "아키텍처 결정", "기술 결정" 키워드에 반응.
model: opus
---

# Creating ADR - 아키텍처 결정 기록 스킬

## 역할

모든 중요한 아키텍처 결정을 체계적으로 기록하고 추적합니다.

**핵심 가치:**
- Why 기록의 핵심
- 대안 분석 문서화
- 결정 히스토리 추적
- 미래 참조 가능

## ADR이란?

**Architecture Decision Record**: 아키텍처 결정을 기록하는 짧은 문서

```yaml
포함 내용:
  - Context: 왜 결정이 필요했는가
  - Decision: 무엇을 결정했는가
  - Alternatives: 어떤 대안이 있었는가
  - Consequences: 결정의 영향은 무엇인가
```

## 워크플로우

```
[1. 트리거] → [2. 번호 할당] → [3. ADR 작성] → [4. 연결] → [5. 상태 관리]
      │            │              │              │            │
  자동/수동    ADR-NNN      템플릿 기반     관련 문서     승인/폐기
   감지         생성          작성         링크 연결      추적
```

## 자동 생성 트리거

```yaml
자동 트리거:
  - 새 라이브러리/프레임워크 도입
  - 아키텍처 패턴 변경
  - API 인터페이스 변경
  - 데이터 구조 변경
  - 알고리즘 변경 (정확도, 성능 영향)
  - 성능 최적화 방법 결정
  - 보안 관련 결정

수동 트리거:
  - "/adr" 명령어
  - "결정 기록해줘"
  - "왜 이렇게 했는지 기록해줘"
```

## ADR 템플릿

**파일 위치:** `docs/decisions/ADR-NNN-{kebab-case-제목}.md`

```markdown
---
번호: ADR-NNN
제목: {결정 제목}
상태: 제안됨 | 승인됨 | 폐기됨 | 대체됨
날짜: YYYY-MM-DD
대체: ADR-XXX (대체된 경우)
---

# ADR-NNN: {결정 제목}

## 상태

{제안됨 | 승인됨 | 폐기됨 | 대체됨}

## 컨텍스트

{왜 이 결정이 필요한가? 어떤 문제를 해결하려 하는가?}

### 배경
- {배경 설명 1}
- {배경 설명 2}

### 제약 조건
- {제약 1}
- {제약 2}

## 결정

**{결정 내용 한 문장}**

{상세 설명}

## 대안

### 대안 1: {대안명}
| 항목 | 내용 |
|------|------|
| 설명 | |
| 장점 | |
| 단점 | |
| 선택 | ❌ |

### 대안 2: {대안명}
| 항목 | 내용 |
|------|------|
| 설명 | |
| 장점 | |
| 단점 | |
| 선택 | ✅ |

## 결과

### 긍정적 영향
- {장점 1}
- {장점 2}

### 부정적 영향
- {단점 1}
- {단점 2}

### 리스크
- {리스크 1}

## 관련 문서

- [관련 기능 문서](../features/...)
- [관련 ADR](ADR-XXX-...)

## 참고 자료

- {외부 링크 또는 참조}
```

## ADR 상태 관리

```yaml
라이프사이클:
  제안됨 (Proposed):
    - 새로 작성된 ADR
    - 검토 대기 중

  승인됨 (Accepted):
    - 검토 완료, 적용 결정
    - 구현에 반영

  폐기됨 (Deprecated):
    - 더 이상 유효하지 않음
    - 새 ADR로 대체 예정

  대체됨 (Superseded):
    - 새 ADR로 대체됨
    - "대체: ADR-XXX" 헤더 추가

상태 변경 기록:
  각 ADR 하단에 변경 이력 추가:

  ## 변경 이력
  | 날짜 | 상태 | 변경 내용 |
  |------|------|----------|
  | 2026-01-06 | 제안됨 | 초기 작성 |
  | 2026-01-07 | 승인됨 | 팀 검토 완료 |
```

## 번호 할당 규칙

```bash
# 다음 번호 확인
ls docs/decisions/ADR-*.md | sort -V | tail -1

# 번호 체계
ADR-001  # 첫 번째
ADR-002  # 두 번째
...
ADR-999  # 최대
```

## ACS 프로젝트 기존 ADR 예시

| ADR | 제목 | 상태 | 설명 |
|-----|------|------|------|
| ADR-001 | Train vs Tilt 명칭 결정 | 승인됨 | 내부: train, UI: Tilt |
| ADR-002 | Orekit 라이브러리 선택 | 승인됨 | SGP4/SDP4 구현 |
| ADR-003 | WebSocket vs Polling | 승인됨 | 실시간 데이터 전송 방식 |

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `architect` | ADR 작성 | 메인 |
| `tech-lead` | 결정 승인 | 검토 시 |
| `project-manager` | 문서 관리 | 연결/정리 |

## 사용 예시

### 예시 1: 라이브러리 결정

```
사용자: "왜 Orekit을 선택했는지 기록해줘"

→ /adr 워크플로우:

[번호 할당] ADR-004

[ADR 작성] docs/decisions/ADR-004-orekit-library-selection.md

  ## 컨텍스트
  위성 궤도 계산 라이브러리 필요

  ## 결정
  Orekit 13.0.2 선택

  ## 대안
  - SGP4-RS (Rust): 성능 좋으나 JVM 통합 어려움 ❌
  - PyOrbital: Python, 언어 불일치 ❌
  - Orekit: Java/Kotlin 호환, 풍부한 기능 ✅

  ## 결과
  + 정확한 궤도 계산
  + Java/Kotlin 네이티브
  - 초기화 데이터 필요 (orekit-data)
```

### 예시 2: 아키텍처 결정

```
사용자: "실시간 통신 방식 WebSocket으로 결정한 이유 기록"

→ /adr 워크플로우:

[ADR 작성] ADR-005-realtime-communication-websocket.md

  ## 컨텍스트
  Frontend-Backend 실시간 데이터 전송 필요
  - 위성 위치 업데이트 (100ms 간격)
  - 시스템 상태 모니터링

  ## 결정
  WebSocket 양방향 통신 채택

  ## 대안
  - HTTP Polling: 오버헤드 큼 ❌
  - SSE: 단방향만 지원 ❌
  - WebSocket: 양방향, 저지연 ✅
```

### 예시 3: 자동 트리거

```
[자동 감지] 새 import 추가:
  import org.some.new.library.NewFeature

→ "새 라이브러리 감지: org.some.new.library"
→ "ADR 생성을 권장합니다. /adr 실행하시겠습니까?"
```

## 트러블슈팅

### Q: 어떤 결정을 ADR로 기록해야 하나요?
A: "나중에 왜 이렇게 했지?" 라고 물을 만한 결정은 모두 기록.

### Q: 기존 결정을 변경하고 싶어요
A: 기존 ADR을 "대체됨"으로 변경하고 새 ADR 생성.

### Q: 이미 구현된 것도 ADR 작성하나요?
A: 예. 과거 결정도 문서화 가치 있음. "승인됨" 상태로 작성.

## 참조 자료

- [Spotify ADR](https://engineering.atspotify.com/2020/04/when-should-i-write-an-architecture-decision-record)
- [AWS ADR Process](https://docs.aws.amazon.com/prescriptive-guidance/latest/architectural-decision-records/adr-process.html)
- [Michael Nygard's ADR](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-06
**호환:** ACS 프로젝트 전용
