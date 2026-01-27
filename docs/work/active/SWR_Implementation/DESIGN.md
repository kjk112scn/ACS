# SWR 패턴 구현 가이드

> **목적**: 추후 한번에 적용할 수 있도록 상세 구현 방법 정리
> **작성일**: 2026-01-27

---

## 목차

1. [Phase 1: HTTP 캐시 헤더 (Backend)](#phase-1-http-캐시-헤더-backend)
2. [Phase 2: SWRv 라이브러리 (Frontend)](#phase-2-swrv-라이브러리-frontend)
3. [Phase 3: Spring @Cacheable (Backend)](#phase-3-spring-cacheable-backend)
4. [Phase 4: 캐시 무효화 전략](#phase-4-캐시-무효화-전략)
5. [API별 캐싱 전략 상세](#api별-캐싱-전략-상세)
6. [테스트 계획](#테스트-계획)

---

## Phase 1: HTTP 캐시 헤더 (Backend)

### 1.1 CacheControl 유틸 클래스 생성

**파일**: `backend/src/main/kotlin/com/example/acs/config/CacheConfig.kt`

```kotlin
package com.example.acs.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfig {
    companion object {
        // 정적 데이터: 1시간 캐시
        val STATIC_CACHE: CacheControl = CacheControl
            .maxAge(1, TimeUnit.HOURS)
            .cachePublic()

        // 준정적 데이터: 5분 캐시 + must-revalidate
        val SEMI_STATIC_CACHE: CacheControl = CacheControl
            .maxAge(5, TimeUnit.MINUTES)
            .mustRevalidate()

        // 짧은 캐시: 1분
        val SHORT_CACHE: CacheControl = CacheControl
            .maxAge(1, TimeUnit.MINUTES)
            .cachePrivate()

        // 캐시 금지
        val NO_CACHE: CacheControl = CacheControl
            .noStore()
            .noCache()
    }
}
```

### 1.2 SettingsController 수정

**파일**: `backend/src/main/kotlin/com/example/acs/controller/SettingsController.kt`

```kotlin
import com.example.acs.config.CacheConfig
import org.springframework.http.ResponseEntity

@GetMapping("/location")
fun getLocation(): ResponseEntity<Map<String, Any>> {
    val data = settingsService.getLocationSettings()
    val etag = generateETag(data)

    return ResponseEntity.ok()
        .cacheControl(CacheConfig.STATIC_CACHE)
        .eTag(etag)
        .body(data)
}

@GetMapping("/antennaspec")
fun getAntennaSpec(): ResponseEntity<Map<String, Any>> {
    val data = settingsService.getAntennaSpecSettings()

    // 안테나 사양은 거의 변경되지 않음: 24시간 캐시
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(24, TimeUnit.HOURS).cachePublic())
        .body(data)
}

// ETag 생성 유틸
private fun generateETag(data: Any): String {
    val hash = data.hashCode().toString(16)
    return "\"$hash\""
}
```

### 1.3 조건부 요청 처리

**파일**: Controller에서 조건부 요청 처리

```kotlin
import org.springframework.web.server.ServerWebExchange

@GetMapping("/tracking")
suspend fun getTracking(exchange: ServerWebExchange): ResponseEntity<Map<String, Any>> {
    val data = settingsService.getTrackingSettings()
    val etag = generateETag(data)

    // 조건부 요청 확인
    if (exchange.checkNotModified(etag)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build()
    }

    return ResponseEntity.ok()
        .cacheControl(CacheConfig.STATIC_CACHE)
        .eTag(etag)
        .body(data)
}
```

### 1.4 Controller별 캐시 설정 요약

| Controller | 엔드포인트 | 캐시 전략 | TTL |
|------------|-----------|----------|-----|
| SettingsController | GET /location | STATIC_CACHE + ETag | 1시간 |
| SettingsController | GET /antennaspec | STATIC_CACHE | 24시간 |
| SettingsController | GET /tracking | STATIC_CACHE + ETag | 1시간 |
| PassScheduleController | GET /tle | SEMI_STATIC_CACHE | 5분 |
| PassScheduleController | GET /tle/{id} | STATIC_CACHE | 12시간 |
| PassScheduleController | GET /tracking/master | SEMI_STATIC_CACHE | 5분 |
| EphemerisController | GET /tracking/mst/merged | SEMI_STATIC_CACHE | 5분 |
| ICDController | GET /firmware-version | STATIC_CACHE | 24시간 |
| PerformanceController | GET /metrics | SHORT_CACHE | 1분 |
| LoggingController | GET /statistics | SHORT_CACHE | 1분 |

---

## Phase 2: SWRv 라이브러리 (Frontend)

### 2.1 패키지 설치

```bash
cd frontend
npm install swrv
```

**package.json 변경**:
```json
{
  "dependencies": {
    "swrv": "^1.0.4"
  }
}
```

### 2.2 SWRv 기본 설정

**파일**: `frontend/src/boot/swrv.ts` (신규)

```typescript
import { IConfig } from 'swrv'

// 전역 SWRv 설정
export const swrvConfig: IConfig = {
  // 포커스 시 재검증 비활성화 (안테나 제어 앱에서는 불필요)
  revalidateOnFocus: false,

  // 재연결 시 재검증
  revalidateOnReconnect: true,

  // 중복 요청 방지 간격 (60초)
  dedupingInterval: 60000,

  // 에러 재시도 횟수
  errorRetryCount: 3,

  // 에러 재시도 간격 (ms)
  errorRetryInterval: 5000,

  // 캐시 TTL (5분)
  ttl: 5 * 60 * 1000,
}

// 정적 데이터용 설정 (Settings 등)
export const staticConfig: IConfig = {
  ...swrvConfig,
  dedupingInterval: 300000,  // 5분
  ttl: 60 * 60 * 1000,       // 1시간
  revalidateOnFocus: false,
  revalidateOnReconnect: false,
}

// 준정적 데이터용 설정 (TLE, Master Data)
export const semiStaticConfig: IConfig = {
  ...swrvConfig,
  dedupingInterval: 60000,   // 1분
  ttl: 5 * 60 * 1000,        // 5분
}
```

### 2.3 Settings Composable 생성

**파일**: `frontend/src/composables/useSettings.ts` (신규)

```typescript
import useSWRV from 'swrv'
import { computed, Ref } from 'vue'
import api from '@/boot/axios'
import { staticConfig } from '@/boot/swrv'

// 타입 정의
interface LocationSettings {
  latitude: number
  longitude: number
  altitude: number
}

interface TrackingSettings {
  interval: number
  duration: number
  minElevation: number
}

// API fetcher
const fetcher = async <T>(url: string): Promise<T> => {
  const response = await api.get<T>(url)
  return response.data
}

// Location Settings
export function useLocationSettings() {
  const { data, error, isValidating, mutate } = useSWRV<LocationSettings>(
    '/api/settings/location',
    fetcher,
    staticConfig
  )

  return {
    locationSettings: data,
    error,
    isLoading: isValidating,
    refresh: mutate,
  }
}

// Tracking Settings
export function useTrackingSettings() {
  const { data, error, isValidating, mutate } = useSWRV<TrackingSettings>(
    '/api/settings/tracking',
    fetcher,
    staticConfig
  )

  return {
    trackingSettings: data,
    error,
    isLoading: isValidating,
    refresh: mutate,
  }
}

// All Settings (통합)
export function useAllSettings() {
  const { data, error, isValidating, mutate } = useSWRV<Record<string, any>>(
    '/api/settings',
    fetcher,
    staticConfig
  )

  return {
    settings: data,
    error,
    isLoading: isValidating,
    refresh: mutate,
  }
}

// Settings 변경 후 캐시 무효화
export function invalidateSettingsCache() {
  // SWRv는 같은 키로 mutate 호출 시 캐시 무효화
  const { mutate } = useSWRV('/api/settings', null)
  mutate()
}
```

### 2.4 TLE Composable 생성

**파일**: `frontend/src/composables/useTle.ts` (신규)

```typescript
import useSWRV from 'swrv'
import api from '@/boot/axios'
import { semiStaticConfig } from '@/boot/swrv'

interface TleData {
  satelliteId: string
  satelliteName: string
  line1: string
  line2: string
  epoch: string
}

const fetcher = async <T>(url: string): Promise<T> => {
  const response = await api.get<{ data: T }>(url)
  return response.data.data
}

// 전체 TLE 목록
export function useAllTles() {
  const { data, error, isValidating, mutate } = useSWRV<TleData[]>(
    '/api/pass-schedule/tle',
    fetcher,
    semiStaticConfig
  )

  return {
    tles: data,
    error,
    isLoading: isValidating,
    refresh: mutate,
  }
}

// 특정 위성 TLE
export function useTle(satelliteId: Ref<string | null>) {
  const key = computed(() =>
    satelliteId.value ? `/api/pass-schedule/tle/${satelliteId.value}` : null
  )

  const { data, error, isValidating, mutate } = useSWRV<TleData>(
    key,
    fetcher,
    {
      ...semiStaticConfig,
      ttl: 12 * 60 * 60 * 1000,  // 12시간 (TLE는 변경 빈도 낮음)
    }
  )

  return {
    tle: data,
    error,
    isLoading: isValidating,
    refresh: mutate,
  }
}

// TLE 캐시 상태
export function useTleCacheStatus() {
  const { data, error, isValidating } = useSWRV(
    '/api/pass-schedule/status',
    fetcher,
    semiStaticConfig
  )

  return {
    cacheStatus: data,
    error,
    isLoading: isValidating,
  }
}
```

### 2.5 Tracking Data Composable

**파일**: `frontend/src/composables/useTrackingData.ts` (신규)

```typescript
import useSWRV from 'swrv'
import { computed, Ref } from 'vue'
import api from '@/boot/axios'
import { semiStaticConfig } from '@/boot/swrv'

interface TrackingMasterData {
  mstId: string
  satelliteId: string
  satelliteName: string
  passCount: number
  generatedAt: string
}

interface TrackingDetailData {
  time: number
  azimuth: number
  elevation: number
  train: number
}

const fetcher = async <T>(url: string): Promise<T> => {
  const response = await api.get<{ data: T }>(url)
  return response.data.data ?? response.data
}

// 전체 마스터 데이터
export function useTrackingMasterData() {
  const { data, error, isValidating, mutate } = useSWRV<TrackingMasterData[]>(
    '/api/pass-schedule/tracking/master',
    fetcher,
    semiStaticConfig
  )

  return {
    masterData: data,
    error,
    isLoading: isValidating,
    refresh: mutate,
  }
}

// 특정 패스 상세 데이터
export function useTrackingDetailData(
  mstId: Ref<string | null>,
  detailId: Ref<string | null>
) {
  const key = computed(() => {
    if (!mstId.value || !detailId.value) return null
    return `/api/pass-schedule/tracking/detail/${mstId.value}/pass/${detailId.value}`
  })

  const { data, error, isValidating, mutate } = useSWRV<TrackingDetailData[]>(
    key,
    fetcher,
    {
      ...semiStaticConfig,
      ttl: 24 * 60 * 60 * 1000,  // 24시간 (세부 데이터는 정적)
    }
  )

  return {
    detailData: data,
    error,
    isLoading: isValidating,
    refresh: mutate,
  }
}
```

### 2.6 기존 Service 개선 (settingsService.ts)

**파일**: `frontend/src/services/api/settingsService.ts`

```typescript
// 기존 코드 유지하면서 SWRv Composable 추가 export

// === 기존 함수 (하위 호환) ===
export async function getLocationSettings(): Promise<LocationSettings> {
  // 기존 구현 유지
}

// === SWRv Composable (권장) ===
export { useLocationSettings, useTrackingSettings, useAllSettings } from '@/composables/useSettings'

// === 캐시 무효화 유틸 ===
import { mutate } from 'swrv'

export function invalidateAllSettingsCache() {
  // 모든 settings 관련 캐시 무효화
  const settingsKeys = [
    '/api/settings',
    '/api/settings/location',
    '/api/settings/tracking',
    '/api/settings/stow/all',
    '/api/settings/antennaspec',
    '/api/settings/anglelimits',
    '/api/settings/speedlimits',
  ]

  settingsKeys.forEach(key => mutate(key))
}

// POST 요청 후 캐시 무효화 패턴
export async function setLocationSettings(data: LocationSettings): Promise<void> {
  await api.post('/api/settings/location', data)

  // 캐시 무효화
  mutate('/api/settings/location')
  mutate('/api/settings')
}
```

### 2.7 컴포넌트에서 사용 예시

**파일**: 페이지/컴포넌트에서 사용

```vue
<script setup lang="ts">
import { useLocationSettings, useTrackingSettings } from '@/composables/useSettings'

// SWRv Composable 사용
const { locationSettings, isLoading: locationLoading, refresh: refreshLocation } = useLocationSettings()
const { trackingSettings, isLoading: trackingLoading } = useTrackingSettings()

// 수동 새로고침
const handleRefresh = () => {
  refreshLocation()
}
</script>

<template>
  <div>
    <q-spinner v-if="locationLoading" />
    <div v-else>
      위도: {{ locationSettings?.latitude }}
      경도: {{ locationSettings?.longitude }}
    </div>
    <q-btn @click="handleRefresh">새로고침</q-btn>
  </div>
</template>
```

---

## Phase 3: Spring @Cacheable (Backend)

### 3.1 Redis 의존성 추가 (선택사항)

**파일**: `backend/build.gradle.kts`

```kotlin
dependencies {
    // 인메모리 캐시 (기본)
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Redis 캐시 (선택 - 분산 환경)
    // implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
}
```

### 3.2 캐시 설정

**파일**: `backend/src/main/kotlin/com/example/acs/config/CacheConfig.kt`

```kotlin
package com.example.acs.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()

        // 캐시별 설정
        cacheManager.registerCustomCache("settings",
            Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100)
                .build()
        )

        cacheManager.registerCustomCache("tle",
            Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(500)
                .build()
        )

        cacheManager.registerCustomCache("tracking-master",
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build()
        )

        return cacheManager
    }
}
```

### 3.3 Service에 @Cacheable 적용

**파일**: `backend/src/main/kotlin/com/example/acs/service/SettingsService.kt`

```kotlin
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut

@Service
class SettingsService(
    private val settingsRepository: SettingsRepository
) {

    @Cacheable(value = ["settings"], key = "'location'")
    fun getLocationSettings(): Map<String, Any> {
        // DB 조회 로직
        return mapOf(
            "latitude" to settings.latitude,
            "longitude" to settings.longitude,
            "altitude" to settings.altitude
        )
    }

    @CacheEvict(value = ["settings"], key = "'location'")
    fun setLocationSettings(data: Map<String, Any>) {
        // DB 저장 로직
    }

    // 전체 캐시 무효화
    @CacheEvict(value = ["settings"], allEntries = true)
    fun clearAllSettingsCache() {
        // 캐시만 지움
    }
}
```

### 3.4 WebFlux에서 Reactive 캐싱

**주의**: Spring WebFlux에서 `@Cacheable`은 `Mono`/`Flux`를 직접 캐싱하지 않음

**해결책 1**: `.cache()` 연산자 사용

```kotlin
@Service
class EphemerisService {

    private val trackingDataCache = ConcurrentHashMap<String, Mono<TrackingData>>()

    fun getTrackingData(mstId: String): Mono<TrackingData> {
        return trackingDataCache.computeIfAbsent(mstId) { key ->
            fetchTrackingData(key)
                .cache(Duration.ofMinutes(5))  // Mono 레벨 캐싱
        }
    }

    fun invalidateCache(mstId: String) {
        trackingDataCache.remove(mstId)
    }
}
```

**해결책 2**: Caffeine AsyncCache

```kotlin
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine

@Service
class TleService {

    private val tleCache: AsyncCache<String, TleData> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(12))
        .maximumSize(500)
        .buildAsync()

    fun getTle(satelliteId: String): Mono<TleData> {
        return Mono.fromFuture(
            tleCache.get(satelliteId) { key ->
                repository.findById(key)
                    .toFuture()
            }
        )
    }
}
```

---

## Phase 4: 캐시 무효화 전략

### 4.1 Event 기반 무효화 (Backend)

**파일**: `backend/src/main/kotlin/com/example/acs/event/CacheInvalidationEvent.kt`

```kotlin
package com.example.acs.event

import org.springframework.context.ApplicationEvent

data class CacheInvalidationEvent(
    val source: Any,
    val cacheNames: List<String>,
    val keys: List<String>? = null  // null이면 전체 무효화
) : ApplicationEvent(source)

// 이벤트 리스너
@Component
class CacheInvalidationListener(
    private val cacheManager: CacheManager
) {

    @EventListener
    fun handleCacheInvalidation(event: CacheInvalidationEvent) {
        event.cacheNames.forEach { cacheName ->
            val cache = cacheManager.getCache(cacheName)
            if (event.keys == null) {
                cache?.clear()
            } else {
                event.keys.forEach { key ->
                    cache?.evict(key)
                }
            }
        }
    }
}
```

### 4.2 Settings 변경 시 캐시 무효화

```kotlin
@Service
class SettingsService(
    private val eventPublisher: ApplicationEventPublisher
) {

    fun setLocationSettings(data: Map<String, Any>) {
        // 1. DB 저장
        repository.save(data)

        // 2. 캐시 무효화 이벤트 발행
        eventPublisher.publishEvent(
            CacheInvalidationEvent(
                source = this,
                cacheNames = listOf("settings"),
                keys = listOf("location", "all")
            )
        )

        // 3. 설정 변경 이벤트 (기존)
        eventPublisher.publishEvent(SettingsChangedEvent(this, "location"))
    }
}
```

### 4.3 Frontend 캐시 무효화 (WebSocket 연동)

**파일**: `frontend/src/services/cacheInvalidation.ts` (신규)

```typescript
import { mutate } from 'swrv'
import { useWebSocket } from '@/services/api/icdService'

// WebSocket 메시지 기반 캐시 무효화
export function setupCacheInvalidation() {
  const ws = useWebSocket()

  ws.subscribe('cache-invalidation', (message: CacheInvalidationMessage) => {
    const { cacheNames, keys } = message

    // 캐시 이름 → URL 매핑
    const cacheKeyMap: Record<string, string[]> = {
      'settings': [
        '/api/settings',
        '/api/settings/location',
        '/api/settings/tracking',
        // ...
      ],
      'tle': [
        '/api/pass-schedule/tle',
      ],
      'tracking-master': [
        '/api/pass-schedule/tracking/master',
      ],
    }

    cacheNames.forEach(cacheName => {
      const urls = cacheKeyMap[cacheName] || []
      urls.forEach(url => mutate(url))
    })
  })
}
```

### 4.4 수동 새로고침 UI

```vue
<template>
  <q-btn
    flat
    round
    icon="refresh"
    :loading="isRefreshing"
    @click="handleRefresh"
  >
    <q-tooltip>데이터 새로고침</q-tooltip>
  </q-btn>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { invalidateAllSettingsCache } from '@/services/api/settingsService'

const isRefreshing = ref(false)

const handleRefresh = async () => {
  isRefreshing.value = true
  try {
    await invalidateAllSettingsCache()
  } finally {
    isRefreshing.value = false
  }
}
</script>
```

---

## API별 캐싱 전략 상세

### Settings API

| 엔드포인트 | FE 캐시 (SWRv) | BE 캐시 | HTTP 헤더 | 무효화 조건 |
|-----------|---------------|---------|----------|------------|
| GET /location | 1시간, dedup 5분 | @Cacheable 1시간 | max-age=3600, ETag | POST /location |
| GET /tracking | 1시간, dedup 5분 | @Cacheable 1시간 | max-age=3600, ETag | POST /tracking |
| GET /antennaspec | 24시간 | @Cacheable 24시간 | max-age=86400 | POST /antennaspec |
| GET /stow/all | 1시간 | @Cacheable 1시간 | max-age=3600 | POST /stow/* |

### PassSchedule API

| 엔드포인트 | FE 캐시 (SWRv) | BE 캐시 | HTTP 헤더 | 무효화 조건 |
|-----------|---------------|---------|----------|------------|
| GET /tle | 5분, dedup 1분 | AsyncCache 5분 | max-age=300 | POST/PUT/DELETE /tle |
| GET /tle/{id} | 12시간 | AsyncCache 12시간 | max-age=43200 | PUT/DELETE /tle/{id} |
| GET /tracking/master | 5분 | Mono.cache() 5분 | max-age=300, must-revalidate | generate 완료 |
| GET /tracking/detail/{..} | 24시간 | AsyncCache 24시간 | max-age=86400 | 거의 없음 (정적) |

### Ephemeris API

| 엔드포인트 | FE 캐시 (SWRv) | BE 캐시 | HTTP 헤더 | 무효화 조건 |
|-----------|---------------|---------|----------|------------|
| GET /tracking/mst/merged | 5분 | Mono.cache() 5분 | max-age=300 | generate 완료 |
| GET /tracking/realtime-data | ❌ 캐시 없음 | ❌ 없음 | no-store | N/A (실시간) |
| GET /detail/{mstId}/pass/{..} | 24시간 | AsyncCache 24시간 | max-age=86400 | DELETE /{mstId} |

### 캐시 불가 API (모두 POST)

```
POST /icd/*                     # 하드웨어 제어 명령
POST /sun-track/start           # 추적 시작
POST /ephemeris/tracking/start  # 추적 시작
POST /ephemeris/tracking/generate  # 계산 작업
WebSocket                       # 실시간 양방향
```

---

## 테스트 계획

### 단위 테스트

```kotlin
// Backend - CacheConfig 테스트
@SpringBootTest
class CacheConfigTest {

    @Autowired
    lateinit var settingsService: SettingsService

    @Test
    fun `settings should be cached`() {
        // 첫 번째 호출
        val result1 = settingsService.getLocationSettings()

        // 두 번째 호출 (캐시 히트)
        val result2 = settingsService.getLocationSettings()

        // DB 호출은 1번만 발생해야 함
        verify(repository, times(1)).findByKey("location")
    }

    @Test
    fun `cache should be invalidated on update`() {
        settingsService.getLocationSettings()
        settingsService.setLocationSettings(mapOf("latitude" to 37.5))
        settingsService.getLocationSettings()

        // DB 호출이 2번 발생해야 함 (캐시 무효화됨)
        verify(repository, times(2)).findByKey("location")
    }
}
```

### 통합 테스트

```typescript
// Frontend - SWRv Composable 테스트
import { renderHook, waitFor } from '@testing-library/vue'
import { useLocationSettings } from '@/composables/useSettings'

describe('useLocationSettings', () => {
  it('should cache data', async () => {
    const { result: result1 } = renderHook(() => useLocationSettings())
    await waitFor(() => expect(result1.current.locationSettings).toBeDefined())

    // API 호출은 1번만 발생
    expect(mockAxios.get).toHaveBeenCalledTimes(1)

    const { result: result2 } = renderHook(() => useLocationSettings())

    // 두 번째 훅에서는 캐시된 데이터 반환
    expect(result2.current.locationSettings).toEqual(result1.current.locationSettings)
    expect(mockAxios.get).toHaveBeenCalledTimes(1)  // 여전히 1번
  })
})
```

### 성능 테스트 체크리스트

- [ ] Settings 페이지 초기 로드 시간 측정
- [ ] 페이지 전환 시 네트워크 요청 수 확인
- [ ] 캐시 히트율 모니터링 (Network 탭)
- [ ] 메모리 사용량 변화 확인
- [ ] 캐시 무효화 후 데이터 일관성 확인

---

## 구현 순서 권장

```
1. [BE] CacheConfig.kt 생성 (HTTP 캐시 헤더 유틸)
2. [BE] SettingsController에 Cache-Control 헤더 추가
3. [FE] swrv 패키지 설치
4. [FE] swrv.ts 설정 파일 생성
5. [FE] useSettings.ts Composable 생성
6. [FE] 1개 컴포넌트에서 테스트 (SettingsModal)
7. [BE] 나머지 Controller에 캐시 헤더 추가
8. [FE] useTle.ts, useTrackingData.ts Composable 생성
9. [BE] @Cacheable 적용 (SettingsService)
10. [FE+BE] 캐시 무효화 전략 구현
11. 전체 테스트 및 성능 측정
```

---

## 참고 자료

- [SWRv 공식 문서](https://docs-swrv.netlify.app/)
- [Kong/swrv GitHub](https://github.com/Kong/swrv)
- [Vue Mastery - SWR and Vue.js](https://www.vuemastery.com/blog/data-fetching-and-caching-with-swr-and-vuejs/)
- [Spring Framework HTTP Caching](https://docs.spring.io/spring-framework/reference/web/webflux/caching.html)
- [Baeldung - Spring WebFlux @Cacheable](https://www.baeldung.com/spring-webflux-cacheable)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
