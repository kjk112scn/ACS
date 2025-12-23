<template>
  <div class="maintenance-settings">
    <h5 class="q-mt-none q-mb-md">Maintenance</h5>

    <!-- 리미트 스위치 테스트 모드 토글 -->
    <div class="test-mode-toggle q-mb-md">
      <q-card flat bordered class="q-pa-sm">
        <div class="row items-center q-gutter-sm">
          <div class="col-auto">
            <q-icon name="science" color="orange" size="sm" />
          </div>
          <div class="col">
            <div class="text-caption text-grey-6">Test Mode</div>
            <div class="text-subtitle2">리미트 스위치 시뮬레이션</div>
          </div>
          <div class="col-auto">
            <q-toggle
              v-model="useTestMode"
              color="orange"
              @update:model-value="onTestModeToggle" />
          </div>
        </div>
      </q-card>
    </div>

    <!-- 디버깅 정보 표시 -->
    <div class="debug-info q-mb-md">
      <q-card flat bordered class="q-pa-sm">
        <div class="text-subtitle2 q-mb-sm">🔍 디버깅 정보</div>
        <div class="row q-gutter-sm">
          <div class="col-4">
            <div class="text-caption text-grey-6">현재 각도</div>
            <div class="text-h6">{{ currentAngles.azimuth.toFixed(1) }}°</div>
          </div>
          <div class="col-4">
            <div class="text-caption text-grey-6">목표 각도</div>
            <div class="text-h6">{{ targetAngles.azimuth || 'N/A' }}°</div>
          </div>
          <div class="col-4">
            <div class="text-caption text-grey-6">현재 속도</div>
            <div class="text-h6">{{ currentSpeeds.azimuth.toFixed(1) }}°/s</div>
          </div>
        </div>
        <div class="row q-gutter-sm q-mt-sm">
          <div class="col-4">
            <div class="text-caption text-grey-6">이동 상태</div>
            <q-chip :color="movingAxes.azimuth ? 'positive' : 'grey'" size="sm">
              {{ movingAxes.azimuth ? 'MOVING' : 'STOPPED' }}
            </q-chip>
          </div>
          <div class="col-4">
            <div class="text-caption text-grey-6">속도 조절</div>
            <q-chip :color="speedAdjustmentIntervals.azimuth ? 'positive' : 'grey'" size="sm">
              {{ speedAdjustmentIntervals.azimuth ? 'ACTIVE' : 'INACTIVE' }}
            </q-chip>
          </div>
          <div class="col-4">
            <div class="text-caption text-grey-6">속도 범위</div>
            <div class="text-caption">{{ getSpeedRange(currentAngles.azimuth, 'azimuth') }}</div>
          </div>
        </div>
      </q-card>
    </div>

    <div class="row q-gutter-sm">
      <!-- Azimuth Maintenance Card -->
      <q-card class="col maintenance-card" flat bordered>
        <q-card-section class="q-pa-sm">
          <div class="text-subtitle1 text-weight-bold q-mb-sm text-center">Azimuth</div>

          <!-- 리미트 스위치 상태 표시 -->
          <div class="limit-switch-status q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Limit Switch Status</div>
            <div class="row q-gutter-xs">
              <div class="col-6 text-center">
                <q-chip
                  :color="azimuthLimitStatus.positiveLimit ? 'negative' : 'positive'"
                  text-color="white"
                  size="sm"
                  :icon="azimuthLimitStatus.positiveLimit ? 'warning' : 'check'">
                  +275° {{ azimuthLimitStatus.positiveLimit ? 'ACTIVE' : 'NORMAL' }}
                </q-chip>
              </div>
              <div class="col-6 text-center">
                <q-chip
                  :color="azimuthLimitStatus.negativeLimit ? 'negative' : 'positive'"
                  text-color="white"
                  size="sm"
                  :icon="azimuthLimitStatus.negativeLimit ? 'warning' : 'check'">
                  -275° {{ azimuthLimitStatus.negativeLimit ? 'ACTIVE' : 'NORMAL' }}
                </q-chip>
              </div>
            </div>
          </div>

          <!-- 테스트 모드일 때 리미트 스위치 토글 버튼들 -->
          <div v-if="useTestMode" class="test-controls q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Test Controls</div>
            <div class="row q-gutter-xs">
              <div class="col-6">
                <q-btn :color="testLimitStatus.azimuth.positiveLimit ? 'negative' : 'positive'"
                  :label="testLimitStatus.azimuth.positiveLimit ? '+275° ON' : '+275° OFF'" size="sm" class="full-width"
                  @click="toggleTestLimit('azimuth', 'positive')" />
              </div>
              <div class="col-6">
                <q-btn :color="testLimitStatus.azimuth.negativeLimit ? 'negative' : 'positive'"
                  :label="testLimitStatus.azimuth.negativeLimit ? '-275° ON' : '-275° OFF'" size="sm" class="full-width"
                  @click="toggleTestLimit('azimuth', 'negative')" />
              </div>
            </div>
          </div>

          <!-- 현재 각도 표시 -->
          <div class="current-angle-display q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Current Angle</div>
            <div class="text-h5 text-weight-bold text-center text-primary">
              {{ currentAngles.azimuth.toFixed(1) }}°
            </div>
          </div>

          <!-- 각도 조절 버튼들 (수평 배치) -->
          <div class="angle-controls">
            <div class="row q-gutter-xs no-wrap">
              <div class="col-6">
                <q-btn color="negative" icon="remove" size="md" class="full-width"
                  @mousedown="startMovement('azimuth', -1)" @mouseup="stopMovement('azimuth')"
                  @mouseleave="stopMovement('azimuth')" @touchstart="startMovement('azimuth', -1)"
                  @touchend="stopMovement('azimuth')" :disable="azimuthLimitStatus.negativeLimit" />
              </div>
              <div class="col-6">
                <q-btn color="positive" icon="add" size="md" class="full-width" @mousedown="startMovement('azimuth', 1)"
                  @mouseup="stopMovement('azimuth')" @mouseleave="stopMovement('azimuth')"
                  @touchstart="startMovement('azimuth', 1)" @touchend="stopMovement('azimuth')"
                  :disable="azimuthLimitStatus.positiveLimit" />
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Elevation Maintenance Card -->
      <q-card class="col maintenance-card" flat bordered>
        <q-card-section class="q-pa-sm">
          <div class="text-subtitle1 text-weight-bold q-mb-sm text-center">Elevation</div>

          <!-- 리미트 스위치 상태 표시 -->
          <div class="limit-switch-status q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Limit Switch Status</div>
            <div class="row q-gutter-xs">
              <div class="col-6 text-center">
                <q-chip
                  :color="elevationLimitStatus.positiveLimit ? 'negative' : 'positive'"
                  text-color="white"
                  size="sm"
                  :icon="elevationLimitStatus.positiveLimit ? 'warning' : 'check'">
                  +185° {{ elevationLimitStatus.positiveLimit ? 'ACTIVE' : 'NORMAL' }}
                </q-chip>
              </div>
              <div class="col-6 text-center">
                <q-chip
                  :color="elevationLimitStatus.negativeLimit ? 'negative' : 'positive'"
                  text-color="white"
                  size="sm"
                  :icon="elevationLimitStatus.negativeLimit ? 'warning' : 'check'">
                  -5° {{ elevationLimitStatus.negativeLimit ? 'ACTIVE' : 'NORMAL' }}
                </q-chip>
              </div>
            </div>
          </div>

          <!-- 테스트 모드일 때 리미트 스위치 토글 버튼들 -->
          <div v-if="useTestMode" class="test-controls q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Test Controls</div>
            <div class="row q-gutter-xs">
              <div class="col-6">
                <q-btn :color="testLimitStatus.elevation.positiveLimit ? 'negative' : 'positive'"
                  :label="testLimitStatus.elevation.positiveLimit ? '+185° ON' : '+185° OFF'" size="sm" class="full-width"
                  @click="toggleTestLimit('elevation', 'positive')" />
              </div>
              <div class="col-6">
                <q-btn :color="testLimitStatus.elevation.negativeLimit ? 'negative' : 'positive'"
                  :label="testLimitStatus.elevation.negativeLimit ? '-5° ON' : '-5° OFF'" size="sm" class="full-width"
                  @click="toggleTestLimit('elevation', 'negative')" />
              </div>
            </div>
          </div>

          <!-- 현재 각도 표시 -->
          <div class="current-angle-display q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Current Angle</div>
            <div class="text-h5 text-weight-bold text-center text-primary">
              {{ currentAngles.elevation.toFixed(1) }}°
            </div>
          </div>

          <!-- 각도 조절 버튼들 (수평 배치) -->
          <div class="angle-controls">
            <div class="row q-gutter-xs no-wrap">
              <div class="col-6">
                <q-btn color="negative" icon="remove" size="md" class="full-width"
                  @mousedown="startMovement('elevation', -1)" @mouseup="stopMovement('elevation')"
                  @mouseleave="stopMovement('elevation')" @touchstart="startMovement('elevation', -1)"
                  @touchend="stopMovement('elevation')" :disable="elevationLimitStatus.negativeLimit" />
              </div>
              <div class="col-6">
                <q-btn color="positive" icon="add" size="md" class="full-width"
                  @mousedown="startMovement('elevation', 1)" @mouseup="stopMovement('elevation')"
                  @mouseleave="stopMovement('elevation')" @touchstart="startMovement('elevation', 1)"
                  @touchend="stopMovement('elevation')" :disable="elevationLimitStatus.positiveLimit" />
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Tilt Maintenance Card (화면에서는 Tilt로 표시) -->
      <q-card class="col maintenance-card" flat bordered>
        <q-card-section class="q-pa-sm">
          <div class="text-subtitle1 text-weight-bold q-mb-sm text-center">Tilt</div>

          <!-- 리미트 스위치 상태 표시 -->
          <div class="limit-switch-status q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Limit Switch Status</div>
            <div class="row q-gutter-xs">
              <div class="col-6 text-center">
                <q-chip
                  :color="trainLimitStatus.positiveLimit ? 'negative' : 'positive'"
                  text-color="white"
                  size="sm"
                  :icon="trainLimitStatus.positiveLimit ? 'warning' : 'check'">
                  +275° {{ trainLimitStatus.positiveLimit ? 'ACTIVE' : 'NORMAL' }}
                </q-chip>
              </div>
              <div class="col-6 text-center">
                <q-chip
                  :color="trainLimitStatus.negativeLimit ? 'negative' : 'positive'"
                  text-color="white"
                  size="sm"
                  :icon="trainLimitStatus.negativeLimit ? 'warning' : 'check'">
                  -275° {{ trainLimitStatus.negativeLimit ? 'ACTIVE' : 'NORMAL' }}
                </q-chip>
              </div>
            </div>
          </div>

          <!-- 테스트 모드일 때 리미트 스위치 토글 버튼들 -->
          <div v-if="useTestMode" class="test-controls q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Test Controls</div>
            <div class="row q-gutter-xs">
              <div class="col-6">
                <q-btn :color="testLimitStatus.train.positiveLimit ? 'negative' : 'positive'"
                  :label="testLimitStatus.train.positiveLimit ? '+275° ON' : '+275° OFF'" size="sm" class="full-width"
                  @click="toggleTestLimit('train', 'positive')" />
              </div>
              <div class="col-6">
                <q-btn :color="testLimitStatus.train.negativeLimit ? 'negative' : 'positive'"
                  :label="testLimitStatus.train.negativeLimit ? '-275° ON' : '-275° OFF'" size="sm" class="full-width"
                  @click="toggleTestLimit('train', 'negative')" />
              </div>
            </div>
          </div>

          <!-- 현재 각도 표시 -->
          <div class="current-angle-display q-mb-sm">
            <div class="text-caption text-grey-6 text-center q-mb-xs">Current Angle</div>
            <div class="text-h5 text-weight-bold text-center text-primary">
              {{ currentAngles.train.toFixed(1) }}°
            </div>
          </div>

          <!-- 각도 조절 버튼들 (수평 배치) -->
          <div class="angle-controls">
            <div class="row q-gutter-xs no-wrap">
              <div class="col-6">
                <q-btn color="negative" icon="remove" size="md" class="full-width"
                  @mousedown="startMovement('train', -1)" @mouseup="stopMovement('train')"
                  @mouseleave="stopMovement('train')" @touchstart="startMovement('train', -1)"
                  @touchend="stopMovement('train')" :disable="trainLimitStatus.negativeLimit" />
              </div>
              <div class="col-6">
                <q-btn color="positive" icon="add" size="md" class="full-width" @mousedown="startMovement('train', 1)"
                  @mouseup="stopMovement('train')" @mouseleave="stopMovement('train')"
                  @touchstart="startMovement('train', 1)" @touchend="stopMovement('train')"
                  :disable="trainLimitStatus.positiveLimit" />
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useICDStore } from '@/stores/icd/icdStore'
import type { MultiControlCommand } from '@/services'

