# 심층 코드 분석 보고서

> **통합 알림**: 이 문서의 내용은 [Comprehensive_Deep_Analysis.md](./Comprehensive_Deep_Analysis.md)에 통합되었습니다. 이 문서는 참고용으로 유지됩니다.

> **작성일**: 2026-01-14
> **분석 범위**: FE 103개 파일, BE 66개 파일 실제 코드 레벨 분석
> **목적**: 기존 RFC에서 다루지 않은 심층 이슈 발굴

---

## 1. Frontend 심층 분석

### 1.1 렌더링 성능 이슈

#### A. watch 과다 사용 - **63건** (기존 RFC 대비 +1)

| 파일 | watch 수 | deep:true | 문제 |
|------|:--------:|:---------:|------|
| DashboardPage.vue | 13 | 0 | 과다 |
| PassSchedulePage.vue | 5 | 3 | deep watch 성능 |
| EphemerisDesignationPage.vue | 3 | 1 | - |
| SettingsModal.vue | 4 | 0 | - |
| 기타 19개 파일 | 38 | 30 | - |

**신규 발견**: `deep: true` 사용 **34건** - 성능 저하 원인

#### B. ECharts setOption 호출 패턴

| 파일 | setOption 호출 | 문제 |
|------|:-------------:|------|
| DashboardPage.vue | 6 | 빈번한 전체 재설정 |
| EphemerisDesignationPage.vue | 6 | notMerge 미사용 |
| PassSchedulePage.vue | 3 | - |

**권장**: `notMerge: true` 옵션으로 부분 업데이트

#### C. 직접 DOM 조작 - **6건** (신규)

| 파일 | 패턴 | 위험도 |
|------|------|:------:|
| windowUtils.ts | document.querySelector 3건 | Medium |
| PassSchedulePage.vue | document.querySelector 2건 | Medium |
| useNotification.ts | document.querySelector 1건 | Low |

**권장**: `ref`를 통한 Vue 방식 DOM 접근으로 변경

### 1.2 상태 관리 패턴 이슈 (신규)

#### A. Provide/Inject 미사용

| 항목 | 현재 | 권장 |
|------|------|------|
| provide() 사용 | **0건** | Props drilling 방지에 활용 |
| inject() 사용 | **0건** | 깊은 컴포넌트 트리에 필요 |

**영향**: Props drilling 발생 가능성 높음

#### B. defineProps 사용 제한적 - **11건**

| 파일 | defineProps | 타입 정의 |
|------|:-----------:|:---------:|
| SelectScheduleContent.vue | O | 부분적 |
| TLEUploadContent.vue | O | 부분적 |
| 기타 6개 | O | 부분적 |
| **대부분 페이지** | X | 없음 |

**문제**: 대형 페이지(PassSchedule, Ephemeris)에서 Props 타입 미정의

### 1.3 번들 최적화 이슈 (신규)

#### A. 중복 차트 라이브러리 확정

```json
// package.json
"chart.js": "^4.4.9",      // ~200KB
"echarts": "^5.6.0",       // ~500KB
"vue-chartjs": "^5.3.2",   // ~50KB (chart.js 래퍼)
```

**낭비**: chart.js + vue-chartjs = **~250KB** (echarts만 사용 시 불필요)

#### B. ECharts 전체 import (추정)

```typescript
// 현재 (추정)
import * as echarts from 'echarts'  // ~500KB 전체 로드

// 권장 (트리 쉐이킹)
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
// → ~150KB로 감소 가능
```

#### C. 테스트 미설정

```json
"scripts": {
  "test": "echo \"No test specified\" && exit 0"  // 테스트 없음
}
```

---

## 2. Backend 심층 분석

### 2.1 동시성/스레드 안전성 (Critical - 신규)

#### A. companion object 가변 상태 - **29건**

| 파일 | companion object 수 | 문제 |
|------|:-------------------:|------|
| ICDService.kt | **19** | 가장 많음 - Critical |
| JKUtil.kt | 2 | - |
| 기타 8개 | 8 | - |

**문제**: companion object 내 `var` 상태는 전역 공유 → 동시성 버그

#### B. 동기화 코드 현황 - **71건**

| 패턴 | 건수 | 주요 파일 |
|------|:----:|----------|
| @Volatile | 12+ | DataStoreService, BatchStorageManager |
| synchronized | 20+ | PushDataController, EphemerisService |
| Atomic* | 5+ | PerformanceController, SPACalculator |

**문제**: 동기화 패턴 불일치 - 일부는 synchronized, 일부는 Atomic, 일부는 없음

#### C. 블로킹 코드 혼재 - **3개 파일** (Critical)

| 파일 | 패턴 | 영향 |
|------|------|------|
| UdpFwICDService.kt | Thread.sleep | WebFlux 스레드 블로킹 |
| BatchStorageManager.kt | Thread.sleep | 스레드 풀 고갈 |
| ElevationCalculator.kt | runBlocking | 코루틴 컨텍스트 블로킹 |

#### D. Reactive 스트림 블로킹 - **19건**

