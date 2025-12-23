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

// 모드별 데이터 매핑 정의
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
      cmd: 'trackingCMDTrainAngle',
      actual: 'trackingActualTrainAngle',
      speed: 'trainSpeed',
    },
  },
  step: {
    azimuth: {
      cmd: 'cmdAzimuthAngle',
      actual: 'azimuthAngle',
      speed: 'azimuthSpeed',
    },
    elevation: {
      cmd: 'cmdElevationAngle',
      actual: 'elevationAngle',
      speed: 'elevationSpeed',
    },
    train: {
      cmd: 'cmdTrainAngle',
      actual: 'trainAngle',
      speed: 'trainSpeed',
    },
  },
  slew: {
    azimuth: {
      cmd: 'cmdAzimuthAngle',
      actual: 'azimuthAngle',
      speed: 'azimuthSpeed',
    },
    elevation: {
      cmd: 'cmdElevationAngle',
      actual: 'elevationAngle',
      speed: 'elevationSpeed',
    },
    train: {
      cmd: 'cmdTrainAngle',
      actual: 'trainAngle',
      speed: 'trainSpeed',
    },
  },
  pedestal: {
    azimuth: {
      cmd: 'cmdAzimuthAngle',
      actual: 'azimuthAngle',
      speed: 'azimuthSpeed',
    },
    elevation: {
      cmd: 'cmdElevationAngle',
      actual: 'elevationAngle',
      speed: 'elevationSpeed',
    },
    train: {
      cmd: 'cmdTrainAngle',
      actual: 'trainAngle',
      speed: 'trainSpeed',
    },
  },
  suntrack: {
    azimuth: {
      cmd: 'cmdAzimuthAngle',
      actual: 'azimuthAngle',
      speed: 'azimuthSpeed',
    },
    elevation: {
      cmd: 'cmdElevationAngle',
      actual: 'elevationAngle',
      speed: 'elevationSpeed',
    },
    train: {
      cmd: 'cmdTrainAngle',
      actual: 'trainAngle',
      speed: 'trainSpeed',
    },
  },
  standby: {
    azimuth: {
      cmd: 'cmdAzimuthAngle',
      actual: 'azimuthAngle',
      speed: 'azimuthSpeed',
    },
    elevation: {
      cmd: 'cmdElevationAngle',
      actual: 'elevationAngle',
      speed: 'elevationSpeed',
    },
    train: {
      cmd: 'cmdTrainAngle',
      actual: 'trainAngle',
      speed: 'trainSpeed',
    },
  },
  feed: {
    azimuth: {
      cmd: 'cmdAzimuthAngle',
      actual: 'azimuthAngle',
      speed: 'azimuthSpeed',
    },
    elevation: {
      cmd: 'cmdElevationAngle',
      actual: 'elevationAngle',
      speed: 'elevationSpeed',
    },
    train: {
      cmd: 'cmdTrainAngle',
      actual: 'trainAngle',
      speed: 'trainSpeed',
    },
  },
}

export const useModeStore = defineStore('mode', () => {
  // 현재 모드 상태
  const currentMode = ref<ModeType>('standby')
  const previousMode = ref<ModeType>('standby')

  // 모드 변경 시간 추적
  const modeChangedAt = ref<Date>(new Date())

  // 현재 모드의 데이터 매핑 가져오기
  const currentModeMapping = computed(() => {
    return MODE_DATA_MAPPINGS[currentMode.value] || MODE_DATA_MAPPINGS.standby
  })

  // 모드 변경 함수
  const setMode = (mode: ModeType) => {
    if (currentMode.value !== mode) {
      previousMode.value = currentMode.value
      currentMode.value = mode
      modeChangedAt.value = new Date()

      console.log(`🔄 모드 변경: ${previousMode.value} → ${currentMode.value}`)

      // 로컬 스토리지에 저장 (페이지 새로고침 시 복원용)
      localStorage.setItem('currentMode', mode)
    }
  }

  // 특정 축의 데이터 필드명 가져오기
  const getAxisFieldName = (
    axis: 'azimuth' | 'elevation' | 'train',
    type: 'cmd' | 'actual' | 'speed',
  ): string => {
    return currentModeMapping.value[axis][type]
  }

  // 모드 초기화 (로컬 스토리지에서 복원)
  const initializeMode = () => {
    const savedMode = localStorage.getItem('currentMode') as ModeType
    if (savedMode && Object.keys(MODE_DATA_MAPPINGS).includes(savedMode)) {
      currentMode.value = savedMode
      console.log(`📱 저장된 모드 복원: ${savedMode}`)
    }
  }

  // 모드 상태 정보
  const modeInfo = computed(() => ({
    current: currentMode.value,
    previous: previousMode.value,
    changedAt: modeChangedAt.value,
    mapping: currentModeMapping.value,
  }))

  return {
    // 상태
    currentMode,
    previousMode,
    modeChangedAt,
    currentModeMapping,
    modeInfo,

    // 메서드
    setMode,
    getAxisFieldName,
    initializeMode,
  }
})
