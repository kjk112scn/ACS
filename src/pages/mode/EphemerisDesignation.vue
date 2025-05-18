<template>
  <div class="ephemeris-mode">
    <div class="section-title text-h5 text-primary q-mb-sm">Ephemeris Designation</div>
    <div class="ephemeris-container">
      <div class="row q-col-gutter-sm">
        <!-- 1번 영역: 차트가 들어갈 네모난 칸 -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Position View</div>
              <div class="chart-area" ref="chartRef"></div>
            </q-card-section>
          </q-card>
        </div>

        <!-- 2번 영역: 계산 정보 표시 영역 -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Tracking Information</div>
              <div class="ephemeris-form">
                <div class="form-row">
                  <q-input
                    v-model.number="ephemerisData.azimuth"
                    label="Azimuth (°)"
                    type="number"
                    outlined
                  />
                  <q-input
                    v-model.number="ephemerisData.elevation"
                    label="Elevation (°)"
                    type="number"
                    outlined
                  />
                  <q-input
                    v-model.number="ephemerisData.tilt"
                    label="Tilt (°)"
                    type="number"
                    outlined
                  />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- 3번 영역: TLE Data -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">TLE Data</div>
              <q-editor
                v-model="tleData.displayText"
                readonly
                flat
                dense
                class="tle-display q-mt-sm"
                :toolbar="[]"
                :definitions="{
                  bold: undefined,
                  italic: undefined,
                  strike: undefined,
                  underline: undefined,
                }"
                content-class="tle-content"
              />

              <!-- 버튼 그룹 추가 -->
              <div class="button-group q-mt-md">
                <q-btn color="primary" label="Cal" @click="openTLEModal" class="q-mr-sm" />
                <q-btn
                  color="positive"
                  label="Go"
                  @click="handleEphemerisCommand"
                  class="q-mr-sm"
                />
                <q-btn color="warning" label="Stop" @click="handleStopCommand" class="q-mr-sm" />
                <q-btn color="negative" label="Stow" @click="handleStowCommand" />
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>

    <!-- TLE 입력 모달 -->
    <q-dialog v-model="showTLEModal" persistent>
      <q-card style="width: 600px; max-width: 90vw">
        <q-card-section class="bg-primary text-white">
          <div class="text-h6">TLE Input</div>
        </q-card-section>

        <q-card-section class="q-pa-md">
          <q-editor
            v-model="tempTLEData.line1"
            min-height="200px"
            class="tle-editor"
            :toolbar="[]"
            :definitions="{
              bold: undefined,
              italic: undefined,
              strike: undefined,
              underline: undefined,
            }"
            content-class="tle-content"
            placeholder="Enter TLE data here..."
          />
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Add" color="primary" @click="addTLEData" />
          <q-btn flat label="Close" color="primary" v-close-popup class="q-ml-sm" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { api } from 'boot/axios'
import { date } from 'quasar'
import { useICDStore } from '../../stores/ICD'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

// 임포트 확인
console.log('ECharts imported:', echarts)

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()
const chartRef = ref<HTMLElement | null>(null)

// TLE 데이터 추가
const tleData = ref({
  displayText: '',
})

// Ephemeris Designation 모드 데이터
const ephemerisData = ref({
  azimuth: 0,
  elevation: 0,
  tilt: 0,
  date: date.formatDate(new Date(), 'YYYY/MM/DD'),
  time: date.formatDate(new Date(), 'HH:mm'),
})
// ECharts 데이터 포인트 타입 정의
interface EChartsScatterParam {
  value: [number, number]
  dataIndex: number
  seriesIndex: number
  seriesName: string
  name: string
  color: string
  borderColor: string
  dimensionNames: string[]
  encode: Record<string, number[]>
  marker: string
  data: [number, number]
  dimensionIndex: number
}

// 차트 관련 변수
let chart: ECharts | null = null

let updateTimer: number | null = null

