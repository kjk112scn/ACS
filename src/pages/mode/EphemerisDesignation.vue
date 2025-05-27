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
            <q-input v-model="inputs[0]" dense outlined type="number" step="0.01" class="q-mb-sm" />
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
            <q-input v-model="inputs[1]" dense outlined type="number" step="0.01" class="q-mb-sm" />
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
            <q-input v-model="inputs[2]" dense outlined type="number" step="0.01" class="q-mb-sm" />
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
            <q-input v-model="inputs[3]" dense outlined type="number" step="0.01" class="q-mb-sm" />
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
import type { QTableProps } from 'quasar' // QTableProps 타입 임포트
import { useICDStore } from '../../stores/API/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { ephemerisTrackService } from '../../services/ephemerisTrackService' // 서비스 임포트
import { formatToLocalTime, formatTimeRemaining, getCalTimeTimestamp } from '../../utils/times'

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

// 인터페이스 정의 - 서비스의 타입과 동일하게 사용
// 임포트하는 대신 동일한 구조로 정의하여 타입 충돌 방지
import type { ScheduleItem } from '../../services/ephemerisTrackService'

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

// Ephemeris Designation 모드 데이터
const ephemerisData = ref({
  azimuth: 0,
  elevation: 0,
  tilt: 0,
  date: date.formatDate(new Date(), 'YYYY/MM/DD'),
  time: date.formatDate(new Date(), 'HH:mm'),
})

// 스케줄 관련 상태
const showScheduleModal = ref(false)
const scheduleData = ref<ScheduleItem[]>([])
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
    format: (val, row) => val || row.SatelliteID || '이름 없음', // 위성 이름이 없으면 Satellite ID 표시, 둘 다 없으면 '이름 없음' 표시
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
// 각 컨트롤의 증가/감소 값 설정 - ref로 정의하여 반응형으로 만듦

// Quasar 인스턴스 가져오기
import { useQuasar } from 'quasar'

const $q = useQuasar()

// 선택된 스케줄 정보를 저장할 상태 추가
const selectedScheduleInfo = ref({
  passId: 0, // passId 추가
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
})

// 남은 시간 계산을 위한 상태
const timeRemaining = ref(0)
let timeUpdateTimer: number | null = null

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
      {
        name: '위성 궤적',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        lineStyle: {
          color: '#2196f3',
          width: 2,
        },

        data: [], // 초기에는 빈 배열
        zlevel: 0,
      },
    ],
    animation: true,
    animationDuration: 150,
    animationEasing: 'linear',
    animationThreshold: 2000,
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
  if (!chart) return

  try {
    // ICD 스토어의 값으로 차트 업데이트
    const azimuth = Number(icdStore.azimuthAngle)
    const elevation = Number(icdStore.elevationAngle)

    // 방위각이 음수인 경우 0-360 범위로 변환
    const normalizedAzimuth = azimuth < 0 ? azimuth + 360 : azimuth % 360

    // 고도각이 음수인 경우 0으로 처리 (차트에서는 0-90만 표시)
    const normalizedElevation = Math.max(0, Math.min(90, elevation))

    // 차트 데이터 업데이트 - 포인트와 선만 업데이트
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
        // 세 번째 시리즈(궤적 라인)는 생략하여 기존 데이터 유지
      ],
    })
  } catch (error) {
    console.error('차트 업데이트 중 오류 발생:', error)
  }
}

