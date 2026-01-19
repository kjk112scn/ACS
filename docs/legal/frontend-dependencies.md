# Frontend Dependencies 라이선스 상세

프론트엔드(Vue 3 + Quasar) 프로젝트의 모든 npm 패키지 라이선스 상세 정보입니다.

**검토일**: 2026-01-19
**패키지 관리자**: npm
**설정 파일**: `frontend/package.json`

---

## Production Dependencies

### Vue Ecosystem

#### vue@3.4.18
- **라이선스**: MIT
- **저작권**: Copyright (c) 2014-present Evan You
- **용도**: 프론트엔드 코어 프레임워크
- **GitHub**: https://github.com/vuejs/core
- **상업적 사용**: ✅ 허용
- **의무사항**: 라이선스 텍스트 포함

#### vue-router@4.0.12
- **라이선스**: MIT
- **저작권**: Copyright (c) 2019-present Eduardo San Martin Morote
- **용도**: SPA 라우팅
- **GitHub**: https://github.com/vuejs/router
- **상업적 사용**: ✅ 허용

#### pinia@3.0.2
- **라이선스**: MIT
- **저작권**: Copyright (c) 2019-present Eduardo San Martin Morote
- **용도**: 상태 관리 (Vuex 대체)
- **GitHub**: https://github.com/vuejs/pinia
- **상업적 사용**: ✅ 허용

#### vue-i18n@9.2.2
- **라이선스**: MIT
- **저작권**: Copyright (c) 2016-present kazuya kawaguchi
- **용도**: 다국어(i18n) 지원
- **GitHub**: https://github.com/intlify/vue-i18n
- **상업적 사용**: ✅ 허용

---

### Quasar Framework

#### quasar@2.16.0
- **라이선스**: MIT
- **저작권**: Copyright (c) 2015-present Razvan Stoenescu
- **용도**: UI 컴포넌트 프레임워크
- **GitHub**: https://github.com/quasarframework/quasar
- **상업적 사용**: ✅ 허용
- **특이사항**: Material Design 기반 컴포넌트 제공

#### @quasar/extras@1.16.4
- **라이선스**: MIT
- **저작권**: Copyright (c) 2015-present Razvan Stoenescu
- **용도**: 아이콘, 폰트 등 추가 리소스
- **GitHub**: https://github.com/quasarframework/quasar
- **상업적 사용**: ✅ 허용
- **포함 리소스**:
  - Roboto Font (Apache 2.0)
  - Material Icons (Apache 2.0)

---

### Data Visualization

#### echarts@5.6.0
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) 2017-2024 The Apache Software Foundation
- **용도**: 인터랙티브 차트 라이브러리
- **GitHub**: https://github.com/apache/echarts
- **상업적 사용**: ✅ 허용
- **의무사항**:
  - 라이선스 텍스트 포함
  - NOTICE 파일 포함
  - 수정 시 변경사항 고지
- **사용 페이지**:
  - DashboardPage.vue
  - EphemerisDesignationPage.vue
  - PassSchedulePage.vue
  - ScheduleChart.vue

#### chart.js@4.4.9
- **라이선스**: MIT
- **저작권**: Copyright (c) 2014-2024 Chart.js Contributors
- **용도**: 캔버스 기반 차트 라이브러리
- **GitHub**: https://github.com/chartjs/Chart.js
- **상업적 사용**: ✅ 허용

#### vue-chartjs@5.3.2
- **라이선스**: MIT
- **저작권**: Copyright (c) 2021 vue-chartjs
- **용도**: Chart.js Vue 래퍼
- **GitHub**: https://github.com/apertureless/vue-chartjs
- **상업적 사용**: ✅ 허용

---

### HTTP & Utilities

#### axios@1.9.0
- **라이선스**: MIT
- **저작권**: Copyright (c) 2014-present Matt Zabriskie & Collaborators
- **용도**: HTTP 클라이언트
- **GitHub**: https://github.com/axios/axios
- **상업적 사용**: ✅ 허용

