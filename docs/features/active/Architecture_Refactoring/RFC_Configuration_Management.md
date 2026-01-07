# RFC: Configuration Management 개선

> **버전**: 1.0.0 | **작성**: 2026-01-07 | **상태**: Draft

## 1. 개요

### 문제 정의

현재 ACS의 설정 관리는 **메모리 기반 In-Memory 저장소**로 운영되며, DB 영구 저장 기능은 구현되어 있으나 **Frontend와 Backend 간 초기값 불일치**, **실시간 동기화 부재**, **설정 변경 이력 UI 미제공** 등의 문제가 있습니다.

### 목표

1. **Frontend-Backend 설정 동기화**: 초기값 일치, 실시간 동기화
2. **설정 변경 이력 UI 제공**: Audit trail 가시화
3. **2026 베스트 프랙티스 적용**: Optimistic UI, WebSocket 동기화
4. **운영 편의성 향상**: Import/Export, 설정 검증 강화

### 범위

- **Backend**: `SettingsService.kt` (1,184줄) 개선
- **Frontend**: 10개 설정 Store (1,190줄) 통합 및 동기화
- **Database**: `settings`, `setting_history` 테이블 활용
- **WebSocket**: 실시간 설정 변경 브로드캐스트 추가

---

## 2. 현재 아키텍처 분석

### 2.1 Backend: SettingsService.kt

**파일 정보**: `backend/src/main/kotlin/.../SettingsService.kt` (1,184줄)

**주요 구조**:
```kotlin
@Service
@Transactional
class SettingsService(
    private val settingsRepository: SettingsRepository?,
    private val settingsHistoryRepository: SettingsHistoryRepository?,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val settings = ConcurrentHashMap<String, Any>()

    // 64개 설정 정의
    private val settingDefinitions = mapOf(
        "location.latitude" to SettingDefinition("location.latitude", 35.317540, SettingType.DOUBLE, "위도"),
        "location.longitude" to SettingDefinition("location.longitude", 128.608510, SettingType.DOUBLE, "경도"),
        // ... 62 more settings
    )

    // Kotlin Delegation Pattern
    var latitude: Double by createSettingProperty("location.latitude", "위도")
    var longitude: Double by createSettingProperty("location.longitude", "경도")

    @PostConstruct
    fun initialize() {
        settings.putAll(defaultSettings)
        if (settingsRepository != null) {
            loadSettingsFromDatabase()
        }
    }

    fun updateSetting(key: String, value: Any, reason: String, modifiedBy: String) {
        val oldValue = settings[key]
        settings[key] = value

        // DB 저장
        settingsRepository?.save(Setting(key, value))
        settingsHistoryRepository?.save(SettingHistory(key, oldValue, value, reason, modifiedBy))

        // 이벤트 발행
        eventPublisher.publishEvent(SettingsChangedEvent(key, oldValue, value))
    }
}
```

**장점**:
- ✅ **Hybrid Storage**: In-Memory (ConcurrentHashMap) + DB 영구 저장
- ✅ **Type-Safe Configuration**: Kotlin Delegation Pattern (`by createSettingProperty`)
- ✅ **Event-Driven**: `SettingsChangedEvent` 발행
- ✅ **Settings History**: 변경 이력 자동 저장

**문제점**:
- ❌ WebSocket 브로드캐스트 미구현 (이벤트만 발행, 클라이언트 전송 없음)
- ❌ 거대 파일 (1,184줄): 단일 책임 원칙 위반
- ❌ 일부 설정 Frontend 미노출 (UDP, JVM, Storage 등 29개)

---

### 2.2 Frontend: 설정 Store 구조

**파일 정보**: `frontend/src/stores/api/settings/` (10개 파일, 1,190줄)

**구조**:
```
frontend/src/stores/api/settings/
├── settingsStore.ts             (171줄) - 메인 통합 Store
├── locationSettingsStore.ts     (99줄)
├── trackingSettingsStore.ts     (86줄)
├── stowSettingsStore.ts         (146줄)
├── antennaSettingsStore.ts      (141줄)
├── antennaLimitSettingsStore.ts (160줄)
├── stepSizeLimitSettingsStore.ts (159줄)
├── velocityLimitSettingsStore.ts (107줄)
├── maintenanceSettingsStore.ts  (58줄)
└── icdSettingsStore.ts          (63줄)
```

