# Settings 시스템 전체 분석 (#R003)

> **검토일**: 2026-01-27
> **검토 유형**: Deep Fresh Review (처음부터 분석)
> **검토 범위**: BE + FE + 통합

---

## Executive Summary

| 영역 | 총 항목 | 정상 | 문제 | 점수 |
|:----:|:------:|:----:|:----:|:----:|
| **BE 설정** | 67개 | 30개 (44%) | 37개 미사용 | 44/100 |
| **FE Store** | 11개 | 9개 (82%) | 2개 이중화 | 70/100 |
| **FE-BE 통합** | 27개 API | 25개 (93%) | 2개 불일치 | 85/100 |
| **종합** | - | - | - | **66/100** |

### Critical 이슈 (4개)

| ID | 영역 | 문제 | 영향 |
|:--:|:----:|------|------|
| #R003-C1 | BE | 37개 설정 미사용 (56%) | 유지보수 부담 |
| #R003-C2 | FE | 2개 Store 이중화 | 메모리 낭비, 동기화 불일치 |
| #R003-C3 | 통합 | Feed 설정 JSON 직렬화 불일치 | 저장/로드 실패 가능 |
| #R003-C4 | 통합 | preparationTimeMinutes BE DTO 누락 | 저장 불가 |

---

## 1. Backend 분석

### 1.1 설정 전수 조사 (67개)

| 카테고리 | 설정 수 | 사용됨 | 미사용 |
|---------|:------:|:-----:|:------:|
| Location | 3 | 3 | 0 |
| Tracking | 4 | 2 | 2 |
| Stow (Angle) | 3 | 3 | 0 |
| Stow (Speed) | 3 | 0 | 3 |
| AntennaSpec | 2 | 1 | 1 |
| AngleLimits | 6 | 6 | 0 |
| SpeedLimits | 6 | 0 | 6 |
| AngleOffsetLimits | 3 | 0 | 3 |
| TimeOffsetLimits | 2 | 0 | 2 |
| StepSizeLimit | 2 | 0 | 2 |
| Algorithm | 1 | 0 | 1 |
| Feed | 1 | 1 | 0 |
| System.UDP | 6 | 2 | 4 |
| System.Tracking | 4 | 1 | 3 |
| System.Storage | 3 | 0 | 3 |
| System.SunTrack | 4 | 4 | 0 |
| System.WebSocket | 1 | 0 | 1 |
| System.Performance | 7 | 6 | 1 |
| System.JVM | 4 | 4 | 0 |
| Ephemeris | 2 | 2 | 0 |
| **합계** | **67** | **30** | **37** |

### 1.2 미사용 설정 상세 (37개)

| 카테고리 | 설정 키 | 정의 위치 | 미사용 사유 |
|---------|--------|---------|-----------|
| Tracking | durationDays | SettingsService.kt:62 | 로직상 고정값 사용 |
| Tracking | minElevationAngle | SettingsService.kt:63 | TODO [보류] 주석 |
| Stow Speed | speed.azimuth/elevation/train | SettingsService.kt:425-439 | Stow 모드에서 각도만 사용 |
| AntennaSpec | trueNorthOffsetAngle | SettingsService.kt:77 | 하드코딩됨 |
| SpeedLimits | 전체 6개 | SettingsService.kt:505-540 | API 응답만, 검증 로직 없음 |
| AngleOffsetLimits | 전체 3개 | SettingsService.kt:548-562 | 검증 로직 없음 |
| TimeOffsetLimits | 전체 2개 | SettingsService.kt:102-103 | 검증 로직 없음 |
| StepSizeLimit | min/max | SettingsService.kt:593,600 | Step 모드 미사용 |
| Algorithm | geoMinMotion | SettingsService.kt | 정의만 있음 |
| System.* | 12개 | 다수 | 정의만 있음 |

### 1.3 BE 잠재적 문제

| 심각도 | 위치 | 문제 | 권장 조치 |
|:-----:|------|------|---------|
| 🔴 HIGH | ElevationCalculator.kt | systemUdpTimeout 잘못 사용 (25ms→10초) | timeout 설정 재검토 |
| 🔴 HIGH | OrekitCalculatorTest.kt | systemUdpMaxBufferSize를 물리량 계수로 사용 | 별도 계수 설정 추가 |
| 🟡 MED | SettingsService.kt:382-386 | minElevationAngle 미적용 | 필터링 로직 추가 |
| 🟡 MED | SettingsService.kt:220-247 | DB 저장 실패 시 메모리만 업데이트 | 롤백 메커니즘 |
| 🟡 MED | SettingsController 전체 | 입력 검증 부족 | @Validated 추가 |
| 🟡 MED | SettingsService.kt:51 | 다중 설정 변경 시 원자성 미보장 | 트랜잭션 추가 |

