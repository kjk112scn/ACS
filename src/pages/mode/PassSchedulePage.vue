<template>
  <div class="pass-schedule-mode">
    <div class="section-title text-h5 text-primary q-mb-sm">Pass Schedule</div>

    <!-- Control Section -->
    <div class="row q-col-gutter-md q-mb-md">
      <!-- Azimuth Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="bg-blue-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Azimuth</div>
          </q-card-section>
          <q-card-section>
            <div class="compact-control-row">
              <q-input v-model="inputs[0]" @input="(val: string) => onInputChange(0, val)" dense outlined type="number"
                step="0.01" class="control-input" label="Input" />
              <div class="control-buttons">
                <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(0)" />
                <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(0)" />
              </div>
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(0)" class="reset-button" />

              <q-input v-model="outputs[0]" dense outlined readonly class="output-input-small" label="Output" />
            </div>
          </q-card-section>
        </q-card>
      </div>
      <!-- Elevation Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="bg-green-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Elevation</div>
          </q-card-section>
          <q-card-section>
            <div class="compact-control-row">
              <q-input v-model="inputs[1]" @input="(val: string) => onInputChange(1, val)" dense outlined type="number"
                step="0.01" class="control-input" label="Input" />
              <div class="control-buttons">
                <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(1)" />
                <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(1)" />
              </div>
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(1)" class="reset-button" />

              <q-input v-model="outputs[1]" dense outlined readonly class="output-input-small" label="Output" />
            </div>
          </q-card-section>
        </q-card>
      </div>
      <!-- Tilt Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="bg-orange-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Tilt</div>
          </q-card-section>
          <q-card-section>
            <div class="compact-control-row">
              <q-input v-model="inputs[2]" @input="(val: string) => onInputChange(2, val)" dense outlined type="number"
                step="0.01" class="control-input" label="Input" />
              <div class="control-buttons">
                <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(2)" />
                <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(2)" />
              </div>
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(2)" class="reset-button" />

              <q-input v-model="outputs[2]" dense outlined readonly class="output-input-small" label="Output" />
            </div>
          </q-card-section>
        </q-card>
      </div>
      <!-- Time Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="purple-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Time</div>
          </q-card-section>
          <q-card-section>
            <div class="compact-control-row">
              <q-input v-model="inputs[3]" @input="(val: string) => onInputChange(3, val)" dense outlined type="number"
                step="1" class="control-input" label="Input" />
              <div class="control-buttons">
                <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(3)" />
                <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(3)" />
              </div>
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(3)" class="reset-button" />


              <div class="time-output-section">
                <q-input v-model="outputs[3]" dense outlined readonly class="output-input" label="Output" />
                <q-input v-model="formattedCalTime" dense outlined readonly label="Cal Time" class="cal-time-input" />
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
    <!-- Main Content Section - 2행 구조로 변경 -->
    <div class="schedule-container">
      <div class="row q-col-gutter-sm">

        <!-- 1행: Position View - 기존 크기 유지 -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Position View</div>
              <div ref="chartRef" class="chart-area"></div>
            </q-card-section>
          </q-card>
        </div>
        <!-- 1행: Schedule Information - 30% 축소 -->
        <div class="col-12 col-md-2">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Schedule Information</div>
              <div class="schedule-form">
                <div class="form-row">
                  <!-- 자동/수동 선택된 스케줄 정보 표시 -->
                  <div v-if="displaySchedule" class="schedule-info q-mt-md">
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
                      <span class="info-value">{{ displaySchedule.duration }}</span>
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
        <!-- Schedule Control - 30% 확대 -->
        <div class="col-12 col-md-6">
          <q-card class="control-section">
            <q-card-section>
              <!-- ✅ Schedule Control 헤더 -->
              <div class="schedule-header">
                <div class="text-subtitle1 text-weight-bold text-primary">Schedule Control</div>
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
              <!-- ✅ 스케줄 테이블 - 체크박스 제거 -->
              <q-table flat bordered :row-class="getRowClass" :row-style="getRowStyleDirect" :rows="sortedScheduleList"
                :columns="scheduleColumns" row-key="no" :pagination="{ rowsPerPage: 0 }" hide-pagination
                :loading="loading" @row-click="onRowClick" class="schedule-table q-mt-sm"
                :style="scheduleData.length <= 20 ? 'height: auto; max-height: 400px' : 'height: 300px'"
                :no-data-label="'등록된 스케줄이 없습니다'" :virtual-scroll="scheduleData.length > 20"
                :virtual-scroll-sticky-size-start="48">
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
                      <div class="tilt">{{ formatAngle(0) }}</div>
                    </div>
                  </q-td>
                </template>
              </q-table>
              <!-- 버튼 그룹 섹션 -->
              <div class="button-group q-mt-md">
                <div class="button-row q-mb-md">
                  <q-btn color="info" label="TLE Upload" icon="upload_file" @click="handleTLEUpload"
                    class="q-mr-sm upload-btn" size="md" />

                  <q-btn color="primary" label="Select Schedule" icon="playlist_add_check" @click="selectScheduleData"
                    class="upload-btn" size="md">
                    <q-tooltip>스케줄 목록을 불러와서 선택할 수 있습니다</q-tooltip>
                  </q-btn>
                </div>

                <div class="control-button-row">
                  <q-btn color="positive" label="Start" @click="handleStartCommand" class="control-btn" size="md" />
                  <q-btn color="warning" label="Stop" @click="handleStopCommand" class="control-btn" size="md" />
                  <q-btn color="negative" label="Stow" @click="handleStowCommand" class="control-btn" size="md" />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { usePassScheduleStore, type ScheduleItem } from '../../stores/mode/passScheduleStore'
