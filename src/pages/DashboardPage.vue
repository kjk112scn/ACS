``<template>
  <q-page class="dashboard-container q-pa-md">
    <!-- 상단 부분: 실시간 ICD 데이터 표시 (3축으로 구분) -->
    <!-- 기존의 q-card와 q-card-section 제거하고 axis-grid만 남기기 -->
    <div class="axis-grid">
      <!-- Azimuth 축 데이터 -->
      <q-card class="axis-card azimuth-card">
        <q-card-section>
          <div class="text-subtitle1 text-weight-bold text-center">Azimuth</div>

          <!-- Azimuth 차트 영역 추가 -->
          <div class="axis-chart" ref="azimuthChartRef" style="height: 220px !important; min-height: 220px !important;">
          </div>

          <div class="axis-data-row">
            <div class="axis-data-item">
              <q-item-label class="adaptive-caption">CMD</q-item-label>
              <q-item-label class="adaptive-text">{{ displayValue(azimuthCmdValue) }}</q-item-label>
            </div>
            <div class="axis-data-item">
              <q-item-label class="adaptive-caption">Actual</q-item-label>
              <q-item-label class="adaptive-text">{{ displayValue(azimuthActualValue) }}</q-item-label>
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
          <div class="text-subtitle1 text-weight-bold text-center">Elevation</div>

          <!-- Elevation 차트 영역 추가 -->
          <div class="axis-chart" ref="elevationChartRef"
            style="height: 220px !important; min-height: 220px !important;"></div>

          <div class="axis-data-row">
            <div class="axis-data-item">
              <q-item-label class="adaptive-caption">CMD</q-item-label>
              <q-item-label class="adaptive-text">{{ displayValue(elevationCmdValue) }}</q-item-label>
            </div>
            <div class="axis-data-item">
              <q-item-label class="adaptive-caption">Actual</q-item-label>
              <q-item-label class="adaptive-text">{{ displayValue(elevationActualValue) }}</q-item-label>
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
          <div class="text-subtitle1 text-weight-bold text-center">Tilt</div>

          <!-- Tilt 차트 영역 추가 -->
          <div class="axis-chart" ref="trainChartRef" style="height: 220px !important; min-height: 220px !important;">
          </div>

          <div class="axis-data-row">
            <div class="axis-data-item">
              <q-item-label class="adaptive-caption">CMD</q-item-label>
              <q-item-label class="adaptive-text">{{ displayValue(trainCmdValue) }}</q-item-label>
            </div>
            <div class="axis-data-item">
              <q-item-label class="adaptive-caption">Actual</q-item-label>
              <q-item-label class="adaptive-text">{{ displayValue(trainActualValue) }}</q-item-label>
            </div>
            <div class="axis-data-item">
              <q-item-label class="adaptive-caption">Speed</q-item-label>
              <q-item-label class="adaptive-text">{{
                displayValue(icdStore.trainSpeed)
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
              <q-btn class="full-width" :color="acsEmergencyActive ? 'grey-8' : 'negative'"
                :label="acsEmergencyActive ? 'Emergency Active' : 'Emergency Stop'" @click="handleEmergencyClick"
                size="md" />
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
              <q-btn flat label="해제" color="primary" @click="
                () => {
                  releaseEmergency()
                  emergencyModal = false
                }
              " v-close-popup />
            </q-card-actions>
          </q-card>
        </q-dialog>

        <!-- Control 카드 -->
        <q-card class="control-card">
          <q-card-section>
            <div class="text-subtitle1 text-weight-bold">Control</div>
            <div class="control-content">
              <div class="control-buttons q-gutter-y-sm">
                <!-- 1행: Control LED 인디케이터 -->
                <div class="control-status-item">
                  <div class="control-led-container">
                    <div class="control-led led-control"></div>
                    <span class="control-label">Control</span>
                  </div>
                </div>

                <!-- 2행: Monitoring LED 인디케이터 -->
                <div class="control-status-item">
                  <div class="control-led-container">
                    <div class="control-led led-monitoring"></div>
                    <span class="control-label">Monitoring</span>
                  </div>
                </div>

                <!-- 3행: Control Request 버튼 -->
                <q-btn color="info" label="Control Request" class="full-width" />
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Status 카드 -->
      <q-card class="status-card">
        <q-card-section>
          <div class="text-subtitle1 text-weight-bold">Status</div>
          <div class="status-content">
            <!-- Status LED 그룹 - Control 카드의 control-buttons와 동일한 구조 -->
            <div class="status-leds-group">
              <!-- Emergency LED - TRUE면 빨간색, FALSE면 녹색 (고정 위치 - 맨 위) -->
              <div class="status-item status-item-top">
                <div class="status-led-container">
                  <div class="status-led" :class="{
                    'led-error': errorEmergencyActive,
                    'led-normal': !errorEmergencyActive,
                  }"></div>
                  <span class="status-label">Emergency</span>
                </div>
              </div>

              <!-- 중간 LED 그룹 - Emergency와 Stow Pin 사이의 LED들 (자동 간격 조절) -->
              <div class="status-middle-group">
                <!-- Positioner LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item">
                  <div class="status-led-container">
                    <div class="status-led" :class="{
                      'led-error': errorPositionerActive,
                      'led-normal': !errorPositionerActive,
                    }"></div>
                    <span class="status-label">Positioner</span>
                  </div>
                </div>

                <!-- Feed LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item">
                  <div class="status-led-container">
                    <div class="status-led" :class="{ 'led-error': errorFeedActive, 'led-normal': !errorFeedActive }">
                    </div>
                    <span class="status-label">Feed</span>
                  </div>
                </div>

                <!-- Protocol LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item">
                  <div class="status-led-container">
                    <div class="status-led" :class="{
                      'led-error': errorProtocolActive,
                      'led-normal': !errorProtocolActive,
                    }"></div>
                    <span class="status-label">Protocol</span>
                  </div>
                </div>

                <!-- Power LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item">
                  <div class="status-led-container">
                    <div class="status-led" :class="{ 'led-error': errorPowerActive, 'led-normal': !errorPowerActive }">
                    </div>
                    <span class="status-label">Power</span>
                  </div>
                </div>

                <!-- ✅ Stow LED - TRUE면 녹색, FALSE면 회색 -->
                <div class="status-item">
                  <div class="status-led-container">
                    <div class="status-led" :class="{ 'led-stow-active': stowActive, 'led-inactive': !stowActive }">
                    </div>
                    <span class="status-label">Stow</span>
                  </div>
                </div>
              </div>

              <!-- ✅ Stow Pin LED - TRUE면 녹색, FALSE면 회색 (Monitoring LED와 같은 높이 - 고정) -->
              <div class="status-item status-item-bottom">
                <div class="status-led-container">
                  <div class="status-led" :class="{ 'led-stow-active': stowPinActive, 'led-inactive': !stowPinActive }">
                  </div>
                  <span class="status-label">Stow Pin</span>
                </div>
              </div>

              <!-- All Status 버튼 (Control Request 버튼과 같은 높이) - control-buttons 안에 버튼이 있는 것과 동일한 구조 -->
              <q-btn color="primary" label="All Status" class="full-width" @click="handleAllStatus" />
            </div>
          </div>
        </q-card-section>
      </q-card>
    </div>

    <!-- 모드 선택 탭 -->
    <q-card flat bordered class="mode-selection-section">
      <q-card-section class="mode-selection-wrapper">
        <q-tabs v-model="currentMode" class="text-primary compact-tabs" active-color="white" indicator-color="transparent"
          align="left" narrow-indicator dense>
          <q-tab name="standby" label="Standby" icon="pause_circle_outline" inline-label class="mode-tab"
            :class="{ 'mode-tab--active': currentMode === 'standby' }" @click="navigateToMode('standby')" />
          <q-tab name="step" label="Step" icon="stairs" inline-label class="mode-tab"
            :class="{ 'mode-tab--active': currentMode === 'step' }" @click="navigateToMode('step')" />
          <q-tab name="slew" label="Slew" icon="sync_alt" inline-label class="mode-tab"
            :class="{ 'mode-tab--active': currentMode === 'slew' }" @click="navigateToMode('slew')" />
          <q-tab name="pedestal" label="Pedestal Position" icon="near_me" inline-label class="mode-tab"
            :class="{ 'mode-tab--active': currentMode === 'pedestal' }" @click="navigateToMode('pedestal')" />
          <q-tab name="ephemeris" label="Ephemeris Designation" icon="public" inline-label class="mode-tab"
            :class="{ 'mode-tab--active': currentMode === 'ephemeris' }" @click="navigateToMode('ephemeris')" />
          <q-tab name="pass-schedule" label="Pass Schedule" icon="event_available" inline-label class="mode-tab"
            :class="{ 'mode-tab--active': currentMode === 'pass-schedule' }" @click="navigateToMode('pass-schedule')" />
          <q-tab name="suntrack" label="Sun Track" icon="wb_sunny" inline-label class="mode-tab"
            :class="{ 'mode-tab--active': currentMode === 'suntrack' }" @click="navigateToMode('suntrack')" />
          <q-tab name="feed" label="Feed" icon="rss_feed" inline-label class="mode-tab"
            :class="{ 'mode-tab--active': currentMode === 'feed' }" @click="navigateToMode('feed')" />
        </q-tabs>
      </q-card-section>
    </q-card>

    <!-- 모드 컨텐츠 섹션 -->
    <q-card class="mode-content-section">
      <q-card-section>
        <!-- 라우터 뷰를 사용하여 현재 모드에 맞는 컴포넌트 표시 -->
        <router-view />
      </q-card-section>
    </q-card>
  </q-page>

  <!-- All Status 모달 추가 -->
  <!-- <AllStatus v-model="showAllStatusModal" /> -->
