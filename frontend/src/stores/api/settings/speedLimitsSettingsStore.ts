import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { SpeedLimitsSettings } from '@/services/api/settingsService'

export const useSpeedLimitsSettingsStore = defineStore('speedLimitsSettings', () => {
  // === 상태 ===
  const speedLimitsSettings = ref<SpeedLimitsSettings>({
    azimuthMin: 0.1,
    azimuthMax: 15.0,
    elevationMin: 0.1,
    elevationMax: 10.0,
    trainMin: 0.1,
    trainMax: 5.0,
  })

  const loadingStates = ref({
    speedLimits: false,
  })

  const errorStates = ref({
    speedLimits: null as string | null,
  })

  // 변경사항 추적을 위한 상태 추가
  const hasUnsavedChanges = ref(false)
  const pendingChanges = ref<SpeedLimitsSettings | null>(null)

  // === 게터 ===
  const isLoading = computed(() => loadingStates.value.speedLimits)
  const hasError = computed(() => errorStates.value.speedLimits !== null)

  // === 액션 ===
  const loadSpeedLimitsSettings = async () => {
    try {
      loadingStates.value.speedLimits = true
      errorStates.value.speedLimits = null

      const settings = await settingsService.getSpeedLimitsSettings()
      speedLimitsSettings.value = settings
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '속도 제한 설정 로드 실패'
      errorStates.value.speedLimits = errorMessage
      console.error('속도 제한 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '속도 제한 설정 로드')
    } finally {
      loadingStates.value.speedLimits = false
    }
  }

  const saveSpeedLimitsSettings = async (settings: SpeedLimitsSettings) => {
    try {
      loadingStates.value.speedLimits = true
      errorStates.value.speedLimits = null
      await settingsService.setSpeedLimitsSettings(settings)
      speedLimitsSettings.value = settings
      hasUnsavedChanges.value = false
      pendingChanges.value = null

      const { addError } = useErrorHandler()
      addError('user', '속도 제한 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '속도 제한 설정 저장 실패'
      errorStates.value.speedLimits = errorMessage
      console.error('속도 제한 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '속도 제한 설정 저장')
    } finally {
      loadingStates.value.speedLimits = false
    }
  }

  // 변경사항 상태 업데이트 함수 추가
  const updateChangeStatus = (hasChanges: boolean, changes?: SpeedLimitsSettings) => {
    hasUnsavedChanges.value = hasChanges
    if (hasChanges && changes) {
      pendingChanges.value = changes
    } else if (!hasChanges) {
      pendingChanges.value = null
    }
  }

  // === 반환값 ===
  return {
    speedLimitsSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,
    isLoading,
    hasError,
    loadSpeedLimitsSettings,
    saveSpeedLimitsSettings,
    updateChangeStatus,
  }
})
