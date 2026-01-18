<template>
  <div class="mode-shell slew-mode">
    <div class="mode-shell__content">
      <q-card class="mode-card slew-card">
        <!-- 축별 제어 패널 (Step 모드와 동일한 구조) -->
        <div class="slew-container">
          <!-- Loop 체크박스 -->
          <div class="loop-checkbox-section">
            <q-checkbox
              v-model="slewStore.loopEnabled"
              color="primary"
              label="Loop"
              class="loop-checkbox"
            />
          </div>
          <div class="row q-col-gutter-md justify-center">
            <!-- Azimuth 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !slewStore.selectedAxes.azimuth }">
                <q-card-section>
                  <div class="axis-header q-mb-md">
                    <div class="checkbox-label-group">
                      <q-checkbox v-model="slewStore.selectedAxes.azimuth" color="primary" class="axis-checkbox" />
                      <div class="text-h6 text-primary axis-title">Azimuth</div>
                    </div>
                  </div>

                  <div class="text-subtitle2">Speed</div>
                  <q-input v-model="slewStore.speeds.azimuth" type="number" outlined dense suffix="°/s"
                    :disable="!slewStore.selectedAxes.azimuth" step="0.01" placeholder="0.00"
                    @update:model-value="formatSpeed('azimuth')" @focus="clearValue('azimuth')"
                    @blur="handleBlur('azimuth')" hide-bottom-space />
                </q-card-section>
              </q-card>
            </div>

            <!-- Elevation 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !slewStore.selectedAxes.elevation }">
                <q-card-section>
                  <div class="axis-header q-mb-md">
                    <div class="checkbox-label-group">
                      <q-checkbox v-model="slewStore.selectedAxes.elevation" color="primary" class="axis-checkbox" />
                      <div class="text-h6 text-primary axis-title">Elevation</div>
                    </div>
                  </div>

                  <div class="text-subtitle2">Speed</div>
                  <q-input v-model="slewStore.speeds.elevation" type="number" outlined dense suffix="°/s"
                    :disable="!slewStore.selectedAxes.elevation" step="0.01" placeholder="0.00"
                    @update:model-value="formatSpeed('elevation')" @focus="clearValue('elevation')"
                    @blur="handleBlur('elevation')" hide-bottom-space />
                </q-card-section>
              </q-card>
            </div>

            <!-- Tilt 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !slewStore.selectedAxes.train }">
                <q-card-section>
                  <div class="axis-header q-mb-md">
                    <div class="checkbox-label-group">
                      <q-checkbox v-model="slewStore.selectedAxes.train" color="primary" class="axis-checkbox" />
                      <div class="text-h6 text-primary axis-title">Tilt</div>
                    </div>
                  </div>

                  <div class="text-subtitle2">Speed</div>
                  <q-input v-model="slewStore.speeds.train" type="number" outlined dense suffix="°/s"
                    :disable="!slewStore.selectedAxes.train" step="0.01" placeholder="0.00"
                    @update:model-value="formatSpeed('train')" @focus="clearValue('train')" @blur="handleBlur('train')"
                    hide-bottom-space />
                </q-card-section>
              </q-card>
            </div>
          </div>

          <!-- 제어 버튼 섹션 (Step 모드와 동일) -->
          <div class="button-section mode-button-bar">
            <div class="row justify-center q-gutter-md">
              <q-btn label="Go" color="positive" icon="play_arrow" size="lg" :disable="!slewStore.isAnyAxisSelected()"
                @click="handleGo" />
              <q-btn label="Stop" color="negative" icon="stop" size="lg" @click="handleStop" />
              <q-btn label="Stow" color="warning" icon="home" size="lg" @click="handleStow" />
            </div>
          </div>
        </div>
      </q-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineComponent, onUnmounted } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { useSlewModeStore, type AxisKey } from '@/stores'
import { useAngleLimitsSettingsStore } from '@/stores/api/settings/angleLimitsSettingsStore'
import { useNotification } from '@/composables/useNotification'
import { useErrorHandler } from '@/composables/useErrorHandler'

