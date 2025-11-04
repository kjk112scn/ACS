# Plans (계획 단계 문서)

> 협의 중인 작업 계획 문서

---

## 📝 용도

새로운 기능이나 개선 작업을 시작할 때 사용자와 협의하며 작성하는 계획서

### 특징
- **Step by Step 형태**: 작업 순서 명확화
- **코드 예시 포함**: 구체적인 구현 방법
- **파일/라인 명시**: 수정할 위치 정확히 표시
- **지속적 업데이트**: 협의하며 계속 수정

---

## 📁 파일 명명 규칙

```
{기능명}_Plan.md

예시:
- Train_Algorithm_Plan.md
- Settings_Management_Plan.md
- WebSocket_Optimization_Plan.md
```

---

## 📋 계획서 템플릿

```markdown
# {기능명} 구현 계획

---
**작성일**: 2024-12-15
**작성자**: GTL Systems
**상태**: 협의 중
---

## 목표

{무엇을 왜 구현하는가}

## 배경

{왜 이 기능이 필요한가}

## Step 1: {단계 제목}

**목적**: {이 단계에서 달성할 것}

**파일**: `경로/파일명.kt`
**수정 위치**: Line 100-150

**수정 내용**:
```kotlin
// 코드 예시
```

**검증 방법**:
- 테스트 케이스
- 예상 결과

## Step 2: {다음 단계}

...

## 예상 결과

{최종 목표 상태}

## 리스크

{예상되는 문제점}

## 다음 단계

{이후 작업 계획}
```

---

## 🔄 계획서 진행 상태

| 파일명 | 상태 | 시작일 | 완료일 |
|--------|------|--------|--------|
| - | - | - | - |

---

## ✅ 계획서 → 완료 문서 (자동 워크플로우)

### 완료 처리 프로세스

작업이 완료되면 다음 명령어 중 하나를 사용하세요:

**완료 판단 키워드**:
- "이 계획은 완료되었어"
- "계획을 완료처리해"
- "이 플랜 완료해줘"
- "{기능명} 플랜 완료"

**자동 처리**:
```
1. docs/plans/{기능명}_Plan.md 읽기
2. docs/completed/{기능명}_Completed.md 생성
   - 파일명 변환: {기능명}_Plan.md → {기능명}_Completed.md
   - 완료일, 상태 메타데이터 추가
   - 완료 문서 템플릿 적용
3. 원본 플랜 파일 삭제 (docs/plans/에서 제거)
4. README.md 업데이트
```

### 파일명 변환 규칙

| 원본 파일명 | 완료 파일명 |
|------------|------------|
| `Train_Algorithm_Plan.md` | `Train_Algorithm_Completed.md` |
| `Frontend_Display_Values_Validation_Plan.md` | `Frontend_Display_Values_Validation_Completed.md` |
| `{기능명}_Plan.md` | `{기능명}_Completed.md` |

### 사용 예시

```
사용자: "Frontend_Display_Values_Validation_Plan.md 계획을 완료처리해"

AI 처리:
1. docs/plans/Frontend_Display_Values_Validation_Plan.md 읽기
2. docs/completed/Frontend_Display_Values_Validation_Completed.md 생성
3. 원본 플랜 파일 삭제
4. README 업데이트
```

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2024-12