// ICD Store 사용
const icdStore = useICDStore()

// 테스트 모드 상태
const useTestMode = ref(false)

// 테스트용 리미트 스위치 상태 (Azimuth, Elevation, Train 모두)
const testLimitStatus = ref({
  azimuth: {
    positiveLimit: false,
    negativeLimit: false
  },
  elevation: {
    positiveLimit: false,
    negativeLimit: false
  },
  train: {
    positiveLimit: false,
    negativeLimit: false
  }
})

// 현재 각도 상태 (icdStore에서 실시간 데이터 가져오기)
const currentAngles = computed(() => ({
  azimuth: parseFloat(icdStore.azimuthAngle) || 0.0,
  elevation: parseFloat(icdStore.elevationAngle) || 0.0,
  train: parseFloat(icdStore.trainAngle) || 0.0
}))

// Azimuth 리미트 스위치 상태 (테스트 모드에 따라 선택)
const azimuthLimitStatus = computed(() => {
  if (useTestMode.value) {
    // 테스트 모드: 로컬 상태 사용
    return {
      positiveLimit: testLimitStatus.value.azimuth.positiveLimit,
      negativeLimit: testLimitStatus.value.azimuth.negativeLimit
    }
  } else {
    // 실제 모드: icdStore에서 가져오기
    return {
      positiveLimit: icdStore.azimuthBoardStatusInfo.limitSwitchPositive275,
      negativeLimit: icdStore.azimuthBoardStatusInfo.limitSwitchNegative275
    }
  }
})

