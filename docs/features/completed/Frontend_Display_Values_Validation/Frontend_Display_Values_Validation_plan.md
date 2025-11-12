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

