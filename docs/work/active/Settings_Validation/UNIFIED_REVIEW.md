# Settings 시스템 통합 리뷰

> **최종 업데이트**: 2026-01-27
> **Review ID**: #R-SETTINGS (통합)
> **상태**: 📋 조치 대기

---

## 리뷰 이력

| 순서 | 문서 | 날짜 | 방식 | 이슈 수 |
|:---:|------|------|------|:------:|
| 1 | [ANALYSIS.md](./ANALYSIS.md) | 2026-01-18 | 최초 분석 | 1 |
| 2 | [REVIEW.md](./REVIEW.md) (#R002) | 2026-01-27 | 기존 기반 재검토 | 10 |
| 3 | [../Review_Settings_Full/REVIEW.md](../Review_Settings_Full/REVIEW.md) (#R003) | 2026-01-27 | 처음부터 새 분석 | 18 |
| 4 | [COMPARISON.md](./COMPARISON.md) | 2026-01-27 | 3개 비교 분석 | - |

---

## Executive Summary

| 지표 | 값 |
|------|:--:|
| **총 설정 수** | 67개 |
| **사용 중** | 30개 (44%) |
| **미사용** | 37개 (56%) |
| **Critical 이슈** | 6개 |
| **High 이슈** | 7개 |
| **Medium 이슈** | 11개 |
| **종합 점수** | **66/100** |

---

## 통합 이슈 목록

### Critical (6개) - 즉시 수정 필요

| ID | 출처 | 영역 | 문제 | 수정 파일 | 연계 |
|:--:|:----:|:----:|------|----------|:----:|
| **C-01** | R002+R003 | FE | Store 이중화 (2개 존재) | `api/settingsStore.ts` (삭제) | /refactor |
| **C-02** | R003 | 통합 | Feed JSON 직렬화 불일치 | `BE: SettingsService.kt`, `FE: settingsService.ts:410` | /bugfix |
| **C-03** | R003 | 통합 | preparationTimeMinutes BE DTO 누락 | `BE: TrackingRequest.kt` | /bugfix |
| **C-04** | R002 | BE | 명령 검증 로직 완전 누락 | `TrackingService.kt` 또는 `IcdCommandService.kt` | /bugfix |
| **C-05** | R002 | FE | 연결 설정 저장 로직 없음 | `SettingsModal.vue:67-76, 193` | /bugfix |
| **C-06** | R003 | BE | 37개 설정 미사용 (56%) | `SettingsService.kt` | /cleanup |

### High (7개) - 이번 주 수정

| ID | 출처 | 영역 | 문제 | 수정 파일 |
|:--:|:----:|:----:|------|----------|
| **H-01** | R002 | BE | AngleLimits 부분 사용 (ElevationMin만) | `EphemerisService.kt`, `PassScheduleService.kt` |
| **H-02** | R002 | FE | FeedSettings 전체 저장 미포함 | `settingsStore.ts` |
| **H-03** | R002+R003 | FE | console.log 351개 잔존 | `stores/*.ts`, `settingsService.ts` |
| **H-04** | R003 | BE | systemUdpTimeout 잘못 사용 (25ms→10초) | `ElevationCalculator.kt` |
| **H-05** | R003 | BE | 입력 검증 부족 | `SettingsController.kt` (전체) |
| **H-06** | R003 | BE | 다중 설정 원자성 미보장 | `SettingsService.kt:51` |
| **H-07** | R003 | 통합 | GET/POST 응답 형식 불일치 | `SettingsController.kt` |

### Medium (11개) - 이번 스프린트

| ID | 출처 | 영역 | 문제 |
|:--:|:----:|:----:|------|
| **M-01** | R002 | FE | Race Condition 위험 (`LocationSettings.vue:111-114`) |
| **M-02** | R002 | FE | updateChangeStatus 서명 불일치 (2개 vs 3개 인자) |
| **M-03** | R002 | BE | 주석 처리된 검증 로직 (`EphemerisService.kt:1867, 3312, 3548`) |
| **M-04** | R003 | BE | minElevationAngle 미적용 |
| **M-05** | R003 | BE | DB 저장 실패 시 롤백 없음 |
| **M-06** | R003 | BE | feedEnabledBands JSON 파싱 취약 |
| **M-07** | R003 | BE | sourceMinElevationAngle 기본값 0 (이상) |
| **M-08** | R003 | FE | Store-컴포넌트 이중 상태 관리 |
| **M-09** | R003 | FE | 초기값 0,0,0 감지 오류 |
| **M-10** | R003 | FE | feedSettingsStore 비동기 미await |
| **M-11** | R003 | 통합 | Map<String, Any> 런타임 타입 위험 |

---

## 수정 파일 참조 (급한 순서)

### Phase 1: Critical (즉시)

| 순위 | 파일 | 이슈 ID | 작업 내용 |
|:---:|------|:------:|----------|
| 1 | `frontend/src/stores/api/settingsStore.ts` | C-01 | **삭제** (레거시 Store) |
| 2 | `backend/.../dto/TrackingRequest.kt` | C-03 | `preparationTimeMinutes: Long` 필드 추가 |
| 3 | `backend/.../service/SettingsService.kt` | C-02 | Feed List 그대로 반환 (JSON 문자열 아님) |
| 4 | `frontend/src/services/api/settingsService.ts:410` | C-02 | JSON.parse 제거 (배열 그대로 수신) |
| 5 | `backend/.../service/TrackingService.kt` | C-04 | `validateCommand()` 함수 추가 |
| 6 | `frontend/src/components/Settings/SettingsModal.vue:193` | C-05 | 연결 설정 localStorage 저장 추가 |

### Phase 2: High (이번 주)

| 순위 | 파일 | 이슈 ID | 작업 내용 |
|:---:|------|:------:|----------|
| 7 | `frontend/src/stores/**/*.ts` | H-03 | console.log 351개 제거 |
| 8 | `backend/.../controller/SettingsController.kt` | H-05 | `@Validated` + 범위 검증 추가 |
| 9 | `frontend/src/stores/api/settings/settingsStore.ts` | H-02 | FeedSettings 저장 로직 통합 |
| 10 | `backend/.../service/EphemerisService.kt` | H-01 | AngleLimits 전체 축 적용 |

### Phase 3: Medium (이번 스프린트)

| 순위 | 파일 | 이슈 ID | 작업 내용 |
|:---:|------|:------:|----------|
| 11 | `backend/.../service/SettingsService.kt` | C-06 | 미사용 37개 설정 정리/문서화 |
| 12 | `frontend/.../LocationSettings.vue:111-114` | M-01 | Race Condition 처리 |
| 13 | `backend/.../service/SettingsService.kt:51` | H-06 | 다중 설정 트랜잭션 추가 |

---

## 설정 현황 (67개)

### 사용 중 (30개) ✅

| 카테고리 | 설정 | 사용 위치 |
|---------|------|----------|
| Location | latitude, longitude, altitude | SPACalculator.kt |
| Tracking | msInterval, preparationTimeMinutes | TrackingService.kt, PassScheduleService.kt |
| Stow Angle | azimuth, elevation, train | PassScheduleService.kt:3419-3421 |
| AntennaSpec | tiltAngle | SatelliteTrackingProcessor.kt |
| AngleLimits | 전체 6개 | SettingsController (API만) |
| Feed | enabledBands | FeedService |
| Ephemeris | sourceMinElevationAngle, keyholeThreshold | PassScheduleService.kt, EphemerisService.kt |
| SunTrack | 4개 threshold | SolarOrekitCalculator.kt |
| Performance | 6개 코어/메모리 | ThreadManager.kt |
| JVM | 4개 튜닝 | ThreadManager.kt |

### 미사용 (37개) ❌

| 카테고리 | 설정 | 미사용 사유 |
|---------|------|-----------|
| Tracking | durationDays, minElevationAngle | 고정값/보류 |
| Stow Speed | 3개 | Stow 각도만 사용 |
| AntennaSpec | trueNorthOffsetAngle | 하드코딩됨 |
| SpeedLimits | 6개 | API만, 검증 로직 없음 |
| AngleOffsetLimits | 3개 | API만, 검증 로직 없음 |
| TimeOffsetLimits | 2개 | API만, 검증 로직 없음 |
| StepSizeLimit | 2개 | Step 모드 미사용 |
| Algorithm | geoMinMotion | 정의만 |
| System.UDP | 4개 | 정의만 |
| System.Tracking | 3개 | 정의만 |
| System.Storage | 3개 | 정의만 |
| System.WebSocket | 1개 | 정의만 |
| System.Performance | threshold | 정의만 |

---

## 수정 완료 이력

| 날짜 | 항목 | 이전 | 이후 | 출처 |
|------|------|------|------|:----:|
| 2026-01-18 | durationDays | 하드코딩 2일 | 설정값 사용 | ANALYSIS |
| 확인됨 | preparationTimeMinutes | - | 사용 중 | R002 |
| 확인됨 | sourceMinElevationAngle | - | 사용 중 | R002 |
| 확인됨 | keyholeAzimuthVelocityThreshold | - | 사용 중 | R002 |
| 확인됨 | stowAngle (3개) | 미사용 | 사용 중 | R002 |

---

## 조치 계획

### 권장 실행 순서

```bash
# Phase 1: Critical (즉시)
/refactor C-01   # Store 이중화 제거
/bugfix C-02     # Feed JSON 직렬화
/bugfix C-03     # preparationTimeMinutes DTO
/bugfix C-04     # 명령 검증 로직
/bugfix C-05     # 연결 설정 저장

# Phase 2: High (이번 주)
/cleanup H-03    # console.log 351개 정리
/bugfix H-05     # 입력 검증 추가

# Phase 3: Cleanup
/cleanup C-06    # 미사용 37개 설정 정리
```

### 예상 소요 시간

| Phase | 이슈 수 | 예상 시간 |
|:-----:|:------:|:---------:|
| 1 (Critical) | 6개 | 4-6시간 |
| 2 (High) | 7개 | 1-2일 |
| 3 (Medium) | 11개 | 1주 |

---

## 아키텍처 개선 권장

### BE SettingsService 분할

```
현재: 단일 SettingsService (1234줄, 67개 필드)

권장:
├── LocationSettingsService
├── TrackingSettingsService
├── StowSettingsService
├── LimitSettingsService (각도, 속도, 오프셋)
├── AlgorithmSettingsService
├── SystemSettingsService
└── EphemerisSettingsService
```

### FE Store 정리

```
삭제:
└── api/settingsStore.ts (레거시)

유지:
├── api/settings/settingsStore.ts (통합)
└── api/settings/*SettingsStore.ts (9개 개별)
```

---

## 관련 문서

| 문서 | 경로 | 용도 |
|------|------|------|
| 최초 분석 | `./ANALYSIS.md` | 이력 |
| 재검토 | `./REVIEW.md` | R002 상세 |
| 전체 분석 | `../Review_Settings_Full/REVIEW.md` | R003 상세 |
| 비교 분석 | `./COMPARISON.md` | 3개 비교 |
| **본 문서** | `./UNIFIED_REVIEW.md` | **통합 액션** |

---

**작성일**: 2026-01-27
**다음 업데이트**: 이슈 수정 시
