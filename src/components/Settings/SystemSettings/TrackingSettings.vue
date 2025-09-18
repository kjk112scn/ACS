<template>
  <div class="tracking-settings">
    <h5>추적 설정</h5>

    <q-form @submit="onSave" class="q-gutter-md">
      <!-- 추적 간격 입력 -->
      <q-input v-model.number="localSettings.msInterval" label="추적 간격 (밀리초)" type="number" :rules="intervalRules"
        outlined :loading="loadingStates.tracking" hint="추적 계산 주기 (밀리초)" suffix="ms" />

      <!-- 추적 기간 입력 -->
      <q-input v-model.number="localSettings.durationDays" label="추적 기간 (일)" type="number" :rules="durationRules"
        outlined :loading="loadingStates.tracking" hint="추적을 수행할 기간" suffix="일" />

      <!-- 최소 고도각 입력 -->
      <q-input v-model.number="localSettings.minElevationAngle" label="최소 고도각 (도)" type="number" :rules="elevationRules"
        outlined :loading="loadingStates.tracking" hint="추적 시 고려할 최소 고도각" suffix="°" />

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn type="submit" color="primary" label="저장" :loading="loadingStates.tracking" :disable="!isFormValid"
          icon="save" />
        <q-btn color="secondary" label="초기화" @click="onReset" :disable="loadingStates.tracking" icon="refresh" />
      </div>
    </q-form>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.tracking" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.tracking }}
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
import type { TrackingSettings } from '../../../services/settingsService'

const $q = useQuasar()
const settingsStore = useSettingsStore()

// 로컬 상태
const localSettings = ref<TrackingSettings>({
  msInterval: 100,
  durationDays: 1,
  minElevationAngle: 0
})

const successMessage = ref<string>('')

// 유효성 검사 규칙
const intervalRules = [
  (val: number) => val !== null && val !== undefined || '추적 간격은 필수입니다',
  (val: number) => val > 0 || '추적 간격은 0보다 커야 합니다',
  (val: number) => val <= 10000 || '추적 간격은 10000ms 이하여야 합니다'
]

const durationRules = [
  (val: number) => val !== null && val !== undefined || '추적 기간은 필수입니다',
  (val: number) => val > 0 || '추적 기간은 0일보다 커야 합니다',
  (val: number) => val <= 365 || '추적 기간은 365일 이하여야 합니다'
]

const elevationRules = [
  (val: number) => val !== null && val !== undefined || '최소 고도각은 필수입니다',
  (val: number) => val >= -90 || '최소 고도각은 -90도 이상이어야 합니다',
  (val: number) => val <= 90 || '최소 고도각은 90도 이하여야 합니다'
]

// 폼 유효성 검사
const isFormValid = computed(() => {
  return localSettings.value.msInterval > 0 && localSettings.value.msInterval <= 10000 &&
    localSettings.value.durationDays > 0 && localSettings.value.durationDays <= 365 &&
    localSettings.value.minElevationAngle >= -90 && localSettings.value.minElevationAngle <= 90
})

// 스토어 상태 가져오기
const { trackingSettings, loadingStates, errorStates } = settingsStore

// 스토어 상태와 로컬 상태 동기화
watch(trackingSettings, (newSettings) => {
  localSettings.value = { ...newSettings }
}, { deep: true })

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  await settingsStore.loadTrackingSettings()
})

// 저장 함수
const onSave = async () => {
  try {
    await settingsStore.saveTrackingSettings(localSettings.value)
    successMessage.value = '추적 설정이 성공적으로 저장되었습니다'

    // 3초 후 성공 메시지 숨기기
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: '추적 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('추적 설정 저장 실패:', error)
  }
}

// 초기화 함수
const onReset = () => {
  localSettings.value = {
    msInterval: 100,
    durationDays: 1,
    minElevationAngle: 0
  }

  $q.notify({
    color: 'info',
    message: '추적 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
}
</script>

<style scoped>
.tracking-settings {
  max-width: 500px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}
</style>
