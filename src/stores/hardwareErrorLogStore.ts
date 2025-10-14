import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { HardwareErrorLog } from '@/types/hardwareError'
import { useI18n } from 'vue-i18n'

export const useHardwareErrorLogStore = defineStore('hardwareErrorLog', () => {
  // 상태
  const errorLogs = ref<HardwareErrorLog[]>([])
  const isLogPanelOpen = ref(false)
  const isPopupOpen = ref(false)
  const isInitialLoad = ref(false)

  // i18n 인스턴스
  const { t, locale } = useI18n()

  /**
   * 에러 키를 현재 언어로 변환하는 함수
   * @param errorKey - 에러 키
   * @param isResolved - 해결 여부
   * @returns 변환된 메시지
   */
  const translateErrorKey = (errorKey: string, isResolved: boolean): string => {
    try {
      const key = isResolved ? `${errorKey}_RESOLVED` : errorKey
      const i18nKey = `hardwareErrors.${key}`
      const translatedMessage = t(i18nKey)

      console.log('🔍 hardwareErrorLogStore translateErrorKey:', {
        errorKey,
        isResolved,
        key,
        i18nKey,
        translatedMessage,
        currentLocale: locale.value,
      })

      if (translatedMessage === i18nKey) {
        console.warn(`🚨 에러 메시지 번역 실패: ${i18nKey}`)
        return errorKey
      }

      return translatedMessage
    } catch (error) {
      console.error('🚨 에러 메시지 번역 중 오류:', error)
      return errorKey
    }
  }

  /**
   * 기존 에러 로그들의 메시지를 현재 언어로 업데이트
   */
  const updateErrorMessages = () => {
    errorLogs.value = errorLogs.value.map((log) => ({
      ...log,
      message: translateErrorKey(log.errorKey, log.isResolved),
      resolvedMessage: log.isResolved ? translateErrorKey(log.errorKey, log.isResolved) : undefined,
    }))
    console.log('🔄 에러 메시지 언어 업데이트 완료')
  }

  // 언어 변경 감지
  watch(locale, () => {
    console.log('🌐 언어 변경 감지:', locale.value)
    updateErrorMessages()
  })

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

    // 에러 메시지가 이미 변환되어 있지 않은 경우 변환
    const processedError = error.message
      ? error
      : {
          ...error,
          message: translateErrorKey(error.errorKey, error.isResolved),
          resolvedMessage: error.isResolved
            ? translateErrorKey(error.errorKey, error.isResolved)
            : undefined,
        }

    console.log('🔍 processedError:', processedError)

    // 중복 ID 체크
    const existingIndex = errorLogs.value.findIndex(
      (existingLog) => existingLog.id === processedError.id,
    )

    if (existingIndex !== -1) {
      // 기존 로그 업데이트
      errorLogs.value[existingIndex] = processedError
    } else {
      // 새 로그 추가
      errorLogs.value.unshift(processedError) // 최신순으로 추가
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
        log.resolvedMessage = translateErrorKey(log.errorKey, true)
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
      console.log('🔍 setPopupOpen 호출됨:', isOpen)
      isPopupOpen.value = isOpen

      if (isOpen) {
        // 팝업 열기 - 백엔드에서 전체 로그 히스토리 가져오기
        const clientId = 'client-' + Date.now() // 임시 클라이언트 ID
        console.log('🔍 팝업 열기 요청 - 클라이언트 ID:', clientId)

        const response = await fetch(
          `http://localhost:8080/api/hardware-error-logs/popup-state?clientId=${encodeURIComponent(clientId)}&isOpen=true`,
          {
            method: 'POST',
          },
        )

        console.log('🔍 팝업 열기 응답 상태:', response.status, response.statusText)

        if (response.ok) {
          const data = await response.json()
          console.log('🔍 팝업 열기 응답 데이터:', data)

          if (data.allLogs && Array.isArray(data.allLogs)) {
            errorLogs.value = data.allLogs
            isInitialLoad.value = true
            saveToLocalStorage()
            console.log('📱 팝업 열기 - 전체 로그 로드 완료:', data.allLogs.length)
            console.log('📱 로드된 로그들:', data.allLogs)
          } else {
            console.error('❌ allLogs가 없거나 배열이 아님:', data)
          }
        } else {
          console.error('❌ 팝업 열기 API 실패:', response.status, response.statusText)
        }
      } else {
        // 팝업 닫기 - 백엔드에 알림
        const clientId = 'client-' + Date.now() // 임시 클라이언트 ID
        console.log('🔍 팝업 닫기 요청 - 클라이언트 ID:', clientId)

        await fetch(
          `http://localhost:8080/api/hardware-error-logs/popup-state?clientId=${encodeURIComponent(clientId)}&isOpen=false`,
          {
            method: 'POST',
          },
        )

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

    newLogs.forEach((newLog) => {
      // 에러 메시지가 이미 변환되어 있지 않은 경우 변환
      const processedLog = newLog.message
        ? newLog
        : {
            ...newLog,
            message: translateErrorKey(newLog.errorKey, newLog.isResolved),
            resolvedMessage: newLog.isResolved
              ? translateErrorKey(newLog.errorKey, newLog.isResolved)
              : undefined,
          }

      const existingIndex = errorLogs.value.findIndex((log) => log.id === processedLog.id)

      if (existingIndex !== -1) {
        // 기존 로그 업데이트 (해결 상태 변경 등)
        errorLogs.value[existingIndex] = processedLog
      } else {
        // 새 로그 추가
        errorLogs.value.unshift(processedLog)
      }
    })

    // 시간순 정렬 (최신순)
    errorLogs.value.sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
    )

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
        const parsedLogs = JSON.parse(saved)
        console.log('🔍 loadFromLocalStorage - 원본 데이터:', parsedLogs)

        // 로컬 스토리지에서 로드한 데이터에 다국어 변환 적용
        errorLogs.value = parsedLogs.map((log: HardwareErrorLog) => {
          const translatedMessage = translateErrorKey(log.errorKey, log.isResolved)
          console.log('🔍 loadFromLocalStorage - 번역 결과:', {
            errorKey: log.errorKey,
            isResolved: log.isResolved,
            translatedMessage,
          })

          return {
            ...log,
            message: translatedMessage,
            resolvedMessage: log.isResolved ? translatedMessage : undefined,
          }
        })

        console.log('🔍 loadFromLocalStorage - 최종 결과:', errorLogs.value)
      }
    } catch (e) {
      console.error('로컬 스토리지 로드 실패:', e)
    }
  }

  // 백엔드에서 히스토리 로드
  const loadHistoryFromBackend = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/hardware-error-logs')
      if (response.ok) {
        const data = await response.json()
        console.log('🔍 loadHistoryFromBackend - 원본 데이터:', data)

        // 백엔드에서 받은 데이터에 다국어 변환 적용
        errorLogs.value = data.map((log: HardwareErrorLog) => {
          const translatedMessage = translateErrorKey(log.errorKey, log.isResolved)
          console.log('🔍 loadHistoryFromBackend - 번역 결과:', {
            errorKey: log.errorKey,
            isResolved: log.isResolved,
            translatedMessage,
          })

          return {
            ...log,
            message: translatedMessage,
            resolvedMessage: log.isResolved ? translatedMessage : undefined,
          }
        })

        console.log('🔍 loadHistoryFromBackend - 최종 결과:', errorLogs.value)
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
    updateErrorMessages,
    translateErrorKey,
  }
})
