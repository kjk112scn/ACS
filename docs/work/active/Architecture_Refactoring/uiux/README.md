# UI/UX Refactoring Plan

> **작성일**: 2026-01-19
> **상태**: 검토 완료, 구현 대기
> **목표**: 사용성 개선 및 시각적 일관성 확보

---

## 요약

| 영역 | 상태 | 우선순위 |
|------|------|----------|
| 다국어 시스템 | 계획 수립 완료 | HIGH |
| Drawer 네비게이션 | 검토 완료 | HIGH |
| 헤더 간소화 | 검토 완료 | MEDIUM |
| 모드 페이지 통일 | 검토 완료 | MEDIUM |
| !important 정리 | 별도 문서 | LOW |

---

## 1. 현재 상태 분석

### 1.1 프론트엔드 구조

| 구분 | 수량 | 설명 |
|------|------|------|
| Pages | 12개 | Dashboard, Login, 모드별 페이지 |
| Components | 40개 | Settings, Common, Content 등 |
| Layouts | 2개 | MainLayout, LoginLayout |
| Stores | 20+ | Pinia 기반 상태 관리 |

### 1.2 기술 스택

- **Framework**: Vue 3 + Quasar 2.x
- **Language**: TypeScript
- **State**: Pinia
- **Styling**: SCSS + CSS Variables (테마)

---

## 2. 발견된 문제점

### 2.1 일관성 문제 (Priority: HIGH)

#### 2.1.1 Drawer 네비게이션 무관
- **파일**: `src/layouts/MainLayout.vue:188-230`
- **문제**: Essential Links가 Quasar 프레임워크 기본 링크
  - Quasar Docs, Discord, Twitter 등
  - ACS 시스템과 전혀 무관
- **해결**: ACS 모드별 네비게이션으로 교체

```typescript
// 현재 (무관한 링크)
const linksList = [
  { title: 'Docs', link: 'https://quasar.dev' },
  { title: 'Discord Chat Channel', link: 'https://chat.quasar.dev' },
  // ...
]

// 개선안 (ACS 모드 네비게이션)
const navigationItems = [
  { title: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
  { title: 'Standby', icon: 'pause_circle', route: '/mode/standby' },
  { title: 'Step', icon: 'straighten', route: '/mode/step' },
  { title: 'Slew', icon: 'speed', route: '/mode/slew' },
  // ...
]
```

#### 2.1.2 다국어 시스템 혼란
- **현재 상태** (전문가 검토 결과):
  - vue-i18n 설정됨 (ko-KR, en-US)
  - 실제 `$t()` 사용: **38건** (6개 파일) - 예상보다 적음
  - 하드코딩 한글: **~1,770건** (89개 파일)
  - 하드코딩 영문: **~1,265건** (73개 파일)
  - 번역 키: ~650개 (ko-KR, en-US)
- **문제점**:
  - vue-i18n은 IDE 지원 부족 (Go to Definition, Hover 미지원)
  - 번역 키 오타 시 런타임에서만 발견
  - 유지보수 시 키-값 매칭 어려움
  - 하드코딩이 i18n보다 훨씬 많음
- **결정**: **TypeScript 상수 객체**로 통일 ([ADR-005](../../../decisions/ADR-005-i18n-typescript-constant-migration.md))

#### 2.1.3 레이아웃 구조 불일치
- **문제**: 페이지마다 다른 레이아웃 구조
  - `mode-shell` 사용: Standby, Step, Slew
  - 직접 스타일링: SunTrack, Ephemeris, PassSchedule
- **해결**: 모든 모드 페이지 `mode-shell` 기반으로 통일

### 2.2 사용성 문제 (Priority: HIGH)

#### 2.2.1 헤더 정보 과밀
- **파일**: `src/layouts/MainLayout.vue:3-43`
- **문제**: 우측 섹션에 정보 밀집
  - UTC/Local 시간 (2줄)
  - 서버 상태 텍스트
  - 설정, 다크모드, 정보 버튼 3개
