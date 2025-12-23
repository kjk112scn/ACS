import { createI18n } from 'vue-i18n'
import enUS from './en-US'
import koKR from './ko-KR'

const messages = {
  'en-US': enUS,
  'ko-KR': koKR,
}

// localStorage에서 저장된 언어 설정 불러오기
const savedLanguage = localStorage.getItem('preferred-language')
const defaultLocale =
  savedLanguage && ['ko-KR', 'en-US'].includes(savedLanguage) ? savedLanguage : 'ko-KR'

const i18n = createI18n({
  legacy: false,
  locale: defaultLocale,
  fallbackLocale: 'en-US',
  messages,
})

// 개발 모드에서 i18n Ally 지원
if (process.env.NODE_ENV === 'development') {
  // i18n Ally 개발 도구 지원을 위한 전역 변수 할당
  // @ts-expect-error - i18n Ally 개발 도구 지원을 위한 전역 변수 할당
  window.__i18n__ = i18n
}

export default i18n
