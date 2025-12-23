# Midnight Schedule Handling Plan

본 문서는 `displayMinElevation` 설정 제거 계획에서 분리된 **위성 스케줄 00시 처리 개선**만을 다룹니다. 자정(00:00)을 경계로 끊어지는 패스를 모두 "연속 패스 병합/연장" 방식으로 처리하여, 시작 00시와 종료 00시 모두 이어서 노출하는 전략을 정의합니다.

---

## 🚨 중요 수정 사항 (2025-11-20)

### 발견된 문제
초기 구현에서 **보완 범위 제한 로직**으로 인해 다음 문제가 발생했습니다:

1. **패스 일부만 재생성**: ElevationDetector가 찾은 완전한 패스(예: 23:50~00:15)를 보완 범위(23:00~00:00)로 제한하여 일부(23:50~00:00)만 생성
2. **데이터 폭증**: connectingPass가 긴 경우(예: 2시간) 불필요하게 긴 구간 재계산 → 800,000개 이상의 데이터 포인트 생성
3. **의도 불일치**: "00:00 걸친 패스 전체 복원"이 아닌 "일부만 복원"

### 적용된 해결책
**ElevationDetector가 찾은 완전한 패스를 그대로 재생성**하도록 수정:

```kotlin
// ❌ 이전: 보완 범위로 제한
val actualStart = if (connectingStart.isBefore(supplementStart)) supplementStart else connectingStart
val actualEnd = if (connectingEnd.isAfter(firstPass.endTime)) firstPass.endTime else connectingEnd

// ✅ 수정: 패스 전체 재생성
val mergedData = generateDetailedTrackingData(
    startTime = connectingStart,  // 패스 전체 시작
    endTime = connectingEnd,      // 패스 전체 종료
    // ...
)
```

### 기대 효과
- ✅ 정확한 패스 복원: 23:50~00:15 전체 패스 재생성
- ✅ 데이터량 정상화: 60,000개 수준으로 복귀 (13배 감소)
- ✅ 처리 시간 개선: 30초 이내로 복귀
- ✅ 의도대로 동작: "00:00 걸친 패스 전체 복원"

---

## 1. 자정 경계 패스 병합 - 시작 00시 (재구현 필요)

### 1.1 배경
- Orekit 계산이 자정 직후부터 시작하면, 실제로는 전날에서 이어지는 패스가 잘린 조각만 남음.
- 반대로 자정을 기점으로 새 패스가 시작되기도 하므로 무조건 제거하면 실제 데이터가 사라짐.
- **현재 상태**: `MidnightPassFilter.removeLeadingMidnightPass()`가 단순 제거만 수행하고 있음. 실제 병합 로직 필요.

### 1.2 구현 전략: 효율적인 선택적 보완

#### 핵심 개념
- **기존 2일치 계산 유지**: 오늘 00:00 ~ 모레 00:00 (연산량 동일)
- **선택적 보완 계산**: 첫 번째 패스가 00:00에 시작하는 경우만 짧은 범위로 재계산
- **연산량 증가**: 평균 2~3% (3일치 계산 대비 약 1/20)

#### 구현 방법
1. **기존 2일치 계산 수행** (오늘 00:00 ~ 모레 00:00)
2. **시작 00:00 패스 감지**: 첫 번째 패스가 정확히 00:00:00에 시작하는지 확인 (초/분 단위)
3. **선택적 보완 계산**: 감지된 경우만 전날 설정 시간 전 ~ 오늘 00:00 범위를 재계산 (전날 23:00 ~ 오늘 00:00, 기본 1시간)
4. **보완 범위 내 패스 필터링**: 보완 계산 결과를 보완 범위 내로 필터링
5. **00:00에 걸쳐있는 스케줄 찾기**: 보완 범위 내 결과에서 오늘 00:00에 걸쳐있는 스케줄 찾기 (전날 시작, 오늘 00:00 이후 종료)
6. **00시에 가장 가까운 패스 1개만 선택**: 보완 범위 내에 여러 패스가 있을 때, 오늘 00:00에 가장 가까운 패스 1개만 선택하고 나머지는 무시
7. **패스 병합**: 선택된 보완 패스 전체 데이터를 재생성하여 기존 스케줄과 병합
8. **보완 실패 처리**: 00:00에 걸쳐있는 패스를 찾지 못하면 원래 00:00 패스 삭제
9. **에러 처리**: 보완 계산 중 예외 발생 시 원래 00:00 패스 삭제

#### 설정 기반 시간 범위
- `ephemeris.tracking.midnightSupplement.startBeforeHours`: 전날 계산 시작 시간 (기본값: 1.0시간 = 전날 23:00)
- **보완 범위**: 전날 23:00 ~ 오늘 00:00 (1시간만 스캔)

### 1.3 구현 상세

#### 1.3.1 SettingsService 설정 추가
```kotlin
// SettingsService.kt의 settingDefinitions에 추가
/**
 * 자정 경계 패스 보완: 시작 00:00 패스 전날 계산 시작 시간 (시간)
 *
 * ## 용도
 * - 전날 (00:00 - startBeforeHours) ~ 오늘 00:00 범위를 재계산하여 00:00 직전 패스를 복구합니다.
 *
 * ## 기본값
 * - 1.0시간 (전날 23:00 ~ 오늘 00:00 스캔)
 *
 * ## 최소값
 * - 0.1시간 (6분) 이상만 허용. 0.0으로 설정하면 보완 범위가 0이 되어 의미가 없어집니다.
 */
"ephemeris.tracking.midnightSupplement.startBeforeHours" to SettingDefinition(
    "ephemeris.tracking.midnightSupplement.startBeforeHours", 
    1.0, SettingType.DOUBLE, 
    "자정 경계 패스 보완: 시작 00:00 패스 보완을 위한 전날 계산 시작 시간 (시간)"
),
```

