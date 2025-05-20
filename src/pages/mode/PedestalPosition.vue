<template>
  <div class="pedestal-position-container">
    <div class="section-title text-h5 text-primary q-mb-sm">Pedestal Position Control</div>

    <!-- 축 선택 체크박스 -->
    <q-card class="q-mb-md">
      <q-card-section>
        <div class="row items-center q-gutter-md">
          <q-checkbox v-model="selectedAxes.azimuth" label="Azimuth" color="primary" />
          <q-checkbox v-model="selectedAxes.elevation" label="Elevation" color="primary" />
          <q-checkbox v-model="selectedAxes.tilt" label="Tilt" color="primary" />
        </div>
      </q-card-section>
    </q-card>

    <!-- 축별 제어 패널 (가로 배치) -->
    <div class="row q-col-gutter-md">
      <!-- Azimuth 패널 -->
      <div class="col-12 col-md-4">
        <q-card class="axis-panel" :class="{ 'disabled-panel': !selectedAxes.azimuth }">
          <q-card-section>
            <div class="text-h6 text-primary">Azimuth</div>

            <div class="q-mt-md">
              <div class="text-subtitle2">Current Position</div>
              <q-input
                v-model="currentPositions.azimuth"
                outlined
                readonly
                dense
                suffix="°"
                class="q-mb-sm"
              />

              <div class="text-subtitle2">Target Position</div>
              <q-input
                v-model="targetPositions.azimuth"
                outlined
                dense
                type="number"
                suffix="°"
                :disable="!selectedAxes.azimuth"
                min="-360"
                max="360"
                step="0.01"
                @update:model-value="formatTargetPosition('azimuth')"
                class="q-mb-sm"
              />

              <div class="text-subtitle2">Target Speed</div>
              <q-input
                v-model="targetSpeeds.azimuth"
                outlined
                dense
                type="number"
                suffix="°/s"
                :disable="!selectedAxes.azimuth"
                min="7"
                max="173"
                step="0.01"
                @update:model-value="formatTargetSpeed('azimuth')"
              />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Elevation 패널 -->
      <div class="col-12 col-md-4">
        <q-card class="axis-panel" :class="{ 'disabled-panel': !selectedAxes.elevation }">
          <q-card-section>
            <div class="text-h6 text-primary">Elevation</div>

            <div class="q-mt-md">
              <div class="text-subtitle2">Current Position</div>
              <q-input
                v-model="currentPositions.elevation"
                outlined
                readonly
                dense
                suffix="°"
                class="q-mb-sm"
              />

              <div class="text-subtitle2">Target Position</div>
              <q-input
                v-model="targetPositions.elevation"
                outlined
                dense
                type="number"
                suffix="°"
                :disable="!selectedAxes.elevation"
                min="-360"
                max="360"
                step="0.01"
                @update:model-value="formatTargetPosition('elevation')"
                class="q-mb-sm"
              />
              <div class="text-subtitle2">Target Speed</div>
              <q-input
                v-model="targetSpeeds.elevation"
                outlined
                dense
                type="number"
                suffix="°/s"
                :disable="!selectedAxes.elevation"
                min="0"
                step="0.01"
                @update:model-value="formatTargetSpeed('elevation')"
              />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Tilt 패널 -->
      <div class="col-12 col-md-4">
        <q-card class="axis-panel" :class="{ 'disabled-panel': !selectedAxes.tilt }">
          <q-card-section>
            <div class="text-h6 text-primary">Tilt</div>

            <div class="q-mt-md">
              <div class="text-subtitle2">Current Position</div>
              <q-input
                v-model="currentPositions.tilt"
                outlined
                readonly
                dense
                suffix="°"
                class="q-mb-sm"
              />

              <div class="text-subtitle2">Target Position</div>
              <q-input
                v-model="targetPositions.tilt"
                outlined
                dense
                type="number"
                suffix="°"
                :disable="!selectedAxes.tilt"
                min="-360"
                max="360"
                step="0.01"
                @update:model-value="formatTargetPosition('tilt')"
                class="q-mb-sm"
              />

              <div class="text-subtitle2">Target Speed</div>
              <q-input
                v-model="targetSpeeds.tilt"
                outlined
                dense
                type="number"
                suffix="°"
                :disable="!selectedAxes.tilt"
                min="-360"
                max="360"
                step="0.01"
                @update:model-value="formatTargetPosition('tilt')"
                class="q-mb-sm"
              />
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- 제어 버튼 섹션 -->
    <q-card class="q-mt-md">
      <q-card-section>
        <div class="row justify-center q-gutter-md">
          <q-btn
            label="Go"
            color="positive"
            icon="play_arrow"
            size="lg"
            :disable="!isAnyAxisSelected"
            @click="handleGoCommand"
            :loading="isGoLoading"
          />
          <q-btn
            label="Stop"
            color="negative"
            icon="stop"
            size="lg"
            :disable="!isAnyAxisSelected"
            @click="handleStopCommand"
            :loading="isStopLoading"
          />
          <q-btn
            label="Stow"
            color="primary"
            icon="home"
            size="lg"
            @click="handleStowCommand"
            :loading="isStowLoading"
          />
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
import { ref, computed, watch, onMounted } from 'vue'
import { useICDStore } from '../../stores/API/icdStore'
import { useQuasar } from 'quasar'

