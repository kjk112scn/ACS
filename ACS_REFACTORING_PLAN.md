# ACS 프로젝트 리팩토링 진행방식 A

## 📋 프로젝트 개요

- **프로젝트명**: ACS (안테나 제어 시스템)
- **기술스택**: Vue 3 + Quasar + TypeScript + Kotlin Spring Boot
- **현재 상태**: 기능적으로는 작동하지만 구조적으로 리팩토링 필요
- **목표**: 코드 일관성, 재사용성, 유지보수성 향상

## 🎯 주요 문제점

1. **거대한 파일들**: DashboardPage.vue (1805줄), icdStore.ts (2258줄)
2. **일관성 없는 패턴**: 에러 처리, API 호출, 상태 관리
3. **분산된 구조**: 타입 정의, 공통 기능들이 여러 곳에 분산
4. **공통 컴포넌트 부족**: 유사한 UI 패턴이 반복 구현
5. **알림 시스템 분산**: 각 컴포넌트마다 개별 구현

## ⚠️ 시작 전 필수 작업

### **Settings 기능 완성** (30-45분)

- [ ] `src/services/settingsService.ts`에서 API 응답 처리 로직 수정
- [ ] 백엔드 응답 형식에 맞게 에러 처리 개선
- [ ] 각 설정별 저장/로드 테스트
- [ ] UI 반응성 테스트

## 📊 전체 진행 계획

### **-1단계: 프로젝트 정리** (예상 소요시간: 1시간)

- [ ] 백엔드 코드 제거 (`src/main/` 폴더)
- [ ] `build.gradle.kts`, `gradle/` 폴더 제거
- [ ] `orekit-data/` 폴더 제거
- [ ] `logs/` 폴더 제거
- [ ] `csv_exports/` 폴더 제거
- [ ] `docs/` 폴더 제거
- [ ] `src/test-sendbox/` 폴더 제거
- [ ] `.gitignore` 최적화
- [ ] 불필요한 파일들 Git에서 제거

### **0단계: 불필요한 파일 정리** (예상 소요시간: 30분)

- [ ] `src/components/models.ts` 제거 (사용되지 않음)
- [ ] `src/components/ExampleComponent.vue` 제거 (예제용)
- [ ] `src/stores/example-store.ts` 제거 (예제용)
- [ ] `void` 파일 제거 (빈 파일)
- [ ] `src/pages/DashboardPage_Test.vue` 제거 (테스트용)
- [ ] `src/pages/ErrorNotFound.vue` 개선 (기본 에러 페이지)

### **0.5단계: 라우터 구조 개선** (예상 소요시간: 1시간)

- [ ] 팝업 라우터 구조 개선 (`/popup/` 경로들)
- [ ] 중첩 라우터 정리 (`dashboard` 하위 모드들)
- [ ] 라우터 가드 최적화
- [ ] 불필요한 리다이렉트 정리
- [ ] 라우터 타입 정의 개선

### **1단계: 폴더 구조 정리** (예상 소요시간: 2-3시간)

- [ ] `src/types/` 디렉토리 생성 및 하위 폴더 구성
  - [ ] `src/types/common.ts` - 공통 타입들
  - [ ] `src/types/settings.ts` - 설정 관련 타입들
  - [ ] `src/types/icd.ts` - ICD 관련 타입들
  - [ ] `src/types/mode.ts` - 모드 관련 타입들
  - [ ] `src/types/ephemeris.ts` - 위성 추적 관련 타입들
  - [ ] `src/types/index.ts` - 모든 타입 export
- [ ] 컴포넌트 폴더 재구성
  - [ ] `src/components/common/` 디렉토리 생성
  - [ ] `src/components/dashboard/` 디렉토리 생성
  - [ ] `src/components/settings/` 디렉토리 생성 (대소문자 통일)
  - [ ] `src/components/content/` → `src/components/dashboard/`로 이동
  - [ ] `src/components/Settings/` → `src/components/settings/`로 이동
- [ ] 서비스 폴더 정리
  - [ ] `src/services/api/` 디렉토리 생성
  - [ ] `src/services/mode/` → `src/services/api/mode/`로 이동
  - [ ] `src/services/settingsService.ts` → `src/services/api/settingsService.ts`로 이동
  - [ ] `src/services/icdService.ts` → `src/services/api/icdService.ts`로 이동
