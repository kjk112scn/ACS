# Bugfixes - 버그 수정 문서

> 버그 분석 및 수정 내용을 체계적으로 기록합니다.

## 구조

```
bugfixes/
├── active/         # 진행 중인 버그 수정
│   └── {버그명}/
│       ├── README.md      # 버그 정보
│       ├── ANALYSIS.md    # 원인 분석
│       └── FIX.md         # 수정 내용
│
└── completed/      # 완료된 버그 수정
    └── {버그명}/
        ├── README.md
        ├── ANALYSIS.md
        ├── FIX.md
        └── IMPLEMENTATION.md  # 구현 결과
```

## 버그명 규칙

`{영역}_{증상}` 형식 (PascalCase + 언더스코어)

예시:
- `PassSchedule_Chart_Slow`
- `Ephemeris_Negative_Elevation`
- `ICD_Connection_Timeout`

## 심각도 분류

| 레벨 | 이모지 | 기준 | 대응 |
|------|-------|------|------|
| Critical | 🔴 | 시스템 다운, 데이터 손실 | 즉시 수정 |
| High | 🟠 | 주요 기능 불가 | 당일 수정 |
| Medium | 🟡 | 기능 제한, 우회 가능 | 이번 주 수정 |
| Low | 🟢 | 경미한 불편 | 백로그 |

## 워크플로우

```
[버그 보고] → [폴더 생성] → [분석] → [수정] → [검증] → [완료]
                 │            │         │        │         │
              active/    ANALYSIS.md  FIX.md   테스트   completed/
```

## 스킬 사용

```
/bugfix    # 버그 수정 워크플로우 시작
/done      # 완료 처리 (active/ → completed/ 이동)
```

## 템플릿

- **README.md**: [`.claude/templates/BUGFIX_README_TEMPLATE.md`](../../.claude/templates/BUGFIX_README_TEMPLATE.md)
- **ANALYSIS.md**: [`.claude/templates/ANALYSIS_TEMPLATE.md`](../../.claude/templates/ANALYSIS_TEMPLATE.md)

## 완료된 버그 목록

| 버그명 | 심각도 | 완료일 | 원인 요약 |
|--------|-------|--------|----------|
| - | - | - | - |