// 커맨드 파라미터 인터페이스 정의
interface CommandParams {
  azimuthAngle?: number
  azimuthSpeed?: number
  elevationAngle?: number
  elevationSpeed?: number
  tiltAngle?: number
  tiltSpeed?: number
  [key: string]: number | undefined
}

const $q = useQuasar()
const icdStore = useICDStore()

// 로딩 상태
const isGoLoading = ref(false)
const isStopLoading = ref(false)
const isStowLoading = ref(false)

// 선택된 축
const selectedAxes = ref({
  azimuth: true,
  elevation: true,
  tilt: true,
})

// 현재 위치 (실시간 업데이트)
const currentPositions = ref({
  azimuth: '0.00',
  elevation: '0.00',
  tilt: '0.00',
})

// 목표 위치
const targetPositions = ref({
  azimuth: '0.00',
  elevation: '0.00',
  tilt: '0.00',
})

// 목표 속도
const targetSpeeds = ref({
  azimuth: '0.50',
  elevation: '0.50',
  tilt: '0.50',
})

// 상태 메시지
const statusMessage = ref('')
const statusClass = ref('bg-positive text-white')

// 적어도 하나의 축이 선택되었는지 확인
const isAnyAxisSelected = computed(() => {
  return selectedAxes.value.azimuth || selectedAxes.value.elevation || selectedAxes.value.tilt
})

// 목표 위치 값 포맷팅 (소수점 2자리까지)
const formatTargetPosition = (axis: 'azimuth' | 'elevation' | 'tilt') => {
  let value = parseFloat(targetPositions.value[axis])

  if (isNaN(value)) {
    value = 0
  }

  // Elevation은 음수 값을 0으로 변환
  if (axis === 'elevation' && value < 0) {
    value = 0
  }

  targetPositions.value[axis] = value.toFixed(2)
}
// 목표 속도 값 포맷팅 (소수점 2자리까지, 양수만)
const formatTargetSpeed = (axis: 'azimuth' | 'elevation' | 'tilt') => {
  let value = parseFloat(targetSpeeds.value[axis])
  if (isNaN(value) || value < 0) {
    value = 0
  }
  targetSpeeds.value[axis] = value.toFixed(2)
}

// ICD 스토어의 값 변경 감지하여 현재 위치 업데이트
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
  () => icdStore.tiltAngle,
  (newValue) => {
    if (newValue) {
      currentPositions.value.tilt = parseFloat(newValue).toFixed(2)
    }
  },
)

// 초기값 설정
onMounted(() => {
  if (icdStore.azimuthAngle) {
    currentPositions.value.azimuth = parseFloat(icdStore.azimuthAngle).toFixed(2)
  }
  if (icdStore.elevationAngle) {
    currentPositions.value.elevation = parseFloat(icdStore.elevationAngle).toFixed(2)
  }
  if (icdStore.tiltAngle) {
    currentPositions.value.tilt = parseFloat(icdStore.tiltAngle).toFixed(2)
  }

  // 초기 값 포맷팅
  formatTargetPosition('azimuth')
  formatTargetPosition('elevation')
  formatTargetPosition('tilt')
  formatTargetSpeed('azimuth')
  formatTargetSpeed('elevation')
  formatTargetSpeed('tilt')
})

