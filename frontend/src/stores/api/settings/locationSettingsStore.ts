import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Notify } from 'quasar'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { LocationSettings } from '@/services/api/settingsService'

export const useLocationSettingsStore = defineStore('locationSettings', () => {
  // === 상태 ===
  const locationSettings = ref<LocationSettings>({
    latitude: 0,
    longitude: 0,
    altitude: 0,
  })

  const loadingStates = ref({
    location: false,
  })

  const errorStates = ref({
    location: null as string | null,
  })

  const hasUnsavedChanges = ref(false)
  const pendingChanges = ref<LocationSettings | null>(null)

  // === 게터 ===
  const isLoading = computed(() => loadingStates.value.location)
  const hasError = computed(() => errorStates.value.location !== null)

  // === 액션 ===
  const loadLocationSettings = async () => {
    try {
      loadingStates.value.location = true
      errorStates.value.location = null

      const settings = await settingsService.getLocationSettings()
      locationSettings.value = settings
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '위치 설정 로드 실패'
      errorStates.value.location = errorMessage
      console.error('위치 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '위치 설정 로드')
    } finally {
      loadingStates.value.location = false
    }
  }

  const saveLocationSettings = async (settings: LocationSettings) => {
    try {
      loadingStates.value.location = true
      errorStates.value.location = null
      await settingsService.setLocationSettings(settings)
      locationSettings.value = settings
      hasUnsavedChanges.value = false
      pendingChanges.value = null

      Notify.create({
        type: 'positive',
        message: '위치 설정이 성공적으로 저장되었습니다.',
        position: 'top-right',
        timeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '위치 설정 저장 실패'
      errorStates.value.location = errorMessage
      console.error('위치 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '위치 설정 저장')
    } finally {
      loadingStates.value.location = false
    }
  }

  const updateChangeStatus = (hasChanges: boolean, changes?: LocationSettings) => {
    hasUnsavedChanges.value = hasChanges
    if (hasChanges && changes) {
      pendingChanges.value = changes
    } else if (!hasChanges) {
      pendingChanges.value = null
    }
  }

  // === 반환값 ===
  return {
    locationSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,
    isLoading,
    hasError,
    loadLocationSettings,
    saveLocationSettings,
    updateChangeStatus,
  }
})
