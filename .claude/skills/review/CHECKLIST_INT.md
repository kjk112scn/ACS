# 통합 체크리스트 (FE-BE Integration)

> Frontend ↔ Backend 통신 및 데이터 일관성 분석

---

## 1. FE-BE 통신

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **INT-C01** | API 응답 타입 불일치 | High | BE DTO와 FE interface 필드 차이 |
| **INT-C02** | WebSocket 재연결 처리 | High | 연결 끊김 시 복구 로직 |
| **INT-C03** | 요청 타임아웃 처리 | Medium | 무한 대기 방지 |
| **INT-C04** | 중복 요청 방지 | Medium | 버튼 연타, 재시도 로직 |
| **INT-C05** | 네트워크 에러 UI 피드백 | Medium | 사용자에게 상태 표시 |
| **INT-C06** | 요청 취소 처리 | Medium | 페이지 전환 시 pending 요청 |

### 타입 불일치 탐지 예시

```typescript
// FE: frontend/src/types/api.d.ts
interface PassScheduleResponse {
  mstId: number      // camelCase
  startTime: string
}

// BE: backend/.../dto/PassScheduleDTO.kt
data class PassScheduleDTO(
  val mst_id: Long,    // snake_case → 불일치!
  val startTime: String
)
```

### WebSocket 재연결 패턴

```typescript
// GOOD: 재연결 로직 포함
class WebSocketManager {
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5

  connect() {
    this.ws = new WebSocket(url)
    this.ws.onclose = () => this.handleReconnect()
  }

  handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      setTimeout(() => {
        this.reconnectAttempts++
        this.connect()
      }, 1000 * Math.pow(2, this.reconnectAttempts)) // 지수 백오프
    }
  }
}
```

---

## 2. 데이터 일관성

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **INT-D01** | 단위 불일치 | Critical | 라디안/도, 초/밀리초 |
| **INT-D02** | 시간대 불일치 | Critical | UTC/로컬 시간 혼용 |
| **INT-D03** | 정밀도 손실 | Medium | Float→Double 변환 |
| **INT-D04** | ID 매핑 오류 | High | mstId, detailId 조합 |
| **INT-D05** | 상태 동기화 지연 | Medium | FE 상태와 BE 상태 불일치 |
| **INT-D06** | 캐시 무효화 누락 | Medium | 업데이트 후 캐시 갱신 |

### ACS 특화 검증

```kotlin
// BE: 내부는 라디안
val azimuthRad = calculateAzimuth() // 라디안

// API 응답: 변환 필요
fun toDTO() = PassScheduleDTO(
    azimuth = Math.toDegrees(azimuthRad) // 도로 변환
)
```

```typescript
// FE: 수신 시 확인
const azimuthDeg = response.azimuth // 이미 도 단위?
// 문서화 필수!
```

---

## 3. 에러 핸들링

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **INT-E01** | BE 예외 FE 전파 | High | 500 에러 시 FE 처리 |
| **INT-E02** | 에러 메시지 사용자 친화적 | Medium | 기술적 메시지 노출 |
| **INT-E03** | 부분 실패 처리 | Medium | 배치 작업 중 일부 실패 |
| **INT-E04** | 롤백 로직 | High | 실패 시 이전 상태 복원 |
| **INT-E05** | 에러 코드 표준화 | Medium | 일관된 에러 응답 형식 |

### 에러 처리 패턴

```typescript
// FE: 에러 처리
try {
  const result = await api.submitSchedule(data)
} catch (error) {
  if (error.response?.status === 400) {
    // 유효성 검사 실패 → 사용자에게 안내
    showValidationError(error.response.data.message)
  } else if (error.response?.status === 500) {
    // 서버 오류 → 일반 메시지
    showError("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
  } else {
    // 네트워크 오류
    showError("네트워크 연결을 확인해주세요.")
  }
}
```

```kotlin
// BE: 표준화된 에러 응답
data class ErrorResponse(
    val code: String,      // "VALIDATION_ERROR", "NOT_FOUND" 등
    val message: String,   // 사용자 친화적 메시지
    val details: Any? = null
)
```

---

## 4. API 계약

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **INT-A01** | API 버전 관리 | Medium | 변경 시 하위 호환성 |
| **INT-A02** | 필수 필드 누락 | High | BE 응답에서 필드 제거 |
| **INT-A03** | Null 처리 일관성 | High | null vs undefined vs 생략 |
| **INT-A04** | 날짜 형식 통일 | Medium | ISO 8601 표준 사용 |
| **INT-A05** | 페이지네이션 | Medium | 대용량 데이터 처리 |

### API 계약 검증

```typescript
// FE: interface 정의
interface ScheduleItem {
  id: number
  name: string
  startTime: string  // ISO 8601
  endTime?: string   // optional
}

// BE: DTO 정의 (일치해야 함)
data class ScheduleItemDTO(
    val id: Long,
    val name: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val startTime: Instant,
    val endTime: Instant? = null
)
```

---

## 5. 상태 동기화

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **INT-S01** | 낙관적 업데이트 불일치 | High | FE 즉시 반영 → BE 실패 |
| **INT-S02** | 폴링 vs 푸시 | Medium | 적절한 방식 선택 |
| **INT-S03** | 연결 상태 표시 | Medium | WebSocket 상태 UI 반영 |
| **INT-S04** | 오프라인 처리 | Low | 네트워크 끊김 시 동작 |
| **INT-S05** | 동시 편집 충돌 | Medium | 다중 사용자 시나리오 |

### 상태 동기화 패턴

```typescript
// 낙관적 업데이트 (위험할 수 있음)
async function updateSchedule(id: number, data: UpdateData) {
  // 1. FE 먼저 반영
  store.updateLocal(id, data)

  try {
    // 2. BE 요청
    await api.update(id, data)
  } catch (error) {
    // 3. 실패 시 롤백
    store.rollback(id)
    throw error
  }
}
```

---

## 6. 성능

| ID | 체크 항목 | 심각도 | 탐지 패턴 |
|----|----------|:------:|----------|
| **INT-P01** | N+1 쿼리 | High | 목록 조회 후 상세 반복 호출 |
| **INT-P02** | 불필요한 데이터 전송 | Medium | 사용하지 않는 필드 포함 |
| **INT-P03** | 요청 배치 처리 | Medium | 개별 요청 vs 일괄 요청 |
| **INT-P04** | 압축 미적용 | Low | 대용량 응답 gzip |
| **INT-P05** | 캐싱 미활용 | Medium | 정적 데이터 캐싱 |

---

## 검사 명령 예시

```bash
# INT-D01: 단위 변환 함수 확인
grep -r "toDegrees\|toRadians\|Math.PI" --include="*.kt" --include="*.ts"

# INT-A04: 날짜 형식 확인
grep -r "@JsonFormat\|DateTimeFormatter" --include="*.kt"

# INT-C01: API 타입 정의 비교
diff frontend/src/types/api.d.ts backend/src/main/kotlin/dto/
```

---

**버전:** 1.0.0
**작성일:** 2026-01-26
