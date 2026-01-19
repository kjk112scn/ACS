import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsService } from '@/services'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type {
  LocationSettings,
  TrackingSettings,
  StowAngleSettings,
  StowSpeedSettings,
  AntennaSpecSettings,
  AngleLimitsSettings,
  SpeedLimitsSettings,
  AngleOffsetLimitsSettings,
  TimeOffsetLimitsSettings,
  AlgorithmSettings,
  StepSizeLimitSettings,
} from '@/services/api/settingsService'

export const useSettingsStore = defineStore('settings', () => {
  // === 상태 (State) ===

  // Location Settings - 초기값을 0으로 변경
  const locationSettings = ref<LocationSettings>({
    latitude: 0,
    longitude: 0,
    altitude: 0,
  })

  // Tracking Settings
  const trackingSettings = ref<TrackingSettings>({
    msInterval: 100,
    durationDays: 1,
    minElevationAngle: 0,
    preparationTimeMinutes: 4,
  })

  // Stow Settings
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

  // Antenna Spec Settings
  const antennaSpecSettings = ref<AntennaSpecSettings>({
    trueNorthOffsetAngle: 0.0,
    tiltAngle: -7.0,
  })

  // Angle Limits Settings
  const angleLimitsSettings = ref<AngleLimitsSettings>({
    azimuthMin: -270.0,
    azimuthMax: 270.0,
    elevationMin: 0.0,
    elevationMax: 180.0,
    trainMin: -270.0,
    trainMax: 270.0,
  })

  // Speed Limits Settings
  const speedLimitsSettings = ref<SpeedLimitsSettings>({
    azimuthMin: 0.1,
    azimuthMax: 15.0,
    elevationMin: 0.1,
    elevationMax: 10.0,
    trainMin: 0.1,
    trainMax: 5.0,
  })

  // Angle Offset Limits Settings
  const angleOffsetLimitsSettings = ref<AngleOffsetLimitsSettings>({
    azimuth: 50.0,
    elevation: 50.0,
    train: 50.0,
  })

  // Time Offset Limits Settings
  const timeOffsetLimitsSettings = ref<TimeOffsetLimitsSettings>({
    min: 0.1,
    max: 99999,
  })

  // Algorithm Settings
  const algorithmSettings = ref<AlgorithmSettings>({
    geoMinMotion: 1.1,
  })

  // Step Size Limit Settings
  const stepSizeLimitSettings = ref<StepSizeLimitSettings>({
    min: 50,
    max: 50,
  })

  // === 로딩 상태 ===
  const loadingStates = ref({
    location: false,
    tracking: false,
    stow: false,
    antennaSpec: false,
    angleLimits: false,
    speedLimits: false,
    offsetLimits: false,
    algorithm: false,
    stepSize: false,
    all: false,
  })

  // === 에러 상태 ===
  const errorStates = ref({
    location: null as string | null,
    tracking: null as string | null,
    stow: null as string | null,
    antennaSpec: null as string | null,
    angleLimits: null as string | null,
    speedLimits: null as string | null,
    offsetLimits: null as string | null,
    algorithm: null as string | null,
    stepSize: null as string | null,
    all: null as string | null,
  })

  // 변경사항 상태 추가 (loadingStates 다음에 추가)
  const hasUnsavedChanges = ref({
    location: false,
    tracking: false,
    stowAngle: false,
    stowSpeed: false,
    antennaSpec: false,
    angleLimits: false,
    speedLimits: false,
    angleOffsetLimits: false,
    timeOffsetLimits: false,
    algorithm: false,
    stepSizeLimit: false,
  })

  // 변경된 값 저장을 위한 상태 추가
  const pendingChanges = ref({
    location: null as LocationSettings | null,
    tracking: null as TrackingSettings | null,
    stowAngle: null as StowAngleSettings | null,
    stowSpeed: null as StowSpeedSettings | null,
    antennaSpec: null as AntennaSpecSettings | null,
    angleLimits: null as AngleLimitsSettings | null,
    speedLimits: null as SpeedLimitsSettings | null,
    angleOffsetLimits: null as AngleOffsetLimitsSettings | null,
    timeOffsetLimits: null as TimeOffsetLimitsSettings | null,
    algorithm: null as AlgorithmSettings | null,
    stepSizeLimit: null as StepSizeLimitSettings | null,
  })

  // 변경된 값 저장 함수 추가
  const setPendingChanges = (settingsType: string, changes: unknown) => {
    pendingChanges.value[settingsType] = changes
  }

  // 변경된 값 가져오기 함수 추가
  const getPendingChanges = (settingsType: string) => {
    return pendingChanges.value[settingsType]
  }

  // 변경사항 상태 업데이트 함수 수정
  const updateChangeStatus = (settingsType: string, hasChanges: boolean, changes?: unknown) => {
    hasUnsavedChanges.value[settingsType] = hasChanges
    if (hasChanges && changes) {
      setPendingChanges(settingsType, changes)
    } else if (!hasChanges) {
      setPendingChanges(settingsType, null)
    }
  }

  // 변경사항 상태 가져오기 함수 추가
  const getChangeStatus = (settingsType: string) => {
    return hasUnsavedChanges.value[settingsType]
  }

  // === 게터 (Getters) ===

  // 전체 로딩 상태
  const isLoading = computed(() => {
    return Object.values(loadingStates.value).some((loading) => loading)
  })

  // 전체 에러 상태
  const hasError = computed(() => {
    return Object.values(errorStates.value).some((error) => error !== null)
  })

  // === 액션 (Actions) ===

  // Location Settings 액션들
  const loadLocationSettings = async () => {
    try {
      console.log('=== Store: 위치 설정 로드 시작 ===')
      loadingStates.value.location = true
      errorStates.value.location = null

      console.log('서비스 호출 전 Store 상태:', locationSettings.value)
      const settings = await settingsService.getLocationSettings()
      console.log('서비스에서 받은 데이터:', settings)

      locationSettings.value = settings
      console.log('업데이트 후 Store 상태:', locationSettings.value)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '위치 설정 로드 실패'
      errorStates.value.location = errorMessage
      console.error('위치 설정 로드 실패:', error)

      // 사용자에게 알림 표시
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

      // 성공 알림
      const { addError } = useErrorHandler()
      addError('user', '위치 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '위치 설정 저장 실패'
      errorStates.value.location = errorMessage
      console.error('위치 설정 저장 실패:', error)

      // 실패 알림
      const { handleApiError } = useErrorHandler()
      handleApiError(error, '위치 설정 저장')
    } finally {
      loadingStates.value.location = false
    }
  }

  // Tracking Settings 액션들
  const loadTrackingSettings = async () => {
    try {
      loadingStates.value.tracking = true
      errorStates.value.tracking = null
      const settings = await settingsService.getTrackingSettings()
      trackingSettings.value = settings
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '추적 설정 로드 실패'
      errorStates.value.tracking = errorMessage
      console.error('추적 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '추적 설정 로드')
    } finally {
      loadingStates.value.tracking = false
    }
  }

  const saveTrackingSettings = async (settings: TrackingSettings) => {
    try {
      loadingStates.value.tracking = true
      errorStates.value.tracking = null
      await settingsService.setTrackingSettings(settings)
      trackingSettings.value = settings

      const { addError } = useErrorHandler()
      addError('user', '추적 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '추적 설정 저장 실패'
      errorStates.value.tracking = errorMessage
      console.error('추적 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '추적 설정 저장')
    } finally {
      loadingStates.value.tracking = false
    }
  }

  // Stow Settings 액션들
  const loadStowAngleSettings = async () => {
    try {
      loadingStates.value.stow = true
      errorStates.value.stow = null
      const settings = await settingsService.getStowAngleSettings()
      stowAngleSettings.value = settings
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Stow 각도 설정 로드 실패'
      errorStates.value.stow = errorMessage
      console.error('Stow 각도 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, 'Stow 각도 설정 로드')
    } finally {
      loadingStates.value.stow = false
    }
  }

  const saveStowAngleSettings = async (settings: StowAngleSettings) => {
    try {
      loadingStates.value.stow = true
      errorStates.value.stow = null
      await settingsService.setStowAngleSettings(settings)
      stowAngleSettings.value = settings

      const { addError } = useErrorHandler()
      addError('user', 'Stow 각도 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Stow 각도 설정 저장 실패'
      errorStates.value.stow = errorMessage
      console.error('Stow 각도 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, 'Stow 각도 설정 저장')
    } finally {
      loadingStates.value.stow = false
    }
  }

  const loadStowSpeedSettings = async () => {
    try {
      loadingStates.value.stow = true
      errorStates.value.stow = null
      const settings = await settingsService.getStowSpeedSettings()
      stowSpeedSettings.value = settings
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Stow 속도 설정 로드 실패'
      errorStates.value.stow = errorMessage
      console.error('Stow 속도 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, 'Stow 속도 설정 로드')
    } finally {
      loadingStates.value.stow = false
    }
  }

  const saveStowSpeedSettings = async (settings: StowSpeedSettings) => {
    try {
      loadingStates.value.stow = true
      errorStates.value.stow = null
      await settingsService.setStowSpeedSettings(settings)
      stowSpeedSettings.value = settings

      const { addError } = useErrorHandler()
      addError('user', 'Stow 속도 설정이 성공적으로 저장되었습니다.', undefined, {
        showNotification: true,
        autoResolve: true,
        resolveTimeout: 3000,
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Stow 속도 설정 저장 실패'
      errorStates.value.stow = errorMessage
      console.error('Stow 속도 설정 저장 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, 'Stow 속도 설정 저장')
    } finally {
      loadingStates.value.stow = false
    }
  }

  // Antenna Spec Settings 액션들
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

  // Angle Limits Settings 액션들
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

  // Speed Limits Settings 액션들
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

  // Angle Offset Limits Settings 액션들
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

  const saveAngleOffsetLimitsSettings = async (settings: AngleOffsetLimitsSettings) => {
    try {
      loadingStates.value.offsetLimits = true
      errorStates.value.offsetLimits = null
      await settingsService.setAngleOffsetLimitsSettings(settings)
      angleOffsetLimitsSettings.value = settings

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

  // Time Offset Limits Settings 액션들
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

  const saveTimeOffsetLimitsSettings = async (settings: TimeOffsetLimitsSettings) => {
    try {
      loadingStates.value.offsetLimits = true
      errorStates.value.offsetLimits = null
      await settingsService.setTimeOffsetLimitsSettings(settings)
      timeOffsetLimitsSettings.value = settings

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

  // Algorithm Settings 액션들
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

  // Step Size Limit Settings 액션들
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

  // 전체 설정 로드
  const loadAllSettings = async () => {
    try {
      loadingStates.value.all = true
      errorStates.value.all = null

      // 모든 설정을 병렬로 로드
      await Promise.all([
        loadLocationSettings(),
        loadTrackingSettings(),
        loadStowAngleSettings(),
        loadStowSpeedSettings(),
        loadAntennaSpecSettings(),
        loadAngleLimitsSettings(),
        loadSpeedLimitsSettings(),
        loadAngleOffsetLimitsSettings(),
        loadTimeOffsetLimitsSettings(),
        loadAlgorithmSettings(),
        loadStepSizeLimitSettings(),
      ])
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '전체 설정 로드 실패'
      errorStates.value.all = errorMessage
      console.error('전체 설정 로드 실패:', error)

      const { handleApiError } = useErrorHandler()
      handleApiError(error, '전체 설정 로드')
    } finally {
      loadingStates.value.all = false
    }
  }

  // 에러 상태 초기화
  const clearErrors = () => {
    Object.keys(errorStates.value).forEach((key) => {
      errorStates.value[key as keyof typeof errorStates.value] = null
    })
  }

  // === 반환값 ===
  return {
    // 상태
    locationSettings,
    trackingSettings,
    stowAngleSettings,
    stowSpeedSettings,
    antennaSpecSettings,
    angleLimitsSettings,
    speedLimitsSettings,
    angleOffsetLimitsSettings,
    timeOffsetLimitsSettings,
    algorithmSettings,
    stepSizeLimitSettings,
    loadingStates,
    errorStates,
    hasUnsavedChanges,
    updateChangeStatus,
    getChangeStatus,
    pendingChanges,
    setPendingChanges,
    getPendingChanges,

    // 게터
    isLoading,
    hasError,

    // 액션
    loadLocationSettings,
    saveLocationSettings,
    loadTrackingSettings,
    saveTrackingSettings,
    loadStowAngleSettings,
    saveStowAngleSettings,
    loadStowSpeedSettings,
    saveStowSpeedSettings,
    loadAntennaSpecSettings,
    saveAntennaSpecSettings,
    loadAngleLimitsSettings,
    saveAngleLimitsSettings,
    loadSpeedLimitsSettings,
    saveSpeedLimitsSettings,
    loadAngleOffsetLimitsSettings,
    saveAngleOffsetLimitsSettings,
    loadTimeOffsetLimitsSettings,
    saveTimeOffsetLimitsSettings,
    loadAlgorithmSettings,
    saveAlgorithmSettings,
    loadStepSizeLimitSettings,
    saveStepSizeLimitSettings,
    loadAllSettings,
    clearErrors,
  }
})
