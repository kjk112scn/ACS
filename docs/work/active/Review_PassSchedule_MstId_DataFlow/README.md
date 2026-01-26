# PassSchedule MstId/DetailId 데이터 흐름 리뷰

## 현재 상태: ✅ 분석 완료

## 개요

| 항목 | 내용 |
|------|------|
| **Review ID** | #R002 |
| **범위** | PassSchedule MstId/DetailId 전체 데이터 흐름 |
| **목적** | 하이라이트 불일치 버그의 근본 원인 분석 및 장기 수정 방안 도출 |

## 핵심 발견

### 근본 원인

```
BE WebSocket: nextTrackingMstId = 4  (원본 MstId)
FE 테이블:    schedule.mstId = 1     (fallback으로 no 사용)
결과:         매칭 실패 → 하이라이트 안 됨
```

### 문제 위치

| 파일 | 문제 | 심각도 |
|------|------|:------:|
| SelectScheduleContent.vue:354 | `mstId: item.mstId ?? item.no` | Critical |
| ScheduleTable.vue:129 | `schedule.mstId ?? schedule.no` | Critical |
| passScheduleStore.ts (복원) | localStorage 복원 시 mstId 누락 가능 | Critical |

## 발견된 이슈

| ID | 심각도 | 설명 | 상태 |
|----|:------:|------|:----:|
| #R002-C1 | Critical | Fallback 로직 불일치 | ⏳ |
| #R002-C2 | Critical | localStorage 복원 시 mstId 누락 | ⏳ |
| #R002-H1 | High | Storage 간 동기화 미보장 | ⏳ |
| #R002-H2 | High | 하이라이트 매칭 복잡도 과다 | ⏳ |
| #R002-H3 | High | mstId 카운터 리셋 이슈 | ⏳ |
| #R002-M1 | Medium | 타입 안전성 부족 | ⏳ |
| #R002-M2 | Medium | 디버깅 로그 부재 | ⏳ |
| #R002-M3 | Medium | API 응답 검증 없음 | ⏳ |

## 권장 수정

### 즉시 (방안 A 권장)

**Fallback 로직 제거 + 검증 강화**

```typescript
// BEFORE (문제)
mstId: item.mstId ?? item.no  // no로 대체 → 불일치

// AFTER (수정)
if (item.mstId === null || item.mstId === undefined) {
  console.error('❌ mstId 누락:', item)
}
mstId: item.mstId  // fallback 없이 원본 사용
```

### 우선순위

1. #R002-C1: Fallback 제거 (3개 파일)
2. #R002-C2: localStorage 복원 검증 (1개 파일)
3. #R002-H2: 하이라이트 로직 단순화 (1개 파일)

## 파일 구조

| 파일 | 설명 |
|------|------|
| [REVIEW.md](REVIEW.md) | 상세 분석 및 수정 방안 |

## 다음 단계

1. `/bugfix #R002-C1` - Fallback 로직 제거
2. `/bugfix #R002-C2` - localStorage 복원 검증
3. 테스트 및 검증

## 관련 버그

- `Bugfix_PassSchedule_Highlight_MstId_Mismatch/` - 하이라이트 불일치
- `Bugfix_PassSchedule_Selection_Conflict/` - 선택 충돌 판정
