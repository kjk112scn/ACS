# FE-BE 데이터 동기화 최적화 (SWR 패턴)

## 개요

**목적**: PassSchedule 데이터 로딩 최적화 - Cache-First + Background Validation (SWR 패턴)
**요청일**: 2026-01-21
**상태**: 📋 계획됨 (이후 작업)
**우선순위**: P2 (기능 완성 후 최적화)
**예상 소요**: 2~3주

## 핵심 아이디어

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Stale-While-Revalidate (SWR) 패턴                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [기존 방식]                                                                 │
│  페이지 진입 → 서버 요청 → 로딩... → 화면 표시 (2-3초)                       │
│                                                                             │
│  [SWR 방식]                                                                  │
│  페이지 진입 → 캐시로 즉시 표시! → 백그라운드 버전 확인 → 변경 시 갱신       │
│               (0ms)                                                         │
│                                                                             │
│  효과: 2번째 방문부터 즉시 로딩 (새로고침, 탭 전환, 재방문 등)                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 전문가 검토 결과

### 적합성 판정: ⚠️ 부분 적합

| 데이터 유형 | SWR 적합? | 권장 전략 | 이유 |
|------------|:--------:|----------|------|
| 스케줄 목록 (MST) | ⚠️ 조건부 | Server-First + 5분 캐시 | TLE 의존성, 하루 단위 갱신 |
| 예측 경로 (DTL) | ❌ 부적합 | 요청 시 로드, 캐싱 금지 | 대용량 (15MB+), localStorage 초과 |
| 선택된 스케줄 ID | ✅ 적합 | localStorage + SWR | UI 상태, 빠른 복원 필요 |
| TLE 데이터 | ❌ 부적합 | Server-First | 업로드 시 전체 무효화 필요 |
| 모니터링 상태 | ✅ 적합 | SWR (5분 TTL) | 상태 확인용, stale 허용 |

### 핵심 제약사항

```yaml
⚠️ ACS는 안전 필수 시스템:
  - 추적 시작 시: 반드시 Server-First (최신 데이터 필수)
  - DTL 경로 데이터: 캐싱 금지 (localStorage 용량 초과)
  - TLE 업로드 후: 전체 캐시 강제 무효화
  - stale 데이터 사용 시: UI에 명시 표시
```

## 기술 설계

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Frontend                                                                   │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  usePassScheduleSWR (새 Composable)                                  │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐  │  │
│  │  │ scheduleData    │  │ selectedIds     │  │ trackingPath       │  │  │
│  │  │ (Server-First   │  │ (Cache-First)   │  │ (On-Demand)        │  │  │
│  │  │  + 5분 캐시)    │  │                 │  │                    │  │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                │                                                           │
│                ▼                                                           │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  GET /api/v1/pass-schedule/version                                   │  │
│  │  응답: { version: Long, lastModified: Long }                         │  │
│  │                                                                      │  │
│  │  버전 동일 → 캐시 사용                                                │  │
│  │  버전 다름 → 전체 데이터 재요청                                        │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  Backend                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  PassScheduleStateManager (새 컴포넌트)                               │  │
│  │  - writeCounter (AtomicLong) 활용                                    │  │
│  │  - TLE 업로드 시 버전 증가                                            │  │
│  │  - 서버 재시작 시 버전 0 → FE에서 감지                                 │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 핵심 코드 패턴

```typescript
// FE: 백그라운드 검증 (await 없이 호출 = 백그라운드 실행)
const loadSchedules = () => {
  // 1. 캐시 즉시 로드 (0ms)
  const cached = loadFromLocalStorage()
  if (cached) {
    scheduleData.value = cached.data
  }

  // 2. 백그라운드 검증 시작 (await 없음!)
  validateAndRefresh()  // ← 비동기로 실행
}

const validateAndRefresh = async () => {
  const serverVersion = await api.get('/pass-schedule/version')
  const cached = loadFromLocalStorage()

  if (cached?.version === serverVersion) {
    return  // 캐시 유효 - 아무것도 안함
  }

  // 버전 다름 - 새 데이터 요청 + 자동 갱신
  const newData = await api.get('/pass-schedule/schedules')
  scheduleData.value = newData  // Vue 반응성으로 UI 자동 업데이트
  saveToLocalStorage({ data: newData, version: serverVersion })
}
```

