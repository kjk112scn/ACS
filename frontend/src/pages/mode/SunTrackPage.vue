<template>
  <div class="sun-track-mode">

    <!-- 1행: Offset Controls - EphemerisDesignationPage와 동일한 구조 -->
    <div class="row q-col-gutter-md offset-control-row">
      <div class="col-12">
        <q-card flat bordered class="control-card">
          <q-card-section class="compact-control purple-1">
            <!-- 모든 간격이 동적으로 조정되는 반응형 레이아웃 -->
            <div class="flexible-offset-layout">
              <!-- Azimuth Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Azimuth<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[0]" @input="(val: string) => onInputChange(0, val)" dense outlined
                      type="number" step="0.01" label="Azimuth" class="offset-input" />
                  </div>
                  <div class="col-auto">
                    <div class="vertical-button-group">
                      <div class="vertical-buttons">
                        <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(0)" />
                        <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(0)" />
                      </div>
                      <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(0)" />
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="outputs[0]" dense outlined readonly label="Output"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Elevation Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Elevation<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[1]" @input="(val: string) => onInputChange(1, val)" dense outlined
                      type="number" step="0.01" label="Elevation"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto">
                    <div class="vertical-button-group">
                      <div class="vertical-buttons">
                        <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(1)" />
                        <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(1)" />
                      </div>
                      <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(1)" />
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="outputs[1]" dense outlined readonly label="Output"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Tilt Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Tilt<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[2]" @input="(val: string) => onInputChange(2, val)" dense outlined
                      type="number" step="0.01" label="Tilt"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto">
                    <div class="vertical-button-group">
                      <div class="vertical-buttons">
                        <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(2)" />
                        <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(2)" />
                      </div>
                      <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(2)" />
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="outputs[2]" dense outlined readonly label="Output"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Time Offset + Cal Time -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Time<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[3]" @input="(val: string) => onInputChange(3, val)" dense outlined
                      type="number" step="0.01" label="Time"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto">
                    <div class="vertical-button-group">
                      <div class="vertical-buttons">
                        <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(3)" />
                        <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(3)" />
                      </div>
                      <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(3)" />
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="outputs[3]" dense outlined readonly label="Result"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto cal-time-field">
                    <q-input v-model="formattedCalTime" dense outlined readonly label="Cal Time"
                      style="min-width: 190px !important; max-width: 220px !important;" />
                  </div>
                </div>
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- 2행: Main Content - EphemerisDesignationPage와 동일한 구조 -->
    <div class="row q-col-gutter-md main-content-row"
      style="display: flex; flex-wrap: nowrap; align-items: stretch; margin-bottom: 0 !important; padding-bottom: 0 !important;">
      <div class="col-12">
        <q-card class="control-section speed-section-card"
          style="min-height: 367.19px !important; max-height: 367.19px !important; height: 367.19px !important; display: flex !important; flex-direction: column !important;">
          <q-card-section class="speed-section-content"
            style="min-height: 367.19px !important; max-height: 367.19px !important; height: 367.19px !important; flex: 1 !important; display: flex !important; flex-direction: column !important; padding-top: 16px !important; padding-bottom: 0px !important;">
            <div class="text-subtitle1 text-weight-bold text-primary speed-section-title">Speed Settings</div>
            <div class="speed-inputs-row"
              style="min-height: 340px !important; height: 100% !important; flex: 1 !important; padding-top: 0 !important; padding-bottom: 0 !important; margin-bottom: 0 !important;">
              <div class="speed-content-wrapper">
                <div class="row q-col-gutter-xl">
                  <!-- Azimuth Speed -->
                  <div class="col-12 col-md-4">
                    <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Azimuth Speed</div>
                    <q-input v-model="speedInputs.azimuth" dense outlined type="number" step="0.1" class="speed-input"
                      style="min-width: 200px !important; width: 100% !important;" />
                  </div>

                  <!-- Elevation Speed -->
                  <div class="col-12 col-md-4">
                    <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Elevation Speed</div>
                    <q-input v-model="speedInputs.elevation" dense outlined type="number" step="0.1" class="speed-input"
                      style="min-width: 200px !important; width: 100% !important;" />
                  </div>

                  <!-- Tilt Speed -->
                  <div class="col-12 col-md-4">
                    <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Tilt Speed</div>
                    <q-input v-model="speedInputs.train" dense outlined type="number" step="0.1" class="speed-input"
                      style="min-width: 200px !important; width: 100% !important;" />
                  </div>
                </div>

                <!-- 설명 텍스트 추가 -->
                <div class="speed-description-text text-caption q-mt-sm" style="color: var(--theme-text-secondary);">
                  속도 값은 Go 버튼을 클릭할 때 적용됩니다.
                </div>

                <!-- 제어 버튼 섹션 - Speed 입력 창 바로 아래에 배치 -->
                <div class="control-buttons-section q-mt-md mode-button-bar">
                  <div class="row q-col-gutter-md justify-center">
                    <div class="col-auto">
                      <q-btn label="Go" color="positive" icon="play_arrow" size="lg" @click="handleGoCommand"
                        :loading="isGoLoading" />
                    </div>
                    <div class="col-auto">
                      <q-btn label="Stop" color="negative" icon="stop" size="lg" @click="handleStopCommand"
                        :loading="isStopLoading" />
                    </div>
                    <div class="col-auto">
                      <q-btn label="Stow" color="warning" icon="home" size="lg" @click="handleStowCommand"
                        :loading="isStowLoading" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()

