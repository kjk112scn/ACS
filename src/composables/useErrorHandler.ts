import { ref, computed, readonly } from 'vue'
import { Notify } from 'quasar'

export interface ErrorInfo {
  id: string
  type: 'api' | 'validation' | 'network' | 'system' | 'user'
  message: string
  details?: string | undefined
  timestamp: number
  resolved?: boolean
}

export interface ErrorHandlerOptions {
  showNotification?: boolean
  logToConsole?: boolean
  autoResolve?: boolean
  resolveTimeout?: number
}

export const useErrorHandler = () => {
  const errors = ref<ErrorInfo[]>([])
  const isProcessing = ref(false)

  // 에러 타입별 색상 매핑
  const errorColors = {
    api: 'negative',
    validation: 'warning',
    network: 'info',
    system: 'negative',
    user: 'warning',
  } as const

  // 에러 타입별 아이콘 매핑
  const errorIcons = {
    api: 'error',
    validation: 'warning',
    network: 'wifi_off',
    system: 'bug_report',
    user: 'person_off',
  } as const

  // 에러 추가
  const addError = (
    type: ErrorInfo['type'],
    message: string,
    details?: string,
    options: ErrorHandlerOptions = {},
  ) => {
    const errorId = `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

    const errorInfo: ErrorInfo = {
      id: errorId,
      type,
      message,
      details,
      timestamp: Date.now(),
      resolved: false,
    }

    errors.value.push(errorInfo)

    // 옵션에 따른 처리
    if (options.showNotification !== false) {
      showErrorNotification(errorInfo)
    }

    if (options.logToConsole !== false) {
      console.error(`[${type.toUpperCase()}] ${message}`, details)
    }

    // 자동 해결 설정
    if (options.autoResolve && options.resolveTimeout) {
      setTimeout(() => {
        resolveError(errorId)
      }, options.resolveTimeout)
    }

    return errorId
  }

  // 에러 해결
  const resolveError = (errorId: string) => {
    const errorIndex = errors.value.findIndex((error) => error.id === errorId)
    if (errorIndex !== -1) {
      errors.value[errorIndex]!.resolved = true
    }
  }

  // 모든 에러 해결
  const resolveAllErrors = () => {
    errors.value.forEach((error) => {
      error.resolved = true
    })
  }

  // 에러 제거
  const removeError = (errorId: string) => {
    const errorIndex = errors.value.findIndex((error) => error.id === errorId)
    if (errorIndex !== -1) {
      errors.value.splice(errorIndex, 1)
    }
  }

  // 모든 에러 제거
  const clearAllErrors = () => {
    errors.value = []
  }

  // 에러 알림 표시
  const showErrorNotification = (error: ErrorInfo) => {
    const notifyOptions: Record<string, unknown> = {
      type: errorColors[error.type],
      icon: errorIcons[error.type],
      message: error.message,
      position: 'top-right',
      timeout: error.type === 'system' ? 0 : 5000,
      actions: [
        {
          label: '닫기',
          color: 'white',
          handler: () => resolveError(error.id),
        },
      ],
    }

    // caption이 있을 때만 추가
    if (error.details) {
      notifyOptions.caption = error.details
    }

    Notify.create(notifyOptions)
  }

  // API 에러 처리
  const handleApiError = (
    error:
      | Error
      | {
          response?: { data?: { message?: string }; status?: number; statusText?: string }
          message?: string
        },
    context?: string,
  ) => {
    const message =
      (error as { response?: { data?: { message?: string } }; message?: string }).response?.data
        ?.message ||
      (error as { message?: string }).message ||
      'API 요청 중 오류가 발생했습니다.'
    const details = context
      ? `${context}: ${(error as { response?: { status?: number; statusText?: string } }).response?.status} ${(error as { response?: { statusText?: string } }).response?.statusText}`
      : undefined

    return addError('api', message, details)
  }

  // 유효성 검사 에러 처리
  const handleValidationError = (field: string, message: string) => {
    return addError('validation', `${field}: ${message}`)
  }

  // 네트워크 에러 처리
  const handleNetworkError = (error: Error | { message?: string }) => {
    const message = '네트워크 연결에 문제가 있습니다.'
    const details = (error as { message?: string }).message || '인터넷 연결을 확인해주세요.'

    return addError('network', message, details)
  }

  // 시스템 에러 처리
  const handleSystemError = (error: Error | { message?: string }, context?: string) => {
    const message = '시스템 오류가 발생했습니다.'
    const details = context
      ? `${context}: ${(error as { message?: string }).message}`
      : (error as { message?: string }).message

    return addError('system', message, details, {
      showNotification: true,
      logToConsole: true,
      autoResolve: false,
    })
  }

  // 사용자 에러 처리
  const handleUserError = (message: string, details?: string) => {
    return addError('user', message, details)
  }

  // 에러 통계
  const errorStats = computed(() => {
    const stats = {
      total: errors.value.length,
      unresolved: errors.value.filter((e) => !e.resolved).length,
      byType: {} as Record<string, number>,
    }

    errors.value.forEach((error) => {
      stats.byType[error.type] = (stats.byType[error.type] || 0) + 1
    })

    return stats
  })

  // 최근 에러들
  const recentErrors = computed(() => {
    return errors.value
      .filter((e) => !e.resolved)
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, 10)
  })

  return {
    // 상태
    errors: readonly(errors),
    isProcessing: readonly(isProcessing),
    errorStats,
    recentErrors,

    // 메서드
    addError,
    resolveError,
    resolveAllErrors,
    removeError,
    clearAllErrors,
    handleApiError,
    handleValidationError,
    handleNetworkError,
    handleSystemError,
    handleUserError,
  }
}
