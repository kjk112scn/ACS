# 리팩토링 실행 체크리스트

> **버전**: 5.1.0 | **작성일**: 2026-01-14 | **최종 수정**: 2026-01-14
> **목적**: 모든 리팩토링 작업을 체계적으로 추적 (Single Source of Truth)
> **전수조사**: 2026-01-13 완료 (FE 103개 파일, BE 66개 파일)

---

## 활용 도구

### 스킬 (워크플로우)

| 스킬 | 용도 | 시점 |
|------|------|------|
| `/health` | 프로젝트 상태 체크 | Phase B 시작 |
| `/plan` | 상세 작업 계획 | 각 RFC 구현 전 |
| `/sync` | 문서 동기화 | 코드 변경 후 |
| `/done` | 완료 처리 + 커밋 | 작업 완료 시 |

### 서브에이전트 (전문가)

| 에이전트 | 용도 | 시점 |
|---------|------|------|
| architect | 아키텍처 검토 | RFC 작성 시 |
| code-reviewer | 코드 품질 검증 | 구현 후 |
| performance-analyzer | 성능 분석 | 최적화 시 |
| test-expert | 테스트 작성 | 구현 후 |
| refactorer | 리팩토링 구현 | 코드 수정 시 |

---

## Phase 0: RFC-001 (데이터베이스) - P0

### 사전 조건
- [ ] PostgreSQL 설치 확인
- [ ] TimescaleDB 확장 설치
- [ ] 테스트 환경 준비

### 구현 체크리스트
- [ ] 테이블 생성 스크립트 작성
- [ ] Entity 클래스 생성
- [ ] Repository 인터페이스 생성
- [ ] R2DBC 설정 추가
- [ ] 마이그레이션 스크립트 작성
- [ ] 기존 코드에 저장 로직 추가

### 검증
- [ ] 빌드 성공
- [ ] 단위 테스트 통과
- [ ] 데이터 저장/조회 확인
- [ ] 30일 롤링 삭제 동작 확인

---

## Phase 1-A: RFC-002 (로깅) - P0

### 구현 체크리스트
- [ ] logback-spring.xml 수정
  - [ ] totalSizeCap 5GB 추가
  - [ ] error 로그 30일로 통일
- [ ] application.properties 정리
  - [ ] 중복 로그 레벨 설정 제거
- [ ] 이모지 로그 정리 (점진적)

### 검증
- [ ] 로그 파일 생성 확인
- [ ] 롤링 정책 동작 확인

---

## Phase 1-B: RFC-004 (API 표준화) - P1

### Phase 0: Critical 버그 수정
- [ ] LoggingController Path Traversal 수정
- [ ] PerformanceController 동시성 버그 수정
- [ ] ICDController String Boolean 수정
- [ ] SunTrackController 예외 처리 추가
- [ ] PushDataController 동기화 추가

### Phase 1: 응답 클래스 도입
- [ ] ApiResponse 클래스 생성
- [ ] ApiErrorResponse 클래스 생성
- [ ] 새 API부터 적용

### Phase 2: 동기 → 비동기 통일
- [ ] SettingsController `@Transactional` 제거
- [ ] LoggingController 비동기 변환 (2개)
- [ ] EphemerisController 비동기 변환 (3개)
- [ ] 나머지 컨트롤러 순차 변환 (68개)

### Phase 3: GlobalExceptionHandler
- [ ] 공통 예외 처리기 구현
- [ ] 컨트롤러별 try-catch 정리

### 검증
- [ ] 빌드 성공
- [ ] Frontend 호환성 확인
- [ ] 기존 기능 동작 확인

---

## Phase 2: RFC-006, 007 - P1/P2

### RFC-006: CI/CD
- [ ] RFC 문서 작성
- [ ] GitHub Actions 워크플로우 생성
- [ ] 빌드 자동화
- [ ] 테스트 자동화

### RFC-007: BE 인프라 (**P0**)

> **2026-01-14 전문가 검증 결과**: 우선순위 P0 상향

#### .subscribe() 에러 핸들러 추가 (19건) [신규]
- [ ] UdpFwICDService.kt:933 (**Critical**)
- [ ] UdpFwICDService.kt:195 (**Critical**)
- [ ] 나머지 17건 (Day 3)

#### companion object 검증 결과
- [X] ~~companion object 정리 (29개)~~ → **조치 불필요** (var 0개, 모두 안전)

