<template>
  <div class="pedestal-position-container">
    <div class="section-title text-h5 text-primary q-mb-sm">Pedestal Position Control</div>

    <!-- 축별 제어 패널 (가로 배치) -->
    <div class="row q-col-gutter-md">
      <!-- Azimuth 패널 -->
      <div class="col-12 col-md-4">
        <q-card class="axis-panel" :class="{ 'disabled-panel': !pedestalStore.selectedAxes.azimuth }">
          <q-card-section>
            <div class="axis-header q-mb-md">
              <q-checkbox v-model="pedestalStore.selectedAxes.azimuth" color="primary" class="axis-checkbox" />
              <div class="text-h6 text-primary axis-title">Azimuth</div>
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
              <q-checkbox v-model="pedestalStore.selectedAxes.elevation" color="primary" class="axis-checkbox" />
              <div class="text-h6 text-primary axis-title">Elevation</div>
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
              <q-checkbox v-model="pedestalStore.selectedAxes.train" color="primary" class="axis-checkbox" />
              <div class="text-h6 text-primary axis-title">Tilt</div>
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
    <q-card class="q-mt-md">
      <q-card-section>
        <div class="row justify-center q-gutter-md">
          <q-btn label="Go" color="positive" icon="play_arrow" size="lg" :disable="!pedestalStore.isAnyAxisSelected()"
            @click="handleGoCommand" :loading="isGoLoading" />
          <q-btn label="Stop" color="negative" icon="stop" size="lg" :disable="!pedestalStore.isAnyAxisSelected()"
            @click="handleStopCommand" :loading="isStopLoading" />
          <q-btn label="Stow" color="primary" icon="home" size="lg" @click="handleStowCommand"
            :loading="isStowLoading" />
        </div>
      </q-card-section>
    </q-card>

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
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
}

.axis-panel {
  height: 100%;
  transition:
    opacity 0.3s,
    filter 0.3s;
}

.disabled-panel {
  opacity: 0.7;
  filter: grayscale(30%);
}

.axis-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.axis-checkbox {
  margin: 0;
  flex-shrink: 0;
}

.axis-title {
  margin: 0;
  flex: 1;
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
