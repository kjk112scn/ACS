# ACS v2.0 리팩토링 주간 실행 일정

> **기간**: 2026-01-15 (수) ~ 2026-01-17 (금) [3일]
> **작업자**: 1인 (퇴근 후 추가 작업 포함)
> **목표**: Critical + High 우선순위 완료, 성능 대폭 개선

---

## 일정 개요

```
┌─────────────────────────────────────────────────────────────────┐
│  Day 1 (수)     │  Day 2 (목)     │  Day 3 (금)                 │
├─────────────────┼─────────────────┼─────────────────────────────┤
│  Critical 버그  │  FE 성능 최적화 │  BE 안정성 + 마무리         │
│  + 즉시 정리    │  (icdStore)     │                             │
└─────────────────┴─────────────────┴─────────────────────────────┘
```

---

## Day 1 (수요일) - Critical 버그 + 즉시 정리

> **⚠️ 2026-01-14 전문가 분석 반영**: modeStore 미사용 확인 → 통합 보류, !! P0 추가

### 오전/업무시간 (4시간)

| 시간 | 작업 | 파일 | 예상 |
|:----:|------|------|:----:|
| 1 | BE 테스트 코드 이동 | `OrekitCalculatorTest.kt` → test/ | 10분 |
| 2 | 주석 처리된 코드 정리 | `EphemerisController.kt`, `DataStoreService.kt` | 10분 |
| 3 | FE Dead Code 삭제 | `ExampleComponent.vue`, `example-store.ts`, `models.ts` | 10분 |
| 4 | **!! 연산자 P0 수정** | `EphemerisService.kt:2717`, `SunTrackService.kt:636`, `PassScheduleService.kt:3729` | 25분 |
| 5 | **.subscribe() 에러 핸들러** | `UdpFwICDService.kt:933,195` | 20분 |
| 6 | Thread.sleep → Mono.delay | `UdpFwICDService.kt` (forceReconnect) | 30분 |
| 7 | runBlocking 제거 | `ElevationCalculator.kt` | 30분 |
| 8 | 빌드 확인 (BE + FE) | - | 30분 |

**체크포인트**: 빌드 성공, Critical 블로킹 제거, P0 Null 안전성 확보

### 저녁/퇴근 후 (3-4시간)

| 시간 | 작업 | 파일 | 예상 |
|:----:|------|------|:----:|
| 8 | devLog 유틸 생성 | `utils/logger.ts` | 30분 |
| 9 | DashboardPage console.log | 60개 → devLog | 1시간 |
| 10 | icdStore console.log | - | 30분 |
| 11 | 빌드 확인 | - | 15분 |

**Day 1 완료 기준**:
- [ ] 테스트 코드 main → test 이동
- [ ] Dead Code 69줄 삭제 (ExampleComponent, example-store, models.ts)
- [ ] ~~modeStore 통합~~ (미사용 확인 → **보류**)
- [ ] **!! 연산자 P0 3건 수정** (EphemerisService, SunTrackService, PassScheduleService)
- [ ] **.subscribe() 2건 에러 핸들러 추가**
- [ ] Thread.sleep → Mono.delay (forceReconnect)
- [ ] runBlocking 제거
- [ ] devLog 유틸 생성 + 일부 적용
- [ ] 빌드 성공

---

## Day 2 (목요일) - FE 성능 최적화 (Phase 1)

> **⚠️ 2026-01-14 전문가 분석 반영**: 대용량 데이터 ref만 shallowRef 적용 (15-20개)

### 오전/업무시간 (4시간)

| 시간 | 작업 | 상세 | 예상 |
|:----:|------|------|:----:|
| 1 | **Phase 1: 기본 상태** (9개) | icdStore L49-57: isModeSwitching, isCommutating 등 | 30분 |
| 2 | **Phase 1: 단순 상태** (27개) | icdStore: 문자열/숫자 단독 ref (참조 파일 3개 미만) | 1.5시간 |
| 3 | 단위 테스트 | Phase 1 변경 파일 동작 확인 | 30분 |
| 4 | updateAntennaData 부분 리팩토링 | Phase 1 영역만 정리 | 1시간 |

**체크포인트**: Phase 1 완료 (36개 ref → shallowRef), 참조 파일 정상 동작

### 저녁/퇴근 후 (3-4시간)

| 시간 | 작업 | 상세 | 예상 |
|:----:|------|------|:----:|
| 5 | deep watch 최적화 | 34개 중 상위 10개 (DashboardPage 0개로 안전) | 1시간 |
| 6 | computed 최적화 | 대형 computed 분리 | 30분 |
| 7 | 통합 테스트 | 실시간 추적 동작 확인 | 30분 |
| 8 | Phase 2 준비 | 복잡한 43개 ref 분석 문서화 | 1시간 |