---

## 2. Frontend 분석

### 2.1 Store 구조

| Store | 역할 | 상태 |
|-------|------|:----:|
| api/settings/settingsStore.ts | 9개 개별 Store 통합 | ✅ 현재 사용 |
| api/settingsStore.ts | 통합 관리 (레거시) | ⚠️ 이중화 |
| locationSettingsStore.ts | 위치 설정 | ✅ |
| trackingSettingsStore.ts | 추적 설정 | ✅ |
| stowSettingsStore.ts | Stow 설정 (각도+속도) | ✅ |
| angleLimitsSettingsStore.ts | 각도 제한 | ✅ |
| speedLimitsSettingsStore.ts | 속도 제한 | ✅ |
| offsetLimitsSettingsStore.ts | 오프셋 제한 | ✅ |
| algorithmSettingsStore.ts | 알고리즘 | ✅ |
| stepSizeLimitSettingsStore.ts | 스텝 크기 | ✅ |
| antennaSpecSettingsStore.ts | 안테나 사양 | ✅ |
| ui/feedSettingsStore.ts | Feed 밴드 | ✅ |

### 2.2 FE 잠재적 문제

| 심각도 | 위치 | 문제 | 권장 조치 |
|:-----:|------|------|---------|
| 🔴 CRIT | settingsStore.ts 2개 | Store 이중화 | api/settingsStore.ts 삭제 |
| 🔴 HIGH | api/settingsStore.ts:201-210 | console.log 4줄 디버깅 흔적 | 제거 |
| 🟡 MED | settingsService.ts:407-457 | Feed 설정 console.log 9줄 | logger로 변경 |
| 🟡 MED | AlgorithmSettings.vue:89,105,114 | console.log 3줄 | 제거 |
| 🟡 MED | 모든 Settings 컴포넌트 | JSON.stringify 비교 성능 | 필드별 비교 |
| 🟡 MED | LocationSettings.vue:134-136 | 초기값 "0,0,0" 감지 오류 가능 | null 사용 |

### 2.3 console.* 통계

| 파일 | 개수 | 유형 |
|------|:----:|------|
| stores/ 전체 | 76줄 | 디버깅 + 에러 로깅 |
| settingsStore.ts (api/) | 4줄 | 디버깅 흔적 |
| settingsService.ts | 9줄 | Feed 관련 |
| AlgorithmSettings.vue | 3줄 | 개발 흔적 |

---

## 3. FE-BE 통합 분석

### 3.1 API 매핑 (27개)

| 상태 | 개수 | 비율 |
|:----:|:----:|:----:|
| ✅ 정상 | 25 | 93% |
| ⚠️ 불일치 | 2 | 7% |

### 3.2 통합 이슈 상세

#### #R003-C3: Feed 설정 JSON 직렬화 불일치 🔴

```
FE 전송: { enabledBands: ["s", "x"] }  ← 배열
BE 저장: "feed.enabledBands" → """["s","x"]"""  ← JSON 문자열!
FE 로드: JSON.parse("""["s","x"]""") → ["s", "x"]  ← 복구

문제: BE에서 List를 JSON 문자열로 직렬화
     → 불필요한 이중 직렬화
     → 파싱 실패 시 기본값으로 대체
```

**위치**: settingsService.ts:410-424

#### #R003-C4: preparationTimeMinutes BE DTO 누락 🔴

```typescript
// FE TrackingSettings
interface TrackingSettings {
  msInterval: number
  durationDays: number
  minElevationAngle: number
  preparationTimeMinutes: number  // ← FE에 있음
}

// BE TrackingRequest
data class TrackingRequest(
  val msInterval: Int,
  val durationDays: Long,
  val minElevationAngle: Float
  // preparationTimeMinutes 없음! ❌
)
```

**영향**: FE에서 BE로 저장 불가

### 3.3 데이터 흐름

```
[FE Component] → [FE Store] → [FE Service] → [axios]
                                    ↓
[BE Controller] → [BE Service] → [ConcurrentHashMap] → [DB]
                                    ↓
                            [API 응답]
                                    ↓
[FE Service 파싱] → [FE Store] → [FE Component]
```

### 3.4 응답 형식 불일치

| 요청 | 응답 형식 |
|-----|---------|
| GET | `Map<String, Any>` 직접 반환 |
| POST | `ResponseEntity<Map>` { status, message, data } |

---

## 4. 발견 이슈 종합

### Critical (4개)

