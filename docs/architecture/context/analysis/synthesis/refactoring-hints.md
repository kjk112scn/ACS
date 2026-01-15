# 리팩토링 포인트 및 권장사항

> 분석 일시: 2026-01-15
> 분석 기반: Phase 1-4 심층 분석 결과

## 1. 개요

코드베이스 심층 분석 결과 식별된 리팩토링 포인트를 정리합니다.

**분석 범위:**
- Backend: 67개 파일, 33,284줄
- Frontend: 93개 파일, 30,000줄+

---

## 2. 즉시 리팩토링 대상 (P1)

### 2.1 Backend: EphemerisService.kt (5,057줄)

**문제점:**
- 단일 파일에 과도한 책임 집중
- 상태머신 로직, TLE 캐시, 배치 저장, 명령 전송 혼재
- 테스트 어려움

**권장 분할:**

```
EphemerisService.kt (5,057줄)
    ↓ 분할
├── EphemerisService.kt (~1,500줄)
│   └── 오케스트레이션, 상태 조회
├── EphemerisTrackingStateMachine.kt (~1,000줄)
│   └── 상태 전이 로직
├── EphemerisTLEManager.kt (~500줄)
│   └── TLE 캐시 및 조회
├── EphemerisDataBatcher.kt (~500줄)
│   └── 배치 저장 로직
└── EphemerisCommandSender.kt (~800줄)
    └── UDP 명령 전송
```

**예상 효과:**
- 각 클래스 단위 테스트 가능
- 상태머신 로직 독립 검증
- 유지보수성 향상

---

### 2.2 Backend: PassScheduleService.kt (3,846줄)

**문제점:**
- v1.0 + v2.0 상태머신 로직 공존
- ScheduleTrackingContext 관리 복잡
- 스케줄 모니터링과 추적 실행 혼재

**권장 분할:**

```
PassScheduleService.kt (3,846줄)
    ↓ 분할
├── PassScheduleService.kt (~1,200줄)
│   └── 스케줄 관리 (CRUD, 조회)
├── PassScheduleStateMachine.kt (~800줄)
│   └── v2.0 통합 상태머신
├── PassScheduleMonitor.kt (~600줄)
│   └── 100ms 타이머 기반 모니터링
├── PassScheduleTracker.kt (~700줄)
│   └── 실시간 추적 실행
└── PassScheduleTLECache.kt (~300줄)
    └── TLE 캐시 관리
```

**추가 작업:**
- v1.0 상태머신 로직 제거
- ScheduleTrackingContext 불변 객체화

---

### 2.3 Frontend: PassSchedulePage.vue (4,838줄)

**문제점:**
- 단일 컴포넌트에 UI/로직 집중
- 차트, 테이블, 오프셋 제어 혼재
- 재사용 불가

**권장 분할:**

```
PassSchedulePage.vue (4,838줄)
    ↓ 분할
├── PassSchedulePage.vue (~1,500줄)
│   └── 레이아웃 및 조합
├── OffsetControlPanel.vue (~400줄)
│   └── 오프셋 입력 (3개 페이지 공유)
├── PositionViewChart.vue (~200줄)
│   └── ECharts 차트
├── ScheduleTable.vue (~300줄)
│   └── 스케줄 테이블
├── ScheduleInfoPanel.vue (~150줄)
│   └── 선택된 스케줄 정보
└── ScheduleControlSection.vue (~200줄)
    └── 제어 버튼 그룹
```

---

### 2.4 Frontend: EphemerisDesignationPage.vue (4,340줄)

**권장 분할:**

```
EphemerisDesignationPage.vue (4,340줄)
    ↓ 분할
├── EphemerisDesignationPage.vue (~1,500줄)
├── OffsetControlPanel.vue (공유)
├── PositionViewChart.vue (공유)
├── SatelliteInfoPanel.vue (~200줄)
├── TLEInputDialog.vue (~150줄)
├── ScheduleSelectionModal.vue (~250줄)
└── KeyholeInfoSection.vue (~150줄)
```

