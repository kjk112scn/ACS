/**
 * useTimezone Composable
 * Timezone 선택 UI를 위한 헬퍼 함수
 *
 * - 자주 사용하는 timezone 목록
 * - 전체 IANA timezone 목록 (Intl API)
 * - 그룹화된 옵션
 */
import { computed } from 'vue'

export interface TimezoneOption {
  value: string
  label: string
  offset: string
  favorite?: boolean
  header?: boolean
}

/**
 * 자주 사용하는 timezone 목록
 */
const FAVORITE_TIMEZONES: TimezoneOption[] = [
  { value: 'UTC', label: 'UTC', offset: 'UTC+0', favorite: true },
  { value: 'Asia/Seoul', label: 'Asia/Seoul', offset: 'UTC+9', favorite: true },
  { value: 'Asia/Tokyo', label: 'Asia/Tokyo', offset: 'UTC+9', favorite: true },
  {
    value: 'America/New_York',
    label: 'America/New_York',
    offset: 'UTC-5',
    favorite: true,
  },
  {
    value: 'America/Los_Angeles',
    label: 'America/Los_Angeles',
    offset: 'UTC-8',
    favorite: true,
  },
  {
    value: 'Europe/London',
    label: 'Europe/London',
    offset: 'UTC+0',
    favorite: true,
  },
]

/**
 * UTC offset 계산
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

export const useTimezone = () => {
  /**
   * 자주 사용하는 timezone
   */
  const favoriteTimezones = FAVORITE_TIMEZONES

  /**
   * 전체 timezone 목록 (브라우저 내장)
   */
  const allTimezones = computed<TimezoneOption[]>(() => {
    try {
      // Intl.supportedValuesOf는 ES2022 이상에서 지원
      // TypeScript 타입 정의에 누락되어 있으므로 타입 단언 사용
      const intl = Intl as typeof Intl & {
        supportedValuesOf: (key: string) => string[]
      }
      const zones = intl.supportedValuesOf('timeZone')
      return zones.map((tz) => ({
        value: tz,
        label: tz,
        offset: getTimezoneOffset(tz),
        favorite: FAVORITE_TIMEZONES.some((f) => f.value === tz),
      }))
    } catch {
      // 구형 브라우저 fallback
      return FAVORITE_TIMEZONES
    }
  })

  /**
   * 그룹화된 옵션 (q-select용)
   */
  const groupedTimezones = computed<TimezoneOption[]>(() => {
    const result: TimezoneOption[] = []

    // 자주 사용 그룹
    result.push({ value: '', label: '★ 자주 사용', offset: '', header: true })
    result.push(...FAVORITE_TIMEZONES)

    // 대륙별 그룹
    const continents = ['Africa', 'America', 'Asia', 'Atlantic', 'Australia', 'Europe', 'Pacific']

    for (const continent of continents) {
      const zones = allTimezones.value.filter(
        (tz) => tz.value.startsWith(`${continent}/`) && !tz.favorite
      )
      if (zones.length > 0) {
        result.push({ value: '', label: continent, offset: '', header: true })
        result.push(...zones) // 모든 시간대 표시 (위성 안테나는 전 세계 설치 가능)
      }
    }

    return result
  })

  /**
   * 검색 필터 함수 (q-select @filter용)
   */
  function filterTimezones(
    val: string,
    update: (callback: () => void) => void,
    options: { value: TimezoneOption[] }
  ) {
    if (val === '') {
      update(() => {
        options.value = groupedTimezones.value
      })
      return
    }

    update(() => {
      const needle = val.toLowerCase()
      options.value = allTimezones.value.filter(
        (tz) =>
          tz.value.toLowerCase().includes(needle) ||
          tz.offset.toLowerCase().includes(needle)
      )
    })
  }

  return {
    favoriteTimezones,
    allTimezones,
    groupedTimezones,
    getTimezoneOffset,
    filterTimezones,
  }
}