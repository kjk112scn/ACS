# TrackingTrajectory DB Null Primitive 오류 수정

## 현재 상태: ✅ 수정 완료

## 개요

| 항목 | 내용 |
|------|------|
| **문제** | DB NULL 컬럼을 primitive double로 읽으려 시도 |
| **심각도** | 🔴 Critical |
| **영향** | 세션 로딩 실패 (28, 30, 33, 35, 36, 37, 38, 41) |

## 증상

```
ERROR c.g.a.s.m.p.PassScheduleDataRepository - [DB→메모리] 세션 37 trajectory 로딩 실패:
Value at column 'train' is null. Cannot return value for primitive 'double'
at TrackingTrajectoryRepository.findBySessionId$lambda$1(TrackingTrajectoryRepository.kt:71)
```

## 원인

| 위치 | 코드 | 문제 |
|------|------|------|
| Repository:71 | `Double::class.java` | primitive double → null 불가 |
| Entity:38 | `train: Double? = null` | nullable ✅ |

**Kotlin 타입 매핑:**
- `Double::class.java` → primitive `double` (null 불가)
- `Double::class.javaObjectType` → boxed `java.lang.Double` (null 허용)

## 수정 내용

### TrackingTrajectoryRepository.kt

```diff
- train = row.get("train", Double::class.java),
- azimuthRate = row.get("azimuth_rate", Double::class.java),
- elevationRate = row.get("elevation_rate", Double::class.java),
+ train = row.get("train", Double::class.javaObjectType),
+ azimuthRate = row.get("azimuth_rate", Double::class.javaObjectType),
+ elevationRate = row.get("elevation_rate", Double::class.javaObjectType),
```

**수정 위치:** 3곳
- `findBySessionId` (line 71-73)
- `findByTimeRange` (line 130-132)
- `mapRowToEntity` (line 151-153)

## 테스트

- [x] 빌드 성공
- [ ] 세션 로딩 정상 동작
- [ ] null train 값 처리 정상

## 수정 파일

| 파일 | 변경 |
|------|------|
| `TrackingTrajectoryRepository.kt` | javaObjectType 사용 |

## 재발 방지

| 대책 | 적용 |
|------|:----:|
| nullable 컬럼은 `javaObjectType` 사용 | ✅ |
| R2DBC 매핑 시 타입 확인 | ✅ |
