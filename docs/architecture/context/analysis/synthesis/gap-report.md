# 문서-코드 Gap 분석 보고서

> 분석 일시: 2026-01-15
> 분석 대상: docs/architecture/context/*.md vs 실제 코드

## 1. 개요

이 보고서는 기존 context 문서와 실제 코드베이스를 비교하여 발견된 불일치(Gap)를 정리합니다.

**분석 범위:**
- Backend 코드: 67개 파일, 33,284줄
- Frontend 코드: 93개 파일, 30,000줄+
- Context 문서: domain/, architecture/ 폴더

---

## 2. Backend 구조 Gap

### 2.1 파일 존재 여부

| 문서 기술 파일 | 실제 존재 | Gap 상태 |
|--------------|----------|----------|
| StepController.kt | ❌ 없음 | **문서 오류** |
| SlewController.kt | ❌ 없음 | **문서 오류** |
| TrackingService.kt | ❌ 없음 | **문서 오류** |
| ICDParser.kt | ❌ (ICDService 내부) | **구조 차이** |
| ICDBuilder.kt | ❌ (ICDService 내부) | **구조 차이** |
| EphemerisController.kt | ✅ 존재 (1,091줄) | 일치 |
| PassScheduleController.kt | ✅ 존재 (1,557줄) | 일치 |
| ICDService.kt | ✅ 존재 (2,788줄) | 일치 |
| EphemerisService.kt | ✅ 존재 (5,057줄) | 크기 차이 |

### 2.2 코드 크기 차이

| 파일 | 문서 기술 | 실제 크기 | 차이 |
|-----|----------|----------|------|
| EphemerisService.kt | 4,986줄 | 5,057줄 | +71줄 |
| PassScheduleService.kt | 1,500줄 | 3,846줄 | **+2,346줄** |
| ICDService.kt | 2,788줄 | 2,788줄 | 일치 |

### 2.3 폴더 구조 차이

**문서 기술:**
```
backend/src/main/kotlin/.../
├── controller/
├── service/
├── algorithm/
│   ├── ephemeris/
│   ├── icd/
│   └── coordinate/
├── dto/
├── model/
└── config/
```

**실제 구조:**
```
backend/src/main/kotlin/.../
├── controller/
│   ├── icd/           # ICDController.kt
│   ├── mode/          # Ephemeris, PassSchedule, SunTrack
│   ├── system/        # Settings, Logging, Performance
│   └── websocket/     # PushDataController.kt
├── service/
│   ├── datastore/     # DataStoreService.kt
│   ├── hardware/      # HardwareErrorLogService.kt
│   ├── icd/           # ICDService.kt
│   ├── mode/          # Ephemeris, PassSchedule, SunTrack
│   ├── system/        # Settings, BatchStorage, Logging
│   ├── udp/           # UdpFwICDService.kt
│   └── websocket/     # PushDataService.kt
├── algorithm/
│   ├── axislimitangle/    # LimitAngleCalculator.kt
│   ├── axistransformation/ # CoordinateTransformer.kt
│   ├── elevation/         # ElevationCalculator.kt
│   ├── satellitetracker/  # OrekitCalculator, Processor
│   └── suntrack/          # SPA, Grena3, SolarOrekit
├── dto/
├── model/
├── config/
├── event/             # ACSEventBus (문서 누락)
├── openapi/           # API 문서 (문서 누락)
├── repository/        # JPA Repository (문서 누락)
└── settings/entity/   # JPA Entity (문서 누락)
```

**주요 차이점:**
- `event/` 폴더 누락 (이벤트 버스 시스템)
- `openapi/` 폴더 누락 (OpenAPI 문서)
- `repository/`, `settings/entity/` 누락 (JPA 관련)
- algorithm 하위 구조 완전히 다름

---

## 3. ICD 프로토콜 Gap

### 3.1 클래스 구조 차이

**문서 기술:**
- `ICDCommand` (별도 클래스)
- `ICDStatus` (별도 클래스)
- `ICDParser.kt` (별도 파일)
- `ICDBuilder.kt` (별도 파일)

**실제 구조:**
- `ICDService.kt` 내 중첩 클래스들:
  - `class Classify` (수신 처리)
  - `class ReadStatus` (상태 읽기)
  - `class SatelliteTrackOne/Two/Three` (추적 명령)
  - `class Standby`, `class Stop`, `class Emergency` 등

### 3.2 데이터 구조 차이

**문서 기술:**
```kotlin
// ICDCommand
header: Int (0xAA55)
commandType: Int
azimuth: Double (라디안)
// ...
checksum: Int (CRC-16)
```

**실제 구조:**
```kotlin
// STX/ETX 프레임 방식
companion object {
  const val ICD_STX: Byte = 0x02
  const val ICD_ETX: Byte = 0x03
}
// 명령별 개별 SetDataFrame/GetDataFrame 클래스
```

### 3.3 통신 프로토콜 차이

**문서 기술:**
- header: 0xAA55 / 0x55AA

**실제 구현:**
- STX (0x02) + 명령코드 + 데이터 + CRC16 + ETX (0x03)
- **완전히 다른 프로토콜 구조**

---

## 4. 모드 시스템 Gap

### 4.1 컨트롤러 구조 차이

**문서 기술:**
| 모드 | Controller |
|-----|-----------|
| Step | StepController.kt |
| Slew | SlewController.kt |

**실제 구조:**
- Step, Slew 모드는 **별도 Controller 없음**
- `ICDController.kt`에서 통합 처리
- 멀티/싱글 매뉴얼 제어로 구현

### 4.2 상태 관리 차이

**문서 기술:**
```kotlin
// TrackingService에서 상태 관리
class TrackingService {
  private val state: TrackingState
}
```

**실제 구현:**
```kotlin
// EphemerisService 내 상태머신
enum class TrackingState {
  IDLE, PREPARING, WAITING, TRACKING, COMPLETED, ERROR
}
enum class PreparingPhase {
  TRAIN_MOVING, TRAIN_STABILIZING, MOVING_TO_TARGET
}
```

### 4.3 Frontend 페이지 정확도

**문서 기술:**
- `pages/mode/StepPage.vue`
- `pages/mode/SlewPage.vue`
- `pages/mode/EphemerisDesignationPage.vue`
- `pages/mode/PassSchedulePage.vue`
- `pages/mode/SunTrackPage.vue`

**실제 존재:**
- ✅ `StepPage.vue`
- ✅ `SlewPage.vue`
- ✅ `EphemerisDesignationPage.vue`
- ✅ `PassSchedulePage.vue`
- ✅ `SunTrackPage.vue`
- ➕ `PedestalPositionPage.vue` (문서 누락)
- ➕ `StandbyPage.vue` (문서 누락)

---

## 5. Frontend Gap

### 5.1 Store 구조 차이

**문서 기술:**
```typescript
// modeStore 중심 구조
interface ModeState {
  currentMode: Mode
  previousMode: Mode
  modeParams: ModeParams
  isTransitioning: boolean
}
```

**실제 구조:**
```
stores/
├── icd/
│   └── icdStore.ts (2,971줄) - 핵심!
├── mode/
│   ├── passScheduleStore.ts (2,452줄)
│   ├── ephemerisTrackStore.ts (1,287줄)
│   ├── stepStore.ts
│   ├── slewStore.ts
│   ├── standbyStore.ts
│   └── pedestalPositionStore.ts
├── settings/
│   └── settingsStore.ts
└── auth/
    └── authStore.ts
```

**주요 차이:**
- `modeStore` 언급되었으나 실제로는 **모드별 개별 Store**
- `icdStore.ts`가 **핵심** (문서에서 덜 강조됨)

### 5.2 성능 최적화 Gap

**문서 기술:**
- shallowRef 그룹화로 리렌더링 최적화

**실제 구현:**
- ✅ shallowRef 적용됨
- ➕ **Web Worker 활용** (문서 누락)
- ➕ **메모리 관리** (문서 누락)

---

## 6. 알고리즘 Gap

### 6.1 계산기 클래스 차이

**문서 기술:**
- `OrekitCalculator`
  - `calculatePosition()`
  - `calculatePointingAngle()`

**실제 구현:**
- `OrekitCalculator.kt` (627줄)
  - `calculateSatellitePositionAndVelocity()`
  - `detectVisibilityPeriods()`
  - `generateSatelliteTrackingSchedule()`
  - `parseUTCString()`
- `SatelliteTrackingProcessor.kt` (1,387줄) - **문서 누락**
  - `processSatelliteTracking()`
  - `applyAxisTransformation()`
  - `convertToLimitAngle()`
  - `calculateMetrics()`

### 6.2 좌표 변환 차이

**문서 기술:**
```
ECI → ECEF → Topocentric → Az/El
```

**실제 구현:**
```
TLE → SGP4/SDP4 전파 → ECI → TopocentricFrame
→ Az/El (2축) → 3축 변환 (CoordinateTransformer)
→ ±270° 변환 (LimitAngleCalculator) → 최종 포지셔너 각도
```

**추가 발견:**
- 3축 변환 로직 (문서 누락)
- ±270° Limit Angle 변환 (문서 누락)
- Keyhole 판단 로직 (문서 누락)

---

## 7. 문서 누락 항목

### 7.1 완전 누락

| 항목 | 파일/폴더 | 중요도 |
|-----|----------|--------|
| 이벤트 버스 | ACSEventBus.kt | 🔴 높음 |
| 데이터 저장소 | DataStoreService.kt | 🔴 높음 |
| UDP 서비스 | UdpFwICDService.kt | 🔴 높음 |
| 배치 저장 | BatchStorageManager.kt | 🟠 중간 |
| 스레드 관리 | ThreadManager.kt | 🟠 중간 |
| OpenAPI 문서 | openapi/*.kt | 🟡 낮음 |
| Web Worker | trackingWorker.ts | 🟠 중간 |
| 좌표 변환기 | CoordinateTransformer.kt | 🔴 높음 |
| 각도 제한 | LimitAngleCalculator.kt | 🔴 높음 |

### 7.2 부분 누락/부정확

| 항목 | 문서 내용 | 실제 내용 |
|-----|----------|----------|
| PassScheduleService 크기 | 1,500줄 | 3,846줄 |
| ICD 프로토콜 | 0xAA55 헤더 | STX/ETX 프레임 |
| 상태 관리 | TrackingService | EphemerisService 내장 |
| Store 구조 | modeStore 중심 | 모드별 개별 Store |

---

## 8. 권장 수정 사항

### 8.1 즉시 수정 필요 (High Priority)

1. **ICD 프로토콜 문서 전면 수정**
   - 0xAA55/0x55AA 헤더 → STX/ETX 프레임 방식
   - ICDCommand/ICDStatus → 내부 클래스 구조 설명

2. **Backend 구조 문서 업데이트**
   - StepController, SlewController 제거
   - TrackingService 제거
   - 실제 폴더 구조로 업데이트

3. **알고리즘 문서 보강**
   - CoordinateTransformer 추가
   - LimitAngleCalculator 추가
   - SatelliteTrackingProcessor 추가

### 8.2 중기 수정 (Medium Priority)

1. **Frontend Store 문서 재작성**
   - icdStore 중심 구조 설명
   - 모드별 Store 개별 설명
   - Web Worker 활용 설명

2. **이벤트 시스템 문서 추가**
   - ACSEventBus 구조
   - 이벤트 타입 및 흐름

3. **데이터 흐름 문서 정확화**
   - 좌표 변환 파이프라인 상세화
   - ±270° 변환 로직 추가

### 8.3 장기 개선 (Low Priority)

1. **코드 크기 정보 업데이트**
2. **OpenAPI 문서화 방식 설명**
3. **테스트 관련 문서 추가**

---

## 9. 요약

| 영역 | 일치율 | 주요 Gap |
|-----|--------|---------|
| Backend 구조 | 60% | 폴더 구조, 파일 존재 여부 |
| ICD 프로토콜 | 30% | 프로토콜 구조 완전 상이 |
| 모드 시스템 | 70% | Controller 구조, 상태 관리 |
| Frontend | 50% | Store 구조, 성능 최적화 |
| 알고리즘 | 40% | 좌표 변환, 각도 제한 |

**총평:**
기존 문서는 전반적인 개념과 주요 컴포넌트를 설명하고 있으나, 세부 구현 내용과 상당한 차이가 있습니다. 특히 ICD 프로토콜과 알고리즘 영역에서 실제 코드와의 Gap이 크므로 우선적인 문서 업데이트가 필요합니다.

---

**문서 버전**: 1.0.0
**작성자**: Analysis Team
**최종 검토**: 2026-01-15
