# PassSchedule 상태머신 코드 리뷰

**리뷰일**: 2026-01-26
**분석 깊이**: Deep
**분석자**: Claude Code /review

---

## 1. 분석 범위

### 대상 파일

| 영역 | 파일 | 라인 수 |
|------|------|--------|
| BE | PassScheduleService.kt | ~2900 |
| FE | SelectScheduleContent.vue | ~1400 |

### 분석 관점

- [x] 상태머신
- [x] 타이밍/비동기
- [x] 경쟁조건
- [x] 데이터 흐름
- [x] 색상 구분 로직
- [x] 스케줄 선택 로직

---

## 2. 요약

| 심각도 | 개수 | 즉시 조치 |
|:------:|:----:|:---------:|
| 🔴 Critical | 1 | Yes |
| 🟠 High | 3 | Yes |
| 🟡 Medium | 6 | No |
| 🟢 Low | 8 | No |
| 💡 Suggestion | 2 | No |

**총 발견 항목:** 20개

---

## 3. 상태머신 분석

### 3.1 상태 정의 (BE)

| 상태 | 설명 | 진입 조건 | 탈출 조건 |
|------|------|----------|----------|
| IDLE | 초기 상태 | Start 또는 완료 후 | 스케줄 로드 |
| STOWING | Stow 이동 중 | Auto-Stow 활성화 | Stow 완료 |
| STOWED | Stow 완료 | Stow 도달 | Train 이동 시작 |
| MOVING_TRAIN | Train 이동 | Stow 후 | 목표 도달 |
| TRAIN_STABILIZING | Train 안정화 | Train 도달 | 3초 대기 완료 |
| MOVING_TO_START | AOS 위치 이동 | Train 안정화 | AOS 위치 도달 |
| READY | 추적 대기 | AOS 위치 도착 | AOS 시간 도달 |
| TRACKING | 추적 중 | AOS 시간 | LOS 도달 |
| POST_TRACKING | 추적 완료 처리 | LOS 도달 | 정리 완료 |
| COMPLETED | 스케줄 완료 | POST_TRACKING 후 | 다음 스케줄 |
| ERROR | 오류 상태 | 예외 발생 | 리셋 |

### 3.2 상태 전이 다이어그램

```
┌──────────┐                     ┌──────────┐
│   IDLE   │──── Start ─────────►│ STOWING  │
└──────────┘                     └────┬─────┘
      ▲                               │
      │                               ▼
┌─────┴────┐                     ┌──────────┐
│COMPLETED │◄────────────────────│  STOWED  │
└──────────┘                     └────┬─────┘
      ▲                               │
      │                               ▼
┌─────┴────────┐                 ┌───────────────┐
│POST_TRACKING │                 │ MOVING_TRAIN  │
└──────────────┘                 └───────┬───────┘
      ▲                                  │
      │                                  ▼
┌─────┴────┐                     ┌────────────────────┐
│ TRACKING │                     │ TRAIN_STABILIZING  │
└──────────┘                     └─────────┬──────────┘
      ▲                                    │
      │                                    ▼
┌─────┴────┐                     ┌─────────────────┐
│  READY   │◄────────────────────│ MOVING_TO_START │
└──────────┘                     └─────────────────┘
```

---

## 4. 색상 구분 로직 분석 (FE)

### 4.1 색상 조건

| 우선순위 | 조건 | CSS 클래스 | 색상 |
|:--------:|------|-----------|------|
| 1 | 시간 중복 | `overlapping-row` | 빨강 배경 |
| 2 | 비활성화 | `disabled-row` | 회색 배경 |
| 3 | Keyhole | `keyhole-row` | 노랑 배경 |
| 4 | 현재 선택 | `selected` | 강조 |
| 5 | 기본 | - | 기본 배경 |

### 4.2 색상 로직 코드

```typescript
// SelectScheduleContent.vue - getRowClass()
function getRowClass(row: PassScheduleRow): string {
  if (isOverlapping(row)) return 'overlapping-row'
  if (row.disabled) return 'disabled-row'
  if (isKeyhole(row)) return 'keyhole-row'
  if (row.mstId === selectedSchedule.value?.mstId) return 'selected'
  return ''
}
```

### 4.3 검증 결과

✅ **색상 구분 로직 정상 동작**
- 우선순위 기반 조건 검사 올바름
- CSS 클래스 적용 정확함
- 현재/다음 스케줄 구분 가능

---

## 5. 🔴 Critical Issues

### C1. 상태 전이 원자성 부재

