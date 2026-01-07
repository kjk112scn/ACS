# Backend Architecture Refactoring (백엔드 리팩토링) 계획서

> 상위 문서: [Architecture_Refactoring_plan.md](./Architecture_Refactoring_plan.md)

---

> ⚠️ **Phase 역할 분리 안내**
>
> 이 문서의 Phase 0-3은 **BE 세부 작업 순서**입니다.
> 메인 계획서의 Phase 1-4와는 다른 역할입니다.
>
> | 메인 계획서 | 이 문서 |
> |------------|---------|
> | Phase 2: BE 리팩토링 | Phase 0-3 전체가 해당 |
>
> 핵심 리팩토링(SatelliteTrackingEngine)은 [RFC_SatelliteTrackingEngine.md](./RFC_SatelliteTrackingEngine.md) 참조

---

## 현황 분석

### 통계

| 항목 | 수치 |
|------|------|
| 소스 파일 | 68개 |
| 테스트 파일 | 1개 (1.5%) |
| 거대 파일 (300줄+) | 29개 |
| 총 코드 줄 수 | 32,648줄 |

### 잘 설계된 부분 (유지)

| 구성 요소 | 설명 |
|----------|------|
| ThreadManager | 하드웨어 자동 감지, 성능 등급별 스레드 풀 관리 |
| 실시간 통신 | UDP(10ms) → BE → WebSocket(30ms) → FE 파이프라인 |
| 우선순위 체계 | CRITICAL(UDP) > HIGH(WebSocket) > NORMAL(Tracking) > LOW(Batch) |
| 계층 분리 | controller → service → repository/model 명확함 |
| 도메인별 구성 | mode, system, icd 등 도메인별 정리 |
| 알고리즘 분리 | algorithm 패키지가 별도로 분리됨 |

---

## 1. 현재 폴더 구조

