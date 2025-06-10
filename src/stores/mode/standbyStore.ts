import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useStandbyModeStore = defineStore('standbyMode', () => {
  // 선택된 축 상태
  const selectedAxes = ref({
    azimuth: false,
    elevation: false,
    tilt: false,
  })

  // 축 상태 업데이트
  const updateAxis = (axis: 'azimuth' | 'elevation' | 'tilt', value: boolean) => {
    selectedAxes.value[axis] = value
  }

  // 모든 축 선택/해제
  const setAllAxes = (value: boolean) => {
    selectedAxes.value.azimuth = value
    selectedAxes.value.elevation = value
    selectedAxes.value.tilt = value
  }

  // 선택된 축 초기화
  const resetAxes = () => {
    selectedAxes.value.azimuth = false
    selectedAxes.value.elevation = false
    selectedAxes.value.tilt = false
  }

  return {
    // 상태
    selectedAxes,

    // 메서드
    updateAxis,
    setAllAxes,
    resetAxes,
  }
})
