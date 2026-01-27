<template>
  <div class="pass-schedule-mode">
    <!-- 1행: Offset Controls -->
    <div class="row q-col-gutter-md q-mb-sm offset-control-row">
      <div class="col-12">
        <OffsetControls
          :inputs="inputs"
          :outputs="outputs"
          :cal-time="formattedCalTime"
          @input-change="onInputChange"
          @increment="increment"
          @decrement="decrement"
          @reset="reset"
        />
      </div>
    </div>
    <!-- 2행: Main Content -->
    <div class="row q-col-gutter-md main-content-row"
      style="display: flex; flex-wrap: nowrap; align-items: stretch; margin-bottom: 0 !important; padding-bottom: 0 !important;">
      <!-- 1번 영역: 차트가 들어갈 네모난 칸 - 반응형 크기 조정 -->
      <div class="col-12 col-md-3 position-view-col">
        <q-card class="control-section position-view-card"
          style="min-height: 360px !important; height: 100% !important; display: flex !important; flex-direction: column !important;">
          <q-card-section class="position-view-section"
            style="min-height: 360px !important; height: 100% !important; flex: 1 !important; display: flex !important; flex-direction: column !important; padding-top: 16px !important; padding-bottom: 0px !important;">
            <div class="text-subtitle1 text-weight-bold text-primary position-view-title">Position View</div>
            <div class="chart-area" ref="chartRef"
              style="min-height: 340px !important; height: 100% !important; flex: 1 !important; padding-top: 0 !important; padding-bottom: 0 !important; margin-bottom: 0 !important;">
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- 2번 영역: Schedule Information -->
      <div class="col-12 col-md-3">
        <ScheduleInfoPanel
          :schedule="displaySchedule"
          :schedule-status="currentScheduleStatus"
          :time-remaining="timeRemaining"
          :tracking-state-info="icdStore.passScheduleTrackingStateInfo"
        />
      </div>

      <!-- 3번 영역: Schedule Control -->
      <div class="col-12 col-md-6 schedule-control-col">
        <q-card class="control-section">
          <q-card-section class="schedule-control-section">
            <!-- ✅ Schedule Control 헤더 -->
            <div class="schedule-header">
              <div class="text-subtitle1 text-weight-bold text-primary schedule-header-title">Schedule Control</div>
              <div class="schedule-header-right">
                <!-- 현재/다음 스케줄 상태 표시 -->
                <div v-if="currentDisplaySchedule" class="current-schedule-display">
                  <q-icon :name="currentDisplaySchedule.type === 'current' ? 'play_arrow' : 'schedule'"
                    :color="currentDisplaySchedule.type === 'current' ? 'positive' : 'primary'" size="sm" />
                  <span class="text-body2 q-ml-xs">
                    {{ currentDisplaySchedule.label }}: MstId {{ currentDisplaySchedule.mstId }}
                  </span>
                  <q-badge :color="currentDisplaySchedule.type === 'current' ? 'positive' : 'primary'"
                    :label="currentDisplaySchedule.type === 'current' ? '추적중' : '대기중'" class="q-ml-sm" />
                </div>
                <!-- 등록된 스케줄 정보 -->
                <div class="registered-schedule-info">
                  <span class="text-body2 text-primary">등록된 스케줄</span>
                  <span class="text-caption text-grey-5 q-ml-xs">{{ scheduleData.length }}개</span>
                </div>
              </div>
            </div>
            <!--
              <div class="debug-panel q-mb-md" v-if="true">
                <q-card flat bordered>
                  <q-card-section class="q-py-sm">
                    <div class="text-caption">
                      <strong>디버깅 정보:</strong>
                      Current: {{ icdStore.currentTrackingMstId }} |
                      Next: {{ icdStore.nextTrackingMstId }} |
                      스케줄 수: {{ sortedScheduleList.length }}
                    </div>
                    <div class="text-caption q-mt-xs">
                      인덱스들: {{sortedScheduleList.map(s => s.index).join(', ')}}
                    </div>
                    <div class="text-caption q-mt-xs">
                      <strong>Next=14 매칭:</strong>
                      {{ icdStore.nextTrackingMstId === 14 ? '✅' : '❌' }} |
                      <strong>테이블 키:</strong> {{
                       }}
                    </div>
                  </q-card-section>
                </q-card>
              </div>
 -->
            <!-- 스케줄 테이블 컴포넌트 -->
            <ScheduleTable
              :schedule-list="sortedScheduleList"
              :columns="scheduleColumns"
              :loading="loading"
              :highlight-info="tableHighlightInfo"
              @row-click="onTableRowClick"
            />
            <!-- 버튼 그룹 섹션 -->
            <div class="button-group">
              <div class="button-row">
                <q-btn color="info" label="TLE Upload" icon="upload_file" @click="handleTLEUpload"
                  class="q-mr-sm upload-btn" size="sm" />

                <q-btn color="primary" label="Select Schedule" icon="playlist_add_check" @click="selectScheduleData"
                  class="upload-btn" size="sm">
                  <q-tooltip>스케줄 목록을 불러와서 선택할 수 있습니다</q-tooltip>
                </q-btn>
              </div>

              <div class="control-button-row">
                <q-btn color="positive" icon="play_arrow" label="Start" @click="handleStartCommand" class="control-btn"
                  size="sm" />
                <q-btn color="negative" icon="stop" label="Stop" @click="handleStopCommand" class="control-btn"
                  size="sm" />
                <q-btn color="warning" icon="home" label="Stow" @click="handleStowCommand" class="control-btn"
                  size="sm" />
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// ✅ keep-alive의 include에서 사용할 컴포넌트 이름 정의
defineOptions({
  name: 'PassSchedulePage'
})

import { ref, onMounted, onUnmounted, computed, watch, onActivated, onDeactivated, nextTick } from 'vue'
import { useQuasar } from 'quasar'
import { usePassScheduleModeStore, type ScheduleItem } from '@/stores'
import { useICDStore } from '../../stores/icd/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import type { QTableProps } from 'quasar'
import { openModal } from '../../utils/windowUtils'
import { getCalTimeTimestamp } from '../../utils/times'
// 분리된 컴포넌트 및 composable import
import { ScheduleInfoPanel, ScheduleTable } from './passSchedule/components'
import { OffsetControls, useOffsetControls } from './shared'
import { useErrorHandler } from '@/composables/useErrorHandler'

const $q = useQuasar()
const { handleApiError } = useErrorHandler()
const passScheduleStore = usePassScheduleModeStore()
const icdStore = useICDStore()

// ✅ 공용 Offset Controls composable 사용 (3개 페이지에서 동기화)
const { inputs, outputs, formattedCalTime, onInputChange, increment, decrement, reset } = useOffsetControls()

// 차트 관련 변수
const chartRef = ref<HTMLElement | null>(null)
let updateTimer: number | null = null

// ECharts 매개변수 타입 정의
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

// PassSchedule 전용 차트 데이터와 상태
const currentPosition = ref({ azimuth: 0, elevation: 0 })
let passChart: ECharts | null = null

// 🆕 차트 초기화 상태 추적
const isChartInitialized = ref(false)

// ✅ 차트 리사이즈 핸들러를 외부 변수로 저장 (onUnmounted에서 제거하기 위해)
let chartResizeHandler: (() => void) | null = null

// 🆕 PassSchedule 전용 차트 업데이트 풀 (EphemerisDesignationPage와 동일한 최적화)
class PassChartUpdatePool {
  private positionData: [number, number][] = [[0, 0]]
  private trackingData: [number, number][] = []
  private predictedData: [number, number][] = []
  private updateOption: {
    series: Array<{ data?: [number, number][] }>
  }

  constructor() {
    this.updateOption = {
      series: [
        { data: this.positionData }, // 현재 위치
        { data: this.trackingData }, // 실시간 추적 경로
        { data: this.predictedData }, // 예측 경로
      ],
    }
  }

  // ✅ updateOption에 대한 public getter 메서드 추가
  getUpdateOption() {
    return this.updateOption
  }

  updatePosition(elevation: number, azimuth: number) {
    // 배열 존재 확인
    if (this.positionData.length > 0 && this.positionData[0]) {
      this.positionData[0][0] = elevation
      this.positionData[0][1] = azimuth
    } else {
      this.positionData = [[elevation, azimuth]]
      // 시리즈 데이터 참조 업데이트
      if (this.updateOption.series[0]) {
        this.updateOption.series[0].data = this.positionData
      }
    }
    return this.updateOption
  }

  updateTrackingPath(newPath: [number, number][]) {
    // ✅ 안전한 배열 업데이트 - 이전 데이터 완전 제거
    this.trackingData.length = 0
    if (Array.isArray(newPath) && newPath.length > 0) {
      this.trackingData.push(...newPath)
    }
    // ✅ 시리즈 데이터 참조 업데이트 (series[1]에 설정)
    if (this.updateOption.series[1]) {
      this.updateOption.series[1].data = this.trackingData
    }
    return this.updateOption
  }

  updatePredictedPath(newPath: [number, number][]) {
    // ✅ 안전한 배열 업데이트 - 이전 데이터 완전 제거
    this.predictedData.length = 0
    if (Array.isArray(newPath) && newPath.length > 0) {
      this.predictedData.push(...newPath)
    }
    // ✅ 시리즈 데이터 참조 업데이트 (series[2]에 설정)
    if (this.updateOption.series[2]) {
      this.updateOption.series[2].data = this.predictedData
    }
    return this.updateOption
  }
}

// 🆕 PassChartUpdatePool 인스턴스 생성
const passChartPool = new PassChartUpdatePool()

// ✅ 하드웨어 초기값 튀는 현상 방지: 추적 시작 시 초기 프레임 스킵
const INITIAL_FRAMES_TO_SKIP = 5  // 처음 5프레임 스킵 (하드웨어 초기값 안정화)
let trackingFrameCount = 0
let isTrackingInitPhase = false  // 추적 초기 단계 플래그

// 🔧 모든 computed를 먼저 정의
// ✅ PassSchedule 데이터 구조 리팩토링: 선택된 스케줄만 표시 (selectedScheduleList 사용)
const scheduleData = computed(() => {
  try {
    // ✅ selectedScheduleList 사용 (선택된 스케줄만 표시)
    const data = passScheduleStore.selectedScheduleList || []
    console.log('🔍 PassSchedulePage scheduleData (선택된 스케줄):', data.length, '개')
    // ✅ 디버깅: 첫 번째 항목의 mstId 확인
    if (data.length > 0) {
      console.log('🔍 첫 번째 항목 mstId 확인:', {
        mstId: data[0].mstId,
        detailId: data[0].detailId,
        no: data[0].no,
        satelliteName: data[0].satelliteName,
        allKeys: Object.keys(data[0])  // ✅ 모든 키 확인
      })
    }
    return data
  } catch (error) {
    console.error('❌ scheduleData computed 에러:', error)
    return []
  }
})

const sortedScheduleList = computed(() => {
  try {
    const schedules = scheduleData.value
    if (!schedules || !Array.isArray(schedules)) {
      return []
    }
    return schedules
      .slice()
      .sort((a, b) => {
        const timeA = new Date(a.startTime).getTime()
        const timeB = new Date(b.startTime).getTime()
        return timeA - timeB
      })
  } catch (error) {
    console.error('❌ sortedScheduleList computed 에러:', error)
    return []
  }
})

// 🔧 반응성 트리거
const reactivityTrigger = ref(0)

// ✅ appendData를 위한 이전 경로 길이 추적 (watch보다 먼저 선언)
const lastTrackingPathLength = ref(0)
const lastPredictedPathLength = ref(0)

// 🆕 Store 값 변경 감지
// ✅ 스케줄 전환 시 경로 초기화 및 신규 스케줄 이론치 경로 로드 로직
watch([() => icdStore.currentTrackingMstId, () => icdStore.currentTrackingDetailId], ([newMstId, newDetailId], [oldMstId, oldDetailId]) => {
  console.log(`🔄 currentTrackingMstId/detailId 변경 감지: ${oldMstId}/${oldDetailId} → ${newMstId}/${newDetailId}`)
  reactivityTrigger.value++

  // ✅ 하이라이트 즉시 업데이트
  void nextTick(() => {
    applyRowColors()
  })

  // 스케줄이 변경된 경우 (이전 스케줄 완료, 다음 스케줄 시작)
  if (oldMstId !== null && newMstId !== null && oldMstId !== newMstId) {
    console.log(`🔄 스케줄 전환 감지: ${oldMstId} → ${newMstId}`)

    // ✅ 1. 이론치 경로와 실제 경로 모두 초기화
    passScheduleStore.clearTrackingPaths()
    // ✅ 2. 차트 풀의 경로도 초기화
    passChartPool.updateTrackingPath([])
    passChartPool.updatePredictedPath([])
    // ✅ 3. 경로 길이 추적 변수 초기화
    lastTrackingPathLength.value = 0
    lastPredictedPathLength.value = 0
    console.log('✅ 스케줄 전환 - 모든 경로 초기화 완료')

    // ✅ 4. 신규 스케줄의 이론치 경로 자동 로드
    void nextTick(async () => {
      try {
        // ✅ mstId와 detailId 기준으로 매칭 (detailId는 WebSocket에서 받아올 수 있지만, 일단 mstId만으로 찾기)
        const newSchedule = sortedScheduleList.value.find(s => Number(s.mstId) === Number(newMstId))
        if (newSchedule) {
          console.log('🚀 신규 스케줄의 이론치 경로 로드 시작:', newSchedule.satelliteName, newSchedule.mstId, newSchedule.detailId)

          // ✅ mstId와 detailId 사용 (satelliteId 불필요)
          const mstId = newSchedule.mstId
          const detailId = newSchedule.detailId

          if (!mstId || detailId == null) {
            console.warn('⚠️ MstId 또는 DetailId가 없음:', {
              mstId: newSchedule.mstId,
              detailId: newSchedule.detailId,
              no: newSchedule.no,
              satelliteName: newSchedule.satelliteName
            })
            return
          }

          // ✅ 스케줄의 keyhole 여부에 따라 DataType 결정
          const isKeyhole = newSchedule.isKeyhole || newSchedule.IsKeyhole || false
          const dataType = isKeyhole ? 'keyhole_optimized_final_transformed' : 'final_transformed'

          const success = await passScheduleStore.loadTrackingDetailData(
            mstId,
            detailId,
            dataType
          )

          if (success) {
            console.log('✅ 신규 스케줄의 이론치 경로 로드 완료')
            // ✅ 차트 업데이트
            if (passChart && !passChart.isDisposed()) {
              updateChart()
            }
          } else {
            console.warn('⚠️ 신규 스케줄의 이론치 경로 로드 실패')
          }
        } else {
          console.warn('⚠️ 신규 스케줄을 찾을 수 없음:', newMstId)
        }
      } catch (error) {
        console.error('❌ 신규 스케줄 이론치 경로 로드 중 오류:', error)
      }
    })

  } else if (oldMstId === null && newMstId !== null) {
    // 추적 시작 시 - 경로 초기화는 passScheduleTrackingState watch에서 처리
    console.log('🚀 추적 시작 - passScheduleTrackingState watch에서 경로 초기화 예정')
    // ✅ clearTrackingPaths() 제거 - TRACKING 전환 시 fallback 체인으로 초기화
    // ✅ 차트 풀의 경로만 초기화
    passChartPool.updateTrackingPath([])
    passChartPool.updatePredictedPath([])
    // ✅ 경로 길이 추적 변수 초기화
    lastTrackingPathLength.value = 0
    lastPredictedPathLength.value = 0

    // ✅ 신규 스케줄의 이론치 경로 자동 로드
    void nextTick(async () => {
      try {
        // ✅ mstId와 detailId 기준으로 매칭 (detailId는 WebSocket에서 받아올 수 있지만, 일단 mstId만으로 찾기)
        const newSchedule = sortedScheduleList.value.find(s => Number(s.mstId) === Number(newMstId))
        if (newSchedule) {
          console.log('🚀 추적 시작 - 신규 스케줄의 이론치 경로 로드 시작:', newSchedule.satelliteName, newSchedule.mstId, newSchedule.detailId)

          // ✅ mstId와 detailId 사용 (satelliteId 불필요)
          const mstId = newSchedule.mstId
          const detailId = newSchedule.detailId

          if (!mstId || detailId == null) {
            console.warn('⚠️ MstId 또는 DetailId가 없음:', {
              mstId: newSchedule.mstId,
              detailId: newSchedule.detailId,
              no: newSchedule.no,
              satelliteName: newSchedule.satelliteName
            })
            return
          }

          // ✅ 스케줄의 keyhole 여부에 따라 DataType 결정
          const isKeyhole = newSchedule.isKeyhole || newSchedule.IsKeyhole || false
          const dataType = isKeyhole ? 'keyhole_optimized_final_transformed' : 'final_transformed'

          const success = await passScheduleStore.loadTrackingDetailData(
            mstId,
            detailId,
            dataType
          )

          if (success) {
            console.log('✅ 추적 시작 - 신규 스케줄의 이론치 경로 로드 완료')
            // ✅ 차트 업데이트
            if (passChart && !passChart.isDisposed()) {
              updateChart()
            }
          } else {
            console.warn('⚠️ 추적 시작 - 신규 스케줄의 이론치 경로 로드 실패')
          }
        } else {
          console.warn('⚠️ 추적 시작 - 신규 스케줄을 찾을 수 없음:', newMstId)
        }
      } catch (error) {
        console.error('❌ 추적 시작 - 신규 스케줄 이론치 경로 로드 중 오류:', error)
      }
    })

  } else if (oldMstId !== null && newMstId === null) {
    // ✅ 추적 완료 시 경로는 유지 (초기화하지 않음)
    console.log('🛑 추적 완료 - 경로 유지 (초기화하지 않음)')

    // ✅ 이론치 경로만 초기화 (실제 이동 경로는 유지)
    passChartPool.updatePredictedPath([])
    lastPredictedPathLength.value = 0

    // ✅ 실제 이동 경로는 유지하되 차트만 업데이트
    if (passChart && !passChart.isDisposed()) {
      updateChart()
    }
  }
}, { immediate: true })

