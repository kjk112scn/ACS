<template>
  <q-page class="dashboard-container q-pa-md">
    <!-- Offset 타이틀 (네모난 칸 밖에 위치) -->
    <div class="section-title text-h5 text-primary q-mb-sm">Offset</div>

    <!-- 입력-출력 박스 -->
    <q-card class="input-output-section q-mb-md">
      <q-card-section>
        <div class="input-output-grid">
          <!-- 입력-출력 쌍 1 (Azimuth) -->
          <div class="input-output-pair">
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
              </div>
            </div>
            <div class="output-box">
              <div class="label-text adaptive-caption" style="opacity: 0">출력</div>
              <q-input v-model="outputs[0]" dense outlined readonly />
            </div>
            <div class="spacer"></div>
          </div>

          <!-- 입력-출력 쌍 2 (Elevation) -->
          <div class="input-output-pair">
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
              </div>
            </div>
            <div class="output-box">
              <div class="label-text adaptive-caption" style="opacity: 0">출력</div>
              <q-input v-model="outputs[1]" dense outlined readonly />
            </div>
            <div class="spacer"></div>
          </div>

          <!-- 입력-출력 쌍 3 (Tilt) -->
          <div class="input-output-pair">
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
              </div>
            </div>
            <div class="output-box">
              <div class="label-text adaptive-caption" style="opacity: 0">출력</div>
              <q-input v-model="outputs[2]" dense outlined readonly />
            </div>
            <div class="spacer"></div>
          </div>

          <!-- 입력-출력 쌍 4 (Polarization) -->
          <div class="input-output-pair">
            <div class="input-box">
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
              </div>
            </div>
            <div class="output-box">
              <div class="label-text adaptive-caption" style="opacity: 0">출력</div>
              <q-input v-model="outputs[3]" dense outlined readonly />
            </div>
            <div class="spacer"></div>
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
    alert(
      `Sun Track이 시작되었습니다. 설정된 속도: Az=${azimuthSpeed}, El=${elevationSpeed}, Tilt=${tiltSpeed}`,
    )
  } catch (error) {
    console.error('Sun Track 시작 중 오류:', error)
    alert('Sun Track 시작 중 오류가 발생했습니다.')
  } finally {
    isGoLoading.value = false
  }
}

// Stop 명령 처리 함수 (Sun Track 중지)
const handleStopCommand = async () => {
  try {
    isStopLoading.value = true

    // Sun Track 중지 API 호출
    await icdStore.stopSunTrack()

    // Notify 대신 console.log 사용 (임시)
    console.log('Sun Track이 중지되었습니다.')

    // Notify가 작동하지 않는 경우 alert 사용 (임시)
    alert('Sun Track이 중지되었습니다.')
  } catch (error) {
    console.error('Sun Track 중지 중 오류:', error)
    alert('Sun Track 중지 중 오류가 발생했습니다.')
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
    alert('Stow 명령이 성공적으로 전송되었습니다.')
  } catch (error) {
    console.error('Stow 명령 처리 중 오류:', error)
    alert('Stow 명령 전송 중 오류가 발생했습니다.')
  } finally {
    isStowLoading.value = false
  }
}
// 증가 함수 (0.01 단위로 증가)
const increment = async (index: number) => {
  const value = inputs.value[index] || '0'
  const currentValue = parseFloat(value)
  if (!isNaN(currentValue)) {
    inputs.value[index] = (currentValue + 0.01).toFixed(2)
  } else {
    inputs.value[index] = '0.01'
  }

  // Azimuth, Elevation, Tilt 값이 변경된 경우에만 API 호출
  if (index <= 2) {
    try {
      // 입력값을 숫자로 변환 (타입 에러 수정)
      const azOffset = parseFloat(inputs.value[0] || '0')
      const elOffset = parseFloat(inputs.value[1] || '0')
      const tiOffset = parseFloat(inputs.value[2] || '0')

      // ICD 스토어의 함수 호출하고 응답 받기
      const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)

      // 응답에서 출력값 업데이트 (백엔드 응답 구조에 맞게 수정 필요)
      if (response && typeof response === 'object') {
        // 백엔드 응답에 해당 값이 있다면 그 값을 사용
        // 예시: response.azimuthResult, response.elevationResult, response.tiltResult
        // 실제 백엔드 응답 구조에 맞게 수정 필요
        outputs.value[0] = response.azimuthResult?.toFixed(2) || inputs.value[0]
        outputs.value[1] = response.elevationResult?.toFixed(2) || inputs.value[1]
        outputs.value[2] = response.tiltResult?.toFixed(2) || inputs.value[2]
      } else {
        // 응답이 없거나 예상 형식이 아닌 경우 입력값을 그대로 출력값으로 사용
        outputs.value[0] = inputs.value[0] || '0.00'
        outputs.value[1] = inputs.value[1] || '0.00'
        outputs.value[2] = inputs.value[2] || '0.00'
      }
    } catch (error) {
      console.error('오프셋 명령 처리 중 오류:', error)
    }
  }
  // Time 값이 변경된 경우 Time Offset API 호출
  else if (index === 3) {
    try {
      // 입력값을 숫자로 변환
      const timeOffset = parseFloat(inputs.value[3] || '0')

      // ICD 스토어의 시간 오프셋 함수 호출하고 응답 받기
      const response = await icdStore.sendTimeOffsetCommand(timeOffset)

      // 응답에서 출력값 업데이트
      if (response && typeof response === 'object' && response.resultTimeOffset !== undefined) {
        outputs.value[3] = response.resultTimeOffset.toFixed(2) || inputs.value[3]
      } else {
        // 응답이 없거나 예상 형식이 아닌 경우 입력값을 그대로 출력값으로 사용
        outputs.value[3] = inputs.value[3] || '0.00'
      }
    } catch (error) {
      console.error('시간 오프셋 명령 처리 중 오류:', error)
    }
  }
}