---

### Type Definitions

#### @types/echarts@4.9.22
- **라이선스**: MIT
- **저작권**: DefinitelyTyped contributors
- **용도**: ECharts TypeScript 타입 정의
- **npm**: https://www.npmjs.com/package/@types/echarts
- **상업적 사용**: ✅ 허용

---

## Development Dependencies

### Build Tools

#### @quasar/app-vite@2.1.0
- **라이선스**: MIT
- **용도**: Quasar CLI (Vite 기반)
- **상업적 사용**: ✅ 허용

#### typescript@5.5.3
- **라이선스**: Apache License 2.0
- **저작권**: Copyright (c) Microsoft Corporation
- **용도**: TypeScript 컴파일러
- **GitHub**: https://github.com/microsoft/TypeScript
- **상업적 사용**: ✅ 허용

#### vue-tsc@2.0.29
- **라이선스**: MIT
- **용도**: Vue TypeScript 타입 체크
- **상업적 사용**: ✅ 허용

#### vite-plugin-checker@0.8.0
- **라이선스**: MIT
- **용도**: Vite 타입 체크 플러그인
- **상업적 사용**: ✅ 허용

#### autoprefixer@10.4.2
- **라이선스**: MIT
- **용도**: CSS 자동 프리픽스
- **상업적 사용**: ✅ 허용

---

### Linting & Formatting

#### eslint@9.14.0
- **라이선스**: MIT
- **저작권**: Copyright OpenJS Foundation and other contributors
- **용도**: JavaScript/TypeScript 린터
- **GitHub**: https://github.com/eslint/eslint
- **상업적 사용**: ✅ 허용

#### @eslint/js@9.14.0
- **라이선스**: MIT
- **용도**: ESLint JavaScript 설정
- **상업적 사용**: ✅ 허용

#### eslint-plugin-vue@9.30.0
- **라이선스**: MIT
- **용도**: Vue ESLint 플러그인
- **상업적 사용**: ✅ 허용

#### prettier@3.3.3
- **라이선스**: MIT
- **저작권**: Copyright (c) 2014-present James Long
- **용도**: 코드 포매터
- **GitHub**: https://github.com/prettier/prettier
- **상업적 사용**: ✅ 허용

#### @vue/eslint-config-prettier@10.1.0
- **라이선스**: MIT
- **용도**: Vue + Prettier 설정
- **상업적 사용**: ✅ 허용

#### @vue/eslint-config-typescript@14.1.3
- **라이선스**: MIT
- **용도**: Vue + TypeScript ESLint 설정
- **상업적 사용**: ✅ 허용

---

### i18n

#### @intlify/unplugin-vue-i18n@2.0.0
- **라이선스**: MIT
- **용도**: Vue I18n 빌드 플러그인
- **상업적 사용**: ✅ 허용

---

### Type Definitions

#### @types/node@20.5.9
- **라이선스**: MIT
- **저작권**: DefinitelyTyped contributors
- **용도**: Node.js TypeScript 타입 정의
- **상업적 사용**: ✅ 허용

#### globals@15.12.0
- **라이선스**: MIT
- **용도**: 전역 변수 정의
- **상업적 사용**: ✅ 허용

---

## 라이선스 분포 요약

| 라이선스 | 개수 | 비율 |
|----------|------|------|
| MIT | 23 | 92% |
| Apache 2.0 | 2 | 8% |

---

## 의무사항 체크리스트

### MIT License 패키지
- [x] 라이선스 텍스트 포함 (`docs/legal/license-texts/MIT.txt`)
- [x] 저작권 표시 유지

### Apache 2.0 패키지 (echarts, typescript)
- [x] 라이선스 텍스트 포함 (`docs/legal/license-texts/Apache-2.0.txt`)
- [ ] NOTICE 파일 포함 (배포 시)
- [ ] 수정 사항 문서화 (수정 시)

---

## 업데이트 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-01-19 | 최초 작성 |