import { useICDStore } from '../../stores/icd/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import type { QTableProps } from 'quasar'
import { openModal } from '../../utils/windowUtils'
import { formatToLocalTime, formatTimeRemaining, getCalTimeTimestamp } from '../../utils/times'
import { useEphemerisTrackStore } from '../../stores/mode/ephemerisTrackStore'

const $q = useQuasar()
const passScheduleStore = usePassScheduleStore()
const icdStore = useICDStore()
const ephemerisStore = useEphemerisTrackStore()
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
  private updateOption: {
    series: Array<{ data?: [number, number][] }>
  }

  constructor() {
    this.updateOption = {
      series: [{ data: this.positionData }, {}, { data: this.trackingData }, {}],
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
}

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

      // DOM 직접 조작으로 색상 적용
      applyRowColors()

    } catch (error) {
      console.error('❌ Store watch 에러:', error)
    }
  },
  { immediate: true, deep: true }
)
watch(
  () => passScheduleStore.selectedScheduleList,
  (newData) => {
    try {
      console.log('👀 Store 변경 감지 - 새 데이터:', newData?.length || 0, '개')
    } catch (error) {
      console.error('❌ passScheduleStore watch 에러:', error)
    }
  },
  { immediate: true, deep: true }
)

// 🆕 Store 상태 변경 즉시 감지
watch(
  () => passScheduleStore.selectedScheduleList,
  (newData) => {
    console.log('👀 Store 변경 감지 - 새 데이터:', newData.length, '개')
  },
  { immediate: true, deep: true } // immediate: true가 중요!
)

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

// 🆕 PassSchedule 전용 차트 초기화
const initChart = () => {
  if (!chartRef.value) return

  // 기존 차트 인스턴스가 있으면 제거
  if (passChart) {
    passChart.dispose()
  }

  // 차트 인스턴스 생성
  passChart = echarts.init(chartRef.value)
  console.log('PassSchedule 차트 인스턴스 생성됨')

  // 차트 옵션 설정 (Ephemeris와 동일한 스타일)
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
      animation: false,
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
      animation: false,
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
        name: '실시간 추적 위치',
        type: 'scatter',
        coordinateSystem: 'polar',
        symbol: 'circle',
        symbolSize: 15,
        animation: false,
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
            return `Az: ${params.value[1].toFixed(2)}°\nEl: ${params.value[0].toFixed(2)}°`
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
        name: '실시간 추적 경로',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        animation: false,
        lineStyle: {
          color: '#ffffff',
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
        animation: false,
        lineStyle: {
          color: '#2196f3',
          width: 2,
        },
        data: [],
        zlevel: 1,
      },
    ],
  }

  // 차트 옵션 적용
  passChart.setOption(option)
  console.log('PassSchedule 차트 옵션 적용됨')

  // 명시적으로 리사이즈 호출
  setTimeout(() => {
    passChart?.resize()
  }, 0)

  // 윈도우 리사이즈 이벤트에 대응
  window.addEventListener('resize', () => {
    passChart?.resize()
  })
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