#### 입력 검증 추가
- [ ] ICDController @Valid 추가
- [ ] EphemerisController @Valid 추가
- [ ] PassScheduleController @Valid 추가

### RFC-009: 접근성 - P2 [신규]

#### 기본 aria 속성
- [ ] 버튼 aria-label 추가
- [ ] 인풋 aria-describedby 연결
- [ ] 실시간 영역 aria-live 추가
- [ ] 토글 aria-pressed 추가

#### 키보드 네비게이션
- [ ] tabindex 정리
- [ ] 포커스 트랩 구현
- [ ] 스킵 링크 추가

#### 검증
- [ ] axe DevTools 오류 0개
- [ ] Lighthouse 접근성 80점+

---

## Phase 3: 점진적 개선 - P2

### RFC-003: 상태 머신 + BE 코드 품질 (점진적)

#### 블로킹 코드 제거 (RFC-004 Phase 2와 함께)
- [ ] `PassScheduleController.kt:1944` - Thread.sleep(100) 제거
- [ ] `BatchStorageManager.kt:294` - Thread.sleep(100) 제거
- [ ] `UdpFwICDService.kt:1074, :1148` - Thread.sleep(1000) 개선
- [ ] `ElevationCalculator.kt:78` - runBlocking 제거

#### BE 코드 품질 (심층 분석 결과) - RFC-003 v1.4.0 반영
- [ ] !! 연산자 제거 (**49건**)
  - **P0 (Critical) - 29건**
  - [ ] SunTrackService.kt (17건) - 태양 추적 안정성
  - [ ] ThreadManager.kt (8건) - `!= true` 패턴 적용
  - [ ] EphemerisService.kt (4건) - 위성 추적
  - [ ] UdpFwICDService.kt (4건) - UDP 통신
  - **P1 (High) - 13건**
  - [ ] PassScheduleService.kt (6건)
  - [ ] OrekitConfig.kt (2건)
  - [ ] ModeController.kt (2건)
  - [ ] 기타 (3건)
  - **P2 (Medium) - 7건** (필요시)
- [ ] 매직 넘버 상수화 (40+건)
  - [ ] 타임아웃 값 상수화
  - [ ] 각도 범위 상수화
  - [ ] ICD 배열 인덱스 enum화
- [ ] mutableListOf → 스레드 안전 컬렉션 (3건)
  - [ ] PushDataController.kt:57
  - [ ] EphemerisService.kt:234
  - [ ] PassScheduleService.kt:156

### RFC-005: 테스트
- [ ] RFC 문서 작성
- [ ] 핵심 로직 테스트 작성
- [ ] 커버리지 50% 달성

### RFC-008: 프론트엔드 구조화 (v1.1.0)

#### Phase 1: Composable 추출 (P1)
- [ ] useTrackingState.ts 생성
  - [ ] DashboardPage.vue 적용 (10회 → 1회)
  - [ ] EphemerisDesignationPage.vue 적용 (8회 → 1회)
  - [ ] PassSchedulePage.vue 적용 (3회 → 1회)
  - [ ] ephemerisTrackStore.ts 적용 (2회 → 1회)
- [ ] useAxisValue.ts 생성
- [ ] useSafeNumber.ts 생성
- [ ] useChartInstance.ts 생성
- [ ] 빌드 성공 확인
- [ ] 기능 테스트

#### Phase 2: 대형 파일 분리 (P2)
- [ ] PassSchedulePage.vue 분리 (4,838줄 → ~2,500줄)
  - [ ] ScheduleTable.vue 추출
  - [ ] ScheduleChart.vue 추출
  - [ ] ScheduleControls.vue 추출
- [ ] EphemerisDesignationPage.vue 분리 (4,340줄 → ~2,500줄)
  - [ ] PositionViewChart.vue 추출
  - [ ] TrackingInfoPanel.vue 추출
  - [ ] ScheduleSelector.vue 추출
- [ ] icdStore.ts 분리 (2,971줄 → ~1,500줄)
  - [ ] icdWebSocket.ts 추출
  - [ ] icdDataParser.ts 추출

#### Phase 3: console.log 정리 (P1) - 988개
- [ ] devLog 유틸 생성
- [ ] PassSchedulePage.vue (128개)
- [ ] passScheduleStore.ts (103개)
- [ ] SelectScheduleContent.vue (80개)
- [ ] TLEUploadContent.vue (64개)
- [ ] EphemerisDesignationPage.vue (63개)
- [ ] DashboardPage.vue (60개)
- [ ] 기타 파일들 (490개)
- [ ] 빌드 크기 비교

