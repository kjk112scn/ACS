# PassSchedule 스케줄 선택 충돌 판정 버그 수정

## 2026-01-26: 스케줄 선택 시 잘못된 충돌 판정

| 항목 | 내용 |
|------|------|
| **심각도** | 🟠 High |
| **상태** | ✅ 수정 완료 |

### 증상

- 서로 시간이 겹치지 않는 스케줄인데도 선택이 안 됨
- TLE 업로드 후 스케줄 선택 시 발생

### 원인

`overlappingGroups` 그룹화 로직이 **전이적 관계(transitive closure)**를 처리하지 못함

- 스케줄 처리 순서에 따라 그룹이 다르게 형성됨
- 이미 `processed`된 스케줄은 후속 비교에서 제외됨
- 결과: 실제로 겹치는 스케줄이 다른 그룹으로 분류되거나, 그룹에서 누락됨

### 수정 방안

**선택한 방안**: `canSelectSchedule`에서 그룹화 의존 제거 → 선택된 스케줄과 직접 비교

#### 대안 비교

| 방안 | 장점 | 단점 | 선택 |
|------|------|------|:----:|
| A: Union-Find로 그룹화 개선 | 정확한 그룹화 | 복잡도 증가, 전체 리팩토링 필요 | ❌ |
| B: 직접 비교 방식으로 변경 | 단순, 확실, 최소 변경 | 선택 시마다 O(n) 검사 | ✅ |

### 변경 내용

#### `SelectScheduleContent.vue`

**기존 코드 (512-529줄):**
```typescript
const canSelectSchedule = (schedule: ScheduleItem): boolean => {
  // 겹치지 않는 스케줄은 항상 선택 가능
  if (!isScheduleOverlapping(schedule.mstId)) {
    return true
  }

  // 겹치는 스케줄인 경우, 같은 그룹의 다른 스케줄이 선택되어 있는지 확인
  const overlappingGroup = getOverlappingGroup(schedule.mstId)
  const otherSelectedInGroup = selectedRows.value.filter(selected => {
    return overlappingGroup.includes(selected.mstId) &&
      (selected.mstId !== schedule.mstId || selected.detailId !== schedule.detailId)
  })

  return otherSelectedInGroup.length === 0
}
```

**수정 코드:**
```typescript
const canSelectSchedule = (schedule: ScheduleItem): boolean => {
  // ✅ 그룹화에 의존하지 않고, 선택된 모든 스케줄과 직접 시간 겹침 검사
  const hasConflict = selectedRows.value.some(selected => {
    // 자기 자신은 제외
    if (selected.mstId === schedule.mstId && selected.detailId === schedule.detailId) {
      return false
    }
    // 시간 겹침 직접 검사
    return checkTimeOverlap(schedule, selected)
  })

  return !hasConflict
}
```

**Why**: 그룹화 로직의 복잡한 버그를 우회하여, 선택된 스케줄과 직접 비교함으로써 정확한 충돌 판정 보장

---

#### 추가 수정: mstId + detailId 조합으로 고유 식별

**문제**: 동일한 mstId에 여러 detailId 행이 존재할 수 있음 → mstId만으로 식별 시 잘못된 스케줄 참조

**수정된 함수들:**

1. **`isScheduleOverlapping`**: `(mstId: number)` → `(schedule: ScheduleItem)`
```typescript
const isScheduleOverlapping = (schedule: ScheduleItem): boolean => {
  return scheduleData.value.some(other =>
    (other.mstId !== schedule.mstId || other.detailId !== schedule.detailId) &&
    checkTimeOverlap(schedule, other)
  )
}
```

2. **`getOverlappingGroup` → `getOverlappingSchedules`**: mstId[] 반환 → ScheduleItem[] 반환
```typescript
const getOverlappingSchedules = (schedule: ScheduleItem): ScheduleItem[] => {
  return scheduleData.value.filter(other =>
    (other.mstId !== schedule.mstId || other.detailId !== schedule.detailId) &&
    checkTimeOverlap(schedule, other)
  )
}
```

