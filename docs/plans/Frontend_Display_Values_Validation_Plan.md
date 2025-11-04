# 프론트엔드 표시 값 검증 및 수정 계획

---
**작성일**: 2025-01-02
**작성자**: GTL Systems
**상태**: 협의 중
**위치**: `docs/plans/Frontend_Display_Values_Validation_Plan.md`
---

## 목표

프론트엔드에 표시해야 하는 모든 값들을 전체적으로 검증하고, 백엔드에서 모든 DataType 데이터를 제공하고 프론트엔드에서 Keyhole 여부에 따라 적절한 값을 선택하여 표시하도록 수정합니다.

## 접근 방식

1. **백엔드**: 모든 DataType의 데이터를 모두 제공 (조건부 로직 최소화)
2. **프론트엔드**: Keyhole 여부에 따라 적절한 값을 선택하여 표시
3. **단계별 진행**: 하나씩 추가하여 최종 단계까지 확장

## 현재 문제점

- 백엔드에서 일부 데이터만 제공하고, Keyhole 여부에 따른 조건부 처리를 백엔드에서 수행
- 프론트엔드에서 Keyhole 데이터를 확인하기 어려움
- 표시해야 하는 값들이 여러 곳에 분산되어 있음

## 수정 목표

- 백엔드: 모든 DataType의 데이터를 모두 제공 (조건부 로직 제거)
- 프론트엔드: Keyhole 여부에 따라 적절한 값을 선택하여 표시

## 장점

- 백엔드 로직 단순화 (조건부 로직 제거)
- 프론트엔드에서 유연하게 처리 가능
- 디버깅 시 모든 데이터 확인 가능
- 나중에 다른 기준으로 선택할 때도 유연함
- 단계별 확장 가능

## 데이터 분리 및 충돌 분석

### 1. 백엔드 데이터 저장 구조

