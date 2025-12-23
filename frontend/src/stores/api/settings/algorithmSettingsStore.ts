import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { AlgorithmSettings } from '@/services/api/settingsService'

export const useAlgorithmSettingsStore = defineStore('algorithmSettings', () => {
  // === 상태 ===
  const algorithmSettings = ref<AlgorithmSettings>({
    geoMinMotion: 1.1,
  })

  const loadingStates = ref({
    algorithm: false,
  })

  const errorStates = ref({
    algorithm: null as string | null,
  })

  // 변경사항 추적을 위한 상태 추가
  const hasUnsavedChanges = ref(false)
  const pendingChanges = ref<AlgorithmSettings | null>(null)

  // === 게터 ===
  const isLoading = computed(() => loadingStates.value.algorithm)
  const hasError = computed(() => errorStates.value.algorithm !== null)

  // === 액션 ===
  const loadAlgorithmSettings = async () => {
    try {
      loadingStates.value.algorithm = true
      errorStates.value.algorithm = null

      const settings = await settingsService.getAlgorithmSettings()
      algorithmSettings.value = settings
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알고리즘 설정 로드 실패'
      errorStates.value.algorithm = errorMessage
      console.error('알고리즘 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '알고리즘 설정 로드')
    } finally {
      loadingStates.value.algorithm = false
    }
  }

  const saveAlgorithmSettings = async (settings: AlgorithmSettings) => {
    try {
      loadingStates.value.algorithm = true
      errorStates.value.algorithm = null
      await settingsService.setAlgorithmSettings(settings)
      algorithmSettings.value = settings
      hasUnsavedChanges.value = false
      pendingChanges.value = null

      const { addError } = useErrorHandler()
      addError('user', '알고리즘 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알고리즘 설정 저장 실패'
      errorStates.value.algorithm = errorMessage
      console.error('알고리즘 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '알고리즘 설정 저장')
    } finally {
      loadingStates.value.algorithm = false
    }
  }

  // 변경사항 상태 업데이트 함수 추가
  const updateChangeStatus = (hasChanges: boolean, changes?: AlgorithmSettings) => {
    hasUnsavedChanges.value = hasChanges
    if (hasChanges && changes) {
      pendingChanges.value = changes
    } else if (!hasChanges) {
      pendingChanges.value = null
    }
  }

  // === 반환값 ===
  return {
    algorithmSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,
    isLoading,
    hasError,
    loadAlgorithmSettings,
    saveAlgorithmSettings,
    updateChangeStatus,
  }
})
