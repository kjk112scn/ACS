import { computed } from 'vue'
import { useI18n as useVueI18n } from 'vue-i18n'
import type { I18nKey, TranslateFunction, Language } from 'src/types'

export const useI18n = () => {
  const { locale, t, availableLocales } = useVueI18n()

  // 현재 언어
  const currentLanguage = computed(() => locale.value as Language)

  // 사용 가능한 언어 목록
  const languages = computed(() => availableLocales as Language[])

  // 언어 변경
  const changeLanguage = (lang: Language) => {
    locale.value = lang
    localStorage.setItem('preferred-language', lang)
  }

  // 타입 안전한 번역 함수
  const translate: TranslateFunction = (key: I18nKey, params?: Record<string, unknown>) => {
    const result = t(key, params || {})

    // 개발 모드에서 키 정보를 콘솔에 출력
    if (process.env.NODE_ENV === 'development') {
      console.log(`🌐 i18n Key: ${key}`, {
        ko: t(key, { locale: 'ko-KR' }),
        en: t(key, { locale: 'en-US' }),
        current: result,
      })
    }

    return result
  }

  // 언어별 번역 가져오기
  const getTranslation = (key: I18nKey, lang: Language) => {
    return t(key, { locale: lang })
  }

  // 번역 키 존재 여부 확인
  const hasTranslation = (key: I18nKey) => {
    return t(key) !== key
  }

  return {
    // 상태
    currentLanguage,
    languages,

    // 메서드
    t: translate,
    changeLanguage,
    getTranslation,
    hasTranslation,
  }
}