// Go 명령 처리 (선택된 축을 입력된 각도와 스피드로 제어)
const handleGoCommand = async () => {
  try {
    isGoLoading.value = true

    // 선택된 축에 대해서만 명령 전송
    const command: CommandParams = {}

    if (selectedAxes.value.azimuth) {
      command.azimuthAngle = parseFloat(targetPositions.value.azimuth)
      command.azimuthSpeed = parseFloat(targetSpeeds.value.azimuth)
    }

    if (selectedAxes.value.elevation) {
      command.elevationAngle = parseFloat(targetPositions.value.elevation)
      command.elevationSpeed = parseFloat(targetSpeeds.value.elevation)
    }

    if (selectedAxes.value.tilt) {
      command.tiltAngle = parseFloat(targetPositions.value.tilt)
      command.tiltSpeed = parseFloat(targetSpeeds.value.tilt)
    }

    // API 호출
    await icdStore.sendMultiControlCommand({
      azimuth: selectedAxes.value.azimuth,
      elevation: selectedAxes.value.elevation,
      tilt: selectedAxes.value.tilt,
      azAngle: command.azimuthAngle ?? 0,
      azSpeed: command.azimuthSpeed ?? 0,
      elAngle: command.elevationAngle ?? 0,
      elSpeed: command.elevationSpeed ?? 0,
      tiAngle: command.tiltAngle ?? 0,
      tiSpeed: command.tiltSpeed ?? 0,
    })

    // 성공 메시지 표시
    statusMessage.value = '위치 명령이 성공적으로 전송되었습니다.'
    statusClass.value = 'bg-positive text-white'

    $q.notify({
      type: 'positive',
      message: '위치 명령이 성공적으로 전송되었습니다.',
      timeout: 2000,
    })
  } catch (error) {
    console.error('위치 명령 전송 중 오류:', error)

    // 오류 메시지 표시
    statusMessage.value = '위치 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'

    $q.notify({
      type: 'negative',
      message: '위치 명령 전송 중 오류가 발생했습니다.',
      timeout: 2000,
    })
  } finally {
    isGoLoading.value = false

    // 3초 후 상태 메시지 숨기기
    setTimeout(() => {
      statusMessage.value = ''
    }, 3000)
  }
}

const handleStopCommand = async () => {
  try {
    isStopLoading.value = true

    console.log('정지 명령 요청:', {
      azimuth: selectedAxes.value.azimuth,
      elevation: selectedAxes.value.elevation,
      tilt: selectedAxes.value.tilt,
    })

    // 선택된 축에 대해서만 정지 명령 전송
    const response = await icdStore.stopCommand(
      selectedAxes.value.azimuth,
      selectedAxes.value.elevation,
      selectedAxes.value.tilt,
    )

    console.log('정지 명령 응답:', response)

    // 성공 메시지 표시
    statusMessage.value = '정지 명령이 성공적으로 전송되었습니다.'
    statusClass.value = 'bg-positive text-white'

    $q.notify({
      type: 'positive',
      message: '정지 명령이 성공적으로 전송되었습니다.',
      timeout: 2000,
    })
  } catch (error) {
    console.error('정지 명령 전송 중 오류:', error)

    // 오류 메시지 표시
    statusMessage.value = '정지 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'

    $q.notify({
      type: 'negative',
      message: '정지 명령 전송 중 오류가 발생했습니다.',
      timeout: 2000,
    })
  } finally {
    isStopLoading.value = false

    // 3초 후 상태 메시지 숨기기
    setTimeout(() => {
      statusMessage.value = ''
    }, 3000)
  }
}

// Stow 명령 처리
const handleStowCommand = async () => {
  try {
    isStowLoading.value = true

    // Stow 명령 전송
    await icdStore.stowCommand()

    // 성공 메시지 표시
    statusMessage.value = 'Stow 명령이 성공적으로 전송되었습니다.'
    statusClass.value = 'bg-positive text-white'

    $q.notify({
      type: 'positive',
      message: 'Stow 명령이 성공적으로 전송되었습니다.',
      timeout: 2000,
    })
  } catch (error) {
    console.error('Stow 명령 전송 중 오류:', error)

    // 오류 메시지 표시
    statusMessage.value = 'Stow 명령 전송 중 오류가 발생했습니다.'
    statusClass.value = 'bg-negative text-white'

    $q.notify({
      type: 'negative',
      message: 'Stow 명령 전송 중 오류가 발생했습니다.',
      timeout: 2000,
    })
  } finally {
    isStowLoading.value = false

    // 3초 후 상태 메시지 숨기기
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
