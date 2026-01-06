# Slew Loop Mode 설계 문서

## 1. 설계 의도

### Why (왜 이 기능이 필요한가)

안테나 성능 테스트 및 검증을 위해 축들이 전체 범위를 자동으로 반복 이동하는 기능이 필요함.
수동으로 매번 Go 버튼을 누르지 않고, 정지 버튼을 누를 때까지 자동 왕복.

### 대안 분석

| 대안 | 장점 | 단점 | 선택 여부 |
|------|------|------|----------|
| Backend 상태 머신 | 안정적, 브라우저 독립 | 개발량 많음, 복잡 | ❌ |
| **Frontend Loop** | 기존 구조 활용, 빠른 개발 | 브라우저 닫으면 중단 | ✅ 선택 |

**선택 이유**: 사용자가 화면을 보면서 제어하는 시나리오에 적합. Stop 버튼이 이미 Frontend에 있음.

## 2. 협의 결정 사항

| 항목 | 결정 |
|------|------|
| Loop 로직 위치 | Frontend (Vue) |
| UI 위치 | 전체 테두리 위, Azimuth 축 위 |
| 3축 이동 방식 | 독립적 (각 축이 자신의 범위에서 각자 왕복) |
| 속도 | 사용자 입력값 그대로 사용 (음수/양수 가능) |
| 시작 방향 | 속도 음수 → Min부터, 속도 양수 → Max부터 |
| 도달 판정 | ±0.5° 연속 3초 OR 모터 ServoMotor 비트 OFF |
| 각도 범위 | angleLimitsSettings의 min/max 값 사용 |

## 3. UI 레이아웃

```
┌─────────────────────────────────────────────────┐
│  [✓] Loop                                       │  ← Loop 체크박스
├─────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │[✓]Azimuth│  │[✓]Elevation│ │[ ] Tilt  │      │
│  │ Speed: 5 │  │ Speed: 3   │ │ Speed: 0 │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                 │
│       [Go]    [Stop]    [Stow]                 │
└─────────────────────────────────────────────────┘
```

## 4. 동작 흐름

### 4.1 Loop 시작 (Go 클릭)

```
1. Loop 체크 상태 확인
2. 선택된 축 확인 (azimuth, elevation, train)
3. 각 축별 시작 방향 결정:
   - 속도 < 0 → 목표 = minAngle
   - 속도 > 0 → 목표 = maxAngle
4. 이동 명령 전송 (sendMultiControlCommand)
5. 모니터링 시작 (setInterval)
```

### 4.2 도달 판정

```typescript
// 각 축별 독립적으로 판정
const isArrived = (actual: number, target: number): boolean => {
  return Math.abs(actual - target) <= 0.5  // ±0.5° 이내
}

// 3초 연속 유지 확인
let stableCount = 0
const checkInterval = setInterval(() => {
  if (isArrived(actual, target)) {
    stableCount++
    if (stableCount >= 3) {  // 3초 (1초 간격 체크 시)
      // 도달 판정 → 방향 전환
    }
  } else {
    stableCount = 0  // 리셋
  }
}, 1000)
```

### 4.3 방향 전환

```
도달 시:
  현재 목표가 minAngle → 새 목표 = maxAngle
  현재 목표가 maxAngle → 새 목표 = minAngle
```

### 4.4 Loop 중단

```
Stop 버튼 클릭 시:
1. 모니터링 interval 정리 (clearInterval)
2. Stop 명령 전송
3. Loop 상태 초기화
```

## 5. 데이터 구조

### 5.1 slewModeStore 확장

```typescript
// 기존
const selectedAxes = ref({
  azimuth: false,
  elevation: false,
  train: false
})

const speeds = ref({
  azimuth: '0.00',
  elevation: '0.00',
  train: '0.00'
})

// 추가
const loopEnabled = ref(false)
const loopRunning = ref(false)

// 각 축별 Loop 상태
const loopState = ref({
  azimuth: {
    currentTarget: null as number | null,  // 현재 목표 (min or max)
    stableCount: 0,                         // 도달 판정 카운터
    direction: 'toMin' | 'toMax'            // 현재 방향
  },
  elevation: { ... },
  train: { ... }
})
```

### 5.2 참조 데이터

```typescript
// angleLimitsSettingsStore에서 가져옴
const limits = {
  azimuthMin: -270,
  azimuthMax: 270,
  elevationMin: 0,
  elevationMax: 180,
  trainMin: -270,
  trainMax: 270
}

// icdStore에서 가져옴
const actual = {
  azimuth: trackingActualAzimuthAngle,
  elevation: trackingActualElevationAngle,
  train: trackingActualTrainAngle
}

// 모터 상태 (선택적)
const motorOff = {
  azimuth: !azimuthBoardServoStatusServoMotor,
  elevation: !elevationBoardServoStatusServoMotor,
  train: !trainBoardServoStatusServoMotor
}
```

## 6. 구현 계획

### Phase 1: UI 추가
- [ ] SlewPage.vue에 Loop 체크박스 추가
- [ ] slewModeStore에 loopEnabled 상태 추가

### Phase 2: Loop 로직 구현
- [ ] startLoop() 함수 구현
- [ ] 도달 판정 로직 구현
- [ ] 방향 전환 로직 구현
- [ ] stopLoop() 함수 구현

### Phase 3: 통합 및 테스트
- [ ] Go 버튼에 Loop 분기 추가
- [ ] Stop 버튼에 Loop 정리 추가
- [ ] 빌드 확인

## 7. 테스트 계획

- [ ] Loop OFF 상태에서 기존 Slew 동작 확인
- [ ] Loop ON + 단일 축 선택 → 왕복 확인
- [ ] Loop ON + 다중 축 선택 → 독립 왕복 확인
- [ ] 이동 중 Stop → 즉시 정지 확인
- [ ] 각도 제한 설정 변경 → 새 범위 적용 확인

## 8. 관련 파일

| 파일 | 역할 |
|------|------|
| `frontend/src/pages/mode/SlewPage.vue` | UI, 이벤트 핸들러 |
| `frontend/src/stores/slewModeStore.ts` | Loop 상태 관리 |
| `frontend/src/stores/api/settings/angleLimitsSettingsStore.ts` | min/max 각도 |
| `frontend/src/stores/icd/icdStore.ts` | 실시간 위치, 모터 상태 |