// Elevation 리미트 스위치 상태 (테스트 모드에 따라 선택)
const elevationLimitStatus = computed(() => {
  if (useTestMode.value) {
    // 테스트 모드: 로컬 상태 사용
    return {
      positiveLimit: testLimitStatus.value.elevation.positiveLimit,
      negativeLimit: testLimitStatus.value.elevation.negativeLimit
    }
  } else {
    // 실제 모드: elevationBoardStatusInfo에서 가져오기
    return {
      positiveLimit: icdStore.elevationBoardStatusInfo.limitSwitchPositive185,
      negativeLimit: icdStore.elevationBoardStatusInfo.limitSwitchNegative5
    }
  }
})

// Train 리미트 스위치 상태 (테스트 모드에 따라 선택)
const trainLimitStatus = computed(() => {
  if (useTestMode.value) {
    // 테스트 모드: 로컬 상태 사용
    return {
      positiveLimit: testLimitStatus.value.train.positiveLimit,
      negativeLimit: testLimitStatus.value.train.negativeLimit
    }
  } else {
    // 실제 모드: trainBoardStatusInfo에서 가져오기
    return {
      positiveLimit: icdStore.trainBoardStatusInfo.limitSwitchPositive275,
      negativeLimit: icdStore.trainBoardStatusInfo.limitSwitchNegative275
    }
  }
})

