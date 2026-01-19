# PassSchedule 리팩토링 실행 계획

> **작성일**: 2026-01-19
> **상태**: ✅ 최종 검토 완료 (4차 전문가 검토 반영)
> **참조**: [STATE_MACHINE.md](./STATE_MACHINE.md)

---

## 개요

전문가 종합 검토 결과를 기반으로 PassScheduleService.kt 버그 수정 및 코드 품질 개선을 진행합니다.

---

## 작업 범위 (3차 협의 반영)

| Phase | 내용 | 버그 수 | 우선순위 |
|:-----:|------|:------:|:--------:|
| A | Critical 버그 수정 | **3** | P0 |
| B | High 버그 수정 | **6** | P1 |
| C | Medium 버그 수정 | 3 | P2 |
| D | 코드 품질 개선 | 22+ | P3 |

---

## Phase A: Critical (P0)

### A1. STOWING→STOWED 위치 확인

| 항목 | 내용 |
|------|------|
| **파일** | PassScheduleService.kt |
| **위치** | determineStateByTime() L2751-2755 |
| **문제** | Stow 이동 중인데 STOWED로 잘못 판정 |
| **해결** | isAtStowPosition() 함수 추가 (Train 축 포함, 허용오차 1.0, **Double 타입**) |

### A2. sendStateToFrontend() detailId 전달

| 항목 | 내용 |
|------|------|
| **파일** | PassScheduleService.kt |
| **위치** | L3001-3005 |
| **문제** | FE 테이블 하이라이트 매칭 실패 |
| **해결** | ctx.detailId 파라미터 추가, nextScheduleContext 전달 |

### A3. resetFlags() 큐 원본 업데이트

| 항목 | 내용 |
|------|------|
| **파일** | PassScheduleService.kt |
| **위치** | evaluateNextSchedule() L2827 |
| **문제** | 다중 위성 추적 시 플래그 초기화 실패 |
| **해결** | scheduleContextQueue[idx] = resetContext 추가 |

---

## Phase B: High (P1)

### B1. executeExitAction 구현

| 항목 | 내용 |
|------|------|
| **문제** | 상태 퇴장 시 정리 로직 누락 |
| **해결** | executeExitAction() 함수 추가, transitionTo()에서 호출 |

### B2. IDLE 상태 명시적 처리

| 항목 | 내용 |
|------|------|
| **위치** | executeEnterAction() L2880-2965 |
| **문제** | 리소스 정리 누락 |
| **해결** | IDLE case 추가 |

### B3. reevaluateScheduleQueue 큐 업데이트

| 항목 | 내용 |
|------|------|
| **위치** | L3042-3058 |
| **문제** | Time Offset 변경 시 플래그 불일치 |
| **해결** | 큐 원본 업데이트 로직 추가 |

### B4. ERROR 전환 + 상태 전환 유효성 검사

| 항목 | 내용 |
|------|------|
| **위치** | 전체 (transitionTo 함수) |
| **문제** | 오류 상태 진입 경로 불명확, 잘못된 상태 전환 감지 안됨 |
| **해결** | transitionToError(reason) 함수 + validTransitions 맵 추가 |
| **ERROR 조건** | 타임아웃 (4분/2분/4분), 상태 순서 위반 |

### B5. 상태별 타임아웃 추가

| 항목 | 내용 |
|------|------|
| **위치** | executePeriodicAction() |
| **문제** | STOWING, MOVING_* 상태에서 무한 대기 가능 (데드락 위험) |
| **해결** | checkStateTimeout() 함수 추가 (4분/2분/4분 타임아웃) |
| **타임아웃** | STOWING: 4분, MOVING_TRAIN: 2분, MOVING_TO_START: 4분 |

> **Note**: H4 (nextScheduleContext FE 전달)는 A2에서 함께 해결

---

## Phase C: Medium (P2)

### C1. isShuttingDown → AtomicBoolean

| 항목 | 내용 |
|------|------|
| **위치** | L192 |
| **문제** | 스레드 안전성 |
| **해결** | AtomicBoolean로 변경 |

### C2-C3. 동시성 개선 (보류)

- updateProgressFlags 컨텍스트 직접 수정
- startStateMachineTracking 스레드 안전성
- → 상세 분석 후 별도 진행

---

## Phase D: 코드 품질 (P3)

| 항목 | 개수 | 작업 |
|------|:----:|------|
| catch(Exception) | 17 | 구체적 예외 타입으로 분리 |
| "No" 필드 (V1) | 3 | "MstId"로 변경 |
| @Deprecated | 1 | 삭제 |
| 로그 레벨 | 1 | INFO → DEBUG |

---

