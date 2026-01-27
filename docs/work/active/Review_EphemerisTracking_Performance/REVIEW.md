# Review: EphemerisDesignationPage 성능 문제 (#R002)

> **Review ID**: #R002
> **대상**: EphemerisDesignationPage.vue Position View 렌더링
> **심각도**: Critical (위성 추적 시간에 비례하여 성능 저하)
> **분석일**: 2026-01-27

---

## 문제 요약

위성 추적 시간이 길어질수록 프론트엔드 성능이 계속 저하되는 현상.

**근본 원인**: `trackingPath.sampledPath` 배열이 **무제한 증가**하며, 100ms마다 전체 배열을 차트에 전달

---

## 발견된 이슈

| Issue ID | 심각도 | 문제 | 위치 | 영향 |
|----------|:------:|------|------|------|
| #R002-C1 | 🔴 Critical | trackingPath 크기 제한 비활성화 | `ephemerisTrackStore.ts:487-489` | 메모리 누수 |
| #R002-C2 | 🔴 Critical | 매 100ms마다 전체 경로 복사 | `EphemerisDesignationPage.vue:1331` | CPU 부하 |
| #R002-H1 | 🟠 High | detailData 매번 map 연산 | `EphemerisDesignationPage.vue:1401-1412` | 불필요한 연산 |
| #R002-M1 | 🟡 Medium | setInterval vs requestAnimationFrame | `EphemerisDesignationPage.vue:2515` | 프레임 드롭 |

---

## 상세 분석

### #R002-C1: trackingPath 크기 제한 비활성화 (Critical)

**위치**: `frontend/src/stores/mode/ephemerisTrackStore.ts:487-489`

```typescript
// 크기 제한
/*      if (currentPath.length > 150) {
  currentPath.splice(0, currentPath.length - 150)
} */
```

**문제**:
- 폴백 함수에서 크기 제한 코드가 **주석 처리됨**
- Worker에 `maxPoints: 150` 설정이 있지만, 폴백 모드에서는 무제한 증가
- 1시간 추적 시: 100ms 간격 × 3600초 = **최대 36,000개 포인트** 누적 가능

**영향**:
- 메모리 사용량 선형 증가
- 배열 복사/전달 시간 O(n) 증가
- GC 압박 증가

---

### #R002-C2: 매 100ms마다 전체 경로 복사 (Critical)

**위치**: `frontend/src/pages/mode/EphemerisDesignationPage.vue:1327-1332`

```typescript
class ChartUpdatePool {
  updateTrackingPath(newPath: [number, number][]) {
    this.trackingData.length = 0
    if (Array.isArray(newPath)) {
      this.trackingData.push(...newPath)  // ⚠️ 전체 배열 복사!
    }
    return this.updateOption
  }
}
```

**호출 경로**:
```
setInterval (100ms)
  → updateChart()
    → chartPool.updateTrackingPath(ephemerisStore.trackingPath.sampledPath)
      → this.trackingData.push(...newPath)  // O(n) 복사
```

**문제**:
- 10,000개 포인트 × 10 updates/sec = **초당 100,000회 배열 요소 push**
- spread operator (`...`)는 새 배열 생성 + 복사

---

### #R002-H1: detailData 매번 map 연산 (High)

**위치**: `frontend/src/pages/mode/EphemerisDesignationPage.vue:1401-1412`

```typescript
// ✅ 위성 궤적 데이터 유지 (series[2]) - 매 100ms마다 실행됨!
if (ephemerisStore.detailData && ephemerisStore.detailData.length > 0) {
  const trajectoryPoints = ephemerisStore.detailData.map((point) => {  // ⚠️
    const az = typeof point.Azimuth === 'number' ? point.Azimuth : 0
    const el = typeof point.Elevation === 'number' ? point.Elevation : 0
    const normalizedAz = az < 0 ? az + 360 : az
    const normalizedEl = Math.max(0, Math.min(90, el))
    return [normalizedEl, normalizedAz] as [number, number]
  })
  // ...
}
```

