<template>
  <div class="mode-shell pedestal-position-container">
    <div class="mode-shell__content">
      <q-card class="mode-card pedestal-card">
        <!-- 축별 제어 패널 (중앙 집중형으로 변경) -->
        <div class="pedestal-container">
          <div class="row q-col-gutter-md justify-center">
            <!-- Azimuth 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !pedestalStore.selectedAxes.azimuth }">
                <q-card-section>
                  <div class="axis-header q-mb-md">
                    <div class="checkbox-label-group">
                      <q-checkbox v-model="pedestalStore.selectedAxes.azimuth" color="primary" class="axis-checkbox" />
                      <div class="text-h6 text-primary axis-title">Azimuth</div>
                    </div>
                  </div>

                  <div class="text-subtitle2">Current Position</div>
                  <q-input v-model="currentPositions.azimuth" outlined readonly dense suffix="°" class="q-mb-sm" />

                  <div class="text-subtitle2">Target Position</div>
                  <q-input v-model="pedestalStore.targetPositions.azimuth" outlined dense type="number" suffix="°"
                    :disable="!pedestalStore.selectedAxes.azimuth" min="-360" max="360" step="0.01"
                    @update:model-value="formatTargetPosition('azimuth')" class="q-mb-sm" />

                  <div class="text-subtitle2">Target Speed</div>
                  <q-input v-model="pedestalStore.targetSpeeds.azimuth" outlined dense type="number" suffix="°/s"
                    :disable="!pedestalStore.selectedAxes.azimuth" min="7" max="173" step="0.01"
                    @update:model-value="formatTargetSpeed('azimuth')" />
                </q-card-section>
              </q-card>
            </div>

            <!-- Elevation 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !pedestalStore.selectedAxes.elevation }">
                <q-card-section>
                  <div class="axis-header q-mb-md">
                    <div class="checkbox-label-group">
                      <q-checkbox v-model="pedestalStore.selectedAxes.elevation" color="primary"
                        class="axis-checkbox" />
                      <div class="text-h6 text-primary axis-title">Elevation</div>
                    </div>
                  </div>

                  <div class="text-subtitle2">Current Position</div>
                  <q-input v-model="currentPositions.elevation" outlined readonly dense suffix="°" class="q-mb-sm" />

                  <div class="text-subtitle2">Target Position</div>
                  <q-input v-model="pedestalStore.targetPositions.elevation" outlined dense type="number" suffix="°"
                    :disable="!pedestalStore.selectedAxes.elevation" min="-360" max="360" step="0.01"
                    @update:model-value="formatTargetPosition('elevation')" class="q-mb-sm" />

                  <div class="text-subtitle2">Target Speed</div>
                  <q-input v-model="pedestalStore.targetSpeeds.elevation" outlined dense type="number" suffix="°/s"
                    :disable="!pedestalStore.selectedAxes.elevation" min="0" step="0.01"
                    @update:model-value="formatTargetSpeed('elevation')" />
                </q-card-section>
              </q-card>
            </div>

            <!-- Tilt 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !pedestalStore.selectedAxes.train }">
                <q-card-section>
                  <div class="axis-header q-mb-md">
                    <div class="checkbox-label-group">
                      <q-checkbox v-model="pedestalStore.selectedAxes.train" color="primary" class="axis-checkbox" />
                      <div class="text-h6 text-primary axis-title">Tilt</div>
                    </div>
                  </div>

                  <div class="text-subtitle2">Current Position</div>
                  <q-input v-model="currentPositions.train" outlined readonly dense suffix="°" class="q-mb-sm" />

                  <div class="text-subtitle2">Target Position</div>
                  <q-input v-model="pedestalStore.targetPositions.train" outlined dense type="number" suffix="°"
                    :disable="!pedestalStore.selectedAxes.train" min="-360" max="360" step="0.01"
                    @update:model-value="formatTargetPosition('train')" class="q-mb-sm" />

                  <div class="text-subtitle2">Target Speed</div>
                  <q-input v-model="pedestalStore.targetSpeeds.train" outlined dense type="number" suffix="°/s"
                    :disable="!pedestalStore.selectedAxes.train" min="-360" max="360" step="0.01"
                    @update:model-value="formatTargetSpeed('train')" />
                </q-card-section>
              </q-card>
            </div>
          </div>

          <!-- 제어 버튼 섹션 - 공용 컴포넌트 사용 -->
          <ControlButtonBar
            class="button-section"
            :go-disabled="!pedestalStore.isAnyAxisSelected()"
            :stop-disabled="!pedestalStore.isAnyAxisSelected()"
            :go-loading="isGoLoading"
            :stop-loading="isStopLoading"
            :stow-loading="isStowLoading"
            @go="handleGoCommand"
            @stop="handleStopCommand"
            @stow="handleStowCommand"
          />
        </div>

        <!-- 상태 메시지 -->
        <q-banner v-if="statusMessage" :class="statusClass" class="q-mt-md">
          {{ statusMessage }}
        </q-banner>
      </q-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { usePedestalPositionModeStore } from '@/stores'