#### 1.3.2 SettingsService 프로퍼티 및 일괄 설정 메서드
```kotlin
/**
 * 자정 경계 패스 보완: 시작 00:00 패스 전날 계산 시작 시간 (시간)
 *
 * 전날 (00:00 - startBeforeHours) ~ 오늘 00:00 범위를 재계산하여 00:00 직전 패스를 복구합니다.
 * - 기본값: 1.0시간 (전날 23:00 ~ 오늘 00:00)
 * - 최소값: 0.1시간 (6분) 이상
 * - 최대값: 24.0시간
 */
var midnightSupplementStartBeforeHours: Double by createSettingProperty(
    "ephemeris.tracking.midnightSupplement.startBeforeHours",
    "자정 경계 패스 보완: 시작 00:00 패스 전날 계산 시작 시간"
)

/**
 * 자정 경계 패스 보완: 종료 00:00 패스 모레 계산 종료 시간 (시간)
 *
 * 모레 00:00 ~ (모레 00:00 + endAfterHours) 범위를 재계산하여 00:00 직후 패스를 복구합니다.
 * - 기본값: 1.0시간 (모레 00:00 ~ 모레 01:00)
 * - 최소값: 0.1시간 (6분) 이상
 * - 최대값: 24.0시간
 */
var midnightSupplementEndAfterHours: Double by createSettingProperty(
    "ephemeris.tracking.midnightSupplement.endAfterHours",
    "자정 경계 패스 보완: 종료 00:00 패스 모레 계산 종료 시간"
)

/**
 * 자정 경계 패스 보완 설정을 동시에 변경합니다.
 *
 * @param startBeforeHours 전날 계산 시작 시간 (0.1~24.0시간)
 * @param endAfterHours 모레 계산 종료 시간 (0.1~24.0시간)
 */
fun setMidnightSupplementSettings(startBeforeHours: Double, endAfterHours: Double) {
    setMultipleSettings(
        "ephemeris.tracking.midnightSupplement.startBeforeHours" to startBeforeHours.coerceIn(0.1, 24.0),
        "ephemeris.tracking.midnightSupplement.endAfterHours" to endAfterHours.coerceIn(0.1, 24.0)
    )
}

/**
 * 자정 경계 패스 보완 설정 그룹을 조회합니다.
 *
 * @return ephemeris.tracking.midnightSupplement.* 설정 맵
 */
fun getMidnightSupplementSettings(): Map<String, Any> =
    settings.filterKeys { it.startsWith("ephemeris.tracking.midnightSupplement.") }
```

