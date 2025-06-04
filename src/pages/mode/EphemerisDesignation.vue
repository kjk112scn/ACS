<template>
  <div class="ephemeris-mode">
    <div class="section-title text-h5 text-primary q-mb-sm">Ephemeris Designation</div>

    <!-- Control Section -->
    <div class="row q-col-gutter-md q-mb-md">
      <!-- Azimuth Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="bg-blue-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Azimuth</div>
          </q-card-section>
          <q-card-section>
            <q-input
              v-model="inputs[0]"
              @input="(val: string) => onInputChange(0, val)"
              dense
              outlined
              type="number"
              step="0.01"
              class="q-mb-sm"
            />
            <div class="row q-gutter-xs">
              <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(0)" />
              <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(0)" />
              <q-space />
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(0)" />
            </div>
            <q-input v-model="outputs[0]" dense outlined readonly label="Output" class="q-mt-sm" />
            <!-- 증가/감소 값 설정 -->
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
            <q-input
              v-model="inputs[1]"
              @input="(val: string) => onInputChange(1, val)"
              dense
              outlined
              type="number"
              step="0.01"
              class="q-mb-sm"
            />
            <div class="row q-gutter-xs">
              <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(1)" />
              <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(1)" />
              <q-space />
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(1)" />
            </div>
            <q-input v-model="outputs[1]" dense outlined readonly label="Output" class="q-mt-sm" />
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
            <q-input
              v-model="inputs[2]"
              @input="(val: string) => onInputChange(2, val)"
              dense
              outlined
              type="number"
              step="0.01"
              class="q-mb-sm"
            />
            <div class="row q-gutter-xs">
              <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(2)" />
              <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(2)" />
              <q-space />
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(2)" />
            </div>
            <q-input v-model="outputs[2]" dense outlined readonly label="Output" class="q-mt-sm" />
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
            <q-input
              v-model="inputs[3]"
              @input="(val: string) => onInputChange(3, val)"
              dense
              outlined
              type="number"
              step="0.01"
              class="q-mb-sm"
            />
            <div class="row q-gutter-xs">
              <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(3)" />
              <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(3)" />
              <q-space />
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(3)" />
            </div>
            <q-input v-model="outputs[3]" dense outlined readonly label="Result" class="q-mt-sm" />
            <q-input
              v-model="formattedCalTime"
              dense
              outlined
              readonly
              label="Cal Time"
              class="q-mt-sm"
            />
          </q-card-section>
        </q-card>
      </div>
    </div>
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

        <!-- 2번 영역: 계산 정보 표시 영역 수정 -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Tracking Information</div>
              <div class="ephemeris-form">
                <div class="form-row">
                  <!-- 추가 정보 표시 영역 -->
                  <div v-if="selectedScheduleInfo.satelliteName" class="schedule-info q-mt-md">
                    <div class="text-subtitle2 text-weight-bold text-primary q-mb-sm">
                      선택된 스케줄 정보
                    </div>

                    <div class="info-row">
                      <span class="info-label">위성 이름:</span>
                      <span class="info-value">{{ selectedScheduleInfo.satelliteName }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">위성 ID:</span>
                      <span class="info-value">{{ selectedScheduleInfo.satelliteId }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">시작 시간:</span>
                      <span class="info-value">{{
                        formatToLocalTime(selectedScheduleInfo.startTime)
                      }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">종료 시간:</span>
                      <span class="info-value">{{
                        formatToLocalTime(selectedScheduleInfo.endTime)
                      }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">지속 시간:</span>
                      <span class="info-value">{{ selectedScheduleInfo.duration }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">시작 방위각/고도:</span>
                      <span class="info-value"
                        >{{ selectedScheduleInfo.startAzimuth }}° /
                        {{ selectedScheduleInfo.startElevation }}°</span
                      >
                    </div>

                    <div class="info-row">
                      <span class="info-label">종료 방위각/고도:</span>
                      <span class="info-value"
                        >{{ selectedScheduleInfo.endAzimuth }}° /
                        {{ selectedScheduleInfo.endElevation }}°</span
                      >
                    </div>

                    <div class="info-row">
                      <span class="info-label">최대 고도:</span>
                      <span class="info-value">{{ selectedScheduleInfo.maxElevation }}°</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">남은 시간:</span>
                      <span
                        class="info-value"
                        :class="{
                          'text-negative': timeRemaining < 0,
                          'text-positive': timeRemaining > 0,
                          'text-grey': timeRemaining === 0,
                        }"
                      >
                        {{ formatTimeRemaining(timeRemaining) }}
                      </span>
                    </div>
                  </div>
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
              <div class="button-group q-mt-md">
                <q-btn color="primary" label="Text" @click="openTLEModal" class="q-mr-sm" />
                <q-btn
                  color="primary"
                  label="Select Schedule"
                  @click="openScheduleModal"
                  class="q-mr-sm"
                />
                <!-- ✅ 다운로드 버튼 추가 -->
                <q-btn
                  color="secondary"
                  label="Download"
                  icon="download"
                  @click="downloadRealtimeData"
                  class="q-mr-sm"
                />
              </div>
              <!-- 버튼 그룹 추가 -->
              <div class="button-group q-mt-md">
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
  </div>

  <!-- TLE 입력 모달 -->
  <q-dialog v-model="showTLEModal" persistent>
    <q-card class="q-pa-md" style="width: 700px; max-width: 95vw">
      <q-card-section class="bg-primary text-white">
        <div class="text-h6">TLE 입력</div>
      </q-card-section>

      <q-card-section class="q-pa-md">
        <div class="text-body2 q-mb-md">
          2줄 또는 3줄 형식의 TLE 데이터를 입력하세요. 3줄 형식인 경우 첫 번째 줄은 위성 이름으로
          처리됩니다.
          <br />예시:
          <pre
            class="q-mt-sm q-pa-sm bg-grey-9 text-white rounded-borders"
            style="font-size: 0.8rem; white-space: pre-wrap"
          >
ISS (ZARYA)
1 25544U 98067A   24054.51736111  .00020125  00000+0  36182-3 0  9999
2 25544  51.6416 142.1133 0003324 324.9821 218.2594 15.49780383446574</pre
          >
        </div>
        <div class="tle-input-container q-mb-md">
          <q-input
            v-model="tempTLEData.line1"
            type="textarea"
            filled
            autogrow
            class="tle-textarea full-width"
            style="min-height: 200px; font-family: monospace; font-size: 0.9rem"
            placeholder="TLE 데이터를 여기에 붙여넣으세요..."
            :input-style="'white-space: pre;'"
            spellcheck="false"
            autofocus
            :error="tleError !== null"
            :error-message="tleError || undefined"
            @keydown.ctrl.enter="addTLEData"
          />
        </div>
      </q-card-section>

      <q-card-actions align="right" class="q-px-md q-pb-md">
        <q-btn
          flat
          label="추가"
          color="primary"
          @click="addTLEData"
          :loading="isProcessingTLE"
          :disable="!tempTLEData.line1.trim()"
        />
        <q-btn
          flat
          label="닫기"
          color="primary"
          v-close-popup
          class="q-ml-sm"
          :disable="isProcessingTLE"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- 스케줄 선택 모달 -->
  <q-dialog v-model="showScheduleModal" persistent maximized>
    <q-card class="q-pa-md" style="width: 1200px; max-width: 98vw; max-height: 70vh">
      <q-card-section class="bg-primary text-white">
        <div class="text-h6">Select Schedule</div>
      </q-card-section>

      <q-card-section class="q-pa-md" style="max-height: 50vh; overflow: auto">
        <q-table
          :rows="scheduleData"
          :columns="scheduleColumns"
          row-key="No"
          :loading="loadingSchedule"
          :pagination="{ rowsPerPage: 10 }"
          selection="single"
          v-model:selected="selectedSchedule"
          class="bg-grey-9 text-white"
          dark
          flat
          bordered
        >
          <template v-slot:loading>
            <q-inner-loading showing color="primary">
              <q-spinner size="50px" color="primary" />
            </q-inner-loading>
          </template>
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          flat
          label="Select"
          color="primary"
          @click="selectSchedule"
          :disable="selectedSchedule.length === 0"
        />
        <q-btn flat label="Close" color="primary" v-close-popup class="q-ml-sm" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { date } from 'quasar'

import type { QTableProps } from 'quasar'
import { useICDStore } from '../../stores/API/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { useEphemerisTrackStore } from '../../stores/API/ephemerisTrackStore'
import { formatToLocalTime, formatTimeRemaining, getCalTimeTimestamp } from '../../utils/times'
// 인터페이스 정의 - 서비스의 타입과 동일하게 사용
import {
  ephemerisTrackService,
  type ScheduleItem,
  type RealtimeTrackingDataItem,
} from '../../services/ephemerisTrackService'
// ✅ 스토어 연동 추가
const ephemerisStore = useEphemerisTrackStore()

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

// 스토어 인스턴스 생성
const icdStore = useICDStore()

// TLE 데이터 인터페이스 정의
interface TLEData {
  displayText: string
  tleLine1: string | undefined
  tleLine2: string | undefined
  satelliteName: string | null | undefined
  startTime?: string
  endTime?: string
  stepSize?: number
}

// 차트 데이터용 인터페이스
interface TrajectoryPoint {
  Time: string
  Azimuth: number
  Elevation: number
  [key: string]: string | number | boolean | null | undefined
}

// 차트 관련 변수
const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null
let updateTimer: number | null = null

// TLE 데이터 상태
const tleData = ref<TLEData>({
  displayText: 'No TLE data available',
  tleLine1: undefined,
  tleLine2: undefined,
  satelliteName: undefined,
})

// Ephemeris Designation 모드 데이터 - 현재 위치 정보 표시용
const currentPosition = ref({
  azimuth: 0,
  elevation: 0,
  tilt: 0,
  date: date.formatDate(new Date(), 'YYYY/MM/DD'),
  time: date.formatDate(new Date(), 'HH:mm'),
})

// ✅ 스토어 상태 연동 - 탭 이동 시에도 데이터 유지
const showScheduleModal = ref(false)

const scheduleData = computed(() => ephemerisStore.masterData)
const selectedSchedule = ref<ScheduleItem[]>([])
const loadingSchedule = ref(false)

// TLE 모달 관련 상태
const showTLEModal = ref(false)
const tempTLEData = ref({
  line1: '',
})

// TLE 관련 상태
const tleError = ref<string | null>(null)
const isProcessingTLE = ref(false)

// QTable 컬럼 타입 정의
type QTableColumn = NonNullable<QTableProps['columns']>[0]

// 스케줄 테이블 컬럼 정의
const scheduleColumns: QTableColumn[] = [
  { name: 'No', label: 'No', field: 'No', align: 'left', sortable: true },
  {
    name: 'SatelliteName',
    label: '위성 이름',
    field: 'SatelliteName',
    align: 'left',
    sortable: true,

    format: (val, row) => val || row.SatelliteID || '이름 없음',
  },
  {
    name: 'StartTime',
    label: '시작 시간',
    field: 'StartTime',
    align: 'left',
    sortable: true,
    format: (val) => formatToLocalTime(val),
  },
  {
    name: 'EndTime',
    label: '종료 시간',
    field: 'EndTime',
    align: 'left',
    sortable: true,
    format: (val) => formatToLocalTime(val),
  },
  { name: 'Duration', label: '지속 시간', field: 'Duration', align: 'left', sortable: true },
  {
    name: 'MaxElevation',
    label: '최대 고도 (°)',
    field: 'MaxElevation',
    align: 'left',
    sortable: true,
  },
]

// 입력 및 출력 필드 (배열로 관리)
const inputs = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])
const outputs = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])

