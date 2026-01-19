# PassSchedule 데이터 구조 리팩토링

> **작성일**: 2025-11-28
> **상태**: 대기 (상태 머신 완료 후 진행)

---

## 1. 목표

MST/DTL 데이터 구조를 **전역 고유 ID 기반**으로 재설계하여 데이터 식별의 일관성과 확장성 확보

---

## 2. 현재 문제점

| 문제 | 설명 |
|------|------|
| 위성별 인덱스 중복 | `No`가 위성별 1부터 시작 → 전역 고유하지 않음 |
| 식별자 혼용 | `No`, `MstId`, `index` 등 혼용 |
| DetailId 부재 | 명시적 관리 안됨, 향후 확장 어려움 |
| SatelliteID 혼동 | 카탈로그 번호 vs 위성 이름 구분 불명확 |

---

## 3. 새로운 구조

### 3.1 필드 정의

| 필드 | 타입 | 의미 | 예시 |
|------|------|------|------|
| `MstId` | Long | 전역 고유 MST ID | `1, 2, 3...` |
| `DetailId` | Int | 위성 내 패스 인덱스 | `0, 1, 2...` |
| `Index` | Int | 100ms 포인트 순번 | `0, 1, 2...` |
| `SatelliteID` | String | NORAD 카탈로그 번호 | `"27424"` |
| `SatelliteName` | String | 위성 표시명 | `"AQUA"` |

### 3.2 MST 구조

```kotlin
data class PassScheduleMaster(
    // Primary Key
    val mstId: Long,              // 전역 고유 ID

    // Detail 구분
    val detailId: Int,            // 위성 내 패스 인덱스

    // 위성 정보
    val satelliteID: String,      // 카탈로그 번호
    val satelliteName: String,    // 위성 이름

    // 시간 정보
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val maxElevation: Double,

    // 각도 정보
    val startAzimuth: Double,
    val startElevation: Double,

    // Keyhole 정보
    val isKeyhole: Boolean,
    val recommendedTrainAngle: Double,

    // 메타
    val dataType: String          // "original", "final_transformed" 등
)
```

### 3.3 DTL 구조

```kotlin
data class PassScheduleDetail(
    // Foreign Keys
    val mstId: Long,              // FK → MST.mstId
    val detailId: Int,            // FK → MST.detailId

    // 100ms 포인트 순번
    val index: Int,               // 0, 1, 2...

    // 추적 포인트
    val time: ZonedDateTime,
    val azimuth: Double,
    val elevation: Double,

    // 메타
    val dataType: String
)
```

---

## 4. 데이터 예시

```json
{
  "masters": [
    {
      "mstId": 1,
      "detailId": 0,
      "satelliteID": "27424",
      "satelliteName": "AQUA",
      "startTime": "2025-11-28T10:00:00Z"
    },
    {
      "mstId": 2,
      "detailId": 1,
      "satelliteID": "27424",
      "satelliteName": "AQUA"
    },
    {
      "mstId": 3,
      "detailId": 0,
      "satelliteID": "27421",
      "satelliteName": "AURA"
    }
  ],
  "details": [
    {
      "mstId": 1,
      "detailId": 0,
      "index": 0,
      "azimuth": 180.5,
      "elevation": 10.2
    }
  ]
}
```

---

## 5. 관계도

```
┌─────────────────────────────────────────────────────────┐
│                      위성 (Satellite)                    │
│                                                         │
│  SatelliteID: "27424"                                   │
│  SatelliteName: "AQUA"                                  │
│                                                         │
│  ┌─────────────────┐  ┌─────────────────┐               │
│  │ MST (mstId=1)   │  │ MST (mstId=2)   │               │
│  │ detailId=0      │  │ detailId=1      │               │
│  │ 패스 #1         │  │ 패스 #2         │               │
│  │                 │  │                 │               │
│  │ ┌─────────────┐ │  │ ┌─────────────┐ │               │
│  │ │DTL index=0 │ │  │ │DTL index=0 │ │               │
│  │ │DTL index=1 │ │  │ │DTL index=1 │ │               │
│  │ │...         │ │  │ │...         │ │               │
│  │ └─────────────┘ │  │ └─────────────┘ │               │
│  └─────────────────┘  └─────────────────┘               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                      위성 (Satellite)                    │
│                                                         │
│  SatelliteID: "27421"                                   │
│  SatelliteName: "AURA"                                  │
│                                                         │
│  ┌─────────────────┐                                    │
│  │ MST (mstId=3)   │  ← 전역 넘버링 계속                 │
│  │ detailId=0      │                                    │
│  │ 패스 #1         │                                    │
│  └─────────────────┘                                    │
└─────────────────────────────────────────────────────────┘
```

---

## 6. 마이그레이션 계획

| 단계 | 작업 | 상태 |
|:----:|------|:----:|
| 1 | BE: 새 DTO 클래스 추가 | ⬜ |
| 2 | BE: 생성 로직에서 전역 mstId 부여 | ⬜ |
| 3 | FE: 타입 정의 업데이트 | ⬜ |
| 4 | FE: 테이블/차트 식별자 변경 | ⬜ |
| 5 | API 응답 구조 변경 | ⬜ |

---

## 7. 의존성

- **상태 머신 리팩토링 완료 후 진행**
- ScheduleTrackingContext가 새 구조 사용

---

## 관련 문서

- [STATE_MACHINE.md](./STATE_MACHINE.md) - 상태 머신 (선행 작업)
- [WORKFLOW.md](./WORKFLOW.md) - BE-FE 흐름