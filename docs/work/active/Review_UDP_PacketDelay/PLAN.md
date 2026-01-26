# UDP 패킷 지연 개선 계획

<!-- @task-system: enabled -->
<!-- @auto-sync: true -->

**Origin:** #R001
**생성일:** 2026-01-26
**목표:** 패킷 지연 90~150ms → 30~50ms 개선

---

## 1. 개요

| 항목 | 내용 |
|------|------|
| 문제 | UDP 패킷 수신 간격 90~150ms (예상: 30ms) |
| 원인 | 동시성 + Windows 타이머 + GC 압박 복합 |
| 목표 | 정상 범위 30~50ms로 개선 |
| 복잡도 | 중간 (코드 수정) ~ 높음 (아키텍처 개선) |

---

## 2. Phase 분류

### Phase A: 즉시 수정 (Critical) - 1일

| Task ID | 이슈 | 작업 | 에이전트 |
|---------|------|------|----------|
| #T001 | #R001-C1 | receiveBuffer ThreadLocal화 | @be-expert |

**예상 효과:** 데이터 손상 방지, 10~20ms 지연 감소

### Phase B: 단기 수정 (High) - 3일

| Task ID | 이슈 | 작업 | 에이전트 |
|---------|------|------|----------|
| #T002 | #R001-H3 | Windows 타이머 1ms 설정 | @be-expert |
| #T003 | #R001-H1,H2 | Send/Receive executor 분리 | @be-expert |
| #T004 | #R001-H4 | PushData.ReadData 객체 풀링 | @be-expert |

**예상 효과:** 40~60ms 지연 감소 (목표 달성 가능)

### Phase C: 중기 개선 (Medium) - 1주

| Task ID | 이슈 | 작업 | 에이전트 |
|---------|------|------|----------|
| #T005 | #R001-M1 | lastPacketTime AtomicLong화 | @be-expert |
| #T006 | #R001-M2 | DataStoreService 경량화 | @be-expert |
| #T007 | #R001-M3 | Selector 패턴 검토/적용 | @architect |

**예상 효과:** 추가 10ms 개선, 안정성 향상

### Phase D: 장기 리팩토링 (Low) - 2주+

| Task ID | 이슈 | 작업 | 에이전트 |
|---------|------|------|----------|
| #T008 | #R001-L1 | UdpFwICDService 계층 분리 | @refactorer |
| #T009 | #R001-L2 | Handler Registry 패턴 적용 | @refactorer |
| #T010 | - | Reactor Netty 전환 검토 | @architect |

**예상 효과:** 유지보수성/확장성 향상

---

## 3. 작업 순서 (의존성)

```
Phase A (즉시)
    └── #T001 receiveBuffer ──┐
                              │
Phase B (단기)                ▼
    ├── #T002 Windows 타이머 ──┬── [중간 검증] ──┐
    ├── #T003 executor 분리 ───┤               │
    └── #T004 객체 풀링 ───────┘               │
                                              ▼
Phase C (중기)                          [목표 달성?]
    ├── #T005 AtomicLong ──────────────────┬── Yes: Phase D 선택
    ├── #T006 DataStore ───────────────────┤
    └── #T007 Selector ────────────────────┘── No: 추가 분석

Phase D (장기) - 선택적
    ├── #T008 계층 분리
    ├── #T009 Handler 패턴
    └── #T010 Netty 검토
```

---

## 4. 진행 체크리스트

### Phase A [parallel: false]
- [ ] #T001 receiveBuffer ThreadLocal화 @be-expert

### Phase B [parallel: true]
- [ ] #T002 Windows 타이머 1ms [depends: T001] @be-expert
- [ ] #T003 executor 분리 [depends: T001] @be-expert
- [ ] #T004 객체 풀링 [depends: T001] @be-expert

### 중간 검증 [parallel: false]
- [ ] #T-V1 패킷 지연 측정 (목표: 50ms 이하) [depends: T002,T003,T004]

### Phase C [parallel: true]
- [ ] #T005 lastPacketTime AtomicLong [depends: T-V1] @be-expert
- [ ] #T006 DataStoreService 경량화 [depends: T-V1] @be-expert
- [ ] #T007 Selector 패턴 적용 [depends: T-V1] @architect

### Phase D [parallel: false] - 선택
- [ ] #T008 UdpFwICDService 분리 [depends: T007] @refactorer
- [ ] #T009 Handler Registry [depends: T008] @refactorer
- [ ] #T010 Netty 검토 ADR [depends: T009] @architect

---

## 5. 예상 일정

| Phase | 작업 | 예상 |
|:-----:|------|------|
| A | Critical 수정 | 1일 |
| B | High 수정 | 3일 |
| 검증 | 지연 측정 | 0.5일 |
| C | Medium 개선 | 5일 |
| D | 리팩토링 | 10일+ |

**최소 목표 (Phase A+B):** 4일 → 40~60ms 달성
**권장 목표 (Phase A+B+C):** 10일 → 30~50ms 달성

---

## 6. 리스크

| 리스크 | 영향 | 대응 |
|--------|:----:|------|
| Windows 타이머 설정 실패 | 중 | JNA 의존성 추가 또는 수동 설정 안내 |
| 객체 풀링으로 인한 상태 오염 | 높 | 필드 초기화 철저히, 테스트 강화 |
| Selector 전환 시 기존 로직 호환 | 중 | 단계적 전환, Feature Flag 사용 |

---

## 7. 완료 기준

### Phase A+B 완료 기준
- [ ] 패킷 지연 평균 50ms 이하
- [ ] receiveBuffer 데이터 손상 없음
- [ ] 빌드 성공

### 전체 완료 기준
- [ ] 패킷 지연 평균 35ms 이하
- [ ] 지연 경고 로그 90% 감소
- [ ] 문서 업데이트 완료

---

## 8. 연계 스킬

| 순서 | 스킬 | 대상 |
|:----:|------|------|
| 1 | `/bugfix #R001-C1` | Phase A |
| 2 | `/bugfix #R001-H1,H2,H3` | Phase B |
| 3 | `/optimize #R001-H4` | Phase B |
| 4 | 검증 | 지연 측정 |
| 5 | `/bugfix #R001-M1,M2` | Phase C |
| 6 | `/refactor #R001-L1,L2` | Phase D (선택) |

---

**다음 단계:** 계획 승인 후 `/bugfix #R001-C1` 실행