</template>
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useICDStore } from '../stores/icd/icdStore'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { openComponent } from '../utils/windowUtils'
import { useTheme } from '../composables/useTheme'
import type { MessageData } from '../services/api/icdService'

const icdStore = useICDStore()
const router = useRouter()
const route = useRoute()

// 테마 관련 추가
const { initializeTheme } = useTheme()

// Dashboard 페이지용 WebSocket 메시지 핸들러
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const handleDashboardMessage = (_message: MessageData) => {
  // console.log('📊 Dashboard 메시지 수신:', _message)
  // 필요시 추가 처리 로직 (예: 특정 데이터 변경 감지, 알림 등)
}

// 차트 관련
const azimuthChartRef = ref<HTMLElement | null>(null)
const elevationChartRef = ref<HTMLElement | null>(null)
const trainChartRef = ref<HTMLElement | null>(null)

let azimuthChart: ECharts | undefined = undefined
let elevationChart: ECharts | undefined = undefined
let trainChart: ECharts | undefined = undefined

const chartsInitialized = ref(false)

const acsEmergencyActive = ref(false)
const emergencyModal = ref(false)

const errorEmergencyActive = computed(() => {
  return (
    acsEmergencyActive.value ||
    icdStore.mainBoardStatusInfo.emergencyStopACU ||
    icdStore.mainBoardStatusInfo.emergencyStopPositioner
  )
})

const errorPositionerActive = computed(() => {
  // ✅ Azimuth 축 상태 체크 (ServoBrake, ServoMotor 제외)
  const azimuthError =
    icdStore.azimuthBoardStatusInfo.limitSwitchNegative275 ||
    icdStore.azimuthBoardStatusInfo.limitSwitchPositive275 ||
    icdStore.azimuthBoardStatusInfo.encoder ||
    icdStore.azimuthBoardServoStatusInfo.servoAlarm

  // ✅ Elevation 축 상태 체크 (ServoBrake, ServoMotor 제외)
  const elevationError =
    icdStore.elevationBoardStatusInfo.limitSwitchNegative5 ||
    icdStore.elevationBoardStatusInfo.limitSwitchNegative0 ||
    icdStore.elevationBoardStatusInfo.limitSwitchPositive180 ||
    icdStore.elevationBoardStatusInfo.limitSwitchPositive185 ||
    icdStore.elevationBoardStatusInfo.encoder ||
    icdStore.elevationBoardServoStatusInfo.servoAlarm

  // ✅ Train 축 상태 체크 (ServoBrake, ServoMotor 제외)
  const trainError =
    icdStore.trainBoardStatusInfo.limitSwitchNegative275 ||
    icdStore.trainBoardStatusInfo.limitSwitchPositive275 ||
    icdStore.trainBoardStatusInfo.encoder ||
    icdStore.trainBoardServoStatusInfo.servoAlarm

  // ✅ 하나라도 에러가 있으면 true 반환
  return azimuthError || elevationError || trainError
})
const errorFeedActive = computed(() => {
  // ✅ Feed X Board Error Status 체크
  const feedXError =
    icdStore.feedXBoardStatusInfo.fanError ||
    icdStore.feedXBoardStatusInfo.xLnaRHCPError ||
    icdStore.feedXBoardStatusInfo.xLnaLHCPError

  // ✅ Feed S Board Error Status 체크
  const feedSError =
    icdStore.feedSBoardStatusInfo.sLnaRHCPError ||
    icdStore.feedSBoardStatusInfo.sLnaLHCPError ||
    icdStore.feedSBoardStatusInfo.sRFSwitchError

  // ✅ 하나라도 에러가 있으면 true 반환
  return feedXError || feedSError
})

const errorProtocolActive = computed(() => {
  // ✅ Protocol Status 체크 - 하나라도 활성화되면 에러로 판단
  const protocolError =
    icdStore.protocolStatusInfo.elevation ||
    icdStore.protocolStatusInfo.azimuth ||
    icdStore.protocolStatusInfo.train ||
    icdStore.protocolStatusInfo.feed

  return protocolError
})

const errorPowerActive = computed(() => {
  const powerError =
    icdStore.mainBoardStatusInfo.powerSurgeProtector ||
    icdStore.mainBoardStatusInfo.powerReversePhaseSensor
  return powerError
})

const stowActive = computed(() => {
  return (
    acsEmergencyActive.value ||
    icdStore.mainBoardStatusInfo.emergencyStopACU ||
    icdStore.mainBoardStatusInfo.emergencyStopPositioner
  )
})
const stowPinActive = computed(() => {
  return (
    icdStore.azimuthBoardStatusInfo.stowPin ||
    icdStore.elevationBoardStatusInfo.stowPin
  )
})

// 추가 상태 LED들
/*
// 실제 데이터와 연결하는 경우 (예시)
const errorPositionerActive = computed(() => icdStore.positionerStatus === 'active')
const errorFeedActive = computed(() => icdStore.feedStatus === 'active')
const errorProtocolActive = computed(() => icdStore.protocolStatus === 'active')
const errorPowerActive = computed(() => icdStore.powerStatus === 'active')
const stowActive = computed(() => icdStore.stowStatus === 'active')
const stowPinActive = computed(() => icdStore.stowPinStatus === 'active')
 */
// ✅ 30ms UI 업데이트 타이머
let uiUpdateTimer: number | null = null
const uiUpdateCount = ref(0)
///computed

