# ACS 데이터 흐름

> Frontend ↔ Backend ↔ 안테나 간 데이터가 어떻게 흐르는지

## 전체 데이터 흐름

```
┌──────────────────────────────────────────────────────────────────┐
│                         Frontend (Vue)                            │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│  │ 컴포넌트     │ ←→ │ Pinia Store │ ←→ │ WebSocket   │           │
│  │ (화면)      │    │ (상태)      │    │ (실시간)    │           │
│  └─────────────┘    └─────────────┘    └─────────────┘           │
└──────────────────────────────────────────────────────────────────┘
         │ 사용자 액션              │ 30ms마다 데이터
         ▼                         ▼
┌──────────────────────────────────────────────────────────────────┐
│                    REST API / WebSocket                           │
└──────────────────────────────────────────────────────────────────┘
         │                         │
         ▼                         ▼
┌──────────────────────────────────────────────────────────────────┐
│                        Backend (Spring)                           │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│  │ Controller  │ →  │ Service     │ →  │ Algorithm   │           │
│  │ (요청처리)   │    │ (로직)      │    │ (계산)      │           │
│  └─────────────┘    └─────────────┘    └─────────────┘           │
│                            │                                      │
│                     ┌──────┴──────┐                               │
│                     ▼             ▼                               │
│              ┌───────────┐  ┌───────────┐                        │
│              │ ICD통신    │  │ WebSocket │                        │
│              │ (UDP)     │  │ Push      │                        │
│              └───────────┘  └───────────┘                        │
└──────────────────────────────────────────────────────────────────┘
                     │
                    UDP
                     │
                     ▼
┌──────────────────────────────────────────────────────────────────┐
│                    안테나 제어장비 (ACU)                           │
└──────────────────────────────────────────────────────────────────┘
```

---

## 주요 데이터 흐름 시나리오

### 1. 실시간 안테나 상태 표시

```
[30ms마다 반복]

안테나(ACU) → UDP 패킷 전송
    │
    ▼
Backend: UdpFwICDService
    │ 바이너리 파싱
    ▼
Backend: GlobalData 업데이트
    │
    ▼
Backend: PushDataController
    │ WebSocket으로 브로드캐스트
    ▼
Frontend: icdStore (WebSocket 수신)
    │ shallowRef 업데이트
    ▼
Frontend: 컴포넌트 (화면 갱신)
    │ Vue 반응형
    ▼
사용자: 화면에서 Az/El/속도 확인
```

**관련 코드**:
- `UdpFwICDService.kt` - UDP 수신 및 파싱
- `PushDataController.kt` - WebSocket 브로드캐스트
- `icdStore.ts` - 상태 저장 및 반응형

---

### 2. 위성 추적 시작 명령

```
사용자: "추적 시작" 버튼 클릭
    │
    ▼
Frontend: 컴포넌트
    │ @click 이벤트
    ▼
Frontend: API 호출
    │ axios.post('/api/ephemeris/track/start')
    ▼
Backend: EphemerisController
    │ @PostMapping
    ▼
Backend: EphemerisService
    │ 추적 상태 변경
    │ 궤도 계산 시작
    ▼
Backend: Algorithm (Orekit)
    │ TLE → 위치 계산
    ▼
Backend: UdpFwICDService
    │ 안테나 명령 생성
    ▼
안테나(ACU): UDP 명령 수신
    │ 안테나 이동 시작
    ▼
안테나 → Backend → Frontend
    │ 실시간 위치 피드백
    ▼
사용자: 화면에서 추적 상태 확인
```

**관련 코드**:
- `EphemerisDesignationPage.vue` - UI
- `EphemerisController.kt` - API 엔드포인트
- `EphemerisService.kt` - 추적 로직
- `OrbitCalculator.kt` - Orekit 계산

---

### 3. 설정 저장