- **해결안**:
  ```
  [로고] [Antenna Control System] [UTC시간] [상태아이콘] [설정]
  ```
  - 시간: 1줄로 축소 (UTC만 또는 토글)
  - 서버 상태: 텍스트 → 아이콘으로 변경
  - 버튼: 설정 하나로 통합 (내부에서 다크모드/정보)

#### 2.2.2 현재 모드 표시 부재
- **문제**: 현재 어떤 모드인지 시각적 표시 없음
- **해결**:
  - 좌측 Drawer에 현재 모드 강조
  - 헤더 또는 브레드크럼에 현재 모드 표시

#### 2.2.3 에러 상태바 조건 누락
- **파일**: `src/layouts/MainLayout.vue:64`
- **코드**: `v-if="true"` - 항상 표시
- **해결**: 에러 있을 때만 표시, 또는 최소화 가능

### 2.3 접근성 문제 (Priority: MEDIUM)

#### 2.3.1 aria-label 불완전
- **문제**: 일부 버튼에만 aria-label 적용
  ```vue
  <!-- 적용됨 -->
  <q-btn aria-label="Settings" />

  <!-- 미적용 -->
  <q-btn label="Go" />
  ```
- **해결**: 모든 인터랙티브 요소에 접근성 속성 추가

#### 2.3.2 색상 대비
- **문제**: `text-secondary: #b0bec5` 대비 검토 필요
- **해결**: WCAG AA 기준 4.5:1 대비 확인

### 2.4 시각적 문제 (Priority: MEDIUM)

#### 2.4.1 그림자 과도
- **파일**: `src/css/mode-common.scss`
- **코드**: `box-shadow: 0 24px 40px rgba(0, 0, 0, 0.35)`
- **문제**: 그림자가 너무 강해 부유감 과도
- **해결**: `0 8px 16px rgba(0, 0, 0, 0.15)` 정도로 완화

#### 2.4.2 높이 계산 복잡
- **코드**: `height: calc(var(--theme-layout-modePageMinHeight, 500px) - 34px - 16px)`
- **문제**: 매직 넘버, 계산식 복잡
- **해결**: CSS Grid/Flexbox 기반 자동 높이

### 2.5 코드 품질 문제 (Priority: LOW)

#### 2.5.1 !important 과다 사용
- **파일**: `src/css/mode-common.scss`
- **수량**: 27회
- **문제**: 스타일 우선순위 충돌, 유지보수 어려움
- **해결**: CSS 특정성 리팩토링

#### 2.5.2 console.log 잔존
- **예시**: `console.log('🔧 시스템 정보 버튼 클릭됨')`
- **해결**: logger.debug()로 교체

#### 2.5.3 인라인 스타일 과다
- **파일**: `EphemerisDesignationPage.vue`
- **예시**: `style="min-height: 360px !important; height: 100% !important;"`
- **해결**: CSS 클래스로 분리

---

## 3. 개선 계획

### Phase 1: 다국어 시스템 마이그레이션 (HIGH)

> **ADR**: [ADR-005](../../../decisions/ADR-005-i18n-typescript-constant-migration.md)

| Task | 파일 | 설명 |
|------|------|------|
| texts/ 폴더 생성 | `src/texts/` | ko.ts, en.ts, index.ts |
| 기존 i18n 키 변환 | `src/i18n/*.ts` → `src/texts/*.ts` | 구조 유지하며 변환 |
| $t() → T. 교체 | **6개 파일 (38건)** | Settings 컴포넌트들 |
| Accept-Language 헤더 | axios interceptor | FE-BE 언어 연동 |
| vue-i18n 제거 | package.json, boot/ | 의존성 정리 |
| LanguageSettings 수정 | LanguageSettings.vue | setLanguage() 연동 |

### Phase 2: 레이아웃 개선 (HIGH)

| Task | 파일 | 설명 |
|------|------|------|
| Drawer 네비게이션 교체 | MainLayout.vue | ACS 모드 메뉴로 변경 |
| 헤더 간소화 | MainLayout.vue | 정보 정리, 버튼 통합 |
| 에러바 조건 추가 | MainLayout.vue | `v-if="hasActiveError"` |
| 현재 모드 표시 | MainLayout.vue | 네비게이션 강조 |

### Phase 3: 구조 개선 (MEDIUM)

