# PassSchedule MstId/DetailId 데이터 흐름 종합 리뷰

**Review ID**: #R002
**작성일**: 2026-01-26
**상태**: ✅ 분석 완료

---

## 1. 개요

| 항목 | 내용 |
|------|------|
| **범위** | PassSchedule 전체 MstId/DetailId 데이터 흐름 |
| **목적** | 하이라이트 불일치 문제의 근본 원인 파악 및 장기 수정 방안 도출 |
| **영향** | FE 테이블 하이라이트, 스케줄 선택, 추적 상태 표시 |

### 증상 요약

```
BE WebSocket: nextTrackingMstId = 4
FE 테이블:    schedule.mstId = 1
결과:         하이라이트 매칭 실패 ❌
```

---

## 2. 데이터 흐름 아키텍처

### 2.1 전체 흐름도

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              BACKEND                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐     ┌────────────────────────┐                        │
│  │ mstIdCounter     │────►│ passScheduleTrackMst   │◄── TLE 계산 결과       │
│  │ AtomicLong(0)    │     │ Storage                │                        │
│  └──────────────────┘     └───────────┬────────────┘                        │
│          │                            │                                      │
│          │                            ▼                                      │
│          │                ┌────────────────────────┐                        │
│          │                │ selectedTrackMst       │◄── 사용자 선택         │
│          │                │ Storage                │                        │
│          │                └───────────┬────────────┘                        │
│          │                            │                                      │
│          │                            ▼                                      │
│          │                ┌────────────────────────┐                        │
│          │                │ scheduleContextQueue   │◄── 추적 큐 생성        │
│          │                │ (ScheduleTrackingCtx)  │                        │
│          │                └───────────┬────────────┘                        │
│          │                            │                                      │
│          │                            ▼                                      │
│          │                ┌────────────────────────┐                        │
│          │                │ DataStoreService       │                        │
│          │                │ ├─ currentMstId        │                        │
│          └───────────────►│ ├─ nextMstId           │                        │
│                           │ ├─ currentDetailId     │                        │
│                           │ └─ nextDetailId        │                        │
│                           └───────────┬────────────┘                        │
│                                       │                                      │
│                                       ▼                                      │
│                           ┌────────────────────────┐                        │
│                           │ PushDataService        │                        │
│                           │ (WebSocket 전송)       │                        │
│                           └───────────┬────────────┘                        │
│                                       │                                      │
└───────────────────────────────────────┼──────────────────────────────────────┘
                                        │ WebSocket
                                        ▼
┌───────────────────────────────────────┼──────────────────────────────────────┐
│                              FRONTEND │                                      │
├───────────────────────────────────────┼──────────────────────────────────────┤
│                                       ▼                                      │
│                           ┌────────────────────────┐                        │
│                           │ icdStore               │                        │
│                           │ ├─ currentTrackingMstId│                        │
│                           │ ├─ nextTrackingMstId   │◄──────────┐            │
│                           │ ├─ currentDetailId     │           │            │
│                           │ └─ nextDetailId        │           │            │
│                           └───────────┬────────────┘           │            │
│                                       │                        │            │
│  ┌────────────────────────────────────┼────────────────────────┼──────────┐ │
│  │                                    │                        │          │ │
│  │  ┌─────────────────────┐          │   ┌─────────────────┐  │          │ │
│  │  │ API 응답            │          │   │ localStorage    │  │          │ │
│  │  │ (pass.MstId)        │          │   │ (selectedList)  │  │          │ │
│  │  └──────────┬──────────┘          │   └────────┬────────┘  │          │ │
│  │             │                     │            │           │          │ │
│  │             ▼                     │            ▼           │          │ │
│  │  ┌─────────────────────┐          │   ┌─────────────────┐  │          │ │
│  │  │ passScheduleStore   │          │   │ 복원 로직       │  │          │ │
│  │  │ selectedScheduleList│◄─────────┼───│ (mstId 유실?)   │  │          │ │
│  │  └──────────┬──────────┘          │   └─────────────────┘  │          │ │
│  │             │                     │                        │          │ │
│  │             ▼                     │                        │          │ │
│  │  ┌─────────────────────┐          │                        │          │ │
│  │  │ SelectSchedule      │          │                        │          │ │
│  │  │ Content.vue         │          │                        │          │ │
│  │  │ mstId: item.mstId   │          │                        │          │ │
│  │  │      ?? item.no ⚠️  │          │                        │          │ │
│  │  └──────────┬──────────┘          │                        │          │ │
│  │             │                     │                        │          │ │
│  └─────────────┼─────────────────────┼────────────────────────┼──────────┘ │
│                │                     │                        │            │
│                ▼                     ▼                        │            │
│  ┌─────────────────────────────────────────────────────────┐  │            │
│  │ ScheduleTable.vue                                       │  │            │
│  │                                                         │  │            │
│  │  scheduleMstId = schedule.mstId ?? schedule.no ⚠️       │  │            │
│  │                           │                             │  │            │
│  │                           ▼                             │  │            │
│  │  nextMatch = (scheduleMstId === next) && ...  ←─────────┘  │            │
│  │               ↑               ↑                            │            │
│  │               │               │                            │            │
│  │            = 1 (no)        = 4 (mstId)                     │            │
│  │                                                            │            │
│  │  결과: 1 !== 4 → 하이라이트 실패 ❌                        │            │
│  └────────────────────────────────────────────────────────────┘            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 ID 체계