```
com/gtlsystems/acs_api/
├── AcsApiApplication.kt (11줄)
├── algorithm/                    # 알고리즘 계산 패키지
│   ├── axislimitangle/
│   │   └── LimitAngleCalculator.kt (738줄)
│   ├── axistransformation/
│   │   └── CoordinateTransformer.kt (166줄)
│   ├── elevation/
│   │   └── ElevationCalculator.kt (257줄)
│   ├── satellitetracker/
│   │   ├── impl/
│   │   │   ├── OrekitCalculator.kt (627줄)
│   │   │   └── OrekitCalcuatorTest.kt (595줄) ⚠️ 테스트 위치 오류
│   │   ├── model/
│   │   │   └── SatelliteTrackData.kt (48줄)
│   │   └── processor/
│   │       ├── SatelliteTrackingProcessor.kt (1,387줄)
│   │       └── model/
│   │           └── ProcessedTrackingData.kt (50줄)
│   └── suntrack/
│       ├── impl/
│       │   ├── Grena3Calculator.kt (89줄)
│       │   ├── SPACalculator.kt (351줄)
│       │   └── SolarOrekitCalculator.kt (890줄)
│       ├── interfaces/
│       │   └── SunPositionCalculator.kt (38줄)
│       └── model/
│           └── SunTrackData.kt (38줄)
├── config/
│   ├── CorsConfig.kt (58줄)
│   ├── GlobalExceptionHandler.kt (74줄)
│   ├── Language.kt (7줄)
│   ├── OpenApiConfiguration.kt (193줄)
│   ├── OrekitConfig.kt (207줄)
│   ├── PerformanceFilter.kt (31줄)
│   ├── ThreadManager.kt (586줄)
│   └── WebSocketConfig.kt (26줄)
├── controller/
│   ├── icd/
│   │   └── ICDController.kt (710줄)
│   ├── mode/
│   │   ├── EphemerisController.kt (1,091줄)
│   │   ├── PassScheduleController.kt (2,021줄) ⚠️ 거대
│   │   └── SunTrackController.kt (65줄)
│   ├── system/
│   │   ├── HardwareErrorLogController.kt (128줄)
│   │   ├── LoggingController.kt (420줄)
│   │   ├── PerformanceController.kt (303줄)
│   │   └── settings/
│   │       └── SettingsController.kt (788줄)
│   └── websocket/
│       └── PushDataController.kt (762줄)
├── dto/
│   ├── request/settings/
│   │   └── SettingsUpdateRequest.kt (9줄)
│   └── response/settings/
│       ├── SettingsHistoryResponse.kt (23줄)
│       └── SettingsResponse.kt (25줄)
├── event/
│   ├── ACSEvent.kt (50줄)
│   ├── ACSEventBus.kt (92줄)
│   └── settings/
│       └── SettingsChangedEvent.kt (12줄)
├── model/
│   ├── GlobalData.kt (90줄) ⚠️ 싱글톤 남용
│   ├── PushData.kt (109줄)
│   └── SystemInfo.kt (78줄)
├── openapi/
│   ├── EphemerisApiDescriptions.kt (635줄)
│   ├── ICDApiDescriptions.kt (581줄)
│   ├── OpenApiUtils.kt (76줄)
│   ├── PassScheduleApiDescriptions.kt (328줄)
│   ├── SettingsApiDescriptions.kt (310줄)
│   └── SunTrackApiDescriptions.kt (137줄)
├── repository/interfaces/settings/
│   ├── SettingsHistoryRepository.kt (12줄)
│   └── SettingsRepository.kt (34줄)
├── service/
│   ├── InitService.kt (52줄)
│   ├── datastore/
│   │   └── DataStoreService.kt (646줄)
│   ├── hardware/
│   │   ├── ErrorMessageConfig.kt (151줄) ⚠️ config 폴더로 이동 필요
│   │   └── HardwareErrorLogService.kt (624줄)
│   ├── icd/
│   │   └── ICDService.kt (2,788줄) ⚠️ 거대
│   ├── mode/
│   │   ├── EphemerisService.kt (4,986줄) ⚠️ 최대 파일
│   │   ├── PassScheduleService.kt (2,896줄) ⚠️ 거대
│   │   └── SunTrackService.kt (979줄)
│   ├── system/
│   │   ├── BatchStorageManager.kt (313줄)
│   │   ├── LoggingService.kt (302줄)
│   │   └── settings/
│   │       └── SettingsService.kt (1,183줄)
│   ├── udp/
│   │   └── UdpFwICDService.kt (1,294줄)
│   └── websocket/
│       └── PushDataService.kt (153줄)
├── settings/entity/
│   ├── Setting.kt (52줄)
│   └── SettingHistory.kt (31줄)
└── util/
    ├── ApiDescriptions.kt (508줄) ⚠️ openapi로 이동 필요
    ├── CRC16Table.kt (45줄)
    └── JKUtil.kt (289줄)
```

---

## 2. 구조적 문제점

### 2.1 애매한 위치의 파일

| 파일 | 현재 위치 | 문제점 | 개선안 |
|------|----------|--------|--------|
| `OrekitCalcuatorTest.kt` | `algorithm/satellitetracker/impl/` | ❌ 테스트가 프로덕션 코드에 위치 | `src/test/kotlin/` 이동 |
| `temp_original.txt` | `backend/` 루트 | ❌ 임시 파일, 용도 불명 | 삭제 또는 .gitignore |
| `ErrorMessageConfig.kt` | `service/hardware/` | ⚠️ 설정이 service에 위치 | `config/` 이동 |
| `ApiDescriptions.kt` | `util/` | ⚠️ API 설명이 유틸리티에 위치 | `openapi/descriptions/` 이동 |

### 2.2 API 문서 파일 분산

```
현재 상태 (7개 파일에 분산):
├── openapi/
│   ├── EphemerisApiDescriptions.kt (635줄)
│   ├── ICDApiDescriptions.kt (581줄)
│   ├── PassScheduleApiDescriptions.kt (328줄)
│   ├── SettingsApiDescriptions.kt (310줄)
│   ├── SunTrackApiDescriptions.kt (137줄)
│   └── OpenApiUtils.kt (76줄)
└── util/
    └── ApiDescriptions.kt (508줄)  ← 분리되어 있음

개선안:
openapi/
├── descriptions/
│   ├── EphemerisApiDescriptions.kt
│   ├── ICDApiDescriptions.kt
│   ├── PassScheduleApiDescriptions.kt
│   ├── SettingsApiDescriptions.kt
│   ├── SunTrackApiDescriptions.kt
│   └── CommonApiDescriptions.kt (기존 util/ApiDescriptions.kt)
├── OpenApiConfiguration.kt
└── OpenApiUtils.kt
```

