ㅈ# ACS 용어 사전

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

## 추가 프로그래밍 용어

### HTTP 관련

| 용어 | 설명 | 예시 |
|------|------|------|
| **GET** | 데이터 조회 요청 | `GET /api/users` |
| **POST** | 데이터 생성 요청 | `POST /api/users` |
| **PUT** | 데이터 전체 수정 | `PUT /api/users/1` |
| **PATCH** | 데이터 부분 수정 | `PATCH /api/users/1` |
| **DELETE** | 데이터 삭제 요청 | `DELETE /api/users/1` |
| **200 OK** | 요청 성공 | 정상 응답 |
| **201 Created** | 생성 성공 | POST 성공 |
| **400 Bad Request** | 잘못된 요청 | 검증 실패 |
| **404 Not Found** | 리소스 없음 | 존재하지 않는 데이터 |
| **500 Internal Error** | 서버 에러 | 예외 발생 |

### TypeScript 키워드

| 키워드 | 설명 | 예시 |
|--------|------|------|
| **interface** | 객체 구조 정의 | `interface User { name: string }` |
| **type** | 타입 별칭 | `type Status = 'active' \| 'inactive'` |
| **generic** | 타입 매개변수 | `Array<T>`, `Promise<T>` |
| **extends** | 타입 확장 | `interface Admin extends User` |
| **keyof** | 객체의 키 타입 | `keyof User` = `'name' \| 'age'` |
| **typeof** | 값에서 타입 추출 | `typeof myObj` |
| **as** | 타입 단언 | `data as User` |
| **unknown** | 안전한 any | 타입 체크 필수 |
| **never** | 절대 발생 안 함 | 에러 throw 함수 반환 |
| **readonly** | 읽기 전용 | `readonly name: string` |

### Vue 3 용어

| 용어 | 설명 | 예시 |
|------|------|------|
| **SFC** | Single File Component | `.vue` 파일 |
| **Composition API** | setup 기반 API | `<script setup>` |
| **Options API** | data/methods 기반 | `export default { data() }` |
| **v-model** | 양방향 바인딩 | `<input v-model="text">` |
| **v-if / v-show** | 조건부 렌더링 | `v-if="visible"` |
| **v-for** | 반복 렌더링 | `v-for="item in items"` |
| **v-on / @** | 이벤트 바인딩 | `@click="handler"` |
| **v-bind / :** | 속성 바인딩 | `:class="className"` |
| **slot** | 컨텐츠 삽입 위치 | `<slot name="header">` |
| **provide/inject** | 깊은 props 전달 | 조상→후손 데이터 전달 |

### Kotlin 키워드

| 키워드 | 설명 | 예시 |
|--------|------|------|
| **val** | 불변 변수 | `val name = "Kim"` |
| **var** | 가변 변수 | `var count = 0` |
| **fun** | 함수 정의 | `fun add(a: Int, b: Int): Int` |
| **class** | 클래스 정의 | `class User(val name: String)` |
| **data class** | 데이터 전용 클래스 | `data class User(val name: String)` |
| **object** | 싱글톤 객체 | `object Config { }` |
| **companion object** | 클래스 동반 객체 | 정적 메서드 대체 |
| **sealed class** | 제한된 하위 클래스 | 상태 표현에 유용 |
| **when** | 패턴 매칭 | `when (x) { 1 -> ... }` |
| **is** | 타입 체크 | `if (obj is String)` |
| **as** | 타입 캐스팅 | `obj as String` |
| **suspend** | 코루틴 함수 | `suspend fun fetch()` |
| **lateinit** | 지연 초기화 | `lateinit var service: Service` |
| **by lazy** | 지연 초기화 (불변) | `val data by lazy { load() }` |

### Spring 어노테이션

| 어노테이션 | 설명 | 사용 위치 |
|-----------|------|----------|
| **@RestController** | REST API 컨트롤러 | 클래스 |
| **@Service** | 서비스 레이어 | 클래스 |
| **@Repository** | 데이터 접근 레이어 | 클래스 |
| **@Component** | 일반 Bean | 클래스 |
| **@Autowired** | 의존성 주입 | 생성자/필드 |
| **@GetMapping** | GET 요청 매핑 | 메서드 |
| **@PostMapping** | POST 요청 매핑 | 메서드 |
| **@RequestBody** | 요청 본문 파싱 | 파라미터 |
| **@PathVariable** | URL 경로 변수 | 파라미터 |
| **@RequestParam** | 쿼리 파라미터 | 파라미터 |
| **@Valid** | 입력 검증 | 파라미터 |
| **@PreDestroy** | 소멸 전 실행 | 메서드 |
| **@PostConstruct** | 생성 후 실행 | 메서드 |
| **@Scheduled** | 스케줄링 | 메서드 |
| **@ControllerAdvice** | 전역 예외 처리 | 클래스 |

---

## 디자인 패턴 용어

| 패턴 | 설명 | ACS 예시 |
|------|------|----------|
| **Singleton** | 인스턴스 하나만 | GlobalData (object) |
| **Observer** | 상태 변경 알림 | Vue watch, WebSocket |
| **Factory** | 객체 생성 추상화 | - |
| **Strategy** | 알고리즘 교체 가능 | 추적 알고리즘 |
| **Repository** | 데이터 접근 추상화 | Repository 레이어 |
| **DTO** | 데이터 전송 객체 | Request/Response 클래스 |
| **DI** | 의존성 주입 | Spring @Autowired |

