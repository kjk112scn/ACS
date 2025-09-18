<template>
  <div class="system-settings">
    <div class="row q-gutter-md">
      <!-- 좌측: 설정 카테고리 목록 -->
      <div class="col-3">
        <q-list padding>
          <q-item v-for="category in settingsCategories" :key="category.id" clickable v-ripple
            :active="activeCategory === category.id" @click="activeCategory = category.id"
            active-class="active-category">
            <q-item-section avatar>
              <q-icon :name="category.icon" />
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ category.name }}</q-item-label>
              <q-item-label caption>{{ category.description }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </div>

      <!-- 우측: 선택된 설정 컴포넌트 -->
      <div class="col">
        <div class="settings-content">
          <!-- 위치 설정 -->
          <LocationSettings v-if="activeCategory === 'location'" />

          <!-- 추적 설정 -->
          <TrackingSettings v-if="activeCategory === 'tracking'" />

          <!-- Stow 설정 -->
          <StowSettings v-if="activeCategory === 'stow'" />

          <!-- 안테나 사양 설정 -->
          <AntennaSpecSettings v-if="activeCategory === 'antenna-spec'" />

          <!-- 각도 제한 설정 -->
          <AngleLimitsSettings v-if="activeCategory === 'angle-limits'" />

          <!-- 속도 제한 설정 -->
          <SpeedLimitsSettings v-if="activeCategory === 'speed-limits'" />

          <!-- 오프셋 제한 설정 -->
          <OffsetLimitsSettings v-if="activeCategory === 'offset-limits'" />

          <!-- 알고리즘 설정 -->
          <AlgorithmSettings v-if="activeCategory === 'algorithm'" />

          <!-- 스텝 사이즈 제한 설정 -->
          <StepSizeLimitSettings v-if="activeCategory === 'step-size'" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSettingsStore } from '../../../stores/settingsStore'

// 개별 설정 컴포넌트들 import
import LocationSettings from './LocationSettings.vue'
import TrackingSettings from './TrackingSettings.vue'
import StowSettings from './StowSettings.vue'
import AntennaSpecSettings from './AntennaSpecSettings.vue'
import AngleLimitsSettings from './AngleLimitsSettings.vue'
import SpeedLimitsSettings from './SpeedLimitsSettings.vue'
import OffsetLimitsSettings from './OffsetLimitsSettings.vue'
import AlgorithmSettings from './AlgorithmSettings.vue'
import StepSizeLimitSettings from './StepSizeLimitSettings.vue'

const settingsStore = useSettingsStore()

// 활성 카테고리
const activeCategory = ref('location')

// 설정 카테고리 목록
const settingsCategories = [
  {
    id: 'location',
    name: '위치 설정',
    description: '위도, 경도, 고도 설정',
    icon: 'place'
  },
  {
    id: 'tracking',
    name: '추적 설정',
    description: '추적 간격, 기간, 최소고도각',
    icon: 'track_changes'
  },
  {
    id: 'stow',
    name: 'Stow 설정',
    description: 'Stow 각도 및 속도 설정',
    icon: 'home_repair_service'
  },
  {
    id: 'antenna-spec',
    name: '안테나 사양',
    description: 'True North Offset, Tilt Angle',
    icon: 'settings_input_antenna'
  },
  {
    id: 'angle-limits',
    name: '각도 제한',
    description: 'Azimuth, Elevation, Train 제한',
    icon: 'rotate_3d'
  },
  {
    id: 'speed-limits',
    name: '속도 제한',
    description: 'Azimuth, Elevation, Train 속도 제한',
    icon: 'speed'
  },
  {
    id: 'offset-limits',
    name: '오프셋 제한',
    description: '각도 오프셋, 시간 오프셋 제한',
    icon: 'tune'
  },
  {
    id: 'algorithm',
    name: '알고리즘 설정',
    description: 'Geo Min Motion 등 알고리즘 파라미터',
    icon: 'psychology'
  },
  {
    id: 'step-size',
    name: '스텝 사이즈 제한',
    description: '안테나 이동 스텝 사이즈 제한',
    icon: 'straighten'
  }
]

// 컴포넌트 마운트 시 모든 설정 로드
onMounted(async () => {
  await settingsStore.loadAllSettings()
})
</script>

<style scoped>
.system-settings {
  height: 100%;
}

.settings-content {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.active-category {
  background-color: rgba(0, 0, 0, 0.1);
  color: var(--q-primary);
  font-weight: 500;
}

.body--dark .active-category {
  background-color: rgba(255, 255, 255, 0.1);
}

.q-list {
  border-right: 1px solid rgba(0, 0, 0, 0.12);
  background-color: rgba(0, 0, 0, 0.02);
}

.body--dark .q-list {
  border-right: 1px solid rgba(255, 255, 255, 0.12);
  background-color: rgba(255, 255, 255, 0.03);
}
</style>