| Task | 파일 | 설명 |
|------|------|------|
| 모드 페이지 통일 | 모드 페이지들 | mode-shell 기반 통일 |
| DashboardPage 분리 | DashboardPage.vue | 축별 컴포넌트 분리 |
| 높이 계산 단순화 | mode-common.scss | Flexbox 기반 |

### Phase 4: 품질 개선 (LOW)

| Task | 파일 | 설명 |
|------|------|------|
| !important 제거 | SCSS 파일들 | CSS 특정성 정리 (별도 문서) |
| 접근성 속성 추가 | 컴포넌트들 | aria-label, role 등 |
| 그림자/스타일 완화 | theme-variables.scss | 시각적 밸런스 |

---

## 4. 상세 작업 목록

### 4.1 다국어 시스템 마이그레이션

> **실제 $t() 사용 파일**: 6개 (전문가 검토 결과)

```
[ ] src/texts/ko.ts 생성 (기존 i18n/ko-KR 기반)
[ ] src/texts/en.ts 생성 (기존 i18n/en-US 기반)
[ ] src/texts/index.ts 생성 (T export, setLanguage)
[ ] composables/useI18n.ts → useTexts.ts 리팩토링

# $t() → T. 교체 (6개 파일, 38건)
[ ] LanguageSettings.vue (2건)
[ ] SettingsModal.vue (13건)
[ ] VersionInfoSettings.vue (7건)
[ ] AdminSettings.vue (1건)
[ ] MCOffSettings.vue (5건)
[ ] ServoAlarmResetSettings.vue (10건)

# 정리
[ ] boot/i18n.ts 제거
[ ] package.json에서 vue-i18n 제거
[ ] src/i18n/ 폴더 삭제

# FE-BE 연동
[ ] axios interceptor에 Accept-Language 헤더 추가
[ ] BE LanguageResolver 구현 (선택)

[ ] 빌드 테스트
```

### 4.2 MainLayout.vue 개선

```
[ ] Drawer Essential Links → ACS Navigation
[ ] 헤더 우측 정보 재구성
[ ] 에러 상태바 조건부 표시
[ ] 시간 표시 단순화
[ ] 현재 모드 표시 추가
```

### 4.3 모드 페이지 통일

```
[ ] SunTrackPage.vue - mode-shell 적용
[ ] EphemerisDesignationPage.vue - mode-shell 적용
[ ] PassSchedulePage.vue - mode-shell 적용
[ ] FeedPage.vue - mode-shell 적용
```

### 4.4 DashboardPage.vue 분리

```
[ ] AxisCard.vue 추출 (Azimuth, Elevation, Tilt)
[ ] EmergencyCard.vue 추출
[ ] ControlCard.vue 추출
[ ] StatusCard.vue 추출
```

### 4.5 스타일 정리

```
[ ] mode-common.scss - !important 제거
[ ] theme-variables.scss - 그림자 값 조정
[ ] 인라인 스타일 → CSS 클래스
```

---

## 5. 다국어 시스템 전략

### 5.1 결정 사항

| 항목 | 결정 |
|------|------|
| **방식** | TypeScript 상수 객체 |
| **이유** | IDE 친화적 (Go to Definition, Hover, 타입체크) |
| **언어** | ko-KR (기본), en-US |
| **vue-i18n** | 제거 |

### 5.2 폴더 구조

```
frontend/src/texts/
├── index.ts      # 언어 전환 로직 + T export
├── ko.ts         # 한국어 텍스트
├── en.ts         # 영어 텍스트
└── types.ts      # 타입 정의 (선택)
```

### 5.3 구현 예시