**Day 2 완료 기준**:
- [ ] **Phase 1 shallowRef 적용 (36개)**
- [ ] Phase 1 참조 파일 전체 테스트 통과
- [ ] deep watch 상위 10개 최적화
- [ ] **실시간 추적 정상 동작 확인**
- [ ] Phase 2-4 계획 문서화 (다음 주 진행)

---

## Day 3 (금요일) - BE 안정성 + 마무리

### 오전/업무시간 (4시간)

| 시간 | 작업 | 상세 | 예상 |
|:----:|------|------|:----:|
| 1 | .subscribe() 에러 처리 | 19개 위치 | 1.5시간 |
| 2 | companion object 정리 | ICDService.kt 우선 (19개) | 1.5시간 |
| 3 | print/println 상위 파일 | ICDService, ElevationCalculator | 1시간 |

**체크포인트**: BE 에러 가시성 확보

### 저녁/퇴근 후 (3-4시간)

| 시간 | 작업 | 상세 | 예상 |
|:----:|------|------|:----:|
| 4 | 나머지 console.log 정리 | PassSchedulePage (128개) | 1.5시간 |
| 5 | passScheduleStore | 103개 | 1시간 |
| 6 | 전체 빌드 + 테스트 | BE + FE | 30분 |
| 7 | 문서 업데이트 | 완료 항목 체크 | 30분 |

**Day 3 완료 기준**:
- [ ] .subscribe() 에러 처리 완료
- [ ] companion object 주요 파일 정리
- [ ] print/println 상위 파일 정리
- [ ] console.log 주요 파일 정리
- [ ] **전체 빌드 성공**
- [ ] **모든 기능 정상 동작**

---

## 작업 우선순위 매트릭스

> **⚠️ 2026-01-14 전문가 분석 반영**: 위험도 재평가, 작업 항목 수정

### P0 - 즉시 (Day 1)

| # | 작업 | 효과 | 위험도 |
|---|------|------|:------:|
| 1 | **!! 연산자 P0 3건** | Null 안전성 | **높음** |
| 2 | **.subscribe() 2건** | 에러 가시성 | 중간 |
| 3 | 테스트 코드 이동 | 코드 정리 | 낮음 |
| 4 | Dead Code 삭제 (69줄) | 혼란 제거 | 낮음 |
| 5 | Thread.sleep → Mono.delay | 블로킹 해소 | 중간 |
| 6 | devLog 유틸 | 기반 마련 | 낮음 |

### P1 - 핵심 (Day 2)

| # | 작업 | 효과 | 위험도 |
|---|------|------|:------:|
| 1 | **shallowRef Phase 1** (36개) | CPU 감소 | **중간** (안전한 ref만) |
| 2 | deep watch 최적화 | 재렌더링 감소 | 중간 |
| 3 | Phase 2-4 계획 수립 | 다음 주 준비 | 낮음 |

### P2 - 안정성 (Day 3)

| # | 작업 | 효과 | 위험도 |
|---|------|------|:------:|
| 1 | .subscribe() 나머지 17건 | 에러 가시성 | 낮음 |
| 2 | ~~companion object~~ | ~~동시성 안정~~ | ✅ **모두 안전** (var 0개) |
| 3 | console.log 정리 | 번들 감소 | 낮음 |

---

## 위험 관리

> **⚠️ 2026-01-14 전문가 분석 반영**: 위험 재평가

### 높은 위험 작업

| 작업 | 위험 | 대응 | 상태 |
|------|------|------|:----:|
| **shallowRef Phase 1** (36개) | 참조 파일 영향 | 안전한 ref만 우선 | Day 2 |
| **shallowRef Phase 2-4** (43개) | 20개 파일 동시 영향 | **다음 주로 연기** | 보류 |
| **!! 연산자 P0** (3건) | 런타임 NPE | Elvis 연산자로 변환 | Day 1 |
| ~~modeStore 통합~~ | ~~import 오류~~ | 미사용 확인됨 | **보류** |

### 롤백 전략

```bash
# 각 Day 시작 전 태그 생성
git tag v2.0-day1-start
git tag v2.0-day2-start
git tag v2.0-day3-start

# 문제 발생 시
git revert HEAD
# 또는
git reset --hard v2.0-day2-start
```

### 테스트 체크리스트

| 기능 | 테스트 방법 |
|------|------------|
| 실시간 추적 | ICD 연결 후 30ms 업데이트 확인 |
| 패스 스케줄 | 스케줄 선택/실행 동작 |
| 에페메리스 | 위성 추적 시작/중지 |
| 대시보드 | 차트 업데이트 확인 |

---

## 완료 후 예상 개선

> **⚠️ 2026-01-14 전문가 분석 반영**: 예상치 수정