- [ ] 스토어 폴더 정리
  - [ ] `src/stores/mode/` → `src/stores/api/mode/`로 이동
  - [ ] `src/stores/icd/` → `src/stores/api/icd/`로 이동
  - [ ] `src/stores/modeStore.ts` → `src/stores/api/modeStore.ts`로 이동

### **2단계: 타입 시스템 통합** (예상 소요시간: 2-3시간)

- [ ] `src/services/api/settingsService.ts`에서 타입 정의들을 `src/types/settings.ts`로 이동
- [ ] `src/services/api/icdService.ts`에서 타입 정의들을 `src/types/icd.ts`로 이동
- [ ] `src/stores/api/modeStore.ts`에서 타입 정의들을 `src/types/mode.ts`로 이동
- [ ] `src/types/ephemerisTrack.ts`를 `src/types/ephemeris.ts`로 이름 변경
- [ ] `src/types/index.ts` 생성하여 모든 타입 export
- [ ] 중복된 인터페이스들 통합
- [ ] 공통 타입들 `src/types/common.ts`로 이동
- [ ] 타입 네이밍 컨벤션 통일

### **3단계: 공통 유틸리티 시스템 구축** (예상 소요시간: 3-4시간)

- [ ] **에러 핸들링 시스템**
  - [ ] `src/utils/errorHandler.ts` 개선
  - [ ] `src/utils/apiError.ts` 생성 (API 에러 전용)
  - [ ] `src/utils/validationError.ts` 생성 (유효성 검사 에러 전용)
  - [ ] `src/utils/errorHandler.ts`에 통합된 에러 처리 로직 추가
- [ ] **API 호출 시스템**
  - [ ] `src/services/apiClient.ts` 생성 (통합 API 클라이언트)
  - [ ] `src/services/api/` 폴더의 모든 서비스에서 공통 클라이언트 사용
  - [ ] `src/services/api/` 폴더의 모든 서비스에서 공통 에러 처리 사용
- [ ] **로딩 상태 관리 시스템**
  - [ ] `src/composables/useLoading.ts` 생성
  - [ ] `src/composables/useLoading.ts`에 통합된 로딩 상태 관리 로직 추가
  - [ ] 모든 페이지에서 개별 로딩 상태를 통합 시스템으로 교체
- [ ] **알림 시스템**
  - [ ] `src/composables/useNotification.ts` 생성
  - [ ] `src/composables/useNotification.ts`에 통합된 알림 시스템 추가
  - [ ] 모든 컴포넌트에서 개별 알림을 통합 시스템으로 교체
- [ ] **다이얼로그 시스템**
  - [ ] `src/composables/useDialog.ts` 생성
  - [ ] `src/composables/useDialog.ts`에 통합된 다이얼로그 시스템 추가
  - [ ] 모든 컴포넌트에서 개별 다이얼로그를 통합 시스템으로 교체
- [ ] **폼 유효성 검사 시스템**
  - [ ] `src/composables/useValidation.ts` 생성
  - [ ] `src/composables/useValidation.ts`에 통합된 유효성 검사 로직 추가
  - [ ] 모든 폼에서 개별 유효성 검사를 통합 시스템으로 교체

### **3.5단계: 타입 안전한 i18n 시스템 구축** (예상 소요시간: 2-3시간)

- [ ] **타입 정의 생성**
  - [ ] `src/i18n/types.ts` 생성
  - [ ] `TranslationKeys` 인터페이스 정의
  - [ ] 모든 번역 키 타입 정의
- [ ] **번역 파일 구축**
  - [ ] `src/i18n/ko-KR/index.ts` 생성
  - [ ] `src/i18n/en-US/index.ts` 생성
  - [ ] 타입 안전한 번역 객체 정의
- [ ] **번역 유틸리티 생성**
  - [ ] `src/composables/useI18n.ts` 생성
  - [ ] 타입 안전한 번역 함수 구현
  - [ ] 언어 변경 기능
- [ ] **개발자 도구 구축**
  - [ ] `src/utils/i18nDevTools.ts` 생성
  - [ ] VS Code 설정 파일 생성
  - [ ] 개발 모드 도구 추가