```
사용자: 설정 값 변경 → "저장" 클릭
    │
    ▼
Frontend: SettingsModal.vue
    │ v-model로 바인딩된 값
    ▼
Frontend: settingsStore
    │ saveSettings() 액션
    ▼
REST API: POST /api/settings
    │ JSON body
    ▼
Backend: SettingsController
    │ @RequestBody @Valid
    ▼
Backend: SettingsService
    │ 검증 + 저장
    ▼
Backend: 파일 시스템 (settings.json)
    │
    ▼
Frontend: 성공 응답 수신
    │
    ▼
사용자: "저장 완료" 알림
```

---

## 통신 방식별 용도

### REST API (HTTP)
```
용도: 일회성 요청/응답
├── 설정 조회/저장
├── TLE 업로드
├── 패스 스케줄 CRUD
└── 시스템 정보 조회

특징:
├── 요청 → 응답 → 연결 종료
├── 상태 없음 (Stateless)
└── 캐싱 가능
```

### WebSocket
```
용도: 실시간 양방향 통신
├── 안테나 상태 (30ms 주기)
├── 추적 진행 상태
├── 알람/이벤트 푸시
└── 로그 스트리밍

특징:
├── 연결 유지 (Persistent)
├── 서버 → 클라이언트 푸시 가능
└── 낮은 오버헤드
```

### UDP (ICD)
```
용도: 안테나 장비 통신
├── 위치 명령 전송
├── 상태 데이터 수신
├── 제어 명령 (MC On/Off 등)
└── 실시간 피드백

특징:
├── 빠름 (TCP보다)
├── 연결 없음 (Connectionless)
├── 패킷 손실 가능
└── 바이너리 프로토콜 (ICD 규격)
```

---

## 상태 동기화 흐름

### Frontend 상태 관리

```
┌─────────────────────────────────────────────────────┐
│                    Pinia Stores                      │
├─────────────────────────────────────────────────────┤
│  icdStore          │ 실시간 안테나 데이터 (WebSocket) │
│  passScheduleStore │ 패스 스케줄 목록                │
│  settingsStore     │ 시스템 설정                     │
│  modeStore         │ 현재 모드 상태                  │
└─────────────────────────────────────────────────────┘
          │
          │ storeToRefs()로 반응형 연결
          ▼
┌─────────────────────────────────────────────────────┐
│               Vue 컴포넌트                           │
│  computed(), watch()로 파생/감시                     │
└─────────────────────────────────────────────────────┘
```

### Backend 상태 관리

```
┌─────────────────────────────────────────────────────┐
│                   GlobalData (object)                │
├─────────────────────────────────────────────────────┤
│  Offset       │ 오프셋 값들                          │
│  Time         │ 시간 정보                            │
│  TrackingData │ 추적 각도                            │
└─────────────────────────────────────────────────────┘
          │
          │ 서비스에서 읽기/쓰기
          ▼
┌─────────────────────────────────────────────────────┐
│               Services                               │
│  EphemerisService, PassScheduleService, ...         │
└─────────────────────────────────────────────────────┘
```

---

## 에러 처리 흐름

### Frontend 에러 처리

```
API 호출 실패
    │
    ▼
catch 블록 또는 onError
    │
    ▼
useErrorHandler composable
    │
    ├── 사용자 알림 (q-notify)
    ├── 콘솔 로깅
    └── (필요시) 에러 리포팅
```

### Backend 에러 처리

```
예외 발생
    │
    ▼
GlobalExceptionHandler
    │
    ├── 예외 타입별 HTTP 상태 코드
    ├── 에러 응답 포맷팅
    └── 로깅
    │
    ▼
클라이언트에 에러 응답
```

---

## 실시간 데이터 갱신 주기

| 데이터 | 주기 | 소스 |
|--------|------|------|
| 안테나 위치 (Az/El) | 30ms | ACU → UDP |
| 추적 상태 | 100ms | Service 계산 |
| 패스 예측 | 1초 | Orekit 계산 |
| 시스템 상태 | 1초 | 내부 모니터링 |

---

**이전**: [tech-stack.md](./tech-stack.md) - 기술 스택
**다음**: [glossary.md](./glossary.md) - 용어 사전