| 패턴 | 건수 | 문제 |
|------|:----:|------|
| .subscribe() | 17 | fire-and-forget, 에러 처리 누락 가능 |
| .block() | 2 | WebFlux에서 블로킹 = 성능 저하 |

### 2.2 서비스 레이어 구조 (신규)

#### A. 서비스 크기 분석

| 서비스 | 줄 수 | 상태 |
|--------|------:|:----:|
| UdpFwICDService.kt | 1,200+ | 분리 필요 |
| EphemerisService.kt | 800+ | - |
| PassScheduleService.kt | 700+ | - |
| ICDService.kt | 600+ | - |

#### B. 순환 참조 가능성

```
EphemerisService
    ↓ 의존
PassScheduleService
    ↓ 의존
ICDService
    ↓ 의존
GlobalData (companion object)
```

**문제**: GlobalData가 모든 서비스에서 접근 → 암묵적 순환 의존

### 2.3 실시간 통신 이슈 (신규)

#### A. WebSocket 세션 관리

| 항목 | 현재 | 권장 |
|------|------|------|
| 세션 저장 | mutableListOf | ConcurrentHashMap |
| 연결 해제 | 부분적 처리 | 완전한 정리 필요 |
| 에러 처리 | 기본적 | 재연결 로직 추가 |

#### B. UDP 통신 (ICD)

| 항목 | 현재 | 위험도 |
|------|------|:------:|
| 패킷 손실 처리 | 미구현 | Medium |
| 타임아웃 | 하드코딩 | Low |
| 버퍼 관리 | 기본 | Low |

---

## 3. 신규 발견 이슈 요약

### 3.1 Critical (즉시 조치)

| # | 영역 | 이슈 | 영향 |
|---|------|------|------|
| 1 | BE | ICDService companion object 19개 | 동시성 버그 |
| 2 | BE | Thread.sleep 3개 파일 | WebFlux 블로킹 |
| 3 | BE | .block() 2건 | 성능 저하 |
| 4 | FE | deep watch 34건 | 렌더링 성능 |

### 3.2 High (1개월)

| # | 영역 | 이슈 | 건수 |
|---|------|------|:----:|
| 1 | FE | chart.js 중복 | ~250KB |
| 2 | FE | ECharts 전체 import | ~350KB 절감 가능 |
| 3 | FE | Provide/Inject 미사용 | 0건 |
| 4 | BE | 동기화 패턴 불일치 | 71건 |
| 5 | BE | .subscribe() fire-and-forget | 17건 |

### 3.3 Medium (3개월)

| # | 영역 | 이슈 | 건수 |
|---|------|------|:----:|
| 1 | FE | 직접 DOM 조작 | 6건 |
| 2 | FE | defineProps 미사용 | 대부분 페이지 |
| 3 | FE | setOption 최적화 | 15건 |
| 4 | BE | GlobalData 암묵적 의존 | 전역 |
| 5 | FE/BE | 테스트 없음 | 0% |

---

## 4. 정량적 지표 (신규 발견 포함)

| 지표 | 기존 RFC | 신규 발견 | 총계 |
|------|:--------:|:--------:|:----:|
| watch 사용 | 62건 | +1 | **63건** |
| deep watch | 미확인 | **34건** | 34건 |
| companion object | 미확인 | **29건** | 29건 |
| 동기화 코드 | 미확인 | **71건** | 71건 |
| Thread.sleep/runBlocking | 4건 | 확정 | **4건** |
| .subscribe() | 미확인 | **17건** | 17건 |
| .block() | 미확인 | **2건** | 2건 |
| DOM 직접 조작 | 미확인 | **6건** | 6건 |
| Provide/Inject | 미확인 | **0건** | 0건 |
| 번들 낭비 | ~250KB | 확정 | ~250KB |

---

## 5. RFC 업데이트 권장

### 5.1 RFC-003 추가 항목

- [ ] companion object var 상태 정리 (19건 → 최소화)
- [ ] 동기화 패턴 통일 (synchronized vs Atomic)

### 5.2 RFC-007 추가 항목

- [ ] Thread.sleep 제거 (delay로 대체)
- [ ] runBlocking 제거 (suspend로 변환)
- [ ] .block() 제거 (flatMap으로 체이닝)
- [ ] .subscribe() 에러 처리 추가

### 5.3 RFC-008 추가 항목

- [ ] deep watch 최적화 (34건)
- [ ] chart.js/vue-chartjs 제거
- [ ] ECharts 트리 쉐이킹 적용
- [ ] Provide/Inject 도입 검토
- [ ] setOption notMerge 옵션 적용

---

## 6. 우선순위 권장

```
즉시 (1주):
├── ICDService companion object 정리
├── Thread.sleep → delay 변환
└── .block() 제거

단기 (1개월):
├── chart.js 제거
├── deep watch 최적화
└── 동기화 패턴 통일

중기 (3개월):
├── ECharts 트리 쉐이킹
├── Provide/Inject 도입
└── 테스트 인프라 구축
```

---

**작성자**: Claude (10개 전문가 에이전트 병렬 분석)
**검토일**: 2026-01-14
