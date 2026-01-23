---
번호: ADR-007
제목: Tracking Session 키 설계 (mst_id + detail_id vs session_id only)
상태: 승인됨
날짜: 2026-01-23
관련: Tracking_Schema_Redesign (V006)
---

# ADR-007: Tracking Session 키 설계

## 상태

**승인됨** - 2026-01-23

## 컨텍스트

### 배경

tracking_session 테이블 설계 시 데이터 식별 방식 결정이 필요했다:

1. **원래 계획**: `session_id` (auto-increment PK) 단독 사용
2. **현재 구현**: `mst_id` (위성 그룹) + `detail_id` (Pass 구분자) + `session_id` 조합

### 핵심 질문

> "모든 쿼리에서 mst_id/detail_id 기준이 필요한 것이 비효율적인가?
> session_id만으로 단순화하는 것이 나은가?"

### 시스템 요구사항

| 요구사항 | 설명 |
|---------|------|
| **위성별 그룹화** | 같은 위성/TLE 데이터는 함께 조회 가능해야 함 |
| **Pass 순서 관리** | 동일 위성의 여러 Pass를 순서대로 관리 |
| **모드별 분리** | Ephemeris vs PassSchedule 구분 |
| **실시간 추적** | 30ms 주기 데이터 저장 (tracking_result) |
| **분석 지원** | 이론치(trajectory) + 실측치(result) JOIN 가능 |

### 제약 조건

- TimescaleDB Hypertable 사용 (tracking_trajectory, tracking_result)
- 기존 코드 17+ 파일에서 mst_id/detail_id 사용 중
- FE/WebSocket에서 mstId + detailId로 세션 식별

## 결정

**mst_id + detail_id + session_id 복합 구조 유지**

```
tracking_session
├── id (PK, auto)           ← DB 내부 FK용
├── mst_id                  ← 위성별 그룹 ID
├── detail_id               ← Pass 순번 (0, 1, 2...)
├── tracking_mode           ← 'EPHEMERIS' | 'PASS_SCHEDULE'
└── UNIQUE(mst_id, detail_id, tracking_mode)
```

### 역할 분리

| 레이어 | 사용 키 | 용도 |
|--------|---------|------|
| **FE/WebSocket** | mst_id + detail_id | 세션 식별, UI 표시 |
| **DB FK** | session_id | 테이블 간 JOIN |
| **비즈니스 로직** | mst_id + detail_id | 그룹화, 순서 관리 |

## 대안

### 대안 A: 현재 구조 유지 (mst_id + detail_id + session_id)

| 항목 | 내용 |
|------|------|
| 설명 | 복합 키 구조, 역할별 분리 |
| 장점 | 위성별 그룹화 자연스러움, Pass 순서 명확, 코드 변경 없음 |
| 단점 | 쿼리 시 조건 2개 필요, 초기 학습 비용 |
| 선택 | **✅ 선택됨** |

### 대안 B: session_id 단독 사용

| 항목 | 내용 |
|------|------|
| 설명 | PK 하나로 모든 조회 |
| 장점 | 쿼리 단순화, 직관적 |
| 단점 | 위성별 그룹화 불가, Pass 순서 추론 어려움, 17+ 파일 수정 필요 |
| 선택 | ❌ 거부됨 |

### 대안 C: satellite_id + created_at 조합

| 항목 | 내용 |
|------|------|
| 설명 | 위성 ID + 생성 시간으로 식별 |
| 장점 | 자연스러운 시간 기반 정렬 |
| 단점 | 동일 시점 중복 가능, 명시적 순서 없음 |
| 선택 | ❌ 보류 (향후 검토 가능) |

## 평가 점수

### 평가 기준 (5점 만점)

| 기준 | 대안 A (현재) | 대안 B (session_id only) | 대안 C |
|------|--------------|-------------------------|--------|
| 사용자 요구 충족 | 5 | 2 | 3 |
| 쿼리 복잡도 | 4 | 5 | 3 |
| 유지보수성 | 5 | 2 | 3 |
| 확장성 | 5 | 3 | 3 |
| 마이그레이션 비용 | 4 | 1 | 4 |
| **총점** | **23/25** | **13/25** | **16/25** |