// Quasar 인스턴스 가져오기
import { useQuasar } from 'quasar'

const $q = useQuasar()

// ✅ 스토어에서 선택된 스케줄 정보 가져오기 - 탭 이동 시에도 유지
const selectedScheduleInfo = computed(() => {
  const selected = ephemerisStore.selectedSchedule
  if (selected) {
    return {
      passId: selected.No,
      satelliteName: selected.SatelliteName || selected.SatelliteID || '알 수 없음',
      satelliteId: selected.SatelliteID || 'N/A',
      startTime: selected.StartTime,
      endTime: selected.EndTime,
      duration: selected.Duration,
      maxElevation: typeof selected.MaxElevation === 'number' ? selected.MaxElevation : 0,
      startTimeMs: new Date(selected.StartTime).getTime(),
      timeRemaining: 0,
      startAzimuth: typeof selected.StartAzimuth === 'number' ? selected.StartAzimuth : 0,
      endAzimuth: typeof selected.EndAzimuth === 'number' ? selected.EndAzimuth : 0,
      startElevation: typeof selected.StartElevation === 'number' ? selected.StartElevation : 0,
      endElevation: typeof selected.EndElevation === 'number' ? selected.EndElevation : 0,
    }
  }

  return {
    passId: 0,
    satelliteName: '',
    satelliteId: '',
    startTime: '',
    endTime: '',
    duration: '',
    maxElevation: 0,
    startTimeMs: 0,
    timeRemaining: 0,
    startAzimuth: 0,
    endAzimuth: 0,
    startElevation: 0,
    endElevation: 0,
  }
})
const downloadRealtimeData = async () => {
  try {
    // Loading 대신 notify로 시작 알림
    $q.notify({
      type: 'info',
      message: '실시간 추적 데이터를 조회하고 있습니다...',
      timeout: 1000,
    })

    const response = await ephemerisTrackService.fetchRealtimeTrackingData()

    if (response.data && response.data.length > 0) {
      downloadCSV(response.data)

      $q.notify({
        type: 'positive',
        message: `${response.totalCount}개의 실시간 추적 데이터를 다운로드했습니다`,
      })
    } else {
      $q.notify({
        type: 'warning',
        message: '다운로드할 실시간 추적 데이터가 없습니다',
      })
    }
  } catch (error) {
    console.error('실시간 추적 데이터 다운로드 실패:', error)
    $q.notify({
      type: 'negative',
      message: '실시간 추적 데이터 다운로드에 실패했습니다',
    })
  }
}
const downloadCSV = (data: RealtimeTrackingDataItem[]) => {
  // 안전한 숫자 포맷팅 함수
  const safeToFixed = (value: number | null | undefined, digits: number = 4): string => {
    if (value === null || value === undefined || isNaN(Number(value))) {
      return '0.0000'
    }
    return Number(value).toFixed(digits)
  }

  // CSV 헤더 정의
  const headers = [
    'Index',
    'Timestamp',
    'CMD Azimuth (°)',
    'CMD Elevation (°)',
    'Elapsed Time (s)',
    'Tracking Azimuth Time (s)',
    'Tracking CMD Azimuth Angle (°)',
    'Tracking Actual Azimuth Angle (°)',
    'Tracking Elevation Time (s)',
    'Tracking CMD Elevation Angle (°)',
    'Tracking Actual Elevation Angle (°)',
    'Tracking Tilt Time (s)',
    'Tracking CMD Tilt Angle (°)',
    'Tracking Actual Tilt Angle (°)',
    'Pass ID',
    'Azimuth Error (°)',
    'Elevation Error (°)',
  ]

  // CSV 데이터 생성 (안전한 처리 적용)
  const csvContent = [
    headers.join(','),
    ...data.map((item) =>
      [
        item.index || 0,
        `"${item.timestamp ? formatToLocalTime(item.timestamp) : new Date().toISOString()}"`,
        safeToFixed(item.cmdAz, 4),
        safeToFixed(item.cmdEl, 4),
        safeToFixed(item.elapsedTimeSeconds, 2),
        safeToFixed(item.trackingAzimuthTime, 2),
        safeToFixed(item.trackingCMDAzimuthAngle, 4),
        safeToFixed(item.trackingActualAzimuthAngle, 4),
        safeToFixed(item.trackingElevationTime, 2),
        safeToFixed(item.trackingCMDElevationAngle, 4),
        safeToFixed(item.trackingActualElevationAngle, 4),
        safeToFixed(item.trackingTiltTime, 2),
        safeToFixed(item.trackingCMDTiltAngle, 4),
        safeToFixed(item.trackingActualTiltAngle, 4),
        item.passId || 0,
        safeToFixed(item.azimuthError, 4),
        safeToFixed(item.elevationError, 4),
      ].join(','),
    ),
  ].join('\n')

  // BOM 추가 (한글 깨짐 방지)
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })

  // 파일명 생성 (현재 시간 포함)
  const now = new Date()
  const timestamp = now.toISOString().replace(/[:.]/g, '-').slice(0, 19)
  const filename = `realtime_tracking_data_${timestamp}.csv`

  // 다운로드 실행
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', filename)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 남은 시간 계산을 위한 상태
const timeRemaining = ref(0)
let timeUpdateTimer: number | null = null

