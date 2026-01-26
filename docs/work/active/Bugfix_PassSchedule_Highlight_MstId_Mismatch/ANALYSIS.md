# PassSchedule 하이라이트 MstId 불일치 버그 분석

## 1. 증상

WAITING(대기중) 상태에서:
- **기대**: 가장 가까운 미래 스케줄이 **파란색(다음 예정)**으로 하이라이트됨
- **실제**: 하이라이트되지 않음

### UI 상태
```
다음 예정: MstId 4 [대기중]   ← 헤더에 표시
테이블 No 18: mstId = 1       ← 테이블 데이터
```

## 2. 재현 단계

1. TLE 파일 업로드
2. Pass Schedule 생성
3. 스케줄 선택 후 추적 시작
4. WAITING 상태 진입 (추적 대기)
5. 테이블에서 가장 가까운 미래 스케줄 확인 → **하이라이트 없음**

## 3. 분석 과정

### 데이터 흐름

```
[BE] buildScheduleQueue()
    ↓
selectedTrackMstStorage.getAllSelectedTrackMst()
    ↓
schedule["MstId"] = 4  ← 🔴 BE 내부 ID
    ↓
scheduleContextQueue에 mstId: 4 저장
    ↓
WebSocket으로 nextTrackingMstId: 4 전송
    ↓
[FE] icdStore.nextTrackingMstId = 4
    ↓
[FE] ScheduleTable.vue 매칭:
    scheduleMstId(=1) === next(=4)  ← 🔴 FAIL!
```

### 핵심 함수

| 위치 | 함수 | 역할 |
|------|------|------|
| BE | `buildScheduleQueue()` | 큐 생성, MstId 추출 |
| BE | `getAllSelectedTrackMst()` | 선택된 스케줄 반환 |
| FE | `getRowStyleDirect()` | 행 스타일 결정 |

### 코드 추적

#### BE: buildScheduleQueue (PassScheduleService.kt:3455-3458)
```kotlin
val allSchedules = getAllSelectedTrackMst().values.flatten()
selectedSchedules.mapNotNull { schedule ->
    val mstId = (schedule["MstId"] as? Number)?.toLong()  // ← 여기서 MstId 추출
    ScheduleTrackingContext(mstId = mstId, ...)
}
```

#### FE: 테이블 데이터 (ScheduleTable.vue:129)
```typescript
const scheduleMstId = schedule.mstId ?? schedule.no  // ← 테이블의 mstId
```

#### FE: 하이라이트 매칭 (ScheduleTable.vue:141-146)
```typescript
const nextMatch =
  next !== null &&
  Number(scheduleMstId) === Number(next) &&  // ← 4 !== 1 → false
  (nextDetailId !== null && scheduleDetailId !== null &&
   Number(scheduleDetailId) === Number(nextDetailId))
```

## 4. 근본 원인 (Root Cause)

### 문제: FE와 BE의 MstId 데이터 소스 불일치

| 구분 | 데이터 소스 | MstId 값 |
|------|------------|----------|
| FE 테이블 | `scheduleData` (API 응답) | 1 |
| BE 큐 | `selectedTrackMstStorage` | 4 |

### Why 분석 (5 Whys)

1. **Why?** 하이라이트가 안 됨
2. **Why?** `nextMatch`가 false
3. **Why?** `scheduleMstId(1) !== next(4)`
4. **Why?** FE 테이블과 BE 큐의 MstId가 다름
5. **Why?** → **두 시스템이 서로 다른 ID를 참조**

### 가설

1. **FE `scheduleData`**: 스케줄 목록 조회 API에서 가져온 원본 mstId
2. **BE `selectedTrackMstStorage`**: 추적 대상 설정 시 별도 저장된 내부 mstId
3. **불일치 원인**:
   - 저장 시 새 ID가 할당되거나
   - 조회 API와 저장 API가 다른 ID 체계 사용

## 5. 영향 범위

| 영역 | 영향 | 설명 |
|------|:----:|------|
| Frontend | ✅ | 하이라이트 표시 불가 |
| Backend | 🟡 | ID 체계 검토 필요 |
| 기능 | 중간 | UX 문제, 기능은 정상 |

## 6. 관련 파일

| 파일 | 역할 | 검토 필요 |
|------|------|:--------:|
| `ScheduleTable.vue` | 하이라이트 매칭 | ✅ |
| `PassScheduleService.kt` | MstId 추출 | ✅ |
| `selectedTrackMstStorage` | ID 저장 | ✅ |
| 스케줄 조회 API | 원본 ID 반환 | ✅ |