## 구현 계획

### Phase 1: BE 버전 API (1~2일)

| 작업 | 파일 | 설명 |
|------|------|------|
| StateManager 추가 | `PassScheduleStateManager.kt` | writeCounter 기반 버전 관리 |
| 버전 API 엔드포인트 | `PassScheduleController.kt` | GET /version |
| 버전 증가 연동 | `PassScheduleService.kt` | TLE 업로드 시 버전++ |

### Phase 2: FE SWR Composable (2~3일)

| 작업 | 파일 | 설명 |
|------|------|------|
| SWR composable 생성 | `usePassScheduleSWR.ts` | 캐시 로드 + 백그라운드 검증 |
| 캐시 매니저 통합 | `cacheManager.ts` | localStorage 추상화 |
| 버전 체크 로직 | `passScheduleService.ts` | 버전 API 호출 |

### Phase 3: Store 리팩토링 (3~5일)

| 작업 | 파일 | 설명 |
|------|------|------|
| init() 수정 | `passScheduleStore.ts` | SWR 패턴 적용 |
| fetchScheduleData() 수정 | `passScheduleStore.ts` | 조건부 로드 |
| 마이그레이션 로직 | `passScheduleStore.ts` | 기존 localStorage 변환 |

### Phase 4: 테스트 & 안정화 (2~4일)

| 테스트 시나리오 | 예상 동작 |
|----------------|----------|
| 새로고침 후 캐시 히트 | 즉시 표시 + 백그라운드 검증 |
| BE 재시작 감지 | 버전 0 감지 → 캐시 무효화 |
| TLE 업로드 후 | 캐시 무효화 → 재로드 |
| 오프라인 상태 | 캐시 폴백 + 오프라인 표시 |

## 리스크 평가

| 리스크 | 확률 | 영향 | 완화 전략 |
|--------|:----:|:----:|----------|
| 기존 데이터 손실 | 낮 | 높 | 7일 지연 삭제 + 마이그레이션 |
| FE-BE 버전 불일치 | 중 | 중 | 버전 0 감지 + 강제 동기화 |
| localStorage 용량 초과 | 낮 | 낮 | DTL 캐싱 금지 (MST만) |
| 복잡도 증가 버그 | 중 | 중 | Feature flag + 점진적 배포 |

## 필수 조건

```yaml
Feature_Flag:
  env: VITE_USE_SWR_PATTERN=true/false
  목적: 문제 발생 시 즉시 롤백

BE_재시작_감지:
  기존: connectionManager 메커니즘 유지
  추가: 버전 0 감지 로직

TLE_업로드_후:
  필수: 전체 캐시 강제 무효화

추적_시작_전:
  필수: 서버에서 최신 데이터 확인 (캐시 사용 금지)
```

## 예상 효과

| 시나리오 | 현재 | SWR 적용 후 |
|---------|:----:|:----------:|
| 첫 방문 | 2-3초 | 2-3초 (동일) |
| 새로고침 | 2-3초 | **0ms** ✨ |
| 페이지 재방문 | 2-3초 | **0ms** ✨ |
| 다음날 접속 | 2-3초 | **0ms** ✨ |
| TLE 업로드 후 | 2-3초 | 즉시 + 자동갱신 ✨ |

## 관련 문서

- [DESIGN.md](DESIGN.md) - 상세 설계
- [ADR-007.md](ADR-007.md) - 아키텍처 결정 기록
- [CHECKLIST.md](CHECKLIST.md) - 구현 체크리스트

## 선행 작업

- [ ] Tracking_Session_Data_Enrichment 완료
- [ ] Admin_Panel_Separation 완료 (선택)

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-01-21 | 초안 작성, 전문가 검토 완료 |
