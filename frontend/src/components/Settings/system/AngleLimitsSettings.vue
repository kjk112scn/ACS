<template>
  <div class="angle-limits-settings">
    <h5 class="q-mt-none q-mb-md">
      각도 제한 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
    </h5>

    <q-form class="q-gutter-md">
      <!-- Azimuth 각도 제한 -->
      <div class="row q-gutter-md">
        <div class="col">
          <q-input v-model.number="localSettings.azimuthMin" label="Azimuth 최소각 (도)" type="number"
            :rules="azimuthMinRules" outlined :loading="loadingStates.angleLimits" hint="방위각 최소 제한값" suffix="°" />
        </div>
        <div class="col">
          <q-input v-model.number="localSettings.azimuthMax" label="Azimuth 최대각 (도)" type="number"
            :rules="azimuthMaxRules" outlined :loading="loadingStates.angleLimits" hint="방위각 최대 제한값" suffix="°" />
        </div>
      </div>

      <!-- Elevation 각도 제한 -->
      <div class="row q-gutter-md">
        <div class="col">
          <q-input v-model.number="localSettings.elevationMin" label="Elevation 최소각 (도)" type="number"
            :rules="elevationMinRules" outlined :loading="loadingStates.angleLimits" hint="고도각 최소 제한값" suffix="°" />
        </div>
        <div class="col">
          <q-input v-model.number="localSettings.elevationMax" label="Elevation 최대각 (도)" type="number"
            :rules="elevationMaxRules" outlined :loading="loadingStates.angleLimits" hint="고도각 최대 제한값" suffix="°" />
        </div>
      </div>

      <!-- Train 각도 제한 -->
      <div class="row q-gutter-md">
        <div class="col">
          <q-input v-model.number="localSettings.trainMin" label="Train 최소각 (도)" type="number" :rules="trainMinRules"
            outlined :loading="loadingStates.angleLimits" hint="Train각 최소 제한값" suffix="°" />
        </div>
        <div class="col">
          <q-input v-model.number="localSettings.trainMax" label="Train 최대각 (도)" type="number" :rules="trainMaxRules"
            outlined :loading="loadingStates.angleLimits" hint="Train각 최대 제한값" suffix="°" />
        </div>
      </div>

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn color="secondary" label="초기화" @click="onReset" :disable="loadingStates.angleLimits" icon="refresh" />
      </div>
    </q-form>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.angleLimits" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.angleLimits }}
    </q-banner>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAngleLimitsSettingsStore } from '@/stores'
import type { AngleLimitsSettings } from '@/services'

const $q = useQuasar()
const angleLimitsSettingsStore = useAngleLimitsSettingsStore()

// 스토어 상태 가져오기
const { loadingStates, errorStates, updateChangeStatus, pendingChanges } = angleLimitsSettingsStore

// 로컬 상태 - Store에서 변경된 값이 있으면 사용, 없으면 초기값
const getInitialLocalSettings = (): AngleLimitsSettings => {
  if (pendingChanges) {
    return { ...pendingChanges }
  }
  return {
    azimuthMin: angleLimitsSettingsStore.angleLimitsSettings.azimuthMin || -270.0,
    azimuthMax: angleLimitsSettingsStore.angleLimitsSettings.azimuthMax || 270.0,
    elevationMin: angleLimitsSettingsStore.angleLimitsSettings.elevationMin || 0.0,
    elevationMax: angleLimitsSettingsStore.angleLimitsSettings.elevationMax || 180.0,
    trainMin: angleLimitsSettingsStore.angleLimitsSettings.trainMin || -270.0,
    trainMax: angleLimitsSettingsStore.angleLimitsSettings.trainMax || 270.0
  }
}

const localSettings = ref<AngleLimitsSettings>(getInitialLocalSettings())

// 원본 상태 - Store에서 초기값 가져오기
const originalSettings = ref<AngleLimitsSettings>({
  azimuthMin: angleLimitsSettingsStore.angleLimitsSettings.azimuthMin || -270.0,
  azimuthMax: angleLimitsSettingsStore.angleLimitsSettings.azimuthMax || 270.0,
  elevationMin: angleLimitsSettingsStore.angleLimitsSettings.elevationMin || 0.0,
  elevationMax: angleLimitsSettingsStore.angleLimitsSettings.elevationMax || 180.0,
  trainMin: angleLimitsSettingsStore.angleLimitsSettings.trainMin || -270.0,
  trainMax: angleLimitsSettingsStore.angleLimitsSettings.trainMax || 270.0
})

