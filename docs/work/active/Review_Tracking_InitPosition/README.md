# Review_Tracking_InitPosition

**Review ID:** #R001
**상태:** ✅ 초기 프레임 스킵 수정 완료
**생성일:** 2026-01-27
**수정일:** 2026-01-27

## 개요

위성 추적 시작 시 빨간 점(현재 위치)이 이상한 곳으로 점프하는 버그 분석.

## 워크플로우

| 단계 | 스킬 | 날짜 | 결과 | 상태 |
|:----:|------|------|------|:----:|
| 1 | /review | 01-27 | #R001 | ✅ |
| 2 | /bugfix | - | 대기 | ⏳ |

## 이슈 추적

| Origin | 심각도 | 설명 | 상태 |
|--------|:------:|------|:----:|
| #R001-H1 | 🟠 | isValidAngle 0도 배제 (Ephemeris) | ⏳ |
| #R001-H2 | 🟠 | isValidAngle 0도 배제 (PassSchedule) | ⏳ |
| #R001-M1 | 🟡 | ChartUpdatePool 초기값 | ⏳ |
| #R001-M2 | 🟡 | PassChartUpdatePool 초기값 | ⏳ |
| #R001-M3 | 🟡 | resetTracking positionData 누락 | ⏳ |

## 파일 구조

```
Review_Tracking_InitPosition/
├── README.md       # 이 파일
└── REVIEW.md       # 상세 분석 결과
```

## 관련 파일

- [EphemerisDesignationPage.vue](../../../frontend/src/pages/mode/EphemerisDesignationPage.vue)
- [PassSchedulePage.vue](../../../frontend/src/pages/mode/PassSchedulePage.vue)
- [ephemerisTrackStore.ts](../../../frontend/src/stores/mode/ephemerisTrackStore.ts)