# Settings 교차 검증 분석

> **분석일**: 2026-01-18
> **목적**: FE-BE Settings 연동 상태 검증 및 Dead Settings 정리

---

## 1. 분석 개요

### 검증 범위

```
[FE Settings UI] ←→ [BE SettingsController] ←→ [실제 사용 로직]
      │                      │                       │
   정의됨                 API 연동               실제 사용?
```

### 분석 결과 요약

| 상태 | 개수 | 설명 |
|:----:|:----:|------|
| ✅ 정상 | 13개 | 정의 + API + 로직 모두 연결 |
| ⚠️ Dead | 15개 | 정의만 존재, 로직 미사용 |
| ⚠️ 부분 | 12개 | API 응답만, 로직 미사용 |

---

## 2. 정상 연동 설정 (13개)

### Location 설정 ✅

| 설정 키 | FE | BE API | 사용 위치 |
|---------|:--:|:------:|----------|
| `location.latitude` | ✅ | ✅ | SunTrackService, TrackingService |
| `location.longitude` | ✅ | ✅ | SunTrackService, TrackingService |
| `location.altitude` | ✅ | ✅ | SunTrackService, TrackingService |

**사용 코드:**
```kotlin
// SunTrackService.kt
val location = GeodeticPoint(
    Math.toRadians(settings.location.latitude),
    Math.toRadians(settings.location.longitude),
    settings.location.altitude
)
```

### Tracking 설정 (부분)

| 설정 키 | FE | BE API | 사용 위치 |
|---------|:--:|:------:|----------|
| `tracking.msInterval` | ✅ | ✅ | TrackingService (WebSocket 주기) |

**사용 코드:**
```kotlin
// TrackingService.kt
val interval = settings.tracking.msInterval // 100ms 기본
```

### AntennaSpec 설정 ✅

| 설정 키 | FE | BE API | 사용 위치 |
|---------|:--:|:------:|----------|
| `antennaspec.tiltAngle` | ✅ | ✅ | TrackingService (Tilt 좌표변환) |

### AngleLimits 설정 (부분)

| 설정 키 | FE | BE API | 사용 위치 |
|---------|:--:|:------:|----------|
| `anglelimits.elevationMin` | ✅ | ✅ | SatelliteService (Pass 필터링) |

**사용 코드:**
```kotlin
// SatelliteService.kt
val passes = propagator.getPassesList(
    minElevation = Math.toRadians(settings.angleLimits.elevationMin)
)
```

### Ephemeris/SunTrack 설정 ✅

| 설정 키 | FE | BE API | 사용 위치 |
|---------|:--:|:------:|----------|
| `ephemeris.tracking.*` | ✅ | ✅ | TrackingService |
| `system.suntrack.*` | ✅ | ✅ | SunTrackService |

### Feed 설정 ✅

| 설정 키 | FE | BE API | 사용 위치 |
|---------|:--:|:------:|----------|
| `feed.enabledBands` | ✅ | ✅ | FeedService |

---

## 3. Dead Settings (15개) ⚠️

### 3.1 즉시 수정 필요 (HIGH)

#### tracking.durationDays

| 항목 | 내용 |
|------|------|
| **현재 상태** | 2일 **하드코딩** |
| **설정값** | 기본 7일 |
| **문제** | 사용자 설정이 무시됨 |

**현재 코드:**
```kotlin
// SatelliteService.kt:123
val endDate = startDate.shiftedBy(2.0 * Constants.JULIAN_DAY)  // 하드코딩!
```

**수정 방안:**
```kotlin
@Value("\${acs.settings.tracking.duration-days:7}")
private var durationDays: Long = 7

val endDate = startDate.shiftedBy(durationDays.toDouble() * Constants.JULIAN_DAY)
```

#### tracking.minElevationAngle

| 항목 | 내용 |
|------|------|
| **현재 상태** | 정의만 존재 |
| **설정값** | 기본 5.0° |
| **문제** | Pass 계산에 미적용 |

**수정 방안:**
```kotlin
// SatelliteService.kt
@Value("\${acs.settings.tracking.min-elevation-angle:5.0}")
private var minElevationAngle: Double = 5.0
```

### 3.2 기능 미구현 (MEDIUM)

#### Stow 설정 (6개)

| 설정 키 | 상태 | 비고 |
|---------|------|------|
| `stow.azimuth` | 미사용 | Stow 모드 미구현 |
| `stow.elevation` | 미사용 | Stow 모드 미구현 |
| `stow.train` | 미사용 | Stow 모드 미구현 |
| `stow.enabled` | 미사용 | |
| `stow.timeout` | 미사용 | |
| `stow.speed` | 미사용 | |

