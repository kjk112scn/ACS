<template>
  <div class="stow-settings">
    <h5 class="q-mt-none q-mb-md">Stow 설정</h5>

    <q-tabs v-model="activeTab" class="text-primary">
      <q-tab name="angle" label="Stow 각도" icon="rotate_3d" />
      <q-tab name="speed" label="Stow 속도" icon="speed" />
    </q-tabs>

    <q-tab-panels v-model="activeTab" animated>
      <!-- Stow 각도 설정 -->
      <q-tab-panel name="angle">
        <q-form @submit="onSaveAngle" class="q-gutter-md">
          <q-input v-model.number="localAngleSettings.azimuth" label="Stow 방위각 (도)" type="number" :rules="angleRules"
            outlined :loading="loadingStates.stow" hint="Stow 위치의 방위각" suffix="°" />

          <q-input v-model.number="localAngleSettings.elevation" label="Stow 고도각 (도)" type="number" :rules="angleRules"
            outlined :loading="loadingStates.stow" hint="Stow 위치의 고도각" suffix="°" />

          <q-input v-model.number="localAngleSettings.train" label="Stow Train각 (도)" type="number" :rules="angleRules"
            outlined :loading="loadingStates.stow" hint="Stow 위치의 Train각" suffix="°" />

          <div class="row q-gutter-sm q-mt-md">
            <q-btn type="submit" color="primary" label="각도 저장" :loading="loadingStates.stow"
              :disable="!isAngleFormValid" icon="save" />
            <q-btn color="secondary" label="각도 초기화" @click="onResetAngle" :disable="loadingStates.stow"
              icon="refresh" />
          </div>
        </q-form>
      </q-tab-panel>

      <!-- Stow 속도 설정 -->
      <q-tab-panel name="speed">
        <q-form @submit="onSaveSpeed" class="q-gutter-md">
          <q-input v-model.number="localSpeedSettings.azimuth" label="Stow 방위각 속도 (도/초)" type="number"
            :rules="speedRules" outlined :loading="loadingStates.stow" hint="Stow 이동 시 방위각 속도" suffix="°/s" />

          <q-input v-model.number="localSpeedSettings.elevation" label="Stow 고도각 속도 (도/초)" type="number"
            :rules="speedRules" outlined :loading="loadingStates.stow" hint="Stow 이동 시 고도각 속도" suffix="°/s" />

          <q-input v-model.number="localSpeedSettings.train" label="Stow Train각 속도 (도/초)" type="number"
            :rules="speedRules" outlined :loading="loadingStates.stow" hint="Stow 이동 시 Train각 속도" suffix="°/s" />

          <div class="row q-gutter-sm q-mt-md">
            <q-btn type="submit" color="primary" label="속도 저장" :loading="loadingStates.stow"
              :disable="!isSpeedFormValid" icon="save" />
            <q-btn color="secondary" label="속도 초기화" @click="onResetSpeed" :disable="loadingStates.stow"
              icon="refresh" />
          </div>
        </q-form>
      </q-tab-panel>
    </q-tab-panels>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.stow" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.stow }}
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
import { useSettingsStore } from '@/stores'
import type { StowAngleSettings, StowSpeedSettings } from '@/services'

const $q = useQuasar()
const settingsStore = useSettingsStore()

// 로컬 상태
const activeTab = ref('angle')
const localAngleSettings = ref<StowAngleSettings>({
  azimuth: 0.0,
  elevation: 90.0,
  train: 0.0
})

const localSpeedSettings = ref<StowSpeedSettings>({
  azimuth: 5.0,
  elevation: 5.0,
  train: 5.0
})

const successMessage = ref<string>('')

// 유효성 검사 규칙
const angleRules = [
  (val: number) => val !== null && val !== undefined || '각도는 필수입니다',
  (val: number) => val >= -360 && val <= 360 || '각도는 -360도 ~ 360도 범위여야 합니다'
]

const speedRules = [
  (val: number) => val !== null && val !== undefined || '속도는 필수입니다',
  (val: number) => val > 0 || '속도는 0보다 커야 합니다',
  (val: number) => val <= 100 || '속도는 100도/초 이하여야 합니다'
]

// 폼 유효성 검사
const isAngleFormValid = computed(() => {
  return localAngleSettings.value.azimuth >= -360 && localAngleSettings.value.azimuth <= 360 &&
    localAngleSettings.value.elevation >= -360 && localAngleSettings.value.elevation <= 360 &&
    localAngleSettings.value.train >= -360 && localAngleSettings.value.train <= 360
})

const isSpeedFormValid = computed(() => {
  return localSpeedSettings.value.azimuth > 0 && localSpeedSettings.value.azimuth <= 100 &&
    localSpeedSettings.value.elevation > 0 && localSpeedSettings.value.elevation <= 100 &&
    localSpeedSettings.value.train > 0 && localSpeedSettings.value.train <= 100
})

// 스토어 상태 가져오기
const { stowAngleSettings, stowSpeedSettings, loadingStates, errorStates } = settingsStore

// 스토어 상태와 로컬 상태 동기화
watch(stowAngleSettings, (newSettings) => {
  localAngleSettings.value = { ...newSettings }
}, { deep: true })

watch(stowSpeedSettings, (newSettings) => {
  localSpeedSettings.value = { ...newSettings }
}, { deep: true })

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  await Promise.all([
    settingsStore.loadStowAngleSettings(),
    settingsStore.loadStowSpeedSettings()
  ])
})

// 각도 저장 함수
const onSaveAngle = async () => {
  try {
    await settingsStore.saveStowAngleSettings(localAngleSettings.value)
    successMessage.value = 'Stow 각도 설정이 성공적으로 저장되었습니다'

    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: 'Stow 각도 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('Stow 각도 설정 저장 실패:', error)
  }
}

// 속도 저장 함수
const onSaveSpeed = async () => {
  try {
    await settingsStore.saveStowSpeedSettings(localSpeedSettings.value)
    successMessage.value = 'Stow 속도 설정이 성공적으로 저장되었습니다'

    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: 'Stow 속도 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('Stow 속도 설정 저장 실패:', error)
  }
}

// 각도 초기화 함수
const onResetAngle = () => {
  localAngleSettings.value = {
    azimuth: 0.0,
    elevation: 90.0,
    train: 0.0
  }

  $q.notify({
    color: 'info',
    message: 'Stow 각도 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
}

// 속도 초기화 함수
const onResetSpeed = () => {
  localSpeedSettings.value = {
    azimuth: 5.0,
    elevation: 5.0,
    train: 5.0
  }

  $q.notify({
    color: 'info',
    message: 'Stow 속도 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
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