// Azimuth 리미트 스위치 상태 변화 감지 및 자동 정지 명령
watch(azimuthLimitStatus, (newStatus, oldStatus) => {
  // 테스트 모드가 아닐 때만 실제 정지 명령 전송
  if (!useTestMode.value) {
    // +275도 리미트 스위치가 새로 활성화된 경우
    if (!oldStatus.positiveLimit && newStatus.positiveLimit) {
      console.warn('🚨 +275도 리미트 스위치 활성화 감지! Azimuth 축 자동 정지 명령 전송')
      void sendStopCommand('azimuth', '+275도 리미트 스위치')
    }

    // -275도 리미트 스위치가 새로 활성화된 경우
    if (!oldStatus.negativeLimit && newStatus.negativeLimit) {
      console.warn('🚨 -275도 리미트 스위치 활성화 감지! Azimuth 축 자동 정지 명령 전송')
      void sendStopCommand('azimuth', '-275도 리미트 스위치')
    }
  }
}, { deep: true })

// Elevation 리미트 스위치 상태 변화 감지 및 자동 정지 명령
watch(elevationLimitStatus, (newStatus, oldStatus) => {
  // 테스트 모드가 아닐 때만 실제 정지 명령 전송
  if (!useTestMode.value) {
    // +185도 리미트 스위치가 새로 활성화된 경우
    if (!oldStatus.positiveLimit && newStatus.positiveLimit) {
      console.warn('🚨 +185도 리미트 스위치 활성화 감지! Elevation 축 자동 정지 명령 전송')
      void sendStopCommand('elevation', '+185도 리미트 스위치')
    }

    // -5도 리미트 스위치가 새로 활성화된 경우
    if (!oldStatus.negativeLimit && newStatus.negativeLimit) {
      console.warn('🚨 -5도 리미트 스위치 활성화 감지! Elevation 축 자동 정지 명령 전송')
      void sendStopCommand('elevation', '-5도 리미트 스위치')
    }
  }
}, { deep: true })