// 컴포넌트 이름 정의
defineComponent({
  name: 'SlewMode',
})

// 스토어 인스턴스 생성
const icdStore = useICDStore()
const slewStore = useSlewModeStore()
const angleLimitsStore = useAngleLimitsSettingsStore()
const { success } = useNotification()
const { handleApiError } = useErrorHandler()

// Loop 도달 판정 상수
const ARRIVAL_THRESHOLD = 0.5 // ±0.5°
const STABLE_COUNT_REQUIRED = 3 // 3초 연속 (1초 간격 체크)
const DIRECTION_CHANGE_COOLDOWN = 5000 // 방향 전환 후 5초 쿨다운 (ms)

// Go 버튼 핸들러
const handleGo = async () => {
  try {
    // Loop 모드 체크
    if (slewStore.loopEnabled) {
      await startLoop()
      return
    }

    // 일반 Slew 모드
    // 속도 값에 따라 각도 계산
    const azSpeed = parseFloat(slewStore.speeds.azimuth)
    const elSpeed = parseFloat(slewStore.speeds.elevation)
    const tiSpeed = parseFloat(slewStore.speeds.train)

    // 각도 계산 함수
    const calculateAzTiAngle = (speed: number) => {
      if (speed < 0) return -270
      if (speed > 0) return 270
      return 0
    }

    const calculateElAngle = (speed: number) => {
      if (speed < 0) return 0
      if (speed > 0) return 180
      return 0
    }

    await icdStore.sendMultiControlCommand({
      azimuth: slewStore.selectedAxes.azimuth,
      elevation: slewStore.selectedAxes.elevation,
      train: slewStore.selectedAxes.train,
      azAngle: calculateAzTiAngle(azSpeed),
      elAngle: calculateElAngle(elSpeed),
      trainAngle: calculateAzTiAngle(tiSpeed),
      azSpeed: azSpeed,
      elSpeed: elSpeed,
      trainSpeed: tiSpeed,
    })
    success('Slew 명령이 전송되었습니다.')
  } catch (error) {
    handleApiError(error, 'Slew 명령')
  }
}

// Stop 버튼 핸들러
const handleStop = async () => {
  try {
    // Loop 실행 중이면 먼저 Loop 정지
    if (slewStore.loopRunning) {
      stopLoop()
    }

    await icdStore.stopCommand(
      slewStore.selectedAxes.azimuth,
      slewStore.selectedAxes.elevation,
      slewStore.selectedAxes.train,
    )
    success('Stop 명령이 전송되었습니다.')
  } catch (error) {
    handleApiError(error, 'Stop 명령')
  }
}

// Stow 버튼 핸들러
const handleStow = async () => {
  try {
    await icdStore.stowCommand()
    success('Stow 명령이 전송되었습니다.')
  } catch (error) {
    handleApiError(error, 'Stow 명령')
  }
}

// 속도 값 포맷팅 (소수점 2자리까지, 음수 허용)
const formatSpeed = (axis: 'azimuth' | 'elevation' | 'train') => {
  let value = parseFloat(slewStore.speeds[axis].toString())
  if (isNaN(value)) {
    value = 0
  }
  // 음수도 허용하므로 Math.max 제거
  slewStore.updateSpeed(axis, value.toFixed(2))
}

// 입력 필드 값 초기화
const clearValue = (axis: 'azimuth' | 'elevation' | 'train') => {
  slewStore.updateSpeed(axis, '')
}

// 입력 필드 포커스 잃을 때 처리
const handleBlur = (axis: 'azimuth' | 'elevation' | 'train') => {
  const value = slewStore.speeds[axis]
  if (value === null || value === undefined || value === '' || isNaN(parseFloat(value.toString()))) {
    slewStore.updateSpeed(axis, '0.00')
  } else {
    const numValue = parseFloat(value.toString())
    // 음수도 허용하므로 Math.max 제거하고 소수점 2자리로 포맷팅
    slewStore.updateSpeed(axis, numValue.toFixed(2))
  }
}

// === Loop 관련 함수 ===

