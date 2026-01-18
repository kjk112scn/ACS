# TypeScript 패턴

> ACS 프로젝트에서 자주 쓰는 TypeScript 패턴

## 기본 타입

### Primitive Types
```typescript
const name: string = 'Kim'
const age: number = 30
const isActive: boolean = true
const data: null = null
const value: undefined = undefined
```

### Array & Object
```typescript
const numbers: number[] = [1, 2, 3]
const names: Array<string> = ['a', 'b']

const user: { name: string; age: number } = {
  name: 'Kim',
  age: 30
}
```

### Union & Literal
```typescript
// 여러 타입 중 하나
type Status = 'pending' | 'active' | 'completed'
type ID = string | number

// 사용
const status: Status = 'active'  // ✅
const status: Status = 'wrong'   // ❌ 컴파일 에러
```

---

## 인터페이스와 타입

### interface vs type
```typescript
// interface: 객체 구조 정의 (확장 가능)
interface User {
  name: string
  age: number
}

interface Admin extends User {
  role: string
}

// type: 모든 타입 정의 (유니온, 교차 등)
type Status = 'active' | 'inactive'
type UserOrAdmin = User | Admin
type UserWithStatus = User & { status: Status }
```

**언제 뭘 쓸까?**
| 상황 | 선택 |
|------|------|
| API 응답 구조 | `interface` |
| 객체 확장 필요 | `interface` |
| 유니온 타입 | `type` |
| 간단한 별칭 | `type` |

---

## 제네릭

### 기본 사용
```typescript
// 타입을 파라미터로 받음
function identity<T>(value: T): T {
  return value
}

const num = identity<number>(42)    // T = number
const str = identity<string>('hi')  // T = string
const inferred = identity(42)       // T = number (추론)
```

### API 응답 타입
```typescript
interface ApiResponse<T> {
  data: T
  status: number
  message: string
}

// 사용
type UserResponse = ApiResponse<User>
type UsersResponse = ApiResponse<User[]>

async function getUser(): Promise<ApiResponse<User>> {
  return await api.get('/user')
}
```

### 제네릭 제약
```typescript
// T는 반드시 id를 가진 객체여야 함
function findById<T extends { id: number }>(items: T[], id: number): T | undefined {
  return items.find(item => item.id === id)
}
```

---

## 유틸리티 타입

### Partial<T> - 모든 속성 선택적
```typescript
interface User {
  name: string
  age: number
  email: string
}

// 모든 속성이 optional
type PartialUser = Partial<User>
// = { name?: string; age?: number; email?: string }

// 사용: 업데이트 시
function updateUser(id: number, updates: Partial<User>) {
  // updates.name만 있어도 OK
}
```

### Required<T> - 모든 속성 필수
```typescript
interface Config {
  timeout?: number
  retries?: number
}

type RequiredConfig = Required<Config>
// = { timeout: number; retries: number }
```

### Pick<T, K> - 특정 속성만 선택
```typescript
interface User {
  id: number
  name: string
  email: string
  password: string
}

type PublicUser = Pick<User, 'id' | 'name' | 'email'>
// = { id: number; name: string; email: string }
```

### Omit<T, K> - 특정 속성 제외
```typescript
type UserWithoutPassword = Omit<User, 'password'>
// = { id: number; name: string; email: string }
```

### Record<K, V> - 키-값 매핑
```typescript
type StatusMap = Record<string, boolean>
// = { [key: string]: boolean }

const status: StatusMap = {
  isLoading: true,
  isError: false
}
```

---

## 타입 가드

### typeof
```typescript
function process(value: string | number) {
  if (typeof value === 'string') {
    // 여기서 value는 string
    return value.toUpperCase()
  } else {
    // 여기서 value는 number
    return value * 2
  }
}
```

### in 연산자
```typescript
interface Bird { fly(): void }
interface Fish { swim(): void }

function move(animal: Bird | Fish) {
  if ('fly' in animal) {
    animal.fly()  // Bird
  } else {
    animal.swim() // Fish
  }
}
```

### 커스텀 타입 가드
```typescript
interface User { type: 'user'; name: string }
interface Admin { type: 'admin'; name: string; permissions: string[] }

// 타입 가드 함수
function isAdmin(person: User | Admin): person is Admin {
  return person.type === 'admin'
}

// 사용
function greet(person: User | Admin) {
  if (isAdmin(person)) {
    console.log(`Admin: ${person.permissions}`)  // Admin 타입 확정
  } else {
    console.log(`User: ${person.name}`)  // User 타입 확정
  }
}
```

---

## as 타입 단언

