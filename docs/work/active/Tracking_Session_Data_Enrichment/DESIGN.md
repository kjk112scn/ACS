# Tracking_Session_Data_Enrichment 설계 문서

## 1. 설계 의도

### Why (왜 이렇게 설계했는가)

tracking_session 테이블은 위성 추적 세션의 메타데이터를 저장하는 테이블입니다.
현재 일부 컬럼들이 null/빈값으로 저장되어 데이터 활용도가 떨어집니다.

이 값들은 이미 존재하는 데이터에서 계산/추출 가능하므로, DB 저장 시점에 채워넣습니다.

### 대안 분석

| 대안 | 장점 | 단점 | 선택 여부 |
|------|------|------|----------|
| A. Repository에서 계산 | 간단, 영향 최소 | 소스 데이터 접근 제한적 | ❌ |
| B. DataStore에서 MST 생성 시 채움 | 원본 데이터 접근 용이 | 변경 범위 넓음 | ✅ 선택 |
| C. DB 트리거로 자동 계산 | 일관성 보장 | TimescaleDB 복잡성 | ❌ |

**선택 이유**: DataStore에서 MST 데이터를 생성할 때 DTL 데이터에 접근 가능하므로, 이 시점에 계산하는 것이 가장 자연스럽습니다.

## 2. 구현 계획

### 2.1 컬럼별 계산 방법

| 컬럼 | 계산 방법 | 소스 |
|------|----------|------|
| satellite_id | TLE NORAD ID 추출 | TLE line 1 (3~7번째 문자) |
| duration | `ChronoUnit.SECONDS.between(startTime, endTime)` | startTime, endTime |
| max_azimuth_rate | DTL 데이터에서 azimuthRate 최대값 | dtlList |
| max_elevation_rate | DTL 데이터에서 elevationRate 최대값 | dtlList |
| total_points | DTL 리스트 크기 | dtlList.size |

### 2.2 수정 위치

**EphemerisDataStore.kt** (또는 EphemerisDataRepository.kt):
```kotlin
// MST 데이터 생성 시
val mst = mapOf(
    "MstId" to mstId,
    "SatelliteId" to extractNoradId(tleLine1),  // 추가
    "SatelliteName" to satelliteName,
    "Duration" to ChronoUnit.SECONDS.between(startTime, endTime).toInt(),  // 추가
    "MaxAzimuthRate" to dtlList.maxOfOrNull { it.azimuthRate },  // 추가
    "MaxElevationRate" to dtlList.maxOfOrNull { it.elevationRate },  // 추가
    "TotalPoints" to dtlList.size,  // 추가
    // ... 기존 필드들
)
```

### 2.3 NORAD ID 추출 함수

```kotlin
/**
 * TLE Line 1에서 NORAD ID 추출
 * 형식: 1 25544U 98067A   ...
 *       ↑ ^^^^^
 *       1 NORAD (2~6번째 문자, 0-indexed)
 */
private fun extractNoradId(tleLine1: String): String {
    return tleLine1.substring(2, 7).trim()
}
```

## 3. 테스트 계획

- [ ] EphemerisDataStore에서 MST 생성 후 값 확인
- [ ] PassScheduleDataStore에서 MST 생성 후 값 확인
- [ ] DB 저장 후 tracking_session 테이블 컬럼 확인

## 4. 관련 ADR

- 없음 (기존 구조 내 데이터 보강)
