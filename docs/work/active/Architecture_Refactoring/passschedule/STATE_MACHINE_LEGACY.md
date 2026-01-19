# 상태머신 리팩토링 계획

> **작성일**: 2026-01-19
> **상태**: ✅ 검토 완료 - 수정 진행
> **목적**: PassSchedule 버그 수정 + FE-BE 상태머신 정리

---

## 1. 현황 분석

### 1.1 BE 상태머신 현황

| 서비스 | 현재 상태 수 | 문제점 |
|--------|-------------|--------|
| **SunTrack** | 4개 | ✅ 적절 |
| **Ephemeris** | 6+3=9개 | ✅ 적절 |
| **PassSchedule** | 5+4+11=20개 | ⚠️ v1.0/v2.0 혼재 |

### 1.2 FE 상태 관리 현황

| 스토어 | 위치 | 문제점 |
|--------|------|--------|
| modeStore (공용) | `stores/common/modeStore.ts` | selectedMode vs activeMode 분리 |
| modeStore (ICD) | `stores/icd/modeStore.ts` | 중복 존재 |

### 1.3 문서 동기화 현황

| 서비스 | 문서 정의 | 실제 코드 | 일치율 |
|--------|----------|----------|--------|
| SunTrack | ❌ 없음 | 4개 | 0% |
| Ephemeris | 6+3개 | 6+3개 | 85% |
| PassSchedule | 5개 | 11개 (v2.0) | 30% |

---

## 2. 목표 상태

### 2.1 BE 상태머신 (확정)

#### SunTrack (4개)

```
IDLE → MOVING_TRAIN → STABILIZING (3초) → TRACKING
  │                                           │
  └───────────── Stop ────────────────────────┘
```

| 상태 | 설명 | 전이 조건 |
|------|------|----------|
| `IDLE` | 대기 | Go 버튼 → MOVING_TRAIN |
| `MOVING_TRAIN` | Train 각도 이동 | 도달 → STABILIZING |
| `STABILIZING` | 3초 안정화 대기 | 3초 경과 → TRACKING |
| `TRACKING` | Az/El 태양 추적 | Stop → IDLE |

#### Ephemeris (6+3개) - 유지

```
메인: IDLE → PREPARING → WAITING → TRACKING → COMPLETED → ERROR
서브: TRAIN_MOVING → TRAIN_STABILIZING → MOVING_TO_TARGET
```

#### PassSchedule (8개) - 단순화

```
현재 v2.0 (11개):
IDLE → STOWING → STOWED → MOVING_TRAIN → TRAIN_STABILIZING
    → MOVING_TO_START → READY → TRACKING → POST_TRACKING → COMPLETED → ERROR

목표 (8개):
IDLE → WAITING → MOVING_TRAIN → TRAIN_STABILIZING
    → PREPARING → TRACKING → COMPLETED → ERROR
```

| 통합 대상 | 결과 |
|----------|------|
| STOWING + STOWED | → WAITING |
| MOVING_TO_START + READY | → PREPARING |
| POST_TRACKING | → COMPLETED 내 로직 |

### 2.2 FE 상태 관리 (확정)

```
stores/
├── common/
│   └── modeStore.ts  ← 단일 모드 스토어로 통합
└── icd/
    ├── icdStore.ts   ← 추적 상태 (ephemerisTrackingState 등)
    └── modeStore.ts  ← 삭제 예정
```

---

## 3. 리팩토링 작업 목록

### Phase 1: BE PassSchedule v1.0 제거

| 작업 | 파일 | 상세 |
|------|------|------|
| 1.1 | PassScheduleService.kt | `TrackingState` enum 삭제 |
| 1.2 | PassScheduleService.kt | `PreparingStep` enum 삭제 |
| 1.3 | PassScheduleService.kt | `currentTrackingState` 변수 삭제 |
| 1.4 | PassScheduleService.kt | `useV2StateMachine` 플래그 삭제 |
| 1.5 | PassScheduleService.kt | v1.0 관련 함수 삭제 |

**예상 삭제 라인**: ~200줄

### Phase 2: BE PassSchedule v2.0 단순화

| 작업 | 변경 |
|------|------|
| 2.1 | `STOWING` + `STOWED` → `WAITING` 통합 |
| 2.2 | `MOVING_TO_START` + `READY` → `PREPARING` 통합 |
| 2.3 | `POST_TRACKING` → `COMPLETED` 로직 통합 |
| 2.4 | `PassScheduleState` → `TrackingState`로 이름 변경 |

### Phase 3: FE modeStore 통합

| 작업 | 파일 | 상세 |
|------|------|------|
| 3.1 | `stores/icd/modeStore.ts` | 삭제 |
| 3.2 | `stores/common/modeStore.ts` | 통합 로직 |
| 3.3 | 사용처 | import 경로 수정 |

### Phase 4: 문서 업데이트