#### Phase 4: 코드 품질 개선 (P2) [신규]
- [ ] 하드코딩 색상 → CSS 변수 (300+건)
  - [ ] PassSchedulePage.vue (45+건)
  - [ ] EphemerisDesignationPage.vue (38+건)
  - [ ] DashboardPage.vue (35+건)
  - [ ] SelectScheduleContent.vue (28+건)
  - [ ] 기타 파일들 (150+건)
- [ ] as 타입 단언 → Type Guard (80+건)
  - [ ] icdStore.ts (22건)
  - [ ] passScheduleStore.ts (15건)
  - [ ] EphemerisDesignationPage.vue (12건)
  - [ ] 기타 (31건)

#### Phase 5: 성능 최적화 (P2) [신규]
- [ ] watch 과다 사용 정리 (62개)
  - [ ] PassSchedulePage.vue (18개)
  - [ ] EphemerisDesignationPage.vue (14개)
  - [ ] DashboardPage.vue (12개)
  - [ ] icdStore.ts (8개)
  - [ ] 기타 (10개)
- [ ] ~~icdStore shallowRef 적용~~ → **Phase 7로 이동 (수정됨)**
- [ ] ECharts 트리 쉐이킹 적용
- [ ] chart.js 미사용 의존성 제거

#### Phase 5-1: Offset Control 통합 (P0 최우선!) [RFC-008 v1.7.0 신규]

> **배경**: Offset이 3곳에 분산되어 모드 전환 시 데이터 불일치
> - ephemerisTrackStore.ts (offsetValues)
> - passScheduleStore.ts (offsetValues)
> - SunTrackPage.vue (offsetCals, outputs)

##### offsetStore 신규 생성
- [ ] `stores/offsetStore.ts` 생성
- [ ] offsetValues (3축: azimuth, elevation, train)
- [ ] calculateOffset(), applyOffset() 함수
- [ ] 모드 전환 시 값 유지 (초기화 금지)

##### 기존 코드 마이그레이션
- [ ] ephemerisTrackStore.ts → offsetStore 참조
- [ ] passScheduleStore.ts → offsetStore 참조
- [ ] SunTrackPage.vue → offsetStore 참조
- [ ] clearAllData() 호출 시 offset 초기화 제거

#### Phase 6: Service 레이어 리팩토링 (P1) [신규 - 사용자 지적]

> **배경**: Position View 차트 실시간 추적 기능이 매우 느리고 부하가 많음

##### 대형 Service 파일 분리
- [ ] ephemerisTrackService.ts (1,193줄 → ~600줄)
  - [ ] 타입 정의 분리 → `types/ephemeris.ts`
  - [ ] 에러 클래스 분리 → `services/utils/errors.ts`
  - [ ] console.log 50개 정리
- [ ] passScheduleService.ts (1,118줄 → ~600줄)
  - [ ] 타입 정의 분리 → `types/passSchedule.ts`
  - [ ] 차트 변환 분리 → `services/utils/chartDataOptimizer.ts`
  - [ ] console.log 42개 정리

##### Position View 차트 성능 최적화 (Critical!)
- [ ] `convertToChartData` 최적화 (passScheduleService.ts:1025-1114)
  - [ ] 4-5회 순회 → 단일 순회로 통합
  - [ ] `new Date()` 반복 생성 제거 (타임스탬프 캐싱)
  - [ ] 전체 배열 복사 제거 (in-place 정렬)
  - [ ] 360° 정규화 while 루프 최적화

##### Service 간 중복 코드 제거 (92줄+)
- [ ] `handleApiError` 공통화 → `services/utils/apiErrorHandler.ts`
- [ ] Schedule 타입 통합 → `types/schedule.ts`
  - [ ] `ScheduleItem` + `PassScheduleMasterData` 통합
- [ ] 에러 클래스 통합 → `services/utils/errors.ts`
  - [ ] `TLEParseError`, `ApiError`, `TLEApiError` 통합
- [ ] `sendTimeOffsetCommand` 공통화

#### Phase 7: icdStore 심층 최적화 (P0) [RFC-008 v1.7.0 반영]

