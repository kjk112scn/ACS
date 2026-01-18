# ACS 기술 스택

> 시스템에서 사용하는 기술들과 선택 이유

## 전체 구조

```
┌─────────────────────────────────────────────────────────────┐
│                        사용자 (브라우저)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Frontend (Vue 3 + Quasar + TypeScript)                     │
│  ├── UI 컴포넌트 (Quasar)                                    │
│  ├── 상태관리 (Pinia)                                        │
│  └── 실시간 데이터 (WebSocket)                               │
└─────────────────────────────────────────────────────────────┘
                    │                    │
              REST API              WebSocket
                    │                    │
                    ▼                    ▼
┌─────────────────────────────────────────────────────────────┐
│  Backend (Kotlin + Spring Boot + WebFlux)                   │
│  ├── REST Controller (HTTP 요청 처리)                        │
│  ├── WebSocket Handler (실시간 데이터 푸시)                   │
│  ├── Service Layer (비즈니스 로직)                           │
│  └── Algorithm (Orekit - 위성 궤도 계산)                     │
└─────────────────────────────────────────────────────────────┘
                              │
                             UDP
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  안테나 제어 장비 (ACU - Antenna Control Unit)               │
│  └── ICD 프로토콜로 통신                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## Frontend 기술 스택

### Vue 3
**무엇인가?**
- JavaScript 프레임워크 (React, Angular와 경쟁)
- 화면(UI)을 만드는 도구

**왜 선택했나?**
| 장점 | 설명 |
|------|------|
| 학습 곡선 낮음 | React보다 직관적 |
| 단일 파일 컴포넌트 | HTML + JS + CSS 한 파일에 |
| Composition API | 로직 재사용 용이 |
| 국내 사용률 높음 | 한국어 자료 풍부 |

**핵심 개념**:
```vue
<template>
  <!-- HTML (화면) -->
  <div>{{ message }}</div>
</template>

<script setup lang="ts">
// JavaScript/TypeScript (로직)
const message = ref('Hello')
</script>

<style scoped>
/* CSS (스타일) */
div { color: blue; }
</style>
```

---

### Quasar 2.x
**무엇인가?**
- Vue용 UI 컴포넌트 라이브러리
- 버튼, 테이블, 다이얼로그 등 미리 만들어진 컴포넌트 제공

**왜 선택했나?**
| 장점 | 설명 |
|------|------|
| 풍부한 컴포넌트 | 70+ 컴포넌트 제공 |
| Material Design | 일관된 디자인 |
| 반응형 | 모바일/데스크탑 자동 대응 |
| 테마 시스템 | 다크모드 등 쉽게 적용 |

**사용 예시**:
```vue
<template>
  <q-btn label="클릭" color="primary" @click="onClick" />
  <q-table :rows="data" :columns="columns" />
  <q-dialog v-model="showDialog">...</q-dialog>
</template>
```

---

### TypeScript 5.x
**무엇인가?**
- JavaScript + 타입 시스템
- 컴파일 시점에 오류 발견

**왜 선택했나?**
| 장점 | 설명 |
|------|------|
| 타입 안전 | 런타임 에러 방지 |
| IDE 지원 | 자동완성, 리팩토링 |
| 문서화 | 타입 자체가 문서 |
| 대형 프로젝트 | 유지보수 용이 |

**비교**:
```javascript
// JavaScript - 런타임에 터짐
function add(a, b) {
  return a + b
}
add("1", 2)  // "12" (???)

// TypeScript - 컴파일 시 에러
function add(a: number, b: number): number {
  return a + b
}
add("1", 2)  // ❌ 컴파일 에러!
```

---

### Pinia
**무엇인가?**
- Vue 전역 상태 관리 라이브러리
- 여러 컴포넌트에서 공유하는 데이터 관리

**왜 선택했나?**
| 장점 | 설명 |
|------|------|
| Vue 3 공식 | Vuex 대체 |
| TypeScript 친화 | 타입 추론 완벽 |
| 간결한 문법 | 보일러플레이트 적음 |
| DevTools | 디버깅 용이 |

**사용 예시**:
```typescript
// stores/userStore.ts
export const useUserStore = defineStore('user', () => {
  const name = ref('Kim')
  const isAdmin = computed(() => name.value === 'Admin')

  function login() { ... }

  return { name, isAdmin, login }
})

// 컴포넌트에서 사용
const userStore = useUserStore()
console.log(userStore.name)
```

---

## Backend 기술 스택

### Kotlin 1.9
**무엇인가?**
- JVM 언어 (Java와 100% 호환)
- Java의 현대적 대안

**왜 선택했나?**
| 장점 | 설명 |
|------|------|
| Null 안전 | NullPointerException 방지 |
| 간결한 문법 | Java 대비 코드량 40% 감소 |
| 코루틴 | 비동기 처리 간편 |
| Spring 공식 지원 | 1급 시민 |

**Java vs Kotlin**:
```java
// Java - 장황함
public class User {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
}

