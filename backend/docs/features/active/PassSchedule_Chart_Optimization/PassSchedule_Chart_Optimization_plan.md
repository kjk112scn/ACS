# PassSchedule 차트 최적화 리팩토링 계획

---
**작성일**: 2025-11-12
**작성자**: GTL Systems
**상태**: 진행 중
**위치**: `docs/features/active/PassSchedule_Chart_Optimization/PassSchedule_Chart_Optimization_plan.md`
**관련 기능**: [PassSchedule_Keyhole_Display_Enhancement](../PassSchedule_Keyhole_Display_Enhancement/PassSchedule_Keyhole_Display_Enhancement_plan.md)
---

## 목표

PassSchedulePage의 Position View 차트를 최적화하여 성능을 개선하고, keyhole 여부에 따라 올바른 경로를 표시하며, 스케줄 전환 시 메모리 누적 문제를 해결합니다.

## 현재 차트 구현 분석

### 차트 구조
- **3개 시리즈**: 현재 위치(scatter), 실시간 추적 경로(line), 예정 위성 궤적(line)
  - **위치 선 제거**: 현재 위치 점이 이동하면서 실시간 경로를 그리므로 원점에서 현재 위치까지의 선은 불필요
- **업데이트 주기**: 100ms 타이머, 스로틀링 200ms (실제 업데이트는 200ms마다)
- **업데이트 방식**: `setOption`으로 전체 경로 다시 그리기
- **데이터 관리**: `PassChartUpdatePool` 클래스로 데이터 풀링

### 백엔드 데이터 구조 확인

**백엔드 API**: `GET /pass-schedule/tracking/detail/{satelliteId}/pass/{passId}`

- 백엔드는 **하나의 스케줄에 대해 하나의 경로 데이터만 반환**함
- `getSelectedTrackDtlByMstId` 함수가 keyhole 여부에 따라 적절한 DataType의 DTL을 반환
  - Keyhole 위성: `keyhole_final_transformed` DataType
  - 일반 위성: `final_transformed` DataType
- 응답 구조:
  ```typescript
  {
    success: boolean
    data: {
      satelliteId: string
      passId: number
      trackingPointCount: number
      trackingPoints: TrackingDetailItem[]  // Azimuth, Elevation 필드 포함
    }
  }
  ```
- **결론**: 백엔드는 여러 경로를 반환하지 않음. 여러 경로가 표시되는 문제는 프론트엔드 시리즈 인덱스 불일치 문제임

### 현재 문제점