> **배경**: ICD 부분 코드 복잡, 30ms마다 175개 ref 업데이트로 성능 저하
>
> ⚠️ **2026-01-14 전문가 검증 결과**: shallowRef 그룹화는 **원시 타입에 무효**
> - icdStore의 175개 ref 중 **~140개가 원시 타입** (string/number/boolean)
> - **실제 병목**: `updateAntennaData` 함수의 70개 조건문

##### ~~개별 ref → shallowRef 그룹화~~ → **폐기**
> 원시 타입은 `ref()`와 `shallowRef()` 동일 동작, 22개 computed 재작성 필요

##### 객체 타입만 shallowRef 적용 (2건)
- [ ] errorStatusBarData → shallowRef 변환
- [ ] errorPopupData → shallowRef 변환

##### updateAntennaData 최적화 (P0 실제 병목!)
- [ ] 필드 매핑 배열 생성 (70개 조건문 → 매핑 1개)
- [ ] forEach 일괄 처리로 리팩토링 (338줄 → ~100줄)
```typescript
// Before: 70개 조건문 개별 체크
if (antennaData.modeStatusBits !== undefined && antennaData.modeStatusBits !== null) {
  modeStatusBits.value = safeToString(antennaData.modeStatusBits)
}
// After: 필드 매핑 + 일괄 처리
fieldMappings.forEach(({ key, ref }) => {
  const value = antennaData[key]
  if (value !== undefined && value !== null) {
    ref.value = safeToString(value)
  }
})
```

##### 비트 파싱 배치 처리 (13회 → 1회)
- [ ] parseAllBoardStatus 래퍼 함수 생성
- [ ] 기존 13개 파싱 함수 순차 호출로 통합

##### 기타 최적화
- [ ] 동적 import 제거 (L1466 hardwareErrorLogStore)
- [ ] safeToString 호출 최소화

##### 예상 효과 (수정됨)
| 항목 | 현재 | 최적화 후 | 비고 |
|------|------|----------|------|
| shallowRef 적용 | 0개 | **2개만** | 객체 타입만 효과 |
| updateAntennaData | 70개 조건문 | 필드 매핑 1개 | **실제 병목 해결** |
| 비트 파싱 함수 호출 | 13회/30ms | 1회/30ms | 배치 처리 |
| 예상 CPU 감소 | - | **40-50%** | 현실적 추정 |

#### Phase 8: BE 놓친 영역 (RFC-004 Phase 6)

##### 입력 검증 추가 (Critical!)
- [ ] ICDController 파라미터 검증
  - [ ] 각도 범위 (-360° ~ 360°)
  - [ ] 속도 범위 (음수 방지)
- [ ] EphemerisController 파라미터 검증
- [ ] PassScheduleController 파라미터 검증
- [ ] GlobalExceptionHandler에 MethodArgumentNotValidException 처리

##### Repository 추상화
- [ ] TrackingDataRepository 인터페이스 정의
- [ ] MemoryTrackingRepository 구현
- [ ] EphemerisService에 DI
- [ ] PassScheduleService에 DI
- [ ] 단위 테스트용 MockRepository 준비

##### GlobalData 체계화
- [ ] ConfigurationService 생성
- [ ] GlobalData.Offset 마이그레이션
- [ ] 변경 이벤트 발행

##### print/println 제거 (102건)
- [ ] ElevationCalculator.kt print → logger.debug
- [ ] InitService.kt print → logger.info
- [ ] ICDService.kt print → logger.debug
- [ ] 기타 파일 순차 정리

---

## 전수조사 결과 (2026-01-13)

> **조사 완료**: FE 103개 파일 (33,000줄+), BE 66개 파일 (33,284줄)

### FE 추가 발견 사항 (RFC-008 업데이트 필요)

| 항목 | 기존 | 전수조사 | 심각도 | RFC |
|------|------|---------|--------|-----|
| console.log | 988개 | **1,513개** | Medium | RFC-008 Phase 3 |
| 타입 단언(as) | 80+건 | **313건** | High | RFC-008 Phase 4 |
| 하드코딩 색상 | 300+건 | **475건** | High | RFC-008 Phase 4 |
| !important | 미확인 | **1,690개** | Medium | RFC-008 추가 |
| 접근성(a11y) | 미확인 | **0개 aria** | **Critical** | 신규 RFC |
| 미사용 코드 | 미확인 | **3건** | Critical | RFC-008 추가 |
| modeStore 중복 | 미확인 | **2개 파일** | High | RFC-008 추가 |
| 메모리 누수 | 미확인 | **2건** | High | RFC-008 추가 |

