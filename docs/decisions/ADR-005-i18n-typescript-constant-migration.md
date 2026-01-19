---
번호: ADR-005
제목: vue-i18n에서 TypeScript 상수 객체로 다국어 시스템 마이그레이션
상태: 승인됨
날짜: 2026-01-19
---

# ADR-005: vue-i18n에서 TypeScript 상수 객체로 다국어 시스템 마이그레이션

## 상태

**승인됨** - 전문가 검토 완료

## 컨텍스트

### 현재 상황

| 항목 | 수치 | 비고 |
|------|------|------|
| vue-i18n 버전 | 9.2.2 | Composition API 지원 |
| `$t()` 호출 | 38건 | 6개 파일 |
| 하드코딩 한글 | ~1,770건 | 89개 파일 |
| 번역 키 | ~650개 | ko-KR, en-US |
| 복수형/포맷팅 | 미사용 | Intl API 직접 사용 |

### 문제점

1. **IDE 지원 부족**
   - vue-i18n은 `$t('key')` 형태로 키를 문자열로 전달
   - Go to Definition 미지원
   - Hover 시 값 미리보기 불가
   - 키 오타 시 런타임에서만 발견

2. **유지보수 어려움**
   - 키-값 매칭을 위해 번역 파일 직접 확인 필요
   - 번역 누락/오타 찾기 어려움
   - 리팩토링 시 키 변경 추적 불가

3. **실제 활용도 낮음**
   - 전체 텍스트 중 ~40%만 i18n 사용
   - 나머지 ~60%는 하드코딩
   - 복수형, 날짜 포맷팅 등 고급 기능 미사용

### 백엔드 현황

백엔드는 이미 **체계적인 상수 객체 방식 다국어 시스템** 구축됨:

| 파일 | 역할 | 내용 |
|------|------|------|
| `ErrorMessageConfig.kt` | 에러 메시지 | 28개 (ko/en) |
| `ApiDescriptions.kt` | API 공통 설명 | 50+ (ko/en) |
| `openapi/*ApiDescriptions.kt` | 컨트롤러별 Swagger | 5개 파일 |
| `OpenApiUtils.kt` | Swagger 언어 자동 적용 | 언어별 분기 |

```kotlin
// ErrorMessageConfig.kt - 에러 메시지
object ErrorMessageConfig {
    val ERROR_MESSAGES = mapOf(
        "EMERGENCY_STOP_ACTIVE" to mapOf(
            "ko" to "비상 정지가 활성화되었습니다",
            "en" to "Emergency stop is active"
        )
    )
}

// ApiDescriptions.kt - Swagger 설명
val EPHEMERIS_DESCRIPTIONS = mapOf(
    "stopEphemerisTracking" to mapOf(
        "ko" to mapOf("summary" to "위성 추적 중지", "description" to "..."),
        "en" to mapOf("summary" to "Stop Satellite Tracking", "description" to "...")
    )
)
```

> **FE 구현 시 BE와 동일한 패턴 사용 → FE-BE 일관성 확보**

## 결정

**vue-i18n을 제거하고 TypeScript 상수 객체 방식으로 마이그레이션한다.**

### 새로운 구조

```
frontend/src/texts/
├── index.ts      # 언어 전환 로직 + T export
├── ko.ts         # 한국어 텍스트
└── en.ts         # 영어 텍스트
```

### 구현 방식

```typescript
// ko.ts
export const ko = {
  common: {
    save: '저장',
    cancel: '취소',
  },
  settings: {
    languageChanged: (lang: string) => `언어가 ${lang}(으)로 변경되었습니다`,
  },
} as const

// en.ts
export const en: typeof ko = {
  common: {
    save: 'Save',
    cancel: 'Cancel',
  },
  settings: {
    languageChanged: (lang: string) => `Language changed to ${lang}`,
  },
} as const

// index.ts
import { computed, ref } from 'vue'
import { ko } from './ko'
import { en } from './en'

export type Language = 'ko' | 'en'
const currentLang = ref<Language>('ko')
export const T = computed(() => texts[currentLang.value])
export const setLanguage = (lang: Language) => { ... }
```

### 사용법 변경

```vue
<!-- Before -->
<q-btn :label="$t('common.save')" />

<!-- After -->
<q-btn :label="T.common.save" />
```

## 대안 검토

| 대안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| **vue-i18n 유지** | 변경 없음 | IDE 지원 부족 유지 | 기각 |
| **vue-i18n + i18n Ally** | 일부 IDE 지원 | 설정 복잡, 불완전 | 기각 |
| **i18next 도입** | 풍부한 기능 | vue-i18n과 동일한 문제 | 기각 |
| **TypeScript 상수** | IDE 완전 지원 | 복수형 직접 구현 | **채택** |
| **하이브리드** | 점진적 전환 | 두 시스템 혼재 | 기각 |

## FE-BE 연동

### 언어 코드 매핑

| Frontend | Backend | localStorage |
|----------|---------|--------------|
| `ko` | `ko` | `ko-KR` |
| `en` | `en` | `en-US` |

### API 언어 전달

```typescript
// axios interceptor
api.interceptors.request.use((config) => {
  config.headers['Accept-Language'] = getCurrentLanguage()
  return config
})
```

### WebSocket 언어 전달

```typescript
// 쿼리 파라미터 방식
const wsUrl = `ws://localhost:8080/ws/push-data?lang=${getCurrentLanguage()}`
```

### 에러 메시지 처리

```
FE → API 요청 (Accept-Language: ko)
BE → 응답 { code: 'EMERGENCY_STOP_ACTIVE', message: '비상 정지...' }
FE → code로 T.errors.EMERGENCY_STOP_ACTIVE 조회 (fallback: message)
```

## 결과

### 장점

| 항목 | Before | After |
|------|--------|-------|
| Go to Definition | 미지원 | Ctrl+Click 이동 |
| Hover Preview | 미지원 | 값 즉시 확인 |
| 타입 체크 | 런타임 오류 | 컴파일 오류 |
| 자동완성 | 미지원 | IDE 완전 지원 |
| 번들 크기 | vue-i18n 포함 | 라이브러리 제거 |
| FE-BE 일관성 | 불일치 | 동일 패턴 |

### 단점

| 항목 | 설명 | 대응 |
|------|------|------|
| 복수형 미지원 | vue-i18n 복수형 기능 없음 | 현재 미사용, 필요 시 함수로 구현 |
| 마이그레이션 비용 | 38건 `$t()` 변환 | 작은 규모, 1일 내 완료 가능 |
| 하드코딩 정리 | 1,770건 별도 작업 | 점진적 진행 |

## 마이그레이션 계획

| Phase | 작업 | 규모 |
|-------|------|------|
| 1 | `texts/` 폴더 + 기본 구조 | 신규 파일 3개 |
| 2 | Settings 6개 파일 마이그레이션 | 38건 `$t()` → `T.` |
| 3 | Accept-Language 헤더 + BE LanguageResolver | FE 1개, BE 1개 |
| 4 | vue-i18n 제거 | package.json, boot/ |
| 5 | 하드코딩 정리 (점진적) | 1,770건 |

## 검토자

- architect: 아키텍처 일관성 검토 완료
- fullstack-helper: FE-BE 통합 구현 검토 완료
- tech-lead: 기술 결정 리스크 검토 완료

## 참조

- [UI/UX 리팩토링 계획](../work/active/Architecture_Refactoring/uiux/README.md)
- [ErrorMessageConfig.kt](../../backend/src/main/kotlin/com/gtlsystems/acs_api/service/hardware/ErrorMessageConfig.kt)