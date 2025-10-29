# ACS API 문서

> ACS (Antenna Control System) API 프로젝트 전체 문서

---

## 📚 문서 구조

### 📖 [Development_Guide.md](Development_Guide.md)
**전체 개발 가이드 (통합)**
- 프로젝트 전체 개요
- 핵심 기능 요약
- 빠른 시작 가이드
- 변경 이력

👉 **시작점**: 프로젝트 이해를 위한 첫 문서

---

### 📝 [plans/](plans/)
**계획 단계 문서**
- 협의 중인 작업 계획
- Step by Step 형태
- 코드 예시 및 수정 위치
- 사용자와 협의하며 업데이트

**용도**: 새 기능 구현 전 계획 수립

---

### ✅ [completed/](completed/)
**완료 단계 문서**
- 작업 완료 후 정리본
- 구현 완료 상태 명시
- 테스트 결과 포함
- 다이어그램/표 중심

**용도**: 작업 완료 상태 확인 및 이력 관리

---

### 📖 [references/](references/)
**세부 참조 문서**
- 아키텍처 설계
- 알고리즘 상세
- API 참조
- 배포 가이드

**용도**: 기능별 상세 기술 문서

#### 하위 폴더
- `architecture/` - 시스템 아키텍처, 데이터 흐름
- `algorithms/` - 알고리즘 상세 설계
- `api/` - API 참조, 엔드포인트
- `deployment/` - 배포 및 운영 가이드
- `user-guide/` - 사용자 가이드

---

## 🔄 문서 라이프사이클

```
1. 계획 작성
   docs/plans/{기능}_Plan.md
   → 사용자와 협의, 계속 업데이트

2. 작업 진행
   → 코드 구현 및 테스트

3. 완료 문서 생성
   docs/completed/{기능}_Completed.md
   → 구현 완료 상태 정리

4. 개발 가이드 통합
   docs/Development_Guide.md 업데이트
   docs/references/{카테고리}/{기능}_Design.md 생성
   → 전체 문서에 새 기능 반영
```

---

## 📋 문서 찾기

### 새로운 기능 시작
1. `plans/` 확인 - 진행 중인 계획이 있는지
2. 없으면 새 계획 작성 요청

### 완료된 기능 확인
1. `completed/` 확인 - 완료 문서
2. `Development_Guide.md` 확인 - 전체 개요

### 상세 기술 문서
1. `references/` 확인 - 카테고리별 상세 문서

---

## 🔢 버전 관리

### Semantic Versioning
```
MAJOR.MINOR.PATCH

예시:
- 1.0.0: 초기 릴리즈
- 1.1.0: 새 기능 추가 (Train 알고리즘)
- 1.1.1: 버그 수정
- 2.0.0: 아키텍처 대규모 변경
```

### 문서 버전
- 각 문서 상단에 버전 명시
- `Development_Guide.md`에 변경 이력 유지
- Git 태그와 연동 권장

---

## 📝 문서 작성 가이드

### plans/ (계획서)
- Step 1, Step 2... 형태
- 코드 예시 포함
- 수정할 파일/라인 명시
- Markdown 형식

### completed/ (완료 문서)
- 구현 완료 상태 명시
- 다이어그램/표 중심
- 테스트 결과
- 코드 참조 (라인 번호)

### references/ (참조 문서)
- 설계 문서 형태
- 기술 상세 설명
- 아키텍처 다이어그램
- 코드 예시 최소화

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2024-12  
**유지 관리자**: GTL Systems

