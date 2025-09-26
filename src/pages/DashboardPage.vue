<template>
  <q-page class="dashboard-container q-pa-md">
    <!-- 상단 부분: 실시간 ICD 데이터 표시 (3축으로 구분) -->
    <q-card class="icd-data-section">
      <q-card-section>
        <!-- header-section 전체 제거 -->

        <div class="axis-grid">
          <!-- Azimuth 축 데이터 -->
          <q-card class="axis-card azimuth-card">
            <q-card-section style="padding: 0 !important;">
              <div class="text-subtitle1 text-weight-bold text-center"
                style="margin: 0 !important; padding: 0.1rem 0 1rem 0 !important;">Azimuth</div>

              <!-- Azimuth 차트 영역 추가 -->
              <div class="axis-chart" ref="azimuthChartRef"
                style="height: 200px !important; min-height: 200px !important;"></div>

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
            <q-card-section style="padding: 0 !important;">
              <div class="text-subtitle1 text-weight-bold text-center"
                style="margin: 0 !important; padding: 0.1rem 0 1rem 0 !important;">Elevation</div>

              <!-- Elevation 차트 영역 추가 -->
              <div class="axis-chart" ref="elevationChartRef"
                style="height: 200px !important; min-height: 200px !important;"></div>

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
            <q-card-section style="padding: 0 !important;">
              <div class="text-subtitle1 text-weight-bold text-center"
                style="margin: 0 !important; padding: 0.1rem 0 1rem 0 !important;">Tilt</div>

              <!-- Tilt 차트 영역 추가 -->
              <div class="axis-chart" ref="trainChartRef"
                style="height: 200px !important; min-height: 200px !important;"></div>

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
                    size="lg" />
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
                <!-- Emergency LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item q-mb-sm">
                  <div class="status-led-container">
                    <div class="status-led" :class="{
                      'led-error': errorEmergencyActive,
                      'led-normal': !errorEmergencyActive,
                    }"></div>
                    <span class="status-label">Emergency</span>
                  </div>
                </div>

                <!-- Positioner LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item q-mb-sm">
                  <div class="status-led-container">
                    <div class="status-led" :class="{
                      'led-error': errorPositionerActive,
                      'led-normal': !errorPositionerActive,
                    }"></div>
                    <span class="status-label">Positioner</span>
                  </div>
                </div>

                <!-- Feed LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item q-mb-sm">
                  <div class="status-led-container">
                    <div class="status-led" :class="{ 'led-error': errorFeedActive, 'led-normal': !errorFeedActive }">
                    </div>
                    <span class="status-label">Feed</span>
                  </div>
                </div>

                <!-- Protocol LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item q-mb-sm">
                  <div class="status-led-container">
                    <div class="status-led" :class="{
                      'led-error': errorProtocolActive,
                      'led-normal': !errorProtocolActive,
                    }"></div>
                    <span class="status-label">Protocol</span>
                  </div>
                </div>

                <!-- Power LED - TRUE면 빨간색, FALSE면 녹색 -->
                <div class="status-item q-mb-sm">
                  <div class="status-led-container">
                    <div class="status-led" :class="{ 'led-error': errorPowerActive, 'led-normal': !errorPowerActive }">
                    </div>
                    <span class="status-label">Power</span>
                  </div>
                </div>

                <!-- ✅ Stow LED - TRUE면 녹색, FALSE면 회색 -->
                <div class="status-item q-mb-sm">
                  <div class="status-led-container">
                    <div class="status-led" :class="{ 'led-stow-active': stowActive, 'led-inactive': !stowActive }">
                    </div>
                    <span class="status-label">Stow</span>
                  </div>
                </div>

                <!-- ✅ Stow Pin LED - TRUE면 녹색, FALSE면 회색 -->
                <div class="status-item q-mb-sm">
                  <div class="status-led-container">
                    <div class="status-led"
                      :class="{ 'led-stow-active': stowPinActive, 'led-inactive': !stowPinActive }"></div>
                    <span class="status-label">Stow Pin</span>
                  </div>
                </div>

                <!-- All Status 버튼 -->
                <div class="all-status-button q-mt-md">
                  <q-btn color="primary" label="All Status" size="sm" outline @click="handleAllStatus"
                    class="full-width" />
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
        <q-tabs v-model="currentMode" class="text-primary compact-tabs" active-color="primary" indicator-color="primary"
          align="left" narrow-indicator>
          <q-tab name="standby" label="Standby" @click="navigateToMode('standby')" />
          <q-tab name="step" label="Step" @click="navigateToMode('step')" />
          <q-tab name="slew" label="Slew" @click="navigateToMode('slew')" />
          <q-tab name="pedestal" label="Pedestal Position" @click="navigateToMode('pedestal')" />
          <q-tab name="ephemeris" label="Ephemeris Designation" @click="navigateToMode('ephemeris')" />
          <q-tab name="pass-schedule" label="Pass Schedule" @click="navigateToMode('pass-schedule')" />
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
//import AllStatus from '../components/modal/status/AllStatus.vue'
import { useTheme } from '../composables/useTheme'

