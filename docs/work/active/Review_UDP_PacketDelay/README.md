# Review: UDP 패킷 지연 분석

**Review ID:** #R001
**생성일:** 2026-01-26
**상태:** 📋 분석/계획 완료, 실행 대기 (기능 안정화 후)

## 개요

UDP 통신에서 90~150ms 패킷 지연이 발생하는 문제에 대한 심층 분석

## 대상 파일

| 파일 | 역할 |
|------|------|
| `ICDService.kt` | ICD 프로토콜 파싱, 패킷 타이밍 모니터링 |
| `UdpFwICDService.kt` | UDP 송수신, 스케줄링 |
| `ThreadManager.kt` | 스레드 풀 관리 |
| `DataStoreService.kt` | 데이터 저장/조회 |

## 워크플로우

| 단계 | 스킬 | 날짜 | 결과 | 상태 |
|:----:|------|------|------|:----:|
| 1 | /review | 01-26 | #R001 (10개 이슈) | ✅ |
| 2 | /plan | 01-26 | Phase A-D 계획 | ✅ |
| 3 | /bugfix | - | Phase A (Critical) | ⏳ |
| 4 | /bugfix | - | Phase B (High) | ⏳ |
| 5 | 검증 | - | 지연 측정 | ⏳ |
| 6 | /bugfix | - | Phase C (Medium) | ⏳ |

## 이슈 추적

| Origin | 심각도 | 설명 | 상태 |
|--------|:------:|------|:----:|
| #R001-C1 | Critical | receiveBuffer 공유 자원 경쟁 | ⏳ |
| #R001-H1 | High | scheduleAtFixedRate 실행 밀림 | ⏳ |
| #R001-H2 | High | Send/Receive executor 경쟁 | ⏳ |
| #R001-H3 | High | Windows 타이머 해상도 15.625ms | ⏳ |
| #R001-H4 | High | PushData.ReadData GC 압박 | ⏳ |
| #R001-M1 | Medium | lastPacketTime 비원자적 접근 | ⏳ |
| #R001-M2 | Medium | DataStoreService 복합 연산 | ⏳ |
| #R001-M3 | Medium | 논블로킹 폴링 비효율 | ⏳ |
| #R001-L1 | Low | UdpFwICDService 역할 비대화 | ⏳ |
| #R001-L2 | Low | ICDService.Classify if/else 체인 | ⏳ |

## 권장 조치

**Critical (즉시):** `/bugfix #R001-C1`
**High (단기):** `/bugfix #R001-H1,H2,H3` + `/optimize #R001-H4`