### 2.3 설정 관리 방식 혼재

```
현재 4가지 방식 혼재:
1. 프로퍼티 파일: application.properties (117줄)
2. 싱글톤 객체: model/GlobalData.kt (시간, 각도, 오프셋)
3. JPA 엔티티: settings/entity/Setting.kt
4. 객체 기반: service/hardware/ErrorMessageConfig.kt

문제점:
- 설정 소스가 분산
- 우선순위 불명확
- 런타임 변경 전략 불명확
```

### 2.4 테스트 파일 오타

```
현재: OrekitCalcuatorTest.kt  ← 'l' 누락
수정: OrekitCalculatorTest.kt
```

---

## 3. 거대 파일 목록 (300줄 이상)

### 3.1 Services (핵심 - 분해 필요)

| 파일 | 줄 수 | 분해 방향 |
|------|-------|----------|
| `EphemerisService.kt` | 5,060 | 추적/계산/명령/상태 분리 |
| `PassScheduleService.kt` | 2,896 | 상태머신/스케줄러/계산 분리 |
| `ICDService.kt` | 2,788 | 명령별 분리 |
| `UdpFwICDService.kt` | 1,294 | 송신/수신/파싱 분리 |
| `SettingsService.kt` | 1,183 | 도메인별 분리 |
| `SunTrackService.kt` | 979 | 위치계산/각도계산/상태/명령 분리 |
| `DataStoreService.kt` | 646 | 도메인별 분리 |
| `HardwareErrorLogService.kt` | 624 | 에러매핑 외부화 |

### 3.2 Controllers

| 파일 | 줄 수 | 개선 방향 |
|------|-------|----------|
| `PassScheduleController.kt` | 2,021 | 기능별 분리 검토 |
| `EphemerisController.kt` | 1,091 | 기능별 분리 검토 |
| `SettingsController.kt` | 788 | 도메인별 분리 검토 |
| `PushDataController.kt` | 762 | OK (WebSocket 브로드캐스트) |
| `ICDController.kt` | 710 | 명령별 분리 검토 |

### 3.3 Algorithms

| 파일 | 줄 수 | 상태 |
|------|-------|------|
| `SatelliteTrackingProcessor.kt` | 1,387 | 분해 필요 |
| `SolarOrekitCalculator.kt` | 890 | 검토 필요 |
| `LimitAngleCalculator.kt` | 738 | OK (단일 책임) |
| `OrekitCalculator.kt` | 627 | 캐싱 적용 필요 |

---

## 4. Phase 0: 폴더 구조 정리

### Task 0.1: 테스트 파일 이동

**작업 내용**:
```
현재: algorithm/satellitetracker/impl/OrekitCalcuatorTest.kt
     ↓
이동: src/test/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/OrekitCalculatorTest.kt
     (파일명 오타 수정 포함)
```

### Task 0.2: 설정 파일 위치 정리

**작업 내용**:
```
이동: service/hardware/ErrorMessageConfig.kt → config/ErrorMessageConfig.kt
이동: util/ApiDescriptions.kt → openapi/descriptions/CommonApiDescriptions.kt
```

### Task 0.3: 임시 파일 정리

**작업 내용**:
```
삭제: backend/temp_original.txt
또는: .gitignore에 추가
```

---

## 5. Phase 1: 에러 매핑 YAML 외부화

### Task 1.1: error-mappings.yml 생성

**목표**: `HardwareErrorLogService.kt`의 하드코딩된 에러 매핑을 설정 파일로 분리

**현재 문제**:
```kotlin
// HardwareErrorLogService.kt - getErrorMappings() 함수 128줄
private fun getErrorMappings(bitType: String): Map<Int, ErrorConfig> {
    return when (bitType) {
        "mainBoardProtocolStatusBits" -> mapOf(
            0 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_ELEVATION_ERROR", "Elevation Protocol"),
            1 to ErrorConfig("PROTOCOL", "ERROR", "PROTOCOL_AZIMUTH_ERROR", "Azimuth Protocol"),
            // ... 14개 타입 × 8개 비트
        )
    }
}
```

