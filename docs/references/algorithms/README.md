# Algorithms - 알고리즘 문서

> 위성/태양 추적 및 좌표 변환 알고리즘 상세 문서

## 핵심 서비스

### EphemerisService (위성 궤도 추적)

**파일**: `backend/src/.../service/mode/EphemerisService.kt` (약 5,000줄)

| 기능 | 설명 |
|------|------|
| TLE 궤도 계산 | Orekit 기반 위성 위치 계산 |
| 3축 변환 | Train/Tilt 적용 좌표 변환 |
| Keyhole 처리 | 천정 통과 시 추적 최적화 |
| 상태머신 | IDLE → PREPARING → WAITING → TRACKING → COMPLETED |

**상세 문서**: [EphemerisService.md](EphemerisService.md)

### 좌표 변환 체계

```
OrekitCalculator (순수 2축)
        ↓
SatelliteTrackingProcessor (3축 변환)
        ↓
CoordinateTransformer (최종 좌표)
        ↓
LimitAngleCalculator (±270° 제한)
```

## 문서 목록

| 문서 | 설명 | 관련 코드 |
|------|------|----------|
| [EphemerisService.md](EphemerisService.md) | 위성 추적 서비스 전체 | EphemerisService.kt |
| [Train_Angle_Algorithm.md](Train_Angle_Algorithm.md) | Train 각도 계산 로직 | CoordinateTransformer.kt |
| [Antenna_Structure_And_Train_Angle_Concept.md](Antenna_Structure_And_Train_Angle_Concept.md) | 안테나 구조 개념 | - |

## 핵심 알고리즘

### 1. TLE 기반 궤도 예측

```kotlin
// Orekit TLE Propagator 사용
val tle = TLE(line1, line2)
val propagator = TLEPropagator.selectExtrapolator(tle)
val pv = propagator.propagate(targetDate)
```

### 2. Keyhole 감지

- `MaxAzRate` > 임계값 시 Keyhole 발생
- Train 각도 조정으로 추적 연속성 확보

### 3. 좌표 변환 DataType

| DataType | 설명 |
|----------|------|
| `original` | 순수 2축 (Orekit) |
| `axis_transformed` | 3축 변환 (Train=0) |
| `final_transformed` | 최종 변환 |
| `keyhole_optimized_final_transformed` | Keyhole 최적화 |

## 관련 문서

- [ICDService](../protocols/ICDService.md) - 펌웨어 통신 프로토콜
- [SYSTEM_OVERVIEW](../architecture/SYSTEM_OVERVIEW.md) - 시스템 아키텍처

## 주의사항

- **각도 단위**: 내부 라디안, 표시 도(°)
- **시간대**: 내부 UTC, 표시 로컬
- **Orekit 초기화**: `orekit-data` 경로 필수

---

**최종 업데이트**: 2026-01-07