// 궤적 라인을 차트에 추가하는 함수
const updateChartWithTrajectory = (data: TrajectoryPoint[]) => {
  if (!chart) {
    console.error('차트가 초기화되지 않았습니다.')
    return
  }

  console.log('궤적 데이터 처리 시작:', data.length, '개의 포인트')

  try {
    // 궤적 데이터 포인트 생성 (방위각, 고도각만 사용)
    const trajectoryPoints = data.map((point) => {
      // 유효한 숫자인지 확인
      const az = typeof point.Azimuth === 'number' ? point.Azimuth : 0
      const el = typeof point.Elevation === 'number' ? point.Elevation : 0

      // 방위각이 음수인 경우 0-360 범위로 변환
      const normalizedAz = az < 0 ? az + 360 : az % 360

      // 고도각이 음수인 경우 0으로 처리 (차트에서는 0-90만 표시)
      const normalizedEl = Math.max(0, Math.min(90, el))

      return [normalizedAz, normalizedEl]
    })

    console.log('생성된 궤적 포인트 샘플:', trajectoryPoints.slice(0, 5))

    // 차트 옵션 업데이트 - 세 번째 시리즈(궤적 라인)만 업데이트
    chart.setOption({
      series: [
        {}, // 첫 번째 시리즈는 그대로 유지
        {}, // 두 번째 시리즈는 그대로 유지
        {
          // 세 번째 시리즈(궤적 라인) 업데이트
          type: 'line',
          data: trajectoryPoints,
        },
      ],
    })

    console.log('차트 옵션 업데이트 완료')
  } catch (error) {
    console.error('차트 옵션 업데이트 중 오류 발생:', error)
  }
} 
// ✅ 개선된 시간 계산 함수 수정
const updateTimeRemaining = () => {
  if (selectedScheduleInfo.value.startTimeMs > 0) {
    try {
      // times.ts의 함수 사용
      const currentCalTime = getCalTimeTimestamp(icdStore.resultTimeOffsetCalTime)

      // 스케줄 시작 시간을 Date 객체로 변환 (KST)
      const scheduleStartTime = new Date(selectedScheduleInfo.value.startTime)

      // 디버깅을 위한 로그
      console.log('시간 계산:', {
        calTime: icdStore.resultTimeOffsetCalTime,
        currentCalTime: new Date(currentCalTime).toISOString(),
        scheduleStartTime: scheduleStartTime.toString(),
        scheduleStartTimeMs: selectedScheduleInfo.value.startTimeMs,
      })

      // 남은 시간 계산 (밀리초 단위)
      const remainingMs = selectedScheduleInfo.value.startTimeMs - currentCalTime
      timeRemaining.value = remainingMs

      // 1분마다 로그 출력 (디버깅용)
      if (Date.now() % 60000 < 30) {
        console.log('⏰ 남은 시간:', {
          calTime: icdStore.resultTimeOffsetCalTime,
          currentCalTime: new Date(currentCalTime).toISOString(),
          scheduleStart: new Date(selectedScheduleInfo.value.startTimeMs).toISOString(),
          remaining: formatTimeRemaining(remainingMs),
          remainingMs,
        })
      }
    } catch (error) {
      console.error('시간 계산 오류:', error)
      // 에러 발생 시 클라이언트 시간으로 대체
      const clientTime = Date.now()
      timeRemaining.value = Math.max(0, selectedScheduleInfo.value.startTimeMs - clientTime)
    }
  } else {
    timeRemaining.value = 0
  }
}
// 스케줄 모달 열기 및 데이터 로드
const openScheduleModal = async () => {
  showScheduleModal.value = true
  await loadScheduleData()
}