**조치:** Stow 모드 구현 시 연결 (P4로 이동)

#### SpeedLimits 설정 (6개)

| 설정 키 | 상태 | 비고 |
|---------|------|------|
| `speedlimits.azimuthMax` | 미사용 | ICD 명령 미구현 |
| `speedlimits.azimuthMin` | 미사용 | |
| `speedlimits.elevationMax` | 미사용 | |
| `speedlimits.elevationMin` | 미사용 | |
| `speedlimits.trainMax` | 미사용 | |
| `speedlimits.trainMin` | 미사용 | |

**조치:** ICD 모터 명령 구현 시 연결

### 3.3 삭제 검토 (LOW)

| 설정 키 | 상태 | 권장 조치 |
|---------|------|----------|
| `stepsizelimit.azimuth` | Step 모드에서 미사용 | 삭제 또는 연결 |
| `stepsizelimit.elevation` | Step 모드에서 미사용 | 삭제 또는 연결 |
| `algorithm.geoMinMotion` | 알고리즘에서 미사용 | 삭제 |

---

## 4. 부분 사용 설정 (12개) ⚠️

### AngleLimits (5개)

| 설정 키 | API 응답 | 로직 사용 | 권장 조치 |
|---------|:--------:|:--------:|----------|
| `anglelimits.azimuthMin` | ✅ | ❌ | 한계 체크 추가 |
| `anglelimits.azimuthMax` | ✅ | ❌ | 한계 체크 추가 |
| `anglelimits.elevationMax` | ✅ | ❌ | 한계 체크 추가 |
| `anglelimits.trainMin` | ✅ | ❌ | 한계 체크 추가 |
| `anglelimits.trainMax` | ✅ | ❌ | 한계 체크 추가 |

**권장 구현:**
```kotlin
// TrackingService.kt - 명령 전송 전 검증
fun validateCommand(az: Double, el: Double, train: Double): Boolean {
    val limits = settingsService.getAngleLimits()
    return az in limits.azimuthMin..limits.azimuthMax &&
           el in limits.elevationMin..limits.elevationMax &&
           train in limits.trainMin..limits.trainMax
}
```

### System 설정 (7개)

| 설정 키 | API 응답 | 로직 사용 | 비고 |
|---------|:--------:|:--------:|------|
| `system.jvm.*` | ✅ | ❌ | 모니터링 용도? |
| `system.performance.*` | ✅ | 부분 | 일부만 사용 |

---

## 5. 조치 계획

### Phase 1: 즉시 수정 (HIGH)

```yaml
작업:
  1. tracking.durationDays 하드코딩 제거
  2. tracking.minElevationAngle 연결

파일:
  - backend/.../service/SatelliteService.kt

예상 영향:
  - Pass 계산 결과가 설정에 따라 변경됨
  - 테스트 필요
```

### Phase 2: 한계 체크 추가 (MEDIUM)

```yaml
작업:
  1. AngleLimits를 명령 검증에 사용

파일:
  - backend/.../service/TrackingService.kt
  - backend/.../service/IcdCommandService.kt

효과:
  - 안테나 물리적 한계 초과 방지
  - 안전성 향상
```

### Phase 3: 정리 (LOW)

```yaml
작업:
  1. 사용하지 않는 설정 삭제 또는 문서화
  2. Stow, SpeedLimits는 미구현 표시

파일:
  - frontend/src/types/settings.ts
  - backend/.../dto/SettingsDto.kt
```

---

## 6. 체크리스트

### 즉시 수정
- [ ] `tracking.durationDays` → SatelliteService 연결
- [ ] `tracking.minElevationAngle` → Pass 필터링 연결
- [ ] 수정 후 테스트

### 중기 개선
- [ ] AngleLimits 한계 체크 구현
- [ ] 명령 검증 로직 추가

### 장기 정리
- [ ] Dead Settings 문서화
- [ ] 미구현 기능 표시 (Stow, SpeedLimits)
- [ ] 불필요한 설정 삭제

---

## 7. 참고: Settings 파일 위치

### Frontend

```
frontend/src/
├── types/settings.ts           # 타입 정의
├── stores/settingsStore.ts     # Pinia 스토어
└── pages/settings/             # Settings UI
```

### Backend

```
backend/src/main/kotlin/.../
├── controller/SettingsController.kt  # API 엔드포인트
├── service/SettingsService.kt        # 비즈니스 로직
└── dto/SettingsDto.kt               # DTO
```

---

**Last Updated**: 2026-01-18
