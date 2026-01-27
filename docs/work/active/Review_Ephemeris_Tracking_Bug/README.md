# Review: Ephemeris 추적 버그 (#R001)

> **상태:** 분석 완료 - BE 로그 확인 대기
> **생성일:** 2026-01-27

## 현재 상황

Ephemeris 모드에서 위성 스케줄 추가 후 추적 시작 시:
- "이상한 각도로 이동하더니 추적하지 않음" 현상 발생
- Dashboard에서 모든 CMD 값이 0으로 표시

## 워크플로우

| 단계 | 스킬 | 날짜 | 결과 | 상태 |
|:----:|------|------|------|:----:|
| 1 | /review | 01-27 | #R001 | ✅ |
| 2 | /bugfix | - | - | ⏳ |

## 이슈 추적

| ID | 심각도 | 설명 | 상태 |
|----|:------:|------|:----:|
| #R001-C1 | Critical | createRealtimeTrackingData 빈 Map 반환 | ⏳ |
| #R001-H1 | High | ephemerisStatus와 CMD 값 설정 타이밍 불일치 | ⏳ |
| #R001-M1 | Medium | Dashboard fallback 없음 | ⏳ |
| #R001-L1 | Low | SunTrack 정지 명령 확인 필요 | ⏳ |

## 다음 단계

**BE 로그 확인 필요:**
1. 추적 실패 시점의 BE 로그 공유
2. `createRealtimeTrackingData` 관련 경고/에러 확인
3. mstId/detailId 일치 여부 확인

## 관련 파일

- [REVIEW.md](./REVIEW.md) - 상세 분석 결과
