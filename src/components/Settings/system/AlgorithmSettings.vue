<template>
  <div class="algorithm-settings">
    <h5 class="q-mt-none q-mb-md">
      알고리즘 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
      <q-badge v-else-if="isSaved" color="green" class="q-ml-sm">
        저장됨
      </q-badge>
    </h5>

    <q-form @submit="onSave" class="q-gutter-md">
      <!-- GeoMinMotion 입력 -->
      <q-input v-model.number="localSettings.geoMinMotion" label="GeoMinMotion" type="number" :rules="geoMinMotionRules"
        outlined :loading="loadingStates.algorithm" hint="GeoMinMotion 값" />

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn type="submit" color="primary" label="저장" :loading="loadingStates.algorithm"
          :disable="!isFormValid || !hasUnsavedChanges" icon="save" />
        <q-btn color="secondary" label="초기화" @click="onReset" :disable="loadingStates.algorithm" icon="refresh" />
      </div>
    </q-form>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.algorithm" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.algorithm }}
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
import { useAlgorithmSettingsStore } from '@/stores'
import type { AlgorithmSettings } from '@/services'

const $q = useQuasar()
const algorithmSettingsStore = useAlgorithmSettingsStore()

// 스토어 상태 가져오기
const { loadingStates, errorStates, updateChangeStatus, pendingChanges } = algorithmSettingsStore

// 로컬 상태 - Store에서 변경된 값이 있으면 사용, 없으면 초기값
const getInitialLocalSettings = (): AlgorithmSettings => {
  if (pendingChanges) {
    return { ...pendingChanges }
  }
  return {
    geoMinMotion: algorithmSettingsStore.algorithmSettings.geoMinMotion || 1.1
  }
}

const localSettings = ref<AlgorithmSettings>(getInitialLocalSettings())

// 원본 상태 - Store에서 초기값 가져오기
const originalSettings = ref<AlgorithmSettings>({
  geoMinMotion: algorithmSettingsStore.algorithmSettings.geoMinMotion || 1.1
})

// 변경사항 상태를 로컬 상태로 직접 계산
const hasUnsavedChanges = computed(() => {
  return JSON.stringify(localSettings.value) !== JSON.stringify(originalSettings.value)
})

// 저장 상태
const isSaved = ref(true)

const successMessage = ref<string>('')

// 유효성 검사 규칙
const geoMinMotionRules = [
  (val: number) => val !== null && val !== undefined || 'GeoMinMotion은 필수입니다',
  (val: number) => val > 0 || 'GeoMinMotion은 0보다 커야 합니다',
  (val: number) => val <= 10 || 'GeoMinMotion은 10 이하여야 합니다'
]

// 폼 유효성 검사
const isFormValid = computed(() => {
  return localSettings.value.geoMinMotion > 0 && localSettings.value.geoMinMotion <= 10
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

// 스토어 상태와 로컬 상태 동기화 (변경사항이 있을 때는 절대 덮어쓰지 않음)
watch(
  () => algorithmSettingsStore.algorithmSettings,
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
    const currentStoreData = algorithmSettingsStore.algorithmSettings
    const isInitialData = currentStoreData.geoMinMotion === 1.1

    if (isInitialData) {
      console.log('초기 데이터이므로 서버에서 로드')
      await algorithmSettingsStore.loadAlgorithmSettings()
      // 초기 로드 시에는 원본 값 설정
      originalSettings.value = { ...algorithmSettingsStore.algorithmSettings }
      localSettings.value = { ...algorithmSettingsStore.algorithmSettings }
    } else {
      // Store에 이미 데이터가 있으면 그 값을 사용
      localSettings.value = { ...algorithmSettingsStore.algorithmSettings }
      originalSettings.value = { ...algorithmSettingsStore.algorithmSettings }
    }
  } catch (error) {
    console.error('알고리즘 설정 로드 실패:', error)
  }
})

// 저장 함수
const onSave = async () => {
  try {
    await algorithmSettingsStore.saveAlgorithmSettings(localSettings.value)

    // 저장 성공 시 변경사항 상태 업데이트
    updateChangeStatus(false)
    originalSettings.value = { ...localSettings.value }
    isSaved.value = true

    successMessage.value = '알고리즘 설정이 성공적으로 저장되었습니다'

    // 3초 후 성공 메시지 숨기기
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: '알고리즘 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('알고리즘 설정 저장 실패:', error)
  }
}

// 초기화 함수 - 서버에서 로드된 값으로 초기화
const onReset = async () => {
  try {
    await algorithmSettingsStore.loadAlgorithmSettings()
    localSettings.value = { ...algorithmSettingsStore.algorithmSettings }
    originalSettings.value = { ...algorithmSettingsStore.algorithmSettings }
    updateChangeStatus(false)
    isSaved.value = true

    $q.notify({
      color: 'info',
      message: '알고리즘 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('알고리즘 설정 초기화 실패:', error)
  }
}
</script>

<style scoped>
.algorithm-settings {
  max-width: 500px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}
</style>
