<template>
  <q-page class="dashboard-container q-pa-md">
    <!-- 상단 부분: 실시간 ICD 데이터 표시 (3축으로 구분) -->
    <q-card class="icd-data-section">
      <q-card-section>
        <div class="header-section">
          <!-- 명령 시간 (좌측 최상단으로 이동) -->
          <div class="cmd-time">
            <span class="adaptive-text time-value">{{ formattedServerTime }}</span>
          </div>
        </div>

        <div class="axis-grid">
          <!-- Azimuth 축 데이터 -->
          <q-card class="axis-card azimuth-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Azimuth</div>

              <!-- Azimuth 차트 영역 추가 -->
              <div class="axis-chart" ref="azimuthChartRef"></div>

              <div class="axis-data-row">
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">CMD</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.cmdAzimuthAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Actual</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.azimuthAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Speed</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.azimuthSpeed)
                  }}</q-item-label>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Elevation 축 데이터 -->
          <q-card class="axis-card elevation-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Elevation</div>

              <!-- Elevation 차트 영역 추가 -->
              <div class="axis-chart" ref="elevationChartRef"></div>

              <div class="axis-data-row">
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">CMD</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.cmdElevationAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Actual</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.elevationAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Speed</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.elevationSpeed)
                  }}</q-item-label>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Tilt 축 데이터 -->
          <q-card class="axis-card tilt-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Tilt</div>

              <!-- Tilt 차트 영역 추가 -->
              <div class="axis-chart" ref="tiltChartRef"></div>

              <div class="axis-data-row">
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">CMD</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.cmdTiltAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Actual</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.tiltAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Speed</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.tiltSpeed)
                  }}</q-item-label>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Emergency와 Control 컨테이너 -->
          <div class="control-container">
            <!-- Emergency 카드 -->
            <q-card class="emergency-card">
              <q-card-section>
                <div class="text-subtitle1 text-weight-bold text-negative">Emergency</div>
                <div class="emergency-content">
                  <q-btn
                    class="full-width"
                    :color="emergencyActive ? 'grey-8' : 'negative'"
                    :label="emergencyActive ? 'Emergency Active' : 'Emergency Stop'"
                    @click="handleEmergencyClick"
                    size="lg"
                  />
                </div>
              </q-card-section>
            </q-card>

            <!-- Emergency 해제 모달 -->
            <q-dialog v-model="emergencyModal">
              <q-card style="min-width: 350px">
                <q-card-section class="row items-center">
                  <div class="text-h6">비상 정지 해제</div>
                  <q-space />
                  <q-btn icon="close" flat round dense v-close-popup />
                </q-card-section>

                <q-card-section>
                  <p>이 버튼을 선택하기 전 확인 후 해제 버튼을 선택해주세요.</p>
                </q-card-section>

                <q-card-actions align="right">
                  <q-btn flat label="닫기" color="grey-7" v-close-popup />
                  <q-btn
                    flat
                    label="해제"
                    color="primary"
                    @click="
                      () => {
                        releaseEmergency()
                        emergencyModal = false
                      }
                    "
                    v-close-popup
                  />
                </q-card-actions>
              </q-card>
            </q-dialog>

            <!-- Control 카드 -->
            <q-card class="control-card">
              <q-card-section>
                <div class="text-subtitle1 text-weight-bold text-primary">Control</div>
                <div class="control-content">
                  <div class="control-buttons q-gutter-y-sm">
                    <q-btn color="primary" label="Initialize" class="full-width" />
                    <q-btn color="warning" label="Reset" class="full-width" />
                    <q-btn color="info" label="Calibrate" class="full-width" />
                  </div>
                </div>
              </q-card-section>
            </q-card>
          </div>

          <!-- Status 카드 -->
          <q-card class="status-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Status</div>
              <div class="status-content">
                <div class="status-messages q-mt-md">
                  <p v-if="icdStore.error" class="text-negative">Error: {{ icdStore.error }}</p>
                  <p v-if="!icdStore.isConnected" class="text-warning">WebSocket Connecting...</p>
                  <p v-if="icdStore.isConnected && !icdStore.error" class="text-positive">
                    Connected
                  </p>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </q-card-section>
    </q-card>

    <!-- 모드 선택 탭 -->
    <q-card class="mode-selection-section q-mt-md">
      <q-card-section>
        <q-tabs
          v-model="currentMode"
          class="text-primary compact-tabs"
          active-color="primary"
          indicator-color="primary"
          align="left"
          narrow-indicator
        >
          <q-tab name="standby" label="Standby" @click="navigateToMode('standby')" />
          <q-tab name="step" label="Step" @click="navigateToMode('step')" />
          <q-tab name="slew" label="Slew" @click="navigateToMode('slew')" />
          <q-tab name="pedestal" label="Pedestal Position" @click="navigateToMode('pedestal')" />
          <q-tab
            name="ephemeris"
            label="Ephemeris Designation"
            @click="navigateToMode('ephemeris')"
          />
          <q-tab name="suntrack" label="Sun Track" @click="navigateToMode('suntrack')" />
          <q-tab name="feed" label="Feed" @click="navigateToMode('feed')" />
        </q-tabs>
      </q-card-section>
    </q-card>

    <!-- 모드 컨텐츠 섹션 -->
    <q-card class="mode-content-section q-mt-md">
      <q-card-section>
        <!-- 라우터 뷰를 사용하여 현재 모드에 맞는 컴포넌트 표시 -->
        <router-view />
      </q-card-section>
    </q-card>
  </q-page>
