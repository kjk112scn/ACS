# PassSchedule 스케줄 수행 구동 방식

## 1. 개요

Pass Schedule 모드는 **백엔드 상태 머신**과 **프론트엔드 Pinia + Vue 컴포넌트**가 긴밀히 연동되어 위성 스케줄을 관리한다.  
이 문서는 다음 세 축을 중심으로 동작을 설명한다.

1. 스케줄 데이터 확보 및 캐시 (TLE 등록 → MST/DTL 생성)
2. 추적 대상 설정과 상태 머신 기반 모니터링
3. PassSchedulePage에서의 UI/상태 동기화와 사용자 명령 흐름

## 2. 백엔드 구동 흐름 (`PassScheduleService`, `PassScheduleController`)

### 2.1 TLE 등록 및 스케줄 생성
- `POST /api/pass-schedule/tle`  
  - 위성 ID를 직접 전달하거나 TLE Line1에서 추출한다.  
  - `PassScheduleService.addPassScheduleTle()`이 TLE 정보를 `passScheduleTleCache`와 MST/DTL 캐시에 저장.
- `POST /api/pass-schedule/tracking/generate/{satelliteId}` 또는 `POST /api/pass-schedule/tle-and-tracking`  
  - Orekit 기반 계산 후 5가지 DataType(original, axis_transformed, final_transformed, keyhole_axis_transformed, keyhole_final_transformed)을 생성하여 `passScheduleTrackMstStorage`/`passScheduleTrackDtlStorage`에 캐시.

### 2.2 추적 대상 설정
- `POST /api/pass-schedule/tracking-targets`  
  - 클라이언트가 선택한 패스(MST ID, 시간, 최대 고도)를 `TrackingTarget` 목록으로 전달.
  - `setTrackingTargetList()`는
    - 대상 리스트를 저장
    - 선택된 패스만 다시 필터링하여 `selectedTrackMstStorage`/`selectedTrackDtlStorage`에 보관
    - `dataStoreService`에 현재/다음 MST ID를 반영 → WebSocket PushData에 포함되어 프론트 상태와 동기화.

### 2.3 추적 모니터링과 상태 머신
- `POST /api/pass-schedule/tracking/start` 호출 시
  - 100ms 타이머가 구동되고 `checkTrackingScheduleWithStateMachine()`이 실행.
  - `TrackingState` (IDLE, WAITING, PREPARING, TRACKING, COMPLETED)와 `PreparingStep`을 통해
    - 2분 이상 남은 경우 Stow 위치 대기
    - 2분 이내면 Train → Az/El 순서로 준비
    - 현재 시간을 포함하는 패스가 있으면 TRACKING 상태, `prepareTrackingStart()`로 초기 헤더/데이터 송신
  - `dataStoreService.setCurrentTrackingMstId()`와 `setNextTrackingMstId()`가 WebSocket PushData에 반영되어 프론트가 현재/다음 스케줄을 강조한다.
- `POST /api/pass-schedule/tracking/stop`은 타이머를 종료하고 캐시/상태를 정리한다.

### 2.4 주요 API 요약
| API | 목적 | 비고 |
| --- | --- | --- |
| `POST /tle` | TLE 추가 | 위성 ID 추출 지원 |
| `POST /tracking/generate/{satelliteId}` | 해당 위성 추적 데이터 생성 | MST/DTL 캐시 |
| `POST /tracking-targets` | 추적 대상 등록 | 프론트 Schedule Control에서 호출 |
| `POST /tracking/start` / `/stop` | 상태 머신 제어 | 100ms 모니터링 |
| `GET /tracking/master` 등 | 스케줄 데이터 조회 | PassSchedulePage 초기화 시 사용 |

## 3. 프론트엔드 흐름 (`passScheduleStore`, `PassSchedulePage.vue`)

### 3.1 `passScheduleStore` (Pinia)
- **데이터 원본**  
  - `selectedScheduleList`: 사용자가 등록한 스케줄 (Schedule Control 테이블)
  - `predictedTrackingPath`, `actualTrackingPath`: 차트에 사용
  - `currentTrackingMstId`, `nextTrackingMstId`: ICD WebSocket 데이터와 연동