// 스케줄 데이터 로드 함수
const loadScheduleData = async () => {
  loadingSchedule.value = true
  try {
    // 서비스를 통해 마스터 데이터 로드
    const data = await ephemerisTrackService.fetchEphemerisMasterData()
    scheduleData.value = data
  } catch (error) {
    console.error('스케줄 데이터 로드 실패:', error)
  } finally {
    loadingSchedule.value = false
  }
}
// 스케줄 선택 함수 수정
const selectSchedule = async () => {
  if (selectedSchedule.value.length === 0) return

  try {
    if (!selectedSchedule.value[0]) {
      console.error('선택된 항목이 없습니다.')
      return
    }

    const selectedItem = selectedSchedule.value[0]
    console.log('선택된 스케줄:', selectedItem)

    // 선택한 스케줄 정보 저장
    selectedScheduleInfo.value = {
      passId: selectedItem.No,
      satelliteName: selectedItem.SatelliteName || selectedItem.SatelliteID || '알 수 없음',
      satelliteId: selectedItem.SatelliteID || 'N/A',
      startTime: selectedItem.StartTime,
      endTime: selectedItem.EndTime,
      duration: selectedItem.Duration,
      maxElevation: typeof selectedItem.MaxElevation === 'number' ? selectedItem.MaxElevation : 0,
      startTimeMs: new Date(selectedItem.StartTime).getTime(),
      timeRemaining: 0,
      startAzimuth: typeof selectedItem.StartAzimuth === 'number' ? selectedItem.StartAzimuth : 0,
      endAzimuth: typeof selectedItem.EndAzimuth === 'number' ? selectedItem.EndAzimuth : 0,
      startElevation:
        typeof selectedItem.StartElevation === 'number' ? selectedItem.StartElevation : 0,
      endElevation: typeof selectedItem.EndElevation === 'number' ? selectedItem.EndElevation : 0,
    }

    // 선택한 스케줄의 세부 데이터 로드 - ephemerisTrackService.ts:249-259 사용
    console.log('세부 데이터 로드 시작:', selectedItem.No)
    const detailData = await ephemerisTrackService.fetchEphemerisDetailData(selectedItem.No)
    console.log('로드된 세부 데이터:', detailData)

    // 세부 데이터가 있으면 첫 번째 항목의 방위각, 고도각, 틸트각 설정
    if (detailData && detailData.length > 0) {
      const firstPoint = detailData[0]

      // 타입 안전성 검사 후 속성 접근
      if (
        firstPoint &&
        typeof firstPoint.Azimuth === 'number' &&
        typeof firstPoint.Elevation === 'number'
      ) {
        ephemerisData.value.azimuth = firstPoint.Azimuth
        ephemerisData.value.elevation = firstPoint.Elevation
      } else {
        console.warn('유효한 방위각/고도각 데이터가 없습니다.')
        ephemerisData.value.azimuth = 0
        ephemerisData.value.elevation = 0
      }
      ephemerisData.value.tilt = 0 // 틸트각은 기본값 0으로 설정

      // 차트에 궤적 표시 - 차트가 초기화되었는지 확인
      if (chart) {
        console.log('차트에 궤적 표시 시작')
        updateChartWithTrajectory(detailData)
        console.log('차트에 궤적 표시 완료')
      } else {
        console.error('차트가 초기화되지 않았습니다.')
        // 차트가 초기화되지 않았다면 초기화 후 궤적 표시
        initChart()
        setTimeout(() => {
          if (chart) {
            updateChartWithTrajectory(detailData)
          }
        }, 100)
      }
    } else {
      console.warn('세부 데이터가 없거나 비어 있습니다.')
    }

    // 남은 시간 계산 시작
    updateTimeRemaining()

    // 이미 타이머가 있으면 제거
    if (timeUpdateTimer !== null) {
      clearInterval(timeUpdateTimer)
    }

    // 1초마다 남은 시간 업데이트
    timeUpdateTimer = window.setInterval(() => {
      updateTimeRemaining()
    }, 200)

    // 모달 닫기
    showScheduleModal.value = false
  } catch (error) {
    console.error('스케줄 세부 데이터 로드 실패:', error)
  }
}

// TLE 모달 열기
const openTLEModal = () => {
  showTLEModal.value = true
  // 이전 입력값을 유지하기 위해 초기화하지 않음
  // tempTLEData.value = { line1: '' }
  tleError.value = null
}

// TLE 데이터 형식화 함수
const formatTLEDisplay = (tleText: string): string => {
  const lines = tleText.split('\n').filter((line) => line.trim() !== '')

  if (lines.length >= 3) {
    // 3줄 형식: 첫 번째 줄은 위성 이름으로 강조
    return `<strong>위성 이름: ${lines[0]}</strong>\n${lines[1] || ''}\n${lines[2] || ''}`
  } else if (lines.length === 2) {
    // 2줄 형식
    return `${lines[0] || ''}\n${lines[1] || ''}`
  }

  return tleText
}

