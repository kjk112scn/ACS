/**
 * Timezone Store
 * 사용자 시간대 설정 관리
 *
 * - 자동감지: 브라우저 Intl API 사용
 * - 수동선택: IANA timezone (Asia/Seoul 등)
 * - 저장: localStorage (인증 미구현 상태)
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const STORAGE_KEY_TIMEZONE = 'userTimezone'
const STORAGE_KEY_AUTO_DETECT = 'timezoneAutoDetect'

export const useTimezoneStore = defineStore('timezone', () => {
  // ========================================
  // State
  // ========================================

  /**
   * 브라우저에서 감지된 timezone (읽기 전용)
   */
  const detectedTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone

  /**
   * 자동감지 사용 여부
   */
  const useAutoDetect = ref(
    localStorage.getItem(STORAGE_KEY_AUTO_DETECT) !== 'false'
  )

  /**
   * 수동 선택된 timezone
   */
  const manualTimezone = ref(
    localStorage.getItem(STORAGE_KEY_TIMEZONE) || detectedTimezone
  )

  // ========================================
  // Computed
  // ========================================

  /**
   * 현재 적용 중인 timezone
   */
  const currentTimezone = computed(() =>
    useAutoDetect.value ? detectedTimezone : manualTimezone.value
  )

  /**
   * 현재 timezone의 UTC offset 문자열
   */
  const currentOffset = computed(() => {
    return getTimezoneOffset(currentTimezone.value)
  })

  /**
   * 표시용 문자열 (예: "Asia/Seoul (UTC+9)")
   */
  const displayString = computed(() =>
    `${currentTimezone.value} (${currentOffset.value})`
  )

  // ========================================
  // Actions
  // ========================================

  /**
   * timezone 수동 설정
   */
  function setTimezone(tz: string) {
    manualTimezone.value = tz
    localStorage.setItem(STORAGE_KEY_TIMEZONE, tz)
  }

  /**
   * 자동감지 토글
   */
  function setAutoDetect(auto: boolean) {
    useAutoDetect.value = auto
    localStorage.setItem(STORAGE_KEY_AUTO_DETECT, auto.toString())
  }

  /**
   * 설정 초기화 (브라우저 감지값으로 복원)
   */
  function reset() {
    useAutoDetect.value = true
    manualTimezone.value = detectedTimezone
    localStorage.removeItem(STORAGE_KEY_TIMEZONE)
    localStorage.removeItem(STORAGE_KEY_AUTO_DETECT)
  }

  // ========================================
  // Utility Functions
  // ========================================

  /**
   * timezone의 UTC offset 문자열 반환
   */
  function getTimezoneOffset(tz: string): string {
    try {
      const now = new Date()
      const formatter = new Intl.DateTimeFormat('en', {
        timeZone: tz,
        timeZoneName: 'shortOffset',
      })
      const parts = formatter.formatToParts(now)
      const offsetPart = parts.find((p) => p.type === 'timeZoneName')
      return offsetPart?.value || 'UTC'
    } catch {
      return 'UTC'
    }
  }

  return {
    // State
    detectedTimezone,
    useAutoDetect,
    manualTimezone,
    // Computed
    currentTimezone,
    currentOffset,
    displayString,
    // Actions
    setTimezone,
    setAutoDetect,
    reset,
    // Utils
    getTimezoneOffset,
  }
})