</template>
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useICDStore } from '../stores/ICD'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()
const router = useRouter()
const route = useRoute()

// 차트 참조 생성 - 명확하게 분리
const azimuthChartRef = ref<HTMLElement | null>(null)
const elevationChartRef = ref<HTMLElement | null>(null)
const tiltChartRef = ref<HTMLElement | null>(null)

// 차트 인스턴스 저장 - 명확하게 분리 (여기서 ECharts 타입 사용)
let azimuthChart: ECharts | undefined = undefined
let elevationChart: ECharts | undefined = undefined
let tiltChart: ECharts | undefined = undefined

// 차트 업데이트 타이머 - 하나만 사용
let chartUpdateTimer: number | null = null

// 값 표시 헬퍼 함수
const displayValue = (value: string | number | null | undefined) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }

  // 숫자로 변환 가능한지 확인
  const num = Number(value)
  if (!isNaN(num)) {
    return num.toFixed(2)
  }

  return value
}

// 서버 시간 포맷팅 (기본 Date 메서드 사용)
const formattedServerTime = computed(() => {
  if (!icdStore.serverTime) return ''

  try {
    // ISO 문자열을 Date 객체로 변환
    const dateObj = new Date(icdStore.serverTime)

    // 시간 문자열 생성 (toISOString은 항상 UTC 시간을 반환)
    // 예: "2023-05-12T16:49:59.928Z" -> "2023-05-12 16:49:59.928"

    const isoString = dateObj.toISOString()
    return isoString.replace('T', ' ').replace('Z', '')
  } catch (error) {
    console.error('날짜 포맷팅 오류:', error)
    return icdStore.serverTime // 오류 발생 시 원본 문자열 반환
  }
})

// 현재 모드 상태
const currentMode = ref('ephemeris')

// 모드 변경 시 해당 라우트로 이동
const navigateToMode = (mode: string) => {
  // void 연산자를 사용하여 Promise를 명시적으로 무시
  void router.push(`/dashboard/${mode}`)
}

