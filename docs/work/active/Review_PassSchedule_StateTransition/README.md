# PassSchedule 상태 전이 + 하이라이트 심층 리뷰

**생성일**: 2026-01-26
**현재 Phase**: ✅ 분석 완료
**Review ID**: #R004

---

## 요약

| 항목 | 값 |
|------|-----|
| 대상 | PassSchedule 상태 전이, 하이라이트 업데이트 |
| 증상 | WAITING→TRACKING 전환 시 녹색 안 됨, 다음 스케줄 파란색 이동 안 됨 |
| 관련 | #R001 (상태머신), #R002 (MstId 흐름) |

---

## 증상 상세

| 현상 | 예상 동작 | 실제 동작 | 상태 |
|------|----------|----------|:----:|
| 파란색 (next) | 다음 스케줄 표시 | ✅ 동작 | ✅ |
| 녹색 (current) | 추적 중 스케줄 표시 | ❌ 안 됨 | 🔴 |
| 시간 오프셋 변경 | TRACKING으로 전환 | ❌ 녹색 미표시 | 🔴 |
| 다음 스케줄 이동 | 파란색 다음으로 이동 | ❌ 안 됨 | 🔴 |

---

## 워크플로우

| 단계 | 스킬 | 날짜 | 결과 | 상태 |
|:----:|------|------|------|:----:|
| 1 | /review | 01-26 | [phases/01_review.md](phases/01_review.md) | ✅ |
| 2 | /bugfix | 01-26 | [FIX.md](FIX.md) - Critical 2개 수정 완료 | ✅ |
| 3 | 테스트 | - | BE 재시작 후 확인 필요 | ⏳ |

---

## 발견된 이슈

| ID | 심각도 | 문제 | 위치 | 상태 |
|----|:------:|------|------|:----:|
| #R004-C1 | 🔴 Critical | handleTimeOffsetChange() 미호출 | PassScheduleService.kt:683-704 | ✅ |
| #R004-C2 | 🔴 Critical | validTransitions 시간 점프 불허 | PassScheduleService.kt:231-242 | ✅ |
| #R004-H1 | 🟠 High | 잘못된 전환 시 ERROR 상태 | PassScheduleService.kt:2960-2966 | ⏳ |
| #R004-M1 | 🟡 Medium | isAfter(startTime) 시작 시간 미포함 | PassScheduleService.kt:2770-2780 | ⏳ |

---

## 다음 단계

**권장: Critical 이슈 즉시 수정**

1. `/bugfix #R004-C1` - handleTimeOffsetChange() 추가
2. `/bugfix #R004-C2` - validTransitions 시간 점프 허용

---

## 분석 범위

### BE (PassScheduleService.kt)

| 분석 항목 | 중점 |
|----------|------|
| 상태 전이 로직 | WAITING → TRACKING 언제 전환? |
| currentTrackingMstId 업데이트 | 언제, 어떻게 설정? |
| nextTrackingMstId 업데이트 | 전환 후 다음 스케줄 계산 |
| WebSocket 전송 | current/next 전송 타이밍 |

### FE (PassSchedulePage.vue, passScheduleStore.ts)

| 분석 항목 | 중점 |
|----------|------|
| WebSocket 수신 | current/next 값 수신 처리 |
| 하이라이트 매칭 | getScheduleRowStyle 로직 |
| 상태 반영 | currentScheduleStatus computed |
| 시간 오프셋 | 시뮬레이션 시간 반영 |

---

## 참조 문서

- [#R001 상태머신](../Review_PassSchedule_State/README.md)
- [#R002 MstId 흐름](../Review_PassSchedule_MstId_DataFlow/README.md)
- [Phase 2 수정](../Bugfix_PassSchedule_Highlight_MstId_Mismatch/FIX.md)