// 차트 초기화 함수
const initChart = () => {
  if (!chartRef.value) return

  // 기존 차트 인스턴스가 있으면 제거
  if (chart) {
    chart.dispose()
  }

  // 차트 인스턴스 생성
  chart = echarts.init(chartRef.value)

  // 차트 옵션 설정
  const option = {
    backgroundColor: 'transparent',
    grid: {
      containLabel: true,
    },
    polar: {
      radius: ['0%', '80%'],
      center: ['50%', '50%'],
    },
    angleAxis: {
      type: 'value',
      startAngle: 90,
      clockwise: true,
      min: 0,
      max: 360,
      axisLine: {
        show: true,
        lineStyle: {
          color: '#555',
        },
      },
      axisTick: {
        show: true,
        interval: 30,
        lineStyle: {
          color: '#555',
        },
      },
      axisLabel: {
        interval: 30,
        formatter: function (value: number) {
          if (value === 0) return 'N (0°)'
          if (value === 90) return 'E (90°)'
          if (value === 180) return 'S (180°)'
          if (value === 270) return 'W (270°)'
          if (value === 45) return 'NE (45°)'
          if (value === 135) return 'SE (135°)'
          if (value === 225) return 'SW (225°)'
          if (value === 315) return 'NW (315°)'
          if (value % 30 === 0) return value + '°'
          return ''
        },
        color: '#999',
        fontSize: 10,
        distance: 10,
      },
      splitLine: {
        show: true,
        interval: 30,
        lineStyle: {
          color: '#555',
          type: 'dashed',
          width: 1,
        },
      },
    },
    radiusAxis: {
      type: 'value',
      min: 0,
      max: 90,
      inverse: true,
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        formatter: '{value}°',
        color: '#999',
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#555',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        name: '고도/방위각',
        type: 'scatter',
        coordinateSystem: 'polar',
        symbol: 'circle',
        symbolSize: 15,
        itemStyle: {
          color: '#ff5722',
        },
        data: [[0, 0]],
        emphasis: {
          itemStyle: {
            color: '#ff9800',
            borderColor: '#fff',
            borderWidth: 2,
          },
        },
        label: {
          show: true,
          formatter: function (params: EChartsScatterParam) {
            return `Az: ${params.value[0].toFixed(2)}°\nEl: ${params.value[1].toFixed(2)}°`
          },
          position: 'top',
          distance: 5,
          color: '#fff',
          backgroundColor: 'rgba(0,0,0,0.5)',
          padding: [4, 8],
          borderRadius: 4,
        },
        zlevel: 2,
      },
      {
        name: '위치 선',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        lineStyle: {
          color: '#ff5722',
          width: 2,
          type: 'dashed',
        },
        data: [
          [0, 0],
          [0, 0],
        ],
        zlevel: 1,
      },
    ],
    animation: true,
    animationDuration: 150,
    animationEasing: 'linear',
    animationThreshold: 2000,
  }

  // 차트 옵션 적용
  chart.setOption(option)

  // 명시적으로 리사이즈 호출
  setTimeout(() => {
    chart?.resize()
  }, 0)

  // 윈도우 리사이즈 이벤트에 대응
  window.addEventListener('resize', () => {
    chart?.resize()
  })
}
// 차트 업데이트 함수
const updateChart = () => {
  if (!chart) return

  // ICD 스토어의 값으로 차트 업데이트
  const azimuth = Number(icdStore.azimuthAngle)
  const elevation = Number(icdStore.elevationAngle)

  // 방위각이 음수인 경우 0-360 범위로 변환
  const normalizedAzimuth = azimuth < 0 ? azimuth + 360 : azimuth

  // 고도각이 음수인 경우 0으로 처리 (차트에서는 0-90만 표시)
  const normalizedElevation = Math.max(0, Math.min(90, elevation))

  // 차트 데이터 업데이트
  chart.setOption({
    series: [
      {
        // 포인트 위치 업데이트
        data: [[normalizedAzimuth, normalizedElevation]],
      },
      {
        // 선 위치 업데이트
        data: [
          [0, 0],
          [normalizedAzimuth, normalizedElevation],
        ],
      },
    ],
  })
}

// 컴포넌트 마운트 시 차트 초기화
onMounted(() => {
  console.log('Component mounted, echarts:', echarts)

  // DOM이 완전히 렌더링된 후 차트 초기화
  setTimeout(() => {
    try {
      initChart()
      console.log('Chart initialization triggered')

      // 실시간 데이터 업데이트 시작 (150ms 간격)
      updateTimer = window.setInterval(() => {
        updateChart()
      }, 150)
    } catch (error) {
      console.error('Error in onMounted:', error)
    }
  }, 100)
})

// ICD 스토어의 값이 변경될 때마다 차트 업데이트
watch(
  () => [icdStore.azimuthAngle, icdStore.elevationAngle],
  () => {
    updateChart()
  },
)

// 컴포넌트 언마운트 시 정리
onUnmounted(() => {
  // 타이머 정리
  if (updateTimer !== null) {
    clearInterval(updateTimer)
  }

  // 차트 인스턴스 정리
  if (chart) {
    chart.dispose()
    chart = null
  }
})

// TLE 모달 관련 상태
const showTLEModal = ref(false)
const tempTLEData = ref({
  line1: '',
})

// TLE 모달 열기
const openTLEModal = () => {
  showTLEModal.value = true
  tempTLEData.value.line1 = ''
}

// TLE 데이터 추가
const addTLEData = () => {
  const inputText = tempTLEData.value.line1.trim()

  // 입력된 텍스트가 있는 경우에만 처리
  if (inputText) {
    // displayText에 입력된 TLE 데이터를 직접 설정
    tleData.value.displayText = inputText

    // 모달 닫기
    showTLEModal.value = false

    // 바로 계산 실행
    void calculateTLE().catch((error) => {
      console.error('Failed to calculate TLE:', error)
    })
  } else {
    console.error('TLE data is empty')
  }
}