#### 1.3.3 보완 함수 구현
```kotlin
/**
 * 시작 00:00 패스를 전날 패스와 병합해 연속성을 복원합니다.
 *
 * ## 동작 순서
 * 1. 첫 번째 패스가 정확히 00:00:00에 시작하는지 (초 단위) 확인
 * 2. 전날 (00:00 - startBeforeHours) ~ 오늘 00:00 범위를 재계산 (기본 1시간)
 * 3. 보완 범위 내 패스 중 00:00을 가로지르는 패스만 추출
 * 4. 00:00에 가장 가까운 단일 패스를 선택 후 기존 패스와 병합
 * 5. 보완 실패 또는 예외 발생 시 원래 00:00 패스를 삭제
 *
 * @param schedule 보완 대상 스케줄
 * @param tleLine1 TLE 1행
 * @param tleLine2 TLE 2행
 * @param minElevation 최소 고도각
 * @param latitude 지상국 위도
 * @param longitude 지상국 경도
 * @param altitude 지상국 고도
 * @param trackingIntervalMs 추적 간격(ms)
 * @param settingsService 보완 설정 조회용 서비스
 *
 * @return 첫 번째 패스가 병합 또는 삭제된 스케줄
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
    
    // 시작 00:00 패스인지 확인 (정확히 00:00:00만 확인)
    val midnight = today.atTime(0, 0, 0).withZoneSameInstant(ZoneOffset.UTC)
    if (firstPass.startTime.truncatedTo(ChronoUnit.SECONDS) != midnight) {
        return schedule  // 정확히 00:00:00 시작 패스가 아니면 보완 불필요
    }
    
    // 설정값 가져오기 및 검증
    val startBeforeHours = (settingsService.midnightSupplementStartBeforeHours ?: 1.0)
        .coerceIn(0.1, 24.0) // 0.0 허용 시 보완 범위가 0이 됨
    
    logger.info("🔍 시작 00:00 패스 감지: ${firstPass.startTime} ~ ${firstPass.endTime}")
    
    // 보완 계산 범위: 전날 23:00 ~ 오늘 00:00 (1시간만 스캔)
    val supplementStart = yesterday.atTime(0, 0, 0)
        .minusHours(startBeforeHours.toLong())
        .withZoneSameInstant(ZoneOffset.UTC)
    val supplementEnd = today.atTime(0, 0, 0).withZoneSameInstant(ZoneOffset.UTC)
    
    logger.info("   → 보완 범위: ${supplementStart} ~ ${supplementEnd}")
    
    try {
        // 보완 계산 (1시간 범위)
        val supplementDuration = Duration.between(supplementStart, supplementEnd)
        val supplementDurationDays = maxOf(1, (supplementDuration.toHours() / 24.0).toInt() + 1)
        
        val supplementPeriods = detectVisibilityPeriods(
            tleLine1, tleLine2,
            supplementStart, 
            supplementDurationDays,
            minElevation, latitude, longitude, altitude
        )
        
        logger.info("📊 보완 계산 결과: ${supplementPeriods.size}개 패스 발견")
        
        // 보완 범위 내로 필터링 (supplementEnd를 초과하는 패스 제외)
        val filteredPeriods = supplementPeriods.filter { period ->
            period.startTime != null && 
            period.endTime != null &&
            period.startTime!!.isBefore(supplementEnd) &&
            period.endTime!!.isAfter(supplementStart)
        }
        
        logger.info("📊 보완 범위 내 패스: ${filteredPeriods.size}개")
        
        // 00:00에 걸쳐있는 스케줄 찾기
        val midnightCrossingPasses = filteredPeriods.filter { period ->
            period.startTime != null && 
            period.endTime != null &&
            period.startTime!!.isBefore(midnight) &&  // 전날 시작
            period.endTime!!.isAfter(midnight)         // 오늘 00:00 이후 종료
        }
        
        logger.info("📊 00:00에 걸쳐있는 패스: ${midnightCrossingPasses.size}개")
        
        if (midnightCrossingPasses.isEmpty()) {
            // 00:00에 걸쳐있는 패스 없음 → 원래 00:00 패스 삭제
            logger.warn("⚠️ 00:00에 걸쳐있는 패스를 찾지 못함. 원래 00:00 패스 삭제")
            val supplementedPasses = schedule.trackingPasses.toMutableList()
            supplementedPasses.removeAt(0)
            if (supplementedPasses.isEmpty()) {
                logger.warn("   → 보완 실패로 모든 패스가 제거됨 (빈 스케줄)")
                return schedule.copy(trackingPasses = supplementedPasses)
            }
            return schedule.copy(trackingPasses = supplementedPasses)
        }
        
        // 여러 패스가 있을 때, 오늘 00:00에 가장 가까운 패스 1개만 선택
        val connectingPass = if (midnightCrossingPasses.size > 1) {
            logger.info("   → ${midnightCrossingPasses.size}개 패스 중 00:00에 가장 가까운 패스 선택")
            midnightCrossingPasses.minByOrNull { period ->
                Duration.between(period.startTime!!, midnight).abs().toMillis()
            }
        } else {
            midnightCrossingPasses.first()
        }
        
        if (connectingPass == null) {
            logger.warn("⚠️ 연결 패스를 찾지 못함. 원래 00:00 패스 삭제")
            val supplementedPasses = schedule.trackingPasses.toMutableList()
            supplementedPasses.removeAt(0)
            if (supplementedPasses.isEmpty()) {
                logger.warn("   → 보완 실패로 모든 패스가 제거됨 (빈 스케줄)")
                return schedule.copy(trackingPasses = supplementedPasses)
            }
            return schedule.copy(trackingPasses = supplementedPasses)
        }
        
        logger.info("✅ 00:00에 걸쳐있는 패스 발견: ${connectingPass.startTime} ~ ${connectingPass.endTime}")
        
        // ✅ ElevationDetector가 찾은 완전한 패스 전체를 재생성
        // connectingPass는 고도각 조건을 만족하는 "전체 패스"이므로 그대로 재생성
        // 메타데이터(MaxElevation, Duration 등)는 Processor가 자동 재계산
        logger.info("   → 패스 전체 재생성: ${connectingPass.startTime} ~ ${connectingPass.endTime}")
        
        val mergedData = generateDetailedTrackingData(
            tleLine1, tleLine2,
            connectingPass.startTime!!,  // ✅ 패스 전체 시작
            connectingPass.endTime!!,    // ✅ 패스 전체 종료
            trackingIntervalMs, latitude, longitude, altitude, minElevation
        )
        
        val mergedPass = firstPass.copy(
            startTime = connectingPass.startTime!!,
            endTime = connectingPass.endTime!!,
            trackingData = mergedData  // 전체 패스 데이터
        )
        
        val mergedDuration = Duration.between(mergedPass.startTime, mergedPass.endTime).toMinutes()
        val originalDuration = Duration.between(firstPass.startTime, firstPass.endTime).toMinutes()
        val extendedDuration = mergedDuration - originalDuration
        
        val supplementedPasses = schedule.trackingPasses.toMutableList()
        supplementedPasses[0] = mergedPass
        
        logger.info("✅ 시작 패스 보완 완료: ${mergedPass.startTime} ~ ${mergedPass.endTime}")
        logger.info("   → 원본 패스: ${firstPass.startTime} ~ ${firstPass.endTime} (지속: ${originalDuration}분)")
        logger.info("   → 병합 후: ${mergedPass.startTime} ~ ${mergedPass.endTime} (지속: ${mergedDuration}분, 연장: +${extendedDuration}분)")
        
        return schedule.copy(trackingPasses = supplementedPasses)
    } catch (e: Exception) {
        // 보완 계산 중 예외 발생 시 원래 00:00 패스 삭제
        logger.error("❌ 보완 계산 중 예외 발생: ${e.message}", e)
        logger.warn("   → 원래 00:00 패스 삭제")
        val supplementedPasses = schedule.trackingPasses.toMutableList()
        supplementedPasses.removeAt(0)
        if (supplementedPasses.isEmpty()) {
            logger.warn("   → 예외 처리 후 모든 패스가 제거됨 (빈 스케줄)")
            return schedule.copy(trackingPasses = supplementedPasses)
        }
        return schedule.copy(trackingPasses = supplementedPasses)
    }
}
```