// 입력 및 출력 필드 (배열로 관리)
const inputs = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])
const outputs = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])
const offsetCals = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])
// 버튼 로딩 상태
const isGoLoading = ref(false)
const isStopLoading = ref(false)
const isStowLoading = ref(false)

// 입력값이 변경될 때마다 소수점 둘째 자리까지 포맷팅
watch(
  inputs,
  (newValues) => {
    for (let i = 0; i < newValues.length; i++) {
      const value = newValues[i] || '0'
      const num = parseFloat(value)
      if (!isNaN(num)) {
        inputs.value[i] = num.toFixed(2)
      }
    }
  },
  { deep: true },
)

// ✅ 입력값 변경 핸들러 (EphemerisDesignationPage와 동일)
const onInputChange = (index: number, value: string) => {
  inputs.value[index] = value
  // 실제 API 호출은 increment/decrement에서 처리
}
// Speed 입력 필드
const speedInputs = ref({
  azimuth: '5.0',
  elevation: '5.0',
  train: '5.0',
})

// Go 명령 처리 함수 (Sun Track 시작)
const handleGoCommand = async () => {
  try {
    isGoLoading.value = true

    // Speed 값 가져오기 (문자열을 숫자로 변환)
    const azimuthSpeed = parseFloat(speedInputs.value.azimuth) || 0.0
    const elevationSpeed = parseFloat(speedInputs.value.elevation) || 0.0
    const trainSpeed = parseFloat(speedInputs.value.train) || 0.0

    // 기본 interval 값 (밀리초 단위, 필요에 따라 조정)
    const interval = 1000 // 1초

    // Sun Track 시작 API 호출
    await icdStore.startSunTrack(interval, azimuthSpeed, elevationSpeed, trainSpeed)

    // Notify 대신 console.log 사용 (임시)
    console.log('Sun Track이 시작되었습니다. 설정된 속도:', azimuthSpeed, elevationSpeed, trainSpeed)

    // Notify가 작동하지 않는 경우 alert 사용 (임시)
    /* alert(
      `Sun Track이 시작되었습니다. 설정된 속도: Az=${azimuthSpeed}, El=${elevationSpeed}, Train=${trainSpeed}`,
    ) */
  } catch (error) {
    console.error('Sun Track 시작 중 오류:', error)
    //alert('Sun Track 시작 중 오류가 발생했습니다.')
  } finally {
    isGoLoading.value = false
  }
}

// Stop 명령 처리 함수 (Sun Track 중지)
const handleStopCommand = async () => {
  try {
    isStopLoading.value = true

    // Sun Track 중지 API 호출
    await icdStore.stopCommand(true, true, true)
    // Notify 대신 console.log 사용 (임시)
    console.log('Sun Track이 중지되었습니다.')

    // Notify가 작동하지 않는 경우 alert 사용 (임시)
    //alert('Sun Track이 중지되었습니다.')
  } catch (error) {
    console.error('Sun Track 중지 중 오류:', error)
    //alert('Sun Track 중지 중 오류가 발생했습니다.')
  } finally {
    isStopLoading.value = false
  }
}

// Stow 명령 처리 함수
const handleStowCommand = async () => {
  try {
    isStowLoading.value = true

    // 비상 정지 API 호출
    await icdStore.stowCommand()

    // Notify 대신 console.log 사용 (임시)
    console.log('Stow 명령이 성공적으로 전송되었습니다.')

    // Notify가 작동하지 않는 경우 alert 사용 (임시)
    //alert('Stow 명령이 성공적으로 전송되었습니다.')
  } catch (error) {
    console.error('Stow 명령 처리 중 오류:', error)
    //alert('Stow 명령 전송 중 오류가 발생했습니다.')
  } finally {
    isStowLoading.value = false
  }
}

