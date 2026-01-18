<template>
  <q-card
    class="control-section position-view-card"
    style="min-height: 360px !important; height: 100% !important; display: flex !important; flex-direction: column !important;"
  >
    <q-card-section
      class="position-view-section"
      style="min-height: 360px !important; height: 100% !important; flex: 1 !important; display: flex !important; flex-direction: column !important; padding-top: 16px !important; padding-bottom: 0px !important;"
    >
      <div class="text-subtitle1 text-weight-bold text-primary position-view-title">Position View</div>
      <div
        class="chart-area"
        ref="chartRef"
        style="min-height: 340px !important; height: 100% !important; flex: 1 !important; padding-top: 0 !important; padding-bottom: 0 !important; margin-bottom: 0 !important;"
      ></div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { useChartTheme } from '@/composables/useChartTheme'

// 테마 색상 캐싱
const { colors: chartColors } = useChartTheme()

// ECharts 매개변수 타입 정의
interface EChartsScatterParam {
  value: [number, number]
  dataIndex: number
  seriesIndex: number
  seriesName: string
  name: string
  color: string
}

interface Position {
  azimuth: number
  elevation: number
}

interface Props {
  currentPosition: Position
  predictedPath: readonly [number, number][]
  actualPath: readonly [number, number][]
  isTracking: boolean
}

const props = defineProps<Props>()

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null
let resizeHandler: (() => void) | null = null
const isInitialized = ref(false)

// 차트 업데이트 풀 클래스
class ChartUpdatePool {
  private positionData: [number, number][] = [[0, 0]]
  private trackingData: [number, number][] = []
  private predictedData: [number, number][] = []
  private updateOption: { series: Array<{ data?: [number, number][] }> }

  constructor() {
    this.updateOption = {
      series: [
        { data: this.positionData },
        { data: this.trackingData },
        { data: this.predictedData },
      ],
    }
  }

  getUpdateOption() {
    return this.updateOption
  }

  updatePosition(elevation: number, azimuth: number) {
    if (this.positionData.length > 0 && this.positionData[0]) {
      this.positionData[0][0] = elevation
      this.positionData[0][1] = azimuth
    } else {
      this.positionData = [[elevation, azimuth]]
      if (this.updateOption.series[0]) {
        this.updateOption.series[0].data = this.positionData
      }
    }
    return this.updateOption
  }

  updateTrackingPath(newPath: [number, number][]) {
    this.trackingData.length = 0
    if (Array.isArray(newPath) && newPath.length > 0) {
      this.trackingData.push(...newPath)
    }
    if (this.updateOption.series[1]) {
      this.updateOption.series[1].data = this.trackingData
    }
    return this.updateOption
  }

  updatePredictedPath(newPath: [number, number][]) {
    this.predictedData.length = 0
    if (Array.isArray(newPath) && newPath.length > 0) {
      this.predictedData.push(...newPath)
    }
    if (this.updateOption.series[2]) {
      this.updateOption.series[2].data = this.predictedData
    }
    return this.updateOption
  }
}

const chartPool = new ChartUpdatePool()

