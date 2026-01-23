# Phase 5: BE 서비스 분리 상세 실행 계획

> **작성일**: 2026-01-16
> **목표**: 대용량 서비스 파일 분리를 통한 유지보수성 향상
> **원칙**: 안전 우선 - 한 번에 잘 되게끔 꼼꼼하게

---

## 1. 현재 상태 분석

### 1.1 EphemerisService.kt (5,393줄)

| 영역 | 라인 범위 | 함수 수 | 의존성 |
|------|-----------|---------|--------|
| State Machine | 79-125 | enum 2개 | 내부 상태 |
| Lifecycle | 127-190 | 3개 | ACSEventBus |
| State Handlers | 1143-1530 | 8개 | UDP, State |
| Movement Commands | 800-875 | 5개 | UDP |
| TLE Cache | 3915-3940 | 4개 | **독립** |
| Data Query | 2940-3900 | 25개 | 내부 Storage |
| CSV Export | 4056-5370 | 10개 | Data Query |
| Tracking Logic | 그 외 | 40+ | 복합 |

### 1.2 PassScheduleService.kt (3,856줄)

| 영역 | 라인 범위 | 함수 수 | 의존성 |
|------|-----------|---------|--------|
| State Machine v1.0 | 69-103 | enum 2개 | 내부 상태 |
| State Machine v2.0 | 105-248 | enum 1개 + data class | 내부 상태 |
| v2.0 Logic | 3196-3856 | 20개 | 복합 |
| TLE Cache | 2950-2998 | 8개 | **독립** |
| Data Query | 2163-2940 | 25개 | 내부 Storage |
| Tracking Logic | 그 외 | 30+ | 복합 |

---

## 2. 안전한 분리 전략 (리스크 최소화)

### 2.1 분리 원칙

1. **독립성 우선**: 의존성이 없는 부분부터 추출
2. **인터페이스 기반**: 명확한 인터페이스로 연결
3. **점진적 접근**: 한 번에 하나씩, 테스트 후 다음 단계
4. **롤백 가능**: 문제 발생 시 원복 가능한 구조

### 2.2 분리 대상 선정 기준

| 우선순위 | 기준 | 예시 |
|----------|------|------|
| 1 | 완전 독립 (외부 의존성 0) | TLE Cache CRUD |
| 2 | 읽기 전용 (상태 변경 없음) | Statistics, Debug |
| 3 | 명확한 경계 (I/O 분리) | Data Export |
| 4 | 복합 의존성 | State Machine |

---

## 3. 분리 실행 계획

### Phase 5-A: TLE 캐시 분리 (리스크: 낮음)

#### EphemerisTLECache.kt

```kotlin
// 경로: service/mode/ephemeris/EphemerisTLECache.kt
@Component
class EphemerisTLECache {
    private val cache = ConcurrentHashMap<String, Pair<String, String>>()

    fun add(satelliteId: String, tleLine1: String, tleLine2: String)
    fun get(satelliteId: String): Pair<String, String>?
    fun remove(satelliteId: String)
    fun getAllIds(): List<String>
    fun clear()
    fun size(): Int
}
```

**변경 영향**:
- EphemerisService: 4개 함수 → 주입된 캐시 사용으로 변경
- 리스크: 매우 낮음 (단순 CRUD)

#### PassScheduleTLECache.kt

```kotlin
// 경로: service/mode/passSchedule/PassScheduleTLECache.kt
@Component
class PassScheduleTLECache {
    private val cache = ConcurrentHashMap<String, Triple<String, String, String>>()

    fun add(satelliteId: String, tleLine1: String, tleLine2: String, name: String?)
    fun get(satelliteId: String): Pair<String, String>?
    fun getWithName(satelliteId: String): Triple<String, String, String>?
    fun getName(satelliteId: String): String?
    fun remove(satelliteId: String)
    fun getAllIds(): List<String>
    fun clear()
    fun size(): Int
}
```

**변경 영향**:
- PassScheduleService: 8개 함수 → 주입된 캐시 사용으로 변경
- 리스크: 매우 낮음 (단순 CRUD)

### Phase 5-B: 데이터 저장소 분리 (리스크: 중간)

> **주의**: 이 단계는 내부 저장소 의존성이 있어 신중히 진행

#### 분리 검토 결과

CSV Export 함수들의 의존성 분석:
1. `ephemerisTrackMstStorage` - 내부 저장소
2. `ephemerisTrackDtlStorage` - 내부 저장소
3. `getAllEphemerisTrackMst()` - 조회 함수
4. `getEphemerisTrackDtlByMstIdAndDataType()` - 조회 함수
5. `settingsService` - 외부 의존성

**결론**: CSV Export 분리는 데이터 저장소 분리와 함께 진행해야 함

### Phase 5-C: 최종 목표 파일 구조

```
service/mode/
├── EphemerisService.kt        (~3,500줄 → 주요 로직)
├── ephemeris/
│   ├── EphemerisTLECache.kt   (~100줄)
│   ├── EphemerisDataRepository.kt  (향후, ~1,500줄)
│   └── EphemerisDataExporter.kt    (향후, ~800줄)
├── PassScheduleService.kt     (~2,800줄 → 주요 로직)
└── passSchedule/
    ├── PassScheduleTLECache.kt (~120줄)
    └── PassScheduleDataRepository.kt (향후, ~800줄)
```

---

## 4. 즉시 실행 가능한 작업

### 4.1 오늘 실행할 작업 (리스크 최소)

| 순서 | 작업 | 예상 라인 감소 | 리스크 |
|------|------|---------------|--------|
| 1 | EphemerisTLECache 추출 | -50줄 | 매우 낮음 |
| 2 | PassScheduleTLECache 추출 | -70줄 | 매우 낮음 |
| 3 | 빌드 테스트 | - | - |

### 4.2 보류 작업 (추가 분석 필요)

| 작업 | 이유 |
|------|------|
| CSV Export 분리 | 내부 저장소 의존성 |
| Data Repository 분리 | 상태 관리 복잡성 |
| State Machine 분리 | 타이머/이벤트 의존성 |

---

## 5. 실행 체크리스트

### Step 1: EphemerisTLECache 분리
- [ ] `service/mode/ephemeris/` 디렉토리 생성
- [ ] `EphemerisTLECache.kt` 파일 생성
- [ ] EphemerisService에서 TLE 캐시 관련 코드 제거
- [ ] EphemerisService에 EphemerisTLECache 주입
- [ ] 빌드 확인

### Step 2: PassScheduleTLECache 분리
- [ ] `service/mode/passSchedule/` 디렉토리 생성
- [ ] `PassScheduleTLECache.kt` 파일 생성
- [ ] PassScheduleService에서 TLE 캐시 관련 코드 제거
- [ ] PassScheduleService에 PassScheduleTLECache 주입
- [ ] 빌드 확인

### Step 3: 검증
- [ ] 백엔드 서버 시작 확인
- [ ] TLE 추가/조회/삭제 API 테스트
- [ ] 로그 확인

---

## 6. 롤백 계획

문제 발생 시:
1. 새로 생성한 파일 삭제
2. EphemerisService.kt / PassScheduleService.kt 원복
3. 빌드 확인

---

## 7. 결론

**즉시 실행**: TLE 캐시 분리 (리스크 최소, 효과 확실)
**보류**: 데이터 저장소/Export 분리 (추가 설계 필요)

이 계획은 "한 번에 잘 되게끔"을 위해 가장 안전한 부분부터 시작합니다.
