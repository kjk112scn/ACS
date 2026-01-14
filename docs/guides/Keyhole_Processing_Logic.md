# Keyhole 처리 로직 문서

## 개요

위성 추적 시 Keyhole(천정 영역)을 통과하는 경우의 안테나 제어 로직을 설명합니다.

## Keyhole이란?

- **정의**: 안테나가 천정(Elevation 90°) 근처를 통과할 때 발생하는 특이점
- **문제점**: 천정 부근에서 Azimuth 각속도가 급격히 증가하여 안테나가 따라가지 못함
- **해결**: Train(Tilt) 축을 회전시켜 Azimuth/Elevation 각속도를 줄임

## DataType 종류

| DataType | 설명 | Train 각도 | 용도 |
|----------|------|-----------|------|
| `original` | TLE 기반 원본 좌표 | 0° | 참조용 |
| `axis_transformed` | 3축 변환 적용 | 0° | 중간 계산 |
| `final_transformed` | 최종 변환 (Non-Keyhole) | 0° | **일반 추적** |
| `keyhole_axis_transformed` | 3축 변환 (Keyhole) | ≠0° | 중간 계산 |
| `keyhole_optimized_final_transformed` | 최종 변환 (Keyhole) | ≠0° | **Keyhole 추적** |

> ⚠️ **주의**: `keyhole_final_transformed`는 구버전 DataType으로 더 이상 사용하지 않음.
> 모든 Keyhole 처리는 `keyhole_optimized_final_transformed`를 사용해야 함.

## Keyhole 판단 기준

```kotlin
// final_transformed MST에서 IsKeyhole 플래그 확인
val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
```

### Keyhole 발생 조건
1. 패스 중 최대 고도(MaxElevation)가 높음 (일반적으로 70° 이상)
2. Azimuth 각속도가 안테나 최대 속도를 초과
3. Train 축 회전으로 각속도를 줄일 수 있는 경우

## 데이터 흐름

### 1. 궤도 데이터 생성 시

```
TLE 데이터 입력
    ↓
OrekitCalculator.calculatePosition()
    ↓
[original] 데이터 저장
    ↓
CoordinateTransformer.transform()
    ↓
[axis_transformed] 데이터 저장
    ↓
각도 제한 적용 (±270°)
    ↓
[final_transformed] 데이터 저장
    ↓
Keyhole 판단 (IsKeyhole 플래그 설정)
    ↓
(Keyhole 발생 시)
    ↓
Train 최적 각도 계산
    ↓
[keyhole_axis_transformed] 데이터 저장
    ↓
각도 제한 적용 (±270°)
    ↓
[keyhole_optimized_final_transformed] 데이터 저장
```

### 2. 추적 시작 시

```kotlin
// startEphemerisTracking 함수
fun startEphemerisTracking(mstId: Long, detailId: Int) {
    // 1. final_transformed MST에서 Keyhole 여부 확인
    val selectedPass = getTrackingPassMst(mstId)
    val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false

    // 2. Keyhole이면 RecommendedTrainAngle 사용
    val recommendedTrainAngle = selectedPass["RecommendedTrainAngle"] as? Double ?: 0.0

    // 3. 추적 시작
    currentTrackingPass = selectedPass
    currentTrackingState = TrackingState.PREPARING
}
```

### 3. 실시간 추적 데이터 생성 시

```kotlin
// createRealtimeTrackingData 함수
fun createRealtimeTrackingData(mstId: Long, detailId: Int, ...) {
    // 1. Keyhole 여부에 따라 DataType 선택
    val finalDataType = if (isKeyhole) {
        "keyhole_optimized_final_transformed"  // ✅ Keyhole
    } else {
        "final_transformed"  // ✅ Non-Keyhole
    }

    // 2. 해당 DataType의 DTL 데이터 조회
    val allPassDetails = getEphemerisTrackDtlByMstIdAndDetailId(mstId, detailId, finalDataType)

    // 3. CMD 값 계산 (Keyhole이면 keyhole_optimized_final_transformed 값 사용)
    val cmdAz = if (isKeyhole && keyholeFinalTransformedAzimuth != null) {
        keyholeFinalTransformedAzimuth
    } else {
        finalTransformedAzimuth
    }
}
```

## Train 각도 설정

### Keyhole 발생 시
```kotlin
// handlePreparingState 함수
val trainAngle = if (isKeyhole) {
    recommendedTrainAngle.toFloat()  // 예: 166°
} else {
    0f
}
moveTrainToZero(trainAngle)
```