// ✅ PassSchedule 추적 상태 변경 감지 (Ephemeris 패턴 적용 - (0,0) 점프 방지)
watch(() => icdStore.passScheduleTrackingState, (newState, oldState) => {
  console.log('🔄 PassSchedule 추적 상태 변경:', oldState, '→', newState)

  // ✅ TRACKING으로 전환될 때만 경로 초기화 (fallback 체인 적용)
  if (newState === 'TRACKING' && oldState !== 'TRACKING') {
    // ✅ 하드웨어 초기값 튀는 현상 방지: 프레임 카운터 리셋
    trackingFrameCount = 0
    isTrackingInitPhase = true
    console.log('🚀 PassSchedule 추적 시작 - 초기 프레임 스킵 활성화 (5프레임)')

    // ✅ FIX: fallback 체인 - (0,0) 점프 방지
    const normalAz = parseFloat(icdStore.azimuthAngle)
    const normalEl = parseFloat(icdStore.elevationAngle)
    const trackingActualAz = parseFloat(icdStore.trackingActualAzimuthAngle)
    const trackingActualEl = parseFloat(icdStore.trackingActualElevationAngle)
    const trackingCmdAz = parseFloat(icdStore.trackingCMDAzimuthAngle)
    const trackingCmdEl = parseFloat(icdStore.trackingCMDElevationAngle)

    // ✅ 유효한 값 판별 함수 (0이 아니고 NaN이 아닌 값)
    const isValidAngle = (val: number) => !isNaN(val) && val !== 0

    // ✅ Azimuth fallback 체인: actualAngle > trackingActual > trackingCMD
    let currentAzimuth = 0
    if (isValidAngle(normalAz)) {
      currentAzimuth = normalAz
    } else if (isValidAngle(trackingActualAz)) {
      currentAzimuth = trackingActualAz
    } else if (isValidAngle(trackingCmdAz)) {
      currentAzimuth = trackingCmdAz
    }

    // ✅ Elevation fallback 체인
    let currentElevation = 0
    if (isValidAngle(normalEl)) {
      currentElevation = normalEl
    } else if (isValidAngle(trackingActualEl)) {
      currentElevation = trackingActualEl
    } else if (isValidAngle(trackingCmdEl)) {
      currentElevation = trackingCmdEl
    }

    // ✅ (0,0)이 아닌 경우에만 경로 초기화
    if (currentAzimuth !== 0 || currentElevation !== 0) {
      passScheduleStore.clearTrackingPathsWithPosition(currentAzimuth, currentElevation)
      console.log('🧹 PassSchedule 추적 시작 - 경로 초기화 완료:', {
        azimuth: currentAzimuth,
        elevation: currentElevation,
      })
    } else {
      console.warn('⚠️ PassSchedule 추적 시작 - 유효한 초기 위치 없음, 빈 경로로 시작')
      passScheduleStore.clearTrackingPathsWithPosition(0, 0)
    }
  }

  // ✅ IDLE 전환 시 경로 초기화 (추적 종료)
  if (newState === 'IDLE' && oldState !== null && oldState !== 'IDLE') {
    console.log('🧹 PassSchedule IDLE 상태 전환 - 경로 유지')
  }
})

watch([() => icdStore.nextTrackingMstId, () => icdStore.nextTrackingDetailId], ([newMstId, newDetailId], [oldMstId, oldDetailId]) => {
  console.log(`🔄 nextTrackingMstId/detailId 변경 감지: ${oldMstId}/${oldDetailId} → ${newMstId}/${newDetailId}`)
  reactivityTrigger.value++

  // ✅ 하이라이트 즉시 업데이트
  void nextTick(() => {
    applyRowColors()
  })

  // ✅ currentTrackingMstId가 null이고 nextTrackingMstId가 설정된 경우 (대기 중 상태)
  // 다음 스케줄의 예측 경로를 미리 로드
  if (icdStore.currentTrackingMstId === null && newMstId !== null && newMstId !== oldMstId) {
    void nextTick(async () => {
      try {
        const nextSchedule = sortedScheduleList.value.find(s => Number(s.mstId) === Number(newMstId))
        if (nextSchedule) {
          console.log('🔮 대기 중 - 다음 스케줄 예측 경로 로드 시작:', nextSchedule.satelliteName, nextSchedule.mstId, nextSchedule.detailId)

          const mstId = nextSchedule.mstId
          const detailId = nextSchedule.detailId

          if (!mstId || detailId == null) {
            console.warn('⚠️ MstId 또는 DetailId가 없음:', {
              mstId: nextSchedule.mstId,
              detailId: nextSchedule.detailId,
              satelliteName: nextSchedule.satelliteName
            })
            return
          }

          // ✅ 스케줄의 keyhole 여부에 따라 DataType 결정
          const isKeyhole = nextSchedule.isKeyhole || nextSchedule.IsKeyhole || false
          const dataType = isKeyhole ? 'keyhole_optimized_final_transformed' : 'final_transformed'

          const success = await passScheduleStore.loadTrackingDetailData(
            mstId,
            detailId,
            dataType
          )

          if (success) {
            console.log('✅ 대기 중 - 다음 스케줄 예측 경로 로드 완료')
            // ✅ 차트 업데이트
            if (passChart && !passChart.isDisposed()) {
              updateChart()
            }
          } else {
            console.warn('⚠️ 대기 중 - 다음 스케줄 예측 경로 로드 실패')
          }
        } else {
          console.warn('⚠️ 대기 중 - 다음 스케줄을 찾을 수 없음:', newMstId)
        }
      } catch (error) {
        console.error('❌ 대기 중 - 다음 스케줄 예측 경로 로드 중 오류:', error)
      }
    })
  }
}, { immediate: true })

const highlightedRows = computed(() => {
  try {
    // 강제 반응성 트리거 (값을 읽어서 의존성 생성)
    void reactivityTrigger.value // ✅ 의존성 생성용 (사용하지 않지만 반응성 유지)

    const current = icdStore.currentTrackingMstId
    const currentDetailId = icdStore.currentTrackingDetailId // ✅ detailId 추가
    const next = icdStore.nextTrackingMstId
    const nextDetailId = icdStore.nextTrackingDetailId // ✅ detailId 추가

    // ✅ 디버깅 로그 제거 (computed는 순수 함수여야 함)

    return { current, currentDetailId, next, nextDetailId }
  } catch (error) {
    console.error('❌ highlightedRows computed 에러:', error)
    return { current: null, currentDetailId: null, next: null, nextDetailId: null }
  }
})

// ScheduleTable 컴포넌트용 하이라이트 정보
const tableHighlightInfo = computed(() => ({
  currentMstId: highlightedRows.value.current,
  currentDetailId: highlightedRows.value.currentDetailId,
  nextMstId: highlightedRows.value.next,
  nextDetailId: highlightedRows.value.nextDetailId
}))

const currentDisplaySchedule = computed(() => {
  try {
    if (icdStore.currentTrackingMstId !== null) {
      return {
        mstId: icdStore.currentTrackingMstId,
        type: 'current',
        label: '현재 추적 중'
      }
    }
    if (icdStore.nextTrackingMstId !== null) {
      return {
        mstId: icdStore.nextTrackingMstId,
        type: 'next',
        label: '다음 예정'
      }
    }
    return null
  } catch (error) {
    console.error('❌ currentDisplaySchedule computed 에러:', error)
    return null
  }
})

// NOTE: getRowStyleDirect, getRowClass 함수는 ScheduleTable 컴포넌트로 이동됨


// 🔧 DOM 직접 조작으로 색상 적용
// ✅ 이전 상태 저장 (값 변경 시에만 실행하기 위함)
let lastAppliedColorState = {
  current: null as number | null,
  currentDetailId: null as number | null,
  next: null as number | null,
  nextDetailId: null as number | null
}

const applyRowColors = () => {
  try {
    const current = icdStore.currentTrackingMstId
    const currentDetailId = icdStore.currentTrackingDetailId
    const next = icdStore.nextTrackingMstId
    const nextDetailId = icdStore.nextTrackingDetailId

    // ✅ 값이 변경되지 않았으면 스킵
    if (
      lastAppliedColorState.current === current &&
      lastAppliedColorState.currentDetailId === currentDetailId &&
      lastAppliedColorState.next === next &&
      lastAppliedColorState.nextDetailId === nextDetailId
    ) {
      return // 변경 없음 - 스킵
    }

    // ✅ 값이 변경되었을 때만 로그 출력
    console.log('🎨 스케줄 하이라이트 변경:', {
      이전: { ...lastAppliedColorState },
      현재: { current, currentDetailId, next, nextDetailId }
    })

    // ✅ 상태 저장
    lastAppliedColorState = { current, currentDetailId, next, nextDetailId }

    setTimeout(() => {
      const rows = document.querySelectorAll('.schedule-table tbody tr')

      // ✅ sortedScheduleList를 사용하여 정확한 매칭
      const schedules = sortedScheduleList.value

      rows.forEach((row, rowIndex) => {
        const htmlRow = row as HTMLElement
        const schedule = schedules[rowIndex]

        if (!schedule) return

        // ✅ FIX: fallback 제거 - mstId는 필수
        const scheduleMstId = schedule.mstId
        const scheduleDetailId = schedule.detailId ?? 0

        // mstId가 없으면 하이라이트 불가
        if (scheduleMstId === null || scheduleMstId === undefined) {
          return
        }

        // ✅ FIX: detailId 매칭 로직 수정 - detailId가 null이면 mstId만으로 매칭
        const currentMatch = current !== null &&
          Number(scheduleMstId) === Number(current) &&
          (currentDetailId === null || Number(scheduleDetailId) === Number(currentDetailId))
        const nextMatch = next !== null &&
          Number(scheduleMstId) === Number(next) &&
          (nextDetailId === null || Number(scheduleDetailId) === Number(nextDetailId))

        // 기존 스타일 제거
        htmlRow.classList.remove('highlight-current-schedule', 'highlight-next-schedule')
        htmlRow.style.removeProperty('background-color')
        htmlRow.style.removeProperty('border-left')

        const cells = row.querySelectorAll('td')
        cells.forEach(cell => {
          const htmlCell = cell as HTMLElement
          htmlCell.style.removeProperty('background-color')
          htmlCell.style.removeProperty('color')
          htmlCell.style.removeProperty('font-weight')
        })

        // ✅ 매칭 확인 및 색상 적용 (mstId와 detailId 기준)
        let shouldHighlight = false
        let bgColor = ''
        let borderColor = ''
        let textColor = ''

        if (current !== null && currentMatch) {
          // 현재 스케줄 - 녹색
          shouldHighlight = true
          bgColor = '#c8e6c9'
          borderColor = '#4caf50'
          textColor = '#2e7d32'
        } else if (next !== null && nextMatch) {
          // 다음 스케줄 - 파란색
          shouldHighlight = true
          bgColor = '#e3f2fd'
          borderColor = '#2196f3'
          textColor = '#1565c0'
        }

        if (shouldHighlight) {
          // 행 전체 스타일 적용
          htmlRow.style.setProperty('background-color', bgColor, 'important')
          htmlRow.style.setProperty('border-left', `4px solid ${borderColor}`, 'important')

          // 모든 셀에 스타일 적용
          cells.forEach(cell => {
            const htmlCell = cell as HTMLElement
            htmlCell.style.setProperty('background-color', bgColor, 'important')
            htmlCell.style.setProperty('color', textColor, 'important')
            htmlCell.style.setProperty('font-weight', '500', 'important')
          })
        }
      })
    }, 100)

  } catch (error) {
    console.error('❌ applyRowColors 에러:', error)
  }
}
/*
// 🔧 현재 Store 값으로 getRowClass 테스트
const testStoreValues = () => {
  try {
    console.log('🧪 DOM 직접 조작 테스트 시작')
    applyRowColors()
  } catch (error) {
    console.error('❌ testStoreValues 에러:', error)
  }
}

// 🔧 DOM 클래스 확인 함수
const checkDOMClasses = () => {
  try {
    console.log('🔍 DOM 클래스 확인 시작')

    setTimeout(() => {
      const rows = document.querySelectorAll('.schedule-table tbody tr')
      console.log(`총 ${rows.length}개 행 발견`)

      rows.forEach((row, idx) => {
        const indexCell = row.querySelector('td:nth-child(2)') // index 컬럼
        const indexValue = indexCell?.textContent?.trim()
        const rowClasses = row.className
        const hasHighlight = rowClasses.includes('highlight-current') || rowClasses.includes('highlight-next')

        console.log(`행 ${idx + 1}: index=${indexValue}, classes="${rowClasses}", 하이라이트=${hasHighlight}`)

        // index 14인 행 특별 확인
        if (indexValue === '14') {
          console.log('🎯 INDEX 14 행 상세 분석:')
          console.log(`  - DOM 클래스: "${rowClasses}"`)
          console.log(`  - 배경색: ${getComputedStyle(row).backgroundColor}`)
          console.log(`  - border-left: ${getComputedStyle(row).borderLeft}`)

          // 수동으로 클래스 추가 테스트
          row.classList.add('highlight-current-schedule')
          console.log(`  - 클래스 추가 후: "${row.className}"`)
        }
      })
    }, 100)

  } catch (error) {
    console.error('❌ checkDOMClasses 에러:', error)
  }
}



// 🔧 실제 매칭 테스트 함수 추가
const realMatchTest = () => {
  try {
    console.log('🔍 실제 매칭 상황 분석')

    const scheduleList = sortedScheduleList.value
    const currentMstId = icdStore.currentTrackingMstId
    const nextMstId = icdStore.nextTrackingMstId

    console.log('📊 Store 상태:', { currentMstId, nextMstId })

    if (scheduleList && scheduleList.length > 0) {
      console.log('📋 모든 스케줄 분석:')
      scheduleList.forEach((schedule, idx) => {
        const isCurrentMatch = currentMstId !== null &&
          (schedule.index === currentMstId || schedule.no === currentMstId)
        const isNextMatch = nextMstId !== null &&
          (schedule.index === nextMstId || schedule.no === nextMstId)

        console.log(`  ${idx + 1}. ${schedule.satelliteName}`)
        console.log(`     no: ${schedule.no}, index: ${schedule.index}`)
        console.log(`     Current 매칭: ${isCurrentMatch} (${currentMstId})`)
        console.log(`     Next 매칭: ${isNextMatch} (${nextMstId})`)

        if (isCurrentMatch || isNextMatch) {
          const cssClass = getRowClass({ row: schedule })
          console.log(`     ✅ 적용될 CSS 클래스: ${cssClass || '없음'}`)
        }
        console.log('     ---')
      })
    }
  } catch (error) {
    console.error('❌ realMatchTest 에러:', error)
  }
}
// 🔧 테이블 강제 업데이트를 위한 키
const tableKey = ref(0)
// 🔧 watch들을 모든 computed 정의 후에 배치
// 🔧 강제 리렌더링 함수
const forceTableUpdate = () => {
  tableKey.value++
  console.log('🔄 테이블 강제 업데이트:', tableKey.value)
} */

// ✅ 중복 Watch 삭제됨 (Watch #1, #2가 이미 동일한 값을 감시하고 있음)
// - Watch #1 (Line 528): currentTrackingMstId, currentTrackingDetailId
// - Watch #2 (Line 675): nextTrackingMstId, nextTrackingDetailId
// - 이 Watch는 위 두 개와 완전히 중복 + deep: true로 인해 무한 루프 발생

// 🆕 Store 데이터 변경 감지 개선
watch(
  () => passScheduleStore.selectedScheduleList,
  (newData, oldData) => {
    try {
      console.log('👀 Store 변경 감지 - 새 데이터:', {
        newCount: newData?.length || 0,
        oldCount: oldData?.length || 0,
        hasData: newData && newData.length > 0
      })

      // 🆕 데이터가 변경되면 차트 업데이트 강제 실행
      if (newData && newData.length > 0) {
        setTimeout(() => {
          if (passChart && !passChart.isDisposed()) {
            updateChart()
            console.log('✅ 데이터 변경으로 차트 업데이트 실행')
          }
        }, 200)
      }
    } catch (error) {
      console.error('❌ passScheduleStore watch 에러:', error)
    }
  },
  { immediate: true, deep: true }
)

