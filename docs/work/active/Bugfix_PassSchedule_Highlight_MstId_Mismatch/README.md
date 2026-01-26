# PassSchedule 하이라이트 MstId 불일치 버그

## 현재 상태: ✅ 수정 완료

## 개요

| 항목 | 내용 |
|------|------|
| **문제** | WAITING 상태에서 다음 스케줄 하이라이트 안 됨 |
| **심각도** | 🔴 Critical |
| **영향** | 하이라이트 매칭 실패 |
| **Review** | #R002 |

## 증상

- BE: `nextTrackingMstId = 4` 전송
- FE 테이블: `mstId = 1` 표시 (fallback으로 no 사용)
- 하이라이트 매칭 실패

## 원인

| # | 위치 | 문제 |
|:-:|------|------|
| 1 | SelectScheduleContent.vue:352 | uid를 순차 숫자로 덮어씀 |
| 2 | SelectScheduleContent.vue:354 | `mstId ?? no` fallback |
| 3 | ScheduleTable.vue:129,175 | `mstId ?? no` fallback |
| 4 | ScheduleTable.vue:141-146 | detailId null이면 매칭 실패 |

## 수정 내용

### SelectScheduleContent.vue

```diff
- uid: String(sortedIndex + 1),
- mstId: item.mstId ?? item.no,
+ uid: item.uid || `${item.mstId}_${item.detailId ?? 0}`,
+ mstId: item.mstId,
```

### ScheduleTable.vue

```diff
- const scheduleMstId = schedule.mstId ?? schedule.no
+ const scheduleMstId = schedule.mstId  // fallback 제거

- (nextDetailId !== null && scheduleDetailId !== null && ...)
+ (nextDetailId === null || Number(scheduleDetailId) === Number(nextDetailId))
```

## 파일

| 파일 | 설명 |
|------|------|
| [ANALYSIS.md](ANALYSIS.md) | 상세 분석 |
| [FIX.md](FIX.md) | 수정 방안 |

## 수정 파일

| 파일 | 변경 |
|------|------|
| `SelectScheduleContent.vue` | uid 원본 유지, mstId fallback 제거 |
| `ScheduleTable.vue` | fallback 제거, detailId 매칭 로직 수정 |

## 테스트

- [x] 빌드 성공
- [ ] WAITING 상태에서 다음 스케줄 파란색 하이라이트
- [ ] TRACKING 상태에서 현재 스케줄 녹색 하이라이트
- [ ] 스케줄 전환 시 하이라이트 정상 이동

## 관련 문서

- [Review #R002](../Review_PassSchedule_MstId_DataFlow/REVIEW.md)