### 1.4 테스트 체크리스트
- [ ] 정확히 00:00:00에 시작하는 패스만 보완 대상으로 감지되는지 확인.
- [ ] 보완 범위 내 패스 필터링이 올바르게 동작하는지 확인 (supplementEnd를 초과하는 패스 제외).
- [ ] 00:00에 걸쳐있는 패스가 정상적으로 병합되는지 확인.
- [ ] 00:00에 걸쳐있는 패스를 찾지 못한 경우 원래 00:00 패스가 삭제되는지 확인.
- [ ] 보완 범위 내에 여러 패스가 있을 때, 00:00에 가장 가까운 패스 1개만 선택되는지 확인.
- [ ] 보완 계산 중 예외 발생 시 원래 00:00 패스가 삭제되는지 확인.
- [ ] PassSchedule 모달과 Ephemeris SelectSchedule 화면에 동일한 병합 결과가 표시되는지 비교.
- [ ] 설정값 변경 시 보완 범위가 올바르게 적용되는지 확인.

---

## 2. 종료 00시 스케줄 확장 (구현 예정)

### 2.1 문제 정의
- 특정 패스가 00:00에 종료될 경우, 다음날 00:00 이후 구간이 별도의 패스로 끊겨 연속 추적이 어려움.
- 스케줄 목록상에서는 두 개의 패스로 보이지만, 실제로는 하나의 패스가 자정 경계를 지나는 경우가 다수 존재.

### 2.2 구현 전략: 효율적인 선택적 보완

#### 핵심 개념
- **기존 2일치 계산 유지**: 오늘 00:00 ~ 모레 00:00 (연산량 동일)
- **선택적 보완 계산**: 마지막 패스가 00:00에 종료하는 경우만 짧은 범위로 재계산
- **연산량 증가**: 평균 2~3% (3일치 계산 대비 약 1/20)

#### 구현 방법
1. **기존 2일치 계산 수행** (오늘 00:00 ~ 모레 00:00)
2. **종료 00:00 패스 감지**: 마지막 패스가 정확히 00:00:00에 종료하는지 확인 (초/분 단위)
3. **선택적 보완 계산**: 감지된 경우만 모레 00:00 ~ 모레 설정 시간 후 범위를 재계산 (모레 00:00 ~ 모레 01:00, 기본 1시간)
4. **보완 범위 내 패스 필터링**: 보완 계산 결과를 보완 범위 내로 필터링
5. **모레 00:00에 걸쳐있는 스케줄 찾기**: 보완 범위 내 결과에서 모레 00:00에 걸쳐있는 스케줄 찾기 (오늘 시작, 모레 00:00 이후 종료)
6. **00시에 가장 가까운 패스 1개만 선택**: 보완 범위 내에 여러 패스가 있을 때, 모레 00:00에 가장 가까운 패스 1개만 선택하고 나머지는 무시
7. **패스 병합**: 선택된 보완 패스 전체 데이터를 재생성하여 기존 스케줄과 병합
8. **보완 실패 처리**: 모레 00:00에 걸쳐있는 패스를 찾지 못하면 원래 00:00 패스 삭제
9. **에러 처리**: 보완 계산 중 예외 발생 시 원래 00:00 패스 삭제

#### 설정 기반 시간 범위
- `ephemeris.tracking.midnightSupplement.endAfterHours`: 모레 계산 종료 시간 (기본값: 1.0시간 = 모레 01:00)
- **보완 범위**: 모레 00:00 ~ 모레 01:00 (1시간만 스캔)

### 2.3 구현 상세

#### 2.3.1 SettingsService 설정 추가
```kotlin
// SettingsService.kt의 settingDefinitions에 추가
/**
 * 자정 경계 패스 보완: 종료 00:00 패스 모레 계산 종료 시간 (시간)
 *
 * ## 용도
 * - 모레 00:00 이후 구간을 재계산하여 00:00 경계를 가로지르는 패스를 복원합니다.
 *
 * ## 기본값
 * - 1.0시간 (모레 00:00 ~ 모레 01:00 스캔)
 *
 * ## 최소값
 * - 0.1시간 (6분) 이상만 허용
 */
"ephemeris.tracking.midnightSupplement.endAfterHours" to SettingDefinition(
    "ephemeris.tracking.midnightSupplement.endAfterHours", 
    1.0, SettingType.DOUBLE, 
    "자정 경계 패스 보완: 종료 00:00 패스 보완을 위한 모레 계산 종료 시간 (시간)"
),
```