**ko.ts**
```typescript
export const ko = {
  common: {
    save: '저장',
    cancel: '취소',
    confirm: '확인',
    delete: '삭제',
  },
  header: {
    title: 'Antenna Control System',
    serverConnected: '서버 연결됨',
    serverDisconnected: '서버 연결 끊김',
  },
  dashboard: {
    azimuth: 'Azimuth',
    elevation: 'Elevation',
    tilt: 'Tilt',
    currentPosition: '현재 위치',
    targetPosition: '목표 위치',
  },
  modes: {
    standby: 'Standby',
    step: 'Step',
    slew: 'Slew',
    sunTrack: 'Sun Track',
    ephemeris: 'Ephemeris Designation',
    passSchedule: 'Pass Schedule',
  },
  settings: {
    title: '설정',
    language: '언어',
    theme: '테마',
    languageChanged: (lang: string) => `언어가 ${lang}(으)로 변경되었습니다`,
  },
  errors: {
    connectionFailed: '연결 실패',
    timeout: '시간 초과',
  },
} as const
```

**en.ts**
```typescript
import type { ko } from './ko'

export const en: typeof ko = {
  common: {
    save: 'Save',
    cancel: 'Cancel',
    confirm: 'Confirm',
    delete: 'Delete',
  },
  header: {
    title: 'Antenna Control System',
    serverConnected: 'Server Connected',
    serverDisconnected: 'Server Disconnected',
  },
  dashboard: {
    azimuth: 'Azimuth',
    elevation: 'Elevation',
    tilt: 'Tilt',
    currentPosition: 'Current Position',
    targetPosition: 'Target Position',
  },
  modes: {
    standby: 'Standby',
    step: 'Step',
    slew: 'Slew',
    sunTrack: 'Sun Track',
    ephemeris: 'Ephemeris Designation',
    passSchedule: 'Pass Schedule',
  },
  settings: {
    title: 'Settings',
    language: 'Language',
    theme: 'Theme',
    languageChanged: (lang: string) => `Language changed to ${lang}`,
  },
  errors: {
    connectionFailed: 'Connection Failed',
    timeout: 'Timeout',
  },
} as const
```

**index.ts**
```typescript
import { computed, ref } from 'vue'
import { ko } from './ko'
import { en } from './en'

export type Language = 'ko' | 'en'

const texts = { ko, en }

// 현재 언어 (localStorage에서 초기화)
const currentLang = ref<Language>(
  (localStorage.getItem('preferred-language')?.split('-')[0] as Language) || 'ko'
)

// 반응형 텍스트 객체
export const T = computed(() => texts[currentLang.value])

// 언어 변경 함수
export const setLanguage = (lang: Language) => {
  currentLang.value = lang
  localStorage.setItem('preferred-language', lang === 'ko' ? 'ko-KR' : 'en-US')
}

// 현재 언어 getter
export const getCurrentLanguage = () => currentLang.value
```

### 5.4 사용 예시

```vue
<template>
  <!-- Before (vue-i18n) -->
  <q-btn :label="$t('common.save')" />

  <!-- After (상수 객체) -->
  <q-btn :label="T.common.save" />

  <!-- 동적 텍스트 -->
  <span>{{ T.settings.languageChanged('English') }}</span>
</template>

<script setup lang="ts">
import { T } from '@/texts'
</script>
```

### 5.5 장점

| 항목 | 설명 |
|------|------|
| **Go to Definition** | Ctrl+Click으로 바로 이동 |
| **Hover Preview** | 마우스 올리면 값 미리보기 |
| **타입 체크** | 오타 시 컴파일 에러 |
| **자동완성** | IDE에서 키 자동완성 |
| **번들 최적화** | Tree-shaking 가능 |

### 5.6 백엔드 다국어 현황

백엔드에 **이미 체계적인 상수 객체 방식 다국어 시스템 구축됨**:

| 파일 | 역할 | 내용 |
|------|------|------|
| `ErrorMessageConfig.kt` | 에러 메시지 | 28개 (ko/en) |
| `ApiDescriptions.kt` | API 공통 설명 | 50+ (ko/en) |
| `openapi/*ApiDescriptions.kt` | 컨트롤러별 Swagger | 5개 파일 |
| `OpenApiUtils.kt` | Swagger 언어 자동 적용 | 언어별 분기 |

**에러 메시지 구조**:
```kotlin
object ErrorMessageConfig {
    val ERROR_MESSAGES = mapOf(
        "EMERGENCY_STOP_ACTIVE" to mapOf(
            "ko" to "비상 정지가 활성화되었습니다",
            "en" to "Emergency stop is active"
        )
    )
    fun getErrorMessage(key: String, language: String = "ko"): String
}
```

