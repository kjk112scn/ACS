# ACS (Antenna Control System)

위성/태양 추적 안테나 제어 시스템

## 기술 스택

| 영역 | 기술 |
|-----|-----|
| Frontend | Vue 3 + Quasar 2.x + TypeScript 5.x + Pinia |
| Backend | Kotlin 1.9 + Spring Boot 3.x + WebFlux |
| 알고리즘 | Orekit 13.0 (위성), solarpositioning (태양) |

## 빌드 명령어

```bash
# Frontend
cd frontend && npm run dev      # 개발 서버
cd frontend && npm run build    # 빌드

# Backend
cd backend && ./gradlew bootRun              # 실행
cd backend && ./gradlew clean build -x test  # 빌드
```

## 핵심 규칙

| 영역 | 상세 문서 |
|-----|----------|
| Frontend | [docs/guides/frontend-rules.md](docs/guides/frontend-rules.md) |
| Backend | [docs/guides/backend-rules.md](docs/guides/backend-rules.md) |
| 패턴 | [docs/handbook/project/acs-patterns.md](docs/handbook/project/acs-patterns.md) |

## 주의사항 (IMPORTANT)

- **각도 단위**: 내부 라디안, 표시 도(°)
- **시간**: 내부 UTC, 표시 로컬
- **Train/Tilt**: 변수명 `train`, UI 표시 `Tilt`
- **Orekit**: 초기화 필요 (orekit-data 경로)

## 모드 시스템

| 모드 | 설명 |
|-----|-----|
| Standby | 대기 모드 |
| Step | 스텝 이동 |
| Slew | 슬루 이동 |
| EphemerisDesignation | 위성 궤도 지정 |
| PassSchedule | 패스 스케줄 |
| SunTrack | 태양 추적 |

## 문서 위치

| 문서 | 경로 |
|-----|-----|
| 시스템 통합 | `docs/architecture/SYSTEM_OVERVIEW.md` |
| 개발 가이드 | `docs/guides/` |
| 팀 핸드북 | `docs/handbook/` |
| 진행중 작업 | `docs/work/active/` |

## 가드레일 (PROHIBITED)

- 이 파일 100줄 초과 금지
- `docs/architecture/context/` 직접 수정 금지 (doc-syncer만 수정)
- 광범위 catch(Exception) 금지

---

## 작업 완료 시 표시

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 다음에 쓸 수 있는 명령어
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  워크플로우: /feature /bugfix /refactor /done
  상태 확인: /health /status /sync
  문서화: /plan /adr /docs

  /guide 로 상세 안내
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