// 컴포넌트 마운트 시 차트 초기화 및 업데이트 타이머 설정
onMounted(() => {
  // 현재 라우트 경로에서 모드 추출
  const pathParts = route.path.split('/')
  const currentPathMode = pathParts[pathParts.length - 1]

  // 유효한 모드인 경우 currentMode 업데이트
  if (
    currentPathMode &&
    ['ephemeris', 'pedestal', 'suntrack', 'feed', 'standby', 'step', 'slew'].includes(
      currentPathMode,
    )
  ) {
    currentMode.value = currentPathMode
  } else {
    // 기본 모드로 리다이렉트
    void router.push('/dashboard/standby')
  }

  // WebSocket 연결 초기화
  icdStore.initialize()

  // DOM이 완전히 렌더링된 후 차트 초기화를 위해 setTimeout 사용
  setTimeout(() => {
    try {
      initCharts()
      console.log('모든 차트 초기화 완료')

      // 차트 업데이트 타이머 설정
      chartUpdateTimer = window.setInterval(() => {
        updateCharts()
      }, 25)
    } catch (error) {
      console.error('차트 초기화 중 오류 발생:', error)
    }
  }, 100)

  // 디버깅용 타이머 - 스토어 값 주기적으로 로깅
  const debugInterval = setInterval(() => {
    console.log('현재 스토어 값:', {
      azimuthAngle: icdStore.azimuthAngle,
      elevationAngle: icdStore.elevationAngle,
      tiltAngle: icdStore.tiltAngle,
    })
  }, 5000)

  // 컴포넌트 언마운트 시 타이머 정리를 위한 변수 저장
  onUnmounted(() => {
    clearInterval(debugInterval)
  })

  // 윈도우 리사이즈 이벤트 리스너 등록
  window.addEventListener('resize', handleResize)
})

// 윈도우 리사이즈 핸들러 - 차트 크기 조정
const handleResize = () => {
  if (azimuthChart) azimuthChart.resize()
  if (elevationChart) elevationChart.resize()
  if (tiltChart) tiltChart.resize()
}

// 컴포넌트가 언마운트될 때 정리 작업
onUnmounted(() => {
  // WebSocket 연결 정리
  icdStore.cleanup()

  // 차트 업데이트 타이머 정리
  if (chartUpdateTimer !== null) {
    clearInterval(chartUpdateTimer)
    chartUpdateTimer = null
  }

  // 차트 인스턴스 정리
  if (azimuthChart) {
    azimuthChart.dispose()
    azimuthChart = undefined
  }

  if (elevationChart) {
    elevationChart.dispose()
    elevationChart = undefined
  }

  if (tiltChart) {
    tiltChart.dispose()
    tiltChart = undefined
  }

  // 윈도우 리사이즈 이벤트 리스너 제거
  window.removeEventListener('resize', handleResize)
})

// 디버깅을 위한 watch 추가
watch(
  () => icdStore.tiltAngle,
  (newValue) => {
    console.log('DashboardPage에서 감지된 tiltAngle 변경:', newValue)
  },
)

// 다른 값들도 감시
watch(
  () => icdStore.elevationAngle,
  (newValue) => {
    console.log('DashboardPage에서 감지된 elevationAngle 변경:', newValue)
  },
)

// 라우트 변경 감지
watch(
  () => route.path,
  (newPath) => {
    const pathParts = newPath.split('/')
    // 배열의 마지막 요소가 존재하는지 확인 후 사용
    const currentPathMode = pathParts.length > 0 ? pathParts[pathParts.length - 1] : ''

    if (
      currentPathMode &&
      ['ephemeris', 'pedestal', 'suntrack', 'feed'].includes(currentPathMode)
    ) {
      currentMode.value = currentPathMode
    }
  },
)

// 차트 업데이트 함수 - 각 차트를 독립적으로 업데이트
const updateCharts = () => {
  // 1. Azimuth 차트 업데이트
  if (azimuthChart) {
    try {
      const azimuth = parseFloat(icdStore.azimuthAngle || '0')
      const normalizedAzimuth = azimuth < 0 ? azimuth + 360 : azimuth % 360

      azimuthChart.setOption({
        series: [
          {
            data: [[1, normalizedAzimuth]],
            label: {
              formatter: function () {
                return `${azimuth.toFixed(2)}°`
              },
            },
          },
        ],
      })
    } catch (error) {
      console.error('Azimuth 차트 업데이트 오류:', error)
    }
  }

  // 2. Elevation 차트 업데이트
  if (elevationChart) {
    try {
      const elevation = parseFloat(icdStore.elevationAngle || '0')
      const normalizedElevation = elevation < 0 ? elevation + 360 : elevation % 360

      elevationChart.setOption({
        series: [
          {
            data: [[0, normalizedElevation]],
            label: {
              formatter: function () {
                return `${elevation.toFixed(2)}°`
              },
            },
          },
        ],
      })
    } catch (error) {
      console.error('Elevation 차트 업데이트 오류:', error)
    }
  }

  // 3. Tilt 차트 업데이트
  if (tiltChart) {
    try {
      const tilt = parseFloat(icdStore.tiltAngle || '0')
      const normalizedTilt = tilt < 0 ? tilt + 360 : tilt % 360

      tiltChart.setOption({
        series: [
          {
            data: [[1, normalizedTilt]],
            label: {
              formatter: function () {
                return `${tilt.toFixed(2)}°`
              },
            },
          },
        ],
      })
    } catch (error) {
      console.error('Tilt 차트 업데이트 오류:', error)
    }
  }
}

