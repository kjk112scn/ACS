# Midnight Schedule Handling Plan

본 문서는 `displayMinElevation` 설정 제거 계획에서 분리된 **위성 스케줄 00시 처리 개선**만을 다룹니다. 자정(00:00)을 경계로 끊어지는 패스를 모두 "연속 패스 병합/연장" 방식으로 처리하여, 시작 00시와 종료 00시 모두 이어서 노출하는 전략을 정의합니다.

---

## 1. 자정 경계 패스 병합 - 시작 00시 (재구현 필요)

### 1.1 배경
- Orekit 계산이 자정 직후부터 시작하면, 실제로는 전날에서 이어지는 패스가 잘린 조각만 남음.
- 반대로 자정을 기점으로 새 패스가 시작되기도 하므로 무조건 제거하면 실제 데이터가 사라짐.
- **현재 상태**: `MidnightPassFilter.removeLeadingMidnightPass()`가 단순 제거만 수행하고 있음. 실제 병합 로직 필요.

### 1.2 구현 전략: 효율적인 선택적 보완

#### 핵심 개념
- **기존 2일치 계산 유지**: 오늘 00:00 ~ 내일 00:00 (연산량 동일)
- **선택적 보완 계산**: 첫 번째 패스가 00:00에 시작하는 경우만 짧은 범위로 재계산
- **연산량 증가**: 평균 4~6% (3일치 계산 대비 약 1/10)

#### 구현 방법
1. **기존 2일치 계산 수행** (오늘 00:00 ~ 내일 00:00)
2. **시작 00:00 패스 감지**: 첫 번째 패스가 00:00에 시작하는지 확인
3. **선택적 보완 계산**: 감지된 경우만 전날 설정 시간 전 ~ 오늘 설정 시간 후 범위만 재계산
4. **패스 병합**: 전날 패스와 오늘 첫 패스 병합

#### 설정 기반 시간 범위
- `ephemeris.tracking.midnightSupplement.startBeforeHours`: 전날 계산 시작 시간 (기본값: 1.0시간 = 전날 23:00)
- `ephemeris.tracking.midnightSupplement.startAfterHours`: 오늘 계산 종료 시간 (기본값: 1.0시간 = 오늘 01:00)

### 1.3 구현 상세

#### 1.3.1 SettingsService 설정 추가
```kotlin
// SettingsService.kt의 settingDefinitions에 추가
"ephemeris.tracking.midnightSupplement.startBeforeHours" to SettingDefinition(
    "ephemeris.tracking.midnightSupplement.startBeforeHours", 
    1.0, SettingType.DOUBLE, 
    "자정 경계 패스 보완: 시작 00:00 패스 보완을 위한 전날 계산 시작 시간 (시간)"
),
"ephemeris.tracking.midnightSupplement.startAfterHours" to SettingDefinition(
    "ephemeris.tracking.midnightSupplement.startAfterHours", 
    1.0, SettingType.DOUBLE, 
    "자정 경계 패스 보완: 시작 00:00 패스 보완을 위한 오늘 계산 종료 시간 (시간)"
),
```