// 추적 경로 저장을 위한 변수 추가 (개선된 버전)
const trackingPath = ref<[number, number][]>([])
const trackingPathRaw = ref<[number, number][]>([]) // 원본 데이터 저장용

// 경로 데이터 샘플링 함수 추가
const sampleTrackingPath = (
  data: [number, number][],
  maxPoints: number = 1000,
): [number, number][] => {
  if (data.length <= maxPoints) {
    return data
  }

  // 샘플링 비율 계산 (예: 10000개 -> 1000개면 10개 중 1개)
  const step = Math.ceil(data.length / maxPoints)
  const sampledData: [number, number][] = []

  // 첫 번째 점은 항상 포함

  const firstPoint = data[0]
  if (firstPoint) {
    sampledData.push(firstPoint)
  }

  // step 간격으로 샘플링
  for (let i = step; i < data.length - 1; i += step) {
    const point = data[i]
    if (point) {
      sampledData.push(point)
    }
  }

  // 마지막 점은 항상 포함 (현재 위치)
  if (data.length > 1) {
    const lastPoint = data[data.length - 1]
    if (lastPoint) {
      sampledData.push(lastPoint)
    }
  }

  return sampledData
}

// 차트 초기화 함수
const initChart = () => {
  if (!chartRef.value) return

  // 기존 차트 인스턴스가 있으면 제거
  if (chart) {
    chart.dispose()
  }

  // 차트 인스턴스 생성
  chart = echarts.init(chartRef.value)
  console.log('차트 인스턴스 생성됨')

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
      // ✅ 축 애니메이션 비활성화
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
      inverse: true, // 고도각은 위에서 아래로 증가
      // ✅ 축 애니메이션 비활성화
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
        // ✅ 축 애니메이션 비활성화
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
            return `Az: ${params.value[0].toFixed(2)}°\nEl: ${params.value[1].toFixed(2)}°`
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
        // ✅ 축 애니메이션 비활성화
        animation: false,
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
        animation: false,
        lineStyle: {
          color: '#ffffff',
          width: 3,
          opacity: 0.8,
        },
        data: [],
        zlevel: 2,
      },
      {
        name: '위성 궤적',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        // ✅ 축 애니메이션 비활성화
        animation: false,
        lineStyle: {
          color: '#2196f3',
          width: 2,
        },

        data: [], // 초기에는 빈 배열

        zlevel: 1,
      },
    ],
  }

  // 차트 옵션 적용
  chart.setOption(option)
  console.log('차트 옵션 적용됨')

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
  if (!chart) {
    console.error('차트가 초기화되지 않았습니다.')
    return
  }

  try {
    let azimuth = 0
    let elevation = 0
    if (icdStore.ephemerisStatusInfo.isActive === true) {
      azimuth = parseFloat(icdStore.trackingActualAzimuthAngle)
      elevation = parseFloat(icdStore.trackingActualElevationAngle)
    } else {
      azimuth = parseFloat(icdStore.azimuthAngle) || 0
      elevation = parseFloat(icdStore.elevationAngle) || 0
    }

    // ✅ DashboardPage와 동일한 정규화 방식 적용
    const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))

    // 현재 위치 정보 업데이트
    currentPosition.value.azimuth = azimuth
    currentPosition.value.elevation = elevation
    currentPosition.value.date = date.formatDate(new Date(), 'YYYY/MM/DD')
    currentPosition.value.time = date.formatDate(new Date(), 'HH:mm:ss')

    // ✅ 추적 중일 때 경로 저장 - 데이터 순서 수정
    if (icdStore.ephemerisStatusInfo.isActive === true) {
      // ✅ [elevation, azimuth] 순서로 변경 (극좌표계: [radius, angle])
      const currentPoint: [number, number] = [normalizedEl, normalizedAz]

      trackingPathRaw.value.push(currentPoint)

      if (trackingPathRaw.value.length > 50000) {
        trackingPathRaw.value = trackingPathRaw.value.slice(-50000)
      }

      trackingPath.value = sampleTrackingPath(trackingPathRaw.value, 1000)
    } else {
      trackingPath.value = []
      trackingPathRaw.value = []
    }

    // ✅ 차트 옵션 업데이트 - [elevation, azimuth] 순서로 변경
    const updateOption = {
      series: [
        {
          // ✅ [radius, angle] = [elevation, azimuth] 순서
          data: [[normalizedEl, normalizedAz]],
        },
        {}, // 두 번째 시리즈는 그대로 유지
        {
          // 세 번째 시리즈(실시간 추적 경로) 업데이트
          data: [...trackingPath.value],
        },
        {}, // 네 번째 시리즈는 그대로 유지
      ],
    } as unknown as Parameters<typeof chart.setOption>[0]

    chart.setOption(updateOption)
  } catch (error) {
    console.error('차트 업데이트 중 오류 발생:', error)
  }
}

