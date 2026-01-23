# docs/work - 작업 문서 관리

> 진행 중/완료된 작업 문서를 체계적으로 관리

## 폴더 구조

```
docs/work/
├── active/       # 진행 중 작업 (Claude 검색 대상)
├── archive/      # 완료된 작업 (Claude 검색 제외 - 토큰 절약)
├── planned/      # 예정된 작업
└── templates/    # 작업 폴더 템플릿
```

## 라이프사이클

```
[planned/] → [active/] → [archive/]
   예정        진행 중       완료
              ↑     ↓
           /revive  /archive (or /done)
```

## 스킬 연계

| 스킬 | 역할 |
|------|------|
| `/feature`, `/bugfix` | active/에 새 폴더 생성 |
| `/done` | 완료 시 archive/ 이동 제안 |
| `/archive` | 수동 아카이브 |
| `/revive` | archive/ → active/ 부활 |

## 작업 폴더 표준 구조

```
{Feature_Name}/
├── README.md      # 필수: 개요, 요구사항, 상태
├── PROGRESS.md    # 권장: 체크리스트, 진행률
├── DESIGN.md      # 선택: 설계 상세
└── FIX.md         # 선택: 버그 수정 기록
```

## 명명 규칙

```
{Category}_{Feature}_{Action}

예시:
  Tracking_Schema_Redesign     ✅
  Admin_Panel_Separation       ✅
  PassSchedule_Chart_Bugfix    ✅

금지:
  PassSchedule_ApplyRowColors_Infinite_Loop  ❌ (너무 김)
```

## 현재 상태

### active/ (진행 중)

| 폴더 | 상태 | 진행률 |
|------|------|--------|
| [README 참조](active/README.md) | | |

### archive/ (완료)

| 폴더 | 완료일 |
|------|--------|
| [README 참조](archive/README.md) | |

## 관련 문서

- [아카이브 목록](archive/README.md)
- [작업 템플릿](templates/)
- [CLAUDE.md 스킬 섹션](../../CLAUDE.md#스킬-명령어)
