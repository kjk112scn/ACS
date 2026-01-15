# Backend 코드 구조 분석

> 분석 일시: 2026-01-15
> 분석 대상: backend/src/main/kotlin/
> 총 파일: 67개 (.kt)
> 총 코드: 33,284줄

## 1. 폴더 구조 (전체 계층도)

```
backend/src/main/kotlin/com/gtlsystems/acs_api/
├── AcsApiApplication.kt                          (애플리케이션 진입점)
│
├── algorithm/                                    (핵심 계산 알고리즘)
│   ├── axislimitangle/
│   │   └── LimitAngleCalculator.kt              (738줄) 축 제한 각도 계산
│   ├── axistransformation/
│   │   └── CoordinateTransformer.kt             (166줄) 좌표 변환
│   ├── elevation/
│   │   └── ElevationCalculator.kt               (257줄) 고도각 계산
│   ├── satellitetracker/
│   │   ├── impl/
│   │   │   ├── OrekitCalculator.kt              (627줄) Orekit 위성 계산
│   │   │   └── OrekitCalcuatorTest.kt           (595줄) 테스트
│   │   ├── model/
│   │   │   └── SatelliteTrackData.kt            (48줄)
│   │   └── processor/
│   │       ├── SatelliteTrackingProcessor.kt    (1,387줄) 추적 프로세서
│   │       └── model/ProcessedTrackingData.kt   (50줄)
│   └── suntrack/
│       ├── impl/
│       │   ├── SPACalculator.kt                 (351줄) SPA 알고리즘
│       │   ├── Grena3Calculator.kt              (89줄) Grena3 알고리즘
│       │   └── SolarOrekitCalculator.kt         (890줄) Orekit 태양 계산
│       ├── interfaces/
│       │   └── SunPositionCalculator.kt         (38줄) 인터페이스
│       └── model/
│           └── SunTrackData.kt                  (38줄)
│
├── config/                                      (설정 및 초기화)
│   ├── CorsConfig.kt                            (58줄)
│   ├── GlobalExceptionHandler.kt                (74줄)
│   ├── Language.kt                              (7줄)
│   ├── OpenApiConfiguration.kt                  (193줄)
│   ├── OrekitConfig.kt                          (207줄) Orekit 초기화
│   ├── PerformanceFilter.kt                     (31줄)
│   ├── ThreadManager.kt                         (586줄) 쓰레드 관리
│   └── WebSocketConfig.kt                       (26줄)
│
├── controller/                                  (REST API)
│   ├── icd/
│   │   └── ICDController.kt                     (710줄)
│   ├── mode/
│   │   ├── EphemerisController.kt               (1,091줄)
│   │   ├── PassScheduleController.kt            (1,557줄)
│   │   └── SunTrackController.kt                (65줄)
│   ├── system/
│   │   ├── HardwareErrorLogController.kt        (128줄)
│   │   ├── LoggingController.kt                 (420줄)
│   │   ├── PerformanceController.kt             (473줄)
│   │   └── settings/SettingsController.kt       (788줄)
│   └── websocket/
│       └── PushDataController.kt                (762줄)
│
├── dto/                                         (데이터 전송 객체)
│   ├── request/settings/
│   │   └── SettingsUpdateRequest.kt             (9줄)
│   └── response/settings/
│       ├── SettingsResponse.kt                  (25줄)
│       └── SettingsHistoryResponse.kt           (23줄)
│
├── event/                                       (이벤트 시스템)
│   ├── ACSEvent.kt                              (50줄)
│   ├── ACSEventBus.kt                           (92줄) Reactor 기반
│   └── settings/SettingsChangedEvent.kt         (12줄)
│
├── model/                                       (도메인 모델)
│   ├── GlobalData.kt                            (90줄) 전역 상태
│   ├── PushData.kt                              (109줄) WebSocket 데이터
│   └── SystemInfo.kt                            (78줄)
│
├── openapi/                                     (OpenAPI 문서)
│   ├── EphemerisApiDescriptions.kt              (635줄)
│   ├── ICDApiDescriptions.kt                    (581줄)
│   ├── OpenApiUtils.kt                          (76줄)
│   ├── PassScheduleApiDescriptions.kt           (328줄)
│   ├── SettingsApiDescriptions.kt               (310줄)
│   └── SunTrackApiDescriptions.kt               (137줄)
│
├── repository/                                  (데이터 접근)
│   └── interfaces/settings/
│       ├── SettingsRepository.kt                (34줄)
│       └── SettingsHistoryRepository.kt         (12줄)
│
├── service/                                     (비즈니스 로직)
│   ├── InitService.kt                           (52줄)
│   ├── datastore/
│   │   └── DataStoreService.kt                  (621줄)
│   ├── hardware/
│   │   ├── ErrorMessageConfig.kt
│   │   └── HardwareErrorLogService.kt           (624줄)
│   ├── icd/
│   │   └── ICDService.kt                        (2,788줄) ⭐ 핵심
│   ├── mode/
│   │   ├── EphemerisService.kt                  (5,057줄) ⭐ 최대
│   │   ├── PassScheduleService.kt               (3,846줄) ⭐ 핵심
│   │   └── SunTrackService.kt                   (979줄)
│   ├── system/
│   │   ├── BatchStorageManager.kt               (313줄)
│   │   ├── LoggingService.kt                    (302줄)
│   │   └── settings/SettingsService.kt          (1,183줄)
│   ├── udp/
│   │   └── UdpFwICDService.kt                   (1,228줄)
│   └── websocket/
│       └── PushDataService.kt                   (153줄)
│
├── settings/entity/                             (JPA 엔티티)
│   ├── Setting.kt                               (52줄)
│   └── SettingHistory.kt                        (31줄)
│
└── util/                                        (유틸리티)
    ├── ApiDescriptions.kt                       (508줄)
    ├── CRC16Table.kt                            (45줄)
    └── JKUtil.kt                                (289줄)
```

