# UDP 패킷 지연 분석 리뷰 결과

**Review ID:** #R001
**분석일:** 2026-01-26
**대상:** ICDService.kt, UdpFwICDService.kt, DataStoreService.kt

---

## 1. 문제 요약

| 항목 | 값 |
|------|-----|
| 현상 | 패킷 지연 경고 (90~150ms) |
| 예상 | 30ms 간격 수신 |
| 근본 원인 | 동시성 + 성능 + 아키텍처 복합 문제 |

---

## 2. 발견된 이슈

| Issue ID | 심각도 | 문제 | 위치 | 연계 |
|----------|:------:|------|------|------|
| #R001-C1 | Critical | receiveBuffer 공유 자원 경쟁 | UdpFwICDService.kt:46 | /bugfix |
| #R001-H1 | High | scheduleAtFixedRate 실행 밀림 | UdpFwICDService.kt:138-160 | /bugfix |
| #R001-H2 | High | Send/Receive executor 경쟁 | ThreadManager | /bugfix |
| #R001-H3 | High | Windows 타이머 해상도 15.625ms | OS 레벨 | /bugfix |
| #R001-H4 | High | PushData.ReadData GC 압박 | ICDService.kt:51 | /optimize |
| #R001-M1 | Medium | lastPacketTime 비원자적 접근 | ICDService.kt:26 | /bugfix |
| #R001-M2 | Medium | DataStoreService 복합 연산 | DataStoreService.kt:32 | /bugfix |
| #R001-M3 | Medium | 논블로킹 폴링 비효율 | UdpFwICDService.kt:205 | /refactor |
| #R001-L1 | Low | UdpFwICDService 역할 비대화 | 1,233줄 | /refactor |
| #R001-L2 | Low | ICDService.Classify if/else 체인 | 17개 분기 | /refactor |

---

## 3. 전문가 분석 요약

### 3.1 동시성 분석 (debugger)

**핵심 발견:**
- `receiveBuffer`가 클래스 인스턴스 필드로 단일 선언되어 스레드 간 경쟁 발생
- `scheduleAtFixedRate`는 이전 실행이 완료되지 않으면 다음 실행이 밀림
- Send/Receive가 동일 executor의 2개 스레드를 경쟁

**지연 계산:**
```
정상: Send(30ms) -> FW응답 -> Receive = ~40ms
실제: Send대기(+20ms) + Send(+5ms) + FW(+10ms) + Receive대기(+30ms) + 처리(+40ms) = 90~115ms
```

### 3.2 성능 분석 (performance-analyzer)

**핵심 발견:**
- Windows 기본 타이머 해상도 15.625ms → 10ms 스케줄 불가
- PushData.ReadData(50+ 필드) 매 패킷마다 생성 → GC 압박
- DataStoreService.updateDataFromUdp() 병합 로직 1-2ms 소요

**예상 개선 효과:**
| 개선 항목 | 현재 | 목표 | 개선율 |
|----------|------|------|:------:|
| 패킷 수신 간격 | 90-150ms | 30-35ms | **70%** |

### 3.3 아키텍처 분석 (architect)

**핵심 발견:**
- UdpFwICDService 1,233줄에 송신/수신/명령/Stow 모두 담당 (단일 책임 위반)
- ICDService.Classify 17개 if/else 분기 (OCP 위반)
- 폴링 방식으로 CPU 낭비 + 지연 발생

**권장 아키텍처:**
- 단기: Handler Registry 패턴 적용
- 중기: Transport/Protocol/Application 계층 분리
- 장기: Reactor Netty 전환 검토

---

## 4. 우선순위별 권장 조치

### 즉시 (Critical)

| # | 조치 | 예상 효과 |
|:-:|------|----------|
| 1 | receiveBuffer ThreadLocal화 | 데이터 손상 방지 |

```kotlin
// 변경 전
private val receiveBuffer = ByteBuffer.allocate(512)

// 변경 후
private val receiveBufferLocal = ThreadLocal.withInitial {
    ByteBuffer.allocateDirect(512)
}
```

### 단기 (High)

| # | 조치 | 예상 효과 |
|:-:|------|----------|
| 2 | Windows 타이머 해상도 1ms 설정 | 20-30ms 지연 감소 |
| 3 | Send/Receive 실행기 분리 | 스레드 경쟁 제거 |
| 4 | 객체 풀링 도입 | GC 압박 50% 감소 |

```kotlin
// Windows 타이머 해상도 조정
@PostConstruct
fun initializeTimerResolution() {
    if (System.getProperty("os.name").contains("Windows")) {
        com.sun.jna.platform.win32.Kernel32.INSTANCE.timeBeginPeriod(1)
    }
}
```

### 중기 (Medium)

| # | 조치 | 예상 효과 |
|:-:|------|----------|
| 5 | lastPacketTime AtomicLong화 | 측정 정확도 향상 |
| 6 | DataStoreService 경량화 | 0.3ms 감소 |
| 7 | Selector 패턴 적용 | 10-20ms 지연 감소 |

### 장기 (Low - 리팩토링)

| # | 조치 | 예상 효과 |
|:-:|------|----------|
| 8 | Handler Registry 패턴 | 확장성/테스트 용이성 |
| 9 | 서비스 계층 분리 | 유지보수성 향상 |

---

## 5. 예상 결과

**현재**: 90~150ms 패킷 지연
**목표**: 30~50ms 패킷 지연 (정상 범위)

| 단계 | 적용 후 예상 |
|------|------------|
| Critical 수정 | 80~130ms |
| High 수정 | 40~60ms |
| Medium 수정 | 30~50ms |

---

## 6. 연계 작업

| 다음 스킬 | 대상 | 조건 |
|----------|------|------|
| `/bugfix #R001-C1` | receiveBuffer | 즉시 수정 필요 |
| `/bugfix #R001-H1,H2,H3` | 스케줄러/타이머 | 단기 |
| `/optimize #R001-H4` | GC 최적화 | 단기 |
| `/refactor #R001-L1,L2` | 구조 개선 | 장기 |

---

**분석 완료:** 2026-01-26
**전문가:** debugger, performance-analyzer, architect
