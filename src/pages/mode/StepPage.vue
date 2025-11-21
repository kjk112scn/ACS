<template>
  <div class="mode-shell step-mode">
    <div class="mode-shell__content">
      <q-card class="mode-card step-card">
        <!-- 축별 제어 패널 (중앙 집중형으로 변경) -->
        <div class="step-container">
          <div class="row q-col-gutter-md justify-center">
            <!-- Azimuth 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !stepStore.selectedAxes.azimuth }">
                <q-card-section>
              <div class="axis-header q-mb-md">
                <div class="checkbox-label-group">
                  <q-checkbox v-model="stepStore.selectedAxes.azimuth" color="primary" class="axis-checkbox" />
                  <div class="text-h6 text-primary axis-title">Azimuth</div>
                </div>
              </div>

              <div class="text-subtitle2">Angle</div>
              <q-input v-model="stepStore.angles.azimuth" type="number" outlined dense class="q-mb-sm" suffix="°"
                :disable="!stepStore.selectedAxes.azimuth" step="0.01" placeholder="0.00"
                @update:model-value="formatAngle('azimuth')" @focus="clearValue('angles', 'azimuth')"
                @blur="handleBlur('angles', 'azimuth')" hide-bottom-space />

              <div class="text-subtitle2">Speed</div>
              <q-input v-model="stepStore.speeds.azimuth" type="number" outlined dense suffix="°/s"
                :disable="!stepStore.selectedAxes.azimuth" min="0" step="0.01" placeholder="0.00"
                @update:model-value="formatSpeed('azimuth')" @focus="clearValue('speeds', 'azimuth')"
                    @blur="handleBlur('speeds', 'azimuth')" hide-bottom-space />
                </q-card-section>
              </q-card>
            </div>

            <!-- Elevation 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !stepStore.selectedAxes.elevation }">
                <q-card-section>
              <div class="axis-header q-mb-md">
                <div class="checkbox-label-group">
                  <q-checkbox v-model="stepStore.selectedAxes.elevation" color="primary" class="axis-checkbox" />
                  <div class="text-h6 text-primary axis-title">Elevation</div>
                </div>
              </div>

              <div class="text-subtitle2">Angle</div>
              <q-input v-model="stepStore.angles.elevation" type="number" outlined dense class="q-mb-sm" suffix="°"
                :disable="!stepStore.selectedAxes.elevation" step="0.01" placeholder="0.00"
                @update:model-value="formatAngle('elevation')" @focus="clearValue('angles', 'elevation')"
                @blur="handleBlur('angles', 'elevation')" hide-bottom-space />

              <div class="text-subtitle2">Speed</div>
              <q-input v-model="stepStore.speeds.elevation" type="number" outlined dense suffix="°/s"
                :disable="!stepStore.selectedAxes.elevation" min="0" step="0.01" placeholder="0.00"
                @update:model-value="formatSpeed('elevation')" @focus="clearValue('speeds', 'elevation')"
                    @blur="handleBlur('speeds', 'elevation')" hide-bottom-space />
                </q-card-section>
              </q-card>
            </div>

            <!-- Tilt 패널 -->
            <div class="col-12 col-md-4">
              <q-card class="axis-panel" :class="{ 'disabled-panel': !stepStore.selectedAxes.train }">
                <q-card-section>
              <div class="axis-header q-mb-md">
                <div class="checkbox-label-group">
                  <q-checkbox v-model="stepStore.selectedAxes.train" color="primary" class="axis-checkbox" />
                  <div class="text-h6 text-primary axis-title">Tilt</div>
                </div>
              </div>

              <div class="text-subtitle2">Angle</div>
              <q-input v-model="stepStore.angles.train" type="number" outlined dense class="q-mb-sm" suffix="°"
                :disable="!stepStore.selectedAxes.train" step="0.01" placeholder="0.00"
                @update:model-value="formatAngle('train')" @focus="clearValue('angles', 'train')"
                @blur="handleBlur('angles', 'train')" hide-bottom-space />

              <div class="text-subtitle2">Speed</div>
              <q-input v-model="stepStore.speeds.train" type="number" outlined dense suffix="°/s"
                :disable="!stepStore.selectedAxes.train" min="0" step="0.01" placeholder="0.00"
                @update:model-value="formatSpeed('train')" @focus="clearValue('speeds', 'train')"
                    @blur="handleBlur('speeds', 'train')" hide-bottom-space />
                </q-card-section>
              </q-card>
            </div>
          </div>

          <!-- 제어 버튼 섹션 -->
          <div class="button-section mode-button-bar">
            <div class="row justify-center q-gutter-md">
              <q-btn label="Go" color="positive" icon="play_arrow" size="lg" :disable="!stepStore.isAnyAxisSelected()"
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
import { defineComponent } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { useStepStore } from '../../stores/mode/stepStore'