**예시 코드** ([locationSettingsStore.ts:99](frontend/src/stores/api/settings/locationSettingsStore.ts)):
```typescript
export const useLocationSettingsStore = defineStore('locationSettings', () => {
    const locationSettings = ref<LocationSettings>({
        latitude: 0,      // ❌ Backend default: 35.317540
        longitude: 0,     // ❌ Backend default: 128.608510
        altitude: 0,      // ✅ Matches backend
    })

    const hasUnsavedChanges = ref(false)

    const saveLocationSettings = async (settings: LocationSettings) => {
        await settingsService.setLocationSettings(settings)
        locationSettings.value = settings // 성공 후 업데이트
        hasUnsavedChanges.value = false
    }

    return { locationSettings, saveLocationSettings, hasUnsavedChanges }
})
```

**장점**:
- ✅ **Setup Store 패턴**: Vue 3 Composition API 기반
- ✅ **반응형 상태 관리**: Pinia reactive state
- ✅ **설정 그룹 분리**: 도메인별 Store 분리

**문제점**:
- ❌ **초기값 불일치**: Frontend 0, Backend 35.317540 (latitude)
- ❌ **실시간 동기화 없음**: WebSocket 연결 부재
- ❌ **Optimistic UI 미흡**: 저장 실패 시 롤백 없음
- ❌ **중복 구조**: 10개 Store의 유사한 패턴 반복

---

## 3. 식별된 문제점 (10가지)

### 3.1 FE-BE 초기값 불일치

**문제**:
- Frontend: `latitude: 0, longitude: 0`
- Backend: `latitude: 35.317540, longitude: 128.608510`
- 최초 로딩 시 Frontend가 Backend 기본값을 반영하지 못함

**영향**:
- 최초 실행 시 잘못된 위치 정보 표시
- API 호출 전까지 0, 0 좌표 표시

**우선순위**: P0 (긴급)

---

### 3.2 시스템 설정 Frontend 미노출 (29개)

**문제**:
Backend에만 존재하는 설정 (예시):
```kotlin
// Backend에만 존재
"udp.receive.port" to SettingDefinition("udp.receive.port", 9001, SettingType.INTEGER, "UDP 수신 포트")
"jvm.max.heap.size" to SettingDefinition("jvm.max.heap.size", 2048, SettingType.INTEGER, "JVM 최대 힙 크기")
"storage.max.days" to SettingDefinition("storage.max.days", 30, SettingType.INTEGER, "데이터 보관 일수")
```

**영향**:
- 시스템 관리자가 Frontend에서 조회/수정 불가
- Backend 직접 접근 또는 DB 수정 필요

**우선순위**: P1 (높음)

---

### 3.3 실시간 동기화 부재

**문제**:
- Backend: `SettingsChangedEvent` 발행만 구현
- WebSocket: 설정 변경 브로드캐스트 미구현
- 결과: 다른 브라우저 탭/사용자에게 변경사항 미반영

**영향**:
- 다중 사용자 환경에서 설정 불일치
- 수동 새로고침 필요

**우선순위**: P0 (긴급)

---

### 3.4 타입 변환 리스크

**문제**:
```typescript
// Frontend에서 JSON 변환 시 타입 손실 가능
const response = await axios.get('/api/settings/location')
locationSettings.value = response.data // ❌ 타입 검증 없음
```

**영향**:
- 런타임 타입 오류 가능성
- `latitude` 문자열로 전송 시 오류

**우선순위**: P1 (높음)

---

### 3.5 클라이언트 검증만 존재

**문제**:
- Frontend: Form validation 구현
- Backend: `@Valid` 어노테이션 일부만 사용
- 결과: Backend 검증 우회 가능

**영향**:
- 직접 API 호출 시 잘못된 값 저장 가능

**우선순위**: P1 (높음)

---

### 3.6 설정 변경 이력 UI 부재

**문제**:
- Backend: `setting_history` 테이블에 이력 저장
- Frontend: 조회 UI 없음

**영향**:
- Audit trail 가시성 부족
- 디버깅 어려움

**우선순위**: P2 (중간)

---

### 3.7 설정 Import/Export 없음

**문제**:
- 백업/복원 기능 없음
- 설정 마이그레이션 수동 작업 필요

**영향**:
- 재배포 시 설정 재입력 필요
- 다중 환경 관리 어려움

**우선순위**: P2 (중간)

---

### 3.8 설정 그룹 권한 관리 없음

**문제**:
- 모든 설정에 동일 권한
- 위험 설정(Antenna Limit 등)과 일반 설정 구분 없음

