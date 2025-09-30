import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { HardwareErrorLog } from '@/types/hardwareError'

export const useHardwareErrorLogStore = defineStore('hardwareErrorLog', () => {
  const { locale } = useI18n()

  // 상태
  const errorLogs = ref<HardwareErrorLog[]>([])
  const isLogPanelOpen = ref(false)

  // 계산된 속성
  const activeErrorCount = computed(() => errorLogs.value.filter((log) => !log.isResolved).length)

  const errorLogsByCategory = computed(() => {
    const categories = [
      'POWER',
      'PROTOCOL',
      'EMERGENCY',
      'SERVO_POWER',
      'STOW',
      'POSITIONER',
      'FEED',
    ]
    return categories.reduce(
      (acc, category) => {
        acc[category] = errorLogs.value.filter((log) => log.category === category)
        return acc
      },
      {} as Record<string, HardwareErrorLog[]>,
    )
  })

  const errorLogsBySeverity = computed(() => {
    const severities = ['INFO', 'WARNING', 'ERROR', 'CRITICAL']
    return severities.reduce(
      (acc, severity) => {
        acc[severity] = errorLogs.value.filter((log) => log.severity === severity)
        return acc
      },
      {} as Record<string, HardwareErrorLog[]>,
    )
  })

  // 액션
  const addErrorLog = (error: HardwareErrorLog) => {
    errorLogs.value.unshift(error)

    // 최대 1000개로 제한
    if (errorLogs.value.length > 1000) {
      errorLogs.value = errorLogs.value.slice(0, 1000)
    }

    // 로컬 스토리지에 저장
    saveToLocalStorage()
  }

  const updateErrorLog = (id: string, updates: Partial<HardwareErrorLog>) => {
    const index = errorLogs.value.findIndex((log) => log.id === id)
    if (index !== -1) {
      errorLogs.value[index] = { ...errorLogs.value[index], ...updates }
      saveToLocalStorage()
    }
  }

  const clearAllLogs = () => {
    errorLogs.value = []
    saveToLocalStorage()
  }

  const clearResolvedLogs = () => {
    errorLogs.value = errorLogs.value.filter((log) => !log.isResolved)
    saveToLocalStorage()
  }

  const toggleLogPanel = () => {
    isLogPanelOpen.value = !isLogPanelOpen.value
  }

  // 로컬 스토리지 관리
  const saveToLocalStorage = () => {
    try {
      localStorage.setItem('hardwareErrorLogs', JSON.stringify(errorLogs.value))
    } catch (e) {
      console.error('로컬 스토리지 저장 실패:', e)
    }
  }

  const loadFromLocalStorage = () => {
    try {
      const saved = localStorage.getItem('hardwareErrorLogs')
      if (saved) {
        errorLogs.value = JSON.parse(saved)
      }
    } catch (e) {
      console.error('로컬 스토리지 로드 실패:', e)
    }
  }

  // 백엔드에서 히스토리 로드
  const loadHistoryFromBackend = async () => {
    try {
      const response = await fetch('/api/hardware-error-logs')
      if (response.ok) {
        const data = await response.json()
        errorLogs.value = data
        saveToLocalStorage()
      }
    } catch (e) {
      console.error('히스토리 로드 실패:', e)
    }
  }

  // 초기화
  const initialize = () => {
    loadFromLocalStorage()
    void loadHistoryFromBackend()
  }

  // 다국어 지원
  const getCurrentMessage = (message: { ko: string; en: string }) => {
    return locale.value === 'ko' ? message.ko : message.en
  }

  const getCategoryName = (category: string) => {
    const categoryMap: Record<string, string> = {
      POWER: 'Power Status',
      PROTOCOL: 'Protocol Status',
      EMERGENCY: 'Emergency Stop Status',
      SERVO_POWER: 'Servo Power Status',
      STOW: 'Stow Pin Status',
      POSITIONER: 'Positioner Status',
      FEED: 'Feed Status',
    }
    return categoryMap[category] || category
  }

  const getSeverityName = (severity: string) => {
    const severityMap: Record<string, string> = {
      INFO: '정보',
      WARNING: '경고',
      ERROR: '오류',
      CRITICAL: '심각',
    }
    return severityMap[severity] || severity
  }

  // 언어 변경 감지
  watch(locale, () => {
    // 언어가 변경되면 UI가 자동으로 업데이트됨 (computed 속성 사용)
  })

  return {
    // 상태
    errorLogs,
    isLogPanelOpen,

    // 계산된 속성
    activeErrorCount,
    errorLogsByCategory,
    errorLogsBySeverity,

    // 액션
    addErrorLog,
    updateErrorLog,
    clearAllLogs,
    clearResolvedLogs,
    toggleLogPanel,

    // 초기화
    initialize,
    loadHistoryFromBackend,

    // 다국어 지원
    getCurrentMessage,
    getCategoryName,
    getSeverityName,
  }
})
