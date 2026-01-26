/**
 * useOffsetControls - Offset 제어 공용 Composable
 *
 * SunTrack, EphemerisDesignation, PassSchedule 3개 페이지에서 공통으로 사용
 * offset input/output 값이 모든 페이지에서 동기화됨
 *
 * ✅ FIX #R005-C4: PassSchedule 모드에서는 전용 API 호출
 *    - PassSchedule: passScheduleStore.sendTimeOffset() → handleTimeOffsetChange() 포함
 *    - 그 외: icdStore.sendTimeOffsetCommand() (기존)
 */
import { ref, computed, readonly } from 'vue'
import { useRoute } from 'vue-router'
import { useICDStore } from '../../../../stores/icd/icdStore'
import { usePassScheduleModeStore } from '../../../../stores/mode/passScheduleStore'

// 싱글톤 상태 (모든 페이지에서 공유)
const inputs = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])
const outputs = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])
const offsetCals = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])

export function useOffsetControls() {
  const icdStore = useICDStore()
  const passScheduleStore = usePassScheduleModeStore()
  const route = useRoute()

  // ✅ FIX #R005-C4: PassSchedule 모드 여부 확인
  const isPassScheduleMode = computed(() => {
    return route.path.includes('pass-schedule')
  })

  // ✅ FIX #R005-C4: 모드별 Time Offset 명령 전송
  const sendTimeOffsetByMode = async (timeOffset: number) => {
    if (isPassScheduleMode.value) {
      // PassSchedule 모드: 전용 API (handleTimeOffsetChange 포함)
      return await passScheduleStore.sendTimeOffset(timeOffset)
    } else {
      // 그 외 모드: 기존 ICD API
      return await icdStore.sendTimeOffsetCommand(timeOffset)
    }
  }

  // Cal Time (computed from icdStore)
  const formattedCalTime = computed(() => {
    const calTime = icdStore.resultTimeOffsetCalTime
    if (!calTime) return ''
    try {
      const date = new Date(calTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      const milliseconds = String(date.getMilliseconds()).padStart(3, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${milliseconds}`
    } catch (error) {
      console.error('Error formatting timestamp:', error)
      return calTime
    }
  })

  // 입력값 변경 핸들러
  const onInputChange = (index: number, value: string) => {
    inputs.value[index] = value
  }

  // 증가 함수
  const increment = async (index: number) => {
    const inputValue = inputs.value[index] || '0'
    const outputValue = outputs.value[index] || '0'
    const currentInputValue = parseFloat(inputValue)
    const currentOutputValue = parseFloat(outputValue)

    if (!isNaN(currentInputValue)) {
      offsetCals.value[index] = (currentInputValue + currentOutputValue).toFixed(2)
    } else {
      inputs.value[index] = '0.01'
    }

    // Azimuth, Elevation, Train (index 0, 1, 2)
    if (index <= 2) {
      try {
        const azOffset = parseFloat(offsetCals.value[0] || '0')
        const elOffset = parseFloat(offsetCals.value[1] || '0')
        const trainOffset = parseFloat(offsetCals.value[2] || '0')

        const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, trainOffset)

        if (response && typeof response === 'object') {
          outputs.value[0] = String(response.azimuthOffset?.toFixed(2) || offsetCals.value[0] || '0.00')
          outputs.value[1] = String(response.elevationOffset?.toFixed(2) || offsetCals.value[1] || '0.00')
          outputs.value[2] = String(response.trainOffset?.toFixed(2) || offsetCals.value[2] || '0.00')
        } else {
          outputs.value[0] = offsetCals.value[0] || '0.00'
          outputs.value[1] = offsetCals.value[1] || '0.00'
          outputs.value[2] = offsetCals.value[2] || '0.00'
        }
      } catch (error) {
        console.error('오프셋 명령 처리 중 오류:', error)
      }
    }
    // Time (index 3)
    else if (index === 3) {
      try {
        const inputVal = inputs.value[index] || '0'
        const outputVal = outputs.value[index] || '0'
        const currentInput = parseFloat(inputVal)
        const currentOutput = parseFloat(outputVal)

        if (!isNaN(currentInput)) {
          offsetCals.value[index] = (currentOutput + currentInput).toFixed(2)
        }

        const timeOffset = parseFloat(offsetCals.value[3] || '0')
        // ✅ FIX #R005-C4: 모드별 API 호출
        const response = await sendTimeOffsetByMode(timeOffset)

        if (response && typeof response === 'object' && typeof response.inputTimeoffset === 'number') {
          outputs.value[3] = String(response.inputTimeoffset.toFixed(2) || offsetCals.value[3] || '0.00')
        } else {
          outputs.value[3] = offsetCals.value[3] || '0.00'
        }
      } catch (error) {
        console.error('시간 오프셋 명령 처리 중 오류:', error)
      }
    }
  }

  // 감소 함수
  const decrement = async (index: number) => {
    const inputValue = inputs.value[index] || '0'
    const outputValue = outputs.value[index] || '0'
    const currentInputValue = parseFloat(inputValue)
    const currentOutputValue = parseFloat(outputValue)

    if (!isNaN(currentInputValue)) {
      offsetCals.value[index] = (currentOutputValue - currentInputValue).toFixed(2)
    } else {
      inputs.value[index] = '0.01'
    }

    // Azimuth, Elevation, Train (index 0, 1, 2)
    if (index <= 2) {
      try {
        const azOffset = parseFloat(offsetCals.value[0] || '0')
        const elOffset = parseFloat(offsetCals.value[1] || '0')
        const trainOffset = parseFloat(offsetCals.value[2] || '0')

        const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, trainOffset)

        if (response && typeof response === 'object') {
          outputs.value[0] = String(response.azimuthOffset?.toFixed(2) || offsetCals.value[0] || '0.00')
          outputs.value[1] = String(response.elevationOffset?.toFixed(2) || offsetCals.value[1] || '0.00')
          outputs.value[2] = String(response.trainOffset?.toFixed(2) || offsetCals.value[2] || '0.00')
        } else {
          outputs.value[0] = offsetCals.value[0] || '0.00'
          outputs.value[1] = offsetCals.value[1] || '0.00'
          outputs.value[2] = offsetCals.value[2] || '0.00'
        }
      } catch (error) {
        console.error('오프셋 명령 처리 중 오류:', error)
      }
    }
    // Time (index 3)
    else if (index === 3) {
      try {
        const inputVal = inputs.value[index] || '0'
        const outputVal = outputs.value[index] || '0'
        const currentInput = parseFloat(inputVal)
        const currentOutput = parseFloat(outputVal)

        if (!isNaN(currentInput)) {
          offsetCals.value[index] = (currentOutput - currentInput).toFixed(2)
        }

        const timeOffset = parseFloat(offsetCals.value[3] || '0')
        // ✅ FIX #R005-C4: 모드별 API 호출
        const response = await sendTimeOffsetByMode(timeOffset)

        if (response && typeof response === 'object' && typeof response.inputTimeoffset === 'number') {
          outputs.value[3] = String(response.inputTimeoffset.toFixed(2) || offsetCals.value[3] || '0.00')
        } else {
          outputs.value[3] = offsetCals.value[3] || '0.00'
        }
      } catch (error) {
        console.error('시간 오프셋 명령 처리 중 오류:', error)
      }
    }
  }

  // 리셋 함수
  const reset = async (index: number) => {
    try {
      const azOffset = offsetCals.value[0] ? parseFloat(offsetCals.value[0]) : 0
      const elOffset = offsetCals.value[1] ? parseFloat(offsetCals.value[1]) : 0
      const tiOffset = offsetCals.value[2] ? parseFloat(offsetCals.value[2]) : 0

      if (index === 0) {
        const response = await icdStore.sendPositionOffsetCommand(0, elOffset, tiOffset)
        outputs.value[0] = response.azimuthOffset?.toFixed(2) || '0.00'
        offsetCals.value[0] = outputs.value[0]
      } else if (index === 1) {
        const response = await icdStore.sendPositionOffsetCommand(azOffset, 0, tiOffset)
        outputs.value[1] = response.elevationOffset?.toFixed(2) || '0.00'
        offsetCals.value[1] = outputs.value[1]
      } else if (index === 2) {
        const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, 0)
        outputs.value[2] = response.trainOffset?.toFixed(2) || '0.00'
        offsetCals.value[2] = outputs.value[2]
      } else if (index === 3) {
        // ✅ FIX #R005-C4: 모드별 API 호출
        const response = await sendTimeOffsetByMode(0)
        if (response && response.resultTimeOffset !== undefined) {
          outputs.value[3] = response.resultTimeOffset.toFixed(2)
        } else {
          outputs.value[3] = '0.00'
        }
        offsetCals.value[3] = '0.00'
      }
    } catch (error) {
      console.error('오프셋 초기화 중 오류:', error)
    }
  }

  // 전체 리셋 함수
  const resetAll = () => {
    inputs.value = ['0.00', '0.00', '0.00', '0.00']
    outputs.value = ['0.00', '0.00', '0.00', '0.00']
    offsetCals.value = ['0.00', '0.00', '0.00', '0.00']
  }

  return {
    // 상태 (readonly로 노출)
    inputs: readonly(inputs),
    outputs: readonly(outputs),
    offsetCals: readonly(offsetCals),
    formattedCalTime,

    // 메서드
    onInputChange,
    increment,
    decrement,
    reset,
    resetAll,
  }
}