// 궤적 라인을 차트에 추가하는 함수@
const updateChartWithTrajectory = (data: TrajectoryPoint[]) => {
  if (!chart) {
    console.error('차트가 초기화되지 않았습니다.')
    return
  }

  console.log('궤적 데이터 처리 시작:', data.length, '개의 포인트')

  try {
    const trajectoryPoints = data.map((point) => {
      const az = typeof point.Azimuth === 'number' ? point.Azimuth : 0
      const el = typeof point.Elevation === 'number' ? point.Elevation : 0

      // ✅ DashboardPage와 동일한 정규화 방식
      const normalizedAz = az < 0 ? az + 360 : az
      const normalizedEl = Math.max(0, Math.min(90, el))

      // ✅ [elevation, azimuth] 순서로 반환 (극좌표계: [radius, angle])
      return [normalizedEl, normalizedAz]
    })

    console.log('생성된 궤적 포인트 샘플:', trajectoryPoints.slice(0, 5))

    // 차트 옵션 업데이트 - 네 번째 시리즈(궤적 라인)만 업데이트
    const trajectoryOption = {
      series: [
        {}, // 첫 번째 시리즈는 그대로 유지
        {}, // 두 번째 시리즈는 그대로 유지
        {}, // 세 번째 시리즈는 그대로 유지
        {
          // 네 번째 시리즈(궤적 라인) 업데이트
          data: trajectoryPoints,
        },
      ],
    } as unknown as Parameters<typeof chart.setOption>[0]

    chart.setOption(trajectoryOption)

    console.log('차트 옵션 업데이트 완료')
  } catch (error) {
    console.error('차트 옵션 업데이트 중 오류 발생:', error)
  }
}
// ✅ 개선된 시간 계산 함수 수정
const updateTimeRemaining = () => {
  if (selectedScheduleInfo.value.startTimeMs > 0) {
    try {
      const currentCalTime = getCalTimeTimestamp(icdStore.resultTimeOffsetCalTime)
      const remainingMs = selectedScheduleInfo.value.startTimeMs - currentCalTime
      timeRemaining.value = remainingMs
    } catch (error) {
      console.error('시간 계산 오류:', error)
      const clientTime = Date.now()
      timeRemaining.value = Math.max(0, selectedScheduleInfo.value.startTimeMs - clientTime)
    }
  }
}