**Swagger 다국어 구조**:
```kotlin
// ApiDescriptions.kt
val EPHEMERIS_DESCRIPTIONS = mapOf(
    "stopEphemerisTracking" to mapOf(
        "ko" to mapOf("summary" to "위성 추적 중지", "description" to "..."),
        "en" to mapOf("summary" to "Stop Satellite Tracking", "description" to "...")
    )
)

// OpenApiUtils.kt - 언어별 자동 적용
fun applyApiDescriptions(operation: Operation, handlerMethod: HandlerMethod, language: Language)
```

> **결론**: FE도 동일한 패턴으로 구현하면 FE-BE 일관성 확보

### 5.7 FE-BE 다국어 연동

| 영역 | 관리 위치 | 설명 |
|------|----------|------|
| UI 텍스트 | FE `src/texts/` | 버튼, 라벨, 제목 등 |
| 에러 메시지 | BE `ErrorMessageConfig` | 하드웨어/시스템 에러 |
| API 응답 메시지 | BE → FE 전달 | language 파라미터 필요 |

**연동 방식**:
```typescript
// FE: API 호출 시 언어 헤더 포함
const api = axios.create({
  headers: {
    'Accept-Language': getCurrentLanguage() // 'ko' | 'en'
  }
})

// BE: 에러 응답 시 해당 언어로 메시지 반환
```

### 5.8 마이그레이션 계획

| 단계 | 작업 | 예상 |
|------|------|------|
| 1 | `texts/` 폴더 생성 및 기본 구조 | 0.5일 |
| 2 | 기존 i18n 키 → 상수 객체 변환 | 1일 |
| 3 | 컴포넌트별 `$t()` → `T.` 교체 | 2일 |
| 4 | vue-i18n 의존성 제거 | 0.5일 |
| 5 | LanguageSettings.vue 수정 | 0.5일 |
| 6 | API 호출 시 언어 헤더 추가 | 0.5일 |

**총 예상: 5일**

---

## 6. 참고 파일

### Frontend
| 파일 | 역할 |
|------|------|
| `src/layouts/MainLayout.vue` | 메인 레이아웃, 헤더, Drawer |
| `src/css/mode-common.scss` | 모드 페이지 공통 스타일 |
| `src/css/theme-variables.scss` | 테마 변수 정의 |
| `src/pages/DashboardPage.vue` | 대시보드 (31,597 tokens) |
| `src/i18n/` | 기존 vue-i18n 폴더 (제거 예정) |
| `src/composables/useI18n.ts` | 기존 i18n 래퍼 (제거 예정) |

### Backend
| 파일 | 역할 |
|------|------|
| `service/hardware/ErrorMessageConfig.kt` | 에러 메시지 다국어 (28개) |
| `util/ApiDescriptions.kt` | API 공통 설명 다국어 (50+) |
| `openapi/EphemerisApiDescriptions.kt` | Ephemeris Swagger 설명 |
| `openapi/PassScheduleApiDescriptions.kt` | PassSchedule Swagger 설명 |
| `openapi/SunTrackApiDescriptions.kt` | SunTrack Swagger 설명 |
| `openapi/ICDApiDescriptions.kt` | ICD Swagger 설명 |
| `openapi/SettingsApiDescriptions.kt` | Settings Swagger 설명 |
| `openapi/OpenApiUtils.kt` | Swagger 언어 자동 적용 유틸 |

---

## 7. 예상 효과

| 항목 | Before | After |
|------|--------|-------|
| 다국어 시스템 | vue-i18n (IDE 미지원) | 상수 객체 (IDE 완전 지원) |
| 네비게이션 | 무관한 링크 | ACS 모드 메뉴 |
| 헤더 복잡도 | 6개 요소 | 3-4개 요소 |
| 언어 일관성 | 혼용 (40% i18n + 60% 하드코딩) | 상수 객체 100% |
| 코드 품질 | !important 27회 | 최소화 |
| 접근성 | 부분 적용 | 전체 적용 |
| 개발 경험 | 키 오타 → 런타임 발견 | 키 오타 → 컴파일 에러 |
