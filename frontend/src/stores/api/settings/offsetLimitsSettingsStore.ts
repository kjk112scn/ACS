import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type {
  AngleOffsetLimitsSettings,
  TimeOffsetLimitsSettings,
} from '@/services/api/settingsService'

export const useOffsetLimitsSettingsStore = defineStore('offsetLimitsSettings', () => {
  // === 상태 ===
  const angleOffsetLimitsSettings = ref<AngleOffsetLimitsSettings>({
    azimuth: 50.0,
    elevation: 50.0,
    train: 50.0,
  })

  const timeOffsetLimitsSettings = ref<TimeOffsetLimitsSettings>({
    min: 0.1,
    max: 99999,
  })

  const loadingStates = ref({
    offsetLimits: false,
  })

  const errorStates = ref({
    offsetLimits: null as string | null,
  })

  // 변경사항 추적을 위한 상태 추가
  const hasUnsavedChanges = ref({
    angle: false,
    time: false,
  })
  const pendingChanges = ref<{
    angle: AngleOffsetLimitsSettings | null
    time: TimeOffsetLimitsSettings | null
  }>({
    angle: null,
    time: null,
  })

  // === 게터 ===
  const isLoading = computed(() => loadingStates.value.offsetLimits)
  const hasError = computed(() => errorStates.value.offsetLimits !== null)

  // === 액션 ===
  const loadAngleOffsetLimitsSettings = async () => {
    try {
      loadingStates.value.offsetLimits = true
      errorStates.value.offsetLimits = null

      const settings = await settingsService.getAngleOffsetLimitsSettings()
      angleOffsetLimitsSettings.value = settings
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : '각도 오프셋 제한 설정 로드 실패'
      errorStates.value.offsetLimits = errorMessage
      console.error('각도 오프셋 제한 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '각도 오프셋 제한 설정 로드')
    } finally {
      loadingStates.value.offsetLimits = false
    }
  }

  const loadTimeOffsetLimitsSettings = async () => {
    try {
      loadingStates.value.offsetLimits = true
      errorStates.value.offsetLimits = null

      const settings = await settingsService.getTimeOffsetLimitsSettings()
      timeOffsetLimitsSettings.value = settings
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : '시간 오프셋 제한 설정 로드 실패'
      errorStates.value.offsetLimits = errorMessage
      console.error('시간 오프셋 제한 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '시간 오프셋 제한 설정 로드')
    } finally {
      loadingStates.value.offsetLimits = false
    }
  }

  const saveAngleOffsetLimitsSettings = async (settings: AngleOffsetLimitsSettings) => {
    try {
      loadingStates.value.offsetLimits = true
      errorStates.value.offsetLimits = null
      await settingsService.setAngleOffsetLimitsSettings(settings)
      angleOffsetLimitsSettings.value = settings
      hasUnsavedChanges.value.angle = false
      pendingChanges.value.angle = null

      const { addError } = useErrorHandler()
      addError('user', '각도 오프셋 제한 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : '각도 오프셋 제한 설정 저장 실패'
      errorStates.value.offsetLimits = errorMessage
      console.error('각도 오프셋 제한 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '각도 오프셋 제한 설정 저장')
    } finally {
      loadingStates.value.offsetLimits = false
    }
  }

  const saveTimeOffsetLimitsSettings = async (settings: TimeOffsetLimitsSettings) => {
    try {
      loadingStates.value.offsetLimits = true
      errorStates.value.offsetLimits = null
      await settingsService.setTimeOffsetLimitsSettings(settings)
      timeOffsetLimitsSettings.value = settings
      hasUnsavedChanges.value.time = false
      pendingChanges.value.time = null

      const { addError } = useErrorHandler()
      addError('user', '시간 오프셋 제한 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : '시간 오프셋 제한 설정 저장 실패'
      errorStates.value.offsetLimits = errorMessage
      console.error('시간 오프셋 제한 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '시간 오프셋 제한 설정 저장')
    } finally {
      loadingStates.value.offsetLimits = false
    }
  }

  // 변경사항 상태 업데이트 함수 추가
  const updateChangeStatus = (
    type: 'angle' | 'time',
    hasChanges: boolean,
    changes?: AngleOffsetLimitsSettings | TimeOffsetLimitsSettings,
  ) => {
    hasUnsavedChanges.value[type] = hasChanges
    if (hasChanges && changes) {
      if (type === 'angle') {
        pendingChanges.value.angle = changes as AngleOffsetLimitsSettings
      } else {
        pendingChanges.value.time = changes as TimeOffsetLimitsSettings
      }
    } else if (!hasChanges) {
      pendingChanges.value[type] = null
    }
  }

  // === 반환값 ===
  return {
    angleOffsetLimitsSettings,
    timeOffsetLimitsSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    pendingChanges,
    isLoading,
    hasError,
    loadAngleOffsetLimitsSettings,
    loadTimeOffsetLimitsSettings,
    saveAngleOffsetLimitsSettings,
    saveTimeOffsetLimitsSettings,
    updateChangeStatus,
  }
})
