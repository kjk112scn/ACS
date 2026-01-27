<template>
  <div class="storage-settings">
    <h5 class="q-mt-none q-mb-md">
      저장 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
    </h5>

    <q-form class="q-gutter-md">
      <!-- 배치 저장 간격 입력 -->
      <q-input
        v-model.number="localSettings.saveInterval"
        label="배치 저장 간격 (ms)"
        type="number"
        :rules="saveIntervalRules"
        outlined
        :loading="loading"
        hint="추적 데이터 배치 저장 주기 (1000ms ~ 30000ms)"
        suffix="ms"
      >
        <template v-slot:prepend>
          <q-icon name="save" />
        </template>
      </q-input>

      <!-- 프리셋 버튼들 -->
      <div class="row q-gutter-sm">
        <q-btn
          v-for="preset in presets"
          :key="preset.value"
          :label="preset.label"
          :color="localSettings.saveInterval === preset.value ? 'primary' : 'grey-5'"
          :outline="localSettings.saveInterval !== preset.value"
          size="sm"
          @click="localSettings.saveInterval = preset.value"
        />
      </div>

      <!-- 설명 카드 -->
      <q-card flat bordered class="q-pa-sm info-card">
        <div class="text-caption info-text">
          <q-icon name="info" color="primary" class="q-mr-xs" />
          <strong>배치 저장이란?</strong>
          <p class="q-mb-none q-mt-xs">
            위성/태양 추적 중 생성되는 데이터를 일정 간격으로 모아서 DB에 저장합니다.
            간격이 짧으면 데이터 손실이 적지만 DB 부하가 증가하고,
            간격이 길면 DB 부하는 줄지만 장애 시 데이터 손실이 커집니다.
          </p>
        </div>
      </q-card>

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn
          color="secondary"
          label="초기화"
          @click="onReset"
          :disable="loading"
          icon="refresh"
        />
      </div>
    </q-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { settingsService } from '@/services'
import type { StorageSettings } from '@/services'

const $q = useQuasar()

// 로딩 상태
const loading = ref(false)

// 로컬 상태
const localSettings = ref<StorageSettings>({
  saveInterval: 5000,
})
const originalSettings = ref<StorageSettings>({ ...localSettings.value })

// 프리셋 옵션
const presets = [
  { label: '1초', value: 1000 },
  { label: '3초', value: 3000 },
  { label: '5초 (권장)', value: 5000 },
  { label: '10초', value: 10000 },
  { label: '30초', value: 30000 },
]

// 변경사항 감지
const hasUnsavedChanges = computed(() => {
  return localSettings.value.saveInterval !== originalSettings.value.saveInterval
})

// 유효성 검사 규칙
const saveIntervalRules = [
  (val: number) => val !== null && val !== undefined || '저장 간격은 필수입니다',
  (val: number) => val >= 1000 || '저장 간격은 1000ms 이상이어야 합니다',
  (val: number) => val <= 30000 || '저장 간격은 30000ms 이하여야 합니다',
]

// 설정 로드
const loadSettings = async () => {
  loading.value = true
  try {
    const settings = await settingsService.getStorageSettings()
    localSettings.value = { ...settings }
    originalSettings.value = { ...settings }
  } catch (error) {
    console.error('Storage 설정 로드 실패:', error)
    $q.notify({
      type: 'negative',
      message: 'Storage 설정을 불러오는데 실패했습니다.',
    })
  } finally {
    loading.value = false
  }
}

// 초기화
const onReset = () => {
  localSettings.value = { ...originalSettings.value }
  $q.notify({
    type: 'info',
    message: '설정이 초기화되었습니다.',
  })
}

// 변경사항 감지 시 자동 저장
watch(
  () => localSettings.value.saveInterval,
  async (newValue, oldValue) => {
    if (newValue === oldValue) return
    if (newValue < 1000 || newValue > 30000) return

    loading.value = true
    try {
      await settingsService.setStorageSettings({ saveInterval: newValue })
      originalSettings.value.saveInterval = newValue
      $q.notify({
        type: 'positive',
        message: `배치 저장 간격이 ${newValue}ms로 변경되었습니다.`,
        timeout: 2000,
      })
    } catch (error) {
      console.error('Storage 설정 저장 실패:', error)
      $q.notify({
        type: 'negative',
        message: 'Storage 설정 저장에 실패했습니다.',
      })
      // 실패 시 이전 값으로 복원
      localSettings.value.saveInterval = oldValue
    } finally {
      loading.value = false
    }
  },
  { flush: 'post' }
)

// 마운트 시 설정 로드
onMounted(() => {
  void loadSettings()
})
</script>

<style scoped>
.storage-settings {
  max-width: 600px;
}

/* Light mode */
.info-card {
  background-color: rgba(33, 150, 243, 0.1);
}

.info-text {
  color: var(--q-dark);
}

/* Dark mode */
.body--dark .info-card {
  background-color: rgba(33, 150, 243, 0.15);
}

.body--dark .info-text {
  color: rgba(255, 255, 255, 0.9);
}

.body--dark .info-text p {
  color: rgba(255, 255, 255, 0.7);
}
</style>