// ===== 스토어 연동 메서드들 =====

// ✅ 스케줄 데이터 로드 - 스토어 사용
const loadScheduleData = async () => {
  loadingSchedule.value = true
  try {
    await ephemerisStore.loadMasterData(true)
  } catch (error) {
    console.error('스케줄 데이터 로드 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 데이터를 불러오는데 실패했습니다',
    })
  } finally {
    loadingSchedule.value = false
  }
}

// ✅ 스케줄 선택 - 스토어에 저장하여 탭 이동 시에도 유지
const selectSchedule = async () => {
  if (selectedSchedule.value.length === 0) return

  try {
    const selectedItem = selectedSchedule.value[0]
    if (!selectedItem) return

    // 스토어에 선택된 스케줄 저장 (탭 이동 시에도 유지됨)
    await ephemerisStore.selectSchedule(selectedItem)

    // 상세 데이터 로드
    // 스토어의 detailData는 selectSchedule 메서드 내에서 이미 로드됨
    const detailData = ephemerisStore.detailData

    // 차트 업데이트
    if (detailData && detailData.length > 0 && chart) {
      updateChartWithTrajectory([...detailData] as TrajectoryPoint[])
    }

    $q.notify({
      type: 'positive',
      message: `${selectedItem.SatelliteName || selectedItem.SatelliteID} 스케줄이 선택되었습니다`,
    })

    showScheduleModal.value = false
  } catch (error) {
    console.error('스케줄 선택 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 선택에 실패했습니다',
    })
  }
}