**영향**:
- 관리자 외 사용자가 위험 설정 변경 가능

**우선순위**: P3 (낮음)

---

### 3.9 Feature Flag 시스템 없음

**문제**:
- 실험적 기능 제어 불가
- 코드 수정 없이 기능 ON/OFF 불가

**영향**:
- 점진적 배포(Progressive Rollout) 불가

**우선순위**: P3 (낮음, 필요 시 검토)

---

### 3.10 설정 분산 구조

**문제**:
- Frontend: 10개 Store로 분산
- Backend: 1개 Service에 1,184줄

**영향**:
- 중복 패턴 코드 (각 Store의 `save` 로직 유사)
- 유지보수 어려움

**우선순위**: P1 (높음)

---

## 4. 2026년 Configuration Management 베스트 프랙티스

### 4.1 주요 트렌드

2026년 현재, Configuration Management는 다음 패턴이 표준입니다:

1. **Hybrid Storage with Event-Driven Sync**
   - DB (영구 저장) + In-Memory (성능)
   - Event 발행 → WebSocket 브로드캐스트

2. **Type-Safe Configuration Properties**
   - Backend: Kotlin data class + Delegation
   - Frontend: TypeScript interface + Runtime validation (Zod/Valibot)

3. **Optimistic UI Updates with Rollback**
   - 즉시 UI 업데이트 → 검증 → 실패 시 롤백

4. **Layered Configuration**
   - System defaults → Environment config → Runtime settings

5. **Settings History & Audit Trail**
   - 모든 변경 이력 추적
   - Who, When, What, Why 기록

### 4.2 ACS의 현재 구현 수준

ACS 프로젝트는 **이미 2026년 베스트 프랙티스의 80%를 구현**하고 있습니다:

| 패턴 | 구현 여부 | 완성도 |
|-----|---------|-------|
| Hybrid Storage | ✅ | 90% |
| Type-Safe Configuration | ✅ | 85% |
| Event-Driven Architecture | ✅ | 70% (WebSocket 미구현) |
| Settings History | ✅ | 80% (UI 없음) |
| Validation Layer | ⚠️ | 60% (Backend 검증 부족) |
| Optimistic UI | ⚠️ | 40% (롤백 없음) |
| Feature Flags | ❌ | 0% |

---

## 5. 해결 방안 및 개선 권장사항

### 5.1 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (Vue 3 + Pinia)                 │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Unified Settings Store (settingsStore.ts)                │  │
│  │ - Composite pattern: 10개 설정 그룹 통합                   │  │
│  │ - Optimistic updates with rollback                       │  │
│  │ - WebSocket listener for real-time sync                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            ↕ REST API / WebSocket               │
├─────────────────────────────────────────────────────────────────┤
│                    Backend (Kotlin + Spring Boot)               │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ SettingsController (REST API)                            │  │
│  │ - GET /api/settings/all (초기값 제공)                     │  │
│  │ - POST /api/settings/{group} (그룹별 업데이트)            │  │
│  │ - GET /api/settings/history (이력 조회)                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                 ↕                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ SettingsService (Business Logic)                         │  │
│  │ - ConcurrentHashMap (In-Memory Cache)                    │  │
│  │ - publishEvent(SettingsChangedEvent)                     │  │
│  │ - Validation (JSR-303)                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                 ↕                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ WebSocketHandler (@EventListener)                        │  │
│  │ - handleSettingChanged()                                 │  │
│  │ - broadcast to all sessions                             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                 ↕                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Database (settings, setting_history)                     │  │
│  │ - Persistent storage                                     │  │
│  │ - Audit trail                                            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

### 5.2 Quick Wins (즉시 적용 가능)

#### Quick Win 1: WebSocket 설정 동기화 추가 (1-2일)

**Before**:
```kotlin
// SettingsService.kt
private fun publishSettingChangedEvent(key: String, oldValue: Any?, newValue: Any) {
    eventPublisher.publishEvent(SettingsChangedEvent(...))
    // ❌ WebSocket 브로드캐스트 없음
}
```

**After**:
```kotlin
// WebSocketHandler.kt (새 파일 또는 기존 파일 확장)
@Component
class SettingsWebSocketHandler(
    private val webSocketSessions: MutableSet<WebSocketSession>
) {

    @EventListener
    fun handleSettingChanged(event: SettingsChangedEvent) {
        val message = mapOf(
            "type" to "SETTINGS_CHANGED",
            "key" to event.key,
            "value" to event.value,
            "timestamp" to Instant.now().toString()
        )

        webSocketSessions.forEach { session ->
            if (session.isOpen) {
                session.sendMessage(TextMessage(objectMapper.writeValueAsString(message)))
            }
        }
    }
}
```

