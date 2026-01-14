# ACS Documentation

> ACS(Antenna Control System) 프로젝트 문서

## 문서 구조

```
docs/
├── architecture/          # 시스템 아키텍처
│   ├── SYSTEM_OVERVIEW.md # 시스템 통합 문서
│   ├── UI_Architecture.md # UI 아키텍처
│   ├── algorithms/        # 알고리즘 문서
│   │   └── *.md           # Orekit, solarpositioning 등
│   └── context/           # 에이전트 컨텍스트
│       ├── domain/        # 도메인 지식
│       ├── architecture/  # 아키텍처 상세
│       └── codebase/      # 코드베이스 현황
│
├── api/                   # API 및 프로토콜
│   ├── README.md          # API 문서 개요
│   └── *.md               # ICD, UDP 프로토콜 등
│
├── guides/                # 개발 가이드
│   └── *.md               # 개발, 운영 가이드
│
├── decisions/             # ADR (Architecture Decision Records)
│   └── ADR-NNN-*.md       # 아키텍처 결정 기록
│
├── work/                  # 작업 문서 (기능, 버그)
│   ├── active/            # 진행 중인 작업
│   └── archive/           # 완료된 작업
│
└── logs/                  # 일일 작업 로그
    └── YYYY-MM-DD.md
```

## 스킬 명령어

| 스킬 | 역할 |
|------|------|
| `/feature` | 신규 기능 개발 워크플로우 |
| `/bugfix` | 버그 수정 워크플로우 |
| `/done` | 작업 마무리 + 문서화 |
| `/sync` | 코드↔문서 동기화, 구조 점검 |
| `/adr` | 아키텍처 결정 기록 |
| `/plan` | 작업 계획 수립 |
| `/status` | 프로젝트 현황 보고 |
| `/health` | 빌드, 타입체크, 기술부채 점검 |
| `/guide` | 에이전트/스킬 사용법 안내 |

## 핵심 원칙

1. **코드가 Single Source of Truth** - 문서는 코드를 반영
2. **Why 기록 필수** - 모든 결정에 이유 기록
3. **자동화 우선** - `/sync`, `/done`으로 자동 동기화

## 빠른 시작

### 새 기능 개발

```
1. /feature 실행
2. docs/work/active/{기능명}/ 폴더 생성
3. DESIGN.md 작성 → 구현
4. /done 실행 → archive/로 이동
```

### 버그 수정

```
1. /bugfix 실행
2. docs/work/active/{버그명}/ 폴더 생성
3. ANALYSIS.md → FIX.md 작성
4. 구현 + 검증
5. /done 실행 → archive/로 이동
```

## 관련 문서

- [시스템 개요](architecture/SYSTEM_OVERVIEW.md)
- [API 문서](api/README.md)
- [개발 가이드](guides/Development_Guide.md)
- [ADR 목록](decisions/README.md)
- [작업 로그](logs/README.md)