- [ ] **기존 메시지 변환**
  - [ ] 주요 메시지들을 타입 안전한 키로 변환
  - [ ] 템플릿에서 번역 함수 사용
  - [ ] 에러 메시지 번역화

### **4단계: 대형 파일 분할** (예상 소요시간: 4-5시간)

- [ ] **DashboardPage.vue 분할**
  - [ ] `src/components/dashboard/DashboardHeader.vue` 생성
  - [ ] `src/components/dashboard/DashboardAxes.vue` 생성
  - [ ] `src/components/dashboard/DashboardCharts.vue` 생성
  - [ ] `src/components/dashboard/DashboardControls.vue` 생성
  - [ ] `src/components/dashboard/DashboardStatus.vue` 생성
  - [ ] `src/pages/DashboardPage.vue`를 분할된 컴포넌트들로 재구성
- [ ] **icdStore.ts 분할**
  - [ ] `src/stores/api/icd/icdDataStore.ts` 생성
  - [ ] `src/stores/api/icd/icdControlStore.ts` 생성
  - [ ] `src/stores/api/icd/icdStatusStore.ts` 생성
  - [ ] `src/stores/api/icd/icdStore.ts`를 분할된 스토어들로 재구성
- [ ] **settingsStore.ts 분할**
  - [ ] `src/stores/api/settings/settingsDataStore.ts` 생성
  - [ ] `src/stores/api/settings/settingsControlStore.ts` 생성
  - [ ] `src/stores/api/settings/settingsValidationStore.ts` 생성
  - [ ] `src/stores/api/settings/settingsStore.ts`를 분할된 스토어들로 재구성

### **5단계: 공통 컴포넌트 구축** (예상 소요시간: 3-4시간)

- [ ] **공통 UI 컴포넌트**
  - [ ] `src/components/common/BaseButton.vue` 생성
  - [ ] `src/components/common/BaseInput.vue` 생성
  - [ ] `src/components/common/BaseSelect.vue` 생성
  - [ ] `src/components/common/BaseCard.vue` 생성
  - [ ] `src/components/common/BaseModal.vue` 생성
- [ ] **공통 차트 컴포넌트**
  - [ ] `src/components/common/BaseChart.vue` 생성
  - [ ] `src/components/common/BaseGauge.vue` 생성
  - [ ] `src/components/common/BaseStatus.vue` 생성
- [ ] **공통 폼 컴포넌트**
  - [ ] `src/components/common/BaseForm.vue` 생성
  - [ ] `src/components/common/BaseField.vue` 생성
  - [ ] `src/components/common/BaseValidation.vue` 생성

### **6단계: Window Utils 개선** (예상 소요시간: 2-3시간)

- [ ] **통합된 컴포넌트 열기 시스템**
  - [ ] `src/utils/windowUtils.ts` 개선
  - [ ] `src/utils/windowUtils.ts`에 통합된 옵션 인터페이스 추가
  - [ ] `src/utils/windowUtils.ts`에 자동 모드 선택 로직 추가
  - [ ] `src/utils/windowUtils.ts`에 간편한 API 추가
- [ ] **모달/팝업 통합 관리**
  - [ ] `src/composables/useWindowManager.ts` 생성
  - [ ] `src/composables/useWindowManager.ts`에 통합된 윈도우 관리 로직 추가
  - [ ] 모든 컴포넌트에서 개별 윈도우 관리를 통합 시스템으로 교체

### **7단계: 성능 최적화** (예상 소요시간: 2-3시간)

- [ ] **지연 로딩 구현**
  - [ ] 모든 페이지에 지연 로딩 적용
  - [ ] 모든 컴포넌트에 지연 로딩 적용
  - [ ] 모든 차트에 지연 로딩 적용
- [ ] **번들 크기 최적화**
  - [ ] 불필요한 라이브러리 제거
  - [ ] 공통 라이브러리 통합
  - [ ] 코드 분할 최적화

### **8단계: 테스트 및 검증** (예상 소요시간: 2-3시간)

- [ ] **기능 테스트**
  - [ ] 모든 페이지 기능 테스트
  - [ ] 모든 API 연동 테스트
  - [ ] 모든 에러 처리 테스트