// 증가 함수 (0.01 단위로 증가)
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

  // Azimuth, Elevation, Train 값이 변경된 경우에만 API 호출
  if (index <= 2) {
    try {
      // 입력값을 숫자로 변환 (타입 에러 수정)
      const azOffset = parseFloat(offsetCals.value[0] || '0')
      const elOffset = parseFloat(offsetCals.value[1] || '0')
      const trainOffset = parseFloat(offsetCals.value[2] || '0')

      // ICD 스토어의 함수 호출하고 응답 받기
      const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, trainOffset)

      // 응답에서 출력값 업데이트 (백엔드 응답 구조에 맞게 수정 필요)
      if (response && typeof response === 'object') {
        // 백엔드 응답에 해당 값이 있다면 그 값을 사용
        // 예시: response.azimuthResult, response.elevationResult, response.trainResult
        // 실제 백엔드 응답 구조에 맞게 수정 필요
        outputs.value[0] = String(
          response.azimuthOffset?.toFixed(2) || offsetCals.value[0] || '0.00',
        )
        outputs.value[1] = String(
          response.elevationOffset?.toFixed(2) || offsetCals.value[1] || '0.00',
        )
        outputs.value[2] = String(response.trainOffset?.toFixed(2) || offsetCals.value[2] || '0.00')
      } else {
        // 응답이 없거나 예상 형식이 아닌 경우 입력값을 그대로 출력값으로 사용
        outputs.value[0] = offsetCals.value[0] || '0.00'
        outputs.value[1] = offsetCals.value[1] || '0.00'
        outputs.value[2] = offsetCals.value[2] || '0.00'
      }
    } catch (error) {
      console.error('오프셋 명령 처리 중 오류:', error)
    }
  }
  // Time 값이 변경된 경우 Time Offset API 호출
  else if (index === 3) {
    try {
      const inputValue = inputs.value[index] || '0'
      const outputValue = outputs.value[index] || '0'
      const currentInputValue = parseFloat(inputValue)
      const currentOutputValue = parseFloat(outputValue)
      if (!isNaN(currentInputValue)) {
        offsetCals.value[index] = (currentOutputValue + currentInputValue).toFixed(2)
      }
      // 입력값을 숫자로 변환
      const timeOffset = parseFloat(offsetCals.value[3] || '0')
      console.debug('오프셋 명령 값', timeOffset)
      // ICD 스토어의 시간 오프셋 함수 호출하고 응답 받기
      const response = await icdStore.sendTimeOffsetCommand(timeOffset)

      // 응답에서 출력값 업데이트
      if (
        response &&
        typeof response === 'object' &&
        typeof response.inputTimeoffset === 'number'
      ) {
        outputs.value[3] = String(
          response.inputTimeoffset.toFixed(2) || offsetCals.value[3] || '0.00',
        )
      } else {
        // 응답이 없거나 예상 형식이 아닌 경우 입력값을 그대로 출력값으로 사용
        outputs.value[3] = offsetCals.value[3] || '0.00'
      }
    } catch (error) {
      console.error('시간 오프셋 명령 처리 중 오류:', error)
    }
  }
}

// 감소소
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

  // Azimuth, Elevation, Train 값이 변경된 경우에만 API 호출
  if (index <= 2) {
    try {
      // 입력값을 숫자로 변환 (타입 에러 수정)
      const azOffset = parseFloat(offsetCals.value[0] || '0')
      const elOffset = parseFloat(offsetCals.value[1] || '0')
      const trainOffset = parseFloat(offsetCals.value[2] || '0')

      // ICD 스토어의 함수 호출하고 응답 받기
      const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, trainOffset)

      // 응답에서 출력값 업데이트 (백엔드 응답 구조에 맞게 수정 필요)
      if (response && typeof response === 'object') {
        // 백엔드 응답에 해당 값이 있다면 그 값을 사용
        // 예시: response.azimuthResult, response.elevationResult, response.trainResult
        // 실제 백엔드 응답 구조에 맞게 수정 필요
        outputs.value[0] = String(
          response.azimuthOffset?.toFixed(2) || offsetCals.value[0] || '0.00',
        )
        outputs.value[1] = String(
          response.elevationOffset?.toFixed(2) || offsetCals.value[1] || '0.00',
        )
        outputs.value[2] = String(response.trainOffset?.toFixed(2) || offsetCals.value[2] || '0.00')
      } else {
        // 응답이 없거나 예상 형식이 아닌 경우 입력값을 그대로 출력값으로 사용
        outputs.value[0] = offsetCals.value[0] || '0.00'
        outputs.value[1] = offsetCals.value[1] || '0.00'
        outputs.value[2] = offsetCals.value[2] || '0.00'
      }
    } catch (error) {
      console.error('오프셋 명령 처리 중 오류:', error)
    }
  }
  // Time 값이 변경된 경우 Time Offset API 호출
  else if (index === 3) {
    try {
      const inputValue = inputs.value[index] || '0'
      const outputValue = outputs.value[index] || '0'
      const currentInputValue = parseFloat(inputValue)
      const currentOutputValue = parseFloat(outputValue)
      if (!isNaN(currentInputValue)) {
        offsetCals.value[index] = (currentOutputValue - currentInputValue).toFixed(2)
      }
      // 입력값을 숫자로 변환
      const timeOffset = parseFloat(offsetCals.value[3] || '0')
      console.debug('오프셋 명령 값', timeOffset)
      // ICD 스토어의 시간 오프셋 함수 호출하고 응답 받기
      const response = await icdStore.sendTimeOffsetCommand(timeOffset)

      // 응답에서 출력값 업데이트
      if (
        response &&
        typeof response === 'object' &&
        typeof response.inputTimeoffset === 'number'
      ) {
        outputs.value[3] = String(
          response.inputTimeoffset.toFixed(2) || offsetCals.value[3] || '0.00',
        )
      } else {
        // 응답이 없거나 예상 형식이 아닌 경우 입력값을 그대로 출력값으로 사용
        outputs.value[3] = offsetCals.value[3] || '0.00'
      }
    } catch (error) {
      console.error('시간 오프셋 명령 처리 중 오류:', error)
    }
  }
}

