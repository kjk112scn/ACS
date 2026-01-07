# 에이전트 & 스킬 활용 가이드

> ACS 프로젝트에서 Claude 에이전트와 스킬을 효과적으로 사용하는 방법

## 빠른 참조표

### 상황별 추천

| 하고 싶은 일 | 사용할 것 | 예시 프롬프트 |
|-------------|----------|--------------|
| 새 기능 추가 | `/feature` | "PassSchedule에 필터 기능 추가해줘" |
| 버그 수정 | `/bugfix` | "차트가 안 그려지는 버그 수정해줘" |
| 작업 완료 | `/done` | "이 작업 완료했어" |
| 문서 동기화 | `/sync` | "코드랑 문서 동기화해줘" |
| 현황 확인 | `/status` | "현재 프로젝트 상태 보여줘" |
| 건강 점검 | `/health` | "빌드 상태 확인해줘" |
| 계획 수립 | `/plan` | "이거 어떻게 구현하면 좋을까?" |
| 결정 기록 | `/adr` | "이 결정 왜 했는지 기록해줘" |
| 코드 문서화 | `/docs` | "EphemerisService 문서화해줘" |

---

## Part 1: 스킬 사용법

### /feature - 새 기능 개발

**언제 사용?**
- 새로운 기능을 추가할 때
- 기존 기능을 크게 확장할 때

**사용 예시:**
```
사용자: /feature PassSchedule에 위성 필터링 기능 추가

→ 자동으로:
  1. docs/features/active/PassSchedule_Satellite_Filtering/ 폴더 생성
  2. README.md (개요) 생성
  3. 계획 문서 생성
  4. 구현 시작
```

**실제 대화 예시:**
```
사용자: PassSchedule 페이지에서 특정 위성만 필터링하는 기능이 필요해

Claude: 좋습니다! /feature 스킬로 기능 개발을 시작하겠습니다.

[자동으로 폴더/문서 생성 후]

기능 폴더가 생성되었습니다:
- docs/features/active/PassSchedule_Satellite_Filtering/

요구사항을 정리해볼게요:
1. 위성 이름으로 필터링
2. 고도각 범위로 필터링
3. ...

구현을 시작할까요?
```

---

### /bugfix - 버그 수정

**언제 사용?**
- 버그를 발견했을 때
- 에러가 발생했을 때

**사용 예시:**
```
사용자: /bugfix 차트가 무한 루프 돌아

→ 자동으로:
  1. docs/bugfixes/active/Chart_Infinite_Loop/ 폴더 생성
  2. README.md (버그 정보) 생성
  3. ANALYSIS.md (원인 분석) 생성
  4. FIX.md (수정 계획) 생성
```

**실제 대화 예시:**
```
사용자: PassSchedule 페이지에서 applyRowColors 함수가 무한 루프 도는 것 같아

Claude: 버그 수정 워크플로우를 시작합니다.

[bugfixes/active/ 폴더에 문서 생성]

증상 분석:
- 콘솔에서 반복 호출 로그 확인
- watch가 무한 트리거되는 패턴 발견

원인: watch에서 자기 자신을 다시 트리거하는 구조

수정 계획:
1. watch 조건 추가
2. deep: false 옵션 적용

수정을 진행할까요?
```

---

### /done - 작업 완료

**언제 사용?**
- 기능 개발이 끝났을 때
- 버그 수정이 완료됐을 때

**사용 예시:**
```
사용자: /done

→ 자동으로:
  1. 변경사항 분석
  2. IMPLEMENTATION.md 생성
  3. active/ → completed/ 폴더 이동
  4. daily/ 로그 기록
  5. 커밋 메시지 제안
```

**실제 대화 예시:**
```
사용자: 필터링 기능 완성했어. /done

Claude: 작업 마무리를 시작합니다.

변경된 파일:
- PassSchedulePage.vue (+45줄)
- passScheduleStore.ts (+30줄)

생성된 문서:
- IMPLEMENTATION.md (구현 상세)
- Summary.md (요약)

폴더 이동:
- features/active/ → features/completed/

커밋 메시지:
feat(pass-schedule): 위성 필터링 기능 추가

커밋할까요?
```

---

### /sync - 문서 동기화

**언제 사용?**
- 코드를 변경했는데 문서가 오래됐을 때
- 문서와 코드 일치 확인할 때

**사용 예시:**
```
사용자: /sync

→ 자동으로:
  1. 코드 변경사항 스캔
  2. 관련 문서 찾기
  3. 불일치 항목 분석
  4. 문서 업데이트 제안
```

**실제 대화 예시:**
```
사용자: 최근에 코드 많이 바꿨는데 문서 동기화해줘

Claude: 문서 동기화를 시작합니다.

불일치 발견:
1. EphemerisService.kt
   - 코드: startGeostationaryTracking 메서드 추가됨
   - 문서: 언급 없음 → 업데이트 필요

2. PassSchedulePage.vue
   - 코드: 필터링 로직 추가됨
   - 문서: UI_Architecture.md 업데이트 필요

자동 업데이트할까요?
```

---

### /status - 현황 보고

