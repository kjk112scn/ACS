<template>
  <div class="ephemeris-mode">
    <div class="section-title text-h5 text-primary q-mb-sm">Ephemeris Designation</div>
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

        <!-- 2번 영역: 계산 정보 표시 영역 -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Tracking Information</div>
              <div class="ephemeris-form">
                <div class="form-row">
                  <q-input
                    v-model.number="ephemerisData.azimuth"
                    label="Azimuth (°)"
                    type="number"
                    outlined
                  />
                  <q-input
                    v-model.number="ephemerisData.elevation"
                    label="Elevation (°)"
                    type="number"
                    outlined
                  />
                  <q-input
                    v-model.number="ephemerisData.tilt"
                    label="Tilt (°)"
                    type="number"
                    outlined
                  />
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

    <!-- TLE 입력 모달 -->
    <q-dialog v-model="showTLEModal" persistent>
      <q-card class="q-pa-md" style="width: 700px; max-width: 95vw">
        <q-card-section class="bg-primary text-white">
          <div class="text-h6">TLE 입력</div>
        </q-card-section>

        <q-card-section class="q-pa-md">
          <div class="text-body2 q-mb-md">
            2줄 또는 3줄 형식의 TLE 데이터를 입력하세요. 3줄 형식인 경우 첫 번째 줄은 위성 이름으로 처리됩니다.
            <br>예시:
            <pre class="q-mt-sm q-pa-sm bg-grey-9 text-white rounded-borders" style="font-size: 0.8rem; white-space: pre-wrap;">ISS (ZARYA)
1 25544U 98067A   24054.51736111  .00020125  00000+0  36182-3 0  9999
2 25544  51.6416 142.1133 0003324 324.9821 218.2594 15.49780383446574</pre>
          </div>
          <div class="tle-input-container q-mb-md">
            <q-input
              v-model="tempTLEData.line1"
              type="textarea"
              filled
              autogrow
              class="tle-textarea full-width"
              style="min-height: 200px; font-family: monospace; font-size: 0.9rem;"
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { api } from 'boot/axios'
import { date } from 'quasar'
import type { QTableProps } from 'quasar' // QTableProps 타입 임포트
import { useICDStore } from '../../stores/API/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

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
}

// EphemerisTrackStore 임시 구현 (실제로는 별도 파일로 분리해야 함)
const ephemerisTrackStore = {
  async fetchEphemerisMasterData() {
    try {
      const response = await api.get('/satellite/ephemeris/master')
      return response.data || []
    } catch (error) {
      console.error('마스터 데이터 조회 실패:', error)
      return []
    }
  },

  async fetchEphemerisDetailData(mstId: number): Promise<ScheduleDetailItem[]> {
    try {
      const response = await api.get<ScheduleDetailItem[]>(`/satellite/ephemeris/detail/${mstId}`)
      return response.data || []
    } catch (error) {
      console.error('세부 데이터 조회 실패:', error)
      return []
    }
  },

  parseTLEData(tleText: string) {
    // 디버깅 로그 추가
    console.log('입력된 TLE 텍스트:', tleText)
    console.log('텍스트 길이:', tleText.length)
    
    // 줄바꿈 문자 정규화
    const normalizedText = tleText
      .replace(/\r\n/g, '\n')  // Windows 줄바꿈을 \n으로 통일
      .replace(/\r/g, '\n')    // Mac 이전 버전 줄바꿈 처리
    
    // 줄바꿈 문자로 분리하고 빈 줄 제거
    const lines = normalizedText.split('\n').filter((line) => line.trim() !== '')
    
    // 디버깅 로그 추가
    console.log('정규화 후 분리된 라인 수:', lines.length)
    console.log('정규화 후 분리된 라인:', lines)

    if (lines.length < 2) {
      throw new Error('유효하지 않은 TLE 형식: 최소 2줄이 필요합니다')
    }

    let tleLine1 = ''
    let tleLine2 = ''
    let satelliteName = null

    if (lines.length >= 3) {
      satelliteName = lines[0]?.trim() || ''
      tleLine1 = lines[1]?.trim() || ''
      tleLine2 = lines[2]?.trim() || ''
    } else if (lines.length >= 2) {
      tleLine1 = lines[0]?.trim() || ''
      tleLine2 = lines[1]?.trim() || ''
    } else if (lines.length === 1) {
      tleLine1 = lines[0]?.trim() || ''
      tleLine2 = ''
    } else {
      // 라인이 없는 경우
      tleLine1 = ''
      tleLine2 = ''
    }

    return { tleLine1, tleLine2, satelliteName }
  },

  async generateEphemerisTrack(request: TLEData) {
    try {
      console.log(
        '위성 궤도 추적 데이터 생성 요청:',
        request.satelliteName,
        request.tleLine1,
        request.tleLine2,
      )
      // API_ENDPOINTS.EPHEMERIS.GENERATE 경로 사용
      const response = await api.post('/satellite/ephemeris/generate', request)
      return response.data
    } catch (error) {
      console.error('위성 궤도 추적 데이터 생성 실패:', error)
      throw error
    }
  },
}

// 인터페이스 정의
interface ScheduleItem {
  No: number
  SatelliteID: string
  SatelliteName: string
  StartTime: string
  EndTime: string
  Duration: string
  MaxElevation: number
  CreationDate: string
  Creator: string
  [key: string]: string | number | boolean | null | undefined
}

