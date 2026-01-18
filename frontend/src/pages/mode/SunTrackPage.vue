<template>
  <div class="sun-track-mode">
    <!-- 1행: Offset Controls -->
    <div class="row q-col-gutter-md q-mb-sm offset-control-row">
      <div class="col-12">
        <OffsetControls
          :inputs="inputs"
          :outputs="outputs"
          :cal-time="formattedCalTime"
          @input-change="onInputChange"
          @increment="increment"
          @decrement="decrement"
          @reset="reset"
        />
      </div>
    </div>

    <!-- 2행: Speed Settings -->
    <div class="row q-col-gutter-md main-content-row">
      <div class="col-12">
        <q-card class="control-section speed-section-card">
          <q-card-section class="speed-section-content">
            <div class="text-subtitle1 text-weight-bold text-primary speed-section-title">Speed Settings</div>
            <div class="speed-inputs-row">
              <div class="speed-content-wrapper">
                <div class="row q-col-gutter-xl">
                  <!-- Azimuth Speed -->
                  <div class="col-12 col-md-4">
                    <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Azimuth Speed</div>
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
                  <div class="col-12 col-md-4">
                    <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Elevation Speed</div>
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
                  <div class="col-12 col-md-4">
                    <div class="text-subtitle2 text-weight-medium text-primary q-mb-xs">Tilt Speed</div>
                    <q-input
                      v-model="speedInputs.train"
                      dense
                      outlined
                      type="number"
                      step="0.1"
                      class="speed-input"
                    />
                  </div>
                </div>

                <!-- 설명 텍스트 -->
                <div class="speed-description-text text-caption q-mt-sm">
                  속도 값은 Go 버튼을 클릭할 때 적용됩니다.
                </div>

                <!-- 제어 버튼 섹션 - 공용 컴포넌트 사용 -->
                <ControlButtonBar
                  class="q-mt-md"
                  :go-loading="isGoLoading"
                  :stop-loading="isStopLoading"
                  :stow-loading="isStowLoading"
                  @go="handleGoCommand"
                  @stop="handleStopCommand"
                  @stow="handleStowCommand"
                />
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { OffsetControls, useOffsetControls } from './shared'
import { useNotification } from '@/composables/useNotification'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { ControlButtonBar } from '@/components/common'

// ICD 스토어
const icdStore = useICDStore()
const { success } = useNotification()
const { handleApiError } = useErrorHandler()

// Offset Controls (공용 composable - 3페이지 동기화)
const {
  inputs,
  outputs,
  formattedCalTime,
  onInputChange,
  increment,
  decrement,
  reset
} = useOffsetControls()

// Speed 입력 필드
const speedInputs = ref({
  azimuth: '5.0',
  elevation: '5.0',
  train: '5.0',
})

// 버튼 로딩 상태
const isGoLoading = ref(false)
const isStopLoading = ref(false)
const isStowLoading = ref(false)

// Go 명령 처리 함수 (Sun Track 시작)
const handleGoCommand = async () => {
  try {
    isGoLoading.value = true
    const azimuthSpeed = parseFloat(speedInputs.value.azimuth) || 0.0
    const elevationSpeed = parseFloat(speedInputs.value.elevation) || 0.0
    const trainSpeed = parseFloat(speedInputs.value.train) || 0.0
    const interval = 1000 // 1초

    await icdStore.startSunTrack(interval, azimuthSpeed, elevationSpeed, trainSpeed)
    success('Sun Track이 시작되었습니다.')
  } catch (error) {
    handleApiError(error, 'Sun Track 시작')
  } finally {
    isGoLoading.value = false
  }
}

// Stop 명령 처리 함수 (Sun Track 중지)
const handleStopCommand = async () => {
  try {
    isStopLoading.value = true
    await icdStore.stopCommand(true, true, true)
    success('Sun Track이 중지되었습니다.')
  } catch (error) {
    handleApiError(error, 'Sun Track 중지')
  } finally {
    isStopLoading.value = false
  }
}

// Stow 명령 처리 함수
const handleStowCommand = async () => {
  try {
    isStowLoading.value = true
    await icdStore.stowCommand()
    success('Stow 명령이 전송되었습니다.')
  } catch (error) {
    handleApiError(error, 'Stow 명령')
  } finally {
    isStowLoading.value = false
  }
}
</script>

<style scoped>
/* SunTrackPage 레이아웃 → mode-common.scss로 통일됨 */

/* Offset Control Row */
.sun-track-mode .offset-control-row {
  margin-bottom: 0.5rem;
  position: relative;
  z-index: 100;
}

/* Main Content Row */
.main-content-row {
  display: flex;
  flex-wrap: nowrap;
  align-items: stretch;
  margin-bottom: 0;
  padding-bottom: 0;
}

/* Speed Section Card */
.speed-section-card {
  background-color: var(--theme-card-background);
  width: 100%;
  height: 100%;
  min-height: 367.19px;
  max-height: 367.19px;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

.speed-section-content {
  padding: 16px 16px 0 16px;
  min-height: 367.19px;
  max-height: 367.19px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow: visible;
  padding-left: 2rem;
  padding-right: 2rem;
}

.speed-section-title {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 10;
  margin: 0;
  padding: 0;
}

.speed-inputs-row {
  min-height: auto;
  max-height: none;
  height: auto;
  flex: 0 0 auto;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  margin-bottom: 0;
  padding: 0 2rem;
  padding-bottom: 0;
  box-sizing: border-box;
  overflow: visible;
  text-align: left;
  position: relative;
}

.speed-content-wrapper {
  width: 100%;
  max-width: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 0;
  padding-left: 2rem;
  padding-right: 2rem;
  box-sizing: border-box;
  margin: 0 auto;
  height: 100%;
}

.speed-description-text {
  margin-top: 0.5rem;
  margin-bottom: 0;
  flex-shrink: 0;
  color: var(--theme-text-secondary);
}

/* control-buttons-section 스타일은 ControlButtonBar 컴포넌트로 이동 */

.speed-input {
  width: 100%;
  min-width: 200px;
}

/* 숫자 입력 필드의 화살표 버튼 숨기기 */
:deep(input[type='number']::-webkit-inner-spin-button),
:deep(input[type='number']::-webkit-outer-spin-button) {
  -webkit-appearance: none;
  margin: 0;
}

:deep(input[type='number']) {
  -moz-appearance: textfield;
  appearance: textfield;
}
</style>