// 🆕 PassSchedule 차트 업데이트
const updateChart = () => {
  if (!passChart) return

  try {
    const azimuth = parseFloat(icdStore.azimuthAngle) || 0
    const elevation = parseFloat(icdStore.elevationAngle) || 0

    const normalizedAz = azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))

    // 현재 위치 업데이트
    currentPosition.value = { azimuth: normalizedAz, elevation: normalizedEl }

    // 🆕 passScheduleStatus가 true일 때만 추적 경로 업데이트 (EphemerisDesignationPage와 동일)
    if (icdStore.passScheduleStatusInfo?.isActive === true) {
      passScheduleStore.updateActualTrackingPath(normalizedAz, normalizedEl)
    }

    // 🆕 최적화된 차트 업데이트 (EphemerisDesignationPage와 동일)
    const actualPath = passScheduleStore.actualTrackingPath
    const predictedPath = passScheduleStore.predictedTrackingPath

    // passScheduleStatus가 true일 때만 추적 경로 표시 (EphemerisDesignationPage와 동일)
    if (icdStore.passScheduleStatusInfo?.isActive === true && actualPath && actualPath.length > 0) {
      passChartPool.updateTrackingPath(actualPath as [number, number][])
    }

    // 예정 궤적 추가 (위치 선 제거됨)
    const updateOption = {
      series: [
        {
          data: [[normalizedEl, normalizedAz]], // 현재 위치
        },
        {
          data: icdStore.passScheduleStatusInfo?.isActive === true ? actualPath : [], // passScheduleStatus가 true일 때만 실시간 추적 경로 표시
        },
        {
          data: predictedPath, // 예정 위성 궤적
        },
      ],
    }

    // 차트가 여전히 존재하는지 확인
    if (passChart && !passChart.isDisposed()) {
      passChart.setOption(updateOption, false, true)
    }
  } catch (error) {
    console.error('PassSchedule 차트 업데이트 오류:', error)
  }
}

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

// 🆕 실제 추적 경로 초기화 (Store 통해서)
const clearActualPath = () => {
  passScheduleStore.clearTrackingPaths()
  updateChart()
}

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
      currentTimeResult: ephemerisStore.offsetValues.timeResult,
    })

    const numValue = Number(parseFloat(value).toFixed(2)) || 0
    console.log('계산된 numValue:', numValue)

    const offsetTypes = ['azimuth', 'elevation', 'tilt', 'time'] as const
    const offsetType = offsetTypes[index]

    if (!offsetType) {
      console.error('Invalid offset index:', index)
      return
    }

    if (index === 3) {
      const timeInputValue = inputs.value[3] || '0.00'
      ephemerisStore.updateOffsetValues('time', timeInputValue)
      try {
        await ephemerisStore.sendTimeOffset(numValue)
        ephemerisStore.updateOffsetValues('timeResult', numValue.toFixed(2))
        console.log('Time Result 업데이트:', numValue.toFixed(2))
      } catch (error) {
        console.error('Time offset command failed:', error)
      }
      return
    }

    // Position Offset 처리 (azimuth, elevation, tilt)
    ephemerisStore.updateOffsetValues(offsetType, numValue.toFixed(2))

    const azOffset = Number((parseFloat(ephemerisStore.offsetValues.azimuth) || 0).toFixed(2))
    const elOffset = Number((parseFloat(ephemerisStore.offsetValues.elevation) || 0).toFixed(2))
    const tiOffset = Number((parseFloat(ephemerisStore.offsetValues.tilt) || 0).toFixed(2))

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
    // 🆕 추적 모니터링 중지
    await passScheduleStore.stopTrackingMonitor()

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
}
// 초기화
const init = async () => {
  console.log('PassSchedulePage 초기화 시작')

  setTimeout(() => {
    initChart()
  }, 100)

  // Store 초기화 호출
  try {
    await passScheduleStore.init() // 🔧 Store의 init 메서드 직접 호출
    console.log('✅ 스케줄 데이터 로드 완료:', passScheduleStore.scheduleData.length, '개')
  } catch (error) {
    console.error('스케줄 데이터 로드 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 데이터를 불러오는데 실패했습니다',
    })
  }
}
// 컴포넌트 마운트
onMounted(async () => {
  console.log('PassSchedulePage 컴포넌트 마운트됨')
  await init()

  // 🆕 PassSchedule 차트 초기화
  initChart()

  // 차트 업데이트 타이머 시작 (PassSchedule 독립적)
  updateTimer = window.setInterval(() => {
    updateChart()
  }, 100)
})