// ✅ 값 표시 헬퍼 함수
const displayValue = (value: string | number | null | undefined) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }

  const num = Number(value)
  if (!isNaN(num)) {
    return num.toFixed(2)
  }

  return value
}

// ✅ 30ms마다 차트만 업데이트 (데이터는 icdStore에서 자동 업데이트)
const updateCharts = () => {
  if (!chartsInitialized.value || !icdStore.isConnected) {
    return
  }

  try {
    const updateOption = {
      animation: false,
      silent: true,
    }

    // 1. Azimuth 차트 업데이트
    if (azimuthChart && icdStore.azimuthAngle !== undefined) {
      const azimuth = Number(icdStore.azimuthAngle)
      if (!isNaN(azimuth)) {
        const normalizedAzimuth = azimuth < 0 ? azimuth + 360 : azimuth
        azimuthChart.setOption(
          {
            series: [
              {
                data: [[1, normalizedAzimuth]],
                label: {
                  formatter: () => `${azimuth.toFixed(2)}°`,
                },
              },
            ],
          },
          updateOption,
        )
      }
    }

    // 2. Elevation 차트 업데이트
    if (elevationChart && icdStore.elevationAngle !== undefined) {
      const elevation = Number(icdStore.elevationAngle)
      if (!isNaN(elevation)) {
        const normalizedElevation = elevation < 0 ? elevation + 360 : elevation % 360
        elevationChart.setOption(
          {
            series: [
              {
                data: [[0, normalizedElevation]],
                label: {
                  formatter: () => `${elevation.toFixed(2)}°`,
                },
              },
            ],
          },
          updateOption,
        )
      }
    }

    // 3. Train 차트 업데이트
    if (trainChart && icdStore.trainAngle !== undefined) {
      const train = Number(icdStore.trainAngle)
      if (!isNaN(train)) {
        const normalizedTrain = train < 0 ? train + 360 : train
        trainChart.setOption(
          {
            series: [
              {
                data: [[1, normalizedTrain]],
                label: {
                  formatter: () => `${train.toFixed(2)}°`,
                },
              },
            ],
          },
          updateOption,
        )
      }
    }

    uiUpdateCount.value++

    // 100번마다 로그
    /*  if (uiUpdateCount.value % 100 === 0) {
      console.log(`🔄 [${uiUpdateCount.value}] 차트 업데이트:`, {
        azimuth: icdStore.azimuthAngle,
        elevation: icdStore.elevationAngle,
        train: icdStore.trainAngle,
        serverTime: icdStore.serverTime,
        storeUpdateCount: icdStore.updateCount,
      })
    } */
  } catch (error) {
    console.error('❌ 차트 업데이트 오류:', error)
  }
}

// ✅ 30ms 차트 업데이트 타이머 시작
const startChartUpdates = () => {
  if (uiUpdateTimer) {
    clearInterval(uiUpdateTimer)
  }

  console.log('🚀 차트 업데이트 타이머 시작 (30ms)')

  uiUpdateTimer = window.setInterval(() => {
    updateCharts()
  }, 30)
}

// ✅ 차트 업데이트 타이머 중지
const stopChartUpdates = () => {
  if (uiUpdateTimer) {
    clearInterval(uiUpdateTimer)
    uiUpdateTimer = null

    console.log('⏹️ 차트 업데이트 타이머 중지')
  }
}

let debugTimer: number | null = null
onMounted(async () => {
  console.log('📱 DashboardPage 컴포넌트 마운트됨')

  // 테마 초기화
  initializeTheme()

  // 라우트 설정
  const pathParts = route.path.split('/')
  const currentPathMode = pathParts[pathParts.length - 1]

  if (
    currentPathMode &&
    ['ephemeris', 'pedestal', 'suntrack', 'feed', 'standby', 'step', 'slew'].includes(
      currentPathMode,
    )
  ) {
    currentMode.value = currentPathMode
  } else {
    void router.push('/dashboard/standby')
  }
  console.log('🚀 DashboardPage 마운트됨')
  // 2. 전역 store 공유 설정 (가장 먼저)
  console.log('🌍 Store 전역 공유 설정 중...')
  window.sharedICDStore = icdStore
  console.log('✅ Store 전역 공유 설정 완료')

  // 3. icdStore 초기화 (WebSocket + 30ms 데이터 업데이트)
  console.log('🚀 시스템 초기화 시작')
  try {
    await icdStore.initialize()
    console.log('✅ 시스템 초기화 완료')

    // Dashboard 페이지용 구독 추가
    icdStore.subscribeWebSocket('dashboard', handleDashboardMessage)
    console.log('📡 Dashboard WebSocket 구독 추가됨')
  } catch (error) {
    console.error('❌ 시스템 초기화 실패:', error)
  }

  // 4. 차트 초기화 (시스템 초기화 후)
  setTimeout(() => {
    try {
      initCharts()
      chartsInitialized.value = true
      console.log('✅ 차트 초기화 완료')

      // 5. 차트 초기화 완료 후 차트 업데이트 시작
      void startChartUpdates()
    } catch (error) {
      console.error('❌ 차트 초기화 실패:', error)
    }
  }, 100)

  // 6. 리사이즈 핸들러 등록
  const handleResize = () => {
    if (chartsInitialized.value) {
      azimuthChart?.resize()
      elevationChart?.resize()
      trainChart?.resize()
    }
  }
  window.addEventListener('resize', handleResize)

  // 7. 디버그 타이머 시작 (5초마다 전체 상태 요약)
  debugTimer = window.setInterval(() => {
    console.log('📋 === 전체 상태 요약 ===')
    console.log('🔄 Ephemeris 활성화:', icdStore.ephemerisStatusInfo.isActive)
    console.log('📊 현재 표시 값들:')
    console.log('  - Azimuth Actual:', azimuthActualValue.value)
    console.log('  - Elevation Actual:', elevationActualValue.value)
    console.log('  - Train Actual:', trainActualValue.value)
    console.log('  - Azimuth CMD:', azimuthCmdValue.value)
    console.log('  - Elevation CMD:', elevationCmdValue.value)
    console.log('  - Train CMD:', trainCmdValue.value)
    console.log('📊 원본 데이터:')
    console.log('  일반 모드:', {
      azimuth: icdStore.azimuthAngle,
      elevation: icdStore.elevationAngle,
      train: icdStore.trainAngle,
      cmdAzimuth: icdStore.cmdAzimuthAngle,
      cmdElevation: icdStore.cmdElevationAngle,
      cmdTrain: icdStore.cmdTrainAngle,
    })
    console.log('  추적 모드:', {
      azimuth: icdStore.trackingActualAzimuthAngle,
      elevation: icdStore.trackingActualElevationAngle,
      train: icdStore.trackingActualTrainAngle,
      cmdAzimuth: icdStore.trackingCMDAzimuthAngle,
      cmdElevation: icdStore.trackingCMDElevationAngle,
      cmdTrain: icdStore.trackingCMDTrainAngle,
    })
    console.log('========================')
  }, 5000)
})

onUnmounted(() => {
  console.log('🧹 DashboardPage 정리 시작')

  // 1. Dashboard 페이지용 구독 제거
  icdStore.unsubscribeWebSocket('dashboard', handleDashboardMessage)
  console.log('📡 Dashboard WebSocket 구독 제거됨')

  // 2. 차트 업데이트 타이머 중지
  stopChartUpdates()

  // 3. 디버그 타이머 정리
  if (debugTimer) {
    clearInterval(debugTimer)
    debugTimer = null
  }

  // 4. 이벤트 리스너 제거
  window.removeEventListener('resize', () => { })

  // 5. icdStore 정리
  icdStore.cleanup()

  console.log('✅ DashboardPage 정리 완료')
})