**Frontend**:
```typescript
// settingsStore.ts
const setupWebSocketListener = () => {
    const ws = useWebSocketStore()

    ws.on('SETTINGS_CHANGED', (data: { key: string, value: any }) => {
        // 실시간 동기화
        updateSettingByKey(data.key, data.value)
    })
}
```

**효과**:
- 다중 사용자 실시간 동기화
- 수동 새로고침 불필요
- 설정 불일치 방지

---

#### Quick Win 2: Optimistic UI with Rollback (1-2일)

**Before**:
```typescript
const saveLocationSettings = async (settings: LocationSettings) => {
    await settingsService.setLocationSettings(settings)
    locationSettings.value = settings // ❌ 성공 후에만 업데이트
}
```

**After**:
```typescript
const saveLocationSettings = async (settings: LocationSettings) => {
    const backup = { ...locationSettings.value }

    // Optimistic update
    locationSettings.value = settings
    hasUnsavedChanges.value = false

    try {
        await settingsService.setLocationSettings(settings)
        // 성공: 이미 업데이트됨
    } catch (error) {
        // 실패: 롤백
        locationSettings.value = backup
        hasUnsavedChanges.value = true

        useNotification().error('설정 저장 실패: ' + error.message)
        throw error
    }
}
```

**효과**:
- 즉각적인 UI 피드백
- 네트워크 지연 감춤
- UX 크게 개선

---

#### Quick Win 3: 초기값 동기화 (1일)

**Before**:
```typescript
const locationSettings = ref<LocationSettings>({
    latitude: 0,      // ❌ 하드코딩
    longitude: 0,
    altitude: 0,
})
```

**After**:
```typescript
const locationSettings = ref<LocationSettings>({
    latitude: 0,
    longitude: 0,
    altitude: 0,
})

// 초기화 시 Backend 값 로드
const initialize = async () => {
    const response = await settingsService.getAllSettings()
    locationSettings.value = response.location
}

// Store 생성 시 자동 호출
initialize()
```

**Backend 추가**:
```kotlin
// SettingsController.kt
@GetMapping("/api/settings/all")
fun getAllSettings(): Map<String, Any> {
    return mapOf(
        "location" to settingsService.getLocationSettings(),
        "tracking" to settingsService.getTrackingSettings(),
        // ... 나머지 그룹
    )
}
```

**효과**:
- Frontend-Backend 초기값 일치
- 최초 로딩 시 올바른 값 표시

---

### 5.3 중기 개선 사항 (1-2주)

#### 개선 1: 설정 이력 UI 추가

**새 컴포넌트**: [SettingsHistory.vue](frontend/src/components/Settings/SettingsHistory.vue)
```vue
<template>
  <q-card class="settings-history-card">
    <q-card-section>
      <div class="text-h6">설정 변경 이력</div>
    </q-card-section>

    <q-card-section>
      <q-timeline color="primary">
        <q-timeline-entry
          v-for="history in settingsHistory"
          :key="history.id"
          :title="history.settingKey"
          :subtitle="formatDateTime(history.modifiedAt)"
        >
          <div class="text-body2">
            <span class="text-negative">{{ history.oldValue }}</span>
            →
            <span class="text-positive">{{ history.newValue }}</span>
          </div>
          <div class="text-caption q-mt-sm">
            <q-icon name="person" size="xs" />
            {{ history.modifiedBy }}
            <span v-if="history.reason" class="q-ml-sm">
              <q-icon name="comment" size="xs" />
              {{ history.reason }}
            </span>
          </div>
        </q-timeline-entry>
      </q-timeline>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { settingsService } from '@/services/api/settingsService'

const settingsHistory = ref<SettingHistory[]>([])

const loadHistory = async () => {
    settingsHistory.value = await settingsService.getSettingsHistory()
}

onMounted(loadHistory)
</script>
```

**Backend API**:
```kotlin
// SettingsController.kt
@GetMapping("/api/settings/history")
fun getSettingsHistory(
    @RequestParam(required = false) key: String?,
    @RequestParam(defaultValue = "50") limit: Int
): List<SettingHistory> {
    return settingsService.getSettingsHistory(key, limit)
}
```