import { useNotification } from '@/composables/useNotification'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { ControlButtonBar } from '@/components/common'

const icdStore = useICDStore()
const pedestalStore = usePedestalPositionModeStore()
const { success } = useNotification()
const { handleApiError } = useErrorHandler()

const isGoLoading = ref(false)
const isStopLoading = ref(false)
const isStowLoading = ref(false)

const currentPositions = ref({
  azimuth: '0.00',
  elevation: '0.00',
  train: '0.00',
})

const statusMessage = ref('')
const statusClass = ref('bg-positive text-white')

const formatTargetPosition = (axis: 'azimuth' | 'elevation' | 'train') => {
  let value = parseFloat(pedestalStore.targetPositions[axis])

  if (isNaN(value)) {
    value = 0
  }

  if (axis === 'elevation' && value < 0) {
    value = 0
  }

  pedestalStore.updateTargetPosition(axis, value.toFixed(2))
}

const formatTargetSpeed = (axis: 'azimuth' | 'elevation' | 'train') => {
  let value = parseFloat(pedestalStore.targetSpeeds[axis])
  if (isNaN(value) || value < 0) {
    value = 0
  }
  pedestalStore.updateTargetSpeed(axis, value.toFixed(2))
}

watch(
  () => icdStore.azimuthAngle,
  (newValue) => {
    if (newValue) {
      currentPositions.value.azimuth = parseFloat(newValue).toFixed(2)
    }
  },
)

watch(
  () => icdStore.elevationAngle,
  (newValue) => {
    if (newValue) {
      currentPositions.value.elevation = parseFloat(newValue).toFixed(2)
    }
  },
)

watch(
  () => icdStore.trainAngle,
  (newValue) => {
    if (newValue) {
      currentPositions.value.train = parseFloat(newValue).toFixed(2)
    }
  },
)

onMounted(() => {
  if (icdStore.azimuthAngle) {
    currentPositions.value.azimuth = parseFloat(icdStore.azimuthAngle).toFixed(2)
  }
  if (icdStore.elevationAngle) {
    currentPositions.value.elevation = parseFloat(icdStore.elevationAngle).toFixed(2)
  }
  if (icdStore.trainAngle) {
    currentPositions.value.train = parseFloat(icdStore.trainAngle).toFixed(2)
  }
})

const handleGoCommand = async () => {
  try {
    isGoLoading.value = true

    // ✅ 각 축별로 체크 여부에 따라 각도와 속도 설정
    let azAngle: number
    let azSpeed: number
    let elAngle: number
    let elSpeed: number
    let trainAngle: number
    let trainSpeed: number

    if (pedestalStore.selectedAxes.azimuth) {
      // ✅ 체크된 경우: 입력한 목표 각도와 속도 사용
      azAngle = parseFloat(pedestalStore.targetPositions.azimuth)
      azSpeed = parseFloat(pedestalStore.targetSpeeds.azimuth)
      console.log(`Azimuth 체크됨 - 목표 각도: ${azAngle}, 속도: ${azSpeed}`)
    } else {
      // ✅ 체크되지 않은 경우: 현재 CMD 각도 유지, 속도는 0
      azAngle = parseFloat(icdStore.cmdAzimuthAngle) || 0
      azSpeed = 0
      console.log(`Azimuth 체크 해제 - 현재 CMD 값 유지: ${azAngle}, 속도: 0`)
    }

    if (pedestalStore.selectedAxes.elevation) {
      // ✅ 체크된 경우: 입력한 목표 각도와 속도 사용
      elAngle = parseFloat(pedestalStore.targetPositions.elevation)
      elSpeed = parseFloat(pedestalStore.targetSpeeds.elevation)
      console.log(`Elevation 체크됨 - 목표 각도: ${elAngle}, 속도: ${elSpeed}`)
    } else {
      // ✅ 체크되지 않은 경우: 현재 CMD 각도 유지, 속도는 0
      elAngle = parseFloat(icdStore.cmdElevationAngle) || 0
      elSpeed = 0
      console.log(`Elevation 체크 해제 - 현재 CMD 값 유지: ${elAngle}, 속도: 0`)
    }

    if (pedestalStore.selectedAxes.train) {
      // ✅ 체크된 경우: 입력한 목표 각도와 속도 사용
      trainAngle = parseFloat(pedestalStore.targetPositions.train)
      trainSpeed = parseFloat(pedestalStore.targetSpeeds.train)
      console.log(`Train 체크됨 - 목표 각도: ${trainAngle}, 속도: ${trainSpeed}`)
    } else {
      // ✅ 체크되지 않은 경우: 현재 CMD 각도 유지, 속도는 0
      trainAngle = parseFloat(icdStore.cmdTrainAngle) || 0
      trainSpeed = 0
      console.log(`Train 체크 해제 - 현재 CMD 값 유지: ${trainAngle}, 속도: 0`)
    }

    await icdStore.sendMultiControlCommand({
      azimuth: pedestalStore.selectedAxes.azimuth,
      elevation: pedestalStore.selectedAxes.elevation,
      train: pedestalStore.selectedAxes.train,
      azAngle,
      azSpeed,
      elAngle,
      elSpeed,
      trainAngle,
      trainSpeed,
    })

    statusMessage.value = '위치 명령이 성공적으로 전송되었습니다.'
    statusClass.value = 'bg-positive text-white'
    success('위치 명령이 성공적으로 전송되었습니다.')
  } catch (error) {
    statusMessage.value = '위치 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'
    handleApiError(error, '위치 명령')
  } finally {
    isGoLoading.value = false

    setTimeout(() => {
      statusMessage.value = ''
    }, 3000)
  }
}

