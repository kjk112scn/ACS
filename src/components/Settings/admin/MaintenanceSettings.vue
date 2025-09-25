<template>
  <div class="maintenance-settings">
    <h5 class="q-mt-none q-mb-md">Maintenance</h5>

    <div class="row q-gutter-sm">
      <!-- Azimuth Maintenance Card -->
      <q-card class="col maintenance-card" flat bordered>
        <q-card-section class="q-pa-sm">
          <div class="text-subtitle1 text-weight-bold q-mb-sm text-center">Azimuth</div>

          <!-- 현재 각도 표시 -->
          <div class="current-angle-display q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Current Angle</div>
            <div class="text-h5 text-weight-bold text-center text-primary">
              {{ currentAngles.azimuth }}°
            </div>
          </div>

          <!-- 각도 조절 버튼들 (수평 배치) -->
          <div class="angle-controls">
            <div class="row q-gutter-xs no-wrap">
              <div class="col-6">
                <q-btn color="negative" icon="remove" size="md" class="full-width" @click="adjustAngle('azimuth', -1)"
                  :loading="loadingStates.azimuth" :disable="loadingStates.azimuth" />
              </div>
              <div class="col-6">
                <q-btn color="positive" icon="add" size="md" class="full-width" @click="adjustAngle('azimuth', 1)"
                  :loading="loadingStates.azimuth" :disable="loadingStates.azimuth" />
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Elevation Maintenance Card -->
      <q-card class="col maintenance-card" flat bordered>
        <q-card-section class="q-pa-sm">
          <div class="text-subtitle1 text-weight-bold q-mb-sm text-center">Elevation</div>

          <!-- 현재 각도 표시 -->
          <div class="current-angle-display q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Current Angle</div>
            <div class="text-h5 text-weight-bold text-center text-primary">
              {{ currentAngles.elevation }}°
            </div>
          </div>

          <!-- 각도 조절 버튼들 (수평 배치) -->
          <div class="angle-controls">
            <div class="row q-gutter-xs no-wrap">
              <div class="col-6">
                <q-btn color="negative" icon="remove" size="md" class="full-width" @click="adjustAngle('elevation', -1)"
                  :loading="loadingStates.elevation" :disable="loadingStates.elevation" />
              </div>
              <div class="col-6">
                <q-btn color="positive" icon="add" size="md" class="full-width" @click="adjustAngle('elevation', 1)"
                  :loading="loadingStates.elevation" :disable="loadingStates.elevation" />
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Tilt Maintenance Card -->
      <q-card class="col maintenance-card" flat bordered>
        <q-card-section class="q-pa-sm">
          <div class="text-subtitle1 text-weight-bold q-mb-sm text-center">Tilt</div>

          <!-- 현재 각도 표시 -->
          <div class="current-angle-display q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Current Angle</div>
            <div class="text-h5 text-weight-bold text-center text-primary">
              {{ currentAngles.tilt }}°
            </div>
          </div>

          <!-- 각도 조절 버튼들 (수평 배치) -->
          <div class="angle-controls">
            <div class="row q-gutter-xs no-wrap">
              <div class="col-6">
                <q-btn color="negative" icon="remove" size="md" class="full-width" @click="adjustAngle('tilt', -1)"
                  :loading="loadingStates.tilt" :disable="loadingStates.tilt" />
              </div>
              <div class="col-6">
                <q-btn color="positive" icon="add" size="md" class="full-width" @click="adjustAngle('tilt', 1)"
                  :loading="loadingStates.tilt" :disable="loadingStates.tilt" />
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 현재 각도 상태
const currentAngles = ref({
  azimuth: 0.0,
  elevation: 0.0,
  tilt: 0.0
})

// 로딩 상태
const loadingStates = ref({
  azimuth: false,
  elevation: false,
  tilt: false
})

// 각도 조절 함수 (UI용 - 나중에 실제 기능 구현)
const adjustAngle = (axis: 'azimuth' | 'elevation' | 'tilt', direction: number) => {
  loadingStates.value[axis] = true

  try {
    // TODO: 실제 각도 조절 API 호출
    console.log(`${axis} axis angle adjustment: ${direction > 0 ? '+' : ''}${direction}`)

    // 임시로 각도 변경 (실제 구현 시 제거)
    currentAngles.value[axis] += direction * 0.1
    currentAngles.value[axis] = Math.round(currentAngles.value[axis] * 10) / 10

  } catch (error) {
    console.error(`${axis} axis angle adjustment failed:`, error)
  } finally {
    loadingStates.value[axis] = false
  }
}
</script>

<style scoped>
.maintenance-settings {
  padding: 16px;
}

.maintenance-card {
  min-height: 160px;
  transition: all 0.3s ease;
}

.maintenance-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.current-angle-display {
  background: rgba(25, 118, 210, 0.1);
  border-radius: 6px;
  padding: 12px;
  border: 1px solid rgba(25, 118, 210, 0.2);
}

.angle-controls {
  margin-top: 12px;
}

.angle-controls .q-btn {
  height: 40px;
  font-weight: bold;
}

.angle-controls .q-btn--negative {
  background: #f44336;
}

.angle-controls .q-btn--positive {
  background: #4caf50;
}

.angle-controls .row {
  display: flex !important;
  flex-direction: row !important;
}

.angle-controls .col-6 {
  flex: 0 0 50% !important;
  max-width: 50% !important;
}
</style>
