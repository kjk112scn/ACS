<template>
  <div class="stow-settings">
    <h5 class="q-mt-none q-mb-md">
      스토우 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
      <q-badge v-else-if="isSaved" color="green" class="q-ml-sm">
        저장됨
      </q-badge>
    </h5>

    <q-tabs v-model="activeTab" class="text-primary">
      <q-tab name="angle" label="스토우 각도" icon="rotate_3d" />
      <q-tab name="speed" label="스토우 속도" icon="speed" />
    </q-tabs>

    <q-tab-panels v-model="activeTab" animated>
      <!-- 스토우 각도 설정 -->
      <q-tab-panel name="angle">
        <q-form @submit="onSaveAngle" class="q-gutter-md">
          <div class="row q-gutter-md">
            <div class="col">
              <q-input v-model.number="localAngleSettings.azimuth" label="Azimuth 스토우 각도 (도)" type="number"
                :rules="azimuthRules" outlined :loading="loadingStates.stowAngle" hint="Azimuth 축 스토우 각도" suffix="°" />
            </div>
            <div class="col">
              <q-input v-model.number="localAngleSettings.elevation" label="Elevation 스토우 각도 (도)" type="number"
                :rules="elevationRules" outlined :loading="loadingStates.stowAngle" hint="Elevation 축 스토우 각도"
                suffix="°" />
            </div>
            <div class="col">
              <q-input v-model.number="localAngleSettings.train" label="Train 스토우 각도 (도)" type="number"
                :rules="trainRules" outlined :loading="loadingStates.stowAngle" hint="Train 축 스토우 각도" suffix="°" />
            </div>
          </div>

          <div class="row q-gutter-sm q-mt-md">
            <q-btn type="submit" color="primary" label="각도 저장" :loading="loadingStates.stowAngle"
              :disable="!isAngleFormValid" icon="save" />
            <q-btn color="secondary" label="각도 초기화" @click="onResetAngle" :disable="loadingStates.stowAngle"
              icon="refresh" />
          </div>
        </q-form>
      </q-tab-panel>

      <!-- 스토우 속도 설정 -->
      <q-tab-panel name="speed">
        <q-form @submit="onSaveSpeed" class="q-gutter-md">
          <div class="row q-gutter-md">
            <div class="col">
              <q-input v-model.number="localSpeedSettings.azimuth" label="Azimuth 스토우 속도 (도/초)" type="number"
                :rules="azimuthSpeedRules" outlined :loading="loadingStates.stowSpeed" hint="Azimuth 축 스토우 속도"
                suffix="°/s" />
            </div>
            <div class="col">
              <q-input v-model.number="localSpeedSettings.elevation" label="Elevation 스토우 속도 (도/초)" type="number"
                :rules="elevationSpeedRules" outlined :loading="loadingStates.stowSpeed" hint="Elevation 축 스토우 속도"
                suffix="°/s" />
            </div>
            <div class="col">
              <q-input v-model.number="localSpeedSettings.train" label="Train 스토우 속도 (도/초)" type="number"
                :rules="trainSpeedRules" outlined :loading="loadingStates.stowSpeed" hint="Train 축 스토우 속도"
                suffix="°/s" />
            </div>
          </div>

          <div class="row q-gutter-sm q-mt-md">
            <q-btn type="submit" color="primary" label="속도 저장" :loading="loadingStates.stowSpeed"
              :disable="!isSpeedFormValid" icon="save" />
            <q-btn color="secondary" label="속도 초기화" @click="onResetSpeed" :disable="loadingStates.stowSpeed"
              icon="refresh" />
          </div>
        </q-form>
      </q-tab-panel>
    </q-tab-panels>

    <!-- 성공 메시지 -->
    <q-banner v-if="successMessage" class="bg-positive text-white q-mt-md" rounded>
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
import { useStowSettingsStore } from '@/stores'
import type { StowAngleSettings, StowSpeedSettings } from '@/services'

const $q = useQuasar()
const stowSettingsStore = useStowSettingsStore()

// 스토어 상태 가져오기
const { loadingStates, updateChangeStatus, pendingChanges, hasUnsavedChanges: storeHasUnsavedChanges } = stowSettingsStore

// 로컬 상태
const activeTab = ref('angle')

// 각도 설정 로컬 상태
const getInitialAngleSettings = (): StowAngleSettings => {
  if (pendingChanges.angle) {
    return { ...pendingChanges.angle }
  }
  return {
    azimuth: stowSettingsStore.stowAngleSettings.azimuth || 0.0,
    elevation: stowSettingsStore.stowAngleSettings.elevation || 90.0,
    train: stowSettingsStore.stowAngleSettings.train || 0.0
  }
}

