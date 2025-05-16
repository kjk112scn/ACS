<template>
  <div class="ephemeris-mode">
    <h2 class="text-primary q-mb-md">Ephemeris Designation 모드</h2>

    <div class="row q-col-gutter-md">
      <!-- 1번 영역: 차트가 들어갈 네모난 칸 - 비율 증가 -->
      <div class="col-12 col-md-6">
        <div class="chart-container">
          <div class="chart-area" ref="chartRef"></div>
        </div>
      </div>

      <!-- 2번 영역: 계산 정보 표시 영역 - 비율 감소 -->
      <div class="col-12 col-md-3">
        <div class="calculation-info">
          <h3 class="q-mb-sm">계산 정보</h3>

          <div class="ephemeris-form">
            <div class="form-row">
              <q-input
                v-model.number="ephemerisData.azimuth"
                label="방위각 (°)"
                type="number"
                outlined
              />
              <q-input
                v-model.number="ephemerisData.elevation"
                label="고도각 (°)"
                type="number"
                outlined
              />
              <q-input
                v-model.number="ephemerisData.tilt"
                label="틸트각 (°)"
                type="number"
                outlined
              />
            </div>
            <div class="form-row">
              <q-date v-model="ephemerisData.date" outlined />
              <q-time v-model="ephemerisData.time" outlined />
            </div>
            <q-btn color="primary" label="위치 지정" @click="sendEphemerisCommand" />
          </div>

          <!-- 현재 ICD 값 표시 (스토어에서 가져옴) -->
          <div class="current-values q-mt-lg q-pa-md">
            <h3 class="q-mb-sm">현재 ICD 값</h3>
            <p>방위각: {{ icdStore.azimuthAngle }}°</p>
            <p>고도각: {{ icdStore.elevationAngle }}°</p>
            <p>틸트각: {{ icdStore.tiltAngle }}°</p>
          </div>
        </div>
      </div>

      <!-- 3번 영역: ACS TLE 입력 (우측) - 비율 감소 -->
      <div class="col-12 col-md-3">
        <div class="tle-input q-pa-md">
          <h3 class="q-mb-sm">ACS TLE 입력</h3>
          <q-input
            v-model="tleData.line1"
            label="TLE Line 1"
            outlined
            class="q-mb-sm full-width"
            placeholder="1 XXXXX XXX XXXXX.XXXXX .XXXXXXXX XXXXX-X XXXXX-X X XXXXX"
          />
          <q-input
            v-model="tleData.line2"
            label="TLE Line 2"
            outlined
            class="q-mb-md full-width"
            placeholder="2 XXXXX XXX.XXXX XXX.XXXX XXXXXXXX XXX.XXXX XXX.XXXX XX.XXXXXX XXXXX"
          />

          <!-- 버튼 그룹 추가 -->
          <div class="button-group">
            <q-btn color="primary" label="Cal" @click="calculateTLE" class="q-mr-sm" />
            <q-btn color="positive" label="Go" @click="sendEphemerisCommand" class="q-mr-sm" />
            <q-btn color="warning" label="Stop" @click="stopCommand" class="q-mr-sm" />
            <q-btn color="negative" label="Stow" @click="stowCommand" />
          </div>
        </div>
      </div>
    </div>
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
  line1: '',
  line2: '',
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
      containLabel: true
    },
    polar: {
      radius: ['0%', '85%'], // 반지름 비율 증가
      center: ['50%', '50%'] // 중앙 배치
    },
    angleAxis: {
      type: 'value',
      startAngle: 90,
      clockwise: false,
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
        interval: 30, // 30도 간격으로 눈금 표시
        lineStyle: {
          color: '#555',
        },
      },
      axisLabel: {
        interval: 30, // 30도 간격으로 라벨 표시
        formatter: function (value: number) {
          // 주요 방위 표시
          if (value === 0) return 'N (0°)'
          if (value === 90) return 'E (90°)'
          if (value === 180) return 'S (180°)'
          if (value === 270) return 'W (270°)'

          // 중간 방위 표시
          if (value === 45) return 'NE (45°)'
          if (value === 135) return 'SE (135°)'
          if (value === 225) return 'SW (225°)'
          if (value === 315) return 'NW (315°)'

          // 그 외 각도는 숫자만 표시
          if (value % 30 === 0) return value + '°'

          return ''
        },
        color: '#999',
        fontSize: 10, // 글자 크기 조정
        distance: 10, // 라벨과 축 사이의 거리
      },
      splitLine: {
        show: true,
        interval: 30, // 30도 간격으로 분할선 표시
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
        data: [[0, 0]], // 초기 데이터 (방위각, 고도각)
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
            return `Az: ${params.value[0].toFixed(1)}°\nEl: ${params.value[1].toFixed(1)}°`
          },
          position: 'top',
          distance: 10,
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
        ], // 중심에서 현재 위치까지의 선
        zlevel: 1,
      },
    ],
    animation: true,
    animationDuration: 200,
    animationEasing: 'cubicOut',
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

      // 실시간 데이터 업데이트 시작 (100ms 간격)
      updateTimer = window.setInterval(() => {
        updateChart()
      }, 100)
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

// TLE 계산 함수
const calculateTLE = async () => {
  try {
    // TLE 데이터 유효성 검사
    if (!tleData.value.line1 || !tleData.value.line2) {
      console.error('TLE 데이터를 모두 입력해주세요.')
      return
    }

    // TLE 계산 API 호출
    const response = await api.post('/tle/calculate', {
      line1: tleData.value.line1,
      line2: tleData.value.line2,
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
  }
}

// Stow 명령 전송
const stowCommand = async () => {
  try {
    await icdStore.stowCommand()
    console.log('Stow 명령 전송 성공')
  } catch (error) {
    console.error('Stow 명령 전송 실패:', error)
  }
}
</script>
<style scoped>
.ephemeris-form {
  margin-top: 1rem;
}

.form-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.current-values {
  margin-top: 2rem;
  border-radius: 4px;
  background-color: var(--q-secondary);
  color: var(--q-secondary-text);
  opacity: 0.8;
}

.tle-input,
.chart-container,
.calculation-info {
  height: 100%;
  border-radius: 4px;
  border: 1px solid var(--q-primary);
  background-color: var(--q-card-background);
}

.button-group {
  display: flex;
  margin-bottom: 1rem;
}

.full-width {
  width: 100%;
}

.chart-area {
  height: 400px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  padding: 0;
  margin: 0;
}

.chart-container {
  height: 100%;
  border-radius: 4px;
  border: 1px solid var(--q-primary);
  background-color: var(--q-card-background);
  padding: 1rem;
  display: flex;
  flex-direction: column;
}

.chart-container h3 {
  margin-bottom: 0.5rem;
}
</style>
