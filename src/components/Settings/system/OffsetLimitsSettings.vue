<template>
  <div class="offset-limits-settings">
    <h5 class="q-mt-none q-mb-md">오프셋 제한 설정</h5>

    <q-tabs v-model="activeTab" class="text-primary">
      <q-tab name="angle" label="각도 오프셋" icon="rotate_3d" />
      <q-tab name="time" label="시간 오프셋" icon="schedule" />
    </q-tabs>

    <q-tab-panels v-model="activeTab" animated>
      <!-- 각도 오프셋 설정 -->
      <q-tab-panel name="angle">
        <q-form @submit="onSaveAngleOffset" class="q-gutter-md">
          <q-input v-model.number="localAngleOffsetSettings.azimuth" label="Azimuth 오프셋 제한 (도)" type="number"
            :rules="angleOffsetRules" outlined :loading="loadingStates.offsetLimits" hint="방위각 오프셋의 최대 제한값"
            suffix="°" />

          <q-input v-model.number="localAngleOffsetSettings.elevation" label="Elevation 오프셋 제한 (도)" type="number"
            :rules="angleOffsetRules" outlined :loading="loadingStates.offsetLimits" hint="고도각 오프셋의 최대 제한값"
            suffix="°" />

          <q-input v-model.number="localAngleOffsetSettings.train" label="Train 오프셋 제한 (도)" type="number"
            :rules="angleOffsetRules" outlined :loading="loadingStates.offsetLimits" hint="Train각 오프셋의 최대 제한값"
            suffix="°" />

          <div class="row q-gutter-sm q-mt-md">
            <q-btn type="submit" color="primary" label="각도 오프셋 저장" :loading="loadingStates.offsetLimits"
              :disable="!isAngleOffsetFormValid" icon="save" />
            <q-btn color="secondary" label="각도 오프셋 초기화" @click="onResetAngleOffset"
              :disable="loadingStates.offsetLimits" icon="refresh" />
          </div>
        </q-form>
      </q-tab-panel>

      <!-- 시간 오프셋 설정 -->
      <q-tab-panel name="time">
        <q-form @submit="onSaveTimeOffset" class="q-gutter-md">
          <q-input v-model.number="localTimeOffsetSettings.min" label="시간 오프셋 최소값 (초)" type="number"
            :rules="timeOffsetMinRules" outlined :loading="loadingStates.offsetLimits" hint="시간 오프셋의 최소 제한값"
            suffix="초" />

          <q-input v-model.number="localTimeOffsetSettings.max" label="시간 오프셋 최대값 (초)" type="number"
            :rules="timeOffsetMaxRules" outlined :loading="loadingStates.offsetLimits" hint="시간 오프셋의 최대 제한값"
            suffix="초" />

          <div class="row q-gutter-sm q-mt-md">
            <q-btn type="submit" color="primary" label="시간 오프셋 저장" :loading="loadingStates.offsetLimits"
              :disable="!isTimeOffsetFormValid" icon="save" />
            <q-btn color="secondary" label="시간 오프셋 초기화" @click="onResetTimeOffset" :disable="loadingStates.offsetLimits"
              icon="refresh" />
          </div>
        </q-form>
      </q-tab-panel>
    </q-tab-panels>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.offsetLimits" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.offsetLimits }}
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
import type { AngleOffsetLimitsSettings, TimeOffsetLimitsSettings } from '@/services'

const $q = useQuasar()
const settingsStore = useSettingsStore()

// 로컬 상태
const activeTab = ref('angle')
const localAngleOffsetSettings = ref<AngleOffsetLimitsSettings>({
  azimuth: 50.0,
  elevation: 50.0,
  train: 50.0
})

const localTimeOffsetSettings = ref<TimeOffsetLimitsSettings>({
  min: 0.1,
  max: 99999
})

const successMessage = ref<string>('')