// ✅ 차트 데이터 복원 함수 (EphemerisDesignationPage.vue와 유사)
const restoreChartData = () => {
  try {
    // ✅ 안전한 null 체크
    if (!passChart || passChart.isDisposed() || !chartRef.value) {
      console.warn('⚠️ 차트가 초기화되지 않아 데이터 복원을 건너뜁니다')
      return
    }

    // ✅ Store에서 데이터 확인
    const actualPath = passScheduleStore.actualTrackingPath
    const predictedPath = passScheduleStore.predictedTrackingPath
    const hasActualPath = actualPath && actualPath.length > 0
    const hasPredictedPath = predictedPath && predictedPath.length > 0

    // ✅ 데이터가 없으면 복원하지 않음 (자동 로드 제거 - 에러 방지)
    if (!hasActualPath && !hasPredictedPath) {
      console.log('⚠️ 복원할 경로 데이터가 없습니다')
      return
    }

    // ✅ 실시간 추적 경로 복원
    if (hasActualPath) {
      passChartPool.updateTrackingPath(actualPath as [number, number][])
      console.log('✅ 실시간 추적 경로 복원:', actualPath.length, '개 포인트')
    }

    // ✅ 예측 경로 복원
    if (hasPredictedPath) {
      passChartPool.updatePredictedPath(predictedPath as [number, number][])
      console.log('✅ 예측 경로 복원:', predictedPath.length, '개 포인트')
    }

    // ✅ 차트 재초기화 없이 데이터만 업데이트 (안전한 체크 추가)
    if (passChart && !passChart.isDisposed()) {
      const updateOption = passChartPool.getUpdateOption()
      passChart.setOption(updateOption, false, true) // ✅ notMerge: false, lazyUpdate: true
      console.log('✅ 차트 데이터 복원 완료 (재초기화 없음)')
    }
  } catch (error) {
    console.error('❌ 차트 데이터 복원 중 오류:', error)
  }
}

// 🆕 컴포넌트 활성화 시 데이터 복원
const handleActivated = () => {
  try {
    console.log('🔄 PassSchedulePage 활성화됨')

    // ✅ 차트가 이미 존재하고 유효하면 재초기화하지 않음
    if (!passChart || passChart.isDisposed() || !chartRef.value) {
      setTimeout(() => {
        // ✅ 컴포넌트가 여전히 마운트되어 있는지 확인
        if (!chartRef.value) {
          console.warn('⚠️ 차트 컨테이너가 없어 초기화를 건너뜁니다')
          return
        }

        try {
          initChart()
          console.log('✅ 차트 재초기화 완료')

          // ✅ 차트 초기화 후 데이터 복원
          void nextTick(() => {
            if (passChart && !passChart.isDisposed() && chartRef.value) {
              restoreChartData()
            }
          })
        } catch (error) {
          console.error('❌ 차트 초기화 중 오류:', error)
        }
      }, 100)
    } else {
      // ✅ keep-alive로 인해 차트 인스턴스가 그대로 유지됨
      // 차트 데이터도 그대로 유지되므로 restoreChartData 호출 불필요
      console.log('✅ 차트가 이미 존재함 - 그대로 유지 (keep-alive)')
    }

    // 🆕 타이머 재시작
    if (!updateTimer) {
      // ✅ 타이머를 100ms로 변경하여 백엔드 모니터링 주기(100ms)와 일치
      updateTimer = window.setInterval(() => {
        try {
          updateChart()
          // ✅ 하이라이트도 주기적으로 업데이트 (실시간 반영)
          applyRowColors()
        } catch (error) {
          console.error('❌ 차트 업데이트 타이머 오류:', error)
        }
      }, 100)
      console.log('✅ 차트 업데이트 타이머 재시작 (100ms)')
    }

    // 🆕 DOM 하이라이트 강제 적용
    setTimeout(() => {
      try {
        applyRowColors()
      } catch (error) {
        console.error('❌ DOM 하이라이트 적용 중 오류:', error)
      }
    }, 200)
  } catch (error) {
    console.error('❌ handleActivated 중 오류:', error)
  }
}

// ✅ 컴포넌트 비활성화 시 타이머만 정리 (keep-alive로 차트와 데이터는 그대로 유지됨)
const handleDeactivated = () => {
  console.log('🔄 PassSchedulePage 비활성화됨 (keep-alive)')

  // ✅ 타이머만 정리 (차트와 추적 경로는 keep-alive로 그대로 유지)
  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
    console.log('✅ 차트 업데이트 타이머 정리됨')
  }

  // ✅ 브라우저 새로고침 대비용 localStorage 저장 (페이지 간 이동에는 불필요하지만 안전을 위해 유지)
  if (saveTimeout) {
    clearTimeout(saveTimeout)
    saveTimeout = null
  }
  passScheduleStore.saveToLocalStorage()
}

// ✅ localStorage 자동 저장을 위한 watch 설정
watch(
  [
    () => passScheduleStore.predictedTrackingPath,
    () => passScheduleStore.actualTrackingPath,
    () => passScheduleStore.selectedSchedule,
    () => passScheduleStore.selectedScheduleList,
  ],
  () => {
    // ✅ 디바운스 처리 (500ms)
    if (saveTimeout) {
      clearTimeout(saveTimeout)
    }
    saveTimeout = window.setTimeout(() => {
      passScheduleStore.saveToLocalStorage()
    }, 500)
  },
  { deep: true }
)

// ✅ 저장 타이머 변수
let saveTimeout: number | null = null

// 🆕 Vue 생명주기 훅 등록
onActivated(handleActivated)
onDeactivated(handleDeactivated)

const selectedSchedule = ref<ScheduleItem | null>(null)

// 🔧 current/next 기준으로 자동 선택된 스케줄
const autoSelectedSchedule = computed(() => {
  try {
    const current = icdStore.currentTrackingMstId
    const currentDetailId = icdStore.currentTrackingDetailId // ✅ detailId 추가
    const next = icdStore.nextTrackingMstId
    const nextDetailId = icdStore.nextTrackingDetailId // ✅ detailId 추가
    const schedules = sortedScheduleList.value

    // 1순위: current 스케줄 찾기
    if (current !== null) {
      // ✅ mstId와 detailId 기준으로 매칭 (detailId가 있으면 함께 비교, 없으면 mstId만으로 매칭)
      const currentSchedule = schedules.find(s => {
        const mstIdMatch = Number(s.mstId) === Number(current)
        // detailId가 둘 다 있으면 함께 비교, 아니면 mstId만으로 매칭
        const detailIdMatch = currentDetailId === null || s.detailId === null ||
                              Number(s.detailId) === Number(currentDetailId)
        return mstIdMatch && detailIdMatch
      })
      if (currentSchedule) {
        console.log('🎯 current 기준 자동 선택:', currentSchedule.satelliteName, currentSchedule.mstId, currentSchedule.detailId)
        return currentSchedule
      }
    }

    // 2순위: next 스케줄 찾기 (current가 없을 때)
    if (next !== null) {
      // ✅ mstId와 detailId 기준으로 매칭 (detailId가 있으면 함께 비교, 없으면 mstId만으로 매칭)
      const nextSchedule = schedules.find(s => {
        const mstIdMatch = Number(s.mstId) === Number(next)
        // detailId가 둘 다 있으면 함께 비교, 아니면 mstId만으로 매칭
        const detailIdMatch = nextDetailId === null || s.detailId === null ||
                              Number(s.detailId) === Number(nextDetailId)
        return mstIdMatch && detailIdMatch
      })
      if (nextSchedule) {
        console.log('🎯 next 기준 자동 선택:', nextSchedule.satelliteName, nextSchedule.mstId, nextSchedule.detailId)
        return nextSchedule
      }
    }

    return null
  } catch (error) {
    console.error('❌ autoSelectedSchedule computed 에러:', error)
    return null
  }
})

// 🔧 최종 표시할 스케줄 (자동 선택 우선, 수동 선택 차순위)
const displaySchedule = computed(() => {
  return autoSelectedSchedule.value || selectedSchedule.value
})

// 🔧 현재 표시할 스케줄의 상태 정보
const currentScheduleStatus = computed(() => {
  const schedule = displaySchedule.value
  if (!schedule) return null

  try {
    const current = icdStore.currentTrackingMstId
    const currentDetailId = icdStore.currentTrackingDetailId // ✅ detailId 추가
    const next = icdStore.nextTrackingMstId
    const nextDetailId = icdStore.nextTrackingDetailId // ✅ detailId 추가
    // ✅ FIX: fallback 제거 - mstId는 필수, null이면 매칭 안 함
    const scheduleMstId = schedule.mstId ? Number(schedule.mstId) : null
    const scheduleDetailId = schedule.detailId ?? 0

    // mstId가 없으면 상태 없음
    if (scheduleMstId === null) {
      return null
    }

    // ✅ FIX: detailId 매칭 로직 수정 - detailId가 null이면 mstId만으로 매칭
    if (current !== null &&
        scheduleMstId === Number(current) &&
        (currentDetailId === null || Number(scheduleDetailId) === Number(currentDetailId))) {
      return {
        color: 'positive',
        label: '추적중'
      }
    }

    // ✅ FIX: detailId 매칭 로직 수정 - detailId가 null이면 mstId만으로 매칭
    if (next !== null &&
        scheduleMstId === Number(next) &&
        (nextDetailId === null || Number(scheduleDetailId) === Number(nextDetailId))) {
      return {
        color: 'primary',
        label: '대기중'
      }
    }

    // 일반 스케줄
    return {
      color: 'grey-5',
      label: '일반'
    }
  } catch (error) {
    console.error('❌ currentScheduleStatus 에러:', error)
    return null
  }
})

// 🔧 남은 시간 계산
const timeRemaining = ref(0)
let timeUpdateTimer: ReturnType<typeof setInterval> | null = null

const updateTimeRemaining = () => {
  if (displaySchedule.value?.startTime) {
    try {
      const startTimeMs = new Date(displaySchedule.value.startTime).getTime()
      const currentCalTime = getCalTimeTimestamp(icdStore.resultTimeOffsetCalTime)
      const remainingMs = startTimeMs - currentCalTime
      timeRemaining.value = remainingMs
    } catch (error) {
      console.error('시간 계산 오류:', error)
      const clientTime = Date.now()
      const startTimeMs = new Date(displaySchedule.value.startTime).getTime()
      timeRemaining.value = Math.max(0, startTimeMs - clientTime)
    }
  } else {
    timeRemaining.value = 0
  }
}

// 🔧 실시간 시간 업데이트 타이머 시작/중지
const startTimeTimer = () => {
  if (timeUpdateTimer) {
    clearInterval(timeUpdateTimer)
  }
  timeUpdateTimer = setInterval(updateTimeRemaining, 1000) // 1초마다 업데이트
}

const stopTimeTimer = () => {
  if (timeUpdateTimer) {
    clearInterval(timeUpdateTimer)
    timeUpdateTimer = null
  }
}

// 🆕 선택된 스케줄의 추적 경로 로드 (watch보다 먼저 선언)
// ✅ 현재 추적 중인 스케줄이 있으면 해당 스케줄의 경로만 로드
const loadSelectedScheduleTrackingPath = async () => {
  try {
    // ✅ 현재 추적 중인 스케줄 우선 확인
    const currentTrackingMstId = icdStore.currentTrackingMstId
    let scheduleToLoad: ScheduleItem | null = null

    if (currentTrackingMstId !== null) {
      // 현재 추적 중인 스케줄이 있으면 해당 스케줄 사용
      // ✅ mstId와 detailId 기준으로 매칭 (전역 고유 ID + 패스 인덱스, detailId가 항상 존재한다고 가정)
      const currentDetailId = icdStore.currentTrackingDetailId
      const currentSchedule = sortedScheduleList.value.find(s =>
        Number(s.mstId) === Number(currentTrackingMstId) &&
        (currentDetailId !== null && s.detailId !== null && Number(s.detailId) === Number(currentDetailId))
      )
      if (currentSchedule) {
        scheduleToLoad = currentSchedule
        console.log('🎯 현재 추적 중인 스케줄의 경로 로드:', currentSchedule.satelliteName, currentSchedule.mstId, currentSchedule.detailId)
      }
    } else {
      // 현재 추적 중인 스케줄이 없으면 선택된 스케줄 사용
      scheduleToLoad = displaySchedule.value
      if (scheduleToLoad) {
        console.log('📌 선택된 스케줄의 경로 로드:', scheduleToLoad.satelliteName)
      }
    }

    if (!scheduleToLoad) {
      console.log('⚠️ 로드할 스케줄이 없음')
      return
    }

    // ✅ mstId와 detailId 사용 (satelliteId 불필요)
    const mstId = scheduleToLoad.mstId
    const detailId = scheduleToLoad.detailId

    if (!mstId || detailId == null) {
      console.warn('⚠️ MstId 또는 DetailId가 없음:', {
        mstId: scheduleToLoad.mstId,
        detailId: scheduleToLoad.detailId,
        no: scheduleToLoad.no,
        satelliteName: scheduleToLoad.satelliteName
      })
      return
    }

    // ✅ 스케줄의 keyhole 여부에 따라 DataType 결정
    const isKeyhole = scheduleToLoad.isKeyhole || scheduleToLoad.IsKeyhole || false
    const dataType = isKeyhole ? 'keyhole_optimized_final_transformed' : 'final_transformed'

    console.log('🚀 스케줄 추적 경로 로드 시작:', {
      satelliteName: scheduleToLoad.satelliteName,
      mstId,
      detailId,
      isKeyhole,
      dataType,
    })

    // ✅ DataType을 Store에 전달
    const success = await passScheduleStore.loadTrackingDetailData(
      mstId,
      detailId,
      dataType  // ✅ DataType 전달
    )

    if (success) {
      console.log('✅ 추적 경로 로드 완료, 차트 업데이트')
      // ✅ 예정 경로 길이 초기화 (새 경로 로드)
      lastPredictedPathLength.value = 0
      // 차트가 초기화되어 있다면 즉시 업데이트 (안전한 호출)
      void nextTick(() => {
        if (passChart && !passChart.isDisposed() && typeof updateChart === 'function') {
          try {
            updateChart()
          } catch (error) {
            console.error('❌ 차트 업데이트 중 오류:', error)
          }
        }
      })
    } else {
      console.warn('⚠️ 추적 경로 로드 실패')
    }
  } catch (error) {
    console.error('❌ 추적 경로 로드 중 오류:', error)
  }
}

// 🔧 스케줄 변경 시 시간 업데이트
watch(displaySchedule, (newSchedule) => {
  updateTimeRemaining()
  if (newSchedule) {
    startTimeTimer()
    // 🆕 스케줄이 선택되면 자동으로 추적 경로 로드
    void loadSelectedScheduleTrackingPath()
  } else {
    stopTimeTimer()
    // 🆕 스케줄이 해제되면 추적 경로 초기화
    passScheduleStore.clearTrackingPaths()
  }
}, { immediate: true })
const loading = passScheduleStore.loading

// ✅ inputs/outputs는 useOffsetControls composable에서 가져옴 (3개 페이지에서 동기화)

// 테이블 컬럼 정의 - Store의 실제 필드명에 맞춤
type QTableColumn = NonNullable<QTableProps['columns']>[0]

const scheduleColumns: QTableColumn[] = [
  { name: 'no', label: 'No', field: 'no', align: 'center' as const, sortable: true, style: 'width: 60px' },
  { name: 'mstId', label: 'MstId', field: 'mstId', align: 'center' as const, sortable: true, style: 'width: 70px' },
  { name: 'detailId', label: 'DetailId', field: 'detailId', align: 'center' as const, sortable: true, style: 'width: 70px' },
  {
    name: 'satelliteInfo',
    label: '위성 ID\n위성 이름',
    field: 'satelliteId',
    align: 'center' as const,
    sortable: true,
    style: 'width: 120px',
    headerStyle: 'white-space: pre-line; line-height: 1.3; text-align: center; vertical-align: middle;'
  },
  {
    name: 'timeRange',
    label: '시작 시간\n종료 시간',
    field: 'startTime',
    align: 'center' as const,
    sortable: true,
    style: 'width: 150px',
    headerStyle: 'white-space: pre-line; line-height: 1.3; text-align: center; vertical-align: middle;'
  },
  {
    name: 'duration',
    label: '지속 시간',
    field: 'duration',
    align: 'center' as const,
    sortable: true,
    format: (val) => formatDuration(val),
    style: 'width: 80px'
  },
  {
    name: 'azimuthRange',
    label: 'Start Az\nEnd Az',
    field: 'startAzimuthAngle',
    align: 'center' as const,
    sortable: true,
    style: 'width: 100px',
    headerStyle: 'white-space: pre-line; line-height: 1.3; text-align: center; vertical-align: middle;'
  },
  {
    name: 'elevationInfo',
    label: 'Max El\nTilt',
    field: 'maxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 80px',
    headerStyle: 'white-space: pre-line; line-height: 1.3; text-align: center; vertical-align: middle;'
  },
  { name: 'keyhole', label: 'keyhole', field: 'keyhole', align: 'center' as const, sortable: false, style: 'width: 80px' },
]

// NOTE: formatDateTime, formatAngle 함수는 ScheduleTable 컴포넌트로 이동됨