// 차트 초기화
const initChart = () => {
  try {
    if (isInitialized.value && chart && !chart.isDisposed()) {
      return
    }

    if (!chartRef.value) {
      console.warn('⚠️ 차트 컨테이너가 아직 준비되지 않음')
      return
    }

    if (chart) {
      try {
        chart.dispose()
      } catch (disposeError) {
        console.warn('차트 dispose 오류:', disposeError)
      }
    }

    const initialSize = 500
    chart = echarts.init(chartRef.value, null, { width: initialSize, height: initialSize })

    // 테마 색상 사용 (캐싱된 값)
    const colors = chartColors.value

    const option = {
      backgroundColor: 'transparent',
      grid: { left: '10%', right: '10%', top: '10%', bottom: '10%', containLabel: false },
      polar: { radius: ['0%', '50%'], center: ['50%', '50%'] },
      angleAxis: {
        type: 'value',
        startAngle: 90,
        clockwise: true,
        min: 0,
        max: 360,
        animation: false,
        axisLine: { show: true, lineStyle: { color: colors.line } },
        axisTick: { show: true, interval: 60, length: 3, lineStyle: { color: colors.line } },
        axisLabel: {
          interval: 60,
          formatter: (value: number) => {
            const labels: Record<number, string> = {
              0: 'N (0°)', 90: 'E (90°)', 180: 'S (180°)', 270: 'W (270°)',
              45: 'NE (45°)', 135: 'SE (135°)', 225: 'SW (225°)', 315: 'NW (315°)'
            }
            return labels[value] || (value % 60 === 0 ? value + '°' : '')
          },
          color: colors.label,
          fontSize: 8,
          distance: -8,
        },
        splitLine: { show: true, interval: 60, lineStyle: { color: colors.line, type: 'dashed', width: 1 } },
      },
      radiusAxis: {
        type: 'value',
        min: 0,
        max: 90,
        inverse: true,
        animation: false,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { formatter: '{value}°', color: colors.label, fontSize: 8 },
        splitLine: { show: true, lineStyle: { color: colors.line, type: 'dashed', width: 1 } },
      },
      series: [
        {
          name: '실시간 추적 위치',
          type: 'scatter',
          coordinateSystem: 'polar',
          symbol: 'circle',
          symbolSize: 15,
          animation: false,
          itemStyle: { color: colors.azimuth },
          data: [[0, 0]],
          emphasis: { itemStyle: { color: colors.warning, borderColor: colors.text, borderWidth: 2 } },
          label: {
            show: true,
            formatter: (params: EChartsScatterParam) => {
              const az = props.currentPosition?.azimuth || params.value[1]
              const el = props.currentPosition?.elevation || params.value[0]
              return `Az: ${az.toFixed(2)}°\nEl: ${el.toFixed(2)}°`
            },
            position: 'top',
            distance: 5,
            color: colors.text,
            backgroundColor: colors.tooltipBg,
            padding: [4, 8],
            borderRadius: 4,
            fontSize: 10,
          },
          zlevel: 3,
        },
        {
          name: '실시간 추적 경로',
          type: 'line',
          coordinateSystem: 'polar',
          symbol: 'none',
          animation: false,
          lineStyle: { color: colors.text, width: 2, opacity: 0.8 },
          data: [],
          zlevel: 2,
        },
        {
          name: '예정 위성 궤적',
          type: 'line',
          coordinateSystem: 'polar',
          symbol: 'none',
          animation: false,
          lineStyle: { color: colors.info, width: 2 },
          data: [],
          zlevel: 1,
        },
      ],
    }

    chart.setOption(option, true)
    chart.resize({ width: initialSize, height: initialSize })

    // DOM 스타일 설정
    void nextTick(() => {
      const chartElement = chartRef.value?.querySelector('div') as HTMLElement | null
      if (chartElement) {
        chartElement.style.width = `${initialSize}px`
        chartElement.style.height = `${initialSize}px`
        chartElement.style.maxWidth = `${initialSize}px`
        chartElement.style.maxHeight = `${initialSize}px`
        chartElement.style.minWidth = `${initialSize}px`
        chartElement.style.minHeight = `${initialSize}px`
        chartElement.style.position = 'absolute'
        chartElement.style.top = '50%'
        chartElement.style.left = '50%'
        chartElement.style.transform = 'translate(-50%, -50%)'
      }
    })

    // 리사이즈 핸들러
    if (resizeHandler) {
      window.removeEventListener('resize', resizeHandler)
    }
    resizeHandler = () => {
      if (!chart || chart.isDisposed()) return
      void nextTick(() => {
        chart?.resize({ width: initialSize, height: initialSize })
      })
    }
    window.addEventListener('resize', resizeHandler)

    isInitialized.value = true
    console.log('✅ ScheduleChart 초기화 완료')
  } catch (error) {
    console.error('차트 초기화 오류:', error)
    isInitialized.value = false
  }
}

// 차트 업데이트
const updateChart = () => {
  if (!chart || chart.isDisposed()) return

  try {
    const { azimuth, elevation } = props.currentPosition
    const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))

    chartPool.updatePosition(normalizedEl, normalizedAz)

    // 실제 추적 경로
    if (props.isTracking && props.actualPath.length > 0) {
      chartPool.updateTrackingPath(props.actualPath.map((p): [number, number] => [p[0], p[1]]))
    } else {
      chartPool.updateTrackingPath([])
    }

    // 예측 경로
    if (props.predictedPath.length > 0) {
      chartPool.updatePredictedPath(props.predictedPath.map((p): [number, number] => [p[0], p[1]]))
    } else {
      chartPool.updatePredictedPath([])
    }

    const finalOption = chartPool.getUpdateOption()
    chart.setOption(finalOption, false, true)
  } catch (error) {
    console.error('차트 업데이트 오류:', error)
  }
}

// Watch for prop changes
watch(
  () => [props.currentPosition, props.predictedPath.length, props.actualPath.length, props.isTracking],
  () => {
    if (isInitialized.value) {
      updateChart()
    }
  },
  { deep: true }
)

onMounted(() => {
  void nextTick(() => {
    initChart()
    updateChart()
  })
})

onUnmounted(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  if (chart) {
    try {
      chart.dispose()
    } catch (e) {
      console.warn('차트 dispose 오류:', e)
    }
    chart = null
  }
  isInitialized.value = false
})

// Expose methods for parent component
defineExpose({
  initChart,
  updateChart,
})
</script>

<style scoped>
.position-view-card {
  background-color: var(--theme-card-background);
}

.position-view-section {
  position: relative;
}

.position-view-title {
  margin-bottom: 8px;
}

.chart-area {
  position: relative;
  width: 100%;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
