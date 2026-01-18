# ACS 용어 사전

> 프로젝트에서 사용되는 용어와 약어

## 도메인 용어 (안테나/위성)

### 안테나 관련

| 용어 | 영문 | 설명 |
|------|------|------|
| **Az** | Azimuth | 방위각. 북쪽 기준 시계방향 각도 (0~360°) |
| **El** | Elevation | 고도각. 수평면 기준 위쪽 각도 (0~90°) |
| **Train/Tilt** | Train | 기울기 각도. 코드에서는 `train`, UI에서는 `Tilt` 표시 |
| **ACU** | Antenna Control Unit | 안테나 제어 장치 (하드웨어) |
| **ICD** | Interface Control Document | 장비 간 통신 규격 문서/프로토콜 |
| **MC** | Motor Controller | 모터 제어기 |
| **AOS** | Acquisition of Signal | 위성 신호 획득 시점 (패스 시작) |
| **LOS** | Loss of Signal | 위성 신호 손실 시점 (패스 종료) |
| **TCA** | Time of Closest Approach | 최근접 시각 (최대 고도각) |

### 위성 관련

| 용어 | 영문 | 설명 |
|------|------|------|
| **TLE** | Two-Line Element | 위성 궤도 요소 (2줄 텍스트 형식) |
| **Pass** | Pass | 위성이 지상국 위를 지나가는 것 |
| **Ephemeris** | Ephemeris | 위성 궤적 예보 데이터 |
| **NORAD ID** | NORAD Catalog Number | 위성 고유 번호 |
| **SGP4** | Simplified General Perturbations 4 | TLE 기반 궤도 예측 알고리즘 |

---

## 프로그래밍 용어

### Frontend (Vue/TypeScript)

| 용어 | 설명 | 예시 |
|------|------|------|
| **ref** | 반응형 변수 (단일 값) | `const count = ref(0)` |
| **reactive** | 반응형 객체 | `const state = reactive({...})` |
| **computed** | 계산된 값 (캐시됨) | `computed(() => a + b)` |
| **watch** | 값 변경 감시 | `watch(value, callback)` |
| **shallowRef** | 얕은 반응형 (성능) | 최상위만 감시 |
| **composable** | 재사용 로직 함수 | `useErrorHandler()` |
| **store** | 전역 상태 저장소 | Pinia store |
| **component** | UI 구성 요소 | `.vue` 파일 |
| **props** | 부모→자식 데이터 전달 | `defineProps()` |
| **emit** | 자식→부모 이벤트 전달 | `defineEmits()` |

### Backend (Kotlin/Spring)

| 용어 | 설명 | 예시 |
|------|------|------|
| **Mono** | 0~1개 결과 (비동기) | `Mono<User>` |
| **Flux** | 0~N개 결과 (스트림) | `Flux<Event>` |
| **suspend** | 코루틴 함수 | `suspend fun fetch()` |
| **data class** | 데이터 전용 클래스 | `data class User(val name: String)` |
| **?.** | Safe call (null 안전) | `user?.name` |
| **?:** | Elvis (기본값) | `name ?: "Unknown"` |
| **let** | 스코프 함수 | `user?.let { ... }` |
| **@Service** | 서비스 레이어 Bean | 비즈니스 로직 |
| **@Controller** | 웹 요청 처리 Bean | REST 엔드포인트 |
| **@PreDestroy** | 종료 전 정리 | Graceful shutdown |

---

## 아키텍처 용어

| 용어 | 설명 |
|------|------|
| **FE** | Frontend (프론트엔드) - 사용자 화면 |
| **BE** | Backend (백엔드) - 서버 로직 |
| **API** | Application Programming Interface - 프로그램 간 통신 규약 |
| **REST** | Representational State Transfer - HTTP 기반 API 스타일 |
| **WebSocket** | 양방향 실시간 통신 프로토콜 |
| **UDP** | User Datagram Protocol - 빠른 단방향 통신 |
| **DTO** | Data Transfer Object - 데이터 전송용 객체 |
| **CRUD** | Create, Read, Update, Delete - 기본 데이터 작업 |

---

## 프로젝트 특화 용어

### 모드 (Mode)

| 모드 | 설명 |
|------|------|
| **Standby** | 대기 모드 - 안테나 정지 |
| **Step** | 스텝 이동 - 지정 각도로 이동 |
| **Slew** | 슬루 이동 - 속도 기반 이동 |
| **EphemerisDesignation** | 위성 궤도 지정 - 위성 추적 |
| **PassSchedule** | 패스 스케줄 - 예약된 패스 자동 추적 |
| **SunTrack** | 태양 추적 - 태양 위치 추적 |

### 파일/폴더 약어

| 약어 | 전체 이름 | 설명 |
|------|----------|------|
| `icdStore` | ICD Store | ICD 데이터 상태 관리 |
| `passScheduleStore` | Pass Schedule Store | 패스 스케줄 상태 |
| `composables/` | Composables | 재사용 로직 모음 |
| `services/` | Services | API 통신 로직 |

---

## 단위

| 단위 | 용도 | 변환 |
|------|------|------|
| **라디안 (rad)** | 내부 계산 | π rad = 180° |
| **도 (°)** | UI 표시 | `Math.toDegrees()` |
| **UTC** | 내부 시간 | 협정 세계시 |
| **로컬 시간** | UI 표시 | 사용자 시간대 |

---

## 약어 모음

| 약어 | 전체 | 설명 |
|------|------|------|
| ACS | Antenna Control System | 안테나 제어 시스템 |
| ACU | Antenna Control Unit | 안테나 제어 장치 |
| ICD | Interface Control Document | 인터페이스 규격 |
| TLE | Two-Line Element | 궤도 요소 |
| Az | Azimuth | 방위각 |
| El | Elevation | 고도각 |
| AOS | Acquisition of Signal | 신호 획득 |
| LOS | Loss of Signal | 신호 손실 |
| TCA | Time of Closest Approach | 최근접 시각 |
| FE | Frontend | 프론트엔드 |
| BE | Backend | 백엔드 |
| API | Application Programming Interface | 프로그램 인터페이스 |
| REST | Representational State Transfer | HTTP API 스타일 |
| DTO | Data Transfer Object | 데이터 전송 객체 |
| CRUD | Create Read Update Delete | 기본 데이터 작업 |

---

## 빠른 참조

### "이거 뭐야?" 시리즈

**`?.let { }`이 뭐야?**
```kotlin
user?.let { u -> sendEmail(u.email) }
// = user가 null이 아니면 블록 실행
```

**`ref()`가 뭐야?**
```typescript
const count = ref(0)  // 반응형 변수
count.value++         // .value로 접근
```

**`Mono<T>`가 뭐야?**
```kotlin
fun getUser(): Mono<User>  // 0~1개 결과를 비동기로 반환
```

**`@PostMapping`이 뭐야?**
```kotlin
@PostMapping("/users")  // POST /users 요청을 이 함수가 처리
fun create() { ... }
```

---

**이전**: [data-flow.md](./data-flow.md) - 데이터 흐름