// TLE 데이터 추가 함수
const addTLEData = async () => {
  if (!tempTLEData.value?.line1?.trim()) {
    tleError.value = 'TLE 데이터를 입력해주세요.'
    return
  }

  isProcessingTLE.value = true
  tleError.value = null

  try {
    const inputText = tempTLEData.value.line1.trim()
    console.log('Processing TLE data:', inputText)

    // TLE 데이터 파싱
    const parsedTLE = ephemerisTrackService.parseTLEData(inputText)
    console.log('Parsed TLE data:', parsedTLE)

    const startTime = new Date().toISOString()
    const endTime = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString() // 24시간 후

    // TLE 데이터 포맷팅 및 저장
    tleData.value = {
      displayText: formatTLEDisplay(inputText),
      tleLine1: parsedTLE.tleLine1,
      tleLine2: parsedTLE.tleLine2,
      satelliteName: parsedTLE.satelliteName || 'Unknown Satellite',
      startTime: startTime,
      endTime: endTime,
      stepSize: 60,
    }

    // 백엔드에 TLE 데이터 전송하여 궤도 추적 데이터 생성
    await ephemerisTrackService.generateEphemerisTrack({
      tleLine1: parsedTLE.tleLine1,
      tleLine2: parsedTLE.tleLine2,
      satelliteName: parsedTLE.satelliteName || 'Unknown Satellite',
      startTime: startTime,
      endTime: endTime,
      stepSize: 60, // 60초 간격
    })

    // 스케줄 데이터 다시 로드
    await loadScheduleData()

    // 궤도 계산 실행
    await calculateTLE()

    // 성공 시 모달 닫기
    showTLEModal.value = false
  } catch (error) {
    const errorMessage =
      error instanceof Error ? error.message : 'TLE 데이터 처리 중 오류가 발생했습니다.'
    console.error('TLE 데이터 처리 오류:', error)
    tleError.value = errorMessage
  } finally {
    isProcessingTLE.value = false
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

    // 줄 단위로 분리
    const lines = tleData.value.displayText.split('\n').filter((line) => line.trim() !== '')

    if (lines.length < 2) {
      console.error('유효하지 않은 TLE 형식: 최소 2줄이 필요합니다')
      return
    }

    let line1 = ''
    let line2 = ''
    let satelliteName: string | null = null

    // 2줄 또는 3줄 형식 처리
    if (lines.length >= 3 && lines[0] && lines[1] && lines[2]) {
      // 3줄 형식: 첫 번째 줄은 위성 이름
      satelliteName = lines[0]
      line1 = lines[1]
      line2 = lines[2]
    } else if (lines.length >= 2 && lines[0] && lines[1]) {
      // 2줄 형식
      line1 = lines[0]
      line2 = lines[1]
    } else {
      console.error('유효하지 않은 TLE 형식')
      return
    }

    // TLE 라인 유효성 검사 (간단한 검사)
    if (!line1.startsWith('1 ') || !line2.startsWith('2 ')) {
      console.error('유효하지 않은 TLE 형식: 라인 1은 "1 "로, 라인 2는 "2 "로 시작해야 합니다')
      return
    }

    // 비동기 작업을 시뮬레이션하기 위한 Promise 추가 (ESLint 에러 해결)
    await Promise.resolve()

    // API 호출 대신 임시 값 설정 (실제 계산 로직이 구현될 때까지)
    // 여기서는 임의의 값을 설정하거나 기본값을 유지할 수 있습니다
    ephemerisData.value.azimuth = 45 // 임의의 방위각 값
    ephemerisData.value.elevation = 30 // 임의의 고도각 값
    ephemerisData.value.tilt = 0 // 임의의 틸트각 값

    console.log('TLE 유효성 검증 성공:', {
      line1,
      line2,
      satelliteName,
      timestamp: `${ephemerisData.value.date} ${ephemerisData.value.time}`,
    })
  } catch (error) {
    console.error('TLE 계산 실패:', error)
  }
}

// Stop 명령 전송
const stopCommand = async () => {
  // 모든 축 정지 (방위각, 고도각, 틸트각)
  await ephemerisTrackService.stopCommand(true, true, true)
  console.log('정지 명령 전송 성공')
}

// Stow 명령 전송
const stowCommand = async () => {
  await ephemerisTrackService.stowCommand()
  console.log('Stow 명령 전송 성공')
}

// Command handlers with error handling
const handleEphemerisCommand = async () => {
  try {
    if (!selectedScheduleInfo.value.passId) {
      console.error('No passId selected')
      return
    }

    const result = await ephemerisTrackService.startEphemerisTracking(
      selectedScheduleInfo.value.passId,
    )
    console.log('Ephemeris tracking started:', result)

    // Show success message to user
    $q.notify({
      type: 'positive',
      message: '위성 추적이 시작되었습니다.',
      position: 'top',
      timeout: 3000,
    })
  } catch (error) {
    console.error('Failed to start ephemeris tracking:', error)

    // Show error message to user
    $q.notify({
      type: 'negative',
      message: '위성 추적 시작에 실패했습니다.',
      position: 'top',
      timeout: 3000,
    })
  }
}

const handleStopCommand = () => {
  void stopCommand().catch((error) => {
    console.error('Failed to send stop command:', error)
  })
}

const handleStowCommand = () => {
  void stowCommand().catch((error) => {
    console.error('Failed to send stow command:', error)
  })
}

// 입력값이 변경될 때마다 소수점 둘째 자리까지 포맷팅
watch(
  inputs,
  (newValues) => {
    for (let i = 0; i < newValues.length; i++) {
      const value = newValues[i] || '0'
      const num = parseFloat(value)
      if (!isNaN(num)) {
        inputs.value[i] = num.toFixed(2)
      }
    }
  },
  { deep: true },
)

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
      await icdStore.sendTimeOffsetCommand(numValue)
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

// Formatted calibration time (for Time control) - UTC 시간 사용
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

  // 시간 업데이트 타이머 정리
  if (timeUpdateTimer !== null) {
    clearInterval(timeUpdateTimer)
  }

  // 차트 인스턴스 정리
  if (chart) {
    chart.dispose()
    chart = null
  }
})
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