| ID | 타입 | 생성 위치 | 범위 | 설명 |
|----|------|----------|------|------|
| **MstId** | Long | BE `mstIdCounter` | 전역 고유 | 패스 스케줄 마스터 ID |
| **DetailId** | Int | API 응답 | MstId 내 고유 | 상세 추적 인덱스 |
| **no** | Int | FE 계산 | UI 표시용 | 정렬 후 순번 (1부터) |

---

## 3. 발견된 이슈

### 3.1 Critical (즉시 수정)

| ID | 설명 | 위치 | 영향 |
|----|------|------|------|
| **#R002-C1** | Fallback 로직 불일치 | FE 여러 곳 | 하이라이트 실패 |
| **#R002-C2** | localStorage 복원 시 mstId 누락 가능성 | passScheduleStore | 데이터 손실 |

#### #R002-C1: Fallback 로직 불일치

**증거**: 3곳에서 서로 다른 fallback 사용

```typescript
// SelectScheduleContent.vue:354-358
mstId: item.mstId ?? item.no,  // ⚠️ no로 대체

// ScheduleTable.vue:129
const scheduleMstId = schedule.mstId ?? schedule.no  // ⚠️ no로 대체

// passScheduleStore.ts:1283
const mstId = pass.MstId  // ✅ 원본 사용 (fallback 없음)
```

**문제**: `item.mstId`가 null/undefined일 때 `no` (1부터 시작하는 UI 순번)로 대체됨
→ BE에서 보내는 `nextTrackingMstId=4`와 매칭 불가

#### #R002-C2: localStorage 복원 시 mstId 누락

**증거**: 저장/복원 로직에서 데이터 구조 불일치 가능성

```typescript
// 저장 시: selectedScheduleList 그대로 저장
// 복원 시: 다른 데이터 구조로 매핑될 수 있음
```

---

### 3.2 High (빠른 수정 권장)

| ID | 설명 | 위치 | 영향 |
|----|------|------|------|
| **#R002-H1** | Storage 간 동기화 미보장 | BE PassScheduleService | 데이터 불일치 |
| **#R002-H2** | 하이라이트 매칭 복잡도 과다 | ScheduleTable.vue | 유지보수 어려움 |
| **#R002-H3** | mstId 카운터 리셋 이슈 | BE mstIdCounter | 서버 재시작 시 충돌 |

#### #R002-H1: Storage 간 동기화

**증거**: 3개 저장소가 독립적으로 관리됨

```kotlin
// PassScheduleService.kt
passScheduleTrackMstStorage  // 전체 스케줄
selectedTrackMstStorage      // 선택된 스케줄
DataStoreService             // 현재/다음 추적 상태
```

