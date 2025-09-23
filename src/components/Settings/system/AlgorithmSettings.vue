<template>
  <div class="algorithm-settings">
    <h5 class="q-mt-none q-mb-md">알고리즘 설정</h5>

    <q-form @submit="onSave" class="q-gutter-md">
      <!-- Geo Min Motion 입력 -->
      <q-input v-model.number="localSettings.geoMinMotion" label="Geo Min Motion (도/초)" type="number"
        :rules="geoMinMotionRules" outlined :loading="loadingStates.algorithm" hint="지구 동기 궤도 최소 모션" suffix="°/s" />

      <!-- 설명 카드 -->
      <q-card flat bordered class="q-mt-md">
        <q-card-section>
          <div class="text-h6 q-mb-sm">알고리즘 설정 설명</div>
          <div class="text-body2">
            <p><strong>Geo Min Motion:</strong> 지구 동기 궤도 위성의 최소 모션 값을 설정합니다.</p>
            <p>이 값은 위성 추적 알고리즘에서 정지궤도 위성의 움직임을 판단하는 기준으로 사용됩니다.</p>
            <ul class="q-mt-sm">
              <li>값이 작을수록: 더 민감하게 움직임을 감지</li>
              <li>값이 클수록: 더 안정적으로 움직임을 감지</li>
              <li>권장 범위: 0.1 ~ 5.0도/초</li>
            </ul>
          </div>
        </q-card-section>
      </q-card>

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn type="submit" color="primary" label="저장" :loading="loadingStates.algorithm" :disable="!isFormValid"
          icon="save" />
        <q-btn color="secondary" label="초기화" @click="onReset" :disable="loadingStates.algorithm" icon="refresh" />
        <q-btn color="info" label="권장값 적용" @click="onApplyRecommended" :disable="loadingStates.algorithm"
          icon="recommend" />
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
import { useSettingsStore } from '@/stores'
import type { AlgorithmSettings } from '@/services'

const $q = useQuasar()
const settingsStore = useSettingsStore()

// 로컬 상태
const localSettings = ref<AlgorithmSettings>({
  geoMinMotion: 1.1
})

const successMessage = ref<string>('')

// 유효성 검사 규칙
const geoMinMotionRules = [
  (val: number) => val !== null && val !== undefined || 'Geo Min Motion은 필수입니다',
  (val: number) => val > 0 || 'Geo Min Motion은 0보다 커야 합니다',
  (val: number) => val <= 10 || 'Geo Min Motion은 10도/초 이하여야 합니다'
]

// 폼 유효성 검사
const isFormValid = computed(() => {
  return localSettings.value.geoMinMotion > 0 && localSettings.value.geoMinMotion <= 10
})

// 스토어 상태 가져오기
const { algorithmSettings, loadingStates, errorStates } = settingsStore

// 스토어 상태와 로컬 상태 동기화
watch(algorithmSettings, (newSettings) => {
  localSettings.value = { ...newSettings }
}, { deep: true })

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  await settingsStore.loadAlgorithmSettings()
})

// 저장 함수
const onSave = async () => {
  try {
    await settingsStore.saveAlgorithmSettings(localSettings.value)
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

// 초기화 함수
const onReset = () => {
  localSettings.value = {
    geoMinMotion: 1.1
  }

  $q.notify({
    color: 'info',
    message: '알고리즘 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
}

// 권장값 적용 함수
const onApplyRecommended = () => {
  localSettings.value = {
    geoMinMotion: 1.1
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
.algorithm-settings {
  max-width: 600px;
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
