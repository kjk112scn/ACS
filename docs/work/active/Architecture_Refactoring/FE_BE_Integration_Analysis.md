# ACS Frontend-Backend 통합 분석 및 리팩토링 전략

> **통합 알림**: 이 문서의 내용은 [Comprehensive_Deep_Analysis.md](./Comprehensive_Deep_Analysis.md)에 통합되었습니다. 이 문서는 참고용으로 유지됩니다.

> 작성일: 2026-01-13
> 분석 범위: WebSocket 통신, REST API 연계, 상태 동기화

---

## 1. 연계 문제점 목록

### 1.1 WebSocket 통신 (icdStore.ts <-> PushDataController.kt)

#### 문제점 #1: 타입 정의 중복 및 불일치

| 영역 | FE 파일/라인 | BE 파일/라인 | 문제점 |
|------|-------------|-------------|--------|
| MessageData | `icdService.ts:11-31` | `PushData.kt` | FE는 느슨한 타입(`[key: string]: unknown`), BE는 엄격한 data class |
| TrackingStatus | `icdService.ts:3-9` | `PushData.kt:TrackingStatus` | 필드명은 동일하나 nullable 처리 다름 |
| 비트 파싱 | `icdStore.ts:352-563` | `DataStoreService.kt` | FE에서 8bit 비트 문자열 수동 파싱, BE에서 이미 파싱된 값 전송 가능 |

**영향**:
- 타입 불일치로 런타임 오류 가능성
- FE에서 불필요한 비트 파싱 로직 중복 (약 200줄)

#### 문제점 #2: 데이터 변환 중복

| FE 위치 | BE 위치 | 중복 로직 |
|---------|---------|----------|
| `icdStore.ts:8-41` (safeToString) | - | 안전한 문자열 변환 |
| `icdStore.ts:352-563` | `ICDService.kt` | 비트 문자열 파싱 로직 |
| `icdStore.ts:1351-1555` | `PushDataService.kt` | 에러 데이터 처리 |

**개선 기회**: BE에서 파싱된 boolean 값을 직접 전송하면 FE 파싱 로직 제거 가능

#### 문제점 #3: 상태 변수 명명 불일치

| FE 변수명 | BE 변수명 | 비고 |
|----------|----------|------|
| `currentTrackingMstId` | `currentTrackingMstId` | 일치 |
| `currentTrackingDetailId` | `currentTrackingDetailId` | 일치 |
| `ephemerisStatus` | `ephemerisStatus` | 일치 |
| `ephemerisTrackingState` | `ephemerisTrackingState` | 일치 |
| `passScheduleTrackingState` | - | **BE에 없음** (추가 필요) |

---

### 1.2 REST API 연계

#### 문제점 #4: API 응답 형식 불일치

| API | FE 기대 타입 | BE 응답 형식 | 불일치 |
|-----|-------------|-------------|--------|
| `POST /ephemeris/tracking/start` | `Map<String, Any>` | `mapOf("message", "mstId", "detailId", "status")` | 일관됨 |
| `POST /pass-schedule/tle` | `TLEResponse` | `mapOf("success", "message", "data", "timestamp")` | **FE 타입과 약간 불일치** |
| `GET /ephemeris/master` | `ScheduleItem[]` | `List<Map<String, Any?>>` | **타입 매핑 필요** |

#### 문제점 #5: 에러 처리 패턴 불일치

**FE 패턴** (`ephemerisTrackService.ts`):
```typescript
try {
  const response = await api.post('/icd/...')
  return response.data
} catch (error) {
  console.error('...failed:', error)
  throw error
}
```

**BE 패턴** (`EphemerisController.kt`):
```kotlin
return try {
    ResponseEntity.ok(mapOf("status" to "success", ...))
} catch (e: Exception) {
    ResponseEntity.internalServerError().body(mapOf("status" to "error", ...))
}
```

**문제점**: FE에서 BE 오류 응답의 `status` 필드를 확인하지 않고 HTTP 상태 코드만 의존

---

