import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { StepSizeLimitSettings } from '@/services/api/settingsService'

export const useStepSizeLimitSettingsStore = defineStore('stepSizeLimitSettings', () => {
  // === 상태 ===
  const stepSizeLimitSettings = ref<StepSizeLimitSettings>({
    min: 50,
    max: 50,
  })

  const loadingStates = ref({
    stepSize: false,
  })

  const errorStates = ref({
    stepSize: null as string | null,
  })

  // 변경사항 추적을 위한 상태 추가
  const hasUnsavedChanges = ref(false)
  const pendingChanges = ref<StepSizeLimitSettings | null>(null)

  // === 게터 ===
  const isLoading = computed(() => loadingStates.value.stepSize)
  const hasError = computed(() => errorStates.value.stepSize !== null)

  // === 액션 ===
  const loadStepSizeLimitSettings = async () => {
    try {
      loadingStates.value.stepSize = true
      errorStates.value.stepSize = null

      const settings = await settingsService.getStepSizeLimitSettings()
      stepSizeLimitSettings.value = settings
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : '스텝 사이즈 제한 설정 로드 실패'
      errorStates.value.stepSize = errorMessage
      console.error('스텝 사이즈 제한 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '스텝 사이즈 제한 설정 로드')
    } finally {
      loadingStates.value.stepSize = false
    }
  }

  const saveStepSizeLimitSettings = async (settings: StepSizeLimitSettings) => {
    try {
      loadingStates.value.stepSize = true
      errorStates.value.stepSize = null
      await settingsService.setStepSizeLimitSettings(settings)
      stepSizeLimitSettings.value = settings
      hasUnsavedChanges.value = false
      pendingChanges.value = null

      const { addError } = useErrorHandler()
      addError('user', '스텝 사이즈 제한 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : '스텝 사이즈 제한 설정 저장 실패'
      errorStates.value.stepSize = errorMessage
      console.error('스텝 사이즈 제한 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '스텝 사이즈 제한 설정 저장')
    } finally {
      loadingStates.value.stepSize = false
    }
  }

  // 변경사항 상태 업데이트 함수 추가
  const updateChangeStatus = (hasChanges: boolean, changes?: StepSizeLimitSettings) => {
    hasUnsavedChanges.value = hasChanges
    if (hasChanges && changes) {
      pendingChanges.value = changes
    } else if (!hasChanges) {
      pendingChanges.value = null
    }
  }

  // === 반환값 ===
  return {
    stepSizeLimitSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,
    isLoading,
    hasError,
    loadStepSizeLimitSettings,
    saveStepSizeLimitSettings,
    updateChangeStatus,
  }
})
