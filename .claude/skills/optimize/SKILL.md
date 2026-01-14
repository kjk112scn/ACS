---
name: optimize
description: 성능 최적화 워크플로우. icdStore, 렌더링, 메모리, 번들 크기 최적화. "최적화", "optimize", "성능", "느려", "빨리" 키워드에 반응.
---

# Optimize - 성능 최적화 스킬

## 역할

프론트엔드/백엔드 성능 문제를 분석하고 최적화합니다.

**핵심 가치:**
- 측정 기반 최적화
- 병목 지점 식별
- 실질적 성능 향상
- 사용자 경험 개선

## 워크플로우

```
[1. 측정] → [2. 분석] → [3. 계획] → [4. 최적화] → [5. 검증] → [6. 완료]
     │           │           │           │           │           │
  현재 성능    병목 식별    개선 방안   코드 수정   성능 비교    /done
   측정        분석        수립                     (Before/After)
```

## 사용법

| 명령 | 설명 |
|------|------|
| `/optimize` | 전체 성능 분석 |
| `/optimize icdStore` | icdStore 최적화 |
| `/optimize {파일}` | 특정 파일 최적화 |
| `/optimize memory` | 메모리 최적화 |
| `/optimize bundle` | 번들 크기 최적화 |

## 최적화 대상 (ACS 프로젝트)

### 🔴 Critical: icdStore 최적화
```
현재 문제:
- 개별 ref 175개 (안테나 81 + 보드상태 72 + 추적 13)
- 30ms마다 전체 리렌더링
- CPU 사용률 과다

목표:
- shallowRef 그룹화 (5-10개)
- 선택적 업데이트
- CPU 70-80% 감소
```

### 🟠 High: 차트 성능
```
현재 문제:
- convertToChartData: 4-5회 순회
- Date 객체 반복 생성

목표:
- 단일 순회 처리
- 객체 재사용
```

### 🟡 Medium: 번들 크기
```
분석 대상:
- 미사용 의존성
- Tree-shaking 확인
- 코드 분할
```

## icdStore 최적화 상세

### Before (현재)
```typescript
// 개별 ref 175개
const azimuthAngle = ref(0)
const elevationAngle = ref(0)
const trainAngle = ref(0)
// ... 172개 더

// 30ms마다 전체 업데이트
setInterval(() => {
  azimuthAngle.value = data.az
  elevationAngle.value = data.el
  // ... 175개 업데이트 → 175회 리렌더링 트리거
}, 30)
```

### After (최적화)
```typescript
// 그룹화된 shallowRef
interface AntennaState {
  azimuth: number
  elevation: number
  train: number
  // ... 관련 필드
}

const antennaState = shallowRef<AntennaState>({...})
const boardState = shallowRef<BoardState>({...})
const trackingState = shallowRef<TrackingState>({...})

// 30ms마다 배치 업데이트
setInterval(() => {
  // 한 번의 객체 교체 → 최소 리렌더링
  antennaState.value = { ...parseAntennaData(data) }
}, 30)
```

### 마이그레이션 단계
```yaml
Step 1: 인터페이스 정의
  - AntennaState, BoardState, TrackingState 타입 정의

Step 2: shallowRef 생성
  - 기존 ref 유지하면서 새 shallowRef 추가

Step 3: 점진적 전환
  - 컴포넌트별로 새 shallowRef 사용으로 전환

Step 4: 기존 ref 제거
  - 모든 컴포넌트 전환 후 제거
```

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `fe-expert` | FE 성능 최적화 | icdStore, 렌더링 |
| `be-expert` | BE 성능 최적화 | API, DB 쿼리 |
| `code-reviewer` | 최적화 검증 | 완료 후 |

## 측정 방법

### Frontend
```typescript
// 렌더링 시간 측정
console.time('render')
// ... 렌더링 로직
console.timeEnd('render')

// Vue DevTools
// - Component 렌더 시간
// - Reactivity 추적

// Chrome DevTools Performance
// - CPU 프로파일링
// - 메모리 사용량
```

### Backend
```kotlin
// API 응답 시간 측정
val start = System.currentTimeMillis()
// ... 로직
logger.info("처리 시간: ${System.currentTimeMillis() - start}ms")
```

## 사용 예시

### 예시 1: icdStore 최적화
```
User: "/optimize icdStore"

→ optimize 워크플로우:

[측정]
  - 현재: 30ms마다 175회 ref 업데이트
  - CPU: 높음
  - 리렌더링: 과다

[분석]
  - 병목: 개별 ref 업데이트
  - 원인: Vue reactivity 오버헤드

[계획]
  1. AntennaState 인터페이스 정의
  2. shallowRef 그룹화
  3. 배치 업데이트 적용

[최적화]
  fe-expert 호출 → 단계별 적용

[검증]
  - Before: CPU 80%
  - After: CPU 15%
  - 개선: 81% 감소 ✅

[완료]
  /done → 커밋
```

### 예시 2: 차트 성능
```
User: "/optimize passScheduleService.ts"

→ optimize 워크플로우:

[분석]
  - convertToChartData: 4-5회 순회
  - Date 객체 매번 생성

[최적화]
  - 단일 순회로 변경
  - Date 객체 캐싱

[검증]
  - Before: 200ms
  - After: 45ms
  - 개선: 78% ✅
```

## 최적화 체크리스트

### Frontend
- [ ] 불필요한 리렌더링 제거
- [ ] computed 캐싱 활용
- [ ] shallowRef 사용 (대규모 객체)
- [ ] v-if vs v-show 적절히 사용
- [ ] 가상 스크롤 (대량 데이터)

### Backend
- [ ] N+1 쿼리 제거
- [ ] 캐싱 적용
- [ ] 비동기 처리
- [ ] 배치 처리

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-14
**호환:** ACS 프로젝트 전용