**해결책**:
```yaml
# backend/src/main/resources/config/error-mappings.yml
error-mappings:
  mainBoardProtocolStatusBits:
    0:
      category: PROTOCOL
      severity: ERROR
      errorKey: PROTOCOL_ELEVATION_ERROR
      component: Elevation Protocol
    1:
      category: PROTOCOL
      severity: ERROR
      errorKey: PROTOCOL_AZIMUTH_ERROR
      component: Azimuth Protocol
    # ...

  mainBoardStatusBits:
    0:
      category: POWER
      severity: CRITICAL
      errorKey: POWER_SURGE_PROTECTOR
      component: Surge Protector
    # ...
```

```kotlin
// ErrorMappingConfig.kt (신규)
@Configuration
@ConfigurationProperties(prefix = "error-mappings")
class ErrorMappingConfig {
    var mappings: Map<String, Map<Int, ErrorConfig>> = emptyMap()
}

// HardwareErrorLogService.kt (수정)
@Service
class HardwareErrorLogService(
    private val errorMappingConfig: ErrorMappingConfig
) {
    private fun getErrorMappings(bitType: String): Map<Int, ErrorConfig> {
        return errorMappingConfig.mappings[bitType] ?: emptyMap()
    }
}
```

**예상 효과**:
- 코드 128줄 → 10줄
- 에러 추가/수정 시 코드 변경 없이 YAML만 수정

---

## 6. Phase 2: SunTrackService 분해

### Task 2.1: 서비스 분해

**목표**: 979줄 서비스를 역할별로 분리

**현재 구조**:
```
SunTrackService.kt (979줄)
├── 태양 위치 계산
├── Train 각도 계산
├── 상태 관리
├── UDP 명령 전송
├── 오프셋 처리
└── 로깅
```

**개선 구조**:
```
service/mode/suntrack/
├── SunTrackService.kt (200줄) - 조율자 역할
├── SunPositionCalculator.kt (150줄) - 태양 위치 계산
├── TrainAngleCalculator.kt (200줄) - Train 각도 계산
├── SunTrackStateManager.kt (100줄) - 상태 관리
├── SunTrackCommandSender.kt (100줄) - UDP 명령 전송
└── model/
    ├── SunTrackState.kt - sealed class 상태 정의
    └── TrainAngleResult.kt - 계산 결과 DTO
```

**코드 예시**:
```kotlin
// SunTrackState.kt
sealed class SunTrackState {
    object Idle : SunTrackState()
    data class Initializing(val trainAngle: Double) : SunTrackState()
    data class MovingToPosition(val targetAngle: Double, val currentAngle: Double) : SunTrackState()
    data class Stabilizing(val attempts: Int) : SunTrackState()
    data class Tracking(val sunPosition: SunPosition) : SunTrackState()
    data class Error(val message: String) : SunTrackState()
}

// SunTrackService.kt (조율자)
@Service
class SunTrackService(
    private val positionCalculator: SunPositionCalculator,
    private val trainCalculator: TrainAngleCalculator,
    private val stateManager: SunTrackStateManager,
    private val commandSender: SunTrackCommandSender
) {
    fun startSunTrack(...) {
        val trainAngle = trainCalculator.calculate(...)
        stateManager.transition(SunTrackState.Initializing(trainAngle))
        commandSender.sendTrainCommand(trainAngle)
    }
}
```

**예상 효과**:
- 각 클래스 200줄 이하
- 단일 책임 원칙 준수
- 테스트 용이성 대폭 향상

---

## 7. Phase 3: EphemerisService 분해

### Task 3.1: 서비스 분해

**목표**: 4,986줄 서비스를 도메인별로 분리

**개선 구조**:
```
service/mode/ephemeris/
├── EphemerisService.kt (300줄) - 조율자
├── tracking/
│   ├── SatelliteTracker.kt - 위성 추적 상태 관리
│   ├── TrackingScheduler.kt - 스케줄 관리
│   └── TrackingCommandSender.kt - 명령 전송
├── calculation/
│   ├── PositionCalculator.kt - 위치 계산
│   ├── KeyholeDetector.kt - Keyhole 판정
│   └── PathPredictor.kt - 경로 예측
├── state/
│   ├── TrackingState.kt - sealed class 정의
│   └── TrackingContext.kt - 추적 컨텍스트
└── model/
    ├── Ephemeris.kt
    └── TrackingResult.kt
```