- **API 연동**  
  - `fetchScheduleDataFromServer()`로 `/api/pass-schedule/tracking/master` 등 호출
  - `sendTimeOffset`, `setTrackingTargets`, `startScheduleTracking`, `stopScheduleTracking`으로 컨트롤러 API와 통신
  - localStorage 저장/복원 기능으로 페이지 이탈 시 상태 유지 (TLE/스케줄/차트 데이터)

### 3.2 `PassSchedulePage.vue`
- **Schedule Control 영역**
  - 테이블 데이터: `sortedScheduleList` (Pinia store에서 가져와 정렬)
  - 현재/다음 스케줄 강조:  
    - `icdStore.currentTrackingMstId`, `icdStore.nextTrackingMstId` 변화를 watch하여 DOM 스타일 적용
  - 버튼 동작:
    - `TLE Upload` → `openModal('tle-upload')` → 업로드 성공 시 스케줄 재조회
    - `Select Schedule` → 모달에서 대상 선택 후 `setTrackingTargets`
    - `Start` → `passScheduleStore.setTrackingTargets` + `startScheduleTracking`
    - `Stop` → `stopScheduleTracking` + ICD stopCommand
    - `Stow` → `icdStore.stowCommand`

- **Position View 차트**
  - `PassChartUpdatePool`로 현재 위치, 실시간 경로(흰색), 예정 경로(파란색)을 관리
  - `updateChart()`가 100ms 간격으로 실행되어
    - `icdStore`에서 실제 각도를 가져와 차트 업데이트
    - `passScheduleStore.actualTrackingPath`/`predictedTrackingPath`를 반영
  - 스케줄 전환 시 `clearTrackingPaths()`로 이전 경로를 초기화

- **시간 보정(Time Offset)**
  - 상단 Offset 컨트롤에서 Time 값을 입력 후 `increment/decrement` → `passScheduleStore.sendTimeOffset()` → 백엔드 `passScheduleTimeOffsetCommand()` 호출
  - Time Offset이 변경되면 `PassScheduleService`가 NTP 명령과 초기 추적 데이터를 다시 전송하여 백엔드/프론트 시각을 맞춘다.

## 4. 백엔드↔프론트 데이터 흐름 요약

1. **TLE 등록 & 추적 데이터 생성**  
   프론트 → `/tle` 및 `/tracking/generate` → 백엔드 캐시 구성
2. **스케줄 선택**  
   PassSchedulePage는 `/tracking/master` 결과를 받아 Schedule Control 테이블 구성
3. **추적 대상 등록**  
   선택된 항목을 `/tracking-targets`로 전송 → 백엔드 `selectedTrackMstStorage` 갱신
4. **모니터링 시작**  
   `/tracking/start` 호출 → 상태 머신 동작 → `dataStoreService`를 통해 WebSocket PushData에 `currentTrackingMstId`, `nextTrackingMstId`, `passScheduleStatusInfo` 반영
5. **프론트 실시간 반영**  
   `icdStore`는 WebSocket 메시지를 받아 Pinia store와 `PassSchedulePage`의 차트/하이라이트를 업데이트
6. **Time Offset 및 Stow/Stop**  
   사용자가 명령 시 대응 API 호출 → 하드웨어/백엔드 상태 동기화

## 5. 제약 및 고려 사항

- **자동 추적 시작/종료**는 상태 머신이 가이드하지만, 실제 하드웨어 명령(Go/Stop/Stow)은 여전히 사용자가 트리거해야 한다.
- **Keyhole 데이터 선택**은 백엔드에서 MST/DTL을 DataType별로 저장하고, 프론트는 API 응답을 신뢰해 차트를 그린다. Keyhole 최적화가 활성화되지 않은 스케줄은 final_transformed 데이터를 사용한다.
- **시간 동기화**  
  - Time Offset 명령 전송 후 백엔드는 즉시 `udpFwICDService.writeNTPCommand()`를 실행한다.  
  - 프론트는 `icdStore.resultTimeOffsetCalTime`을 사용하여 남은 시간을 계산하므로 NTP 명령이 실패하면 표시값이 어긋날 수 있다.