#### 2.3.2 보완 함수 구현
```kotlin
/**
 * 종료 00:00 패스를 모레 패스와 병합해 연속성을 복원합니다.
 *
 * ## 동작 순서
 * 1. 마지막 패스가 정확히 00:00:00에 종료하는지 (초 단위) 확인
 * 2. 모레 00:00 ~ (모레 00:00 + endAfterHours) 범위를 재계산 (기본 1시간)
 * 3. 보완 범위 내 패스 중 모레 00:00을 가로지르는 패스만 추출
 * 4. 00:00에 가장 가까운 단일 패스를 선택 후 기존 패스와 병합
 * 5. 보완 실패 또는 예외 발생 시 원래 00:00 패스를 삭제
 *
 * @param schedule 보완 대상 스케줄
 * @param tleLine1 TLE 1행
 * @param tleLine2 TLE 2행
 * @param minElevation 최소 고도각
 * @param latitude 지상국 위도
 * @param longitude 지상국 경도
 * @param altitude 지상국 고도
 * @param trackingIntervalMs 추적 간격(ms)
 * @param settingsService 보완 설정 조회용 서비스
 *
 * @return 마지막 패스가 병합 또는 삭제된 스케줄
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
    val dayAfterTomorrow = today.plusDays(2)  // 모레
    
    // 종료 00:00 패스인지 확인 (정확히 00:00:00만 확인)
    val midnight = dayAfterTomorrow.atTime(0, 0, 0).withZoneSameInstant(ZoneOffset.UTC)
    if (lastPass.endTime.truncatedTo(ChronoUnit.SECONDS) != midnight) {
        return schedule  // 정확히 00:00:00 종료 패스가 아니면 보완 불필요
    }
    
    // 설정값 가져오기 및 검증
    val endAfterHours = (settingsService.midnightSupplementEndAfterHours ?: 1.0)
        .coerceIn(0.1, 24.0) // 0.0 허용 시 보완 범위가 0이 됨
    
    logger.info("🔍 종료 00:00 패스 감지: ${lastPass.startTime} ~ ${lastPass.endTime}")
    
    // 보완 계산 범위: 모레 00:00 ~ 모레 01:00 (1시간만 스캔)
    val supplementStart = dayAfterTomorrow.atTime(0, 0, 0).withZoneSameInstant(ZoneOffset.UTC)
    val supplementEnd = dayAfterTomorrow.atTime(0, 0, 0)
        .plusHours(endAfterHours.toLong())
        .withZoneSameInstant(ZoneOffset.UTC)
    
    logger.info("   → 보완 범위: ${supplementStart} ~ ${supplementEnd}")
    
    try {
        // 보완 계산 (1시간 범위)
        val supplementDuration = Duration.between(supplementStart, supplementEnd)
        val supplementDurationDays = maxOf(1, (supplementDuration.toHours() / 24.0).toInt() + 1)
        
        val supplementPeriods = detectVisibilityPeriods(
            tleLine1, tleLine2,
            supplementStart,
            supplementDurationDays,
            minElevation, latitude, longitude, altitude
        )
        
        logger.info("📊 보완 계산 결과: ${supplementPeriods.size}개 패스 발견")
        
        // 보완 범위 내로 필터링 (supplementEnd를 초과하는 패스 제외)
        val filteredPeriods = supplementPeriods.filter { period ->
            period.startTime != null && 
            period.endTime != null &&
            period.startTime!!.isBefore(supplementEnd) &&
            period.endTime!!.isAfter(supplementStart)
        }
        
        logger.info("📊 보완 범위 내 패스: ${filteredPeriods.size}개")
        
        // 모레 00:00에 걸쳐있는 스케줄 찾기
        val midnightCrossingPasses = filteredPeriods.filter { period ->
            period.startTime != null && 
            period.endTime != null &&
            period.startTime!!.isBefore(midnight) &&  // 오늘 시작
            period.endTime!!.isAfter(midnight)         // 모레 00:00 이후 종료
        }
        
        logger.info("📊 모레 00:00에 걸쳐있는 패스: ${midnightCrossingPasses.size}개")
        
        if (midnightCrossingPasses.isEmpty()) {
            // 모레 00:00에 걸쳐있는 패스 없음 → 원래 00:00 패스 삭제
            logger.warn("⚠️ 모레 00:00에 걸쳐있는 패스를 찾지 못함. 원래 00:00 패스 삭제")
            val supplementedPasses = schedule.trackingPasses.toMutableList()
            supplementedPasses.removeAt(supplementedPasses.size - 1)
            if (supplementedPasses.isEmpty()) {
                logger.warn("   → 보완 실패로 모든 패스가 제거됨 (빈 스케줄)")
                return schedule.copy(trackingPasses = supplementedPasses)
            }
            return schedule.copy(trackingPasses = supplementedPasses)
        }
        
        // 여러 패스가 있을 때, 모레 00:00에 가장 가까운 패스 1개만 선택
        val connectingPass = if (midnightCrossingPasses.size > 1) {
            logger.info("   → ${midnightCrossingPasses.size}개 패스 중 00:00에 가장 가까운 패스 선택")
            midnightCrossingPasses.minByOrNull { period ->
                Duration.between(period.startTime!!, midnight).abs().toMillis()
            }
        } else {
            midnightCrossingPasses.first()
        }
        
        if (connectingPass == null) {
            logger.warn("⚠️ 연결 패스를 찾지 못함. 원래 00:00 패스 삭제")
            val supplementedPasses = schedule.trackingPasses.toMutableList()
            supplementedPasses.removeAt(supplementedPasses.size - 1)
            if (supplementedPasses.isEmpty()) {
                logger.warn("   → 보완 실패로 모든 패스가 제거됨 (빈 스케줄)")
                return schedule.copy(trackingPasses = supplementedPasses)
            }
            return schedule.copy(trackingPasses = supplementedPasses)
        }
        
        logger.info("✅ 모레 00:00에 걸쳐있는 패스 발견: ${connectingPass.startTime} ~ ${connectingPass.endTime}")
        
        // ✅ ElevationDetector가 찾은 완전한 패스 전체를 재생성
        // connectingPass는 고도각 조건을 만족하는 "전체 패스"이므로 그대로 재생성
        // 메타데이터(MaxElevation, Duration 등)는 Processor가 자동 재계산
        logger.info("   → 패스 전체 재생성: ${connectingPass.startTime} ~ ${connectingPass.endTime}")
        
        val mergedData = generateDetailedTrackingData(
            tleLine1, tleLine2,
            connectingPass.startTime!!,  // ✅ 패스 전체 시작
            connectingPass.endTime!!,    // ✅ 패스 전체 종료
            trackingIntervalMs, latitude, longitude, altitude, minElevation
        )
        
        val mergedPass = lastPass.copy(
            startTime = connectingPass.startTime!!,
            endTime = connectingPass.endTime!!,
            trackingData = mergedData  // 전체 패스 데이터
        )
        
        val mergedDuration = Duration.between(mergedPass.startTime, mergedPass.endTime).toMinutes()
        val originalDuration = Duration.between(lastPass.startTime, lastPass.endTime).toMinutes()
        val extendedDuration = mergedDuration - originalDuration
        
        val supplementedPasses = schedule.trackingPasses.toMutableList()
        supplementedPasses[supplementedPasses.size - 1] = mergedPass
        
        logger.info("✅ 종료 패스 보완 완료: ${mergedPass.startTime} ~ ${mergedPass.endTime}")
        logger.info("   → 원본 패스: ${lastPass.startTime} ~ ${lastPass.endTime} (지속: ${originalDuration}분)")
        logger.info("   → 병합 후: ${mergedPass.startTime} ~ ${mergedPass.endTime} (지속: ${mergedDuration}분, 연장: +${extendedDuration}분)")
        
        return schedule.copy(trackingPasses = supplementedPasses)
    } catch (e: Exception) {
        // 보완 계산 중 예외 발생 시 원래 00:00 패스 삭제
        logger.error("❌ 보완 계산 중 예외 발생: ${e.message}", e)
        logger.warn("   → 원래 00:00 패스 삭제")
        val supplementedPasses = schedule.trackingPasses.toMutableList()
        supplementedPasses.removeAt(supplementedPasses.size - 1)
        if (supplementedPasses.isEmpty()) {
            logger.warn("   → 예외 처리 후 모든 패스가 제거됨 (빈 스케줄)")
            return schedule.copy(trackingPasses = supplementedPasses)
        }
        return schedule.copy(trackingPasses = supplementedPasses)
    }
}
```

