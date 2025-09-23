import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { AngleLimitsSettings } from '@/services/api/settingsService'

export const useAngleLimitsSettingsStore = defineStore('angleLimitsSettings', () => {
  // === 상태 ===
  const angleLimitsSettings = ref<AngleLimitsSettings>({
    azimuthMin: -270.0,
    azimuthMax: 270.0,
    elevationMin: 0.0,
    elevationMax: 180.0,
    trainMin: -270.0,
    trainMax: 270.0,
  })

  const loadingStates = ref({
    angleLimits: false,
  })

  const errorStates = ref({
    angleLimits: null as string | null,
  })

  // 변경사항 추적을 위한 상태 추가
  const hasUnsavedChanges = ref(false)
  const pendingChanges = ref<AngleLimitsSettings | null>(null)

  // === 게터 ===
  const isLoading = computed(() => loadingStates.value.angleLimits)
  const hasError = computed(() => errorStates.value.angleLimits !== null)

  // === 액션 ===
  const loadAngleLimitsSettings = async () => {
    try {
      loadingStates.value.angleLimits = true
      errorStates.value.angleLimits = null

      const settings = await settingsService.getAngleLimitsSettings()
      angleLimitsSettings.value = settings
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '각도 제한 설정 로드 실패'
      errorStates.value.angleLimits = errorMessage
      console.error('각도 제한 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '각도 제한 설정 로드')
    } finally {
      loadingStates.value.angleLimits = false
    }
  }

  const saveAngleLimitsSettings = async (settings: AngleLimitsSettings) => {
    try {
      loadingStates.value.angleLimits = true
      errorStates.value.angleLimits = null
      await settingsService.setAngleLimitsSettings(settings)
      angleLimitsSettings.value = settings
      hasUnsavedChanges.value = false
      pendingChanges.value = null

      const { addError } = useErrorHandler()
      addError('user', '각도 제한 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '각도 제한 설정 저장 실패'
      errorStates.value.angleLimits = errorMessage
      console.error('각도 제한 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '각도 제한 설정 저장')
    } finally {
      loadingStates.value.angleLimits = false
    }
  }

  // 변경사항 상태 업데이트 함수 추가
  const updateChangeStatus = (hasChanges: boolean, changes?: AngleLimitsSettings) => {
    hasUnsavedChanges.value = hasChanges
    if (hasChanges && changes) {
      pendingChanges.value = changes
    } else if (!hasChanges) {
      pendingChanges.value = null
    }
  }

  // === 반환값 ===
  return {
    angleLimitsSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,
    isLoading,
    hasError,
    loadAngleLimitsSettings,
    saveAngleLimitsSettings,
    updateChangeStatus,
  }
})