- **캐시 관리**  
  - PassScheduleService는 5가지 DataType 전체를 메모리에 유지하므로, 대량 위성 등록 시 캐시 정리 API(`/tracking` DELETE 등)를 주기적으로 호출해야 한다.

## 6. 데이터 구조 및 용어

| 용어 | 설명 |
| --- | --- |
| MST (Master) | 패스 메타데이터 (시작/종료, 최대 고도, Keyhole 여부) |
| DTL (Detail) | 패스별 궤적 포인트 (시간, Az, El) |
| TrackingTarget | 사용자가 선택한 MST ID 묶음 |
| currentTrackingMstId / nextTrackingMstId | WebSocket으로 전달되는 현재/다음 추적 대상 |
| passScheduleStatusInfo | WebSocket 상태 플래그 (isActive 등) |

## 7. 향후 관리 포인트 (현안)

1. **상태 머신 로그 → 프론트 전달**  
   현재는 서버 로그에만 남으므로, REST API 또는 PushData에 요약 상태를 포함하면 UX 개선.
2. **Schedule Control과 API 호출 간 중복**  
   페이지 진입 시 자동으로 데이터를 모두 재요청하므로, 필요할 때만 API 호출하도록 로컬 캐시 정책을 조정할 필요가 있다.
3. **Keyhole 데이터 검증 UI**  
   백엔드에서 결정한 Keyhole 여부와 추천 Train 각도를 프론트 Schedule Control에 명확히 표시하면 운영 중 판단이 쉬워짐.
4. **Time Offset 실패 시 재시도 전략**  
   현재는 프론트에서 성공 여부만 로그로 확인하므로, 실패 시 사용자에게 재시도 버튼을 제공할 필요가 있다.

## 8. 최근 협의 및 개선 사항

- **MST 식별자 정렬**  
  - Select Schedule 모달에서 화면 편의를 위해 `no`를 1부터 재생성하고, 서버에서 내려온 실제 MST ID는 `index` 필드에 보관한다.  
  - `/tracking-targets` 호출 시 `mstId = schedule.index`로 고정하도록 `passScheduleStore.setTrackingTargets()`를 수정하여, 현재/다음 스케줄 판단이 정확히 이루어지도록 했다.
- **Schedule Control 하이라이트 색상 개편**  
  - `highlight-current-schedule` 행은 딥그린 배경 + 민트 텍스트, `highlight-next-schedule` 행은 딥블루 배경 + 라이트 블루 텍스트로 변경해 가독성을 확보했다.  
  - 내부 셀과 버튼에는 `transparent`를 적용해 글자색 덮어쓰기를 방지했다.
- **Go 버튼 관계 정리**  
  - 추적 대상 등록은 Select Schedule 후 즉시 이루어지며 Go(Start) 버튼과 무관하다. Go 버튼은 모니터링 재시작 및 Stow→Tracking 명령을 묶어 전송하는 역할이다.  
  - 대상 등록에 사용되는 MST ID가 잘못 전달될 경우(current/next 미표시) Go 동작과 상관없이 상태가 비어 있으므로, ID 전달 로직 수정으로 문제를 해소했다.

## 9. 위성 추적 시퀀스 (Schedule Control 기준)

1. **대상 선택 & 등록**
   - 사용자 `Select Schedule` → `passScheduleStore.replaceSelectedSchedules()`  
   - 모달 정렬 과정에서 `no`는 화면 순번, `index`는 서버 MST ID로 보존.  
   - `/tracking-targets` 요청 시 `mstId = index`, `satelliteId`, `start/endTime`가 함께 전달되고 `setTrackingTargetList()`가 `selectedTrackMstStorage`를 재구성한다.

