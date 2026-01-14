# 안테나 제어 (Antenna Control)

> ACS 시스템의 핵심 하드웨어 인터페이스: 안테나의 물리적 움직임 제어

## 개요

```
지향각 명령 → ICD 프로토콜 → UDP 통신 → 안테나 컨트롤러 → 모터 구동
```

## 안테나 축 시스템

### 주요 축

| 축 | 변수명 | UI 표시 | 범위 | 설명 |
|---|-------|--------|------|------|
| Azimuth | `azimuth` | Azimuth | 0-360° | 방위각 (북쪽 기준 시계방향) |
| Elevation | `elevation` | Elevation | 0-90° | 고도각 (수평면 기준) |
| Train | `train` | **Tilt** | ±180° | 편파 조정 |

**중요:** 변수명은 `train`, UI에서는 `Tilt`로 표시

### 좌표계 변환

```
위성 위치 (ECI) → 지상국 기준 (Topocentric) → 안테나 지향각 (Az/El/Train)
```

## 제어 모드

### Step 모드
- 지정된 각도만큼 단계적 이동
- 수동 미세 조정에 사용
- `StepController.kt`

### Slew 모드
- 목표 각도로 연속 이동
- 빠른 위치 변경에 사용
- `SlewController.kt`

### 추적 모드 (Tracking)
- 실시간 위성 위치 추적
- 30ms 주기로 명령 갱신
- `TrackingController.kt`

## ICD 프로토콜

### 명령 구조

```kotlin
data class AntennaCommand(
    val azimuth: Double,      // 라디안
    val elevation: Double,    // 라디안
    val train: Double,        // 라디안
    val mode: ControlMode,
    val timestamp: Long
)
```

### 상태 응답

```kotlin
data class AntennaStatus(
    val currentAzimuth: Double,
    val currentElevation: Double,
    val currentTrain: Double,
    val targetAzimuth: Double,
    val targetElevation: Double,
    val targetTrain: Double,
    val motorStatus: MotorStatus,
    val errorCode: Int
)
```

### 통신 주기

| 항목 | 주기 | 프로토콜 |
|-----|------|---------|
| 명령 전송 | 30ms | UDP |
| 상태 수신 | 30ms | UDP |
| 브로드캐스트 | 30ms | WebSocket |

## 코드 위치

### Backend
| 기능 | 파일 |
|------|------|
| ICD 프로토콜 | `backend/service/ICDService.kt` |
| 명령 생성 | `backend/controller/*Controller.kt` |
| 상태 파싱 | `backend/algorithm/icd/` |

### Frontend
| 기능 | 파일 |
|------|------|
| 상태 표시 | `frontend/stores/icdStore.ts` |
| 제어 UI | `frontend/pages/mode/*Page.vue` |

## 안전 제한

### 소프트웨어 리밋
```kotlin
object AntennaLimits {
    const val AZ_MIN = 0.0
    const val AZ_MAX = 360.0
    const val EL_MIN = 0.0
    const val EL_MAX = 90.0
    const val TRAIN_MIN = -180.0
    const val TRAIN_MAX = 180.0
}
```

### 속도 제한
- Azimuth: 최대 10°/s
- Elevation: 최대 5°/s
- Train: 최대 15°/s

## 에러 처리

### 주요 에러 코드
| 코드 | 설명 | 조치 |
|-----|------|------|
| 0x01 | 통신 타임아웃 | 연결 재시도 |
| 0x02 | 리밋 초과 | 명령 취소 |
| 0x03 | 모터 에러 | 안전 정지 |

**에러 메시지 상세:** `docs/guides/Hardware_Error_Messages.md`

## 주의사항

- **단위 변환**: API는 라디안, UI는 도(°)
- **Wrap-around**: Azimuth 360° → 0° 처리 필요
- **인터락**: Elevation < 5° 시 경고
- **비상 정지**: 모든 모드에서 즉시 가능해야 함

## 참조

- [위성 추적](satellite-tracking.md)
- [ICD 프로토콜](icd-protocol.md)
- [모드 시스템](mode-system.md)

---

**최종 수정:** 2026-01-14