**문제**: 한 저장소 업데이트 시 다른 저장소와 동기화 보장 없음

#### #R002-H2: 하이라이트 매칭 복잡도

**증거**: 5단계 매칭 로직

```typescript
// ScheduleTable.vue getRowStyleDirect()
1. schedule.mstId ?? schedule.no
2. schedule.detailId ?? null
3. Number() 변환
4. mstId 비교
5. detailId 비교 (조건부)
```

**문제**: 각 단계에서 null/undefined 처리가 다름 → 버그 발생 확률 높음

---

### 3.3 Medium (개선 권장)

| ID | 설명 | 위치 | 영향 |
|----|------|------|------|
| **#R002-M1** | 타입 안전성 부족 | FE 전체 | 런타임 오류 |
| **#R002-M2** | 디버깅 로그 부재 | FE/BE | 원인 추적 어려움 |
| **#R002-M3** | API 응답 검증 없음 | passScheduleStore | 잘못된 데이터 전파 |

#### #R002-M1: 타입 안전성

**증거**: number | null | undefined 혼용

```typescript
// icdStore.ts
const nextTrackingMstId = ref<number | null>(null)

// API 응답
pass.MstId  // 타입: unknown → Number?

// 비교 시
Number(scheduleMstId) === Number(next)  // NaN 발생 가능
```

---

### 3.4 Low (백로그)

| ID | 설명 | 위치 | 영향 |
|----|------|------|------|
| **#R002-L1** | 스케줄 선택 충돌 판정 복잡성 | SelectScheduleContent | UX |
| **#R002-L2** | 이론치 차트 정규화 이슈 | passScheduleService | 시각화 |

---

## 4. 권장 수정 방안

### 4.1 단기 (즉시 적용)

#### 방안 A: Fallback 로직 제거 + 검증 강화

**목표**: mstId가 null이면 오류로 처리, no로 대체하지 않음

```typescript
// SelectScheduleContent.vue
const scheduleData = computed(() => {
  return sortedData.value.map((item, index) => {
    // ✅ mstId 필수 검증
    if (item.mstId === null || item.mstId === undefined) {
      console.error('❌ mstId 누락:', item)
      // 또는 throw new Error()
    }
    return {
      mstId: item.mstId,  // fallback 제거
      detailId: item.detailId ?? 0,
      no: index + 1,  // UI 표시용 (별도 필드)
      // ...
    }
  })
})
```

```typescript
// ScheduleTable.vue
const getRowStyleDirect = (schedule) => {
  // ✅ mstId 필수, fallback 제거
  const scheduleMstId = schedule.mstId
  if (scheduleMstId === null || scheduleMstId === undefined) {
    console.warn('⚠️ mstId 없는 스케줄:', schedule)
    return {} // 하이라이트 없음
  }
  // ...
}
```

**장점**: 근본 원인 해결, 데이터 문제 즉시 발견
**단점**: 기존 데이터에 mstId 누락 시 오류 발생 → 데이터 마이그레이션 필요

#### 방안 B: 시간 기반 Fallback 매칭

**목표**: mstId 매칭 실패 시 시간으로 보조 매칭

```typescript
// ScheduleTable.vue
const isScheduleMatchByTime = (schedule, targetInfo) => {
  if (!targetInfo || !schedule.startTime) return false
  const scheduleStart = new Date(schedule.startTime).getTime()
  const targetStart = new Date(targetInfo.startTime).getTime()
  return Math.abs(scheduleStart - targetStart) < 1000 // 1초 이내
}

const nextMatch =
  (Number(scheduleMstId) === Number(next)) ||
  (next !== null && isScheduleMatchByTime(schedule, nextTrackingScheduleInfo))
```

**장점**: 빠른 수정, 영향 범위 최소
**단점**: 근본 원인 미해결, 동일 시간 스케줄 시 오매칭 가능

---

### 4.2 중기 (1-2주)

#### mstId 일관성 보장 시스템

