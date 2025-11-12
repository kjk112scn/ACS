<template>
  <div class="pass-schedule-mode">
    <!-- 1행: Offset Controls - EphemerisDesignationPage와 동일한 구조 -->
    <div class="row q-col-gutter-md q-mb-sm offset-control-row">
      <div class="col-12">
        <q-card flat bordered class="control-card">
          <q-card-section class="compact-control purple-1">
            <!-- 모든 간격이 동적으로 조정되는 반응형 레이아웃 -->
            <div class="flexible-offset-layout">
              <!-- Azimuth Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Azimuth<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[0]" @input="(val: string) => onInputChange(0, val)" dense outlined
                      type="number" step="0.01" label="Azimuth" class="offset-input" />
                  </div>
                  <div class="col-auto">
                    <div class="vertical-button-group">
                      <div class="vertical-buttons">
                <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(0)" />
                <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(0)" />
              </div>
                      <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(0)" />
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="outputs[0]" dense outlined readonly label="Output"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Elevation Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Elevation<br>Offset
            </div>
      </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[1]" @input="(val: string) => onInputChange(1, val)" dense outlined
                      type="number" step="0.01" label="Elevation"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto">
                    <div class="vertical-button-group">
                      <div class="vertical-buttons">
                <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(1)" />
                <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(1)" />
              </div>
                      <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(1)" />
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="outputs[1]" dense outlined readonly label="Output"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Tilt Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Tilt<br>Offset
            </div>
      </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[2]" @input="(val: string) => onInputChange(2, val)" dense outlined
                      type="number" step="0.01" label="Tilt"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto">
                    <div class="vertical-button-group">
                      <div class="vertical-buttons">
                <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(2)" />
                <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(2)" />
              </div>
                      <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(2)" />
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="outputs[2]" dense outlined readonly label="Output"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Time Offset + Cal Time -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Time<br>Offset
            </div>
      </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[3]" @input="(val: string) => onInputChange(3, val)" dense outlined
                      type="number" step="0.01" label="Time"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto">
                    <div class="vertical-button-group">
                      <div class="vertical-buttons">
                <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(3)" />
                <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(3)" />
              </div>
                      <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(3)" />
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="outputs[3]" dense outlined readonly label="Result"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto cal-time-field">
                    <q-input v-model="formattedCalTime" dense outlined readonly label="Cal Time"
                      style="min-width: 190px !important; max-width: 220px !important;" />
                  </div>
                </div>
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
    <!-- 2행: Main Content -->
    <div class="row q-col-gutter-md main-content-row" style="display: flex; flex-wrap: nowrap; align-items: stretch; margin-bottom: 0 !important; padding-bottom: 0 !important;">
      <!-- 1번 영역: 차트가 들어갈 네모난 칸 - 반응형 크기 조정 -->
      <div class="col-12 col-md-3 position-view-col">
        <q-card class="control-section position-view-card" style="min-height: 360px !important; height: 100% !important; display: flex !important; flex-direction: column !important;">
          <q-card-section class="position-view-section" style="min-height: 360px !important; height: 100% !important; flex: 1 !important; display: flex !important; flex-direction: column !important; padding-top: 16px !important; padding-bottom: 0px !important;">
            <div class="text-subtitle1 text-weight-bold text-primary position-view-title">Position View</div>
            <div class="chart-area" ref="chartRef" style="min-height: 340px !important; height: 100% !important; flex: 1 !important; padding-top: 0 !important; padding-bottom: 0 !important; margin-bottom: 0 !important;"></div>
            </q-card-section>
          </q-card>
        </div>

      <!-- 2번 영역: Schedule Information -->
      <div class="col-12 col-md-3">
          <q-card class="control-section">
            <q-card-section>
            <div class="text-subtitle1 text-weight-bold text-primary q-mb-xs">Schedule Information</div>
              <div class="schedule-form">
                <div class="form-row">
                  <!-- 자동/수동 선택된 스케줄 정보 표시 -->
                <div v-if="displaySchedule" class="schedule-info q-mt-xs">
                    <div class="info-row">
                      <span class="info-label">스케줄 ID / 상태:</span>
                      <div class="info-value-with-badge">
                        <span class="info-value">{{ displaySchedule.no }}</span>
                        <q-badge v-if="currentScheduleStatus" :color="currentScheduleStatus.color"
                          :label="currentScheduleStatus.label" class="q-ml-sm" />
                      </div>
                    </div>
                    <!--     <div class="info-row">
                      <span class="info-label">Index:</span>
                      <span class="info-value">{{ displaySchedule.index }}</span>
                    </div> -->
                    <div class="info-row">
                      <span class="info-label">위성 이름:</span>
                      <span class="info-value">{{ displaySchedule.satelliteName }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">시작 시간:</span>
                      <span class="info-value">{{ formatToLocalTime(displaySchedule.startTime) }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">종료 시간:</span>
                      <span class="info-value">{{ formatToLocalTime(displaySchedule.endTime) }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">지속 시간:</span>
                    <span class="info-value">{{ formatDuration(displaySchedule.duration) }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">시작 방위각:</span>
                      <span class="info-value">{{ displaySchedule.startAzimuthAngle.toFixed(2) }}°</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">최대 고도각:</span>
                      <span class="info-value">{{ displaySchedule.maxElevation?.toFixed(2) }}°</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">남은 시간:</span>
                      <span class="info-value" :class="{
                        'text-negative': timeRemaining < 0,
                        'text-positive': timeRemaining > 0,
                        'text-grey': timeRemaining === 0,
                      }">
                        {{ formatTimeRemaining(timeRemaining) }}
                      </span>
                    </div>

                  </div>
                  <!-- 스케줄이 선택되지 않은 경우 -->
                  <div v-else class="no-schedule-selected">
                    <div class="text-grey-5">추적 중인 스케줄이 없습니다</div>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>
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
                      {{ currentDisplaySchedule.label }}: Index {{ currentDisplaySchedule.mstId }}
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
            <!-- ✅ 스케줄 테이블 - 체크박스 제거, 높이 고정 (모든 데이터 표시, 화면에는 정확히 3개 행만 보이도록) -->
              <q-table flat bordered :row-class="getRowClass" :row-style="getRowStyleDirect" :rows="sortedScheduleList"
                :columns="scheduleColumns" row-key="no" :pagination="{ rowsPerPage: 0 }" hide-pagination
              :loading="loading && sortedScheduleList.length === 0" @row-click="onRowClick" class="schedule-table q-mt-sm"
              style="height: 210px; max-height: 210px;"
              :no-data-label="'등록된 스케줄이 없습니다'"
              :virtual-scroll="false">
                <template v-slot:loading>
                  <q-inner-loading showing color="primary">
                    <q-spinner size="50px" color="primary" />
                  </q-inner-loading>
                </template>
                <!-- 삭제 버튼 컬럼 -->
                <template v-slot:body-cell-actions="props">
                  <q-td :props="props">
                    <q-btn icon="delete" color="negative" size="sm" flat round>
                      <q-tooltip>목록에서 제거</q-tooltip>
                    </q-btn>
                  </q-td>
                </template>
                <!-- 위성 정보 컬럼 템플릿 -->
                <template v-slot:body-cell-satelliteInfo="props">
                  <q-td :props="props" class="satellite-info-cell">
                    <div class="satellite-container">
                      <div class="satellite-id">{{ props.row.satelliteId || '-' }}</div>
                      <div class="satellite-name">{{ props.row.satelliteName }}</div>
                    </div>
                  </q-td>
                </template>

                <!-- 시간 범위 컬럼 템플릿 - formatDateTime 함수 사용 -->
                <template v-slot:body-cell-timeRange="props">
                  <q-td :props="props" class="time-range-cell">
                    <div class="time-container">


                      <div class="start-time">{{ formatDateTime(props.row.startTime) }}</div>
                      <div class="end-time">{{ formatDateTime(props.row.endTime) }}</div>
                    </div>
                  </q-td>
                </template>

                <!-- Azimuth 범위 컬럼 템플릿 -->
                <template v-slot:body-cell-azimuthRange="props">
                  <q-td :props="props" class="azimuth-range-cell">
                    <div class="azimuth-container">
                      <div class="start-az">{{ formatAngle(props.row.startAzimuthAngle) }}</div>
                      <div class="end-az">{{ formatAngle(props.row.endAzimuthAngle) }}</div>
                    </div>
                  </q-td>
                </template>

                <!-- Elevation 정보 컬럼 템플릿 -->
                <template v-slot:body-cell-elevationInfo="props">
                  <q-td :props="props" class="elevation-info-cell">
                    <div class="elevation-container">
                      <div class="max-elevation">{{ formatAngle(props.row.maxElevation) }}</div>
                      <div class="train">{{ formatAngle(0) }}</div>
                    </div>
                  </q-td>
                </template>
              </q-table>
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
                <q-btn color="positive" label="Start" @click="handleStartCommand" class="control-btn" size="sm" />
                <q-btn color="warning" label="Stop" @click="handleStopCommand" class="control-btn" size="sm" />
                <q-btn color="negative" label="Stow" @click="handleStowCommand" class="control-btn" size="sm" />
                </div>
              </div>
            </q-card-section>
          </q-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, onActivated, onDeactivated, nextTick } from 'vue'
import { useQuasar } from 'quasar'
import { usePassScheduleModeStore, type ScheduleItem } from '@/stores'
import { useICDStore } from '../../stores/icd/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import type { QTableProps } from 'quasar'
import { openModal } from '../../utils/windowUtils'
import { formatToLocalTime, formatTimeRemaining, getCalTimeTimestamp } from '../../utils/times'

const $q = useQuasar()
const passScheduleStore = usePassScheduleModeStore()
const icdStore = useICDStore()
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
    // 안전한 배열 업데이트
    this.trackingData.length = 0
    if (Array.isArray(newPath)) {
      this.trackingData.push(...newPath)
    }
    return this.updateOption
  }

  updatePredictedPath(newPath: [number, number][]) {
    // 안전한 배열 업데이트
    this.predictedData.length = 0
    if (Array.isArray(newPath)) {
      this.predictedData.push(...newPath)
    }
    return this.updateOption
  }
}

