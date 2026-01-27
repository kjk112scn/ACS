<template>
  <div class="speed-limits-settings">
    <h5 class="q-mt-none q-mb-md">
      속도 제한 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
    </h5>

    <q-form class="q-gutter-md">
      <!-- Azimuth 속도 제한 -->
      <div class="row q-gutter-md">
        <div class="col">
          <q-input v-model.number="localSettings.azimuthMin" label="Azimuth 최소속도 (도/초)" type="number"
            :rules="azimuthMinRules" outlined :loading="loadingStates.speedLimits" hint="방위각 최소 속도" suffix="°/s" />
        </div>
        <div class="col">
          <q-input v-model.number="localSettings.azimuthMax" label="Azimuth 최대속도 (도/초)" type="number"
            :rules="azimuthMaxRules" outlined :loading="loadingStates.speedLimits" hint="방위각 최대 속도" suffix="°/s" />
        </div>
      </div>

      <!-- Elevation 속도 제한 -->
      <div class="row q-gutter-md">
        <div class="col">
          <q-input v-model.number="localSettings.elevationMin" label="Elevation 최소속도 (도/초)" type="number"
            :rules="elevationMinRules" outlined :loading="loadingStates.speedLimits" hint="고도각 최소 속도" suffix="°/s" />
        </div>
        <div class="col">
          <q-input v-model.number="localSettings.elevationMax" label="Elevation 최대속도 (도/초)" type="number"
            :rules="elevationMaxRules" outlined :loading="loadingStates.speedLimits" hint="고도각 최대 속도" suffix="°/s" />
        </div>
      </div>

      <!-- Train 속도 제한 -->
      <div class="row q-gutter-md">
        <div class="col">
          <q-input v-model.number="localSettings.trainMin" label="Train 최소속도 (도/초)" type="number" :rules="trainMinRules"
            outlined :loading="loadingStates.speedLimits" hint="Train각 최소 속도" suffix="°/s" />
        </div>
        <div class="col">
          <q-input v-model.number="localSettings.trainMax" label="Train 최대속도 (도/초)" type="number" :rules="trainMaxRules"
            outlined :loading="loadingStates.speedLimits" hint="Train각 최대 속도" suffix="°/s" />
        </div>
      </div>

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn color="secondary" label="초기화" @click="onReset" :disable="loadingStates.speedLimits" icon="refresh" />
      </div>
    </q-form>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.speedLimits" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.speedLimits }}
    </q-banner>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useSpeedLimitsSettingsStore } from '@/stores'
import type { SpeedLimitsSettings } from '@/services'

const $q = useQuasar()
const speedLimitsSettingsStore = useSpeedLimitsSettingsStore()

// 스토어 상태 가져오기
const { loadingStates, errorStates, updateChangeStatus, pendingChanges } = speedLimitsSettingsStore

// 로컬 상태 - Store에서 변경된 값이 있으면 사용, 없으면 초기값
const getInitialLocalSettings = (): SpeedLimitsSettings => {
  if (pendingChanges) {
    return { ...pendingChanges }
  }
  return {
    azimuthMin: speedLimitsSettingsStore.speedLimitsSettings.azimuthMin || 0.1,
    azimuthMax: speedLimitsSettingsStore.speedLimitsSettings.azimuthMax || 15.0,
    elevationMin: speedLimitsSettingsStore.speedLimitsSettings.elevationMin || 0.1,
    elevationMax: speedLimitsSettingsStore.speedLimitsSettings.elevationMax || 10.0,
    trainMin: speedLimitsSettingsStore.speedLimitsSettings.trainMin || 0.1,
    trainMax: speedLimitsSettingsStore.speedLimitsSettings.trainMax || 5.0
  }
}

const localSettings = ref<SpeedLimitsSettings>(getInitialLocalSettings())

// 원본 상태 - Store에서 초기값 가져오기
const originalSettings = ref<SpeedLimitsSettings>({
  azimuthMin: speedLimitsSettingsStore.speedLimitsSettings.azimuthMin || 0.1,
  azimuthMax: speedLimitsSettingsStore.speedLimitsSettings.azimuthMax || 15.0,
  elevationMin: speedLimitsSettingsStore.speedLimitsSettings.elevationMin || 0.1,
  elevationMax: speedLimitsSettingsStore.speedLimitsSettings.elevationMax || 10.0,
  trainMin: speedLimitsSettingsStore.speedLimitsSettings.trainMin || 0.1,
  trainMax: speedLimitsSettingsStore.speedLimitsSettings.trainMax || 5.0
})

// 변경사항 상태를 로컬 상태로 직접 계산
const hasUnsavedChanges = computed(() => {
  return JSON.stringify(localSettings.value) !== JSON.stringify(originalSettings.value)
})

// 유효성 검사 규칙
const azimuthMinRules = [
  (val: number) => val !== null && val !== undefined || 'Azimuth 최소속도는 필수입니다',
  (val: number) => val > 0 || 'Azimuth 최소속도는 0보다 커야 합니다',
  (val: number) => val <= 100 || 'Azimuth 최소속도는 100도/초 이하여야 합니다'
]

