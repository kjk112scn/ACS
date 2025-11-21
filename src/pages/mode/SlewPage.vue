<template>
  <div class="mode-shell slew-mode">
    <div class="mode-shell__content">
      <q-card class="mode-card slew-card">
        <!-- 축별 제어 패널 (Step 모드와 동일한 구조) -->
        <div class="slew-container">
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
import { defineComponent } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { useSlewModeStore } from '@/stores'

// 컴포넌트 이름 정의
defineComponent({
  name: 'SlewMode',
})

// 스토어 인스턴스 생성
const icdStore = useICDStore()
const slewStore = useSlewModeStore()

// Go 버튼 핸들러
const handleGo = async () => {
  try {
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
    console.log('Slew 명령 전송 성공')
  } catch (error) {
    console.error('Slew 명령 전송 실패:', error)
  }
}

// Stop 버튼 핸들러
const handleStop = async () => {
  try {
    await icdStore.stopCommand(
      slewStore.selectedAxes.azimuth,
      slewStore.selectedAxes.elevation,
      slewStore.selectedAxes.train,
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
</script>

<style scoped>
/* Step 모드와 동일한 중앙 집중형 스타일 */
.slew-mode {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
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
.axis-panel {
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
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