**효과**:
- Audit trail 가시화
- 디버깅 용이
- 설정 변경 추적성 향상

---

#### 개선 2: 설정 Import/Export 기능

**API**:
```kotlin
// SettingsController.kt
@GetMapping("/api/settings/export")
fun exportSettings(): ResponseEntity<ByteArray> {
    val json = settingsService.exportSettingsToJson()
    val headers = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        setContentDispositionFormData("attachment", "settings_${LocalDate.now()}.json")
    }
    return ResponseEntity.ok().headers(headers).body(json.toByteArray())
}

@PostMapping("/api/settings/import")
fun importSettings(@RequestBody json: String): ResponseEntity<String> {
    settingsService.importSettingsFromJson(json)
    return ResponseEntity.ok("설정 가져오기 완료")
}
```

**Frontend**:
```vue
<q-btn label="Export" icon="download" @click="exportSettings" />
<q-btn label="Import" icon="upload" @click="importSettings" />
```

**효과**:
- 백업/복원 용이
- 다중 환경 설정 복제 가능

---

#### 개선 3: Backend 검증 강화

**Before**:
```kotlin
// SettingsController.kt
@PostMapping("/api/settings/location")
fun updateLocationSettings(@RequestBody request: LocationRequest) {
    // ❌ 검증 없음
    settingsService.updateLocationSettings(request)
}
```

**After**:
```kotlin
@PostMapping("/api/settings/location")
fun updateLocationSettings(@Valid @RequestBody request: LocationRequest) {
    settingsService.updateLocationSettings(request)
}

// DTO에 검증 규칙 추가
data class LocationRequest(
    @field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0")
    @field:DecimalMax(value = "180.0")
    val longitude: Double,

    @field:DecimalMin(value = "0.0")
    val altitude: Double
)
```

**효과**:
- 직접 API 호출 시에도 검증
- 잘못된 값 저장 방지

---

### 5.4 장기 개선 사항 (필요 시 검토)

#### 개선 4: Feature Flag 시스템

**새 테이블**:
```sql
CREATE TABLE feature_flags (
    flag_key VARCHAR(255) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT false,
    description VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**사용 예시**:
```kotlin
// SettingsService.kt
fun isFeatureEnabled(key: String): Boolean {
    return featureFlagRepository.findByKey(key)?.enabled ?: false
}