| 항목 | 내용 |
|------|------|
| **파일** | `PassScheduleService.kt` |
| **위치** | 전체 (상태 관리 부분) |
| **문제** | `currentPassScheduleState` 변수가 일반 `var`로 선언되어 동시성 문제 발생 가능 |
| **영향** | 상태 전이 중 다른 스레드 접근 시 불일치 상태 |
| **권장 조치** | `AtomicReference` 또는 `synchronized` 블록 사용 |

```kotlin
// 현재 코드 (위험)
private var currentPassScheduleState = PassScheduleState.IDLE

// 권장 수정
private val currentPassScheduleState = AtomicReference(PassScheduleState.IDLE)

// 상태 전이 시
fun transitionTo(newState: PassScheduleState) {
    val oldState = currentPassScheduleState.getAndSet(newState)
    logger.info("State transition: $oldState -> $newState")
}
```

**연계:** `/bugfix` 권장

---

## 6. 🟠 High Issues

### H1. 큐 동기화 불일치

| 항목 | 내용 |
|------|------|
| **파일** | `PassScheduleService.kt` |
| **위치** | L2860-2867 |
| **문제** | `queueLock`이 일부 큐 접근에만 사용됨 |
| **권장 조치** | 모든 큐 접근에 동기화 적용 |

### H2. 상태 플래그 분산

| 항목 | 내용 |
|------|------|
| **파일** | `PassScheduleService.kt` |
| **위치** | 여러 위치 |
| **문제** | `isStowed`, `trainMoveStarted` 등 개별 플래그가 상태머신과 분리됨 |
| **권장 조치** | 상태머신에 통합하거나 명확한 동기화 |

### H3. 타이밍 드리프트 미처리

| 항목 | 내용 |
|------|------|
| **파일** | `PassScheduleService.kt` |
| **위치** | 타이머 관련 부분 |
| **문제** | 30ms 주기 타이머의 누적 오차 미보정 |
| **권장 조치** | 절대 시간 기반 보정 로직 추가 |

---

## 7. 🟡 Medium Issues

### M1. watch deep 옵션 사용

| 항목 | 내용 |
|------|------|
| **파일** | `SelectScheduleContent.vue` |
| **위치** | L380-415 |
| **문제** | `{ deep: true }` 옵션이 성능에 영향 |
| **권장 조치** | 필요한 속성만 개별 watch |

### M2. 매직 넘버 사용

| 항목 | 내용 |
|------|------|
| **파일** | `PassScheduleService.kt` |
| **위치** | 여러 위치 |
| **문제** | `3000` (3초 대기), `120000` (2분) 등 하드코딩 |
| **권장 조치** | 상수로 추출 |

```kotlin
companion object {
    const val TRAIN_STABILIZATION_MS = 3000L
    const val PREPARE_BEFORE_AOS_MS = 120000L
}
```

### M3. 에러 복구 로직 미흡

| 항목 | 내용 |
|------|------|
| **파일** | `PassScheduleService.kt` |
| **위치** | ERROR 상태 처리 |
| **문제** | ERROR 상태에서 IDLE 복귀 로직 불명확 |
| **권장 조치** | 명시적 복구 메서드 추가 |

### M4. console.log 잔존 (FE)

| 항목 | 내용 |
|------|------|
| **파일** | `SelectScheduleContent.vue` |
| **위치** | 여러 위치 |
| **문제** | 디버깅용 console.log 잔존 |
| **권장 조치** | `/cleanup` 실행 |

### M5. 타입 단언 사용 (FE)

| 항목 | 내용 |
|------|------|
| **파일** | `SelectScheduleContent.vue` |
| **위치** | API 응답 처리 |
| **문제** | `as` 타입 단언 다수 사용 |
| **권장 조치** | 타입 가드 함수 사용 |

### M6. catch(Exception) 사용 (BE)

| 항목 | 내용 |
|------|------|
| **파일** | `PassScheduleService.kt` |
| **위치** | 예외 처리 블록 |
| **문제** | 광범위한 Exception catch |
| **권장 조치** | 구체적 예외 타입 지정 |

---

## 8. 🟢 Low Issues

| # | 문제 | 파일 | 권장 조치 |
|---|------|------|----------|
| L1 | KDoc 누락 | PassScheduleService.kt | 문서화 추가 |
| L2 | 중복 코드 | PassScheduleService.kt | 헬퍼 함수 추출 |
| L3 | 미사용 import | SelectScheduleContent.vue | 정리 |
| L4 | 긴 함수 | PassScheduleService.kt | 분리 |
| L5 | 하드코딩 색상 | SelectScheduleContent.vue | CSS 변수 사용 |
| L6 | 변수명 일관성 | 양쪽 | train/tilt 통일 |
| L7 | 로깅 레벨 | PassScheduleService.kt | 적절한 레벨 |
| L8 | 테스트 부재 | - | 단위 테스트 추가 |