| 작업 | 파일 |
|------|------|
| 4.1 | `docs/architecture/context/domain/mode-system.md` |
| 4.2 | `docs/architecture/context/domain/satellite-tracking.md` |
| 4.3 | `docs/architecture/context/architecture/backend.md` |

---

## 4. 위험 요소

| 위험 | 영향 | 대응 |
|------|------|------|
| v1.0 제거 시 폴백 기능 상실 | 낮음 | v2.0이 기본값으로 이미 동작 중 |
| 상태 통합 시 전이 로직 버그 | 중간 | 단위 테스트 필수 |
| FE import 경로 누락 | 낮음 | grep으로 전체 검색 |

---

## 5. 검증 계획

### 5.1 BE 검증

```bash
# 빌드 확인
cd backend && ./gradlew clean build -x test

# 서버 시작 확인
./gradlew bootRun
```

### 5.2 FE 검증

```bash
# 빌드 확인
cd frontend && npm run build

# 타입 체크
npm run type-check
```

### 5.3 통합 검증

| 테스트 | 확인 사항 |
|--------|----------|
| SunTrack | Go → Train 이동 → 3초 대기 → 태양 추적 |
| Ephemeris | TLE 입력 → 스케줄 생성 → 추적 시작/종료 |
| PassSchedule | 위성 추가 → 스케줄 → 자동 추적 |

---

## 6. 일정

| Phase | 작업 | 예상 |
|-------|------|------|
| 1 | v1.0 제거 | - |
| 2 | v2.0 단순화 | - |
| 3 | FE 통합 | - |
| 4 | 문서 업데이트 | - |
| 5 | 통합 테스트 | - |

---

## 7. 승인

| 역할 | 상태 | 의견 |
|------|------|------|
| architect | ✅ 조건부 승인 | SunTrack 상태 이름 수정 필요 |
| tech-lead | ✅ 조건부 진행 | v2.0 단순화는 테스트 후 판단 |
| code-reviewer | ✅ 수정 필요 | 삭제 라인 310줄로 수정 |
| debugger | ✅ 분석 완료 | PassSchedule Critical 버그 4건 발견 |
| fullstack-helper | ✅ 분석 완료 | FE passScheduleTrackingStateInfo 누락 |

---

## 8. 발견된 버그 (전문가 분석)

### 8.1 BE Critical 버그

| # | 위치 | 문제 | 영향 |
|---|------|------|------|
| 1 | `resetTrackingState()` L851-857 | `currentPreparingStep`, `preparingPassId` 초기화 누락 | 재시작 시 상태 꼬임 |
| 2 | `useV2StateMachine` L239,543 | v1.0/v2.0 플래그 명시적 설정 없음 | v1.0 호출 시에도 v2.0 실행 |
| 3 | `currentTrackingState` vs `currentPassScheduleState` | 이중 상태 변수 독립 존재 | 상태 불일치 |
| 4 | `evaluateV2NextSchedule()` | 컨텍스트 전환 시 공유 변수 초기화 안됨 | 다중 위성 전환 오류 |

### 8.2 FE Critical 문제

| # | 문제 | 영향 |
|---|------|------|
| 1 | `passScheduleTrackingStateInfo` computed 속성 없음 | V2 상태 (10개)가 UI에 표시 안됨 |
| 2 | PassSchedulePage가 BE 상태 미사용 | mstId 기반 '추적중'/'대기중'만 표시 |
| 3 | ERROR 상태 미처리 | 오류 시 사용자 피드백 없음 |

### 8.3 Ephemeris와 비교

| 항목 | Ephemeris (정상) | PassSchedule (버그) |
|------|-----------------|-------------------|
| 상태 머신 | 단일 (6개) | v1.0 + v2.0 혼재 (20개) |
| 상태 초기화 | 완전 | 불완전 (일부 누락) |
| FE computed | `ephemerisTrackingStateInfo` ✅ | **없음** ❌ |
| 시간 판단 | ms 단위 | 분 단위 (정밀도 낮음) |
| 위치 도달 확인 | Az/El/Train 모두 | Train만 (Az/El 즉시 완료) |

---

## 9. 진행 체크리스트

### Phase 1: FE 상태 표시 수정
- [x] icdStore.ts에 `passScheduleTrackingStateInfo` computed 추가 ✅
- [x] icdStore.ts에서 export 추가 ✅
- [x] ScheduleInfoPanel.vue Props에 trackingStateInfo 추가 ✅
- [x] PassSchedulePage.vue에서 prop 전달 ✅
- [x] 빌드 검증 (`npm run build`) ✅ (2026-01-19)

### Phase 2: BE 상태 초기화 수정
- [x] `PREPARATION_TIME_MINUTES` 상수 제거 → `settingsService.preparationTimeMinutes`로 변경 ✅
- [x] `V2_PREPARATION_TIME_MS` 상수 제거 → `preparationTimeMs` getter로 변경 ✅
- [x] v1/v2 로직의 모든 하드코딩 2분 → Settings 연동 (4곳) ✅
- [x] 빌드 검증 (`./gradlew build -x test`) ✅ (2026-01-19)

