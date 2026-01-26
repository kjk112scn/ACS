# PassSchedule 스케줄 선택 충돌 판정 버그 분석

## 1. 증상

TLE 업로드 후 스케줄 선택 시:
- 서로 **시간이 겹치지 않는** 스케줄인데도 선택이 안 됨
- 선택 자체는 되지만 충돌로 판정되어 비활성화됨

## 2. 재현 단계

1. TLE 파일 업로드
2. Pass Schedule 생성
3. 여러 스케줄 중 하나 선택
4. 시간이 겹치지 않는 다른 스케줄 선택 시도
5. 선택 불가 또는 경고 발생

## 3. 분석 과정

### 관련 파일
- `frontend/src/components/content/SelectScheduleContent.vue`

### 핵심 함수

| 함수 | 라인 | 역할 |
|------|------|------|
| `checkTimeOverlap()` | 418-459 | 두 스케줄 시간 겹침 검사 |
| `overlappingGroups` | 462-498 | 겹치는 스케줄 그룹 계산 |
| `canSelectSchedule()` | 512-529 | 선택 가능 여부 판단 |

### 코드 추적

#### `overlappingGroups` 계산 로직 (문제 발생 지점)

```typescript
data.forEach((schedule, index) => {
  if (processed.has(schedule.mstId)) return  // ⚠️ 이미 처리된 건 스킵

  const overlappingSchedules = [schedule.mstId]

  data.forEach((otherSchedule, otherIndex) => {
    if (index !== otherIndex && !processed.has(otherSchedule.mstId)) {  // ⚠️ processed 검사
      if (checkTimeOverlap(schedule, otherSchedule)) {
        overlappingSchedules.push(otherSchedule.mstId)
      }
    }
  })

  if (overlappingSchedules.length > 1) {
    groups.push(overlappingSchedules)
    overlappingSchedules.forEach(mstId => processed.add(mstId))  // ⚠️ 그룹 멤버 추가
  }
})
```

### 데이터 흐름

```
스케줄 데이터 → [overlappingGroups 계산] → [canSelectSchedule 판단] → UI 비활성화
                      │
                      └─ 🔴 전이적 관계 미처리
```

## 4. 근본 원인 (Root Cause)

### 문제: 그룹화 로직의 전이적 관계(Transitive Closure) 미처리

**예시 시나리오:**
- 스케줄 A: 10:00 ~ 11:00
- 스케줄 B: 10:30 ~ 11:30
- 스케줄 C: 11:00 ~ 12:00

**실제 겹침 관계:**
- A ↔ B: 겹침 (10:30~11:00)
- B ↔ C: 겹침 (11:00~11:30)
- A ↔ C: **겹치지 않음** (A 종료 = C 시작, 경계)

**현재 로직 실행:**
1. A 처리 → A와 B 겹침 → `[A, B]` 그룹 생성 → A, B를 `processed`에 추가
2. B는 이미 `processed` → **스킵**
3. C 처리 → B가 `processed`이므로 C와 B 겹침 검사 **안 함** → C는 독립

**결과:**
- 그룹: `[[A, B]]` (C는 어떤 그룹에도 없음)
- C는 `isScheduleOverlapping(C) = false` → 항상 선택 가능
- A와 C가 동시 선택 가능으로 판정됨 (올바름)
- **그러나** B와 C는 겹치는데도 C가 그룹에 없어서 동시 선택 가능으로 판정될 수 있음

### 실제 버그 시나리오

```
스케줄 순서가 [A, C, B]일 경우:

1. A 처리 → C와 비교 (안 겹침) → B와 비교 (겹침) → [A, B] 그룹
2. C 처리 → A는 processed → B는 processed → 아무것도 안 함
3. 결과: C가 그룹에 없음 → B와 C 겹침인데도 둘 다 선택 가능
```

### Why 분석 (5 Whys)

1. **Why?** 겹치지 않는 스케줄인데 선택이 안 됨
2. **Why?** `canSelectSchedule`이 false 반환
3. **Why?** `overlappingGroups`에 잘못된 스케줄이 포함됨
4. **Why?** 그룹화 시 `processed` 집합이 후속 비교를 차단
5. **Why?** → **전이적 관계(transitive closure)를 고려하지 않은 알고리즘 설계**

## 5. 영향 범위

| 영역 | 영향 | 설명 |
|------|:----:|------|
| Frontend | ✅ | SelectScheduleContent.vue |
| Backend | ❌ | 영향 없음 |
| Algorithm | ❌ | 시간 겹침 검사 자체는 정상 |

## 6. 관련 파일

| 파일 | 역할 | 수정 필요 |
|------|------|:--------:|
| `frontend/src/components/content/SelectScheduleContent.vue` | 스케줄 선택 UI | ✅ |

---

# PassSchedule 이론치 차트 표시 오류 분석 (2026-01-26)

## 1. 증상

