# 구현 체크리스트

## Phase 1: BE 버전 API (예상: 1~2일)

### 필수 작업

- [ ] `PassScheduleStateManager.kt` 생성
  ```kotlin
  @Component
  class PassScheduleStateManager {
      private val dataVersion = AtomicLong(0)
      private val instanceId = UUID.randomUUID().toString()

      fun getVersion() = dataVersion.get()
      fun incrementVersion() = dataVersion.incrementAndGet()
      fun getInstanceId() = instanceId
  }
  ```

- [ ] 버전 API 엔드포인트 추가
  ```
  GET /api/v1/pass-schedule/version
  응답: { version: Long, instanceId: String, lastModified: Long }
  ```

- [ ] `PassScheduleService` 연동
  - [ ] TLE 업로드 완료 시 `stateManager.incrementVersion()`
  - [ ] 전체 삭제 시 `stateManager.incrementVersion()`

- [ ] 단위 테스트
  - [ ] 버전 증가 검증
  - [ ] 동시 쓰기 시 버전 일관성

### 선택 작업 (향후)

- [ ] If-None-Match 헤더 지원 (304 응답)
- [ ] WebSocket에 version 필드 추가

---

## Phase 2: FE SWR Composable (예상: 2~3일)

### 필수 작업

- [ ] `usePassScheduleCache.ts` composable 생성
  ```typescript
  interface CacheEntry<T> {
    data: T
    version: number
    instanceId: string
    cachedAt: number
  }

  export function usePassScheduleCache() {
    const loadFromCache = <T>(key: string): CacheEntry<T> | null
    const saveToCache = <T>(key: string, data: T, meta: CacheMeta): void
    const invalidateCache = (key: string): void
    const validateCache = async (key: string): Promise<boolean>
  }
  ```

- [ ] 버전 체크 API 서비스 추가
  ```typescript
  // passScheduleService.ts
  getDataVersion(): Promise<{ version: number, instanceId: string }>
  ```

- [ ] 백그라운드 검증 로직 구현
  ```typescript
  const validateAndRefresh = async () => {
    const serverMeta = await passScheduleService.getDataVersion()
    const cached = loadFromCache('schedules')

    // BE 재시작 감지 (instanceId 변경)
    if (cached?.instanceId !== serverMeta.instanceId) {
      invalidateAllCache()
      return fetchFresh()
    }

    // 버전 비교
    if (cached?.version < serverMeta.version) {
      return fetchFresh()
    }

    return cached.data
  }
  ```

### 테스트

- [ ] 캐시 히트 시나리오
- [ ] 버전 불일치 시나리오
- [ ] BE 재시작 감지 시나리오

---

## Phase 3: Store 리팩토링 (예상: 3~5일)

### 필수 작업

- [ ] `passScheduleStore.ts` init() 수정
  ```typescript
  const init = async () => {
    // 1. 캐시 즉시 로드 (await 없음)
    loadFromCacheImmediate()

    // 2. 백그라운드 검증 (await 없음!)
    validateAndRefresh()
  }

  const loadFromCacheImmediate = () => {
    const cached = loadFromCache('pass-schedule-data')
    if (cached) {
      scheduleData.value = cached.data
    }
  }
  ```

- [ ] Feature flag 추가
  ```typescript
  const USE_SWR = import.meta.env.VITE_USE_SWR_PATTERN === 'true'

  const init = async () => {
    if (USE_SWR) {
      await initWithSWR()
    } else {
      await initLegacy()  // 기존 로직 유지
    }
  }
  ```

- [ ] 마이그레이션 로직
  ```typescript
  const migrateCache = () => {
    const oldKeys = ['pass-schedule-data', 'pass-schedule-schedule-data']
    // 기존 데이터 변환 + 버전 필드 추가
  }
  ```

- [ ] TLE 업로드 후 캐시 무효화
  ```typescript
  const uploadTLE = async (tleData) => {
    await passScheduleService.uploadTLE(tleData)
    invalidateAllScheduleCache()  // 필수!
    await fetchScheduleDataFromServer()
  }
  ```

### 삭제/정리

- [ ] 불필요한 localStorage 키 정리
- [ ] 중복 캐시 로직 제거

---

## Phase 4: 테스트 & 안정화 (예상: 2~4일)

### 수동 테스트 시나리오

| # | 시나리오 | 예상 동작 | 통과 |
|---|---------|----------|:----:|
| 1 | 첫 방문 | 서버에서 로드 + 캐시 저장 | [ ] |
| 2 | 새로고침 (F5) | 즉시 표시 + 백그라운드 검증 | [ ] |
| 3 | 탭 닫고 다시 열기 | 즉시 표시 + 백그라운드 검증 | [ ] |
| 4 | BE 재시작 후 새로고침 | 캐시 무효화 + 서버 로드 | [ ] |
| 5 | TLE 업로드 후 | 캐시 무효화 + 자동 갱신 | [ ] |
| 6 | 오프라인 상태 | 캐시 폴백 + 오프라인 표시 | [ ] |
| 7 | Feature flag OFF | 기존 로직 동작 | [ ] |

### 성능 측정

| 항목 | 목표 | 측정값 |
|------|------|--------|
| 캐시 히트 시 로딩 | < 100ms | [ ] |
| 버전 API 응답 | < 50ms | [ ] |
| localStorage 읽기 | < 10ms | [ ] |

### 롤백 테스트

- [ ] Feature flag OFF 시 정상 동작
- [ ] 캐시 손상 시 서버 폴백
- [ ] 마이그레이션 실패 시 복구

---

## 배포 체크리스트

### 개발 환경

- [ ] Feature flag: `VITE_USE_SWR_PATTERN=true`
- [ ] BE 버전 API 정상 동작
- [ ] FE 캐시 로직 정상 동작
- [ ] 모든 테스트 시나리오 통과

### QA 환경

- [ ] 1주일 운영 테스트
- [ ] 버그 수정 완료
- [ ] 성능 지표 확인

### 프로덕션

- [ ] Feature flag: `VITE_USE_SWR_PATTERN=false` (초기)
- [ ] 점진적 활성화 (10% → 50% → 100%)
- [ ] 모니터링 대시보드 확인
- [ ] 롤백 계획 준비

---

## 완료 조건

- [ ] 모든 체크리스트 항목 완료
- [ ] 테스트 시나리오 100% 통과
- [ ] 성능 목표 달성
- [ ] 코드 리뷰 완료
- [ ] 문서 업데이트 완료