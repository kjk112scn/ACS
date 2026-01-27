# Review: 위성 추적 시작 시 빨간 점 초기 위치 버그

**Review ID:** #R001
**대상:** Ephemeris + PassSchedule 모드 추적 시작 시 초기 좌표 처리
**심각도:** 🔴 High (시각적 오류)
**작성일:** 2026-01-27

---

## 요약

위성 추적 시작 시 빨간 점(현재 위치 마커)이 이상한 곳으로 점프했다가 정상 추적되는 현상.
**원인:** 0도를 "무효한 값"으로 처리하는 로직 + ChartUpdatePool 초기값 문제

---

## 발견된 이슈

| ID | 심각도 | 문제 | 위치 | 상태 |
|----|:------:|------|------|:----:|
| #R001-H1 | 🟠 High | `isValidAngle(0) = false` - 0도 배제 | EphemerisDesignationPage.vue:969 | ⏳ |
| #R001-H2 | 🟠 High | `isValidAngle(0) = false` - 0도 배제 | PassSchedulePage.vue:469 | ⏳ |
| #R001-M1 | 🟡 Medium | ChartUpdatePool 초기값 `[[0,0]]` | EphemerisDesignationPage.vue:1220 | ⏳ |
| #R001-M2 | 🟡 Medium | PassChartUpdatePool 초기값 `[[0,0]]` | PassSchedulePage.vue:188 | ⏳ |
| #R001-M3 | 🟡 Medium | `resetTracking()`이 positionData 미초기화 | EphemerisDesignationPage.vue:1275-1278 | ⏳ |
| #R001-L1 | 🟢 Low | ScheduleChart도 동일 패턴 | ScheduleChart.vue:50 | ⏳ |
| **#R001-FIX** | ✅ | **초기 5프레임 스킵으로 하드웨어 튀는 값 방지** | 두 파일 | ✅ 완료 |

---

## 상세 분석

### 1. 핵심 문제: 0도 검증 로직

**문제 코드 (두 파일 동일):**
```typescript
// EphemerisDesignationPage.vue:969, PassSchedulePage.vue:469
const isValidAngle = (val: number) => !isNaN(val) && val !== 0
//                                                      ^^^^^^^
//                              ❌ 0도를 "무효"로 처리 - 정남(0°)도 유효한 좌표!
```

**영향:**
- 안테나 실제 위치가 0도(정남)일 때 → "무효" 판정 → Fallback 체인 실행
- 잘못된 좌표(스케줄 예측 시작점 등)로 초기화됨

**Fallback 체인:**
```
normalAz (0) → "무효" → trackingActualAz (undefined) → "무효"
→ trackingCmdAz (undefined) → "무효" → startAzimuth (예측값) ← 사용됨!
```

### 2. ChartUpdatePool 초기값 문제

**문제 코드:**
```typescript
// EphemerisDesignationPage.vue:1220, PassSchedulePage.vue:188
private positionData: [number, number][] = [[0, 0]]  // ← 남극 근처 (El=0, Az=0)
```

**영향:**
- 차트 초기화 시 빨간 점이 (0,0) = 우측 하단 모서리에 표시
- 첫 `updatePosition()` 호출 전까지 유지

### 3. resetTracking() 불완전

**문제 코드:**
```typescript
// EphemerisDesignationPage.vue:1275-1278
resetTracking() {
  this.trackingData.length = 0     // ✅ 흰 선 초기화
  this.lastTrackingLength = 0
  // ❌ positionData 초기화 없음!
}
```

**영향:**
- 추적 재시작 시 positionData가 이전 값 유지
- 새 위치 업데이트 전까지 구 좌표 표시

---

## 타이밍 시퀀스

