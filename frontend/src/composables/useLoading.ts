import { ref, computed } from 'vue'
import { Loading } from 'quasar'

export interface LoadingOptions {
  message?: string
  spinner?: unknown
  spinnerColor?: string
  spinnerSize?: number
  backgroundColor?: string
  customClass?: string
  boxClass?: string
  delay?: number
}

export const useLoading = () => {
  const loadingStates = ref<Map<string, boolean>>(new Map())
  const globalLoading = ref(false)

  // 개별 로딩 상태 관리
  const setLoading = (key: string, isLoading: boolean) => {
    loadingStates.value.set(key, isLoading)
  }

  const isLoading = (key: string) => {
    return loadingStates.value.get(key) || false
  }

  const clearLoading = (key: string) => {
    loadingStates.value.delete(key)
  }

  // 전역 로딩 상태 관리
  const setGlobalLoading = (isLoading: boolean, options: LoadingOptions = {}) => {
    globalLoading.value = isLoading

    if (isLoading) {
      const loadingOptions: Record<string, unknown> = {
        message: options.message || '로딩 중...',
        spinner: options.spinner,
        spinnerColor: options.spinnerColor || 'primary',
        spinnerSize: options.spinnerSize || 40,
        backgroundColor: options.backgroundColor || 'rgba(0,0,0,0.5)',
        delay: options.delay || 0,
      }

      // customClass가 있을 때만 추가
      if (options.customClass) {
        loadingOptions.customClass = options.customClass
      }

      // boxClass가 있을 때만 추가
      if (options.boxClass) {
        loadingOptions.boxClass = options.boxClass
      }

      Loading.show(loadingOptions)
    } else {
      Loading.hide()
    }
  }

  // 로딩 상태 확인
  const hasAnyLoading = computed(() => {
    return (
      Array.from(loadingStates.value.values()).some((loading) => loading) || globalLoading.value
    )
  })

  // 로딩 개수 확인
  const loadingCount = computed(() => {
    return (
      Array.from(loadingStates.value.values()).filter((loading) => loading).length +
      (globalLoading.value ? 1 : 0)
    )
  })

  // 모든 로딩 상태 초기화
  const clearAllLoading = () => {
    loadingStates.value.clear()
    globalLoading.value = false
    Loading.hide()
  }

  // 비동기 작업 래퍼
  const withLoading = async <T>(
    key: string,
    asyncFn: () => Promise<T>,
    options: LoadingOptions = {},
  ): Promise<T> => {
    try {
      setLoading(key, true)
      if (options.message) {
        setGlobalLoading(true, options)
      }
      return await asyncFn()
    } finally {
      setLoading(key, false)
      if (options.message) {
        setGlobalLoading(false)
      }
    }
  }

  return {
    // 상태
    loadingStates: computed(() => loadingStates.value),
    globalLoading: computed(() => globalLoading.value),
    hasAnyLoading,
    loadingCount,

    // 메서드
    setLoading,
    isLoading,
    clearLoading,
    setGlobalLoading,
    clearAllLoading,
    withLoading,
  }
}
