# Review: Dashboard 시간 표시 끊김 현상 (#R001)

> **분석일**: 2026-01-27
> **대상**: DashboardPage.vue 상단 UTC/Local 시간 표시
> **증상**: 위성 추적 중 5~10초 간격으로 시간 표시 정지

---

## 분석 요약

| 구분 | 결론 |
|------|------|
| **근본 원인** | FE 측 타이머 충돌 + 업데이트 스킵 로직 |
| **BE 문제** | 없음 (스레드 분리, serverTime 매번 새로 계산) |
| **심각도** | Medium (기능적 문제 없음, UX 불편) |

---

## 발견된 이슈

| Issue ID | 심각도 | 문제 | 위치 | 권장 조치 |
|----------|:------:|------|------|----------|
| #R001-H1 | 🟠 High | 업데이트 스킵 로직에서 serverTime도 함께 스킵 | icdStore.ts:1587 | `/bugfix` |
| #R001-H2 | 🟠 High | 이중 30ms 타이머 경쟁 조건 | DashboardPage + icdStore | `/refactor` |
| #R001-M1 | 🟡 Medium | GC 압박 (매 30ms 새 객체 생성) | DashboardPage.vue:591-655 | `/optimize` |
| #R001-M2 | 🟡 Medium | 과도한 ref 업데이트 (50+) | icdStore.ts:1839-1987 | `/optimize` |
| #R001-L1 | 🟢 Low | performanceHistory 배열 조작 비효율 | icdStore.ts:1581-1583 | `/optimize` |

---

## 상세 분석

### 1. 주원인: 업데이트 스킵 로직 (#R001-H1)

```typescript
// icdStore.ts Line 1587
if (currentInterval < UPDATE_INTERVAL * 0.5) {  // 15ms 미만이면
  console.warn(`⚠️ 너무 빠른 업데이트 건너뛰기: ${currentInterval.toFixed(2)}ms`)
  return  // ❌ serverTime 업데이트도 스킵됨!
}
```

**문제**: 빠른 업데이트(15ms 미만)가 감지되면 **전체 업데이트가 스킵**되면서 serverTime도 함께 건너뛰어짐.

**영향**: 타이머 드리프트나 브라우저 스로틀링 발생 시 연속적으로 스킵되어 5-10초 동안 시간 정지.

---

### 2. 이중 30ms 타이머 경쟁 (#R001-H2)

| 위치 | 타이머 | 역할 |
|------|--------|------|
| DashboardPage.vue:684 | `setInterval(updateCharts, 30)` | 차트 3개 업데이트 |
| icdStore.ts:2273 | `preciseSetInterval(updateUIFromBuffer, 30)` | WebSocket 데이터 처리 |

**문제**: 두 타이머가 동기화 없이 독립 실행. 동시에 실행될 때:
1. icdStore가 50+ ref 변수 업데이트
2. Vue 반응성 트리거로 20+ computed 재계산
3. DashboardPage 차트 업데이트 (setOption 3회)
4. 30ms 내 완료 못하면 프레임 드롭

---

### 3. GC 압박 (#R001-M1)

```typescript
// DashboardPage.vue - 매 30ms마다 새 객체 생성
azimuthChart?.setOption({
  series: [{ data: [[1, normalizedAzimuth]] }]  // 새 객체
}, { animation: false, silent: true })
```

**계산**:
- 초당 33회 × 3개 차트 = 99개 옵션 객체/초
- 5-10초마다 Major GC 발생 → 일시적 freeze

---

### 4. 과도한 반응성 트리거 (#R001-M2)

```typescript
// icdStore.ts updataAntennaData - 30ms마다 50+ ref 업데이트
if (antennaData.azimuthAngle !== undefined) azimuthAngle.value = ...
if (antennaData.elevationAngle !== undefined) elevationAngle.value = ...
// ... 50개 이상
```

---

## 데이터 흐름

```
[Backend 30ms]                    [Frontend 30ms × 2]
     |                                 |
generateRealtimeData()           icdStore.preciseSetInterval()
     |                                 |
serverTime = ZonedDateTime.now()      |
     |                                 |
broadcastToAllSubscribers()      updateUIFromBuffer()
     |                                 |
     +------- WebSocket -------->      |
                                      |
                             if (currentInterval < 15ms) return  // ❌ 스킵!
                                      |
                             serverTime.value = ...
                                      |
                             updataAntennaData() [50+ ref]
                                      |
                             Vue 반응성 트리거
                                      |
                             MainLayout.vue displayUTCTime computed 재계산
                                      |
                                 DashboardPage.updateCharts() [별도 30ms 타이머]
                                      |
                                 ECharts.setOption() × 3
                                      |
                                 === 30ms 내 완료 필요 ===
```

---

## 권장 수정 순서

### 즉시 (이번 주) - #R001-H1 수정

**serverTime 업데이트를 스킵 로직 밖으로 분리**

```typescript
// icdStore.ts updateUIFromBuffer 함수 수정
const updateUIFromBuffer = () => {
  // ✅ serverTime은 항상 업데이트 (스킵 로직 전에)
  if (message?.data?.serverTime) {
    serverTime.value = safeToString(message.data.serverTime)
  }

  // 기존 스킵 로직
  if (currentInterval < UPDATE_INTERVAL * 0.5) {
    return  // 다른 데이터만 스킵
  }

  // 나머지 업데이트...
}
```

### 단기 (이번 달) - #R001-H2, M1, M2

1. **타이머 통합**: DashboardPage의 차트 업데이트를 icdStore의 단일 타이머로 통합
2. **ECharts 객체 재사용**: 미리 생성된 옵션 객체 재사용
3. **배치 업데이트**: reactive 객체로 묶어서 1회 업데이트

### 장기 (분기)

- requestAnimationFrame 기반 렌더링
- Web Worker로 데이터 처리 분리

---

## 예상 효과

| 개선 | 효과 |
|------|------|
| serverTime 분리 | 끊김 현상 **완전 해결** |
| 타이머 통합 | 프레임 충돌 제거 |
| 객체 재사용 | GC 압박 50% 감소 |
| 배치 업데이트 | Vue 트리거 95% 감소 |

---

## 연계 작업

```
/bugfix #R001-H1  → serverTime 스킵 로직 수정 (즉시)
/refactor #R001-H2 → 타이머 통합 (단기)
/optimize #R001-M1,M2 → 성능 최적화 (단기)
```

---

## 참조 파일

| 파일 | 분석 지점 |
|------|----------|
| MainLayout.vue:109-170 | 시간 표시 computed |
| icdStore.ts:1587 | 업데이트 스킵 로직 (주원인) |
| icdStore.ts:1612-1621 | serverTime 업데이트 |
| DashboardPage.vue:684-686 | 차트 타이머 |

---

**작성자**: Claude (Review Skill)
**검토자**: FE Expert, Performance Analyzer, BE Expert (병렬 분석)