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
              v-model="azimuthChecked"
              label="Azimuth"
              color="primary"
              class="axis-checkbox"
              size="lg"
            />
            <q-checkbox
              v-model="elevationChecked"
              label="Elevation"
              color="primary"
              class="axis-checkbox"
              size="lg"
            />
            <q-checkbox
              v-model="tiltChecked"
              label="Tilt"
              color="primary"
              class="axis-checkbox"
              size="lg"
            />
          </div>

          <!-- 버튼 그룹 -->
          <div class="button-group q-mt-lg q-gutter-x-lg">
            <q-btn color="primary" label="Standby" @click="handleStandby" size="lg" />
            <q-btn color="secondary" label="All Standby" @click="handleAllStandby" size="lg" />
            <q-btn color="negative" label="Stow" @click="handleStow" size="lg" />
          </div>
        </q-card-section>
      </q-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { useStandbyModeStore } from '../../stores/mode/standbyStore'
import { defineComponent } from 'vue'

// 컴포넌트 이름 정의
defineComponent({
  name: 'StandbyMode',
})

// 스토어 인스턴스 생성
const icdStore = useICDStore()
const standbyStore = useStandbyModeStore()

// 체크박스 상태를 computed로 양방향 바인딩
const azimuthChecked = computed({
  get: () => standbyStore.selectedAxes.azimuth,
  set: (value: boolean) => standbyStore.updateAxis('azimuth', value),
})

const elevationChecked = computed({
  get: () => standbyStore.selectedAxes.elevation,
  set: (value: boolean) => standbyStore.updateAxis('elevation', value),
})

const tiltChecked = computed({
  get: () => standbyStore.selectedAxes.tilt,
  set: (value: boolean) => standbyStore.updateAxis('tilt', value),
})

// Standby 버튼 핸들러 - 선택된 축만 standby
const handleStandby = async () => {
  try {
    const axes = standbyStore.selectedAxes
    await icdStore.standbyCommand(axes.azimuth, axes.elevation, axes.tilt)
    console.log('Standby 명령 전송 성공')
  } catch (error) {
    console.error('Standby 명령 전송 실패:', error)
  }
}

// 기존 코드 유지
const handleAllStandby = async () => {
  try {
    await icdStore.standbyCommand(true, true, true)
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
