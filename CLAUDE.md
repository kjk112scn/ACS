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
| **시스템 통합** | `docs/architecture/SYSTEM_OVERVIEW.md` |
| 개발 가이드 | `docs/guides/Development_Guide.md` |
| API 명세 | `docs/api/README.md` |
| 진행중 작업 | `docs/work/active/` |
| 완료된 작업 | `docs/work/archive/` |
| **컨텍스트 문서** | `docs/architecture/context/_INDEX.md` |

<!-- 필요 시 로드 (에이전트가 자동 참조) -->
<!-- @docs/architecture/context/domain/satellite-tracking.md -->
<!-- @docs/architecture/context/domain/icd-protocol.md -->
<!-- @docs/architecture/context/architecture/frontend.md -->
<!-- @docs/architecture/context/architecture/backend.md -->

## 모드 시스템

| 모드 | 설명 |
|-----|-----|
| Standby | 대기 모드 |
| Step | 스텝 이동 |
| Slew | 슬루 이동 |
| EphemerisDesignation | 위성 궤도 지정 |
| PassSchedule | 패스 스케줄 |
| SunTrack | 태양 추적 |

## 테스트

```bash
# Frontend
cd frontend && npm run test           # Vitest 단위 테스트
cd frontend && npx vue-tsc --noEmit   # 타입 체크

# Backend
cd backend && ./gradlew test          # JUnit 테스트
```

- 새 기능: 테스트 작성 권장
- 버그 수정: 회귀 테스트 추가

## 에러 처리

### Frontend
- `useErrorHandler` composable 사용
- try-catch 시 사용자 알림 필수

### Backend
- `GlobalExceptionHandler` 활용
- 광범위 `catch (Exception)` 금지 → 구체적 예외
- `.subscribe()` 에러 핸들러 필수

## 보안

- 입력 검증: `@Valid`, `@NotNull` 사용
- Path Traversal 주의 (파일 경로 검증)
- 하드코딩 비밀번호/키 금지

## 주의사항

- 각도 단위: 내부 라디안, 표시 도(°)
- 시간: 내부 UTC, 표시 로컬
- Orekit 초기화 필요 (orekit-data 경로)
- **Train/Tilt 구분**: 변수명은 `train`, UI 표시는 `Tilt`

## 작업 방식 (대화 종료 시 가이드 표시)

**중요**: 모든 작업 완료 후, 대화 마지막에 아래 가이드 블록을 표시합니다.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 다음에 쓸 수 있는 명령어
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔧 워크플로우
  /feature   새 기능 개발
  /bugfix    버그 수정
  /refactor  리팩토링 (파일 분리)
  /optimize  성능 최적화
  /cleanup   코드 정리 (console.log 등)
  /done      작업 완료 + 커밋

📊 상태 확인
  /health    빌드/품질 점검
  /status    프로젝트 현황
  /sync      문서 동기화

📝 문서화
  /plan      계획 수립
  /adr       아키텍처 결정 기록
  /docs      코드 → 문서 생성
  /api-sync  FE-BE 타입 동기화

💡 /guide 로 상세 안내
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 에이전트 (자동 호출)

| 에이전트 | 역할 | 모델 |
|---------|------|------|
| `fe-expert` | Vue/TS/Pinia 전문가 | Opus |
| `be-expert` | Kotlin/Spring 전문가 | Opus |
| `tech-lead` | 복잡한 요청 분석 | Opus |
| `architect` | 설계/ADR | Opus |
| `algorithm-expert` | Orekit/좌표 | Opus |
| `code-reviewer` | 품질 검증 | Opus |
| `code-counter` | 카운팅 | Haiku |
| `doc-syncer` | 문서 동기화 | Haiku |