1. **API 응답 검증**
```typescript
// passScheduleStore.ts
const validateScheduleData = (pass: unknown): ValidatedPass => {
  if (typeof pass.MstId !== 'number') {
    throw new Error(`Invalid MstId: ${pass.MstId}`)
  }
  return {
    mstId: pass.MstId,
    detailId: pass.DetailId ?? 0,
    // ...
  }
}
```

2. **localStorage 복원 검증**
```typescript
const restoreFromLocalStorage = () => {
  const saved = localStorage.getItem('selectedSchedules')
  if (!saved) return []

  const parsed = JSON.parse(saved)
  return parsed.filter(s => {
    if (s.mstId === null || s.mstId === undefined) {
      console.error('❌ 복원 시 mstId 누락, 제외:', s)
      return false
    }
    return true
  })
}
```

3. **BE Storage 동기화 강화**
```kotlin
// PassScheduleService.kt
@Synchronized
fun updateScheduleSelection(schedules: List<Schedule>) {
  // 트랜잭션처럼 모든 저장소 일괄 업데이트
  passScheduleTrackMstStorage.update(schedules)
  selectedTrackMstStorage.sync(schedules)
  dataStoreService.refreshTrackingIds()
}
```

---

### 4.3 장기 (리팩토링)

#### 단일 ID 체계 + 상태 관리 통합

```typescript
// 새로운 타입 정의
interface ScheduleIdentifier {
  mstId: number  // 필수, non-null
  detailId: number  // 필수, 기본값 0
}

// 통합 상태 관리
interface ScheduleState {
  all: Map<string, Schedule>  // key: `${mstId}_${detailId}`
  selected: Set<string>
  current: ScheduleIdentifier | null
  next: ScheduleIdentifier | null
}
```

---

## 5. 우선순위 로드맵

| 순위 | 이슈 ID | 방안 | 예상 영향 파일 |
|:----:|---------|------|---------------|
| 1 | #R002-C1 | Fallback 제거 + 검증 | 3개 |
| 2 | #R002-C2 | localStorage 복원 검증 | 1개 |
| 3 | #R002-H2 | 하이라이트 로직 단순화 | 1개 |
| 4 | #R002-M1 | 타입 강화 | 4개 |
| 5 | #R002-H1 | BE Storage 동기화 | 2개 |

---

## 6. 테스트 체크리스트

### 수정 후 확인

- [ ] WAITING 상태에서 다음 스케줄 파란색 하이라이트
- [ ] TRACKING 상태에서 현재 스케줄 녹색 하이라이트
- [ ] 브라우저 새로고침 후 하이라이트 유지
- [ ] 여러 스케줄 선택 후 하이라이트 정상

### 회귀 테스트

- [ ] 스케줄 선택 기능 정상
- [ ] 추적 시작/종료 정상
- [ ] 스케줄 전환 정상
- [ ] 빌드 성공

---

## 7. 관련 문서

| 문서 | 경로 |
|------|------|
| 하이라이트 버그 분석 | `Bugfix_PassSchedule_Highlight_MstId_Mismatch/` |
| 선택 충돌 버그 분석 | `Bugfix_PassSchedule_Selection_Conflict/` |
| BE 컨텍스트 | `docs/architecture/context/architecture/backend.md` |
| FE 컨텍스트 | `docs/architecture/context/architecture/frontend.md` |

---

## 8. 결론

### 근본 원인

**FE와 BE가 서로 다른 ID 소스를 참조**

- BE WebSocket: `DataStoreService.nextTrackingMstId` (추적 큐에서 추출)
- FE 테이블: `schedule.mstId ?? schedule.no` (API 응답 + fallback)

### 핵심 수정 포인트

1. **Fallback 로직 제거**: `mstId ?? no` 패턴 모두 제거
2. **데이터 검증 강화**: API 응답, localStorage 복원 시 mstId 필수 검증
3. **디버깅 로그 추가**: 불일치 발생 시 즉시 감지

### 권장 접근

**방안 A (Fallback 제거 + 검증)** 우선 적용
- 근본 원인 해결
- 향후 유사 버그 방지
- 데이터 품질 향상

---

**작성**: Claude Code Review Agent
**버전**: 1.0.0