// ===== 기존 메서드들 유지 =====

// 입력값 업데이트 함수들
// 증가 함수 - 입력된 값만큼 증가
const increment = async (index: number) => {
  // 현재 출력값 (현재 상태)
  const currentOutput = parseFloat(outputs.value[index] || '0')

  // 입력된 값 (증가량)
  const inputValue = parseFloat(inputs.value[index] || '0')

  // 새로운 값 계산 (현재 출력값 + 입력된 값)
  const newValue = (currentOutput + inputValue).toFixed(2)

  // 출력값 업데이트
  outputs.value[index] = newValue

  // 오프셋 업데이트 (서버에 전송)
  await updateOffset(index, newValue)
}

// 감소 함수 - 입력된 값만큼 감소
const decrement = async (index: number) => {
  // 현재 출력값 (현재 상태)
  const currentOutput = parseFloat(outputs.value[index] || '0')

  // 입력된 값 (감소량)
  const inputValue = parseFloat(inputs.value[index] || '0')

  // 새로운 값 계산 (현재 출력값 - 입력된 값)
  const newValue = (currentOutput - inputValue).toFixed(2)

  // 출력값 업데이트
  outputs.value[index] = newValue

  // 오프셋 업데이트 (서버에 전송)
  await updateOffset(index, newValue)
}