// ✅ Duration 포맷 함수 추가 (V006 Fix: 숫자/ISO 8601 모두 처리)
const formatDuration = (duration: string | number | null | undefined): string => {
  if (duration === null || duration === undefined) return '0분 0초'

  // ✅ 숫자(초 단위)인 경우 직접 변환
  if (typeof duration === 'number') {
    const totalSeconds = Math.round(duration)
    const hours = Math.floor(totalSeconds / 3600)
    const minutes = Math.floor((totalSeconds % 3600) / 60)
    const seconds = totalSeconds % 60
    const parts: string[] = []
    if (hours > 0) parts.push(`${hours}시간`)
    if (minutes > 0) parts.push(`${minutes}분`)
    if (seconds > 0 || parts.length === 0) parts.push(`${seconds}초`)
    return parts.join(' ')
  }

  // ISO 8601 Duration 형식 (PT13M43.6S) 파싱
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return String(duration) // 파싱 실패 시 원본 반환

  const hours = parseInt(match[1] || '0', 10)
  const minutes = parseInt(match[2] || '0', 10)
  const seconds = parseFloat(match[3] || '0')

  const parts: string[] = []
  if (hours > 0) parts.push(`${hours}시간`)
  if (minutes > 0) parts.push(`${minutes}분`)
  if (seconds > 0) parts.push(`${Math.round(seconds)}초`)

  return parts.length > 0 ? parts.join(' ') : '0분 0초'
}

// TLE 업로드 핸들러
const handleTLEUpload = async () => {
  try {
    console.log('TLE 업로드 모달 열기')

    const modal = await openModal('tle-upload', {
      width: 1000,
      height: 860,
      modalClass: 'tle-upload-modal',
      onClose: () => {
        void (async () => {
          console.log('TLE 업로드 모달 닫힘')

          // ✅ TLE 업로드 후 스케줄 데이터 갱신
          // 업로드가 성공했을 가능성이 있으므로 스케줄 데이터를 강제로 다시 로드
          try {
            console.log('🔄 TLE 업로드 후 스케줄 데이터 갱신 시작')

            // ✅ 최신 스케줄 데이터 로드 (fetchScheduleDataFromServer가 내부에서 자동으로 덮어씀)
            await passScheduleStore.fetchScheduleDataFromServer()

            console.log('✅ 스케줄 데이터 갱신 완료:', passScheduleStore.scheduleData.length, '개')
          } catch (error) {
            console.error('❌ 스케줄 데이터 갱신 실패:', error)
          }
        })()
      },
      onError: (error) => {
        console.error('TLE 업로드 모달 오류:', error)
        $q.notify({
          type: 'negative',
          message: 'TLE 업로드 창을 열 수 없습니다',
        })
      },
    })

    if (modal) {
      console.log('TLE 업로드 모달 열기 성공')
    }
  } catch (err) {
    handleApiError(err, 'TLE 업로드 모달 열기')
  }
}

// ✅ 차트 크기 조정 함수 (외부에서도 호출 가능) - DOM 스타일을 먼저 설정하여 깜빡임 방지
const adjustChartSize = async () => {
  await nextTick() // ✅ Vue의 DOM 업데이트 완료 대기

  if (!passChart || passChart.isDisposed() || !chartRef.value) return

  // ✅ 차트 크기 설정
  const chartSize = 500

  // ✅ 1단계: DOM 스타일을 먼저 설정 (리사이즈 전에!)
  // 이렇게 하면 차트가 처음부터 올바른 위치에서 렌더링되어 깜빡임이 없음
  const chartElement = chartRef.value.querySelector('div') as HTMLElement | null
  if (chartElement) {
    // ✅ 스타일을 먼저 설정하여 차트가 올바른 위치에서 렌더링되도록 함
    chartElement.style.width = `${chartSize}px`
    chartElement.style.height = `${chartSize}px`
    chartElement.style.maxWidth = `${chartSize}px`
    chartElement.style.maxHeight = `${chartSize}px`
    chartElement.style.minWidth = `${chartSize}px`
    chartElement.style.minHeight = `${chartSize}px`
    // ✅ 중앙 정렬
    chartElement.style.top = '50%'
    chartElement.style.position = 'absolute'
    chartElement.style.left = '50%'
    chartElement.style.transform = 'translate(-50%, -50%)'
  }

  // ✅ 2단계: DOM 스타일 적용 후 리사이즈 (스타일이 적용된 상태에서)
  await nextTick()
  passChart.resize({
    width: chartSize,
    height: chartSize
  })

  console.log('차트 리사이즈 완료:', chartSize)
}

// ✅ 차트 초기화 함수 수정 - 컨테이너 크기에 맞춘 크기
const initChart = () => {
  try {
    // ✅ 이미 초기화되었으면 재초기화하지 않음
    if (isChartInitialized.value && passChart && !passChart.isDisposed()) {
      console.log('✅ 차트가 이미 초기화되어 있음 - 재초기화 건너뜀')
      return
    }

    if (!chartRef.value) {
      console.warn('⚠️ 차트 컨테이너가 아직 준비되지 않음')
      return
    }

    // 기존 차트 인스턴스가 있으면 제거
    if (passChart) {
      try {
        passChart.dispose()
      } catch (disposeError) {
        console.warn('차트 dispose 오류 (무시):', disposeError)
      }
    }

    // ✅ 차트 크기 설정 (차트를 더 크게, Position View 구역 크기와 독립적)
    const initialSize = 500

    // 차트 인스턴스 생성
    passChart = echarts.init(chartRef.value, null, {
      width: initialSize,
      height: initialSize
    })
    console.log('PassSchedule 차트 인스턴스 생성됨, 크기:', initialSize)

    // 차트 옵션 설정
    const option = {
      backgroundColor: 'transparent',
      grid: {
        left: '10%', /* ✅ 균등한 여백 확보 */
        right: '10%',
        top: '10%',
        bottom: '10%',
        containLabel: false
      },
      polar: {
        radius: ['0%', '50%'],
        center: ['50%', '50%'],
      },
      angleAxis: {
        type: 'value',
        startAngle: 90,
        clockwise: true,
        min: 0,
        max: 360,
        animation: false, // ✅ 애니메이션 완전 비활성화
        axisLine: {
          show: true,
          lineStyle: {
            color: '#555',
          },
        },
        axisTick: {
          show: true,
          interval: 60,
          length: 3,
          lineStyle: {
            color: '#555',
          },
        },
        axisLabel: {
          interval: 60,
          formatter: function (value: number) {
            if (value === 0) return 'N (0°)'
            if (value === 90) return 'E (90°)'
            if (value === 180) return 'S (180°)'
            if (value === 270) return 'W (270°)'
            if (value === 45) return 'NE (45°)'
            if (value === 135) return 'SE (135°)'
            if (value === 225) return 'SW (225°)'
            if (value === 315) return 'NW (315°)'
            if (value % 60 === 0) return value + '°'
            return ''
          },
          color: '#999',
          fontSize: 8,
          distance: -8,
        },
        splitLine: {
          show: true,
          interval: 60,
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
        animation: false, // ✅ 애니메이션 완전 비활성화
        axisLine: {
          show: false,
        },
        axisTick: {
          show: false,
        },
        axisLabel: {
          formatter: '{value}°',
          color: '#999',
          fontSize: 8,
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
          name: '실시간 추적 위치',
          type: 'scatter',
          coordinateSystem: 'polar',
          symbol: 'circle',
          symbolSize: 15,
          animation: false, // ✅ 애니메이션 완전 비활성화
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
              // ✅ 원본 값 표시 (정규화된 값이 아닌)
              const originalAz = currentPosition.value?.azimuth || params.value[1]
              const originalEl = currentPosition.value?.elevation || params.value[0]
              return `Az: ${originalAz.toFixed(2)}°\nEl: ${originalEl.toFixed(2)}°`
            },
            position: 'top',
            distance: 5,
            color: '#fff',
            backgroundColor: 'rgba(0,0,0,0.7)',
            padding: [4, 8],
            borderRadius: 4,
            fontSize: 10,
          },
          zlevel: 3,
        },
        // ✅ 위치 선 제거 - 현재 위치 점이 이동하면서 실시간 경로를 그리므로 불필요
        {
          name: '실시간 추적 경로',
          type: 'line',
          coordinateSystem: 'polar',
          symbol: 'none',
          animation: false, // ✅ 애니메이션 완전 비활성화
          lineStyle: {
            color: '#ffffff', // 흰색
            width: 2,
            opacity: 0.8,
          },
          data: [],
          zlevel: 2,
        },
        {
          name: '예정 위성 궤적',
          type: 'line',
          coordinateSystem: 'polar',
          symbol: 'none',
          animation: false, // ✅ 애니메이션 완전 비활성화
          lineStyle: {
            color: '#2196f3', // 파란색
            width: 2,
          },
          data: [],
          zlevel: 1,
        },
      ],
    }

    // 차트 옵션 적용 (초기 크기)
    passChart.setOption(option, true)
    passChart.resize({
      width: initialSize,
      height: initialSize
    })
    console.log('PassSchedule 차트 옵션 적용됨')

    // ✅ DOM 스타일을 먼저 설정 (리사이즈 전에!) - EphemerisDesignationPage.vue와 동일
    void nextTick(() => {
      const chartElement = chartRef.value?.querySelector('div') as HTMLElement | null
      if (chartElement) {
        // ✅ 스타일을 먼저 설정하여 차트가 올바른 위치에서 렌더링되도록 함
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

      // ✅ 스타일 적용 후 리사이즈
      void nextTick(() => {
        if (passChart && !passChart.isDisposed()) {
          passChart.resize({
            width: initialSize,
            height: initialSize
          })
        }
      })
    })

    // ✅ 윈도우 리사이즈 이벤트에 대응 (반응형) - 컨테이너 크기 기반
    // ✅ 기존 리사이즈 리스너 제거 (중복 방지)
    if (chartResizeHandler) {
      window.removeEventListener('resize', chartResizeHandler)
      chartResizeHandler = null
    }

    chartResizeHandler = () => {
      if (!passChart || passChart.isDisposed()) return

      nextTick().then(() => {
        // ✅ 리사이즈 시에도 컨테이너 크기에 맞춰 조정
        adjustChartSize().catch(console.error)
      }).catch(console.error)
    }

    window.addEventListener('resize', chartResizeHandler)

    // ✅ 차트 초기화 완료 플래그 설정
    isChartInitialized.value = true
    console.log('✅ 차트 초기화 완료 플래그 설정')
  } catch (error) {
    console.error('차트 초기화 중 오류:', error)
    // 차트 초기화 실패해도 컴포넌트는 계속 렌더링되도록 함
    isChartInitialized.value = false
  }
}



// 🆕 성능 최적화를 위한 변수들
const lastUpdateTime = ref(0)
const updateThrottle = 100 // ✅ 100ms로 변경하여 백엔드 모니터링 주기(100ms)와 일치
const lastPathLength = ref(0)
const pathUpdateThreshold = 5 // 경로 포인트가 5개 이상 변경될 때만 업데이트
// ✅ appendData를 위한 이전 경로 길이 추적 (이미 위에서 선언됨)

// 🆕 경로 매칭 로그 스로틀링
const lastPathMatchLogTime = ref(0)
const PATH_MATCH_LOG_INTERVAL = 10000 // 10초

// 🆕 이전 상태 추적 (변경 감지용)
const lastPosition = ref<{ azimuth: number; elevation: number } | null>(null)
const lastPathInfo = ref<{ mstId: number | null; detailId: number | null } | null>(null)
// ✅ lastPredictedPathLength는 이미 515라인에서 선언됨 (중복 선언 제거)
const POSITION_CHANGE_THRESHOLD = 0.1 // 0.1도 이상 변경될 때만 업데이트

// 🆕 성능 모니터링 및 적응형 해상도 조정
const performanceMonitor = {
  lastFrameTime: 0,
  frameCount: 0,
  averageFrameTime: 0,
  performanceThreshold: 16.67, // 60fps 기준 (16.67ms)
  slowFrameThreshold: 33.33, // 30fps 기준 (33.33ms)
  currentResolution: 1, // 1 = 모든 포인트 표시, 10 = 1/10 표시
  maxResolution: 1,
  minResolution: 10,
}

// 🆕 적응형 해상도 조정 함수
const adjustDisplayResolution = (pathLength: number, frameTime: number) => {
  const currentRes = performanceMonitor.currentResolution

  // 성능이 좋으면 해상도 높이기
  if (frameTime < performanceMonitor.performanceThreshold && currentRes > performanceMonitor.maxResolution) {
    performanceMonitor.currentResolution = Math.max(performanceMonitor.maxResolution, currentRes - 1)
    console.log(`🟢 성능 개선 - 해상도 증가: 1/${currentRes} → 1/${performanceMonitor.currentResolution}`)
  }

  // 성능이 나쁘면 해상도 낮추기
  if (frameTime > performanceMonitor.slowFrameThreshold && currentRes < performanceMonitor.minResolution) {
    performanceMonitor.currentResolution = Math.min(performanceMonitor.minResolution, currentRes + 1)
    console.log(`🔴 성능 저하 - 해상도 감소: 1/${currentRes} → 1/${performanceMonitor.currentResolution}`)
  }

  // 포인트 수 기반 자동 조정
  if (pathLength > 1000 && currentRes === 1) {
    performanceMonitor.currentResolution = 10
    console.log(`📊 포인트 수 초과 - 자동 해상도 조정: 1/1 → 1/10`)
  }

  return performanceMonitor.currentResolution
}

// 🆕 적응형 경로 최적화 함수
const optimizePathAdaptive = (path: [number, number][], resolution: number): [number, number][] => {
  if (!path || path.length === 0) return []

  // 해상도에 따라 포인트 샘플링
  const optimizedPath: [number, number][] = []

  for (let i = 0; i < path.length; i += resolution) {
    const point = path[i]
    if (point && Array.isArray(point) && point.length === 2) {
      optimizedPath.push(point)
    }
  }

  // 마지막 포인트는 항상 포함
  const lastPoint = path[path.length - 1]
  if (lastPoint && Array.isArray(lastPoint) && lastPoint.length === 2 &&
    optimizedPath[optimizedPath.length - 1] !== lastPoint) {
    optimizedPath.push(lastPoint)
  }

  return optimizedPath
}

