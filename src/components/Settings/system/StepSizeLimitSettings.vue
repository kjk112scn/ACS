<template>
  <div class="step-size-limit-settings">
    <h5 class="q-mt-none q-mb-md">
      스텝 사이즈 제한 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
      <q-badge v-else-if="isSaved" color="green" class="q-ml-sm">
        저장됨
      </q-badge>
    </h5>

    <q-form @submit="onSave" class="q-gutter-md">
      <!-- 스텝 사이즈 제한 -->
      <div class="row q-gutter-md">
        <div class="col">
          <q-input v-model.number="localSettings.min" label="스텝 사이즈 최소값 (도)" type="number" :rules="minRules" outlined
            :loading="loadingStates.stepSize" hint="스텝 사이즈의 최소 제한값" suffix="°" />
        </div>
        <div class="col">
          <q-input v-model.number="localSettings.max" label="스텝 사이즈 최대값 (도)" type="number" :rules="maxRules" outlined
            :loading="loadingStates.stepSize" hint="스텝 사이즈의 최대 제한값" suffix="°" />
        </div>
      </div>

      <!-- 설명 카드 -->
      <q-card flat bordered class="q-mt-md">
        <q-card-section>
          <div class="text-h6 q-mb-sm">스텝 사이즈 제한 설명</div>
          <div class="text-body2">
            <p><strong>스텝 사이즈 제한:</strong> 안테나 이동 시 한 번에 움직일 수 있는 최소/최대 각도를 설정합니다.</p>
            <p>이 값들은 안테나의 정밀한 제어와 안전한 이동을 위해 사용됩니다.</p>
            <ul class="q-mt-sm">
              <li><strong>최소값:</strong> 너무 작은 움직임을 방지하여 시스템 안정성 확보</li>
              <li><strong>최대값:</strong> 너무 큰 움직임을 방지하여 안테나 보호</li>
              <li>권장 범위: 0.1도 ~ 180도</li>
            </ul>
          </div>
        </q-card-section>
      </q-card>

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn type="submit" color="primary" label="저장" :loading="loadingStates.stepSize"
          :disable="!isFormValid || !hasUnsavedChanges" icon="save" />
        <q-btn color="secondary" label="초기화" @click="onReset" :disable="loadingStates.stepSize" icon="refresh" />
        <q-btn color="info" label="권장값 적용" @click="onApplyRecommended" :disable="loadingStates.stepSize"
          icon="recommend" />
      </div>
    </q-form>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.stepSize" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.stepSize }}
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
import { useStepSizeLimitSettingsStore } from '@/stores'
import type { StepSizeLimitSettings } from '@/services'

const $q = useQuasar()
const stepSizeLimitSettingsStore = useStepSizeLimitSettingsStore()

// 스토어 상태 가져오기
const { loadingStates, errorStates, updateChangeStatus, pendingChanges } = stepSizeLimitSettingsStore

// 로컬 상태 - Store에서 변경된 값이 있으면 사용, 없으면 초기값
const getInitialLocalSettings = (): StepSizeLimitSettings => {
  if (pendingChanges) {
    return { ...pendingChanges }
  }
  return {
    min: stepSizeLimitSettingsStore.stepSizeLimitSettings.min || 50,
    max: stepSizeLimitSettingsStore.stepSizeLimitSettings.max || 50
  }
}

const localSettings = ref<StepSizeLimitSettings>(getInitialLocalSettings())

// 원본 상태 - Store에서 초기값 가져오기
const originalSettings = ref<StepSizeLimitSettings>({
  min: stepSizeLimitSettingsStore.stepSizeLimitSettings.min || 50,
  max: stepSizeLimitSettingsStore.stepSizeLimitSettings.max || 50
})

// 변경사항 상태를 로컬 상태로 직접 계산
const hasUnsavedChanges = computed(() => {
  return JSON.stringify(localSettings.value) !== JSON.stringify(originalSettings.value)
})

// 저장 상태
const isSaved = ref(true)

