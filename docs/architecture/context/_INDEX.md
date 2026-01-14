# ACS Context Reference Index

> 서브 에이전트와 스킬이 참조하는 ACS 시스템 전문 지식 저장소

## 구조

```
docs/architecture/context/
├── _INDEX.md              # 이 파일 (전체 맵)
├── domain/                # 도메인 지식
│   ├── satellite-tracking.md    # 위성 추적
│   ├── antenna-control.md       # 안테나 제어
│   ├── mode-system.md           # 모드 시스템
│   └── icd-protocol.md          # ICD 통신 프로토콜
├── architecture/          # 아키텍처
│   ├── frontend.md              # FE 아키텍처
│   ├── backend.md               # BE 아키텍처
│   └── data-flow.md             # 데이터 흐름
└── codebase/              # 코드베이스 현황
    ├── file-structure.md        # 파일 구조
    └── key-components.md        # 핵심 컴포넌트
```

## 참조 방법

### CLAUDE.md에서 (@import)
```markdown
# 필요 시 로드
@docs/architecture/context/domain/satellite-tracking.md
@docs/architecture/context/architecture/backend.md
```

### 에이전트에서
```markdown
# 에이전트 정의 시
참조 문서:
- [위성 추적](../../docs/architecture/context/domain/satellite-tracking.md)
- [FE 아키텍처](../../docs/architecture/context/architecture/frontend.md)
```

## 문서 관리

### 업데이트 주기
| 문서 유형 | 업데이트 시점 | 담당 |
|----------|--------------|------|
| domain/ | 도메인 로직 변경 시 | 수동 + /sync 제안 |
| architecture/ | 구조 변경 시 | /sync 자동 제안 |
| codebase/ | 파일 추가/삭제 시 | /sync 자동 업데이트 |
| changelog/ | /done 실행 시 | 자동 생성 |

### 품질 관리 (/sync)
- 일관성 검사: 중복 정보 탐지
- 최신성 검사: 코드와 문서 불일치 탐지
- 링크 검사: 깨진 참조 탐지

## 빠른 참조

### 핵심 도메인 개념
- **위성 추적**: TLE 파싱 → Orekit 전파 → 안테나 지향
- **안테나 제어**: Azimuth(방위각), Elevation(고도각), Train/Tilt
- **모드 시스템**: Standby, Step, Slew, EphemerisDesignation, PassSchedule, SunTrack
- **ICD 프로토콜**: UDP 통신, 30ms 주기, WebSocket 브로드캐스트

### 아키텍처 핵심
- **Frontend**: Vue 3 + Quasar + Pinia (Setup Store)
- **Backend**: Kotlin + Spring WebFlux (리액티브)
- **통신**: REST API, WebSocket, UDP

---

**버전:** 1.0.0
**생성일:** 2026-01-14
**관리:** doc-syncer 에이전트
