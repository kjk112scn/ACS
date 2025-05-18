<template>
  <div class="standby-mode">
    <div class="section-title text-h5 text-primary q-mb-sm">Standby Mode</div>
    <div class="standby-container">
      <q-card class="control-section">
        <q-card-section>
          <div class="text-subtitle1 text-weight-bold text-primary q-mb-md">Axis Selection</div>

          <!-- 체크박스 그룹 -->
          <div class="checkbox-group q-gutter-x-xl">
            <q-checkbox
              v-model="selectedAxes.azimuth"
              label="Azimuth"
              color="primary"
              class="axis-checkbox"
              size="lg"
            />
            <q-checkbox
              v-model="selectedAxes.elevation"
              label="Elevation"
              color="primary"
              class="axis-checkbox"
              size="lg"
            />
            <q-checkbox
              v-model="selectedAxes.tilt"
              label="Tilt"
              color="primary"
              class="axis-checkbox"
              size="lg"
            />
          </div>

          <!-- 버튼 그룹 -->
          <div class="button-group q-mt-lg q-gutter-x-lg">
            <q-btn
              color="primary"
              label="Standby"
              :disable="!isAnyAxisSelected"
              @click="handleStow"
              size="lg"
            />
            <q-btn color="secondary" label="All Standby" @click="handleStow" size="lg" />
            <q-btn color="negative" label="Stow" @click="handleStow" size="lg" />
          </div>
        </q-card-section>
      </q-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useICDStore } from '../../stores/ICD'
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
/*
// Standby 버튼 핸들러
const handleStandby = async () => {
  try {
    // 선택된 축에 대해서만 Standby 명령 전송
     await icdStore.standbyCommand(
      selectedAxes.value.azimuth,
      selectedAxes.value.elevation,
      selectedAxes.value.tilt,
    )
    console.log('Standby 명령 전송 성공')
  } catch (error) {
    console.error('Standby 명령 전송 실패:', error)
  }
}

// All Standby 버튼 핸들러
const handleAllStandby = async () => {
  try {
    // 모든 축에 대해 Standby 명령 전송
    await icdStore.standbyCommand(true, true, true)
    console.log('All Standby 명령 전송 성공')
  } catch (error) {
    console.error('All Standby 명령 전송 실패:', error)
  }
}
 */
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
  max-width: 800px;
  margin: 0 auto;
  padding: 1rem;
}

.checkbox-group {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  padding: 1rem;
}

.axis-checkbox {
  font-size: 1.2rem;
  margin: 0 1rem;
}

.button-group {
  display: flex;
  flex-direction: row;
  justify-content: center;
  gap: 1rem;
  padding: 0.5rem;
}

:deep(.q-checkbox__label) {
  font-size: 1.2rem;
  color: white;
  padding-left: 8px;
}

:deep(.q-btn) {
  height: 48px;
  font-size: 1.1rem;
  min-width: 180px;
  padding: 0.5rem 2rem;
}
</style>
