# RFC-003: BE 코드 품질 개선

> **버전**: 1.4.0 | **작성일**: 2026-01-14
> **상태**: Draft | **우선순위**: P2 (점진적 개선)
> **역할**: BE 코드 품질 이슈 (Null 안전성, 매직넘버, 동시성) 및 장기적 상태 머신 통합

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.4.0 | 2026-01-14 | PassScheduleService !! 3건→6건 수정, ThreadManager `!= true` 패턴 추가 |
| 1.3.0 | 2026-01-14 | !! 연산자 상세 위치 표 추가 (우선순위별 분류: P0 29건, P1 10건, P2 7건) |
| 1.2.0 | 2026-01-13 | 문서 역할 명확화: 제목 변경 "상태 머신 추출" → "BE 코드 품질 개선" |
| 1.1.0 | 2026-01-13 | BE 코드 품질 이슈 추가 (!! 46건, 매직넘버 40+, mutableListOf 3건), 관련 RFC 섹션 추가 |
| 1.0.0 | 2026-01-13 | 초기 작성 (기존 RFC_SatelliteTrackingEngine.md 기반, P2 격하 반영) |

---

## 1. 배경 (Context)

### 왜 이 변경이 필요한가?

| 현재 | 문제점 |
|------|--------|
| EphemerisService.kt (5,060줄) | 모든 로직 혼재 |
| PassScheduleService.kt (2,896줄) | 40% 코드 중복 |
| FE 상태 분리 관리 | 동기화 어려움 |

### 우선순위 격하 이유

| 이유 | 설명 |
|------|------|
| **현재 동작** | 시스템이 정상 작동 중 |
| **리스크** | 추적 로직 변경은 고위험 |
| **P0/P1 우선** | DB, 로깅, API 표준화가 더 급함 |
| **점진적 접근** | 전면 리팩토링보다 부분 개선 |

---

## 2. 점진적 개선 전략

### 2.1 당장 하지 않는 것

| 항목 | 이유 |
|------|------|
| SatelliteTrackingEngine 추출 | 고위험, P0/P1 완료 후 |
| trackingStateStore 생성 | FE 전면 수정 필요 |
| 전면 리팩토링 | 안정성 우선 |

### 2.2 점진적으로 개선할 것

| 순서 | 작업 | 시점 |
|------|------|------|
| 1 | 블로킹 코드 제거 (Thread.sleep) | RFC-004 Phase 2와 함께 |
| 2 | 중복 메서드 추출 (필요시) | 버그 수정/기능 추가 시 |
| 3 | 상태 머신 통합 | P0/P1 완료 후 |

---

## 3. 현재 유지할 구조

### 3.1 백엔드

```
현재 구조 (유지)
├── EphemerisService.kt     - Ephemeris 전용 로직
├── PassScheduleService.kt  - PassSchedule 전용 로직
└── SunTrackService.kt      - SunTrack 별도 유지
```

> **SunTrack 별도 유지**: 위성 추적과 태양 추적은 완전히 다른 도메인이므로 통합하지 않음

### 3.2 프론트엔드

```
현재 구조 (유지)
├── stores/ephemeris/       - Ephemeris 상태
├── stores/passSchedule/    - PassSchedule 상태
└── stores/icd/             - ICD 실시간 데이터
```

---

## 4. 참고: 원래 계획 (추후 진행)

기존 RFC_SatelliteTrackingEngine.md의 전체 계획은 [legacy/RFC_SatelliteTrackingEngine.md](./legacy/RFC_SatelliteTrackingEngine.md)에서 확인 가능합니다.

### 요약

| 항목 | 내용 |
|------|------|
| **목표** | 40% 중복 → 10% 이하 |
| **방법** | SatelliteTrackingEngine 공통 클래스 추출 |
| **예상** | BE 1개, FE 1개 신규 파일 |
| **기간** | 14일 (P0/P1 완료 후) |

---

## 5. 체크리스트

### 즉시 적용 (RFC-004 Phase 2와 함께)