## 실행 순서 (3차 협의 반영)

```
┌─────────────────────────────────────────────────────────┐
│  Phase A: Critical (P0) - 3개 (병렬 가능)                │
│  ┌─────┐   ┌─────┐   ┌─────┐                            │
│  │ A1  │   │ A2  │   │ A3  │                            │
│  └─────┘   └─────┘   └─────┘                            │
│  빌드 검증 ──────────────────────────────────────────►  │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Phase B-1: 기반 작업                                    │
│  ┌─────┐   ┌─────┐                                      │
│  │ B4  │ → │ B5  │  (ERROR 전환 먼저 → 타임아웃)         │
│  └─────┘   └─────┘                                      │
│  빌드 검증 ──────────────────────────────────────────►  │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Phase B-2: 상태 관리                                    │
│  ┌─────┐   ┌─────┐   ┌─────┐                            │
│  │ B1  │ → │ B2  │   │ B3  │  (Exit → IDLE, 큐 업데이트)│
│  └─────┘   └─────┘   └─────┘                            │
│  빌드 검증 ──────────────────────────────────────────►  │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Phase C-D: Medium + 품질 (P2-P3)                       │
│  별도 PR로 진행                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 검증 체크리스트 (업데이트)

### Phase A 완료 조건

- [ ] A1: isAtStowPosition() 함수 추가됨 (Az/El/Train 3축)
- [ ] A1: 허용 오차 1.0, Double 타입 적용됨
- [ ] A1: determineStateByTime()에서 위치 확인 로직 적용됨
- [ ] A2: sendStateToFrontend()에 detailId 파라미터 추가됨
- [ ] A2: nextScheduleContext 정보 FE 전달됨
- [ ] A3: resetFlags() 결과가 큐에 반영됨
- [ ] 빌드 성공 (`./gradlew clean build -x test`)

### Phase B 완료 조건

- [ ] B4: transitionToError(reason) 함수 추가됨
- [ ] B4: validTransitions 맵 추가됨 (상태 순서 검증)
- [ ] B5: checkStateTimeout() 함수 추가됨 (4분/2분/4분)
- [ ] B1: executeExitAction() 함수 구현됨
- [ ] B1: transitionTo()에서 executeExitAction() 호출됨
- [ ] B2: IDLE 상태 case 추가됨
- [ ] B3: reevaluateScheduleQueue에서 큐 업데이트됨
- [ ] 빌드 성공

---

## 수동 테스트 시나리오 ⭐ 신규

| # | 시나리오 | 검증 포인트 |
|---|----------|------------|
| T1 | 단일 위성 추적 | IDLE→...→TRACKING→COMPLETED 전체 흐름 |
| T2 | 다중 위성 연속 추적 | 스케줄 A 완료 → 스케줄 B 자동 시작, 플래그 초기화 |
| T3 | Time Offset 변경 중 추적 | reevaluateScheduleQueue 동작 |
| T4 | 추적 중 강제 정지 | IDLE 전환 확인 |
| T5 | STOWING 타임아웃 | 4분 후 ERROR 전환 |
| T6 | MOVING_TRAIN 타임아웃 | 2분 후 ERROR 전환 |
| T7 | 잘못된 상태 전환 시도 | IDLE→TRACKING 시도 시 ERROR 전환 |
| T8 | ERROR 복구 | ERROR→IDLE 전환 후 다음 스케줄 정상 진행 |
| T9 | 연속 타임아웃 | ERROR 후 다음 스케줄도 타임아웃 시 처리 |

---

## 예상 영향

| 영역 | 영향 | 비고 |
|------|------|------|
| BE 상태 머신 | 직접 수정 | 핵심 로직 |
| FE 하이라이트 | 개선 | detailId 전달로 정확도 향상 |
| 다중 위성 추적 | 버그 수정 | 플래그 초기화 문제 해결 |
| 시스템 안전성 | 향상 | 타임아웃 추가 (4분/2분/4분) |

---

## 승인

| 항목 | 상태 |
|------|:----:|
| 계획 검토 (3차 협의) | ✅ |
| 4차 전문가 검토 | ✅ |
| Phase A 승인 | ✅ |
| Phase B 승인 | ✅ |

---

## 전문가 검토 피드백 (4차)

| 전문가 | 피드백 | 반영 |
|--------|--------|:----:|
| code-reviewer | Float→Double 권장 | ✅ A1 수정 |
| tech-lead | T8/T9 테스트 추가 | ✅ 추가됨 |
| algorithm-expert | 타임아웃 필수 확인 | ✅ B5 |
| architect | ERROR 복구 로직 검증 | ✅ T8 추가 |

---

**다음 단계**: Phase A 구현 시작