const handleStopCommand = async () => {
  try {
    isStopLoading.value = true

    await icdStore.stopCommand(
      pedestalStore.selectedAxes.azimuth,
      pedestalStore.selectedAxes.elevation,
      pedestalStore.selectedAxes.train,
    )

    statusMessage.value = '정지 명령이 성공적으로 전송되었습니다.'
    statusClass.value = 'bg-positive text-white'
    success('정지 명령이 성공적으로 전송되었습니다.')
  } catch (error) {
    statusMessage.value = '정지 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'
    handleApiError(error, '정지 명령')
  } finally {
    isStopLoading.value = false

    setTimeout(() => {
      statusMessage.value = ''
    }, 3000)
  }
}
const handleStowCommand = async () => {
  try {
    isStowLoading.value = true

    await icdStore.stowCommand()

    statusMessage.value = 'Stow 명령이 성공적으로 전송되었습니다.'
    statusClass.value = 'bg-positive text-white'
    success('Stow 명령이 성공적으로 전송되었습니다.')
  } catch (error) {
    statusMessage.value = 'Stow 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'
    handleApiError(error, 'Stow 명령')
  } finally {
    isStowLoading.value = false

    setTimeout(() => {
      statusMessage.value = ''
    }, 3000)
  }
}
</script>

<style scoped>
.pedestal-position-container {
  width: 100%;
  /* ✅ height: 100% 제거 - mode-shell의 높이 설정 사용 */
  /* ✅ justify-content와 align-items는 mode-shell__content에서 처리 */
}

/* 섹션 제목 → mode-common.scss로 통일됨 */

/* 중앙 집중형 컨테이너 */
.pedestal-container {
  padding: 0.25rem 0.5rem;
  /* 상단 패딩을 0.5rem → 0.25rem으로 줄임 */
  /* 상단/좌우 패딩만 유지하고 하단은 버튼 카드 마진으로 처리 */
  width: 100%;
  height: auto;
  /* height를 auto로 변경하여 내용에 맞게 조정 */
  max-width: 1200px;
  /* PedestalPosition과 동일한 너비 */
  margin: 0 auto;
  /* 중앙 정렬 */
}

/* 축 패널 스타일 - 공간 최적화 */
/* ✅ 테두리는 mode-common.scss의 .axis-panel에서 통일 관리 */
.axis-panel {
  background-color: var(--theme-card-background);
  /* ✅ border, border-radius, box-shadow는 mode-common.scss에서 통일 관리 */
  height: 100%;
  min-height: 200px;
  /* 높이 줄임 */
  transition: opacity 0.3s, filter 0.3s;
}

/* 카드 섹션 패딩 - PedestalPositionPage 커스텀 */
.axis-panel .q-card-section {
  padding: 0.1rem 0.8rem 1.2rem 0.8rem;
  /* 상단 패딩을 최소화하고 하단 패딩을 최대화해서 위아래 균형 맞춤 */
}

/* 체크박스/라벨 그룹 - PedestalPositionPage 커스텀 */
.checkbox-label-group {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  width: 100%;
  margin: 0;
  margin-top: -0.5rem;
  /* 체크박스 그룹을 위로 올림 */
}

/* 축 헤더 - PedestalPositionPage 커스텀 */
.axis-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.2rem;
  margin-top: -0.5rem;
  /* 헤더를 위로 올림 */
  width: 100%;
}

/* 체크박스 위 공간 - PedestalPositionPage 커스텀 */
.axis-checkbox {
  margin: 0.5rem 0 0 0;
  flex-shrink: 0;
}

.disabled-panel {
  opacity: 0.7;
  filter: grayscale(30%);
}

.axis-title {
  margin: 0;
  flex: 1;
}

/* 입력 필드 마진 - PedestalPositionPage 커스텀 */
.axis-panel .q-field {
  margin-bottom: 0;
}

.axis-panel .q-field:last-child {
  margin-bottom: 0;
}

/* 라벨 마진 - PedestalPositionPage 커스텀 */
.axis-panel .text-subtitle2 {
  margin-bottom: 0.2rem;
}

.axis-panel .text-subtitle2:first-of-type {
  margin-top: 0;
}

/* 버튼 섹션 스타일 */
.button-section {
  margin-top: 1.5rem;
  background: transparent;
  border: none;
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

/* 모바일 화면에서는 카드 간격 조정 */
@media (max-width: 768px) {
  .col-12 {
    margin-bottom: 16px;
  }
}
</style>
