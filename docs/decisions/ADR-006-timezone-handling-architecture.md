---
번호: ADR-006
제목: 시간대(Timezone) 처리 아키텍처 표준화
상태: 제안됨
날짜: 2026-01-20
---

# ADR-006: 시간대(Timezone) 처리 아키텍처 표준화

## 상태

**제안됨** - 검토 및 구현 대기

## 컨텍스트

### 현재 상황

| 영역 | 현황 | 문제점 |
|------|------|--------|
| **DB** | TIMESTAMPTZ (내부 UTC) | 설정 누락 |
| **Backend** | OffsetDateTime 사용 | KST 하드코딩 존재 |
| **Frontend** | times.ts 유틸리티 | locale 하드코딩, 사용자 설정 없음 |
| **알고리즘** | UTC 기반 계산 | 정상 |

### 발견된 문제점

| # | 위치 | 문제 | 심각도 |
|---|------|------|--------|
| 1 | `SunTrackService.kt` | `ZoneId.of("Asia/Seoul")` 하드코딩 | HIGH |
| 2 | `EphemerisService.kt` | CSV 내보내기에 `UTC+9` 하드코딩 | HIGH |
| 3 | `GlobalExceptionHandler.kt` | `LocalDateTime.now()` (timezone 없음) | MEDIUM |
| 4 | `application-with-db.properties` | DB 연결에 timezone 설정 없음 | MEDIUM |
| 5 | `times.ts` | `ko-KR`, `en-US` locale 하드코딩 | LOW |
| 6 | Frontend | 사용자 timezone 선택 기능 없음 | MEDIUM |

### 배경

- ACS는 위성 추적 시스템으로 **시간 정확성이 핵심**
- 폐쇄망 환경에서도 동작해야 함 (인터넷 불필요)
- 설치 위치가 불명확 (국내/해외 다양한 지상국)
- CLAUDE.md 규칙: "내부 UTC, 표시 로컬"

### 제약 조건

- 폐쇄망 환경 지원 필수 (인터넷 없이 동작)
- 기존 코드 최소 변경
- 위성 궤도 계산 정확성 유지 (UTC 필수)
- 전 세계 시간대 지원 필요

## 결정

**IANA Timezone 기반 + 브라우저 자동감지 + 사용자 수동 선택 방식을 채택한다.**

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│  데이터 흐름                                                             │
│                                                                         │
│  [DB/Backend]              [API]                    [Frontend]          │
│                                                                         │
│   UTC 저장/처리    →    ISO 8601 UTC 응답    →    사용자 TZ로 변환       │
│   (OffsetDateTime)      (2026-01-20T07:00:00Z)    (Intl API 사용)       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  사용자 Timezone 설정                                                    │
│                                                                         │
│  1. 자동감지 (기본): Intl.DateTimeFormat().resolvedOptions().timeZone   │
│  2. 수동선택 (옵션): Intl.supportedValuesOf('timeZone') 전체 목록       │
│  3. 저장위치: localStorage (인증 미구현 상태)                            │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 핵심 원칙

| 계층 | 원칙 | 구현 |
|------|------|------|
| **DB** | UTC 저장 | TIMESTAMPTZ, 연결 설정에 timezone=UTC |
| **Backend** | UTC 처리 | OffsetDateTime(UTC), 하드코딩 제거 |
| **API** | UTC 응답 | ISO 8601 형식 `Z` suffix |
| **Frontend** | 로컬 표시 | 사용자 선택 timezone으로 변환 |
| **알고리즘** | UTC 계산 | 변경 없음 (현재 정상) |

## 대안

### 대안 1: UTC Offset 방식 (+9, -5)

| 항목 | 내용 |
|------|------|
| 설명 | 단순 offset 값만 선택 |
| 장점 | 구현 간단, 선택지 적음 (~25개) |
| 단점 | DST 자동 처리 불가, 사용자 수동 변경 필요 |
| 선택 | ❌ 기각 - 정확성 부족 |

### 대안 2: IANA Timezone + 자동감지 (권장)

| 항목 | 내용 |
|------|------|
| 설명 | IANA 표준 시간대 + 브라우저 자동감지 + 수동 선택 |
| 장점 | DST 자동 처리, 전체 시간대 지원, 인터넷 불필요 |
| 단점 | 선택지 많음 (~400개) → 검색 UI 필요 |
| 선택 | ✅ 채택 |

### 대안 3: 서버에서 변환

| 항목 | 내용 |
|------|------|
| 설명 | API 응답 시 서버에서 사용자 timezone으로 변환 |
| 장점 | Frontend 로직 단순화 |
| 단점 | 인증 필요, 서버 부하 증가, 캐싱 복잡 |
| 선택 | ❌ 기각 - 현재 인증 미구현 |

## 구현 범위

### Phase 1: Backend 하드코딩 제거 (필수)

| 파일 | 변경 내용 |
|------|----------|
| `SunTrackService.kt` | `"Asia/Seoul"` → `GlobalData.Time.clientTimeZone` |
| `EphemerisService.kt` | CSV 내보내기 timezone 설정 기반으로 변경 |
| `GlobalExceptionHandler.kt` | `LocalDateTime.now()` → `OffsetDateTime.now(ZoneOffset.UTC)` |
| `application-with-db.properties` | `?timezone=UTC` 추가 |