// 컴포넌트 이름 정의
defineComponent({
  name: 'StepMode',
})

// 스토어 인스턴스 생성
const icdStore = useICDStore()
const stepStore = useStepStore()

// Go 버튼 핸들러
const handleGo = async () => {
  try {
    // azAngle 계산 로직
    let calculatedAzAngle = parseFloat(stepStore.angles.azimuth)

    if (stepStore.selectedAxes.azimuth) {
      // azimuthBoardServoStatusInfo의 servoMotor 값 확인
      if (icdStore.azimuthBoardServoStatusInfo.servoMotor) {
        // servoMotor가 true이면 cmdAzimuthAngle 값에 stepStore.angles.azimuth 값을 더함
        const cmdAzimuthValue = parseFloat(icdStore.cmdAzimuthAngle) || 0
        calculatedAzAngle = cmdAzimuthValue + parseFloat(stepStore.angles.azimuth)
        console.log(
          `Azimuth 계산 (ServoMotor=true): ${cmdAzimuthValue} + ${parseFloat(stepStore.angles.azimuth)} = ${calculatedAzAngle}`,
        )
      } else {
        // servoMotor가 false이면 azimuthAngle 값에 stepStore.angles.azimuth 값을 더함
        const azimuthValue = parseFloat(icdStore.azimuthAngle) || 0
        calculatedAzAngle = azimuthValue + parseFloat(stepStore.angles.azimuth)
        console.log(
          `Azimuth 계산 (ServoMotor=false): ${azimuthValue} + ${parseFloat(stepStore.angles.azimuth)} = ${calculatedAzAngle}`,
        )
      }
    }

    // elAngle 계산 로직
    let calculatedElAngle = parseFloat(stepStore.angles.elevation)

    if (stepStore.selectedAxes.elevation) {
      // elevationBoardServoStatusInfo의 servoMotor 값 확인
      if (icdStore.elevationBoardServoStatusInfo.servoMotor) {
        // servoMotor가 true이면 cmdElevationAngle 값에 stepStore.angles.elevation 값을 더함
        const cmdElevationValue = parseFloat(icdStore.cmdElevationAngle) || 0
        calculatedElAngle = cmdElevationValue + parseFloat(stepStore.angles.elevation)
        console.log(
          `Elevation 계산 (ServoMotor=true): ${cmdElevationValue} + ${parseFloat(stepStore.angles.elevation)} = ${calculatedElAngle}`,
        )
      } else {
        // servoMotor가 false이면 elevationAngle 값에 stepStore.angles.elevation 값을 더함
        const elevationValue = parseFloat(icdStore.elevationAngle) || 0
        calculatedElAngle = elevationValue + parseFloat(stepStore.angles.elevation)
        console.log(
          `Elevation 계산 (ServoMotor=false): ${elevationValue} + ${parseFloat(stepStore.angles.elevation)} = ${calculatedElAngle}`,
        )
      }
    }

    // tiAngle 계산 로직
    let calculatedTrainAngle = parseFloat(stepStore.angles.train)

    if (stepStore.selectedAxes.train) {
      // trainBoardServoStatusInfo의 servoMotor 값 확인
      if (icdStore.trainBoardServoStatusInfo.servoMotor) {
        // servoMotor가 true이면 cmdTrainngle 값에 stepStore.angles.train 값을 더함
        const cmdTrainValue = parseFloat(icdStore.cmdTrainAngle) || 0
        calculatedTrainAngle = cmdTrainValue + parseFloat(stepStore.angles.train)
        console.log(
          `Train 계산 (ServoMotor=true): ${cmdTrainValue} + ${parseFloat(stepStore.angles.train)} = ${calculatedTrainAngle}`,
        )
      } else {
        // servoMotor가 false이면 trainAngle 값에 stepStore.angles.train 값을 더함
        const trainValue = parseFloat(icdStore.trainAngle) || 0
        calculatedTrainAngle = trainValue + parseFloat(stepStore.angles.train)
        console.log(
          `Train 계산 (ServoMotor=false): ${trainValue} + ${parseFloat(stepStore.angles.train)} = ${calculatedTrainAngle}`,
        )
      }
    }

    await icdStore.sendMultiControlCommand({
      azimuth: stepStore.selectedAxes.azimuth,
      elevation: stepStore.selectedAxes.elevation,
      train: stepStore.selectedAxes.train,
      azAngle: calculatedAzAngle,
      elAngle: calculatedElAngle,
      trainAngle: calculatedTrainAngle,
      azSpeed: parseFloat(stepStore.speeds.azimuth),
      elSpeed: parseFloat(stepStore.speeds.elevation),
      trainSpeed: parseFloat(stepStore.speeds.train),
    })
    console.log('Step 명령 전송 성공')
  } catch (error) {
    console.error('Step 명령 전송 실패:', error)
  }
}

