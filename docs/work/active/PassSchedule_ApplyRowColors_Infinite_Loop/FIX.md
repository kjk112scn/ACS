# PassSchedule_ApplyRowColors_Infinite_Loop 수정 계획

## 1. 수정 전략

**선택된 방안**: Option A + B + C 조합 (사용자 승인: 2026-01-06)

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **Option A** | 중복 watch 제거 | 트리거 횟수 감소 | - |
| **Option B** | 디바운스 적용 | 연속 호출 방지 | - |
| **Option C** | 값 변경 체크 추가 | 바뀔 때만 로그+색상 적용 | - |

## 2. 수정 내용

### 2.1 중복 Watch 제거 (Line 1163-1186)

**삭제할 코드:**
```typescript
// 🔴 삭제: Line 1163-1186
watch(
  [
    () => icdStore?.currentTrackingMstId,
    () => icdStore?.currentTrackingDetailId,
    () => icdStore?.nextTrackingMstId,
    () => icdStore?.nextTrackingDetailId
  ],
  (newValues, oldValues) => {
    // ...
    setTimeout(() => {
      applyRowColors()
    }, 100)
  },
  { immediate: true, deep: true }
)
```

**이유:**
- Watch #1 (Line 528-534)이 이미 `currentTrackingMstId`, `currentTrackingDetailId` 감시
- Watch #2 (Line 675-682)이 이미 `nextTrackingMstId`, `nextTrackingDetailId` 감시
- Watch #3은 완전히 중복 + `deep: true`로 인해 과도한 트리거 발생

### 2.2 기존 Watch에 디바운스 적용

**수정할 코드 (Watch #1, Line 528-534):**
```typescript
// 수정 전
watch([() => icdStore.currentTrackingMstId, () => icdStore.currentTrackingDetailId], () => {
  applyRowColors()
})

// 수정 후
let applyRowColorsDebounceTimer: ReturnType<typeof setTimeout> | null = null

watch([() => icdStore.currentTrackingMstId, () => icdStore.currentTrackingDetailId], () => {
  if (applyRowColorsDebounceTimer) {
    clearTimeout(applyRowColorsDebounceTimer)
  }
  applyRowColorsDebounceTimer = setTimeout(() => {
    applyRowColors()
  }, 100)
})
```

**수정할 코드 (Watch #2, Line 675-682):**
```typescript
// 동일한 디바운스 타이머 공유하여 중복 호출 방지
watch([() => icdStore.nextTrackingMstId, () => icdStore.nextTrackingDetailId], () => {
  if (applyRowColorsDebounceTimer) {
    clearTimeout(applyRowColorsDebounceTimer)
  }
  applyRowColorsDebounceTimer = setTimeout(() => {
    applyRowColors()
  }, 100)
})
```

### 2.3 applyRowColors() 내부 값 체크 추가 (선택적)

```typescript
// applyRowColors() 함수 시작 부분에 추가
let lastAppliedState = {
  current: null as number | null,
  currentDetail: null as number | null,
  next: null as number | null,
  nextDetail: null as number | null
}

function applyRowColors() {
  const currentState = {
    current: icdStore.currentTrackingMstId,
    currentDetail: icdStore.currentTrackingDetailId,
    next: icdStore.nextTrackingMstId,
    nextDetail: icdStore.nextTrackingDetailId
  }

  // 값이 동일하면 스킵
  if (JSON.stringify(currentState) === JSON.stringify(lastAppliedState)) {
    return
  }
  lastAppliedState = { ...currentState }

  // 기존 로직...
}
```

## 3. 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/pages/mode/PassSchedulePage.vue` | Watch 중복 제거, 디바운스 추가 |

## 4. 테스트 계획

### 4.1 기능 테스트

| 테스트 항목 | 예상 결과 |
|------------|----------|
| Pass Schedule 페이지 진입 | 콘솔에 "DOM 직접 조작" 로그가 1번만 출력 |
| 스케줄 선택 시 | 하이라이트가 정상 적용, 로그 중복 없음 |
| WebSocket 데이터 수신 시 | 값 변경 시에만 DOM 업데이트 |

### 4.2 성능 테스트

| 항목 | 수정 전 | 예상 수정 후 |
|------|--------|-------------|
| applyRowColors() 호출/초 | 10+ 회 | 0-1 회 |
| 콘솔 로그 출력 | 무한 | 값 변경 시에만 |

## 5. 롤백 계획

문제 발생 시 git revert로 원복 가능.

## 6. 작업 순서

1. [ ] `PassSchedulePage.vue` 백업 (git stash 또는 복사)
2. [ ] 중복 Watch (Line 1163-1186) 제거
3. [ ] 디바운스 타이머 변수 선언 추가
4. [ ] Watch #1, #2에 디바운스 적용
5. [ ] 빌드 테스트 (`npm run build`)
6. [ ] 기능 테스트 (페이지 진입 후 콘솔 확인)
7. [ ] 커밋