// ECharts 데이터 포인트 타입 정의

interface EChartsScatterParam {
  value: (number | string)[]
  dataIndex: number
  seriesIndex: number
  seriesName: string
  name: string
  color: string
  borderColor: string
  dimensionNames: string[]
  encode: Record<string, number[]>
  marker: string
  data: unknown
  dimensionIndex: number
}

// 차트 초기화 함수 - 각 차트를 완전히 독립적으로 초기화
const initCharts = () => {
  console.log('차트 초기화 시작')

  // 1. Azimuth 차트 초기화 - 완전히 독립적으로
  if (azimuthChartRef.value) {
    console.log('Azimuth 차트 DOM 요소 존재함')

    // 기존 차트가 있으면 제거
    if (azimuthChart) {
      azimuthChart.dispose()
    }

    // 새 차트 인스턴스 생성
    azimuthChart = echarts.init(azimuthChartRef.value)

    // 현재 Actual 값으로 초기 데이터 설정
    const initialAzimuth = parseFloat(icdStore.azimuthAngle || '0')
    console.log('Initial Azimuth value:', initialAzimuth)

    // Azimuth 차트만의 옵션 설정
    const azimuthOption = {
      backgroundColor: 'transparent',
      grid: { containLabel: true },
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
          lineStyle: { color: '#555', width: 1 },
        },
        axisTick: {
          show: true,
          interval: 30,
          lineStyle: { color: '#555' },
        },
        axisLabel: {
          interval: 30,
          formatter: function (value: number) {
            if (value === 0) return 'N(0°)'
            if (value === 90) return '{vAlign|E}\n(90°)'
            if (value === 180) return 'S(180°)'
            if (value === 270) return '{vAlign|W}\n(270°)'
            return value + '°'
          },
          color: '#999',
          fontSize: 13,
          distance: 25,
          rich: {
            vAlign: {
              align: 'center',
              padding: [0, 0, 2, 0],
              verticalAlign: 'bottom',
            },
          },
        },
        splitLine: {
          show: true,
          interval: 30,
          lineStyle: { color: '#555', type: 'dashed', width: 1 },
        },
      },
      radiusAxis: {
        type: 'value',
        min: 0,
        max: 1,
        inverse: false,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
        splitLine: {
          show: true,
          lineStyle: { color: '#555', type: 'dashed', width: 1 },
        },
      },
      series: [
        {
          name: '방위각',
          type: 'scatter',
          coordinateSystem: 'polar',
          symbol: 'circle',
          symbolSize: 12,
          itemStyle: { color: '#ff5722' },
          data: [[1, initialAzimuth]], // [radius, angle] 형식으로 변경
          zlevel: 2,
          label: {
            show: true,
            formatter: function (params: EChartsScatterParam) {
              if (Array.isArray(params.value) && params.value.length > 0) {
                const val = params.value[1] // angle은 두 번째 값
                return `${Number(val).toFixed(2)}°`
              }
              return '0.00°'
            },
            position: 'top',
            distance: 0,
            color: '#ff5722',
            fontSize: 15,
            padding: [4, 8],
            backgroundColor: 'rgba(0,0,0,0.5)',
            borderRadius: 4,
            align: 'center',
          },
        },
      ],
      animation: false,
    }

    // 옵션 적용
    azimuthChart.setOption(azimuthOption)
    console.log('Azimuth 차트 초기화 완료')
  } else {
    console.error('Azimuth 차트 DOM 요소가 없음')
  }

  // 2. Elevation 차트 초기화 - 완전히 독립적으로
  if (elevationChartRef.value) {
    console.log('Elevation 차트 DOM 요소 존재함')

    // 기존 차트가 있으면 제거
    if (elevationChart) {
      elevationChart.dispose()
    }

    // 새 차트 인스턴스 생성
    elevationChart = echarts.init(elevationChartRef.value)
    // 초기 tilt 값 가져오기
    const initialElevation = parseFloat(icdStore.elevationAngle || '0')
    const normalizedInitialElevation =
      initialElevation < 0 ? initialElevation + 360 : initialElevation % 360
    // Elevation 차트만의 옵션 설정
    const elevationOption = {
      backgroundColor: 'transparent',
      grid: { containLabel: true },
      polar: {
        radius: ['0%', '80%'],
        center: ['50%', '50%'],
      },
      angleAxis: {
        type: 'value',
        startAngle: 180,
        clockwise: true,
        min: 0,
        max: 360,
        axisLine: {
          show: true,
          lineStyle: { color: '#555', width: 1 },
        },
        axisTick: {
          show: true,
          interval: 30,
          lineStyle: { color: '#555' },
        },
        axisLabel: {
          interval: 30,
          formatter: function (value: number) {
            if (value === 0) return '{upLabel|W(0°)}'
            if (value === 90) return 'N(90°)'
            if (value === 180) return '{upLabel|E(180°)}'
            return value + '°'
          },
          color: '#999',
          fontSize: 13,
          distance: 25,
          rich: {
            upLabel: {
              align: 'center',
              padding: [0, 0, 10, 0],
              verticalAlign: 'bottom',
            },
          },
        },
        splitLine: {
          show: true,
          interval: 30,
          lineStyle: { color: '#555', type: 'dashed', width: 1 },
        },
      },
      radiusAxis: {
        type: 'value',
        min: 0,
        max: 1,
        inverse: true,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
        splitLine: {
          show: true,
          lineStyle: { color: '#555', type: 'dashed', width: 1 },
        },
      },
      series: [
        {
          name: '고도각',
          type: 'scatter',
          coordinateSystem: 'polar',
          symbol: 'circle',
          symbolSize: 12,
          itemStyle: { color: '#2196f3' },
          data: [[0, normalizedInitialElevation]],
          zlevel: 2,
          label: {
            show: true,
            formatter: function () {
              return `${initialElevation.toFixed(2)}°`
            },
            position: 'top',
            distance: 0,
            color: '#2196f3',
            fontSize: 15,
            padding: [4, 8],
            backgroundColor: 'rgba(0,0,0,0.5)',
            borderRadius: 4,
          },
        },
      ],
      animation: false,
    }

    // 옵션 적용
    elevationChart.setOption(elevationOption)
    console.log('Elevation 차트 초기화 완료')
  } else {
    console.error('Elevation 차트 DOM 요소가 없음')
  }

  // 3. Tilt 차트 초기화
  if (tiltChartRef.value) {
    if (tiltChart) {
      tiltChart.dispose()
    }
    tiltChart = echarts.init(tiltChartRef.value)

    // 초기 tilt 값 가져오기
    const initialTilt = parseFloat(icdStore.tiltAngle || '0')
    const normalizedInitialTilt = initialTilt < 0 ? initialTilt + 360 : initialTilt % 360

    const tiltOption = {
      backgroundColor: 'transparent',
      grid: { containLabel: true },
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
          lineStyle: { color: '#555', width: 1 },
        },
        axisTick: {
          show: true,
          interval: 30,
          lineStyle: { color: '#555' },
        },
        axisLabel: {
          interval: 30,
          formatter: function (value: number) {
            if (value === 0) return 'N(0°)'
            if (value === 90) return '{vAlign|E}\n(90°)'
            if (value === 180) return 'S(180°)'
            if (value === 270) return '{vAlign|W}\n(270°)'
            return value + '°'
          },
          color: '#999',
          fontSize: 13,
          distance: 25,
          rich: {
            vAlign: {
              align: 'center',
              padding: [0, 0, 2, 0],
            },
          },
        },
        splitLine: {
          show: true,
          interval: 30,
          lineStyle: { color: '#555', type: 'dashed', width: 1 },
        },
      },
      radiusAxis: {
        type: 'value',
        min: 0,
        max: 1,
        inverse: false,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
        splitLine: {
          show: true,
          lineStyle: { color: '#555', type: 'dashed', width: 1 },
        },
      },
      series: [
        {
          name: '틸트각',
          type: 'scatter',
          coordinateSystem: 'polar',
          symbol: 'circle',
          symbolSize: 12,
          itemStyle: { color: '#4caf50' },
          data: [[1, normalizedInitialTilt]], // 초기값을 현재 tilt 값으로 설정
          zlevel: 2,
          label: {
            show: true,
            formatter: function () {
              return `${initialTilt.toFixed(2)}°`
            },
            position: 'top',
            distance: 0,
            color: '#4caf50',
            fontSize: 15,
            padding: [4, 8],
            backgroundColor: 'rgba(0,0,0,0.5)',
            borderRadius: 4,
          },
        },
      ],
      animation: false,
    }

    tiltChart.setOption(tiltOption)
  }

  // 모든 차트 초기화 후 명시적으로 리사이즈 호출
  setTimeout(() => {
    if (azimuthChart) azimuthChart.resize()
    if (elevationChart) elevationChart.resize()
    if (tiltChart) tiltChart.resize()
  }, 0)
}

