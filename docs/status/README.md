# Status - 프로젝트 현황

> 프로젝트의 현재 상태를 추적하는 문서들입니다.

## 구조

```
status/
├── README.md                # 이 파일
├── PROJECT_STATUS.md        # 프로젝트 현황 (자동 업데이트)
├── CODE_METRICS.md          # 코드 메트릭스 (자동 생성)
└── TECHNICAL_DEBT.md        # 기술 부채 추적
```

## 문서 설명

### PROJECT_STATUS.md

프로젝트 전체 현황을 한눈에 파악

- Controller, Service 수
- 진행 중/완료 작업
- 문서↔코드 일치율

### CODE_METRICS.md

코드 메트릭스 자동 집계

- 파일 수, 라인 수
- 대형 파일 목록
- 복잡도 분석

### TECHNICAL_DEBT.md

기술 부채 추적

- 리팩토링 필요 항목
- 우선순위별 분류
- 해결 계획

## 업데이트 방법

```
/status       # 전체 현황 보고
/status code  # 코드 메트릭스만
/status debt  # 기술 부채만
/sync         # 자동 동기화 및 업데이트
```

## 자동 업데이트

`/sync` 또는 `/done` 스킬 실행 시 자동으로 현황 문서가 업데이트됩니다.
