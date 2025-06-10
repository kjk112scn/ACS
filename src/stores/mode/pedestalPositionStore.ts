import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface PedestalAxes {
  azimuth: boolean
  elevation: boolean
  tilt: boolean
}

export interface PedestalPositions {
  azimuth: string
  elevation: string
  tilt: string
}

export interface PedestalSpeeds {
  azimuth: string
  elevation: string
  tilt: string
}

export const usePedestalPositionStore = defineStore('pedestalPosition', () => {
  // 선택된 축 상태
  const selectedAxes = ref<PedestalAxes>({
    azimuth: false,
    elevation: false,
    tilt: false,
  })

  // 목표 위치
  const targetPositions = ref<PedestalPositions>({
    azimuth: '0.00',
    elevation: '0.00',
    tilt: '0.00',
  })

  // 목표 속도
  const targetSpeeds = ref<PedestalSpeeds>({
    azimuth: '0.50',
    elevation: '0.50',
    tilt: '0.50',
  })

  // 축 선택 상태 업데이트
  const updateSelectedAxis = (axis: keyof PedestalAxes, value: boolean) => {
    selectedAxes.value[axis] = value
  }

  // 목표 위치 업데이트
  const updateTargetPosition = (axis: keyof PedestalPositions, value: string) => {
    targetPositions.value[axis] = value
  }

  // 목표 속도 업데이트
  const updateTargetSpeed = (axis: keyof PedestalSpeeds, value: string) => {
    targetSpeeds.value[axis] = value
  }

  // 모든 축 선택 해제
  const clearAllAxes = () => {
    selectedAxes.value = {
      azimuth: false,
      elevation: false,
      tilt: false,
    }
  }

  // 모든 목표 위치 초기화
  const resetAllPositions = () => {
    targetPositions.value = {
      azimuth: '0.00',
      elevation: '0.00',
      tilt: '0.00',
    }
  }

  // 모든 목표 속도 초기화
  const resetAllSpeeds = () => {
    targetSpeeds.value = {
      azimuth: '0.50',
      elevation: '0.50',
      tilt: '0.50',
    }
  }

  // 전체 상태 초기화
  const resetAll = () => {
    selectedAxes.value = {
      azimuth: true,
      elevation: true,
      tilt: true,
    }
    resetAllPositions()
    resetAllSpeeds()
  }

  // 최소 하나의 축이 선택되었는지 확인
  const isAnyAxisSelected = () => {
    return selectedAxes.value.azimuth || selectedAxes.value.elevation || selectedAxes.value.tilt
  }

  return {
    // 상태
    selectedAxes,
    targetPositions,
    targetSpeeds,

    // 액션
    updateSelectedAxis,
    updateTargetPosition,
    updateTargetSpeed,
    clearAllAxes,
    resetAllPositions,
    resetAllSpeeds,
    resetAll,
    isAnyAxisSelected,
  }
})