2. **Start(Go) 버튼 동작**
   - `handleStartCommand()`가
     1. 선택된 스케줄 전체를 다시 `/tracking-targets`로 보내 동기화
     2. `passScheduleStore.startScheduleTracking()` → `PassScheduleService.startScheduleTracking()` 호출
     3. 차트 경로 캐시 로딩 및 WebSocket 상태 로그를 강제 출력
   - Stop/Stow 버튼은 각각 `stopScheduleTracking()` + ICD stop 명령, `stopTrackingMonitor()` + `stowCommand()`를 호출한다.

3. **상태 머신 흐름 (100ms 주기 `checkTrackingScheduleWithStateMachine()`)**
   - `currentSchedule = getCurrentSelectedTrackingPassWithTime(calTime)`  
     `nextSchedule = getNextSelectedTrackingPassWithTime(calTime)`
   - 상태 결정:
     - **TRACKING**: 현재 시간이 `StartTime~EndTime` 범위에 있으면 즉시 진입. `prepareTrackingStart()`가 헤더/추적 데이터를 송신하고 `currentTrackingMstId`를 PushData에 반영.
     - **PREPARING**: 다음 스케줄까지 2분(120초) 이하일 때.
       1. `PreparingStep.INIT` → `moveToStartPosition()`에서 DTL 첫 포인트를 읽고 Train 회전 대상 MST ID(`preparingPassId`) 셋업
       2. `MOVING_TRAIN` → Keyhole 여부에 따라 Train 각도 이동 (`moveTrainToZero`)
       3. `WAITING_TRAIN` → 3초(TIMEOUT) 동안 안정화 확인 (`isTrainStabilized`)
       4. `MOVING_AZ_EL` → 목표 Az/El 이동 (`moveToTargetAzEl`)
     - **WAITING**: 2분 이상 남은 경우 Stow 위치 유지(`moveToStowPosition`) 하며 `dataStoreService.setPassScheduleTracking(false)`
     - **COMPLETED**: 현재/다음 모두 없으면 종료. `cleanupTrackingEnd()`가 캐시 삭제와 추적 상태 해제를 수행.
   - 상태 전환 시 `updateTrackingMstIds()`가 항상 `currentTrackingMstId`/`nextTrackingMstId`를 갱신하여 Schedule Control 하이라이트를 유지한다.

4. **하이라이트 및 차트 업데이트**
   - `PassSchedulePage.vue`는 WebSocket 값 변화를 watch하여
     - 현재 MST ID가 있으면 초록색(실제 추적) 행으로 표시, 없고 다음만 있으면 파란색(대기) 행으로 표시.
     - `loadSelectedScheduleTrackingPath()`가 해당 MST의 Keyhole 여부에 따라 적절한 DTL DataType을 로드하고 `PassChartUpdatePool`에 주입.
   - PREPARING 단계에서 Train/Start 위치로 이동하는 동안에도 현재/다음 표시가 변경되므로 UI가 바로 업데이트된다.

5. **다음 스케줄 관리**
   - `trackingTargetList`에 등록된 MST 목록을 순서대로 순회하며, 현재 시간 기준으로 `current`/`next`를 계속 재계산한다.
   - 한 패스가 종료되면 `currentTrackingMstId`는 `null`, `nextTrackingMstId`는 바로 뒤 스케줄로 자동 이동. `startScheduleTracking()`이 계속 실행 중이면 상태 머신이 다음 스케줄을 PREPARING/WAITING/ TRACKING 상태로 자연스럽게 전환한다.

6. **단일 위성 추적 시퀀스 요약**
   1. 스케줄 선택 → `/tracking-targets` 등록
   2. 필요 시 Go(Start) → 상태 머신 가동
   3. WAITING (Stow) → 시작 2분 전 PREPARING (Train → Az/El) → TRACKING (헤더/실시간 데이터 전송) → COMPLETED (Stow 복귀)
   4. 다음 스케줄이 있으면 `next`가 자동으로 PREPARING/WAITING 상태로 넘어가며, 없으면 상태 머신은 IDLE/COMPLETED 상태로 멈춘다 (Stop 버튼이나 모니터링 종료 시 `stopScheduleTracking()` 호출)