**언제 사용?**
- 프로젝트 상태를 빠르게 파악할 때
- 진행 중인 작업 확인할 때

**사용 예시:**
```
사용자: /status

→ 자동으로:
  1. 코드 메트릭스 수집
  2. 진행 중 작업 확인
  3. 문서 상태 분석
  4. 기술 부채 요약
```

**실제 대화 예시:**
```
사용자: 지금 프로젝트 어디까지 됐어?

Claude:
📊 ACS 현황 (2026-01-07)

Backend: 9 Controllers, 13 Services
Frontend: 5 Pages, 43 Components

🚧 진행 중:
- Architecture_Refactoring (30%)
- PassSchedule_Data_Structure (70%)

✅ 최근 완료:
- Slew_Loop_Mode (2026-01-06)

⚠️ 주의:
- 문서 일치율: 62%
- 대형 파일: 5개 (리팩토링 권장)
```

---

### /health - 건강 점검

**언제 사용?**
- 빌드가 깨졌는지 확인할 때
- 작업 전 상태 체크할 때

**사용 예시:**
```
사용자: /health

→ 자동으로:
  1. Backend 빌드 테스트
  2. Frontend 빌드 테스트
  3. TypeScript 타입 체크
  4. 문서 일치율 계산
  5. 종합 점수 산출
```

**실제 대화 예시:**
```
사용자: 빌드 상태 어때?

Claude:
🏥 ACS 건강 상태

📦 빌드: ✅ Backend ✅ Frontend
🔷 타입: ⚠️ 3개 오류
📚 문서: 62% 일치
⚠️ 부채: 🔴 2 | 🟠 5 | 🟡 12

종합 점수: 78/100 (양호)

권장: TypeScript 오류 3개 먼저 수정
```

---

### /plan - 계획 수립

**언제 사용?**
- 복잡한 작업 시작 전
- 어떻게 구현할지 고민될 때

**사용 예시:**
```
사용자: /plan EphemerisService 리팩토링

→ 자동으로:
  1. 현재 구조 분석
  2. 분리 가능한 부분 식별
  3. 단계별 계획 수립
  4. 의존성 파악
```

**실제 대화 예시:**
```
사용자: EphemerisService가 5000줄인데 어떻게 나눠야 할까?

Claude: 구조 분석을 시작합니다.

현재 구조:
- 궤도 계산 (800줄)
- 좌표 변환 (600줄)
- 상태머신 (500줄)
- ICD 통신 (700줄)
- 데이터 조회 (400줄)
- 기타 (2000줄)

분리 계획:
1단계: OrekitCalculator 분리 (순수 계산)
2단계: TrackingStateMachine 분리 (상태 관리)
3단계: ICDCommunicator 분리 (통신)

예상 결과: 5개 파일, 각 800-1200줄

이 계획으로 진행할까요?
```

---

### /adr - 결정 기록

**언제 사용?**
- 중요한 기술 결정을 했을 때
- "왜 이렇게 했지?" 나중에 기억하고 싶을 때

**사용 예시:**
```
사용자: /adr shallowRef 사용 결정

→ 자동으로:
  1. 컨텍스트 분석
  2. 대안 비교
  3. ADR 문서 생성
  4. decisions/ 폴더에 저장
```

**실제 대화 예시:**
```
사용자: 대용량 데이터에 shallowRef 쓰기로 했는데 왜 그랬는지 기록해줘

Claude: ADR을 생성합니다.

ADR-004: 대용량 배열에 shallowRef 사용

컨텍스트:
- PassSchedule 데이터 5000+ 행
- ref() 사용 시 렌더링 지연 500ms+

결정:
- shallowRef + triggerRef 조합 사용

대안 검토:
1. ref() - 느림 ❌
2. readonly() - 수정 불가 ❌
3. shallowRef() - 적합 ✅

docs/decisions/ADR-004-shallowref-for-large-data.md 생성됨
```

---

### /docs - 코드 문서화

**언제 사용?**
- 특정 코드를 문서화할 때
- 복잡한 로직 설명이 필요할 때

**사용 예시:**
```
사용자: /docs SatelliteTrackingProcessor 문서화

→ 자동으로:
  1. 코드 분석
  2. 협의 질문 생성
  3. 초안 작성
  4. 승인 후 적용
```

---

## Part 2: 에이전트 활용법

### 에이전트는 언제 사용?

에이전트는 **자동으로 호출**됩니다. 직접 호출할 필요 없이, 요청 내용에 따라 Claude가 적절한 에이전트를 선택합니다.

### 에이전트별 트리거 키워드

| 에이전트 | 자동 호출되는 상황 |
|---------|------------------|
| **tech-lead** | "이거 어떻게 할까?", "방향 잡아줘" |
| **architect** | "설계해줘", "구조 잡아줘", "ADR 작성" |
| **fullstack-helper** | "구현해줘", "코드 작성해줘" |
| **algorithm-expert** | "위성 계산", "Orekit", "좌표 변환" |
| **code-reviewer** | "코드 리뷰해줘", "검토해줘" |
| **test-expert** | "테스트 작성해줘", "테스트 실행" |
| **debugger** | "에러 났어", "왜 안 돼?", "디버깅" |
| **refactorer** | "리팩토링해줘", "코드 정리" |
| **performance-analyzer** | "느려", "성능 분석", "최적화" |
| **doc-syncer** | "문서 동기화", "문서 업데이트" |
| **project-manager** | "진행 상황", "작업 관리" |