---

## 3. 중기 리팩토링 대상 (P2)

### 3.1 ICDService.kt 구조화 (2,788줄)

**현재 문제:**
- 19개 내부 클래스가 하나의 파일에 존재
- 명령별 클래스 간 중복 코드

**권장 구조:**

```
service/icd/
├── ICDService.kt (~500줄)
│   └── 서비스 진입점, 라우팅
├── parser/
│   ├── ICDClassify.kt (~300줄)
│   └── ICDStatusParser.kt (~400줄)
├── commands/
│   ├── SatelliteTrackCommand.kt (~500줄)
│   ├── ManualControlCommand.kt (~400줄)
│   ├── SystemCommand.kt (~300줄)
│   └── FeedCommand.kt (~200줄)
└── protocol/
    ├── ICDFrame.kt (~100줄)
    └── CRC16.kt (기존)
```

---

### 3.2 Frontend Store 최적화

**icdStore.ts (2,971줄) 분할:**

```
stores/icd/
├── icdStore.ts (~800줄)
│   └── 핵심 상태 + 연결 관리
├── icdAntennaState.ts (~600줄)
│   └── 안테나 위치/속도 상태
├── icdBoardStatus.ts (~800줄)
│   └── 보드별 상태 비트
├── icdTrackingState.ts (~400줄)
│   └── 추적 상태 (Ephemeris/Pass/Sun)
└── icdErrorHandler.ts (~200줄)
    └── i18n 에러 변환
```

---

### 3.3 DashboardPage.vue (2,728줄) 분할

```
DashboardPage.vue
    ↓ 분할
├── DashboardPage.vue (~1,200줄)
├── AxisCard.vue (~300줄)
├── EmergencyControl.vue (~150줄)
├── StatusIndicator.vue (~100줄)
├── ModeSelector.vue (~100줄)
└── ModeContentArea.vue (~100줄)
```

---

## 4. 장기 리팩토링 대상 (P3)

### 4.1 TLE 캐시 통합

**현재:**
- EphemerisService: `satelliteTleCache`
- PassScheduleService: `passScheduleTleCache`
- 중복 관리, 동기화 문제 가능

**권장:**

```kotlin
@Service
class TLECacheService {
    private val cache = ConcurrentHashMap<String, TLEData>()

    fun get(satelliteId: String): TLEData?
    fun put(satelliteId: String, data: TLEData)
    fun invalidate(satelliteId: String)
    fun clear()
}
```

---

### 4.2 Algorithm 계층 인터페이스 추출

**현재:**
- OrekitCalculator 직접 의존
- 테스트 시 Orekit 초기화 필요

**권장:**

```kotlin
interface SatelliteCalculator {
    fun calculatePosition(tle: TLE, time: ZonedDateTime): SatellitePosition
    fun detectVisibilityPeriods(...): List<VisibilityPeriod>
}

class OrekitSatelliteCalculator : SatelliteCalculator {
    // Orekit 기반 구현
}

class MockSatelliteCalculator : SatelliteCalculator {
    // 테스트용 구현
}
```

---

### 4.3 Frontend Composables 추출

**공통 로직 추출:**

```typescript
// composables/useAxisFormatter.ts
export function useAxisFormatter() {
  const formatAngle = (value: number, precision = 2) => ...
  const formatSpeed = (value: number) => ...
  return { formatAngle, formatSpeed }
}

// composables/useChartRendering.ts
export function useChartRendering(chartRef: Ref<HTMLElement>) {
  const initChart = (options: EChartsOption) => ...
  const updateChart = (data: ChartData) => ...
  const disposeChart = () => ...
  return { initChart, updateChart, disposeChart }
}

// composables/useAxisControl.ts
export function useAxisControl() {
  const sendGoCommand = (...) => ...
  const sendStopCommand = (...) => ...
  const sendStowCommand = (...) => ...
  return { sendGoCommand, sendStopCommand, sendStowCommand }
}
```

---

## 5. 코드 품질 개선

### 5.1 광범위 Exception 처리 제거

