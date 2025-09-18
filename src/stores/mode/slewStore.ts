import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface SlewAxes {
  azimuth: boolean
  elevation: boolean
  train: boolean
}

export interface SlewSpeeds {
  azimuth: string
  elevation: string
  train: string
}

export const useSlewStore = defineStore('slew', () => {
  // 선택된 축 상태
  const selectedAxes = ref<SlewAxes>({
    azimuth: false,
    elevation: false,
    train: false,
  })

  // 속도 값
  const speeds = ref<SlewSpeeds>({
    azimuth: '0.00',
    elevation: '0.00',
    train: '0.00',
  })

  // 축 선택 상태 업데이트
  const updateSelectedAxis = (axis: keyof SlewAxes, value: boolean) => {
    selectedAxes.value[axis] = value
  }

  // 속도 업데이트
  const updateSpeed = (axis: keyof SlewSpeeds, value: string) => {
    speeds.value[axis] = value
  }

  // 모든 축 선택 해제
  const clearAllAxes = () => {
    selectedAxes.value = {
      azimuth: false,
      elevation: false,
      train: false,
    }
  }

  // 모든 속도 초기화
  const resetAllSpeeds = () => {
    speeds.value = {
      azimuth: '0.00',
      elevation: '0.00',
      train: '0.00',
    }
  }

  // 전체 상태 초기화
  const resetAll = () => {
    clearAllAxes()
    resetAllSpeeds()
  }

  // 최소 하나의 축이 선택되었는지 확인
  const isAnyAxisSelected = () => {
    return selectedAxes.value.azimuth || selectedAxes.value.elevation || selectedAxes.value.train
  }

  return {
    // 상태
    selectedAxes,
    speeds,

    // 액션
    updateSelectedAxis,
    updateSpeed,
    clearAllAxes,
    resetAllSpeeds,
    resetAll,
    isAnyAxisSelected,
  }
})
