import { defineStore } from 'pinia'
import { ref } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { StowAngleSettings, StowSpeedSettings } from '@/services/api/settingsService'

export const useStowSettingsStore = defineStore('stowSettings', () => {
  // === 상태 ===
  const stowAngleSettings = ref<StowAngleSettings>({
    azimuth: 0.0,
    elevation: 90.0,
    train: 0.0,
  })

  const stowSpeedSettings = ref<StowSpeedSettings>({
    azimuth: 5.0,
    elevation: 5.0,
    train: 5.0,
  })

  const loadingStates = ref({
    stowAngle: false,
    stowSpeed: false,
  })

  const errorStates = ref({
    stowAngle: null as string | null,
    stowSpeed: null as string | null,
  })

  // === 변경사항 관리 ===
  const hasUnsavedChanges = ref({
    angle: false,
    speed: false,
  })

  const pendingChanges = ref<{
    angle: StowAngleSettings | null
    speed: StowSpeedSettings | null
  }>({
    angle: null,
    speed: null,
  })

  // === 액션 ===
  const loadStowAngleSettings = async () => {
    loadingStates.value.stowAngle = true
    errorStates.value.stowAngle = null

    try {
      const response = await settingsService.getStowAngleSettings()
      stowAngleSettings.value = response
    } catch (error) {
      const errorMessage = useErrorHandler().handleApiError(error)
      errorStates.value.stowAngle = errorMessage
      throw error
    } finally {
      loadingStates.value.stowAngle = false
    }
  }

  const saveStowAngleSettings = async (settings: StowAngleSettings) => {
    loadingStates.value.stowAngle = true
    errorStates.value.stowAngle = null

    try {
      await settingsService.setStowAngleSettings(settings)
      // 저장 후 로컬 상태 업데이트
      stowAngleSettings.value = { ...settings }
      hasUnsavedChanges.value.angle = false
      pendingChanges.value.angle = null
    } catch (error) {
      const errorMessage = useErrorHandler().handleApiError(error)
      errorStates.value.stowAngle = errorMessage
      throw error
    } finally {
      loadingStates.value.stowAngle = false
    }
  }

  const loadStowSpeedSettings = async () => {
    loadingStates.value.stowSpeed = true
    errorStates.value.stowSpeed = null

    try {
      const response = await settingsService.getStowSpeedSettings()
      stowSpeedSettings.value = response
    } catch (error) {
      const errorMessage = useErrorHandler().handleApiError(error)
      errorStates.value.stowSpeed = errorMessage
      throw error
    } finally {
      loadingStates.value.stowSpeed = false
    }
  }

  const saveStowSpeedSettings = async (settings: StowSpeedSettings) => {
    loadingStates.value.stowSpeed = true
    errorStates.value.stowSpeed = null

    try {
      await settingsService.setStowSpeedSettings(settings)
      // 저장 후 로컬 상태 업데이트
      stowSpeedSettings.value = { ...settings }
      hasUnsavedChanges.value.speed = false
      pendingChanges.value.speed = null
    } catch (error) {
      const errorMessage = useErrorHandler().handleApiError(error)
      errorStates.value.stowSpeed = errorMessage
      throw error
    } finally {
      loadingStates.value.stowSpeed = false
    }
  }

  // === 변경사항 관리 함수 ===
  const updateChangeStatus = (
    type: 'angle' | 'speed',
    hasChanges: boolean,
    changes?: StowAngleSettings | StowSpeedSettings,
  ) => {
    hasUnsavedChanges.value[type] = hasChanges
    if (hasChanges && changes) {
      pendingChanges.value[type] = changes
    } else if (!hasChanges) {
      pendingChanges.value[type] = null
    }
  }

  return {
    // 상태
    stowAngleSettings,
    stowSpeedSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,

    // 액션
    loadStowAngleSettings,
    saveStowAngleSettings,
    loadStowSpeedSettings,
    saveStowSpeedSettings,
    updateChangeStatus,
  }
})
