# EphemerisService

## 개요

`EphemerisService`는 ACS(Antenna Control System)의 핵심 서비스로, **위성 궤도 추적**을 담당합니다. TLE(Two-Line Element) 데이터를 기반으로 위성의 위치를 계산하고, 안테나 제어를 위한 추적 명령을 생성하여 펌웨어에 전송합니다.

**파일 위치**: `backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`

**파일 크기**: 약 5,000줄 (대형 서비스 - 리팩토링 권장)

## 핵심 기능

1. **위성 궤도 계산**: TLE 데이터를 사용하여 위성의 Azimuth/Elevation 좌표 계산
2. **좌표 변환**: 3축 변환 (Tilt 보정) 및 방위각 제한 (+-270도) 적용
3. **Keyhole 처리**: 천정 통과(Keyhole) 발생 시 Train 축 활용하여 추적 최적화
4. **실시간 추적**: 상태머신 기반 자동 추적 (PREPARING -> WAITING -> TRACKING -> COMPLETED)
5. **펌웨어 통신**: UDP를 통한 추적 데이터 전송 (ICD 프로토콜 12.1, 12.2, 12.3)
6. **정지궤도 추적**: GEO 위성 고정 추적 지원

## 아키텍처

```
                    ┌────────────────────────────┐
                    │      EphemerisService      │
                    │    (상태머신 / 추적 제어)   │
                    └─────────────┬──────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
        ▼                         ▼                         ▼
┌───────────────┐      ┌──────────────────┐      ┌──────────────────┐
│OrekitCalculator│     │SatelliteTracking │      │ CoordinateTransf │
│  (순수 2축)    │     │   Processor      │      │    ormer         │
│  위성 위치 계산│     │ (3축 변환 처리)   │      │  (좌표 변환)      │
└───────────────┘      └──────────────────┘      └──────────────────┘
        │                         │                         │
        └─────────────────────────┼─────────────────────────┘
                                  │
                    ┌─────────────┴──────────────┐
                    │                            │
                    ▼                            ▼
           ┌───────────────┐           ┌────────────────┐
           │UdpFwICDService│           │DataStoreService│
           │  (UDP 통신)   │           │  (상태 저장)    │
           └───────────────┘           └────────────────┘
```

## 주요 메서드

### 궤도 계산 및 추적 시작

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| `generateEphemerisDesignationTrackSync` | tleLine1, tleLine2, satelliteName? | Pair<List, List> | TLE 기반 궤도 계산 (동기) |
| `generateEphemerisDesignationTrackAsync` | tleLine1, tleLine2, satelliteName? | Mono<Pair> | TLE 기반 궤도 계산 (비동기) |
| `startEphemerisTracking` | mstId, detailId | Unit | 위성 추적 시작 |
| `stopEphemerisTracking` | - | Unit | 위성 추적 중지 |
| `startGeostationaryTracking` | tleLine1, tleLine2 | Unit | 정지궤도 위성 추적 |

### 펌웨어 데이터 전송 (ICD 프로토콜)

| 메서드 | 프로토콜 | 설명 |
|--------|---------|------|
| `sendHeaderTrackingData` | 12.1 (TT) | 추적 헤더 정보 (AOS/LOS 시간) |
| `sendInitialTrackingData` | 12.2 (TM) | 초기 제어 명령 (최초 50개 포인트) |
| `sendAdditionalTrackingData` | 12.3 (TR) | 추가 데이터 응답 |
| `handleEphemerisTrackingDataRequest` | - | 펌웨어 요청 처리 |

### 좌표 계산 및 변환

| 메서드 | 설명 |
|--------|------|
| `getCurrentSatellitePosition` | 현재 시간 위성 좌표 (2축) |
| `getCurrentGeostationaryPositionWith3AxisTransform` | 3축 변환 적용 좌표 |
| `calculateAxisTransform` | 축 변환 계산 |

### 데이터 조회