// 유효성 검사 규칙
const angleOffsetRules = [
  (val: number) => val !== null && val !== undefined || '오프셋 제한값은 필수입니다',
  (val: number) => val > 0 || '오프셋 제한값은 0보다 커야 합니다',
  (val: number) => val <= 360 || '오프셋 제한값은 360도 이하여야 합니다'
]

const timeOffsetMinRules = [
  (val: number) => val !== null && val !== undefined || '시간 오프셋 최소값은 필수입니다',
  (val: number) => val >= 0 || '시간 오프셋 최소값은 0 이상이어야 합니다',
  (val: number) => val <= 100000 || '시간 오프셋 최소값은 100000초 이하여야 합니다'
]

const timeOffsetMaxRules = [
  (val: number) => val !== null && val !== undefined || '시간 오프셋 최대값은 필수입니다',
  (val: number) => val > 0 || '시간 오프셋 최대값은 0보다 커야 합니다',
  (val: number) => val <= 100000 || '시간 오프셋 최대값은 100000초 이하여야 합니다',
  (val: number) => val >= localTimeOffsetSettings.value.min || '시간 오프셋 최대값은 최소값보다 크거나 같아야 합니다'
]

// 폼 유효성 검사
const isAngleOffsetFormValid = computed(() => {
  return localAngleOffsetSettings.value.azimuth > 0 && localAngleOffsetSettings.value.azimuth <= 360 &&
    localAngleOffsetSettings.value.elevation > 0 && localAngleOffsetSettings.value.elevation <= 360 &&
    localAngleOffsetSettings.value.train > 0 && localAngleOffsetSettings.value.train <= 360
})

const isTimeOffsetFormValid = computed(() => {
  return localTimeOffsetSettings.value.min >= 0 && localTimeOffsetSettings.value.min <= 100000 &&
    localTimeOffsetSettings.value.max > 0 && localTimeOffsetSettings.value.max <= 100000 &&
    localTimeOffsetSettings.value.max >= localTimeOffsetSettings.value.min
})

// 스토어 상태 가져오기
const { angleOffsetLimitsSettings, timeOffsetLimitsSettings, loadingStates, errorStates } = settingsStore

// 스토어 상태와 로컬 상태 동기화
watch(angleOffsetLimitsSettings, (newSettings) => {
  localAngleOffsetSettings.value = { ...newSettings }
}, { deep: true })

watch(timeOffsetLimitsSettings, (newSettings) => {
  localTimeOffsetSettings.value = { ...newSettings }
}, { deep: true })

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  await Promise.all([
    settingsStore.loadAngleOffsetLimitsSettings(),
    settingsStore.loadTimeOffsetLimitsSettings()
  ])
})

// 각도 오프셋 저장 함수
const onSaveAngleOffset = async () => {
  try {
    await settingsStore.saveAngleOffsetLimitsSettings(localAngleOffsetSettings.value)
    successMessage.value = '각도 오프셋 제한 설정이 성공적으로 저장되었습니다'

    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: '각도 오프셋 제한 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('각도 오프셋 제한 설정 저장 실패:', error)
  }
}

// 시간 오프셋 저장 함수
const onSaveTimeOffset = async () => {
  try {
    await settingsStore.saveTimeOffsetLimitsSettings(localTimeOffsetSettings.value)
    successMessage.value = '시간 오프셋 제한 설정이 성공적으로 저장되었습니다'

    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: '시간 오프셋 제한 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('시간 오프셋 제한 설정 저장 실패:', error)
  }
}

// 각도 오프셋 초기화 함수
const onResetAngleOffset = () => {
  localAngleOffsetSettings.value = {
    azimuth: 50.0,
    elevation: 50.0,
    train: 50.0
  }

  $q.notify({
    color: 'info',
    message: '각도 오프셋 제한 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
}

// 시간 오프셋 초기화 함수
const onResetTimeOffset = () => {
  localTimeOffsetSettings.value = {
    min: 0.1,
    max: 99999
  }

  $q.notify({
    color: 'info',
    message: '시간 오프셋 제한 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
}
</script>

<style scoped>
.offset-limits-settings {
  max-width: 600px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}
</style>
