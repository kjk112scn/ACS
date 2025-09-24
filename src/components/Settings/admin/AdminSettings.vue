<template>
  <div>
    <h5 class="q-mt-none q-mb-md">관리자 설정</h5>

    <!-- 서브 탭 -->
    <q-tabs v-model="activeSubTab" class="text-grey" active-color="primary" indicator-color="primary" align="left">
      <q-tab name="servo-preset" label="Servo Preset" />
      <q-tab name="servo-alarm-reset" label="Servo Alarm Reset" />
      <q-tab name="mc-onoff" label="M/C On/Off" />
    </q-tabs>

    <q-separator />

    <!-- 서브 탭 컨텐츠 -->
    <div class="q-mt-md">
      <ServoEncoderPresetSettings v-if="activeSubTab === 'servo-preset'" />
      <ServoAlarmResetSettings v-if="activeSubTab === 'servo-alarm-reset'" />
      <MCOffSettings v-if="activeSubTab === 'mc-onoff'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ServoEncoderPresetSettings, ServoAlarmResetSettings, MCOffSettings } from '../../../components'

// 서브 탭 상태
const activeSubTab = ref('servo-preset')
</script>

<style scoped>
/* 추가 스타일이 필요한 경우 */
</style>
