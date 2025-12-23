<template>
  <div class="location-settings">
    <h5 class="q-mt-none q-mb-md">
      위치 설정
      <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
        변경됨
      </q-badge>
      <q-badge v-else-if="isSaved" color="green" class="q-ml-sm">
        저장됨
      </q-badge>
    </h5>

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
        <q-btn type="submit" color="primary" label="저장" :loading="loadingStates.location"
          :disable="!isFormValid || !hasUnsavedChanges" icon="save" />
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
import { useLocationSettingsStore } from '@/stores'
import type { LocationSettings } from '@/services'

const $q = useQuasar()
const locationSettingsStore = useLocationSettingsStore()

// 스토어 상태 가져오기
const { loadingStates, errorStates, updateChangeStatus, pendingChanges } = locationSettingsStore

// 로컬 상태 - Store에서 변경된 값이 있으면 사용, 없으면 초기값
const getInitialLocalSettings = (): LocationSettings => {
  if (pendingChanges) {
    return { ...pendingChanges }
  }
  return {
    latitude: locationSettingsStore.locationSettings.latitude || 0,
    longitude: locationSettingsStore.locationSettings.longitude || 0,
    altitude: locationSettingsStore.locationSettings.altitude || 0
  }
}

const localSettings = ref<LocationSettings>(getInitialLocalSettings())

// 원본 상태 - Store에서 초기값 가져오기
const originalSettings = ref<LocationSettings>({
  latitude: locationSettingsStore.locationSettings.latitude || 0,
  longitude: locationSettingsStore.locationSettings.longitude || 0,
  altitude: locationSettingsStore.locationSettings.altitude || 0
})

// 변경사항 상태를 로컬 상태로 직접 계산
const hasUnsavedChanges = computed(() => {
  return JSON.stringify(localSettings.value) !== JSON.stringify(originalSettings.value)
})

// 저장 상태
const isSaved = ref(true)

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
  () => locationSettingsStore.locationSettings,
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
    const currentStoreData = locationSettingsStore.locationSettings
    const isInitialData = currentStoreData.latitude === 0 &&
      currentStoreData.longitude === 0 &&
      currentStoreData.altitude === 0

    if (isInitialData) {
      console.log('초기 데이터이므로 서버에서 로드')
      await locationSettingsStore.loadLocationSettings()
      // 초기 로드 시에는 원본 값 설정
      originalSettings.value = { ...locationSettingsStore.locationSettings }
      localSettings.value = { ...locationSettingsStore.locationSettings }
    } else {
      // Store에 이미 데이터가 있으면 그 값을 사용
      localSettings.value = { ...locationSettingsStore.locationSettings }
      originalSettings.value = { ...locationSettingsStore.locationSettings }
    }
  } catch (error) {
    console.error('위치 설정 로드 실패:', error)
  }
})

// 저장 함수
const onSave = async () => {
  try {
    await locationSettingsStore.saveLocationSettings(localSettings.value)

    // 저장 성공 시 변경사항 상태 업데이트
    updateChangeStatus(false)
    originalSettings.value = { ...localSettings.value }
    isSaved.value = true

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

// 초기화 함수 - 서버에서 로드된 값으로 초기화
const onReset = async () => {
  try {
    await locationSettingsStore.loadLocationSettings()
    localSettings.value = { ...locationSettingsStore.locationSettings }
    originalSettings.value = { ...locationSettingsStore.locationSettings }
    updateChangeStatus(false)
    isSaved.value = true

    $q.notify({
      color: 'info',
      message: '위치 설정이 서버 값으로 초기화되었습니다',
      icon: 'refresh',
      position: 'top'
    })
  } catch (error) {
    console.error('위치 설정 초기화 실패:', error)
  }
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