TLE 업로드 후 Select Schedule에서 위성 선택 시:
- Position View의 **이론치(예정 궤적) 차트**가 잘못된 값으로 표시됨
- 실제 위성 궤적과 다른 이상한 패턴이 그려짐

## 2. 재현 단계

1. TLE 파일 업로드
2. Pass Schedule 생성
3. Select Schedule 버튼 클릭
4. 위성(스케줄) 선택
5. Position View에서 이론치 궤적 확인 → **비정상 패턴**

## 3. 분석 과정

### 데이터 흐름

```
TLE 업로드
    ↓
Select Schedule 모달에서 위성 선택
    ↓
passScheduleStore.loadTrackingDetailData(mstId, detailId, dataType)
    ↓
Backend API: /api/pass-schedule/tracking/detail/{mstId}/pass/{detailId}
    ↓
응답: TrackingDetailItem[] (Azimuth: ±270° 범위, Elevation: 0~90°)
    ↓
passScheduleService.convertToChartData() ← 🔴 문제 발생 지점
    ↓
predictedTrackingPath ref 업데이트
    ↓
PassSchedulePage의 차트 업데이트 (Polar 좌표: [elevation, azimuth])
```

### 핵심 함수 분석

**`passScheduleService.convertToChartData()` (lines 1025-1114)**

```typescript
// 문제 코드 (lines 1059-1076)
if (previousAzimuth !== null && acc.length > 0) {
  let delta = azimuth - previousAzimuth

  // 180도 이상 차이나면 반대 방향으로 보정
  if (delta > 180) {
    delta -= 360
  } else if (delta < -180) {
    delta += 360
  }

  // 🔴 문제 1: 변환된 값에 delta 추가
  const previousConverted = acc[acc.length - 1][1]
  azimuth = previousConverted + delta

  // 🔴 문제 2: 즉시 0~360° 정규화
  while (azimuth < 0) azimuth += 360
  while (azimuth >= 360) azimuth -= 360
}
```

### 버그 시나리오 예시

**입력 데이터 (백엔드 응답):**
| Index | Azimuth (원본) |
|-------|---------------|
| 0 | 350° |
| 1 | 355° |
| 2 | 360° → 0° (경계) |
| 3 | 5° |
| 4 | 10° |

**현재 로직 실행:**

| Step | previousAzimuth | azimuth (원본) | delta | previousConverted | 계산 | 정규화 후 |
|------|-----------------|----------------|-------|-------------------|------|-----------|
| 0 | null | 350 | - | - | 350 | **350** |
| 1 | 350 | 355 | +5 | 350 | 355 | **355** |
| 2 | 355 | 0 | -355 → +5 | 355 | 360 | **0** ← 경계에서 리셋 |
| 3 | 0 | 5 | +5 | 0 | 5 | **5** |
| 4 | 5 | 10 | +5 | 5 | 10 | **10** |

**문제점:**
- Step 2에서 `azimuth = 360`이 `0`으로 정규화됨
- 시각적으로 연속적인 궤적이 **0° 지점에서 점프**하는 것처럼 보임
- Polar 차트에서 360°→0° 경계를 넘는 궤적이 **급격한 회전**으로 표시됨

## 4. 근본 원인 (Root Cause)

**원인**: Azimuth 연속성 유지와 0~360° 범위 정규화의 충돌

### Why 분석 (5 Whys)

1. **Why?** 이론치 차트가 이상한 패턴으로 그려짐
2. **Why?** Azimuth 값이 360°/0° 경계에서 급격히 변함
3. **Why?** `convertToChartData`에서 연속성 유지 후 즉시 정규화
4. **Why?** 정규화가 연속성을 파괴함 (360° → 0°)
5. **Why?** → **연속성 유지와 범위 정규화가 양립 불가능한 로직 설계**

### 핵심 문제

```typescript
// 연속성 유지: previousConverted + delta
azimuth = previousConverted + delta  // 예: 355 + 5 = 360

// 즉시 정규화: 연속성 파괴
while (azimuth >= 360) azimuth -= 360  // 360 → 0 (점프!)
```

**시각화:**
```
기대 궤적:  ... 350° → 355° → 360°(=0°) → 5° → 10° ...  (부드러운 호)
현재 결과:  ... 350° → 355° → 0° → 5° → 10° ...         (급격한 점프)
```

## 5. 영향 범위

| 영역 | 영향 | 설명 |
|------|:----:|------|
| Frontend | ✅ | passScheduleService.ts |
| Backend | ❌ | 영향 없음 (데이터 자체는 정상) |
| Algorithm | ❌ | 영향 없음 |

## 6. 관련 파일

| 파일 | 역할 | 수정 필요 |
|------|------|:--------:|
| `frontend/src/services/mode/passScheduleService.ts` | 차트 데이터 변환 | ✅ |
| `frontend/src/pages/mode/PassSchedulePage.vue` | 차트 렌더링 | ❌ |
| `frontend/src/stores/mode/passScheduleStore.ts` | 상태 관리 | ❌ |