#### 1.3.2 보완 함수 구현
```kotlin
/**
 * 시작 00:00 패스 보완
 * 
 * 조건:
 * - 첫 번째 패스가 00:00에 시작하는 경우
 * - 전날 설정 시간 전 ~ 오늘 설정 시간 후 범위만 재계산
 * - 전날 패스와 연속되는 경우 병합
 */
fun OrekitCalculator.supplementStartMidnightPass(
    schedule: SatelliteTrackingSchedule,
    tleLine1: String,
    tleLine2: String,
    minElevation: Float,
    latitude: Double,
    longitude: Double,
    altitude: Double,
    trackingIntervalMs: Int,
    settingsService: SettingsService
): SatelliteTrackingSchedule {
    if (schedule.trackingPasses.isEmpty()) return schedule
    
    val firstPass = schedule.trackingPasses.first()
    val today = schedule.startDate.truncatedTo(ChronoUnit.DAYS)
    val yesterday = today.minusDays(1)
    
    // 시작 00:00 패스인지 확인
    if (firstPass.startTime.truncatedTo(ChronoUnit.MINUTES).hour != 0 ||
        firstPass.startTime.truncatedTo(ChronoUnit.MINUTES).minute != 0) {
        return schedule  // 00:00 시작 패스가 아니면 보완 불필요
    }
    
    // 설정값 가져오기
    val startBeforeHours = settingsService.midnightSupplementStartBeforeHours
    val startAfterHours = settingsService.midnightSupplementStartAfterHours
    
    logger.info("🔍 시작 00:00 패스 감지: ${firstPass.startTime} ~ ${firstPass.endTime}")
    logger.info("   → 전날 ${yesterday.atTime(0, 0).minusHours(startBeforeHours.toLong())} ~ 오늘 ${today.atTime(0, 0).plusHours(startAfterHours.toLong())} 범위 재계산")
    
    // 보완 계산 범위
    val supplementStart = yesterday.atTime(0, 0)
        .minusHours(startBeforeHours.toLong())
        .withZoneSameInstant(ZoneOffset.UTC)
    val supplementEnd = today.atTime(0, 0)
        .plusHours(startAfterHours.toLong())
        .withZoneSameInstant(ZoneOffset.UTC)
    
    // 짧은 범위만 재계산
    val supplementPeriods = detectVisibilityPeriods(
        tleLine1, tleLine2,
        supplementStart, 
        (startBeforeHours + startAfterHours).toInt() / 24 + 1, // 시간을 일수로 변환
        minElevation, latitude, longitude, altitude
    )
    
    // 오늘 00:00 이전에 시작하는 패스 찾기
    val previousPass = supplementPeriods.find { period ->
        period.startTime != null && 
        period.endTime != null &&
        period.endTime!!.truncatedTo(ChronoUnit.MINUTES) == firstPass.startTime.truncatedTo(ChronoUnit.MINUTES)
    }
    
    if (previousPass != null) {
        logger.info("✅ 전날 패스 발견: ${previousPass.startTime} ~ ${previousPass.endTime}")
        
        // 전날 패스의 상세 데이터 생성
        val previousData = generateDetailedTrackingData(
            tleLine1, tleLine2,
            previousPass.startTime!!, previousPass.endTime!!,
            trackingIntervalMs, latitude, longitude, altitude, minElevation
        )
        
        // 첫 번째 패스와 병합
        val mergedPass = firstPass.copy(
            startTime = previousPass.startTime!!,
            trackingData = previousData + firstPass.trackingData
        )
        
        val supplementedPasses = schedule.trackingPasses.toMutableList()
        supplementedPasses[0] = mergedPass
        
        logger.info("✅ 시작 패스 보완 완료: ${mergedPass.startTime} ~ ${mergedPass.endTime}")
        return schedule.copy(trackingPasses = supplementedPasses)
    }
    
    return schedule  // 전날 패스가 없으면 독립 패스로 유지
}
```

### 1.4 테스트 체크리스트
- [ ] 직전 패스와 연속된 00:00 패스는 하나로 노출되는지 확인.
- [ ] 실제로 00:00에 시작하는 독립 패스도 정상 노출되는지 확인.
- [ ] PassSchedule 모달과 Ephemeris SelectSchedule 화면에 동일한 병합 결과가 표시되는지 비교.
- [ ] 설정값 변경 시 보완 범위가 올바르게 적용되는지 확인.

---

## 2. 종료 00시 스케줄 확장 (구현 예정)

### 2.1 문제 정의
- 특정 패스가 00:00에 종료될 경우, 다음날 00:00 이후 구간이 별도의 패스로 끊겨 연속 추적이 어려움.
- 스케줄 목록상에서는 두 개의 패스로 보이지만, 실제로는 하나의 패스가 자정 경계를 지나는 경우가 다수 존재.

### 2.2 구현 전략: 효율적인 선택적 보완

#### 핵심 개념
- **기존 2일치 계산 유지**: 오늘 00:00 ~ 내일 00:00 (연산량 동일)
- **선택적 보완 계산**: 마지막 패스가 00:00에 종료하는 경우만 짧은 범위로 재계산
- **연산량 증가**: 평균 4~6% (3일치 계산 대비 약 1/10)

