<template>
  <div class="location-settings">
    <h5 class="q-mt-none q-mb-md">위치 설정</h5>

    <q-form @submit="onSave" class="q-gutter-md">
      <!-- 위도 입력 -->
      <q-input v-model.number="localSettings.latitude" label="위도 (도)" type="number" :rules="latitudeRules" outlined
        :loading="loadingStates.location" hint="위도 범위: -90도 ~ 90도" suffix="°" />

      <!-- 경도 입력 -->
      <q-input v-model.number="localSettings.longitude" label="경도 (도)" type="number" :rules="longitudeRules" outlined
        :loading="loadingStates.location" hint="경도 범위: -180도 ~ 180도" suffix="°" />

      <!-- 고도 입력 -->
      <q-input v-model.number="localSettings.altitude" label="고도 (미터)" type="number" :rules="altitudeRules" outlined
        :loading="loadingStates.location" hint="고도는 0미터 이상이어야 합니다" suffix="m" />

      <!-- 버튼들 -->
      <div class="row q-gutter-sm q-mt-md">
        <q-btn type="submit" color="primary" label="저장" :loading="loadingStates.location" :disable="!isFormValid"
          icon="save" />
        <q-btn color="secondary" label="초기화" @click="onReset" :disable="loadingStates.location" icon="refresh" />
        <q-btn color="info" label="현재 위치 가져오기" @click="onGetCurrentLocation" :disable="loadingStates.location"
          icon="my_location" />
      </div>
    </q-form>

    <!-- 에러 메시지 -->
    <q-banner v-if="errorStates.location" class="bg-negative text-white q-mt-md">
      <template v-slot:avatar>
        <q-icon name="error" />
      </template>
      {{ errorStates.location }}
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
import type { LocationSettings } from '../../../services/settingsService'

const $q = useQuasar()
const settingsStore = useSettingsStore()

// 로컬 상태
const localSettings = ref<LocationSettings>({
  latitude: 35.317540,
  longitude: 128.608510,
  altitude: 0.0
})

const successMessage = ref<string>('')

// 유효성 검사 규칙
const latitudeRules = [
  (val: number) => val !== null && val !== undefined || '위도는 필수입니다',
  (val: number) => val >= -90 && val <= 90 || '위도는 -90도 ~ 90도 범위여야 합니다'
]

const longitudeRules = [
  (val: number) => val !== null && val !== undefined || '경도는 필수입니다',
  (val: number) => val >= -180 && val <= 180 || '경도는 -180도 ~ 180도 범위여야 합니다'
]

const altitudeRules = [
  (val: number) => val !== null && val !== undefined || '고도는 필수입니다',
  (val: number) => val >= 0 || '고도는 0미터 이상이어야 합니다'
]

// 폼 유효성 검사
const isFormValid = computed(() => {
  return localSettings.value.latitude >= -90 && localSettings.value.latitude <= 90 &&
    localSettings.value.longitude >= -180 && localSettings.value.longitude <= 180 &&
    localSettings.value.altitude >= 0
})

// 스토어 상태 가져오기
const { locationSettings, loadingStates, errorStates } = settingsStore

// 스토어 상태와 로컬 상태 동기화
watch(locationSettings, (newSettings) => {
  localSettings.value = { ...newSettings }
}, { deep: true })

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  await settingsStore.loadLocationSettings()
})

// 저장 함수
const onSave = async () => {
  try {
    await settingsStore.saveLocationSettings(localSettings.value)
    successMessage.value = '위치 설정이 성공적으로 저장되었습니다'

    // 3초 후 성공 메시지 숨기기
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

    $q.notify({
      color: 'positive',
      message: '위치 설정이 저장되었습니다',
      icon: 'check',
      position: 'top'
    })
  } catch (error) {
    console.error('위치 설정 저장 실패:', error)
  }
}

// 초기화 함수
const onReset = () => {
  localSettings.value = {
    latitude: 35.317540,
    longitude: 128.608510,
    altitude: 0.0
  }

  $q.notify({
    color: 'info',
    message: '위치 설정이 초기화되었습니다',
    icon: 'refresh',
    position: 'top'
  })
}

// 현재 위치 가져오기 함수
const onGetCurrentLocation = () => {
  if (!navigator.geolocation) {
    $q.notify({
      color: 'negative',
      message: '이 브라우저는 위치 서비스를 지원하지 않습니다',
      icon: 'error',
      position: 'top'
    })
    return
  }

  $q.loading.show({
    message: '현재 위치를 가져오는 중...'
  })

  navigator.geolocation.getCurrentPosition(
    (position) => {
      localSettings.value = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        altitude: position.coords.altitude || 0
      }

      $q.loading.hide()

      $q.notify({
        color: 'positive',
        message: '현재 위치를 가져왔습니다',
        icon: 'my_location',
        position: 'top'
      })
    },
    (error) => {
      $q.loading.hide()

      let message = '위치를 가져올 수 없습니다'
      switch (error.code) {
        case error.PERMISSION_DENIED:
          message = '위치 접근 권한이 거부되었습니다'
          break
        case error.POSITION_UNAVAILABLE:
          message = '위치 정보를 사용할 수 없습니다'
          break
        case error.TIMEOUT:
          message = '위치 요청 시간이 초과되었습니다'
          break
      }

      $q.notify({
        color: 'negative',
        message,
        icon: 'error',
        position: 'top'
      })
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 60000
    }
  )
}
</script>

<style scoped>
.location-settings {
  max-width: 500px;
}

.q-input {
  margin-bottom: 16px;
}

.q-banner {
  border-radius: 4px;
}
</style>
