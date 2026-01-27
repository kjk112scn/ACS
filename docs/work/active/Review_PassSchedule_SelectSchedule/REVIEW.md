# Code Review: PassSchedule Select Schedule 화면 데이터 이상 문제

**Review ID**: #R001
**대상**: PassSchedulePage.vue → SelectScheduleContent.vue 데이터 흐름
**심각도 분류**: C(Critical), H(High), M(Medium), L(Low)
**작성일**: 2026-01-27

---

## 1. 문제 설명

사용자 보고:
> PassSchedulePage.vue에서 위성 TLE 입력 후 연산을 완료하고 Select Schedule 화면에 들어가면 이상한 데이터가 출력됨

---

## 2. 분석 범위

| 파일 | 역할 |
|------|------|
| `frontend/src/pages/mode/PassSchedulePage.vue` | 메인 페이지, Select Schedule 모달 호출 |
| `frontend/src/components/content/SelectScheduleContent.vue` | 스케줄 선택 모달 UI |
| `frontend/src/stores/mode/passScheduleStore.ts` | 스케줄 데이터 상태 관리 |
| `frontend/src/services/mode/passScheduleService.ts` | BE API 호출 |

---

## 3. 데이터 흐름 분석

```
[TLE 입력] → [BE API: tle-and-tracking] → [passScheduleStore.scheduleData 업데이트]
                                                         ↓
                                              [localStorage 저장]
                                                         ↓
[Select Schedule 클릭] → [loadScheduleDataFromLocalStorage] → [SelectScheduleContent 표시]
                                   ↓ (캐시 없음 시)
                         [fetchScheduleDataFromServer]
```

---

## 4. 발견된 이슈

### #R001-C1 🔴 Critical: 캐시 우선 로직으로 인한 데이터 불일치