// 🆕 성능 모니터링이 포함된 차트 업데이트
const updateChartWithPerformanceMonitoring = () => {
  try {
    if (!passChart || passChart.isDisposed()) {
      return
    }

    const startTime = performance.now()
    const now = Date.now()

    // 스로틀링
    if (now - lastUpdateTime.value < updateThrottle) {
      return
    }

    // ✅ 추적 상태 확인 (TRACKING 상태에서만 trackingActual 사용)
    // PassSchedule은 passScheduleTrackingState를 사용 (ephemerisTrackingState가 아님)
    // COMPLETED, IDLE, WAITING, PREPARING 상태에서는 일반 각도 값 사용
    const trackingState = icdStore.passScheduleTrackingState
    const isActuallyTracking = trackingState === 'TRACKING'

    // ✅ 하드웨어 초기값 튀는 현상 방지: 추적 시작 시 초기 프레임 스킵
    if (isTrackingInitPhase && isActuallyTracking) {
      trackingFrameCount++
      if (trackingFrameCount <= INITIAL_FRAMES_TO_SKIP) {
        console.log(`⏭️ PassSchedule 초기 프레임 스킵 중... (${trackingFrameCount}/${INITIAL_FRAMES_TO_SKIP})`)
        return  // 차트 업데이트 스킵
      } else {
        // 초기 단계 종료
        isTrackingInitPhase = false
        console.log('✅ PassSchedule 초기 프레임 스킵 완료 - 정상 추적 시작')
      }
    }

    // ✅ 기본값: 일반 각도 값 (안테나 실제 위치)
    let azimuth = parseFloat(icdStore.azimuthAngle) || 0
    let elevation = parseFloat(icdStore.elevationAngle) || 0

    // ✅ 실제 추적 중일 때만 trackingActual 값 사용 (점프 현상 방지)
    if (isActuallyTracking) {
      const trackingAz = parseFloat(icdStore.trackingActualAzimuthAngle)
      const trackingEl = parseFloat(icdStore.trackingActualElevationAngle)
      // ✅ trackingCMD 값 추가 (이전 세션 값 검증용)
      const trackingCmdAz = parseFloat(icdStore.trackingCMDAzimuthAngle)
      const trackingCmdEl = parseFloat(icdStore.trackingCMDElevationAngle)

      // ✅ trackingActual이 CMD 값과 근접한지 확인 (이전 세션 값 방지)
      // CMD 값이 유효하지 않으면 trackingActual도 사용하지 않음 (추적 시작 직후 점프 방지)
      const hasCmdAz = !isNaN(trackingCmdAz) && trackingCmdAz !== 0
      const hasCmdEl = !isNaN(trackingCmdEl) && trackingCmdEl !== 0
      const isTrackingAzValid = !isNaN(trackingAz) && trackingAz !== 0 &&
        hasCmdAz && Math.abs(trackingAz - trackingCmdAz) < 5
      const isTrackingElValid = !isNaN(trackingEl) && trackingEl !== 0 &&
        hasCmdEl && Math.abs(trackingEl - trackingCmdEl) < 5

      // ✅ 검증된 trackingActual → trackingCMD → 일반 값
      if (isTrackingAzValid) {
        azimuth = trackingAz
      } else if (hasCmdAz) {
        azimuth = trackingCmdAz
      }
      // else: 일반 azimuthAngle 값 유지

      if (isTrackingElValid) {
        elevation = trackingEl
      } else if (hasCmdEl) {
        elevation = trackingCmdEl
      }
      // else: 일반 elevationAngle 값 유지
    }
    // ✅ isActuallyTracking === false일 때는 일반 azimuthAngle/elevationAngle 값 그대로 사용

    const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))

    // 🆕 위치 변경 감지 (실제 변경이 있을 때만 업데이트)
    const currentPos = { azimuth: normalizedAz, elevation: normalizedEl }
    const hasPositionChanged = !lastPosition.value ||
      Math.abs(currentPos.azimuth - lastPosition.value.azimuth) > POSITION_CHANGE_THRESHOLD ||
      Math.abs(currentPos.elevation - lastPosition.value.elevation) > POSITION_CHANGE_THRESHOLD

    currentPosition.value = currentPos

    // 경로 업데이트 조건
    const shouldUpdatePath = icdStore.passScheduleStatusInfo?.isActive === true ||
      icdStore.currentTrackingMstId !== null ||
      icdStore.nextTrackingMstId !== null

    // 🆕 actualPath 선언
    const actualPath = passScheduleStore.actualTrackingPath

    if (shouldUpdatePath) {
      const currentPathLength = actualPath?.length || 0

      if (Math.abs(currentPathLength - lastPathLength.value) >= pathUpdateThreshold) {
        console.log('✅ 추적 경로 업데이트 시작 (적응형 해상도)')
        void passScheduleStore.updateActualTrackingPath(normalizedAz, normalizedEl)
        lastPathLength.value = currentPathLength
      }
    }

    // 🆕 적응형 해상도 조정
    // ✅ 차트 경로 표시 우선순위: 현재 추적 중인 스케줄 우선, 없으면 선택된 스케줄만 표시
    const currentTrackingMstId = icdStore.currentTrackingMstId
    const selectedSchedule = displaySchedule.value
    const shouldShowPredictedPath = currentTrackingMstId !== null || selectedSchedule !== null

    // 🆕 pathInfo를 함수 상단에서 선언 (스코프 문제 해결)
    const pathInfo = passScheduleStore.currentTrackingPathInfo

    // 현재 추적 중인 스케줄이 있으면 해당 스케줄의 경로만, 없으면 선택된 스케줄의 경로만 표시
    let predictedPathToShow: [number, number][] = []

    if (shouldShowPredictedPath) {
      const currentPath = passScheduleStore.predictedTrackingPath

      // ✅ 경로가 있으면 매칭 확인, 없으면 표시하지 않음 (mstId, detailId만 비교)
      if (currentPath && currentPath.length > 0) {
        // 현재 추적 중인 스케줄이 있으면 해당 스케줄의 경로 사용
        if (currentTrackingMstId !== null) {
          // ✅ mstId 기준으로 매칭 (전역 고유 ID)
          const currentSchedule = sortedScheduleList.value.find(s => Number(s.mstId) === Number(currentTrackingMstId))
          if (currentSchedule) {
            const scheduleMstId = currentSchedule.mstId
            const scheduleDetailId = currentSchedule.detailId

            // ✅ 경로 매칭: mstId와 detailId만 비교
            if (!pathInfo.passId ||
                (pathInfo.passId === scheduleMstId &&
                 pathInfo.detailId === scheduleDetailId)) {
              predictedPathToShow = currentPath.map((point: readonly [number, number]) => [...point])

              // 🆕 로그 스로틀링 (10초당 1개)
              const currentTime = Date.now()
              if (currentTime - lastPathMatchLogTime.value >= PATH_MATCH_LOG_INTERVAL) {
                console.log('✅ 현재 추적 스케줄 경로 표시:', {
                  scheduleMstId,
                  scheduleDetailId,
                  pathInfoMstId: pathInfo.passId,
                  pathInfoDetailId: pathInfo.detailId,
                  pathLength: currentPath.length
                })
                lastPathMatchLogTime.value = currentTime
              }
            } else {
              // 🆕 로그 스로틀링 (10초당 1개)
              const currentTime = Date.now()
              if (currentTime - lastPathMatchLogTime.value >= PATH_MATCH_LOG_INTERVAL) {
                console.log('⚠️ 경로 매칭 실패 (현재 추적):', {
                  scheduleMstId,
                  scheduleDetailId,
                  pathInfoMstId: pathInfo.passId,
                  pathInfoDetailId: pathInfo.detailId
                })
                lastPathMatchLogTime.value = currentTime
              }
            }
          }
        } else if (selectedSchedule) {
          // ✅ 선택된 스케줄의 경로 사용
          const scheduleMstId = selectedSchedule.mstId
          const scheduleDetailId = selectedSchedule.detailId

          // ✅ 경로 매칭: mstId와 detailId만 비교
          if (!pathInfo.passId ||
              (pathInfo.passId === scheduleMstId &&
               pathInfo.detailId === scheduleDetailId)) {
            predictedPathToShow = currentPath.map((point: readonly [number, number]) => [...point])

            // 🆕 로그 스로틀링 (10초당 1개)
            const currentTime = Date.now()
            if (currentTime - lastPathMatchLogTime.value >= PATH_MATCH_LOG_INTERVAL) {
              console.log('✅ 선택된 스케줄 경로 표시:', {
                scheduleMstId,
                scheduleDetailId,
                pathInfoMstId: pathInfo.passId,
                pathInfoDetailId: pathInfo.detailId,
                pathLength: currentPath.length
              })
              lastPathMatchLogTime.value = currentTime
            }
          } else {
            // 🆕 로그 스로틀링 (10초당 1개)
            const currentTime = Date.now()
            if (currentTime - lastPathMatchLogTime.value >= PATH_MATCH_LOG_INTERVAL) {
              console.log('⚠️ 경로 매칭 실패 (선택된 스케줄):', {
                scheduleMstId,
                scheduleDetailId,
                pathInfoMstId: pathInfo.passId,
                pathInfoDetailId: pathInfo.detailId
              })
              lastPathMatchLogTime.value = currentTime
            }
          }
        } else {
          // ✅ 스케줄이 없어도 경로가 있으면 표시 (pathInfo가 없을 때)
          if (!pathInfo.passId) {
            predictedPathToShow = currentPath.map((point: readonly [number, number]) => [...point])
            console.log('✅ 경로 정보 없음 - 경로 표시:', currentPath.length, '개 포인트')
          }
        }
      } else {
        console.log('⚠️ 예상 경로 데이터 없음')
      }
    }

    const shouldShowTrackingPath = icdStore.passScheduleStatusInfo?.isActive === true &&
      actualPath && actualPath.length > 0

    // 성능 모니터링 및 해상도 조정
    const currentFrameTime = performance.now() - startTime
    const resolution = adjustDisplayResolution(actualPath?.length || 0, currentFrameTime)

    // 적응형 경로 최적화
    const displayPath = shouldShowTrackingPath ?
      optimizePathAdaptive(actualPath as [number, number][], resolution) : []

    // 🆕 변경 감지: 경로 길이, 위치, 경로 정보가 변경되었는지 확인
    const currentPathInfo = {
      mstId: pathInfo?.passId ?? null,
      detailId: pathInfo?.detailId ?? null
    }
    const hasPathInfoChanged = !lastPathInfo.value ||
      lastPathInfo.value.mstId !== currentPathInfo.mstId ||
      lastPathInfo.value.detailId !== currentPathInfo.detailId
    const hasPredictedPathLengthChanged = predictedPathToShow.length !== lastPredictedPathLength.value
    const hasDisplayPathChanged = displayPath.length !== lastTrackingPathLength.value

    // 🆕 실제 변경이 있을 때만 차트 업데이트
    // ✅ 현재 위치는 항상 업데이트 (hasPositionChanged)
    // 추적 중: 위치 변경이나 실제 경로 변경 시 업데이트
    // 이론치만 표시: 위치 변경이나 경로 정보 변경 시 업데이트
    const shouldUpdateChart = hasPositionChanged || // ✅ 현재 위치 변경 시 항상 업데이트
      (shouldShowTrackingPath
        ? hasDisplayPathChanged // 추적 중: 실제 경로 변경 시
        : (hasPathInfoChanged || hasPredictedPathLengthChanged)) // 이론치만: 경로 정보 변경 시

    // ✅ PassChartUpdatePool을 사용한 차트 업데이트 (기존 방식으로 복원)
    try {
      if (shouldUpdateChart) {
        passChartPool.updatePosition(normalizedEl, normalizedAz)
        passChartPool.updateTrackingPath(displayPath)
        // ✅ 현재 추적 중인 스케줄 또는 선택된 스케줄의 경로만 표시
        passChartPool.updatePredictedPath(predictedPathToShow)

        // ✅ 전체 업데이트 (appendData 대신 setOption 사용)
        const finalOption = passChartPool.getUpdateOption()

        if (passChart && !passChart.isDisposed()) {
          passChart.setOption(finalOption, false, true)

          // ✅ 경로 길이 추적 업데이트
          if (shouldShowTrackingPath) {
            lastTrackingPathLength.value = displayPath.length
          } else {
            lastTrackingPathLength.value = 0
          }
          lastPredictedPathLength.value = predictedPathToShow.length

          // 🆕 이전 상태 업데이트
          lastPosition.value = currentPos
          lastPathInfo.value = currentPathInfo

          lastUpdateTime.value = now
        }
          // 성능 통계 업데이트
          performanceMonitor.frameCount++
          performanceMonitor.averageFrameTime =
            (performanceMonitor.averageFrameTime * (performanceMonitor.frameCount - 1) + currentFrameTime) /
            performanceMonitor.frameCount

          // 성능 로그 (10프레임마다)
          if (performanceMonitor.frameCount % 10 === 0) {
            console.log(`📊 성능 통계: 평균 ${performanceMonitor.averageFrameTime.toFixed(2)}ms, 해상도: 1/${resolution}, 포인트: ${displayPath.length}/${actualPath?.length || 0}`)
          }
        } else {
          // 🆕 변경이 없으면 스킵 (리소스 절약)
          return
        }
    } catch (chartError) {
      console.error('차트 업데이트 중 오류:', chartError)
      // 차트 업데이트 실패해도 컴포넌트는 계속 동작하도록 함
    }
  } catch (error) {
    console.error('PassSchedule 차트 업데이트 오류:', error)
    // 에러가 발생해도 컴포넌트는 계속 동작하도록 함
  }
}

// 🆕 기존 updateChart 함수를 성능 모니터링 버전으로 교체
const updateChart = updateChartWithPerformanceMonitoring

const selectScheduleData = async () => {
  try {
    console.log('스케줄 선택 모달 열기')

    // ✅ 1순위: localStorage에서 스케줄 데이터 로드 (빠름)
    const cached = passScheduleStore.loadScheduleDataFromLocalStorage()

    if (cached && passScheduleStore.scheduleData.length > 0) {
      console.log('✅ 캐시된 스케줄 데이터 사용 (API 호출 생략):', passScheduleStore.scheduleData.length, '개')
    } else {
      // ✅ 2순위: API 호출 (캐시가 없을 때만)
      console.log('📡 스케줄 데이터 API 호출 시작 (캐시 없음)')
      try {
        await passScheduleStore.fetchScheduleDataFromServer()
        console.log('✅ 스케줄 데이터 로드 완료:', passScheduleStore.scheduleData.length, '개')
      } catch (err) {
        handleApiError(err, '스케줄 데이터 로드')
        return
      }
    }

    const modal = await openModal('select-schedule', {
      width: 1200,
      height: 700,
      modalClass: 'select-schedule-modal',
      onClose: (selectedData?: ScheduleItem) => {
        console.log('스케줄 선택 모달 닫힘', selectedData)
        // 🔧 SelectScheduleContent에서 이미 처리했으므로 추가 작업 없음
        if (selectedData) {
          console.log('✅ 스케줄이 이미 SelectScheduleContent에서 처리되었습니다:', selectedData.satelliteName)
        }
      },
      onError: (error) => {
        console.error('스케줄 선택 모달 오류:', error)
        $q.notify({
          type: 'negative',
          message: '스케줄 선택 창을 열 수 없습니다',
        })
      },
    })

    if (modal) {
      console.log('스케줄 선택 모달 열기 성공')
    }
  } catch (err) {
    handleApiError(err, '스케줄 선택 모달 열기')
  }
}

// 테이블 행 클릭 이벤트 핸들러
const onRowClick = (evt: Event, row: ScheduleItem) => {
  selectedSchedule.value = row
  passScheduleStore.selectSchedule(row) // Store에도 선택 상태 저장
  void updateScheduleChart() // 비동기 함수를 명시적으로 무시

  console.log('스케줄 선택됨:', {
    mstId: row.mstId,
    no: row.no,
    satelliteName: row.satelliteName,
    startTime: row.startTime,
  })
}

// ScheduleTable 컴포넌트에서 emit된 row-click 이벤트 핸들러
const onTableRowClick = (evt: Event, row: ScheduleItem) => {
  onRowClick(evt, row)
}

// 🆕 선택된 스케줄에 따른 차트 업데이트 (사용하지 않음 - loadSelectedScheduleTrackingPath로 대체)
const updateScheduleChart = async () => {
  if (!passChart || !selectedSchedule.value) return

  try {
    // Store의 추적 경로 초기화
    passScheduleStore.clearTrackingPaths()

    // ✅ mstId와 detailId 사용 (satelliteId 불필요)
    const mstId = selectedSchedule.value.mstId
    const detailId = selectedSchedule.value.detailId

    console.log('🔍 updateScheduleChart - mstId/detailId 확인:', {
      mstId: selectedSchedule.value.mstId,
      detailId: selectedSchedule.value.detailId,
      no: selectedSchedule.value.no,
      satelliteName: selectedSchedule.value.satelliteName,
      schedule: selectedSchedule.value // ✅ 전체 스케줄 객체 확인
    })

    if (!mstId || detailId == null) {
      console.warn('⚠️ MstId 또는 DetailId가 없음:', {
        mstId: selectedSchedule.value.mstId,
        detailId: selectedSchedule.value.detailId,
        no: selectedSchedule.value.no,
        satelliteName: selectedSchedule.value.satelliteName
      })
      return
    }

    if (mstId && detailId != null) {
      // ✅ 스케줄의 keyhole 여부에 따라 DataType 결정
      const isKeyhole = selectedSchedule.value.isKeyhole || selectedSchedule.value.IsKeyhole || false
      const dataType = isKeyhole ? 'keyhole_optimized_final_transformed' : 'final_transformed'

      console.log(`🛰️ 스케줄 선택 - 추적 경로 조회: mstId=${mstId}, detailId=${detailId}, DataType: ${dataType}`)

      // ✅ DataType을 Store에 전달
      const success = await passScheduleStore.loadTrackingDetailData(mstId, detailId, dataType)

      if (success) {
        console.log('✅ 추적 경로 데이터 로드 성공')
        updateChart()
      } else {
        console.warn('❌ 추적 경로 데이터 로드 실패 - 데이터가 없어 경로를 표시하지 않습니다')
        // ✅ 데이터가 없으면 더미 경로를 설정하지 않음 (경로 표시 안 함)
        passScheduleStore.clearTrackingPaths()
        updateChart()
      }
    } else {
      console.warn('❌ 스케줄에서 필요한 정보를 찾을 수 없음:', selectedSchedule.value)
    }

  } catch (error) {
    console.error('스케줄 차트 업데이트 오류:', error)
  }
}

// 🆕 예상 경로 설정 함수 (Store 통해서)
// ✅ 더미 경로 설정 함수 제거 - 데이터가 없으면 경로를 표시하지 않음
/*
// 🆕 실제 추적 경로 초기화 (Store 통해서)
const clearActualPath = () => {
  passScheduleStore.clearTrackingPaths()
  updateChart()
}
 */
// ✅ offset 관련 함수들 (increment, decrement, reset, onInputChange, formattedCalTime)은
//    useOffsetControls composable에서 가져옴 - 3개 페이지에서 동기화됨