// TLE 계산 함수
const calculateTLE = async () => {
  try {
    // TLE 데이터 유효성 검사
    if (!tleData.value.displayText) {
      console.error('TLE 데이터를 입력해주세요.')
      return
    }

    const lines = tleData.value.displayText.split('\n')
    if (lines.length < 2) {
      console.error('Invalid TLE format: Need two lines')
      return
    }

    // TLE 계산 API 호출
    const response = await api.post('/tle/calculate', {
      line1: lines[0],
      line2: lines[1],
      timestamp: `${ephemerisData.value.date} ${ephemerisData.value.time}`,
    })

    // 계산된 값으로 업데이트
    if (response.data) {
      ephemerisData.value.azimuth = response.data.azimuthAngle || 0
      ephemerisData.value.elevation = response.data.elevationAngle || 0
      ephemerisData.value.tilt = response.data.tiltAngle || 0
    }

    console.log('TLE 계산 성공:', response.data)
  } catch (error) {
    console.error('TLE 계산 실패:', error)
  }
}

// Ephemeris Designation 명령 전송
const sendEphemerisCommand = async () => {
  try {
    const datetime = `${ephemerisData.value.date} ${ephemerisData.value.time}`
    const command = {
      azimuthAngle: ephemerisData.value.azimuth,
      elevationAngle: ephemerisData.value.elevation,
      tiltAngle: ephemerisData.value.tilt,
      timestamp: datetime,
    }

    const response = await api.post('/icd/set-position', command)
    console.log('위치 지정 명령 전송 성공:', response.data)
  } catch (error) {
    console.error('위치 지정 명령 전송 실패:', error)
    throw error // Re-throw to allow handling by caller
  }
}

// Stop 명령 전송
const stopCommand = async () => {
  try {
    // 모든 축 정지 (방위각, 고도각, 틸트각)
    await icdStore.stopCommand(true, true, true)
    console.log('정지 명령 전송 성공')
  } catch (error) {
    console.error('정지 명령 전송 실패:', error)
    throw error
  }
}

// Stow 명령 전송
const stowCommand = async () => {
  try {
    await icdStore.stowCommand()
    console.log('Stow 명령 전송 성공')
  } catch (error) {
    console.error('Stow 명령 전송 실패:', error)
    throw error
  }
}

// Command handlers with error handling
const handleEphemerisCommand = () => {
  void sendEphemerisCommand().catch((error) => {
    console.error('Failed to send ephemeris command:', error)
    // Here you could add user notification of the error
  })
}

const handleStopCommand = () => {
  void stopCommand().catch((error) => {
    console.error('Failed to send stop command:', error)
    // Here you could add user notification of the error
  })
}

const handleStowCommand = () => {
  void stowCommand().catch((error) => {
    console.error('Failed to send stow command:', error)
    // Here you could add user notification of the error
  })
}
</script>
<style scoped>
.ephemeris-mode {
  height: 100%;
  width: 100%;
}

.ephemeris-container {
  padding: 1rem;
  width: 100%;
  height: 100%;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
}

.row {
  width: 100%;
  margin: 0;
}

.control-section {
  height: 100%;
  width: 100%;
  background-color: var(--q-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.chart-area {
  height: 400px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.5rem;
}

.ephemeris-form {
  margin-top: 0.5rem;
  width: 100%;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
}

.button-group {
  display: flex;
  gap: 0.25rem;
  margin-top: 0.5rem;
  width: 100%;
  justify-content: space-between;
}

.full-width {
  width: 100%;
}

:deep(.q-field__control) {
  padding: 0 8px;
}

:deep(.q-card__section) {
  padding: 16px;
}

:deep(.q-card) {
  background: var(--q-dark);
  box-shadow:
    0 1px 5px rgb(0 0 0 / 20%),
    0 2px 2px rgb(0 0 0 / 14%),
    0 3px 1px -2px rgb(0 0 0 / 12%);
}

:deep(.col-md-4) {
  width: 33.3333%;
  padding: 4px;
}

:deep(.q-btn) {
  flex: 1;
}

.tle-editor {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
}

:deep(.tle-editor .q-editor__content) {
  font-family: monospace !important;
  line-height: 1.5;
  padding: 12px;
}

.tle-display {
  font-family: monospace !important;
  background-color: var(--q-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  min-height: 80px;
}

:deep(.tle-display .q-editor__content) {
  font-family: monospace !important;
  color: #fff;
  padding: 12px;
  line-height: 1.5;
}

:deep(.tle-display.q-editor--readonly) {
  border-color: rgba(255, 255, 255, 0.12);
}

:deep(.tle-content) {
  font-family: monospace !important;
  font-size: 14px;
  white-space: pre;
}
</style>
