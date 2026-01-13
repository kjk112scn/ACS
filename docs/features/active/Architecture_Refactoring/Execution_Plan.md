# ACS v2.0 리팩토링 실행 계획서

> **버전**: 1.0.0 | **작성일**: 2026-01-13
> **기반**: 전수조사 결과 (FE 103개 파일, BE 66개 파일)
> **원칙**: 소규모 팀 관리 가능, 오버엔지니어링 금지

---

## 1. 현황 요약

### 1.1 전수조사 결과 (2026-01-13)

| 영역 | 파일 수 | 코드량 | 주요 이슈 |
|------|---------|--------|----------|
| Frontend | 103개 | 33,000줄+ | aria 0개, console.log 1,513개, !important 1,690개 |
| Backend | 66개 | 33,284줄 | 광범위 catch 180+건, print 102건, !! 46건 |

### 1.2 심각도별 분류

```
┌─────────────────────────────────────────────────────────────┐
│ CRITICAL (즉시)                                              │
├─────────────────────────────────────────────────────────────┤
│ FE: 미사용 코드 3건, modeStore 중복, 메모리 누수 2건        │
│ BE: 테스트 코드 혼재 595줄, Path Traversal 취약점           │
├─────────────────────────────────────────────────────────────┤
│ HIGH (1개월)                                                 │
├─────────────────────────────────────────────────────────────┤
│ FE: 타입 단언 280건, 하드코딩 색상 520건, icdStore 175 ref  │
│ BE: print/println 102건, 광범위 catch 180+건, !! 46건       │
├─────────────────────────────────────────────────────────────┤
│ MEDIUM (3개월)                                               │
├─────────────────────────────────────────────────────────────┤
│ FE: console.log 1,513개, !important 1,690개, 대형 파일      │
│ BE: 매직 넘버 40+건, 리소스 누수 2건                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 실행 계획

### Sprint 0: 즉시 조치 (1-2일)

> **목표**: 빌드/런타임에 영향 없는 즉시 수정 가능 항목

#### 2.1 BE 즉시 정리

| 작업 | 파일 | 설명 | 예상 시간 |
|------|------|------|----------|
| 테스트 코드 이동 | `OrekitCalculatorTest.kt` | main/ → test/ 이동 (595줄) | 10분 |
| 주석 정리 | `EphemerisController.kt:908-909` | 주석 처리된 close() 제거 | 5분 |
| 주석 정리 | `DataStoreService.kt:40-41` | 주석 처리된 검증 로직 제거 | 5분 |

#### 2.2 FE 즉시 정리

| 작업 | 파일 | 설명 | 예상 시간 |
|------|------|------|----------|
| Dead Code 삭제 | `ExampleComponent.vue` | 미사용 예제 컴포넌트 삭제 | 5분 |
| Dead Code 삭제 | `example-store.ts` | 미사용 예제 스토어 삭제 | 5분 |
| modeStore 분석 | `stores/common/` vs `stores/icd/` | 중복 파일 확인 후 통합 | 30분 |

#### 2.3 검증

```bash
# BE 빌드 확인
cd backend && ./gradlew clean build -x test

