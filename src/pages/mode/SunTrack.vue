<template>
  <q-page class="dashboard-container q-pa-md">
    <!-- Offset 타이틀 (네모난 칸 밖에 위치) -->
    <div class="section-title text-h5 text-primary q-mb-sm">Offset</div>

    <!-- 입력-출력 박스 -->
    <q-card class="input-output-section q-mb-md">
      <q-card-section>
        <div class="input-output-grid">
          <!-- 입력-출력 쌍 1 (Azimuth) -->

          <div class="input-output-pair standard-width">
            <div class="input-box">
              <div class="label-text text-weight-medium text-primary">Azimuth</div>
              <div class="input-with-buttons">
                <q-input
                  v-model="inputs[0]"
                  dense
                  outlined
                  class="input-field"
                  type="number"
                  step="0.01"
                />
                <div class="button-group">
                  <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(0)" />
                  <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(0)" />
                </div>
                <q-btn
                  icon="refresh"
                  size="sm"
                  color="primary"
                  dense
                  flat
                  class="reset-button"
                  @click="reset(0)"
                />
              </div>
            </div>
            <div class="output-box">
              <div class="label-text adaptive-caption" style="opacity: 0">출력</div>
              <q-input v-model="outputs[0]" dense outlined readonly />
            </div>
            <div class="spacer spacer-between"></div>
          </div>

          <!-- 입력-출력 쌍 2 (Elevation) -->

          <div class="input-output-pair standard-width">
            <div class="input-box">
              <div class="label-text text-weight-medium text-primary">Elevation</div>
              <div class="input-with-buttons">
                <q-input
                  v-model="inputs[1]"
                  dense
                  outlined
                  class="input-field"
                  type="number"
                  step="0.01"
                />
                <div class="button-group">
                  <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(1)" />
                  <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(1)" />
                </div>
                <q-btn
                  icon="refresh"
                  size="sm"
                  color="primary"
                  dense
                  flat
                  class="reset-button"
                  @click="reset(1)"
                />
              </div>
            </div>
            <div class="output-box">
              <div class="label-text adaptive-caption" style="opacity: 0">출력</div>
              <q-input v-model="outputs[1]" dense outlined readonly />
            </div>
            <div class="spacer spacer-between"></div>
          </div>

          <!-- 입력-출력 쌍 3 (Tilt) -->

          <div class="input-output-pair standard-width">
            <div class="input-box">
              <div class="label-text text-weight-medium text-primary">Tilt</div>
              <div class="input-with-buttons">
                <q-input
                  v-model="inputs[2]"
                  dense
                  outlined
                  class="input-field"
                  type="number"
                  step="0.01"
                />
                <div class="button-group">
                  <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(2)" />
                  <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(2)" />
                </div>
                <q-btn
                  icon="refresh"
                  size="sm"
                  color="primary"
                  dense
                  flat
                  class="reset-button"
                  @click="reset(2)"
                />
              </div>
            </div>
            <div class="output-box">
              <div class="label-text adaptive-caption" style="opacity: 0">출력</div>
              <q-input v-model="outputs[2]" dense outlined readonly />
            </div>
            <div class="spacer spacer-between"></div>
          </div>

          <!-- 입력-출력 쌍 4 (TimeOffset) - 더 넓게 표시 -->
          <div class="input-output-pair wide-width">
            <div class="input-box time-input">
              <div class="label-text text-weight-medium text-primary">Time</div>
              <div class="input-with-buttons">
                <q-input
                  v-model="inputs[3]"
                  dense
                  outlined
                  class="input-field"
                  type="number"
                  step="0.01"
                />
                <div class="button-group">
                  <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(3)" />
                  <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(3)" />
                </div>
                <q-btn
                  icon="refresh"
                  size="sm"
                  color="primary"
                  dense
                  flat
                  class="reset-button"
                  @click="reset(3)"
                />
              </div>
            </div>

            <div class="output-box time-result">
              <div class="label-text text-weight-medium text-primary">Result</div>
              <q-input v-model="outputs[3]" dense outlined readonly />
            </div>
            <!-- 새로 추가된 출력창 -->

            <div class="output-box time-caltime">
              <div class="label-text text-weight-medium text-primary">Cal Time</div>

              <q-input v-model="formattedCalTime" dense outlined readonly />
            </div>
            <div class="spacer spacer-end"></div>
          </div>
        </div>

        <!-- 상태 메시지 표시 -->
        <div class="status-message q-mt-md" v-if="showStatusMessage">
          <q-banner
            :class="
              icdStore.lastOffsetCommandStatus.success
                ? 'bg-positive text-white'
                : 'bg-negative text-white'
            "
          >
            {{ icdStore.lastOffsetCommandStatus.message }}
          </q-banner>
          <!-- Time Offset 상태 메시지 -->
          <q-banner
            :class="
              icdStore.lastTimeOffsetCommandStatus.success
                ? 'bg-positive text-white'
                : 'bg-negative text-white'
            "
          >
            {{ icdStore.lastTimeOffsetCommandStatus.message }}
          </q-banner>
        </div>
      </q-card-section>
    </q-card>
    <!-- Speed 타이틀 (네모난 칸 밖에 위치) -->
    <div class="section-title text-h5 text-primary q-mb-sm q-mt-lg">Speed</div>

    <!-- Speed 입력 박스 -->
    <q-card class="speed-section q-mb-md">
      <q-card-section>
        <div class="row q-col-gutter-md">
          <!-- Azimuth Speed -->
          <div class="col-4">
            <div class="label-text text-weight-medium text-primary">Azimuth Speed</div>
            <q-input
              v-model="speedInputs.azimuth"
              dense
              outlined
              type="number"
              step="0.1"
              class="speed-input"
            />
          </div>

          <!-- Elevation Speed -->
          <div class="col-4">
            <div class="label-text text-weight-medium text-primary">Elevation Speed</div>
            <q-input
              v-model="speedInputs.elevation"
              dense
              outlined
              type="number"
              step="0.1"
              class="speed-input"
            />
          </div>

          <!-- Tilt Speed -->
          <div class="col-4">
            <div class="label-text text-weight-medium text-primary">Tilt Speed</div>
            <q-input
              v-model="speedInputs.tilt"
              dense
              outlined
              type="number"
              step="0.1"
              class="speed-input"
            />
          </div>
        </div>

        <!-- 설명 텍스트 추가 -->
        <div class="text-caption q-mt-sm text-grey-7">
          속도 값은 Go 버튼을 클릭할 때 적용됩니다.
        </div>
      </q-card-section>
    </q-card>

    <!-- 제어 버튼 섹션 -->
    <div class="control-buttons-section q-mt-md">
      <q-card>
        <q-card-section>
          <div class="row q-col-gutter-md justify-center">
            <div class="col-auto">
              <q-btn
                label="Go"
                color="positive"
                icon="play_arrow"
                size="lg"
                @click="handleGoCommand"
                :loading="isGoLoading"
              />
            </div>
            <div class="col-auto">
              <q-btn
                label="Stop"
                color="negative"
                icon="stop"
                size="lg"
                @click="handleStopCommand"
                :loading="isStopLoading"
              />
            </div>
            <div class="col-auto">
              <q-btn
                label="Stow"
                color="primary"
                icon="home"
                size="lg"
                @click="handleStowCommand"
                :loading="isStowLoading"
              />
            </div>
          </div>
        </q-card-section>
      </q-card>
    </div>
  </q-page>
