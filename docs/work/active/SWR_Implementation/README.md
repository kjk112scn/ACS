# SWR (Stale-While-Revalidate) 패턴 적용

> **상태**: 조사 완료 - 구현 대기
> **작성일**: 2026-01-27
> **예상 효과**: 네트워크 트래픽 40~60% 감소

## 개요

프론트엔드와 백엔드에 SWR 캐싱 패턴을 적용하여 성능 최적화 및 사용자 경험 개선

## 현재 상태 분석

| 항목 | 현황 | 문제점 |
|------|------|--------|
| HTTP 캐시 헤더 | ❌ 미구현 | 매 요청마다 전체 응답 전송 |
| 클라이언트 캐싱 | ❌ Pinia 메모리만 | 페이지 새로고침 시 데이터 손실 |
| 중복 요청 제거 | ❌ 미구현 | 동일 API 다중 호출 |
| 조건부 요청 | ❌ 미구현 | ETag/Last-Modified 미사용 |

## 구현 범위

### Phase 1: HTTP 캐시 헤더 (Backend)
- **난도**: 🟢 Low
- **파일 수**: 8개 Controller
- **효과**: 브라우저 캐싱 활용, 즉시 효과

### Phase 2: SWRv 라이브러리 (Frontend)
- **난도**: 🟡 Medium
- **파일 수**: 7개 Service/Composable
- **효과**: 중복 요청 제거, 백그라운드 갱신

### Phase 3: Spring @Cacheable (Backend)
- **난도**: 🟡 Medium
- **파일 수**: 3개 Service
- **효과**: 서버 부하 감소

### Phase 4: 캐시 무효화 전략
- **난도**: 🟠 High
- **파일 수**: 5개
- **효과**: 데이터 일관성 보장

## 변경 대상 파일 목록

### Backend (11개)
```
backend/src/main/kotlin/.../controller/
├── SettingsController.kt      # Cache-Control 헤더 추가
├── PassScheduleController.kt  # ETag 지원
├── EphemerisController.kt     # 조건부 응답
├── ICDController.kt           # 정적 데이터만
├── LoggingController.kt       # 짧은 TTL
├── PerformanceController.kt   # 짧은 TTL
├── HardwareErrorLogController.kt
└── SunTrackController.kt

backend/src/main/kotlin/.../config/
├── CacheConfig.kt             # 신규 생성
└── WebFluxConfig.kt           # 캐시 필터 추가
```

### Frontend (15개)
```
frontend/src/
├── boot/
│   └── axios.ts               # Interceptor 개선
├── services/
│   ├── api/
│   │   ├── settingsService.ts # SWRv 적용
│   │   └── icdService.ts      # 부분 적용
│   └── mode/
│       ├── ephemerisTrackService.ts
│       └── passScheduleService.ts
├── composables/               # 신규 폴더
│   ├── useCache.ts            # 캐시 유틸
│   ├── useSettings.ts         # Settings SWR
│   ├── useTle.ts              # TLE SWR
│   └── useTrackingData.ts     # 추적 데이터 SWR
├── stores/
│   └── settingsStore.ts       # SWR 연동
└── package.json               # swrv 추가
```

## 예상 효과

| 지표 | 현재 | 적용 후 | 개선율 |
|------|------|---------|--------|
| Settings 초기 로드 | 11회 요청 | 1회 요청 | 91%↓ |
| 네트워크 트래픽 | 100% | 40-60% | 40-60%↓ |
| 페이지 전환 속도 | ~500ms | ~50ms | 90%↓ |
| 서버 부하 | 100% | 60-70% | 30-40%↓ |

## 위험 요소

| 위험 | 영향 | 완화 방안 |
|------|------|----------|
| 캐시 불일치 | 설정 변경 미반영 | Event 기반 무효화 |
| 메모리 증가 | 클라이언트 메모리 | LRU 캐시 + TTL |
| 복잡도 증가 | 유지보수 어려움 | Composable 패턴화 |

## 상세 문서

- [DESIGN.md](./DESIGN.md) - 상세 구현 가이드 및 코드 예시

## 참고 자료

- [SWRv 공식 문서](https://docs-swrv.netlify.app/)
- [Kong/swrv GitHub](https://github.com/Kong/swrv)
- [Spring WebFlux HTTP Caching](https://docs.spring.io/spring-framework/reference/web/webflux/caching.html)
- [Baeldung - Spring WebFlux @Cacheable](https://www.baeldung.com/spring-webflux-cacheable)