---

## 9. 💡 Suggestions

### S1. 상태머신 패턴 적용

현재 상태 관리를 공식 상태머신 패턴으로 리팩토링하면 유지보수성 향상

```kotlin
sealed class PassScheduleState {
    object Idle : PassScheduleState()
    object Tracking : PassScheduleState()
    // ...

    fun canTransitionTo(target: PassScheduleState): Boolean {
        return when (this) {
            is Idle -> target is Stowing || target is MovingTrain
            // ...
        }
    }
}
```

### S2. FE-BE 타입 동기화

API 응답 타입과 FE 인터페이스 자동 동기화 고려

**연계:** `/api-sync` 실행 권장

---

## 10. 체크리스트 결과

### Backend

| ID | 항목 | 결과 | 비고 |
|----|------|:----:|------|
| BE-W01 | block() 미사용 | ✅ | |
| BE-M01 | 상태 전이 정확 | ⚠️ | 원자성 문제 |
| BE-M06 | 상태 전이 원자성 | ❌ | Critical |
| BE-C01 | Race Condition 없음 | ❌ | 동기화 불일치 |
| BE-K01 | !! 연산자 미사용 | ✅ | |

### Frontend

| ID | 항목 | 결과 | 비고 |
|----|------|:----:|------|
| FE-R01 | shallowRef 적절 사용 | ✅ | |
| FE-R04 | watch deep 제한 | ⚠️ | 개선 필요 |
| FE-L01 | 리소스 정리 | ✅ | |
| FE-D01 | 라디안/도 변환 | ✅ | |

### 모드 시스템

| ID | 항목 | 결과 | 비고 |
|----|------|:----:|------|
| PS-01 | 스케줄 큐 동기화 | ⚠️ | 부분적 |
| PS-02 | 컨텍스트 플래그 리셋 | ✅ | |
| PS-03 | 2분 기준 시간 계산 | ✅ | |
| PS-04 | Train 안정화 3초 | ✅ | |
| PS-07 | 색상 구분 로직 | ✅ | |

---

## 11. 권장 개선 작업

| 우선순위 | 작업 | 스킬 | 영향 |
|:--------:|------|------|------|
| 1 | 상태 전이 원자성 수정 | `/bugfix` | Critical |
| 2 | 큐 동기화 통일 | `/bugfix` | High |
| 3 | 상태 플래그 통합 | `/refactor` | High |
| 4 | 타이밍 드리프트 처리 | `/bugfix` | High |
| 5 | console.log 정리 | `/cleanup` | Medium |
| 6 | 매직 넘버 상수화 | `/refactor` | Medium |
| 7 | 상태머신 패턴 적용 | `/feature` | 구조 개선 |

---

## 12. 다음 단계

개선을 진행하시겠습니까?

1. **🔴 Critical 즉시 수정** → `/bugfix` 연계 (상태 전이 원자성)
2. **🟠 High 수정** → `/bugfix` 연계 (큐 동기화, 타이밍)
3. **구조 개선** → `/refactor` 연계 (상태머신 패턴)
4. **코드 정리** → `/cleanup` 연계 (console.log, 미사용 코드)
5. **보류** → 나중에 처리

---

## 부록

### A. 분석에 사용된 체크리스트

- `CHECKLIST_FE.md`
- `CHECKLIST_BE.md`
- `CHECKLIST_INT.md`
- `CHECKLIST_MODE.md`

### B. 참조 문서

- `docs/architecture/context/domain/mode-system.md`
- `docs/architecture/context/domain/satellite-tracking.md`

### C. 분석 로그

```
Phase 1: 준비
  - T001 파일 목록 수집 ✅
  - T002 파일 분류 ✅
  - T003 우선순위 결정 ✅

Phase 2: 스캔 (병렬)
  - T004 BE 코드 스캔 ✅ @be-expert
  - T005 FE 코드 스캔 ✅ @fe-expert

Phase 3: 분석 (병렬)
  - T006 공통 규칙 검사 ✅
  - T007 BE 로직 분석 ✅ @be-expert
  - T008 FE 로직 분석 ✅ @fe-expert

Phase 4: 종합
  - T009 데이터 흐름 분석 ✅
  - T010 REVIEW.md 생성 ✅

Phase 5: 보고
  - T011 요약 보고 (진행중)
  - T012 개선 작업 연계 (대기)
```
