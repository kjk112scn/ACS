# ACS (Antenna Control System)

위성/태양 추적 안테나 제어 시스템

## 기술 스택

| 영역 | 기술 |
|-----|-----|
| Frontend | Vue 3 + Quasar 2.x + TypeScript 5.x + Pinia |
| Backend | Kotlin 1.9 + Spring Boot 3.x + WebFlux (리액티브) |
| 알고리즘 | Orekit 13.0 (위성), solarpositioning (태양) |
| 통신 | REST API, WebSocket, UDP (ICD) |

## 빌드 명령어

```bash
# Frontend
cd frontend && npm run dev      # 개발 서버
cd frontend && npm run build    # 빌드

# Backend
cd backend && ./gradlew bootRun              # 실행
cd backend && ./gradlew clean build -x test  # 빌드
```

## 프로젝트 구조

```
frontend/src/
├── components/     # Vue 컴포넌트
├── pages/          # 페이지 (mode/ 하위에 모드별 페이지)
├── stores/         # Pinia 스토어
├── services/       # API 서비스
├── composables/    # Vue Composition 함수
└── types/          # TypeScript 타입

backend/src/main/kotlin/.../
├── controller/     # REST API 엔드포인트
├── service/        # 비즈니스 로직
├── algorithm/      # 계산 알고리즘 (위성/태양 추적)
├── dto/            # 데이터 전송 객체
└── model/          # 도메인 모델
```

## 핵심 코딩 규칙

### Frontend
- `<script setup lang="ts">` 필수
- 색상은 테마 변수 사용: `var(--theme-*)`, 하드코딩 금지
- Composables 활용: useErrorHandler, useNotification, useLoading
- Pinia 스토어: Setup Store 패턴

### Backend
- Kotlin idiom 준수, KDoc 주석 필수
- 계층 분리: Controller → Service → Algorithm → Repository
- WebFlux: Mono, Flux, suspend 함수 활용
- 순수 함수: Algorithm 계층은 외부 의존성 최소화

## 문서 위치

| 문서 | 경로 |
|-----|-----|
| **시스템 통합** | `docs/references/architecture/SYSTEM_OVERVIEW.md` |
| 프로젝트 현황 | `docs/references/PROJECT_STATUS_SUMMARY.md` |
| 개발 가이드 | `docs/references/development/Development_Guide.md` |
| API 명세 | `docs/references/api/README.md` |
| **에러 메시지 관리** | `docs/references/Hardware_Error_Messages.md` |
| 진행중 작업 | `docs/features/active/` |
| 완료된 작업 | `docs/features/completed/` |

## 모드 시스템

| 모드 | 설명 |
|-----|-----|
| Standby | 대기 모드 |
| Step | 스텝 이동 |
| Slew | 슬루 이동 |
| EphemerisDesignation | 위성 궤도 지정 |
| PassSchedule | 패스 스케줄 |
| SunTrack | 태양 추적 |

## 주의사항

- 각도 단위: 내부 라디안, 표시 도(°)
- 시간: 내부 UTC, 표시 로컬
- Orekit 초기화 필요 (orekit-data 경로)
- **Train/Tilt 구분**: 변수명은 `train`, UI 표시는 `Tilt`