### 대안 B 거부 사유

1. **그룹화 기능 상실**: 같은 위성의 여러 Pass를 묶어서 조회 불가
2. **Pass 순서 추론 어려움**: 별도 쿼리로 정렬해야 함
3. **대규모 리팩토링 필요**: 17+ 파일 수정
   - BE: EphemerisService, PassScheduleService, Repository 등
   - FE: EphemerisDesignationPage, PassSchedulePage, stores
   - WebSocket: 세션 식별 로직 전면 수정

## 결과

### 긍정적 영향

- **사용자 요구 충족**: 위성별 그룹화, Pass 순서 관리 자연스럽게 지원
- **코드 안정성**: 검증된 현재 구조 유지, 신규 버그 위험 최소화
- **유연한 조회**: mst_id만으로 위성 전체 조회, (mst_id, detail_id)로 특정 Pass 조회
- **TimescaleDB 호환**: session_id FK로 Hypertable JOIN 최적

### 부정적 영향

- **쿼리 조건 증가**: 단순 조회에도 mst_id/detail_id 조건 필요
- **초기 학습 비용**: 신규 개발자가 구조 이해 필요

### 리스크

- **mst_id 의미 혼란**: V006 정책 명확히 문서화 필요
  - mst_id = 위성 그룹 ID (같은 위성 = 같은 mst_id)
  - detail_id = Pass 순번 (0, 1, 2...)

## 데이터 구조 예시

### PassSchedule: 위성 2개, 각 3개 Pass

```
                위성 A (mst_id=1)          위성 B (mst_id=2)
                      │                          │
          ┌──────────┼──────────┐    ┌──────────┼──────────┐
          ▼          ▼          ▼    ▼          ▼          ▼
       Pass 0     Pass 1     Pass 2  Pass 0   Pass 1    Pass 2
      (d_id=0)   (d_id=1)   (d_id=2) (d_id=0) (d_id=1)  (d_id=2)
```

### tracking_session 테이블

| id | mst_id | detail_id | satellite_id | tracking_mode |
|----|--------|-----------|--------------|---------------|
| 1 | 1 | 0 | 25544 | PASS_SCHEDULE |
| 2 | 1 | 1 | 25544 | PASS_SCHEDULE |
| 3 | 1 | 2 | 25544 | PASS_SCHEDULE |
| 4 | 2 | 0 | 43013 | PASS_SCHEDULE |
| 5 | 2 | 1 | 43013 | PASS_SCHEDULE |
| 6 | 2 | 2 | 43013 | PASS_SCHEDULE |

### 주요 쿼리 패턴

```sql
-- 위성 A의 모든 Pass 조회
SELECT * FROM tracking_session WHERE mst_id = 1;

-- 위성 A의 두 번째 Pass 조회
SELECT * FROM tracking_session WHERE mst_id = 1 AND detail_id = 1;

-- 특정 세션의 이론치 + 실측치 JOIN
SELECT r.*, t.*
FROM tracking_result r
JOIN tracking_trajectory t ON r.session_id = t.session_id
WHERE r.session_id = :sessionId;
```

## 관련 문서

- [Tracking Schema Redesign DESIGN.md](../work/active/Tracking_Schema_Redesign/DESIGN.md)
- [Tracking Schema Redesign PROGRESS.md](../work/active/Tracking_Schema_Redesign/PROGRESS.md)
- [모드 시스템](../architecture/context/domain/mode-system.md)
- [데이터 흐름](../architecture/context/architecture/data-flow.md)

## 참고 자료

- V006 마이그레이션: `V006__Tracking_schema_redesign.sql`
- TimescaleDB 모범 사례: Narrow Table + Hypertable

---

## 변경 이력

| 날짜 | 상태 | 변경 내용 |
|------|------|----------|
| 2026-01-23 | 승인됨 | 초기 작성 - 전문가 검토 완료 |

---

**작성자**: Claude (architect, fullstack-helper 에이전트 분석 기반)
**최종 수정**: 2026-01-23