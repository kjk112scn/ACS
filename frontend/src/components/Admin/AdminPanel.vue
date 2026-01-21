<template>
  <q-dialog v-model="isOpen" @hide="onHide" maximized>
    <q-card class="admin-panel-card">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">
          <q-icon name="admin_panel_settings" class="q-mr-sm" />
          {{ T.settings.admin.title }}
        </div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>

      <!-- 경고 배너 -->
      <q-banner class="warning-banner bg-warning text-dark q-mx-md q-mt-md">
        <template v-slot:avatar>
          <q-icon name="warning" color="dark" />
        </template>
        <div class="text-weight-medium">
          {{ T.settings.admin.warningMessage }}
        </div>
        <div class="text-caption q-mt-xs">
          {{ T.settings.admin.warningDescription }}
        </div>
      </q-banner>

      <q-card-section class="admin-content q-pa-md">
        <!-- 탭 -->
        <q-tabs v-model="activeSubTab" class="text-grey" active-color="primary" indicator-color="primary" align="left">
          <q-tab name="servoPreset" label="Servo Preset" />
          <q-tab name="servoAlarmReset" label="Servo Alarm Reset" />
          <q-tab name="mcOnOff" label="M/C On/Off" />
          <q-tab name="maintenance" label="Maintenance" />
        </q-tabs>

        <!-- 탭 내용 -->
        <q-tab-panels v-model="activeSubTab" animated class="q-mt-md">
          <q-tab-panel name="servoPreset">
            <ServoEncoderPresetSettings />
          </q-tab-panel>

          <q-tab-panel name="servoAlarmReset">
            <ServoAlarmResetSettings />
          </q-tab-panel>

          <q-tab-panel name="mcOnOff">
            <MCOffSettings />
          </q-tab-panel>

          <q-tab-panel name="maintenance">
            <MaintenanceSettings />
          </q-tab-panel>
        </q-tab-panels>
      </q-card-section>

      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat :label="T.buttons.close" color="primary" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { T } from '@/texts'

// 기존 admin 컴포넌트 재사용
import ServoEncoderPresetSettings from '@/components/Settings/admin/ServoEncoderPresetSettings.vue'
import ServoAlarmResetSettings from '@/components/Settings/admin/ServoAlarmResetSettings.vue'
import MCOffSettings from '@/components/Settings/admin/MCOffSettings.vue'
import MaintenanceSettings from '@/components/Settings/admin/MaintenanceSettings.vue'

// Props 정의
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
})

// Emits 정의
const emit = defineEmits(['update:modelValue'])

// 로컬 상태 관리
const isOpen = ref(props.modelValue)
const activeSubTab = ref('servoPreset')

// props 변경 감지
watch(
  () => props.modelValue,
  (newVal) => {
    isOpen.value = newVal
  },
)

// 내부 상태 변경 감지 및 부모에게 알림
watch(isOpen, (newVal) => {
  emit('update:modelValue', newVal)
})

// 모달 닫힘 이벤트 처리
const onHide = () => {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.admin-panel-card {
  width: 90vw;
  max-width: 1200px;
  height: 80vh;
  max-height: 800px;
  display: flex;
  flex-direction: column;
}

.warning-banner {
  border-radius: 8px;
}

.admin-content {
  flex: 1;
  overflow-y: auto;
}
</style>