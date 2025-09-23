<template>
  <div class="tracking-settings">
    <h5 class="q-mt-none q-mb-md">
      추적 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
      <q-badge v-else-if="isSaved" color="green" class="q-ml-sm">
        저장됨
      </q-badge>
    </h5>

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
import { useTrackingSettingsStore } from '@/stores'
import type { TrackingSettings } from '@/services'

const $q = useQuasar()
const trackingSettingsStore = useTrackingSettingsStore()

// 스토어 상태 가져오기
const { loadingStates, updateChangeStatus, pendingChanges, hasUnsavedChanges: storeHasUnsavedChanges } = trackingSettingsStore

// 로컬 상태 - Store에서 변경된 값이 있으면 사용, 없으면 초기값
const getInitialLocalSettings = (): TrackingSettings => {
  if (pendingChanges) {
    return { ...pendingChanges }
  }
  return {
    msInterval: trackingSettingsStore.trackingSettings.msInterval || 1000,
    durationDays: trackingSettingsStore.trackingSettings.durationDays || 1,
    minElevationAngle: trackingSettingsStore.trackingSettings.minElevationAngle || 10.0
  }
}

const localSettings = ref<TrackingSettings>(getInitialLocalSettings())
const originalSettings = ref<TrackingSettings>({ ...localSettings.value })

const successMessage = ref<string>('')

// 변경사항 감지 - Store 상태와 로컬 상태를 모두 고려
const hasUnsavedChanges = computed(() => {
  // Store에 변경사항이 있으면 true
  if (storeHasUnsavedChanges) {
    return true
  }
  // 로컬 상태와 원본 상태를 비교
  return JSON.stringify(localSettings.value) !== JSON.stringify(originalSettings.value)
})

// 저장 상태
const isSaved = ref<boolean>(true)

// 유효성 검사 규칙
const intervalRules = [
  (val: number) => val !== null && val !== undefined || '추적 간격은 필수입니다',
  (val: number) => val > 0 || '추적 간격은 0보다 커야 합니다',
  (val: number) => val <= 10000 || '추적 간격은 10000ms 이하여야 합니다'
]

const durationRules = [
  (val: number) => val !== null && val !== undefined || '추적 기간은 필수입니다',
  (val: number) => val > 0 || '추적 기간은 0보다 커야 합니다',
  (val: number) => val <= 365 || '추적 기간은 365일 이하여야 합니다'
]

const elevationRules = [
  (val: number) => val !== null && val !== undefined || '최소 고도각은 필수입니다',
  (val: number) => val >= 0 || '최소 고도각은 0도 이상이어야 합니다',
  (val: number) => val <= 90 || '최소 고도각은 90도 이하여야 합니다'
]

// 폼 유효성 검사
const isFormValid = computed(() => {
  return localSettings.value.msInterval !== null && localSettings.value.msInterval !== undefined &&
    localSettings.value.durationDays !== null && localSettings.value.durationDays !== undefined &&
    localSettings.value.minElevationAngle !== null && localSettings.value.minElevationAngle !== undefined
})

// 변경사항 감지 watch - Store 상태 업데이트
watch(
  localSettings,
  (newSettings) => {
    const hasChanges = JSON.stringify(newSettings) !== JSON.stringify(originalSettings.value)
    updateChangeStatus(hasChanges, newSettings)
  },
  { deep: true }
)

// 컴포넌트 마운트 시 데이터 로드
onMounted(async () => {
  try {
    // Store에 데이터가 없으면 서버에서 로드
    if (trackingSettingsStore.trackingSettings.msInterval === 0 &&
      trackingSettingsStore.trackingSettings.durationDays === 0 &&
      trackingSettingsStore.trackingSettings.minElevationAngle === 0) {
      await trackingSettingsStore.loadTrackingSettings()
    }

    // Store 데이터로 로컬 상태 초기화 (변경사항이 없을 때만)
    if (!storeHasUnsavedChanges) {
      localSettings.value = { ...trackingSettingsStore.trackingSettings }
      originalSettings.value = { ...trackingSettingsStore.trackingSettings }
    }
  } catch (error) {
    console.error('추적 설정 로드 실패:', error)
  }
})

// Store 상태와 로컬 상태 동기화 (저장되지 않은 변경사항이 있을 때만)
watch(
  trackingSettingsStore.trackingSettings,
  (newSettings) => {
    // Store에 변경사항이 없고, 로컬에도 변경사항이 없을 때만 동기화
    if (!storeHasUnsavedChanges && !hasUnsavedChanges.value) {
      localSettings.value = { ...newSettings }
      originalSettings.value = { ...newSettings }
    }
  },
  { deep: true }
)

// 저장 함수
const onSave = async () => {
  try {
    await trackingSettingsStore.saveTrackingSettings(localSettings.value)
    originalSettings.value = { ...localSettings.value }
    updateChangeStatus(false)
    isSaved.value = true

    successMessage.value = '추적 설정이 저장되었습니다'
    setTimeout(() => { successMessage.value = '' }, 3000)

    $q.notify({
      color: 'positive',
      message: '추적 설정이 저장되었습니다',
      icon: 'check_circle',
      position: 'top'
    })
  } catch (error) {
    console.error('추적 설정 저장 실패:', error)
    $q.notify({
      color: 'negative',
      message: '추적 설정 저장에 실패했습니다',
      icon: 'error',
      position: 'top'
    })
  }
}

// 초기화 함수 - 서버에서 로드된 값으로 초기화
const onReset = async () => {
  try {
    await trackingSettingsStore.loadTrackingSettings()
    localSettings.value = { ...trackingSettingsStore.trackingSettings }
    originalSettings.value = { ...trackingSettingsStore.trackingSettings }
    updateChangeStatus(false)
    isSaved.value = true

    $q.notify({
      color: 'info',
      message: '추적 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('추적 설정 초기화 실패:', error)
  }
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
