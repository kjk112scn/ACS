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

// Loop 관련 타입
export type AxisKey = 'azimuth' | 'elevation' | 'train'
export type LoopDirection = 'toMin' | 'toMax'

export interface LoopAxisState {
  currentTarget: number | null
  stableCount: number
  direction: LoopDirection
  lastDirectionChangeTime: number // 방향 전환 시간 (timestamp)
}

export interface LoopState {
  azimuth: LoopAxisState
  elevation: LoopAxisState
  train: LoopAxisState
}

// Loop 축 상태 초기값 생성 함수
const createInitialLoopAxisState = (): LoopAxisState => ({
  currentTarget: null,
  stableCount: 0,
  direction: 'toMax',
  lastDirectionChangeTime: 0,
})

export const useSlewModeStore = defineStore('slew', () => {
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

  // Loop 관련 상태
  const loopEnabled = ref(false)
  const loopRunning = ref(false)
  const loopIntervalId = ref<ReturnType<typeof setInterval> | null>(null)
  const loopState = ref<LoopState>({
    azimuth: createInitialLoopAxisState(),
    elevation: createInitialLoopAxisState(),
    train: createInitialLoopAxisState(),
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

  // Loop 활성화 토글
  const toggleLoopEnabled = (value: boolean) => {
    loopEnabled.value = value
  }

  // Loop 상태 초기화
  const resetLoopState = () => {
    loopState.value = {
      azimuth: createInitialLoopAxisState(),
      elevation: createInitialLoopAxisState(),
      train: createInitialLoopAxisState(),
    }
  }

  // Loop 축 상태 업데이트
  const updateLoopAxisState = (axis: AxisKey, updates: Partial<LoopAxisState>) => {
    loopState.value[axis] = {
      ...loopState.value[axis],
      ...updates,
    }
  }

  // Loop interval 설정
  const setLoopInterval = (intervalId: ReturnType<typeof setInterval> | null) => {
    loopIntervalId.value = intervalId
  }

  // Loop interval 정리
  const clearLoopInterval = () => {
    if (loopIntervalId.value) {
      clearInterval(loopIntervalId.value)
      loopIntervalId.value = null
    }
  }

  // Loop 실행 상태 설정
  const setLoopRunning = (value: boolean) => {
    loopRunning.value = value
  }

  return {
    // 상태
    selectedAxes,
    speeds,
    loopEnabled,
    loopRunning,
    loopState,
    loopIntervalId,

    // 액션
    updateSelectedAxis,
    updateSpeed,
    clearAllAxes,
    resetAllSpeeds,
    resetAll,
    isAnyAxisSelected,
    toggleLoopEnabled,
    resetLoopState,
    updateLoopAxisState,
    setLoopInterval,
    clearLoopInterval,
    setLoopRunning,
  }
})
