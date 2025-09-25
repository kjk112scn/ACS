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
              <q-item clickable v-ripple :active="activeTab === 'general'" @click="activeTab = 'general'"
                active-class="active-tab">
                <q-item-section avatar>
                  <q-icon name="settings" />
                </q-item-section>
                <q-item-section>일반 설정</q-item-section>
              </q-item>

              <q-item clickable v-ripple :active="activeTab === 'connection'" @click="activeTab = 'connection'"
                active-class="active-tab">
                <q-item-section avatar>
                  <q-icon name="wifi" />
                </q-item-section>
                <q-item-section>연결 설정</q-item-section>
              </q-item>

              <!-- 시스템 설정 탭 -->
              <q-item clickable v-ripple :active="activeTab === 'system'" @click="activeTab = 'system'"
                active-class="active-tab">
                <q-item-section avatar>
                  <q-icon name="engineering" />
                </q-item-section>
                <q-item-section>시스템 설정</q-item-section>
              </q-item>

              <!-- ✅ 관리자 설정 탭 추가 -->
              <q-item clickable v-ripple :active="activeTab === 'admin'" @click="activeTab = 'admin'"
                active-class="active-tab">
                <q-item-section avatar>
                  <q-icon name="admin_panel_settings" />
                </q-item-section>
                <q-item-section>관리자 설정</q-item-section>
              </q-item>

              <!-- ✅ 버전 정보 탭 추가 -->
              <q-item clickable v-ripple :active="activeTab === 'version'" @click="activeTab = 'version'"
                active-class="active-tab">
                <q-item-section avatar>
                  <q-icon name="info" />
                </q-item-section>
                <q-item-section>버전 정보</q-item-section>
              </q-item>
            </q-list>
          </div>

          <!-- 우측 컨텐츠 영역 -->
          <div class="col settings-content-area q-pa-md">
            <!-- 일반 설정 탭 -->
            <GeneralSettings v-if="activeTab === 'general'" :dark-mode="localDarkMode"
              @update:dark-mode="localDarkMode = $event" />

            <!-- 연결 설정 탭 -->
            <div v-if="activeTab === 'connection'">
              <h5 class="q-mt-none q-mb-md">연결 설정</h5>

              <q-input outlined v-model="localServerAddress" label="WebSocket 서버 주소" class="q-mb-md" />

              <q-input outlined v-model="apiBaseUrl" label="API 기본 URL" class="q-mb-md" />

              <q-toggle v-model="autoReconnect" label="연결 끊김 시 자동 재연결" />
            </div>

            <!-- 시스템 설정 탭 -->
            <SystemSettings v-if="activeTab === 'system'" />

            <!-- ✅ 관리자 설정 탭 -->
            <AdminSettings v-if="activeTab === 'admin'" />

            <!-- ✅ 버전 정보 탭 -->
            <VersionInfoSettings v-if="activeTab === 'version'" />
          </div>
        </div>
      </q-card-section>

      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat label="취소" color="primary" v-close-popup />
        <q-btn flat label="저장" color="primary" :loading="loadingStates.saveAll" :disable="!hasAnyUnsavedChanges"
          @click="onSaveAll" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch, defineProps, defineEmits, computed } from 'vue'
import { useSettingsStore } from '@/stores'
import { useNotification } from '@/composables/useNotification'

import GeneralSettings from './GeneralSettings.vue'
import SystemSettings from './system/SystemSettings.vue'
import AdminSettings from './admin/AdminSettings.vue'
import VersionInfoSettings from './VersionInfoSettings.vue'

const settingsStore = useSettingsStore()
const { success, error: showError } = useNotification()

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
const localDarkMode = ref(false)
const localServerAddress = ref(props.serverAddress)
const activeTab = ref('general')
const apiBaseUrl = ref('http://localhost:8080/api')
const autoReconnect = ref(true)

// Store 상태 가져오기
const { loadingStates } = settingsStore

// 변경사항 감지를 위한 로컬 computed
const hasAnyUnsavedChanges = computed(() => {
  // 시스템 설정 탭이 활성화되어 있을 때만 Store의 변경사항 확인
  if (activeTab.value === 'system') {
    return settingsStore.hasAnyUnsavedChanges
  }

  // 다른 탭에서는 기존 설정 변경사항 확인
  return localDarkMode.value !== props.darkMode ||
    localServerAddress.value !== props.serverAddress ||
    apiBaseUrl.value !== 'http://localhost:8080/api' ||
    autoReconnect.value !== true
})

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

// 일괄 저장 함수
const onSaveAll = async () => {
  try {
    // 시스템 설정 탭이 활성화되어 있으면 Store의 일괄 저장 실행
    if (activeTab.value === 'system') {
      await settingsStore.saveAllSettings()
    }

    // 기존 설정도 함께 저장
    localStorage.setItem('isDarkMode', String(localDarkMode.value))

    // 부모 컴포넌트에 저장 이벤트 발생
    emit('save', {
      darkMode: localDarkMode.value,
      serverAddress: localServerAddress.value,
      apiBaseUrl: apiBaseUrl.value,
      autoReconnect: autoReconnect.value,
    })

    // ✅ useNotification 사용
    success('모든 설정이 저장되었습니다')

    // 모달 닫기
    isOpen.value = false
  } catch (error) {
    console.error('일괄 저장 실패:', error)
    // ✅ useNotification 사용
    showError('설정 저장에 실패했습니다')
  }
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