1. **시리즈 인덱스 불일치 및 색상 반전 문제 (우선순위: 높음)**
   - **차트 초기화 시 시리즈 구조** (위치 선 제거, 3개 시리즈로 단순화):
     - series[0]: 실시간 추적 위치 (scatter) - 빨간색 점
     - series[1]: 실시간 추적 경로 (line) - **흰색** (#ffffff)
     - series[2]: 예정 위성 궤적 (line) - **파란색** (#2196f3)
     - **위치 선(series[1]) 제거**: 현재 위치 점이 이동하면서 실시간 경로를 그리므로 원점에서 현재 위치까지의 선은 불필요
   
   - **PassChartUpdatePool 구조** (현재 잘못됨):
     - series[0]: positionData (현재 위치) ✅
     - series[1]: trackingData (실시간 추적 경로) ❌ → series[1]은 맞지만 색상이 잘못됨
     - series[2]: predictedData (예정 위성 궤적) ❌ → series[2]는 맞지만 색상이 잘못됨
     - **문제**: PassChartUpdatePool이 3개만 관리하지만, 차트는 4개 시리즈로 초기화되어 인덱스 불일치 발생
   
   - **문제점**:
     - 차트 초기화 시 4개 시리즈(위치 선 포함)로 정의되어 있음
     - PassChartUpdatePool은 3개만 관리하여 인덱스 불일치
     - trackingData가 series[1]에 설정되어 흰색으로 표시되어야 하는데, 차트 초기화 시 series[2]가 흰색으로 정의됨
     - predictedData가 series[2]에 설정되어 파란색으로 표시되어야 하는데, 차트 초기화 시 series[3]가 파란색으로 정의됨
     - **색상이 반대로 표시됨**: 예정 경로가 흰색으로, 실시간 경로가 파란색으로 보임

2. **여러 경로가 동시에 그려지는 문제**
   - 이전 경로가 제대로 초기화되지 않아 누적 표시
   - 스케줄 변경 시 경로 일치 확인 로직 부족

3. **비효율적인 차트 업데이트 및 타이머 불일치**
   - 매번 `setOption`으로 전체 경로를 다시 그리므로 성능 저하
   - ECharts의 `appendData` API 미사용으로 증분 업데이트 불가
   - **타이머 불일치**: 백엔드는 100ms 주기로 데이터 전송하지만, 프론트엔드는 200ms 주기로 업데이트하여 데이터 누적 문제 발생

4. **메모리 누적 문제 재정의**
   - **정상 동작**: 스케줄 추적 중에는 실시간 추적 경로(`actualTrackingPath`)가 계속 누적되어야 함 (요구사항)
   - **문제점**: 스케줄이 완료되어 다음 스케줄로 넘어갈 때 이전 스케줄의 경로가 초기화되지 않아 여러 스케줄의 경로가 누적됨
   - **해결 방안**: `currentTrackingMstId` 변경 감지하여 스케줄 전환 시 이전 스케줄의 `actualTrackingPath`만 초기화
   - **메모리 보호**: 50,000개 포인트 초과 시 `cleanupOldPoints`로 오래된 포인트 정리 (이미 구현됨, 유지)

5. **경로 데이터 변환 및 시간 정렬 문제**
   - `convertToChartData`가 단순히 `Azimuth`, `Elevation`만 사용하고 시간 정렬 없음
   - 백엔드에서 ±270° 범위로 제한된 값을 전달하지만, 프론트엔드에서 단순히 +360을 하여 경로가 왜곡됨
   - 시간 순서 정렬 누락으로 경로가 왔다갔다 하는 문제 발생
   - 백엔드가 이미 회전 방향성을 보장했으므로, 프론트엔드에서 연속성을 유지하면서 0~360°로 변환 필요

---

## 구현 계획

### Phase 1: 시리즈 인덱스 매핑 및 색상 수정 (우선순위: 높음)

**파일**: `ACS/src/pages/mode/PassSchedulePage.vue` (라인 1185-1372: initChart, 라인 384-435: PassChartUpdatePool)

1. **차트 초기화 수정** (initChart 함수)
   - 위치 선(series[1]) 제거하여 3개 시리즈로 단순화
   - 시리즈 구조:
     - series[0]: 실시간 추적 위치 (scatter) - 빨간색 점
     - series[1]: 실시간 추적 경로 (line) - 흰색 (#ffffff)
     - series[2]: 예정 위성 궤적 (line) - 파란색 (#2196f3)

2. **PassChartUpdatePool 구조 수정**
   - 3개 시리즈로 유지 (차트 구조와 일치)
   - 시리즈 인덱스 정확히 매핑:
     - series[0]: positionData (현재 위치)
     - series[1]: trackingData (실시간 추적 경로) - 흰색
     - series[2]: predictedData (예정 위성 궤적) - 파란색

3. **updateTrackingPath 메서드 수정**
   - series[1]에 데이터 설정 (올바른 인덱스)

4. **updatePredictedPath 메서드 수정**
   - series[2]에 데이터 설정 (올바른 인덱스)

### Phase 2: 경로 일치 확인 로직 추가 및 Index/No 혼용 문제 해결

**파일**: `ACS/src/pages/mode/PassSchedulePage.vue` (라인 1478-1495, 1625-1645)

#### Index와 No의 관계 분석

**백엔드 구조**:
- `No`: MST의 고유 ID (Primary Key)
- `currentTrackingMstId`/`nextTrackingMstId`: 백엔드에서 `No` 값을 사용

**프론트엔드 구조**:
- `SelectScheduleContent.vue`에서:
  - `index = item.no` (원본 MST의 `No` 보존) ✅
  - `no = sortedIndex + 1` (정렬된 순서로 재생성) ❌

**문제점**:
1. `loadTrackingDetailData`에서 `passId = scheduleToLoad.index || scheduleToLoad.no` 사용
   - `no`는 재생성된 값이라 MST ID가 아님
2. 경로 일치 확인 로직에서 `schedulePassId = currentSchedule.index || currentSchedule.no` 사용
   - `no`는 재생성된 값이라 잘못된 매칭 가능
3. `currentTrackingMstId`는 백엔드에서 `No`를 사용하므로, 프론트엔드에서는 `index`만 사용해야 함

#### 수정 사항

1. **loadSelectedScheduleTrackingPath 함수 수정** (라인 1478-1495)
   - `passId = scheduleToLoad.index || scheduleToLoad.no` → `passId = scheduleToLoad.index`로 변경
   - `no`는 재생성된 값이므로 사용하지 않음

2. **updateChartWithPerformanceMonitoring 함수 수정** (라인 1625-1645)
   - 경로 일치 확인 로직 추가
   - 현재 스케줄의 satelliteId와 passId를 확인하여 일치하는 경로만 표시
   - `schedulePassId = currentSchedule.index || currentSchedule.no` → `schedulePassId = currentSchedule.index`로 변경
   - 일치하지 않으면 빈 배열로 초기화

3. **차트 업데이트 시 이전 경로 명시적 초기화**
   - `setOption` 호출 시 모든 시리즈를 명시적으로 초기화하여 이전 경로 제거

### Phase 3: 프론트엔드 데이터 변환 개선 (우선순위: 높음)

**파일**: `ACS/src/services/mode/passScheduleService.ts`

1. **convertToChartData 함수 개선**
   - ✅ 시간 순서 정렬 추가: `Time` 필드 기준으로 정렬하여 경로 순서 보장
   - ✅ 연속성 유지 변환: 백엔드가 이미 ±270° 범위로 제한했으므로, 이전 값과의 차이(delta)를 계산하여 연속성을 유지하면서 0~360°로 변환
   - ✅ 첫 번째 포인트: 음수면 +360, 양수면 그대로
   - ✅ 이후 포인트: 이전 변환된 값에 delta를 더하여 연속성 유지
   - ✅ 샘플링 시 마지막 포인트 항상 포함

### Phase 4: 차트 업데이트 최적화

**파일**: `ACS/src/pages/mode/PassSchedulePage.vue`

1. **ECharts appendData API 활용**
   - 실시간 추적 경로는 `appendData`로 증분 업데이트
   - 예정 위성 궤적은 스케줄 변경 시에만 전체 업데이트
   - 현재 위치는 기존 방식 유지 (단일 포인트)

2. **타이머 및 스로틀링 일치 (우선순위: 높음)**
   - ✅ 타이머를 100ms로 변경하여 백엔드 모니터링 주기(100ms)와 일치
   - ✅ `updateThrottle`을 100ms로 변경
   - 백엔드가 100ms마다 데이터를 전송하므로 프론트엔드도 100ms마다 업데이트하여 데이터 누적 방지

3. **차트 업데이트 진행도 표시**
   - 차트 업데이트 중 로딩 인디케이터 표시
   - 업데이트 성능 통계 표시 (선택사항)

4. **메모리 관리 개선**
   - **스케줄 전환 시 경로 초기화**: `currentTrackingMstId` 변경 감지하여 이전 스케줄의 `actualTrackingPath` 초기화
   - **스케줄 추적 중 경로 보존**: 스케줄 추적 중에는 모든 경로를 보존 (요구사항)
   - **메모리 보호**: 50,000개 포인트 초과 시 오래된 포인트 정리 (이미 구현됨, 유지)
   - **차트 업데이트 최적화**: 스케줄 전환 시에만 경로 초기화, 추적 중에는 계속 누적

### Phase 5: 경로 데이터 로딩 최적화

**파일**: `ACS/src/stores/mode/passScheduleStore.ts`

1. **loadTrackingDetailData 개선**
   - 스케줄 정보에서 keyhole 여부 확인
   - 경로 정보에 keyhole 여부 및 스케줄 정보 저장
   - 경로 일치 확인 로직 추가

2. **경로 데이터 캐싱**
   - 동일한 스케줄의 경로는 캐시하여 재사용
   - 스케줄 변경 시에만 새로 로드

### Phase 6: 차트 업데이트 로직 개선

**파일**: `ACS/src/pages/mode/PassSchedulePage.vue`

1. **updateChartWithPerformanceMonitoring 개선**
   - 경로 선택 시 keyhole 여부 확인
   - 이전 경로 명시적 초기화
   - `PassChartUpdatePool.updatePredictedPath`에서 이전 데이터 완전 제거 후 새 데이터 추가

2. **PassChartUpdatePool 구조 수정**
   - 차트 시리즈 구조와 일치하도록 3개 시리즈 관리
   - 위치 선 제거로 인덱스 정확히 매핑

3. **스케줄 전환 시 경로 초기화**
   - `currentTrackingMstId` 변경 감지하여 스케줄 전환 시 이전 스케줄의 경로만 초기화

---

## 구현 세부사항

### Phase 1: 차트 초기화 및 PassChartUpdatePool 구조 수정 (시리즈 인덱스 매핑 및 색상 수정)

#### 차트 초기화 수정 (initChart 함수)

```typescript
// initChart 함수 내부 - 시리즈 정의 부분
series: [
  {
    name: '실시간 추적 위치',
    type: 'scatter',
    coordinateSystem: 'polar',
    symbol: 'circle',
    symbolSize: 15,
    itemStyle: {
      color: '#ff5722', // 빨간색
    },
    data: [[0, 0]],
    zlevel: 3,
  },
  // ✅ 위치 선 제거 - 불필요한 시리즈
  {
    name: '실시간 추적 경로',
    type: 'line',
    coordinateSystem: 'polar',
    symbol: 'none',
    lineStyle: {
      color: '#ffffff', // 흰색
      width: 2,
      opacity: 0.8,
    },
    data: [],
    zlevel: 2,
  },
  {
    name: '예정 위성 궤적',
    type: 'line',
    coordinateSystem: 'polar',
    symbol: 'none',
    lineStyle: {
      color: '#2196f3', // 파란색
      width: 2,
    },
    data: [],
    zlevel: 1,
  },
]
```

#### convertToChartData 함수 개선 (시간 정렬 및 연속성 유지)

```typescript
convertToChartData(trackingPoints: TrackingDetailItem[]): [number, number][] {
  try {
    if (!Array.isArray(trackingPoints) || trackingPoints.length === 0) {
      console.warn('⚠️ 변환할 추적 포인트가 없음')
      return []
    }

    // ✅ 1. 시간 순서대로 정렬 (Time 필드 기준)
    const sortedPoints = [...trackingPoints].sort((a, b) => {
      const timeA = new Date(a.Time || 0).getTime()
      const timeB = new Date(b.Time || 0).getTime()
      return timeA - timeB
    })

    // ✅ 2. 백엔드가 이미 ±270° 범위로 제한했으므로, 연속성을 유지하면서 0~360°로 변환
    let previousAzimuth: number | null = null
    const chartData: [number, number][] = sortedPoints
      .filter((point) => {
        // 유효한 데이터만 필터링
        return (
          point.Azimuth !== null &&
          point.Azimuth !== undefined &&
          point.Elevation !== null &&
          point.Elevation !== undefined &&
          !isNaN(Number(point.Azimuth)) &&
          !isNaN(Number(point.Elevation))
        )
      })
      .map((point) => {
        const elevation = Math.max(0, Math.min(90, Number(point.Elevation)))
        let azimuth = Number(point.Azimuth) // 백엔드에서 이미 ±270° 범위로 제한됨
        
        // ✅ 백엔드가 이미 회전 방향성을 보장했으므로, 연속성을 유지하면서 0~360°로 변환
        if (previousAzimuth !== null) {
          // 이전 값과의 차이 계산 (360°/0° 경계 고려)
          let delta = azimuth - previousAzimuth
          
          // 180도 이상 차이나면 반대 방향으로 보정
          if (delta > 180) {
            delta -= 360
          } else if (delta < -180) {
            delta += 360
          }
          
          // 이전 변환된 값에 delta를 더함 (연속성 유지)
          const previousConverted = chartData[chartData.length - 1][1]
          azimuth = previousConverted + delta
          
          // 0~360° 범위로 정규화
          while (azimuth < 0) azimuth += 360
          while (azimuth >= 360) azimuth -= 360
        } else {
          // 첫 번째 포인트: 음수면 +360, 양수면 그대로
          if (azimuth < 0) {
            azimuth = azimuth + 360
          }
        }
        
        previousAzimuth = Number(point.Azimuth) // 원본 값 저장 (다음 포인트와 비교용)
        
        return [elevation, azimuth] as [number, number]
      })

    console.log(`✅ 차트 데이터 변환 완료: ${chartData.length}개 포인트 (시간 순 정렬, 연속성 유지)`)

    // ✅ 3. 샘플링 (정렬된 데이터에서 샘플링)
    if (chartData.length > 200) {
      const step = Math.ceil(chartData.length / 200)
      const sampledData = chartData.filter((_, index) => index % step === 0)
      // ✅ 마지막 포인트는 항상 포함
      if (sampledData.length > 0 && 
          sampledData[sampledData.length - 1] !== chartData[chartData.length - 1]) {
        sampledData.push(chartData[chartData.length - 1])
      }
      console.log(`📊 데이터 샘플링: ${chartData.length} → ${sampledData.length}개 포인트`)
      return sampledData
    }

    return chartData
  } catch (error) {
    console.error('❌ 차트 데이터 변환 실패:', error)
    return []
  }
}
```

#### 타이머 및 스로틀링 일치

```typescript
// ✅ 타이머를 100ms로 변경하여 백엔드 모니터링 주기(100ms)와 일치
const updateThrottle = 100 // 200ms → 100ms로 변경

// 타이머도 100ms로 변경
updateTimer = window.setInterval(() => {
  try {
    updateChart()
  } catch (timerError) {
    console.error('차트 업데이트 타이머 오류:', timerError)
  }
}, 100) // 200 → 100으로 변경
```

### Phase 2: 경로 일치 확인 로직 추가 및 Index/No 혼용 문제 해결

#### loadSelectedScheduleTrackingPath 함수 수정

```typescript
// loadSelectedScheduleTrackingPath 함수 내부 (라인 1478-1495)
const satelliteId = scheduleToLoad.satelliteId || scheduleToLoad.satelliteName
// ✅ index만 사용 (no는 재생성된 값이므로 사용하지 않음)
const passId = scheduleToLoad.index

if (!satelliteId || !passId) {
  console.log('⚠️ 위성 ID 또는 패스 ID가 없음')
  return
}
```

#### updateChartWithPerformanceMonitoring 함수 수정

```typescript
// updateChartWithPerformanceMonitoring 함수 내부 (라인 1625-1645)
let predictedPathToShow: [number, number][] = []

if (shouldShowPredictedPath) {
  if (currentTrackingMstId !== null) {
    const currentSchedule = sortedScheduleList.value.find(s => Number(s.index) === Number(currentTrackingMstId))
    if (currentSchedule) {
      // ✅ 현재 스케줄의 satelliteId와 passId 확인하여 일치하는 경로만 사용
      const currentPath = passScheduleStore.predictedTrackingPath
      const pathInfo = passScheduleStore.currentTrackingPathInfo
      const scheduleSatelliteId = currentSchedule.satelliteId || currentSchedule.satelliteName
      // ✅ index만 사용 (no는 재생성된 값이므로 사용하지 않음)
      const schedulePassId = currentSchedule.index
      
      if (currentPath && currentPath.length > 0 && 
          pathInfo.satelliteId === scheduleSatelliteId &&
          pathInfo.passId === schedulePassId) {
        predictedPathToShow = currentPath.map((point: readonly [number, number]) => [...point])
      } else {
        // ✅ 경로가 일치하지 않으면 빈 배열로 초기화
        predictedPathToShow = []
      }
    }
  } else if (selectedSchedule) {
    // ✅ 선택된 스케줄의 satelliteId와 passId 확인하여 일치하는 경로만 사용
    const selectedPath = passScheduleStore.predictedTrackingPath
    const pathInfo = passScheduleStore.currentTrackingPathInfo
    const scheduleSatelliteId = selectedSchedule.satelliteId || selectedSchedule.satelliteName
    // ✅ index만 사용 (no는 재생성된 값이므로 사용하지 않음)
    const schedulePassId = selectedSchedule.index
    
    if (selectedPath && selectedPath.length > 0 &&
        pathInfo.satelliteId === scheduleSatelliteId &&
        pathInfo.passId === schedulePassId) {
      predictedPathToShow = selectedPath.map((point: readonly [number, number]) => [...point])
    } else {
      // ✅ 경로가 일치하지 않으면 빈 배열로 초기화
      predictedPathToShow = []
    }
  }
}
```

### ECharts appendData 활용 (Phase 4)

```typescript
// 실시간 추적 경로 증분 업데이트
if (newTrackingPoints.length > 0) {
  passChart.appendData({
    seriesIndex: 1, // 실시간 추적 경로 시리즈 (위치 선 제거로 인덱스 변경)
    data: newTrackingPoints.map(p => [p[0], p[1]])
  })
}

// 예정 위성 궤적 전체 업데이트 (스케줄 변경 시)
passChart.setOption({
  series: [{
    data: [] // series[0] 유지
  }, {
    data: [] // series[1] 유지
  }, {
    data: predictedPath // series[2]만 업데이트
  }]
}, false, true) // notMerge: false, lazyUpdate: true
```

### 스케줄 전환 시 경로 초기화 로직

```typescript
// currentTrackingMstId 변경 감지하여 스케줄 전환 시 경로 초기화
watch(() => icdStore.currentTrackingMstId, (newMstId, oldMstId) => {
  // 스케줄이 변경된 경우 (이전 스케줄 완료, 다음 스케줄 시작)
  if (oldMstId !== null && newMstId !== null && oldMstId !== newMstId) {
    console.log(`🔄 스케줄 전환 감지: ${oldMstId} → ${newMstId}`)
    // 이전 스케줄의 실시간 추적 경로만 초기화 (predictedTrackingPath는 새 스케줄 로드 시 자동 교체)
    passScheduleStore.actualTrackingPath = []
    console.log('✅ 이전 스케줄의 실시간 추적 경로 초기화 완료')
  } else if (oldMstId === null && newMstId !== null) {
    // 추적 시작 시 빈 경로에서 시작
    console.log('🚀 추적 시작 - 실시간 추적 경로 초기화 (빈 경로에서 시작)')
    passScheduleStore.actualTrackingPath = []
  } else if (oldMstId !== null && newMstId === null) {
    // 추적 완료 시 경로 초기화
    console.log('🛑 추적 완료 - 실시간 추적 경로 초기화')
    passScheduleStore.actualTrackingPath = []
  }
}, { immediate: true })
```

### 차트 경로 표시 우선순위

1. 현재 추적 중인 스케줄 (`icdStore.currentTrackingMstId`)
2. 선택된 스케줄 (`displaySchedule.value`)
3. 둘 다 없으면 경로 표시 안 함

**핵심 원칙**:
1. **스케줄 추적 중**: 경로 계속 누적 (요구사항 - 하나의 스케줄이 이동한 경로는 다른 스케줄로 넘어가기 전까지 모두 표시)
2. **스케줄 전환 시**: 이전 스케줄의 경로만 초기화
3. **메모리 보호**: 50,000개 포인트 초과 시 자동 정리 (유지)

---

## 예상 변경 파일

- `ACS/src/pages/mode/PassSchedulePage.vue` (주요 수정 파일)
- `ACS/src/stores/mode/passScheduleStore.ts` (경로 데이터 로딩 최적화)
- `ACS/src/services/mode/passScheduleService.ts` (데이터 변환 개선)

## 테스트 시나리오

### Phase 1 테스트 (시리즈 매핑 및 색상 수정)
1. 차트 초기화 시 3개 시리즈만 정의되는지 확인 (위치 선 제거)
2. 1개 스케줄 등록 시 차트에 1개의 예정 경로만 표시되는지 확인 (파란색)
3. 추적 시작 시 실시간 추적 경로가 흰색으로 표시되는지 확인
4. 예정 경로는 파란색, 실시간 추적 경로는 흰색으로 올바르게 표시되는지 확인
5. 현재 위치 점이 올바르게 표시되는지 확인 (빨간색 점)

### Phase 2 테스트 (경로 일치 확인 및 Index/No 혼용 문제 해결)
5. Index와 No의 관계 확인: Index는 원본 MST ID, No는 정렬된 순서임을 확인
6. `loadTrackingDetailData`에서 `index`만 사용하여 올바른 경로를 로드하는지 확인
7. 경로 일치 확인 로직에서 `index`만 사용하여 올바른 경로만 표시되는지 확인
8. 스케줄 변경 시 이전 경로가 제거되고 새 경로만 표시되는지 확인
9. 차트에 하나의 경로만 표시되는지 확인 (중복 경로 없음)

### Phase 3 테스트 (데이터 변환 및 타이머 개선)
10. **시간 순서 정렬**: 경로가 시간 순서대로 그려지는지 확인
11. **연속성 유지**: 백엔드 ±270° 범위 값이 연속성을 유지하면서 0~360°로 변환되는지 확인
12. **경로 왜곡 해결**: 경로가 왔다갔다 하지 않고 한 선으로 그려지는지 확인
13. **타이머 일치**: 백엔드 100ms 주기와 프론트엔드 100ms 업데이트가 일치하는지 확인
14. **데이터 누적 방지**: 100ms마다 업데이트하여 데이터가 누적되지 않는지 확인

### Phase 4-6 테스트 (기존 계획)
15. keyhole 위성 스케줄 선택 시 keyhole 경로 표시 확인
16. 일반 위성 스케줄 선택 시 3축 최종 변환 경로 표시 확인
17. **스케줄 추적 중 실시간 경로가 계속 누적되어 표시되는지 확인** (요구사항)
18. **스케줄 전환 시 이전 스케줄의 경로가 초기화되고 새 스케줄의 경로만 표시되는지 확인**
19. 실시간 추적 경로가 증분 업데이트되는지 확인 (appendData 활용)
20. **메모리 보호**: 50,000개 포인트 초과 시 오래된 포인트가 자동으로 정리되는지 확인
21. 차트 업데이트 성능 개선 확인 (appendData vs setOption 비교)

---

## 참고

- **백엔드 데이터 구조**: 백엔드는 하나의 스케줄에 대해 하나의 경로 데이터만 반환함. 여러 경로가 표시되는 문제는 프론트엔드 시리즈 인덱스 불일치 문제임
- **Index와 No의 관계**: 
  - `Index`: 원본 MST의 `No` (백엔드 MST ID) - `currentTrackingMstId`와 매칭에 사용
  - `No`: 정렬된 순서 (1, 2, 3, ...) - MST ID가 아님, 경로 로드나 매칭에 사용하면 안 됨
  - `SelectScheduleContent.vue`에서 `index = item.no`로 원본 보존, `no = sortedIndex + 1`로 재생성
  - 경로 로드 및 일치 확인 시 `index`만 사용해야 함 (`no`는 사용하지 않음)
- **시리즈 구조 단순화**: 위치 선(원점에서 현재 위치까지의 선)은 불필요하므로 제거하여 3개 시리즈로 단순화
  - 현재 위치 점이 이동하면서 실시간 경로를 그리므로 위치 선은 중복됨
- **시리즈 인덱스 매핑**: 차트 초기화와 PassChartUpdatePool 모두 3개 시리즈로 일치시켜야 함
- **색상 반전 원인**: trackingData가 series[1]에, predictedData가 series[2]에 설정되어 색상이 반대로 표시됨. 올바른 인덱스(series[2], series[3])에 설정해야 함
- **메모리 관리**: 스케줄 추적 중에는 경로를 계속 누적하되, 스케줄 전환 시에만 이전 경로 초기화
- **자동 스케줄 전환**: 백엔드 상태 머신이 자동으로 처리하므로 프론트엔드에서는 `currentTrackingMstId` 변경 감지만 필요
- **백엔드 데이터**: 백엔드가 이미 keyhole 여부에 따라 적절한 DataType의 데이터를 반환하므로 추가 변환 불필요
- **백엔드 ±270° 제한**: 백엔드 `LimitAngleCalculator`가 0~360° 범위를 ±270° 범위로 제한하여 포지셔너 물리적 제한 준수
  - 회전 방향성을 보장하면서 ±270° 범위로 변환
  - 프론트엔드 ECharts는 0~360° 범위만 지원하므로 연속성을 유지하면서 변환 필요
- **타이머 일치**: 백엔드 모니터링 주기(100ms)와 프론트엔드 업데이트 주기(100ms)를 일치시켜 데이터 누적 방지
- **시간 정렬**: 백엔드에서 시간 순서 정렬 없이 전달할 수 있으므로 프론트엔드에서 `Time` 필드 기준 정렬 필요
- **차트 성능**: ECharts의 `appendData` API 활용으로 증분 업데이트 구현

