<template>
  <div class="sun-track-mode">

    <!-- 1행: Offset Controls - EphemerisDesignationPage와 동일한 구조 -->
    <div class="row q-col-gutter-md q-mb-sm offset-control-row">
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
    <!-- Speed 섹션 -->
    <div class="section-title text-h5 text-primary q-mb-sm q-mt-lg">Speed</div>

    <!-- Speed 입력 박스 -->
    <q-card class="control-section q-mb-md">
      <q-card-section>
        <div class="row q-col-gutter-md">
          <!-- Azimuth Speed -->
          <div class="col-4">
            <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Azimuth Speed</div>
            <q-input v-model="speedInputs.azimuth" dense outlined type="number" step="0.1" class="speed-input" />
          </div>

          <!-- Elevation Speed -->
          <div class="col-4">
            <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Elevation Speed</div>
            <q-input v-model="speedInputs.elevation" dense outlined type="number" step="0.1" class="speed-input" />
          </div>

          <!-- Tilt Speed -->
          <div class="col-4">
            <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Tilt Speed</div>
            <q-input v-model="speedInputs.train" dense outlined type="number" step="0.1" class="speed-input" />
          </div>
        </div>

        <!-- 설명 텍스트 추가 -->
        <div class="text-caption q-mt-sm" style="color: var(--theme-text-secondary);">
          속도 값은 Go 버튼을 클릭할 때 적용됩니다.
        </div>
      </q-card-section>
    </q-card>

    <!-- 제어 버튼 섹션 -->
    <div class="control-buttons-section q-mt-md">
      <q-card class="control-section">
        <q-card-section>
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
        </q-card-section>
      </q-card>
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
}
</style>

<style scoped>
/* ✅ 섹션 제목 스타일 */
.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
  margin-bottom: 0.5rem !important;
}

/* ✅ 제목과 상태 표시 컨테이너 */
.section-title-container {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.tracking-status-chip {
  font-weight: 500;
}

/* ✅ 오프셋 컨트롤 행 하단 여백 줄이기 */
.sun-track-mode .offset-control-row {
  margin-bottom: 0.5rem !important;
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

/* ✅ 라벨 스타일 - EphemerisDesignationPage와 동일 */
.position-offset-label {
  background: linear-gradient(135deg, rgba(25, 118, 210, 0.15) 0%, rgba(25, 118, 210, 0.08) 100%);
  padding: 4px 8px;
  border-radius: 6px;
  border-right: 3px solid var(--q-primary);
  min-width: 50px;
  margin-right: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  line-height: 1.2 !important;
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
.control-section {
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
  width: 100%;
}

.control-card {
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
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

/* ✅ 카드 테두리 위아래 패딩 완전 제거 */
.q-card.control-card .q-card-section.compact-control {
  padding: 0px 8px !important;
}

.q-card-section.compact-control {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
}

/* ✅ 더 강력한 강제 적용 */
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

/* ✅ 카드 테두리 위아래 패딩 완전 제거 */
.q-card.control-card .q-card-section.compact-control {
  padding: 0px 8px !important;
}

.q-card-section.compact-control {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
}

/* ✅ 더 강력한 강제 적용 */
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

/* ✅ SunTrack 전용 보정 */
.sun-track-mode .flexible-offset-layout {
  gap: 20px;
  padding: 0 8px;
}

.sun-track-mode .q-card.control-card {
  overflow: visible !important;
}

.sun-track-mode .q-card-section.compact-control {
  display: block !important;
  padding-left: 16px !important;
  padding-right: 16px !important;
  overflow: visible !important;
}

/* ✅ 추가 높이 줄이기 */
.sun-track-mode .q-input {
  min-height: auto !important;
}

.sun-track-mode .q-field__control {
  min-height: auto !important;
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
</style>