// Emergency 상태 관리
const emergencyActive = ref(false)
const emergencyModal = ref(false)

// Emergency 버튼 클릭 핸들러
const handleEmergencyClick = async () => {
  if (!emergencyActive.value) {
    // 비상 정지 활성화 ('E' 명령 전송)
    try {
      await icdStore.sendEmergency('E')
      emergencyActive.value = true
      console.log('Emergency Stop 활성화됨')
    } catch (error) {
      console.error('Emergency Stop 활성화 실패:', error)
    }
  } else {
    // 이미 활성화된 상태면 모달 표시
    emergencyModal.value = true
  }
}

// Emergency 해제 함수
const releaseEmergency = async () => {
  console.log('releaseEmergency 함수 호출됨') // 디버깅 로그 추가

  try {
    await icdStore.sendEmergency('S')

    emergencyActive.value = false
    console.log('Emergency Stop 해제됨')
  } catch (error) {
    console.error('Emergency Stop 해제 실패:', error)
  }
}
</script>

<style>
/* 전역 스타일: 다크 모드와 라이트 모드에 따른 텍스트 색상 조정 */
.body--dark .adaptive-text {
  color: white !important;
}

.body--light .adaptive-text {
  color: black !important;
}

/* 다크 모드와 라이트 모드에 따른 caption 텍스트 색상 조정 */
.body--dark .adaptive-caption {
  color: rgba(255, 255, 255, 0.7) !important;
}