### Phase 3: v1.0 코드 제거
- [ ] `TrackingState` enum 삭제 (L73-88)
- [ ] `PreparingStep` enum 삭제 (L95-107)
- [ ] `currentTrackingState` 변수 삭제
- [ ] `currentPreparingStep` 변수 삭제
- [ ] `useV2StateMachine` 플래그 제거
- [ ] v1.0 관련 함수 삭제 (~310줄)
- [ ] 빌드 검증

### Phase 4: v2.0 상태머신 개선 (테스트 후)
- [ ] 시간 판단 정밀도 개선 (분 → ms)
- [ ] Az/El 도달 확인 로직 추가
- [ ] 상태 단순화 검토 (11개 → 8개)

### Phase 5: 문서 업데이트
- [ ] `mode-system.md` 업데이트
- [ ] `satellite-tracking.md` 업데이트
- [ ] `backend.md` 업데이트

---

## 10. 확정된 수정 계획

### Phase 1: FE 상태 표시 수정 (즉시)

```
1. icdStore.ts에 passScheduleTrackingStateInfo computed 추가
2. PassSchedulePage에서 상태 정보 활용
3. ScheduleInfoPanel에 추적 상태 Chip 추가
```

**파일:**
- `frontend/src/stores/icd/icdStore.ts`
- `frontend/src/pages/mode/PassSchedulePage.vue`

### Phase 2: BE 상태 초기화 수정 (즉시)

```
1. resetTrackingState()에 누락된 변수 초기화 추가
   - currentPreparingStep = PreparingStep.INIT
   - preparingPassId = null
   - targetAzimuth = 0f
   - targetElevation = 0f
   - trainStabilizationStartTime = 0
```

**파일:**
- `backend/.../service/mode/PassScheduleService.kt`

### Phase 3: v1.0 코드 제거 (다음 단계)

```
1. TrackingState enum 삭제
2. PreparingStep enum 삭제
3. v1.0 관련 함수 삭제 (~310줄)
4. useV2StateMachine 플래그 제거
```

### Phase 4: v2.0 상태머신 개선 (선택)

```
1. 시간 판단 정밀도 개선 (분 → ms)
2. Az/El 도달 확인 로직 추가
3. 11개 → 8개 상태 단순화 (테스트 후 판단)
```

---

## 10. 즉시 수정 코드

### 10.1 FE: passScheduleTrackingStateInfo 추가

**위치:** `frontend/src/stores/icd/icdStore.ts` (ephemerisTrackingStateInfo 근처)

```typescript
const passScheduleTrackingStateInfo = computed(() => {
  const state = passScheduleTrackingState.value
  switch (state) {
    // V2 상태
    case 'IDLE': return { displayLabel: '정지', displayColor: 'grey' }
    case 'STOWING': return { displayLabel: 'Stow 이동', displayColor: 'blue' }
    case 'STOWED': return { displayLabel: 'Stow 대기', displayColor: 'blue-grey' }
    case 'MOVING_TRAIN': return { displayLabel: 'Train 이동', displayColor: 'deep-orange' }
    case 'TRAIN_STABILIZING': return { displayLabel: 'Train 안정화', displayColor: 'amber-7' }
    case 'MOVING_TO_START': return { displayLabel: '시작위치 이동', displayColor: 'cyan' }
    case 'READY': return { displayLabel: '추적 준비완료', displayColor: 'light-green' }
    case 'TRACKING': return { displayLabel: '추적 중', displayColor: 'green' }
    case 'POST_TRACKING': return { displayLabel: '추적 후 처리', displayColor: 'teal' }
    case 'COMPLETED': return { displayLabel: '완료', displayColor: 'purple' }
    case 'ERROR': return { displayLabel: '오류', displayColor: 'red' }
    // V1 호환
    case 'WAITING': return { displayLabel: '대기 중', displayColor: 'blue-grey' }
    case 'PREPARING': return { displayLabel: '준비 중', displayColor: 'orange' }
    default: return { displayLabel: '알 수 없음', displayColor: 'grey' }
  }
})
```

### 10.2 BE: resetTrackingState() 수정

**위치:** `PassScheduleService.kt` L851-857

```kotlin
private fun resetTrackingState() {
    currentTrackingState = TrackingState.IDLE
    currentPreparingStep = PreparingStep.INIT  // 추가
    preparingPassId = null  // 추가
    targetAzimuth = 0f  // 추가
    targetElevation = 0f  // 추가
    trainStabilizationStartTime = 0L  // 추가
    logger.debug("🔄 추적 상태 초기화 완료")
}
```

---

**Last Updated**: 2026-01-19