- [ ] `PassScheduleController.kt:1944` - `Thread.sleep(100)` 제거
- [ ] `BatchStorageManager.kt:294` - `Thread.sleep(100)` 제거
- [ ] `UdpFwICDService.kt:1074, :1148` - `Thread.sleep(1000)` 개선
- [ ] `ElevationCalculator.kt:78` - `runBlocking` 제거

### BE 코드 품질 이슈 (심층 분석 결과)

#### Null 안전성 (`!!` 연산자) - 46건

##### 파일별 분포

| 파일 | 건수 | 우선순위 |
|------|------|----------|
| SunTrackService.kt | 17건 | P0 (실시간 추적) |
| ThreadManager.kt | 8건 | P0 (스레드 관리) |
| EphemerisService.kt | 4건 | P0 (위성 추적) |
| UdpFwICDService.kt | 4건 | P0 (UDP 통신) |
| PassScheduleService.kt | 6건 | P1 (스케줄 관리) |
| OrekitConfig.kt | 2건 | P1 (초기화) |
| ModeController.kt | 2건 | P1 (API) |
| ElevationCalculator.kt | 2건 | P2 (유틸리티) |
| TLEParser.kt | 2건 | P2 (파싱) |
| SolarOrekitCalculator.kt | 1건 | P2 (태양 계산) |
| 기타 | 1건 | P2 |
| **합계** | **49건** | |

##### 우선순위별 상세 위치

**P0 (Critical) - 29건**: 실시간 추적 중 NPE 발생 시 시스템 중단

| 파일:라인 | 코드 | 위험도 |
|-----------|------|--------|
| SunTrackService.kt:488-489 | `sunData!!.azimuth`, `sunData!!.elevation` | 태양 추적 중단 |
| SunTrackService.kt:782-783 | `position!!.azimuth`, `position!!.elevation` | 위치 계산 실패 |
| SunTrackService.kt:기타 | 13건 (유사 패턴) | 추적 실패 |
| ThreadManager.kt:145 | `thread!!.interrupt()` | 스레드 관리 실패 |
| ThreadManager.kt:기타 | 7건 (유사 패턴) | 스레드 누수 |
| EphemerisService.kt:1200 | `satellite!!.propagate()` | 위성 추적 중단 |
| UdpFwICDService.kt:488-489 | `channel!!.send()` | UDP 통신 실패 |

**P1 (High) - 13건**: 운영 중 오류 발생 가능

| 파일:라인 | 코드 | 영향 |
|-----------|------|------|
| PassScheduleService.kt | 6건 (`.status`, `.pass` 등) | 스케줄 조회 실패 |
| OrekitConfig.kt:78 | `dataPath!!` | 초기화 실패 |
| ModeController.kt:234 | `currentMode!!` | API 응답 오류 |

**P2 (Medium) - 7건**: 유틸리티/파싱 로직

| 파일:라인 | 코드 | 영향 |
|-----------|------|------|
| ElevationCalculator.kt:78 | `result!!` | 계산 오류 |
| TLEParser.kt:45 | `line!!` | 파싱 실패 |

##### 수정 방안

**방안 1**: `?.let {}` 안전 호출
```kotlin
// Before (위험)
val result = satellite!!.propagate()

// After (안전)
satellite?.let { sat ->
    val result = sat.propagate()
    // 후속 처리
} ?: logger.warn("Satellite not initialized")
```

**방안 2**: `requireNotNull()` 명시적 예외
```kotlin
// Before (위험)
val result = satellite!!.propagate()

// After (명시적)
val validSatellite = requireNotNull(satellite) {
    "Satellite must be initialized before propagation"
}
val result = validSatellite.propagate()
```

**방안 3**: nullable 타입 재설계 (권장)
```kotlin
// Before (nullable + !!)
private var satellite: Satellite? = null

fun propagate() = satellite!!.propagate()

// After (non-null 보장)
private lateinit var satellite: Satellite

fun propagate() = satellite.propagate()  // 초기화 검증 필수
```

**방안 4**: `!= true` 패턴 (Boolean 반환 메서드용 - ThreadManager)
```kotlin
// Before (위험) - ThreadManager.kt:547-570
if (!executor?.awaitTermination(5, TimeUnit.SECONDS)!!) {
    executor?.shutdownNow()
}

// After (안전) - null은 false로 처리
if (executor?.awaitTermination(5, TimeUnit.SECONDS) != true) {
    executor?.shutdownNow()
}
```