const successMessage = ref<string>('')

// 유효성 검사 규칙
const minRules = [
  (val: number) => val !== null && val !== undefined || '스텝 사이즈 최소값은 필수입니다',
  (val: number) => val > 0 || '스텝 사이즈 최소값은 0보다 커야 합니다',
  (val: number) => val <= 180 || '스텝 사이즈 최소값은 180도 이하여야 합니다'
]

const maxRules = [
  (val: number) => val !== null && val !== undefined || '스텝 사이즈 최대값은 필수입니다',
  (val: number) => val > 0 || '스텝 사이즈 최대값은 0보다 커야 합니다',
  (val: number) => val <= 180 || '스텝 사이즈 최대값은 180도 이하여야 합니다',
  (val: number) => val >= localSettings.value.min || '스텝 사이즈 최대값은 최소값보다 크거나 같아야 합니다'
]

// 폼 유효성 검사
const isFormValid = computed(() => {
  return localSettings.value.min > 0 && localSettings.value.min <= 180 &&
    localSettings.value.max > 0 && localSettings.value.max <= 180 &&
    localSettings.value.max >= localSettings.value.min
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
  () => stepSizeLimitSettingsStore.stepSizeLimitSettings,
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
    const currentStoreData = stepSizeLimitSettingsStore.stepSizeLimitSettings
    const isInitialData = currentStoreData.min === 50 && currentStoreData.max === 50

    if (isInitialData) {
      console.log('초기 데이터이므로 서버에서 로드')
      await stepSizeLimitSettingsStore.loadStepSizeLimitSettings()
      // 초기 로드 시에는 원본 값 설정
      originalSettings.value = { ...stepSizeLimitSettingsStore.stepSizeLimitSettings }
      localSettings.value = { ...stepSizeLimitSettingsStore.stepSizeLimitSettings }
    } else {
      // Store에 이미 데이터가 있으면 그 값을 사용
      localSettings.value = { ...stepSizeLimitSettingsStore.stepSizeLimitSettings }
      originalSettings.value = { ...stepSizeLimitSettingsStore.stepSizeLimitSettings }
    }
  } catch (error) {
    console.error('스텝 사이즈 제한 설정 로드 실패:', error)
  }
})

// 저장 함수
const onSave = async () => {
  try {
    await stepSizeLimitSettingsStore.saveStepSizeLimitSettings(localSettings.value)

    // 저장 성공 시 변경사항 상태 업데이트
    updateChangeStatus(false)
    originalSettings.value = { ...localSettings.value }
    isSaved.value = true

    successMessage.value = '스텝 사이즈 제한 설정이 성공적으로 저장되었습니다'

    // 3초 후 성공 메시지 숨기기
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: '스텝 사이즈 제한 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('스텝 사이즈 제한 설정 저장 실패:', error)
  }
}

// 초기화 함수 - 서버에서 로드된 값으로 초기화
const onReset = async () => {
  try {
    await stepSizeLimitSettingsStore.loadStepSizeLimitSettings()
    localSettings.value = { ...stepSizeLimitSettingsStore.stepSizeLimitSettings }
    originalSettings.value = { ...stepSizeLimitSettingsStore.stepSizeLimitSettings }
    updateChangeStatus(false)
    isSaved.value = true

    $q.notify({
      color: 'info',
      message: '스텝 사이즈 제한 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('스텝 사이즈 제한 설정 초기화 실패:', error)
  }
}

// 권장값 적용 함수
const onApplyRecommended = () => {
  localSettings.value = {
    min: 0.1,
    max: 90.0
  }

  $q.notify({
    color: 'info',
    message: '권장값이 적용되었습니다',
    icon: 'recommend',
    position: 'top'
  })
}
</script>

<style scoped>
.step-size-limit-settings {
  max-width: 800px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}

.q-card {
  background-color: rgba(0, 0, 0, 0.02);
}

.body--dark .q-card {
  background-color: rgba(255, 255, 255, 0.03);
}
</style>