interface ScheduleDetailItem {
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
  satelliteName: undefined
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
  },
  { name: 'StartTime', label: '시작 시간', field: 'StartTime', align: 'left', sortable: true },
  { name: 'EndTime', label: '종료 시간', field: 'EndTime', align: 'left', sortable: true },
  { name: 'Duration', label: '지속 시간', field: 'Duration', align: 'left', sortable: true },
  {
    name: 'MaxElevation',
    label: '최대 고도 (°)',
    field: 'MaxElevation',
    align: 'left',
    sortable: true,
  },
]

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
        data: [],
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
      // 궤적 라인은 그대로 유지
    ],
  })
}

// 궤적 라인을 차트에 추가하는 함수
const updateChartWithTrajectory = (detailData: ScheduleDetailItem[]) => {
  if (!chart) return

  // 궤적 데이터 포인트 생성 (방위각, 고도각만 사용)
  const trajectoryPoints = detailData.map((point) => [point.Azimuth, point.Elevation])

  // 차트 옵션 업데이트 (궤적 라인만 업데이트)
  chart.setOption({
    series: [
      {}, // 기존 포인트 시리즈는 그대로 유지
      {}, // 기존 라인 시리즈는 그대로 유지
      {
        // 궤적 라인 업데이트
        data: trajectoryPoints,
      },
    ],
  })
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
    // 스토어에서 마스터 데이터 로드
    const data = await ephemerisTrackStore.fetchEphemerisMasterData()
    scheduleData.value = data
  } catch (error) {
    console.error('스케줄 데이터 로드 실패:', error)
  } finally {
    loadingSchedule.value = false
  }
}

// 스케줄 선택 함수
const selectSchedule = async () => {
  if (selectedSchedule.value.length === 0) return

  try {
    // selectedSchedule.value가 비어있지 않다는 것을 확인했으므로 첫 번째 항목은 존재함
    // 하지만 TypeScript는 이를 인식하지 못하므로 타입 가드 추가
    if (!selectedSchedule.value[0]) {
      console.error('선택된 항목이 없습니다.')
      return
    }

    const selectedItem = selectedSchedule.value[0]

    // 선택한 스케줄의 세부 데이터 로드
    const detailData = await ephemerisTrackStore.fetchEphemerisDetailData(selectedItem.No)

    // 세부 데이터가 있으면 첫 번째 항목의 방위각, 고도각, 틸트각 설정
    if (detailData.length > 0) {
      // detailData가 비어있지 않다는 것을 확인했으므로 첫 번째 항목은 존재함
      // 하지만 TypeScript는 이를 인식하지 못하므로 타입 가드 추가
      if (!detailData[0]) {
        console.error('세부 데이터의 첫 번째 항목이 없습니다.')
        return
      }

      const firstPoint = detailData[0]

      ephemerisData.value.azimuth = firstPoint.Azimuth
      ephemerisData.value.elevation = firstPoint.Elevation
      ephemerisData.value.tilt = 0 // 틸트각은 기본값 0으로 설정

      // 차트에 궤적 표시
      updateChartWithTrajectory(detailData)
    }

    // 모달 닫기
    showScheduleModal.value = false
  } catch (error) {
    console.error('스케줄 세부 데이터 로드 실패:', error)
  }
}

// TLE 관련 상태
const tleError = ref<string | null>(null)
const isProcessingTLE = ref(false)

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
    const parsedTLE = ephemerisTrackStore.parseTLEData(inputText)
    console.log('Parsed TLE data:', parsedTLE)
    
    // 유효성 검증
    if (!parsedTLE.tleLine1 || !parsedTLE.tleLine2) {
      throw new Error('유효하지 않은 TLE 형식입니다. 2줄 또는 3줄 형식의 TLE 데이터를 입력해주세요.')
    }
    
    // TLE 데이터 포맷팅 및 저장
    tleData.value = {
      displayText: formatTLEDisplay(inputText),
      tleLine1: parsedTLE.tleLine1,
      tleLine2: parsedTLE.tleLine2,
      satelliteName: parsedTLE.satelliteName || 'Unknown Satellite'
    }
    
    // 백엔드에 TLE 데이터 전송하여 궤도 추적 데이터 생성
    await ephemerisTrackStore.generateEphemerisTrack({
      displayText: formatTLEDisplay(inputText),
      ...parsedTLE
    })
    
    // 스케줄 데이터 다시 로드
    await loadScheduleData()
    
    // 궤도 계산 실행
    await calculateTLE()
    
    // 성공 시 모달 닫기
    showTLEModal.value = false
    
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'TLE 데이터 처리 중 오류가 발생했습니다.'
    console.error('TLE 데이터 처리 오류:', error)
    tleError.value = errorMessage
    
    // 오류 발생 시 사용자에게 알림
    // 여기에 알림 컴포넌트나 대화상자를 표시할 수 있습니다.
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

    // TLE 계산 API 호출
    const response = await api.post('/satellite/tle/calculate', {
      line1: line1,
      line2: line2,
      satelliteName: satelliteName, // 위성 이름이 있는 경우 전송
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
    throw error // Re-throw to allow handling by caller
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
    throw error
  }
}

// Stow 명령 전송
const stowCommand = async () => {
  try {
    await icdStore.stowCommand()
    console.log('Stow 명령 전송 성공')
  } catch (error) {
    console.error('Stow 명령 전송 실패:', error)
    throw error
  }
}

// Command handlers with error handling
const handleEphemerisCommand = () => {
  void sendEphemerisCommand().catch((error) => {
    console.error('Failed to send ephemeris command:', error)
    // Here you could add user notification of the error
  })
}

const handleStopCommand = () => {
  void stopCommand().catch((error) => {
    console.error('Failed to send stop command:', error)
    // Here you could add user notification of the error
  })
}

const handleStowCommand = () => {
  void stowCommand().catch((error) => {
    console.error('Failed to send stow command:', error)
    // Here you could add user notification of the error
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