// 리셋 함수
const reset = async (index: number) => {
  inputs.value[index] = '0.00'
  await updateOffset(index, '0.00')
}

// 오프셋 업데이트 함수
const updateOffset = async (index: number, value: string) => {
  try {
    // Parse the new value, default to 0 if invalid
    const numValue = Number(parseFloat(value).toFixed(2)) || 0

    // Update the output value
    outputs.value[index] = numValue.toFixed(2)

    // For time offset (index 3), call the time offset command
    if (index === 3) {
      await ephemerisTrackService.sendTimeOffsetCommand(numValue)
      return
    }

    // For position offsets (azimuth, elevation, tilt)

    const azOffset = Number((parseFloat(outputs.value[0] || '0') || 0).toFixed(2))
    const elOffset = Number((parseFloat(outputs.value[1] || '0') || 0).toFixed(2))
    const tiOffset = Number((parseFloat(outputs.value[2] || '0') || 0).toFixed(2))

    // Send position offset command
    await icdStore.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)
  } catch (error) {
    console.error('Error updating offset:', error)
  }
}

// 입력값이 변경될 때 호출되는 함수
const onInputChange = (index: number, value: string) => {
  inputs.value[index] = value
  void updateOffset(index, value)
}

// 서버 시간 포맷팅을 위한 계산된 속성
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

// TLE 관련 함수들
const openTLEModal = () => {
  showTLEModal.value = true
  tempTLEData.value.line1 = ''
  tleError.value = null
}

const addTLEData = async () => {
  if (!tempTLEData.value.line1.trim()) {
    tleError.value = 'TLE 데이터를 입력하세요'
    return
  }

  isProcessingTLE.value = true
  tleError.value = null

  try {
    // TLE 데이터 직접 처리
    await ephemerisStore.processTLEData(tempTLEData.value.line1)

    // 처리된 TLE 데이터 저장 (UI 표시용)
    // TLE 데이터 저장 (UI 표시용)
    tleData.value = {
      displayText: tempTLEData.value.line1,
      tleLine1: tempTLEData.value.line1,
      tleLine2: '',
      satelliteName: 'Unknown',
      startTime: new Date().toISOString(),
      endTime: new Date(Date.now() + 86400000).toISOString(),
      stepSize: 60,
    }

    $q.notify({
      type: 'positive',
      message: 'TLE 데이터가 성공적으로 처리되었습니다',
    })

    showTLEModal.value = false
  } catch (error) {
    console.error('TLE 처리 실패:', error)
    tleError.value = error instanceof Error ? error.message : 'TLE 데이터 처리에 실패했습니다'
  } finally {
    isProcessingTLE.value = false
  }
}

// 스케줄 모달 관련
const openScheduleModal = async () => {
  showScheduleModal.value = true

  // 데이터가 없으면 로드
  if (ephemerisStore.masterData.length === 0) {
    await loadScheduleData()
  }
}

// 명령 실행 함수들
const handleEphemerisCommand = async () => {
  try {
    if (!selectedScheduleInfo.value.passId) {
      $q.notify({
        type: 'warning',
        message: '먼저 스케줄을 선택하세요',
      })
      return
    }

    await ephemerisStore.startTracking()

    $q.notify({
      type: 'positive',
      message: 'Ephemeris 추적이 시작되었습니다',
    })
  } catch (error) {
    console.error('Failed to start ephemeris tracking:', error)
    $q.notify({
      type: 'negative',
      message: 'Ephemeris 추적 시작에 실패했습니다',
    })
  }
}

