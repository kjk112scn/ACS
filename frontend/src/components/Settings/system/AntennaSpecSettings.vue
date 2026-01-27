<template>
  <div class="antenna-spec-settings">
    <h5 class="q-mt-none q-mb-md">
      안테나 사양 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
    </h5>

    <q-form class="q-gutter-md">
      <!-- True North Offset Angle 입력 -->
      <q-input v-model.number="localSettings.trueNorthOffsetAngle" label="True North Offset Angle (도)" type="number"
        :rules="offsetRules" outlined :loading="loadingStates.antennaSpec" hint="True North 기준 오프셋 각도" suffix="°" />

      <!-- Tilt Angle 입력 -->
      <q-input v-model.number="localSettings.tiltAngle" label="Tilt Angle (도)" type="number" :rules="tiltRules" outlined
        :loading="loadingStates.antennaSpec" hint="안테나 틸트 각도" suffix="°" />

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn color="secondary" label="초기화" @click="onReset" :disable="loadingStates.antennaSpec" icon="refresh" />
      </div>
    </q-form>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.antennaSpec" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.antennaSpec }}
    </q-banner>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAntennaSpecSettingsStore } from '@/stores'
import type { AntennaSpecSettings } from '@/services'

const $q = useQuasar()
const antennaSpecSettingsStore = useAntennaSpecSettingsStore()

// 스토어 상태 가져오기
const { loadingStates, errorStates, updateChangeStatus, pendingChanges } = antennaSpecSettingsStore

// 로컬 상태 - Store에서 변경된 값이 있으면 사용, 없으면 초기값
const getInitialLocalSettings = (): AntennaSpecSettings => {
  if (pendingChanges) {
    return { ...pendingChanges }
  }
  return {
    trueNorthOffsetAngle: antennaSpecSettingsStore.antennaSpecSettings.trueNorthOffsetAngle || 0.0,
    tiltAngle: antennaSpecSettingsStore.antennaSpecSettings.tiltAngle || -7.0
  }
}

const localSettings = ref<AntennaSpecSettings>(getInitialLocalSettings())

// 원본 상태 - Store에서 초기값 가져오기
const originalSettings = ref<AntennaSpecSettings>({
  trueNorthOffsetAngle: antennaSpecSettingsStore.antennaSpecSettings.trueNorthOffsetAngle || 0.0,
  tiltAngle: antennaSpecSettingsStore.antennaSpecSettings.tiltAngle || -7.0
})

// 변경사항 상태를 로컬 상태로 직접 계산
const hasUnsavedChanges = computed(() => {
  return JSON.stringify(localSettings.value) !== JSON.stringify(originalSettings.value)
})

// 유효성 검사 규칙
const offsetRules = [
  (val: number) => val !== null && val !== undefined || 'True North Offset Angle은 필수입니다',
  (val: number) => val >= -360 && val <= 360 || 'True North Offset Angle은 -360도 ~ 360도 범위여야 합니다'
]

const tiltRules = [
  (val: number) => val !== null && val !== undefined || 'Tilt Angle은 필수입니다',
  (val: number) => val >= -90 && val <= 90 || 'Tilt Angle은 -90도 ~ 90도 범위여야 합니다'
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
  () => antennaSpecSettingsStore.antennaSpecSettings,
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
    const currentStoreData = antennaSpecSettingsStore.antennaSpecSettings
    const isInitialData = currentStoreData.trueNorthOffsetAngle === 0.0 &&
      currentStoreData.tiltAngle === -7.0

    if (isInitialData) {
      console.log('초기 데이터이므로 서버에서 로드')
      await antennaSpecSettingsStore.loadAntennaSpecSettings()
      // 초기 로드 시에는 원본 값 설정
      originalSettings.value = { ...antennaSpecSettingsStore.antennaSpecSettings }
      localSettings.value = { ...antennaSpecSettingsStore.antennaSpecSettings }
    } else {
      // Store에 이미 데이터가 있으면 그 값을 사용
      localSettings.value = { ...antennaSpecSettingsStore.antennaSpecSettings }
      originalSettings.value = { ...antennaSpecSettingsStore.antennaSpecSettings }
    }
  } catch (error) {
    console.error('안테나 사양 설정 로드 실패:', error)
  }
})

// 초기화 함수 - 서버에서 로드된 값으로 초기화
const onReset = async () => {
  try {
    await antennaSpecSettingsStore.loadAntennaSpecSettings()
    localSettings.value = { ...antennaSpecSettingsStore.antennaSpecSettings }
    originalSettings.value = { ...antennaSpecSettingsStore.antennaSpecSettings }
    updateChangeStatus(false)

    $q.notify({
      color: 'info',
      message: '안테나 사양 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('안테나 사양 설정 초기화 실패:', error)
  }
}
</script>

<style scoped>
.antenna-spec-settings {
  max-width: 500px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}
</style>