</template>
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useICDStore } from '../../stores/ICD'

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

// 상태 메시지 표시 여부 (최근 3초 이내의 메시지만 표시)
const showStatusMessage = computed(() => {
  const currentTime = Date.now()
  const messageTime = icdStore.lastOffsetCommandStatus.timestamp
  return currentTime - messageTime < 3000 && icdStore.lastOffsetCommandStatus.message !== ''
})

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
// Speed 입력 필드
const speedInputs = ref({
  azimuth: '0.0',
  elevation: '0.0',
  tilt: '0.0',
})

// Go 명령 처리 함수 (Sun Track 시작)
const handleGoCommand = async () => {
  try {
    isGoLoading.value = true

    // Speed 값 가져오기 (문자열을 숫자로 변환)
    const azimuthSpeed = parseFloat(speedInputs.value.azimuth) || 0.0
    const elevationSpeed = parseFloat(speedInputs.value.elevation) || 0.0
    const tiltSpeed = parseFloat(speedInputs.value.tilt) || 0.0

    // 기본 interval 값 (밀리초 단위, 필요에 따라 조정)
    const interval = 100 // 1초

    // Sun Track 시작 API 호출
    await icdStore.startSunTrack(interval, azimuthSpeed, elevationSpeed, tiltSpeed)

    // Notify 대신 console.log 사용 (임시)
    console.log('Sun Track이 시작되었습니다. 설정된 속도:', azimuthSpeed, elevationSpeed, tiltSpeed)

    // Notify가 작동하지 않는 경우 alert 사용 (임시)
    /* alert(
      `Sun Track이 시작되었습니다. 설정된 속도: Az=${azimuthSpeed}, El=${elevationSpeed}, Tilt=${tiltSpeed}`,
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

  // Azimuth, Elevation, Tilt 값이 변경된 경우에만 API 호출
  if (index <= 2) {
    try {
      // 입력값을 숫자로 변환 (타입 에러 수정)
      const azOffset = parseFloat(offsetCals.value[0] || '0')
      const elOffset = parseFloat(offsetCals.value[1] || '0')
      const tiOffset = parseFloat(offsetCals.value[2] || '0')

      // ICD 스토어의 함수 호출하고 응답 받기
      const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)

      // 응답에서 출력값 업데이트 (백엔드 응답 구조에 맞게 수정 필요)
      if (response && typeof response === 'object') {
        // 백엔드 응답에 해당 값이 있다면 그 값을 사용
        // 예시: response.azimuthResult, response.elevationResult, response.tiltResult
        // 실제 백엔드 응답 구조에 맞게 수정 필요
        outputs.value[0] = response.azimuthResult?.toFixed(2) || offsetCals.value[0]
        outputs.value[1] = response.elevationResult?.toFixed(2) || offsetCals.value[1]
        outputs.value[2] = response.tiltResult?.toFixed(2) || offsetCals.value[2]
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
      if (response && typeof response === 'object' && response.inputTimeoffset !== undefined) {
        outputs.value[3] = response.inputTimeoffset.toFixed(2) || offsetCals.value[3]
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

  // Azimuth, Elevation, Tilt 값이 변경된 경우에만 API 호출
  if (index <= 2) {
    try {
      // 입력값을 숫자로 변환 (타입 에러 수정)
      const azOffset = parseFloat(offsetCals.value[0] || '0')
      const elOffset = parseFloat(offsetCals.value[1] || '0')
      const tiOffset = parseFloat(offsetCals.value[2] || '0')

      // ICD 스토어의 함수 호출하고 응답 받기
      const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)

      // 응답에서 출력값 업데이트 (백엔드 응답 구조에 맞게 수정 필요)
      if (response && typeof response === 'object') {
        // 백엔드 응답에 해당 값이 있다면 그 값을 사용
        // 예시: response.azimuthResult, response.elevationResult, response.tiltResult
        // 실제 백엔드 응답 구조에 맞게 수정 필요
        outputs.value[0] = response.azimuthResult?.toFixed(2) || offsetCals.value[0]
        outputs.value[1] = response.elevationResult?.toFixed(2) || offsetCals.value[1]
        outputs.value[2] = response.tiltResult?.toFixed(2) || offsetCals.value[2]
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
      if (response && typeof response === 'object' && response.inputTimeoffset !== undefined) {
        outputs.value[3] = response.inputTimeoffset.toFixed(2) || offsetCals.value[3]
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
      outputs.value[0] = response.azimuthResult?.toFixed(2) || '0.00'
      offsetCals.value[0] = outputs.value[0] as string
    } else if (index === 1) {
      // 고도각 오프셋 초기화 - 다른 축은 현재 값 유지
      const response = await icdStore.sendPositionOffsetCommand(azOffset, 0, tiOffset)
      outputs.value[1] = response.elevationResult?.toFixed(2) || '0.00'
      offsetCals.value[1] = outputs.value[1] as string
    } else if (index === 2) {
      // 틸트각 오프셋 초기화 - 다른 축은 현재 값 유지
      const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, 0)
      outputs.value[2] = response.tiltResult?.toFixed(2) || '0.00'
      offsetCals.value[2] = outputs.value[2] as string
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

    // Format as yyyy-mm-dd<br>hh:mm:ss.SSS using UTC methods
    const year = date.getUTCFullYear()
    const month = String(date.getUTCMonth() + 1).padStart(2, '0')
    const day = String(date.getUTCDate()).padStart(2, '0')
    const hours = String(date.getUTCHours()).padStart(2, '0')
    const minutes = String(date.getUTCMinutes()).padStart(2, '0')
    const seconds = String(date.getUTCSeconds()).padStart(2, '0')
    const milliseconds = String(date.getUTCMilliseconds()).padStart(3, '0')

    // 날짜와 시간을 HTML 줄바꿈 태그로 구분
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${milliseconds}`
  } catch (error) {
    console.error('Error formatting timestamp:', error)
    return calTime // Return original value if formatting fails
  }
})
</script>

<style>
/* 전역 스타일: 다크 모드와 라이트 모드에 따른 텍스트 색상 조정 */
.body--dark .adaptive-text {
  color: white !important;
}

.body--light .adaptive-text {
  color: black !important;
}

/* 다크 모드와 라이트 모드에 따른 caption 텍스트 색상 조정 */
.body--dark .adaptive-caption {
  color: rgba(255, 255, 255, 0.7) !important;
}

.body--light .adaptive-caption {
  color: rgba(0, 0, 0, 0.6) !important;
}

/* 숫자 입력 필드의 화살표 버튼 숨기기 */
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
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
}

/* 입력 및 출력 섹션 스타일 */
.input-output-grid {
  display: flex;
  flex-wrap: wrap;

  gap: 0.25rem; /* 간격을 최소화 */
  justify-content: flex-start; /* 왼쪽부터 배치 */
}

.input-output-pair {
  display: flex;
  position: relative;

  padding: 0 0.1rem; /* 패딩 최소화 */
}

/* 표준 너비 (Azimuth, Elevation, Tilt) */
.standard-width {
  width: calc(20% - 0.25rem); /* 간격을 고려하여 약간 줄임 */
}

/* 넓은 너비 (Time) */
.wide-width {
  width: calc(40% - 0.25rem); /* 나머지 공간을 차지하도록 조정 */
}

.input-box {
  flex: 2.2;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin: 0;
}
.output-box {
  flex: 1.2;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin: 0;
}

.label-text {
  font-size: 0.9rem;

  margin-bottom: 0.1rem; /* 마진 줄이기 */
}

.input-with-buttons {
  display: flex;
  align-items: center;

  height: 40px;
  position: relative; /* 상대 위치 설정 */
}

.input-field {
  flex: 1;
}

.button-group {
  display: flex;
  flex-direction: column;
  margin-left: 2px;
}

.reset-button {
  margin-left: 2px;
  margin-right: 5px; /* 더 큰 여백 */
}

/* 출력 필드 높이 맞추기 */
.output-box .q-field {
  height: 40px;
}

/* 간격 유지를 위한 스페이서 - 제거하거나 최소화 */
.spacer-between {
  width: 15px; /* 요소 사이의 간격 */
}

/* 마지막 요소 뒤의 spacer */
.spacer-end {
  width: 2px; /* 최소 너비로 설정 */
}

/* 상태 메시지 스타일 */
.status-message {
  transition: opacity 0.3s;
}

/* 제어 버튼 섹션 스타일 */
.control-buttons-section .q-btn {
  min-width: 120px;
}

/* 모바일 화면에서는 레이아웃 조정 */
@media (max-width: 768px) {
  .input-output-grid {
    flex-direction: column;
  }

  .input-output-pair {
    width: 100% !important;
    margin-bottom: 0.5rem; /* 마진 줄이기 */
  }

  .spacer {
    display: none;
  }
}

/* Time 관련 요소의 너비 조정 */
.time-input {
  flex: 0.6; /* Time 입력 부분을 절반 크기로 줄임 */
}

.time-result {
  flex: 0.45; /* 입력과 Result 출력 부분을 절반 크기로 줄임 */
}

.time-caltime {
  flex: 0.95; /* Cal Time 부분을 2배로 키움 (입력과 Result 합친 것보다 크게) */
}
</style>
