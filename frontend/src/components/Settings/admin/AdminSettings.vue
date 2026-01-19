<template>
  <div class="admin-settings">
    <h5 class="q-mt-none q-mb-md">{{ T.settings.admin.title }}</h5>

    <!-- ✅ 탭 라벨을 하드코딩된 영어로 변경 -->
    <q-tabs v-model="activeSubTab" class="text-grey" active-color="primary" indicator-color="primary" align="left">
      <q-tab name="servoPreset" label="Servo Preset" />
      <q-tab name="servoAlarmReset" label="Servo Alarm Reset" />
      <q-tab name="mcOnOff" label="M/C On/Off" />
      <q-tab name="maintenance" label="Maintenance" />
    </q-tabs>

    <!-- 탭 내용 -->
    <q-tab-panels v-model="activeSubTab" animated>
      <q-tab-panel name="servoPreset">
        <ServoEncoderPresetSettings />
      </q-tab-panel>

      <q-tab-panel name="servoAlarmReset">
        <ServoAlarmResetSettings />
      </q-tab-panel>

      <q-tab-panel name="mcOnOff">
        <MCOffSettings />
      </q-tab-panel>

      <!-- ✅ Maintenance 탭 추가 -->
      <q-tab-panel name="maintenance">
        <MaintenanceSettings />
      </q-tab-panel>
    </q-tab-panels>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { T } from '@/texts'
import ServoEncoderPresetSettings from './ServoEncoderPresetSettings.vue'
import ServoAlarmResetSettings from './ServoAlarmResetSettings.vue'
import MCOffSettings from './MCOffSettings.vue'
// ✅ MaintenanceSettings 컴포넌트 import 추가
import MaintenanceSettings from './MaintenanceSettings.vue'

const activeSubTab = ref('servoPreset')
</script>

<style scoped>
/* 추가 스타일이 필요한 경우 */
</style>