// Stop 버튼 핸들러
const handleStop = async () => {
  try {
    await icdStore.stopCommand(
      stepStore.selectedAxes.azimuth,
      stepStore.selectedAxes.elevation,
      stepStore.selectedAxes.train,
    )
    console.log('Stop 명령 전송 성공')
  } catch (error) {
    console.error('Stop 명령 전송 실패:', error)
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

// 속도 값 포맷팅 (소수점 2자리까지, 양수만)
const formatSpeed = (axis: 'azimuth' | 'elevation' | 'train') => {
  let value = parseFloat(stepStore.speeds[axis])
  if (isNaN(value) || value < 0) {
    value = 0
  }
  stepStore.updateSpeed(axis, value.toFixed(2))
}

// 각도 값 포맷팅 (소수점 2자리까지, 음수 허용)
const formatAngle = (axis: 'azimuth' | 'elevation' | 'train') => {
  let value = parseFloat(stepStore.angles[axis])
  if (isNaN(value)) {
    value = 0
  }
  stepStore.updateAngle(axis, value.toFixed(2))
}

// 입력 필드 값 초기화
const clearValue = (type: 'speeds' | 'angles', axis: 'azimuth' | 'elevation' | 'train') => {
  if (type === 'speeds') {
    stepStore.updateSpeed(axis, '')
  } else {
    stepStore.updateAngle(axis, '')
  }
}

// 입력 필드 포커스 잃을 때 처리
const handleBlur = (type: 'speeds' | 'angles', axis: 'azimuth' | 'elevation' | 'train') => {
  const value = type === 'speeds' ? stepStore.speeds[axis] : stepStore.angles[axis]
  if (value === '' || value === undefined || value === null) {
    if (type === 'speeds') {
      stepStore.updateSpeed(axis, '0.00')
    } else {
      stepStore.updateAngle(axis, '0.00')
    }
  }
}
</script>

<style scoped>
/* StandbyMode와 동일한 중앙 집중형 스타일 */
.step-mode {
  width: 100%;
}

.step-card {
  padding: 1.5rem 2rem 1.75rem 2rem;
}

/* 전체 섹션 간격 최소화 */
.step-container {
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
.axis-panel {
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
  height: 100%;
  min-height: 200px;
  /* 높이 줄임 */
  transition: opacity 0.3s, filter 0.3s;
}

/* 카드 섹션 패딩 최소화 */
.axis-panel .q-card-section {
  padding: 0.8rem;
  /* 패딩 줄임 */
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
  margin-bottom: 0.5rem !important;
  /* 마진 줄임 */
  width: 100% !important;
  /* 전체 너비 사용 */
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
  margin-bottom: 0.5rem !important;
  /* 마진 줄임 */
}

.axis-panel .q-field:last-child {
  margin-bottom: 0 !important;
  /* 마지막 필드는 하단 마진 없음 */
}

.axis-checkbox {
  margin: 0;
  /* 체크박스 마진 제거 */
  flex-shrink: 0;
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

  .step-container {
    max-width: 100%;
    padding: 0.5rem;
  }
}
</style>