// 명령 핸들러들 - handleStartCommand 수정
const handleStartCommand = async () => {
  // 🔧 선택된 스케줄이 아닌 등록된 모든 스케줄을 처리
  if (scheduleData.value.length === 0) {
    $q.notify({
      type: 'warning',
      message: '등록된 스케줄이 없습니다',
    })
    return
  }

  try {
    console.log('🚀 ACS Start 명령 시작 - 등록된 모든 스케줄:', scheduleData.value.length, '개')
    console.log('🔍 Start 전 Store 상태:', {
      current: icdStore.currentTrackingMstId,
      next: icdStore.nextTrackingMstId
    })

    // 🔧 등록된 모든 스케줄을 추적 대상으로 설정
    const success = await passScheduleStore.setTrackingTargets(scheduleData.value)

    if (success) {
      console.log('✅ 추적 대상 설정 성공')

      // 🆕 백엔드 추적 시작 API 호출 추가
      try {
        console.log('🚀 백엔드 추적 시작 API 호출')
        const trackingStartResult = await passScheduleStore.startScheduleTracking()

        if (trackingStartResult.success) {
          console.log('✅ 백엔드 추적 시작 성공:', trackingStartResult)
        } else {
          console.warn('⚠️ 백엔드 추적 시작 실패:', trackingStartResult.message)
          $q.notify({
            type: 'warning',
            message: '추적 대상은 설정되었으나 백엔드 추적 시작에 실패했습니다',
            caption: trackingStartResult.message
          })
        }
      } catch (error) {
        console.error('❌ 백엔드 추적 시작 API 호출 실패:', error)
        $q.notify({
          type: 'warning',
          message: '추적 대상은 설정되었으나 백엔드 추적 시작에 실패했습니다',
          caption: 'API 연결 오류'
        })
      }

      // 🆕 예측 경로 로드 (첫 번째 스케줄 기준)
      if (scheduleData.value.length > 0) {
        const firstSchedule = scheduleData.value[0]
        if (firstSchedule) {
          // ✅ mstId와 detailId 사용 (satelliteId 불필요)
          const mstId = firstSchedule.mstId
          const detailId = firstSchedule.detailId

          if (!mstId || detailId == null) {
            console.warn('⚠️ MstId 또는 DetailId가 없음:', {
              mstId: firstSchedule.mstId,
              detailId: firstSchedule.detailId,
              no: firstSchedule.no,
              satelliteName: firstSchedule.satelliteName
            })
            return
          }

          if (mstId && detailId != null) {
            console.log('🛰️ 예측 경로 로드 시작: mstId=', mstId, 'detailId=', detailId)
            try {
              const pathLoaded = await passScheduleStore.loadTrackingDetailData(mstId, detailId)
              if (pathLoaded) {
                console.log('✅ 예측 경로 로드 성공')
              } else {
                console.warn('⚠️ 예측 경로 로드 실패')
              }
            } catch (error) {
              console.error('❌ 예측 경로 로드 중 오류:', error)
            }
          }
        }
      }

      // Store 값 변경 확인을 위한 지연된 체크
      setTimeout(() => {
        console.log('🔍 Start 후 Store 상태:', {
          current: icdStore.currentTrackingMstId,
          next: icdStore.nextTrackingMstId
        })
        // 강제 업데이트 제거 - watch에서 자동으로 처리됨
        // forceTableUpdate()
      }, 1000)

      // 🆕 추적 대상 설정 성공 후 모니터링 시작
      const monitoringStarted = await passScheduleStore.startTrackingMonitor()
      if (monitoringStarted) {
        $q.notify({
          type: 'positive',
          message: `${scheduleData.value.length}개의 스케줄 추적이 시작되었습니다`,
          caption: '모니터링이 활성화되었습니다 (100ms 주기)'
        })

        console.log('✅ ACS Start 명령 완료 - 추적 대상 설정 및 모니터링 시작됨')

        // 🆕 테이블 하이라이트 디버깅
        setTimeout(() => {
          console.log('🔍 Start 후 하이라이트 상태 확인:')
          console.log('  - currentTrackingMstId:', icdStore.currentTrackingMstId)
          console.log('  - nextTrackingMstId:', icdStore.nextTrackingMstId)
          console.log('  - scheduleData:', scheduleData.value.length, '개')

          // WebSocket 데이터 확인
          console.log('📡 WebSocket 데이터 확인:')
          console.log('  - icdStore.currentTrackingMstId:', icdStore.currentTrackingMstId)
          console.log('  - icdStore.nextTrackingMstId:', icdStore.nextTrackingMstId)

          // 강제 반응성 트리거
          reactivityTrigger.value++

          // DOM 직접 조작으로 하이라이트 적용
          applyRowColors()
        }, 2000)

        // 5초 후 최종 상태 확인
        setTimeout(() => {
          console.log('⏰ 5초 후 최종 상태 확인:')
          console.log('  - icdStore.currentTrackingMstId:', icdStore.currentTrackingMstId)
          console.log('  - icdStore.nextTrackingMstId:', icdStore.nextTrackingMstId)
          console.log('  - scheduleData:', scheduleData.value.length, '개')
        }, 5000)
      } else {
        $q.notify({
          type: 'warning',
          message: '추적 대상은 설정되었으나 모니터링 시작에 실패했습니다',
          caption: '수동으로 모니터링을 시작해주세요'
        })
      }
    } else {
      $q.notify({
        type: 'negative',
        message: '추적 대상 설정에 실패했습니다',
      })
    }
  } catch (err) {
    handleApiError(err, 'ACS Start 명령')
  }
}

const handleStopCommand = async () => {
  try {
    // 🆕 백엔드 추적 중지 API 호출 추가
    try {
      console.log('🛑 백엔드 추적 중지 API 호출')
      const trackingStopResult = await passScheduleStore.stopScheduleTracking()

      if (trackingStopResult.success) {
        console.log('✅ 백엔드 추적 중지 성공:', trackingStopResult)
      } else {
        console.warn('⚠️ 백엔드 추적 중지 실패:', trackingStopResult.message)
      }
    } catch (error) {
      console.error('❌ 백엔드 추적 중지 API 호출 실패:', error)
    }

    // 🆕 추적 모니터링 먼저 중지
    const monitoringStopped = await passScheduleStore.stopTrackingMonitor()

    // 기존 ICD 정지 명령
    await icdStore.stopCommand(true, true, true)

    if (monitoringStopped) {
      $q.notify({
        type: 'positive',
        message: '추적 모니터링 및 시스템이 정지되었습니다',
      })
    } else {
      $q.notify({
        type: 'warning',
        message: '시스템 정지 명령은 전송되었으나 모니터링 중지에 실패했습니다',
      })
    }
  } catch (err) {
    handleApiError(err, '정지 명령 전송')
  }
}
const handleStowCommand = async () => {
  try {
    // 🆕 추적 중지
    await passScheduleStore.stopTrackingMonitor()
    await icdStore.stowCommand()

    $q.notify({
      type: 'positive',
      message: 'Stow 명령이 전송되었습니다',
    })
  } catch (err) {
    handleApiError(err, 'Stow 명령 전송')
  }
}
/*
const handleReset = async () => {
  try {


    // 기존 리셋 로직
    selectedSchedule.value = null
    inputs.value = ['0.00', '0.00', '0.00', '0']
    outputs.value = ['0.00', '0.00', '0.00', '0']

    // 🔧 선택된 스케줄 목록도 초기화
    passScheduleStore.clearSelectedSchedules()

    // 🆕 추적 경로 초기화 (Store 통해서)
    clearActualPath()

    // 모든 오프셋 리셋
    await icdStore.sendPositionOffsetCommand(0, 0, 0)

    $q.notify({
      type: 'info',
      message: 'PassSchedule이 리셋되었습니다',
      caption: '모니터링이 중지되고 모든 설정이 초기화되었습니다'
    })
  } catch (error) {
    console.error('Failed to reset:', error)
    $q.notify({
      type: 'negative',
      message: '리셋에 실패했습니다',
    })
  }
} */
// 초기화 함수 (사용하지 않음 - onMounted에서 직접 처리)
// const init = async () => {
//   console.log('PassSchedulePage 초기화 시작')
//   // ... 기존 코드
// }
// 컴포넌트 마운트
onMounted(() => {
  try {
    console.log('PassSchedulePage 컴포넌트 마운트됨')

    // ✅ localStorage에서 데이터 복원
    const restored = passScheduleStore.loadFromLocalStorage()
    if (restored) {
      console.log('✅ localStorage 데이터 복원 완료')
    }

    // ✅ 차트는 즉시 초기화 (서버 연결과 무관)
    void nextTick(() => {
      try {
        initChart()
        console.log('✅ 차트 즉시 초기화 완료')

        // ✅ 복원된 데이터가 있으면 차트에 반영
        if (restored) {
          restoreChartData()
        }

        // 차트 업데이트 타이머 시작
        if (updateTimer) {
          clearInterval(updateTimer)
        }
        updateTimer = window.setInterval(() => {
          try {
            updateChart()
          } catch (timerError) {
            console.error('차트 업데이트 타이머 오류:', timerError)
          }
        }, 100)
      } catch (chartError) {
        console.error('차트 초기화 오류:', chartError)
      }
    })

    // ✅ 서버 데이터 로드는 "Select Schedule" 버튼 클릭 시에만 수행
    // selectScheduleData()에서 모달을 열 때 데이터가 없으면 로드하도록 처리
    // 페이지 접근 시 불필요한 API 호출 방지
  } catch (error) {
    console.error('PassSchedulePage 마운트 중 오류:', error)
  }
})

// 컴포넌트 언마운트
onUnmounted(() => {
  console.log('PassSchedulePage 컴포넌트 언마운트됨')

  // 🆕 차트 업데이트 타이머 정리 (기존 타이머가 있을 때만)
  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
    console.log('✅ 차트 업데이트 타이머 정리됨')
  }

  // ✅ 차트는 유지 (dispose하지 않음) - keep-alive나 재마운트 시 재사용
  // 차트는 onDeactivated에서도 유지되므로 여기서도 유지
  // 실제로 컴포넌트가 완전히 제거될 때만 dispose (일반적으로 발생하지 않음)
  // if (passChart && !passChart.isDisposed()) {
  //   passChart.dispose()
  //   passChart = null
  //   console.log('✅ PassSchedule 차트 인스턴스 정리됨')
  // }

  // 🆕 시간 업데이트 타이머 정리
  stopTimeTimer()

  // 🆕 추적 경로 데이터는 유지 (Store에서 관리)
  // passScheduleStore.clearTrackingPaths() 제거

  // 🆕 이벤트 리스너 정리
  if (chartResizeHandler) {
    window.removeEventListener('resize', chartResizeHandler)
    chartResizeHandler = null
  }

  // ✅ 저장 타이머 정리
  if (saveTimeout) {
    clearTimeout(saveTimeout)
    saveTimeout = null
  }

  // ✅ 마지막 저장 실행
  passScheduleStore.saveToLocalStorage()

  console.log('✅ PassSchedulePage 정리 완료 (차트는 유지)')
})

// ✅ formattedCalTime은 useOffsetControls composable에서 가져옴
</script>

<style scoped>
/* 모든 간격이 동적으로 조정되는 반응형 레이아웃 */
.flexible-offset-layout {
  display: flex;
  align-items: stretch;
  justify-content: center;
  width: 100%;
  gap: 40px;
  row-gap: 8px;
  flex-wrap: wrap;
}

/* 개별 Offset 그룹 - Elevation, Tilt, Time은 좌측 공간 축소 */
.offset-group {
  flex: none;
  min-width: 0;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.01);
  display: flex;
  align-items: center;
}

/* ✅ 간격 통일 - padding-left 제거하고 gap만으로 간격 관리 */

/* 라벨 스타일 */
.position-offset-label {
  min-width: 80px;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(25, 118, 210, 0.1);
  border: 1px solid rgba(25, 118, 210, 0.3);
}

/* Cal Time 필드 스타일 - 확보된 공간 활용 */
.cal-time-field {
  flex-shrink: 0;
  min-width: 190px;
}

/* 반응형 동작 - 1900px 기준으로 줄바꿈 */
@media (max-width: 1900px) {
  .flexible-offset-layout {
    flex-wrap: wrap;
    gap: 20px;
    row-gap: 8px;
    justify-content: center;
  }

  .offset-group {
    flex: none;
    min-width: 0;
    padding: 8px;
  }

  .position-offset-label {
    min-width: 70px;
    font-size: 0.8rem;
  }

  .cal-time-field {
    min-width: 180px;
    max-width: 200px;
  }
}

@media (min-width: 1901px) {
  .flexible-offset-layout {
    flex-wrap: nowrap;
    gap: 40px;
    justify-content: center;
  }

  .offset-group {
    flex: none;
    min-width: 0;
  }

  .position-offset-label {
    min-width: 80px;
    font-size: 0.875rem;
  }
}

/* ✅ 오프셋 컨트롤 행 - EphemerisDesignationPage와 동일하게 설정 */
.pass-schedule-mode .offset-control-row {
  margin-bottom: 0.5rem !important;
  position: relative;
  z-index: 100;
}

/* ✅ 메인 콘텐츠 행 하단 여백을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.pass-schedule-mode .main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
}

/* ✅ Quasar q-col-gutter-md가 행에 추가하는 하단 마진을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.pass-schedule-mode .main-content-row.q-col-gutter-md,
.pass-schedule-mode .row.q-col-gutter-md.main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ Quasar row 기본 스타일 오버라이드 (더 강력한 선택자) - EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.pass-schedule-mode .main-content-row.row,
.pass-schedule-mode .row.main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ 1단계: pass-schedule-mode와 부모 요소의 하단 여백 완전 제거 */
/* router-view, q-page-container 내부의 pass-schedule-mode 하단 여백 제거 */
router-view .pass-schedule-mode,
q-page-container .pass-schedule-mode,
q-page .pass-schedule-mode,
.pass-schedule-mode,
[class*="pass-schedule-mode"],
div.pass-schedule-mode {
  height: auto !important;
  /* ✅ height: 100% 제거하여 내용에 맞게 조정 */
  width: 100%;
  padding: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 상단 패딩 제거 */
  margin: 0 !important;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
  /* ✅ min-height는 공통 CSS의 var(--theme-layout-modePageMinHeight, 500px) 사용 */
  max-height: none !important;
  /* ✅ 최대 높이 제거 */
  display: flex !important;
  /* ✅ flexbox로 변경 */
  flex-direction: column !important;
  /* ✅ 세로 방향 */
  gap: 0 !important;
  /* ✅ flex gap 제거 */
  row-gap: 0 !important;
  /* ✅ flex row-gap 제거 */
  column-gap: 0 !important;
  /* ✅ flex column-gap 제거 */
}