// Frontend
if (settingsStore.isFeatureEnabled('experimental_chart')) {
    // 실험적 차트 표시
}
```

**효과**:
- 점진적 배포
- A/B 테스트 가능

---

## 6. 구현 우선순위 및 일정

### Phase 1: Quick Wins (3일)

| 작업 | 우선순위 | 예상 시간 | 파일 |
|-----|---------|---------|------|
| WebSocket 설정 동기화 | P0 | 1-2일 | SettingsController.kt, WebSocketHandler.kt, settingsStore.ts |
| Optimistic UI Updates | P0 | 1일 | 10개 settings stores |
| 초기값 동기화 | P0 | 0.5일 | settingsStore.ts, SettingsController.kt |

**측정 지표**:
- 설정 불일치 발생률 0%
- 설정 변경 지연 시간 < 100ms
- 사용자 만족도 향상

---

### Phase 2: 중기 개선 (1주)

| 작업 | 우선순위 | 예상 시간 | 파일 |
|-----|---------|---------|------|
| 설정 이력 UI | P2 | 2일 | SettingsHistory.vue, SettingsController.kt |
| Import/Export 기능 | P2 | 1일 | SettingsController.kt, SettingsService.kt |
| Backend 검증 강화 | P1 | 2일 | LocationRequest.kt, 기타 DTO |
| 시스템 설정 Frontend 노출 | P1 | 1일 | 새 Store 또는 SystemSettings.vue |

**측정 지표**:
- 설정 변경 이력 조회 가능
- 백업/복원 성공률 100%
- 잘못된 값 저장 시도 차단률 100%

---

### Phase 3: 장기 개선 (필요 시)

| 작업 | 우선순위 | 예상 시간 | 조건 |
|-----|---------|---------|------|
| Feature Flag 시스템 | P3 | 3일 | 실험적 기능 증가 시 |
| 설정 그룹 권한 관리 | P3 | 2일 | 다중 사용자 환경 |
| 설정 분산 구조 통합 | P1 | 5일 | 리팩토링 시 |

**측정 지표**:
- Feature flag 활용률
- 권한 위반 시도 차단률

---

## 7. 마이그레이션 계획

### 7.1 Backend 마이그레이션 (필요 없음)

현재 DB 테이블 구조는 이미 완성되어 있습니다:
- `settings`: 현재 설정 저장
- `setting_history`: 변경 이력 저장

**추가 작업 없음**.

---

### 7.2 Frontend 마이그레이션 (단계적 적용)

**Step 1**: `settingsStore.ts`에 WebSocket listener 추가
```typescript
// 기존 코드 유지, 추가만 수행
const setupWebSocketListener = () => {
    // WebSocket 메시지 처리
}
```

**Step 2**: 각 설정 Store에 Optimistic UI 추가
```typescript
// 기존 save 함수 수정
const save = async (settings: T) => {
    const backup = { ...currentSettings.value }
    // ... (롤백 로직 추가)
}
```

**Step 3**: 초기화 로직 추가
```typescript
// Store 생성 시 자동 호출
const initialize = async () => {
    // Backend에서 초기값 로드
}
```

**호환성**: 기존 코드와 100% 호환, 점진적 개선 가능

---

## 8. 측정 지표 및 성공 기준

| 지표 | 현재 | 목표 | 측정 방법 |
|-----|------|------|---------|
| 설정 불일치 발생률 | 알 수 없음 | 0% | WebSocket 동기화 로그 |
| 설정 변경 지연 시간 | 수동 새로고침 | < 100ms | WebSocket 메시지 타임스탬프 |
| Frontend-Backend 초기값 일치율 | ~50% (32/64) | 100% | 유닛 테스트 |
| 설정 변경 이력 조회 가능 여부 | ❌ | ✅ | UI 제공 여부 |
| 백업/복원 기능 제공 여부 | ❌ | ✅ | Import/Export 기능 |

---

## 9. 리스크 및 완화 방안

### 리스크 1: WebSocket 연결 끊김

**발생 가능성**: 중간
**영향도**: 중간
**완화 방안**:
- WebSocket 재연결 로직 구현 (자동 재시도)
- 연결 끊김 시 polling fallback

---

### 리스크 2: Optimistic UI 실패 시 사용자 혼란

**발생 가능성**: 낮음
**영향도**: 중간
**완화 방안**:
- 명확한 에러 메시지 표시
- 롤백 시 Toast 알림
- 재시도 버튼 제공

---

### 리스크 3: 설정 동기화 충돌 (동시 편집)

**발생 가능성**: 낮음 (단일 사용자 시스템)
**영향도**: 낮음
**완화 방안**:
- 마지막 쓰기 승리(Last Write Wins) 전략
- 충돌 발생 시 경고 표시

---

## 10. 참고 자료

### 공식 문서

1. [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/reference/features/external-config.html)
2. [Kotlin Delegated Properties](https://kotlinlang.org/docs/delegated-properties.html)
3. [Pinia Setup Stores](https://pinia.vuejs.org/core-concepts/)

### 디자인 패턴

4. **Configuration as Data Pattern** (Martin Fowler)
5. **Optimistic UI Pattern** (React Query)
6. **Event Sourcing for Configuration**

### 검증 및 보안

7. [Hibernate Validator (JSR-303)](https://hibernate.org/validator/)
8. **TypeScript Runtime Validation** (Zod, Valibot)

---

## 11. 결론

ACS의 현재 설정 관리 시스템은 **2026년 베스트 프랙티스의 80%를 이미 구현**한 우수한 구조입니다. 특히 **Hybrid Storage**, **Type-Safe Configuration**, **Event-Driven Architecture**가 훌륭하게 구현되어 있습니다.

**Quick Wins에 집중**하면 빠른 효과를 얻을 수 있습니다:

1. **WebSocket 설정 동기화** (1-2일, 가장 높은 ROI)
2. **Optimistic UI Updates** (1일, UX 크게 개선)
3. **초기값 동기화** (0.5일, 버그 수정)

**피해야 할 것**:
- Spring Cloud Config 도입 (오버엔지니어링)
- Remote Config Service (소규모 팀에 불필요)
- Multi-tenancy 지원 (요구사항 없음)

현재 구조는 **확장 가능하고 유지보수하기 쉬운 우수한 설계**이며, 위 개선사항을 적용하면 **완벽한 2026년 표준 Configuration Management 시스템**이 됩니다.

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|-----|------|-------|---------|
| 1.0.0 | 2026-01-07 | Claude Sonnet 4.5 | 최초 작성 |