**현재 패턴:**
```kotlin
try {
    // 복잡한 로직
} catch (e: Exception) {
    logger.error("Error", e)
}
```

**권장 패턴:**
```kotlin
try {
    // 로직
} catch (e: TLEParseException) {
    logger.warn("TLE parsing failed: ${e.message}")
    throw BadRequestException("Invalid TLE format")
} catch (e: OrekitException) {
    logger.error("Orekit calculation error", e)
    throw InternalServerException("Satellite calculation failed")
}
```

---

### 5.2 DTO 부족 해결

**현재:**
- Map<String, Any?> 광범위 사용
- 타입 안전성 부족

**권장:**
```kotlin
// dto/tracking/
data class TrackingScheduleResponse(
    val mstId: Long,
    val detailId: Int,
    val satelliteName: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val startAzimuth: Double,
    val startElevation: Double,
    val trainAngle: Double,
    val isKeyhole: Boolean
)
```

---

### 5.3 테스트 추가

**현재:**
- Backend 테스트: 1개 (OrekitCalculatorTest.kt)
- Frontend 테스트: 없음

**권장 추가:**
```
backend/src/test/
├── service/
│   ├── EphemerisServiceTest.kt
│   ├── PassScheduleServiceTest.kt
│   └── ICDServiceTest.kt
├── algorithm/
│   ├── OrekitCalculatorTest.kt (기존)
│   ├── LimitAngleCalculatorTest.kt
│   └── CoordinateTransformerTest.kt
└── integration/
    └── TrackingFlowTest.kt

frontend/src/__tests__/
├── stores/
│   ├── icdStore.spec.ts
│   └── passScheduleStore.spec.ts
├── composables/
│   └── useAxisFormatter.spec.ts
└── components/
    └── OffsetControlPanel.spec.ts
```

---

## 6. 리팩토링 우선순위 요약

| 순위 | 대상 | 현재 크기 | 목표 | 예상 효과 |
|-----|------|----------|------|----------|
| P1-1 | EphemerisService.kt | 5,057줄 | 5개 파일 | 테스트 가능, 유지보수성 |
| P1-2 | PassScheduleService.kt | 3,846줄 | 5개 파일 | 상태머신 단순화 |
| P1-3 | PassSchedulePage.vue | 4,838줄 | 6개 파일 | 컴포넌트 재사용 |
| P1-4 | EphemerisDesignationPage.vue | 4,340줄 | 7개 파일 | 컴포넌트 재사용 |
| P2-1 | ICDService.kt | 2,788줄 | 폴더 구조화 | 명령 관리 용이 |
| P2-2 | icdStore.ts | 2,971줄 | 5개 파일 | 상태 분리 |
| P2-3 | DashboardPage.vue | 2,728줄 | 6개 파일 | 컴포넌트 재사용 |
| P3-1 | TLE 캐시 통합 | 2개 서비스 | 1개 서비스 | 중복 제거 |
| P3-2 | Algorithm 인터페이스 | - | 테스트 용이 | 의존성 주입 |
| P3-3 | Composables 추출 | - | 3개 파일 | 로직 재사용 |

---

## 7. 리팩토링 실행 가이드

### Phase A: 공통 컴포넌트 추출 (1주)

1. `OffsetControlPanel.vue` 추출
2. `AxisCard.vue` 추출
3. 3개 페이지에 적용

### Phase B: 대형 서비스 분할 (2주)

1. `EphemerisService.kt` 분할
2. `PassScheduleService.kt` 분할
3. 기존 테스트 마이그레이션

### Phase C: 대형 페이지 분할 (2주)

1. `PassSchedulePage.vue` 분할
2. `EphemerisDesignationPage.vue` 분할
3. `DashboardPage.vue` 분할

### Phase D: 품질 개선 (1주)

1. DTO 클래스 추가
2. Exception 처리 개선
3. 단위 테스트 추가

---

**문서 버전**: 1.0.0
**작성자**: Analysis Team
**최종 검토**: 2026-01-15
