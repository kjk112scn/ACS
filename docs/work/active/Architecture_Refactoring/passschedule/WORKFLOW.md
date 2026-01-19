# PassSchedule 워크플로우

> **작성일**: 2025-11
> **상태**: 완료 (참조용)

---

## 1. 개요

PassSchedule 모드는 **백엔드 상태 머신**과 **프론트엔드 Pinia + Vue 컴포넌트**가 연동되어 위성 스케줄을 관리합니다.

### 핵심 흐름

1. TLE 등록 → MST/DTL 생성
2. 추적 대상 설정 → 상태 머신 기반 모니터링
3. PassSchedulePage UI 동기화 → 사용자 명령

---

## 2. 백엔드 구동 흐름

### 2.1 TLE 등록 및 스케줄 생성

```
POST /api/pass-schedule/tle
  └─ addPassScheduleTle() → passScheduleTleCache 저장

POST /api/pass-schedule/tracking/generate/{satelliteId}
  └─ Orekit 계산 → 5가지 DataType 생성
     - original
     - axis_transformed
     - final_transformed
     - keyhole_axis_transformed
     - keyhole_final_transformed
```

### 2.2 추적 대상 설정

```
POST /api/pass-schedule/tracking-targets
  └─ setTrackingTargetList()
     ├─ trackingTargetList 저장
     ├─ selectedTrackMstStorage/selectedTrackDtlStorage 필터링
     └─ dataStoreService.setCurrentTrackingMstId() → WebSocket PushData
```

### 2.3 추적 모니터링

```
POST /api/pass-schedule/tracking/start
  └─ startScheduleTracking()
     └─ 100ms 타이머 → checkTrackingScheduleWithStateMachine()
        ├─ determineStateByTime(calTime)
        ├─ transitionTo(newState)
        └─ sendStateToFrontend()
```

### 2.4 주요 API

| API | 목적 |
|-----|------|
| `POST /tle` | TLE 추가 |
| `POST /tracking/generate/{satelliteId}` | 추적 데이터 생성 |
| `POST /tracking-targets` | 추적 대상 등록 |
| `POST /tracking/start` | 상태 머신 시작 |
| `POST /tracking/stop` | 상태 머신 종료 |
| `GET /tracking/master` | 스케줄 데이터 조회 |

---

## 3. 프론트엔드 흐름

### 3.1 passScheduleStore (Pinia)

```typescript
// 핵심 상태
selectedScheduleList      // 등록된 스케줄
predictedTrackingPath     // 차트 예정 경로
actualTrackingPath        // 차트 실제 경로
currentTrackingMstId      // 현재 추적 중 (icdStore 연동)
nextTrackingMstId         // 다음 대기 중

// API 연동
fetchScheduleDataFromServer()
setTrackingTargets()
startScheduleTracking()
stopScheduleTracking()
```

### 3.2 PassSchedulePage.vue

| 영역 | 기능 |
|------|------|
| Schedule Control | 테이블, 현재/다음 하이라이트, 버튼 |
| Position View | 차트 (현재 위치, 예정/실제 경로) |
| Time Offset | 시간 보정 컨트롤 |

### 3.3 하이라이트 색상

| 상태 | 색상 | CSS 클래스 |
|------|------|-----------|
| 현재 추적 | 녹색 | `highlight-current-schedule` |
| 다음 대기 | 파란색 | `highlight-next-schedule` |

---

## 4. 데이터 흐름

```
┌──────────────────────────────────────────────────────────────┐
│ 1. TLE 등록                                                  │
│    FE → POST /tle → BE passScheduleTleCache                  │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 2. 스케줄 생성                                               │
│    FE → POST /tracking/generate → BE MST/DTL Storage         │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 3. 추적 대상 등록                                            │
│    FE Select Schedule → POST /tracking-targets → BE Queue    │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 4. 모니터링 시작                                             │
│    FE Start → POST /tracking/start → 100ms 타이머            │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 5. 실시간 동기화                                             │
│    BE State Machine → WebSocket PushData → FE icdStore       │
│    └─ currentTrackingMstId, nextTrackingMstId, state         │
└──────────────────────────────────────────────────────────────┘
```

---

## 5. 위성 추적 시퀀스

### 5.1 단일 위성

```
1. Select Schedule → /tracking-targets 등록
2. Start → 상태 머신 가동
3. STOWED → MOVING_TRAIN → TRAIN_STABILIZING → MOVING_TO_START
4. READY → TRACKING (헤더/실시간 데이터 전송)
5. POST_TRACKING → COMPLETED (Stow 복귀)
```

### 5.2 다중 위성

```
위성 A 추적 완료
    ↓
POST_TRACKING에서 다음 스케줄 평가
    ├─ 위성 B 시작까지 ≤4분 → MOVING_TRAIN
    ├─ 위성 B 시작까지 >4분 → STOWING
    └─ 다음 없음 → COMPLETED
```

---

## 6. 제약 및 고려사항

| 항목 | 설명 |
|------|------|
| 자동 추적 | 상태 머신 가이드, 실제 Go/Stop은 사용자 트리거 |
| Keyhole | BE에서 DataType별 저장, FE는 API 응답 신뢰 |
| 시간 동기화 | Time Offset 변경 시 NTP 명령 + 스케줄 재평가 |
| 캐시 관리 | 대량 위성 시 주기적 캐시 정리 필요 |

---

## 7. 데이터 용어

| 용어 | 설명 |
|------|------|
| MST | Master - 패스 메타데이터 (시작/종료, 최대 고도) |
| DTL | Detail - 궤적 포인트 (시간, Az, El) |
| TrackingTarget | 사용자 선택 MST ID 묶음 |
| mstId / detailId | 스케줄 식별자 (전역 고유) |

---

## 관련 문서

- [STATE_MACHINE.md](./STATE_MACHINE.md) - 상태 머신 상세
- [DATA_STRUCTURE.md](./DATA_STRUCTURE.md) - MST/DTL 구조