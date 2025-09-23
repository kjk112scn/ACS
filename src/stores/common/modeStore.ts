import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type ModeType = 'standby' | 'step' | 'slew' | 'pedestal' | 'ephemeris' | 'suntrack' | 'feed'

export interface AxisData {
  cmd: string
  actual: string
  speed: string
}

export interface ModeDataMapping {
  azimuth: AxisData
  elevation: AxisData
  train: AxisData
}

// 모드별 데이터 매핑 정의 (기존과 동일)
const MODE_DATA_MAPPINGS: Record<ModeType, ModeDataMapping> = {
  ephemeris: {
    azimuth: {
      cmd: 'trackingCMDAzimuthAngle',
      actual: 'trackingActualAzimuthAngle',
      speed: 'azimuthSpeed',
    },
    elevation: {
      cmd: 'trackingCMDElevationAngle',
      actual: 'trackingActualElevationAngle',
      speed: 'elevationSpeed',
    },
    train: {
      cmd: 'trackingCMDTiltAngle',
      actual: 'trackingActualTiltAngle',
      speed: 'tiltSpeed',
    },
  },
  // ... 다른 모드들은 기본 데이터 사용
  step: {
    azimuth: { cmd: 'cmdAzimuthAngle', actual: 'azimuthAngle', speed: 'azimuthSpeed' },
    elevation: { cmd: 'cmdElevationAngle', actual: 'elevationAngle', speed: 'elevationSpeed' },
    train: { cmd: 'cmdTiltAngle', actual: 'tiltAngle', speed: 'tiltSpeed' },
  },
  slew: {
    azimuth: { cmd: 'cmdAzimuthAngle', actual: 'azimuthAngle', speed: 'azimuthSpeed' },
    elevation: { cmd: 'cmdElevationAngle', actual: 'elevationAngle', speed: 'elevationSpeed' },
    train: { cmd: 'cmdTiltAngle', actual: 'tiltAngle', speed: 'tiltSpeed' },
  },
  pedestal: {
    azimuth: { cmd: 'cmdAzimuthAngle', actual: 'azimuthAngle', speed: 'azimuthSpeed' },
    elevation: { cmd: 'cmdElevationAngle', actual: 'elevationAngle', speed: 'elevationSpeed' },
    train: { cmd: 'cmdTiltAngle', actual: 'tiltAngle', speed: 'tiltSpeed' },
  },
  suntrack: {
    azimuth: { cmd: 'cmdAzimuthAngle', actual: 'azimuthAngle', speed: 'azimuthSpeed' },
    elevation: { cmd: 'cmdElevationAngle', actual: 'elevationAngle', speed: 'elevationSpeed' },
    train: { cmd: 'cmdTiltAngle', actual: 'tiltAngle', speed: 'tiltSpeed' },
  },
  standby: {
    azimuth: { cmd: 'cmdAzimuthAngle', actual: 'azimuthAngle', speed: 'azimuthSpeed' },
    elevation: { cmd: 'cmdElevationAngle', actual: 'elevationAngle', speed: 'elevationSpeed' },
    train: { cmd: 'cmdTiltAngle', actual: 'tiltAngle', speed: 'tiltSpeed' },
  },
  feed: {
    azimuth: { cmd: 'cmdAzimuthAngle', actual: 'azimuthAngle', speed: 'azimuthSpeed' },
    elevation: { cmd: 'cmdElevationAngle', actual: 'elevationAngle', speed: 'elevationSpeed' },
    train: { cmd: 'cmdTiltAngle', actual: 'tiltAngle', speed: 'tiltSpeed' },
  },
}

