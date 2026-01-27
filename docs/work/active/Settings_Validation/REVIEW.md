# Settings_Validation 심층 재검토 (#R002)

> **검토일**: 2026-01-27
> **검토 유형**: Deep Review
> **이전 분석**: [ANALYSIS.md](./ANALYSIS.md) (2026-01-18)

---

## 1. 검토 요약

| 카테고리 | 이전 상태 | 현재 상태 | 변화 |
|---------|:--------:|:--------:|:----:|
| 정상 연동 | 13개 | 21개 | +8 |
| Dead Settings | 15개 | **14개** | -1 |
| 부분 사용 | 12개 | 6개 | -6 |
| **신규 발견** | - | **10개** | 🔴 |

### 핵심 발견사항

| ID | 심각도 | 영역 | 설명 |
|:--:|:------:|:----:|------|
| #R002-C1 | 🔴 | BE | 명령 검증 로직 완전 누락 |
| #R002-C2 | 🔴 | FE | Two Store Systems 설계 결함 |
| #R002-C3 | 🔴 | FE | 연결 설정 저장 로직 없음 |
| #R002-H1 | 🟠 | BE | AngleLimits 부분 사용 (1축만) |
| #R002-H2 | 🟠 | FE | FeedSettings 전체 저장 미포함 |
| #R002-H3 | 🟠 | FE | console.log 351개 잔존 |
| #R002-M1 | 🟡 | FE | Race Condition 위험 |
| #R002-M2 | 🟡 | FE | updateChangeStatus 서명 불일치 |
| #R002-M3 | 🟡 | BE | 주석 처리된 검증 로직 |
| #R002-L1 | 🟢 | BE | Dead Settings 문서화 미비 |

---

## 2. 이전 분석 대비 변경사항

### 2.1 수정 완료 확인 ✅

| 항목 | 이전 상태 | 현재 상태 | 증거 |
|------|:--------:|:--------:|------|
| `durationDays` | 하드코딩 | ✅ 설정값 사용 | PassScheduleService.kt:1464, EphemerisService.kt:446 |
| `preparationTimeMinutes` | 미확인 | ✅ 사용 중 | PassScheduleService.kt:248, 2801 외 |
| `sourceMinElevationAngle` | 미확인 | ✅ 사용 중 | PassScheduleService.kt:1459, 1614 |
| `keyholeAzimuthVelocityThreshold` | 미확인 | ✅ 사용 중 | SatelliteTrackingProcessor.kt 4곳 |
| `stowAngle*` (3개) | 미사용 | ✅ 사용 중 | PassScheduleService.kt:3419-3421 |

### 2.2 여전히 미해결 ⚠️

| 항목 | ANALYSIS.md 상태 | 현재 상태 | 비고 |
|------|:---------------:|:--------:|------|
| `minElevationAngle` | 보류 | ⏸️ 보류 유지 | sourceMinElevationAngle과 역할 중복 |
| SpeedLimits (6개) | 미사용 | ❌ 미사용 | API 응답만, 로직 없음 |
| AngleOffsetLimits (3개) | 미사용 | ❌ 미사용 | API 응답만, 로직 없음 |
| StowSpeed (3개) | 미사용 | ❌ 미사용 | stowAngle만 사용됨 |
| StepSizeLimit (2개) | 미사용 | ❌ 미사용 | 용도 불명확 |

---

## 3. 신규 발견 이슈 (Critical/High)

### #R002-C1: 명령 검증 로직 완전 누락 🔴

**심각도**: Critical
**영향**: 하드웨어 손상 위험

```
검색 결과: validateCommand, checkLimits, limitCheck → 0건
```

**문제점:**
- AngleLimits 설정값이 정의되어 있지만 명령 전송 전 검증하지 않음
- 안테나 물리적 한계를 초과하는 명령이 전송될 수 있음

**권장 구현:**
```kotlin
// TrackingService.kt 또는 IcdCommandService.kt
fun validateCommand(az: Double, el: Double, train: Double): Boolean {
    val limits = settingsService
    return az in limits.angleAzimuthMin..limits.angleAzimuthMax &&
           el in limits.angleElevationMin..limits.angleElevationMax &&
           train in limits.angleTrainMin..limits.angleTrainMax
}
```

**관련 파일:**
- `SettingsService.kt:462-497` - AngleLimits 정의
- `EphemerisService.kt:1867, 3312, 3548` - 주석 처리된 검증 로직

---

### #R002-C2: Two Store Systems 설계 결함 🔴

**심각도**: Critical
**영향**: 상태 불일치, 유지보수 어려움

**현재 구조:**
```
useSettingsStore (통합 Store)
  ├── import useLocationSettingsStore
  ├── import useAngleLimitsSettingsStore
  ├── import useSpeedLimitsSettingsStore
  ├── ... (9개 개별 Store)
  │
  └── 기능: 개별 Store들의 프록시 역할만 수행
```

**문제점:**
- `settingsStore.ts:6-17` - 9개 개별 Store import
- 통합 Store는 단순히 개별 Store를 호출하는 프록시
- 중복 로직, 상태 동기화 복잡성 증가

**권장 조치:**
1. 통합 Store 제거, 개별 Store 직접 사용 **또는**
2. 개별 Store 제거, 통합 Store로 통합

---

### #R002-C3: 연결 설정 저장 로직 없음 🔴

**심각도**: Critical
**영향**: 사용자 입력 데이터 손실

**위치**: `SettingsModal.vue:67-76, 193`