### Task 3.2: PassScheduleService 분해

**목표**: 2,896줄 서비스를 역할별로 분리

**개선 구조**:
```
service/mode/passschedule/
├── PassScheduleService.kt (300줄) - 조율자
├── scheduler/
│   ├── ScheduleManager.kt - 스케줄 관리
│   ├── ScheduleValidator.kt - 스케줄 검증
│   └── ScheduleExecutor.kt - 스케줄 실행
├── state/
│   ├── PassScheduleState.kt - sealed class 정의
│   └── StateTransitionManager.kt - 상태 전이 관리
└── model/
    └── ScheduleResult.kt
```

### Task 3.3: ICDService 분해

**목표**: 2,788줄 서비스를 명령별로 분리

**개선 구조**:
```
service/icd/
├── ICDService.kt (200줄) - 조율자
├── commands/
│   ├── AntennaCommandHandler.kt - 안테나 명령
│   ├── SystemCommandHandler.kt - 시스템 명령
│   └── StatusQueryHandler.kt - 상태 조회
├── protocol/
│   ├── MessageEncoder.kt - 메시지 인코딩
│   ├── MessageDecoder.kt - 메시지 디코딩
│   └── ProtocolValidator.kt - 프로토콜 검증
└── model/
    └── ICDMessage.kt
```

---

## 8. Phase 3: 테스트 커버리지 향상

### Task 3.4: 테스트 작성

**목표**: BE 1.5% → 60%

**우선순위**:
1. 알고리즘 테스트 (위성 위치, 태양 위치 계산)
2. 서비스 단위 테스트 (SunTrackService, EphemerisService)
3. 컨트롤러 통합 테스트

**테스트 예시**:
```kotlin
// SunPositionCalculatorTest.kt
class SunPositionCalculatorTest {

    private val calculator = SunPositionCalculator()

    @Test
    fun `서울에서 하지 정오의 태양 위치 계산`() {
        // Given
        val latitude = 37.5665
        val longitude = 126.9780
        val dateTime = LocalDateTime.of(2024, 6, 21, 12, 0)

        // When
        val result = calculator.calculate(latitude, longitude, dateTime)

        // Then
        assertThat(result.elevation).isGreaterThan(70.0)
        assertThat(result.azimuth).isBetween(170.0, 190.0)
    }
}
```

---

## 9. 권장 폴더 구조 (최종)

```
com/gtlsystems/acs_api/
├── AcsApiApplication.kt
├── algorithm/                    # 순수 계산 알고리즘 (유지)
│   ├── axislimitangle/
│   ├── axistransformation/
│   ├── elevation/
│   ├── satellitetracker/
│   └── suntrack/
├── config/                       # 설정 통합
│   ├── CorsConfig.kt
│   ├── ErrorMessageConfig.kt    ← 이동
│   ├── GlobalExceptionHandler.kt
│   ├── Language.kt
│   ├── OpenApiConfiguration.kt
│   ├── OrekitConfig.kt
│   ├── PerformanceFilter.kt
│   ├── ThreadManager.kt
│   └── WebSocketConfig.kt
├── controller/                   # 유지
│   ├── icd/
│   ├── mode/
│   ├── system/
│   └── websocket/
├── dto/                          # 유지
├── event/                        # 유지
├── model/                        # 유지 (GlobalData 개선 검토)
├── openapi/                      # API 문서 통합
│   ├── descriptions/            ← 신규
│   │   ├── CommonApiDescriptions.kt
│   │   ├── EphemerisApiDescriptions.kt
│   │   ├── ICDApiDescriptions.kt
│   │   ├── PassScheduleApiDescriptions.kt
│   │   ├── SettingsApiDescriptions.kt
│   │   └── SunTrackApiDescriptions.kt
│   ├── OpenApiConfiguration.kt
│   └── OpenApiUtils.kt
├── repository/                   # 유지
├── service/                      # 분해
│   ├── datastore/
│   ├── hardware/
│   ├── icd/                     ← 분해
│   │   ├── ICDService.kt
│   │   ├── commands/
│   │   └── protocol/
│   ├── mode/
│   │   ├── ephemeris/           ← 분해
│   │   ├── passschedule/        ← 분해
│   │   └── suntrack/            ← 분해
│   ├── system/
│   ├── udp/
│   └── websocket/
├── settings/                     # 유지
└── util/                         # ApiDescriptions 이동 후
    ├── CRC16Table.kt
    └── JKUtil.kt
```