export const useModeStore = defineStore('mode', () => {
  // 현재 선택된 모드 (탭 선택)
  const selectedMode = ref<ModeType>('standby')

  // 실제 활성화된 모드 (Go 버튼 클릭 후)
  const activeMode = ref<ModeType>('standby')

  // 이전 활성 모드
  const previousActiveMode = ref<ModeType>('standby')

  // 모드 활성화 시간 추적
  const modeActivatedAt = ref<Date>(new Date())

  // 모드별 활성화 상태
  const modeActivationStatus = ref<Record<ModeType, boolean>>({
    standby: true, // standby는 기본적으로 활성화
    step: false,
    slew: false,
    pedestal: false,
    ephemeris: false,
    suntrack: false,
    feed: false,
  })

  // 현재 활성 모드의 데이터 매핑 가져오기
  const activeModeMapping = computed(() => {
    return MODE_DATA_MAPPINGS[activeMode.value] || MODE_DATA_MAPPINGS.standby
  })

  // 선택된 모드 변경 (탭 클릭 시)
  const setSelectedMode = (mode: ModeType) => {
    selectedMode.value = mode
    console.log(`📋 모드 선택됨: ${mode} (아직 활성화되지 않음)`)

    // 로컬 스토리지에 선택된 모드 저장
    localStorage.setItem('selectedMode', mode)
  }

  // 모드 활성화 (Go 버튼 클릭 시)
  const activateMode = (mode: ModeType) => {
    // 이전 모드 비활성화
    if (activeMode.value !== mode) {
      modeActivationStatus.value[activeMode.value] = false
      previousActiveMode.value = activeMode.value
    }

    // 새 모드 활성화
    activeMode.value = mode
    modeActivationStatus.value[mode] = true
    modeActivatedAt.value = new Date()

    console.log(`🚀 모드 활성화됨: ${previousActiveMode.value} → ${activeMode.value}`)

    // 로컬 스토리지에 활성 모드 저장
    localStorage.setItem('activeMode', mode)
  }

  // 모드 비활성화 (Stop 버튼 클릭 시)
  const deactivateMode = (mode: ModeType) => {
    if (mode !== 'standby') {
      modeActivationStatus.value[mode] = false

      // standby 모드로 복귀
      if (activeMode.value === mode) {
        previousActiveMode.value = activeMode.value
        activeMode.value = 'standby'
        modeActivationStatus.value.standby = true
        modeActivatedAt.value = new Date()

        console.log(`⏹️ 모드 비활성화됨: ${mode} → standby`)
        localStorage.setItem('activeMode', 'standby')
      }
    }
  }

  // 특정 축의 데이터 필드명 가져오기 (활성 모드 기준)
  const getAxisFieldName = (
    axis: 'azimuth' | 'elevation' | 'train',
    type: 'cmd' | 'actual' | 'speed',
  ): string => {
    return activeModeMapping.value[axis][type]
  }

  // 모드가 활성화되어 있는지 확인
  const isModeActive = (mode: ModeType): boolean => {
    return modeActivationStatus.value[mode] || false
  }

  // 현재 활성 모드가 특정 모드인지 확인
  const isCurrentActiveMode = (mode: ModeType): boolean => {
    return activeMode.value === mode && modeActivationStatus.value[mode]
  }

  // 초기화 (로컬 스토리지에서 복원)
  const initializeMode = () => {
    const savedSelectedMode = localStorage.getItem('selectedMode') as ModeType
    const savedActiveMode = localStorage.getItem('activeMode') as ModeType

    if (savedSelectedMode && Object.keys(MODE_DATA_MAPPINGS).includes(savedSelectedMode)) {
      selectedMode.value = savedSelectedMode
      console.log(`📱 저장된 선택 모드 복원: ${savedSelectedMode}`)
    }

    if (savedActiveMode && Object.keys(MODE_DATA_MAPPINGS).includes(savedActiveMode)) {
      // 모든 모드 비활성화 후 저장된 모드만 활성화
      Object.keys(modeActivationStatus.value).forEach((mode) => {
        modeActivationStatus.value[mode as ModeType] = false
      })

      activeMode.value = savedActiveMode
      modeActivationStatus.value[savedActiveMode] = true
      console.log(`🚀 저장된 활성 모드 복원: ${savedActiveMode}`)
    }
  }

  // 모드 상태 정보
  const modeInfo = computed(() => ({
    selected: selectedMode.value,
    active: activeMode.value,
    previous: previousActiveMode.value,
    activatedAt: modeActivatedAt.value,
    activationStatus: { ...modeActivationStatus.value },
    mapping: activeModeMapping.value,
  }))

  return {
    // 상태
    selectedMode,
    activeMode,
    previousActiveMode,
    modeActivatedAt,
    modeActivationStatus,
    activeModeMapping,
    modeInfo,

    // 메서드
    setSelectedMode,
    activateMode,
    deactivateMode,
    getAxisFieldName,
    isModeActive,
    isCurrentActiveMode,
    initializeMode,
  }
})