**문제점:**
```vue
<!-- 연결 설정 탭 (라인 67-76) -->
<q-input v-model="localServerAddress" label="Local Server" />
<q-input v-model="apiBaseUrl" label="API Base URL" />
<q-checkbox v-model="autoReconnect" label="Auto Reconnect" />

<!-- 저장 로직 (라인 193) -->
localStorage.setItem('isDarkMode', ...) // isDarkMode만 저장!
// localServerAddress, apiBaseUrl, autoReconnect → 저장 안 됨
```

---

### #R002-H1: AngleLimits 부분 사용 🟠

**심각도**: High
**영향**: 불완전한 안전 검증

| 설정 | 사용 여부 | 위치 |
|------|:--------:|------|
| `angleElevationMin` | ✅ | EphemerisService.kt:4810, PassScheduleService.kt:1831 |
| `angleElevationMax` | ❌ | - |
| `angleAzimuthMin/Max` | ❌ | - |
| `angleTrainMin/Max` | ❌ | - |

**문제**: Elevation Min만 필터링, 다른 축/방향 무시

---

### #R002-H2: FeedSettings 전체 저장 미포함 🟠

**심각도**: High
**영향**: 설정 저장 불완전

**위치**: `settingsStore.ts`, `FeedSettings.vue`

**문제점:**
- `saveAllSettings()` 함수에 FeedSettings 로직 없음
- FeedSettings는 체크박스 변경 시 개별 저장됨
- 전체 저장 버튼 클릭 시 Feed 설정 제외

---

### #R002-H3: console.log 351개 잔존 🟠

**심각도**: High
**영향**: 프로덕션 로그 오염, CLAUDE.md 규칙 위반

**검색 결과:**
```
frontend/src/stores/ 내 console 호출: 351개 (16개 파일)
```

**주요 위치:**
| 파일 | 개수 |
|------|:----:|
| passScheduleStore.ts | 151 |
| icdStore.ts | 62 |
| ephemerisTrackStore.ts | 40 |
| settingsStore.ts | 27 |

---

## 4. 신규 발견 이슈 (Medium/Low)

### #R002-M1: Race Condition 위험 🟡

**위치**: `LocationSettings.vue:111-114`

```typescript
// 변경 중이면 서버 업데이트 무시
if (hasUnsavedChanges.value) return
```

**문제**: 다중 탭/컴포넌트에서 동시 수정 시 마지막 값 손실 가능

---

### #R002-M2: updateChangeStatus 서명 불일치 🟡

| Store | 인자 개수 | 서명 |
|-------|:--------:|------|
| locationSettingsStore | 2개 | `(hasChanges, changes?)` |
| stowSettingsStore | 3개 | `(type, hasChanges, changes?)` |
| offsetLimitsSettingsStore | 3개 | `(type, hasChanges, changes?)` |

**문제**: 호출자가 각 Store마다 다른 패턴 사용 필요

---

### #R002-M3: 주석 처리된 검증 로직 🟡

**위치**: `EphemerisService.kt:1867, 3312, 3548`

```kotlin
// 주석 처리됨 (비활성화 이유 불명확)
// val filterThreshold = settingsService.angleElevationMin
// if (filteredPoints.last().elevationAngle < filterThreshold) { ... }
```

**문제**: 왜 비활성화되었는지 문서화 없음

---

### #R002-L1: Dead Settings 문서화 미비 🟢

**현황**: 14개 Dead Settings가 왜 정의되어 있는지 불명확

| 카테고리 | 개수 | 추정 용도 |
|---------|:----:|---------|
| SpeedLimits | 6개 | ICD 모터 속도 제한? |
| AngleOffsetLimits | 3개 | 오프셋 보정 한계? |
| StowSpeed | 3개 | Stow 이동 속도? |
| StepSizeLimit | 2개 | Step 이동 제한? |

**권장**: 각 설정에 TODO 주석 추가 또는 삭제 결정

---

## 5. 조치 계획

### Phase 1: Critical (즉시)

| ID | 작업 | 담당 | 예상 파일 |
|:--:|------|:----:|----------|
| #R002-C1 | 명령 검증 로직 구현 | BE | TrackingService.kt |
| #R002-C2 | Store 구조 결정 | FE | settingsStore.ts |
| #R002-C3 | 연결 설정 저장 구현 | FE | SettingsModal.vue |

### Phase 2: High (이번 주)

| ID | 작업 | 담당 | 예상 파일 |
|:--:|------|:----:|----------|
| #R002-H1 | AngleLimits 전체 적용 | BE | EphemerisService.kt |
| #R002-H2 | FeedSettings 통합 | FE | settingsStore.ts |
| #R002-H3 | console.log 정리 | FE | stores/*.ts |

### Phase 3: Medium (이번 스프린트)

| ID | 작업 | 담당 |
|:--:|------|:----:|
| #R002-M1 | Race Condition 처리 | FE |
| #R002-M2 | 함수 서명 통일 | FE |
| #R002-M3 | 주석 로직 검토 | BE |

---

## 6. 권장 다음 단계

```
🔴 Critical 문제 발견. 다음 스킬 실행을 권장합니다:

1. /bugfix #R002-C1  - BE 명령 검증 로직 구현
2. /refactor #R002-C2 - FE Store 구조 개선
3. /cleanup #R002-H3  - console.log 정리
```

---

**검토 완료**: 2026-01-27
**Review ID**: #R002
**이전 분석**: #R001 (ANALYSIS.md, 2026-01-18)