---

## 10. 완료 기준

### Phase 0 완료 기준
- [ ] `OrekitCalcuatorTest.kt` 이동 및 파일명 수정
- [ ] `ErrorMessageConfig.kt` → `config/` 이동
- [ ] `ApiDescriptions.kt` → `openapi/descriptions/` 이동
- [ ] `temp_original.txt` 정리
- [ ] 빌드 성공

### Phase 1 완료 기준
- [ ] `error-mappings.yml` 생성
- [ ] `ErrorMappingConfig.kt` 생성
- [ ] `HardwareErrorLogService.kt` 수정
- [ ] 기존 에러 감지 기능 정상 동작 확인

### Phase 2 완료 기준
- [ ] SunTrackService 5개 클래스로 분리
- [ ] 각 클래스 200줄 이하
- [ ] 분리된 서비스 테스트 작성
- [ ] 태양 추적 기능 정상 동작 확인

### Phase 3 완료 기준
- [ ] EphemerisService 분리 완료
- [ ] PassScheduleService 분리 완료
- [ ] ICDService 분리 완료
- [ ] 백엔드 테스트 커버리지 60% 달성
- [ ] 모든 기능 정상 동작 확인

---

## 11. 리스크 평가

| Phase | 리스크 | 영향도 | 대응 |
|-------|--------|--------|------|
| Phase 0 | 🟢 낮음 | 파일 이동만 | import 경로 수정 |
| Phase 1 | 🟢 낮음 | 에러 감지만 영향 | 설정 검증 |
| Phase 2 | 🟡 중간 | 태양 추적 영향 | 단계별 적용, 테스트 |
| Phase 3 | 🔴 높음 | 핵심 기능 영향 | 충분한 테스트, 롤백 준비 |

---

## 12. 롤백 계획

각 Phase는 독립적인 Git 브랜치에서 작업:
- `feature/phase0-folder-structure`
- `feature/phase1-error-mapping`
- `feature/phase2-suntrack-refactor`
- `feature/phase3-ephemeris-refactor`
- `feature/phase3-passschedule-refactor`
- `feature/phase3-icd-refactor`

문제 발생 시 해당 브랜치만 롤백하여 다른 개선 사항 유지

---

---

## 13. 추후 작업: 실시간 DB 저장 전략

> **적용 시점**: DB 연계 구축 후 구현
> **목적**: 100ms 간격 readStatus 데이터를 실시간 성능 저하 없이 DB에 저장

### 13.1 핵심 원칙

**두 개의 독립적인 경로로 분리**:

```
경로 1: 실시간 제어 (절대 지연 없음)
UDP(10ms) → BE 메모리 처리 → WebSocket(30ms) → FE
                  │
                  ↓ (복사만, 블로킹 없음)
경로 2: DB 저장 (별도 스레드)
            버퍼 큐 → 1초 배치 → DB INSERT
```

- 실시간 경로와 DB 저장 경로 완전 분리
- DB 작업이 실시간 통신에 절대 영향 없음
- buffer.offer()는 O(1) 나노초 연산

### 13.2 비동기 배치 저장 서비스

```kotlin
@Service
class RealtimeDataStorageService(
    private val jdbcTemplate: JdbcTemplate
) {
    // 메모리 버퍼 (락-프리 큐)
    private val buffer = ConcurrentLinkedQueue<ReadStatusData>()

    /**
     * 실시간 데이터 수신 (즉시 반환, 블로킹 없음)
     * UDP 수신 스레드에서 호출 - 절대 지연되면 안됨
     */
    fun addData(data: ReadStatusData) {
        buffer.offer(data)  // O(1), 논블로킹
    }

    /**
     * 1초마다 배치 저장 (별도 스레드에서 실행)
     * 실시간 경로와 완전히 독립적
     */
    @Scheduled(fixedRate = 1000)
    fun flushToDatabase() {
        val batch = mutableListOf<ReadStatusData>()

        // 버퍼에서 최대 100개 꺼내기 (1초분 = 10개 예상)
        repeat(100) {
            buffer.poll()?.let { batch.add(it) } ?: return@repeat
        }

        if (batch.isNotEmpty()) {
            batchInsert(batch)  // 한 번에 INSERT
        }
    }

    private fun batchInsert(data: List<ReadStatusData>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO read_status (timestamp, azimuth, elevation, ...) VALUES (?, ?, ?, ...)",
            data.map { arrayOf(it.timestamp, it.azimuth, it.elevation, /* ... */) }
        )
    }
}
```