> **참고**: `!= true`는 `null` 또는 `false`를 모두 `true`가 아님으로 처리하여 안전함.

#### 매직 넘버/하드코딩 - 40+건

| 유형 | 예시 | 권장 |
|------|------|------|
| 타임아웃 | `delay(1000)` | `TRACKING_DELAY_MS` 상수화 |
| 각도 범위 | `if (angle > 360)` | `MAX_AZIMUTH_ANGLE` 상수화 |
| 배열 인덱스 | `data[7]` | `enum class ICDField` 활용 |

```kotlin
// Before (매직 넘버)
if (elevation > 5.0) { ... }

// After (상수화)
companion object {
    const val MIN_ELEVATION_THRESHOLD = 5.0  // 최소 고도각 (도)
}
if (elevation > MIN_ELEVATION_THRESHOLD) { ... }
```

#### 동시성 문제 (`mutableListOf`) - 3건

| 파일 | 위치 | 문제 |
|------|------|------|
| PushDataController.kt | :57 | WebSocket 세션 동시 접근 |
| EphemerisService.kt | :234 | 동시 추적 요청 시 충돌 |
| PassScheduleService.kt | :156 | 스케줄 업데이트 중 읽기 |

**수정 방안**: `CopyOnWriteArrayList` 또는 `Collections.synchronizedList()` 사용

```kotlin
// Before (스레드 안전하지 않음)
private val sessions = mutableListOf<WebSocketSession>()

// After (스레드 안전)
private val sessions = CopyOnWriteArrayList<WebSocketSession>()
```

### P0/P1 완료 후 (점진적)

- [ ] 중복 메서드 식별 및 추출
- [ ] 상태 전이 로직 통합 검토
- [ ] FE 스토어 분리 검토
- [ ] 매직 넘버 상수화 (40+ 건)
- [ ] mutableListOf 동시성 안전 컬렉션으로 교체 (3건)

### !! 연산자 제거 우선순위

**P0 (Critical) - 29건** - RFC-007 Phase 5 이후
- [ ] SunTrackService.kt - 17건 (태양 추적 안정성)
- [ ] ThreadManager.kt - 8건 (스레드 관리)
- [ ] EphemerisService.kt - 4건 (위성 추적)
- [ ] UdpFwICDService.kt - 4건 (UDP 통신) - **RFC-007 Phase 2와 함께**

**P1 (High) - 13건** - P0 완료 후
- [ ] PassScheduleService.kt - 6건
- [ ] OrekitConfig.kt - 2건
- [ ] ModeController.kt - 2건
- [ ] 기타 - 3건

**P2 (Medium) - 7건** - 필요시
- [ ] ElevationCalculator.kt - 2건
- [ ] TLEParser.kt - 2건
- [ ] SolarOrekitCalculator.kt - 1건
- [ ] 기타 - 2건

---

## 6. 관련 RFC

| RFC | 관계 | 설명 |
|-----|------|------|
| [RFC-004](./RFC-004_API_Standardization.md) | **선행 필수** | Thread.sleep/runBlocking 제거가 Phase 2에 포함 |
| [RFC-007](./RFC-007_BE_Infrastructure.md) | **연관** | GlobalData 동시성 (Phase 7), UdpFwICDService !! 제거 (Phase 2) |
| [RFC-008](./RFC-008_Frontend_Restructuring.md) | 연관 | FE 스토어 분리와 연계 (icdStore 분리) |
| RFC-005 (예정) | 후속 | 리팩토링 후 테스트 작성 |

### 의존성 그래프

```
RFC-004 Phase 2 (블로킹 코드 제거)
    │
    ├───────────────────┐
    ▼                   ▼
RFC-003 (BE 품질)    RFC-007 Phase 7-8
    │                (GlobalData/lateinit)
    │                   │
    ├──→ RFC-008 ◄──────┘
    │   (FE 스토어)
    ▼
RFC-005 (테스트)
```

---

**작성자**: Claude
**최종 수정**: 2026-01-14
**검토자**: 백엔드 심층 분석 기반
**승인일**: -