| 메서드 | 설명 |
|--------|------|
| `getAllEphemerisTrackMst` | 전체 MST 데이터 |
| `getAllEphemerisTrackMstMerged` | Original + FinalTransformed 병합 |
| `getEphemerisTrackDtlByMstIdAndDetailId` | Keyhole 여부에 따른 DTL |
| `isTracking` | 추적 상태 확인 |
| `getCurrentTrackingPass` | 현재 추적 중인 패스 정보 |

## 의존성

### 알고리즘 라이브러리

| 의존성 | 역할 |
|--------|------|
| **OrekitCalculator** | Orekit 기반 순수 2축 위성 위치 계산 |
| **SatelliteTrackingProcessor** | 3축 변환 및 Keyhole 처리 |
| **CoordinateTransformer** | Train/Tilt 적용 좌표 변환 |
| **LimitAngleCalculator** | 방위각 +-270도 제한 변환 |

### 서비스 의존성

| 의존성 | 역할 |
|--------|------|
| **UdpFwICDService** | UDP 통신으로 펌웨어 명령 전송 |
| **DataStoreService** | 안테나 위치/상태 저장 |
| **SettingsService** | 설정값 (Tilt, 각도 제한 등) |
| **ACSEventBus** | 이벤트 기반 통신 |
| **ThreadManager** | 추적 타이머 스레드 관리 |

## 내부 구조

### 상태머신 (TrackingState)

```kotlin
enum class TrackingState {
    IDLE,           // 대기 (추적 비활성)
    PREPARING,      // 준비 중 (Train 이동 + 안정화 + Az/El 이동)
    WAITING,        // 시작 대기 (12.1 헤더 전송 완료)
    TRACKING,       // 추적 중 (12.2 초기 데이터, 실시간 추적)
    COMPLETED,      // 완료
    ERROR           // 오류
}
```

### 데이터 타입 (DataType)

| DataType | 설명 | Train 각도 |
|----------|------|-----------|
| `original` | 순수 2축 계산 | N/A |
| `axis_transformed` | 3축 변환 (Train=0) | 0도 |
| `final_transformed` | 최종 변환 | 0도 |
| `keyhole_axis_transformed` | Keyhole 3축 변환 | 계산값 |
| `keyhole_final_transformed` | Keyhole 최종 | 계산값 |
| `keyhole_optimized_final_transformed` | Keyhole 최적화 | 최적값 |

### MST/DTL 데이터 구조

**MST (마스터 - 패스 정보)**
- MstId, DetailId, SatelliteName
- StartTime, EndTime, MaxElevation
- IsKeyhole, RecommendedTrainAngle
- MaxAzRate, MaxElRate

**DTL (상세 - 시간별 좌표)**
- MstId, DetailId, Time
- Azimuth, Elevation, Range, Altitude

## 사용 예시

```kotlin
// 1. TLE 데이터로 궤도 계산
val (mstList, dtlList) = ephemerisService.generateEphemerisDesignationTrackSync(
    tleLine1 = "1 25544U 98067A ...",
    tleLine2 = "2 25544  51.6416 ...",
    satelliteName = "ISS (ZARYA)"
)

// 2. 특정 패스 추적 시작
ephemerisService.startEphemerisTracking(mstId = 1L, detailId = 0)

// 3. 추적 상태 확인
val isTracking = ephemerisService.isTracking()

// 4. 추적 중지
ephemerisService.stopEphemerisTracking()
```

## 주의사항

### 각도 단위
- **내부 계산**: 라디안 (Orekit)
- **저장/표시**: 도 (Degree)

### 시간대
- **내부 처리**: UTC
- **UI 표시**: 로컬 시간으로 변환

### Keyhole 처리
- `MaxAzRate`가 임계값 이상이면 Keyhole 발생
- Keyhole 발생 시 `keyhole_optimized_final_transformed` 사용

### 타이머 주기
- 모드 타이머: 100ms
- UDP 수신: 10ms
- WebSocket 전송: 30ms

## 관련 문서

- [시스템 통합 개요](../architecture/SYSTEM_OVERVIEW.md)
- [Train 각도 알고리즘](./Train_Angle_Algorithm.md)
- [안테나 구조 개념](./Antenna_Structure_And_Train_Angle_Concept.md)

---

**문서 버전**: 1.0.0
**최종 업데이트**: 2026-01-07