### 2.4 통합 함수

#### 2.4.1 보완 로직 통합 함수
```kotlin
/**
 * 시작/종료 00:00 패스를 순차적으로 보완합니다.
 *
 * @param schedule 기본 스케줄
 * @param tleLine1 TLE 1행
 * @param tleLine2 TLE 2행
 * @param minElevation 최소 고도각
 * @param latitude 지상국 위도
 * @param longitude 지상국 경도
 * @param altitude 지상국 고도
 * @param trackingIntervalMs 추적 간격(ms)
 * @param settingsService 보완 설정 조회용 서비스
 *
 * @return 시작 보완 → 종료 보완 순으로 적용된 스케줄
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

#### 2.4.2 OrekitCalculator 래퍼 함수 (권장)
```kotlin
/**
 * 자정 경계 보완이 포함된 위성 추적 스케줄을 생성합니다.
 *
 * @param tleLine1 TLE 1행
 * @param tleLine2 TLE 2행
 * @param startDate 시작 시간 (UTC)
 * @param durationDays 계산 일수
 * @param minElevation 최소 고도각
 * @param latitude 지상국 위도
 * @param longitude 지상국 경도
 * @param altitude 지상국 고도
 * @param trackingIntervalMs 추적 간격(ms)
 * @param settingsService 보완 설정 조회용 서비스
 *
 * @return 자정 경계 보완이 적용된 최종 스케줄
 */
fun OrekitCalculator.generateSatelliteTrackingScheduleWithMidnightSupplement(
    tleLine1: String,
    tleLine2: String,
    startDate: ZonedDateTime,
    durationDays: Int,
    minElevation: Float,
    latitude: Double,
    longitude: Double,
    altitude: Double,
    trackingIntervalMs: Int = 100,
    settingsService: SettingsService
): SatelliteTrackingSchedule {
    // 1. 기본 스케줄 생성
    val schedule = generateSatelliteTrackingSchedule(
        tleLine1, tleLine2, startDate, durationDays,
        minElevation, latitude, longitude, altitude, trackingIntervalMs
    )
    
    // 2. 자정 경계 보완 적용
    return supplementMidnightPasses(
        schedule, tleLine1, tleLine2, minElevation,
        latitude, longitude, altitude, trackingIntervalMs, settingsService
    )
}
```

#### 2.4.3 Service 단계 적용 예시
```kotlin
// EphemerisService.kt - generateEphemerisDesignationTrackSync()
var schedule = orekitCalculator.generateSatelliteTrackingScheduleWithMidnightSupplement(
    tleLine1 = tleLine1,
    tleLine2 = tleLine2,
    startDate = today.withZoneSameInstant(ZoneOffset.UTC),
    durationDays = 2,
    minElevation = sourceMinEl,
    latitude = locationData.latitude,
    longitude = locationData.longitude,
    altitude = locationData.altitude,
    trackingIntervalMs = 100,
    settingsService = settingsService
)
// removeLeadingMidnightPass() 호출 제거됨

