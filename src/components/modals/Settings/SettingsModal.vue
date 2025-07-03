<template>
  <q-dialog v-model="isOpen" @hide="onHide" maximized>
    <q-card class="settings-modal-card">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Settings</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>

      <q-card-section class="settings-content q-pa-none">
        <div class="row no-wrap" style="height: 100%">
          <!-- 좌측 탭 컨트롤 -->
          <div class="col-3 col-md-2 settings-tabs">
            <q-list padding>
              <q-item
                clickable
                v-ripple
                :active="activeTab === 'general'"
                @click="activeTab = 'general'"
                active-class="active-tab"
              >
                <q-item-section avatar>
                  <q-icon name="settings" />
                </q-item-section>
                <q-item-section>일반 설정</q-item-section>
              </q-item>

              <q-item
                clickable
                v-ripple
                :active="activeTab === 'servo-preset'"
                @click="activeTab = 'servo-preset'"
                active-class="active-tab"
              >
                <q-item-section avatar>
                  <q-icon name="tune" />
                </q-item-section>
                <q-item-section>Servo Preset</q-item-section>
              </q-item>

              <q-item
                clickable
                v-ripple
                :active="activeTab === 'connection'"
                @click="activeTab = 'connection'"
                active-class="active-tab"
              >
                <q-item-section avatar>
                  <q-icon name="wifi" />
                </q-item-section>
                <q-item-section>연결 설정</q-item-section>
              </q-item>
            </q-list>
          </div>

          <!-- 우측 컨텐츠 영역 -->
          <div class="col settings-content-area q-pa-md">
            <!-- 일반 설정 탭 -->

            <GeneralSettings
              v-if="activeTab === 'general'"
              :dark-mode="localDarkMode"
              @update:dark-mode="localDarkMode = $event"
            />

            <!-- Server Preset 탭 -->

            <ServoEncoderPresetSettings v-if="activeTab === 'servo-preset'" />

            <!-- 연결 설정 탭 -->
            <div v-if="activeTab === 'connection'">
              <h5 class="q-mt-none q-mb-md">연결 설정</h5>

              <q-input
                outlined
                v-model="localServerAddress"
                label="WebSocket 서버 주소"
                class="q-mb-md"
              />

              <q-input outlined v-model="apiBaseUrl" label="API 기본 URL" class="q-mb-md" />

              <q-toggle v-model="autoReconnect" label="연결 끊김 시 자동 재연결" />
            </div>
          </div>
        </div>
      </q-card-section>

      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat label="취소" color="primary" v-close-popup />
        <q-btn flat label="저장" color="primary" @click="saveSettings" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch, defineProps, defineEmits } from 'vue'
import { useQuasar } from 'quasar'

import GeneralSettings from './GeneralSettings.vue'
import ServoEncoderPresetSettings from './ServoEncoderPresetSettings.vue'

const $q = useQuasar()

// Props 정의
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  darkMode: {
    type: Boolean,
    default: false,
  },
  serverAddress: {
    type: String,
    default: 'ws://localhost:8080/ws/push-data',
  },
})

// Emits 정의
const emit = defineEmits(['update:modelValue', 'save'])

// 로컬 상태 관리
const isOpen = ref(props.modelValue)
const localDarkMode = ref($q.dark.isActive)
const localServerAddress = ref(props.serverAddress)
const activeTab = ref('general')
const apiBaseUrl = ref('http://localhost:8080/api')
const autoReconnect = ref(true)

// props 변경 감지
watch(
  () => props.modelValue,
  (newVal) => {
    isOpen.value = newVal
  },
)

watch(
  () => props.darkMode,
  (newVal) => {
    localDarkMode.value = newVal
  },
)

watch(
  () => props.serverAddress,
  (newVal) => {
    localServerAddress.value = newVal
  },
)

// 모달 닫힘 이벤트 처리
const onHide = () => {
  emit('update:modelValue', false)
}

// 설정 저장
const saveSettings = () => {
  // Quasar 다크 모드 설정 직접 변경
  $q.dark.set(localDarkMode.value)

  // 로컬 스토리지에 설정 저장
  localStorage.setItem('isDarkMode', String(localDarkMode.value))

  // 부모 컴포넌트에 저장 이벤트 발생
  emit('save', {
    darkMode: localDarkMode.value,
    serverAddress: localServerAddress.value,
    apiBaseUrl: apiBaseUrl.value,
    autoReconnect: autoReconnect.value,
    // 여기에 다른 설정들도 추가할 수 있습니다
  })

  // 저장 완료 알림
  $q.notify({
    color: 'positive',
    message: '설정이 저장되었습니다',
    icon: 'check',
    position: 'top',
  })
}

// 내부 상태 변경 감지 및 부모에게 알림
watch(isOpen, (newVal) => {
  emit('update:modelValue', newVal)
})
</script>

<style scoped>
.settings-modal-card {
  width: 90vw;
  max-width: 1200px;
  height: 80vh;
  max-height: 800px;
  display: flex;
  flex-direction: column;
}

.settings-content {
  flex: 1;
  overflow: hidden;
}

.settings-tabs {
  border-right: 1px solid rgba(0, 0, 0, 0.12);
  background-color: rgba(0, 0, 0, 0.02);
}

.body--dark .settings-tabs {
  border-right: 1px solid rgba(255, 255, 255, 0.12);
  background-color: rgba(255, 255, 255, 0.03);
}

.settings-content-area {
  overflow-y: auto;
}

.active-tab {
  background-color: rgba(0, 0, 0, 0.1);
  color: var(--q-primary);
  font-weight: 500;
}

.body--dark .active-tab {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>