const azimuthMaxRules = [
  (val: number) => val !== null && val !== undefined || 'Azimuth 최대속도는 필수입니다',
  (val: number) => val > 0 || 'Azimuth 최대속도는 0보다 커야 합니다',
  (val: number) => val <= 100 || 'Azimuth 최대속도는 100도/초 이하여야 합니다',
  (val: number) => val >= localSettings.value.azimuthMin || 'Azimuth 최대속도는 최소속도보다 크거나 같아야 합니다'
]

const elevationMinRules = [
  (val: number) => val !== null && val !== undefined || 'Elevation 최소속도는 필수입니다',
  (val: number) => val > 0 || 'Elevation 최소속도는 0보다 커야 합니다',
  (val: number) => val <= 100 || 'Elevation 최소속도는 100도/초 이하여야 합니다'
]

const elevationMaxRules = [
  (val: number) => val !== null && val !== undefined || 'Elevation 최대속도는 필수입니다',
  (val: number) => val > 0 || 'Elevation 최대속도는 0보다 커야 합니다',
  (val: number) => val <= 100 || 'Elevation 최대속도는 100도/초 이하여야 합니다',
  (val: number) => val >= localSettings.value.elevationMin || 'Elevation 최대속도는 최소속도보다 크거나 같아야 합니다'
]

const trainMinRules = [
  (val: number) => val !== null && val !== undefined || 'Train 최소속도는 필수입니다',
  (val: number) => val > 0 || 'Train 최소속도는 0보다 커야 합니다',
  (val: number) => val <= 100 || 'Train 최소속도는 100도/초 이하여야 합니다'
]

const trainMaxRules = [
  (val: number) => val !== null && val !== undefined || 'Train 최대속도는 필수입니다',
  (val: number) => val > 0 || 'Train 최대속도는 0보다 커야 합니다',
  (val: number) => val <= 100 || 'Train 최대속도는 100도/초 이하여야 합니다',
  (val: number) => val >= localSettings.value.trainMin || 'Train 최대속도는 최소속도보다 크거나 같아야 합니다'
]

// 변경사항 감지 watch - Store 상태 업데이트
watch(
  localSettings,
  (newSettings) => {
    const hasChanges = JSON.stringify(newSettings) !== JSON.stringify(originalSettings.value)
    updateChangeStatus(hasChanges, newSettings)
  },
  { deep: true }
)

// 스토어 상태와 로컬 상태 동기화 (변경사항이 있을 때는 절대 덮어쓰지 않음)
watch(
  () => speedLimitsSettingsStore.speedLimitsSettings,
  (newSettings) => {
    // 변경사항이 있을 때는 절대 서버 값으로 덮어쓰지 않음
    if (hasUnsavedChanges.value) {
      console.log('변경사항이 있어서 서버 값으로 덮어쓰지 않음')
      return
    }

    // 변경사항이 없을 때만 서버 값으로 동기화
    localSettings.value = { ...newSettings }
    originalSettings.value = { ...newSettings }
  },
  { deep: true, immediate: true }
)

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  try {
    // 변경사항이 있을 때는 서버에서 로드하지 않음
    if (hasUnsavedChanges.value) {
      console.log('변경사항이 있어서 서버에서 로드하지 않음')
      return
    }

    // Store에 데이터가 없거나 초기값일 때만 서버에서 로드
    const currentStoreData = speedLimitsSettingsStore.speedLimitsSettings
    const isInitialData = currentStoreData.azimuthMin === 0.1 &&
      currentStoreData.azimuthMax === 15.0 &&
      currentStoreData.elevationMin === 0.1 &&
      currentStoreData.elevationMax === 10.0 &&
      currentStoreData.trainMin === 0.1 &&
      currentStoreData.trainMax === 5.0

    if (isInitialData) {
      console.log('초기 데이터이므로 서버에서 로드')
      await speedLimitsSettingsStore.loadSpeedLimitsSettings()
      // 초기 로드 시에는 원본 값 설정
      originalSettings.value = { ...speedLimitsSettingsStore.speedLimitsSettings }
      localSettings.value = { ...speedLimitsSettingsStore.speedLimitsSettings }
    } else {
      // Store에 이미 데이터가 있으면 그 값을 사용
      localSettings.value = { ...speedLimitsSettingsStore.speedLimitsSettings }
      originalSettings.value = { ...speedLimitsSettingsStore.speedLimitsSettings }
    }
  } catch (error) {
    console.error('속도 제한 설정 로드 실패:', error)
  }
})

// 초기화 함수 - 서버에서 로드된 값으로 초기화
const onReset = async () => {
  try {
    await speedLimitsSettingsStore.loadSpeedLimitsSettings()
    localSettings.value = { ...speedLimitsSettingsStore.speedLimitsSettings }
    originalSettings.value = { ...speedLimitsSettingsStore.speedLimitsSettings }
    updateChangeStatus(false)

    $q.notify({
      color: 'info',
      message: '속도 제한 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('속도 제한 설정 초기화 실패:', error)
  }
}
</script>

<style scoped>
.speed-limits-settings {
  max-width: 800px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}
</style>
