# PassSchedule Keyhole 표시 개선 계획

---
**작성일**: 2025-11-12
**작성자**: GTL Systems
**상태**: 진행 중
**위치**: `docs/features/active/PassSchedule_Keyhole_Display_Enhancement/PassSchedule_Keyhole_Display_Enhancement_plan.md`
---

## 목표

PassSchedulePage에서 keyhole 위성 정보를 더 명확하게 표시합니다.

## 접근 방식

1. **Schedule Control 테이블**: "작업" 컬럼을 "keyhole"로 변경하고 keyhole 배지 표시
2. **Schedule Information**: 상태 배지 옆에 keyhole 배지 추가

> **참고**: Position View 차트 최적화는 별도 리팩토링 계획서로 분리되었습니다.  
> 자세한 내용은 [PassSchedule 차트 최적화 리팩토링](../PassSchedule_Chart_Optimization/PassSchedule_Chart_Optimization_plan.md) 문서를 참조하세요.

## 현재 문제점

- Schedule Control 테이블에서 keyhole 위성 여부를 확인하기 어려움
- Schedule Information에 keyhole 정보가 표시되지 않음

## 수정 목표

- Schedule Control 테이블에 keyhole 컬럼 추가 및 배지 표시
- Schedule Information에 keyhole 배지 추가

## 장점

- keyhole 위성 정보를 한눈에 확인 가능
- 사용자 경험 개선 (명확한 정보 표시)

---

## Phase 1: Schedule Control 테이블에 Keyhole 표시 추가

### 목표

Schedule Control 테이블의 "작업" 컬럼을 "keyhole"로 변경하고, keyhole 위성인 경우 KEYHOLE 배지를 표시합니다.

### 작업 내용

**파일**: `ACS/src/pages/mode/PassSchedulePage.vue`

1. **컬럼 정의 수정** (라인 1114)
   - `scheduleColumns` 배열에서 `actions` 컬럼을 `keyhole`로 변경
   - `label`을 "keyhole"로 변경
   - `field`를 `keyhole`로 변경

2. **템플릿 수정** (라인 273-279)
   - `body-cell-actions` 템플릿을 `body-cell-keyhole`로 변경
   - 삭제 버튼 제거
   - `props.row.isKeyhole` 또는 `props.row.IsKeyhole` 확인하여 KEYHOLE 배지 표시

### 구현 세부사항

```typescript
// 컬럼 정의
{ name: 'keyhole', label: 'keyhole', field: 'keyhole', align: 'center' as const, sortable: false, style: 'width: 80px' }

// 템플릿
<template v-slot:body-cell-keyhole="props">
  <q-td :props="props">
    <q-badge v-if="props.row.isKeyhole || props.row.IsKeyhole" color="red" label="KEYHOLE" />
  </q-td>
</template>
```

---

## Phase 2: Schedule Information에 Keyhole 표시 추가

### 목표

Schedule Information 영역의 상태 배지 옆에 keyhole 배지를 추가합니다.

### 작업 내용

**파일**: `ACS/src/pages/mode/PassSchedulePage.vue`

1. **배지 추가** (라인 155-160)
   - 스케줄 ID/상태 표시 부분에 keyhole 배지 추가
   - `displaySchedule.value.isKeyhole` 또는 `displaySchedule.value.IsKeyhole` 확인
   - 상태 배지 옆에 빨간색 "KEYHOLE" 배지 표시

### 구현 세부사항

```vue
<div class="info-value-with-badge">
  <span class="info-value">{{ displaySchedule.no }}</span>
  <q-badge v-if="currentScheduleStatus" :color="currentScheduleStatus.color"
    :label="currentScheduleStatus.label" class="q-ml-sm" />
  <q-badge v-if="displaySchedule.isKeyhole || displaySchedule.IsKeyhole" color="red" label="KEYHOLE" class="q-ml-sm" />
</div>
```

---

> **참고**: Position View 차트 최적화 관련 내용은 별도 리팩토링 계획서로 분리되었습니다.  
> 자세한 내용은 [PassSchedule 차트 최적화 리팩토링](../PassSchedule_Chart_Optimization/PassSchedule_Chart_Optimization_plan.md) 문서를 참조하세요.

---

## 예상 변경 파일

- `ACS/src/pages/mode/PassSchedulePage.vue` (주요 수정 파일)

## 테스트 항목

1. Keyhole 위성 선택 시 Schedule Control 테이블에 KEYHOLE 배지 표시 확인
2. Schedule Information에 keyhole 배지 표시 확인

> **참고**: 차트 관련 테스트 항목은 [PassSchedule 차트 최적화 리팩토링](../PassSchedule_Chart_Optimization/PassSchedule_Chart_Optimization_plan.md) 문서를 참조하세요.

---

## 참고

- Keyhole 위성 판단은 `isKeyhole` 또는 `IsKeyhole` 필드를 사용
- 백엔드에서 제공하는 데이터 구조에 따라 필드명이 다를 수 있음
- 차트 최적화 관련 내용은 [PassSchedule 차트 최적화 리팩토링](../PassSchedule_Chart_Optimization/PassSchedule_Chart_Optimization_plan.md) 문서 참조

