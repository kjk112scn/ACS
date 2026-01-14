# 위성 추적 (Satellite Tracking)

> ACS 시스템의 핵심 도메인: 위성의 위치를 계산하고 안테나를 지향

## 개요

```
TLE 데이터 → Orekit 궤도 전파 → 위성 위치 계산 → 안테나 지향각 → 안테나 제어
```

## 핵심 개념

### TLE (Two-Line Element)
위성의 궤도 요소를 2줄로 표현한 형식

```
ISS (ZARYA)
1 25544U 98067A   24001.00000000  .00000000  00000-0  00000-0 0  0000
2 25544  51.6400 000.0000 0000000  00.0000 000.0000 00.00000000000000
```

**주요 요소:**
- NORAD ID: 위성 식별자 (25544 = ISS)
- 경사각(Inclination): 궤도면과 적도면의 각도
- 이심률(Eccentricity): 궤도의 타원 정도
- RAAN: 승교점 적경
- 평균 근점 이각: 궤도상 위치

### Orekit 라이브러리
Java 기반 우주 역학 라이브러리 (v13.0 사용)

**핵심 클래스:**
```kotlin
// 궤도 전파
TLEPropagator.selectExtrapolator(tle)

// 지상국 기준 좌표
TopocentricFrame(earthFrame, groundStation, "ACS")

// 시간 처리
AbsoluteDate(date, TimeScalesFactory.getUTC())
```

### 좌표계

| 좌표계 | 설명 | 용도 |
|-------|------|------|
| ECI (J2000) | 지구 중심 관성 좌표계 | 궤도 계산 |
| ECEF (ITRF) | 지구 고정 좌표계 | 지상 위치 |
| Topocentric | 지상국 기준 좌표계 | 안테나 지향 |

### 지향각 계산

```
Azimuth (방위각): 북쪽 기준 시계방향 각도 (0-360°)
Elevation (고도각): 수평면 기준 위쪽 각도 (0-90°)
```

**계산 흐름:**
1. TLE로 위성 궤도 정의
2. 특정 시각의 위성 위치 계산 (ECI)
3. 지상국 좌표계로 변환 (Topocentric)
4. Azimuth, Elevation 추출

## 코드 위치

| 기능 | 파일 |
|------|------|
| 궤도 계산 | `backend/algorithm/ephemeris/` |
| TLE 파싱 | `backend/service/EphemerisService.kt` |
| 안테나 지향 | `backend/service/TrackingService.kt` |

## 주요 서비스

### EphemerisService
- TLE 파싱 및 저장
- Orekit 기반 궤도 전파
- 패스 예측 (가시 시간 계산)

### TrackingService
- 실시간 위성 위치 계산
- 안테나 지향각 계산
- 추적 명령 생성

## 관련 모드

- **EphemerisDesignation**: 특정 위성 지정 추적
- **PassSchedule**: 위성 패스 스케줄 기반 자동 추적

## 주의사항

- **각도 단위**: 내부 계산은 라디안, UI 표시는 도(°)
- **시간대**: 내부 UTC, 표시 로컬 시간
- **Orekit 초기화**: orekit-data 경로 필수 설정
- **TLE 유효기간**: 일반적으로 1-2주, 정밀도 저하

## 참조

- [Orekit 공식 문서](https://www.orekit.org/)
- [Celestrak TLE](https://celestrak.org/)
- [안테나 제어](antenna-control.md)
- [모드 시스템](mode-system.md)

---

**최종 수정:** 2026-01-14