// 현재 모드 상태
const currentMode = ref('ephemeris')

// 모드 변경 시 해당 라우트로 이동
const navigateToMode = (mode: string) => {
  // void 연산자를 사용하여 Promise를 명시적으로 무시
  void router.push(`/dashboard/${mode}`)
}

// 새로운 컨트롤 관련 상태 변수들
// 현재 사용하지 않는 변수들이지만 향후 사용 가능성이 있어 주석 처리
// const manualControl = ref(false)
// const manualSpeed = ref(50)

// 상태 정보 관련 computed 속성들 - 템플릿에서 사용되는 경우 주석 해제 필요
// 현재 사용하지 않는 computed 속성들이지만 향후 사용 가능성이 있어 주석 처리
/*
const operationMode = computed(() => {
  return icdStore.modeStatusBits ? `Mode ${icdStore.modeStatusBits}` : 'Unknown'
})

const systemStatus = computed(() => {
  if (!icdStore.isConnected) return 'Disconnected'
  if (icdStore.error) return 'Error'
  return 'Normal Operation'
})

const errorCode = computed(() => {
  return icdStore.error ? 'ERR-001' : null
})

const formattedLastUpdate = computed(() => {
  if (!icdStore.serverTime) return 'N/A'
  try {
    const date = new Date(icdStore.serverTime)
    return date.toLocaleTimeString()
  } catch (e) {
    return 'Invalid Time'
  }
})
*/

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

// ✅ 조건부 데이터 computed 속성들 (실제 추적 상태 확인)
// 테스트 예정
// 백앤드에서 오프셋 변경 시 조건 확인 가능 여기서 조건 확인되면 수동 이동 하는 방식으로 검토중.
/*
const azimuthCmdValue = computed((): number => {

  const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingCMDAzimuthAngle : icdStore.cmdAzimuthAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue

})

const azimuthActualValue = computed((): number => {
  const numValue = Number(icdStore.azimuthAngle)
  return isNaN(numValue) ? 0 : numValue
})


const elevationCmdValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingCMDElevationAngle : icdStore.cmdElevationAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})

const elevationActualValue = computed((): number => {
  const numValue = Number(icdStore.elevationAngle)
  return isNaN(numValue) ? 0 : numValue
})

const trainCmdValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingCMDTrainAngle : icdStore.cmdTrainAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})

const trainActualValue = computed((): number => {
  const numValue = Number(icdStore.trainAngle)
  return isNaN(numValue) ? 0 : numValue
})
 */

const azimuthCmdValue = computed((): number => {

  const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingCMDAzimuthAngle : icdStore.cmdAzimuthAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue

})

const azimuthActualValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingActualAzimuthAngle : icdStore.azimuthAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})


const elevationCmdValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingCMDElevationAngle : icdStore.cmdElevationAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})

const elevationActualValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingActualElevationAngle : icdStore.elevationAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})

const trainCmdValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingCMDTrainAngle : icdStore.cmdTrainAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})

const trainActualValue = computed((): number => {
  const numValue = Number(icdStore.trainAngle)
  return isNaN(numValue) ? 0 : numValue
})

// ✅ 차트에서 사용할 실제 값들을 computed로 변경 (실제 추적 상태 확인)
const getCurrentAzimuthActualValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisStatusInfo.isActive || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingActualAzimuthAngle : icdStore.azimuthAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})

const getCurrentElevationActualValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisStatusInfo.isActive || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingActualElevationAngle : icdStore.elevationAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})