const reset = async (index: number) => {
  //inputs.value[index] = '0'

  try {
    // offsetCals가 정의되어 있는지 확인하고, 각 값에 대해 안전하게 처리
    const azOffset = offsetCals.value && offsetCals.value[0] ? parseFloat(offsetCals.value[0]) : 0
    const elOffset = offsetCals.value && offsetCals.value[1] ? parseFloat(offsetCals.value[1]) : 0
    const tiOffset = offsetCals.value && offsetCals.value[2] ? parseFloat(offsetCals.value[2]) : 0
    //const timeOffset = offsetCals.value && offsetCals.value[3] ? parseFloat(offsetCals.value[3]) : 0

    if (index === 0) {
      // 방위각 오프셋 초기화 - 다른 축은 현재 값 유지
      const response = await icdStore.sendPositionOffsetCommand(0, elOffset, tiOffset)
      // azOffset 대신 azimuthResult 사용
      outputs.value[0] = response.azimuthOffset?.toFixed(2) || '0.00'
      offsetCals.value[0] = outputs.value[0]
    } else if (index === 1) {
      // 고도각 오프셋 초기화 - 다른 축은 현재 값 유지
      const response = await icdStore.sendPositionOffsetCommand(azOffset, 0, tiOffset)
      outputs.value[1] = response.elevationOffset?.toFixed(2) || '0.00'
      offsetCals.value[1] = outputs.value[1]
    } else if (index === 2) {
      // 틸트각 오프셋 초기화 - 다른 축은 현재 값 유지
      const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, 0)
      outputs.value[2] = response.trainOffset?.toFixed(2) || '0.00'
      offsetCals.value[2] = outputs.value[2]
    } else if (index === 3) {
      const response = await icdStore.sendTimeOffsetCommand(0)
      // 응답 객체와 속성이 존재하는지 확인 후 안전하게 처리
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

const formattedCalTime = computed(() => {
  const calTime = icdStore.resultTimeOffsetCalTime
  if (!calTime) return ''
  try {
    // Parse the timestamp
    const date = new Date(calTime)

    // Format as yyyy-mm-dd hh:mm:ss.SSS using local time methods
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    const milliseconds = String(date.getMilliseconds()).padStart(3, '0')

    // 날짜와 시간을 한 줄로 구분
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${milliseconds}`
  } catch (error) {
    console.error('Error formatting timestamp:', error)
    return calTime // Return original value if formatting fails
  }
})
</script>

<style>
/* ✅ 숫자 입력 필드의 화살표 버튼 숨기기 */
input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type='number'] {
  -moz-appearance: textfield;
  appearance: textfield;
}

/* ✅ SunTrackPage 내부 스타일만 적용 - .sun-track-mode로 스코프 제한 */
.sun-track-mode .q-card__section {
  padding: 16px;
}

/* ✅ SunTrackPage 내부의 q-card만 스타일 적용 - EphemerisDesignationPage.vue와 동일하게 Quasar 기본 그림자 사용 */
.sun-track-mode .q-card {
  background: var(--theme-card-background);
  /* ✅ box-shadow 제거 - Quasar 기본 q-card 그림자 사용 (EphemerisDesignationPage.vue와 동일) */
}

.sun-track-mode .q-btn {
  flex: 1;
}

/* ✅ 방법 1: 왼쪽 세로 라벨 (카드 안) - 높이 최적화 - EphemerisDesignationPage와 동일 */
.position-offset-label {
  background: linear-gradient(135deg, rgba(25, 118, 210, 0.15) 0%, rgba(25, 118, 210, 0.08) 100%);
  padding: 4px 8px;
  /* 높이 줄임: 8px 12px → 4px 8px */
  border-radius: 6px;
  border-right: 3px solid var(--q-primary);
  min-width: 50px;
  /* 너비도 약간 줄임: 60px → 50px */
  margin-right: 6px;
  /* 간격도 줄임: 8px → 6px */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  /* 텍스트 크기 줄임 */
  line-height: 1.2 !important;
  /* 줄 간격 줄임 */
}
</style>

<style scoped>
/* ✅ 1단계: sun-track-mode와 부모 요소의 하단 여백 완전 제거 (EphemerisDesignationPage와 동일) */
/* router-view, q-page-container 내부의 sun-track-mode 하단 여백 제거 */
router-view .sun-track-mode,
q-page-container .sun-track-mode,
q-page .sun-track-mode,
.sun-track-mode,
[class*="sun-track-mode"],
div.sun-track-mode {
  height: calc(var(--theme-layout-modePageMinHeight, 500px) - 34px - 16px) !important;
  /* ✅ 테마 변수를 사용하여 높이 설정: (500px - 32px 패딩 - 2px border - 16px 여백) = 450px */
  /* ✅ DashboardPage의 .mode-content-section .q-card__section 패딩(16px 상하 = 32px)과 border(1px 상하 = 2px)를 고려 */
  /* ✅ q-mb-sm (8px) + q-col-gutter-md 마진/패딩 (약 8px) = 16px 추가 차감 */
  min-height: calc(var(--theme-layout-modePageMinHeight, 500px) - 34px - 16px) !important;
  /* ✅ 최소 높이도 테마 변수 사용 */
  max-height: calc(var(--theme-layout-modePageMinHeight, 500px) - 34px - 16px) !important;
  /* ✅ 최대 높이도 테마 변수 사용 */
  width: 100%;
  padding: 0 !important;
  margin: 0 !important;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
  overflow: hidden;
  /* ✅ 내용이 넘치면 숨김 */
  display: flex !important;
  /* ✅ flexbox로 변경 */
  flex-direction: column !important;
  /* ✅ 세로 방향 */
  justify-content: center;
  /* ✅ 컨텐츠를 가운데 정렬 */
  align-items: center;
  /* ✅ 컨텐츠를 가운데 정렬 */
  gap: 0 !important;
  /* ✅ flex gap 제거 */
  row-gap: 0 !important;
  /* ✅ flex row-gap 제거 */
  column-gap: 0 !important;
  /* ✅ flex column-gap 제거 */
  box-sizing: border-box;
}

/* router-view, q-page-container의 하단 패딩/마진 제거 */
router-view .sun-track-mode,
q-page-container .sun-track-mode {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ 오프셋 컨트롤 행 하단 여백 - 높이 계산에 포함 */
.sun-track-mode .offset-control-row {
  margin-bottom: 0.5rem !important;
  /* ✅ q-mb-sm (0.5rem = 8px) 유지 - 높이 계산에 포함됨 */
}

/* ✅ ephemeris-mode 내부의 모든 직접 자식 요소 하단 여백 제거 - EphemerisDesignationPage와 동일한 순서 */
.sun-track-mode>* {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ ephemeris-mode의 마지막 div 요소 하단 여백 완전 제거 (더 강력한 선택자) - EphemerisDesignationPage와 동일 */
.sun-track-mode>div:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ ephemeris-mode의 마지막 row 요소 하단 여백 완전 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode>.row:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ ephemeris-mode의 모든 직접 자식 row 요소 하단 여백 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode>.row {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ ephemeris-mode의 모든 직접 자식 div 요소 하단 여백 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode>div {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row가 ephemeris-mode의 마지막 자식일 때 하단 여백 완전 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode>.main-content-row:last-child,
.sun-track-mode>.row.main-content-row:last-child,
.sun-track-mode>div.main-content-row:last-child,
.sun-track-mode>.main-content-row,
.sun-track-mode>.row.main-content-row,
.sun-track-mode>div.main-content-row {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
  margin-top: 0 !important;
  padding-top: 0 !important;
}

/* ✅ 메인 콘텐츠 행 하단 여백을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.sun-track-mode .main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
}

/* ✅ Quasar q-col-gutter-md가 행에 추가하는 하단 마진을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.sun-track-mode .main-content-row.q-col-gutter-md,
.sun-track-mode .row.q-col-gutter-md.main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ Quasar row 기본 스타일 오버라이드 (더 강력한 선택자) - EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.sun-track-mode .main-content-row.row,
.sun-track-mode .row.main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 하단 여백 완전 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode .main-content-row>[class*="col-"] {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 하단 여백 완전 제거 (더 구체적인 선택자) - EphemerisDesignationPage와 동일 */
.sun-track-mode .main-content-row>[class*="col-"]:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card 하단 여백 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode .main-content-row>[class*="col-"] .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card 하단 여백 제거 (더 구체적인 선택자) - EphemerisDesignationPage와 동일 */
.sun-track-mode .main-content-row>[class*="col-"]:last-child .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card-section 하단 여백 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode .main-content-row>[class*="col-"] .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card-section 하단 여백 제거 (더 구체적인 선택자) - EphemerisDesignationPage와 동일 */
.sun-track-mode .main-content-row>[class*="col-"]:last-child .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 Quasar q-card 하단 마진/패딩 완전 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode .main-content-row .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 control-section 하단 여백 제거 - EphemerisDesignationPage와 동일 */
.sun-track-mode .main-content-row .control-section {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ 모든 간격이 동적으로 조정되는 반응형 레이아웃 */
.flexible-offset-layout {
  display: flex;
  align-items: stretch;
  justify-content: center;
  width: 100%;
  gap: 40px;
  row-gap: 8px;
  flex-wrap: wrap;
}

/* ✅ 개별 Offset 그룹 */
.offset-group {
  flex: none;
  min-width: 0;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.01);
  display: flex;
  align-items: center;
}

/* ✅ 간격 통일 - padding-left 제거하고 gap만으로 간격 관리 */

/* 라벨 스타일 - EphemerisDesignationPage와 동일 */
.position-offset-label {
  min-width: 80px;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(25, 118, 210, 0.1);
  border: 1px solid rgba(25, 118, 210, 0.3);
}

/* ✅ Cal Time 필드 스타일 */
.cal-time-field {
  flex-shrink: 0;
  min-width: 190px;
}

/* ✅ 세로 버튼 레이아웃 */
.vertical-button-group {
  display: flex !important;
  align-items: center !important;
  gap: 4px !important;
}

.vertical-buttons {
  display: flex !important;
  flex-direction: column !important;
  gap: 2px !important;
}

/* ✅ 반응형 동작 */
@media (max-width: 1900px) {
  .flexible-offset-layout {
    flex-wrap: wrap;
    gap: 20px;
    row-gap: 8px;
    justify-content: center;
  }

  .offset-group {
    flex: none;
    min-width: 0;
    padding: 8px;
  }

  .position-offset-label {
    min-width: 70px;
    font-size: 0.8rem;
  }

  .cal-time-field {
    min-width: 180px;
    max-width: 200px;
  }
}

@media (min-width: 1901px) {
  .flexible-offset-layout {
    flex-wrap: nowrap;
    gap: 40px;
    justify-content: center;
  }

  .offset-group {
    flex: none;
    min-width: 0;
  }

  .position-offset-label {
    min-width: 80px;
    font-size: 0.875rem;
  }
}

/* ✅ 카드 스타일 통일 */
/* ✅ 테두리는 mode-common.scss의 .control-section에서 통일 관리 */
.control-section {
  background-color: var(--theme-card-background);
  /* ✅ border, border-radius, box-shadow는 mode-common.scss에서 통일 관리 */
  width: 100%;
  height: 100%;
  max-height: 500px;
  display: flex;
  flex-direction: column;
  margin-bottom: 0 !important;
}

/* ✅ Speed Section 카드 높이 강제 고정 - control-section 기본 스타일 오버라이드 */
.sun-track-mode .control-section.speed-section-card {
  min-height: 367.19px !important;
  max-height: 367.19px !important;
  height: 367.19px !important;
  /* ✅ 높이를 367.19px로 강제 고정 */
  width: 100% !important;
  /* ✅ 너비를 100%로 설정하여 전체 너비 사용 */
  max-width: none !important;
  /* ✅ 최대 너비 제한 제거 */
  overflow: visible !important;
  /* ✅ overflow를 visible로 변경하여 확대된 내용이 보이도록 */
}

/* ✅ control-section .q-card-section 스타일 - EphemerisDesignationPage와 동일 */
.control-section .q-card-section {
  padding: 16px !important;
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 (상단 공간과 동일하게) */
  /* ✅ 남은 공간을 채우도록 flex 추가 - 내부 구성 변경 없음 */
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  /* ✅ 제목 absolute positioning을 위한 기준점 */
}

/* ✅ Speed Section 카드 섹션 높이 강제 고정 - 더 강력한 선택자로 오버라이드 */
.sun-track-mode .control-section.speed-section-card .q-card-section.speed-section-content,
.sun-track-mode .control-section.speed-section-card .q-card__section.speed-section-content,
.sun-track-mode .q-card.control-section.speed-section-card .q-card-section.speed-section-content,
.sun-track-mode .q-card.control-section.speed-section-card .q-card__section.speed-section-content,
.sun-track-mode .control-section.speed-section-card .q-card-section,
.sun-track-mode .control-section.speed-section-card .q-card__section {
  min-height: 367.19px !important;
  max-height: 367.19px !important;
  height: 367.19px !important;
  /* ✅ 높이를 367.19px로 강제 고정 */
  width: 100% !important;
  /* ✅ 너비를 100%로 설정 */
  max-width: none !important;
  /* ✅ 최대 너비 제한 제거 */
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: center;
  /* ✅ 컨텐츠를 세로 가운데 정렬 */
  align-items: center;
  /* ✅ 컨텐츠를 가로 가운데 정렬 */
  overflow: visible !important;
  /* ✅ overflow를 visible로 변경하여 확대된 내용이 보이도록 */
  padding-left: 2rem !important;
  padding-right: 2rem !important;
  /* ✅ 좌우 패딩을 늘려서 넓이 확대 */
}

/* ✅ control-card 스타일 제거 - EphemerisDesignationPage와 동일하게 Quasar 기본 스타일 사용 */

/* ✅ Speed Section 카드 높이 제한 - 367.19px로 고정 */
.sun-track-mode .control-section.speed-section-card,
.sun-track-mode .control-section.speed-section-card.q-card {
  min-height: 367.19px !important;
  max-height: 367.19px !important;
  height: 367.19px !important;
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Speed Section 카드 섹션 높이 조정 - 367.19px로 고정 */
.sun-track-mode .control-section.speed-section-card .q-card-section.speed-section-content {
  min-height: 367.19px !important;
  max-height: 367.19px !important;
  /* ✅ 최대 높이 제한 */
  height: 367.19px !important;
  /* ✅ 고정 높이로 설정하여 카드를 넘지 않도록 */
  flex: 1 !important;
  /* ✅ 남은 공간 채우기 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Speed Section 패딩 오버라이드 - EphemerisDesignationPage의 position-view-section과 동일 */
.speed-section-content {
  padding: 16px 16px 0px 16px !important;
  /* ✅ 상단 패딩을 다른 패널과 동일하게 16px로 맞춤, 하단 패딩 제거 */
  min-height: 367.19px !important;
  max-height: 367.19px !important;
  height: 367.19px !important;
  /* ✅ 높이를 367.19px로 고정 */
}

/* ✅ Speed Section 제목 - absolute positioning (EphemerisDesignationPage와 동일) */
.speed-section-title {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 10;
  margin: 0;
  padding: 0;
}

/* ✅ Speed 입력 영역 - 높이 제한하여 카드를 넘지 않도록 */
.speed-inputs-row {
  min-height: auto !important;
  /* ✅ 최소 높이를 auto로 변경하여 내용에 맞게 조정 */
  max-height: none !important;
  /* ✅ 최대 높이 제한 제거 */
  height: auto !important;
  /* ✅ 고정 높이 제거하여 내용에 맞게 조정 */
  flex: 0 0 auto !important;
  /* ✅ flex-shrink와 flex-grow를 0으로 설정하여 내용 크기만큼만 차지 */
  width: 100%;
  display: flex;
  align-items: center;
  /* ✅ 컨텐츠를 가운데 정렬 */
  justify-content: center;
  margin: 0 auto;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  padding: 0 2rem !important;
  /* ✅ 좌우 패딩을 늘려서 넓이 확대 */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
  box-sizing: border-box;
  overflow: visible !important;
  /* ✅ overflow를 visible로 변경하여 확대된 내용이 보이도록 */
  text-align: left;
  position: relative;
}

/* ✅ Speed 컨텐츠 래퍼 - 제목 아래 공간 확보 */
.speed-content-wrapper {
  width: 100%;
  max-width: none !important;
  /* ✅ 최대 너비 제한 제거 */
  display: flex;
  flex-direction: column;
  align-items: center;
  /* ✅ 컨텐츠를 가운데 정렬 */
  justify-content: center;
  /* ✅ 컨텐츠를 세로 가운데 정렬 */
  padding-top: 0 !important;
  /* ✅ 상단 패딩 제거 - 가운데 정렬을 위해 */
  padding-left: 2rem !important;
  padding-right: 2rem !important;
  /* ✅ 좌우 패딩을 늘려서 넓이 확대 */
  box-sizing: border-box;
  /* ✅ height: 100% 제거 - 내용에 맞게 자동 조정 */
  margin: 0 auto;
  /* ✅ 가운데 정렬 */
  height: 100%;
  /* ✅ 높이를 100%로 설정하여 가운데 정렬 가능하도록 */
}

/* ✅ Speed 설명 텍스트 */
.speed-description-text {
  margin-top: 0.5rem;
  margin-bottom: 0;
  flex-shrink: 0;
}

/* ✅ 제어 버튼 섹션 - Speed 입력 창 바로 아래에 배치 */
.sun-track-mode .control-section.speed-section-card .control-buttons-section {
  margin-top: 1rem !important;
  /* ✅ 입력 창 바로 아래에 위치 (auto 제거) */
  padding-top: 0 !important;
  flex-shrink: 0 !important;
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ 컴팩트 컨트롤 스타일 */
.sun-track-mode .compact-control {
  padding: 0 8px;
  margin: 0;
  min-height: auto;
  height: auto;
  line-height: 1;
  vertical-align: top;
}

.sun-track-mode .compact-control .q-input {
  margin-bottom: 0.25rem;
}

.sun-track-mode .compact-control .q-btn {
  min-height: 2rem;
  padding: 0.25rem;
}

/* ✅ 레이아웃 정렬 스타일 - EphemerisDesignationPage와 동일 */
.sun-track-mode .align-center {
  align-items: center;
}

.sun-track-mode .justify-end {
  justify-content: flex-end;
}

.sun-track-mode .justify-center {
  justify-content: center;
}

/* ✅ 입력 필드 스타일 */
.sun-track-mode .offset-input {
  width: 110px;
  min-width: 110px;
  max-width: 110px;
}

/* ✅ 제어 버튼 섹션 스타일 */
.control-buttons-section .q-btn {
  min-width: 120px;
}

/* ✅ Speed 입력 필드 스타일 */
.speed-input {
  width: 100%;
}

/* ✅ 카드 테두리 위아래 패딩 완전 제거 - EphemerisDesignationPage와 동일 */
.q-card.control-card .q-card-section.compact-control {
  padding: 0px 8px !important;
}

/* ✅ 추가적인 강제 적용 - EphemerisDesignationPage와 동일 */
.q-card-section.compact-control {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
}

/* ✅ q-gutter-none 전역 스타일 - EphemerisDesignationPage와 동일 */
.compact-control .row.q-gutter-none {
  margin: 0 !important;
  padding: 0 !important;
}

.compact-control .row.q-gutter-none>div {
  padding-left: 0.25rem !important;
  padding-right: 0.25rem !important;
}

.compact-control .row.q-gutter-none>div:first-child {
  padding-left: 0 !important;
}

.compact-control .row.q-gutter-none>div:last-child {
  padding-right: 0 !important;
}

/* ✅ 더 강력한 강제 적용 - 모든 가능한 셀렉터 - EphemerisDesignationPage와 동일 */
.q-card-section.compact-control.purple-1,
.q-card.control-card .q-card-section.compact-control.purple-1,
.q-card-section[class*="compact-control"],
.q-card-section[class*="purple-1"] {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
  margin-top: 0px !important;
  margin-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
  display: flex !important;
  align-items: flex-start !important;
}

/* ✅ SunTrackPage 전역 스타일 추가 - EphemerisDesignationPage와 동일 */
/* 전역 스타일은 이미 위에 정의되어 있으므로, .sun-track-mode 스코프 스타일만 추가 */

/* ✅ Quasar 기본 스타일 덮어쓰기 */
.sun-track-mode .q-card.control-card .q-card-section {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

.sun-track-mode .q-card.control-card {
  margin-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

.sun-track-mode .q-card.control-card .q-card__section {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

/* ✅ 방법 1: 왼쪽 세로 라벨 (카드 안) - 높이 최적화 - scoped 스타일 안에 전역 선택자 (EphemerisDesignationPage와 동일) */
.position-offset-label {
  background: linear-gradient(135deg, rgba(25, 118, 210, 0.15) 0%, rgba(25, 118, 210, 0.08) 100%);
  padding: 4px 8px;
  /* 높이 줄임: 8px 12px → 4px 8px */
  border-radius: 6px;
  border-right: 3px solid var(--q-primary);
  min-width: 50px;
  /* 너비도 약간 줄임: 60px → 50px */
  margin-right: 6px;
  /* 간격도 줄임: 8px → 6px */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  /* 텍스트 크기 줄임 */
  line-height: 1.2 !important;
  /* 줄 간격 줄임 */
}

/* ✅ SunTrack 전용 보정 - EphemerisDesignationPage와 동일 */
.sun-track-mode .position-offset-label {
  padding: 4px 8px !important;
  min-width: 50px !important;
  border-right: 1px solid var(--q-primary) !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) !important;
}

.sun-track-mode .position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  line-height: 1.2 !important;
}

/* ✅ 간격 제거로 더 타이트하게 - EphemerisDesignationPage와 동일 */
.sun-track-mode .compact-control .row.q-gutter-none {
  margin: 0 !important;
  padding: 0 !important;
}

.sun-track-mode .compact-control .row.q-gutter-none>div {
  padding-left: 0.25rem !important;
  padding-right: 0.25rem !important;
}

.sun-track-mode .compact-control .row.q-gutter-none>div:first-child {
  padding-left: 0 !important;
}

.sun-track-mode .compact-control .row.q-gutter-none>div:last-child {
  padding-right: 0 !important;
}

/* ✅ 추가 높이 줄이기 */
.sun-track-mode .q-input {
  min-height: auto !important;
}

.sun-track-mode .q-field__control {
  min-height: auto !important;
}

.sun-track-mode .q-field__native {
  padding: auto !important;
}

.sun-track-mode .q-btn {
  min-height: auto !important;
}

.sun-track-mode .q-btn--dense {
  min-height: auto !important;
}

/* ✅ 컴팩트 컨트롤 레이아웃 */
.sun-track-mode .compact-control .row {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  width: 100%;
}

.sun-track-mode .compact-control .q-field {
  margin-bottom: 0;
}

.sun-track-mode .compact-control .col-auto {
  flex-shrink: 0;
}

/* ✅ 세부 레이아웃 스타일 */
.sun-track-mode .compact-control .row .row {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 0.25rem;
}

.sun-track-mode .compact-control .text-subtitle2 {
  display: flex;
  align-items: center;
  height: 100%;
  margin: 0;
  padding: 0;
  font-size: 0.9rem;
  white-space: nowrap;
}

.sun-track-mode .compact-control .col-1 {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-width: fit-content;
}
</style>
