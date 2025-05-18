<template>
  <div class="slew-mode">
    <div class="section-title text-h5 text-primary q-mb-sm">Slew Mode</div>
    <div class="slew-container">
      <div class="row q-col-gutter-md justify-center">
        <!-- Axis Selection 섹션 -->
        <div class="col-12 col-md-8">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary q-mb-md">Axis Selection</div>

              <!-- 체크박스 그룹 -->
              <div class="checkbox-group q-gutter-x-xl q-mb-lg">
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

              <!-- 각도 입력 -->
              <div class="angle-inputs q-gutter-y-md">
                <div class="input-group">
                  <div class="text-subtitle2 q-mb-sm">Azimuth Speed</div>
                  <q-input
                    v-model.number="speeds.azimuth"
                    type="number"
                    outlined
                    dense
                    class="full-width"
                    suffix="°/s"
                    :disable="!selectedAxes.azimuth"
                    step="0.01"
                    placeholder="0.00"
                    @update:model-value="formatSpeed('azimuth')"
                    @focus="clearValue('azimuth')"
                    @blur="handleBlur('azimuth')"
                    hide-bottom-space
                  />
                </div>
                <div class="input-group">
                  <div class="text-subtitle2 q-mb-sm">Elevation Speed</div>
                  <q-input
                    v-model.number="speeds.elevation"
                    type="number"
                    outlined
                    dense
                    class="full-width"
                    suffix="°/s"
                    :disable="!selectedAxes.elevation"
                    step="0.01"
                    placeholder="0.00"
                    @update:model-value="formatSpeed('elevation')"
                    @focus="clearValue('elevation')"
                    @blur="handleBlur('elevation')"
                    hide-bottom-space
                  />
                </div>
                <div class="input-group">
                  <div class="text-subtitle2 q-mb-sm">Tilt Speed</div>
                  <q-input
                    v-model.number="speeds.tilt"
                    type="number"
                    outlined
                    dense
                    class="full-width"
                    suffix="°/s"
                    :disable="!selectedAxes.tilt"
                    step="0.01"
                    placeholder="0.00"
                    @update:model-value="formatSpeed('tilt')"
                    @focus="clearValue('tilt')"
                    @blur="handleBlur('tilt')"
                    hide-bottom-space
                  />
                </div>
              </div>

              <!-- 버튼 그룹 -->
              <div class="button-group q-mt-lg q-gutter-x-lg">
                <q-btn
                  color="positive"
                  label="Go"
                  size="lg"
                  :disable="!isAnyAxisSelected"
                  @click="handleGo"
                  icon="play_arrow"
                />
                <q-btn color="negative" label="Stop" size="lg" @click="handleStop" icon="stop" />
                <q-btn color="primary" label="Stow" size="lg" @click="handleStow" icon="home" />
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useICDStore } from '../../stores/ICD'
import { defineComponent } from 'vue'

// 컴포넌트 이름 정의
defineComponent({
  name: 'SlewMode',
})

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()

// 선택된 축 상태 관리
const selectedAxes = ref({
  azimuth: false,
  elevation: false,
  tilt: false,
})

// 속도 상태 관리
const speeds = ref({
  azimuth: '0.00',
  elevation: '0.00',
  tilt: '0.00',
})

// 최소 하나의 축이 선택되었는지 확인
const isAnyAxisSelected = computed(() => {
  return selectedAxes.value.azimuth || selectedAxes.value.elevation || selectedAxes.value.tilt
})

// 속도 값 포맷팅 (소수점 2자리)
const formatSpeed = (axis: 'azimuth' | 'elevation' | 'tilt') => {
  let value = parseFloat(speeds.value[axis])
  if (isNaN(value)) {
    value = 0
  }
  speeds.value[axis] = value.toFixed(2)
}

// 입력 필드 값 초기화
const clearValue = (axis: 'azimuth' | 'elevation' | 'tilt') => {
  speeds.value[axis] = ''
}

// 입력 필드 포커스 잃을 때 처리
const handleBlur = (axis: 'azimuth' | 'elevation' | 'tilt') => {
  const value = speeds.value[axis]
  if (value === '' || value === undefined || value === null) {
    speeds.value[axis] = '0.00'
  }
}

// Go 버튼 핸들러
const handleGo = async () => {
  try {
    await icdStore.sendMultiControlCommand({
      azimuth: selectedAxes.value.azimuth,
      elevation: selectedAxes.value.elevation,
      tilt: selectedAxes.value.tilt,
      azAngle: 0,
      elAngle: 0,
      tiAngle: 0,
      azSpeed: parseFloat(speeds.value.azimuth),
      elSpeed: parseFloat(speeds.value.elevation),
      tiSpeed: parseFloat(speeds.value.tilt),
    })
    console.log('Slew 명령 전송 성공')
  } catch (error) {
    console.error('Slew 명령 전송 실패:', error)
  }
}

// Stop 버튼 핸들러
const handleStop = async () => {
  try {
    await icdStore.stopCommand(
      selectedAxes.value.azimuth,
      selectedAxes.value.elevation,
      selectedAxes.value.tilt,
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
</script>

<style scoped>
.slew-mode {
  max-width: 1400px;
  margin: 0 auto;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
}

.slew-container {
  padding: 1rem;
  width: 100%;
  height: 100%;
}

.control-section {
  background-color: var(--q-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.control-section .q-card-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 24px;
}

.input-group {
  margin-bottom: 1rem;
}

.text-subtitle2 {
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.checkbox-group {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
}

.axis-checkbox {
  font-size: 1.2rem;
}

.button-group {
  display: flex;
  flex-direction: row;
  justify-content: center;
  gap: 1.5rem;
  padding: 1rem;
  margin-top: 2rem;
}

:deep(.q-checkbox__label) {
  font-size: 1.2rem;
  color: white;
  padding-left: 8px;
}

:deep(.q-btn) {
  height: 48px;
  font-size: 1.1rem;
  min-width: 140px;
  padding: 0.5rem 2.5rem;
}

:deep(.q-field) {
  font-size: 1.1rem;
}

:deep(.q-field__label) {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.7);
}

:deep(.q-field__native) {
  padding: 0.5rem;
  color: white;
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

/* 모바일 화면에서는 카드 간격 조정 */
@media (max-width: 768px) {
  .col-12 {
    margin-bottom: 16px;
  }
}
</style>