// 축별 각도 제한 가져오기
const getAxisLimits = (axis: AxisKey) => {
  const limits = angleLimitsStore.angleLimitsSettings
  switch (axis) {
    case 'azimuth':
      return { min: limits.azimuthMin, max: limits.azimuthMax }
    case 'elevation':
      return { min: limits.elevationMin, max: limits.elevationMax }
    case 'train':
      return { min: limits.trainMin, max: limits.trainMax }
  }
}

// 축별 현재 각도 가져오기 (Slew 모드는 일반 각도 사용, tracking각도는 EPHEMERIS/PASS Schedule만)
const getActualAngle = (axis: AxisKey): number => {
  switch (axis) {
    case 'azimuth':
      return parseFloat(icdStore.azimuthAngle) || 0
    case 'elevation':
      return parseFloat(icdStore.elevationAngle) || 0
    case 'train':
      return parseFloat(icdStore.trainAngle) || 0
  }
}

// 축별 모터 상태 가져오기 (ServoMotor 비트)
const isMotorOff = (axis: AxisKey): boolean => {
  switch (axis) {
    case 'azimuth':
      return !icdStore.azimuthBoardServoStatusInfo.servoMotor
    case 'elevation':
      return !icdStore.elevationBoardServoStatusInfo.servoMotor
    case 'train':
      return !icdStore.trainBoardServoStatusInfo.servoMotor
  }
}

// 도달 판정: ±0.5° 이내인지 확인
const isArrived = (actual: number, target: number): boolean => {
  return Math.abs(actual - target) <= ARRIVAL_THRESHOLD
}

// 축별 이동 명령 전송
const sendAxisCommand = async (axis: AxisKey, targetAngle: number) => {
  const speed = Math.abs(parseFloat(slewStore.speeds[axis]))

  await icdStore.sendMultiControlCommand({
    azimuth: axis === 'azimuth',
    elevation: axis === 'elevation',
    train: axis === 'train',
    azAngle: axis === 'azimuth' ? targetAngle : 0,
    elAngle: axis === 'elevation' ? targetAngle : 0,
    trainAngle: axis === 'train' ? targetAngle : 0,
    azSpeed: axis === 'azimuth' ? speed : 0,
    elSpeed: axis === 'elevation' ? speed : 0,
    trainSpeed: axis === 'train' ? speed : 0,
  })
}

// Loop 시작
const startLoop = async () => {
  if (slewStore.loopRunning) return

  slewStore.setLoopRunning(true)
  slewStore.resetLoopState()

  const axes: AxisKey[] = ['azimuth', 'elevation', 'train']

  // 각 축별 초기 방향 설정 및 첫 이동 명령
  for (const axis of axes) {
    if (!slewStore.selectedAxes[axis]) continue

    const speed = parseFloat(slewStore.speeds[axis])
    if (speed === 0) continue

    const limits = getAxisLimits(axis)
    const currentAngle = getActualAngle(axis)

    // 속도 음수 → Min부터 시작, 속도 양수 → Max부터 시작
    let initialDirection: 'toMin' | 'toMax' = speed < 0 ? 'toMin' : 'toMax'
    let initialTarget = initialDirection === 'toMin' ? limits.min : limits.max

    // 이미 목표 근처에 있으면 반대 방향으로 시작
    if (isArrived(currentAngle, initialTarget)) {
      initialDirection = initialDirection === 'toMin' ? 'toMax' : 'toMin'
      initialTarget = initialDirection === 'toMin' ? limits.min : limits.max
      console.log(`${axis} 이미 목표(${currentAngle.toFixed(2)}°) 근처 → 반대 방향(${initialTarget}°)으로 시작`)
    }

    slewStore.updateLoopAxisState(axis, {
      direction: initialDirection,
      currentTarget: initialTarget,
      stableCount: 0,
      lastDirectionChangeTime: Date.now(), // 시작 시 쿨다운 적용
    })

    // 첫 이동 명령 전송
    await sendAxisCommand(axis, initialTarget)
  }

  // 모니터링 interval 시작 (1초 간격)
  const intervalId = setInterval(() => {
    void monitorLoopProgress()
  }, 1000)

  slewStore.setLoopInterval(intervalId)
  console.log('Loop 시작')
}