// Kotlin - 한 줄
data class User(val name: String, val age: Int)
```

---

### Spring Boot 3.x
**무엇인가?**
- Java/Kotlin 웹 애플리케이션 프레임워크
- 설정 자동화, 내장 서버

**왜 선택했나?**
| 장점 | 설명 |
|------|------|
| 산업 표준 | 대부분의 기업에서 사용 |
| 생태계 | 풍부한 라이브러리 |
| 자동 설정 | 설정 최소화 |
| 안정성 | 검증된 프레임워크 |

**구조**:
```
Controller (요청 받음)
    ↓
Service (비즈니스 로직)
    ↓
Repository (데이터 접근)
```

---

### WebFlux (리액티브)
**무엇인가?**
- Spring의 비동기/논블로킹 웹 프레임워크
- 적은 스레드로 많은 요청 처리

**왜 선택했나?**
| 장점 | 설명 |
|------|------|
| 고성능 | 적은 리소스로 많은 동시 접속 |
| 실시간 적합 | WebSocket, SSE 지원 |
| 배압 제어 | 데이터 흐름 제어 |

**일반 vs 리액티브**:
```kotlin
// 일반 (블로킹) - 스레드가 대기
fun getUser(id: Long): User {
    return repository.findById(id)  // DB 응답까지 스레드 멈춤
}

// 리액티브 (논블로킹) - 스레드 반환
fun getUser(id: Long): Mono<User> {
    return repository.findById(id)  // 즉시 반환, 나중에 결과 전달
}
```

---

### Orekit 13.0
**무엇인가?**
- 우주 역학 라이브러리 (Java)
- 위성 궤도 계산, TLE 처리

**왜 선택했나?**
| 장점 | 설명 |
|------|------|
| 정확도 | NASA, ESA 수준 |
| 오픈소스 | 무료 |
| TLE 지원 | Two-Line Element 파싱 |
| 좌표 변환 | 다양한 좌표계 지원 |

**사용 예시**:
```kotlin
// TLE로 위성 위치 계산
val tle = TLE(line1, line2)
val propagator = SGP4(tle)
val position = propagator.propagate(targetDate)
val azElRange = toAzElRange(position, groundStation)
```

---

## 통신 프로토콜

### REST API
**용도**: 일반적인 요청/응답 (설정 저장, 데이터 조회)

```
클라이언트 → GET /api/users/1 → 서버
클라이언트 ← { "name": "Kim" } ← 서버
```

### WebSocket
**용도**: 실시간 양방향 통신 (안테나 상태, 추적 데이터)

```
연결 수립 후 계속 열려있음
서버 → 30ms마다 데이터 푸시 → 클라이언트
클라이언트 → 명령 전송 → 서버
```

### UDP (ICD)
**용도**: 안테나 제어 장비와 통신

```
Backend ←→ UDP 패킷 ←→ ACU (안테나)
         ↑
    ICD 프로토콜 (바이너리)
```

---

## 왜 이 조합인가?

### Frontend: Vue + Quasar + TypeScript
```
React도 좋지만...
├── Vue가 더 직관적 (템플릿 문법)
├── Quasar로 UI 빠르게 구축
└── TypeScript로 안정성 확보
```

### Backend: Kotlin + Spring + WebFlux
```
Java도 되지만...
├── Kotlin이 더 현대적 (null 안전, 간결)
├── Spring이 산업 표준
└── WebFlux로 실시간 처리 최적화
```

### 전체 조합의 시너지
| FE ↔ BE | 연결 |
|---------|------|
| TypeScript ↔ Kotlin | 둘 다 타입 안전, 비슷한 문법 |
| Pinia ↔ WebSocket | 실시간 상태 동기화 |
| Vue 반응형 ↔ Flux 리액티브 | 데이터 흐름 일관성 |

---

## 버전 정보

| 기술 | 버전 | 비고 |
|------|------|------|
| Vue | 3.x | Composition API |
| Quasar | 2.x | Vue 3 전용 |
| TypeScript | 5.x | 최신 |
| Pinia | 2.x | Vue 3 공식 |
| Kotlin | 1.9 | LTS |
| Spring Boot | 3.x | Java 17+ |
| WebFlux | (Spring 포함) | 리액티브 |
| Orekit | 13.0 | 위성 궤도 |

---

**다음**: [data-flow.md](./data-flow.md) - 데이터가 어떻게 흐르는지