### 1.3 상태 동기화

#### 문제점 #6: State Machine 상태값 불일치

**EphemerisService 상태** (`EphemerisService.kt:79-86`):
```kotlin
enum class TrackingState {
    IDLE, PREPARING, WAITING, TRACKING, COMPLETED, ERROR
}
```

**PassScheduleService 상태** (`PassScheduleService.kt:115-149`):
```kotlin
enum class PassScheduleState {
    IDLE, STOWING, STOWED, MOVING_TRAIN, TRAIN_STABILIZING,
    MOVING_TO_START, READY, TRACKING, POST_TRACKING, COMPLETED, ERROR
}
```

**FE icdStore** (`icdStore.ts:294-299`):
```typescript
const ephemerisTrackingState = ref<string | null>(null)
const passScheduleTrackingState = ref<string | null>(null) // BE에 없음!
```

**문제점**:
- PassSchedule의 상세 상태가 FE에 전달되지 않음
- `passScheduleTrackingState`가 BE에서 전송되지 않음

#### 문제점 #7: ID 체계 불일치

| 영역 | FE 타입 | BE 타입 | 비고 |
|------|--------|--------|------|
| `mstId` | `number` | `Long` | JavaScript의 number 범위 확인 필요 |
| `detailId` | `number` | `Int` | 일치 |
| `passId` (deprecated) | `number` | - | FE에서만 사용, 제거 필요 |

---

### 1.4 공통 문제점

#### 문제점 #8: 비트 파싱 로직 중복

**FE** (`icdStore.ts:352-563`): 비트 문자열을 개별 boolean으로 파싱
```typescript
const parseProtocolStatusBits = (bitString: string) => {
  const bits = bitString.padStart(8, '0').split('').reverse()
  protocolElevationStatus.value = bits[0] === '1'
  // ... 7개 더
}
```

**BE** (`ICDService.kt`): 바이트 배열에서 비트 추출

**개선 방안**: BE에서 파싱된 개별 필드를 WebSocket으로 전송

#### 문제점 #9: ScheduleItem 인터페이스 불일치

**FE ephemerisTrackService.ts** (약 40개 필드):
```typescript
export interface ScheduleItem {
  No: number
  mstId?: number
  detailId?: number
  SatelliteID: string
  // ... 37개 더
}
```

**FE passScheduleStore.ts** (약 35개 필드):
```typescript
export interface ScheduleItem {
  mstId: number
  detailId: number
  no: number
  // ... 32개 더
}
```

**문제점**: 같은 이름의 인터페이스가 다른 정의를 가짐

---

## 2. 통합 리팩토링 전략

### 2.1 대안 비교

| 대안 | 성능 | 유지보수 | 테스트 | 호환성 | 복잡도 | 총점 |
|------|------|---------|--------|--------|--------|------|
| A: 점진적 통합 | 3 | 5 | 4 | 5 | 2 | **19** |
| B: 전면 재설계 | 5 | 4 | 3 | 2 | 5 | 19 |
| C: API 계약 우선 | 4 | 5 | 5 | 4 | 3 | **21** |

**결론**: **대안 C (API 계약 우선)** 선택

### 2.2 API 계약 우선 전략

```
Phase 1: 공유 타입 정의
└── 1.1 FE/BE 공통 타입 스키마 작성 (JSON Schema)
└── 1.2 FE TypeScript 타입 자동 생성
└── 1.3 BE DTO 일치 검증

Phase 2: WebSocket 데이터 최적화
└── 2.1 BE에서 파싱된 boolean 값 직접 전송
└── 2.2 FE 비트 파싱 로직 제거
└── 2.3 TrackingStatus 확장 (passScheduleTrackingState 추가)

Phase 3: REST API 일관성
└── 3.1 에러 응답 형식 표준화
└── 3.2 FE 에러 처리 개선
└── 3.3 API 응답 타입 일치 검증

Phase 4: 상태 머신 동기화
└── 4.1 BE 상태 → FE 상태 매핑 정의
└── 4.2 passScheduleTrackingState 전송 구현
└── 4.3 상태 변경 이벤트 테스트
```

