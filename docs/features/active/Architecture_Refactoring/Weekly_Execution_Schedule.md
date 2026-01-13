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

### 오전/업무시간 (4시간)

| 시간 | 작업 | 파일 | 예상 |
|:----:|------|------|:----:|
| 1 | BE 테스트 코드 이동 | `OrekitCalculatorTest.kt` → test/ | 10분 |
| 2 | 주석 처리된 코드 정리 | `EphemerisController.kt`, `DataStoreService.kt` | 10분 |
| 3 | FE Dead Code 삭제 | `ExampleComponent.vue`, `example-store.ts` | 10분 |
| 4 | **modeStore 중복 통합** | `stores/common/` + `stores/icd/` | 1시간 |
| 5 | Thread.sleep → delay | `UdpFwICDService.kt` (2곳) | 30분 |
| 6 | runBlocking 제거 | `ElevationCalculator.kt` | 30분 |
| 7 | 빌드 확인 (BE + FE) | - | 30분 |

**체크포인트**: 빌드 성공, Critical 블로킹 제거

### 저녁/퇴근 후 (3-4시간)

| 시간 | 작업 | 파일 | 예상 |
|:----:|------|------|:----:|
| 8 | devLog 유틸 생성 | `utils/logger.ts` | 30분 |
| 9 | DashboardPage console.log | 60개 → devLog | 1시간 |
| 10 | icdStore console.log | - | 30분 |
| 11 | 빌드 확인 | - | 15분 |

**Day 1 완료 기준**:
- [ ] 테스트 코드 main → test 이동
- [ ] Dead Code 3건 삭제
- [ ] modeStore 통합 (2개 → 1개)
- [ ] Thread.sleep/runBlocking 제거
- [ ] devLog 유틸 생성 + 일부 적용
- [ ] 빌드 성공

---

## Day 2 (목요일) - FE 성능 최적화 (핵심!)

### 오전/업무시간 (4시간)

| 시간 | 작업 | 상세 | 예상 |
|:----:|------|------|:----:|
| 1 | **AntennaState 인터페이스 정의** | angles, speeds, status 그룹화 | 30분 |
| 2 | **shallowRef 적용 (기본 상태)** | icdStore L49-57 (9개 ref) | 30분 |
| 3 | **shallowRef 적용 (안테나 데이터)** | icdStore L109-284 (81개 → 5개 그룹) | 2시간 |
| 4 | updateAntennaData 리팩토링 | 338줄 → ~100줄 | 1시간 |

**체크포인트**: icdStore ref 81개 → 10개 미만

### 저녁/퇴근 후 (3-4시간)

| 시간 | 작업 | 상세 | 예상 |
|:----:|------|------|:----:|
| 5 | **비트 파싱 배치 처리** | 13개 함수 → 1개 통합 | 1.5시간 |
| 6 | deep watch 최적화 | 34개 중 상위 10개 | 1시간 |
| 7 | computed 최적화 | 대형 computed 분리 | 30분 |
| 8 | 통합 테스트 | 실시간 추적 동작 확인 | 30분 |

**Day 2 완료 기준**:
- [ ] icdStore shallowRef 적용
- [ ] ref 업데이트 81회 → 10회 미만
- [ ] 비트 파싱 배치 처리
- [ ] deep watch 상위 10개 최적화
- [ ] **실시간 추적 정상 동작 확인**

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

### P0 - 즉시 (Day 1)

| # | 작업 | 효과 | 위험도 |
|---|------|------|:------:|
| 1 | 테스트 코드 이동 | 코드 정리 | 낮음 |
| 2 | Dead Code 삭제 | 혼란 제거 | 낮음 |
| 3 | modeStore 통합 | 중복 제거 | 중간 |
| 4 | Thread.sleep 제거 | 블로킹 해소 | 중간 |
| 5 | devLog 유틸 | 기반 마련 | 낮음 |

### P1 - 핵심 (Day 2)

| # | 작업 | 효과 | 위험도 |
|---|------|------|:------:|
| 1 | **shallowRef 적용** | **CPU 80% 감소** | **높음** |
| 2 | 비트 파싱 배치 | 함수 호출 감소 | 중간 |
| 3 | deep watch 최적화 | 재렌더링 감소 | 중간 |

### P2 - 안정성 (Day 3)

| # | 작업 | 효과 | 위험도 |
|---|------|------|:------:|
| 1 | .subscribe() 에러 | 에러 가시성 | 낮음 |
| 2 | companion object | 동시성 안정 | 중간 |
| 3 | console.log 정리 | 번들 감소 | 낮음 |

---

## 위험 관리

### 높은 위험 작업

| 작업 | 위험 | 대응 |
|------|------|------|
| **shallowRef 적용** | 전체 동작 영향 | 단계별 적용, 즉시 테스트 |
| modeStore 통합 | import 오류 | IDE 리팩토링 기능 활용 |
| updateAntennaData | 데이터 누락 | 기존 로직 백업 |

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

### 정량적 효과

| 지표 | 현재 | 3일 후 | 개선율 |
|------|:----:|:------:|:------:|
| icdStore ref | 81개 | ~10개 | **88% 감소** |
| CPU 사용률 | 높음 | 낮음 | **~80% 감소** |
| deep watch | 34개 | ~20개 | 40% 감소 |
| console.log | 1,513개 | ~800개 | 47% 감소 |
| Dead Code | 3건 | 0건 | 100% |

### 남은 작업 (다음 주)

| 작업 | 우선순위 |
|------|:--------:|
| 대형 페이지 분리 | P2 |
| !important 정리 (1,690개) | P3 |
| 하드코딩 색상 (520건) | P3 |
| @Valid 입력 검증 | P2 |
| 나머지 console.log | P3 |

---

## 일별 체크리스트

### Day 1 체크리스트

```
[ ] BE 테스트 코드 이동
[ ] 주석 처리 코드 정리 (2건)
[ ] ExampleComponent.vue 삭제
[ ] example-store.ts 삭제
[ ] modeStore 통합
[ ] Thread.sleep → delay (2곳)
[ ] runBlocking 제거
[ ] devLog 유틸 생성
[ ] DashboardPage console.log 정리
[ ] 빌드 성공 확인
[ ] 커밋: "refactor: Day 1 - Critical fixes"
```

### Day 2 체크리스트

```
[ ] AntennaState 인터페이스 정의
[ ] icdStore shallowRef 적용
[ ] updateAntennaData 리팩토링
[ ] 비트 파싱 배치 처리
[ ] deep watch 최적화 (10개)
[ ] 실시간 추적 테스트
[ ] 빌드 성공 확인
[ ] 커밋: "perf: Day 2 - icdStore optimization"
```

### Day 3 체크리스트

```
[ ] .subscribe() 에러 처리 (19개)
[ ] companion object 정리
[ ] print/println 정리
[ ] PassSchedulePage console.log
[ ] passScheduleStore console.log
[ ] 전체 기능 테스트
[ ] 문서 업데이트
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
