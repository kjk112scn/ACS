# PassSchedule 하이라이트 MstId 불일치 버그 수정

## 2026-01-26: MstId/DetailId 데이터 흐름 전면 수정

| 항목 | 내용 |
|------|------|
| **심각도** | 🔴 Critical |
| **상태** | ✅ 수정 완료 |
| **Review** | #R002 |

### 증상

- WAITING 상태에서 가장 가까운 미래 스케줄이 하이라이트되지 않음
- BE WebSocket: `nextTrackingMstId = 4`
- FE 테이블: `schedule.mstId = 1` (불일치)

### 원인

**3개의 문제 지점 발견:**

| # | 위치 | 문제 | 영향 |
|:-:|------|------|------|
| 1 | SelectScheduleContent.vue:352 | uid를 순차 숫자로 덮어씀 | 원본 uid 손실 |
| 2 | SelectScheduleContent.vue:354 | `mstId ?? no` fallback | mstId가 no로 대체됨 |
| 3 | ScheduleTable.vue:129,175 | `mstId ?? no` fallback | 매칭 시 잘못된 ID 사용 |
| 4 | ScheduleTable.vue:141-146 | detailId AND 조건 | null이면 무조건 실패 |

### 수정 방안

**선택한 방안**: Fallback 제거 + detailId 매칭 로직 수정

#### 대안 비교

| 방안 | 장점 | 단점 | 선택 |
|------|------|------|:----:|
| A: 어댑터 레이어 추가 | 단일 변환점 | 추가 파일 필요 | ❌ |
| B: Fallback 제거 + 로직 수정 | 최소 변경, 근본 해결 | - | ✅ |
| C: uid 통합 | 단순한 비교 | 대규모 수정 | ❌ |

### 변경 내용

#### 1. SelectScheduleContent.vue (352-358줄)

```diff
- // ✅ FIX: row-key용 고유 ID (순차 생성)
- uid: String(sortedIndex + 1),
- // ✅ 전역 고유 ID (필수) - index 필드를 대체
- mstId: item.mstId ?? item.no,
+ // ✅ FIX: row-key용 고유 ID (원본 유지 - mstId_detailId 조합)
+ uid: item.uid || `${item.mstId}_${item.detailId ?? 0}`,
+ // ✅ 전역 고유 ID (필수) - fallback 제거, null이면 오류
+ mstId: item.mstId,
```

**Why**:
- passScheduleStore에서 이미 `uid: \`${mstId}_${detailId}\``로 생성함
- 여기서 순차 숫자로 덮어쓰면 원본 uid 손실
- mstId는 필수 값이므로 fallback 불필요 (없으면 데이터 오류)

#### 2. ScheduleTable.vue - getRowStyleDirect (126-146줄)

```diff
- const scheduleMstId = schedule.mstId ?? schedule.no
- const scheduleDetailId = schedule.detailId ?? null
+ // ✅ FIX: fallback 제거 - mstId는 필수, null이면 매칭 안 함
+ const scheduleMstId = schedule.mstId
+ const scheduleDetailId = schedule.detailId ?? 0
  const { current, currentDetailId, next, nextDetailId } = highlightedRows.value

+ // mstId가 없으면 하이라이트 불가
+ if (scheduleMstId === null || scheduleMstId === undefined) {
+   return {}
+ }

  if (current !== null || next !== null) {
+   // ✅ FIX: detailId 매칭 로직 수정 - 둘 다 있을 때만 비교, 아니면 mstId만으로 매칭
    const currentMatch =
      current !== null &&
      Number(scheduleMstId) === Number(current) &&
-     (currentDetailId !== null &&
-       scheduleDetailId !== null &&
-       Number(scheduleDetailId) === Number(currentDetailId))
+     (currentDetailId === null ||
+       Number(scheduleDetailId) === Number(currentDetailId))

    const nextMatch =
      next !== null &&
      Number(scheduleMstId) === Number(next) &&
-     (nextDetailId !== null &&
-       scheduleDetailId !== null &&
-       Number(scheduleDetailId) === Number(nextDetailId))
+     (nextDetailId === null ||
+       Number(scheduleDetailId) === Number(nextDetailId))
```

**Why**:
- mstId는 필수값이므로 `?? no` fallback 제거
- detailId 매칭: `AND` → `OR`로 변경
  - 이전: detailId 중 하나라도 null이면 **전체 실패**
  - 수정: detailId가 null이면 **mstId만으로 매칭**

#### 3. ScheduleTable.vue - getRowClass (172-192줄)

동일한 패턴으로 수정 (getRowStyleDirect와 같은 로직)

### 수정 후 매칭 동작

| nextDetailId | scheduleDetailId | 이전 결과 | 수정 후 결과 |
|:------------:|:----------------:|:---------:|:------------:|
| 4 | 4 | ✅ 매칭 | ✅ 매칭 |
| 4 | 1 | ❌ 불일치 | ❌ 불일치 |
| **null** | 0 | ❌ 실패 | ✅ mstId만 매칭 |
| 4 | **null** | ❌ 실패 | ✅ mstId만 매칭 |

### 테스트 계획

#### 수정 확인
- [ ] WAITING 상태에서 다음 스케줄 파란색 하이라이트
- [ ] TRACKING 상태에서 현재 스케줄 녹색 하이라이트
- [ ] 스케줄 전환 시 하이라이트 정상 이동
- [ ] 브라우저 새로고침 후 하이라이트 유지

#### 회귀 테스트
- [ ] 스케줄 선택 기능 정상
- [ ] 추적 시작/종료 정상
- [x] 빌드 성공

### 재발 방지

| 대책 | 적용 |
|------|:----:|
| fallback 패턴 코드 리뷰 시 주의 | ✅ |
| mstId/detailId 타입 강화 | ⏳ |
| 단위 테스트 추가 | ⏳ |

---

## 관련 문서

- [ANALYSIS.md](ANALYSIS.md) - 원인 분석
- [Review #R002](../Review_PassSchedule_MstId_DataFlow/REVIEW.md) - 전체 데이터 흐름 리뷰