# FE 빌드 확인
cd frontend && npm run build
```

---

### Sprint 1: 보안 및 안정성 (1주)

> **목표**: Critical 보안 이슈 및 안정성 문제 해결

#### 3.1 RFC-004 Phase 0: Critical 버그 수정

| 작업 | 파일 | 이슈 | 영향 |
|------|------|------|------|
| Path Traversal | `LoggingController.kt` | 보안 취약점 | Critical |
| 동시성 버그 | `PerformanceController.kt` | 레이스 컨디션 | High |
| String Boolean | `ICDController.kt` | 예상치 못한 동작 | Medium |
| 예외 처리 | `SunTrackController.kt` | NPE 가능성 | Medium |
| 동기화 | `PushDataController.kt` | 스레드 안전성 | Medium |

#### 3.2 RFC-007 Phase 6: 리소스 누수 수정

| 작업 | 파일 | 수정 내용 |
|------|------|----------|
| HttpURLConnection | `ElevationCalculator.kt` | `.use { }` 블록 적용 |
| DatagramChannel | `UdpFwICDService.kt` | `.use { }` 블록 적용 |

#### 3.3 RFC-008 Phase 7: 메모리 누수 수정

| 작업 | 파일 | 수정 내용 |
|------|------|----------|
| 이벤트 리스너 | `windowUtils.ts` | `onBeforeUnmount` 추가 |

#### 3.4 검증 체크리스트

- [ ] 빌드 성공 (BE + FE)
- [ ] 기존 기능 동작 확인
- [ ] 보안 취약점 재검증

---

### Sprint 2: 인프라 기반 (2주)

> **목표**: DB, 로깅, API 표준화 기반 구축

#### 4.1 RFC-001: 데이터베이스 (P0)

| 작업 | 설명 | 선행 조건 |
|------|------|----------|
| PostgreSQL 설정 | 테이블 생성, TimescaleDB 확장 | 환경 준비 |
| Entity 생성 | `TrackingData`, `PassSchedule` | - |
| Repository | R2DBC 기반 인터페이스 | Entity |
| 마이그레이션 | 기존 데이터 저장 로직 연결 | Repository |

#### 4.2 RFC-002: 로깅 (P0)

| 작업 | 설명 |
|------|------|
| logback 수정 | totalSizeCap 5GB, 30일 보관 |
| 중복 설정 제거 | application.properties 정리 |

#### 4.3 RFC-004 Phase 1-2: API 표준화

| 작업 | 설명 |
|------|------|
| ApiResponse 도입 | 공통 응답 래퍼 클래스 |
| 비동기 통일 | `suspend fun` 기반 통일 (68개 메서드) |

#### 4.4 검증 체크리스트

- [ ] DB 연결 및 저장 확인
- [ ] 로그 롤링 동작 확인
- [ ] API 응답 형식 일관성

---

### Sprint 3: 코드 품질 (2주)

> **목표**: BE/FE 코드 품질 개선

#### 5.1 RFC-003: BE 코드 품질

| 작업 | 건수 | 우선순위 |
|------|------|----------|
| !! 연산자 제거 | 46건 | High |
| 매직 넘버 상수화 | 40+건 | Medium |
| mutableListOf 개선 | 3건 | Medium |

#### 5.2 RFC-007 Phase 4-5: BE 인프라

| 작업 | 건수 | 우선순위 |
|------|------|----------|
| print/println 제거 | 102건 | High |
| 광범위 catch 개선 | 180+건 | High |
| 입력 검증 추가 | 3개 Controller | Critical |

#### 5.3 RFC-008 Phase 1, 3: FE 코드 품질

| 작업 | 건수 | 우선순위 |
|------|------|----------|
| Composable 추출 | 4개 | High |
| console.log 정리 | 1,513개 | Medium |
| devLog 유틸 생성 | 1개 | High |

#### 5.4 검증 체크리스트

- [ ] TypeScript 오류 0개
- [ ] Kotlin 빌드 경고 최소화
- [ ] console.log prod 빌드에서 제거 확인

---

### Sprint 4: 성능 최적화 (2주)

> **목표**: icdStore 최적화 및 차트 성능 개선

#### 6.1 RFC-008 Phase 5, 7: icdStore 최적화 (Critical!)

| 작업 | 현재 | 목표 |
|------|------|------|
| 개별 ref → shallowRef | 175개 | 5-10개 그룹 |
| 비트 파싱 배치 | 13회/30ms | 1회/30ms |
| computed 최적화 | 22개 전체 | 필요 시만 |

**예상 효과**: CPU 사용률 70-80% 감소

#### 6.2 RFC-008 Phase 6: 차트 성능

| 작업 | 파일 | 개선 내용 |
|------|------|----------|
| convertToChartData | `passScheduleService.ts` | 4-5회 순회 → 1회 |
| Date 객체 캐싱 | `passScheduleService.ts` | 반복 생성 제거 |

#### 6.3 검증 체크리스트

- [ ] 실시간 추적 CPU 사용률 측정
- [ ] 차트 렌더링 시간 측정
- [ ] 메모리 사용량 비교

---

### Sprint 5: 구조화 (3주+)

> **목표**: 대형 파일 분리, 장기 유지보수성 확보

#### 7.1 RFC-008 Phase 2: 대형 FE 파일 분리

| 파일 | 현재 | 목표 | 분리 컴포넌트 |
|------|------|------|--------------|
| PassSchedulePage.vue | 4,838줄 | ~2,500줄 | Table, Chart, Controls |
| EphemerisDesignationPage.vue | 4,340줄 | ~2,500줄 | PositionView, Info, Selector |
| icdStore.ts | 2,971줄 | ~1,500줄 | WebSocket, Parser |

#### 7.2 RFC-008 Phase 4: 코드 스타일

| 작업 | 건수 | 방법 |
|------|------|------|
| 하드코딩 색상 | 520건 | CSS 변수 전환 |
| as 타입 단언 | 280건 | Type Guard 적용 |
| !important | 1,690개 | CSS 특이성 재설계 |

#### 7.3 RFC-007 Phase 2-3: BE 구조화

| 작업 | 설명 |
|------|------|
| Repository 추상화 | 인터페이스 정의, DI |
| GlobalData 체계화 | ConfigurationService |

---

## 3. 우선순위 매트릭스

```
긴급도 ↑
        │
   P0   │  Sprint 0-1: 즉시 조치 + 보안
   ─────┼─────────────────────────────────
   P1   │  Sprint 2-3: 인프라 + 품질
   ─────┼─────────────────────────────────
   P2   │  Sprint 4-5: 성능 + 구조화
        │
        └───────────────────────────────→ 영향도