// Loop 진행 모니터링
const monitorLoopProgress = async () => {
  if (!slewStore.loopRunning) return

  const axes: AxisKey[] = ['azimuth', 'elevation', 'train']

  for (const axis of axes) {
    if (!slewStore.selectedAxes[axis]) continue

    const loopAxisState = slewStore.loopState[axis]
    if (loopAxisState.currentTarget === null) continue

    // 쿨다운 체크: 방향 전환 후 일정 시간 동안은 다음 전환 불가
    const now = Date.now()
    const timeSinceLastChange = now - loopAxisState.lastDirectionChangeTime
    if (timeSinceLastChange < DIRECTION_CHANGE_COOLDOWN) {
      continue // 쿨다운 중이면 스킵
    }

    const actualAngle = getActualAngle(axis)
    const targetAngle = loopAxisState.currentTarget
    const motorOff = isMotorOff(axis)
    const arrived = isArrived(actualAngle, targetAngle)

    // 도달 판정: ±0.5° 이내 도달 필수
    if (arrived) {
      // Case 1: 도달 + 모터 OFF → 즉시 방향 전환
      if (motorOff) {
        const limits = getAxisLimits(axis)
        const newDirection = loopAxisState.direction === 'toMin' ? 'toMax' : 'toMin'
        const newTarget = newDirection === 'toMin' ? limits.min : limits.max

        slewStore.updateLoopAxisState(axis, {
          direction: newDirection,
          currentTarget: newTarget,
          stableCount: 0,
          lastDirectionChangeTime: now,
        })

        await sendAxisCommand(axis, newTarget)
        console.log(`${axis} 방향 전환 (모터OFF): ${newDirection}, 목표: ${newTarget}°`)
      } else {
        // Case 2: 도달 + 모터 ON → 3초 카운트
        const newStableCount = loopAxisState.stableCount + 1

        if (newStableCount >= STABLE_COUNT_REQUIRED) {
          // 3초 경과 → 방향 전환
          const limits = getAxisLimits(axis)
          const newDirection = loopAxisState.direction === 'toMin' ? 'toMax' : 'toMin'
          const newTarget = newDirection === 'toMin' ? limits.min : limits.max

          slewStore.updateLoopAxisState(axis, {
            direction: newDirection,
            currentTarget: newTarget,
            stableCount: 0,
            lastDirectionChangeTime: now,
          })

          await sendAxisCommand(axis, newTarget)
          console.log(`${axis} 방향 전환 (3초경과): ${newDirection}, 목표: ${newTarget}°`)
        } else {
          // 아직 3초 미만 → 카운트만 증가
          slewStore.updateLoopAxisState(axis, {
            stableCount: newStableCount,
          })
        }
      }
    } else {
      // 목표에 도달하지 않음 → 카운트 리셋
      if (loopAxisState.stableCount > 0) {
        slewStore.updateLoopAxisState(axis, {
          stableCount: 0,
        })
      }
    }
  }
}

// Loop 정지
const stopLoop = () => {
  slewStore.clearLoopInterval()
  slewStore.setLoopRunning(false)
  slewStore.resetLoopState()
  console.log('Loop 정지')
}

// 컴포넌트 언마운트 시 Loop 정리
onUnmounted(() => {
  if (slewStore.loopRunning) {
    stopLoop()
  }
})
</script>

<style scoped>
/* Step 모드와 동일한 중앙 집중형 스타일 */
.slew-mode {
  width: 100%;
  /* ✅ height: 100% 제거 - mode-shell의 높이 설정 사용 */
  /* ✅ justify-content와 align-items 제거 - mode-shell에서 처리 */
}

/* 전체 섹션 간격 최소화 */
.slew-container {
  padding: 0.25rem 0.5rem;
  /* 상단 패딩을 0.5rem → 0.25rem으로 줄임 */
  /* 상단/좌우 패딩만 유지하고 하단은 버튼 카드 마진으로 처리 */
  width: 100%;
  height: auto;
  /* height를 auto로 변경하여 내용에 맞게 조정 */
  max-width: 1200px;
  /* PedestalPosition과 동일한 너비 */
  margin: 0 auto;
  /* 중앙 정렬 */
}