**문제**:
- `detailData`는 스케줄 선택 시에만 변경됨
- 그러나 매 100ms마다 전체 데이터를 map 연산
- 불필요한 CPU 사용

---

### #R002-M1: setInterval vs requestAnimationFrame (Medium)

**위치**: `frontend/src/pages/mode/EphemerisDesignationPage.vue:2515`

```typescript
updateTimer = window.setInterval(() => {
  void updateChart()
  updateTimeRemaining()
}, 100)
```

**문제**:
- `setInterval`은 브라우저 렌더링 주기와 동기화되지 않음
- 탭이 비활성화되어도 계속 실행
- 프레임 드롭 발생 가능

---

## 개선안

### 즉시 적용 (Critical)

#### 1. trackingPath 크기 제한 복원

```typescript
// ephemerisTrackStore.ts - fallbackUpdatePath 함수
currentPath.push(currentPoint)

// ✅ 크기 제한 복원 (주석 해제)
if (currentPath.length > 150) {
  currentPath.splice(0, currentPath.length - 150)
}
```

#### 2. 증분 업데이트 방식 도입

```typescript
class ChartUpdatePool {
  private lastTrackingLength = 0

  updateTrackingPath(newPath: [number, number][]) {
    // ✅ 변경 없으면 스킵
    if (newPath.length === this.lastTrackingLength) {
      return this.updateOption
    }

    // ✅ 새로운 포인트만 추가 (증분 업데이트)
    if (newPath.length > this.lastTrackingLength) {
      const newPoints = newPath.slice(this.lastTrackingLength)
      this.trackingData.push(...newPoints)
    } else {
      // 배열이 리셋된 경우 (추적 시작)
      this.trackingData.length = 0
      this.trackingData.push(...newPath)
    }

    this.lastTrackingLength = newPath.length
    return this.updateOption
  }
}
```

### 권장 적용 (High)

#### 3. detailData 캐싱

```typescript
// 캐시 변수
let cachedTrajectoryPoints: [number, number][] | null = null
let lastDetailDataLength = 0

const updateChart = () => {
  // ...

  // ✅ detailData 캐싱 (변경 시에만 재계산)
  if (ephemerisStore.detailData.length !== lastDetailDataLength) {
    cachedTrajectoryPoints = ephemerisStore.detailData.map((point) => {
      // ... 변환 로직
    })
    lastDetailDataLength = ephemerisStore.detailData.length
  }

  if (cachedTrajectoryPoints) {
    option.series[2].data = cachedTrajectoryPoints
  }
}
```

### 선택 적용 (Medium)

#### 4. requestAnimationFrame 사용

```typescript
let animationFrameId: number | null = null
let lastUpdateTime = 0
const UPDATE_INTERVAL = 100 // ms

const updateLoop = (currentTime: number) => {
  if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
    updateChart()
    lastUpdateTime = currentTime
  }
  animationFrameId = requestAnimationFrame(updateLoop)
}

// 시작
animationFrameId = requestAnimationFrame(updateLoop)

// 정리
if (animationFrameId) {
  cancelAnimationFrame(animationFrameId)
}
```

---

## 예상 효과

| 개선안 | 메모리 | CPU | 난이도 |
|--------|:------:|:---:|:------:|
| 크기 제한 복원 | ↓90% | ↓50% | 쉬움 |
| 증분 업데이트 | - | ↓80% | 중간 |
| detailData 캐싱 | - | ↓30% | 쉬움 |
| requestAnimationFrame | - | ↓10% | 쉬움 |

---

## 권장 조치

**즉시 수행**: `/optimize #R002-C1` - 크기 제한 복원 (1줄 주석 해제)

**후속 작업**: `/optimize #R002-C2` - 증분 업데이트 구현

진행할까요?
- `/optimize #R002-C1` - Critical 즉시 수정
- `/optimize origin:#R002` - 전체 이슈 순차 수정
