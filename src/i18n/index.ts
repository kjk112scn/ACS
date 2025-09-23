import { createI18n } from 'vue-i18n'
import enUS from './en-US'
import koKR from './ko-KR'

const messages = {
  'en-US': enUS,
  'ko-KR': koKR,
}

const i18n = createI18n({
  legacy: false,
  locale: 'ko-KR', // 기본 언어를 한국어로 설정
  fallbackLocale: 'en-US',
  messages,
})

export default i18n