// Train 리미트 스위치 상태 변화 감지 및 자동 정지 명령
watch(trainLimitStatus, (newStatus, oldStatus) => {
  // 테스트 모드가 아닐 때만 실제 정지 명령 전송
  if (!useTestMode.value) {
    // +275도 리미트 스위치가 새로 활성화된 경우
    if (!oldStatus.positiveLimit && newStatus.positiveLimit) {
      console.warn('🚨 +275도 리미트 스위치 활성화 감지! Train 축 자동 정지 명령 전송')
      void sendStopCommand('train', '+275도 리미트 스위치')
    }

    // -275도 리미트 스위치가 새로 활성화된 경우
    if (!oldStatus.negativeLimit && newStatus.negativeLimit) {
      console.warn('🚨 -275도 리미트 스위치 활성화 감지! Train 축 자동 정지 명령 전송')
      void sendStopCommand('train', '-275도 리미트 스위치')
    }
  }
}, { deep: true })

// 이동 중인 축 추적
const movingAxes = ref<{
  azimuth: boolean,
  elevation: boolean,
  train: boolean
}>({
  azimuth: false,
  elevation: false,
  train: false
})

// 목표 각도 추적
const targetAngles = ref<{
  azimuth: number | null,
  elevation: number | null,
  train: number | null
}>({
  azimuth: null,
  elevation: null,
  train: null
})

// 현재 속도 추적 (실시간 속도 조절을 위해)
const currentSpeeds = ref<{
  azimuth: number,
  elevation: number,
  train: number
}>({
  azimuth: 0,
  elevation: 0,
  train: 0
})

// 속도 조절 인터벌 ID 추적
const speedAdjustmentIntervals = ref<{
  azimuth: NodeJS.Timeout | null,
  elevation: NodeJS.Timeout | null,
  train: NodeJS.Timeout | null
}>({
  azimuth: null,
  elevation: null,
  train: null
})

// 각도 범위별 속도 계산 함수 (축별로 다른 범위 적용)
const calculateSpeed = (currentAngle: number, axis: 'azimuth' | 'elevation' | 'train'): number => {
  if (axis === 'azimuth' || axis === 'train') {
    // Azimuth와 Train: ±275도 기준
    const absAngle = Math.abs(currentAngle)
    if (absAngle >= 270 && absAngle <= 275) {
      return 0.1 // 270°~275° 구간: 0.1°/s
    } else if (absAngle >= 0 && absAngle < 270) {
      return 1.0 // 0°~270° 구간: 1.0°/s
    } else {
      return 1.0 // 범위 밖: 기본 속도
    }
  } else if (axis === 'elevation') {
    // Elevation: ±185도 기준
    if (currentAngle >= 180 && currentAngle <= 185) {
      return 0.1 // 180°~185° 구간: 0.1°/s
    } else if (currentAngle >= -5 && currentAngle < 0) {
      return 0.1 // -5°~0° 구간: 0.1°/s
    } else if (currentAngle >= 0 && currentAngle < 180) {
      return 1.0 // 0°~180° 구간: 1.0°/s
    } else {
      return 1.0 // 범위 밖: 기본 속도
    }
  }
  
  return 1.0 // 기본값
}

// 속도 범위 정보 반환 (디버깅용)
const getSpeedRange = (currentAngle: number, axis: 'azimuth' | 'elevation' | 'train'): string => {
  if (axis === 'azimuth' || axis === 'train') {
    // Azimuth와 Train: ±275도 기준
    const absAngle = Math.abs(currentAngle)
    if (absAngle >= 270 && absAngle <= 275) {
      return '270-275° (0.1°/s)'
    } else if (absAngle >= 0 && absAngle < 270) {
      return '0-270° (1.0°/s)'
    } else {
      return '범위 밖 (1.0°/s)'
    }
  } else if (axis === 'elevation') {
    // Elevation: ±185도 기준
    if (currentAngle >= 180 && currentAngle <= 185) {
      return '180-185° (0.1°/s)'
    } else if (currentAngle >= -5 && currentAngle < 0) {
      return '-5-0° (0.1°/s)'
    } else if (currentAngle >= 0 && currentAngle < 180) {
      return '0-180° (1.0°/s)'
    } else {
      return '범위 밖 (1.0°/s)'
    }
  }
  
  return '알 수 없음'
}

// 목표 각도까지 도달했는지 확인
const isTargetReached = (currentAngle: number, targetAngle: number, direction: number): boolean => {
  if (direction > 0) {
    // + 방향: 현재 각도가 목표 각도 이상이면 도달
    return currentAngle >= targetAngle
  } else {
    // - 방향: 현재 각도가 목표 각도 이하이면 도달
    return currentAngle <= targetAngle
  }
}