### 정량적 효과

| 지표 | 현재 | 3일 후 | 개선율 | 비고 |
|------|:----:|:------:|:------:|------|
| shallowRef 적용 | 0개 | 15-20개 | 대용량 데이터 | 성능 향상 |
| CPU 사용률 | 높음 | 중간 | **~30% 감소** | 대용량 ref 최적화 |
| deep watch | 34개 | ~24개 | 29% 감소 | 상위 10개 |
| console.log | 1,513개 | ~1,100개 | 27% 감소 | 주요 파일만 |
| Dead Code | 69줄 | 0줄 | **100%** | 전체 삭제 |
| !! P0 | 3건 | 0건 | **100%** | Critical 완료 |

### 남은 작업: Phase D - 통일 작업 (일관성/유지보수)

> **원칙**: 에러가 나지 않고 코드 품질이 향상된다면 **통일해야 함**

| 작업 | 건수 | 작업량 | 통일 효과 |
|------|:----:|:------:|----------|
| **!important → CSS변수** | 1,690개 | 3-4일 | 테마 일관성, 유지보수 |
| **하드코딩 색상 → 변수** | 220건 | 1-2일 | 다크모드 지원 |
| **catch → GlobalHandler** | 196건 | 2일 | 에러 처리 일관성 |
| **as → Zod 검증** | 313건 | 2-3일 | 타입 안전성 |

### 남은 작업: 기타

| 작업 | 우선순위 | 비고 |
|------|:--------:|------|
| 대형 페이지 분리 | P2 | PassSchedulePage 4,838줄 |
| @Valid 입력 검증 | P2 | BE 보안 |
| 나머지 console.log (~400개) | P3 | |
| !! 연산자 나머지 (43개) | P2 | P0 제외 |

---

## 일별 체크리스트

### Day 1 체크리스트 (2026-01-14 수정)

```
[ ] BE 테스트 코드 이동
[ ] 주석 처리 코드 정리 (EphemerisController.kt 490줄)
[ ] ExampleComponent.vue 삭제 (37줄)
[ ] example-store.ts 삭제 (22줄)
[ ] models.ts 삭제 (9줄)
[ ] !! 연산자 P0 수정 (3건: EphemerisService:2717, SunTrackService:636, PassScheduleService:3729)
[ ] .subscribe() 에러 핸들러 추가 (2건: UdpFwICDService:933,195)
[X] modeStore 통합 → 보류 (미사용 확인)
[ ] Thread.sleep → Mono.delay (forceReconnect)
[ ] runBlocking 제거
[ ] devLog 유틸 생성
[ ] DashboardPage console.log 정리
[ ] 빌드 성공 확인
[ ] 커밋: "refactor: Day 1 - Critical fixes + P0 null safety"
```

### Day 2 체크리스트 (2026-01-14 수정)

```
[ ] 대용량 데이터 ref → shallowRef (15-20개: 배열/객체 데이터)
[ ] 참조 파일 동작 테스트
[ ] deep watch 최적화 (상위 10개)
[ ] 실시간 추적 테스트
[ ] 빌드 성공 확인
[ ] 커밋: "perf: Day 2 - shallowRef for large data refs"
```

### Day 3 체크리스트 (2026-01-14 수정)

```
[ ] .subscribe() 에러 처리 (나머지 17개 - 2개는 Day 1에서 완료)
[X] companion object 정리 → 불필요 (var 사용 0개, 모두 안전)
[ ] print/println 정리 (102개 중 상위 파일)
[ ] PassSchedulePage console.log (128개)
[ ] passScheduleStore console.log (103개)
[ ] 전체 기능 테스트
[ ] 문서 업데이트 (완료 항목 체크)
[ ] 커밋: "refactor: Day 3 - BE stability + cleanup"
```

---

## 긴급 연락처 / 참고

### 주요 파일 위치

| 파일 | 경로 |
|------|------|
| icdStore | `frontend/src/stores/icd/icdStore.ts` |
| modeStore (common) | `frontend/src/stores/common/modeStore.ts` |
| modeStore (icd) | `frontend/src/stores/icd/modeStore.ts` |
| UdpFwICDService | `backend/src/.../service/udp/UdpFwICDService.kt` |
| ICDService | `backend/src/.../service/icd/ICDService.kt` |

### 참고 문서

| 문서 | 역할 |
|------|------|
| [Comprehensive_Deep_Analysis.md](./Comprehensive_Deep_Analysis.md) | 전체 분석 결과 |
| [Execution_Checklist.md](./Execution_Checklist.md) | 상세 체크리스트 |
| [RFC-008](./RFC-008_Frontend_Restructuring.md) | FE 상세 계획 |

---

**작성자**: Claude
**작성일**: 2026-01-14
**검토자**: -
