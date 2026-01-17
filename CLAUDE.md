# ACS (Antenna Control System)

위성/태양 추적 안테나 제어 시스템

## IMPORTANT - 핵심 규칙

| 규칙 | 설명 |
|------|------|
| **각도 단위** | 내부 라디안, 표시 도(°) |
| **시간** | 내부 UTC, 표시 로컬 |
| **Train = Tilt** | 변수명 `train`, UI는 `Tilt` |
| **Orekit** | 초기화 필요 (orekit-data) |

## PROHIBITED - 금지 사항

```
!! 연산자       → ?: 또는 requireNotNull()
catch(Exception) → 구체적 예외 타입
하드코딩 색상    → var(--theme-*)
println()       → logger.debug()
```

## 컨텍스트 로딩 (작업 전 참조)

| 작업 유형 | 필수 참조 문서 |
|----------|---------------|
| BE 수정 | `docs/architecture/context/architecture/backend.md` |
| FE 수정 | `docs/architecture/context/architecture/frontend.md` |
| 위성 추적 | `docs/architecture/context/domain/satellite-tracking.md` |
| ICD 통신 | `docs/architecture/context/domain/icd-protocol.md` |
| 모드 시스템 | `docs/architecture/context/domain/mode-system.md` |
| 진행중 작업 | `docs/work/active/` |

## 효과적인 요청 패턴

**Research → Plan → Implement** 순서로 진행

```
# 좋은 요청 (구체적 + 컨텍스트 지정)
"PassSchedule 기능 추가해줘.
먼저 satellite-tracking.md 읽고,
PassScheduleService.kt 구조 파악 후 계획 세워줘"

# 피해야 할 요청 (모호함)
"기능 추가해줘"
```

## 빌드 명령어

```bash
cd frontend && npm run build              # FE 빌드
cd backend && ./gradlew clean build -x test  # BE 빌드
```

## 기술 스택

| 영역 | 기술 |
|-----|-----|
| Frontend | Vue 3 + Quasar 2.x + TypeScript + Pinia |
| Backend | Kotlin 1.9 + Spring Boot 3.x + WebFlux |
| 알고리즘 | Orekit 13.0 (위성), solarpositioning (태양) |

## 모드 시스템

| 모드 | 설명 |
|-----|-----|
| Standby | 대기 |
| Step / Slew | 이동 |
| EphemerisDesignation | 위성 궤도 지정 |
| PassSchedule | 패스 스케줄 |
| SunTrack | 태양 추적 |

## 상세 문서

| 문서 | 경로 |
|-----|-----|
| 개발 가이드 | `docs/guides/` |
| 팀 핸드북 | `docs/handbook/` |
| 시스템 개요 | `docs/architecture/SYSTEM_OVERVIEW.md` |

---

## 작업 완료 시

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 다음에 쓸 수 있는 명령어
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  워크플로우: /feature /bugfix /refactor /done
  상태 확인: /health /status /sync
  문서화: /plan /adr /docs

  /guide 로 상세 안내 + 대화 패턴 가이드
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