const icdStore = useICDStore()
const router = useRouter()
const route = useRoute()

// 테마 관련 추가
const { initializeTheme } = useTheme()

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

  // 테마 초기화 추가
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

  // 1. 차트 업데이트 타이머 중지
  stopChartUpdates()

  // 2. 디버그 타이머 정리
  if (debugTimer) {
    clearInterval(debugTimer)
    debugTimer = null
  }

  // 3. 이벤트 리스너 제거
  window.removeEventListener('resize', () => { })

  // 4. icdStore 정리
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
          symbolSize: 12,
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
    // 초기 train 값 가져오기
    const elevation = getCurrentElevationActualValue.value // ✅ computed 값 사용
    const normalizedInitialElevation = elevation < 0 ? elevation + 360 : elevation % 360
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
              return `${elevation.toFixed(2)}°`
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
          data: [[1, normalizedInitialTrain]], // 초기값을 현재 train 값으로 설정
          zlevel: 2,
          label: {
            show: true,
            formatter: function () {
              return `${train.toFixed(2)}°`
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


/* 기존 dashboard-container 스타일 */
.dashboard-container {
  max-width: 1880px;
  margin: 0 auto;
  background-color: var(--theme-background);
  min-height: 100vh;
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

.axis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1.2fr) minmax(0, 1.2fr) minmax(0, 0.8fr) minmax(0, 0.8fr);
  gap: 1rem;
  margin-top: 1rem;
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
  padding: 0.25rem 1rem 0.5rem 1rem;
}

/* 각 축 카드의 상단 테두리만 색상 유지, 나머지는 밝은 회색 */
.azimuth-card {
  border-top: 3px solid var(--theme-azimuth-color);
  /* 주황색 상단만 */
  border-left: 1px solid var(--theme-border);
  /* 밝은 회색 */
  border-right: 1px solid var(--theme-border);
  /* 밝은 회색 */
  border-bottom: 1px solid var(--theme-border);
  /* 밝은 회색 */
  background-color: #15282f;
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
.q-card.azimuth-card .q-card-section {
  padding: 0 !important;
}

.azimuth-card {
  background-color: #15282f !important;
  border-top: 1px solid var(--theme-border) !important;
  /* 주황색 제거하고 일반 테두리로 변경 */
}

/* Elevation 카드 전용 스타일 */
.q-card.elevation-card .q-card-section {
  padding: 0 !important;
}

.elevation-card {
  background-color: #15282f !important;
  border-top: 1px solid var(--theme-border) !important;
  /* 파란색 제거하고 일반 테두리로 변경 */
}

/* Tilt 카드 전용 스타일 */
.q-card.tilt-card .q-card-section {
  padding: 0 !important;
}

.tilt-card {
  background-color: #15282f !important;
  border-top: 1px solid var(--theme-border) !important;
  /* 녹색 제거하고 일반 테두리로 변경 */
}

/* 모든 축 카드의 텍스트 높이 통일 */
.q-card.azimuth-card .q-card-section,
.q-card.elevation-card .q-card-section,
.q-card.tilt-card .q-card-section {
  padding: 0 !important;
}

.azimuth-card,
.elevation-card,
.tilt-card {
  background-color: #15282f !important;
  border-top: 1px solid var(--theme-border) !important;
}

/* Azimuth의 정확한 위치를 Elevation, Tilt에 정확히 적용 */
.azimuth-card .text-subtitle1,
.elevation-card .text-subtitle1,
.tilt-card .text-subtitle1 {
  margin: 0 !important;
  padding: 0.1rem 0 1rem 0 !important;
  /* Azimuth의 정확한 위치로 통일 */
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
  margin: 1rem 0 0.25rem 0 !important;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #15282f !important;
  border-radius: 4px;
  border: none !important;
}

/* Elevation은 기존 높이 유지 */
.elevation-card .axis-chart {
  height: 240px !important;
  min-height: 240px !important;
  width: 100%;
  margin: 1rem 0 0.25rem 0 !important;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #15282f !important;
  border-radius: 4px;
  border: none !important;
}

.axis-data-row {
  margin-top: auto;
  margin-bottom: 0;
  padding-bottom: 0;
}

.axis-card .text-subtitle1 {
  margin-bottom: 0.15rem;
  font-size: 1rem;
  color: var(--theme-text);
  font-weight: 600;
}

/* Emergency와 Control 컨테이너 */
.control-container {
  grid-column: span 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  height: 100%;
}

/* Emergency 카드 - 밝은 회색 테두리 */
.emergency-card {
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
  border-radius: 8px;
  flex: 1;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
}

.emergency-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.1rem 0;
}

/* Control 카드 - 밝은 회색 테두리 */
.control-card {
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
  border-top: 3px solid var(--theme-primary);
  /* 파란색 상단만 */
  border-radius: 8px;
  flex: 1;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
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

/* Status 카드 - 밝은 회색 테두리 */
.status-card {
  grid-column: span 1;
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
  /* 밝은 회색 테두리 */
  border-top: 3px solid var(--theme-primary);
  /* 파란색 상단만 */
  border-radius: 8px;
  height: 100%;
  box-shadow: 0 2px 4px var(--theme-shadow-light);
}

.status-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
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

/* 차트 영역 스타일 */
.axis-chart {
  height: 240px;
  min-height: 240px;
  width: 100%;
  margin: 0.25rem 0 0.25rem 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #15282f;
  /* 차트 배경색 통일 */
  border-radius: 4px;
  border: 1px solid var(--theme-border-light);
  /* 밝은 회색 테두리 */
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
}

/* 컴팩트 탭 스타일 */
.compact-tabs {
  height: 42px;
}

.compact-tabs .q-tab {
  padding: 0 12px;
  min-height: 42px;
  color: var(--theme-text-secondary);
}

.compact-tabs .q-tab--active {
  color: var(--theme-primary);
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

.all-status-button {
  margin-top: 1rem;
}

.all-status-button .q-btn {
  font-size: 0.9rem;
  padding: 8px 16px;
  background-color: var(--theme-button-primary);
  color: white;
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
  background-color: #15282f !important;
  border: none !important;
  margin: 1rem 0 0.25rem 0 !important;
}

/* 또는 더 구체적인 선택자 */
.axis-card.azimuth-card .axis-chart,
.axis-card.elevation-card .axis-chart,
.axis-card.tilt-card .axis-chart {
  height: 200px !important;
  min-height: 200px !important;
  background-color: #15282f !important;
  border: none !important;
  margin: 1rem 0 0.25rem 0 !important;
}
</style>