// PassScheduleService.kt - generatePassScheduleTrackingDataAsync()
var schedule = orekitCalculator.generateSatelliteTrackingScheduleWithMidnightSupplement(
    tleLine1 = tleLine1,
    tleLine2 = tleLine2,
    startDate = today.withZoneSameInstant(ZoneOffset.UTC),
    durationDays = 2,
    minElevation = sourceMinEl,
    latitude = locationData.latitude,
    longitude = locationData.longitude,
    altitude = locationData.altitude,
    trackingIntervalMs = 100,
    settingsService = settingsService
)
// removeLeadingMidnightPass() 호출 제거됨
```

### 2.5 요구 사항
1. **스케줄 지속성**: 00:00 종료 패스가 바로 다음 패스로 이어지면 자동 병합.
2. **데이터 연속성**: 병합 시 DTL 데이터 재생성, 메타데이터(MaxElevation, Duration 등)는 Processor가 자동 재계산.
3. **00시에 가장 가까운 패스 선택**: 보완 범위 내에 여러 패스가 있을 때, 00:00에 가장 가까운 패스 1개만 선택.
4. **보완 실패 처리**: 모레 패스를 찾지 못하면 원래 00:00 패스 삭제.
5. **에러 처리**: 보완 계산 중 예외 발생 시 원래 00:00 패스 삭제.
6. **UI 일관성**: PassSchedule 및 Ephemeris 화면 모두 병합된 단일 패스를 노출.
7. **로깅 & 트레이싱**: 병합/연장 여부, 병합된 패스 수 등을 로깅.
8. **성능 최적화**: 필요한 경우만 보완 계산 수행 (연산량 증가 최소화).

### 2.6 테스트 체크리스트
- [ ] 정확히 00:00:00에 종료하는 패스만 보완 대상으로 감지되는지 확인.
- [ ] 보완 범위 내 패스 필터링이 올바르게 동작하는지 확인 (supplementEnd를 초과하는 패스 제외).
- [ ] 모레 00:00에 걸쳐있는 패스가 정상적으로 병합되는지 확인.
- [ ] 모레 00:00에 걸쳐있는 패스를 찾지 못한 경우 원래 00:00 패스가 삭제되는지 확인.
- [ ] 보완 범위 내에 여러 패스가 있을 때, 모레 00:00에 가장 가까운 패스 1개만 선택되는지 확인.
- [ ] 보완 계산 중 예외 발생 시 원래 00:00 패스가 삭제되는지 확인.
- [ ] PassSchedule 모달과 Ephemeris SelectSchedule 화면에 동일한 병합 결과가 표시되는지 비교.
- [ ] 설정값 변경 시 보완 범위가 올바르게 적용되는지 확인.

### 2.7 TODO
- [ ] SettingsService에 자정 경계 보완 설정 추가 (4개, 기본값 1.0시간)
- [ ] `supplementStartMidnightPass()` 함수 구현 (00시에 가장 가까운 패스 1개만 선택 로직 포함)
- [ ] `supplementEndMidnightPass()` 함수 구현 (00시에 가장 가까운 패스 1개만 선택 로직 포함)
- [ ] `supplementMidnightPasses()` 통합 함수 구현
- [ ] `OrekitCalculator.generateSatelliteTrackingScheduleWithMidnightSupplement()` 래퍼 함수 추가
- [ ] `EphemerisService.kt`에서 보완 로직 통합된 함수 사용 및 `removeLeadingMidnightPass()` 제거
- [ ] `PassScheduleService.kt`에서 보완 로직 통합된 함수 사용 및 `removeLeadingMidnightPass()` 제거
- [ ] Processor/Service 단계에서 병합된 데이터로 MaxElevation/Duration 재계산 검증
- [ ] PassScheduleStore UI 시나리오 점검 및 회귀 테스트
- [ ] 설정값 변경 시 동작 검증
- [ ] 보완 범위 내 여러 패스 중 00시에 가장 가까운 패스 선택 로직 검증
- [ ] 보완 범위 필터링 로직 검증 (supplementEnd를 초과하는 패스 제외)
- [ ] 에러 처리 로직 검증 (보완 계산 중 예외 발생 시 원래 00:00 패스 삭제)

---

## 3. 로깅 및 모니터링

### 3.1 상세 로그 항목

#### 3.1.1 시작 00:00 패스 보완 로그
- **보완 계산 결과**: 발견된 모든 패스의 개수
- **보완 범위 내 패스 정보**: 보완 범위 내로 필터링된 패스의 개수
- **00:00에 걸쳐있는 패스 정보**: 발견된 모든 패스의 시작/종료 시간, 지속 시간
- **선택된 패스 정보**: 00:00에 가장 가까운 패스의 상세 정보 및 선택 이유
- **삭제 상세 정보**: 삭제되는 패스의 시작/종료 시간, 지속 시간, 삭제 이유
- **병합 성공 상세**: 원본 패스와 병합 후 패스의 비교 정보 (지속 시간, 연장 시간)
- **에러 처리 상세**: 보완 계산 중 예외 발생 시 예외 메시지 및 삭제 정보

#### 3.1.2 종료 00:00 패스 보완 로그
- **보완 계산 결과**: 발견된 모든 패스의 개수
- **보완 범위 내 패스 정보**: 보완 범위 내로 필터링된 패스의 개수
- **모레 00:00에 걸쳐있는 패스 정보**: 발견된 모든 패스의 시작/종료 시간, 지속 시간
- **선택된 패스 정보**: 모레 00:00에 가장 가까운 패스의 상세 정보 및 선택 이유
- **삭제 상세 정보**: 삭제되는 패스의 시작/종료 시간, 지속 시간, 삭제 이유
- **병합 성공 상세**: 원본 패스와 병합 후 패스의 비교 정보 (지속 시간, 연장 시간)
- **에러 처리 상세**: 보완 계산 중 예외 발생 시 예외 메시지 및 삭제 정보

### 3.2 로그 레벨
- **INFO**: 정상 처리 (패스 발견, 병합 성공)
- **WARN**: 패스 미발견, 삭제 처리
- **ERROR**: 보완 계산 중 예외 발생
- **DEBUG**: 상세 계산 과정 (선택사항)

### 3.3 로그 예시

#### 정상 병합 케이스
```
🔍 시작 00:00 패스 감지: 2024-01-02T00:00:00Z ~ 2024-01-02T00:15:00Z
   → 보완 범위: 2024-01-01T23:00:00Z ~ 2024-01-02T00:00:00Z
📊 보완 계산 결과: 3개 패스 발견
📊 보완 범위 내 패스: 2개
📊 00:00에 걸쳐있는 패스: 1개
✅ 00:00에 걸쳐있는 패스 발견: 2024-01-01T23:50:00Z ~ 2024-01-02T00:15:00Z
✅ 시작 패스 보완 완료: 2024-01-01T23:50:00Z ~ 2024-01-02T00:15:00Z
   → 원본 패스: 2024-01-02T00:00:00Z ~ 2024-01-02T00:15:00Z (지속: 15분)
   → 병합 후: 2024-01-01T23:50:00Z ~ 2024-01-02T00:15:00Z (지속: 25분, 연장: +10분)
```

#### 보완 실패 케이스
```
🔍 시작 00:00 패스 감지: 2024-01-02T00:00:00Z ~ 2024-01-02T00:15:00Z
   → 보완 범위: 2024-01-01T23:00:00Z ~ 2024-01-02T00:00:00Z