const handleStopCommand = async () => {
  try {
    await icdStore.stopCommand(true, true, true)
    await ephemerisStore.stopTracking()

    $q.notify({
      type: 'positive',
      message: '정지 명령이 전송되었습니다',
    })
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

// ===== 라이프사이클 훅 =====

onMounted(async () => {
  console.log('EphemerisDesignation 컴포넌트 마운트됨')

  // 차트 초기화
  setTimeout(() => {
    initChart()
  }, 100)

  // ✅ 스토어에 데이터가 없으면 로드 (탭 이동 시에도 데이터 유지)
  if (ephemerisStore.masterData.length === 0) {
    await loadScheduleData()
  }

  // ✅ 이미 선택된 스케줄이 있으면 차트 업데이트
  if (ephemerisStore.selectedSchedule && ephemerisStore.detailData.length > 0) {
    setTimeout(() => {
      if (chart) {
        updateChartWithTrajectory([...ephemerisStore.detailData] as TrajectoryPoint[])
      }
    }, 200)
  }

  // 차트 업데이트 타이머 시작
  updateTimer = window.setInterval(() => {
    updateChart()
    updateTimeRemaining()
  }, 100)

  // 시간 업데이트 타이머 시작
  timeUpdateTimer = window.setInterval(() => {
    updateTimeRemaining()
  }, 1000)
})

onUnmounted(() => {
  console.log('EphemerisDesignation 컴포넌트 언마운트됨')

  // 타이머 정리
  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
  }

  if (timeUpdateTimer) {
    clearInterval(timeUpdateTimer)
    timeUpdateTimer = null
  }

  // 차트 정리
  if (chart) {
    chart.dispose()
    chart = null
  }

  // 윈도우 이벤트 리스너 정리
  window.removeEventListener('resize', () => {})
})

// ✅ 스토어 상태 변화 감시 - 다른 탭에서 선택한 스케줄 반영
watch(
  () => ephemerisStore.selectedSchedule,
  (newSchedule) => {
    if (newSchedule && chart) {
      // 선택된 스케줄이 변경되면 차트 업데이트
      setTimeout(() => {
        if (ephemerisStore.detailData.length > 0) {
          updateChartWithTrajectory([...ephemerisStore.detailData] as TrajectoryPoint[])
        }
      }, 100)
    }
  },
  { immediate: true },
)

// ✅ 상세 데이터 변화 감시 - 차트 업데이트
watch(
  () => ephemerisStore.detailData,
  (newDetailData) => {
    if (newDetailData.length > 0 && chart) {
      setTimeout(() => {
        updateChartWithTrajectory([...newDetailData] as TrajectoryPoint[])
      }, 100)
    }
  },
  { immediate: true },
)
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

.tle-editor {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
}

.tle-display {
  font-family: monospace !important;
  background-color: var(--q-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  min-height: 80px;
}

/* 스케줄 테이블 스타일 */
.schedule-table {
  background-color: var(--q-dark);
  color: white;
}
</style>

<style>
/* 전역 스타일 - 깊은 선택자가 필요한 경우 */
.q-field__control {
  padding: 0 8px;
}

.q-card__section {
  padding: 16px;
}

.q-card {
  background: var(--q-dark);
  box-shadow:
    0 1px 5px rgb(0 0 0 / 20%),
    0 2px 2px rgb(0 0 0 / 14%),
    0 3px 1px -2px rgb(0 0 0 / 12%);
}

.col-md-4 {
  width: 33.3333%;
  padding: 4px;
}

.q-btn {
  flex: 1;
}

.tle-editor .q-editor__content {
  font-family: monospace !important;
  line-height: 1.5;
  padding: 12px;
}

.tle-display .q-editor__content {
  font-family: monospace !important;
  color: #fff;
  padding: 12px;
  line-height: 1.5;
}

.tle-display.q-editor--readonly {
  border-color: rgba(255, 255, 255, 0.12);
}

.tle-content {
  font-family: monospace !important;
  font-size: 14px;
  white-space: pre;
}

/* 스케줄 테이블 스타일 */
.schedule-table .q-table__top,
/* 테이블 스타일 */
.schedule-table {
  /* Quasar의 dark 테마와 통합 */
}

/* TLE 에디터 스타일 */
.q-editor.bg-grey-9 {
  border: 1px solid var(--q-dark);
  border-radius: 4px;
}

/* 다크 모드에서의 입력 필드 스타일 */
.q-field--dark .q-field__control {
  color: white;
}

/* 모달 내부 여백 조정 */
.q-dialog__inner--minimized > div {
  padding: 16px;
}
</style>