// 컴포넌트 언마운트
onUnmounted(() => {
  console.log('PassSchedulePage 컴포넌트 언마운트됨')

  // 차트 업데이트 타이머 정리
  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
  }

  // 🆕 PassSchedule 차트 인스턴스 정리
  if (passChart) {
    passChart.dispose()
    passChart = null
  }

  // 시간 업데이트 타이머 정리
  stopTimeTimer()

  // 🆕 추적 경로 데이터 정리 (Store 통해서)
  passScheduleStore.clearTrackingPaths()

  window.removeEventListener('resize', () => { })
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
/* ===== 1. 기본 컨테이너 스타일 ===== */
.pass-schedule-mode {
  height: 100%;
  width: 100%;
  padding: 0;
  margin: 0;
}

.schedule-container {
  padding: 0.5rem 1rem 1rem 1rem;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
  margin-bottom: 1rem;
}

/* ===== 2. 컨트롤 섹션 기본 스타일 ===== */
.control-section {
  height: 500px;
  width: 100%;
  background-color: var(--q-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  border-radius: 4px;
}

.control-section .q-card-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 1rem;
}

/* ===== 3. 차트 영역 스타일 ===== */
.chart-area {
  height: 400px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.2);
}

/* ===== 4. 컨트롤 카드 스타일 ===== */
.control-card {
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.control-card .q-card-section {
  padding: 0.75rem;
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
  flex: 1;
  overflow-y: auto;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
  height: 100%;
}

.schedule-info {
  padding: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.05);
  flex: 1;
  overflow-y: auto;
}

.no-schedule-selected {
  padding: 2rem;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.02);
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.info-label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  min-width: 80px;
}

.info-value {
  font-weight: 600;
  color: white;
  font-size: 13px;
  text-align: right;
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
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.schedule-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-schedule-display {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  background-color: rgba(25, 118, 210, 0.1);
  border-radius: 4px;
  border: 1px solid rgba(25, 118, 210, 0.3);
}

.registered-schedule-info {
  background-color: rgba(0, 0, 0, 0.8);
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.registered-schedule-info .text-body2 {
  margin-bottom: 2px;
  font-weight: 600;
  font-size: 12px;
  color: #2196f3;
}

.registered-schedule-info .text-caption {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* ===== 9. 테이블 기본 스타일 ===== */
.schedule-table {
  background-color: var(--q-dark);
  color: white;
  flex: 1;
  border-radius: 6px;
  overflow: hidden;
}

/* Quasar 테이블 기본 설정 초기화 */
.schedule-table :deep(.q-table__container) {
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.12);
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

/* 테이블 헤더 스타일 */
.schedule-table :deep(.q-table thead th) {
  background-color: rgba(255, 255, 255, 0.05);
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
  position: relative;
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
  font-size: 12px;
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
.schedule-table tbody tr.highlight-current-schedule .tilt {
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
.schedule-table tbody tr.highlight-next-schedule .tilt {
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
  font-size: 11px;
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
.tilt {
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  font-family: 'Courier New', monospace;
}

.max-elevation {
  color: #9c27b0;
}

.tilt {
  color: #607d8b;
}

/* ===== 14. 버튼 그룹 스타일 ===== */
.button-group {
  margin-top: 1rem;
  width: 100%;
  flex-shrink: 0;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.button-row {
  display: flex;
  gap: 0.75rem;
  width: 100%;
  margin-bottom: 1rem;
}

.control-button-row {
  display: flex;
  gap: 0.75rem;
  width: 100%;
}

/* 업로드 버튼 스타일 */
.upload-btn {
  flex: 1;
  min-width: 0;
  height: 48px;
  font-size: 14px;
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
  height: 40px;
  font-size: 14px;
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
    padding: 0.25rem;
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

/* 컬럼 비율 조정 */
.col-md-2 {
  width: 21.6667% !important;
  padding: 4px;
}

.col-md-4 {
  width: 33.3333% !important;
  padding: 4px;
}

.col-md-6 {
  width: 45% !important;
  padding: 4px;
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
