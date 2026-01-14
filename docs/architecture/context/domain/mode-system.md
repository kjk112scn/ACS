# 모드 시스템 (Mode System)

> ACS 시스템의 운용 모드: 상황에 따른 안테나 제어 방식

## 개요

```
모드 선택 → 파라미터 설정 → 제어 시작 → 실시간 모니터링 → 모드 종료
```

## 모드 목록

| 모드 | 설명 | 자동화 수준 |
|-----|------|------------|
| Standby | 대기 | 수동 |
| Step | 단계 이동 | 반자동 |
| Slew | 슬루 이동 | 반자동 |
| EphemerisDesignation | 위성 지정 추적 | 자동 |
| PassSchedule | 패스 스케줄 | 완전 자동 |
| SunTrack | 태양 추적 | 자동 |

## 모드 상세

### Standby (대기 모드)
```yaml
목적: 안테나 정지 상태 유지
동작: 현재 위치 고정
전환: 모든 모드에서 전환 가능
```

### Step (스텝 모드)
```yaml
목적: 정밀한 각도 조정
동작: 지정 각도만큼 단계 이동
파라미터:
  - stepSize: 이동 각도 (0.1° ~ 10°)
  - axis: 이동 축 (Az/El/Train)
사용: 수동 미세 조정, 테스트
```

### Slew (슬루 모드)
```yaml
목적: 목표 위치로 빠른 이동
동작: 목표 각도까지 연속 이동
파라미터:
  - targetAzimuth: 목표 방위각
  - targetElevation: 목표 고도각
  - targetTrain: 목표 편파각
사용: 사전 위치 지정, 초기화
```

### EphemerisDesignation (위성 지정 추적)
```yaml
목적: 특정 위성 실시간 추적
동작: 위성 위치 계산 → 안테나 지향
파라미터:
  - satelliteId: NORAD ID
  - tle: TLE 데이터
연동: EphemerisService, TrackingService
사용: 단일 위성 추적
```

### PassSchedule (패스 스케줄)
```yaml
목적: 다중 위성 자동 추적
동작: 스케줄 기반 자동 모드 전환
파라미터:
  - schedule: 패스 목록
  - autoSwitch: 자동 전환 여부
연동: PassScheduleService
사용: 무인 운용, 다중 위성
```

### SunTrack (태양 추적)
```yaml
목적: 태양 위치 추적
동작: 태양 위치 계산 → 안테나 지향
라이브러리: solarpositioning
사용: 태양 관측, 안테나 테스트
```

## 모드 전환

### 전환 규칙
```
┌─────────┐
│ Standby │ ←─────────────────┐
└────┬────┘                   │
     │                        │
     ▼                        │
┌─────────┐    ┌─────────┐    │
│  Step   │ ←→ │  Slew   │────┤
└────┬────┘    └────┬────┘    │
     │              │         │
     ▼              ▼         │
┌──────────────────────────┐  │
│  EphemerisDesignation    │──┤
└────────────┬─────────────┘  │
             │                │
             ▼                │
┌──────────────────────────┐  │
│      PassSchedule        │──┤
└──────────────────────────┘  │
                              │
┌──────────────────────────┐  │
│       SunTrack           │──┘
└──────────────────────────┘
```

### 전환 조건
- Standby → 다른 모드: 항상 가능
- 추적 모드 간 전환: Standby 경유 권장
- 긴급 시: 모든 상태에서 Standby 즉시 전환

## 코드 위치

### Backend
| 모드 | Controller | Service |
|-----|-----------|---------|
| Step | StepController.kt | StepService.kt |
| Slew | SlewController.kt | SlewService.kt |
| Ephemeris | EphemerisController.kt | EphemerisService.kt |
| PassSchedule | PassScheduleController.kt | PassScheduleService.kt |
| SunTrack | SunTrackController.kt | SunTrackService.kt |

### Frontend
| 모드 | 페이지 |
|-----|-------|
| Step | pages/mode/StepPage.vue |
| Slew | pages/mode/SlewPage.vue |
| Ephemeris | pages/mode/EphemerisDesignationPage.vue |
| PassSchedule | pages/mode/PassSchedulePage.vue |
| SunTrack | pages/mode/SunTrackPage.vue |

## 상태 관리

### 모드 상태 (Pinia)
```typescript
// stores/modeStore.ts
interface ModeState {
  currentMode: Mode
  previousMode: Mode
  modeParams: ModeParams
  isTransitioning: boolean
}
```

### 모드별 파라미터
```typescript
type ModeParams =
  | StepParams
  | SlewParams
  | EphemerisParams
  | PassScheduleParams
  | SunTrackParams
```

## 주의사항

- **모드 전환 시**: 이전 명령 완료 확인 필요
- **추적 모드**: 30ms 주기 유지 필수
- **PassSchedule**: 시간 동기화 중요 (NTP)
- **비상 정지**: 모든 모드에서 Standby 전환 가능해야 함

## 참조

- [위성 추적](satellite-tracking.md)
- [안테나 제어](antenna-control.md)
- [ICD 프로토콜](icd-protocol.md)

---

**최종 수정:** 2026-01-14
