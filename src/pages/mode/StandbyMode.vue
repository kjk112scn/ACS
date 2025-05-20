<template>
  <div class="standby-mode">
    <div class="section-title text-h5 text-primary q-mb-sm">Standby Mode</div>
    <div class="standby-container">
      <q-card class="control-section">
        <q-card-section>
          <div class="text-subtitle1 text-weight-bold text-primary q-mb-md">Axis Selection</div>

          <!-- 체크박스 그룹 -->
          <div class="checkbox-group q-gutter-x-lg">
            <q-checkbox
              v-model="selectedAxes.azimuth"
              label="Azimuth"
              color="primary"
              class="axis-checkbox"
            />
            <q-checkbox
              v-model="selectedAxes.elevation"
              label="Elevation"
              color="primary"
              class="axis-checkbox"
            />
            <q-checkbox
              v-model="selectedAxes.tilt"
              label="Tilt"
              color="primary"
              class="axis-checkbox"
            />
          </div>

          <!-- 버튼 그룹 -->
          <div class="button-group q-mt-lg q-gutter-x-md">
            <q-btn
              color="primary"
              label="Standby"
              :disable="!isAnyAxisSelected"
              @click="handleStandby"
            />
            <q-btn color="secondary" label="All Standby" @click="handleAllStandby" />
            <q-btn color="negative" label="Stow" @click="handleStow" />
          </div>
        </q-card-section>
      </q-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useICDStore } from '../../stores/API/icdStore'
import { defineComponent } from 'vue'

// 컴포넌트 이름 정의
defineComponent({
  name: 'StandbyMode',
})

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()

// 선택된 축 상태 관리
const selectedAxes = ref({
  azimuth: false,
  elevation: false,
  tilt: false,
})

// 최소 하나의 축이 선택되었는지 확인
const isAnyAxisSelected = computed(() => {
  return selectedAxes.value.azimuth || selectedAxes.value.elevation || selectedAxes.value.tilt
})

// Standby 버튼 핸들러 (선택된 축 정지)
const handleStandby = async () => {
  try {
    // 선택된 축에 대해서만 정지 명령 전송
    await icdStore.stopCommand(
      selectedAxes.value.azimuth,
      selectedAxes.value.elevation,
      selectedAxes.value.tilt,
    )
    console.log('Standby 명령 전송 성공')
  } catch (error) {
    console.error('Standby 명령 전송 실패:', error)
  }
}

// All Standby 버튼 핸들러 (모든 축 정지)
const handleAllStandby = async () => {
  try {
    // 모든 축에 대해 정지 명령 전송
    await icdStore.stopCommand(true, true, true)
    console.log('All Standby 명령 전송 성공')
  } catch (error) {
    console.error('All Standby 명령 전송 실패:', error)
  }
}

// Stow 버튼 핸들러
const handleStow = async () => {
  try {
    await icdStore.stowCommand()
    console.log('Stow 명령 전송 성공')
  } catch (error) {
    console.error('Stow 명령 전송 실패:', error)
  }
}
</script>

<style scoped>
.standby-mode {
  height: 100%;
  width: 100%;
}

.standby-container {
  padding: 1rem;
  width: 100%;
  height: 100%;
}

.control-section {
  background-color: var(--q-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
  max-width: 400px;
  margin: 0 auto;
}

.checkbox-group {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  padding: 0.5rem;
}

.axis-checkbox {
  font-size: 1.1rem;
}

.button-group {
  display: flex;
  flex-direction: row;
  justify-content: center;
  gap: 0.5rem;
}

:deep(.q-checkbox__label) {
  font-size: 1.1rem;
  color: white;
}

:deep(.q-btn) {
  height: 42px;
  font-size: 1rem;
  min-width: 120px;
}
</style>