```

### 3.1 병렬 진행 가능 항목

```
BE 작업                    FE 작업
────────────────────────   ────────────────────────
RFC-004 Critical 버그  ←→  RFC-008 Dead Code 삭제
RFC-002 로깅          ←→  RFC-008 console.log 정리
RFC-003 !! 연산자     ←→  RFC-008 Composable 추출
RFC-007 print/println ←→  RFC-008 icdStore 최적화
```

### 3.2 의존성 순서 (필수)

```
RFC-001 (DB)
    │
    ├─→ RFC-002 (로깅) ─┬─→ RFC-004 (API)
    │                   │
    └─→ RFC-007 (BE 인프라) ─→ RFC-005 (테스트)
                        │
RFC-008 (FE) ───────────┘
    │
    └─→ RFC-009 (접근성)
```

---

## 4. 측정 기준

### 4.1 정량적 목표

| 지표 | 현재 | 목표 |
|------|------|------|
| 테스트 커버리지 | 1.5% | 50%+ |
| console.log (prod) | 1,513개 | 0개 |
| TypeScript 오류 | 미확인 | 0개 |
| Lighthouse 접근성 | 미측정 | 80점+ |
| icdStore ref | 175개 | 10개 미만 |

### 4.2 정성적 목표

- [ ] 모든 API 응답 형식 통일
- [ ] 대형 파일 3,000줄 이하
- [ ] WCAG Level A 준수
- [ ] 코드 리뷰 가능한 파일 크기

---

## 5. 리스크 관리

### 5.1 주요 리스크

| 리스크 | 영향 | 대응 |
|--------|------|------|
| icdStore 변경 시 전체 동작 오류 | Critical | 단계별 마이그레이션, 충분한 테스트 |
| API 변경 시 FE 호환성 | High | BE-FE 동시 변경, 버전 관리 |
| 대형 파일 분리 시 import 오류 | Medium | IDE 리팩토링 기능 활용 |

### 5.2 롤백 전략

```bash
# 각 Sprint 시작 전 태그 생성
git tag v2.0-sprint0-start
git tag v2.0-sprint1-start
# ...

# 문제 발생 시 롤백
git revert HEAD
# 또는
git reset --hard v2.0-sprint1-start
```

---

## 6. 관련 문서

| 문서 | 역할 |
|------|------|
| [Execution_Checklist.md](./Execution_Checklist.md) | 상세 체크리스트 |
| [RFC-001](./RFC-001_Database_Strategy.md) ~ [RFC-009](./RFC-009_Accessibility.md) | 각 영역 상세 설계 |
| [README.md](./README.md) | 프로젝트 개요 |

---

**작성자**: Claude
**검토자**: -
**승인일**: -
