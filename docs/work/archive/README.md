# Archive - 완료된 작업 보관소

> Claude 검색에서 제외됨 (토큰 절약). 부활 필요 시 `/revive` 스킬 사용.

## 아카이브 목록

| 폴더명 | 내용 요약 | 아카이브 날짜 |
|--------|----------|--------------|
| `2026-01-Architecture_Refactoring_Legacy` | RFC-001~010, 분석 리포트, 실행 계획 (35개) | 2026-01-22 |
| `DisplayMinElevationAngle_Removal` | 최소 고도각 표시 제거 | 2026-01 |
| `Elevation_Filtering_Management` | 고도 필터링 관리 | 2026-01 |
| `Frontend_Display_Values_Validation` | FE 표시값 검증 | 2026-01 |
| `Keyhole_Train_Angle_Management` | 키홀 Train 각도 관리 | 2026-01 |
| `passschedule_legacy` | PassSchedule 레거시 코드 | 2026-01 |
| `PassScheduleService_Improvement` | PassSchedule 서비스 개선 | 2026-01 |
| `Slew_Loop_Mode` | Slew Loop 모드 구현 | 2026-01 |
| `Train_Algorithm` | Train 알고리즘 설계 | 2026-01 |

## 부활 방법

```bash
# 방법 1: /revive 스킬 사용 (권장)
/revive PassSchedule

# 방법 2: 수동 이동
mv docs/work/archive/{폴더명} docs/work/active/
```

## 정리 기준

- **완료된 작업**: PROGRESS.md에 `완료` 또는 `100%`
- **30일 이상 미수정**: 검토 후 아카이브
- **legacy 태그**: 즉시 아카이브