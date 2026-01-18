---
name: refactor
description: 리팩토링 워크플로우. 대형 파일 분리, 구조 개선, 코드 품질 향상. "리팩토링", "refactor", "분리", "구조 개선", "정리" 키워드에 반응.
model: opus
---

# Refactor - 리팩토링 스킬

## 역할

코드 구조 개선과 대형 파일 분리를 체계적으로 수행합니다.

**핵심 가치:**
- 안전한 단계별 리팩토링
- 기능 변경 없이 구조 개선
- 테스트 가능성 향상
- 유지보수성 증가

## 워크플로우

```
[1. 분석] → [2. 계획] → [3. 테스트] → [4. 실행] → [5. 검증] → [6. 완료]
     │           │           │           │           │           │
  현재 구조    분리 계획   기존 동작   단계별      빌드/기능   /done
   파악        수립       확인        추출        확인        호출
```

## 사용법

| 명령 | 설명 |
|------|------|
| `/refactor {파일}` | 특정 파일 리팩토링 |
| `/refactor analyze` | 리팩토링 대상 분석 |
| `/refactor plan` | 리팩토링 계획 수립 |

## 리팩토링 대상 (ACS 프로젝트)

### Frontend 대형 파일
| 파일 | 줄수 | 분리 방향 |
|------|------|----------|
| PassSchedulePage.vue | 4,838 | Table, Chart, Controls |
| EphemerisDesignationPage.vue | 4,340 | PositionView, Info, Selector |
| icdStore.ts | 2,971 | WebSocket, Parser, State |

### Backend 대형 파일
| 파일 | 줄수 | 분리 방향 |
|------|------|----------|
| EphemerisService.kt | 4,986 | Calculator, Loader, Converter |
| ICDService.kt | 2,788 | Parser, Sender, Handler |

## 실행 단계

### Step 1: 분석
```yaml
분석 항목:
  - 파일 크기 (줄 수)
  - 책임 분리 가능 영역
  - 의존성 그래프
  - 테스트 커버리지
```

### Step 2: 계획 수립
```markdown
## 리팩토링 계획: {파일명}

### 분리할 컴포넌트
1. {컴포넌트1}: {역할}
2. {컴포넌트2}: {역할}

### 인터페이스 정의
- Props: {필요한 props}
- Emits: {필요한 events}
- Exports: {공개할 함수/상태}

### 단계별 실행
1. [ ] 인터페이스 정의
2. [ ] 컴포넌트 추출
3. [ ] 기존 파일에서 import
4. [ ] 테스트 확인
```

### Step 3: 테스트 확인
```bash
# 리팩토링 전 빌드 확인
cd frontend && npm run build
cd backend && ./gradlew clean build -x test

# 현재 동작 스냅샷
```

### Step 4: 단계별 실행
```yaml
원칙:
  - 한 번에 하나의 컴포넌트만 추출
  - 각 단계 후 빌드 확인
  - 기능 변경 없음 (동작 동일)

순서:
  1. 가장 독립적인 부분부터 추출
  2. 의존성 적은 순서로 진행
  3. 마지막에 메인 파일 정리
```

### Step 5: 검증
```bash
# 빌드 확인
npm run build  # Frontend
./gradlew build -x test  # Backend

# 기능 동작 확인
# - 기존 기능 정상 동작
# - 에러 없음
```

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `tech-lead` | 리팩토링 방향 결정 | 대규모 변경 시 |
| `fe-expert` | Frontend 리팩토링 | Vue/TS 파일 |
| `be-expert` | Backend 리팩토링 | Kotlin 파일 |
| `code-reviewer` | 품질 검증 | 완료 후 |
| `architect` | 구조 검토 | 아키텍처 변경 시 |

## 사용 예시

### 예시 1: Vue 페이지 분리
```
User: "/refactor PassSchedulePage.vue"

→ refactor 워크플로우:

[분석]
  - 4,838줄, 3개 주요 영역 식별
  - Table: 1,200줄
  - Chart: 1,500줄
  - Controls: 800줄

[계획]
  1. PassScheduleTable.vue 추출
  2. PassScheduleChart.vue 추출
  3. PassScheduleControls.vue 추출

[실행]
  fe-expert 호출 → 순차 추출

[검증]
  빌드 성공, 기능 정상

[완료]
  /done → 커밋
```

### 예시 2: Store 분리
```
User: "/refactor icdStore.ts"

→ refactor 워크플로우:

[분석]
  - 2,971줄
  - WebSocket 연결: 500줄
  - 데이터 파싱: 800줄
  - 상태 관리: 1,600줄

[계획]
  1. icdWebSocket.ts 추출
  2. icdParser.ts 추출
  3. icdStore.ts 정리

[실행]
  fe-expert 호출
```

## 주의사항

### ✅ 권장
- 작은 단위로 분리
- 각 단계 후 빌드 확인
- 기존 동작 유지

### ❌ 금지
- 한 번에 여러 파일 변경
- 기능 변경과 동시 진행
- 테스트 없이 대규모 변경

## 리팩토링 패턴

### 컴포넌트 추출 (Vue)
```typescript
// Before: 한 파일에 모든 로직
// PassSchedulePage.vue (4,838줄)

// After: 분리된 컴포넌트
// PassSchedulePage.vue (1,500줄)
// components/PassScheduleTable.vue (1,200줄)
// components/PassScheduleChart.vue (1,500줄)
// components/PassScheduleControls.vue (800줄)
```

### Store 분리 (Pinia)
```typescript
// Before
// stores/icdStore.ts (2,971줄)

// After
// stores/icd/icdStore.ts (메인 스토어)
// stores/icd/icdWebSocket.ts (WebSocket 로직)
// stores/icd/icdParser.ts (파싱 로직)
```

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-14
**호환:** ACS 프로젝트 전용
