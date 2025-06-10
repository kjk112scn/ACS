import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface StepAxes {
  azimuth: boolean
  elevation: boolean
  tilt: boolean
}

export interface StepAngles {
  azimuth: string
  elevation: string
  tilt: string
}

export interface StepSpeeds {
  azimuth: string
  elevation: string
  tilt: string
}

export const useStepStore = defineStore('step', () => {
  // 선택된 축 상태
  const selectedAxes = ref<StepAxes>({
    azimuth: false,
    elevation: false,
    tilt: false,
  })

  // 각도 상태
  const angles = ref<StepAngles>({
    azimuth: '0.00',
    elevation: '0.00',
    tilt: '0.00',
  })

  // 속도 상태
  const speeds = ref<StepSpeeds>({
    azimuth: '0.00',
    elevation: '0.00',
    tilt: '0.00',
  })

  // 축 선택 상태 업데이트
  const updateSelectedAxis = (axis: keyof StepAxes, value: boolean) => {
    selectedAxes.value[axis] = value
  }

  // 각도 값 업데이트
  const updateAngle = (axis: keyof StepAngles, value: string) => {
    angles.value[axis] = value
  }

  // 속도 값 업데이트
  const updateSpeed = (axis: keyof StepSpeeds, value: string) => {
    speeds.value[axis] = value
  }

  // 모든 축 선택 해제
  const clearAllAxes = () => {
    selectedAxes.value = {
      azimuth: false,
      elevation: false,
      tilt: false,
    }
  }

  // 모든 각도 초기화
  const resetAllAngles = () => {
    angles.value = {
      azimuth: '0.00',
      elevation: '0.00',
      tilt: '0.00',
    }
  }

  // 모든 속도 초기화
  const resetAllSpeeds = () => {
    speeds.value = {
      azimuth: '0.00',
      elevation: '0.00',
      tilt: '0.00',
    }
  }

  // 전체 상태 초기화
  const resetAll = () => {
    clearAllAxes()
    resetAllAngles()
    resetAllSpeeds()
  }

  // 최소 하나의 축이 선택되었는지 확인
  const isAnyAxisSelected = () => {
    return selectedAxes.value.azimuth || selectedAxes.value.elevation || selectedAxes.value.tilt
  }

  return {
    // 상태
    selectedAxes,
    angles,
    speeds,

    // 액션
    updateSelectedAxis,
    updateAngle,
    updateSpeed,
    clearAllAxes,
    resetAllAngles,
    resetAllSpeeds,
    resetAll,
    isAnyAxisSelected,
  }
})