3. **`overlappingGroups`**: `number[][]` → `string[][]` (키: `${mstId}_${detailId}`)
```typescript
const overlappingGroups = computed(() => {
  const getScheduleKey = (s: ScheduleItem) => `${s.mstId}_${s.detailId}`
  // ... 스케줄 키 기반 그룹화
})
```

4. **`showOverlapWarning`**: 직접 비교 방식으로 변경
5. **선택 상태 복원 로직**: 그룹 기반 → 직접 비교 방식 단순화

### 테스트 계획

#### 수정 확인
- [ ] 겹치지 않는 스케줄 다중 선택 가능
- [ ] 겹치는 스케줄 선택 시 경고/비활성화
- [ ] 스케줄 순서와 무관하게 동일한 결과

#### 회귀 테스트
- [ ] 기존 스케줄 선택 기능 정상 작동
- [x] 빌드 성공
- [ ] 브라우저 콘솔 에러 없음

### 재발 방지

| 대책 | 적용 |
|------|:----:|
| 그룹화 로직 단순화 | ✅ |
| 직접 비교 방식 사용 | ✅ |

---

## 2026-01-26: 이론치 차트(Position View) 표시 오류

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 Medium |
| **상태** | 🔧 수정중 |

### 증상

- TLE 업로드 후 스케줄 선택 시 Position View 이론치 궤적이 이상하게 표시됨
- 360°/0° 경계를 지나는 궤적에서 급격한 점프 발생

### 원인

`passScheduleService.convertToChartData` 함수에서:
1. Azimuth 연속성 유지를 위해 `previousConverted + delta` 계산
2. 직후 0~360° 정규화 수행 (`while (azimuth >= 360) azimuth -= 360`)
3. 정규화가 연속성을 파괴 (360° → 0° 점프)

### 수정 방안

**선택한 방안**: Polar 차트가 0~360° 외의 값도 처리 가능하므로, **정규화를 제거하고 연속성만 유지**

#### 대안 비교

| 방안 | 장점 | 단점 | 선택 |
|------|------|------|:----:|
| A: 정규화 제거 (연속 값 유지) | 단순, 최소 변경, ECharts 지원 | 값이 360 초과 가능 | ✅ |
| B: 궤적 분할 (360 경계에서) | 정확한 시각화 | 복잡도 증가, 다중 시리즈 필요 | ❌ |
| C: 각도 보간 | 매끄러운 궤적 | 데이터 왜곡 가능, 복잡 | ❌ |

### 변경 내용

#### `passScheduleService.ts` (lines 1074-1076)

**기존 코드:**
```typescript
// 이전 변환된 값에 delta를 더함 (연속성 유지)
const previousConverted = acc[acc.length - 1][1]
azimuth = previousConverted + delta

// 0~360° 범위로 정규화  ← 🔴 문제
while (azimuth < 0) azimuth += 360
while (azimuth >= 360) azimuth -= 360
```

**수정 코드:**
```typescript
// 이전 변환된 값에 delta를 더함 (연속성 유지)
const previousConverted = acc[acc.length - 1][1]
azimuth = previousConverted + delta

// ✅ 정규화 제거: ECharts Polar 차트는 360° 초과/음수 값도 올바르게 처리
// 연속성 유지를 위해 값을 그대로 사용
```

**Why**: ECharts의 angleAxis는 `type: 'value'`로 설정되어 있고, min: 0, max: 360 범위를 지정해도 실제 데이터는 이 범위를 벗어날 수 있음. 차트는 모듈러 연산을 내부적으로 수행하여 올바르게 표시함. 정규화를 제거하면 연속성이 유지되어 부드러운 궤적이 그려짐.

### 테스트 계획

#### 수정 확인
- [ ] 360°/0° 경계를 지나는 궤적이 부드럽게 표시
- [ ] 이론치 궤적이 실제 예상 경로와 일치
- [ ] 다양한 위성 선택 시 정상 동작

#### 회귀 테스트
- [ ] 기존 추적 경로 표시 정상
- [ ] 실시간 추적 위치 표시 정상
- [ ] 빌드 성공

### 재발 방지

| 대책 | 적용 |
|------|:----:|
| Polar 차트 값 범위 특성 문서화 | ⏳ |
| 정규화 로직 단순화 | ✅ |