#### 구현 방법
1. **기존 2일치 계산 수행** (오늘 00:00 ~ 내일 00:00)
2. **종료 00:00 패스 감지**: 마지막 패스가 00:00에 종료하는지 확인
3. **선택적 보완 계산**: 감지된 경우만 오늘 설정 시간 전 ~ 내일 설정 시간 후 범위만 재계산
4. **패스 병합**: 오늘 마지막 패스와 내일 첫 패스 병합

#### 설정 기반 시간 범위
- `ephemeris.tracking.midnightSupplement.endBeforeHours`: 오늘 계산 시작 시간 (기본값: 1.0시간 = 오늘 23:00)
- `ephemeris.tracking.midnightSupplement.endAfterHours`: 내일 계산 종료 시간 (기본값: 1.0시간 = 내일 01:00)

### 2.3 구현 상세

#### 2.3.1 SettingsService 설정 추가
```kotlin
// SettingsService.kt의 settingDefinitions에 추가
"ephemeris.tracking.midnightSupplement.endBeforeHours" to SettingDefinition(
    "ephemeris.tracking.midnightSupplement.endBeforeHours", 
    1.0, SettingType.DOUBLE, 
    "자정 경계 패스 보완: 종료 00:00 패스 보완을 위한 오늘 계산 시작 시간 (시간)"
),
"ephemeris.tracking.midnightSupplement.endAfterHours" to SettingDefinition(
    "ephemeris.tracking.midnightSupplement.endAfterHours", 
    1.0, SettingType.DOUBLE, 
    "자정 경계 패스 보완: 종료 00:00 패스 보완을 위한 내일 계산 종료 시간 (시간)"
),
```

#### 2.3.2 보완 함수 구현
```kotlin
/**
 * 종료 00:00 패스 보완
 * 
 * 조건:
 * - 마지막 패스가 00:00에 종료하는 경우
 * - 오늘 설정 시간 전 ~ 내일 설정 시간 후 범위만 재계산
 * - 오늘 마지막 패스와 내일 첫 패스가 연속되는 경우 병합
 */
fun OrekitCalculator.supplementEndMidnightPass(
    schedule: SatelliteTrackingSchedule,
    tleLine1: String,
    tleLine2: String,
    minElevation: Float,
    latitude: Double,
    longitude: Double,
    altitude: Double,
    trackingIntervalMs: Int,
    settingsService: SettingsService
): SatelliteTrackingSchedule {
    if (schedule.trackingPasses.isEmpty()) return schedule
    
    val lastPass = schedule.trackingPasses.last()
    val today = schedule.startDate.truncatedTo(ChronoUnit.DAYS)
    val tomorrow = today.plusDays(1)
    
    // 종료 00:00 패스인지 확인
    if (lastPass.endTime.truncatedTo(ChronoUnit.MINUTES).hour != 0 ||
        lastPass.endTime.truncatedTo(ChronoUnit.MINUTES).minute != 0) {
        return schedule  // 00:00 종료 패스가 아니면 보완 불필요
    }
    
    // 설정값 가져오기
    val endBeforeHours = settingsService.midnightSupplementEndBeforeHours
    val endAfterHours = settingsService.midnightSupplementEndAfterHours
    
    logger.info("🔍 종료 00:00 패스 감지: ${lastPass.startTime} ~ ${lastPass.endTime}")
    logger.info("   → 오늘 ${today.atTime(0, 0).minusHours(endBeforeHours.toLong())} ~ 내일 ${tomorrow.atTime(0, 0).plusHours(endAfterHours.toLong())} 범위 재계산")
    
    // 보완 계산 범위
    val supplementStart = today.atTime(0, 0)
        .minusHours(endBeforeHours.toLong())
        .withZoneSameInstant(ZoneOffset.UTC)
    val supplementEnd = tomorrow.atTime(0, 0)
        .plusHours(endAfterHours.toLong())
        .withZoneSameInstant(ZoneOffset.UTC)
    
    // 짧은 범위만 재계산
    val supplementPeriods = detectVisibilityPeriods(
        tleLine1, tleLine2,
        supplementStart,
        (endBeforeHours + endAfterHours).toInt() / 24 + 1, // 시간을 일수로 변환
        minElevation, latitude, longitude, altitude
    )
    
    // 내일 00:00 이후에 시작하는 패스 찾기
    val nextPass = supplementPeriods.find { period ->
        period.startTime != null && 
        period.endTime != null &&
        period.startTime!!.truncatedTo(ChronoUnit.MINUTES) == lastPass.endTime.truncatedTo(ChronoUnit.MINUTES)
    }
    
    if (nextPass != null) {
        logger.info("✅ 내일 패스 발견: ${nextPass.startTime} ~ ${nextPass.endTime}")
        
        // 내일 패스의 상세 데이터 생성
        val nextData = generateDetailedTrackingData(
            tleLine1, tleLine2,
            nextPass.startTime!!, nextPass.endTime!!,
            trackingIntervalMs, latitude, longitude, altitude, minElevation
        )
        
        // 마지막 패스와 병합
        val mergedPass = lastPass.copy(
            endTime = nextPass.endTime!!,
            trackingData = lastPass.trackingData + nextData
        )
        
        val supplementedPasses = schedule.trackingPasses.toMutableList()
        supplementedPasses[supplementedPasses.size - 1] = mergedPass
        
        logger.info("✅ 종료 패스 보완 완료: ${mergedPass.startTime} ~ ${mergedPass.endTime}")
        return schedule.copy(trackingPasses = supplementedPasses)
    }
    
    return schedule  // 내일 패스가 없으면 독립 패스로 유지
}
```