---

## 3. 구체적인 실행 계획

### Phase 1: 공유 타입 정의 (1-2일)

#### 1.1 타입 스키마 작성

**FE/BE 동시 작업**

| 파일 | 위치 | 작업 |
|------|------|------|
| `api-types.json` | `docs/api/` | JSON Schema 작성 |
| `WebSocketMessage.ts` | `frontend/src/types/` | 타입 정의 생성 |
| `WebSocketMessage.kt` | `backend/.../dto/` | Kotlin data class 검증 |

**변경 내용**:
```json
// docs/api/websocket-schema.json
{
  "WebSocketMessage": {
    "topic": "string",
    "data": {
      "data": "ReadData",
      "trackingStatus": "TrackingStatus",
      "serverTime": "string",
      "currentTrackingMstId": "number|null",
      "currentTrackingDetailId": "number|null"
    }
  }
}
```

**위험도**: 낮음 (문서 작업)

---

### Phase 2: WebSocket 데이터 최적화 (2-3일)

#### 2.1 BE 파싱된 값 전송

**BE 작업** (`PushDataService.kt`):
- 비트 필드를 개별 boolean으로 변환
- `mainBoardProtocolStatus`, `mainBoardMCStatus` 등 추가

**FE 작업** (`icdStore.ts`):
- 비트 파싱 함수 deprecated 처리
- 직접 boolean 값 사용

**영향 파일**:
| BE | FE |
|----|----|
| `PushDataService.kt` | `icdStore.ts:352-563` |
| `DataStoreService.kt` | `icdService.ts` |

**위험도**: 중간 (기존 로직 대체)

#### 2.2 TrackingStatus 확장

**BE 작업** (`PushData.kt`):
```kotlin
data class TrackingStatus(
    val ephemerisStatus: Boolean? = null,
    val ephemerisTrackingState: String? = null,
    val passScheduleStatus: Boolean? = null,
    val passScheduleTrackingState: String? = null,  // 추가
    val sunTrackStatus: Boolean? = null,
    val sunTrackTrackingState: String? = null
)
```

**FE 작업**: 이미 `passScheduleTrackingState` 정의됨, BE 연동만 필요

**위험도**: 낮음

---

### Phase 3: REST API 일관성 (1-2일)

#### 3.1 에러 응답 표준화