### 13.3 하이브리드 저장 (정상 종료 시 데이터 보존)

```kotlin
@Service
class HybridStorageService(
    private val jdbcTemplate: JdbcTemplate
) {
    private val buffer = ConcurrentLinkedQueue<ReadStatusData>()

    /**
     * 실시간 데이터 추가 (오버플로우 보호 포함)
     */
    fun addData(data: ReadStatusData) {
        buffer.offer(data)
        // 버퍼 크기 제한 (메모리 보호)
        if (buffer.size > 500) {
            buffer.poll()  // 오래된 것 제거
        }
    }

    /**
     * 1초 배치 저장
     */
    @Scheduled(fixedRate = 1000)
    fun normalFlush() {
        flushBuffer(maxItems = 50)
    }

    /**
     * 정상 종료 시 남은 데이터 전부 저장
     * 정전 등 비정상 종료 시에는 최대 1초분 손실
     */
    @PreDestroy
    fun onShutdown() {
        flushBuffer(maxItems = buffer.size)  // 전부 저장
    }

    private fun flushBuffer(maxItems: Int) {
        val batch = mutableListOf<ReadStatusData>()
        repeat(maxItems) {
            buffer.poll()?.let { batch.add(it) } ?: return@repeat
        }
        if (batch.isNotEmpty()) {
            batchInsert(batch)
        }
    }

    private fun batchInsert(data: List<ReadStatusData>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO read_status (timestamp, azimuth, elevation, ...) VALUES (?, ?, ?, ...)",
            data.map { arrayOf(it.timestamp, it.azimuth, it.elevation, /* ... */) }
        )
    }
}
```

### 13.4 설정 상수

```kotlin
object StorageConfig {
    const val BATCH_INTERVAL_MS = 1000L  // 1초 배치 간격
    const val BUFFER_MAX_SIZE = 100      // 최대 100개 버퍼 (1초분)
    const val DB_THREAD_POOL_SIZE = 1    // DB 전용 스레드 1개
}
```

### 13.5 성능 영향 분석

| 항목 | 영향 |
|------|------|
| 실시간 경로 지연 | **0ms** (buffer.offer는 나노초 연산) |
| DB 저장 스레드 | 실시간 스레드와 완전 분리 |
| 메모리 사용량 | ~100개 데이터 버퍼 (수 KB) |
| DB 부하 | 10 INSERT/초 → 1 배치/초 (90% 감소) |
| 정전 시 데이터 손실 | 최대 1초분 (10개 레코드) |
| 정상 종료 시 손실 | **0개** (@PreDestroy로 전부 저장) |

### 13.6 배치 간격 선택 가이드

| 배치 간격 | 장점 | 단점 | 권장 상황 |
|----------|------|------|----------|
| 1초 | 데이터 손실 최소화 | DB 부하 약간 높음 | ✅ ACS 제어 시스템 |
| 5초 | DB 부하 최소화 | 정전 시 50개 손실 | 모니터링 전용 시스템 |

**ACS 권장**: 1초 배치
- 제어 시스템이므로 데이터 보존 중요
- 정전 시에도 최대 1초(10개) 손실만 허용

---

---

**문서 버전**: 2.2.0
**최종 수정**: 2026-01-07
**작성일**: 2024-12

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0.0 | 2024-12 | 최초 작성 |
| 2.0.0 | 2024-12 | 구조적 문제점, Phase별 상세 계획 추가 |
| 2.1.0 | 2024-12 | 실시간 DB 저장 전략 섹션 추가 (13장) |
| **2.2.0** | **2026-01-07** | **Phase 역할 분리 안내 추가, EphemerisService 줄 수 수정 (4,986→5,060)** |