const localAngleSettings = ref<StowAngleSettings>(getInitialAngleSettings())
const originalAngleSettings = ref<StowAngleSettings>({ ...localAngleSettings.value })

// 속도 설정 로컬 상태
const getInitialSpeedSettings = (): StowSpeedSettings => {
  if (pendingChanges.speed) {
    return { ...pendingChanges.speed }
  }
  return {
    azimuth: stowSettingsStore.stowSpeedSettings.azimuth || 5.0,
    elevation: stowSettingsStore.stowSpeedSettings.elevation || 5.0,
    train: stowSettingsStore.stowSpeedSettings.train || 5.0
  }
}

const localSpeedSettings = ref<StowSpeedSettings>(getInitialSpeedSettings())
const originalSpeedSettings = ref<StowSpeedSettings>({ ...localSpeedSettings.value })

const successMessage = ref<string>('')

// 변경사항 감지 - Store 상태와 로컬 상태를 모두 고려
const hasUnsavedChanges = computed(() => {
  // Store에 변경사항이 있으면 true
  if (storeHasUnsavedChanges.angle || storeHasUnsavedChanges.speed) {
    return true
  }
  // 로컬 상태와 원본 상태를 비교
  const angleChanged = JSON.stringify(localAngleSettings.value) !== JSON.stringify(originalAngleSettings.value)
  const speedChanged = JSON.stringify(localSpeedSettings.value) !== JSON.stringify(originalSpeedSettings.value)
  return angleChanged || speedChanged
})

// 저장 상태
const isSaved = ref<boolean>(true)

// 유효성 검사 규칙
const azimuthRules = [
  (val: number) => val !== null && val !== undefined || 'Azimuth 스토우 각도는 필수입니다',
  (val: number) => val >= -360 && val <= 360 || 'Azimuth 스토우 각도는 -360~360도 범위여야 합니다'
]

const elevationRules = [
  (val: number) => val !== null && val !== undefined || 'Elevation 스토우 각도는 필수입니다',
  (val: number) => val >= 0 && val <= 180 || 'Elevation 스토우 각도는 0~180도 범위여야 합니다'
]

const trainRules = [
  (val: number) => val !== null && val !== undefined || 'Train 스토우 각도는 필수입니다',
  (val: number) => val >= -360 && val <= 360 || 'Train 스토우 각도는 -360~360도 범위여야 합니다'
]

const azimuthSpeedRules = [
  (val: number) => val !== null && val !== undefined || 'Azimuth 스토우 속도는 필수입니다',
  (val: number) => val > 0 || 'Azimuth 스토우 속도는 0보다 커야 합니다'
]

const elevationSpeedRules = [
  (val: number) => val !== null && val !== undefined || 'Elevation 스토우 속도는 필수입니다',
  (val: number) => val > 0 || 'Elevation 스토우 속도는 0보다 커야 합니다'
]

const trainSpeedRules = [
  (val: number) => val !== null && val !== undefined || 'Train 스토우 속도는 필수입니다',
  (val: number) => val > 0 || 'Train 스토우 속도는 0보다 커야 합니다'
]

// 폼 유효성 검사
const isAngleFormValid = computed(() => {
  return localAngleSettings.value.azimuth !== null && localAngleSettings.value.azimuth !== undefined &&
    localAngleSettings.value.elevation !== null && localAngleSettings.value.elevation !== undefined &&
    localAngleSettings.value.train !== null && localAngleSettings.value.train !== undefined
})

const isSpeedFormValid = computed(() => {
  return localSpeedSettings.value.azimuth !== null && localSpeedSettings.value.azimuth !== undefined &&
    localSpeedSettings.value.elevation !== null && localSpeedSettings.value.elevation !== undefined &&
    localSpeedSettings.value.train !== null && localSpeedSettings.value.train !== undefined
})

// 변경사항 감지 watch - Store 상태 업데이트
watch(
  localAngleSettings,
  (newSettings) => {
    const hasChanges = JSON.stringify(newSettings) !== JSON.stringify(originalAngleSettings.value)
    updateChangeStatus('angle', hasChanges, newSettings)
  },
  { deep: true }
)

watch(
  localSpeedSettings,
  (newSettings) => {
    const hasChanges = JSON.stringify(newSettings) !== JSON.stringify(originalSpeedSettings.value)
    updateChangeStatus('speed', hasChanges, newSettings)
  },
  { deep: true }
)