// 실시간 속도 조절 함수
const adjustSpeedInRealTime = (axis: 'azimuth' | 'elevation' | 'train') => {
  if (!movingAxes.value[axis]) {
    return
  }

  const currentAngle = currentAngles.value[axis]
  const newSpeed = calculateSpeed(currentAngle, axis)
  const currentSpeed = currentSpeeds.value[axis]

  // 속도가 변경되었을 때만 새로운 명령 전송
  if (Math.abs(newSpeed - currentSpeed) > 0.01) {
    console.log(`🔄 ${axis} 축 속도 조절: ${currentSpeed}°/s → ${newSpeed}°/s (현재각도: ${currentAngle}°)`)
    console.log(`📊 속도 변경 상세:`, {
      axis,
      currentAngle,
      oldSpeed: currentSpeed,
      newSpeed,
      speedRange: getSpeedRange(currentAngle, axis),
      timestamp: new Date().toLocaleTimeString()
    })

    const targetAngle = targetAngles.value[axis]
    if (targetAngle !== null) {
      // 새로운 속도로 이동 명령 전송
      const command: MultiControlCommand = {
        azimuth: axis === 'azimuth',
        elevation: axis === 'elevation',
        train: axis === 'train',
        azAngle: axis === 'azimuth' ? targetAngle : undefined,
        elAngle: axis === 'elevation' ? targetAngle : undefined,
        trainAngle: axis === 'train' ? targetAngle : undefined,
        azSpeed: axis === 'azimuth' ? newSpeed : undefined,
        elSpeed: axis === 'elevation' ? newSpeed : undefined,
        trainSpeed: axis === 'train' ? newSpeed : undefined
      }

      console.log(`📤 새로운 속도로 명령 전송:`, command)

      void icdStore.sendMultiControlCommand(command).then(result => {
        if (result.success) {
          currentSpeeds.value[axis] = newSpeed
          console.log(`✅ 속도 조절 성공: ${axis} 축이 ${newSpeed}°/s로 변경됨`)
        } else {
          console.error(`❌ ${axis} 축 속도 조절 실패:`, result.error)
        }
      })
    }
  }
}

// 속도 조절 인터벌 시작
const startSpeedAdjustment = (axis: 'azimuth' | 'elevation' | 'train') => {
  if (speedAdjustmentIntervals.value[axis]) {
    return // 이미 실행 중
  }

  console.log(`🚀 ${axis} 축 실시간 속도 조절 시작 (100ms 간격)`)

  speedAdjustmentIntervals.value[axis] = setInterval(() => {
    adjustSpeedInRealTime(axis)
  }, 100) // 100ms마다 속도 체크
}

// 속도 조절 인터벌 중지
const stopSpeedAdjustment = (axis: 'azimuth' | 'elevation' | 'train') => {
  if (speedAdjustmentIntervals.value[axis]) {
    clearInterval(speedAdjustmentIntervals.value[axis])
    speedAdjustmentIntervals.value[axis] = null
    console.log(`⏹️ ${axis} 축 실시간 속도 조절 중지`)
  }
}

// 테스트 모드 토글 핸들러
const onTestModeToggle = (value: boolean) => {
  console.log(`🧪 테스트 모드 ${value ? '활성화' : '비활성화'}`)
  if (!value) {
    // 테스트 모드 비활성화 시 테스트 상태 초기화
    testLimitStatus.value = {
      azimuth: {
        positiveLimit: false,
        negativeLimit: false
      },
      elevation: {
        positiveLimit: false,
        negativeLimit: false
      },
      train: {
        positiveLimit: false,
        negativeLimit: false
      }
    }
  }
}

// 테스트용 리미트 스위치 토글 (Azimuth, Elevation, Train 모두 지원)
const toggleTestLimit = (axis: 'azimuth' | 'elevation' | 'train', type: 'positive' | 'negative') => {
  if (type === 'positive') {
    testLimitStatus.value[axis].positiveLimit = !testLimitStatus.value[axis].positiveLimit
    console.log(`🧪 ${axis} ${axis === 'elevation' ? '+185' : '+275'}도 리미트 스위치: ${testLimitStatus.value[axis].positiveLimit ? 'ACTIVE' : 'NORMAL'}`)

    // 테스트 모드에서도 리미트 스위치 활성화 시 자동 정지 시뮬레이션
    if (testLimitStatus.value[axis].positiveLimit && movingAxes.value[axis]) {
      console.warn(`🧪 테스트 모드: ${axis} ${axis === 'elevation' ? '+185' : '+275'}도 리미트 스위치 활성화! ${axis} 축 정지 시뮬레이션`)
      void stopMovement(axis)
    }
  } else {
    testLimitStatus.value[axis].negativeLimit = !testLimitStatus.value[axis].negativeLimit
    console.log(`🧪 ${axis} ${axis === 'elevation' ? '-5' : '-275'}도 리미트 스위치: ${testLimitStatus.value[axis].negativeLimit ? 'ACTIVE' : 'NORMAL'}`)

    // 테스트 모드에서도 리미트 스위치 활성화 시 자동 정지 시뮬레이션
    if (testLimitStatus.value[axis].negativeLimit && movingAxes.value[axis]) {
      console.warn(`🧪 테스트 모드: ${axis} ${axis === 'elevation' ? '-5' : '-275'}도 리미트 스위치 활성화! ${axis} 축 정지 시뮬레이션`)
      void stopMovement(axis)
    }
  }
}

