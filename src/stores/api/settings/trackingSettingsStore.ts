import { defineStore } from 'pinia'
import { ref } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { TrackingSettings } from '@/services/api/settingsService'

export const useTrackingSettingsStore = defineStore('trackingSettings', () => {
  // === 상태 ===
  const trackingSettings = ref<TrackingSettings>({
    msInterval: 1000,
    durationDays: 1,
    minElevationAngle: 10.0,
  })

  const loadingStates = ref({
    tracking: false,
  })

  const errorStates = ref({
    tracking: null as string | null,
  })

  // === 변경사항 관리 ===
  const hasUnsavedChanges = ref<boolean>(false)
  const pendingChanges = ref<TrackingSettings | null>(null)

  // === 액션 ===
  const loadTrackingSettings = async () => {
    loadingStates.value.tracking = true
    errorStates.value.tracking = null

    try {
      const response = await settingsService.getTrackingSettings()
      trackingSettings.value = response
    } catch (error) {
      const errorMessage = useErrorHandler().handleApiError(error)
      errorStates.value.tracking = errorMessage
      throw error
    } finally {
      loadingStates.value.tracking = false
    }
  }

  const saveTrackingSettings = async (settings: TrackingSettings) => {
    loadingStates.value.tracking = true
    errorStates.value.tracking = null

    try {
      await settingsService.setTrackingSettings(settings)
      // 저장 후 로컬 상태 업데이트
      trackingSettings.value = { ...settings }
      hasUnsavedChanges.value = false
      pendingChanges.value = null
    } catch (error) {
      const errorMessage = useErrorHandler().handleApiError(error)
      errorStates.value.tracking = errorMessage
      throw error
    } finally {
      loadingStates.value.tracking = false
    }
  }

  // === 변경사항 관리 함수 ===
  const updateChangeStatus = (hasChanges: boolean, changes?: TrackingSettings) => {
    hasUnsavedChanges.value = hasChanges
    if (hasChanges && changes) {
      pendingChanges.value = changes
    } else if (!hasChanges) {
      pendingChanges.value = null
    }
  }

  return {
    // 상태
    trackingSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,

    // 액션
    loadTrackingSettings,
    saveTrackingSettings,
    updateChangeStatus,
  }
})
