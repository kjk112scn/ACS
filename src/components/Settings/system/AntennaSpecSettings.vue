<template>
  <div class="antenna-spec-settings">
    <h5 class="q-mt-none q-mb-md">안테나 사양 설정</h5>

    <q-form @submit="onSave" class="q-gutter-md">
      <!-- True North Offset Angle 입력 -->
      <q-input v-model.number="localSettings.trueNorthOffsetAngle" label="True North Offset Angle (도)" type="number"
        :rules="offsetRules" outlined :loading="loadingStates.antennaSpec" hint="True North 기준 오프셋 각도" suffix="°" />

      <!-- Tilt Angle 입력 -->
      <q-input v-model.number="localSettings.tiltAngle" label="Tilt Angle (도)" type="number" :rules="tiltRules" outlined
        :loading="loadingStates.antennaSpec" hint="안테나 틸트 각도" suffix="°" />

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn type="submit" color="primary" label="저장" :loading="loadingStates.antennaSpec" :disable="!isFormValid"
          icon="save" />
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
import type { AntennaSpecSettings } from '@/services'

const $q = useQuasar()
const settingsStore = useSettingsStore()

// 로컬 상태
const localSettings = ref<AntennaSpecSettings>({
  trueNorthOffsetAngle: 0.0,
  tiltAngle: -7.0
})

const successMessage = ref<string>('')

// 유효성 검사 규칙
const offsetRules = [
  (val: number) => val !== null && val !== undefined || 'True North Offset Angle은 필수입니다',
  (val: number) => val >= -360 && val <= 360 || 'True North Offset Angle은 -360도 ~ 360도 범위여야 합니다'
]

const tiltRules = [
  (val: number) => val !== null && val !== undefined || 'Tilt Angle은 필수입니다',
  (val: number) => val >= -90 && val <= 90 || 'Tilt Angle은 -90도 ~ 90도 범위여야 합니다'
]

// 폼 유효성 검사
const isFormValid = computed(() => {
  return localSettings.value.trueNorthOffsetAngle >= -360 && localSettings.value.trueNorthOffsetAngle <= 360 &&
    localSettings.value.tiltAngle >= -90 && localSettings.value.tiltAngle <= 90
})

// 스토어 상태 가져오기
const { antennaSpecSettings, loadingStates, errorStates } = settingsStore

// 스토어 상태와 로컬 상태 동기화
watch(antennaSpecSettings, (newSettings) => {
  localSettings.value = { ...newSettings }
}, { deep: true })

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  await settingsStore.loadAntennaSpecSettings()
})

// 저장 함수
const onSave = async () => {
  try {
    await settingsStore.saveAntennaSpecSettings(localSettings.value)
    successMessage.value = '안테나 사양 설정이 성공적으로 저장되었습니다'

    // 3초 후 성공 메시지 숨기기
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: '안테나 사양 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('안테나 사양 설정 저장 실패:', error)
  }
}

// 초기화 함수
const onReset = () => {
  localSettings.value = {
    trueNorthOffsetAngle: 0.0,
    tiltAngle: -7.0
  }

  $q.notify({
    color: 'info',
    message: '안테나 사양 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
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