// 리미트 스위치 감지 시 자동 정지 명령 전송
const sendStopCommand = async (axis: 'azimuth' | 'elevation' | 'train', reason: string) => {
  try {
    // 이동 중인 축만 정지 명령 전송
    if (movingAxes.value[axis]) {
      console.log(`🛑 ${axis} 축 자동 정지 명령 전송 (${reason})`)

      const result = await icdStore.stopCommand(
        axis === 'azimuth',
        axis === 'elevation',
        axis === 'train'
      )

      if (result.success) {
        movingAxes.value[axis] = false
        targetAngles.value[axis] = null // 목표 각도 초기화
        currentSpeeds.value[axis] = 0 // 속도 초기화
        stopSpeedAdjustment(axis) // 속도 조절 인터벌 중지
        console.log(`✅ ${axis} 축 자동 정지 완료 (${reason})`)
      } else {
        console.error(`❌ ${axis} 축 자동 정지 실패:`, result.error)
      }
    } else {
      console.log(`ℹ️ ${axis} 축이 이동 중이 아니므로 정지 명령 생략 (${reason})`)
    }
  } catch (error) {
    console.error(`❌ ${axis} 축 자동 정지 오류:`, error)
  }
}

// 이동 시작 함수 (버튼을 누를 때)
const startMovement = async (axis: 'azimuth' | 'elevation' | 'train', direction: number) => {
  // 이미 이동 중이면 중복 실행 방지
  if (movingAxes.value[axis]) {
    console.log(`⚠️ ${axis} 축이 이미 이동 중이므로 중복 실행 방지`)
    return
  }

  // 각 축별 리미트 스위치 체크
  if (axis === 'azimuth') {
    if (direction > 0 && azimuthLimitStatus.value.positiveLimit) {
      console.warn('⚠️ +275도 리미트 스위치가 활성화되어 있어 + 방향 이동 불가')
      return
    }
    if (direction < 0 && azimuthLimitStatus.value.negativeLimit) {
      console.warn('⚠️ -275도 리미트 스위치가 활성화되어 있어 - 방향 이동 불가')
      return
    }
  } else if (axis === 'elevation') {
    if (direction > 0 && elevationLimitStatus.value.positiveLimit) {
      console.warn('⚠️ +185도 리미트 스위치가 활성화되어 있어 + 방향 이동 불가')
      return
    }
    if (direction < 0 && elevationLimitStatus.value.negativeLimit) {
      console.warn('⚠️ -5도 리미트 스위치가 활성화되어 있어 - 방향 이동 불가')
      return
    }
  } else if (axis === 'train') {
    if (direction > 0 && trainLimitStatus.value.positiveLimit) {
      console.warn('⚠️ +275도 리미트 스위치가 활성화되어 있어 + 방향 이동 불가')
      return
    }
    if (direction < 0 && trainLimitStatus.value.negativeLimit) {
      console.warn('⚠️ -275도 리미트 스위치가 활성화되어 있어 - 방향 이동 불가')
      return
    }
  }

  try {
    const currentAngle = currentAngles.value[axis]
    let targetAngle: number

    // 목표 각도 설정 (축별로 다른 목표 각도)
    if (axis === 'azimuth' || axis === 'train') {
      targetAngle = direction > 0 ? 275 : -275 // Azimuth와 Train: +275도 또는 -275도
    } else if (axis === 'elevation') {
      targetAngle = direction > 0 ? 185 : -5 // Elevation: +185도 또는 -5도
    } else {
      // 기본값 (사용되지 않음)
      targetAngle = currentAngle + (direction * 10)
    }

    // 목표 각도까지 이미 도달했는지 확인
    if (isTargetReached(currentAngle, targetAngle, direction)) {
      console.log(`ℹ️ ${axis} 축이 이미 목표 각도에 도달함: ${currentAngle}° → ${targetAngle}°`)
      return
    }

    // 초기 속도 계산
    const initialSpeed = calculateSpeed(currentAngle, axis)

    // 목표 각도 저장
    targetAngles.value[axis] = targetAngle
    currentSpeeds.value[axis] = initialSpeed

    console.log(`🎯 ${axis} 축 이동 시작:`, {
      currentAngle,
      targetAngle,
      direction: direction > 0 ? '+' : '-',
      initialSpeed,
      speedRange: getSpeedRange(currentAngle, axis),
      timestamp: new Date().toLocaleTimeString()
    })

    // 이동 명령 구성 (목표 각도 포함)
    const command: MultiControlCommand = {
      azimuth: axis === 'azimuth',
      elevation: axis === 'elevation',
      train: axis === 'train',
      azAngle: axis === 'azimuth' ? targetAngle : undefined, // 목표 각도 설정
      elAngle: axis === 'elevation' ? targetAngle : undefined,
      trainAngle: axis === 'train' ? targetAngle : undefined,
      azSpeed: axis === 'azimuth' ? initialSpeed : undefined,
      elSpeed: axis === 'elevation' ? initialSpeed : undefined,
      trainSpeed: axis === 'train' ? initialSpeed : undefined
    }

    console.log(`📤 첫 번째 이동 명령 전송:`, command)

    // 이동 명령 전송
    const result = await icdStore.sendMultiControlCommand(command)

    if (result.success) {
      movingAxes.value[axis] = true
      startSpeedAdjustment(axis) // 실시간 속도 조절 시작
      console.log(`✅ ${axis} 축 이동 시작 성공: ${direction > 0 ? '+' : '-'} 방향, 초기속도: ${initialSpeed}°/s, 목표: ${targetAngle}°`)
    } else {
      console.error(`❌ ${axis} 축 이동 시작 실패:`, result.error)
    }

  } catch (error) {
    console.error(`❌ ${axis} 축 이동 시작 오류:`, error)
  }
}

