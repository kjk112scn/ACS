import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { HardwareErrorLog } from '@/types/hardwareError'

export const useHardwareErrorLogStore = defineStore('hardwareErrorLog', () => {
  // 상태
  const errorLogs = ref<HardwareErrorLog[]>([])
  const isLogPanelOpen = ref(false)

  // 계산된 속성
  const activeErrorCount = computed(() => errorLogs.value.filter((log) => !log.isResolved).length)
  const resolvedErrorCount = computed(() => errorLogs.value.filter((log) => log.isResolved).length)

  const errorLogsByCategory = computed(() => {
    const categories = [
      'POWER',
      'PROTOCOL',
      'EMERGENCY',
      'SERVO_POWER',
      'STOW',
      'POSITIONER',
      'FEED',
      'TEST',
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
    console.log('🔍 addErrorLog 호출됨:', error)
    console.log('🔍 추가 전 로그 개수:', errorLogs.value.length)

    // 중복 ID 체크
    const existingIndex = errorLogs.value.findIndex((existingLog) => existingLog.id === error.id)

    if (existingIndex !== -1) {
      // 기존 로그 업데이트
      errorLogs.value[existingIndex] = error
    } else {
      // 새 로그 추가
      errorLogs.value.unshift(error) // 최신순으로 추가
    }

    // 최대 1000개로 제한
    if (errorLogs.value.length > 1000) {
      errorLogs.value = errorLogs.value.slice(0, 1000)
    }

    console.log('🔍 추가 후 로그 개수:', errorLogs.value.length)
    console.log('🔍 최신 로그:', errorLogs.value[0])

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

  // ✅ deleteErrorLog 메서드 추가
  const deleteErrorLog = (id: string) => {
    const index = errorLogs.value.findIndex((log) => log.id === id)
    if (index !== -1) {
      errorLogs.value.splice(index, 1)
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

  const resolveAllErrors = () => {
    errorLogs.value.forEach((log) => {
      if (!log.isResolved) {
        log.isResolved = true
        log.resolvedAt = new Date().toISOString()
        log.resolvedMessage = {
          ko: '일괄 해결 처리됨',
          en: 'Bulk resolved',
        }
      }
    })
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

  return {
    // 상태
    errorLogs,
    isLogPanelOpen,

    // 계산된 속성
    activeErrorCount,
    resolvedErrorCount,
    errorLogsByCategory,
    errorLogsBySeverity,

    // 액션
    addErrorLog,
    updateErrorLog,
    deleteErrorLog, // ✅ 추가
    clearAllLogs,
    clearResolvedLogs,
    resolveAllErrors,
    toggleLogPanel,

    // 초기화
    initialize,
    loadHistoryFromBackend,
  }
})