.body--light .adaptive-caption {
  color: rgba(0, 0, 0, 0.6) !important;
}

/* 차트 툴팁 스타일 조정 */
.echarts-tooltip {
  background-color: rgba(50, 50, 50, 0.7) !important;
  border: 1px solid #666 !important;
  border-radius: 4px !important;
  padding: 6px 8px !important;
  color: white !important;
  font-size: 12px !important;
}

/* 차트 라벨 스타일 조정 */
.body--dark .echarts-label {
  color: rgba(255, 255, 255, 0.9) !important;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5) !important;
}

.body--light .echarts-label {
  color: rgba(0, 0, 0, 0.9) !important;
  text-shadow: 0 0 2px rgba(255, 255, 255, 0.5) !important;
}
</style>

<style scoped>
.dashboard-container {
  max-width: 1880px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 1rem;
}

.cmd-time {
  display: flex;
  align-items: center;
}

.time-label {
  font-weight: 500;
  font-size: 1rem;
  margin-right: 0.25rem;
}

.time-value {
  font-weight: 500;
  font-size: 1rem;
}

.axis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1.2fr) minmax(0, 1.2fr) minmax(0, 0.8fr) minmax(
      0,
      0.8fr
    );
  gap: 1rem;
  margin-top: 1rem;
}

.axis-card {
  border: 1px solid var(--q-primary);
  display: flex;
  flex-direction: column;
}

.axis-card .q-card-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0.25rem 1rem 0.5rem 1rem;
}

