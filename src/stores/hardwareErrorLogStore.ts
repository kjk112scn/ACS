import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { HardwareErrorLog } from '@/types/hardwareError'

export const useHardwareErrorLogStore = defineStore('hardwareErrorLog', () => {
  // 상태
  const errorLogs = ref<HardwareErrorLog[]>([])
  const isLogPanelOpen = ref(false)
  const isPopupOpen = ref(false)
  const isInitialLoad = ref(false)

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
  
  // 팝업 상태 관리
  const setPopupOpen = async (isOpen: boolean) => {
    try {
      isPopupOpen.value = isOpen
      
      if (isOpen) {
        // 팝업 열기 - 백엔드에서 전체 로그 히스토리 가져오기
        const clientId = 'client-' + Date.now() // 임시 클라이언트 ID
        const response = await fetch('/api/hardware-error-logs/popup-state', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: `clientId=${encodeURIComponent(clientId)}&isOpen=true`
        })
        
        if (response.ok) {
          const data = await response.json()
          if (data.allLogs && Array.isArray(data.allLogs)) {
            errorLogs.value = data.allLogs
            isInitialLoad.value = true
            saveToLocalStorage()
            console.log('📱 팝업 열기 - 전체 로그 로드 완료:', data.allLogs.length)
          }
        }
      } else {
        // 팝업 닫기 - 백엔드에 알림
        const clientId = 'client-' + Date.now() // 임시 클라이언트 ID
        await fetch('/api/hardware-error-logs/popup-state', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: `clientId=${encodeURIComponent(clientId)}&isOpen=false`
        })
        
        isInitialLoad.value = false
        console.log('📱 팝업 닫기 완료')
      }
    } catch (error) {
      console.error('❌ 팝업 상태 설정 실패:', error)
    }
  }
  
  // 새로운 로그들 추가 (팝업이 열려있을 때 실시간 업데이트용)
  const addNewLogs = (newLogs: HardwareErrorLog[]) => {
    if (!isPopupOpen.value || !isInitialLoad.value) {
      return // 팝업이 닫혀있거나 초기 로드가 완료되지 않았으면 무시
    }
    
    newLogs.forEach(newLog => {
      const existingIndex = errorLogs.value.findIndex(log => log.id === newLog.id)
      
      if (existingIndex !== -1) {
        // 기존 로그 업데이트 (해결 상태 변경 등)
        errorLogs.value[existingIndex] = newLog
      } else {
        // 새 로그 추가
        errorLogs.value.unshift(newLog)
      }
    })
    
    // 시간순 정렬 (최신순)
    errorLogs.value.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
    
    // 최대 1000개로 제한
    if (errorLogs.value.length > 1000) {
      errorLogs.value = errorLogs.value.slice(0, 1000)
    }
    
    saveToLocalStorage()
    console.log('📱 실시간 로그 업데이트:', newLogs.length, '개')
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
    isPopupOpen,
    isInitialLoad,

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
    setPopupOpen,
    addNewLogs,

    // 초기화
    initialize,
    loadHistoryFromBackend,
  }
})