## 2. 패키지별 상세

### 2.1 algorithm/ (8개 파일, 4,316줄)

**역할**: 순수 계산 로직, 외부 의존성 최소화

| 파일 | 줄 수 | 역할 | 의존성 |
|-----|------|------|--------|
| LimitAngleCalculator.kt | 738 | 축 제한 범위 내 각도 계산 | 없음 |
| CoordinateTransformer.kt | 166 | ECI↔Topocentric 좌표 변환 | 수학 라이브러리 |
| ElevationCalculator.kt | 257 | 고도각 계산 | 없음 |
| OrekitCalculator.kt | 627 | **TLE 기반 위성 위치 계산** | Orekit 13.0 |
| SatelliteTrackingProcessor.kt | 1,387 | 추적 데이터 처리/상태관리 | OrekitCalculator |
| SPACalculator.kt | 351 | SPA 알고리즘 태양 위치 | solarpositioning |
| Grena3Calculator.kt | 89 | Grena3 알고리즘 | solarpositioning |
| SolarOrekitCalculator.kt | 890 | Orekit 기반 태양 위치 | Orekit |

### 2.2 controller/ (11개 파일, 5,688줄)

**역할**: REST API 엔드포인트, 입력 검증, 응답 변환

| 파일 | 줄 수 | 엔드포인트 | 주요 기능 |
|-----|------|----------|----------|
| EphemerisController.kt | 1,091 | /api/ephemeris/* | 위성 추적 시작/중지, TLE 설정 |
| PassScheduleController.kt | 1,557 | /api/schedule/* | 패스 정보, 스케줄 관리 |
| ICDController.kt | 710 | /api/icd/* | 하드웨어 명령/상태 |
| SettingsController.kt | 788 | /api/settings/* | 설정 CRUD |
| PushDataController.kt | 762 | /ws/* | WebSocket 연결 |
| SunTrackController.kt | 65 | /api/suntrack/* | 태양 추적 |
| LoggingController.kt | 420 | /api/log/* | 로그 조회 |
| PerformanceController.kt | 473 | /api/perf/* | 성능 메트릭 |
| HardwareErrorLogController.kt | 128 | /api/error/* | 에러 로그 |

### 2.3 service/ (14개 파일, 11,633줄)

**역할**: 비즈니스 로직, 트랜잭션, 외부 시스템 연동

| 파일 | 줄 수 | 역할 | 의존성 |
|-----|------|------|--------|
| **EphemerisService.kt** | **5,057** | 위성 추적 오케스트레이션 | OrekitCalc, ICDService, EventBus |
| **PassScheduleService.kt** | **3,846** | 패스 스케줄 관리 | OrekitCalc, ICDService |
| **ICDService.kt** | **2,788** | ICD 프로토콜 처리 | UdpService, DataStore |
| UdpFwICDService.kt | 1,228 | UDP 펌웨어 통신 | Netty |
| SettingsService.kt | 1,183 | 설정 관리 (DB/RAM) | Repository |
| SunTrackService.kt | 979 | 태양 추적 제어 | SolarCalc, ICDService |
| HardwareErrorLogService.kt | 624 | 하드웨어 에러 로깅 | - |
| DataStoreService.kt | 621 | 메모리 데이터 저장소 | - |
| BatchStorageManager.kt | 313 | 배치 저장 (성능 최적화) | - |
| LoggingService.kt | 302 | 로깅 서비스 | - |
| PushDataService.kt | 153 | WebSocket 메시지 전송 | - |
| InitService.kt | 52 | 애플리케이션 초기화 | - |

## 3. 핵심 파일 (Phase 2 심층 분석 대상)

### 3.1 대형 파일 (500줄+, 17개)

| 순위 | 파일 | 줄 수 | 중요도 | 분석 우선순위 |
|-----|-----|------|--------|-------------|
| 1 | EphemerisService.kt | 5,057 | 🔴 극중요 | P1 |
| 2 | PassScheduleService.kt | 3,846 | 🔴 극중요 | P1 |
| 3 | ICDService.kt | 2,788 | 🔴 극중요 | P1 |
| 4 | PassScheduleController.kt | 1,557 | 🟠 중요 | P2 |
| 5 | SatelliteTrackingProcessor.kt | 1,387 | 🟠 중요 | P2 |
| 6 | UdpFwICDService.kt | 1,228 | 🟠 중요 | P2 |
| 7 | SettingsService.kt | 1,183 | 🟠 중요 | P2 |
| 8 | EphemerisController.kt | 1,091 | 🟠 중요 | P2 |
| 9 | SunTrackService.kt | 979 | 🟡 보통 | P3 |
| 10 | SolarOrekitCalculator.kt | 890 | 🟡 보통 | P3 |

### 3.2 핵심 서비스 의존성

```
EphemerisService (5,057줄)
├── OrekitCalculator
├── SatelliteTrackingProcessor
├── ICDService
├── UdpFwICDService
├── DataStoreService
├── SettingsService
└── ACSEventBus

PassScheduleService (3,846줄)
├── OrekitCalculator
├── SatelliteTrackingProcessor
├── ICDService
└── ACSEventBus

ICDService (2,788줄)
├── DataStoreService
├── ACSEventBus
└── JKUtil
```

## 4. 설정 파일

### application.properties 주요 설정

```properties
# 서버
server.port=8080

# 프로필
spring.profiles.active=no-db  # 또는 with-db

# WebSocket
spring.webflux.netty.max-frame-payload-size=65536

# UDP 설정
udp.firmware.ip=192.168.1.100
udp.firmware.port=5000
udp.receive.interval=30
udp.send.interval=30

# 추적
tracking.default.interval=100
tracking.performance.threshold=50

# 배치 저장
storage.batch.size=1000
storage.save.interval=100
```

### build.gradle.kts 주요 의존성

| 라이브러리 | 버전 | 용도 |
|---------|------|------|
| Kotlin | 1.9.25 | 언어 |
| Spring Boot | 3.4.4 | 프레임워크 |
| Orekit | 13.0.2 | 위성 궤도 계산 |
| solarpositioning | 2.0.3 | 태양 위치 계산 |
| SpringDoc OpenAPI | 2.8.6 | API 문서 |
| PostgreSQL | - | DB (옵션) |

## 5. 아키텍처 특징

### 계층 구조
```
Controller (REST/WebSocket)
    ↓
Service (비즈니스 로직)
    ↓
Algorithm (순수 계산)
    ↓
Repository/DataStore (데이터)
```

### 주요 패턴
- **Reactive**: Spring WebFlux + Project Reactor
- **Event-driven**: ACSEventBus (pub/sub)
- **상태 머신**: EphemerisTrackingState, SunTrackState
- **DI**: 생성자 기반 의존성 주입

### 순환 의존성
**없음** ✅ - 계층간 단방향 의존성 유지

## 6. 특이사항 및 발견점

### 긍정적
1. 계층 분리 명확 (Controller → Service → Algorithm)
2. 순수 함수형 Algorithm 계층
3. 이벤트 기반 느슨한 결합
4. OpenAPI 문서 한/영 이중 지원

### 개선 필요
1. **EphemerisService 과대** (5,057줄) → 분할 검토
2. **DTO 부족** (3개만) → Map<String, Any> 사용 중
3. **테스트 부족** (1개만) → 단위 테스트 추가 필요
4. **주석/문서화** 일부 누락

---

**다음**: Phase 2에서 핵심 서비스 심층 분석
