<template>
  <div class="pedestal-position-container">
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
                  <q-checkbox v-model="pedestalStore.selectedAxes.elevation" color="primary" class="axis-checkbox" />
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

      <!-- 제어 버튼 섹션 -->
      <div class="button-section">
        <div class="row justify-center q-gutter-md">
          <q-btn label="Go" color="positive" icon="play_arrow" size="lg" :disable="!pedestalStore.isAnyAxisSelected()"
            @click="handleGoCommand" :loading="isGoLoading" />
          <q-btn label="Stop" color="negative" icon="stop" size="lg" :disable="!pedestalStore.isAnyAxisSelected()"
            @click="handleStopCommand" :loading="isStopLoading" />
          <q-btn label="Stow" color="warning" icon="home" size="lg" @click="handleStowCommand"
            :loading="isStowLoading" />
        </div>
      </div>
    </div>

    <!-- 상태 메시지 -->
    <q-banner v-if="statusMessage" :class="statusClass" class="q-mt-md">
      {{ statusMessage }}
    </q-banner>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { usePedestalPositionModeStore } from '@/stores'
import { useQuasar } from 'quasar'

interface CommandParams {
  azimuthAngle?: number
  azimuthSpeed?: number
  elevationAngle?: number
  elevationSpeed?: number
  trainAngle?: number
  trainSpeed?: number
  [key: string]: number | undefined
}

const $q = useQuasar()
const icdStore = useICDStore()
const pedestalStore = usePedestalPositionModeStore()

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

    const command: CommandParams = {}

    if (pedestalStore.selectedAxes.azimuth) {
      command.azimuthAngle = parseFloat(pedestalStore.targetPositions.azimuth)
      command.azimuthSpeed = parseFloat(pedestalStore.targetSpeeds.azimuth)
    }

    if (pedestalStore.selectedAxes.elevation) {
      command.elevationAngle = parseFloat(pedestalStore.targetPositions.elevation)
      command.elevationSpeed = parseFloat(pedestalStore.targetSpeeds.elevation)
    }

    if (pedestalStore.selectedAxes.train) {
      command.trainAngle = parseFloat(pedestalStore.targetPositions.train)
      command.trainSpeed = parseFloat(pedestalStore.targetSpeeds.train)
    }

    await icdStore.sendMultiControlCommand({
      azimuth: pedestalStore.selectedAxes.azimuth,
      elevation: pedestalStore.selectedAxes.elevation,
      train: pedestalStore.selectedAxes.train,
      azAngle: command.azimuthAngle ?? 0,
      azSpeed: command.azimuthSpeed ?? 0,
      elAngle: command.elevationAngle ?? 0,
      elSpeed: command.elevationSpeed ?? 0,
      trainAngle: command.trainAngle ?? 0,
      trainSpeed: command.trainSpeed ?? 0,
    })

    statusMessage.value = '위치 명령이 성공적으로 전송되었습니다.'
    statusClass.value = 'bg-positive text-white'

    $q.notify({
      type: 'positive',
      message: '위치 명령이 성공적으로 전송되었습니다.',
      timeout: 2000,
    })
  } catch (error) {
    console.error('위치 명령 전송 중 오류:', error)

    statusMessage.value = '위치 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'

    $q.notify({
      type: 'negative',
      message: '위치 명령 전송 중 오류가 발생했습니다.',
      timeout: 2000,
    })
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

    $q.notify({
      type: 'positive',
      message: '정지 명령이 성공적으로 전송되었습니다.',
      timeout: 2000,
    })
  } catch (error) {
    console.error('정지 명령 전송 중 오류:', error)

    statusMessage.value = '정지 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'

    $q.notify({
      type: 'negative',
      message: '정지 명령 전송 중 오류가 발생했습니다.',
      timeout: 2000,
    })
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

    $q.notify({
      type: 'positive',
      message: 'Stow 명령이 성공적으로 전송되었습니다.',
      timeout: 2000,
    })
  } catch (error) {
    console.error('Stow 명령 전송 중 오류:', error)

    statusMessage.value = 'Stow 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'

    $q.notify({
      type: 'negative',
      message: 'Stow 명령 전송 중 오류가 발생했습니다.',
      timeout: 2000,
    })
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
  height: 100%;
  width: 100%;
}

/* 섹션 제목 간격 최소화 */
.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
  margin-bottom: 0.5rem !important;
  /* 마진 줄임 */
}

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
.axis-panel {
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
  height: 100%;
  min-height: 200px;
  /* 높이 줄임 */
  transition: opacity 0.3s, filter 0.3s;
}

/* 카드 섹션 패딩 최소화 */
.axis-panel .q-card-section {
  padding: 0.1rem 0.8rem 1.2rem 0.8rem !important;
  /* 상단 패딩을 최소화하고 하단 패딩을 최대화해서 위아래 균형 맞춤 */
}

/* 체크박스와 라벨을 함께 가운데 정렬하는 그룹 */
.checkbox-label-group {
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
  justify-content: center !important;
  /* 체크박스와 라벨을 함께 가운데 정렬 */
  width: 100% !important;
  margin: 0 !important;
  /* 모든 마진 제거 */
  margin-top: -0.5rem !important;
  /* 체크박스 그룹을 위로 올림 */
}

/* 축 헤더 간격 최소화 */
.axis-header {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  /* 헤더 전체를 가운데 정렬 */
  margin-bottom: 0.2rem !important;
  /* 체크박스와 Speed 간격을 위아래 동일하게 맞춤 */
  margin-top: -0.5rem !important;
  /* 헤더를 위로 올림 */
  width: 100% !important;
  /* 전체 너비 사용 */
}

/* 체크박스 위 공간 늘리기 */
.axis-checkbox {
  margin: 0.5rem 0 0 0 !important;
  /* 체크박스 위 공간을 늘림 */
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

/* 입력 필드 마진 최소화 */
.axis-panel .q-field {
  margin-bottom: 0 !important;
  /* Speed 아래 공간을 완전히 제거 */
}

.axis-panel .q-field:last-child {
  margin-bottom: 0 !important;
  /* 마지막 필드는 하단 마진 없음 */
}

/* 라벨 마진 최소화 */
.axis-panel .text-subtitle2 {
  margin-bottom: 0.2rem !important;
  /* 라벨 하단 마진 최소화 */
}

.axis-panel .text-subtitle2:first-of-type {
  margin-top: 0 !important;
  /* 첫 번째 라벨은 상단 마진 없음 */
}

/* 버튼 섹션 스타일 */
.button-section {
  margin-top: 1.5rem;
  /* 버튼 섹션 상단 마진 */
  background: transparent;
  /* 배경색 제거 */
  border: none;
}

/* 버튼 크기 통일 */
.button-section .q-btn {
  min-width: 150px !important;
  /* 최소 너비를 150px로 설정 */
  width: 150px !important;
  /* 고정 너비 150px */
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
