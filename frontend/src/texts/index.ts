import { computed, ref } from 'vue'
import { ko, type TextsType } from './ko'
import { en } from './en'

export type Language = 'ko' | 'en'

const texts: Record<Language, TextsType> = { ko, en }

// localStorage 키 (기존 vue-i18n과 호환)
const STORAGE_KEY = 'preferred-language'

// localStorage에서 언어 초기화
const getInitialLanguage = (): Language => {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    // 'ko-KR' -> 'ko', 'en-US' -> 'en'
    const lang = stored.split('-')[0]
    if (lang === 'ko' || lang === 'en') {
      return lang
    }
  }
  return 'ko'
}

// 현재 언어 (반응형)
const currentLang = ref<Language>(getInitialLanguage())

/**
 * 반응형 텍스트 객체
 * @example
 * ```vue
 * <template>
 *   <q-btn :label="T.common.save" />
 * </template>
 *
 * <script setup>
 * import { T } from '@/texts'
 * </script>
 * ```
 */
export const T = computed(() => texts[currentLang.value])

/**
 * 언어 변경 함수
 * @param lang - 'ko' 또는 'en'
 * @example
 * ```ts
 * import { setLanguage } from '@/texts'
 * setLanguage('en')
 * ```
 */
export const setLanguage = (lang: Language) => {
  currentLang.value = lang
  // localStorage에 저장 (기존 형식 유지: 'ko-KR', 'en-US')
  localStorage.setItem(STORAGE_KEY, lang === 'ko' ? 'ko-KR' : 'en-US')
}

/**
 * 현재 언어 getter
 * @returns 'ko' 또는 'en'
 */
export const getCurrentLanguage = (): Language => currentLang.value

/**
 * 현재 언어 Ref (watch용)
 */
export const currentLanguage = currentLang

/**
 * 현재 언어 코드 (BE API용)
 * @returns 'ko' 또는 'en' (Accept-Language 헤더용)
 */
export const getLanguageCode = (): string => currentLang.value

// 타입 re-export
export type { TextsType }