#### 저장소 구조
- **`ephemerisTrackMstStorage`**: 모든 DataType의 MST 데이터를 포함하는 단일 리스트
- **각 MST 레코드**:
  - `DataType` 필드로 구분: `"original"`, `"final_transformed"`, `"keyhole_final_transformed"` 등
  - `No` 필드로 패스 구분: 동일 패스는 동일한 `No` 값 (예: 패스 #1의 original, final_transformed, keyhole_final_transformed 모두 `No = 1`)

#### 데이터 저장 예시
```kotlin
// ephemerisTrackMstStorage에 저장되는 구조:
[
  { "No": 1, "DataType": "original", "StartAzimuth": 100.0, ... },
  { "No": 1, "DataType": "final_transformed", "StartAzimuth": 105.0, ... },
  { "No": 1, "DataType": "keyhole_final_transformed", "StartAzimuth": 110.0, ... },
  { "No": 2, "DataType": "original", "StartAzimuth": 200.0, ... },
  { "No": 2, "DataType": "final_transformed", "StartAzimuth": 205.0, ... },
  // Keyhole 아닐 경우 keyhole_final_transformed 없음
]
```

### 2. 백엔드 데이터 조회 로직

#### 필터링 (Line 2066-2076)
```kotlin
val originalMst = ephemerisTrackMstStorage.filter { it["DataType"] == "original" }
val finalMst = ephemerisTrackMstStorage.filter { it["DataType"] == "final_transformed" }
val keyholeMst = ephemerisTrackMstStorage.filter { it["DataType"] == "keyhole_final_transformed" }
```
- **DataType으로 필터링**: 각 DataType별로 별도 리스트 생성
- **결과**: 완전히 분리된 리스트들 (섞이지 않음)

#### 패스 매칭 (Line 2076-2080)
```kotlin
val mergedData = finalMst.map { final ->
    val mstId = final["No"] as UInt
    val keyhole = keyholeMst.find { it["No"] == mstId }
}
```
- **기준**: `finalMst`를 기준으로 루프
- **매칭**: 각 패스의 `No` (MstId)로 `keyholeMst`에서 해당 패스 찾기
- **결과**: 같은 패스의 데이터만 매칭됨 (다른 패스와 섞이지 않음)

### 3. 데이터 병합 로직 (Line 2097-2140)

#### 필드명 분리 구조
```kotlin
final.toMutableMap().apply {
    // 기존 필드 (final_transformed MST의 원본 필드)
    // - StartAzimuth, EndAzimuth, StartElevation, EndElevation, MaxElevation
    // - 이 필드들은 final 객체에 이미 포함되어 있음
    
    // 새 필드 (명시적 데이터 소스 표시)
    put("FinalTransformedStartAzimuth", final["StartAzimuth"])  // ← final에서 가져옴
    put("FinalTransformedMaxElevation", final["MaxElevation"])  // ← final에서 가져옴
    
    put("KeyholeFinalTransformedStartAzimuth", keyhole?.get("StartAzimuth"))  // ← keyhole에서 가져옴
    put("KeyholeFinalTransformedMaxElevation", keyhole?.get("MaxElevation"))  // ← keyhole에서 가져옴
}
```

#### 필드명 충돌 분석

**충돌 없음 - 이유:**

1. **기존 필드 유지**:
   - `StartAzimuth`, `EndAzimuth` 등은 `final` 객체의 원본 필드
   - `final.toMutableMap()`으로 복사되어 그대로 유지

2. **새 필드 추가**:
   - `FinalTransformedStartAzimuth`: 새 필드명 (충돌 없음)
   - `KeyholeFinalTransformedStartAzimuth`: 새 필드명 (충돌 없음)

3. **명시적 데이터 소스**:
   - `FinalTransformed*`: `final_transformed` MST에서 온 값임을 명시
   - `KeyholeFinalTransformed*`: `keyhole_final_transformed` MST에서 온 값임을 명시

4. **데이터 소스 분리**:
   ```kotlin
   // final 객체에서 가져오는 값
   put("FinalTransformedStartAzimuth", final["StartAzimuth"])
   
   // keyhole 객체에서 가져오는 값 (별도 객체, 별도 메모리)
   put("KeyholeFinalTransformedStartAzimuth", keyhole?.get("StartAzimuth"))
   ```
   - `final`과 `keyhole`는 **별도의 Map 객체**
   - `final["StartAzimuth"]`와 `keyhole["StartAzimuth"]`는 **서로 다른 메모리 위치**
   - 같은 필드명이지만 다른 객체에서 가져오므로 충돌 없음

### 4. 데이터 무결성 검증

#### 시나리오 분석

**시나리오 1: Keyhole 발생 패스**
```kotlin
// 패스 #1이 Keyhole인 경우
final = { "No": 1, "DataType": "final_transformed", "StartAzimuth": 105.0, ... }
keyhole = { "No": 1, "DataType": "keyhole_final_transformed", "StartAzimuth": 110.0, ... }

// 병합 결과:
{
  "StartAzimuth": 105.0,  // ← final 객체의 값 (하위 호환성)
  "FinalTransformedStartAzimuth": 105.0,  // ← final 객체의 값
  "KeyholeFinalTransformedStartAzimuth": 110.0  // ← keyhole 객체의 값 (다른 값!)
}
```
- **결과**: ✅ 데이터 분리 정확, 충돌 없음

**시나리오 2: Keyhole 미발생 패스**
```kotlin
// 패스 #2가 Keyhole 아닌 경우
final = { "No": 2, "DataType": "final_transformed", "StartAzimuth": 205.0, ... }
keyhole = null  // ← keyhole_final_transformed MST에 없음

// 병합 결과:
{
  "StartAzimuth": 205.0,  // ← final 객체의 값
  "FinalTransformedStartAzimuth": 205.0,  // ← final 객체의 값
  "KeyholeFinalTransformedStartAzimuth": null  // ← keyhole 없음
}
```
- **결과**: ✅ 데이터 분리 정확, null 처리 안전

**시나리오 3: 여러 패스 혼합**
```kotlin
// 패스 #1 (Keyhole), 패스 #2 (일반), 패스 #3 (Keyhole)
finalMst = [
  { "No": 1, "StartAzimuth": 105.0 },
  { "No": 2, "StartAzimuth": 205.0 },
  { "No": 3, "StartAzimuth": 305.0 }
]
keyholeMst = [
  { "No": 1, "StartAzimuth": 110.0 },
  { "No": 3, "StartAzimuth": 310.0 }
]

// 각 패스별로 find로 매칭:
// 패스 #1: keyhole = { "No": 1, "StartAzimuth": 110.0 } ✅ 정확한 매칭
// 패스 #2: keyhole = null ✅ 정확한 매칭
// 패스 #3: keyhole = { "No": 3, "StartAzimuth": 310.0 } ✅ 정확한 매칭
```
- **결과**: ✅ 패스별로 정확히 매칭됨, 섞이지 않음

### 5. 잠재적 문제점 및 해결책

#### 문제점 1: 기존 필드와 새 필드 혼용
**문제**: 기존 `StartAzimuth` 필드도 있고, 새로운 `FinalTransformedStartAzimuth` 필드도 있어 혼란 가능

**해결책**: 
- 기존 필드는 하위 호환성을 위해 유지
- 프론트엔드에서는 새로운 명시적 필드(`FinalTransformedStartAzimuth` 등) 우선 사용
- 기존 필드는 fallback으로만 사용

#### 문제점 2: Keyhole 없을 때 null 값
**문제**: `keyhole_final_transformed` MST가 없으면 `KeyholeFinalTransformedStartAzimuth`가 null

**해결책**:
- 프론트엔드에서 null 체크 후 `FinalTransformedStartAzimuth` 사용
- 또는 백엔드에서 Keyhole 아닐 경우 `FinalTransformed*` 값으로 채우기 (선택적)

#### 문제점 3: 필드명 일관성
**문제**: 일부는 `FinalTransformed*`, 일부는 기존 필드명 유지

**권장사항**:
- 점진적 마이그레이션: 기존 필드는 하위 호환성 유지
- 새로운 코드에서는 명시적 필드명 사용
- 장기적으로 기존 필드 deprecate 고려

### 6. 결론

#### 데이터 분리: ✅ 완벽하게 분리됨
1. **DataType 필터링**: DataType으로 완전히 분리된 리스트
2. **패스 매칭**: `No` (MstId) 기반으로 정확한 패스 매칭
3. **필드명 분리**: 명시적 접두사로 데이터 소스 명확히 표시
4. **객체 분리**: `final`과 `keyhole`는 별도 객체, 메모리 충돌 없음

#### 데이터 충돌: ✅ 충돌 없음
1. **필드명 충돌 없음**: 새 필드명 사용 (`FinalTransformed*`, `KeyholeFinalTransformed*`)
2. **메모리 충돌 없음**: 서로 다른 객체에서 가져옴
3. **값 충돌 없음**: 같은 패스 내에서도 명시적으로 구분됨

#### 안전성: ✅ 높음
1. **null 안전 처리**: `keyhole?.get()` 사용으로 null 안전
2. **타입 안전성**: Kotlin의 타입 시스템으로 런타임 에러 방지
3. **데이터 무결성**: 패스별 정확한 매칭

---

## Phase 1: StartAzimuth/EndAzimuth 시작/종료 각도 및 MaxElevation

### 목표

시작/종료 각도(StartAzimuth, EndAzimuth, StartElevation, EndElevation)와 최대 고도(MaxElevation)를 백엔드에서 모든 DataType으로 제공하고, 프론트엔드에서 Keyhole 여부에 따라 선택

### 현재 상태 분석

#### 백엔드 (`EphemerisService.kt`)

**위치**: `getAllEphemerisTrackMstMerged()` 함수 (Line 2076-2139)

**현재 문제점**:
- `final` 객체는 `final_transformed` MST 데이터
- `final.toMutableMap().apply { ... }`에서 `final` 객체에 기본적으로 포함된 필드들이 그대로 사용됨
- 즉, 다음 필드들이 **항상 `final_transformed` MST**에서 가져온 값:
  - `StartAzimuth` → `final["StartAzimuth"]` (final_transformed, Train=0, ±270°)
  - `EndAzimuth` → `final["EndAzimuth"]` (final_transformed, Train=0, ±270°)
  - `StartElevation` → `final["StartElevation"]` (final_transformed, Train=0, ±270°)
  - `EndElevation` → `final["EndElevation"]` (final_transformed, Train=0, ±270°)
  - `MaxElevation` → `final["MaxElevation"]` (final_transformed, Train=0, ±270°)

#### 프론트엔드

**`ephemerisTrackService.ts`** (Line 419-423):
- 백엔드 응답의 `StartAzimuth`, `EndAzimuth`, `StartElevation`, `EndElevation`을 그대로 사용
- 즉, **항상 `final_transformed` 값** 사용

**`EphemerisDesignationPage.vue`** (Line 875, 878-881):
- `selected.MaxElevation` → `final_transformed` 값 사용
- `selected.StartAzimuth`, `EndAzimuth`, `StartElevation`, `EndElevation` → `final_transformed` 값 사용

#### 문제점

1. **Keyhole일 때도 `final_transformed` (Train=0, ±270°) 값이 사용됨**
2. **Keyhole일 때는 `keyhole_final_transformed` (Train≠0, ±270°) 값이 사용되어야 함**
3. **`MaxElevation`도 마찬가지로 Keyhole일 때 다른 값이 필요함**

#### 결론

- 현재: `final_transformed` MST의 3축 변환 값 사용 (Train=0, ±270°)
- 요구: Keyhole일 때는 `keyhole_final_transformed` MST의 값 사용 (Train≠0, ±270°)
- 해결: 백엔드에서 두 DataType의 값을 모두 제공하고, 프론트엔드에서 Keyhole 여부에 따라 선택

### 백엔드 수정

**파일**: `E:\001.GTL\SW\ACS_API\src\main\kotlin\com\gtlsystems\acs_api\service\mode\EphemerisService.kt`

**위치**: `getAllEphemerisTrackMstMerged()` 함수 (Line 2097-2139)

**작업 내용**:
`final.toMutableMap().apply { ... }` 블록에 다음 필드 추가:

```kotlin
// ✅ FinalTransformed 시작/종료 각도 및 최대 고도 (Train=0, ±270°)
// 항상 final_transformed MST의 값 제공
put("FinalTransformedStartAzimuth", final["StartAzimuth"])
put("FinalTransformedEndAzimuth", final["EndAzimuth"])
put("FinalTransformedStartElevation", final["StartElevation"])
put("FinalTransformedEndElevation", final["EndElevation"])
put("FinalTransformedMaxElevation", final["MaxElevation"])

// ✅ KeyholeFinalTransformed 시작/종료 각도 및 최대 고도 (Train≠0, ±270°)
// 항상 keyhole_final_transformed MST의 값 제공 (없으면 null)
// 조건부 로직 없이 항상 제공 - 프론트엔드에서 선택
put("KeyholeFinalTransformedStartAzimuth", keyhole?.get("StartAzimuth"))
put("KeyholeFinalTransformedEndAzimuth", keyhole?.get("EndAzimuth"))
put("KeyholeFinalTransformedStartElevation", keyhole?.get("StartElevation"))
put("KeyholeFinalTransformedEndElevation", keyhole?.get("EndElevation"))
put("KeyholeFinalTransformedMaxElevation", keyhole?.get("MaxElevation"))
```

**중요**: 백엔드는 조건부 로직 없이 항상 모든 데이터를 제공합니다. Keyhole 여부에 따른 선택은 프론트엔드에서 처리합니다.

**기존 `StartAzimuth`, `EndAzimuth`, `StartElevation`, `EndElevation`, `MaxElevation`는 그대로 유지 (하위 호환성)**

### 프론트엔드 수정

#### 1. TypeScript 인터페이스 수정

**파일**: `c:\Users\NG2\source\repos\VueQuasar\ACS\src\services\mode\ephemerisTrackService.ts`

**작업 내용**:
`ScheduleItem` 인터페이스에 다음 필드 추가:

```typescript
// ✅ FinalTransformed 시작/종료 각도 및 최대 고도 (Train=0, ±270°)
FinalTransformedStartAzimuth?: number
FinalTransformedEndAzimuth?: number
FinalTransformedStartElevation?: number
FinalTransformedEndElevation?: number
FinalTransformedMaxElevation?: number

// ✅ KeyholeFinalTransformed 시작/종료 각도 및 최대 고도 (Train≠0, ±270°, Keyhole일 경우만)
KeyholeFinalTransformedStartAzimuth?: number
KeyholeFinalTransformedEndAzimuth?: number
KeyholeFinalTransformedStartElevation?: number
KeyholeFinalTransformedEndElevation?: number
KeyholeFinalTransformedMaxElevation?: number
```

#### 2. API 매핑 수정

**파일**: `c:\Users\NG2\source\repos\VueQuasar\ACS\src\services\mode\ephemerisTrackService.ts`

**위치**: `fetchEphemerisMasterData()` 함수 (Line 400-450)

**작업 내용**:
`mappedData` 매핑에 새 필드 추가:

```typescript
// ✅ FinalTransformed 시작/종료 각도 및 최대 고도
FinalTransformedStartAzimuth: item.FinalTransformedStartAzimuth as number | undefined,
FinalTransformedEndAzimuth: item.FinalTransformedEndAzimuth as number | undefined,
FinalTransformedStartElevation: item.FinalTransformedStartElevation as number | undefined,
FinalTransformedEndElevation: item.FinalTransformedEndElevation as number | undefined,
FinalTransformedMaxElevation: item.FinalTransformedMaxElevation as number | undefined,

// ✅ KeyholeFinalTransformed 시작/종료 각도 및 최대 고도
KeyholeFinalTransformedStartAzimuth: item.KeyholeFinalTransformedStartAzimuth as number | undefined,
KeyholeFinalTransformedEndAzimuth: item.KeyholeFinalTransformedEndAzimuth as number | undefined,
KeyholeFinalTransformedStartElevation: item.KeyholeFinalTransformedStartElevation as number | undefined,
KeyholeFinalTransformedEndElevation: item.KeyholeFinalTransformedEndElevation as number | undefined,
KeyholeFinalTransformedMaxElevation: item.KeyholeFinalTransformedMaxElevation as number | undefined,
```

#### 3. 표시 로직 수정

**파일**: `c:\Users\NG2\source\repos\VueQuasar\ACS\src\pages\mode\EphemerisDesignationPage.vue`

**위치**: `selectedScheduleInfo` computed property (Line 865-893)

**작업 내용**:
Keyhole 여부에 따라 적절한 값 선택:

```typescript
// 시작/종료 각도 및 최대 고도: Keyhole 여부에 따라 선택
startAzimuth: selected.isKeyhole 
  ? (selected.KeyholeFinalTransformedStartAzimuth ?? selected.FinalTransformedStartAzimuth ?? selected.StartAzimuth ?? 0)
  : (selected.FinalTransformedStartAzimuth ?? selected.StartAzimuth ?? 0),
endAzimuth: selected.isKeyhole 
  ? (selected.KeyholeFinalTransformedEndAzimuth ?? selected.FinalTransformedEndAzimuth ?? selected.EndAzimuth ?? 0)
  : (selected.FinalTransformedEndAzimuth ?? selected.EndAzimuth ?? 0),
startElevation: selected.isKeyhole 
  ? (selected.KeyholeFinalTransformedStartElevation ?? selected.FinalTransformedStartElevation ?? selected.StartElevation ?? 0)
  : (selected.FinalTransformedStartElevation ?? selected.StartElevation ?? 0),
endElevation: selected.isKeyhole 
  ? (selected.KeyholeFinalTransformedEndElevation ?? selected.FinalTransformedEndElevation ?? selected.EndElevation ?? 0)
  : (selected.FinalTransformedEndElevation ?? selected.EndElevation ?? 0),
maxElevation: selected.isKeyhole 
  ? (selected.KeyholeFinalTransformedMaxElevation ?? selected.FinalTransformedMaxElevation ?? selected.MaxElevation ?? 0)
  : (selected.FinalTransformedMaxElevation ?? selected.MaxElevation ?? 0),
```

---

## Phase 2: (향후 추가 예정)

- 추가할 검증 항목들이 있으면 여기에 추가
- 단계별로 하나씩 추가하여 최종 단계까지 확장

---

## Phase 3: (향후 추가 예정)

- 추가할 검증 항목들이 있으면 여기에 추가
- 단계별로 하나씩 추가하여 최종 단계까지 확장

---

## 최종 단계: 전체 검증 및 정리

- 모든 표시 값들의 일관성 검증
- 문서화 및 테스트
- 완료 문서 작성 (`docs/completed/Frontend_Display_Values_Validation_Completed.md`)

---

## 최종 검토 결과

### ✅ 검토 완료 항목

#### 1. 백엔드 수정 위치
- **파일**: `EphemerisService.kt`
- **위치**: Line 2131 이후 (Keyhole 관련 정보 설정 전)
- **확인**: `keyhole` 객체는 이미 조회되어 있음 (Line 2080)
- **필드명**: 정확함 (`FinalTransformed*`, `KeyholeFinalTransformed*`)
- **결론**: ✅ 문제없음

#### 2. 프론트엔드 수정 위치
- **인터페이스**: `ScheduleItem`에 필드 추가 (Line 66-70 이후)
- **API 매핑**: `fetchEphemerisMasterData()`에 매핑 추가 (Line 419-423 이후)
- **표시 로직**: `selectedScheduleInfo`에서 선택 로직 (Line 865-893)
- **결론**: ✅ 문제없음

#### 3. 데이터 흐름
- **백엔드**: 조건부 로직 없이 모든 데이터 제공
- **프론트엔드**: Keyhole 여부에 따라 선택
- **결론**: ✅ 문제없음

#### 4. 필드명 일관성
- **백엔드**: `FinalTransformedStartAzimuth`, `KeyholeFinalTransformedStartAzimuth`
- **프론트엔드**: 동일한 필드명 사용
- **결론**: ✅ 일치함

#### 5. Null 안전 처리
- **백엔드**: `keyhole?.get()` 사용 (null 안전)
- **프론트엔드**: `??` 연산자로 fallback 처리 (null 안전)
- **결론**: ✅ 안전함

### ⚠️ 주의 사항

#### 1. 템플릿에서 toFixed() 직접 호출
**위치**: `EphemerisDesignationPage.vue` Line 211-213, 218

**현재 코드**:
```vue
{{ selectedScheduleInfo.startAzimuth.toFixed(6) }}°
```

**문제점**: `selectedScheduleInfo.startAzimuth`가 null이면 에러 발생 가능

**해결책**: 
- `selectedScheduleInfo`에서 fallback을 0으로 설정하므로 안전함
- 더 안전하게 하려면 `safeToFixed()` 사용 권장:
```vue
{{ safeToFixed(selectedScheduleInfo.startAzimuth, 6) }}°
```

**권장사항**: 현재는 안전하지만, 향후 변경 시 `safeToFixed()` 사용 권장

#### 2. 수정 순서
1. **백엔드 수정** (먼저 수행)
   - 백엔드에서 새 필드 제공
2. **프론트엔드 인터페이스 수정** (다음)
   - TypeScript 인터페이스에 필드 추가
3. **프론트엔드 API 매핑 수정** (다음)
   - API 응답 매핑 추가
4. **프론트엔드 표시 로직 수정** (마지막)
   - `selectedScheduleInfo`에서 선택 로직 수정

**중요**: 백엔드 수정 후 배포/테스트 후 프론트엔드 수정 진행 권장

#### 3. 하위 호환성
- 기존 필드(`StartAzimuth`, `EndAzimuth` 등)는 유지
- 기존 코드와의 호환성 보장
- 점진적 마이그레이션 가능

### ✅ 최종 결론

**작업 수행 가능 여부**: ✅ **문제없음**

**이유**:
1. 데이터 분리: ✅ 완벽하게 분리됨
2. 필드명 충돌: ✅ 충돌 없음
3. 수정 위치: ✅ 정확함
4. 안전성: ✅ null 처리 안전
5. 하위 호환성: ✅ 기존 필드 유지

**작업 순서**: 백엔드 → 프론트엔드 인터페이스 → API 매핑 → 표시 로직

**주의사항**: 
- 백엔드 수정 후 프론트엔드 수정 진행
- 템플릿에서 `safeToFixed()` 사용 권장 (선택사항)

---

## 참조 문서

- Train 각도 알고리즘: `docs/completed/Train_Algorithm_Completed.md`
- Train 각도 설계: `docs/references/algorithms/Train_Algorithm_Design.md`

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2025-01-02  
**최종 검토**: 2025-01-02
