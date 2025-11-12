# Features (기능별 문서)

> 모든 기능의 계획 및 완료 문서를 통합 관리

---

## 📁 구조

```
features/
  active/                      # 진행 중인 기능
    {기능명}/
      README.md                # 기능 개요
      {기능명}_plan.md         # 원본 계획 문서
      (completed/ 폴더 없음)
  
  completed/                   # 완료된 기능
    {기능명}/
      README.md                # 기능 개요
      {기능명}_plan.md         # 원본 계획 문서
      completed/               # 완료 문서
        {기능명}_Final_Result.md     # 최종 구현 결과
        {기능명}_Summary.md           # 요약 문서
```

---

## 🔄 워크플로우

### 1. 계획 작성 (진행 중)
```
features/active/{기능명}/ 폴더 생성
features/active/{기능명}/{기능명}_plan.md 작성
features/active/{기능명}/README.md 작성
```

### 2. 작업 진행
- `active/{기능명}/{기능명}_plan.md` 유지하며 작업 진행
- 필요시 계획 업데이트
- `features/README.md`의 "진행 중" 섹션에 표시

### 3. 완료 처리
```
1. active/{기능명}/ → completed/{기능명}/ 이동
2. completed/{기능명}/completed/ 폴더 생성
3. completed/{기능명}_Final_Result.md 생성 (계획 + 수정사항 + 최종 결과)
4. completed/{기능명}_Summary.md 생성 (요약)
5. features/README.md의 "완료" 섹션으로 이동
```

---

## 📋 기능 목록

### 🔄 진행 중

| 기능명 | 시작일 | 담당 | 상태 |
|--------|--------|------|------|
| PassSchedule_Keyhole_Display_Enhancement | 2025-01 | - | 진행 중 |
| PassSchedule_Chart_Optimization | 2025-01 | - | 진행 중 |

---

### ✅ 완료

| 기능명 | 완료일 | 통합 여부 | 링크 |
|--------|--------|----------|------|
| Train_Algorithm | 2024-12 | - | [상세](./completed/Train_Algorithm/README.md) |
| PassScheduleService_Improvement | 2024-12 | ✅ (2개 통합) | [상세](./completed/PassScheduleService_Improvement/README.md) |
| Keyhole_Train_Angle_Management | 2024-12 | ✅ (2개 통합) | [상세](./completed/Keyhole_Train_Angle_Management/README.md) |
| Elevation_Filtering_Management | 2024-12 | ✅ (3개 통합) | [상세](./completed/Elevation_Filtering_Management/README.md) |
| Frontend_Display_Values_Validation | 2025-01 | - | [상세](./completed/Frontend_Display_Values_Validation/README.md) |

---

## 📝 새 기능 추가 시

### 진행 중인 기능 시작

1. `features/active/{기능명}/` 폴더 생성
2. `features/active/{기능명}/{기능명}_plan.md` 작성
3. `features/active/{기능명}/README.md` 작성
4. `features/README.md`의 "진행 중" 섹션에 추가
5. 작업 진행

### 완료 처리

1. `features/active/{기능명}/` → `features/completed/{기능명}/` 이동
2. `features/completed/{기능명}/completed/` 폴더 생성
3. `completed/{기능명}_Final_Result.md` 생성
4. `completed/{기능명}_Summary.md` 생성
5. `features/README.md`의 "완료" 섹션으로 이동

---

## 🎯 상태 구분 방법

### 진행 중 (active/)
- `completed/` 폴더가 없음
- `{기능명}_plan.md`만 존재
- 작업 중인 상태

### 완료 (completed/)
- `completed/` 폴더가 있음
- `completed/{기능명}_Final_Result.md` 존재
- `completed/{기능명}_Summary.md` 존재
- 구현 완료 상태

---

**문서 버전**: 2.0.0  
**최종 업데이트**: 2025-01