### Phase 2: Frontend 인프라 구축 (필수)

| 파일 | 역할 |
|------|------|
| `stores/common/timezoneStore.ts` | Pinia store (신규) |
| `utils/times.ts` | timezone 인자 추가 (수정) |
| `composables/useTimezone.ts` | 컴포넌트 헬퍼 (신규) |

### Phase 3: 설정 UI (필수)

| 컴포넌트 | 기능 |
|----------|------|
| `TimezoneSelector.vue` | 검색형 timezone 선택 |
| 설정 페이지 통합 | 기존 설정에 추가 |

### Phase 4: 기존 컴포넌트 마이그레이션 (필수)

| 대상 | 변경 |
|------|------|
| `ScheduleTable.vue` | `formatToLocalTime()` → timezone store 연동 |
| `SatelliteTrackingInfo.vue` | 동일 |
| `HardwareErrorLogPanel.vue` | `toLocaleString()` → 통합 유틸 사용 |
| 기타 시간 표시 컴포넌트 | 전수 조사 후 마이그레이션 |

## 영향 범위 분석

### Backend 영향

| 모듈 | 영향도 | 변경 필요 |
|------|--------|----------|
| 위성 추적 (Orekit) | 없음 | UTC 유지 |
| 태양 추적 | 중간 | 하드코딩 제거 |
| ICD 통신 | 없음 | UTC 유지 |
| API 응답 | 없음 | 이미 ISO 8601 |
| 에러 핸들링 | 낮음 | timestamp 수정 |
| DB 연결 | 낮음 | 설정 추가 |

### Frontend 영향

| 모듈 | 영향도 | 변경 필요 |
|------|--------|----------|
| 시간 유틸리티 | 높음 | timezone 인자 추가 |
| 설정 페이지 | 중간 | UI 추가 |
| 패스 스케줄 | 중간 | 표시 변환 |
| 하드웨어 모니터링 | 중간 | 표시 변환 |
| 차트/그래프 | 낮음 | 축 레이블만 |

### 알고리즘 영향

| 모듈 | 영향도 | 변경 필요 |
|------|--------|----------|
| 궤도 전파 | 없음 | UTC 기반 유지 |
| 패스 예측 | 없음 | UTC 기반 유지 |
| 태양 위치 계산 | 없음 | UTC 기반 유지 |
| 좌표 변환 | 없음 | UTC 기반 유지 |

## 기술 선택

### Frontend 라이브러리

| 옵션 | 번들 크기 | 결정 |
|------|----------|------|
| date-fns-tz | ~2KB | ✅ 채택 |
| dayjs + timezone | ~7KB | 대안 |
| luxon | ~70KB | 기각 |
| moment-timezone | ~200KB | 기각 |

**선택 이유**: 경량, Intl API 기반, 트리쉐이킹 최적

### Timezone 목록

| 방식 | 결정 |
|------|------|
| 하드코딩 목록 | 기각 |
| `Intl.supportedValuesOf('timeZone')` | ✅ 채택 |

**선택 이유**: 브라우저 내장, 자동 전체 목록, 인터넷 불필요

## 결과

### 긍정적 영향

- 전 세계 어디서나 정확한 시간 표시
- DST 자동 처리
- 폐쇄망 완벽 지원
- 사용자 경험 향상 (자동감지 + 수동 선택)
- CLAUDE.md 규칙 완전 준수

### 부정적 영향

- 마이그레이션 작업 필요
- 테스트 범위 증가

### 리스크

| 리스크 | 확률 | 대응 |
|--------|------|------|
| 기존 시간 표시 누락 | 중간 | 전수 조사 체크리스트 |
| 알고리즘 영향 | 낮음 | UTC 유지로 영향 없음 |
| 브라우저 호환성 | 낮음 | Intl API는 IE 제외 전체 지원 |

## 테스트 계획

| 테스트 | 범위 |
|--------|------|
| 단위 테스트 | timezone 변환 유틸 함수 |
| 통합 테스트 | API → FE 시간 표시 |
| E2E 테스트 | timezone 변경 시 전체 UI 반영 |
| 수동 테스트 | 다양한 timezone 설정 후 확인 |

## 관련 문서

- [CLAUDE.md](../../CLAUDE.md) - "내부 UTC, 표시 로컬" 규칙
- [times.ts](../../frontend/src/utils/times.ts) - 현재 시간 유틸리티
- [GlobalData.kt](../../backend/src/main/kotlin/com/gtlsystems/acs_api/data/GlobalData.kt) - 서버 시간 설정

## 참고 자료

- [IANA Time Zone Database](https://www.iana.org/time-zones)
- [Intl.supportedValuesOf()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/supportedValuesOf)
- [date-fns-tz](https://github.com/marnusw/date-fns-tz)

---

## 변경 이력

| 날짜 | 상태 | 변경 내용 |
|------|------|----------|
| 2026-01-20 | 제안됨 | 초기 작성 |