// 컴포넌트 마운트 시 데이터 로드
onMounted(async () => {
  try {
    // Store에 데이터가 없으면 서버에서 로드
    if (stowSettingsStore.stowAngleSettings.azimuth === 0 &&
      stowSettingsStore.stowAngleSettings.elevation === 0 &&
      stowSettingsStore.stowAngleSettings.train === 0) {
      await stowSettingsStore.loadStowAngleSettings()
    }

    if (stowSettingsStore.stowSpeedSettings.azimuth === 0 &&
      stowSettingsStore.stowSpeedSettings.elevation === 0 &&
      stowSettingsStore.stowSpeedSettings.train === 0) {
      await stowSettingsStore.loadStowSpeedSettings()
    }

    // Store 데이터로 로컬 상태 초기화 (변경사항이 없을 때만)
    if (!stowSettingsStore.hasUnsavedChanges.angle) {
      localAngleSettings.value = { ...stowSettingsStore.stowAngleSettings }
      originalAngleSettings.value = { ...stowSettingsStore.stowAngleSettings }
    }

    if (!stowSettingsStore.hasUnsavedChanges.speed) {
      localSpeedSettings.value = { ...stowSettingsStore.stowSpeedSettings }
      originalSpeedSettings.value = { ...stowSettingsStore.stowSpeedSettings }
    }
  } catch (error) {
    console.error('스토우 설정 로드 실패:', error)
  }
})

// Store 상태와 로컬 상태 동기화 (저장되지 않은 변경사항이 있을 때만)
watch(
  stowSettingsStore.stowAngleSettings,
  (newSettings) => {
    // Store에 변경사항이 없고, 로컬에도 변경사항이 없을 때만 동기화
    if (!stowSettingsStore.hasUnsavedChanges.angle && !hasUnsavedChanges.value) {
      localAngleSettings.value = { ...newSettings }
      originalAngleSettings.value = { ...newSettings }
    }
  },
  { deep: true }
)

watch(
  stowSettingsStore.stowSpeedSettings,
  (newSettings) => {
    // Store에 변경사항이 없고, 로컬에도 변경사항이 없을 때만 동기화
    if (!stowSettingsStore.hasUnsavedChanges.speed && !hasUnsavedChanges.value) {
      localSpeedSettings.value = { ...newSettings }
      originalSpeedSettings.value = { ...newSettings }
    }
  },
  { deep: true }
)

// 각도 저장 함수
const onSaveAngle = async () => {
  try {
    await stowSettingsStore.saveStowAngleSettings(localAngleSettings.value)
    originalAngleSettings.value = { ...localAngleSettings.value }
    updateChangeStatus('angle', false)
    isSaved.value = true

    successMessage.value = '스토우 각도 설정이 저장되었습니다'
    setTimeout(() => { successMessage.value = '' }, 3000)

    $q.notify({
      color: 'positive',
      message: '스토우 각도 설정이 저장되었습니다',
      icon: 'check_circle',
      position: 'top'
    })
  } catch (error) {
    console.error('스토우 각도 설정 저장 실패:', error)
    $q.notify({
      color: 'negative',
      message: '스토우 각도 설정 저장에 실패했습니다',
      icon: 'error',
      position: 'top'
    })
  }
}

// 속도 저장 함수
const onSaveSpeed = async () => {
  try {
    await stowSettingsStore.saveStowSpeedSettings(localSpeedSettings.value)
    originalSpeedSettings.value = { ...localSpeedSettings.value }
    updateChangeStatus('speed', false)
    isSaved.value = true

    successMessage.value = '스토우 속도 설정이 저장되었습니다'
    setTimeout(() => { successMessage.value = '' }, 3000)

    $q.notify({
      color: 'positive',
      message: '스토우 속도 설정이 저장되었습니다',
      icon: 'check_circle',
      position: 'top'
    })
  } catch (error) {
    console.error('스토우 속도 설정 저장 실패:', error)
    $q.notify({
      color: 'negative',
      message: '스토우 속도 설정 저장에 실패했습니다',
      icon: 'error',
      position: 'top'
    })
  }
}

// 각도 초기화 함수
const onResetAngle = async () => {
  try {
    await stowSettingsStore.loadStowAngleSettings()
    localAngleSettings.value = { ...stowSettingsStore.stowAngleSettings }
    originalAngleSettings.value = { ...stowSettingsStore.stowAngleSettings }
    updateChangeStatus('angle', false)
    isSaved.value = true

    $q.notify({
      color: 'info',
      message: '스토우 각도 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('스토우 각도 설정 초기화 실패:', error)
  }
}

// 속도 초기화 함수
const onResetSpeed = async () => {
  try {
    await stowSettingsStore.loadStowSpeedSettings()
    localSpeedSettings.value = { ...stowSettingsStore.stowSpeedSettings }
    originalSpeedSettings.value = { ...stowSettingsStore.stowSpeedSettings }
    updateChangeStatus('speed', false)
    isSaved.value = true

    $q.notify({
      color: 'info',
      message: '스토우 속도 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('스토우 속도 설정 초기화 실패:', error)
  }
}
</script>

<style scoped>
.stow-settings {
  max-width: 600px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}
</style>