- [ ] **성능 테스트**
  - [ ] 로딩 속도 테스트
  - [ ] 메모리 사용량 테스트
  - [ ] 번들 크기 테스트

### **9단계: 문서화** (예상 소요시간: 1-2시간)

- [ ] **코드 문서화**
  - [ ] 모든 컴포넌트에 JSDoc 추가
  - [ ] 모든 함수에 JSDoc 추가
  - [ ] 모든 타입에 JSDoc 추가
- [ ] **사용법 문서화**
  - [ ] `README.md` 업데이트
  - [ ] `docs/` 폴더 생성 및 사용법 문서 작성
  - [ ] `docs/` 폴더에 컴포넌트 사용법 문서 작성

### **10단계: 최종 정리** (예상 소요시간: 1-2시간)

- [ ] **코드 정리**
  - [ ] 불필요한 파일 제거
  - [ ] 사용하지 않는 import 제거
  - [ ] 코드 포맷팅 통일
- [ ] **최종 검증**
  - [ ] 전체 프로젝트 빌드 테스트
  - [ ] 전체 프로젝트 실행 테스트
  - [ ] 전체 프로젝트 기능 테스트

## 📈 예상 효과

### **단기 효과 (1-2주)**

- 코드 일관성 향상
- 개발 속도 20-30% 향상
- 버그 발생률 40-50% 감소

### **중기 효과 (1-3개월)**

- 새 기능 개발 시간 50% 단축
- 유지보수 비용 60% 감소
- 코드 이해도 대폭 향상

### **장기 효과 (6개월+)**

- 팀 생산성 2-3배 향상
- 기술 부채 대폭 감소
- 확장성 크게 향상

## 📊 전체 요약

- **총 단계**: 13단계 (-1단계 ~ 10단계)
- **총 예상 소요시간**: 29-40시간
- **우선순위**: Settings 기능 완성 → 진행방식 A 시작

## 🚀 시작 방법

1. **Settings 기능 완성** (30-45분) - 필수!
2. 진행방식 A -1단계부터 순차적으로 진행
3. 각 단계 완료 후 다음 단계로 진행
4. 문제 발생 시 이전 단계로 롤백

## 진행 상황 체크

- [ ] **Settings 기능 완성** (필수)
- [ ] -1단계: 프로젝트 정리
- [ ] 0단계: 불필요한 파일 정리
- [ ] 0.5단계: 라우터 구조 개선
- [ ] 1단계: 폴더 구조 정리
- [ ] 2단계: 타입 시스템 통합
- [ ] 3단계: 공통 유틸리티 시스템 구축
- [ ] 3.5단계: 타입 안전한 i18n 시스템 구축
- [ ] 4단계: 대형 파일 분할
- [ ] 5단계: 공통 컴포넌트 구축
- [ ] 6단계: Window Utils 개선
- [ ] 7단계: 성능 최적화
- [ ] 8단계: 테스트 및 검증
- [ ] 9단계: 문서화
- [ ] 10단계: 최종 정리

## 🔧 새 PC에서 이어서 진행하는 방법

### **1. 프로젝트 복사**

```bash
# Git 클론 또는 프로젝트 폴더 복사
git clone [repository-url]
cd ACS
```

### **2. 의존성 설치**

```bash
npm install
```

### **3. 진행방식 A 파일 확인**

```bash
# ACS_REFACTORING_PLAN.md 파일 확인
cat ACS_REFACTORING_PLAN.md
```

### **4. 현재 진행 상황 확인**

- Settings 기능 완성 여부 확인
- 진행방식 A에서 완료된 단계 체크
- 다음 단계부터 진행

### **5. 새 대화에서 이어서 진행**

```
"ACS_REFACTORING_PLAN.md 파일을 참고해서 [단계번호] 단계부터 시작해주세요"
예: "ACS_REFACTORING_PLAN.md 파일을 참고해서 -1단계부터 시작해주세요"
```

## ⚠️ 주의사항

- **각 단계는 순차적으로 진행**해야 함
- **Settings 기능을 먼저 완성**해야 함
- **각 단계 완료 후 테스트** 필수
- **문제 발생 시 즉시 중단**하고 이전 단계로 롤백
- **백업을 자주 생성**할 것

---

**생성일**: 2024년 12월 19일  
**버전**: 1.1  
**상태**: 완성