// 변경사항 상태를 로컬 상태로 직접 계산
const hasUnsavedChanges = computed(() => {
  return JSON.stringify(localSettings.value) !== JSON.stringify(originalSettings.value)
})

// 유효성 검사 규칙
const azimuthMinRules = [
  (val: number) => val !== null && val !== undefined || 'Azimuth 최소각은 필수입니다',
  (val: number) => val >= -360 && val <= 360 || 'Azimuth 최소각은 -360도 ~ 360도 범위여야 합니다'
]

const azimuthMaxRules = [
  (val: number) => val !== null && val !== undefined || 'Azimuth 최대각은 필수입니다',
  (val: number) => val >= -360 && val <= 360 || 'Azimuth 최대각은 -360도 ~ 360도 범위여야 합니다',
  (val: number) => val > localSettings.value.azimuthMin || 'Azimuth 최대각은 최소각보다 커야 합니다'
]

const elevationMinRules = [
  (val: number) => val !== null && val !== undefined || 'Elevation 최소각은 필수입니다',
  (val: number) => val >= -90 && val <= 90 || 'Elevation 최소각은 -90도 ~ 90도 범위여야 합니다'
]

const elevationMaxRules = [
  (val: number) => val !== null && val !== undefined || 'Elevation 최대각은 필수입니다',
  (val: number) => val >= -90 && val <= 90 || 'Elevation 최대각은 -90도 ~ 90도 범위여야 합니다',
  (val: number) => val > localSettings.value.elevationMin || 'Elevation 최대각은 최소각보다 커야 합니다'
]

const trainMinRules = [
  (val: number) => val !== null && val !== undefined || 'Train 최소각은 필수입니다',
  (val: number) => val >= -360 && val <= 360 || 'Train 최소각은 -360도 ~ 360도 범위여야 합니다'
]

const trainMaxRules = [
  (val: number) => val !== null && val !== undefined || 'Train 최대각은 필수입니다',
  (val: number) => val >= -360 && val <= 360 || 'Train 최대각은 -360도 ~ 360도 범위여야 합니다',
  (val: number) => val > localSettings.value.trainMin || 'Train 최대각은 최소각보다 커야 합니다'
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
  () => angleLimitsSettingsStore.angleLimitsSettings,
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
    const currentStoreData = angleLimitsSettingsStore.angleLimitsSettings
    const isInitialData = currentStoreData.azimuthMin === -270.0 &&
      currentStoreData.azimuthMax === 270.0 &&
      currentStoreData.elevationMin === 0.0 &&
      currentStoreData.elevationMax === 180.0 &&
      currentStoreData.trainMin === -270.0 &&
      currentStoreData.trainMax === 270.0

    if (isInitialData) {
      console.log('초기 데이터이므로 서버에서 로드')
      await angleLimitsSettingsStore.loadAngleLimitsSettings()
      // 초기 로드 시에는 원본 값 설정
      originalSettings.value = { ...angleLimitsSettingsStore.angleLimitsSettings }
      localSettings.value = { ...angleLimitsSettingsStore.angleLimitsSettings }
    } else {
      // Store에 이미 데이터가 있으면 그 값을 사용
      localSettings.value = { ...angleLimitsSettingsStore.angleLimitsSettings }
      originalSettings.value = { ...angleLimitsSettingsStore.angleLimitsSettings }
    }
  } catch (error) {
    console.error('각도 제한 설정 로드 실패:', error)
  }
})

// 초기화 함수 - 서버에서 로드된 값으로 초기화
const onReset = async () => {
  try {
    await angleLimitsSettingsStore.loadAngleLimitsSettings()
    localSettings.value = { ...angleLimitsSettingsStore.angleLimitsSettings }
    originalSettings.value = { ...angleLimitsSettingsStore.angleLimitsSettings }
    updateChangeStatus(false)

    $q.notify({
      color: 'info',
      message: '각도 제한 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('각도 제한 설정 초기화 실패:', error)
  }
}
</script>

<style scoped>
.angle-limits-settings {
  max-width: 800px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}
</style>