### 기본 사용
```typescript
const input = document.getElementById('input') as HTMLInputElement
input.value = 'hello'

// 또는 꺾쇠 문법 (JSX에서 안 됨)
const input = <HTMLInputElement>document.getElementById('input')
```

### 주의사항
```typescript
// ❌ 나쁜 예: 무분별한 as
const data = response.data as User  // 런타임에 실패할 수 있음

// ✅ 좋은 예: 타입 가드 사용
function isUser(data: unknown): data is User {
  return typeof data === 'object' && data !== null && 'name' in data
}

if (isUser(response.data)) {
  const user = response.data  // 안전하게 User 타입
}
```

---

## ACS 프로젝트 패턴

### 1. API 응답 타입
```typescript
// types/api.ts
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
}

export interface PaginatedResponse<T> extends ApiResponse<T[]> {
  total: number
  page: number
  pageSize: number
}
```

### 2. 이벤트 핸들러 타입
```typescript
// Vue 이벤트
const onClick = (event: MouseEvent) => { ... }
const onInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  console.log(target.value)
}

// 커스텀 이벤트
type EmitEvents = {
  (e: 'update', value: number): void
  (e: 'delete', id: string): void
}

const emit = defineEmits<EmitEvents>()
```

### 3. Props 타입
```typescript
// Vue 3 + TypeScript
interface Props {
  title: string
  count?: number  // optional
  items: string[]
}

const props = withDefaults(defineProps<Props>(), {
  count: 0  // 기본값
})
```

### 4. Store 타입
```typescript
// stores/userStore.ts
interface UserState {
  currentUser: User | null
  isLoading: boolean
  error: string | null
}

export const useUserStore = defineStore('user', () => {
  const state = reactive<UserState>({
    currentUser: null,
    isLoading: false,
    error: null
  })

  // ...
})
```

### 5. WebSocket 메시지 타입
```typescript
// ICD 데이터 타입
interface ICDMessage {
  type: 'position' | 'status' | 'error'
  timestamp: number
  data: unknown
}

interface PositionData {
  azimuth: number
  elevation: number
  train: number
}

// 타입 가드
function isPositionMessage(msg: ICDMessage): msg is ICDMessage & { data: PositionData } {
  return msg.type === 'position'
}
```

---

## 유용한 패턴

### 1. Discriminated Union
```typescript
type LoadingState = { status: 'loading' }
type SuccessState<T> = { status: 'success'; data: T }
type ErrorState = { status: 'error'; error: string }

type AsyncState<T> = LoadingState | SuccessState<T> | ErrorState

// 사용
function render(state: AsyncState<User>) {
  switch (state.status) {
    case 'loading':
      return <Spinner />
    case 'success':
      return <UserProfile user={state.data} />  // data 접근 가능
    case 'error':
      return <Error message={state.error} />    // error 접근 가능
  }
}
```

### 2. Mapped Types
```typescript
// 모든 속성을 readonly로
type Readonly<T> = {
  readonly [P in keyof T]: T[P]
}

// 모든 속성을 nullable로
type Nullable<T> = {
  [P in keyof T]: T[P] | null
}
```

### 3. Template Literal Types
```typescript
type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
type Endpoint = '/users' | '/posts' | '/comments'

type ApiRoute = `${HttpMethod} ${Endpoint}`
// = 'GET /users' | 'GET /posts' | ... (12가지)
```

---

## 흔한 실수

### ❌ any 남용
```typescript
// 나쁜 예
function process(data: any) { ... }

// 좋은 예
function process<T>(data: T) { ... }
function process(data: unknown) { ... }  // 타입 체크 필요
```

### ❌ 불필요한 타입 단언
```typescript
// 나쁜 예
const name = user.name as string  // 이미 string인데?

// 좋은 예
const name = user.name  // 타입 추론 활용
```

### ❌ 타입과 값 혼동
```typescript
// 타입 (컴파일 타임)
interface User { name: string }

// 값 (런타임)
const user: User = { name: 'Kim' }

// ❌ 런타임에 타입 체크 불가
if (data instanceof User) { }  // 에러!

// ✅ 타입 가드 사용
function isUser(data: unknown): data is User { ... }
```

---

## Quick Reference

```typescript
// 유니온
type A = string | number

// 인터섹션
type B = User & Admin

// 제네릭
function fn<T>(x: T): T

// 유틸리티
Partial<T>    // 모든 속성 optional
Required<T>   // 모든 속성 required
Pick<T, K>    // K 속성만
Omit<T, K>    // K 속성 제외
Record<K, V>  // 키-값 매핑

// 타입 가드
typeof x === 'string'
'prop' in obj
function isX(x): x is X
```

---

**이전**: [vue-composables.md](./vue-composables.md) - Composable 패턴
