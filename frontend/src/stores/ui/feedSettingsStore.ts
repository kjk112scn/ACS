import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsService } from '@/services/api/settingsService'
import { useErrorHandler } from '@/composables/useErrorHandler'

/**
 * 피드 밴드 표시 설정 스토어
 * S-Band, X-Band, Ka-Band의 표시 여부를 관리합니다.
 */
export const useFeedSettingsStore = defineStore('feedSettings', () => {
  // 선택된 밴드 목록 (기본값: S-Band, X-Band만 선택)
  const enabledBands = ref<('s' | 'x' | 'ka')[]>(['s', 'x'])

  /**
   * 밴드 활성화 상태 확인
   * @param band - 확인할 밴드 ('s' | 'x' | 'ka')
   * @returns 밴드가 활성화되어 있으면 true
   */
  const isBandEnabled = (band: 's' | 'x' | 'ka'): boolean => {
    return enabledBands.value.includes(band)
  }

  // ✅ 각 밴드별 computed (반응형 보장)
  const isSBandEnabled = computed(() => enabledBands.value.includes('s'))
  const isXBandEnabled = computed(() => enabledBands.value.includes('x'))
  const isKaBandEnabled = computed(() => enabledBands.value.includes('ka'))

  /**
   * 밴드 활성화/비활성화 토글
   * @param band - 토글할 밴드 ('s' | 'x' | 'ka')
   */
  const toggleBand = async (band: 's' | 'x' | 'ka') => {
    const index = enabledBands.value.indexOf(band)
    if (index > -1) {
      // 최소 하나의 밴드는 활성화되어 있어야 함
      if (enabledBands.value.length > 1) {
        enabledBands.value.splice(index, 1)
        await saveSettings()
      }
    } else {
      enabledBands.value.push(band)
      await saveSettings()
    }
  }

  // 에러 핸들러
  const { handleApiError } = useErrorHandler()

  /**
   * 밴드 설정 저장 (백엔드 API 우선, 실패 시 로컬 스토리지)
   */
  const saveSettings = async () => {
    console.log('💾 피드 설정 저장 시작:', enabledBands.value)
    try {
      // 백엔드 API에 저장 시도
      await settingsService.setFeedSettings({
        enabledBands: enabledBands.value,
      })
      console.log('✅ 백엔드 API 저장 성공')
      // 성공 시 로컬 스토리지에도 백업 저장
      try {
        localStorage.setItem('feedSettings', JSON.stringify(enabledBands.value))
        console.log('✅ 로컬 스토리지 백업 저장 완료')
      } catch (localError) {
        console.warn('⚠️ 로컬 스토리지 백업 저장 실패:', localError)
      }
    } catch (error) {
      // 백엔드 저장 실패 시 로컬 스토리지에만 저장
      console.error('❌ 백엔드 API 저장 실패:', error)
      handleApiError(error, '피드 설정 저장')
      try {
        localStorage.setItem('feedSettings', JSON.stringify(enabledBands.value))
        console.warn('⚠️ 백엔드 저장 실패, 로컬 스토리지에만 저장됨')
      } catch (localError) {
        console.error('❌ 로컬 스토리지 저장도 실패:', localError)
      }
    }
  }

  /**
   * 밴드 설정 로드 (백엔드 API 우선, 실패 시 로컬 스토리지)
   */
  const loadSettings = async () => {
    // 먼저 로컬 스토리지에서 로드 시도 (빠른 응답)
    try {
      const saved = localStorage.getItem('feedSettings')
      if (saved) {
        const parsed = JSON.parse(saved)
        // 유효성 검사: 배열이고 최소 1개 이상의 밴드가 있어야 함
        if (Array.isArray(parsed) && parsed.length > 0) {
          const validBands = parsed.filter((b: string) => ['s', 'x', 'ka'].includes(b)) as (
            | 's'
            | 'x'
            | 'ka'
          )[]
          if (validBands.length > 0) {
            enabledBands.value = validBands
            console.log('✅ 로컬 스토리지에서 피드 설정 로드:', enabledBands.value)
          }
        }
      }
    } catch (error) {
      console.warn('로컬 스토리지에서 피드 설정 로드 실패:', error)
    }

    // 백엔드 API에서 로드 시도 (백그라운드에서 동기화)
    try {
      const feedSettings = await settingsService.getFeedSettings()
      if (feedSettings.enabledBands && feedSettings.enabledBands.length > 0) {
        // 유효성 검사: 허용된 밴드만 포함
        const validBands = feedSettings.enabledBands.filter((b: string) =>
          ['s', 'x', 'ka'].includes(b),
        ) as ('s' | 'x' | 'ka')[]
        if (validBands.length > 0) {
          enabledBands.value = validBands
          // 로컬 스토리지에도 동기화
          try {
            localStorage.setItem('feedSettings', JSON.stringify(enabledBands.value))
            console.log('✅ 백엔드에서 피드 설정 로드 및 동기화 완료:', enabledBands.value)
          } catch (localError) {
            console.warn('로컬 스토리지 동기화 실패:', localError)
          }
          return
        }
      }
    } catch (error) {
      // 백엔드 로드 실패는 무시 (로컬 스토리지 값 사용)
      console.warn('⚠️ 백엔드에서 피드 설정 로드 실패 (로컬 스토리지 값 사용):', error)
    }

    // 로컬 스토리지에도 값이 없으면 기본값 사용
    if (enabledBands.value.length === 0) {
      enabledBands.value = ['s', 'x']
      console.log('📝 기본값 사용:', enabledBands.value)
    }
  }

  /**
   * 활성화된 밴드 개수
   */
  const enabledBandCount = computed(() => enabledBands.value.length)

  /**
   * 모든 밴드 활성화
   */
  const enableAllBands = async () => {
    enabledBands.value = ['s', 'x', 'ka']
    await saveSettings()
  }

  /**
   * 모든 밴드 비활성화 (최소 하나는 유지)
   */
  const disableAllBands = async () => {
    enabledBands.value = ['s'] // 최소 하나는 유지
    await saveSettings()
  }

  // 초기 로드 (비동기이지만 await 없이 호출 - 초기화 시점이므로)
  loadSettings().catch((error) => {
    console.error('초기 피드 설정 로드 실패:', error)
  })

  return {
    enabledBands,
    isBandEnabled,
    isSBandEnabled, // ✅ 추가
    isXBandEnabled, // ✅ 추가
    isKaBandEnabled, // ✅ 추가
    toggleBand,
    saveSettings,
    loadSettings,
    enabledBandCount,
    enableAllBands,
    disableAllBands,
  }
})