/* router-view, q-page-container의 하단 패딩/마진 제거 */
router-view,
q-page-container,
router-view>*,
q-page-container>* {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* router-view 내부의 모든 요소 하단 여백 제거 */
router-view .pass-schedule-mode,
q-page-container .pass-schedule-mode {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode 내부의 마지막 요소 하단 여백을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.pass-schedule-mode>*:last-child {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode 내부의 모든 직접 자식 요소 하단 여백 제거 */
.pass-schedule-mode>* {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row가 pass-schedule-mode의 마지막 자식일 때 하단 여백 완전 제거 */
.pass-schedule-mode>.main-content-row:last-child,
.pass-schedule-mode>.row.main-content-row:last-child,
.pass-schedule-mode>div.main-content-row:last-child,
.pass-schedule-mode>.main-content-row,
.pass-schedule-mode>.row.main-content-row,
.pass-schedule-mode>div.main-content-row {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
  margin-top: 0 !important;
  padding-top: 0 !important;
}

/* ✅ pass-schedule-mode의 마지막 div 요소 하단 여백 완전 제거 (더 강력한 선택자) */
.pass-schedule-mode>div:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode의 마지막 row 요소 하단 여백 완전 제거 */
.pass-schedule-mode>.row:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode의 모든 직접 자식 row 요소 하단 여백 제거 */
.pass-schedule-mode>.row {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode의 모든 직접 자식 div 요소 하단 여백 제거 */
.pass-schedule-mode>div {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ 외각 공간 제어 - 단순화 */
.pass-schedule-mode .schedule-container {
  padding: 0;
  width: 100%;
  height: 100%;
  margin: 0;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
  margin-bottom: 1rem;
}

/* ===== 2. 컨트롤 섹션 기본 스타일 ===== */
.control-section {
  height: 100%;
  max-height: 500px;
  width: 100%;
  background-color: var(--theme-card-background);
  /* ✅ border, border-radius, box-shadow는 mode-common.scss에서 통일 관리 */
  /* ✅ EphemerisDesignationPage.vue와 동일한 높이를 위해 flex 추가 - 내부 구성 변경 없음 */
  display: flex;
  flex-direction: column;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
}

/* ✅ main-content-row 내부의 모든 컬럼 하단 여백 완전 제거 */
.pass-schedule-mode .main-content-row>[class*="col-"] {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 하단 여백 완전 제거 (더 구체적인 선택자) */
.pass-schedule-mode .main-content-row>[class*="col-"]:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card 하단 여백 제거 */
.pass-schedule-mode .main-content-row>[class*="col-"] .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card 하단 여백 제거 (더 구체적인 선택자) */
.pass-schedule-mode .main-content-row>[class*="col-"]:last-child .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card-section 하단 여백 제거 */
.pass-schedule-mode .main-content-row>[class*="col-"] .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card-section 하단 여백 제거 (더 구체적인 선택자) */
.pass-schedule-mode .main-content-row>[class*="col-"]:last-child .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 Quasar q-card 하단 마진/패딩 완전 제거 */
.pass-schedule-mode .main-content-row .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 control-section 하단 여백 제거 */
.pass-schedule-mode .main-content-row .control-section {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ Position View 카드 높이 제한 */
.pass-schedule-mode .control-section.position-view-card,
.pass-schedule-mode .control-section.position-view-card.q-card {
  min-height: 360px !important;
  /* ✅ 최소 높이 보장 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 (다른 패널과 동일하게) */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Position View 카드 섹션 높이 조정 */
.pass-schedule-mode .control-section.position-view-card .q-card-section.position-view-section {
  min-height: 360px !important;
  /* ✅ 차트 영역 최소 높이 보장 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 (다른 패널과 동일하게) */
  flex: 1 !important;
  /* ✅ 남은 공간 채우기 */
  display: flex !important;
  flex-direction: column !important;
}

.control-section .q-card-section {
  padding: 16px !important;
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 (상단 공간과 동일하게) */
  /* ✅ 남은 공간을 채우도록 flex 추가 - 내부 구성 변경 없음 */
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  /* ✅ 제목 absolute positioning을 위한 기준점 */
}

/* ✅ Schedule Information 카드 높이를 Position View와 동일하게 설정 (360px) */
.pass-schedule-mode .main-content-row>[class*="col-"]:nth-child(2) .control-section,
.pass-schedule-mode .main-content-row>[class*="col-"]:nth-child(2) .control-section.q-card {
  min-height: 360px !important;
  /* ✅ 최소 높이 보장 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Schedule Information 카드 섹션 높이 조정 */
.pass-schedule-mode .main-content-row>[class*="col-"]:nth-child(2) .control-section .q-card-section {
  min-height: 360px !important;
  /* ✅ 최소 높이 보장 */
  flex: 1 !important;
  /* ✅ 남은 공간 채우기 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Schedule Control 카드 높이를 Position View와 동일하게 설정 (367px) */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section,
.pass-schedule-mode .main-content-row .schedule-control-col .control-section.q-card {
  min-height: 367px !important;
  /* ✅ 최소 높이 보장 - EphemerisDesignationPage.vue와 동일 (367px) */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Schedule Control 카드 섹션 높이 조정 */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section {
  min-height: 367px !important;
  /* ✅ 최소 높이 보장 - EphemerisDesignationPage.vue와 동일 (367px) */
  flex: 1 1 auto !important;
  /* ✅ 남은 공간 채우기 (flex-grow: 1, flex-shrink: 1, flex-basis: auto) */
  display: flex !important;
  flex-direction: column !important;
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 완전 제거 (상단 공간과 동일하게) */
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  overflow: hidden !important;
  /* ✅ 하단 여백 방지 */
  justify-content: flex-start !important;
  /* ✅ 상단 정렬로 하단 여백 제거 */
}

/* ✅ 3단계: schedule-control-section 내부의 button-group 하단 여백 완전 제거 (더 구체적인 선택자) */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
  flex-shrink: 0 !important;
  /* ✅ 버튼 그룹이 축소되지 않도록 */
}

/* ✅ schedule-control-section 내부의 마지막 요소 하단 여백 완전 제거 */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section>*:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ schedule-control-section 내부의 모든 직접 자식 요소 하단 여백 제거 */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section>* {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

.position-view-section {
  padding: 16px 16px 0px 16px !important;
  /* ✅ 상단 패딩을 다른 패널과 동일하게 16px로 맞춤, 하단 패딩 제거 */
}

.position-view-title {
  position: absolute;
  /* ✅ 제목을 absolute로 배치하여 차트 영역이 전체 공간 사용 */
  top: 16px;
  left: 16px;
  z-index: 10;
  margin: 0;
  padding: 0;
}

.chart-area {
  min-height: 340px !important;
  /* ✅ 최소 높이 보장 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 */
  flex: 1 !important;
  /* ✅ 남은 공간 채우기 */
  width: 100%;
  display: flex;
  align-items: center;
  /* ✅ 중앙 정렬 */
  justify-content: center;
  margin: 0 auto;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  padding: 0 !important;
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
  box-sizing: border-box;
  overflow: visible !important;
  /* ✅ 차트가 넘쳐도 보이도록 변경 */
  text-align: center;
  position: relative;
}

/* ✅ 차트 컨테이너 - 차트를 더 크게 (Position View 구역 크기와 독립적) */
.chart-area>div {
  position: absolute !important;
  left: 50% !important;
  top: 50% !important;
  /* ✅ 중앙 정렬 */
  transform: translate(-50%, -50%) !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
  /* ✅ 차트를 더 크게 설정 (Position View 구역 크기와 독립적) */
  width: 500px !important;
  height: 500px !important;
  max-width: 500px !important;
  max-height: 500px !important;
  min-width: 500px !important;
  min-height: 500px !important;
  aspect-ratio: 1 !important;
  /* ✅ 정사각형 유지 */
}

/* 반응형 차트 크기 조정 - 차트를 더 크게 (Position View 구역 크기와 독립적) */
@media (max-width: 1900px) {
  .chart-area>div {
    width: 500px !important;
    height: 500px !important;
    max-width: 500px !important;
    max-height: 500px !important;
    min-width: 500px !important;
    min-height: 500px !important;
    top: 50% !important;
    /* ✅ 중앙 정렬 */
  }
}

@media (max-width: 1600px) {
  .chart-area>div {
    width: 470px !important;
    height: 470px !important;
    max-width: 470px !important;
    max-height: 470px !important;
    min-width: 470px !important;
    min-height: 470px !important;
    top: 50% !important;
    /* ✅ 중앙 정렬 */
  }
}

@media (max-width: 1200px) {
  .chart-area>div {
    width: 420px !important;
    height: 420px !important;
    max-width: 420px !important;
    max-height: 420px !important;
    min-width: 420px !important;
    min-height: 420px !important;
    top: 50% !important;
    /* ✅ 중앙 정렬 */
  }
}

/* ===== 4. 컨트롤 카드 스타일 ===== */
.control-card {
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
}

/* ✅ 컴팩트 컨트롤 스타일 - 정리 */
.pass-schedule-mode .compact-control {
  padding: 0 8px;
  margin: 0;
  min-height: auto;
  height: auto;
  line-height: 1;
  vertical-align: top;
}

.pass-schedule-mode .compact-control .q-input {
  margin-bottom: 0.25rem;
}

.pass-schedule-mode .compact-control .q-btn {
  min-height: 2rem;
  padding: 0.25rem;
}

/* ✅ 레이아웃 정렬 스타일 - 정리 */
.pass-schedule-mode .align-center {
  align-items: center;
}

.pass-schedule-mode .justify-end {
  justify-content: flex-end;
}

.pass-schedule-mode .justify-center {
  justify-content: center;
}

/* ✅ 컴팩트 컨트롤 레이아웃 */
.pass-schedule-mode .compact-control .row {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  width: 100%;
}

.pass-schedule-mode .compact-control .q-field {
  margin-bottom: 0;
}

.pass-schedule-mode .compact-control .col-auto {
  flex-shrink: 0;
}

/* ✅ 세부 레이아웃 스타일 - 정리 */
.pass-schedule-mode .compact-control .row .row {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 0.25rem;
}

.pass-schedule-mode .compact-control .text-subtitle2 {
  display: flex;
  align-items: center;
  height: 100%;
  margin: 0;
  padding: 0;
  font-size: 0.9rem;
  white-space: nowrap;
}

.pass-schedule-mode .compact-control .col-1 {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-width: fit-content;
}

/* ✅ 입력 필드 스타일 - 통일 */
.pass-schedule-mode .offset-input {
  width: 110px;
  min-width: 110px;
  max-width: 110px;
}

.pass-schedule-mode .cal-time-field {
  min-width: 190px;
  max-width: 220px;
}

/* ✅ 새로운 세로 버튼 레이아웃 */
.vertical-button-group {
  display: flex !important;
  align-items: center !important;
  gap: 4px !important;
}

.vertical-buttons {
  display: flex !important;
  flex-direction: column !important;
  gap: 2px !important;
}

/* ✅ 방법 1: 왼쪽 세로 라벨 (카드 안) - 높이 최적화 */
.position-offset-label {
  background: linear-gradient(135deg, rgba(25, 118, 210, 0.15) 0%, rgba(25, 118, 210, 0.08) 100%);
  padding: 4px 8px;
  border-radius: 6px;
  border-right: 3px solid var(--q-primary);
  min-width: 50px;
  margin-right: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  line-height: 1.2 !important;
}

/* ✅ 카드 테두리 위아래 패딩 완전 제거 - 더 구체적인 셀렉터 */
.q-card.control-card .q-card-section.compact-control {
  padding: 0px 8px !important;
}

/* ✅ 추가적인 강제 적용 */
.q-card-section.compact-control {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
}

/* ✅ 더 강력한 강제 적용 - 모든 가능한 셀렉터 */
.q-card-section.compact-control.purple-1,
.q-card.control-card .q-card-section.compact-control.purple-1,
.q-card-section[class*="compact-control"],
.q-card-section[class*="purple-1"] {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
  margin-top: 0px !important;
  margin-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
  display: flex !important;
  align-items: flex-start !important;
}

/* ✅ Quasar 기본 스타일 덮어쓰기 - 1행 offset control 카드에만 적용 */
.pass-schedule-mode .q-card.control-card .q-card-section {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

/* ✅ 2행 control-section 카드는 기본 패딩 유지 */
.pass-schedule-mode .control-section .q-card-section {
  padding: 16px !important;
}

/* ✅ 카드 자체 마진도 제거 */
.pass-schedule-mode .q-card.control-card {
  margin-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

.pass-schedule-mode .q-card.control-card .q-card__section {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

/* ✅ 추가 높이 줄이기 - 모든 요소의 높이 최소화 */
.pass-schedule-mode .q-input {
  min-height: auto !important;
}

.pass-schedule-mode .q-field__control {
  min-height: auto !important;
}

.pass-schedule-mode .q-field__native {
  padding: auto !important;
}

.pass-schedule-mode .q-btn {
  min-height: auto !important;
}

.pass-schedule-mode .q-btn--dense {
  min-height: auto !important;
}

/* ✅ 라벨 테두리 높이만 줄이기 - 내부 구성은 유지 */
.pass-schedule-mode .position-offset-label {
  padding: 4px 8px !important;
  min-width: 50px !important;
  border-right: 1px solid var(--q-primary) !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) !important;
}

/* ✅ 간격 제거로 더 타이트하게 */
.pass-schedule-mode .compact-control .row.q-gutter-none {
  margin: 0 !important;
  padding: 0 !important;
}

.pass-schedule-mode .compact-control .row.q-gutter-none>div {
  padding-left: 0.25rem !important;
  padding-right: 0.25rem !important;
}

.pass-schedule-mode .compact-control .row.q-gutter-none>div:first-child {
  padding-left: 0 !important;
}

.pass-schedule-mode .compact-control .row.q-gutter-none>div:last-child {
  padding-right: 0 !important;
}

/* ===== 5. 컴팩트 컨트롤 행 스타일 ===== */
.compact-control-row {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-height: 48px;
}

/* 데스크톱에서 확실히 가로 배치 유지 */
@media (min-width: 768px) {
  .compact-control-row {
    flex-direction: row !important;
    align-items: center !important;
    gap: 6px !important;
  }
}

/* Input 필드 스타일 */
.control-input {
  flex: 1;
  min-width: 80px;
  max-width: 120px;
}

.control-input :deep(.q-field__control) {
  height: 40px;
}

/* 데스크톱에서 입력 필드 크기 고정 */
@media (min-width: 768px) {
  .control-input {
    flex: 1 !important;
    min-width: 80px !important;
    max-width: 120px !important;
  }
}

/* 버튼 그룹 스타일 */
.control-buttons {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
  width: 32px;
}

.control-buttons .q-btn {
  min-width: 32px !important;
  width: 32px !important;
  height: 19px;
  padding: 0;
  flex-shrink: 0;
}

/* 리셋 버튼 스타일 */
.reset-button {
  min-width: 32px !important;
  width: 32px !important;
  height: 40px !important;
  flex-shrink: 0 !important;
}

/* 데스크톱에서 리셋 버튼 크기 고정 */
@media (min-width: 768px) {
  .reset-button {
    min-width: 32px !important;
    width: 32px !important;
    height: 40px !important;
    flex-shrink: 0 !important;
  }
}

/* Output 필드 스타일 */
.output-input-small {
  flex: 1;
  min-width: 80px;
  max-width: 120px;
}

.output-input-small :deep(.q-field__control) {
  height: 40px;
}

/* ===== 6. Time 컨트롤 특별 스타일 ===== */
.time-output-section {
  flex: 2;
  display: flex;
  gap: 4px;
  align-items: center;
}

.time-output-section .output-input {
  flex: 1;
  min-width: 80px;
  max-width: 120px;
}

.time-output-section .output-input :deep(.q-field__control) {
  height: 40px;
}

.time-output-section .cal-time-input {
  flex: 1.5;
  min-width: 140px;
}

.time-output-section .cal-time-input :deep(.q-field__control) {
  height: 40px;
}

.time-output-section .cal-time-input :deep(.q-field__control input) {
  font-size: 11px;
  font-family: 'Courier New', monospace;
}

/* ===== 7. 스케줄 정보 섹션 스타일 ===== */
.schedule-form {
  margin-top: 0.5rem;
  width: 100%;
  margin-bottom: 0;
  /* ✅ 하단 마진 제거 */
  flex: 1;
  /* ✅ 남은 공간을 채워서 하단 정렬 */
  display: flex;
  flex-direction: column;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  /* ✅ gap 줄임 (0.5rem → 0.25rem) */
  width: 100%;
  flex: 1;
  /* ✅ 남은 공간을 채워서 하단 정렬 */
  justify-content: flex-start;
  /* ✅ 상단부터 시작 */
}

.schedule-info {
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px 16px 8px 16px;
  /* ✅ 하단 패딩 줄임 (16px → 8px) */
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex: 1;
  /* ✅ 남은 공간을 채워서 하단 정렬 */
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  /* ✅ 상단부터 시작 */
}

.no-schedule-selected {
  padding: 2rem;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.02);
  /* ✅ flex: 1 제거 - EphemerisDesignationPage.vue 기준으로 높이 맞추기 */
  display: flex;
  align-items: center;
  justify-content: center;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  /* ✅ 패딩 줄임 (8px → 6px) */
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-row:last-child {
  border-bottom: none;
  margin-bottom: auto;
  /* ✅ 마지막 행 아래에 자동 여백 추가하여 하단 정렬 */
}

.info-label {
  font-weight: 500;
  color: var(--theme-info-light);
  min-width: 120px;
}

.info-value {
  font-weight: 400;
  color: var(--theme-text);
  text-align: right;
  flex: 1;
}

.info-value-with-badge {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* ===== 8. 스케줄 헤더 스타일 ===== */
.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.schedule-header-title {
  line-height: 1.2;
  margin: 0;
  padding: 0;
}

.schedule-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.current-schedule-display {
  display: flex;
  align-items: center;
  padding: 2px 6px;
  background-color: rgba(25, 118, 210, 0.1);
  border-radius: 4px;
  border: 1px solid rgba(25, 118, 210, 0.3);
}

.registered-schedule-info {
  background-color: rgba(0, 0, 0, 0.8);
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.registered-schedule-info .text-body2 {
  margin-bottom: 0;
  font-weight: 600;
  font-size: 11px;
  color: var(--theme-info);
  line-height: 1.2;
}

.registered-schedule-info .text-caption {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  line-height: 1.2;
}

/* ===== 9. 테이블 기본 스타일 ===== */
.schedule-table {
  background-color: var(--theme-card-background);
  color: white;
  /* ✅ flex: 1 제거 - EphemerisDesignationPage.vue 기준으로 높이 맞추기 */
  border-radius: 6px;
  overflow: hidden;
  height: 210px !important;
  /* ✅ 높이 고정 (정확히 3개 행만 보이도록) */
  max-height: 210px !important;
  /* ✅ 최대 높이 고정 */
}

/* Quasar 테이블 기본 설정 초기화 */
.schedule-table :deep(.q-table__container) {
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  height: 210px !important;
  /* ✅ 높이 고정 (정확히 3개 행만 보이도록) */
  max-height: 210px !important;
  /* ✅ 최대 높이 고정 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* ✅ 컨테이너는 스크롤 없음 */
}

/* ✅ 테이블 바디 영역만 스크롤 가능하도록 설정 */
.schedule-table :deep(.q-table__middle) {
  flex: 1;
  overflow-y: auto;
  /* ✅ 세로 스크롤 가능 */
  overflow-x: auto;
  /* ✅ 가로 스크롤도 가능 */
  min-height: 0;
}

.schedule-table :deep(.q-table__top) {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.schedule-table :deep(.q-table__bottom) {
  display: none !important;
}

.schedule-table :deep(.q-table__control) {
  display: none !important;
}

/* ✅ 테이블 헤더 고정 (Sticky Header) */
.schedule-table :deep(.q-table thead) {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: var(--theme-card-background);
}

/* 테이블 헤더 스타일 */
.schedule-table :deep(.q-table thead th) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  font-size: 12px;
  padding: 8px 20px 8px 6px;
  border-bottom: 2px solid rgba(255, 255, 255, 0.1);
  text-align: center !important;
  vertical-align: middle !important;
  white-space: pre-line;
  line-height: 1.2;
  height: 50px !important;
  position: sticky;
  top: 0;
  z-index: 10;
}

/* 헤더 정렬 요소를 flexbox로 분리 배치 */
.schedule-table :deep(.q-table thead th .q-table__sort) {
  display: flex !important;
  justify-content: space-between !important;
  align-items: center !important;
  width: 100% !important;
  height: 100% !important;
}

/* 헤더 텍스트 부분 가운데 정렬 */
.schedule-table :deep(.q-table thead th .q-table__sort > span) {
  flex: 1 !important;
  text-align: center !important;
  white-space: pre-line !important;
  line-height: 1.2 !important;
}

/* 정렬 아이콘을 우측에 고정 배치 */
.schedule-table :deep(.q-table thead th .q-table__sort-icon) {
  position: absolute !important;
  right: 4px !important;
  top: 50% !important;
  transform: translateY(-50%) !important;
  flex-shrink: 0 !important;
  margin: 0 !important;
}





/* 테이블 바디 기본 스타일 */
.schedule-table :deep(.q-table tbody) {
  background-color: transparent;
}

/* ===== 10. 테이블 행 기본 스타일 ===== */
.schedule-table :deep(.q-table tbody tr) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.2s ease;
  cursor: pointer;
}

/* 기본 호버 효과 */
.schedule-table :deep(.q-table tbody tr:hover) {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

/* 짝수 행 스타일 제거 (Quasar 기본값 오버라이드) */
.schedule-table :deep(.q-table tbody tr:nth-child(even)) {
  background-color: transparent;
}

/* 테이블 셀 기본 스타일 */
.schedule-table :deep(.q-table tbody td) {
  padding: 8px 6px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  border-right: 1px solid rgba(255, 255, 255, 0.04);
  vertical-align: middle;
  text-align: center;
}

.schedule-table :deep(.q-table tbody td:last-child) {
  border-right: none;
}

/* 행 선택 효과 */
.schedule-table :deep(.q-table tbody tr.selected) {
  background-color: rgba(33, 150, 243, 0.1) !important;
  border-left: 3px solid var(--theme-info);
}

/* ===== 11. 하이라이트 스타일 (최고 우선순위) ===== */

/* 현재 추적 중인 스케줄 하이라이트 */
.schedule-table :deep(.q-table tbody tr.current-tracking-row) {
  background-color: var(--theme-positive-bg) !important;
  border-left: 4px solid var(--theme-positive) !important;
  color: var(--theme-positive-dark) !important;
}

.schedule-table :deep(.q-table tbody tr.current-tracking-row td) {
  background-color: var(--theme-positive-bg) !important;
  color: var(--theme-positive-dark) !important;
  font-weight: 500;
}

.schedule-table :deep(.q-table tbody tr.current-tracking-row:hover) {
  background-color: var(--theme-positive-hover) !important;
}

.schedule-table :deep(.q-table tbody tr.current-tracking-row:hover td) {
  background-color: var(--theme-positive-hover) !important;
}

/* 다음 예정 스케줄 하이라이트 */
.schedule-table :deep(.q-table tbody tr.next-tracking-row) {
  background-color: var(--theme-info-bg) !important;
  border-left: 4px solid var(--theme-info) !important;
  color: var(--theme-info-dark) !important;
}

.schedule-table :deep(.q-table tbody tr.next-tracking-row td) {
  background-color: var(--theme-info-bg) !important;
  color: var(--theme-info-dark) !important;
  font-weight: 500;
}

.schedule-table :deep(.q-table tbody tr.next-tracking-row:hover) {
  background-color: var(--theme-info-hover) !important;
}

.schedule-table :deep(.q-table tbody tr.next-tracking-row:hover td) {
  background-color: var(--theme-info-hover) !important;
}

/* 테스트용 첫 번째 행 하이라이트 */
.schedule-table :deep(.q-table tbody tr.highlight-first-row) {
  background-color: var(--theme-warning-bg) !important;
  color: #000 !important;
  border-left: 4px solid var(--theme-warning) !important;
}

.schedule-table :deep(.q-table tbody tr.highlight-first-row td) {
  background-color: var(--theme-warning-bg) !important;
  color: #000 !important;
  font-weight: 600;
}

.schedule-table :deep(.q-table tbody tr.highlight-first-row:hover) {
  background-color: var(--theme-warning-hover) !important;
}

.schedule-table :deep(.q-table tbody tr.highlight-first-row:hover td) {
  background-color: var(--theme-warning-hover) !important;
  /* ===== 12. 테이블 컬럼별 특별 스타일 ===== */

  /* 위성 정보 컬럼 */
  .satellite-info-cell {
    padding: 8px 6px !important;
    min-width: 100px;
  }

  .satellite-container {
    display: flex;
    flex-direction: column;
    gap: 3px;
    align-items: flex-start;
  }

  .satellite-id {
    font-weight: 600;
    font-size: 11px;
    color: var(--theme-info);
    line-height: 1.2;
  }

  .satellite-name {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.8);
    font-weight: 400;
    line-height: 1.2;
    word-break: break-word;
  }

  /* 시간 범위 컬럼 */
  .time-range-cell {
    padding: 8px 6px !important;
    min-width: 130px;
  }

  .time-container {
    display: flex;
    flex-direction: column;
    gap: 3px;
    align-items: flex-start;
  }

  .start-time,
  .end-time {
    font-size: 10px;
    font-weight: 500;
    line-height: 1.2;
    font-family: 'Courier New', monospace;
  }

  .start-time {
    color: var(--theme-positive);
  }

  .end-time {
    color: var(--theme-warning);
  }
}

/* ===== 12. 테이블 컬럼별 특별 스타일 ===== */

/* 위성 정보 컬럼 */
.satellite-info-cell {
  padding: 8px 6px !important;
  min-width: 100px;
  text-align: center;
}

.satellite-container {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
}

.satellite-id {
  font-weight: 600;
  font-size: 14px;
  color: var(--theme-info);
  line-height: 1.2;
}

.satellite-name {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 400;
  line-height: 1.2;
  word-break: break-word;
}

/* 시간 범위 컬럼 */
.time-range-cell {
  padding: 8px 6px !important;
  min-width: 130px;
  text-align: center;
}

.time-container {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
}

.start-time,
.end-time {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
  font-family: 'Courier New', monospace;
}

.start-time {
  color: var(--theme-positive);
}

.end-time {
  color: var(--theme-warning);
}

/* ===== 12.5. 스케줄 하이라이트 스타일 ===== */

/* 현재 추적 중인 스케줄 하이라이트 - 더 강력한 선택자 */
.schedule-table tbody tr.highlight-current-schedule {
  background-color: rgba(27, 94, 32, 0.92) !important;
  border-left: 4px solid var(--theme-positive-light) !important;
  color: var(--theme-positive-light) !important;
}

.schedule-table tbody tr.highlight-current-schedule td {
  background-color: transparent !important;
  color: inherit !important;
  font-weight: 600 !important;
}

.schedule-table tbody tr.highlight-current-schedule * {
  background-color: transparent !important;
  color: inherit !important;
}

.schedule-table tbody tr.highlight-current-schedule .q-btn,
.schedule-table tbody tr.highlight-current-schedule .q-icon {
  background-color: transparent !important;
}

.schedule-table tbody tr.highlight-current-schedule:hover td {
  background-color: rgba(46, 125, 50, 0.95) !important;
}

.schedule-table tbody tr.highlight-next-schedule {
  background-color: rgba(13, 71, 161, 0.9) !important;
  border-left: 4px solid var(--theme-info-light) !important;
  color: var(--theme-info-light) !important;
}

.schedule-table tbody tr.highlight-next-schedule td {
  background-color: transparent !important;
  color: inherit !important;
  font-weight: 600 !important;
}

.schedule-table tbody tr.highlight-next-schedule * {
  background-color: transparent !important;
  color: inherit !important;
}

.schedule-table tbody tr.highlight-next-schedule .q-btn,
.schedule-table tbody tr.highlight-next-schedule .q-icon {
  background-color: transparent !important;
}

.schedule-table tbody tr.highlight-next-schedule:hover td {
  background-color: rgba(25, 118, 210, 0.92) !important;
}



/* ===== 13. Azimuth/Elevation 컬럼 스타일 ===== */

/* Azimuth 범위 컬럼 */
.azimuth-range-cell {
  padding: 8px 6px !important;
  vertical-align: middle !important;
  min-width: 80px;
  text-align: center;
}

.azimuth-container {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
  justify-content: center;
  min-height: 35px;
}

.start-az,
.end-az {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
  font-family: 'Courier New', monospace;
}

.start-az {
  color: var(--theme-positive);
}

.end-az {
  color: var(--theme-warning);
}

/* Elevation 정보 컬럼 */
.elevation-info-cell {
  padding: 8px 6px !important;
  vertical-align: middle !important;
  min-width: 70px;
  text-align: center;
}

.elevation-container {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
  justify-content: center;
  min-height: 35px;
}

.max-elevation,
.train {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
  font-family: 'Courier New', monospace;
}

.max-elevation {
  color: var(--theme-accent);
}

.train {
  color: var(--theme-text-muted);
}

/* ===== 14. 버튼 그룹 스타일 ===== */
.button-group {
  margin-top: 0.25rem;
  /* ✅ 상단 마진 줄임 (0.5rem → 0.25rem) */
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 완전 제거 */
  width: 100%;
  flex-shrink: 0;
  padding-top: 0.25rem;
  /* ✅ 상단 패딩 줄임 (0.5rem → 0.25rem) */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 완전 제거 */
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  box-sizing: border-box !important;
  display: flex !important;
  /* ✅ flex 컨테이너로 명시 */
  flex-direction: column !important;
  /* ✅ 세로 방향 */
  gap: 0.5rem !important;
  /* ✅ 버튼 행 사이 간격 명시 (겹침 방지) */
}

.button-row {
  display: flex;
  gap: 0.5rem;
  width: 100%;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 완전 제거 (상단 공간과 동일하게) */
  flex-shrink: 0 !important;
  /* ✅ 버튼 행이 축소되지 않도록 */
}

/* ✅ Quasar q-mb-sm 클래스 오버라이드 (button-row에 적용된 경우) */
.button-row.q-mb-sm,
.schedule-control-section .button-row.q-mb-sm {
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 완전 제거 */
}

.control-button-row {
  display: flex;
  gap: 0.5rem;
  width: 100%;
  flex-shrink: 0 !important;
  /* ✅ 버튼 행이 축소되지 않도록 */
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
}

/* 업로드 버튼 스타일 - 크기는 유지하고 폰트만 확대 */
.upload-btn {
  flex: 1;
  min-width: 0;
  height: 36px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.pass-schedule-mode .button-group :deep(.upload-btn .q-btn__content) {
  font-size: 13px;
  line-height: 1.2;
}

.pass-schedule-mode :deep(.control-btn .q-btn__content) {
  font-size: 13px;
  line-height: 1.2;
}

.upload-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* 컨트롤 버튼 스타일 - 크기는 유지하고 폰트만 확대 */
.control-btn {
  flex: 1;
  min-width: 0;
  height: 32px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.control-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
}

/* 디버그 버튼 스타일 */
.debug-buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 0.5rem;
  padding: 0.5rem;
  background-color: rgba(255, 255, 255, 0.02);
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.debug-buttons .q-btn {
  font-size: 11px;
  height: 28px;
  padding: 0 8px;
}

/* ===== 15. 디버그 패널 스타일 ===== */
.debug-panel {
  background-color: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.3);
  border-radius: 4px;
}

.debug-panel .q-card-section {
  padding: 8px 12px;
}

.debug-panel .text-caption {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  font-family: 'Courier New', monospace;
  line-height: 1.4;
}

.debug-panel .text-caption strong {
  color: var(--theme-warning);
  font-weight: 600;
}

/* 현재 스케줄 상태 표시 */
.current-schedule-status {
  background-color: rgba(0, 0, 0, 0.3);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.current-schedule-status .q-card-section {
  padding: 12px 16px;
}

.current-schedule-status .row {
  align-items: center;
}

.current-schedule-status .q-icon {
  margin-right: 8px;
}

.current-schedule-status .text-body2 {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.current-schedule-status .q-badge {
  font-size: 10px;
  font-weight: 600;
}

/* ===== 16. 반응형 디자인 ===== */

/* 태블릿 크기 (1024px 이하) */
@media (max-width: 1023px) {
  .control-section {
    height: auto;
    min-height: 400px;
  }

  .chart-area {
    height: 300px;
  }

  /* 오프셋 컨트롤을 2x2로 배치 */
  .row:first-of-type .col-sm-3 {
    flex: 0 0 50%;
    max-width: 50%;
  }

  .schedule-container {
    padding: 0.5rem;
  }

  .button-row,
  .control-button-row {
    flex-direction: column;
    gap: 0.5rem;
  }

  .upload-btn,
  .control-btn {
    width: 100%;
    height: 44px;
  }
}

/* 모바일 크기 (768px 이하) */
@media (max-width: 767px) {
  .pass-schedule-mode {
    padding: 0.25rem !important;
    /* ✅ 모바일에서만 패딩 적용 */
    padding-bottom: 0 !important;
    /* ✅ 하단 패딩은 여전히 제거 */
  }

  .schedule-container {
    padding: 0.25rem;
  }

  /* 오프셋 컨트롤을 세로로 배치 */
  .row:first-of-type .col-sm-3 {
    flex: 0 0 100%;
    max-width: 100%;
    margin-bottom: 0.5rem;
  }

  .control-section {
    height: auto;
    min-height: 300px;
  }

  .chart-area {
    height: 250px;
  }

  .schedule-table {
    font-size: 11px;
  }

  .schedule-table :deep(.q-table thead th) {
    font-size: 10px;
    padding: 8px 4px;
  }

  .schedule-table :deep(.q-table tbody td) {
    padding: 6px 4px;
    font-size: 10px;
  }

  .compact-control-row {
    flex-direction: column;
    gap: 4px;
    align-items: stretch;
  }

  .control-input,
  .output-input-small {
    flex: none;
    width: 100%;
    max-width: none;
  }

  .time-output-section {
    flex-direction: column;
    gap: 4px;
  }

  .time-output-section .output-input,
  .time-output-section .cal-time-input {
    flex: none;
    width: 100%;
    max-width: none;
  }
}

/* 작은 모바일 크기 (480px 이하) */
@media (max-width: 479px) {
  .section-title {
    font-size: 1.2rem;
    padding-left: 0.25rem;
  }

  .control-section .q-card-section {
    padding: 0.5rem;
  }

  .schedule-info {
    padding: 0.5rem;
  }

  .info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
  }

  .info-label,
  .info-value {
    font-size: 12px;
  }

  .registered-schedule-info {
    padding: 6px 8px;
  }

  .registered-schedule-info .text-body2 {
    font-size: 11px;
  }

  .registered-schedule-info .text-caption {
    font-size: 10px;
  }
}

/* ===== 17. 전역 스타일 (Quasar 오버라이드) ===== */

/* ✅ 컬럼 비율 조정 - padding 제거하여 q-col-gutter-md 간격만 사용 */
/* .col-md-2 오버라이드 제거 - Quasar 기본값 사용 */

.col-md-4 {
  width: 33.3333% !important;
}

.col-md-6 {
  width: 50% !important;
  /* ✅ 정확한 50%로 수정 */
}

/* ✅ Schedule Control이 남은 공간을 차지하도록 설정 */
.schedule-control-col {
  flex: 1 1 auto;
  min-width: 0;
}

.col-md-7 {
  width: 58.3333% !important;
}

/* 오프셋 컨트롤 카드 비중 조정 */
.col-sm-3:not(:last-child) {
  flex: 0 0 22%;
  max-width: 22%;
}

.col-sm-3:last-child {
  flex: 0 0 34%;
  max-width: 34%;
}

/* Quasar 테이블 강제 스타일 오버라이드 */
.schedule-table .q-table tbody tr.highlight-first-row {
  background-color: var(--theme-warning-bg) !important;
  color: #000 !important;
  border-left: 4px solid var(--theme-warning) !important;
}

.schedule-table .q-table tbody tr.highlight-first-row td {
  background-color: var(--theme-warning-bg) !important;
  color: #000 !important;
}

.schedule-table .q-table tbody tr.current-tracking-row {
  background-color: var(--theme-positive-bg) !important;
  color: var(--theme-positive-dark) !important;
  border-left: 4px solid var(--theme-positive) !important;
}

.schedule-table .q-table tbody tr.current-tracking-row td {
  background-color: var(--theme-positive-bg) !important;
  color: var(--theme-positive-dark) !important;
}

.schedule-table .q-table tbody tr.next-tracking-row {
  background-color: var(--theme-info-bg) !important;
  color: var(--theme-info-dark) !important;
  border-left: 4px solid var(--theme-info) !important;
}

.schedule-table .q-table tbody tr.next-tracking-row td {
  background-color: var(--theme-info-bg) !important;
  color: var(--theme-info-dark) !important;
}

/* 스크롤바 스타일링 */
.schedule-table .q-table__container {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;
}

.schedule-table .q-table__container::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.schedule-table .q-table__container::-webkit-scrollbar-track {
  background: transparent;
}

.schedule-table .q-table__container::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.schedule-table .q-table__container::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.5);
}
</style>