---

## 개발 도구 용어

| 용어 | 설명 |
|------|------|
| **IDE** | 통합 개발 환경 (IntelliJ, VSCode) |
| **npm** | Node.js 패키지 매니저 |
| **Gradle** | JVM 빌드 도구 |
| **Git** | 버전 관리 시스템 |
| **CI/CD** | 지속적 통합/배포 |
| **Lint** | 코드 스타일 검사 |
| **Unit Test** | 단위 테스트 |
| **E2E Test** | End-to-End 테스트 |
| **HMR** | Hot Module Replacement (코드 변경 즉시 반영) |
| **Transpile** | 코드 변환 (TS→JS) |
| **Bundle** | 파일 묶음 (Vite) |
| **Minify** | 코드 압축 |
| **Source Map** | 디버깅용 원본 매핑 |

---

## 수학/좌표 용어

| 용어 | 설명 | 예시 |
|------|------|------|
| **라디안 (rad)** | 각도 단위 (π = 180°) | `Math.PI / 2 = 90°` |
| **도 (°)** | 일반적인 각도 단위 | `360°` = 1회전 |
| **위도 (Latitude)** | 적도 기준 남북 위치 | -90° ~ +90° |
| **경도 (Longitude)** | 본초자오선 기준 동서 위치 | -180° ~ +180° |
| **고도 (Altitude)** | 해수면 기준 높이 | 미터 (m) |
| **방위각 (Azimuth)** | 북쪽 기준 시계방향 각도 | 0° ~ 360° |
| **고도각 (Elevation)** | 수평면 기준 위쪽 각도 | 0° ~ 90° |
| **ECEF** | 지구 중심 좌표계 | X, Y, Z (미터) |
| **ECI** | 관성 좌표계 | 위성 궤도 계산 |
| **Topocentric** | 관측자 중심 좌표 | Az/El 계산 |

---

## 위성 궤도 용어 (상세)

| 용어 | 설명 |
|------|------|
| **TLE 1행** | 위성 번호, 분류, 에포크 시각 |
| **TLE 2행** | 궤도 요소 (경사각, 이심률 등) |
| **Epoch** | TLE 기준 시각 |
| **Inclination** | 궤도 경사각 |
| **Eccentricity** | 이심률 (원=0, 타원<1) |
| **Mean Motion** | 하루당 공전 횟수 |
| **RAAN** | 승교점 적경 |
| **Argument of Perigee** | 근지점 인수 |
| **Mean Anomaly** | 평균 이각 |
| **Propagator** | 궤도 전파기 (SGP4) |
| **Ground Track** | 지상 궤적 |
| **Footprint** | 위성 가시 영역 |

---

## 통신 프로토콜 용어

| 용어 | 설명 |
|------|------|
| **TCP** | 연결 기반, 신뢰성 보장 |
| **UDP** | 비연결, 빠름, 손실 가능 |
| **HTTP** | 웹 통신 프로토콜 |
| **HTTPS** | 암호화된 HTTP |
| **WebSocket** | 양방향 실시간 통신 |
| **JSON** | JavaScript Object Notation |
| **Binary** | 이진 데이터 형식 |
| **Packet** | 데이터 전송 단위 |
| **Payload** | 실제 데이터 부분 |
| **Header** | 메타데이터 부분 |
| **Handshake** | 연결 수립 과정 |
| **Heartbeat** | 연결 유지 확인 |

---

## 에러/예외 용어

| 용어 | 설명 |
|------|------|
| **Exception** | 예외 (복구 가능) |
| **Error** | 에러 (복구 불가) |
| **try/catch** | 예외 처리 구문 |
| **throw** | 예외 발생 |
| **finally** | 항상 실행되는 블록 |
| **Stack Trace** | 에러 발생 위치 추적 |
| **NullPointerException** | null 참조 에러 |
| **IllegalArgumentException** | 잘못된 인자 |
| **IndexOutOfBoundsException** | 배열 범위 초과 |
| **RuntimeException** | 실행 중 예외 |
| **Checked Exception** | 처리 강제 예외 (Java) |
| **Unchecked Exception** | 처리 선택 예외 |

---

## Quick Lookup

```
Frontend (Vue/TS)
├── ref()         반응형 변수
├── reactive()    반응형 객체
├── computed()    계산된 값
├── watch()       값 변경 감시
├── v-model       양방향 바인딩
├── @click        이벤트 핸들러
└── :class        동적 클래스

Backend (Kotlin/Spring)
├── @Service      서비스 클래스
├── @GetMapping   GET 요청
├── @PostMapping  POST 요청
├── ?.            Safe call
├── ?:            Elvis (기본값)
├── ?.let { }     null 아니면 실행
└── data class    데이터 클래스

통신
├── REST API      HTTP 요청/응답
├── WebSocket     실시간 양방향
├── UDP           빠른 단방향
└── JSON          데이터 형식

안테나/위성
├── Az            방위각 (0~360°)
├── El            고도각 (0~90°)
├── TLE           궤도 요소
├── AOS           신호 획득
├── LOS           신호 손실
└── TCA           최근접 시각
```

---

**이전**: [data-flow.md](./data-flow.md) - 데이터 흐름