### Train 각도 계산 로직
- 목표: Azimuth/Elevation 각속도를 안테나 최대 속도 이하로 유지
- 방법: 패스 전체에서 각속도가 최소가 되는 Train 각도 탐색
- 결과: `RecommendedTrainAngle` 필드에 저장

## 주요 함수

| 함수 | 파일 | 역할 |
|------|------|------|
| `getTrackingPassMst` | EphemerisService.kt | Keyhole 여부에 따라 적절한 MST 선택 |
| `createRealtimeTrackingData` | EphemerisService.kt | 실시간 추적 데이터 생성 |
| `startEphemerisTracking` | EphemerisService.kt | 추적 시작 및 상태머신 초기화 |
| `handlePreparingState` | EphemerisService.kt | Train 각도 설정 및 이동 |
| `exportMstDataToCsv` | EphemerisService.kt | CSV 내보내기 |

## 2024-12-24 수정 사항

### 문제점 1: DataType 불일치
- `createRealtimeTrackingData`에서 `keyhole_final_transformed` (구버전) DataType을 사용
- 실제 데이터는 `keyhole_optimized_final_transformed` (신버전)으로 저장됨
- 결과: Keyhole 발생 시 데이터를 찾지 못함

### 수정 내용 1: DataType 통일
1. **createRealtimeTrackingData** (라인 1837)
   - `keyhole_final_transformed` → `keyhole_optimized_final_transformed`

2. **exportMstDataToCsv** (라인 4381, 4410)
   - `keyhole_final_transformed` → `keyhole_optimized_final_transformed`

3. **calculateFinalTransformedSumMethodRates 호출** (라인 3088)
   - `keyhole_final_transformed` → `keyhole_optimized_final_transformed`

### 문제점 2: 재추적 시 12.2 초기 데이터 미전송
- `sendInitialTrackingData`에서 `ephemerisStatus` 체크
- 재추적 플로우에서 `sendInitialTrackingData` 호출 시점에 `ephemerisStatus = true`가 아직 설정되지 않음
- 결과: 12.2 초기 데이터가 전송되지 않아 펌웨어가 추적을 시작하지 않음

### 수정 내용 2: 재추적 플로우 순서 수정
```kotlin
// startEphemerisTracking (라인 951-967)
if (isInTrackingTime) {
    // ✅ 상태를 먼저 설정 (sendInitialTrackingData에서 ephemerisStatus 체크하므로)
    currentTrackingState = TrackingState.TRACKING
    trackingStatus.ephemerisStatus = true
    trackingStatus.ephemerisTrackingState = "TRACKING"
    dataStoreService.setEphemerisTracking(true)
    dataStoreService.updateTrackingStatus(trackingStatus)  // ✅ WebSocket 전송용 상태 동기화

    // ✅ 상태 설정 후 12.1, 12.2 전송
    sendHeaderTrackingData(mstId, actualDetailId)
    sendInitialTrackingData(mstId, actualDetailId)
}
```

### 영향받는 기능
- 위성 추적 (Keyhole 패스)
- 위성 재추적 (중지 후 재시작)
- CSV 내보내기
- 속도 계산

## Fallback 처리

Keyhole 데이터가 없는 경우의 처리:

```kotlin
val cmdAz = if (isKeyhole && keyholeFinalTransformedAzimuth != null) {
    keyholeFinalTransformedAzimuth  // Keyhole 데이터 사용
} else {
    finalTransformedAzimuth  // fallback: Non-Keyhole 데이터 사용
}
```

> 경고 로그: "Keyhole 발생 시 keyhole_optimized_final_transformed 데이터가 없습니다. final_transformed로 fallback합니다."

## 테스트 방법

### Keyhole 패스 테스트
1. 최대 고도가 높은 위성 선택 (예: AQUA, 최대 고도 70° 이상)
2. 패스 생성 후 `IsKeyhole: true` 확인
3. 추적 시작 → Train이 `RecommendedTrainAngle`로 이동하는지 확인
4. 추적 중 CMD 값이 `keyhole_optimized_final_transformed` 데이터를 참조하는지 확인

### Non-Keyhole 패스 테스트
1. 최대 고도가 낮은 위성 선택
2. 패스 생성 후 `IsKeyhole: false` 확인
3. 추적 시작 → Train이 0°로 유지되는지 확인
4. 추적 중 CMD 값이 `final_transformed` 데이터를 참조하는지 확인

## 관련 문서

- [SYSTEM_OVERVIEW.md](../architecture/SYSTEM_OVERVIEW.md) - 시스템 전체 구조
- [Development_Guide.md](./Development_Guide.md) - 개발 가이드
- [API 명세](../api/README.md) - REST API 문서