/* 섹션 제목 간격 최소화 */
.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
  margin-bottom: 0.5rem !important;
  /* 마진 줄임 */
}

/* 축 패널 스타일 - 공간 최적화 */
/* ✅ 테두리는 mode-common.scss의 .axis-panel에서 통일 관리 */
.axis-panel {
  background-color: var(--theme-card-background);
  /* ✅ border, border-radius, box-shadow는 mode-common.scss에서 통일 관리 */
  height: 100%;
  min-height: 200px;
  /* 원래 높이 유지 */
  transition: opacity 0.3s, filter 0.3s;
}

/* 카드 섹션 패딩 최소화 */
.axis-panel .q-card-section {
  padding: 0.8rem 0.8rem 0.4rem 0.8rem;
  /* 하단 패딩을 더 줄임 */
}

/* 체크박스와 라벨을 함께 가운데 정렬하는 그룹 */
.checkbox-label-group {
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
  justify-content: center !important;
  /* 체크박스와 라벨을 함께 가운데 정렬 */
  width: 100% !important;
  margin: 0 auto !important;
  /* 가운데 정렬 강제 */
}

/* 축 헤더 간격 최소화 */
.axis-header {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  /* 헤더 전체를 가운데 정렬 */
  margin-bottom: 2rem !important;
  /* 체크박스와 Speed 간격을 조금 줄여서 Speed를 조금 올림 */
  width: 100% !important;
  /* 전체 너비 사용 */
}

/* 체크박스 위 공간 늘리기 */
.axis-checkbox {
  margin: 0.5rem 0 0 0 !important;
  /* 체크박스 위 공간을 늘림 */
  flex-shrink: 0;
}

.disabled-panel {
  opacity: 0.7;
  filter: grayscale(30%);
}

/* 입력 필드 간격 최소화 */
.axis-panel .text-subtitle2 {
  font-size: 0.9rem;
  margin-bottom: 0.25rem !important;
  /* 마진 최소화 */
  margin-top: 0.5rem !important;
  /* 첫 번째 라벨만 상단 마진 */
}

.axis-panel .text-subtitle2:first-of-type {
  margin-top: 0 !important;
  /* 첫 번째 라벨은 상단 마진 없음 */
}

/* 입력 필드 마진 최소화 */
.axis-panel .q-field {
  margin-bottom: 0 !important;
  /* Speed 아래 공간을 완전히 제거 */
}

.axis-panel .q-field:last-child {
  margin-bottom: 0 !important;
  /* 마지막 필드는 하단 마진 없음 */
}

/* Speed 입력창 높이 줄이기 */
.axis-panel .q-field__control {
  min-height: 2rem !important;
  /* 입력창 높이를 줄임 */
}

.axis-panel .q-field__native {
  padding: 0.3rem 0.5rem !important;
  /* 입력창 내부 패딩 줄임 */
}

.axis-title {
  margin: 0;
  flex: 1;
}

/* 숫자 입력 필드의 화살표 버튼 숨기기 */
input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type='number'] {
  -moz-appearance: textfield;
}

/* Loop 체크박스 섹션 스타일 */
.loop-checkbox-section {
  display: flex;
  justify-content: flex-start;
  padding: 0.5rem 0 0.75rem 0;
  margin-bottom: 0.5rem;
}

.loop-checkbox {
  font-size: 1rem;
  font-weight: 500;
}

/* 제어 버튼 섹션 스타일 - 배경색과 테두리 제거 */
.button-section {
  background: transparent;
  border: none;
  padding: 2rem 0 0.25rem 0;
  margin-top: 1.5rem;
}

/* 모바일 화면에서는 카드 간격 조정 */
@media (max-width: 768px) {
  .col-12 {
    margin-bottom: 16px;
  }

  .slew-container {
    max-width: 100%;
    padding: 0.5rem;
  }
}
</style>