// 🆕 PassChartUpdatePool 인스턴스 생성
const passChartPool = new PassChartUpdatePool()

// 🔧 모든 computed를 먼저 정의
const scheduleData = computed(() => {
  try {
    const data = passScheduleStore.selectedScheduleList || []
    console.log('🔍 PassSchedulePage scheduleData:', data.length, '개')
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

// 🆕 Store 값 변경 감지
watch(() => icdStore.currentTrackingMstId, (newVal, oldVal) => {
  console.log(`🔄 currentTrackingMstId 변경 감지: ${oldVal} → ${newVal}`)
  reactivityTrigger.value++
}, { immediate: true })

watch(() => icdStore.nextTrackingMstId, (newVal, oldVal) => {
  console.log(`🔄 nextTrackingMstId 변경 감지: ${oldVal} → ${newVal}`)
  reactivityTrigger.value++
}, { immediate: true })

const highlightedRows = computed(() => {
  try {
    // 강제 반응성 트리거 (값을 읽어서 의존성 생성)
    const trigger = reactivityTrigger.value

    const current = icdStore.currentTrackingMstId
    const next = icdStore.nextTrackingMstId

    console.log('🎯 highlightedRows computed 실행:', {
      current,
      next,
      currentType: typeof current,
      nextType: typeof next,
      trigger
    })

    return { current, next }
  } catch (error) {
    console.error('❌ highlightedRows computed 에러:', error)
    return { current: null, next: null }
  }
})

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
// 🔧 직접 스타일 적용 함수
const getRowStyleDirect = (props: { row: ScheduleItem }) => {
  try {
    if (!props || !props.row) {
      return ''
    }
    const schedule = props.row
    const tableIndex = schedule.index
    const { current, next } = highlightedRows.value

    console.log(`🎨 getRowStyleDirect 호출: index=${tableIndex}, current=${current}, next=${next}`)

    if (tableIndex !== undefined) {
      const currentMatch = current !== null && Number(tableIndex) === Number(current)
      const nextMatch = next !== null && Number(tableIndex) === Number(next)

      // 현재 추적 중인 스케줄이 있는 경우
      if (current !== null) {
        if (currentMatch) {
          console.log('✅ 현재 스케줄 매칭 - 직접 녹색 스타일 적용:', tableIndex)
          return {
            backgroundColor: '#c8e6c9 !important',
            borderLeft: '4px solid #4caf50 !important',
            color: '#2e7d32 !important',
            fontWeight: '500 !important'
          }
        }
        if (nextMatch) {
          console.log('✅ 다음 스케줄 매칭 - 직접 파란색 스타일 적용:', tableIndex)
          return {
            backgroundColor: '#e3f2fd !important',
            borderLeft: '4px solid #2196f3 !important',
            color: '#1565c0 !important',
            fontWeight: '500 !important'
          }
        }
      }
      // 현재 추적 중인 스케줄이 없고 다음 예정만 있는 경우
      else if (current === null && next !== null && nextMatch) {
        console.log('✅ 현재 없음 + 다음 스케줄 매칭 - 직접 녹색 스타일 적용:', tableIndex)
        return {
          backgroundColor: '#c8e6c9 !important',
          borderLeft: '4px solid #4caf50 !important',
          color: '#2e7d32 !important',
          fontWeight: '500 !important'
        }
      }
    }

    return {}
  } catch (error) {
    console.error('❌ getRowStyleDirect 에러:', error)
    return {}
  }
}

// 🔧 CSS 클래스 기반 행 스타일링
const getRowClass = (props: { row: ScheduleItem }) => {
  try {
    if (!props || !props.row) {
      return ''
    }
    const schedule = props.row
    const tableIndex = schedule.index
    const { current, next } = highlightedRows.value

    // 모든 getRowClass 호출 로그 (임시 디버깅)
    console.log(`📋 getRowClass 호출: index=${tableIndex}, current=${current}, next=${next}`)

    // 🔧 스케줄 하이라이트 로직 - 강화된 디버깅
    if (tableIndex !== undefined) {
      const currentMatch = current !== null && Number(tableIndex) === Number(current)
      const nextMatch = next !== null && Number(tableIndex) === Number(next)

      // index 14인 경우 강제로 상세 로그 출력
      if (Number(tableIndex) === 14) {
        console.log('🔥 INDEX 14 디버깅:', {
          satelliteName: schedule.satelliteName,
          tableIndex,
          tableIndexNumber: Number(tableIndex),
          current,
          currentNumber: Number(current),
          next,
          nextNumber: Number(next),
          currentMatch,
          nextMatch,
          currentIsNull: current === null,
          nextIsNotNull: next !== null
        })
      }

      // 1. 현재 추적 중인 스케줄이 있는 경우
      if (current !== null) {
        if (currentMatch) {
          console.log('✅ 현재 스케줄 매칭 - 녹색 적용:', tableIndex)
          return 'highlight-current-schedule'
        }
        if (nextMatch) {
          console.log('✅ 다음 스케줄 매칭 - 파란색 적용:', tableIndex)
          return 'highlight-next-schedule'
        }
      }
      // 2. 현재 추적 중인 스케줄이 없고 다음 예정만 있는 경우
      else if (current === null && next !== null && nextMatch) {
        console.log('🎯 현재 없음 + 다음 스케줄 매칭 - 녹색 적용:', tableIndex)
        console.log('🎨 반환할 클래스: highlight-current-schedule')
        return 'highlight-current-schedule'  // 다음 스케줄을 현재 색상으로
      }

      // index 14인데 매칭되지 않은 경우 원인 분석
      if (Number(tableIndex) === 14 && !currentMatch && !nextMatch) {
        console.log('❌ INDEX 14 매칭 실패 원인:', {
          current값: current,
          current타입: typeof current,
          next값: next,
          next타입: typeof next,
          tableIndex값: tableIndex,
          tableIndex타입: typeof tableIndex,
          조건1_current가null: current === null,
          조건2_next가notNull: next !== null,
          조건3_nextMatch: nextMatch,
          전체조건: current === null && next !== null && nextMatch
        })
      }
    }

    return ''
  } catch (error) {
    console.error('❌ getRowClass 에러:', error)
    return ''
  }
}





// 🔧 DOM 직접 조작으로 색상 적용
const applyRowColors = () => {
  try {
    console.log('🎨 DOM 직접 조작으로 색상 적용 시작')

    const current = icdStore.currentTrackingMstId
    const next = icdStore.nextTrackingMstId

    console.log('현재 Store 상태:', { current, next })

    setTimeout(() => {
      const rows = document.querySelectorAll('.schedule-table tbody tr')
      console.log(`총 ${rows.length}개 행 처리`)

      rows.forEach((row) => {
        const htmlRow = row as HTMLElement
        const indexCell = htmlRow.querySelector('td:nth-child(2)') // index 컬럼
        const indexValue = indexCell?.textContent?.trim()
        const indexNumber = Number(indexValue)

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

        // 매칭 확인 및 색상 적용
        let shouldHighlight = false
        let bgColor = ''
        let borderColor = ''
        let textColor = ''

        if (current !== null && indexNumber === current) {
          // 현재 스케줄 - 녹색
          shouldHighlight = true
          bgColor = '#c8e6c9'
          borderColor = '#4caf50'
          textColor = '#2e7d32'
          console.log(`✅ 현재 스케줄 매칭 - index ${indexValue}를 녹색으로 적용`)
        } else if (next !== null && indexNumber === next) {
          // 다음 스케줄은 항상 파란색
          shouldHighlight = true
          bgColor = '#e3f2fd'
          borderColor = '#2196f3'
          textColor = '#1565c0'
          console.log(`✅ 다음 스케줄 매칭 - index ${indexValue}를 파란색으로 적용`)
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

      console.log('✅ DOM 직접 조작 완료')
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

// 🔧 Store 값 변경 시 DOM 직접 조작
watch(
  [() => icdStore?.currentTrackingMstId, () => icdStore?.nextTrackingMstId],
  (newValues, oldValues) => {
    try {
      console.log('🔄 Store 상태 변경 감지:', {
        이전값: oldValues,
        새값: newValues,
        current: icdStore.currentTrackingMstId,
        next: icdStore.nextTrackingMstId
      })

      // 🆕 지연된 DOM 직접 조작으로 색상 적용
      setTimeout(() => {
        applyRowColors()
      }, 100)

    } catch (error) {
      console.error('❌ Store watch 에러:', error)
    }
  },
  { immediate: true, deep: true }
)

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

// 🆕 컴포넌트 활성화 시 데이터 복원
const handleActivated = () => {
  console.log('🔄 PassSchedulePage 활성화됨')

  // 🆕 차트가 없으면 재초기화
  if (!passChart || passChart.isDisposed()) {
    setTimeout(() => {
      initChart()
      console.log('✅ 차트 재초기화 완료')
    }, 100)
  }

  // 🆕 타이머 재시작
  if (!updateTimer) {
    updateTimer = window.setInterval(() => {
      updateChart()
    }, 100)
    console.log('✅ 차트 업데이트 타이머 재시작')
  }

  // 🆕 DOM 하이라이트 강제 적용
  setTimeout(() => {
    applyRowColors()
  }, 200)
}

// 🆕 컴포넌트 비활성화 시 정리
const handleDeactivated = () => {
  console.log('🔄 PassSchedulePage 비활성화됨')

  // 🆕 타이머만 정리 (차트는 유지)
  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
    console.log('✅ 차트 업데이트 타이머 정리됨')
  }
}

// 🆕 Vue 생명주기 훅 등록
onActivated(handleActivated)
onDeactivated(handleDeactivated)

const selectedSchedule = ref<ScheduleItem | null>(null)

// 🔧 current/next 기준으로 자동 선택된 스케줄
const autoSelectedSchedule = computed(() => {
  try {
    const current = icdStore.currentTrackingMstId
    const next = icdStore.nextTrackingMstId
    const schedules = sortedScheduleList.value

    // 1순위: current 스케줄 찾기
    if (current !== null) {
      const currentSchedule = schedules.find(s => Number(s.index) === Number(current))
      if (currentSchedule) {
        console.log('🎯 current 기준 자동 선택:', currentSchedule.satelliteName)
        return currentSchedule
      }
    }

    // 2순위: next 스케줄 찾기 (current가 없을 때)
    if (next !== null) {
      const nextSchedule = schedules.find(s => Number(s.index) === Number(next))
      if (nextSchedule) {
        console.log('🎯 next 기준 자동 선택:', nextSchedule.satelliteName)
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
    const next = icdStore.nextTrackingMstId
    const scheduleIndex = Number(schedule.index)

    // 현재 추적 중인 스케줄인지 확인
    if (current !== null && scheduleIndex === Number(current)) {
      return {
        color: 'positive',
        label: '추적중'
      }
    }

    // 다음 예정 스케줄인지 확인
    if (next !== null && scheduleIndex === Number(next)) {
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

// 입력값과 출력값 - PassSchedule 독립적 상태
const inputs = ref<string[]>(['0.00', '0.00', '0.00', '0'])
const outputs = ref<string[]>(['0.00', '0.00', '0.00', '0'])

// 테이블 컬럼 정의 - Store의 실제 필드명에 맞춤
type QTableColumn = NonNullable<QTableProps['columns']>[0]

const scheduleColumns: QTableColumn[] = [
  { name: 'no', label: 'No', field: 'no', align: 'center' as const, sortable: true, style: 'width: 60px' },
  { name: 'index', label: 'Index', field: 'index', align: 'center' as const, sortable: true, style: 'width: 70px' },
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
  { name: 'actions', label: '작업', field: 'actions', align: 'center' as const, sortable: false, style: 'width: 60px' },
]

const formatDateTime = (dateString: string): string => {
  try {
    return formatToLocalTime(dateString)
  } catch (error) {
    console.error('시간 포맷팅 오류:', error)
    return dateString
  }
}

const formatAngle = (angle: number | undefined | null): string => {
  if (angle === undefined || angle === null) return '-'
  return `${angle.toFixed(1)}°`
}

// ✅ Duration 포맷 함수 추가 (ISO 8601 Duration 형식 파싱)
const formatDuration = (duration: string): string => {
  if (!duration) return '0분 0초'

  // ISO 8601 Duration 형식 (PT13M43.6S) 파싱
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return duration // 파싱 실패 시 원본 반환

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
        console.log('TLE 업로드 모달 닫힘')
        // 모달 닫힌 후 스케줄 데이터 새로고침
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
  } catch (error) {
    console.error('TLE 업로드 모달 열기 실패:', error)
    $q.notify({
      type: 'negative',
      message: 'TLE 업로드 창을 열 수 없습니다',
    })
  }
}

// ✅ 차트 초기화 함수 수정 - 컨테이너 크기에 맞춘 크기
const initChart = () => {
  if (!chartRef.value) return

  // 기존 차트 인스턴스가 있으면 제거
  if (passChart) {
    passChart.dispose()
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
      {
        name: '위치 선',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        animation: false, // ✅ 애니메이션 완전 비활성화
        lineStyle: {
          color: '#ff5722',
          width: 2,
          type: 'dashed',
        },
        data: [
          [0, 0],
          [0, 0],
        ],
        zlevel: 2,
      },
      {
        name: '실시간 추적 경로',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        animation: false, // ✅ 애니메이션 완전 비활성화
        lineStyle: {
          color: '#ffffff',
          width: 2, // ✅ 3 → 2로 줄여서 렌더링 부하 감소
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
          color: '#2196f3',
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

  // ✅ 차트 크기 조정 (차트를 더 크게, Position View 구역 크기와 독립적)
  const adjustChartSize = async () => {
    await nextTick() // ✅ Vue의 DOM 업데이트 완료 대기

    if (!passChart || passChart.isDisposed() || !chartRef.value) return

    // ✅ 차트를 더 크게 설정 (Position View 구역 크기와 독립적)
    const chartSize = 500

    console.log('차트 크기 설정:', chartSize)

    // 리사이즈 수행
    passChart.resize({
      width: chartSize,
      height: chartSize
    })

    // ✅ ECharts가 생성한 실제 DOM 요소에 크기 설정
    await nextTick()
    const chartElement = chartRef.value.querySelector('div') as HTMLElement | null
    if (chartElement) {
      // ✅ 차트를 더 크게 설정
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

      // 다시 리사이즈하여 적용 확인
      passChart.resize({
        width: chartSize,
        height: chartSize
      })
    }

    console.log('차트 리사이즈 완료:', chartSize)
  }

  // ✅ Vue의 nextTick을 사용하여 안전하게 차트 조정
  setTimeout(() => {
    adjustChartSize().catch(console.error)
    // 추가 리사이즈 (레이아웃 완료 대기)
    setTimeout(() => {
      adjustChartSize().catch(console.error)
    }, 200)
  }, 100)

  // ✅ 윈도우 리사이즈 이벤트에 대응 (반응형) - 컨테이너 크기 기반
  const handleResize = () => {
    if (!passChart || passChart.isDisposed()) return

    nextTick().then(() => {
      // ✅ 리사이즈 시에도 컨테이너 크기에 맞춰 조정
      adjustChartSize().catch(console.error)
    }).catch(console.error)
  }

  window.addEventListener('resize', handleResize)
}


// 🆕 선택된 스케줄의 추적 경로 로드
const loadSelectedScheduleTrackingPath = async () => {
  try {
    const schedule = displaySchedule.value
    if (!schedule) {
      console.log('⚠️ 로드할 스케줄이 없음')
      return
    }

    const satelliteId = schedule.satelliteId || schedule.satelliteName
    const passId = schedule.index || schedule.no

    if (!satelliteId || !passId) {
      console.log('⚠️ 위성 ID 또는 패스 ID가 없음')
      return
    }

    console.log('🚀 스케줄 추적 경로 로드 시작:', {
      satelliteName: schedule.satelliteName,
      satelliteId,
      passId
    })

    const success = await passScheduleStore.loadTrackingDetailData(
      satelliteId,
      passId
    )

    if (success) {
      console.log('✅ 추적 경로 로드 완료, 차트 업데이트')
      // 차트가 초기화되어 있다면 즉시 업데이트
      if (passChart) {
        updateChart()
      }
    } else {
      console.warn('⚠️ 추적 경로 로드 실패')
    }
  } catch (error) {
    console.error('❌ 추적 경로 로드 중 오류:', error)
  }
}

// 🆕 성능 최적화를 위한 변수들
const lastUpdateTime = ref(0)
const updateThrottle = 200 // 200ms로 업데이트 간격 증가
const lastPathLength = ref(0)
const pathUpdateThreshold = 5 // 경로 포인트가 5개 이상 변경될 때만 업데이트

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
  if (!passChart) return

  const startTime = performance.now()

  try {
    const now = Date.now()

    // 스로틀링
    if (now - lastUpdateTime.value < updateThrottle) {
      return
    }

    const azimuth = parseFloat(icdStore.azimuthAngle) || 0
    const elevation = parseFloat(icdStore.elevationAngle) || 0

    const normalizedAz = azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))

    currentPosition.value = { azimuth: normalizedAz, elevation: normalizedEl }

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
    const predictedPath = passScheduleStore.predictedTrackingPath

    const shouldShowTrackingPath = icdStore.passScheduleStatusInfo?.isActive === true &&
      actualPath && actualPath.length > 0

    // 성능 모니터링 및 해상도 조정
    const frameTime = performance.now() - startTime
    const resolution = adjustDisplayResolution(actualPath?.length || 0, frameTime)

    // 적응형 경로 최적화
    const displayPath = shouldShowTrackingPath ?
      optimizePathAdaptive(actualPath as [number, number][], resolution) : []

    // 🆕 PassChartUpdatePool을 사용한 차트 업데이트
    const updateOption = passChartPool.updatePosition(normalizedEl, normalizedAz)
    passChartPool.updateTrackingPath(displayPath)
    passChartPool.updatePredictedPath((predictedPath || []).map((point: readonly [number, number]) => [...point]))

    if (passChart && !passChart.isDisposed()) {
      passChart.setOption(updateOption, false, true)
      lastUpdateTime.value = now

      // 성능 통계 업데이트
      performanceMonitor.frameCount++
      performanceMonitor.averageFrameTime =
        (performanceMonitor.averageFrameTime * (performanceMonitor.frameCount - 1) + frameTime) /
        performanceMonitor.frameCount

      // 성능 로그 (10프레임마다)
      if (performanceMonitor.frameCount % 10 === 0) {
        console.log(`📊 성능 통계: 평균 ${performanceMonitor.averageFrameTime.toFixed(2)}ms, 해상도: 1/${resolution}, 포인트: ${displayPath.length}/${actualPath?.length || 0}`)
      }
    }
  } catch (error) {
    console.error('PassSchedule 차트 업데이트 오류:', error)
  }
}

// 🆕 기존 updateChart 함수를 성능 모니터링 버전으로 교체
const updateChart = updateChartWithPerformanceMonitoring

const selectScheduleData = async () => {
  try {
    console.log('스케줄 선택 모달 열기')

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
  } catch (error) {
    console.error('스케줄 선택 모달 열기 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 선택 창을 열 수 없습니다',
    })
  }
}

// 테이블 행 클릭 이벤트 핸들러
const onRowClick = (evt: Event, row: ScheduleItem) => {
  selectedSchedule.value = row
  passScheduleStore.selectSchedule(row) // Store에도 선택 상태 저장
  void updateScheduleChart() // 비동기 함수를 명시적으로 무시

  console.log('스케줄 선택됨:', {
    no: row.no,
    satelliteName: row.satelliteName,
    startTime: row.startTime,
  })
}

// 🆕 선택된 스케줄에 따른 차트 업데이트 (사용하지 않음 - loadSelectedScheduleTrackingPath로 대체)
const updateScheduleChart = async () => {
  if (!passChart || !selectedSchedule.value) return

  try {
    // Store의 추적 경로 초기화
    passScheduleStore.clearTrackingPaths()

    // 선택된 스케줄에서 satelliteId와 passId 추출
    const satelliteId = selectedSchedule.value.satelliteId || selectedSchedule.value.satelliteName
    const passId = selectedSchedule.value.index || selectedSchedule.value.no

    if (satelliteId && passId) {
      console.log(`🛰️ 스케줄 선택 - 추적 경로 조회: ${satelliteId}, 패스: ${passId}`)

      // Store를 통해 추적 경로 세부 데이터 조회
      const success = await passScheduleStore.loadTrackingDetailData(satelliteId, passId)

      if (success) {
        console.log('✅ 추적 경로 데이터 로드 성공')
        updateChart()
      } else {
        console.warn('❌ 추적 경로 데이터 로드 실패')
        // 백업용 더미 경로 설정
        const dummyTrajectory = [
          { azimuth: 0, elevation: 10 },
          { azimuth: 30, elevation: 20 },
          { azimuth: 60, elevation: 35 },
          { azimuth: 90, elevation: 45 },
          { azimuth: 120, elevation: 35 },
          { azimuth: 150, elevation: 20 },
          { azimuth: 180, elevation: 10 }
        ]
        setPredictedPath(dummyTrajectory)
      }
    } else {
      console.warn('❌ 스케줄에서 필요한 정보를 찾을 수 없음:', selectedSchedule.value)
    }

  } catch (error) {
    console.error('스케줄 차트 업데이트 오류:', error)
  }
}

// 🆕 예상 경로 설정 함수 (Store 통해서)
const setPredictedPath = (trajectoryData: Array<{ azimuth: number, elevation: number }>) => {
  try {
    const predictedPath: [number, number][] = trajectoryData.map(point => [
      Math.max(0, Math.min(90, point.elevation)),
      point.azimuth < 0 ? point.azimuth + 360 : point.azimuth
    ])

    passScheduleStore.setPredictedTrackingPath(predictedPath)
    updateChart()
  } catch (error) {
    console.error('예상 경로 설정 오류:', error)
  }
}
/*
// 🆕 실제 추적 경로 초기화 (Store 통해서)
const clearActualPath = () => {
  passScheduleStore.clearTrackingPaths()
  updateChart()
}
 */
// 입력값 변경 핸들러
const onInputChange = (index: number, value: string) => {
  inputs.value[index] = value
  updateOutputs()
}

// 증가 함수
const increment = async (index: number) => {
  const currentOutput = parseFloat(outputs.value[index] || '0')
  const inputValue = parseFloat(inputs.value[index] || '0')
  const newValue = (currentOutput + inputValue).toFixed(index === 3 ? 0 : 2)

  outputs.value[index] = newValue
  await updateOffset(index, newValue)
}

// 감소 함수
const decrement = async (index: number) => {
  const currentOutput = parseFloat(outputs.value[index] || '0')
  const inputValue = parseFloat(inputs.value[index] || '0')
  const newValue = (currentOutput - inputValue).toFixed(index === 3 ? 0 : 2)

  outputs.value[index] = newValue
  await updateOffset(index, newValue)
}

// 리셋 함수
const reset = async (index: number) => {
  inputs.value[index] = index === 3 ? '0' : '0.00'
  outputs.value[index] = index === 3 ? '0' : '0.00'
  await updateOffset(index, outputs.value[index])
}

// 출력값 업데이트
const updateOutputs = () => {
  outputs.value = [...inputs.value]
}

// ✅ updateOffset 함수 수정 - Time 처리 분리
const updateOffset = async (index: number, value: string) => {
  try {
    // ✅ 디버깅 로그 추가
    console.log('updateOffset 호출됨:', {
      index,
      value,
      valueType: typeof value,
      inputs3: inputs.value[3],
      currentTimeResult: passScheduleStore.offsetValues.timeResult,
    })

    const numValue = Number(parseFloat(value).toFixed(2)) || 0
    console.log('계산된 numValue:', numValue)

    const offsetTypes = ['azimuth', 'elevation', 'train', 'time'] as const
    const offsetType = offsetTypes[index]

    if (!offsetType) {
      console.error('Invalid offset index:', index)
      return
    }

    if (index === 3) {
      const timeInputValue = inputs.value[3] || '0.00'
      passScheduleStore.updateOffsetValues('time', timeInputValue)
      try {
        await passScheduleStore.sendTimeOffset(numValue)
        passScheduleStore.updateOffsetValues('timeResult', numValue.toFixed(2))
        console.log('Time Result 업데이트:', numValue.toFixed(2))
      } catch (error) {
        console.error('Time offset command failed:', error)
      }
      return
    }

    // Position Offset 처리 (azimuth, elevation, train)
    passScheduleStore.updateOffsetValues(offsetType, numValue.toFixed(2))

    const azOffset = Number((parseFloat(passScheduleStore.offsetValues.azimuth) || 0).toFixed(2))
    const elOffset = Number((parseFloat(passScheduleStore.offsetValues.elevation) || 0).toFixed(2))
    const tiOffset = Number((parseFloat(passScheduleStore.offsetValues.train) || 0).toFixed(2))

    await icdStore.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)
  } catch (error) {
    console.error('Error updating offset:', error)
  }
}
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
          const satelliteId = firstSchedule.satelliteId || firstSchedule.satelliteName
          const passId = firstSchedule.index || firstSchedule.no

          if (satelliteId && passId) {
            console.log('🛰️ 예측 경로 로드 시작:', satelliteId, passId)
            try {
              const pathLoaded = await passScheduleStore.loadTrackingDetailData(satelliteId, passId)
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
  } catch (error) {
    console.error('❌ ACS Start 명령 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 시작에 실패했습니다',
    })
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
  } catch (error) {
    console.error('Failed to send stop command:', error)
    $q.notify({
      type: 'negative',
      message: '정지 명령 전송에 실패했습니다',
    })
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
  } catch (error) {
    console.error('Failed to send stow command:', error)
    $q.notify({
      type: 'negative',
      message: 'Stow 명령 전송에 실패했습니다',
    })
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
onMounted(async () => {
  console.log('PassSchedulePage 컴포넌트 마운트됨')

  // 🆕 기존 데이터 복원 확인
  const hasExistingData = passScheduleStore.selectedScheduleList.length > 0
  console.log('기존 데이터 확인:', {
    hasExistingData,
    scheduleCount: passScheduleStore.selectedScheduleList.length,
    currentTrackingMstId: icdStore.currentTrackingMstId,
    nextTrackingMstId: icdStore.nextTrackingMstId
  })

  // 🆕 Store 초기화 (기존 데이터가 있으면 건너뛰기)
  if (!hasExistingData) {
    try {
      await passScheduleStore.init()
      console.log('✅ 스케줄 데이터 로드 완료:', passScheduleStore.scheduleData.length, '개')
    } catch (error) {
      console.error('스케줄 데이터 로드 실패:', error)
      $q.notify({
        type: 'negative',
        message: '스케줄 데이터를 불러오는데 실패했습니다',
      })
    }
  } else {
    console.log('✅ 기존 스케줄 데이터 사용:', passScheduleStore.selectedScheduleList.length, '개')
  }

  // 🆕 PassSchedule 차트 초기화 (지연 시간 증가)
  setTimeout(() => {
    initChart()

    // 🆕 차트 업데이트 타이머 시작 (기존 타이머 정리 후 시작)
    if (updateTimer) {
      clearInterval(updateTimer)
    }
    updateTimer = window.setInterval(() => {
      updateChart()
    }, 100)

    console.log('✅ PassSchedule 차트 및 타이머 초기화 완료')
  }, 200) // 지연 시간을 200ms로 증가
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

  // 🆕 PassSchedule 차트 인스턴스 정리 (기존 인스턴스가 있을 때만)
  if (passChart && !passChart.isDisposed()) {
    passChart.dispose()
    passChart = null
    console.log('✅ PassSchedule 차트 인스턴스 정리됨')
  }

  // 🆕 시간 업데이트 타이머 정리
  stopTimeTimer()

  // 🆕 추적 경로 데이터는 유지 (Store에서 관리)
  // passScheduleStore.clearTrackingPaths() 제거

  // 🆕 이벤트 리스너 정리
  window.removeEventListener('resize', () => { })

  console.log('✅ PassSchedulePage 정리 완료')
})

// 서버 시간 포맷팅을 위한 계산된 속성 추가
const formattedCalTime = computed(() => {
  const calTime = icdStore.resultTimeOffsetCalTime
  if (!calTime) return ''
  try {
    // 서버 시간 파싱
    const dateObj = new Date(calTime)

    // 유효한 날짜인지 확인
    if (isNaN(dateObj.getTime())) {
      return calTime // 유효하지 않은 날짜면 원본 반환
    }

    // UTC 기준으로 시간 형식 지정
    const utcYear = dateObj.getFullYear()
    const utcMonth = String(dateObj.getMonth() + 1).padStart(2, '0')
    const utcDay = String(dateObj.getDate()).padStart(2, '0')
    const utcHours = String(dateObj.getHours()).padStart(2, '0')
    const utcMinutes = String(dateObj.getMinutes()).padStart(2, '0')
    const utcSeconds = String(dateObj.getSeconds()).padStart(2, '0')
    const utcMilliseconds = String(dateObj.getMilliseconds()).padStart(3, '0')

    // YYYY-MM-DD HH:MM:SS.mmm (UTC) 형식
    return `${utcYear}-${utcMonth}-${utcDay} ${utcHours}:${utcMinutes}:${utcSeconds}.${utcMilliseconds} `
  } catch (e) {
    console.error('Error formatting cal time:', e)
    return calTime
  }
})
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

/* ✅ 오프셋 컨트롤 행 하단 여백 줄이기 */
.pass-schedule-mode .offset-control-row {
  margin-bottom: 0.5rem !important; /* ✅ 기본 q-mb-sm (0.5rem) 유지하되 명시적으로 설정 */
}

/* ✅ 메인 콘텐츠 행 하단 여백을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.pass-schedule-mode .main-content-row {
  margin-bottom: 0 !important; /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important; /* ✅ 하단 패딩 제거 */
}

/* ✅ Quasar q-col-gutter-md가 행에 추가하는 하단 마진을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.pass-schedule-mode .main-content-row.q-col-gutter-md,
.pass-schedule-mode .row.q-col-gutter-md.main-content-row {
  margin-bottom: 0 !important; /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ Quasar row 기본 스타일 오버라이드 (더 강력한 선택자) - EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.pass-schedule-mode .main-content-row.row,
.pass-schedule-mode .row.main-content-row {
  margin-bottom: 0 !important; /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
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
  height: auto !important; /* ✅ height: 100% 제거하여 내용에 맞게 조정 */
  width: 100%;
  padding: 0 !important; /* ✅ EphemerisDesignationPage.vue와 동일하게 상단 패딩 제거 */
  margin: 0 !important;
  margin-bottom: 0 !important; /* ✅ 하단 마진 제거 */
  padding-bottom: 0 !important; /* ✅ 하단 패딩 제거 */
  min-height: auto !important; /* ✅ 최소 높이 제거 */
  max-height: none !important; /* ✅ 최대 높이 제거 */
  display: flex !important; /* ✅ flexbox로 변경 */
  flex-direction: column !important; /* ✅ 세로 방향 */
  gap: 0 !important; /* ✅ flex gap 제거 */
  row-gap: 0 !important; /* ✅ flex row-gap 제거 */
  column-gap: 0 !important; /* ✅ flex column-gap 제거 */
}

/* router-view, q-page-container의 하단 패딩/마진 제거 */
router-view,
q-page-container,
router-view > *,
q-page-container > * {
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
.pass-schedule-mode > *:last-child {
  margin-bottom: 0 !important; /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode 내부의 모든 직접 자식 요소 하단 여백 제거 */
.pass-schedule-mode > * {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row가 pass-schedule-mode의 마지막 자식일 때 하단 여백 완전 제거 */
.pass-schedule-mode > .main-content-row:last-child,
.pass-schedule-mode > .row.main-content-row:last-child,
.pass-schedule-mode > div.main-content-row:last-child,
.pass-schedule-mode > .main-content-row,
.pass-schedule-mode > .row.main-content-row,
.pass-schedule-mode > div.main-content-row {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
  margin-top: 0 !important;
  padding-top: 0 !important;
}

/* ✅ pass-schedule-mode의 마지막 div 요소 하단 여백 완전 제거 (더 강력한 선택자) */
.pass-schedule-mode > div:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode의 마지막 row 요소 하단 여백 완전 제거 */
.pass-schedule-mode > .row:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode의 모든 직접 자식 row 요소 하단 여백 제거 */
.pass-schedule-mode > .row {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ pass-schedule-mode의 모든 직접 자식 div 요소 하단 여백 제거 */
.pass-schedule-mode > div {
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
  border: 1px solid rgba(255, 255, 255, 0.12);
  /* ✅ EphemerisDesignationPage.vue와 동일한 높이를 위해 flex 추가 - 내부 구성 변경 없음 */
  display: flex;
  flex-direction: column;
  margin-bottom: 0 !important; /* ✅ 하단 마진 제거 */
}

/* ✅ main-content-row 내부의 모든 컬럼 하단 여백 완전 제거 */
.pass-schedule-mode .main-content-row > [class*="col-"] {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 하단 여백 완전 제거 (더 구체적인 선택자) */
.pass-schedule-mode .main-content-row > [class*="col-"]:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card 하단 여백 제거 */
.pass-schedule-mode .main-content-row > [class*="col-"] .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card 하단 여백 제거 (더 구체적인 선택자) */
.pass-schedule-mode .main-content-row > [class*="col-"]:last-child .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card-section 하단 여백 제거 */
.pass-schedule-mode .main-content-row > [class*="col-"] .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card-section 하단 여백 제거 (더 구체적인 선택자) */
.pass-schedule-mode .main-content-row > [class*="col-"]:last-child .q-card-section {
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
  min-height: 360px !important; /* ✅ 최소 높이 보장 */
  height: 100% !important; /* ✅ 부모 높이에 맞춤 (다른 패널과 동일하게) */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Position View 카드 섹션 높이 조정 */
.pass-schedule-mode .control-section.position-view-card .q-card-section.position-view-section {
  min-height: 360px !important; /* ✅ 차트 영역 최소 높이 보장 */
  height: 100% !important; /* ✅ 부모 높이에 맞춤 (다른 패널과 동일하게) */
  flex: 1 !important; /* ✅ 남은 공간 채우기 */
  display: flex !important;
  flex-direction: column !important;
}

.control-section .q-card-section {
  padding: 16px !important;
  padding-bottom: 0 !important; /* ✅ 하단 패딩 제거 (상단 공간과 동일하게) */
  /* ✅ 남은 공간을 채우도록 flex 추가 - 내부 구성 변경 없음 */
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative; /* ✅ 제목 absolute positioning을 위한 기준점 */
}

/* ✅ Schedule Information 카드 높이를 Position View와 동일하게 설정 (360px) */
.pass-schedule-mode .main-content-row > [class*="col-"]:nth-child(2) .control-section,
.pass-schedule-mode .main-content-row > [class*="col-"]:nth-child(2) .control-section.q-card {
  min-height: 360px !important; /* ✅ 최소 높이 보장 */
  height: 100% !important; /* ✅ 부모 높이에 맞춤 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Schedule Information 카드 섹션 높이 조정 */
.pass-schedule-mode .main-content-row > [class*="col-"]:nth-child(2) .control-section .q-card-section {
  min-height: 360px !important; /* ✅ 최소 높이 보장 */
  flex: 1 !important; /* ✅ 남은 공간 채우기 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Schedule Control 카드 높이를 Position View와 동일하게 설정 (360px) */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section,
.pass-schedule-mode .main-content-row .schedule-control-col .control-section.q-card {
  min-height: 360px !important; /* ✅ 최소 높이 보장 */
  height: 100% !important; /* ✅ 부모 높이에 맞춤 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Schedule Control 카드 섹션 높이 조정 */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section {
  min-height: 360px !important; /* ✅ 최소 높이 보장 */
  flex: 1 1 auto !important; /* ✅ 남은 공간 채우기 (flex-grow: 1, flex-shrink: 1, flex-basis: auto) */
  display: flex !important;
  flex-direction: column !important;
  padding-bottom: 0 !important; /* ✅ 하단 패딩 완전 제거 (상단 공간과 동일하게) */
  margin-bottom: 0 !important; /* ✅ 하단 마진 제거 */
  overflow: hidden !important; /* ✅ 하단 여백 방지 */
  justify-content: flex-start !important; /* ✅ 상단 정렬로 하단 여백 제거 */
}

/* ✅ 3단계: schedule-control-section 내부의 button-group 하단 여백 완전 제거 (더 구체적인 선택자) */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
  flex-shrink: 0 !important; /* ✅ 버튼 그룹이 축소되지 않도록 */
}

/* ✅ schedule-control-section 내부의 마지막 요소 하단 여백 완전 제거 */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section > *:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ schedule-control-section 내부의 모든 직접 자식 요소 하단 여백 제거 */
.pass-schedule-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section > * {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

.position-view-section {
  padding: 16px 16px 0px 16px !important; /* ✅ 상단 패딩을 다른 패널과 동일하게 16px로 맞춤, 하단 패딩 제거 */
}

.position-view-title {
  position: absolute; /* ✅ 제목을 absolute로 배치하여 차트 영역이 전체 공간 사용 */
  top: 16px;
  left: 16px;
  z-index: 10;
  margin: 0;
  padding: 0;
}

.chart-area {
  min-height: 340px !important; /* ✅ 최소 높이 보장 */
  height: 100% !important; /* ✅ 부모 높이에 맞춤 */
  flex: 1 !important; /* ✅ 남은 공간 채우기 */
  width: 100%;
  display: flex;
  align-items: center; /* ✅ 중앙 정렬 */
  justify-content: center;
  margin: 0 auto;
  margin-bottom: 0 !important; /* ✅ 하단 마진 제거 */
  padding: 0 !important;
  padding-bottom: 0 !important; /* ✅ 하단 패딩 제거 */
  box-sizing: border-box;
  overflow: visible !important; /* ✅ 차트가 넘쳐도 보이도록 변경 */
  text-align: center;
  position: relative;
}

/* ✅ 차트 컨테이너 - 차트를 더 크게 (Position View 구역 크기와 독립적) */
.chart-area>div {
  position: absolute !important;
  left: 50% !important;
  top: 50% !important; /* ✅ 중앙 정렬 */
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
  aspect-ratio: 1 !important; /* ✅ 정사각형 유지 */
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
    top: 50% !important; /* ✅ 중앙 정렬 */
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
    top: 50% !important; /* ✅ 중앙 정렬 */
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
    top: 50% !important; /* ✅ 중앙 정렬 */
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

.pass-schedule-mode .position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  line-height: 1.2 !important;
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
  margin-bottom: 0; /* ✅ 하단 마진 제거 */
  flex: 1; /* ✅ 남은 공간을 채워서 하단 정렬 */
  display: flex;
  flex-direction: column;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.25rem; /* ✅ gap 줄임 (0.5rem → 0.25rem) */
  width: 100%;
  flex: 1; /* ✅ 남은 공간을 채워서 하단 정렬 */
  justify-content: flex-start; /* ✅ 상단부터 시작 */
}

.schedule-info {
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px 16px 8px 16px; /* ✅ 하단 패딩 줄임 (16px → 8px) */
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex: 1; /* ✅ 남은 공간을 채워서 하단 정렬 */
  display: flex;
  flex-direction: column;
  justify-content: flex-start; /* ✅ 상단부터 시작 */
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
  padding: 6px 0; /* ✅ 패딩 줄임 (8px → 6px) */
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-row:last-child {
  border-bottom: none;
  margin-bottom: auto; /* ✅ 마지막 행 아래에 자동 여백 추가하여 하단 정렬 */
}

.info-label {
  font-weight: 500;
  color: #90caf9;
  min-width: 120px;
}

.info-value {
  font-weight: 400;
  color: #ffffff;
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
  color: #2196f3;
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
  height: 210px !important; /* ✅ 높이 고정 (정확히 3개 행만 보이도록) */
  max-height: 210px !important; /* ✅ 최대 높이 고정 */
}

/* Quasar 테이블 기본 설정 초기화 */
.schedule-table :deep(.q-table__container) {
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  height: 210px !important; /* ✅ 높이 고정 (정확히 3개 행만 보이도록) */
  max-height: 210px !important; /* ✅ 최대 높이 고정 */
  display: flex;
  flex-direction: column;
  overflow: hidden; /* ✅ 컨테이너는 스크롤 없음 */
}

/* ✅ 테이블 바디 영역만 스크롤 가능하도록 설정 */
.schedule-table :deep(.q-table__middle) {
  flex: 1;
  overflow-y: auto; /* ✅ 세로 스크롤 가능 */
  overflow-x: auto; /* ✅ 가로 스크롤도 가능 */
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
  border-left: 3px solid #2196f3;
}

/* ===== 11. 하이라이트 스타일 (최고 우선순위) ===== */

/* 현재 추적 중인 스케줄 하이라이트 */
.schedule-table :deep(.q-table tbody tr.current-tracking-row) {
  background-color: #c8e6c9 !important;
  border-left: 4px solid #4caf50 !important;
  color: #2e7d32 !important;
}

.schedule-table :deep(.q-table tbody tr.current-tracking-row td) {
  background-color: #c8e6c9 !important;
  color: #2e7d32 !important;
  font-weight: 500;
}

.schedule-table :deep(.q-table tbody tr.current-tracking-row:hover) {
  background-color: #a5d6a7 !important;
}

.schedule-table :deep(.q-table tbody tr.current-tracking-row:hover td) {
  background-color: #a5d6a7 !important;
}

/* 다음 예정 스케줄 하이라이트 */
.schedule-table :deep(.q-table tbody tr.next-tracking-row) {
  background-color: #e3f2fd !important;
  border-left: 4px solid #2196f3 !important;
  color: #1565c0 !important;
}

.schedule-table :deep(.q-table tbody tr.next-tracking-row td) {
  background-color: #e3f2fd !important;
  color: #1565c0 !important;
  font-weight: 500;
}

.schedule-table :deep(.q-table tbody tr.next-tracking-row:hover) {
  background-color: #bbdefb !important;
}

.schedule-table :deep(.q-table tbody tr.next-tracking-row:hover td) {
  background-color: #bbdefb !important;
}

/* 테스트용 첫 번째 행 하이라이트 */
.schedule-table :deep(.q-table tbody tr.highlight-first-row) {
  background-color: #ffeb3b !important;
  color: #000 !important;
  border-left: 4px solid #ffc107 !important;
}

.schedule-table :deep(.q-table tbody tr.highlight-first-row td) {
  background-color: #ffeb3b !important;
  color: #000 !important;
  font-weight: 600;
}

.schedule-table :deep(.q-table tbody tr.highlight-first-row:hover) {
  background-color: #ffc107 !important;
}

.schedule-table :deep(.q-table tbody tr.highlight-first-row:hover td) {
  background-color: #ffc107 !important;
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
    color: #2196f3;
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
    color: #4caf50;
  }

  .end-time {
    color: #ff9800;
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
  color: #2196f3;
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
  color: #4caf50;
}

.end-time {
  color: #ff9800;
}

/* ===== 12.5. 스케줄 하이라이트 스타일 ===== */

/* 현재 추적 중인 스케줄 하이라이트 - 더 강력한 선택자 */
.schedule-table tbody tr.highlight-current-schedule {
  background-color: #c8e6c9 !important;
  border-left: 4px solid #4caf50 !important;
}

.schedule-table tbody tr.highlight-current-schedule td {
  background-color: #c8e6c9 !important;
  color: #2e7d32 !important;
  font-weight: 500 !important;
}

/* 모든 하위 요소들에도 강제 적용 */
.schedule-table tbody tr.highlight-current-schedule * {
  background-color: #c8e6c9 !important;
  color: #2e7d32 !important;
}

/* 특정 클래스들도 명시적으로 적용 */
.schedule-table tbody tr.highlight-current-schedule .start-time,
.schedule-table tbody tr.highlight-current-schedule .end-time,
.schedule-table tbody tr.highlight-current-schedule .satellite-name,
.schedule-table tbody tr.highlight-current-schedule .satellite-id,
.schedule-table tbody tr.highlight-current-schedule .start-az,
.schedule-table tbody tr.highlight-current-schedule .end-az,
.schedule-table tbody tr.highlight-current-schedule .max-elevation,
.schedule-table tbody tr.highlight-current-schedule .train {
  background-color: #c8e6c9 !important;
  color: #2e7d32 !important;
  font-weight: 500 !important;
}

/* Quasar 컴포넌트들 오버라이드 */
.schedule-table tbody tr.highlight-current-schedule .q-btn,
.schedule-table tbody tr.highlight-current-schedule .q-icon {
  background-color: #c8e6c9 !important;
}

/* 다음 예정 스케줄 하이라이트 - 더 강력한 선택자 */
.schedule-table tbody tr.highlight-next-schedule {
  background-color: #e3f2fd !important;
  border-left: 4px solid #2196f3 !important;
}

.schedule-table tbody tr.highlight-next-schedule td {
  background-color: #e3f2fd !important;
  color: #1565c0 !important;
  font-weight: 500 !important;
}

/* 모든 하위 요소들에도 강제 적용 */
.schedule-table tbody tr.highlight-next-schedule * {
  background-color: #e3f2fd !important;
  color: #1565c0 !important;
}

/* 특정 클래스들도 명시적으로 적용 */
.schedule-table tbody tr.highlight-next-schedule .start-time,
.schedule-table tbody tr.highlight-next-schedule .end-time,
.schedule-table tbody tr.highlight-next-schedule .satellite-name,
.schedule-table tbody tr.highlight-next-schedule .satellite-id,
.schedule-table tbody tr.highlight-next-schedule .start-az,
.schedule-table tbody tr.highlight-next-schedule .end-az,
.schedule-table tbody tr.highlight-next-schedule .max-elevation,
.schedule-table tbody tr.highlight-next-schedule .train {
  background-color: #e3f2fd !important;
  color: #1565c0 !important;
  font-weight: 500 !important;
}

/* Quasar 컴포넌트들 오버라이드 */
.schedule-table tbody tr.highlight-next-schedule .q-btn,
.schedule-table tbody tr.highlight-next-schedule .q-icon {
  background-color: #e3f2fd !important;
}

/* 하이라이트된 행에서 호버 효과 유지 */
.schedule-table tbody tr.highlight-current-schedule:hover td {
  background-color: #a5d6a7 !important;
}

.schedule-table tbody tr.highlight-next-schedule:hover td {
  background-color: #bbdefb !important;
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
  color: #4caf50;
}

.end-az {
  color: #ff9800;
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
  color: #9c27b0;
}

.train {
  color: #607d8b;
}

/* ===== 14. 버튼 그룹 스타일 ===== */
.button-group {
  margin-top: 0.25rem; /* ✅ 상단 마진 줄임 (0.5rem → 0.25rem) */
  margin-bottom: 0 !important; /* ✅ 하단 마진 완전 제거 */
  width: 100%;
  flex-shrink: 0;
  padding-top: 0.25rem; /* ✅ 상단 패딩 줄임 (0.5rem → 0.25rem) */
  padding-bottom: 0 !important; /* ✅ 하단 패딩 완전 제거 */
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  box-sizing: border-box !important;
  display: flex !important; /* ✅ flex 컨테이너로 명시 */
  flex-direction: column !important; /* ✅ 세로 방향 */
  gap: 0.5rem !important; /* ✅ 버튼 행 사이 간격 명시 (겹침 방지) */
}

.button-row {
  display: flex;
  gap: 0.5rem;
  width: 100%;
  margin-bottom: 0 !important; /* ✅ 하단 마진 완전 제거 (상단 공간과 동일하게) */
  flex-shrink: 0 !important; /* ✅ 버튼 행이 축소되지 않도록 */
}

/* ✅ Quasar q-mb-sm 클래스 오버라이드 (button-row에 적용된 경우) */
.button-row.q-mb-sm,
.schedule-control-section .button-row.q-mb-sm {
  margin-bottom: 0 !important; /* ✅ 하단 마진 완전 제거 */
}

.control-button-row {
  display: flex;
  gap: 0.5rem;
  width: 100%;
  flex-shrink: 0 !important; /* ✅ 버튼 행이 축소되지 않도록 */
  margin-bottom: 0 !important; /* ✅ 하단 마진 제거 */
}

/* 업로드 버튼 스타일 */
.upload-btn {
  flex: 1;
  min-width: 0;
  height: 36px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.upload-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* 컨트롤 버튼 스타일 */
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
  color: #ffc107;
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
    padding: 0.25rem !important; /* ✅ 모바일에서만 패딩 적용 */
    padding-bottom: 0 !important; /* ✅ 하단 패딩은 여전히 제거 */
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
  width: 50% !important; /* ✅ 정확한 50%로 수정 */
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
  background-color: #ffeb3b !important;
  color: #000 !important;
  border-left: 4px solid #ffc107 !important;
}

.schedule-table .q-table tbody tr.highlight-first-row td {
  background-color: #ffeb3b !important;
  color: #000 !important;
}

.schedule-table .q-table tbody tr.current-tracking-row {
  background-color: #c8e6c9 !important;
  color: #2e7d32 !important;
  border-left: 4px solid #4caf50 !important;
}

.schedule-table .q-table tbody tr.current-tracking-row td {
  background-color: #c8e6c9 !important;
  color: #2e7d32 !important;
}

.schedule-table .q-table tbody tr.next-tracking-row {
  background-color: #e3f2fd !important;
  color: #1565c0 !important;
  border-left: 4px solid #2196f3 !important;
}

.schedule-table .q-table tbody tr.next-tracking-row td {
  background-color: #e3f2fd !important;
  color: #1565c0 !important;
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