const getCurrentTrainActualValue = computed((): number => {
  const isTrackingActive = icdStore.ephemerisStatusInfo.isActive || icdStore.passScheduleStatusInfo.isActive
  const value = isTrackingActive ? icdStore.trackingActualTrainAngle : icdStore.trainAngle
  const numValue = Number(value)
  return isNaN(numValue) ? 0 : numValue
})

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

    // ✅ computed 값으로 초기 데이터 설정
    const azimuth = getCurrentAzimuthActualValue.value
    console.log('Initial Azimuth value:', azimuth)

    // Azimuth 차트만의 옵션 설정
    const azimuthOption = {
      backgroundColor: 'transparent',
      grid: { containLabel: true },
      polar: {
        radius: ['0%', '70%'], // 80%에서 70%로 줄여서 여백 확보
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
          fontSize: 12, // 13에서 12로 줄여서 공간 확보
          distance: 20, // 25에서 20으로 줄여서 공간 확보
          rich: {
            vAlign: {
              align: 'center',
              padding: [0, 0, 1, 0],
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
          symbolSize: 10, // 12에서 10으로 줄여서 공간 확보
          itemStyle: { color: '#ff5722' },
          data: [[1, azimuth]], // [radius, angle] 형식으로 변경
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
            distance: 5, // 0에서 5로 증가하여 여백 확보
            color: '#ff5722',
            fontSize: 13, // 15에서 13으로 줄여서 공간 확보
            padding: [2, 6], // [4, 8]에서 [2, 6]으로 줄여서 공간 확보
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
    // 초기 train 값 가져오기
    const elevation = getCurrentElevationActualValue.value // ✅ computed 값 사용
    const normalizedInitialElevation = elevation < 0 ? elevation + 360 : elevation % 360
    // Elevation 차트만의 옵션 설정
    const elevationOption = {
      backgroundColor: 'transparent',
      grid: { containLabel: true },
      polar: {
        radius: ['0%', '70%'], // 80%에서 70%로 줄여서 여백 확보
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
          fontSize: 12, // 13에서 12로 줄여서 공간 확보
          distance: 20, // 25에서 20으로 줄여서 공간 확보
          rich: {
            upLabel: {
              align: 'center',
              padding: [0, 0, 8, 0], // 10에서 8로 줄여서 공간 확보
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
          symbolSize: 10, // 12에서 10으로 줄여서 공간 확보
          itemStyle: { color: '#2196f3' },
          data: [[0, normalizedInitialElevation]],
          zlevel: 2,
          label: {
            show: true,
            formatter: function () {
              return `${elevation.toFixed(2)}°`
            },
            position: 'top',
            distance: 5, // 0에서 5로 증가하여 여백 확보
            color: '#2196f3',
            fontSize: 13, // 15에서 13으로 줄여서 공간 확보
            padding: [2, 6], // [4, 8]에서 [2, 6]으로 줄여서 공간 확보
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

  // 3. Train 차트 초기화
  if (trainChartRef.value) {
    if (trainChart) {
      trainChart.dispose()
    }
    trainChart = echarts.init(trainChartRef.value)

    // 초기 traub 값 가져오기
    const train = getCurrentTrainActualValue.value // ✅ computed 값 사용
    const normalizedInitialTrain = train < 0 ? train + 360 : train % 360

    const trainOption = {
      backgroundColor: 'transparent',
      grid: { containLabel: true },
      polar: {
        radius: ['0%', '70%'], // 80%에서 70%로 줄여서 여백 확보
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
          fontSize: 12, // 13에서 12로 줄여서 공간 확보
          distance: 20, // 25에서 20으로 줄여서 공간 확보
          rich: {
            vAlign: {
              align: 'center',
              padding: [0, 0, 1, 0], // 2에서 1로 줄여서 공간 확보
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
          symbolSize: 10, // 12에서 10으로 줄여서 공간 확보
          itemStyle: { color: '#4caf50' },
          data: [[1, normalizedInitialTrain]], // 초기값을 현재 train 값으로 설정
          zlevel: 2,
          label: {
            show: true,
            formatter: function () {
              return `${train.toFixed(2)}°`
            },
            position: 'top',
            distance: 5, // 0에서 5로 증가하여 여백 확보
            color: '#4caf50',
            fontSize: 13, // 15에서 13으로 줄여서 공간 확보
            padding: [2, 6], // [4, 8]에서 [2, 6]으로 줄여서 공간 확보
            backgroundColor: 'rgba(0,0,0,0.5)',
            borderRadius: 4,
          },
        },
      ],
      animation: false,
    }

    trainChart.setOption(trainOption)
  }

  // 모든 차트 초기화 후 명시적으로 리사이즈 호출
  setTimeout(() => {
    if (azimuthChart) azimuthChart.resize()
    if (elevationChart) elevationChart.resize()
    if (trainChart) trainChart.resize()
  }, 0)
}
// Emergency 버튼 클릭 핸들러
const handleEmergencyClick = async () => {
  console.log('Emergency 버튼 클릭됨')

  if (!acsEmergencyActive.value) {
    // 비상 정지 활성화 ('E' 명령 전송)
    try {
      await icdStore.sendEmergency('E')
      acsEmergencyActive.value = true
      console.log('Emergency Stop 활성화됨')
    } catch (error) {
      console.error('Emergency Stop 활성화 실패:', error)
    }
  } else {
    // 이미 비상 정지 상태인 경우 해제 확인 모달 표시
    emergencyModal.value = true
  }
}

// Emergency 해제 함수
const releaseEmergency = async () => {
  console.log('releaseEmergency 함수 호출됨') // 디버깅 로그 추가

  try {
    await icdStore.sendEmergency('S')

    acsEmergencyActive.value = false
    console.log('Emergency Stop 해제됨')
  } catch (error) {
    console.error('Emergency Stop 해제 실패:', error)
  }
}

// ✅ 디버깅용 - Ephemeris 상태 변경 감시
watch(
  () => icdStore.ephemerisStatusInfo.isActive,
  (newVal) => {
    console.log('🔄 Ephemeris 상태 변경:', newVal)
    console.log(
      '📊 Azimuth Actual 값:',
      newVal ? icdStore.trackingActualAzimuthAngle : icdStore.azimuthAngle,
    )
    console.log(
      '📊 Elevation Actual 값:',
      newVal ? icdStore.trackingActualElevationAngle : icdStore.elevationAngle,
    )
    console.log(
      '📊 Train Actual 값:',
      newVal ? icdStore.trackingActualTrainAngle : icdStore.trainAngle,
    )
    console.log(
      '📊 Azimuth CMD 값:',
      newVal ? icdStore.trackingCMDAzimuthAngle : icdStore.cmdAzimuthAngle,
    )
    console.log(
      '📊 Elevation CMD 값:',
      newVal ? icdStore.trackingCMDElevationAngle : icdStore.cmdElevationAngle,
    )
    console.log('📊 Train CMD 값:', newVal ? icdStore.trackingCMDTrainAngle : icdStore.cmdTrainAngle)
  },
)
/*
// ✅ 개별 값 변경 감시
watch(
  () => icdStore.azimuthAngle,
  (newVal) => {
    console.log('🎯 일반 Azimuth 각도 변경:', newVal)
  },
)

watch(
  () => icdStore.trackingActualAzimuthAngle,
  (newVal) => {
    console.log('🛰️ 추적 Azimuth 각도 변경:', newVal)
  },
)

watch(
  () => icdStore.elevationAngle,
  (newVal) => {
    console.log('🎯 일반 Elevation 각도 변경:', newVal)
  },
)

watch(
  () => icdStore.trackingActualElevationAngle,
  (newVal) => {
    console.log('🛰️ 추적 Elevation 각도 변경:', newVal)
  },
)

watch(
  () => icdStore.trainAngle,
  (newVal) => {
    console.log('🎯 일반 Train 각도 변경:', newVal)
  },
)

watch(
  () => icdStore.trackingActualTrainAngle,
  (newVal) => {
    console.log('🛰️ 추적 Train 각도 변경:', newVal)
  },
)

// ✅ computed 값 변경 감시
watch(
  () => azimuthActualValue.value,
  (newVal) => {
    console.log(
      '📈 표시되는 Azimuth Actual 값:',
      newVal,
      `(Ephemeris: ${icdStore.ephemerisStatusInfo.isActive})`,
    )
  },
)

watch(
  () => elevationActualValue.value,
  (newVal) => {
    console.log(
      '📈 표시되는 Elevation Actual 값:',
      newVal,
      `(Ephemeris: ${icdStore.ephemerisStatusInfo.isActive})`,
    )
  },
)

watch(
  () => trainActualValue.value,
  (newVal) => {
    console.log(
      '📈 표시되는 Train Actual 값:',
      newVal,
      `(Ephemeris: ${icdStore.ephemerisStatusInfo.isActive})`,
    )
  },
)

watch(
  () => azimuthCmdValue.value,
  (newVal) => {
    console.log(
      '📈 표시되는 Azimuth CMD 값:',
      newVal,
      `(Ephemeris: ${icdStore.ephemerisStatusInfo.isActive})`,
    )
  },
)

watch(
  () => elevationCmdValue.value,
  (newVal) => {
    console.log(
      '📈 표시되는 Elevation CMD 값:',
      newVal,
      `(Ephemeris: ${icdStore.ephemerisStatusInfo.isActive})`,
    )
  },
)

watch(
  () => trainCmdValue.value,
  (newVal) => {
    console.log(
      '📈 표시되는 Train CMD 값:',
      newVal,
      `(Ephemeris: ${icdStore.ephemerisStatusInfo.isActive})`,
    )
  },
) */
// ✅ Window 인터페이스 확장으로 타입 안전성 확보
declare global {
  interface Window {
    sharedICDStore?: ReturnType<typeof useICDStore>
  }
}

// All Status 버튼 핸들러 - 스마트 중앙 배치
const handleAllStatus = () => {
  console.log('All Status 버튼 클릭됨')
  // ✅ 이렇게 되어야 함!
  void openComponent('all-status', {
    mode: 'popup',
    width: 1700,
    height: 700,
    location: false,
  })
  /*
  void openComponent('all-status', {
    mode: 'modal', // 'popup' | 'modal' | 'auto'
    width: 1700, // 너비
    height: 700, // 높이
    props: {
      // 컴포넌트에 전달할 props
      customData: 'some data',
      theme: 'dark',
    },
    onClose: () => {
      console.log('창이 닫혔습니다')
    },
    onError: (error) => {
      console.error('오류 발생:', error)
      alert('창을 열 수 없습니다.')
    },
  }) */
}

// 수동으로 배치 방식 선택하고 싶다면
/* const handleAllStatusManual = () => {
  const baseUrl = window.location.origin
  const popupUrl = `${baseUrl}/#/popup/all-status`

  // 현재 창 기준 중앙 배치 (듀얼 모니터 고려)
  const popup = openCenteredPopup(popupUrl, 'AllStatusPopup', {
    width: 1400,
    height: 900,
    relativeTo: 'window', // 'window' 또는 'screen'
  })

  if (!popup) {
    alert('팝업이 차단되었습니다.')
  }
}
// AllStatus 모달 상태
//const showAllStatusModal = ref(false)
// All Status 버튼 핸들러
const handleAllStatus = () => {
  console.log('All Status 버튼 클릭됨')
  openAllStatusPopup()
  //showAllStatusModal.value = true
}
 */
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


/* 1920x1080 최적화된 dashboard-container 스타일 */
.dashboard-container {
  max-width: 100%;
  /* 화면 전체 너비 사용 */
  /* 스크롤 방지를 위해 조정 */
  margin: 0 auto;
  background-color: var(--theme-background);
  min-height: 100vh;
  padding: 4.4rem 0.875rem 0rem 0.875rem;
  /* 상단 4.4rem(70.4px) 복구, 좌우 0.875rem(14px), 하단 0rem(0px) = 상단 공간 복구하고 하단은 mode 높이 최대화 */
  display: flex;
  flex-direction: column;
  /* 세로 방향 flexbox로 mode-content-section이 남은 공간 사용 */
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.1rem;
  padding: 1rem;
  background-color: var(--theme-surface);
  border-radius: 8px;
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
}

.cmd-time {
  display: flex;
  align-items: center;
}

.time-value {
  font-weight: 500;
  font-size: 1rem;
  color: var(--theme-text);
}

/* 1920x1080 최적화된 axis-grid - mode-selection-section과 우측 정렬 통일 */
.axis-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 0.8fr 0.8fr;
  /* 전체 너비 사용 */
  gap: 0.8rem;
  margin-top: 1.5rem !important;
  /* 상단 마진을 1.5rem(24px)로 증가 */
  width: 100%;
  /* 전체 너비 사용 */
}

/* 헤더 그림자 제거 - 더 강력한 선택자 사용 */
.q-header,
.q-toolbar,
.q-layout__header,
.q-page__header,
.q-layout__section--marginal,
.q-layout__section--marginal .q-toolbar,
.q-layout__header .q-toolbar,
.q-page__header .q-toolbar,
.q-layout__header .q-toolbar__content,
.q-page__header .q-toolbar__content {
  box-shadow: none !important;
  border-bottom: none !important;
  background: transparent !important;
}

/* 헤더 하단 그림자 효과 제거 - 더 구체적인 선택자 */
.q-layout__header,
.q-page__header,
.q-toolbar,
.q-header,
.q-layout__header .q-toolbar,
.q-page__header .q-toolbar {
  box-shadow: none !important;
  border-bottom: none !important;
  background: transparent !important;
}

/* 페이지 전체 헤더 관련 그림자 제거 */
.q-layout,
.q-page {
  box-shadow: none !important;
}

/* 헤더 배경과 그림자 완전 제거 */
.q-layout__header .q-toolbar,
.q-page__header .q-toolbar {
  box-shadow: none !important;
  border-bottom: none !important;
  background: transparent !important;
}

/* 추가 헤더 관련 요소들 */
.q-layout__header .q-toolbar__content,
.q-page__header .q-toolbar__content,
.q-layout__header .q-toolbar__title,
.q-page__header .q-toolbar__title {
  box-shadow: none !important;
  border-bottom: none !important;
}

/* 헤더의 모든 하위 요소 */
.q-layout__header *,
.q-page__header * {
  box-shadow: none !important;
  border-bottom: none !important;
}

/* 전역 헤더 스타일 오버라이드 */
.q-layout__header::after,
.q-page__header::after,
.q-toolbar::after {
  display: none !important;
}

/* 헤더 관련 모든 그림자와 테두리 제거 */
.q-layout__header,
.q-page__header,
.q-toolbar,
.q-header,
.q-layout__header .q-toolbar,
.q-page__header .q-toolbar,
.q-layout__header .q-toolbar__content,
.q-page__header .q-toolbar__content {
  box-shadow: none !important;
  border-bottom: none !important;
  background: transparent !important;
  text-shadow: none !important;
  filter: none !important;
}

/* 모든 패널의 기본 테두리를 밝은 회색으로 변경 */
.axis-card {
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
  transition: none;
  /* 애니메이션 제거 */
}

.axis-card:hover {
  box-shadow: 0 2px 4px var(--theme-shadow-light);
  /* 기본 그림자 유지 */
  transform: none;
  /* 올라오는 효과 제거 */
}

.axis-card .q-card-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-top: 0.15rem !important;
  /* 상단 패딩 30% 감소: 0.25rem → 0.15rem */
  padding-left: 1rem !important;
  padding-right: 1rem !important;
  padding-bottom: 0 !important;
  /* 하단 패딩 완전 제거: 0.05rem → 0 - CMD/Actual/Speed 하단 빈 공간 제거 */
}

/* 각 축 카드의 상단 테두리만 색상 유지, 나머지는 밝은 회색 */
.azimuth-card {
  border-top: 5px solid var(--theme-border) !important;
  /* 흰색 상단 테두리로 통일하되 두께는 5px로 증가 */
  border-left: 1px solid var(--theme-border);
  /* 밝은 회색 */
  border-right: 1px solid var(--theme-border);
  /* 밝은 회색 */
  border-bottom: 1px solid var(--theme-border);
  /* 밝은 회색 */
  background-color: var(--theme-card-background) !important;
  /* 내부 색상 통일 */
  transition: none;
  /* 애니메이션 제거 */
}

/* Azimuth 카드 호버 효과 제거 */
.azimuth-card:hover {
  box-shadow: 0 2px 4px var(--theme-shadow-light);
  /* 기본 그림자 유지 */
  transform: none;
  /* 올라오는 효과 제거 */
}

/* Azimuth 카드 전용 스타일 - 상단 테두리 색상 제거 */
/* 주석: .axis-card .q-card-section에서 이미 하단 패딩이 설정되므로 별도 설정 불필요 */

.azimuth-card {
  background-color: var(--theme-card-background) !important;
  border-top: 5px solid var(--theme-border) !important;
  /* 흰색 테두리로 통일하되 두께는 5px로 증가 */
}

/* Elevation 카드 전용 스타일 */
/* 주석: .axis-card .q-card-section에서 이미 하단 패딩이 설정되므로 별도 설정 불필요 */

.elevation-card {
  background-color: var(--theme-card-background) !important;
  border-top: 5px solid var(--theme-border) !important;
  /* 흰색 테두리로 통일하되 두께는 5px로 증가 */
}

/* Tilt 카드 전용 스타일 */
/* 주석: .axis-card .q-card-section에서 이미 하단 패딩이 설정되므로 별도 설정 불필요 */

.tilt-card {
  background-color: var(--theme-card-background) !important;
  border-top: 5px solid var(--theme-border) !important;
  /* 흰색 테두리로 통일하되 두께는 5px로 증가 */
}

/* 모든 축 카드의 텍스트 높이 통일 */
/* 주석: .axis-card .q-card-section에서 이미 하단 패딩이 설정되므로 별도 설정 불필요 */

.azimuth-card,
.elevation-card,
.tilt-card {
  background-color: var(--theme-card-background) !important;
  border-top: 5px solid var(--theme-border) !important;
  /* 흰색 테두리로 통일하되 두께는 5px로 증가 */
}

/* Azimuth의 정확한 위치를 Elevation, Tilt에 정확히 적용 */
.azimuth-card .text-subtitle1,
.elevation-card .text-subtitle1,
.tilt-card .text-subtitle1 {
  margin: 0 !important;
  padding: 0.1rem 0 0.3rem 0 !important;
  /* 제목 하단 패딩 30% 감소: 0.7rem → 0.3rem (약 4.8px) */
  text-align: center !important;
  color: var(--theme-text) !important;
  font-size: 1rem !important;
  font-weight: 600 !important;
  line-height: 1.2 !important;
  /* 라인 높이도 통일 */
}

/* Azimuth와 Tilt 차트 높이 동일하게 설정 */
.azimuth-card .axis-chart,
.tilt-card .axis-chart {
  height: 300px !important;
  /* 동일한 높이 */
  min-height: 300px !important;
  width: 100%;
  margin: 0.3rem 0 0.05rem 0 !important;
  /* 상단 마진 30% 감소: 0.7rem → 0.3rem, 하단 마진 50% 감소: 0.1rem → 0.05rem */
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--theme-card-background) !important;
  border-radius: 4px;
  border: none !important;
}

/* Elevation은 기존 높이 유지 */
.elevation-card .axis-chart {
  height: 240px !important;
  min-height: 240px !important;
  width: 100%;
  margin: 0.3rem 0 0.05rem 0 !important;
  /* 상단 마진 30% 감소: 0.7rem → 0.3rem, 하단 마진 50% 감소: 0.1rem → 0.05rem */
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--theme-card-background) !important;
  border-radius: 4px;
  border: none !important;
}

.axis-data-row {
  margin-top: 0.05rem !important;
  /* 상단 마진 50% 감소: 0.1rem → 0.05rem (약 0.8px) - margin-top: auto 제거 */
  margin-bottom: 0;
  padding-bottom: 0;
  display: flex;
  justify-content: space-between;
}

/* 주석: .azimuth-card .text-subtitle1 등에서 이미 스타일이 설정되어 있으므로 중복 제거 */

/* Emergency와 Control 컨테이너 */
.control-container {
  grid-column: span 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  height: 100%;
}

/* Emergency 카드 - 빨간색 상단 테두리 유지 */
.emergency-card {
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
  border-top: 5px solid #f44336 !important;
  /* 빨간색 상단 테두리 유지하되 두께는 5px로 증가 */
  border-radius: 8px;
  flex: 1;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
}

.emergency-content {
  height: 100%;
  display: flex;
  align-items: stretch;
  /* 버튼이 전체 높이를 사용하도록 */
  justify-content: center;
  padding: 0.5rem 0;
  /* Control과 Status와 동일한 패딩으로 통일 */
}

/* Emergency Stop 버튼 높이 50% 감소 */
.emergency-content .q-btn {
  height: 50% !important;
  min-height: 40px !important;
  font-size: 0.9rem !important;
  font-weight: 600 !important;
  padding: 0.5rem 0.5rem !important;
}

/* Control 카드 - 밝은 회색 테두리 */
.control-card {
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  border-top: 5px solid var(--theme-border) !important;
  border-radius: 8px;
  flex: 1;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
}

/* Control 카드 하단 패딩 줄이기 */
.control-card .q-card-section {
  padding-bottom: 0.25rem !important;
  /* 하단 패딩 더 줄이기 - Control Request 버튼 아래 공간 최소화 */
  padding-top: 1rem !important;
  /* 상단 패딩 명시적으로 설정 - Status 카드와 동일하게 맞추기 */
}

.control-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  /* space-between으로 변경하여 버튼을 아래로 내리기 */
  padding: 0.5rem 0 0.1rem 0;
  /* 하단 패딩 더 줄이기: 0.25rem → 0.1rem - Control Request 버튼 아래 공간 최소화 */
}

/* Control Request 버튼 높이 50% 감소 */
.control-content .q-btn {
  height: auto !important;
  min-height: 32px !important;
  /* 버튼 높이 줄이기: 30px → 32px (최소 높이만 설정) */
  font-size: 0.85rem !important;
  padding: 0.35rem 0.5rem !important;
  /* 패딩 30% 감소: 0.4rem → 0.35rem */
}

.control-buttons {
  width: 100%;
}

/* Control Request 버튼만 아래로 내리기 */
.control-buttons .q-btn {
  margin-top: 0.1rem;
  /* Control Request 버튼 상단 마진 추가 - LED와 버튼 사이 간격 유지 */
}

/* Control LED 인디케이터 스타일 - Status LED와 동일한 스타일로 통일 */
.control-status-item {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
}

.control-led-container {
  display: flex;
  align-items: center;
  gap: 12px;
  /* Status와 동일한 간격 */
}

.control-led {
  width: 20px;
  /* Status와 동일한 크기 */
  height: 20px;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}

.control-led.led-control {
  background-color: var(--theme-led-normal);
  /* Status와 동일한 색상 */
  box-shadow: 0 0 8px var(--theme-led-normal), 0 0 16px var(--theme-led-normal);
}

.control-led.led-monitoring {
  background-color: var(--theme-led-inactive);
  /* Status와 동일한 색상 */
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}

.control-label {
  font-size: 1rem;
  /* Status와 동일한 폰트 크기 */
  font-weight: 500;
  color: var(--theme-text);
}

/* Status 카드 - 밝은 회색 테두리 */
.status-card {
  grid-column: span 1;
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
  border-top: 5px solid var(--theme-border) !important;
  /* 흰색 테두리로 통일하되 두께는 5px로 증가 */
  border-radius: 8px;
  height: 100%;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
}

/* Status 카드 하단 패딩 줄이기 */
.status-card .q-card-section {
  padding-bottom: 0.25rem !important;
  /* 하단 패딩 더 줄이기 - All Status 버튼 아래 공간 최소화 */
  padding-top: 1rem !important;
  /* 상단 패딩 명시적으로 설정 - Control 카드와 동일하게 맞추기 */
}

.status-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  /* Control 카드와 동일한 구조: LED 그룹은 위에, 버튼은 아래에 */
  padding: 0.5rem 0 0.1rem 0;
  /* 하단 패딩 더 줄이기: 0.25rem → 0.1rem - All Status 버튼 아래 공간 최소화 */
}

/* Status LED 그룹 - Control 카드의 control-buttons와 동일한 구조 */
.status-leds-group {
  width: 100%;
  /* Control 카드의 control-buttons와 동일한 너비 */
  display: flex;
  flex-direction: column;
  /* Control 카드의 control-buttons와 동일한 flex 방향 */
  /* height: 100% 제거 - 자동 높이로 변경하여 버튼 위치 조정 가능하게 함 */
}

/* Status LED 항목들을 감싸는 영역 - flex-grow로 공간 차지 */
.status-content>.status-item,
.status-leds-group>.status-item {
  flex-shrink: 0;
  /* LED 항목들이 줄어들지 않도록 */
}

/* Status 카드의 모든 LED 항목 - 동일한 간격 적용 */
.status-leds-group .status-item {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  margin: 0 0 0.4rem 0 !important;
  /* 모든 LED 아래 동일한 간격 0.4rem */
}

/* 마지막 LED (Stow Pin) - 다른 LED와 동일한 간격 유지 */
.status-item-bottom {
  margin-bottom: 0.4rem !important;
  /* 다른 LED와 동일한 간격 */
}

/* 중간 LED 그룹 - 간격 제어 없이 LED들만 포함 */
.status-middle-group {
  display: flex;
  flex-direction: column;
  margin: 0 !important;
  /* 그룹 자체는 간격 제어하지 않음 */
  gap: 0 !important;
  /* gap 사용 안 함, LED 자체의 margin-bottom으로 제어 */
}

/* All Status 버튼 (Control Request 버튼과 같은 높이) */
.status-leds-group .q-btn {
  flex-shrink: 0;
  /* 버튼은 줄어들지 않도록 */
  margin-top: 0.1rem !important;
  /* Control 카드의 버튼과 동일한 margin-top 적용 */
}

/* All Status 버튼 높이 50% 감소 */
.all-status-button .q-btn {
  height: auto !important;
  min-height: 32px !important;
  /* 버튼 높이 줄이기: 30px → 32px (최소 높이만 설정) */
  font-size: 0.85rem !important;
  padding: 0.35rem 0.5rem !important;
  /* 패딩 30% 감소: 0.4rem → 0.35rem */
}

.status-messages {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.status-messages p {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--theme-text);
}

/* 차트 영역 스타일 - 성능 최적화 */
.axis-chart {
  height: 240px;
  min-height: 240px;
  width: 100%;
  margin: 0.3rem 0 0.05rem 0;
  /* 상단 마진: 0.25rem → 0.3rem, 하단 마진 50% 감소: 0.1rem → 0.05rem */
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--theme-card-background);
  /* 차트 배경색 통일 */
  border-radius: 4px;
  border: 1px solid var(--theme-border-light);
  /* 밝은 회색 테두리 */
  will-change: auto;
  /* GPU 가속으로 성능 최적화 */
  transform: translateZ(0);
  /* 하드웨어 가속 강제 활성화 */
}

.axis-data-row {
  display: flex;
  justify-content: space-between;
  margin-top: 0.1rem;
  margin-bottom: 0;
  padding-bottom: 0;
}

.axis-data-item {
  flex: 1;
  text-align: center;
  padding: 0.1rem 0.5rem;
  margin-bottom: 0;
}

/* q-item-label 마진 제거 */
.axis-data-item .q-item-label {
  margin: 0 !important;
  padding: 0 !important;
}

/* 모드 선택 섹션 - 밝은 회색 테두리 */
.mode-selection-section {
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
  border-radius: 8px;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
}

.mode-content-section {
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
  border-radius: 8px;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
  flex: 1;
  /* flex: 1을 다시 추가하여 하단까지 확장 */
  margin-bottom: -0.5rem !important;
  /* 음수 마진을 절반으로 줄여서 적절한 하단 공간 확보 */
  padding: 0 1rem 0 1rem;
  /* 하단 패딩을 완전히 제거하여 푸터와의 간격 최소화 */
  height: auto;
  /* 자동 높이로 설정하여 내용에 맞게 조정 */
  padding-bottom: 0;
  /* 하단 패딩을 명시적으로 0으로 설정 */
}

/* mode-selection-section 상단/하단 마진 추가 */
.mode-selection-section {
  margin-top: 0.5rem !important;
  /* 상단 마진 추가 */
  margin-bottom: 0.5rem !important;
  /* 하단 마진 추가 */
}

/* mode-selection-section 패딩 50% 감소 */
.mode-selection-section .q-card-section {
  padding: 0.5rem 1rem !important;
}

/* 모든 모드 컴포넌트가 동일한 하단 공간을 가지도록 통일 */
.mode-content-section .q-card__section {
  padding: 1rem !important;
  /* q-card-section 내부 패딩 강제 적용 */
}

/* 컴팩트 탭 스타일 */
.mode-selection-section {
  background-color: var(--theme-card-background);
  border-color: rgba(255, 255, 255, 0.08);
}

.mode-selection-wrapper {
  padding: 6px 12px !important;
}

.compact-tabs {
  height: 54px;
  min-height: 54px;
}

.compact-tabs .q-tabs__content {
  display: flex;
  gap: 6px;
  overflow: visible;
}

.compact-tabs .q-tab {
  padding: 8px 18px;
  min-height: 52px;
  border-radius: 9px;
  transition: all 0.18s ease;
}

.mode-tab {
  color: var(--theme-text-secondary);
  font-weight: 500;
  font-size: 0.9rem;
  letter-spacing: 0.03em;
  background-color: transparent;
  border: 1px solid transparent;
  position: relative;
  white-space: nowrap;
}

.mode-tab .q-tab__content {
  gap: 8px;
  align-items: center;
  flex-direction: row;
}

.mode-tab .q-tab__icon {
  font-size: 1.05rem;
  line-height: 1;
}

.mode-tab .q-tab__label {
  font-size: 0.87rem;
  font-weight: 600;
  text-transform: uppercase;
}

.mode-tab::before {
  content: '';
  position: absolute;
  inset: auto 8px -4px 8px;
  height: 2px;
  background: transparent;
  border-radius: 4px;
  transition: background 0.2s ease;
}

.mode-tab--active {
  color: var(--theme-text);
  background: rgba(25, 118, 210, 0.12);
  border-color: rgba(33, 150, 243, 0.4);
  box-shadow: inset 0 0 10px rgba(33, 150, 243, 0.15);
}

.mode-tab--active::before {
  background: var(--theme-primary);
}

.mode-tab:hover:not(.mode-tab--active) {
  color: var(--theme-text);
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
}

.compact-tabs .q-tab__indicator {
  display: none;
}

.mode-content-section .q-card-section {
  padding-top: 8px !important;
  padding-bottom: 10px !important;
}

@media (max-width: 1280px) {
  .compact-tabs .q-tabs__content {
    flex-wrap: wrap;
  }
}

/* Status LED 스타일 */
.status-item {
  display: flex;
  align-items: center;
}

.status-led-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-led {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}

/* LED 색상 */
.led-normal {
  background-color: var(--theme-led-normal);
  box-shadow: 0 0 8px var(--theme-led-normal), 0 0 16px var(--theme-led-normal);
}

.led-error {
  background-color: var(--theme-led-error);
  box-shadow: 0 0 12px var(--theme-led-error), 0 0 24px var(--theme-led-error);
}

.led-stow-active {
  background-color: var(--theme-led-stow-active);
  box-shadow: 0 0 8px var(--theme-led-stow-active), 0 0 16px var(--theme-led-stow-active);
}

.led-inactive {
  background-color: var(--theme-led-inactive);
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}

.status-label {
  font-size: 1rem;
  font-weight: 500;
  color: var(--theme-text);
}


/* Control Request와 All Status 버튼 높이 통일 */
.control-content .q-btn,
.all-status-button .q-btn {
  font-size: 0.85rem !important;
  /* 폰트 크기 30% 감소: 1rem → 0.85rem */
  padding: 0.35rem 0.5rem !important;
  /* 패딩 30% 감소: 12px 16px → 0.35rem 0.5rem */
  background-color: var(--theme-button-primary);
  color: white;
  font-weight: 600;
  min-height: 32px !important;
  /* 최소 높이 30% 감소: 48px → 32px */
  height: auto !important;
  /* 높이 자동 설정 */
}

/* 반응형 디자인 유지 */
@media (max-width: 1279px) {
  .axis-grid {
    grid-template-columns: minmax(0, 1.2fr) minmax(0, 1.2fr) minmax(0, 1.2fr) minmax(0, 0.8fr);
  }

  .status-card {
    grid-column: 1 / -1;
    margin-top: 1rem;
  }
}

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

@media (max-width: 767px) {
  .axis-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .control-container {
    grid-column: 1 / -1;
    flex-direction: column;
  }
}

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

/* Elevation 차트 특별 스타일 유지 */
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

/* 더 강력한 선택자로 차트 높이 줄이기 */
.q-card.azimuth-card .axis-chart,
.q-card.elevation-card .axis-chart,
.q-card.tilt-card .axis-chart {
  height: 200px !important;
  min-height: 200px !important;
  background-color: var(--theme-card-background) !important;
  border: none !important;
  margin: 0.3rem 0 0.05rem 0 !important;
  /* 상단 마진: 1rem → 0.3rem, 하단 마진 50% 감소: 0.1rem → 0.05rem */
}

/* 또는 더 구체적인 선택자 */
.axis-card.azimuth-card .axis-chart,
.axis-card.elevation-card .axis-chart,
.axis-card.tilt-card .axis-chart {
  height: 200px !important;
  min-height: 200px !important;
  background-color: var(--theme-card-background) !important;
  border: none !important;
  margin: 0.3rem 0 0.05rem 0 !important;
  /* 상단 마진: 1rem → 0.3rem, 하단 마진 50% 감소: 0.1rem → 0.05rem */
}
</style>