### 2.4 통합 함수
```kotlin
/**
 * 자정 경계 패스 보완 통합 함수
 * 
 * 시작 00:00와 종료 00:00 패스를 모두 보완
 */
fun OrekitCalculator.supplementMidnightPasses(
    schedule: SatelliteTrackingSchedule,
    tleLine1: String,
    tleLine2: String,
    minElevation: Float,
    latitude: Double,
    longitude: Double,
    altitude: Double,
    trackingIntervalMs: Int,
    settingsService: SettingsService
): SatelliteTrackingSchedule {
    // 1. 시작 00:00 패스 보완
    val afterStartSupplement = supplementStartMidnightPass(
        schedule, tleLine1, tleLine2, minElevation,
        latitude, longitude, altitude, trackingIntervalMs, settingsService
    )
    
    // 2. 종료 00:00 패스 보완
    return supplementEndMidnightPass(
        afterStartSupplement, tleLine1, tleLine2, minElevation,
        latitude, longitude, altitude, trackingIntervalMs, settingsService
    )
}
```

### 2.5 요구 사항
1. **스케줄 지속성**: 00:00 종료 패스가 바로 다음 패스로 이어지면 자동 병합.
2. **데이터 연속성**: 병합 시 DTL 데이터 및 메타데이터(MaxElevation, Duration 등) 재계산.
3. **UI 일관성**: PassSchedule 및 Ephemeris 화면 모두 병합된 단일 패스를 노출.
4. **로깅 & 트레이싱**: 병합/연장 여부, 병합된 패스 수 등을 로깅.
5. **성능 최적화**: 필요한 경우만 보완 계산 수행 (연산량 증가 최소화).

### 2.6 TODO
- [ ] SettingsService에 자정 경계 보완 설정 추가
- [ ] `supplementStartMidnightPass()` 함수 구현
- [ ] `supplementEndMidnightPass()` 함수 구현
- [ ] `supplementMidnightPasses()` 통합 함수 구현
- [ ] `OrekitCalculator.generateSatelliteTrackingSchedule()`에 보완 로직 통합
- [ ] Processor/Service 단계에서 병합된 데이터로 MaxElevation/Duration 재계산 검증
- [ ] PassScheduleStore UI 시나리오 점검 및 회귀 테스트
- [ ] 설정값 변경 시 동작 검증

---

## 3. 성능 분석

### 3.1 연산량 비교

#### 기존 방법 (3일치 계산)
- 연산량: 3일치 = 72시간
- 증가율: 50% (2일치 대비)