#### 미사용 코드 상세
- [ ] `components/common/ExampleComponent.vue` - 삭제 필요
- [ ] `stores/example-store.ts` - 삭제 필요
- [X] ~~`stores/common/modeStore.ts` vs `stores/icd/modeStore.ts`~~ - **보류** (미사용 확인됨)

#### 메모리 누수 상세
- [ ] `windowUtils.ts` - 이벤트 리스너 미정리 (L507, L943)
- [ ] 다이얼로그 생성 시 리스너 누적 가능성

### BE 추가 발견 사항 (RFC-003, RFC-007 업데이트 필요)

| 항목 | 기존 | 전수조사 | 심각도 | RFC |
|------|------|---------|--------|-----|
| !! 연산자 | 46건 | **49건** (P0:29, P1:13, P2:7) | High | RFC-003 v1.4.0 |
| 매직 넘버 | 40+건 | **40+건 확인** | Medium | RFC-003 |
| print/println | 102건 | **102건 확인** | High | RFC-007 Phase 4 |
| 테스트 코드 혼재 | 미확인 | **595줄** | High | 즉시 정리 |
| 주석 처리 close() | 미확인 | **2건** | High | 즉시 정리 |
| 리소스 누수 | 미확인 | **2건** | Medium | RFC-007 추가 |
| 광범위 catch | 미확인 | **180+건** | High | RFC-007 추가 |

#### 즉시 정리 대상 (BE)
- [ ] `algorithm/.../OrekitCalcuatorTest.kt` - test/ 폴더로 이동 (595줄)
- [ ] `EphemerisController.kt:908-909` - 주석 처리된 close() 정리
- [ ] `DataStoreService.kt:40-41` - 주석 처리된 검증 로직 정리

#### 리소스 누수 상세
- [ ] `ElevationCalculator.kt:93, 121` - HttpURLConnection disconnect() 미호출
- [ ] `UdpFwICDService.kt:1080` - DatagramChannel close() 예외 처리 부재

### 심각도별 우선순위

```
┌─────────────────────────────────────────────────────────┐
│ CRITICAL (즉시 조치)                                     │
├─────────────────────────────────────────────────────────┤
│ • FE 접근성(a11y): 0개 aria 속성 (WCAG 위반)           │
│ • FE 미사용 코드: 3건 삭제                              │
│ • BE 테스트 코드 혼재: 595줄 이동                       │
│ • BE GlobalData 동시성: 128개 companion object         │
├─────────────────────────────────────────────────────────┤
│ HIGH (1~2개월 내)                                       │
├─────────────────────────────────────────────────────────┤
│ • FE 타입 단언(as): 313건                              │
│ • FE 하드코딩 색상: 475건                              │
│ • FE modeStore 중복: 2개 파일 통합                     │
│ • FE 메모리 누수: 이벤트 리스너 정리                   │
│ • BE print/println: 102건                              │
│ • BE 광범위 catch: 180+건                              │
├─────────────────────────────────────────────────────────┤
│ MEDIUM (3~6개월)                                        │
├─────────────────────────────────────────────────────────┤
│ • FE !important: 1,690개                               │
│ • FE console.log: 1,513개                              │
│ • BE 매직 넘버: 40+건                                  │
│ • BE 리소스 누수: 2건                                  │
└─────────────────────────────────────────────────────────┘
```

---

## 롤백 가이드

### Git 기반 롤백
```bash
# 마지막 안정 커밋으로 롤백
git revert HEAD

# 특정 커밋으로 롤백
git revert <commit-hash>
```

### 롤백 지점 (커밋 태그)
- [ ] `v2.0-pre-db` - DB 작업 전
- [ ] `v2.0-pre-api` - API 표준화 전
- [ ] `v2.0-pre-auth` - 인증 추가 전

---

## 완료 기준

| 기준 | 측정 방법 |
|------|----------|
| 빌드 성공 | `./gradlew clean build` |
| 테스트 통과 | `./gradlew test` |
| 기존 기능 동작 | 수동 테스트 |
| 문서 동기화 | `/sync` 스킬 실행 |

---

**작성자**: Claude
**최종 수정**: 2026-01-13
