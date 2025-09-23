import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { AntennaSpecSettings } from '@/services/api/settingsService'

export const useAntennaSpecSettingsStore = defineStore('antennaSpecSettings', () => {
  // === 상태 ===
  const antennaSpecSettings = ref<AntennaSpecSettings>({
    trueNorthOffsetAngle: 0.0,
    tiltAngle: -7.0,
  })

  const loadingStates = ref({
    antennaSpec: false,
  })

  const errorStates = ref({
    antennaSpec: null as string | null,
  })

  // 변경사항 추적을 위한 상태 추가
  const hasUnsavedChanges = ref(false)
  const pendingChanges = ref<AntennaSpecSettings | null>(null)

  // === 게터 ===
  const isLoading = computed(() => loadingStates.value.antennaSpec)
  const hasError = computed(() => errorStates.value.antennaSpec !== null)

  // === 액션 ===
  const loadAntennaSpecSettings = async () => {
    try {
      loadingStates.value.antennaSpec = true
      errorStates.value.antennaSpec = null

      const settings = await settingsService.getAntennaSpecSettings()
      antennaSpecSettings.value = settings
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '안테나 사양 설정 로드 실패'
      errorStates.value.antennaSpec = errorMessage
      console.error('안테나 사양 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '안테나 사양 설정 로드')
    } finally {
      loadingStates.value.antennaSpec = false
    }
  }

  const saveAntennaSpecSettings = async (settings: AntennaSpecSettings) => {
    try {
      loadingStates.value.antennaSpec = true
      errorStates.value.antennaSpec = null
      await settingsService.setAntennaSpecSettings(settings)
      antennaSpecSettings.value = settings
      hasUnsavedChanges.value = false
      pendingChanges.value = null

      const { addError } = useErrorHandler()
      addError('user', '안테나 사양 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '안테나 사양 설정 저장 실패'
      errorStates.value.antennaSpec = errorMessage
      console.error('안테나 사양 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '안테나 사양 설정 저장')
    } finally {
      loadingStates.value.antennaSpec = false
    }
  }

  // 변경사항 상태 업데이트 함수 추가
  const updateChangeStatus = (hasChanges: boolean, changes?: AntennaSpecSettings) => {
    hasUnsavedChanges.value = hasChanges
    if (hasChanges && changes) {
      pendingChanges.value = changes
    } else if (!hasChanges) {
      pendingChanges.value = null
    }
  }

  // === 반환값 ===
  return {
    antennaSpecSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,
    isLoading,
    hasError,
    loadAntennaSpecSettings,
    saveAntennaSpecSettings,
    updateChangeStatus,
  }
})