#### 개선 방법 (선택적 보완)
- 기본 연산량: 2일치 = 48시간
- 보완 연산량: 최대 4시간 (시작 2시간 + 종료 2시간)
- 총 연산량: 최대 52시간
- 증가율: 약 8% (2일치 대비)

#### 실제 시나리오
- 대부분의 경우: 시작/종료 중 하나만 보완 필요 → 약 4% 증가
- 최악의 경우: 둘 다 보완 필요 → 약 8% 증가
- 평균: 약 4~6% 증가

### 3.2 효율성
- **3일치 계산 대비**: 약 1/10 연산량
- **필요한 경우만 계산**: 불필요한 연산 최소화
- **설정 기반 조정**: 패스 길이에 따라 계산 범위 조정 가능

---

## 4. 구현 위치

### 4.1 백엔드
- **OrekitCalculator.kt**: 
  - `supplementStartMidnightPass()` 함수 추가
  - `supplementEndMidnightPass()` 함수 추가
  - `supplementMidnightPasses()` 통합 함수 추가
  - `generateSatelliteTrackingSchedule()` 또는 별도 래퍼 함수에서 보완 로직 호출
- **SettingsService.kt**: 
  - 자정 경계 보완 설정 4개 추가
  - 프로퍼티 및 일괄 설정 메서드 추가
- **EphemerisService.kt** / **PassScheduleService.kt**: 
  - 보완 로직 통합된 스케줄 생성 함수 사용
- **SatelliteTrackingProcessor**: 
  - 병합된 패스 데이터로 메타데이터 재계산 보장

### 4.2 프론트엔드
- **passScheduleStore.ts**: 
  - 기본적으로 백엔드 병합 결과를 그대로 사용
  - 과도기/캐시 이슈 시 프론트에서도 "연속 패스 병합" fallback 유틸 제공 (선택사항)
- **Ephemeris UI**: 
  - 연속 패스 병합 시 Progress/Countdown 계산이 다음날까지 연장되므로 타이머 계산 로직 검토

---

## 5. 설정 관리

### 5.1 설정 항목
- `ephemeris.tracking.midnightSupplement.startBeforeHours`: 시작 00:00 패스 전날 계산 시작 시간 (기본값: 1.0시간)
- `ephemeris.tracking.midnightSupplement.startAfterHours`: 시작 00:00 패스 오늘 계산 종료 시간 (기본값: 1.0시간)
- `ephemeris.tracking.midnightSupplement.endBeforeHours`: 종료 00:00 패스 오늘 계산 시작 시간 (기본값: 1.0시간)
- `ephemeris.tracking.midnightSupplement.endAfterHours`: 종료 00:00 패스 내일 계산 종료 시간 (기본값: 1.0시간)

### 5.2 설정 사용 예시
```kotlin
// 기본값 (1시간 전후)
// 전날 23:00 ~ 오늘 01:00, 오늘 23:00 ~ 내일 01:00

// 긴 패스 대응 (2시간 전후)
settingsService.setMidnightSupplementSettings(
    startBeforeHours = 2.0,
    startAfterHours = 2.0,
    endBeforeHours = 2.0,
    endAfterHours = 2.0
)

// 짧은 패스 최적화 (30분 전후)
settingsService.setMidnightSupplementSettings(
    startBeforeHours = 0.5,
    startAfterHours = 0.5,
    endBeforeHours = 0.5,
    endAfterHours = 0.5
)
```

---

## 참고
- DisplayMinElevation 설정 제거 계획: `docs/plans/Remove_DisplayMinElevationAngle.md`
- 관련 백엔드 파일:
  - `src/main/kotlin/com/gtlsystems/acs_api/algorithm/satellitetracker/impl/OrekitCalculator.kt`
  - `src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt`
  - `src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
  - `src/main/kotlin/com/gtlsystems/acs_api/service/system/settings/SettingsService.kt`
- 관련 프론트엔드 파일:
  - `src/stores/mode/passScheduleStore.ts`
  - `src/pages/mode/EphemerisDesignationPage.vue`