// 감소 함수 (0.01 단위로 감소)
const decrement = async (index: number) => {
  const value = inputs.value[index] || '0'
  const currentValue = parseFloat(value)
  if (!isNaN(currentValue)) {
    inputs.value[index] = (currentValue - 0.01).toFixed(2)
  } else {
    inputs.value[index] = '-0.01'
  }

  // Azimuth, Elevation, Tilt 값이 변경된 경우에만 API 호출
  if (index <= 2) {
    try {
      // 입력값을 숫자로 변환 (타입 에러 수정)
      const azOffset = parseFloat(inputs.value[0] || '0')
      const elOffset = parseFloat(inputs.value[1] || '0')
      const tiOffset = parseFloat(inputs.value[2] || '0')

      // ICD 스토어의 함수 호출하고 응답 받기
      const response = await icdStore.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)

      // 응답에서 출력값 업데이트 (백엔드 응답 구조에 맞게 수정 필요)
      if (response && typeof response === 'object') {
        // 백엔드 응답에 해당 값이 있다면 그 값을 사용
        // 예시: response.azimuthResult, response.elevationResult, response.tiltResult
        // 실제 백엔드 응답 구조에 맞게 수정 필요
        outputs.value[0] = response.azimuthResult?.toFixed(2) || inputs.value[0]
        outputs.value[1] = response.elevationResult?.toFixed(2) || inputs.value[1]
        outputs.value[2] = response.tiltResult?.toFixed(2) || inputs.value[2]
      } else {
        // 응답이 없거나 예상 형식이 아닌 경우 입력값을 그대로 출력값으로 사용
        outputs.value[0] = inputs.value[0] || '0.00'
        outputs.value[1] = inputs.value[1] || '0.00'
        outputs.value[2] = inputs.value[2] || '0.00'
      }
    } catch (error) {
      console.error('오프셋 명령 처리 중 오류:', error)
    }
  }
  // Time 값이 변경된 경우 Time Offset API 호출
  else if (index === 3) {
    try {
      // 입력값을 숫자로 변환
      const timeOffset = parseFloat(inputs.value[3] || '0')

      // ICD 스토어의 시간 오프셋 함수 호출하고 응답 받기
      const response = await icdStore.sendTimeOffsetCommand(timeOffset)

      // 응답에서 출력값 업데이트
      if (response && typeof response === 'object' && response.resultTimeOffset !== undefined) {
        outputs.value[3] = response.resultTimeOffset.toFixed(2) || inputs.value[3]
      } else {
        // 응답이 없거나 예상 형식이 아닌 경우 입력값을 그대로 출력값으로 사용
        outputs.value[3] = inputs.value[3] || '0.00'
      }
    } catch (error) {
      console.error('시간 오프셋 명령 처리 중 오류:', error)
    }
  }
}
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
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
}

.input-output-pair {
  display: flex;
  position: relative;
  padding: 0 0.5rem;
}

.input-box,
.output-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.label-text {
  font-size: 0.9rem;
  margin-bottom: 0.25rem;
}

.input-with-buttons {
  display: flex;
  align-items: center;
  height: 40px; /* 입력 필드 높이 고정 */
}

.input-field {
  flex: 1;
}

.button-group {
  display: flex;
  flex-direction: column;
  margin-left: 4px;
}

/* 출력 필드 높이 맞추기 */
.output-box .q-field {
  height: 40px; /* 출력 필드 높이 고정 */
}

/* 간격 유지를 위한 스페이서 */
.spacer {
  width: 20px; /* 이전 구분선 컨테이너와 동일한 너비 */
  margin-top: 20px; /* 제목 아래부터 시작 */
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
    grid-template-columns: 1fr;
  }

  .input-output-pair {
    flex-direction: column;
    padding: 0;
  }

  .spacer {
    display: none; /* 모바일에서는 스페이서 숨김 */
  }
}
</style>
