import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useLocationSettingsStore } from './locationSettingsStore'
import { useAlgorithmSettingsStore } from './algorithmSettingsStore'
import { useAngleLimitsSettingsStore } from './angleLimitsSettingsStore'
import { useAntennaSpecSettingsStore } from './antennaSpecSettingsStore'
import { useOffsetLimitsSettingsStore } from './offsetLimitsSettingsStore'
import { useSpeedLimitsSettingsStore } from './speedLimitsSettingsStore'
import { useStepSizeLimitSettingsStore } from './stepSizeLimitSettingsStore'
import { useStowSettingsStore } from './stowSettingsStore'
import { useTrackingSettingsStore } from './trackingSettingsStore'

export const useSettingsStore = defineStore('settings', () => {
  // === 상태 ===
  const loadingStates = ref({
    loadAll: false,
    saveAll: false,
  })

  const errorStates = ref({
    loadAll: null as string | null,
    saveAll: null as string | null,
  })

  // === 액션 ===
  const loadAllSettings = async () => {
    loadingStates.value.loadAll = true
    errorStates.value.loadAll = null

    try {
      // 모든 개별 Store의 load 함수들을 병렬로 실행
      await Promise.all([
        useLocationSettingsStore().loadLocationSettings(),
        useAlgorithmSettingsStore().loadAlgorithmSettings(),
        useAngleLimitsSettingsStore().loadAngleLimitsSettings(),
        useAntennaSpecSettingsStore().loadAntennaSpecSettings(),
        useOffsetLimitsSettingsStore().loadAngleOffsetLimitsSettings(),
        useOffsetLimitsSettingsStore().loadTimeOffsetLimitsSettings(),
        useSpeedLimitsSettingsStore().loadSpeedLimitsSettings(),
        useStepSizeLimitSettingsStore().loadStepSizeLimitSettings(),
        useStowSettingsStore().loadStowAngleSettings(),
        useStowSettingsStore().loadStowSpeedSettings(),
        useTrackingSettingsStore().loadTrackingSettings(),
      ])
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '설정 로드 실패'
      errorStates.value.loadAll = errorMessage
      throw error
    } finally {
      loadingStates.value.loadAll = false
    }
  }

  const saveAllSettings = async () => {
    loadingStates.value.saveAll = true
    errorStates.value.saveAll = null

    try {
      // 변경사항이 있는 Store들만 저장
      const savePromises = []

      // 각 Store의 변경사항 확인 및 저장
      const locationStore = useLocationSettingsStore()
      if (locationStore.hasUnsavedChanges) {
        savePromises.push(locationStore.saveLocationSettings(locationStore.pendingChanges))
      }

      const algorithmStore = useAlgorithmSettingsStore()
      if (algorithmStore.hasUnsavedChanges) {
        savePromises.push(algorithmStore.saveAlgorithmSettings(algorithmStore.pendingChanges))
      }

      const angleLimitsStore = useAngleLimitsSettingsStore()
      if (angleLimitsStore.hasUnsavedChanges) {
        savePromises.push(angleLimitsStore.saveAngleLimitsSettings(angleLimitsStore.pendingChanges))
      }

      const antennaSpecStore = useAntennaSpecSettingsStore()
      if (antennaSpecStore.hasUnsavedChanges) {
        savePromises.push(antennaSpecStore.saveAntennaSpecSettings(antennaSpecStore.pendingChanges))
      }

      const offsetLimitsStore = useOffsetLimitsSettingsStore()
      if (offsetLimitsStore.hasUnsavedChanges.angle) {
        savePromises.push(
          offsetLimitsStore.saveAngleOffsetLimitsSettings(offsetLimitsStore.pendingChanges.angle),
        )
      }
      if (offsetLimitsStore.hasUnsavedChanges.time) {
        savePromises.push(
          offsetLimitsStore.saveTimeOffsetLimitsSettings(offsetLimitsStore.pendingChanges.time),
        )
      }

      const speedLimitsStore = useSpeedLimitsSettingsStore()
      if (speedLimitsStore.hasUnsavedChanges) {
        savePromises.push(speedLimitsStore.saveSpeedLimitsSettings(speedLimitsStore.pendingChanges))
      }

      const stepSizeLimitStore = useStepSizeLimitSettingsStore()
      if (stepSizeLimitStore.hasUnsavedChanges) {
        savePromises.push(
          stepSizeLimitStore.saveStepSizeLimitSettings(stepSizeLimitStore.pendingChanges),
        )
      }

      const stowStore = useStowSettingsStore()
      if (stowStore.hasUnsavedChanges.angle) {
        savePromises.push(stowStore.saveStowAngleSettings(stowStore.pendingChanges.angle))
      }
      if (stowStore.hasUnsavedChanges.speed) {
        savePromises.push(stowStore.saveStowSpeedSettings(stowStore.pendingChanges.speed))
      }

      const trackingStore = useTrackingSettingsStore()
      if (trackingStore.hasUnsavedChanges) {
        savePromises.push(trackingStore.saveTrackingSettings(trackingStore.pendingChanges))
      }

      // 변경사항이 있는 경우에만 저장 실행
      if (savePromises.length > 0) {
        await Promise.all(savePromises)
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '설정 저장 실패'
      errorStates.value.saveAll = errorMessage
      throw error
    } finally {
      loadingStates.value.saveAll = false
    }
  }

  // === 변경사항 확인 ===
  const hasAnyUnsavedChanges = computed(() => {
    // 각 Store의 상태를 직접 참조하여 반응성 확보
    const locationStore = useLocationSettingsStore()
    const algorithmStore = useAlgorithmSettingsStore()
    const angleLimitsStore = useAngleLimitsSettingsStore()
    const antennaSpecStore = useAntennaSpecSettingsStore()
    const offsetLimitsStore = useOffsetLimitsSettingsStore()
    const speedLimitsStore = useSpeedLimitsSettingsStore()
    const stepSizeLimitStore = useStepSizeLimitSettingsStore()
    const stowStore = useStowSettingsStore()
    const trackingStore = useTrackingSettingsStore()

    return (
      locationStore.hasUnsavedChanges ||
      algorithmStore.hasUnsavedChanges ||
      angleLimitsStore.hasUnsavedChanges ||
      antennaSpecStore.hasUnsavedChanges ||
      offsetLimitsStore.hasUnsavedChanges.angle ||
      offsetLimitsStore.hasUnsavedChanges.time ||
      speedLimitsStore.hasUnsavedChanges ||
      stepSizeLimitStore.hasUnsavedChanges ||
      stowStore.hasUnsavedChanges.angle ||
      stowStore.hasUnsavedChanges.speed ||
      trackingStore.hasUnsavedChanges
    )
  })

  return {
    // 상태
    loadingStates,
    errorStates,
    hasAnyUnsavedChanges,

    // 액션
    loadAllSettings,
    saveAllSettings,
  }
})
