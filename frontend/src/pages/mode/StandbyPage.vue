<template>
  <div class="mode-shell standby-mode">
    <div class="mode-shell__content standby-container">
      <q-card class="mode-card standby-card">
        <q-card-section>
          <!-- 체크박스 그룹 -->
          <div class="checkbox-group q-gutter-x-xl">
            <q-checkbox v-model="azimuthChecked" label="Azimuth" color="primary" class="axis-checkbox" size="lg" />
            <q-checkbox v-model="elevationChecked" label="Elevation" color="primary" class="axis-checkbox" size="lg" />
            <q-checkbox v-model="trainChecked" label="Tilt" color="primary" class="axis-checkbox" size="lg" />
          </div>

          <!-- 버튼 그룹 -->
          <div class="button-group q-mt-lg q-gutter-x-lg mode-button-bar">
            <q-btn color="primary" label="Standby" @click="handleStandby" size="lg" />
            <q-btn color="secondary" label="All Standby" @click="handleAllStandby" size="lg" />
            <q-btn color="warning" label="Stow" @click="handleStow" size="lg" />
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

const trainChecked = computed({
  get: () => standbyStore.selectedAxes.train,
  set: (value: boolean) => standbyStore.updateAxis('train', value),
})

// Standby 버튼 핸들러 - 선택된 축만 standby
const handleStandby = async () => {
  try {
    const axes = standbyStore.selectedAxes
    await icdStore.standbyCommand(axes.azimuth, axes.elevation, axes.train)
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
  width: 100%;
}

.standby-container {
  width: 100%;
  max-width: 960px;
}

.standby-card {
  padding: 2rem 3rem;
}

.checkbox-group {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  padding: 1rem 2rem;
  /* 패딩을 늘려서 공간 확장 */
  margin-bottom: 2rem;
  /* 하단 마진을 늘려서 버튼과의 간격 확장 */
}

.axis-checkbox {
  font-size: 1.4rem;
  /* 폰트 크기를 1.2rem → 1.4rem으로 증가 */
  margin: 0 1.5rem;
  /* 마진을 1rem → 1.5rem으로 증가하여 더 넓은 간격 */
}

.button-group {
  display: flex;
  flex-direction: row;
  justify-content: center;
  gap: 2rem;
  /* 버튼 간격을 1rem → 2rem으로 늘림 */
  padding: 1rem 2rem;
  /* 패딩을 늘려서 공간 확장 */
  margin-top: 0;
  /* 상단 마진 제거하여 체크박스와 더 가깝게 */
}

:deep(.q-checkbox__label) {
  font-size: 1.4rem;
  /* 폰트 크기를 1.2rem → 1.4rem으로 증가 */
  color: var(--theme-text);
  padding-left: 8px;
}

/* 버튼 스타일은 mode-common.scss의 .mode-button-bar .q-btn로 통일됨 */

/* 섹션 제목 스타일 */
.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
  margin-bottom: 0.5rem !important;
  /* 마진 줄임 */
}
</style>