| ID | 영역 | 문제 | 위치 | 연계 |
|:--:|:----:|------|------|:----:|
| #R003-C1 | BE | 37개 설정 미사용 (56%) | SettingsService.kt | /cleanup |
| #R003-C2 | FE | 2개 Store 이중화 | settingsStore.ts x2 | /refactor |
| #R003-C3 | 통합 | Feed JSON 직렬화 불일치 | settingsService.ts:410 | /bugfix |
| #R003-C4 | 통합 | preparationTimeMinutes 누락 | BE TrackingRequest | /bugfix |

### High (6개)

| ID | 영역 | 문제 | 위치 | 연계 |
|:--:|:----:|------|------|:----:|
| #R003-H1 | BE | systemUdpTimeout 잘못 사용 | ElevationCalculator.kt | /bugfix |
| #R003-H2 | BE | 입력 검증 부족 | SettingsController | /bugfix |
| #R003-H3 | BE | 다중 설정 원자성 미보장 | SettingsService.kt:51 | /bugfix |
| #R003-H4 | FE | console.log 76줄 | stores/, service | /cleanup |
| #R003-H5 | FE | JSON.stringify 성능 | Settings 컴포넌트 | /optimize |
| #R003-H6 | 통합 | GET/POST 응답 형식 불일치 | SettingsController | /refactor |

### Medium (8개)

| ID | 영역 | 문제 |
|:--:|:----:|------|
| #R003-M1 | BE | minElevationAngle 미적용 |
| #R003-M2 | BE | DB 저장 실패 시 롤백 없음 |
| #R003-M3 | BE | feedEnabledBands JSON 파싱 취약 |
| #R003-M4 | BE | sourceMinElevationAngle 기본값 0 (이상) |
| #R003-M5 | FE | Store-컴포넌트 이중 상태 관리 |
| #R003-M6 | FE | 초기값 0,0,0 감지 오류 |
| #R003-M7 | FE | feedSettingsStore 비동기 미await |
| #R003-M8 | 통합 | Map<String, Any> 런타임 타입 위험 |

---

## 5. 조치 계획

### Phase 1: Critical (즉시)

| 순번 | ID | 작업 | 담당 |
|:---:|:--:|------|:----:|
| 1 | #R003-C2 | api/settingsStore.ts 삭제 (Store 통합) | FE |
| 2 | #R003-C3 | Feed 설정 BE에서 List 그대로 반환 | BE |
| 3 | #R003-C4 | TrackingRequest에 preparationTimeMinutes 추가 | BE |

### Phase 2: High (이번 주)

| 순번 | ID | 작업 | 담당 |
|:---:|:--:|------|:----:|
| 4 | #R003-H1 | systemUdpTimeout 용도 확인 및 수정 | BE |
| 5 | #R003-H2 | @Validated + 범위 검증 추가 | BE |
| 6 | #R003-H4 | console.log 76줄 제거/logger 변경 | FE |

### Phase 3: Medium (이번 스프린트)

| 순번 | ID | 작업 |
|:---:|:--:|------|
| 7 | #R003-C1 | 미사용 설정 37개 정리 검토 |
| 8 | #R003-H3 | 다중 설정 변경 트랜잭션 추가 |
| 9 | #R003-M1 | minElevationAngle 필터링 로직 추가 |

---

## 6. 아키텍처 개선 권장

### BE SettingsService 분할

```
현재: 단일 SettingsService (1234줄, 67개 필드)

권장:
├── LocationSettings
├── TrackingSettings
├── StowSettings
├── AntennaSpecSettings
├── LimitSettings (각도, 속도, 오프셋)
├── AlgorithmSettings
├── SystemSettings
└── EphemerisSettings
```

### FE Store 구조 정리

```
현재:
├── api/settingsStore.ts (레거시, 삭제 대상)
├── api/settings/settingsStore.ts (통합)
└── api/settings/*SettingsStore.ts (9개)

권장:
├── api/settings/settingsStore.ts (통합 - 유지)
└── api/settings/*SettingsStore.ts (9개 - 유지)
```

---

## 7. 권장 다음 단계

```
🔴 Critical 문제 4개 발견. 다음 스킬 실행을 권장합니다:

1. /bugfix #R003-C3  - Feed JSON 직렬화 수정 (BE)
2. /bugfix #R003-C4  - preparationTimeMinutes DTO 추가 (BE)
3. /refactor #R003-C2 - Store 이중화 제거 (FE)
4. /cleanup #R003-H4  - console.log 76줄 정리
```

진행하시겠습니까?

---

**Review ID**: #R003
**검토 완료**: 2026-01-27
**분석 방식**: 처음부터 전체 분석 (기존 문서 무시)