**BE 작업**: 모든 컨트롤러에 표준 응답 형식 적용
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
```

**FE 작업** (`services/api/*.ts`):
- 에러 처리 유틸리티 함수 작성
- `status` 필드 확인 로직 추가

**영향 파일**:
| BE | FE |
|----|----|
| `EphemerisController.kt` | `ephemerisTrackService.ts` |
| `PassScheduleController.kt` | `passScheduleService.ts` |
| `SettingsController.kt` | `settingsService.ts` |
| `ICDController.kt` | `icdService.ts` |

**위험도**: 중간 (API 응답 형식 변경)

---

### Phase 4: 상태 머신 동기화 (2-3일)

#### 4.1 상태 매핑 정의

**EphemerisService 상태 매핑**:
| BE 상태 | FE 표시 | 설명 |
|---------|---------|------|
| IDLE | `"IDLE"` | 대기 |
| PREPARING | `"PREPARING"` | 준비 중 (Train 이동, 안정화, Az/El 이동) |
| WAITING | `"WAITING"` | 시작 대기 |
| TRACKING | `"TRACKING"` | 추적 중 |
| COMPLETED | `"COMPLETED"` | 완료 |
| ERROR | `"ERROR"` | 오류 |

**PassScheduleService 상태 매핑**:
| BE 상태 | FE 표시 | 설명 |
|---------|---------|------|
| IDLE | `"IDLE"` | 대기 |
| STOWING | `"PREPARING"` | Stow 이동 중 |
| STOWED | `"WAITING"` | Stow 대기 |
| MOVING_TRAIN | `"PREPARING"` | Train 이동 중 |
| TRAIN_STABILIZING | `"PREPARING"` | Train 안정화 |
| MOVING_TO_START | `"PREPARING"` | 시작 위치 이동 |
| READY | `"WAITING"` | 시작 대기 |
| TRACKING | `"TRACKING"` | 추적 중 |
| POST_TRACKING | `"COMPLETED"` | 종료 처리 |
| COMPLETED | `"COMPLETED"` | 완료 |
| ERROR | `"ERROR"` | 오류 |

#### 4.2 BE 구현

**PassScheduleService.kt** 변경:
```kotlin
// DataStoreService에 상태 업데이트
dataStoreService.updateTrackingStatus(
    PushData.TrackingStatus(
        passScheduleStatus = true,
        passScheduleTrackingState = currentState.name  // 추가
    )
)
```

**위험도**: 중간 (상태 관리 로직 변경)

---

## 4. 우선순위 및 일정

| Phase | 작업 | 우선순위 | 예상 일수 | FE/BE |
|-------|------|---------|---------|-------|
| 1.1 | 타입 스키마 작성 | 높음 | 1일 | 공동 |
| 2.2 | TrackingStatus 확장 | 높음 | 0.5일 | BE |
| 4.2 | passScheduleTrackingState 구현 | 높음 | 1일 | BE+FE |
| 2.1 | BE 파싱된 값 전송 | 중간 | 2일 | BE |
| 2.1 | FE 비트 파싱 제거 | 중간 | 1일 | FE |
| 3.1 | 에러 응답 표준화 | 낮음 | 2일 | BE+FE |

**총 예상 기간**: 5-7일 (병렬 작업 시)

---

## 5. ADR 필요 여부

| 결정 사항 | ADR 필요 | 이유 |
|----------|---------|------|
| 공유 타입 스키마 도입 | **예** | 아키텍처 패턴 변경 |
| WebSocket 데이터 구조 변경 | 아니오 | 최적화, 기능 변경 없음 |
| 에러 응답 표준화 | **예** | API 인터페이스 변경 |
| 상태 머신 매핑 | 아니오 | 기존 상태의 FE 표시 방식 변경 |

---

## 6. 참조 파일

### Frontend
- `e:\001.GTL\SW\ACS\frontend\src\stores\icd\icdStore.ts` (2,971줄)
- `e:\001.GTL\SW\ACS\frontend\src\services\api\icdService.ts` (814줄)
- `e:\001.GTL\SW\ACS\frontend\src\services\mode\ephemerisTrackService.ts` (1,192줄)
- `e:\001.GTL\SW\ACS\frontend\src\services\mode\passScheduleService.ts` (1,117줄)
- `e:\001.GTL\SW\ACS\frontend\src\stores\mode\ephemerisTrackStore.ts` (1,300줄)
- `e:\001.GTL\SW\ACS\frontend\src\stores\mode\passScheduleStore.ts` (2,452줄)

### Backend
- `e:\001.GTL\SW\ACS\backend\src\main\kotlin\com\gtlsystems\acs_api\controller\websocket\PushDataController.kt` (762줄)
- `e:\001.GTL\SW\ACS\backend\src\main\kotlin\com\gtlsystems\acs_api\service\datastore\DataStoreService.kt` (621줄)
- `e:\001.GTL\SW\ACS\backend\src\main\kotlin\com\gtlsystems\acs_api\controller\mode\EphemerisController.kt` (1,091줄)
- `e:\001.GTL\SW\ACS\backend\src\main\kotlin\com\gtlsystems\acs_api\controller\mode\PassScheduleController.kt` (1,557줄)
- `e:\001.GTL\SW\ACS\backend\src\main\kotlin\com\gtlsystems\acs_api\service\mode\EphemerisService.kt` (5,057줄)
- `e:\001.GTL\SW\ACS\backend\src\main\kotlin\com\gtlsystems\acs_api\service\mode\PassScheduleService.kt` (3,846줄)

---

**문서 버전**: 1.0.0
**최종 업데이트**: 2026-01-13
