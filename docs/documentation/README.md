# Documentation - 문서화 작업

> `/docs` 스킬을 통한 체계적인 문서화 작업을 관리합니다.

## 구조

```
documentation/
├── README.md          # 이 파일
├── active/            # 진행 중인 문서화 작업
│   └── {주제명}/
│       ├── README.md      # 문서화 목표
│       ├── ANALYSIS.md    # 코드 분석 결과
│       ├── DISCUSSION.md  # 협의 내용 기록
│       └── DRAFT.md       # 문서 초안
└── completed/         # 완료된 문서화 작업
```

## 워크플로우

```
[1. 영역 선택] → [2. 코드 분석] → [3. 협의] → [4. 초안 작성] → [5. 승인] → [6. 적용]
```

## 사용법

```
/docs 시스템 아키텍처       # 전체 시스템 문서화
/docs EphemerisService    # 특정 서비스 문서화
/docs 모드 시스템          # 특정 기능 문서화
```

## 문서화 유형

| 유형 | 설명 | 출력 위치 |
|------|------|----------|
| 시스템 아키텍처 | 전체 구조 | `concepts/architecture/` |
| 특정 모듈 | 단일 서비스/컴포넌트 | `concepts/{카테고리}/` |
| 알고리즘 | 계산 로직 | `concepts/algorithms/` |
| API 명세 | 엔드포인트 | `concepts/api/` |

## 진행 중인 작업

| 주제 | 상태 | 시작일 |
|------|------|--------|
| - | - | - |

## 완료된 작업

| 주제 | 완료일 | 출력 문서 |
|------|--------|----------|
| - | - | - |

## 관련 스킬

- [/docs 스킬](../../.claude/skills/docs/SKILL.md)
- [/done 스킬](../../.claude/skills/done/SKILL.md)
- [/sync 스킬](../../.claude/skills/sync/SKILL.md)