/* 각 축 카드에 고유한 스타일 적용 */
.axis-card {
  grid-column: span 1;
  border-top: 3px solid var(--q-primary);
  padding: 0.5rem 0;
}

.azimuth-card {
  border-top-color: #ff5722;
}

.tilt-card {
  border-top-color: #4caf50;
}

.elevation-card {
  border-top-color: #2196f3;
}

.axis-data-row {
  margin-top: auto;
  margin-bottom: 0;
  padding-bottom: 0;
}

.axis-card .text-subtitle1 {
  margin-bottom: 0.15rem;
  font-size: 1rem;
}

/* Emergency와 Control 컨테이너 */
.control-container {
  grid-column: span 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  height: 100%;
}

/* Emergency 카드 */
.emergency-card {
  border: 1px solid var(--q-negative);
  border-top: 3px solid var(--q-negative);
  flex: 1;
}

.emergency-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 0;
}

/* Control 카드 */
.control-card {
  border: 1px solid var(--q-primary);
  border-top: 3px solid var(--q-primary);
  flex: 1;
}

.control-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 0;
}

.control-buttons {
  width: 100%;
}

/* Status 카드 */
.status-card {
  grid-column: span 1;
  border: 1px solid var(--q-primary);
  border-top: 3px solid var(--q-primary);
  height: 100%;
}

.status-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.status-messages p {
  margin: 0.25rem 0;
  font-size: 0.9rem;
}

/* 차트 영역 스타일 */
.axis-chart {
  height: 240px;
  min-height: 240px;
  width: 100%;
  margin: 0.25rem 0 0.25rem 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.axis-data-row {
  display: flex;
  justify-content: space-between;
  margin-top: 0.25rem;
  margin-bottom: 0;
}

.axis-data-item {
  flex: 1;
  text-align: center;
  padding: 0.25rem 0.5rem;
}

.mode-toggle {
  width: 100%;
  max-width: 500px;
}

/* 컴팩트 탭 스타일 */
.compact-tabs {
  height: 42px;
}

.compact-tabs .q-tab {
  padding: 0 12px;
  min-height: 42px;
}

/* 큰 태블릿 화면 (1280px 미만) */
@media (max-width: 1279px) {
  .axis-grid {
    grid-template-columns: minmax(0, 1.2fr) minmax(0, 1.2fr) minmax(0, 1.2fr) minmax(0, 0.8fr);
  }

  .status-card {
    grid-column: 1 / -1;
    margin-top: 1rem;
  }
}

/* 태블릿 화면 (1024px 미만) */
@media (max-width: 1023px) {
  .axis-grid {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1fr);
  }

  .control-container {
    grid-column: 1 / -1;
    flex-direction: row;
    gap: 1rem;
  }

  .emergency-card,
  .control-card {
    flex: 1;
  }

  .status-card {
    grid-column: 1 / -1;
  }
}

/* 작은 태블릿 화면 (768px 미만) */
@media (max-width: 767px) {
  .axis-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .control-container {
    grid-column: 1 / -1;
    flex-direction: column;
  }
}

/* 모바일 화면 (480px 미만) */
@media (max-width: 479px) {
  .axis-grid {
    grid-template-columns: 1fr;
  }

  .azimuth-card,
  .elevation-card,
  .tilt-card,
  .control-container,
  .status-card {
    grid-column: 1 / -1;
  }
}

/* 모바일 화면에서의 차트 크기 조정 */
@media (max-width: 768px) {
  .axis-chart {
    height: 220px;
    min-height: 220px;
  }

  .azimuth-card .axis-chart {
    height: 280px;
    min-height: 280px;
  }
}

@media (max-width: 480px) {
  .axis-chart {
    height: 200px;
    min-height: 200px;
  }

  .azimuth-card .axis-chart {
    height: 250px;
    min-height: 250px;
  }
}

/* CSS 수정 */
.elevation-card .axis-chart {
  height: 240px;
  min-height: 240px;
  position: relative;
  overflow: hidden;
  clip-path: inset(0 0 50% 0);
  transform: translateY(25%);
}

.elevation-card .axis-chart::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: transparent;
  pointer-events: none;
}

</style>