**위치**: [PassSchedulePage.vue:2069-2088](frontend/src/pages/mode/PassSchedulePage.vue#L2069-L2088)

**문제**:
```typescript
const selectScheduleData = async () => {
  // ✅ 1순위: localStorage에서 스케줄 데이터 로드 (빠름)
  const cached = passScheduleStore.loadScheduleDataFromLocalStorage()

  if (cached && passScheduleStore.scheduleData.length > 0) {
    console.log('✅ 캐시된 스케줄 데이터 사용 (API 호출 생략)')
  } else {
    // ✅ 2순위: API 호출 (캐시가 없을 때만)
    await passScheduleStore.fetchScheduleDataFromServer()
  }
  // ... 모달 열기
}
```

**영향**:
1. TLE 업로드 완료 후에도 **이전 캐시 데이터**가 표시될 수 있음
2. Store의 `scheduleData`가 이미 업데이트되었더라도 `loadScheduleDataFromLocalStorage()`가 localStorage 값을 덮어씀
3. 사용자가 방금 입력한 TLE의 스케줄이 아닌 **과거 데이터**가 보임

**근본 원인**:
- TLE 업로드 시 `fetchScheduleDataFromServer(true)` + `saveScheduleDataToLocalStorage()` 호출됨 (line 961-976)
- 그러나 `selectScheduleData()`는 `loadScheduleDataFromLocalStorage()` 성공 시 Store의 현재 값을 무시

---

### #R001-H1 🟠 High: `no` 필드 재계산 중복으로 인한 혼란

**위치**:
- Store: [passScheduleStore.ts:1412-1416](frontend/src/stores/mode/passScheduleStore.ts#L1412-L1416)
- Component: [SelectScheduleContent.vue:357](frontend/src/components/content/SelectScheduleContent.vue#L357)

**문제**:
```typescript
// Store에서 정렬 후 no 부여
allSchedules.forEach((schedule, index) => {
  schedule.no = index + 1 // UI 표시용 재순번
})

// SelectScheduleContent에서 다시 정렬 후 no 재부여
const result = sortedData.map((item, sortedIndex) => {
  return {
    ...itemWithoutIndex,
    no: sortedIndex + 1  // ✅ UI 표시용 재순번
  }
})
```

**영향**:
1. 동일한 로직이 두 번 실행됨 (불필요한 연산)
2. localStorage에 저장된 `no`와 화면에 표시되는 `no`가 다를 수 있음
3. "이상한 데이터" 현상의 원인 중 하나일 수 있음

---

### #R001-H2 🟠 High: scheduleData computed의 불필요한 데이터 변환

**위치**: [SelectScheduleContent.vue:318-363](frontend/src/components/content/SelectScheduleContent.vue#L318-L363)

**문제**:
```typescript
const scheduleData = computed(() => {
  const rawData = passScheduleStore.scheduleData

  // 시간 순으로 정렬 (Store에서 이미 정렬됨)
  const sortedData = [...rawData].sort((a, b) => {
    return new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
  })

  // index 필드 제거 후 재구성
  const result = sortedData.map((item, sortedIndex) => {
    const { index: _index, ...itemWithoutIndex } = item as ScheduleItem & { index?: number }
    return {
      ...itemWithoutIndex,
      uid: item.uid || `${item.mstId}_${item.detailId ?? 0}`,
      mstId: item.mstId,
      detailId: item.detailId ?? 0,
      no: sortedIndex + 1
    }
  })
  return result
})
```

**영향**:
1. Store에서 이미 정렬된 데이터를 다시 정렬
2. 객체를 spread 연산자로 복사하면서 필드가 누락되거나 변형될 수 있음
3. `mstId: item.mstId`에서 원본 `mstId`가 `undefined`일 경우 문제 발생

---

### #R001-M1 🟡 Medium: localStorage 캐시 만료 로직 부재

**위치**: [passScheduleStore.ts:1025-1055](frontend/src/stores/mode/passScheduleStore.ts#L1025-L1055)

**문제**:
```typescript
const loadScheduleDataFromLocalStorage = (): boolean => {
  const savedData = localStorage.getItem(storageKey)
  const parsed = JSON.parse(savedData)

  // savedAt 타임스탬프가 있지만 만료 체크를 하지 않음
  if (parsed.scheduleData && parsed.scheduleData.length > 0) {
    scheduleData.value = parsed.scheduleData
    return true
  }
}
```

**영향**:
- 오래된 캐시 데이터가 영구적으로 사용될 수 있음
- TLE 삭제/수정 후에도 캐시 데이터가 남아있음

---

### #R001-L1 💡 Low: 불필요한 디버그 로그

**위치**:
- [SelectScheduleContent.vue:320](frontend/src/components/content/SelectScheduleContent.vue#L320)
- [passScheduleStore.ts:1282-1291](frontend/src/stores/mode/passScheduleStore.ts#L1282-L1291)

**문제**:
```typescript
console.log('🔍 원본 데이터 확인:', rawData.slice(0, 3))
console.log('🔍 패스 데이터 원본...', { ... })
```

**영향**:
- 프로덕션 환경에서 성능 영향
- 콘솔 로그 오염

---

## 5. 권장 수정 방안

### #R001-C1 해결: 캐시 무효화 및 항상 최신 데이터 사용

```typescript
// PassSchedulePage.vue
const selectScheduleData = async () => {
  try {
    // ✅ 항상 Store의 현재 데이터 사용 (캐시 로드 제거)
    // Store가 비어있으면 API 호출
    if (passScheduleStore.scheduleData.length === 0) {
      console.log('📡 스케줄 데이터 API 호출 (Store 비어있음)')
      await passScheduleStore.fetchScheduleDataFromServer()
    } else {
      console.log('✅ Store의 현재 스케줄 데이터 사용:', passScheduleStore.scheduleData.length, '개')
    }

    // 모달 열기
    const modal = await openModal('select-schedule', { ... })
  } catch (err) {
    handleApiError(err, '스케줄 선택 모달 열기')
  }
}
```

### #R001-H1, H2 해결: SelectScheduleContent 데이터 변환 간소화

```typescript
// SelectScheduleContent.vue
const scheduleData = computed(() => {
  // Store 데이터 그대로 사용 (이미 정렬됨)
  return passScheduleStore.scheduleData
})
```

---

## 6. 이슈 요약

| Issue ID | 심각도 | 문제 | 위치 | 권장 조치 |
|----------|:------:|------|------|----------|
| #R001-C1 | 🔴 Critical | 캐시 우선 로직으로 이전 데이터 표시 | PassSchedulePage.vue:2069-2088 | ✅ 완료 |
| #R001-H1 | 🟠 High | no 필드 중복 재계산 | Store + Component | ⏸️ SWR 적용 시 |
| #R001-H2 | 🟠 High | 불필요한 데이터 변환 | SelectScheduleContent.vue:318-363 | ⏸️ SWR 적용 시 |
| #R001-M1 | 🟡 Medium | 캐시 만료 로직 부재 | passScheduleStore.ts:1025-1055 | ⏸️ SWR 적용 시 |
| #R001-L1 | 💡 Low | 디버그 로그 정리 필요 | 여러 파일 | `/cleanup` |

---

## 7. 다음 단계

**권장**: #R001-C1 먼저 `/bugfix` 실행

```bash
/bugfix #R001-C1  # Critical 이슈 즉시 수정
```

---

**Review 완료**: 2026-01-27
**Reviewer**: Claude Code