```
T0: TRACKING 상태 전환
    ├─ ICD 패킷 아직 미수신 또는 초기값(0,0)
    └─ icdStore: azimuthAngle = "" 또는 "0"

T1: Watch 감지 (100ms 이내)
    ├─ parseFloat("") = NaN 또는 parseFloat("0") = 0
    ├─ isValidAngle(0/NaN) = false
    ├─ Fallback → startAzimuth 사용 ← ⚠️ 잘못된 초기화
    └─ clearTrackingPath(잘못된좌표)

T2: updateChart() (100ms 주기)
    ├─ chartPool.positionData = [[0, 0]] (아직 미갱신)
    └─ 차트 렌더링 → 빨간 점 (0,0) 또는 잘못된 위치

T3: ICD 정상 좌표 수신
    └─ updatePosition(정상El, 정상Az) → 빨간 점 정상 위치로 이동 (점프!)
```

---

## 권장 수정안

### Fix 1: isValidAngle 수정 (#R001-H1, #R001-H2)

```typescript
// Before
const isValidAngle = (val: number) => !isNaN(val) && val !== 0

// After - 0도도 유효한 값으로 처리
const isValidAngle = (val: number) => !isNaN(val) && isFinite(val)

// 또는 더 명시적으로
const isValidAngle = (val: number) =>
  typeof val === 'number' && !isNaN(val) && isFinite(val)
```

### Fix 2: (0,0) 판별 로직 분리

```typescript
// (0,0) 체크는 별도로 - 둘 다 0일 때만 무효
const hasValidPosition = (az: number, el: number) =>
  isValidAngle(az) && isValidAngle(el) && !(az === 0 && el === 0)
```

### Fix 3: ChartUpdatePool 초기화 개선 (#R001-M1, #R001-M2)

```typescript
class ChartUpdatePool {
  private positionData: [number, number][] = []  // 빈 배열로 시작

  // 또는 초기 위치 설정 메서드 추가
  initPosition(elevation: number, azimuth: number) {
    this.positionData = [[elevation, azimuth]]
    // 시리즈 참조 업데이트
    if (this.updateOption.series[0]) {
      this.updateOption.series[0].data = this.positionData
    }
  }

  resetTracking(initialEl?: number, initialAz?: number) {
    this.trackingData.length = 0
    this.lastTrackingLength = 0
    // ✅ positionData도 초기화
    if (initialEl !== undefined && initialAz !== undefined) {
      this.initPosition(initialEl, initialAz)
    }
  }
}
```

### Fix 4: 초기값 튀는 현상 방지 (선택적)

```typescript
// 첫 N개 샘플 스킵 또는 이동 평균 적용
const INITIAL_SKIP_COUNT = 3
let skipCount = 0

watch(() => icdStore.ephemerisTrackingState, (newState) => {
  if (newState === 'TRACKING') {
    skipCount = 0  // 리셋
  }
})

// updateChart에서
if (skipCount < INITIAL_SKIP_COUNT) {
  skipCount++
  return  // 초기 몇 개 프레임 스킵
}
```

---

## 영향 범위

| 파일 | 수정 필요 | 영향 |
|------|:---------:|------|
| EphemerisDesignationPage.vue | ✅ | isValidAngle, ChartUpdatePool |
| PassSchedulePage.vue | ✅ | isValidAngle, PassChartUpdatePool |
| ScheduleChart.vue | ⚠️ | ChartUpdatePool (선택적) |
| ephemerisTrackStore.ts | ✅ | (0,0) 처리 로직 조정 |
| passScheduleStore.ts | ⚠️ | 필요시 동기화 |

---

## 테스트 시나리오

1. **정남(0°) 추적 시작**: 안테나가 0도에서 시작할 때 점프 없이 추적
2. **초기값 지연**: ICD 통신 지연 시 차트 초기 표시 확인
3. **추적 재시작**: 추적 중지 → 재시작 시 이전 좌표 잔상 없음
4. **Ephemeris + PassSchedule 모두 확인**

---

## 다음 단계

```
/bugfix #R001-H1  ← isValidAngle 수정 (두 파일)
/bugfix #R001-M1  ← ChartUpdatePool 초기화 개선
```

**권장:** H1, H2를 먼저 수정 (핵심 원인), 이후 M1~M3 수정 (보조)