// 이동 중지 함수 (버튼을 뗄 때)
const stopMovement = async (axis: 'azimuth' | 'elevation' | 'train') => {
  if (!movingAxes.value[axis]) {
    console.log(`ℹ️ ${axis} 축이 이동 중이 아니므로 정지 명령 생략`)
    return
  }

  try {
    console.log(`🛑 ${axis} 축 수동 정지 명령 전송`)

    // 정지 명령 전송 (stopCommand 사용)
    const result = await icdStore.stopCommand(
      axis === 'azimuth',
      axis === 'elevation',
      axis === 'train'
    )

    if (result.success) {
      movingAxes.value[axis] = false
      targetAngles.value[axis] = null // 목표 각도 초기화
      currentSpeeds.value[axis] = 0 // 속도 초기화
      stopSpeedAdjustment(axis) // 속도 조절 인터벌 중지
      console.log(`✅ ${axis} 축 이동 중지 완료`)
    } else {
      console.error(`❌ ${axis} 축 이동 중지 실패:`, result.error)
    }

  } catch (error) {
    console.error(`❌ ${axis} 축 이동 중지 오류:`, error)
  }
}

// 목표 각도 도달 감지 (실시간 모니터링)
watch(currentAngles, (newAngles) => {
  Object.keys(newAngles).forEach(axis => {
    const axisKey = axis as 'azimuth' | 'elevation' | 'train'
    const currentAngle = newAngles[axisKey]
    const targetAngle = targetAngles.value[axisKey]

    if (targetAngle !== null && movingAxes.value[axisKey]) {
      // 목표 각도까지 도달했는지 확인
      const direction = targetAngle > currentAngle ? 1 : -1
      if (isTargetReached(currentAngle, targetAngle, direction)) {
        console.log(`🎯 ${axisKey} 축 목표 각도 도달: ${currentAngle}° (목표: ${targetAngle}°)`)
        void stopMovement(axisKey)
      }
    }
  })
}, { deep: true })
</script>

<style scoped>
.maintenance-settings {
  padding: 16px;
}

.test-mode-toggle {
  background: rgba(255, 193, 7, 0.05);
  border-radius: 8px;
}

.debug-info {
  background: rgba(33, 150, 243, 0.05);
  border-radius: 8px;
}

.maintenance-card {
  min-height: 200px;
  /* 테스트 컨트롤 추가로 높이 증가 */
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

.limit-switch-status {
  background: rgba(76, 175, 80, 0.05);
  border-radius: 6px;
  padding: 8px;
  border: 1px solid rgba(76, 175, 80, 0.2);
}

.test-controls {
  background: rgba(255, 193, 7, 0.05);
  border-radius: 6px;
  padding: 8px;
  border: 1px solid rgba(255, 193, 7, 0.2);
}

.angle-controls {
  margin-top: 8px;
}

.angle-controls .q-btn {
  border-radius: 8px;
}
</style>
