# RFC-002: 로깅 시스템 고도화

> **버전**: 1.0.0 | **작성일**: 2026-01-13
> **상태**: Draft | **우선순위**: P0

---

## 1. 배경 (Context)

### 왜 이 변경이 필요한가?

현재 ACS 로깅 시스템은 다음 문제를 가지고 있습니다:

| 문제 | 영향 |
|-----|------|
| 설정 충돌 | application.properties와 logback-spring.xml 간 레벨 불일치 |
| 용량 제한 없음 | totalSizeCap 미설정으로 디스크 풀 위험 |
| 보관 기간 불일치 | error: 90일, 나머지: 30일 (정책 불일치) |
| 맥락 정보 부족 | 어떤 위성, 어떤 세션인지 추적 어려움 |

### 목표

- 로그 용량을 **5GB 이내**로 제한
- 보관 기간 **30일 통일**
- 설정 충돌 해결
- 향후 MDC 도입 계획 수립

---

## 2. 현재 상태 (Current State)

### 2.1 로깅 설정 현황

#### logback-spring.xml (247줄)

| Appender | 파일명 | 용도 | maxHistory |
|----------|--------|------|------------|
| FILE-ALL | acs-application.log | 전체 로그 | 30일 |
| FILE-ERROR | acs-error.log | 에러만 | **90일** (불일치) |
| FILE-PERFORMANCE | acs-performance.log | 성능 | 30일 |
| FILE-BUSINESS | acs-business.log | 비즈니스 | **90일** (불일치) |

**문제점**: totalSizeCap 미설정, maxHistory 불일치

#### application.properties 충돌

```properties
# application.properties
logging.level.com.gtlsystems.acs_api.service=WARN

# logback-spring.xml
<logger name="com.gtlsystems.acs_api.service" level="INFO">
```

→ 어떤 설정이 적용되는지 예측 어려움

### 2.2 로그 호출 현황

| 파일 | 로그 수 | 비고 |
|-----|--------|------|
| EphemerisService.kt | 468 | 이모지 다수 |
| PassScheduleService.kt | 290 | 상태 변경 로그 |
| UdpFwICDService.kt | 111 | 명령 전송 로그 |
| 기타 28개 파일 | 664 | - |
| **합계** | **1,533** | 31개 파일 |

### 2.3 이모지 사용 현황

```kotlin
// 현재 (374건)
logger.info("🚀 정지궤도 위성 추적 시작")
logger.info("✅ 3축 변환 완료")
logger.warn("⚠️ 배치 처리 종료 중")
```

**문제점**: 로그 검색/파싱 어려움, 터미널 호환성

---

## 3. 제안 (Proposal)

### 3.1 즉시 적용 (설정 변경)

#### A. logback-spring.xml 수정

```xml
<!-- 변경 전 -->
<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <maxFileSize>100MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <!-- totalSizeCap 없음 -->
</rollingPolicy>

<!-- 변경 후 -->
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <maxFileSize>100MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>5GB</totalSizeCap>  <!-- 추가 -->
</rollingPolicy>
```

#### B. 보관 기간 통일

| Appender | 현재 | 변경 |
|----------|------|------|
| FILE-ERROR | 90일 | **30일** |
| FILE-BUSINESS | 90일 | **30일** |

#### C. application.properties 정리

```properties
# 제거할 설정 (logback으로 통합)
# logging.level.com.gtlsystems.acs_api=DEBUG
# logging.level.com.gtlsystems.acs_api.service=WARN
# logging.level.org.hibernate.SQL=DEBUG
```

### 3.2 점진적 적용 (코드 수정)

#### A. 이모지 제거 가이드라인

```kotlin
// Before
logger.info("🚀 정지궤도 위성 추적 시작 (3축 변환 적용)")

// After
logger.info("[GEO_TRACK] 정지궤도 위성 추적 시작, 3축 변환 적용")
```

**적용 시점**: 해당 파일 리팩토링 시 순차적 제거

#### B. MDC 도입 계획 (상태머신 리팩토링 시)

```kotlin
// 향후 적용 패턴
MDC.put("sessionId", sessionId)
MDC.put("satelliteId", satelliteId)
MDC.put("mode", currentMode)
try {
    // 비즈니스 로직
    logger.info("추적 시작")  // 자동으로 맥락 정보 포함
} finally {
    MDC.clear()
}
```

**WebFlux 환경 대응 (필수)**:

ACS는 WebFlux(리액티브) 기반이므로 MDC 도입 시 추가 설정이 필요합니다:

```kotlin
// Application.kt 또는 설정 클래스에 추가
@PostConstruct
fun setupReactorContext() {
    Hooks.enableAutomaticContextPropagation()
}
```

**리팩토링 시 같이 적용하는 이유**:
- 리팩토링 자체가 WebFlux 문제를 해결하는 것은 아님
- 코드 구조가 명확해진 후 **진입점 식별이 쉬워짐**
- WebFlux 맥락 전파 설정과 MDC를 **동시에 올바르게 적용** 가능

---

## 4. 대안 (Alternatives)

### 4.1 구조화 로깅 (JSON) - 미채택

```json
{"timestamp":"2026-01-13T10:00:00","level":"INFO","satelliteId":"ISS","message":"추적 시작"}
```

**미채택 사유**:
- ELK 스택 미사용 결정
- 사람이 읽기 어려움
- 현재 텍스트 로그로 충분

### 4.2 MDC 즉시 도입 - 미채택

**미채택 사유**:
- WebFlux 환경에서 ThreadLocal 기반 MDC 호환성 문제
- 현재 코드 구조(5,000줄+ 파일)에서 진입점 식별 어려움
- 테스트 커버리지 1.5%로 누락 검증 불가
- 상태머신 리팩토링 시 같이 적용하는 것이 안전

---

## 5. 영향 분석 (Impact)

### 5.1 변경 파일

| 파일 | 변경 내용 |
|-----|----------|
| logback-spring.xml | totalSizeCap, maxHistory 수정 |
| application.properties | 로깅 설정 제거 |

### 5.2 예상 효과

| 지표 | 현재 | 변경 후 |
|-----|------|--------|
| 최대 로그 용량 | 무제한 | 5GB |
| 보관 기간 | 30~90일 혼재 | 30일 통일 |
| 설정 충돌 | 있음 | 해결 |

### 5.3 위험 요소

| 위험 | 대응 |
|-----|------|
| 로그 용량 부족 | 운영 후 모니터링, 필요 시 조정 |
| 중요 로그 소실 | ERROR 로그는 DB 저장 검토 (추후) |

---

## 6. 마이그레이션 (Migration)

### 6.1 단계별 적용

```
Phase 1: 설정 변경 (즉시)
├── logback-spring.xml 수정
├── application.properties 정리
└── 빌드 및 테스트

Phase 2: 모니터링 (1주)
├── 로그 용량 추적
├── 누락 로그 확인
└── 레벨 조정 필요 시 반영

Phase 3: 이모지 제거 (점진적)
├── 리팩토링 시 해당 파일 수정
└── 코드 리뷰에서 확인

Phase 4: MDC 도입 (상태머신 리팩토링 시)
├── WebFlux 맥락 전파 설정
├── 주요 서비스 진입점에 MDC 적용
└── 로그 패턴에 MDC 필드 추가
```

### 6.2 롤백 계획

설정 변경만이므로 git revert로 즉시 롤백 가능

---

## 7. 검증 (Verification)

### 7.1 체크리스트

- [ ] logback-spring.xml totalSizeCap 5GB 설정
- [ ] 모든 appender maxHistory 30일 통일
- [ ] application.properties 로깅 설정 제거
- [ ] 빌드 성공 확인
- [ ] 로그 파일 정상 생성 확인
- [ ] 1주 후 용량 모니터링

### 7.2 성공 기준

| 기준 | 측정 방법 |
|-----|----------|
| 로그 용량 5GB 이하 | `du -sh logs/` |
| 30일 이상 로그 자동 삭제 | 파일 목록 확인 |
| 설정 충돌 없음 | 로그 레벨 일관성 확인 |

---

## 8. 결정 사항 요약

| 항목 | 결정 | 비고 |
|-----|------|------|
| 설정 통합 | logback만 사용 | application.properties 정리 |
| 용량 제한 | 5GB | totalSizeCap 설정 |
| 보관 기간 | 30일 통일 | 다른 데이터와 일치 |
| 이모지 | 점진적 제거 | 리팩토링 시 적용 |
| 구조화 로깅 | 텍스트 유지 | ELK 미사용 |
| MDC | 상태머신 리팩토링 시 | WebFlux 호환성 문제 |

---

**작성자**: Claude
**검토자**: -
**승인일**: -