📊 보완 계산 결과: 2개 패스 발견
📊 보완 범위 내 패스: 1개
📊 00:00에 걸쳐있는 패스: 0개
⚠️ 00:00에 걸쳐있는 패스를 찾지 못함. 원래 00:00 패스 삭제
   → 삭제 대상: 2024-01-02T00:00:00Z ~ 2024-01-02T00:15:00Z (지속: 15분)
   → 삭제 완료: 패스 시작=2024-01-02T00:00:00Z, 종료=2024-01-02T00:15:00Z
```

#### 에러 처리 케이스
```
🔍 시작 00:00 패스 감지: 2024-01-02T00:00:00Z ~ 2024-01-02T00:15:00Z
   → 보완 범위: 2024-01-01T23:00:00Z ~ 2024-01-02T00:00:00Z
❌ 보완 계산 중 예외 발생: TLE 파싱 오류
   → 원래 00:00 패스 삭제
   → 삭제 완료: 패스 시작=2024-01-02T00:00:00Z, 종료=2024-01-02T00:15:00Z
```

---

## 4. 성능 분석

### 4.1 연산량 비교

#### 기존 방법 (3일치 계산)
- 연산량: 3일치 = 72시간
- 증가율: 50% (2일치 대비)

#### 개선 방법 (선택적 보완)
- 기본 연산량: 2일치 = 48시간 (오늘 00:00 ~ 모레 00:00)
- 보완 연산량: 최대 2시간 (시작 1시간 + 종료 1시간, 각각 1시간 전후)
- 총 연산량: 최대 50시간
- 증가율: 약 4.17% (2일치 대비)

#### 실제 시나리오
- 대부분의 경우: 시작/종료 중 하나만 보완 필요 → 약 2% 증가
- 최악의 경우: 둘 다 보완 필요 → 약 4.17% 증가
- 평균: 약 2~3% 증가

### 4.2 효율성
- **3일치 계산 대비**: 약 69% 연산량 (50시간 / 72시간)
- **필요한 경우만 계산**: 불필요한 연산 최소화
- **설정 기반 조정**: 패스 길이에 따라 계산 범위 조정 가능

---

## 5. 구현 위치

### 5.1 백엔드
- **OrekitCalculator.kt**: 
  - `supplementStartMidnightPass()` 함수 추가 (00시에 가장 가까운 패스 1개만 선택 로직 포함)
  - `supplementEndMidnightPass()` 함수 추가 (00시에 가장 가까운 패스 1개만 선택 로직 포함)
  - `supplementMidnightPasses()` 통합 함수 추가
  - `generateSatelliteTrackingScheduleWithMidnightSupplement()` 래퍼 함수 추가 (권장)
    - 기존 `generateSatelliteTrackingSchedule()` 호출 후 `supplementMidnightPasses()` 자동 적용
- **SettingsService.kt**: 
  - 자정 경계 보완 설정 4개 추가 (기본값: 1.0시간)
  - 프로퍼티 및 일괄 설정 메서드 추가
- **EphemerisService.kt**: 
  - `generateEphemerisDesignationTrackSync()`에서 보완 로직 통합된 스케줄 생성 함수 사용
  - 기존 `removeLeadingMidnightPass()` 호출 제거
- **PassScheduleService.kt**: 
  - `generatePassScheduleTrackingDataAsync()`에서 보완 로직 통합된 스케줄 생성 함수 사용
  - 기존 `removeLeadingMidnightPass()` 호출 제거
  - 여러 위성 스케줄 관리 시 각 위성별로 동일한 보완 로직 적용
- **SatelliteTrackingProcessor**: 
  - 병합된 패스 데이터로 메타데이터 재계산 보장

### 5.2 프론트엔드
- **passScheduleStore.ts**: 
  - 기본적으로 백엔드 병합 결과를 그대로 사용
  - 과도기/캐시 이슈 시 프론트에서도 "연속 패스 병합" fallback 유틸 제공 (선택사항)
- **Ephemeris UI**: 
  - 연속 패스 병합 시 Progress/Countdown 계산이 다음날까지 연장되므로 타이머 계산 로직 검토

---

## 6. 설정 관리

### 6.1 설정 항목
- `ephemeris.tracking.midnightSupplement.startBeforeHours`  
  - 전날 (00:00 - startBeforeHours) ~ 오늘 00:00 범위 재계산  
  - 기본값 1.0시간, 최소값 0.1시간, 최대값 24시간  
  - 실제 보완 로직에서 사용
- `ephemeris.tracking.midnightSupplement.endAfterHours`  
  - 모레 00:00 ~ (모레 00:00 + endAfterHours) 범위 재계산  
  - 기본값 1.0시간, 최소값 0.1시간, 최대값 24시간  
  - 실제 보완 로직에서 사용

### 6.2 설정 사용 예시
```kotlin
// 기본값 (1시간 전후)
// 시작 보완: 전날 23:00 ~ 오늘 00:00
// 종료 보완: 모레 00:00 ~ 모레 01:00

// 더 긴 패스 대응 (2시간 전후)
settingsService.setMidnightSupplementSettings(
    startBeforeHours = 2.0,
    endAfterHours = 2.0
)
// 시작 보완: 전날 22:00 ~ 오늘 00:00
// 종료 보완: 모레 00:00 ~ 모레 02:00

// 짧은 패스 최적화 (30분 전후)
settingsService.setMidnightSupplementSettings(
    startBeforeHours = 0.5,
    endAfterHours = 0.5
)
// 시작 보완: 전날 23:30 ~ 오늘 00:00
// 종료 보완: 모레 00:00 ~ 모레 00:30
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