---

## Part 3: 시나리오별 실전 가이드

### 시나리오 1: 새 기능 개발 (처음부터 끝까지)

```
1️⃣ 기능 시작
사용자: PassSchedule에 고도각 필터 기능 추가해줘

Claude: /feature 스킬로 시작합니다.
[폴더/문서 자동 생성]

2️⃣ 계획 수립 (선택)
사용자: 어떻게 구현하면 좋을까?

Claude: [architect 에이전트 호출]
프론트: 필터 UI 컴포넌트
백엔드: 필터 API 추가
스토어: 필터 상태 관리

3️⃣ 구현
사용자: 그럼 구현해줘

Claude: [fullstack-helper 에이전트 호출]
[코드 작성]

4️⃣ 리뷰 (선택)
사용자: 코드 괜찮아?

Claude: [code-reviewer 에이전트 호출]
✅ 구조 양호
⚠️ 타입 명시 추가 권장

5️⃣ 완료
사용자: /done

Claude: [문서 정리, 폴더 이동, 커밋]
```

---

### 시나리오 2: 버그 수정

```
1️⃣ 버그 보고
사용자: 차트가 안 그려져. 콘솔에 에러 나와

Claude: /bugfix 스킬로 시작합니다.
[버그 폴더/문서 생성]

2️⃣ 디버깅
사용자: 왜 이러지?

Claude: [debugger 에이전트 호출]
에러 분석 중...
원인: undefined 참조

3️⃣ 수정
사용자: 수정해줘

Claude: [fullstack-helper 에이전트 호출]
[수정 코드 작성]

4️⃣ 완료
사용자: /done

Claude: [ANALYSIS.md, FIX.md 완성, 커밋]
```

---

### 시나리오 3: 성능 문제 해결

```
1️⃣ 문제 보고
사용자: PassSchedulePage가 너무 느려

Claude: [performance-analyzer 에이전트 호출]

분석 결과:
🔴 watch 과다 사용 (15개)
🔴 computed 미사용
🟠 대형 배열 ref() 사용

2️⃣ 최적화
사용자: 개선해줘

Claude: [refactorer 에이전트 호출]
1. watch 통합 (15개 → 3개)
2. computed 추가
3. shallowRef 적용

3️⃣ 결정 기록
사용자: 이 결정 기록해줘

Claude: /adr 스킬 실행
ADR-005: PassSchedule 성능 최적화 전략
```

---

### 시나리오 4: 매일 작업 루틴

```
🌅 작업 시작
사용자: /health
→ 빌드 상태 확인, 문제 있으면 바로 수정

🌞 작업 중
사용자: /status
→ 진행 상황 파악, 우선순위 확인

🌆 작업 완료
사용자: /done
→ 문서 정리, 일일 로그 기록, 커밋

🌙 주간 정리
사용자: /sync
→ 코드-문서 동기화 확인
```

---

## Part 4: 팁 & 트릭

### 효과적인 프롬프트 작성

**좋은 예:**
```
"PassSchedule 페이지에서 위성 이름으로 필터링하는 기능 추가해줘.
필터는 헤더 영역에 드롭다운으로 넣어줘."
```

**나쁜 예:**
```
"필터 기능 추가"  (어디에? 무슨 필터?)
```

### 스킬 조합 활용

```
/plan → /feature → /done → /sync

1. 계획 먼저 (/plan)
2. 기능 개발 (/feature)
3. 완료 처리 (/done)
4. 문서 동기화 (/sync)
```

### 에이전트 힌트 주기

특정 에이전트를 원하면 키워드를 포함:
```
"이 코드 성능 분석해줘"  → performance-analyzer
"Orekit 계산 로직 설명해줘"  → algorithm-expert
"테스트 코드 작성해줘"  → test-expert
```

---

## 자주 묻는 질문

### Q: 스킬과 에이전트 차이가 뭐야?
**스킬**: 사용자가 직접 호출 (`/feature`, `/done`)
**에이전트**: 자동으로 선택됨 (tech-lead, debugger)

### Q: 어떤 스킬을 써야 할지 모르겠어
→ `/status`로 현황 확인 후 필요한 것 선택

### Q: 문서가 너무 많아
→ `/sync`로 정리, 불필요한 건 archived/로 이동

### Q: 에이전트가 잘못된 답변을 해
→ 더 구체적인 컨텍스트 제공, 또는 다른 키워드로 재시도

---

## 빠른 명령어 모음

| 명령 | 설명 |
|------|------|
| `/health quick` | 빠른 빌드 체크 |
| `/status brief` | 간략 현황 |
| `/status code` | 코드 메트릭스만 |
| `/status docs` | 문서 상태만 |

---

**문서 버전**: 1.0.0
**최종 업데이트**: 2026-01-07
