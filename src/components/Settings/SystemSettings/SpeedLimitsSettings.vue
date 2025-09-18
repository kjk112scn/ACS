<template>
  <div class="speed-limits-settings">
    <h5 class="q-mt-none q-mb-md">속도 제한 설정</h5>

    <q-form @submit="onSave" class="q-gutter-md">
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
        <q-btn type="submit" color="primary" label="저장" :loading="loadingStates.speedLimits" :disable="!isFormValid"
          icon="save" />
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

    <!-- 성공 메시지 -->
    <q-banner v-if="successMessage" class="bg-positive text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="check_circle" />
      </template>
      {{ successMessage }}
    </q-banner>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useSettingsStore } from '../../../stores/settingsStore'
import type { SpeedLimitsSettings } from '../../../services/settingsService'

const $q = useQuasar()
const settingsStore = useSettingsStore()

// 로컬 상태
const localSettings = ref<SpeedLimitsSettings>({
  azimuthMin: 0.1,
  azimuthMax: 15.0,
  elevationMin: 0.1,
  elevationMax: 10.0,
  trainMin: 0.1,
  trainMax: 5.0
})

const successMessage = ref<string>('')

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

// 폼 유효성 검사
const isFormValid = computed(() => {
  return localSettings.value.azimuthMin > 0 && localSettings.value.azimuthMin <= 100 &&
    localSettings.value.azimuthMax > 0 && localSettings.value.azimuthMax <= 100 &&
    localSettings.value.azimuthMax >= localSettings.value.azimuthMin &&
    localSettings.value.elevationMin > 0 && localSettings.value.elevationMin <= 100 &&
    localSettings.value.elevationMax > 0 && localSettings.value.elevationMax <= 100 &&
    localSettings.value.elevationMax >= localSettings.value.elevationMin &&
    localSettings.value.trainMin > 0 && localSettings.value.trainMin <= 100 &&
    localSettings.value.trainMax > 0 && localSettings.value.trainMax <= 100 &&
    localSettings.value.trainMax >= localSettings.value.trainMin
})

// 스토어 상태 가져오기
const { speedLimitsSettings, loadingStates, errorStates } = settingsStore

// 스토어 상태와 로컬 상태 동기화
watch(speedLimitsSettings, (newSettings) => {
  localSettings.value = { ...newSettings }
}, { deep: true })

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  await settingsStore.loadSpeedLimitsSettings()
})

// 저장 함수
const onSave = async () => {
  try {
    await settingsStore.saveSpeedLimitsSettings(localSettings.value)
    successMessage.value = '속도 제한 설정이 성공적으로 저장되었습니다'

    // 3초 후 성공 메시지 숨기기
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: '속도 제한 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('속도 제한 설정 저장 실패:', error)
  }
}

// 초기화 함수
const onReset = () => {
  localSettings.value = {
    azimuthMin: 0.1,
    azimuthMax: 15.0,
    elevationMin: 0.1,
    elevationMax: 10.0,
    trainMin: 0.1,
    trainMax: 5.0
  }

  $q.notify({
    color: 'info',
    message: '속도 제한 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
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
